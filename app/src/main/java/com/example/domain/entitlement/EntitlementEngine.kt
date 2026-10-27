package com.example.domain.entitlement

import java.util.concurrent.TimeUnit

/**
 * Defines strict entitlement boundaries between Core Offline Ledger features
 * (which are permanently free and never restricted) and 21-Day Trial / Pro features.
 */
enum class AppFeature(val title: String, val requiresPremiumOrTrial: Boolean) {
    // Core Offline Ledger Features — Always Free & Unconditional
    CORE_LEDGER_CRUD("OFFLINE LEDGER CRUD & EXACT ARITHMETIC", false),
    REPAYMENT_HISTORY("IMMUTABLE REPAYMENT HISTORY LOG", false),
    SEARCH_AND_FILTER("INSTANT COUNTERPARTY & STATUS SEARCH", false),
    JSON_BACKUP_RESTORE("LOCAL JSON BACKUP & RESTORE", false),

    // Premium / 21-Day Trial Features
    OVERDUE_AGING_ANALYTICS("OVERDUE AGING BUCKET ANALYTICS (1-7D / 8-30D / 31D+)", true),
    STATEMENT_DIGEST_EXPORT("COUNTERPARTY REMINDER & AUDIT STATEMENT BUILDER", true),
    BATCH_OVERDUE_DIGEST("ONE-TAP OVERDUE COLLECTION DIGEST", true)
}

enum class EntitlementTier(val label: String) {
    TRIAL_ACTIVE("21-DAY PREMIUM TRIAL ACTIVE"),
    TRIAL_EXPIRED("TRIAL EXPIRED • CORE OFFLINE MODE"),
    PRO_VERIFIED("PRO ENTITLEMENT ACTIVE")
}

enum class SandboxEntitlementOverride(val label: String) {
    NONE("REAL CLOCK (21-DAY TRIAL FROM FIRST INSTALL)"),
    SIMULATE_DAY_18("SANDBOX: TRIAL DAY 18 (3 DAYS LEFT)"),
    SIMULATE_EXPIRED("SANDBOX: TRIAL EXPIRED (DAY 22)"),
    SIMULATE_PRO("SANDBOX: PRO ENTITLEMENT UNLOCKED")
}

data class EntitlementState(
    val tier: EntitlementTier,
    val installEpochMillis: Long,
    val elapsedDays: Int,
    val daysRemainingInTrial: Int,
    val trialTotalDays: Int = EntitlementEngine.TRIAL_DURATION_DAYS,
    val overrideMode: SandboxEntitlementOverride = SandboxEntitlementOverride.NONE
) {
    val hasPremiumAccess: Boolean
        get() = tier == EntitlementTier.TRIAL_ACTIVE || tier == EntitlementTier.PRO_VERIFIED

    fun isFeatureAllowed(feature: AppFeature): Boolean =
        if (!feature.requiresPremiumOrTrial) true else hasPremiumAccess
}

/**
 * Deterministic 21-day trial and entitlement boundary calculator.
 */
object EntitlementEngine {
    const val TRIAL_DURATION_DAYS = 21
    private const val MILLIS_PER_DAY = 86_400_000L

