# DENA-PAONA

**Offline-first personal ledger for money owed to you and money you owe.**

[![Android CI](https://github.com/mrrahulbrohma/dena-paona/actions/workflows/android-ci.yml/badge.svg)](https://github.com/mrrahulbrohma/dena-paona/actions/workflows/android-ci.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

- **Status:** Development / testing. Not represented as Google Play approved.
- **Current app version:** `0.0.1` (version code `1`)
- **Platform:** Native Android, Kotlin, Jetpack Compose, Material 3
- **Local storage:** Room / SQLite, with preferences stored locally
- **License:** Apache-2.0; see [LICENSE](LICENSE)

DENA-PAONA (DENA = payable, PAONA = receivable) is designed to help an individual keep separate, persistent records of money they owe and money other people owe them. Core ledger actions are intended to work offline. This project is not accounting, lending, or financial advice.

## Features in the codebase

- Local Room database and reactive state using Kotlin Coroutines / Flow.
- Separate receivable and payable entries.
- Exact monetary values stored in integer minor units (paisa), with decimal-safe parsing and formatting.
- Repayment entries and remaining-balance calculations.
- Seed-once initial sample ledger records.
- Search, summaries, preferences, and JSON backup / restore flows.
- 21-day premium-trial and feature-entitlement scaffolding.
- Payment and referral abstractions that are designed not to claim verification or rewards when no trusted service is configured.
- Light and dark theme support and locally bundled Inter font.

Implementation status varies by feature. A source file, UI, or interface does not by itself prove that an end-to-end service is configured. In particular, Google Play Billing, external payment verification, server-authoritative referrals, and cloud features must be independently configured and tested before production use.

## Initial ledger fixture

The source-defined first-run dataset contains six editable records. Dates that were not known are left unknown; they are not inferred.

| Direction | Counterparty | Amount |
|---|---|---:|
| Receivable | TAMIR | BDT 200 |
| Payable | SAKIB'S SHOP | BDT 30 |
| Payable | SAKIB'S SHOP | BDT 30 |
| Payable | SAKIB'S SHOP | BDT 20 |
| Payable | JONY BHAI'S SHOP | BDT 20 |
| Payable | KALAM MAMA'S SHOP | BDT 10 |

Expected first-run figures: **receivables BDT 200**, **payables BDT 110**, **net position BDT +90**. Treat this as a fixture derived from personal notes, not as a verified financial statement.

## Repository layout

```text
app/src/main/java/com/example/
├── data/        Room database, repositories, preferences, backup/restore
├── domain/      Money math, due dates, entitlements and integration boundaries
└── ui/          Compose screens, components, theme and ViewModel
app/src/test/    Unit and Robolectric tests
.github/         CI workflow, issue templates and pull-request template
docs/            Release checklist and privacy-policy draft
```

## Development setup

### Requirements

- Android Studio with a compatible Android SDK.
- JDK 17 or another JDK version supported by the pinned Android Gradle Plugin / Gradle combination.
- Gradle **9.3.1** for the current CI build configuration.
- Android SDK Platform **36.1** as requested by the current app module.

Set the Android SDK path through Android Studio's SDK Manager. Do not commit `local.properties`.

### Build and test

The repository currently contains `gradle/wrapper/gradle-wrapper.properties`, but the generated wrapper scripts and `gradle-wrapper.jar` are not checked in on this branch. Until the wrapper is restored, install/use the pinned Gradle distribution and run from the repository root:

**Windows PowerShell**
```powershell
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

**macOS / Linux**
```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

The local Android SDK must be installed and discoverable. If the build asks for a package license or SDK package, install/accept it through Android Studio's SDK Manager. The CI workflow sets up the required JDK, Android SDK packages, and Gradle version on a clean GitHub runner.

When the wrapper scripts and matching JAR have been restored to the repository, prefer the wrapper for reproducible local builds:

- Windows: `.gradlew.bat :app:assembleDebug`
- macOS / Linux: `./gradlew :app:assembleDebug`

### Build outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release Android App Bundle (AAB): `app/build/outputs/bundle/release/app-release.aab` (only after release signing is securely configured)

A successful debug build is not a signed Play Store release and does not mean Play review or Play Protect approval. The debug keystore is for development only. Never commit release signing keys or passwords.

## Security and privacy

Financial entries and exported backups may contain sensitive personal information. Use synthetic data in bug reports and screenshots. Do not upload real ledgers, backups, signing keys, `.env`, or device-specific configuration to issues or pull requests.

- Read [SECURITY.md](SECURITY.md) before reporting a vulnerability.
- Read the [privacy policy draft](docs/PRIVACY_POLICY_DRAFT.md); it is not yet a published legal policy. Verify actual manifest, dependency, and runtime behavior before publishing.
- Review [the release checklist](docs/RELEASE_CHECKLIST.md) before distributing an artifact.
- See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution and local build guidance.

## Open-source contributions

Issues and pull requests are welcome. Use the supplied templates, explain how you reproduced an issue, and include exact commands/results for tests you ran. Do not claim tests passed unless they were actually executed.

## Roadmap / release blockers

- Restore and commit Gradle Wrapper scripts plus the matching wrapper JAR, then validate a clean clone.
- Run CI and resolve any failing build or tests.
- Review package/application ID before the first public release.
- Verify the release signing flow and produce a signed AAB.
- Complete privacy, data-safety, testing, and store-listing requirements against the actual release artifact.
- Configure and validate any billing, payment, referral, or cloud backend before enabling it in production.

See [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) for the release checklist.

## License

DENA-PAONA is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
