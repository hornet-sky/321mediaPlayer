package com.example.mobileplayer.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BatteryStatusBroadcastReceiver extends BroadcastReceiver {
    private OnBatteryStatusChangeListener onBatteryStatusChangeListener;
    public BatteryStatusBroadcastReceiver(OnBatteryStatusChangeListener onBatteryStatusChangeListener) {
        this.onBatteryStatusChangeListener = onBatteryStatusChangeListener;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra("level", 0);
        Log.w("myTag3", "BatteryStatusBroadcastReceiver.onReceive[level=" + level + "]");
        if(onBatteryStatusChangeListener != null) {
            onBatteryStatusChangeListener.onChange(level);
        }
    }
    public static interface OnBatteryStatusChangeListener {
        void onChange(int level);
    }
}
