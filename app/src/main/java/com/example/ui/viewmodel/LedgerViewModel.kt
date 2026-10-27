package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupImportResult
import com.example.data.local.LedgerEntryWithRepayments
import com.example.data.local.RepaymentEntity
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.ThemeMode
import com.example.data.repository.LedgerRepository
import com.example.data.repository.LedgerSummaryMetrics
import com.example.domain.entitlement.EntitlementEngine
import com.example.domain.entitlement.EntitlementState
import com.example.domain.entitlement.OfflineModularPaymentGateway
import com.example.domain.entitlement.PaymentGatewayModule
import com.example.domain.entitlement.PaymentVerificationResult
import com.example.domain.entitlement.ReferralModule
import com.example.domain.entitlement.ReferralStatus
import com.example.domain.entitlement.SandboxEntitlementOverride
import com.example.domain.model.DueStatus
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchFilterChip(val label: String) {
    ALL("ALL"),
    RECEIVABLE("PAONA (IN)"),
    PAYABLE("DENA (OUT)"),
    OVERDUE("OVERDUE"),
    DUE_TODAY("DUE TODAY"),
    SETTLED("SETTLED")
}

data class LedgerUiState(
    val isInitialized: Boolean = false,
    val allEntries: List<LedgerEntryWithRepayments> = emptyList(),
    val receivableEntries: List<LedgerEntryWithRepayments> = emptyList(),
    val payableEntries: List<LedgerEntryWithRepayments> = emptyList(),
    val searchResults: List<LedgerEntryWithRepayments> = emptyList(),
    val allRepayments: List<RepaymentEntity> = emptyList(),
    val metrics: LedgerSummaryMetrics = LedgerSummaryMetrics(),
    val searchQuery: String = "",
    val searchFilter: SearchFilterChip = SearchFilterChip.ALL,
    val entitlementState: EntitlementState = EntitlementEngine.evaluate(System.currentTimeMillis()),
    val referralStatus: ReferralStatus = ReferralStatus("DP-INIT", 0),
    val themeMode: ThemeMode = ThemeMode.DARK,
    val lastExportedBackupJson: String = "",
    val feedbackBanner: String? = null
)

