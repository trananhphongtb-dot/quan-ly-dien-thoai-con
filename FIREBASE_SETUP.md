# Firebase thật — đã chuẩn bị xong ngày 2026-11-03

Trạng thái console project quan-ly-dien-thoai-con:
- Auth: Email/Password và Anonymous đều đã bật.
- Firestore: đã tạo ở chế độ production, vị trí asia-southeast1.
- Rules: file firestore.rules đã dán và Publish thành công.
- Cloud Messaging: đã kiểm tra là dùng được, chưa gửi tin thử, chưa tạo Cloud Function.
- Gói Spark miễn phí, không bật thanh toán, Google Analytics vẫn tắt
  (nên một số tính năng đo lường và nhắm mục tiêu của FCM sẽ hạn chế).

# Phần code và bước còn lại

Muse không có nút nối Firebase sẵn như GitHub hay Drive, nên không tự tạo
project Firebase trong tài khoản Google của anh được. Code Firebase trong
project này đã là code thật, không còn là dữ liệu mẫu ở lớp này nữa:

Muse không có nút nối Firebase sẵn như GitHub hay Drive, nên không tự tạo
project Firebase trong tài khoản Google của anh được. Code Firebase trong
project này đã là code thật, không còn là dữ liệu mẫu ở lớp này nữa:

- `data/firebase/ParentAuth.kt` — bố mẹ đăng nhập email/mật khẩu,
  máy con đăng nhập ẩn danh để gửi yêu cầu ghép nối.
- `data/firebase/PairingRepository.kt` — tạo gia đình, tạo mã 6 số hết hạn
  10 phút, con nhận mã, bố mẹ duyệt mới tạo máy con và đánh dấu APPROVED.
- `data/firebase/RulesRepository.kt` — ghi và lắng nghe realtime luật chung,
  luật từng app, lệnh khóa Tab 4, thống kê và giờ mở/đóng máy.
- `data/firebase/ParentalMessagingService.kt` — nhận lệnh khóa nhanh qua FCM.
- `data/firebase/LocalStore.kt` — nhớ vai trò, familyId, childId, token.
- `firestore.rules` — chỉ chủ gia đình mới đọc/ghi luật của con mình.

Cấu trúc Firestore thật:

- `families/{familyId}` — ownerUid
- `families/{familyId}/children/{childId}` — trạng thái, app đang dùng,
  tổng phút, giờ mở/đóng, isLocked, lockReason, fcmToken
- `families/{familyId}/rules/general` — luật chung Tab 2
- `families/{familyId}/rules/general/perApp/{packageName}` — luật từng app Tab 3
- `families/{familyId}/children/{childId}/usage/{yyyy-MM-dd}` — thống kê Tab 1
- `pairingCodes/{mã 6 số}` — familyId, status, childName, expiresAt

## Bước duy nhất còn thiếu: google-services.json

Anh làm 1 trong 2 cách, không gửi mật khẩu qua chat:

**Cách 1 — anh tự tạo project (khoảng 5 phút):**

1. Vào console.firebase.google.com, đăng nhập tài khoản Google của anh.
2. Add project, đặt tên ví dụ `quan-ly-dien-thoai-con`.
3. Add app Android, package name đúng `vn.phong.parental`.
4. Tải `google-services.json`, đặt vào thư mục `app/` của project này.
5. Trong Firebase console bật: Authentication → Email/Password và
   Anonymous, Firestore Database, Cloud Messaging.
6. Vào Firestore → Rules, dán toàn bộ nội dung file `firestore.rules`
   rồi Publish. Nên test bằng Rules Playground trước khi cho con dùng.

**Cách 2 — để em mở console giúp anh:**

Anh bảo em mở trình duyệt tới Firebase console, anh tự đăng nhập và tạo
project như trên, em kiểm tra cùng anh xem đã bật đủ Auth, Firestore,
FCM và dán rules chưa. Em không nhập mật khẩu giúp anh.

## Sau khi có google-services.json

- Build mới chạy được, vì plugin google-services bắt buộc phải có file này.
- Nối tiếp ViewModel vào màn hình: `ParentPairingScreen` gọi
  `PairingRepository.createPairingCode`, `ChildPairingScreen` gọi
  `claimPairingCode`, `ParentHomeScreen` dùng `RulesRepository.listen*`.
  Lớp repository đã xong nên bước nối màn hình này là bước tiếp theo,
  không phải viết lại Firebase.
- Chưa có trong bước này: Cloud Functions tự gửi FCM khi bố mẹ khóa máy
  (hiện tại máy con nhận qua listener Firestore realtime là đủ, FCM chỉ
  để tới nhanh hơn), UsageStats thật và khóa máy thật bằng Accessibility.
  Đó là 2 bước sau Firebase.
