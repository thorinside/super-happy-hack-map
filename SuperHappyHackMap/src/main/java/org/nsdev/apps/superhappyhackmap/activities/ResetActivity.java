package org.nsdev.apps.superhappyhackmap.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.receivers.HackReceiver;

public class ResetActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset);

        if (HackReceiver.ACTION_RESET_LAST_HACK.equals(getIntent().getAction())) {
            sendBroadcast(new Intent(HackReceiver.ACTION_RESET_LAST_HACK, null, getBaseContext(), HackReceiver.class));
        }
        finish();
    }

}
