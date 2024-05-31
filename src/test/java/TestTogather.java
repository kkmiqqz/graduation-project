import org.junit.Test;
import org.urbcomp.startdb.deserializer.ElfOriginDeserializer;
import org.urbcomp.startdb.deserializer.IDeserializer;
import org.urbcomp.startdb.deserializer.KryoOriginDeserializer;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.ElfOriginSerializer;
import org.urbcomp.startdb.serializer.ISerializer;
import org.urbcomp.startdb.serializer.KryoOriginSerializer;
import utils.GPSBlockReader;

import java.io.File;
import java.util.List;

public class TestTogather {

    private static final int BLOCK_SIZE = 1000;

    //数据集
    private String inputFileName = "src/main/resources/T-drive";

    @Test
    public void testAll(){

    }

    @Test
    public void testTD_TRandElfOrigin(){
        //
        double compressTime = 0;
        double decompressTime = 0;
        long compressSize = 0;
        long decompressSize = 0;

        //序列化器与反序列化器
        ElfOriginSerializer serializer = new ElfOriginSerializer();
        ElfOriginDeserializer deserializer = new ElfOriginDeserializer();

        //读取文件
        File folder = new File(inputFileName);
        File[] files = folder.listFiles();

        if(files == null){
            System.out.println("file is empty");
            return;
        }

        for(File file : files){
            if(file.isFile()){
                String filename = file.getName();
                System.out.println("Reading file : " + file.getName());

                try (GPSBlockReader br = new GPSBlockReader(inputFileName + "/" + filename, BLOCK_SIZE)) {
                    List<gpsPoint> gpsPoints;

                    while ((gpsPoints = br.nextBlock()) != null) {
                        //转换
                        List<String> UidList = utils.SplitListOfGpsPoints.getUidList(gpsPoints);
                        List<Long> TimeList = utils.SplitListOfGpsPoints.getTimeList(gpsPoints);
                        List<Double> LonList = utils.SplitListOfGpsPoints.getLonList(gpsPoints);
                        List<Double> LatList = utils.SplitListOfGpsPoints.getLatList(gpsPoints);

                        //压缩
                        double sComTime = System.currentTimeMillis();
                        serializer.compressTime(TimeList);
                        serializer.compressLon(LonList);
                        serializer.compressLat(LatList);
                        serializer.compressUid(UidList);
                        double eComTime = System.currentTimeMillis();

                        compressTime += eComTime - sComTime;

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
//                        if(decValues.equals(gpsPoints)) {} else{
//                            System.out.println("decompress wrong!!!");
//                        }

                    }
                } catch (Exception e) {
                    throw new RuntimeException(filename, e);
                }

                //



            }
            System.out.println();
        }



        //压缩方式

        //打印结果：压缩率、压缩时间、解压缩时间
        System.out.println("compressTime: " + compressTime + " ms");
        System.out.println("decompressTime: " + decompressTime + " ms");
    }
}
