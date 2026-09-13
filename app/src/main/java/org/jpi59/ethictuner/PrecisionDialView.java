/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/** Original linear chromatic-tuner display. It receives tracked measurements, never audio. */
public final class PrecisionDialView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private String note = "—";
    private double frequencyHz;
    private int cents;
    private PitchTracker.State state = PitchTracker.State.NONE;
    private boolean darkMode;

    public PrecisionDialView(Context context) { super(context); initialise(); }
    public PrecisionDialView(Context context, AttributeSet attrs) { super(context, attrs); initialise(); }

    private void initialise() {
        setMinimumHeight(dp(252));
        setContentDescription("Afinador cromático. Esperando una nota.");
    }

    public void setReading(String note, double frequencyHz, int cents, PitchTracker.State state) {
        String safeNote = note == null ? "—" : note;
        int bounded = Math.max(-50, Math.min(50, cents));
        if (safeNote.equals(this.note) && this.frequencyHz == frequencyHz && this.cents == bounded && this.state == state) return;
        this.note = safeNote;
        this.frequencyHz = frequencyHz;
        this.cents = bounded;
        this.state = state;
        String description = state == PitchTracker.State.NONE ? "Esperando una nota" : safeNote + ", "
                + String.format(java.util.Locale.US, "%.1f Hz, %+d cents", frequencyHz, bounded) + ", " + stateLabel();
        setContentDescription("Afinador cromático: " + description + ".");
        invalidate();
    }

    public void setDarkMode(boolean darkMode) {
        if (this.darkMode == darkMode) return;
        this.darkMode = darkMode;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        panel.set(dp(2), dp(2), width - dp(2), height - dp(2));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(darkMode ? Color.rgb(20, 29, 25) : Color.rgb(240, 246, 242));
        canvas.drawRoundRect(panel, dp(24), dp(24), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(darkMode ? Color.rgb(49, 86, 69) : Color.rgb(185, 209, 193));
        canvas.drawRoundRect(panel, dp(24), dp(24), paint);

        drawHeader(canvas, width);
        drawReadout(canvas, width, height);
        drawScale(canvas, width, height);
    }

    private void drawHeader(Canvas canvas, float width) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(sp(11));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(darkMode ? Color.rgb(164, 186, 172) : Color.rgb(73, 102, 85));
        canvas.drawText("LECTURA CROMÁTICA", dp(22), dp(30), paint);
        String label = stateLabel().toUpperCase(java.util.Locale.ROOT);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(state == PitchTracker.State.AMBIGUOUS ? Color.rgb(183, 109, 33)
                : darkMode ? Color.rgb(153, 215, 180) : Color.rgb(30, 112, 76));
        canvas.drawText(label, width - dp(22), dp(30), paint);
        paint.setStrokeWidth(dp(1));
        paint.setColor(darkMode ? Color.rgb(48, 72, 59) : Color.rgb(202, 219, 207));
        canvas.drawLine(dp(22), dp(44), width - dp(22), dp(44), paint);
    }

    private void drawReadout(Canvas canvas, float width, float height) {
        float centerX = width / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(sp(64));
        paint.setColor(darkMode ? Color.rgb(233, 241, 235) : Color.rgb(26, 43, 33));
        canvas.drawText(note, centerX, height * .40f, paint);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(sp(15));
        paint.setColor(darkMode ? Color.rgb(180, 198, 186) : Color.rgb(75, 100, 84));
        String frequency = state == PitchTracker.State.NONE ? "Toca una nota para comenzar" : String.format(java.util.Locale.US, "%.1f Hz", frequencyHz);
        canvas.drawText(frequency, centerX, height * .40f + dp(25), paint);
    }

    private void drawScale(Canvas canvas, float width, float height) {
        float left = dp(34);
        float right = width - dp(34);
        float axisY = height * .71f;
        float unit = (right - left) / 100f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(darkMode ? Color.rgb(41, 62, 51) : Color.rgb(214, 226, 218));
        canvas.drawRoundRect(new RectF(left, axisY - dp(7), right, axisY + dp(7)), dp(8), dp(8), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        for (int value = -50; value <= 50; value += 5) {
            boolean major = value % 10 == 0;
            float x = left + (value + 50) * unit;
            paint.setStrokeWidth(dp(major ? 2 : 1));
            paint.setColor(darkMode ? Color.rgb(177, 198, 184) : Color.rgb(68, 92, 77));
            canvas.drawLine(x, axisY - dp(major ? 18 : 12), x, axisY + dp(major ? 18 : 12), paint);
        }
        float zero = left + 50 * unit;
        paint.setStrokeWidth(dp(2));
        paint.setColor(darkMode ? Color.rgb(142, 224, 174) : Color.rgb(24, 122, 81));
        canvas.drawLine(zero, axisY - dp(27), zero, axisY + dp(27), paint);
        if (state != PitchTracker.State.NONE) {
            float marker = left + (cents + 50) * unit;
            paint.setStrokeWidth(dp(4));
            paint.setColor(markerColor());
            canvas.drawLine(marker, axisY - dp(37), marker, axisY + dp(37), paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setTextSize(sp(12));
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(darkMode ? Color.rgb(162, 182, 168) : Color.rgb(76, 100, 85));
        canvas.drawText("−50", left, axisY + dp(58), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("0", zero, axisY + dp(58), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("+50", right, axisY + dp(58), paint);
        if (state != PitchTracker.State.NONE) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(sp(14));
            paint.setColor(markerColor());
            canvas.drawText(String.format(java.util.Locale.US, "%+d cents", cents), width / 2f, height - dp(24), paint);
        }
    }

    private int markerColor() {
        if (state == PitchTracker.State.AMBIGUOUS) return Color.rgb(205, 130, 47);
        if (state == PitchTracker.State.STABLE && Math.abs(cents) <= 5) return getContext().getColor(darkMode ? R.color.accent_dark : R.color.accent);
        return darkMode ? Color.rgb(229, 239, 232) : Color.rgb(27, 61, 42);
    }

    private String stateLabel() {
        if (state == PitchTracker.State.HELD) return "señal débil";
        if (state == PitchTracker.State.AMBIGUOUS) return "lectura ambigua";
        if (state == PitchTracker.State.STABLE) return "lectura estable";
        return "esperando señal";
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int sp(int value) { return Math.round(value * getResources().getDisplayMetrics().scaledDensity); }
}
