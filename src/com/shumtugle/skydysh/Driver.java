package com.shumtugle.skydysh;

/**
 * The one who really drives the pickup. She sits at the wheel and turns it, but the way is kept by
 * this driver: down the middle of its lane, round the corners in a curve, past a slower car when
 * the other lane is clear, slower before a corner and before what stands in the way. She only says
 * where: to go on or to stop, and which way to turn at the next crossing (left, right, or on).
 *
 * Off the streets (a square, the promenade, the beach) there is no lane to keep: there the wheel
 * is hers, and the pickup goes slowly.
 *
 * The streets are the town's grid (see City): along x = i * CELL and z = j * CELL, none out to sea.
 * The driver keeps to the right of the middle; the turn is a curve from the lane in to the lane out.
 */
final class Driver {
    static final float LANE = 1.35f;          // its lane's middle, right of the road's middle
    static final float INTO = 6.5f;           // the curve of a turn begins so far before a crossing's middle
    static final float TOP = 30f, AROUND = 8.5f, ON_ROAD = 5.5f;
    static final float SLOWING = 9f;          // how hard it slows for a corner ahead, meters a second a second
    static final float OFF_ROAD_TOP = 12f;

    /** Where it is going: the crossing it set out from (ci, cj), the way it goes (di, dj). */
    int ci, cj, di, dj;
    /** What she said for the next crossing: -1 left, 0 on, 1 right. */
    int turn;
    /** Whether it is on a street, keeping its lane; off it, the wheel is hers. */
    boolean onRoad;
    // overtaking: on the other side of the road (-1) or its own (1), and the car being passed
    private float side = 1f;
    private int passing = -1;
    // the way it goes at the next crossing, as worked out from turn; whether the turn is taken at
    // this crossing (said too late to slow for it, it is kept for the next)
    private int ni, nj;
    private boolean committed;
    static final float EASING = 9f;           // how hard it slows for a corner at most, as the pickup can
    private final float[] at = new float[2];

    /** Got into at (x, z), facing h: the street it is on and the way along it, if it is on one. */
    void start(float x, float z, float h) {
        float fx = (float) Math.sin(h), fz = (float) Math.cos(h);
        turn = 0;
        committed = false;
        side = 1f;
        passing = -1;
        if (Math.abs(fx) > Math.abs(fz)) {
            di = fx > 0f ? 1 : -1;
            dj = 0;
            cj = Math.round(z / City.CELL);
            ci = (int) Math.floor(x / City.CELL) + (di < 0 ? 1 : 0);
        } else {
            di = 0;
            dj = fz > 0f ? 1 : -1;
            ci = Math.round(x / City.CELL);
            cj = (int) Math.floor(z / City.CELL) + (dj < 0 ? 1 : 0);
        }
        check(x, z);
    }

    /** Whether (x, z) is on the street it means to be on, near enough its lane; and the street there at all. */
    private void check(float x, float z) {
        float across = (x - ci * City.CELL) * dj - (z - cj * City.CELL) * di;
        onRoad = Math.abs(across) < ON_ROAD && City.road(cj, dj);
    }

    /** The way out of the next crossing, as she asked if there is such a street; on, or the other turn, if not. */
    private void plan(int turn) {
        int ti = ci + di, tj = cj + dj;
        int li = -dj, lj = di, ri = dj, rj = -di;
        int[][] tries = turn < 0 ? new int[][]{{li, lj}, {di, dj}, {ri, rj}} : turn > 0 ? new int[][]{{ri, rj}, {di, dj}, {li, lj}}
                : new int[][]{{di, dj}, {ri, rj}, {li, lj}};
        for (int[] w : tries)
            if (City.road(tj, w[1])) {
                ni = w[0];
                nj = w[1];
                return;
            }
        ni = -di;
        nj = -dj;
    }

    /**
     * A point on the way ahead, u meters on from where it is (along s, meters past the crossing it
     * set out from), into at: down its lane, round the curve at the next crossing, on down the
     * lane out.
     */
    private void ahead(float s, float u) {
        ahead(s, u, Float.NaN, Float.NaN);
    }

