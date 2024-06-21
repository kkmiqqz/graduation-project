package org.urbcomp.startdb.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class StreamTimeDecompressor {
    private Long previousTime = null;
    private Long previousDelta = null;

    public long decompressTime(byte[] compressed) throws IOException {
        long value = readVarLong(compressed);
        long currentTime;
        if (previousTime == null) {
            // 第一个时间点，直接是时间
            currentTime = value;
            previousDelta = null; // 初始化
        } else if (previousDelta == null) {
            // 第二个时间点，是第一次差分
            long delta = value;
            currentTime = previousTime + delta;
            previousDelta = delta;
        } else {
            // 后续时间点，是差分差
            long deltaOfDelta = value;
            long currentDelta = previousDelta + deltaOfDelta;
            currentTime = previousTime + currentDelta;
            previousDelta = currentDelta;
        }
        previousTime = currentTime;
        return currentTime;
    }

    private long readVarLong(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        long result = 0;
        int shift = 0;
        while (shift < 64) {
            int b = in.read();
            if (b == -1) {
                return Long.MIN_VALUE;
            }
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return (result >>> 1) ^ -(result & 1); // 解码ZigZag
            }
            shift += 7;
        }
        throw new RuntimeException("Malformed variable-length integer");
    }
}
