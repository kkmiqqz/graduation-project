package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.utils.distanceUtils;
import org.urbcomp.startdb.gpsPoint;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TD_TR implements ISimplificator{
    private static double epsilon = 10.0;
//    private double sedSum = 0.0;
//    private long num = 0;

    public TD_TR(double epsi){
        epsilon = epsi;
    }

    @Override
    public List<gpsPoint> simplify(List<gpsPoint> traj) {
        List<gpsPoint> res = new ArrayList<>();
        //空数据
        if(traj.isEmpty()){
            return res;
        }
        //简化
        Set<Integer> resIndex = compress(0, traj.size() - 1, traj);
        //index转换为List
        for(int index : resIndex){
            res.add(traj.get(index));
        }

        return res;
    }

    private Set<Integer> compress(int start, int end, List<gpsPoint> traj) {
        Set<Integer> res = new LinkedHashSet<>();
        double sedMax = 0;
        int index = start;

        for (int i = start + 1; i < end; i++) {
            double d = distanceUtils.calcSED(traj.get(start), traj.get(i), traj.get(end));
            if (d > sedMax) {
                index = i;
                sedMax = d;
            }
        }

        if (sedMax > epsilon) {
            Set<Integer> subRes1 = compress(start, index, traj);
            Set<Integer> subRes2 = compress(index, end, traj);
            res.addAll(subRes1);
            res.addAll(subRes2);
        } else {
            res.add(start);
            res.add(end);
        }

        return res;
    }

    private Set<Integer> compressByAveSed(int start, int end, List<gpsPoint> traj) {
        Set<Integer> res = new LinkedHashSet<>();
        double sedMax = 0;
        int index = start;

        for (int i = start + 1; i < end; i++) {
            double d = distanceUtils.calcSED(traj.get(start), traj.get(i), traj.get(end));
            if (d > sedMax) {
                index = i;
                sedMax = d;
            }
        }

        if (sedMax > epsilon) {
            Set<Integer> subRes1 = compressByAveSed(start, index, traj);
            Set<Integer> subRes2 = compressByAveSed(index, end, traj);
            res.addAll(subRes1);
            res.addAll(subRes2);
        } else {
            res.add(start);
            res.add(end);
        }

        return res;
    }

    public static void main(String[] args) {

    }
}
