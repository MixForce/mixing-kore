// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce & Arm (1999-2026)">
/*
 * SPDX-License-Identifier: LGPL-3.0-or-later
 * Copyright (C) 2026 MixForce
 * Modified from Arm's optimized-routines.
 * Original Copyright (c) 1999-2026, Arm Limited.
 */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

public final class AccurateMath {
    private AccurateMath() {}

    private static final long[] ET = {0x3ff0000000000000L, 0x3fefd9b0d3158574L, 0x3fefb5586cf9890fL, 0x3fef9301d0125b51L, 0x3fef72b83c7d517bL, 0x3fef54873168b9aaL, 0x3fef387a6e756238L, 0x3fef1e9df51fdee1L, 0x3fef06fe0a31b715L, 0x3feef1a7373aa9cbL, 0x3feedea64c123422L, 0x3feece086061892dL, 0x3feebfdad5362a27L, 0x3feeb42b569d4f82L, 0x3feeab07dd485429L, 0x3feea47eb03a5585L, 0x3feea09e667f3bcdL, 0x3fee9f75e8ec5f74L, 0x3feea11473eb0187L, 0x3feea589994cce13L, 0x3feeace5422aa0dbL, 0x3feeb737b0cdc5e5L, 0x3feec49182a3f090L, 0x3feed503b23e255dL, 0x3feee89f995ad3adL, 0x3feeff76f2fb5e47L, 0x3fef199bdd85529cL, 0x3fef3720dcef9069L, 0x3fef5818dcfba487L, 0x3fef7c97337b9b5fL, 0x3fefa4afa2a490daL, 0x3fefd0765b6e4540L};
    private static final double ILN2 = 0x1.71547652b82fep5, MS = 0x1.8p52;
    private static final double E0 = 0x1.c6af84b912394p-20, E1 = 0x1.ebfce50fac4f3p-13, E2 = 0x1.62e42ff0c52d6p-6;
    private static final double[] IC = {0x1.661ec79f8f3bep0, 0x1.571ed4aaf883dp0, 0x1.49539f0f010bp0, 0x1.3c995b0b80385p0, 0x1.30d190c8864a5p0, 0x1.25e227b0b8eap0, 0x1.1bb4a4a1a343fp0, 0x1.12358f08ae5bap0, 0x1.0953f419900a7p0, 0x1p0, 0x1.e608cfd9a47acp-1, 0x1.ca4b31f026aap-1, 0x1.b2036576afce6p-1, 0x1.9c2d163a1aa2dp-1, 0x1.886e6037841edp-1, 0x1.767dcf5534862p-1};
    private static final double[] LC = {-0x1.57bf7808caadep-2, -0x1.2bef0a7c06ddbp-2, -0x1.01eae7f513a67p-2, -0x1.b31d8a68224e9p-3, -0x1.6574f0ac07758p-3, -0x1.1aa2bc79c81p-3, -0x1.a4e76ce8c0e5ep-4, -0x1.1973c5a611cccp-4, -0x1.252f438e10c1ep-5, 0.0, 0x1.aa5aa5df25984p-5, 0x1.c5e53aa362eb4p-4, 0x1.526e57720db08p-3, 0x1.bc2860d22477p-3, 0x1.1058bc8a07ee1p-2, 0x1.4043057b6ee09p-2};
    private static final double LN2 = 0x1.62e42fefa39efp-1;
    private static final double P0 = -0x1.00ea348b88334p-2, P1 = 0x1.5575b0be00b6ap-2, P2 = -0x1.ffffef20a4123p-2;
    private static final double[] WI = {0x1.661ec79f8f3bep0, 0x1.571ed4aaf883dp0, 0x1.49539f0f010bp0, 0x1.3c995b0b80385p0, 0x1.30d190c8864a5p0, 0x1.25e227b0b8eap0, 0x1.1bb4a4a1a343fp0, 0x1.12358f08ae5bap0, 0x1.0953f419900a7p0, 0x1p0, 0x1.e608cfd9a47acp-1, 0x1.ca4b31f026aap-1, 0x1.b2036576afce6p-1, 0x1.9c2d163a1aa2dp-1, 0x1.886e6037841edp-1, 0x1.767dcf5534862p-1};
    private static final double[] WC = {-0x1.efec65b963019p-2, -0x1.b0b6832d4fca4p-2, -0x1.7418b0a1fb77bp-2, -0x1.39de91a6dcf7bp-2, -0x1.01d9bf3f2b631p-2, -0x1.97c1d1b3b7afp-3, -0x1.2f9e393af3c9fp-3, -0x1.960cbbf788d5cp-4, -0x1.a6f9db6475fcep-5, 0.0, 0x1.338ca9f24f53dp-4, 0x1.476a9543891bap-3, 0x1.e840b4ac4e4d2p-3, 0x1.40645f0c6651cp-2, 0x1.88e9c2c1b9ff8p-2, 0x1.ce0a44eb17bccp-2};
    private static final double Q0 = 0x1.27616c9496e0bp-2, Q1 = -0x1.71969a075c67ap-2, Q2 = 0x1.ec70a6ca7baddp-2, Q3 = -0x1.7154748bef6c8p-1, Q4 = 0x1.71547652ab82bp0;
    private static final double X0 = 0x1.c6af84b912394p-5, X1 = 0x1.ebfce50fac4f3p-3, X2 = 0x1.62e42ff0c52d6p-1;
    private static final double HPI = 0x1.921fb54442d18p0, HINV = 0x1.45f306dc9c883p23, P63 = 0x1.921fb54442d18p-62, PD = 3.141592653589793;
    private static final double S1 = -0x1.555545995a603p-3, S2 = 0x1.1107605230bc4p-7, S3 = -0x1.994eb3774cf24p-13;
    private static final double C1 = -0x1.ffffffd0c621cp-2, C2 = 0x1.55553e1068f19p-5, C3 = -0x1.6c087e89a359dp-10, C4 = 0x1.99343027bf8c3p-16;
    private static final double[] SG = {1, -1, -1, 1};
    private static final int[] I4 = {0xa2, 0xa2f9, 0xa2f983, 0xa2f9836e, 0xf9836e4e, 0x836e4e44, 0x6e4e4415, 0x4e441529, 0x441529fc, 0x1529fc27, 0x29fc2757, 0xfc2757d1, 0x2757d1f5, 0x57d1f534, 0xd1f534dd, 0xf534ddc0, 0x34ddc0db, 0xddc0db62, 0xc0db6295, 0xdb629599, 0x6295993c, 0x95993c43, 0x993c4390, 0x3c439041};
    private static final double V1 = 0x1.ffffffeba5b28p-1, V3 = -0x1.555533fc6088fp-2, V5 = 0x1.9990825220db1p-3, V7 = -0x1.24159a96a47e3p-3, V9 = 0x1.c02f81f56a9a9p-4, V11 = -0x1.57228506134f6p-4, V13 = 0x1.d77781b4ac82ap-5, V15 = -0x1.f8b3d882abef5p-6, V17 = 0x1.5f80210256598p-7, V19 = -0x1.cbc8426f116f2p-10;
    private static final double T1 = 0x1.ffffff7fd46f8p-1, T3 = -0x1.5554e9f529b84p-2, T5 = 0x1.11021ccbd40b4p-3, T7 = -0x1.b87e8fcd8f97p-5, T9 = 0x1.5b172ff1b06b1p-6, T11 = -0x1.ec7d924988074p-8, T13 = 0x1.05a0f76ea6863p-9, T15 = -0x1.1ee8787dc4937p-12;
    private static final double HPIL = 0x1.1a62633145c07p-54;
    private static final double A0 = 0x1.5555555555555p-3, A1 = -0x1.4d61203eb6f7dp-2, A2 = 0x1.9c1550e884455p-3, A3 = -0x1.48228b5688f3bp-5, A4 = 0x1.9efe07501b288p-11, A5 = 0x1.23de10dfdf709p-15;
    private static final double R1 = -0x1.33a271c8a2d4bp1, R2 = 0x1.02ae59c598ac8p1, R3 = -0x1.6066c1b8d0159p-1, R4 = 0x1.3b8c5b12e9282p-4;

