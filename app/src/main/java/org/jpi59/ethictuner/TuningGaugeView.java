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

/** A readable analogue-style pitch dial. It does not process or retain audio. */
public final class TuningGaugeView extends View {
    private static final int RANGE_CENTS = 50;
    private static final int TOLERANCE_CENTS = 5;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private int cents;
    private boolean hasPitch;
    private boolean darkMode;

    public TuningGaugeView(Context context) { super(context); initialise(); }
    public TuningGaugeView(Context context, AttributeSet attrs) { super(context, attrs); initialise(); }

    private void initialise() {
        setMinimumHeight(dp(150));
        setContentDescription("Indicador de afinación. Esperando una nota.");
    }

    public void setPitch(int cents, boolean hasPitch) {
        int bounded = Math.max(-RANGE_CENTS, Math.min(RANGE_CENTS, cents));
        if (this.cents == bounded && this.hasPitch == hasPitch) return;
        this.cents = bounded;
        this.hasPitch = hasPitch;
        String state = !hasPitch ? "Esperando una nota" : Math.abs(bounded) <= TOLERANCE_CENTS
                ? "Afinado" : bounded < 0 ? "Bajo" : "Agudo";
        setContentDescription("Indicador de afinación: " + state + ", " + Math.abs(bounded) + " cents.");
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
        float centreX = width / 2f;
        float centreY = getHeight() - dp(20);
        float radius = Math.min(width * .44f, getHeight() * .86f);
        float stroke = dp(8);
        arc.set(centreX - radius, centreY - radius, centreX + radius, centreY + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(darkMode ? Color.rgb(72, 78, 73) : Color.rgb(207, 211, 207));
        canvas.drawArc(arc, 200, 140, false, paint);
        paint.setColor(accentColor());
        canvas.drawArc(arc, 263, 14, false, paint);

        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(dp(2));
        for (int value = -50; value <= 50; value += 10) {
            double angle = Math.toRadians(270 + value * 1.4);
            float inner = radius - dp(value % 20 == 0 ? 20 : 14);
            float x1 = centreX + (float) Math.cos(angle) * inner;
            float y1 = centreY + (float) Math.sin(angle) * inner;
            float x2 = centreX + (float) Math.cos(angle) * radius;
            float y2 = centreY + (float) Math.sin(angle) * radius;
            paint.setColor(darkMode ? Color.rgb(177, 184, 177) : Color.rgb(95, 98, 95));
            canvas.drawLine(x1, y1, x2, y2, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(13));
        paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        paint.setColor(darkMode ? Color.rgb(177, 184, 177) : Color.rgb(95, 98, 95));
        canvas.drawText("BAJO", centreX - radius * .72f, centreY - radius * .12f, paint);
        canvas.drawText("AGUDO", centreX + radius * .72f, centreY - radius * .12f, paint);

        if (hasPitch) {
            double angle = Math.toRadians(270 + cents * 1.4);
            float needle = radius - dp(27);
            float x = centreX + (float) Math.cos(angle) * needle;
            float y = centreY + (float) Math.sin(angle) * needle;
            paint.setStrokeWidth(dp(4));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(Math.abs(cents) <= TOLERANCE_CENTS
                    ? accentColor() : darkMode ? Color.rgb(226, 232, 226) : Color.rgb(52, 55, 52));
            canvas.drawLine(centreX, centreY, x, y, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(centreX, centreY, dp(8), paint);
        } else {
            paint.setColor(darkMode ? Color.rgb(119, 126, 119) : Color.rgb(150, 153, 150));
            canvas.drawCircle(centreX, centreY, dp(6), paint);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int sp(int value) { return Math.round(value * getResources().getDisplayMetrics().scaledDensity); }
    private int accentColor() { return getContext().getColor(darkMode ? R.color.accent_dark : R.color.accent); }
}
