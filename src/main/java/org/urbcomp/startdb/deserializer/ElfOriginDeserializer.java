package org.urbcomp.startdb.deserializer;

import org.urbcomp.startdb.selfstar.decompressor.ElfPlusDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.IDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.xor.ElfPlusXORDecompressor;
import org.urbcomp.startdb.utils.DeltaOfDelta;

import java.io.IOException;
import java.util.List;

public class ElfOriginDeserializer {
    private List<Long> Timestamp;
    private List<Double> Longitude;
    private List<Double> Latitude;
    private String Uid;
    private final IDecompressor Decompressor;

    public ElfOriginDeserializer(){
        Decompressor = new ElfPlusDecompressor(new ElfPlusXORDecompressor());
    }

    //解压时间
    public void decompressTime(byte[] timeStream){
        //System.out.println("timestream size: " + timeStream.length);
        try {
            Timestamp = DeltaOfDelta.decompress(timeStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Timestamp size: " + Timestamp.size());
    }

    //解压浮点
    public void decompressLon(byte[] lonStream){
        Decompressor.setBytes(lonStream);
        Longitude = Decompressor.decompress();

        Decompressor.refresh();
    }
    public void decompressLat(byte[] latStream){
        Decompressor.setBytes(latStream);
        Latitude = Decompressor.decompress();

        Decompressor.refresh();
    }

    public List<Long> getTimestamp(){
        return Timestamp;
    }
    public List<Double> getLongitude(){
        return Longitude;
    }
    public List<Double> getLatitude(){
        return Latitude;
    }

    public long getSize(){
        return getTimestamp().size() + getLongitude().size() + getLatitude().size();
    }

}