    private static double expCore(double xd) {
        double z = xd * ILN2;
        long ki = Double.doubleToRawLongBits(z + MS);
        double kd = Double.longBitsToDouble(ki) - MS;
        double r = z - kd, r2 = r * r;
        double s = Double.longBitsToDouble(ET[(int) (ki & 31)] + (ki << 47));
        return ((E0 * r + E1) * r2 + (E2 * r + 1.0)) * s;
    }

    public static float exp(float x) {
        int ix = Float.floatToRawIntBits(x);
        int a = (ix >>> 20) & 0x7ff;
        if (a >= 0x42b) {
            if (ix == 0xff800000) return 0.0f;
            if (a >= 0x7ff) return x + x;
            if (x > 0x1.62e42ep6f) return Float.POSITIVE_INFINITY;
            if (x < -0x1.9fe36ep6f) return 0.0f;
        }
        return (float) expCore(x);
    }

    public static float log(float x) {
        int ix = Float.floatToRawIntBits(x);
        if (ix == 0x3f800000) return 0.0f;
        if (Integer.compareUnsigned(ix - 0x800000, 0x7f000000) >= 0) {
            if ((ix & 0x7fffffff) == 0) return Float.NEGATIVE_INFINITY;
            if (ix == 0x7f800000) return Float.POSITIVE_INFINITY;
            if (ix < 0 || (ix & 0x7fffffff) > 0x7f800000) return Float.NaN;
            ix = Float.floatToRawIntBits(x * 0x1p23f) - 0xb800000;
        }
        int t = ix - 0x3f330000;
        int i = (t >>> 19) & 15;
        double r = Float.intBitsToFloat(ix - (t & 0xff800000)) * IC[i] - 1.0;
        double y0 = LC[i] + (t >> 23) * LN2;
        double r2 = r * r;
        double p = P1 * r + P2;
        p = P0 * r2 + p;
        return (float) (p * r2 + (y0 + r));
    }

