package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.LedgerEntryWithRepayments
import com.example.data.repository.LedgerSummaryMetrics
import com.example.domain.entitlement.EntitlementState
import com.example.domain.entitlement.EntitlementTier
import com.example.domain.model.DueDateCalculator
import com.example.domain.model.DueStatus
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import kotlin.math.roundToInt

/**
 * Formats an epoch timestamp in the Asia/Dhaka timezone as YYYY-MM-DD.
 */
fun formatDhakaDateUppercase(epochMillis: Long): String =
    DueDateCalculator.formatDisplayDateDhaka(epochMillis)

@Composable
fun NetPositionHeroCard(
    metrics: LedgerSummaryMetrics,
    onAddReceivable: () -> Unit,
    onAddPayable: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_net_position_hero"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET POSITION (PAONA - DENA)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (metrics.netPositionMinor >= 0L) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = if (metrics.netPositionMinor >= 0L) "SURPLUS" else "DEFICIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (metrics.netPositionMinor >= 0L) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = metrics.formattedNetPosition,
                style = MaterialTheme.typography.displayLarge,
                color = if (metrics.netPositionMinor >= 0L) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.testTag("text_net_position_value")
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Receivables Column
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "RECEIVABLES (PAONA)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metrics.formattedReceivables,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("text_receivables_value")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${metrics.activeReceivablesCount} ACTIVE RECORDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Payables Column
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PAYABLES (DENA)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metrics.formattedPayables,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("text_payables_value")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${metrics.activePayablesCount} ACTIVE RECORDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddReceivable,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("btn_quick_add_receivable"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ADD PAONA", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD PAONA", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onAddPayable,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("btn_quick_add_payable"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "ADD DENA", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ADD DENA", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun TrialEntitlementBanner(
    entitlementState: EntitlementState,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (entitlementState.tier) {
        EntitlementTier.TRIAL_ACTIVE -> MaterialTheme.colorScheme.secondaryContainer
        EntitlementTier.PRO_VERIFIED -> MaterialTheme.colorScheme.primaryContainer
        EntitlementTier.TRIAL_EXPIRED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (entitlementState.tier) {
        EntitlementTier.TRIAL_ACTIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        EntitlementTier.PRO_VERIFIED -> MaterialTheme.colorScheme.onPrimaryContainer
        EntitlementTier.TRIAL_EXPIRED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpenSettingsClick)
            .testTag("banner_trial_entitlement"),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "ENTITLEMENT STATUS",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    val headline = when (entitlementState.tier) {
                        EntitlementTier.TRIAL_ACTIVE ->
                            "21-DAY PREMIUM TRIAL • ${entitlementState.daysRemainingInTrial} DAYS LEFT"
                        EntitlementTier.PRO_VERIFIED ->
                            "PRO ENTITLEMENT ACTIVE • ALL ANALYTICS UNLOCKED"
                        EntitlementTier.TRIAL_EXPIRED ->
                            "21-DAY TRIAL ENDED • CORE OFFLINE LEDGER ALWAYS FREE"
                    }
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                    Text(
                        text = "CORE LEDGER, REPAYMENTS & BACKUP NEVER EXPIRE • TAP FOR BOUNDARIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(
    item: LedgerEntryWithRepayments,
    onLogRepaymentClick: (LedgerEntryWithRepayments) -> Unit,
    onViewHistoryClick: (LedgerEntryWithRepayments) -> Unit,
    onEditClick: (LedgerEntryWithRepayments) -> Unit,
    onDeleteClick: (LedgerEntryWithRepayments) -> Unit,
    modifier: Modifier = Modifier
) {
    val entry = item.entry
    val dueEval = entry.evaluateDue()
    val isReceivable = entry.ledgerType == LedgerType.RECEIVABLE
    val accentColor = if (isReceivable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    val statusColor = when (dueEval.status) {
        DueStatus.SETTLED -> MaterialTheme.colorScheme.primary
        DueStatus.OVERDUE -> MaterialTheme.colorScheme.error
        DueStatus.DUE_TODAY, DueStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        DueStatus.NO_DUE_DATE, DueStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ledger_entry_card_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isReceivable) "PAONA • IN" else "DENA • OUT",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = dueEval.badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.counterpartyName.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (entry.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = entry.description.uppercase(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val txDateStr = entry.transactionDateEpochMillis?.let {
                        formatDhakaDateUppercase(it)
                    } ?: "UNKNOWN"
                    val dueDateStr = entry.dueDateEpochMillis?.let {
                        formatDhakaDateUppercase(it)
                    } ?: "UNKNOWN"

                    Text(
                        text = buildString {
                            append("TX DATE: $txDateStr • DUE: $dueDateStr")
                            if (entry.phoneOrReference.isNotBlank()) {
                                append(" • REF: ${entry.phoneOrReference.uppercase()}")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyMath.formatBdt(entry.remainingMinor),
                        style = MaterialTheme.typography.headlineMedium,
                        color = accentColor
                    )
                    Text(
                        text = if (entry.repaidMinor > 0L) {
                            "OF ${MoneyMath.formatBdt(entry.principalMinor)} (${MoneyMath.formatBdt(entry.repaidMinor)} PAID)"
                        } else {
                            "PRINCIPAL ${MoneyMath.formatBdt(entry.principalMinor)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!entry.isSettled) {
                    Button(
                        onClick = { onLogRepaymentClick(item) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_repay_${entry.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "LOG REPAYMENT",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REPAY", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "SETTLED",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FULLY SETTLED",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { onViewHistoryClick(item) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_history_${entry.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "REPAYMENT HISTORY",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOG (${item.repayments.size})",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(
                        onClick = { onEditClick(item) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_edit_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "EDIT ENTRY",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { onDeleteClick(item) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_delete_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "DELETE ENTRY",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryFormDialog(
    initialEntry: LedgerEntryWithRepayments?,
    defaultType: LedgerType,
    onDismiss: () -> Unit,
    onSave: (
        editingId: Long?,
        counterparty: String,
        phoneOrRef: String,
        description: String,
        type: LedgerType,
        amountInput: String,
        transactionDateEpochMillis: Long?,
        dueDateEpochMillis: Long?
    ) -> Unit
) {
    val existing = initialEntry?.entry
    var selectedType by remember { mutableStateOf(existing?.ledgerType ?: defaultType) }
    var counterparty by remember { mutableStateOf(existing?.counterpartyName ?: "") }
    var phoneOrRef by remember { mutableStateOf(existing?.phoneOrReference ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var amountInput by remember {
        mutableStateOf(
            if (existing != null) MoneyMath.formatPlainAmount(existing.principalMinor) else ""
        )
    }

    var txDateIsoInput by remember {
        mutableStateOf(
            existing?.transactionDateEpochMillis?.let { DueDateCalculator.formatIsoDateDhaka(it) } ?: ""
        )
    }

    val initialDaysOffset: Int? = remember(existing) {
        val existingDue = existing?.dueDateEpochMillis ?: return@remember null
        val today = DueDateCalculator.normalizeToStartOfDayDhaka(System.currentTimeMillis())
        val due = DueDateCalculator.normalizeToStartOfDayDhaka(existingDue)
        ((due - today).toDouble() / 86_400_000.0).roundToInt()
    }
    var dueOffsetDays by remember { mutableStateOf(initialDaysOffset) }

    val dayPresets: List<Pair<Int?, String>> = listOf(
        null to "UNKNOWN / NO DUE DATE",
        -3 to "OVERDUE 3D",
        0 to "DUE TODAY",
        2 to "IN 2 DAYS",
        7 to "IN 7 DAYS",
        14 to "IN 14 DAYS",
        30 to "IN 30 DAYS"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existing == null) "ADD NEW LEDGER ENTRY" else "EDIT LEDGER ENTRY #${existing.id}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == LedgerType.RECEIVABLE,
                        onClick = { selectedType = LedgerType.RECEIVABLE },
                        label = { Text("PAONA (RECEIVABLE)") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chip_type_receivable")
                    )
                    FilterChip(
                        selected = selectedType == LedgerType.PAYABLE,
                        onClick = { selectedType = LedgerType.PAYABLE },
                        label = { Text("DENA (PAYABLE)") },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chip_type_payable")
                    )
                }

                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it.uppercase() },
                    label = { Text("COUNTERPARTY NAME (UPPERCASE)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_counterparty_name")
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("PRINCIPAL AMOUNT (BDT)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_principal_amount")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("PURPOSE / LEDGER NOTE (OPTIONAL)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_description")
                )

                OutlinedTextField(
                    value = txDateIsoInput,
                    onValueChange = { txDateIsoInput = it.trim() },
                    label = { Text("TX DATE YYYY-MM-DD (BLANK = UNKNOWN)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_transaction_date")
                )

                OutlinedTextField(
                    value = phoneOrRef,
                    onValueChange = { phoneOrRef = it.uppercase() },
                    label = { Text("PHONE OR REFERENCE ID (OPTIONAL)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone_ref")
                )

                Text(
                    text = "DUE SCHEDULE (ASIA/DHAKA):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayPresets.forEach { (days, label) ->
                        FilterChip(
                            selected = dueOffsetDays == days,
                            onClick = { dueOffsetDays = days },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTxDate = DueDateCalculator.parseIsoDateDhaka(txDateIsoInput)
                    val targetDue = dueOffsetDays?.let { offset ->
                        val todayStart = DueDateCalculator.normalizeToStartOfDayDhaka(System.currentTimeMillis())
                        DueDateCalculator.shiftDays(todayStart, offset)
                    }
                    onSave(
                        existing?.id,
                        counterparty,
                        phoneOrRef,
                        description,
                        selectedType,
                        amountInput,
                        parsedTxDate,
                        targetDue
                    )
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_save_entry_confirm")
            ) {
                Text("SAVE ENTRY")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun RecordRepaymentDialog(
    item: LedgerEntryWithRepayments,
    onDismiss: () -> Unit,
    onConfirmRepayment: (entryId: Long, amountInput: String, note: String) -> Unit
) {
    val entry = item.entry
    val remainingFormatted = MoneyMath.formatPlainAmount(entry.remainingMinor)
    var amountInput by remember { mutableStateOf(remainingFormatted) }
    var noteInput by remember { mutableStateOf("CASH SETTLEMENT") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "LOG REPAYMENT • ${entry.counterpartyName.uppercase()}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "OUTSTANDING BALANCE: ${MoneyMath.formatBdt(entry.remainingMinor)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val half = (entry.remainingMinor / 2L).coerceAtLeast(100L)
                            amountInput = MoneyMath.formatPlainAmount(half)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("50% PARTIAL")
                    }
                    OutlinedButton(
                        onClick = {
                            amountInput = MoneyMath.formatPlainAmount(entry.remainingMinor)
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("100% FULL")
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("REPAYMENT AMOUNT (BDT)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_repayment_amount")
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it.uppercase() },
                    label = { Text("REPAYMENT NOTE / RECEIPT REF") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_repayment_note")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRepayment(entry.id, amountInput, noteInput) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("btn_confirm_repayment")
            ) {
                Text("CONFIRM REPAYMENT")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
fun RepaymentHistoryDialog(
    item: LedgerEntryWithRepayments,
    hasStatementEntitlement: Boolean,
    onCopyStatement: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val entry = item.entry
    val repayments = item.sortedRepayments

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "REPAYMENT LOG • ${entry.counterpartyName.uppercase()}",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PRINCIPAL: ${MoneyMath.formatBdt(entry.principalMinor)} • " +
                                "REPAID: ${MoneyMath.formatBdt(entry.repaidMinor)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "REMAINING: ${MoneyMath.formatBdt(entry.remainingMinor)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (repayments.isEmpty()) {
                    Text(
                        text = "NO REPAYMENTS LOGGED FOR THIS ENTRY YET.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(repayments, key = { it.id }) { rep ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rep.note.uppercase(),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = "${formatDhakaDateUppercase(rep.paidAtEpochMillis)} • " +
                                                "BAL AFTER: ${MoneyMath.formatBdt(rep.remainingAfterMinor)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = MoneyMath.formatBdt(rep.amountMinor),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasStatementEntitlement) {
                Button(
                    onClick = {
                        val statement = buildString {
                            append("DENA-PAONA LEDGER STATEMENT: ${entry.counterpartyName.uppercase()} | ")
                            append("TYPE: ${entry.ledgerType.banglaTerm} | ")
                            append("PRINCIPAL: ${MoneyMath.formatBdt(entry.principalMinor)} | ")
                            append("REPAID: ${MoneyMath.formatBdt(entry.repaidMinor)} | ")
                            append("REMAINING: ${MoneyMath.formatBdt(entry.remainingMinor)} | ")
                            append("REPAYMENTS COUNT: ${repayments.size}")
                        }
                        onCopyStatement(statement)
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "COPY STATEMENT", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COPY STATEMENT")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("CLOSE")
            }
        }
    )
}