    /** The same, from where it is (px, pz): once into the curve, how far along it is found from there. */
    private void ahead(float s, float u, float px, float pz) {
        float cx = ci * City.CELL, cz = cj * City.CELL;
        float tx = cx + di * City.CELL, tz = cz + dj * City.CELL;
        float rx = dj, rz = -di, ox = nj, oz = -ni;
        float into = City.CELL - INTO - s;
        if (u <= into) {
            float k = s + u;
            at[0] = cx + di * k + rx * LANE * side;
            at[1] = cz + dj * k + rz * LANE * side;
            return;
        }
        float ex = tx - di * INTO + rx * LANE, ez = tz - dj * INTO + rz * LANE;
        float xx = tx + ni * INTO + ox * LANE, xz = tz + nj * INTO + oz * LANE;
        // the corner of the curve: where the lane in and the lane out would meet
        float qx, qz;
        if (ni == di && nj == dj) {
            qx = (ex + xx) * 0.5f;
            qz = (ez + xz) * 0.5f;
        } else {
            qx = tx + rx * LANE + ox * LANE;
            qz = tz + rz * LANE + oz * LANE;
        }
        float len = dist(ex, ez, qx, qz) + dist(qx, qz, xx, xz);
        float v = u - Math.max(0f, into);
        if (into < 0f && !Float.isNaN(px)) {
            // into the curve: how far along it it is now, the nearest of a few points on it
            float best = 1e9f, bt = 0f;
            for (int k = 0; k <= 16; k++) {
                float t = k / 16f, a = 1f - t;
                float bx = a * a * ex + 2f * a * t * qx + t * t * xx, bz = a * a * ez + 2f * a * t * qz + t * t * xz;
                float dd = (bx - px) * (bx - px) + (bz - pz) * (bz - pz);
                if (dd < best) {
                    best = dd;
                    bt = t;
                }
            }
            v = bt * len + u;
        }
        if (v <= len) {
            float t = v / Math.max(1e-3f, len), a = 1f - t;
            at[0] = a * a * ex + 2f * a * t * qx + t * t * xx;
            at[1] = a * a * ez + 2f * a * t * qz + t * t * xz;
            return;
        }
        at[0] = xx + ni * (v - len);
        at[1] = xz + nj * (v - len);
    }

    private static float dist(float ax, float az, float bx, float bz) {
        return (float) Math.sqrt((ax - bx) * (ax - bx) + (az - bz) * (az - bz));
    }