    public static float pow(float x, float y) {
        int ix = Float.floatToRawIntBits(x), iy = Float.floatToRawIntBits(y), sb = 0;
        if (Integer.compareUnsigned(ix - 0x800000, 0x7f000000) >= 0 || zinfnan(iy)) {
            if (zinfnan(iy)) {
                if ((iy << 1) == 0) return 1.0f;
                if ((iy & 0x7fffffff) != 0x7f800000) return Float.NaN;
                int ax = ix & 0x7fffffff;
                if (ax == 0x3f800000 || ax > 0x7f800000) return Float.NaN;
                return (ax < 0x3f800000) == (iy >= 0) ? 0.0f : Float.POSITIVE_INFINITY;
            }
            if (zinfnan(ix)) {
                float x2 = x * x;
                if (ix < 0 && ckint(iy) == 1) { x2 = -x2; sb = 0x10000; }
                return (iy & 0x80000000) != 0 ? 1.0f / x2 : x2;
            }
            if (ix < 0) {
                int c = ckint(iy);
                if (c == 0) return Float.NaN;
                if (c == 1) sb = 0x10000;
                ix &= 0x7fffffff;
            }
            if (Integer.compareUnsigned(ix, 0x800000) < 0) {
                ix = Float.floatToRawIntBits(x * 0x1p23f) & 0x7fffffff;
                ix -= 0xb800000;
            }
        }
        double yl = log2p(ix) * (double) y;
        if ((Double.doubleToRawLongBits(yl) >>> 47 & 0xffff) >= 0x80be) {
            if (yl <= -150.0) return sb != 0 ? -0.0f : 0.0f;
            if (yl > 0x1.fffffffd1d571p6) return sb != 0 ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
            if (yl > 0x1.fffffffa3aae2p6) return sb != 0 ? -0x1.fffffep127f : 0x1.fffffep127f;
        }
        return exp2p(yl, sb);
    }

