package com.example.reminderalarm;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.media.metrics.Event;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.reminderalarm.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;





    // 여러 퍼미션 요청하는 함수
    private ActivityResultLauncher<String[]> requestMultiplePermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    for (String s : result.keySet()) {
                        Log.i("requested permission", s);
                    }
                    requestMultiplePermissions.unregister();
                }
            }
    );

    // 런타임 때 퍼미션 확인, 요청하는 함수
    private void checkPermissions(int callbackId, String... permissionsId) {
        boolean permissions = true;

        for (String p : permissionsId) {
            permissions = permissions && ContextCompat.checkSelfPermission(this, p) == PERMISSION_GRANTED;
        }

        if (!permissions) {
            requestMultiplePermissions.launch(permissionsId);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        // 런타임 권한 요청
        final int callbackId = 42;
        checkPermissions(callbackId, Manifest.permission.READ_CALENDAR);

    }

    /* 캘린더 계정 찾기 */
    // 캘린더 가진 계정  찾는 쿼리
    public static final String[] CALENDAR_PROJECTION = new String[]{
            Calendars._ID,                           // 0
            Calendars.ACCOUNT_NAME,                  // 1
            Calendars.ACCOUNT_TYPE,                  // 2
            Calendars.CALENDAR_DISPLAY_NAME,         // 3
            Calendars._SYNC_ID,                      // 4
            Calendars.CALENDAR_TIME_ZONE             // 5
    };

    private static final int PROJECTION_CALENDAR_ID = 0;
    private static final int PROJECTION_ACCOUNT_NAME = 1;
    private static final int PROJECTION_ACCOUNT_TYPE = 2;
    private static final int PROJECTION_DISPLAY_NAME = 3;
    private static final int PROJECTION_SYNC_ID = 4;
    private static final int PROJECTION_TIME_ZONE = 5;

    // 캘린더 가진 계정 찾는 메소드
    public List<CalendarCoreInfo> calendarQuery() {
        ContentResolver cr = getContentResolver();
        Uri uri = Calendars.CONTENT_URI;

        // 각 계정별 이벤트가 담긴 핵심 캘린더를 추출
        List<CalendarCoreInfo> calendarCoreInfoPerAccount = new ArrayList<>();

        // Submit the query and get a Cursor object back.
        Cursor cursor = cr.query(uri, CALENDAR_PROJECTION, "", null, null);
        Log.i("checkpoint", "requested to content provider to get calendars");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String calendarId = cursor.getString(PROJECTION_CALENDAR_ID);
                String accountName = cursor.getString(PROJECTION_ACCOUNT_NAME);
                String accountType = cursor.getString(PROJECTION_ACCOUNT_TYPE);
                String displayName = cursor.getString(PROJECTION_DISPLAY_NAME);
                String syncId = cursor.getString(PROJECTION_SYNC_ID);
                String timeZone = cursor.getString(PROJECTION_TIME_ZONE);

                Log.i("calendar query", "calendar id: " + calendarId + ", account name: " + accountName
                        + ", account type: " + accountType + ", display name: " + displayName
                        + ", sync id: " + syncId + ", time zone: " + timeZone);

                if (accountName.equals(displayName)) {
                    calendarCoreInfoPerAccount.add(new CalendarCoreInfo(calendarId, accountName, accountType, timeZone));
                }
            }

            cursor.close();
        } else {
            Log.e("unexpected", "There was an error or result is empty");
        }

        for (CalendarCoreInfo calendarCoreInfo : calendarCoreInfoPerAccount) {
            Log.i("core calendar", calendarCoreInfo.toString());
        }

        return calendarCoreInfoPerAccount;
    }


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

    // 이벤트 찾는 메소드
    public void eventQuery(List<CalendarCoreInfo> calendarCoreInfoList) {
        ContentResolver cr = getContentResolver();
        Uri uri = CalendarContract.Events.CONTENT_URI;

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

            Cursor cursor = cr.query(uri, EVENT_PROJECTION, EVENT_SELECTION_CLAUSE, selectionArgs, EVENT_QUERY_ORDER);
            Log.i("checkpoint", "requested to content provider whose calender's id is " + calendarCoreInfo.getCalenderId());
            Log.i("checkpoint", "requested to content provider to get events destined to do today");
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
                    if (! timeZone.equals("null")) {
                        dateTimeZone = DateTimeZone.forID(timeZone);
                    } else {
                        dateTimeZone = DateTimeZone.forTimeZone(TimeZone.getDefault());
                    }

                    DateTime eventsDateTime = new DateTime(Long.parseLong(dtStart), dateTimeZone);

                    Log.i("event query", "event id: " + eventId + ", title: " + title
                            + ", description: " + description + ", dtStart: " + dtStart
                            + ", dtEnd: " + dtEnd + ", location: " + location + ", timezone: " + timeZone);

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}