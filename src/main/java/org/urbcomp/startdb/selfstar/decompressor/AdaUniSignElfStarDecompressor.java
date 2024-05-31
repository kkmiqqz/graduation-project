package org.urbcomp.startdb.selfstar.decompressor;

import org.urbcomp.startdb.selfstar.decompressor.xor.IXORDecompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdaUniSignElfStarDecompressor implements IDecompressor {
    private final IXORDecompressor xorDecompressor;
    //    private int lastBetaStar = Integer.MAX_VALUE;
    private int uniAlpha;
    private int numberOfValues;
    private int[] signList;
    private int[] noEraseList;

    public AdaUniSignElfStarDecompressor(IXORDecompressor xorDecompressor) {
        this.xorDecompressor = xorDecompressor;
    }

    public List<Double> decompress() {
        numberOfValues = readInt(32);
        uniAlpha = readInt(32);

        // signList
        signList = new int[numberOfValues];
        int signLength = readInt(32);
        int signCntWidth = readInt(32);

        // 禁用二值序列优化
        int signValue,signCnt,signIdx=0;
        for (int i=0;i<signLength;i++){
            signValue = readInt(1);
            signCnt = signCnt = signCntWidth==0 ? 1 : readInt(signCntWidth);
            for (int j=0;j<signCnt;j++){
                signList[signIdx++] = signValue;
            }
        }

        // 启用二值序列优化
//        int nonSplit = readInt(1);
//        if (nonSplit==0){
//            int signValue,signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
//                signValue = readInt(1);
//                signCnt = readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//            }
//        }else{
//            int signValue = readInt(1);
//            int signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
////                signValue = readInt(1);
//                signCnt = readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//                signValue = 1-signValue;
//            }
//        }

        // noEraseList
        noEraseList = new int[numberOfValues];
        int noEraseListLength = readInt(32);
        int noEraseListCntWidth = readInt(32);

        // 禁用二值序列优化
        int noEraseListValue,noEraseListCnt,noEraseListIdx=0;
        for (int i=0;i<noEraseListLength;i++){
            noEraseListValue = readInt(1);
            noEraseListCnt = noEraseListCntWidth == 0 ? 1 : readInt(noEraseListCntWidth);
            for (int j=0;j<noEraseListCnt;j++){
                noEraseList[noEraseListIdx++] = noEraseListValue;
            }
        }

        // 启用二值序列优化
//        int nonSplit = readInt(1);
//        if (nonSplit==0){
//            int signValue,signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
//                signValue = readInt(1);
//                signCnt = readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//            }
//        }else{
//            int signValue = readInt(1);
//            int signCnt,signIdx=0;
//            for (int i=0;i<signLength;i++){
////                signValue = readInt(1);
//                signCnt = readInt(signCntWidth);
//                for (int j=0;j<signCnt;j++){
//                    signList[signIdx++] = signValue;
//                }
//                signValue = 1-signValue;
//            }
//        }


        List<Double> values = new ArrayList<>(1024);
        Double value;
        for(int i=0; i<numberOfValues; i++) {
            value = nextValue(i);
            values.add(signList[i]==0?value:-value);
        }
        return values;
    }

    @Override
    public void refresh() {
//        lastBetaStar = Integer.MAX_VALUE;
        xorDecompressor.refresh();
    }

    @Override
    public void setBytes(byte[] bs) {
        this.xorDecompressor.setBytes(bs);
    }

    @Override
    public Double nextValue() {
        Double v;

        v = recoverVByBetaStar(0);

//        if (readInt(1) == 0) {
//            v = recoverVByBetaStar();               // case 0
//        } else if (readInt(1) == 0) {
//            v = xorDecompressor.readValue();        // case 10
//        } else {
//            lastBetaStar = readInt(4);          // case 11
//            v = recoverVByBetaStar();
//        }
        return v;
    }
    public Double nextValue(int i) {
        Double v;

        v = recoverVByBetaStar(i);
        return v;
    }

    private Double recoverVByBetaStar(int i) {
        double v;
        Double vPrime = xorDecompressor.readValue();
        int FAlpha = Elf64Utils.getFAlpha(uniAlpha);
        int exp = (int) (Double.doubleToRawLongBits(vPrime) >> 52 & 0x7ff);
        int gAlpha = FAlpha + exp - 1023;
        int eraseBits = 52 - gAlpha;
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
