package com.cardarchive.widget

import android.content.Context
import java.time.LocalDate

data class ArchiveState(val coins: Int, val totalCheckIns: Int, val lastCheckInDate: String?) {
    val stageProgress: Int get() = totalCheckIns % 3
    val isCheckedInToday: Boolean get() = lastCheckInDate == LocalDate.now().toString()
}

object CheckInStore {
    private const val PREFS = "card_archive_widget"
    private const val KEY_COINS = "coins"
    private const val KEY_TOTAL = "total_check_ins"
    private const val KEY_LAST_DATE = "last_check_in_date"
    private const val KEY_CHEST_MILESTONE = "last_chest_milestone"

    fun read(context: Context): ArchiveState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ArchiveState(p.getInt(KEY_COINS, 0), p.getInt(KEY_TOTAL, 0), p.getString(KEY_LAST_DATE, null))
    }

    @Synchronized fun checkIn(context: Context): CheckInResult {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (p.getString(KEY_LAST_DATE, null) == today) return CheckInResult.AlreadyCheckedIn
        val total = p.getInt(KEY_TOTAL, 0) + 1
        var coins = p.getInt(KEY_COINS, 0) + 20
        val bonus = total % 3 == 0
        if (bonus) coins += 30
        val previousChestMilestone = p.getInt(KEY_CHEST_MILESTONE, 0)
        val chest = total % 6 == 0 && total / 6 > previousChestMilestone
        val edit = p.edit().putInt(KEY_TOTAL, total).putInt(KEY_COINS, coins).putString(KEY_LAST_DATE, today)
        if (chest) edit.putInt(KEY_CHEST_MILESTONE, total / 6)
        edit.apply()
        return CheckInResult.Success(ArchiveState(coins, total, today), bonus, chest)
    }
}

sealed interface CheckInResult {
    data object AlreadyCheckedIn : CheckInResult
    data class Success(val state: ArchiveState, val milestoneBonus: Boolean, val chestEarned: Boolean) : CheckInResult
}
