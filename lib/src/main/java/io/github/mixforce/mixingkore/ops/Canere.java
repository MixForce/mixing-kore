// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.ops;

import io.github.mixforce.mixingkore.mathx.BitVec;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
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

public final class Canere {
    private Canere() {}

    public static void extract(ItemStack s, Object2IntMap<Holder<Enchantment>> out) {
        out.clear();
        if (s.isEmpty()) return;
        for (var e : exa(s).entrySet()) out.put(e.getKey(), e.getIntValue());
    }
    public static int level(ItemStack s, ResourceKey<Enchantment> k) {
        if (s.isEmpty()) return 0;
        for (var e : exa(s).entrySet())
            if (e.getKey().is(k)) return e.getIntValue();
        return 0;
    }
    public static int level(Object2IntMap<Holder<Enchantment>> m, ResourceKey<Enchantment> k) {
        for (var e : Object2IntMaps.fastIterable(m))
            if (e.getKey().is(k)) return e.getIntValue();
        return 0;
    }
    public static boolean ifAll(ItemStack s, Collection<ResourceKey<Enchantment>> ks) {
        if (s.isEmpty() || ks.isEmpty()) return false;
        ItemEnchantments en = exa(s);
        for (ResourceKey<Enchantment> k : ks) {
            boolean f = false;
            for (var e : en.entrySet())
                if (e.getKey().is(k)) { f = true; break; }
            if (!f) return false;
        }
        return true;
    }
    @SafeVarargs
    public static boolean ifAll(ItemStack s, ResourceKey<Enchantment>... keys) { return ifAll(s, List.of(keys)); }
    public static boolean ifAny(ItemStack s, Collection<ResourceKey<Enchantment>> ks) {
        if (s.isEmpty() || ks.isEmpty()) return false;
        for (var e : exa(s).entrySet())
            for (ResourceKey<Enchantment> k : ks)
                if (e.getKey().is(k)) return true;
        return false;
    }
    @SafeVarargs
    public static boolean ifAny(ItemStack s, ResourceKey<Enchantment>... ks) { return ifAny(s, List.of(ks)); }
    public static boolean ifHas(ItemStack s, ResourceKey<Enchantment> k) {
        if (s.isEmpty()) return false;
        for (var e : exa(s).entrySet())
            if (e.getKey().is(k)) return true;
        return false;
    }
    public static boolean ifHas(Object2IntMap<Holder<Enchantment>> m, ResourceKey<Enchantment> k) {
        for (var e : Object2IntMaps.fastIterable(m))
            if (e.getKey().is(k)) return true;
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
            is = new int[ts.size()];
            leafs = new String[ts.size()];
            for (int i = 0; i < ts.size(); i++) {
                ItemStack s = ts.get(i);
                Arrays.fill(lv, 0L); Arrays.fill(pr, 0L);
                for (var en : es.get(i).object2IntEntrySet()) {
                    int e = idx.get(en.getKey()), l2 = en.getIntValue();
                    lv[e >>> 4] |= (long) l2 << ((e & 15) << 2);
                    if (e < 64) pr[0] |= 1L << e; else pr[1] |= 1L << e;
                }
                int ty = s.getItem() == Items.ENCHANTED_BOOK ? 0 : 1;
                is[i] = pool.intern(lv, pr, s.getOrDefault(DataComponents.REPAIR_COST, 0), ty);
                leafs[i] = i == 0 ? "T" : (ty == 0 ? "B" : s.getHoverName().getString());
            }
        }
        public void config(Optimizer.Config config) { cfg = config; }
        public List<String> warnings() {
            if (cfg == null) return List.of();
            List<String> out = new ArrayList<>(warns);
            int rm = 0;
            for (int i = 1; i < is.length; i++) if (!Optimizer.Prepare.usable(pool, lex, cfg.policy, is[0], is[i])) { rm++; }
            if (rm > 0) out.add("Consumed " + rm + " useless material" + (rm > 1 ? "s" : "") + ".");
            return out;
        }
        public Optimizer.Solution optimize() {
            return Optimizer.explore(pool, is[0], tail(), leafs, cfg);
        }
        public List<Step> trace(Optimizer.Solution s) {
            if (!s.feasible()) return List.of();
            List<Step> out = new ArrayList<>();
            walk(s.tr(), s.eval(), s.tr().root(), out, new long[1]);
            return out;
        }
        private Map<Holder<Enchantment>, Integer> enc(int id) {
            Map<Holder<Enchantment>, Integer> m = new LinkedHashMap<>();
            pool.view(id).forEach((e, l) -> m.put(enc.get(e), l));
            return m;
        }
        private long walk(Optimizer.Tr p, Optimizer.Eval ev, int slot, List<Step> out, long[] acc) {
            if (p.ifLeaf(slot)) return 0;
            long sub = walk(p, ev, p.l[slot], out, acc) + walk(p, ev, p.r[slot], out, acc) + ev.aStep[slot];
            acc[0] += ev.aStep[slot];
            out.add(new Step(p.dName(p.l[slot]), p.dName(p.r[slot]), p.dName(slot), enc(ev.ids[p.l[slot]]), enc(ev.ids[p.r[slot]]), enc(ev.ids[slot]), ev.aStep[slot], acc[0], ev.conflicts[slot]));
            return sub;
        }
        private int[] tail() {
            int[] m = new int[is.length - 1];
            System.arraycopy(is, 1, m, 0, m.length);
            return m;
        }
    }

    public static final class Optimizer {
        private Optimizer() {}

        // Bits
        static final int MAX = 128, LVW = 8, PRW = 2, MAX_LV = 15;

        /**
         * @see net.minecraft.world.inventory.AnvilMenu
         * クリエイティブ以外デフォルトルール
         */
        static final class Ref {
            static void merge(Pool pool, Lexicon lex, Policy pol, int l, int r, Probe p) {
                p.id = -1; p.ok = false; p.cost = 0; p.conflicts = 0; p.applied = 0; p.upgraded = 0; p.newPenalty = -1;
                int tl = pool.typ[l], tr = pool.typ[r];
                p.newType = tl;
                if (!lex.ifStore[tl]) return;
                boolean rb = lex.book[tr];
                if (!rb && (tr != tl || !lex.dmg[tl])) return;
                Int2IntLinkedOpenHashMap m = new Int2IntLinkedOpenHashMap();
                for (int w = 0; w < PRW; w++) {
                    long x = pool.pr[l * PRW + w];
                    while (x != 0) { int e = (w << 6) + Long.numberOfTrailingZeros(x); x &= x - 1; m.put(e, pool.level(l, e)); }
                }
                long cost = (long) pool.pen[l] + pool.pen[r];
                boolean any = false; int cf = 0, ap = 0, upg = 0;
                for (int w = 0; w < PRW; w++) {
                    long x = pool.pr[r * PRW + w];
                    while (x != 0) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x); x &= x - 1;
                        boolean bl = lex.book[tl] || lex.ifApply(tl, e);
                        for (int o : m.keySet()) if (o != e && lex.conflicts(e, o)) { cost++; cf++; bl = false; }
                        if (!bl) continue;
                        any = true; ap++;
                        int cur = m.get(e), rl = pool.level(r, e);
                        int nl = Math.min(cur == rl ? rl + 1 : Math.max(cur, rl), lex.maxLv[e]);
                        if (nl > cur) upg++;
                        m.put(e, nl);
                        cost += (long) (rb ? lex.bkFee[e] : lex.fee[e]) * nl;
                    }
                }
                p.ok = any && cost < pol.limit;
                p.cost = cost; p.conflicts = cf; p.applied = ap; p.upgraded = upg;
                p.newPenalty = (int) Math.min((long) Math.max(pool.pen[l], pool.pen[r]) * 2 + 1, Integer.MAX_VALUE);
                Arrays.fill(p.lv, 0L); Arrays.fill(p.pr, 0L);
                for (var en : m.int2IntEntrySet()) {
                    int e = en.getIntKey(), sh = (e & 15) << 2, w2 = e >>> 4;
                    p.lv[w2] |= (long) en.getIntValue() << sh;
                    if (w2 == 0) p.pr[0] |= 1L << e; else p.pr[1] |= 1L << e;
                }
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

        /** Config */
        public record Policy(int limit) {
            public Policy { if (limit < 1) throw new IllegalArgumentException("limit not valid"); }
            static final Policy VANILLA = new Policy(40);
            static final Policy UNLIMITED = new Policy(5000);
        }
        public static final class Config {
            public int greedy = 8, topK = 3, bWidth = 24, bbLeaves = 14, lStall = 300, pIsm = 0; // 0 auto
            public float bLambda = 1.0f;
            public boolean beam = true, lns = true, bb = false, auto = false;
            public long seed = 42, budget = 0 /* Ns */, bbNodes = 2000000L, bbGap = 8;
            public Policy policy = Policy.VANILLA;
            /** Just adjusts configurations */
            static void tune(Config cfg, int[] ss) {
                if (!cfg.auto) return;
                int[] c = ss.clone();
                Arrays.sort(c);
                int m = 0;
                for (int i = 0; i < c.length; i++) if (i == 0 || c[i] != c[i - 1]) m++;
                if (m <= 10) {
                    if (cfg.greedy < 8) cfg.greedy = 8;
                    if (cfg.beam) cfg.beam = false;
                    if (!cfg.lns) cfg.lns = true;
                } else if (m > 40) {
                    if (!cfg.lns) cfg.lns = true;
                    if (!cfg.beam) cfg.beam = true;
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

        record Metrics(long greedy, long beam, long lns, long bb, int reports, long lb0, long lbEst, double gap, boolean proven) {}
        private record WRes(Tr tr, long gN, long bN, long lN, int rep) {}

        /** Solver facade */
        static Solution explore(Pool pool, int t, int[] mats, String[] ls, Config cfg) {
            long t0 = System.nanoTime();
            Vanilla k = new Vanilla(pool.lex(), pool, cfg.policy);
            Prepare.Prepared prep = Prepare.apply(pool, cfg.policy, t, mats, Arrays.copyOfRange(ls, 1, ls.length));
            if (!prep.ok) return new Solution(null, null, false, 0, System.nanoTime() - t0,  null, prep.fail);
            int nm = prep.ids().length;
            int[] all = new int[nm + 1]; all[0] = t;
            System.arraycopy(prep.ids(), 0, all, 1, nm);
            String[] names = new String[nm + 1]; names[0] = ls[0];
            System.arraycopy(prep.names(), 0, names, 1, nm);
            Config.tune(cfg, all);
            long lb0 = new Floor(pool, cfg.policy.limit).compute(new WSet(all), pool.type(all[0]));
            if (lb0 < 0) return new Solution(null, null, false, 0, System.nanoTime() - t0, new Metrics(0, 0, 0, 0, 0, -1, -1, Double.NaN, false), "Infeasible.");
            Evaluator ev = new Evaluator(k);
            Ceiling ub = new Ceiling();
            long tGreedy, tBeam, tLns;
            int reports, dn;
            int nw = cfg.pIsm > 0 ? Math.clamp(cfg.greedy, 1, cfg.pIsm) : Runtime.getRuntime().availableProcessors();
            if (all.length == 1) nw = 1;
            int sW = Math.max(20, (int) (cfg.lStall / Math.sqrt(nw)));
            long deadline = cfg.budget > 0 ? t0 + cfg.budget : 0;
            WRes[] res = new WRes[nw];
            ExecutorService ex = Executors.newFixedThreadPool(nw, r0 -> {
                Thread th = new Thread(r0, "mixing-optimizer");
                th.setDaemon(true);
                return th;
            });
            try {
                Future<?>[] fs = new Future[nw];
                for (int wk = 0; wk < nw; wk++) {
                    final int w = wk;
                    Pool fp = pool.fork();
                    fs[wk] = ex.submit(() -> res[w] = worker(fp, t, prep.ids(), names, cfg, w, sW, deadline));
                }
                for (Future<?> f : fs) {
                    try { f.get(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    catch (ExecutionException e) { e.getCause().printStackTrace(); }
                }
            } finally { ex.shutdown(); }
            long wG = 0, wB = 0, wL = 0; int rep = 0, ok = 0;
            for (WRes r : res) {
                if (r == null) continue;
                rep += r.rep; wG = Math.max(wG, r.gN); wB = Math.max(wB, r.bN); wL = Math.max(wL, r.lN);
                if (r.tr == null) continue;
                Eval e = ev.evaluate(r.tr, null);
                if (e.feasible) { ub.report(r.tr, e); ok++; }
            }
            tGreedy = wG; tBeam = wB; tLns = wL; reports = rep; dn = ok;
            if (!ub.found()) {
                Metrics st = new Metrics(tGreedy, tBeam, tLns, 0, reports, lb0, -1, Double.NaN, false);
                return new Solution(null, null, false, dn, System.nanoTime() - t0, st, "All " + dn + " failed.");
            }
            Height he = Height.of(pool, t, prep.ids());
            long lb = he.lb; // -1
            boolean se = lb >= 0;
            long al = se ? Math.max(0, ub.cost - lb) : Long.MIN_VALUE;
            double gap = se ? (ub.cost > 0 ? (double) al / ub.cost : 0.0) : (ub.cost > 0 ? (double) (ub.cost - lb0) / ub.cost : Double.NaN);
            boolean proven = se && ub.cost == lb;
            long tBb = 0;
            if (se && !proven && nm >= 2 && cfg.bb && nm <= cfg.bbLeaves && al <= cfg.bbGap && (deadline == 0 || deadline - System.nanoTime() > (deadline - t0) / 4)) {
                long tb0 = System.nanoTime();
                proven = BranchBound.con(pool, k, ev, all, names, cfg.bbNodes, deadline, cfg.bLambda, ub);
                tBb = System.nanoTime() - tb0;
                if (proven) gap = 0.0;
                else { al = Math.max(0, ub.cost - lb); gap = (double) al / ub.cost; }
            }
            Metrics st = new Metrics(tGreedy, tBeam, tLns, tBb, reports, lb0, lb, gap, proven);
            return new Solution(ub.tr, ub.eval, true, dn, System.nanoTime() - t0, st, null);
        }
        static WRes worker(Pool pool, int t, int[] mats, String[] names, Config cfg, int wk, int sW, long deadline) {
            Vanilla k = new Vanilla(pool.lex(), pool, cfg.policy);
            Evaluator ev = new Evaluator(k);
            Floor fr = new Floor(pool, cfg.policy.limit);
            int[] a = new int[mats.length + 1]; a[0] = t;
            System.arraycopy(mats, 0, a, 1, mats.length);
            long q1 = cfg.budget > 0 ? deadline - cfg.budget * 3 / 4 : 0, q2 = cfg.budget > 0 ? deadline - cfg.budget / 2 : 0;
            Ceiling ub = new Ceiling();
            long seed = cfg.seed ^ (wk + 1) * 0x9E3779B97F4A7C15L;
            long tg0 = System.nanoTime();
            for (int s = 0; s < cfg.greedy; s++) {
                if (q1 > 0 && System.nanoTime() > q1) break;
                Greedy.GRes g = s == 0 ? Greedy.bkCon(pool, k, a) : Greedy.con(pool, k, a, Scorer.PRESETS[(s - 1 + wk) % Scorer.PRESETS.length], Math.max(1, cfg.topK), new Random(seed ^ s * 0x9E3779B97F4A7C15L));
                if (!g.ok) continue;
                Tr tr = new Tr(g.l, g.r, a, names);
                Eval e = ev.evaluate(tr, null);
                if (e.feasible) ub.report(tr, e);
            }
            long tG = System.nanoTime() - tg0;
            long tb0 = System.nanoTime();
            if (cfg.beam) Beam.con(pool, k, ev, fr, a, names, cfg.bWidth, cfg.bLambda, q2, ub);
            long tB = System.nanoTime() - tb0;
            long tl0 = System.nanoTime();
            if (cfg.lns) Lns.con(pool, k, ev, fr, a, names, deadline, sW, seed ^ 0x5DEECE66DL, ub);
            long tL = System.nanoTime() - tl0;
            return new WRes(ub.tr, tG, tB, tL, ub.version);
        }

        /** GRASP greedy */
        static final class Greedy {
            record GRes(int[] l, int[] r, boolean ok, int active, long cost, WSet ws) {}
            static final class Ctx {
                long[] hp = new long[256]; int hs;
                int[] sqU = new int[256], sqV = new int[256]; int sqN;
                int[] mA = new int[64], mB = new int[64]; int mN;
                float[] cwB = new float[0];
                PairSeen seen; int seenK;
                final Probe pb = new Probe();
                final int[] pkU = new int[8], pkV = new int[8], pkO = new int[8];
                void reset() { hs = 0; mN = 0; sqN = 0; }
                float score(Pool pool, Scorer sc, int u, int v, Probe pb) {
                    float s = sc.cost * pb.cost + sc.penNew * pb.newPenalty + sc.conf * pb.conflicts - sc.upg * pb.upgraded;
                    if (sc.penSum != 0) s += sc.penSum * (pool.penalty(u) + pool.penalty(v));
                    if (sc.cw != 0) {
                        cwB = cw(pool, cwB, u); cwB = cw(pool, cwB, v);
                        if (sc.cw > 0) s += sc.cw * (cwB[u] + cwB[v]);
                        else if (pool.penalty(u) == 0 && pool.penalty(v) == 0 && pool.ifBook(u) && pool.ifBook(v)) s += sc.cw * cwB[v];
                    }
                    return s;
                }
            }
            static GRes con(Pool pool, Vanilla k, int[] ss, Scorer sc, int topK, Random r) {
                return con(pool, k, ss, ss.length, sc, topK, r, new Ctx());
            }
            static GRes con(Pool pool, Vanilla k, int[] ss, int len, Scorer sc, int topK, Random r, Ctx g) {
                if (topK < 1) topK = 1;
                if (topK > 8) topK = 8;
                g.reset();
                if (g.seenK < len) { g.seen = new PairSeen(4 * len * len); g.seenK = len; }
                WSet ws = new WSet(ss, len);
                int n = len;
                g.seen.round();
                for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                    int u = ws.liveV[i], v = ws.liveV[j];
                    if (u == v) {
                        if (ws.idCnt[u] >= 2 && !g.seen.seen(pkey(u, v))) {
                            k.probe(u, v, g.pb);
                            if (g.pb.ok) push(g, g.score(pool, sc, u, v, g.pb), u, v);
                        }
                        continue;
                    }
                    if (!g.seen.seen(pkey(u, v))) {
                        k.probe(u, v, g.pb);
                        if (g.pb.ok) push(g, g.score(pool, sc, u, v, g.pb), u, v);
                    }
                    if (!g.seen.seen(pkey(v, u))) {
                        k.probe(v, u, g.pb);
                        if (g.pb.ok) push(g, g.score(pool, sc, v, u, g.pb), v, u);
                    }
                }
                long total = 0;
                while (ws.cnt > 1) {
                    int pu, pv, pord;
                    if (topK == 1) {
                        pu = -1; pv = -1; pord = 0;
                        while (g.hs > 0) {
                            long key = pop(g);
                            int seq = (int) key;
                            int u = g.sqU[seq], v = g.sqV[seq];
                            if (!livePair(ws, u, v)) continue;
                            k.probe(u, v, g.pb);
                            if (!g.pb.ok) continue;
                            pu = u; pv = v; pord = (int) (key >>> 32);
                            break;
                        }
                        if (pu < 0) return new GRes(null, null, false, ws.cnt, -1, ws);
                    } else {
                        int fi = 0;
                        while (g.hs > 0 && fi < topK) {
                            long key = pop(g);
                            int seq = (int) key;
                            int u = g.sqU[seq], v = g.sqV[seq];
                            if (!livePair(ws, u, v)) continue;
                            g.pkU[fi] = u; g.pkV[fi] = v; g.pkO[fi] = (int) (key >>> 32);
                            fi++;
                        }
                        if (fi == 0) return new GRes(null, null, false, ws.cnt, -1, ws);
                        int pick = fi == 1 ? 0 : r.nextInt(fi);
                        pu = g.pkU[pick]; pv = g.pkV[pick]; pord = g.pkO[pick];
                        for (int t2 = 0; t2 < fi; t2++) if (t2 != pick) { pushRaw(g, g.pkO[t2], g.pkU[t2], g.pkV[t2]); }
                    }
                    k.probe(pu, pv, g.pb);
                    if (!g.pb.ok) continue;
                    int nid = k.commit(g.pb);
                    total += g.pb.cost;
                    if (g.mN == g.mA.length) {
                        g.mA = Arrays.copyOf(g.mA, g.mN * 2); g.mB = Arrays.copyOf(g.mB, g.mN * 2);
                    }
                    g.mA[g.mN] = pu; g.mB[g.mN] = pv; g.mN++;
                    int pp = RefSeq.find(ws, pu, -1), pq = RefSeq.find(ws, pv, pp);
                    if (pp < 0 || pq < 0) return new GRes(null, null, false, ws.cnt, -1, ws);
                    if (pp < pq) ws.take(pp, pq, 0, nid); else ws.take(pq, pp, 1, nid);
                    if (livePair(ws, pu, pv)) pushRaw(g, pord, pu, pv);
                    g.seen.round();
                    for (int p2 = 0; p2 < ws.cnt; p2++) {
                        int x2 = ws.liveV[p2];
                        if (!g.seen.seen(pkey(nid, x2))) {
                            k.probe(nid, x2, g.pb);
                            if (g.pb.ok) push(g, g.score(pool, sc, nid, x2, g.pb), nid, x2);
                        }
                        if (x2 != nid && !g.seen.seen(pkey(x2, nid))) {
                            k.probe(x2, nid, g.pb);
                            if (g.pb.ok) push(g, g.score(pool, sc, x2, nid, g.pb), x2, nid);
                        }
                    }
                }
                return new GRes(ws.L, ws.R, true, 1, total, ws);
            }
            static boolean livePair(WSet ws, int u, int v) {
                int cu = ws.idCnt[u], cv = ws.idCnt[v];
                return cu > 0 && cv > 0 && (u != v || cu >= 2);
            }
            static long pkey(int u, int v) { return ((long) u << 32 | v) + 1L; }
            static void push(Ctx g, float s, int u, int v) {
                int bits = Float.floatToRawIntBits(s);
                pushRaw(g, bits ^ ((bits >> 31) | 0x80000000), u, v);
            }
            static void pushRaw(Ctx g, int ord, int u, int v) {
                if (g.sqN == g.sqU.length) {
                    g.sqU = Arrays.copyOf(g.sqU, g.sqN * 2);
                    g.sqV = Arrays.copyOf(g.sqV, g.sqN * 2);
                }
                long key = ((long) ord << 32) | g.sqN;
                g.sqU[g.sqN] = u; g.sqV[g.sqN] = v; g.sqN++;
                if (g.hs == g.hp.length) g.hp = Arrays.copyOf(g.hp, g.hs * 2);
                int i = g.hs++;
                while (i > 0) {
                    int p2 = (i - 1) >> 1;
                    if (g.hp[p2] <= key) break;
                    g.hp[i] = g.hp[p2]; i = p2;
                }
                g.hp[i] = key;
            }
            static long pop(Ctx g) {
                long top = g.hp[0];
                long last = g.hp[--g.hs];
                int i = 0;
                while (true) {
                    int c = 2 * i + 1;
                    if (c >= g.hs) break;
                    if (c + 1 < g.hs && g.hp[c + 1] < g.hp[c]) c++;
                    if (g.hp[c] >= last) break;
                    g.hp[i] = g.hp[c]; i = c;
                }
                if (g.hs > 0) g.hp[i] = last;
                return top;
            }
            static float[] cw(Pool pool, float[] buf, int id) {
                if (id >= buf.length) buf = Arrays.copyOf(buf, Math.max(pool.size(), id + 1));
                if (buf[id] != 0) return buf;
                Lexicon t = pool.lex();
                float[] v = {pool.penalty(id)};
                boolean bk = pool.ifBook(id);
                pool.view(id).forEach((e, lv) -> v[0] += (bk ? t.baCost(e) : t.aCost(e)) * lv);
                buf[id] = v[0];
                return buf;
            }
            // Book first heuristic
            static GRes bkCon(Pool pool, Vanilla k, int[] ss) {
                WSet ws = new WSet(ss);
                int n = ss.length;
                if (n == 1) return new GRes(ws.L, ws.R, true, 1, 0, ws);
                Probe pb = new Probe();
                if (pool.ifBook(ss[0])) {
                    int[] r = new int[n - 1];
                    for (int i = 1; i < n; i++) r[i - 1] = i;
                    sortCw(pool, ss, r, n - 1, -1);
                    int tgt = 0;
                    for (int i = 0; i < r.length; i++) {
                        tgt = merge(k, ws, pb, ws.pos(tgt), ws.pos(r[i]));
                        if (tgt < 0) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                    }
                    return new GRes(ws.L, ws.R, true, 1, ws.cost, ws);
                }
                int[] bks = new int[n], nbks = new int[n]; int nb = 0, nt = 0;
                for (int i = 1; i < n; i++) { if (pool.ifBook(ss[i])) bks[nb++] = i; else nbks[nt++] = i; }
                sortCw(pool, ss, bks, nb, -1);
                sortCw(pool, ss, nbks, nt, 1);
                int bkHead = -1;
                for (int b = 0; b < nb; b++) {
                    int pos = ws.pos(bks[b]);
                    if (bkHead < 0) { bkHead = ws.liveS[pos]; continue; }
                    bkHead = merge(k, ws, pb, ws.pos(bkHead), pos);
                    if (bkHead < 0) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                }
                int[] heads = new int[nt]; int nh = 0; int g = -1, gHead = -1;
                for (int t2 = 0; t2 < nt; t2++) {
                    int ty = pool.type(ss[nbks[t2]]);
                    if (ty != g) {
                        if (gHead >= 0) heads[nh++] = gHead;
                        g = ty; gHead = ws.liveS[ws.pos(nbks[t2])];
                        continue;
                    }
                    gHead = merge(k, ws, pb, ws.pos(gHead), ws.pos(nbks[t2]));
                    if (gHead < 0) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                }
                if (gHead >= 0) heads[nh++] = gHead;
                sortHeads(pool, ws, heads, nh);
                int tgt = 0;
                if (bkHead >= 0) {
                    tgt = merge(k, ws, pb, ws.pos(tgt), ws.pos(bkHead));
                    if (tgt < 0) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                }
                for (int h2 = 0; h2 < nh; h2++) {
                    tgt = merge(k, ws, pb, ws.pos(tgt), ws.pos(heads[h2]));
                    if (tgt < 0) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                }
                if (ws.cnt != 1) return con(pool, k, ss, Scorer.COST, 1, new Random(0));
                return new GRes(ws.L, ws.R, true, 1, ws.cost, ws);
            }
            static int merge(Vanilla k, WSet ws, Probe pb, int lp, int rp) {
                k.probe(ws.liveV[lp], ws.liveV[rp], pb);
                if (!pb.ok) return -1;
                int nid = k.commit(pb);
                ws.cost += pb.cost;
                int i = Math.min(lp, rp), j = Math.max(lp, rp);
                ws.take(i, j, lp < rp ? 0 : 1, nid);
                return ws.liveS[i];
            }
            static float bkCw(Pool pool, int id) {
                Lexicon t = pool.lex();
                float[] v = {pool.penalty(id)};
                boolean book = pool.ifBook(id);
                pool.view(id).forEach((e, lv) -> v[0] += (book ? t.baCost(e) : t.aCost(e)) * lv);
                return v[0];
            }
            static void sortCw(Pool pool, int[] ss, int[] idx, int len, int m) {
                for (int i = 1; i < len; i++) {
                    int x = idx[i];
                    float cx = bkCw(pool, ss[x]);
                    int tx = m == 1 ? pool.type(ss[x]) : 0;
                    int j = i - 1;
                    while (j >= 0) {
                        int y = idx[j];
                        float cy = bkCw(pool, ss[y]);
                        if (m == 1 ? (pool.type(ss[y]) > tx || (pool.type(ss[y]) == tx && cy > cx)) : cy > cx) {
                            idx[j + 1] = y; j--;
                        } else break;
                    }
                    idx[j + 1] = x;
                }
            }
            static void sortHeads(Pool pool, WSet ws, int[] heads, int len) {
                for (int i = 1; i < len; i++) {
                    int x = heads[i];
                    float cx = bkCw(pool, ws.liveV[ws.pos(x)]);
                    int j = i - 1;
                    while (j >= 0 && bkCw(pool, ws.liveV[ws.pos(heads[j])]) > cx) { heads[j + 1] = heads[j]; j--; }
                    heads[j + 1] = x;
                }
            }
        }

        /** Beam search */
        static final class Beam {
            static void con(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names, int B, float lambda, long deadline, Ceiling ub) {
                if (B < 1) B = 1;
                final int type = pool.type(ss[0]);
                WSet w0 = new WSet(ss);
                if (w0.cnt == 1) { Tr p = w0.trs(ss, names); ub.report(p, ev.evaluate(p, null)); return; }
                WSet[] beam = {w0};
                float[] cs = new float[B]; int[] cp = new int[B], ci = new int[B], cj = new int[B], cd = new int[B];
                Mem tr = new Mem(1 << 13);
                WSet[] free = new WSet[2 * B + 1]; int fn = 0;
                Probe pb = new Probe();
                while (beam.length > 0) {
                    if (deadline > 0 && System.nanoTime() > deadline) return;
                    int fi = 0;
                    for (int pi = 0; pi < beam.length; pi++) {
                        WSet w = beam[pi];
                        if (ub.found()) {
                            long lb = fr.compute(w, type);
                            if (lb < 0 || w.cost + lb >= ub.cost) continue;
                        }
                        if (fi == B) {
                            int mp = Integer.MAX_VALUE;
                            for (int i2 = 0; i2 < w.cnt; i2++) { int pq = pool.pen[w.liveV[i2]]; if (pq < mp) mp = pq; }
                            if (w.cost + 2f * mp + lambda * (2f * mp + 1f) >= cs[B - 1]) continue;
                        }
                        for (int i = 0; i < w.cnt; i++) for (int j = i + 1; j < w.cnt; j++) {
                            for (int d = 0; d < 2; d++) {
                                int u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                                k.probe(u, v, pb);
                                if (!pb.ok) continue;
                                float s = w.cost + pb.cost + lambda * pb.newPenalty + 0.25f * pb.conflicts;
                                if (fi < B || s < cs[B - 1]) {
                                    int p2 = Math.min(fi, B - 1);
                                    while (p2 > 0 && cs[p2 - 1] > s) { cs[p2] = cs[p2 - 1]; cp[p2] = cp[p2 - 1]; ci[p2] = ci[p2 - 1]; cj[p2] = cj[p2 - 1]; cd[p2] = cd[p2 - 1]; p2--; }
                                    cs[p2] = s; cp[p2] = pi; ci[p2] = i; cj[p2] = j; cd[p2] = d;
                                    if (fi < B) fi++;
                                }
                            }
                        }
                    }
                    if (fi == 0) return;
                    WSet[] next = new WSet[fi]; int nn = 0;
                    for (int c2 = 0; c2 < fi; c2++) {
                        WSet src = beam[cp[c2]];
                        WSet w = fn > 0 ? free[--fn] : null;
                        if (w == null) w = src.copy(); else w.copyFrom(src);
                        int i = ci[c2], j = cj[c2], d = cd[c2];
                        int u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                        k.probe(u, v, pb);
                        if (!pb.ok) { if (fn < free.length) free[fn++] = w; continue; }
                        int nid = k.commit(pb);
                        w.cost += pb.cost;
                        w.take(i, j, d, nid);
                        if (w.cnt == 1) {
                            Tr tr1 = w.trs(ss, names);
                            ub.report(tr1, ev.evaluate(tr1, null));
                            if (fn < free.length) free[fn++] = w;
                            continue;
                        }
                        if (tr.dominated(w.msh, w.cost)) { if (fn < free.length) free[fn++] = w; continue; }
                        next[nn++] = w;
                    }
                    for (WSet p2 : beam) if (fn < free.length) free[fn++] = p2;
                    beam = nn == 0 ? new WSet[0] : (nn == next.length ? next : Arrays.copyOf(next, nn));
                }
            }
        }

        /** B&B */
        static final class BranchBound {
            static boolean con(Pool pool, Vanilla k, Evaluator ev, int[] ss, String[] names, long budget, long deadline, float lambda, Ceiling ub) { return con(pool, k, ev, new WSet(ss), ss, names, budget, deadline, lambda, ub); }
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
                final int cc;
                final float[][] bs; final int[][] bi, bj, bd;
                final int[] fCnt, fCur, fSi, fVi, fSj, fVj, fNid;
                final long[] fCost, fMsh;
                long nodes; boolean abort;
                Ctx(Vanilla k, Evaluator ev, int[] ss, String[] names, long budget, long deadline, float lambda, Ceiling ub) {
                    this.k = k; this.ev = ev; this.ss = ss; this.names = names;
                    this.budget = budget; this.deadline = deadline; this.lambda = lambda; this.ub = ub;
                    tr = new Mem(Math.clamp(budget, 4096, 1 << 21));
                    fr = new Floor(k.pool, k.limit());
                    type = k.pool.type(ss[0]);
                    int n = ss.length;
                    if (n > 32) throw new IllegalStateException("B&B supports at most 32 leaves");
                    cc = n * (n - 1);
                    bs = new float[n][cc]; bi = new int[n][cc]; bj = new int[n][cc]; bd = new int[n][cc];
                    fCnt = new int[n]; fCur = new int[n]; fSi = new int[n]; fVi = new int[n]; fSj = new int[n]; fVj = new int[n];
                    fCost = new long[n]; fMsh = new long[n]; fNid = new int[n];
                }
            }
            static void dfs(Ctx cx, WSet w) {
                if (++cx.nodes > cx.budget || (cx.nodes & 511) == 0 && cx.deadline > 0 && System.nanoTime() > cx.deadline) {
                    cx.abort = true; return;
                }
                if (w.cnt == 1) {
                    Tr p = w.trs(cx.ss, cx.names);
                    cx.ub.report(p, cx.ev.evaluate(p, null));
                    return;
                }
                long lbv = cx.fr.compute(w, cx.type);
                if (lbv < 0) return;
                if (cx.ub.found() && w.cost + lbv >= cx.ub.cost) return;
                if (cx.tr.dominated(w.msh, w.cost)) return;
                int dep = w.cur - cx.ss.length, cc = cx.cc;
                float[] bs = cx.bs[dep]; int[] bi = cx.bi[dep], bj = cx.bj[dep], bd = cx.bd[dep];
                int found = 0;
                for (int i = 0; i < w.cnt; i++) for (int j = i + 1; j < w.cnt; j++)
                    for (int d = 0; d < 2; d++) {
                        int u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                        cx.k.probe(u, v, cx.pb);
                        if (!cx.pb.ok) continue;
                        float s = cx.pb.cost + cx.lambda * cx.pb.newPenalty;
                        if (found < cc || s < bs[cc - 1]) {
                            int p2 = Math.min(found, cc - 1);
                            while (p2 > 0 && bs[p2 - 1] > s) {
                                bs[p2] = bs[p2 - 1]; bi[p2] = bi[p2 - 1]; bj[p2] = bj[p2 - 1]; bd[p2] = bd[p2 - 1]; p2--;
                            }
                            bs[p2] = s; bi[p2] = i; bj[p2] = j; bd[p2] = d;
                            if (found < cc) found++;
                        }
                    }
                for (int c2 = 0; c2 < found && !cx.abort; c2++) {
                    int i = bi[c2], j = bj[c2], d = bd[c2];
                    int u = d == 0 ? w.liveV[i] : w.liveV[j], v = d == 0 ? w.liveV[j] : w.liveV[i];
                    cx.k.probe(u, v, cx.pb);
                    if (!cx.pb.ok) continue;
                    int nid = cx.k.commit(cx.pb);
                    cx.fCnt[dep] = w.cnt; cx.fCur[dep] = w.cur; cx.fCost[dep] = w.cost; cx.fMsh[dep] = w.msh;
                    cx.fSi[dep] = w.liveS[i]; cx.fVi[dep] = w.liveV[i];
                    cx.fSj[dep] = w.liveS[j]; cx.fVj[dep] = w.liveV[j];
                    cx.fNid[dep] = nid;
                    w.cost += cx.pb.cost;
                    w.take(i, j, d, nid);
                    dfs(cx, w);
                    if (cx.abort) return;
                    w.cnt = cx.fCnt[dep]; w.cur = cx.fCur[dep]; w.cost = cx.fCost[dep]; w.msh = cx.fMsh[dep];
                    w.liveS[i] = cx.fSi[dep]; w.liveV[i] = cx.fVi[dep];
                    w.liveS[j] = cx.fSj[dep]; w.liveV[j] = cx.fVj[dep];
                    w.idCnt[nid]--; w.idCnt[cx.fVi[dep]]++; w.idCnt[cx.fVj[dep]]++;
                }
            }
        }

        /** Lns */
        static final class Lns {
            static final class Lx {
                final Pool pool; final Vanilla k; final Evaluator ev;
                final Floor fr; final int type;
                final int[] ss, side = {0}, post, lc; final String[] names;
                final Probe pb = new Probe();
                final boolean[] skip, subM, ancM;
                final Greedy.Ctx g = new Greedy.Ctx();
                final Eval[] ebuf = new Eval[2];
                final Tr[] pbuf = new Tr[2];
                WSet skel;
                int bflip;
                Lx(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names) {
                    int n = ss.length;
                    this.pool = pool; this.k = k; this.ev = ev; this.fr = fr; this.type = pool.type(ss[0]); this.ss = ss; this.names = names;
                    skip = new boolean[n - 1]; post = new int[n - 1]; subM = new boolean[2 * n - 1]; ancM = new boolean[2 * n - 1]; lc = new int[2 * n - 1];
                    ebuf[0] = new Eval(2 * n - 1); ebuf[1] = new Eval(2 * n - 1);
                }
            }
            static void con(Pool pool, Vanilla k, Evaluator ev, Floor fr, int[] ss, String[] names, long deadline, int stall, long seed, Ceiling ub) {
                int n = ss.length;
                if (n < 3 || !ub.found()) return;
                Random r = new Random(seed);
                Lx x = new Lx(pool, k, ev, fr, ss, names);
                RefSeq rs = RefSeq.of(ub.tr, ub.eval);
                float[] w = {1f, 1f};
                int stallCt = 0, decay = 0;
                while (stallCt < stall) {
                    if (deadline > 0 && System.nanoTime() > deadline) return;
                    int op = r.nextFloat() * (w[0] + w[1]) < w[0] ? 0 : 1;
                    long prev = ub.cost;
                    boolean acc = op == 0 ? dig(x, r, rs, ub) : swapOp(x, r, ub);
                    x.bflip ^= 1;
                    if (acc) {
                        rs = RefSeq.of(ub.tr, ub.eval);
                        if (ub.cost < prev) { stallCt = 0; x.side[0] = 0; w[op] += 1f; }
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
            static boolean dig(Lx x, Random r, RefSeq rs, Ceiling ub) {
                Tr tr = ub.tr;
                Eval cur = ub.eval;
                int n = tr.n;
                tr.postOrder(x.post);
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
                if (x.skel == null) x.skel = new WSet(x.ss); else x.skel.reset(x.ss);
                WSet ws = x.skel;
                for (int i = 0; i < rs.lu.length; i++) {
                    if (x.skip[i]) continue;
                    int pu = RefSeq.find(ws, rs.lu[i], -1), pv = RefSeq.find(ws, rs.ru[i], pu);
                    if (pu < 0 || pv < 0) return false;
                    x.k.probe(rs.lu[i], rs.ru[i], x.pb);
                    if (!x.pb.ok) return false;
                    int nid = x.k.commit(x.pb);
                    ws.cost += x.pb.cost;
                    if (pu < pv) ws.take(pu, pv, 0, nid); else ws.take(pv, pu, 1, nid);
                }
                int kk = ws.cnt;
                if (kk < 2) return false;
                if (x.side[0] >= 3) {
                    long lb = x.fr.compute(ws, x.type);
                    if (lb < 0 || ws.cost + lb >= ub.cost) return false;
                }
                Greedy.GRes rep = Greedy.con(x.pool, x.k, ws.liveV, kk, Scorer.PRESETS[r.nextInt(Scorer.PRESETS.length)], 1 + r.nextInt(3), r, x.g);
                if (!rep.ok) return false;
                for (int i = 0; i < x.g.mN; i++) {
                    int u = x.g.mA[i], v = x.g.mB[i];
                    int pu = RefSeq.find(ws, u, -1), pv = RefSeq.find(ws, v, pu);
                    if (pu < 0 || pv < 0) return false;
                    x.k.probe(u, v, x.pb);
                    if (!x.pb.ok) return false;
                    int nid = x.k.commit(x.pb);
                    ws.cost += x.pb.cost;
                    if (pu < pv) ws.take(pu, pv, 0, nid); else ws.take(pv, pu, 1, nid);
                }
                if (ws.cnt != 1) return false;
                return accept(x.ev, ub, ws, x.ss, x.names, cur.total, x.side);
            }
            static boolean swapOp(Lx x, Random r, Ceiling ub) {
                int n = ub.tr.n, bi = x.bflip;
                Tr p2 = x.pbuf[bi];
                if (p2 == null) p2 = x.pbuf[bi] = ub.tr.copy();
                else p2.copyFrom(ub.tr);
                for (int t = 0; t < 4; t++) {
                    int a = r.nextInt(2 * n - 1), b = r.nextInt(2 * n - 1);
                    if (!p2.swap(a, b)) continue;
                    Eval e2 = x.ev.evaluate(p2, x.ebuf[bi]);
                    if (e2.feasible && (e2.total < ub.cost || (e2.total == ub.cost && x.side[0] < 3))) {
                        if (e2.total == ub.cost) x.side[0]++;
                        ub.report(p2.copy(), e2.copy());
                        return true;
                    }
                    return false;
                }
                return false;
            }
            static boolean accept(Evaluator ev, Ceiling ub, WSet ws, int[] ss, String[] names, long cCost, int[] s) {
                Tr p = ws.trs(ss, names);
                Eval e = ev.evaluate(p, null);
                if (!e.feasible) return false;
                if (e.total < cCost) { ub.report(p, e); return true; }
                if (e.total == cCost && s[0] < 3) { s[0]++; ub.report(p, e); return true; }
                return false;
            }
        }

        /** Container for holding intermediate ids */
        static final class Probe {
            boolean ok;
            long cost, key;
            int conflicts, applied, upgraded, newPenalty, newType, id;
            final long[] lv = new long[LVW], pr = new long[PRW], am = new long[PRW];
        }

        static interface Kernel {
            void probe(int l, int r, Probe p);
            int commit(Probe p);
            default int apply(int l, int r, Probe p) { probe(l, r, p); return commit(p); }
        }

        /**
         * @see Ref
         */
        static final class Vanilla implements Kernel {
            final Lexicon t; final Pool pool; final Policy pol; final Cache cache;
            long probes, hits, evals, commits, interns;
            Vanilla(Lexicon t, Pool pool, Policy pol) { this.t = t; this.pool = pool; this.pol = pol; this.cache = new Cache(pool, 1 << 16); }
            int limit() { return pol.limit; }
            @Override public void probe(int l, int r, Probe p) {
                p.key = ((long) l << 32 | r) + 1L;
                p.id = -1;
                probes++;
                if (cache.get(l, r, p)) { hits++; return; }
                evals++;
                eval(l, r, p);
                cache.put(p, -1);
            }
            @Override public int commit(Probe p) {
                if (!p.ok) return -1;
                commits++;
                if (p.id >= 0) return p.id;
                eval((int) ((p.key - 1) >>> 32), (int) (p.key - 1), p);
                int id = pool.intern(p.lv, p.pr, p.newPenalty, p.newType);
                p.id = id;
                cache.put(p, id);
                interns++;
                return id;
            }
            void eval(int l, int r, Probe p) {
                p.ok = false; p.cost = 0; p.conflicts = 0; p.applied = 0; p.upgraded = 0; p.newPenalty = -1;
                if (l < 0 || r < 0 || l >= pool.n || r >= pool.n) throw new IllegalArgumentException("bad state id");
                int tl = pool.typ[l], tr = pool.typ[r];
                p.newType = tl;
                if (!t.ifStore[tl]) return;
                boolean rb = t.book[tr], lb = t.book[tl];
                if (!rb && (tr != tl || !t.dmg[tl])) return;
                final int limit = pol.limit;
                long cost = (long) pool.pen[l] + pool.pen[r];
                if (cost >= limit) return;
                final long[] con = t.conflict, can = t.ifApply, rlv = pool.lv;
                final int canB = tl * PRW, rOff = r * LVW;
                long p0 = pool.pr[l * PRW], p1 = pool.pr[l * PRW + 1], a0 = 0, a1 = 0;
                int cf = 0, ap = 0, upg = 0;
                boolean monster = false; // nya
                for (int w = 0; w < PRW; w++) {
                    long x = w == 0 ? pool.pr[r * PRW] : pool.pr[r * PRW + 1];
                    while (x != 0) {
                        int e = (w << 6) + Long.numberOfTrailingZeros(x); x &= x - 1;
                        int eb = e * PRW;
                        int k = Long.bitCount(con[eb] & p0) + Long.bitCount(con[eb + 1] & p1);
                        if (k != 0) {
                            cf += k; cost += k;
                            if (cost >= limit) return;
                        } else if (lb || (can[canB + (e >>> 6)] >>> e & 1L) != 0) {
                            if (!monster) { System.arraycopy(pool.lv, l * LVW, p.lv, 0, t.nw); monster = true; }
                            int sh = (e & 15) << 2, w2 = e >>> 4;
                            int cur = (int) (p.lv[w2] >>> sh) & 15, rl = (int) (rlv[rOff + w2] >>> sh) & 15;
                            int nl = cur == rl ? rl + 1 : Math.max(cur, rl), mx = t.maxLv[e];
                            if (nl > mx) nl = mx;
                            p.lv[w2] = p.lv[w2] & ~(15L << sh) | (long) nl << sh;
                            if (w == 0) { a0 |= 1L << e; p0 |= 1L << e; } else { a1 |= 1L << e; p1 |= 1L << e; }
                            cost += (long) (rb ? t.bkFee[e] : t.fee[e]) * nl;
                            if (cost >= limit) return;
                            if (nl > cur) upg++;
                            ap++;
                        }
                    }
                }
                p.ok = ap > 0 && cost < limit;
                p.cost = cost; p.conflicts = cf; p.applied = ap; p.upgraded = upg;
                p.newPenalty = (int) Math.min((long) Math.max(pool.pen[l], pool.pen[r]) * 2 + 1, Integer.MAX_VALUE);
                p.pr[0] = p0; p.pr[1] = p1; p.am[0] = a0; p.am[1] = a1;
            }
        }

        /** Eval dispatcher */
        static final class Evaluator {
            final Kernel k;
            final Probe pb = new Probe();
            int[] post = new int[0];
            Evaluator(Kernel k) { this.k = k; }
            Eval evaluate(Tr tr, Eval ev) {
                int n = tr.n, m = 2 * n - 1;
                if (ev == null || ev.ids.length != m) ev = new Eval(m);
                if (post.length != n - 1) post = new int[n - 1];
                tr.postOrder(post);
                for (int i = 0; i < n; i++) {
                    ev.ids[i] = tr.leaf[i]; ev.aStep[i] = 0; ev.sub[i] = 0; ev.conflicts[i] = 0;
                }
                ev.feasible = true; ev.failIdx = -1;
                long total = 0;
                for (int i = 0; i < n - 1; i++) {
                    int s = post[i];
                    k.probe(ev.ids[tr.l[s]], ev.ids[tr.r[s]], pb);
                    if (!pb.ok) { ev.feasible = false; ev.failIdx = s; ev.total = -1; return ev; }
                    ev.ids[s] = k.commit(pb);
                    long c = pb.cost;
                    ev.aStep[s] = (int) c;
                    ev.conflicts[s] = pb.conflicts;
                    ev.sub[s] = ev.sub[tr.l[s]] + ev.sub[tr.r[s]] + (int) c;
                    total += c;
                }
                ev.total = total;
                return ev;
            }
            // seems useless but written
            void touch(Tr tr, Eval ev, int slot) {
                if (slot < tr.n) ev.ids[slot] = tr.leaf[slot];
                int s = tr.p[slot];
                while (s >= 0) {
                    k.probe(ev.ids[tr.l[s]], ev.ids[tr.r[s]], pb);
                    if (!pb.ok) { ev.feasible = false; ev.failIdx = s; ev.total = -1; return; }
                    ev.ids[s] = k.commit(pb);
                    long c = pb.cost;
                    ev.aStep[s] = (int) c;
                    ev.conflicts[s] = pb.conflicts;
                    ev.sub[s] = ev.sub[tr.l[s]] + ev.sub[tr.r[s]] + (int) c;
                    s = tr.p[s];
                }
                ev.feasible = true; ev.failIdx = -1;
                ev.total = ev.sub[tr.root()];
            }
        }

        /** DP Pruning */
        static final class Mem {
            long[] h, c;
            int mask, cnt, limit;
            Mem(int cap) { int x = 16; while (x < cap) x <<= 1; h = new long[x]; c = new long[x]; mask = x - 1; limit = x - x / 4; }
            boolean dominated(long hh, long cost) {
                if (hh == 0) hh = 1;
                int i = (int) BitVec.mix64(hh) & mask;
                while (h[i] != 0) {
                    if (h[i] == hh) {
                        if (c[i] <= cost) return true;
                        c[i] = cost; return false;
                    }
                    i = (i + 1) & mask;
                }
                if (cnt >= limit) {
                    if ((mask + 1) >= (1 << 23)) return false;
                    grow();
                    return dominated(hh, cost);
                }
                h[i] = hh; c[i] = cost; cnt++;
                return false;
            }
            void grow() {
                long[] h2 = new long[h.length << 1]; long[] c2 = new long[c.length << 1];
                int m = h2.length - 1;
                for (int j = 0; j < h.length; j++) {
                    if (h[j] == 0) continue;
                    int i = (int) BitVec.mix64(h[j]) & m;
                    while (h2[i] != 0) i = (i + 1) & m;
                    h2[i] = h[j]; c2[i] = c[j];
                }
                h = h2; c = c2; mask = m; limit = h.length - h.length / 4;
            }
        }

        /** Suzume */
        static final class Ceiling { // TODO
            Tr tr; Eval eval; long cost = Long.MAX_VALUE;
            int version;
            boolean report(Tr p, Eval e) {
                if (!e.feasible || e.total >= cost) return false;
                tr = p; eval = e; cost = e.total; version++; return true;
            }
            boolean found() { return cost < Long.MAX_VALUE; }
        }
        static final class Floor {
            final Pool pool; final Lexicon t; final int cap;
            long[] pa = new long[0], pe = new long[0];
            Floor(Pool pool, int cap) { this.pool = pool; t = pool.lex(); this.cap = cap; }
            long compute(WSet w, int type) {
                final int L = w.cnt;
                if (L <= 1) return 0;
                int m = Integer.MAX_VALUE; long sum = 0;
                for (int i = 0; i < L; i++) {
                    int p = pool.pen[w.liveV[i]];
                    sum += p;
                    if (p < m) m = p;
                }
                int mx = Integer.MAX_VALUE;
                for (int i = 0; i < L; i++) if (pool.typ[w.liveV[i]] == type && pool.pen[w.liveV[i]] < mx) mx = pool.pen[w.liveV[i]];
                if (mx == Integer.MAX_VALUE) return -1;
                int H = 32 - Integer.numberOfLeadingZeros(L - 1);
                if (2L * m >= cap) return -1;
                if (L >= 3 && ((1L << (H - 1)) * (m + 1L) - 1) + m >= cap) return -1;
                int c0 = 0;
                for (int i = 0; i < L; i++) if (pool.pen[w.liveV[i]] == m) c0++;
                int q0 = Math.min(c0 >> 1, Math.min(L >> 1, L - 2)), l = Math.max(0, H - 2), r = Math.max(0, L - 2 - q0 - l);
                long in = (long) q0 * (2L * m + 1) + (long) r * (2L * m + 3);
                for (int h = 2; h < H; h++) in += (1L << h) * (m + 1L) - 1;
                long u0 = 0, u1 = 0;
                for (int i = 0; i < L; i++) { int b = w.liveV[i] * PRW; u0 |= pool.pr[b]; u1 |= pool.pr[b + 1]; }
                if (pa.length < L) pa = new long[Math.max(L, 8)];
                long tl = 0, ml = 0;
                for (int i = 0; i < L; i++) {
                    int is = w.liveV[i]; long pv = 0;
                    for (int wd = 0; wd < PRW; wd++) {
                        long x = pool.pr[is * PRW + wd];
                        while (x != 0) {
                            int e = (wd << 6) + Long.numberOfTrailingZeros(x); x &= x - 1;
                            if (!t.ifApply(type, e)) continue;
                            boolean cf = (t.conflict[e * PRW] & u0) != 0 || (t.conflict[e * PRW + 1] & u1) != 0;
                            pv += cf ? 1 : (long) t.bkFee[e] * Math.min(pool.level(is, e), t.maxLv[e]);
                        }
                    }
                    pa[i] = pv; tl += pv;
                    if (pool.typ[is] == type && pv > ml) ml = pv;
                }
                long e1 = tl - ml;
                long bs = in;
                if (L <= 256) {
                    Sdp dp = Sdp.at(L);
                    Arrays.sort(pa, 0, L);
                    int rm = Arrays.binarySearch(pa, 0, L, ml);
                    if (rm < 0) rm = 0;
                    if (pe.length < L) pe = new long[Math.max(L, 8)];
                    pe[0] = 0;
                    for (int i = 0, j = 0; i < L; i++) if (i != rm) pe[++j] = pe[j - 1] + pa[i];
                    final long INF = Long.MAX_VALUE >>> 2; long ql = INF;
                    for (int q = 0; q <= L - 2; q++) {
                        long ch = Sdp.chain(mx, L - 1 - q);
                        if (ch >= INF) continue;
                        long f = 0;
                        if (q > 0) {
                            f = INF; long[] rl = dp.f2[q];
                            for (int cc = 1, cm = Math.min(q, L - 1 - q); cc <= cm; cc++) if (rl[cc] < f) f = rl[cc];
                            if (f >= INF) continue;
                        }
                        long v = ch + f + pe[q];
                        if (v < ql) ql = v;
                    }
                    if (ql < INF) bs = Math.max(bs, ql);
                }
                return sum + bs + e1;
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
                final long[] pes = new long[PRW];
                pool.view(t).forEach((e, lv) -> pes[e >>> 6] |= 1L << (e & 63));
                for (int i = 0; i < n; i++) { pool.view(mats[i]).forEach((e, _) -> { if (lex.ifApply(ty, e)) pes[e >>> 6] |= 1L << (e & 63); }); }
                final long[] c = new long[n], acc = {0}; final boolean[] rk = {false};
                for (int i = 0; i < n; i++) {
                    final int fi = i;
                    pool.view(mats[i]).forEach((e, lv) -> {
                        if (!lex.ifApply(ty, e)) return;
                        boolean cf = (lex.conflict[e * PRW] & pes[0]) != 0 || (lex.conflict[e * PRW + 1] & pes[1]) != 0;
                        long v = cf ? 1 : (long) lex.baCost(e) * Math.min(lv, lex.maxLv(e));
                        if (cf) rk[0] = true;
                        acc[0] += v; c[fi] += v;
                    });
                }
                final long[] tpv = {0};
                pool.view(t).forEach((e, lv) -> {
                    if (!lex.ifApply(ty, e)) return;
                    boolean cf = (lex.conflict[e * PRW] & pes[0]) != 0 || (lex.conflict[e * PRW + 1] & pes[1]) != 0;
                    tpv[0] += cf ? 1 : (long) lex.baCost(e) * Math.min(lv, lex.maxLv(e));
                });
                long mx = 0;
                for (int i = 0; i < n; i++) if (!pool.ifBook(mats[i]) && c[i] > mx) mx = c[i];
                long nl = acc[0] - Math.max(0, mx - tpv[0]);
                Arrays.sort(c);
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
                return new Height(lb, rk[0]);
            }
        }

        /** Pre-processing for solving */
        static final class Prepare {
            record Prepared(boolean ok, String fail, int[] ids, String[] names) {}
            static Prepared apply(Pool pool, Policy pol, int t, int[] mats, String[] names) {
                Lexicon lex = pool.lex();
                if (!lex.ifStore[pool.type(t)]) return new Prepared(false, "target cannot store enchantments", null, null);
                if (pool.penalty(t) >= pol.limit) return new Prepared(false, "target penalty out of cap", null, null);
                int keep = 0;
                for (int i = 0; i < mats.length; i++) if (usable(pool, lex, pol, t, mats[i])) keep++;
                if (keep == mats.length) return new Prepared(true, null, mats, names);
                int[] st = new int[keep]; String[] nm = new String[keep];
                for (int i = 0, j = 0; i < mats.length; i++) if (usable(pool, lex, pol, t, mats[i])) { st[j] = mats[i]; nm[j] = names[i]; j++; }
                return new Prepared(true, null, st, nm);
            }
            static boolean usable(Pool pool, Lexicon lex, Policy pol, int t, int mat) {
                if (pool.penalty(mat) >= pol.limit) return false;
                int tm = pool.type(mat), tt = pool.type(t);
                if (lex.book[tm]) {
                    boolean[] s = new boolean[1];
                    pool.view(mat).forEach((e, lv) -> { if (lex.ifApply(tt, e)) s[0] = true; });
                    return s[0];
                }
                if (tm != tt || !lex.dmg[tt]) return false;
                boolean[] seen = new boolean[1];
                boolean tb = lex.book[tt];
                pool.view(mat).forEach((e, lv) -> { if (tb || lex.ifApply(tt, e)) seen[0] = true; });
                return seen[0];
            }
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
            List<Node> trace(Tr tr, Eval ev) {
                if (!ev.feasible) return List.of();
                List<Node> out = new ArrayList<>();
                walk(tr, ev, tr.root(), out, new long[1]);
                return out;
            }
            long walk(Tr tr, Eval ev, int s, List<Node> out, long[] acc) {
                if (tr.ifLeaf(s)) return 0;
                long sub = walk(tr, ev, tr.l[s], out, acc) + walk(tr, ev, tr.r[s], out, acc) + ev.aStep[s];
                acc[0] += ev.aStep[s];
                long step = ev.aStep[s];
                out.add(new Node(tr.dName(tr.l[s]), tr.dName(tr.r[s]), tr.dName(s), snap(ev.ids[tr.l[s]]), snap(ev.ids[tr.r[s]]), snap(ev.ids[s]), (int) step, acc[0], height(tr, s), ev.conflicts[s]));
                return sub;
            }
            static int height(Tr tr, int s) { return tr.ifLeaf(s) ? 0 : 1 + Math.max(height(tr, tr.l[s]), height(tr, tr.r[s])); }
            Map<String, Integer> snap(int id) {
                Map<String, Integer> m = new LinkedHashMap<>();
                pool.view(id).forEach((e, lv) -> m.put(t.eName(e), lv));
                return m;
            }
        }

        /** Lex for lookups */
        static final class Lexicon {
            final int ne, nt, nw, pw;
            final boolean truncated;
            final int[] maxLv, fee, bkFee;
            final long[] conflict, ifApply;
            final boolean[] book, dmg, ifStore;
            final String[] eName, tName;
            Lexicon(int ne, int nt, int nw, int pw, boolean truncated, int[] maxLv, int[] fee, int[] bkFee, long[] conflict, long[] ifApply, boolean[] book, boolean[] dmg, boolean[] ifStore, String[] eName, String[] tName) { this.ne = ne; this.nt = nt; this.nw = nw; this.pw = pw; this.truncated = truncated;this.maxLv = maxLv; this.fee = fee; this.bkFee = bkFee;this.conflict = conflict; this.ifApply = ifApply;this.book = book; this.dmg = dmg; this.ifStore = ifStore;this.eName = eName; this.tName = tName; }
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
            LexTrans conflict(int a, int b) {
                if (a != b) pairs.add(new int[]{ a, b }); return this;
            }
            LexTrans allow(int type, int e) { allow.add(new int[]{ type, e }); return this; }
            Lexicon begin() {
                int ne = es.size(), nt = ts.size();
                if (ne > MAX) throw new IllegalStateException("too many enchantments: " + ne);
                boolean tr = false;
                int[] ml = new int[ne], fee = new int[ne], bf = new int[ne];
                for (int e = 0; e < ne; e++) {
                    int[] s = es.get(e);
                    if (s[0] > MAX_LV) { ml[e] = MAX_LV; tr = true; } else ml[e] = s[0];
                    if (s[0] < 0) throw new IllegalArgumentException("negative max level");
                    fee[e] = s[1]; bf[e] = Math.max(1, s[1] / 2);
                }
                long[] con = new long[ne * PRW];
                for (int[] p : pairs) {
                    int a = p[0], b = p[1];
                    chk(a, ne); chk(b, ne);
                    con[a * PRW + (b >>> 6)] |= 1L << b;
                    con[b * PRW + (a >>> 6)] |= 1L << a;
                }
                long[] ca = new long[nt * PRW];
                for (int[] a : allow) {
                    chk(a[1], ne); if (a[0] < 0 || a[0] >= nt) throw new IllegalArgumentException("bad type id");
                    ca[a[0] * PRW + (a[1] >>> 6)] |= 1L << a[1];
                }
                boolean[] bk = new boolean[nt], dmg = new boolean[nt], st = new boolean[nt];
                for (int i = 0; i < nt; i++) {
                    int[] s = ts.get(i); bk[i] = s[0] != 0; dmg[i] = s[1] != 0; st[i] = s[2] != 0;
                }
                int nw = (ne + 15) >>> 4, pw = (ne + 63) >>> 6;
                return new Lexicon(ne, nt, nw, pw, tr, ml, fee, bf, con, ca, bk, dmg, st, en.toArray(new String[0]), tn.toArray(new String[0]));
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
            Lexicon lex() { return lex; }
            int size() { return n; }
            int intern(long[] l, long[] p2, int pen, int type) {
                if (pen < 0 || type < 0 || type >= lex.nt) throw new IllegalStateException("bad meta: pen=" + pen + " type=" + type);
                long m0 = lex.ne >= 64 ? -1L : (1L << lex.ne) - 1L;
                long m1 = lex.ne <= 64 ? 0L : lex.ne >= 128 ? -1L : (1L << (lex.ne - 64)) - 1L;
                if ((p2[0] & ~m0) != 0L || (p2[1] & ~m1) != 0L) throw new IllegalStateException("dirty presence: ne=" + lex.ne + " pr=" + Long.toHexString(p2[0]) + "," + Long.toHexString(p2[1]));
                for (int i = lex.nw; i < LVW; i++) if (l[i] != 0L) throw new IllegalStateException("dirty levels beyond nw");
                for (int i = lex.pw; i < PRW; i++) if (p2[i] != 0L) throw new IllegalStateException("dirty presence beyond pw");
                long h = hs(l, p2, pen, type);
                int i = (int) h & tMask;
                while (tab[i] != 0) {
                    int id = tab[i] - 1;
                    if (hsh[id] == h && eq(id, l, p2, pen, type)) return id; // fully same
                    i = (i + 1) & tMask;
                }
                if (n == cap) { grow(); i = (int) h & tMask; while (tab[i] != 0) i = (i + 1) & tMask; }
                int id = n++;
                System.arraycopy(l, 0, lv, id * LVW, lex.nw);
                System.arraycopy(p2, 0, pr, id * PRW, lex.pw);
                this.pen[id] = pen; typ[id] = type; hsh[id] = h; tab[i] = id + 1;
                return id;
            }
            int level(int id, int e) { return (int) (lv[id * LVW + (e >>> 4)] >>> ((e & 15) << 2)) & 15; }
            int penalty(int id) { return pen[id]; }
            int type(int id) { return typ[id]; }
            boolean ifBook(int id) { return lex.book[typ[id]]; }
            void copy(int id, long[] dstLv, long[] dstPr) {
                System.arraycopy(lv, id * LVW, dstLv, 0, LVW);
                System.arraycopy(pr, id * PRW, dstPr, 0, PRW);
            }
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
            long hs(long[] l, long[] p2, int pen, int type) {
                long h = 0x9E3779B97F4A7C15L;
                for (int i = 0; i < lex.nw; i++) h = BitVec.mix64(h ^ l[i]);
                for (int i = 0; i < lex.pw; i++) h = BitVec.mix64(h ^ p2[i]);
                return BitVec.mix64(h ^ pen * 0xC2B2AE3D27D4EB4FL ^ type * 0x165667B19E3779F9L);
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
            int enchantCount() { return Long.bitCount(p.pr[id * PRW]) + Long.bitCount(p.pr[id * PRW + 1]); }
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
            long[] k, meta;
            int[] st, cfa;
            int mask, cnt;
            final Pool pool;
            Cache(Pool pool, int cap) {
                this.pool = pool;
                int c = 16; while (c < cap) c <<= 1;
                k = new long[c]; meta = new long[c];
                st = new int[c]; cfa = new int[c];
                mask = c - 1;
            }
            void clear() { Arrays.fill(k, 0L); cnt = 0; }
            boolean get(int l, int r, Probe p) {
                long key = ((long) l << 32 | r) + 1L;
                int i = (int) BitVec.mix64(key) & mask;
                while (k[i] != 0) {
                    if (k[i] == key) {
                        int s = st[i];
                        long m = meta[i];
                        p.ok = m < 0;
                        p.cost = (m >>> 32) & 0x7FFFFFFFL;
                        p.newPenalty = (int) m;
                        int f = cfa[i];
                        p.conflicts = f >>> 16; p.applied = (f >>> 8) & 0xFF; p.upgraded = f & 0xFF;
                        p.newType = pool.type(l);
                        p.id = s >= 0 ? s : -1;
                        return true;
                    }
                    i = (i + 1) & mask;
                }
                return false;
            }
            void put(Probe p, int stateId) {
                if ((cnt + 1) * 2 > mask) grow();
                int i = (int) BitVec.mix64(p.key) & mask;
                while (k[i] != 0) {
                    if (k[i] == p.key) { if (stateId >= 0 && st[i] < 0) st[i] = stateId; return; }
                    i = (i + 1) & mask;
                }
                k[i] = p.key;
                meta[i] = (p.ok ? Long.MIN_VALUE : 0L) | ((p.cost & 0x7FFFFFFFL) << 32) | (p.newPenalty & 0xFFFFFFFFL);
                cfa[i] = (p.conflicts << 16) | (p.applied << 8) | p.upgraded;
                st[i] = stateId >= 0 ? stateId : (p.ok ? -1 : -2);
                cnt++;
            }
            void grow() {
                int c = k.length << 1;
                long[] k2 = new long[c]; long[] m2 = new long[c];
                int[] s2 = new int[c]; int[] f2 = new int[c];
                int m = c - 1;
                for (int j = 0; j < k.length; j++) {
                    if (k[j] == 0) continue;
                    int i = (int) BitVec.mix64(k[j]) & m;
                    while (k2[i] != 0) i = (i + 1) & m;
                    k2[i] = k[j]; m2[i] = meta[j]; s2[i] = st[j]; f2[i] = cfa[j];
                }
                k = k2; meta = m2; st = s2; cfa = f2; mask = m;
            }
        }

        /** Binary tree */
        static final class Tr {
            final int n;
            final int[] l, r, p, leaf;
            final String[] name;
            Tr(String[] ns, int[] leafs) {
                n = leafs.length;
                int m = 2 * n - 1;
                l = new int[m]; r = new int[m]; p = new int[m]; leaf = new int[n];
                name = ns == null ? null : Arrays.copyOf(ns, n);
                System.arraycopy(leafs, 0, leaf, 0, n);
                for (int i = 0; i < m; i++) { l[i] = r[i] = -1; p[i] = -1; }
                for (int i = 1; i < n; i++) {
                    int idx = n - 1 + i;
                    l[idx] = i == 1 ? 0 : idx - 1;
                    r[idx] = i;
                }
                refParents();
            }
            Tr(int[] left, int[] right, int[] leafs, String[] names) {
                n = leafs.length;
                if (left.length != 2 * n - 1) throw new IllegalArgumentException("bad topology size");
                l = left.clone(); r = right.clone(); p = new int[2 * n - 1];
                leaf = leafs.clone();
                name = names == null ? null : Arrays.copyOf(names, n);
                Arrays.fill(p, -1);
                refParents();
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
            int slots() { return 2 * n - 1; }
            int root() { return 2 * n - 2; }
            boolean ifLeaf(int s) { return s < n; }
            String lName(int i) { return name == null ? "L" + i : name[i]; }
            String dName(int s) { while (s >= n) s = l[s]; return lName(s); }
            boolean ifAncestor(int a, int x) {
                for (int s = x; s >= 0; s = p[s]) if (s == a) return true;
                return false;
            }
            /** Swaps two subtrees */
            boolean swap(int a, int b) {
                int m = 2 * n - 1;
                if (a == b || a < 0 || b < 0 || a >= m || b >= m) return false;
                if (p[a] < 0 || p[b] < 0) return false;
                if (ifAncestor(a, b) || ifAncestor(b, a)) return false;
                int pa = p[a], pb = p[b];
                if (l[pa] == a) l[pa] = b; else r[pa] = b;
                if (l[pb] == b) l[pb] = a; else r[pb] = a;
                p[a] = pb; p[b] = pa;
                return true;
            }
            void refParents() {
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
            /** Check if the subtree contains node 'x' */
            boolean contains(int slot, int x) {
                if (slot == x) return true;
                if (slot < n || slot < 0) return false;
                return contains(l[slot], x) || contains(r[slot], x);
            }
            void postOrder(int[] out) {
                if (n == 1) return;
                int[] ss = new int[2 * n];
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
            final int[] ids, aStep, sub, conflicts; // step & subtree costs
            boolean feasible;
            long total = -1;
            int failIdx = -1;
            Eval(int slots) { ids = new int[slots]; aStep = new int[slots]; sub = new int[slots]; conflicts = new int[slots]; }
            int rootIdx() { return ids[ids.length - 1]; }
            Eval copy() {
                Eval c = new Eval(ids.length);
                System.arraycopy(ids, 0, c.ids, 0, ids.length);
                System.arraycopy(aStep, 0, c.aStep, 0, aStep.length);
                System.arraycopy(sub, 0, c.sub, 0, sub.length);
                System.arraycopy(conflicts, 0, c.conflicts, 0, conflicts.length);
                c.feasible = feasible; c.total = total; c.failIdx = failIdx;
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
                    rs.lu[i] = ev.ids[tr.l[s]];
                    rs.ru[i] = ev.ids[tr.r[s]];
                }
                return rs;
            }
            static int find(WSet ws, int id, int excl) {
                for (int i = 0; i < ws.cnt; i++) if (i != excl && ws.liveV[i] == id) return i;
                return -1;
            }
            boolean replayAll(Vanilla k, WSet ws, Probe pb) {
                for (int i = 0; i < lu.length; i++) {
                    int pu = find(ws, lu[i], -1), pv = find(ws, ru[i], pu);
                    if (pu < 0 || pv < 0) return false;
                    k.probe(lu[i], ru[i], pb);
                    if (!pb.ok) return false;
                    int nid = k.commit(pb);
                    ws.cost += pb.cost;
                    if (pu < pv) ws.take(pu, pv, 0, nid); else ws.take(pv, pu, 1, nid);
                }
                return true;
            }
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
                        long rl = (1L << (32 - Integer.numberOfLeadingZeros(mx - 1) + 1)) - 1;
                        long v = sdp[a] + sdp[s - a] + rl;
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
            final int[] liveS, liveV, L, R;
            int[] idCnt;
            int cnt, cur;
            long cost, msh;
            WSet(int[] ss) { this(ss, ss.length); }
            WSet(int[] ss, int len) {
                n = len;
                int m = 2 * n - 1;
                liveS = new int[n]; liveV = new int[n];
                L = new int[m]; R = new int[m];
                Arrays.fill(L, -1); Arrays.fill(R, -1);
                int mx = 0;
                long h = 0;
                for (int i = 0; i < n; i++) {
                    liveS[i] = i; liveV[i] = ss[i]; h += sig(ss[i]);
                    if (ss[i] > mx) mx = ss[i];
                }
                idCnt = new int[mx + 1];
                for (int i = 0; i < n; i++) idCnt[ss[i]]++;
                cnt = n; cur = n; msh = h;
            }
            WSet(WSet o) {
                n = o.n;
                liveS = o.liveS.clone(); liveV = o.liveV.clone();
                L = o.L.clone(); R = o.R.clone();
                idCnt = o.idCnt.clone();
                cnt = o.cnt; cur = o.cur; cost = o.cost; msh = o.msh;
            }
            WSet copy() { return new WSet(this); }
            void copyFrom(WSet o) {
                System.arraycopy(o.liveS, 0, liveS, 0, n);
                System.arraycopy(o.liveV, 0, liveV, 0, n);
                System.arraycopy(o.L, 0, L, 0, 2 * n - 1);
                System.arraycopy(o.R, 0, R, 0, 2 * n - 1);
                if (idCnt.length < o.idCnt.length) idCnt = new int[o.idCnt.length * 2];
                System.arraycopy(o.idCnt, 0, idCnt, 0, o.idCnt.length);
                Arrays.fill(idCnt, o.idCnt.length, idCnt.length, 0);
                cnt = o.cnt; cur = o.cur; cost = o.cost; msh = o.msh;
            }
            void reset(int[] ss) {
                int mx = 0;
                long h = 0;
                for (int i = 0; i < n; i++) {
                    liveS[i] = i; liveV[i] = ss[i]; h += sig(ss[i]);
                    if (ss[i] > mx) mx = ss[i];
                }
                if (idCnt.length <= mx) idCnt = new int[(mx + 1) * 2];
                else Arrays.fill(idCnt, 0);
                for (int i = 0; i < n; i++) idCnt[ss[i]]++;
                Arrays.fill(L, -1); Arrays.fill(R, -1);
                cnt = n; cur = n; cost = 0; msh = h;
            }
            void take(int i, int j, int dir, int nid) {
                int s = cur++;
                L[s] = dir == 0 ? liveS[i] : liveS[j];
                R[s] = dir == 0 ? liveS[j] : liveS[i];
                int u = liveV[i], v = liveV[j];
                msh += sig(nid) - sig(u) - sig(v);
                if (nid >= idCnt.length) idCnt = Arrays.copyOf(idCnt, Math.max(nid + 1, idCnt.length * 2));
                idCnt[u]--; idCnt[v]--; idCnt[nid]++;
                liveS[i] = s; liveV[i] = nid;
                liveS[j] = liveS[cnt - 1]; liveV[j] = liveV[cnt - 1];
                cnt--;
            }
            static long sig(int id) { return BitVec.mix64((id + 1) * 0x9E3779B97F4A7C15L); }
            int pos(int slot) {
                for (int i = 0; i < cnt; i++) if (liveS[i] == slot) return i;
                return -1;
            }
            Tr trs(int[] leafs, String[] names) { return new Tr(L, R, leafs, names); }
        }

        /** O(1) Generation Set */
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