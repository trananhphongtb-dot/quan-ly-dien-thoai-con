package vn.phong.parental.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PairingStatus
import vn.phong.parental.model.PerAppLimit

/**
 * Luật và thống kê thật qua Firestore.
 * Bố mẹ ghi luật → máy con lắng nghe realtime và chạy theo luật mới nhất.
 * Mất mạng: máy con giữ bản luật đã tải lần cuối trong LocalStore / Room
 * ở bước khóa thật, nên vẫn khóa được dù offline.
 */
class RulesRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun childDoc(familyId: String, childId: String) =
        db.collection(FirebaseStore.FAMILIES).document(familyId)
            .collection(FirebaseStore.CHILDREN).document(childId)

    private fun generalDoc(familyId: String) =
        db.collection(FirebaseStore.FAMILIES).document(familyId)
            .collection(FirebaseStore.RULES).document(FirebaseStore.GENERAL_RULE_DOC)

    private fun perAppCollection(familyId: String) =
        db.collection(FirebaseStore.FAMILIES).document(familyId)
            .collection(FirebaseStore.RULES)
            .document(FirebaseStore.GENERAL_RULE_DOC)
            .collection(FirebaseStore.PER_APP)

    // ---- Bố mẹ ghi luật ----

    suspend fun saveGeneralLimit(familyId: String, limit: GeneralLimit) {
        generalDoc(familyId).set(
            mapOf(
                "dailyTotalMinutes" to limit.dailyTotalMinutes,
                "bedtimeStart" to limit.bedtimeStart,
                "bedtimeEnd" to limit.bedtimeEnd,
                "studyLockStart" to limit.studyLockStart,
                "studyLockEnd" to limit.studyLockEnd,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun savePerAppLimit(familyId: String, limit: PerAppLimit) {
        perAppCollection(familyId).document(limit.packageName).set(
            mapOf(
                "appName" to limit.appName,
                "dailyMinutes" to limit.dailyMinutes,
                "isMandatory" to limit.isMandatory,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /** Lệnh khóa ngay Tab 4: ghi isLocked, máy con nhận realtime + FCM. */
    suspend fun setLocked(
        familyId: String,
        childId: String,
        locked: Boolean,
        reason: String = ""
    ) {
        childDoc(familyId, childId).update(
            mapOf(
                "isLocked" to locked,
                "lockReason" to reason,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    // ---- Máy con gửi lên ----

    suspend fun updateCurrentApp(
        familyId: String,
        childId: String,
        appName: String,
        since: String,
        todayTotalMinutes: Int
    ) {
        childDoc(familyId, childId).update(
            mapOf(
                "currentApp" to appName,
                "currentAppSince" to since,
                "todayTotalMinutes" to todayTotalMinutes,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun updateDeviceTimes(
        familyId: String,
        childId: String,
        firstUnlockToday: String,
        lastLockToday: String
    ) {
        childDoc(familyId, childId).update(
            mapOf(
                "firstUnlockToday" to firstUnlockToday,
                "lastLockToday" to lastLockToday,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun saveFcmToken(familyId: String, childId: String, token: String) {
        childDoc(familyId, childId).update("fcmToken", token).await()
    }

    /** Con bấm xin mở khóa / xin thêm giờ trên màn khóa. */
    suspend fun requestUnlock(familyId: String, childId: String, message: String) {
        childDoc(familyId, childId).update(
            mapOf(
                "unlockRequest" to message,
                "unlockRequestAt" to Timestamp.now()
            )
        ).await()
    }

    suspend fun saveDailyUsage(
        familyId: String,
        childId: String,
        date: String,
        stats: List<AppUsageStat>
    ) {
        val mapped = stats.map {
            mapOf(
                "appName" to it.appName,
                "minutes" to it.minutesToday,
                "opens" to it.opensToday
            )
        }
        childDoc(familyId, childId)
            .collection(FirebaseStore.USAGE)
            .document(date)
            .set(mapOf("stats" to mapped, "updatedAt" to Timestamp.now()))
            .await()
    }

    // ---- Lắng nghe realtime ----

    fun listenChild(
        familyId: String,
        childId: String,
        onChange: (ChildDevice?) -> Unit
    ): ListenerRegistration {
        return childDoc(familyId, childId).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) {
                onChange(null)
                return@addSnapshotListener
            }
            onChange(
                ChildDevice(
                    id = snap.id,
                    name = snap.getString("name") ?: "",
                    pairingStatus = PairingStatus.PAIRED,
                    currentApp = snap.getString("currentApp") ?: "Chưa có dữ liệu",
                    currentAppSince = snap.getString("currentAppSince") ?: "--:--",
                    todayTotalMinutes = snap.getLong("todayTotalMinutes")?.toInt() ?: 0,
                    firstUnlockToday = snap.getString("firstUnlockToday") ?: "--:--",
                    lastLockToday = snap.getString("lastLockToday") ?: "--:--",
                    isLocked = snap.getBoolean("isLocked") ?: false,
                    lockReason = snap.getString("lockReason") ?: ""
                )
            )
        }
    }

    fun listenGeneralLimit(
        familyId: String,
        onChange: (GeneralLimit) -> Unit
    ): ListenerRegistration {
        return generalDoc(familyId).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) return@addSnapshotListener
            onChange(
                GeneralLimit(
                    dailyTotalMinutes = snap.getLong("dailyTotalMinutes")?.toInt() ?: 120,
                    bedtimeStart = snap.getString("bedtimeStart") ?: "21:30",
                    bedtimeEnd = snap.getString("bedtimeEnd") ?: "06:30",
                    studyLockStart = snap.getString("studyLockStart") ?: "19:00",
                    studyLockEnd = snap.getString("studyLockEnd") ?: "20:00"
                )
            )
        }
    }

    fun listenPerAppLimits(
        familyId: String,
        onChange: (List<PerAppLimit>) -> Unit
    ): ListenerRegistration {
        return perAppCollection(familyId).addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map { doc ->
                PerAppLimit(
                    packageName = doc.id,
                    appName = doc.getString("appName") ?: doc.id,
                    dailyMinutes = doc.getLong("dailyMinutes")?.toInt() ?: 0,
                    isMandatory = doc.getBoolean("isMandatory") ?: false
                )
            } ?: emptyList()
            onChange(list)
        }
    }
}
