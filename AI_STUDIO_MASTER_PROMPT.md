# DENA-PAONA — One-Pass Engineering Master Prompt (Google AI Studio / Android Studio)

You are the senior Android engineer, product designer, QA lead, security reviewer, and release engineer working on the EXISTING DENA-PAONA repository. Work in-place. Audit first, preserve working code, make minimal targeted changes, run commands/tests, inspect failures, fix root causes, and rerun until passing or until a clearly documented external blocker remains. Do not merely explain what should be done. Do not stop after generating a plan. Do not ask the user to copy multiple snippets between files.

## Mission
Deliver a production-oriented native Android app named **দেনা-পাওনা (DENA-PAONA)**, starting at version `0.0.1`, with English and Bangla UI, offline-first personal debt/receivables tracking, minimal modern mobile-finance UX inspired by the clarity of reputable payment apps (do not copy any brand assets), accessible for a wide range of ages, with restrained animation, exact accounting, safe backups, referral credits, a 21-day premium trial, and an honest path to Play Store readiness.

## First: inspect the real repository
1. Read the complete project tree, Gradle files/version catalog, Room entities/DAO/database/migrations, seed mechanism, repositories/ViewModels, all Compose screens/components/theme, strings/resources, manifest, backup/export, entitlement/referral/payment logic, and every test.
2. Establish a baseline by running the project’s actual Gradle wrapper, unit tests, lint if configured, and debug/release builds where credentials permit.
3. Report the exact baseline failures before changing anything. Never invent test results.
4. Preserve working code and user data. Do not replace the application wholesale, reset the database, or change the existing applicationId without a deliberate migration plan.

## Core product requirements
- Native Kotlin + Jetpack Compose + Material 3; Room SQLite; MVVM; Coroutines/Flow; DataStore or existing preferences architecture where appropriate.
- Offline-first ledger: ordinary entries, repayments, totals, search/filter, backup/restore, and core navigation must work without internet.
- App name: **দেনা-পাওনা (DENA-PAONA)**. Start version `0.0.1`, versionCode `1`; set package identity deliberately and never casually change applicationId after distribution.
- No ads, no mandatory account, no unnecessary permissions, no hidden network dependency. Open-source license and clear privacy/data handling.
- Currency BDT. Store money as integer poisha/minor units; never use Float/Double for financial arithmetic. Validate positive amounts and repayments. Preserve immutable repayment history and atomic transactions.
- Unknown transaction/due dates remain unknown/null. Never invent dates.
- Dashboard must show receivables (PAONA), payables (DENA), net position, outstanding amounts, due/overdue status, and transaction counts. Make it clear that net position is receivables minus payables.
- Per-entry note/comment, transaction date, optional due date, repayment history, edit/delete confirmation, search, filters, and accessible forms.
- Use Asia/Dhaka for date input, display, and calendar-day overdue calculation. Avoid UTC truncation/timezone drift. Unknown due dates must never appear overdue.
- Premium policy: first 21 days free; then optional BDT 100/month. Referral points may unlock defined premium features. Implement entitlement state transparently and test trial boundaries. Do not fake payment verification or grant premium solely because a user typed a transaction ID.
- bKash personal payment number: +8801689700023. Nagad personal payment number: +8801302539321. Clearly label manual payment as pending review; show copy actions, payment instructions and a reference submission field. Do not claim these personal transfers are automatically verified. Avoid collecting sensitive credentials or OTPs.
- No ads. Do not claim Google Play approval; include a Play-readiness checklist (privacy policy, Data Safety, target API, app signing, content rating, store listing, testing, permissions, payment-policy review).

## Exact canonical seed data
Create exactly these six starter entries on a genuinely fresh installation:
1. RECEIVABLE — TAMIR — BDT 200. Transaction date unknown; due date unknown; note: `Money lent in early 2026.`
2. PAYABLE — SAKIB'S SHOP — BDT 30. Transaction date unknown; due date unknown; note: `Credit before Puja.`
3. PAYABLE — SAKIB'S SHOP — BDT 30. Transaction date unknown; due date unknown; note: `Separate credit entry; exact date unknown.`
4. PAYABLE — SAKIB'S SHOP — BDT 20. Transaction date `2026-10-27` in Asia/Dhaka; due date unknown; note may be empty.
5. PAYABLE — JONY BHAI'S SHOP — BDT 20. Transaction date unknown; due date unknown; note: `Credit during Durga Puja.`
6. PAYABLE — KALAM MAMA'S SHOP — BDT 10. Transaction date unknown; due date unknown; note: `Credit before Puja.`

Expected principal totals: receivables BDT 200; payables BDT 110; net position BDT +90. Verify totals using integer minor units. Do not merge the two separate Sakib BDT 30 entries.

## Mandatory safe correction for already initialized databases
- Inspect existing seed metadata and all historical seed versions. Current data may already have been initialized with incorrect demo rows.
- Add a new, versioned, idempotent correction mechanism that runs even when an older seed-completion metadata key already exists.
- Delete or replace ONLY records proven to be unchanged app-generated demo/seed records via a strong exact signature or explicit seed-origin metadata. Never delete a record that has repayments, user edits, changed notes/dates/amounts, or cannot be confidently identified as app-generated.
- Preserve every user-created record, all repayment history, backup data, and IDs where possible. Do not clear the whole database or use destructive fallback migrations.
- Insert only missing canonical records; avoid duplicates across repeated app launches. Handle duplicate names by using full signatures and occurrence counts, not names alone.
- If a legacy row is ambiguous, preserve it and report the ambiguity rather than deleting user data. Add a safe reconciliation/diagnostic report and tests for fresh install, old demo installation, partially edited seed records, user-created records, repayments, repeated launch, and backup-restored data.
- Use Room schema migrations that preserve data. Do not assume a seed correction is the same as a schema migration.

