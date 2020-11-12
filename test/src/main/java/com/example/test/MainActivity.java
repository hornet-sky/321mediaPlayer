package com.example.test;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button openMediaPlayerBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openMediaPlayerBtn = findViewById(R.id.openMediaPlayerBtn);
        openMediaPlayerBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // Uri uri = Uri.parse("http://10.1.56.111:8080/hotel_california/1.mp4");
        // Uri uri = Uri.parse("http://10.1.56.111:8080/hotel_california/%E5%8A%A0%E5%B7%9E%E6%97%85%E9%A6%86.rmvb");
        Uri uri = Uri.parse("http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8");
        Intent intent = new Intent();
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }
}