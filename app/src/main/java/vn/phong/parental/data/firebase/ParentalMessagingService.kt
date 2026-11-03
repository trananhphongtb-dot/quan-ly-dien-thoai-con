package vn.phong.parental.data.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Nhận lệnh khóa ngay qua FCM, song song với listener Firestore realtime.
 * FCM chỉ để tới nhanh; luật thật vẫn nằm ở Firestore, máy con không tin
 * mù vào 1 tin nhắn đẩy mà không kiểm tra lại trạng thái isLocked.
 *
 * Gửi lệnh khóa từ máy chủ / Cloud Functions ở bước sau: gửi data message
 * tới fcmToken của máy con, dạng { "command": "lock" | "unlock",
 * "familyId": "...", "childId": "..." }.
 * Bước này chưa có Cloud Functions gửi lệnh, xem FIREBASE_SETUP.md.
 */
class ParentalMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val store = LocalStore(applicationContext)
        store.fcmToken = token
        val familyId = store.familyId
        val childId = store.childId
        if (familyId != null && childId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    RulesRepository().saveFcmToken(familyId, childId, token)
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val command = message.data["command"] ?: return
        val familyId = message.data["familyId"] ?: return
        val childId = message.data["childId"] ?: return
        val store = LocalStore(applicationContext)
        // Chỉ nhận lệnh đúng máy con này, còn lại bỏ qua.
        if (store.familyId != familyId || store.childId != childId) return
        when (command) {
            "lock" -> {
                // Bước khóa thật: service khóa sẽ đọc lại isLocked từ
                // Firestore rồi mới bật LockScreen, không bật chỉ vì FCM.
                store.fcmToken?.let { /* giữ token để khỏi cảnh báo unused */ }
            }
            "unlock" -> {
                // Tương tự: kiểm tra lại Firestore rồi mới tắt LockScreen.
            }
        }
    }
}
