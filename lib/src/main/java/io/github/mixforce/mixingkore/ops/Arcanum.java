// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.ops;

import io.github.mixforce.mixingkore.mathx.FastMath;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * @see net.minecraft.world.entity.projectile.EyeOfEnder
 * @see net.minecraft.world.item.EnderEyeItem
 * @see net.minecraft.world.level.chunk.ChunkGeneratorStructureState
 * @see net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
 * @see net.minecraft.world.level.levelgen.structure.placement.StructurePlacement
 * @see net.minecraft.world.level.chunk.ChunkGenerator
 * @see net.minecraft.world.level.biome.BiomeSource
 * @see "data/minecraft/worldgen/structure_set/strongholds.json"
 * @see "data/minecraft/tags/worldgen/biome/stronghold_biased_to.json"
 */
public final class Arcanum {
    private Arcanum() {}

    public static final class Tracer {
        public Tracer() { this(new Config()); }
        public Tracer(Config config) { this(Locus.Rings.vanilla(), config); }
        Tracer(Locus.Rings rsv, Config cfg) { this.rsv = rsv; this.cfg = cfg; hi.defaultReturnValue(-1); }

        public static final class Config {
            public float clusRad = 16;
            public double dop = 0.05;
            public int topN = 5;
        }

        public static record Solution(Locus.Candidate[] c, double conf, int eyes, int clusters, String[] warns) {}

        public Solution explore() {
            if (!dty && vcl == cfg.clusRad && vdp == cfg.dop && vtn == cfg.topN) return sol;
            int m = 0, ns = n.size();
            if (arr.length < ns) arr = new Locus.Ray[ns];
            for (Node e : n) if (e.fchd > 0) arr[m++] = new Locus.Ray(e.ox, e.oz, e.fdx, e.fdz, e.fchd);
            if (m == 0) return co(null);
            if (la.length < m) { la = new double[m]; lb = new double[m]; lc = new double[m]; ld = new double[m]; }
            for (int i = 0; i < m; i++) { Locus.Ray r = arr[i]; la[i] = -r.dz(); lb[i] = r.dx(); lc[i] = (double) r.dz() * r.ox() - (double) r.dx() * r.oz(); ld[i] = (double) r.dx() * r.ox() + (double) r.dz() * r.oz(); }
            List<String> w = new ArrayList<>(warns);
            ol.clear(); ml.clear(); hi.clear();
            if (gk.length < m * (m - 1) / 2) gk = new long[m * (m - 1) / 2];
            if (us.length < m) us = new boolean[m];
            for (int i = 0; i < m; i++) us[i] = false;
            int gn = 0, ncl = 0, ln = m;
            boolean apr = false;
            for (int i = 0; i < m; i++) for (int j = i + 1; j < m; j++) {
                double cr = la[i] * lb[j] - lb[i] * la[j];
                if (Math.abs(cr) <= 0x1.1df42eae296e2p-8) continue;
                apr = true;
                double t1 = (la[j] * arr[i].ox() + lb[j] * arr[i].oz() + lc[j]) / cr, t2 = -(la[i] * arr[j].ox() + lb[i] * arr[j].oz() + lc[i]) / cr;
                if (t1 < 0 || t2 < 0) continue;
                long l = Locus.al((int) Math.round((arr[i].ox() + (double) arr[i].dx() * t1) / 16.0), (int) Math.round((arr[i].oz() + (double) arr[i].dz() * t1) / 16.0));
                if (hi.get(l) < 0) { hi.put(l, gn); gk[gn++] = l; }
            }
            while (ln >= 2) {
                int nm = Math.max(2, (int) Math.ceil(cfg.dop * ln)), bi = -1, bc = 0;
                double bm = 0;
                for (int g = 0; g < gn; g++) {
                    double kx = (int) gk[g] * 16.0, kz = (int) (gk[g] >> 32) * 16.0, sp = 0;
                    int c = 0;
                    for (int i = 0; i < m; i++) {
                        if (us[i]) continue;
                        double pp = Math.abs(la[i] * kx + lb[i] * kz + lc[i]);
                        if (pp > cfg.clusRad || lb[i] * kx - la[i] * kz - ld[i] <= 0) continue;
                        c++; sp += pp;
                    }
                    if (c < nm) continue;
                    double mp = sp / c;
                    if (c > bc || (c == bc && mp < bm)) { bi = g; bc = c; bm = mp; }
                    if (bc == ln) break;
                }
                if (bi < 0) break;
                double kx = (int) gk[bi] * 16.0, kz = (int) (gk[bi] >> 32) * 16.0, ox = 0, oz = 0, wv = 0;
                int c = 0;
                for (int i = 0; i < m; i++) {
                    if (us[i]) continue;
                    double pp = Math.abs(la[i] * kx + lb[i] * kz + lc[i]);
                    if (pp > cfg.clusRad || lb[i] * kx - la[i] * kz - ld[i] <= 0) continue;
                    us[i] = true; ln--; ox += arr[i].ox(); oz += arr[i].oz(); if (pp > wv) wv = pp; c++;
                }
                ox /= c; oz /= c;
                ac(ol, (int) gk[bi], (int) (gk[bi] >> 32), bm, wv, ox, oz, c, (double) c / m, w);
                ncl++;
            }
            if (ncl > 0 && ln > 0) w.add("Excluded " + ln + " inconsistent eye.");
            if (ln == 1) {
                for (int i = 0; i < m; i++) if (!us[i]) { exp(arr[i], ol); ncl++; break; }
            } else if (ln >= 2) {
                w.add(!apr ? "Rays parallel." : gn == 0 ? "Rays divergent." : "No ray consensus.");
                exp(arr[Locus.longest(arr, us, m)], ol);
                ncl++;
            }
            hi.clear();
            for (Locus.Candidate c : ol) {
                long l = Locus.al(c.cx(), c.cz());
                int ix = hi.get(l);
                if (ix < 0) { hi.put(l, ml.size()); ml.add(c); }
                else { Locus.Candidate p = ml.get(ix); ml.set(ix, new Locus.Candidate(p.cx(), p.cz(), Math.max(p.sc(), c.sc()) * 1.5, p.ring(), Math.max(p.rays(), c.rays()), Math.max(p.dop(), c.dop()), Math.min(p.sd(), c.sd()), Math.min(p.rd(), c.rd()))); }
            }
            ml.sort((a, b) -> Double.compare(b.sc(), a.sc()));
            int tn = Math.min(ml.size(), cfg.topN);
            Locus.Candidate[] nt = ml.subList(0, tn).toArray(new Locus.Candidate[0]);
            double sum = 0;
            for (Locus.Candidate c : nt) sum += c.sc();
            double conf = sum > 0 ? nt[0].sc() / sum : 0;
            return co(new Solution(nt, conf, m, ncl, w.toArray(new String[0])));
        }

