package com.example.mobileplayer.component;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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

    private Uri videoUri;

    private GestureDetector gestureDetector;

    private int hideVideoControllerCountdown;

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            // 播放进度和播放时间
            if(mVideoView.isPlaying()) {
                currentPosition = mVideoView.getCurrentPosition();
                mVideoProgressSeekBar.setProgress(currentPosition);
                mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(currentPosition));
            }
            // 系统时间
            refreshSystemTime();
            // 控制面板定时消失
            if(mVideoController.getVisibility() == View.VISIBLE) {
                if(--hideVideoControllerCountdown <= 0) {
                    mVideoController.setVisibility(View.GONE);
                }
            }
            // 缓冲
            int max = mVideoProgressSeekBar.getMax();
            if(mVideoProgressSeekBar.getSecondaryProgress() < max) {
                mVideoProgressSeekBar.setSecondaryProgress(mVideoProgressSeekBar.getMax() * mVideoView.getBufferPercentage() / 100);
            }
            handler.postDelayed(this, 1000);
        }
    };

    private BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver;

    private AudioManager audioManager;

    private int screenWidth, screenHeight, videoWidth, videoHeight;
    private boolean isFullScreen;
    private float startX;
    private int startPosition;
    private boolean videoStartStatusIsPlaying, isSlightShaking = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("myTag", "SystemVideoPlayer.onCreate");
        initView();
        initEvent();
        initData();
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
                videoHeight = mediaPlayer.getVideoHeight();
                videoWidth = mediaPlayer.getVideoWidth();
                if(isFullScreen) {
                    fitInside(screenWidth, screenHeight, videoWidth, videoHeight);
                }
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
                    resetVideoControllerCountdown();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.w("myTag3", "mVideoProgressSeekBar.onStartTrackingTouch");
                isMediaPlayerPlaying = mVideoView.isPlaying();
                if(isMediaPlayerPlaying) {
                    mVideoView.pause(); // 移动进度条的时候先暂停播放
                }
                resetVideoControllerCountdown();
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
                    resetVideoControllerCountdown();
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                resetVideoControllerCountdown();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                Log.w("myTag", "GestureDetector.onLongPress - " + isSlightShaking);
                if(isSlightShaking) { // 如果不是大幅度划屏，则开关视频播放
                    exePauseOrStart();
                }
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.w("myTag", "GestureDetector.onDoubleTap");
                fullScreenToggle();
                gestureDetector.setIsLongpressEnabled(false); // 如果不禁用“长按”，则“双击”后会继续执行“长按”
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.w("myTag", "GestureDetector.onSingleTapConfirmed");
                if(mVideoController.getVisibility() == View.VISIBLE) {
                    mVideoController.setVisibility(View.GONE);
                } else {
                    mVideoController.setVisibility(View.VISIBLE);
                    resetVideoControllerCountdown();
                }
                return true;
            }
        });
    }

    private void initData() {
        df = new SimpleDateFormat("HH:mm:ss");
        Intent intent = getIntent();
        mediaItems = intent.getParcelableArrayListExtra("data");

        if(mediaItems != null && !mediaItems.isEmpty()) {
            itemIndex = intent.getIntExtra("itemIndex", -1);
            MediaItem mediaItem = mediaItems.get(itemIndex);
            mVideoView.setVideoURI(mediaItem.getUri());
            mVideoTitleTextView.setText(mediaItem.getName());
        }
        videoUri = intent.getData();
        if(videoUri != null) {
            mVideoView.setVideoURI(videoUri);
            String fileName = getFileRealNameFromUri(videoUri);
            mVideoTitleTextView.setText(fileName);
        }
        Log.w("myTag7", "initData[mediaItems=" + mediaItems + ", videoUri=" + videoUri + "]"); //initData[mediaItems=null, videoUri=content://com.android.providers.media.documents/document/video%3A145]
        refreshBottomControlBarBtnStatus();
        // 获取屏幕宽度、高度，用于后续设置全屏模式
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        screenHeight = outMetrics.heightPixels;
        screenWidth = outMetrics.widthPixels;
        // 设置音量进度条
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mVoiceVolumeSeekBar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        mVoiceVolumeSeekBar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private void refreshBottomControlBarBtnStatus() {
        if(mediaItems== null || mediaItems.isEmpty() || itemIndex <= 0) {
            mPrevBtn.setBackgroundResource(R.drawable.btn_pre_gray);
            mPrevBtn.setEnabled(false);
        } else {
            mPrevBtn.setBackgroundResource(R.drawable.btn_prev_selector);
            mPrevBtn.setEnabled(true);
        }

        if(mediaItems== null || mediaItems.isEmpty() || itemIndex >= mediaItems.size() - 1) {
            mNextBtn.setBackgroundResource(R.drawable.btn_next_gray);
            mNextBtn.setEnabled(false);
        } else {
            mNextBtn.setBackgroundResource(R.drawable.btn_next_selector);
            mNextBtn.setEnabled(true);
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voiceBtn:
                voiceMuteToggle();
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
                fullScreenToggle();
                break;
        }
        resetVideoControllerCountdown();
    }

    private void fullScreenToggle() {
        isFullScreen = !isFullScreen;
        if(isFullScreen) {
            fitInside(screenWidth, screenHeight, videoWidth, videoHeight);
            mScreenBtn.setBackgroundResource(R.drawable.btn_default_screen_selector);
        } else {
            suit();
            mScreenBtn.setBackgroundResource(R.drawable.btn_full_screen_selector);
        }
    }

    private void suit() {
        if(getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) { // 不是竖屏先切回竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        ViewGroup.LayoutParams params = mVideoView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        mVideoView.setLayoutParams(params);
    }

    private boolean isPortrait() {
        return getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    private boolean isLandscape() {
        return getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void fitInside(int screenWidth, int screenHeight, int videoWidth, int videoHeight) {
        Log.w("myTag5", "fitInside[screenWidth=" + screenWidth
                + ", screenHeight=" + screenHeight
                + ", videoWidth=" + videoWidth
                + ", videoHeight=" + videoHeight + "]");
        ViewGroup.LayoutParams params;
        if(videoHeight > videoWidth) {
            if(!isPortrait()) { // 需要切成竖屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if(videoWidth * screenHeight > videoHeight * screenWidth) {
                params = mVideoView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mVideoView.setLayoutParams(params);
                Log.w("myTag5", "fitInside - 竖屏 - 宽度撑满");
            } else {
                params = mVideoView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mVideoView.setLayoutParams(params);
                Log.w("myTag5", "fitInside - 竖屏 - 高度撑满");
            }
        } else {
            if(!isLandscape()) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if(videoWidth * screenWidth > videoHeight * screenHeight) {
                params = mVideoView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mVideoView.setLayoutParams(params);
                Log.w("myTag5", "fitInside - 横屏 - 宽度撑满");
            } else {
                params = mVideoView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mVideoView.setLayoutParams(params);
                Log.w("myTag5", "fitInside - 横屏 - 高度撑满");
            }
        }
    }


    private void resetVideoControllerCountdown() {
        hideVideoControllerCountdown = 4;
    }

    private void voiceMuteToggle() {
        setVoiceMute(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.w("myTag", "onTouchEvent - " + event.getAction());
        gestureDetector.onTouchEvent(event);
        resetVideoControllerCountdown();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                videoStartStatusIsPlaying = mVideoView.isPlaying();
                startPosition = currentPosition;
                startX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - startX;
                isSlightShaking = Math.abs(distance) < 6;
                if(!isSlightShaking) { // 防抖动
                    if(videoStartStatusIsPlaying && mVideoView.isPlaying()) {
                        mVideoView.pause();
                    }
                    int positionOffset = (int ) (distance / (isFullScreen ? screenHeight : screenWidth) * mVideoProgressSeekBar.getMax());
                    currentPosition = startPosition + positionOffset;
                    mVideoProgressSeekBar.setProgress(currentPosition);
                    mVideoView.seekTo(currentPosition);
                    mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(currentPosition));
                }
                break;
            case MotionEvent.ACTION_UP:
                if(videoStartStatusIsPlaying && !mVideoView.isPlaying()
                        && !isSlightShaking) { // 如果是轻微抖动，则不操作。避免影响长按事件的响应效果
                    mVideoView.start();
                }
                isSlightShaking = true;
                if(!gestureDetector.isLongpressEnabled()) { // 恢复“长按”
                    gestureDetector.setIsLongpressEnabled(true);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            setVoiceVolume(1); // 手动调节音量大小
            resetVideoControllerCountdown();
            return true; // 消费了事件，因此不会触发系统调节音量大小。系统调节音量大小时会出现额外的UI
        }
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            setVoiceVolume(-1);
            resetVideoControllerCountdown();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            setVoiceMute(true);
            resetVideoControllerCountdown();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setVoiceVolume(int increment) {
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) + increment;
        if(currentVolume > maxVolume) {
            currentVolume = maxVolume;
        } else if(currentVolume < 0) {
            currentVolume = 0;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
        mVoiceVolumeSeekBar.setProgress(currentVolume);
    }

    private void setVoiceMute(boolean state) {
        /*
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, state);
        mVoiceVolumeSeekBar.setProgress(state ? 0 : audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        */
        int volume = mVoiceVolumeSeekBar.getProgress();
        if(state) {
            if(volume > 0) {
                mVoiceVolumeSeekBar.setTag(volume);
                mVoiceVolumeSeekBar.setProgress(0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            }
        } else {
            Integer oldVolume = (Integer) mVoiceVolumeSeekBar.getTag();
            if(volume == 0 && oldVolume != null) {
                mVoiceVolumeSeekBar.setProgress(oldVolume);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
                mVoiceVolumeSeekBar.setTag(null);
            }
        }
    }

    private void prev() {
        mVideoView.pause();
        mVideoView.setVideoURI(mediaItems.get(--itemIndex).getUri());
        currentPosition = 0;
        refreshBottomControlBarBtnStatus();
    }

    private void next() {
        mVideoView.pause();
        mVideoView.setVideoURI(mediaItems.get(++itemIndex).getUri());
        currentPosition = 0;
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
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.w("myTag6", "onDetachedFromWindow");
        mVideoView.stopPlayback();
        mVideoView.setOnPreparedListener(null);
        mVideoView.setOnErrorListener(null);
        mVideoView.setOnCompletionListener(null);
        mVideoView = null;
    }

    private String getFileRealNameFromUri(Uri uri) {
        if(uri == null) {
            return "";
        }
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        return documentFile != null ? documentFile.getName() : "";
    }

    @Override
    protected void onDestroy() { // 比onDetachedFromWindow先执行
        super.onDestroy();
        Log.w("myTag6", "onDestroy");
        unregisterReceiver(this.batteryStatusBroadcastReceiver);
        this.batteryStatusBroadcastReceiver = null;
        this.handler.removeCallbacks(task);
        this.handler = null;
    }
}