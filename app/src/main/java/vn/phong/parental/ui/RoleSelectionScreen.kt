package vn.phong.parental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.phong.parental.model.Role

/**
 * Màn đầu tiên khi cài app: bắt buộc chọn vai.
 * Chưa chọn thì không vào được trong.
 */
@Composable
fun RoleSelectionScreen(onRoleSelected: (Role) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bạn dùng app này với vai trò nào?")
        Spacer(Modifier.height(8.dp))
        Text("Chọn 1 lần. Sau khi ghép nối, máy con không tự đổi sang vai Bố Mẹ được.")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onRoleSelected(Role.PARENT) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Tôi là Bố Mẹ") }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { onRoleSelected(Role.CHILD) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Tôi là Con") }
    }
}
