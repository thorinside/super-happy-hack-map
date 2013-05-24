package org.nsdev.apps.superhappyhackmap;

import android.graphics.Color;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.android.gms.maps.model.Circle;

import java.util.HashMap;
import java.util.List;

/**
 * Created by neal 13-05-23 9:24 PM
 */
public class SelectedCircleActionMode implements ActionMode.Callback
{
    private List<Circle> circles;

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

                HackTimerApp.getBus().post(new CirclesBurnedEvent(circles));

                actionMode.finish();
                return true;
            case R.id.menu_hack:

                HackTimerApp.getBus().post(new CirclesHackedEvent(circles));
                actionMode.finish();
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
}
