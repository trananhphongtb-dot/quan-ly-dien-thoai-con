package vn.phong.parental.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Bên con: nhập mã 6 số hoặc quét QR (quét QR nối ở bước sau),
 * đặt tên máy, gửi yêu cầu và chờ bố mẹ duyệt.
 * Khi có ViewModel thì nhận mã qua PairingRepository Firebase thật,
 * không có thì chỉ chạy thử giao diện.
 * Sau bước này mới tới màn cấp quyền từng bước.
 */
@Composable
fun ChildPairingScreen(
    viewModel: ParentalViewModel? = null,
    onRequestSent: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("Máy của Bin") }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Vai trò: Con")
        Spacer(Modifier.height(8.dp))
        Text("Nhập mã 6 số do bố mẹ đưa, hoặc quét mã QR.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Tên máy con") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("Mã ghép nối 6 số") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (viewModel != null) {
                    viewModel.childClaimCode(code, deviceName, onRequestSent)
                } else {
                    onRequestSent()
                }
            },
            enabled = code.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Gửi yêu cầu ghép nối") }
        Spacer(Modifier.height(8.dp))
        Text("Sau khi gửi, bố mẹ phải bấm Duyệt thì 2 máy mới nối xong.")
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text("Lỗi: $it")
        }
    }
}