class LedgerViewModel(
    private val repository: LedgerRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val paymentGateway: PaymentGatewayModule = OfflineModularPaymentGateway()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchFilter = MutableStateFlow(SearchFilterChip.ALL)
    val searchFilter: StateFlow<SearchFilterChip> = _searchFilter.asStateFlow()

    private val _lastExportedBackupJson = MutableStateFlow("")
    private val _feedbackBanner = MutableStateFlow<String?>(null)
    private val _isSeedChecked = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repository.ensureSeededOnce()
            _isSeedChecked.value = true
        }
    }

    val uiState: StateFlow<LedgerUiState> = combine(
        repository.allEntriesWithRepayments,
        repository.allRepayments,
        _searchQuery,
        _searchFilter,
        preferencesRepository.themeMode,
        preferencesRepository.sandboxOverride,
        preferencesRepository.referralShareCount,
        _lastExportedBackupJson,
        _feedbackBanner,
        _isSeedChecked
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val entries = values[0] as List<LedgerEntryWithRepayments>
        @Suppress("UNCHECKED_CAST")
        val repayments = values[1] as List<RepaymentEntity>
        val query = values[2] as String
        val filter = values[3] as SearchFilterChip
        val theme = values[4] as ThemeMode
        val sandboxOverride = values[5] as SandboxEntitlementOverride
        val shareCount = values[6] as Int
        val backupJson = values[7] as String
        val banner = values[8] as String?
        val isInit = values[9] as Boolean

        val now = System.currentTimeMillis()
        val metrics = LedgerRepository.computeMetrics(entries, now)
        val receivables = entries.filter { it.entry.ledgerType == LedgerType.RECEIVABLE }
        val payables = entries.filter { it.entry.ledgerType == LedgerType.PAYABLE }

        val normalizedQuery = query.trim().uppercase()
        val filtered = entries.filter { item ->
            val e = item.entry
            val dueEval = e.evaluateDue(now)
            val matchesFilter = when (filter) {
                SearchFilterChip.ALL -> true
                SearchFilterChip.RECEIVABLE -> e.ledgerType == LedgerType.RECEIVABLE
                SearchFilterChip.PAYABLE -> e.ledgerType == LedgerType.PAYABLE
                SearchFilterChip.OVERDUE -> dueEval.status == DueStatus.OVERDUE
                SearchFilterChip.DUE_TODAY -> dueEval.status == DueStatus.DUE_TODAY
                SearchFilterChip.SETTLED -> dueEval.status == DueStatus.SETTLED
            }
            val matchesQuery = if (normalizedQuery.isEmpty()) {
                true
            } else {
                e.counterpartyName.uppercase().contains(normalizedQuery) ||
                    e.description.uppercase().contains(normalizedQuery) ||
                    e.phoneOrReference.uppercase().contains(normalizedQuery) ||
                    dueEval.badgeText.uppercase().contains(normalizedQuery) ||
                    MoneyMath.formatBdt(e.remainingMinor).uppercase().contains(normalizedQuery)
            }
            matchesFilter && matchesQuery
        }

        val entitlement = EntitlementEngine.evaluate(
            installEpochMillis = preferencesRepository.installEpochMillis,
            nowEpochMillis = now,
            isVerifiedPro = false,
            sandboxOverride = sandboxOverride
        )

        val referral = ReferralStatus(
            localInviteCode = ReferralModule.generateDeterministicCode(preferencesRepository.installEpochMillis),
            localShareCount = shareCount
        )

        LedgerUiState(
            isInitialized = isInit,
            allEntries = entries,
            receivableEntries = receivables,
            payableEntries = payables,
            searchResults = filtered,
            allRepayments = repayments,
            metrics = metrics,
            searchQuery = query,
            searchFilter = filter,
            entitlementState = entitlement,
            referralStatus = referral,
            themeMode = theme,
            lastExportedBackupJson = backupJson,
            feedbackBanner = banner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchFilter(filter: SearchFilterChip) {
        _searchFilter.value = filter
    }

    fun dismissFeedbackBanner() {
        _feedbackBanner.value = null
    }

    fun showFeedback(message: String) {
        _feedbackBanner.value = message.uppercase()
    }

    fun saveEntry(
        editingEntryId: Long?,
        counterpartyName: String,
        phoneOrReference: String,
        description: String,
        type: LedgerType,
        amountInput: String,
        transactionDateEpochMillis: Long?,
        dueDateEpochMillis: Long?,
        onSuccess: () -> Unit
    ) {
        val cleanName = counterpartyName.trim().uppercase()
        if (cleanName.isEmpty()) {
            showFeedback("COUNTERPARTY NAME IS REQUIRED.")
            return
        }
        val minorAmount = MoneyMath.parseBdtToMinor(amountInput)
        if (minorAmount == null || minorAmount <= 0L) {
            showFeedback("ENTER A VALID POSITIVE BDT AMOUNT (MAX 2 DECIMALS).")
            return
        }

        viewModelScope.launch {
            if (editingEntryId == null || editingEntryId <= 0L) {
                repository.addEntry(
                    counterpartyName = cleanName,
                    phoneOrReference = phoneOrReference,
                    description = description,
                    type = type,
                    principalMinor = minorAmount,
                    transactionDateEpochMillis = transactionDateEpochMillis,
                    dueDateEpochMillis = dueDateEpochMillis
                )
                showFeedback("${type.banglaTerm} ENTRY ADDED FOR $cleanName (${MoneyMath.formatBdt(minorAmount)}).")
                onSuccess()
            } else {
                val updated = repository.updateEntry(
                    entryId = editingEntryId,
                    counterpartyName = cleanName,
                    phoneOrReference = phoneOrReference,
                    description = description,
                    type = type,
                    principalMinor = minorAmount,
                    transactionDateEpochMillis = transactionDateEpochMillis,
                    dueDateEpochMillis = dueDateEpochMillis
                )
                if (updated) {
                    showFeedback("ENTRY UPDATED FOR $cleanName.")
                    onSuccess()
                } else {
                    showFeedback("PRINCIPAL CANNOT BE LESS THAN AMOUNT ALREADY REPAID.")
                }
            }
        }
    }

    fun recordRepayment(
        entryId: Long,
        amountInput: String,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val repaymentMinor = MoneyMath.parseBdtToMinor(amountInput)
        if (repaymentMinor == null || repaymentMinor <= 0L) {
            showFeedback("ENTER A VALID POSITIVE REPAYMENT AMOUNT.")
            return
        }
        viewModelScope.launch {
            val ok = repository.recordRepayment(
                entryId = entryId,
                repaymentMinor = repaymentMinor,
                note = note
            )
            if (ok) {
                showFeedback("REPAYMENT OF ${MoneyMath.formatBdt(repaymentMinor)} RECORDED IN LEDGER HISTORY.")
                onSuccess()
            } else {
                showFeedback("REPAYMENT EXCEEDS REMAINING OUTSTANDING BALANCE.")
            }
        }
    }

    fun deleteEntry(entryId: Long, counterpartyName: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            showFeedback("DELETED ENTRY FOR ${counterpartyName.uppercase()}.")
            onDeleted()
        }
    }

    fun generateBackupJson(onGenerated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            _lastExportedBackupJson.value = json
            showFeedback("BACKUP SNAPSHOT GENERATED (${json.length} BYTES).")
            onGenerated(json)
        }
    }

    fun restoreFromBackupJson(jsonInput: String, onRestored: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.importBackupJson(jsonInput)) {
                is BackupImportResult.Success -> {
                    showFeedback(
                        "RESTORED ${result.payload.entries.size} ENTRIES & " +
                            "${result.payload.repayments.size} REPAYMENTS."
                    )
                    onRestored()
                }
                is BackupImportResult.Error -> {
                    showFeedback("RESTORE FAILED: ${result.reason}")
                }
            }
        }
    }

    fun resetToSixSeedRecords() {
        viewModelScope.launch {
            repository.resetToInitialSixSeedRecords()
            showFeedback("RESET TO CANONICAL 6 SEED RECORDS (PAONA BDT 200 • DENA BDT 110 • NET BDT +90).")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
        showFeedback("THEME SET TO ${mode.label}.")
    }

    fun setSandboxOverride(overrideMode: SandboxEntitlementOverride) {
        preferencesRepository.setSandboxOverride(overrideMode)
        showFeedback("ENTITLEMENT MODE: ${overrideMode.label}.")
    }

    fun verifyPaymentTransactionReference(reference: String) {
        val cleanRef = reference.trim()
        if (cleanRef.isEmpty()) {
            showFeedback("ENTER A TRANSACTION REFERENCE ID TO TEST MODULAR GATEWAY.")
            return
        }
        when (val res = paymentGateway.requestVerification(cleanRef)) {
            is PaymentVerificationResult.BackendNotConfigured -> {
                showFeedback(res.message)
            }
        }
    }

    fun recordReferralShare(): String {
        preferencesRepository.recordLocalReferralShare()
        val code = ReferralModule.generateDeterministicCode(preferencesRepository.installEpochMillis)
        showFeedback("LOCAL REFERRAL SHARE LOGGED FOR CODE $code.")
        return ReferralModule.buildShareMessage(code)
    }

    companion object {
        fun provideFactory(
            repository: LedgerRepository,
            preferencesRepository: AppPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LedgerViewModel(repository, preferencesRepository) as T
            }
        }
    }
}
