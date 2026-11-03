package vn.phong.parental.data.firebase

import com.google.firebase.auth.FirebaseAuth
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

    fun signOut() = auth.signOut()
}
