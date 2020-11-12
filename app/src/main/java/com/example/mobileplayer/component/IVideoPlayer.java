package com.example.mobileplayer.component;

import android.content.DialogInterface;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;

import com.example.mobileplayer.R;
import com.example.mobileplayer.broadcastreceiver.BatteryStatusBroadcastReceiver;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.utils.MediaUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IVideoPlayer extends AppCompatActivity implements View.OnClickListener {
    private String tag = getClass().getSimpleName();
    private ConstraintLayout mVideoController;
    private SurfaceView mSurfaceView;
    private IjkMediaPlayer mMediaPlayer;
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

    private long currentPosition;

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
            if(mMediaPlayer.isPlaying()) {
                currentPosition = mMediaPlayer.getCurrentPosition();
                mVideoProgressSeekBar.setProgress((int) currentPosition);
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
            // 网速
            if(mSpeedCoverLayout.getVisibility() == View.VISIBLE) {
                mNetSpeedTextView.setText(MediaUtils.getNetSpeed(getApplicationContext()));
            }
            handler.postDelayed(this, 1000);
        }
    };

    private BatteryStatusBroadcastReceiver batteryStatusBroadcastReceiver;

    private AudioManager audioManager;

    private int screenWidth, screenHeight, mVideoWidth, mVideoHeight;
    private boolean isFullScreen;
    private float startX;
    private long startPosition;
    private boolean videoStartStatusIsPlaying, isSlightShaking = true;

    {
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(tag, "SystemVideoPlayer.onCreate");
        initView();
        initPlayer();
        initEvent();
        try {
            initData();
            initBroadcastReceiver();
            handler.postDelayed(task, 0);
        } catch (IOException e) {
            Log.e(tag, e.getMessage(), e);
        }
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

    private void initPlayer() {
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.release();
        }
        mMediaPlayer = new IjkMediaPlayer();
        mMediaPlayer.setLogEnabled(true);
        // 开启硬解码
        // mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
        mMediaPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mediaPlayer) {
                /*
                videoHeight = mediaPlayer.getVideoHeight();
                videoWidth = mediaPlayer.getVideoWidth();
                Log.w(tag, "mMediaPlayer.onPrepared[videoHeight=" + videoHeight + ", videoWidth=" + videoWidth + "]");
                if(isFullScreen) {
                    fitInside(screenWidth, screenHeight, videoWidth, videoHeight);
                }
                 */
                //mediaPlayer.setLooping(true);
                long duration = mediaPlayer.getDuration();
                mVideoProgressSeekBar.setMax((int) duration);
                String endTime = MediaUtils.formatDuration(duration);
                mVideoEndTimeTextView.setText(endTime);
                String currentTime = MediaUtils.formatDuration(currentPosition);
                mVideoCurrentTimeTextView.setText(currentTime);
                mediaPlayer.seekTo(currentPosition);
                mediaPlayer.start();
                mSpeedCoverLayout.setVisibility(View.GONE);

                mediaPlayer.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(IMediaPlayer mp) {
                        // 记录拖动的位置，用于分析用户观看习惯
                    }
                });
            }
        });

        mMediaPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mediaPlayer, int what, int extra) {
                Log.e(tag, "mMediaPlayer.onError - " + what + " - " + extra);
                //IjkMediaPlayer.
                //1.播放的视频格式不支持--跳转到万能播放器继续播放
                //2.播放网络视频的时候，网络中断---1.如果网络确实断了，可以提示用于网络断了；2.网络断断续续的，重新播放
                //3.播放的时候本地文件中间有空白---下载做完成
                mSpeedCoverLayout.setVisibility(View.GONE);
                mMsgTextView.setText("加载视频失败");
                mMsgCoverLayout.setVisibility(View.VISIBLE);
                return true;
            }
        });
        mMediaPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mediaPlayer) {
                Log.w(tag, "mMediaPlayer.onCompletion");
                if(itemIndex < mediaItems.size() - 1) { //自动播放下一个视频
                    try {
                        next();
                    } catch (IOException e) {
                        Log.e(tag, e.getMessage(), e);
                    }
                }
            }
        });
        mMediaPlayer.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                Log.w(tag, "onInfo - " + what + " - " + extra);
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.w(tag, "MEDIA_INFO_BUFFERING_START");
                        mSpeedCoverLayout.setVisibility(View.VISIBLE);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        Log.w(tag, "MEDIA_INFO_BUFFERING_END");
                        mSpeedCoverLayout.setVisibility(View.GONE);
                        break;
                    default:;
                }
                return false;
            }
        });
        mMediaPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(IMediaPlayer mp, int width, int height,
                                           int sar_num, int sar_den) {
                mVideoWidth = mp.getVideoWidth();
                mVideoHeight = mp.getVideoHeight();
                Log.w(tag, "onVideoSizeChanged[videoWidth=" + mVideoWidth + ", videoHeight=" + mVideoHeight + "]");
                if (mVideoWidth != 0 && mVideoHeight != 0) {
                    if(isFullScreen) {
                        fitInside();
                    } else {
                        suit();
                    }
                }
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int percent) {
                Log.w(tag, "onBufferingUpdate[percent=" + percent + "]");
                // 视频缓冲
                int max = mVideoProgressSeekBar.getMax();
                if(mVideoProgressSeekBar.getSecondaryProgress() < max) {
                    mVideoProgressSeekBar.setSecondaryProgress(mVideoProgressSeekBar.getMax() * percent / 100);
                }
            }
        });
    }

    private void initEvent() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.w(tag, "SurfaceHolder.surfaceCreated");
                mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.w(tag, "SurfaceHolder.surfaceChanged");
            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.w(tag, "SurfaceHolder.surfaceDestroyed");
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
                    mMediaPlayer.seekTo(progress);
                    mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(progress));
                    resetVideoControllerCountdown();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.w(tag, "mVideoProgressSeekBar.onStartTrackingTouch");
                isMediaPlayerPlaying = mMediaPlayer.isPlaying();
                if(isMediaPlayerPlaying) {
                    mMediaPlayer.pause(); // 移动进度条的时候先暂停播放
                }
                resetVideoControllerCountdown();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.w(tag, "mVideoProgressSeekBar.onStopTrackingTouch");
                if(isMediaPlayerPlaying) {
                    mMediaPlayer.start(); // 继续播放
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
                Log.w(tag, "GestureDetector.onLongPress - " + isSlightShaking);
                if(isSlightShaking) { // 如果不是大幅度划屏，则开关视频播放
                    exePauseOrStart();
                }
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.w(tag, "GestureDetector.onDoubleTap");
                fullScreenToggle();
                gestureDetector.setIsLongpressEnabled(false); // 如果不禁用“长按”，则“双击”后会继续执行“长按”
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Log.w(tag, "GestureDetector.onSingleTapConfirmed");
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

    private void initData() throws IOException {
        df = new SimpleDateFormat("HH:mm:ss");
        Intent intent = getIntent();
        mediaItems = intent.getParcelableArrayListExtra("data");

        if(mediaItems != null && !mediaItems.isEmpty()) {
            itemIndex = intent.getIntExtra("itemIndex", -1);
            MediaItem mediaItem = mediaItems.get(itemIndex);
            mMediaPlayer.setDataSource(this, mediaItem.getUri());
            mMediaPlayer.prepareAsync();
            mVideoTitleTextView.setText(mediaItem.getName());
        }
        videoUri = intent.getData();
        if(videoUri != null) {
            mMediaPlayer.setDataSource(this, videoUri);
            mMediaPlayer.prepareAsync();
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
        setContentView(R.layout.activity_ijkplayer);
        mVideoController = findViewById(R.id.videoController);
        mSurfaceView = findViewById(R.id.surfaceView);
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
        try {
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
        } catch (IOException e) {
            Log.w(tag, e.getMessage(), e);
        }
    }

    private void showCompatibilityInfo() {
        new AlertDialog.Builder(this)
                .setTitle("提示信息")
                .setMessage("当花屏时，可以切换到系统播放器。\n现在是否切换到系统播放器？")
                .setNegativeButton("取消", null)
                .setPositiveButton("切换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchSystemVideoPlayer();
                    }
                })
                .show();
    }

    private void switchSystemVideoPlayer() {
        Intent intent = getIntent();
        intent.setClass(IVideoPlayer.this, SystemVideoPlayer.class);
        intent.putExtra("itemIndex", itemIndex);
        startActivity(intent);
        finish();
    }

    private void fullScreenToggle() {
        isFullScreen = !isFullScreen;
        if(isFullScreen) {
            fitInside();
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
        ViewGroup.LayoutParams params = mSurfaceView.getLayoutParams();
        // params.height = ((ViewGroup) mSurfaceView.getParent()).getWidth() * mVideoHeight / mVideoWidth;
        params.height = screenWidth * mVideoHeight / mVideoWidth;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT; // 等价screenWidth
        mSurfaceView.setLayoutParams(params);
    }

    private boolean isPortrait() {
        return getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    }

    private boolean isLandscape() {
        return getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    }

    private void fitInside() {
        Log.w(tag, "fitInside[screenWidth=" + screenWidth
                + ", screenHeight=" + screenHeight
                + ", videoWidth=" + mVideoWidth
                + ", videoHeight=" + mVideoHeight + "]");
        ViewGroup.LayoutParams params;
        if(mVideoHeight > mVideoWidth) {
            if(!isPortrait()) { // 需要切成竖屏
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if(mVideoWidth * screenHeight > mVideoHeight * screenWidth) {
                params = mSurfaceView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mSurfaceView.setLayoutParams(params);
                Log.w(tag, "fitInside - 竖屏 - 宽度撑满");
            } else {
                params = mSurfaceView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mSurfaceView.setLayoutParams(params);
                Log.w(tag, "fitInside - 竖屏 - 高度撑满");
            }
        } else {
            if(!isLandscape()) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            if(mVideoWidth * screenWidth > mVideoHeight * screenHeight) {
                params = mSurfaceView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                mSurfaceView.setLayoutParams(params);
                Log.w(tag, "fitInside - 横屏 - 宽度撑满");
            } else {
                params = mSurfaceView.getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mSurfaceView.setLayoutParams(params);
                Log.w(tag, "fitInside - 横屏 - 高度撑满");
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
        Log.w(tag, "onTouchEvent - " + event.getAction());
        gestureDetector.onTouchEvent(event);
        resetVideoControllerCountdown();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                videoStartStatusIsPlaying = mMediaPlayer.isPlaying();
                startPosition = currentPosition;
                startX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float distance = event.getX() - startX;
                isSlightShaking = Math.abs(distance) < 6;
                if(!isSlightShaking) { // 防抖动
                    if(videoStartStatusIsPlaying && mMediaPlayer.isPlaying()) {
                        mMediaPlayer.pause();
                    }
                    int positionOffset = (int ) (distance / (isFullScreen ? screenHeight : screenWidth) * mVideoProgressSeekBar.getMax());
                    currentPosition = startPosition + positionOffset;
                    mVideoProgressSeekBar.setProgress((int) currentPosition);
                    mMediaPlayer.seekTo(currentPosition);
                    mVideoCurrentTimeTextView.setText(MediaUtils.formatDuration(currentPosition));
                }
                break;
            case MotionEvent.ACTION_UP:
                if(videoStartStatusIsPlaying && !mMediaPlayer.isPlaying()
                        && !isSlightShaking) { // 如果是轻微抖动，则不操作。避免影响长按事件的响应效果
                    mMediaPlayer.start();
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

    private void resetMediaPlayer() {
        initPlayer();
        mMediaPlayer.setDisplay(mSurfaceView.getHolder());
        mVideoProgressSeekBar.setSecondaryProgress(0);
        currentPosition = 0;
    }

    private void prev() throws IOException {
        resetMediaPlayer();
        mMediaPlayer.setDataSource(this, mediaItems.get(--itemIndex).getUri());
        mMediaPlayer.prepareAsync();
        mVideoTitleTextView.setText(mediaItems.get(itemIndex).getName());
        mMsgCoverLayout.setVisibility(View.GONE);
        mSpeedCoverLayout.setVisibility(View.VISIBLE);
        refreshBottomControlBarBtnStatus();
    }

    private void next() throws IOException {
        resetMediaPlayer();
        mMediaPlayer.setDataSource(this, mediaItems.get(++itemIndex).getUri());
        mMediaPlayer.prepareAsync();
        mVideoTitleTextView.setText(mediaItems.get(itemIndex).getName());
        mMsgCoverLayout.setVisibility(View.GONE);
        mSpeedCoverLayout.setVisibility(View.VISIBLE);
        refreshBottomControlBarBtnStatus();
    }

    private void exePauseOrStart() {
        if(mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_play_selector);
        } else {
            mMediaPlayer.start();
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
    protected void onStop() {
        super.onStop();
        mMediaPlayer.pause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mMediaPlayer.start();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.w(tag, "onDetachedFromWindow");
        mMediaPlayer.release();
        mMediaPlayer = null;
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    protected void onDestroy() { // 比onDetachedFromWindow先执行
        super.onDestroy();
        Log.w(tag, "onDestroy");
        unregisterReceiver(this.batteryStatusBroadcastReceiver);
        this.batteryStatusBroadcastReceiver = null;
        this.handler.removeCallbacks(task);
        this.handler = null;
    }
}