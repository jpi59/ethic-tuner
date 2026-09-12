# Ethic Tuner

Ethic Tuner is a local, privacy-focused chromatic tuner for Android. It
analyses microphone samples in memory and shows pitch, frequency and deviation
without recording or transmitting audio.

## Features

- Chromatic tuning plus instrument presets for guitar, bass, ukulele, violin,
  piano, recorder, transverse flute, clarinet and saxophones.
- English or Latin note names.
- A4 calibration from 424–456 Hz.
- Light and dark themes, portrait and landscape layouts.
- No network permission, accounts, analytics, advertising or scan history.

## Privacy

The only runtime permission is `RECORD_AUDIO`, requested when tuning starts.
Samples are analysed in memory and discarded; the app does not create audio
files or send audio to a service. Appearance, instrument, notation and A4
preferences are stored locally by Android. The app does not claim to control
what the operating system or other applications may do with device data.

The detailed [privacy statement](docs/PRIVACY.md) records the data flow,
permission boundary and user controls for the published release.

## Build from source

This repository includes its Gradle wrapper and is self-contained.

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
./gradlew :app:assembleRelease :app:lintVitalRelease
```

Version `1.0.0` has a [production-signed APK and SHA-256 checksum on its GitHub
release](https://github.com/jpi59/ethic-tuner/releases/tag/v1.0.0). F-Droid
builds and signs its own artifacts from the published source and metadata, so a
future F-Droid APK will use a different signature. `local.properties` is
machine-specific and must not be committed.

## License and attribution

Ethic Tuner is released under GPL-3.0-or-later. See [COPYING](COPYING) and
[NOTICE.md](NOTICE.md). The source, icon and interface in this repository are
original to this project; the notice records related work consulted during
development without claiming copied code or assets.

## F-Droid status

The source and signed direct release are public, but F-Droid inclusion is not
automatic. A maintainer must review the source, build recipe, licensing,
metadata and reproducibility in the F-Droid data repository. No F-Droid
publication is claimed until that review occurs.