    private static boolean zinfnan(int i) {
        return Integer.compareUnsigned((i << 1) - 1, 0xfeffffff) >= 0;
    }

    private static int ckint(int iy) {
        int e = (iy >>> 23) & 0xff;
        if (e < 127) return 0;
        if (e > 150) return 2;
        if ((iy & ((1 << (150 - e)) - 1)) != 0) return 0;
        return ((iy >>> (150 - e)) & 1) != 0 ? 1 : 2;
    }

    private static double log2p(int ix) {
        int t = ix - 0x3f330000, i = (t >>> 19) & 15;
        double r = Float.intBitsToFloat(ix - (t & 0xff800000)) * WI[i] - 1.0;
        double y = Q0 * r + Q1, p = Q2 * r + Q3;
        double q = Q4 * r + (WC[i] + (t >> 23));
        double r2 = r * r;
        return y * (r2 * r2) + (p * r2 + q);
    }

    private static float exp2p(double xd, int sb) {
        long ki = Double.doubleToRawLongBits(xd + 0x1.8p47);
        double kd = Double.longBitsToDouble(ki) - 0x1.8p47;
        double r = xd - kd, r2 = r * r;
        double s = Double.longBitsToDouble(ET[(int) (ki & 31)] + ((ki + sb) << 47));
        double y = (X0 * r + X1) * r2 + (X2 * r + 1.0);
        return (float) (y * s);
    }

    private static double sinp(double x, double x2) {
        double x3 = x * x2;
        return x + x3 * S1 + x3 * x2 * (S2 + x2 * S3);
    }

    private static double cosp(double x2, boolean neg) {
        double c = 1.0 + x2 * C1 + (x2 * x2) * C2;
        c += (x2 * x2 * x2) * (C3 + x2 * C4);
        return neg ? -c : c;
    }

    private static double rlarge(int xi, int[] q) {
        int i = (xi >>> 26) & 15, sh = (xi >>> 23) & 7;
        xi = ((xi & 0xffffff) | 0x800000) << sh;
        long m = xi & 0xffffffffL;
        long p1 = m * (I4[i + 4] & 0xffffffffL);
        long p2 = m * (I4[i + 8] & 0xffffffffL);
        long res = ((long) (xi * I4[i]) << 32) | (p2 >>> 32);
        res += p1;
        long n = (res + (1L << 61)) >>> 62;
        res -= n << 62;
        q[0] = (int) n;
        return (double) res * P63;
    }

    public static float sin(float y) {
        int iy = Float.floatToRawIntBits(y);
        int a = (iy >>> 20) & 0x7ff;
        if (a < 0x3f4) return a < 0x388 ? y : (float) sinp(y, (double) y * y);
        if (a >= 0x7f8) return Float.NaN;
        double x = y, x2;
        if (a < 0x42f) {
            int n = ((int) (x * HINV) + 0x800000) >> 24;
            x -= n * HPI;
            x2 = x * x;
            return (n & 1) == 0 ? (float) sinp(x * SG[n & 3], x2) : (float) cosp(x2, (n & 2) != 0);
        }
        int[] q = {0};
        x = rlarge(iy, q);
        int n = (q[0] + (iy >>> 31)) & 3;
        x2 = x * x;
        return (q[0] & 1) == 0 ? (float) sinp(x * SG[n], x2) : (float) cosp(x2, (n & 2) != 0);
    }

