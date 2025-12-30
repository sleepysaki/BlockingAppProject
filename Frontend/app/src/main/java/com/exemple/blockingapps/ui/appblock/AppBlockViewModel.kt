package com.exemple.blockingapps.ui.appblock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.exemple.blockingapps.model.BlockRule
import com.exemple.blockingapps.utils.BlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppBlockViewModel(application: Application) : AndroidViewModel(application) {

    private val _rules = MutableStateFlow<List<BlockRule>>(emptyList())
    val rules = _rules.asStateFlow()

    private val context = application.applicationContext

    init {
        loadRules()
    }

    private fun loadRules() {
        viewModelScope.launch {
            // Mock data
            val mockList = listOf(
                BlockRule(
                    packageName = "com.facebook.katana",
                    isBlocked = false,
                    limitMinutes = null,
                    startTime = "22:00",
                    endTime = "07:00"
                ),
                BlockRule(
                    packageName = "com.google.android.youtube",
                    isBlocked = true,
                    limitMinutes = 60,
                    startTime = "08:00",
                    endTime = "17:00"
                )
            )
            _rules.value = mockList

            BlockManager.saveRulesFromUI(context, mockList)
        }
    }

    fun updateRule(
        groupId: Long,
        packageName: String,
        isBlocked: Boolean,
        startTime: String,
        endTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = _rules.value.toMutableList()
            val index = currentList.indexOfFirst { it.packageName == packageName }

            val existingRule = currentList.getOrNull(index)

            val newRule = BlockRule(
                packageName = packageName,
                isBlocked = isBlocked,
                limitMinutes = existingRule?.limitMinutes,
                startTime = startTime,
                endTime = endTime
            )

            if (index != -1) {
                currentList[index] = newRule
            } else {
                currentList.add(newRule)
            }

            _rules.value = currentList

            BlockManager.saveRulesFromUI(context, currentList)

            println("Debug: Rule saved to storage for $packageName")
        }
    }
}