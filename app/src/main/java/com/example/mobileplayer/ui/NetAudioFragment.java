package com.example.mobileplayer.ui;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.mobileplayer.R;

public class NetAudioFragment extends BaseFragment {
    public NetAudioFragment(Context context) {
        super(context);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_net_audio;
    }

    @Override
    protected void initView(FrameLayout rootView) {
        ((TextView) rootView.getChildAt(0)).setText("网络音乐");
    }

    @Override
    protected void initData() {

    }
}