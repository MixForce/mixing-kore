// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

public final class RoughMath {
    private RoughMath() {}

    private static final float LN2F = 0.6931472f, PF2 = 3.1415927f, HPF2 = 1.5707964f;
    private static final float EM = 12102203f, EB = 1064986823f, PM = 8388608f;
    private static final float G0 = -2.51285648345947265625f, G1 = 4.070096492767333984375f, G2 = -2.1206815242767333984375f, G3 = 0.645145475864410400390625f, G4 = -8.1616364419460296630859375e-2f;
    private static final float W1 = -0.16665832698345184326171875f, W2 = 8.31404142081737518310546875e-3f, W3 = -1.85360040632076561450958251953125e-4f;
    private static final float A0 = 0x1.ffe432p-1f, A1 = -0x1.4da0c4p-2f, A2 = 0x1.3ea034p-3f, A3 = -0x1.6b1fecp-5f;
    private static final float B1 = 8.3143532276153564453125e-2f, B2 = 2.041300944983959197998046875e-2f, B3 = 1.217094599269330501556396484375e-3f, B4 = 5.940486676990985870361328125e-3f;

    public static float exp(float x) {
        x = x > 88.7f ? 88.7f : x < -87.3f ? -87.3f : x;
        return Float.intBitsToFloat((int) Math.fma(EM, x, EB));
    }

    public static float exp2(float x) {
        x = x > 128f ? 128f : x < -126f ? -126f : x;
        return Float.intBitsToFloat((int) Math.fma(PM, x, EB));
    }

    public static float log2(float x) {
        int b = Float.floatToRawIntBits(x);
        if (b <= 0) return Float.NaN;
        int e = (b >> 23) - 127;
        float m = Float.intBitsToFloat((b & 0x7fffff) | 0x3f800000);
        float p = Math.fma(m, G4, G3);
        p = Math.fma(p, m, G2);
        p = Math.fma(p, m, G1);
        return Math.fma(p, m, G0) + e;
    }

    public static float log(float x) {
        return log2(x) * LN2F;
    }

    public static float pow(float x, float y) {
        if (y == 0f) return 1f;
        if (y == 1f) return x;
        if (x == 1f) return 1f;
        if (y == 2f) return x * x;
        if (y == 3f) return x * x * x;
        if (y == 0.5f) return (float) Math.sqrt(x);
        if (x == 0f) return y > 0f ? 0f : Float.POSITIVE_INFINITY;
        float t = y * log2(x);
        t = t > 127f ? 127f : t < -126f ? -126f : t;
        return Float.intBitsToFloat((int) Math.fma(PM, t, EB));
    }

    public static float sin(float x) {
        int n = (int) Math.fma(x, 0.31830987f, 12582912f) - 12582912; //Math.round(x * 0.31830987f);
        float r = Math.fma(-n, PF2, x);
        float r2 = r * r;
        float s = r * Math.fma(r2, Math.fma(r2, Math.fma(r2, W3, W2), W1), 1f);
        return (n & 1) == 0 ? s : -s;
    }

    public static float cos(float x) {
        return sin(x + HPF2);
    }

    public static void sinCos(float x, float[] o) {
        o[0] = sin(x);
        o[1] = sin(x + HPF2);
    }

    public static float asin(float a) {
        float x = a < -1f ? -1f : a > 1f ? 1f : a;
        float ax = x < 0 ? -x : x;
        float u = 1f - ax;
        float q = Math.fma(u, B4, B3);
        q = Math.fma(u, q, B2);
        q = Math.fma(u, q, B1);
        float r = (float) Math.sqrt(u + u) * Math.fma(u, q, 1f);
        return x >= 0 ? HPF2 - r : r - HPF2;
    }

    public static float acos(float a) {
        float x = a < -1f ? -1f : a > 1f ? 1f : a;
        float ax = x < 0 ? -x : x;
        float u = 1f - ax;
        float q = Math.fma(u, B4, B3);
        q = Math.fma(u, q, B2);
        q = Math.fma(u, q, B1);
        float r = (float) Math.sqrt(u + u) * Math.fma(u, q, 1f);
        return x >= 0 ? r : PF2 - r;
    }

    public static float atan(float a) {
        float ax = a < 0 ? -a : a;
        boolean red = ax > 1f;
        float z = red ? 1f / ax : ax;
        float z2 = z * z;
        float p = z * Math.fma(z2, Math.fma(z2, Math.fma(z2, A3, A2), A1), A0);
        float r = red ? HPF2 - p : p;
        return a < 0 ? -r : r;
    }

    public static float atan2(float y, float x) {
        float ax = x < 0 ? -x : x, ay = y < 0 ? -y : y;
        boolean sw = ay > ax;
        float z = sw ? ax / ay : ay / ax;
        float z2 = z * z;
        float r = z * Math.fma(z2, Math.fma(z2, Math.fma(z2, A3, A2), A1), A0);
        float th = sw ? HPF2 - r : r;
        if (x < 0f) th = PF2 - th;
        return y < 0f ? -th : th;
    }

    public static float tanh(float x) {
        float x2 = x * x;
        float t = x * (27f + x2) / Math.fma(9f, x2, 27f);
        return t > 1f ? 1f : t < -1f ? -1f : t;
    }

    public static float sigmoid(float x) {
        return Math.fma(0.5f, tanh(0.5f * x), 0.5f);
    }
}