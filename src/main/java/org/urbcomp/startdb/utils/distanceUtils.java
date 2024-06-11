package org.urbcomp.startdb.utils;

import org.urbcomp.startdb.gpsPoint;

public class distanceUtils {
    // 地球半径常量，单位为米
    private static final double EARTH_RADIUS = 6371393.0;
    public static double calcSED(gpsPoint s, gpsPoint m, gpsPoint e){
        double timeRatio = (double) (m.getTimestamp() - s.getTimestamp()) / (e.getTimestamp() - s.getTimestamp());
        double lon = s.getLongitude() + (e.getLongitude() - s.getLongitude()) * timeRatio;
        double lat = s.getLatitude() + (e.getLatitude() - s.getLatitude()) * timeRatio;
        return haversine(lat, lon, m.getLatitude(), m.getLongitude());
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        // 将经纬度从度数转换为弧度
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // 计算经纬度差值
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // 应用 Haversine 公式
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 计算距离
        return EARTH_RADIUS * c;
    }

    public static double euclideanDistance(double lat1, double lon1, double lat2, double lon2) {
        // 将经纬度差值转换为公里

        // 纬度差值
        double dLat = lat2 - lat1;
        // 经度差值
        double dLon = lon2 - lon1;

        // 将经纬度差值转换为弧度
        double dLatRad = Math.toRadians(dLat);
        double dLonRad = Math.toRadians(dLon);

        // 计算实际的横向和纵向距离
        double latDist = dLatRad * EARTH_RADIUS;
        double lonDist = dLonRad * EARTH_RADIUS * Math.cos(Math.toRadians(lat1));

        // 计算欧式距离
        return Math.sqrt(latDist * latDist + lonDist * lonDist);
    }

//    public static void main(String[] args) {
//        // 示例坐标，264.7km
//        //重庆
////        double lat1 = 29.5591108;
////        double lon1 = 106.5564824;
////        //成都
////        double lat2 = 30.5792582;
////        double lon2 = 104.0682480;
//
//        //虎溪西
//        double lat1 = 29.6045919;
//        double lon1 = 106.2998552;
//        //虎溪东
//        double lat2 = 29.6048902;
//        double lon2 = 106.3138148;
//
//        double distance = haversine(lat1, lon1, lat2, lon2);
//        System.out.println("大圆距离为：" + distance + " 米");
//
//        double distance2 = euclideanDistance(lat1, lon1, lat2, lon2);
//        System.out.println("欧式距离为：" + distance2 + " 米");
//    }
}
