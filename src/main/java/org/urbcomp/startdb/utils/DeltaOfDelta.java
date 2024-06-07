package org.urbcomp.startdb.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Random;

public class DeltaOfDelta {

    public static byte[] compress(long[] data) throws IOException {
        if (data.length == 0) {
            return new byte[0];
        }

        // 第一次差分 (Delta)
        long[] deltas = new long[data.length];
        deltas[0] = data[0];
        for (int i = 1; i < data.length; i++) {
            deltas[i] = data[i] - data[i - 1];
        }

        // 第二次差分 (Delta of Delta)
        long[] deltaOfDeltas = new long[deltas.length];
        deltaOfDeltas[0] = deltas[0];
        for (int i = 1; i < deltas.length; i++) {
            deltaOfDeltas[i] = deltas[i] - deltas[i - 1];
        }

        // 变长编码
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (long value : deltaOfDeltas) {
            writeVarLong(output, value);
        }

        return output.toByteArray();
    }

    public static byte[] compress(List<Long> data) throws IOException {
        if (data.isEmpty()) {
            return new byte[0];
        }

        // 第一次差分 (Delta)
        long[] deltas = new long[data.size()];
        deltas[0] = data.get(0);  // 第一个数据点作为初始值
        for (int i = 1; i < data.size(); i++) {
            deltas[i] = data.get(i) - data.get(i - 1);
        }

        // 第二次差分 (Delta of Delta)
        long[] deltaOfDeltas = new long[data.size()];
        deltaOfDeltas[0] = deltas[0];  // 第一个 Delta 值作为初始值
        for (int i = 1; i < deltas.length; i++) {
            deltaOfDeltas[i] = deltas[i] - deltas[i - 1];
        }

        // 变长编码
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (long value : deltaOfDeltas) {
            writeVarLong(output, value);
        }

        return output.toByteArray();
    }

//    public static long[] decompress(byte[] compressedData) throws IOException {
//        ByteArrayInputStream input = new ByteArrayInputStream(compressedData);
//        List<Long> deltaOfDeltas = new ArrayList<>();
//
//        // 解码变长编码字节流
//        long value;
//        while ((value = readVarLong(input)) != Long.MIN_VALUE) {
//            deltaOfDeltas.add(value);
//        }
//
//        // 重建 Delta 数列
//        long[] deltas = new long[deltaOfDeltas.size()];
//        if (!deltaOfDeltas.isEmpty()) {
//            deltas[0] = deltaOfDeltas.get(0); // 假设初始 Delta 为第一个 Delta of Delta 值
//            for (int i = 1; i < deltaOfDeltas.size(); i++) {
//                deltas[i] = deltas[i - 1] + deltaOfDeltas.get(i);
//            }
//        }
//
//        // 重建原始数列
//        long[] originalData = new long[deltas.length];
//        if(deltas.length != 0){
//            originalData[0] = deltas[0];
//            for (int i = 1; i < originalData.length; i++) {
//                originalData[i] = originalData[i - 1] + deltas[i];
//            }
//        }
//
//        return originalData;
//    }

    public static List<Long> decompress(byte[] compressedData) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(compressedData);
        // 验证 ByteArrayInputStream 的可用字节数
        //System.out.println("Available bytes: " + input.available());

        List<Long> deltaOfDeltas = new ArrayList<>();

        //有问题，deltaOfDeltas size为1，代解决

        // 解码变长编码字节流
        long value;
        while ((value = readVarLong(input)) != Long.MIN_VALUE) {
            deltaOfDeltas.add(value);
        }

        //System.out.println("deltaOfDeltas size: " + deltaOfDeltas.size());

        // 重建 Delta 数列
        long[] deltas = new long[deltaOfDeltas.size()];
        if (!deltaOfDeltas.isEmpty()) {
            deltas[0] = deltaOfDeltas.get(0); // 假设初始 Delta 为第一个 Delta of Delta 值
            for (int i = 1; i < deltaOfDeltas.size(); i++) {
                deltas[i] = deltas[i - 1] + deltaOfDeltas.get(i);
            }
        }

        // 重建原始数列
        List<Long> originalData = new ArrayList<>();
        if(deltas.length != 0){
            originalData.add(deltas[0]);
            for (int i = 1; i < deltas.length; i++) {
                long pre = originalData.get(i - 1);
                originalData.add(pre + deltas[i]) ;
            }
        }

        //System.out.println("originalData size: " + originalData.size());

        return originalData;
    }

    private static void writeVarLong(ByteArrayOutputStream out, long value) throws IOException {
        value = (value << 1) ^ (value >> 63); // Apply ZigZag encoding
        while ((value & ~0x7FL) != 0) { //若等于0，则说明第7位之后全为0
            out.write((byte) ((value & 0x7F) | 0x80)); //第8位置1，表示后面还有字节
            value >>>= 7; //无符号右移
        }
        out.write((byte) value);
    }

    private static long readVarLong(ByteArrayInputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (shift < 64) {
            //final byte b = (byte) in.read();
            int b = in.read(); //使用int可以避免读取到值为-1的byte时造成的提前结束
            if (b == -1) { // End of stream，流中数据读取完时，返回-1
                return Long.MIN_VALUE;
            }
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return (result >>> 1) ^ -(result & 1); // Decode ZigZag back to a signed long
            }
            shift += 7;
        }
        throw new RuntimeException("Malformed variable-length integer");
    }

    public static void main(String[] args) {
        try {
            // 创建一个长度为1000的long数组
            long[] timestamps = new long[2000];
            // 创建一个Random对象
            Random random = new Random();
            // 循环赋值随机数
            for (int i = 0; i < timestamps.length; i++) {
                timestamps[i] = random.nextLong() + 1;
            }

            //compress
            byte[] compressedData = compress(timestamps);
            System.out.println("Compressed Data Length: " + compressedData.length);

            //decompress
            List<Long> decompressedTime = decompress(compressedData);
//            for(long data : decompressedTime){
//                System.out.println(data);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
