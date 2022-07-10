package com.example.reminderalarm.broadcastReceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.reminderalarm.activity.AlarmRingActivity;
import com.example.reminderalarm.service.AlarmService;
import com.example.reminderalarm.R;

public class AlarmReceiver extends BroadcastReceiver {

    /* 알람 설정한 시간이 되면 알람 울리고 다시 다음날 알람 설정 */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("broadcast", "received");

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                context.getPackageName() + ":offDozeMode");
        /* 1분 동안 CPU 작동시킴, 화면을 켤 수 있는 상태 - doze 모드 해제 */
        wakeLock.acquire(1*60*1000L /*1 minutes*/);


        /* activity 시작 */
        Intent activityIntent = new Intent(context.getApplicationContext(), AlarmRingActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(activityIntent);


        // alarm service 호출
        Intent serviceIntent = new Intent(context, AlarmService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        context.startService(serviceIntent);

    }


}
