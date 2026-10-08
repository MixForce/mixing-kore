// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

import io.github.mixforce.mixingkore.internal.Platform;
import io.github.mixforce.mixingkore.internal.Simd;

public final class BitVec {
    private BitVec() {}

    private static final byte[] SEL8 = Platform.BW ? sel8() : null;
    static final long M2 = 0x5555555555555555L, M4 = 0x3333333333333333L, M8 = 0x0f0f0f0f0f0f0f0fL, H4 = 0x1111111111111111L;

    public static long msk(int w) { return w == 64 ? -1L : (1L << w) - 1L; }
    public static long msk(int o, int w) { return msk(w) << o; }
    public static long mlo(int i) { return (1L << i) - 1L; }
    public static long mhi(int i) { return -1L << i; }
    public static long mle(int t) { return -1L >>> -t; }

    public static long ext(long x, int o, int w) { return x >>> o & msk(w); }
    public static long exts(long x, int o, int w) { return ext(x, o, w) << 64 - w >> 64 - w; }
    public static long dep(long x, int o, int w, long v) { return x & ~msk(o, w) | ext(v, 0, w) << o; }
    public static long pext(long x, long m) { return Long.compress(x, m); }
    public static long pdep(long x, long m) { return Long.expand(x, m); }

    public static int pop(long x) { return Long.bitCount(x); }
    public static int pop(long[] v) { return pop(v, v.length); }
    public static int pop(long[] v, int n) { int s = 0; for (int i = 0; i < n; i++) s += Long.bitCount(v[i]); return s; }

    public static long pop2(long x) { return (x & M2) + (x >>> 1 & M2); }
    public static long pop4(long x) { x -= x >>> 1 & M2; return (x & M4) + (x >>> 2 & M4); }
    public static long zmsk2(long x) { return ~(x | x >>> 1 & M2) & M2; }
    public static long zmsk4(long x) { long y = x | x >>> 1; return ~(y | y >>> 2) & H4; }
    public static long bcast2(int s) { return M2 * s; }
    public static long bcast4(int s) { return H4 * s; }
    public static long eq2(long a, long b) { return zmsk2(a ^ b); }
    public static long eq4(long a, long b) { return zmsk4(a ^ b); }
    public static long eq2(long x, int s) { return zmsk2(x ^ bcast2(s)); }
    public static long eq4(long x, int s) { return zmsk4(x ^ bcast4(s)); }
    public static int cnt2(long x, int s) { return Long.bitCount(eq2(x, s)); }
    public static int cnt4(long x, int s) { return Long.bitCount(eq4(x, s)); }

    public static int cnt2(long[] v, int off, int n, int s) {
        long pat = bcast2(s);
        int c = 0, k = 0;
        while (k < n) {
            int p = off + k, w = p >>> 5, sh = (p & 31) << 1;
            int m = Math.min(32 - (p & 31), n - k);
            c += Long.bitCount(zmsk2(v[w] >>> sh ^ pat) & msk(m << 1));
            k += m;
        }
        return c;
    }

    public static int r2(long[] v, int i) { return (int) (v[i >>> 5] >>> (i & 31) * 2) & 3; }
    public static void w2(long[] v, int i, int a) {
        int sh = (i & 31) << 1, k = i >>> 5;
        v[k] = v[k] & ~(3L << sh) | (long) (a & 3) << sh;
    }
    public static int r4(long[] v, int i) { return (int) (v[i >>> 4] >>> (i & 15) * 4) & 15; }
    public static void w4(long[] v, int i, int a) {
        int sh = (i & 15) << 2, k = i >>> 4;
        v[k] = v[k] & ~(15L << sh) | (long) (a & 15) << sh;
    }

