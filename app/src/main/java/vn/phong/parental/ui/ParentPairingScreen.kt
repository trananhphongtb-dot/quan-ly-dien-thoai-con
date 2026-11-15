package vn.phong.parental.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Nút "Đăng nhập bằng Google": trắng nổi, chữ G nhiều màu trong vòng tròn. */
@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, spotColor = Ui.Shadow)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Ui.Line, shape)
            .clickable(enabled = !loading, onClick = onClick)
            .padding(horizontal = 14.dp)
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFF1F5FB)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "G", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))
                    )
                )
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (loading) Lang.t("Đang mở Google...", "Opening Google...")
            else Lang.t("Đăng nhập bằng Google", "Continue with Google"),
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ui.Text
        )
    }
}

/**
 * Bên bố mẹ: đăng nhập (Google ở trên cùng, email bên dưới), rồi tạo mã ghép nối
 * và duyệt máy con. Chưa đăng nhập thì chỉ hiện khung đăng nhập.
 */
@Composable
fun ParentPairingScreen(
    viewModel: ParentalViewModel,
    onPaired: () -> Unit,
    onSignedOut: () -> Unit = {},
    /** Có khi đang thêm máy con mới: cho phép quay lại màn chính mà không ghép nối. */
    onBack: (() -> Unit)? = null
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
                        viewModel.setError(Lang.t("Google không trả về mã xác thực, bấm đăng nhập lại.", "Google returned no token, try again."))
                    } else {
                        viewModel.parentGoogleSignIn(token) {}
                    }
                } else {
                    viewModel.setError(Lang.t("Không lấy được tài khoản Google, thử lại.", "Could not get the Google account, try again."))
                }
            } catch (e: GetCredentialCancellationException) {
                // Bố mẹ tự đóng hộp chọn tài khoản, không coi là lỗi.
            } catch (e: NoCredentialException) {
                viewModel.setError(
                    Lang.t(
                        "Máy chưa có tài khoản Google nào để chọn. Thêm tài khoản Google trong Cài đặt máy, hoặc đăng nhập bằng email.",
                        "No Google account on this phone. Add one in Settings, or sign in with email."
                    )
                )
            } catch (e: Exception) {
                viewModel.setError(
                    Lang.t(
                        "Đăng nhập Google lỗi (${e.javaClass.simpleName}): ${e.message ?: "không rõ"}. Nếu lỗi lặp lại, nhờ người cài app kiểm tra SHA-1 trong Firebase.",
                        "Google sign-in failed (${e.javaClass.simpleName}): ${e.message ?: "unknown"}. If it repeats, check the SHA-1 in Firebase."
                    )
                )
            } finally {
                googleLoading = false
            }
        }
    }

    ScreenScaffold(Lang.t("Bố Mẹ", "Parent"), Lang.t("Đăng nhập rồi ghép nối với máy con", "Sign in, then pair with your child's phone")) {
        error?.let {
            Notice(it, NoticeKind.ERROR) {
                TextButton(onClick = { viewModel.clearError() }) { Text(Lang.t("Đóng", "Close")) }
            }
        }

        if (signedInEmail == null) {
            SectionCard {
                GoogleButton(googleLoading) { googleSignIn() }
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(Ui.Line))
                    Text(
                        Lang.t("hoặc dùng email", "or use email"),
                        color = Ui.Soft, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(Ui.Line))
                }
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text(Lang.t("Mật khẩu", "Password")) }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(Lang.t("Đăng nhập", "Sign in"), { viewModel.parentSignIn(email, password) {} })
                SecondaryButton(
                    Lang.t("Tạo tài khoản mới (mật khẩu từ 6 ký tự)", "Create account (password 6+ chars)"),
                    { viewModel.parentSignUp(email, password) {} }
                )
            }
        } else {
            Notice(Lang.t("Đã đăng nhập: $signedInEmail", "Signed in: $signedInEmail"), NoticeKind.SUCCESS)
            SectionCard(
                Lang.t("Ghép nối máy con", "Pair a child phone"),
                Lang.t("Mã có hiệu lực 10 phút. Đưa mã cho máy con nhập.", "The code is valid for 10 minutes. Enter it on the child's phone.")
            ) {
                PrimaryButton(
                    if (code == null) Lang.t("Tạo mã ghép nối", "Create pairing code")
                    else Lang.t("Tạo mã mới", "New code"),
                    { viewModel.createPairingCode() }, color = Ui.Green
                )
                code?.let {
                    Text(
                        it.chunked(3).joinToString(" "),
                        fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Ui.Primary,
                        letterSpacing = 4.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                    if (status == "WAITING_APPROVAL") {
                        Notice(
                            Lang.t("Máy \"$childName\" đã nhập mã. Bấm Duyệt để ghép nối.", "\"$childName\" entered the code. Tap Approve to pair."),
                            NoticeKind.SUCCESS
                        )
                    } else {
                        Notice(Lang.t("Đang chờ máy con nhập mã...", "Waiting for the child to enter the code..."), NoticeKind.INFO)
                    }
                    PrimaryButton(
                        Lang.t("Duyệt máy con, vào app", "Approve and continue"),
                        { viewModel.approveCurrentCode { onPaired() } },
                        enabled = status == "WAITING_APPROVAL"
                    )
                }
            }
            if (onBack != null) {
                SecondaryButton(Lang.t("Quay lại", "Back"), onBack)
            }
            TextButton(
                onClick = { viewModel.signOutParent(onSignedOut) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text(Lang.t("Đăng xuất", "Sign out"), color = Ui.Soft) }
        }
    }
}
