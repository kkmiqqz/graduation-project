package org.urbcomp.startdb.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VarLongUtils {
    public static byte[] toVarLongBytes(long value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeVarLong(out, value);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeVarLong(ByteArrayOutputStream out, long value) throws IOException {
        value = (value << 1) ^ (value >> 63); // ZigZag编码
        while ((value & ~0x7FL) != 0) {
            out.write((byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((byte) value);
    }
}