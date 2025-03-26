package org.urbcomp.startdb.api;

public class CompressionResult {
    private long totalPoints;
    private double averageCompressTime;      // 毫秒
    private double averageDecompressTime;    // 毫秒
    private double compressionRatio;

    // Getter & Setter
    public long getTotalPoints() {
        return totalPoints;
    }
    public void setTotalPoints(long totalPoints) {
        this.totalPoints = totalPoints;
    }
    public double getAverageCompressTime() {
        return averageCompressTime;
    }
    public void setAverageCompressTime(double averageCompressTime) {
        this.averageCompressTime = averageCompressTime;
    }
    public double getAverageDecompressTime() {
        return averageDecompressTime;
    }
    public void setAverageDecompressTime(double averageDecompressTime) {
        this.averageDecompressTime = averageDecompressTime;
    }
    public double getCompressionRatio() {
        return compressionRatio;
    }
    public void setCompressionRatio(double compressionRatio) {
        this.compressionRatio = compressionRatio;
    }
}
