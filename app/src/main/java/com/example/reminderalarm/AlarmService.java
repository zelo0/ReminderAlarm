package com.example.reminderalarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class AlarmService extends Service {
    private MediaPlayer mediaPlayer;

    public AlarmService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        mediaPlayer = MediaPlayer.create(this, uri);
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
        super.onDestroy();
    }
}