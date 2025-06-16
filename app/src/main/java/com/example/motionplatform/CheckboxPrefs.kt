package com.example.motionplatform

import android.content.Context
import android.content.SharedPreferences

class CheckboxPrefHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("checkbox_states", Context.MODE_PRIVATE)

    fun saveCheckboxState(id: Int, state: Boolean) {
        prefs.edit().putBoolean(id.toString(), state).apply()
    }

    fun getCheckboxState(id: Int): Boolean {
        return prefs.getBoolean(id.toString(), false)
    }

    fun getAllStates(): Map<Int, Boolean> {
        return prefs.all.mapNotNull {
            val key = it.key.toIntOrNull()
            val value = it.value as? Boolean
            if (key != null && value != null) key to value else null
        }.toMap()
    }
}

