package vn.phong.parental.data

import kotlin.random.Random

/**
 * Mã ghép nối khung app.
 *
 * Quy tắc đã chốt: bố mẹ tạo mã 6 số + QR, hiệu lực 10 phút.
 * Con nhập mã → trạng thái WAITING_APPROVAL → bố mẹ duyệt mới PAIRED.
 * Bước sau thay bằng mã tạo trên Firestore, kèm chữ ký để chống đoán mã.
 */
data class PairingCode(
    val code: String,
    val expiresAtMillis: Long,
    val isExpired: Boolean = false
)

object Pairing {
    const val CODE_VALID_MINUTES = 10L

    fun generateCode(nowMillis: Long = System.currentTimeMillis()): PairingCode {
        val code = (100000 + Random.nextInt(900000)).toString()
        return PairingCode(
            code = code,
            expiresAtMillis = nowMillis + CODE_VALID_MINUTES * 60 * 1000
        )
    }

    fun isValid(code: PairingCode, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return code.code.length == 6 && code.code.all { it.isDigit() } && nowMillis <= code.expiresAtMillis
    }
}
