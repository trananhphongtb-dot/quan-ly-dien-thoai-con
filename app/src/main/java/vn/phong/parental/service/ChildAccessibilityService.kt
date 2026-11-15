package vn.phong.parental.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.lock.Exempt
import vn.phong.parental.ui.LockScreenActivity

/**
 * Trợ năng trên máy con: nghe sự kiện đổi cửa sổ.
 * LockEnforcementService ghi phạm vi khóa vào LocalStore.lockScope:
 * - "" : không khóa, service này không làm gì.
 * - "ALL": khóa cả máy, mọi cửa sổ trừ app chính / quay số / hệ thống đều
 *   bị phủ lại bằng màn khóa (kể cả launcher khi con bấm Home).
 * - "APP:<gói>": chỉ cửa sổ của đúng app đó bị phủ.
 * Nó không khóa cứng nút Home như Device Owner, và cần người dùng bật
 * quyền trợ năng cho app trong Cài đặt → Trợ năng.
 */
class ChildAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val scope = LocalStore(applicationContext).lockScope
        if (scope.isEmpty()) return
        val exempt = Exempt.packages(applicationContext)
        val mustLock = when {
            scope == "ALL" -> pkg !in (exempt - Exempt.launchers(applicationContext))
            scope.startsWith("APP:") -> pkg == scope.removePrefix("APP:") && pkg !in exempt
            else -> false
        }
        if (!mustLock) return
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Không có gì để dọn dẹp.
    }
}
