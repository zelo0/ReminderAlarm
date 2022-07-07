package com.example.reminderalarm;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmService extends Service implements TextToSpeech.OnInitListener {

    private MediaPlayer mediaPlayer;
    private List<EventCoreInfo> todayEventList;

    /* TTS */
    private TextToSpeech textToSpeech;
    // 말할 내용
    private StringBuffer whatToSay;
    private static final String ALARM_SPEECH_ID = "ALARM_SPEECH_ID";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 알람 울릴 때 사용할 사운드
        Uri bellUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        // mediaPlayer 생성
        mediaPlayer = MediaPlayer.create(this, bellUri);

        // 재생
        mediaPlayer.setVolume(50,50);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        Toast.makeText(this, "ring ~ ring ~", Toast.LENGTH_SHORT).show();

        /* 오늘의 일정 받아오기 */
        fetchTodayEvents();
        System.out.println("todayEventList.size() = " + todayEventList.size());

        /* 날씨 받아오기 */

        /* TTS 생성 -> 준비되면 speak */
        textToSpeech = new TextToSpeech(AlarmService.this, this);


        return super.onStartCommand(intent, flags, startId);
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
            Toast.makeText(getApplicationContext(), "TTS에 문제가 있습니다", Toast.LENGTH_LONG).show();
            return;
        }

        /* TTS 언어 변경, 텍스트 반복하게 설정 */
        setupTTS();


        /* 일정 speak */
        speakTodayEvents();

        /* 날씨 speak */
//        addText();
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

        Toast.makeText(this, "알람을 종료했습니다.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }


}