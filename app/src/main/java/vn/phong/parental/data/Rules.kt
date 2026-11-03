package vn.phong.parental.data

import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PairingStatus
import vn.phong.parental.model.PerAppLimit

/**
 * Dữ liệu mẫu cho khung giao diện. Bước sau thay bằng:
 * - UsageStatsManager thật trên máy con cho Tab 1,
 * - Firestore realtime cho luật Tab 2, Tab 3 và lệnh khóa Tab 4.
 */
object Rules {
    val generalLimit = GeneralLimit(
        dailyTotalMinutes = 120,
        bedtimeStart = "21:30",
        bedtimeEnd = "06:30",
        studyLockStart = "19:00",
        studyLockEnd = "20:00"
    )

    val sampleChild = ChildDevice(
        id = "child-demo",
        name = "Máy của Bin",
        pairingStatus = PairingStatus.PAIRED,
        currentApp = "YouTube",
        currentAppSince = "17:42",
        todayTotalMinutes = 68,
        firstUnlockToday = "06:55",
        lastLockToday = "17:40",
        isLocked = false
    )

    val sampleStats = listOf(
        AppUsageStat("YouTube", 35, 6),
        AppUsageStat("Game", 22, 3),
        AppUsageStat("App học", 11, 2)
    )

    val samplePerAppLimits = listOf(
        PerAppLimit("com.example.game", "Game", dailyMinutes = 60, usedMinutesToday = 22),
        PerAppLimit("com.example.video", "Video", dailyMinutes = 30, usedMinutesToday = 35),
        PerAppLimit("com.example.hoc", "App học", dailyMinutes = 120, usedMinutesToday = 11, isMandatory = true)
    )
}
