package com.craftpass.auth;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

/**
 * Self-contained OpenBSD-style Blowfish password hashing (BCrypt).
 * Embedded to ensure zero external dependency issues across any Minecraft version.
 */
public class BCrypt {
    private static final int BCRYPT_SALT_LEN = 16;
    private static final int BLOWFISH_NUM_ROUNDS = 16;

    private static final int[] P_orig = {
        0x243f6a88, 0x85a308d3, 0x13198a2e, 0x03707344,
        0xa4093822, 0x299f31d0, 0x082efa98, 0xec4e6c89,
        0x452821e6, 0x38d01377, 0xbe5466cf, 0x34e90c6c,
        0xc0ac29b7, 0xc97c50dd, 0x3f84d5b5, 0xb5470917,
        0x9216d5d9, 0x8979fb1b
    };

    private static final int[] S_orig = {
        0xd1310ba6, 0x98dfb5ac, 0x2ffd72db, 0xd01adfb7, 0xb8e1afed, 0x6a267e96, 0xba7c9045, 0xf12c7f99,
        0x24a19947, 0xb3916cf7, 0x0801f2e2, 0x858efc16, 0x636920d8, 0x71574e69, 0xa458fea3, 0xf4933d7e,
        0x0d95748f, 0x728eb658, 0x718bcd58, 0x82154aee, 0x7b54a41d, 0xc25a59b5, 0x9c30d539, 0x2af26013,
        0xc5d1b023, 0x286085f0, 0xca417918, 0xb8db38ef, 0x8e79dcb0, 0x603a180e, 0x6c9e0e8b, 0xb01e8a3e,
        0xd71577c1, 0xbd314b27, 0x78af2fda, 0x55605c60, 0xe65525f3, 0xaa55ab94, 0x57489862, 0x63e81440,
        0x55ca396a, 0x2aab10b6, 0xb4cc5c34, 0x1141e8ce, 0xa15486af, 0x7c72e993, 0xb3ee1411, 0x636fbc2a,
        0x2ba9c55d, 0x741831f6, 0xce5c3e16, 0x9b87931e, 0xafd6ba33, 0x6c24cf5c, 0x7a325381, 0x28958677,
        0x3b8f4898, 0x6b4bb9af, 0xc4bfe81b, 0x66282193, 0x61d809cc, 0xfb21a991, 0x487cac60, 0x5dec8032,
        0x04f15747, 0x62e007b0, 0x114e1366, 0xec34f571, 0x3e914e50, 0x956842a6, 0x83822607, 0x92fadd10,
        0x61f365d1, 0x5236ec6b, 0x90483a43, 0x80a75394, 0xbe477ac0, 0xfe3f0c14, 0x759f49e3, 0x800cc44e,
        0x641e5e16, 0x782390c8, 0x4e783f6b, 0x4752de73, 0xdbcdec60, 0x0c605a61, 0x4dd82421, 0x9cb28e47,
        0x9794e0aa, 0x110b79a3, 0x58f425de, 0xe0041d00, 0xe1f56805, 0xbfe404d0, 0x6d4f3027, 0x5c2b0e0e,
        0x86e79867, 0xbec0eb34, 0x800d7779, 0x9fbff97e, 0xab894b48, 0x2147743f, 0x36413f93, 0x4fcac0ab,
        0x4bf924d6, 0xa573a10d, 0xaaf9504e, 0xd0a02c50, 0x52bf6e6d, 0x8a63011e, 0x384752fb, 0x661a37bb,
        0xeb44814a, 0x1e3a9926, 0x665248cf, 0x2b30e541, 0xf64256fc, 0x98717728, 0xbff3654f, 0x7aec34d6,
        0x563253fa, 0x85e24eb0, 0xe655d9f2, 0x632d43a7, 0xbb946e79, 0x84e1b0ad, 0x6aa2b9f0, 0x5fa63085,
        0xad00a945, 0x4443a6cd, 0x52acfc10, 0x9e000838, 0x01031836, 0xb4d0eff5, 0x3b5f789f, 0x7a15f02f,
        0x8338327c, 0xf764fb5b, 0x499d0ab8, 0xb8a69f74, 0xdd9505e0, 0x3c018d54, 0xcdcf7f78, 0x0777e40f,
        0xb62690e0, 0x186ff6ec, 0xdd73f323, 0x17d643a0, 0xf474efea, 0x4e040c4a, 0x52834791, 0xace4d96e,
        0xfaaa6e38, 0x876d7252, 0xb509e628, 0x86edfc52, 0xfdeba614, 0x80e44d62, 0x6a396493, 0x70932441,
        0x3ec61580, 0xca8476c0, 0x00808aec, 0x51567b42, 0x49764ab9, 0x43e1f79c, 0xb0e12b4c, 0x5499110a,
        0xd532d14d, 0x302e5325, 0x52e51f1e, 0xbe079d6b, 0x05466302, 0x4516ff80, 0xe027416f, 0x07f3c9e0,
        0x5342802e, 0x62b8a4f2, 0xf141bce2, 0x41172266, 0x6447e35b, 0x306dddec, 0x1f1766c6, 0x644fa8fb,
        0x301a3000, 0xc1025527, 0xfaed736f, 0x3d4d4b04, 0xca392327, 0x47da5835, 0x312e625e, 0x0f168df2,
        0x35db6778, 0x0ca4ec3e, 0xe9741f25, 0xa9776770, 0x902e4143, 0xb971f24a, 0x60177747, 0xc4492c18,
        0xd982ce4e, 0x9801f42e, 0x9908614c, 0x7b083473, 0x9f45b27e, 0x315ff121, 0x23b0366b, 0x99723a9d,
        0x7fa6462e, 0x63e56e80, 0x0f21f182, 0x412008c3, 0x5973222e, 0xb54ff4ee, 0xb73f14f3, 0x595d23b7,
        0x34bc37a6, 0x14f76141, 0x1025471e, 0xec70d0c0, 0x44914f74, 0x5f84a663, 0x5aecf70c, 0x633633d2,
        0xf2826c9f, 0x7877e74d, 0x4ece6fb0, 0x69a60e47, 0x47b01b48, 0x2c59524c, 0x83260076, 0x3126b949,
        0xbef825f4, 0x883f02c8, 0xcd9f33fc, 0x654f3a14, 0x83444823, 0x97e00830, 0x2949e354, 0x90153317,
        0x800c4766, 0x1b4115f4, 0x9902d730, 0x5f428402, 0x8ac3320a, 0x6264597b, 0xf24a8ac1, 0x73916ec6,
        0x20ba5790, 0xb503f2d1, 0xe395163d, 0x677947ab, 0x94e2d303, 0x8dd01c2d, 0xa6f2611f, 0xf9247f2f
    };

