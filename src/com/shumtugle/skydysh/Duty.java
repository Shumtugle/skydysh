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

    /** The island's yacht, if it is out: hers to take while the yachts are whole. */
    Pickup boat() {
        for (int k = 0; k < all.size(); k++) if (all.get(k).boat) return all.get(k);
        return null;
    }

    /** All taken in, the yacht as well (it is back at its pier when they are put out again). */
    void clear() {
        all.clear();
        Island.boatOut = false;
    }

    /**
     * The ones within reach of (x, z) put out, the ones left far behind taken in; the yacht too,
     * if it may be out now (not in the job that ends at its pier).
     */
    void update(float x, float z, City city, Pickup driving, boolean yacht) {
        // the yacht: at its pier while the yachts are whole, wherever she left it; gone with them
        Pickup b = boat();
        if (Island.wrecked || !yacht) {
            if (b != null && b != driving) all.remove(b);
        } else if (b == null) {
            Island.mooring(at);
            all.add(new Pickup(at[0], at[1], at[2], BOAT, true));
        }
        Island.boatOut = boat() != null;
        for (int k = all.size() - 1; k >= 0; k--) {
            Pickup p = all.get(k);
            // the one she drives, and one with a load on it, wherever it was left, stay out; the yacht too
            if (p == driving || p.load > 0 || p.kept || p.boat) continue;
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

    /** So near another pickup at its own curb that two would stand there, meters. */
    static final float CLOSE = 20f;
    /** The yacht's own place: none at a curb. */
    static final long BOAT = 0xB0A7L;

    /**
     * She has got out of p here: one standing at its own curb close by (home's, as a rule) is
     * taken in, and p stands for it, so that there are not two. p's own place, if it had one,
     * gets its pickup back when she comes to it again.
     */
    void settle(Pickup p) {
        if (p.kept || p.load > 0 || p.boat) return;
        for (int k = all.size() - 1; k >= 0; k--) {
            Pickup q = all.get(k);
            if (q == p || q.kept || q.load > 0 || q.boat || moved.contains(q.home)) continue;
            float dx = q.x - p.x, dz = q.z - p.z;
            if (dx * dx + dz * dz > CLOSE * CLOSE) continue;
            all.remove(k);
            moved.remove(p.home);
            p.home = q.home;
            return;
        }
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

    /** The pickups about, all but one (the one she sits in while the eye is in her head; none, none). */
    void draw(DrawList d, float ex, float ez, float fx, float fz, float time, Pickup but) {
        for (int k = 0; k < all.size(); k++) {
            Pickup p = all.get(k);
            if (p == but) continue;
            float dx = p.x - ex, dz = p.z - ez;
            float far = p.boat ? 700f : 200f;
            if (dx * fx + dz * fz < -12f || dx * dx + dz * dz > far * far) continue;
            p.draw(d, time);
        }
    }
}
