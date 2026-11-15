package vn.phong.parental.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.phong.parental.R
import vn.phong.parental.model.Role

/** Màn đầu tiên khi cài app: chọn vai Bố Mẹ hoặc Con. */
@Composable
fun RoleSelectionScreen(onRoleSelected: (Role) -> Unit) {
    Column(Modifier.fillMaxSize().background(Ui.Bg)) {
        // Nửa trên: logo + tên app trên nền chuyển màu
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp), spotColor = Ui.Shadow)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(Brush.verticalGradient(listOf(Ui.PrimaryLight, Ui.PrimaryDark)))
                .statusBarsPadding()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { LangToggle() }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.size(84.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = Color.Black)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(84.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                Lang.t("Quản lý điện thoại con", "Kids Phone Guard"),
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                Lang.t("Đặt giờ, theo dõi và khóa máy an toàn", "Set limits, monitor and lock safely"),
                fontSize = 13.sp, color = Color(0xFFD3E4F9), textAlign = TextAlign.Center
            )
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                Lang.t("Máy này là của ai?", "Whose phone is this?"),
                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ui.Text,
                modifier = Modifier.padding(top = 6.dp)
            )
            RoleCard(
                Icons.Filled.Person,
                Lang.t("Tôi là Bố Mẹ", "I'm a Parent"),
                Lang.t("Đặt luật, theo dõi và khóa máy của con", "Set rules, monitor and lock your child's phone"),
                Ui.Primary
            ) { onRoleSelected(Role.PARENT) }
            RoleCard(
                Icons.Filled.Phone,
                Lang.t("Tôi là Con", "I'm the Child"),
                Lang.t("Nhập mã do bố mẹ đưa để ghép nối", "Enter the code from your parent to pair"),
                Ui.Green
            ) { onRoleSelected(Role.CHILD) }
            Spacer(Modifier.weight(1f))
            Text(
                Lang.t(
                    "Chọn đúng vai ngay từ đầu. Sau khi ghép nối, máy con không tự đổi sang vai Bố Mẹ được.",
                    "Pick the right role now. After pairing, a child phone cannot switch to Parent."
                ),
                fontSize = 12.sp, color = Ui.Soft, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RoleCard(icon: ImageVector, title: String, sub: String, accent: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(7.dp, shape, spotColor = Ui.Shadow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF4F8FE))))
            .border(1.dp, Color.White, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(52.dp)
                .shadow(6.dp, CircleShape, spotColor = accent)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.75f), accent))),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ui.Text)
            Text(sub, fontSize = 12.sp, color = Ui.Soft)
        }
    }
}
