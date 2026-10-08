// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

import io.github.mixforce.mixingkore.internal.Platform;
import io.github.mixforce.mixingkore.internal.Simd;

public final class FastMath {
    private FastMath() {}

    private static final float LOG2E = 0x1.715476p0f, LN2H = 0.693359375f, LN2L = -2.1219444e-4f;
    private static final float E0 = 0x1.ffffe8p-1f, E1 = 0x1.fffb34p-1f, E2 = 0x1.00059cp-1f, E3 = 0x1.57e0b8p-3f, E4 = 0x1.53ac16p-5f;
    private static final double L0 = 0x1.ffffde26b37dfp-1, L1 = -0x1.000503520ee0ap-1, L2 = 0x1.5573f04474984p-2, L3 = -0x1.fbd1cf4fa2b61p-3, L4 = 0x1.92a9ee27f1147p-3, L5 = -0x1.93833d1aed72ap-3, L6 = 0x1.6cf52ce7d53dap-3, LNN = 0x1.62e42fefa39efp-1;
    private static final double HALF2PI = 0x1.45f306dc9c883p-1;
    private static final float LN2C = 0x1.62e42fep-1f;
    private static final float XE1 = 0.2402265071868896484375f, XE2 = 0.055502593517303466796875f, XE3 = 0.009617992676794528961181640625f;
    private static final float Q0 = 0x1.27616c9496e0bp-2f, Q1 = -0x1.71969a075c67ap-2f, Q2 = 0x1.ec70a6ca7baddp-2f, Q3 = -0x1.7154748bef6c8p-1f, Q4 = 0x1.71547652ab82bp0f;
    private static final float[] WI = {0x1.661ec79f8f3bep0f, 0x1.571ed4aaf883dp0f, 0x1.49539f0f010bp0f, 0x1.3c995b0b80385p0f, 0x1.30d190c8864a5p0f, 0x1.25e227b0b8eap0f, 0x1.1bb4a4a1a343fp0f, 0x1.12358f08ae5bap0f, 0x1.0953f419900a7p0f, 1f, 0x1.e608cfd9a47acp-1f, 0x1.ca4b31f026aap-1f, 0x1.b2036576afce6p-1f, 0x1.9c2d163a1aa2dp-1f, 0x1.886e6037841edp-1f, 0x1.767dcf5534862p-1f};
    private static final float[] WC = {-0x1.efec65b963019p-2f, -0x1.b0b6832d4fca4p-2f, -0x1.7418b0a1fb77bp-2f, -0x1.39de91a6dcf7bp-2f, -0x1.01d9bf3f2b631p-2f, -0x1.97c1d1b3b7afp-3f, -0x1.2f9e393af3c9fp-3f, -0x1.960cbbf788d5cp-4f, -0x1.a6f9db6475fcep-5f, 0f, 0x1.338ca9f24f53dp-4f, 0x1.476a9543891bap-3f, 0x1.e840b4ac4e4d2p-3f, 0x1.40645f0c6651cp-2f, 0x1.88e9c2c1b9ff8p-2f, 0x1.ce0a44eb17bccp-2f};
    private static final int[] TI = {0, 371395, 759234, 1164243, 1587184, 2028850, 2490071, 2971711, 3474675, 3999908, 4548394, 5121164, 5719293, 6343903, 6996167, 7677309};
    private static final float HPIA = 0x1.921fb6p0f, HPIB = -4.371139e-8f;
    private static final float S1 = -0x1.555544p-3f, S2 = 0x1.1106e6p-7f, S3 = -0x1.992cf8p-13f;
    private static final float D1 = 0x1.55554ap-5f, D2 = -0x1.6c0c2ap-10f, D3 = 0x1.99e914p-16f;
    private static final float T0 = -0x1.5554dcp-2f, T1 = 0x1.9978ecp-3f, T2 = -0x1.230a94p-3f, T3 = 0x1.b4debp-4f, T4 = -0x1.3550dap-4f, T5 = 0x1.61eebp-5f, T6 = -0x1.0c17d4p-6f, T7 = 0x1.7ea694p-9f;
    private static final float HPF = 0x1.921fb6p0f, PIF = 0x1.921fb6p1f;
    private static final float TH0 = 0.5000002384185791015625f, TH1 = 0.16666519641876220703125f, TH2 = 0.0416606962680816650390625f, TH3 = 0.008368813432753086090087890625f, TH4 = 0.001425857655704021453857421875f;
    private static final float U0 = 0x1.5554b8p-3f, U1 = 0x1.33c82ep-4f, U2 = 0x1.57dcdcp-5f, U3 = 0x1.76a24p-5f;
    private static final int VEC_MIN_N = 64;

