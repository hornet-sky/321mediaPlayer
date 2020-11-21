package com.example.mobileplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SearchFromNetActivity extends AppCompatActivity {
    private TextView searchTextView;
    private ImageView voiceImageView;
    private TextView searchBtn;
    private RecyclerView recyclerView;
    private TextView searchNoDataTextView;
    private ProgressBar loadingProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_from_net);

        initView();
        initEvent();
    }

    private void initView() {
        searchTextView = findViewById(R.id.searchTextView);
        voiceImageView = findViewById(R.id.voiceImageView);
        searchBtn = findViewById(R.id.searchBtn);
        recyclerView = findViewById(R.id.recyclerView);
        searchNoDataTextView = findViewById(R.id.searchNoDataTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void initEvent() {

    }
}