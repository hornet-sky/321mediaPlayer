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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobileplayer.ui.AudioFragment;
import com.example.mobileplayer.ui.BaseFragment;
import com.example.mobileplayer.ui.NetAudioFragment;
import com.example.mobileplayer.ui.NetVideoFragment;
import com.example.mobileplayer.ui.VideoFragment;
import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private static final int REQUEST_CODE_APPLY_RIGHTS = 1000;
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001;
    private static final int REQUEST_CODE_RECORD_AUDIO = 1002;
    private static final int REQUEST_CODE_ACTION_APPLICATION_DETAILS_SETTINGS = 2001;
    private SparseArray<BaseFragment> fragments;
    private FrameLayout contentFrameLayout;
    private RadioGroup bottomRadioGroup;
    private int prevKey;

    private TextView searchTextView;
    private FrameLayout gameBtn;
    private TextView recordBtn;

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
        if(requestCode == REQUEST_CODE_APPLY_RIGHTS) {
            String permission;
            for(int i = 0, len = permissions.length; i < len; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                permission = permissions[i];
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    initPermission();
                    return;
                }
                String message = "";
                if(permission == Manifest.permission.READ_EXTERNAL_STORAGE) {
                    message = "播放视频、音乐需要外部存储设备读写权限";
                } else if(permission == Manifest.permission.RECORD_AUDIO) {
                    message = "音乐跳动频谱需要录音权限";
                }
                new AlertDialog.Builder(this)
                        .setTitle("提示")
                        .setMessage(message)
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
                return;
            }
            initData();
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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            initData();
            return;
        }
        // 也可以先判断一下外部存储设备是否已挂载
        List<String> permissions = new ArrayList<>(2);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), REQUEST_CODE_APPLY_RIGHTS);
    }

    private void initView() {
        contentFrameLayout = findViewById(R.id.contentFrameLayout);
        bottomRadioGroup = findViewById(R.id.bottomRadioGroup);
        searchTextView = findViewById(R.id.searchTextView);
        gameBtn = findViewById(R.id.gameBtn);
        recordBtn = findViewById(R.id.recordBtn);
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
        searchTextView.setOnClickListener(this);
        gameBtn.setOnClickListener(this);
        recordBtn.setOnClickListener(this);
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

    private long indate;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(bottomRadioGroup.getCheckedRadioButtonId() != R.id.videoRadioBtn) {
                bottomRadioGroup.check(R.id.videoRadioBtn);
                return true;
            }
            long now = System.currentTimeMillis();
            if(now > indate) {
                indate = now + 2000;
                Toast toast = Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.searchTextView:
                Intent intent = new Intent(this, SearchFromNetActivity.class);
                startActivity(intent);
                break;
            case R.id.gameBtn:
                Toast.makeText(this, "游戏", Toast.LENGTH_SHORT).show();
                break;
            case R.id.recordBtn:
                Toast.makeText(this, "历史记录", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}