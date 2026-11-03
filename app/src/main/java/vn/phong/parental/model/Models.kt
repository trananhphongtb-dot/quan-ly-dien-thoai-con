package vn.phong.parental.model

/** Vai trò chọn ngay lần mở app đầu tiên, sau khi ghép nối thì bị khóa. */
enum class Role { PARENT, CHILD }

/** Trạng thái ghép nối máy con với máy bố mẹ. */
enum class PairingStatus { NOT_PAIRED, WAITING_APPROVAL, PAIRED }

/** Một máy con trong nhà. */
data class ChildDevice(
    val id: String,
    val name: String,
    val pairingStatus: PairingStatus = PairingStatus.NOT_PAIRED,
    val currentApp: String = "Chưa có dữ liệu",
    val currentAppSince: String = "--:--",
    val todayTotalMinutes: Int = 0,
    val firstUnlockToday: String = "--:--",
    val lastLockToday: String = "--:--",
    val isLocked: Boolean = false,
    val lockReason: String = ""
)

/** Giới hạn chung — Tab 2. */
data class GeneralLimit(
    val dailyTotalMinutes: Int = 120,
    val bedtimeStart: String = "21:30",
    val bedtimeEnd: String = "06:30",
    val studyLockStart: String = "19:00",
    val studyLockEnd: String = "20:00"
)

/** Giới hạn từng app — Tab 3. */
data class PerAppLimit(
    val packageName: String,
    val appName: String,
    val dailyMinutes: Int,
    val usedMinutesToday: Int = 0,
    /** App bắt buộc, ví dụ app học: được mở trong khung giờ học. */
    val isMandatory: Boolean = false
) {
    val isOutOfTime: Boolean get() = usedMinutesToday >= dailyMinutes
}

/** Thống kê 1 app trong ngày — Tab 1. */
data class AppUsageStat(
    val appName: String,
    val minutesToday: Int,
    val opensToday: Int
)
