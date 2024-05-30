package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.gpsPoint;

import java.util.List;

public interface ISimplificator {
     List<gpsPoint> simplify(List<gpsPoint> traj);
}
