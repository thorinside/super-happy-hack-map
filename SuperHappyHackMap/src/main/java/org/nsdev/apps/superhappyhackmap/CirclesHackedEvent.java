package org.nsdev.apps.superhappyhackmap;

import com.google.android.gms.maps.model.Circle;

import java.util.List;

/**
 * Created by neal 13-05-23 9:53 PM
 */
public class CirclesHackedEvent
{
    private final List<Circle> circles;

    public CirclesHackedEvent(List<Circle> circles)
    {
        this.circles = circles;
    }

    public List<Circle> getCircles()
    {
        return circles;
    }
}
