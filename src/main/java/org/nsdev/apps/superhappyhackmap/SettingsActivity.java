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