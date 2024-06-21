package org.urbcomp.startdb.simplificator.DPhullCommon;

import org.urbcomp.startdb.gpsPoint;

public class ExtremeResult {
    double dist;
    gpsPoint extremePoint;

    public ExtremeResult(double dist, gpsPoint extremePoint) {
        this.dist = dist;
        this.extremePoint = extremePoint;
    }

    public double getDist() {
        return dist;
    }

    public gpsPoint getExtremePoint() {
        return extremePoint;
    }
}
