package com.example.mobileplayer.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.mobileplayer.R;

public class TitleBar extends LinearLayout implements View.OnClickListener {
    public TitleBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        TextView searchBar = (TextView) getChildAt(1);
        FrameLayout gameBtn = (FrameLayout) getChildAt(2);
        TextView recordBtn = (TextView) getChildAt(3);

        searchBar.setOnClickListener(this);
        gameBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchTextView:
                Toast.makeText(view.getContext(), "搜索", Toast.LENGTH_SHORT).show();
                break;
            case R.id.gameBtn:
                Toast.makeText(view.getContext(), "游戏", Toast.LENGTH_SHORT).show();
                break;
            case R.id.recordBtn:
                Toast.makeText(view.getContext(), "播放历史", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
