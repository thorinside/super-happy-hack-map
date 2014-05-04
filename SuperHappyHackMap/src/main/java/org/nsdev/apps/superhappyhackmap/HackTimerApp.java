package org.nsdev.apps.superhappyhackmap;

import android.app.Application;

import com.squareup.otto.Bus;

/**
 * Created by neal 13-03-11 10:16 PM
 */
public class HackTimerApp extends Application
{
    private static Bus bus;

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialized the database
        DatabaseManager.init(this);

        bus = new Bus();
        bus.register(this);

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
