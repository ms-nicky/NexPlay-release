# NexPlay Native Android

This folder is a standalone Android Studio project for the native Kotlin / Jetpack Compose NexPlay target.

Open it from Android Studio with **File > Open > `NexPlay/android`**. Android Studio will install/use the required Gradle, Android SDK, CMake, and NDK components from the project configuration.

## Build Targets

- `:app:assembleDebug` builds a debug APK.
- `:app:assembleRelease` builds the direct-distribution release APK with APK update support.
- `:app:bundleRelease` builds the Google Play Android App Bundle. This task removes `REQUEST_INSTALL_PACKAGES` and disables APK self-updates so Play installs use Google Play's update mechanism.

Release and debug builds include `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86`. The `armeabi-v7a` and `x86` slices support 32-bit ARM phones, 32-bit Android TV firmware, and Intel TVs. NexPlay recommends 720p/30 FPS/12 Mbps for 32-bit processes and memory-constrained TVs, and warns when a custom profile exceeds that recommendation without overriding the user's selection.

## APK Update Manifest

Sideload and debug builds check the `update.json` at the repository root,
`https://raw.githubusercontent.com/ms-nicky/NexPlay-release/main/update.json`, and can download the
APK it points at. App Bundle builds installed from Google Play detect `com.android.vending` as the
install source and do not check, download, or install APK updates.

```json
{
  "android": {
    "versionCode": 159,
    "versionName": "2.0.3",
    "apkUrl": "https://github.com/ms-nicky/NexPlay-release/releases/download/v2.0.3/app-release.apk",
    "sha256": "64 lowercase hex characters covering the built APK",
    "releaseNotes": "Short notes shown in Settings\nSecond line"
  }
}
```

`apkUrl`, `artifactUrl`, or `url` may point at the APK, flat or nested under `android`. `releaseNotes`
may use real newlines or literal `\n` separators.

**`sha256` is required.** The app refuses to install a downloaded APK unless its bytes match the
digest the manifest published, and re-checks the digest at install time so a file swapped after
download is caught. A manifest with a missing, truncated, or mismatched digest fails the update
rather than installing unverified bytes.

## Telemetry (optional, opt-in)

NexPlay ships no analytics by default. There is deliberately no project token committed to the
repository — a token in a public repo is an active, writable ingest credential. To build with
analytics, supply your own PostHog project token:

```sh
./gradlew :app:assembleRelease -Pposthog.projectToken=phc_... -Pposthog.host=https://...
# or POSTHOG_PROJECT_TOKEN / POSTHOG_HOST environment variables
# or posthog.projectToken / posthog.host in android/local.properties (gitignored)
```

Without a token the SDK is inert and performs no network work. With one, telemetry still only runs
after the user answers the consent prompt, and every payload passes a sanitizer that drops
credential-shaped properties and redacts free text before it leaves the device. Server-side
geolocation is disabled on each event. Session replay, screenshots, and logcat capture are off.

## YouTube Live rebroadcast (optional)

An RTMP rebroadcast of the device screen, driven by a `mediaProjection` foreground service. Requires
`FOREGROUND_SERVICE_MEDIA_PROJECTION`, explicit screen-capture consent per broadcast, and a stream key
configured in Settings.

RTMP is not encrypted, so treat the stream key as a live-broadcast credential: it is never logged, is
stripped out of any RTMP URL that reaches a log or diagnostic export, and is excluded from analytics
payloads. The ingest host stays visible in logs so broadcast failures remain diagnosable.

## Runtime Notes

- UI is native Compose.
- GFN auth, catalog, subscription, CloudMatch session creation/polling/claim/stop, signaling, and input packet behavior are implemented in Kotlin from the Electron project contracts.
- Streaming uses Android WebRTC plus hardware MediaCodec probing. The bundled `nexplay_native` JNI library exposes native runtime diagnostics and keeps the NDK/CMake path wired for media-sensitive code.
- Queue ad metadata is preserved in `SessionInfo`; ad playback can use the included Media3 dependency when the server returns an ad media URL.
- HDR output, screen recording, gyroscope input, touch-controller skins and presets, and settings backup are all present; see the Settings panels.
- Release builds trust only system certificate roots. User-installed CAs are honoured in debug builds only, so a proxy certificate on the device cannot intercept the GFN session in a shipped build.

## Experimental NVST

Settings > Advanced > Experimental streaming > NVST transport selects the native transport
ported from dev commit `3002e5c67`. NVST defaults off for all distributions. On the first 1.6.4
launch, a persisted migration also switches off the previous automatic APK opt-in. Users can
enable the experiment again afterward; that choice survives subsequent launches and upgrades.
The choice applies when attaching a stream; changing settings does not replace a running transport.

The NVST runtime performs RTSP-over-WebSocket negotiation, authenticated SRTP UDP video with
FEC/NACK recovery, Opus audio, and native SCTP keyboard/mouse/gamepad input. Android keeps its
hardware decoder and renderer. Input and encoded-media queues are bounded. Reconnects retain the
cloud session; only the existing explicit End Session action owns cloud-session termination.

Microphone upstream uses 48 kHz Opus with 20 ms frames and RED redundancy on the encrypted
bundle. Capture follows microphone permission, foreground-service, mute, and attachment lifecycle;
muting releases the recorder and clears redundant speech on resume. Native game touch uses reliable
control messages, and received rumble uses the existing vibration switch and controller/device routing.
On-screen gamepad and touch mouse continue to use ordinary gamepad/mouse packets. A previously allocated WebRTC session might not provide an NVST endpoint;
the app reports this and retains that session rather than terminating or replacing it.

Building now requires Python 3, CMake, Cargo/Rust (via rustup recommended), and Android Rust targets:

```sh
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
./gradlew :app:assembleDebug
```

Gradle builds the pinned Rust sources for all four packaged ABIs using its NDK, including statically linked Opus, with 16 KB ELF
page alignment. No precompiled NVST binaries are downloaded. The initial build downloads crates;
later builds reuse Cargo and Gradle outputs. `CARGO` can select an alternative Cargo executable.

Protocol validation: `cargo test --manifest-path nvst/Cargo.toml --workspace`.
Live GFN playback, touch/controller hardware, HDR output, and network-loss recovery still need
physical-device verification. Source provenance and Android-specific differences: `nvst/UPSTREAM.md`.
