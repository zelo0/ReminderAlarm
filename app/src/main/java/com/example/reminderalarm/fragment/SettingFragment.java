package com.example.reminderalarm.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.reminderalarm.R;

public class SettingFragment extends PreferenceFragmentCompat {
    private static final int REQUEST_CODE_ALERT_RINGTONE = 10;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> pickSoundLauncher;
    private SharedPreferences.Editor editor;
    private MediaPlayer alarmSoundPlayer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /* 액티비티 시작하고 결과 받는 리스너 등록 */
        pickSoundLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            /* 새로 선택된 벨소리를 shared preferences에 저장, summary에 업데이트 */
                            Intent data = result.getData();
                            if (data != null) {
                                Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                                if (ringtoneUri == null) {
                                    // no selected
                                    ringtoneUri = getDefaultRingtone();
                                }
                                /* shared prefrence에 저장 */
                                setRingtonePreferenceValue(ringtoneUri.toString());

                                /* 벨소리 제목으로 업데이트 */
                                Ringtone selectedRingtone = RingtoneManager.getRingtone(getContext().getApplicationContext(), ringtoneUri);
                                String soundTitle = selectedRingtone.getTitle(getContext().getApplicationContext());
                                updateAlarmSound(soundTitle);
                            }
                        }
                    }
                });
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.setting, rootKey);


        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext());
        editor = sharedPreferences.edit();

        /* 알람 소리 저장된 값 읽어서 세팅 */
        Preference alarmSoundPreference = findPreference(getString(R.string.KEY_ALARM_SOUND));
        alarmSoundPreference.setSummary(sharedPreferences.getString(getString(R.string.KEY_ALARM_SOUND_TITLE), "기본"));

        /* sound volume  */
        Preference soundVolumePreference = findPreference(getString(R.string.KEY_SOUND_VOLUME));
        /* change listener */
        soundVolumePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            if (alarmSoundPlayer != null && alarmSoundPlayer.isPlaying()) {
                Integer newValueInt = (Integer) newValue;
                float newVolume = newValueInt.intValue() / (float)100;
                alarmSoundPlayer.setVolume(newVolume, newVolume);
            }
            System.out.println("changed: " + newValue);
            return true;
        });

        /* 이벤트 기준 시간 */
        EditTextPreference eventBaseTimePreference = findPreference(getString(R.string.KEY_EVENT_BASE_TIME));
        // 저장된 값 읽어 세팅
        eventBaseTimePreference.setSummary(
                sharedPreferences.getString(getString(R.string.KEY_EVENT_BASE_TIME), "0") + "시간");
        /* 양수만 가능하게 */
        eventBaseTimePreference.setOnBindEditTextListener(
                editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
        /* 변화 리스너 등록 - summary 변경 */
        eventBaseTimePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                eventBaseTimePreference.setSummary(newValue.toString() + "시간");
                return true;
            }
        });
    }




    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        if (preference.getKey().equals(getString(R.string.KEY_ALARM_SOUND))) {
            /* 알람 소리 선택 클릭 시 */
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL  );
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, getDefaultRingtone());

            String existingValue = getRingtonePreferenceValue();
            if (existingValue != null) {
                if (existingValue.length() == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getDefaultRingtone());
            }

            pickSoundLauncher.launch(intent);
            return true;
        } else if (preference.getKey().equals(getString(R.string.KEY_SOUND_VOLUME))) {
            /* 알람 소리 볼륨 클릭 시 재생  */
            System.out.println("clicked");
            if (alarmSoundPlayer == null) {
                alarmSoundPlayer = MediaPlayer.create(getContext(), Uri.parse(getRingtonePreferenceValue()));
                alarmSoundPlayer.setLooping(true);
                float soundVolume = getSoundVolumePreferenceValueInFloat();
                alarmSoundPlayer.setVolume(soundVolume, soundVolume);
                alarmSoundPlayer.start();
            } else if (!alarmSoundPlayer.isPlaying()) {
                alarmSoundPlayer.start();
            } else if (alarmSoundPlayer.isPlaying()) {
//                alarmSoundPlayer.seekTo(0); // 처음부터 실행할까 말까
                alarmSoundPlayer.pause();
            }


            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }

    }



    private Uri getDefaultRingtone() {
        return RingtoneManager.getActualDefaultRingtoneUri(getActivity().getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
    }

    private String getRingtonePreferenceValue() {
        return sharedPreferences.getString(getString(R.string.KEY_ALARM_SOUND), getDefaultRingtone().toString());
    }

    private float getSoundVolumePreferenceValueInFloat() {
        return sharedPreferences.getInt(getString(R.string.KEY_SOUND_VOLUME), 50) / (float)100;
    }


  
    private void setRingtonePreferenceValue(String alarmSound) {
        editor.putString(getString(R.string.KEY_ALARM_SOUND), alarmSound);
        editor.apply();
    }

    public void updateAlarmSound(String soundTitle) {
        /* 저장 */
        editor.putString(getString(R.string.KEY_ALARM_SOUND_TITLE), soundTitle);
        editor.apply();

        /* summary 텍스트 변경 */
        Preference alarmSoundPreference = findPreference(getString(R.string.KEY_ALARM_SOUND));
        alarmSoundPreference.setSummary(soundTitle);
    }


    @Override
    public void onStop() {
        if (alarmSoundPlayer != null) {
            alarmSoundPlayer.release();
            alarmSoundPlayer = null;
        }

        super.onStop();
    }
}

