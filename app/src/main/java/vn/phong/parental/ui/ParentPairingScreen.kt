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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import vn.phong.parental.R
import vn.phong.parental.data.Pairing
import vn.phong.parental.data.PairingCode

private val BgBlue = Color(0xFFEAF2FB)
private val MainBlue = Color(0xFF1565C0)
private val DarkText = Color(0xFF1A2A3A)
private val SoftText = Color(0xFF5A6B7D)
private val ErrorBg = Color(0xFFFDECEA)
private val ErrorText = Color(0xFFB3261E)
private val Green = Color(0xFF1B8A4B)

/**
 * Bên bố mẹ: đăng nhập Firebase Auth, rồi tạo mã ghép nối.
 * Đăng nhập Google dùng Credential Manager (lấy ID token mới mỗi lần),
 * không dùng GoogleSignIn cũ nữa — cách cũ hay trả về token rỗng và
 * Firebase báo "Given String is empty or null".
 */
@Composable
fun ParentPairingScreen(
    viewModel: ParentalViewModel? = null,
    onPaired: () -> Unit
) {
    var sampleCode by remember { mutableStateOf<PairingCode?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var googleLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseCode by viewModel?.pairingCode?.collectAsState() ?: remember { mutableStateOf<String?>(null) }
    val error by viewModel?.error?.collectAsState() ?: remember { mutableStateOf<String?>(null) }

    fun googleSignIn() {
        if (viewModel == null || googleLoading) return
        googleLoading = true
        scope.launch {
            try {
                val webClientId = context.getString(R.string.default_web_client_id)
                if (webClientId.isBlank()) {
                    viewModel.setError("Thiếu mã web client của Firebase, vui lòng báo lại để sửa bản build")
                    return@launch
                }
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    if (idToken.isBlank()) {
                        viewModel.setError("Google không trả về ID token hợp lệ, vui lòng bấm đăng nhập lại")
                    } else {
                        viewModel.parentGoogleSignIn(idToken) {}
                    }
                } else {
                    viewModel.setError("Không lấy được thông tin tài khoản Google, vui lòng thử lại")
                }
            } catch (e: Exception) {
                viewModel.setError(e.message ?: "Đăng nhập Google thất bại, vui lòng thử lại")
            } finally {
                googleLoading = false
            }
        }
    }

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
                Text(
                    "Quản lý điện thoại con",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Vai trò: Bố Mẹ",
                    color = Color(0xFFD6E6F7),
                    fontSize = 15.sp
                )
            }
        }

        if (viewModel != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Bước 1: Đăng nhập", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
                    Spacer(Modifier.height(4.dp))
                    Text("Đăng nhập bằng Google hoặc bằng email đã đăng ký.", color = SoftText, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email bố mẹ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.parentSignIn(email, password) {} },
                        colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Đăng nhập", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { googleSignIn() },
                        enabled = !googleLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            if (googleLoading) "Đang đăng nhập..." else "Đăng nhập bằng Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                    }
                    error?.let {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ErrorBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lỗi: $it", color = ErrorText, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Bước 2: Tạo mã ghép nối", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
                    Spacer(Modifier.height(4.dp))
                    Text("Mã có hiệu lực 10 phút. Đưa mã này cho máy con nhập.", color = SoftText, fontSize = 14.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.createPairingCode() },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Tạo mã ghép nối mới", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    firebaseCode?.let {
                        Spacer(Modifier.height(16.dp))
                        Text("Mã ghép nối:", color = SoftText, fontSize = 14.sp)
                        Text(it, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MainBlue)
                        Spacer(Modifier.height(6.dp))
                        Text("Khi máy con nhập mã, anh bấm Duyệt.", color = SoftText, fontSize = 14.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.approveCurrentCode { onPaired() } },
                            colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Duyệt máy con, vào app", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Tạo mã ghép nối cho máy con", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkText)
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { sampleCode = Pairing.generateCode() },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) { Text("Tạo mã ghép nối mới", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    sampleCode?.let {
                        Spacer(Modifier.height(16.dp))
                        Text("Mã ghép nối:", color = SoftText, fontSize = 14.sp)
                        Text(it.code, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MainBlue)
                        Spacer(Modifier.height(6.dp))
                        Text("Hiệu lực 10 phút. Đưa mã này cho máy con nhập hoặc quét QR.", color = SoftText, fontSize = 14.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onPaired,
                            colors = ButtonDefaults.buttonColors(containerColor = MainBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("Đã duyệt máy con, vào app", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
