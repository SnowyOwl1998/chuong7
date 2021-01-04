package com.example.chuong7;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    private static final String TAG = "MyService";
    @Override
    public IBinder onBind(Intent intent) {
// we return null as client cannot bind to the service
        return null;
    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "Alarm went off – Service was started");
        stopSelf(); // remember to call stopSelf() when done to free resources
    }
}
