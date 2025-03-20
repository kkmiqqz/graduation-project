package org.urbcomp.startdb;

public class gpsPoint {
    private String id;
    private long timestamp;
    private double longitude;
    private double latitude;
    private static final double EPS = 1e-19;

    public gpsPoint(String id, long timestamp, double longitude, double latitude) {
        this.id = id;
        this.timestamp = timestamp;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public gpsPoint(double longitude, double latitude) {
        this.id = null;
        this.timestamp = 0;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public gpsPoint() {
        this.id = "0";
        this.timestamp = 0;
        this.longitude = 0.0;
        this.latitude = 0.0;
    }

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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        gpsPoint other = (gpsPoint) obj;
        return this.getId().equals(other.getId()) &&
                this.getTimestamp() == other.getTimestamp() &&
                this.getLongitude() == other.getLongitude() &&
                this.getLatitude() == other.getLatitude();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "gpsPoint(" + latitude + ", " + longitude + ", " + timestamp + ")";
    }
}
