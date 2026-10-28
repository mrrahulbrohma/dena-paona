# DENA-PAONA Privacy Policy — Draft

**Status: Draft for review, not yet a published legal notice.** Publish a stable HTTPS page and confirm every statement against the final APK, merged manifest, libraries, and any configured backend before linking this policy in the app or Play Console.

**Effective date:** To be set before publication  
**App:** DENA-PAONA  
**Maintainer/contact:** Use the repository maintainer's current public contact channel.

## Intended data behavior

DENA-PAONA is designed as an offline-first personal ledger for money owed to the user and money the user owes. Core ledger records are intended to be stored locally on the device. The app's source includes backup and restore functionality, so a user may choose to export a file or select a backup file to restore.

Financial records can be sensitive. Do not share backups or screenshots unless you intend to disclose the information they contain.

## Data collection and sharing

The project is intended to avoid advertising SDKs and unnecessary analytics. This draft does not establish that the compiled app has no network activity or third-party data processing. Before publication, maintainers must inspect the actual dependency list, merged manifest, runtime behavior, and any enabled cloud/payment/referral integrations, then list the data types, recipients, purposes, retention periods, and controls accurately.

If optional account, cloud sync, referral, subscription, or payment-claim services are enabled, update this policy to describe their exact data flows and processors before release.

## Permissions

The project is intended to request only permissions necessary for implemented features. Verify the merged manifest of the release artifact and explain every permission in the final published policy.

## Backup and deletion

Users should be told where backups are saved and that exported files can be copied or shared by the user outside the app. Document how users can delete local records, reset app data, or request account/cloud-data deletion if online accounts are implemented. Do not promise deletion from backups or third-party systems unless that process exists and is verified.

## Children's privacy

Before release, the maintainer must identify the intended audience and update this section to match applicable requirements and the actual product.

## Security

The project aims to minimize collection and keep core ledger features available offline. Do not interpret this as a guarantee of encryption at rest or absolute security. Describe any implemented encryption, backup protection, and transport security precisely.

## Changes and contact

Update this policy when data behavior changes. Add the effective date and a reliable contact channel before publication.
