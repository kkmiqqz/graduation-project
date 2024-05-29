package org.urbcomp.startdb.serializer.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public class firstBezier extends AbstractFunction {

    @Override
    public gpsPoint estimate(gpsPoint... variables) {
        if (variables.length != 3) {
            throw new IllegalArgumentException("FirstOrderFunction requires exactly 2 variables.");
        }

        gpsPoint s = variables[0];
        gpsPoint e = variables[1];
        gpsPoint next = variables[2];
        double t = (next.getTimestamp() - s.getTimestamp()) / (e.getTimestamp() - s.getTimestamp());

        gpsPoint estPoint = new gpsPoint();
        //
        double estLat = (1 - t) * s.getLatitude() + t * e.getLatitude();
        double estLon = (1 - t) * s.getLongitude() + t * e.getLongitude();
        estPoint.setLatitude(estLat);
        estPoint.setLongitude(estLon);

        return estPoint;
    }
}
