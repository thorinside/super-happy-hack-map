package org.nsdev.apps.superhappyhackmap.services;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;

import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.receivers.HackReceiver;

import wei.mark.standout.StandOutWindow;
import wei.mark.standout.constants.StandOutFlags;
import wei.mark.standout.ui.Window;

/**
 * Definition of a small hack window for showing a persistent on-screen widget that
 * users can interact with.
 * <p/>
 * Created by neal on 2014-05-03.
 */
public class HackWindow extends StandOutWindow {

    public static final String HACK_WINDOW_PREFERENCES = "hack_window";

    @Override
    public String getAppName() {
        return getString(R.string.hack_window_app_name);
    }

    @Override
    public int getAppIcon() {
        return android.R.drawable.ic_menu_close_clear_cancel;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        // create a new layout from body.xml
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.window_hack, frame, true);
        Button button = (Button) v.findViewById(R.id.button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendBroadcast(new Intent(HackReceiver.ACTION_HACK, null, getBaseContext(), HackReceiver.class));
                }
            });
        }
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {

        final SharedPreferences preferences = getSharedPreferences(HACK_WINDOW_PREFERENCES, MODE_PRIVATE);

        return new StandOutLayoutParams(id,
                getResources().getDimensionPixelOffset(R.dimen.hack_window_width),
                getResources().getDimensionPixelOffset(R.dimen.hack_window_height),
                preferences.getInt("x", StandOutLayoutParams.CENTER),
                preferences.getInt("y", StandOutLayoutParams.CENTER));
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_EDGE_LIMITS_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
    }

    @Override
    public String getPersistentNotificationMessage(int id) {
        return getString(R.string.hack_window_notification_text);
    }

    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return StandOutWindow.getCloseIntent(this, HackWindow.class, id);
    }

    @Override
    public Animation getShowAnimation(int id) {
        return AnimationUtils.loadAnimation(this,
                android.R.anim.slide_in_left);
    }

    @Override
    public Animation getHideAnimation(int id) {
        return AnimationUtils.loadAnimation(this,
                android.R.anim.slide_out_right);
    }

    @Override
    public boolean onTouchHandleMove(int id, Window window, View view, MotionEvent event) {
        final SharedPreferences preferences = getSharedPreferences(HACK_WINDOW_PREFERENCES, MODE_PRIVATE);
        preferences.edit().putInt("x", window.getLayoutParams().x)
                .putInt("y", window.getLayoutParams().y).commit();

        return super.onTouchHandleMove(id, window, view, event);
    }
}
