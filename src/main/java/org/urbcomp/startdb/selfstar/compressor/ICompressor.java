package org.urbcomp.startdb.selfstar.compressor;

import java.io.IOException;

public interface ICompressor {
    void addValue(double v);

    byte[] getBytes();

    void close() throws IOException;

    double getCompressionRatio();

    long getCompressedSizeInBits();

    default String getKey() {
        return getClass().getSimpleName();
    }

    void refresh();

    default void setDistribution(int[] distribution1, int[] distribution2) {
    }
}
