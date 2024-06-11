package org.urbcomp.startdb.simplificator.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public class vector {
    private AbstractFunction function;
    private long sTime;
    private long eTime;

    public vector() {}
    public vector(AbstractFunction func, long stime, long etime){
        function = func;
        sTime = stime;
        eTime = etime;
    }

    public void setETime(long tEnd){
        eTime = tEnd;
    }

    public long getETime() {
        return eTime;
    }

    public gpsPoint getEstimate(long time) {
        return function.estimate(time);
    }

    public gpsPoint getPstart() {
        return getEstimate(sTime);
    }

    public gpsPoint getPend() {
        return getEstimate(eTime);
    }
}
