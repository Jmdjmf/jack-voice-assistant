package com.example.jackvoiceassistant

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    companion object {
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.log("MyAccessibilityService: onServiceConnected() fired, instance set")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for typing; typing is triggered directly from VoiceAssistantService
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun typeTextIntoFocusedField(text: String): Boolean {
        val root = rootInActiveWindow
        if (root == null) {
            Logger.log("rootInActiveWindow is NULL")
            return false
        }
        Logger.log("Root found: ${root.className}")

        val focusedByInput = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        Logger.log("findFocus(FOCUS_INPUT) result: $focusedByInput")

        val focusedNode = focusedByInput ?: findFocusedEditableNode(root)
        if (focusedNode == null) {
            Logger.log("No focused editable node found at all")
            return false
        }

        Logger.log("Found node: ${focusedNode.className}, editable=${focusedNode.isEditable}")

        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            text
        )
        val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        Logger.log("performAction ACTION_SET_TEXT result: $result")
        return result
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null
        if (root.isEditable && (root.isFocused || root.isAccessibilityFocused)) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFocusedEditableNode(child)
            if (result != null) return result
        }
        return null
    }
}
