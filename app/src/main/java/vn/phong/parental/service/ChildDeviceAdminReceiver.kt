package vn.phong.parental.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Quản trị thiết bị để chống gỡ app dễ dàng.
 * Khi admin đang bật, muốn gỡ app phải tắt admin trước, và app sẽ yêu cầu
 * mã PIN của bố mẹ trước khi cho tắt trong màn hình của mình.
 * Lưu ý: Android vẫn cho người dùng tắt admin trong Cài đặt nếu họ cố tình,
 * nhưng khi đó service sẽ báo về máy bố mẹ ở bước tiếp theo.
 */
class ChildDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Bước sau: gửi cảnh báo "máy con vừa tắt quyền quản trị" về Firestore.
    }
}