    public static float exp(float x) {
        if (!(x <= 88.37f & x >= -87.33f)) return x != x ? x : x > 88.37f ? Float.POSITIVE_INFINITY : 0.0f;
        int k = (int) Math.fma(x, LOG2E, 12582912f) - 12582912;
        float r = Math.fma(-k, LN2L, Math.fma(-k, LN2H, x));
        float p = Math.fma(r, Math.fma(r, Math.fma(r, Math.fma(r, E4, E3), E2), E1), E0);
        return p * Float.intBitsToFloat((k + 127) << 23);
    }

    public static float log(float x) {
        int b = Float.floatToRawIntBits(x);
        if (b >= 0x7f800000) return b == 0x7f800000 ? Float.POSITIVE_INFINITY : Float.NaN;
        if (b <= 0) return (b & 0x7fffffff) == 0 ? Float.NEGATIVE_INFINITY : Float.NaN;
        int fs = 0;
        if ((b & 0x7f800000) == 0) { x *= 0x1p23f; b = Float.floatToRawIntBits(x); fs = 23; }
        int e = (b >> 23) - 127 - fs;
        float m = Float.intBitsToFloat((b & 0x7fffff) | 0x3f800000);
        boolean h = m >= 0x1.555556p0f;
        if (h) m *= 0.5f;
        e = h ? e + 1 : e;
        double f = (double) m - 1.0, f2 = f * f;
        double lo = Math.fma(Math.fma(L3, f, L2), f2, Math.fma(L1, f, L0));
        double hi = Math.fma(Math.fma(L6, f, L5), f, L4);
        double q = Math.fma(hi, f2 * f2, lo);
        return (float) Math.fma(q, f, e * LNN);
    }

    private static float log2p(int b) {
        int t = b - 0x3f330000;
        int i = (t >>> 19) & 15;
        float r = Math.fma(Float.intBitsToFloat(b - (t & 0xff800000)), WI[i], -1f);
        float h = Math.fma(r, Q0, Q1);
        float p = Math.fma(r, Q2, Q3);
        float r2 = r * r;
        return Math.fma(h, r2 * r2, Math.fma(p, r2, Math.fma(Q4, r, WC[i] + (t >> 23))));
    }

    private static float exp2p(float d) {
        int k = (int) Math.fma(d, 16f, 12582912f) - 12582912;
        float r = Math.fma(k, -0.0625f, d);
        float h = Math.fma(r, XE3, XE2);
        h = Math.fma(r, h, XE1);
        h = Math.fma(r, h, LN2C);
        return Math.fma(r, h, 1f) * Float.intBitsToFloat((((k >> 4) + 127) << 23) + TI[k & 15]);
    }

    @Deprecated
    public static float pow(float x, float y) {
        if (y == 0f) return 1f;
        if (y == 1f) return x;
        if (x == 1f) return (y - y) == 0f ? 1f : Float.NaN;
        if (y == 2f) return x * x;
        if (y == 3f) return x * x * x;
        if (y == 0.5f) return (float) Math.sqrt(x);
        if (x != x | y != y) return Float.NaN;
        if (!(x < Float.POSITIVE_INFINITY & x > Float.NEGATIVE_INFINITY)) return y > 0f ? Float.POSITIVE_INFINITY : 0f;
        if (x == 0f) return y > 0f ? 0f : Float.POSITIVE_INFINITY;
        boolean neg = false;
        if (x < 0f) {
            float yi = (float) (int) y;
            if (yi != y) return Float.NaN;
            neg = ((int) yi & 1) != 0;
            x = -x;
        }
        int b = Float.floatToRawIntBits(x);
        if (b < 0x00800000) b = Float.floatToRawIntBits(x * 0x1p23f) - 0x0b800000;
        float yl = y * log2p(b);
        if (yl > 127.9375f) return Float.MAX_VALUE;
        if (yl < -126f) return 0f;
        float v = exp2p(yl);
        return neg ? -v : v;
    }

