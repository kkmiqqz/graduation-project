package org.urbcomp.startdb.selfstar.decompressor.xor;

import org.urbcomp.startdb.selfstar.utils.InputBitStream;

public interface IXORDecompressor {
    Double readValue();

    default Double readValue(int noEraseFlag) {
        return null;
    }

    InputBitStream getInputStream();

    void setBytes(byte[] bs);

    void refresh();
}
