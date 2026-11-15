package vn.phong.parental.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import vn.phong.parental.data.firebase.FirebaseStore
import vn.phong.parental.data.firebase.LocalStore
import vn.phong.parental.data.firebase.PairingRepository
import vn.phong.parental.data.firebase.ParentAuth
import vn.phong.parental.data.firebase.RulesRepository
import vn.phong.parental.model.AppUsageStat
import vn.phong.parental.model.ChildDevice
import vn.phong.parental.model.GeneralLimit
import vn.phong.parental.model.InstalledApp
import vn.phong.parental.model.PerAppLimit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel nối màn hình vào Firebase thật.
 *
 * - Bố mẹ: đăng nhập → tạo family → tạo mã 6 số → thấy con nhập mã → duyệt.
 * - Con: đăng nhập ẩn danh → nhập mã → chờ duyệt → cấp quyền → giám sát.
 * - Sau đó bố mẹ nghe realtime con, luật, thống kê; ghi luật và lệnh khóa.
 */
class ParentalViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LocalStore(application)
    private val pairingRepo = PairingRepository()
    private val rulesRepo = RulesRepository()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var pairingListener: ListenerRegistration? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _signedInEmail = MutableStateFlow<String?>(ParentAuth.currentEmail)
    val signedInEmail: StateFlow<String?> = _signedInEmail

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode

    private val _pairingStatus = MutableStateFlow("NOT_PAIRED")
    val pairingStatus: StateFlow<String> = _pairingStatus

    /** Tên máy con đã nhập mã (bố mẹ thấy trước khi bấm Duyệt). */
    private val _pairingChildName = MutableStateFlow("")
    val pairingChildName: StateFlow<String> = _pairingChildName

    private val _child = MutableStateFlow<ChildDevice?>(null)
    val child: StateFlow<ChildDevice?> = _child

    private val _generalLimit = MutableStateFlow(GeneralLimit())
    val generalLimit: StateFlow<GeneralLimit> = _generalLimit

    private val _perAppLimits = MutableStateFlow<List<PerAppLimit>>(emptyList())
    val perAppLimits: StateFlow<List<PerAppLimit>> = _perAppLimits

    private val _usage = MutableStateFlow<List<AppUsageStat>>(emptyList())
    val usage: StateFlow<List<AppUsageStat>> = _usage

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

    var familyId: String?
        get() = store.familyId
        set(value) { store.familyId = value }

    var childId: String?
        get() = store.childId
        set(value) { store.childId = value }

    val role: String? get() = store.role
    val childStage: String? get() = store.childStage
    val pairingCodeSaved: String? get() = store.pairingCode
    var parentPhone: String
        get() = store.parentPhone ?: ""
        set(value) { store.parentPhone = value.trim() }

    fun setChildStage(stage: String) { store.childStage = stage }

    fun clearError() { _error.value = null }
    fun setError(message: String) { _error.value = message }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // ---- Bố mẹ ----

    fun parentSignIn(email: String, password: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.signIn(email.trim(), password)
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
                val uid = ParentAuth.signUp(email.trim(), password)
                store.parentUid = uid
                store.role = "PARENT"
                _signedInEmail.value = ParentAuth.currentEmail ?: email
            }.onSuccess { onDone() }
                .onFailure { _error.value = it.message }
        }
    }

    /** Tạo family nếu chưa có, tạo mã ghép nối, rồi nghe trạng thái mã. */
    fun createPairingCode() {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.currentUid
                    ?: throw IllegalStateException("Bố mẹ chưa đăng nhập, vui lòng đăng nhập trước")
                val fid = store.familyId ?: pairingRepo.createFamily(uid).also {
                    store.familyId = it
                }
                pairingRepo.createPairingCode(fid)
            }.onSuccess { doc ->
                _pairingCode.value = doc.code
                _pairingStatus.value = "PENDING"
                _pairingChildName.value = ""
                pairingListener?.remove()
                pairingListener = pairingRepo.listenPairingCode(doc.code) { d ->
                    if (d != null) {
                        _pairingStatus.value = d.status
                        _pairingChildName.value = d.childName
                    }
                }
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
                    pairingListener?.remove()
                    pairingListener = null
                    onDone(newChildId)
                }
                .onFailure { _error.value = it.message }
        }
    }

    /** Hủy ghép nối máy con hiện tại (máy con sẽ tự ngừng giám sát). */
    fun unpairChild(onDone: () -> Unit) {
        val fid = store.familyId ?: return
        val cid = store.childId ?: return
        viewModelScope.launch {
            runCatching { pairingRepo.removeChild(fid, cid) }
                .onSuccess {
                    stopListening()
                    store.childId = null
                    _child.value = null
                    _pairingCode.value = null
                    _pairingStatus.value = "NOT_PAIRED"
                    onDone()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun signOutParent(onDone: () -> Unit) {
        stopListening()
        ParentAuth.signOut()
        store.clear()
        _signedInEmail.value = null
        _child.value = null
        _pairingCode.value = null
        _pairingStatus.value = "NOT_PAIRED"
        onDone()
    }

    // ---- Con ----

    /** Con nhập mã: đăng nhập ẩn danh, nhận mã chờ duyệt, lưu mã để mở lại vẫn chờ tiếp. */
    fun childClaimCode(code: String, deviceName: String, onSent: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                val uid = ParentAuth.currentUid ?: ParentAuth.signInAnonymouslyForChild()
                val deviceId = Build.MODEL ?: "android-device"
                val ok = pairingRepo.claimPairingCode(code, deviceName.trim(), deviceId, uid)
                if (!ok) throw IllegalStateException("Mã sai, hết hạn hoặc đã có người nhận")
                store.role = "CHILD"
                store.pairingCode = code
                store.childStage = "WAITING"
                _pairingCode.value = code
                _pairingStatus.value = "WAITING_APPROVAL"
            }.onSuccess { onSent() }
                .onFailure { _error.value = it.message }
        }
    }

    /**
     * Máy con chờ bố mẹ duyệt: nghe mã, khi APPROVED thì lưu familyId/childId
     * và chuyển sang bước cấp quyền.
     */
    fun childWaitForApproval(onApproved: () -> Unit) {
        val code = store.pairingCode ?: return
        pairingListener?.remove()
        pairingListener = pairingRepo.listenPairingCode(code) { d ->
            if (d == null) return@listenPairingCode
            _pairingStatus.value = d.status
            if (d.status == FirebaseStore.STATUS_APPROVED && d.childId.isNotBlank()) {
                store.familyId = d.familyId
                store.childId = d.childId
                store.childStage = "PERMISSIONS"
                pairingListener?.remove()
                pairingListener = null
                onApproved()
            }
        }
    }

    /** Máy con bỏ chờ, quay lại chọn vai (mã hết hạn / nhập nhầm). */
    fun childCancelPairing(onDone: () -> Unit) {
        pairingListener?.remove()
        pairingListener = null
        store.clear()
        ParentAuth.signOut()
        onDone()
    }

    // ---- Lắng nghe realtime và ghi luật (bố mẹ) ----

    fun startListening(familyId: String, childId: String) {
        stopListening()
        listeners += rulesRepo.listenChild(familyId, childId) { _child.value = it }
        listeners += rulesRepo.listenGeneralLimit(familyId) { _generalLimit.value = it }
        listeners += rulesRepo.listenPerAppLimits(familyId) { _perAppLimits.value = it }
        listeners += rulesRepo.listenUsage(familyId, childId, today()) { _usage.value = it }
        listeners += rulesRepo.listenInstalledApps(familyId, childId) { _installedApps.value = it }
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

    fun deletePerAppLimit(packageName: String) {
        val fid = store.familyId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.deletePerAppLimit(fid, packageName) }
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

    /** Cho con thêm phút hôm nay (đồng thời mở khóa và xóa lời xin). */
    fun grantBonus(minutes: Int) {
        val fid = store.familyId ?: return
        val cid = store.childId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.grantBonus(fid, cid, minutes, today()) }
                .onFailure { _error.value = it.message }
        }
    }

    fun dismissUnlockRequest() {
        val fid = store.familyId ?: return
        val cid = store.childId ?: return
        viewModelScope.launch {
            runCatching { rulesRepo.clearUnlockRequest(fid, cid) }
                .onFailure { _error.value = it.message }
        }
    }

    private fun stopListening() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    override fun onCleared() {
        stopListening()
        pairingListener?.remove()
        super.onCleared()
    }
}
