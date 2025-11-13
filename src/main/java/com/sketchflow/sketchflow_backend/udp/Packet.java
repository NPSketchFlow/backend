package com.sketchflow.sketchflow_backend.udp;

import java.nio.ByteBuffer;

public class Packet {
    public enum Type {
        DATA((byte)1), ACK((byte)2), NACK((byte)3), HEARTBEAT((byte)4);

        public final byte code;
        Type(byte c){ this.code = c; }
        public static Type fromByte(byte b){
            for(Type t: values()) if(t.code == b) return t;
            throw new IllegalArgumentException("Unknown type " + b);
        }
    }

    public final long id;
    public final Type type;
    public final byte[] payload;
    public final int checksum;

    public Packet(long id, Type type, byte[] payload, int checksum){
        this.id = id; this.type = type; this.payload = payload; this.checksum = checksum;
    }

    public static Packet of(long id, Type type, byte[] payload){
        int cs = PacketUtils.crc32(payload == null ? new byte[0] : payload);
        return new Packet(id, type, payload == null ? new byte[0] : payload, cs);
    }

    public byte[] toBytes(){
        int payloadLen = payload == null ? 0 : payload.length;
        ByteBuffer bb = ByteBuffer.allocate(8 + 1 + 4 + 4 + payloadLen);
        bb.putLong(id);
        bb.put(type.code);
        bb.putInt(payloadLen);
        bb.putInt(checksum);
        if(payloadLen>0) bb.put(payload);
        return bb.array();
    }

    public static Packet fromBytes(ByteBuffer bb){
        bb.rewind();
        long id = bb.getLong();
        byte typeB = bb.get();
        int len = bb.getInt();
        int cs = bb.getInt();
        byte[] p = new byte[Math.max(0, len)];
        if(len>0) bb.get(p);
        return new Packet(id, Type.fromByte(typeB), p, cs);
    }

    public boolean isValid(){
        return PacketUtils.crc32(payload == null ? new byte[0] : payload) == checksum;
    }
}

