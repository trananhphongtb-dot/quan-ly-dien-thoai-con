package vn.phong.parental.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PerAppLimit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgBlue = Ui.Bg
private val MainBlue = Ui.Primary
private val DarkText = Ui.Text
private val SoftText = Ui.Soft
private val Green = Ui.Green
private val Red = Ui.Red
private val TimeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * App bên bố mẹ, 4 tab:
 * 1. Theo dõi & Thống kê — 2. Giới hạn chung — 3. Giới hạn từng app — 4. Khóa máy.
 * Mọi số liệu lấy từ máy con thật qua Firestore, tab 2 và 3 sửa được.
 */
@Composable
fun ParentHomeScreen(
    viewModel: ParentalViewModel,
    onSignedOut: () -> Unit = {},
    onUnpaired: () -> Unit = {}
) {
    val child by viewModel.child.collectAsState()
    val general by viewModel.generalLimit.collectAsState()
    val perApp by viewModel.perAppLimits.collectAsState()
    val usage by viewModel.usage.collectAsState()
    val installed by viewModel.installedApps.collectAsState()
    val error by viewModel.error.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Theo dõi", "Giới hạn", "Từng app", "Khóa máy")
    val icons = listOf(Icons.Filled.Home, Icons.Filled.Settings, Icons.AutoMirrored.Filled.List, Icons.Filled.Lock)

    Column(modifier = Modifier.fillMaxSize().background(BgBlue)) {
        val now = System.currentTimeMillis()
        val online = child != null && child!!.updatedAtMillis > 0 && now - child!!.updatedAtMillis < 150_000
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Ui.PrimaryDark, Ui.Primary)))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppAvatar(child?.name?.ifBlank { "Con" } ?: "Con", 48)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    child?.name?.ifBlank { "Máy của con" } ?: "Máy của con",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        if (child == null) "Đang tải" else if (online) "Online" else "Mất kết nối",
                        if (online) Ui.Green else Ui.Amber
                    )
                    if (child?.isLocked == true) StatusChip("Đang khóa", Ui.Red)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let {
                Notice("Lỗi: $it", NoticeKind.ERROR) {
                    TextButton(onClick = { viewModel.clearError() }) { Text("Đóng") }
                }
            }
            val c = child
            when (tab) {
                0 -> TrackTab(c, general, usage, viewModel)
                1 -> GeneralTab(general) { viewModel.saveGeneralLimit(it) }
                2 -> PerAppTab(perApp, usage, installed, viewModel)
                3 -> LockTab(c, viewModel, onSignedOut, onUnpaired)
            }
            Spacer(Modifier.height(8.dp))
        }
        NavigationBar(containerColor = Color.White) {
            titles.forEachIndexed { index, title ->
                NavigationBarItem(
                    selected = tab == index,
                    onClick = { tab = index },
                    icon = { Icon(icons[index], contentDescription = title) },
                    label = { Text(title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MainBlue,
                        indicatorColor = MainBlue
                    )
                )
            }
        }
    }
}

// ---- Tab 1 ----

