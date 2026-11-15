package vn.phong.parental.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.phong.parental.R

/** Màn đầu tiên khi mở app lần đầu: chọn ngôn ngữ. Sau đó mới tới chọn vai. */
@Composable
fun LanguageScreen(onPicked: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ui.PrimaryLight, Ui.PrimaryDark)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(88.dp)
                .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) { Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(88.dp)) }
        Spacer(Modifier.height(18.dp))
        Text(
            "Chọn ngôn ngữ\nChoose your language",
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(26.dp))
        PrimaryButton("Tiếng Việt", { Lang.set(context, false); onPicked() }, color = Ui.Green)
        Spacer(Modifier.height(14.dp))
        PrimaryButton("English", { Lang.set(context, true); onPicked() }, color = Ui.Primary)
        Spacer(Modifier.height(18.dp))
        Text(
            "Đổi lại được bất cứ lúc nào bằng nút VI | EN\nYou can change it anytime with the VI | EN button",
            color = Color(0xFFD3E4F9), fontSize = 12.sp, textAlign = TextAlign.Center
        )
    }
}
