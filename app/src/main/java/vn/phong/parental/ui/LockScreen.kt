package vn.phong.parental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Màn hình khóa bên máy con — Tab 4.
 * Chỉ còn: gọi bố mẹ, đèn pin, SOS, xin mở khóa / xin thêm giờ.
 * Nút gọi và đèn pin sẽ nối Intent gọi điện và CameraManager ở bước sau.
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Máy đang bị khóa")
        Spacer(Modifier.height(8.dp))
        Text(lockReason)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onCallParent, modifier = Modifier.fillMaxWidth()) {
            Text("Gọi bố mẹ")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onToggleFlashlight, modifier = Modifier.fillMaxWidth()) {
            Text("Đèn pin")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onSos, modifier = Modifier.fillMaxWidth()) {
            Text("SOS — gửi vị trí cho bố mẹ")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRequestUnlock, modifier = Modifier.fillMaxWidth()) {
            Text("Xin mở khóa / xin thêm giờ")
        }
    }
}
