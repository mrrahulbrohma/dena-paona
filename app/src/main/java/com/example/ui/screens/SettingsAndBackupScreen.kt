package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.data.preferences.ThemeMode
import com.example.domain.entitlement.AppFeature
import com.example.domain.entitlement.SandboxEntitlementOverride
import com.example.ui.viewmodel.LedgerUiState

@Composable
fun SettingsAndBackupScreen(
    uiState: LedgerUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectSandboxOverride: (SandboxEntitlementOverride) -> Unit,
    onGenerateBackupJson: ((String) -> Unit) -> Unit,
    onRestoreBackupJson: (String) -> Unit,
    onResetSeedData: () -> Unit,
    onVerifyPaymentReference: (String) -> Unit,
    onRecordReferralShare: () -> String,
    onShowFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var backupJsonText by remember { mutableStateOf(uiState.lastExportedBackupJson) }
    var paymentRefInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.lastExportedBackupJson) {
        if (uiState.lastExportedBackupJson.isNotBlank()) {
            backupJsonText = uiState.lastExportedBackupJson
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_settings"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Theme & Accessibility Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPEARANCE & ACCESSIBILITY",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UPPERCASE ENGLISH INTERFACE • INTER TYPOGRAPHY • 48DP+ TARGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.themeMode == mode,
                                onClick = { onSelectTheme(mode) },
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("chip_theme_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 2. JSON Backup & Restore Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_backup_restore"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "OFFLINE JSON BACKUP & RESTORE",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EXPORTS ALL LEDGER ENTRIES, MINOR-UNIT AMOUNTS, AND REPAYMENT HISTORY.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onGenerateBackupJson { exported ->
                                    backupJsonText = exported
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_export_backup")
                        ) {
                            Text("EXPORT JSON")
                        }

                        OutlinedButton(
                            onClick = {
                                if (backupJsonText.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(backupJsonText))
                                    onShowFeedback("BACKUP JSON COPIED TO CLIPBOARD.")
                                } else {
                                    onGenerateBackupJson { exported ->
                                        backupJsonText = exported
                                        clipboardManager.setText(AnnotatedString(exported))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_copy_backup")
                        ) {
                            Text("COPY JSON")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        label = { Text("BACKUP JSON PAYLOAD (PASTE TO RESTORE)") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_backup_json")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onRestoreBackupJson(backupJsonText) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_restore_backup")
                        ) {
                            Text("RESTORE FROM JSON")
                        }

                        OutlinedButton(
                            onClick = onResetSeedData,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_reset_seed_records"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("RESET 6 SEED RECORDS")
                        }
                    }
                }
            }
        }

        // 3. 21-Day Trial & Entitlement Boundaries Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "21-DAY PREMIUM TRIAL & ENTITLEMENT BOUNDARIES",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CURRENT TIER: ${uiState.entitlementState.tier.label} • " +
                            "REMAINING: ${uiState.entitlementState.daysRemainingInTrial} / ${uiState.entitlementState.trialTotalDays} DAYS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AppFeature.entries.forEach { feature ->
                        val allowed = uiState.entitlementState.isFeatureAllowed(feature)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (feature.requiresPremiumOrTrial) {
                                        "BOUNDARY: 21-DAY TRIAL OR PRO"
                                    } else {
                                        "BOUNDARY: PERMANENTLY FREE CORE LEDGER"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (allowed) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = if (allowed) "ACTIVE" else "LOCKED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (allowed) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "QA SANDBOX ENTITLEMENT CLOCK OVERRIDE:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SandboxEntitlementOverride.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.entitlementState.overrideMode == mode,
                                onClick = { onSelectSandboxOverride(mode) },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("chip_sandbox_${mode.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // 4. Modular Payment & Referral Architecture Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MODULAR PAYMENT & REFERRAL ARCHITECTURE",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "NO FAKE OFFLINE PAYMENT VERIFICATION OR FAKE REFERRAL REWARDS.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymentRefInput,
                        onValueChange = { paymentRefInput = it.uppercase() },
                        label = { Text("PAYMENT TRX REFERENCE (BACKEND VERIFICATION REQUIRED)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_payment_ref")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onVerifyPaymentReference(paymentRefInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_verify_payment")
                    ) {
                        Text("CHECK PAYMENT GATEWAY CONTRACT")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "LOCAL INVITE CODE: ${uiState.referralStatus.localInviteCode} • " +
                            "SHARES LOGGED: ${uiState.referralStatus.localShareCount}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.referralStatus.statusNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val msg = onRecordReferralShare()
                            clipboardManager.setText(AnnotatedString(msg))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_copy_referral_code")
                    ) {
                        Text("COPY INVITE CODE & LOG LOCAL SHARE")
                    }
                }
            }
        }

        // 5. About / License Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DENA-PAONA • VERSION 0.0.1",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "REPOSITORY: HTTPS://GITHUB.COM/MRRAHULBROHMA/DENA-PAONA\n" +
                            "LICENSE: APACHE LICENSE 2.0 • OFFLINE-FIRST PERSONAL LEDGER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
