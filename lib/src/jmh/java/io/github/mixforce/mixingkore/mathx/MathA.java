// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.mathx;

import org.openjdk.jmh.annotations.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(value = 3, jvmArgsAppend = {"--add-modules", "jdk.incubator.vector", "-Xms2g", "-Xmx2g", "-XX:+AlwaysPreTouch", "-Xbatch"})
//@Fork(value = 3, jvmArgsAppend = {"--add-modules", "jdk.incubator.vector", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseJVMCICompiler", "-Djdk.graal.Vectorization=true", "-Djdk.graal.Autovectorize=true", "-Djdk.graal.VectorUnroll=4", "-Djdk.graal.UsePriorityInlining=true", "-Djdk.graal.TuneInlinerExploration=1", "-Djdk.graal.OptDuplication=true", "-Djdk.graal.LoopRotation=true", "-Xms2g", "-Xmx2g", "-XX:+AlwaysPreTouch", "-Xbatch"})
@Warmup(iterations = 15, time = 2)
@Measurement(iterations = 10, time = 2)
public class MathA {
    float[] va = new float[1024], vl = new float[1024], vt = new float[1024], vs = new float[1024], vu = new float[1024], out = new float[1024], pair = new float[2];
    int c0, c1, c2, c3, c4, c5;

    @Setup(Level.Trial)
    public void setup() {
        Random rg = new Random(42);
        for (int i = 0; i < 1024; i++) {
            va[i] = (float) ((rg.nextDouble() * 2 - 1) * 60);
            vl[i] = (float) Math.pow(10, rg.nextDouble() * 12 - 6);
            vt[i] = (float) ((rg.nextDouble() * 2 - 1) * 3);
            vs[i] = (float) ((rg.nextDouble() * 2 - 1) * 10);
            vu[i] = (float) (rg.nextDouble() * 2 - 1);
        }
    }

    private float na() { c0 = (c0 + 1) & 1023; return va[c0]; }
    private float nl() { c1 = (c1 + 1) & 1023; return vl[c1]; }
    private float nt() { c2 = (c2 + 1) & 1023; return vt[c2]; }
    private float ns() { c3 = (c3 + 1) & 1023; return vs[c3]; }
    private float nv() { c4 = (c4 + 1) & 1023; return va[c4]; }
    private float nu() { c5 = (c5 + 1) & 1023; return vu[c5]; }

    @Benchmark public float jdkExp() { return (float) Math.exp(na()); }
    @Benchmark public float accExp() { return AccurateMath.exp(na()); }
    @Benchmark public float fastExp() { return FastMath.exp(na()); }
    @Benchmark public float roughExp() { return RoughMath.exp(na()); }

    @Benchmark public float jdkLog() { return (float) Math.log(nl()); }
    @Benchmark public float accLog() { return AccurateMath.log(nl()); }
    @Benchmark public float fastLog() { return FastMath.log(nl()); }
    @Benchmark public float roughLog() { return RoughMath.log(nl()); }

    @Benchmark public float jdkSin() { return (float) Math.sin(ns()); }
    @Benchmark public float accSin() { return AccurateMath.sin(ns()); }
    @Benchmark public float fastSin() { return FastMath.sin(ns()); }
    @Benchmark public float roughSin() { return RoughMath.sin(ns()); }

    @Benchmark public float jdkCos() { return (float) Math.cos(ns()); }
    @Benchmark public float accCos() { return AccurateMath.cos(ns()); }
    @Benchmark public float fastCos() { return FastMath.cos(ns()); }
    @Benchmark public float roughCos() { return RoughMath.cos(ns()); }

    @Benchmark public float jdkSC() { float x = ns(); pair[0] = (float) Math.sin(x); pair[1] = (float) Math.cos(x); return pair[0] + pair[1]; }
    @Benchmark public float accSC() { AccurateMath.sinCos(ns(), pair); return pair[0] + pair[1]; }
    @Benchmark public float fastSC() { FastMath.sinCos(ns(), pair); return pair[0] + pair[1]; }

    @Benchmark public float jdkPow() { return (float) Math.pow(3.7f, na()); }
    @Benchmark public float accPow() { return AccurateMath.pow(3.7f, na()); }
    @Benchmark public float fastPow() { return FastMath.pow(3.7f, na()); }
    @Benchmark public float roughPow() { return RoughMath.pow(3.7f, na()); }

    @Benchmark public float jdkAtan() { return (float) Math.atan(nv()); }
    @Benchmark public float accAtan() { return AccurateMath.atan(nv()); }
    @Benchmark public float fastAtan() { return FastMath.atan(nv()); }
    @Benchmark public float roughAtan() { return RoughMath.atan(nv()); }

    @Benchmark public float jdkAtan2() { return (float) Math.atan2(ns(), nv()); }
    @Benchmark public float accAtan2() { return AccurateMath.atan2(ns(), nv()); }
    @Benchmark public float fastAtan2() { return FastMath.atan2(ns(), nv()); }

    @Benchmark public float jdkTanh() { return (float) Math.tanh(nt()); }
    @Benchmark public float accTanh() { return AccurateMath.tanh(nt()); }
    @Benchmark public float fastTanh() { return FastMath.tanh(nt()); }
    @Benchmark public float roughTanh() { return RoughMath.tanh(nt()); }

    @Benchmark public float jdkSig() { float x = nt(); return 1f / (1f + (float) Math.exp(-x)); }
    @Benchmark public float accSig() { return AccurateMath.sigmoid(nt()); }
    @Benchmark public float fastSig() { return FastMath.sigmoid(nt()); }
    @Benchmark public float roughSig() { return RoughMath.sigmoid(nt()); }

    @Benchmark public float jdkSM() {
        float m = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < 1024; i++) if (vs[i] > m) m = vs[i];
        float s = 0f;
        for (int i = 0; i < 1024; i++) s += out[i] = (float) Math.exp(vs[i] - m);
        float inv = 1f / s;
        for (int i = 0; i < 1024; i++) out[i] *= inv;
        return out[0];
    }

    @Benchmark public float accSM() { AccurateMath.softmax(vs, out, 1024); return out[0]; }
    @Benchmark public float fastSM() { FastMath.softmax(vs, out, 1024); return out[0]; }

    @Benchmark public float jdkAsin() { return (float) Math.asin(nu()); }
    @Benchmark public float accAsin() { return AccurateMath.asin(nu()); }
    @Benchmark public float fastAsin() { return FastMath.asin(nu()); }
    @Benchmark public float roughAsin() { return RoughMath.asin(nu()); }

    @Benchmark public float jdkAcos() { return (float) Math.acos(nu()); }
    @Benchmark public float accAcos() { return AccurateMath.acos(nu()); }
    @Benchmark public float fastAcos() { return FastMath.acos(nu()); }
    @Benchmark public float roughAcos() { return RoughMath.acos(nu()); }
}