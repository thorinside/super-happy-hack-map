package org.nsdev.apps.superhappyhackmap;

import android.app.Application;

import com.squareup.otto.Bus;

import org.nsdev.apps.superhappyhackmap.model.DatabaseManager;

/**
 * Created by neal 13-03-11 10:16 PM
 */
public class HackTimerApp extends Application
{
    private static Bus bus;
    private static HackTimerApp instance;

    @Override
    public void onCreate()
    {
        instance = this;

        super.onCreate();

        // Initialized the database
        DatabaseManager.init(this);

        bus = new Bus();
        bus.register(this);

    }

    public static HackTimerApp getInstance() {
        return instance;
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();

        bus.unregister(this);
    }

    public static Bus getBus()
    {
        return bus;
    }
}
