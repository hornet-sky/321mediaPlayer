package com.example.mobileplayer.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.mobileplayer.entity.MediaItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                mediaItems.add(new MediaItem(name, uri, duration, size));
            }
            cursor.close();
        }
        return mediaItems;
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
}
