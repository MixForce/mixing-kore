// <editor-fold defaultstate="collapsed" desc="// LGPL-3.0-or-later | Copyright (c) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* SPDX-FileCopyrightText: 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.ops;

import io.github.mixforce.mixingkore.mathx.BitVec;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Canere {
    private Canere() {}

    public static void extract(ItemStack s, Object2IntMap<Holder<Enchantment>> out) {
        out.clear();
        if (s.isEmpty()) return;
        for (var e : exa(s).entrySet()) out.put(e.getKey(), e.getIntValue());
    }
    public static int level(ItemStack s, ResourceKey<Enchantment> k) {
        if (s.isEmpty()) return 0;
        for (var e : exa(s).entrySet()) if (e.getKey().is(k)) return e.getIntValue();
        return 0;
    }
    public static int level(Object2IntMap<Holder<Enchantment>> m, ResourceKey<Enchantment> k) {
        for (var e : Object2IntMaps.fastIterable(m)) if (e.getKey().is(k)) return e.getIntValue();
        return 0;
    }
    public static boolean ifAll(ItemStack s, Collection<ResourceKey<Enchantment>> ks) {
        if (ks.isEmpty()) return true;
        if (s.isEmpty()) return false;
        ItemEnchantments enc = exa(s);
        for (ResourceKey<Enchantment> k : ks) {
            boolean f = false;
            for (var e : enc.entrySet()) if (e.getKey().is(k)) { f = true; break; }
            if (!f) return false;
        }
        return true;
    }
    @SafeVarargs
    public static boolean ifAll(ItemStack s, ResourceKey<Enchantment>... keys) { return ifAll(s, List.of(keys)); }
    public static boolean ifAny(ItemStack s, Collection<ResourceKey<Enchantment>> ks) {
        if (s.isEmpty() || ks.isEmpty()) return false;
        for (var e : exa(s).entrySet()) for (ResourceKey<Enchantment> k : ks) if (e.getKey().is(k)) return true;
        return false;
    }
    @SafeVarargs
    public static boolean ifAny(ItemStack s, ResourceKey<Enchantment>... ks) { return ifAny(s, List.of(ks)); }
    public static boolean ifHas(ItemStack s, ResourceKey<Enchantment> k) {
        if (s.isEmpty()) return false;
        for (var e : exa(s).entrySet()) if (e.getKey().is(k)) return true;
        return false;
    }
    public static boolean ifHas(Object2IntMap<Holder<Enchantment>> m, ResourceKey<Enchantment> k) {
        for (var e : Object2IntMaps.fastIterable(m)) if (e.getKey().is(k)) return true;
        return false;
    }
    public static ItemEnchantments exa(ItemStack s) { return s.getItem() == Items.ENCHANTED_BOOK ? s.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY) : s.getEnchantments(); }

    public static final class Mixer {
        public static record Step(String left, String right, String out, Map<Holder<Enchantment>, Integer> le, Map<Holder<Enchantment>, Integer> re, Map<Holder<Enchantment>, Integer> oe, int cost, long cumulative, int conflicts) {}
        final Optimizer.Lexicon lex;
        final Optimizer.Pool pool;
        final int[] is;
        final String[] leafs;
        final List<Holder<Enchantment>> enc;
        final List<String> warns = new ArrayList<>();
        Optimizer.Config cfg = null;
        public Mixer(ItemStack t, List<ItemStack> u) {
            if (t == null || t.isEmpty()) throw new IllegalArgumentException("empty target");
            if (t.getCount() != 1) throw new IllegalArgumentException("target count must be 1");
            if (t.getItem() == Items.ENCHANTED_BOOK) throw new IllegalArgumentException("unsupported type of target");
            List<ItemStack> mats = u == null ? List.of() : u, ts = new ArrayList<>();
            ts.add(t);
            for (int i = 0; i < mats.size(); i++) {
                ItemStack s = mats.get(i);
                if (s == null || s.isEmpty()) continue;
                Item is = s.getItem();
                if (is != t.getItem() && is != Items.ENCHANTED_BOOK) {
                    warns.add("Unmergeable material " + s.getDisplayName().getString() + ".");
                    continue;
                }
                for (int c = 0; c < s.getCount(); c++) { ts.add(s); }
            }
            Map<Holder<Enchantment>, Integer> idx = new LinkedHashMap<>();
            enc = new ArrayList<>();
            List<Object2IntMap<Holder<Enchantment>>> es = new ArrayList<>(ts.size());
            int n = 0;
            for (ItemStack s : ts) {
                Object2IntLinkedOpenHashMap<Holder<Enchantment>> m = new Object2IntLinkedOpenHashMap<>();
                for (var en : exa(s).entrySet()) {
                    Holder<Enchantment> h = en.getKey();
                    Integer id = idx.get(h);
                    if (id == null) { id = enc.size(); idx.put(h, id); enc.add(h); }
                    int lv = en.getIntValue();
                    if (lv > Optimizer.MAX_LV) { lv = Optimizer.MAX_LV; n++; }
                    if (lv > 0) m.put(h, lv);
                }
                es.add(m);
            }
            if (enc.size() > Optimizer.MAX) throw new IllegalStateException("too many distinct enchants: " + enc.size());
            if (n > 0) warns.add("Truncated " + n + " level" + (n > 1 ? "s" : "") + " to 15.");
            Optimizer.LexTrans lt = new Optimizer.LexTrans();
            lt.type("B", true, false, true);
            lt.type(t.getHoverName().getString(), false, t.isDamageableItem(), EnchantmentHelper.canStoreEnchantments(t));
            n = 0;
            for (Holder<Enchantment> h : enc) {
                int ml = h.value().getMaxLevel();
                if (ml > Optimizer.MAX_LV) { ml = Optimizer.MAX_LV; n++; }
                lt.enchant(h.getRegisteredName(), ml, h.value().getAnvilCost());
            }
            for (int a = 0; a < enc.size(); a++) for (int b = a + 1; b < enc.size(); b++) if (!Enchantment.areCompatible(enc.get(a), enc.get(b))) { lt.conflict(a, b); }
            for (int e = 0; e < enc.size(); e++) if (enc.get(e).value().canEnchant(t)) { lt.allow(1, e); }
            lex = lt.begin();
            if (n > 0) warns.add("Truncated " + n + " max level" + (n > 1 ? "s" : "") + " to 15.");
            pool = new Optimizer.Pool(lex, ts.size() * 2 + 16);
            long[] lv = new long[Optimizer.LVW], pr = new long[Optimizer.PRW];
            is = new int[ts.size()]; leafs = new String[ts.size()];
            for (int i = 0; i < ts.size(); i++) {
                ItemStack s = ts.get(i);
                Arrays.fill(lv, 0L); Arrays.fill(pr, 0L);
                for (var en : es.get(i).object2IntEntrySet()) {
                    int e = idx.get(en.getKey()), l2 = en.getIntValue();
                    lv[e >>> 4] |= (long) l2 << ((e & 15) << 2);
                    if (e < 64) pr[0] |= 1L << e; else pr[1] |= 1L << e;
                }
                int ty = s.getItem() == Items.ENCHANTED_BOOK ? 0 : 1;
                is[i] = pool.intern(lv, pr, s.getOrDefault(DataComponents.REPAIR_COST, 0), ty, i == 0);
                leafs[i] = i == 0 ? "T" : (ty == 0 ? "B" : s.getHoverName().getString());
            }
        }
        public void config(Optimizer.Config config) { cfg = config; }
        public List<String> warnings() {
            if (cfg == null) return List.of();
            List<String> out = new ArrayList<>(warns);
            int rm = 0, k = 0; int[] us = new int[is.length - 1]; String[] un = new String[is.length - 1];
            for (int i = 1; i < is.length; i++) {
                if (Optimizer.Prepare.usable(pool, lex, cfg.policy, is[0], is[i])) { us[k] = is[i]; un[k] = leafs[i]; k++; }
                else rm++;
            }
            if (k < us.length) { us = Arrays.copyOf(us, k); un = Arrays.copyOf(un, k); }
            int sp = Optimizer.Prepare.surplus(pool, lex, is[0], us, un);
            if (rm > 0 && sp > 0) out.add("Ignored " + rm + " useless & " + sp + " surplus materials.");
            else if (rm > 0) out.add("Ignored " + rm + " useless material" + (rm > 1 ? "s" : "") + ".");
            else if (sp > 0) out.add("Ignored " + sp + " surplus material" + (sp > 1 ? "s" : "") + ".");
            return out;
        }
        public Optimizer.Solution optimize() {
            if (cfg == null) cfg = new Optimizer.Config();
            return Optimizer.explore(pool, is[0], tail(), leafs, cfg);
        }
        @SuppressWarnings("unchecked")
        public List<Step> trace(Optimizer.Solution s) {
            if (!s.feasible()) return List.of();
            Optimizer.Tr tr = s.tr();
            List<Step> out = new ArrayList<>(tr.n - 1);
            walk(tr, s.eval(), tr.root(), out, new long[1], new int[2 * tr.n - 1], new Map[2 * tr.n - 1]);
            return out;
        }
        void walk(Optimizer.Tr p, Optimizer.Eval ev, int s, List<Step> out, long[] acc, int[] lm, Map<Holder<Enchantment>, Integer>[] ms) {
            if (p.ifLeaf(s)) { lm[s] = s; return; }
            walk(p, ev, p.l[s], out, acc, lm, ms); walk(p, ev, p.r[s], out, acc, lm, ms);
            acc[0] += ev.aStep[s]; lm[s] = lm[p.l[s]];
            out.add(new Step(p.lName(lm[p.l[s]]), p.lName(lm[p.r[s]]), p.lName(lm[s]), enc(ev.ids[p.l[s]], ms, p.l[s]), enc(ev.ids[p.r[s]], ms, p.r[s]), enc(ev.ids[s], ms, s), ev.aStep[s], acc[0], ev.conflicts[s]));
        }
        Map<Holder<Enchantment>, Integer> enc(int id, Map<Holder<Enchantment>, Integer>[] ms, int s) {
            Map<Holder<Enchantment>, Integer> h = ms[s];
            if (h != null) return h;
            final Map<Holder<Enchantment>, Integer> m = new LinkedHashMap<>();
            pool.view(id).forEach((e, l) -> m.put(enc.get(e), l)); ms[s] = m;
            return m;
        }
        int[] tail() {
            int[] m = new int[is.length - 1];
            System.arraycopy(is, 1, m, 0, m.length);
            return m;
        }
    }

    public static final class Optimizer {
        private Optimizer() {}

        // Bits
        static final int MAX = 128, LVW = 8, PRW = 2, MAX_LV = 15;

        // いますぐ輪廻
        static final int RETRY_NOW = 3;

        // Pool
        static final ExecutorService EX = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()), r0 -> {
            Thread th = new Thread(r0, "mixing-optimizer"); th.setPriority(Thread.NORM_PRIORITY + 1); th.setDaemon(true);
            return th;
        });

        /** Config */
        public static record Policy(int limit) {
            public Policy { if (limit < 1) throw new IllegalArgumentException("limit not valid"); }
            public static final Policy VANILLA = new Policy(40);
            public static final Policy UNLIMITED = new Policy(5000);
        }
        public static final class Config {
            public int greedy = 8, topK = 3, bWidth = 24, bbLeaves = 14, lStall = 300, pIsm = 4; // 0 auto
            public float bLambda = 1.0f;
            public boolean beam = true, lns = true, bb = false, auto = false;
            public long seed = 42, budget = 0 /* Ns */, bbNodes = 2000000L, bbGap = 10;
            public Policy policy = Policy.VANILLA;
            Config copy() {
                Config c = new Config(); c.greedy = greedy; c.topK = topK; c.bWidth = bWidth; c.bbLeaves = bbLeaves; c.lStall = lStall; c.pIsm = pIsm; c.bLambda = bLambda; c.beam = beam; c.lns = lns; c.bb = bb; c.auto = auto; c.seed = seed; c.budget = budget; c.bbNodes = bbNodes; c.bbGap = bbGap; c.policy = policy;
                return c;
            }
            /** Just adjusts configurations */
            static void tune(Config cfg, int[] ss) {
                if (!cfg.auto) return;
                int[] c = ss.clone();
                if (c.length <= 32) for (int i = 1; i < c.length; i++) { int x = c[i]; int j = i - 1; while (j >= 0 && c[j] > x) { c[j + 1] = c[j]; j--; } c[j + 1] = x; }
                else Arrays.sort(c);
                int m = 0;
                for (int i = 0; i < c.length; i++) if (i == 0 || c[i] != c[i - 1]) m++;
                if (m <= 10) {
                    if (cfg.greedy < 8) cfg.greedy = 8;
                    cfg.beam = false; cfg.lns = true; // cfg.bb = true;
                } else if (m > 40) {
                    cfg.lns = true; cfg.beam = true;
                    if (cfg.greedy < 8) cfg.greedy = 8;
                    if (cfg.bWidth == 24) cfg.bWidth = 12;
                    if (cfg.lStall == 300) cfg.lStall = 600;
                }
            }
        }

        /** Result */
        public static record Solution(Tr tr, Eval eval, boolean feasible, int starts, long ns, Metrics metrics, String fail) {
            public long cost() { return feasible && eval != null ? eval.total : -1; }
            public double gap() { return metrics == null ? Double.NaN : metrics.gap; }
            public boolean proven() { return metrics != null && metrics.proven; }
        }

        record Metrics(long greedy, long beam, long lns, long bb, int reports, double gap, boolean proven) {}
        record WRes(Tr tr, long gN, long bN, long lN, int rep) {}

        /** Meta */
        static Solution explore(Pool pool, int t, int[] mats, String[] ls, Config cfg) {
            long t0 = System.nanoTime();
            Prepare.Prepared prep = Prepare.apply(pool, cfg.policy, t, mats, Arrays.copyOfRange(ls, 1, ls.length));
            if (!prep.ok) return new Solution(null, null, false, 0, System.nanoTime() - t0,  null, prep.fail);
            Lock lc = Lock.of(pool, t, prep.ids());
            Vanilla k = new Vanilla(pool.lex(), pool, cfg.policy, lc);
            int nm = prep.ids().length;
            int[] all = new int[nm + 1]; all[0] = t;
            System.arraycopy(prep.ids(), 0, all, 1, nm);
            String[] names = new String[nm + 1]; names[0] = ls[0];
            System.arraycopy(prep.names(), 0, names, 1, nm);
            Config tc = cfg.copy(); Config.tune(tc, all);
            long lb0 = new Floor(pool, cfg.policy.limit, lc).compute(new WSet(pool, null, all), pool.type(all[0]));
            if (lb0 < 0) return new Solution(null, null, false, 0, System.nanoTime() - t0, new Metrics(0, 0, 0, 0, 0, Double.NaN, false), "Infeasible.");
            Height he = Height.of(pool, t, prep.ids());
            long lb = he.lb, sn = he.lb >= 0 && pool.lex().pf && !he.risky ? he.lb : -1L;
            boolean se = sn >= 0;
            Evaluator ev = new Evaluator(k, pool, lc);
            Ceiling ub = new Ceiling();
            long tGreedy, tBeam, tLns;
            int reports, dn, nw = tc.pIsm > 0 ? Math.max(1, tc.pIsm) : Math.min(8, Runtime.getRuntime().availableProcessors());
            if (all.length == 1) nw = 1;
            int sW = Math.max(20, (int) (tc.lStall / Math.sqrt(nw)));
            long deadline = tc.budget > 0 ? t0 + tc.budget : 0;
            WRes[] res = new WRes[nw];
            String wn = null; AtomicLong sh = new AtomicLong(Long.MAX_VALUE);
            AtomicInteger di = new AtomicInteger();
            int gT = 1 + nw * (tc.greedy - 1);
            final int NW = nw;
            Future<?>[] fs = new Future<?>[nw];
            for (int wk = 0; wk < nw; wk++) {
                final int w = wk;
                Pool fp = pool.fork();
                fs[wk] = EX.submit(() -> res[w] = worker(fp, t, prep.ids(), names, tc, w, NW, di, gT, sW, deadline, sh, lc, sn));
            }
            for (Future<?> f : fs) {
                try { f.get(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (ExecutionException e) { wn = String.valueOf(e.getCause()); e.getCause().printStackTrace(); }
            }
            long wG = 0, wB = 0, wL = 0; int rep = 0, ok = 0;
            Eval cv = new Eval(2 * nm + 1);
            for (WRes r : res) {
                if (r == null) continue;
                rep += r.rep; wG = Math.max(wG, r.gN); wB = Math.max(wB, r.bN); wL = Math.max(wL, r.lN);
                if (r.tr == null) continue;
                Eval e = ev.evaluate(r.tr, cv);
                if (e.feasible) { ub.report(r.tr, e); ok++; if (se && ub.cost <= sn) break; }
            }
            tGreedy = wG; tBeam = wB; tLns = wL; reports = rep; dn = ok;
            if (!ub.found()) {
                Metrics st = new Metrics(tGreedy, tBeam, tLns, 0, reports, Double.NaN, false);
                return new Solution(null, null, false, dn, System.nanoTime() - t0, st, wn != null ? wn : "All " + dn + " failed.");
            }
            long al = se ? Math.max(0, ub.cost - lb) : Long.MIN_VALUE;
            double gap = se ? (ub.cost > 0 ? (double) al / ub.cost : 0.0) : (ub.cost > 0 ? (double) (ub.cost - lb0) / ub.cost : Double.NaN);
            boolean proven = se && ub.cost == lb;
            long tBb = 0;
            if (se && !proven && nm >= 2 && tc.bb && nm <= Math.min(tc.bbLeaves, 32) && al <= tc.bbGap && (deadline == 0 || deadline - System.nanoTime() > (deadline - t0) / 4)) {
                long tb0 = System.nanoTime();
                proven = BranchBound.con(pool, k, ev, all, names, tc.bbNodes, deadline, tc.bLambda, ub);
                tBb = System.nanoTime() - tb0;
                if (proven) gap = 0.0;
                else { al = Math.max(0, ub.cost - lb); gap = (double) al / ub.cost; }
            }
            Metrics st = new Metrics(tGreedy, tBeam, tLns, tBb, reports, gap, proven);
            return new Solution(ub.tr, ub.eval, true, dn, System.nanoTime() - t0, st, null);
        }
        static WRes worker(Pool pool, int t, int[] mats, String[] names, Config cfg, int wk, int nw, AtomicInteger di, int gT, int sW, long deadline, AtomicLong sh, Lock lc, long sn) {
            Vanilla k = new Vanilla(pool.lex(), pool, cfg.policy, lc);
            Evaluator ev = new Evaluator(k, pool, lc);
            Floor fr = new Floor(pool, cfg.policy.limit, lc);
            int[] a = new int[mats.length + 1]; a[0] = t;
            System.arraycopy(mats, 0, a, 1, mats.length);
            long q1 = cfg.budget > 0 ? deadline - cfg.budget * 3 / 4 : 0, q2 = cfg.budget > 0 ? deadline - cfg.budget / 2 : 0;
            Ceiling ub = new Ceiling(); ub.ext = sh;
            Eval sv = new Eval(2 * a.length - 1);
            Greedy.Ctx cx = new Greedy.Ctx();
            long seed = cfg.seed ^ (wk + 1) * 0x9E3779B97F4A7C15L, tg0 = System.nanoTime();
            int g;
            while ((g = di.getAndIncrement()) < gT) {
                if (sn >= 0 && ub.bn() <= sn) break;
                if (q1 > 0 && System.nanoTime() > q1) break;
                Greedy.GRes gr;
                if (g == 0) gr = Greedy.bkCon(pool, k, a, lc, cx);
                else {
                    int rw = (g - 1) % nw, s = (g - 1) / nw + 1;
                    gr = Greedy.con(pool, k, a, lc, Scorer.PRESETS[(s - 1 + rw) % Scorer.PRESETS.length], Math.max(1, cfg.topK), new Random(cfg.seed ^ (rw + 1) * 0x9E3779B97F4A7C15L ^ s * 0x9E3779B97F4A7C15L), cx);
                }
                if (!gr.ok) continue;
                Tr tr = new Tr(gr.l, gr.r, a, names);
                Eval e = ev.evaluate(tr, sv);
                if (e.feasible) ub.report(tr, e);
            }
            long tG = System.nanoTime() - tg0, tb0 = System.nanoTime();
            if (cfg.beam && (wk == 0 || !ub.found())) Beam.con(pool, k, ev, fr, a, names, cfg.bWidth, cfg.bLambda, q2, ub, lc, sn);
            long tB = System.nanoTime() - tb0, tl0 = System.nanoTime();
            if (cfg.lns) {
                Lns.con(pool, k, ev, fr, a, names, deadline, sW, seed ^ 0x5DEECE66DL, ub, lc, sn);
                for (int i = 1; i < RETRY_NOW && sn >= 0 && ub.bn() > sn; i++) Lns.con(pool, k, ev, fr, a, names, deadline, sW, seed ^ 0x5DEECE66DL ^ (long) i * 0xD1B54A32D192ED03L, ub, lc, sn);
            }
            long tL = System.nanoTime() - tl0;
            return new WRes(ub.tr, tG, tB, tL, ub.ver);
        }

        /** GRASP greedy */
        static final class Greedy {
            record GRes(int[] l, int[] r, boolean ok) {}
            static GRes con(Pool pool, Vanilla k, int[] ss, Lock lc, Scorer sc, int topK, Random r, Ctx g) { return con(pool, k, new WSet(pool, lc, ss, ss.length), sc, topK, r, g); }
            static GRes con(Pool pool, Vanilla k, WSet ws, Scorer sc, int topK, Random r, Ctx g) {
                if (topK < 1) topK = 1;
                if (topK > 8) topK = 8;
                g.reset(); g.lc = ws.lc; g.wv = ws.lc != null && ((ws.lc.w0 | ws.lc.w1) != 0);
                int len = ws.cnt;
                if (g.seenK < len) { g.seen = new PairSeen(4 * len * len); g.seenK = len; }
                g.seen.round();
                int n = len;
                for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                    int u = ws.liveV[i], v = ws.liveV[j];
                    if (u == v) {
                        if (!g.seen.seen(pkey(u, v))) {
                            k.probe(u, v, g.pb);
                            if (g.pb.ok && Lock.ok(ws.lc, ws.p, g.pb.p)) push(g, g.score(pool, sc, u, v, g.pb), u, v);
                        }
                        continue;
                    }
                    if (!g.seen.seen(pkey(u, v))) {
                        k.probe(u, v, g.pb);
                        if (g.pb.ok && Lock.ok(ws.lc, ws.p, g.pb.p)) push(g, g.score(pool, sc, u, v, g.pb), u, v);
                    }
                    if (!g.seen.seen(pkey(v, u))) {
                        k.probe(v, u, g.pb);
                        if (g.pb.ok && Lock.ok(ws.lc, ws.p, g.pb.p)) push(g, g.score(pool, sc, v, u, g.pb), v, u);
                    }
                }
                while (ws.cnt > 1) {
                    int pu, pv, pd;
                    if (topK == 1) {
                        pu = -1; pv = -1; pd = 0;
                        while (g.hs > 0) {
                            long key = pop(g);
                            int s = (int) key, u = g.sqU[s], v = g.sqV[s];
                            if (!livePair(ws, u, v)) continue;
                            k.probe(u, v, g.pb);
                            if (!g.pb.ok || !Lock.ok(ws.lc, ws.p, g.pb.p)) continue;
                            pu = u; pv = v; pd = (int) (key >>> 32);
                            break;
                        }
                        if (pu < 0) return new GRes(null, null, false);
                    } else {
                        int fs = 0;
                        while (g.hs > 0 && fs < topK) {
                            long key = pop(g);
                            int s = (int) key, u = g.sqU[s], v = g.sqV[s];
                            if (!livePair(ws, u, v)) continue;
                            g.pkU[fs] = u; g.pkV[fs] = v; g.pkO[fs] = (int) (key >>> 32); fs++;
                        }
                        if (fs == 0) return new GRes(null, null, false);
                        int p = fs == 1 ? 0 : r.nextInt(fs);
                        pu = g.pkU[p]; pv = g.pkV[p]; pd = g.pkO[p];
                        for (int t2 = 0; t2 < fs; t2++) if (t2 != p) { pushRaw(g, g.pkO[t2], g.pkU[t2], g.pkV[t2]); }
                    }
                    k.probe(pu, pv, g.pb);
                    if (!g.pb.ok || !Lock.ok(ws.lc, ws.p, g.pb.p)) continue;
                    int nid = k.commit(g.pb); ws.cost += g.pb.cost;
                    int pp = RefSeq.at(ws, pu, -1), pq = RefSeq.at(ws, pv, pp);
                    if (pp < pq) ws.take(pp, pq, 0, nid, g.pb.p); else ws.take(pq, pp, 1, nid, g.pb.p);
                    if (ws.cnt == 1) break;
                    if (livePair(ws, pu, pv)) pushRaw(g, pd, pu, pv);
                    g.seen.round();
                    for (int p2 = 0; p2 < ws.cnt; p2++) {
                        int x2 = ws.liveV[p2];
                        if (!g.seen.seen(pkey(nid, x2))) {
                            k.probe(nid, x2, g.pb);
                            if (g.pb.ok && Lock.ok(ws.lc, ws.p, g.pb.p)) push(g, g.score(pool, sc, nid, x2, g.pb), nid, x2);
                        }
                        if (x2 != nid && !g.seen.seen(pkey(x2, nid))) {
                            k.probe(x2, nid, g.pb);
                            if (g.pb.ok && Lock.ok(ws.lc, ws.p, g.pb.p)) push(g, g.score(pool, sc, x2, nid, g.pb), x2, nid);
                        }
                    }
                }
                return new GRes(ws.L, ws.R, true);
            }
            /** Book first heuristic */
            static GRes bkCon(Pool pool, Vanilla k, int[] ss, Lock lc, Ctx cx) {
                WSet ws = new WSet(pool, lc, ss);
                if (ws.cnt <= 1) return new GRes(ws.L, ws.R, true);
                Probe pb = new Probe();
                Lexicon t = pool.lex();
                int tn = 0;
                if (lc != null) {
                    int[] is = new int[ws.cnt], clv = new int[ws.cnt];
                    for (int i = 0; i < lc.ev.length && ws.cnt >= 2; i++) {
                        int e = lc.ev[i] >>> 4, n2 = lc.ev[i] & 15, m = 0;
                        for (int p = 0; p < ws.cnt; p++) {
                            int u = ws.liveV[p], lu = pool.level(u, e);
                            if (lu > 0 && lu < n2) { is[m] = u; clv[m] = lu; m++; }
                        }
                        sortCo(pool, is, clv, m);
                        while (m >= 2) {
                            int bu = -1, bv = -1, plv = 0;
                            sx: for (int p = 0; p < m; p++) for (int q = p + 1; q < m; q++) {
                                if (clv[p] != clv[q]) continue;
                                int u = is[p], v = is[q];
                                boolean ubk = pool.ifBook(u), vbk = pool.ifBook(v), au = Lock.applies(pool, t, u, e, e >>> 6, 1L << (e & 63)), av = Lock.applies(pool, t, v, e, e >>> 6, 1L << (e & 63));
                                if (!au && !av) continue;
                                int l2, r2;
                                if (ubk != vbk) {
                                    l2 = ubk ? v : u; r2 = l2 == u ? v : u;
                                    if (!(ubk ? av : au)) continue;
                                } else { l2 = av ? v : u; r2 = l2 == u ? v : u; }
                                k.probe(l2, r2, pb);
                                if (!pb.ok && ubk == vbk && au && av) {
                                    k.probe(r2, l2, pb);
                                    if (pb.ok) { int x = l2; l2 = r2; r2 = x; }
                                }
                                if (!pb.ok || !Lock.ok(lc, ws.p, pb.p)) continue;
                                bu = l2; bv = r2; plv = clv[p];
                                break sx;
                            }
                            if (bu < 0) break;
                            int lp = ws.at(bu, -1), rp = ws.at(bv, lp), a = ws.liveS[lp], b2 = ws.liveS[rp], ns = merge(k, ws, pb, lp, rp);
                            if (a == tn || b2 == tn) tn = ns;
                            int nid = pb.id, rm = bu == bv ? 2 : 1;
                            for (int rr = 0; rr < rm; rr++) { int ri = 0; while (is[ri] != bu) ri++; is[ri] = is[--m]; clv[ri] = clv[m]; }
                            if (bu != bv) { int ri = 0; while (is[ri] != bv) ri++; is[ri] = is[--m]; clv[ri] = clv[m]; }
                            if (plv + 1 < n2) { is[m] = nid; clv[m] = plv + 1; m++; }
                        }
                    }
                }
                int[] nbk = new int[ws.cnt]; int nn = 0;
                for (int i = 0; i < ws.cnt; i++) if (ws.liveS[i] != tn && !pool.ifBook(ws.liveV[i])) nbk[nn++] = ws.liveS[i];
                int[] gh = new int[nn]; int ng = 0, g = -1, gH = -1;
                sortCw(pool, ws, nbk, nn, 1);
                for (int i = 0; i < nn; i++) {
                    int ty = pool.type(ws.liveV[ws.pos(nbk[i])]);
                    if (ty != g) {
                        if (gH >= 0) gh[ng++] = gH;
                        g = ty; gH = nbk[i];
                        continue;
                    }
                    int ns = merge(k, ws, pb, ws.pos(gH), ws.pos(nbk[i]));
                    if (ns < 0) return con(pool, k, ss, lc, Scorer.COST, 1, new Random(0), cx);
                    gH = ns;
                }
                if (gH >= 0) gh[ng++] = gH;
                int[] ac = new int[ws.cnt]; int na = 0;
                for (int i = 0; i < ws.cnt; i++) if (ws.liveS[i] != tn && pool.ifBook(ws.liveV[i])) ac[na++] = ws.liveS[i];
                int[] fx = new int[na]; int nf = 0;
                float[] cb = new float[0]; long[] key = new long[ws.cnt];
                long lm = k.limit();
                while (na >= 2) {
                    sortCw(pool, ws, ac, na, -1);
                    int kn = 0;
                    for (int i = 0; i < na; i++) { int id = ws.liveV[ws.pos(ac[i])]; cb = cw(pool, cb, id); key[kn++] = ((long) pool.penalty(id) << 32) | Float.floatToRawIntBits(cb[id]); }
                    for (int i = 0; i < nf; i++) { int id = ws.liveV[ws.pos(fx[i])]; cb = cw(pool, cb, id); key[kn++] = ((long) pool.penalty(id) << 32) | Float.floatToRawIntBits(cb[id]); }
                    for (int i = 0; i < ng; i++) { int id = ws.liveV[ws.pos(gh[i])]; cb = cw(pool, cb, id); key[kn++] = ((long) pool.penalty(id) << 32) | Float.floatToRawIntBits(cb[id]); }
                    if (abs(key, kn, pool.penalty(ws.liveV[ws.pos(tn)]), lm)) break;
                    int[] n = new int[na]; int q = 0;
                    for (int lo = 0, hi = na - 1; lo < hi; lo++, hi--) {
                        int ns = merge(k, ws, pb, ws.pos(ac[hi]), ws.pos(ac[lo]));
                        if (ns < 0) ns = merge(k, ws, pb, ws.pos(ac[lo]), ws.pos(ac[hi]));
                        if (ns < 0) { fx[nf++] = ac[lo]; fx[nf++] = ac[hi]; }
                        else n[q++] = ns;
                    }
                    if ((na & 1) != 0) n[q++] = ac[na >> 1];
                    ac = n; na = q;
                }
                int[] hd = new int[na + nf + ng]; int m = 0;
                for (int i = 0; i < na; i++) hd[m++] = ac[i];
                for (int i = 0; i < nf; i++) hd[m++] = fx[i];
                for (int i = 0; i < ng; i++) hd[m++] = gh[i];
                sortHs(pool, ws, hd, m);
                int tx = tn;
                for (int i = 0; i < m; i++) {
                    int ns = merge(k, ws, pb, ws.pos(tx), ws.pos(hd[i]));
                    if (ns < 0) return con(pool, k, ss, lc, Scorer.COST, 1, new Random(0), cx);
                    tx = ns;
                }
                return new GRes(ws.L, ws.R, true);
            }
            static final class Ctx {
                long[] hp = new long[256]; int hs;
                int[] sqU = new int[256], sqV = new int[256]; int sqN;
                float[] cwB = new float[0];
                PairSeen seen; int seenK; Lock lc; boolean wv;
                final Probe pb = new Probe();
                final int[] pkU = new int[8], pkV = new int[8], pkO = new int[8];
                void reset() { hs = 0; sqN = 0; }
                float score(Pool pool, Scorer sc, int u, int v, Probe pb) {
                    float s = sc.cost * pb.cost + sc.penNew * pb.newPenalty + sc.conf * pb.conflicts - sc.upg * pb.upgraded;
                    if (sc.penSum != 0) s += sc.penSum * (pool.penalty(u) + pool.penalty(v));
                    if (wv) s += waste(pool, u, v);
                    if (sc.cw != 0) {
                        cwB = cw(pool, cwB, u); cwB = cw(pool, cwB, v);
                        if (sc.cw > 0) s += sc.cw * (cwB[u] + cwB[v]);
                        else if (pool.penalty(u) == 0 && pool.penalty(v) == 0 && pool.ifBook(u) && pool.ifBook(v)) s += sc.cw * cwB[v];
                    }
                    return s;
                }
                float waste(Pool pool, int u, int v) {
                    Lexicon t = pool.lex();
                    float w = 0;
                    int ub = u * PRW, tu = pool.typ[u];
                    boolean bk = t.book[tu];
                    long p0 = pool.pr[ub], p1 = pool.pr[ub + 1], x = lc.w0 & pool.pr[v * PRW];
                    while (x != 0) { int e = Long.numberOfTrailingZeros(x); x &= x - 1; w += wv(pool, t, u, v, e, bk, tu, p0, p1); }
                    x = lc.w1 & pool.pr[v * PRW + 1];
                    while (x != 0) { int e = 64 + Long.numberOfTrailingZeros(x); x &= x - 1; w += wv(pool, t, u, v, e, bk, tu, p0, p1); }
                    return w;
                }
                static float wv(Pool pool, Lexicon t, int u, int v, int e, boolean bk, int tu, long p0, long p1) {
                    int eb = e * PRW;
                    if (((t.conflict[eb] & p0) | (t.conflict[eb + 1] & p1)) != 0) return 0;
                    if (!bk && !t.ifApply(tu, e)) return 0;
                    int lv = pool.level(v, e), lu = pool.level(u, e), nl = lu == lv ? lv + 1 : Math.max(lu, lv), mx = t.maxLv[e];
                    if (nl > mx) nl = mx;
                    return nl == lu ? 1L << (lv - 1) : 0;
                }
            }
            static boolean abs(long[] key, int q, long p, long lm) {
                if (q <= 32) for (int i = 1; i < q; i++) { long x = key[i]; int j = i - 1; while (j >= 0 && key[j] > x) { key[j + 1] = key[j]; j--; } key[j + 1] = x; }
                else Arrays.sort(key, 0, q);
                for (int i = 0; i < q; i++) {
                    long l = key[i] >>> 32;
                    if (p + l + (long) Float.intBitsToFloat((int) key[i]) >= lm) return false;
                    p = Math.min(2 * Math.max(p, l) + 1, 1L << 40);
                }
                return true;
            }
            static int merge(Vanilla k, WSet ws, Probe pb, int lp, int rp) {
                k.probe(ws.liveV[lp], ws.liveV[rp], pb);
                if (!pb.ok || !Lock.ok(k.lc, ws.p, pb.p)) return -1;
                int nid = k.commit(pb); ws.cost += pb.cost;
                int i = Math.min(lp, rp), j = Math.max(lp, rp);
                ws.take(i, j, lp < rp ? 0 : 1, nid, pb.p);
                return ws.liveS[i];
            }
            static long pkey(int u, int v) { return ((long) u << 32 | v) + 1L; }
            static void push(Ctx g, float s, int u, int v) {
                int bits = Float.floatToRawIntBits(s);
                pushRaw(g, bits ^ ((bits >> 31) | 0x80000000), u, v);
            }
            static void pushRaw(Ctx g, int ord, int u, int v) {
                if (g.sqN == g.sqU.length) { g.sqU = Arrays.copyOf(g.sqU, g.sqN * 2); g.sqV = Arrays.copyOf(g.sqV, g.sqN * 2); }
                long key = ((long) ord << 32) | g.sqN;
                g.sqU[g.sqN] = u; g.sqV[g.sqN] = v; g.sqN++;
                if (g.hs == g.hp.length) g.hp = Arrays.copyOf(g.hp, g.hs * 2);
                int i = g.hs++;
                while (i > 0) {
                    int p2 = (i - 1) >> 1;
                    if (Long.compareUnsigned(g.hp[p2], key) <= 0) break;
                    g.hp[i] = g.hp[p2]; i = p2;
                }
                g.hp[i] = key;
            }
            static long pop(Ctx g) {
                long lt = g.hp[0], ls = g.hp[--g.hs];
                int i = 0;
                while (true) {
                    int c = 2 * i + 1;
                    if (c >= g.hs) break;
                    if (c + 1 < g.hs && Long.compareUnsigned(g.hp[c + 1], g.hp[c]) < 0) c++;
                    if (Long.compareUnsigned(g.hp[c], ls) >= 0) break;
                    g.hp[i] = g.hp[c]; i = c;
                }
                if (g.hs > 0) g.hp[i] = ls;
                return lt;
            }
            static boolean livePair(WSet ws, int u, int v) {
                int cu = ws.idCnt[u], cv = ws.idCnt[v];
                return cu > 0 && cv > 0 && (u != v || cu >= 2);
            }
            static float[] cw(Pool pool, float[] buf, int id) {
                if (id >= buf.length) buf = Arrays.copyOf(buf, Math.max(pool.size(), id + 1));
                if (buf[id] != 0) return buf;
                buf[id] = cwv(pool, id);
                return buf;
            }
            static float bkCw(Pool pool, int id) { return cwv(pool, id); }
            static void sortCo(Pool pool, int[] is, int[] clv, int m) {
                if (m < 2) return;
                float[] cws = new float[m];
                for (int i = 0; i < m; i++) cws[i] = bkCw(pool, is[i]);
                for (int i = 1; i < m; i++) {
                    int x = is[i], xl = clv[i]; float cx = cws[i]; int j = i - 1;
                    while (j >= 0 && cws[j] > cx) { is[j + 1] = is[j]; clv[j + 1] = clv[j]; cws[j + 1] = cws[j]; j--; }
                    is[j + 1] = x; clv[j + 1] = xl; cws[j + 1] = cx;
                }
            }
            static void sortCw(Pool pool, WSet ws, int[] idx, int len, int m) {
                if (len < 2) return;
                float[] cws = new float[len]; int[] ids = new int[len];
                for (int i = 0; i < len; i++) { ids[i] = ws.liveV[ws.pos(idx[i])]; cws[i] = bkCw(pool, ids[i]); }
                for (int i = 1; i < len; i++) {
                    int x = idx[i], xi = ids[i], tx = m == 1 ? pool.type(xi) : 0, j = i - 1;
                    float cx = cws[i];
                    while (j >= 0) {
                        int y = idx[j], yi = ids[j]; float cy = cws[j];
                        if (m == 1 ? (pool.type(yi) > tx || (pool.type(yi) == tx && cy > cx)) : cy > cx) { idx[j + 1] = y; cws[j + 1] = cy; ids[j + 1] = yi; j--; }
                        else break;
                    }
                    idx[j + 1] = x; cws[j + 1] = cx; ids[j + 1] = xi;
                }
            }
            static void sortHs(Pool pool, WSet ws, int[] heads, int len) {
                if (len < 2) return;
                float[] cws = new float[len]; int[] ps = new int[len];
                for (int i = 0; i < len; i++) { int id = ws.liveV[ws.pos(heads[i])]; cws[i] = bkCw(pool, id); ps[i] = pool.penalty(id); }
                for (int i = 1; i < len; i++) {
                    int x = heads[i], px = ps[i]; float cx = cws[i]; int j = i - 1;
                    while (j >= 0 && (ps[j] > px || (ps[j] == px && cws[j] > cx))) { heads[j + 1] = heads[j]; ps[j + 1] = ps[j]; cws[j + 1] = cws[j]; j--; }
                    heads[j + 1] = x; ps[j + 1] = px; cws[j + 1] = cx;
                }
            }
            static float cwv(Pool pool, int id) {
                Lexicon t = pool.lex();
                long v = pool.penalty(id), a = pool.pr[id * PRW], b = pool.pr[id * PRW + 1];
                boolean bk = pool.ifBook(id);
                while (a != 0) { int e = Long.numberOfTrailingZeros(a); a &= a - 1; v += (long) (bk ? t.baCost(e) : t.aCost(e)) * pool.level(id, e); }
                while (b != 0) { int e = 64 + Long.numberOfTrailingZeros(b); b &= b - 1; v += (long) (bk ? t.baCost(e) : t.aCost(e)) * pool.level(id, e); }
                return v;
            }
        }

        /** Beam search */
        static final class Beam {
            static void con(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names, int B, float lambda, long deadline, Ceiling ub, Lock lc, long sn) {
                if (B < 1) B = 1;
                int type = pool.type(ss[0]);
                Eval sv = new Eval(2 * ss.length - 1);
                WSet w0 = new WSet(pool, lc, ss);
                Probe pb = new Probe();
                if (w0.cnt == 1) { Tr p = w0.trs(ss, names); ub.report(p, ev.evaluate(p, sv)); return; }
                if (sn >= 0 && ub.bn() <= sn) return;
                if (deadline > 0 && System.nanoTime() > deadline) return;
                WSet[] beam = new WSet[B], nf = new WSet[B]; Cand[] tl = new Cand[B], tc = new Cand[B]; beam[0] = w0; tl[0] = new Cand(B);
                begin(k, lc, lambda, w0, tl[0], pb);
                float[] cs = new float[B]; int[] cp = new int[B], ct = new int[B];
                Mem tr = new Mem(1 << 13);
                PairSeen ps = new PairSeen(2 * (B + 2 * ss.length));
                WSet[] f = new WSet[2 * B + 1]; int fn = 0; Cand[] tf = new Cand[2 * B + 1]; int tfn = 0;
                int bl = 1;
                while (bl > 0) {
                    if (sn >= 0 && ub.bn() <= sn) return;
                    if (deadline > 0 && System.nanoTime() > deadline) return;
                    long ln = ub.bn(); int fi = 0;
                    for (int pi = 0; pi < bl; pi++) {
                        WSet w = beam[pi];
                        if (ln < Long.MAX_VALUE) {
                            long lb = fr.compute(w, type);
                            if (lb < 0 || w.cost + lb >= ln) continue;
                        }
                        if (fi == B && lambda >= 0f) {
                            int mp = Integer.MAX_VALUE;
                            for (int i2 = 0; i2 < w.cnt; i2++) { int pq = pool.pen[w.liveV[i2]]; if (pq < mp) mp = pq; }
                            if (w.cost + 2f * mp + lambda * (2f * mp + 1f) >= cs[B - 1]) continue;
                        }
                        Cand c = tl[pi];
                        for (int ti = 0; ti < c.n; ti++) {
                            float s = c.sc[ti];
                            if (fi < B || s < cs[B - 1]) {
                                int p2 = Math.min(fi, B - 1);
                                while (p2 > 0 && cs[p2 - 1] > s) { cs[p2] = cs[p2 - 1]; cp[p2] = cp[p2 - 1]; ct[p2] = ct[p2 - 1]; p2--; }
                                cs[p2] = s; cp[p2] = pi; ct[p2] = ti;
                                if (fi < B) fi++;
                            }
                        }
                    }
                    if (fi == 0) return;
                    WSet[] nw = nf; Cand[] nc = tc; int nn = 0;
                    for (int c2 = 0; c2 < fi; c2++) {
                        WSet src = beam[cp[c2]], w = fn > 0 ? f[--fn] : null;
                        if (w == null) w = src.copy(); else w.copyFrom(src);
                        Cand ec = tl[cp[c2]];
                        int l = ec.l[ct[c2]], r = ec.r[ct[c2]], pl = w.at(l, -1), pr = w.at(r, pl);
                        k.probe(l, r, pb);
                        int nid = k.commit(pb); w.cost += pb.cost;
                        int i0;
                        if (pl < pr) { i0 = pl; w.take(pl, pr, 0, nid, pb.p); } else { i0 = pr; w.take(pr, pl, 1, nid, pb.p); }
                        if (w.cnt == 1) {
                            Tr tr1 = w.trs(ss, names);
                            ub.report(tr1, ev.evaluate(tr1, sv));
                            f[fn++] = w;
                            continue;
                        }
                        if (tr.dominated(w.msh, w.msh2, w.cost)) { f[fn++] = w; continue; }
                        Cand c = tfn > 0 ? tf[--tfn] : null;
                        if (c == null) c = new Cand(B); else c.clear();
                        fill(k, lc, lambda, w, ec, c, nid, i0, pb, ps);
                        nw[nn] = w; nc[nn] = c; nn++;
                    }
                    for (int pi2 = 0; pi2 < bl; pi2++) { f[fn++] = beam[pi2]; tf[tfn++] = tl[pi2]; }
                    WSet[] ob = beam; beam = nw; nf = ob; bl = nn;
                    Cand[] oc = tl; tl = nc; tc = oc;
                }
            }
            static final class Cand {
                final int cap;
                int n;
                final float[] sc;
                final int[] l, r;
                Cand(int cap) { this.cap = cap; sc = new float[cap]; l = new int[cap]; r = new int[cap]; }
                void clear() { n = 0; }
                boolean before(float s, int l, int r, int i) {
                    if (s != sc[i]) return s < sc[i];
                    if (l != this.l[i]) return l < this.l[i];
                    return r < this.r[i];
                }
                void insert(float s, int l, int r) {
                    int i = n;
                    if (i == cap) {
                        if (!before(s, l, r, cap - 1)) return;
                        i = cap - 1;
                    }
                    while (i > 0 && before(s, l, r, i - 1)) { sc[i] = sc[i - 1]; this.l[i] = this.l[i - 1]; this.r[i] = this.r[i - 1]; i--; }
                    sc[i] = s; this.l[i] = l; this.r[i] = r;
                    if (n < cap) n++;
                }
            }
            static void begin(Vanilla k, Lock lc, float lambda, WSet w, Cand t, Probe pb) {
                t.clear();
                int n = w.cnt;
                for (int i = 0; i < n; i++) {
                    int u = w.liveV[i];
                    if (w.ipx[u] != i) continue;
                    if (w.idCnt[u] >= 2) push(k, lc, lambda, w, t, u, u, pb);
                    for (int j = i + 1; j < n; j++) {
                        int v = w.liveV[j];
                        if (v == u || w.ipx[v] != j) continue;
                        push(k, lc, lambda, w, t, u, v, pb); push(k, lc, lambda, w, t, v, u, pb);
                    }
                }
            }
            static void push(Vanilla k, Lock lc, float lambda, WSet w, Cand t, int u, int v, Probe pb) {
                k.probe(u, v, pb);
                if (!pb.ok || !Lock.ok(lc, w.p, pb.p)) return;
                t.insert(w.cost + pb.cost + lambda * pb.newPenalty + 0.25f * pb.conflicts, u, v);
            }
            static void fill(Vanilla k, Lock lc, float lambda, WSet w, Cand et, Cand t, int nid, int i0, Probe pb, PairSeen ps) {
                t.clear(); ps.round();
                for (int ti = 0; ti < et.n; ti++) {
                    int el = et.l[ti], er = et.r[ti], cu = w.idCnt[el], cv = w.idCnt[er];
                    if (!(cu > 0 && cv > 0 && (el != er || cu >= 2))) continue;
                    if (ps.seen(Greedy.pkey(el, er))) continue;
                    k.probe(el, er, pb);
                    if (!pb.ok || !Lock.ok(lc, w.p, pb.p)) continue;
                    t.insert(w.cost + pb.cost + lambda * pb.newPenalty + 0.25f * pb.conflicts, el, er);
                }
                int cnt = w.cnt;
                for (int p = 0; p < cnt; p++) {
                    if (p == i0) continue;
                    int x = w.liveV[p];
                    if (!ps.seen(Greedy.pkey(nid, x))) push(k, lc, lambda, w, t, nid, x, pb);
                    if (x != nid && !ps.seen(Greedy.pkey(x, nid))) push(k, lc, lambda, w, t, x, nid, pb);
                }
            }
        }

        /** B&B */
        static final class BranchBound {
            static boolean con(Pool pool, Vanilla k, Evaluator ev, int[] ss, String[] names, long budget, long deadline, float lambda, Ceiling ub) { return con(pool, k, ev, new WSet(pool, k.lc, ss), ss, names, budget, deadline, lambda, ub); }
            static boolean con(Pool pool, Vanilla k, Evaluator ev, WSet w0, int[] ss, String[] names, long budget, long deadline, float lambda, Ceiling ub) {
                if (w0.cnt == 1) {
                    Tr p = w0.trs(ss, names);
                    ub.report(p, ev.evaluate(p, null));
                    return true;
                }
                Ctx cx = new Ctx(k, ev, ss, names, budget, deadline, lambda, ub);
                dfs(cx, w0);
                return !cx.abort;
            }
            static final class Ctx {
                final Vanilla k; final Evaluator ev; final Floor fr;
                final int[] ss; final String[] names; final int type;
                final float lambda; final long deadline, budget;
                final Ceiling ub;
                final Mem tr;
                final Probe pb = new Probe();
                final Eval sc;
                final int cc;
                final long[][] ky;
                final int[] fCnt, fCur, fSi, fVi, fSj, fVj, fMsh2;
                final long[] fCost, fMsh;
                final long[][] fp;
                long nodes; boolean abort;
                Ctx(Vanilla k, Evaluator ev, int[] ss, String[] names, long budget, long deadline, float lambda, Ceiling ub) {
                    this.k = k; this.ev = ev; this.ss = ss; this.names = names;
                    this.budget = budget; this.deadline = deadline; this.lambda = lambda; this.ub = ub;
                    tr = new Mem(Math.clamp(budget, 4096, 1 << 21));
                    fr = new Floor(k.pool, k.limit(), k.lc);
                    type = k.pool.type(ss[0]);
                    int n = ss.length;
                    if (n > 32) throw new IllegalStateException("B&B supports at most 32 leaves");
                    cc = n * (n - 1);
                    ky = new long[n - 1][cc];
                    sc = new Eval(2 * n - 1);
                    fCnt = new int[n]; fCur = new int[n]; fSi = new int[n]; fVi = new int[n]; fSj = new int[n]; fVj = new int[n]; fCost = new long[n]; fMsh = new long[n]; fMsh2 = new int[n];
                    fp = k.lc == null ? null : new long[n][k.lc.ev.length];
                }
            }
            static void dfs(Ctx cx, WSet w) {
                if (++cx.nodes > cx.budget || (cx.nodes & 511) == 0 && cx.deadline > 0 && System.nanoTime() > cx.deadline) { cx.abort = true; return; }
                if (w.cnt == 1) {
                    Tr p = w.trs(cx.ss, cx.names);
                    cx.ub.report(p, cx.ev.evaluate(p, cx.sc));
                    return;
                }
                if (cx.tr.dominated(w.msh, w.msh2, w.cost)) return;
                long lbv = cx.fr.compute(w, cx.type);
                if (lbv < 0) return;
                if (cx.ub.found() && w.cost + lbv >= cx.ub.cost) return;
                int dep = w.cur - cx.ss.length;
                long[] ky = cx.ky[dep];
                int q = 0;
                for (int i = 0; i < w.cnt; i++) for (int j = i + 1; j < w.cnt; j++)
                    for (int d = 0; d < 2; d++) {
                        if (d == 1 && w.liveV[i] == w.liveV[j]) continue;
                        int u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                        cx.k.probe(u, v, cx.pb);
                        if (!cx.pb.ok || !Lock.ok(cx.k.lc, w.p, cx.pb.p)) continue;
                        int ib = Float.floatToRawIntBits(cx.pb.cost + cx.lambda * cx.pb.newPenalty);
                        ky[q++] = (((long) (ib ^ ((ib >> 31) & 0x7FFFFFFF))) << 11) | ((long) i << 6) | (j << 1 | d);
                    }
                if (q > 1) {
                    if (q <= 32) for (int i2 = 1; i2 < q; i2++) { long x = ky[i2]; int j2 = i2 - 1; while (j2 >= 0 && ky[j2] > x) { ky[j2 + 1] = ky[j2]; j2--; } ky[j2 + 1] = x; }
                    else Arrays.sort(ky, 0, q);
                }
                for (int c2 = 0; c2 < q && !cx.abort; c2++) {
                    long kk = ky[c2];
                    int i = (int) (kk >>> 6) & 31, j = (int) (kk >>> 1) & 31, d = (int) kk & 1, u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                    cx.k.probe(u, v, cx.pb);
                    int nid = cx.k.commit(cx.pb);
                    cx.fCnt[dep] = w.cnt; cx.fCur[dep] = w.cur; cx.fCost[dep] = w.cost; cx.fMsh[dep] = w.msh; cx.fMsh2[dep] = w.msh2;
                    cx.fSi[dep] = w.liveS[i]; cx.fVi[dep] = w.liveV[i];
                    cx.fSj[dep] = w.liveS[j]; cx.fVj[dep] = w.liveV[j];
                    if (w.p != null) System.arraycopy(w.p, 0, cx.fp[dep], 0, w.p.length);
                    w.cost += cx.pb.cost;
                    w.take(i, j, d, nid, cx.pb.p);
                    dfs(cx, w);
                    if (cx.abort) return;
                    w.cnt = cx.fCnt[dep]; w.cur = cx.fCur[dep]; w.cost = cx.fCost[dep]; w.msh = cx.fMsh[dep]; w.msh2 = cx.fMsh2[dep];
                    if (w.p != null) System.arraycopy(cx.fp[dep], 0, w.p, 0, w.p.length);
                    w.liveS[i] = cx.fSi[dep]; w.liveV[i] = cx.fVi[dep];
                    w.liveS[j] = cx.fSj[dep]; w.liveV[j] = cx.fVj[dep];
                    w.idCnt[nid]--; w.idCnt[cx.fVi[dep]]++; w.idCnt[cx.fVj[dep]]++;
                }
            }
        }

        /** Lns */
        static final class Lns {
            static void con(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names, long deadline, int stall, long seed, Ceiling ub, Lock lo, long sn) {
                int n = ss.length;
                if (n < 3 || !ub.found()) return;
                Random r = new Random(seed);
                Lx x = new Lx(pool, k, ev, fr, ss, names, lo);
                RefSeq rs = RefSeq.of(ub.tr, ub.eval);
                float[] w = {1f, 1f};
                int stallCt = 0, decay = 0;
                while (stallCt < stall) {
                    if (sn >= 0 && ub.bn() <= sn) return;
                    if (deadline > 0 && System.nanoTime() > deadline) return;
                    int op = r.nextFloat() * (w[0] + w[1]) < w[0] ? 0 : 1;
                    long prev = ub.cost;
                    boolean acc = op == 0 ? dig(x, r, rs, ub) : swapOp(x, r, ub);
                    if (acc) {
                        if (ub.cost < prev) { rs = RefSeq.of(ub.tr, ub.eval); stallCt = 0; x.side[0] = 0; w[op] += 1f; }
                        else stallCt++;
                    } else {
                        stallCt++;
                        w[op] = Math.max(0.05f, w[op] * 0.995f);
                    }
                    if (++decay >= 100) {
                        decay = 0;
                        w[0] = 1f + (w[0] - 1f) * 0.5f;
                        w[1] = 1f + (w[1] - 1f) * 0.5f;
                    }
                }
            }
            static final class Lx {
                final Pool pool; final Vanilla k; final Evaluator ev;
                final Floor fr; final int type;
                final int[] ss, side = {0}, post, lc; final String[] names;
                final Probe pb = new Probe();
                final boolean[] skip, subM, ancM; final int[] stk;
                final Greedy.Ctx g = new Greedy.Ctx();
                final Lock lo;
                final WSet bw;
                final Eval[] ebuf = new Eval[1];
                final Tr[] pbuf = new Tr[1];
                WSet skel;
                Lx(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names, Lock lo) {
                    int n = ss.length;
                    this.pool = pool; this.k = k; this.ev = ev; this.fr = fr; this.type = pool.type(ss[0]); this.ss = ss; this.names = names; this.lo = lo;
                    skip = new boolean[n - 1]; post = new int[n - 1]; stk = new int[2 * n]; subM = new boolean[2 * n - 1]; ancM = new boolean[2 * n - 1]; lc = new int[2 * n - 1];
                    ebuf[0] = new Eval(2 * n - 1); bw = new WSet(pool, lo, ss);
                }
            }
            static boolean dig(Lx x, Random r, RefSeq rs, Ceiling ub) {
                Tr tr = ub.tr;
                Eval cur = ub.eval;
                int n = tr.n;
                tr.postOrder(x.post, x.stk);
                for (int i = 0; i < n; i++) x.lc[i] = 1;
                for (int i = 0; i < n - 1; i++) { int s = x.post[i]; x.lc[s] = x.lc[tr.l[s]] + x.lc[tr.r[s]]; }
                int st = r.nextInt(4), X;
                if (st == 3) {
                    if (n <= 40) X = tr.root();
                    else {
                        int best = 0; X = n;
                        for (int s = n; s < 2 * n - 1; s++) if (x.lc[s] <= 32 && x.lc[s] > best) { best = x.lc[s]; X = s; }
                    }
                } else if (st == 0) X = n + r.nextInt(n - 1);
                else {
                    long bv = Long.MIN_VALUE; X = n;
                    for (int s = n; s < 2 * n - 1; s++) {
                        long v = st == 1 ? cur.aStep[s] : cur.conflicts[s];
                        if (v > bv) { bv = v; X = s; }
                    }
                }
                Arrays.fill(x.subM, false); Arrays.fill(x.ancM, false);
                for (int i = 0; i < n - 1; i++) {
                    int s = x.post[i];
                    x.subM[s] = s == X || x.subM[tr.l[s]] || x.subM[tr.r[s]];
                }
                for (int s = tr.p[X]; s >= 0; s = tr.p[s]) x.ancM[s] = true;
                for (int i = 0; i < n - 1; i++) x.skip[i] = x.subM[x.post[i]] || x.ancM[x.post[i]];
                if (x.skel == null) x.skel = x.bw.copy(); else x.skel.copyFrom(x.bw);
                WSet ws = x.skel;
                for (int i = 0; i < rs.lu.length; i++) {
                    if (x.skip[i]) continue;
                    int pu = RefSeq.at(ws, rs.lu[i], -1), pv = RefSeq.at(ws, rs.ru[i], pu);
                    x.k.probe(rs.lu[i], rs.ru[i], x.pb);
                    int nid = x.k.commit(x.pb); ws.cost += x.pb.cost;
                    if (pu < pv) ws.take(pu, pv, 0, nid, x.pb.p); else ws.take(pv, pu, 1, nid, x.pb.p);
                }
                if (x.side[0] >= 3) {
                    long lb = x.fr.compute(ws, x.type);
                    if (lb < 0 || ws.cost + lb >= ub.bn()) return false;
                }
                Greedy.GRes rep = Greedy.con(x.pool, x.k, ws, Scorer.PRESETS[r.nextInt(Scorer.PRESETS.length)], 1 + r.nextInt(3), r, x.g);
                if (!rep.ok) return false;
                return accept(x.ev, ub, ws, x.ss, x.names, cur.total, x.side, x.ebuf[0]);
            }
            static boolean swapOp(Lx x, Random r, Ceiling ub) {
                int n = ub.tr.n;
                Tr p2 = x.pbuf[0];
                if (p2 == null) p2 = x.pbuf[0] = ub.tr.copy();
                else p2.copyFrom(ub.tr);
                for (int t = 0; t < 4; t++) {
                    int a = r.nextInt(2 * n - 1), b = r.nextInt(2 * n - 1);
                    if (!p2.swap(a, b)) continue;
                    Eval e2 = x.ev.evaluate(p2, x.ebuf[0]);
                    if (e2.feasible && (e2.total < ub.cost || (e2.total == ub.cost && x.side[0] < 3))) {
                        if (e2.total == ub.cost) x.side[0]++;
                        if (e2.total < ub.cost) ub.report(p2.copy(), e2);
                        return true;
                    }
                    return false;
                }
                return false;
            }
            static boolean accept(Evaluator ev, Ceiling ub, WSet ws, int[] ss, String[] names, long cCost, int[] s, Eval sv) {
                Tr p = ws.trs(ss, names);
                Eval e = ev.evaluate(p, sv);
                if (!e.feasible) return false;
                if (e.total < cCost) { ub.report(p, e); return true; }
                if (e.total == cCost && s[0] < 3) { s[0]++; return true; }
                return false;
            }
        }

        /** Container for holding intermediate ids */
        static final class Probe {
            boolean ok, fr;
            long cost, key;
            int conflicts, upgraded, newPenalty, newType, id, cs = -1;
            boolean ts;
            long[] p = new long[0];
            final long[] lv = new long[LVW], pr = new long[PRW];
        }

        static interface Kernel {
            void probe(int l, int r, Probe p);
            int commit(Probe p);
        }

        /**
         * @see net.minecraft.world.inventory.AnvilMenu
         * クリエイティブ以外デフォルトルール
         */
        static final class Vanilla implements Kernel {
            final Lexicon t; final Pool pool; final Policy pol; final Lock lc; final int pN; final int[] idx; final Cache cache;
            Vanilla(Lexicon t, Pool pool, Policy pol, Lock lc) {
                this.t = t; this.pool = pool; this.pol = pol; this.lc = lc; pN = lc == null ? 0 : lc.ev.length;
                if (lc == null) idx = null;
                else {
                    idx = new int[t.ne];
                    Arrays.fill(idx, -1);
                    for (int i = 0; i < lc.ev.length; i++) idx[lc.ev[i] >>> 4] = i;
                }
                cache = new Cache(1 << Math.clamp(64 - Long.numberOfLeadingZeros((long) pool.n * pool.n * 8), 10, 16), pN);
            }
            int limit() { return pol.limit; }
            @Override public void probe(int l, int r, Probe p) {
                p.key = ((long) l << 32 | r) + 1L; p.id = -1; p.cs = -1;
                if (pN > 0 && p.p.length < pN + 1) p.p = new long[pN + 1];
                if (cache.get(p)) { p.fr = false; return; }
                eval(l, r, p);
                p.cs = cache.put(p, -1); p.fr = true;
            }
            @Override public int commit(Probe p) {
                if (!p.ok) return -1;
                if (p.id >= 0) return p.id;
                if (!p.fr) eval((int) ((p.key - 1) >>> 32), (int) (p.key - 1), p);
                int id = pool.intern(p.lv, p.pr, p.newPenalty, p.newType, p.ts);
                p.id = id;
                if (p.cs >= 0 && cache.k[p.cs] == p.key) { if (cache.st[p.cs] < 0) cache.st[p.cs] = id; }
                else cache.put(p, id);
                return id;
            }
            void eval(int l, int r, Probe p) {
                p.ok = false; p.cost = 0; p.conflicts = 0; p.upgraded = 0; p.newPenalty = -1;
                if (l < 0 || r < 0 || l >= pool.n || r >= pool.n) throw new IllegalArgumentException("bad idx");
                int tl = pool.typ[l], tr = pool.typ[r];
                boolean lt = pool.hsh[l] < 0, rt = pool.hsh[r] < 0;
                p.newType = tl; p.ts = lt | rt;
                if (!t.ifStore[tl]) return;
                boolean rb = t.book[tr], lb = t.book[tl];
                if (!rb && (tr != tl || !t.dmg[tl])) return;
                int li = pol.limit;
                long cost = (long) pool.pen[l] + pool.pen[r];
                if (cost >= li) return;
                long[] con = t.conflict, can = t.ifApply, rlv = pool.lv, pi = p.p;
                int canB = tl * PRW, rOff = r * LVW;
                long p0 = pool.pr[l * PRW], p1 = pool.pr[l * PRW + 1], nz = 0;
                boolean fz = false, monster = false; // nya
                int cf = 0, ap = 0, upg = 0;
                for (int w = 0; w < PRW; w++) {
                    long x = w == 0 ? pool.pr[r * PRW] : pool.pr[r * PRW + 1];
                    while (x != 0) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x), eb = e * PRW, di = idx == null ? -1 : idx[e]; x &= x - 1;
                        long c0 = con[eb] & p0, c1 = con[eb + 1] & p1;
                        if ((c0 | c1) != 0) {
                            int k = Long.bitCount(c0) + Long.bitCount(c1);
                            cf += k; cost += k;
                            if (cost >= li) return;
                            if (di >= 0) { if (!fz) { Arrays.fill(pi, 0, pN, 0L); fz = true; } long v = -(1L << (((int) (rlv[rOff + (e >>> 4)] >>> ((e & 15) << 2)) & 15) - 1)); pi[di] = v; nz |= v; }
                        } else if (lb || (can[canB + (e >>> 6)] >>> e & 1L) != 0) {
                            if (!monster) { System.arraycopy(pool.lv, l * LVW, p.lv, 0, t.nw); monster = true; }
                            int sh = (e & 15) << 2, w2 = e >>> 4, cur = (int) (p.lv[w2] >>> sh) & 15, rl = (int) (rlv[rOff + w2] >>> sh) & 15, nl = cur == rl ? rl + 1 : Math.max(cur, rl), mx = t.maxLv[e];
                            if (nl > mx) nl = mx;
                            p.lv[w2] = p.lv[w2] & ~(15L << sh) | (long) nl << sh;
                            if (w == 0) p0 |= 1L << e; else p1 |= 1L << e;
                            cost += (long) (rb ? t.bkFee[e] : t.fee[e]) * nl;
                            if (cost >= li) return;
                            if (nl > cur) upg++;
                            ap++;
                            if (di >= 0) { if (!fz) { Arrays.fill(pi, 0, pN, 0L); fz = true; } long v = (1L << (nl - 1)) - (cur > 0 ? 1L << (cur - 1) : 0L) - (1L << (rl - 1)); pi[di] = v; nz |= v; }
                        } else if (di >= 0) { if (!fz) { Arrays.fill(pi, 0, pN, 0L); fz = true; } long v = -(1L << (((int) (rlv[rOff + (e >>> 4)] >>> ((e & 15) << 2)) & 15) - 1)); pi[di] = v; nz |= v; }
                    }
                }
                if (ap == 0 || rt && !rev(l, r, tl, tr, cf, cost)) return;
                p.ok = true; p.cost = cost; p.conflicts = cf; p.upgraded = upg; p.pr[0] = p0; p.pr[1] = p1;
                p.newPenalty = (int) Math.min((long) Math.max(pool.pen[l], pool.pen[r]) * 2 + 1, Integer.MAX_VALUE);
                if (pN > 0) pi[pN] = nz;
            }
            boolean rev(int l, int r, int tl, int tr, int cf, long cost) {
                if (cf > 0) return false;
                if (!t.ifStore[tr] || (!t.book[tl] && (tl != tr || !t.dmg[tr]))) return true;
                long[] cl = t.ifApply, ac = t.conflict;
                int ci = tr * PRW, lo = l * LVW, ro = r * LVW, po = l * PRW, pr = r * PRW;
                boolean lb = t.book[tr], rb = t.book[tl];
                long cv = (long) pool.pen[l] + pool.pen[r], q0 = pool.pr[pr], q1 = pool.pr[pr + 1];
                int ap = 0;
                for (int w = 0; w < PRW; w++) {
                    long x = pool.pr[po + w];
                    while (x != 0) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x); x &= x - 1;
                        long d0 = ac[e * PRW] & q0, d1 = ac[e * PRW + 1] & q1;
                        if ((d0 | d1) != 0) {
                            cv += Long.bitCount(d0) + Long.bitCount(d1);
                            if (cv > cost) return true;
                            continue;
                        }
                        if (!lb && (cl[ci + (e >>> 6)] >>> e & 1L) == 0L) continue;
                        int sh = (e & 15) << 2, w2 = e >>> 4, c = (int) (pool.lv[ro + w2] >>> sh) & 15, rl = (int) (pool.lv[lo + w2] >>> sh) & 15, nl = c == rl ? rl + 1 : Math.max(c, rl), mx = t.maxLv[e];
                        if (nl > mx) nl = mx;
                        cv += (long) (rb ? t.bkFee[e] : t.fee[e]) * nl; ap++;
                        if (w == 0) q0 |= 1L << e; else q1 |= 1L << e;
                        if (cv > cost) return true;
                    }
                }
                return ap == 0;
            }
            /*
            record Item(Object2IntMap<Holder<Enchantment>> enc, ItemStack item, int penalty, boolean book) {}
            Pair<Item, Integer> merge(Item left, Item right) {
                if (!EnchantmentHelper.canStoreEnchantments(left.item()) || left.item().getCount() != 1 || (!right.book() && !(left.item().is(right.item().getItem()) && left.item().isDamageableItem())))
                    return null;
                var merged = new Object2IntLinkedOpenHashMap<>(left.enc());
                long cost = (long) left.penalty() + right.penalty();
                boolean any = false;
                for (var re : right.enc().object2IntEntrySet()) {
                    var ench = re.getKey();
                    boolean bl = left.book() || ench.value().canEnchant(left.item());
                    for (var other : merged.keySet()) {
                        if (other == ench) continue;
                        if (!Enchantment.areCompatible(ench, other)) {
                            cost++;
                            bl = false;
                        }
                    }
                    if (!bl) continue;
                    any = true;
                    int cur = merged.getInt(ench), rl = re.getIntValue();
                    int lvl = Math.min(cur == rl ? rl + 1 : Math.max(cur, rl), ench.value().getMaxLevel());
                    merged.put(ench, lvl);
                    int fee = ench.value().getAnvilCost();
                    if (right.book()) fee = Math.max(1, fee / 2);
                    cost += (long) fee * lvl;
                }
                if (!any || cost >= 40) return null;
                int n = (int) Math.min((long) Math.max(left.penalty(), right.penalty()) * 2 + 1, Integer.MAX_VALUE);
                return Pair.of(new Item(merged, left.item(), n, left.book()), (int) cost);
            }
             */
        }

        /** Eval dispatcher */
        static final class Evaluator {
            final Kernel k; final Pool pool; final Lock lc;
            final Probe pb = new Probe();
            int[] post = new int[0], stk = new int[0];
            Evaluator(Kernel k, Pool pool, Lock lc) { this.k = k; this.pool = pool; this.lc = lc; }
            Eval evaluate(Tr tr, Eval ev) {
                int n = tr.n, m = 2 * n - 1;
                if (ev == null || ev.ids.length != m) ev = new Eval(m);
                if (post.length != n - 1) post = new int[n - 1];
                if (stk.length != 2 * n) stk = new int[2 * n];
                tr.postOrder(post, stk);
                System.arraycopy(tr.leaf, 0, ev.ids, 0, n);
                ev.feasible = true;
                long total = 0;
                for (int i = 0; i < n - 1; i++) {
                    int s = post[i];
                    k.probe(ev.ids[tr.l[s]], ev.ids[tr.r[s]], pb);
                    if (!pb.ok) { ev.feasible = false; ev.total = -1; return ev; }
                    ev.ids[s] = k.commit(pb);
                    long c = pb.cost;
                    ev.aStep[s] = (int) c; ev.conflicts[s] = pb.conflicts; total += c;
                }
                if (lc != null) { int rt = ev.ids[tr.root()]; for (int i = 0; i < lc.ev.length; i++) if (pool.level(rt, lc.ev[i] >>> 4) < (lc.ev[i] & 15)) { ev.feasible = false; ev.total = -1; return ev; } }
                ev.total = total;
                return ev;
            }
        }

        /** DP Pruning */
        static final class Mem {
            long[] h; int[] h2; long[] c;
            int mask, cnt, limit;
            Mem(int cap) { int x = 16; while (x < cap) x <<= 1; h = new long[x]; h2 = new int[x]; c = new long[x]; mask = x - 1; limit = x - x / 4; }
            boolean dominated(long hh, int hg, long cost) {
                if (hh == 0) hh = 1;
                int i = (int) BitVec.mix64(hh) & mask;
                while (h[i] != 0) {
                    if (h[i] == hh && h2[i] == hg) {
                        if (c[i] <= cost) return true;
                        c[i] = cost; return false;
                    }
                    i = (i + 1) & mask;
                }
                if (cnt >= limit) {
                    if ((mask + 1) >= (1 << 23)) return false;
                    grow();
                    return dominated(hh, hg, cost);
                }
                h[i] = hh; h2[i] = hg; c[i] = cost; cnt++;
                return false;
            }
            void grow() {
                long[] a = new long[h.length << 1]; int[] b = new int[h.length << 1]; long[] c2 = new long[c.length << 1];
                int m = a.length - 1;
                for (int j = 0; j < h.length; j++) {
                    if (h[j] == 0) continue;
                    int i = (int) BitVec.mix64(h[j]) & m;
                    while (a[i] != 0) i = (i + 1) & m;
                    a[i] = h[j]; b[i] = h2[j]; c2[i] = c[j];
                }
                h = a; h2 = b; c = c2; mask = m; limit = h.length - h.length / 4;
            }
        }

        /** Suzume */
        static final class Ceiling {
            Tr tr; Eval eval; AtomicLong ext; long cost = Long.MAX_VALUE;
            int ver;
            boolean report(Tr p, Eval e) {
                if (!e.feasible || e.total >= cost) return false;
                tr = p; eval = e.copy(); cost = e.total; ver++;
                if (ext != null) ext.accumulateAndGet(cost, Math::min);
                return true;
            }
            boolean found() { return cost < Long.MAX_VALUE; }
            long bn() { return ext == null ? cost : Math.min(cost, ext.get()); }
        }
        static final class Floor {
            final Pool pool; final Lexicon t; final int cap; final Lock lc; final long df;
            long[] pa = new long[0], pe = new long[0];
            Floor(Pool pool, int cap, Lock lc) {
                this.pool = pool; t = pool.lex(); this.cap = cap; this.lc = lc;
                long d = 0;
                if (lc != null) for (int i = 0; i < lc.ev.length; i++) { int e = lc.ev[i] >>> 4; d += (long) Math.min(t.bkFee[e], t.fee[e]) * (lc.ev[i] & 15); }
                df = d;
            }
            long compute(WSet w, int type) {
                int cl = w.cnt;
                if (cl <= 1) return 0;
                int m = Integer.MAX_VALUE, mx = Integer.MAX_VALUE, c0 = 0; long sum = 0;
                for (int i = 0; i < cl; i++) {
                    int id = w.liveV[i], p = pool.pen[id];
                    sum += p;
                    if (p < m) { m = p; c0 = 1; } else if (p == m) c0++;
                    if (pool.typ[id] == type && p < mx) mx = p;
                }
                if (mx == Integer.MAX_VALUE) return -1;
                int H = 32 - Integer.numberOfLeadingZeros(cl - 1);
                if (2L * m >= cap) return -1;
                if (cl >= 3 && ((1L << (H - 1)) * (m + 1L) - 1) + m >= cap) return -1;
                int q0 = Math.min(c0 >> 1, Math.min(cl >> 1, cl - 2)), l = Math.max(0, H - 2), r = Math.max(0, cl - 2 - q0 - l);
                long in = 0;
                if (t.pf) {
                    in = (long) q0 * (2L * m + 1) + (long) r * (2L * m + 3);
                    for (int h = 2; h < H; h++) in += (1L << h) * (m + 1L) - 1;
                }
                long u0 = 0, u1 = 0;
                for (int i = 0; i < cl; i++) { int b = w.liveV[i] * PRW; u0 |= pool.pr[b]; u1 |= pool.pr[b + 1]; }
                long cu0 = 0, cu1 = 0;
                for (long x = u0; x != 0; x &= x - 1) { int t2 = Long.numberOfTrailingZeros(x); cu0 |= t.conflict[t2 * PRW]; cu1 |= t.conflict[t2 * PRW + 1]; }
                for (long x = u1; x != 0; x &= x - 1) { int t2 = 64 + Long.numberOfTrailingZeros(x); cu0 |= t.conflict[t2 * PRW]; cu1 |= t.conflict[t2 * PRW + 1]; }
                long ia0 = t.ifApply[type * PRW], ia1 = t.ifApply[type * PRW + 1];
                if (pa.length < cl) pa = new long[Math.max(cl, 8)];
                int[] mlv = t.maxLv; long[] lva = pool.lv;
                long tl = 0, ml = 0;
                for (int i = 0; i < cl; i++) {
                    int is = w.liveV[i], lb = is * LVW;
                    long a0 = pool.pr[is * PRW] & ia0, a1 = pool.pr[is * PRW + 1] & ia1, pv = 0;
                    if (t.pf) {
                        pv = Long.bitCount(a0 & cu0) + Long.bitCount(a1 & cu1);
                        for (long x = a0 & ~cu0; x != 0; x &= x - 1) { int e = Long.numberOfTrailingZeros(x); pv += (long) t.bkFee[e] * Math.min((int) (lva[lb + (e >>> 4)] >>> ((e & 15) << 2)) & 15, mlv[e]); }
                        for (long x = a1 & ~cu1; x != 0; x &= x - 1) { int e = 64 + Long.numberOfTrailingZeros(x); pv += (long) t.bkFee[e] * Math.min((int) (lva[lb + (e >>> 4)] >>> ((e & 15) << 2)) & 15, mlv[e]); }
                    } else {
                        for (long x = a0; x != 0; x &= x - 1) { int e = Long.numberOfTrailingZeros(x); if ((cu0 >>> e & 1L) == 0) pv += (long) Math.min(t.bkFee[e], t.fee[e]) * Math.min((int) (lva[lb + (e >>> 4)] >>> ((e & 15) << 2)) & 15, mlv[e]); }
                        for (long x = a1; x != 0; x &= x - 1) { int e = 64 + Long.numberOfTrailingZeros(x); if ((cu1 >>> (e - 64) & 1L) == 0) pv += (long) Math.min(t.bkFee[e], t.fee[e]) * Math.min((int) (lva[lb + (e >>> 4)] >>> ((e & 15) << 2)) & 15, mlv[e]); }
                    }
                    pa[i] = pv; tl += pv;
                    if (pool.typ[is] == type && pv > ml) ml = pv;
                }
                long e1 = tl - ml, bs = in;
                if (t.pf && cl <= 256) {
                    Sdp dp = Sdp.at(cl);
                    if (cl <= 32) for (int i = 1; i < cl; i++) { long x = pa[i]; int j = i - 1; while (j >= 0 && pa[j] > x) { pa[j + 1] = pa[j]; j--; } pa[j + 1] = x; }
                    else Arrays.sort(pa, 0, cl);
                    int rm = Arrays.binarySearch(pa, 0, cl, ml);
                    if (pe.length < cl) pe = new long[Math.max(cl, 8)];
                    pe[0] = 0;
                    for (int i = 0, j = 0; i < cl; i++) if (i != rm) pe[++j] = pe[j - 1] + pa[i];
                    final long INF = Long.MAX_VALUE >>> 2; long ql = INF;
                    for (int q = 0; q <= cl - 2; q++) {
                        long ch = Sdp.chain(mx, cl - 1 - q);
                        if (ch >= INF) continue;
                        long f = 0;
                        if (q > 0) {
                            f = INF; long[] rl = dp.f2[q];
                            for (int cc = 1, cm = Math.min(q, cl - 1 - q); cc <= cm; cc++) if (rl[cc] < f) f = rl[cc];
                            if (f >= INF) continue;
                        }
                        long v = ch + f + pe[q];
                        if (v < ql) ql = v;
                    }
                    if (ql < INF) bs = Math.max(bs, ql);
                }
                return sum + bs + (bs > in ? e1 : Math.max(e1, df));
            }
        }
        static final class Height {
            final long lb; final boolean risky;
            Height(long lb, boolean risky) { this.lb = lb; this.risky = risky; }
            static Height of(Pool pool, int t, int[] mats) {
                Lexicon lex = pool.lex();
                int ty = pool.type(t), n = mats.length;
                if (n <= 0) return new Height(0, false);
                if (n > 256) return new Height(-1, false);
                final long[] plv = new long[PRW];
                for (int w = 0; w < PRW; w++) {
                    plv[w] = pool.pr[t * PRW + w];
                    for (int i = 0; i < n; i++) plv[w] |= pool.pr[mats[i] * PRW + w];
                }
                final long[] c = new long[n];
                long acc = 0; boolean rk = false;
                for (int i = 0; i < n; i++) {
                    int mi = mats[i];
                    for (int w = 0; w < PRW; w++) for (long x = pool.pr[mi * PRW + w]; x != 0; x &= x - 1) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x);
                        if (!lex.ifApply(ty, e)) continue;
                        boolean cf = (lex.conflict[e * PRW] & plv[0]) != 0 || (lex.conflict[e * PRW + 1] & plv[1]) != 0;
                        long v = cf ? 1 : (long) lex.baCost(e) * Math.min(pool.level(mi, e), lex.maxLv(e));
                        if (cf) rk = true;
                        acc += v; c[i] += v;
                    }
                }
                long tpv = 0;
                for (int w = 0; w < PRW; w++) for (long x = pool.pr[t * PRW + w]; x != 0; x &= x - 1) {
                    int e = (w << 6) + Long.numberOfTrailingZeros(x);
                    if (!lex.ifApply(ty, e)) continue;
                    boolean cf = (lex.conflict[e * PRW] & plv[0]) != 0 || (lex.conflict[e * PRW + 1] & plv[1]) != 0;
                    if (cf) rk = true;
                    tpv += cf ? 1 : (long) lex.baCost(e) * Math.min(pool.level(t, e), lex.maxLv(e));
                }
                long mx = 0;
                for (int i = 0; i < n; i++) if (!pool.ifBook(mats[i]) && c[i] > mx) mx = c[i];
                long nl = acc - Math.max(0, mx - tpv);
                if (n <= 32) for (int i = 1; i < n; i++) { long x = c[i]; int j = i - 1; while (j >= 0 && c[j] > x) { c[j + 1] = c[j]; j--; } c[j + 1] = x; }
                else Arrays.sort(c);
                long[] pe = new long[n + 1];
                for (int i = 0; i < n; i++) pe[i + 1] = pe[i] + c[i];
                int p0 = pool.penalty(t); long lp = p0;
                for (int i = 0; i < n; i++) {
                    int p = pool.penalty(mats[i]); lp += p;
                    if (!pool.ifBook(mats[i]) && p < p0) p0 = p;
                }
                Sdp dp = Sdp.at(n);
                final long INF = Long.MAX_VALUE >>> 2; long lb = INF;
                for (int q = 0; q < n; q++) {
                    long ch = Sdp.chain(p0, n - q);
                    if (ch >= INF) continue;
                    long f = 0;
                    if (q > 0) {
                        f = INF; long[] rw = dp.f2[q];
                        for (int cc = 1, cm = Math.min(q, n - q); cc <= cm; cc++) if (rw[cc] < f) f = rw[cc];
                        if (f >= INF) continue;
                    }
                    long v = lp + ch + f + nl + pe[q];
                    if (v < lb) lb = v;
                }
                if (lb >= INF) return new Height(-1, false);
                return new Height(lb, rk);
            }
        }

        /** Pre-processing for solving */
        static final class Prepare {
            record Prepared(boolean ok, String fail, int[] ids, String[] names) {}
            static Prepared apply(Pool pool, Policy pol, int t, int[] mats, String[] names) {
                Lexicon lex = pool.lex();
                if (!lex.ifStore[pool.type(t)]) return new Prepared(false, "Target can't be enchanted.", null, null);
                if (pool.penalty(t) >= pol.limit) return new Prepared(false, "Target too expensive.", null, null);
                int k = 0;
                for (int i = 0; i < mats.length; i++) if (usable(pool, lex, pol, t, mats[i])) k++;
                int[] st; String[] nm;
                if (k == mats.length) { st = mats.clone(); nm = names.clone(); }
                else {
                    st = new int[k]; nm = new String[k];
                    for (int i = 0, j = 0; i < mats.length; i++) if (usable(pool, lex, pol, t, mats[i])) { st[j] = mats[i]; nm[j] = names[i]; j++; }
                }
                int rm = surplus(pool, lex, t, st, nm);
                if (rm > 0) { st = Arrays.copyOf(st, st.length - rm); nm = Arrays.copyOf(nm, nm.length - rm); }
                return new Prepared(true, null, st, nm);
            }
            static int surplus(Pool pool, Lexicon lex, int t, int[] ids, String[] names) {
                int n = ids.length;
                if (n == 0) return 0;
                int ty = pool.type(t);
                long[] mx = new long[lex.ne], ton = new long[PRW], s = new long[PRW];
                int[] pc = new int[lex.ne];
                for (int w = 0; w < PRW; w++) for (long x = pool.pr[t * PRW + w]; x != 0; x &= x - 1) {
                    int e = (w << 6) + Long.numberOfTrailingZeros(x);
                    mx[e] += 1L << (pool.level(t, e) - 1); ton[w] |= 1L << e; s[w] |= 1L << e; pc[e]++;
                }
                for (int i = 0; i < n; i++) {
                    int m = ids[i];
                    for (int w = 0; w < PRW; w++) for (long x = pool.pr[m * PRW + w]; x != 0; x &= x - 1) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x);
                        pc[e]++; s[w] |= 1L << e;
                        if (lex.ifApply(ty, e)) mx[e] += 1L << (pool.level(m, e) - 1);
                    }
                }
                long[] d0 = new long[PRW];
                for (int wd = 0; wd < PRW; wd++) for (long x = ton[wd]; x != 0; x &= x - 1) { int t2 = (wd << 6) + Long.numberOfTrailingZeros(x); d0[0] |= lex.conflict[t2 * PRW]; d0[1] |= lex.conflict[t2 * PRW + 1]; }
                int[] ws = new int[n], ix = new int[n];
                for (int i = 0; i < n; i++) { ws[i] = ids[i]; ix[i] = i; }
                for (int i = 1; i < n; i++) {
                    int x = ws[i], xi = ix[i], px = pool.penalty(x), j = i - 1;
                    while (j >= 0 && pool.penalty(ws[j]) < px) { ws[j + 1] = ws[j]; ix[j + 1] = ix[j]; j--; }
                    ws[j + 1] = x; ix[j + 1] = xi;
                }
                int[] rv = new int[lex.ne], rv2 = new int[lex.ne];
                long[] lk = new long[PRW], lk2 = new long[PRW];
                Lock.resolve(pool, lex, t, ty, mx, ton, s, rv, lk);
                int ln = n, k = 0;
                while (k < ln) {
                    int m = ws[k];
                    boolean wl = true;
                    for (int w = 0; w < PRW && wl; w++) for (long x = pool.pr[m * PRW + w]; x != 0; x &= x - 1) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x);
                        if (pc[e] < 2 && (d0[e >>> 6] >>> e & 1L) == 0) { wl = false; break; }
                    }
                    if (wl) {
                        int lc = ln - 1, d = ws[lc], di = ix[lc], mi = ix[k];
                        ws[k] = d; ix[k] = di; ws[lc] = m; ix[lc] = mi;
                        for (int w = 0; w < PRW; w++) for (long x = pool.pr[m * PRW + w]; x != 0; x &= x - 1) {
                            int e = (w << 6) + Long.numberOfTrailingZeros(x);
                            if (--pc[e] == 0) s[w] &= ~(1L << e);
                            if (lex.ifApply(ty, e)) mx[e] -= 1L << (pool.level(m, e) - 1);
                        }
                        Lock.resolve(pool, lex, t, ty, mx, ton, s, rv2, lk2);
                        wl = lk[0] == lk2[0] && lk[1] == lk2[1] && Arrays.equals(rv, rv2);
                        if (wl) { ln = lc; continue; }
                        for (int w = 0; w < PRW; w++) for (long x = pool.pr[m * PRW + w]; x != 0; x &= x - 1) {
                            int e = (w << 6) + Long.numberOfTrailingZeros(x);
                            if (++pc[e] == 1) s[w] |= 1L << e;
                            if (lex.ifApply(ty, e)) mx[e] += 1L << (pool.level(m, e) - 1);
                        }
                        ws[lc] = ws[k]; ix[lc] = ix[k]; ws[k] = m; ix[k] = mi;
                    }
                    k++;
                }
                boolean[] bk = new boolean[n];
                for (int i = 0; i < ln; i++) bk[ix[i]] = true;
                int w = 0;
                for (int i = 0; i < n; i++) if (bk[i]) { ids[w] = ids[i]; if (names != null) names[w] = names[i]; w++; }
                return n - w;
            }
            static boolean usable(Pool pool, Lexicon lex, Policy pol, int t, int mat) {
                if (pool.penalty(mat) >= pol.limit) return false;
                int tm = pool.type(mat), tt = pool.type(t);
                if (!lex.book[tm] && (tm != tt || !lex.dmg[tt])) return false;
                return ((pool.pr[mat * PRW] & lex.ifApply[tt * PRW]) | (pool.pr[mat * PRW + 1] & lex.ifApply[tt * PRW + 1])) != 0;
            }
        }

        /** Degraded Pillar */
        static final class Lock {
            final int[] ev; final long w0, w1;
            Lock(int[] ev, long w0, long w1) { this.ev = ev; this.w0 = w0; this.w1 = w1; }
            static Lock of(Pool pool, int t, int[] mats) {
                Lexicon lex = pool.lex();
                int ty = pool.type(t);
                long[] mx = new long[lex.ne], ton = new long[PRW], s = new long[PRW];
                int[] pc = new int[lex.ne];
                for (int w = 0; w < PRW; w++) for (long x = pool.pr[t * PRW + w]; x != 0; x &= x - 1) {
                    int e = (w << 6) + Long.numberOfTrailingZeros(x);
                    mx[e] += 1L << (pool.level(t, e) - 1); ton[w] |= 1L << e; s[w] |= 1L << e;
                }
                for (int m : mats) for (int w = 0; w < PRW; w++) for (long x = pool.pr[m * PRW + w]; x != 0; x &= x - 1) {
                    int e = (w << 6) + Long.numberOfTrailingZeros(x);
                    s[w] |= 1L << e; pc[e]++;
                    if (lex.ifApply(ty, e)) mx[e] += 1L << (pool.level(m, e) - 1);
                }
                int[] rv = new int[lex.ne]; long[] lk = new long[PRW];
                resolve(pool, lex, t, ty, mx, ton, s, rv, lk);
                int cn = Long.bitCount(lk[0]) + Long.bitCount(lk[1]);
                if (cn == 0) return null;
                int[] ev = new int[cn];
                long w0 = 0, w1 = 0;
                for (int e = 0, w = 0; e < lex.ne; e++) {
                    if ((lk[e >>> 6] >>> e & 1L) == 0) continue;
                    ev[w++] = e << 4 | rv[e];
                    if (pc[e] >= 2) { if (e < 64) w0 |= 1L << e; else w1 |= 1L << (e - 64); }
                }
                return new Lock(ev, w0, w1);
            }
            static boolean ok(Lock lc, long[] sum, long[] d) {
                if (lc == null) return true;
                if (d[lc.ev.length] == 0) return true;
                for (int i = 0; i < lc.ev.length; i++) if ((sum[i] + d[i]) >>> ((lc.ev[i] & 15) - 1) == 0L) return false;
                return true;
            }
            static boolean applies(Pool pool, Lexicon t, int l2, int e, int w6, long eb) {
                int tl2 = pool.typ[l2];
                if (!t.ifStore[tl2] || !(t.book[tl2] || (t.ifApply[tl2 * PRW + w6] & eb) != 0)) return false;
                return (t.conflict[e * PRW] & pool.pr[l2 * PRW]) == 0 && (t.conflict[e * PRW + 1] & pool.pr[l2 * PRW + 1]) == 0;
            }
            static void resolve(Pool pool, Lexicon lex, int t, int ty, long[] mx, long[] ton, long[] s, int[] rv, long[] lk) {
                long[] d0 = new long[PRW];
                for (int wd = 0; wd < PRW; wd++) for (long x = ton[wd]; x != 0; x &= x - 1) { int t2 = (wd << 6) + Long.numberOfTrailingZeros(x); d0[0] |= lex.conflict[t2 * PRW]; d0[1] |= lex.conflict[t2 * PRW + 1]; }
                Arrays.fill(lk, 0L); Arrays.fill(rv, 0);
                for (int e = 0; e < lex.ne; e++) {
                    if (mx[e] == 0 || !lex.ifApply(ty, e) || (d0[e >>> 6] >>> e & 1L) != 0) continue;
                    if (((lex.conflict[e * PRW] & s[0] & ~d0[0]) | (lex.conflict[e * PRW + 1] & s[1] & ~d0[1])) != 0) continue;
                    int r = Math.min(Math.min(64 - Long.numberOfLeadingZeros(mx[e]), lex.maxLv[e]), MAX_LV);
                    if (r <= pool.level(t, e)) continue;
                    lk[e >>> 6] |= 1L << e; rv[e] = r;
                }
            }
            static long lvw(Pool pool, int id, int e) { int lv = pool.level(id, e); return lv > 0 ? 1L << (lv - 1) : 0L; }
        }

        /** Weights of scoring heuristics for the greedy */
        static final class Scorer {
            final float cost, penNew, conf, upg, penSum, cw;
            Scorer(float c, float pn, float cf, float up, float ps, float cw) { this.cost = c; this.penNew = pn; this.conf = cf; this.upg = up; this.penSum = ps; this.cw = cw; }
            static final Scorer COST   = new Scorer(   1,    0, 0.25f, 0, 0,     0);
            static final Scorer HUFF   = new Scorer(   0,    0,     0, 0, 1,     0);
            static final Scorer LAMBDA = new Scorer(   1,    2, 0.25f, 0, 0,     0);
            static final Scorer UPG    = new Scorer(   1,    0, 0.25f, 4, 0,     0);
            static final Scorer CW     = new Scorer(0.3f,    0, 0.25f, 0, 0,     1);
            static final Scorer PAIR   = new Scorer(   1,-0.5f, 0.25f, 0, 0, -0.5f);
            static final Scorer[] PRESETS = { COST, HUFF, LAMBDA, UPG, CW, PAIR };
        }

        /** Result Tracer */
        static final class Tracer {
            static record Node(String left, String right, String out, Map<String, Integer> le, Map<String, Integer> re, Map<String, Integer> oe, int cost, long cumulative, int height, int conflicts) {}
            final Pool pool; final Lexicon t;
            Tracer(Pool pool) { this.pool = pool; t = pool.lex(); }
            @SuppressWarnings("unchecked")
            List<Node> trace(Tr tr, Eval ev) {
                if (!ev.feasible) return List.of();
                List<Node> out = new ArrayList<>(tr.n - 1);
                walk(tr, ev, tr.root(), out, new long[1], new int[2 * tr.n - 1], new Map[2 * tr.n - 1]);
                return out;
            }
            int walk(Tr tr, Eval ev, int s, List<Node> out, long[] acc, int[] lm, Map<String, Integer>[] ms) {
                if (tr.ifLeaf(s)) { lm[s] = s; return 0; }
                int hl = walk(tr, ev, tr.l[s], out, acc, lm, ms), hr = walk(tr, ev, tr.r[s], out, acc, lm, ms);
                acc[0] += ev.aStep[s]; lm[s] = lm[tr.l[s]];
                out.add(new Node(tr.lName(lm[tr.l[s]]), tr.lName(lm[tr.r[s]]), tr.lName(lm[s]), enc(ev.ids[tr.l[s]], ms, tr.l[s]), enc(ev.ids[tr.r[s]], ms, tr.r[s]), enc(ev.ids[s], ms, s), ev.aStep[s], acc[0], 1 + Math.max(hl, hr), ev.conflicts[s]));
                return 1 + Math.max(hl, hr);
            }
            Map<String, Integer> enc(int id, Map<String, Integer>[] ms, int s) {
                Map<String, Integer> h = ms[s];
                if (h != null) return h;
                final Map<String, Integer> m = new LinkedHashMap<>();
                pool.view(id).forEach((e, lv) -> m.put(t.eName(e), lv)); ms[s] = m;
                return m;
            }
        }

        /** Lex for lookups */
        static final class Lexicon {
            final int ne, nt, nw, pw;
            final boolean truncated, pf;
            final int[] maxLv, fee, bkFee;
            final long[] conflict, ifApply;
            final boolean[] book, dmg, ifStore;
            final String[] eName, tName;
            Lexicon(int ne, int nt, int nw, int pw, boolean truncated, boolean pf, int[] maxLv, int[] fee, int[] bkFee, long[] conflict, long[] ifApply, boolean[] book, boolean[] dmg, boolean[] ifStore, String[] eName, String[] tName) { this.ne = ne; this.nt = nt; this.nw = nw; this.pw = pw; this.truncated = truncated; this.pf = pf;this.maxLv = maxLv; this.fee = fee; this.bkFee = bkFee;this.conflict = conflict; this.ifApply = ifApply;this.book = book; this.dmg = dmg; this.ifStore = ifStore;this.eName = eName; this.tName = tName; }
            int maxLv(int e) { return maxLv[e]; }
            int aCost(int e) { return fee[e]; } // Anvil costs
            int baCost(int e) { return bkFee[e]; } // Blue Archive costs
            boolean ifBook(int ty) { return book[ty]; }
            boolean ifDamageable(int ty) { return dmg[ty]; }
            boolean ifStore(int ty) { return ifStore[ty]; }
            boolean ifApply(int ty, int e) { return (ifApply[ty * PRW + (e >>> 6)] >>> e & 1L) != 0; }
            boolean conflicts(int a, int b) { return (conflict[a * PRW + (b >>> 6)] >>> b & 1L) != 0; }
            String eName(int e) { return eName[e]; }
            String tName(int ty) { return tName[ty]; }
        }

        /** Lex Transformer */
        static final class LexTrans {
            final List<int[]> es = new ArrayList<>(), pairs = new ArrayList<>(), ts = new ArrayList<>(), allow = new ArrayList<>();
            final List<String> en = new ArrayList<>(), tn = new ArrayList<>();
            int enchant(String name, int maxLv, int aCost) { es.add(new int[]{ maxLv, aCost }); en.add(name); return es.size() - 1; }
            int type(String name, boolean ifBook, boolean damageable, boolean ifStore) { ts.add(new int[]{ ifBook ? 1 : 0, damageable ? 1 : 0, ifStore ? 1 : 0 }); tn.add(name); return ts.size() - 1; }
            LexTrans conflict(int a, int b) { if (a != b) pairs.add(new int[]{ a, b }); return this; }
            LexTrans allow(int type, int e) { allow.add(new int[]{ type, e }); return this; }
            Lexicon begin() {
                int ne = es.size(), nt = ts.size();
                if (ne > MAX) throw new IllegalStateException("too many enchantments: " + ne);
                boolean tr = false, pf = true;
                int[] ml = new int[ne], fee = new int[ne], bf = new int[ne];
                for (int e = 0; e < ne; e++) {
                    int[] s = es.get(e);
                    if (s[0] > MAX_LV) { ml[e] = MAX_LV; tr = true; } else ml[e] = s[0];
                    if (s[0] < 0) throw new IllegalArgumentException("negative max level");
                    if (s[1] < 0) throw new IllegalArgumentException("negative anvil cost");
                    fee[e] = s[1]; bf[e] = Math.max(1, s[1] / 2);
                    if (s[1] == 0) pf = false;
                }
                long[] con = new long[ne * PRW];
                for (int[] p : pairs) {
                    int a = p[0], b = p[1];
                    chk(a, ne); chk(b, ne);
                    con[a * PRW + (b >>> 6)] |= 1L << b; con[b * PRW + (a >>> 6)] |= 1L << a;
                }
                long[] ca = new long[nt * PRW];
                for (int[] a : allow) {
                    chk(a[1], ne); if (a[0] < 0 || a[0] >= nt) throw new IllegalArgumentException("bad type id");
                    ca[a[0] * PRW + (a[1] >>> 6)] |= 1L << a[1];
                }
                boolean[] bk = new boolean[nt], dmg = new boolean[nt], st = new boolean[nt];
                for (int i = 0; i < nt; i++) { int[] s = ts.get(i); bk[i] = s[0] != 0; dmg[i] = s[1] != 0; st[i] = s[2] != 0; }
                int nw = (ne + 15) >>> 4, pw = (ne + 63) >>> 6;
                return new Lexicon(ne, nt, nw, pw, tr, pf, ml, fee, bf, con, ca, bk, dmg, st, en.toArray(new String[0]), tn.toArray(new String[0]));
            }
            static void chk(int id, int ne) { if (id < 0 || id >= ne) throw new IllegalArgumentException("bad enchant id"); }
        }

        /** Registries lookup */
        static final class Pool {
            final Lexicon lex;
            long[] lv, pr, hsh;
            int[] pen, typ, tab;
            int n, cap, tMask;
            Pool(Lexicon lex, int cap) {
                this.lex = lex;
                int c = 16; while (c < cap) c <<= 1;
                this.cap = c;
                lv = new long[c * LVW]; pr = new long[c * PRW]; pen = new int[c]; typ = new int[c]; hsh = new long[c]; tab = new int[c * 2]; tMask = tab.length - 1;
            }
            int intern(long[] l, long[] p2, int pen, int type, boolean t) {
                if (pen < 0 || type < 0 || type >= lex.nt) throw new IllegalStateException("bad meta: pen=" + pen + " type=" + type);
                long m0 = lex.ne >= 64 ? -1L : (1L << lex.ne) - 1L, m1 = lex.ne <= 64 ? 0L : lex.ne >= 128 ? -1L : (1L << (lex.ne - 64)) - 1L;
                if ((p2[0] & ~m0) != 0L || (p2[1] & ~m1) != 0L) throw new IllegalStateException("dirty presence: ne=" + lex.ne + " pr=" + Long.toHexString(p2[0]) + "," + Long.toHexString(p2[1]));
                for (int i = lex.nw; i < LVW; i++) if (l[i] != 0L) throw new IllegalStateException("dirty levels beyond nw");
                for (int i = lex.pw; i < PRW; i++) if (p2[i] != 0L) throw new IllegalStateException("dirty presence beyond pw");
                long h = hs(l, p2, pen, type, t);
                int i = (int) h & tMask;
                while (tab[i] != 0) {
                    int id = tab[i] - 1;
                    if (hsh[id] == h && eq(id, l, p2, pen, type)) return id;
                    i = (i + 1) & tMask;
                }
                if (n == cap) { grow(); i = (int) h & tMask; while (tab[i] != 0) i = (i + 1) & tMask; }
                int id = n++;
                System.arraycopy(l, 0, lv, id * LVW, lex.nw);
                System.arraycopy(p2, 0, pr, id * PRW, lex.pw);
                this.pen[id] = pen; typ[id] = type; hsh[id] = h; tab[i] = id + 1;
                return id;
            }
            Lexicon lex() { return lex; }
            int size() { return n; }
            int level(int id, int e) { return (int) (lv[id * LVW + (e >>> 4)] >>> ((e & 15) << 2)) & 15; }
            int penalty(int id) { return pen[id]; }
            int type(int id) { return typ[id]; }
            boolean ifBook(int id) { return lex.book[typ[id]]; }
            View view(int id) { return new View(this, lex, id); }
            Pool fork() {
                Pool f = new Pool(lex, Math.max(16, n));
                System.arraycopy(lv, 0, f.lv, 0, n * LVW);
                System.arraycopy(pr, 0, f.pr, 0, n * PRW);
                System.arraycopy(pen, 0, f.pen, 0, n);
                System.arraycopy(typ, 0, f.typ, 0, n);
                System.arraycopy(hsh, 0, f.hsh, 0, n);
                f.n = n;
                for (int id = 0; id < n; id++) {
                    int i = (int) f.hsh[id] & f.tMask;
                    while (f.tab[i] != 0) i = (i + 1) & f.tMask;
                    f.tab[i] = id + 1;
                }
                return f;
            }
            boolean eq(int id, long[] l, long[] p2, int pen, int type) {
                int b = id * LVW;
                for (int i = 0; i < lex.nw; i++) if (lv[b + i] != l[i]) return false;
                b = id * PRW;
                for (int i = 0; i < lex.pw; i++) if (pr[b + i] != p2[i]) return false;
                return this.pen[id] == pen && typ[id] == type;
            }
            void grow() {
                cap <<= 1;
                lv = Arrays.copyOf(lv, cap * LVW); pr = Arrays.copyOf(pr, cap * PRW);
                pen = Arrays.copyOf(pen, cap); typ = Arrays.copyOf(typ, cap); hsh = Arrays.copyOf(hsh, cap);
                tab = new int[cap * 2]; tMask = tab.length - 1;
                for (int id = 0; id < n; id++) {
                    int i = (int) hsh[id] & tMask;
                    while (tab[i] != 0) i = (i + 1) & tMask;
                    tab[i] = id + 1;
                }
            }
            long hs(long[] l, long[] p2, int pen, int type, boolean t) {
                long h = 0x9E3779B97F4A7C15L;
                for (int i = 0; i < lex.nw; i++) h = BitVec.mix64(h ^ l[i]);
                for (int i = 0; i < lex.pw; i++) h = BitVec.mix64(h ^ p2[i]);
                h = BitVec.mix64(h ^ pen * 0xC2B2AE3D27D4EB4FL ^ type * 0x165667B19E3779F9L);
                return t ? h | Long.MIN_VALUE : h & Long.MAX_VALUE;
            }
        }

        /** View for pooled ids */
        static final class View {
            final Pool p; final Lexicon t;
            final int id;
            View(Pool p, Lexicon t, int id) { this.p = p; this.t = t; this.id = id; }
            int lv(int e) { return p.level(id, e); }
            int penalty() { return p.penalty(id); }
            int type() { return p.type(id); }
            boolean ifBook() { return p.ifBook(id); }
            int enchants() { return Long.bitCount(p.pr[id * PRW]) + Long.bitCount(p.pr[id * PRW + 1]); }
            String tName() { return t.tName[p.type(id)]; }
            void forEach(Visit v) {
                long a = p.pr[id * PRW], b = p.pr[id * PRW + 1];
                while (a != 0) { int e = Long.numberOfTrailingZeros(a); a &= a - 1; v.accept(e, p.level(id, e)); }
                while (b != 0) { int e = 64 + Long.numberOfTrailingZeros(b); b &= b - 1; v.accept(e, p.level(id, e)); }
            }
            interface Visit { void accept(int e, int lvl); }
        }

        /** Caches */
        static final class Cache {
            long[] k, meta, p;
            int[] st, cfa;
            int mask, cnt;
            final int pN, pW;
            Cache(int cap, int pN) {
                this.pN = pN; pW = pN == 0 ? 0 : (pN + 1) >> 1;
                int c = 16; while (c < cap) c <<= 1;
                k = new long[c]; meta = new long[c]; p = pW > 0 ? new long[c * pW] : null; st = new int[c]; cfa = new int[c];
                mask = c - 1;
            }
            boolean get(Probe p) {
                long key = p.key;
                int i = (int) BitVec.mix64(key) & mask;
                while (k[i] != 0) {
                    if (k[i] == key) {
                        int s = st[i]; long m = meta[i];
                        p.ok = m < 0; p.cost = (m >>> 32) & 0x7FFFFFFFL; p.newPenalty = (int) m;
                        int f = cfa[i];
                        p.conflicts = f >>> 16; p.newType = (f >>> 8) & 0xFF; p.upgraded = f & 0xFF; p.id = s >= 0 ? s : -1;
                        if (pN > 0) {
                            long w = this.p[i * pW];
                            if (w >= 0) p.p[pN] = 0L;
                            else {
                                p.p[pN] = 1L;
                                for (int j = 0, b = i * pW; j < pW; j++) {
                                    long v = this.p[b + j];
                                    p.p[j << 1] = (int) (v & 0x7FFFFFFFL) - (1 << 30);
                                    if ((j << 1 | 1) < pN) p.p[j << 1 | 1] = (int) (v >>> 32 & 0x7FFFFFFFL) - (1 << 30);
                                }
                            }
                        }
                        return true;
                    }
                    i = (i + 1) & mask;
                }
                return false;
            }
            int put(Probe p, int id) {
                int i = (int) BitVec.mix64(p.key) & mask;
                while (k[i] != 0) {
                    if (k[i] == p.key) { if (id >= 0 && st[i] < 0) st[i] = id; return i; }
                    i = (i + 1) & mask;
                }
                if ((cnt + 1) * 2 > mask) {
                    if ((mask + 1) >= (1 << 23)) return -1;
                    grow();
                    i = (int) BitVec.mix64(p.key) & mask;
                    while (k[i] != 0) i = (i + 1) & mask;
                }
                k[i] = p.key;
                meta[i] = (p.ok ? Long.MIN_VALUE : 0L) | ((p.cost & 0x7FFFFFFFL) << 32) | (p.newPenalty & 0xFFFFFFFFL);
                cfa[i] = (p.conflicts << 16) | (p.newType << 8) | p.upgraded;
                if (pN > 0) {
                    if (!p.ok || p.p[pN] == 0L) this.p[i * pW] = 0L;
                    else {
                        for (int j = 0, b = i * pW; j < pW; j++) {
                            long w = (p.p[j << 1] + (1L << 30)) & 0x7FFFFFFFL;
                            if ((j << 1 | 1) < pN) w |= ((p.p[j << 1 | 1] + (1L << 30)) & 0x7FFFFFFFL) << 32;
                            this.p[b + j] = w;
                        }
                        this.p[i * pW] |= Long.MIN_VALUE;
                    }
                }
                st[i] = id >= 0 ? id : -1; cnt++;
                return i;
            }
            void grow() {
                int c = k.length << 1;
                long[] k2 = new long[c]; long[] m2 = new long[c]; long[] p2 = pW > 0 ? new long[c * pW] : null;
                int[] s2 = new int[c]; int[] f2 = new int[c];
                int m = c - 1;
                for (int j = 0; j < k.length; j++) {
                    if (k[j] == 0) continue;
                    int i = (int) BitVec.mix64(k[j]) & m;
                    while (k2[i] != 0) i = (i + 1) & m;
                    k2[i] = k[j]; m2[i] = meta[j]; s2[i] = st[j]; f2[i] = cfa[j];
                    if (pW > 0) System.arraycopy(p, j * pW, p2, i * pW, pW);
                }
                k = k2; meta = m2; st = s2; cfa = f2; p = p2; mask = m;
            }
        }

        /** Binary tree */
        static final class Tr {
            final int n;
            final int[] l, r, p, leaf;
            final String[] name;
            Tr(int[] left, int[] right, int[] leafs, String[] names) {
                n = leafs.length;
                if (left.length != 2 * n - 1) throw new IllegalArgumentException("bad topology size");
                l = left.clone(); r = right.clone(); p = new int[2 * n - 1];
                leaf = leafs.clone();
                name = names == null ? null : Arrays.copyOf(names, n);
                Arrays.fill(p, -1);
                ref();
            }
            Tr(Tr o) { n = o.n; l = o.l.clone(); r = o.r.clone(); p = o.p.clone(); leaf = o.leaf.clone(); name = o.name == null ? null : o.name.clone(); }
            Tr copy() { return new Tr(this); }
            void copyFrom(Tr o) {
                int m = 2 * n - 1;
                System.arraycopy(o.l, 0, l, 0, m);
                System.arraycopy(o.r, 0, r, 0, m);
                System.arraycopy(o.p, 0, p, 0, m);
                System.arraycopy(o.leaf, 0, leaf, 0, n);
            }
            int root() { return 2 * n - 2; }
            boolean ifLeaf(int s) { return s < n; }
            String lName(int i) { return name == null ? "L" + i : name[i]; }
            String dName(int s) { while (s >= n) s = l[s]; return lName(s); }
            boolean ifAncestor(int a, int x) {
                for (int s = x; s >= 0; s = p[s]) if (s == a) return true;
                return false;
            }
            boolean swap(int a, int b) {
                int m = 2 * n - 1;
                if (a == b || a < 0 || b < 0 || a >= m || b >= m) return false;
                int pa = p[a], pb = p[b];
                if (pa < 0 || pb < 0) return false;
                if (pa == pb) { int t = l[pa]; l[pa] = r[pa]; r[pa] = t; return true; }
                if (ifAncestor(a, b) || ifAncestor(b, a)) return false;
                boolean al = l[pa] == a, bl = l[pb] == b;
                if (al) l[pa] = b; else r[pa] = b;
                if (bl) l[pb] = a; else r[pb] = a;
                p[a] = pb; p[b] = pa;
                return true;
            }
            void ref() {
                int[] ss = new int[n];
                int sp = 0; ss[sp++] = root();
                p[root()] = -1;
                while (sp > 0) {
                    int s = ss[--sp];
                    int lc = l[s], rc = r[s];
                    if (lc >= 0) { p[lc] = s; if (lc >= n) ss[sp++] = lc; }
                    if (rc >= 0) { p[rc] = s; if (rc >= n) ss[sp++] = rc; }
                }
            }
            void postOrder(int[] out) { postOrder(out, new int[2 * n]); }
            void postOrder(int[] out, int[] ss) {
                if (n == 1) return;
                int sp = 0, oi = n - 1;
                ss[sp++] = 2 * n - 2;
                while (sp > 0) {
                    int s = ss[--sp];
                    out[--oi] = s;
                    if (l[s] >= n) ss[sp++] = l[s];
                    if (r[s] >= n) ss[sp++] = r[s];
                }
            }
        }

        /** Node evaluation */
        static final class Eval {
            final int[] ids, aStep, conflicts;
            boolean feasible;
            long total = -1;
            Eval(int slots) { ids = new int[slots]; aStep = new int[slots]; conflicts = new int[slots]; }
            Eval copy() {
                Eval c = new Eval(ids.length);
                System.arraycopy(ids, 0, c.ids, 0, ids.length);
                System.arraycopy(aStep, 0, c.aStep, 0, aStep.length);
                System.arraycopy(conflicts, 0, c.conflicts, 0, conflicts.length);
                c.feasible = feasible; c.total = total;
                return c;
            }
        }

        /** Sequence for LNS */
        static final class RefSeq {
            final int[] lu, ru;
            RefSeq(int i) { lu = new int[i]; ru = new int[i]; }
            static RefSeq of(Tr tr, Eval ev) {
                int n = tr.n;
                RefSeq rs = new RefSeq(n - 1);
                int[] post = new int[n - 1];
                tr.postOrder(post);
                for (int i = 0; i < n - 1; i++) {
                    int s = post[i];
                    rs.lu[i] = ev.ids[tr.l[s]]; rs.ru[i] = ev.ids[tr.r[s]];
                }
                return rs;
            }
            static int at(WSet ws, int id, int e) { return ws.at(id, e); }
        }

        /** Sdp Mem */
        static final class Sdp {
            static volatile Sdp inst = new Sdp(32);
            final int n; final long[] sdp; final long[][] f2;
            Sdp(int n) {
                this.n = n;
                final long INF = Long.MAX_VALUE >>> 2;
                sdp = new long[n + 1];
                for (int s = 2; s <= n; s++) {
                    long bs = INF;
                    for (int a = 1; a < s; a++) {
                        int mx = Math.max(a, s - a);
                        long rl = (1L << (32 - Integer.numberOfLeadingZeros(mx - 1) + 1)) - 1, v = sdp[a] + sdp[s - a] + rl;
                        if (v < bs) bs = v;
                    }
                    sdp[s] = bs;
                }
                f2 = new long[n][n + 1];
                for (long[] rw : f2) Arrays.fill(rw, INF);
                f2[0][0] = 0;
                for (int q = 0; q < n; q++) for (int cc = 0; cc <= q; cc++) {
                    if (f2[q][cc] >= INF) continue;
                    for (int s = 2; q + s - 1 < n; s++) {
                        long nv = f2[q][cc] + sdp[s];
                        if (nv < f2[q + s - 1][cc + 1]) f2[q + s - 1][cc + 1] = nv;
                    }
                }
            }
            static Sdp at(int need) {
                Sdp s = inst;
                if (s.n >= need) return s;
                synchronized (Sdp.class) {
                    s = inst;
                    if (s.n < need) inst = s = new Sdp(Math.max(need, s.n << 1));
                }
                return s;
            }
            static long chain(long p0, int m) {
                if (m == 1) return 0;
                if (m > 62 || (p0 + 1L) > (Long.MAX_VALUE >> m)) return Long.MAX_VALUE >>> 2;
                return (p0 + 1L) * ((1L << m) - 2) - (m - 1);
            }
        }

        /** Dynamic Set */
        static final class WSet {
            final int n;
            final Lock lc;
            final int[] liveS, liveV, L, R;
            int[] idCnt, ipx, spx;
            long[] p;
            int cnt, cur, msh2;
            long cost, msh;
            WSet(Pool pool, Lock lc, int[] ss) { this(pool, lc, ss, ss.length); }
            WSet(Pool pool, Lock lc, int[] ss, int len) {
                n = len; this.lc = lc;
                int m = 2 * n - 1;
                liveS = new int[n]; liveV = new int[n];
                L = new int[m]; R = new int[m];
                Arrays.fill(L, -1); Arrays.fill(R, -1);
                int mx = 0, h2 = 0; long h = 0;
                for (int i = 0; i < n; i++) {
                    liveS[i] = i; liveV[i] = ss[i]; h += s64(ss[i]); h2 += s32(ss[i]);
                    if (ss[i] > mx) mx = ss[i];
                }
                idCnt = new int[mx + 1]; ipx = new int[mx + 1]; Arrays.fill(ipx, -1); spx = new int[2 * n - 1];
                for (int i = 0; i < n; i++) { idCnt[ss[i]]++; if (ipx[ss[i]] < 0) ipx[ss[i]] = i; spx[liveS[i]] = i; }
                cnt = n; cur = n; msh = h; msh2 = h2;
                if (lc == null) p = null;
                else {
                    p = new long[lc.ev.length];
                    for (int k = 0; k < lc.ev.length; k++) {
                        long s2 = 0;
                        for (int i = 0; i < n; i++) s2 += Lock.lvw(pool, ss[i], lc.ev[k] >>> 4);
                        p[k] = s2;
                    }
                }
            }
            WSet(WSet o) {
                n = o.n; lc = o.lc;
                liveS = o.liveS.clone(); liveV = o.liveV.clone();
                L = o.L.clone(); R = o.R.clone();
                idCnt = o.idCnt.clone(); ipx = o.ipx.clone(); spx = o.spx.clone();
                p = o.p == null ? null : o.p.clone();
                cnt = o.cnt; cur = o.cur; cost = o.cost; msh = o.msh; msh2 = o.msh2;
            }
            WSet copy() { return new WSet(this); }
            void copyFrom(WSet o) {
                System.arraycopy(o.liveS, 0, liveS, 0, n);
                System.arraycopy(o.liveV, 0, liveV, 0, n);
                System.arraycopy(o.L, 0, L, 0, 2 * n - 1);
                System.arraycopy(o.R, 0, R, 0, 2 * n - 1);
                if (idCnt.length < o.idCnt.length) { idCnt = new int[o.idCnt.length * 2]; ipx = new int[idCnt.length]; }
                System.arraycopy(o.idCnt, 0, idCnt, 0, o.idCnt.length);
                Arrays.fill(idCnt, o.idCnt.length, idCnt.length, 0);
                System.arraycopy(o.ipx, 0, ipx, 0, o.idCnt.length);
                System.arraycopy(o.spx, 0, spx, 0, spx.length);
                if (p != null) System.arraycopy(o.p, 0, p, 0, p.length);
                cnt = o.cnt; cur = o.cur; cost = o.cost; msh = o.msh; msh2 = o.msh2;
            }
            void take(int i, int j, int dir, int nid, long[] d) {
                int s = cur++;
                L[s] = dir == 0 ? liveS[i] : liveS[j];
                R[s] = dir == 0 ? liveS[j] : liveS[i];
                int u = liveV[i], v = liveV[j];
                msh += s64(nid) - s64(u) - s64(v); msh2 += s32(nid) - s32(u) - s32(v);
                if (p != null && d[p.length] != 0L) for (int k = 0; k < p.length; k++) p[k] += d[k];
                if (nid >= idCnt.length) {
                    int ol = idCnt.length;
                    idCnt = Arrays.copyOf(idCnt, Math.max(nid + 1, idCnt.length * 2)); ipx = Arrays.copyOf(ipx, idCnt.length);
                    Arrays.fill(ipx, ol, ipx.length, -1);
                }
                idCnt[u]--; idCnt[v]--; idCnt[nid]++;
                liveS[i] = s; liveV[i] = nid; liveS[j] = liveS[cnt - 1]; liveV[j] = liveV[cnt - 1];
                cnt--;
            }
            int pos(int sp) {
                int c = spx[sp];
                if (c >= 0 && c < cnt && liveS[c] == sp) return c;
                for (int i = 0; i < cnt; i++) if (liveS[i] == sp) { spx[sp] = i; return i; }
                return -1;
            }
            int at(int id, int e) {
                if (id < ipx.length && idCnt[id] == 1) {
                    int c = ipx[id];
                    if (c >= 0 && c < cnt && c != e && liveV[c] == id) return c;
                    for (int i = 0; i < cnt; i++) if (i != e && liveV[i] == id) { ipx[id] = i; return i; }
                    return -1;
                }
                for (int i = 0; i < cnt; i++) if (i != e && liveV[i] == id) return i;
                return -1;
            }
            Tr trs(int[] leafs, String[] names) { return new Tr(L, R, leafs, names); }
            static long s64(int id) { return BitVec.mix64((id + 1) * 0x9E3779B97F4A7C15L); }
            static int s32(int id) { return (int) BitVec.mix64((id + 1) * 0xD1B54A32D192ED03L); }
        }

        /** Generation Set */
        static final class PairSeen {
            final long[] k; final int[] g; final int mask; int gen;
            PairSeen(int cap) { int c = 1024; while (c < cap) c <<= 1; k = new long[c]; g = new int[c]; mask = c - 1; }
            void round() { gen++; }
            boolean seen(long key) {
                int i = (int) BitVec.mix64(key) & mask;
                while (g[i] == gen) {
                    if (k[i] == key) return true;
                    i = (i + 1) & mask;
                }
                k[i] = key; g[i] = gen;
                return false;
            }
        }
    }
}