    public static long extSpan(long[] v, int b, int w) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) return ext(v[i], o, w);
        return v[i] >>> o | ext(v[i + 1], 0, o + w - 64) << 64 - o;
    }
    public static void depSpan(long[] v, int b, int w, long val) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) { v[i] = dep(v[i], o, w, val); return; }
        int n = 64 - o;
        v[i] = dep(v[i], o, n, val);
        v[i + 1] = dep(v[i + 1], 0, w - n, val >>> n);
    }
    public static void setSpan(long[] v, int b, int w) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) { v[i] |= msk(o, w); return; }
        v[i] |= mhi(o); v[i + 1] |= msk(w - (64 - o));
    }
    public static void clrSpan(long[] v, int b, int w) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) { v[i] &= ~msk(o, w); return; }
        v[i] &= ~mhi(o); v[i + 1] &= ~msk(w - (64 - o));
    }
    public static void xorSpan(long[] v, int b, int w) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) { v[i] ^= msk(o, w); return; }
        v[i] ^= mhi(o); v[i + 1] ^= msk(w - (64 - o));
    }
    public static int cntSpan(long[] v, int b, int w) {
        int i = b >>> 6, o = b & 63;
        if (o + w <= 64) return Long.bitCount(v[i] >>> o & msk(w));
        return Long.bitCount(v[i] >>> o) + Long.bitCount(v[i + 1] & msk(w - (64 - o)));
    }

    public static boolean tst(long x, int i) { return (x >>> i & 1L) != 0L; }
    public static long set(long x, int i) { return x | 1L << i; }
    public static long clr(long x, int i) { return x & ~(1L << i); }
    public static long xor(long x, int i) { return x ^ 1L << i; }
    public static boolean tst(long[] v, int i) { return (v[i >>> 6] >>> i & 1L) != 0L; }
    public static void set(long[] v, int i) { v[i >>> 6] |= 1L << i; }
    public static void clr(long[] v, int i) { v[i >>> 6] &= ~(1L << i); }
    public static void xor(long[] v, int i) { v[i >>> 6] ^= 1L << i; }

    public static int next1(long[] v, int from) {
        if (from < 0) from = 0;
        int i = from >>> 6;
        if (i >= v.length) return -1;
        long x = v[i] & mhi(from);
        while (x == 0L) { if (++i == v.length) return -1; x = v[i]; }
        return (i << 6) + Long.numberOfTrailingZeros(x);
    }
    public static int prev1(long[] v, int from) {
        int n = v.length << 6;
        if (from < 0 || n == 0) return -1;
        if (from >= n) from = n - 1;
        int i = from >>> 6;
        long x = v[i] & -1L >>> 63 - from;
        while (x == 0L) { if (--i < 0) return -1; x = v[i]; }
        return (i << 6) | 63 - Long.numberOfLeadingZeros(x);
    }

    public static void setRng(long[] v, int f, int t) {
        if (f >= t) return;
        int i = f >>> 6, j = t - 1 >>> 6;
        if (i == j) { v[i] |= msk(f & 63, t - f); return; }
        v[i] |= mhi(f);
        for (int k = i + 1; k < j; k++) v[k] = -1L;
        v[j] |= mle(t);
    }
    public static void clrRng(long[] v, int f, int t) {
        if (f >= t) return;
        int i = f >>> 6, j = t - 1 >>> 6;
        if (i == j) { v[i] &= ~msk(f & 63, t - f); return; }
        v[i] &= ~mhi(f);
        for (int k = i + 1; k < j; k++) v[k] = 0L;
        v[j] &= ~mle(t);
    }
    public static void xorRng(long[] v, int f, int t) {
        if (f >= t) return;
        int i = f >>> 6, j = t - 1 >>> 6;
        if (i == j) { v[i] ^= msk(f & 63, t - f); return; }
        v[i] ^= mhi(f);
        for (int k = i + 1; k < j; k++) v[k] ^= -1L;
        v[j] ^= mle(t);
    }
    public static int cntRng(long[] v, int f, int t) {
        if (f >= t) return 0;
        int i = f >>> 6, j = t - 1 >>> 6;
        if (i == j) return Long.bitCount(v[i] >>> f & msk(t - f));
        int c = Long.bitCount(v[i] >>> f);
        for (int k = i + 1; k < j; k++) c += Long.bitCount(v[k]);
        return c + Long.bitCount(v[j] & mle(t));
    }

    private static byte[] sel8() {
        byte[] t = new byte[2048];
        for (int b = 0; b < 256; b++) {
            int x = b, p = 0;
            do { t[b << 3 | p] = (byte) Integer.numberOfTrailingZeros(x); x &= x - 1; } while (++p < 8);
        }
        return t;
    }
    public static int sel(long x, int r) {
        if (!Platform.BW) return Long.numberOfTrailingZeros(Long.expand(1L << r, x));
        long c = x - (x >>> 1 & M2);
        c = (c & M4) + (c >>> 2 & M4);
        c = (c + (c >>> 4)) & M8;
        c *= 0x0101010101010101L;
        int b = Long.numberOfTrailingZeros((c << 1) + (126 - 2 * r) * 0x0101010101010101L & 0x8080808080808080L) >>> 3;
        int rr = r - (int) (c << 8 >>> (b << 3) & 0xFF);
        return (b << 3) + (SEL8[(int) (x >>> (b << 3) & 0xFF) << 3 | rr] & 0xFF);
    }

    public static int wordsFor(int bits) { return bits + 63 >>> 6; }

    public static long add128(long lo, long hi, long lo2, long hi2, long[] out) {
        long s = lo + lo2;
        long c = Long.compareUnsigned(s, lo) < 0 ? 1L : 0L;
        out[0] = s; return out[1] = hi + hi2 + c;
    }
    public static long sub128(long lo, long hi, long lo2, long hi2, long[] out) {
        long d = lo - lo2;
        long b = Long.compareUnsigned(lo, lo2) < 0 ? 1L : 0L;
        out[0] = d; return out[1] = hi - hi2 - b;
    }
    public static int cmp128(long loA, long hiA, long loB, long hiB) {
        int c = Long.compareUnsigned(hiA, hiB);
        return c != 0 ? c : Long.compareUnsigned(loA, loB);
    }
    public static boolean eq128(long a, long b, long c, long d) { return a == c && b == d; }
    public static void shl128(long lo, long hi, int n, long[] out) {
        if (n >= 128) { out[0] = 0L; out[1] = 0L; return; }
        if (n == 0) { out[0] = lo; out[1] = hi; return; }
        if (n >= 64) { out[0] = 0L; out[1] = lo << n - 64; return; }
        out[0] = lo << n;
        out[1] = hi << n | lo >>> 64 - n;
    }
    public static void shr128(long lo, long hi, int n, long[] out) {
        if (n >= 128) { out[0] = 0L; out[1] = 0L; return; }
        if (n == 0) { out[0] = lo; out[1] = hi; return; }
        if (n >= 64) { out[0] = hi >>> n - 64; out[1] = 0L; return; }
        out[0] = lo >>> n | hi << 64 - n;
        out[1] = hi >>> n;
    }
    public static void sra128(long lo, long hi, int n, long[] out) {
        if (n >= 128) { out[0] = hi >> 63; out[1] = hi >> 63; return; }
        if (n == 0) { out[0] = lo; out[1] = hi; return; }
        if (n >= 64) { out[0] = hi >> n - 64; out[1] = hi >> 63; return; }
        out[0] = lo >>> n | hi << 64 - n;
        out[1] = hi >> n;
    }
    public static long pop128(long lo, long hi) { return Long.bitCount(lo) + Long.bitCount(hi); }
    public static long ext128(long lo, long hi, int o, int w) {
        if (o + w <= 64) return ext(lo, o, w);
        if (o >= 64) return ext(hi, o - 64, w);
        return lo >>> o | ext(hi, 0, w - (64 - o)) << 64 - o;
    }
    public static void dep128(long lo, long hi, int o, int w, long v, long[] out) {
        if (o + w <= 64) { out[0] = dep(lo, o, w, v); out[1] = hi; return; }
        if (o >= 64) { out[0] = lo; out[1] = dep(hi, o - 64, w, v); return; }
        int n = 64 - o;
        out[0] = dep(lo, o, n, v);
        out[1] = dep(hi, 0, w - n, v >>> n);
    }
    public static long mul128(long a, long b, long[] out) {
        out[0] = a * b; return out[1] = Math.unsignedMultiplyHigh(a, b);
    }
    private static void mixr(long[] o, long k) {
        long l = o[0], h = o[1];
        l ^= l >>> 33 | h << 31;
        h >>>= 33;
        o[0] = l * k; o[1] = h * k + Math.unsignedMultiplyHigh(l, k);
    }
    public static void mix128(long lo, long hi, long[] out) {
        out[0] = lo; out[1] = hi;
        mixr(out, 0xff51afd7ed558ccdL);
        mixr(out, 0xc4ceb9fe1a85ec53L);
        long l = out[0], h = out[1];
        out[0] = l ^ (l >>> 33 | h << 31);
        out[1] = h ^ (h >>> 33);
    }
    public static long mix64(long h) {
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33; return h;
    }

    public static int vecPop(long[] a, int n) { return Platform.VECTOR_OK ? Simd.pop(a, n) : pop(a, n); }

    public static final class Rank {
        static final int WPB = 7, BPS = 8, SH = 3;
        final long[] a;
        final int[] spr;
        final int nb, n1;
        public Rank(long[] bits) {
            int nw = bits.length;
            nb = (nw + WPB - 1) / WPB;
            a = new long[nb << 3];
            long c = 0;
            for (int b = 0, w = 0; b < nb; b++) {
                a[b << 3] = c;
                for (int j = 1; j <= WPB && w < nw; j++) c += Long.bitCount(a[b << 3 | j] = bits[w++]);
            }
            n1 = (int) c;
            spr = new int[nb + BPS - 1 >>> SH];
            for (int s = 0; s < spr.length; s++) spr[s] = (int) a[(s << SH) << 3];
        }
        public int rnk(int i) {
            int w = i >>> 6, b = w / WPB, e = (b << 3) + 1 + w - b * WPB, c = (int) a[b << 3];
            for (int j = (b << 3) + 1; j < e; j++) c += Long.bitCount(a[j]);
            return c + Long.bitCount(a[e] & mlo(i & 63));
        }
        public int sel(int k) {
            if (k < 0 || k >= n1) return -1;
            int lo = 0, hi = spr.length - 1;
            while (lo < hi) { int m = lo + hi + 1 >>> 1; if (spr[m] <= k) lo = m; else hi = m - 1; }
            int b = lo << SH, e = Math.min(b + BPS, nb);
            while (b + 1 < e && (int) a[(b + 1) << 3] <= k) b++;
            int c = (int) a[b << 3], j = (b << 3) + 1, w = b * WPB;
            for (int d = 0; d < WPB; d++, j++, w++) {
                int p = Long.bitCount(a[j]);
                if (c + p > k) return w << 6 | Long.numberOfTrailingZeros(Long.expand(1L << k - c, a[j]));
                c += p;
            }
            return -1;
        }
    }
}