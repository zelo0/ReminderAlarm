package com.example.reminderalarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class AlarmService extends Service {

    private MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 알람 울릴 때 사용할 사운드
        Uri bellUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        // mediaPlayer 생성
        mediaPlayer = MediaPlayer.create(this, bellUri);

        // 재생
        mediaPlayer.setVolume(50,50);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Toast.makeText(this, "ring ~ ring ~", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mediaPlayer.stop();
        mediaPlayer.release();
        Toast.makeText(this, "알람을 종료했습니다.", Toast.LENGTH_SHORT);
        super.onDestroy();
    }
}