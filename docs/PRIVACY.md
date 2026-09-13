# Privacy statement

Last reviewed: 2026-09-13 for version 1.0.3 (`versionCode 4`).

Ethic Tuner works locally on the device. Its manifest declares
`RECORD_AUDIO`, requested at runtime only when the person starts tuning.
Microphone samples are analysed in memory to estimate pitch and are discarded;
the app does not create audio recordings, transmit audio, use network access,
or integrate accounts, analytics or advertising.

The optional "Reducción de ruido del dispositivo" setting asks Android to use
its system noise-suppression effect for the same local microphone session when
that effect is available. It does not send audio to a cloud or external audio
service. Device makers may implement the effect differently, and it can be
disabled in the app settings if it makes a particular instrument harder to tune.

Instrument, notation, calibration, appearance and the noise-reduction
preference are retained in
the app's private local storage until changed, cleared in Android settings or
removed by uninstalling the app. Automatic Android backup is disabled by the
manifest. Device-to-device migration behaviour supplied by an operating system
or manufacturer is outside the app's control and is not a Tuner service.

The app is a tuning aid, not a safety, medical or professional calibration
instrument. Its output can be affected by microphone quality and environmental
noise. Users may revoke microphone access, clear local data or uninstall the
app through Android settings.
