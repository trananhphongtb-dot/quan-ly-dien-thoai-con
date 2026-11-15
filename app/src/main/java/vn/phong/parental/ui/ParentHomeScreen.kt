package vn.phong.parental.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.InstalledApp
import vn.phong.parental.model.PerAppLimit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TimeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

/**
 * App bên bố mẹ, 4 tab:
 * 1. Theo dõi — 2. Giới hạn chung — 3. Từng app — 4. Khóa máy.
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
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000)
            clock = System.currentTimeMillis()
        }
    }
    val tabs = listOf(
        TabItem(Lang.t("Theo dõi", "Monitor"), Icons.Filled.Home),
        TabItem(Lang.t("Giới hạn", "Limits"), Icons.Filled.Settings),
        TabItem(Lang.t("Từng app", "Apps"), Icons.AutoMirrored.Filled.List),
        TabItem(Lang.t("Khóa máy", "Lock"), Icons.Filled.Lock)
    )

    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        val c = child
        val online = c != null && c.updatedAtMillis > 0 && clock - c.updatedAtMillis < 150_000
        AppHeader(
            title = c?.name?.ifBlank { null } ?: Lang.t("Máy của con", "Child's phone"),
            subtitle = when {
                c == null -> Lang.t("Đang tải...", "Loading...")
                online -> Lang.t("Đang online", "Online")
                else -> Lang.t("Mất kết nối", "Offline")
            },
            leading = { AppAvatar(c?.name?.ifBlank { null } ?: "C", 40) },
            trailing = {
                if (c?.isLocked == true) StatusChip(Lang.t("Đang khóa", "Locked"), Ui.Red)
                else if (c != null && c.unlockUntilMillis > clock) StatusChip(Lang.t("Mở tạm", "Pass"), Ui.Green)
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            error?.let {
                Notice(Lang.t("Lỗi: $it", "Error: $it"), NoticeKind.ERROR) {
                    TextButton(onClick = { viewModel.clearError() }) { Text(Lang.t("Đóng", "Close")) }
                }
            }
            when (tab) {
                0 -> TrackTab(c, general, usage, viewModel, clock)
                1 -> GeneralTab(general) { viewModel.saveGeneralLimit(it) }
                2 -> PerAppTab(perApp, usage, installed, viewModel)
                3 -> LockTab(c, viewModel, onSignedOut, onUnpaired)
            }
            Spacer(Modifier.height(4.dp))
        }
        AppTabBar(tabs, tab) { tab = it }
    }
}

// ---- Tab 1: Theo dõi ----

@Composable
private fun TrackTab(
    child: ChildDevice?,
    general: GeneralLimit,
    usage: List<AppUsageStat>,
    viewModel: ParentalViewModel,
    clock: Long
) {
    if (child == null) {
        SectionCard(Lang.t("Chưa có dữ liệu từ máy con", "No data from the child's phone yet")) {
            Text(
                Lang.t(
                    "Mở app trên máy con và chờ vài giây. Nếu mãi không có, kiểm tra mạng của máy con.",
                    "Open the app on the child's phone and wait a few seconds. If nothing shows, check its internet."
                ),
                color = Ui.Soft, fontSize = 13.sp
            )
        }
        return
    }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val bonus = if (child.bonusDate == today) child.bonusMinutes else 0
    val limit = general.dailyTotalMinutes + bonus
    if (child.permMissing.isNotBlank()) {
        Notice(
            Lang.t(
                "Máy con đang thiếu quyền: ${child.permMissing}. Nhờ con bật lại, nếu không app có thể không khóa được.",
                "The child's phone lacks permissions: ${child.permMissing}. Ask them to turn them on, or locking may fail."
            ),
            NoticeKind.ERROR
        )
    }
    if (child.unlockRequest.isNotBlank()) {
        SectionCard(Lang.t("Con đang xin", "Your child is asking")) {
            Text(child.unlockRequest, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ui.Text)
            if (child.unlockRequestAtMillis > 0) {
                Text(
                    Lang.t("Lúc ", "At ") + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(child.unlockRequestAtMillis)),
                    color = Ui.Soft, fontSize = 12.sp
                )
            }
            Text(
                Lang.t("Đồng ý sẽ mở khóa tạm, bỏ qua giờ ngủ, giờ học và giới hạn app:", "Approving gives a temporary pass over bedtime, study time and app limits:"),
                color = Ui.Soft, fontSize = 12.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(Lang.t("15 phút", "15 min"), { viewModel.grantPass(15) }, Modifier.weight(1f), color = Ui.Green)
                PrimaryButton(Lang.t("30 phút", "30 min"), { viewModel.grantPass(30) }, Modifier.weight(1f), color = Ui.Green)
                PrimaryButton(Lang.t("1 giờ", "1 hour"), { viewModel.grantPass(60) }, Modifier.weight(1f), color = Ui.Green)
            }
            SecondaryButton(Lang.t("Từ chối", "Decline"), { viewModel.denyRequest() }, color = Ui.Red)
        }
    }
    if (child.unlockUntilMillis > clock) {
        val left = ((child.unlockUntilMillis - clock) / 60_000L + 1).toInt()
        Notice(Lang.t("Đang mở khóa tạm, còn khoảng $left phút.", "Temporary pass active, about $left min left."), NoticeKind.SUCCESS) {
            TextButton(onClick = { viewModel.setLocked(true, Lang.t("Bố mẹ khóa máy", "Locked by parent")) }) {
                Text(Lang.t("Khóa lại", "Lock now"), color = Ui.Red)
            }
        }
    }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UsageRing(child.todayTotalMinutes, limit, 130)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Lang.t("Hôm nay", "Today"), fontSize = 12.sp, color = Ui.Soft)
                val left = (limit - child.todayTotalMinutes).coerceAtLeast(0)
                Text(
                    if (child.todayTotalMinutes >= limit) Lang.t("Đã hết giờ", "Time is up")
                    else Lang.t("Còn ${left / 60}g ${left % 60}p", "${left / 60}h ${left % 60}m left"),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = if (child.todayTotalMinutes >= limit) Ui.Red else Ui.Text
                )
                if (bonus > 0) Text(Lang.t("Đã cho thêm $bonus phút", "$bonus extra min granted"), color = Ui.Soft, fontSize = 12.sp)
                Text(Lang.t("Mở máy ${child.firstUnlockToday}", "Unlocked ${child.firstUnlockToday}"), color = Ui.Soft, fontSize = 12.sp)
                Text(Lang.t("Đóng máy ${child.lastLockToday}", "Locked ${child.lastLockToday}"), color = Ui.Soft, fontSize = 12.sp)
            }
        }
    }
    SectionCard(Lang.t("Đang dùng", "Using now")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(child.currentApp, 40)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(child.currentApp, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ui.Text)
                Text(Lang.t("Từ ${child.currentAppSince}", "Since ${child.currentAppSince}"), color = Ui.Soft, fontSize = 12.sp)
            }
        }
    }
    SectionCard(Lang.t("Top app hôm nay", "Top apps today")) {
        if (usage.isEmpty()) {
            Text(Lang.t("Chưa có thống kê. Máy con cập nhật mỗi vài phút.", "No stats yet. The child's phone updates every few minutes."), color = Ui.Soft, fontSize = 13.sp)
        }
        val sorted = usage.sortedByDescending { it.minutesToday }.take(8)
        val max = (sorted.firstOrNull()?.minutesToday ?: 1).coerceAtLeast(1)
        sorted.forEach {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                AppAvatar(it.appName, 34)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Text(it.appName, modifier = Modifier.weight(1f), color = Ui.Text, fontSize = 14.sp, maxLines = 1)
                        Text(Lang.t("${it.minutesToday} phút", "${it.minutesToday} min"), color = Ui.Soft, fontSize = 12.sp)
                    }
                    LinearProgressIndicator(
                        progress = { it.minutesToday.toFloat() / max },
                        modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(5.dp).clip(RoundedCornerShape(50)),
                        color = Ui.Primary, trackColor = Ui.PrimarySoft
                    )
                }
            }
        }
    }
}

/** Vòng tròn thời gian dùng hôm nay so với giới hạn. */
@Composable
private fun UsageRing(used: Int, limit: Int, size: Int) {
    val frac = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    val color = when {
        frac >= 1f -> Ui.Red
        frac >= 0.8f -> Color(0xFFE08A00)
        else -> Ui.Primary
    }
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = (size * 0.11f).dp.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(Ui.PrimarySoft, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            if (frac > 0f) {
                drawArc(color, -90f, 360f * frac, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$used", fontSize = (size * 0.26f).sp, fontWeight = FontWeight.Bold, color = Ui.Text)
            Text("/ $limit", fontSize = 11.sp, color = Ui.Soft)
        }
    }
}

/** Hình tròn có chữ cái đầu, màu theo tên, thay cho biểu tượng app. */
@Composable
fun AppAvatar(name: String, size: Int) {
    val palette = listOf(0xFF1565C0, 0xFF2E7D32, 0xFFEF6C00, 0xFF6A1B9A, 0xFFC62828, 0xFF00838F, 0xFF4E342E)
    val color = Color(palette[(name.hashCode() and 0x7fffffff) % palette.size])
    Box(
        Modifier
            .size(size.dp)
            .shadow(4.dp, CircleShape, spotColor = color)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.78f), color))),
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
        modifier = Modifier
            .shadow(3.dp, RoundedCornerShape(50), spotColor = bg)
            .clip(RoundedCornerShape(50)).background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Nút nhỏ − / + nổi 3D để chỉnh số. */
