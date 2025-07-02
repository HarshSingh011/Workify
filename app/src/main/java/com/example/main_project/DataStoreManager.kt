package com.example.main_project

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreManager(private val context: Context) {

    private val TOKEN_KEY = stringPreferencesKey("user_token")
    private val ROLE_KEY = stringPreferencesKey("user_role")

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    suspend fun saveRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[ROLE_KEY] = role
        }
    }

    fun getToken(): Flow<String?> {
        return context.dataStore.data
            .map { preferences -> preferences[TOKEN_KEY] }
    }

    fun getRole(): Flow<String?> {
        return context.dataStore.data
            .map { preferences -> preferences[ROLE_KEY] }
    }

    suspend fun deleteToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(ROLE_KEY)
        }
    }

    suspend fun checkToken(): Boolean {
        val storedToken = getToken().first()
        return storedToken != null
    }

    suspend fun logStoredToken() {
        val storedToken = getToken().first()
        Log.d("DataStoreManager", "Stored Token: $storedToken")
    }
}
