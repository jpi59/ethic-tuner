# Ethic Tuner

Ethic Tuner is a local, privacy-focused chromatic tuner for Android. It
analyses microphone samples in memory and shows pitch, frequency and deviation
without recording or transmitting audio.

## Features

- Chromatic tuning plus instrument presets for guitar, bass, ukulele, violin,
  piano, recorder, transverse flute, clarinet and saxophones.
- English or Latin note names.
- A4 calibration from 424–456 Hz.
- Optional device-provided noise reduction, clearly labelled and controllable in settings when the device supports it.
- A centred green inverse needle and the explicit “ESTÁ AFINADO” state for an
  in-tune reading. The last needle remains visible in grey if the input drops,
  plus optional short local sound and haptic confirmation.
- Optional Hz reference view: measured frequency alongside the mathematically
  calculated equal-temperament target for the selected A4 calibration.
- Light and dark themes, portrait and landscape layouts.
- No network permission, accounts, analytics, advertising or scan history.

## Privacy

`RECORD_AUDIO` is requested when tuning starts. On Android 13 and later, the
app separately asks for the optional notification permission so that an active
microphone session remains visibly indicated if the app is temporarily left.
Samples are analysed in memory and discarded; the app does not create audio
files or send audio to a service. When enabled and available, the optional
noise-reduction effect is supplied by Android on the same local audio session;
it is not a cloud or third-party audio service and its results vary by device.
Appearance, instrument, notation, A4, noise-reduction, confirmation sound,
haptic confirmation and Hz-reference preferences are stored locally by Android.
The app does not claim to control what the operating system or other
applications may do with device data.

The detailed [privacy statement](docs/PRIVACY.md) records the data flow,
permission boundary and user controls for the published release.

## Build from source

This repository includes its Gradle wrapper and is self-contained.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease :app:lintVitalRelease
```

Release signing is intentionally opt-in and local. Copy
[`signing.properties.example`](signing.properties.example) to
`keystore.properties`, fill it with paths and credentials that remain under the
maintainer's control, then run `./gradlew :app:assembleRelease`. The example
file contains no secret and `keystore.properties` and key material are ignored
by Git. Do not publish an unsigned APK. Verify a signed APK with
`apksigner verify --verbose --print-certs <apk>` before attaching it to a
release.

F-Droid builds and signs its own artifacts from the published source and
metadata, so a future F-Droid APK will use a different certificate from a
directly distributed APK. `local.properties` is also machine-specific and must
not be committed. See [the release handover](docs/RELEASING.md) for the exact
maintainer sequence.

## License and attribution

Ethic Tuner is released under GPL-3.0-or-later. See [COPYING](COPYING) and
[NOTICE.md](NOTICE.md). The source, icon and interface in this repository are
original to this project; the notice records related work consulted during
development without claiming copied code or assets.

## F-Droid status

The source carries localised catalogue metadata, a current application
screenshot and an F-Droid build-recipe draft. Inclusion is not automatic: an
F-Droid maintainer must review the source, exact build recipe, licensing,
metadata and reproducibility in the F-Droid Data repository. No F-Droid
publication is claimed until that review and publication occur.
