package com.example.reminderalarm.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.reminderalarm.service.AlarmService;
import com.example.reminderalarm.util.AlarmUtil;
import com.example.reminderalarm.R;
import com.example.reminderalarm.databinding.FragmentAlarmRingBinding;

import java.util.Calendar;


public class AlarmRingFragment extends Fragment {

    private FragmentAlarmRingBinding binding;
    private AlarmUtil alarmUtil;
    private SharedPreferences sharedPref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAlarmRingBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        alarmUtil = new AlarmUtil(getContext().getApplicationContext());

        alarmUtil.checkIfCanScheduleExactAlarms();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        /* 알람 종료 버튼 클릭 시 서비스 종료, 다음 매일 알람 예약, 화면 종료 */
        Button alarmStopBtn = binding.alarmStopBtn;
        alarmStopBtn.setOnClickListener(v -> {
            // 포그라운드 서비스 -> 백그라운드 서비스

            // 알람 백그라운드 서비스 종료
            getActivity().stopService(new Intent(getContext().getApplicationContext(), AlarmService.class));

            // 매일 알람 시간의 다음 알람을 예약
            int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
            int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);

            System.out.println("dailyAlarmHourPreference = " + dailyAlarmHourPreference);
            System.out.println("dailyAlarmMinutePreference = " + dailyAlarmMinutePreference);

            Calendar nextAlarmCalendar = alarmUtil.getCalendarOfNextDailyAlarmTime(dailyAlarmHourPreference, dailyAlarmMinutePreference);

            boolean isNotShowingDialog
                    = alarmUtil.setNextAlarmCheckingFirstEvent(getChildFragmentManager(), nextAlarmCalendar, false);

            Log.i("flag", "here after stop service");
            // 다이얼로그 없으면 바로 화면 종료
            // 다이얼로그 있으면 다이얼로그의 onDetach() 때 activity 종료
            if (isNotShowingDialog) {
                getActivity().finish();
            }
        });


    }
}