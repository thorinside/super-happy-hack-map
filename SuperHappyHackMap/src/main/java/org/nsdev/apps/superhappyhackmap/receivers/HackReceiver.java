package org.nsdev.apps.superhappyhackmap.receivers;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import org.nsdev.apps.superhappyhackmap.HackTimerApp;
import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.activities.MainActivity;
import org.nsdev.apps.superhappyhackmap.activities.SettingsActivity;
import org.nsdev.apps.superhappyhackmap.events.HackDatabaseUpdatedEvent;
import org.nsdev.apps.superhappyhackmap.model.DatabaseManager;
import org.nsdev.apps.superhappyhackmap.model.Hack;
import org.nsdev.apps.superhappyhackmap.services.HackWindow;
import org.nsdev.apps.superhappyhackmap.services.LocationLockService;
import org.nsdev.apps.superhappyhackmap.utils.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import wei.mark.standout.StandOutWindow;

/**
 * Created by neal 13-03-14 6:23 PM
 */
public class HackReceiver extends BroadcastReceiver {
    private static final boolean DEBUG = false;
    private static final String TAG = "HackReceiver";
    public static final String ACTION_BURNOUT = "org.nsdev.superhappyhackmap.action.BURNOUT";
    public static final String ACTION_DELETE = "org.nsdev.superhappyhackmap.action.DELETE";
    public static final String ACTION_HACK = "org.nsdev.superhappyhackmap.action.HACK";
    public static final String ACTION_TRIGGER = "org.nsdev.superhappyhackmap.action.TRIGGER";
    public static final String ACTION_ALARM = "org.nsdev.superhappyhackmap.action.ALARM";
    public static final String ACTION_MOVE = "org.nsdev.superhappyhackmap.action.MOVE";
    public static final String ACTION_TRANSITION = "org.nsdev.superhappyhackmap.action.TRANSITION";
    public static final String ACTION_SET_COOLDOWN = "org.nsdev.superhappyhackmap.action.SET_COOLDOWN";

    private static long lastUpdate = 0L;
    private static int hackCount;
    private static List<Hack> cachedHacks;
    static Object cacheLock = new Object();
    private static PendingIntent currentAlarm;
    private static HashMap<Integer, PendingIntent> hackAlarms = new HashMap<Integer, PendingIntent>();

    public static void trigger(final Context context) {
        if (lastUpdate < SystemClock.elapsedRealtime() - 1000L) {
            new HackReceiver().updateNotification(context, false);
            lastUpdate = SystemClock.elapsedRealtime();
        }
        context.sendBroadcast(new Intent(ACTION_TRIGGER, null, context, HackReceiver.class));
    }

