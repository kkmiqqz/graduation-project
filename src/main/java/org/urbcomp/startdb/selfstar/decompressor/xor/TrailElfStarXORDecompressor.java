package org.urbcomp.startdb.selfstar.decompressor.xor;

import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;
import org.urbcomp.startdb.selfstar.utils.PostOfficeSolver;

import java.io.IOException;
import java.util.Arrays;

public class TrailElfStarXORDecompressor implements IXORDecompressor {
    private long storedVal = 0;
    private int storedLeadingZeros = Integer.MAX_VALUE;
//    private int storedTrailingZeros = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;

    private int[] leadingRepresentation;

    private int[] trailingRepresentation;

    private int leadingBitsPerValue;

    private int trailingBitsPerValue;

    private int[] trailingZeros;

    private int idx = 0;

    public TrailElfStarXORDecompressor() {
    }

    private void initLeadingRepresentation() {
        try {
            int num = in.readInt(5);
            if (num == 0) {
                num = 32;
            }
            leadingBitsPerValue = PostOfficeSolver.positionLength2Bits[num];
            leadingRepresentation = new int[num];
            for (int i = 0; i < num; i++) {
                leadingRepresentation[i] = in.readInt(6);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void initTrailingRepresentation() {
        try {
            int num = in.readInt(5);
            if (num == 0) {
                num = 32;
            }
            trailingBitsPerValue = PostOfficeSolver.positionLength2Bits[num];
            trailingRepresentation = new int[num];
            for (int i = 0; i < num; i++) {
                trailingRepresentation[i] = in.readInt(6);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void setTrailingZeros(int[] trailingZeros){
        this.trailingZeros = trailingZeros;
    }

    @Override
    public void setBytes(byte[] bs) {
        in = new InputBitStream(bs);
    }

    @Override
    public InputBitStream getInputStream() {
        return in;
    }

    @Override
    public void refresh() {
        storedVal = 0;
        storedLeadingZeros = Integer.MAX_VALUE;
//        storedTrailingZeros = Integer.MAX_VALUE;
        first = true;
        endOfStream = false;

        Arrays.fill(trailingRepresentation, 0);
        trailingBitsPerValue = 0;
        Arrays.fill(leadingRepresentation, 0);
        leadingBitsPerValue = 0;
        idx = 0;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    @Override
    public Double readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Double.longBitsToDouble(storedVal);
    }

    private void next() throws IOException {
        if (first) {
//            initLeadingRepresentation();
//            initTrailingRepresentation();
            first = false;
            int trailingZeros = in.readInt(7);
            if (trailingZeros < 64) {
                storedVal = ((in.readLong(63 - trailingZeros) << 1) + 1) << trailingZeros;
            } else {
                storedVal = 0;
            }
            if (storedVal == Elf64Utils.END_SIGN) {
                endOfStream = true;
            }
            idx++;
        } else {
            nextValue();
            idx++;
        }
    }

    public void initLeadAndTrailRepresentation(){
        initLeadingRepresentation();
        initTrailingRepresentation();
    }

    private void nextValue() throws IOException {
        long value;
        int centerBits;
        int trailingZero = trailingRepresentation[trailingZeros[idx]];

        if (in.readInt(1) == 1) {
            // case 1

            centerBits = 64 - storedLeadingZeros - trailingZero;
            value = in.readLong(centerBits) << trailingZero;
            value = storedVal ^ value;
            if (value == Elf64Utils.END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        } else if (in.readInt(1) == 0) {
            // case 00
            int lead = in.readInt(leadingBitsPerValue);
            storedLeadingZeros = leadingRepresentation[lead];
            centerBits = 64 - storedLeadingZeros - trailingZero;

            value = in.readLong(centerBits) << trailingZero;
            value = storedVal ^ value;
            if (value == Elf64Utils.END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        }
    }
}
