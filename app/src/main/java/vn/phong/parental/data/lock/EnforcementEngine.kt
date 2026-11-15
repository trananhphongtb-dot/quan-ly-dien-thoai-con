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

    /** Kết quả kiểm tra: lý do khóa, và có khóa cả máy hay chỉ một app. */
    data class Block(val reason: String, val wholeDevice: Boolean, val packageName: String? = null)

    /**
     * Quyết định có khóa không. Thứ tự: khóa cả máy trước (từ xa, hết tổng
     * giờ, giờ ngủ), rồi mới tới luật theo app. Gói được miễn trừ (màn khóa,
     * trình quay số, launcher...) không bao giờ bị khóa theo app, còn khi
     * khóa cả máy thì chỉ màn khóa/dialer được qua (do nơi gọi xử lý).
     *
     * @param packageName app đang ở foreground, null nếu chưa biết.
     * @param exempt các gói luôn được mở khi chỉ khóa theo app.
     * @param bonusMinutes phút bố mẹ cho thêm hôm nay, cộng vào tổng giờ.
     */
    fun evaluate(
        packageName: String?,
        limits: List<PerAppLimit>,
        usedMinutesToday: Map<String, Int>,
        generalLimit: GeneralLimit,
        totalMinutesToday: Int,
        exempt: Set<String>,
        remoteLocked: Boolean,
        remoteReason: String,
        bonusMinutes: Int = 0,
        nowMinutes: Int = nowMinutes()
    ): Block? {
        if (remoteLocked) {
            return Block(remoteReason.ifBlank { "Bố mẹ đã khóa máy" }, true)
        }
        val effective = generalLimit.copy(
            dailyTotalMinutes = generalLimit.dailyTotalMinutes + bonusMinutes
        )
        if (totalMinutesToday >= effective.dailyTotalMinutes) {
            return Block("Hết giờ dùng máy hôm nay, mai dùng tiếp nhé", true)
        }
        if (isInWindow(effective.bedtimeStart, effective.bedtimeEnd, nowMinutes)) {
            return Block("Đang trong giờ ngủ, máy tạm khóa", true)
        }
        if (packageName == null || packageName in exempt) return null
        val limit = limits.find { it.packageName == packageName }
        if (isStudyWindow(effective, nowMinutes)) {
            if (limit?.isMandatory == true) return null
            return Block("Đang trong giờ học, chỉ mở được app học", false, packageName)
        }
        if (limit != null) {
            val used = usedMinutesToday[packageName] ?: 0
            if (used >= limit.dailyMinutes) {
                return Block("App ${limit.appName} đã hết ${limit.dailyMinutes} phút hôm nay", false, packageName)
            }
        }
        return null
    }
}
