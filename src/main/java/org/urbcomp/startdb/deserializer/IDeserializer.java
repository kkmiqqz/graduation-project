package org.urbcomp.startdb.deserializer;

import org.urbcomp.startdb.gpsPoint;

import java.util.List;

public interface IDeserializer {
    List<gpsPoint> deserialize(byte[] obj);
}
