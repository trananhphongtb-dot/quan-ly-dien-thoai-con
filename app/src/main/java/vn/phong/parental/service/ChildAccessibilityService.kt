package vn.phong.parental.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import vn.phong.parental.ui.LockScreenActivity

/**
 * Trợ năng trên máy con: nghe sự kiện đổi cửa sổ.
 * Khi con bấm Home hoặc Back để thoát màn khóa, service này thấy cửa sổ
 * mới không phải màn khóa và bật lại LockScreenActivity ngay.
 *
 * Đây là cách khóa thực tế nhất cho app cài thường trong nhà.
 * Nó không khóa cứng nút Home như Device Owner, và cần người dùng bật
 * quyền trợ năng cho app trong Cài đặt → Trợ năng.
 */
class ChildAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        // Không can thiệp vào chính app quản lý, trình quay số và màn khóa.
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.dialer" || packageName == "com.google.android.dialer") return
        // Chỉ bật lại màn khóa khi service khóa đang yêu cầu khóa.
        // Trạng thái khóa thật do LockEnforcementService giữ; ở đây service
        // này chỉ là lớp bật lại nhanh, còn quyết định khóa nằm ở service kia.
        // Để tránh bật khóa lung tung khi chưa bị khóa, lớp này không tự
        // bật màn khóa nếu không có cờ khóa — cờ này do LockScreenActivity
        // và LockEnforcementService đặt qua SharedPreferences.
        val prefs = getSharedPreferences("parental_local", MODE_PRIVATE)
        val lockRequested = prefs.getBoolean("lock_requested", false)
        if (!lockRequested) return
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(LockScreenActivity.EXTRA_REASON, "Máy đang bị khóa")
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Không có gì để dọn dẹp.
    }
}
