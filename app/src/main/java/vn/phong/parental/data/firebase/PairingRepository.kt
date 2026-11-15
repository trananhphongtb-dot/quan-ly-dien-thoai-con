package vn.phong.parental.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import vn.phong.parental.model.PairingStatus
import com.google.firebase.firestore.FieldValue
import java.security.SecureRandom

/**
 * Ghép nối thật qua Firestore, đúng luồng đã chốt:
 * bố mẹ tạo mã → con nhập mã → bố mẹ duyệt → mới nối xong.
 *
 * Mã 6 số là document id của collection pairingCodes, hiệu lực 10 phút.
 * Không đoán mã để qua mặt: rules chỉ cho đọc mã khi đã đăng nhập,
 * và bố mẹ vẫn phải duyệt thì trạng thái mới thành APPROVED.
 */
class PairingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun pairingDoc(code: String) =
        db.collection(FirebaseStore.PAIRING_CODES).document(code)

    private fun familyDoc(familyId: String) =
        db.collection(FirebaseStore.FAMILIES).document(familyId)

    /** Bố mẹ tạo gia đình lần đầu, trả về familyId. */
    suspend fun createFamily(ownerUid: String): String {
        val ref = familyDoc(db.collection(FirebaseStore.FAMILIES).document().id)
        ref.set(
            mapOf(
                "ownerUid" to ownerUid,
                "childUids" to emptyList<String>(),
                "createdAt" to Timestamp.now()
            )
        ).await()
        return ref.id
    }

    /** Bố mẹ tạo mã ghép nối mới, hiệu lực 10 phút. */
    suspend fun createPairingCode(familyId: String): PairingCodeDoc {
        val code = (100000 + SecureRandom().nextInt(900000)).toString()
        val expiresAt = Timestamp(
            System.currentTimeMillis() / 1000 +
                FirebaseStore.PAIRING_CODE_VALID_MINUTES * 60, 0
        )
        val doc = PairingCodeDoc(
            code = code,
            familyId = familyId,
            status = FirebaseStore.STATUS_PENDING,
            expiresAt = expiresAt
        )
        pairingDoc(code).set(
            mapOf(
                "familyId" to familyId,
                "status" to doc.status,
                "childName" to "",
                "childDeviceId" to "",
                "childUid" to "",
                "childId" to "",
                "expiresAt" to expiresAt,
                "createdAt" to Timestamp.now()
            )
        ).await()
        return doc
    }

    /**
     * Con nhập mã: chỉ chuyển PENDING → WAITING_APPROVAL khi mã còn hạn.
     * Trả về false khi mã sai, hết hạn hoặc đã có người khác nhận.
     */
    suspend fun claimPairingCode(
        code: String,
        childName: String,
        childDeviceId: String,
        childUid: String
    ): Boolean {
        val ref = pairingDoc(code)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            val doc = snap.toObject(PairingCodeDoc::class.java)?.copy(code = snap.id)
            if (doc == null || doc.status != FirebaseStore.STATUS_PENDING || doc.isExpired) {
                false
            } else {
                tx.update(
                    ref,
                    mapOf(
                        "status" to FirebaseStore.STATUS_WAITING_APPROVAL,
                        "childName" to childName,
                        "childDeviceId" to childDeviceId,
                        "childUid" to childUid
                    )
                )
                true
            }
        }.await()
    }

    /**
     * Bố mẹ duyệt: tạo document con trong gia đình, rồi mới đánh dấu
     * mã thành APPROVED. Máy con đang lắng nghe mã sẽ thấy và đi tiếp
     * tới màn cấp quyền.
     */
    suspend fun approvePairing(code: String): String {
        val snap = pairingDoc(code).get().await()
        val doc = snap.toObject(PairingCodeDoc::class.java)?.copy(code = snap.id)
            ?: throw IllegalStateException("Không tìm thấy mã ghép nối")
        if (doc.status != FirebaseStore.STATUS_WAITING_APPROVAL) {
            throw IllegalStateException("Mã chưa ở trạng thái chờ duyệt")
        }
        if (doc.isExpired) throw IllegalStateException("Mã đã hết hạn 10 phút")
        if (doc.childUid.isBlank()) throw IllegalStateException("Máy con chưa gửi định danh, nhờ con nhập lại mã")
        val childRef = familyDoc(doc.familyId)
            .collection(FirebaseStore.CHILDREN)
            .document()
        val batch = db.batch()
        batch.set(
            childRef,
            mapOf(
                "name" to doc.childName,
                "deviceId" to doc.childDeviceId,
                "childUid" to doc.childUid,
                "pairingStatus" to PairingStatus.PAIRED.name,
                "currentApp" to "Chưa có dữ liệu",
                "currentAppSince" to "--:--",
                "todayTotalMinutes" to 0,
                "firstUnlockToday" to "--:--",
                "lastLockToday" to "--:--",
                "isLocked" to false,
                "lockReason" to "",
                "fcmToken" to "",
                "unlockRequest" to "",
                "bonusMinutes" to 0,
                "bonusDate" to "",
                "permMissing" to "",
                "updatedAt" to Timestamp.now()
            )
        )
        batch.update(familyDoc(doc.familyId), "childUids", FieldValue.arrayUnion(doc.childUid))
        batch.update(
            pairingDoc(code),
            mapOf("status" to FirebaseStore.STATUS_APPROVED, "childId" to childRef.id)
        )
        batch.commit().await()
        return childRef.id
    }

    /** Lắng nghe realtime 1 mã ghép nối, để cả 2 máy cùng thấy trạng thái mới. */
    fun listenPairingCode(
        code: String,
        onChange: (PairingCodeDoc?) -> Unit
    ): ListenerRegistration {
        return pairingDoc(code).addSnapshotListener { snap, _ ->
            val doc = snap?.toObject(PairingCodeDoc::class.java)?.copy(code = snap.id)
            onChange(doc)
        }
    }

    /** Bố mẹ hủy ghép nối một máy con: xóa doc con và thu quyền uid của nó. */
    suspend fun removeChild(familyId: String, childId: String) {
        val childRef = familyDoc(familyId).collection(FirebaseStore.CHILDREN).document(childId)
        val uid = childRef.get().await().getString("childUid")
        if (uid != null) {
            familyDoc(familyId).update("childUids", FieldValue.arrayRemove(uid)).await()
        }
        childRef.delete().await()
    }
}
