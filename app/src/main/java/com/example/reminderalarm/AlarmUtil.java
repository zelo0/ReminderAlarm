package com.example.reminderalarm;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;

public class AlarmUtil {
    private Context context;
    private AlarmManager alarmManager;
    private PendingIntent pendingAlarmIntent;
    private CalendarEventManager calendarEventManager;


    public AlarmUtil(Context applicationContext) {
        context = applicationContext;
        // 시스템의 알람 서비스와 바인딩
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 알람 브로캐스트용 pending 인텐트 생성
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.example.reminderalarm.alarm");
        // 기존의 인텐트가 있으면 제거 후 대체
        pendingAlarmIntent =
                PendingIntent.getBroadcast(context, AlarmReceiver.NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        calendarEventManager = new CalendarEventManager(context);
    }



    // 다음 알람 예약하기
    // setRepeating()은 API 19 이상에서 정확하게 원하는 시간에 작동하지 않는다
    // 다음 알람을 예약하기 전에 알람 울리는 날의 첫 일정이 알람 시간보다 빠른 지 확인
    public void setNextAlarm(Calendar calendarOfNextAlarmTime) {
        // 다음 알람 설정
        alarmManager.setAlarmClock(new AlarmClockInfo(calendarOfNextAlarmTime.getTimeInMillis(), pendingAlarmIntent), pendingAlarmIntent);
    }


    /* 단발성 매일 알람 시간 설정 로직 */
    // 새로 변경한 알람 시간이 현재 시간보다 나중이라면 오늘 해당 시간에 알람 예약
    // 새로 변경한 알람 시간이 현재 시간보다 이전이라면 다음날 해당 시간에 알람 예약
    @NonNull
    public Calendar getCalendarOfNextDailyAlarmTime(int hourOfDay, int minute) {

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

        // 현재 시간이 새 알람 시간보다 앞서거나 같으면 당일 알람 예약
        // (예) 현재 오전 1시인데 오전 9시로 알람 시간을 설정
        // equals()는 타임피커에서 현재 시간으로 변경 시 알람이 울리는 걸 방지
        if (calendarByNow.after(calendarByNextAlarmTime) || calendarByNow.equals(calendarByNextAlarmTime)) {
            calendarByNextAlarmTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return calendarByNextAlarmTime;
    }

    /* nSleep 시간 후의 캘린더 */
    @NonNull
    public Calendar getCalendarOfNextNSleepAlarmTime(int nSleepHourPreference, int nSleepMinutePreference) {
        Calendar nextAlarmCalendar = Calendar.getInstance();
        nextAlarmCalendar.setTimeInMillis(System.currentTimeMillis());
        nextAlarmCalendar.add(Calendar.HOUR_OF_DAY, nSleepHourPreference);
        nextAlarmCalendar.add(Calendar.MINUTE, nSleepMinutePreference);
        return nextAlarmCalendar;
    }


    public void checkIfCanScheduleExactAlarms() {
        // VERSION_CODES.S 이상일 때는 SCHEDULE_EXACT_ALARM이 있어야만 정확한 시간에 알람 가능 - 권한 설정돼있는 지 체크
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // 정확한 알람 설정 권한 있는 지 체크
            boolean hasAlarmPermission = false;

            hasAlarmPermission = alarmManager.canScheduleExactAlarms();
            // 해당 권한이 없으면 설정 화면으로 이동시킴
            if (!hasAlarmPermission) {
                Toast.makeText(context, "정확한 시간에 알람이 울리기 위해서 권한을 허가해주세요", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(intent);
            }
        }
    }


    public AlarmClockInfo getNextAlarmClock() {
        return alarmManager.getNextAlarmClock();
    }


    /* 알람 울리는 날의 첫 이벤트를 확인하면서 알람을 설정 */
    // 다음 알람 설정
    // 다음 알람을 예약하기 전에 알람 울리는 날의 첫 일정이 알람 시간보다 빠른 지 확인
    public void setNextAlarmCheckingFirstEvent(FragmentManager fragmentManager, Calendar nextAlarmCalendar, boolean isDailyAlarmSetting) {
        Log.i("check", "entered");
        // 알람 울리는 날의 자정 시간 갖는 캘린더
        Calendar midnightCalendar = Calendar.getInstance();
        midnightCalendar.setTimeInMillis(nextAlarmCalendar.getTimeInMillis());
        midnightCalendar.set(Calendar.AM_PM, Calendar.AM);
        midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
        midnightCalendar.set(Calendar.MINUTE, 0);
        midnightCalendar.set(Calendar.SECOND, 0);

        // 알람 울리는 날의 알람 시간 전에 시작하는 이벤트
        EventCoreInfo firstEventFromMidnightToNextAlarm = calendarEventManager.getFirstEventFromMidnightToNextAlarm(midnightCalendar.getTimeInMillis(), nextAlarmCalendar.getTimeInMillis());
        // 존재하지 않으면 이 시간대로 알람 예약
        // 존재하면 다이얼로그 띄우기
        if (firstEventFromMidnightToNextAlarm == null) {
            setNextAlarm(nextAlarmCalendar);
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
            // 매일 알람 시간 설정 중인지 여부
            bundle.putBoolean("isDailyAlarmSetting", isDailyAlarmSetting);


            // 다이얼로그 생성, arg 넘기기
            ManualAlarmDialogFragment manualAlarmDialogFragment = new ManualAlarmDialogFragment();
            manualAlarmDialogFragment.setArguments(bundle);

            // 다이얼로그 띄우기
            manualAlarmDialogFragment.show(fragmentManager, "manualAlarm");
        }
    }




}
