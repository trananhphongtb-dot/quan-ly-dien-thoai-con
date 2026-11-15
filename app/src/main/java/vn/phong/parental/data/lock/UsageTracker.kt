package vn.phong.parental.data.lock

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/**
 * Đọc thời gian dùng app thật bằng UsageStatsManager.
 * Cần người dùng cấp quyền "Truy cập dữ liệu sử dụng" trong Cài đặt,
 * app không tự cấp được quyền này.
 *
 * Giờ dùng tính từ chuỗi sự kiện (mở app / thoát app / tắt màn hình) từ 0h
 * tới giờ, chính xác hơn queryUsageStats và không bị trễ theo "xô" thời gian.
 */
object UsageTracker {

    private const val SCREEN_NON_INTERACTIVE = 16
    private const val KEYGUARD_SHOWN = 17

    /** Kết quả một lần tính. */
    data class Summary(
        val minutesByPackage: Map<String, Int>,
        val opensByPackage: Map<String, Int>,
        /** App đang ở foreground (null nếu màn hình tắt / chưa rõ). */
        val foregroundPackage: String?,
        /** Thời điểm app đó bắt đầu được dùng (ms). */
        val foregroundSinceMillis: Long,
        /** Lần mở màn hình đầu tiên và lần tắt gần nhất trong ngày (ms, 0 nếu chưa có). */
        val firstScreenOnMillis: Long,
        val lastScreenOffMillis: Long
    ) {
        /** Tổng phút của mọi app, trừ các gói được miễn (chính app, dialer, launcher...). */
        fun totalMinutes(excluded: Set<String>): Int =
            minutesByPackage.filterKeys { it !in excluded }.values.sum()
    }

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun summarizeToday(context: Context): Summary {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(startOfTodayMillis(), now)
        val ms = mutableMapOf<String, Long>()
        val opens = mutableMapOf<String, Int>()
        val event = UsageEvents.Event()
        var currentPkg: String? = null
        var currentSince = 0L
        var firstOn = 0L
        var lastOff = 0L

        fun closeCurrent(at: Long) {
            val p = currentPkg ?: return
            if (at > currentSince) ms[p] = (ms[p] ?: 0L) + (at - currentSince)
            currentPkg = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val t = event.timeStamp
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (currentPkg != event.packageName) {
                        closeCurrent(t)
                        currentPkg = event.packageName
                        currentSince = t
                        opens[event.packageName] = (opens[event.packageName] ?: 0) + 1
                    }
                    if (firstOn == 0L) firstOn = t
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (currentPkg == event.packageName) closeCurrent(t)
                }
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN -> {
                    closeCurrent(t)
                    lastOff = t
                }
            }
        }
        val fg = currentPkg
        val since = currentSince
        closeCurrent(now)
        return Summary(
            minutesByPackage = ms.mapValues { (it.value / 60_000L).toInt() },
            opensByPackage = opens,
            foregroundPackage = fg,
            foregroundSinceMillis = since,
            firstScreenOnMillis = firstOn,
            lastScreenOffMillis = lastOff
        )
    }
}
