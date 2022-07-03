package com.example.reminderalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.reminderalarm.databinding.ActivityAlarmRingBinding;
import com.example.reminderalarm.databinding.ActivityMainBinding;

public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding binding;
    private AlarmUtil alarmUtil;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        alarmUtil = new AlarmUtil(getApplicationContext());

        sharedPref = getPreferences(Context.MODE_PRIVATE);


        /* 알람 종료 버튼 클릭 시 서비스 종료, 다음 매일 알람 예약, 화면 종료 */
        Button alarmStopBtn = binding.alarmStopBtn;
        alarmStopBtn.setOnClickListener(v -> {
            // 알람 백그라운드 서비스 종료
            stopService(new Intent(this, AlarmService.class));

            // 매일 알람 시간의 다음 알람을 예약
            int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
            int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);
            alarmUtil.setNextAlarm(alarmUtil.getCalendarOfNextDailyAlarmTime(dailyAlarmHourPreference, dailyAlarmMinutePreference));

            // 화면 종료
            finish();
        });
    }
}