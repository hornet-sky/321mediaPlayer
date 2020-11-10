package com.example.mobileplayer.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.mobileplayer.R;

public abstract class BaseFragment extends Fragment {
    private Context context;
    private FrameLayout rootView;

    public BaseFragment(Context context) {
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.w("myTag", "BaseFragment.onCreateView");
        rootView = (FrameLayout) inflater.inflate(getLayout(), container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
    }

    protected void initData() {}

    protected void initView(FrameLayout rootView) {}

    protected int getLayout() { return 0; }
}