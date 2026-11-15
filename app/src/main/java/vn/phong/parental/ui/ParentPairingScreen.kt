package vn.phong.parental.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import vn.phong.parental.R

/**
 * Bên bố mẹ: đăng nhập (email hoặc Google), rồi tạo mã ghép nối và duyệt máy con.
 * Chưa đăng nhập thì chỉ hiện khung đăng nhập; đăng nhập xong mới hiện khung ghép nối.
 */
@Composable
fun ParentPairingScreen(
    viewModel: ParentalViewModel,
    onPaired: () -> Unit,
    onSignedOut: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val code by viewModel.pairingCode.collectAsState()
    val error by viewModel.error.collectAsState()
    val signedInEmail by viewModel.signedInEmail.collectAsState()
    val status by viewModel.pairingStatus.collectAsState()
    val childName by viewModel.pairingChildName.collectAsState()

    fun googleSignIn() {
        if (googleLoading) return
        googleLoading = true
        viewModel.clearError()
        scope.launch {
            try {
                val webClientId = context.getString(R.string.default_web_client_id)
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val result = CredentialManager.create(context).getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    if (token.isBlank()) {
                        viewModel.setError("Google không trả về mã xác thực, bấm đăng nhập lại.")
                    } else {
                        viewModel.parentGoogleSignIn(token) {}
                    }
                } else {
                    viewModel.setError("Không lấy được tài khoản Google, thử lại.")
                }
            } catch (e: GetCredentialCancellationException) {
                // Bố mẹ tự đóng hộp chọn tài khoản, không coi là lỗi.
            } catch (e: NoCredentialException) {
                viewModel.setError(
                    "Máy chưa có tài khoản Google nào để chọn. Thêm tài khoản Google trong Cài đặt máy, " +
                        "hoặc đăng nhập bằng email."
                )
            } catch (e: Exception) {
                viewModel.setError(
                    "Đăng nhập Google lỗi (${e.javaClass.simpleName}): ${e.message ?: "không rõ"}. " +
                        "Nếu lỗi lặp lại, nhờ người cài app kiểm tra SHA-1 trong Firebase."
                )
            } finally {
                googleLoading = false
            }
        }
    }

    ScreenScaffold("Bố Mẹ", "Đăng nhập rồi ghép nối với máy con") {
        error?.let { Notice(it, NoticeKind.ERROR) { TextButton(onClick = { viewModel.clearError() }) { Text("Đóng") } } }

        if (signedInEmail == null) {
            SectionCard("Đăng nhập", "Dùng email đã đăng ký hoặc tài khoản Google.") {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Mật khẩu") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton("Đăng nhập", { viewModel.parentSignIn(email, password) {} })
                SecondaryButton("Tạo tài khoản mới (mật khẩu từ 6 ký tự)", { viewModel.parentSignUp(email, password) {} })
                Text("hoặc", color = Ui.Soft, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                SecondaryButton(
                    if (googleLoading) "Đang mở Google..." else "Đăng nhập bằng Google",
                    { googleSignIn() }, enabled = !googleLoading
                )
            }
        } else {
            Notice("Đã đăng nhập: $signedInEmail", NoticeKind.SUCCESS)
            SectionCard("Ghép nối máy con", "Mã có hiệu lực 10 phút. Đưa mã cho máy con nhập.") {
                PrimaryButton(
                    if (code == null) "Tạo mã ghép nối" else "Tạo mã mới",
                    { viewModel.createPairingCode() }, color = Ui.Green
                )
                code?.let {
                    Text(
                        it.chunked(3).joinToString(" "),
                        fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Ui.Primary,
                        letterSpacing = 4.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    if (status == "WAITING_APPROVAL") {
                        Notice("Máy \"$childName\" đã nhập mã. Bấm Duyệt để ghép nối.", NoticeKind.SUCCESS)
                    } else {
                        Notice("Đang chờ máy con nhập mã...", NoticeKind.INFO)
                    }
                    PrimaryButton(
                        "Duyệt máy con, vào app",
                        { viewModel.approveCurrentCode { onPaired() } },
                        enabled = status == "WAITING_APPROVAL"
                    )
                }
            }
            TextButton(
                onClick = { viewModel.signOutParent(onSignedOut) },
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
            ) { Text("Đăng xuất", color = Ui.Soft) }
        }
    }
}
