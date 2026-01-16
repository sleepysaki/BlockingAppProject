package com.exemple.blockingapps.ui.group

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.data.model.AppItem
import com.exemple.blockingapps.model.CreateGroupRequest
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.JoinGroupRequest
import com.exemple.blockingapps.model.LeaveGroupRequest
import com.exemple.blockingapps.model.RemoveMemberRequest
import com.exemple.blockingapps.model.UserGroup
import com.exemple.blockingapps.model.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupViewModel : ViewModel() {

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members = _members.asStateFlow()

    private val _currentJoinCode = MutableStateFlow("")
    val currentJoinCode = _currentJoinCode.asStateFlow()

    private val _myGroups = MutableStateFlow<List<UserGroup>>(emptyList())
    val myGroups = _myGroups.asStateFlow()

    private val _groupRules = MutableStateFlow<List<GroupRuleDTO>>(emptyList())
    val groupRules = _groupRules.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    fun getJoinCodeForGroup(groupId: String): String {
        return _myGroups.value.find { it.groupId == groupId }?.joinCode ?: "N/A"
    }

    fun fetchMyGroups(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getUserGroups(userId)
                if (response.isSuccessful) {
                    val groups = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        _myGroups.value = groups.map {
                            UserGroup(
                                groupId = it.groupId,
                                groupName = it.groupName ?: "",
                                role = it.role ?: "",
                                joinCode = it.inviteCode ?: ""
                            )
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun createGroup(context: Context, groupName: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = CreateGroupRequest(name = groupName, adminId = userId)
                val response = RetrofitClient.apiService.createGroup(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        _currentJoinCode.value = data?.joinCode ?: ""
                        Toast.makeText(context, "Group Created! Code: ${data?.joinCode}", Toast.LENGTH_LONG).show()
                        fetchMyGroups(userId)
                    } else {
                        Toast.makeText(context, "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun joinGroup(context: Context, joinCode: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = JoinGroupRequest(joinCode = joinCode, userId = userId)
                val response = RetrofitClient.apiService.joinGroup(request)

                withContext(Dispatchers.Main) {
                    val body = response.body()
                    if (response.isSuccessful && body?.get("status") == "success") {
                        Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                        fetchMyGroups(userId)
                    } else {
                        Toast.makeText(context, body?.get("message") ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Connection Error", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun fetchMembers(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getGroupMembers(groupId)
                if (response.isSuccessful) {
                    _members.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) { Log.e("GroupVM", "Error fetching members", e) }
        }
    }

    fun promoteMember(context: Context, groupId: String, currentUserId: String, targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = com.exemple.blockingapps.model.PromoteMemberRequest(
                    groupId = groupId,
                    adminId = currentUserId,
                    targetUserId = targetUserId
                )
                val response = RetrofitClient.apiService.promoteMember(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Promoted successfully", Toast.LENGTH_SHORT).show()
                        fetchMembers(groupId)
                    } else {
                        Toast.makeText(context, "Failed to promote", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun leaveGroup(context: Context, groupId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = LeaveGroupRequest(groupId, userId)
                val res = RetrofitClient.apiService.leaveGroup(req)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                        onSuccess()
                        fetchMyGroups(userId)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun removeMember(context: Context, groupId: String, adminId: String, targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = RemoveMemberRequest(groupId, adminId, targetUserId)
                val res = RetrofitClient.apiService.removeMember(req)
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                        fetchMembers(groupId)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchGroupRules(context: Context, groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getGroupRules(groupId)
                if (response.isSuccessful) {
                    val rules = response.body() ?: emptyList()
                    _groupRules.value = rules
                    com.exemple.blockingapps.utils.BlockManager.saveBlockedPackages(context, rules)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- HÀM THÊM RULE NHANH (Cũ - Có thể xóa nếu không dùng) ---
    fun addGroupRule(context: Context, groupId: String, app: AppItem) {
        // Mặc định thêm rule chặn vĩnh viễn nếu gọi hàm này
        updateGroupRule(context, groupId, app.packageName, true, null, null)
    }

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val apps = allApps.mapNotNull { info ->
                val launchIntent = pm.getLaunchIntentForPackage(info.packageName)
                if (launchIntent != null) {
                    AppItem(
                        name = info.loadLabel(pm).toString(),
                        packageName = info.packageName,
                        icon = info.loadIcon(pm),
                        isSelected = false
                    )
                } else { null }
            }.filter { it.packageName != context.packageName }
                .sortedBy { it.name }
            _installedApps.value = apps
        }
    }

    // --- UPDATED: HÀM CẬP NHẬT RULE (HỖ TRỢ THỜI GIAN) ---
    fun updateGroupRule(
        context: Context,
        groupId: String,
        packageName: String,
        isBlocked: Boolean,
        startTime: String? = null,
        endTime: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentList = _groupRules.value
                val existingRule = currentList.find { it.packageName == packageName }

                // Copy hoặc tạo mới rule
                val ruleToSend = existingRule?.copy(
                    isBlocked = isBlocked,
                    startTime = startTime,
                    endTime = endTime
                ) ?: GroupRuleDTO(
                    groupId = groupId,
                    packageName = packageName,
                    isBlocked = isBlocked,
                    startTime = startTime,
                    endTime = endTime
                )

                Log.d("GroupVM", "Sending Rule: $ruleToSend")

                val res = RetrofitClient.apiService.updateGroupRule(ruleToSend)

                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        Toast.makeText(context, if(isBlocked) "Rule Saved!" else "App Unblocked!", Toast.LENGTH_SHORT).show()
                        fetchGroupRules(context, groupId) // Refresh list
                    } else {
                        Toast.makeText(context, "Failed: ${res.code()}", Toast.LENGTH_SHORT).show()
                        fetchGroupRules(context, groupId) // Refresh to reset state
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    fetchGroupRules(context, groupId)
                }
            }
        }
    }
}