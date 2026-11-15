package vn.phong.parental.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import vn.phong.parental.data.lock.PermissionState
import vn.phong.parental.service.ChildDeviceAdminReceiver


/** Một dòng quyền: tên, giải thích, trạng thái, nút mở cài đặt. */
@Composable
private fun PermissionRow(
    title: String,
    why: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit
) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ui.Text, modifier = Modifier.weight(1f))
            Text(
                if (granted) "Đã bật" else if (required) "Cần bật" else "Nên bật",
                color = if (granted) Ui.Green else if (required) Ui.Red else Ui.Amber,
                fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
        Text(why, color = Ui.Soft, fontSize = 13.sp)
        if (!granted) PrimaryButton("Mở cài đặt để bật", onGrant)
    }
}

/** Làm tươi trạng thái quyền mỗi khi quay lại app từ màn hình cài đặt. */
@Composable
fun rememberResumeTick(): Int {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val owner = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    return tick
}

fun openAdminRequest(context: Context) {
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(
            DevicePolicyManager.EXTRA_DEVICE_ADMIN,
            ComponentName(context, ChildDeviceAdminReceiver::class.java)
        )
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "Giúp app khó bị gỡ khi chưa có sự đồng ý của bố mẹ."
        )
    }
    context.startActivity(intent)
}

fun openSetting(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}

/** Bước cấp quyền trên máy con, sau khi bố mẹ đã duyệt. */
@Composable
fun ChildPermissionScreen(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // tick dùng làm khóa để tính lại trạng thái sau mỗi lần quay lại app.
    val usage = remember(tick) { PermissionState.usage(context) }
    val overlay = remember(tick) { PermissionState.overlay(context) }
    val access = remember(tick) { PermissionState.accessibility(context) }
    val admin = remember(tick) { PermissionState.admin(context) }
    val battery = remember(tick) { PermissionState.battery(context) }
    val notif = remember(tick) { PermissionState.notifications(context) }
    val phoneOk = phone.filter { it.isDigit() }.length >= 8
    val ready = usage && overlay && access && phoneOk

    ScreenScaffold("Cấp quyền cho máy con", "Bố mẹ đã duyệt. Bật các quyền dưới đây.") {
        PermissionRow("Truy cập dữ liệu sử dụng", "Để đo máy đang dùng app nào và bao lâu.", usage, true) {
            openSetting(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        PermissionRow("Hiển thị trên ứng dụng khác", "Để màn hình khóa hiện lên trên app đang mở.", overlay, true) {
            openSetting(
                context,
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        }
        PermissionRow(
            "Trợ năng",
            "Để bật lại màn hình khóa nếu con bấm Home hoặc đổi app. App không đọc nội dung màn hình.",
            access, true
        ) { openSetting(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        PermissionRow("Quản trị thiết bị", "Để khó gỡ app khi chưa có bố mẹ đồng ý.", admin, false) {
            openAdminRequest(context)
        }
        PermissionRow("Không bị tối ưu pin", "Để app không bị hệ thống tắt khi chạy nền.", battery, false) {
            openSetting(
                context,
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }
        if (Build.VERSION.SDK_INT >= 33) {
            PermissionRow("Thông báo", "Để thấy thông báo app đang chạy.", notif, false) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        SectionCard("Số điện thoại bố mẹ", "Nút \"Gọi bố mẹ\" trên màn hình khóa sẽ quay số này.") {
            OutlinedTextField(
                value = phone,
                onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() || c == '+' }.take(15)) },
                label = { Text("Số điện thoại") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
        PrimaryButton("Hoàn tất, bắt đầu bảo vệ", onDone, enabled = ready, color = Ui.Green)
        if (!ready) {
            Text(
                "Cần bật 3 quyền bắt buộc và nhập số điện thoại bố mẹ mới hoàn tất được.",
                color = Ui.Soft, fontSize = 13.sp
            )
        }
    }
}

/** Một dòng trạng thái: nhãn, giá trị, màu theo tốt/xấu. */
@Composable
private fun StatusLine(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ui.Soft, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (ok) Ui.Green else Ui.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/** Màn chính của máy con sau khi đã chạy giám sát, kèm bảng chẩn đoán. */
@Composable
fun ChildActiveScreen(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val store = remember { vn.phong.parental.data.firebase.LocalStore(context) }
    var clock by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            clock = System.currentTimeMillis()
        }
    }
    val missing = remember(tick, clock) { PermissionState.missingText(context) }
    val fmt = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    val loopAt = remember(clock) { store.lastLoopAt }
    val syncAt = remember(clock) { store.lastSyncAt }
    val scope = remember(clock) { store.lockScope }
    val err = remember(clock) { store.lastError }
    val serviceOk = loopAt > 0 && clock - loopAt < 40_000
    val syncOk = syncAt > 0 && clock - syncAt < 180_000
    val accOk = PermissionState.accessibility(context) &&
        vn.phong.parental.service.ChildAccessibilityService.instance != null

    ScreenScaffold("Máy đang được bảo vệ", "App chạy nền theo luật bố mẹ đặt") {
        if (missing.isNotEmpty()) {
            Notice("Đang thiếu quyền: $missing. Bố mẹ cũng thấy cảnh báo này.", NoticeKind.ERROR)
            SectionCard("Bật lại quyền") {
                if (!PermissionState.accessibility(context)) {
                    SecondaryButton("Mở cài đặt trợ năng", { openSetting(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                }
                if (!PermissionState.usage(context)) {
                    SecondaryButton("Mở cài đặt truy cập sử dụng", { openSetting(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
                }
                if (!PermissionState.overlay(context)) {
                    SecondaryButton("Mở cài đặt hiển thị trên app khác", {
                        openSetting(
                            context,
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    })
                }
                if (!PermissionState.admin(context)) {
                    SecondaryButton("Bật quản trị thiết bị", { openAdminRequest(context) })
                }
            }
        }
        SectionCard("Tình trạng hoạt động", "Nếu khóa không chạy, chụp màn hình này gửi người cài app.") {
            StatusLine("Dịch vụ chạy nền", if (serviceOk) "Đang chạy" else "Không chạy", serviceOk)
            StatusLine("Dịch vụ Trợ năng", if (accOk) "Đã kết nối" else "Chưa kết nối", accOk)
            StatusLine(
                "Kết nối với bố mẹ",
                if (syncOk) "Tốt (${fmt.format(java.util.Date(syncAt))})"
                else if (syncAt > 0) "Mất từ ${fmt.format(java.util.Date(syncAt))}" else "Chưa kết nối",
                syncOk
            )
            StatusLine(
                "Trạng thái khóa",
                when {
                    scope.isEmpty() -> "Không khóa"
                    scope == "ALL" -> "Khóa cả máy"
                    else -> "Khóa 1 app"
                },
                true
            )
            if (err.isNotBlank()) {
                Notice("Lỗi gần nhất: $err", NoticeKind.ERROR)
                SecondaryButton("Xóa thông báo lỗi", { store.lastError = "" })
            }
            SecondaryButton("Khởi động lại dịch vụ", {
                runCatching {
                    androidx.core.content.ContextCompat.startForegroundService(
                        context,
                        Intent(context, vn.phong.parental.service.LockEnforcementService::class.java)
                    )
                }
            })
        }
        SectionCard("Số điện thoại bố mẹ", "Dùng cho nút \"Gọi bố mẹ\" trên màn hình khóa.") {
            OutlinedTextField(
                value = phone,
                onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() || c == '+' }.take(15)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