@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .shadow(4.dp, shape, spotColor = Ui.Shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFEAF1FB))))
            .border(1.dp, Ui.Line, shape)
            .clickable(onClick = onClick)
            .size(width = 58.dp, height = 40.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, fontWeight = FontWeight.Bold, color = Ui.Primary, fontSize = 15.sp) }
}

// ---- Tab 2: Giới hạn chung ----

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

    SectionCard(Lang.t("Tổng giờ mỗi ngày", "Daily total time")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−15") { minutes = (minutes - 15).coerceAtLeast(15) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Lang.t("$minutes phút", "$minutes min"), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Ui.Primary)
                Text(Lang.t("= ${minutes / 60} giờ ${minutes % 60} phút", "= ${minutes / 60} h ${minutes % 60} min"), color = Ui.Soft, fontSize = 12.sp)
            }
            StepButton("+15") { minutes = (minutes + 15).coerceAtMost(720) }
        }
    }
    SectionCard(Lang.t("Giờ ngủ khóa máy", "Bedtime lock")) {
        TimeRow(bedStart, bedEnd, { bedStart = it }, { bedEnd = it })
    }
    SectionCard(Lang.t("Giờ học (chỉ mở app học)", "Study time (study apps only)")) {
        TimeRow(studyStart, studyEnd, { studyStart = it }, { studyEnd = it })
    }
    if (!valid) Text(Lang.t("Giờ phải đúng dạng HH:mm, ví dụ 21:30.", "Time must look like HH:mm, e.g. 21:30."), color = Ui.Red, fontSize = 12.sp)
    PrimaryButton(
        if (changed) Lang.t("Lưu luật chung", "Save rules") else Lang.t("Đã lưu", "Saved"),
        { onSave(draft) }, enabled = valid && changed
    )
    Text(
        Lang.t(
            "Hết tổng giờ thì khóa máy, chỉ chừa gọi bố mẹ. Giờ ngủ qua nửa đêm (ví dụ 21:30–06:30) vẫn đúng.",
            "When the total time is used up the phone locks, leaving only calls to you. Bedtime across midnight (e.g. 21:30–06:30) works."
        ),
        color = Ui.Soft, fontSize = 12.sp
    )
}

