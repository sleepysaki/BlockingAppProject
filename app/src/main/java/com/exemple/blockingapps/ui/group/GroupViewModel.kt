package com.exemple.blockingapps.ui.group

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun getJoinCodeForGroup(groupId: String): String {
        return _myGroups.value.find { it.groupId == groupId }?.joinCode ?: "N/A"
    }

    fun fetchMyGroups(userId: String) {
        _myGroups.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val groups = RetrofitClient.api.getUserGroups(userId)
                _myGroups.value = groups
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createGroup(context: Context, groupName: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = CreateGroupRequest(name = groupName, adminId = userId)
                val response = RetrofitClient.api.createGroup(req)

                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        _currentJoinCode.value = response.joinCode
                        Toast.makeText(context, "Group Created! Code: ${response.joinCode}", Toast.LENGTH_LONG).show()
                        fetchMyGroups(userId)
                    } else {
                        Toast.makeText(context, "Failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun joinGroup(context: Context, joinCode: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = JoinGroupRequest(joinCode = joinCode, userId = userId)
                val response = RetrofitClient.api.joinGroup(req)

                withContext(Dispatchers.Main) {
                    val msg = response["message"] ?: "Unknown response"
                    if (response["status"] == "success") {
                        Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                        fetchMyGroups(userId)
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun fetchMembers(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = RetrofitClient.api.getGroupMembers(groupId)
                _members.value = list
            } catch (e: Exception) {
                Log.e("GroupVM", "Error fetching members", e)
            }
        }
    }

    fun leaveGroup(context: Context, groupId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = LeaveGroupRequest(groupId, userId)
                val res = RetrofitClient.api.leaveGroup(req)
                withContext(Dispatchers.Main) {
                    if (res["status"] == "success") {
                        Toast.makeText(context, "Left group successfully", Toast.LENGTH_SHORT).show()
                        onSuccess()
                        fetchMyGroups(userId)
                    } else {
                        Toast.makeText(context, "Failed: ${res["error"]}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun removeMember(context: Context, groupId: String, adminId: String, targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = RemoveMemberRequest(groupId, adminId, targetUserId)
                val res = RetrofitClient.api.removeMember(req)
                withContext(Dispatchers.Main) {
                    if (res["status"] == "success") {
                        Toast.makeText(context, "Member removed", Toast.LENGTH_SHORT).show()
                        fetchMembers(groupId)
                    } else {
                        Toast.makeText(context, "Failed: ${res["error"]}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun fetchGroupRules(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rules = RetrofitClient.api.getGroupRules(groupId)
                _groupRules.value = rules
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateGroupRule(context: Context, groupId: String, packageName: String, isBlocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rule = GroupRuleDTO(groupId, packageName, isBlocked)
                val res = RetrofitClient.api.updateGroupRule(rule)

                withContext(Dispatchers.Main) {
                    if (res["status"] == "success") {
                        fetchGroupRules(groupId)
                    } else {
                        Toast.makeText(context, "Failed: ${res["error"]}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}