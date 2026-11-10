package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The town's traffic: cars of a few kinds (sedans, hatchbacks, little town cars, taxis in black
 * and yellow, vans) on the right of the road, going on or turning at the
 * crossings as it falls out; slowing for a turn, for a car ahead, for the runner in the road
 * before them; stopping at a red light and for people on a zebra. Something standing in the way
 * (a wreck, a pickup, someone in the road) they wait for a moment, then go round by the other lane
 * when it is clear. A pickup on the move coming at them, they get out of its way, aside and faster
 * or braking. Rammed, a car slides to a wreck, and a while later comes to and drives off. Not for a
 * brum on the run: they run into it, stop short a while, and wait while it lies there or stands in
 * the road. They come from out of sight round the runner and go out of it behind her, and are put
 * back somewhere else round her.
 *
 * Also the cars themselves, for any car anywhere: the parked ones along the curbs are built with
 * the town (see City).
 */
final class Cars {
    static final int[] PAINT = {0xE9B63A, 0xB5352C, 0xE9E1CB, 0x2D3A57, 0x6E9DB0, 0x9DBB8E, 0x3A3B40, 0xF4F2EC};
    static final float LANE = 2f;                   // right of the road's middle, clear of the parked
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
    /** How high the roof of the car standing over (px, pz) is, if one is; 0 if none. */
    float topAt(float px, float pz) {
        float best = 0f;
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            float ox = px - x[c], oz = pz - z[c];
            if (Math.abs(ox * fx + oz * fz) < half(kind[c]) + 0.2f && Math.abs(ox * fz - oz * fx) < 1.15f)
                best = Math.max(best, top(kind[c]));
        }
        return best;
    }

    /** The car whose roof is over (px, pz), however high she is: its number, or -1 for none. */
    int over(float px, float pz) {
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            float ox = px - x[c], oz = pz - z[c];
            if (Math.abs(ox * fx + oz * fz) < half(kind[c]) + 0.2f && Math.abs(ox * fz - oz * fx) < 1.15f) return c;
        }
        return -1;
    }

    /** For the desktop stand: the nearest car going along to (px, pz), at least so fast; -1 for none. */
    int nearestGoing(float px, float pz, float fast) {
        int best = -1;
        float bd = Float.MAX_VALUE;
        for (int c = 0; c < MOST; c++) {
            if (!live[c] || speed[c] < fast) continue;
            float dx = x[c] - px, dz = z[c] - pz, dd = dx * dx + dz * dz;
            if (dd < bd) {
                bd = dd;
                best = c;
            }
        }
        return best;
    }

    /** How high car c's roof is. */
    float roofOf(int c) {
        return top(kind[c]);
    }

    /** How near the roof's height her feet must be to stand on it, meters. */
    static final float RIDE_SLACK = 0.15f;
    /** Feet so high off the road are over the cars: no car stops for her up there. */
    static final float OVER_ROAD = 1f;

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
    // frightened by the mallet: off down the road fast for so long, not waiting for anyone farther off than a few meters
    private final float[] spooked = new float[MOST];

    /** Every car within reach of (ax, az) frightened off: fast away down its road. How many. */
    int alarm(float ax, float az, float reach) {
        int n = 0;
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float dx = x[c] - ax, dz = z[c] - az;
            if (dx * dx + dz * dz < reach * reach) {
                if (spooked[c] <= 0f) n++;
                spooked[c] = 5f;
                halt[c] = 0f;
            }
        }
        return n;
    }
    // how long it has stood held up by other cars; how long more it edges out past them
    private final float[] waited = new float[MOST], edging = new float[MOST];
    // rammed by the pickup she drives: sliding away so fast (x, z), turning so fast, for so long
    // more; then a wreck, standing where it came to rest till it is out of sight
    private final float[] kvx = new float[MOST], kvz = new float[MOST], spin = new float[MOST], knocked = new float[MOST];
    private final boolean[] wreck = new boolean[MOST];
    /** The town, for a rammed car to slide against its walls; none, none. */
    City city;
    /** How many times a car has been rammed, and where the last was: the world hears of it. */
    int rams;
    float ramX, ramZ, ramHard;
    static final float SLIDE_DRAG = 2.2f, SPIN_DRAG = 2.4f, KNOCKED_FOR = 2.6f;

    // why a car stands or crawls: nothing holds it; it waits its turn (a light, people on a zebra,
    // the cars before it waiting); something stands in its way (a wreck, a pickup, someone in the
    // road); it is caught in a knot of cars at a crossing
    static final int FREE = 0, WAIT = 1, BLOCK = 2, KNOT = 3;
    // held up by something in the way so long, it goes round it by the other lane when that is clear
    // so far ahead; a little faster than a crawl while it pulls out; given up after so long
    static final float PASS_AFTER = 0.9f, PASS_CLEAR = 34f, PASS_CREEP = 3.2f, PASS_MOST = 9f, PASS_ANYWAY = 5f;
    // how far a car may be off its lane: into the other lane, short of the cars parked on its right
    static final float OFF_LEFT = -LANE - City.ROAD + 1.1f, OFF_RIGHT = 1f, OFF_RATE = 4.5f;
    // a pickup coming at it: looked for so far ahead in time, as near as this it would pass; out
    // of its way so far aside, for so long; faster, when it comes from behind
    static final float DODGE_LOOK = 1.9f, DODGE_NEAR = 3.4f, DODGE_FOR = 0.75f, DODGE_FAST = 1.7f;
    // met head on it keeps rolling so fast to steer aside; and it jinks aside so fast at most (meters a second)
    static final float DODGE_ROLL = 6f, DODGE_JINK = 4.5f;
    // how far aside of a pickup's way a car must be to be clear of it, middle to middle
    static final float DODGE_CLEAR = 2.6f;
    // a wreck comes to after a while and drives off, if no one stands by it
    static final float COME_TO = 4.5f, COME_TO_MORE = 3.5f;

    private final int[] why = new int[MOST];
    // how long it has been held up by something in the way; going round it (seconds left), and what it goes round
    private final float[] blocked = new float[MOST], passing = new float[MOST], passX = new float[MOST], passZ = new float[MOST];
    // how far off its lane it keeps (to its right +), and where it is going to keep
    private final float[] off = new float[MOST];
    // getting out of a pickup's way: for so long more, so far off its lane, faster (+1) or braking (-1)
    private final float[] dodging = new float[MOST], dodgeOff = new float[MOST], dodgeGas = new float[MOST];
    // a wreck: how long it has stood, and how long it stands before it comes to
    private final float[] stood = new float[MOST], comeTo = new float[MOST];

    /** The pickup she drives, if she does: the cars keep out of its way as of any pickup on the move. */
    Pickup driven;
    /** The people about, for the cars to stop for at a zebra and in the road; none, none. */
    Walkers walkers;
    private final Pickup[] threats = new Pickup[8];
    private int threatN;

    /** Why car c stands or crawls (FREE, WAIT, BLOCK, KNOT). */
    int why(int c) {
        return why[c];
    }

    /** Whether car c is going round something in its way. */
    boolean passing(int c) {
        return passing[c] > 0f;
    }

    /** Whether car c is getting out of a pickup's way. */
    boolean dodging(int c) {
        return dodging[c] > 0f;
    }

    void update(float dt, Runner r, Chase[] brums) {
        Crossings.time += dt;
        // the pickups on the move about the town
        threatN = 0;
        if (driven != null && Math.abs(driven.speed) > 2f) threats[threatN++] = driven;
        if (duty != null) for (int k = 0; k < duty.all.size() && threatN < threats.length; k++) {
            Pickup p = duty.all.get(k);
            if (p != driven && Math.abs(p.speed) > 2f) threats[threatN++] = p;
        }
        for (int c = 0; c < MOST; c++) {
            float rx = x[c] - r.x, rz = z[c] - r.z;
            if (!live[c] || rx * rx + rz * rz > GONE * GONE) {
                spawn(c, r);
                continue;
            }
            if (knocked[c] > 0f || wreck[c]) {
                slide(c, dt);
                if (wreck[c] && live[c]) {
                    stood[c] += dt;
                    // come to and off, when no one is by it
                    if (stood[c] > comeTo[c] && rx * rx + rz * rz > 16f && !touched(c)) comeTo(c);
                }
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
            int whyNow = FREE;
            // what holds it in its way: its nearest, and where it is
            float hold = CRUISE, holdX = 0f, holdZ = 0f;
            // a light at the crossing ahead: red, or amber while it can still stop, it stops before the zebra
            if (Crossings.lit(ti[c], tj[c])) {
                int light = Crossings.light(ti[c], tj[c], di[c] != 0);
                float before = left - Crossings.STOP;
                if (before > -0.6f && (light == Crossings.HALT || light == Crossings.WARN && before > speed[c] * speed[c] / 14f)) {
                    float lim = Math.max(0f, before * 1.1f);
                    if (lim < want) {
                        want = lim;
                        whyNow = WAIT;
                    }
                }
            }
            // people on a zebra ahead, or waiting at one with no light: it stops before it, if it can
            if (walkers != null) {
                float lim = walkers.zebraAhead(x[c], z[c], fx, fz, speed[c]);
                if (lim < want) {
                    want = lim;
                    whyNow = WAIT;
                }
            }
            // someone in the road ahead: stop short of them (the runner, a brum down, a pickup left)
            float others = r.y > OVER_ROAD ? CRUISE : clear(c, fx, fz, r.x, r.z);
            if (others < hold) {
                hold = others;
                holdX = r.x;
                holdZ = r.z;
            }
            for (Chase g : brums)
                if (g.down()) {
                    float o = clear(c, fx, fz, g.x, g.z);
                    if (o < hold) {
                        hold = o;
                        holdX = g.x;
                        holdZ = g.z;
                    }
                }
            if (duty != null) for (int k = 0; k < duty.all.size(); k++) {
                Pickup p = duty.all.get(k);
                if (p.driven || Math.abs(p.speed) > 2f) continue;
                float o = clear(c, fx, fz, p.x, p.z);
                if (o < hold) {
                    hold = o;
                    holdX = p.x;
                    holdZ = p.z;
                }
            }
            others = hold;
            // people crossing in the road where there is no zebra (or fleeing over it): stop short of them too
            float folk = walkers != null ? walkers.inRoadAhead(x[c], z[c], fx, fz) : CRUISE;
            // a car ahead going the same way, or across it, or a wreck however it lies: keep back from
            // it; one coming the other way in its lane, slow for it and keep to the right
            float byCars = CRUISE, headOn = CRUISE;
            int byWhy = FREE;
            for (int o = 0; o < MOST; o++) {
                if (o == c || !live[o]) continue;
                float ox = x[o] - x[c], oz = z[o] - z[c];
                float along = ox * fx + oz * fz, across = Math.abs(ox * fz - oz * fx);
                if (along <= 0f || along >= GAP + 10f || across >= 2.2f) continue;
                boolean dead = wreck[o] || knocked[o] > 0f;
                if (!dead && (float) Math.cos(heading[o] - heading[c]) < -0.3f) {
                    if (along < 24f) headOn = Math.min(headOn, Math.max(0f, (along - 7f) * 0.9f));
                    continue;
                }
                if (along >= GAP) continue;
                float lim = Math.max(0f, (along - 6f) * 1.2f);
                if (lim < byCars) {
                    byCars = lim;
                    byWhy = dead || halt[o] > 0f || why[o] == BLOCK ? BLOCK : why[o] == WAIT ? WAIT : KNOT;
                    if (byWhy == BLOCK) {
                        holdX = x[o];
                        holdZ = z[o];
                    }
                }
            }
            if (byWhy == BLOCK && byCars < others) others = byCars;
            // which holds it most, and so why it stands
            float most = Math.min(Math.min(others, byCars), Math.min(folk, headOn));
            if (most < want) {
                want = most;
                whyNow = most == others && others < CRUISE ? BLOCK : most == byCars ? byWhy : WAIT;
            }
            // something in its way a while: round it by the other lane, when that is clear
            if (whyNow == BLOCK && speed[c] < 1.2f) blocked[c] += dt;
            else blocked[c] = Math.max(0f, blocked[c] - dt * 2f);
            // (long held, it pulls out all the same: what comes the other way slows for it)
            if (passing[c] <= 0f && blocked[c] > PASS_AFTER && left > 10f && (oncomingClear(c) || blocked[c] > PASS_ANYWAY)) {
                passing[c] = PASS_MOST;
                passX[c] = holdX;
                passZ[c] = holdZ;
                blocked[c] = 0f;
            }
            float offWant = 0f, turnRate = 2.4f;
            if (passing[c] > 0f) {
                passing[c] -= dt;
                float behind = (passX[c] - x[c]) * di[c] + (passZ[c] - z[c]) * dj[c];
                if (behind < -5f || turns && left < 8f) passing[c] = 0f;
                else if (headOn < 3f && behind > 2f) passing[c] = 0f;   // one coming: back in, and wait
                else {
                    offWant = -2f * LANE;
                    // what it goes round no longer holds it, only what is right before it
                    if (whyNow == BLOCK) want = Math.max(want, Math.min(PASS_CREEP, Math.min(folk, headOn)));
                }
            } else if (headOn < CRUISE) offWant = OFF_RIGHT;
            // held up by the other cars alone a good while (a knot at a crossing, not a queue at a
            // light): it edges out past them, never minding them a moment
            edging[c] -= dt;
            if (whyNow == KNOT && speed[c] < 0.5f && halt[c] <= 0f) {
                if ((waited[c] += dt) > 2.5f) {
                    edging[c] = 2f;
                    waited[c] = 0f;
                }
            } else waited[c] = 0f;
            if (edging[c] > 0f && whyNow == KNOT) want = Math.max(want, Math.min(TURNING * 0.6f, Math.min(others, folk)));
            spooked[c] -= dt;
            if (spooked[c] > 0f) {
                // frightened: away fast, minding only what is right before it
                want = Math.max(want, CRUISE * 1.7f);
                if (others < 3.3f || folk < 3.3f) want = Math.min(want, Math.min(others, folk));
                halt[c] = 0f;
            }
            // a pickup on the move coming at it: out of its way, aside, and faster or braking
            dodge(c, fx, fz);
            float slide = 0f;
            if (dodging[c] > 0f) {
                dodging[c] -= dt;
                offWant = dodgeOff[c];
                turnRate = 4.5f;
                if (dodgeGas[c] > 0f) want = Math.max(want, Math.min(CRUISE * DODGE_FAST, Math.max(others, 4f)));
                else if (dodgeGas[c] < -0.5f) want = 0f;
                else want = Math.min(Math.max(want, DODGE_ROLL), CRUISE);
                // nimble: it jinks aside at once, as much as it steers
                float lat = di[c] != 0 ? -(z[c] - tj[c] * City.CELL) * di[c] : (x[c] - ti[c] * City.CELL) * dj[c];
                slide = Math.max(-DODGE_JINK, Math.min(DODGE_JINK, (LANE + dodgeOff[c] - lat) * 4f));
            }
            why[c] = whyNow;
            if (halt[c] > 0f) {
                halt[c] -= dt;
                want = 0f;
            }
            float rate = want < speed[c] ? (dodging[c] > 0f ? 14f : 9f) : (dodging[c] > 0f ? 6f : 2.5f);
            speed[c] += Math.max(-rate * dt, Math.min(rate * dt, want - speed[c]));
            if (turns && left < 14f && passing[c] <= 0f) offWant = 0f;
            offWant = Math.max(OFF_LEFT, Math.min(OFF_RIGHT, offWant));
            off[c] += Math.max(-OFF_RATE * dt, Math.min(OFF_RATE * dt, offWant - off[c]));
            // the way to steer: along its street, a little ahead, keeping the line it means to keep;
            // turned off the street's way (into a turn, or coming to after a knock), toward the
            // crossing it goes to
            float sh = (float) Math.atan2(di[c], dj[c]);
            float aim;
            if (left > 12f && (float) Math.cos(sh - heading[c]) > 0.85f) {
                float look = Math.max(6f, speed[c] * 0.7f), line = LANE + off[c];
                float ax = di[c] != 0 ? x[c] + di[c] * look : ti[c] * City.CELL + dj[c] * line;
                float az = di[c] != 0 ? tj[c] * City.CELL - di[c] * line : z[c] + dj[c] * look;
                aim = (float) Math.atan2(ax - x[c], az - z[c]);
            } else aim = (float) Math.atan2(ex, ez);
            float d = aim - heading[c];
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            float turn = turnRate * dt * Math.max(0.3f, speed[c] / CRUISE);
            heading[c] += Math.max(-turn, Math.min(turn, d));
            x[c] += (float) Math.sin(heading[c]) * speed[c] * dt + dj[c] * slide * dt;
            z[c] += (float) Math.cos(heading[c]) * speed[c] * dt - di[c] * slide * dt;
        }
    }

    /** Whether the other lane is clear for car c to go round something: no car coming in it for a good way. */
    private boolean oncomingClear(int c) {
        for (int o = 0; o < MOST; o++) {
            if (o == c || !live[o]) continue;
            float ox = x[o] - x[c], oz = z[o] - z[c];
            float along = ox * di[c] + oz * dj[c], aside = ox * dj[c] - oz * di[c];
            // in the other lane (to its left), ahead or beside
            if (along < -4f || along > PASS_CLEAR || aside > 1f || aside < -City.ROAD - 1f) continue;
            if (wreck[o] || knocked[o] > 0f) {
                if (aside < -1.5f && along < 14f) return false;
                continue;
            }
            if ((float) Math.cos(heading[o] - heading[c]) < 0.3f || aside < -2.4f) return false;
        }
        for (int k = 0; k < threatN; k++) {
            float ox = threats[k].x - x[c], oz = threats[k].z - z[c];
            if (ox * ox + oz * oz < PASS_CLEAR * PASS_CLEAR) return false;
        }
        return true;
    }

    /**
     * A pickup on the move that would run into car c soon: the car makes for the side away from
     * where it would pass, braking if it would be before the car, faster if behind.
     */
    private void dodge(int c, float fx, float fz) {
        for (int k = 0; k < threatN; k++) {
            Pickup p = threats[k];
            float pvx = (float) Math.sin(p.heading) * p.speed, pvz = (float) Math.cos(p.heading) * p.speed;
            float rx = p.x - x[c], rz = p.z - z[c];
            if (rx * rx + rz * rz > 45f * 45f) continue;
            float vx = pvx - fx * speed[c], vz = pvz - fz * speed[c], vv = vx * vx + vz * vz;
            if (vv < 1f) continue;
            float t = -(rx * vx + rz * vz) / vv;
            if (t < 0f || t > DODGE_LOOK) continue;
            float qx = rx + vx * t, qz = rz + vz * t;
            if (qx * qx + qz * qz > DODGE_NEAR * DODGE_NEAR) continue;
            // where it would pass the car: on its right (+) or left, before it (+) or behind
            float side = qx * fz - qz * fx, fore = qx * fx + qz * fz;
            float now = rx * fx + rz * fz;
            // away from where it would pass; but if there is no room that way (the parked cars on
            // the right), the other way, round it
            float need = DODGE_CLEAR - Math.abs(side), roomLeft = off[c] - OFF_LEFT, roomRight = OFF_RIGHT - off[c];
            boolean left = side > 0f;
            if (left && roomLeft < need && roomRight > roomLeft) left = false;
            else if (!left && roomRight < need && roomLeft > roomRight) left = true;
            float to = left ? off[c] - Math.max(need, 0f) - 0.8f : off[c] + Math.max(need, 0f) + 0.8f;
            to = Math.max(OFF_LEFT, Math.min(OFF_RIGHT, to));
            dodging[c] = DODGE_FOR;
            dodgeOff[c] = to;
            // from behind: away faster; head on: on, aside; across its way ahead: it brakes
            boolean meets = (float) Math.cos(p.heading - heading[c]) * Math.signum(p.speed) < -0.6f;
            dodgeGas[c] = fore < 0f && now < 0f ? 1f : meets ? 0f : -1f;
            halt[c] = 0f;
            return;
        }
    }

    /** Whether someone stands at car c (she, a pickup), so that a wreck does not drive off into them. */
    private boolean touched(int c) {
        if (duty == null) return false;
        for (int k = 0; k < duty.all.size(); k++) {
            Pickup p = duty.all.get(k);
            float dx = p.x - x[c], dz = p.z - z[c];
            if (dx * dx + dz * dz < 25f) return true;
        }
        return false;
    }

    /**
     * A wreck comes to: it finds the street it lies nearest, turns its way (the way it is nearer
     * facing) and drives off down it, from where it lies, back into its lane.
     */
    private void comeTo(int c) {
        float gx = Math.round(x[c] / City.CELL), gz = Math.round(z[c] / City.CELL);
        float awayX = Math.abs(x[c] - gx * City.CELL), awayZ = Math.abs(z[c] - gz * City.CELL);
        float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
        int row = (int) gz;
        boolean alongZ = awayX < awayZ;
        // the street along x must be one of the town's; so must the one along z go where it points
        if (!alongZ && (row < City.SHORE || row > City.EDGE_J)) alongZ = true;
        if (alongZ) {
            int b = fz >= 0f ? 1 : -1;
            int j = b > 0 ? City.index(z[c]) + 1 : City.index(z[c]);
            if (j > City.EDGE_J || j < City.SHORE) {
                b = -b;
                j = b > 0 ? City.index(z[c]) + 1 : City.index(z[c]);
            }
            if (j > City.EDGE_J || j < City.SHORE) {
                live[c] = false;
                return;
            }
            di[c] = 0;
            dj[c] = b;
            ti[c] = (int) gx;
            tj[c] = j;
            off[c] = Math.max(OFF_LEFT, Math.min(OFF_RIGHT, (x[c] - gx * City.CELL) * b - LANE));
        } else {
            int a = fx >= 0f ? 1 : -1;
            di[c] = a;
            dj[c] = 0;
            ti[c] = a > 0 ? City.index(x[c]) + 1 : City.index(x[c]);
            tj[c] = row;
            off[c] = Math.max(OFF_LEFT, Math.min(OFF_RIGHT, -(z[c] - gz * City.CELL) * a - LANE));
        }
        choose(c);
        wreck[c] = false;
        knocked[c] = 0f;
        stood[c] = 0f;
        speed[c] = 0f;
        halt[c] = 0.6f;
        passing[c] = 0f;
        dodging[c] = 0f;
        why[c] = FREE;
        waited[c] = edging[c] = 0f;
    }

    /** A rammed car sliding and turning to rest, against the town's walls; then a wreck. */
    private void slide(int c, float dt) {
        speed[c] = 0f;
        if (knocked[c] <= 0f) return;
        knocked[c] -= dt;
        float ox = x[c], oz = z[c];
        x[c] += kvx[c] * dt;
        z[c] += kvz[c] * dt;
        heading[c] += spin[c] * dt;
        float slow = (float) Math.exp(-SLIDE_DRAG * dt);
        kvx[c] *= slow;
        kvz[c] *= slow;
        spin[c] *= (float) Math.exp(-SPIN_DRAG * dt);
        if (city != null) {
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            float[] p = {0f, 0f};
            for (int e = -1; e <= 1; e += 2) {
                p[0] = x[c] + fx * e * 1.2f;
                p[1] = z[c] + fz * e * 1.2f;
                float px0 = p[0], pz0 = p[1];
                if (city.push(p, 1.05f, 0f)) {
                    // against a wall: it stops there, what is left of the slide lost
                    x[c] += p[0] - px0;
                    z[c] += p[1] - pz0;
                    kvx[c] *= 0.3f;
                    kvz[c] *= 0.3f;
                    spin[c] *= 0.5f;
                }
            }
            if (City.overWater(x[c], z[c])) {
                x[c] = ox;
                z[c] = oz;
                kvx[c] = kvz[c] = 0f;
            }
        }
        if (knocked[c] <= 0f || kvx[c] * kvx[c] + kvz[c] * kvz[c] < 0.04f && Math.abs(spin[c]) < 0.05f) {
            knocked[c] = 0f;
            wreck[c] = true;
            stood[c] = 0f;
            comeTo[c] = COME_TO + rnd.nextFloat() * COME_TO_MORE;
            // never left standing in a bay at the factory's gate or the warehouse's: it is towed off
            if (City.inBay(true, x[c], z[c]) || City.inBay(false, x[c], z[c]) || City.inClinicBay(x[c], z[c]) || City.inWorkshopBay(x[c], z[c])) live[c] = false;
        }
    }

    /**
     * A car parked at a curb, rammed off it: taken into the traffic (the one farthest from the
     * blow, if none is free) as a car of its kind and paint at (x, z) facing yaw, and sent off.
     */
    void unparked(float x, float z, float yaw, int k, int p, float fx, float fz, float v, float hx, float hz) {
        int c = -1;
        float far = -1f;
        for (int o = 0; o < MOST; o++) {
            if (!live[o]) {
                c = o;
                break;
            }
            float dx = this.x[o] - hx, dz = this.z[o] - hz, dd = dx * dx + dz * dz;
            if (dd > far) {
                far = dd;
                c = o;
            }
        }
        this.x[c] = x;
        this.z[c] = z;
        heading[c] = yaw;
        kind[c] = k;
        paint[c] = p;
        live[c] = true;
        halt[c] = 0f;
        spooked[c] = 0f;
        speed[c] = 0f;
        rammed(c, fx, fz, v, hx, hz);
    }

    /**
     * Car c rammed by the pickup she drives, going so fast along (fx, fz), the blow at (hx, hz): off
     * it slides the way it was struck, turning by where it was struck, and stands a wreck.
     */
    private void rammed(int c, float fx, float fz, float v, float hx, float hz) {
        float push = Math.max(v * 0.85f, 3f);
        // away from the blow as much as along the pickup's way
        float ax = x[c] - hx, az = z[c] - hz, al = (float) Math.sqrt(ax * ax + az * az) + 1e-3f;
        kvx[c] = fx * push * 0.75f + ax / al * push * 0.35f + speed[c] * (float) Math.sin(heading[c]) * 0.5f;
        kvz[c] = fz * push * 0.75f + az / al * push * 0.35f + speed[c] * (float) Math.cos(heading[c]) * 0.5f;
        // turned by where along it the blow came: struck at an end, it spins
        float cfx = (float) Math.sin(heading[c]), cfz = (float) Math.cos(heading[c]);
        float along = (hx - x[c]) * cfx + (hz - z[c]) * cfz;
        float across = fx * cfz - fz * cfx;
        spin[c] = Math.max(-4f, Math.min(4f, -along * across * push * 0.35f + (rnd.nextFloat() - 0.5f) * 0.6f));
        knocked[c] = KNOCKED_FOR;
        wreck[c] = false;
        speed[c] = 0f;
        rams++;
        ramX = hx;
        ramZ = hz;
        ramHard = Math.min(1f, v / 20f);
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
            knocked[c] = 0f;
            wreck[c] = false;
            off[c] = passing[c] = dodging[c] = blocked[c] = 0f;
            why[c] = FREE;
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

    /** For the desktop stand: car c a wreck at (x, z) facing so, just come to rest. */
    void standWreck(int c, float px, float pz, float yaw) {
        x[c] = px;
        z[c] = pz;
        heading[c] = yaw;
        speed[c] = 0f;
        halt[c] = 0f;
        kind[c] = SEDAN;
        paint[c] = PAINT[3];
        live[c] = true;
        knocked[c] = 0f;
        wreck[c] = true;
        stood[c] = 0f;
        comeTo[c] = COME_TO + COME_TO_MORE;
    }

    /**
     * For the desktop stand: car 0 coming down the street at the pickup p, so far ahead of it, in
     * its own lane; the pickup put in that lane too, so that they meet head on.
     */
    void standComing(Pickup p, float ahead) {
        int a = Math.round((float) Math.sin(p.heading)), b = Math.round((float) Math.cos(p.heading));
        if (a != 0) b = 0;
        int c = 0;
        if (a != 0) {
            float zs = Math.round(p.z / City.CELL) * City.CELL;
            di[c] = -a;
            dj[c] = 0;
            z[c] = zs - di[c] * LANE;
            p.z = z[c];
            x[c] = p.x + a * ahead;
            ti[c] = di[c] > 0 ? City.index(x[c]) + 1 : City.index(x[c]);
            tj[c] = Math.round(zs / City.CELL);
        } else {
            float xs = Math.round(p.x / City.CELL) * City.CELL;
            di[c] = 0;
            dj[c] = -b;
            x[c] = xs + dj[c] * LANE;
            p.x = x[c];
            z[c] = p.z + b * ahead;
            ti[c] = Math.round(xs / City.CELL);
            tj[c] = dj[c] > 0 ? City.index(z[c]) + 1 : City.index(z[c]);
        }
        ni[c] = di[c];
        nj[c] = dj[c];
        heading[c] = (float) Math.atan2(di[c], dj[c]);
        speed[c] = CRUISE;
        halt[c] = 0f;
        kind[c] = SEDAN;
        paint[c] = PAINT[1];
        live[c] = true;
        wreck[c] = false;
        knocked[c] = 0f;
        off[c] = passing[c] = dodging[c] = 0f;
    }

    /** For the desktop stand: the cars within so far of (px, pz), each told: how fast, why it stands, going round, dodging, a wreck. */
    String told(float px, float pz, float far) {
        StringBuilder b = new StringBuilder();
        String[] whys = {"free", "wait", "block", "knot"};
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float dx = x[c] - px, dz = z[c] - pz;
            if (dx * dx + dz * dz > far * far) continue;
            b.append(String.format(" [%d %.0f,%.0f %.1fm/s %s%s%s%s]", c, x[c], z[c], speed[c], whys[why[c]],
                    passing[c] > 0f ? " passing" : "", dodging[c] > 0f ? " dodging" : "", wreck[c] ? " wreck " + Math.round(stood[c]) : ""));
        }
        return b.toString();
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

    /** How many pickups are on the move about the town this frame, and each. */
    int threatCount() {
        return threatN;
    }

    Pickup threat(int k) {
        return threats[k];
    }

    /** Whether car c is a wreck, or sliding to become one. */
    boolean wrecked(int c) {
        return wreck[c] || knocked[c] > 0f;
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

    /**
     * The same, all but one pickup (the one that is pushing). The one she drives is not stopped by
     * a car going fast enough: the car is rammed out of its way (the pickup does not break).
     */
    boolean push(float[] p, float rr, float lift, Pickup but) {
        boolean hit = false;
        if (duty != null && lift < Pickup.TOP - 0.1f) hit = duty.push(p, rr, but);
        if (lift > TOP - 0.1f) return hit;
        boolean rams = but != null && but.driven && Math.abs(but.speed) > RAM_FROM;
        for (int c = 0; c < MOST; c++) {
            if (!live[c]) continue;
            float fx = (float) Math.sin(heading[c]), fz = (float) Math.cos(heading[c]);
            for (int e = -1; e <= 1; e += 2) {
                float cx = x[c] + fx * e * 1.2f, cz = z[c] + fz * e * 1.2f;
                float dx = p[0] - cx, dz = p[1] - cz, dd = dx * dx + dz * dz, m = rr + 1.05f;
                if (dd >= m * m || dd < 1e-8f) continue;
                float l = (float) Math.sqrt(dd);
                if (rams) {
                    // the car shoved out of the way by what overlapped, and sent off
                    float over = m - l;
                    x[c] -= dx / l * over;
                    z[c] -= dz / l * over;
                    if (knocked[c] < KNOCKED_FOR - 0.25f) {
                        float bf = (float) Math.sin(but.heading), bz = (float) Math.cos(but.heading);
                        rammed(c, bf * Math.signum(but.speed), bz * Math.signum(but.speed), Math.abs(but.speed), p[0], p[1]);
                    }
                    continue;
                }
                p[0] = cx + dx / l * m;
                p[1] = cz + dz / l * m;
                hit = true;
            }
        }
        return hit;
    }

    // the pickup she drives rams a car out of its way going faster than this (meters a second)
    static final float RAM_FROM = 3f;

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
