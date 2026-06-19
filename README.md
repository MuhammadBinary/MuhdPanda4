# MuhdPanda4

MuhdPanda4 is a simple Android app that chats through OpenRouter using the free router model (`openrouter/free`).

## What this app does

- Lets you paste your own OpenRouter API key on your phone.
- Uses `openrouter/free` by default.
- Sends chat messages to OpenRouter.
- Saves your API key locally on your phone with Android SharedPreferences.
- Includes a GitHub Actions workflow that builds a downloadable debug APK.

## Get an OpenRouter key

1. Go to <https://openrouter.ai>.
2. Sign in.
3. Create an API key.
4. Keep the model field as `openrouter/free` in the app.

## Build locally

```bash
gradle assembleDebug
```

The APK will be created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build APK with GitHub Actions

1. Push this repo to GitHub. The workflow installs Gradle and the Android SDK automatically.
2. Open the repo on GitHub.
3. Click **Actions**.
4. Run **Build Android APK**.
5. Download the `muhdpanda4-debug-apk` artifact.
6. Send the APK to your phone and install it.

## Install on phone

1. Download the APK from GitHub Actions.
2. Send it to your phone using USB, Google Drive, Telegram, LocalSend, or email.
3. Open the APK on your phone.
4. If Android blocks it, allow "Install unknown apps" for your browser or file manager.
5. Open MuhdPanda4.
6. Paste your OpenRouter API key.
7. Tap **Send with OpenRouter/free**.

## Important note

This is a safe starter version. It does not control your phone yet. It only chats with OpenRouter. Phone-control features require Accessibility Service permissions and should be added carefully.
