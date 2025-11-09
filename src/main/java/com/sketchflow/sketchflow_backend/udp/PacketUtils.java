package com.sketchflow.sketchflow_backend.udp;

import java.util.zip.CRC32;

public class PacketUtils {
    public static int crc32(byte[] data){
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }
}

