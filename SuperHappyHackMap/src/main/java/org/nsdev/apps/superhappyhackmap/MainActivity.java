package org.nsdev.apps.superhappyhackmap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.ui.BubbleIconFactory;
import com.squareup.otto.Subscribe;

import de.psdev.licensesdialog.LicensesDialogFragment;
import com.cocosw.undobar.UndoBarController;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends FragmentActivity
{
    private static String TAG = "SHHM";
    private SupportMapFragment mapFragment;

    private GoogleMap map;
    private ArrayList<Circle> circles = new ArrayList<Circle>();
    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private boolean hasZoomed = false;
    private SharedPreferences preferences;
    private Handler handler;

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

        handler = new Handler();

        setContentView(R.layout.main);

        //int res = R.raw.asl_20_full;

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        map = mapFragment.getMap();


        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (errorCode != ConnectionResult.SUCCESS)
        {
            GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
            return;
        }

        Intent i = new Intent(getBaseContext(), LocationLockService.class);
        i.setAction(LocationLockService.ACTION_MONITOR_LOCATION);
        startService(i);

        if (map != null)
        {
            ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(this, "CAMERA_POS", MODE_PRIVATE);
            CameraPosition pos = complexPreferences.getObject("cameraPosition", CameraPosition.class);
            if (pos != null)
            {
                map.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                hasZoomed = true;
            }

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
                    if (cameraPosition.zoom < 15)
                    {
                        for (Marker m : markers)
                        {
                            if (m.isVisible())
                            {
                                m.setVisible(false);
                            }
                        }
                    }
                    else
                    {
                        for (Marker m : markers)
                        {
                            if (!m.isVisible())
                            {
                                m.setVisible(true);
                            }
                        }
                    }
                }
            });

            map.setOnMapClickListener(new GoogleMap.OnMapClickListener()
            {
                @Override
                public void onMapClick(LatLng latLng)
                {
                    if (selectedCircleActionMode != null)
                    {
                        return;
                    }

                    // Find the hack that was tapped
                    List<Circle> tappedCircles = findTappedCircles(latLng.latitude, latLng.longitude);

                    if (tappedCircles.size() > 0)
                    {
                        selectedCircleActionMode = new SelectedCircleActionMode(getSupportFragmentManager());
                        selectedCircleActionMode.setCircles(tappedCircles);

                        List<Hack> tappedHacks = new ArrayList<Hack>();
                        for (Circle c : tappedCircles)
                        {
                            LatLng center = c.getCenter();
                            Hack h = DatabaseManager.getInstance().findHackAt(center);
                            tappedHacks.add(h);
                        }

                        selectedCircleActionMode.setHacks(tappedHacks);

                        startActionMode(selectedCircleActionMode);
                    }
                }
            });

            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
            {
                public boolean onMarkerClick(Marker marker)
                {
                    return true;
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
    }

    private List<Circle> findTappedCircles(double latitude, double longitude)
    {

        ArrayList<Circle> circleList = new ArrayList<Circle>();

        for (Circle c : circles)
        {
            float[] results = new float[3];
            Location.distanceBetween(latitude, longitude, c.getCenter().latitude, c.getCenter().longitude, results);
            if (results[0] <= c.getRadius())
            {
                circleList.add(c);
            }
        }

        return circleList;
    }

    @Subscribe
    public void onCirclesCoolDownTimeChanged(CirclesCoolDownTimeChangedEvent evt)
    {
        for (Hack h : evt.getHacks())
        {
            updateCooldownForHack(h);
        }
    }

    private void updateCooldownForHack(Hack h) {
        Intent intent = new Intent(HackReceiver.ACTION_SET_COOLDOWN, null, getBaseContext(), HackReceiver.class);
        intent.putExtra("hack_id", h.getId());

        sendBroadcast(intent);
    }

    @Subscribe
    public void onCirclesDeleted(CirclesDeletedEvent evt)
    {
        for (Circle c : evt.getCircles())
        {
            LatLng center = c.getCenter();
            Hack h = DatabaseManager.getInstance().findHackAt(center);

            if (h == null)
            {
                Log.d(TAG, "Deleted hack not found.");
                return;
            }

            deleteHack(h);
        }
    }

    private void deleteHack(Hack h) {
        Intent intent = new Intent(HackReceiver.ACTION_ALARM, null, getBaseContext(), HackReceiver.class);
        intent.putExtra("hack_id", h.getId());
        intent.putExtra("sound", false);
        intent.putExtra("userDeleted", true);

        // Delete the geofence as well now
        Intent deleteFenceIntent = new Intent(LocationLockService.ACTION_HACK, null, getBaseContext(), LocationLockService.class);
        deleteFenceIntent.putExtra("hack_id", h.getId());
        deleteFenceIntent.putExtra("delete", true);

        DatabaseManager.getInstance().deleteHack(h);
        sendBroadcast(intent);

        startService(deleteFenceIntent);
    }

    @Subscribe
    public void onCirclesHacked(CirclesHackedEvent evt)
    {
        for (Circle c : evt.getCircles())
        {
            LatLng center = c.getCenter();
            Hack h = DatabaseManager.getInstance().findHackAt(center);
            h.setBurnedOut(false);

            Intent intent = new Intent(HackReceiver.ACTION_ALARM, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("sound", false);
            intent.putExtra("userDeleted", true);

            sendBroadcast(intent);

            intent = new Intent(HackReceiver.ACTION_HACK, null, getBaseContext(), HackReceiver.class);
            intent.putExtra("hack_id", h.getId());
            intent.putExtra("force", true);

            DatabaseManager.getInstance().save(h);

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
            h.setBurnedOut(true);

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
        for (final Circle c : circles)
        {
            c.remove();
        }

        for (final Marker m : markers)
        {
            m.remove();
        }

        circles.clear();
        markers.clear();

        List<Hack> hacks = DatabaseManager.getInstance().getAllHacks();
        for (Hack h : hacks)
        {
            final CircleOptions opts = new CircleOptions();
            final LatLng center = new LatLng(h.getLatitude(), h.getLongitude());
            opts.center(center);
            opts.radius(40);
            opts.fillColor(Color.argb(64, 255, 0, 0));
            opts.strokeColor(Color.RED);
            opts.strokeWidth(2);
            opts.zIndex(100);

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

            // Display the next available hack time on the circles
            if (h.isBurnedOut() || h.timeUntilHackable() > 0 && preferences
                    .getBoolean(SettingsActivity.PREF_SHOW_NEXT_HACK_TIME, true))
            {

                BubbleIconFactory factory = new BubbleIconFactory(MainActivity.this);
                if (h.isBurnedOut())
                    factory.setStyle(BubbleIconFactory.Style.DEFAULT);
                else
                    factory.setStyle(BubbleIconFactory.Style.RED);
                factory.setContentRotation(90);
                final Bitmap bmp = factory.makeIcon(h.getNextHackableTimeString(MainActivity.this));

                markers.add(map.addMarker(new MarkerOptions()
                        .position(center)
                        .icon(BitmapDescriptorFactory.fromBitmap(bmp))
                        .anchor(0.5f, 0.75f)
                ));
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onDestroy()
    {
        CameraPosition pos = map.getCameraPosition();

        ComplexPreferences complexPreferences = ComplexPreferences.getComplexPreferences(this, "CAMERA_POS", MODE_PRIVATE);
        complexPreferences.putObject("cameraPosition", pos);
        complexPreferences.commit();

        super.onDestroy();
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
            case R.id.menu_settings:
                startActivity(new Intent(getBaseContext(), SettingsActivity.class));
                return true;
            case R.id.menu_about:
                //final LicensesDialogFragment fragment = LicensesDialogFragment.newInstace(R.raw.notices, true);
                //fragment.show(getSupportFragmentManager(), null);
                return true;
            case R.id.menu_clear_all:
                final List<Hack> allHacks = DatabaseManager.getInstance().getAllHacks();
                for (Hack h : allHacks) {
                    deleteHack(h);
                }

                UndoBarController.show(this, "Undo delete?", new UndoBarController.UndoListener() {
                    @Override
                    public void onUndo(Parcelable parcelable) {
                        for (Hack h : allHacks) {
                            DatabaseManager.getInstance().save(h);
                            updateCooldownForHack(h);
                        }
                        onHackDatabaseUpdated(new HackDatabaseUpdatedEvent());
                    }
                });
                /*
                */
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

