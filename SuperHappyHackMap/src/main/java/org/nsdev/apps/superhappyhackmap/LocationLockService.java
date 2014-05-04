package org.nsdev.apps.superhappyhackmap;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.os.Process;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Service class that uses the Google Play Services location provider on NO_POWER mode and
 * handles geofence operations.
 * <p/>
 * Created by neal 13-03-03 1:42 PM
 */
public class LocationLockService extends Service
{
    private static final String TAG = "LocationLockService";

    public static final String ACTION_STOP = "org.nsdev.apps.superhappyhackmap.action.STOP";
    public static final String ACTION_MONITOR_LOCATION = "org.nsdev.apps.superhappyhackmap.action.MONITOR_LOCATION";
    public static final String ACTION_FENCEUPDATE = "org.nsdev.apps.superhappyhackmap.action.FENCE_UPDATE";
    public static final String ACTION_HACK = "org.nsdev.apps.superhappyhackmap.action.HACK";

    private static final int MSG_STOP = 0;
    private static final int MSG_START = 1;
    private static final int MSG_FENCE_UPDATE = 2;
    private static final int MSG_HACK = 3;

    private static final int FASTEST_INTERVAL = 1000;
    private static final int INTERVAL = 5000;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private LocationClient locationClient;

    private static Location currentLocation;

    private LocationListener locationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location)
        {
            Log.d("TAG", "Got a location update: " + location.toString());
            currentLocation = location;
        }
    };

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
                case MSG_STOP:
                {
                    stopSelf(msg.arg1);
                    break;
                }
                case MSG_START:
                {
                    locationServiceConnect();

                    break;
                }
                case MSG_FENCE_UPDATE:
                {
                    Intent intent = (Intent)msg.getData().get("intent");

                    if (LocationClient.hasError(intent))
                    {
                        // Get the error code with a static method
                        int errorCode = LocationClient.getErrorCode(intent);
                        // Log the error
                        Log.e("ReceiveTransitionsIntentService",
                                "Location Services error: " +
                                        Integer.toString(errorCode));
                        /*
                         * You can also send the error code to an Activity or
                         * Fragment with a broadcast Intent
                         */
                        /*
                         * If there's no error, get the transition type and the IDs
                         * of the geofence or geofences that triggered the transition
                         */
                    }
                    else
                    {
                        // Get the type of transition (entry or exit)
                        int transitionType = LocationClient.getGeofenceTransition(intent);

                        // Test that a valid transition was reported
                        if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                                || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT))
                        {
                            List<Geofence> triggerList =
                                    LocationClient.getTriggeringGeofences(intent);

                            int[] triggerIds = new int[triggerList.size()];

                            for (int i = 0; i < triggerIds.length; i++)
                            {
                                // Store the Id of each geofence
                                triggerIds[i] = Integer.parseInt(triggerList.get(i).getRequestId());
                            }
                            /*
                             * At this point, you can store the IDs for further use
                             * display them, or display the details associated with
                             * them.
                             */

                            Intent transitionIntent = new Intent(HackReceiver.ACTION_TRANSITION, null, getBaseContext(), HackReceiver.class);
                            transitionIntent.putExtra("hacks", triggerIds);
                            transitionIntent.putExtra("transitionType", transitionType);

                            sendBroadcast(transitionIntent);

                        }
                        else
                        {
                            // An invalid transition was reported
                        }
                    }

                    break;
                }
                case MSG_HACK:
                {
                    Intent intent = (Intent)msg.getData().get("intent");
                    double hackLatitude = intent.getDoubleExtra("hack_latitude", 0);
                    double hackLongitude = intent.getDoubleExtra("hack_longitude", 0);
                    int hack_id = intent.getIntExtra("hack_id", -1);
                    boolean deleteHack = intent.getBooleanExtra("delete", false);

                    if (hack_id == -1)
                    {
                        return;
                    }

                    locationServiceConnect();

                    if (locationClient == null) return;

                    if (deleteHack)
                    {
                        Log.d(TAG, "Removing fence for hack " + hack_id);

                        ArrayList<String> fences = new ArrayList<String>();
                        fences.add(String.format("%d", hack_id));
                        locationClient.removeGeofences(fences, new LocationClient.OnRemoveGeofencesResultListener()
                        {
                            @Override
                            public void onRemoveGeofencesByRequestIdsResult(int i, String[] strings)
                            {
                                Log.d(TAG, "Geofences Removed Result: " + i);
                            }

                            @Override
                            public void onRemoveGeofencesByPendingIntentResult(int i, PendingIntent pendingIntent)
                            {
                            }
                        });
                        return;
                    }

                    Geofence.Builder builder = new Geofence.Builder();
                    builder.setCircularRegion(hackLatitude, hackLongitude, 40)
                            .setExpirationDuration(24 * 60 * 60 * 1000)  // Expire a geofence in 24 hours
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                            .setRequestId(String.format("%d", hack_id));
                    Geofence fence = builder.build();

                    List<Geofence> fences = new ArrayList<Geofence>();
                    fences.add(fence);

                    Intent i = new Intent(LocationLockService.ACTION_FENCEUPDATE, null, getBaseContext(), LocationLockService.class);

                    if (i != null)
                    {
                        PendingIntent pendingIntent = PendingIntent.getService(getBaseContext(), 0, i, 0);

                        locationClient.addGeofences(fences, pendingIntent, new LocationClient.OnAddGeofencesResultListener()
                        {
                            @Override
                            public void onAddGeofencesResult(int i, String[] strings)
                            {
                                Log.d(TAG, "add Geofences Result " + i);
                            }
                        });
                    }

                    break;
                }
            }
        }
    }

    private void locationServiceConnect()
    {
        if (locationClient != null)
        {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        locationClient = new LocationClient(this, new GooglePlayServicesClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected(Bundle bundle)
            {
                LocationRequest request = LocationRequest.create();
                request.setPriority(LocationRequest.PRIORITY_NO_POWER);
                request.setFastestInterval(FASTEST_INTERVAL);
                request.setInterval(INTERVAL);

                locationClient.requestLocationUpdates(request, locationListener);
                latch.countDown();
            }

            @Override
            public void onDisconnected()
            {
                locationClient = null;
            }
        }, new GooglePlayServicesClient.OnConnectionFailedListener()
        {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult)
            {
                Log.e("TAG", "Connection failed.");
                locationClient = null;
                latch.countDown();
            }
        }
        );

        locationClient.connect();

        try
        {
            latch.await();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onCreate()
    {
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

        if (ACTION_STOP.equals(intent.getAction()))
        {
            msg.arg2 = MSG_STOP;
        }
        else if (ACTION_MONITOR_LOCATION.equals(intent.getAction()))
        {
            msg.arg2 = MSG_START;
        }
        else if (ACTION_FENCEUPDATE.equals(intent.getAction()))
        {
            msg.arg2 = MSG_FENCE_UPDATE;
        }
        else if (ACTION_HACK.equals(intent.getAction()))
        {
            msg.arg2 = MSG_HACK;
        }

        Bundle b = new Bundle();
        b.putParcelable("intent", intent);
        msg.setData(b);
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
        if (locationClient != null)
        {
            locationClient.disconnect();
            locationClient = null;
        }
    }

    static Location getCurrentLocation()
    {
        return currentLocation;
    }
}