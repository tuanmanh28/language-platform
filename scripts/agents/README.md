# Parallel AI agents

`orchestrator.py` runs several Claude Code agents at once on tasks from [`docs/backlog`](../../docs/backlog/README.md).
Each task gets its own git worktree and a conventional branch (`feat/backend-config`, `fix/build-green`…),
so agents never step on each other.
The orchestrator checks every result itself (it re-runs the task's verify command) before asking you to review.

```
backlog ──► ready tasks ──► agent per worktree ──► verify ──► code-reviewer ──► you ──► merge ──► next tasks unblock
                                          ▲                          │ blocker/major findings
                                          └──────── fix round (max 2) ┘
```

## Requirements

- Claude Code CLI, recent version (`claude update`), signed in with your Claude subscription (`claude` once, then `/login`).
- Python 3 (ships with Xcode Command Line Tools), git, JDK 17+, Android SDK (`local.properties` is copied into each worktree).
- Node.js for AND-01 (design tokens). Docker for backend integration tests (Testcontainers).

## Daily use

```bash
cd ~/StudioProjects/language-platform

# See what's ready
python3 scripts/agents/orchestrator.py status

# Start agents (2 at a time is the safe default on 16 GB RAM) and keep supervising
python3 scripts/agents/orchestrator.py run --parallel 2 --lanes core,be,android --watch

# Follow one agent
python3 scripts/agents/orchestrator.py logs BE-01

# Review a finished task: open its worktree in Android Studio / your editor
open ../language-platform-worktrees/BE-01
git -C ../language-platform-worktrees/BE-01 log -p main..HEAD

# Accept it: squash-merges into main as ONE conventional commit (`feat: …`, no task id),
# removes the worktree; --watch then starts tasks that depended on it
python3 scripts/agents/orchestrator.py merge BE-01
git push
```

Not happy with a result?

```bash
python3 scripts/agents/orchestrator.py retry BE-01           # run again on the same branch (keeps its commits)
python3 scripts/agents/orchestrator.py retry BE-01 --fresh   # throw the branch away, start from main
python3 scripts/agents/orchestrator.py stop BE-01            # stop a running agent
python3 scripts/agents/orchestrator.py clean BE-01           # remove worktree + branch + state
```

You can also edit the task spec (`docs/backlog/BE-01.md`), commit it on `main`, then `retry --fresh`.

## States

| State | Meaning | Your move |
| --- | --- | --- |
| `pending` | Waiting for dependencies to be merged | — |
| `running` | Agent working (or fixing review findings) | `logs <ID>` |
| `reviewing` | Mandatory `code-reviewer` subagent checking the branch | wait |
| `review` | Verify and code review passed (minor notes in `.agents/logs/<ID>.review.md`) | Look, then `merge` |
| `failed` | No commit, uncommitted leftovers, verify failed, or review still rejecting after 2 fix rounds | Read `logs` / `.agents/logs/<ID>.verify.log`, then `retry` |
| `blocked` | Agent wrote `BLOCKED.md` (needs a secret, a decision…) | Unblock, then `retry` |
| `merged` | In `main` | — |

## Git hooks

The orchestrator sets `git config core.hooksPath .githooks` for this repo (worktrees share it). `.githooks/commit-msg`
rejects commits whose subject is not `<type>: <summary>`, that mention task ids or AI attribution, or that are made on
a branch not named `<type>/<slug>`. To use it without the orchestrator: `git config core.hooksPath .githooks`.

## Code review

After verify passes, `.claude/agents/code-reviewer.md` reviews `git diff main...HEAD` against `CLAUDE.md` and the skills
(read-only tools). `blocker`/`major` findings are sent back to the same agent to fix, up to 2 rounds. Run it manually with
`python3 scripts/agents/orchestrator.py review <ID>`.

## Safety

- Agents run headless with `--permission-mode acceptEdits`, a tool allowlist (Gradle, read-only/commit git commands,
  npm, xcodebuild, docker…) and a denylist (`git push`, `rebase`, `reset`, branch/config changes, `rm -rf`, `sudo`).
  Anything else is denied automatically (`--permission-prompts none`); edit `ALLOWED_TOOLS` in `orchestrator.py` to adjust.
- Agents never push. Nothing reaches GitHub until you `merge` and `git push`.
- Commits use this repo's git identity; `CLAUDE.md` forbids AI attribution lines.
- State and logs live in `.agents/` (gitignored). Worktrees live next to the repo in `../language-platform-worktrees/`.

## Tips

- RAM: every worktree runs its own Gradle daemon; the orchestrator stops it when a task ends. Close the emulator or
  lower `--parallel` to 1 when building Android while agents run.
- Usage limits: each agent run consumes your Claude plan's usage. If a run stops because of limits, `retry` it later.
- `--model opus` / `--model sonnet` (before the subcommand) to choose the model; `--max-turns 200` as a safety stop.
- Keep tasks small. A task that fails twice usually needs a clearer spec or splitting.
