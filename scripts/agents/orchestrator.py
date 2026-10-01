#!/usr/bin/env python3
r"""
Run several Claude Code agents in parallel on backlog tasks, each in its own git worktree.

    python3 scripts/agents/orchestrator.py status
    python3 scripts/agents/orchestrator.py run --parallel 2 --lanes be,android,core --watch
    python3 scripts/agents/orchestrator.py logs BE-01
    python3 scripts/agents/orchestrator.py verify BE-01
    python3 scripts/agents/orchestrator.py merge BE-01
    python3 scripts/agents/orchestrator.py retry BE-01
    python3 scripts/agents/orchestrator.py clean BE-01

Lifecycle of a task:
    pending -> running -> (verifying) -> review -> merged
                       \-> failed / blocked        (retry -> pending)

A task is *ready* when every dependency is merged into main. You review a task in its worktree,
then `merge` it; with --watch the orchestrator keeps going and starts whatever became ready.

Only the Python standard library is used. Requires git and the `claude` CLI (Claude Code) on PATH.
"""
from __future__ import annotations

import argparse
import json
import os
import shlex
import shutil
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

# ---------------------------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------------------------

MAIN_BRANCH = "main"
BRANCH_PREFIX = "task/"
POLL_SECONDS = 20

# Tools the agent may use without asking. Anything else is denied (nobody is there to approve).
ALLOWED_TOOLS = [
    "Read", "Edit", "Write", "Glob", "Grep", "WebFetch", "WebSearch", "TodoWrite",
    "Bash(./gradlew *)", "Bash(./gradlew)",
    "Bash(git status)", "Bash(git status *)", "Bash(git diff)", "Bash(git diff *)",
    "Bash(git add *)", "Bash(git commit *)", "Bash(git log *)", "Bash(git show *)",
    "Bash(git mv *)", "Bash(git rm *)", "Bash(git restore *)",
    "Bash(ls *)", "Bash(ls)", "Bash(mkdir *)", "Bash(find *)", "Bash(cat *)", "Bash(head *)",
    "Bash(tail *)", "Bash(wc *)", "Bash(grep *)", "Bash(rg *)", "Bash(sort *)", "Bash(diff *)",
    "Bash(npm *)", "Bash(npx *)", "Bash(node *)",
    "Bash(xcodegen *)", "Bash(xcodebuild *)", "Bash(docker *)", "Bash(java -version)",
]
DISALLOWED_TOOLS = [
    "Bash(git push *)", "Bash(git push)", "Bash(git rebase *)", "Bash(git reset *)",
    "Bash(git checkout *)", "Bash(git switch *)", "Bash(git branch *)", "Bash(git config *)",
    "Bash(git worktree *)", "Bash(rm -rf *)", "Bash(sudo *)",
]

# Gitignored files that each worktree needs (copied from the main checkout when present).
LOCAL_FILES = ["local.properties", "app-android/google-services.json"]

STATUS_ORDER = ["running", "verifying", "review", "blocked", "failed", "pending", "merged"]


# ---------------------------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------------------------

def sh(cmd: list[str], cwd: Path, check: bool = True, capture: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=cwd, check=check, text=True,
                          stdout=subprocess.PIPE if capture else None,
                          stderr=subprocess.STDOUT if capture else None)


def now() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def repo_root() -> Path:
    out = subprocess.run(["git", "rev-parse", "--show-toplevel"], text=True, capture_output=True,
                         cwd=Path(__file__).resolve().parent)
    if out.returncode != 0:
        sys.exit("Not inside a git repository.")
    return Path(out.stdout.strip())


