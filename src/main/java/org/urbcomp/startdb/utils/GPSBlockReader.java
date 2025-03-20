package org.urbcomp.startdb.utils;

import org.urbcomp.startdb.gpsPoint;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class GPSBlockReader implements Closeable {
    private final int blockSize;
    private final BufferedReader br;
    private boolean end = false;
    private String id;

    public GPSBlockReader(String fileName, int blockSize, String id) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(fileName));
        this.blockSize = blockSize;
        this.id = id;
    }

    /**
     * 支持多种格式的数据：
     * 1. 3 列格式：时间, 经度, 纬度
     *    时间格式可能为 "yyyy-MM-dd HH:mm:ss.S" 或 "yyyy-MM-dd HH:mm:ss"
     *
     * 2. Geolife plt 格式（≥7列）：例如
     *    40.013867,116.306473,0,226,39744.9868518518,2008-10-23,23:41:04
     *    使用第1列（纬度）、第2列（经度），第6列和第7列合并为时间，格式 "yyyy-MM-dd HH:mm:ss"
     *
     * 3. CSV/txt 格式（≥4列）：例如
     *    1,2008-02-02 15:36:08,116.51172,39.92123
     *    使用第2列为时间（"yyyy-MM-dd HH:mm:ss"）、第3列为经度、第4列为纬度
     */
    public gpsPoint nextPoint() throws IOException {
        if (end) {
            return null;
        }
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] values = line.split(",");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            if (values.length < 3) {
                System.out.println("Skipping invalid line: " + line);
                continue;
            }
            try {
                long timestamp;
                double longitude;
                double latitude;
                SimpleDateFormat sdf;
                if (values.length == 3) {
                    // 格式：时间, 经度, 纬度
                    String timeStr = values[0];
                    if (timeStr.contains(".")) {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
                    } else {
                        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    }
                    timestamp = sdf.parse(timeStr).getTime();
                    longitude = Double.parseDouble(values[1]);
                    latitude = Double.parseDouble(values[2]);
                } else if (values.length >= 7) {
                    // Geolife plt 格式：例如
                    // 40.013867,116.306473,0,226,39744.9868518518,2008-10-23,23:41:04
                    latitude = Double.parseDouble(values[0]);
                    longitude = Double.parseDouble(values[1]);
                    String dateStr = values[5] + " " + values[6];
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    timestamp = sdf.parse(dateStr).getTime();
                } else if (values.length >= 4) {
                    // CSV/txt 格式：例如
                    // 1,2008-02-02 15:36:08,116.51172,39.92123
                    String dateStr = values[1];
                    sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    timestamp = sdf.parse(dateStr).getTime();
                    longitude = Double.parseDouble(values[2]);
                    latitude = Double.parseDouble(values[3]);
                } else {
                    System.out.println("Skipping unrecognized format: " + line);
                    continue;
                }
                return new gpsPoint(id, timestamp, longitude, latitude);
            } catch (NumberFormatException | ParseException e) {
                System.out.println("Skipping invalid line: " + line + " due to " + e);
                continue;
            }
        }
        end = true;
        return null;
    }

    @Override
    public void close() throws IOException {
        br.close();
    }
}
