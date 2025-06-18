package com.example.motionplatform

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListPrefHelper(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("list_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveStringList(key: String, list: List<String>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    fun getStringList(key: String): List<String> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }
}