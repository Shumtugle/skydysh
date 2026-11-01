package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The town's traffic: cars of a few kinds (sedans, hatchbacks, little town cars, taxis in black
 * and yellow, vans) on the right of the road, going on or turning at the
 * crossings as it falls out, never turning back; slowing for a turn, for a car ahead and for
 * the runner in the road before them. Not for a brum on the run: they run into it, stop short
 * a while, and wait while it lies there or stands in the road. They come from out of sight round the runner and go out of
 * it behind her, and are put back somewhere else round her.
 *
 * Also the cars themselves, for any car anywhere: the parked ones along the curbs are built with
 * the town (see City).
 */
final class Cars {
    static final int[] PAINT = {0xE9B63A, 0xB5352C, 0xE9E1CB, 0x2D3A57, 0x6E9DB0, 0x9DBB8E, 0x3A3B40, 0xF4F2EC};
    static final float LANE = 1.5f;                 // right of the road's middle, clear of the parked
    static final float CRUISE = 10.5f, TURNING = 5.8f, GAP = 12f;
    static final float BORN_NEAR = 45f, BORN_FAR = 130f, GONE = 170f;
    static final int MOST = 18;
    static final float TOP = 1.45f;                 // how high a car's roof is (a sedan's)
    // the kinds of car
    static final int SEDAN = 0, HATCH = 1, SMALL = 2, TAXI = 3, VAN = 4;
    static final int TAXI_BLACK = 0x1E1E22, TAXI_YELLOW = 0xF2C230;
    static final int[] VAN_PAINT = {0xF1EFE8, 0xD9DCDD, 0x6E9DB0, 0x2D3A57, 0xC9B79A};
    static final int[] SMALL_PAINT = {0x9DD1B8, 0xF0E2B6, 0xD9473A, 0x8FB6DA, 0xF4F2EC, 0xE9B63A};

    /** A kind of car, as they come in the town: sedans and hatchbacks most, then little ones, taxis, vans. */
    static int kind(Random rnd) {
        int r = rnd.nextInt(20);
        return r < 7 ? SEDAN : r < 12 ? HATCH : r < 15 ? SMALL : r < 18 ? TAXI : VAN;
    }

    /** A colour for a car of a kind (a taxi's is its own). */
    static int paint(Random rnd, int kind) {
        if (kind == TAXI) return TAXI_BLACK;
        if (kind == VAN) return VAN_PAINT[rnd.nextInt(VAN_PAINT.length)];
        if (kind == SMALL) return SMALL_PAINT[rnd.nextInt(SMALL_PAINT.length)];
        return PAINT[rnd.nextInt(PAINT.length)];
    }

    /** How high the roof of a car of a kind is. */
    static float top(int kind) {
        return kind == VAN ? 2.1f : kind == SMALL ? 1.5f : TOP;
    }

    /** Half the length of a car of a kind. */
    static float half(int kind) {
        return kind == VAN ? 2.4f : kind == SMALL ? 1.65f : kind == HATCH ? 1.95f : 2.2f;
    }

    private final Random rnd = new Random(0xCA25L);
    final float[] x = new float[MOST], z = new float[MOST], heading = new float[MOST], speed = new float[MOST];
    private final int[] ti = new int[MOST], tj = new int[MOST], di = new int[MOST], dj = new int[MOST];
    private final int[] ni = new int[MOST], nj = new int[MOST], paint = new int[MOST], kind = new int[MOST];
    private final boolean[] live = new boolean[MOST];
    private final float[] halt = new float[MOST];       // stopped short after striking her, seconds left
    // how long it has stood held up by other cars; how long more it edges out past them
    private final float[] waited = new float[MOST], edging = new float[MOST];

