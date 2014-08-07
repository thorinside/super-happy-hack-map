package org.nsdev.apps.superhappyhackmap.model;

import android.content.Context;
import android.text.format.DateFormat;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by neal 13-03-14 4:20 PM
 */
@DatabaseTable(tableName = "Hack")
public class Hack {

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a");
    public static final int FOUR_HOURS_MS = 4 * 60 * 60 * 1000;
    public static final int FIVE_MINUTES_S = 5 * 60;

    @DatabaseField(generatedId = true)
    private int id;

    public static final String LATITUDE_FIELD_NAME = "latitude";
    @DatabaseField(columnName = LATITUDE_FIELD_NAME)
    private double latitude;

    public static final String LONGITUDE_FIELD_NAME = "longitude";
    @DatabaseField(columnName = LONGITUDE_FIELD_NAME)
    private double longitude;

    public static final String FIRST_HACKED_FIELD_NAME = "firstHacked";
    @DatabaseField(columnName = FIRST_HACKED_FIELD_NAME, dataType = DataType.DATE_LONG)
    private Date firstHacked;

    public static final String COOL_DOWN_SECONDS_FIELD_NAME = "coolDownSeconds";

    @DatabaseField(columnName = COOL_DOWN_SECONDS_FIELD_NAME)
    private int coolDownSeconds;

    public static final String LAST_HACKED_FIELD_NAME = "lastHacked";
    @DatabaseField(columnName = LAST_HACKED_FIELD_NAME, dataType = DataType.DATE_LONG)
    private Date lastHacked;

    public static final String HACK_COUNT_FIELD_NAME = "hackCount";
    @DatabaseField(columnName = HACK_COUNT_FIELD_NAME)
    private int hackCount;

    public static final String BURNED_OUT_FIELD_NAME = "burnedOut";
    @DatabaseField(columnName = BURNED_OUT_FIELD_NAME)
    private boolean burnedOut;

    public Hack() {
        // Required
    }

    public Hack(double latitude, double longitude, Date firstHacked, Date lastHacked) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstHacked = firstHacked;
        this.lastHacked = lastHacked;
        this.setCoolDownSeconds(300);
    }

    public static String formatTimeString(long ms) {
        long s = ms / 1000;
        if (s > 60) {
            long m = s / 60;
            s = s - 60 * m;
            return String.format("%d:%s", m, s < 10 ? "0" + s : "" + s);
        } else {
            return String.format("0:%s", s < 10 ? "0" + s : "" + s);
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Date getFirstHacked() {
        return firstHacked;
    }

    public void setFirstHacked(Date date) {
        firstHacked = date;
    }

    public Date getLastHacked() {
        return lastHacked;
    }

    public void setLastHacked(Date date) {
        lastHacked = date;
    }

    public int getId() {
        return id;
    }

    public int getHackCount() {
        return hackCount;
    }

    public void setHackCount(int hackCount) {
        this.hackCount = hackCount;
    }

    public int getCoolDownSeconds() {
        // Default to five minutes
        if (coolDownSeconds == 0)
            return FIVE_MINUTES_S;
        return coolDownSeconds;
    }

    public void setCoolDownSeconds(int coolDownSeconds) {
        this.coolDownSeconds = coolDownSeconds;
    }

    public void setBurnedOut(boolean burnedOut) {
        this.burnedOut = burnedOut;
    }

    public boolean isBurnedOut() {
        Date now = new Date();
        long burnoutLength = FOUR_HOURS_MS; // 4 hours in milliseconds
        long sinceFirstHack = now.getTime() - getFirstHacked().getTime();
        boolean shouldBeResetByNow = (burnoutLength - sinceFirstHack) <= 0;
        return burnedOut && !shouldBeResetByNow;
    }

    public void incrementHackCount() {
        // If the last hack was more than four hours ago, reset the hack count and reset the
        // first hacked timer.
        Date now = new Date();
        long sinceLastHack = now.getTime() - getLastHacked().getTime();
        if (sinceLastHack > FOUR_HOURS_MS) {
            setFirstHacked(now);
            setBurnedOut(false);
            setHackCount(0);
        }

        setHackCount(getHackCount() + 1);
    }

    /**
     * @return the time, in ms, until this area is hackable.
     */
    public long timeUntilHackable() {
        Date now = new Date();
        if (isBurnedOut()) {
            long burnoutLength = FOUR_HOURS_MS; // 4 hours in milliseconds
            long sinceFirstHack = now.getTime() - getFirstHacked().getTime();
            return burnoutLength - sinceFirstHack;
        } else {
            long hackAge = now.getTime() - getLastHacked().getTime();
            return (getCoolDownSeconds() * 1000 - hackAge);
        }
    }

    /**
     * Return the Maximum wait time for this portal in MS
     */
    public int getMaxWait() {
        if (isBurnedOut()) {
            return FOUR_HOURS_MS;
        } else {
            return getCoolDownSeconds() * 1000;
        }
    }

    public int getWait() {
        return (int) timeUntilHackable();
    }

    public boolean isHackable() {
        return !isBurnedOut() && timeUntilHackable() <= 0;
    }

    public String getNextHackableTimeString(Context context) {
        Calendar c = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();
        String time;
        if (isBurnedOut()) {
            c.setTime(getFirstHacked());
            c.add(Calendar.MILLISECOND, FOUR_HOURS_MS);
            time = DateFormat.getTimeFormat(context).format(c.getTime());
        } else {
            c.setTime(getLastHacked());
            c.add(Calendar.SECOND, getCoolDownSeconds());

            c2.setTime(getFirstHacked());
            c2.add(Calendar.MILLISECOND, FOUR_HOURS_MS);

            time = String.format("%s\nReset: %s",
                    DateFormat.getTimeFormat(context).format(c.getTime()),
                    DateFormat.getTimeFormat(context).format(c2.getTime()));
        }

        return time;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
