package org.nsdev.apps.superhappyhackmap.actions;

import android.graphics.Color;
import android.support.v4.app.FragmentManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.doomonafireball.betterpickers.hmspicker.HmsPickerBuilder;
import com.doomonafireball.betterpickers.hmspicker.HmsPickerDialogFragment;
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

import java.util.HashMap;
import java.util.List;

/**
 * Created by neal 13-05-23 9:24 PM
 */
public class SelectedCircleActionMode implements ActionMode.Callback, HmsPickerDialogFragment.HmsPickerDialogHandler
{
    private List<Circle> circles;
    private List<Hack> hacks;

    private FragmentManager fragmentManager;
    private ActionMode actionMode;
    private Boolean restoreColor = true;

    public SelectedCircleActionMode(FragmentManager supportFragmentManager)
    {
        this.fragmentManager = supportFragmentManager;
    }

    public void setHacks(List<Hack> hacks)
    {
        this.hacks = hacks;
    }

    public List<Hack> getHacks()
    {
        return hacks;
    }

    private class PreviousCircleColor
    {
        public int stroke;
        public int fill;
    }

    private HashMap<Circle, PreviousCircleColor> prev = new HashMap<Circle, PreviousCircleColor>();

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu)
    {
        // Inflate a menu resource providing context menu items
        MenuInflater inflater = actionMode.getMenuInflater();
        inflater.inflate(R.menu.circle_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu)
    {
        for (Circle c : circles)
        {
            PreviousCircleColor old = new PreviousCircleColor();
            old.stroke = c.getStrokeColor();
            old.fill = c.getFillColor();

            c.setStrokeColor(Color.BLUE);
            c.setFillColor(Color.argb(64,0,0,255));

            prev.put(c, old);
        }
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem)
    {
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

                HmsPickerBuilder hpb = new HmsPickerBuilder()
                        .setFragmentManager(fragmentManager)
                        .setStyleResId(R.style.BetterPickersDialogFragment);
                hpb.addHmsPickerDialogHandler(this);
                hpb.show();

                this.actionMode = actionMode;

                return true;

            case R.id.menu_move:

                if (circles.size() == 1) {
                    Toast.makeText(HackTimerApp.getInstance(), R.string.move_instructions, Toast.LENGTH_LONG).show();
                    HackTimerApp.getBus().post(new MoveCircleEvent(circles.get(0)));

                    this.actionMode = actionMode;
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
    public void onDestroyActionMode(ActionMode actionMode)
    {
        if (!restoreColor) return;

        for (Circle c : circles)
        {
            PreviousCircleColor old = prev.get(c);
            c.setStrokeColor(old.stroke);
            c.setFillColor(old.fill);

            prev.put(c, old);
        }
    }

    public List<Circle> getCircles()
    {
        return circles;
    }

    public void setCircles(List<Circle> circles)
    {
        this.circles = circles;
    }

    @Override
    public void onDialogHmsSet(int reference, int hours, int minutes, int seconds)
    {
        int totalSeconds = seconds + 60 * minutes + 60 * 60 * hours;

        for (Hack h: hacks)
        {
            h.setCoolDownSeconds(totalSeconds);
            DatabaseManager.getInstance().save(h);
        }
        HackTimerApp.getBus().post(new CirclesCoolDownTimeChangedEvent(circles, hacks, totalSeconds));
        actionMode.finish();
    }


}
