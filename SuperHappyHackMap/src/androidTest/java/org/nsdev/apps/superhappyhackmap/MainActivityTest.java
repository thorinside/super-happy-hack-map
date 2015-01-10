package org.nsdev.apps.superhappyhackmap;

import org.junit.Before;
import org.junit.Test;
import org.nsdev.apps.superhappyhackmap.activities.MainActivity;
import org.nsdev.apps.superhappyhackmap.test.RobojavaTestBase;
import org.robolectric.Robolectric;

import static org.junit.Assert.assertTrue;

/**
 * Tests for the MainActivity.
 *
 * Created by neal on 15-01-08.
 */
public class MainActivityTest extends RobojavaTestBase {

    private MainActivity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(MainActivity.class).create().get();
    }

    @Test
    public void testSomething() throws Exception {
        assertTrue(mActivity != null);
    }
}
