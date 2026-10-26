// <editor-fold defaultstate="collapsed" desc="// LGPL-3.0-or-later | SPDX-FileCopyrightText: 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* SPDX-FileCopyrightText: 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

/**
 * <i>「悪しき霊よ、還りたまえ！」<br><br>
 * 「恐れないで……。<br>
 * &nbsp;&nbsp;&nbsp;希望を掲げて進歩を続けるのです」<br><br>
 * 「お願い負けないでカラスさんっ<br>
 * &nbsp;&nbsp;&nbsp;きっとあと少しです！」</i>
 */
public final class Nyan384 implements RandomGenerator.SplittableGenerator {
    public Nyan384(long s) { this(s, mix(s, K6, K7), 0L, 0L); }
    public Nyan384(long lo, long hi) { this(lo, hi, 0L, 0L); }

    static final long C1 = 0x300AD87B6FCBDA47L, C2 = 0xB10B6D24D3143C7FL, C3 = 0xB630487DA7E50FBFL, KT = 0x415D4F6036E7610BL;
    static final long K0 = 0x8CB3F299AB1A985DL, K1 = 0x1B671734CD36A1E7L, K2 = 0x14EE82EC222E3D4DL, K3 = 0x5DF9D97160135825L, K4 = 0x7D30A7959E2CA223L, K5 = 0x6EA44F948450AC6DL, K6 = 0x37E532043257C55DL, K7 = 0x2AA7B08E04A6B34FL;
    static final long SA = 0x003877BAB47AAFF3L, SB = 0xEA9A9815A828B6FBL, SC = 0x8546B52929DA1390L, SD = 0x030112EAAE97ECD7L;
    static final long PHI = 0x9E3779B97F4A7C15L;

    final long s0, s1;
    long c0, c1, m0, m1, m2, m3, nSp;

    Nyan384(long s0, long s1, long k0, long k1) { this.s0 = s0; this.s1 = s1; seek(k0, k1); }
    Nyan384(long s0, long s1, long c0, long c1, long m0, long m1, long m2, long m3) { this.s0 = s0; this.s1 = s1; this.c0 = c0; this.c1 = c1; this.m0 = m0; this.m1 = m1; this.m2 = m2; this.m3 = m3; }

    public Nyan384 seek(long k) { return seek(k, 0L); }
    public Nyan384 seek(long k0, long k1) {
        c0 = k0; c1 = k1;
        long w0 = s0 ^ Long.rotateLeft(k0, 7) ^ SA, w1 = s1 + Long.rotateLeft(k1, 41) ^ SB, w2 = k0 + Long.rotateLeft(s1, 13) ^ SC, w3 = k1 ^ Long.rotateLeft(s0, 29) ^ SD;
        m0 = mix(w0, K0, K1);
        m1 = mix(w1 ^ Long.rotateLeft(m0, 23), K2, K3);
        m2 = mix(w2 ^ Long.rotateLeft(m0, 47) ^ Long.rotateLeft(m1, 23), K4, K5);
        m3 = mix(w3 ^ Long.rotateLeft(m0, 7) ^ Long.rotateLeft(m1, 41) ^ Long.rotateLeft(m2, 23), K6, K7);
        m0 ^= Long.rotateLeft(m3, 19);
        m1 ^= Long.rotateLeft(m0, 53);
        m2 ^= Long.rotateLeft(m3, 29);
        return this;
    }
    static long mix(long z, long ka, long kb) {
        z ^= z >>> 30; z *= ka;
        z ^= z >>> 27; z *= kb;
        return z ^ (z >>> 31);
    }

    @Override public long nextLong() {
        if (++c0 == 0L) ++c1;
        return (c0 & 1L) == 0L ? tick(m0, m3, c0) : tick(m2, m1, c1);
    }
    long tick(long ma, long mb, long cc) {
        long x = Math.unsignedMultiplyHigh(ma ^ Long.rotateLeft(cc, (int) (mb >>> 58)), C1);
        x ^= Long.rotateLeft(x, 27); x = Math.unsignedMultiplyHigh(x ^ mb, C2);
        long out = (x ^ (x >>> 33)) * C3;
        long g0 = Math.unsignedMultiplyHigh(m2 ^ Long.rotateLeft(c0, 24), KT), g1 = (m3 ^ Long.rotateLeft(m2, 15)) + Long.rotateLeft(c1, 31);
        long n2 = m0 ^ g0 ^ Long.rotateLeft(g1, 29), n3 = m1 ^ (g1 + Long.rotateLeft(g0, 21));
        m0 = m2; m1 = m3; m2 = n2; m3 = n3;
        return out;
    }

    public void nextLongs(long[] dst, int off, int len) {
        Objects.checkFromToIndex(off, off + len, dst.length);
        int i = 0, lim = len & ~1;
        if ((c0 & 1L) == 0L) {
            while (i < lim) {
                if (++c0 == 0L) ++c1;
                dst[off + i++] = tick(m2, m1, c1);
                if (++c0 == 0L) ++c1;
                dst[off + i++] = tick(m0, m3, c0);
            }
        } else {
            while (i < lim) {
                if (++c0 == 0L) ++c1;
                dst[off + i++] = tick(m0, m3, c0);
                if (++c0 == 0L) ++c1;
                dst[off + i++] = tick(m2, m1, c1);
            }
        }
        for (; i < len; i++) dst[off + i] = nextLong();
    }

    @Override public void nextBytes(byte[] bytes) {
        int i = 0;
        for (int n = bytes.length - 7; i < n; i += 8) {
            long v = nextLong();
            bytes[i] = (byte) v; bytes[i + 1] = (byte) (v >>> 8); bytes[i + 2] = (byte) (v >>> 16); bytes[i + 3] = (byte) (v >>> 24); bytes[i + 4] = (byte) (v >>> 32); bytes[i + 5] = (byte) (v >>> 40); bytes[i + 6] = (byte) (v >>> 48); bytes[i + 7] = (byte) (v >>> 56);
        }
        if (i < bytes.length) {
            long v = nextLong();
            for (; i < bytes.length; i++) { bytes[i] = (byte) v; v >>>= 8; }
        }
    }

    public void leap() { seek(c0, c1 + 1L); }
    public double span() { return 0x1.0p64; }
    public Nyan384 copy() { Nyan384 r = new Nyan384(s0, s1, c0, c1, m0, m1, m2, m3); r.nSp = nSp; return r; }

    @Override public Nyan384 split() {
        long d = digest(PHI); nSp++;
        return new Nyan384(d, mix(d ^ c0 ^ nSp, K3, K6), 0L, 0L);
    }
    @Override public Nyan384 split(SplittableGenerator source) { return new Nyan384(source.nextLong(), source.nextLong()); }
    @SuppressWarnings("SameParameterValue") long digest(long dom) { return mix(dom ^ s0 ^ c0 ^ nSp ^ Long.rotateLeft(s1, 17) ^ Long.rotateLeft(c1, 43) ^ m0 ^ Long.rotateLeft(m1, 7) ^ Long.rotateLeft(m2, 31) ^ Long.rotateLeft(m3, 47), K1, K4); }
    @Override public Stream<SplittableGenerator> splits(long size) { return Stream.<SplittableGenerator>generate(this::split).limit(size); }
    @Override public Stream<SplittableGenerator> splits(SplittableGenerator source) { return Stream.generate(() -> split(source)); }
    @Override public Stream<SplittableGenerator> splits(long size, SplittableGenerator source) { return Stream.<SplittableGenerator>generate(() -> split(source)).limit(size); }
}