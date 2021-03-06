package com.example.reminderalarm.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.metrics.Event;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.example.reminderalarm.R;
import com.example.reminderalarm.activity.AlarmRingActivity;
import com.example.reminderalarm.util.CalendarEventManager;
import com.example.reminderalarm.data.EventCoreInfo;
import com.example.reminderalarm.util.WeatherApiManager;
import com.example.reminderalarm.util.WeatherApiManager.WeatherResponse;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AlarmService extends Service implements TextToSpeech.OnInitListener {
    public static final String UPDATE_EVENT_BROADCAST = AlarmService.class.getName() + ".UPDATE_EVENT_BROADCAST";
    public static final String UPDATE_WEATHER_BROADCAST = AlarmService.class.getName() + ".UPDATE_WEATHER_BROADCAST";
    public static final String TODAY_EVENTS = "TODAY_EVENTS";
    public static final String CURRENT_TEMPERATURE = "CURRENT_TEMPERATURE";
    public static final String MIN_TEMPERATURE = "MIN_TEMPERATURE";
    public static final String MAX_TEMPERATURE = "MAX_TEMPERATURE";
    public static final String WILL_RAIN = "WILL_RAIN";

    private NotificationManager notificationManager;
    private MediaPlayer mediaPlayer;
    private List<EventCoreInfo> todayEventList = null;
    private WeatherResponse weatherResponse = null;

    public static final int NOTIFICATION_ID = 48;
    public static final String NOTIFICATION_CHANNEL_ID = "ALARM_NOTIFICATION_CHANNEL";
    public static final String NOTIFICATION_NAME = "alarm notification";

    /* TTS */
    private TextToSpeech textToSpeech;
    // 말할 내용
    private StringBuffer whatToSay = new StringBuffer();
    private static final String ALARM_SPEECH_ID = "ALARM_SPEECH_ID";

    private IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public AlarmService getService() {
            return AlarmService.this;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /* 알림 띄우기 */
        putAlarmNotification(getApplicationContext());

        // 알람 울릴 때 사용할 사운드
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);

        Uri alarmSoundUri = Uri.parse(sharedPreferences.getString(getString(R.string.KEY_ALARM_SOUND),
                defaultRingtoneUri.toString() ));

        // mediaPlayer 생성
        mediaPlayer = MediaPlayer.create(this, alarmSoundUri);

        // 재생
        float soundVolume = sharedPreferences.getInt(getString(R.string.KEY_SOUND_VOLUME), 50) / (float)100;

        mediaPlayer.setVolume(soundVolume, soundVolume);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        /* 오늘의 일정 받아오기 */
        fetchTodayEvents();

        /* TTS 생성 -> 준비되면 speak */
        textToSpeech = new TextToSpeech(AlarmService.this, this);


        return super.onStartCommand(intent, flags, startId);
    }

    public List<EventCoreInfo> getTodayEventList() {
        return todayEventList;
    }

    public WeatherResponse getTodayWeather() {
        return weatherResponse;
    }



    private void sendTodayWeatherByBroadcast() {
        Intent updateWeatherIntent = new Intent(UPDATE_WEATHER_BROADCAST);
        updateWeatherIntent.putExtra(CURRENT_TEMPERATURE, weatherResponse.getCurrentTemperature());
        updateWeatherIntent.putExtra(MIN_TEMPERATURE, weatherResponse.getMinTemperature());
        updateWeatherIntent.putExtra(MAX_TEMPERATURE, weatherResponse.getMaxTemperature());
        updateWeatherIntent.putExtra(WILL_RAIN, weatherResponse.getWillRain());
        LocalBroadcastManager.getInstance(this).sendBroadcast(updateWeatherIntent);
    }


    private void putAlarmNotification(Context context) {
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /* notification 채널 생성 */
        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );

        notificationChannel.enableVibration(true);
        notificationChannel.setDescription("퍼펙트 알람 앱을 위한 알람 알림 채널입니다");
        /* notification 채널 등록 */
        notificationManager.createNotificationChannel(notificationChannel);


        /* notification 생성 */
        // 알람 시간이 되면 알람 울리는 화면으로 진입
        Intent alarmIntent = new Intent(context, AlarmRingActivity.class);

        // activity를 시작하는 intent
        PendingIntent pendingAlarmIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        Notification alarmNotification = notificationBuilder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("퍼펙트 데이")
                .setContentText("일어날 시간이에요. 오늘도 완벽한 하루 되세요")
                .setFullScreenIntent(pendingAlarmIntent, true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true) // 알림 클릭 시 상단 바에서 사라자게 해줌
                .build();

        notificationManager.notify(NOTIFICATION_ID, alarmNotification);


        startForeground(NOTIFICATION_ID, alarmNotification);

    }

    /* 날씨 정보 얻기 */
    private void fetchWeatherInformation() {
        System.out.println("weather request");

        /* 요구하는 패턴의 문자열로 변환 */
        Date date = new Date();
        Date yesterday = new Date(date.getTime() - 24 * 60 * 60 * 1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String yesterdayDateString = dateFormat.format(yesterday);

        SimpleDateFormat hourFormat = new SimpleDateFormat("HH00");
        String currentHourString = hourFormat.format(date);

        /* 위도, 경도 */
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 위치 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "현재 위치의 날씨를 제공받기 위해서는 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show();
   /*         Intent permissionIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplicationContext().startActivity(permissionIntent);*/
//            return new String();
        }

        // gps 위치 -> 없으면 네트워크 위치
        // gps로 위치 얻으려면 FINE_LOCATION 권한이 필요하다
        Location lastKnownLocationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


        if (lastKnownLocationByNetwork != null) {
            /* 날씨 api에 정보 요청 */
            WeatherApiManager weatherApiManager = new WeatherApiManager(getApplicationContext(), yesterdayDateString,
                    currentHourString, lastKnownLocationByNetwork.getLatitude(), lastKnownLocationByNetwork.getLongitude());
            try {
                weatherApiManager.execute().get();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            weatherResponse = weatherApiManager.getWeatherResponse();

           /* weatherResponse = weatherApiManager.requestWeather(todayDateString, currentHourString,
                    lastKnownLocationByNetwork.getLatitude(), lastKnownLocationByNetwork.getLongitude());*/

        } else {
            // 위치 정보를 못 가져온 상황
//            Toast.makeText(getApplicationContext(), "위치 정보를 가져오지 못 했습니다", Toast.LENGTH_SHORT).show();
        }




    }

    private String getWeatherTextToSpeak() {
        StringBuilder resultBuilder = new StringBuilder();

        /* 말할 날씨 정보 생성 */
        if (weatherResponse != null) {
            resultBuilder.append("현재 기온은 ");
            resultBuilder.append(weatherResponse.getCurrentTemperature());
            resultBuilder.append("도입니다. ");
            resultBuilder.append("오늘의 최저 기온은 ");
            resultBuilder.append(weatherResponse.getMinTemperature());
            resultBuilder.append("도, ");
            resultBuilder.append("최고 기온은 ");
            resultBuilder.append(weatherResponse.getMaxTemperature());
            resultBuilder.append("도입니다. ");
            if (weatherResponse.getWillRain()) {
                resultBuilder.append("오늘은 특정 시간에 비가 올 확률이 ");
                resultBuilder.append(WeatherApiManager.RAIN_DROP_BASE_PERCENT);
                resultBuilder.append("퍼센트 이상이니 우산을 챙기는 게 좋아요.");
            }
        }

        return "                                  " + resultBuilder.toString();
    }

    private void fetchTodayEvents() {
        /* 오늘의 일정 받아오기 */
        CalendarEventManager calendarEventManager = new CalendarEventManager(getApplicationContext());

        Calendar startOFDayCalendar = Calendar.getInstance();
        startOFDayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startOFDayCalendar.set(Calendar.MINUTE, 0);
        startOFDayCalendar.set(Calendar.SECOND, 0);
        startOFDayCalendar.set(Calendar.MILLISECOND, 0);

        Calendar endOfDayCalendar = Calendar.getInstance();
        endOfDayCalendar.set(Calendar.HOUR_OF_DAY, 23);
        endOfDayCalendar.set(Calendar.MINUTE, 59);
        endOfDayCalendar.set(Calendar.SECOND, 59);
        endOfDayCalendar.set(Calendar.MILLISECOND, 999);

        todayEventList =
                calendarEventManager.getAllEventsBetweenTimesAsSorted(startOFDayCalendar.getTimeInMillis(), endOfDayCalendar.getTimeInMillis());
    }

    /* TTS가 준비되면 기본 설정 후 speak */
    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
//            Toast.makeText(getApplicationContext(), "TTS에 문제가 있습니다", Toast.LENGTH_LONG).show();
            return;
        }

        /* TTS 언어 변경, 텍스트 반복하게 설정 */
        setupTTS();


        /* 일정 speak */
        speakTodayEvents();

        /* 날씨 받아오기 */
        fetchWeatherInformation();

        /* 날씨 정보 브로드캐스트로 전달 */
        // 날씨 정보를 성공적으로 가져왔으면
        if (weatherResponse != null) {
            sendTodayWeatherByBroadcast();
        }

        /* 날씨에 관해 말할 내용 만들기 */
        String weatherInformation = getWeatherTextToSpeak();

        /* 날씨 speak */
        addText(weatherInformation);
        System.out.println("whatToSay = " + whatToSay);
    }

    private void setupTTS() {
        // 폰의 기본 설정 가져오게 변경 예정
        int result = textToSpeech.setLanguage(Locale.KOREA);
        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTS", "This Language is not supported");
        }


        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            Handler handler = new Handler();

            @Override
            public void onStart(String utteranceId) {

            }

            // 끝나면 다시 반복해서 말하기
            @Override
            public void onDone(String utteranceId) {
                // 한 번 말한 후부터는 말이 끝나고 1초 뒤에 말하기
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textToSpeech.speak(whatToSay, TextToSpeech.QUEUE_FLUSH, null, ALARM_SPEECH_ID);
                    }
                }, 1000);
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void speakTodayEvents() {
        Calendar eventCalendar = Calendar.getInstance();

        StringBuilder whatToSay = new StringBuilder();
        whatToSay.append("일어날 시간이에요. ");

        // 오늘의 일정이 있으면
        if (!todayEventList.isEmpty()) {
            whatToSay.append("오늘의 일정을 알려드릴게요. ");
            // 각 이벤트에 대해 speak
            for (EventCoreInfo event : todayEventList) {

                eventCalendar.setTimeInMillis(Long.parseLong(event.getDtStart()));

                // 이벤트 시작 시간
                int isAMOrPM = eventCalendar.get(Calendar.AM_PM);
                if (isAMOrPM == Calendar.AM) {
                    whatToSay.append("오전 ");
                } else {
                    whatToSay.append("오후 ");
                }
                whatToSay.append(" ");
                whatToSay.append(eventCalendar.get(Calendar.HOUR) + "시  ");
                whatToSay.append(eventCalendar.get(Calendar.MINUTE) + "분  ");

                // 이벤트 이름
                whatToSay.append(event.getTitle());

                whatToSay.append("       ");
            }

        } else {
            whatToSay.append("오늘은 기록해놓은 일정이 없으세요. ");
        }

        speakText(whatToSay.toString());
    }

    public void speakText(String text) {
        whatToSay = new StringBuffer(text);
        textToSpeech.speak(whatToSay, TextToSpeech.QUEUE_ADD, null,  ALARM_SPEECH_ID);
    }

    // 기존에 말하던 게 끝나면 onDone()이 호출되면서 (기존 텍스트 + 추가 텍스트)를 말함
    public void addText(String text) {
        whatToSay.append(text);
    }

    @Override
    public void onDestroy() {
        // 알람 소리 종료
        mediaPlayer.stop();
        mediaPlayer.release();

        // TTS 종료
        textToSpeech.stop();
        textToSpeech.shutdown();

        // 알림 창에 있는 알림 제거
        notificationManager.cancelAll();


//        Toast.makeText(this, "알람을 종료했습니다.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


}