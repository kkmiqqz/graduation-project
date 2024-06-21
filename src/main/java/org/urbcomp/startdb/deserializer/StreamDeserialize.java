package org.urbcomp.startdb.deserializer;

import org.urbcomp.startdb.gpsPoint;
import org.urbcomp.startdb.selfstar.decompressor.ElfPlusDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.IDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.xor.ElfPlusXORDecompressor;
import org.urbcomp.startdb.utils.StreamTimeDecompressor;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;

public class StreamDeserialize {
    private final StreamTimeDecompressor timeDecompressor = new StreamTimeDecompressor();
    private final IDecompressor decompressor = new ElfPlusDecompressor(new ElfPlusXORDecompressor());
    private final HashMap<String, Deque<Double>> lonWindow = new HashMap<>();
    private final HashMap<String, Deque<Double>> latWindow = new HashMap<>();
    private String currentId = null;  // 当前轨迹的 UID
    private static final int WINDOW_SIZE = 5;  // 窗口大小

    public gpsPoint deserialize(byte[] compressed) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(compressed);

        // 读取标志位
        int flag = in.read();
        if (flag == 1) {
            int uidLen = readVarInt(in);
            byte[] uidBytes = new byte[uidLen];
            in.read(uidBytes);
            currentId = new String(uidBytes);
        } else if (flag == 0) {
            if (currentId == null) {
                throw new IOException("UID not initialized for the first point");
            }
        } else {
            throw new IOException("Invalid flag value: " + flag);
        }

        // 读取时间
        int timeLen = readVarInt(in);
        byte[] timeBytes = new byte[timeLen];
        in.read(timeBytes);
        long time = timeDecompressor.decompressTime(timeBytes);

        // 读取经度误差
        int lonLen = readVarInt(in);
        byte[] lonBytes = new byte[lonLen];
        in.read(lonBytes);
        decompressor.setBytes(lonBytes);
        double deltaLon = decompressor.decompress().get(0);
        decompressor.refresh();

        // 读取纬度误差
        int latLen = readVarInt(in);
        byte[] latBytes = new byte[latLen];
        in.read(latBytes);
        decompressor.setBytes(latBytes);
        double deltaLat = decompressor.decompress().get(0);
        decompressor.refresh();

        // 获取或初始化窗口
        lonWindow.putIfAbsent(currentId, new ArrayDeque<>());
        latWindow.putIfAbsent(currentId, new ArrayDeque<>());
        Deque<Double> lonDeque = lonWindow.get(currentId);
        Deque<Double> latDeque = latWindow.get(currentId);

        // 预测经纬度
        double predictedLon = predict(lonDeque);
        double predictedLat = predict(latDeque);

        // 重建绝对经纬度
        double currentLon = predictedLon + deltaLon;
        double currentLat = predictedLat + deltaLat;

        // 创建 GPS 点
        gpsPoint point = new gpsPoint(currentId, time, currentLon, currentLat);

        // 更新窗口
        updateWindow(lonDeque, currentLon);
        updateWindow(latDeque, currentLat);

        return point;
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

    private int readVarInt(ByteArrayInputStream in) throws IOException {
        int value = 0;
        int shift = 0;
        while (true) {
            int b = in.read();
            if (b == -1) {
                throw new EOFException("Unexpected end of stream");
            }
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
            shift += 7;
            if (shift >= 32) {
                throw new IOException("VarInt too large");
            }
        }
    }
}