    private static final int[] bf_crypt_ciphertext = {
        0x4f727068, 0x65616e42, 0x65686f6c,
        0x64657253, 0x63727970, 0x74686173
    };

    private static final char[] base64_code = {
        '.', '/', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
        'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
        'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
        'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
        'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9'
    };

    private static final byte[] index_64 = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,  0,  1,
        54, 55, 56, 57, 58, 59, 60, 61, 62, 63, -1, -1, -1, -1, -1, -1,
        -1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15, 16,
        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, -1, -1, -1, -1, -1,
        -1, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
        43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, -1, -1, -1, -1, -1
    };

    private int[] P;
    private int[] S;

    private static void encode_base64(byte[] d, int len, StringBuilder rs) {
        int off = 0;
        int c1, c2;
        while (off < len) {
            c1 = (d[off++] & 0xff);
            rs.append(base64_code[(c1 >> 2) & 0x3f]);
            c1 = (c1 & 0x03) << 4;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = (d[off++] & 0xff);
            c1 |= (c2 >> 4) & 0x0f;
            rs.append(base64_code[c1 & 0x3f]);
            c1 = (c2 & 0x0f) << 2;
            if (off >= len) {
                rs.append(base64_code[c1 & 0x3f]);
                break;
            }
            c2 = (d[off++] & 0xff);
            c1 |= (c2 >> 6) & 0x03;
            rs.append(base64_code[c1 & 0x3f]);
            rs.append(base64_code[c2 & 0x3f]);
        }
    }

    private static byte char64(char x) {
        if (x < 0 || x > 127) return -1;
        return index_64[x];
    }

    private static byte[] decode_base64(String s, int maxolen) {
        StringBuilder rs = new StringBuilder();
        int off = 0, slen = s.length(), olen = 0;
        byte[] ret = new byte[maxolen];
        byte c1, c2, c3, c4;

        while (off < slen - 1 && olen < maxolen) {
            c1 = char64(s.charAt(off++));
            c2 = char64(s.charAt(off++));
            if (c1 == -1 || c2 == -1) break;
            ret[olen++] = (byte) ((c1 << 2) | ((c2 & 0x30) >> 4));
            if (olen >= maxolen || off >= slen) break;
            c3 = char64(s.charAt(off++));
            if (c3 == -1) break;
            ret[olen++] = (byte) (((c2 & 0x0f) << 4) | ((c3 & 0x3c) >> 2));
            if (olen >= maxolen || off >= slen) break;
            c4 = char64(s.charAt(off++));
            if (c4 == -1) break;
            ret[olen++] = (byte) (((c3 & 0x03) << 6) | c4);
        }
        return ret;
    }

    private void init_key() {
        P = (int[]) P_orig.clone();
        S = (int[]) S_orig.clone();
    }

    private int streamtoword(byte[] data, int[] offp) {
        int i;
        int word = 0;
        int off = offp[0];

        for (i = 0; i < 4; i++) {
            word = (word << 8) | (data[off] & 0xff);
            off = (off + 1) % data.length;
        }

        offp[0] = off;
        return word;
    }

    private void encipher(int[] lr, int off) {
        int i, n, l = lr[off], r = lr[off + 1];

        l ^= P[0];
        for (i = 0; i <= BLOWFISH_NUM_ROUNDS - 2; ) {
            // Round 1
            n = S[l >>> 24];
            n += S[0x100 | ((l >> 16) & 0xff)];
            n ^= S[0x200 | ((l >> 8) & 0xff)];
            n += S[0x300 | (l & 0xff)];
            r ^= n ^ P[++i];
            // Round 2
            n = S[r >>> 24];
            n += S[0x100 | ((r >> 16) & 0xff)];
            n ^= S[0x200 | ((r >> 8) & 0xff)];
            n += S[0x300 | (r & 0xff)];
            l ^= n ^ P[++i];
        }
        lr[off] = r ^ P[BLOWFISH_NUM_ROUNDS + 1];
        lr[off + 1] = l;
    }

    private void key(byte[] key) {
        int i;
        int[] koff = {0};
        int[] lr = {0, 0};
        int plen = P.length, slen = S.length;

        for (i = 0; i < plen; i++)
            P[i] = P[i] ^ streamtoword(key, koff);

        for (i = 0; i < plen; i += 2) {
            encipher(lr, 0);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (i = 0; i < slen; i += 2) {
            encipher(lr, 0);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private void ekskey(byte[] data, byte[] key) {
        int i;
        int[] doff = {0}, koff = {0};
        int[] lr = {0, 0};
        int plen = P.length, slen = S.length;

        for (i = 0; i < plen; i++)
            P[i] = P[i] ^ streamtoword(key, koff);

        for (i = 0; i < plen; i += 2) {
            lr[0] ^= streamtoword(data, doff);
            lr[1] ^= streamtoword(data, doff);
            encipher(lr, 0);
            P[i] = lr[0];
            P[i + 1] = lr[1];
        }

        for (i = 0; i < slen; i += 2) {
            lr[0] ^= streamtoword(data, doff);
            lr[1] ^= streamtoword(data, doff);
            encipher(lr, 0);
            S[i] = lr[0];
            S[i + 1] = lr[1];
        }
    }

    private byte[] crypt_raw(byte[] password, byte[] salt, int log_rounds) {
        int rounds, i, c;
        int[] lr = {0, 0};
        byte[] ret = new byte[bf_crypt_ciphertext.length * 4];

        rounds = 1 << log_rounds;

        init_key();
        ekskey(salt, password);
        for (i = 0; i < rounds; i++) {
            key(password);
            key(salt);
        }

        int[] cdata = (int[]) bf_crypt_ciphertext.clone();
        int clen = cdata.length;
        for (i = 0; i < 64; i++) {
            for (c = 0; c < clen; c += 2) {
                lr[0] = cdata[c];
                lr[1] = cdata[c + 1];
                encipher(lr, 0);
                cdata[c] = lr[0];
                cdata[c + 1] = lr[1];
            }
        }

        for (i = 0; i < clen; i++) {
            ret[i * 4] = (byte) ((cdata[i] >> 24) & 0xff);
            ret[i * 4 + 1] = (byte) ((cdata[i] >> 16) & 0xff);
            ret[i * 4 + 2] = (byte) ((cdata[i] >> 8) & 0xff);
            ret[i * 4 + 3] = (byte) (cdata[i] & 0xff);
        }
        return ret;
    }

    public static String hashpw(String password, String salt) {
        BCrypt B;
        String real_salt;
        byte[] passwordb, saltb, hashed;
        char minor = (char) 0;
        int rounds, off;
        StringBuilder rs = new StringBuilder();

        if (salt == null || salt.length() < 28)
            throw new IllegalArgumentException("Invalid salt length");
        if (salt.charAt(0) != '$' || salt.charAt(1) != '2')
            throw new IllegalArgumentException("Invalid salt version");
        if (salt.charAt(2) == '$') {
            off = 3;
        } else {
            minor = salt.charAt(2);
            if ((minor != 'a' && minor != 'b' && minor != 'y') || salt.charAt(3) != '$')
                throw new IllegalArgumentException("Invalid salt revision");
            off = 4;
        }

        if (salt.charAt(off + 2) > '$')
            throw new IllegalArgumentException("Missing salt rounds");
        rounds = Integer.parseInt(salt.substring(off, off + 2));

        real_salt = salt.substring(off + 3, off + 25);
        try {
            passwordb = (password + (minor >= 'a' ? "\000" : "")).getBytes("UTF-8");
        } catch (Exception e) {
            passwordb = password.getBytes();
        }

        saltb = decode_base64(real_salt, BCRYPT_SALT_LEN);

        B = new BCrypt();
        hashed = B.crypt_raw(passwordb, saltb, rounds);

        rs.append("$2a$");
        if (rounds < 10)
            rs.append("0");
        rs.append(rounds);
        rs.append("$");
        encode_base64(saltb, saltb.length, rs);
        encode_base64(hashed, bf_crypt_ciphertext.length * 4 - 1, rs);
        return rs.toString();
    }

    public static String gensalt(int log_rounds) {
        StringBuilder rs = new StringBuilder();
        byte[] rnd = new byte[BCRYPT_SALT_LEN];
        new SecureRandom().nextBytes(rnd);

        rs.append("$2a$");
        if (log_rounds < 10)
            rs.append("0");
        rs.append(log_rounds);
        rs.append("$");
        encode_base64(rnd, rnd.length, rs);
        return rs.toString();
    }

    public static String gensalt() {
        return gensalt(10);
    }

    public static boolean checkpw(String plaintext, String hashed) {
        if (hashed == null || hashed.length() < 28) return false;
        try {
            return hashed.equals(hashpw(plaintext, hashed));
        } catch (Exception e) {
            return false;
        }
    }
}
