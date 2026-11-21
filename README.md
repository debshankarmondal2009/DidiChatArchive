# Chat Archive (Android, Kotlin + Jetpack Compose)

Offline, read-only reconstruction of one WhatsApp conversation, shown the way it would look on **Didi's** phone.

* Didi (`Didi 🤔`) = device owner → RIGHT, pink bubbles
* Debshankar (`𝔻ᴇʙsʜᴀɴᴋᴀʀ 𝕄ᴏɴᴅᴀʟ`) → LEFT, white bubbles; header "Debshankar Mondal"
* "You deleted this message" entries are removed completely.
* Original order, timestamps, multi-line text, Bengali/English/emoji, AI auto-replies, contact cards, locations, polls kept as exported.
* "You ..." system notices (written from Debshankar's phone in the export) are reworded to "Debshankar ...".
* `<Media omitted>` is shown honestly as "Media omitted" (the ZIP contains no media files).
* No INTERNET permission, no backup, data lives in app-private storage (`files/chat/messages.jsonl` + `meta.json`).

## Build the APK
1. Open this folder in Android Studio (Ladybug or newer, JDK 17) and let Gradle sync, or on the command line: `./gradlew assembleDebug`
2. APK: `app/build/outputs/apk/debug/app-debug.apk` (`assembleRelease` also works; it is debug-signed so it installs directly).
3. Install, tap **Choose export file**, pick `WhatsApp_Chat_with_Didi_.zip`, check the preview, tap **Looks right**.

## Verify the import rules without Android
`python3 -I tools/reference_import.py "WhatsApp Chat with Didi 🤔.txt"` prints the same counts the app's preview shows
(expected: 87,199 entries, 299 deleted removed, 86,900 kept = Didi 39,971 right, Debshankar 46,914 left, 15 notices).
