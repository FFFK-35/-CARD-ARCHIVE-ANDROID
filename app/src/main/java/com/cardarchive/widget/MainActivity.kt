package com.cardarchive.widget

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Button
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Typeface
import android.graphics.Color
import android.net.Uri
import org.json.JSONObject
import java.time.LocalDate

class MainActivity : Activity() {
    private val createBackupRequest = 4101
    private val restoreBackupRequest = 4102
    private lateinit var details: TextView
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 48, 36, 36)
            setBackgroundColor(Color.rgb(247, 245, 250))
        }
        val title = TextView(this).apply {
            text = "CARD ARCHIVE"
            textSize = 25f
            setTextColor(Color.rgb(48, 43, 58))
            typeface = Typeface.DEFAULT_BOLD
        }
        details = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.rgb(80, 74, 90))
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 20)
        }
        val export = Button(this).apply {
            text = "导出数据备份"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "card-archive-backup.json")
                }
                startActivityForResult(intent, createBackupRequest)
            }
        }
        val restore = Button(this).apply {
            text = "从备份恢复"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                startActivityForResult(intent, restoreBackupRequest)
            }
        }
        message = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.rgb(105, 91, 125))
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 0)
        }
        root.addView(title)
        root.addView(details, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(export, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(restore, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(message, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
        refreshDetails()
    }

    private fun refreshDetails() {
        val state = CheckInStore.read(this)
        details.text = "桌面小组件运行正常\n\n金币：${state.coins}\n累计有效打卡：${state.totalCheckIns} 次\n阶段进度：${state.stageProgress} / 3\n普通宝箱：${state.chestCount} 个\n\n备份只包含当前原生版数据。"
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            when (requestCode) {
                createBackupRequest -> exportBackup(uri)
                restoreBackupRequest -> importBackup(uri)
            }
        } catch (e: Exception) {
            message.text = "操作失败：文件无效或无法访问。原有数据未主动清除。"
        }
    }

    private fun exportBackup(uri: Uri) {
        val state = CheckInStore.read(this)
        val json = JSONObject().apply {
            put("format", "card-archive-native-backup")
            put("version", 1)
            put("exportedAt", LocalDate.now().toString())
            put("coins", state.coins)
            put("totalCheckIns", state.totalCheckIns)
            put("chestCount", state.chestCount)
            if (state.lastCheckInDate == null) put("lastCheckInDate", JSONObject.NULL)
            else put("lastCheckInDate", state.lastCheckInDate)
        }
        contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json.toString(2)) }
            ?: throw IllegalStateException("Cannot open output")
        message.text = "备份已导出。请妥善保存 JSON 文件。"
    }

    private fun importBackup(uri: Uri) {
        val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Cannot open input")
        val json = JSONObject(raw)
        require(json.getString("format") == "card-archive-native-backup")
        require(json.getInt("version") == 1)
        val coins = json.getInt("coins")
        val total = json.getInt("totalCheckIns")
        val chests = json.getInt("chestCount")
        require(coins >= 0 && total >= 0 && chests >= 0)
        val dateValue = json.opt("lastCheckInDate")
        val date = if (dateValue == null || dateValue == JSONObject.NULL) null else dateValue.toString()
        if (date != null) LocalDate.parse(date)
        val state = ArchiveState(coins, total, chests, date)
        CheckInStore.restore(this, state)
        refreshWidget()
        refreshDetails()
        message.text = "恢复成功！原生版数据已更新，小组件也已刷新。"
    }

    private fun refreshWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(ComponentName(this, CardArchiveWidgetProvider::class.java))
        val intent = Intent(this, CardArchiveWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }
}
