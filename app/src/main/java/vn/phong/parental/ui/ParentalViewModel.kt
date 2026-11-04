package vn.phong.parental.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.PairingRepository
import vn.phong.parental.data.firebase.ParentAuth
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.PerAppLimit

/**
 * ViewModel nối màn hình vào Firebase thật.
 *
 * Luồng đã chốt:
 * - Bố mẹ: đăng nhập Auth → tạo family → tạo mã 6 số → duyệt mã.
 * - Con: đăng nhập ẩn danh → nhập mã → chờ duyệt.
 * - Sau đó: bố mẹ lắng nghe realtime máy con, luật chung, luật từng app;
 *   nút khóa Tab 4 ghi isLocked lên Firestore, máy con lắng nghe và bật LockScreen.
 *
 * Màn hình vẫn giữ dữ liệu mẫu khi ViewModel chưa có dữ liệu thật,
 * để chạy thử giao diện mà không cần mạng.
 */
class ParentalViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LocalStore(application)
    private val pairingRepo = PairingRepository()
    private val rulesRepo = RulesRepository()
    private val listeners = mutableListOf<ListenerRegistration>()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Email đang đăng nhập (nếu có) — màn ghép nối hiện rõ để bố mẹ biết
    // mình đã đăng nhập thành công, thay vì màn hình im lặng như trước.
    private val _signedInEmail = MutableStateFlow<String?>(ParentAuth.currentEmail)
    val signedInEmail: StateFlow<String?> = _signedInEmail

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode

    private val _pairingStatus = MutableStateFlow("NOT_PAIRED")
    val pairingStatus: StateFlow<String> = _pairingStatus

    private val _child = MutableStateFlow<ChildDevice?>(null)
    val child: StateFlow<ChildDevice?> = _child

    private val _generalLimit = MutableStateFlow(GeneralLimit())
    val generalLimit: StateFlow<GeneralLimit> = _generalLimit

    private val _perAppLimits = MutableStateFlow<List<PerAppLimit>>(emptyList())
    val perAppLimits: StateFlow<List<PerAppLimit>> = _perAppLimits

    var familyId: String?
        get() = store.familyId
        set(value) { store.familyId = value }

    var childId: String?
        get() = store.childId
        set(value) { store.childId = value }

    fun clearError() { _error.value = null }

    fun setError(message: String) { _error.value = message }

    // ---- Bố mẹ ----

    fun parentSignIn(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.signIn(email, password)
                store.parentUid = uid
                store.role = "PARENT"
                _signedInEmail.value = ParentAuth.currentEmail ?: email
            }.onSuccess { onDone() }
                .onFailure { _error.value = it.message }
        }
    }

    fun parentGoogleSignIn(idToken: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.signInWithGoogle(idToken)
                store.parentUid = uid
                store.role = "PARENT"
                _signedInEmail.value = ParentAuth.currentEmail
            }.onSuccess { onDone() }
                .onFailure { _error.value = it.message }
        }
    }

    fun parentSignUp(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.signUp(email, password)
                store.parentUid = uid
                store.role = "PARENT"
                _signedInEmail.value = ParentAuth.currentEmail ?: email
            }.onSuccess { onDone() }
                .onFailure { _error.value = it.message }
        }
    }

    /** Tạo family nếu chưa có, rồi tạo mã ghép nối Firebase thật. */
    fun createPairingCode() {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.currentUid
                    ?: throw IllegalStateException("Bố mẹ chưa đăng nhập Firebase Auth")
                val fid = store.familyId ?: pairingRepo.createFamily(uid).also {
                    store.familyId = it
                }
                pairingRepo.createPairingCode(fid)
            }.onSuccess { doc ->
                _pairingCode.value = doc.code
                _pairingStatus.value = "PENDING"
            }.onFailure { _error.value = it.message }
        }
    }

    /** Bố mẹ duyệt mã đang chờ, tạo máy con rồi vào 4 tab. */
    fun approveCurrentCode(onDone: (String) -> Unit) {
        val code = _pairingCode.value ?: return
        viewModelScope.launch {
            runCatching { pairingRepo.approvePairing(code) }
                .onSuccess { newChildId ->
                    store.childId = newChildId
                    _pairingStatus.value = "APPROVED"
                    onDone(newChildId)
                }
                .onFailure { _error.value = it.message }
        }
    }

    // ---- Con ----

    /** Con nhập mã: đăng nhập ẩn danh trước, rồi nhận mã chờ duyệt. */
    fun childClaimCode(code: String, deviceName: String, onSent: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                ParentAuth.signInAnonymouslyForChild()
                val deviceId = Build.MODEL ?: "android-device"
                val ok = pairingRepo.claimPairingCode(code, deviceName, deviceId)
                if (!ok) throw IllegalStateException("Mã sai, hết hạn hoặc đã có người nhận")
                store.role = "CHILD"
                _pairingCode.value = code
                _pairingStatus.value = "WAITING_APPROVAL"
            }.onSuccess { onSent() }
                .onFailure { _error.value = it.message }
        }
    }

    // ---- Lắng nghe realtime và ghi luật ----

    fun startListening(familyId: String, childId: String) {
        stopListening()
        listeners += rulesRepo.listenChild(familyId, childId) { _child.value = it }
        listeners += rulesRepo.listenGeneralLimit(familyId) { _generalLimit.value = it }
        listeners += rulesRepo.listenPerAppLimits(familyId) { _perAppLimits.value = it }
    }

    fun saveGeneralLimit(limit: GeneralLimit) {
        val fid = store.familyId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.saveGeneralLimit(fid, limit) }
                .onFailure { _error.value = it.message }
        }
    }

    fun savePerAppLimit(limit: PerAppLimit) {
        val fid = store.familyId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.savePerAppLimit(fid, limit) }
                .onFailure { _error.value = it.message }
        }
    }

    /** Nút khóa Tab 4: ghi isLocked lên Firestore, máy con nhận realtime. */
    fun setLocked(locked: Boolean, reason: String = "") {
        val fid = store.familyId ?: return
        val cid = store.childId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.setLocked(fid, cid, locked, reason) }
                .onFailure { _error.value = it.message }
        }
    }

    fun requestUnlock(message: String) {
        val fid = store.familyId ?: return
        val cid = store.childId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.requestUnlock(fid, cid, message) }
                .onFailure { _error.value = it.message }
        }
    }

    private fun stopListening() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
