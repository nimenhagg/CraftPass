package com.craftpass.net;

/**
 * Protocol packet opcodes for CraftPass TCP protocol.
 */
public enum PacketType {
    HANDSHAKE_REQ(0x0001),
    HANDSHAKE_RESP(0x0002),

    AUTH_REQ(0x0003),
    AUTH_RESP(0x0004),

    GET_INVENTORY_REQ(0x0005),
    GET_INVENTORY_RESP(0x0006),

    CHANGE_PASSWORD_REQ(0x0007),
    CHANGE_PASSWORD_RESP(0x0008),

    MIGRATE_ACCOUNT_REQ(0x0009),
    MIGRATE_ACCOUNT_RESP(0x000A),

    GET_GEYSER_LINK_REQ(0x000B),
    GET_GEYSER_LINK_RESP(0x000C),
    LINK_GEYSER_REQ(0x000D),
    LINK_GEYSER_RESP(0x000E),
    UNLINK_GEYSER_REQ(0x000F),
    UNLINK_GEYSER_RESP(0x0010),

    HEARTBEAT(0x00FE),
    ERROR(0x00FF);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) return type;
        }
        return ERROR;
    }
}
