package com.sidhart.walkover.utils

import android.content.Context
import android.content.SharedPreferences

class AppPreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("walkover_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    fun hasAcceptedPrivacyPolicy(): Boolean {
        return sharedPreferences.getBoolean(KEY_PRIVACY_ACCEPTED, false)
    }

    fun setPrivacyPolicyAccepted(accepted: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_PRIVACY_ACCEPTED, accepted)
            apply()
        }
    }

    fun hasCompletedOnboarding(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            apply()
        }
    }

    fun resetFirstLaunchFlow() {
        sharedPreferences.edit().apply {
            remove(KEY_PRIVACY_ACCEPTED)
            remove(KEY_ONBOARDING_COMPLETED)
            apply()
        }
    }
}