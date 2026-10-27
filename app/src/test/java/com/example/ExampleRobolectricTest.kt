package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupImportResult
import com.example.data.local.DenaPaonaDatabase
import com.example.data.local.LedgerDao
import com.example.data.local.LedgerEntryEntity
import com.example.data.local.SystemMetaEntity
import com.example.data.repository.LedgerRepository
import com.example.domain.model.DueDateCalculator
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: DenaPaonaDatabase
    private lateinit var ledgerDao: LedgerDao
    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DenaPaonaDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        ledgerDao = database.ledgerDao()
        repository = LedgerRepository(ledgerDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app_name resource is DENA-PAONA`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("DENA-PAONA", context.getString(R.string.app_name))
    }

    @Test
    fun `seedInitialRecordsOnce seeds six exact records once and verifies totals`() = runBlocking {
        val firstSeed = repository.ensureSeededOnce(1_760_000_000_000L)
        val secondSeed = repository.ensureSeededOnce(1_760_000_050_000L)

        assertTrue("First invocation must seed the 6 initial records", firstSeed)
        assertFalse("Second invocation must be a no-op", secondSeed)

        val entries = repository.allEntriesWithRepayments.first()
        assertEquals(6, entries.size)

        val names = entries.map { it.entry.counterpartyName }
        assertTrue(names.contains("TAMIR"))
        assertEquals(3, names.count { it == "SAKIB'S SHOP" })
        assertTrue(names.contains("JONY BHAI'S SHOP"))
        assertTrue(names.contains("KALAM MAMA'S SHOP"))

        val metrics = LedgerRepository.computeMetrics(entries, 1_760_000_000_000L)
        assertEquals("BDT 200", metrics.formattedReceivables)
        assertEquals("BDT 110", metrics.formattedPayables)
        assertEquals("BDT +90", metrics.formattedNetPosition)
    }

    @Test
    fun `versioned seed migration replaces legacy v1 demo records while preserving user-created records without duplication`() = runBlocking {
        val now = 1_760_000_000_000L
        // Simulate an existing V1 installation that had legacy demo records + 1 user-created record
        val legacyDemo1 = LedgerEntryEntity(
            id = 1L,
            counterpartyName = "RAHIM UDDIN",
            phoneOrReference = "01711-223344",
            description = "EMERGENCY MEDICAL ADVANCE",
            type = LedgerType.RECEIVABLE.name,
            principalMinor = 8000L,
            repaidMinor = 0L,
            createdAtEpochMillis = now,
            transactionDateEpochMillis = now,
            dueDateEpochMillis = now
        )
        val userCreatedRecord = LedgerEntryEntity(
            id = 2L,
            counterpartyName = "MY NEIGHBOR HASAN",
            phoneOrReference = "01700-999999",
            description = "USER CREATED PERSONAL LOAN",
            type = LedgerType.RECEIVABLE.name,
            principalMinor = 50000L, // BDT 500
            repaidMinor = 10000L,
            createdAtEpochMillis = now,
            transactionDateEpochMillis = now,
            dueDateEpochMillis = null
        )
        ledgerDao.insertEntries(listOf(legacyDemo1, userCreatedRecord))
        ledgerDao.putMeta(
            SystemMetaEntity(
                metaKey = LedgerDao.META_KEY_INITIAL_SEED_V1,
                metaValue = "SEEDED_V0.0.1_COUNT_6",
                recordedAtEpochMillis = now
            )
        )

        // Run versioned V2 seed migration
        val migrated = repository.ensureSeededOnce(now + 1000L)
        assertTrue("Migration from V1 to V2 should execute once", migrated)

        // Running again must be a strict no-op
        val secondRun = repository.ensureSeededOnce(now + 2000L)
        assertFalse("Subsequent ensureSeededOnce must not re-run", secondRun)

        val allEntries = repository.allEntriesWithRepayments.first().map { it.entry }
        // Legacy "RAHIM UDDIN" must be removed, 6 canonical records added, and "MY NEIGHBOR HASAN" preserved = 7 total
        assertEquals(7, allEntries.size)
        assertFalse(allEntries.any { it.counterpartyName == "RAHIM UDDIN" })
        assertTrue(allEntries.any { it.counterpartyName == "MY NEIGHBOR HASAN" && it.principalMinor == 50000L })
        assertEquals(1, allEntries.count { it.counterpartyName == "TAMIR" && it.principalMinor == 20000L })
        assertEquals(3, allEntries.count { it.counterpartyName == "SAKIB'S SHOP" })
        assertEquals(1, allEntries.count { it.counterpartyName == "JONY BHAI'S SHOP" })
        assertEquals(1, allEntries.count { it.counterpartyName == "KALAM MAMA'S SHOP" })
    }

    @Test
    fun `repayment transaction updates remaining balance and preserves repayment history and nullable dates across backup and restore`() = runBlocking {
        repository.ensureSeededOnce(1_760_000_000_000L)

        // Entry #1 is TAMIR with BDT 200 (20000 minor units) and null dates
        val recorded = repository.recordRepayment(
            entryId = 1L,
            repaymentMinor = MoneyMath.bdtToMinor(50L),
            note = "PARTIAL CASH SETTLEMENT",
            paidAtEpochMillis = 1_760_000_100_000L
        )
        assertTrue(recorded)

        val entriesAfterRepay = repository.allEntriesWithRepayments.first()
        val entry1 = entriesAfterRepay.first { it.entry.id == 1L }
        assertEquals(MoneyMath.bdtToMinor(150L), entry1.entry.remainingMinor)
        assertEquals(1, entry1.repayments.size)
        assertEquals("PARTIAL CASH SETTLEMENT", entry1.repayments.first().note)
        assertNull(entry1.entry.transactionDateEpochMillis)
        assertNull(entry1.entry.dueDateEpochMillis)

        // Export JSON backup
        val exportedJson = repository.exportBackupJson(1_760_000_200_000L)
        assertTrue(exportedJson.contains("DENA-PAONA"))
        assertTrue(exportedJson.contains("PARTIAL CASH SETTLEMENT"))

        // Delete entry #1 and verify count drops to 5
        repository.deleteEntry(1L)
        assertEquals(5, repository.allEntriesWithRepayments.first().size)

        // Restore from JSON backup and verify all 6 entries, null dates, and 2026-10-27 date are restored
        val importResult = repository.importBackupJson(exportedJson, 1_760_000_300_000L)
        assertTrue(importResult is BackupImportResult.Success)

        val restoredEntries = repository.allEntriesWithRepayments.first()
        assertEquals(6, restoredEntries.size)
        val restoredEntry1 = restoredEntries.first { it.entry.id == 1L }
        assertEquals(MoneyMath.bdtToMinor(150L), restoredEntry1.entry.remainingMinor)
        assertNull(restoredEntry1.entry.transactionDateEpochMillis)
        assertNull(restoredEntry1.entry.dueDateEpochMillis)
        assertEquals(1, restoredEntry1.repayments.size)

        val restoredEntry4 = restoredEntries.first { it.entry.id == 4L }
        assertEquals(
            "2026-10-27",
            DueDateCalculator.formatIsoDateDhaka(restoredEntry4.entry.transactionDateEpochMillis!!)
        )
        assertNull(restoredEntry4.entry.dueDateEpochMillis)
    }
}