        public void spawn(int id, double x, double z) { n.add(new Node(id, x, z, 36.0)); dty = true; }

        public void tick(int id, double x, double z) {
            if (n.isEmpty()) return;
            Node e = at(id);
            if (e == null) return;
            double dx = x - e.ox, dz = z - e.oz, d2 = dx * dx + dz * dz;
            if (!(d2 >= e.nxl) || d2 <= 0) return;
            double chd = Math.sqrt(d2);
            float inv = (float) (1.0 / chd), nx = (float) (dx * inv), nz = (float) (dz * inv);
            if (e.fchd > 0) {
                double cross = Math.abs((double) e.fdx * nz - (double) e.fdz * nx), dot = (double) e.fdx * nx + (double) e.fdz * nz;
                if (cross > 0x1.1df37c4954c21p-7 || dot < 0) {
                    dty = true;
                    if (!e.bn) { e.bn = true; warns.add("Bent eye " + e.id + " by " + String.format(java.util.Locale.ROOT, "%.2f", Math.toDegrees(FastMath.atan2((float) cross, (float) dot))) + " deg."); }
                    dx = x - e.lx; dz = z - e.lz; d2 = dx * dx + dz * dz;
                    if (d2 <= 0) return;
                    chd = Math.sqrt(d2); inv = (float) (1.0 / chd); nx = (float) (dx * inv); nz = (float) (dz * inv);
                    e.ox = e.lx; e.oz = e.lz;
                }
            }
            e.fdx = nx; e.fdz = nz; e.fchd = (float) chd; e.nxl = d2 * 2.25; e.lx = x; e.lz = z;
            dty = true;
        }

        public void undo() { if (!n.isEmpty()) { n.remove(n.size() - 1); dty = true; } }
        public void clear() { n.clear(); warns.clear(); dty = true; }
        public int ndx() { return n.size(); }
        public List<String> warns() { return List.copyOf(warns); }

