package vn.phong.parental.data.lock

import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PerAppLimit
import java.util.Calendar

/**
 * Luật khóa thuần túy, không phụ thuộc Android hay Firebase,
 * để dễ test và dễ đọc: đầu vào là luật + thời gian + phút đã dùng,
 * đầu ra là có khóa hay không và vì sao.
 */
object EnforcementEngine {

    fun minutesOf(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) return 0
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        return hour * 60 + minute
    }

    /** Cửa sổ giờ có thể qua đêm, ví dụ ngủ 21:30–06:30. */
    fun isInWindow(start: String, end: String, nowMinutes: Int): Boolean {
        val s = minutesOf(start)
        val e = minutesOf(end)
        return if (s <= e) {
            nowMinutes in s until e
        } else {
            nowMinutes >= s || nowMinutes < e
        }
    }

    fun nowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /** Khóa toàn máy: hết tổng giờ, hoặc đang trong giờ ngủ. */
    fun shouldLockDevice(
        limit: GeneralLimit,
        totalMinutesToday: Int,
        nowMinutes: Int = nowMinutes()
    ): Boolean {
        if (totalMinutesToday >= limit.dailyTotalMinutes) return true
        if (isInWindow(limit.bedtimeStart, limit.bedtimeEnd, nowMinutes)) return true
        return false
    }

    /** Có đang trong khung giờ học bắt buộc không. */
    fun isStudyWindow(limit: GeneralLimit, nowMinutes: Int = nowMinutes()): Boolean {
        return isInWindow(limit.studyLockStart, limit.studyLockEnd, nowMinutes)
    }

    /** App này có được mở lúc này không. */
    fun isAppAllowed(
        packageName: String,
        limits: List<PerAppLimit>,
        usedMinutesToday: Map<String, Int>,
        generalLimit: GeneralLimit,
        ownPackageName: String,
        nowMinutes: Int = nowMinutes()
    ): Boolean {
        // Luôn cho mở chính app quản lý và trình quay số, để còn gọi bố mẹ.
        if (packageName == ownPackageName) return true
        if (packageName == "com.android.dialer" || packageName == "com.google.android.dialer") return true

        // Giờ học: chỉ mở app bắt buộc (app học).
        if (isStudyWindow(generalLimit, nowMinutes)) {
            val limit = limits.find { it.packageName == packageName }
            return limit?.isMandatory == true
        }

        // Ngoài giờ học: app nào hết giờ riêng thì khóa app đó.
        val limit = limits.find { it.packageName == packageName } ?: return true
        val used = usedMinutesToday[packageName] ?: 0
        return used < limit.dailyMinutes
    }

    fun blockReason(
        packageName: String,
        limits: List<PerAppLimit>,
        usedMinutesToday: Map<String, Int>,
        generalLimit: GeneralLimit,
        totalMinutesToday: Int,
        ownPackageName: String,
        nowMinutes: Int = nowMinutes()
    ): String? {
        if (shouldLockDevice(generalLimit, totalMinutesToday, nowMinutes)) {
            return "Hết giờ dùng máy hoặc đang trong giờ ngủ"
        }
        if (isStudyWindow(generalLimit, nowMinutes)) {
            return "Đang trong giờ học, chỉ mở được app học"
        }
        val limit = limits.find { it.packageName == packageName }
        if (limit != null) {
            val used = usedMinutesToday[packageName] ?: 0
            if (used >= limit.dailyMinutes) {
                return "App ${limit.appName} đã hết ${limit.dailyMinutes} phút hôm nay"
            }
        }
        if (!isAppAllowed(packageName, limits, usedMinutesToday, generalLimit, ownPackageName, nowMinutes)) {
            return "App này đang bị khóa"
        }
        return null
    }
}
