package com.exemple.blockingapps.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.exemple.blockingapps.data.common.BlockState
import com.exemple.blockingapps.data.local.FakeLocalDatabase

class AppBlockerAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.e("BLOCKER", "SERVICE DA KET NOI - DA SAN SANG CHAN")
        // Load danh sách chặn cứng từ DB
        BlockState.blockedPackages = FakeLocalDatabase.loadBlockedPackages().toSet()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Chỉ bắt sự kiện khi mở app mới
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Bỏ qua app hệ thống
        if (pkg == packageName ||
            pkg.contains("launcher") ||
            pkg == "com.android.systemui" ||
            pkg == "android"
        ) return

        // LOGIC CHẶN GỐC CỦA BÁC
        val isInstantBlocking = BlockState.instantLockRemaining.value > 0L
        val remainingDailyTime = BlockState.remainingTimeSeconds[pkg] ?: 0L
        val isDailyLimitExceeded = remainingDailyTime <= 0L

        // Kiểm tra xem app có trong danh sách chặn không và có thỏa mãn điều kiện thời gian không
        val shouldBlock = BlockState.isBlocking &&
                (pkg in BlockState.blockedPackages) &&
                (isInstantBlocking || isDailyLimitExceeded)

        if (shouldBlock) {
            Log.e("BLOCKER", "==> PHÁT HIỆN VI PHẠM: $pkg. ĐÁ VỀ HOME!")
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {
        Log.e("BLOCKER", "Service bị gián đoạn!")
    }
}