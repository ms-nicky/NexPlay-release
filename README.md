# NexPlay

Custom GeForce NOW client for Android, built on the open-source [OpenNOW](https://github.com/OpenCloudGaming/OpenNOW) native Android client.

NexPlay is a community-built GeForce NOW client written natively in Kotlin and Jetpack Compose. It lets you browse the catalog, tune your stream, and launch cloud gaming sessions from your phone or Android TV.

## Features

- Native Kotlin / Jetpack Compose UI
- GeForce NOW auth, catalog, subscription, and session management
- WebRTC streaming with hardware MediaCodec decoding
- Stream tuning (resolution, FPS, codec, bitrate, region, HDR)
- Touch controller, mouse/keyboard input, microphone support
- In-stream diagnostics overlay and session reports
- APK self-update support (disabled in Play bundles)

## Build

Open `android/` as a project in Android Studio, or build from the command line:

```sh
cd android
# Debug APK (all ABIs)
./gradlew :app:assembleDebug
# Debug APK (32-bit ARM only)
./gradlew :app:assembleDebug -Pabi32Only=true
# Debug APK (32-bit + 64-bit ARM)
./gradlew :app:assembleDebug -Pabi32And64Only=true
```

Requires Android SDK with platform 37, NDK 28.2, and CMake 3.22.1.

## Disclaimer

Independent community project. Not affiliated with, endorsed by, or sponsored by NVIDIA. NVIDIA and GeForce NOW are trademarks of NVIDIA Corporation. You must use your own GeForce NOW account.

## License

MIT — based on OpenNOW (MIT). See [LICENSE](LICENSE).
