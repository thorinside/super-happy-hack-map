package org.nsdev.apps.superhappyhackmap;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by neal 13-03-14 4:20 PM
 */
@DatabaseTable
public class Hack
{
    public static final int FOUR_HOURS_MS = 4 * 60 * 60 * 1000;
    public static final int FIVE_MINUTES_MS = 5 * 60 * 1000;

    @DatabaseField(generatedId = true)
    private int id;

    public static final String LATITUDE_FIELD_NAME = "latitude";
    @DatabaseField(columnName = LATITUDE_FIELD_NAME)
    private double latitude;

    public static final String LONGITUDE_FIELD_NAME = "longitude";
    @DatabaseField(columnName = LONGITUDE_FIELD_NAME)
    private double longitude;

    @DatabaseField
    private Date firstHacked;

    @DatabaseField
    private Date lastHacked;

    @DatabaseField
    private int hackCount;

    public Hack()
    {
        // Required
    }

    public Hack(double latitude, double longitude, Date firstHacked, Date lastHacked)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstHacked = firstHacked;
        this.lastHacked = lastHacked;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public Date getFirstHacked()
    {
        return firstHacked;
    }

    public void setFirstHacked(Date date)
    {
        firstHacked = date;
    }

    public Date getLastHacked()
    {
        return lastHacked;
    }

    public void setLastHacked(Date date)
    {
        lastHacked = date;
    }

    public int getId()
    {
        return id;
    }

    public int getHackCount()
    {
        return hackCount;
    }

    public void setHackCount(int hackCount)
    {
        this.hackCount = hackCount;
    }

    public void incrementHackCount()
    {
        setHackCount(getHackCount() + 1);
        if (hackCount > 4)
        {
            hackCount = 4;
        }
    }

    public boolean isBurnedOut()
    {
        Date now = new Date();
        long burnoutLength = FOUR_HOURS_MS; // 4 hours in milliseconds
        long sinceFirstHack = now.getTime() - getFirstHacked().getTime();
        boolean shouldBeResetByNow = (burnoutLength - sinceFirstHack) <= 0;
        return hackCount == 4 && !shouldBeResetByNow;
    }

    public long timeUntilHackable()
    {
        Date now = new Date();
        if (isBurnedOut())
        {
            long burnoutLength = FOUR_HOURS_MS; // 4 hours in milliseconds
            long sinceFirstHack = now.getTime() - getFirstHacked().getTime();
            return burnoutLength - sinceFirstHack;
        }
        else
        {
            long hackAge = now.getTime() - getLastHacked().getTime();
            return (FIVE_MINUTES_MS - hackAge);
        }
    }

    public int getMaxWait()
    {
        if (isBurnedOut())
        {
            return FOUR_HOURS_MS;
        }
        else
        {
            return FIVE_MINUTES_MS;
        }
    }

    public int getWait()
    {
        return (int)timeUntilHackable();
    }

    public boolean isHackable()
    {
        return !isBurnedOut() && timeUntilHackable() <= 0;
    }
}
