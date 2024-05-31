package utils;

import org.urbcomp.startdb.gpsPoint;

import java.util.ArrayList;
import java.util.List;

public class SplitListOfGpsPoints {
    //public static final int DEFAULT_BLOCK_SIZE = 1000;
    public static List<Double> getLonList(List<gpsPoint> trajectory) {
        List<Double> lonList = new ArrayList<Double>();
        for(gpsPoint point : trajectory){
            lonList.add(point.getLongitude());
        }
        return lonList;
    }

    public static List<Double> getLatList(List<gpsPoint> trajectory) {
        List<Double> latList = new ArrayList<Double>();
        for(gpsPoint point : trajectory){
            latList.add(point.getLatitude());
        }
        return latList;
    }

    public static List<Long> getTimeList(List<gpsPoint> trajectory){
        List<Long> timeList = new ArrayList<Long>();
        for(gpsPoint point : trajectory){
            timeList.add(point.getTimestamp());
        }
        return timeList;
    }

    public static List<String> getUidList(List<gpsPoint> trajectory){
        List<String> UidList = new ArrayList<>();
        for(gpsPoint point : trajectory){
            UidList.add(point.getId());
        }
        return UidList;
    }
}
