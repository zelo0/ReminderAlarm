package com.example.reminderalarm.activity;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.reminderalarm.R;
import com.example.reminderalarm.activity.MainActivity;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            // 일반 권한이라 런타임 때 요청할 필요 없다
//            Manifest.permission.USE_FULL_SCREEN_INTENT,
            // 이 권한은 환경설정 화면에서 설정 가능
//            Manifest.permission.SCHEDULE_EXACT_ALARM,
    };

    private Snackbar snackbar;


    // 모든 퍼미션이 허가되었는 지 확인
    private boolean hasAllPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    // 퍼미션들을 체크한 후 콜백함수
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (hasAllPermissions(grantResults)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // 앱의 권한 설정 페이지로 이동하게 해주는 스낵바
            snackbar.show();
        }
    }

    // 런타임 때 퍼미션 확인, 요청하는 함수
    private void checkPermissions(String... permissions) {

        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 권한 설정 화면으로 이동하게 해주는 스낵바 생성
        snackbar = Snackbar.make(findViewById(android.R.id.content), "앱의 원활한 사용을 위해 모든 권한을 허가히주세요", Snackbar.LENGTH_INDEFINITE)
                .setAction("이동", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                });
    }



    @Override
    protected void onStart() {
        super.onStart();
        // 런타임 권한 요청
        checkPermissions(PERMISSIONS);
    }
}