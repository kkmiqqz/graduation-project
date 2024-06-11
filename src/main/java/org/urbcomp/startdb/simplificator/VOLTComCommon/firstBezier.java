package org.urbcomp.startdb.simplificator.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public class firstBezier extends AbstractFunction {
    public firstBezier(gpsPoint pstart, gpsPoint pend){
        Pstart = pstart;
        Pend = pend;
    }

    @Override
    public gpsPoint estimate(long time) {
        double t = (double) (time - Pstart.getTimestamp()) / (Pend.getTimestamp() - Pstart.getTimestamp());

        gpsPoint estPoint = new gpsPoint();
        //
        double estLat = (1 - t) * Pstart.getLatitude() + t * Pend.getLatitude();
        double estLon = (1 - t) * Pstart.getLongitude() + t * Pend.getLongitude();
        estPoint.setLatitude(estLat);
        estPoint.setLongitude(estLon);

        return estPoint;
    }
}
