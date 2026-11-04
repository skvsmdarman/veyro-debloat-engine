# ⚡ Veyro Debloat Engine

**Lag Destroyer • Rootless ADB Engine** — an ultra-lightweight (≈1.3 MB) Android debloat &
performance suite built with Kotlin, Jetpack Compose (Material 3) and the official
[Shizuku](https://github.com/RikkaApps/Shizuku) API.

> Made by ⚡ **Nova Devs** — join us on Telegram: [t.me/xcid_Kagenou](https://t.me/xcid_Kagenou)

## ⬇️ Download

Grab the latest APK from [**Releases**](https://github.com/skvsmdarman/veyro-debloat-engine/releases).
The app **updates itself**: on every launch it checks [`update.json`](update.json) on this repo,
shows an in-app banner + heads-up notification when a new release exists, downloads it via the
system DownloadManager, and installs it — silently through Shizuku (`pm install -r`) when
available, or through the system installer.

## ✨ Features

| Area | What you get |
|---|---|
| **Debloat** | Three non-destructive strategies — 🧊 Freeze (`pm suspend`), ⏸ Disable (`pm disable-user`), 🗑 Uninstall (`pm uninstall -k --user 0`) — with per-batch action chips, cache clearing, APK backup vault (`/sdcard/Veyro/Backups/`), wake-lock cutter (AppOps) and a full RemovalLog-driven **Reversible Restore Engine** |
| **Modes** | 🌱 Eco (battery saver + 0.5x animations) · ⚡ Performance (multitasking) · 🎮 Gaming (RAM clearing + animations off + Game Focus launch) with **Game Overdrive** (720p FPS booster, competitive touch latency) |
| **Dashboard** | Spring-animated RAM/Storage gauges, flash-throttle alerts, Storage Unchoker (`pm trim-caches 1000G`), **OS-Wide AdBlock DNS** (AdGuard/Cloudflare Private DNS), **Display Smoother** (60/90/120Hz locks), System Animation Tweaker with live preview |
| **Tools Lab** | AOT Speed Compiler, Battery Analyzer (watts/temperature/cycles), RAM Expansion auditor + adjuster, Thermal throttle detector, Sensor privacy kill-switch, Log buffer flush/shrink, RRO overlay purger |
| **Safety** | Strict safety locks (dialer, SMS, Wi-Fi, BT, camera, keyboard, launcher, GMS, Play Store, OEM account centers can never be removed) · Shizuku-less fallbacks · guided 4-step Shizuku tutorial |
| **Motion** | 19 bundled Lottie animations (UseAnimations + custom Nova Devs artwork), branded launch splash, animated 4-tab navigation |

## 🔄 Releasing a new version

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`
2. Tag and push: `git tag v1.0.1 && git push origin v1.0.1` — the GitHub Action builds the APK
   and attaches `veyro-debloat-engine.apk` to the release automatically (or upload your APK manually)
3. Edit **[`update.json`](update.json)** in `main` to point at the new release URL:

```json
{
  "versionCode": 2,
  "versionName": "1.0.1",
  "releaseNotes": "• bullet notes",
  "downloadUrl": "https://github.com/skvsmdarman/veyro-debloat-engine/releases/download/v1.0.1/veyro-debloat-engine.apk",
  "mandatory": false
}
```

Every installed app compares that `versionCode` against its own and offers the update in-app.

## 🛠 Building from source

```bash
./gradlew assembleRelease     # R8 full-mode, ~1.3 MB
./gradlew assembleDebug       # for quick adb testing
```

## 📄 License

[MIT](LICENSE) — © 2026 Nova Devs

## 🙏 Credits

- Animated icons: [UseAnimations](https://useanimations.com) (MIT) + custom Nova Devs Lottie artwork
- [Lottie Android](https://github.com/airbnb/lottie-android) runtime
- [Shizuku](https://github.com/RikkaApps/Shizuku) — rootless shell access
