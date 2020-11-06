package com.example.mobileplayer.ui;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.mobileplayer.R;

public class NetVideoFragment extends BaseFragment {
    public NetVideoFragment(Context context) {
        super(context);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_net_video;
    }

    @Override
    protected void initView(FrameLayout rootView) {
        ((TextView) rootView.getChildAt(0)).setText("网络视频");
    }

    @Override
    protected void initData() {

    }
}