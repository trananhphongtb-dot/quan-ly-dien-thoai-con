package vn.phong.parental.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Đăng nhập vai Bố Mẹ bằng Firebase Auth.
 * Bố mẹ dùng email + mật khẩu, hoặc Google Sign-In (nối Credential Manager
 * ở bước sau). Máy con không cần tài khoản riêng: nó đăng nhập ẩn danh
 * để có uid hợp lệ khi gửi yêu cầu ghép nối, xem firestore.rules.
 */
object ParentAuth {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUid: String? get() = auth.currentUser?.uid
    val currentEmail: String? get() = auth.currentUser?.email
    val isSignedIn: Boolean get() = auth.currentUser != null

    suspend fun signUp(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid
            ?: throw IllegalStateException("Không tạo được tài khoản Firebase Auth")
    }

    suspend fun signIn(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid
            ?: throw IllegalStateException("Không đăng nhập được Firebase Auth")
    }

    /** Máy con dùng để có uid ẩn danh trước khi nhập mã ghép nối. */
    suspend fun signInAnonymouslyForChild(): String {
        val result = auth.signInAnonymously().await()
        return result.user?.uid
            ?: throw IllegalStateException("Không đăng nhập ẩn danh được")
    }

    /** Bố mẹ đăng nhập bằng tài khoản Google, qua ID token của Google.
     * Token rỗng thì báo rõ bằng tiếng Việt, không để Firebase ném lỗi
     * "Given String is empty or null" khó hiểu ra màn hình. */
    suspend fun signInWithGoogle(idToken: String): String {
        require(idToken.isNotBlank()) {
            "Google không trả về ID token hợp lệ, vui lòng bấm đăng nhập lại"
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid
            ?: throw IllegalStateException("Không đăng nhập được bằng Google")
    }

    fun signOut() = auth.signOut()
}
