package com.sketchflow.sketchflow_backend.udp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

public class PacketUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static int crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }

    public static byte[] serialize(Object obj) throws Exception {
        return objectMapper.writeValueAsBytes(obj);
    }

    public static DatagramPacket createPacket(byte[] data, InetSocketAddress target) {
        return new DatagramPacket(data, data.length, target.getAddress(), target.getPort());
    }
}