    fun evaluate(
        installEpochMillis: Long,
        nowEpochMillis: Long = System.currentTimeMillis(),
        isVerifiedPro: Boolean = false,
        sandboxOverride: SandboxEntitlementOverride = SandboxEntitlementOverride.NONE
    ): EntitlementState {
        when (sandboxOverride) {
            SandboxEntitlementOverride.SIMULATE_PRO -> {
                return EntitlementState(
                    tier = EntitlementTier.PRO_VERIFIED,
                    installEpochMillis = installEpochMillis,
                    elapsedDays = 0,
                    daysRemainingInTrial = TRIAL_DURATION_DAYS,
                    overrideMode = sandboxOverride
                )
            }
            SandboxEntitlementOverride.SIMULATE_DAY_18 -> {
                return EntitlementState(
                    tier = EntitlementTier.TRIAL_ACTIVE,
                    installEpochMillis = installEpochMillis,
                    elapsedDays = 18,
                    daysRemainingInTrial = 3,
                    overrideMode = sandboxOverride
                )
            }
            SandboxEntitlementOverride.SIMULATE_EXPIRED -> {
                return EntitlementState(
                    tier = EntitlementTier.TRIAL_EXPIRED,
                    installEpochMillis = installEpochMillis,
                    elapsedDays = 22,
                    daysRemainingInTrial = 0,
                    overrideMode = sandboxOverride
                )
            }
            SandboxEntitlementOverride.NONE -> Unit
        }

        if (isVerifiedPro) {
            return EntitlementState(
                tier = EntitlementTier.PRO_VERIFIED,
                installEpochMillis = installEpochMillis,
                elapsedDays = 0,
                daysRemainingInTrial = 0,
                overrideMode = SandboxEntitlementOverride.NONE
            )
        }

        val elapsedMillis = (nowEpochMillis - installEpochMillis).coerceAtLeast(0L)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
        val remainingDays = (TRIAL_DURATION_DAYS - elapsedDays).coerceAtLeast(0)

        val tier = if (elapsedDays < TRIAL_DURATION_DAYS) {
            EntitlementTier.TRIAL_ACTIVE
        } else {
            EntitlementTier.TRIAL_EXPIRED
        }

        return EntitlementState(
            tier = tier,
            installEpochMillis = installEpochMillis,
            elapsedDays = elapsedDays,
            daysRemainingInTrial = remainingDays,
            overrideMode = SandboxEntitlementOverride.NONE
        )
    }
}

/**
 * Modular Payment Verification Contract.
 *
 * Intentionally refuses to fake production payment verification offline.
 * Production builds can bind Google Play Billing or a server-verified bKash/SSLCommerz webhook.
 */
sealed class PaymentVerificationResult {
    data class BackendNotConfigured(val message: String) : PaymentVerificationResult()
}

interface PaymentGatewayModule {
    val providerName: String
    val isConfiguredForProduction: Boolean
    fun requestVerification(transactionReference: String): PaymentVerificationResult
}

class OfflineModularPaymentGateway : PaymentGatewayModule {
    override val providerName: String = "MODULAR BILLING GATEWAY (OFFLINE BUILD)"
    override val isConfiguredForProduction: Boolean = false

    override fun requestVerification(transactionReference: String): PaymentVerificationResult {
        return PaymentVerificationResult.BackendNotConfigured(
            "PAYMENT VERIFICATION REQUIRES SERVER-SIDE SIGNATURE VALIDATION. " +
                "OFFLINE CLIENT REFUSES TO FAKE PAYMENT VERIFICATION FOR REF '${transactionReference.uppercase()}'. " +
                "USE DEVELOPER SANDBOX TOGGLE BELOW ONLY FOR LOCAL ENTITLEMENT QA."
        )
    }
}

/**
 * Modular Referral Architecture.
 *
 * Generates deterministic local invite codes from installation ID and tracks outbound shares,
 * without faking server-side referral reward credits.
 */
data class ReferralStatus(
    val localInviteCode: String,
    val localShareCount: Int,
    val verifiedRewardDays: Int = 0,
    val statusNotice: String = "PRODUCTION REFERRAL REWARDS REQUIRE BACKEND ATTRIBUTION VERIFICATION. NO FAKE OFFLINE REWARDS ARE CREDITED."
)

object ReferralModule {
    fun generateDeterministicCode(installEpochMillis: Long): String {
        val hash = ((installEpochMillis xor 0x5DEECE66DL) and 0xFFFFFL)
            .toString(36)
            .uppercase()
            .padStart(4, 'X')
            .takeLast(4)
        return "DP-$hash"
    }

    fun buildShareMessage(inviteCode: String): String =
        "TRACK PERSONAL DEBTS & RECEIVABLES OFFLINE WITH DENA-PAONA (V0.0.1). " +
            "REPO: HTTPS://GITHUB.COM/MRRAHULBROHMA/DENA-PAONA • INVITE CODE: $inviteCode"
}
