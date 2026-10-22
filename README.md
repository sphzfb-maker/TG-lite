# TG Lite (com.phsuvo.tg)

Minimal Telegram client on top of TDLib. Plain Android views, no AppCompat/Material/Kotlin, armeabi-v7a (32-bit ARM) only, works on 32-bit and most 64-bit phones.

Stage 1: login (phone, code, 2FA), chat list, open chat, send/receive text.

## Build on GitHub
1. Upload everything in this folder to a new GitHub repo (branch `main`). Make sure `.github/workflows/build.yml` is included.
2. Repo > Settings > Secrets and variables > Actions > New repository secret:
   - `TG_API_ID`   = your api_id (numbers only)
   - `TG_API_HASH` = your api_hash
3. Actions tab > "Build APK" > Run workflow (or just push). First run builds TDLib (30-40 min), later runs are fast.
4. When done, open the run and download the `tg-lite-apk` artifact. Unzip it and install the APK.

App name: edit `app/src/main/res/values/strings.xml`.
