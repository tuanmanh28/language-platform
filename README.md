# Language Platform

Nền tảng luyện IELTS chạy native trên **Android, iOS, macOS, Windows** (và Web ở Phase 3).
Logic dùng chung viết một lần bằng **Kotlin Multiplatform (KMP)**; UI viết native theo từng hệ.

| Nền tảng | UI | Thư mục |
| --- | --- | --- |
| Android | Jetpack Compose | `app-android/` + `ui-compose/` |
| Windows (và Linux/macOS qua JVM) | Compose Desktop | `app-desktop/` + `ui-compose/` |
| iOS + macOS | SwiftUI | `app-apple/` |
| Web | Next.js — Phase 3 | `web/` |
| Backend | Ktor (Kotlin), Docker | `backend/` |

Trạng thái: **khung Phase 1** — một lát cắt dọc hoàn chỉnh cho Reading:
danh sách đề → làm bài có đếm giờ → chấm điểm + band ước tính, chạy được cả khi offline.

## Cấu trúc

```
language-platform/
├── core/
│   ├── model/          # KMP: model đề thi + kết quả (dùng chung app & backend), nhúng đề mẫu lúc build
│   └── exam-engine/    # KMP: chấm điểm, chuẩn hoá đáp án, quy đổi band (có unit test)
├── shared/             # KMP: network (Ktor), cache (SQLDelight), repository, ViewModel, Koin
│                       #      → xuất framework "Shared" cho Swift qua SKIE
├── ui-compose/         # Màn hình Compose dùng chung Android + Desktop
├── app-android/        # Entry Android
├── app-desktop/        # Entry Desktop → đóng gói .msi/.exe cho Windows
├── app-apple/          # SwiftUI iOS + macOS; project Xcode sinh từ project.yml (XcodeGen)
├── backend/            # Ktor API + Dockerfile
├── content/
│   ├── reading/        # Đề Reading dạng JSON (nguồn sự thật duy nhất cho đề mẫu)
│   └── schema/         # JSON Schema của đề
├── web/                # Phase 3
└── docker-compose.yml  # API + PostgreSQL cho dev local
```

Luồng dữ liệu: UI chỉ render `state` (StateFlow) của ViewModel trong `shared` và gọi hàm hành động
(`answer`, `submit`…). Repository thử API trước, lỗi thì dùng cache SQLite, cuối cùng dùng đề nhúng sẵn.

## Yêu cầu

- Android Studio (bản mới, hỗ trợ AGP 9) + Android SDK
- JDK 17+ (Gradle tự tải JDK 17 qua foojay nếu cần)
- Xcode 26 + `brew install xcodegen` (cho iOS/macOS)
- Docker (cho backend local)

> Mở thư mục gốc bằng Android Studio một lần để nó tạo `local.properties` (đường dẫn Android SDK).
> Chạy Gradle từ terminal mà chưa có file này thì cần đặt biến `ANDROID_HOME`.

## Chạy

### 1. Backend

```bash
./gradlew :backend:run                    # chạy trực tiếp, http://localhost:8080/health
# hoặc bằng Docker:
./gradlew :backend:shadowJar && docker compose up --build
```

API Phase 1:

| Method | Path | Mô tả |
| --- | --- | --- |
| GET | `/health` | Kiểm tra sống |
| GET | `/api/v1/reading/tests` | Danh sách đề |
| GET | `/api/v1/reading/tests/{id}` | Chi tiết đề |
| POST | `/api/v1/reading/tests/{id}/submit` | Chấm bài (`{"answers": {"q1": "TRUE", ...}}`) |

Không chạy backend thì app vẫn dùng được đề nhúng sẵn (hiện nhãn offline).

### 2. Android

Chạy cấu hình `app-android` trong Android Studio, hoặc:

```bash
./gradlew :app-android:installDebug
```

Emulator gọi backend qua `http://10.0.2.2:8080` (đặt trong `app-android/build.gradle.kts`).

### 3. Windows / Desktop

```bash
./gradlew :app-desktop:run
./gradlew :app-desktop:packageMsi   # chạy trên Windows để ra bộ cài .msi
```

### 4. iOS / macOS

```bash
cd app-apple
xcodegen                     # sinh LanguagePlatform.xcodeproj từ project.yml
open LanguagePlatform.xcodeproj
```

Chọn scheme `LanguagePlatformiOS` (Simulator) hoặc `LanguagePlatformMac`, rồi Run. Build phase của Xcode tự gọi
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` để build framework Kotlin.
Chạy trên máy thật cần điền Team trong *Signing & Capabilities*.

### Test

```bash
./gradlew :core:model:jvmTest :core:exam-engine:jvmTest :shared:jvmTest :backend:test
```

## Thêm đề mới

1. Tạo file JSON trong `content/reading/` theo `content/schema/reading-test.schema.json`
   (số thứ tự câu liên tục từ 1; đáp án MCQ phải là `key` của option).
2. Build lại: đề được nhúng vào app và backend; `BundledContentTest` kiểm tra tính hợp lệ.

Nội dung đề phải là **đề tự soạn hoặc có license** — không dùng đề trong sách Cambridge/đề thi thật.

## Ghi chú kỹ thuật

- Bảng quy đổi band trong `BandScale` là bảng tham khảo; UI luôn ghi "band ước tính".
- Phase 1 gửi đáp án về client để chấm offline. Khi có Premium, chuyển chấm sang server.
- Chỉ build `macosArm64` (Apple Silicon). Cần Mac Intel thì thêm target `macosX64`.
- Windows dùng Compose Desktop (JVM). PeopleInSpace có thêm hướng WinUI 3 thuần native gọi Kotlin/Native
  qua NuGet (xem `_reference/PeopleInSpace/windows/`) — còn thử nghiệm, cân nhắc sau.

## Tham khảo

Cấu hình KMP ban đầu học theo [PeopleInSpace](https://github.com/joreilly/PeopleInSpace) (Apache-2.0) — xem `NOTICE`.
Bản clone để đọc nằm ở `_reference/PeopleInSpace` (không thuộc repo, đã có trong `.gitignore`).
