package com.example.mobileplayer.component;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.mobileplayer.R;
import com.example.mobileplayer.broadcastreceiver.BatteryStatusBroadcastReceiver;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.utils.MediaUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SystemVideoPlayer extends AppCompatActivity implements View.OnClickListener {
    private ConstraintLayout mVideoController;
    private VideoView mVideoView;
    private LinearLayout mVideoTitleBar;
    private TextView mVideoTitleTextView;
    private ImageView mBatteryStatusImageView;
    private TextView mSystemTimeTextView;
    private LinearLayout mVideoProgressTopBar;
    private Button mVoiceBtn;
    private SeekBar mVoiceVolumeSeekBar;
    private Button mInfoBtn;
    private LinearLayout mVideoProgressBottomBar;
    private TextView mVideoCurrentTimeTextView;
    private SeekBar mVideoProgressSeekBar;
    private TextView mVideoEndTimeTextView;
    private LinearLayout mVideoControlBottomBar;
    private Button mExitBtn;
    private Button mPrevBtn;
    private Button mPauseOrStartBtn;
    private Button mNextBtn;
    private Button mScreenBtn;

    private int currentPosition;

    private SimpleDateFormat df;

    private List<MediaItem> mediaItems;
    private int itemIndex;

    private GestureDetector gestureDetector;

    private int hideVideoControllerCountdown;

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            if(mVideoView.isPlaying()) {
                int currentPosition = mVideoView.getCurrentPosition();
                mVideoProgressSeekBar.setProgress(currentPosition);
                mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(currentPosition));
            }
            refreshSystemTime();
            if(mVideoController.getVisibility() == View.VISIBLE) {
                if(--hideVideoControllerCountdown <= 0) {
                    mVideoController.setVisibility(View.GONE);
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    private BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("myTag", "SystemVideoPlayer.onCreate");
        initView();
        initData();
        initEvent();
        initBroadcastReceiver();
        handler.postDelayed(task, 0);
    }

    private void initBroadcastReceiver() {
        // 像电量、锁屏这种广播必须动态注册广播接收器 才有效果
        this.batteryStatusBroadcastReceiver = new BatteryStatusBroadcastReceiver(new BatteryStatusBroadcastReceiver.OnBatteryStatusChangeListener() {
            @Override
            public void onChange(int level) {
                if(level <= 0) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_0);
                } else if(level <= 10) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_10);
                } else if(level <= 20) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_20);
                } else if(level <= 40) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_40);
                } else if(level <= 60) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_60);
                } else if(level <= 80) {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_80);
                } else {
                    mBatteryStatusImageView.setImageResource(R.drawable.ic_battery_100);
                }
            }
        });
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(this.batteryStatusBroadcastReceiver, intentFilter);
    }

    private void initEvent() {
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.w("myTag2", "videoView.onPrepared");
                //mediaPlayer.setLooping(true);
                int duration = mediaPlayer.getDuration();
                mVideoProgressSeekBar.setMax(duration);
                String endTime = MediaUtils.formatDuration(duration);
                mVideoEndTimeTextView.setText(endTime);
                String currentTime = MediaUtils.formatDuration(currentPosition);
                mVideoCurrentTimeTextView.setText(currentTime);
                mediaPlayer.seekTo(currentPosition);
                mediaPlayer.start();
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.e("myTag", "videoView.onError");
                return true;
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.w("myTag", "videoView.onCompletion");
                if(itemIndex < mediaItems.size() - 1) { //自动播放下一个视频
                    next();
                }
            }
        });

        mVoiceBtn.setOnClickListener(this);
        mInfoBtn.setOnClickListener(this);
        mExitBtn.setOnClickListener(this);
        mPrevBtn.setOnClickListener(this);
        mPauseOrStartBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mScreenBtn.setOnClickListener(this);

        mVideoProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean isMediaPlayerPlaying;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    mVideoView.seekTo(progress);
                    mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(progress));
                    hideVideoControllerCountdown = 4;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.w("myTag3", "mVideoProgressSeekBar.onStartTrackingTouch");
                isMediaPlayerPlaying = mVideoView.isPlaying();
                if(isMediaPlayerPlaying) {
                    mVideoView.pause(); // 移动进度条的时候先暂停播放
                }
                hideVideoControllerCountdown = 4;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.w("myTag3", "mVideoProgressSeekBar.onStopTrackingTouch");
                if(isMediaPlayerPlaying) {
                    mVideoView.start(); // 继续播放
                }
            }
        });

        mVoiceVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    hideVideoControllerCountdown = 4;

                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                hideVideoControllerCountdown = 4;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {

            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if(mVideoController.getVisibility() == View.VISIBLE) {
                    mVideoController.setVisibility(View.GONE);
                } else {
                    mVideoController.setVisibility(View.VISIBLE);
                    hideVideoControllerCountdown = 4;
                }
                return true;
            }
        });
    }

    private void initData() {
        df = new SimpleDateFormat("HH:mm:ss");
        Intent intent = getIntent();
        mediaItems = intent.getParcelableArrayListExtra("data");
        itemIndex = intent.getIntExtra("itemIndex", -1);
        mVideoView.setVideoURI(mediaItems.get(itemIndex).getUri());
        refreshBottomControlBarBtnStatus();
    }

    private void refreshBottomControlBarBtnStatus() {
        if(itemIndex <= 0) {
            mPrevBtn.setBackgroundResource(R.drawable.btn_pre_gray);
            mPrevBtn.setClickable(false);
        } else {
            mPrevBtn.setBackgroundResource(R.drawable.btn_prev_selector);
            mPrevBtn.setClickable(true);
        }

        if(itemIndex >= mediaItems.size() - 1) {
            mNextBtn.setBackgroundResource(R.drawable.btn_next_gray);
            mNextBtn.setClickable(false);
        } else {
            mNextBtn.setBackgroundResource(R.drawable.btn_next_selector);
            mNextBtn.setClickable(true);
        }
    }

    private void initView() {
        setContentView(R.layout.activity_system_video_player);
        mVideoController = findViewById(R.id.videoController);
        mVideoView = findViewById(R.id.videoView);
        mVideoTitleBar = (LinearLayout) findViewById(R.id.videoTitleBar);
        mVideoTitleTextView = (TextView) findViewById(R.id.videoTitleTextView);
        mBatteryStatusImageView = (ImageView) findViewById(R.id.batteryStatusImageView);
        mSystemTimeTextView = (TextView) findViewById(R.id.systemTimeTextView);
        mVideoProgressTopBar = (LinearLayout) findViewById(R.id.videoProgressTopBar);
        mVoiceBtn = (Button) findViewById(R.id.voiceBtn);
        mVoiceVolumeSeekBar = (SeekBar) findViewById(R.id.voiceVolumeSeekBar);
        mInfoBtn = (Button) findViewById(R.id.infoBtn);
        mVideoProgressBottomBar = (LinearLayout) findViewById(R.id.videoProgressBottomBar);
        mVideoCurrentTimeTextView = (TextView) findViewById(R.id.videoCurrentTimeTextView);
        mVideoProgressSeekBar = (SeekBar) findViewById(R.id.videoProgressSeekBar);
        mVideoEndTimeTextView = (TextView) findViewById(R.id.videoEndTimeTextView);
        mVideoControlBottomBar = (LinearLayout) findViewById(R.id.videoControlBottomBar);
        mExitBtn = (Button) findViewById(R.id.exitBtn);
        mPrevBtn = (Button) findViewById(R.id.prevBtn);
        mPauseOrStartBtn = (Button) findViewById(R.id.pauseOrStartBtn);
        mNextBtn = (Button) findViewById(R.id.nextBtn);
        mScreenBtn = (Button) findViewById(R.id.screenBtn);
    }

    @Override
    protected void onPause() {
        Log.w("myTag", "SystemVideoPlayer.onPause");
        super.onPause();
        mVideoView.pause();
        currentPosition = mVideoView.getCurrentPosition();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voiceBtn:
                break;
            case R.id.infoBtn:
                break;
            case R.id.exitBtn:
                finish();
                break;
            case R.id.prevBtn:
                prev();
                break;
            case R.id.pauseOrStartBtn:
                exePauseOrStart();
                break;
            case R.id.nextBtn:
                next();
                break;
            case R.id.screenBtn:
                break;
        }
        hideVideoControllerCountdown = 4;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private void prev() {
        mVideoView.setVideoURI(mediaItems.get(--itemIndex).getUri());
        refreshBottomControlBarBtnStatus();
    }

    private void next() {
        mVideoView.setVideoURI(mediaItems.get(++itemIndex).getUri());
        refreshBottomControlBarBtnStatus();
    }

    private void exePauseOrStart() {
        if(mVideoView.isPlaying()) {
            mVideoView.pause();
            mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_play_selector);
        } else {
            mVideoView.start();
            mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_pause_selector);
        }
    }

    private void refreshSystemTime() {
        this.mSystemTimeTextView.setText(df.format(new Date()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.batteryStatusBroadcastReceiver);
        handler.removeCallbacks(task);
    }
}