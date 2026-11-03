package vn.phong.parental.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.phong.parental.data.Rules
import vn.phong.parental.model.ChildDevice

/**
 * App bên bố mẹ, đúng 4 tab đã chốt:
 * 1. Theo dõi & Thống kê — 2. Giới hạn chung — 3. Giới hạn từng app — 4. Khóa máy.
 */
@Composable
fun ParentHomeScreen(
    child: ChildDevice = Rules.sampleChild,
    viewModel: ParentalViewModel? = null,
    onLockChanged: (Boolean) -> Unit = {}
) {
    // Khi có ViewModel thì dùng dữ liệu Firebase realtime, không có thì dùng mẫu.
    val liveChild by viewModel?.child?.collectAsState() ?: remember { mutableStateOf<ChildDevice?>(null) }
    val liveGeneral by viewModel?.generalLimit?.collectAsState() ?: remember { mutableStateOf(Rules.generalLimit) }
    val livePerApp by viewModel?.perAppLimits?.collectAsState() ?: remember { mutableStateOf(Rules.samplePerAppLimits) }
    val effectiveChild = liveChild ?: child
    val effectiveGeneral = liveGeneral
    val effectivePerApp = if (livePerApp.isNotEmpty()) livePerApp else Rules.samplePerAppLimits
    var tab by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(effectiveChild.isLocked) }
    val titles = listOf("Theo dõi", "Giới hạn chung", "Từng app", "Khóa máy")
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title) }
                )
            }
        }
        Column(modifier = Modifier.padding(24.dp)) {
            when (tab) {
                0 -> {
                    Text("Đang dùng: ${effectiveChild.currentApp} từ ${effectiveChild.currentAppSince}")
                    Text("Hôm nay: ${effectiveChild.todayTotalMinutes} phút")
                    Text("Mở máy: ${effectiveChild.firstUnlockToday} — Đóng máy: ${effectiveChild.lastLockToday}")
                    Spacer(Modifier.height(16.dp))
                    Text("Top app hôm nay:")
                    Rules.sampleStats.forEach {
                        Text("- ${it.appName}: ${it.minutesToday} phút, mở ${it.opensToday} lần")
                    }
                }
                1 -> {
                    val limit = effectiveGeneral
                    Text("Tổng mỗi ngày: ${limit.dailyTotalMinutes} phút")
                    Text("Giờ ngủ khóa máy: ${limit.bedtimeStart} – ${limit.bedtimeEnd}")
                    Text("Giờ học khóa máy: ${limit.studyLockStart} – ${limit.studyLockEnd}")
                    Text("Hết tổng giờ thì khóa máy, chỉ chừa gọi cho bố mẹ.")
                }
                2 -> {
                    Text("Giới hạn từng app:")
                    effectivePerApp.forEach {
                        val note = if (it.isMandatory) " (app bắt buộc)" else ""
                        val status = if (it.isOutOfTime) " — hết giờ, đã khóa" else ""
                        Text("- ${it.appName}: ${it.usedMinutesToday}/${it.dailyMinutes} phút$note$status")
                    }
                    Text("Khung 19:00–20:00 chỉ mở được app học.")
                }
                3 -> {
                    Text(if (locked) "Máy con đang bị khóa." else "Máy con đang mở.")
                    Text("Lý do khóa sẽ hiện lên máy con.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        locked = !locked
                        if (viewModel != null) {
                            viewModel.setLocked(locked, if (locked) "Bố mẹ khóa máy" else "")
                        }
                        onLockChanged(locked)
                    }) {
                        Text(if (locked) "Mở khóa máy con" else "Khóa máy con ngay")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Khi khóa, máy con chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa.")
                }
            }
        }
    }
}
