package org.urbcomp.startdb.selfstar.decompressor32.xor;

import org.urbcomp.startdb.selfstar.utils.Elf32Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;
import org.urbcomp.startdb.selfstar.utils.PostOfficeSolver32;

import java.io.IOException;
import java.util.Arrays;

public class ElfStarXORDecompressor32 implements IXORDecompressor32 {
    private int storedVal = 0;
    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = Integer.MAX_VALUE;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;

    private int[] leadingRepresentation;

    private int[] trailingRepresentation;

    private int leadingBitsPerValue;

    private int trailingBitsPerValue;

    public ElfStarXORDecompressor32() {
    }

    private void initLeadingRepresentation() {
        try {
            int num = in.readInt(4);
            if (num == 0) {
                num = 16;
            }
            leadingBitsPerValue = PostOfficeSolver32.positionLength2Bits[num];
            leadingRepresentation = new int[num];
            for (int i = 0; i < num; i++) {
                leadingRepresentation[i] = in.readInt(5);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void initTrailingRepresentation() {
        try {
            int num = in.readInt(4);
            if (num == 0) {
                num = 16;
            }
            trailingBitsPerValue = PostOfficeSolver32.positionLength2Bits[num];
            trailingRepresentation = new int[num];
            for (int i = 0; i < num; i++) {
                trailingRepresentation[i] = in.readInt(5);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
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
        storedTrailingZeros = Integer.MAX_VALUE;
        first = true;
        endOfStream = false;

        Arrays.fill(trailingRepresentation, 0);
        trailingBitsPerValue = 0;
        Arrays.fill(leadingRepresentation, 0);
        leadingBitsPerValue = 0;
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    @Override
    public Float readValue() {
        try {
            next();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (endOfStream) {
            return null;
        }
        return Float.intBitsToFloat(storedVal);
    }

    private void next() throws IOException {
        if (first) {
            initLeadingRepresentation();
            initTrailingRepresentation();
            first = false;
            int trailingZeros = in.readInt(6);
            if (trailingZeros < 32) {
                storedVal = ((in.readInt(31 - trailingZeros) << 1) + 1) << trailingZeros;
            } else {
                storedVal = 0;
            }
            if (storedVal == Elf32Utils.END_SIGN) {
                endOfStream = true;
            }
        } else {
            nextValue();
        }
    }

    private void nextValue() throws IOException {
        int value;
        int centerBits;

        if (in.readInt(1) == 1) {
            // case 1
            centerBits = 32 - storedLeadingZeros - storedTrailingZeros;
            value = in.readInt(centerBits) << storedTrailingZeros;
            value = storedVal ^ value;
            if (value == Elf32Utils.END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        } else if (in.readInt(1) == 0) {
            // case 00
            int leadAndTrail = in.readInt(leadingBitsPerValue + trailingBitsPerValue);
            int lead = leadAndTrail >>> trailingBitsPerValue;
            int trail = ~(0xffff << trailingBitsPerValue) & leadAndTrail;
            storedLeadingZeros = leadingRepresentation[lead];
            storedTrailingZeros = trailingRepresentation[trail];
            centerBits = 32 - storedLeadingZeros - storedTrailingZeros;

            value = in.readInt(centerBits) << storedTrailingZeros;
            value = storedVal ^ value;
            if (value == Elf32Utils.END_SIGN) {
                endOfStream = true;
            } else {
                storedVal = value;
            }
        }
    }
}
