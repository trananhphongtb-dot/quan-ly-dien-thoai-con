package vn.phong.parental.data.firebase

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

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

    fun saveParentPin(pin: String) {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
        prefs.edit().putString("parent_pin_hash", hash).apply()
    }

    fun checkParentPin(pin: String): Boolean {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return prefs.getString("parent_pin_hash", null) == hash
    }

    fun clear() = prefs.edit().clear().apply()
}
