package com.example.mobileplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Environment;
import android.os.PatternMatcher;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.net.TrafficStatsCompat;

import com.alibaba.fastjson.JSON;
import com.example.mobileplayer.entity.Lyric;
import com.example.mobileplayer.entity.MediaResult;
import com.example.mobileplayer.entity.NewsItem;
import com.example.mobileplayer.entity.NewsResult;
import com.example.mobileplayer.invariable.Constants;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.invariable.MediaType;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static android.os.Environment.DIRECTORY_MUSIC;

public final class MediaUtils {
    private static final String regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.+)";
    private static final String[] lyricExtensions = new String[] { ".trc", ".lrc", ".txt" };
    private MediaUtils() {}
    public static List<MediaItem> listAllExternalVideos(Context context) {
        List<MediaItem> mediaItems = new ArrayList<>();
        Uri external = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();
        // String selection = MediaStore.Video.Media.TITLE + "=?"; // 查询条件
        // String[] args = new String[] {"Image"}; // 查询条件里的参数
        String[] projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE
        };
        Cursor cursor = resolver.query(external, projection, null, null, null);
        if(cursor != null) {
            String name;
            Uri uri;
            long duration, size;
            while(cursor.moveToNext()) {
                uri = ContentUris.withAppendedId(external, cursor.getLong(0));
                name = cursor.getString(1);
                duration = cursor.getLong(2);
                size = cursor.getLong(3);
                mediaItems.add(new MediaItem(name, uri, duration, size, MediaType.LOCAL_VIDEO));
            }
            cursor.close();
        }
        return mediaItems;
    }

    public static List<MediaItem> listAllExternalAudios(Context context) {
        List<MediaItem> mediaItems = new ArrayList<>();
        Uri external = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();
        // String selection = MediaStore.Audio.Media.TITLE + "=?"; // 查询条件
        // String[] args = new String[] {"MySong"}; // 查询条件里的参数
        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                // MediaStore.Audio.Media.DATA  android 10(Q) 之前可以利用DATA字段获得媒体文件的物理路径，可以用于后期搜索同目录下的歌词文件
        };
        Cursor cursor = resolver.query(external, projection, null, null, null);
        if(cursor != null) {
            long id, albumId;
            String name, artist, album, albumArtist;
            Uri uri;
            long duration, size;
            while(cursor.moveToNext()) {
                id = cursor.getLong(0);
                uri = ContentUris.withAppendedId(external, id);
                name = cursor.getString(1);
                if(name != null) {
                    int idx = name.lastIndexOf(".");
                    if(idx != -1) {
                        name = name.substring(0, idx);
                    }
                }
                duration = cursor.getLong(2);
                size = cursor.getLong(3);
                artist = cursor.getString(4); // 王杰
                album = cursor.getString(5); // 惊世记录Ⅱ
                albumArtist = cursor.getString(6); // 王杰
                albumId = cursor.getLong(7); // 1
                // Log.w("myTag", "album=" + album + ", albumArtist=" + albumArtist + ", albumIdd=" + albumId);
                mediaItems.add(new MediaItem(id, name, uri, duration, size, artist, album, albumId, MediaType.LOCAL_AUDIO));
            }
            cursor.close();
        }
        return mediaItems;
    }

    public static Bitmap getAlbumArt(Context context, long albumId) {
        // String mUriAlbums = "content://media/external/audio/albums";
        ContentResolver resolver = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId);

        String[] projection = new String[] { MediaStore.Audio.Albums.ALBUM_ART, MediaStore.Audio.Albums.ALBUM };
        Cursor cur = resolver.query(uri,  projection, null, null, null);
        String albumArt = null, album;
        if (cur.moveToNext()) {
            albumArt = cur.getString(0);
            album = cur.getString(1);
            Log.w("myTag15", "getAlbumArt[albumArt=" + albumArt + ", album=" + album + "]");
        }
        cur.close();

        if(albumArt != null) {
            try (InputStream inputStream = resolver.openInputStream(Uri.fromFile(new File(albumArt)))) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                Log.w("myTag15", "getAlbumArt[bitmap=" + bitmap + "]");
                return bitmap;
            } catch (IOException e) {
                Log.w("myTag15", e.getMessage(), e);
            }
        }
        return null;
    }

    public static List<Lyric> loadLyrics(Context context, String audioName) {
        List<Lyric> lyrics = new ArrayList<>();
        ContentResolver resolver = context.getContentResolver();
        // 这种读取非媒体文件的方式在android10及以上版本行不通，报错提示没权限。即使显式申请权限了也读不了。
        // 最好是放到应用自己的目录中，随便读写，不需要任何权限。
        File musicDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC);
        Log.w("myTag14", "loadLyrics[audioName=" + musicDir.getAbsolutePath() + "]");
        File lyricFile;
        Uri lyricUri;
        String line;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher;
        long time;
        String content;
        for(int i = 0, len = lyricExtensions.length; i < len; i++) {
            lyricFile = new File(musicDir, audioName + lyricExtensions[i]);
            if(!lyricFile.isFile()) {
                continue;
            }
            lyricUri = Uri.fromFile(lyricFile);
            BufferedReader in = null;
            try {
                BufferedInputStream is = new BufferedInputStream(context.getContentResolver().openInputStream(lyricUri));
                String charset = getCharset(is);
                Log.w("myTag14", "解析歌词得到的编码是：" + charset);
                in = new BufferedReader(new InputStreamReader(is, charset));
                while(null != (line = in.readLine())) {
                    Log.w("myTag14", "歌词：" + line);
                    matcher = pattern.matcher(line);
                    if(matcher.matches()) {
                        time = Long.parseLong(matcher.group(1)) * 60 * 1000
                                + Long.parseLong(matcher.group(2)) * 1000
                                + Long.parseLong(matcher.group(3))
                                - 500; // 500毫秒是为了消除误差，让歌声和歌词的进度一致
                        content = matcher.group(4);
                        lyrics.add(new Lyric(time, content));
                    }
                }
                break;
            } catch (IOException e) {
                Log.e("myTag14", e.getMessage(), e);
            } finally {
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e("myTag14", e.getMessage(), e);
                    }
                }
            }
        }
        Collections.sort(lyrics, new Comparator<Lyric>() {
            @Override
            public int compare(Lyric o1, Lyric o2) {
                return (int) (o1.getPosition() - o2.getPosition());
            }
        });
        return lyrics;
    }

    public static void listNetVideos(int pageNo, int pageSize, String type, NetVideosLoadCallback callback) {
        String url = Constants.NET_VIDEOS_API_URL;
        RequestParams requestParams = new RequestParams(url);
        requestParams.addQueryStringParameter("pageNo", pageNo);
        requestParams.addQueryStringParameter("pageSize", pageSize);
        requestParams.addQueryStringParameter("type", type);
        if(callback != null) {
            callback.beforeRequest();
        }
        x.http().get(requestParams, new Callback.PrepareCallback<String, List<MediaItem>>() {
            @Override
            public void onSuccess(List<MediaItem> result) {
                for(MediaItem item : result) {
                    item.setMediaType(MediaType.NET_VIDEO);
                }
                // http://10.1.56.135:8080/hotel_california/%E5%8A%A0%E5%B7%9E%E6%97%85%E9%A6%86.rmvb
                // 补充一个rmvb格式的视频
                MediaItem item = new MediaItem();
                item.setMediaType(MediaType.NET_VIDEO);
                item.setCoverImg("http://img5.mtime.cn/mg/2019/06/21/175640.99146689_120X90X4.jpg");
                item.setName("加州旅馆");
                item.setTitle("加州旅馆再度归来");
                item.setUri("http://10.1.56.111:8080/hotel_california/%E5%8A%A0%E5%B7%9E%E6%97%85%E9%A6%86.rmvb");
                result.add(0, item);

                if(callback != null) {
                    callback.onSuccess(result);
                }
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                if(callback != null) {
                    callback.onError(ex);
                }
            }
            @Override
            public void onCancelled(CancelledException cex) {
                if(callback != null) {
                    callback.onError(cex);
                }
            }
            @Override
            public void onFinished() {
                if(callback != null) {
                    callback.onFinished();
                }
            }
            @Override
            public List<MediaItem> prepare(String rawData) throws Throwable {
                Log.w("myTag", "MediaUtils.listNetVideos.PrepareCallback.prepare[rawData=" + rawData + "]");
                MediaResult mediaResult = JSON.parseObject(rawData, MediaResult.class);
                return mediaResult.getTrailers();
            }
        });
    }

    public static void listNews(int pageNo, int pageSize, String searchKey, NewsLoadCallback callback) {
        String url = Constants.NEWS_SEARCH_API_URL;
        RequestParams requestParams = new RequestParams(url);
        // &page=1&pagesize=20&qtext=
        requestParams.addQueryStringParameter("page", pageNo);
        requestParams.addQueryStringParameter("pagesize", pageSize);
        requestParams.addQueryStringParameter("qtext", searchKey);
        if(callback != null) {
            callback.beforeRequest();
        }
        x.http().get(requestParams, new Callback.PrepareCallback<String, List<NewsItem>>() {
            @Override
            public void onSuccess(List<NewsItem> result) {
                if(callback != null) {
                    callback.onSuccess(result);
                }
            }
            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                if(callback != null) {
                    callback.onError(ex);
                }
            }
            @Override
            public void onCancelled(CancelledException cex) {
                if(callback != null) {
                    callback.onError(cex);
                }
            }
            @Override
            public void onFinished() {
                if(callback != null) {
                    callback.onFinished();
                }
            }
            @Override
            public List<NewsItem> prepare(String rawData) throws Throwable {
                Log.w("myTag", "MediaUtils.listNews.PrepareCallback.prepare[rawData=" + rawData + "]");
                NewsResult result = JSON.parseObject(rawData, NewsResult.class);
                List<NewsItem> newsItems = result.getList();
                return newsItems == null ? Collections.emptyList() : newsItems;
            }
        });
    }

    public static String formatDuration(long milliseconds) {
        int seconds = (int) (milliseconds / 1000);
        String standardTime;
        if (seconds <= 0) {
            standardTime = "00:00";
        } else if (seconds < 60) {
            standardTime = String.format(Locale.getDefault(), "00:%02d", seconds % 60);
        } else if (seconds < 3600) {
            standardTime = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
        } else {
            standardTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, seconds % 3600 / 60, seconds % 60);
        }
        return standardTime;
    }
    private static long lastTimestamp = 0, lastTotalRxBytes = 0;
    public static String getNetSpeed(Context context) {
        long nowTimestamp = System.currentTimeMillis();
        long nowTotalRxBytes = getTotalRxBytes(context.getApplicationInfo().uid);
        long netSpeed = (nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimestamp - lastTimestamp);
        String netSpeedDisplay = "0kb/s";
        if(netSpeed < 1024) {
            netSpeedDisplay = netSpeed + "b/s";
        } else if(netSpeed < 1024 * 1024) {
            netSpeedDisplay = netSpeed / 1024 + "kb/s";
        } else if(netSpeed < 1024 * 1024 * 1024) {
            netSpeedDisplay = netSpeed / 1024 / 1024 + "mb/s";
        } else {
            netSpeedDisplay = netSpeed / 1024 / 1024 / 1024 + "gb/s";
        }
        lastTimestamp = nowTimestamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return netSpeedDisplay;
    }

    private static long getTotalRxBytes(int uid) {
        return TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : TrafficStats.getTotalRxBytes();
    }
    /**
     * 判断文件编码
     * @param in 输入流
     * @return 编码：GBK,UTF-8,UTF-16LE
     */
    public static String getCharset(BufferedInputStream in) {
        String charset = "GBK";
        byte[] first3Bytes = new byte[3];
        try {
            boolean checked = false;
            in.mark(0);
            int read = in.read(first3Bytes, 0, 3);
            if (read == -1)
                return charset;
            if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE
                    && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE";
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF
                    && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8";
                checked = true;
            }
            in.reset();
            if (!checked) {
                int loc = 0;
                while ((read = in.read()) != -1) {
                    loc++;
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = in.read();
                        if (0x80 <= read && read <= 0xBF)
                            continue;
                        else
                            break;
                    } else if (0xE0 <= read && read <= 0xEF) {
                        read = in.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = in.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                                break;
                            } else
                                break;
                        } else
                            break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e("myTag17", e.getMessage(), e);
        } finally {
            if(in != null) {
                try {
                    in.reset();
                } catch (IOException e) {
                    Log.e("myTag17", e.getMessage(), e);
                }
            }
        }
        return charset;
    }

    public static interface NetVideosLoadCallback {
        void beforeRequest();
        void onSuccess(List<MediaItem> items);
        void onError(Throwable ex);
        void onFinished();
    }

    public static interface NewsLoadCallback {
        void beforeRequest();
        void onSuccess(List<NewsItem> items);
        void onError(Throwable ex);
        void onFinished();
    }
}
