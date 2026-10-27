package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.LedgerEntryWithRepayments
import com.example.data.preferences.ThemeMode
import com.example.domain.entitlement.AppFeature
import com.example.domain.model.LedgerType
import com.example.ui.components.EntryFormDialog
import com.example.ui.components.RecordRepaymentDialog
import com.example.ui.components.RepaymentHistoryDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FilteredTypeLedgerScreen
import com.example.ui.screens.SearchLedgerScreen
import com.example.ui.screens.SettingsAndBackupScreen
import com.example.ui.theme.DenaPaonaTheme
import com.example.ui.viewmodel.LedgerViewModel

enum class DenaPaonaTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "DASHBOARD", Icons.Default.AccountBalanceWallet),
    RECEIVABLES("receivables", "PAONA", Icons.Default.ArrowDownward),
    PAYABLES("payables", "DENA", Icons.Default.ArrowUpward),
    SEARCH("search", "SEARCH", Icons.Default.Search),
    SETTINGS("settings", "SETTINGS", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenaPaonaApp(viewModel: LedgerViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var currentTab by rememberSaveable { mutableStateOf(DenaPaonaTab.DASHBOARD) }
    var showEntryFormDialog by remember { mutableStateOf(false) }
    var formDefaultType by remember { mutableStateOf(LedgerType.RECEIVABLE) }
    var editingEntry by remember { mutableStateOf<LedgerEntryWithRepayments?>(null) }
    var repaymentTarget by remember { mutableStateOf<LedgerEntryWithRepayments?>(null) }
    var historyTarget by remember { mutableStateOf<LedgerEntryWithRepayments?>(null) }

    if (currentTab != DenaPaonaTab.DASHBOARD) {
        BackHandler {
            currentTab = DenaPaonaTab.DASHBOARD
        }
    }

    DenaPaonaTheme(themeMode = uiState.themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DENA-PAONA",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("top_bar_title")
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "V0.0.1",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val nextMode = if (uiState.themeMode == ThemeMode.LIGHT) {
                                    ThemeMode.DARK
                                } else {
                                    ThemeMode.LIGHT
                                }
                                viewModel.setThemeMode(nextMode)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("btn_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (uiState.themeMode == ThemeMode.LIGHT) {
                                    Icons.Default.DarkMode
                                } else {
                                    Icons.Default.LightMode
                                },
                                contentDescription = "TOGGLE LIGHT OR DARK THEME"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                if (currentTab != DenaPaonaTab.SETTINGS) {
                    FloatingActionButton(
                        onClick = {
                            editingEntry = null
                            formDefaultType = if (currentTab == DenaPaonaTab.PAYABLES) {
                                LedgerType.PAYABLE
                            } else {
                                LedgerType.RECEIVABLE
                            }
                            showEntryFormDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_entry")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "ADD LEDGER ENTRY")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NEW ENTRY", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DenaPaonaTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(visible = uiState.feedbackBanner != null) {
                    uiState.feedbackBanner?.let { bannerText ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("feedback_banner"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bannerText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissFeedbackBanner() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "DISMISS BANNER",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        DenaPaonaTab.DASHBOARD -> DashboardScreen(
                            uiState = uiState,
                            onAddReceivable = {
                                editingEntry = null
                                formDefaultType = LedgerType.RECEIVABLE
                                showEntryFormDialog = true
                            },
                            onAddPayable = {
                                editingEntry = null
                                formDefaultType = LedgerType.PAYABLE
                                showEntryFormDialog = true
                            },
                            onLogRepayment = { repaymentTarget = it },
                            onViewHistory = { historyTarget = it },
                            onEditEntry = {
                                editingEntry = it
                                formDefaultType = it.entry.ledgerType
                                showEntryFormDialog = true
                            },
                            onDeleteEntry = {
                                viewModel.deleteEntry(it.entry.id, it.entry.counterpartyName)
                            },
                            onNavigateToSettings = { currentTab = DenaPaonaTab.SETTINGS }
                        )

                        DenaPaonaTab.RECEIVABLES -> FilteredTypeLedgerScreen(
                            type = LedgerType.RECEIVABLE,
                            entries = uiState.receivableEntries,
                            totalMinor = uiState.metrics.totalReceivablesMinor,
                            onAddClick = {
                                editingEntry = null
                                formDefaultType = LedgerType.RECEIVABLE
                                showEntryFormDialog = true
                            },
                            onLogRepayment = { repaymentTarget = it },
                            onViewHistory = { historyTarget = it },
                            onEditEntry = {
                                editingEntry = it
                                formDefaultType = LedgerType.RECEIVABLE
                                showEntryFormDialog = true
                            },
                            onDeleteEntry = {
                                viewModel.deleteEntry(it.entry.id, it.entry.counterpartyName)
                            }
                        )

                        DenaPaonaTab.PAYABLES -> FilteredTypeLedgerScreen(
                            type = LedgerType.PAYABLE,
                            entries = uiState.payableEntries,
                            totalMinor = uiState.metrics.totalPayablesMinor,
                            onAddClick = {
                                editingEntry = null
                                formDefaultType = LedgerType.PAYABLE
                                showEntryFormDialog = true
                            },
                            onLogRepayment = { repaymentTarget = it },
                            onViewHistory = { historyTarget = it },
                            onEditEntry = {
                                editingEntry = it
                                formDefaultType = LedgerType.PAYABLE
                                showEntryFormDialog = true
                            },
                            onDeleteEntry = {
                                viewModel.deleteEntry(it.entry.id, it.entry.counterpartyName)
                            }
                        )

                        DenaPaonaTab.SEARCH -> SearchLedgerScreen(
                            uiState = uiState,
                            onQueryChange = viewModel::updateSearchQuery,
                            onFilterChange = viewModel::updateSearchFilter,
                            onLogRepayment = { repaymentTarget = it },
                            onViewHistory = { historyTarget = it },
                            onEditEntry = {
                                editingEntry = it
                                formDefaultType = it.entry.ledgerType
                                showEntryFormDialog = true
                            },
                            onDeleteEntry = {
                                viewModel.deleteEntry(it.entry.id, it.entry.counterpartyName)
                            }
                        )

                        DenaPaonaTab.SETTINGS -> SettingsAndBackupScreen(
                            uiState = uiState,
                            onSelectTheme = viewModel::setThemeMode,
                            onSelectSandboxOverride = viewModel::setSandboxOverride,
                            onGenerateBackupJson = viewModel::generateBackupJson,
                            onRestoreBackupJson = { json -> viewModel.restoreFromBackupJson(json) },
                            onResetSeedData = viewModel::resetToSixSeedRecords,
                            onVerifyPaymentReference = viewModel::verifyPaymentTransactionReference,
                            onRecordReferralShare = viewModel::recordReferralShare,
                            onShowFeedback = viewModel::showFeedback
                        )
                    }
                }
            }
        }

        if (showEntryFormDialog) {
            EntryFormDialog(
                initialEntry = editingEntry,
                defaultType = formDefaultType,
                onDismiss = {
                    showEntryFormDialog = false
                    editingEntry = null
                },
                onSave = { id, counterparty, phoneOrRef, desc, type, amountStr, txMillis, dueMillis ->
                    viewModel.saveEntry(
                        editingEntryId = id,
                        counterpartyName = counterparty,
                        phoneOrReference = phoneOrRef,
                        description = desc,
                        type = type,
                        amountInput = amountStr,
                        transactionDateEpochMillis = txMillis,
                        dueDateEpochMillis = dueMillis,
                        onSuccess = {
                            showEntryFormDialog = false
                            editingEntry = null
                        }
                    )
                }
            )
        }

        repaymentTarget?.let { target ->
            RecordRepaymentDialog(
                item = target,
                onDismiss = { repaymentTarget = null },
                onConfirmRepayment = { entryId, amountStr, note ->
                    viewModel.recordRepayment(
                        entryId = entryId,
                        amountInput = amountStr,
                        note = note,
                        onSuccess = { repaymentTarget = null }
                    )
                }
            )
        }

        historyTarget?.let { target ->
            val refreshed = uiState.allEntries.firstOrNull { it.entry.id == target.entry.id } ?: target
            RepaymentHistoryDialog(
                item = refreshed,
                hasStatementEntitlement = uiState.entitlementState.isFeatureAllowed(
                    AppFeature.STATEMENT_DIGEST_EXPORT
                ),
                onCopyStatement = { statement ->
                    clipboardManager.setText(AnnotatedString(statement))
                    viewModel.showFeedback("LEDGER STATEMENT COPIED TO CLIPBOARD.")
                    historyTarget = null
                },
                onDismiss = { historyTarget = null }
            )
        }
    }
}
