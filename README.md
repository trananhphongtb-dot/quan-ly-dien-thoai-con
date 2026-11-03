# App quản lý điện thoại của con — Khung app

Khung app Android (Kotlin + Jetpack Compose), 1 file APK dùng cho cả 2 vai trò.
Lớp Firebase đã viết thật (Auth, Firestore, FCM, ghép nối, luật realtime),
`google-services.json` đã có, và khóa máy thật bằng UsageStats /
Accessibility đã viết xong — chưa build APK để test trên máy thật.

## Luồng chính — chọn vai ngay khi mở app lần đầu

1. Mở app lần đầu → màn **Chọn vai trò**: `Bố Mẹ` hoặc `Con`. Chưa chọn thì không vào được trong.
2. Vai **Bố Mẹ**:
   - Đăng nhập (email / Google, sẽ nối Firebase Auth ở bước sau).
   - Đặt mã PIN quản lý.
   - Tạo mã ghép nối 6 số + QR, hiệu lực 10 phút, hết hạn thì tạo mã mới.
   - Mỗi con dùng 1 mã riêng, quản lý chung trong 1 app.
3. Vai **Con**:
   - Nhập mã 6 số hoặc quét QR.
   - Đặt tên máy con, ví dụ: Máy của Bin.
   - Gửi yêu cầu về máy bố mẹ, bố mẹ bấm **Duyệt** thì 2 máy mới nối xong.
   - Cấp quyền từng bước: xem thời gian dùng app, trợ năng, quản trị thiết bị,
     hiển thị trên app khác (màn hình khóa), vị trí, gọi điện, đèn pin.
4. Sau khi nối, vai trò bị khóa: máy con không tự đổi sang vai Bố Mẹ được.
   Muốn đổi phải nhập PIN của bố mẹ và được bố mẹ duyệt trên máy bố mẹ.
5. Mất mạng: máy con vẫn chạy theo luật đã tải về lần cuối.

## App bên bố mẹ — 4 tab (đã chốt ngày 2026-11-03)

- **Tab 1 — Theo dõi & Thống kê:** app đang dùng, dùng từ lúc nào, tổng giờ
  dùng máy, top app, mỗi app dùng bao lâu, mở bao nhiêu lần, giờ mở máy đầu
  tiên và giờ đóng máy cuối cùng trong ngày.
- **Tab 2 — Giới hạn chung:** tổng thời gian dùng máy mỗi ngày, giờ khóa máy
  (giờ ngủ, giờ học), hết tổng giờ thì khóa máy, chỉ chừa gọi cho bố mẹ.
- **Tab 3 — Giới hạn từng app:** đặt riêng từng app (game, TikTok...),
  hết giờ thì khóa app đó thôi; chọn app bắt buộc như app học và khung giờ
  chỉ mở được app đó.
- **Tab 4 — Khóa máy:** khóa ngay / mở khóa từ xa, hẹn giờ khóa, ghi lý do
  khóa hiện lên máy con, chọn sẵn số được phép gọi khi đang khóa.

## Màn hình khóa bên máy con — Tab 4

Chỉ hiện: đồng hồ + lý do bị khóa, nút **Gọi bố mẹ**, nút **Đèn pin**,
nút **SOS**, nút **Xin mở khóa / xin thêm giờ**. Không mở được app khác.

## Khóa máy thật — đã viết ngày 2026-11-03

- `data/lock/UsageTracker.kt` — đọc phút đã dùng thật từng app và app đang
  mở thật bằng UsageStatsManager, cần cấp quyền truy cập dữ liệu sử dụng.
- `data/lock/EnforcementEngine.kt` — luật khóa thuần túy: hết tổng giờ,
  giờ ngủ qua đêm, giờ học chỉ mở app bắt buộc, app hết giờ riêng.
- `service/LockEnforcementService.kt` — service nền kiểm tra mỗi 30 giây,
  lắng nghe luật Firestore, gửi app đang dùng về Tab 1, bật LockScreenActivity
  khi bị khóa, gửi thống kê về cho bố mẹ.
