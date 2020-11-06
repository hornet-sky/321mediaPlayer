package com.example.mobileplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SplashActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView skipBtn;
    private Handler handler;
    private Runnable startMainActivityRunnable = new Runnable() {
        @Override
        public void run() {
            startMainActivity();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initView();
        initEvent();

        handler = new Handler();
        handler.postDelayed(startMainActivityRunnable, 2000);
    }

    private void initEvent() {
        skipBtn.setOnClickListener(this);
    }

    private void initView() {
        skipBtn = findViewById(R.id.skipBtn);
    }

    @Override
    public void onClick(View view) {
        startMainActivity();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(startMainActivityRunnable);
    }
}