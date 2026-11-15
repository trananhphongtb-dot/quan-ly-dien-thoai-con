package vn.phong.parental.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Lưu cục bộ trên từng máy, để app nhớ vai trò và máy đã ghép nối nào
 * mà không cần đăng nhập lại. PIN chỉ lưu dạng hash, không lưu số gốc.
 * Bước sau có thể chuyển sang DataStore / EncryptedSharedPreferences.
 */
class LocalStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("parental_local", Context.MODE_PRIVATE)

    var role: String?
        get() = prefs.getString("role", null)
        set(value) = prefs.edit().putString("role", value).apply()

    var familyId: String?
        get() = prefs.getString("family_id", null)
        set(value) = prefs.edit().putString("family_id", value).apply()

    var childId: String?
        get() = prefs.getString("child_id", null)
        set(value) = prefs.edit().putString("child_id", value).apply()

    var parentUid: String?
        get() = prefs.getString("parent_uid", null)
        set(value) = prefs.edit().putString("parent_uid", value).apply()

    var fcmToken: String?
        get() = prefs.getString("fcm_token", null)
        set(value) = prefs.edit().putString("fcm_token", value).apply()

    /** Số điện thoại bố mẹ để màn khóa quay số tới, do bố mẹ nhập khi ghép nối. */
    var parentPhone: String?
        get() = prefs.getString("parent_phone", null)
        set(value) = prefs.edit().putString("parent_phone", value).apply()

    /** Phạm vi khóa hiện tại: "" = không khóa, "ALL" = cả máy, "APP:<package>" = một app. */
    var lockScope: String
        get() = prefs.getString("lock_scope", "") ?: ""
        set(value) = prefs.edit().putString("lock_scope", value).apply()

    /** Lý do khóa đang hiển thị trên màn khóa. */
    var lockReason: String
        get() = prefs.getString("lock_reason", "") ?: ""
        set(value) = prefs.edit().putString("lock_reason", value).apply()

    /** Mã ghép nối con đang chờ duyệt (để mở lại app vẫn tiếp tục chờ). */
    var pairingCode: String?
        get() = prefs.getString("pairing_code", null)
        set(value) = prefs.edit().putString("pairing_code", value).apply()

    /**
     * Bước hiện tại của máy con: WAITING (chờ bố mẹ duyệt), PERMISSIONS
     * (đang cấp quyền), ACTIVE (đã chạy giám sát).
     */
    var childStage: String?
        get() = prefs.getString("child_stage", null)
        set(value) = prefs.edit().putString("child_stage", value).apply()

    /** PIN bố mẹ: PBKDF2 có muối ngẫu nhiên, không lưu số gốc. */
    fun saveParentPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString("parent_pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("parent_pin_hash", Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP))
            .apply()
    }

    fun checkParentPin(pin: String): Boolean {
        val saltText = prefs.getString("parent_pin_salt", null) ?: return false
        val expected = prefs.getString("parent_pin_hash", null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val actual = Base64.encodeToString(hashPin(pin, salt), Base64.NO_WRAP)
        return java.security.MessageDigest.isEqual(expected.toByteArray(), actual.toByteArray())
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun clear() = prefs.edit().clear().apply()
}
