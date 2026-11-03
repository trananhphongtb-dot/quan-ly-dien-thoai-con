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
 */
object UsageTracker {

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun startOfTodayMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** Map packageName -> phút đã dùng hôm nay. */
    fun todayUsageMinutes(context: Context): Map<String, Int> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfTodayMillis(),
            now
        ) ?: return emptyMap()
        return stats
            .filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to (it.totalTimeInForeground / 60_000).toInt() }
    }

    fun totalMinutesToday(context: Context): Int {
        // Trừ chính app quản lý ra để không tính giờ của nó vào giờ chơi.
        return todayUsageMinutes(context)
            .filterKeys { it != context.packageName }
            .values.sum()
    }

    /** App đang ở foreground gần nhất, từ UsageEvents. */
    fun currentForegroundApp(context: Context): String? {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - 60_000, now)
        var lastApp: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastApp = event.packageName
            }
        }
        return lastApp
    }
}
