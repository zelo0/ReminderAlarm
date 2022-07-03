package com.example.reminderalarm;

import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.reminderalarm.databinding.FragmentFirstBinding;

import java.util.Calendar;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private AlarmUtil alarmUtil;

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

        alarmUtil = new AlarmUtil(getContext().getApplicationContext());

        // canScheduleExactAlarms 권한이 취소되지 않았는지 체크
        alarmUtil.checkIfCanScheduleExactAlarms();


        /* sharedPreferences */
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        /* sharedPreferences 읽어서 초기 값 세팅 */
        int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
        int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);
        int nSleepHourPreference = sharedPref.getInt(getString(R.string.nSleepHour), 7);
        int nSleepMinutePreference = sharedPref.getInt(getString(R.string.nSleepMinute), 0);


        /* 알람 시간 보여주는 텍스트 설정 */
        // getNextAlarmClock() 사용해서 변경하자
        updateNextAlarmText();


        /* 매일 알람 시간 설정 */
        TimePicker dailyAlarmTimePicker = binding.dailyAlarmTimePicker;
        // 기존 설정 값 불러오기
        setTimeOfTimePicker(dailyAlarmHourPreference, dailyAlarmMinutePreference, dailyAlarmTimePicker);
        // 매일 알람시간 타임피커 리스너 등록
        dailyAlarmTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                // 새로 설정한 시간 저장
                saveChangedTimeInSharedPreference(hourOfDay, minute, sharedPref);

                /* 시스템에 매일 알람 예약 걸기 */

                // 다음 알람 시간을 가지는 캘린더 생성
                Calendar calendarOfNextAlarmTime = alarmUtil.getCalendarOfNextDailyAlarmTime(hourOfDay, minute);

                // 다음 알람 설정
                alarmUtil.setNextAlarm(calendarOfNextAlarmTime);
                // 현재 예약된 알람 시간 보여주기
                updateNextAlarmText();
            }
        });


        /* N시간 수면 numberPicker 시간, 분 범위 초기 설정, 리스너 등록 */

        /* 시간 */
        NumberPicker nSleepHourPicker = binding.nSleepHourPicker;
        // 범위 설정
        setRangeOfNumberPicker(nSleepHourPicker, 0, 12);
        // 기존 설정 값 불러오기
        loadInitValueInNumberPicker(nSleepHourPreference, nSleepHourPicker);
        //리스너 등록
        setChangedListenerOfNumberPickerWithPreference(sharedPref, nSleepHourPicker, R.string.nSleepHour);

        /* 분 */
        NumberPicker nSleepMinutePicker = binding.nSleepMinPicker;
        // 범위 설정
        setRangeOfNumberPicker(nSleepMinutePicker, 0, 59);
        // 기존 설정 값 불러오기
        loadInitValueInNumberPicker(nSleepMinutePreference, nSleepMinutePicker);
        // 리스너 등록
        setChangedListenerOfNumberPickerWithPreference(sharedPref, nSleepMinutePicker, R.string.nSleepMinute);


        // mSleep 버튼 클릭 리스너
        binding.nSleepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 현재 시점의 nSleep 시간, 분 찾기
                int nSleepHourPreference = sharedPref.getInt(getString(R.string.nSleepHour), 7);
                int nSleepMinutePreference = sharedPref.getInt(getString(R.string.nSleepMinute), 0);

                // 다음 알람 시간을 갖는 캘린더 설정
                Calendar nextAlarmCalendar = alarmUtil.getCalendarOfNextNSleepAlarmTime(nSleepHourPreference, nSleepMinutePreference);

                // 다음 알람 설정
                alarmUtil.setNextAlarm(nextAlarmCalendar);
                // 현재 예약된 알람 시간 보여주기
                updateNextAlarmText();
            }
        });


        /* 이벤트 찾기 세트 */
        // 첫 화면 진입 시 캘린더 가지는 계정 찾기
        MainActivity mainActivity = (MainActivity) getActivity();
        List<CalendarCoreInfo> calendarCoreInfoList = mainActivity.calendarQuery();
        // 캘린더 가져온 후에 이벤트 가져오기
        mainActivity.eventQuery(calendarCoreInfoList);
    }

    private void setChangedListenerOfNumberPickerWithPreference(SharedPreferences sharedPref, NumberPicker numberPicker, int preferenceKey) {
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                SharedPreferences.Editor editor = sharedPref.edit();
                // 설정 변경
                editor.putInt(getString(preferenceKey), newVal);
                // 디스크에 비동기로 write
                editor.apply();
            }
        });
    }

    private void loadInitValueInNumberPicker(int initValue, NumberPicker numberPicker) {
        numberPicker.setValue(initValue);
    }

    /* NumberPicker의 숫자 범위 설정 */
    private void setRangeOfNumberPicker(NumberPicker numberPicker, int minValue, int maxValue) {
        numberPicker.setMinValue(minValue);
        numberPicker.setMaxValue(maxValue);
    }


    private void saveChangedTimeInSharedPreference(int hourOfDay, int minute, SharedPreferences sharedPref) {
        SharedPreferences.Editor editor = sharedPref.edit();
        // 설정 변경
        editor.putInt(getString(R.string.dailyAlarmHour), hourOfDay);
        editor.putInt(getString(R.string.dailyAlarmMinute), minute);
        // 디스크에 비동기로 write
        editor.apply();
    }

    private void setTimeOfTimePicker(int dailyAlarmHourPreference, int dailyAlarmMinutePreference, TimePicker dailyAlarmTimePicker) {
        dailyAlarmTimePicker.setHour(dailyAlarmHourPreference);
        dailyAlarmTimePicker.setMinute(dailyAlarmMinutePreference);
    }

    private void updateNextAlarmText() {
        // 다음 알람 시간 보여주기
        TextView alarmInfoText = binding.alarmInfoText;
        StringBuilder sb = new StringBuilder();

        // 정확히 다음 알람 시간 get
        AlarmClockInfo nextAlarmClockInfo = alarmUtil.getNextAlarmClock();
        // 다음 알람이 없으면
        if (nextAlarmClockInfo == null) {
            sb.append("다음 알람이 없습니다.");
        } else {
            long triggerTimeInUTC = nextAlarmClockInfo.getTriggerTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(triggerTimeInUTC);

            sb.append("다음 알람은 ");
            sb.append(calendar.get(Calendar.HOUR));
            sb.append("시 ");
            sb.append(calendar.get(Calendar.MINUTE));
            sb.append("분에 울립니다");
        }

        alarmInfoText.setText(sb.toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}