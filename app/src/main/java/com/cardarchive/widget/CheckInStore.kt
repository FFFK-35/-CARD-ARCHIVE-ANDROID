package com.cardarchive.widget

import android.content.Context
import java.time.LocalDate

data class ArchiveState(
    val coins: Int,
    val totalCheckIns: Int,
    val chestCount: Int,
    val lastCheckInDate: String?
) {
    val stageProgress: Int get() = totalCheckIns % 3
    val isCheckedInToday: Boolean get() = lastCheckInDate == LocalDate.now().toString()
}

object CheckInStore {
    private const val PREFS = "card_archive_widget"
    private const val KEY_COINS = "coins"
    private const val KEY_TOTAL = "total_check_ins"
    private const val KEY_CHESTS = "chest_count"
    private const val KEY_LAST_DATE = "last_check_in_date"

    fun read(context: Context): ArchiveState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ArchiveState(
            coins = p.getInt(KEY_COINS, 0),
            totalCheckIns = p.getInt(KEY_TOTAL, 0),
            chestCount = p.getInt(KEY_CHESTS, 0),
            lastCheckInDate = p.getString(KEY_LAST_DATE, null)
        )
    }

    fun restore(context: Context, state: ArchiveState) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_COINS, state.coins)
            .putInt(KEY_TOTAL, state.totalCheckIns)
            .putInt(KEY_CHESTS, state.chestCount)
        if (state.lastCheckInDate == null) editor.remove(KEY_LAST_DATE)
        else editor.putString(KEY_LAST_DATE, state.lastCheckInDate)
        editor.commit()
    }

    @Synchronized
    fun checkIn(context: Context): CheckInResult {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (p.getString(KEY_LAST_DATE, null) == today) return CheckInResult.AlreadyCheckedIn

        val total = p.getInt(KEY_TOTAL, 0) + 1
        var coins = p.getInt(KEY_COINS, 0) + 20
        val milestoneBonus = total % 3 == 0
        if (milestoneBonus) coins += 30

        val chestEarned = total % 6 == 0
        val chests = p.getInt(KEY_CHESTS, 0) + if (chestEarned) 1 else 0
        p.edit()
            .putInt(KEY_TOTAL, total)
            .putInt(KEY_COINS, coins)
            .putInt(KEY_CHESTS, chests)
            .putString(KEY_LAST_DATE, today)
            .apply()

        return CheckInResult.Success(
            ArchiveState(coins, total, chests, today),
            milestoneBonus,
            chestEarned
        )
    }
}

sealed interface CheckInResult {
    data object AlreadyCheckedIn : CheckInResult
    data class Success(
        val state: ArchiveState,
        val milestoneBonus: Boolean,
        val chestEarned: Boolean
    ) : CheckInResult
}
