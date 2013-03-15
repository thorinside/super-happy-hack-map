package org.nsdev;

import android.content.Context;

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
            getHelper().getHackDao().create(hack);
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
}