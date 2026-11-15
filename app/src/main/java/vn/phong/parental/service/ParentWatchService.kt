package vn.phong.parental.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.google.firebase.firestore.ListenerRegistration
import vn.phong.parental.MainActivity
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.ParentAuth
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.ui.Lang

/**
 * Chạy nền trên máy bố mẹ để báo ngay khi con bấm SOS hoặc xin mở khóa,
 * kể cả khi bố mẹ đã thoát khỏi app. SOS: thông báo + âm báo (không rung).
 * Lời xin mở khóa: chỉ thông báo, không kêu.
 */
class ParentWatchService : Service() {
    private var registration: ListenerRegistration? = null
    private val ringed = mutableMapOf<String, Long>()
    private val requested = mutableMapOf<String, Long>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Lang.init(applicationContext)
        startAsForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val store = LocalStore(this)
        val familyId = store.familyId
        if (store.role != "PARENT" || familyId == null || !ParentAuth.isSignedIn) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (registration == null) {
            registration = RulesRepository().listenChildren(familyId) { list ->
                list.forEach { handle(store, it) }
            }
        }
        return START_STICKY
    }

    private fun handle(store: LocalStore, c: ChildDevice) {
        val now = System.currentTimeMillis()
        val name = c.name.ifBlank { Lang.t("Máy con", "Child") }

        if (c.sosAtMillis > store.sosDismissed(c.id) && c.sosAtMillis > (ringed[c.id] ?: 0L)) {
            ringed[c.id] = c.sosAtMillis
            if (now - c.sosAtMillis < 10 * 60_000L) {
                notify(
                    id = c.id.hashCode(), channel = "parental_sos", importance = NotificationManager.IMPORTANCE_HIGH,
                    channelName = Lang.t("Báo SOS từ con", "SOS from child"),
                    title = Lang.t("SOS từ $name!", "SOS from $name!"),
                    text = Lang.t("Con đang cần giúp, mở app để xem.", "Your child needs help, open the app.")
                )
                SosAlert.start(this)
            }
        }
        if (c.unlockRequest.isNotBlank() && c.unlockRequestAtMillis > (requested[c.id] ?: 0L)) {
            requested[c.id] = c.unlockRequestAtMillis
            if (now - c.unlockRequestAtMillis < 10 * 60_000L && c.sosAtMillis < c.unlockRequestAtMillis - 2000) {
                notify(
                    id = c.id.hashCode() + 1, channel = "parental_request", importance = NotificationManager.IMPORTANCE_DEFAULT,
                    channelName = Lang.t("Con xin mở khóa", "Unlock requests"),
                    title = Lang.t("$name đang xin mở khóa", "$name asks to unlock"),
                    text = c.unlockRequest
                )
            }
        }
    }

    private fun builder(channel: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, channel)
        else @Suppress("DEPRECATION") Notification.Builder(this)

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun notify(id: Int, channel: String, importance: Int, channelName: String, title: String, text: String) {
        runCatching {
            val nm = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val ch = NotificationChannel(channel, channelName, importance)
                // Âm thanh SOS do SosAlert phát; kênh im lặng để khỏi kêu hai lần, không rung.
                ch.setSound(null, null)
                ch.enableVibration(false)
                nm.createNotificationChannel(ch)
            }
            nm.notify(
                id,
                builder(channel).setContentTitle(title).setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentIntent(openAppIntent())
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    private fun startAsForeground() {
        val channel = "parental_watch"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channel, Lang.t("Theo dõi SOS", "SOS watch"), NotificationManager.IMPORTANCE_MIN)
            )
        }
        val n = builder(channel)
            .setContentTitle(Lang.t("Đang theo dõi báo SOS của con", "Watching for SOS from your child"))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2, n)
        }
    }

    override fun onDestroy() {
        registration?.remove()
        registration = null
        super.onDestroy()
    }
}
