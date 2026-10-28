# Contributing to DENA-PAONA

Thanks for considering a contribution.

## Before you start

- Search existing issues before opening a new one.
- For substantial changes, open an issue first to discuss the proposed approach.
- Never commit passwords, API keys, signing keys, keystores, local configuration, or personal financial data.

## Development setup

1. Install Android Studio and a compatible Android SDK.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to finish.
4. Build the debug variant and run the available unit tests before submitting changes.

The repository currently configures Gradle 9.3.1 in its wrapper properties. If the Gradle Wrapper scripts are missing from your checkout, use the Gradle version configured in CI until the wrapper is restored.

## Pull requests

- Keep each pull request focused.
- Explain the problem, solution, and any user-visible changes.
- Include tests for business logic where practical.
- Do not invent transaction dates or due dates in seed or migration data.
- Preserve exact money calculations using integer minor units; avoid floating-point arithmetic for currency.
- Check accessibility, offline behavior, and database migration safety when relevant.
- Confirm the Android CI workflow passes.

## Reporting bugs

Use the bug report issue template when available. Include reproduction steps and relevant logs, but remove private data, tokens, and financial information first.
