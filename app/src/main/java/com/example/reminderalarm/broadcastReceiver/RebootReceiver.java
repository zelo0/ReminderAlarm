package com.example.reminderalarm.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.reminderalarm.util.AlarmUtil;

import java.util.Calendar;

public class RebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        /* 알람 재설정 */
        AlarmUtil alarmUtil = new AlarmUtil(context.getApplicationContext());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(alarmUtil.getNextAlarmClock().getTriggerTime());

        alarmUtil.setNextAlarm(calendar);
    }
}
