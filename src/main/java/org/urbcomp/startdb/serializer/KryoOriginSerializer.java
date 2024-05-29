package org.urbcomp.startdb.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Output;
import org.urbcomp.startdb.gpsPoint;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class KryoOriginSerializer implements ISerializer{
    private Kryo kryo;
    public KryoOriginSerializer(){
        kryo = new Kryo();
        kryo.register(ArrayList.class);
        kryo.register(gpsPoint.class);
    }
    public void serializeData(List<gpsPoint> trajectory, String outputFile) {
        kryo = new Kryo();
        kryo.register(gpsPoint.class);

        try (Output output = new Output(new FileOutputStream(outputFile))) {
            for(gpsPoint point : trajectory){
                kryo.writeObject(output, point);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] serialize(List<gpsPoint> gpsPoints) {
        ByteBufferOutput output = new ByteBufferOutput(1024, -1); // no maximum size
        kryo.writeObject(output, gpsPoints);
        return output.toBytes();
    }


//    public byte[] serialize2ByteArray(List<gpsPoint> points) {
//
//        ByteBufferOutput output = new ByteBufferOutput(1024, -1); // no maximum size
//        kryo.writeObject(output, points);
//        return output.toBytes();
//    }
}
