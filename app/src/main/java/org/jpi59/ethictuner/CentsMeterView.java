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

/** A compact, accessible ruler for the tracked cents value. */
public final class CentsMeterView extends View {
    private static final int RANGE_CENTS = 50;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private int cents;
    private PitchTracker.State state = PitchTracker.State.NONE;
    private boolean darkMode;

    public CentsMeterView(Context context) { super(context); initialise(); }
    public CentsMeterView(Context context, AttributeSet attrs) { super(context, attrs); initialise(); }

    private void initialise() {
        setMinimumHeight(dp(66));
        setContentDescription("Regla de desviación en cents. Esperando una nota.");
    }

    public void setPitch(int cents, PitchTracker.State state) {
        int bounded = Math.max(-RANGE_CENTS, Math.min(RANGE_CENTS, cents));
        if (this.cents == bounded && this.state == state) return;
        this.cents = bounded;
        this.state = state;
        String text = state == PitchTracker.State.NONE ? "Esperando una nota" : String.format(java.util.Locale.US, "%+d cents", bounded);
        setContentDescription("Regla de afinación: " + text + ".");
        invalidate();
    }

    public void setDarkMode(boolean darkMode) {
        if (this.darkMode == darkMode) return;
        this.darkMode = darkMode;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = dp(3);
        bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(darkMode ? Color.rgb(25, 34, 30) : Color.rgb(228, 237, 231));
        canvas.drawRoundRect(bounds, dp(14), dp(14), paint);
        float left = dp(18), right = getWidth() - dp(18), centre = (left + right) / 2f;
        float baseline = getHeight() - dp(21);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int value = -50; value <= 50; value += 5) {
            float x = centre + (right - left) * value / (RANGE_CENTS * 2f);
            boolean major = value % 10 == 0;
            paint.setColor(darkMode ? Color.rgb(144, 158, 148) : Color.rgb(81, 97, 87));
            paint.setStrokeWidth(dp(major ? 2 : 1));
            canvas.drawLine(x, baseline, x, baseline - dp(major ? 18 : 10), paint);
        }
        paint.setColor(darkMode ? Color.rgb(188, 207, 195) : Color.rgb(57, 74, 64));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(11));
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        canvas.drawText("−50", left, dp(17), paint);
        canvas.drawText("0", centre, dp(17), paint);
        canvas.drawText("+50", right, dp(17), paint);
        if (state == PitchTracker.State.NONE) return;
        float marker = centre + (right - left) * cents / (RANGE_CENTS * 2f);
        paint.setColor(markerColor());
        if (state == PitchTracker.State.HELD) paint.setAlpha(135);
        paint.setStrokeWidth(dp(3));
        canvas.drawLine(marker, dp(12), marker, baseline + dp(4), paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(marker, dp(12), dp(5), paint);
        paint.setAlpha(255);
    }

    private int markerColor() {
        if (state == PitchTracker.State.AMBIGUOUS) return Color.rgb(185, 113, 38);
        return getContext().getColor(darkMode ? R.color.accent_dark : R.color.accent);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int sp(int value) { return Math.round(value * getResources().getDisplayMetrics().scaledDensity); }
}
