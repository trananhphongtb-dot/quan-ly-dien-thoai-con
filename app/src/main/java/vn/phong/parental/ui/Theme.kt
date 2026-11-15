package vn.phong.parental.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bảng màu dùng chung cho mọi màn hình, để giao diện đồng nhất. */
object Ui {
    val Primary = Color(0xFF1565C0)
    val PrimaryLight = Color(0xFF2F80E0)
    val PrimaryDark = Color(0xFF0B3C8C)
    val PrimarySoft = Color(0xFFE3EEFB)
    val Bg = Color(0xFFEDF2FA)
    val Text = Color(0xFF14213D)
    val Soft = Color(0xFF64748B)
    val Line = Color(0xFFDDE5F0)
    val Green = Color(0xFF1B8A4B)
    val GreenSoft = Color(0xFFE6F4EA)
    val Red = Color(0xFFC62828)
    val RedSoft = Color(0xFFFDECEA)
    val Amber = Color(0xFF8A5A00)
    val AmberSoft = Color(0xFFFFF4DB)
    val Shadow = Color(0xFF1B3A6B)
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ui.Primary,
            onPrimary = Color.White,
            secondary = Ui.Primary,
            background = Ui.Bg,
            surface = Color.White,
            onSurface = Ui.Text,
            error = Ui.Red
        ),
        content = content
    )
}

/** Hiệu ứng nổi: đổ bóng xanh đậm nhẹ + viền sáng, tạo cảm giác 3D. */
fun Modifier.raised(radius: Int = 18, elevation: Int = 6): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .shadow(elevation.dp, shape, ambientColor = Ui.Shadow.copy(alpha = 0.25f), spotColor = Ui.Shadow.copy(alpha = 0.35f))
        .clip(shape)
}

private fun lighter(c: Color) = Color(
    red = c.red + (1f - c.red) * 0.22f,
    green = c.green + (1f - c.green) * 0.22f,
    blue = c.blue + (1f - c.blue) * 0.22f,
    alpha = c.alpha
)

private fun darker(c: Color) = Color(red = c.red * 0.72f, green = c.green * 0.72f, blue = c.blue * 0.72f, alpha = c.alpha)

/** Nút chọn ngôn ngữ nhỏ gọn: VI | EN. */
@Composable
fun LangToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val en = Lang.en
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(2.dp)
    ) {
        listOf("VI" to false, "EN" to true).forEach { (label, isEn) ->
            val selected = en == isEn
            Text(
                label,
                color = if (selected) Ui.PrimaryDark else Color.White,
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { Lang.set(context, isEn) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

/** Dải tiêu đề phía trên: nền chuyển màu, bo tròn đáy, có nút ngôn ngữ. */
@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showLang: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), spotColor = Ui.Shadow)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(Ui.PrimaryLight, Ui.PrimaryDark)))
            .statusBarsPadding()
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, color = Color(0xFFD3E4F9), fontSize = 12.sp, maxLines = 1)
            }
        }
        trailing?.invoke()
        if (showLang) {
            Spacer(Modifier.width(8.dp))
            LangToggle()
        }
    }
}

/** Khung màn hình: tiêu đề gọn ở trên, nội dung cuộn bên dưới. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        AppHeader(title, subtitle)
        val base = Modifier.fillMaxWidth().weight(1f)
        Column(
            modifier = (if (scroll) base.verticalScroll(rememberScrollState()) else base)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Thẻ trắng nổi, bo tròn, có tiêu đề tùy chọn. */
@Composable
fun SectionCard(
    title: String? = null,
    hint: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .raised(18, 5)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF8FBFF))))
            .border(1.dp, Color.White, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (title != null) Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ui.Text)
        if (hint != null) Text(hint, fontSize = 12.sp, color = Ui.Soft)
        content()
    }
}

enum class NoticeKind { ERROR, SUCCESS, WARN, INFO }

/** Hộp thông báo màu: lỗi, thành công, cảnh báo, thông tin. */
@Composable
fun Notice(text: String, kind: NoticeKind, action: (@Composable () -> Unit)? = null) {
    val (bg, fg) = when (kind) {
        NoticeKind.ERROR -> Ui.RedSoft to Ui.Red
        NoticeKind.SUCCESS -> Ui.GreenSoft to Ui.Green
        NoticeKind.WARN -> Ui.AmberSoft to Ui.Amber
        NoticeKind.INFO -> Ui.PrimarySoft to Ui.PrimaryDark
    }
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 13.sp, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

/** Nút chính kiểu 3D: nền chuyển màu, cạnh đáy đậm, nhấn thì lún xuống. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Ui.Primary
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val base = if (enabled) color else Color(0xFFB8C2D1)
    val shape = RoundedCornerShape(14.dp)
    val lift = if (pressed) 1.dp else 4.dp
    Box(
        modifier
            .fillMaxWidth()
            .shadow(if (pressed) 2.dp else 6.dp, shape, spotColor = darker(base))
            .clip(shape)
            .background(darker(base))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = lift)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(lighter(base), base)))
                .height(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Nút phụ: trắng nổi, viền mảnh. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Ui.Text
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, spotColor = Ui.Shadow.copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5FB))))
            .border(1.dp, Ui.Line, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) color else Ui.Soft, maxLines = 1)
    }
}

/** Thanh tab dưới cùng: biểu tượng trong ô bo tròn, tab đang chọn nổi màu và đậm chữ. */
data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun AppTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), spotColor = Ui.Shadow)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, item ->
            val on = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(width = 52.dp, height = 30.dp)
                        .then(
                            if (on) Modifier
                                .shadow(5.dp, CircleShape, spotColor = Ui.Primary)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.verticalGradient(listOf(Ui.PrimaryLight, Ui.Primary)))
                            else Modifier.clip(RoundedCornerShape(50)).background(Ui.PrimarySoft.copy(alpha = 0.6f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = if (on) Color.White else Ui.Soft, modifier = Modifier.size(20.dp))
                }
                Text(
                    item.label, fontSize = 11.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    color = if (on) Ui.Primary else Ui.Soft, maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