- `service/ChildAccessibilityService.kt` — nghe đổi cửa sổ, bật lại màn khóa
  khi con bấm Home/Back thoát màn khóa.
- `ui/LockScreenActivity.kt` — màn khóa thật: chặn Back, gọi bố mẹ bằng
  trình quay số, đèn pin bằng CameraManager, SOS/xin mở khóa gửi về Firestore.
- `service/ChildDeviceAdminReceiver.kt` + `service/BootReceiver.kt` —
  chống gỡ dễ dàng và khởi động lại service sau khi reboot.

Lưu ý kỹ thuật, nói thật với anh: máy Android thường không khóa cứng 100%
như máy công ty (Device Owner). Cách này dùng màn khóa phủ toàn màn hình +
trợ năng: con bấm thoát ra thì màn khóa bật lại ngay, đủ dùng trong nhà.
Muốn service không bị hệ thống tắt khi tiết kiệm pin, máy con cần tắt tối ưu
pin cho app này. Khi đăng lên Play Store, quyền QUERY_ALL_PACKAGES và trợ năng
sẽ bị Google xét kỹ — bản này làm để cài trực tiếp APK trong nhà trước.

## Đồng bộ — Firebase thật, xem `FIREBASE_SETUP.md`

- Bố mẹ đổi luật → ghi lên Firestore → máy con lắng nghe realtime.
- Lệnh khóa ngay → gửi thêm qua FCM để tới nhanh.
- Máy con gửi thống kê và vị trí về cho bố mẹ.
- Chưa có `google-services.json`, chưa có key hay mật khẩu trong repo này.
  Muse không có connector Firebase sẵn, nên bước tạo project và tải file
  này cần làm trong tài khoản Google của anh Phong, xem `FIREBASE_SETUP.md`.

## Cấu trúc code

- `app/src/main/java/vn/phong/parental/MainActivity.kt` — điều hướng theo vai trò.
- `ui/RoleSelectionScreen.kt` — chọn vai lần đầu.
- `ui/ParentPairingScreen.kt` — bố mẹ tạo mã ghép nối.
- `ui/ChildPairingScreen.kt` — con nhập mã / quét QR, chờ duyệt.
- `ui/ParentHomeScreen.kt` — 4 tab bên bố mẹ.
- `ui/LockScreen.kt` — màn hình khóa bên máy con.
- `model/Models.kt` — vai trò, trạng thái ghép nối, luật chung, luật từng app.
- `data/Pairing.kt` — sinh mã 6 số mẫu cho khung giao diện.
- `data/Rules.kt` — luật mẫu cho khung giao diện.
- `data/firebase/` — Firebase thật: ParentAuth, PairingRepository,
  RulesRepository, ParentalMessagingService, LocalStore.
- `ui/ParentalViewModel.kt` — nối màn hình vào Firebase: bố mẹ đăng nhập,
  tạo mã, duyệt mã; con nhận mã; 4 tab lắng nghe realtime; nút khóa Tab 4
  ghi isLocked lên Firestore. MainActivity tạo ViewModel này và truyền
  vào các màn hình.
- `firestore.rules` — rules Firestore, dán vào Firebase console.

## Build

- Cùng nền tảng với app Bé Tập Đếm của anh Phong: AGP 8.11.1, Kotlin 2.1.21,
  compileSdk / targetSdk 36, minSdk 24, Java 17.
- `google-services.json` đã có trong `app/`, ViewModel đã nối màn hình
  vào Firebase, Auth / Firestore / FCM và rules đã bật xong trong console.
  Còn lại: build APK để test trên 2 máy thật, rồi mới biết trên máy thật
  có máy nào tắt service hay chặn quyền gì không.

## Tính năng đã tạm bỏ

Đổi nhiệm vụ lấy giờ chơi và bảng thưởng tuần — tạm bỏ theo quyết định
ngày 2026-11-03, không có trong khung này.
