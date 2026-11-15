package vn.phong.parental.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    LaunchedEffect(error) { if (error != null) sending = false }

    ScreenScaffold("Con", "Nhập mã do bố mẹ đưa") {
        error?.let { Notice("Lỗi: $it", NoticeKind.ERROR) }
        SectionCard("Ghép nối với máy bố mẹ", "Mã 6 số hiện trên máy của bố mẹ, có hiệu lực 10 phút.") {
            OutlinedTextField(
                value = deviceName, onValueChange = { deviceName = it.take(30) },
                label = { Text("Tên của con (ví dụ: Máy của Bin)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                label = { Text("Mã ghép nối 6 số") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                if (sending) "Đang gửi..." else "Gửi yêu cầu ghép nối",
                {
                    sending = true
                    viewModel.clearError()
                    viewModel.childClaimCode(code, deviceName.ifBlank { "Máy của con" }) {
                        sending = false
                        onRequestSent()
                    }
                },
                enabled = code.length == 6 && !sending
            )
            Text("Sau khi gửi, bố mẹ phải bấm Duyệt thì hai máy mới nối xong.", color = Ui.Soft, fontSize = 13.sp)
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
    LaunchedEffect(Unit) { viewModel.childWaitForApproval(onApproved) }
    ScreenScaffold("Con", "Đang chờ bố mẹ") {
        SectionCard("Đang chờ bố mẹ duyệt") {
            Text(
                "Nhờ bố mẹ mở app trên máy của bố mẹ và bấm Duyệt. " +
                    "Duyệt xong màn này tự chuyển sang bước cấp quyền.",
                color = Ui.Soft, fontSize = 14.sp
            )
            LinearWaiting()
            SecondaryButton("Hủy, nhập lại mã", onCancel)
        }
    }
}

@Composable
private fun LinearWaiting() {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
