package vn.phong.parental.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgBlue = Color(0xFFEAF2FB)
private val MainBlue = Color(0xFF1565C0)
private val DarkText = Color(0xFF1A2A3A)
private val SoftText = Color(0xFF5A6B7D)
private val ErrorBg = Color(0xFFFDECEA)
private val ErrorText = Color(0xFFB3261E)

/** Bên con: nhập mã 6 số, đặt tên máy, gửi yêu cầu rồi chờ bố mẹ duyệt. */
@Composable
fun ChildPairingScreen(
    viewModel: ParentalViewModel,
    onRequestSent: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsState()
    androidx.compose.runtime.LaunchedEffect(error) { if (error != null) sending = false }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBlue)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MainBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Quản lý điện thoại con", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Vai trò: Con", color = Color(0xFFD6E6F7), fontSize = 15.sp)
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Nhập mã ghép nối", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nhập mã 6 số mà bố mẹ đang hiện trên máy của bố mẹ.",
                    color = SoftText, fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = { deviceName = it.take(30) },
                    label = { Text("Tên của con (ví dụ: Máy của Bin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                    label = { Text("Mã ghép nối 6 số") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        sending = true
                        viewModel.clearError()
                        viewModel.childClaimCode(code, deviceName.ifBlank { "Máy của con" }) {
                            sending = false
                            onRequestSent()
                        }
                    },
                    enabled = code.length == 6 && !sending,
                    colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(if (sending) "Đang gửi..." else "Gửi yêu cầu ghép nối", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Text("Sau khi gửi, bố mẹ phải bấm Duyệt thì 2 máy mới nối xong.", color = SoftText, fontSize = 13.sp)
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Lỗi: $it", color = ErrorText, fontSize = 14.sp, modifier = Modifier.padding(12.dp)) }
                }
            }
        }
    }
}

/** Máy con chờ bố mẹ duyệt. Tự chuyển sang cấp quyền khi được duyệt. */
@Composable
fun ChildWaitingScreen(
    viewModel: ParentalViewModel,
    onApproved: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.childWaitForApproval(onApproved) }
    Column(
        modifier = Modifier.fillMaxSize().background(BgBlue).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Đang chờ bố mẹ duyệt", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkText)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nhờ bố mẹ mở app trên máy của bố mẹ và bấm Duyệt. " +
                        "Duyệt xong màn này tự chuyển sang bước cấp quyền.",
                    color = SoftText, fontSize = 15.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Hủy, nhập lại mã", color = DarkText) }
            }
        }
    }
}
