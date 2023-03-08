package com.lgh.widgetanalysis;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);
        TextView tv1 = findViewById(R.id.tv_1);
        TextView tv2 = findViewById(R.id.tv_2);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED && MyAccessibilityService.mainFunction == null) {
                    Settings.Secure.putString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, getPackageName() + File.separator + MyAccessibilityService.class.getName());
                }
                if (MyAccessibilityService.mainFunction != null) {
                    if (MyAccessibilityService.mainFunction.canCapture()) {
                        MyAccessibilityService.mainFunction.createForegroundNotification();
                        MyAccessibilityService.mainFunction.showAnalysisFloatWindow();
                        finishAndRemoveTask();
                    } else {
                        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), 0x01);
                    }
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
        tv1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("无障碍服务打开命令", tv1.getText().toString());
                ClipboardManager clipboardManager = getSystemService(ClipboardManager.class);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(getPackageName(), tv1.getText().toString()));
                Toast.makeText(MainActivity.this, "该命令已复制到剪贴板，已通过log打印输出", Toast.LENGTH_SHORT).show();
            }
        });
        tv2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("无障碍服务打开命令", tv2.getText().toString());
                ClipboardManager clipboardManager = getSystemService(ClipboardManager.class);
                clipboardManager.setPrimaryClip(ClipData.newPlainText(getPackageName(), tv2.getText().toString()));
                Toast.makeText(MainActivity.this, "该命令已复制到剪贴板，已通过log打印输出", Toast.LENGTH_SHORT).show();
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