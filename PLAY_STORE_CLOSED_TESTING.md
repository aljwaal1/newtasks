# Google Play Closed Testing — Smart Tasks 1.3.6

## App identity

- App name: منبه المهام الذكي
- Package: `com.aljwaal.newtasks`
- Version name: `1.3.6`
- Version code: `13`
- Target SDK: `36`
- Minimum SDK: `23` (Android 6.0+)
- Contact email: `fastunlocked2017@gmail.com`

## Release track

Use **Closed testing** for this build first. The signed Google Play workflow is prepared to build with live AdMob identifiers so genuine testers can receive monetized ads.

## Ads declaration

Choose **Yes, my app contains ads**.

Normal app screens use an anchored adaptive AdMob banner at the bottom. The urgent full-screen alarm activity is intentionally ad-free to keep Stop/Snooze controls separate from advertising and reduce accidental clicks.

## Required GitHub repository secrets

### Signing

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

### AdMob live revenue

- `ADMOB_APP_ID` — format `ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy`
- `ADMOB_BANNER_ID` — format `ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz`

The Google Play AAB workflow uses Google's demo IDs when the live AdMob IDs are not configured, and uses the real IDs automatically once both secrets are present.

## AdMob setup

Create/add this exact app in AdMob using package:

`com.aljwaal.newtasks`

Then create an **Adaptive banner** ad unit and put the resulting App ID and Banner Ad Unit ID in the two GitHub secrets above.

For the developer's own devices, enable test-device mode in AdMob while inspecting/clicking ads. Do not click your own live ads.

## Google UMP / consent

The app includes Google User Messaging Platform (UMP) and refreshes consent information before requesting ads. Publish the appropriate Privacy & messaging message in AdMob, including a European regulations message for EEA/UK/Switzerland traffic when applicable.

## Privacy policy URL

`https://apps.explapp.com/privacy-smarttasks.html`

The same policy is accessible inside the app.

## app-ads.txt

Developer website: `https://apps.explapp.com/`

Authorized sellers file: `https://apps.explapp.com/app-ads.txt`

Before production, verify the app's `app-ads.txt` status inside AdMob.

## Data safety notes

Task titles, notes, categories, priorities, due dates, completion status, local backups, and diagnostic logs remain local unless the user manually exports or shares them.

Because Google Mobile Ads is present, do not declare the app as collecting no data at all without reviewing the current Mobile Ads SDK disclosure. Google advertising services may process technical data such as IP address, device/ad identifiers when available, ad/app interactions, and diagnostics for ad delivery, measurement, and fraud prevention.

## Permissions / declarations to review in Play Console

The app uses:

- Notifications
- Exact alarms (`SCHEDULE_EXACT_ALARM`)
- Full-screen intent (`USE_FULL_SCREEN_INTENT`)
- Boot completed
- Vibration / wake lock
- Foreground media playback service during alarms
- Internet and network state for ads and privacy messaging

The core purpose of exact alarms and full-screen alarm behavior is time-sensitive task reminders. Complete any Google Play declarations shown for these permissions truthfully based on this core function.

## Store listing draft (Arabic)

### Short description

نظّم مهامك واضبط تنبيهات دقيقة مع تكرار وأولويات ونسخ احتياطي محلي.

### Full description

منبه المهام الذكي يساعدك على تنظيم مهامك اليومية ومواعيدك في واجهة عربية واضحة وسريعة.

يمكنك إنشاء المهام وتحديد التاريخ والوقت والأولوية والتصنيف والتكرار، مع تنبيهات قابلة للتحكم وخيارات لتأجيل التذكير أو تذكيرك في اليوم التالي أو في موعد جديد.

أهم المزايا:

- إضافة وتعديل وحذف وإكمال المهام.
- البحث والتصفية حسب الحالة والتاريخ والأولوية.
- تنبيهات دقيقة مع الصوت والاهتزاز.
- تكرار المهام حسب الحاجة.
- تصنيفات وأولويات قابلة للتخصيص.
- نسخ احتياطي محلي واستيراد وتصدير JSON.
- بيانات المهام الأساسية تبقى محليًا على جهازك.
- دعم Android 6.0 فأعلى في هذه النسخة.

يحتوي التطبيق على إعلانات Google AdMob في أسفل الواجهات العادية.

## Before production

1. Verify the real AdMob app is linked to `com.aljwaal.newtasks`.
2. Verify `ADMOB_APP_ID` and `ADMOB_BANNER_ID` secrets.
3. Publish the required AdMob Privacy & messaging consent messages.
4. Verify `app-ads.txt` for this app.
5. Recheck Data safety and Ads declarations.
6. Increment versionCode for every new upload.
7. Build every Google Play update with the same upload keystore.
8. Keep the developer's own devices in test-device mode when testing ad clicks.
