# Google Play permission declarations — Smart Tasks

Package: `com.aljwaal1.newtasks`

## Full-screen intent (`USE_FULL_SCREEN_INTENT`)

### Core use case

Smart Tasks lets users create a task and explicitly choose a date/time for an alarm-style reminder. At the user-selected time, the app can present a high-priority alarm experience with Stop/Snooze/reschedule controls so the reminder is not missed when the device is locked.

### Suggested Play Console explanation

> The app's core user-facing function includes user-configured alarm reminders. A user explicitly creates a task and selects the exact date and time of the reminder. Full-screen intent is used only for the alarm experience at that user-selected time, including stop, snooze, remind tomorrow, and reschedule actions. It is never used for advertising. If full-screen access is not granted, the app gracefully falls back to the normal high-priority notification experience and provides a user-controlled settings entry point.

### Important

- Never use full-screen intent to display or force interaction with ads.
- The alarm activity itself remains ad-free.
- On Android 14+, the app checks/links to the system special-access screen so the user controls this permission.
- If Play Console does not approve automatic enablement, use the user-granted path and keep the fallback notification behavior.

## Exact alarms (`SCHEDULE_EXACT_ALARM`)

Smart Tasks uses `SCHEDULE_EXACT_ALARM`, not the more restricted `USE_EXACT_ALARM` permission.

### Suggested explanation if requested

> Exact timing is used for user-created task alarms. The user explicitly chooses the reminder time. The app requests special exact-alarm access when needed and falls back to an inexact alarm/notification path when the system does not permit exact scheduling.

## Foreground service

The app declares a `mediaPlayback` foreground service used only while an alarm sound is actively playing. The service is not used for continuous background tracking or network activity.

## Ads separation

AdMob banners are confined to normal application screens. They are not displayed inside the urgent AlarmActivity and are not launched by full-screen notifications.
