package org.urbcomp.startdb.selfstar.decompressor;

import org.urbcomp.startdb.selfstar.decompressor.xor.IXORDecompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;

import java.io.IOException;

public class FORFastAdaUniSignElfStarDecompressor {
    private final IXORDecompressor xorDecompressor;
    //    private int lastBetaStar = Integer.MAX_VALUE;
    private int uniAlpha;
    private int numberOfValues;
    private int[] signList;
    private int[] noEraseList;
    private InputBitStream in;
    private double offset=0;

    public FORFastAdaUniSignElfStarDecompressor(IXORDecompressor xorDecompressor) {
        this.xorDecompressor = xorDecompressor;
    }

    public Double[] decompress() throws IOException {
        in = xorDecompressor.getInputStream();
        numberOfValues = in.readInt(32);
        uniAlpha = in.readInt(32);

        // signList
        signList = new int[numberOfValues];
        int signLength = in.readInt(32);
        int signCntWidth = in.readInt(32);

        // 禁用二值序列优化
        int signValue,signCnt,signIdx=0;
        for (int i=0;i<signLength;i++){
            signValue = in.readInt(1);
            signCnt = signCntWidth==0 ? 1 : in.readInt(signCntWidth);
            for (int j=0;j<signCnt;j++){
                signList[signIdx++] = signValue;
            }
        }

        // 启用二值序列优化
//        int nonSplit = in.readInt(1);
//        if (nonSplit==0){
//            int signValue,signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
//                signValue = in.readInt(1);
//                signCnt = in.readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//            }
//        }else{
//            int signValue = in.readInt(1);
//            int signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
////                signValue = in.readInt(1);
//                signCnt = in.readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//                signValue = 1-signValue;
//            }
//        }

        // noEraseList
        noEraseList = new int[numberOfValues];
        int noEraseListLength = in.readInt(32);
        int noEraseListCntWidth = in.readInt(32);

        // 禁用二值序列优化
        int noEraseListValue,noEraseListCnt,noEraseListIdx=0;
        for (int i=0;i<noEraseListLength;i++){
            noEraseListValue = in.readInt(1);
            noEraseListCnt = noEraseListCntWidth == 0 ? 1 : in.readInt(noEraseListCntWidth);
            for (int j=0;j<noEraseListCnt;j++){
                noEraseList[noEraseListIdx++] = noEraseListValue;
            }
        }

        // 启用二值序列优化
//        int nonSplit = in.readInt(1);
//        if (nonSplit==0){
//            int signValue,signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
//                signValue = in.readInt(1);
//                signCnt = in.readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//            }
//        }else{
//            int signValue = in.readInt(1);
//            int signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
////                signValue = in.readInt(1);
//                signCnt = in.readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//                signValue = 1-signValue;
//            }
//        }
        if (in.readBit()==1) {
            offset = Double.longBitsToDouble(in.readLong(64));
        }

        Double[] values = new Double[numberOfValues];
        Double value;
        for(int i=0; i<numberOfValues; i++) {

            value = recoverVByBetaStar(i);
//            将正负判断设置提至recoverVByBetaStar()中
//            values[i] = signList[i]==0?value:-value;
            values[i] = value;
        }
        return values;
    }


    public void refresh() {
//        lastBetaStar = Integer.MAX_VALUE;
        xorDecompressor.refresh();
    }


    public void setBytes(byte[] bs) {
        this.xorDecompressor.setBytes(bs);
    }

    private Double recoverVByBetaStar(int i) {
        double v;
        Double vPrime = xorDecompressor.readValue();
//        改XOR版本
//        Double vPrime = xorDecompressor.readValue(noEraseList[i]);
        int FAlpha = Elf64Utils.getFAlpha(uniAlpha);
        int exp = (int) (Double.doubleToRawLongBits(vPrime) >> 52 & 0x7ff);
        int gAlpha = FAlpha + exp - 1023;
        int eraseBits = 52 - gAlpha;
        if (noEraseList[i]==0){
            vPrime -= offset; // 1048576
        }
        vPrime = signList[i]==0?vPrime:-vPrime;
        if (eraseBits>0 && noEraseList[i]==0){
//            if (uniAlpha<16)
            v = Elf64Utils.roundUp(vPrime, uniAlpha);
//            else{
//                BigDecimal bd = new BigDecimal(vPrime);
//                v = bd.setScale(uniAlpha,BigDecimal.ROUND_UP).doubleValue();
//            }
        }else{
            v = vPrime;
        }
//        int sp = Elf64Utils.getSP(Math.abs(vPrime));
//        if (lastBetaStar == 0) {
//            v = Elf64Utils.get10iN(-sp - 1);
//            if (vPrime < 0) {
//                v = -v;
//            }
//        } else {
//            int alpha = lastBetaStar - sp - 1;
//            v = Elf64Utils.roundUp(vPrime, alpha);
//        }
        return v;
    }

    private int readInt(int len) {
        InputBitStream in = xorDecompressor.getInputStream();
        try {
            return in.readInt(len);
        } catch (IOException e) {
            throw new RuntimeException("IO error: " + e.getMessage());
        }
    }
}
