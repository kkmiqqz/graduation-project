package org.urbcomp.startdb.serializer;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.selfstar.compressor.ElfPlusCompressor;
import org.urbcomp.startdb.selfstar.compressor.xor.ElfXORCompressor;
import org.urbcomp.startdb.utils.StreamTimeCompressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class StreamSerialize {
    private final StreamTimeCompressor timeCompressor = new StreamTimeCompressor();
    private final ElfPlusCompressor compressor = new ElfPlusCompressor(new ElfXORCompressor());
    private final HashMap<String, Deque<Double>> lonWindow = new HashMap<>();
    private final HashMap<String, Deque<Double>> latWindow = new HashMap<>();
    private String prevId = null;  // 记录上一个点的 UID
    private static final int WINDOW_SIZE = 9;  // 窗口大小
    // 新增统计字段
    private int currentTimeOriginalSize;
    private int currentTimeCompressedSize;
    private int currentLonLatOriginalSize;
    private int currentLonLatCompressedSize;
    // 新增统计字段的获取方法
    public int getCurrentTimeOriginalSize() { return currentTimeOriginalSize; }
    public int getCurrentTimeCompressedSize() { return currentTimeCompressedSize; }
    public int getCurrentLonLatOriginalSize() { return currentLonLatOriginalSize; }
    public int getCurrentLonLatCompressedSize() { return currentLonLatCompressedSize; }
    // 新增 UID 统计字段
    private int currentUidOriginalSize;
    private int currentUidCompressedSize;

    // 新增获取方法
    public int getCurrentUidOriginalSize() { return currentUidOriginalSize; }
    public int getCurrentUidCompressedSize() { return currentUidCompressedSize; }

    public byte[] serialize(gpsPoint point) throws IOException {
        byte[] timeBytes = timeCompressor.compressTime(point.getTimestamp()); //时间压缩
        currentTimeOriginalSize = 8;  // 原始时间戳为 long 型（8字节）
        currentTimeCompressedSize = timeBytes.length;
        String id = point.getId();
        double currentLon = point.getLongitude();
        double currentLat = point.getLatitude();

        // 获取或初始化窗口
        Deque<Double> lonDeque = lonWindow.computeIfAbsent(id, k -> new ArrayDeque<>());
        Deque<Double> latDeque = latWindow.computeIfAbsent(id, k -> new ArrayDeque<>());

        // 预测
        double predictedLon = predict(lonDeque);
        double predictedLat = predict(latDeque);
/*
        System.out.printf("currentLon= %f\n",currentLon);
        System.out.printf("currentLat= %f\n",currentLat);
        System.out.printf("predictedLon= %f\n",predictedLon);
        System.out.printf("predictedLat= %f\n",predictedLat);
*/

        // 计算预测误差
        double deltaLon = currentLon - predictedLon;
        double deltaLat = currentLat - predictedLat;
/*        System.out.printf("deltaLon= %f\n",deltaLon);
        System.out.printf("deltaLat= %f\n",deltaLat);*/
        // 压缩预测误差（经度和纬度合并）
        compressor.addValue(deltaLon);
        compressor.addValue(deltaLat);
        //compressor.close();
        compressor.flush();
        byte[] combinedLonLatBytes = compressor.getBytes();
        currentLonLatOriginalSize = 16;  // 原始经纬度为两个 double（各8字节）
        currentLonLatCompressedSize = combinedLonLatBytes.length;
        compressor.refresh();

        // 组合数据
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean isNewId = !id.equals(prevId);
        out.write(isNewId ? 1 : 0);  // 写入标志位

        if (isNewId) {
            byte[] uidBytes = id.getBytes();
            writeVarInt(out, uidBytes.length);
            out.write(uidBytes);
            prevId = id;
        }
        currentUidOriginalSize= id.getBytes().length;
        currentUidCompressedSize=1;
        writeVarInt(out, timeBytes.length);
        out.write(timeBytes);
        writeVarInt(out, combinedLonLatBytes.length);
        out.write(combinedLonLatBytes);

        byte[] combined = out.toByteArray();

        // 更新窗口
        updateWindow(lonDeque, currentLon);
        updateWindow(latDeque, currentLat);

        return combined;
    }

    private double predict(Deque<Double> deque) {
        if (deque.size() < 2) {
            return deque.isEmpty() ? 0.0 : deque.getLast();
        }
        double last = deque.removeLast();
        double secondLast = deque.getLast();
        deque.addLast(last);  // 恢复
        return 2 * last - secondLast;  // 线性预测
    }

    private void updateWindow(Deque<Double> deque, double value) {
        if (deque.size() >= WINDOW_SIZE) {
            deque.removeFirst();
        }
        deque.addLast(value);
    }

    private void writeVarInt(ByteArrayOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}