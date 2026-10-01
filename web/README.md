# Web (Phase 3)

Web sẽ làm bằng **Next.js (React + TypeScript)** để có SSR/SEO cho landing page và blog IELTS.

- Gọi chung backend (`/api/v1/...`); API client sinh tự động từ OpenAPI của backend.
- Chấm Listening/Reading trên web dùng endpoint `POST /api/v1/reading/tests/{id}/submit`
  (chạy cùng code `core/exam-engine` với app), nên kết quả khớp với mobile/desktop.

Chưa khởi tạo ở Phase 1.
