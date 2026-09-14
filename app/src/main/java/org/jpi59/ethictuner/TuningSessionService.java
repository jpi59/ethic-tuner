/* Copyright (C) 2026 jpi59. SPDX-License-Identifier: GPL-3.0-or-later */
package org.jpi59.ethictuner;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/** Visible Android foreground-service indicator while an opted-in tuning session is active. */
public final class TuningSessionService extends Service {
    private static final String CHANNEL_ID = "tuning_session";
    private static final int NOTIFICATION_ID = 4101;

    public static void start(Context context) {
        Intent intent = new Intent(context, TuningSessionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent); else context.startService(intent);
    }

    public static void stop(Context context) { context.stopService(new Intent(context, TuningSessionService.class)); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startMicrophoneForeground();
        } else {
            startForeground(NOTIFICATION_ID, notification());
        }
        return START_NOT_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.R)
    private void startMicrophoneForeground() {
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        MainActivity.stopActiveTuningFromService();
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private Notification notification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.tuning_notification_channel), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.tuning_notification_description));
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_ethic_tuner)
                .setContentTitle(getString(R.string.tuning_notification_title))
                .setContentText(getString(R.string.tuning_notification_text))
                .setContentIntent(content)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }
}
