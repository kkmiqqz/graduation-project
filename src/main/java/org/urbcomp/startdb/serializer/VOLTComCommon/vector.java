package org.urbcomp.startdb.serializer.VOLTComCommon;

public class vector {
    private AbstractFunction function;
    private long sTime;
    private long eTime;

    public vector() {}
    public vector(AbstractFunction func, long stime, long etime){
        function = func;
        sTime = stime;
        eTime = etime;
    }
}
