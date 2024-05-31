package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.ISerializer;
import org.urbcomp.startdb.simplificator.VOLTComCommon.vector;

import java.util.ArrayList;
import java.util.List;

public class VOLTCom implements ISerializer {

    //向量

    //向量函数
    private class function{

    }

    @Override
    public byte[] serialize(List<gpsPoint> obj) {
        return new byte[0];
    }

    private List<vector> extractVector(List<gpsPoint> trajectory, double threshold){
        List<vector> compressedTraj = new ArrayList<>();

        //

        return compressedTraj;
    }



}
