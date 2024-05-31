package org.urbcomp.startdb.selfstar.compressor;

import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.OutputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.urbcomp.startdb.selfstar.compressor.BatchUtils.AdaRLE;
import static org.urbcomp.startdb.selfstar.compressor.BatchUtils.getWidthNeeded;

class Exceptions {
    public List<Integer> pos;
    public List<Double> value;

    public Exceptions(){
        pos = new ArrayList<>();
        value = new ArrayList<>();
    }

    public void addValue(int pos, double value){
        this.pos.add(pos);
        this.value.add(value);
    }

    public int getSize(){
        return pos.size();
    }
}



public class BatchElfCompressor {
    private static int batchSize;
    private int compressedSizeInBits = 0;
    private final OutputBitStream out;
    private int numberOfValues = 0;
    int uniAlpha = 0;

    public BatchElfCompressor(int batchSize) {
        BatchElfCompressor.batchSize = batchSize;
        out = new OutputBitStream(new byte[100000]);
    }

    public BatchElfCompressor() {
        BatchElfCompressor.batchSize = 1000;
        out = new OutputBitStream(new byte[100000]);
    }

    public byte[] getBytes() {
        return this.out.getBuffer();
    }

    public void close() throws IOException {
        out.close();
    }

    public double getCompressionRatio() {
        return compressedSizeInBits / (numberOfValues * 64.0);
    }

    public long getCompressedSizeInBits() {
        return compressedSizeInBits;
    }

