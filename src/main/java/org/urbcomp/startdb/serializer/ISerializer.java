package org.urbcomp.startdb.serializer;

import org.urbcomp.startdb.gpsPoint;

import java.util.List;

public interface ISerializer {
    byte[] serialize(List<gpsPoint> obj);
}
