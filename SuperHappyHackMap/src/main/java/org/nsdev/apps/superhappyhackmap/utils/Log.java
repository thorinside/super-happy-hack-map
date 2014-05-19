package org.nsdev.apps.superhappyhackmap.utils;

/**
 * A utility class that helps reduce the debug logging output of applications at
 * runtime. This delegates to the Android Log class and follows a similar API.
 *
 * @author Neal
 */
public class Log
{
    public static int d(String tag, String message)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.DEBUG))
        {
            return android.util.Log.d(tag, message);
        }
        return 0;
    }

    public static int d(String tag, String message, Throwable t)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.DEBUG))
        {
            return android.util.Log.d(tag, message, t);
        }
        return 0;
    }

    public static int i(String tag, String message)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.INFO))
        {
            return android.util.Log.i(tag, message);
        }
        return 0;
    }

    public static int i(String tag, String message, Throwable t)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.INFO))
        {
            return android.util.Log.i(tag, message, t);
        }
        return 0;
    }

    public static int w(String tag, String message)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.WARN))
        {
            return android.util.Log.w(tag, message);
        }
        return 0;
    }

    public static int w(String tag, String message, Throwable t)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.WARN))
        {
            return android.util.Log.w(tag, message, t);
        }
        return 0;
    }

    public static int e(String tag, String message)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.ERROR))
        {
            return android.util.Log.e(tag, message);
        }
        return 0;
    }

    public static int e(String tag, String message, Throwable t)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.ERROR))
        {
            return android.util.Log.e(tag, message, t);
        }
        return 0;
    }

    public static int v(String tag, String message)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.VERBOSE))
        {
            return android.util.Log.v(tag, message);
        }
        return 0;
    }

    public static int v(String tag, String message, Throwable t)
    {
        if (android.util.Log.isLoggable(tag, android.util.Log.VERBOSE))
        {
            return android.util.Log.v(tag, message, t);
        }
        return 0;
    }
}
