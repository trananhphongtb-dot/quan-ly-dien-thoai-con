package vn.phong.parental.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Màn hình khóa bên máy con — Tab 4.
 * Chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa / xin thêm giờ.
 */
@Composable
fun LockScreen(
    lockReason: String = "Hết giờ chơi, mai dùng tiếp nhé",
    onCallParent: () -> Unit = {},
    onToggleFlashlight: () -> Unit = {},
    onSos: () -> Unit = {},
    onRequestUnlock: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102A43))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Máy đang bị khóa",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A56)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                lockReason,
                color = Color(0xFFD6E6F7),
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(20.dp).fillMaxWidth()
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onCallParent,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B8A4B)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Gọi bố mẹ", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onToggleFlashlight,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Đèn pin", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSos,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("SOS — gửi vị trí cho bố mẹ", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRequestUnlock,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Xin mở khóa / xin thêm giờ", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White) }
    }
}
