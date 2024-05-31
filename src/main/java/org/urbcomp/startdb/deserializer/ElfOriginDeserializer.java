package org.urbcomp.startdb.deserializer;

import org.urbcomp.startdb.selfstar.decompressor.ElfPlusDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.IDecompressor;
import org.urbcomp.startdb.selfstar.decompressor.xor.ElfPlusXORDecompressor;
import org.urbcomp.startdb.utils.DeltaOfDelta;

import java.io.IOException;
import java.util.List;

public class ElfOriginDeserializer {
    private List<Long> Timestamp;
    private List<Double> Lon;
    private List<Double> Lat;
    private String Uid;
    private IDecompressor Decompressor;

    public ElfOriginDeserializer(){
        Decompressor = new ElfPlusDecompressor(new ElfPlusXORDecompressor());
    }

    //解压时间
    public void decompressTime(byte[] timeStream){
        try {
            Timestamp = DeltaOfDelta.decompress(timeStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //解压浮点
    public void decompressLon(byte[] lonStream){
        Decompressor.setBytes(lonStream);
        Lon = Decompressor.decompress();

        Decompressor.refresh();
    }
    public void decompressLat(byte[] latStream){
        Decompressor.setBytes(latStream);
        Lat = Decompressor.decompress();

        Decompressor.refresh();
    }



}
