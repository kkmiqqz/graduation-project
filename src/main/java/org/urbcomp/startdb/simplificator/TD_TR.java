package org.urbcomp.startdb.simplificator;

import org.urbcomp.startdb.serializer.ISerializer;
import org.urbcomp.startdb.simplificator.utils.GPSBlockReader;
import org.urbcomp.startdb.simplificator.utils.sedUtils;
import org.urbcomp.startdb.gpsPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TD_TR implements ISimplificator{
    private static double epsilon = 1000.0;

//    public TD_TR(double epsi){
//        epsilon = epsi;
//    }

    @Override
    public List<gpsPoint> simplify(List<gpsPoint> traj) {
        List<gpsPoint> res = new ArrayList<>();
        //空数据
        if(traj.isEmpty()){
            return res;
        }
        //简化
        Set<Integer> resIndex = compress(0, traj.size() - 1, traj);
        //index转换为List
        for(int index : resIndex){
            res.add(traj.get(index));
        }

        return res;
    }

    private static Set<Integer> compress(int start, int end, List<gpsPoint> traj) {
        Set<Integer> res = new LinkedHashSet<>();
        double sedMax = 0;
        int index = start;

        for (int i = start + 1; i < end; i++) {
            double d = sedUtils.calcSED(traj.get(start), traj.get(i), traj.get(end));
            if (d > sedMax) {
                index = i;
                sedMax = d;
            }
        }

        if (sedMax > epsilon) {
            Set<Integer> subRes1 = compress(start, index, traj);
            Set<Integer> subRes2 = compress(index, end, traj);
            res.addAll(subRes1);
            res.addAll(subRes2);
        } else {
            res.add(start);
            res.add(end);
        }

        return res;
    }

    public static void main(String[] args) {
        //测试
        String inputFileName = "src/main/resources/T-drive";
        double compressTime = 0.0;
        int originNum = 0;
        int comNum = 0;

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

                        //压缩
                        double sComTime = System.currentTimeMillis();
                        Set<Integer> comPoints = compress(0, gpsPoints.size() - 1, gpsPoints);
                        //List<gpsPoint> comPoints = simplify
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
//                        if (decValues.equals(gpsPoints)) {
//                        } else {
//                            System.out.println("decompress wrong!!!");
//                        }

                        //
                        originNum += gpsPoints.size();
                        comNum += comPoints.size();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(filename, e);
                }

            }
        }
        System.out.println("comNum: " + comNum);
        System.out.println("originNum: " + originNum);
        System.out.println("简化率：" + (double)comNum/originNum);
        System.out.println("简化时间：" + compressTime + "ms");
    }
}