class Orchestrator:
    def __init__(self, root: Path, worktrees: Path | None = None):
        self.root = root
        self.state_dir = root / ".agents"
        self.logs_dir = self.state_dir / "logs"
        self.state_file = self.state_dir / "state.json"
        self.worktrees = worktrees or root.parent / f"{root.name}-worktrees"
        self.logs_dir.mkdir(parents=True, exist_ok=True)
        tasks_file = root / "docs" / "backlog" / "tasks.json"
        self.tasks = {t["id"]: t for t in json.loads(tasks_file.read_text())["tasks"]}
        self.prompt_template = (root / "scripts" / "agents" / "task-prompt.md").read_text()
        self.state: dict[str, dict] = json.loads(self.state_file.read_text()) if self.state_file.exists() else {}

    # -- state ------------------------------------------------------------------------------

    def save(self) -> None:
        tmp = self.state_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.state, indent=2, ensure_ascii=False))
        tmp.replace(self.state_file)

    def entry(self, task_id: str) -> dict:
        return self.state.setdefault(task_id, {"status": "pending"})

    def branch(self, task_id: str) -> str:
        return BRANCH_PREFIX + task_id

    def worktree(self, task_id: str) -> Path:
        return self.worktrees / task_id

    def branch_exists(self, branch: str) -> bool:
        return sh(["git", "rev-parse", "--verify", "--quiet", branch], self.root, check=False).returncode == 0

    def is_merged(self, task_id: str) -> bool:
        e = self.entry(task_id)
        if e.get("status") == "merged":
            return True
        # A reviewed branch that you merged yourself (e.g. via a GitHub PR, then `git pull`).
        br = self.branch(task_id)
        if e.get("status") == "review" and self.branch_exists(br):
            if sh(["git", "merge-base", "--is-ancestor", br, MAIN_BRANCH], self.root, check=False).returncode == 0:
                e.update(status="merged", merged_at=now(), note="Merged outside the orchestrator")
                return True
        return False

    def is_ready(self, task_id: str) -> bool:
        if self.entry(task_id).get("status", "pending") != "pending":
            return False
        return all(self.is_merged(dep) for dep in self.tasks[task_id]["deps"])

    # -- agent lifecycle ----------------------------------------------------------------------

    def start(self, task_id: str, model: str | None, max_turns: int | None) -> None:
        task, e = self.tasks[task_id], self.entry(task_id)
        wt, br = self.worktree(task_id), self.branch(task_id)
        self.worktrees.mkdir(parents=True, exist_ok=True)
        if not wt.exists():
            if self.branch_exists(br):
                sh(["git", "worktree", "add", str(wt), br], self.root)
            else:
                sh(["git", "worktree", "add", "-b", br, str(wt), MAIN_BRANCH], self.root)
        for rel in LOCAL_FILES:
            src = self.root / rel
            if src.exists() and not (wt / rel).exists():
                (wt / rel).parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, wt / rel)

        prompt = self.prompt_template.format(id=task_id, title=task["title"], spec=task["spec"],
                                             verify=task["verify"], branch=br)
        cmd = ["claude", "-p", prompt,
               "--permission-mode", "acceptEdits",
               "--permission-prompts", "none",
               "--output-format", "stream-json", "--verbose",
               "--allowedTools", *ALLOWED_TOOLS,
               "--disallowedTools", *DISALLOWED_TOOLS]
        if model:
            cmd += ["--model", model]
        if max_turns:
            cmd += ["--max-turns", str(max_turns)]

        log = self.logs_dir / f"{task_id}.jsonl"
        with open(log, "w") as fh:
            proc = subprocess.Popen(cmd, cwd=wt, stdout=fh, stderr=subprocess.STDOUT,
                                    stdin=subprocess.DEVNULL, start_new_session=True)
        e.update(status="running", pid=proc.pid, branch=br, worktree=str(wt), log=str(log),
                 started_at=now(), finished_at=None, note=None)
        self.save()
        print(f"▶ {task_id} started (pid {proc.pid}) in {wt}")

    def alive(self, pid: int | None) -> bool:
        if not pid:
            return False
        try:
            finished, _ = os.waitpid(pid, os.WNOHANG)
            if finished == pid:
                return False
        except ChildProcessError:
            pass  # not our child (orchestrator restarted): fall back to kill(0)
        try:
            os.kill(pid, 0)
            return True
        except ProcessLookupError:
            return False
        except PermissionError:
            return True

    def finish(self, task_id: str) -> None:
        e = self.entry(task_id)
        wt = Path(e["worktree"])
        e["finished_at"] = now()
        if (wt / "BLOCKED.md").exists():
            e.update(status="blocked", note="Agent wrote BLOCKED.md")
        elif sh(["git", "rev-list", "--count", f"{MAIN_BRANCH}..HEAD"], wt).stdout.strip() == "0":
            e.update(status="failed", note="Agent made no commit (see logs)")
        else:
            self.run_verify(task_id)
        self.stop_gradle(wt)
        self.save()
        print(f"■ {task_id}: {e['status']}" + (f" — {e['note']}" if e.get("note") else ""))

    def run_verify(self, task_id: str) -> bool:
        e = self.entry(task_id)
        wt = Path(e["worktree"])
        e["status"] = "verifying"
        self.save()
        if sh(["git", "status", "--porcelain"], wt).stdout.strip():
            e.update(status="failed", note="Uncommitted changes left in the worktree")
            return False
        vlog = self.logs_dir / f"{task_id}.verify.log"
        with open(vlog, "w") as fh:
            rc = subprocess.run(self.tasks[task_id]["verify"], shell=True, cwd=wt, stdout=fh,
                                stderr=subprocess.STDOUT).returncode
        e["verify_log"] = str(vlog)
        if rc == 0:
            e.update(status="review", note="Verify passed — review the worktree, then `merge`")
            return True
        e.update(status="failed", note=f"Verify failed (exit {rc}), see {vlog.name}")
        return False

    @staticmethod
    def stop_gradle(wt: Path) -> None:
        # Free RAM: each worktree starts its own Gradle daemon.
        if (wt / "gradlew").exists():
            subprocess.run(["./gradlew", "--stop"], cwd=wt, stdout=subprocess.DEVNULL,
                           stderr=subprocess.DEVNULL)

    # -- commands -----------------------------------------------------------------------------

    def run(self, parallel: int, lanes: set[str] | None, only: set[str] | None, watch: bool,
            model: str | None, max_turns: int | None) -> None:
        def eligible(tid: str) -> bool:
            return (lanes is None or self.tasks[tid]["lane"] in lanes) and (only is None or tid in only)

        print(f"Worktrees: {self.worktrees}   parallel={parallel}   Ctrl+C stops the loop (agents keep running)")
        try:
            while True:
                for tid, e in list(self.state.items()):
                    if e.get("status") == "running" and not self.alive(e.get("pid")):
                        self.finish(tid)
                running = [t for t, e in self.state.items() if e.get("status") == "running"]
                for tid in self.tasks:
                    if len(running) >= parallel:
                        break
                    if eligible(tid) and self.is_ready(tid):
                        self.start(tid, model, max_turns)
                        running.append(tid)
                self.save()

                remaining = [t for t in self.tasks if eligible(t) and self.entry(t).get("status") != "merged"]
                if not remaining:
                    print("All selected tasks are merged. 🎉")
                    return
                if not running and not watch:
                    waiting = [t for t in remaining if self.entry(t).get("status") in ("review", "blocked", "failed")]
                    print("Nothing running. Waiting on you for: " + (", ".join(waiting) or "dependencies"))
                    self.print_status()
                    return
                time.sleep(POLL_SECONDS)
        except KeyboardInterrupt:
            print("\nLoop stopped. Running agents continue; run `status` or `run` again later.")

    def print_status(self) -> None:
        def order(tid: str) -> tuple:
            st = self.entry(tid).get("status", "pending")
            return (STATUS_ORDER.index(st) if st in STATUS_ORDER else 99, tid)

        print(f"{'TASK':8} {'STATUS':10} {'READY':6} {'LANE':8} TITLE / NOTE")
        for tid in sorted(self.tasks, key=order):
            e, t = self.entry(tid), self.tasks[tid]
            ready = "yes" if self.is_ready(tid) else ""
            print(f"{tid:8} {e.get('status', 'pending'):10} {ready:6} {t['lane']:8} {t['title']}")
            if e.get("note"):
                print(f"{'':35}↳ {e['note']}")
        self.save()

    def logs(self, task_id: str, raw: bool) -> None:
        log = self.logs_dir / f"{task_id}.jsonl"
        if not log.exists():
            sys.exit(f"No log for {task_id}")
        for line in log.read_text().splitlines():
            if raw:
                print(line)
                continue
            try:
                ev = json.loads(line)
            except json.JSONDecodeError:
                print(line)
                continue
            if ev.get("type") == "assistant":
                for block in ev.get("message", {}).get("content", []):
                    if block.get("type") == "text" and block.get("text", "").strip():
                        print(f"💬 {block['text'].strip()}")
                    elif block.get("type") == "tool_use":
                        inp = block.get("input", {})
                        detail = inp.get("command") or inp.get("file_path") or inp.get("pattern") or ""
                        print(f"🔧 {block.get('name')} {str(detail)[:160]}")
            elif ev.get("type") == "result":
                print(f"✅ result: {ev.get('subtype')}  turns={ev.get('num_turns')}  "
                      f"duration={round((ev.get('duration_ms') or 0) / 1000)}s")

    def merge(self, task_id: str, force: bool) -> None:
        e = self.entry(task_id)
        if e.get("status") != "review" and not force:
            sys.exit(f"{task_id} is '{e.get('status')}', not 'review'. Use --force to merge anyway.")
        if sh(["git", "rev-parse", "--abbrev-ref", "HEAD"], self.root).stdout.strip() != MAIN_BRANCH:
            sys.exit(f"Check out {MAIN_BRANCH} in {self.root} first.")
        if sh(["git", "status", "--porcelain", "--untracked-files=no"], self.root).stdout.strip():
            sys.exit("Main checkout has uncommitted changes.")
        br = self.branch(task_id)
        title = self.tasks[task_id]["title"]
        if sh(["git", "merge", "--ff-only", br], self.root, check=False).returncode != 0:
            res = sh(["git", "merge", "--no-ff", br, "-m", f"Merge {task_id}: {title}"], self.root, check=False)
            if res.returncode != 0:
                sh(["git", "merge", "--abort"], self.root, check=False)
                sys.exit(f"Merge conflict. Resolve manually, or run `retry {task_id} --rebase` "
                         f"to let an agent redo it on top of {MAIN_BRANCH}.\n{res.stdout}")
        self.remove_worktree(task_id)
        sh(["git", "branch", "-d", br], self.root, check=False)
        e.update(status="merged", merged_at=now(), note=None)
        self.save()
        print(f"✔ {task_id} merged into {MAIN_BRANCH}. Push when you are ready: git push")

    def retry(self, task_id: str, fresh: bool) -> None:
        e = self.entry(task_id)
        if e.get("status") == "running":
            sys.exit(f"{task_id} is still running.")
        if fresh:
            self.remove_worktree(task_id)
            sh(["git", "branch", "-D", self.branch(task_id)], self.root, check=False)
        else:
            wt = self.worktree(task_id)
            if (wt / "BLOCKED.md").exists():
                print("Note: BLOCKED.md is still in the worktree; remove it once the blocker is solved.")
        self.state[task_id] = {"status": "pending"}
        self.save()
        print(f"{task_id} reset to pending" + (" (fresh branch from main)" if fresh else " (keeps its branch)"))

    def remove_worktree(self, task_id: str) -> None:
        wt = self.worktree(task_id)
        if wt.exists():
            self.stop_gradle(wt)
            sh(["git", "worktree", "remove", "--force", str(wt)], self.root, check=False)
        sh(["git", "worktree", "prune"], self.root, check=False)

    def stop(self, task_id: str) -> None:
        e = self.entry(task_id)
        pid = e.get("pid")
        if e.get("status") == "running" and pid:
            try:
                os.killpg(pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            e.update(status="failed", note="Stopped by user")
            self.save()
            print(f"{task_id} stopped")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--worktrees", type=Path, help="Directory for task worktrees (default: ../<repo>-worktrees)")
    sub = p.add_subparsers(dest="cmd", required=True)

    r = sub.add_parser("run", help="Start ready tasks and supervise them")
    r.add_argument("--parallel", type=int, default=2, help="Max agents at once (default 2; Gradle needs RAM)")
    r.add_argument("--lanes", help="Comma-separated lanes, e.g. core,be,android")
    r.add_argument("--only", help="Comma-separated task ids")
    r.add_argument("--watch", action="store_true", help="Keep running and pick up tasks as you merge")
    r.add_argument("--model", help="Claude model alias, e.g. opus or sonnet")
    r.add_argument("--max-turns", type=int, help="Safety limit on agent turns")

    sub.add_parser("status", help="Show task states")
    lg = sub.add_parser("logs", help="Readable agent log")
    lg.add_argument("task")
    lg.add_argument("--raw", action="store_true")
    for name, helptext in [("verify", "Re-run the verify command in the task worktree"),
                           ("stop", "Stop a running agent")]:
        sp = sub.add_parser(name, help=helptext)
        sp.add_argument("task")
    m = sub.add_parser("merge", help="Merge a reviewed task into main and remove its worktree")
    m.add_argument("task")
    m.add_argument("--force", action="store_true")
    rt = sub.add_parser("retry", help="Reset a failed/blocked task to pending")
    rt.add_argument("task")
    rt.add_argument("--rebase", "--fresh", dest="fresh", action="store_true",
                    help="Discard the old branch and start again from main")
    c = sub.add_parser("clean", help="Remove a task's worktree and branch")
    c.add_argument("task")

    a = p.parse_args()
    o = Orchestrator(repo_root(), a.worktrees.resolve() if a.worktrees else None)
    task = getattr(a, "task", None)
    if task and task not in o.tasks:
        sys.exit(f"Unknown task {task}. Known: {', '.join(o.tasks)}")

    if a.cmd == "run":
        if shutil.which("claude") is None:
            sys.exit("`claude` CLI not found. Install Claude Code and run `claude` once to sign in.")
        split = lambda s: {x.strip() for x in s.split(",") if x.strip()} if s else None
        o.run(a.parallel, split(a.lanes), split(a.only), a.watch, a.model, a.max_turns)
    elif a.cmd == "status":
        o.print_status()
    elif a.cmd == "logs":
        o.logs(task, a.raw)
    elif a.cmd == "verify":
        ok = o.run_verify(task)
        o.save()
        print(f"{task}: {'passed' if ok else 'failed'} — {o.entry(task).get('note')}")
    elif a.cmd == "stop":
        o.stop(task)
    elif a.cmd == "merge":
        o.merge(task, a.force)
    elif a.cmd == "retry":
        o.retry(task, a.fresh)
    elif a.cmd == "clean":
        o.remove_worktree(task)
        sh(["git", "branch", "-D", o.branch(task)], o.root, check=False)
        o.state.pop(task, None)
        o.save()
        print(f"{task} cleaned")


if __name__ == "__main__":
    main()
