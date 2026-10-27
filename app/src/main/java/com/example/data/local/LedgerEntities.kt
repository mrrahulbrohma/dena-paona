package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.domain.model.DueDateCalculator
import com.example.domain.model.DueEvaluation
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath

@Entity(
    tableName = "ledger_entries",
    indices = [
        Index(value = ["type"]),
        Index(value = ["counterpartyName"]),
        Index(value = ["dueDateEpochMillis"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val counterpartyName: String,
    val phoneOrReference: String = "",
    val description: String,
    val type: String, // "RECEIVABLE" or "PAYABLE"
    val principalMinor: Long, // Original amount in minor units (100 = 1 BDT)
    val repaidMinor: Long = 0L, // Cumulative repaid in minor units
    val createdAtEpochMillis: Long,
    val transactionDateEpochMillis: Long? = null, // Nullable: null when transaction date is unknown
    val dueDateEpochMillis: Long? = null, // Nullable: null when due date is unknown
    val updatedAtEpochMillis: Long = createdAtEpochMillis
) {
    val ledgerType: LedgerType
        get() = if (type.equals(LedgerType.PAYABLE.name, ignoreCase = true)) {
            LedgerType.PAYABLE
        } else {
            LedgerType.RECEIVABLE
        }

    val remainingMinor: Long
        get() = MoneyMath.remainingMinor(principalMinor, repaidMinor)

    val isSettled: Boolean
        get() = remainingMinor == 0L

    fun evaluateDue(nowEpochMillis: Long = System.currentTimeMillis()): DueEvaluation =
        DueDateCalculator.evaluate(
            dueDateEpochMillis = dueDateEpochMillis,
            remainingMinor = remainingMinor,
            nowEpochMillis = nowEpochMillis
        )
}

@Entity(
    tableName = "repayment_history",
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class RepaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val amountMinor: Long,
    val remainingAfterMinor: Long,
    val note: String,
    val paidAtEpochMillis: Long
)

@Entity(tableName = "system_meta")
data class SystemMetaEntity(
    @PrimaryKey val metaKey: String,
    val metaValue: String,
    val recordedAtEpochMillis: Long
)

data class LedgerEntryWithRepayments(
    @Embedded val entry: LedgerEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val repayments: List<RepaymentEntity>
) {
    val sortedRepayments: List<RepaymentEntity>
        get() = repayments.sortedByDescending { it.paidAtEpochMillis }
}
