package com.example.mobileplayer.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public final class SizeUtils {
    private static SizeUtils sInstance;
    private Context context;

    private SizeUtils(Context context) {
        this.context = context;
    }

    public static SizeUtils getInstance(Context context) {
        if(sInstance == null) {
            synchronized (SizeUtils.class) {
                if(sInstance == null) {
                    sInstance = new SizeUtils(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public int px2dip(float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
