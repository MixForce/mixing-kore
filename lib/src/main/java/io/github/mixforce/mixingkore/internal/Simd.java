// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.internal;

import io.github.mixforce.mixingkore.mathx.FastMath;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public final class Simd {
    private Simd() {}

    private static final VectorSpecies<Long> L = LongVector.SPECIES_PREFERRED;

    public static int pop(long[] a, int n) {
        LongVector acc = LongVector.zero(L);
        int i = 0;
        for (; i < L.loopBound(n); i += L.length()) acc = acc.add(LongVector.fromArray(L, a, i).lanewise(VectorOperators.BIT_COUNT));
        long s = acc.reduceLanes(VectorOperators.ADD);
        for (; i < n; i++) s += Long.bitCount(a[i]);
        return (int) s;
    }

    private static final VectorSpecies<Float> F = FloatVector.SPECIES_PREFERRED;
    private static final float E0 = 0x1.ffffe8p-1f, E1 = 0x1.fffb34p-1f, E2 = 0x1.00059cp-1f, E3 = 0x1.57e0b8p-3f, E4 = 0x1.53ac16p-5f;
    private static final float LOG2E = 0x1.715476p0f, LN2H = 0.693359375f, LN2L = -2.1219444e-4f, MAGIC = 0x1.8p23f, CLAMP = -87.0f;

    public static void softmax(float[] x, float[] o, int n) {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) if (x[i] > m) m = x[i];
        int lanes = F.length(), upper = F.loopBound(n);
        FloatVector acc = FloatVector.zero(F);
        for (int i = 0; i < upper; i += lanes) {
            FloatVector vz = FloatVector.fromArray(F, x, i).sub(m).lanewise(VectorOperators.MAX, CLAMP);
            FloatVector kf = vz.mul(LOG2E).add(MAGIC).sub(MAGIC);
            FloatVector vr = kf.lanewise(VectorOperators.FMA, -LN2H, vz);
            vr = kf.lanewise(VectorOperators.FMA, -LN2L, vr);
            FloatVector p = vr.fma(E4, E3);
            p = vr.lanewise(VectorOperators.FMA, p, E2);
            p = vr.lanewise(VectorOperators.FMA, p, E1);
            p = vr.lanewise(VectorOperators.FMA, p, E0);
            IntVector kb = (IntVector) kf.convert(VectorOperators.F2I, 0);
            FloatVector ps = p.mul(kb.add(127).mul(1 << 23).viewAsFloatingLanes());
            ps.intoArray(o, i);
            acc = acc.add(ps);
        }
        float s = acc.reduceLanes(VectorOperators.ADD);
        for (int i = upper; i < n; i++) s += o[i] = FastMath.exp(x[i] - m);
        float inv = 1f / s;
        for (int i = 0; i < upper; i += lanes) FloatVector.fromArray(F, o, i).mul(inv).intoArray(o, i);
        for (int i = upper; i < n; i++) o[i] *= inv;
    }
}