package vn.phong.parental.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.data.lock.Exempt

/**
 * Trợ năng trên máy con. Đây là nơi hiện màn hình khóa:
 * một lớp phủ toàn màn hình (TYPE_ACCESSIBILITY_OVERLAY) do chính dịch vụ trợ năng
 * vẽ ra, nên không bị Android chặn kiểu "app nền không được mở màn hình mới".
 *
 * LockEnforcementService quyết định có khóa không và ghi LocalStore.lockScope:
 * - "": không khóa, gỡ lớp phủ.
 * - "ALL": khóa cả máy, che mọi app trừ app gọi điện (để con gọi bố mẹ).
 * - "APP:<gói>": chỉ che đúng app đó.
 * Dịch vụ này gọi refresh() mỗi khi đổi cửa sổ và mỗi khi service kia yêu cầu.
 */
class ChildAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: ChildAccessibilityService? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var reasonView: TextView? = null
    private var statusView: TextView? = null
    private var torchOn = false
    private var lastPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Cửa sổ của chính app (kể cả lớp phủ khóa) không làm đổi trạng thái.
        if (pkg == packageName) return
        lastPackage = pkg
        refresh()
    }

    /** Hiện hoặc gỡ lớp phủ khóa theo phạm vi khóa hiện tại. Gọi từ luồng nào cũng được. */
    fun refresh(foreground: String? = null) {
        main.post {
            if (lastPackage == null && foreground != null && foreground != packageName) lastPackage = foreground
            applyLockState()
        }
    }

    private fun applyLockState() {
        val scope = LocalStore(applicationContext).lockScope
        if (scope.isEmpty()) {
            removeOverlay()
            return
        }
        val pkg = lastPackage
        val dialers = Exempt.dialers(applicationContext)
        val shouldCover = when {
            scope == "ALL" -> pkg == null || pkg !in dialers
            scope.startsWith("APP:") -> pkg != null && pkg == scope.removePrefix("APP:")
            else -> false
        }
        if (shouldCover) showOverlay() else removeOverlay()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun roundButton(text: String, bg: Int, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 16f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(bg)
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
            ).apply { topMargin = dp(12) }
        }

    private fun showOverlay() {
        val store = LocalStore(applicationContext)
        val reason = store.lockReason.ifBlank { "Máy đang bị khóa" }
        if (overlay != null) {
            reasonView?.text = reason
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#102A43"))
            setPadding(dp(28), dp(28), dp(28), dp(28))
            isClickable = true
        }
        root.addView(TextView(this).apply {
            text = "🔒"
            textSize = 56f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Máy đang bị khóa"
            setTextColor(Color.WHITE)
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(12))
        })
        val reasonText = TextView(this).apply {
            text = reason
            setTextColor(Color.parseColor("#D6E6F7"))
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(20))
        }
        reasonView = reasonText
        root.addView(reasonText)
        root.addView(roundButton("Gọi bố mẹ", Color.parseColor("#1B8A4B")) { dialParent() })
        root.addView(roundButton("Đèn pin", Color.parseColor("#2C4A6B")) { toggleTorch() })
        root.addView(roundButton("SOS - báo khẩn cho bố mẹ", Color.parseColor("#B3261E")) {
            send("SOS khẩn cấp từ máy con", true)
        })
        root.addView(roundButton("Xin mở khóa / xin thêm giờ", Color.parseColor("#2C4A6B")) {
            send("Con xin mở khóa / xin thêm giờ", false)
        })
        val status = TextView(this).apply {
            setTextColor(Color.parseColor("#FFD54F"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        statusView = status
        root.addView(status)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )
        runCatching {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(root, params)
            overlay = root
        }.onFailure {
            LocalStore(applicationContext).lastError = "Không hiện được màn khóa: ${it.message}"
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) }
        overlay = null
        reasonView = null
        statusView = null
    }

    private fun dialParent() {
        val phone = LocalStore(applicationContext).parentPhone
        val intent = if (!phone.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure {
            statusView?.text = "Không mở được trình quay số"
        }
    }

    private fun toggleTorch() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = manager.cameraIdList.firstOrNull() ?: return
        torchOn = !torchOn
        runCatching { manager.setTorchMode(id, torchOn) }
    }

    private fun send(message: String, sos: Boolean) {
        val store = LocalStore(applicationContext)
        val familyId = store.familyId
        val childId = store.childId
        if (familyId == null || childId == null) {
            statusView?.text = "Chưa ghép nối, không gửi được"
            return
        }
        statusView?.text = "Đang gửi cho bố mẹ..."
        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching { RulesRepository().requestUnlock(familyId, childId, message, sos) }
            main.post {
                statusView?.text = if (result.isSuccess) {
                    "Đã gửi cho bố mẹ, chờ bố mẹ trả lời"
                } else {
                    "Gửi lỗi: ${result.exceptionOrNull()?.message ?: "kiểm tra mạng"}"
                }
            }
        }
    }

    override fun onInterrupt() {
        // Không có gì để dọn dẹp.
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        main.post { removeOverlay() }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        instance = null
        removeOverlay()
        super.onDestroy()
    }
}
