package org.nsdev.apps.superhappyhackmap;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by neal 13-03-14 6:23 PM
 */
public class HackReceiver extends BroadcastReceiver
{
    private static final String TAG = "HackReceiver";
    public static final String ACTION_DELETE = "org.nsdev.superhappyhackmap.delete";
    public static final String ACTION_HACK = "org.nsdev.superhappyhackmap.hack";
    public static final String ACTION_TRIGGER = "org.nsdev.superhappyhackmap.trigger";
    public static final String ACTION_ALARM = "org.nsdev.superhappyhackmap.alarm";

    private static long lastUpdate = 0L;
    private static int hackCount;
    private static List<Hack> cachedHacks;
    static Object cacheLock = new Object();
    private static PendingIntent currentAlarm;
    private static HashMap<Integer, PendingIntent> hackAlarms = new HashMap<Integer, PendingIntent>();

    public static void trigger(final Context context)
    {
        if (lastUpdate < SystemClock.elapsedRealtime() - 1000L)
        {
            new HackReceiver().updateNotification(context, false);
            lastUpdate = SystemClock.elapsedRealtime();
        }
        context.sendBroadcast(new Intent(ACTION_TRIGGER, null, context, HackReceiver.class));
    }

    private boolean updateNotification(Context context, boolean notifyWithSound)
    {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context);
        b.setPriority(1000);
        b.setContentIntent(PendingIntent
                .getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT));

        Intent deleteIntent = new Intent(ACTION_DELETE, null, context, HackReceiver.class);
        b.setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));


        b.setContentTitle(context.getResources().getString(R.string.app_name));
        b.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_notification));
        b.setSmallIcon(R.drawable.ic_notification);
        b.setAutoCancel(false);
        RemoteViews v = new RemoteViews(context.getPackageName(),
                R.layout.notification);

        Intent hack = new Intent(ACTION_HACK, null, context, HackReceiver.class);
        v.setOnClickPendingIntent(R.id.btn_hack,
                PendingIntent.getBroadcast(context, 0, hack, PendingIntent.FLAG_CANCEL_CURRENT));

        if (LocationLockService.getCurrentLocation() != null)
        {
            Location currentLocation = LocationLockService.getCurrentLocation();

            Hack h = findNearestUnexpiredHack(currentLocation);
            if (h != null && h.timeUntilHackable() >= 0)
            {
                float[] results = new float[3];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), h
                        .getLatitude(), h.getLongitude(), results);

                v.setTextViewText(R.id.text_timer, String
                        .format("%.2f %s", results[0], formatTimeString(h.timeUntilHackable())));
                //v.setViewVisibility(R.id.btn_hack, View.INVISIBLE);
            }
            else
            {
                v.setTextViewText(R.id.text_timer, "?:??");
                //v.setViewVisibility(R.id.btn_hack, View.VISIBLE);
            }
        }

        boolean updated = hasNonZeroHackCount();

        if (notifyWithSound)
        {
            Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            b.setSound(defaultUri);
            b.setVibrate(new long[] {0, 200});
        }
        b.setContent(v);

        NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, b.build());

        return updated;
    }


    @Override
    public void onReceive(Context context, Intent intent)
    {
        String a = intent.getAction();
        Log.e(TAG, "onReceive(" + a + ")");
        if (intent.getAction().equals(ACTION_DELETE))
        {
            Intent i = new Intent(context, LocationLockService.class);
            i.setAction(null);
            context.startService(i);

            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(currentAlarm);

            for (PendingIntent pendingIntent : hackAlarms.values())
            {
                am.cancel(pendingIntent);
            }

            hackAlarms.clear();

        }
        else if (intent.getAction().equals(ACTION_HACK))
        {
            if (LocationLockService.getCurrentLocation() != null)
            {
                Location currentLocation = LocationLockService.getCurrentLocation();

                Hack h = findNearestUnexpiredHack(currentLocation);
                if (h != null && h.timeUntilHackable() > 0)
                {
                    Toast.makeText(context, "Hack allowed in " + formatTimeString(h
                            .timeUntilHackable()), Toast.LENGTH_LONG).show();
                    return;
                }

                Hack hack;

                if (h == null)
                {
                    hack = new Hack(currentLocation.getLatitude(), currentLocation
                            .getLongitude(), new Date(), new Date());
                }
                else
                {
                    hack = h;
                    if (hack.isBurnedOut() || hack.getHackCount() == 4)
                    {
                        // Burnout period expired, so reset the first hack time
                        hack.setFirstHacked(new Date());
                        hack.setHackCount(0);
                    }
                    hack.incrementHackCount();
                    hack.setLastHacked(new Date());
                }
                DatabaseManager.getInstance().save(hack);

                Toast.makeText(context, "Hack location recorded.", Toast.LENGTH_LONG).show();
                synchronized (cacheLock)
                {
                    cachedHacks = null;
                }

                HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());

                // Create an alarm for this hack
                createHackAlarm(context, hack);
                updateNotification(context, false);
                schedNext(context);
            }

        }
        else if (intent.getAction().equals(ACTION_TRIGGER))
        {
            if (updateNotification(context, false))
            {
                schedNext(context);
            }
        }
        else if (intent.getAction().equals(ACTION_ALARM))
        {
            updateNotification(context, true);
            hackAlarms.remove(intent.getIntExtra("hack_id", 0));
            HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
        }
        else
        {
            Log.e(TAG, "Got: " + intent.getAction());
        }
    }

    private Hack findNearestUnexpiredHack(Location currentLocation)
    {
        Date now = new Date();

        List<Hack> hacks;
        synchronized (cacheLock)
        {
            if (cachedHacks != null)
            {
                Log.d(TAG, "Using cached hacks.");
                hacks = cachedHacks;
            }
            else
            {
                Log.d(TAG, "Loading hacks from database.");
                hacks = DatabaseManager.getInstance().getAllHacks();
                cachedHacks = hacks;
            }
        }
        hackCount = hacks.size();

        Hack closestHack = null;
        float closestDistance = Float.MAX_VALUE;

        for (Hack h : hacks)
        {
            float[] results = new float[3];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), h.getLatitude(), h
                    .getLongitude(), results);
            float distance = results[0];
            Log.d(TAG, String.format("Distance: %.2f", distance));

            if (false && h.getHackCount() == 4 && h.timeUntilHackable() <= 0)
            {
                Log.e(TAG, "Removing old hack.");
                DatabaseManager.getInstance().deleteHack(h);
                synchronized (cacheLock)
                {
                    Log.d(TAG, "Dumping Cache");
                    cachedHacks = null;
                }

                HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
            }
            else
            {
                if (distance < 40f)
                {
                    if (distance < closestDistance)
                    {
                        closestHack = h;
                    }
                }
            }
        }

        return closestHack;
    }

    private boolean hasNonZeroHackCount()
    {
        return hackCount > 0;
    }

    private String formatTimeString(long ms)
    {
        long s = ms / 1000;
        if (s > 60)
        {
            long m = s / 60;
            s = s - 60 * m;
            return String.format("%d:%s", m, s < 10 ? "0" + s : "" + s);
        }
        else
        {
            return String.format("0:%s", s < 10 ? "0" + s : "" + s);
        }
    }

    private void schedNext(final Context context)
    {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        long t = SystemClock.elapsedRealtime() + (pm.isScreenOn() ? 1000 : 5000);

        currentAlarm = PendingIntent.getBroadcast(
                context, 9991, new Intent(ACTION_TRIGGER, null, context, HackReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        am.set(AlarmManager.ELAPSED_REALTIME, t, currentAlarm);
    }

    private void createHackAlarm(Context context, Hack hack)
    {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_ALARM, null, context, HackReceiver.class);
        intent.putExtra("hack_id", hack.getId());

        PendingIntent hackAlarm = PendingIntent
                .getBroadcast(context, hack.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        hackAlarms.put(hack.getId(), hackAlarm);

        long t = SystemClock.elapsedRealtime() + hack.timeUntilHackable();

        am.set(AlarmManager.ELAPSED_REALTIME, t, hackAlarm);
    }

}