        static final class Node {
            final int id; double ox, oz; double lx, lz, nxl; float fdx, fdz, fchd; boolean bn;
            Node(int id, double x, double z, double nxl) { this.id = id; ox = x; oz = z; lx = x; lz = z; this.nxl = nxl; }
        }

        final Locus.Rings rsv;
        final Config cfg;
        final List<Node> n = new ArrayList<>();
        final List<String> warns = new ArrayList<>();
        final Long2IntOpenHashMap hi = new Long2IntOpenHashMap();
        final List<Locus.Candidate> ol = new ArrayList<>(), ml = new ArrayList<>();
        Locus.Ray[] arr = new Locus.Ray[0];
        final long[] ko = new long[16]; final double[] hs = new double[16]; final int[] hr = new int[16];
        boolean[] us = new boolean[0];
        long[] gk = new long[0];
        double[] la = new double[0], lb = new double[0], lc = new double[0], ld = new double[0];
        Solution sol;
        double vcl, vdp; int vtn;
        boolean dty = true;

        Node at(int id) {
            for (int i = n.size() - 1; i >= 0; i--) { Node e = n.get(i); if (e.id == id) return e; }
            return null;
        }
        Solution co(Solution s) { vcl = cfg.clusRad; vdp = cfg.dop; vtn = cfg.topN; dty = false; return sol = s; }
        void ac(List<Locus.Candidate> out, int cx, int cz, double sd, double rd, double ox, double oz, int rays, double d, List<String> w) {
            int ring = rsv.at(Math.sqrt((double) cx * cx + (double) cz * cz));
            if (ring < 0) w.add("Chunk " + cx + "," + cz + " out of ring.");
            double ex = cx * 16.0 - ox, ez = cz * 16.0 - oz, p = ring < 0 ? 1e-9 : rsv.sc(ring, (ex * ex + ez * ez) / 256.0, Math.sqrt(ox * ox + oz * oz) / 16.0), q = FastMath.exp((float) (-(sd * sd) / 64.0));
            out.add(new Locus.Candidate(cx, cz, q * p * d, ring, rays, d, sd, rd));
        }
        void exp(Locus.Ray r, List<Locus.Candidate> out) {
            double ox = r.ox() / 16.0, oz = r.oz() / 16.0, b = ox * r.dx() + oz * r.dz(), c0 = ox * ox + oz * oz, d0 = Math.sqrt(c0), rm = rsv.ob(rsv.rsv() - 1);
            if (b >= 0 && d0 > rm) return;
            int hn = 0;
            long pk = 0; boolean pf = false;
            for (int q = 0; q < rsv.rsv(); q++) {
                int k = rsv.od(q);
                if (hn == 16 && 0.15 * rsv.dn(k) <= hs[15]) break;
                double ri = rsv.ib(k), ro = rsv.ob(k), dR = b * b - c0 + ro * ro;
                if (dR < 0) continue;
                double sq = Math.sqrt(dR), t1 = -b + sq, t0 = Math.max(0, -b - sq), dI = b * b - c0 + ri * ri, li = ri - 0.707, lo = ro + 0.707, li2 = li > 0 ? li * li : 0, lo2 = lo * lo, ha = 0, hb = -1;
                if (d0 > ri) { if (b < 0 && dI >= 0) { t1 = -b - Math.sqrt(dI); ha = -b + Math.sqrt(dI); hb = -b + sq; } }
                else t0 = Math.max(t0, -b + Math.sqrt(dI));
                for (int g = 0; g < 2; g++) {
                    if (g == 1) { if (hb < 0) break; t0 = ha; t1 = hb; }
                    if (t1 <= t0) continue;
                    int cx = (int) Math.floor(ox + r.dx() * t0), cz = (int) Math.floor(oz + r.dz() * t0);
                    double tx = r.dx() == 0 ? Double.POSITIVE_INFINITY : ((r.dx() > 0 ? cx + 1 : cx) - ox) / r.dx();
                    double tz = r.dz() == 0 ? Double.POSITIVE_INFINITY : ((r.dz() > 0 ? cz + 1 : cz) - oz) / r.dz();
                    double sx = r.dx() == 0 ? 0 : 1.0 / Math.abs(r.dx()), sz = r.dz() == 0 ? 0 : 1.0 / Math.abs(r.dz());
                    for (;;) {
                        double ccx = cx + 0.5, ccz = cz + 0.5, dc2 = ccx * ccx + ccz * ccz;
                        if (dc2 >= li2 && dc2 <= lo2) {
                            double de2 = (ccx - ox) * (ccx - ox) + (ccz - oz) * (ccz - oz), sc = 0.15 * rsv.sc(k, de2, d0);
                            if (hn == 16 && sc <= hs[15]) break;
                            long l = Locus.al(cx, cz);
                            if (!pf || l != pk) {
                                pk = l; pf = true; int i = hn < 16 ? hn++ : 15;
                                while (i > 0 && hs[i - 1] < sc) { hs[i] = hs[i - 1]; ko[i] = ko[i - 1]; hr[i] = hr[i - 1]; i--; }
                                hs[i] = sc; ko[i] = l; hr[i] = k;
                            }
                        }
                        double tn = Math.min(tx, tz);
                        if (tn > t1) break;
                        if (tx <= tz) { cx += r.dx() > 0 ? 1 : -1; tx += sx; } else { cz += r.dz() > 0 ? 1 : -1; tz += sz; }
                    }
                }
            }
            for (int i = 0; i < hn; i++) out.add(new Locus.Candidate((int) ko[i], (int) (ko[i] >> 32), hs[i], hr[i], 1, 0, 0, 0));
        }
    }

