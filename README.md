# DENA-PAONA (`v0.0.1`)

[![Android CI](https://github.com/mrrahulbrohma/dena-paona/actions/workflows/android-ci.yml/badge.svg)](https://github.com/mrrahulbrohma/dena-paona/actions/workflows/android-ci.yml)

> **Development status:** This repository is under active development. A successful local debug build is not a production release or a guarantee of Google Play approval. Review the package identity, release signing, privacy disclosures, and app behavior before publishing.

**ANDROID APK · OFFLINE-FIRST PERSONAL DEBT & RECEIVABLES LEDGER**

> This repository is organized around the installable Android APK. Download the latest build from GitHub Actions artifacts or the Releases page when a release APK is published. The current CI artifact is a debug/testing APK, not a Play Store release.

- **Repository:** [https://github.com/mrrahulbrohma/dena-paona](https://github.com/mrrahulbrohma/dena-paona)
- **Version:** `0.0.1` (`versionCode = 1`)
- **Stack:** Kotlin, Jetpack Compose, Material 3, Room SQLite (KSP), Coroutines & Flow, MVVM
- **License:** [Apache License 2.0](./LICENSE)

---

## 1. Overview & Key Capabilities

**DENA-PAONA** is a native Android personal debt (`DENA` / Payables) and receivables (`PAONA` / Receivables) ledger designed for zero-latency offline reliability, exact monetary arithmetic, and full user data sovereignty.

### Core Features
1. **Offline-First SQLite Ledger (Room):** Complete local persistence with reactive `Flow` queries and zero external network dependencies required for daily ledger operations.
2. **Exact Monetary Arithmetic (`MoneyMath`):** All monetary values are stored in integer minor units (`1 BDT = 100 paisa`) and parsed/calculated with `BigDecimal` (`RoundingMode.HALF_UP`) to eliminate floating-point drift across partial repayments and net position totals.
3. **Immutable Repayment History (`RepaymentEntity`):** Every partial or full repayment is recorded atomically inside a Room `@Transaction`, updating the parent entry's repaid balance while preserving the full chronological audit trail.
4. **Idempotent Initial Seed & Safe Migration (`InitialSeedData`):** Seeds the six canonical initial records **strictly once** (and safely migrates legacy v1 demo installations without deleting user-created records or inventing missing dates):
   - **Receivable (`PAONA`):**
     - `1. TAMIR` — `BDT 200` • Transaction date: Unknown • Due date: Unknown • Note: *Money lent in early 2026.*
     - **Total Initial Receivables:** **`BDT 200`**
   - **Payables (`DENA`):**
     - `2. SAKIB'S SHOP` — `BDT 30` • Transaction date: Unknown • Due date: Unknown • Note: *Credit before Puja.*
     - `3. SAKIB'S SHOP` — `BDT 30` • Transaction date: Unknown • Due date: Unknown • Note: *Separate credit entry; exact date unknown.*
     - `4. SAKIB'S SHOP` — `BDT 20` • Transaction date: `2026-10-27` (`Asia/Dhaka`) • Due date: Unknown
     - `5. JONY BHAI'S SHOP` — `BDT 20` • Transaction date: Unknown • Due date: Unknown • Note: *Credit during Durga Puja.*
     - `6. KALAM MAMA'S SHOP` — `BDT 10` • Transaction date: Unknown • Due date: Unknown • Note: *Credit before Puja.*
     - **Total Initial Payables:** **`BDT 110`**
   - **Initial Net Position (`PAONA - DENA`):** **`BDT +90`**
5. **Minimal Accessible English Uppercase UI:** High-contrast Obsidian Dark and Paper Light themes, bundled local **Inter** typography (`res/font/inter.ttf`), uppercase labels with generous letter-spacing, and `48dp+` minimum touch targets.
6. **21-Day Premium Trial & Clear Entitlement Boundaries (`EntitlementEngine`):**
   - **Permanently Free Core Offline Ledger:** Adding/editing entries, logging repayments, viewing repayment history, searching/filtering, and JSON backup/restore **never expire** and are never locked behind a paywall.
   - **21-Day Trial / Pro Features:** Overdue aging bucket analytics (`1-7D`, `8-30D`, `31D+`) and one-tap formatted statement copy.
7. **Modular Payment & Referral Architecture:**
   - `PaymentGatewayModule` and `OfflineModularPaymentGateway` define a clean verification boundary that refuses to fake payment verification offline.
   - `ReferralModule` generates deterministic local invite codes and logs outbound shares locally without faking server-side referral rewards.

---

## 2. Project Structure

```text
app/src/main/java/com/example/ (current source namespace; review before release)
├── MainActivity.kt
├── data/
│   ├── backup/BackupRestoreManager.kt      # Deterministic JSON export & restore
│   ├── local/
│   │   ├── DenaPaonaDatabase.kt            # Room database configuration
│   │   ├── InitialSeedData.kt              # 6 canonical seed records (200 / 110 / +90)
│   │   ├── LedgerDao.kt                    # Atomic seed-once, repayment & restore transactions
│   │   └── LedgerEntities.kt               # LedgerEntryEntity, RepaymentEntity, SystemMetaEntity
│   ├── preferences/AppPreferencesRepository.kt
│   └── repository/LedgerRepository.kt      # MVVM repository & exact summary metrics
├── domain/
│   ├── entitlement/EntitlementEngine.kt    # 21-day trial, modular payment & referral contracts
│   └── model/
│       ├── DueDateCalculator.kt            # Deterministic UTC day-normalized overdue evaluation
│       └── MoneyMath.kt                    # Exact minor-unit & BigDecimal arithmetic
└── ui/
    ├── DenaPaonaApp.kt                     # Root scaffold, navigation & dialogs
    ├── components/LedgerComponents.kt      # Hero card, entry cards, forms & history modal
    ├── screens/
    │   ├── MainLedgerScreens.kt            # Dashboard, Receivables, Payables & Search screens
    │   └── SettingsAndBackupScreen.kt      # Theme, JSON backup/restore, trial & sandbox controls
    ├── theme/                              # Color.kt, Type.kt (Inter), Theme.kt
    └── viewmodel/LedgerViewModel.kt        # Reactive StateFlow MVVM state holder
```

---

## 3. Download or Build the APK

### Download a ready-to-install APK

1. Open the [GitHub Actions runs](https://github.com/mrrahulbrohma/dena-paona/actions/workflows/android-ci.yml).
2. Select the latest successful **Android CI** run.
3. In **Artifacts**, download `dena-paona-debug-apk` and extract the ZIP.
4. Install `app-debug.apk` on your Android device. Android may ask you to allow installation from that source; only install builds you trust.

**Important:** A CI/debug APK is for testing and may show Android/Play Protect warnings because it is not a Play-distributed release. Do not disable Play Protect to install it. If Android blocks it or reports a specific threat, stop and share the exact warning for review.

### Build an APK on your computer

## 4. Build & Test Commands

### Run Unit & Robolectric Tests
Run these commands from the project root with Gradle 9.3.1 and a compatible JDK installed. The GitHub Actions workflow runs the same checks on pushes and pull requests.

```bash
gradle :app:testDebugUnitTest
```

### Build Debug APK
```bash
gradle :app:assembleDebug
```
The compiled debug APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Build Release Android App Bundle (AAB)
**Do not use the debug APK as a Play Store release.** Before making a release build, confirm the final unique `applicationId` and `namespace` in `app/build.gradle.kts`, create and securely back up your own upload keystore, and verify that release signing is configured correctly. Never commit the keystore or its passwords.

Configure your signing keystore credentials via environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) and run:
```bash
gradle :app:bundleRelease
```
The signed release AAB will be generated at:
`app/build/outputs/bundle/release/app-release.aab`

---

## 5. Publishing a downloadable APK

For a public, stable download link, create a GitHub Release and attach the tested APK file under **Assets**. Do not label a debug APK as a production release. The repository currently has CI artifacts, but a binary APK has not been attached to a GitHub Release by these repository changes.

## 6. Exporting & Pushing to GitHub

To push this project from AI Studio to `https://github.com/mrrahulbrohma/dena-paona`:
1. Use the **Push to GitHub** or **Export ZIP** option in the AI Studio settings menu, or run from your local clone:
   ```bash
   git init
   git remote add origin https://github.com/mrrahulbrohma/dena-paona.git
   git checkout -b main
   git add .
   git commit -m "feat: initialize DENA-PAONA v0.0.1 offline ledger"
   git push -u origin main
   ```

---

## 7. License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](./LICENSE) for full details.
