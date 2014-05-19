package org.nsdev.apps.superhappyhackmap.events;

import com.google.android.gms.maps.model.Circle;

import java.util.List;

/**
 * Created by neal 13-05-23 9:54 PM
 */
public class CirclesDeletedEvent {
    private final List<Circle> circles;

    public CirclesDeletedEvent(List<Circle> circles) {
        this.circles = circles;
    }

    public List<Circle> getCircles() {
        return circles;
    }
}
