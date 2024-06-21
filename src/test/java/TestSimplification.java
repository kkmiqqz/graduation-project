import org.junit.Test;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.simplificator.DPhull;
import org.urbcomp.startdb.simplificator.ISimplificator;
import org.urbcomp.startdb.simplificator.TD_TR;
import org.urbcomp.startdb.simplificator.VOLTCom;
import org.urbcomp.startdb.simplificator.VOLTComCommon.vector;
import utils.GPSBlockReader;

import java.io.File;
import java.util.List;

public class TestSimplification {
    //数据集
    String inputFileName = "src/main/resources/test";
    private String dataName = "Geolife";

    @Test
    public void testAll(){

    }
    @Test
    public void testTD_TR(){
        for(double epsi = 10.0; epsi <= 100.0; epsi += 10.0) {
            //metric
            double simplifyTime = 0.0;
            int originNum = 0;
            int simpNum = 0;

            //读取文件
            File folder = new File(inputFileName);
            File[] files = folder.listFiles();

            if (files == null) {
                System.out.println("file is empty");
                return;
            }

            for (File file : files) {
                if (file.isFile()) {
                    String filename = file.getName();
                    //System.out.println("Reading file : " + file.getName());

                    //简化器
                    TD_TR simplificator = new TD_TR(epsi);

                    try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, 1000, dataName)) {
                        List<gpsPoint> gpsPoints;

                        while ((gpsPoints = br.nextBlock()) != null) {

                            //简化
                            double sSimpTime = System.currentTimeMillis();
                            //Set<Integer> comPoints = simplificator.compress(0, gpsPoints.size() - 1, gpsPoints);
                            List<gpsPoint> simpPoints = simplificator.simplify(gpsPoints);
                            double eSimpTime = System.currentTimeMillis();

                            simplifyTime += eSimpTime - sSimpTime;
                            originNum += gpsPoints.size();
                            simpNum += simpPoints.size();

//                        //解压缩
//                        double sDecTime = System.currentTimeMillis();
//                        List<gpsPoint> decValues = deserializer.deserialize(comValues);
//                        double eDecTime = System.currentTimeMillis();
//
//                        decompressTime += eDecTime - sDecTime;
//
//                        //校验
//                        System.out.println("originSize: " + gpsPoints.size());
//                        System.out.println("decompressSize: " + decValues.size());
//                        if (decValues.equals(gpsPoints)) {
//                        } else {
//                            System.out.println("decompress wrong!!!");
//                        }

                        }
                    } catch (Exception e) {
                        throw new RuntimeException(filename, e);
                    }

                }
            }
            System.out.println("epsilon: " + epsi);
            System.out.println("comNum: " + simpNum);
            System.out.println("originNum: " + originNum);
            System.out.println("简化率：" + (double)simpNum/originNum);
            System.out.println("简化时间：" + simplifyTime + "ms");
            System.out.println();
        }
    }

    @Test
    public void testVOLTCom() {
        for(double epsi = 10.0; epsi <= 100.0; epsi += 10.0) {

            //metric
            double simplifyTime = 0.0;
            int originSize = 0;
            int simpSize = 0;

            //读取文件
            File folder = new File(inputFileName);
            File[] files = folder.listFiles();

            if (files == null) {
                System.out.println("file is empty");
                return;
            }

            for (File file : files) {
                if (file.isFile()) {
                    String filename = file.getName();
                    //System.out.println("Reading file : " + file.getName());

                    //简化器
                    VOLTCom simplificator = new VOLTCom(epsi);

                    try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, 1000, dataName)) {
                        List<gpsPoint> gpsPoints;

                        while ((gpsPoints = br.nextBlock()) != null) {

                            //简化
                            double sSimpTime = System.currentTimeMillis();
                            List<vector> simpPoints = simplificator.extractVectorOnlyOne(gpsPoints);
                            //List<vector> simpPoints = simplificator.extractVectorOrigin(gpsPoints);
                            //List<vector> simpPoints = simplificator.extractVectorAlternate(gpsPoints);
                            double eSimpTime = System.currentTimeMillis();

                            simplifyTime += eSimpTime - sSimpTime;
                            originSize += gpsPoints.size();
                            simpSize += simpPoints.size();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(filename, e);
                    }

                }
            }
            System.out.println("epsilon: " + epsi);
            System.out.println("simpSize: " + simpSize);
            System.out.println("originSize: " + originSize);
            System.out.println("简化率：" + (double)simpSize/originSize);
            System.out.println("简化时间：" + simplifyTime + "ms");
            System.out.println();
        }
    }

    @Test
    public void testDPhull() {
        for(double epsi = 0.00005; epsi <= 0.00005; epsi += 10.0) {

            //metric
            double simplifyTime = 0.0;
            int originSize = 0;
            int simpSize = 0;

            //读取文件
            File folder = new File(inputFileName);
            File[] files = folder.listFiles();

            if (files == null) {
                System.out.println("file is empty");
                return;
            }

            for (File file : files) {
                if (file.isFile()) {
                    String filename = file.getName();
                    //System.out.println("Reading file : " + file.getName());

                    //简化器
                    DPhull simplificator = new DPhull(epsi);

                    try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, 1000, dataName)) {
                        List<gpsPoint> gpsPoints;

                        while ((gpsPoints = br.nextBlock()) != null) {

                            //简化
                            double sSimpTime = System.currentTimeMillis();
                            List<gpsPoint> simpPoints = simplificator.simplify(gpsPoints);
                            double eSimpTime = System.currentTimeMillis();

                            simplifyTime += eSimpTime - sSimpTime;
                            originSize += gpsPoints.size();
                            simpSize += simpPoints.size();
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(filename, e);
                    }
                }
            }
            System.out.println("epsilon: " + epsi);
            System.out.println("comSize: " + simpSize);
            System.out.println("originSize: " + originSize);
            System.out.println("简化率：" + (double)simpSize/originSize);
            System.out.println("简化时间：" + simplifyTime + "ms");
            System.out.println();
        }
    }
}
