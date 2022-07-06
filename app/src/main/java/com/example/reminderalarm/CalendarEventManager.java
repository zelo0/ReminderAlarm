package com.example.reminderalarm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.metrics.Event;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;

import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class CalendarEventManager {

    /* 캘린더 계정 찾기 */
    // 캘린더 가진 계정  찾는 쿼리
    public static final String[] CALENDAR_PROJECTION = new String[]{
            CalendarContract.Calendars._ID,                           // 0
            CalendarContract.Calendars.ACCOUNT_NAME,                  // 1
            CalendarContract.Calendars.ACCOUNT_TYPE,                  // 2
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,         // 3
            CalendarContract.Calendars._SYNC_ID,                      // 4
            CalendarContract.Calendars.CALENDAR_TIME_ZONE             // 5
    };

    private static final int PROJECTION_CALENDAR_ID = 0;
    private static final int PROJECTION_ACCOUNT_NAME = 1;
    private static final int PROJECTION_ACCOUNT_TYPE = 2;
    private static final int PROJECTION_DISPLAY_NAME = 3;
    private static final int PROJECTION_SYNC_ID = 4;
    private static final int PROJECTION_TIME_ZONE = 5;


    /* 캘린더 이벤트 찾기 */
    // 이벤트 찾는 쿼리
    public static final String[] EVENT_PROJECTION = new String[]{
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.EVENT_TIMEZONE,
    };

    private static final int PROJECTION_EVENT_ID = 0;
    private static final int PROJECTION_EVENT_TITLE = 1;
    private static final int PROJECTION_EVENT_DESCRIPTION = 2;
    private static final int PROJECTION_EVENT_DTSTART = 3;
    private static final int PROJECTION_EVENT_DTEND = 4;
    private static final int PROJECTION_EVENT_LOCATION = 5;
    private static final int PROJECTION_EVENT_TIMEZONE = 6;

    // 쿼리의 where 절 - calendarId로 찾기
    private static final String EVENT_SELECTION_CLAUSE = CalendarContract.Events.CALENDAR_ID + "= ?";
    // 쿼리의 정렬 방식 - 일찍 해야 하는 일정부터
    private static final String EVENT_QUERY_ORDER = CalendarContract.Events.DTSTART + " ASC";

    /* 특정 시간 사이의 첫 이벤트 찾기 쿼리 */
    // 쿼리의 where 절 - calendarId로 찾기
    private static final String EVENT_BETWEEN_TIMES_SELECTION_CLAUSE =
            CalendarContract.Events.CALENDAR_ID + "= ? AND " + CalendarContract.Events.DTSTART + " BETWEEN ? AND ?";

    private Context context;
    private ContentResolver contentResolver;
    private Uri calendarUri = CalendarContract.Calendars.CONTENT_URI;
    private Uri eventUri = CalendarContract.Events.CONTENT_URI;

    // 각 계정별 이벤트가 담긴 핵심 캘린더
    private List<CalendarCoreInfo> validCalendarCoreInfoList = new ArrayList<>();

    public CalendarEventManager(Context applicationContext) {
        context = applicationContext;

        contentResolver = context.getContentResolver();
    }

    // 캘린더 가진 계정 찾는 메소드
    public List<CalendarCoreInfo> calendarQuery() {

        // 쿼리
        Cursor cursor = contentResolver.query(calendarUri, CALENDAR_PROJECTION, "", null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String calendarId = cursor.getString(PROJECTION_CALENDAR_ID);
                String accountName = cursor.getString(PROJECTION_ACCOUNT_NAME);
                String accountType = cursor.getString(PROJECTION_ACCOUNT_TYPE);
                String displayName = cursor.getString(PROJECTION_DISPLAY_NAME);
                String syncId = cursor.getString(PROJECTION_SYNC_ID);
                String timeZone = cursor.getString(PROJECTION_TIME_ZONE);

      /*          Log.i("calendar query", "calendar id: " + calendarId + ", account name: " + accountName
                        + ", account type: " + accountType + ", display name: " + displayName
                        + ", sync id: " + syncId + ", time zone: " + timeZone);*/

                if (accountName.equals(displayName)) {
                    validCalendarCoreInfoList.add(new CalendarCoreInfo(calendarId, accountName, accountType, timeZone));
                }
            }

            cursor.close();
        } else {
            Log.e("unexpected", "There was an error or result is empty");
        }
/*
        for (CalendarCoreInfo calendarCoreInfo : validCalendarCoreInfoList) {
            Log.i("core calendar", calendarCoreInfo.toString());
        }*/

        return validCalendarCoreInfoList;
    }


    // 이벤트 찾는 메소드
    public void eventQuery(List<CalendarCoreInfo> calendarCoreInfoList) {
        // 이벤트 쿼리 결과를 담는 set
        Set<EventCoreInfo> eventCoreInfoSet = new HashSet<>();

        // 오늘(지금) datetime
        ZonedDateTime now = ZonedDateTime.now(TimeZone.getDefault().toZoneId()); // 폰으로 확인 시 제대로 된 타임존
        System.out.println("now date = " + now);

        // 오늘 일정들만 담기
        for (CalendarCoreInfo calendarCoreInfo : calendarCoreInfoList) {
            String[] selectionArgs = new String[]{
                    calendarCoreInfo.getCalenderId()
            };

            Cursor cursor = contentResolver.query(eventUri, EVENT_PROJECTION, EVENT_SELECTION_CLAUSE, selectionArgs, EVENT_QUERY_ORDER);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String eventId = cursor.getString(PROJECTION_EVENT_ID);
                    String title = cursor.getString(PROJECTION_EVENT_TITLE);
                    String description = cursor.getString(PROJECTION_EVENT_DESCRIPTION);
                    String dtStart = cursor.getString(PROJECTION_EVENT_DTSTART);
                    String dtEnd = cursor.getString(PROJECTION_EVENT_DTEND);
                    String location = cursor.getString(PROJECTION_EVENT_LOCATION);
                    String timeZone = cursor.getString(PROJECTION_EVENT_TIMEZONE);

                    // 이벤트의 날짜
                    // timezone이 없는 이벤트 방어 코드
                    DateTimeZone dateTimeZone;
                    if (!timeZone.equals("null")) {
                        dateTimeZone = DateTimeZone.forID(timeZone);
                    } else {
                        dateTimeZone = DateTimeZone.forTimeZone(TimeZone.getDefault());
                    }

                    DateTime eventsDateTime = new DateTime(Long.parseLong(dtStart), dateTimeZone);
/*

                    Log.i("event query", "event id: " + eventId + ", title: " + title
                            + ", description: " + description + ", dtStart: " + dtStart
                            + ", dtEnd: " + dtEnd + ", location: " + location + ", timezone: " + timeZone);
*/

                    // 오늘의 일정만 담기
                    if (now.toLocalDate().toString().equals(eventsDateTime.toLocalDate().toString())) {
                        eventCoreInfoSet.add(new EventCoreInfo(eventId, title, description, dtStart, dtEnd, location));
                    }

                }
                cursor.close();
            } else {
                Log.e("unexpected", "There was an error or result is empty");
            }
        }

    }

    /*
        다음 알람 시간 이전에 그 날의 이벤트가 존재한다면 그 날의 첫번째 이벤트 리턴
        그렇지 않다면 null 리턴
        매개변수: 다음 알람 시간의 자정 시간, 다음 알람 시간
     */
    @Nullable
    public EventCoreInfo getFirstEventFromMidnightToNextAlarm(long midnightEpoch, long nextAlarmEpoch) {
        List<EventCoreInfo> eventListBetweenTimes = new ArrayList<>();

        // 캘린더 조회를 아직 하지 않았으면 조회하기
        if (validCalendarCoreInfoList.isEmpty()) {
            validCalendarCoreInfoList = calendarQuery();
        }

        // 특정 시간 사이의 이벤트
        for (CalendarCoreInfo calendarCoreInfo : validCalendarCoreInfoList) {
            /*
                calendarId
                검색할 시간 (부터)
                검새할 시간 (까지)
            */
            String[] selectionArgs = new String[]{
                    calendarCoreInfo.getCalenderId(),
                    Long.toString(midnightEpoch),
                    Long.toString(nextAlarmEpoch)
            };

            Cursor cursor = contentResolver.query(eventUri, EVENT_PROJECTION, EVENT_BETWEEN_TIMES_SELECTION_CLAUSE, selectionArgs, EVENT_QUERY_ORDER);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String eventId = cursor.getString(PROJECTION_EVENT_ID);
                    String title = cursor.getString(PROJECTION_EVENT_TITLE);
                    String description = cursor.getString(PROJECTION_EVENT_DESCRIPTION);
                    String dtStart = cursor.getString(PROJECTION_EVENT_DTSTART);
                    String dtEnd = cursor.getString(PROJECTION_EVENT_DTEND);
                    String location = cursor.getString(PROJECTION_EVENT_LOCATION);
                    String timeZone = cursor.getString(PROJECTION_EVENT_TIMEZONE);
/*

                    Log.i("event query", "event id: " + eventId + ", title: " + title
                            + ", description: " + description + ", dtStart: " + dtStart
                            + ", dtEnd: " + dtEnd + ", location: " + location + ", timezone: " + timeZone);
*/

                    eventListBetweenTimes.add(new EventCoreInfo(eventId, title, description, dtStart, dtEnd, location));

                }
                cursor.close();
            } else {
                Log.e("unexpected", "There was an error or result is empty");
            }
        }

        // 이벤트들을 시작 시간 기준으로 정렬
        Collections.sort(eventListBetweenTimes);


        // 가장 앞의 이벤트 리턴
        if (eventListBetweenTimes.isEmpty()) {
            return null;
        } else {
            return eventListBetweenTimes.get(0);
        }
    }


    /* 특정 시간 사이의 일정 전부 가져오기 */
    public List<EventCoreInfo> getAllEventsBetweenTimesAsSorted(long startEpoch, long endEpoch) {
        List<EventCoreInfo> eventListBetweenTimes = new ArrayList<>();

        // 캘린더 조회를 아직 하지 않았으면 조회하기
        if (validCalendarCoreInfoList.isEmpty()) {
            validCalendarCoreInfoList = calendarQuery();
        }

        // 특정 시간 사이의 이벤트
        for (CalendarCoreInfo calendarCoreInfo : validCalendarCoreInfoList) {
            /*
                calendarId
                검색할 시간 (부터)
                검새할 시간 (까지)
            */
            String[] selectionArgs = new String[]{
                    calendarCoreInfo.getCalenderId(),
                    Long.toString(startEpoch),
                    Long.toString(endEpoch)
            };

            Cursor cursor = contentResolver.query(eventUri, EVENT_PROJECTION, EVENT_BETWEEN_TIMES_SELECTION_CLAUSE, selectionArgs, EVENT_QUERY_ORDER);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String eventId = cursor.getString(PROJECTION_EVENT_ID);
                    String title = cursor.getString(PROJECTION_EVENT_TITLE);
                    String description = cursor.getString(PROJECTION_EVENT_DESCRIPTION);
                    String dtStart = cursor.getString(PROJECTION_EVENT_DTSTART);
                    String dtEnd = cursor.getString(PROJECTION_EVENT_DTEND);
                    String location = cursor.getString(PROJECTION_EVENT_LOCATION);
                    String timeZone = cursor.getString(PROJECTION_EVENT_TIMEZONE);

                /*    Log.i("event query", "event id: " + eventId + ", title: " + title
                            + ", description: " + description + ", dtStart: " + dtStart
                            + ", dtEnd: " + dtEnd + ", location: " + location + ", timezone: " + timeZone);
*/
                    eventListBetweenTimes.add(new EventCoreInfo(eventId, title, description, dtStart, dtEnd, location));

                }
                cursor.close();
            } else {
                Log.e("unexpected", "There was an error or result is empty");
            }
        }

        // 이벤트들을 시작 시간 기준으로 정렬
        Collections.sort(eventListBetweenTimes);

        return eventListBetweenTimes;
    }

}
