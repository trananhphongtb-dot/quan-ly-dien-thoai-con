package vn.phong.parental.ui

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import androidx.compose.material3.MaterialTheme
import vn.phong.parental.data.firebase.LocalStore

/**
 * Activity màn hình khóa thật bên máy con.
 * - Chặn nút Back để con không bấm Back thoát màn khóa.
 * - Nút Gọi: mở trình quay số với số bố mẹ đã lưu (ACTION_DIAL, không cần
 *   quyền gọi trực tiếp, người dùng vẫn bấm gọi trong trình quay số).
 * - Nút Đèn pin: bật/tắt bằng CameraManager.
 * - Nút SOS / Xin mở khóa: gửi yêu cầu về Firestore cho bố mẹ.
 * - Khi activity này mở, nó đặt cờ lock_requested để AccessibilityService
 *   bật lại màn khóa nếu con thoát bằng Home.
 */
class LockScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REASON = "lock_reason"
    }

    private var flashlightOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Máy đang bị khóa"
        getSharedPreferences("parental_local", MODE_PRIVATE)
            .edit().putBoolean("lock_requested", true).apply()

        // Chặn Back.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Cố tình không làm gì: không cho Back thoát màn khóa.
            }
        })

        setContent {
            MaterialTheme {
                LockScreen(
                    lockReason = reason,
                    onCallParent = { dialParent() },
                    onToggleFlashlight = { toggleFlashlight() },
                    onSos = { sendSos() },
                    onRequestUnlock = { requestUnlock() }
                )
            }
        }
    }

    override fun onDestroy() {
        // Chỉ xóa cờ khi activity bị hủy hẳn; service khóa sẽ đặt lại nếu còn bị khóa.
        super.onDestroy()
    }

    private fun dialParent() {
        val store = LocalStore(this)
        val phone = store.parentPhone
        val intent = if (!phone.isNullOrBlank()) {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        } else {
            Intent(Intent.ACTION_DIAL)
        }
        startActivity(intent)
    }

    private fun toggleFlashlight() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull() ?: return
        flashlightOn = !flashlightOn
        runCatching { manager.setTorchMode(cameraId, flashlightOn) }
    }

    private fun sendSos() {
        // SOS thực chất là 1 yêu cầu mở khóa khẩn cấp, kèm vị trí ở bước sau.
        requestUnlock("SOS từ máy con")
    }

    private fun requestUnlock(message: String = "Con xin mở khóa") {
        // ViewModel sẽ gọi RulesRepository.requestUnlock khi màn hình này
        // được mở từ luồng đã ghép nối. Ở Activity độc lập này, yêu cầu được
        // lưu cục bộ để service khóa gửi lên Firestore ở lần kiểm tra sau.
        getSharedPreferences("parental_local", MODE_PRIVATE)
            .edit().putString("pending_unlock_request", message).apply()
    }
}
