package com.cardarchive.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class CheckInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (val result = CheckInStore.checkIn(context)) {
            CheckInResult.AlreadyCheckedIn -> Toast.makeText(context, "今天已经打过卡啦", Toast.LENGTH_SHORT).show()
            is CheckInResult.Success -> {
                val message = buildString {
                    append("打卡成功，金币 +20")
                    if (result.milestoneBonus) append("，阶段奖励 +30")
                    if (result.chestEarned) append("，获得普通宝箱")
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
        CardArchiveWidgetProvider.updateAll(context)
    }
}
