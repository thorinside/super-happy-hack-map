package org.nsdev.apps.superhappyhackmap.events;

import com.google.android.gms.maps.model.Circle;

/**
 * Created by neal on 2014-05-19.
 */
public class MoveCircleEvent {
    private final Circle mCircle;

    public MoveCircleEvent(Circle circle) {
        mCircle = circle;
    }

    public Circle getCircle() {
        return mCircle;
    }
}