    private boolean updateNotification(Context context, boolean notifyWithSound) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean trackDistance = sharedPref.getBoolean(SettingsActivity.PREF_TRACK_DISTANCE, true);
        boolean highPriority = sharedPref.getBoolean(SettingsActivity.PREF_HIGH_PRIORITY, true);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context);

        b.setColor(HackTimerApp.getInstance().getResources().getColor(R.color.main_color));

        if (highPriority) {
            b.setPriority(Integer.MAX_VALUE);
        }

        // Fixes flashing notification tray item
        b.setWhen(0);

        b.setContentIntent(PendingIntent
                .getActivity(context, 0,
                        new Intent(context, MainActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT));

        Intent deleteIntent = new Intent(ACTION_DELETE, null, context, HackReceiver.class);
        b.setDeleteIntent(PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT));

        b.setSmallIcon(R.drawable.ic_notification_flat);
        b.setAutoCancel(false);

        Intent hack = new Intent(ACTION_HACK, null, context, HackReceiver.class);
        hack.putExtra("runningHotTime", 300);
        b.addAction(R.drawable.ic_location_place, context.getString(R.string.btn_hack), PendingIntent
                .getBroadcast(context, 0, hack, PendingIntent.FLAG_CANCEL_CURRENT));

        Intent burnout = new Intent(ACTION_BURNOUT, null, context, HackReceiver.class);
        b.addAction(R.drawable.ic_menu_burn, context.getString(R.string.mark_burned_out), PendingIntent
                .getBroadcast(context, 0, burnout, PendingIntent.FLAG_CANCEL_CURRENT));

        if (LocationLockService.getCurrentLocation() != null) {
            Location currentLocation = LocationLockService.getCurrentLocation();

            Hack h = findNearestUnexpiredHack(currentLocation);
            if (h != null && h.timeUntilHackable() >= 0) {
                b.setProgress(h.getMaxWait(), h.getWait(), false);
                if (trackDistance) {
                    float[] results = new float[3];
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), h.getLatitude(), h.getLongitude(), results);
                    b.setContentTitle(String.format("%s %s (%.2fm)", Hack.formatTimeString(h.timeUntilHackable()), context.getString(R.string.til_next_hack), results[0]));
                } else {
                    b.setContentTitle(String.format("%s %s", Hack.formatTimeString(h.timeUntilHackable()), context.getString(R.string.til_next_hack)));
                }

                if (!h.isBurnedOut()) {
                    b.setContentText(context.getString(R.string.hack_unavailable));
                    b.setNumber(h.getHackCount());
                } else {
                    b.setContentText(context.getString(R.string.burned_out));
                    b.setNumber(0);
                }
            } else if (h != null) {
                b.setProgress(0, 0, false);
                b.setContentTitle(context.getString(R.string.app_name));
                b.setContentText(context.getString(R.string.hack_available));
                b.setNumber(h.getHackCount());
            } else {
                b.setProgress(0, 0, false);
                b.setContentTitle(context.getString(R.string.app_name));
                b.setContentText(context.getString(R.string.ready));
            }
        }

        boolean updated = hasNonZeroHackCount();

        if (notifyWithSound) {
            final String notification = sharedPref.getString("pref_notification_ringtone", "DEFAULT");
            if (notification.equals("DEFAULT")) {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                b.setSound(uri);
            } else {
                Uri uri = Uri.parse(notification);
                b.setSound(uri);
            }
            b.setVibrate(new long[]{0, 2000});
        }

        // Wearable extensions
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();
        wearableExtender.setBackground(BitmapFactory.decodeResource(HackTimerApp.getInstance().getResources(), R.drawable.ic_launcher));
        b.extend(wearableExtender);

        b.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, b.build());

        return updated;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_DELETE)) {
            Intent i = new Intent(LocationLockService.ACTION_STOP, null, context, LocationLockService.class);
            context.startService(i);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(currentAlarm);

            for (PendingIntent pendingIntent : hackAlarms.values()) {
                am.cancel(pendingIntent);
            }

            StandOutWindow.closeAll(context, HackWindow.class);

            hackAlarms.clear();

        } else if (intent.getAction().equals(ACTION_HACK)) {
            if (LocationLockService.getCurrentLocation() != null) {
                Location currentLocation = LocationLockService.getCurrentLocation();

                int hackId = intent.getIntExtra("hack_id", -1);
                boolean forced = intent.getBooleanExtra("force", false);

                Hack h;

                if (hackId != -1) {
                    h = DatabaseManager.getInstance().findHackById(hackId);
                } else {
                    h = findNearestUnexpiredHack(currentLocation);
                }

                if (h != null && h.timeUntilHackable() > 0 && !forced) {
                    Toast.makeText(context, String
                            .format(context.getString(R.string.hack_allowed_in), Hack.formatTimeString(h
                                    .timeUntilHackable())), Toast.LENGTH_LONG).show();
                    return;
                }

                Hack hack;

                if (h == null) {
                    hack = new Hack(currentLocation.getLatitude(), currentLocation
                            .getLongitude(), new Date(), new Date());
                    hack.incrementHackCount();
                } else {
                    hack = h;
                    hack.incrementHackCount();
                    hack.setLastHacked(new Date());
                }
                DatabaseManager.getInstance().save(hack);

                if (forced) {
                    Toast.makeText(context, context.getString(R.string.forced_hack), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, context.getString(R.string.hack_location_recorded), Toast.LENGTH_SHORT)
                            .show();

                    // Refire an intent containing the hack_id so that the location service can create and watch
                    // the geofence.

                    Intent i = new Intent(context.getApplicationContext(), LocationLockService.class);
                    i.setAction(LocationLockService.ACTION_HACK);
                    i.putExtra("hack_id", hack.getId());
                    i.putExtra("hack_latitude", hack.getLatitude());
                    i.putExtra("hack_longitude", hack.getLongitude());

                    context.startService(i);

                }
                synchronized (cacheLock) {
                    cachedHacks = null;
                }

                HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());

                // Create an alarm for this hack
                createHackAlarm(context, hack);
                updateNotification(context, false);
                schedNext(context);
            }

        } else if (intent.getAction().equals(ACTION_TRIGGER)) {
            if (updateNotification(context, false)) {
                schedNext(context);
            }
        } else if (intent.getAction().equals(ACTION_ALARM)) {
            boolean withSound = intent.getBooleanExtra("sound", true);
            boolean userDeleted = intent.getBooleanExtra("userDeleted", false);
            updateNotification(context, withSound);
            int hackId = intent.getIntExtra("hack_id", 0);
            if (userDeleted) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.cancel(hackAlarms.get(hackId));
                synchronized (cacheLock) {
                    cachedHacks = null;
                }
            }
            hackAlarms.remove(hackId);
            HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
        } else if (intent.getAction().equals(ACTION_MOVE)) {
            synchronized (cacheLock) {
                cachedHacks = null;
            }
        } else if (intent.getAction().equals(ACTION_BURNOUT)) {
            if (LocationLockService.getCurrentLocation() != null) {
                Location currentLocation = LocationLockService.getCurrentLocation();

                Hack h = findNearestUnexpiredHack(currentLocation);

                if (h == null) {
                    return;
                }

                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                am.cancel(currentAlarm);

                PendingIntent hackPendingIntent = hackAlarms.get(h.getId());
                am.cancel(hackPendingIntent);

                hackAlarms.remove(h.getId());
                h.setBurnedOut(true);
                DatabaseManager.getInstance().save(h);

                HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());

                synchronized (cacheLock) {
                    cachedHacks = null;
                }

                if (updateNotification(context, false)) {
                    schedNext(context);
                }
            }
        } else if (intent.getAction().equals(ACTION_TRANSITION)) {
            int[] hacks = intent.getIntArrayExtra("hacks");
            int transitionType = intent.getIntExtra("transitionType", -1);

            if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                for (int hackId : hacks) {
                    Hack hack = DatabaseManager.getInstance().findHackById(hackId);
                    if (hack != null) {
                        if (hack.isHackable()) {
                            Toast.makeText(context.getApplicationContext(), context
                                    .getString(R.string.hack_available), Toast.LENGTH_SHORT).show();

                            boolean buzz = PreferenceManager.getDefaultSharedPreferences(context)
                                    .getBoolean(SettingsActivity.PREF_BUZZ_IF_HACKABLE, true);

                            if (buzz) {
                                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                                vibrator.vibrate(1000);
                            }
                        } else {
                            Log.w(TAG, "Entered zone not hackable.");
                        }
                    }
                }
            }
        } else if (intent.getAction().equals(ACTION_SET_COOLDOWN)) {
            // First, flush the cache
            synchronized (cacheLock) {
                cachedHacks = null;
            }

            int hack_id = intent.getIntExtra("hack_id", -1);
            if (hack_id == -1) return;

            Hack h = DatabaseManager.getInstance().findHackById(hack_id);

            // Then reschedule the timers for the hack in question
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            PendingIntent hackPendingIntent = hackAlarms.get(h.getId());
            am.cancel(hackPendingIntent);

            hackAlarms.remove(h.getId());

            createHackAlarm(context, h);

            HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
        } else {
            if (DEBUG) {
                Log.e(TAG, "Got: " + intent.getAction());
            }
        }
    }

    private Hack findNearestUnexpiredHack(Location currentLocation) {
        Date now = new Date();

        List<Hack> hacks;
        synchronized (cacheLock) {
            if (cachedHacks != null) {
                if (DEBUG) {
                    Log.d(TAG, "Using cached hacks.");
                }
                hacks = cachedHacks;
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Loading hacks from database.");
                }
                hacks = DatabaseManager.getInstance().getAllHacks();
                cachedHacks = hacks;
            }
        }
        hackCount = hacks.size();

        Hack closestHack = null;
        float closestDistance = Float.MAX_VALUE;

        for (Hack h : hacks) {
            float[] results = new float[3];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), h.getLatitude(), h
                    .getLongitude(), results);
            float distance = results[0];
            if (DEBUG) {
                Log.d(TAG, String.format("Distance: %.2f", distance));
            }

            if (false && h.getHackCount() == 4 && h.timeUntilHackable() <= 0) {
                if (DEBUG) {
                    Log.e(TAG, "Removing old hack.");
                }
                DatabaseManager.getInstance().deleteHack(h);
                synchronized (cacheLock) {
                    if (DEBUG) {
                        Log.d(TAG, "Dumping Cache");
                    }
                    cachedHacks = null;
                }

                HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
            } else {
                if (distance < 40f) {
                    if (distance < closestDistance) {
                        closestHack = h;
                    }
                }
            }
        }

        return closestHack;
    }

    private boolean hasNonZeroHackCount() {
        return hackCount > 0;
    }

    private void schedNext(final Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        long t = SystemClock.elapsedRealtime() + (pm.isScreenOn() ? 1000 : 5000);

        currentAlarm = PendingIntent.getBroadcast(
                context, 9991, new Intent(ACTION_TRIGGER, null, context, HackReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        am.set(AlarmManager.ELAPSED_REALTIME, t, currentAlarm);
    }

    private void createHackAlarm(Context context, Hack hack) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ACTION_ALARM, null, context, HackReceiver.class);
        intent.putExtra("hack_id", hack.getId());

        PendingIntent hackAlarm = PendingIntent
                .getBroadcast(context, hack.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        hackAlarms.put(hack.getId(), hackAlarm);

        long t = SystemClock.elapsedRealtime() + hack.timeUntilHackable();

        am.set(AlarmManager.ELAPSED_REALTIME, t, hackAlarm);
    }

}
