package org.nsdev.apps.superhappyhackmap.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.nsdev.apps.superhappyhackmap.model.DatabaseManager;

import java.util.Date;

/**
 * Just makes sure to reset the alarm if the phone has rebooted.
 * <p/>
 * Created by neal on 15-04-01.
 */
public class AutoStart extends BroadcastReceiver {
    public static final String TAG = "AutoStart";

    @Override
    public void onReceive(Context context, Intent broadcastIntent) {
        Date mostRecentHackTime = DatabaseManager.getInstance().getMostRecentHackTime();
        HackReceiver.create23HourAlarm(context, mostRecentHackTime);
    }
}