@Composable
private fun TimeRow(start: String, end: String, onStart: (String) -> Unit, onEnd: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = start, onValueChange = { onStart(it.take(5)) },
            label = { Text(Lang.t("Từ", "From")) }, singleLine = true,
            isError = !TimeRegex.matches(start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        OutlinedTextField(
            value = end, onValueChange = { onEnd(it.take(5)) },
            label = { Text(Lang.t("Đến", "To")) }, singleLine = true,
            isError = !TimeRegex.matches(end),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.weight(1f)
        )
    }
}

// ---- Tab 3: Từng app ----

@Composable
private fun PerAppTab(
    limits: List<PerAppLimit>,
    usage: List<AppUsageStat>,
    installed: List<InstalledApp>,
    viewModel: ParentalViewModel
) {
    var adding by remember { mutableStateOf(false) }
    val usedByPkg = usage.associate { it.packageName to it.minutesToday }
    PrimaryButton(Lang.t("+ Thêm app cần giới hạn", "+ Add app limit"), { adding = true })
    if (limits.isEmpty()) {
        SectionCard { Text(Lang.t("Chưa có app nào bị giới hạn.", "No app limits yet."), color = Ui.Soft, fontSize = 13.sp) }
    }
    limits.sortedBy { it.appName.lowercase() }.forEach { l ->
        val used = usedByPkg[l.packageName] ?: 0
        val out = l.dailyMinutes > 0 && used >= l.dailyMinutes
        SectionCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppAvatar(l.appName, 36)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(l.appName, color = Ui.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(
                        if (out) Lang.t("Hết giờ, đã khóa", "Time is up, locked")
                        else Lang.t("$used / ${l.dailyMinutes} phút", "$used / ${l.dailyMinutes} min"),
                        color = if (out) Ui.Red else Ui.Soft, fontSize = 12.sp
                    )
                }
                TextButton(onClick = { viewModel.deletePerAppLimit(l.packageName) }) {
                    Text(Lang.t("Bỏ", "Remove"), color = Ui.Red)
                }
            }
            LinearProgressIndicator(
                progress = { if (l.dailyMinutes > 0) (used.toFloat() / l.dailyMinutes).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = if (out) Ui.Red else Ui.Primary, trackColor = Ui.PrimarySoft
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−10") { viewModel.savePerAppLimit(l.copy(dailyMinutes = (l.dailyMinutes - 10).coerceAtLeast(0))) }
                Spacer(Modifier.width(8.dp))
                StepButton("+10") { viewModel.savePerAppLimit(l.copy(dailyMinutes = l.dailyMinutes + 10)) }
                Spacer(Modifier.weight(1f))
                Text(Lang.t("App học", "Study app"), color = Ui.Soft, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Switch(checked = l.isMandatory, onCheckedChange = { viewModel.savePerAppLimit(l.copy(isMandatory = it)) })
            }
        }
    }
    Text(
        Lang.t(
            "Bật \"App học\": trong giờ học chỉ các app này mở được. 0 phút = khóa hẳn app đó.",
            "Turn on \"Study app\": during study time only these apps open. 0 min = app fully blocked."
        ),
        color = Ui.Soft, fontSize = 12.sp
    )
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
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onAdd: (InstalledApp, Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(30) }
    val filtered = apps.filter { it.appName.contains(query, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Lang.t("Thêm app giới hạn", "Add app limit")) },
        text = {
            Column {
                if (apps.isEmpty()) {
                    Text(
                        Lang.t("Chưa có danh sách app từ máy con. Mở máy con vài phút rồi thử lại.", "No app list from the child's phone yet. Open it for a few minutes and retry."),
                        fontSize = 14.sp
                    )
                } else {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        label = { Text(Lang.t("Tìm app", "Search app")) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        StepButton("−10") { minutes = (minutes - 10).coerceAtLeast(0) }
                        Text(
                            Lang.t("$minutes phút/ngày", "$minutes min/day"), modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                        )
                        StepButton("+10") { minutes += 10 }
                    }
                    LazyColumn(Modifier.height(260.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            TextButton(onClick = { onAdd(app, minutes) }, modifier = Modifier.fillMaxWidth()) {
                                Text(app.appName, modifier = Modifier.fillMaxWidth(), color = Ui.Text)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Lang.t("Đóng", "Close")) } }
    )
}

// ---- Tab 4: Khóa máy ----

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
            val tint = if (locked) Ui.Red else Ui.Green
            Box(
                Modifier.size(104.dp)
                    .shadow(12.dp, CircleShape, spotColor = tint)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color.White, if (locked) Ui.RedSoft else Ui.GreenSoft)))
                    .border(2.dp, tint.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Lock, null, tint = tint, modifier = Modifier.size(50.dp)) }
            Spacer(Modifier.height(8.dp))
            Text(
                if (locked) Lang.t("Máy con đang bị khóa", "Child's phone is locked") else Lang.t("Máy con đang mở", "Child's phone is unlocked"),
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tint
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(
                if (locked) Lang.t("Mở khóa máy con", "Unlock child's phone") else Lang.t("Khóa máy con ngay", "Lock child's phone now"),
                { viewModel.setLocked(!locked, if (!locked) Lang.t("Bố mẹ khóa máy", "Locked by parent") else "") },
                enabled = child != null, color = if (locked) Ui.Green else Ui.Red
            )
        }
    }
    SectionCard(
        Lang.t("Cho mở khóa tạm", "Temporary unlock"),
        Lang.t("Bỏ qua hết giờ, giờ ngủ, giờ học và giới hạn app trong thời gian này.", "Overrides time up, bedtime, study time and app limits for this period.")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(Lang.t("15 phút", "15 min"), { viewModel.grantPass(15) }, Modifier.weight(1f), enabled = child != null, color = Ui.Green)
            PrimaryButton(Lang.t("30 phút", "30 min"), { viewModel.grantPass(30) }, Modifier.weight(1f), enabled = child != null, color = Ui.Green)
            PrimaryButton(Lang.t("1 giờ", "1 hour"), { viewModel.grantPass(60) }, Modifier.weight(1f), enabled = child != null, color = Ui.Green)
        }
    }
    SectionCard(
        Lang.t("Cho thêm giờ hôm nay", "Add time today"),
        Lang.t("Cộng vào tổng giờ của hôm nay.", "Adds to today's total time.")
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("+15", { viewModel.grantBonus(15) }, Modifier.weight(1f), enabled = child != null)
            SecondaryButton("+30", { viewModel.grantBonus(30) }, Modifier.weight(1f), enabled = child != null)
            SecondaryButton("+60", { viewModel.grantBonus(60) }, Modifier.weight(1f), enabled = child != null)
        }
        child?.let {
            if (it.bonusMinutes > 0) {
                Text(Lang.t("Hôm nay đã cho thêm ${it.bonusMinutes} phút.", "${it.bonusMinutes} extra min granted today."), color = Ui.Soft, fontSize = 12.sp)
            }
        }
    }
    Text(
        Lang.t(
            "Khi khóa, máy con chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa.",
            "While locked, the child's phone only has: call parent, flashlight, SOS, request unlock."
        ),
        color = Ui.Soft, fontSize = 12.sp
    )
    SectionCard(Lang.t("Tài khoản", "Account")) {
        SecondaryButton(Lang.t("Hủy ghép nối máy con", "Unpair child's phone"), { confirmUnpair = true }, color = Ui.Red)
        SecondaryButton(Lang.t("Đăng xuất", "Sign out"), { viewModel.signOutParent(onSignedOut) })
    }
    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text(Lang.t("Hủy ghép nối?", "Unpair?")) },
            text = {
                Text(
                    Lang.t(
                        "Máy con sẽ ngừng được quản lý. Muốn quản lý lại phải ghép nối từ đầu.",
                        "The child's phone will no longer be managed. You must pair again to manage it."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnpair = false
                    viewModel.unpairChild(onUnpaired)
                }) { Text(Lang.t("Hủy ghép nối", "Unpair"), color = Ui.Red) }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text(Lang.t("Giữ lại", "Keep")) } }
        )
    }
}
