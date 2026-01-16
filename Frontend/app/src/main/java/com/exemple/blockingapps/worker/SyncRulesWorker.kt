package com.exemple.blockingapps.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.exemple.blockingapps.model.GroupRuleDTO
import com.exemple.blockingapps.model.network.RetrofitClient
import com.exemple.blockingapps.utils.BlockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRulesWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("SyncWorker", "🔄 Starting background sync...")

                // 1. Lấy userId hiện tại (Lưu ý: Bạn cần lưu userId vào SharedPreferences khi Login)
                // Tạm thời mình hardcode ID để test, sau này bạn thay bằng:
                // val userId = applicationContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).getString("user_id", "")
                val currentUserId = "36050457-f112-4762-a7f7-24daab6986ce"

                if (currentUserId.isNullOrEmpty()) {
                    return@withContext Result.failure()
                }

                // 2. Lấy danh sách nhóm mà user này tham gia
                val groupsResponse = RetrofitClient.apiService.getUserGroups(currentUserId)
                if (!groupsResponse.isSuccessful) return@withContext Result.retry()

                val groups = groupsResponse.body() ?: emptyList()
                val allRules = mutableListOf<GroupRuleDTO>()

                // 3. Lặp qua từng nhóm để lấy luật
                for (group in groups) {
                    val rulesResponse = RetrofitClient.apiService.getGroupRules(group.groupId)
                    if (rulesResponse.isSuccessful) {
                        val rules = rulesResponse.body() ?: emptyList()
                        allRules.addAll(rules)
                    }
                }

                // 4. Lưu tất cả luật xuống BlockManager (Local Storage)
                if (allRules.isNotEmpty()) {
                    BlockManager.saveBlockedPackages(applicationContext, allRules)
                    Log.d("SyncWorker", "✅ Synced ${allRules.size} rules successfully!")
                } else {
                    Log.d("SyncWorker", "⚠️ No rules found to sync.")
                }

                Result.success()
            } catch (e: Exception) {
                Log.e("SyncWorker", "❌ Sync failed", e)
                Result.retry()
            }
        }
    }
}