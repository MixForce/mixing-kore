// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

import java.util.Random;

public final class Harness {

    interface F1 { float at(float x); }
    interface F2 { float at(float y, float x); }

    private static long ord(int b) { return b < 0 ? Integer.MIN_VALUE - b : b; }

    private static long ulp(float a, float b) {
        return Math.abs(ord(Float.floatToRawIntBits(a)) - ord(Float.floatToRawIntBits(b)));
    }
    private static void rel(String n, F1 ref, F1 tst, double lo, double hi, int c) {
        double mr = 0, ma = 0;
        Random rg = new Random(7);
        for (int i = 0; i < c; i++) {
            float x = (float) (lo + (hi - lo) * rg.nextDouble());
            double e = ref.at(x), g = tst.at(x);
            double d = Math.abs(e - g);
            if (d > ma) ma = d;
            double rr = d / Math.max(Math.abs(e), 1e-30);
            if (rr > mr) mr = rr;
        }
        System.out.println(n + ": rel=" + mr + " abs=" + ma);
    }

    private static void chk(String name, F1 ref, F1 tst, double lo, double hi, int n) {
        long mx = 0, bad = 0;
        double wx = 0;
        for (int i = 0; i < n; i++) {
            float x = (float) (lo + (hi - lo) * (i / (double) n));
            long u = ulp(ref.at(x), tst.at(x));
            if (u > mx) { mx = u; wx = x; }
            if (u > 1) bad++;
        }
        System.out.println(name + ": ulp=" + mx + " over1=" + bad + " at " + wx);
    }

    private static void chk(String name, F1 ref, F1 tst, int n, Random rg, long tol) {
        long mx = 0, bad = 0;
        double wx = 0;
        for (int i = 0; i < n; i++) {
            float x = Float.intBitsToFloat(rg.nextInt());
            float e = ref.at(x), g = tst.at(x);
            if (Float.isNaN(e) && Float.isNaN(g)) continue;
            long u = ulp(e, g);
            if (u > mx) { mx = u; wx = x; }
            if (u > tol) bad++;
        }
        System.out.println(name + ": ulp=" + mx + " over" + tol + "=" + bad + " at " + wx);
    }

    private static void sp(String name, F1 ref, F1 tst, float[] xs) {
        int bad = 0;
        for (float x : xs) {
            float e = ref.at(x), g = tst.at(x);
            if (Float.compare(e, g) != 0) {
                System.out.println(name + " MISS x=" + x + " (" + Integer.toHexString(Float.floatToRawIntBits(x)) + ") ref=" + e + " got=" + g);
                bad++;
            }
        }
        System.out.println(name + " specials: " + (bad == 0 ? "OK" : bad + " BAD"));
    }

    private static void sp2(String name, F2 ref, F2 tst, float[] xs) {
        int bad = 0;
        for (float x : xs) for (float y : xs) {
            float e = ref.at(y, x), g = tst.at(y, x);
            if (Float.compare(e, g) != 0) {
                System.out.println(name + " MISS y=" + y + " x=" + x + " ref=" + e + " got=" + g);
                bad++;
            }
        }
        System.out.println(name + " specials: " + (bad == 0 ? "OK" : bad + " BAD"));
    }

    private static void sp(String name, F1 ref, F1 tst, float[] xs, long tol) {
        int bad = 0;
        for (float x : xs) {
            float e = ref.at(x), g = tst.at(x);
            if (Float.isNaN(e) && Float.isNaN(g)) continue;
            if (ulp(e, g) > tol) { System.out.println(name + " MISS x=" + x + " ref=" + e + " got=" + g); bad++; }
        }
        System.out.println(name + " specials(ulp<=" + tol + "): " + (bad == 0 ? "OK" : bad + " BAD"));
    }

    void main() {
        //System.out.println(Double.toHexString()));
    }
}