package org.urbcomp.startdb.simplificator.utils;

import org.urbcomp.startdb.gpsPoint;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GPSBlockReader implements Closeable {
    //private static final String dir = "src/main/resources/floating/";
    private final int blockSize;
    private final BufferedReader br;
    private boolean end = false;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public GPSBlockReader(String fileName, int blockSize) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(fileName));
        this.blockSize = blockSize;
    }

    public List<gpsPoint> nextBlock() throws IOException, ParseException {
        if (end) {
            return null;
        }
        List<gpsPoint> gpsPoints = new ArrayList<>(blockSize);
        int i = 0;
        String line;
        while (i < blockSize && (line = br.readLine()) != null) {
//            if (line.startsWith("#") || line.equals("")) {
//                continue;
//            }
            //解析T-drive
            String[] values = line.split(",");
            gpsPoint gpsPoint = new gpsPoint(
                    values[0],
                    dateFormat.parse(values[1]).getTime(),
                    Double.parseDouble(values[2]),
                    Double.parseDouble(values[3])
            );
            gpsPoints.add(gpsPoint);
            i++;
        }
        if (i < blockSize) {
            end = true;
        }
        if (gpsPoints.isEmpty()) {
            return null;
        }

        return gpsPoints;
    }


    public List<Float> nextSingleBlock() throws IOException {
        if (end) {
            return null;
        }
        List<Float> floatings = new ArrayList<>(blockSize);
        int i = 0;
        String line;
        while (i < blockSize && (line = br.readLine()) != null) {
            if (line.startsWith("#") || line.equals("")) {
                continue;
            }
            i++;
            floatings.add(Float.parseFloat(line));

        }
        if (i < blockSize) {
            end = true;
        }
        if (floatings.isEmpty()) {
            return null;
        }

        return floatings;
    }

    @Override
    public void close() throws IOException {
        br.close();
    }
}
