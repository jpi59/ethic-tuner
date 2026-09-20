/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioAttributes;
import android.media.AudioTrack;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/** A local chromatic tuner. Audio samples are analysed in memory and discarded. */
public final class MainActivity extends Activity {
    private static final int REQUEST_MICROPHONE = 10;
    private static final int REQUEST_NOTIFICATIONS = 11;
    private static final long HAPTIC_RELEASE_GRACE_MS = 450;
    private static final String[] INSTRUMENTS = {"Cromático", "Guitarra", "Bajo", "Ukelele", "Violín", "Teclado / Piano", "Flauta dulce", "Flauta travesera", "Clarinete en Si♭", "Saxofón sopranino (Mi♭)", "Saxofón soprano (Si♭)", "Saxofón alto (Mi♭)", "Saxofón tenor (Si♭)", "Saxofón barítono (Mi♭)", "Saxofón bajo (Si♭)", "Saxofón contrabajo (Mi♭)"};
    private static final String[] INSTRUMENT_GUIDES_ENGLISH = {"Cualquier nota", "Afinación estándar: E A D G B E · ignora graves por debajo de 60 Hz", "Afinación estándar: E A D G", "Afinación estándar: G C E A", "Afinación estándar: G D A E", "Toca una tecla; usa A4 para calibrar", "Nota de concierto", "Nota de concierto", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭"};
    private static final String[] INSTRUMENT_GUIDES_LATIN = {"Cualquier nota", "Afinación estándar: Mi La Re Sol Si Mi · ignora graves por debajo de 60 Hz", "Afinación estándar: Mi La Re Sol", "Afinación estándar: Sol Do Mi La", "Afinación estándar: Sol Re La Mi", "Toca una tecla; usa A4 para calibrar", "Nota de concierto", "Nota de concierto", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭"};
    private static final int[] NOTE_TRANSPOSITIONS = {0, 0, 0, 0, 0, 0, 0, 0, 2, 9, 2, 9, 2, 9, 2, 9};
    private static final String[] NOTATIONS = {"Notación inglesa · C D E", "Notación latina · Do Re Mi"};
    private static final String[] ENGLISH_NOTES = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
    private static final String[] LATIN_NOTES = {"Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si"};
    private TextView status, calibration, instrumentGuide, tuningState;
    private PrecisionDialView precisionDial;
    private Button toggle;
    private LinearLayout root;
    private LinearLayout tunerPanel;
    private TextView title, privacy;
    private Button legal;
    private ImageButton settings, themeToggle;
    private Spinner instruments, notationSpinner;
    private ArrayAdapter<String> instrumentAdapter, notationAdapter;
    private AlertDialog settingsDialog;
    private LinearLayout settingsPanel;
    private TextView instrumentLabel, notationLabel, noiseReductionHint, inTuneSoundHint, referenceFrequencyHint, hapticFeedbackHint;
    private Switch noiseReductionSwitch, inTuneSoundSwitch, referenceFrequencySwitch, hapticFeedbackSwitch;
    private Button inTuneSoundTest, hapticFeedbackTest;
    private SharedPreferences preferences;
    private boolean darkMode, hasPitch, inTune, acceptingNotationChanges, acceptingInstrumentChanges;
    private volatile boolean noiseReduction, noiseReductionEnabled, inTuneSound, hapticFeedback, referenceFrequencyVisible, signalMissing = true;
    private int lastDeviation;
    private int selectedInstrument, noteTransposition, notation;
    private double lastHz, lastConfidence;
    private volatile boolean running;
    private volatile long captureGeneration;
    private volatile AudioRecord activeRecorder;
    private volatile NoiseSuppressor activeNoiseSuppressor;
    private final Object captureLock = new Object();
    private PitchTracker.State measurementState = PitchTracker.State.NONE;
    private int displayedMidi = Integer.MIN_VALUE;
    private int pendingMidi = Integer.MIN_VALUE;
    private int pendingMidiFrames;
    private int a4 = 440;
    private String lastNoteName = "—";
    private final InTuneFeedbackGate inTuneFeedbackGate = new InTuneFeedbackGate();
    private AudioTrack inTuneTone;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean hapticRunning;
    private final Runnable inTuneVibrationPulse = new Runnable() {
        @Override public void run() {
            if (!hapticRunning || !hapticFeedback || !running) { stopInTuneVibration(); return; }
            Vibrator vibrator = systemVibrator();
            if (vibrator == null || !vibrator.hasVibrator()) { stopInTuneVibration(); return; }
            // A finite effect can be restarted reliably if Android interrupts it for another
            // haptic or audio effect. Requested amplitude is maximum; the device may scale it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrateFeedback(vibrator, VibrationEffect.createWaveform(new long[]{0, 180, 80, 180}, new int[]{0, 255, 0, 255}, -1));
            else vibrator.vibrate(new long[]{0, 180, 80, 180}, -1);
            mainHandler.postDelayed(this, 600);
        }
    };
    private final Runnable hapticRelease = this::stopInTuneVibration;
    private static volatile WeakReference<MainActivity> activeInstance = new WeakReference<>(null);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        activeInstance = new WeakReference<>(this);
        preferences = getSharedPreferences("appearance", MODE_PRIVATE);
        darkMode = preferences.getBoolean("dark_mode", false); noiseReduction = preferences.getBoolean("noise_reduction", true); inTuneSound = preferences.getBoolean("in_tune_sound", true); hapticFeedback = preferences.getBoolean("haptic_feedback", false); referenceFrequencyVisible = preferences.getBoolean("reference_frequency_visible", false); a4 = preferences.getInt("a4", 440); selectedInstrument = Math.max(0, Math.min(INSTRUMENTS.length - 1, preferences.getInt("instrument", 0))); noteTransposition = NOTE_TRANSPOSITIONS[selectedInstrument]; notation = Math.max(0, Math.min(NOTATIONS.length - 1, preferences.getInt("notation", 0)));
        buildUi();
    }
    private void buildUi() {
        Configuration configuration = getResources().getConfiguration();
        boolean landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
        int widthDp = configuration.screenWidthDp;
        boolean largeScreen = configuration.smallestScreenWidthDp >= 600;
        // A narrow landscape phone benefits from two compact columns. A portrait tablet does
        // not split until it has enough width for a readable tuner and control column.
        boolean splitLayout = landscape || widthDp >= 840;
        int horizontalPaddingDp = splitLayout ? (largeScreen ? 24 : 16) : Math.max(20, (widthDp - 680) / 2);
        int horizontalPadding = dp(horizontalPaddingDp);
        int topPadding = dp(landscape ? 12 : 20);
        int bottomPadding = dp(landscape ? 12 : 16);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(splitLayout && largeScreen ? Gravity.CENTER_VERTICAL : Gravity.CENTER_HORIZONTAL); root.setPadding(horizontalPadding, topPadding, horizontalPadding, bottomPadding);
        LinearLayout header = column();
        header.setPadding(0, dp(landscape ? 16 : 24), 0, 0);
        tunerPanel = column();
        LinearLayout controls = column();
        title = text("Ethic Tuner", landscape ? 22 : 25); medium(title);
        FrameLayout headerControls = new FrameLayout(this);
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER); headerControls.addView(title, titleParams);
        settings = new ImageButton(this); settings.setScaleType(ImageView.ScaleType.CENTER); settings.setPadding(0, 0, 0, 0); settings.setMinimumWidth(0); settings.setMinimumHeight(dp(52)); settings.setContentDescription(getString(R.string.settings)); settings.setOnClickListener(v -> showSettings()); FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(56), dp(52), Gravity.START | Gravity.CENTER_VERTICAL); headerControls.addView(settings, settingsParams);
        themeToggle = new ImageButton(this); themeToggle.setScaleType(ImageView.ScaleType.CENTER); themeToggle.setPadding(0, 0, 0, 0); themeToggle.setMinimumWidth(0); themeToggle.setMinimumHeight(dp(52)); themeToggle.setOnClickListener(v -> { darkMode = !darkMode; preferences.edit().putBoolean("dark_mode", darkMode).apply(); applyTheme(); }); FrameLayout.LayoutParams themeParams = new FrameLayout.LayoutParams(dp(56), dp(52), Gravity.END | Gravity.CENTER_VERTICAL); headerControls.addView(themeToggle, themeParams);
        header.addView(headerControls, new LinearLayout.LayoutParams(-1, dp(52)));
        status = text(getString(R.string.waiting), 13); status.setVisibility(View.GONE); header.addView(status, new LinearLayout.LayoutParams(-1, -2));
        tunerPanel.setPadding(0, dp(landscape ? 6 : 8), 0, dp(landscape ? 4 : 6));
        precisionDial = new PrecisionDialView(this); precisionDial.setReferenceFrequencyVisible(referenceFrequencyVisible); tunerPanel.addView(precisionDial, new LinearLayout.LayoutParams(-1, dp(dialHeight(splitLayout, largeScreen))));
        tuningState = text(getString(R.string.pitch_waiting), 18); medium(tuningState); tunerPanel.addView(tuningState);
        toggle = new Button(this); toggle.setText(R.string.start); toggle.setTextSize(18); toggle.setAllCaps(false); medium(toggle); toggle.setOnClickListener(v -> requestOrToggle());
        LinearLayout.LayoutParams primaryAction = new LinearLayout.LayoutParams(-1, dp(52)); primaryAction.setMargins(0, dp(8), 0, dp(2)); controls.addView(toggle, primaryAction);
        legal = new Button(this); legal.setText(R.string.license_action); legal.setTextSize(15); legal.setAllCaps(false); medium(legal); legal.setGravity(Gravity.CENTER); legal.setMinHeight(dp(48)); legal.setPadding(dp(12), dp(8), dp(12), dp(8)); legal.setBackgroundColor(Color.TRANSPARENT); legal.setContentDescription(getString(R.string.license_action)); legal.setOnClickListener(v -> showLegalNotice()); controls.addView(legal);
        privacy = text(getString(R.string.privacy), 13); privacy.setGravity(Gravity.CENTER); privacy.setPadding(0, dp(landscape ? 6 : 10), 0, 0);
        if (splitLayout) {
            LinearLayout leftColumn = column(); leftColumn.addView(header); leftColumn.addView(controls); leftColumn.addView(privacy);
            root.setOrientation(LinearLayout.HORIZONTAL); root.addView(leftColumn, weighted()); root.addView(tunerPanel, weighted());
        } else { root.addView(header); root.addView(tunerPanel); root.addView(controls); root.addView(privacy); }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            // Android can place its navigation bar on the right in landscape. Keep all tuner
            // content inside that safe area, while retaining the full usable height.
            root.setPadding(horizontalPadding + insets.getSystemWindowInsetLeft(), topPadding,
                    horizontalPadding + insets.getSystemWindowInsetRight(), bottomPadding);
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
        applyTheme();
        if (running) { showListeningStatus(); toggle.setText(R.string.stop); precisionDial.setReading(lastNoteName, lastHz, lastDeviation, measurementState); if (signalMissing) precisionDial.setSignalMissing(); }
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setGravity(Gravity.CENTER_HORIZONTAL); return layout; }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1f); }
    /**
     * Keep the meter dominant without turning it into an empty billboard on a large display.
     * The visual is allowed to grow to a useful upper bound; additional space becomes calm
     * surrounding space instead of stretching controls or typography beyond readability.
     */
    private int dialHeight(boolean splitLayout, boolean largeScreen) {
        float density = getResources().getDisplayMetrics().density;
        int screenHeightDp = Math.round(getResources().getDisplayMetrics().heightPixels / density);
        int availableForDial = screenHeightDp - (splitLayout ? 70 : 324);
        int minimum = splitLayout ? 270 : 350;
        int maximum = splitLayout ? (largeScreen ? 560 : 320) : (largeScreen ? 540 : 560);
        return Math.max(minimum, Math.min(maximum, availableForDial));
    }
    private ArrayAdapter<String> themedAdapter(String[] values) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) { return colourRow(super.getView(position, convertView, parent)); }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) { return colourRow(super.getDropDownView(position, convertView, parent)); }
            private View colourRow(View view) {
                if (view instanceof TextView) {
                    TextView row = (TextView) view;
                    row.setTextColor(darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink));
                    row.setTextSize(20);
                    row.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                }
                return view;
            }
        };
    }
    private String guideFor(int instrument) { return notation == 1 ? INSTRUMENT_GUIDES_LATIN[instrument] : INSTRUMENT_GUIDES_ENGLISH[instrument]; }
    private void updateInstrumentGuide() {
        if (instrumentGuide == null) return;
        instrumentGuide.setText(guideFor(selectedInstrument));
        instrumentGuide.setVisibility(selectedInstrument == 0 ? View.GONE : View.VISIBLE);
    }
    private void selectInstrument(int position) {
        selectedInstrument = position; noteTransposition = NOTE_TRANSPOSITIONS[position]; preferences.edit().putInt("instrument", position).apply(); updateInstrumentGuide();
        if (lastHz > 0 && !signalMissing) showPitch(lastHz, lastConfidence, measurementState);
    }
    private void selectNotation(int position) {
        notation = position;
        preferences.edit().putInt("notation", position).apply();
        updateInstrumentGuide();
        if (lastHz > 0 && !signalMissing) showPitch(lastHz, lastConfidence, measurementState);
    }
    private void showSettings() {
        acceptingInstrumentChanges = false;
        acceptingNotationChanges = false;
        LinearLayout panel = column();
        panel.setPadding(dp(24), dp(8), dp(24), 0);
        panel.setContentDescription("Ajustes de instrumento, notación y calibración");
        TextView instrumentLabel = text(getString(R.string.instrument), 15); medium(instrumentLabel); panel.addView(instrumentLabel);
        instruments = new Spinner(this); instrumentAdapter = themedAdapter(INSTRUMENTS); instruments.setAdapter(instrumentAdapter); instruments.setContentDescription(getString(R.string.instrument)); panel.addView(instruments, new LinearLayout.LayoutParams(-1, -2));
        instrumentGuide = text(guideFor(selectedInstrument), 15); instrumentGuide.setPadding(0, 0, 0, dp(8)); panel.addView(instrumentGuide); updateInstrumentGuide();
        TextView notationLabel = text(getString(R.string.notation), 15); medium(notationLabel); panel.addView(notationLabel);
        notationSpinner = new Spinner(this); notationAdapter = themedAdapter(NOTATIONS); notationSpinner.setAdapter(notationAdapter); notationSpinner.setContentDescription(getString(R.string.notation)); panel.addView(notationSpinner, new LinearLayout.LayoutParams(-1, -2));
        calibration = text(getString(R.string.calibration, a4), 17); numeric(calibration); calibration.setPadding(0, dp(16), 0, 0); panel.addView(calibration);
        SeekBar slider = new SeekBar(this); slider.setMax(32); slider.setProgress(Math.max(0, Math.min(32, a4 - 424))); slider.setContentDescription(getString(R.string.calibration, a4)); panel.addView(slider, new LinearLayout.LayoutParams(-1, -2));
        noiseReductionSwitch = new Switch(this); noiseReductionSwitch.setText(R.string.noise_reduction); noiseReductionSwitch.setTextSize(16); medium(noiseReductionSwitch); noiseReductionSwitch.setPadding(0, dp(12), 0, 0); noiseReductionSwitch.setContentDescription(getString(R.string.noise_reduction)); noiseReductionSwitch.setChecked(noiseReduction); panel.addView(noiseReductionSwitch, new LinearLayout.LayoutParams(-1, -2));
        noiseReductionHint = text(getString(R.string.noise_reduction_hint), 13); noiseReductionHint.setSingleLine(false); noiseReductionHint.setPadding(0, 0, 0, dp(8)); panel.addView(noiseReductionHint, new LinearLayout.LayoutParams(-1, -2));
        inTuneSoundSwitch = new Switch(this); inTuneSoundSwitch.setText(R.string.in_tune_sound); inTuneSoundSwitch.setTextSize(16); medium(inTuneSoundSwitch); inTuneSoundSwitch.setPadding(0, dp(4), 0, 0); inTuneSoundSwitch.setContentDescription(getString(R.string.in_tune_sound)); inTuneSoundSwitch.setChecked(inTuneSound); panel.addView(inTuneSoundSwitch, new LinearLayout.LayoutParams(-1, -2));
        inTuneSoundHint = text(getString(R.string.in_tune_sound_hint), 13); inTuneSoundHint.setSingleLine(false); inTuneSoundHint.setPadding(0, 0, 0, dp(8)); panel.addView(inTuneSoundHint, new LinearLayout.LayoutParams(-1, -2));
        inTuneSoundTest = new Button(this); inTuneSoundTest.setText(R.string.test_sound); inTuneSoundTest.setTextSize(14); inTuneSoundTest.setAllCaps(false); medium(inTuneSoundTest); inTuneSoundTest.setContentDescription(getString(R.string.test_sound)); inTuneSoundTest.setOnClickListener(view -> playInTuneTone()); panel.addView(inTuneSoundTest, new LinearLayout.LayoutParams(-2, dp(44)));
        hapticFeedbackSwitch = new Switch(this); hapticFeedbackSwitch.setText(R.string.haptic_feedback); hapticFeedbackSwitch.setTextSize(16); medium(hapticFeedbackSwitch); hapticFeedbackSwitch.setPadding(0, dp(8), 0, 0); hapticFeedbackSwitch.setContentDescription(getString(R.string.haptic_feedback)); hapticFeedbackSwitch.setChecked(hapticFeedback); panel.addView(hapticFeedbackSwitch, new LinearLayout.LayoutParams(-1, -2));
        hapticFeedbackHint = text(getString(R.string.haptic_feedback_hint), 13); hapticFeedbackHint.setSingleLine(false); hapticFeedbackHint.setPadding(0, 0, 0, dp(8)); panel.addView(hapticFeedbackHint, new LinearLayout.LayoutParams(-1, -2));
        hapticFeedbackTest = new Button(this); hapticFeedbackTest.setText(R.string.test_vibration); hapticFeedbackTest.setTextSize(14); hapticFeedbackTest.setAllCaps(false); medium(hapticFeedbackTest); hapticFeedbackTest.setContentDescription(getString(R.string.test_vibration)); hapticFeedbackTest.setOnClickListener(view -> vibrateInTune()); panel.addView(hapticFeedbackTest, new LinearLayout.LayoutParams(-2, dp(44)));
        referenceFrequencySwitch = new Switch(this); referenceFrequencySwitch.setText(R.string.reference_frequency); referenceFrequencySwitch.setTextSize(16); medium(referenceFrequencySwitch); referenceFrequencySwitch.setPadding(0, dp(8), 0, 0); referenceFrequencySwitch.setContentDescription(getString(R.string.reference_frequency)); referenceFrequencySwitch.setChecked(referenceFrequencyVisible); panel.addView(referenceFrequencySwitch, new LinearLayout.LayoutParams(-1, -2));
        referenceFrequencyHint = text(getString(R.string.reference_frequency_hint), 13); referenceFrequencyHint.setSingleLine(false); referenceFrequencyHint.setPadding(0, 0, 0, dp(8)); panel.addView(referenceFrequencyHint, new LinearLayout.LayoutParams(-1, -2));
        ScrollView settingsScroll = new ScrollView(this); settingsScroll.setFillViewport(false); settingsScroll.addView(panel);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(settingsScroll).setPositiveButton(R.string.done, null).create();
        instruments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { if (acceptingInstrumentChanges) selectInstrument(position); }
        });
        instruments.setSelection(selectedInstrument, false);
        instruments.post(() -> { instruments.setSelection(selectedInstrument, false); acceptingInstrumentChanges = true; });
        notationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) { }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { if (acceptingNotationChanges) selectNotation(position); }
        });
        notationSpinner.setSelection(notation, false);
        notationSpinner.post(() -> { notationSpinner.setSelection(notation, false); acceptingNotationChanges = true; });
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { a4 = 424 + progress; calibration.setText(getString(R.string.calibration, a4)); bar.setContentDescription(getString(R.string.calibration, a4)); preferences.edit().putInt("a4", a4).apply(); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        noiseReductionSwitch.setOnCheckedChangeListener((button, checked) -> {
            noiseReduction = checked;
            preferences.edit().putBoolean("noise_reduction", checked).apply();
            applyNoiseReductionPreference();
        });
        inTuneSoundSwitch.setOnCheckedChangeListener((button, checked) -> {
            inTuneSound = checked;
            preferences.edit().putBoolean("in_tune_sound", checked).apply();
            inTuneFeedbackGate.reset();
        });
        hapticFeedbackSwitch.setOnCheckedChangeListener((button, checked) -> {
            hapticFeedback = checked;
            preferences.edit().putBoolean("haptic_feedback", checked).apply();
            inTuneFeedbackGate.reset();
            if (!checked) stopInTuneVibration();
        });
        referenceFrequencySwitch.setOnCheckedChangeListener((button, checked) -> {
            referenceFrequencyVisible = checked;
            preferences.edit().putBoolean("reference_frequency_visible", checked).apply();
            precisionDial.setReferenceFrequencyVisible(checked);
        });
        dialog.show();
        settingsDialog = dialog; settingsPanel = panel; this.instrumentLabel = instrumentLabel; this.notationLabel = notationLabel;
        dialog.setOnDismissListener(ignored -> { settingsDialog = null; settingsPanel = null; instrumentGuide = null; this.notationLabel = null; noiseReductionSwitch = null; noiseReductionHint = null; inTuneSoundSwitch = null; inTuneSoundHint = null; inTuneSoundTest = null; hapticFeedbackSwitch = null; hapticFeedbackHint = null; hapticFeedbackTest = null; referenceFrequencySwitch = null; referenceFrequencyHint = null; });
        styleSettingsPanel();
    }
    private void styleSettingsPanel() {
        if (settingsPanel == null || settingsDialog == null) return;
        int ink = darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink);
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);
        int surface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        settingsPanel.setBackgroundColor(surface); instrumentLabel.setTextColor(muted); instrumentGuide.setTextColor(muted); notationLabel.setTextColor(muted); calibration.setTextColor(ink); if (noiseReductionSwitch != null) noiseReductionSwitch.setTextColor(ink); if (noiseReductionHint != null) noiseReductionHint.setTextColor(muted); if (inTuneSoundSwitch != null) inTuneSoundSwitch.setTextColor(ink); if (inTuneSoundHint != null) inTuneSoundHint.setTextColor(muted); if (inTuneSoundTest != null) { inTuneSoundTest.setTextColor(actionColor()); inTuneSoundTest.setBackground(roundedBackground(surface, actionColor(), 16)); } if (hapticFeedbackSwitch != null) hapticFeedbackSwitch.setTextColor(ink); if (hapticFeedbackHint != null) hapticFeedbackHint.setTextColor(muted); if (hapticFeedbackTest != null) { hapticFeedbackTest.setTextColor(actionColor()); hapticFeedbackTest.setBackground(roundedBackground(surface, actionColor(), 16)); } if (referenceFrequencySwitch != null) referenceFrequencySwitch.setTextColor(ink); if (referenceFrequencyHint != null) referenceFrequencyHint.setTextColor(muted);
        instruments.getBackground().setTint(ink); instruments.setPopupBackgroundDrawable(new ColorDrawable(surface)); notationSpinner.getBackground().setTint(ink); notationSpinner.setPopupBackgroundDrawable(new ColorDrawable(surface));
        Button done = settingsDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (done != null) { done.setTextColor(actionColor()); done.setTextSize(16); done.setAllCaps(false); medium(done); }
        if (settingsDialog.getWindow() != null) settingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(surface));
    }
    private void applyTheme() {
        int ink = darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink);
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);
        int surface = darkMode ? Color.rgb(20, 23, 21) : getColor(R.color.surface);
        int action = actionColor();
        root.setBackgroundColor(surface); tunerPanel.setBackgroundColor(Color.TRANSPARENT); getWindow().setStatusBarColor(surface); getWindow().setNavigationBarColor(surface);
        int systemBars = darkMode ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!darkMode && Build.VERSION.SDK_INT >= 26) systemBars |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(systemBars);
        title.setTextColor(ink); status.setTextColor(muted); styleSettingsButton(action); styleThemeToggle(action); legal.setTextColor(action); privacy.setTextColor(muted); privacy.setAlpha(1f);
        toggle.setTextColor(Color.rgb(248, 247, 243)); toggle.setBackground(roundedBackground(getColor(R.color.accent), getColor(R.color.accent), 18));
        tuningState.setTextColor(!hasPitch ? muted : inTune ? action : ink);
        precisionDial.setDarkMode(darkMode);
        if (settingsDialog != null && settingsDialog.isShowing()) {
            if (instrumentAdapter != null) instrumentAdapter.notifyDataSetChanged();
            if (notationAdapter != null) notationAdapter.notifyDataSetChanged();
            if (instrumentGuide != null) instrumentGuide.setTextColor(muted);
            styleSettingsPanel();
        }
    }
    private int actionColor() { return darkMode ? getColor(R.color.accent_dark) : getColor(R.color.accent); }
    private void styleSettingsButton(int action) {
        if (settings == null) return;
        Drawable icon = getDrawable(R.drawable.ic_settings_menu);
        if (icon != null) { icon.setTint(action); settings.setImageDrawable(icon); }
        settings.setBackgroundColor(Color.TRANSPARENT);
        settings.setElevation(0);
    }
    private void styleThemeToggle(int action) {
        if (themeToggle == null) return;
        Drawable icon = getDrawable(darkMode ? R.drawable.ic_theme_sun : R.drawable.ic_theme_moon);
        if (icon != null) { icon.setTint(action); themeToggle.setImageDrawable(icon); }
        themeToggle.setContentDescription(getString(darkMode ? R.string.light_mode : R.string.dark_mode));
        // The 56 dp button remains an accessible touch target, but the visible interface is
        // only the theme glyph. A permanent border would compete with the tuning readout.
        themeToggle.setBackgroundColor(Color.TRANSPARENT);
        themeToggle.setElevation(0);
    }
    /**
     * Every readout has a fixed one-line metrics box.  Pitch changes therefore repaint only
     * their glyphs; they never cause a line wrap, a baseline shift, or a layout transition.
     */
    private TextView text(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);
        v.setIncludeFontPadding(false);
        v.setPadding(0, dp(5), 0, dp(5));
        return v;
    }
    private void medium(TextView view) { view.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); }
    private void numeric(TextView view) { view.setFontFeatureSettings("tnum"); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable roundedBackground(int fill, int stroke, int radius) { GradientDrawable background = new GradientDrawable(); background.setColor(fill); background.setCornerRadius(dp(radius)); background.setStroke(dp(1), stroke); return background; }
    private void showLegalNotice() { EthicEcosystemMenu.show(this, darkMode); }
    private void requestOrToggle() {
        if (running) { stop(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { requestNotificationThenStart(); return; }
        new AlertDialog.Builder(this).setTitle(R.string.microphone_title).setMessage(R.string.permission_rationale)
                .setNegativeButton(R.string.not_now, (dialog, which) -> showPermissionNeeded())
                .setPositiveButton(R.string.continue_action, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE)).show();
    }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == REQUEST_MICROPHONE) {
            if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) requestNotificationThenStart(); else showPermissionNeeded();
        } else if (r == REQUEST_NOTIFICATIONS) {
            beginTuning();
        }
    }
    private void showPermissionNeeded() { status.setText(R.string.permission_needed); status.setVisibility(View.VISIBLE); }
    private void requestNotificationThenStart() {
        if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) { beginTuning(); return; }
        new AlertDialog.Builder(this).setTitle(R.string.notification_title).setMessage(R.string.notification_rationale)
                .setNegativeButton(R.string.not_now, (dialog, which) -> beginTuning())
                .setPositiveButton(R.string.continue_action, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS)).show();
    }
    private void beginTuning() {
        if (running) return;
        running = true; signalMissing = true; noiseReductionEnabled = false; long generation = ++captureGeneration; toggle.setText(R.string.stop); showListeningStatus(); tuningState.setText(R.string.pitch_waiting); tuningState.setTextColor(darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted)); tuningState.setVisibility(View.VISIBLE);
        try { TuningSessionService.start(this); } catch (SecurityException | IllegalStateException ignored) { }
        new Thread(() -> listen(generation), "EthicTunerAudio").start();
    }
    private void stop() {
        running = false; ++captureGeneration; AudioRecord recorder = activeRecorder;
        if (recorder != null) try { recorder.stop(); } catch (IllegalStateException ignored) { }
        TuningSessionService.stop(this); stopInTuneVibration(); inTuneFeedbackGate.reset(); signalMissing = true; noiseReductionEnabled = false; hasPitch = false; lastConfidence = 0; measurementState = PitchTracker.State.NONE; lastNoteName = "—"; displayedMidi = Integer.MIN_VALUE; pendingMidi = Integer.MIN_VALUE; pendingMidiFrames = 0; toggle.setText(R.string.start); status.setText(R.string.waiting); status.setVisibility(View.GONE); if (precisionDial != null) precisionDial.setReading(lastNoteName, 0, 0, PitchTracker.State.NONE);
        if (tuningState != null) { tuningState.setText(R.string.pitch_waiting); tuningState.setTextColor(darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted)); tuningState.setVisibility(View.VISIBLE); }
    }
    private boolean captureIsCurrent(long generation) { return running && generation == captureGeneration; }
    @SuppressLint("MissingPermission") private void listen(long generation) {
        synchronized (captureLock) {
            if (!captureIsCurrent(generation)) return;
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { runOnUiThread(this::showPermissionNeeded); return; }
            final int rate = 44100; int minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) { showAudioProblem(R.string.audio_unavailable); return; }
            AudioRecord recorder = openRecorder(rate, Math.max(4096, minimum * 2));
            if (recorder == null) { showAudioProblem(R.string.audio_unavailable); return; }
            activeRecorder = recorder; NoiseSuppressor suppressor = createNoiseSuppressor(recorder); activeNoiseSuppressor = suppressor; runOnUiThread(this::showListeningStatus); PitchDetector detector = new PitchDetector(); PitchTracker tracker = new PitchTracker(); short[] samples = new short[4096];
            try {
                recorder.startRecording();
                while (captureIsCurrent(generation)) {
                    int count = recorder.read(samples, 0, samples.length, AudioRecord.READ_BLOCKING);
                    PitchDetector.Result result = count <= 0 ? null : detector.detect(samples, count, recorder.getSampleRate());
                    PitchTracker.Frame frame = tracker.update(result, android.os.SystemClock.elapsedRealtime());
                    showMeasurement(frame);
                }
            } catch (IllegalStateException | SecurityException error) { if (captureIsCurrent(generation)) showAudioProblem(R.string.audio_unavailable); }
            finally {
                try { recorder.stop(); } catch (IllegalStateException ignored) { }
                if (activeNoiseSuppressor == suppressor) activeNoiseSuppressor = null;
                if (suppressor != null) suppressor.release();
                noiseReductionEnabled = false;
                recorder.release(); if (activeRecorder == recorder) activeRecorder = null;
            }
        }
    }
    @SuppressLint("MissingPermission") private AudioRecord openRecorder(int rate, int size) {
        for (int source : new int[]{android.media.MediaRecorder.AudioSource.UNPROCESSED, android.media.MediaRecorder.AudioSource.MIC}) {
            try {
                AudioRecord recorder = new AudioRecord(source, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size);
                if (recorder.getState() == AudioRecord.STATE_INITIALIZED) return recorder;
                recorder.release();
            } catch (IllegalArgumentException | SecurityException ignored) { }
        }
        return null;
    }
    private NoiseSuppressor createNoiseSuppressor(AudioRecord recorder) {
        if (!noiseReduction || !NoiseSuppressor.isAvailable()) { noiseReductionEnabled = false; return null; }
        try {
            NoiseSuppressor suppressor = NoiseSuppressor.create(recorder.getAudioSessionId());
            if (suppressor != null) {
                suppressor.setEnabled(true);
                noiseReductionEnabled = suppressor.getEnabled();
            }
            return suppressor;
        } catch (RuntimeException ignored) {
            noiseReductionEnabled = false;
            return null;
        }
    }
    private void applyNoiseReductionPreference() {
        NoiseSuppressor suppressor = activeNoiseSuppressor;
        boolean enabled = false;
        if (suppressor != null) try { suppressor.setEnabled(noiseReduction); enabled = suppressor.getEnabled(); } catch (RuntimeException ignored) { }
        noiseReductionEnabled = enabled;
        showListeningStatus();
    }
    private void showListeningStatus() { if (running && status != null) { status.setText(noiseReductionEnabled ? R.string.listening_filtered : R.string.listening); status.setVisibility(noiseReductionEnabled ? View.VISIBLE : View.GONE); } }
    private void showAudioProblem(int message) { runOnUiThread(() -> { if (running) { stop(); status.setText(message); status.setVisibility(View.VISIBLE); } }); }
    private void showMeasurement(PitchTracker.Frame frame) {
        if (frame.state == PitchTracker.State.STABLE && InstrumentRange.accepts(selectedInstrument, frame.frequencyHz)) {
            showPitch(frame.frequencyHz, frame.confidence, frame.state);
            return;
        }
        if (frame.state != PitchTracker.State.NONE && InstrumentRange.accepts(selectedInstrument, frame.frequencyHz)) return;
        displayedMidi = Integer.MIN_VALUE;
        pendingMidi = Integer.MIN_VALUE;
        pendingMidiFrames = 0;
        if (signalMissing) return;
        signalMissing = true;
        runOnUiThread(() -> {
            if (!running) return;
            inTuneFeedbackGate.shouldPlay(false, android.os.SystemClock.elapsedRealtime());
            hasPitch = false;
            inTune = false;
            stopInTuneVibration();
            precisionDial.setSignalMissing();
            tuningState.setText(R.string.pitch_waiting);
            tuningState.setTextColor(darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted));
            tuningState.setVisibility(View.VISIBLE);
        });
    }

    private int displayedMidi(double midi, PitchTracker.State state) {
        int proposed = (int) Math.round(midi);
        if (displayedMidi == Integer.MIN_VALUE) {
            displayedMidi = proposed;
            return displayedMidi;
        }
        if (state != PitchTracker.State.STABLE || proposed == displayedMidi) {
            if (proposed == displayedMidi) { pendingMidi = Integer.MIN_VALUE; pendingMidiFrames = 0; }
            return displayedMidi;
        }
        double centsFromDisplayed = 100 * (midi - displayedMidi);
        boolean pastHysteresis = proposed > displayedMidi ? centsFromDisplayed >= 60 : centsFromDisplayed <= -60;
        if (!pastHysteresis) { pendingMidi = Integer.MIN_VALUE; pendingMidiFrames = 0; return displayedMidi; }
        if (pendingMidi == proposed) pendingMidiFrames++; else { pendingMidi = proposed; pendingMidiFrames = 1; }
        if (pendingMidiFrames >= 3) { displayedMidi = proposed; pendingMidi = Integer.MIN_VALUE; pendingMidiFrames = 0; }
        return displayedMidi;
    }

    private void showPitch(double hz, double confidence, PitchTracker.State state) {
        double midi = 69 + 12 * Math.log(hz / a4) / Math.log(2);
        int proposedMidi = (int) Math.round(midi);
        int stableMidi = displayedMidi(midi, state);
        // Do not combine a previous note name with a new frequency while the note latch is
        // collecting evidence. Keep the last stable display instead of replacing it with a
        // transient warning that would make the meter appear to flicker.
        if (state == PitchTracker.State.STABLE && stableMidi != proposedMidi && lastHz > 0) {
            runOnUiThread(this::stopInTuneVibration);
            return;
        }
        int deviation = (int) Math.round(100 * (midi - stableMidi));
        double targetFrequencyHz = NoteReference.frequencyHz(stableMidi, a4);
        String[] names = notation == 1 ? LATIN_NOTES : ENGLISH_NOTES;
        int displayedNote = stableMidi + noteTransposition;
        String name = names[(displayedNote % 12 + 12) % 12] + (displayedNote / 12 - 1);
        boolean pitchInTune = state == PitchTracker.State.STABLE && Math.abs(deviation) <= 5;
        signalMissing = false;
        runOnUiThread(() -> {
            if (!running) return;
            hasPitch = true;
            measurementState = state;
            inTune = pitchInTune;
            updateInTuneVibration(pitchInTune);
            playInTuneCueIfNeeded(pitchInTune);
            lastHz = hz;
            lastDeviation = deviation;
            lastConfidence = confidence;
            lastNoteName = name;
            precisionDial.setReading(name, hz, targetFrequencyHz, deviation, state);
            tuningState.setVisibility(View.VISIBLE);
            tuningState.setText(inTune ? R.string.in_tune : deviation < 0 ? R.string.pitch_low : R.string.pitch_high);
            tuningState.setTextColor(inTune ? actionColor() : darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink));
        });
    }
    static void stopActiveTuningFromService() {
        MainActivity activity = activeInstance.get();
        if (activity != null) activity.runOnUiThread(activity::stop);
    }
    private void playInTuneCueIfNeeded(boolean isInTune) {
        if (!inTuneFeedbackGate.shouldPlay(isInTune, android.os.SystemClock.elapsedRealtime())) return;
        if (inTuneSound) try { playInTuneTone(); } catch (RuntimeException ignored) { }
    }
    private void updateInTuneVibration(boolean isInTune) {
        if (!hapticFeedback) { stopInTuneVibration(); return; }
        if (!isInTune) {
            // Prevent a one-frame boundary fluctuation from interrupting a useful, continuous
            // confirmation. The screen remains truthful immediately; only haptic release is
            // delayed briefly. A real missing signal still calls stopInTuneVibration directly.
            if (hapticRunning) {
                mainHandler.removeCallbacks(hapticRelease);
                mainHandler.postDelayed(hapticRelease, HAPTIC_RELEASE_GRACE_MS);
            }
            return;
        }
        mainHandler.removeCallbacks(hapticRelease);
        if (hapticRunning) return;
        hapticRunning = true;
        mainHandler.removeCallbacks(inTuneVibrationPulse);
        inTuneVibrationPulse.run();
    }
    private void stopInTuneVibration() {
        mainHandler.removeCallbacks(inTuneVibrationPulse);
        mainHandler.removeCallbacks(hapticRelease);
        if (!hapticRunning) return;
        Vibrator vibrator = systemVibrator();
        if (vibrator != null) vibrator.cancel();
        hapticRunning = false;
    }
    @SuppressWarnings("deprecation") private void vibrateInTune() {
        Vibrator vibrator = systemVibrator();
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrateFeedback(vibrator, VibrationEffect.createWaveform(new long[]{0, 150, 80, 150}, new int[]{0, 255, 0, 255}, -1));
        else vibrator.vibrate(new long[]{0, 150, 80, 150}, -1);
    }
    private void vibrateFeedback(Vibrator vibrator, VibrationEffect effect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibrationAttributes attributes = new VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_MEDIA).build();
            vibrator.vibrate(effect, attributes);
        } else vibrator.vibrate(effect);
    }
    private Vibrator systemVibrator() {
        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = getSystemService(VibratorManager.class);
            vibrator = manager == null ? null : manager.getDefaultVibrator();
        } else vibrator = getSystemService(Vibrator.class);
        return vibrator;
    }
    private void playInTuneTone() {
        releaseInTuneTone();
        try {
            short[] pcm = ConfirmationTone.createPcm();
            inTuneTone = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(ConfirmationTone.SAMPLE_RATE_HZ).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.length * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            inTuneTone.write(pcm, 0, pcm.length, AudioTrack.WRITE_BLOCKING);
            inTuneTone.play();
            mainHandler.postDelayed(this::releaseInTuneTone, 120);
        } catch (RuntimeException ignored) {
            releaseInTuneTone();
        }
    }
    private void releaseInTuneTone() {
        if (inTuneTone == null) return;
        try { inTuneTone.stop(); } catch (IllegalStateException ignored) { }
        inTuneTone.release();
        inTuneTone = null;
    }
    @Override public void onConfigurationChanged(Configuration configuration) { super.onConfigurationChanged(configuration); buildUi(); }
    /**
     * Temporary interruptions such as a notification, a permission surface or a call UI can
     * pause this activity. They must not silently cancel a tuning session the person chose to
     * keep running. The user stops it with the explicit control; closing the activity releases
     * the recorder here. Android may still revoke the microphone for a real hardware or system
     * conflict, which is reported as unavailable rather than hidden.
     */
    @Override protected void onDestroy() { if (activeInstance.get() == this) activeInstance = new WeakReference<>(null); stop(); releaseInTuneTone(); super.onDestroy(); }
}
