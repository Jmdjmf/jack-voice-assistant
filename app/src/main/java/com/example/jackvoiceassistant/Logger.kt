package com.example.jackvoiceassistant

import android.os.Handler
import android.os.Looper

object Logger {
    private val listeners = mutableListOf<(String) -> Unit>()
    private val history = StringBuilder()

    fun addListener(listener: (String) -> Unit) {
        listeners.add(listener)
        listener(history.toString())
    }

    fun removeListener(listener: (String) -> Unit) {
        listeners.remove(listener)
    }

    fun log(message: String) {
        val line = "$message\n"
        history.append(line)
        if (history.length > 4000) {
            history.delete(0, history.length - 4000)
        }
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it(history.toString()) }
        }
    }
}
