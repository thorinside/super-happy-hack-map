package org.nsdev.apps.superhappyhackmap.actions;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.maps.model.Circle;

import org.nsdev.apps.superhappyhackmap.HackTimerApp;
import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.events.CirclesBurnedEvent;
import org.nsdev.apps.superhappyhackmap.events.CirclesCoolDownTimeChangedEvent;
import org.nsdev.apps.superhappyhackmap.events.CirclesDeletedEvent;
import org.nsdev.apps.superhappyhackmap.events.CirclesHackedEvent;
import org.nsdev.apps.superhappyhackmap.events.MoveCircleEvent;
import org.nsdev.apps.superhappyhackmap.model.DatabaseManager;
import org.nsdev.apps.superhappyhackmap.model.Hack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import hugo.weaving.DebugLog;

/**
 * Created by neal 13-05-23 9:24 PM
 */
public class SelectedCircleActionMode implements ActionMode.Callback {
    private List<Circle> circles;
    private List<Hack> hacks;

    private final Context mContext;
    private Boolean restoreColor = true;
    private ActionMode mActionMode;
    private double mSeconds = 5.0 * 60;

    public SelectedCircleActionMode(Context context) {
        mContext = context;
    }

    public void setHacks(List<Hack> hacks) {
        this.hacks = hacks;
    }

    public List<Hack> getHacks() {
        return hacks;
    }

    private class PreviousCircleColor {
        public int stroke;
        public int fill;
    }

    private HashMap<Circle, PreviousCircleColor> prev = new HashMap<>();

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        mActionMode = actionMode;
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.circle_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        for (Circle c : circles) {
            PreviousCircleColor old = new PreviousCircleColor();
            old.stroke = c.getStrokeColor();
            old.fill = c.getFillColor();

            c.setStrokeColor(Color.BLUE);
            c.setFillColor(Color.argb(64, 0, 0, 255));

            prev.put(c, old);
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_burned:

                restoreColor = false;
                HackTimerApp.getBus().post(new CirclesBurnedEvent(circles));

                actionMode.finish();
                return true;
            case R.id.menu_hack:
                restoreColor = false;
                HackTimerApp.getBus().post(new CirclesHackedEvent(circles));
                actionMode.finish();
                return true;
            case R.id.menu_set_cooldown:

                new MaterialDialog.Builder(mContext)
                        .content("Choose the ")
                        .title("Cooldown Time Modifiers")
                        .items(new String[]{"Common HS", "Rare HS", "VR HS"})
                        .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMulti() {
                            @DebugLog
                            @Override
                            public void onSelection(MaterialDialog materialDialog, Integer[] which, CharSequence[] charSequences) {

                                ArrayList<Integer> mods = new ArrayList<>();
                                Collections.addAll(mods, which);

                                Collections.sort(mods);
                                Collections.reverse(mods);

                                mSeconds = 5 * 60.0;
                                double modifiers = 1.0;

                                boolean isFirst = true;
                                for (Integer mod : mods) {
                                    switch (mod) {
                                        case 0:
                                            double value = 0.2;
                                            if (!isFirst) {
                                                value = value / 2.0;
                                            }
                                            modifiers = modifiers * (1 - value);
                                            break;
                                        case 1:
                                            value = 0.5;
                                            if (!isFirst) {
                                                value = value / 2.0;
                                            }
                                            modifiers = modifiers * (1 - value);
                                            break;
                                        case 2:
                                            value = 0.7;
                                            if (!isFirst) {
                                                value = value / 2.0;
                                            }
                                            modifiers = modifiers * (1 - value);
                                            break;
                                    }
                                    isFirst = false;
                                }

                                double cooldownDecrease = 1.0 - modifiers;
                                mSeconds = mSeconds * (1.0 - cooldownDecrease);
                            }
                        })
                        .positiveText("Choose Modifiers")
                        .dismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {

                                Toast.makeText(mContext, String.format("Cooldown will be %d seconds.", (int) mSeconds), Toast.LENGTH_SHORT).show();

                                for (Hack h : hacks) {
                                    h.setCoolDownSeconds((int) mSeconds);
                                    DatabaseManager.getInstance().save(h);
                                }
                                HackTimerApp.getBus().post(new CirclesCoolDownTimeChangedEvent(circles, hacks, (int) mSeconds));
                                mActionMode.finish();
                            }
                        })
                        .show();

                ActionMode actionMode1 = actionMode;

                return true;

            case R.id.menu_move:

                if (circles.size() == 1) {
                    Toast.makeText(HackTimerApp.getInstance(), R.string.move_instructions, Toast.LENGTH_LONG).show();
                    HackTimerApp.getBus().post(new MoveCircleEvent(circles.get(0)));

                    actionMode1 = actionMode;
                } else {
                    Toast.makeText(HackTimerApp.getInstance(), R.string.move_too_many, Toast.LENGTH_LONG).show();
                }

                return true;

            case R.id.menu_delete:

                HackTimerApp.getBus().post(new CirclesDeletedEvent(circles));
                actionMode.finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (!restoreColor) return;

        for (Circle c : circles) {
            PreviousCircleColor old = prev.get(c);
            c.setStrokeColor(old.stroke);
            c.setFillColor(old.fill);

            prev.put(c, old);
        }
    }

    public List<Circle> getCircles() {
        return circles;
    }

    public void setCircles(List<Circle> circles) {
        this.circles = circles;
    }

}
