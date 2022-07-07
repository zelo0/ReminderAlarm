package com.example.reminderalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.provider.MediaStore;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    public static final int NOTIFICATION_ID = 48;
    public static final String NOTIFICATION_CHANNEL_ID = "ALARM_NOTIFICATION_CHANNEL";
    public static final String NOTIFICATION_NAME = "alarm notification";

    private NotificationManager notificationManager;

    /* 알람 설정한 시간이 되면 알람 울리고 다시 다음날 알람 설정 */
    @Override
    public void onReceive(Context context, Intent intent) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /* notification 채널 생성 */
        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        notificationChannel.enableVibration(true);
        notificationChannel.setDescription("퍼펙트 알람 앱을 위한 알람 알림 채널입니다");
        /* notification 채널 등록 */
        notificationManager.createNotificationChannel(notificationChannel);


        /* notification 생성 */
        // 알람 시간이 되면 알람 울리는 화면으로 진입
        Intent alarmIntent = new Intent(context, AlarmRingActivity.class);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // activity를 시작하는 intent
        PendingIntent pendingAlarmIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        Notification alarmNotification = notificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("퍼펙트 데이")
                .setContentText("일어날 시간이에요. 오늘도 완벽한 하루 되세요")
                .setFullScreenIntent(pendingAlarmIntent, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true) // 알림 클릭 시 상단 바에서 사라자게 해줌
                .build();

        notificationManager.notify(NOTIFICATION_ID, alarmNotification);


        // alarm service 호출
        Intent serviceIntent = new Intent(context, AlarmService.class);
        context.startService(serviceIntent);
    }
}
