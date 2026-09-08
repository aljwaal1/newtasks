# Google Play Closed Testing — Smart Tasks 1.3.5

## App identity

- App name: منبه المهام الذكي
- Package: `com.aljwaal.newtasks`
- Version name: `1.3.5`
- Version code: `12`
- Target SDK: `36`
- Minimum SDK: `21`
- Contact email: `fastunllocked2017@gmail.com`

## Release track

Use **Closed testing** for this build. Do not promote to Production until test ads are replaced with the real AdMob IDs and the production privacy/consent setup is reviewed.

## Ads declaration

Choose **Yes, my app contains ads**.

This closed-testing build uses Google's official demo AdMob App ID and demo adaptive banner unit. The alarm screen is intentionally ad-free to avoid accidental clicks around urgent alarm controls.

## Privacy policy URL after merge to main

`https://github.com/aljwaal1/newtasks/blob/main/PRIVACY_POLICY.md`

The same policy is also accessible inside the app.

## Data safety notes

Task titles, notes, categories, priorities, due dates, completion status, local backups, and diagnostic logs remain local unless the user manually exports or shares them.

Because Google Mobile Ads is present, do not declare the app as collecting no data at all without reviewing the Mobile Ads SDK disclosure. Google advertising services may process technical data such as IP address, device/ad identifiers when available, ad/app interactions, and diagnostics for ad delivery, measurement, and fraud prevention.

## Permissions / declarations to review in Play Console

The app uses:

- Notifications
- Exact alarms (`SCHEDULE_EXACT_ALARM`)
- Full-screen intent (`USE_FULL_SCREEN_INTENT`)
- Boot completed
- Vibration / wake lock
- Foreground media playback service during alarms
- Internet and network state for test ads

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
- دعم Android 5.0 فأعلى في هذه النسخة.

هذه النسخة مخصصة للاختبار المغلق وقد تعرض إعلانات Google تجريبية في أسفل الواجهات العادية.

## Before production

1. Replace Google demo AdMob App ID with the real app ID.
2. Replace demo adaptive banner unit with the real banner unit.
3. Configure consent/privacy messaging required for production audiences and regions.
4. Verify `app-ads.txt` for the production AdMob app.
5. Recheck Data safety and Ads declarations.
6. Increment versionCode for every new upload.
7. Build a signed AAB using the same upload keystore for all future updates.
