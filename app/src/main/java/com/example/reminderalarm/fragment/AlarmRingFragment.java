package com.example.reminderalarm.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.reminderalarm.adapter.EventAdapter;
import com.example.reminderalarm.data.EventCoreInfo;
import com.example.reminderalarm.service.AlarmService;
import com.example.reminderalarm.util.AlarmUtil;
import com.example.reminderalarm.R;
import com.example.reminderalarm.databinding.FragmentAlarmRingBinding;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


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


        /* ?????? ?????? ?????? ?????? ??? ????????? ??????, ?????? ?????? ?????? ??????, ?????? ?????? */
        Button alarmStopBtn = binding.alarmStopBtn;
        alarmStopBtn.setOnClickListener(v -> {
            // ??????????????? ????????? -> ??????????????? ?????????

            // ?????? ??????????????? ????????? ??????
            getActivity().stopService(new Intent(getContext().getApplicationContext(), AlarmService.class));

            // ?????? ?????? ????????? ?????? ????????? ??????
            int dailyAlarmHourPreference = sharedPref.getInt(getString(R.string.dailyAlarmHour), 7);
            int dailyAlarmMinutePreference = sharedPref.getInt(getString(R.string.dailyAlarmMinute), 0);

            System.out.println("dailyAlarmHourPreference = " + dailyAlarmHourPreference);
            System.out.println("dailyAlarmMinutePreference = " + dailyAlarmMinutePreference);

            Calendar nextAlarmCalendar = alarmUtil.getCalendarOfNextDailyAlarmTime(dailyAlarmHourPreference, dailyAlarmMinutePreference);

            boolean isNotShowingDialog
                    = alarmUtil.setNextAlarmCheckingFirstEvent(getChildFragmentManager(), nextAlarmCalendar, false);

            Log.i("flag", "here after stop service");


            // ??????????????? ????????? ?????? ?????? ??????
            // ??????????????? ????????? ?????????????????? onDetach() ??? activity ??????
            if (isNotShowingDialog) {
                getActivity().finish();
            }
        });



    }
}