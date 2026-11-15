package vn.phong.parental.data.lock

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import vn.phong.parental.service.ChildAccessibilityService
import vn.phong.parental.service.ChildDeviceAdminReceiver

/** Trạng thái các quyền máy con cần để khóa thật. */
object PermissionState {

    fun usage(context: Context) = UsageTracker.hasUsagePermission(context)

    fun overlay(context: Context) = Settings.canDrawOverlays(context)

    fun accessibility(context: Context): Boolean {
        val expected = ComponentName(context, ChildAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun admin(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(context, ChildDeviceAdminReceiver::class.java))
    }

    fun battery(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun notifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Ba quyền bắt buộc để khóa hoạt động. */
    fun requiredOk(context: Context) = usage(context) && overlay(context) && accessibility(context)

    /** Danh sách quyền đang thiếu, dạng chữ cho bố mẹ đọc ("" = đủ). */
    fun missingText(context: Context): String {
        val list = mutableListOf<String>()
        if (!usage(context)) list += "Truy cập dữ liệu sử dụng"
        if (!overlay(context)) list += "Hiển thị trên ứng dụng khác"
        if (!accessibility(context)) list += "Trợ năng"
        if (!admin(context)) list += "Quản trị thiết bị"
        return list.joinToString(", ")
    }
}
