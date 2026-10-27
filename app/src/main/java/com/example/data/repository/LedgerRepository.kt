package com.example.data.repository

import com.example.data.backup.BackupImportResult
import com.example.data.backup.BackupRestoreSerializer
import com.example.data.local.InitialSeedData
import com.example.data.local.LedgerDao
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.LedgerEntryWithRepayments
import com.example.data.local.RepaymentEntity
import com.example.domain.model.AgingBucket
import com.example.domain.model.DueStatus
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import kotlinx.coroutines.flow.Flow

data class LedgerSummaryMetrics(
    val totalReceivablesMinor: Long = 0L,
    val totalPayablesMinor: Long = 0L,
    val netPositionMinor: Long = 0L,
    val totalReceivablePrincipalMinor: Long = 0L,
    val totalPayablePrincipalMinor: Long = 0L,
    val activeReceivablesCount: Int = 0,
    val activePayablesCount: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val settledCount: Int = 0,
    val agingBreakdownMinor: Map<AgingBucket, Long> = emptyMap()
) {
    val formattedReceivables: String
        get() = MoneyMath.formatBdt(totalReceivablesMinor, showPlusSign = false)

    val formattedPayables: String
        get() = MoneyMath.formatBdt(totalPayablesMinor, showPlusSign = false)

    val formattedNetPosition: String
        get() = MoneyMath.formatBdt(netPositionMinor, showPlusSign = true)
}

/**
 * Repository mediating between Room [LedgerDao] and MVVM ViewModels.
 */
class LedgerRepository(private val ledgerDao: LedgerDao) {

    val allEntriesWithRepayments: Flow<List<LedgerEntryWithRepayments>> =
        ledgerDao.observeAllEntriesWithRepayments()

    val allRepayments: Flow<List<RepaymentEntity>> =
        ledgerDao.observeAllRepayments()

    fun observeEntryById(entryId: Long): Flow<LedgerEntryWithRepayments?> =
        ledgerDao.observeEntryWithRepaymentsById(entryId)

    /**
     * Seeds or migrates the 6 initial records safely and strictly once.
     */
    suspend fun ensureSeededOnce(referenceEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val seedRecords = InitialSeedData.buildInitialSixRecords(referenceEpochMillis)
        return ledgerDao.seedInitialRecordsExactlyOnce(seedRecords, referenceEpochMillis)
    }

    /**
     * Re-seeds the 6 canonical records on explicit user request from Settings.
     */
    suspend fun resetToInitialSixSeedRecords(referenceEpochMillis: Long = System.currentTimeMillis()) {
        val seedRecords = InitialSeedData.buildInitialSixRecords(referenceEpochMillis)
        ledgerDao.restoreSnapshotTransaction(
            entries = seedRecords,
            repayments = emptyList(),
            nowEpochMillis = referenceEpochMillis
        )
    }

