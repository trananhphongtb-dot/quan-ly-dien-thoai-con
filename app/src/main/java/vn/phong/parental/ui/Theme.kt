package vn.phong.parental.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bảng màu và thành phần dùng chung cho mọi màn hình, để giao diện đồng nhất. */
object Ui {
    val Primary = Color(0xFF1565C0)
    val PrimaryDark = Color(0xFF0D47A1)
    val PrimarySoft = Color(0xFFE3EEFB)
    val Bg = Color(0xFFF3F6FB)
    val Text = Color(0xFF14213D)
    val Soft = Color(0xFF64748B)
    val Line = Color(0xFFE2E8F0)
    val Green = Color(0xFF1B8A4B)
    val GreenSoft = Color(0xFFE6F4EA)
    val Red = Color(0xFFC62828)
    val RedSoft = Color(0xFFFDECEA)
    val Amber = Color(0xFF8A5A00)
    val AmberSoft = Color(0xFFFFF4DB)
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

/** Khung màn hình: dải tiêu đề xanh gọn ở trên, nội dung cuộn bên dưới. */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Ui.Primary)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = Color(0xFFCFE0F6), fontSize = 13.sp)
            }
        }
        val base = Modifier.fillMaxWidth().weight(1f)
        Column(
            modifier = (if (scroll) base.verticalScroll(rememberScrollState()) else base)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Thẻ trắng bo tròn, viền mảnh, có tiêu đề tùy chọn. */
@Composable
fun SectionCard(
    title: String? = null,
    hint: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Ui.Line),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ui.Text)
            if (hint != null) Text(hint, fontSize = 13.sp, color = Ui.Soft)
            content()
        }
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
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = fg, fontSize = 14.sp, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Ui.Primary
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().height(50.dp)
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Ui.Text
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, Ui.Line),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth().height(48.dp)
    ) { Text(text, fontSize = 15.sp, color = color) }
}
