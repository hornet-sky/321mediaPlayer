package com.example.mobileplayer.component;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.mobileplayer.IAudioPlayerService;
import com.example.mobileplayer.R;
import com.example.mobileplayer.entity.Lyric;
import com.example.mobileplayer.service.AudioPlayerService;
import com.example.mobileplayer.utils.MediaUtils;
import com.terry.AudioFx.BaseVisualizerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.List;

public class AudioPlayer extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    public static final String AUDIO_PLAYER_PLAY_MODE = "audio_player_play_mode";
    public static final String AUDIO_PLAYER_LYRIC_DISPLAY = "audio_player_lyric_display";
    private ImageView mPlayingMatrixImageView;
    private BaseVisualizerView mAudioVisualizerView;
    private TextView mArtistNameTextView;
    private TextView mAudioNameTextView;
    private LinearLayout mLinearLayout;
    private Button mModeBtn;
    private Button mPrevBtn;
    private Button mPauseOrStartBtn;
    private Button mNextBtn;
    private Button mLyricBtn;
    private SeekBar mAudioProgressSeekBar;
    private TextView mAudioTimeTextView;
    private LyricsView mLyricsView;
    private ImageView mAudioDefCoverImageView;
    private AnimationDrawable matrixAnimDrawable;

    private IAudioPlayerService iAudioPlayerService;
    private int currentPlayMode;

    private boolean isLyricDisplay;

    private long duration;

    private String intentFrom;

    private Visualizer mVisualizer;

    private Handler handler = new Handler();
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            try {
                if(iAudioPlayerService.isPlaying()) {
                    int currentPosition = (int) iAudioPlayerService.getCurrentPosition();
                    mAudioTimeTextView.setText(MediaUtils.formatDuration(currentPosition) + " / " + MediaUtils.formatDuration(duration));
                    mAudioProgressSeekBar.setProgress(currentPosition);
                    mLyricsView.setPosition(currentPosition);
                }
                handler.postDelayed(this, 1000);
            } catch(RemoteException e) {
                Log.e("myTag12", e.getMessage(), e);
            }
        }
    };

    private ServiceConnection mConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.w("myTag11", "ServiceConnection.onServiceConnected - " + intentFrom);
            iAudioPlayerService = IAudioPlayerService.Stub.asInterface(service);
            try {
                if("audioFragment".equals(intentFrom)) {

                } else if("audioNotification".equals(intentFrom)) {
                    // iAudioPlayerService.requireRefreshAudioInfoDisplay();
                }
                boolean isPlaying = iAudioPlayerService.isPlaying();
                mPauseOrStartBtn.setBackgroundResource(isPlaying ? R.drawable.btn_audio_pause_selector : R.drawable.btn_audio_play_selector);
                if(isPlaying) {
                    startMatrixDrawableAnim();
                } else {
                    stopMatrixDrawableAnim();
                }
            } catch (RemoteException e) {
                Log.e("myTag11", e.getMessage(), e);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w("myTag11", "ServiceConnection.onServiceDisconnected");
            iAudioPlayerService = null;
        }
    };

    private void startMatrixDrawableAnim() {
        if(!matrixAnimDrawable.isRunning()) matrixAnimDrawable.start();
    }

    private void stopMatrixDrawableAnim() {
        if(matrixAnimDrawable.isRunning()) matrixAnimDrawable.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        initView();
        initEvent();
        try {
            initData();
            startAndConnectAudioPlayerService();
        } catch (RemoteException e) {
            Log.e("myTag11", e.getMessage(), e);
        }
    }

    private void initView() {
        mPlayingMatrixImageView = (ImageView) findViewById(R.id.playingMatrixImageView);
        mAudioVisualizerView = (BaseVisualizerView) findViewById(R.id.audioVisualizerView);
        mArtistNameTextView = (TextView) findViewById(R.id.artistNameTextView);
        mAudioNameTextView = (TextView) findViewById(R.id.audioNameTextView);
        mLinearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        mModeBtn = (Button) findViewById(R.id.modeBtn);
        mPrevBtn = (Button) findViewById(R.id.prevBtn);
        mPauseOrStartBtn = (Button) findViewById(R.id.pauseOrStartBtn);
        mNextBtn = (Button) findViewById(R.id.nextBtn);
        mLyricBtn = (Button) findViewById(R.id.lyrcBtn);
        mAudioProgressSeekBar = (SeekBar) findViewById(R.id.audioProgressSeekBar);
        mAudioTimeTextView = (TextView) findViewById(R.id.audioTimeTextView);
        mLyricsView = (LyricsView) findViewById(R.id.lyricsTextView);
        mAudioDefCoverImageView = (ImageView) findViewById(R.id.audioDefCoverImageView); // 可以加上动画做成旋转的唱片
    }

    private void initEvent() {
        mModeBtn.setOnClickListener(this);
        mPrevBtn.setOnClickListener(this);
        mPauseOrStartBtn.setOnClickListener(this);
        mNextBtn.setOnClickListener(this);
        mLyricBtn.setOnClickListener(this);

        mAudioProgressSeekBar.setOnSeekBarChangeListener(this);

    }

    private void initData() throws RemoteException {
        currentPlayMode = getPlayModeFromSp();
        mModeBtn.setBackgroundResource(getModeBtnBgDrawableSelectorByPlayMode(currentPlayMode));

        isLyricDisplay = getLyricDisplayFromSp();
        mLyricsView.setVisibility(isLyricDisplay ? View.VISIBLE : View.INVISIBLE);
        mAudioDefCoverImageView.setVisibility(isLyricDisplay ? View.GONE : View.VISIBLE);

        matrixAnimDrawable = (AnimationDrawable) mPlayingMatrixImageView.getDrawable();
    }

    private int getPlayModeFromSp() {
        SharedPreferences sp = getSharedPreferences("media_player", MODE_PRIVATE);
        return sp.getInt(AUDIO_PLAYER_PLAY_MODE, AudioPlayerService.PLAY_MODE_NORMAL_ORDER);
    }

    private void setPlayModeToSp(int playMode) {
        SharedPreferences sp = getSharedPreferences("media_player", MODE_PRIVATE);
        sp.edit().putInt(AUDIO_PLAYER_PLAY_MODE, playMode).commit();
    }

    private boolean getLyricDisplayFromSp() {
        SharedPreferences sp = getSharedPreferences("media_player", MODE_PRIVATE);
        return sp.getBoolean(AUDIO_PLAYER_LYRIC_DISPLAY, true);
    }

    private void setLyricDisplayToSp(boolean isLyricDisplay) {
        SharedPreferences sp = getSharedPreferences("media_player", MODE_PRIVATE);
        sp.edit().putBoolean(AUDIO_PLAYER_LYRIC_DISPLAY, isLyricDisplay).commit();
    }

    private int getModeBtnBgDrawableSelectorByPlayMode(int playMode) {
        try {
            Field field = R.drawable.class.getField("btn_audio_mode" + playMode + "_selector");
            return field.getInt(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("myTag11", e.getMessage(), e);
            return AudioPlayerService.PLAY_MODE_NORMAL_ORDER;
        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.modeBtn:
                    currentPlayMode = currentPlayMode % 3 + 1;
                    mModeBtn.setBackgroundResource(getModeBtnBgDrawableSelectorByPlayMode(currentPlayMode));
                    setPlayModeToSp(currentPlayMode);
                    iAudioPlayerService.setPlayMode(currentPlayMode);
                    break;
                case R.id.prevBtn:
                    iAudioPlayerService.prev();
                    break;
                case R.id.pauseOrStartBtn:
                    if(iAudioPlayerService.isPlaying()) {
                        iAudioPlayerService.pause();
                        mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_audio_play_selector);
                        stopMatrixDrawableAnim();
                    } else {
                        iAudioPlayerService.start();
                        mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_audio_pause_selector);
                        startMatrixDrawableAnim();
                    }
                    break;
                case R.id.nextBtn:
                    iAudioPlayerService.next();
                    break;
                case R.id.lyrcBtn:
                    toggleLyricsDisplay();
                    break;
            }
        } catch(RemoteException e) {
            Log.e("myTag", e.getMessage(), e);
        }
    }

    private void toggleLyricsDisplay() {
        isLyricDisplay = !isLyricDisplay;
        setLyricDisplayToSp(isLyricDisplay);
        mLyricsView.clearAnimation(); // 需要先把动画停了，不然设置可见性没效果

        // 可以添加过渡动画使两个页面翻转切换
        mLyricsView.setVisibility(isLyricDisplay ? View.VISIBLE : View.GONE);
        mAudioDefCoverImageView.setVisibility(isLyricDisplay ? View.GONE : View.VISIBLE);
    }

    private void startAndConnectAudioPlayerService() throws RemoteException {
        Log.w("myTag11", "AudioPlayer.startAudioPlayerService");
        Intent intent = getIntent();
        intent.setClass(this, AudioPlayerService.class);
        intentFrom = intent.getStringExtra("intentFrom");
        if("audioFragment".equals(intentFrom)) {
            intent.putExtra(AUDIO_PLAYER_PLAY_MODE, currentPlayMode);
        }
        bindService(intent, mConn, BIND_AUTO_CREATE);
        startService(intent); // 调用startService可以防止service实例重复创建
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onRefreshAudioInfoDisplay(RefreshAudioInfoDisplayEvent event) {
        Log.w("myTag12", "audioPlayer.onRefreshAudioInfoDisplay");
        mArtistNameTextView.setText(event.artist);
        mAudioNameTextView.setText(event.audioName);
        mAudioTimeTextView.setText(MediaUtils.formatDuration(event.position) + " / " + MediaUtils.formatDuration(event.duration));
        mAudioProgressSeekBar.setMax((int) event.duration);
        mAudioProgressSeekBar.setProgress((int) event.position);
        this.duration = event.duration;
        handler.removeCallbacks(task);
        handler.postDelayed(task, 0);
        try {
            if(iAudioPlayerService != null && iAudioPlayerService.isPlaying()) {
                mPauseOrStartBtn.setBackgroundResource(R.drawable.btn_audio_pause_selector);
                startMatrixDrawableAnim();
            }
            mLyricsView.setLyrics(MediaUtils.loadLyrics(this, event.audioName));
            Bitmap bitmap = MediaUtils.getAlbumArt(this, event.albumId); // /storage/emulated/0/Android/data/com.android.providers.media/albumthumbs/1605787904109
            if(bitmap != null) {
                mAudioDefCoverImageView.setImageBitmap(bitmap);
            } else {
                mAudioDefCoverImageView.setImageResource(R.drawable.music_default_bg);
            }
            int audioSessionId = iAudioPlayerService.getAudioSessionId();
            Log.w("myTag17", "audioSessionId - " + audioSessionId);
            if(mVisualizer != null) {
                mVisualizer.release();
            }
            mVisualizer = new Visualizer(audioSessionId);
            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            mAudioVisualizerView.setVisualizer(mVisualizer);
            mVisualizer.setEnabled(true);
            Log.w("myTag15", "album=" + event.album + ", albumId=" + event.albumId);
        } catch (RemoteException e) {
            Log.e("myTag13", e.getMessage(), e);
        }
    }

    @Override
    protected void onStart() {
        Log.w("myTag11", "AudioPlayer.onStart");
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        Log.w("myTag11", "AudioPlayer.onStop");
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w("myTag11", "AudioPlayer.onDestroy");
        unbindService(mConn);
        handler.removeCallbacks(task);
        if(mVisualizer != null) {
            mVisualizer.release();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(fromUser) {
            try {
                iAudioPlayerService.setPosition(progress);
            } catch (RemoteException e) {
                Log.e("myTag12", e.getMessage(), e);
            }
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public static class RefreshAudioInfoDisplayEvent {
        public String artist;
        public String audioName;
        public long duration;
        public long position;
        public String album;
        public long albumId;
    }
}