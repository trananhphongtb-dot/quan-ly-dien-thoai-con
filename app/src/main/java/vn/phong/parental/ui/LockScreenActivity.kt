package vn.phong.parental.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.RulesRepository

/**
 * Màn hình khóa thật bên máy con.
 * - Chặn Back khi còn đang khóa.
 * - Gọi bố mẹ: mở trình quay số (ACTION_DIAL) với số bố mẹ đã lưu.
 * - Đèn pin: CameraManager.
 * - SOS / Xin mở khóa: ghi yêu cầu lên Firestore cho bố mẹ thấy.
 * - Tự đóng khi LockEnforcementService xóa phạm vi khóa (hết khóa).
 */
class LockScreenActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REASON = "lock_reason"
    }

    private var flashlightOn = false
    private var reason by mutableStateOf("Máy đang bị khóa")
    private var status by mutableStateOf<String?>(null)
    private lateinit var store: LocalStore

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "lock_scope" && store.lockScope.isEmpty()) finish()
        if (key == "lock_reason") reason = store.lockReason.ifBlank { reason }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LocalStore(this)
        reason = intent.getStringExtra(EXTRA_REASON)
            ?: store.lockReason.ifBlank { "Máy đang bị khóa" }
        if (store.lockScope.isEmpty()) {
            // Mở nhầm khi không còn khóa.
            finish()
            return
        }
        getSharedPreferences("parental_local", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Không cho Back thoát màn khóa.
            }
        })

        setContent {
            MaterialTheme {
                LockScreen(
                    lockReason = reason,
                    status = status,
                    onCallParent = { dialParent() },
                    onToggleFlashlight = { toggleFlashlight() },
                    onSos = { send("SOS khẩn cấp từ máy con", sos = true) },
                    onRequestUnlock = { send("Con xin mở khóa / xin thêm giờ", sos = false) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_REASON)?.let { reason = it }
    }

    override fun onDestroy() {
        runCatching {
            getSharedPreferences("parental_local", Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(prefsListener)
        }
        super.onDestroy()
    }

    private fun dialParent() {
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

    private fun send(message: String, sos: Boolean) {
        val familyId = store.familyId
        val childId = store.childId
        if (familyId == null || childId == null) {
            status = "Chưa ghép nối, không gửi được"
            return
        }
        status = "Đang gửi cho bố mẹ..."
        CoroutineScope(Dispatchers.IO).launch {
            val ok = runCatching {
                RulesRepository().requestUnlock(familyId, childId, message, sos)
            }.isSuccess
            runOnUiThread {
                status = if (ok) "Đã gửi cho bố mẹ, chờ bố mẹ trả lời" else "Gửi lỗi, kiểm tra mạng rồi thử lại"
            }
        }
    }
}