    /**
     * A frame at the wheel: go (a finger down) or not, brake, her wheel (off the streets); the town
     * and the traffic. Into out: the wheel (-1 to 1) and the speed wanted.
     */
    void drive(Pickup p, boolean go, float handWheel, Cars cars, City city, Runner r, float[] out) {
        if (!onRoad) {
            // off the streets the wheel is hers; back on one, the driver takes it again
            out[0] = handWheel;
            out[1] = go ? OFF_ROAD_TOP : 0f;
            start(p.x, p.z, p.heading);
            return;
        }
        float cx = ci * City.CELL, cz = cj * City.CELL;
        float s = (p.x - cx) * di + (p.z - cz) * dj;
        float v = Math.abs(p.speed);
        // a turn said: taken here if there is room yet to slow for it, kept for the next crossing if not
        if (turn != 0 && !committed) {
            float need = (v * v - AROUND * AROUND) / (2f * EASING);
            if (need < City.CELL - INTO - s - 1f) committed = true;
        }
        plan(committed ? turn : 0);
        // past the crossing: on along the way out, the turn done with
        float tx = cx + di * City.CELL, tz = cz + dj * City.CELL;
        if ((p.x - tx) * ni + (p.z - tz) * nj > INTO - 0.5f) {
            ci += di;
            cj += dj;
            boolean turned = ni != di || nj != dj;
            di = ni;
            dj = nj;
            // the turn done with; one said too late is still to come, at this next crossing
            if (committed || turned) turn = 0;
            committed = false;
            side = 1f;
            passing = -1;
            plan(0);
            cx = ci * City.CELL;
            cz = cj * City.CELL;
            s = (p.x - cx) * di + (p.z - cz) * dj;
        }
        check(p.x, p.z);
        if (!onRoad) {
            out[0] = handWheel;
            out[1] = go ? OFF_ROAD_TOP : 0f;
            return;
        }
        // overtaking: a slower car in its lane close ahead, and the other lane clear far enough on
        overtake(p, cars, s);
        // the wheel: toward a point on the way so far ahead, the farther the faster
        float look = 5f + v * 0.45f;
        ahead(s, look, p.x, p.z);
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading);
        float gx = at[0] - p.x, gz = at[1] - p.z;
        float alpha = (float) Math.atan2(gx * fz - gz * fx, gx * fx + gz * fz);
        float bend = 2f * (float) Math.sin(alpha) / look;
        float can = Math.min(Math.max(v, 1.2f) / Pickup.TIGHTEST, Pickup.YAW_MID + (Pickup.YAW_TOP - Pickup.YAW_MID) * Math.min(1f, v / Pickup.VMAX));
        out[0] = Math.max(-1f, Math.min(1f, Math.max(v, 1.2f) * bend / Math.max(1e-3f, can)));
        // the speed: the top on a straight; slower in time for a corner ahead; behind a car it cannot pass
        float want = go ? TOP : 0f;
        boolean corner = ni != di || nj != dj;
        float toCorner = City.CELL - INTO - s;
        if (corner) want = Math.min(want, (float) Math.sqrt(AROUND * AROUND + 2f * SLOWING * Math.max(0f, toCorner)));
        if (go && passing < 0) want = Math.min(want, behind(p, cars, fx, fz));
        out[1] = want;
    }

    /** How fast it may go behind the nearest car ahead in its lane (it keeps back from it). */
    private float behind(Pickup p, Cars cars, float fx, float fz) {
        float t = TOP;
        for (int c = 0; c < Cars.MOST; c++) {
            if (!cars.live(c)) continue;
            float ox = cars.x[c] - p.x, oz = cars.z[c] - p.z;
            float along = ox * fx + oz * fz, across = Math.abs(ox * fz - oz * fx);
            if (along < 0f || along > 40f || across > 1.9f) continue;
            t = Math.min(t, Math.max(0f, cars.speed[c]) + Math.max(0f, along - 7f) * 1.2f);
        }
        return t;
    }

    /** Out into the other lane past a slower car close ahead when the other lane is clear; back in once past it. */
    private void overtake(Pickup p, Cars cars, float s) {
        float fx = di, fz = dj;
        if (passing >= 0) {
            float ox = cars.x[passing] - p.x, oz = cars.z[passing] - p.z;
            if (!cars.live(passing) || ox * fx + oz * fz < -9f) {
                side = 1f;
                passing = -1;
            }
            return;
        }
        // never into a crossing on the wrong side
        if (City.CELL - INTO - s < 30f) return;
        int slow = -1;
        for (int c = 0; c < Cars.MOST; c++) {
            if (!cars.live(c)) continue;
            float ox = cars.x[c] - p.x, oz = cars.z[c] - p.z;
            float along = ox * fx + oz * fz, across = ox * fz - oz * fx;
            if (along > 4f && along < 22f && Math.abs(across - LANE * 0f) < 2.2f && cars.speed[c] < p.speed - 1f
                    && (float) Math.cos(cars.heading[c] - p.heading) > 0.7f) slow = c;
        }
        if (slow < 0) return;
        // the other lane clear: nothing coming within so far
        for (int c = 0; c < Cars.MOST; c++) {
            if (!cars.live(c) || c == slow) continue;
            float ox = cars.x[c] - p.x, oz = cars.z[c] - p.z;
            float along = ox * fx + oz * fz, across = ox * fz - oz * fx;
            if (along > -6f && along < 60f && across < -0.3f && across > -4f) return;
        }
        side = -1f;
        passing = slow;
    }
}
