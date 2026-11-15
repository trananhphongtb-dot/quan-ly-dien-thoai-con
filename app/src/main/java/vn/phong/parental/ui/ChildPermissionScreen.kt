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
import androidx.compose.foundation.background
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

private val BgBlue = Color(0xFFEAF2FB)
private val MainBlue = Color(0xFF1565C0)
private val DarkText = Color(0xFF1A2A3A)
private val SoftText = Color(0xFF5A6B7D)
private val Green = Color(0xFF1B8A4B)
private val Red = Color(0xFFB3261E)

/** Một dòng quyền: tên, giải thích, trạng thái, nút mở cài đặt. */
@Composable
private fun PermissionRow(
    title: String,
    why: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText, modifier = Modifier.weight(1f))
                Text(
                    if (granted) "Đã bật" else if (required) "Cần bật" else "Nên bật",
                    color = if (granted) Green else Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(why, color = SoftText, fontSize = 13.sp)
            if (!granted) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) { Text("Mở cài đặt để bật", fontSize = 15.sp) }
            }
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlue)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MainBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Cấp quyền cho máy con", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bố mẹ đã duyệt. Bật các quyền dưới đây để app đo giờ dùng và khóa máy đúng luật.",
                    color = Color(0xFFD6E6F7), fontSize = 14.sp
                )
            }
        }
        PermissionRow(
            "Truy cập dữ liệu sử dụng",
            "Để đo máy đang dùng app nào và bao lâu.",
            usage, true
        ) { openSetting(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        PermissionRow(
            "Hiển thị trên ứng dụng khác",
            "Để màn hình khóa hiện lên trên app đang mở.",
            overlay, true
        ) {
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
        PermissionRow(
            "Quản trị thiết bị",
            "Để khó gỡ app khi chưa có bố mẹ đồng ý.",
            admin, false
        ) { openAdminRequest(context) }
        PermissionRow(
            "Không bị tối ưu pin",
            "Để app không bị hệ thống tắt khi chạy nền.",
            battery, false
        ) {
            openSetting(
                context,
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
            )
        }
        if (Build.VERSION.SDK_INT >= 33) {
            PermissionRow(
                "Thông báo",
                "Để thấy thông báo app đang chạy.",
                notif, false
            ) { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Số điện thoại bố mẹ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                Spacer(Modifier.height(4.dp))
                Text("Nút \"Gọi bố mẹ\" trên màn hình khóa sẽ quay số này.", color = SoftText, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() || c == '+' }.take(15)) },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Button(
            onClick = onDone,
            enabled = ready,
            colors = ButtonDefaults.buttonColors(containerColor = Green),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Hoàn tất, bắt đầu bảo vệ", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        if (!ready) {
            Text(
                "Cần bật 3 quyền bắt buộc và nhập số điện thoại bố mẹ mới hoàn tất được.",
                color = SoftText, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Màn chính của máy con sau khi đã chạy giám sát. */
@Composable
fun ChildActiveScreen(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    val context = LocalContext.current
    val tick = rememberResumeTick()
    val missing = remember(tick) { PermissionState.missingText(context) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlue)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MainBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Máy đang được bảo vệ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("App chạy nền theo luật bố mẹ đặt. Không cần mở app này.", color = Color(0xFFD6E6F7), fontSize = 14.sp)
            }
        }
        if (missing.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEA)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Đang thiếu quyền: $missing", color = Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Bố mẹ cũng sẽ thấy cảnh báo này. Bật lại quyền để app khóa đúng.", color = Red, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { openSetting(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mở cài đặt trợ năng", color = DarkText) }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { openSetting(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Mở cài đặt truy cập sử dụng", color = DarkText) }
                    if (!PermissionState.overlay(context)) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                openSetting(
                                    context,
                                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Mở cài đặt hiển thị trên app khác", color = DarkText) }
                    }
                    if (!PermissionState.admin(context)) {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { openAdminRequest(context) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Bật quản trị thiết bị", color = DarkText) }
                    }
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Số điện thoại bố mẹ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
                Spacer(Modifier.height(8.dp))
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
}
