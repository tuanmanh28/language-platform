# Blocked: removing answer keys from GET breaks scoring in the apps

The backend part of this task is done and passes verify (see the commits on this branch). The code review blocked the
merge for a reason outside this task's scope.

## The conflict

The spec requires `GET /api/v1/{reading,listening}/tests/{id}` to return no answer keys and no explanations.
The apps still score locally:

- `shared/.../reading/data/OfflineFirstReadingRepository.getTest` uses the API copy whenever the call succeeds and caches
  it in SQLDelight (`cacheTest`); the offline lookups prefer the cache over `BundledReadingTests`.
- `shared/.../reading/ReadingSessionViewModel.kt` (`finish`) scores that copy with `ReadingScorer.score`.

Once this branch is merged, every online Reading attempt in the apps will be marked all wrong. After one online load,
the bundled sample will score all wrong offline too, because the cached copy without answer keys takes its place. No
backlog task moves app scoring to the submit endpoint (AND-09 only adds the review UI on top of `QuestionResult`).

## What I need (one of)

1. **Preferred:** a paired AND task that merges before or together with this branch. It would:
   - add `submit(id, answers)` to `ReadingApi`/`ReadingRepository`, calling `POST /api/v1/reading/tests/{id}/submit`;
   - make `ReadingSessionViewModel` score through the repository, and fall back to `ReadingScorer` only for tests
     that still have an answer key (bundled content);
   - stop a cached copy without answer keys from replacing a bundled test;
   - add a shared test showing that a test fetched from the API scores correctly.
2. Approval to widen this task's scope to `shared` so I can make that change here.
3. A decision to temporarily keep `acceptedAnswers` in GET payloads and remove only explanations. That drops the
   "no answers" half of the acceptance criterion until the apps score on the server.

When one of these is chosen, remove this file with `git rm BLOCKED.md`.
