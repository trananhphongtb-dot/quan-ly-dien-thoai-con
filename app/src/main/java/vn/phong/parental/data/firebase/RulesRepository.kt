package vn.phong.parental.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await
import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.InstalledApp
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
    /** Nơi nhận lỗi từ các listener (ví dụ PERMISSION_DENIED), để hiện cho người dùng. */
    var errorSink: ((String) -> Unit)? = null

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
                // Bố mẹ khóa lại thì hủy luôn quyền mở khóa tạm đang có.
                "unlockUntil" to if (locked) 0L else FieldValue.delete(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Bố mẹ đồng ý lời xin: cho mở khóa tạm [minutes] phút, bỏ qua mọi luật giờ
     * (hết giờ, giờ ngủ, giờ học, hết giờ từng app), xóa lệnh khóa từ xa,
     * và ghi phản hồi để máy con báo cho con biết.
     */
    suspend fun grantPass(familyId: String, childId: String, minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        childDoc(familyId, childId).update(
            mapOf(
                "unlockUntil" to until,
                "isLocked" to false,
                "lockReason" to "",
                "unlockRequest" to "",
                "replyKind" to "APPROVED",
                "replyMinutes" to minutes,
                "replyAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /** Bố mẹ từ chối lời xin: xóa lời xin và báo lại cho con. */
    suspend fun denyRequest(familyId: String, childId: String) {
        childDoc(familyId, childId).update(
            mapOf(
                "unlockRequest" to "",
                "replyKind" to "DENIED",
                "replyMinutes" to 0,
                "replyAt" to Timestamp.now(),
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
        todayTotalMinutes: Int,
        permMissing: String = ""
    ) {
        childDoc(familyId, childId).update(
            mapOf(
                "currentApp" to appName,
                "currentAppSince" to since,
                "todayTotalMinutes" to todayTotalMinutes,
                "permMissing" to permMissing,
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

    /** Con bấm xin mở khóa / xin thêm giờ / SOS trên màn khóa. */
    suspend fun requestUnlock(
        familyId: String,
        childId: String,
        message: String,
        sos: Boolean = false
    ) {
        val data = mutableMapOf<String, Any>(
            "unlockRequest" to message,
            "unlockRequestAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        if (sos) data["sosAt"] = Timestamp.now()
        childDoc(familyId, childId).update(data).await()
    }

    /** Máy con báo "còn sống" để bố mẹ biết đang online. */
    suspend fun heartbeat(familyId: String, childId: String) {
        childDoc(familyId, childId).update("updatedAt", Timestamp.now()).await()
    }

    /** Máy con báo quyền bị tắt (Accessibility, Usage, Admin...). */
    suspend fun reportPermissions(familyId: String, childId: String, missing: String) {
        childDoc(familyId, childId).update(
            mapOf("permMissing" to missing, "updatedAt" to Timestamp.now())
        ).await()
    }

    suspend fun reportAdminDisabled(familyId: String, childId: String) {
        childDoc(familyId, childId).update(
            mapOf("adminDisabledAt" to Timestamp.now(), "updatedAt" to Timestamp.now())
        ).await()
    }

    // ---- Bố mẹ xử lý yêu cầu / cho thêm giờ ----

    suspend fun clearUnlockRequest(familyId: String, childId: String) {
        childDoc(familyId, childId).update(
            mapOf("unlockRequest" to "", "updatedAt" to Timestamp.now())
        ).await()
    }

    /** Cho thêm phút cho hôm nay; sang ngày mới thì bắt đầu lại từ số phút này. */
    suspend fun grantBonus(familyId: String, childId: String, minutes: Int, today: String) {
        val ref = childDoc(familyId, childId)
        db.runTransaction(Transaction.Function<Void?> { tx ->
            val snap = tx.get(ref)
            val sameDay = snap.getString("bonusDate") == today
            val old = if (sameDay) (snap.getLong("bonusMinutes")?.toInt() ?: 0) else 0
            tx.update(
                ref,
                mapOf(
                    "bonusMinutes" to old + minutes,
                    "bonusDate" to today,
                    "isLocked" to false,
                    "lockReason" to "",
                    "unlockRequest" to "",
                    "updatedAt" to Timestamp.now()
                )
            )
            null
        }).await()
    }

    suspend fun deletePerAppLimit(familyId: String, packageName: String) {
        perAppCollection(familyId).document(packageName).delete().await()
    }

    suspend fun saveInstalledApps(familyId: String, childId: String, apps: List<InstalledApp>) {
        childDoc(familyId, childId)
            .collection(FirebaseStore.META).document(FirebaseStore.APPS_DOC)
            .set(
                mapOf(
                    "apps" to apps.map { mapOf("pkg" to it.packageName, "name" to it.appName) },
                    "updatedAt" to Timestamp.now()
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
                "pkg" to it.packageName,
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
        return childDoc(familyId, childId).addSnapshotListener { snap, err ->
            if (err != null) {
                errorSink?.invoke("${err.code}: ${err.message}")
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            if (!snap.exists()) {
                // Bản cache chưa có doc không có nghĩa là đã bị xóa; chỉ tin bản từ máy chủ.
                if (!snap.metadata.isFromCache) onChange(null)
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
                    lockReason = snap.getString("lockReason") ?: "",
                    unlockRequest = snap.getString("unlockRequest") ?: "",
                    unlockRequestAtMillis = snap.getTimestamp("unlockRequestAt")?.toDate()?.time ?: 0L,
                    bonusMinutes = snap.getLong("bonusMinutes")?.toInt() ?: 0,
                    bonusDate = snap.getString("bonusDate") ?: "",
                    permMissing = snap.getString("permMissing") ?: "",
                    updatedAtMillis = snap.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                    unlockUntilMillis = snap.getLong("unlockUntil") ?: 0L,
                    replyKind = snap.getString("replyKind") ?: "",
                    replyMinutes = snap.getLong("replyMinutes")?.toInt() ?: 0,
                    replyAtMillis = snap.getTimestamp("replyAt")?.toDate()?.time ?: 0L
                )
            )
        }
    }

    fun listenGeneralLimit(
        familyId: String,
        onChange: (GeneralLimit) -> Unit
    ): ListenerRegistration {
        return generalDoc(familyId).addSnapshotListener { snap, err ->
            if (err != null) {
                errorSink?.invoke("${err.code}: ${err.message}")
                return@addSnapshotListener
            }
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
        return perAppCollection(familyId).addSnapshotListener { snap, err ->
            if (err != null) {
                errorSink?.invoke("${err.code}: ${err.message}")
                return@addSnapshotListener
            }
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

    fun listenUsage(
        familyId: String,
        childId: String,
        date: String,
        onChange: (List<AppUsageStat>) -> Unit
    ): ListenerRegistration {
        return childDoc(familyId, childId).collection(FirebaseStore.USAGE).document(date)
            .addSnapshotListener { snap, err ->
            if (err != null) {
                errorSink?.invoke("${err.code}: ${err.message}")
                return@addSnapshotListener
            }
                if (snap == null) return@addSnapshotListener
                val raw = snap.get("stats") as? List<*> ?: emptyList<Any>()
                onChange(
                    raw.mapNotNull { item ->
                        val m = item as? Map<*, *> ?: return@mapNotNull null
                        AppUsageStat(
                            packageName = m["pkg"] as? String ?: "",
                            appName = m["appName"] as? String ?: "",
                            minutesToday = (m["minutes"] as? Number)?.toInt() ?: 0,
                            opensToday = (m["opens"] as? Number)?.toInt() ?: 0
                        )
                    }
                )
            }
    }

    fun listenInstalledApps(
        familyId: String,
        childId: String,
        onChange: (List<InstalledApp>) -> Unit
    ): ListenerRegistration {
        return childDoc(familyId, childId).collection(FirebaseStore.META)
            .document(FirebaseStore.APPS_DOC)
            .addSnapshotListener { snap, err ->
            if (err != null) {
                errorSink?.invoke("${err.code}: ${err.message}")
                return@addSnapshotListener
            }
                if (snap == null) return@addSnapshotListener
                val raw = snap.get("apps") as? List<*> ?: emptyList<Any>()
                onChange(
                    raw.mapNotNull { item ->
                        val m = item as? Map<*, *> ?: return@mapNotNull null
                        val pkg = m["pkg"] as? String ?: return@mapNotNull null
                        InstalledApp(pkg, m["name"] as? String ?: pkg)
                    }.sortedBy { it.appName.lowercase() }
                )
            }
    }
}
