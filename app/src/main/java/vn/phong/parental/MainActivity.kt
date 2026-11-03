package vn.phong.parental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.phong.parental.model.Role
import vn.phong.parental.ui.ChildPairingScreen
import vn.phong.parental.ui.LockScreen
import vn.phong.parental.ui.ParentHomeScreen
import vn.phong.parental.ui.ParentPairingScreen
import vn.phong.parental.ui.ParentalViewModel
import vn.phong.parental.ui.RoleSelectionScreen

private enum class Screen {
    ROLE, PARENT_PAIRING, CHILD_PAIRING, CHILD_WAITING, PARENT_HOME, LOCK
}

/**
 * Điều hướng khung app theo vai trò, đã nối ViewModel Firebase thật.
 * Khi bố mẹ duyệt xong, MainActivity bắt đầu lắng nghe realtime máy con
 * để 4 tab hiển thị dữ liệu thật thay vì dữ liệu mẫu.
 */
class MainActivity : ComponentActivity() {
    private val viewModel: ParentalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf(Screen.ROLE) }
                var locked by remember { mutableStateOf(false) }
                when (screen) {
                    Screen.ROLE -> RoleSelectionScreen { role ->
                        screen = if (role == Role.PARENT) Screen.PARENT_PAIRING else Screen.CHILD_PAIRING
                    }
                    Screen.PARENT_PAIRING -> ParentPairingScreen(viewModel = viewModel) {
                        val fid = viewModel.familyId
                        val cid = viewModel.childId
                        if (fid != null && cid != null) {
                            viewModel.startListening(fid, cid)
                        }
                        screen = Screen.PARENT_HOME
                    }
                    Screen.CHILD_PAIRING -> ChildPairingScreen(viewModel = viewModel) {
                        screen = Screen.CHILD_WAITING
                    }
                    Screen.CHILD_WAITING -> LockScreen(
                        lockReason = "Đang chờ bố mẹ duyệt ghép nối. Xong bước này sẽ tới màn cấp quyền.",
                        onRequestUnlock = { viewModel.requestUnlock("Con xin mở khóa") }
                    )
                    Screen.PARENT_HOME -> ParentHomeScreen(
                        viewModel = viewModel,
                        onLockChanged = { isLocked -> locked = isLocked }
                    )
                    Screen.LOCK -> LockScreen()
                }
                if (locked) {
                    // Luồng thật: máy con lắng nghe isLocked từ Firestore
                    // rồi chuyển sang LOCK, không dựa vào cờ cục bộ này.
                }
            }
        }
    }
}
