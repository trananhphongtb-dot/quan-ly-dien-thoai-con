package vn.phong.parental

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import vn.phong.parental.data.firebase.ParentAuth
import vn.phong.parental.model.Role
import vn.phong.parental.service.LockEnforcementService
import vn.phong.parental.ui.AppTheme
import vn.phong.parental.ui.ChildActiveScreen
import vn.phong.parental.ui.ChildPairingScreen
import vn.phong.parental.ui.ChildPermissionScreen
import vn.phong.parental.ui.ChildWaitingScreen
import vn.phong.parental.ui.ParentHomeScreen
import vn.phong.parental.ui.ParentPairingScreen
import vn.phong.parental.ui.ParentalViewModel
import vn.phong.parental.ui.RoleSelectionScreen

private enum class Screen {
    ROLE, PARENT_PAIRING, CHILD_PAIRING, CHILD_WAITING, CHILD_PERMISSIONS, CHILD_ACTIVE, PARENT_HOME
}

/**
 * Điều hướng theo vai trò, nhớ trạng thái sau khi đóng/mở lại app:
 * - Chưa chọn vai → màn chọn vai.
 * - Bố mẹ đã đăng nhập và có máy con → vào thẳng 4 tab.
 * - Con: tiếp tục đúng bước đang dở (chờ duyệt / cấp quyền / đang bảo vệ).
 */
class MainActivity : ComponentActivity() {
    private val viewModel: ParentalViewModel by viewModels()

    private fun initialScreen(): Screen = when (viewModel.role) {
        "PARENT" ->
            if (ParentAuth.isSignedIn && viewModel.familyId != null && viewModel.childId != null)
                Screen.PARENT_HOME
            else Screen.PARENT_PAIRING
        "CHILD" -> when (viewModel.childStage) {
            "ACTIVE" -> Screen.CHILD_ACTIVE
            "PERMISSIONS" -> Screen.CHILD_PERMISSIONS
            "WAITING" -> Screen.CHILD_WAITING
            else -> Screen.CHILD_PAIRING
        }
        else -> Screen.ROLE
    }

    private fun startEnforcement() {
        ContextCompat.startForegroundService(this, Intent(this, LockEnforcementService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                var screen by remember { mutableStateOf(initialScreen()) }
                var phone by remember { mutableStateOf(viewModel.parentPhone) }

                LaunchedEffect(screen) {
                    when (screen) {
                        Screen.PARENT_HOME -> {
                            val fid = viewModel.familyId
                            val cid = viewModel.childId
                            if (fid != null && cid != null) viewModel.startListening(fid, cid)
                        }
                        Screen.CHILD_ACTIVE -> runCatching { startEnforcement() }
                        else -> {}
                    }
                }

                when (screen) {
                    Screen.ROLE -> RoleSelectionScreen { role ->
                        screen = if (role == Role.PARENT) Screen.PARENT_PAIRING else Screen.CHILD_PAIRING
                    }
                    Screen.PARENT_PAIRING -> ParentPairingScreen(
                        viewModel = viewModel,
                        onPaired = { screen = Screen.PARENT_HOME },
                        onSignedOut = { screen = Screen.ROLE }
                    )
                    Screen.CHILD_PAIRING -> ChildPairingScreen(viewModel = viewModel) {
                        screen = Screen.CHILD_WAITING
                    }
                    Screen.CHILD_WAITING -> ChildWaitingScreen(
                        viewModel = viewModel,
                        onApproved = { screen = Screen.CHILD_PERMISSIONS },
                        onCancel = {
                            viewModel.childCancelPairing { screen = Screen.ROLE }
                        }
                    )
                    Screen.CHILD_PERMISSIONS -> ChildPermissionScreen(
                        phone = phone,
                        onPhoneChange = { phone = it; viewModel.parentPhone = it },
                        onDone = {
                            viewModel.parentPhone = phone
                            viewModel.setChildStage("ACTIVE")
                            screen = Screen.CHILD_ACTIVE
                        }
                    )
                    Screen.CHILD_ACTIVE -> ChildActiveScreen(
                        phone = phone,
                        onPhoneChange = { phone = it; viewModel.parentPhone = it }
                    )
                    Screen.PARENT_HOME -> ParentHomeScreen(
                        viewModel = viewModel,
                        onSignedOut = { screen = Screen.ROLE },
                        onUnpaired = { screen = Screen.PARENT_PAIRING }
                    )
                }
            }
        }
    }
}
