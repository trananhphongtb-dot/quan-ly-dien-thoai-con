package vn.phong.parental.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import vn.phong.parental.model.Role

/** Màn đầu tiên khi cài app: chọn vai Bố Mẹ hoặc Con. */
@Composable
fun RoleSelectionScreen(onRoleSelected: (Role) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Ui.Bg).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Ui.Primary),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(vn.phong.parental.R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(96.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Quản lý điện thoại con", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ui.Primary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Bạn dùng máy này với vai trò nào?", fontSize = 15.sp, color = Ui.Soft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        RoleCard("Tôi là Bố Mẹ", "Đặt luật, theo dõi và khóa máy của con", Ui.Primary, Color.White) { onRoleSelected(Role.PARENT) }
        Spacer(Modifier.height(12.dp))
        RoleCard("Tôi là Con", "Nhập mã do bố mẹ đưa để ghép nối", Color.White, Ui.Text) { onRoleSelected(Role.CHILD) }
        Spacer(Modifier.height(20.dp))
        Text(
            "Chọn đúng vai ngay từ đầu. Sau khi ghép nối, máy con không tự đổi sang vai Bố Mẹ được.",
            fontSize = 13.sp, color = Ui.Soft, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoleCard(title: String, sub: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = fg)
        Spacer(Modifier.height(4.dp))
        Text(sub, fontSize = 13.sp, color = if (bg == Color.White) Ui.Soft else Color(0xFFCFE0F6))
    }
}
