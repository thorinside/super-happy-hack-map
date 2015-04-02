package org.nsdev.apps.superhappyhackmap;

import android.app.Activity;
import android.view.Menu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nsdev.apps.superhappyhackmap.activities.MainActivity;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by neal on 15-04-01.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(emulateSdk = 21, constants = BuildConfig.class)

public class MainActivityTest {

    @Test
    public void onCreate_shouldInflateTheMenu() throws Exception {
        Activity activity = Robolectric.setupActivity(MainActivity.class);

        final Menu menu = shadowOf(activity).getOptionsMenu();
        assertThat(menu.findItem(R.id.menu_about).getTitle()).isEqualTo("About");
        assertThat(menu.findItem(R.id.menu_settings).getTitle()).isEqualTo("Settings");
    }
}
