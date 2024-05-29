package org.urbcomp.startdb;

public class gpsPoint  {
    private String id;
    private long timestamp;
    private double longitude;
    private double latitude;

    public gpsPoint(String id, long timestamp, double longitude, double latitude) {
        this.id = id;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public gpsPoint() { //反序列化时需要
        this.id = "0";
        this.timestamp = 0;
        this.longitude = 0.0;
        this.latitude = 0.0;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    boolean equals(gpsPoint point){
        return this.getId() == point.getId() && this.getTimestamp() == point.getTimestamp()
                && this.getLongitude() == point.getLongitude() && this.getLatitude() == point.getLatitude();
    }
}
