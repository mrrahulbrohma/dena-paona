# Security Policy

## Scope

DENA-PAONA stores personal financial records. Security reports involving private ledger data, backup/restore, local storage, dependency vulnerabilities, build secrets, release signing, or data integrity are in scope.

## Reporting a vulnerability

Please do **not** publish exploitable vulnerability details, real financial records, screenshots containing private information, signing keys, passwords, API keys, or access tokens in a public issue.

Use GitHub's private vulnerability reporting feature for this repository if it is enabled. Otherwise, contact the repository maintainer privately through the contact option on the maintainer's GitHub profile and include:

- A concise description and potential impact.
- Affected version/commit and Android version, where known.
- Reproduction steps using synthetic data only.
- Any suggested mitigation.

Do not attach a real user's ledger database or backup. If the report contains personal data, remove it before sending.

## Response expectations

This is a community-maintained project without a guaranteed response SLA. The maintainer will assess reports as time permits and may request safe reproduction details. Please allow a reasonable period for investigation before public disclosure.

## Supported versions

Security fixes are primarily intended for the latest code on the default branch. Older releases may not receive fixes.

## Security notes

- Never commit `local.properties`, `.env`, debug/release keystores, passwords, signing material, or user backup files.
- The debug signing key is for local testing only. Do not reuse it for release signing.
- Report suspected accidental secret exposure promptly so the credential can be revoked or rotated.
- Do not assume that a successful build, open-source license, or Play Protect warning alone proves the app safe or unsafe.
