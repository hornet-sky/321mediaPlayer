package com.example.mobileplayer.ui;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.mobileplayer.R;

public class AudioFragment extends BaseFragment {
    public AudioFragment(Context context) {
        super(context);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_audio;
    }

    @Override
    protected void initView(FrameLayout rootView) {
        ((TextView) rootView.getChildAt(0)).setText("本地音乐");
    }

    @Override
    protected void initData() {

    }
}