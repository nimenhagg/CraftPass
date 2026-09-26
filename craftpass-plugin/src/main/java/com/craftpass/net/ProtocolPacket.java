package com.craftpass.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Binary frame protocol for CraftPass TCP connection.
 * Format:
 * [4-byte int TotalLength]
 * [2-byte short Magic: 0x4350 ('C''P')]
 * [2-byte short PacketTypeId]
 * [UTF-8 JSON Payload bytes]
 */
public class ProtocolPacket {
    public static final short MAGIC = (short) 0x4350; // "CP"
    public static final int MAX_PACKET_SIZE = 1024 * 1024; // 1 MB max frame

    private final PacketType type;
    private final String payloadJson;

    public ProtocolPacket(PacketType type, String payloadJson) {
        this.type = type;
        this.payloadJson = payloadJson != null ? payloadJson : "{}";
    }

    public PacketType getType() {
        return type;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void writeTo(DataOutputStream out) throws IOException {
        byte[] payloadBytes = payloadJson.getBytes(StandardCharsets.UTF_8);
        int totalLength = 2 + 2 + payloadBytes.length; // Magic (2) + Type (2) + Payload
        out.writeInt(totalLength);
        out.writeShort(MAGIC);
        out.writeShort(type.getId());
        out.write(payloadBytes);
        out.flush();
    }

    public static ProtocolPacket readFrom(DataInputStream in) throws IOException {
        int totalLength = in.readInt();
        if (totalLength < 4 || totalLength > MAX_PACKET_SIZE) {
            throw new IOException("Invalid packet length: " + totalLength);
        }
        short magic = in.readShort();
        if (magic != MAGIC) {
            throw new IOException(String.format("Invalid protocol magic: 0x%04X (expected 0x%04X)", magic, MAGIC));
        }
        short typeId = in.readShort();
        PacketType type = PacketType.fromId(typeId);

        int payloadLen = totalLength - 4;
        byte[] payloadBytes = new byte[payloadLen];
        in.readFully(payloadBytes);

        String json = new String(payloadBytes, StandardCharsets.UTF_8);
        return new ProtocolPacket(type, json);
    }
}
