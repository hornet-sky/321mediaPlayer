package com.example.mobileplayer.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobileplayer.IAudioPlayerService;
import com.example.mobileplayer.R;
import com.example.mobileplayer.component.AudioPlayer;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.invariable.MediaType;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

import static com.example.mobileplayer.component.AudioPlayer.AUDIO_PLAYER_PLAY_MODE;

public class AudioPlayerService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final int PLAY_MODE_NORMAL_ORDER = 1;
    public static final int PLAY_MODE_ALL_REPEAT = 2;
    public static final int PLAY_MODE_SINGLE_REPEAT = 3;

    private List<MediaItem> mediaItems;
    private int itemIdx = -1;
    private int playMode;
    private MediaPlayer mediaPlayer;

    private NotificationChannel notificationChannel;

    public AudioPlayerService() {
        Log.w("myTag11", "AudioPlayerService.constructor");
    }

    @Override
    public void onCreate() {
        Log.w("myTag11", "AudioPlayerService.onCreate");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.w("myTag11", "AudioPlayerService.onBind");
        return binder; // 返回代理人
    }

    private void initData(Intent intent) throws IOException {
        String intentFrom = intent.getStringExtra("intentFrom");
        Log.w("myTag11", "AudioPlayerService.initData[intentFrom=" + intentFrom + "]");
        if("audioFragment".equals(intentFrom)) {
            mediaItems = intent.getParcelableArrayListExtra("data");
            playMode = intent.getIntExtra(AUDIO_PLAYER_PLAY_MODE, 1);
            int oldItemIdx = itemIdx;
            itemIdx = intent.getIntExtra("itemIndex", 0);
            if(oldItemIdx == itemIdx) {
                if(!mediaPlayer.isPlaying()) start();
                requireRefreshAudioInfoDisplay();
            } else {
                initMediaPlayer();
                prepare();
            }
        } else if("audioNotification".equals(intentFrom)) {
            requireRefreshAudioInfoDisplay();
        }
    }

    private void initMediaPlayer() {
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void setPlayMode(int mode) {
        this.playMode = mode;
        mediaPlayer.setLooping(mode == PLAY_MODE_SINGLE_REPEAT);
    }

    public long getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    public String getArtist() {
        return mediaItems.get(itemIdx).getArtist();
    }

    public String getName() {
        return mediaItems.get(itemIdx).getName();
    }

    public String getAlbum() {
        return mediaItems.get(itemIdx).getAlbum();
    }

    public long getAlbumId() {
        return mediaItems.get(itemIdx).getAlbumId();
    }

    private void showNotification() {
        Intent intent = new Intent(this, AudioPlayer.class);
        intent.putExtra("intentFrom", "audioNotification");
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("正在播放：" + mediaItems.get(itemIdx).getName())
                    .setSmallIcon(R.drawable.notification_music_playing)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_music_playing_big))
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pi)
                    .build();
            notificationManager.notify(1001, notification);
            return;
        }
        // 这块应该封装成一个工具类，channel在全局存在一份就行
        if(notificationChannel == null) {
            String id = "channel_audio_info";
            String name = "音乐播放频道";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            //int importance = NotificationManager.IMPORTANCE_HIGH; // 设置成“高”在真机上反而被当作不重要的通知
            notificationChannel = new NotificationChannel(id, name, importance);
            notificationChannel.enableLights(true);
            notificationChannel.enableVibration(true); // 在虚拟机上有效果，但在小米真机android10系统上没效果，没响声也不震动
            notificationChannel.setVibrationPattern(new long[] {1000, 1000}); // 高版本真机上没效果
            Log.w("myTag13", "shouldVibrate=" + notificationChannel.shouldVibrate() + ", shouldShowLights=" + notificationChannel.shouldShowLights());
            notificationManager.createNotificationChannel(notificationChannel);
        }
        Notification notification = new Notification.Builder(this, notificationChannel.getId())
                .setContentTitle(getString(R.string.app_name))
                .setContentText("正在播放：" + mediaItems.get(itemIdx).getName())
                .setSmallIcon(R.drawable.notification_music_playing) // 左上角小图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notification_music_playing_big)) // 右侧大图标
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pi)
                //.setAutoCancel(true) // true点击通知后 通知消失。默认false
                .build();
        notificationManager.notify(1001, notification);
        // notificationManager.cancel(1001); // 手动取消通知
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.w("myTag12", "AudioPlayerService.onPrepared");
        mp.setLooping(playMode == PLAY_MODE_SINGLE_REPEAT);
        mp.start();
        requireRefreshAudioInfoDisplay();

        showNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(playMode == PLAY_MODE_ALL_REPEAT || (playMode == PLAY_MODE_NORMAL_ORDER && itemIdx < mediaItems.size() - 1)) {
            next();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        next();
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("myTag14", "AudioPlayerService.onStartCommand");
        try {
            initData(intent);
        } catch (IOException e) {
            Log.e("myTag11", e.getMessage(), e);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void start() {
        mediaPlayer.start();
    }

    public void prepare() {
        MediaItem mediaItem = mediaItems.get(itemIdx);
        try {
            mediaPlayer.setDataSource(getApplicationContext(), mediaItem.getUri());
            if(mediaItem.getMediaType() == MediaType.LOCAL_AUDIO) {
                mediaPlayer.prepare();
            } else {
                mediaPlayer.prepareAsync();
            }
        } catch (IOException e) {
            Log.e("myTag11", e.getMessage(), e);
            next();
        }
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public void next() {
        itemIdx = (itemIdx + 1) % mediaItems.size();
        initMediaPlayer();
        prepare();
    }

    public void prev() {
        itemIdx--;
        if(itemIdx < 0) {
            itemIdx = mediaItems.size() - 1;
        }
        initMediaPlayer();
        prepare();
    }

    public int getItemIndex() {
        return this.itemIdx;
    }

    public void setItemIndex(int currentIndex) {
        if(currentIndex > mediaItems.size() - 1) {
            this.itemIdx = mediaItems.size() - 1;
        } else if(currentIndex < 0) {
            this.itemIdx = 0;
        } else {
            this.itemIdx = currentIndex;
        }
    }

    public int getAudioSessionId() {
        return mediaPlayer.getAudioSessionId();
    }

    public void requireRefreshAudioInfoDisplay() {
        AudioPlayer.RefreshAudioInfoDisplayEvent event = new AudioPlayer.RefreshAudioInfoDisplayEvent();
        event.duration = getDuration();
        event.artist = getArtist();
        event.audioName = getName();
        event.position = getCurrentPosition();
        event.album = getAlbum();
        event.albumId = getAlbumId();
        //EventBus.getDefault().postSticky(event);
        EventBus.getDefault().post(event);
    }

    private IAudioPlayerService.Stub binder = new IAudioPlayerService.Stub() {
        @Override
        public int getAudioSessionId() {
            return AudioPlayerService.this.getAudioSessionId();
        }
        @Override
        public void start() throws RemoteException {
            AudioPlayerService.this.start();
        }

        @Override
        public void pause() throws RemoteException {
            AudioPlayerService.this.pause();
        }

        @Override
        public void next() throws RemoteException {
            AudioPlayerService.this.next();
        }

        @Override
        public void prev() throws RemoteException {
            AudioPlayerService.this.prev();
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return AudioPlayerService.this.isPlaying();
        }

        @Override
        public void setPlayMode(int mode) throws RemoteException {
            AudioPlayerService.this.setPlayMode(mode);
        }

        @Override
        public long getCurrentPosition() throws RemoteException {
            return AudioPlayerService.this.getCurrentPosition();
        }

        @Override
        public long getDuration() throws RemoteException {
            return AudioPlayerService.this.getDuration();
        }

        @Override
        public String getArtist() throws RemoteException {
            return AudioPlayerService.this.getArtist();
        }

        @Override
        public String getName() throws RemoteException {
            return AudioPlayerService.this.getName();
        }

        @Override
        public void requireRefreshAudioInfoDisplay() throws RemoteException {
            Log.w("myTag12", "IAudioPlayerService.Stub.requireRefreshAudioInfoDisplay");
            AudioPlayerService.this.requireRefreshAudioInfoDisplay();
        }

        @Override
        public void setPosition(int position) throws RemoteException {
            AudioPlayerService.this.seekTo(position);
        }

        @Override
        public int getItemIndex() throws RemoteException {
            return AudioPlayerService.this.getItemIndex();
        }

        @Override
        public void setItemIndex(int currentIndex) throws RemoteException {
            AudioPlayerService.this.setItemIndex(currentIndex);
        }

        @Override
        public void prepare() throws RemoteException {
            AudioPlayerService.this.prepare();
        }
    };
}