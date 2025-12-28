package com.suptv.tv.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    fun saveLastSelectedChannel(providerId: Long, categoryId: Long, itemId: Long) {
        prefs.edit().apply {
            putLong(KEY_LAST_PROVIDER_ID, providerId)
            putLong(KEY_LAST_CATEGORY_ID, categoryId)
            putLong(KEY_LAST_ITEM_ID, itemId)
            apply()
        }
    }
    
    fun getLastSelectedChannel(providerId: Long): LastSelectedChannel? {
        val savedProviderId = prefs.getLong(KEY_LAST_PROVIDER_ID, -1L)
        if (savedProviderId != providerId) {
            return null
        }
        
        val categoryId = prefs.getLong(KEY_LAST_CATEGORY_ID, -1L)
        val itemId = prefs.getLong(KEY_LAST_ITEM_ID, -1L)
        
        return if (categoryId != -1L && itemId != -1L) {
            LastSelectedChannel(categoryId, itemId)
        } else {
            null
        }
    }
    
    companion object {
        private const val PREFS_NAME = "suptv_prefs"
        private const val KEY_LAST_PROVIDER_ID = "last_provider_id"
        private const val KEY_LAST_CATEGORY_ID = "last_category_id"
        private const val KEY_LAST_ITEM_ID = "last_item_id"
    }
}

data class LastSelectedChannel(
    val categoryId: Long,
    val itemId: Long
)