    public void refresh() {
        compressedSizeInBits = 0;
        numberOfValues = 0;
        out.flush();
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

    private void getUniAlpha(double[] dbs){
        for (double db:dbs){
            uniAlpha = Math.max(uniAlpha,getAlphaLowSpeed(db));
        }
    }

    public void entry(double[] dbs) throws IOException {

        getUniAlpha(dbs);

        numberOfValues = dbs.length;
        double db;
        long dbLong;
        int[] sign = new int[numberOfValues];
        long[] expAndMantissa = new long[numberOfValues];
        long tmp;
        int FAlpha = Elf64Utils.getFAlpha(uniAlpha);
//        int[] betaStar = new int[numberOfValues];
        int[] continueMap = new int[numberOfValues];
        Exceptions exception = new Exceptions();
        for (int i=0;i< dbs.length;i++) {
            db = dbs[i];
            if (db == 0){
                exception.addValue(i,db);
                if (i!=0)
                    expAndMantissa[i] = expAndMantissa[i-1];    // 记为前值而不是0，有利于FOR和RLE的压缩
                continue;
            }

            dbLong = Double.doubleToRawLongBits(db);

            // get sign
            sign[i] = db >= 0 ? 0 : 1;

            // get exp
            int exp = (int) (dbLong >> 52 & 0x7ff);

            // get exp and mantissa
            tmp = dbLong & 0x7fffffffffffffffL;

            // get α and β*
//            int[] alphaAndBetaStar = Elf64Utils.getAlphaAndBetaStar(db);
//            alphaAndBetaStar[0] = 6; // 6
//            alphaAndBetaStar[1] = 8; // 8
//            betaStar[i] = alphaAndBetaStar[1];



            // get g(α), which means the bits needed to maintain α
            int gAlpha = FAlpha + exp - 1023;

            int eraseBits = Math.max(0,52 - gAlpha);
            long mask = 0xffffffffffffffffL << eraseBits;
            expAndMantissa[i] = mask & tmp;
        }

        // 序列化 基础信息
        compressedSizeInBits += out.writeInt(numberOfValues,32);
        compressedSizeInBits += out.writeLong(expAndMantissa[0],64);

        // sign 单独存储压缩（RLE）
        RLEresult signResult = AdaRLE(sign);
        // 序列化 sign
        compressedSizeInBits += out.writeInt(signResult.length,32); // TODO: 可隐含
        compressedSizeInBits += out.writeInt(signResult.cntWidth,32);
        for (int i=0;i<signResult.length;i++){
            compressedSizeInBits += out.writeBit(signResult.nums[i]);
            compressedSizeInBits += out.writeInt(signResult.cnts[i],signResult.cntWidth);
        }

        // uniAlpha
        compressedSizeInBits += out.writeInt(uniAlpha,32);

//        // β* 单独存储压缩
//        // 暂时未实现统一精度的方法
//        // β* FOR
//        // TODO: 自识别异常偏离值
//        int minBetaStar = betaStar[0];
//        int maxBetaStar = betaStar[0];
//        for (int num:betaStar){
//            minBetaStar = Math.min(num, minBetaStar);
//            maxBetaStar = Math.max(num, maxBetaStar);
//        }
//        for (int i = 0; i < betaStar.length; i++) {
//            betaStar[i] -= minBetaStar;
//        }
//        int betaStarWidth = getWidthNeeded(maxBetaStar-minBetaStar);
//
//        // β* 级联 RLE压缩
//        RLEresult betaStarResult = RLE(betaStar,10);
//        // 序列化 β*
//        compressedSizeInBits += out.writeInt(betaStarResult.length, 32);
//        compressedSizeInBits += out.writeInt(betaStarWidth, 32);
//        compressedSizeInBits += out.writeInt(betaStarResult.cntWidth, 32);
//        compressedSizeInBits += out.writeInt(minBetaStar,32);
//        for (int i=0;i<betaStarResult.length;i++){
//            compressedSizeInBits += out.writeInt(betaStarResult.nums[i],betaStarWidth);
//            compressedSizeInBits += out.writeInt(betaStarResult.cnts[i],betaStarResult.cntWidth);
//        }

        // expAndMantissa 异或
        long[] xorResult = new long[numberOfValues - 1];
        int[] leadingZero = new int[numberOfValues - 1];
        int[] trailingZero = new int[numberOfValues - 1];
//        int centerBitWidth = 0;
        long storedValue = expAndMantissa[0];
        for (int i =0 ;i<expAndMantissa.length - 1;i++){
            xorResult[i] = expAndMantissa[i+1] ^ storedValue;
            if(i!=0 && xorResult[i]==0){
                leadingZero[i] = leadingZero[i-1];
                trailingZero[i] = trailingZero[i-1];
                continueMap[i+1] = 1;
                continue;
            }
            leadingZero[i] = Long.numberOfLeadingZeros(xorResult[i]);
            trailingZero[i] = Long.numberOfTrailingZeros(xorResult[i]);
            xorResult[i] = xorResult[i] >>>trailingZero[i];
//            centerBitWidth = Math.max(centerBitWidth,64 - leadingZero[i] - trailingZero[i]);
            storedValue = expAndMantissa[i+1];
        }

        // continueMap
        for (int i=0;i<numberOfValues;i++){
            out.writeBit(continueMap[i]);
        }

        // leadingZero
        // leadingZero FOR
        // TODO: 自识别异常偏离值
        int minLeadingZero = leadingZero[0];
        int maxLeadingZero = leadingZero[0];
        for (int num:leadingZero){
            minLeadingZero = Math.min(num, minLeadingZero);
            maxLeadingZero = Math.max(num, maxLeadingZero);
        }
//        int checkpoint0 = 1;
        for (int i = 0; i < leadingZero.length; i++) {
            leadingZero[i] -= minLeadingZero;
        }
        int leadingZeroWidth = getWidthNeeded(maxLeadingZero-minLeadingZero);

        // leadingZero 级联 RLE压缩
        RLEresult leadingZeroResult = AdaRLE(leadingZero,leadingZeroWidth);
        // 序列化 leadingZero
        compressedSizeInBits += out.writeInt(leadingZeroResult.length, 32);
        compressedSizeInBits += out.writeInt(leadingZeroWidth, 32);
        compressedSizeInBits += out.writeInt(leadingZeroResult.cntWidth, 32);
        compressedSizeInBits += out.writeInt(minLeadingZero,32);
        for (int i=0;i<leadingZeroResult.length;i++){
            compressedSizeInBits += out.writeInt(leadingZeroResult.nums[i],leadingZeroWidth);
            compressedSizeInBits += out.writeInt(leadingZeroResult.cnts[i],leadingZeroResult.cntWidth);
        }

        // trailingZero
        // trailingZero FOR
        // TODO: 自识别异常偏离值
        int minTrailingZero = trailingZero[0];
        int maxTrailingZero = trailingZero[0];
        for (int num:trailingZero){
            minTrailingZero = Math.min(num, minTrailingZero);
            maxTrailingZero = Math.max(num, maxTrailingZero);
        }
//        int checkpoint1 = 1;
        for (int i = 0; i <trailingZero.length; i++) {
            trailingZero[i] -= minTrailingZero;
        }
        int trailingZeroWidth = getWidthNeeded(maxTrailingZero-minTrailingZero);

        // trailingZero 级联 RLE压缩
        RLEresult trailingZeroResult = AdaRLE(trailingZero,trailingZeroWidth);
        // 序列化 trailingZero
        compressedSizeInBits += out.writeInt(trailingZeroResult.length, 32);
        compressedSizeInBits += out.writeInt(trailingZeroWidth, 32);
        compressedSizeInBits += out.writeInt(trailingZeroResult.cntWidth, 32);
        compressedSizeInBits += out.writeInt(minTrailingZero,32);
        for (int i=0;i<trailingZeroResult.length;i++){
            compressedSizeInBits += out.writeInt(trailingZeroResult.nums[i],trailingZeroWidth);
            compressedSizeInBits += out.writeInt(trailingZeroResult.cnts[i],trailingZeroResult.cntWidth);
        }

//        System.out.print("");
        // 序列化 centerBits
//        out.writeInt(centerBitWidth, 32);
        for (int i=0;i<numberOfValues - 1;i++){
//            out.writeLong(xorResult[i],centerBitWidth);
            int width = getWidthNeeded(xorResult[i]);
            compressedSizeInBits += out.writeLong(xorResult[i],getWidthNeeded(xorResult[i]));
//            int checkpoint2 = 1;
        }

        compressedSizeInBits += out.writeInt(exception.getSize(), 32);
        for (int i=0;i<exception.getSize();i++){
            compressedSizeInBits += out.writeInt(exception.pos.get(i), 32);
            compressedSizeInBits += out.writeLong(Double.doubleToRawLongBits(exception.value.get(i)), 64);
        }

        out.flush();
    }


    public static void main(String[] args) throws IOException {
        double[] dbs = {
                88.51872,
                88.51789,
                88.51872,
                88.51956,
                88.51739,
                88.51972,
                88.51956,
                88.51739,
                88.51706,
                88.51856,
                88.51989
        };
        BatchElfCompressor batchElf = new BatchElfCompressor(dbs.length);
        batchElf.entry(dbs);
    }
}
