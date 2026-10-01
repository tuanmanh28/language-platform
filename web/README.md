# Web (Phase 3)

The web app will be built with **Next.js (React + TypeScript)** to get SSR/SEO for the landing page and the IELTS blog.

- Talks to the same backend (`/api/v1/...`); the API client is generated from the backend's OpenAPI spec.
- Listening/Reading scoring on the web uses `POST /api/v1/reading/tests/{id}/submit`
  (the same `core/exam-engine` code as the apps), so results match mobile/desktop.

Not initialised in Phase 1.
