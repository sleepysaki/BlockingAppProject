package com.exemple.blockingapps.ui.group

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete // Import thêm icon xóa
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.exemple.blockingapps.model.GroupMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupName: String,
    currentUserId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    val members by viewModel.members.collectAsState()
    val context = LocalContext.current // Lấy context để hiển thị Toast

    LaunchedEffect(groupId) {
        viewModel.fetchMembers(groupId)
    }

    // Kiểm tra quyền Admin
    val isAdmin = remember(members, currentUserId) {
        val currentUserMember = members.find {
            it.userId.trim() == currentUserId.trim()
        }
        currentUserMember?.role.equals("ADMIN", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(groupName)
                        Text("ID: ${groupId.take(4)}...", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Members List (${members.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f) // Chiếm phần không gian còn lại
            ) {
                items(members) { member ->
                    MemberItem(
                        member = member,
                        isCurrentUserAdmin = isAdmin,
                        onRemoveClick = {
                            // Gọi hàm xóa thành viên từ ViewModel
                            viewModel.removeMember(context, groupId, currentUserId, member.userId)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 👇 Nút Rời Nhóm (Leave Group)
            Button(
                onClick = {
                    viewModel.leaveGroup(context, groupId, currentUserId) {
                        onBack() // Quay lại màn hình trước sau khi rời nhóm
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Leave Group", color = Color.White)
            }
        }
    }
}

@Composable
fun MemberItem(
    member: GroupMember,
    isCurrentUserAdmin: Boolean,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = member.fullName, fontWeight = FontWeight.Bold)
                if (!member.email.isNullOrEmpty()) {
                    Text(text = member.email, style = MaterialTheme.typography.bodySmall)
                }
            }

            val isMemberAdmin = member.role.equals("ADMIN", ignoreCase = true)

            if (isMemberAdmin) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "ADMIN",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                // 👇 Nếu mình là Admin và người này KHÔNG phải Admin -> Hiện nút xóa
                if (isCurrentUserAdmin) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Member",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}