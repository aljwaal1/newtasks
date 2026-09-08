# Google Play Data safety — Smart Tasks 1.3.5

This file is a working guide for the Play Console Data safety form. Recheck it whenever the Google Mobile Ads SDK or app features change.

## App-owned task data

The app stores the following locally on the device and does not automatically transmit it to the developer:

- task title
- notes
- category
- priority
- due date/time
- completion state
- local backup data
- local diagnostic log

The user can manually export/share backup or log files using Android's share sheet.

## Google Mobile Ads SDK 25.4.0

Google's current disclosure for Mobile Ads SDK 25.4.0 states that the SDK automatically collects and shares technical data for advertising, analytics, and fraud prevention.

When completing Play Console, review/declare the categories that correspond to:

1. **Approximate location** — IP address can be used to estimate general location.
2. **App activity / App interactions** — app launch, taps, ad/video interactions.
3. **App info and performance / Diagnostics** — SDK/app performance diagnostics such as launch time and hang/energy information.
4. **Device or other IDs** — advertising ID, app set ID, and applicable device/account identifiers.

Google states this SDK data is encrypted in transit with TLS.

## Purposes to review

For the Google Mobile Ads SDK data above, review the purposes corresponding to:

- Advertising or marketing
- Analytics
- Fraud prevention, security, and compliance

## Sharing

Because Google's disclosure states the Mobile Ads SDK collects **and shares** the listed technical data, do not answer the Play form as if the entire application shares no data merely because task content is local.

## Consent

The app integrates Google UMP. Consent information is refreshed before requesting ads, and the privacy-options entry point appears when UMP reports it is required.

## Data deletion / accounts

Smart Tasks does not create a developer-hosted user account. Users can delete tasks and local app data from within the app/device. The app does not maintain a server-side task database belonging to the developer.

## Security practices

- Task data is stored locally unless the user exports it.
- Advertising/consent traffic uses the Google SDK and HTTPS/TLS.
- Signing credentials are not stored in the source repository.

## Recheck before submitting

Before clicking Save in Play Console, compare this guide against Google's current Mobile Ads SDK data disclosure page and any additional SDKs added later.
