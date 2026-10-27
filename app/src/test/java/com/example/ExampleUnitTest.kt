package com.example

import com.example.data.local.InitialSeedData
import com.example.data.local.LedgerEntryWithRepayments
import com.example.data.repository.LedgerRepository
import com.example.domain.entitlement.AppFeature
import com.example.domain.entitlement.EntitlementEngine
import com.example.domain.entitlement.EntitlementTier
import com.example.domain.entitlement.OfflineModularPaymentGateway
import com.example.domain.entitlement.PaymentVerificationResult
import com.example.domain.entitlement.ReferralModule
import com.example.domain.model.AgingBucket
import com.example.domain.model.DueDateCalculator
import com.example.domain.model.DueStatus
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `initial six seed records match exact specification without inventing missing dates or due dates`() {
        val now = 1_760_000_000_000L
        val seedEntries = InitialSeedData.buildInitialSixRecords(now)
        assertEquals(6, seedEntries.size)

        // 1. TAMIR: BDT 200, Receivable, txDate unknown, dueDate unknown, "Money lent in early 2026."
        val r1 = seedEntries[0]
        assertEquals("TAMIR", r1.counterpartyName)
        assertEquals(LedgerType.RECEIVABLE, r1.ledgerType)
        assertEquals(20_000L, r1.principalMinor)
        assertNull(r1.transactionDateEpochMillis)
        assertNull(r1.dueDateEpochMillis)
        assertEquals("Money lent in early 2026.", r1.description)

        // 2. SAKIB'S SHOP: BDT 30, Payable, txDate unknown, dueDate unknown, "Credit before Puja."
        val r2 = seedEntries[1]
        assertEquals("SAKIB'S SHOP", r2.counterpartyName)
        assertEquals(LedgerType.PAYABLE, r2.ledgerType)
        assertEquals(3_000L, r2.principalMinor)
        assertNull(r2.transactionDateEpochMillis)
        assertNull(r2.dueDateEpochMillis)
        assertEquals("Credit before Puja.", r2.description)

        // 3. SAKIB'S SHOP: BDT 30, Payable, txDate unknown, dueDate unknown, "Separate credit entry; exact date unknown."
        val r3 = seedEntries[2]
        assertEquals("SAKIB'S SHOP", r3.counterpartyName)
        assertEquals(LedgerType.PAYABLE, r3.ledgerType)
        assertEquals(3_000L, r3.principalMinor)
        assertNull(r3.transactionDateEpochMillis)
        assertNull(r3.dueDateEpochMillis)
        assertEquals("Separate credit entry; exact date unknown.", r3.description)

        // 4. SAKIB'S SHOP: BDT 20, Payable, txDate 2026-10-27 (Asia/Dhaka), dueDate unknown
        val r4 = seedEntries[3]
        assertEquals("SAKIB'S SHOP", r4.counterpartyName)
        assertEquals(LedgerType.PAYABLE, r4.ledgerType)
        assertEquals(2_000L, r4.principalMinor)
        assertNotNull(r4.transactionDateEpochMillis)
        assertEquals("2026-10-27", DueDateCalculator.formatIsoDateDhaka(r4.transactionDateEpochMillis!!))
        assertNull(r4.dueDateEpochMillis)
        assertEquals("", r4.description)

        // 5. JONY BHAI'S SHOP: BDT 20, Payable, txDate unknown, dueDate unknown, "Credit during Durga Puja."
        val r5 = seedEntries[4]
        assertEquals("JONY BHAI'S SHOP", r5.counterpartyName)
        assertEquals(LedgerType.PAYABLE, r5.ledgerType)
        assertEquals(2_000L, r5.principalMinor)
        assertNull(r5.transactionDateEpochMillis)
        assertNull(r5.dueDateEpochMillis)
        assertEquals("Credit during Durga Puja.", r5.description)

        // 6. KALAM MAMA'S SHOP: BDT 10, Payable, txDate unknown, dueDate unknown, "Credit before Puja."
        val r6 = seedEntries[5]
        assertEquals("KALAM MAMA'S SHOP", r6.counterpartyName)
        assertEquals(LedgerType.PAYABLE, r6.ledgerType)
        assertEquals(1_000L, r6.principalMinor)
        assertNull(r6.transactionDateEpochMillis)
        assertNull(r6.dueDateEpochMillis)
        assertEquals("Credit before Puja.", r6.description)

        // Verify Totals: Receivables BDT 200, Payables BDT 110, Net Position BDT +90
        val wrapped = seedEntries.map { LedgerEntryWithRepayments(it, emptyList()) }
        val metrics = LedgerRepository.computeMetrics(wrapped, now)

        assertEquals(20_000L, metrics.totalReceivablesMinor)
        assertEquals(11_000L, metrics.totalPayablesMinor)
        assertEquals(9_000L, metrics.netPositionMinor)

        assertEquals("BDT 200", metrics.formattedReceivables)
        assertEquals("BDT 110", metrics.formattedPayables)
        assertEquals("BDT +90", metrics.formattedNetPosition)
    }

    @Test
    fun `exact monetary arithmetic avoids floating point drift and validates repayments`() {
        assertEquals(20_000L, MoneyMath.parseBdtToMinor("200"))
        assertEquals(11_055L, MoneyMath.parseBdtToMinor("110.55"))
        assertNull(MoneyMath.parseBdtToMinor("-5"))
        assertNull(MoneyMath.parseBdtToMinor("12.345"))

        val principal = MoneyMath.parseBdtToMinor("100.00")!!
        val rep1 = MoneyMath.parseBdtToMinor("33.33")!!
        val rep2 = MoneyMath.parseBdtToMinor("33.33")!!
        val rep3 = MoneyMath.parseBdtToMinor("33.34")!!

        val after1 = MoneyMath.remainingMinor(principal, rep1)
        assertEquals(6667L, after1)
        assertEquals("BDT 66.67", MoneyMath.formatBdt(after1))

        val after2 = MoneyMath.remainingMinor(principal, rep1 + rep2)
        assertEquals(3334L, after2)

        val after3 = MoneyMath.remainingMinor(principal, rep1 + rep2 + rep3)
        assertEquals(0L, after3)
        assertFalse(MoneyMath.isValidRepayment(1L, after3))
    }

    @Test
    fun `due date calculator uses Asia Dhaka timezone consistently and handles unknown due dates`() {
        // Unknown due date -> NO_DUE_DATE, never invented
        val unknownEval = DueDateCalculator.evaluate(
            dueDateEpochMillis = null,
            remainingMinor = 20_000L,
            nowEpochMillis = System.currentTimeMillis()
        )
        assertEquals(DueStatus.NO_DUE_DATE, unknownEval.status)
        assertNull(unknownEval.daysDelta)
        assertEquals("DUE DATE UNKNOWN", unknownEval.badgeText)

        // 2026-10-27 00:00:00+06:00 in Asia/Dhaka is 2026-10-26T18:00:00Z in UTC.
        // At 2026-10-27 01:30:00+06:00 (2026-10-26T19:30:00Z), UTC is still Oct 26,
        // but Asia/Dhaka is already Oct 27.
        val dhakaOct27Start = DueDateCalculator.parseIsoDateDhaka("2026-10-27")!!
        val dhakaOct27At130Am = dhakaOct27Start + (90 * 60 * 1000L) // +1.5 hours

        // A due date of 2026-10-26 in Asia/Dhaka must be evaluated as OVERDUE BY 1 DAY at 01:30 AM Dhaka time on 2026-10-27
        val dhakaOct26Due = DueDateCalculator.parseIsoDateDhaka("2026-10-26")!!
        val evalAt130AmDhaka = DueDateCalculator.evaluate(
            dueDateEpochMillis = dhakaOct26Due,
            remainingMinor = 2_000L,
            nowEpochMillis = dhakaOct27At130Am
        )
        assertEquals(DueStatus.OVERDUE, evalAt130AmDhaka.status)
        assertEquals(-1L, evalAt130AmDhaka.daysDelta)
        assertEquals("OVERDUE BY 1 DAY", evalAt130AmDhaka.badgeText)
        assertEquals(AgingBucket.DAYS_1_TO_7, evalAt130AmDhaka.agingBucket)

        // Due on 2026-10-27 evaluated at 01:30 AM Dhaka time on 2026-10-27 must be DUE TODAY
        val todayEval = DueDateCalculator.evaluate(
            dueDateEpochMillis = dhakaOct27Start,
            remainingMinor = 3_500L,
            nowEpochMillis = dhakaOct27At130Am
        )
        assertEquals(DueStatus.DUE_TODAY, todayEval.status)
        assertEquals(0L, todayEval.daysDelta)

        // Settled entry
        val settledEval = DueDateCalculator.evaluate(
            dueDateEpochMillis = dhakaOct26Due,
            remainingMinor = 0L,
            nowEpochMillis = dhakaOct27At130Am
        )
        assertEquals(DueStatus.SETTLED, settledEval.status)
    }

    @Test
    fun `21-day trial architecture enforces entitlement boundaries without locking core offline ledger`() {
        val installTime = 1_760_000_000_000L
        val day10 = DueDateCalculator.shiftDays(installTime, 10)
        val activeState = EntitlementEngine.evaluate(installTime, day10)

        assertEquals(EntitlementTier.TRIAL_ACTIVE, activeState.tier)
        assertEquals(11, activeState.daysRemainingInTrial)
        assertTrue(activeState.isFeatureAllowed(AppFeature.CORE_LEDGER_CRUD))
        assertTrue(activeState.isFeatureAllowed(AppFeature.OVERDUE_AGING_ANALYTICS))

        val day22 = DueDateCalculator.shiftDays(installTime, 22)
        val expiredState = EntitlementEngine.evaluate(installTime, day22)

        assertEquals(EntitlementTier.TRIAL_EXPIRED, expiredState.tier)
        assertEquals(0, expiredState.daysRemainingInTrial)
        assertTrue(expiredState.isFeatureAllowed(AppFeature.CORE_LEDGER_CRUD))
        assertTrue(expiredState.isFeatureAllowed(AppFeature.REPAYMENT_HISTORY))
        assertTrue(expiredState.isFeatureAllowed(AppFeature.JSON_BACKUP_RESTORE))
        assertFalse(expiredState.isFeatureAllowed(AppFeature.OVERDUE_AGING_ANALYTICS))
    }

    @Test
    fun `modular payment gateway and referral module do not fake verification or rewards`() {
        val gateway = OfflineModularPaymentGateway()
        assertFalse(gateway.isConfiguredForProduction)
        val result = gateway.requestVerification("TRX-12345")
        assertTrue(result is PaymentVerificationResult.BackendNotConfigured)

        val code = ReferralModule.generateDeterministicCode(1_760_000_000_000L)
        assertNotNull(code)
        assertTrue(code.startsWith("DP-"))
    }
}
