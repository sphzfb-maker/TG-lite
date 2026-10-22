package com.phsuvo.tg;

import android.app.Application;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Tg.init(this);
    }
}
