package utils;

import org.codehaus.jackson.map.ObjectMapper;
import org.urbcomp.startdb.gpsPoint;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GPSBlockReader implements Closeable {
    //private static final String dir = "src/main/resources/floating/";
    private final int blockSize;
    private String dataName;
    private final BufferedReader br;
    private boolean end = false;
    SimpleDateFormat TdriveDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat chengduDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    public GPSBlockReader(String fileName, int blockSize, String dataName) throws FileNotFoundException {
        this.br = new BufferedReader(new FileReader(fileName));
        this.blockSize = blockSize;
        this.dataName = dataName;
    }

    public List<gpsPoint> nextBlock() throws IOException, ParseException {
        if (end) {
            return null;
        }
        List<gpsPoint> gpsPoints = new ArrayList<>(blockSize);

        //解析不同数据集
        switch (dataName){
            case "T-drive" :
                parseTdrive(gpsPoints);
                break;
            case "Chengdu" :
                parseChengdu(gpsPoints);
                break;
            case "Geolife" :
                parseGeolife(gpsPoints);
                break;
            default :
                System.out.println("Invalid dataset");
                break;
        }

        if (gpsPoints.isEmpty()) {
            return null;
        }

        return gpsPoints;
    }

    /**
     * T-drive每行格式都一样：1,2008-02-02 15:36:08,116.51172,39.92123
     * @param gpsPoints
     * @throws IOException
     * @throws ParseException
     */
    public void parseTdrive(List<gpsPoint> gpsPoints) throws IOException, ParseException {
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
                    TdriveDateFormat.parse(values[1]).getTime(),
                    Double.parseDouble(values[2]),
                    Double.parseDouble(values[3])
            );
            gpsPoints.add(gpsPoint);
            i++;
        }

        if (i < blockSize) {
            end = true;
        }
    }

    /**
     * 成都数据集格式：第一行 dceae818438b836e3d306296b4ccfbd，后续 2018-09-30 19:15:38.0,104.04235,30.69204 ...
     * 轨迹较短，部分数据集均小于1000条
     * @param gpsPoints
     * @throws IOException
     * @throws ParseException
     */
    public void parseChengdu(List<gpsPoint> gpsPoints) throws IOException, ParseException {
        int i = 0;
        String line;
        String id = null;
        while (i < blockSize && (line = br.readLine()) != null) {
            //第一行
            if(i == 0){
                id = line;

            } else {
                //后续
                String[] values = line.split(",");
                gpsPoint gpsPoint = new gpsPoint(
                        id,
                        chengduDateFormat.parse(values[0]).getTime(),
                        Double.parseDouble(values[1]),
                        Double.parseDouble(values[2])
                );
                gpsPoints.add(gpsPoint);
            }

            i++;
        }

        if (i < blockSize) {
            end = true;
        }
    }

    /**
     * 前6行没用，后续格式：39.998875,116.324514,0,492,39766.4268055556,2008-11-14,10:14:36
     * @param gpsPoints
     * @throws IOException
     * @throws ParseException
     */
    public void parseGeolife(List<gpsPoint> gpsPoints) throws IOException, ParseException {
        int i = 0;
        String line;
        String id = null;
        while (i < blockSize && (line = br.readLine()) != null) {
            String[] values = line.split(",");
            if(values.length == 7) {
                gpsPoint gpsPoint = new gpsPoint(
                        "Uid",
                        TdriveDateFormat.parse(values[5] + " " + values[6]).getTime(),
                        Double.parseDouble(values[1]),
                        Double.parseDouble(values[0])
                );
                gpsPoints.add(gpsPoint);
                i++;
            }
        }

        if (i < blockSize) {
            end = true;
        }
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
