package com.example.smartpillbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class ReminderBroadcast extends BroadcastReceiver {

    public static String TEXT = "Text";



    @Override
    public void onReceive(Context context, Intent intent) {

        String medication = intent.getStringExtra("Med Name");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "channel1")
                .setSmallIcon(R.drawable.ic_baseline_medical_services_24)
                .setContentTitle("Medication Reminder")
                .setContentText("Take " + medication)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(m, builder.build());
    }
}
