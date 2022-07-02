package com.example.reminderalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.reminderalarm.databinding.ActivityAlarmRingBinding;
import com.example.reminderalarm.databinding.ActivityMainBinding;

public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        /* 알람 종료 버튼 클릭 시 서비스 종료 후 화면 종료 */
        Button alarmStopBtn = binding.alarmStopBtn;
        alarmStopBtn.setOnClickListener(v -> {
            // 알람 백그라운드 서비스 종료
            stopService(new Intent(this, AlarmService.class));
            // 화면 종료
            finish();
        });
    }
}