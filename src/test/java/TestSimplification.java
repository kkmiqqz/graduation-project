import org.junit.Test;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.simplificator.TD_TR;
import org.urbcomp.startdb.utils.GPSBlockReader;

import java.io.File;
import java.util.List;

public class TestSimplification {
    //数据集
    String inputFileName = "src/main/resources/T-drive";

    @Test
    public  void testAll(){
        //metric
        double simplifyTime = 0.0;
        int originNum = 0;
        int simpNum = 0;

        //简化器
        TD_TR simplificator = new TD_TR(100.0);

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
                System.out.println("Reading file : " + file.getName());

                try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, 1000)) {
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
        System.out.println("comNum: " + simpNum);
        System.out.println("originNum: " + originNum);
        System.out.println("简化率：" + (double)simpNum/originNum);
        System.out.println("简化时间：" + simplifyTime + "ms");
    }
}