    void update(float dt, Runner r, Chase[] brums) {
        for (int c = 0; c < MOST; c++) {
            float rx = x[c] - r.x, rz = z[c] - r.z;
            if (!live[c] || rx * rx + rz * rz > GONE * GONE) {
                spawn(c, r);
                continue;
            }
            // toward the next crossing, on the right of the road
            float tx = ti[c] * City.CELL + dj[c] * LANE, tz = tj[c] * City.CELL - di[c] * LANE;
            float ex = tx - x[c], ez = tz - z[c], left = (float) Math.sqrt(ex * ex + ez * ez);
            if (left < 2.5f) {
                di[c] = ni[c];
                dj[c] = nj[c];
                ti[c] += di[c];
                tj[c] += dj[c];
                choose(c);
                tx = ti[c] * City.CELL + dj[c] * LANE;
                tz = tj[c] * City.CELL - di[c] * LANE;
                ex = tx - x[c];
                ez = tz - z[c];
                left = (float) Math.sqrt(ex * ex + ez * ez);
            }
            boolean turns = ni[c] != di[c] || nj[c] != dj[c];
            float want = turns && left < 14f ? TURNING : CRUISE;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            // someone in the road ahead: stop short of them (the runner, a brum down, a pickup left)
            float others = clear(c, fx, fz, r.x, r.z);
            for (Chase g : brums) if (g.down()) others = Math.min(others, clear(c, fx, fz, g.x, g.z));
            if (duty != null) for (int k = 0; k < duty.all.size(); k++) {
                Pickup p = duty.all.get(k);
                if (!p.driven) others = Math.min(others, clear(c, fx, fz, p.x, p.z));
            }
            // a car ahead going the same way, or across it: keep back from it; not one coming the
            // other way, nor any while it is edging out of a knot of them
            float byCars = CRUISE;
            for (int o = 0; o < MOST; o++) {
                if (o == c || !live[o]) continue;
                float ox = x[o] - x[c], oz = z[o] - z[c];
                float along = ox * fx + oz * fz, across = Math.abs(ox * fz - oz * fx);
                if (along <= 0f || along >= GAP || across >= 2.2f) continue;
                if ((float) Math.cos(heading[o] - heading[c]) < -0.3f) continue;
                byCars = Math.min(byCars, Math.max(0f, (along - 6f) * 1.2f));
            }
            // held up by the other cars alone a good while (a knot at a crossing, one stopped
            // after a knock): it edges out past them, never minding them a moment
            edging[c] -= dt;
            if (byCars < 0.5f && others > 1f && speed[c] < 0.5f && halt[c] <= 0f) {
                if ((waited[c] += dt) > 2.5f) {
                    edging[c] = 2f;
                    waited[c] = 0f;
                }
            } else waited[c] = 0f;
            if (edging[c] > 0f) byCars = Math.max(byCars, TURNING * 0.6f);
            want = Math.min(want, Math.min(byCars, others));
            if (halt[c] > 0f) {
                halt[c] -= dt;
                want = 0f;
            }
            float rate = want < speed[c] ? 9f : 2.5f;
            speed[c] += Math.max(-rate * dt, Math.min(rate * dt, want - speed[c]));
            float aim = (float) Math.atan2(ex, ez), d = aim - heading[c];
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            float most = 2.4f * dt * Math.max(0.3f, speed[c] / CRUISE);
            heading[c] += Math.max(-most, Math.min(most, d));
            x[c] += (float) Math.sin(heading[c]) * speed[c] * dt;
            z[c] += (float) Math.cos(heading[c]) * speed[c] * dt;
        }
    }

    /** How fast car c may go with someone at (px, pz): no faster than it can stop short of them. */
    private float clear(int c, float fx, float fz, float px, float pz) {
        float ox = px - x[c], oz = pz - z[c];
        float along = ox * fx + oz * fz, across = Math.abs(ox * fz - oz * fx);
        if (along < 0f || along > 16f || across > 2.4f) return CRUISE;
        return Math.max(0f, (along - 4f) * 1.1f);
    }

