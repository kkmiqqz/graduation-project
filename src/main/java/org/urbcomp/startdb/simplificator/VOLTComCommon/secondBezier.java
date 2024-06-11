package org.urbcomp.startdb.simplificator.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public class secondBezier extends AbstractFunction{
    private gpsPoint Pmid;
    public secondBezier(gpsPoint pstart, gpsPoint pmid, gpsPoint pend){
        Pstart = pstart;
        Pmid = pmid;
        Pend = pend;
    }

    @Override
    public gpsPoint estimate(long time) {
        double t = (double) (time - Pstart.getTimestamp()) / (Pend.getTimestamp() - Pstart.getTimestamp());

        gpsPoint estiPoint = new gpsPoint();
        //
        double estiLat = Math.pow(1 - t, 2) * Pstart.getLatitude() + 2 * t * (1 - t) * Pmid.getLatitude() + Math.pow(t, 2) * Pend.getLatitude();
        double estiLon = Math.pow(1 - t, 2) * Pstart.getLongitude() + 2 * t * (1 - t) * Pmid.getLongitude() + Math.pow(t, 2) * Pend.getLongitude();
        estiPoint.setLatitude(estiLat);
        estiPoint.setLongitude(estiLon);

        return estiPoint;
    }

}
