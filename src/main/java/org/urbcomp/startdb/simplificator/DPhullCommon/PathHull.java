package org.urbcomp.startdb.simplificator.DPhullCommon;

import org.urbcomp.startdb.gpsPoint;

public  class PathHull {
    public static final int HULL_MAX = 10002;
    public static final int TWICE_HULL_MAX = 20002;
    public static final int THRICE_HULL_MAX = 30002;
    int top, bot, hp; //top and bot are the two ends,hp is the stack pointer
    int[] op; //the history stack of operation
    gpsPoint[] elt, helt; //elt is a double ended queue storing a convex hull, helt is the history stack of points

    public PathHull() {
        top = bot = hp = 0;
        op = new int[THRICE_HULL_MAX];
        elt = new gpsPoint[TWICE_HULL_MAX];
        helt = new gpsPoint[THRICE_HULL_MAX];
    }
}
