package vn.phong.parental.data.lock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telecom.TelecomManager

/**
 * Các gói luôn được mở, kể cả khi máy khóa: chính app, trình quay số
 * (để còn gọi bố mẹ), màn hình chính (launcher) và giao diện hệ thống.
 */
object Exempt {
    fun packages(context: Context): Set<String> {
        val set = mutableSetOf(
            context.packageName,
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.systemui",
            "com.android.phone",
            "com.android.server.telecom"
        )
        runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set += it }
        }
        runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { set += it.activityInfo.packageName }
        }
        return set
    }

    /** Launcher thì chỉ miễn trừ khi lock theo từng app, không miễn khi khóa cả máy. */
    fun launchers(context: Context): Set<String> {
        val out = mutableSetOf<String>()
        runCatching {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { out += it.activityInfo.packageName }
        }
        return out
    }

    /** Các app gọi điện: khi đang mở thì màn khóa nhường chỗ để con gọi bố mẹ. */
    fun dialers(context: Context): Set<String> {
        val set = mutableSetOf(
            "com.android.dialer", "com.google.android.dialer",
            "com.android.incallui", "com.samsung.android.dialer",
            "com.samsung.android.incallui", "com.android.phone",
            "com.android.server.telecom"
        )
        runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { set += it }
        }
        return set
    }
}
