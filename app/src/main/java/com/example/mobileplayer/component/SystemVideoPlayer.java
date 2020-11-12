package com.example.mobileplayer.component;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mobileplayer.R;
import com.example.mobileplayer.broadcastreceiver.BatteryStatusBroadcastReceiver;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.utils.MediaUtils;
import com.example.mobileplayer.utils.SystemUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.vov.vitamio.Vitamio;

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
    private LinearLayout mSpeedCoverLayout;
    private TextView mNetSpeedTextView;
    private LinearLayout mMsgCoverLayout;
    private TextView mMsgTextView;

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
            // 视频缓冲
            int max = mVideoProgressSeekBar.getMax();
            if(mVideoProgressSeekBar.getSecondaryProgress() < max) {
                mVideoProgressSeekBar.setSecondaryProgress(mVideoProgressSeekBar.getMax() * mVideoView.getBufferPercentage() / 100);
            }
            // 网速
            if(mSpeedCoverLayout.getVisibility() == View.VISIBLE) {
                mNetSpeedTextView.setText(MediaUtils.getNetSpeed(getApplicationContext()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver;

    private AudioManager audioManager;

    private int screenWidth, screenHeight, videoWidth, videoHeight;
    private boolean isFullScreen;
    private float startX, startY;
    private boolean xLock, yLock;
    private int startPosition;
    private boolean videoStartStatusIsPlaying, isSlightShaking = true;

    private String deviceBrand;
    private int maxScreenBrightness, currentScreenBrightness, startScreenBrightness;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w("myTag", "SystemVideoPlayer.onCreate");
        initView();
        initEvent();
        try {
            initData();
        } catch (Settings.SettingNotFoundException e) {
            Log.e("myTag", e.getMessage(), e);
        }
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
                mSpeedCoverLayout.setVisibility(View.GONE);

                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        // 记录拖动的位置，用于分析用户观看习惯
                    }
                });
            }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                Log.e("myTag", "videoView.onError - " + what + " - " + extra);
                //1.播放的视频格式不支持--跳转到万能播放器继续播放
                //2.播放网络视频的时候，网络中断---1.如果网络确实断了，可以提示用于网络断了；2.网络断断续续的，重新播放
                //3.播放的时候本地文件中间有空白---下载做完成
                // rmvb格式不支持  1（未知错误）  -2147483648（系统错误）
                mSpeedCoverLayout.setVisibility(View.GONE);
                /*
                mMsgTextView.setText("加载视频失败");
                mMsgCoverLayout.setVisibility(View.VISIBLE);
                 */
                switchVitamioVideoPlayer(); // Vitamio太古老，现在支持的不是很好了
                // switchIjkVideoPlayer(); // bilibili万能播放器
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
        mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.w("myTag", "onInfo - " + what + " - " + extra);
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.w("myTag", "MEDIA_INFO_BUFFERING_START");
                        mSpeedCoverLayout.setVisibility(View.VISIBLE);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        Log.w("myTag", "MEDIA_INFO_BUFFERING_END");
                        mSpeedCoverLayout.setVisibility(View.GONE);
                        break;
                    default:;
                }
                return false;
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

    private void initData() throws Settings.SettingNotFoundException {
        deviceBrand = SystemUtils.getDeviceBrand();
        if(deviceBrand != null) {
            deviceBrand = deviceBrand.toLowerCase();
        }
        maxScreenBrightness = 255;
        if("redmi".equals(deviceBrand) || "xiaomi".equals(deviceBrand)) { // 不同手机厂商的屏幕亮度值不同,因此需要适配
            maxScreenBrightness = 2047;
        }
        currentScreenBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        Log.w("myTag", "initData[maxScreenBrightness=" + maxScreenBrightness + ", currentSystemScreenBrightness=" + currentScreenBrightness + "]");

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
        mSpeedCoverLayout = (LinearLayout) findViewById(R.id.speedCoverLayout);
        mNetSpeedTextView = findViewById(R.id.netSpeedTextView);
        mMsgCoverLayout = (LinearLayout) findViewById(R.id.msgCoverLayout);
        mMsgTextView = findViewById(R.id.msgTextView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.voiceBtn:
                voiceMuteToggle();
                break;
            case R.id.infoBtn:
                showCompatibilityInfo();
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

    private void showCompatibilityInfo() {
        new AlertDialog.Builder(this)
                .setTitle("提示信息")
                .setMessage("当只有声音没有画面时，可以切换到万能播放器。\n现在是否切换到万能播放器？")
                .setNegativeButton("取消", null)
                .setPositiveButton("切换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchVitamioVideoPlayer(); // Vitamio太古老，现在支持的不是很好了
                        // switchIjkVideoPlayer(); // bilibili万能播放器
                    }
                })
                .show();
    }

    private void switchVitamioVideoPlayer() {
        Intent intent = getIntent();
        intent.setClass(SystemVideoPlayer.this, VitamioVideoPlayer.class);
        intent.putExtra("itemIndex", itemIndex);
        startActivity(intent);
        finish();
    }

    private void switchIjkVideoPlayer() {
        Intent intent = getIntent();
        intent.setClass(SystemVideoPlayer.this, IVideoPlayer.class);
        intent.putExtra("itemIndex", itemIndex);
        startActivity(intent);
        finish();
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
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
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
                startY = event.getY();
                startScreenBrightness = currentScreenBrightness;
                break;
            case MotionEvent.ACTION_MOVE:
                // 调节屏幕亮度
                float distance = startY - event.getY();
                isSlightShaking = Math.abs(distance) < 10;
                Log.w("myTag9", "distance=" + distance);
                if(!isSlightShaking && !yLock) { // 防抖动
                    xLock = true; // 锁住X轴,只允许在Y轴滑动
                    int offset = (int) (distance / (isFullScreen ? screenWidth : screenHeight / 2) * maxScreenBrightness);
                    currentScreenBrightness = startScreenBrightness + offset;
                    Log.w("myTag9", "offset=" + offset + ", startScreenBrightness=" + startScreenBrightness);
                    if(currentScreenBrightness > maxScreenBrightness) {
                        currentScreenBrightness = maxScreenBrightness;
                    } else if(currentScreenBrightness < 0) {
                        currentScreenBrightness = 0;
                    }
                    Window localWindow = getWindow();
                    WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
                    localLayoutParams.screenBrightness = Float.valueOf(currentScreenBrightness) / maxScreenBrightness;
                    localWindow.setAttributes(localLayoutParams);
                }
                // 调节视频进度
                distance = event.getX() - startX;
                isSlightShaking = Math.abs(distance) < 10;
                if(!isSlightShaking && !xLock) { // 防抖动
                    yLock = true; // 锁住Y轴,只允许在X轴滑动
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
                xLock = false;
                yLock = false;
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
        mVideoTitleTextView.setText(mediaItems.get(itemIndex).getName());
        currentPosition = 0;
        mMsgCoverLayout.setVisibility(View.GONE);
        mSpeedCoverLayout.setVisibility(View.VISIBLE);
        refreshBottomControlBarBtnStatus();
    }

    private void next() {
        mVideoView.pause();
        mVideoView.setVideoURI(mediaItems.get(++itemIndex).getUri());
        mVideoTitleTextView.setText(mediaItems.get(itemIndex).getName());
        currentPosition = 0;
        mMsgCoverLayout.setVisibility(View.GONE);
        mSpeedCoverLayout.setVisibility(View.VISIBLE);
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

    private String getFileRealNameFromUri(Uri uri) {
        if(uri == null) {
            return "";
        }
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        return documentFile != null ? documentFile.getName() : "";
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