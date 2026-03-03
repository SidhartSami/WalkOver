package com.sidhart.walkover.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.sidhart.walkover.data.LiveWalkState
import androidx.core.content.edit

class WalkRecoveryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "walkover_recovery_prefs"
        private const val KEY_WALK_STATE = "saved_walk_state"
    }

    /**
     * Saves the current walk state to allow recovery if the app is killed.
     */
    fun saveWalkState(state: LiveWalkState) {
        if (!state.isTracking && state.points.isEmpty()) {
            clearSavedWalk()
            return
        }
        val json = gson.toJson(state)
        prefs.edit { putString(KEY_WALK_STATE, json) }
    }

    /**
     * Retrieves the last saved walk state if it exists.
     */
    fun getSavedWalkState(): LiveWalkState? {
        val json = prefs.getString(KEY_WALK_STATE, null)
        if (json != null) {
            try {
                return gson.fromJson(json, LiveWalkState::class.java)
            } catch (_: Exception) {
                // Formatting issue, clear the bad data
                clearSavedWalk()
            }
        }
        return null
    }

    /**
     * Deletes the saved walk state, usually called when a walk is cleanly finished or discarded.
     */
    fun clearSavedWalk() {
        prefs.edit { remove(KEY_WALK_STATE) }
    }
}
