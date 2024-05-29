package org.urbcomp.startdb.deserializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.Input;
import org.urbcomp.startdb.gpsPoint;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class KryoOriginDeserializer implements IDeserializer{
    private Kryo kryo;
    public KryoOriginDeserializer(){
        kryo = new Kryo();
        kryo.register(ArrayList.class);
        kryo.register(gpsPoint.class);
    }
    public  List<gpsPoint> deserializeData(String inputFile) {
        List<gpsPoint> result = new ArrayList<>();
        try (Input input = new Input(new FileInputStream(inputFile))) { //没有将文件加载到内存
            while (!input.end()) {
            gpsPoint tempPoint = kryo.readObject(input, gpsPoint.class); //Kryo按照注册的对象类型进行read
            result.add(tempPoint);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public  List<gpsPoint> deserialize(byte[] data) {
        ByteBufferInput input = new ByteBufferInput(data);
        return kryo.readObject(input, ArrayList.class);
    }
}
