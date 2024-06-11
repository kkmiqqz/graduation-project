package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.simplificator.VOLTComCommon.firstBezier;
import org.urbcomp.startdb.simplificator.VOLTComCommon.secondBezier;
import org.urbcomp.startdb.simplificator.VOLTComCommon.vector;
import org.urbcomp.startdb.utils.distanceUtils;

import java.util.ArrayList;
import java.util.List;

public class VOLTCom implements ISimplificator {
    private static double epsilon = 10.0;

    public VOLTCom(double epsi){
        epsilon = epsi;
    }
    @Override
    public List<gpsPoint> simplify(List<gpsPoint> traj) {
        if(traj.size() <= 2) {
            return traj;
        } else {

        }
        return null;
    }

    public List<vector> extractVectorOrigin(List<gpsPoint> traj){
        List<vector> comTraj = new ArrayList<>();
        Boolean init = true;
        gpsPoint first;
        gpsPoint second;
        gpsPoint curPoint;
        vector curVector;

        if(traj.size() == 0) {
            return comTraj;
        } else if (traj.size() == 1) { //暂定
            curPoint = traj.get(0);
            vector tmpVector = new vector(new firstBezier(curPoint, curPoint), curPoint.getTimestamp(), curPoint.getTimestamp());
            comTraj.add(tmpVector);
            return comTraj;
        }

        for(int i = 0; i < traj.size(); i++) {
            if(init) {
                first = traj.get(0);
                second =  traj.get(1);
                vector tmpVector = new vector(new firstBezier(first, second), first.getTimestamp(), second.getTimestamp());
                comTraj.add(tmpVector);
                init = false;
            }
            else {
                curPoint = traj.get(i);
                curVector = comTraj.get(comTraj.size() - 1);
                gpsPoint estimatePoint = curVector.getEstimate(curPoint.getTimestamp());

                //拟合满足阈值
                if(distanceUtils.haversine(curPoint.getLatitude(), curPoint.getLongitude(),
                        estimatePoint.getLatitude(), estimatePoint.getLongitude()) < epsilon) {
                    curVector.setETime(curPoint.getTimestamp());
                } else { //不满足，持续启用二阶
                    secondBezier tmpFunction = new secondBezier(curVector.getPstart(), curVector.getPend(), curPoint);
                    vector tmpVector = new vector(tmpFunction, curVector.getETime(), curPoint.getTimestamp());
                    comTraj.add(tmpVector);
                }
            }
        }

        return comTraj;
    }

    public List<vector> extractVectorOnlyOne(List<gpsPoint> traj){
        List<vector> comTraj = new ArrayList<>();
        Boolean init = true;
        gpsPoint first;
        gpsPoint second;
        gpsPoint curPoint;
        vector curVector;

        if(traj.isEmpty()) {
            return comTraj;
        } else if (traj.size() == 1) { //暂定
            curPoint = traj.get(0);
            vector tmpVector = new vector(new firstBezier(curPoint, curPoint), curPoint.getTimestamp(), curPoint.getTimestamp());
            comTraj.add(tmpVector);
            return comTraj;
        }

        for(int i = 0; i < traj.size(); i++) {
            if(init) {
                first = traj.get(0);
                second =  traj.get(1);
                vector tmpVector = new vector(new firstBezier(first, second), first.getTimestamp(), second.getTimestamp());
                comTraj.add(tmpVector);
                init = false;
            }
            else {
                curPoint = traj.get(i);
                curVector = comTraj.get(comTraj.size() - 1);
                gpsPoint estimatePoint = curVector.getEstimate(curPoint.getTimestamp());

                //拟合满足阈值
                if(distanceUtils.haversine(curPoint.getLatitude(), curPoint.getLongitude(),
                        estimatePoint.getLatitude(), estimatePoint.getLongitude()) < epsilon) {
                    curVector.setETime(curPoint.getTimestamp());
                } else { //不满足
                    if(i < traj.size() - 1) {
                        first = traj.get(i);
                        second =  traj.get(i + 1); //ToDO：跳过 i + 1 的判断
                        vector tmpVector = new vector(new firstBezier(first, second), first.getTimestamp(), second.getTimestamp());
                        comTraj.add(tmpVector);
                    } else { //最后一个点，暂定
                        vector tmpVector = new vector(new firstBezier(curPoint, curPoint), curPoint.getTimestamp(), curPoint.getTimestamp());
                        comTraj.add(tmpVector);
                    }
                }
            }
        }

        return comTraj;
    }



}
