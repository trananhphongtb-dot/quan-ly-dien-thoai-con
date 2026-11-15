package vn.phong.parental.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.ui.Lang
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.data.lock.EnforcementEngine
import vn.phong.parental.data.lock.Exempt
import vn.phong.parental.data.lock.PermissionState
import vn.phong.parental.data.lock.UsageTracker
import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.InstalledApp
import vn.phong.parental.model.PerAppLimit
import vn.phong.parental.ui.LockScreenActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service chạy nền trên máy con, kiểm tra mỗi 10 giây:
 * - Luật mới nhất từ Firestore (Firestore tự cache nên mất mạng vẫn dùng bản cuối).
 * - Phút đã dùng thật tính từ sự kiện UsageStats.
 * - Nếu máy bị khóa từ xa, hết tổng giờ (đã cộng giờ bố mẹ cho thêm), đang
 *   giờ ngủ, giờ học mà mở app không phải app học, hoặc app hết giờ riêng
 *   → ghi phạm vi khóa vào LocalStore và bật LockScreenActivity.
 * - Báo lên Firestore: app đang dùng, tổng phút, giờ mở/đóng máy, thống kê
 *   theo app, danh sách app cài, quyền đang thiếu.
 *
 * Nút Home vẫn thoát được, nhưng ChildAccessibilityService đọc cùng phạm vi
 * khóa và bật lại màn khóa ngay. Khóa cứng 100% cần Device Owner.
 */
