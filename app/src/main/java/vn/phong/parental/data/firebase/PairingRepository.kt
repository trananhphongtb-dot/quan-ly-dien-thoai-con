package vn.phong.parental.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import vn.phong.parental.model.PairingStatus
import kotlin.random.Random

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
                "createdAt" to Timestamp.now()
            )
        ).await()
        return ref.id
    }

    /** Bố mẹ tạo mã ghép nối mới, hiệu lực 10 phút. */
    suspend fun createPairingCode(familyId: String): PairingCodeDoc {
        val code = (100000 + Random.nextInt(900000)).toString()
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
        childDeviceId: String
    ): Boolean {
        val snap = pairingDoc(code).get().await()
        val doc = snap.toObject(PairingCodeDoc::class.java)?.copy(code = snap.id)
            ?: return false
        if (doc.status != FirebaseStore.STATUS_PENDING || doc.isExpired) return false
        pairingDoc(code).update(
            mapOf(
                "status" to FirebaseStore.STATUS_WAITING_APPROVAL,
                "childName" to childName,
                "childDeviceId" to childDeviceId
            )
        ).await()
        return true
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
        val childRef = familyDoc(doc.familyId)
            .collection(FirebaseStore.CHILDREN)
            .document()
        childRef.set(
            mapOf(
                "name" to doc.childName,
                "deviceId" to doc.childDeviceId,
                "pairingStatus" to PairingStatus.PAIRED.name,
                "currentApp" to "Chưa có dữ liệu",
                "currentAppSince" to "--:--",
                "todayTotalMinutes" to 0,
                "firstUnlockToday" to "--:--",
                "lastLockToday" to "--:--",
                "isLocked" to false,
                "lockReason" to "",
                "fcmToken" to "",
                "updatedAt" to Timestamp.now()
            )
        ).await()
        pairingDoc(code).update("status", FirebaseStore.STATUS_APPROVED).await()
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
}
