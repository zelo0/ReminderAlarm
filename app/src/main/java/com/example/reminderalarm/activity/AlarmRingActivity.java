package com.example.reminderalarm.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.reminderalarm.R;
import com.example.reminderalarm.adapter.EventAdapter;
import com.example.reminderalarm.data.EventCoreInfo;
import com.example.reminderalarm.databinding.ActivityAlarmRingBinding;
import com.example.reminderalarm.service.AlarmService;
import com.example.reminderalarm.util.WeatherApiManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AlarmRingActivity extends AppCompatActivity {

    private ActivityAlarmRingBinding binding;

    private EventAdapter eventAdapter;
    private List<EventCoreInfo> showingEventList = new ArrayList<>();

    private AlarmService mService;
    private boolean mBound = false;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AlarmService.LocalBinder binder = (AlarmService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // 오늘의 일정 띄워 주기
            List<EventCoreInfo> todayEventList = mService.getTodayEventList();

            for (EventCoreInfo todayEvent : todayEventList) {
                showingEventList.add(todayEvent);
            }
            eventAdapter.notifyDataSetChanged();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityAlarmRingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* 리사이클러뷰, 어댑터 연결 */
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventAdapter = new EventAdapter(showingEventList);
        recyclerView.setAdapter(eventAdapter);


        // 서비스와 바인드
        // 일정 받기
        Intent intent = new Intent(this, AlarmService.class);
        bindService(intent, mConnection, Context.BIND_IMPORTANT);


        /* 날씨 받기 */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AlarmService.UPDATE_EVENT_BROADCAST);
        intentFilter.addAction(AlarmService.UPDATE_WEATHER_BROADCAST);

        /* 브로드캐스트 리시버 */
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        /* 오늘의 일정 브로드캐스트 */
                        /*if (intent.getAction().equals(AlarmService.UPDATE_EVENT_BROADCAST)) {
                            Log.i("flag", "bradcast");
                            String todayEventJson = intent.getStringExtra(AlarmService.TODAY_EVENTS);
                            try {
                                JSONArray jsonArray = new JSONArray(todayEventJson);
                                System.out.println("jsonArray = " + jsonArray);
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    try {
                                        EventCoreInfo event = (EventCoreInfo) jsonArray.get(i);
                                        System.out.println("event = " + event);
                                        showingEventList.add(event);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            *//* 어댑터 변경 *//*
                            eventAdapter.notifyDataSetChanged();
                        }*/
                        /* 오늘의 날씨 브로드캐스트 리시버 */
                        if (intent.getAction().equals(AlarmService.UPDATE_WEATHER_BROADCAST)) {
                            String currentTemperature = intent.getStringExtra(AlarmService.CURRENT_TEMPERATURE);
                            String minTemperature = intent.getStringExtra(AlarmService.MIN_TEMPERATURE);
                            String maxTemperature = intent.getStringExtra(AlarmService.MAX_TEMPERATURE);
                            boolean willRain = intent.getBooleanExtra(AlarmService.WILL_RAIN, false);

                            /* 텍스트뷰 내용 변경 */
                            TextView currentTemperatureView = (TextView) findViewById(R.id.currentTemperature);
                            currentTemperatureView.setText(String.format("%s℃", currentTemperature));

                            TextView minTemperatureView = (TextView) findViewById(R.id.minTemperature);
                            minTemperatureView.setText(String.format("%s℃", minTemperature));

                            TextView maxTemperatureView = (TextView) findViewById(R.id.maxTemperature);
                            maxTemperatureView.setText(String.format("%s℃", maxTemperature));

                            View rainImageView =  findViewById(R.id.rainImage);
                            if (willRain) {
                                rainImageView.setVisibility(View.VISIBLE);
                            } else {
                                rainImageView.setVisibility(View.INVISIBLE);
                            }
                        }
                    }

                }, intentFilter
        );

        Log.i("flag", "receiver enrolled");

    }

    @Override
    protected void onStop() {
        super.onStop();

//        unbindService(mConnection);
    }


}