class LockEnforcementService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val rulesRepo = RulesRepository()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var loopJob: Job? = null
    /** Báo cho vòng kiểm tra chạy ngay (khi luật/lệnh khóa vừa đổi), không đợi 10 giây. */
    private val kick = Channel<Unit>(Channel.CONFLATED)
    private var lastHeartbeatAt = 0L

    @Volatile private var generalLimit = GeneralLimit()
    @Volatile private var perAppLimits: List<PerAppLimit> = emptyList()
    @Volatile private var child: ChildDevice? = null

    private var lastReportedApp: String? = null
    private var lastReportedMinutes = -1
    private var lastReportAt = 0L
    private var lastUsageUploadAt = 0L
    private var lastAppsUploadAt = 0L
    private var lastPermMissing: String? = null
    private var lastTimes = ""

    override fun onCreate() {
        super.onCreate()
        Lang.init(applicationContext)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = LocalStore(this)
        val familyId = store.familyId
        val childId = store.childId
        if (familyId == null || childId == null || store.role != "CHILD") {
            stopSelf()
            return START_NOT_STICKY
        }
        if (loopJob == null) {
            rulesRepo.errorSink = { store.lastError = it }
            listeners += rulesRepo.listenGeneralLimit(familyId) { generalLimit = it; kick.trySend(Unit) }
            listeners += rulesRepo.listenPerAppLimits(familyId) { perAppLimits = it; kick.trySend(Unit) }
            listeners += rulesRepo.listenChild(familyId, childId) { doc ->
                if (doc == null) {
                    // Bố mẹ đã hủy ghép nối máy này.
                    store.lockScope = ""
                    store.clear()
                    stopSelf()
                } else {
                    child = doc
                    handleReply(store, doc)
                    store.lastSyncAt = System.currentTimeMillis()
                    kick.trySend(Unit)
                }
            }
            runCatching {
                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                    scope.launch { runCatching { rulesRepo.saveFcmToken(familyId, childId, token) } }
                }
            }
            loopJob = scope.launch {
                while (isActive) {
                    store.lastLoopAt = System.currentTimeMillis()
                    runCatching { enforceOnce(store, familyId, childId) }
                        .onFailure { store.lastError = "Vòng kiểm tra lỗi: ${it.javaClass.simpleName}: ${it.message}" }
                    withTimeoutOrNull(10_000) { kick.receive() }
                    delay(300)
                }
            }
        }
        return START_STICKY
    }

    /** Bố mẹ trả lời lời xin: báo cho con bằng thông báo và dòng chữ trên màn hình. */
    private fun handleReply(store: LocalStore, doc: ChildDevice) {
        if (doc.replyAtMillis <= 0L || doc.replyAtMillis <= store.lastReplySeen) return
        // Lần đầu mở app, bỏ qua phản hồi cũ để khỏi báo lại.
        if (store.lastReplySeen == 0L && System.currentTimeMillis() - doc.replyAtMillis > 120_000L) {
            store.lastReplySeen = doc.replyAtMillis
            return
        }
        store.lastReplySeen = doc.replyAtMillis
        val text = if (doc.replyKind == "APPROVED") {
            Lang.t(
                "Bố mẹ đã đồng ý, máy được mở ${doc.replyMinutes} phút",
                "Your parent approved: unlocked for ${doc.replyMinutes} min"
            )
        } else {
            Lang.t("Bố mẹ chưa đồng ý lúc này", "Your parent said not right now")
        }
        notifyReply(text)
        ChildAccessibilityService.instance?.showMessage(text)
    }

    private fun notifyReply(text: String) {
        runCatching {
            val channelId = "parental_reply"
            val nm = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(channelId, "Phản hồi của bố mẹ", NotificationManager.IMPORTANCE_HIGH)
                )
            }
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(this, channelId)
            } else {
                @Suppress("DEPRECATION") Notification.Builder(this)
            }
            nm.notify(
                4242,
                builder.setContentTitle(Lang.t("Phản hồi từ bố mẹ", "Reply from your parent"))
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun hhmm(millis: Long): String =
        if (millis <= 0L) "--:--" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    private fun labelOf(pkg: String): String = runCatching {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private suspend fun enforceOnce(store: LocalStore, familyId: String, childId: String) {
        // Quyền thiếu thì báo bố mẹ, nhưng vẫn khóa nếu còn đủ quyền để khóa.
        val missing = PermissionState.missingText(this)
        if (missing != lastPermMissing) {
            runCatching { rulesRepo.reportPermissions(familyId, childId, missing) }
            lastPermMissing = missing
        }

        val exempt = Exempt.packages(this)
        // Thiếu quyền Usage thì vẫn áp lệnh khóa từ xa / giờ ngủ, chỉ không có số liệu.
        val summary = if (PermissionState.usage(this)) {
            UsageTracker.summarizeToday(this)
        } else {
            UsageTracker.Summary(emptyMap(), emptyMap(), null, 0L, 0L, 0L)
        }
        val total = summary.totalMinutes(exempt)
        val fg = summary.foregroundPackage
        val doc = child
        val bonus = if (doc != null && doc.bonusDate == today()) doc.bonusMinutes else 0

        val block = EnforcementEngine.evaluate(
            packageName = fg,
            limits = perAppLimits,
            usedMinutesToday = summary.minutesByPackage,
            generalLimit = generalLimit,
            totalMinutesToday = total,
            exempt = exempt,
            remoteLocked = doc?.isLocked ?: false,
            remoteReason = doc?.lockReason ?: "",
            bonusMinutes = bonus,
            unlockUntilMillis = doc?.unlockUntilMillis ?: 0L
        )

        // Phạm vi khóa dùng chung cho màn khóa và dịch vụ trợ năng.
        // Đang hiện màn khóa của chính app (fg = app mình) thì giữ phạm vi
        // khóa theo app, tránh khóa xong lại tự mở khóa rồi khóa lại liên tục.
        val prevScope = store.lockScope
        store.lockScope = when {
            block == null ->
                if (fg == packageName && prevScope.startsWith("APP:")) prevScope else ""
            block.wholeDevice -> "ALL"
            else -> "APP:${block.packageName}"
        }
        if (block != null) store.lockReason = block.reason

        // Màn khóa do dịch vụ trợ năng vẽ (không bị Android chặn). Nếu trợ năng chưa
        // kết nối thì mới thử mở màn khóa bằng activity (có thể bị hệ thống chặn).
        val acc = ChildAccessibilityService.instance
        if (acc != null) {
            acc.refresh(fg)
        } else if (block != null) {
            store.lastError = "Dịch vụ Trợ năng chưa chạy, màn khóa có thể không hiện"
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(LockScreenActivity.EXTRA_REASON, block.reason)
            }
            runCatching { startActivity(intent) }
        }

        val now = System.currentTimeMillis()

        // Nhịp tim mỗi 60 giây để bố mẹ thấy máy con đang online.
        if (now - lastHeartbeatAt >= 60_000) {
            runCatching { rulesRepo.heartbeat(familyId, childId) }
                .onSuccess { store.lastSyncAt = now }
                .onFailure { store.lastError = "Không gửi được lên bố mẹ: ${it.message}" }
            lastHeartbeatAt = now
        }

        // Báo app đang dùng + tổng phút: khi có đổi, tối đa mỗi 30 giây.
        val shownApp = fg?.let { labelOf(it) } ?: "Màn hình tắt"
        val changed = shownApp != lastReportedApp || total != lastReportedMinutes
        if (changed && now - lastReportAt >= 30_000) {
            runCatching {
                rulesRepo.updateCurrentApp(
                    familyId, childId, shownApp,
                    hhmm(if (fg != null) summary.foregroundSinceMillis else now),
                    total, missing
                )
            }
            lastReportedApp = shownApp
            lastReportedMinutes = total
            lastReportAt = now
        }

        val times = hhmm(summary.firstScreenOnMillis) + "|" + hhmm(summary.lastScreenOffMillis)
        if (times != lastTimes) {
            runCatching {
                rulesRepo.updateDeviceTimes(
                    familyId, childId,
                    hhmm(summary.firstScreenOnMillis), hhmm(summary.lastScreenOffMillis)
                )
            }
            lastTimes = times
        }

        // Thống kê theo app mỗi 3 phút.
        if (now - lastUsageUploadAt >= 180_000) {
            val stats = summary.minutesByPackage
                .filter { it.value > 0 && it.key !in exempt }
                .entries.sortedByDescending { it.value }
                .take(40)
                .map {
                    AppUsageStat(
                        packageName = it.key,
                        appName = labelOf(it.key),
                        minutesToday = it.value,
                        opensToday = summary.opensByPackage[it.key] ?: 0
                    )
                }
            runCatching { rulesRepo.saveDailyUsage(familyId, childId, today(), stats) }
            lastUsageUploadAt = now
        }

        // Danh sách app cài để bố mẹ chọn khi thêm giới hạn, 6 giờ một lần.
        if (now - lastAppsUploadAt >= 6 * 3600_000L) {
            val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val apps = packageManager.queryIntentActivities(launch, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != packageName }
                .map { InstalledApp(it, labelOf(it)) }
            runCatching { rulesRepo.saveInstalledApps(familyId, childId, apps) }
            lastAppsUploadAt = now
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
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("Quản lý điện thoại con đang chạy")
            .setContentText("Đang theo dõi giờ dùng máy theo luật của bố mẹ")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        listeners.forEach { it.remove() }
        listeners.clear()
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
