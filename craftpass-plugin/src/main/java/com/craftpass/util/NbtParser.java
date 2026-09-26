package com.craftpass.util;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Pure Java, 100% version-agnostic NBT Parser.
 * Operates without any NMS or external NBT library dependencies,
 * guaranteeing seamless compatibility across Minecraft 1.16.5 through 1.21.x and modern Paper 26.x.
 */
public class NbtParser {

    public static CompoundTag readGzipFile(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file);
             InputStream gis = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gis)) {
            byte type = dis.readByte();
            if (type != 10) { // TagType.COMPOUND
                throw new IOException("Root tag is not a COMPOUND tag (type=" + type + ")");
            }
            readString(dis); // root name (usually empty)
            return readCompound(dis);
        }
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readUnsignedShort();
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    private static Object readTagPayload(byte type, DataInputStream in) throws IOException {
        switch (type) {
            case 0: return null; // END
            case 1: return in.readByte();
            case 2: return in.readShort();
            case 3: return in.readInt();
            case 4: return in.readLong();
            case 5: return in.readFloat();
            case 6: return in.readDouble();
            case 7: { // BYTE_ARRAY
                int len = in.readInt();
                byte[] b = new byte[len];
                in.readFully(b);
                return b;
            }
            case 8: return readString(in);
            case 9: { // LIST
                byte elemType = in.readByte();
                int size = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, size));
                for (int i = 0; i < size; i++) {
                    list.add(readTagPayload(elemType, in));
                }
                return list;
            }
            case 10: return readCompound(in);
            case 11: { // INT_ARRAY
                int len = in.readInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) arr[i] = in.readInt();
                return arr;
            }
            case 12: { // LONG_ARRAY
                int len = in.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) arr[i] = in.readLong();
                return arr;
            }
            default:
                throw new IOException("Unknown NBT tag type: " + type);
        }
    }

    private static CompoundTag readCompound(DataInputStream in) throws IOException {
        CompoundTag compound = new CompoundTag();
        while (true) {
            byte type = in.readByte();
            if (type == 0) break; // TAG_End
            String name = readString(in);
            Object value = readTagPayload(type, in);
            compound.put(name, value);
        }
        return compound;
    }

    public static class CompoundTag {
        private final Map<String, Object> map = new LinkedHashMap<>();

        public void put(String key, Object val) {
            map.put(key, val);
        }

        public boolean containsKey(String key) {
            return map.containsKey(key);
        }

        public Object get(String key) {
            return map.get(key);
        }

        public String getString(String key, String def) {
            Object v = map.get(key);
            return (v instanceof String) ? (String) v : def;
        }

        public int getInt(String key, int def) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).intValue();
            return def;
        }

        public byte getByte(String key, byte def) {
            Object v = map.get(key);
            if (v instanceof Number) return ((Number) v).byteValue();
            return def;
        }

        @SuppressWarnings("unchecked")
        public List<Object> getList(String key) {
            Object v = map.get(key);
            if (v instanceof List) return (List<Object>) v;
            return Collections.emptyList();
        }

        public CompoundTag getCompound(String key) {
            Object v = map.get(key);
            if (v instanceof CompoundTag) return (CompoundTag) v;
            return null;
        }

        public Map<String, Object> asMap() {
            return map;
        }
    }
}
