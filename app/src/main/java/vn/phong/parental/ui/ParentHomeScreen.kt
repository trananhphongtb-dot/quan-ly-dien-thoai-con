package vn.phong.parental.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.phong.parental.data.Rules
import vn.phong.parental.model.ChildDevice

private val BgBlue = Color(0xFFEAF2FB)
private val MainBlue = Color(0xFF1565C0)
private val DarkText = Color(0xFF1A2A3A)
private val SoftText = Color(0xFF5A6B7D)
private val Green = Color(0xFF1B8A4B)
private val Red = Color(0xFFB3261E)

/**
 * App bên bố mẹ, đúng 4 tab đã chốt:
 * 1. Theo dõi & Thống kê — 2. Giới hạn chung — 3. Giới hạn từng app — 4. Khóa máy.
 * Giao diện thẻ trắng bo tròn trên nền xanh nhạt, chữ lớn, nút to dễ bấm.
 */
@Composable
fun ParentHomeScreen(
    child: ChildDevice = Rules.sampleChild,
    viewModel: ParentalViewModel? = null,
    onLockChanged: (Boolean) -> Unit = {}
) {
    val liveChild by viewModel?.child?.collectAsState() ?: remember { mutableStateOf<ChildDevice?>(null) }
    val liveGeneral by viewModel?.generalLimit?.collectAsState() ?: remember { mutableStateOf(Rules.generalLimit) }
    val livePerApp by viewModel?.perAppLimits?.collectAsState() ?: remember { mutableStateOf(Rules.samplePerAppLimits) }
    val effectiveChild = liveChild ?: child
    val effectiveGeneral = liveGeneral
    val effectivePerApp = if (livePerApp.isNotEmpty()) livePerApp else Rules.samplePerAppLimits
    var tab by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(effectiveChild.isLocked) }
    val titles = listOf("Theo dõi", "Giới hạn chung", "Từng app", "Khóa máy")

    Column(modifier = Modifier.fillMaxSize().background(BgBlue)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MainBlue),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Máy của ${effectiveChild.name}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (effectiveChild.isLocked) "Đang bị khóa" else "Đang mở",
                    color = Color(0xFFD6E6F7),
                    fontSize = 15.sp
                )
            }
        }
        TabRow(selectedTabIndex = tab, containerColor = Color.White) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title, fontSize = 13.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (tab) {
                0 -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard("Hôm nay", "${effectiveChild.todayTotalMinutes} phút", Modifier.weight(1f))
                        StatCard("Mở máy", effectiveChild.firstUnlockToday, Modifier.weight(1f))
                        StatCard("Đóng máy", effectiveChild.lastLockToday, Modifier.weight(1f))
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Đang dùng", color = SoftText, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(effectiveChild.currentApp, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DarkText)
                            Text("Từ ${effectiveChild.currentAppSince}", color = SoftText, fontSize = 14.sp)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Top app hôm nay", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
                            Spacer(Modifier.height(10.dp))
                            Rules.sampleStats.forEach {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                                    Text(it.appName, modifier = Modifier.weight(1f), color = DarkText, fontSize = 15.sp)
                                    Text("${it.minutesToday} phút · mở ${it.opensToday} lần", color = SoftText, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val limit = effectiveGeneral
                    InfoCard("Tổng mỗi ngày", "${limit.dailyTotalMinutes} phút")
                    InfoCard("Giờ ngủ khóa máy", "${limit.bedtimeStart} – ${limit.bedtimeEnd}")
                    InfoCard("Giờ học khóa máy", "${limit.studyLockStart} – ${limit.studyLockEnd}")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Hết tổng giờ thì khóa máy, chỉ chừa gọi cho bố mẹ.",
                            color = SoftText,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
                2 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Giới hạn từng app", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
                            Spacer(Modifier.height(10.dp))
                            effectivePerApp.forEach {
                                Column(Modifier.padding(vertical = 7.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(it.appName, modifier = Modifier.weight(1f), color = DarkText, fontSize = 15.sp)
                                        Text(
                                            "${it.usedMinutesToday}/${it.dailyMinutes} phút",
                                            color = if (it.isOutOfTime) Red else SoftText,
                                            fontSize = 14.sp
                                        )
                                    }
                                    if (it.isMandatory) {
                                        Text("App bắt buộc", color = MainBlue, fontSize = 13.sp)
                                    }
                                    if (it.isOutOfTime) {
                                        Text("Hết giờ, đã khóa", color = Red, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (it.dailyMinutes > 0) {
                                                (it.usedMinutesToday.toFloat() / it.dailyMinutes).coerceIn(0f, 1f)
                                            } else 0f
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Khung 19:00–20:00 chỉ mở được app học.", color = SoftText, fontSize = 14.sp)
                        }
                    }
                }
                3 -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (locked) Color(0xFFFDECEA) else Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                if (locked) "Máy con đang bị khóa." else "Máy con đang mở.",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (locked) Red else Green
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Lý do khóa sẽ hiện lên máy con.", color = SoftText, fontSize = 14.sp)
                        }
                    }
                    Button(
                        onClick = {
                            locked = !locked
                            if (viewModel != null) {
                                viewModel.setLocked(locked, if (locked) "Bố mẹ khóa máy" else "")
                            }
                            onLockChanged(locked)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (locked) Green else Red),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(
                            if (locked) "Mở khóa máy con" else "Khóa máy con ngay",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Khi khóa, máy con chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa.",
                        color = SoftText,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = SoftText, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkText)
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp)) {
            Text(label, modifier = Modifier.weight(1f), color = DarkText, fontSize = 16.sp)
            Text(value, fontWeight = FontWeight.Bold, color = MainBlue, fontSize = 16.sp)
        }
    }
}