    public static void sinCos(float y, float[] o) {
        if (!(Math.abs(y) < 1.0e8f)) { o[0] = AccurateMath.sin(y); o[1] = AccurateMath.cos(y); return; }
        int n = (int) Math.rint(y * HALF2PI);
        float r = Math.fma(-n, HPIB, Math.fma(-n, HPIA, y));
        float r2 = r * r;
        float s = Math.fma(r * r2, Math.fma(r2, Math.fma(r2, S3, S2), S1), r);
        float c = Math.fma(r2, Math.fma(r2, Math.fma(r2, Math.fma(r2, D3, D2), D1), -0.5f), 1f);
        int q = n & 3;
        float sv = (q & 1) == 0 ? s : c, cv = (q & 1) == 0 ? c : s;
        o[0] = (q & 2) == 0 ? sv : -sv;
        o[1] = ((q + 1) & 2) == 0 ? cv : -cv;
    }

    public static float sin(float y) {
        if (!(Math.abs(y) < 1.0e8f)) return AccurateMath.sin(y);
        int n = (int) Math.rint(y * HALF2PI);
        float r = Math.fma(-n, HPIB, Math.fma(-n, HPIA, y));
        float r2 = r * r;
        int q = n & 3;
        float v = (q & 1) == 0 ? Math.fma(r * r2, Math.fma(r2, Math.fma(r2, S3, S2), S1), r) : Math.fma(r2, Math.fma(r2, Math.fma(r2, Math.fma(r2, D3, D2), D1), -0.5f), 1f);
        return (q & 2) == 0 ? v : -v;
    }

    public static float cos(float y) {
        if (!(Math.abs(y) < 1.0e8f)) return AccurateMath.cos(y);
        int n = (int) Math.rint(y * HALF2PI);
        float r = Math.fma(-n, HPIB, Math.fma(-n, HPIA, y));
        float r2 = r * r;
        int q = n & 3;
        float v = (q & 1) == 0 ? Math.fma(r2, Math.fma(r2, Math.fma(r2, Math.fma(r2, D3, D2), D1), -0.5f), 1f) : Math.fma(r * r2, Math.fma(r2, Math.fma(r2, S3, S2), S1), r);
        return ((q + 1) & 2) == 0 ? v : -v;
    }

    public static float asin(float a) {
        float ax = a < 0 ? -a : a;
        boolean hi = ax > 0.5f;
        float t = hi ? Math.fma(-0.5f, ax, 0.5f) : ax * ax;
        float y = hi ? (float) Math.sqrt(t) : ax;
        float r = Math.fma(t * y, Math.fma(t * t, Math.fma(t, U3, U2), Math.fma(t, U1, U0)), y);
        float v = hi ? Math.fma(-2f, r, HPF) : r;
        return a < 0 ? -v : v;
    }

    public static float acos(float a) {
        float ax = a < 0 ? -a : a;
        boolean hi = ax > 0.5f;
        float t = hi ? Math.fma(-0.5f, ax, 0.5f) : ax * ax;
        float y = hi ? (float) Math.sqrt(t) : ax;
        float r = Math.fma(t * y, Math.fma(t * t, Math.fma(t, U3, U2), Math.fma(t, U1, U0)), y);
        float v;
        if (hi) v = a < 0 ? Math.fma(-2f, r, PIF) : 2f * r;
        else v = HPF - (a < 0 ? -r : r);
        return v;
    }

