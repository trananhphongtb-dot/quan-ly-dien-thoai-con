package vn.phong.parental.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import vn.phong.parental.data.firebase.LocalStore
import java.util.Locale

/**
 * Ngôn ngữ giao diện (Việt / Anh). Lang.t("tiếng Việt", "English") trả về chuỗi
 * theo ngôn ngữ đang chọn. Vì đọc qua state của Compose nên đổi ngôn ngữ là
 * mọi màn hình tự vẽ lại ngay, không cần khởi động lại app.
 * Mặc định theo ngôn ngữ của máy: máy tiếng Việt thì Việt, còn lại là Anh.
 */
object Lang {
    var en by mutableStateOf(false)
        private set

    fun init(context: Context) {
        val saved = LocalStore(context.applicationContext).lang
        en = when (saved) {
            "en" -> true
            "vi" -> false
            else -> Locale.getDefault().language != "vi"
        }
    }

    fun set(context: Context, english: Boolean) {
        en = english
        LocalStore(context.applicationContext).lang = if (english) "en" else "vi"
    }

    fun t(vi: String, en: String): String = if (this.en) en else vi
}
