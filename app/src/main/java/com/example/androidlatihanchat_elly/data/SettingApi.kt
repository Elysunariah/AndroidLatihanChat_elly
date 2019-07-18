package com.example.androidlatihanchat_elly.data

import android.content.Context
import android.content.SharedPreferences
import com.example.androidlatihanchat_elly.R

class SettingApi {
    internal var mContext: Context
    private var sharedSetting: SharedPreferences

    constructor(context: Context){
        mContext = context
        sharedSetting =
            mContext.getSharedPreferences(mContext.getString(R.string.settings_file_name), Context.MODE_PRIVATE)
    }

    fun readSetting(key: String): String {
        return sharedSetting.getString(key, "na")
    }

    fun addUpdateSetting(key: String, value: String) {
        val editor = sharedSetting.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun deleteAllSettings() {
        sharedSetting.edit().clear().apply()
    }

    fun readAll(): List<String> {
        val allUser = ArrayList<String> ()
        val allEntries = sharedSetting.all
        for ((key, value) in allEntries) {
        if (key.contains("@"))
            allUser.add("$key ($value)")
    }
    return allUser
    }

}