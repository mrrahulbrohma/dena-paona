package com.example.data.local

import com.example.domain.model.DueDateCalculator
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath

/**
 * Provides the 6 canonical initial ledger records for DENA-PAONA:
 *
 * RECEIVABLE:
 * 1. TAMIR: BDT 200. Transaction date unknown. Due date unknown. Note: Money lent in early 2026.
 *
 * PAYABLES:
 * 2. SAKIB'S SHOP: BDT 30. Transaction date unknown. Note: Credit before Puja.
 * 3. SAKIB'S SHOP: BDT 30. Transaction date unknown. Note: Separate credit entry; exact date unknown.
 * 4. SAKIB'S SHOP: BDT 20. Transaction date: 2026-10-27. Due date unknown.
 * 5. JONY BHAI'S SHOP: BDT 20. Transaction date unknown. Note: Credit during Durga Puja.
 * 6. KALAM MAMA'S SHOP: BDT 10. Transaction date unknown. Note: Credit before Puja.
 *
 * Expected totals:
 * - Receivables: BDT 200 (20,000 minor units)
 * - Payables:    BDT 110 (11,000 minor units)
 * - Net position: BDT +90 (+9,000 minor units)
 *
 * Missing transaction dates and due dates are strictly preserved as null (never invented).
 */
object InitialSeedData {
    const val EXPECTED_SEED_COUNT = 6
    val EXPECTED_RECEIVABLES_MINOR = MoneyMath.bdtToMinor(200L) // 20,000L
    val EXPECTED_PAYABLES_MINOR = MoneyMath.bdtToMinor(110L)    // 11,000L
    val EXPECTED_NET_MINOR = MoneyMath.bdtToMinor(90L)          // +9,000L

    /**
     * Exact 2026-10-27 transaction date in Asia/Dhaka timezone for Record #4.
     */
    val SAKIB_OCT_27_2026_DHAKA_MILLIS: Long =
        DueDateCalculator.parseIsoDateDhaka("2026-10-27")!!

    /**
     * Signatures of the legacy v1 demo records so existing v1 installations can safely
     * remove only unmodified legacy demo records without touching user-created records.
     */
    val LEGACY_V1_DEMO_SIGNATURES: Set<String> = setOf(
        "RAHIM UDDIN|01711-223344|EMERGENCY MEDICAL ADVANCE|RECEIVABLE|8000",
        "KARIM TRADERS|01819-556677|WHOLESALE CONSIGNMENT BALANCE|RECEIVABLE|7000",
        "NASRIN AKTER|01912-889900|SHARED TRANSIT & COURIER FARE|RECEIVABLE|5000",
        "TAREQ GROCERY|01611-334455|MONTHLY HOUSEHOLD PROVISIONS|PAYABLE|5000",
        "SHAFAYAT PHARMACY|01511-667788|FIRST AID & PRESCRIPTION SUPP.|PAYABLE|3500",
        "MITHU TELECOM|01311-990011|UTILITY & MOBILE DATA TOP-UP|PAYABLE|2500"
    )

    fun isLegacyV1DemoRecord(entry: LedgerEntryEntity): Boolean {
        val sig = "${entry.counterpartyName.trim().uppercase()}|" +
            "${entry.phoneOrReference.trim().uppercase()}|" +
            "${entry.description.trim().uppercase()}|" +
            "${entry.type.trim().uppercase()}|" +
            "${entry.principalMinor}"
        return sig in LEGACY_V1_DEMO_SIGNATURES && entry.repaidMinor == 0L
    }

    fun canonicalSignature(entry: LedgerEntryEntity): String {
        return "${entry.counterpartyName.trim().uppercase()}|" +
            "${entry.type.trim().uppercase()}|" +
            "${entry.principalMinor}|" +
            "${entry.description.trim().uppercase()}|" +
            "${entry.transactionDateEpochMillis ?: "UNKNOWN"}|" +
            "${entry.dueDateEpochMillis ?: "UNKNOWN"}"
    }

    fun buildInitialSixRecords(referenceEpochMillis: Long = System.currentTimeMillis()): List<LedgerEntryEntity> {
        return listOf(
            // 1. RECEIVABLE: TAMIR: BDT 200. Transaction date unknown. Due date unknown. Note: Money lent in early 2026.
            LedgerEntryEntity(
                id = 1L,
                counterpartyName = "TAMIR",
                phoneOrReference = "",
                description = "Money lent in early 2026.",
                type = LedgerType.RECEIVABLE.name,
                principalMinor = MoneyMath.bdtToMinor(200L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = null,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            ),
            // 2. PAYABLE: SAKIB'S SHOP: BDT 30. Transaction date unknown. Note: Credit before Puja.
            LedgerEntryEntity(
                id = 2L,
                counterpartyName = "SAKIB'S SHOP",
                phoneOrReference = "",
                description = "Credit before Puja.",
                type = LedgerType.PAYABLE.name,
                principalMinor = MoneyMath.bdtToMinor(30L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = null,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            ),
            // 3. PAYABLE: SAKIB'S SHOP: BDT 30. Transaction date unknown. Note: Separate credit entry; exact date unknown.
            LedgerEntryEntity(
                id = 3L,
                counterpartyName = "SAKIB'S SHOP",
                phoneOrReference = "",
                description = "Separate credit entry; exact date unknown.",
                type = LedgerType.PAYABLE.name,
                principalMinor = MoneyMath.bdtToMinor(30L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = null,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            ),
            // 4. PAYABLE: SAKIB'S SHOP: BDT 20. Transaction date: 2026-10-27. Due date unknown.
            LedgerEntryEntity(
                id = 4L,
                counterpartyName = "SAKIB'S SHOP",
                phoneOrReference = "",
                description = "",
                type = LedgerType.PAYABLE.name,
                principalMinor = MoneyMath.bdtToMinor(20L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = SAKIB_OCT_27_2026_DHAKA_MILLIS,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            ),
            // 5. PAYABLE: JONY BHAI'S SHOP: BDT 20. Transaction date unknown. Note: Credit during Durga Puja.
            LedgerEntryEntity(
                id = 5L,
                counterpartyName = "JONY BHAI'S SHOP",
                phoneOrReference = "",
                description = "Credit during Durga Puja.",
                type = LedgerType.PAYABLE.name,
                principalMinor = MoneyMath.bdtToMinor(20L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = null,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            ),
            // 6. PAYABLE: KALAM MAMA'S SHOP: BDT 10. Transaction date unknown. Note: Credit before Puja.
            LedgerEntryEntity(
                id = 6L,
                counterpartyName = "KALAM MAMA'S SHOP",
                phoneOrReference = "",
                description = "Credit before Puja.",
                type = LedgerType.PAYABLE.name,
                principalMinor = MoneyMath.bdtToMinor(10L),
                repaidMinor = 0L,
                createdAtEpochMillis = referenceEpochMillis,
                transactionDateEpochMillis = null,
                dueDateEpochMillis = null,
                updatedAtEpochMillis = referenceEpochMillis
            )
        )
    }
}
