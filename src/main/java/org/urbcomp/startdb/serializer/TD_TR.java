package org.urbcomp.startdb.serializer;

import org.urbcomp.startdb.serializer.utils.sedUtils;
import org.urbcomp.startdb.gpsPoint;

import java.util.ArrayList;
import java.util.List;

public class TD_TR implements ISerializer{
    private double epsilon = 10.0;

    @Override
    public byte[] serialize(List<gpsPoint> traj) {
        List<Integer> res = compress(0, traj.size() - 1, traj);


        return new byte[0];
    }

    private List<Integer> compress(int start, int end, List<gpsPoint> traj){
        List<Integer> res = new ArrayList<>();
        double sedMax = 0;
        int index = start;

        for (int i = start + 1; i < end; i++) {
            double d = sedUtils.calcSED(traj.get(start), traj.get(i), traj.get(end));
            if(d > sedMax){
                index = i;
                sedMax = d;
            }
        }

        if (sedMax > epsilon){
            List<Integer> subRes1 = compress(start, index, traj);
            List<Integer> subRes2 = compress(index, end, traj);
            res.addAll(subRes1);
            res.addAll(subRes2);
        } else {
            res.add(start);
            res.add(end);
        }

        return res;
    }
}
