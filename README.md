# MuhdPanda4

MuhdPanda4 is an Android phone-control assistant inspired by Panda/Blurr. It uses OpenRouter's free router model (`openrouter/free`) by default and an Android Accessibility Service to read and interact with the current screen when you ask it to.

## What this app does

- Lets you paste your own OpenRouter API key on your phone.
- Uses `openrouter/free` by default.
- Can chat normally with OpenRouter.
- Can control the phone through Android Accessibility after you enable **MuhdPanda4 Controller**.
- Reads visible screen text/content descriptions and sends that screen context to OpenRouter for an action plan.
- Supports these phone actions: open app, tap visible text, type text into fields, back, home, recents, scroll up/down, coordinate tap, and wait.
- Saves your API key locally on your phone with Android SharedPreferences.
- Includes a GitHub Actions workflow that builds a downloadable debug APK.

## Important safety note

Accessibility permission is powerful. It lets the app read visible screen content and perform taps/actions when you ask it to. Only install builds from your own repo and only paste API keys you control.

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
7. Keep the model as `openrouter/free`.
8. Tap **Enable phone control permission**.
9. In Android Accessibility settings, enable **MuhdPanda4 Controller**.
10. Return to MuhdPanda4.
11. Type a command and tap **Control phone with OpenRouter/free**.

## Example commands

Try commands like:

- `Open Settings`
- `Open Chrome`
- `Tap Search`
- `Type hello world`
- `Scroll down`
- `Go back`
- `Go home`
- `Open YouTube and tap Search`

## Current limitations

This is now a real Accessibility-based controller, but it is still a starter implementation. It does not yet use screenshots or vision. It works best when the target buttons/text are visible to Android Accessibility. Some apps hide content from Accessibility, and some actions may need to be retried with a simpler command.
