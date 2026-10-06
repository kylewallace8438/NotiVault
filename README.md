# NotiVault

A personal Android app that saves notifications from the apps you choose (your banking apps) into an on-phone database and pulls out the amount, direction (in/out) and balance.

Native Kotlin + Jetpack Compose + Room. Min Android 8.0, built for Android 15 (targetSdk 35).

## Build and install

1. Open the `NotiVault` folder in Android Studio (File > Open). Let Gradle sync; Studio downloads Gradle and the libraries. If it offers to upgrade the Android Gradle Plugin, accepting is fine.
2. On the Xiaomi: Settings > About phone > tap "OS version" 7 times to enable Developer options. Then in Settings > Additional settings > Developer options turn on **USB debugging**, **Install via USB** and **USB debugging (Security settings)**. Xiaomi asks you to sign in to a Mi account for the last two.
3. Connect the phone and press Run.

Installing from Android Studio normally avoids the "restricted settings" block. If you later install the APK from a file manager instead and the access switch is greyed out, use App info > ⋮ > Allow restricted settings.

## First run

1. Tap **Grant access** and switch NotiVault on in Notification access.
2. Tap **Apps** and tick your banking apps.
3. Settings > **Xiaomi / HyperOS checklist**: Autostart on, Battery saver "No restrictions", lock the app in Recents.
4. Settings > **Send test notification**. A green or red amount on the home list means everything works.

## Where the data is

Room (SQLite) database, private to the app: `/data/data/com.notivault/databases/notivault.db`.

- Browse and run SQL on it live: Android Studio > View > Tool Windows > **App Inspection** > Database Inspector.
- Settings > Data exports **JSON**, **CSV** (opens in Excel) or the raw **SQLite .db** (opens in DB Browser for SQLite).

### Table `notifications`

| Group | Columns |
|---|---|
| Source | `id`, `packageName`, `appLabel`, `postedAt`, `receivedAt`, `channelId`, `category`, `notificationKey`, `notificationId`, `tag` |
| Content | `title`, `text`, `bigText`, `subText`, `summaryText`, `infoText`, `textLines`, `messages`, `tickerText` |
| For regex | **`fullText`**: all the text fields above combined, duplicates removed |
| Everything | **`rawExtras`**: the whole notification extras bundle as JSON, so nothing is lost |
| Parsed | `amount`, `currency`, `direction` (IN/OUT), `counterparty`, `balance`, `reference`, `parserId`, `parsedAt` |

Times are epoch milliseconds. Parsed columns stay empty until a parser matches.

## Adding your own regex (per bank)

A generic parser (`parsing/GenericAmountParser.kt`) already handles common formats like `+1,250,000 VND`, `-50.000đ`, `USD 12.50`, `Balance: …`, `Số dư …`, `SD: …`. It doesn't extract the counterparty, because that's different for every bank.

To add a bank:

1. Let the app collect a few real notifications from it.
2. Open one, long-press the **Full text** to copy it, and note the **Package** shown under Fields.
3. Copy `parsing/TemplateBankParser.kt` to e.g. `MyBankParser.kt`. Set the package name and write regexes against that text (regex101.com with the "Java 8" flavor is handy).
4. Add it to `parsing/ParserRegistry.kt` **above** `GenericAmountParser`.
5. Reinstall, then Settings > **Re-run parsers** to apply it to everything already stored.

Raw data is never changed by parsing, so you can rewrite parsers as often as you like.

## Adding database columns later

Add the field to `NotificationEntity`, bump `version` in `AppDatabase`, and add `autoMigrations = [AutoMigration(from = 1, to = 2)]`. Room's schema files are written to `app/schemas/`; keep them. Don't use `fallbackToDestructiveMigration()`, it deletes your history.

## Code map

```
service/BankNotificationListener.kt  receives notifications, filters to watched apps
service/NotificationMapper.kt        notification -> row (text fields, fullText, raw JSON, dedup)
service/Recorder.kt                  map -> parse -> save; re-run parsers
parsing/                             parsers (add yours here)
data/                                Room entity, DAO, database, watched-apps store, export
ui/                                  Compose screens: Home, Detail, App picker, Settings
```

## Limits

- Only notifications posted while access is on are captured. There's no way to read older history.
- You only get what the bank puts in the notification. If a bank says "You have a new transaction", that's all there is.
- Android 15 can hide the text of notifications it thinks contain one-time codes from listener apps.
- Ongoing notifications and group summaries are skipped (see the flags at the top of `NotificationMapper.kt`).
- Data isn't included in Android cloud backup (`allowBackup=false`). Export if you change phones.
