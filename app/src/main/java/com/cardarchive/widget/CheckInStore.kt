package com.cardarchive.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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
    private const val KEY_LEGACY_JSON = "legacy_archive_json"

    fun read(context: Context): ArchiveState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ArchiveState(
            coins = p.getInt(KEY_COINS, 0),
            totalCheckIns = p.getInt(KEY_TOTAL, 0),
            chestCount = p.getInt(KEY_CHESTS, 0),
            lastCheckInDate = p.getString(KEY_LAST_DATE, null)
        )
    }

    fun readLegacyData(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LEGACY_JSON, null)

    fun restoreLegacyData(context: Context, raw: String?) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (raw == null) editor.remove(KEY_LEGACY_JSON) else editor.putString(KEY_LEGACY_JSON, raw)
        editor.commit()
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
        val editor = p.edit()
            .putInt(KEY_TOTAL, total)
            .putInt(KEY_COINS, coins)
            .putInt(KEY_CHESTS, chests)
            .putString(KEY_LAST_DATE, today)

        // Keep the imported V0.4 archive in step with future native check-ins.
        p.getString(KEY_LEGACY_JSON, null)?.let { raw ->
            try {
                val legacy = JSONObject(raw)
                val dates = legacy.optJSONArray("checkins") ?: JSONArray()
                dates.put(today)
                legacy.put("checkins", dates)
                legacy.put("coins", coins)
                legacy.put("chests", chests)
                editor.putString(KEY_LEGACY_JSON, legacy.toString())
            } catch (_: Exception) {
                // Never block a valid native check-in because of an old backup payload.
            }
        }
        editor.apply()

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
