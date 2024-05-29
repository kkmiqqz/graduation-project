package org.urbcomp.startdb.serializer.VOLTComCommon;

import org.urbcomp.startdb.gpsPoint;

public abstract class AbstractFunction {
    public abstract gpsPoint estimate(gpsPoint... variables);
}
