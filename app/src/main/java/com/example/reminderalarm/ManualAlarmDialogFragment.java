package com.example.reminderalarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.example.reminderalarm.databinding.FragmentFirstBinding;
import com.example.reminderalarm.databinding.ManualAlarmSettingDialogBinding;

import java.util.Calendar;


public class ManualAlarmDialogFragment extends DialogFragment {

    private ManualAlarmSettingDialogBinding binding;
    private TimePicker manualAlarmTimePicker;
    private TextView firstEventTimeTextView;
    private TextView preparedAlarmTimeTextView;
    private TextView firstEventNameTextView;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        // view 바인딩
        Bundle args = getArguments();

        binding = ManualAlarmSettingDialogBinding.inflate(LayoutInflater.from(getContext()));
        manualAlarmTimePicker = binding.manualAlarmTimePicker;
        firstEventTimeTextView = binding.firstEventTimeTextView;
        preparedAlarmTimeTextView = binding.preparedAlarmTimeTextView;
        firstEventNameTextView = binding.firstEventName;

        // arg로 넘어온 값 받기
        int firstEventHour = args.getInt("firstEventHour");
        int firstEventMinute = args.getInt("firstEventMinute");
        String firstEventName = args.getString("firstEventName");
        int preparedAlarmHour = args.getInt("preparedAlarmHour");
        int preparedAlarmMinute = args.getInt("preparedAlarmMinute");
        boolean isDailyAlarmSetting = args.getBoolean("isDailyAlarmSetting");

        // 타임피커 시간을 첫 이벤트 시간으로 초기화
        manualAlarmTimePicker.setHour(firstEventHour);
        manualAlarmTimePicker.setMinute(firstEventMinute);

        // 안내 텍스트 설정
        firstEventNameTextView.setText("'" + firstEventName + "'");
        firstEventTimeTextView.setText("첫 일정 : " + firstEventHour + " : " + firstEventMinute);
        preparedAlarmTimeTextView.setText("예정 알람 : " + preparedAlarmHour + " : " + preparedAlarmMinute);

        // SharedPreferences
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

        // 다이얼로그 설정
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        /* 매일 알람 시간 설정 도중 띄운 다이얼로그면 sharedPreference에 저장 필요 */
        builder.setView(binding.getRoot())
                // 설정 버튼 - 새로운 알람 시간으로 설정
                .setPositiveButton(R.string.set, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlarmUtil alarmUtil = new AlarmUtil(getContext().getApplicationContext());

                        // 다이얼로그의 타임피커 시간으로 알람 예약
                        AlarmTime manualAlarmTime = getManualAlarmTime();

                        Calendar calendarOfNextDailyAlarmTime = alarmUtil.getCalendarOfNextDailyAlarmTime(manualAlarmTime.getHour(), manualAlarmTime.getMinute());
                        alarmUtil.setNextAlarm(calendarOfNextDailyAlarmTime);

                        // 매일 알람 시간 설정 중이면 sharedPreference에 저장
                        if (isDailyAlarmSetting) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            // 설정 변경
                            editor.putInt(getString(R.string.dailyAlarmHour), manualAlarmTime.getHour());
                            editor.putInt(getString(R.string.dailyAlarmMinute), manualAlarmTime.getMinute());
                            // 디스크에 비동기로 write
                            editor.apply();
                        }
                    }
                })
                // 이벤트 무시하고 알람 설정
                .setNegativeButton(R.string.progress, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 예정 알람 시간으로 알람 예약
                        AlarmUtil alarmUtil = new AlarmUtil(getContext().getApplicationContext());
                        Calendar calendarOfNextDailyAlarmTime = alarmUtil.getCalendarOfNextDailyAlarmTime(preparedAlarmHour, preparedAlarmMinute);
                        alarmUtil.setNextAlarm(calendarOfNextDailyAlarmTime);

                        // 매일 알람 시간 설정 중이면 sharedPreference에 저장
                        if (isDailyAlarmSetting) {
                            SharedPreferences.Editor editor = sharedPref.edit();
                            // 설정 변경
                            editor.putInt(getString(R.string.dailyAlarmHour), calendarOfNextDailyAlarmTime.get(Calendar.HOUR_OF_DAY));
                            editor.putInt(getString(R.string.dailyAlarmMinute), calendarOfNextDailyAlarmTime.get(Calendar.MINUTE));
                            // 디스크에 비동기로 write
                            editor.apply();
                        }
                    }
                });

        return builder.create();
    }



    /* 수동 알람 시간 설정 타임피커의 시간 get */
    public AlarmTime getManualAlarmTime() {
        return new AlarmTime(manualAlarmTimePicker.getHour(), manualAlarmTimePicker.getMinute());
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // 다음 알람 텍스트 변경
        FirstFragment parentFragment = (FirstFragment) getParentFragment();
        parentFragment.updateNextAlarmText();
    }
}