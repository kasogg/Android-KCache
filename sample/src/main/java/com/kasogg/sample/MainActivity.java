package com.kasogg.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.kasogg.kcache.KCache;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        KCache kCache = new KCache(this, 1, 50 * 1024 * 1024);
        String key = "key";
        kCache.put(key, "value");
        Log.i("Result from cache", kCache.getAsString(key)); //"value"
        kCache.remove(key);
        Log.i("Result from cache", kCache.getAsString(key) + "");
    }
}