    public static float atan(float a) {
        float ax = a < 0 ? -a : a;
        boolean red = ax > 1f;
        float z2, mul, add;
        if (!red) { z2 = a * a; mul = a * z2; add = a; }
        else { float z = 1f / ax; z2 = z * z; mul = z * z2; add = z; }
        float u = z2 * z2;
        float q0 = Math.fma(z2, T1, T0), q1 = Math.fma(z2, T3, T2), q2 = Math.fma(z2, T5, T4), q3 = Math.fma(z2, T7, T6);
        float p = Math.fma(u * u, Math.fma(u, q3, q2), Math.fma(u, q1, q0));
        float r = Math.fma(mul, p, add);
        if (red) r = HPF - r;
        return red ? (a < 0 ? -r : r) : r;
    }

    public static float atan2(float y, float x) {
        int bx = Float.floatToRawIntBits(x), by = Float.floatToRawIntBits(y);
        int ax = bx & 0x7fffffff, ay = by & 0x7fffffff;
        if (ax > 0x7f800000 | ay > 0x7f800000) return Float.NaN;
        boolean xs = bx < 0, ys = by < 0;
        if ((ax - 0x7f800000 | ay - 0x7f800000) >= 0) {
            if (ax != 0x7f800000) return ys ? -HPF : HPF;
            if (ay != 0x7f800000) return xs ? (ys ? -PIF : PIF) : (ys ? -0.0f : 0.0f);
            float q = xs ? HPF * 1.5f : HPF * 0.5f;
            return ys ? -q : q;
        }
        if ((ax - 1 | ay - 1) < 0) {
            if (ay != 0) return ys ? -HPF : HPF;
            return xs ? (ys ? -PIF : PIF) : (ys ? -0.0f : 0.0f);
        }
        float u = Float.intBitsToFloat(ax), v = Float.intBitsToFloat(ay);
        boolean sw = v > u;
        float z = sw ? -u / v : v / u;
        float s = (bx < 0 ? -1f : 0f) + (sw ? 0.5f : 0f);
        float z2 = z * z, w = z2 * z2;
        float a = Math.fma(z2, T1, T0), b = Math.fma(z2, T3, T2), c = Math.fma(z2, T5, T4), d = Math.fma(z2, T7, T6);
        float p = Math.fma(w * w, Math.fma(w, d, c), Math.fma(w, b, a));
        float r = Math.fma(z * z2, p, Math.fma(s, PIF, z));
        return (by ^ bx) < 0 ? -r : r;
    }

    private static float expm1Core(float x) {
        int k = (int) Math.fma(x, LOG2E, 12582912f) - 12582912;
        float f = Math.fma(-k, LN2L, Math.fma(-k, LN2H, x));
        float f2 = f * f;
        float p = Math.fma(f, TH4, TH3);
        p = Math.fma(f, p, TH2);
        p = Math.fma(f, p, TH1);
        p = Math.fma(f, p, TH0);
        float q = Math.fma(f2, p, f);
        float s = Float.intBitsToFloat((k + 127) << 23);
        return Math.fma(q, s, s - 1f);
    }

    public static float tanh(float x) {
        if (!(x > -0x1.205966p3f & x < 0x1.205966p3f)) return x != x ? x : x < 0f ? -1f : 1f;
        if (x == 0f) return x;
        float q = expm1Core(x + x);
        return q / (q + 2f);
    }

    public static float sigmoid(float x) {
        if (!(x < 17.3287f & x > -87.33f)) return x != x ? x : x > 0f ? 1f : 0f;
        if (x < 0f) { float e = exp(x); return e / (1f + e); }
        float q = expm1Core(x);
        return (q + 1f) / (q + 2f);
    }

    public static void softmax(float[] x, float[] o, int n) {
        if (n >= VEC_MIN_N && Platform.VECTOR_OK) Simd.softmax(x, o, n);
        else softmaxScalar(x, o, n);
    }

    private static void softmaxScalar(float[] x, float[] o, int n) {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) if (x[i] > m) m = x[i];
        float s = 0f;
        for (int i = 0; i < n; i++) s += o[i] = exp(x[i] - m);
        float inv = 1f / s;
        for (int i = 0; i < n; i++) o[i] *= inv;
    }
}