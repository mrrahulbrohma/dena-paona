# Security Policy

## Supported versions

Security fixes are currently considered for the latest code on the `main` branch. No stable production release has been declared yet.

## Reporting a vulnerability

Please do not publish exploitable vulnerabilities, credentials, private transaction data, or signing material in a public issue.

Use GitHub's private vulnerability reporting feature for this repository if it is enabled. If it is not available, contact the repository maintainer privately through their GitHub profile and include:

- A concise description of the issue.
- The potential impact.
- Reproduction steps or a proof of concept that does not expose real user data.
- Any suggested mitigation.

Please allow time for investigation before publicly disclosing a vulnerability.

## Data safety principles

- Never commit `.env`, keystores, signing passwords, API credentials, or real ledger exports.
- Treat debt, repayment, and contact records as sensitive personal data.
- Report accidental exposure promptly and rotate any exposed credentials.