    public static float cos(float y) {
        int iy = Float.floatToRawIntBits(y);
        int a = (iy >>> 20) & 0x7ff;
        if (a < 0x3f4) return a < 0x388 ? 1.0f : (float) cosp((double) y * y, false);
        if (a >= 0x7f8) return Float.NaN;
        double x = y, x2;
        if (a < 0x42f) {
            int n = ((int) (x * HINV) + 0x800000) >> 24;
            x -= n * HPI;
            x2 = x * x;
            return ((n ^ 1) & 1) == 0 ? (float) sinp(x * SG[n & 3], x2) : (float) cosp(x2, (n & 2) != 0);
        }
        int[] q = {0};
        x = rlarge(iy, q);
        int n = (q[0] + (iy >>> 31)) & 3;
        x2 = x * x;
        return ((q[0] ^ 1) & 1) == 0 ? (float) sinp(x * SG[n], x2) : (float) cosp(x2, (n & 2) != 0);
    }

    public static void sinCos(float y, float[] o) {
        int iy = Float.floatToRawIntBits(y);
        int a = (iy >>> 20) & 0x7ff;
        if (a >= 0x3f4 && a < 0x7f8) {
            double x, x2;
            int q0, n;
            if (a < 0x42f) {
                n = ((int) ((double) y * HINV) + 0x800000) >> 24;
                x = y - n * HPI;
                q0 = n;
            } else {
                int[] qb = {0};
                x = rlarge(iy, qb);
                q0 = qb[0];
                n = (q0 + (iy >>> 31)) & 3;
            }
            x2 = x * x;
            double p = sinp(x * SG[n & 3], x2), q = cosp(x2, (n & 2) != 0);
            boolean odd = (q0 & 1) != 0;
            o[0] = (float) (odd ? q : p);
            o[1] = (float) (odd ? p : q);
        } else {
            o[0] = sin(y);
            o[1] = cos(y);
        }
    }

    private static double asinCore(double z) {
        double p = (((((A5 * z + A4) * z + A3) * z + A2) * z + A1) * z + A0);
        double q = ((((R4 * z + R3) * z + R2) * z + R1) * z + 1.0);
        return z * p / q;
    }

    public static float asin(float a) {
        double x = a;
        int ix = Float.floatToRawIntBits(a) & 0x7fffffff;
        if (ix >= 0x3f800000) {
            if (a == 1.0f) return 0x1.921fb6p0f;
            if (a == -1.0f) return -0x1.921fb6p0f;
            return Float.NaN;
        }
        double r;
        if (ix < 0x3f000000) {
            r = (ix < 0x31800000) ? x : x + x * asinCore(x * x);
            return (float) r;
        }
        double z = (1.0 - Math.abs(x)) * 0.5, s = Math.sqrt(z);
        r = HPI - 2.0 * (s + s * asinCore(z));
        return (float) (a < 0 ? -r : r);
    }

    public static float acos(float a) {
        double x = a;
        int ix = Float.floatToRawIntBits(a) & 0x7fffffff;
        if (ix >= 0x3f800000) {
            if (a == 1.0f) return 0.0f;
            if (a == -1.0f) return 0x1.921fb6p1f;
            return Float.NaN;
        }
        double r;
        if (ix < 0x3f000000) {
            r = (ix < 0x31800000) ? HPI : HPI - (x - (HPIL - x * asinCore(x * x)));
        } else if (a > 0) {
            double z = (1.0 - x) * 0.5, s = Math.sqrt(z);
            double df = Double.longBitsToDouble(Double.doubleToRawLongBits(s) & 0xffffffff00000000L);
            double c = (z - df * df) / (s + df);
            r = 2.0 * (df + asinCore(z) * s + c);
        } else {
            double z = (1.0 + x) * 0.5, s = Math.sqrt(z);
            r = PD - 2.0 * (s + s * asinCore(z));
        }
        return (float) r;
    }

    private static double atpoly(double x2) {
        double u0 = V1 + x2 * V3, u1 = V5 + x2 * V7, u2 = V9 + x2 * V11, u3 = V13 + x2 * V15, u4 = V17 + x2 * V19;
        double x4 = x2 * x2, x8 = x4 * x4;
        return (u0 + x4 * u1) + x8 * ((u2 + x4 * u3) + x8 * u4);
    }

