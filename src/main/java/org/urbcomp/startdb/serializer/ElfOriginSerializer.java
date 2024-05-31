package org.urbcomp.startdb.serializer;

import org.urbcomp.startdb.selfstar.compressor.ElfPlusCompressor;
import org.urbcomp.startdb.selfstar.compressor.ICompressor;
import org.urbcomp.startdb.selfstar.compressor.xor.ElfPlusXORCompressor;
import org.urbcomp.startdb.utils.DeltaOfDelta;

import java.io.IOException;
import java.util.List;

public class ElfOriginSerializer {
    private byte[] Timestream;
    private byte[] Lonstream;
    private byte[] Latstream;
    private String Uid;
    private final ICompressor Compressor;

    public ElfOriginSerializer(){
        Compressor = new ElfPlusCompressor(new ElfPlusXORCompressor());
    }

    //压缩时间戳
    public void compressTime(List<Long> timeList){
        try {
            Timestream = DeltaOfDelta.compress(timeList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //压缩浮点
    public void compressLon(List<Double> lonList) throws IOException {
        lonList.forEach(Compressor::addValue);
        Compressor.close();

        Lonstream = Compressor.getBytes();

        Compressor.refresh(); //ElfStar中可以保留部分参数
    }
    public void compressLat(List<Double> latList) throws IOException {
        latList.forEach(Compressor::addValue);
        Compressor.close();

        Latstream = Compressor.getBytes();

        Compressor.refresh(); //ElfStar中可以保留部分参数
    }

    //压缩Uid
    public void compressUid(List<String> uidList){
       Uid = uidList.get(0); //同一轨迹相同，只记录一个
    }

    public byte[] getTimestream(){
        return Timestream;
    }
    public byte[] getLonstream(){
        return Lonstream;
    }
    public byte[] getLatstream(){
        return Latstream;
    }
    public long getByteSize(){
        return Timestream.length + Lonstream.length + Latstream.length + Uid.length();
    }


}
