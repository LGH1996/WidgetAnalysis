package com.lgh.widgetanalysis;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView button = findViewById(R.id.button0);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyAccessibilityService.mainFunction != null) {
                    MyAccessibilityService.mainFunction.showAnalysisFloatWindow();
                } else {
                    Toast.makeText(MainActivity.this, "请打开无障碍服务", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}