package com.example.reminderalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.AlarmManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.reminderalarm.databinding.FragmentFirstBinding;

import java.util.Calendar;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 삭제 예정
        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                // 캘린더 가지는 계정 찾는 쿼리
                List<CalendarCoreInfo> calendarCoreInfos = ((MainActivity) getActivity()).calendarQuery();

            }
        });


        // 시스템의 알람 서비스와 바인딩
        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);


        // VERSION_CODES.S 이상일 때는 SCHEDULE_EXACT_ALARM이 있어야만 정확한 시간에 알람 가능 - 권한 설정돼있는 지 체크
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // 정확한 알람 설정 권한 있는 지 체크
            boolean hasAlarmPermission = false;

            hasAlarmPermission = alarmManager.canScheduleExactAlarms();
            // 해당 권한이 없으면 설정 화면으로 이동시킴
            if (!hasAlarmPermission) {
                Toast.makeText(getContext(), "정확한 시간에 알람이 울리기 위해서 권한을 허가해주세요", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        /* sharedPreferences */
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        /* sharedPreferences 읽어서 초기 값 세팅 */
        int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
        int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);
        int nSleepHourPreference = sharedPref.getInt(getString(R.string.nSleepHour), 7);
        int nSleepMinutePreference = sharedPref.getInt(getString(R.string.nSleepMinute), 0);


        /* 알람 시간 보여주는 텍스트 설정 */
        // getNextAlarmClock() 사용해서 변경하자
        setDailyAlarmText(dailyAlarmHourPreference, dailyAlarmMinutePreference);


        /* 매일 알람 시간 설정 */
        TimePicker dailyAlarmTimePicker = binding.dailyAlarmTimePicker;
        // 기존 설정 값 불러오기
        dailyAlarmTimePicker.setHour(dailyAlarmHourPreference);
        dailyAlarmTimePicker.setMinute(dailyAlarmMinutePreference);
        // 리스너 등록
        dailyAlarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                SharedPreferences.Editor editor = sharedPref.edit();
                // 설정 변경
                editor.putInt(getString(R.string.dailyAlarmHour), hourOfDay);
                editor.putInt(getString(R.string.dailyAlarmMinute), minute);
                // 디스크에 비동기로 write
                editor.apply();
                // 가장 위에 있는 알람 시간 알려주는 텍스트에도 변경분 반영
                setDailyAlarmText(hourOfDay, minute);


                /* 시스템에 매일 알람 예약 걸기 */

                // 알람 intent
                Intent intent = new Intent(getContext(), AlarmReceiver.class);
                intent.setAction("com.example.reminderalarm.alarm");
                // 기존의 인텐트가 있으면 제거 후 대체
                alarmIntent = PendingIntent.getBroadcast(getContext(), AlarmReceiver.NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);


                /* 단발성 알람 시간 설정 로직 */
                // 새로 변경한 알람 시간이 현재 시간보다 나중이라면 오늘 해당 시간에 알람 예약
                // 새로 변경한 알람 시간이 현재 시간보다 이전이라면 다음날 해당 시간에 알람 예약

                // 현재 시간
                Calendar calendarByNow = Calendar.getInstance(); // 기본 타임존에 먖춰서

                // 오늘의 (변경된 시간, 변경된 분)으로 설정된 epoch로부터의 시간(캘린더)
                Calendar calendarByNextAlarmTime = Calendar.getInstance();
                calendarByNextAlarmTime.setTimeInMillis(System.currentTimeMillis());
                calendarByNextAlarmTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendarByNextAlarmTime.set(Calendar.MINUTE, minute);

                /* 현재 시간(캘린더)과 오늘의 변경된 알람 시간(캘린더) 비교 */
                // 현재 시간이 새 알람 시간보다 나중이면 다음날 알람 예약
                // (예) 현재 오전 10시인데 오전 8시로 알람 시간을 설정

                // 현재 시간이 새 알람 시간보다 앞서면 앞서면 당일 알람 예약
                // (예) 현재 오전 1시인데 오전 9시로 알람 시간을 설정
                if (calendarByNow.after(calendarByNextAlarmTime)) {
                    calendarByNextAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
                }

                // 다음 알람 설정
                // setRepeating()은 API 19 이상에서 정확하게 원하는 시간에 작동하지 않는다
                // setExact()로 정확히 알람 울리고 알람 울릴 때마다 다시 알람 예약 필요
                alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(calendarByNextAlarmTime.getTimeInMillis(), alarmIntent), alarmIntent);
            }
        });


        /* N시간 수면 numberPicker 시간, 분 범위 설정 */
        /* 시간 */
        NumberPicker nSleepHourPicker = binding.nSleepHourPicker;
        // 범위 설정
        nSleepHourPicker.setMinValue(0);
        nSleepHourPicker.setMaxValue(12);
        // 기존 설정 값 불러오기
        nSleepHourPicker.setValue(nSleepHourPreference);
        //리스너 등록
        nSleepHourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                SharedPreferences.Editor editor = sharedPref.edit();
                // 설정 변경
                editor.putInt(getString(R.string.nSleepHour), newVal);
                // 디스크에 비동기로 write
                editor.apply();
            }
        });

        /* 분 */
        NumberPicker nSleepMinutePicker = binding.nSleepMinPicker;
        // 범위 설정
        nSleepMinutePicker.setMinValue(0);
        nSleepMinutePicker.setMaxValue(59);
        // 기존 설정 값 불러오기
        nSleepMinutePicker.setValue(nSleepMinutePreference);
        // 리스너 등록
        nSleepMinutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                SharedPreferences.Editor editor = sharedPref.edit();
                // 설정 변경
                editor.putInt(getString(R.string.nSleepMinute), newVal);
                // 디스크에 비동기로 write
                editor.apply();
            }
        });


        /* 이벤트 찾기 세트 */
        // 첫 화면 진입 시 캘린더 가지는 계정 찾기
        MainActivity mainActivity = (MainActivity) getActivity();
        List<CalendarCoreInfo> calendarCoreInfoList = mainActivity.calendarQuery();
        // 캘린더 가져온 후에 이벤트 가져오기
        mainActivity.eventQuery(calendarCoreInfoList);
    }

    private void setDailyAlarmText(int dailyAlarmHourPreference, int dailyAlarmMinutePreference) {
        TextView alarmInfoText = binding.alarmInfoText;

        StringBuilder sb = new StringBuilder();
        sb.append("매일 ");
        sb.append(dailyAlarmHourPreference);
        sb.append("시 ");
        sb.append(dailyAlarmMinutePreference);
        sb.append("분에 알람이 울립니다");

        alarmInfoText.setText(sb.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}