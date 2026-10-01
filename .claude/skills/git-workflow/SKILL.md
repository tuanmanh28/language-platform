---
name: git-workflow
description: Use when creating a branch, writing a commit message, splitting work into commits, merging, tagging a release or editing .gitignore - any git operation in this repo.
---

# Git workflow

One person, many agents, one `main`. History must read like a changelog: one commit per task on `main`, each with a
Conventional Commits subject and no planning noise.

References:
- Conventional Commits 1.0.0 — https://www.conventionalcommits.org/en/v1.0.0/
- Semantic Versioning 2.0.0 — https://semver.org/
- `scripts/agents/orchestrator.py` (`merge`) — how task branches are squashed into `main`.
- `.githooks/commit-msg` — enforces the rules below on every commit.

## Branches

| Branch | Use |
| --- | --- |
| `main` | Always releasable; only the orchestrator's squash merges or the owner's commits land here |
| `<type>/<slug>` | One per task: `feat/backend-config`, `fix/timer-restart`, `refactor/align-with-skills` |

- `<type>` is one of the commit types below; `<slug>` is kebab-case, 2–4 English words, describing the outcome.
- Never a task id, ticket number, date or person's name in a branch name.

## Commit messages

```
<type>: <summary>

<optional body: why the change was needed and what it affects>
```

| Type | When |
| --- | --- |
| `feat` | New user-visible capability or API |
| `fix` | Bug fix |
| `refactor` | Code change with no behaviour change |
| `update` | Dependency, tooling or content upgrade |
| `perf` | Faster or lighter, same behaviour |
| `test` | Tests only |
| `docs` | Documentation only |
| `build` | Gradle, Xcode project, Docker |
| `ci` | GitHub Actions |
| `chore` | Anything else that changes no production code |

- Summary: English, imperative, lowercase start, no trailing period, at most 72 characters for the whole subject line
  (`feat: add vocabulary review screen`, not `Added Vocabulary Review screen.`).
- No scope in parentheses. Add `!` after the type only for a breaking API or schema change, and explain it in the body.
- Body only when the *why* is not obvious; wrap at 72 columns; bullet list for several points.
- **Never** a task id (`BE-01`, `AND-03`), `WIP`, emoji, `Co-Authored-By`, "Generated with", or any AI attribution.

## Commits on a task branch

- Commit in small logical steps that each compile; the subject of the first commit becomes the summary on `main`, so
  make it describe the whole task.
- Never commit secrets (`local.properties`, `.env`, keys, service-account JSON), build output, IDE files or generated
  code; add new patterns to `.gitignore` when a tool creates files.
- Large binaries (audio, images over ~1 MB) do not go into git; they live in object storage and are referenced by URL.
- Agents never push, rebase, reset, amend pushed commits, change git config or bypass hooks (`--no-verify`).

## Merging

- `python3 scripts/agents/orchestrator.py merge <ID>` squashes the branch into one `main` commit
  `<type>: <summary>`, with the other commit subjects as the body, and deletes the branch and worktree.
- Conflicts: `retry <ID> --fresh` so the agent redoes the work on top of the current `main`.
- The owner pushes `main` manually after reviewing.

## Releases

- Tags `v<major>.<minor>.<patch>` on `main` (SemVer); app version name and code come from the tag.
- `feat` bumps minor, `fix`/`perf` bump patch, `!` bumps major once past `1.0.0`.

## Checklist

- [ ] Branch `<type>/<slug>`, no ids.
- [ ] Every subject passes `.githooks/commit-msg`.
- [ ] No secrets, generated files or large binaries staged (`git diff --cached --stat`).
