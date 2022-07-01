package com.example.reminderalarm;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;

public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            // 일반 권한이라 런타임 때 요청할 필요 없다
            // 이 권한은 환경설정 화면에서 설정 가능
//            Manifest.permission.USE_FULL_SCREEN_INTENT,
//            Manifest.permission.SCHEDULE_EXACT_ALARM,
    };


    // 모든 퍼미션이 허가되었는 지 확인
    private boolean hasAllPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    // 퍼미션들을 체크한 후 콜백함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasAllPermissions(this, permissions)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            Toast.makeText(this, "앱의 정상적인 작동을 위해서는 접근 권한 허용이 필요합니다. 다시 실행 후 권한을 허용해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    // 런타임 때 퍼미션 확인, 요청하는 함수
    private void checkPermissions(String... permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

        /*
        if (!hasAllPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            // SCHEDULE_EXACT_ALARM 제외하고 모두 권한이 승인됐을 때만 메인 액티비티 실행
            startActivity(new Intent(this, MainActivity.class));
        }*/
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 런타임 권한 요청
        checkPermissions(PERMISSIONS);
    }
}