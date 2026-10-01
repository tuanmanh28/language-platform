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
With --auto it merges every task that passed verify + review itself (and pushes with --push),
retries a failed task once from a fresh branch, and has the agent merge main into a branch that conflicts.

Only the Python standard library is used. Requires git and the `claude` CLI (Claude Code) on PATH.
"""
from __future__ import annotations

import argparse
import json
import re
import os
import shlex
import shutil
import signal
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

MAIN_BRANCH = "main"
POLL_SECONDS = 20
REVIEW_ROUNDS = 2
REVIEW_TOOLS = ["Read", "Grep", "Glob", "Bash(git diff *)", "Bash(git log *)", "Bash(git show *)", "Bash(git status)"]
ACTIVE = ("running", "reviewing")

ALLOWED_TOOLS = [
    "Read", "Edit", "Write", "Glob", "Grep", "WebFetch", "WebSearch", "TodoWrite",
    "Bash(./gradlew *)", "Bash(./gradlew)",
    "Bash(git status)", "Bash(git status *)", "Bash(git diff)", "Bash(git diff *)",
    "Bash(git add *)", "Bash(git commit *)", "Bash(git log *)", "Bash(git show *)",
    "Bash(git mv *)", "Bash(git rm *)", "Bash(git restore *)", "Bash(git merge --no-edit main)",
    "Bash(ls *)", "Bash(ls)", "Bash(mkdir *)", "Bash(find *)", "Bash(cat *)", "Bash(head *)",
    "Bash(tail *)", "Bash(wc *)", "Bash(grep *)", "Bash(rg *)", "Bash(sort *)", "Bash(diff *)",
    "Bash(npm *)", "Bash(npx *)", "Bash(node *)",
    "Bash(xcodegen *)", "Bash(xcodebuild *)", "Bash(xcrun *)", "Bash(swift *)", "Bash(docker *)",
    "Bash(java -version)", "Bash(bash scripts/*)", "Bash(./scripts/*)", "Bash(chmod +x *)",
    "Bash(git update-index --chmod=+x *)",
]
DISALLOWED_TOOLS = [
    "Bash(git push *)", "Bash(git push)", "Bash(git rebase *)", "Bash(git reset *)",
    "Bash(git checkout *)", "Bash(git switch *)", "Bash(git branch *)", "Bash(git config *)",
    "Bash(git worktree *)", "Bash(rm -rf *)", "Bash(sudo *)",
]

LOCAL_FILES = ["local.properties", "app-android/google-services.json"]

AUTO_RETRIES = 1
LIMIT_WAIT_SECONDS = 30 * 60
LIMIT_PATTERN = re.compile(r"usage limit|limit reached|rate.?limit|overloaded|resets? at", re.IGNORECASE)
CONFLICT_ROUNDS = 2
CI_ROUNDS = 2
CI_NO_CHECKS_SECONDS = 20 * 60
BASE_REF = f"origin/{MAIN_BRANCH}"
CI_FIX_PROMPT = ("\n\nYour branch passed review, but CI on GitHub failed. Reproduce the failure locally, fix the cause "
                 "(never weaken or skip a check), run the verify command and commit the fix (`fix: …`). "
                 "Failed job logs:\n\n")

REVIEW_FIX_PROMPT = ("\n\nThe mandatory code review rejected your previous attempt. Fix every finding below, "
                     "run the verify command again and commit the fixes (`fix: …` or `refactor: …`).\n\n")
CONFLICT_PROMPT = (f"\n\nYour work passed review, but {MAIN_BRANCH} moved on and your branch now conflicts with it. "
                   f"Run `git merge --no-edit {MAIN_BRANCH}`, resolve every conflict so both your change and the "
                   f"new {MAIN_BRANCH} work are kept, run the verify command, then `git add` the files and "
                   f"`git commit --no-edit`. Change nothing else.")

STATUS_ORDER = ["running", "verifying", "reviewing", "review", "ci", "blocked", "failed", "pending", "merged"]


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


class MergeError(Exception):
    def __init__(self, message: str, conflict: bool = False):
        super().__init__(message)
        self.conflict = conflict


def notify(message: str) -> None:
    print(message)
    if sys.platform == "darwin":
        script = f"display notification {json.dumps(message)} with title \"Language Platform agents\""
        subprocess.run(["osascript", "-e", script], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


class Orchestrator:
    def __init__(self, root: Path, worktrees: Path | None = None):
        self.model: str | None = None
        self.max_turns: int | None = None
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
        self.install_hooks()

    def install_hooks(self) -> None:
        if (self.root / ".githooks").is_dir():
            sh(["git", "config", "core.hooksPath", ".githooks"], self.root)

    def save(self) -> None:
        tmp = self.state_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.state, indent=2, ensure_ascii=False))
        tmp.replace(self.state_file)

    def entry(self, task_id: str) -> dict:
        return self.state.setdefault(task_id, {"status": "pending"})

    def branch(self, task_id: str) -> str:
        """Conventional branch name, e.g. feat/backend-config (kept stable once a task started)."""
        known = self.state.get(task_id, {}).get("branch")
        if known:
            return known
        t = self.tasks[task_id]
        slug = t.get("slug") or re.sub(r"[^a-z0-9]+", "-", t["title"].lower()).strip("-")[:40]
        return f"{t.get('type', 'feat')}/{slug}"

    def worktree(self, task_id: str) -> Path:
        return self.worktrees / task_id

    def branch_exists(self, branch: str) -> bool:
        return sh(["git", "rev-parse", "--verify", "--quiet", branch], self.root, check=False).returncode == 0

    def is_merged(self, task_id: str) -> bool:
        e = self.entry(task_id)
        if e.get("status") == "merged":
            return True
        br = self.branch(task_id)
        if e.get("status") == "review" and self.branch_exists(br):
            if sh(["git", "merge-base", "--is-ancestor", br, MAIN_BRANCH], self.root, check=False).returncode == 0:
                e.update(status="merged", merged_at=now(), note="Merged outside the orchestrator")
                return True
        return False

    def is_ready(self, task_id: str) -> bool:
        e = self.entry(task_id)
        if e.get("status", "pending") != "pending" or time.time() < e.get("not_before", 0):
            return False
        return all(self.is_merged(dep) for dep in self.tasks[task_id]["deps"])

    def start(self, task_id: str, extra: str | None = None) -> None:
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
                                             verify=task["verify"], branch=br, type=task.get("type", "feat"))
        if extra:
            prompt += extra
        cmd = ["claude", "-p", prompt,
               "--permission-mode", "acceptEdits",
               "--permission-prompts", "none",
               "--output-format", "stream-json", "--verbose",
               "--allowedTools", *ALLOWED_TOOLS,
               "--disallowedTools", *DISALLOWED_TOOLS]
        if self.model:
            cmd += ["--model", self.model]
        if self.max_turns:
            cmd += ["--max-turns", str(self.max_turns)]

        log = self.logs_dir / f"{task_id}.jsonl"
        with open(log, "a" if extra else "w") as fh:
            proc = subprocess.Popen(cmd, cwd=wt, stdout=fh, stderr=subprocess.STDOUT,
                                    stdin=subprocess.DEVNULL, start_new_session=True)
        e.pop("not_before", None)
        e.update(status="running", pid=proc.pid, branch=br, worktree=str(wt), log=str(log),
                 started_at=now(), finished_at=None, note="Continuing: " + extra.strip()[:60] + "…" if extra else None)
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
            pass
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
        if self.hit_limit(Path(e["log"])):
            e.update(status="pending", not_before=time.time() + LIMIT_WAIT_SECONDS,
                     note="Claude usage limit reached — continuing on the same branch later")
        elif (wt / "BLOCKED.md").exists():
            e.update(status="blocked", note="Agent wrote BLOCKED.md")
        elif sh(["git", "rev-list", "--count", f"{MAIN_BRANCH}..HEAD"], wt).stdout.strip() == "0":
            e.update(status="failed", note="Agent made no commit (see logs)")
        elif self.run_verify(task_id):
            e["review_stage"] = 0
            self.start_review(task_id)
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
            e.update(status="verified", note="Verify passed")
            return True
        e.update(status="failed", note=f"Verify failed (exit {rc}), see {vlog.name}")
        return False

    def reviewers(self, task_id: str) -> list[str]:
        return self.tasks[task_id].get("reviewers", ["code-reviewer"])

    def review_log(self, task_id: str) -> Path:
        agent = self.reviewers(task_id)[self.entry(task_id).get("review_stage", 0)]
        return self.logs_dir / f"{task_id}.{agent}.json"

    def start_review(self, task_id: str) -> None:
        e, task = self.entry(task_id), self.tasks[task_id]
        agent = self.reviewers(task_id)[e.setdefault("review_stage", 0)]
        prompt = (f"Review the changes on this branch for task {task_id} — {task['title']}. "
                  f"The task spec is {task['spec']}. Follow your instructions and end with the JSON verdict only.")
        cmd = ["claude", "-p", prompt, "--agent", agent,
               "--permission-prompts", "none", "--output-format", "json",
               "--allowedTools", *REVIEW_TOOLS]
        if self.model:
            cmd += ["--model", self.model]
        with open(self.review_log(task_id), "w") as fh:
            proc = subprocess.Popen(cmd, cwd=e["worktree"], stdout=fh, stderr=subprocess.STDOUT,
                                    stdin=subprocess.DEVNULL, start_new_session=True)
        e.update(status="reviewing", pid=proc.pid, note=f"{agent} in progress")
        self.save()
        print(f"🔍 {task_id} {agent} started (pid {proc.pid})")

    def finish_review(self, task_id: str) -> None:
        e = self.entry(task_id)
        log = self.review_log(task_id)
        stage = e.get("review_stage", 0)
        verdict = self.read_verdict(log)
        if verdict is None and self.hit_limit(log):
            e.update(status="verified", not_before=time.time() + LIMIT_WAIT_SECONDS,
                     note="Claude usage limit reached during review — reviewing again later")
        elif verdict is None:
            e.update(status="failed", note=f"Review output unreadable, see {log.name}")
        else:
            report = log.with_suffix(".md")
            report.write_text(self.format_findings(verdict))
            blocking = [f for f in verdict.get("findings", []) if f.get("severity") in ("blocker", "major")]
            if verdict.get("approved") and not blocking and stage + 1 < len(self.reviewers(task_id)):
                e["review_stage"] = stage + 1
                self.start_review(task_id)
                return
            if verdict.get("approved") and not blocking:
                e.update(status="review", note="Verify + " + " + ".join(self.reviewers(task_id)) + " passed — `merge` when ready")
            else:
                e["review_stage"] = 0
                rounds = e.get("review_round", 0) + 1
                e["review_round"] = rounds
                if rounds <= REVIEW_ROUNDS:
                    self.save()
                    self.start(task_id, extra=REVIEW_FIX_PROMPT + report.read_text())
                    return
                e.update(status="failed", note=f"Review rejected after {REVIEW_ROUNDS} fix rounds, see {report.name}")
        self.save()
        print(f"■ {task_id}: {e['status']} — {e.get('note')}")

    @staticmethod
    def hit_limit(log: Path) -> bool:
        """True when the run's final result is an error caused by a usage or rate limit."""
        try:
            with open(log, "rb") as fh:
                fh.seek(max(0, log.stat().st_size - 16384))
                lines = fh.read().decode("utf-8", "replace").splitlines()
        except OSError:
            return False
        for line in reversed(lines):
            try:
                event = json.loads(line)
            except json.JSONDecodeError:
                continue
            if isinstance(event, dict) and event.get("type") == "result":
                failed = event.get("is_error") or event.get("subtype", "success") != "success"
                return bool(failed and LIMIT_PATTERN.search(str(event.get("result", ""))))
        return False

    @staticmethod
    def read_verdict(path: Path) -> dict | None:
        try:
            raw = json.loads(path.read_text())
        except (OSError, json.JSONDecodeError):
            return None
        if isinstance(raw, dict) and isinstance(raw.get("structured_output"), dict):
            return raw["structured_output"]
        text = raw.get("result", "") if isinstance(raw, dict) else ""
        start, end = text.find("{"), text.rfind("}")
        if start < 0 or end <= start:
            return None
        try:
            return json.loads(text[start:end + 1])
        except json.JSONDecodeError:
            return None

    @staticmethod
    def format_findings(verdict: dict) -> str:
        lines = [f"Review: {'approved' if verdict.get('approved') else 'changes requested'} — {verdict.get('summary', '')}", ""]
        for f in verdict.get("findings", []):
            where = f"{f.get('file', '?')}:{f.get('line', '?')}"
            lines.append(f"- [{f.get('severity')}] {where} — {f.get('problem')} → {f.get('fix')}")
        return "\n".join(lines) + "\n"

    @staticmethod
    def stop_gradle(wt: Path) -> None:
        if (wt / "gradlew").exists():
            subprocess.run(["./gradlew", "--stop"], cwd=wt, stdout=subprocess.DEVNULL,
                           stderr=subprocess.DEVNULL)

    def run(self, parallel: int, lanes: set[str] | None, only: set[str] | None, watch: bool,
            auto: bool = False, push: bool = False, ci: bool = False) -> None:
        def eligible(tid: str) -> bool:
            return (lanes is None or self.tasks[tid]["lane"] in lanes) and (only is None or tid in only)

        if auto:
            for e in self.state.values():
                if e.get("status") == "failed":
                    e.update(auto_retries=0, notified=False)
        if ci:
            self.sync_main()
        mode = ("merge after green CI" if ci else "auto-merge" + (" + push" if push else "")) if auto else "manual merge"
        print(f"Worktrees: {self.worktrees}   parallel={parallel}   {mode}   Ctrl+C stops the loop (agents keep running)")
        try:
            while True:
                for tid, e in list(self.state.items()):
                    if e.get("status") in ACTIVE and not self.alive(e.get("pid")):
                        if e["status"] == "running":
                            self.finish(tid)
                        else:
                            self.finish_review(tid)
                for tid, e in list(self.state.items()):
                    if e.get("status") == "verified" and e.get("not_before") and time.time() >= e["not_before"]:
                        e.pop("not_before")
                        self.start_review(tid)
                if auto:
                    self.auto_advance(eligible, push, ci)
                running = [t for t, e in self.state.items() if e.get("status") in ACTIVE]
                for tid in self.tasks:
                    if len(running) >= parallel:
                        break
                    if eligible(tid) and self.is_ready(tid):
                        self.start(tid)
                        running.append(tid)
                self.save()

                remaining = [t for t in self.tasks if eligible(t) and self.entry(t).get("status") != "merged"]
                if not remaining:
                    notify("All selected tasks are merged. 🎉")
                    return
                if not running and not watch:
                    waiting = [t for t in remaining if self.entry(t).get("status") in ("review", "blocked", "failed")]
                    print("Nothing running. Waiting on you for: " + (", ".join(waiting) or "dependencies"))
                    self.print_status()
                    return
                time.sleep(POLL_SECONDS)
        except KeyboardInterrupt:
            print("\nLoop stopped. Running agents continue; run `status` or `run` again later.")

    def auto_advance(self, eligible, push: bool, ci: bool = False) -> None:
        if ci:
            self.sync_main()
            self.ship_queue()
        for tid in self.tasks:
            if not eligible(tid):
                continue
            e = self.entry(tid)
            if e.get("status") == "review" and ci:
                self.open_pr(tid)
            elif e.get("status") == "ci":
                self.check_ci(tid)
            elif e.get("status") == "review":
                self.auto_merge(tid, push)
            elif e.get("status") == "failed" and not e.get("notified"):
                if e.get("auto_retries", 0) < AUTO_RETRIES:
                    print(f"↻ {tid} failed ({e.get('note')}) — retrying once from a fresh branch")
                    self.retry(tid, fresh=True, auto_retries=e.get("auto_retries", 0) + 1)
                else:
                    e["notified"] = True
                    notify(f"{tid} failed: {e.get('note')}")
            elif e.get("status") == "blocked" and not e.get("notified"):
                e["notified"] = True
                notify(f"{tid} is blocked — read BLOCKED.md in its worktree")
        self.save()

    def auto_merge(self, task_id: str, push: bool) -> None:
        try:
            self.merge(task_id, force=False)
        except MergeError as err:
            e = self.entry(task_id)
            if err.conflict:
                self.handle_conflict(task_id)
            elif e.get("note") != str(err):
                e["note"] = str(err)
                print(f"⏸ {task_id} waits: {err}")
            return
        if push:
            res = sh(["git", "push", "origin", MAIN_BRANCH], self.root, check=False)
            print("⇡ pushed" if res.returncode == 0 else f"⚠ push failed: {res.stdout.strip()}")

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
            raise MergeError(f"{task_id} is '{e.get('status')}', not 'review'. Use --force to merge anyway.")
        if sh(["git", "rev-parse", "--abbrev-ref", "HEAD"], self.root).stdout.strip() != MAIN_BRANCH:
            raise MergeError(f"Check out {MAIN_BRANCH} in {self.root} first.")
        if sh(["git", "status", "--porcelain", "--untracked-files=no"], self.root).stdout.strip():
            raise MergeError("Main checkout has uncommitted changes.")
        br = self.branch(task_id)
        subject, body = self.commit_message(task_id)
        res = sh(["git", "merge", "--squash", br], self.root, check=False)
        if res.returncode != 0:
            sh(["git", "merge", "--abort"], self.root, check=False)
            sh(["git", "reset", "--merge"], self.root, check=False)
            raise MergeError(f"Merge conflict. Resolve manually, or run `retry {task_id} --fresh` "
                             f"to let an agent redo it on top of {MAIN_BRANCH}.\n{res.stdout}", conflict=True)
        if sh(["git", "diff", "--cached", "--quiet"], self.root, check=False).returncode == 0:
            print(f"{task_id} made no file changes — nothing to commit, marking it merged.")
        else:
            msg = ["-m", subject] + (["-m", body] if body else [])
            sh(["git", "commit", "-q", *msg], self.root)
        self.remove_worktree(task_id)
        sh(["git", "branch", "-D", br], self.root, check=False)
        e.update(status="merged", merged_at=now(), note=None)
        self.save()
        print(f"✔ {task_id} merged into {MAIN_BRANCH}")

    def commit_message(self, task_id: str) -> tuple[str, str]:
        task, br = self.tasks[task_id], self.branch(task_id)
        log = sh(["git", "log", "--reverse", "--format=%s", f"{MAIN_BRANCH}..{br}"], self.root).stdout
        subjects = [x for x in log.split("\n") if x.strip()]
        kind = task.get("type", "feat")
        summary = self.fit_subject(kind, self.summary_from(subjects, task), task)
        details = [self.strip_prefix(x) for x in subjects if not x.startswith("Merge ")]
        details = [x for x in details if x and x != summary]
        return f"{kind}: {summary}", "\n".join(f"- {x}" for x in details)

    def gh(self, *args: str) -> subprocess.CompletedProcess:
        return sh(["gh", *args], self.root, check=False)

    def sync_main(self) -> None:
        sh(["git", "fetch", "-q", "origin", MAIN_BRANCH], self.root, check=False)
        on_main = sh(["git", "rev-parse", "--abbrev-ref", "HEAD"], self.root).stdout.strip() == MAIN_BRANCH
        clean = not sh(["git", "status", "--porcelain", "--untracked-files=no"], self.root).stdout.strip()
        if on_main and clean:
            sh(["git", "merge", "-q", "--ff-only", BASE_REF], self.root, check=False)
        else:
            sh(["git", "branch", "-f", MAIN_BRANCH, BASE_REF], self.root, check=False)

    def open_pr(self, task_id: str) -> None:
        e, br = self.entry(task_id), self.branch(task_id)
        subject, body = self.commit_message(task_id)
        push = sh(["git", "push", "-q", "--force", "origin", f"{br}:refs/heads/{br}"], self.root, check=False)
        if push.returncode != 0:
            e["note"] = f"Push failed, retrying: {push.stdout.strip()[-200:]}"
            return
        if self.gh("pr", "view", br, "--json", "number").returncode != 0:
            res = self.gh("pr", "create", "--base", MAIN_BRANCH, "--head", br, "--title", subject,
                          "--body", body or subject)
            if res.returncode != 0:
                e["note"] = f"Could not open the pull request, retrying: {res.stdout.strip()[-200:]}"
                return
        e.update(status="ci", ci_since=time.time(), notified=False, note="Pull request open — waiting for CI")
        print(f"⇡ {task_id} pushed to {br}, waiting for CI")

    def ci_checks(self, branch: str) -> list[dict]:
        res = self.gh("pr", "checks", branch, "--json", "name,bucket")
        try:
            checks = json.loads(res.stdout)
        except json.JSONDecodeError:
            return []
        return checks if isinstance(checks, list) else []

    def ci_failure_logs(self, branch: str) -> str:
        sha = sh(["git", "rev-parse", branch], self.root).stdout.strip()
        runs = self.gh("run", "list", "--branch", branch, "--limit", "20",
                       "--json", "databaseId,conclusion,headSha,workflowName")
        try:
            runs = json.loads(runs.stdout)
        except json.JSONDecodeError:
            runs = []
        parts = []
        for run in runs:
            if run.get("headSha") == sha and run.get("conclusion") in ("failure", "cancelled", "timed_out"):
                log = self.gh("run", "view", str(run["databaseId"]), "--log-failed").stdout.splitlines()
                parts.append(f"### {run.get('workflowName')}\n" + "\n".join(log[-150:]))
        text = "\n\n".join(parts)
        return text[-15000:] if text else "CI failed but its logs could not be fetched; run the CI steps locally."

    def check_ci(self, task_id: str) -> None:
        e, br = self.entry(task_id), self.branch(task_id)
        wt = Path(e["worktree"])
        if sh(["git", "merge-base", "--is-ancestor", BASE_REF, br], self.root, check=False).returncode != 0:
            res = sh(["git", "merge", "--no-edit", BASE_REF], wt, check=False)
            if res.returncode != 0:
                sh(["git", "merge", "--abort"], wt, check=False)
                self.handle_conflict(task_id)
                return
            self.open_pr(task_id)
            e["note"] = f"Updated with {MAIN_BRANCH} — CI running again"
            return
        checks = self.ci_checks(br)
        buckets = {c.get("bucket") for c in checks}
        if not checks or "pending" in buckets:
            if not checks and time.time() - e.get("ci_since", time.time()) > CI_NO_CHECKS_SECONDS and not e.get("notified"):
                e["notified"] = True
                notify(f"{task_id}: no CI checks reported on its pull request after 20 minutes")
            return
        if buckets & {"fail", "cancel"}:
            rounds = e.get("ci_round", 0) + 1
            e["ci_round"] = rounds
            if rounds <= CI_ROUNDS:
                print(f"✖ {task_id} CI failed — sending the logs to the agent (round {rounds})")
                self.start(task_id, extra=CI_FIX_PROMPT + self.ci_failure_logs(br))
            else:
                e.update(status="failed", note=f"CI still failing after {CI_ROUNDS} fix rounds", notified=True)
                notify(f"{task_id}: CI still failing after {CI_ROUNDS} fix rounds")
            return
        subject, body = self.commit_message(task_id)
        res = self.gh("pr", "merge", br, "--squash", "--subject", subject, "--body", body)
        if res.returncode != 0:
            if '"CONFLICTING"' in self.gh("pr", "view", br, "--json", "mergeable").stdout:
                self.handle_conflict(task_id)
            else:
                e["note"] = f"GitHub refused the merge, retrying: {res.stdout.strip()[-200:]}"
            return
        sh(["git", "push", "-q", "origin", "--delete", br], self.root, check=False)
        self.sync_main()
        self.remove_worktree(task_id)
        sh(["git", "branch", "-D", br], self.root, check=False)
        e.update(status="merged", merged_at=now(), note="Merged on GitHub after green CI")
        print(f"✔ {task_id} merged into {MAIN_BRANCH} after green CI")

    def handle_conflict(self, task_id: str) -> None:
        e = self.entry(task_id)
        if e.get("conflict_rounds", 0) < CONFLICT_ROUNDS:
            e["conflict_rounds"] = e.get("conflict_rounds", 0) + 1
            print(f"↻ {task_id} conflicts with {MAIN_BRANCH} — agent merges {MAIN_BRANCH} and resolves it")
            self.start(task_id, extra=CONFLICT_PROMPT)
        else:
            e.update(status="failed", note=f"Merge conflict after {CONFLICT_ROUNDS} resolve rounds", notified=True)
            notify(f"{task_id}: merge conflict persists — resolve it manually")

    def ship_queue(self) -> None:
        """Branches queued by hand (`ship`) go through the same pull request + CI gate."""
        path = self.state_dir / "ship-queue.json"
        if not path.exists():
            return
        queue = json.loads(path.read_text())
        for item in queue:
            br, status = item["branch"], item.get("status", "pending")
            if status == "pending":
                push = sh(["git", "push", "-q", "--force", "origin", f"{br}:refs/heads/{br}"], self.root, check=False)
                if push.returncode == 0 and (self.gh("pr", "view", br, "--json", "number").returncode == 0 or
                                             self.gh("pr", "create", "--base", MAIN_BRANCH, "--head", br, "--title",
                                                     item["title"], "--body", item.get("body") or item["title"]).returncode == 0):
                    item.update(status="ci", since=time.time())
                    print(f"⇡ {br} pushed, waiting for CI")
            elif status == "ci":
                if sh(["git", "merge-base", "--is-ancestor", BASE_REF, br], self.root, check=False).returncode != 0:
                    self.gh("pr", "update-branch", br)
                    sh(["git", "fetch", "-q", "origin", f"{br}:{br}"], self.root, check=False)
                    continue
                buckets = {c.get("bucket") for c in self.ci_checks(br)}
                if not buckets or "pending" in buckets:
                    continue
                if buckets & {"fail", "cancel"}:
                    item.update(status="failed")
                    notify(f"{br}: CI failed on its pull request")
                elif self.gh("pr", "merge", br, "--squash", "--subject", item["title"],
                             "--body", item.get("body", "")).returncode == 0:
                    sh(["git", "push", "-q", "origin", "--delete", br], self.root, check=False)
                    self.sync_main()
                    sh(["git", "branch", "-D", br], self.root, check=False)
                    item.update(status="merged")
                    print(f"✔ {br} merged into {MAIN_BRANCH} after green CI")
        path.write_text(json.dumps(queue, indent=2, ensure_ascii=False))

    @staticmethod
    def strip_prefix(subject: str) -> str:
        """Drop 'type(scope):' / 'TASK-ID:' prefixes and any task ids from a commit subject."""
        text = re.sub(r"^(\w+(\([^)]*\))?!?|[A-Z]+-\d+):\s*", "", subject).strip()
        text = re.sub(r"\s*[\[(]?\b[A-Z]{1,5}-\d{1,3}\b[\])]?", "", text).strip()
        return text

    @classmethod
    def summary_from(cls, subjects: list[str], task: dict) -> str:
        """Use the agent's first commit subject (prefixes and ids removed), else the task title."""
        for subj in subjects:
            text = cls.strip_prefix(subj)
            if text and not text.lower().startswith("merge"):
                return text[0].lower() + text[1:] if text[:2] != text[:2].upper() else text
        return task["title"][0].lower() + task["title"][1:]

    @staticmethod
    def fit_subject(kind: str, summary: str, task: dict, limit: int = 72) -> str:
        """Keep '<type>: <summary>' within the commit-msg hook's limit, falling back to the task title."""
        budget = limit - len(kind) - 2
        for text in (summary, task["title"][0].lower() + task["title"][1:]):
            text = text.rstrip(". ")
            if len(text) <= budget:
                return text
        return text[:budget].rsplit(" ", 1)[0]

    def retry(self, task_id: str, fresh: bool, auto_retries: int = 0) -> None:
        e = self.entry(task_id)
        if e.get("status") in ACTIVE:
            sys.exit(f"{task_id} is still running.")
        if fresh:
            self.remove_worktree(task_id)
            sh(["git", "branch", "-D", self.branch(task_id)], self.root, check=False)
        else:
            wt = self.worktree(task_id)
            if (wt / "BLOCKED.md").exists():
                print("Note: BLOCKED.md is still in the worktree; remove it once the blocker is solved.")
        keep = {} if fresh else {k: v for k, v in e.items() if k == "branch"}
        self.state[task_id] = {"status": "pending", **keep, **({"auto_retries": auto_retries} if auto_retries else {})}
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
        if e.get("status") in ACTIVE and pid:
            try:
                os.killpg(pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
            e.update(status="failed", note="Stopped by user")
            self.save()
            print(f"{task_id} stopped")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--model", help="Claude model alias for agents and reviewer, e.g. opus or sonnet")
    p.add_argument("--max-turns", type=int, help="Safety limit on agent turns")
    p.add_argument("--worktrees", type=Path, help="Directory for task worktrees (default: ../<repo>-worktrees)")
    sub = p.add_subparsers(dest="cmd", required=True)

    r = sub.add_parser("run", help="Start ready tasks and supervise them")
    r.add_argument("--parallel", type=int, default=2, help="Max agents at once (default 2; Gradle needs RAM)")
    r.add_argument("--lanes", help="Comma-separated lanes, e.g. core,be,android")
    r.add_argument("--only", help="Comma-separated task ids")
    r.add_argument("--watch", action="store_true", help="Keep running and pick up tasks as you merge")
    r.add_argument("--auto", action="store_true",
                   help="Merge tasks that pass verify + review automatically; retry failures once (implies --watch)")
    r.add_argument("--push", action="store_true", help="With --auto: git push main after every merge")
    r.add_argument("--ci", action="store_true",
                   help="With --auto: open a pull request per task and merge on GitHub only after CI is green")
    sp = sub.add_parser("ship", help="Queue a branch to merge through a pull request once CI is green")
    sp.add_argument("branch")
    sp.add_argument("--title", required=True, help="Squash commit subject, e.g. 'docs: update the roadmap'")
    sub.add_parser("setup-github", help="Protect main: pull requests only, squash merge, required 'CI passed' check")

    sub.add_parser("status", help="Show task states")
    lg = sub.add_parser("logs", help="Readable agent log")
    lg.add_argument("task")
    lg.add_argument("--raw", action="store_true")
    for name, helptext in [("verify", "Re-run the verify command in the task worktree"),
                           ("review", "Run the mandatory code review now and wait for it"),
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

    o.model, o.max_turns = a.model, a.max_turns
    if a.cmd == "run":
        if shutil.which("claude") is None:
            sys.exit("`claude` CLI not found. Install Claude Code and run `claude` once to sign in.")
        split = lambda s: {x.strip() for x in s.split(",") if x.strip()} if s else None
        if a.ci and o.gh("auth", "status").returncode != 0:
            sys.exit("GitHub CLI is not signed in. Run: brew install gh && gh auth login")
        o.run(a.parallel, split(a.lanes), split(a.only), a.watch or a.auto, a.auto, a.push, a.ci)
    elif a.cmd == "status":
        o.print_status()
    elif a.cmd == "logs":
        o.logs(task, a.raw)
    elif a.cmd == "verify":
        ok = o.run_verify(task)
        o.save()
        print(f"{task}: {'passed' if ok else 'failed'} — {o.entry(task).get('note')}")
    elif a.cmd == "review":
        o.entry(task)["review_stage"] = 0
        o.start_review(task)
        while o.entry(task).get("status") == "reviewing":
            while o.alive(o.entry(task).get("pid")):
                time.sleep(POLL_SECONDS)
            o.finish_review(task)
    elif a.cmd == "stop":
        o.stop(task)
    elif a.cmd == "merge":
        try:
            o.merge(task, a.force)
        except MergeError as err:
            sys.exit(str(err))
    elif a.cmd == "retry":
        o.retry(task, a.fresh)
    elif a.cmd == "ship":
        path = o.state_dir / "ship-queue.json"
        queue = json.loads(path.read_text()) if path.exists() else []
        queue.append({"branch": a.branch, "title": a.title, "status": "pending"})
        path.write_text(json.dumps(queue, indent=2, ensure_ascii=False))
        print(f"{a.branch} queued; autopilot opens its pull request and merges it after green CI")
    elif a.cmd == "setup-github":
        repo = o.gh("repo", "view", "--json", "nameWithOwner", "-q", ".nameWithOwner").stdout.strip()
        protection = json.dumps({"required_status_checks": {"strict": False, "checks": [{"context": "CI passed"}]},
                                 "enforce_admins": True, "required_pull_request_reviews": None, "restrictions": None})
        steps = [
            subprocess.run(["gh", "api", "-X", "PUT", f"repos/{repo}/branches/{MAIN_BRANCH}/protection", "--input", "-"],
                           input=protection, text=True, capture_output=True),
            subprocess.run(["gh", "api", "-X", "PATCH", f"repos/{repo}", "-F", "allow_squash_merge=true",
                            "-F", "allow_merge_commit=false", "-F", "allow_rebase_merge=false",
                            "-F", "delete_branch_on_merge=true"], text=True, capture_output=True),
        ]
        for step in steps:
            print(step.stdout.strip()[:300] or "ok", step.stderr.strip()[:300])
        print(f"{repo}: main now accepts only squash-merged pull requests with a green 'CI passed' check")
    elif a.cmd == "clean":
        o.remove_worktree(task)
        sh(["git", "branch", "-D", o.branch(task)], o.root, check=False)
        o.state.pop(task, None)
        o.save()
        print(f"{task} cleaned")


if __name__ == "__main__":
    main()