@Composable
private fun TrackTab(
    child: ChildDevice?,
    general: GeneralLimit,
    usage: List<vn.phong.parental.model.AppUsageStat>,
    viewModel: ParentalViewModel
) {
    if (child == null) {
        SectionCard("Chưa có dữ liệu từ máy con") {
            Text("Mở app trên máy con và chờ vài giây. Nếu mãi không có, kiểm tra mạng của máy con.", color = SoftText, fontSize = 14.sp)
        }
        return
    }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val bonus = if (child.bonusDate == today) child.bonusMinutes else 0
    val limit = general.dailyTotalMinutes + bonus
    if (child.permMissing.isNotBlank()) {
        Notice("Máy con đang thiếu quyền: ${child.permMissing}. Nhờ con bật lại, nếu không app có thể không khóa được.", NoticeKind.ERROR)
    }
    if (child.unlockRequest.isNotBlank()) {
        SectionCard("Con đang xin") {
            Text(child.unlockRequest, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
            if (child.unlockRequestAtMillis > 0) {
                Text(
                    "Lúc " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(child.unlockRequestAtMillis)),
                    color = SoftText, fontSize = 13.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Cho +15 phút", { viewModel.grantBonus(15) }, Modifier.weight(1f), color = Green)
                PrimaryButton("Cho +30 phút", { viewModel.grantBonus(30) }, Modifier.weight(1f), color = Green)
            }
            TextButton(onClick = { viewModel.dismissUnlockRequest() }) { Text("Bỏ qua lời xin") }
        }
    }
    SectionCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            UsageRing(child.todayTotalMinutes, limit)
            Spacer(Modifier.height(8.dp))
            val left = (limit - child.todayTotalMinutes).coerceAtLeast(0)
            Text(
                if (child.todayTotalMinutes >= limit) "Đã hết giờ dùng hôm nay"
                else "Còn ${left / 60} giờ ${left % 60} phút",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = if (child.todayTotalMinutes >= limit) Red else DarkText
            )
            if (bonus > 0) Text("Đã cho thêm $bonus phút hôm nay", color = SoftText, fontSize = 13.sp)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("Mở máy", child.firstUnlockToday, Modifier.weight(1f))
        StatCard("Đóng máy", child.lastLockToday, Modifier.weight(1f))
    }
    SectionCard("Đang dùng") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(child.currentApp, 44)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(child.currentApp, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Text("Từ ${child.currentAppSince}", color = SoftText, fontSize = 13.sp)
            }
        }
    }
    SectionCard("Top app hôm nay") {
        if (usage.isEmpty()) {
            Text("Chưa có thống kê. Máy con cập nhật mỗi vài phút.", color = SoftText, fontSize = 14.sp)
        }
        val sorted = usage.sortedByDescending { it.minutesToday }.take(8)
        val max = (sorted.firstOrNull()?.minutesToday ?: 1).coerceAtLeast(1)
        sorted.forEach {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                AppAvatar(it.appName, 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(it.appName, modifier = Modifier.weight(1f), color = DarkText, fontSize = 15.sp, maxLines = 1)
                        Text("${it.minutesToday} phút", color = SoftText, fontSize = 13.sp)
                    }
                    LinearProgressIndicator(
                        progress = { it.minutesToday.toFloat() / max },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = MainBlue, trackColor = Ui.PrimarySoft
                    )
                    Text("mở ${it.opensToday} lần", color = SoftText, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Vòng tròn thời gian dùng hôm nay so với giới hạn. */
@Composable
private fun UsageRing(used: Int, limit: Int) {
    val frac = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val color = when {
        frac >= 1f -> Ui.Red
        frac >= 0.8f -> Color(0xFFE08A00)
        else -> Ui.Primary
    }
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 18.dp.toPx()
            val inset = w / 2
            val arcSize = Size(size.width - w, size.height - w)
            drawArc(Ui.PrimarySoft, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            if (frac > 0f) {
                drawArc(color, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$used", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = DarkText)
            Text("/ $limit phút", fontSize = 14.sp, color = SoftText)
        }
    }
}

/** Hình tròn có chữ cái đầu, màu theo tên, thay cho biểu tượng app. */
@Composable
fun AppAvatar(name: String, size: Int) {
    val palette = listOf(0xFF1565C0, 0xFF2E7D32, 0xFFEF6C00, 0xFF6A1B9A, 0xFFC62828, 0xFF00838F, 0xFF4E342E)
    val color = Color(palette[(name.hashCode() and 0x7fffffff) % palette.size])
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size * 0.45f).sp
        )
    }
}

@Composable
fun StatusChip(text: String, bg: Color) {
    Text(
        text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

// ---- Tab 2 ----

@Composable
private fun GeneralTab(saved: GeneralLimit, onSave: (GeneralLimit) -> Unit) {
    var minutes by remember(saved) { mutableIntStateOf(saved.dailyTotalMinutes) }
    var bedStart by remember(saved) { mutableStateOf(saved.bedtimeStart) }
    var bedEnd by remember(saved) { mutableStateOf(saved.bedtimeEnd) }
    var studyStart by remember(saved) { mutableStateOf(saved.studyLockStart) }
    var studyEnd by remember(saved) { mutableStateOf(saved.studyLockEnd) }
    val valid = listOf(bedStart, bedEnd, studyStart, studyEnd).all { TimeRegex.matches(it) }
    val draft = GeneralLimit(minutes, bedStart, bedEnd, studyStart, studyEnd)
    val changed = draft != saved

    WhiteCard {
        Text("Tổng giờ mỗi ngày", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { minutes = (minutes - 15).coerceAtLeast(15) }) { Text("−15") }
            Text(
                "$minutes phút",
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MainBlue,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            OutlinedButton(onClick = { minutes = (minutes + 15).coerceAtMost(720) }) { Text("+15") }
        }
        Text("= ${minutes / 60} giờ ${minutes % 60} phút", color = SoftText, fontSize = 13.sp)
    }
    WhiteCard {
        Text("Giờ ngủ khóa máy", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
        Spacer(Modifier.height(8.dp))
        TimeRow(bedStart, bedEnd, { bedStart = it }, { bedEnd = it })
    }
    WhiteCard {
        Text("Giờ học (chỉ mở app học)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
        Spacer(Modifier.height(8.dp))
        TimeRow(studyStart, studyEnd, { studyStart = it }, { studyEnd = it })
    }
    if (!valid) Text("Giờ phải đúng dạng HH:mm, ví dụ 21:30.", color = Red, fontSize = 13.sp)
    Button(
        onClick = { onSave(draft) },
        enabled = valid && changed,
        colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) { Text(if (changed) "Lưu luật chung" else "Đã lưu", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    Text(
        "Hết tổng giờ thì khóa máy, chỉ chừa gọi bố mẹ. Giờ ngủ qua nửa đêm (ví dụ 21:30–06:30) vẫn đúng.",
        color = SoftText, fontSize = 13.sp
    )
}

@Composable
private fun TimeRow(start: String, end: String, onStart: (String) -> Unit, onEnd: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = start, onValueChange = { onStart(it.take(5)) },
            label = { Text("Từ") }, singleLine = true,
            isError = !TimeRegex.matches(start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = end, onValueChange = { onEnd(it.take(5)) },
            label = { Text("Đến") }, singleLine = true,
            isError = !TimeRegex.matches(end),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
    }
}

// ---- Tab 3 ----

@Composable
private fun PerAppTab(
    limits: List<PerAppLimit>,
    usage: List<vn.phong.parental.model.AppUsageStat>,
    installed: List<vn.phong.parental.model.InstalledApp>,
    viewModel: ParentalViewModel
) {
    var adding by remember { mutableStateOf(false) }
    val usedByPkg = usage.associate { it.packageName to it.minutesToday }
    WhiteCard {
        Text("Giới hạn từng app", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
        Spacer(Modifier.height(10.dp))
        if (limits.isEmpty()) {
            Text("Chưa có app nào bị giới hạn.", color = SoftText, fontSize = 14.sp)
        }
        limits.sortedBy { it.appName.lowercase() }.forEach { l ->
            val used = usedByPkg[l.packageName] ?: 0
            val out = l.dailyMinutes > 0 && used >= l.dailyMinutes
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppAvatar(l.appName, 36)
                    Spacer(Modifier.width(10.dp))
                    Text(l.appName, modifier = Modifier.weight(1f), color = DarkText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("$used/${l.dailyMinutes} phút", color = if (out) Red else SoftText, fontSize = 14.sp)
                }
                if (out) Text("Hết giờ, đã khóa", color = Red, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (l.dailyMinutes > 0) (used.toFloat() / l.dailyMinutes).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        viewModel.savePerAppLimit(l.copy(dailyMinutes = (l.dailyMinutes - 10).coerceAtLeast(0)))
                    }) { Text("−10") }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = {
                        viewModel.savePerAppLimit(l.copy(dailyMinutes = l.dailyMinutes + 10))
                    }) { Text("+10") }
                    Spacer(Modifier.weight(1f))
                    Text("App học", color = SoftText, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = l.isMandatory,
                        onCheckedChange = { viewModel.savePerAppLimit(l.copy(isMandatory = it)) }
                    )
                }
                TextButton(onClick = { viewModel.deletePerAppLimit(l.packageName) }) {
                    Text("Bỏ giới hạn app này", color = Red)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Button(
            onClick = { adding = true },
            colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Thêm app", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Text(
            "App học bật công tắc \"App học\": trong giờ học chỉ các app này mở được. " +
                "0 phút = khóa hẳn app đó.",
            color = SoftText, fontSize = 13.sp
        )
    }
    if (adding) {
        AddAppDialog(
            apps = installed.filter { app -> limits.none { it.packageName == app.packageName } },
            onDismiss = { adding = false },
            onAdd = { app, minutes ->
                viewModel.savePerAppLimit(PerAppLimit(app.packageName, app.appName, minutes))
                adding = false
            }
        )
    }
}

@Composable
private fun AddAppDialog(
    apps: List<vn.phong.parental.model.InstalledApp>,
    onDismiss: () -> Unit,
    onAdd: (vn.phong.parental.model.InstalledApp, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(30) }
    val filtered = apps.filter { it.appName.contains(query, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm app giới hạn") },
        text = {
            Column {
                if (apps.isEmpty()) {
                    Text("Chưa có danh sách app từ máy con. Mở máy con vài phút rồi thử lại.", fontSize = 14.sp)
                } else {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text("Tìm app") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        OutlinedButton(onClick = { minutes = (minutes - 10).coerceAtLeast(0) }) { Text("−10") }
                        Text(
                            "$minutes phút/ngày", modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        OutlinedButton(onClick = { minutes += 10 }) { Text("+10") }
                    }
                    LazyColumn(Modifier.height(260.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            TextButton(
                                onClick = { onAdd(app, minutes) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(app.appName, modifier = Modifier.fillMaxWidth(), color = DarkText)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

// ---- Tab 4 ----

@Composable
private fun LockTab(
    child: ChildDevice?,
    viewModel: ParentalViewModel,
    onSignedOut: () -> Unit,
    onUnpaired: () -> Unit
) {
    val locked = child?.isLocked ?: false
    var confirmUnpair by remember { mutableStateOf(false) }
    SectionCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(130.dp).clip(CircleShape)
                    .background(if (locked) Ui.RedSoft else Ui.GreenSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Lock, contentDescription = null,
                    tint = if (locked) Red else Green, modifier = Modifier.size(64.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (locked) "Máy con đang bị khóa" else "Máy con đang mở",
                fontSize = 19.sp, fontWeight = FontWeight.Bold,
                color = if (locked) Red else Green
            )
            Text("Lý do khóa hiện trên máy con.", color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                if (locked) "Mở khóa máy con" else "Khóa máy con ngay",
                { viewModel.setLocked(!locked, if (!locked) "Bố mẹ khóa máy" else "") },
                enabled = child != null,
                color = if (locked) Green else Red
            )
        }
    }
    WhiteCard {
        Text("Cho con thêm giờ hôm nay", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
        Text("Cộng vào tổng giờ của hôm nay và mở khóa nếu đang hết giờ.", color = SoftText, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.grantBonus(15) }, enabled = child != null, modifier = Modifier.weight(1f)) { Text("+15 phút") }
            OutlinedButton(onClick = { viewModel.grantBonus(30) }, enabled = child != null, modifier = Modifier.weight(1f)) { Text("+30 phút") }
            OutlinedButton(onClick = { viewModel.grantBonus(60) }, enabled = child != null, modifier = Modifier.weight(1f)) { Text("+60 phút") }
        }
        child?.let {
            if (it.bonusMinutes > 0) {
                Spacer(Modifier.height(6.dp))
                Text("Hôm nay đã cho thêm ${it.bonusMinutes} phút.", color = SoftText, fontSize = 13.sp)
            }
        }
    }
    Text(
        "Khi khóa, máy con chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa.",
        color = SoftText, fontSize = 14.sp
    )
    WhiteCard {
        Text("Tài khoản", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { confirmUnpair = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Hủy ghép nối máy con", color = Red) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.signOutParent(onSignedOut) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Đăng xuất", color = DarkText) }
    }
    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text("Hủy ghép nối?") },
            text = { Text("Máy con sẽ ngừng được quản lý. Muốn quản lý lại phải ghép nối từ đầu.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    viewModel.unpairChild(onUnpaired)
                }) { Text("Hủy ghép nối", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("Giữ lại") } }
        )
    }
}

// ---- Thành phần chung ----

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    SectionCard { content() }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Ui.Line),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
        }
    }
}
