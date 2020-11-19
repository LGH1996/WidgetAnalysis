package com.lgh.widgetanalysis;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MyAccessibilityService.mainFunction != null) {
            MyAccessibilityService.mainFunction.showAnalysisFloatWindow();
        } else {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(intent);
            }
            Toast.makeText(MainActivity.this, "请打开无障碍服务", Toast.LENGTH_SHORT).show();
        }
        finishAndRemoveTask();
    }
}