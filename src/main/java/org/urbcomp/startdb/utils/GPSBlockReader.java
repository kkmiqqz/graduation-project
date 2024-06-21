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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public GPSBlockReader(String fileName, int blockSize, String id) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(fileName));
        this.blockSize = blockSize;
        this.id = id;
    }

    public gpsPoint nextPoint() throws IOException {
        if (end) {
            return null;
        }
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#") || line.equals("")) {
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
                String timestampStr = values[0];
                int decimalIndex = timestampStr.indexOf('.');
                if (decimalIndex != -1) {
                    timestampStr = timestampStr.substring(0, decimalIndex);
                }
                long timestamp = dateFormat.parse(timestampStr).getTime();
                double longitude = Double.parseDouble(values[1]);
                double latitude = Double.parseDouble(values[2]);
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