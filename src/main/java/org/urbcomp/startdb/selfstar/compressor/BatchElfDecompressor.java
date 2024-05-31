package org.urbcomp.startdb.selfstar.compressor;

import org.urbcomp.startdb.selfstar.utils.Elf64Utils;
import org.urbcomp.startdb.selfstar.utils.InputBitStream;

import java.io.IOException;

public class BatchElfDecompressor {
    private final InputBitStream in;
    private int numberOfValues;

    public BatchElfDecompressor(byte[] bs) {
        in = new InputBitStream(bs);
    }

    public double[] decompress() throws IOException {

        numberOfValues = in.readInt(32);

        int[] sign = new int[numberOfValues];
//        int[] betaStar = new int[numberOfValues];
        long[] xorResult = new long[numberOfValues - 1];
        int[] leadingZero = new int[numberOfValues - 1];
        int[] trailingZero = new int[numberOfValues - 1];
        long[] expAndMantissa = new long[numberOfValues];
        double[] dbs = new double[numberOfValues];
        int[] continueMap = new int[numberOfValues];
        expAndMantissa[0] = in.readLong(64);


        int signLength = in.readInt(32);
        int signCntWidth = in.readInt(32);
        int signValue,signCnt,signIdx=0;
        for (int i=0;i<signLength;i++){
            signValue = in.readBit();
            signCnt = in.readInt(signCntWidth);
            for (int j=0;j<signCnt;j++){
                sign[signIdx++] = signValue;
            }
        }

        int uniAlpha = in.readInt(32);

//        int betaStarLength = in.readInt(32);
//        int betaStarWidth = in.readInt(32);
//        int betaStarCntWidth = in.readInt(32);
//        int minBetaStar = in.readInt(32);
//        int betaStarValue,betaStarCnt,betaStarIdx=0;
//        for (int i=0;i<betaStarLength;i++){
//            betaStarValue = in.readInt(betaStarWidth);
//            betaStarCnt = in.readInt(betaStarCntWidth);
//            for (int j=0;j<betaStarCnt;j++){
//                betaStar[betaStarIdx++] = betaStarValue + minBetaStar;
//            }
//        }

        for (int i=0;i<numberOfValues;i++){
            continueMap[i] = in.readBit();
        }

        int leadingZeroLength = in.readInt(32);
        int leadingZeroWidth = in.readInt(32);
        int leadingZeroCntWidth = in.readInt(32);
        int minLeadingZero = in.readInt(32);
        int leadingZeroValue,leadingZeroCnt,leadingZeroIdx=0;
        for (int i=0;i<leadingZeroLength;i++){
            leadingZeroValue = in.readInt(leadingZeroWidth);
            leadingZeroCnt = in.readInt(leadingZeroCntWidth);
            for (int j=0;j<leadingZeroCnt;j++){
                leadingZero[leadingZeroIdx++] = leadingZeroValue + minLeadingZero;
            }
        }

        int trailingZeroLength = in.readInt(32);
        int trailingZeroWidth = in.readInt(32);
        int trailingZeroCntWidth = in.readInt(32);
        int minTrailingZero = in.readInt(32);
        int trailingZeroValue,trailingZeroCnt,trailingZeroIdx=0;
        for (int i=0;i<trailingZeroLength;i++){
            trailingZeroValue = in.readInt(trailingZeroWidth);
            trailingZeroCnt = in.readInt(trailingZeroCntWidth);
            for (int j=0;j<trailingZeroCnt;j++){
                trailingZero[trailingZeroIdx++] = trailingZeroValue + minTrailingZero;
            }
        }

        for (int i=0;i<numberOfValues-1;i++){
            if (continueMap[i+1]!=1)
                xorResult[i] = in.readLong(Math.max(0, 64-leadingZero[i]-trailingZero[i]));
        }

        long storedValue = expAndMantissa[0];
        double tmp;
//        int alpha,sp;
        int FAlpha = Elf64Utils.getFAlpha(uniAlpha);
        int exp = (int) (expAndMantissa[0] >> 52 & 0x7ff);
        int gAlpha = FAlpha + exp - 1023;
        int eraseBits = 52 - gAlpha;
        dbs[0] = Double.longBitsToDouble(((long) sign[0] <<63) | expAndMantissa[0]);
//        sp = (int) Math.floor(Math.log10(Math.abs(dbs[0])));
//        alpha = betaStar[0] - sp - 1;
        if (eraseBits>0 )
            dbs[0] = Elf64Utils.roundUp(dbs[0], uniAlpha);
        for (int i=1;i<numberOfValues;i++){
            if (continueMap[i]==1){
                dbs[i]=sign[i]==0 ? Math.abs(dbs[i-1]) : -Math.abs(dbs[i-1]);
            }else {
                long xored = (xorResult[i - 1] << trailingZero[i - 1]);
                expAndMantissa[i] = storedValue ^ xored;
                tmp = Double.longBitsToDouble(((long) sign[i] << 63) | expAndMantissa[i]);
//                sp = (int) Math.floor(Math.log10(Math.abs(tmp)));
//                alpha = betaStar[i] - sp - 1;
                dbs[i] = Elf64Utils.roundUp(tmp, uniAlpha);

                exp = (int) (expAndMantissa[i] >> 52 & 0x7ff);
                gAlpha = FAlpha + exp - 1023;
                eraseBits = 52 - gAlpha;

//                if (uniAlpha >= 15)
                if (eraseBits<=0)
                    dbs[i] = tmp;
                storedValue = expAndMantissa[i];
            }
        }

        int exceptionCnt = in.readInt(32);
        for (int i=0;i<exceptionCnt;i++){
            dbs[in.readInt(32)] = Double.longBitsToDouble(in.readLong(64));
        }

        return dbs;
    }
}
