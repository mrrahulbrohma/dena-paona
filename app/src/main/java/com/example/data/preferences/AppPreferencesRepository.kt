package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.entitlement.EntitlementEngine
import com.example.domain.entitlement.EntitlementState
import com.example.domain.entitlement.ReferralModule
import com.example.domain.entitlement.ReferralStatus
import com.example.domain.entitlement.SandboxEntitlementOverride
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("SYSTEM DEFAULT"),
    LIGHT("HIGH-CONTRAST LIGHT"),
    DARK("OBSIDIAN DARK")
}

class AppPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _sandboxOverride = MutableStateFlow(loadSandboxOverride())
    val sandboxOverride: StateFlow<SandboxEntitlementOverride> = _sandboxOverride.asStateFlow()

    private val _referralShareCount = MutableStateFlow(prefs.getInt(KEY_REFERRAL_SHARES, 0))
    val referralShareCount: StateFlow<Int> = _referralShareCount.asStateFlow()

    val installEpochMillis: Long = getOrInitInstallTimestamp()

    private fun getOrInitInstallTimestamp(): Long {
        val existing = prefs.getLong(KEY_INSTALL_TIMESTAMP, -1L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_INSTALL_TIMESTAMP, now).apply()
        return now
    }

    private fun loadThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.DARK
    }

    private fun loadSandboxOverride(): SandboxEntitlementOverride {
        val raw = prefs.getString(KEY_SANDBOX_OVERRIDE, SandboxEntitlementOverride.NONE.name)
            ?: SandboxEntitlementOverride.NONE.name
        return SandboxEntitlementOverride.entries.firstOrNull { it.name == raw }
            ?: SandboxEntitlementOverride.NONE
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setSandboxOverride(overrideMode: SandboxEntitlementOverride) {
        prefs.edit().putString(KEY_SANDBOX_OVERRIDE, overrideMode.name).apply()
        _sandboxOverride.value = overrideMode
    }

    fun recordLocalReferralShare() {
        val next = _referralShareCount.value + 1
        prefs.edit().putInt(KEY_REFERRAL_SHARES, next).apply()
        _referralShareCount.value = next
    }

    fun evaluateEntitlement(nowEpochMillis: Long = System.currentTimeMillis()): EntitlementState {
        return EntitlementEngine.evaluate(
            installEpochMillis = installEpochMillis,
            nowEpochMillis = nowEpochMillis,
            isVerifiedPro = false,
            sandboxOverride = _sandboxOverride.value
        )
    }

    fun buildReferralStatus(): ReferralStatus {
        return ReferralStatus(
            localInviteCode = ReferralModule.generateDeterministicCode(installEpochMillis),
            localShareCount = _referralShareCount.value
        )
    }

    companion object {
        private const val PREFS_NAME = "dena_paona_prefs"
        private const val KEY_INSTALL_TIMESTAMP = "install_epoch_millis"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_SANDBOX_OVERRIDE = "sandbox_entitlement_override"
        private const val KEY_REFERRAL_SHARES = "local_referral_shares"
    }
}
