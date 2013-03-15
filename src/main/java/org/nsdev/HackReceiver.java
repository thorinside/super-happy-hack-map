package org.nsdev;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * Created by neal 13-03-14 6:23 PM
 */
public class HackReceiver extends BroadcastReceiver
{
    private static final String TAG = "HackReceiver";

    private static long lastUpdate = 0L;
    private static int hackCount;
    private static List<Hack> cachedHacks;
    static Object cacheLock = new Object();
    private static PendingIntent currentAlarm;

    public static void trigger(final Context context)
    {
        if (lastUpdate < SystemClock.elapsedRealtime() - 1000L)
        {
            new HackReceiver().updateNotification(context);
            lastUpdate = SystemClock.elapsedRealtime();
        }
        context.sendBroadcast(new Intent("org.nsdev.ingresstoolbelt.trigger", null, context, HackReceiver.class));
    }

    private boolean updateNotification(Context context)
    {
        NotificationCompat.Builder b = new NotificationCompat.Builder(context);
        b.setPriority(1000);
        b.setContentIntent(PendingIntent
                .getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT));

        Intent deleteIntent = new Intent("org.nsdev.ingresstoolbelt.delete", null, context, HackReceiver.class);
        b.setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));


        b.setContentTitle(context.getResources().getString(R.string.app_name));
        b.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_notification));
        b.setSmallIcon(R.drawable.ic_notification);
        b.setAutoCancel(false);
        RemoteViews v = new RemoteViews(context.getPackageName(),
                R.layout.notification);

        Intent hack = new Intent("org.nsdev.ingresstoolbelt.hack", null, context, HackReceiver.class);
        v.setOnClickPendingIntent(R.id.btn_hack,
                PendingIntent.getBroadcast(context, 0, hack, PendingIntent.FLAG_CANCEL_CURRENT));

        if (LocationLockService.getCurrentLocation() != null)
        {
            Location currentLocation = LocationLockService.getCurrentLocation();

            Hack h = findNearestUnexpiredHack(currentLocation);
            if (h != null)
            {
                float[] results = new float[3];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), h
                        .getLatitude(), h.getLongitude(), results);

                v.setTextViewText(R.id.text_timer, String
                        .format("%.2f %s", results[0], formatTimeString(timeUntilHackable(h))));
                //v.setViewVisibility(R.id.btn_hack, View.INVISIBLE);
            }
            else
            {
                v.setTextViewText(R.id.text_timer, "?:??");
                //v.setViewVisibility(R.id.btn_hack, View.VISIBLE);
            }
        }

        boolean updated = hasNonZeroHackCount();

        //b.setOngoing(updated);

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
        if (intent.getAction().equals("org.nsdev.ingresstoolbelt.delete"))
        {
            Intent i = new Intent(context, LocationLockService.class);
            i.setAction(null);
            context.startService(i);

            AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

            am.cancel(currentAlarm);

        }
        else if (intent.getAction().equals("org.nsdev.ingresstoolbelt.hack"))
        {
            if (LocationLockService.getCurrentLocation() != null)
            {
                Location currentLocation = LocationLockService.getCurrentLocation();

                Hack h = findNearestUnexpiredHack(currentLocation);
                if (h != null)
                {
                    Toast.makeText(context, "Hack allowed in " + formatTimeString(timeUntilHackable(h)), Toast.LENGTH_LONG)
                         .show();
                    //closeStatusBar(context);
                    return;
                }

                Hack hack = new Hack(currentLocation.getLatitude(), currentLocation.getLongitude(), new Date());
                DatabaseManager.getInstance().save(hack);
                Toast.makeText(context, "Hack location recorded.", Toast.LENGTH_LONG).show();
                synchronized (cacheLock)
                {
                    cachedHacks = null;
                }

                updateNotification(context);
                schedNext(context);
            }

        }
        else if (intent.getAction().equals("org.nsdev.ingresstoolbelt.trigger"))
        {
            if (updateNotification(context))
            {
                schedNext(context);
            }
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
            long hackAge = now.getTime() - h.getTimestamp().getTime();

            if (hackAge > 1000 * 60 * 5)
            {
                Log.e(TAG, "Removing old hack.");
                DatabaseManager.getInstance().deleteHack(h);
                synchronized (cacheLock)
                {
                    Log.d(TAG, "Dumping Cache");
                    cachedHacks = null;
                }
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

    private long timeUntilHackable(Hack h)
    {
        Date now = new Date();
        long hackAge = now.getTime() - h.getTimestamp().getTime();
        long timeUntil = (1000 * 60 * 5 - hackAge);
        return timeUntil;
    }

    private boolean hasNonZeroHackCount()
    {
        return hackCount > 0;
    }

    private void closeStatusBar(Context context)
    {
        try
        {
            Object statusBarService = context.getSystemService("statusbar");
            Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
            boolean hasCollapsePanelsMethod = false;
            for (Method method : statusbarManager.getMethods())
            {
                if ("collapsePanels".equals(method.getName()))
                {
                    hasCollapsePanelsMethod = true;
                }
            }
            if (hasCollapsePanelsMethod)
            {
                Method method = statusbarManager.getMethod("collapsePanels");
                method.invoke(statusBarService);
            }
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Uh, yeah.", ex);
        }
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
                context, 9991, new Intent("org.nsdev.ingresstoolbelt.trigger", null, context, HackReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        am.set(AlarmManager.ELAPSED_REALTIME, t, currentAlarm);
    }

}
