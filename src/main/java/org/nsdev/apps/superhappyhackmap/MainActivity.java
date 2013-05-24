package org.nsdev.apps.superhappyhackmap;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends FragmentActivity
{

    private static String TAG = "SHHM";
    private SupportMapFragment mapFragment;

    private boolean hackLocationSet = false;
    private CircleOptions circleOptions;
    private GoogleMap map;
    private ArrayList<Circle> circles = new ArrayList<Circle>();
    private boolean hasZoomed = false;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in onSaveInstanceState(Bundle). <b>Note: Otherwise it is null.</b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setContentView(R.layout.main);

        mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        map = mapFragment.getMap();


        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS)
        {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
            return;
        }

        if (savedInstanceState != null)
        {
            Log.d("TAG", "Restoring instance state.");
            hackLocationSet = savedInstanceState.getBoolean("hackLocationSet", false);
            circleOptions = savedInstanceState.getParcelable("circle");
            if (circleOptions != null && map != null)
            {
                map.addCircle(circleOptions);
            }
        }

        Intent i = new Intent(getBaseContext(), LocationLockService.class);
        i.setAction(LocationLockService.ACTION_LOCKGPS);
        startService(i);

        if (map != null)
        {
            map.setMyLocationEnabled(true);
            UiSettings settings = map.getUiSettings();
            settings.setMyLocationButtonEnabled(true);
            settings.setZoomControlsEnabled(false);
            map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener()
            {
                @Override
                public void onMyLocationChange(Location location)
                {
                    if (!hasZoomed)
                    {
                        map.animateCamera(CameraUpdateFactory
                                .newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 17));
                    }
                    hasZoomed = true;
                }
            });

            map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener()
            {
                @Override
                public void onCameraChange(CameraPosition cameraPosition)
                {
                }
            });

            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng)
                {
                    if (selectedCircleActionMode != null) return;

                    // Find the hack that was tapped
                    List<Circle> tappedCircles = findTappedCircles(latLng.latitude, latLng.longitude);

                    if (tappedCircles.size() > 0)
                    {
                        selectedCircleActionMode = new SelectedCircleActionMode();
                        selectedCircleActionMode.setCircles(tappedCircles);

                        startActionMode(selectedCircleActionMode);
                    }
                }
            });

            //LocationInfo location = new LocationInfo(this.getBaseContext());
        }

        HackReceiver.trigger(getBaseContext());
    }

    SelectedCircleActionMode selectedCircleActionMode = null;

    @Override
    public void onActionModeFinished(ActionMode mode)
    {
        selectedCircleActionMode = null;
        super.onActionModeFinished(mode);
        if (mode.equals(selectedCircleActionMode))
        {
            selectedCircleActionMode = null;
        }
    }

    private List<Circle> findTappedCircles(double latitude, double longitude)
    {

        ArrayList<Circle> circleList = new ArrayList<Circle>();

        for (Circle c : circles)
        {
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, c.getCenter().latitude, c.getCenter().longitude, results);
            if (results[0] <= c.getRadius())
                circleList.add(c);
        }

        return circleList;
    }

    @Subscribe
    public void onCirclesDeleted(CirclesDeletedEvent evt)
    {
        for (Circle c : evt.getCircles())
        {
            LatLng center = c.getCenter();
            Hack h = DatabaseManager.getInstance().findHackAt(center);

            Intent intent = new Intent(HackReceiver.ACTION_ALARM, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("sound", false);
            intent.putExtra("userDeleted", true);

            DatabaseManager.getInstance().deleteHack(h);

            sendBroadcast(intent);
        }
    }

    @Subscribe
    public void onCirclesHacked(CirclesHackedEvent evt)
    {
        for (Circle c : evt.getCircles())
        {
            LatLng center = c.getCenter();
            Hack h = DatabaseManager.getInstance().findHackAt(center);

            Intent intent = new Intent(HackReceiver.ACTION_ALARM, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("sound", false);
            intent.putExtra("userDeleted", true);

            sendBroadcast(intent);

            intent = new Intent(HackReceiver.ACTION_HACK, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("force", true);

            sendBroadcast(intent);
        }
    }

    @Subscribe
    public void onCirclesBurned(CirclesBurnedEvent evt)
    {
        for (Circle c : evt.getCircles())
        {
            LatLng center = c.getCenter();
            Hack h = DatabaseManager.getInstance().findHackAt(center);
            h.setHackCount(4);

            Intent intent = new Intent(HackReceiver.ACTION_ALARM, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("sound", false);
            intent.putExtra("userDeleted", true);

            DatabaseManager.getInstance().save(h);

            sendBroadcast(intent);
        }
    }

    @Subscribe
    public void onHackDatabaseUpdated(HackDatabaseUpdatedEvent evt)
    {
        for (Circle c : circles)
        {
            c.remove();
        }

        circles.clear();

        List<Hack> hacks = DatabaseManager.getInstance().getAllHacks();
        for (Hack h : hacks)
        {
            CircleOptions opts = new CircleOptions();
            opts.center(new LatLng(h.getLatitude(), h.getLongitude()));
            opts.radius(40);
            opts.fillColor(Color.argb(64, 255, 0, 0));
            opts.strokeColor(Color.RED);
            opts.strokeWidth(2);

            if (h.isBurnedOut())
            {
                opts.strokeColor(Color.BLACK);
                opts.fillColor(Color.argb(64, 0, 0, 0));
            }
            else if (h.timeUntilHackable() <= 0)
            {
                opts.strokeColor(Color.GREEN);
                opts.fillColor(Color.argb(64, 0, 255, 0));
            }

            circles.add(map.addCircle(opts));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        Log.d("TAG", "Saving instance state.");
        outState.putBoolean("hackLocationSet", hackLocationSet);
        outState.putParcelable("circle", circleOptions);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart()
    {
        HackTimerApp.getBus().register(this);
        HackTimerApp.getBus().post(new HackDatabaseUpdatedEvent());
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        HackTimerApp.getBus().unregister(this);
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_hack:
                sendBroadcast(new Intent(HackReceiver.ACTION_HACK, null, getBaseContext(), HackReceiver.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

