package org.nsdev;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.os.Vibrator;
import android.widget.Toast;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by neal 13-03-11 10:16 PM
 */
public class HackTimerApp extends Application {

    private static Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialized the database
        DatabaseManager.init(this);

        bus = new Bus();
        bus.register(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        bus.unregister(this);
    }

    @Subscribe
    public void onLocation(Location location) {
        /*
        Toast.makeText(this, String.format("%f %f %f", location.getLatitude(), location.getLongitude(), location.getAccuracy()), Toast.LENGTH_LONG).show();

        ArrayList<LocationLockService.Hack> expired = new ArrayList<LocationLockService.Hack>();

        for (LocationLockService.Hack hack: LocationLockService.getHacks()) {

            Calendar hackDate = Calendar.getInstance();
            hackDate.setTime(hack.getDate());
            hackDate.add(Calendar.MINUTE, 5);

            Date now = new Date();
            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.setTime(now);

            if (hackDate.before(nowCalendar)) {
                expired.add(hack);

                Toast.makeText(getApplicationContext(), "Hack timer expired.", Toast.LENGTH_LONG).show();

                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(5000L);
            }

        }

        for (LocationLockService.Hack hack: expired) {
            LocationLockService.getHacks().remove(hack);
        }
        */
    }

    public static Bus getBus() {
        return bus;
    }
}
