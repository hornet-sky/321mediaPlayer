package com.example.mobileplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import com.example.mobileplayer.ui.AudioFragment;
import com.example.mobileplayer.ui.BaseFragment;
import com.example.mobileplayer.ui.NetAudioFragment;
import com.example.mobileplayer.ui.NetVideoFragment;
import com.example.mobileplayer.ui.VideoFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001;
    private static final int REQUEST_CODE_ACTION_APPLICATION_DETAILS_SETTINGS = 2001;
    private SparseArray<BaseFragment> fragments;
    private FrameLayout contentFrameLayout;
    private RadioGroup bottomRadioGroup;
    private int prevKey;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initEvent();
        initPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if(permissions.length > 0 && Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[0])
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.w("myTag", "onRequestPermissionsResult - PERMISSION_GRANTED");
                initData();
            } else if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.w("myTag", "onRequestPermissionsResult - shouldShowRequestPermissionRationale");
                initPermission();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage("播放视频、音乐需要外部存储设备读写权限")
                        .setPositiveButton("现在去授权", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivityForResult(intent, REQUEST_CODE_ACTION_APPLICATION_DETAILS_SETTINGS);
                            }
                        })
                        .setNegativeButton("以后再说", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish(); // 退出App
                            }
                        }).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w("myTag", "onActivityResult[requestCode=" + requestCode + ", resultCode=" + resultCode + "]");
        if(requestCode == REQUEST_CODE_ACTION_APPLICATION_DETAILS_SETTINGS) {
            initPermission();
        }
    }

    private void initPermission() {
        // 也可以先判断一下外部存储设备是否已挂载
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_READ_EXTERNAL_STORAGE);
        } else {
            initData();
        }
    }

    private void initView() {
        contentFrameLayout = findViewById(R.id.contentFrameLayout);
        bottomRadioGroup = findViewById(R.id.bottomRadioGroup);
    }

    private void initData() {
        fragments = new SparseArray<>(4);
        fragments.put(R.id.videoRadioBtn, new VideoFragment(this));
        fragments.put(R.id.audioRadioBtn, new AudioFragment(this));
        fragments.put(R.id.netVideoRadioBtn, new NetVideoFragment(this));
        fragments.put(R.id.netAudioRadioBtn, new NetAudioFragment(this));
        bottomRadioGroup.check(R.id.videoRadioBtn);
    }

    private void initEvent() {
        bottomRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int radioButtonId) {
        showFragment(radioButtonId);
    }

    private void showFragment(int currentKey) {
        Log.w("myTag", "showFragment[currentKey=" + currentKey + ", prevKey=" + prevKey + "]");
        if(currentKey == prevKey) {
            return;
        }
        BaseFragment currentFragment = fragments.get(currentKey);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if(prevKey != 0) {
            fragmentTransaction.hide(fragments.get(prevKey));
        }
        if(currentFragment.isAdded()) {
            fragmentTransaction.show(currentFragment);
        } else {
            fragmentTransaction.add(R.id.contentFrameLayout, currentFragment);
        }
        fragmentTransaction.commit();
        prevKey = currentKey;
    }
}