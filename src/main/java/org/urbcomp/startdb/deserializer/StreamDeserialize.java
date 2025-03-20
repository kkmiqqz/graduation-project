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
import java.util.List;

public class StreamDeserialize {
    private final StreamTimeDecompressor timeDecompressor = new StreamTimeDecompressor();
    private final IDecompressor decompressor = new ElfPlusDecompressor(new ElfPlusXORDecompressor());
    private final HashMap<String, Deque<Double>> lonWindow = new HashMap<>();
    private final HashMap<String, Deque<Double>> latWindow = new HashMap<>();
    private String currentId = null;  // 当前轨迹的 UID
    private static final int WINDOW_SIZE = 2;  // 窗口大小

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

        // 读取合并的经纬度压缩数据
        int combinedLonLatLen = readVarInt(in);
        byte[] combinedLonLatBytes = new byte[combinedLonLatLen];
        in.read(combinedLonLatBytes);

        // 解压经度和纬度
        decompressor.setBytes(combinedLonLatBytes);
        List<Double> values = decompressor.decompress();
        if (values.size() < 2) {
            throw new IOException("Expected at least 2 values (lon and lat deltas), got " + values.size());
        }
        double deltaLon = values.get(0);  // 第一个值为经度差值
        double deltaLat = values.get(1);  // 第二个值为纬度差值
        decompressor.refresh();

        // 获取或初始化窗口
        Deque<Double> lonDeque = lonWindow.computeIfAbsent(currentId, k -> new ArrayDeque<>());
        Deque<Double> latDeque = latWindow.computeIfAbsent(currentId, k -> new ArrayDeque<>());

        // 预测
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
        deque.addLast(last);
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