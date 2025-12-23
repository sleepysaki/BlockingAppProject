package com.exemple.blockingapps.ui.group

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.model.CreateGroupRequest
import com.exemple.blockingapps.model.GroupMember
import com.exemple.blockingapps.model.JoinGroupRequest
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

    // Create a new group
    fun createGroup(context: Context, groupName: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = CreateGroupRequest(name = groupName, adminId = userId)
                val response = RetrofitClient.api.createGroup(req)

                withContext(Dispatchers.Main) {
                    if (response.status == "success") {
                        _currentJoinCode.value = response.joinCode
                        Toast.makeText(context, "Group Created! Code: ${response.joinCode}", Toast.LENGTH_LONG).show()
                        // Automatically fetch members after creation
                        fetchMembers(response.groupId)
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

    // Join existing group
    fun joinGroup(context: Context, joinCode: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val req = JoinGroupRequest(joinCode = joinCode, userId = userId)
                val response = RetrofitClient.api.joinGroup(req)

                withContext(Dispatchers.Main) {
                    val msg = response["message"] ?: "Unknown response"
                    if (response["status"] == "success") {
                        Toast.makeText(context, "Joined Successfully!", Toast.LENGTH_SHORT).show()
                        // Note: To fetch members, we need groupId.
                        // For simplicity, we just show success toast here.
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

    // Get list of members
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
}