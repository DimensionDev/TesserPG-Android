package com.sujitech.tessercubecore.common

import android.content.Context
import android.content.SharedPreferences
import com.sujitech.tessercubecore.appContext

object Settings {
    val preferences: SharedPreferences by lazy {
        appContext.getSharedPreferences("tessercube", Context.MODE_PRIVATE)
    }
    inline fun <reified T> get(key: String, defaultValue: T): T {
        return when (defaultValue) {
            is String -> preferences.getString(key, defaultValue) as T
            is Boolean -> preferences.getBoolean(key, defaultValue) as T
            is Long -> preferences.getLong(key, defaultValue) as T
            is Float -> preferences.getFloat(key, defaultValue) as T
            is Double -> preferences.getFloat(key, defaultValue.toFloat()).toDouble() as T
            is Int -> preferences.getInt(key, defaultValue) as T
            else -> return defaultValue
        }
    }
    inline fun <reified T> set(key: String, value: T) {
        preferences.edit().also {
            when (value) {
                is String -> it.putString(key, value)
                is Boolean -> it.putBoolean(key, value)
                is Long -> it.putLong(key, value)
                is Float -> it.putFloat(key, value)
                is Double -> it.putFloat(key, value.toFloat())
                is Int -> it.putInt(key, value)
            }
        }.apply()
    }
}