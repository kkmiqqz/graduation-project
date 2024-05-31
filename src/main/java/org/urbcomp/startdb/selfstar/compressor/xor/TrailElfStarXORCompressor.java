package org.urbcomp.startdb.selfstar.compressor.xor;

import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.OutputBitStream;
import org.urbcomp.startdb.selfstar.utils.PostOfficeSolver;

import java.util.Arrays;

public class TrailElfStarXORCompressor implements IXORCompressor {
    private final int[] leadingRepresentation = new int[64];
    private final int[] leadingRound = new int[64];
    private final int[] trailingRepresentation = new int[64];
    private final int[] trailingRound = new int[64];
    private int storedLeadingZeros = Integer.MAX_VALUE;
//    private int storedTrailingZeros = Integer.MAX_VALUE;
    private long storedVal = 0;
    private boolean first = true;
    private int[] leadDistribution;
    private int[] trailDistribution;

    private OutputBitStream out;

    private int leadingBitsPerValue;

    private int trailingBitsPerValue;

    private final int capacity;

//    private List<Integer> trailingZeroList = new ArrayList<>();

    public TrailElfStarXORCompressor() {
        this(1000);
    }

    public TrailElfStarXORCompressor(int window) {
        this.capacity = window;
        out = new OutputBitStream(
                new byte[(int) (((capacity + 1) * 8 + capacity / 8 + 1) * 1.2)]);
    }

    @Override
    public OutputBitStream getOutputStream() {
        return this.out;
    }

    private int initLeadingRoundAndRepresentation(int[] distribution) {
        int[] positions = PostOfficeSolver.initRoundAndRepresentation(distribution, leadingRepresentation, leadingRound);
        leadingBitsPerValue = PostOfficeSolver.positionLength2Bits[positions.length];
        return PostOfficeSolver.writePositions(positions, out);
    }

    private int initTrailingRoundAndRepresentation(int[] distribution) {
        int[] positions = PostOfficeSolver.initRoundAndRepresentation(distribution, trailingRepresentation, trailingRound);
        trailingBitsPerValue = PostOfficeSolver.positionLength2Bits[positions.length];
        return PostOfficeSolver.writePositions(positions, out);
    }

    /**
     * Adds a new long value to the series. Note, values must be inserted in order.
     *
     * @param value next floating point value in the series
     */
    @Override
    public int addValue(long value) {
        if (first) {
            return writeFirst(value);
        } else {
            return compressValue(value);
        }
    }

    public int initLeadingAndTrailing(){
        return initLeadingRoundAndRepresentation(leadDistribution)
                + initTrailingRoundAndRepresentation(trailDistribution);
    }

    public void encodeTrailingZero(int[] trailingZeros){
        for (int i=0;i<trailingZeros.length;i++){
            trailingZeros[i] = trailingRepresentation[trailingRound[trailingZeros[i]]];
        }
    }

    private int writeFirst(long value) {
        first = false;
        storedVal = value;
        int trailingZeros = Long.numberOfTrailingZeros(value);
        out.writeInt(trailingZeros, 7);
        if (trailingZeros < 64) {
            out.writeLong(storedVal >>> (trailingZeros + 1), 63 - trailingZeros);
            return 70 - trailingZeros;
        } else {
            return 7;
        }
    }

    /**
     * Closes the block and writes the remaining stuff to the BitOutput.
     */
    @Override
    public int close() {
        int thisSize = addValue(Elf64Utils.END_SIGN);
        out.flush();
        return thisSize;
    }

    private int compressValue(long value) {
        int thisSize = 0;
        long xor = storedVal ^ value;

        if (xor == 0) {
            // case 01
            out.writeInt(1, 2);
            thisSize += 2;
        } else {
            int leadingZeros = leadingRound[Long.numberOfLeadingZeros(xor)];
            int trailingZeros = trailingRound[Long.numberOfTrailingZeros(xor)];
//            trailingZeroList.add(trailingZeros);

            if (leadingZeros >= storedLeadingZeros &&
                    (leadingZeros - storedLeadingZeros) < 1 + leadingBitsPerValue) {
                // case 1
                int centerBits = 64 - storedLeadingZeros - trailingZeros;
                int len = 1 + centerBits;
                if (len > 64) {
                    out.writeInt(1, 1);
                    out.writeLong(xor >>> trailingZeros, centerBits);
                } else {
                    out.writeLong((1L << centerBits) | (xor >>> trailingZeros), 1 + centerBits);
                }
                thisSize += len;
            } else {
                storedLeadingZeros = leadingZeros;
//                storedTrailingZeros = trailingZeros;
                int centerBits = 64 - storedLeadingZeros - trailingZeros;

                // case 00
                int len = 2 + leadingBitsPerValue + centerBits;
                if (len > 64) {
                    out.writeInt(leadingRepresentation[storedLeadingZeros], 2 + leadingBitsPerValue);
                    out.writeLong(xor >>> trailingZeros, centerBits);
                } else {
                    out.writeLong(
                            ((long) leadingRepresentation[storedLeadingZeros] << centerBits) |
                                    ( xor >>> trailingZeros ),
                            len
                    );
                }
                thisSize += len;
            }
            storedVal = value;
        }
        return thisSize;
    }

    @Override
    public byte[] getOut() {
        return out.getBuffer();
    }

    public int getTrailingBitsPerValue(){
        return trailingBitsPerValue;
    }

    @Override
    public void refresh() {
        out = new OutputBitStream(
                new byte[(int) (((capacity + 1) * 8 + capacity / 8 + 1) * 1.2)]);
        storedLeadingZeros = Integer.MAX_VALUE;
//        storedTrailingZeros = Integer.MAX_VALUE;
        storedVal = 0;
        first = true;
        Arrays.fill(leadingRepresentation, 0);
        Arrays.fill(leadingRound, 0);
        Arrays.fill(trailingRepresentation, 0);
        Arrays.fill(trailingRound, 0);
    }

    @Override
    public void setDistribution(int[] leadDistribution, int[] trailDistribution) {
        this.leadDistribution = leadDistribution;
        this.trailDistribution = trailDistribution;
    }
}
