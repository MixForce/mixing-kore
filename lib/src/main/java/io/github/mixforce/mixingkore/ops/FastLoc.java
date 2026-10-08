// <editor-fold defaultstate="collapsed" desc="// SPDX: LGPL-3.0-or-later | Copyright (C) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* Copyright (C) 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.ops;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;

import java.util.Arrays;

public final class FastLoc {
    private FastLoc() {}

    public static final class Pos {
        private static final int PACKED_X_LENGTH = 26;
        private static final int PACKED_Y_LENGTH = 12;
        private static final int PACKED_Z_LENGTH = 26;
        private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
        private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
        private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
        private static final int Y_OFFSET = 0;
        private static final int Z_OFFSET = PACKED_Y_LENGTH;
        private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;
        public static final Pos ZERO = new Pos(0, 0, 0);
        public static final Pos ONE = new Pos(1, 1, 1);
        private final long l;
        public Pos(int x, int y, int z) { this.l = asLong(x, y, z); }
        public Pos(long l) { this.l = l; }
        public Pos(BlockPos pos) { this.l = pos.asLong(); }
        public Pos(double x, double y, double z) { this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)); }
        public Pos(Position pos) { this(pos.x(), pos.y(), pos.z()); }
        public int x() { return extX(l); }
        public int y() { return extY(l); }
        public int z() { return extZ(l); }
        public long bits() { return l; }
        public Pos withX(int x) { return new Pos(asLong(x, y(), z())); }
        public Pos withY(int y) { return new Pos(asLong(x(), y, z())); }
        public Pos withZ(int z) { return new Pos(asLong(x(), y(), z)); }
        public Pos offset(int dx, int dy, int dz) { return (dx | dy | dz) == 0 ? this : new Pos(offset(l, dx, dy, dz)); }
        public Pos relative(Direction dir) { return offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()); }
        public Pos relative(Direction dir, int i) { return offset(dir.getStepX() * i, dir.getStepY() * i, dir.getStepZ() * i); }
        public Pos above() { return offset(0, 1, 0); }
        public Pos above(int i) { return offset(0, i, 0); }
        public Pos below() { return offset(0, -1, 0); }
        public Pos below(int i) { return offset(0, -i, 0); }
        public Pos north() { return offset(0, 0, -1); }
        public Pos north(int i) { return offset(0, 0, -i); }
        public Pos south() { return offset(0, 0, 1); }
        public Pos south(int i) { return offset(0, 0, i); }
        public Pos west() { return offset(-1, 0, 0); }
        public Pos west(int i) { return offset(-i, 0, 0); }
        public Pos east() { return offset(1, 0, 0); }
        public Pos east(int i) { return offset(i, 0, 0); }
        public Vec3d center() { return new Vec3d(x() + 0.5, y() + 0.5, z() + 0.5); }
        public Vec3d bottomCenter() { return new Vec3d(x() + 0.5, y(), z() + 0.5); }
        public BlockPos blockPos() { return BlockPos.of(l); }
        public Vector3i vector3i() { return new Vector3i(x(), y(), z()); }
        public Mutable mutable() { return new Mutable(l); }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Pos) return this.l == ((Pos) o).l;
            if (o instanceof Mutable) return this.l == ((Mutable) o).l;
            return false;
        }
        @Override
        public int hashCode() { return Long.hashCode(l); }
        @Override
        public String toString() { return "(" + x() + ", " + y() + ", " + z() + ")"; }
        public static long asLong(int x, int y, int z) {
            long l = 0L;
            l |= ((long) x & PACKED_X_MASK) << X_OFFSET;
            l |= ((long) y & PACKED_Y_MASK) << Y_OFFSET;
            l |= ((long) z & PACKED_Z_MASK) << Z_OFFSET;
            return l;
        }
        public static int extX(long l) { return (int) (l << (64 - X_OFFSET - PACKED_X_LENGTH) >> (64 - PACKED_X_LENGTH)); }
        public static int extY(long l) { return (int) (l << (64 - PACKED_Y_LENGTH) >> (64 - PACKED_Y_LENGTH)); }
        public static int extZ(long l) { return (int) (l << (64 - Z_OFFSET - PACKED_Z_LENGTH) >> (64 - PACKED_Z_LENGTH)); }
        public static long offset(long l, int dx, int dy, int dz) {
            return (dx | dy | dz) == 0 ? l : asLong(extX(l) + dx, extY(l) + dy, extZ(l) + dz);
        }
        public static final class Mutable {
            private long l;
            public Mutable() { this.l = 0L; }
            public Mutable(int x, int y, int z) { this.l = Pos.asLong(x, y, z); }
            public Mutable(long l) { this.l = l; }
            public Mutable(Pos pos) { this.l = pos.bits(); }
            public Mutable(BlockPos pos) { this.l = pos.asLong(); }
            public Mutable(double x, double y, double z) { this((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)); }
            public Mutable(Position pos) { this(pos.x(), pos.y(), pos.z()); }
            public Mutable set(int x, int y, int z) { this.l = Pos.asLong(x, y, z); return this; }
            public Mutable set(long l) { this.l = l; return this; }
            public Mutable set(Pos pos) { this.l = pos.bits(); return this; }
            public Mutable set(Mutable m) { this.l = m.l; return this; }
            public Mutable set(BlockPos pos) { this.l = pos.asLong(); return this; }
            public int x() { return Pos.extX(l); }
            public int y() { return Pos.extY(l); }
            public int z() { return Pos.extZ(l); }
            public long bits() { return l; }
            public Mutable withX(int x) { this.l = Pos.asLong(x, y(), z()); return this; }
            public Mutable withY(int y) { this.l = Pos.asLong(x(), y, z()); return this; }
            public Mutable withZ(int z) { this.l = Pos.asLong(x(), y(), z); return this; }
            public Mutable offset(int dx, int dy, int dz) { this.l = Pos.offset(l, dx, dy, dz); return this; }
            public Mutable relative(Direction dir) { return offset(dir.getStepX(), dir.getStepY(), dir.getStepZ()); }
            public Mutable relative(Direction dir, int i) { return offset(dir.getStepX() * i, dir.getStepY() * i, dir.getStepZ() * i); }
            public Mutable above() { return offset(0, 1, 0); }
            public Mutable above(int i) { return offset(0, i, 0); }
            public Mutable below() { return offset(0, -1, 0); }
            public Mutable below(int i) { return offset(0, -i, 0); }
            public Mutable north() { return offset(0, 0, -1); }
            public Mutable north(int i) { return offset(0, 0, -i); }
            public Mutable south() { return offset(0, 0, 1); }
            public Mutable south(int i) { return offset(0, 0, i); }
            public Mutable west() { return offset(-1, 0, 0); }
            public Mutable west(int i) { return offset(-i, 0, 0); }
            public Mutable east() { return offset(1, 0, 0); }
            public Mutable east(int i) { return offset(i, 0, 0); }
            public Vec3d center() { return new Vec3d(x() + 0.5, y() + 0.5, z() + 0.5); }
            public Vec3d bottomCenter() { return new Vec3d(x() + 0.5, y(), z() + 0.5); }
            public BlockPos blockPos() { return BlockPos.of(l); }
            public Vector3i vector3i() { return new Vector3i(x(), y(), z()); }
            public Pos freeze() { return new Pos(l); }
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o instanceof Mutable) return this.l == ((Mutable) o).l;
                if (o instanceof Pos) return this.l == ((Pos) o).l;
                return false;
            }
            @Override
            public int hashCode() { return Long.hashCode(l); }
            @Override
            public String toString() { return "(" + x() + ", " + y() + ", " + z() + ")"; }
        }
        public static final class List {
            private long[] a;
            private int size;
            public List(int c) { a = new long[Math.max(8, c)]; }
            public List() { this(8); }
            public int size() { return size; }
            public boolean isEmpty() { return size == 0; }
            public void add(long l) {
                if (size == a.length) grow(size + (size >> 1) + 8);
                a[size++] = l;
            }
            public void insert(int i, long l) {
                if (size == a.length) grow(size + (size >> 1) + 8);
                int n = size - i;
                System.arraycopy(a, i, a, i + 1, n);
                a[i] = l;
                size++;
            }
            public void addAll(long[] ls) {
                int n = ls.length;
                if (n == 0) return;
                if (size + n > a.length) grow(size + n);
                System.arraycopy(ls, 0, a, size, n);
                size += n;
            }
            public void addAll(List o) {
                if (o.size == 0) return;
                if (size + o.size > a.length) grow(size + o.size);
                System.arraycopy(o.a, 0, a, size, o.size);
                size += o.size;
            }
            public void set(int i, long l) { a[i] = l; }
            public void swap(int i, int j) { long t = a[i]; a[i] = a[j]; a[j] = t; }
            public void apply(Op op) {
                Mutable m = new Mutable();
                for (int i = 0; i < size; i++) {
                    m.set(a[i]);
                    op.apply(m);
                    a[i] = m.bits();
                }
            }
            public Pos get(int i) { return new Pos(a[i]); }
            public Mutable get(int i, Mutable out) { return out.set(a[i]); }
            public long bits(int i) { return a[i]; }
            public long first() { return a[0]; }
            public long last() { return a[size - 1]; }
            public boolean contains(long l) { return indexOf(l) >= 0; }
            public int indexOf(long l) {
                for (int i = 0; i < size; i++) if (a[i] == l) return i;
                return -1;
            }
            public long[] array() { return Arrays.copyOf(a, size); }
            public long pop() { return a[--size]; }
            public void pop(int i) { a[i] = a[--size]; }
            public void rmLast() { size--; }
            public void rm(int i) {
                int n = --size - i;
                System.arraycopy(a, i + 1, a, i, n);
            }
            public void clear() { size = 0; }
            public void trim() { if (size < a.length) a = Arrays.copyOf(a, size); }
            public void sort() { Arrays.sort(a, 0, size); }
            public int binarySearch(long l) { return Arrays.binarySearch(a, 0, size, l); }
            private void grow(int cap) { a = Arrays.copyOf(a, cap); }
            @FunctionalInterface
            public interface Op { void apply(Mutable m); }
        }
    }
    public static final class Vec3d {
        public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
        public static final Vec3d ONE = new Vec3d(1.0, 1.0, 1.0);
        public static final Vec3d X_AXIS = new Vec3d(1.0, 0.0, 0.0);
        public static final Vec3d Y_AXIS = new Vec3d(0.0, 1.0, 0.0);
        public static final Vec3d Z_AXIS = new Vec3d(0.0, 0.0, 1.0);
        public final double x, y, z;
        public Vec3d(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
        public Vec3d(double xyz) { this(xyz, xyz, xyz); }
        public Vec3d(Vec3 vec) { this.x = vec.x; this.y = vec.y; this.z = vec.z; }
        public Vec3d(Vector3dc v) { this.x = v.x(); this.y = v.y(); this.z = v.z(); }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public Vec3d withX(double x) { return new Vec3d(x, y, z); }
        public Vec3d withY(double y) { return new Vec3d(x, y, z); }
        public Vec3d withZ(double z) { return new Vec3d(x, y, z); }
        public Vec3d add(double x, double y, double z) { return new Vec3d(this.x + x, this.y + y, this.z + z); }
        public Vec3d sub(double x, double y, double z) { return add(-x, -y, -z); }
        public Vec3d mul(double f) { return new Vec3d(x * f, y * f, z * f); }
        public Vec3d div(double f) { return new Vec3d(x / f, y / f, z / f); }
        public Vec3d negate() { return mul(-1.0); }
        public Vec3d cross(Vec3d o) { return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        public Vec3d cross(Vec3 o) { return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        public Vec3d cross(Mutable o) { return new Vec3d(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        public Vec3d lerp(Vec3d to, double d) { return new Vec3d(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
        public Vec3d lerp(Vec3 to, double d) { return new Vec3d(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
        public Vec3d lerp(Mutable to, double d) { return new Vec3d(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
        public Vec3d lerpf(Vec3d to, double d) { return new Vec3d(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
        public Vec3d lerpf(Vec3 to, double d) { return new Vec3d(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
        public Vec3d lerpf(Mutable to, double d) { return new Vec3d(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
        public Vec3d normalize() {
            double s = len();
            return s < (double) 1.0E-5F ? ZERO : div(s);
        }
        public Vec3d normalizef() {
            double s = lenSqrf();
            if (s < 1.0E-10) return ZERO;
            double i = 1.0 / Math.sqrt(s);
            return new Vec3d(x * i, y * i, z * i);
        }
        public double lenSqr() { return x * x + y * y + z * z; }
        public double lenSqrf() { return Math.fma(x, x, Math.fma(y, y, z * z)); }
        public double len() { return Math.sqrt(lenSqr()); }
        public double lenf() { return Math.sqrt(lenSqrf()); }
        public double dot(Vec3d o) { return x * o.x + y * o.y + z * o.z; }
        public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
        public double dot(Mutable o) { return x * o.x + y * o.y + z * o.z; }
        public double dot(double ox, double oy, double oz) { return x * ox + y * oy + z * oz; }
        public double dotf(Vec3d o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
        public double dotf(Vec3 o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
        public double dotf(Mutable o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
        public double dotf(double ox, double oy, double oz) { return Math.fma(x, ox, Math.fma(y, oy, z * oz)); }
        public double distSqr(Vec3d o) { return distSqr(o.x, o.y, o.z); }
        public double distSqr(Vec3 o) { return distSqr(o.x, o.y, o.z); }
        public double distSqr(Mutable o) { return distSqr(o.x, o.y, o.z); }
        public double distSqr(double ox, double oy, double oz) {
            double xd = ox - x, yd = oy - y, zd = oz - z;
            return xd * xd + yd * yd + zd * zd;
        }
        public double distSqrf(Vec3d o) { return distSqrf(o.x, o.y, o.z); }
        public double distSqrf(Vec3 o) { return distSqrf(o.x, o.y, o.z); }
        public double distSqrf(Mutable o) { return distSqrf(o.x, o.y, o.z); }
        public double distSqrf(double ox, double oy, double oz) {
            double xd = ox - x, yd = oy - y, zd = oz - z;
            return Math.fma(xd, xd, Math.fma(yd, yd, zd * zd));
        }
        public double dist(Vec3d o) { return Math.sqrt(distSqr(o)); }
        public double dist(Vec3 o) { return Math.sqrt(distSqr(o)); }
        public double dist(Mutable o) { return Math.sqrt(distSqr(o)); }
        public double distf(Vec3d o) { return Math.sqrt(distSqrf(o)); }
        public double distf(Vec3 o) { return Math.sqrt(distSqrf(o)); }
        public double distf(Mutable o) { return Math.sqrt(distSqrf(o)); }
        public double horizontalDistSqr() { return x * x + z * z; }
        public double horizontalDistSqrf() { return Math.fma(x, x, z * z); }
        public double horizontalDist() { return Math.sqrt(horizontalDistSqr()); }
        public double horizontalDistf() { return Math.sqrt(horizontalDistSqrf()); }
        public boolean ifZero() { return x == 0.0 && y == 0.0 && z == 0.0; }
        public boolean ifFinite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
        public Vector3d vector3d() { return new Vector3d(x, y, z); }
        public Vec3 vec3() { return new Vec3(x, y, z); }
        public Pos floorPos() { return new Pos(x, y, z); }
        public Mutable mutable() { return new Mutable(this); }
        private static boolean tst(double ax, double ay, double az, double bx, double by, double bz) { return Double.doubleToLongBits(ax) == Double.doubleToLongBits(bx) && Double.doubleToLongBits(ay) == Double.doubleToLongBits(by) && Double.doubleToLongBits(az) == Double.doubleToLongBits(bz); }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Vec3d) { Vec3d t = (Vec3d) o; return tst(x, y, z, t.x, t.y, t.z); }
            if (o instanceof Mutable) { Mutable t = (Mutable) o; return tst(x, y, z, t.x, t.y, t.z); }
            return false;
        }
        @Override
        public int hashCode() {
            int hs = Double.hashCode(x);
            hs = 31 * hs + Double.hashCode(y);
            return 31 * hs + Double.hashCode(z);
        }
        @Override
        public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
        public static final class Mutable {
            public double x, y, z;
            public Mutable() {}
            public Mutable(double x, double y, double z) { set(x, y, z); }
            public Mutable(double xyz) { set(xyz); }
            public Mutable(Vec3d v) { set(v); }
            public Mutable(Vec3 v) { set(v); }
            public Mutable(Vector3dc v) { set(v); }
            public Mutable(Mutable m) { set(m); }
            public Mutable set(double x, double y, double z) { this.x = x; this.y = y; this.z = z; return this; }
            public Mutable set(double xyz) { return set(xyz, xyz, xyz); }
            public Mutable set(Vec3d v) { return set(v.x, v.y, v.z); }
            public Mutable set(Mutable m) { return set(m.x, m.y, m.z); }
            public Mutable set(Vec3 v) { return set(v.x, v.y, v.z); }
            public Mutable set(Vector3dc v) { return set(v.x(), v.y(), v.z()); }
            public double x() { return x; }
            public double y() { return y; }
            public double z() { return z; }
            public Mutable withX(double x) { this.x = x; return this; }
            public Mutable withY(double y) { this.y = y; return this; }
            public Mutable withZ(double z) { this.z = z; return this; }
            public Mutable setZero() { return set(0.0, 0.0, 0.0); }
            public Mutable add(double x, double y, double z) { this.x += x; this.y += y; this.z += z; return this; }
            public Mutable sub(double x, double y, double z) { return add(-x, -y, -z); }
            public Mutable mul(double f) { x *= f; y *= f; z *= f; return this; }
            public Mutable div(double f) { x /= f; y /= f; z /= f; return this; }
            public Mutable negate() { return mul(-1.0); }
            public Mutable cross(Vec3d o) { return set(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
            public Mutable cross(Vec3 o) { return set(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
            public Mutable cross(Mutable o) { return set(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
            public Mutable lerp(Vec3d to, double d) { return set(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
            public Mutable lerp(Vec3 to, double d) { return set(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
            public Mutable lerp(Mutable to, double d) { return set(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
            public Mutable lerpf(Vec3d to, double d) { return set(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
            public Mutable lerpf(Vec3 to, double d) { return set(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
            public Mutable lerpf(Mutable to, double d) { return set(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
            public Mutable normalize() {
                double s = len();
                return s < (double) 1.0E-5F ? setZero() : div(s);
            }
            public Mutable normalizef() {
                double s = lenSqrf();
                if (s < 1.0E-10) return setZero();
                double i = 1.0 / Math.sqrt(s);
                return set(x * i, y * i, z * i);
            }
            public double lenSqr() { return x * x + y * y + z * z; }
            public double lenSqrf() { return Math.fma(x, x, Math.fma(y, y, z * z)); }
            public double len() { return Math.sqrt(lenSqr()); }
            public double lenf() { return Math.sqrt(lenSqrf()); }
            public double dot(Vec3d o) { return x * o.x + y * o.y + z * o.z; }
            public double dot(Vec3 v) { return x * v.x + y * v.y + z * v.z; }
            public double dot(Mutable v) { return x * v.x + y * v.y + z * v.z; }
            public double dot(double ox, double oy, double oz) { return x * ox + y * oy + z * oz; }
            public double dotf(Vec3d o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
            public double dotf(Vec3 o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
            public double dotf(Mutable o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
            public double dotf(double ox, double oy, double oz) { return Math.fma(x, ox, Math.fma(y, oy, z * oz)); }
            public double distSqr(Vec3d o) { return distSqr(o.x, o.y, o.z); }
            public double distSqr(Vec3 o) { return distSqr(o.x, o.y, o.z); }
            public double distSqr(Mutable o) { return distSqr(o.x, o.y, o.z); }
            public double distSqr(double ox, double oy, double oz) {
                double xd = ox - x, yd = oy - y, zd = oz - z;
                return xd * xd + yd * yd + zd * zd;
            }
            public double distSqrf(Vec3d o) { return distSqrf(o.x, o.y, o.z); }
            public double distSqrf(Vec3 o) { return distSqrf(o.x, o.y, o.z); }
            public double distSqrf(Mutable o) { return distSqrf(o.x, o.y, o.z); }
            public double distSqrf(double ox, double oy, double oz) {
                double xd = ox - x, yd = oy - y, zd = oz - z;
                return Math.fma(xd, xd, Math.fma(yd, yd, zd * zd));
            }
            public double dist(Vec3d o) { return Math.sqrt(distSqr(o)); }
            public double dist(Vec3 o) { return Math.sqrt(distSqr(o)); }
            public double dist(Mutable o) { return Math.sqrt(distSqr(o)); }
            public double distf(Vec3d o) { return Math.sqrt(distSqrf(o)); }
            public double distf(Vec3 o) { return Math.sqrt(distSqrf(o)); }
            public double distf(Mutable o) { return Math.sqrt(distSqrf(o)); }
            public double horizontalDistSqr() { return x * x + z * z; }
            public double horizontalDistSqrf() { return Math.fma(x, x, z * z); }
            public double horizontalDist() { return Math.sqrt(horizontalDistSqr()); }
            public double horizontalDistf() { return Math.sqrt(horizontalDistSqrf()); }
            public boolean ifZero() { return x == 0.0 && y == 0.0 && z == 0.0; }
            public boolean ifFinite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
            public Vector3d vector3d() { return new Vector3d(x, y, z); }
            public Vec3 vec3() { return new Vec3(x, y, z); }
            public Pos floorPos() { return new Pos(x, y, z); }
            public Vec3d freeze() { return new Vec3d(x, y, z); }
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o instanceof Mutable) { Mutable t = (Mutable) o; return tst(x, y, z, t.x, t.y, t.z); }
                if (o instanceof Vec3d) { Vec3d t = (Vec3d) o; return tst(x, y, z, t.x, t.y, t.z); }
                return false;
            }
            @Override
            public int hashCode() {
                int result = Double.hashCode(x);
                result = 31 * result + Double.hashCode(y);
                return 31 * result + Double.hashCode(z);
            }
            @Override
            public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
        }
        public static final class List {
            private double[] xs, ys, zs;
            private int size;
            public List(int c) { xs = new double[Math.max(8, c)]; ys = new double[xs.length]; zs = new double[xs.length]; }
            public List() { this(8); }
            public int size() { return size; }
            public boolean isEmpty() { return size == 0; }
            public void add(double x, double y, double z) {
                if (size == xs.length) grow(size + (size >> 1) + 8);
                xs[size] = x; ys[size] = y; zs[size] = z; size++;
            }
            public void add(Vec3d v) { add(v.x, v.y, v.z); }
            public void add(Mutable v) { add(v.x, v.y, v.z); }
            public void add(Vec3 v) { add(v.x, v.y, v.z); }
            public void insert(int i, double x, double y, double z) {
                if (size == xs.length) grow(size + (size >> 1) + 8);
                System.arraycopy(xs, i, xs, i + 1, size - i); System.arraycopy(ys, i, ys, i + 1, size - i); System.arraycopy(zs, i, zs, i + 1, size - i);
                xs[i] = x; ys[i] = y; zs[i] = z; size++;
            }
            public void insert(int i, Vec3d v) { insert(i, v.x, v.y, v.z); }
            public void addAll(List o) {
                if (o.size == 0) return;
                if (size + o.size > xs.length) grow(size + o.size);
                System.arraycopy(o.xs, 0, xs, size, o.size); System.arraycopy(o.ys, 0, ys, size, o.size); System.arraycopy(o.zs, 0, zs, size, o.size);
                size += o.size;
            }
            public void set(int i, double x, double y, double z) { xs[i] = x; ys[i] = y; zs[i] = z; }
            public void set(int i, Vec3d v) { xs[i] = v.x; ys[i] = v.y; zs[i] = v.z; }
            public void set(int i, Mutable v) { xs[i] = v.x; ys[i] = v.y; zs[i] = v.z; }
            public void swap(int i, int j) {
                double tx = xs[i]; xs[i] = xs[j]; xs[j] = tx;
                double ty = ys[i]; ys[i] = ys[j]; ys[j] = ty;
                double tz = zs[i]; zs[i] = zs[j]; zs[j] = tz;
            }
            public void apply(Op op) {
                Mutable m = new Mutable();
                for (int i = 0; i < size; i++) {
                    m.set(xs[i], ys[i], zs[i]);
                    op.apply(m);
                    xs[i] = m.x; ys[i] = m.y; zs[i] = m.z;
                }
            }
            public double x(int i) { return xs[i]; }
            public double y(int i) { return ys[i]; }
            public double z(int i) { return zs[i]; }
            public Vec3d get(int i) { return new Vec3d(xs[i], ys[i], zs[i]); }
            public Mutable get(int i, Mutable out) { return out.set(xs[i], ys[i], zs[i]); }
            public double[] flatArray() {
                double[] out = new double[size * 3];
                for (int i = 0, j = 0; i < size; i++) { out[j++] = xs[i]; out[j++] = ys[i]; out[j++] = zs[i]; }
                return out;
            }
            public void rmLast() { size--; }
            public void pop(int i) {
                int last = --size;
                xs[i] = xs[last]; ys[i] = ys[last]; zs[i] = zs[last];
            }
            public void rm(int i) {
                int n = --size - i;
                System.arraycopy(xs, i + 1, xs, i, n); System.arraycopy(ys, i + 1, ys, i, n); System.arraycopy(zs, i + 1, zs, i, n);
            }
            public void clear() { size = 0; }
            public void trim() {
                if (size < xs.length) {
                    xs = Arrays.copyOf(xs, size); ys = Arrays.copyOf(ys, size); zs = Arrays.copyOf(zs, size);
                }
            }
            private void grow(int c) { xs = Arrays.copyOf(xs, c); ys = Arrays.copyOf(ys, c); zs = Arrays.copyOf(zs, c); }
            @FunctionalInterface
            public static interface Op { void apply(Mutable m); }
        }
    }
    public static final class Vec3f {
        public static final Vec3f ZERO = new Vec3f(0.0F, 0.0F, 0.0F);
        public static final Vec3f ONE = new Vec3f(1.0F, 1.0F, 1.0F);
        public static final Vec3f X_AXIS = new Vec3f(1.0F, 0.0F, 0.0F);
        public static final Vec3f Y_AXIS = new Vec3f(0.0F, 1.0F, 0.0F);
        public static final Vec3f Z_AXIS = new Vec3f(0.0F, 0.0F, 1.0F);
        public final float x, y, z;
        public Vec3f(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
        public Vec3f(float xyz) { this(xyz, xyz, xyz); }
        public Vec3f(double x, double y, double z) { this((float) x, (float) y, (float) z); }
        public Vec3f(Vec3 vec) { this((float) vec.x, (float) vec.y, (float) vec.z); }
        public Vec3f(Vector3fc v) { this.x = v.x(); this.y = v.y(); this.z = v.z(); }
        public float x() { return x; }
        public float y() { return y; }
        public float z() { return z; }
        public Vec3f withX(float x) { return new Vec3f(x, y, z); }
        public Vec3f withY(float y) { return new Vec3f(x, y, z); }
        public Vec3f withZ(float z) { return new Vec3f(x, y, z); }
        public Vec3f setZero() { return ZERO; }
        public Vec3f add(float x, float y, float z) { return new Vec3f(this.x + x, this.y + y, this.z + z); }
        public Vec3f sub(float x, float y, float z) { return add(-x, -y, -z); }
        public Vec3f mul(float f) { return new Vec3f(x * f, y * f, z * f); }
        public Vec3f div(float f) { return new Vec3f(x / f, y / f, z / f); }
        public Vec3f negate() { return mul(-1.0F); }
        public Vec3f cross(Vec3f o) { return new Vec3f(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        public Vec3f cross(Vec3 o) {
            float ox = (float) o.x, oy = (float) o.y, oz = (float) o.z;
            return new Vec3f(y * oz - z * oy, z * ox - x * oz, x * oy - y * ox);
        }
        public Vec3f cross(Mutable o) { return new Vec3f(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
        public Vec3f lerp(Vec3f to, float d) { return new Vec3f(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
        public Vec3f lerp(Vec3 to, float d) {
            float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;
            return new Vec3f(Mth.lerp(d, x, tx), Mth.lerp(d, y, ty), Mth.lerp(d, z, tz));
        }
        public Vec3f lerp(Mutable to, float d) { return new Vec3f(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
        public Vec3f lerpf(Vec3f to, float d) { return new Vec3f(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
        public Vec3f lerpf(Vec3 to, float d) {
            float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;
            return new Vec3f(Math.fma(d, tx - x, x), Math.fma(d, ty - y, y), Math.fma(d, tz - z, z));
        }
        public Vec3f lerpf(Mutable to, float d) { return new Vec3f(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
        public Vec3f normalize() {
            float s = len();
            return s < 1.0E-5F ? ZERO : div(s);
        }
        public Vec3f normalizef() {
            float s = lenSqrf();
            if (s < 1.0E-10F) return ZERO;
            float i = 1.0f / (float) Math.sqrt(s);
            return new Vec3f(x * i, y * i, z * i);
        }
        public float lenSqr() { return x * x + y * y + z * z; }
        public float lenSqrf() { return Math.fma(x, x, Math.fma(y, y, z * z)); }
        public float len() { return (float) Math.sqrt(lenSqr()); }
        public float lenf() { return (float) Math.sqrt(lenSqrf()); }
        public float dot(Vec3f o) { return x * o.x + y * o.y + z * o.z; }
        public float dot(Vec3 o) { return dot((float) o.x, (float) o.y, (float) o.z); }
        public float dot(Mutable o) { return x * o.x + y * o.y + z * o.z; }
        public float dot(float ox, float oy, float oz) { return x * ox + y * oy + z * oz; }
        public float dotf(Vec3f o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
        public float dotf(Vec3 o) { return Math.fma(x, (float) o.x, Math.fma(y, (float) o.y, z * (float) o.z)); }
        public float dotf(Mutable o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
        public float dotf(float ox, float oy, float oz) { return Math.fma(x, ox, Math.fma(y, oy, z * oz)); }
        public float distSqr(Vec3f o) { return distSqr(o.x, o.y, o.z); }
        public float distSqr(Vec3 o) { return distSqr((float) o.x, (float) o.y, (float) o.z); }
        public float distSqr(Mutable o) { return distSqr(o.x, o.y, o.z); }
        public float distSqr(float ox, float oy, float oz) {
            float xd = ox - x, yd = oy - y, zd = oz - z;
            return xd * xd + yd * yd + zd * zd;
        }
        public float distSqrf(Vec3f o) { return distSqrf(o.x, o.y, o.z); }
        public float distSqrf(Vec3 o) { return distSqrf((float) o.x, (float) o.y, (float) o.z); }
        public float distSqrf(Mutable o) { return distSqrf(o.x, o.y, o.z); }
        public float distSqrf(float ox, float oy, float oz) {
            float xd = ox - x, yd = oy - y, zd = oz - z;
            return Math.fma(xd, xd, Math.fma(yd, yd, zd * zd));
        }
        public float dist(Vec3f o) { return (float) Math.sqrt(distSqr(o)); }
        public float dist(Vec3 o) { return (float) Math.sqrt(distSqr(o)); }
        public float dist(Mutable o) { return (float) Math.sqrt(distSqr(o)); }
        public float distf(Vec3f o) { return (float) Math.sqrt(distSqrf(o)); }
        public float distf(Vec3 o) { return (float) Math.sqrt(distSqrf(o)); }
        public float distf(Mutable o) { return (float) Math.sqrt(distSqrf(o)); }
        public float horizontalDistSqr() { return x * x + z * z; }
        public float horizontalDistSqrf() { return Math.fma(x, x, z * z); }
        public float horizontalDist() { return (float) Math.sqrt(horizontalDistSqr()); }
        public float horizontalDistf() { return (float) Math.sqrt(horizontalDistSqrf()); }
        public boolean ifZero() { return x == 0.0F && y == 0.0F && z == 0.0F; }
        public boolean ifFinite() { return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z); }
        public Vector3f vector3f() { return new Vector3f(x, y, z); }
        public Vec3 vec3() { return new Vec3(x, y, z); }
        public Pos floorPos() { return new Pos(x, y, z); }
        public Mutable mutable() { return new Mutable(this); }
        private static boolean tst(float ax, float ay, float az, float bx, float by, float bz) { return Float.floatToIntBits(ax) == Float.floatToIntBits(bx) && Float.floatToIntBits(ay) == Float.floatToIntBits(by) && Float.floatToIntBits(az) == Float.floatToIntBits(bz); }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Vec3f) { Vec3f t = (Vec3f) o; return tst(x, y, z, t.x, t.y, t.z); }
            if (o instanceof Mutable) { Mutable t = (Mutable) o; return tst(x, y, z, t.x, t.y, t.z); }
            return false;
        }
        @Override
        public int hashCode() {
            int result = Float.hashCode(x);
            result = 31 * result + Float.hashCode(y);
            return 31 * result + Float.hashCode(z);
        }
        @Override
        public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
        public static final class Mutable {
            public float x, y, z;
            public Mutable() {}
            public Mutable(float x, float y, float z) { set(x, y, z); }
            public Mutable(float xyz) { set(xyz); }
            public Mutable(Vec3f v) { set(v); }
            public Mutable(Vec3 v) { set(v); }
            public Mutable(Vector3fc v) { set(v); }
            public Mutable(Mutable m) { set(m); }
            public Mutable set(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
            public Mutable set(float xyz) { return set(xyz, xyz, xyz); }
            public Mutable set(Vec3f v) { return set(v.x, v.y, v.z); }
            public Mutable set(Mutable m) { return set(m.x, m.y, m.z); }
            public Mutable set(Vec3 v) { return set((float) v.x, (float) v.y, (float) v.z); }
            public Mutable set(Vector3fc v) { return set(v.x(), v.y(), v.z()); }
            public float x() { return x; }
            public float y() { return y; }
            public float z() { return z; }
            public Mutable withX(float x) { this.x = x; return this; }
            public Mutable withY(float y) { this.y = y; return this; }
            public Mutable withZ(float z) { this.z = z; return this; }
            public Mutable setZero() { return set(0.0F, 0.0F, 0.0F); }
            public Mutable add(float x, float y, float z) { this.x += x; this.y += y; this.z += z; return this; }
            public Mutable sub(float x, float y, float z) { return add(-x, -y, -z); }
            public Mutable mul(float f) { x *= f; y *= f; z *= f; return this; }
            public Mutable div(float f) { x /= f; y /= f; z /= f; return this; }
            public Mutable negate() { return mul(-1.0F); }
            public Mutable cross(Vec3f o) { return set(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
            public Mutable cross(Vec3 o) {
                float ox = (float) o.x, oy = (float) o.y, oz = (float) o.z;
                return set(y * oz - z * oy, z * ox - x * oz, x * oy - y * ox);
            }
            public Mutable cross(Mutable o) { return set(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
            public Mutable lerp(Vec3f to, float d) { return set(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
            public Mutable lerp(Vec3 to, float d) {
                float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;
                return set(Mth.lerp(d, x, tx), Mth.lerp(d, y, ty), Mth.lerp(d, z, tz));
            }
            public Mutable lerp(Mutable to, float d) { return set(Mth.lerp(d, x, to.x), Mth.lerp(d, y, to.y), Mth.lerp(d, z, to.z)); }
            public Mutable lerpf(Vec3f to, float d) { return set(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
            public Mutable lerpf(Vec3 to, float d) {
                float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;
                return set(Math.fma(d, tx - x, x), Math.fma(d, ty - y, y), Math.fma(d, tz - z, z));
            }
            public Mutable lerpf(Mutable to, float d) { return set(Math.fma(d, to.x - x, x), Math.fma(d, to.y - y, y), Math.fma(d, to.z - z, z)); }
            public Mutable normalize() {
                float s = len();
                return s < 1.0E-5F ? setZero() : div(s);
            }
            public Mutable normalizef() {
                float s = lenSqrf();
                if (s < 1.0E-10F) return setZero();
                float i = 1.0f / (float) Math.sqrt(s);
                return set(x * i, y * i, z * i);
            }
            public float lenSqr() { return x * x + y * y + z * z; }
            public float lenSqrf() { return Math.fma(x, x, Math.fma(y, y, z * z)); }
            public float len() { return (float) Math.sqrt(lenSqr()); }
            public float lenf() { return (float) Math.sqrt(lenSqrf()); }
            public float dot(Vec3f o) { return x * o.x + y * o.y + z * o.z; }
            public float dot(Vec3 o) { return dot((float) o.x, (float) o.y, (float) o.z); }
            public float dot(Mutable o) { return x * o.x + y * o.y + z * o.z; }
            public float dot(float ox, float oy, float oz) { return x * ox + y * oy + z * oz; }
            public float dotf(Vec3f o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
            public float dotf(Vec3 o) { return Math.fma(x, (float) o.x, Math.fma(y, (float) o.y, z * (float) o.z)); }
            public float dotf(Mutable o) { return Math.fma(x, o.x, Math.fma(y, o.y, z * o.z)); }
            public float dotf(float ox, float oy, float oz) { return Math.fma(x, ox, Math.fma(y, oy, z * oz)); }
            public float distSqr(Vec3f o) { return distSqr(o.x, o.y, o.z); }
            public float distSqr(Vec3 o) { return distSqr((float) o.x, (float) o.y, (float) o.z); }
            public float distSqr(Mutable o) { return distSqr(o.x, o.y, o.z); }
            public float distSqr(float ox, float oy, float oz) {
                float xd = ox - this.x, yd = oy - this.y, zd = oz - this.z;
                return xd * xd + yd * yd + zd * zd;
            }
            public float distSqrf(Vec3f o) { return distSqrf(o.x, o.y, o.z); }
            public float distSqrf(Vec3 o) { return distSqrf((float) o.x, (float) o.y, (float) o.z); }
            public float distSqrf(Mutable o) { return distSqrf(o.x, o.y, o.z); }
            public float distSqrf(float ox, float oy, float oz) {
                float xd = ox - x, yd = oy - y, zd = oz - z;
                return Math.fma(xd, xd, Math.fma(yd, yd, zd * zd));
            }
            public float dist(Vec3f o) { return (float) Math.sqrt(distSqr(o)); }
            public float dist(Vec3 o) { return (float) Math.sqrt(distSqr(o)); }
            public float dist(Mutable o) { return (float) Math.sqrt(distSqr(o)); }
            public float distf(Vec3f o) { return (float) Math.sqrt(distSqrf(o)); }
            public float distf(Vec3 o) { return (float) Math.sqrt(distSqrf(o)); }
            public float distf(Mutable o) { return (float) Math.sqrt(distSqrf(o)); }
            public float horizontalDistSqr() { return x * x + z * z; }
            public float horizontalDistSqrf() { return Math.fma(x, x, z * z); }
            public float horizontalDist() { return (float) Math.sqrt(horizontalDistSqr()); }
            public float horizontalDistf() { return (float) Math.sqrt(horizontalDistSqrf()); }
            public boolean ifZero() { return x == 0.0F && y == 0.0F && z == 0.0F; }
            public boolean ifFinite() { return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z); }
            public Vector3f vector3f() { return new Vector3f(x, y, z); }
            public Vec3 vec3() { return new Vec3(x, y, z); }
            public Pos floorPos() { return new Pos(x, y, z); }
            public Vec3f freeze() { return new Vec3f(x, y, z); }
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o instanceof Mutable) { Mutable t = (Mutable) o; return tst(x, y, z, t.x, t.y, t.z); }
                if (o instanceof Vec3f) { Vec3f t = (Vec3f) o; return tst(x, y, z, t.x, t.y, t.z); }
                return false;
            }
            @Override
            public int hashCode() {
                int result = Float.hashCode(x);
                result = 31 * result + Float.hashCode(y);
                return 31 * result + Float.hashCode(z);
            }
            @Override
            public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
        }
        public static final class List {
            private float[] xs, ys, zs;
            private int size;
            public List(int c) { xs = new float[Math.max(8, c)]; ys = new float[xs.length]; zs = new float[xs.length]; }
            public List() { this(8); }
            public int size() { return size; }
            public boolean isEmpty() { return size == 0; }
            public void add(float x, float y, float z) {
                if (size == xs.length) grow(size + (size >> 1) + 8);
                xs[size] = x; ys[size] = y; zs[size] = z; size++;
            }
            public void add(Vec3f v) { add(v.x, v.y, v.z); }
            public void add(Mutable v) { add(v.x, v.y, v.z); }
            public void add(Vec3 v) { add((float) v.x, (float) v.y, (float) v.z); }
            public void insert(int i, float x, float y, float z) {
                if (size == xs.length) grow(size + (size >> 1) + 8);
                System.arraycopy(xs, i, xs, i + 1, size - i); System.arraycopy(ys, i, ys, i + 1, size - i); System.arraycopy(zs, i, zs, i + 1, size - i);
                xs[i] = x; ys[i] = y; zs[i] = z; size++;
            }
            public void insert(int i, Vec3f v) { insert(i, v.x, v.y, v.z); }
            public void addAll(List o) {
                if (o.size == 0) return;
                if (size + o.size > xs.length) grow(size + o.size);
                System.arraycopy(o.xs, 0, xs, size, o.size); System.arraycopy(o.ys, 0, ys, size, o.size); System.arraycopy(o.zs, 0, zs, size, o.size);
                size += o.size;
            }
            public void set(int i, float x, float y, float z) { xs[i] = x; ys[i] = y; zs[i] = z; }
            public void set(int i, Vec3f v) { xs[i] = v.x; ys[i] = v.y; zs[i] = v.z; }
            public void set(int i, Mutable v) { xs[i] = v.x; ys[i] = v.y; zs[i] = v.z; }
            public void swap(int i, int j) {
                float tx = xs[i]; xs[i] = xs[j]; xs[j] = tx;
                float ty = ys[i]; ys[i] = ys[j]; ys[j] = ty;
                float tz = zs[i]; zs[i] = zs[j]; zs[j] = tz;
            }
            public void apply(Op op) {
                Mutable m = new Mutable();
                for (int i = 0; i < size; i++) {
                    m.set(xs[i], ys[i], zs[i]);
                    op.apply(m);
                    xs[i] = m.x; ys[i] = m.y; zs[i] = m.z;
                }
            }
            public float x(int i) { return xs[i]; }
            public float y(int i) { return ys[i]; }
            public float z(int i) { return zs[i]; }
            public Vec3f get(int i) { return new Vec3f(xs[i], ys[i], zs[i]); }
            public Mutable get(int i, Mutable out) { return out.set(xs[i], ys[i], zs[i]); }
            public float[] flatArray() {
                float[] out = new float[size * 3];
                for (int i = 0, j = 0; i < size; i++) { out[j++] = xs[i]; out[j++] = ys[i]; out[j++] = zs[i]; }
                return out;
            }
            public void rmLast() { size--; }
            public void pop(int i) {
                int last = --size;
                xs[i] = xs[last]; ys[i] = ys[last]; zs[i] = zs[last];
            }
            public void rm(int i) {
                int n = --size - i;
                System.arraycopy(xs, i + 1, xs, i, n); System.arraycopy(ys, i + 1, ys, i, n); System.arraycopy(zs, i + 1, zs, i, n);
            }
            public void clear() { size = 0; }
            public void trim() {
                if (size < xs.length) {
                    xs = Arrays.copyOf(xs, size); ys = Arrays.copyOf(ys, size); zs = Arrays.copyOf(zs, size);
                }
            }
            private void grow(int cap) { xs = Arrays.copyOf(xs, cap); ys = Arrays.copyOf(ys, cap); zs = Arrays.copyOf(zs, cap); }
            @FunctionalInterface
            public static interface Op { void apply(Mutable m); }
        }
    }
}