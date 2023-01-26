package com.example.pitjarusproject.helper

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesHelper(context: Context) {
    private var sharedPref: SharedPreferences
    private val editor: SharedPreferences.Editor

    init {
        sharedPref = context.getSharedPreferences("pitjarus", Context.MODE_PRIVATE)
        editor = sharedPref.edit()
    }

    // sharedPref string
    fun put(key: String, value: String) {
        editor.putString(key, value).apply()
    }

    fun getString(key: String): String? {
        return sharedPref.getString(key, null)
    }

    // sharedPref int
    fun putInt(key: String, value: Int) {
        editor.putInt(key, value).apply()
    }

    fun getInt(key: String): Int {
        return sharedPref.getInt(key, 0)
    }

    // sharedPref boolean
    fun put(key: String, value: Boolean) {
        editor.putBoolean(key, value).apply()
    }

    fun getBoolean(key: String): Boolean {
        return sharedPref.getBoolean(key, false)
    }

    // clear data
    fun clear() {
        editor.clear().apply()
    }
}