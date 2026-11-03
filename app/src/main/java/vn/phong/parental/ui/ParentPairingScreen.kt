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
import vn.phong.parental.data.Pairing
import vn.phong.parental.data.PairingCode

/**
 * Bên bố mẹ: đăng nhập Firebase Auth, đặt mã PIN, rồi tạo mã ghép nối.
 * Khi có ViewModel thì dùng mã Firebase thật (PairingRepository),
 * không có thì dùng mã mẫu để chạy thử giao diện.
 * QR sẽ vẽ từ mã này ở bước sau.
 */
@Composable
fun ParentPairingScreen(
    viewModel: ParentalViewModel? = null,
    onPaired: () -> Unit
) {
    var sampleCode by remember { mutableStateOf<PairingCode?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val firebaseCode by viewModel?.pairingCode?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Vai trò: Bố Mẹ")
        Spacer(Modifier.height(8.dp))
        if (viewModel != null) {
            Text("Bước 1: đăng nhập Firebase Auth.")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email bố mẹ") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Mật khẩu") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.parentSignIn(email, password) {} }) {
                Text("Đăng nhập")
            }
            Spacer(Modifier.height(8.dp))
            Text("Bước 2: tạo mã ghép nối Firebase thật, hiệu lực 10 phút.")
            Button(onClick = { viewModel.createPairingCode() }) {
                Text("Tạo mã ghép nối mới")
            }
            firebaseCode?.let {
                Spacer(Modifier.height(16.dp))
                Text("Mã ghép nối: $it")
                Text("Đưa mã này cho máy con nhập. Khi máy con nhập mã, anh bấm Duyệt.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.approveCurrentCode { onPaired() } }) {
                    Text("Duyệt máy con, vào app")
                }
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text("Lỗi: $it")
            }
        } else {
            Text("Bước 1: đăng nhập và đặt mã PIN quản lý.")
            Text("Bước 2: tạo mã ghép nối cho máy con.")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { sampleCode = Pairing.generateCode() }) {
                Text("Tạo mã ghép nối mới")
            }
            sampleCode?.let {
                Spacer(Modifier.height(16.dp))
                Text("Mã ghép nối: ${it.code}")
                Text("Hiệu lực 10 phút. Đưa mã này cho máy con nhập hoặc quét QR.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPaired) { Text("Đã duyệt máy con, vào app") }
            }
        }
    }
}