    public static final class Locus {
        private Locus() {};

        public static record Candidate(int cx, int cz, double sc, int ring, int rays, double dop, double sd, double rd) {}
        static record Ray(double ox, double oz, float dx, float dz, float chd) {}

        static long al(int x, int z) { return ((long) x & 0xFFFFFFFFL) | ((long) z & 0xFFFFFFFFL) << 32; }
        static int longest(Ray[] rs, boolean[] u, int n) {
            int bi = -1;
            for (int i = 0; i < n; i++) if (!u[i] && (bi < 0 || rs[i].chd() > rs[bi].chd())) bi = i;
            return bi;
        }

        static final class Rings {
            static final int BS = 10;
            final int half, bw;
            final int[] cnt, be, od;
            final double[] dn, ia;
            public static Rings vanilla() { return of(32, 3, 128); }
            public static Rings of(int dist, int s0, int cx) {
                List<Integer> asl = new ArrayList<>();
                int s = s0, i = 0, cc = 0;
                for (int x = 0; x < cx; x++) {
                    asl.add(cc);
                    if (++i == s) { cc++; i = 0; s += 2 * s / (cc + 1); s = Math.min(s, cx - x); }
                }
                int[] c = new int[cc + 1];
                for (int v : asl) c[v]++;
                return new Rings(dist, c);
            }
            Rings(int dist, int[] cnt) {
                half = dist * 5 / 4; bw = half + BS; this.cnt = cnt; be = new int[cnt.length]; dn = new double[cnt.length]; ia = new double[cnt.length]; od = new int[cnt.length];
                for (int r = 0; r < cnt.length; r++) {
                    be[r] = dist * (4 + 6 * r);
                    double b = be[r], v = Math.PI * ((b + half) * (b + half) - (b - half) * (b - half));
                    dn[r] = cnt[r] / v; ia[r] = 1.0 / v;
                }
                for (int r = 0; r < od.length; r++) { int j = r; while (j > 0 && dn[r] > dn[od[j - 1]]) { od[j] = od[j - 1]; j--; } od[j] = r; }
            }
            int rsv() { return cnt.length; }
            int ic(int r) { return be[r] - half; }
            int oc(int r) { return be[r] + half; }
            int ib(int r) { return be[r] - bw; }
            int ob(int r) { return be[r] + bw; }
            double dn(int r) { return dn[r]; }
            int od(int q) { return od[q]; }
            int at(double cd) {
                for (int r = 0; r < cnt.length; r++) if (cd >= ib(r) && cd <= ob(r)) return r;
                return -1;
            }
            double sc(int r, double d2, double s) {
                double nax = 1.0 - (lux(d2, oc(r), s) - lux(d2, ic(r), s)) * ia[r];
                return dn[r] * Math.max(nax, 1e-3);
            }
            static double lux(double a2, double b, double s) {
                double d = b - s;
                if (a2 <= d * d) return s > b ? 0 : Math.PI * a2;
                if (a2 >= (s + b) * (s + b)) return Math.PI * b * b;
                double b2 = b * b, x = (a2 - b2 + s * s) / (2 * s), y2 = a2 - x * x, y = Math.sqrt(y2 > 0 ? y2 : 0);
                return a2 * FastMath.atan2((float) y, (float) x) + b2 * FastMath.atan2((float) y, (float) (s - x)) - s * y;
            }
        }
    }
}