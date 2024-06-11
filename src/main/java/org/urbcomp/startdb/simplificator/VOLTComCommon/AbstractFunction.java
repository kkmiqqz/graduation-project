package org.urbcomp.startdb.simplificator.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public abstract class AbstractFunction {
    protected gpsPoint Pstart;
    protected gpsPoint Pend;

    public abstract gpsPoint estimate(long time);

    public gpsPoint getPstart() {
        return Pstart;
    }
}
