# NexPlay Release

Repositori untuk rilis APK NexPlay dan manifes update.

- `update.json` — sumber info update yang dicek aplikasi.
- Rilis APK di tab **Releases**.

## Cara merilis versi baru

1. Buat tag & release baru di repo ini, upload file `NexPlay-<versi>-release.apk`.
2. Update `update.json`:
   ```json
   {
     "android": {
       "apkUrl": "https://github.com/ms-nicky/NexPlay-release/releases/download/v<versi>/NexPlay-<versi>-release.apk",
       "versionName": "<versi>",
       "versionCode": <kode>,
       "releaseNotes": "Catatan perubahan"
     }
   }
   ```
3. Commit & push ke `main`. Aplikasi akan mendeteksi update otomatis.
