package com.example.setsolver

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Custom view for displaying diagnostic logs with timestamps
 */
class DiagnosticsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val textView: TextView = TextView(context).apply {
        textSize = 12f
        setPadding(16, 16, 16, 16)
        setTextIsSelectable(true)
        setTextColor(context.getColor(R.color.white))
    }

    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    init {
        addView(textView)
    }

    /**
     * Adds a log entry with timestamp
     */
    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        logBuilder.append("[$timestamp] $message\n")
        updateDisplay()
    }

    /**
     * Adds a section header
     */
    fun logSection(title: String) {
        val timestamp = dateFormat.format(Date())
        logBuilder.append("\n[$timestamp] === $title ===\n")
        updateDisplay()
    }

    /**
     * Clears all logs
     */
    fun clear() {
        logBuilder.clear()
        updateDisplay()
    }

    /**
     * Updates the display and auto-scrolls to bottom
     */
    private fun updateDisplay() {
        textView.text = logBuilder.toString()
        post {
            fullScroll(FOCUS_DOWN)
        }
    }

    /**
     * Gets the current log content
     */
    fun getLogContent(): String {
        return logBuilder.toString()
    }
}
