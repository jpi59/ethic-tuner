# Privacy statement

Last reviewed: 2026-09-14 for version 1.0.28 (`versionCode 29`).

Ethic Tuner works locally on the device. Its manifest declares
`RECORD_AUDIO`, requested at runtime only when the person starts tuning. On
Android 13 and later, `POST_NOTIFICATIONS` is requested separately and is
optional: it lets the app display its persistent foreground-service indicator
in the notification drawer while tuning continues through a temporary screen
change. Declining it does not cause audio to be sent elsewhere or grant any
additional access; Android still exposes foreground-service notices in its
Task Manager.
Microphone samples are analysed in memory to estimate pitch and are discarded;
the app does not create audio recordings, transmit audio, use network access,
or integrate accounts, analytics or advertising.

The optional "Reducción de ruido del dispositivo" setting asks Android to use
its system noise-suppression effect for the same local microphone session when
that effect is available. It does not send audio to a cloud or external audio
service. Device makers may implement the effect differently, and it can be
disabled in the app settings if it makes a particular instrument harder to tune.

The optional confirmation sound is generated locally by Android when a stable
reading enters the in-tune window. It is not an audio recording, does not use
an external sound asset, and does not send or retain audio.

When the person starts a session, Ethic Tuner runs a visible Android foreground
service with microphone type. Its persistent indicator says that the microphone
is active and points to the in-app Detener control. The service is not restarted
automatically after the task is removed.

Instrument, notation, calibration, appearance and the noise-reduction
preference, along with whether the optional local confirmation beep is enabled,
whether haptic confirmation is enabled, and whether the Hz reference is shown,
are retained in
the app's private local storage until changed, cleared in Android settings or
removed by uninstalling the app. Automatic Android backup is disabled by the
manifest. Device-to-device migration behaviour supplied by an operating system
or manufacturer is outside the app's control and is not a Tuner service.

The app is a tuning aid, not a safety, medical or professional calibration
instrument. Its output can be affected by microphone quality and environmental
noise. Users may revoke microphone access, clear local data or uninstall the
app through Android settings.
