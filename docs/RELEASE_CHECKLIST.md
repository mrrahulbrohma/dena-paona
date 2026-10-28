# Android Release Readiness Checklist

This checklist is a working record, not a claim that the app is approved by Google Play.

## Before a test release

- [ ] Confirm the final package/application ID. Changing it after release creates a separate Android app.
- [ ] Confirm app name, launcher icon, version name, and monotonically increasing version code.
- [ ] Restore and commit the Gradle Wrapper scripts and matching `gradle-wrapper.jar`; verify the wrapper distribution URL.
- [ ] Build with a supported JDK, Android SDK, and pinned Gradle distribution.
- [ ] Run `testDebugUnitTest` and review failures rather than treating a compile as a full test pass.
- [ ] Verify the six seed records are inserted once and display receivables BDT 200, payables BDT 110, and net position BDT +90.
- [ ] Test add/edit, partial repayment, settlement, deletion confirmation, database migration, backup, and restore using synthetic data.
- [ ] Test on physical devices, font scaling, dark/light themes, and screen reader navigation.
- [ ] Inspect merged manifest and dependency tree for permissions, SDKs, and unexpected network activity.
- [ ] Check that no real financial data, debug credentials, local properties, or signing keys are tracked.

## Signing and artifacts

- [ ] Keep the debug keystore for local testing only.
- [ ] Create and back up a separate release/upload key securely; never commit it.
- [ ] Inject signing credentials through a secure local environment or CI secret store only.
- [ ] Build and verify a release APK for direct distribution, if needed.
- [ ] Build and verify a release Android App Bundle (AAB) for Google Play.
- [ ] Confirm installation/update behavior and package identity before distributing.
- [ ] Keep a record of artifact checksums and the source commit used for each release.

## Privacy and Play Console

- [ ] Publish a privacy policy at a stable public HTTPS URL that matches the actual app behavior.
- [ ] Complete the Play Console Data safety form based on the actual manifest, SDKs, and runtime behavior.
- [ ] Complete content rating, target audience, store listing, screenshots, icon, and app access declarations as applicable.
- [ ] Verify the current Play Console target API and testing requirements at submission time.
- [ ] Configure Google Play Billing products and server-side purchase verification before enabling paid digital features.
- [ ] Remove any direct-distribution payment flow from a Play-distributed build unless the current policy explicitly allows it.
- [ ] Confirm account deletion requirements if account creation is introduced.
- [ ] Review app access, data deletion, backup behavior, and retention disclosures.
- [ ] Complete required internal/closed testing and any developer-account-specific requirements shown in Play Console.

## Do not claim complete until

- [ ] The release build and tests were actually run.
- [ ] All release-blocking failures are resolved or explicitly documented.
- [ ] Payment, billing, referral, and cloud features are clearly labeled as unconfigured until real integrations are deployed and verified.
- [ ] Play Console review is complete. A local build does not mean Google Play approval.
