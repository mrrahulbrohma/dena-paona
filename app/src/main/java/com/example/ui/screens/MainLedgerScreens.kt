package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.local.LedgerEntryWithRepayments
import com.example.domain.entitlement.AppFeature
import com.example.domain.model.AgingBucket
import com.example.domain.model.DueStatus
import com.example.domain.model.LedgerType
import com.example.domain.model.MoneyMath
import com.example.ui.components.LedgerEntryCard
import com.example.ui.components.NetPositionHeroCard
import com.example.ui.components.TrialEntitlementBanner
import com.example.ui.components.formatDhakaDateUppercase
import com.example.ui.viewmodel.LedgerUiState
import com.example.ui.viewmodel.SearchFilterChip

@Composable
fun DashboardScreen(
    uiState: LedgerUiState,
    onAddReceivable: () -> Unit,
    onAddPayable: () -> Unit,
    onLogRepayment: (LedgerEntryWithRepayments) -> Unit,
    onViewHistory: (LedgerEntryWithRepayments) -> Unit,
    onEditEntry: (LedgerEntryWithRepayments) -> Unit,
    onDeleteEntry: (LedgerEntryWithRepayments) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val priorityOverdueOrToday = uiState.allEntries.filter {
        val status = it.entry.evaluateDue().status
        status == DueStatus.OVERDUE || status == DueStatus.DUE_TODAY
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TrialEntitlementBanner(
                entitlementState = uiState.entitlementState,
                onOpenSettingsClick = onNavigateToSettings
            )
        }

        item {
            NetPositionHeroCard(
                metrics = uiState.metrics,
                onAddReceivable = onAddReceivable,
                onAddPayable = onAddPayable
            )
        }

        // Priority Overdue & Due Today Section
        if (priorityOverdueOrToday.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "OVERDUE ALERT",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PRIORITY ATTENTION (${priorityOverdueOrToday.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${uiState.metrics.overdueCount} OVERDUE • ${uiState.metrics.dueTodayCount} DUE TODAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overdue Aging Analytics (21-Day Trial / Pro Entitlement Boundary Card)
        item {
            val agingAllowed = uiState.entitlementState.isFeatureAllowed(AppFeature.OVERDUE_AGING_ANALYTICS)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_aging_analytics"),
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OVERDUE AGING BUCKET ANALYTICS",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (agingAllowed) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ) {
                            Text(
                                text = if (agingAllowed) "TRIAL / PRO ACTIVE" else "TRIAL EXPIRED",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (agingAllowed) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (agingAllowed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AgingBucket.DAYS_1_TO_7 to "1-7D OVERDUE",
                                AgingBucket.DAYS_8_TO_30 to "8-30D OVERDUE",
                                AgingBucket.DAYS_31_PLUS to "31D+ OVERDUE"
                            ).forEach { (bucket, label) ->
                                val amountMinor = uiState.metrics.agingBreakdownMinor[bucket] ?: 0L
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = MoneyMath.formatBdt(amountMinor),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "TRIAL EXPIRED",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "21-DAY TRIAL EXPIRED FOR AGING BUCKET ANALYTICS. CORE OFFLINE LEDGER & BACKUP REMAIN 100% UNLOCKED.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Global Recent Repayment History Section
        if (uiState.allRepayments.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_recent_repayments"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "REPAYMENT HISTORY",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RECENT REPAYMENT AUDIT LOG (${uiState.allRepayments.size})",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        uiState.allRepayments.take(4).forEach { rep ->
                            val matchingEntry = uiState.allEntries.firstOrNull { it.entry.id == rep.entryId }?.entry
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${matchingEntry?.counterpartyName ?: "ENTRY #${rep.entryId}"} • ${rep.note}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${MoneyMath.formatBdt(rep.amountMinor)} (${formatDhakaDateUppercase(rep.paidAtEpochMillis)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "ALL LEDGER RECORDS (${uiState.allEntries.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(uiState.allEntries, key = { it.entry.id }) { item ->
            LedgerEntryCard(
                item = item,
                onLogRepaymentClick = onLogRepayment,
                onViewHistoryClick = onViewHistory,
                onEditClick = onEditEntry,
                onDeleteClick = onDeleteEntry
            )
        }
    }
}

@Composable
fun FilteredTypeLedgerScreen(
    type: LedgerType,
    entries: List<LedgerEntryWithRepayments>,
    totalMinor: Long,
    onAddClick: () -> Unit,
    onLogRepayment: (LedgerEntryWithRepayments) -> Unit,
    onViewHistory: (LedgerEntryWithRepayments) -> Unit,
    onEditEntry: (LedgerEntryWithRepayments) -> Unit,
    onDeleteEntry: (LedgerEntryWithRepayments) -> Unit,
    modifier: Modifier = Modifier
) {
    val isReceivable = type == LedgerType.RECEIVABLE
    val accentColor = if (isReceivable) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(if (isReceivable) "screen_receivables" else "screen_payables"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, accentColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isReceivable) {
                                "TOTAL RECEIVABLES (PAONA)"
                            } else {
                                "TOTAL PAYABLES (DENA)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyMath.formatBdt(totalMinor),
                            style = MaterialTheme.typography.displayMedium,
                            color = accentColor,
                            modifier = Modifier.testTag(
                                if (isReceivable) "header_total_receivables" else "header_total_payables"
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${entries.size} RECORDS IN LEDGER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddClick,
                        modifier = Modifier
                            .heightIn(min = 52.dp)
                            .testTag(if (isReceivable) "btn_add_receivable_tab" else "btn_add_payable_tab"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "ADD ENTRY", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ADD ${type.banglaTerm}")
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NO ${type.banglaTerm} RECORDS FOUND",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP 'ADD ${type.banglaTerm}' ABOVE TO CREATE AN OFFLINE LEDGER ENTRY.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(entries, key = { it.entry.id }) { item ->
                LedgerEntryCard(
                    item = item,
                    onLogRepaymentClick = onLogRepayment,
                    onViewHistoryClick = onViewHistory,
                    onEditClick = onEditEntry,
                    onDeleteClick = onDeleteEntry
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchLedgerScreen(
    uiState: LedgerUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (SearchFilterChip) -> Unit,
    onLogRepayment: (LedgerEntryWithRepayments) -> Unit,
    onViewHistory: (LedgerEntryWithRepayments) -> Unit,
    onEditEntry: (LedgerEntryWithRepayments) -> Unit,
    onDeleteEntry: (LedgerEntryWithRepayments) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_search"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onQueryChange(it.uppercase()) },
                label = { Text("SEARCH COUNTERPARTY, PHONE, NOTE OR STATUS") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "SEARCH")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "CLEAR SEARCH")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_query")
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchFilterChip.entries.forEach { chip ->
                    FilterChip(
                        selected = uiState.searchFilter == chip,
                        onClick = { onFilterChange(chip) },
                        label = { Text(chip.label, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("filter_chip_${chip.name.lowercase()}")
                    )
                }
            }
        }

        item {
            Text(
                text = "MATCHING RECORDS (${uiState.searchResults.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.searchResults, key = { it.entry.id }) { item ->
            LedgerEntryCard(
                item = item,
                onLogRepaymentClick = onLogRepayment,
                onViewHistoryClick = onViewHistory,
                onEditClick = onEditEntry,
                onDeleteClick = onDeleteEntry
            )
        }
    }
}
