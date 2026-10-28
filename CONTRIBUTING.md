# Contributing to DENA-PAONA

Thank you for helping improve DENA-PAONA. This is a personal financial ledger, so correctness, privacy, and preserving user records take priority over cosmetic changes.

## Before opening an issue

- Search existing issues first.
- Never include real people's names, financial balances, account details, screenshots containing personal financial data, API keys, or signing credentials.
- For security vulnerabilities, follow [SECURITY.md](SECURITY.md) and do not disclose exploitable details in a public issue.

## Development setup

1. Install Android Studio and its Android SDK.
2. Clone the repository and open the repository root in Android Studio.
3. Let Gradle sync complete.
4. Use the Gradle wrapper when available:
   - Windows PowerShell: ` .\gradlew.bat :app:testDebugUnitTest `
   - macOS/Linux: `./gradlew :app:testDebugUnitTest`
5. Build a debug APK:
   - Windows PowerShell: ` .\gradlew.bat :app:assembleDebug `
   - macOS/Linux: `./gradlew :app:assembleDebug`

If wrapper scripts or `gradle-wrapper.jar` are missing from your checkout, do not invent replacements. Open an issue or regenerate the wrapper with a compatible Gradle installation, then commit the scripts and wrapper JAR. Never commit `local.properties`, local signing keys, or secrets.

## Pull request guidelines

- Keep changes focused and explain the reason for the change.
- Include tests for monetary calculations, repayments, due-date logic, migrations, backup/restore, and entitlement boundaries when relevant.
- Use integer minor currency units (paisa) or another exact decimal-safe representation. Never use floating-point arithmetic for money.
- Do not invent transaction dates, due dates, repayments, or payment confirmations.
- Never silently delete ledger records or mutate repayment history.
- Preserve offline operation for core ledger functionality.
- Do not add advertising or unnecessary data collection.
- Avoid unrelated UI rewrites and undocumented dependency upgrades.
- Include build/test results and list anything that was not run.

## Commit messages

Use clear, short messages such as `fix: prevent repayment overpayment`, `test: cover due date boundaries`, or `docs: clarify local-only features`.

## License

By contributing, you agree that your contributions are provided under the repository's Apache-2.0 license, unless a different written agreement applies.
