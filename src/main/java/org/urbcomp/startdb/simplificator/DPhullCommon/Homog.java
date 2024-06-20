package org.urbcomp.startdb.simplificator.DPhullCommon;

import org.urbcomp.startdb.gpsPoint;

public class Homog {
    // ww represents the cross product of the points
    private double ww;
    // xx represents the negative difference of the y coordinates
    private double xx;
    // yy represents the difference of the x coordinates
    private double yy;

    public Homog() {
    }

    public Homog(double ww, double xx, double yy) {
        this.ww = ww;
        this.xx = xx;
        this.yy = yy;
    }

    public double getWw() {
        return ww;
    }

    public double getXx() {
        return xx;
    }

    public double getYy() {
        return yy;
    }

    public void crossProd2cch(gpsPoint p, gpsPoint q) {
        ww = p.getLongitude() * q.getLatitude() - p.getLatitude() * q.getLongitude();
        xx = p.getLatitude() - q.getLatitude();
        yy = - p.getLongitude() + q.getLongitude();
    }
}
