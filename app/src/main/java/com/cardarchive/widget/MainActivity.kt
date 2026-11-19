package com.cardarchive.widget

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.view.ViewGroup
import android.graphics.Typeface
import android.graphics.Color

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = CheckInStore.read(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(247, 245, 250))
        }
        val title = TextView(this).apply {
            text = "CARD ARCHIVE"
            textSize = 24f
            setTextColor(Color.rgb(48, 43, 58))
            typeface = Typeface.DEFAULT_BOLD
        }
        val details = TextView(this).apply {
            text = "原生桌面小组件已安装\n\n金币：${state.coins}\n累计有效打卡：${state.totalCheckIns} 次\n阶段进度：${state.stageProgress} / 3\n\n后续版本将接入完整收藏器。"
            textSize = 16f
            setTextColor(Color.rgb(80, 74, 90))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }
        root.addView(title, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(details, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }
}
