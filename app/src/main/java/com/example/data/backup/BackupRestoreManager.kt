package com.example.data.backup

import com.example.data.local.LedgerEntryEntity
import com.example.data.local.RepaymentEntity
import com.example.domain.model.LedgerType
import org.json.JSONArray
import org.json.JSONObject

data class LedgerBackupPayload(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAtEpochMillis: Long,
    val entries: List<LedgerEntryEntity>,
    val repayments: List<RepaymentEntity>
)

sealed class BackupImportResult {
    data class Success(val payload: LedgerBackupPayload) : BackupImportResult()
    data class Error(val reason: String) : BackupImportResult()
}

/**
 * Deterministic JSON Backup & Restore serializer for DENA-PAONA.
 * Preserves all ledger entries, nullable transaction/due dates, exact minor-unit amounts,
 * and complete repayment history without inventing missing dates.
 */
object BackupRestoreSerializer {
    const val CURRENT_SCHEMA_VERSION = 2
    const val APP_VERSION = "0.0.1"

    fun serializeToJson(
        entries: List<LedgerEntryEntity>,
        repayments: List<RepaymentEntity>,
        exportedAtEpochMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("app", "DENA-PAONA")
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("appVersion", APP_VERSION)
        root.put("exportedAtEpochMillis", exportedAtEpochMillis)

        val entriesArray = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("counterpartyName", entry.counterpartyName)
            obj.put("phoneOrReference", entry.phoneOrReference)
            obj.put("description", entry.description)
            obj.put("type", entry.type)
            obj.put("principalMinor", entry.principalMinor)
            obj.put("repaidMinor", entry.repaidMinor)
            obj.put("createdAtEpochMillis", entry.createdAtEpochMillis)
            if (entry.transactionDateEpochMillis != null) {
                obj.put("transactionDateEpochMillis", entry.transactionDateEpochMillis)
            } else {
                obj.put("transactionDateEpochMillis", JSONObject.NULL)
            }
            if (entry.dueDateEpochMillis != null) {
                obj.put("dueDateEpochMillis", entry.dueDateEpochMillis)
            } else {
                obj.put("dueDateEpochMillis", JSONObject.NULL)
            }
            obj.put("updatedAtEpochMillis", entry.updatedAtEpochMillis)
            entriesArray.put(obj)
        }
        root.put("entries", entriesArray)

        val repaymentsArray = JSONArray()
        for (rep in repayments) {
            val obj = JSONObject()
            obj.put("id", rep.id)
            obj.put("entryId", rep.entryId)
            obj.put("amountMinor", rep.amountMinor)
            obj.put("remainingAfterMinor", rep.remainingAfterMinor)
            obj.put("note", rep.note)
            obj.put("paidAtEpochMillis", rep.paidAtEpochMillis)
            repaymentsArray.put(obj)
        }
        root.put("repayments", repaymentsArray)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonString: String): BackupImportResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return BackupImportResult.Error("BACKUP PAYLOAD IS EMPTY.")
        }
        return try {
            val root = JSONObject(trimmed)
            val appName = root.optString("app", "")
            if (appName != "DENA-PAONA") {
                return BackupImportResult.Error("INVALID BACKUP HEADER: EXPECTED 'DENA-PAONA'.")
            }
            val schemaVersion = root.optInt("schemaVersion", 0)
            if (schemaVersion < 1) {
                return BackupImportResult.Error("UNSUPPORTED BACKUP SCHEMA VERSION.")
            }
            val appVersion = root.optString("appVersion", APP_VERSION)
            val exportedAt = root.optLong("exportedAtEpochMillis", System.currentTimeMillis())

            val entriesJson = root.getJSONArray("entries")
            val entries = mutableListOf<LedgerEntryEntity>()
            val validEntryIds = mutableSetOf<Long>()

            for (i in 0 until entriesJson.length()) {
                val item = entriesJson.getJSONObject(i)
                val id = item.getLong("id")
                val counterparty = item.getString("counterpartyName").trim().uppercase()
                if (counterparty.isEmpty()) {
                    return BackupImportResult.Error("ENTRY #$id HAS EMPTY COUNTERPARTY NAME.")
                }
                val rawType = item.getString("type").trim().uppercase()
                if (rawType != LedgerType.RECEIVABLE.name && rawType != LedgerType.PAYABLE.name) {
                    return BackupImportResult.Error("ENTRY #$id HAS INVALID TYPE '$rawType'.")
                }
                val principalMinor = item.getLong("principalMinor")
                val repaidMinor = item.optLong("repaidMinor", 0L)
                if (principalMinor <= 0L || repaidMinor < 0L || repaidMinor > principalMinor) {
                    return BackupImportResult.Error("ENTRY #$id HAS INVALID MONETARY AMOUNTS.")
                }
                val createdAt = item.optLong("createdAtEpochMillis", exportedAt)
                val txDate: Long? = if (item.has("transactionDateEpochMillis") && !item.isNull("transactionDateEpochMillis")) {
                    item.getLong("transactionDateEpochMillis")
                } else {
                    null
                }
                val dueDate: Long? = if (item.has("dueDateEpochMillis") && !item.isNull("dueDateEpochMillis")) {
                    item.getLong("dueDateEpochMillis")
                } else {
                    null
                }
                val updatedAt = item.optLong("updatedAtEpochMillis", createdAt)

                entries.add(
                    LedgerEntryEntity(
                        id = id,
                        counterpartyName = counterparty,
                        phoneOrReference = item.optString("phoneOrReference", "").trim(),
                        description = item.optString("description", "").trim(),
                        type = rawType,
                        principalMinor = principalMinor,
                        repaidMinor = repaidMinor,
                        createdAtEpochMillis = createdAt,
                        transactionDateEpochMillis = txDate,
                        dueDateEpochMillis = dueDate,
                        updatedAtEpochMillis = updatedAt
                    )
                )
                validEntryIds.add(id)
            }

            val repayments = mutableListOf<RepaymentEntity>()
            val repaymentsJson = root.optJSONArray("repayments")
            if (repaymentsJson != null) {
                for (i in 0 until repaymentsJson.length()) {
                    val rep = repaymentsJson.getJSONObject(i)
                    val entryId = rep.getLong("entryId")
                    if (entryId !in validEntryIds) {
                        return BackupImportResult.Error("REPAYMENT REFERENCES UNKNOWN ENTRY ID #$entryId.")
                    }
                    val amountMinor = rep.getLong("amountMinor")
                    if (amountMinor <= 0L) {
                        return BackupImportResult.Error("REPAYMENT AMOUNT MUST BE POSITIVE.")
                    }
                    repayments.add(
                        RepaymentEntity(
                            id = rep.optLong("id", 0L),
                            entryId = entryId,
                            amountMinor = amountMinor,
                            remainingAfterMinor = rep.optLong("remainingAfterMinor", 0L),
                            note = rep.optString("note", "REPAYMENT").trim().uppercase(),
                            paidAtEpochMillis = rep.optLong("paidAtEpochMillis", exportedAt)
                        )
                    )
                }
            }

            BackupImportResult.Success(
                LedgerBackupPayload(
                    schemaVersion = schemaVersion,
                    appVersion = appVersion,
                    exportedAtEpochMillis = exportedAt,
                    entries = entries,
                    repayments = repayments
                )
            )
        } catch (e: Exception) {
            BackupImportResult.Error("MALFORMED BACKUP JSON: ${e.message?.uppercase() ?: "PARSE ERROR"}")
        }
    }
}
