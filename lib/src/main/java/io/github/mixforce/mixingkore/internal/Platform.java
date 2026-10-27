// <editor-fold defaultstate="collapsed" desc="// LGPL-3.0-or-later | Copyright (c) 2026 MixForce">
/* SPDX-License-Identifier: LGPL-3.0-or-later */
/* SPDX-FileCopyrightText: 2026 MixForce */
// </editor-fold>
package io.github.mixforce.mixingkore.internal;

public final class Platform {
    private Platform() {}
    static final String[] REQUIRED = {
            "jdk.incubator.vector.LongVector",
            "jdk.incubator.vector.FloatVector",
            "jdk.incubator.vector.DoubleVector",
    };
    public static final boolean VECTOR_OK = probe();
    public static final boolean BW = Boolean.getBoolean("mix.broadword");

    static boolean probe() {
        try {
            ClassLoader cl = Platform.class.getClassLoader();
            for (String name : REQUIRED) Class.forName(name, true, cl);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}