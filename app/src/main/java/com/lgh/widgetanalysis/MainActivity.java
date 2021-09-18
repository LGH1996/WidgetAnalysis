package com.lgh.widgetanalysis;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button_0 = findViewById(R.id.button0);
        button_0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED && MyAccessibilityService.mainFunction == null) {
                    Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, getPackageName() + File.separator + MyAccessibilityService.class.getName());
                    Toast.makeText(MainActivity.this, "请再试一次", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (MyAccessibilityService.mainFunction != null) {
                    MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 0x01);
                } else {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                        startActivity(intent);
                    }
                    Toast.makeText(MainActivity.this, "请打开无障碍服务", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0x01 && resultCode == RESULT_OK && data != null) {
            MyAccessibilityService.mainFunction.createForegroundNotification();
            MyAccessibilityService.mainFunction.initCapture(resultCode, data);
            MyAccessibilityService.mainFunction.showAnalysisFloatWindow();
            finishAndRemoveTask();
        } else {
            Toast.makeText(this, "截屏请求失败", Toast.LENGTH_SHORT).show();
        }
    }
}