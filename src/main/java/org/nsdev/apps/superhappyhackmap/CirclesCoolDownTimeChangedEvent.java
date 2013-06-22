package org.nsdev.apps.superhappyhackmap;

import com.google.android.gms.maps.model.Circle;

import java.util.List;

/**
 * Created by neal 13-06-17 9:27 PM
 */
public class CirclesCoolDownTimeChangedEvent
{
    private final List<Hack> hacks;
    private final int coolDownSeconds;

    private final List<Circle> circles;

    public CirclesCoolDownTimeChangedEvent(List<Circle> circles, List<Hack> hacks, int coolDownSeconds)
    {
        this.circles = circles;
        this.hacks = hacks;
        this.coolDownSeconds = coolDownSeconds;
    }

    public List<Circle> getCircles()
    {
        return circles;
    }

    public int getCoolDownSeconds()
    {
        return coolDownSeconds;
    }

    public List<Hack> getHacks()
    {
        return hacks;
    }
}
