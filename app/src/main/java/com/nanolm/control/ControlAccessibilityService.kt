package com.nanolm.control

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File
import java.time.Instant

class ControlAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: ControlAccessibilityService? = null
            private set
    }

    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private val timer = Handler(Looper.getMainLooper())
    private var controls: LinearLayout? = null
    private val expire = Runnable { cancel() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() = cancel()
    override fun onDestroy() {
        cancel()
        instance = null
        super.onDestroy()
    }

    fun home() = performGlobalAction(GLOBAL_ACTION_HOME)

    fun cancel() {
        timer.removeCallbacks(expire)
        controls?.let { windows.removeView(it) }
        controls = null
    }

    fun arm(command: Command): Boolean {
        cancel()
        val label = when (command) {
            is Command.TypeText -> "Escrever aqui"
            Command.Back -> "Voltar aqui"
            else -> return false
        }
        val panel = LinearLayout(this)
        panel.addView(Button(this).apply {
            text = label
            setOnClickListener {
                val root = rootInActiveWindow
                val ok = when {
                    root == null || root.packageName?.toString() !in setOf("com.whatsapp", "com.android.chrome", "com.google.android.youtube") -> false
                    !root.refresh() -> false
                    command is Command.TypeText -> {
                        val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                        if (field == null || field.packageName != root.packageName || !field.isEditable || field.isPassword || !field.isEnabled) false
                        else field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command.text)
                        })
                    }
                    else -> performGlobalAction(GLOBAL_ACTION_BACK)
                }
                cancel()
                File(filesDir, "audit.log").appendText("${Instant.now()} | EXECUTED ${command::class.simpleName} requested=$ok\n")
                Toast.makeText(this@ControlAccessibilityService, if (ok) "Solicitação aceita pelo Android" else "Destino incompatível ou fora da lista", Toast.LENGTH_SHORT).show()
            }
        })
        panel.addView(Button(this).apply {
            text = "Cancelar"
            setOnClickListener { cancel() }
        })
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END }
        windows.addView(panel, params)
        controls = panel
        timer.postDelayed(expire, 45000)
        return true
    }
}
