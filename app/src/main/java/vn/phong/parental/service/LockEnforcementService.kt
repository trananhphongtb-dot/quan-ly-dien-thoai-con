package vn.phong.parental.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.data.lock.EnforcementEngine
import vn.phong.parental.data.lock.UsageTracker
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PerAppLimit
import vn.phong.parental.ui.LockScreenActivity

/**
 * Service chạy nền trên máy con, kiểm tra mỗi 30 giây:
 * - Đọc luật mới nhất từ Firestore (đã cache lần cuối nếu mất mạng).
 * - Đọc phút đã dùng thật từ UsageStats, app đang mở thật.
 * - Nếu máy bị khóa từ xa, hết tổng giờ, đang giờ ngủ, đang giờ học
 *   mà mở app không phải app học, hoặc app đó hết giờ riêng → bật
 *   LockScreenActivity phủ toàn màn hình.
 * - Gửi app đang dùng + tổng phút về Firestore để Tab 1 bên bố mẹ thấy.
 *
 * Service này không tự khóa cứng nút Home. Nút Home trên Android thường
 * vẫn thoát được, nhưng ChildAccessibilityService sẽ phát hiện cửa sổ
 * mới và bật lại màn khóa ngay. Muốn khóa cứng 100% phải dùng Device Owner
 * (máy công ty), không phải app cài thường trong nhà.
 */
class LockEnforcementService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val rulesRepo = RulesRepository()
    private val listeners = mutableListOf<ListenerRegistration>()

    private var generalLimit = GeneralLimit()
    private var perAppLimits: List<PerAppLimit> = emptyList()
    private var remoteLocked = false
    private var remoteReason = ""
    private var lastReportedApp: String? = null
    private var lastReportedMinutes = -1

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        val store = LocalStore(this)
        val familyId = store.familyId
        val childId = store.childId
        if (familyId != null && childId != null) {
            listeners += rulesRepo.listenGeneralLimit(familyId) { generalLimit = it }
            listeners += rulesRepo.listenPerAppLimits(familyId) { perAppLimits = it }
            listeners += rulesRepo.listenChild(familyId, childId) {
                remoteLocked = it?.isLocked ?: false
                remoteReason = it?.lockReason ?: ""
            }
        }
        scope.launch {
            while (isActive) {
                runCatching { enforceOnce() }
                delay(30_000)
            }
        }
    }

    private suspend fun enforceOnce() {
        if (!UsageTracker.hasUsagePermission(this)) return
        val store = LocalStore(this)
        val familyId = store.familyId ?: return
        val childId = store.childId ?: return

        val used = UsageTracker.todayUsageMinutes(this)
        val total = UsageTracker.totalMinutesToday(this)
        val currentApp = UsageTracker.currentForegroundApp(this)

        // Gửi thống kê về cho bố mẹ, chỉ gửi khi có thay đổi để đỡ tốn pin.
        if (currentApp != null && (currentApp != lastReportedApp || total != lastReportedMinutes)) {
            val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            runCatching {
                rulesRepo.updateCurrentApp(familyId, childId, currentApp, now, total)
            }
            lastReportedApp = currentApp
            lastReportedMinutes = total
        }

        val reason = if (remoteLocked) {
            remoteReason.ifBlank { "Bố mẹ đã khóa máy" }
        } else if (currentApp != null) {
            EnforcementEngine.blockReason(
                packageName = currentApp,
                limits = perAppLimits,
                usedMinutesToday = used,
                generalLimit = generalLimit,
                totalMinutesToday = total,
                ownPackageName = packageName
            )
        } else {
            null
        }

        if (reason != null) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(LockScreenActivity.EXTRA_REASON, reason)
            }
            startActivity(intent)
        }
    }

    private fun startForegroundNotification() {
        val channelId = "parental_lock"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Quản lý điện thoại con",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Quản lý điện thoại con đang chạy")
            .setContentText("Đang theo dõi giờ dùng máy theo luật của bố mẹ")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        listeners.forEach { it.remove() }
        listeners.clear()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
