package org.urbcomp.startdb.selfstar.compressor;

import org.urbcomp.startdb.selfstar.compressor.xor.IXORCompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.OutputBitStream;

import java.util.Arrays;

/**
 * 此版本建立在ElfStarCompressor的基础上，区别是统一按照最大精度擦除
 */
public class UniElfStarCompressor implements ICompressor {
    private final IXORCompressor xorCompressor;
//    private final int[] betaStarList;
    private final long[] vPrimeList;
    private final int[] leadDistribution = new int[64];
    private final int[] trailDistribution = new int[64];
    private OutputBitStream os;
    private int compressedSizeInBits = 0;
//    private int lastBetaStar = Integer.MAX_VALUE;
    private int numberOfValues = 0;
    private int uniAlpha = 0;

    public UniElfStarCompressor(IXORCompressor xorCompressor, int window) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[window + 1];     // one for the end sign
        this.vPrimeList = new long[window + 1];      // one for the end sign
    }

    public UniElfStarCompressor(IXORCompressor xorCompressor) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[1001];     // one for the end sign
        this.vPrimeList = new long[1001];      // one for the end sign
    }

    private static int getAlphaLowSpeed(double db) {
        if (db == 0.0) {
            return 0;
        }
        String strDb = Double.toString(db);
        int indexOfDecimalPoint = strDb.indexOf('.');
        int cnt = 0;

        if (indexOfDecimalPoint >= 0) {
            for (int i = indexOfDecimalPoint + 1; i < strDb.length(); ++i) {
                if (strDb.charAt(i) != 'E') {
                    cnt++;
                } else {
                    i++;
                    cnt -= Integer.parseInt(strDb.substring(i));
                    return Math.max(cnt, 0);
                }
            }
            return cnt;
        } else {
            return 0; // 没有小数点，小数位数为0
        }
    }

    public void unify(double v){
        if (!(v == 0.0 || Double.isInfinite(v) || Double.isNaN(v))) {
            // C1: v is a normal or subnormal
            int alpha = getAlphaLowSpeed(v);
            uniAlpha = Math.max(uniAlpha,alpha);
        }
    }

    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);

        if (v == 0.0 || Double.isInfinite(v)) {
            vPrimeList[numberOfValues] = vLong;
//            betaStarList[numberOfValues] = Integer.MAX_VALUE;
        } else if (Double.isNaN(v)) {
            vPrimeList[numberOfValues] = 0xfff8000000000000L & vLong;
//            betaStarList[numberOfValues] = Integer.MAX_VALUE;
        } else {
            // C1: v is a normal or subnormal
//            int[] alphaAndBetaStar = Elf64Utils.getAlphaAndBetaStar(v, lastBetaStar);
            int e = ((int) (vLong >> 52)) & 0x7ff;
            int gAlpha = Elf64Utils.getFAlpha(uniAlpha) + e - 1023;
            int eraseBits = Math.max(0,52 - gAlpha);
            long mask = 0xffffffffffffffffL << eraseBits;
//            long delta = (~mask) & vLong;
//            if (delta != 0 && eraseBits > 4) {  // C2
//                if (maxBetaStar != lastBetaStar) {
//                    lastBetaStar = maxBetaStar;
//                }
//                betaStarList[numberOfValues] = lastBetaStar;
            vPrimeList[numberOfValues] = mask & vLong;
//            } else {
//                betaStarList[numberOfValues] = Integer.MAX_VALUE;
//                vPrimeList[numberOfValues] = vLong;
//            }
        }

        numberOfValues++;
    }

    private void calculateDistribution() {
        long lastValue = vPrimeList[0];
        for (int i = 1; i < numberOfValues; i++) {
            long xor = lastValue ^ vPrimeList[i];
            if (xor != 0) {
                trailDistribution[Long.numberOfTrailingZeros(xor)]++;
                leadDistribution[Long.numberOfLeadingZeros(xor)]++;
                lastValue = vPrimeList[i];
            }
        }
    }

    private void compress() {
        xorCompressor.setDistribution(leadDistribution, trailDistribution);
//        lastBetaStar = Integer.MAX_VALUE;

        compressedSizeInBits += os.writeInt(numberOfValues,32);
        compressedSizeInBits += os.writeInt(uniAlpha,32);

        for (int i = 0; i < numberOfValues; i++) {
//            if (betaStarList[i] == Integer.MAX_VALUE) {
//                compressedSizeInBits += os.writeInt(2, 2); // case 10
//            } else if (betaStarList[i] == lastBetaStar) {
//                compressedSizeInBits += os.writeBit(false);    // case 0
//            } else {
//                compressedSizeInBits += os.writeInt(betaStarList[i] | 0x30, 6);  // case 11, 2 + 4 = 6
//                lastBetaStar = betaStarList[i];
//            }
            compressedSizeInBits += xorCompressor.addValue(vPrimeList[i]);
        }
    }

    public double getCompressionRatio() {
        return compressedSizeInBits / (numberOfValues * 64.0);
    }

    @Override
    public long getCompressedSizeInBits() {
        return compressedSizeInBits;
    }

    public byte[] getBytes() {
        int byteCount = (int) Math.ceil(compressedSizeInBits / 8.0);
        return Arrays.copyOf(xorCompressor.getOut(), byteCount);
    }

    public void close() {
        calculateDistribution();
        compress();
        // we write one more bit here, for marking an end of the stream.
        compressedSizeInBits += os.writeInt(2, 2);  // case 10
        compressedSizeInBits += xorCompressor.close();
    }

    public String getKey() {
        return xorCompressor.getKey();
    }

    public void refresh() {
        xorCompressor.refresh();
        compressedSizeInBits = 0;
//        lastBetaStar = Integer.MAX_VALUE;
        numberOfValues = 0;
        os = xorCompressor.getOutputStream();
        Arrays.fill(leadDistribution, 0);
        Arrays.fill(trailDistribution, 0);
    }
}
