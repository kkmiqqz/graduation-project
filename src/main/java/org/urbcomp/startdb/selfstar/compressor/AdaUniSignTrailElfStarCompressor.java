package org.urbcomp.startdb.selfstar.compressor;

import org.urbcomp.startdb.selfstar.compressor.xor.IXORCompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.OutputBitStream;

import java.util.*;

import static org.urbcomp.startdb.selfstar.compressor.BatchUtils.AdaRLE;
//import static org.urbcomp.startdb.selfstar.utils.Elf64Utils.getAlphaAndBetaStar;

/**
 * 此版本建立在UniSignElfStarCompressor的基础上，区别是动态调整统一精度
 */
public class AdaUniSignTrailElfStarCompressor implements ICompressor {
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
    private int[] signList;
    private int[] alphaList;
    private int[] noEraseList; // 0表示擦除，1表示不擦除

    private int eraseBits;

    public AdaUniSignTrailElfStarCompressor(IXORCompressor xorCompressor, int window) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[window + 1];     // one for the end sign
        this.vPrimeList = new long[window + 1];      // one for the end sign
    }

    public AdaUniSignTrailElfStarCompressor(IXORCompressor xorCompressor) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[1001];     // one for the end sign
        this.vPrimeList = new long[1001];      // one for the end sign
        this.signList = new int[1000];
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

    public void AdaUnify(List<Double> dbs){
        TreeMap<Integer,Integer> alphaMap = new TreeMap<>(Comparator.reverseOrder());
        alphaList = new int[dbs.size()];
        noEraseList = new int[dbs.size()];
        int idx=0;
        for (double db:dbs) {
            if (!(db == 0.0 || Double.isInfinite(db) || Double.isNaN(db))) {
                // C1: v is a normal or subnormal
                int alpha = getAlphaLowSpeed(db);
                alphaList[idx] = alpha;
                // 统计各精度出现的次数
                alphaMap.put(alpha, alphaMap.getOrDefault(alpha, 0) + 1);
            }
            idx++;
        }
        int voter = 0;
        for (Map.Entry<Integer, Integer> entry : alphaMap.entrySet()) {
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            voter += entry.getValue();
            if (voter > dbs.size()*0.1){
                uniAlpha = entry.getKey();
                if (uniAlpha>=16){
                    for(int i=0;i<alphaList.length;i++){
                        noEraseList[i]=1;
                    }
                }else {
                    for (int i = 0; i < alphaList.length; i++) {
                        if (alphaList[i] > uniAlpha) {
                            noEraseList[i] = 1;
                        }
                    }
                }
                return;
            }
        }

        uniAlpha = alphaMap.firstEntry().getKey(); // 设为最大精度
        if (uniAlpha>=16){
            for(int i=0;i<alphaList.length;i++){
                noEraseList[i]=1;
            }
        }
    }


    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);

        signList[numberOfValues] = v>=0 ? 0 : 1;

        if (v == 0.0 ||Double.isInfinite(v)) {
            vPrimeList[numberOfValues] = vLong;
//            betaStarList[numberOfValues] = Integer.MAX_VALUE;
        } else if (noEraseList[numberOfValues]==1) {
            vPrimeList[numberOfValues] = 0x7fffffffffffffffL & vLong;
        } else if (Double.isNaN(v)) {
            vPrimeList[numberOfValues] = 0xfff8000000000000L & vLong;
//            betaStarList[numberOfValues] = Integer.MAX_VALUE;
        }
//        else if (uniAlpha>=16 && alphaList[numberOfValues]<uniAlpha){
//            noEraseList[numberOfValues]=1;
//            vPrimeList[numberOfValues] = 0x7fffffffffffffffL & vLong;
//        }
        else {
            // C1: v is a normal or subnormal
//            int[] alphaAndBetaStar = Elf64Utils.getAlphaAndBetaStar(v, lastBetaStar);

//            if (v == 0.1932806702479387)
//                System.out.println();
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
//            double after = Double.longBitsToDouble(mask & vLong & 0x7fffffffffffffffL);
            vPrimeList[numberOfValues] = mask & vLong & 0x7fffffffffffffffL;
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

        // signList
        // 禁用二值序列优化
        RLEresult signResult = AdaRLE(signList,1);

        // 启用二值序列优化
//        RLEresult signResult = AdaRLE(signList);

        // 序列化 sign
        compressedSizeInBits += os.writeInt(signResult.length,32); // TODO: 可隐含
        compressedSizeInBits += os.writeInt(signResult.cntWidth,32);

        // 禁用二值序列优化
        for (int i=0;i<signResult.length;i++){
            compressedSizeInBits += os.writeInt(signResult.nums[i],1);
            compressedSizeInBits += os.writeInt(signResult.cnts[i],signResult.cntWidth);
        }

        // 启用二值序列优化
//        if (signResult.nonSplit){
//            compressedSizeInBits += os.writeInt(1,1);
//            compressedSizeInBits += os.writeInt(signResult.nums[0],1);
//            for (int i=0;i<signResult.length;i++){
//                compressedSizeInBits += os.writeInt(signResult.cnts[i],signResult.cntWidth);
//            }
//        }else{
//            compressedSizeInBits += os.writeInt(0,1);
//            for (int i=0;i<signResult.length;i++){
//                compressedSizeInBits += os.writeInt(signResult.nums[i],1);
//                compressedSizeInBits += os.writeInt(signResult.cnts[i],signResult.cntWidth);
//            }
//        }

        // noEraseList
        // 禁用二值序列优化
        RLEresult eraseResult = AdaRLE(noEraseList,1);

        // 启用二值序列优化
//        RLEresult signResult = AdaRLE(signList);

        // 序列化 sign
        compressedSizeInBits += os.writeInt(eraseResult.length,32); // TODO: 可隐含
        compressedSizeInBits += os.writeInt(eraseResult.cntWidth,32);

        // 禁用二值序列优化
        for (int i=0;i<eraseResult.length;i++){
            compressedSizeInBits += os.writeInt(eraseResult.nums[i],1);
            compressedSizeInBits += os.writeInt(eraseResult.cnts[i],eraseResult.cntWidth);
        }

        // 启用二值序列优化
//        if (signResult.nonSplit){
//            compressedSizeInBits += os.writeInt(1,1);
//            compressedSizeInBits += os.writeInt(signResult.nums[0],1);
//            for (int i=0;i<signResult.length;i++){
//                compressedSizeInBits += os.writeInt(signResult.cnts[i],signResult.cntWidth);
//            }
//        }else{
//            compressedSizeInBits += os.writeInt(0,1);
//            for (int i=0;i<signResult.length;i++){
//                compressedSizeInBits += os.writeInt(signResult.nums[i],1);
//                compressedSizeInBits += os.writeInt(signResult.cnts[i],signResult.cntWidth);
//            }
//        }

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
