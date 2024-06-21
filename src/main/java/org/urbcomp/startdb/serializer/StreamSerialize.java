package org.urbcomp.startdb.serializer;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.selfstar.compressor.ElfPlusCompressor;
import org.urbcomp.startdb.selfstar.compressor.ICompressor;
import org.urbcomp.startdb.selfstar.compressor.xor.ElfPlusXORCompressor;
import org.urbcomp.startdb.utils.StreamTimeCompressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class StreamSerialize {
    private final StreamTimeCompressor timeCompressor = new StreamTimeCompressor();
    private final ICompressor compressor = new ElfPlusCompressor(new ElfPlusXORCompressor());
    private final HashMap<String, Deque<Double>> lonWindow = new HashMap<>();
    private final HashMap<String, Deque<Double>> latWindow = new HashMap<>();
    private String prevId = null;  // 记录上一个点的 UID
    private static final int WINDOW_SIZE = 5;  // 窗口大小

    public byte[] serialize(gpsPoint point) throws IOException {
        byte[] timeBytes = timeCompressor.compressTime(point.getTimestamp());
        String id = point.getId();
        double currentLon = point.getLongitude();
        double currentLat = point.getLatitude();

        // 获取或初始化窗口
        lonWindow.putIfAbsent(id, new ArrayDeque<>());
        latWindow.putIfAbsent(id, new ArrayDeque<>());
        Deque<Double> lonDeque = lonWindow.get(id);
        Deque<Double> latDeque = latWindow.get(id);

        // 预测经纬度
        double predictedLon = predict(lonDeque);
        double predictedLat = predict(latDeque);

        // 计算预测误差
        double deltaLon = currentLon - predictedLon;
        double deltaLat = currentLat - predictedLat;

        // 压缩预测误差
        compressor.addValue(deltaLon);
        compressor.close();
        byte[] lonBytes = compressor.getBytes();
        compressor.refresh();

        compressor.addValue(deltaLat);
        compressor.close();
        byte[] latBytes = compressor.getBytes();
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

        writeVarInt(out, timeBytes.length);
        out.write(timeBytes);
        writeVarInt(out, lonBytes.length);
        out.write(lonBytes);
        writeVarInt(out, latBytes.length);
        out.write(latBytes);

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