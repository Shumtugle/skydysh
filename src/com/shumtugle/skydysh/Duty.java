package com.shumtugle.skydysh;

import java.util.ArrayList;

/**
 * The duty pickups about the town: one stands at the curb in a cell now and then (see
 * City.duty), parked where the cell keeps its place free. They are put out as she comes near
 * and taken in when she is far off; one she has driven stays where she left it, and its place
 * at the curb stays empty while it is out.
 */
final class Duty {
    static final float NEAR = 190f, FAR = 300f;
    final ArrayList<Pickup> all = new ArrayList<Pickup>();
    // the cells whose pickup has been driven off and is out somewhere else
    private final java.util.HashSet<Long> moved = new java.util.HashSet<Long>();
    private final float[] at = new float[3];

    /** The ones within reach of (x, z) put out, the ones left far behind taken in. */
    void update(float x, float z, City city, Pickup driving) {
        for (int k = all.size() - 1; k >= 0; k--) {
            Pickup p = all.get(k);
            // the one she drives, and one with a load on it, wherever it was left, stay out
            if (p == driving || p.load > 0) continue;
            float dx = p.x - x, dz = p.z - z;
            if (dx * dx + dz * dz > FAR * FAR) {
                all.remove(k);
                // driven off and left far away: back at its own curb when it is come to again
                moved.remove(p.home);
            }
        }
        int ci = City.index(x), cj = City.index(z), reach = (int) Math.ceil(NEAR / City.CELL);
        for (int j = cj - reach; j <= cj + reach; j++)
            for (int i = ci - reach; i <= ci + reach; i++) {
                if (!city.duty(i, j)) continue;
                long key = ((long) i << 32) ^ (j & 0xFFFFFFFFL);
                if (moved.contains(key) || has(key)) continue;
                City.dutyPlace(i, j, at);
                float dx = at[0] - x, dz = at[1] - z;
                if (dx * dx + dz * dz > NEAR * NEAR) continue;
                Pickup p = new Pickup(at[0], at[1], at[2], key);
                p.y = city.ground(p.x, p.z);
                all.add(p);
            }
    }

    private boolean has(long key) {
        for (Pickup p : all) if (p.home == key) return true;
        return false;
    }

    /** It has been driven off its place at the curb: the place stays empty while it is out. */
    void driven(Pickup p) {
        moved.add(p.home);
    }

    /** Pushes a round body out of every pickup but one (the one being driven, or none). */
    boolean push(float[] p, float rr, Pickup but) {
        boolean hit = false;
        for (int k = 0; k < all.size(); k++) {
            Pickup q = all.get(k);
            if (q != but) hit |= q.push(p, rr);
        }
        return hit;
    }

    void draw(DrawList d, float ex, float ez, float fx, float fz, float time) {
        for (int k = 0; k < all.size(); k++) {
            Pickup p = all.get(k);
            float dx = p.x - ex, dz = p.z - ez;
            if (dx * fx + dz * fz < -6f || dx * dx + dz * dz > 200f * 200f) continue;
            p.draw(d, time);
        }
    }
}
