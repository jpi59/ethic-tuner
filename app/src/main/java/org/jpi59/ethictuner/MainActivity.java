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
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

/** A local chromatic tuner. Audio samples are analysed in memory and discarded. */
public final class MainActivity extends Activity {
    private static final int REQUEST_MICROPHONE = 10;
    private static final String[] INSTRUMENTS = {"Cromático", "Guitarra", "Bajo", "Ukelele", "Violín", "Teclado / Piano", "Flauta dulce", "Flauta travesera", "Clarinete en Si♭", "Saxofón sopranino (Mi♭)", "Saxofón soprano (Si♭)", "Saxofón alto (Mi♭)", "Saxofón tenor (Si♭)", "Saxofón barítono (Mi♭)", "Saxofón bajo (Si♭)", "Saxofón contrabajo (Mi♭)"};
    private static final String[] INSTRUMENT_GUIDES_ENGLISH = {"Cualquier nota", "Afinación estándar: E A D G B E", "Afinación estándar: E A D G", "Afinación estándar: G C E A", "Afinación estándar: G D A E", "Toca una tecla; usa A4 para calibrar", "Nota de concierto", "Nota de concierto", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭"};
    private static final String[] INSTRUMENT_GUIDES_LATIN = {"Cualquier nota", "Afinación estándar: Mi La Re Sol Si Mi", "Afinación estándar: Mi La Re Sol", "Afinación estándar: Sol Do Mi La", "Afinación estándar: Sol Re La Mi", "Toca una tecla; usa A4 para calibrar", "Nota de concierto", "Nota de concierto", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭", "Nota escrita · instrumento en Si♭", "Nota escrita · instrumento en Mi♭"};
    private static final int[] NOTE_TRANSPOSITIONS = {0, 0, 0, 0, 0, 0, 0, 0, 2, 9, 2, 9, 2, 9, 2, 9};
    private static final String[] NOTATIONS = {"Notación inglesa · C D E", "Notación latina · Do Re Mi"};
    private static final String[] ENGLISH_NOTES = {"C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B"};
    private static final String[] LATIN_NOTES = {"Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si"};
    private TextView status, calibration, instrumentGuide, tuningState, signal;
    private PrecisionDialView precisionDial;
    private Button toggle;
    private LinearLayout root;
    private LinearLayout tunerPanel;
    private TextView title, privacy;
    private Button legal;
    private Button settings;
    private Switch darkSwitch;
    private Spinner instruments, notationSpinner;
    private ArrayAdapter<String> instrumentAdapter, notationAdapter;
    private AlertDialog settingsDialog;
    private LinearLayout settingsPanel;
    private TextView instrumentLabel, notationLabel, noiseReductionHint;
    private Switch noiseReductionSwitch;
    private SharedPreferences preferences;
    private boolean darkMode, hasPitch, inTune, acceptingNotationChanges, acceptingInstrumentChanges;
    private volatile boolean noiseReduction, noiseReductionEnabled;
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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("appearance", MODE_PRIVATE);
        darkMode = preferences.getBoolean("dark_mode", false); noiseReduction = preferences.getBoolean("noise_reduction", true); a4 = preferences.getInt("a4", 440); selectedInstrument = Math.max(0, Math.min(INSTRUMENTS.length - 1, preferences.getInt("instrument", 0))); noteTransposition = NOTE_TRANSPOSITIONS[selectedInstrument]; notation = Math.max(0, Math.min(NOTATIONS.length - 1, preferences.getInt("notation", 0)));
        buildUi();
    }
    private void buildUi() {
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL); root.setPadding(dp(landscape ? 16 : 20), dp(landscape ? 12 : 20), dp(landscape ? 16 : 20), dp(landscape ? 12 : 16));
        LinearLayout header = column();
        tunerPanel = column();
        LinearLayout controls = column();
        title = text("Ethic Tuner", landscape ? 22 : 25); medium(title); header.addView(title);
        LinearLayout headerControls = new LinearLayout(this); headerControls.setOrientation(LinearLayout.HORIZONTAL); headerControls.setGravity(Gravity.CENTER_VERTICAL);
        darkSwitch = new Switch(this); darkSwitch.setText(R.string.dark_mode); darkSwitch.setTextSize(16); medium(darkSwitch); darkSwitch.setPadding(dp(7), 0, 0, 0); darkSwitch.setGravity(Gravity.CENTER_VERTICAL); darkSwitch.setContentDescription(getString(R.string.dark_mode)); darkSwitch.setChecked(darkMode); darkSwitch.setOnCheckedChangeListener((button, checked) -> { darkMode = checked; preferences.edit().putBoolean("dark_mode", checked).apply(); applyTheme(); }); headerControls.addView(darkSwitch, new LinearLayout.LayoutParams(0, -2, 1f));
        settings = new Button(this); settings.setText(R.string.settings); settings.setTextSize(15); settings.setAllCaps(false); medium(settings); settings.setGravity(Gravity.CENTER); settings.setPadding(dp(14), dp(4), dp(14), dp(4)); settings.setMinWidth(0); settings.setMinHeight(dp(40)); settings.setContentDescription(getString(R.string.settings)); settings.setOnClickListener(v -> showSettings()); headerControls.addView(settings, new LinearLayout.LayoutParams(-2, -2));
        header.addView(headerControls, new LinearLayout.LayoutParams(-1, -2));
        status = text(getString(R.string.waiting), 13); status.setVisibility(View.GONE); header.addView(status, new LinearLayout.LayoutParams(-1, -2));
        tunerPanel.setPadding(0, dp(landscape ? 6 : 8), 0, dp(landscape ? 4 : 6));
        precisionDial = new PrecisionDialView(this); tunerPanel.addView(precisionDial, new LinearLayout.LayoutParams(-1, dp(landscape ? 270 : 350)));
        tuningState = text(getString(R.string.pitch_waiting), 18); medium(tuningState); tuningState.setVisibility(View.INVISIBLE); tunerPanel.addView(tuningState);
        signal = text(getString(R.string.signal_waiting), 14); signal.setVisibility(hasPitch ? View.VISIBLE : View.INVISIBLE); tunerPanel.addView(signal);
        toggle = new Button(this); toggle.setText(R.string.start); toggle.setTextSize(18); toggle.setAllCaps(false); medium(toggle); toggle.setOnClickListener(v -> requestOrToggle());
        LinearLayout.LayoutParams primaryAction = new LinearLayout.LayoutParams(-1, dp(52)); primaryAction.setMargins(0, dp(8), 0, dp(2)); controls.addView(toggle, primaryAction);
        legal = new Button(this); legal.setText(R.string.license_action); legal.setTextSize(15); legal.setAllCaps(false); medium(legal); legal.setGravity(Gravity.CENTER); legal.setMinHeight(dp(48)); legal.setPadding(dp(12), dp(8), dp(12), dp(8)); legal.setBackgroundColor(Color.TRANSPARENT); legal.setContentDescription(getString(R.string.license_action)); legal.setOnClickListener(v -> showLegalNotice()); controls.addView(legal);
        privacy = text(getString(R.string.privacy), 13); privacy.setGravity(Gravity.CENTER); privacy.setPadding(0, dp(landscape ? 6 : 10), 0, 0);
        if (landscape) {
            LinearLayout leftColumn = column(); leftColumn.addView(header); leftColumn.addView(controls); leftColumn.addView(privacy);
            root.setOrientation(LinearLayout.HORIZONTAL); root.addView(leftColumn, weighted()); root.addView(tunerPanel, weighted());
        } else { root.addView(header); root.addView(tunerPanel); root.addView(controls); root.addView(privacy); }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(root);
        setContentView(scroll);
        applyTheme();
        if (running) { showListeningStatus(); toggle.setText(R.string.stop); precisionDial.setReading(lastNoteName, lastHz, lastDeviation, measurementState); if (hasPitch) updateSignal(lastConfidence); }
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setGravity(Gravity.CENTER_HORIZONTAL); return layout; }
    private LinearLayout.LayoutParams weighted() { return new LinearLayout.LayoutParams(0, -2, 1f); }
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
        if (lastHz > 0) showPitch(lastHz, lastConfidence, measurementState);
    }
    private void selectNotation(int position) {
        notation = position;
        preferences.edit().putInt("notation", position).apply();
        updateInstrumentGuide();
        if (lastHz > 0) showPitch(lastHz, lastConfidence, measurementState);
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
        AlertDialog dialog = new AlertDialog.Builder(this).setView(panel).setPositiveButton(R.string.done, null).create();
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
        dialog.show();
        settingsDialog = dialog; settingsPanel = panel; this.instrumentLabel = instrumentLabel; this.notationLabel = notationLabel;
        dialog.setOnDismissListener(ignored -> { settingsDialog = null; settingsPanel = null; instrumentGuide = null; this.notationLabel = null; noiseReductionSwitch = null; noiseReductionHint = null; });
        styleSettingsPanel();
    }
    private void styleSettingsPanel() {
        if (settingsPanel == null || settingsDialog == null) return;
        int ink = darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink);
        int muted = darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted);
        int surface = darkMode ? Color.rgb(30, 35, 32) : Color.rgb(255, 255, 255);
        settingsPanel.setBackgroundColor(surface); instrumentLabel.setTextColor(muted); instrumentGuide.setTextColor(muted); notationLabel.setTextColor(muted); calibration.setTextColor(ink); if (noiseReductionSwitch != null) noiseReductionSwitch.setTextColor(ink); if (noiseReductionHint != null) noiseReductionHint.setTextColor(muted);
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
        title.setTextColor(ink); status.setTextColor(muted); darkSwitch.setTextColor(ink); settings.setTextColor(action); settings.setBackground(roundedBackground(surface, action, 20)); legal.setTextColor(action); privacy.setTextColor(muted); privacy.setAlpha(1f); signal.setTextColor(muted);
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
    private void showLegalNotice() { new AlertDialog.Builder(this).setTitle(R.string.license_title).setMessage(R.string.license_message).setPositiveButton(android.R.string.ok, null).show(); }
    private void requestOrToggle() {
        if (running) { stop(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { start(); return; }
        new AlertDialog.Builder(this).setTitle(R.string.microphone_title).setMessage(R.string.permission_rationale)
                .setNegativeButton(R.string.not_now, (dialog, which) -> showPermissionNeeded())
                .setPositiveButton(R.string.continue_action, (dialog, which) -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE)).show();
    }
    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) { super.onRequestPermissionsResult(r, p, g); if (r == REQUEST_MICROPHONE && g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) start(); else showPermissionNeeded(); }
    private void showPermissionNeeded() { status.setText(R.string.permission_needed); status.setVisibility(View.VISIBLE); tuningState.setVisibility(View.INVISIBLE); }
    private void start() {
        if (running) return;
        running = true; noiseReductionEnabled = false; long generation = ++captureGeneration; toggle.setText(R.string.stop); showListeningStatus(); tuningState.setVisibility(View.INVISIBLE); signal.setVisibility(View.INVISIBLE);
        new Thread(() -> listen(generation), "EthicTunerAudio").start();
    }
    private void stop() {
        running = false; ++captureGeneration; AudioRecord recorder = activeRecorder;
        if (recorder != null) try { recorder.stop(); } catch (IllegalStateException ignored) { }
        noiseReductionEnabled = false; hasPitch = false; lastConfidence = 0; measurementState = PitchTracker.State.NONE; lastNoteName = "—"; displayedMidi = Integer.MIN_VALUE; pendingMidi = Integer.MIN_VALUE; pendingMidiFrames = 0; toggle.setText(R.string.start); status.setText(R.string.waiting); status.setVisibility(View.GONE); if (precisionDial != null) precisionDial.setReading(lastNoteName, 0, 0, PitchTracker.State.NONE);
        if (tuningState != null) { tuningState.setVisibility(View.INVISIBLE); signal.setVisibility(View.INVISIBLE); }
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
    private void showAudioProblem(int message) { runOnUiThread(() -> { if (running) { stop(); status.setText(message); status.setVisibility(View.VISIBLE); signal.setText(R.string.signal_unavailable); } }); }
    private void showMeasurement(PitchTracker.Frame frame) {
        if (frame.state != PitchTracker.State.NONE) {
            showPitch(frame.frequencyHz, frame.confidence, frame.state);
            return;
        }
        displayedMidi = Integer.MIN_VALUE;
        pendingMidi = Integer.MIN_VALUE;
        pendingMidiFrames = 0;
        runOnUiThread(() -> {
            if (!running) return;
            hasPitch = false;
            measurementState = PitchTracker.State.NONE;
            lastHz = 0;
            lastNoteName = "—";
            precisionDial.setReading(lastNoteName, 0, 0, PitchTracker.State.NONE);
            tuningState.setVisibility(View.INVISIBLE);
            signal.setVisibility(View.INVISIBLE);
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
        // collecting evidence. The prior display remains truthful and visibly ambiguous.
        if (state == PitchTracker.State.STABLE && stableMidi != proposedMidi && lastHz > 0) {
            runOnUiThread(() -> {
                if (!running) return;
                hasPitch = true;
                measurementState = PitchTracker.State.AMBIGUOUS;
                inTune = false;
                precisionDial.setReading(lastNoteName, lastHz, lastDeviation, PitchTracker.State.AMBIGUOUS);
                tuningState.setVisibility(View.VISIBLE);
                tuningState.setText(R.string.pitch_ambiguous);
                tuningState.setTextColor(darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink));
                signal.setVisibility(View.INVISIBLE);
            });
            return;
        }
        int deviation = (int) Math.round(100 * (midi - stableMidi));
        String[] names = notation == 1 ? LATIN_NOTES : ENGLISH_NOTES;
        int displayedNote = stableMidi + noteTransposition;
        String name = names[(displayedNote % 12 + 12) % 12] + (displayedNote / 12 - 1);
        boolean pitchInTune = state == PitchTracker.State.STABLE && Math.abs(deviation) <= 5;
        runOnUiThread(() -> {
            if (!running) return;
            hasPitch = true;
            measurementState = state;
            inTune = pitchInTune;
            lastHz = hz;
            lastDeviation = deviation;
            lastConfidence = confidence;
            lastNoteName = name;
            precisionDial.setReading(name, hz, deviation, state);
            tuningState.setVisibility(View.VISIBLE);
            if (state == PitchTracker.State.HELD) {
                tuningState.setText(R.string.pitch_held);
                tuningState.setTextColor(darkMode ? Color.rgb(177, 184, 177) : getColor(R.color.muted));
                signal.setVisibility(View.INVISIBLE);
            } else if (state == PitchTracker.State.AMBIGUOUS) {
                tuningState.setText(R.string.pitch_ambiguous);
                tuningState.setTextColor(darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink));
                signal.setVisibility(View.INVISIBLE);
            } else {
                tuningState.setText(inTune ? R.string.in_tune : deviation < 0 ? R.string.pitch_low : R.string.pitch_high);
                tuningState.setTextColor(inTune ? actionColor() : darkMode ? Color.rgb(226, 232, 226) : getColor(R.color.ink));
                updateSignal(confidence);
            }
        });
    }
    private void updateSignal(double confidence) { signal.setVisibility(confidence > 0 ? View.VISIBLE : View.INVISIBLE); if (confidence > 0) signal.setText(getString(R.string.signal_stable, Math.round(confidence * 100))); }
    @Override public void onConfigurationChanged(Configuration configuration) { super.onConfigurationChanged(configuration); buildUi(); }
    @Override protected void onPause() { stop(); super.onPause(); }
}
