package vn.phong.parental.data.firebase

import com.google.firebase.Timestamp

/**
 * Cấu trúc Firestore thật cho app quản lý điện thoại con.
 *
 * families/{familyId}
 *   ownerUid: String, createdAt: Timestamp
 * families/{familyId}/children/{childId}
 *   name, pairingStatus, currentApp, currentAppSince,
 *   todayTotalMinutes, firstUnlockToday, lastLockToday,
 *   isLocked, lockReason, fcmToken, updatedAt
 * families/{familyId}/rules/general
 *   dailyTotalMinutes, bedtimeStart, bedtimeEnd, studyLockStart, studyLockEnd
 * families/{familyId}/rules/perApp/{packageName}
 *   appName, dailyMinutes, isMandatory
 * families/{familyId}/children/{childId}/usage/{yyyy-MM-dd}
 *   stats: List<Map<String, Any>> (appName, minutes, opens)
 * pairingCodes/{6-digit-code}
 *   familyId, status, childName, childDeviceId, expiresAt, createdAt
 *
 * Quy tắc bảo mật xem file firestore.rules ở thư mục gốc project.
 */
object FirebaseStore {
    const val FAMILIES = "families"
    const val CHILDREN = "children"
    const val RULES = "rules"
    const val GENERAL_RULE_DOC = "general"
    const val PER_APP = "perApp"
    const val USAGE = "usage"
    const val PAIRING_CODES = "pairingCodes"

    const val STATUS_PENDING = "PENDING"
    const val STATUS_WAITING_APPROVAL = "WAITING_APPROVAL"
    const val STATUS_APPROVED = "APPROVED"
    const val STATUS_EXPIRED = "EXPIRED"

    const val PAIRING_CODE_VALID_MINUTES = 10L
}

data class PairingCodeDoc(
    val code: String = "",
    val familyId: String = "",
    val status: String = FirebaseStore.STATUS_PENDING,
    val childName: String = "",
    val childDeviceId: String = "",
    val expiresAt: Timestamp? = null
) {
    val isExpired: Boolean
        get() {
            val exp = expiresAt ?: return true
            return System.currentTimeMillis() > exp.toDate().time
        }
}
