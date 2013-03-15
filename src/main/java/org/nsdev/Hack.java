package org.nsdev;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * Created by neal 13-03-14 4:20 PM
 */
@DatabaseTable
public class Hack
{
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private double latitude;

    @DatabaseField
    private double longitude;

    @DatabaseField
    private Date timestamp;

    public Hack()
    {

    }

    public Hack(double latitude, double longitude, Date timestamp)
    {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public double getLatitude()
    {
        return latitude;
    }

    public double getLongitude()
    {
        return longitude;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public int getId()
    {
        return id;
    }
}
