package org.nsdev.apps.superhappyhackmap;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by neal 13-06-02 1:21 PM
 */
public class SettingsActivity extends Activity
{

    public static final String PREF_TRACK_DISTANCE = "pref_track_distance";
    public static final String PREF_HIGH_PRIORITY = "pref_high_priority";
    public static final String PREF_SHOW_NEXT_HACK_TIME = "pref_show_next_hack_time";
    public static final String PREF_BUZZ_IF_HACKABLE = "pref_buzz_if_hackable";
    public static final String PREF_SHOW_HACK_HELPER_WINDOW = "pref_show_hack_helper_window";

    public static class SettingsFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}