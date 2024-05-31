package org.urbcomp.startdb.selfstar.compressor;

import org.urbcomp.startdb.selfstar.compressor.xor.IXORCompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.OutputBitStream;

import java.io.IOException;
import java.util.*;

import static org.urbcomp.startdb.selfstar.compressor.BatchUtils.AdaRLE;
import static org.urbcomp.startdb.selfstar.compressor.BatchUtils.getWidthNeeded;
import static org.urbcomp.startdb.selfstar.utils.Elf64Utils.getAlpha;
//import static org.urbcomp.startdb.selfstar.utils.Elf64Utils.getAlphaAndBetaStar;

/**
 * 优化符号编码、擦除flag编码
 * 添加了溢出检测
 * 修复了判断是否需要偏移时的逻辑bug
 * 优化了计算偏移值时使用的最大值、最小值以及精度逻辑
 * 局部细节代码优化
 * TODO:batch_size=30时居然会错误 问题出现在Roundup过程中乘法操作引入的误差上
 */
public class ProFORAdaUniSignElfStarCompressor implements ICompressor {
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
    private final int[] signList;
    private final int[] alphaList;
    private final int[] noEraseList; // 0表示擦除，1表示不擦除
    private double minValue = Double.MAX_VALUE;
    private double maxValue = Double.MIN_VALUE;
    private double offset = 0;

