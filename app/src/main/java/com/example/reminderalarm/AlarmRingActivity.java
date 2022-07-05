package com.example.reminderalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import com.example.reminderalarm.databinding.ActivityAlarmRingBinding;

import java.util.Calendar;

public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

    }
}