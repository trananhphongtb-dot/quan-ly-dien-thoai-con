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

    ScreenScaffold(Lang.t("Con", "Child"), Lang.t("Nhập mã do bố mẹ đưa", "Enter the code from your parent")) {
        error?.let { Notice(Lang.t("Lỗi: $it", "Error: $it"), NoticeKind.ERROR) }
        SectionCard(Lang.t("Ghép nối với máy bố mẹ", "Pair with the parent phone"), Lang.t("Mã 6 số hiện trên máy của bố mẹ, có hiệu lực 10 phút.", "The 6-digit code is shown on the parent phone, valid 10 minutes.")) {
            OutlinedTextField(
                value = deviceName, onValueChange = { deviceName = it.take(30) },
                label = { Text(Lang.t("Tên của con (ví dụ: Máy của Bin)", "Child name (e.g. Bin's phone)")) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isDigit() }.take(6) },
                label = { Text(Lang.t("Mã ghép nối 6 số", "6-digit pairing code")) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                if (sending) Lang.t("Đang gửi...", "Sending...") else Lang.t("Gửi yêu cầu ghép nối", "Send pairing request"),
                {
                    sending = true
                    viewModel.clearError()
                    viewModel.childClaimCode(code, deviceName.ifBlank { Lang.t("Máy của con", "Child phone") }) {
                        sending = false
                        onRequestSent()
                    }
                },
                enabled = code.length == 6 && !sending
            )
            Text(Lang.t("Sau khi gửi, bố mẹ phải bấm Duyệt thì hai máy mới nối xong.", "After sending, your parent must tap Approve to finish pairing."), color = Ui.Soft, fontSize = 12.sp)
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
    ScreenScaffold(Lang.t("Con", "Child"), Lang.t("Đang chờ bố mẹ", "Waiting for your parent")) {
        SectionCard(Lang.t("Đang chờ bố mẹ duyệt", "Waiting for approval")) {
            Text(
                Lang.t(
                    "Nhờ bố mẹ mở app trên máy của bố mẹ và bấm Duyệt. Duyệt xong màn này tự chuyển sang bước cấp quyền.",
                    "Ask your parent to open the app on their phone and tap Approve. This screen moves on automatically."
                ),
                color = Ui.Soft, fontSize = 13.sp
            )
            LinearWaiting()
            SecondaryButton(Lang.t("Hủy, nhập lại mã", "Cancel, re-enter code"), onCancel)
        }
    }
}

@Composable
private fun LinearWaiting() {
    androidx.compose.material3.LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}
