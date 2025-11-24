package com.milkaholic.authenty.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SecureRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs", // File name
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val gson = Gson()

    // --- SAVE ACCOUNT ---
    fun addAccount(account: AccountModel) {
        val currentList = getAccounts().toMutableList()
        currentList.add(account)
        saveList(currentList)
    }

    // --- GET ALL ACCOUNTS ---
    fun getAccounts(): List<AccountModel> {
        val json = sharedPreferences.getString("accounts_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<AccountModel>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    // --- DELETE ACCOUNT ---
    fun deleteAccount(accountId: Long) {
        val currentList = getAccounts().toMutableList()
        currentList.removeAll { it.id == accountId }
        saveList(currentList)
    }

    // Helper to write the list to storage
    private fun saveList(list: List<AccountModel>) {
        val json = gson.toJson(list)
        sharedPreferences.edit().putString("accounts_list", json).apply()
    }
}