package com.example.reminderalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.reminderalarm.databinding.FragmentFirstBinding;

import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

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

        /* sharedPreferences */
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        /* sharedPreferences 읽어서 초기 값 세팅 */
        int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
        int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);
        int nSleepHourPreference = sharedPref.getInt(getString(R.string.nSleepHour), 7);
        int nSleepMinutePreference = sharedPref.getInt(getString(R.string.nSleepMinute), 0);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}