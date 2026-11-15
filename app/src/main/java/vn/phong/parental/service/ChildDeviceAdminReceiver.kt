package vn.phong.parental.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.RulesRepository

/**
 * Quản trị thiết bị: khi đang bật, muốn gỡ app phải tắt quyền này trước.
 * Android vẫn cho tắt trong Cài đặt, nhưng khi đó máy con báo ngay về
 * bố mẹ (adminDisabledAt) để bố mẹ biết.
 */
class ChildDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        val store = LocalStore(context)
        val familyId = store.familyId ?: return
        val childId = store.childId ?: return
        if (store.role != "CHILD") return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { RulesRepository().reportAdminDisabled(familyId, childId) }
        }
    }
}
