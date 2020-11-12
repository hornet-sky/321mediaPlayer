package com.example.mobileplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.TrafficStats;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.net.TrafficStatsCompat;

import com.alibaba.fastjson.JSON;
import com.example.mobileplayer.entity.MediaResult;
import com.example.mobileplayer.invariable.Constants;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.invariable.MediaType;

import org.xutils.common.Callback;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class MediaUtils {
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

    public static interface NetVideosLoadCallback {
        void beforeRequest();
        void onSuccess(List<MediaItem> items);
        void onError(Throwable ex);
        void onFinished();
    }
}
