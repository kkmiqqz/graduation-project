package org.urbcomp.startdb.utils;

public class StreamTimeCompressor {
    private Long previousTime = null;
    private Long previousDelta = null;

    public byte[] compressTime(long time) {
        byte[] compressed;
        if (previousTime == null) {
            // 第一个时间点，直接传输
            compressed = VarLongUtils.toVarLongBytes(time);
            previousDelta = null; // 初始化
        } else if (previousDelta == null) {
            // 第二个时间点，传输第一次差分
            long delta = time - previousTime;
            compressed = VarLongUtils.toVarLongBytes(delta);
            previousDelta = delta;
        } else {
            // 后续时间点，传输差分差
            long currentDelta = time - previousTime;
            long deltaOfDelta = currentDelta - previousDelta;
            compressed = VarLongUtils.toVarLongBytes(deltaOfDelta);
            previousDelta = currentDelta;
        }
        previousTime = time;
        return compressed;
    }
}