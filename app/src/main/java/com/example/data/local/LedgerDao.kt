package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.domain.model.MoneyMath
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {

    @Transaction
    @Query(
        "SELECT * FROM ledger_entries ORDER BY " +
            "CASE WHEN (principalMinor - repaidMinor) > 0 THEN 0 ELSE 1 END ASC, " +
            "CASE WHEN dueDateEpochMillis IS NULL THEN 1 ELSE 0 END ASC, " +
            "dueDateEpochMillis ASC, id ASC"
    )
    fun observeAllEntriesWithRepayments(): Flow<List<LedgerEntryWithRepayments>>

    @Transaction
    @Query("SELECT * FROM ledger_entries WHERE id = :entryId LIMIT 1")
    fun observeEntryWithRepaymentsById(entryId: Long): Flow<LedgerEntryWithRepayments?>

    @Query("SELECT * FROM repayment_history ORDER BY paidAtEpochMillis DESC, id DESC")
    fun observeAllRepayments(): Flow<List<RepaymentEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY id ASC")
    suspend fun getAllEntriesSnapshot(): List<LedgerEntryEntity>

    @Query("SELECT * FROM repayment_history ORDER BY id ASC")
    suspend fun getAllRepaymentsSnapshot(): List<RepaymentEntity>

    @Query("SELECT * FROM ledger_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: Long): LedgerEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LedgerEntryEntity>)

    @Update
    suspend fun updateEntry(entry: LedgerEntryEntity)

    @Query("DELETE FROM ledger_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: RepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayments(repayments: List<RepaymentEntity>)

    @Query("DELETE FROM repayment_history")
    suspend fun clearAllRepayments()

    @Query("DELETE FROM ledger_entries")
    suspend fun clearAllEntries()

    @Query("SELECT * FROM system_meta WHERE metaKey = :key LIMIT 1")
    suspend fun getMeta(key: String): SystemMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: SystemMetaEntity)

    /**
     * Safe, versioned seeding and migration strategy:
     * 1. If META_KEY_INITIAL_SEED_V2 is already recorded, returns false immediately (strictly idempotent).
     * 2. If upgrading from legacy META_KEY_INITIAL_SEED_V1 (or if legacy v1 demo rows are present),
     *    removes ONLY unmodified legacy v1 demo rows while preserving all user-created or modified records,
     *    then inserts only the missing canonical V2 records without colliding with existing user IDs or duplicating rows.
     * 3. On a fresh database, inserts the 6 canonical V2 records and records META_KEY_INITIAL_SEED_V2.
     */
    @Transaction
    suspend fun seedInitialRecordsExactlyOnce(
        seedEntries: List<LedgerEntryEntity>,
        nowEpochMillis: Long
    ): Boolean {
        val existingV2Meta = getMeta(META_KEY_INITIAL_SEED_V2)
        if (existingV2Meta != null) {
            return false
        }

        val existingV1Meta = getMeta(META_KEY_INITIAL_SEED_V1)
        val currentEntries = getAllEntriesSnapshot()

        // If user restored a custom snapshot previously via V1 restore, respect their restored data
        if (existingV1Meta != null && existingV1Meta.metaValue.startsWith("RESTORED_AT_")) {
            putMeta(
                SystemMetaEntity(
                    metaKey = META_KEY_INITIAL_SEED_V2,
                    metaValue = existingV1Meta.metaValue,
                    recordedAtEpochMillis = nowEpochMillis
                )
            )
            return false
        }

        // Remove only unmodified legacy v1 demo rows if present
        val legacyRowsToDelete = currentEntries.filter { InitialSeedData.isLegacyV1DemoRecord(it) }
        for (legacyRow in legacyRowsToDelete) {
            deleteEntryById(legacyRow.id)
        }

        val remainingEntries = getAllEntriesSnapshot()
        if (remainingEntries.isEmpty()) {
            // Clean slate: insert with canonical IDs 1..6
            insertEntries(seedEntries)
        } else {
            // User-created or existing records exist: insert only missing canonical records
            // using id = 0L so SQLite AUTOINCREMENT never collides with existing or newly assigned IDs.
            val existingCounts = mutableMapOf<String, Int>()
            for (entry in remainingEntries) {
                val sig = InitialSeedData.canonicalSignature(entry)
                existingCounts[sig] = (existingCounts[sig] ?: 0) + 1
            }

            for (seed in seedEntries) {
                val sig = InitialSeedData.canonicalSignature(seed)
                val currentCount = existingCounts[sig] ?: 0
                if (currentCount <= 0) {
                    insertEntry(seed.copy(id = 0L))
                } else {
                    existingCounts[sig] = currentCount - 1
                }
            }
        }

        putMeta(
            SystemMetaEntity(
                metaKey = META_KEY_INITIAL_SEED_V2,
                metaValue = "SEEDED_V2_COUNT_${seedEntries.size}",
                recordedAtEpochMillis = nowEpochMillis
            )
        )
        return true
    }

    /**
     * Atomically records a repayment against an entry, preserving full repayment history
     * and updating the entry's repaidMinor using exact integer arithmetic.
     */
    @Transaction
    suspend fun recordRepaymentTransaction(
        entryId: Long,
        repaymentMinor: Long,
        note: String,
        paidAtEpochMillis: Long
    ): Boolean {
        val entry = getEntryById(entryId) ?: return false
        val currentRemaining = MoneyMath.remainingMinor(entry.principalMinor, entry.repaidMinor)
        if (!MoneyMath.isValidRepayment(repaymentMinor, currentRemaining)) {
            return false
        }
        val newRepaid = Math.addExact(entry.repaidMinor, repaymentMinor)
        val remainingAfter = MoneyMath.remainingMinor(entry.principalMinor, newRepaid)

        updateEntry(
            entry.copy(
                repaidMinor = newRepaid,
                updatedAtEpochMillis = paidAtEpochMillis
            )
        )
        insertRepayment(
            RepaymentEntity(
                entryId = entryId,
                amountMinor = repaymentMinor,
                remainingAfterMinor = remainingAfter,
                note = note.trim().uppercase().ifEmpty { "PARTIAL / FULL REPAYMENT" },
                paidAtEpochMillis = paidAtEpochMillis
            )
        )
        return true
    }

    /**
     * Atomically replaces all ledger entries and repayment history from a validated backup payload.
     */
    @Transaction
    suspend fun restoreSnapshotTransaction(
        entries: List<LedgerEntryEntity>,
        repayments: List<RepaymentEntity>,
        nowEpochMillis: Long
    ) {
        clearAllRepayments()
        clearAllEntries()
        insertEntries(entries)
        insertRepayments(repayments)
        putMeta(
            SystemMetaEntity(
                metaKey = META_KEY_INITIAL_SEED_V2,
                metaValue = "RESTORED_AT_$nowEpochMillis",
                recordedAtEpochMillis = nowEpochMillis
            )
        )
    }

    companion object {
        const val META_KEY_INITIAL_SEED_V1 = "INITIAL_SEED_V1"
        const val META_KEY_INITIAL_SEED_V2 = "INITIAL_SEED_V2"
    }
}
