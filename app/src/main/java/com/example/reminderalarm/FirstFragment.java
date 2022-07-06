package com.example.reminderalarm;

import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.reminderalarm.databinding.FragmentFirstBinding;

import java.util.Calendar;
import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private AlarmUtil alarmUtil;
    private CalendarEventManager calendarEventManager;

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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());

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
                Calendar nextAlarmCalendar = alarmUtil.getCalendarOfNextDailyAlarmTime(hourOfDay, minute);

                alarmUtil.setNextAlarmCheckingFirstEvent(getChildFragmentManager(), nextAlarmCalendar, true);

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

                alarmUtil.setNextAlarmCheckingFirstEvent(getChildFragmentManager(), nextAlarmCalendar, false);

                // 알람 예약이 끝나면 현재 예약된 알람 시간 보여주기
                updateNextAlarmText();
            }
        });

        calendarEventManager = new CalendarEventManager(getContext().getApplicationContext());

        /* 이벤트 찾기 세트 */
        // 첫 화면 진입 시 캘린더 가지는 계정 찾기
        MainActivity mainActivity = (MainActivity) getActivity();
        List<CalendarCoreInfo> calendarCoreInfoList = calendarEventManager.calendarQuery();
        // 캘린더 가져온 후에 이벤트 가져오기
        calendarEventManager.eventQuery(calendarCoreInfoList);
    }

    /* 알람 울리는 날의 첫 이벤트를 확인하면서 알람을 설정 */
    // 다음 알람 설정
    // 다음 알람을 예약하기 전에 알람 울리는 날의 첫 일정이 알람 시간보다 빠른 지 확인
    private void setNextAlarmCheckingFirstEvent(Calendar nextAlarmCalendar) {

        // 알람 울리는 날의 자정 시간 갖는 캘린더
        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.setTimeInMillis(nextAlarmCalendar.getTimeInMillis());
        midnightCalendar.set(Calendar.AM_PM, Calendar.AM);
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);

        // 알람 울리는 날의 알람 시간 전에 시작하는 이벤트
        EventCoreInfo firstEventFromMidnightToNextAlarm = calendarEventManager.getFirstEventFromMidnightToNextAlarm(midnightCalendar.getTimeInMillis(), nextAlarmCalendar.getTimeInMillis());
        System.out.println("firstEventFromMidnightToNextAlarm = " + firstEventFromMidnightToNextAlarm);
        // 존재하지 않으면 이 시간대로 알람 예약
        // 존재하면 다이얼로그 띄우기
        if (firstEventFromMidnightToNextAlarm == null) {
            alarmUtil.setNextAlarm(nextAlarmCalendar);
        } else {
            /* 다이얼로그 띄우고 사용자에게 물은 후 다음 알람 예약 */

            // 첫 이벤트 시간을 갖는 캘린더
            Calendar firstEventTimeCalendar = Calendar.getInstance();
            firstEventTimeCalendar.setTimeInMillis(Long.parseLong(firstEventFromMidnightToNextAlarm.getDtStart()));

            /* 첫 이벤트 time, 원래 예약하려던 시간 을 다이얼로그에 전달 */
            Bundle bundle = new Bundle();
            // 첫 이벤트 time
            bundle.putInt("firstEventHour", firstEventTimeCalendar.get(Calendar.HOUR_OF_DAY));
            bundle.putInt("firstEventMinute", firstEventTimeCalendar.get(Calendar.MINUTE));
            // 첫 이벤트 이름
            bundle.putString("firstEventName", firstEventFromMidnightToNextAlarm.getTitle());
            // 원래 예약하려던 알람 시간
            bundle.putInt("preparedAlarmHour", nextAlarmCalendar.get(Calendar.HOUR_OF_DAY));
            bundle.putInt("preparedAlarmMinute", nextAlarmCalendar.get(Calendar.MINUTE));


            // 다이얼로그 생성, arg 넘기기
            ManualAlarmDialogFragment manualAlarmDialogFragment = new ManualAlarmDialogFragment();
            manualAlarmDialogFragment.setArguments(bundle);

            // 다이얼로그 띄우기
            manualAlarmDialogFragment.show(getChildFragmentManager(), "manualAlarm");
        }
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

    public void updateNextAlarmText() {
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
            sb.append(calendar.get(Calendar.HOUR_OF_DAY));
            sb.append("시 ");
            sb.append(calendar.get(Calendar.MINUTE));
            sb.append("분에 울립니다");
        }

        alarmInfoText.setText(sb.toString());
    }

/*
    // 해당 시간, 분의 다음 알람을 예약해주고
    // 여러 메소드 묶어서 수행
    private void setNextAlarmThenUpdateAlarmTextView(int alarmHour, int alarmMinute, boolean canIgnoreEventBeforeAlarm) {
        Calendar calendarOfNextDailyAlarmTime = alarmUtil.getCalendarOfNextDailyAlarmTime(alarmHour, alarmMinute);
        alarmUtil.setNextAlarm(calendarOfNextDailyAlarmTime, true);

        // 다음 알람 설정
        try {
            alarmUtil.setNextAlarm(calendarOfNextAlarmTime, false);
        } catch (AlarmUtil.HasEventBeforeAlarmException e) {
            // 다이얼로그 띄우고 사용자에게 묻기

        }

        // 현재 예약된 알람 시간 보여주기
        updateNextAlarmText();
    }*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }


}