import org.junit.Test;
import org.urbcomp.startdb.deserializer.ElfOriginDeserializer;
import org.urbcomp.startdb.deserializer.IDeserializer;
import org.urbcomp.startdb.deserializer.KryoOriginDeserializer;
import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.serializer.ElfOriginSerializer;
import org.urbcomp.startdb.serializer.ISerializer;
import org.urbcomp.startdb.serializer.KryoOriginSerializer;
import org.urbcomp.startdb.simplificator.TD_TR;
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
        //metric
        double simplifyTime = 0.0;
        int originNum = 0;
        int simpNum = 0;

        double compressTime = 0;
        double decompressTime = 0;
        long compressedByteSize = 0;
        long originByteSize = 0;

        //简化器
        TD_TR simplificator = new TD_TR(100.0);
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
                        //简化
                        double sSimpTime = System.currentTimeMillis();
                        //Set<Integer> comPoints = simplificator.compress(0, gpsPoints.size() - 1, gpsPoints);
                        List<gpsPoint> simpPoints = simplificator.simplify(gpsPoints);
                        double eSimpTime = System.currentTimeMillis();

                        simplifyTime += eSimpTime - sSimpTime;
                        originNum += gpsPoints.size();
                        simpNum += simpPoints.size();

                        //转换
                        List<String> UidList = utils.SplitListOfGpsPoints.getUidList(simpPoints);
                        List<Long> TimeList = utils.SplitListOfGpsPoints.getTimeList(simpPoints);
                        List<Double> LonList = utils.SplitListOfGpsPoints.getLonList(simpPoints);
                        List<Double> LatList = utils.SplitListOfGpsPoints.getLatList(simpPoints);

                        //压缩
                        double sComTime = System.currentTimeMillis();
                        serializer.compressTime(TimeList);
                        serializer.compressLon(LonList);
                        serializer.compressLat(LatList);
                        serializer.compressUid(UidList);
                        double eComTime = System.currentTimeMillis();

                        compressTime += eComTime - sComTime;
                        //不考虑uid
                        compressedByteSize += serializer.getByteSize();
                        originByteSize += simpPoints.size()*(24);

                        //获取压缩流
                        byte[] timeStream = serializer.getTimestream();
                        //System.out.println(timeStream.length);
                        byte[] lonStream = serializer.getLonstream();
                        byte[] latStream = serializer.getLatstream();

                        //解压缩
                        double sDecTime = System.currentTimeMillis();
                        deserializer.decompressTime(timeStream);
                        deserializer.decompressLon(lonStream);
                        deserializer.decompressLat(latStream);
                        double eDecTime = System.currentTimeMillis();

                        decompressTime += eDecTime - sDecTime;

                        //校验
                        //获取解压数据
                        List<Long> timestamp = deserializer.getTimestamp();
                        List<Double> longitude = deserializer.getLongitude();
                        List<Double> latitude = deserializer.getLatitude();
//                        System.out.println(timestamp.size());
//                        System.out.println(longitude.size());
//                        System.out.println(latitude.size());

                        //System.out.println("originSize: " + gpsPoints.size());
                        //System.out.println("decompressSize: " + deserializer.getSize());
                        //时间校验
//                        if(!timestamp.equals(TimeList)) {
//                            System.out.println("time wrong!!!");
//                        }
                        //经纬度校验
                        if(!longitude.equals(LonList)){
                            System.out.println("lon wrong!!!");
                        }
                        if(!latitude.equals(LatList)){
                            System.out.println("lon wrong!!!");
                        }
                        //System.out.println();

                    }
                } catch (Exception e) {
                    throw new RuntimeException(filename, e);
                }

                //



            }
            //System.out.println();
        }


        //压缩方式

        //打印结果：压缩率、压缩时间、解压缩时间
        System.out.println("简化时间：" + simplifyTime + "ms");
        System.out.println("comNum: " + simpNum);
        System.out.println("originNum: " + originNum);
        System.out.println("简化率：" + (double)simpNum/originNum);

        System.out.println("compressTime: " + compressTime + " ms");
        System.out.println("decompressTime: " + decompressTime + " ms");
        System.out.println("压缩率：" + (double)(compressedByteSize)/(originByteSize));

        System.out.println("总压缩率：" + (double)(compressedByteSize)/(originNum*24));
    }
}
