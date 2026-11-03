package vn.phong.parental.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Khởi động lại service khóa sau khi máy con reboot,
 * để luật không bị mất chỉ vì con tắt máy rồi mở lại.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences("parental_local", Context.MODE_PRIVATE)
        if (prefs.getString("role", null) != "CHILD") return
        val serviceIntent = Intent(context, LockEnforcementService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