## Bangla + English localization
- Provide a visible language selector in Settings: English and বাংলা. Persist the user’s choice across app restarts.
- Translate all user-facing UI text in both languages: navigation, labels, forms, hints, errors, confirmations, empty states, dates/statuses, premium/trial, referral, payment instructions, backup/restore, accessibility descriptions, and dialogs. No hard-coded English strings should remain in Compose screens where resources are appropriate.
- Use Android resource localization: `res/values/strings.xml` for English and `res/values-bn/strings.xml` for Bangla. Keep translation keys aligned and add a test/check to catch missing keys.
- Language change must update UI immediately if feasible, without deleting data or requiring reinstall. If Android version behavior requires app locale APIs, implement a correct compatible approach and document it.
- Keep the brand mark **DENA-PAONA / দেনা-পাওনা** readable in both languages. English-first is not allowed as the only language.

## UI/UX and animation
- Minimal, modern financial-app layout: clear hierarchy, generous spacing, high contrast, restrained color, large readable amounts, strong primary actions, familiar bottom navigation, obvious DENA vs PAONA separation.
- Use Material 3, Inter/Open Sans where available with a safe fallback for Bangla script; never force a Latin font that renders Bangla incorrectly.
- Buttons and touch targets should generally be at least 48dp. Support font scaling, screen readers, content descriptions, contrast, keyboard navigation where applicable, and older/less technical users.
- Add restrained Compose animations for navigation/content transitions, number/summary changes, sheet/dialog appearance and successful actions. Respect reduced-motion/accessibility needs; animation must not block interaction or harm performance.
- Avoid clutter, decorative charts without meaning, fake financial metrics, dark patterns, forced referrals, manipulative scarcity, ad-like banners, or lookalike copying of bKash/Nagad branding.
- Keep UI responsive on small screens and large font settings. Provide meaningful loading, empty, validation, and error states.

## Quality, security, and self-repair workflow
- Inspect every relevant source file before editing; prefer targeted changes over mass rewrites.
- After each logical change, compile. On failure, read the FIRST meaningful error and relevant stack trace, identify root cause, patch, and rerun. Do not hide errors or claim automatic self-repair beyond actual capabilities.
- Run unit tests, Room migration/DAO tests, timezone/date tests, money arithmetic tests, entitlement/referral tests, localization completeness checks, and a release build where possible.
- Add regression tests for: exact six seed records and totals; BDT minor-unit arithmetic; Dhaka midnight/date boundaries; null due dates; partial repayments; idempotent seed correction; preserving user data and repayment history; trial day 21/22; language preference persistence and complete translations.
- Check Android Studio sync and CI separately. Use the repository Gradle wrapper. Keep wrapper scripts and wrapper JAR checked in. Do not silently change Gradle/AGP/Kotlin versions to mask errors.
- Do not commit keystores, passwords, API keys, local.properties, signing credentials, real payment credentials, or user backups. Keep secrets out of logs.
- Never disable Play Protect or weaken Android security to bypass warnings.

## Release/versioning requirements
- App label is `দেনা-পাওনা (DENA-PAONA)`; user-visible version begins at `0.0.1`.
- Produce a clearly named APK such as `DENA-PAONA-v0.0.1.apk` only for a release build, not a debug build.
- Configure a signed release using a persistent signing key kept outside source control. All future APK updates must use the same applicationId and signing identity; increment versionCode on every release and update versionName intentionally.
- Add a GitHub Actions workflow that builds, tests, signs only when secrets are correctly configured, names the APK from version/tag, and attaches it to GitHub Releases. If signing secrets are absent, fail with a clear actionable message rather than publishing a debug APK as production.
- Ensure release workflow can run on a version tag and that Release Assets actually contain the APK. Verify by inspecting the run and release asset; do not claim success before verification.

## Final acceptance report
At the end, report:
1. Exact files changed and why.
2. Migration/seed safety behavior and what ambiguous legacy records were preserved.
3. Build/test/lint/release results with actual command output summaries and any failures/blockers.
4. Exact APK path and version only if the file exists and build succeeded.
5. Git commit/tag/Release URL only if actually created and verified.
6. Remaining Play Store requirements.
Do not say “all done” if any required test was not run or failed. Do not fabricate output.

## Negative prompt / prohibited shortcuts
Do NOT rewrite the app from scratch; do NOT remove working features; do NOT reset/delete all Room data; do NOT invent dates; do NOT merge distinct debts; do NOT use floating-point money; do NOT use UTC for Dhaka calendar-day logic; do NOT hard-code only English; do NOT ship fake/placeholder payment verification; do NOT commit secrets; do NOT publish debug APK as production; do NOT change applicationId or signing key casually; do NOT disable security protections; do NOT claim tests, Play approval, or release uploads without evidence.

Start now: inspect repository and establish baseline; then implement, test, fix, and verify the actual project end-to-end.