    public ProFORAdaUniSignElfStarCompressor(IXORCompressor xorCompressor, int window) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[window + 1];     // one for the end sign
        this.vPrimeList = new long[window + 1];      // one for the end sign
        this.signList = new int[window];
        alphaList = new int[window];
        noEraseList = new int[window];
    }

    public ProFORAdaUniSignElfStarCompressor(IXORCompressor xorCompressor) {
        this.xorCompressor = xorCompressor;
        this.os = xorCompressor.getOutputStream();
//        this.betaStarList = new int[1001];     // one for the end sign
        this.vPrimeList = new long[1001];      // one for the end sign
        this.signList = new int[1000];
        alphaList = new int[1000];
        noEraseList = new int[1000];
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

    /**
     * 检查在当前数据上进行偏移是否会有导致溢出的风险
     * @param offset 偏移值
     * @return 是否有风险
     */
    private boolean checkOverflow(double offset){
        long modifiedMax = (long) (maxValue * Math.pow(10,uniAlpha));
        long modifiedOffset = (long) (offset * Math.pow(10,uniAlpha));
        long sum = modifiedOffset + modifiedMax;
        if (getWidthNeeded(sum)>53)
            return true;
        else
            return false;
    }

    /**
     * 在此处对数据进行偏移处理，异常值直接不偏移
     * @param dbs
     */
    public void FOR(List<Double> dbs){
        double delta = maxValue - minValue;
        double off = Math.pow(2, Math.ceil(Math.log(delta) / Math.log(2))) - Math.floor(minValue);
        if (Math.ceil(Math.log(maxValue) / Math.log(2)) == Math.ceil(Math.log(minValue) / Math.log(2))){
            offset =0;
        } else if (checkOverflow(off)) { //  uniAlpha>=15
            offset = 0;
        } else {
            offset = Math.pow(2, Math.ceil(Math.log(delta) / Math.log(2))) - Math.floor(minValue);
            for (int i=0;i<noEraseList.length;i++){
                if (noEraseList[i]==0) { // 正常值
                    dbs.set(i, dbs.get(i)+offset );
                }
            }
        }

    }

    /**
     * 在此处对数据进行偏移处理，异常值直接不偏移
     * @param dbs
     */
    public void FOR(double[] dbs){
        if (Math.ceil(Math.log(maxValue) / Math.log(2)) == Math.ceil(Math.log(minValue) / Math.log(2))){
            offset =0;
        } else{
            double delta = maxValue - minValue;
            offset = Math.pow(2, Math.ceil(Math.log(Math.max(1,delta)) / Math.log(2))) - Math.floor(minValue);
            if (checkOverflow(offset)) { //  uniAlpha>=15
                offset = 0;
            } else {
                for (int i=0;i<noEraseList.length;i++){
                    if (noEraseList[i]==0) { // 正常值
                        dbs[i] = dbs[i]+offset;
                    }
                }
            }
        }
    }


    public void AdaUnify(List<Double> dbs){
        TreeMap<Integer,Integer> alphaMap = new TreeMap<>(Comparator.reverseOrder());
        int idx=0;
        double db;
        while (idx< dbs.size()) {
            db = dbs.get(idx);
            signList[idx] = Double.doubleToRawLongBits(db)>>63==0 ? 0 : 1;
            db = Math.abs(db);
            dbs.set(idx, db);

            if (!(db == 0.0 || Double.isInfinite(db) || Double.isNaN(db))) {
                int alpha = getAlphaLowSpeed(db);
                alphaList[idx] = alpha;
                // 统计各精度出现的次数
                alphaMap.put(alpha, alphaMap.getOrDefault(alpha, 0) + 1);
            }
            idx++;
        }

        int voter = 0;
        for (Map.Entry<Integer, Integer> entry : alphaMap.entrySet()) {
            voter += entry.getValue();
            if (voter > dbs.size()*0.05){
                uniAlpha = entry.getKey();
                if (uniAlpha>=16){
//                    for(int i=0;i<alphaList.length;i++){
//                        noEraseList[i]=1;
//                    }
                    Arrays.fill(noEraseList,1);
                    maxValue=minValue=0; // 全部无法擦除时，不进行偏移
                }else {
                    for (int i = 0; i < alphaList.length; i++) {
                        if (alphaList[i] > uniAlpha) {
                            noEraseList[i] = 1;
                        }else{
                            // 更新最大最小值 [因为只对可擦除的值进行偏移，所以最大最小值也只在该范围内探查]
                            minValue = Math.min(minValue,dbs.get(i));
                            maxValue = Math.max(maxValue,dbs.get(i));
                        }
                    }
                }
                return;
            }
        }
    }

    public void AdaUnify(double[] dbs){
        TreeMap<Integer,Integer> alphaMap = new TreeMap<>(Comparator.reverseOrder());
        int idx=0;
        double db;
        while (idx< dbs.length) {
            db = dbs[idx];
            signList[idx] = Double.doubleToRawLongBits(db)>>63==0 ? 0 : 1;
            db = Math.abs(db);
            dbs[idx]=db;

            if (!(db == 0.0 || Double.isInfinite(db) || Double.isNaN(db))) {
//                int alpha = getAlphaLowSpeed(db);
                int alpha = getAlpha(db);
                alphaList[idx] = alpha;
                // 统计各精度出现的次数
                alphaMap.put(alpha, alphaMap.getOrDefault(alpha, 0) + 1);
            }
            idx++;
        }

        int voter = 0;
        for (Map.Entry<Integer, Integer> entry : alphaMap.entrySet()) {
            voter += entry.getValue();
            if (voter > dbs.length*0.05){
                uniAlpha = entry.getKey();
                if (uniAlpha>=16){
//                    for(int i=0;i<alphaList.length;i++){
//                        noEraseList[i]=1;
//                    }
                    Arrays.fill(noEraseList,1);
                    maxValue=minValue=0; // 全部无法擦除时，不进行偏移
                }else {
                    for (int i = 0; i < alphaList.length; i++) {
                        if (alphaList[i] > uniAlpha) {
                            noEraseList[i] = 1;
                        }else{
                            // 更新最大最小值 [因为只对可擦除的值进行偏移，所以最大最小值也只在该范围内探查]
                            minValue = Math.min(minValue,dbs[i]);
                            maxValue = Math.max(maxValue,dbs[i]);
                        }
                    }
                }
                return;
            }
        }
    }


    public void addValue(double v) {
        long vLong = Double.doubleToRawLongBits(v);

//        符号位提前至AdaUnify处
//        signList[numberOfValues] = v>=0 ? 0 : 1;

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

    private void compress() throws IOException {
        xorCompressor.setDistribution(leadDistribution, trailDistribution);
//        lastBetaStar = Integer.MAX_VALUE;

        compressedSizeInBits += os.writeInt(numberOfValues,16);
        compressedSizeInBits += os.writeInt(uniAlpha,16);

        // signList
        // 禁用二值序列优化
        RLEresult signResult = AdaRLE(signList,1);

////         启用二值序列优化
//        RLEresult signResult = AdaRLE(signList);

        // 序列化 sign
        if (signResult.length==1){
            compressedSizeInBits += os.writeBit(1);
            compressedSizeInBits += os.writeBit(signResult.nums[0]);
        }else {
            compressedSizeInBits += os.writeBit(0);
//        compressedSizeInBits += os.writeInt(signResult.length,16); // 可隐含
            compressedSizeInBits += os.writeInt(signResult.cntWidth, 16);

            // 禁用二值序列优化
            for (int i = 0; i < signResult.length; i++) {
                compressedSizeInBits += os.writeInt(signResult.nums[i], 1);
                compressedSizeInBits += os.writeInt(signResult.cnts[i], signResult.cntWidth);
            }

//            // 启用二值序列优化
//            if (signResult.nonSplit) {
//                compressedSizeInBits += os.writeInt(1, 1);
//                compressedSizeInBits += os.writeInt(signResult.nums[0], 1);
//                for (int i = 0; i < signResult.length; i++) {
//                    compressedSizeInBits += os.writeInt(signResult.cnts[i], signResult.cntWidth);
//                }
//            }else {
//                compressedSizeInBits += os.writeInt(0, 1);
//                for (int i = 0; i < signResult.length; i++) {
//                    compressedSizeInBits += os.writeInt(signResult.nums[i], 1);
//                    compressedSizeInBits += os.writeInt(signResult.cnts[i], signResult.cntWidth);
//                }
//            }
        }
        // noEraseList
        // 禁用二值序列优化
        RLEresult eraseResult = AdaRLE(noEraseList,1);

//        // 启用二值序列优化
//        RLEresult eraseResult = AdaRLE(noEraseList);

        // 序列化 noEraseFlag
        if (eraseResult.length==1){
            compressedSizeInBits += os.writeBit(1);
            compressedSizeInBits += os.writeBit(eraseResult.nums[0]);
        }else {
            compressedSizeInBits += os.writeBit(0);
//          compressedSizeInBits += os.writeInt(eraseResult.length,16); // 可隐含
            compressedSizeInBits += os.writeInt(eraseResult.cntWidth, 16);

            // 禁用二值序列优化
            for (int i = 0; i < eraseResult.length; i++) {
                compressedSizeInBits += os.writeInt(eraseResult.nums[i], 1);
                compressedSizeInBits += os.writeInt(eraseResult.cnts[i], eraseResult.cntWidth);
            }

//            //  启用二值序列优化
//          if (eraseResult.nonSplit){
//              compressedSizeInBits += os.writeInt(1,1);
//              compressedSizeInBits += os.writeInt(eraseResult.nums[0],1);
//              for (int i=0;i<eraseResult.length;i++){
//                  compressedSizeInBits += os.writeInt(eraseResult.cnts[i],eraseResult.cntWidth);
//              }
//          }else{
//              compressedSizeInBits += os.writeInt(0,1);
//              for (int i=0;i<eraseResult.length;i++){
//                  compressedSizeInBits += os.writeInt(eraseResult.nums[i],1);
//                  compressedSizeInBits += os.writeInt(eraseResult.cnts[i],eraseResult.cntWidth);
//              }
//          }
        }

        // 序列化offset
        if (offset==0){
            compressedSizeInBits += os.writeBit(false); // 用于标记是否有偏移的flag位
        }else {
            compressedSizeInBits += os.writeBit(true); // 用于标记是否有偏移的flag位
            compressedSizeInBits += os.writeLong(Double.doubleToRawLongBits(offset), 64);
        }

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
//            改XOR版本
//            compressedSizeInBits += xorCompressor.addValue(vPrimeList[i], noEraseList[i]);
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

    public void close() throws IOException {
        calculateDistribution();
        compress();
        // we write one more bit here, for marking an end of the stream.
//        compressedSizeInBits += os.writeInt(2, 2);  // case 10
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
