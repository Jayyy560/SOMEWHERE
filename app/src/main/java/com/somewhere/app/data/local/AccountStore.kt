package com.somewhere.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedAccount(
    val userId: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val accessToken: String,
    val refreshToken: String
)

object AccountStore {
    private const val PREFS_FILE = "secure_accounts_prefs"
    private const val KEY_ACCOUNTS = "saved_accounts_json"
    
    private var sharedPreferences: SharedPreferences? = null
    
    fun init(context: Context) {
        if (sharedPreferences == null) {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
    
    fun getSavedAccounts(): List<SavedAccount> {
        val prefs = sharedPreferences ?: return emptyList()
        val jsonString = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<SavedAccount>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    fun saveAccount(account: SavedAccount) {
        val prefs = sharedPreferences ?: return
        val accounts = getSavedAccounts().toMutableList()
        
        // Remove existing if any
        accounts.removeAll { it.userId == account.userId }
        // Add new
        accounts.add(account)
        
        prefs.edit().putString(KEY_ACCOUNTS, Json.encodeToString(accounts)).apply()
    }
    
    fun removeAccount(userId: String) {
        val prefs = sharedPreferences ?: return
        val accounts = getSavedAccounts().toMutableList()
        accounts.removeAll { it.userId == userId }
        prefs.edit().putString(KEY_ACCOUNTS, Json.encodeToString(accounts)).apply()
    }
}