    /**
     * Adds a new Receivable (PAONA) or Payable (DENA) entry.
     * Both [transactionDateEpochMillis] and [dueDateEpochMillis] are optional (nullable) so missing dates are never invented.
     */
    suspend fun addEntry(
        counterpartyName: String,
        phoneOrReference: String,
        description: String,
        type: LedgerType,
        principalMinor: Long,
        transactionDateEpochMillis: Long? = null,
        dueDateEpochMillis: Long? = null,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Long {
        require(principalMinor > 0L) { "PRINCIPAL AMOUNT MUST BE POSITIVE" }
        val entry = LedgerEntryEntity(
            counterpartyName = counterpartyName.trim().uppercase(),
            phoneOrReference = phoneOrReference.trim(),
            description = description.trim(),
            type = type.name,
            principalMinor = principalMinor,
            repaidMinor = 0L,
            createdAtEpochMillis = nowEpochMillis,
            transactionDateEpochMillis = transactionDateEpochMillis,
            dueDateEpochMillis = dueDateEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
        return ledgerDao.insertEntry(entry)
    }

    /**
     * Updates an existing ledger entry while preserving its repayment history.
     * Principal cannot be reduced below already-repaid amount.
     */
    suspend fun updateEntry(
        entryId: Long,
        counterpartyName: String,
        phoneOrReference: String,
        description: String,
        type: LedgerType,
        principalMinor: Long,
        transactionDateEpochMillis: Long?,
        dueDateEpochMillis: Long?,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val existing = ledgerDao.getEntryById(entryId) ?: return false
        if (principalMinor <= 0L || principalMinor < existing.repaidMinor) {
            return false
        }
        val updated = existing.copy(
            counterpartyName = counterpartyName.trim().uppercase(),
            phoneOrReference = phoneOrReference.trim(),
            description = description.trim(),
            type = type.name,
            principalMinor = principalMinor,
            transactionDateEpochMillis = transactionDateEpochMillis,
            dueDateEpochMillis = dueDateEpochMillis,
            updatedAtEpochMillis = nowEpochMillis
        )
        ledgerDao.updateEntry(updated)
        return true
    }

    suspend fun deleteEntry(entryId: Long) {
        ledgerDao.deleteEntryById(entryId)
    }

    suspend fun recordRepayment(
        entryId: Long,
        repaymentMinor: Long,
        note: String,
        paidAtEpochMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return ledgerDao.recordRepaymentTransaction(
            entryId = entryId,
            repaymentMinor = repaymentMinor,
            note = note,
            paidAtEpochMillis = paidAtEpochMillis
        )
    }

    suspend fun exportBackupJson(nowEpochMillis: Long = System.currentTimeMillis()): String {
        val entries = ledgerDao.getAllEntriesSnapshot()
        val repayments = ledgerDao.getAllRepaymentsSnapshot()
        return BackupRestoreSerializer.serializeToJson(
            entries = entries,
            repayments = repayments,
            exportedAtEpochMillis = nowEpochMillis
        )
    }

    suspend fun importBackupJson(
        jsonString: String,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): BackupImportResult {
        val result = BackupRestoreSerializer.deserializeFromJson(jsonString)
        if (result is BackupImportResult.Success) {
            ledgerDao.restoreSnapshotTransaction(
                entries = result.payload.entries,
                repayments = result.payload.repayments,
                nowEpochMillis = nowEpochMillis
            )
        }
        return result
    }

    companion object {
        /**
         * Pure exact calculation of ledger totals from a list of entries.
         */
        fun computeMetrics(
            entries: List<LedgerEntryWithRepayments>,
            nowEpochMillis: Long = System.currentTimeMillis()
        ): LedgerSummaryMetrics {
            var receivablesRemaining = 0L
            var payablesRemaining = 0L
            var receivablesPrincipal = 0L
            var payablesPrincipal = 0L
            var activeReceivables = 0
            var activePayables = 0
            var overdue = 0
            var dueToday = 0
            var settled = 0
            val agingMap = mutableMapOf(
                AgingBucket.CURRENT to 0L,
                AgingBucket.DAYS_1_TO_7 to 0L,
                AgingBucket.DAYS_8_TO_30 to 0L,
                AgingBucket.DAYS_31_PLUS to 0L
            )

            for (item in entries) {
                val e = item.entry
                val rem = e.remainingMinor
                val eval = e.evaluateDue(nowEpochMillis)

                if (e.ledgerType == LedgerType.RECEIVABLE) {
                    receivablesRemaining = Math.addExact(receivablesRemaining, rem)
                    receivablesPrincipal = Math.addExact(receivablesPrincipal, e.principalMinor)
                    if (rem > 0L) activeReceivables++
                } else {
                    payablesRemaining = Math.addExact(payablesRemaining, rem)
                    payablesPrincipal = Math.addExact(payablesPrincipal, e.principalMinor)
                    if (rem > 0L) activePayables++
                }

                when (eval.status) {
                    DueStatus.SETTLED -> settled++
                    DueStatus.OVERDUE -> overdue++
                    DueStatus.DUE_TODAY -> dueToday++
                    DueStatus.NO_DUE_DATE, DueStatus.DUE_SOON, DueStatus.UPCOMING -> Unit
                }

                if (rem > 0L) {
                    val bucketPrev = agingMap[eval.agingBucket] ?: 0L
                    agingMap[eval.agingBucket] = Math.addExact(bucketPrev, rem)
                }
            }

            val netMinor = Math.subtractExact(receivablesRemaining, payablesRemaining)
            return LedgerSummaryMetrics(
                totalReceivablesMinor = receivablesRemaining,
                totalPayablesMinor = payablesRemaining,
                netPositionMinor = netMinor,
                totalReceivablePrincipalMinor = receivablesPrincipal,
                totalPayablePrincipalMinor = payablesPrincipal,
                activeReceivablesCount = activeReceivables,
                activePayablesCount = activePayables,
                overdueCount = overdue,
                dueTodayCount = dueToday,
                settledCount = settled,
                agingBreakdownMinor = agingMap
            )
        }
    }
}