    /** A car put on a street round the runner, out of her sight, going one way or the other. */
    private void spawn(int c, Runner r) {
        for (int tries = 0; tries < 20; tries++) {
            int ci = City.index(r.x) + rnd.nextInt(5) - 2, cj = City.index(r.z) + rnd.nextInt(5) - 2;
            int dir = rnd.nextInt(4);
            if (!City.road(cj, dir == 2 ? 1 : dir == 3 ? -1 : 0)) continue;
            int a = dir == 0 ? 1 : dir == 1 ? -1 : 0, b = dir == 2 ? 1 : dir == 3 ? -1 : 0;
            float half = City.CELL * 0.5f;
            float px = ci * City.CELL + a * half + b * LANE, pz = cj * City.CELL + b * half - a * LANE;
            float dx = px - r.x, dz = pz - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < BORN_NEAR || dd > BORN_FAR) continue;
            // not right before her eyes
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if ((dx * fx + dz * fz) / dd > 0.6f && dd < 90f) continue;
            x[c] = px;
            z[c] = pz;
            di[c] = a;
            dj[c] = b;
            ti[c] = ci + a;
            tj[c] = cj + b;
            heading[c] = (float) Math.atan2(a, b);
            speed[c] = CRUISE;
            halt[c] = 0f;
            waited[c] = edging[c] = 0f;
            kind[c] = kind(rnd);
            paint[c] = paint(rnd, kind[c]);
            choose(c);
            live[c] = true;
            return;
        }
        live[c] = false;
    }

    /** For the desktop stand: car 0 put behind her in her lane, so far back, coming on at speed. */
    void standBehind(Chase ch, float back) {
        int a = Math.round((float) Math.sin(ch.heading)), b = Math.round((float) Math.cos(ch.heading));
        if (a != 0) b = 0;
        float px = ch.x - a * back, pz = ch.z - b * back;
        int ri = Math.round(ch.x / City.CELL), rj = Math.round(ch.z / City.CELL);
        // on the street she is on, in its right lane, to the next crossing that way
        if (a != 0) {
            pz = rj * City.CELL - a * LANE;
            ti[0] = City.index(px) + (a > 0 ? 1 : 0);
            tj[0] = rj;
        } else {
            px = ri * City.CELL + b * LANE;
            ti[0] = ri;
            tj[0] = City.index(pz) + (b > 0 ? 1 : 0);
        }
        x[0] = px;
        z[0] = pz;
        di[0] = a;
        dj[0] = b;
        ni[0] = a;
        nj[0] = b;
        heading[0] = (float) Math.atan2(a, b);
        speed[0] = CRUISE;
        halt[0] = 0f;
        paint[0] = PAINT[1];
        kind[0] = SEDAN;
        live[0] = true;
    }

    /** For the desktop stand: car 0 standing across her way, so far ahead of her, stopped. */
    void standAcross(Runner r, float ahead) {
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        x[0] = r.x + fx * ahead;
        z[0] = r.z + fz * ahead;
        heading[0] = r.heading + (float) Math.PI * 0.5f;
        speed[0] = 0f;
        halt[0] = 1000f;
        paint[0] = PAINT[1];
        kind[0] = SEDAN;
        live[0] = true;
    }

    /** Car c put standing at (x, z) facing so, of a kind and paint, held there (the stand's show). */
    void stand(int c, float px, float pz, float yaw, int k, int p) {
        x[c] = px;
        z[c] = pz;
        heading[c] = yaw;
        speed[c] = 0f;
        halt[c] = 1000f;
        paint[c] = p;
        kind[c] = k;
        live[c] = true;
    }

    /** What car c does at the crossing after the one it is going to: on, mostly; left or right now and then. */
    private void choose(int c) {
        int roll = rnd.nextInt(5);
        ni[c] = di[c];
        nj[c] = dj[c];
        if (roll == 3) {
            ni[c] = -dj[c];
            nj[c] = di[c];
        } else if (roll == 4) {
            ni[c] = dj[c];
            nj[c] = -di[c];
        }
        // no street out to sea from the shore road: straight on, or a turn that is a street
        if (!City.road(tj[c], nj[c])) {
            int[][] ways = {{di[c], dj[c]}, {-dj[c], di[c]}, {dj[c], -di[c]}, {-di[c], -dj[c]}};
            for (int[] w : ways)
                if (City.road(tj[c], w[1])) {
                    ni[c] = w[0];
                    nj[c] = w[1];
                    break;
                }
        }
    }

    /**
     * The car, if any, running into a round body of radius rr at (px, pz): touching it at its
     * nose or its side, and coming at it fast enough to knock it down; -1 if none.
     */
    int striking(float px, float pz, float rr) {
        for (int c = 0; c < MOST; c++) {
            if (!live[c] || speed[c] < 2.5f) continue;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            for (int e = -1; e <= 1; e += 2) {
                float cx = x[c] + fx * e * 1.2f, cz = z[c] + fz * e * 1.2f;
                float dx = px - cx, dz = pz - cz, m = rr + 1.15f;
                if (dx * dx + dz * dz >= m * m) continue;
                // coming at her: she is ahead of the car's middle, not behind it
                if ((px - x[c]) * fx + (pz - z[c]) * fz > 0.6f) return c;
            }
        }
        return -1;
    }

    /** Car c has struck someone: it jolts to a stop and stands a moment. */
    void struck(int c) {
        speed[c] *= 0.25f;
        halt[c] = 1.4f;
    }

    /**
     * Pushes a round body of radius rr at (p[0], p[1]) out of any car it went into: each car two
     * round ends along its length. True if it touched one.
     */
    boolean push(float[] p, float rr) {
        return push(p, rr, 0f);
    }

    /** The duty pickups, pushed against as the traffic is (see Duty); none, none. */
    Duty duty;

    boolean live(int c) {
        return live[c];
    }

    /** The same, for one whose feet are this high off the road: over a car's roof is clear of it. */
    boolean push(float[] p, float rr, float lift) {
        return push(p, rr, lift, null);
    }

    /** The same, all but one pickup (the one that is pushing). */
    boolean push(float[] p, float rr, float lift, Pickup but) {
        boolean hit = false;
        if (duty != null && lift < Pickup.TOP - 0.1f) hit = duty.push(p, rr, but);
        if (lift > TOP - 0.1f) return hit;
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            for (int e = -1; e <= 1; e += 2) {
                float cx = x[c] + fx * e * 1.2f, cz = z[c] + fz * e * 1.2f;
                float dx = p[0] - cx, dz = p[1] - cz, dd = dx * dx + dz * dz, m = rr + 1.05f;
                if (dd >= m * m || dd < 1e-8f) continue;
                float l = (float) Math.sqrt(dd);
                p[0] = cx + dx / l * m;
                p[1] = cz + dz / l * m;
                hit = true;
            }
        }
        return hit;
    }

    void draw(DrawList d, float ex, float ez, float fx, float fz) {
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float dx = x[c] - ex, dz = z[c] - ez;
            if (dx * fx + dz * fz < -6f) continue;
            car(d, x[c], z[c], heading[c], paint[c], false, kind[c]);
        }
    }

    // ---- the cars ----

    private static float px, py, pz;
    private static final float[] frame = new float[9], t = new float[9];
    // the car being drawn: its kind for the skin, and its paint where the skin is paint
    private static int skinKind, skinPaint;
    /** Which skin a pickup wears (the skins of the five kinds come first). */
    static final int PICKUP_SKIN = 5;

    /** A sedan standing on the road (see the other). */
    static void car(DrawList d, float x, float z, float yaw, int paint, boolean lean) {
        car(d, x, z, yaw, paint, lean, SEDAN);
    }

    /**
     * A car of a kind standing on the road, its nose along yaw (0 along +z); lean, without its
     * small things (a parked one: there are many of them).
     */
    static void car(DrawList d, float x, float z, float yaw, int paint, boolean lean, int kind) {
        float cy = (float) Math.cos(yaw), sy = (float) Math.sin(yaw);
        px = x;
        py = 0f;
        pz = z;
        frame[0] = cy; frame[1] = 0f; frame[2] = sy;
        frame[3] = 0f; frame[4] = 1f; frame[5] = 0f;
        frame[6] = -sy; frame[7] = 0f; frame[8] = cy;
        skinKind = kind;
        skinPaint = paint;
        if (kind == HATCH) hatch(d, paint, lean);
        else if (kind == SMALL) small(d, paint, lean);
        else if (kind == VAN) van(d, paint, lean);
        else {
            sedan(d, paint, lean);
            if (kind == TAXI) {
                // the doors yellow, a green lamp on the roof
                for (int side = -1; side <= 1; side += 2)
                    part(d, side * 0.905f, 0.6f, -0.05f, 0.01f, 0.2f, 0.95f, 0f, TAXI_YELLOW, DrawList.PLAIN);
                part(d, 0f, 1.39f, -0.25f, 0.2f, 0.065f, 0.09f, 0f, 0x7CF29A, DrawList.GLOW);
            }
        }
    }

    /** What every car has: four wheels (hubs too, unless lean), lamps at both ends, bumpers. */
    private static void ends(DrawList d, float half, float wheelAt, float w, float wheel, float lampY, int paint, boolean lean) {
        float plain = DrawList.PLAIN;
        for (int side = -1; side <= 1; side += 2) {
            for (int end = -1; end <= 1; end += 2) {
                wheel(d, side * (w - 0.05f), wheel, end * wheelAt, 0.12f, wheel);
            }
            part(d, side * (w - 0.3f), lampY, half + 0.005f, 0.15f, 0.07f, 0.01f, 0f, 0xFFF0C2, plain);
            part(d, side * (w - 0.26f), lampY + 0.02f, -half - 0.005f, 0.15f, 0.07f, 0.01f, 0f, 0xD43A2E, plain);
        }
        part(d, 0f, 0.42f, half + 0.04f, w + 0.03f, 0.07f, 0.05f, 0f, 0xD2CEC6, plain);
        part(d, 0f, 0.42f, -half - 0.04f, w + 0.03f, 0.07f, 0.05f, 0f, 0xD2CEC6, plain);
    }

    /** A hatchback: shorter, its roof running on to a steep back of glass. */
    private static void hatch(DrawList d, int paint, boolean lean) {
        float plain = DrawList.PLAIN, glass = DrawList.GLASS;
        int clear = 0x33414E;
        part(d, 0f, 0.6f, 0f, 0.88f, 0.25f, 1.95f, 0f, paint, plain);
        part(d, 0f, 0.36f, 0f, 0.84f, 0.05f, 1.55f, 0f, 0x2A2829, plain);
        float top = 1.33f, mid = (0.85f + top) * 0.5f;
        part(d, 0f, mid, -0.7f, 0.77f, 0.24f, 1.0f, 0f, clear, glass);
        part(d, 0f, mid, 0.6f, 0.77f, 0.37f, 0.01f, -0.9f, clear, glass);
        part(d, 0f, mid, -1.72f, 0.77f, 0.25f, 0.01f, 0.25f, clear, glass);
        part(d, 0f, top + 0.01f, -0.7f, 0.75f, 0.025f, 1.02f, 0f, paint, plain);
        if (!lean) for (int side = -1; side <= 1; side += 2)
            part(d, side * 0.775f, mid, -0.35f, 0.01f, 0.23f, 0.06f, 0f, paint, plain);
        ends(d, 1.95f, 1.22f, 0.88f, 0.31f, 0.7f, paint, lean);
    }

    /** A little town car: short, high, round-shouldered, two doors. */
    private static void small(DrawList d, int paint, boolean lean) {
        float plain = DrawList.PLAIN, glass = DrawList.GLASS;
        int clear = 0x33414E;
        part(d, 0f, 0.56f, 0f, 0.8f, 0.24f, 1.65f, 0f, paint, plain);
        part(d, 0f, 0.8f, 0.95f, 0.72f, 0.06f, 0.55f, 0f, paint, plain);
        part(d, 0f, 0.34f, 0f, 0.76f, 0.05f, 1.2f, 0f, 0x2A2829, plain);
        float top = 1.46f, mid = (0.8f + top) * 0.5f;
        part(d, 0f, mid, -0.45f, 0.72f, 0.33f, 0.82f, 0f, clear, glass);
        part(d, 0f, mid + 0.02f, 0.48f, 0.72f, 0.36f, 0.01f, -0.55f, clear, glass);
        part(d, 0f, top + 0.01f, -0.45f, 0.7f, 0.03f, 0.84f, 0f, Street.scale(paint, 1.06f), plain);
        if (!lean) part(d, 0f, top + 0.05f, -0.45f, 0.5f, 0.02f, 0.6f, 0f, 0xF4F2EC, plain);
        ends(d, 1.65f, 1.05f, 0.8f, 0.28f, 0.66f, paint, lean);
    }

    /** A van: a tall box behind a short bonnet, the cab glazed, sliding door, the back shut. */
    private static void van(DrawList d, int paint, boolean lean) {
        float plain = DrawList.PLAIN, glass = DrawList.GLASS;
        int clear = 0x33414E;
        part(d, 0f, 0.62f, 0f, 0.95f, 0.3f, 2.4f, 0f, paint, plain);
        part(d, 0f, 0.36f, 0f, 0.9f, 0.05f, 2.0f, 0f, 0x2A2829, plain);
        part(d, 0f, 1.47f, -0.42f, 0.95f, 0.58f, 1.98f, 0f, paint, plain);
        part(d, 0f, 1.37f, 1.58f, 0.9f, 0.42f, 0.01f, -0.45f, clear, glass);
        for (int side = -1; side <= 1; side += 2) {
            part(d, side * 0.955f, 1.4f, 1.05f, 0.01f, 0.27f, 0.42f, 0f, clear, glass);
            if (!lean) part(d, side * 0.955f, 1.25f, 0.45f, 0.01f, 0.55f, 0.012f, 0f, Street.scale(paint, 0.7f), plain);
        }
        part(d, 0f, 1.25f, -2.405f, 0.012f, 0.55f, 0.01f, 0f, Street.scale(paint, 0.7f), plain);
        ends(d, 2.4f, 1.6f, 0.95f, 0.34f, 0.78f, paint, lean);
    }

    /** A small sedan, its glass sloping front and back. */
    private static void sedan(DrawList d, int paint, boolean lean) {
        float plain = DrawList.PLAIN, glass = DrawList.GLASS;
        int clear = 0x33414E;
        // body, with a darker sill
        part(d, 0f, 0.6f, 0f, 0.9f, 0.25f, 2.2f, 0f, paint, plain);
        part(d, 0f, 0.36f, 0f, 0.86f, 0.05f, 1.75f, 0f, 0x2A2829, plain);
        // cabin: glass all round, the screens sloping, a roof, the pillars between the doors
        float top = 1.3f, mid = (0.85f + top) * 0.5f;
        part(d, 0f, mid, -0.275f, 0.79f, 0.225f, 0.675f, 0f, clear, glass);
        part(d, 0f, mid, 0.7f, 0.79f, 0.375f, 0.01f, -0.927f, clear, glass);
        part(d, 0f, mid, -1.1f, 0.79f, 0.27f, 0.01f, 0.588f, clear, glass);
        part(d, 0f, top + 0.01f, -0.275f, 0.77f, 0.025f, 0.7f, 0f, paint, plain);
        for (int side = -1; side <= 1; side += 2) {
            if (!lean) part(d, side * 0.795f, mid, -0.25f, 0.01f, 0.22f, 0.07f, 0f, paint, plain);
            for (int end = -1; end <= 1; end += 2) {
                wheel(d, side * 0.85f, 0.32f, end * 1.38f, 0.12f, 0.32f);
            }
            part(d, side * 0.6f, 0.68f, 2.205f, 0.17f, 0.07f, 0.01f, 0f, 0xFFF0C2, plain);
            part(d, side * 0.64f, 0.7f, -2.205f, 0.17f, 0.07f, 0.01f, 0f, 0xD43A2E, plain);
        }
        // bumpers, grille, plates
        part(d, 0f, 0.42f, 2.24f, 0.93f, 0.07f, 0.05f, 0f, 0xD2CEC6, plain);
        part(d, 0f, 0.42f, -2.24f, 0.93f, 0.07f, 0.05f, 0f, 0xD2CEC6, plain);
        if (lean) return;
        part(d, 0f, 0.64f, 2.205f, 0.3f, 0.08f, 0.01f, 0f, 0x2A2A2C, plain);
        part(d, 0f, 0.43f, 2.3f, 0.24f, 0.06f, 0.01f, 0f, 0xF2F0E6, plain);
        part(d, 0f, 0.43f, -2.3f, 0.24f, 0.06f, 0.01f, 0f, 0xF2F0E6, plain);
    }

    /** A wheel at (lx, ly, lz) in the car's frame: round about the axle, so wide, of radius r. */
    private static void wheel(DrawList d, float lx, float ly, float lz, float hx, float r) {
        wheel(d, px, py, pz, frame, lx, ly, lz, hx, r);
    }

    /** A wheel of a car standing at (x, y, z) and turned by f (row by row). */
    static void wheel(DrawList d, float x, float y, float z, float[] f, float lx, float ly, float lz, float hx, float r) {
        d.ellipsoid(x + f[0] * lx + f[1] * ly + f[2] * lz, y + f[3] * lx + f[4] * ly + f[5] * lz,
                z + f[6] * lx + f[7] * ly + f[8] * lz, hx, r, r, f, 0x1C1B1D, DrawList.TYRE);
    }

    /**
     * Whether a box of a car wears the painted skin: all of it but what glows and what is set
     * at a slant (the sloping glass keeps its own shine).
     */
    static boolean skinned(float tilt, float material) {
        return tilt == 0f && material != DrawList.GLOW;
    }

    /** A box of the car in its own frame: tilted about its own x first (sloping glass), then turned with it. */
    private static void part(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, float tilt,
                             int color, float material) {
        float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        float[] f = frame;
        for (int r = 0; r < 3; r++) {
            t[r * 3] = f[r * 3];
            t[r * 3 + 1] = f[r * 3 + 1] * ct + f[r * 3 + 2] * st;
            t[r * 3 + 2] = -f[r * 3 + 1] * st + f[r * 3 + 2] * ct;
        }
        d.box(px + f[0] * lx + f[1] * ly + f[2] * lz, py + f[3] * lx + f[4] * ly + f[5] * lz,
                pz + f[6] * lx + f[7] * ly + f[8] * lz, hx, hy, hz, t, color, material);
        if (skinned(tilt, material)) d.skin(skinKind, lx, ly, lz, skinKind == TAXI ? color : skinPaint);
    }
}
