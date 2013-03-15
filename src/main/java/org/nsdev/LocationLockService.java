package org.nsdev;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.*;
import android.os.Process;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;

/**
 * Service class that starts a listener for GPS location, which should keep the
 * GPS lock indefinitely while running.
 * <p/>
 * Created by neal 13-03-03 1:42 PM
 */
public class LocationLockService extends Service
{
    public static final String ACTION_LOCKGPS = "org.nsdev.ingresstoolbelt.lockgps";
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private static Location currentLocation;
    private Handler mainHandler;

    public static class Hack
    {

        private Date date;
        private Location location;

        public Hack(Date date, Location location)
        {
            this.date = date;
            this.location = location;
        }

        public Date getDate()
        {
            return date;
        }

        public void setDate(Date date)
        {
            this.date = date;
        }

        public Location getLocation()
        {
            return location;
        }

        public void setLocation(Location location)
        {
            this.location = location;
        }
    }

    private static ArrayList<Hack> hacks = new ArrayList<Hack>(15);

    private LocationListener mLocationListener = new LocationListener()
    {

        public void onLocationChanged(final Location location)
        {
            currentLocation = location;
            mainHandler.post(new Runnable()
            {
                public void run()
                {
                    HackTimerApp.getBus().post(location);
                }
            });
        }

        public void onStatusChanged(String s, int i, Bundle bundle)
        {
        }

        public void onProviderEnabled(String s)
        {
        }

        public void onProviderDisabled(String s)
        {
        }
    };

    public static void addHack(Date date, Location location)
    {
        hacks.add(new Hack(date, location));
    }

    public static ArrayList<Hack> getHacks()
    {
        return hacks;
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler
    {
        public ServiceHandler(Looper looper)
        {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.arg2)
            {
                case 0:
                {
                    stopSelf(msg.arg1);
                    break;
                }
                case 1:
                {
                    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setCostAllowed(false);

                    locationManager.requestLocationUpdates(1000L, 2f, criteria, mLocationListener, getLooper());
                    Toast.makeText(LocationLockService.this, "GPS Locked", Toast.LENGTH_SHORT).show();

                    break;
                }
            }
        }
    }

    @Override
    public void onCreate()
    {

        mainHandler = new Handler();

        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent == null)
        {
            return START_STICKY;
        }

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.arg2 = (ACTION_LOCKGPS.equals(intent.getAction())) ? 1 : 0;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy()
    {
        LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(mLocationListener);
        Toast.makeText(this, "GPS Unlocked", Toast.LENGTH_SHORT).show();
    }

    public static Location getCurrentLocation()
    {
        return currentLocation;
    }
}