    public static float atan(float a) {
        int ix = Float.floatToRawIntBits(a);
        double x = Float.intBitsToFloat(ix & 0x7fffffff);
        boolean red = x > 1.0;
        double z = red ? 1.0 / x : x;
        double y = z * atpoly(z * z);
        double res = red ? HPI - y : y;
        return (float) (ix < 0 ? -res : res);
    }

    public static float atan2(float y, float x) {
        int ix = Float.floatToRawIntBits(x), iy = Float.floatToRawIntBits(y);
        if (zinfnan(ix) || zinfnan(iy)) return atn2sp(y, x, ix, iy);
        double ax = Math.abs(x), ay = Math.abs(y);
        boolean sw = ay > ax;
        double z = sw ? ax / ay : ay / ax;
        double r = z * atpoly(z * z);
        double res = sw ? (ix < 0 ? HPI + r : HPI - r) : (ix < 0 ? PD - r : r);
        return (float) (iy < 0 ? -res : res);
    }

    private static float atn2sp(float y, float x, int ix, int iy) {
        if (Float.isNaN(x) || Float.isNaN(y)) return Float.NaN;
        boolean ys = iy < 0;
        if ((iy & 0x7fffffff) == 0x7f800000) {
            if ((ix & 0x7fffffff) == 0x7f800000)
                return ix < 0 ? (ys ? -0x1.2d97c8p1f : 0x1.2d97c8p1f) : (ys ? -0x1.921fb6p-1f : 0x1.921fb6p-1f);
            return ys ? -0x1.921fb6p0f : 0x1.921fb6p0f;
        }
        if (y == 0.0f) return ix < 0 ? (ys ? -0x1.921fb6p1f : 0x1.921fb6p1f) : y;
        if ((ix & 0x7fffffff) == 0x7f800000)
            return ix < 0 ? (ys ? -0x1.921fb6p1f : 0x1.921fb6p1f) : (ys ? -0.0f : 0.0f);
        double ax = Math.abs(x), ay = Math.abs(y);
        boolean sw = ay > ax;
        double z = sw ? ax / ay : ay / ax;
        double r = z * atpoly(z * z);
        double res = sw ? (ix < 0 ? HPI + r : HPI - r) : (ix < 0 ? PD - r : r);
        return (float) (ys ? -res : res);
    }

    private static double thpoly(double x2) {
        double u0 = T1 + x2 * T3, u1 = T5 + x2 * T7, u2 = T9 + x2 * T11, u3 = T13 + x2 * T15;
        double x4 = x2 * x2, x8 = x4 * x4;
        return (u0 + x4 * u1) + x8 * (u2 + x4 * u3);
    }

    public static float tanh(float v) {
        int iv = Float.floatToRawIntBits(v);
        double ax = Float.intBitsToFloat(iv & 0x7fffffff);
        if (ax >= 0x1.205966p3) return iv < 0 ? -1.0f : 1.0f;
        double r;
        if (ax > 1.1) {
            double e = expCore(ax + ax);
            r = (e - 1.0) / (e + 1.0);
        } else {
            r = ax * thpoly(ax * ax);
        }
        return (float) (iv < 0 ? -r : r);
    }

    public static float sigmoid(float x) {
        int ix = Float.floatToRawIntBits(x);
        double ax = Float.intBitsToFloat(ix & 0x7fffffff);
        if (ax >= 104.0) return ix < 0 ? 0.0f : 1.0f;
        double e = expCore(-ax);
        return (float) (ix < 0 ? e / (1.0 + e) : 1.0 / (1.0 + e));
    }

    public static void softmax(float[] x, float[] o, int n) {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) if (x[i] > m) m = x[i];
        double s = 0.0;
        for (int i = 0; i < n; i++) s += o[i] = exp(x[i] - m);
        double inv = 1.0 / s;
        for (int i = 0; i < n; i++) o[i] *= inv;
    }
}