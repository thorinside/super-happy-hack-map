package org.nsdev.apps.superhappyhackmap;

import android.content.Context;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.List;

public class DatabaseManager
{

    static private DatabaseManager instance;

    static public void init(Context ctx)
    {
        if (null == instance)
        {
            instance = new DatabaseManager(ctx);
        }
    }

    static public DatabaseManager getInstance()
    {
        return instance;
    }

    private DatabaseHelper helper;

    private DatabaseManager(Context ctx)
    {
        helper = new DatabaseHelper(ctx);
    }

    private DatabaseHelper getHelper()
    {
        return helper;
    }

    public List<Hack> getAllHacks()
    {
        List<Hack> hacks = null;
        try
        {
            hacks = getHelper().getHackDao().queryForAll();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return hacks;
    }

    public void save(Hack hack)
    {
        try
        {
            getHelper().getHackDao().createOrUpdate(hack);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void deleteHack(Hack hack)
    {
        try
        {
            getHelper().getHackDao().delete(hack);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public Hack findHackAt(LatLng center)
    {
        List<Hack> hacks = null;
        try
        {
            QueryBuilder<Hack, Integer> queryBuilder = getHelper().getHackDao().queryBuilder();

            PreparedQuery<Hack> preparedQuery = queryBuilder.where()
                                                            .between(Hack.LATITUDE_FIELD_NAME, center.latitude - 0.000001, center.latitude + 0.000001)
                                                            .and()
                                                            .between(Hack.LONGITUDE_FIELD_NAME, center.longitude - 0.000001, center.longitude + 0.000001)
                                                            .prepare();

            Hack hack = getHelper().getHackDao().queryForFirst(preparedQuery);

            return hack;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public Hack findHackById(int hackId)
    {
        try
        {
            return getHelper().getHackDao().queryForId(hackId);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}