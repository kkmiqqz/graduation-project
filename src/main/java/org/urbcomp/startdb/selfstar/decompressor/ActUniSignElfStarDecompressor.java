package org.urbcomp.startdb.selfstar.decompressor;

import org.urbcomp.startdb.selfstar.decompressor.xor.IXORDecompressor;
import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ActUniSignElfStarDecompressor implements IDecompressor {
    private final IXORDecompressor xorDecompressor;
    //    private int lastBetaStar = Integer.MAX_VALUE;
    private int uniAlpha;
    private int numberOfValues;
    private int[] signList;

    public ActUniSignElfStarDecompressor(IXORDecompressor xorDecompressor) {
        this.xorDecompressor = xorDecompressor;
    }

    public List<Double> decompress() {
        numberOfValues = readInt(32);
        uniAlpha = readInt(32);
        signList = new int[numberOfValues];
        int signLength = readInt(32);
        int signCntWidth = readInt(32);

        // 禁用二值序列优化
//        int signValue,signCnt,signIdx=0;
//        for (int i=0;i<signLength;i++){
//            signValue = readInt(1);
//            signCnt = readInt(signCntWidth);
//            for (int j=0;j<signCnt;j++){
//                signList[signIdx++] = signValue;
//            }
//        }

        // 启用二值序列优化
        int nonSplit = readInt(1);
        if (nonSplit==0){
            int signValue,signCnt,signIdx=0;
            for (int i=0;i<signLength;i++){
                signValue = readInt(1);
                signCnt = readInt(signCntWidth);
                for (int j=0;j<signCnt;j++){
                    signList[signIdx++] = signValue;
                }
            }
        }else{
            int signValue = readInt(1);
            int signCnt,signIdx=0;
            for (int i=0;i<signLength;i++){
//                signValue = readInt(1);
                signCnt = readInt(signCntWidth);
                for (int j=0;j<signCnt;j++){
                    signList[signIdx++] = signValue;
                }
                signValue = 1-signValue;
            }
        }


        List<Double> values = new ArrayList<>(1024);
        Double value;
        for(int i=0; i<numberOfValues; i++) {
            value = nextValue();
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

        v = recoverVByBetaStar();

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

    private Double recoverVByBetaStar() {
        double v;
        Double vPrime = xorDecompressor.readValue();

        int FAlpha = Elf64Utils.getFAlpha(uniAlpha);
        int exp = (int) (Double.doubleToRawLongBits(vPrime) >> 52 & 0x7ff);
        int gAlpha = FAlpha + exp - 1023;
        int eraseBits = 52 - gAlpha;
        if (eraseBits>0){
            v = Elf64Utils.roundUp(vPrime, uniAlpha);
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
