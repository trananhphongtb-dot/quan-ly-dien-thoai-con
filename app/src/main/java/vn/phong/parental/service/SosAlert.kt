package vn.phong.parental.service

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import vn.phong.parental.data.firebase.LocalStore

/**
 * Phát âm báo SOS trên máy bố mẹ. Chỉ phát tiếng (không rung), đi theo kênh
 * báo thức nên vẫn kêu khi máy để chế độ im lặng thường. Tự tắt sau 30 giây
 * hoặc khi bố mẹ bấm "Đã biết".
 */
object SosAlert {
    private var ringtone: Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stop() }

    fun defaultUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    /** Tên dễ đọc của âm báo đã chọn. */
    fun titleOf(context: Context, uriText: String): String {
        val uri = if (uriText.isBlank()) defaultUri() else Uri.parse(uriText)
        return runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }
            .getOrNull() ?: "—"
    }

    /** Phát âm báo. [force] = true để bấm thử kể cả khi đang tắt chuông. */
    fun start(context: Context, force: Boolean = false) {
        val appContext = context.applicationContext
        val store = LocalStore(appContext)
        if (!force && !store.sosSoundOn) return
        handler.post {
            stopNow()
            val uri = if (store.sosSoundUri.isBlank()) defaultUri() else Uri.parse(store.sosSoundUri)
            val r = runCatching { RingtoneManager.getRingtone(appContext, uri) }.getOrNull() ?: return@post
            r.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            if (Build.VERSION.SDK_INT >= 28) r.isLooping = true
            runCatching { r.play() }
            ringtone = r
            handler.postDelayed(autoStop, 30_000)
        }
    }

    fun stop() {
        handler.post { stopNow() }
    }

    private fun stopNow() {
        handler.removeCallbacks(autoStop)
        runCatching { ringtone?.stop() }
        ringtone = null
    }
}
