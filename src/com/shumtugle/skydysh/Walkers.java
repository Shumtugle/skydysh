package com.shumtugle.skydysh;

import java.util.Random;

/**
 * People out for the evening on the pavements: each goes round a block, one way or the other,
 * along the middle of the pavement. Someone running at them makes them step aside toward the
 * houses and slow, not stop; one standing in their way, they turn back from. A mallet thrown
 * past them, a blow struck near, a burning brum running by, and they are off: away from it at a
 * run, round what is in the way, until they are gone; one in five turns first and stares a moment,
 * as one does at whoever did it. Like the traffic, they come and go out of sight round the runner.
 *
 * A person is one of the town's meshes when the game has them (see FolkPack), walking with the
 * skeleton's own walk; otherwise round parts, the stride swinging the legs, the arms against them.
 */
final class Walkers {
    private static final int[] SKINS = {0xE9BC98, 0xD69F7A, 0xB98058, 0x7E5236, 0xF0CDAE};
    private static final int[] TOPS = {0xF4F1EA, 0xD9735A, 0xE8C25A, 0x5E9E92, 0x6F92C6, 0xD58FAE, 0x2E2E34, 0xB8CF8A};
    private static final int[] LEGS = {0x3F5679, 0xCDBB98, 0x54704A, 0x2F2F36, 0xE3DDCF, 0xB44A3E};
    private static final int[] HAIR = {0x2E2118, 0x5A3C24, 0xC9A25E, 0x161311, 0x8A4A26};
    private static final int SHOE = 0x3A302A, STRAW = 0xE3C784, BAG = 0x6A4A32;
    private static final int SHORTS = 0, TROUSERS = 1, SKIRT = 2;

    // the ring round a block they walk on: so far in from each street's middle
    static final float RING = City.CURB + 1.9f;
    // a bench's seat over the pavement; the hips over the feet on the skeleton
    static final float SEAT = 0.5f, HIPS = 0.93f;
    static final float BORN_NEAR = 28f, BORN_FAR = 110f, GONE = 140f, SHY = 3.2f, KEPT = 80f, SEATED = 65f;
    // so many go round the blocks; so many sit on the benches; so many walk about the squares; on
    // the beach, so many lie on the loungers and so many stroll along the sand
    static final int ROUNDERS = 54, SITTERS = 24, SQUARERS = 14, LOUNGERS = 10, BATHERS = 12;
    static final int MOST = ROUNDERS + SITTERS + SQUARERS + LOUNGERS + BATHERS;
    private static final int FIRST_LOUNGER = ROUNDERS + SITTERS + SQUARERS, FIRST_BATHER = FIRST_LOUNGER + LOUNGERS;
    // the beach: kept for her only so far inland; lying on a lounger, the hips so far over the sand
    static final float BEACH_NEAR = 120f, LOUNGE_SEAT = 0.3f;
    // what one is doing: going round a block, sitting on a bench, going round a fountain, to and
    // fro along a garden's path
    static final int ROUND = 0, SITS = 1, FOUNTAIN = 2, PATH = 3, FLEE = 4, CROSS = 5;
    // going round a block, they cross the street at a zebra now and then: straight on at a corner,
    // over to the block across at one before a place's way in; at the curb they wait for the light
    // (red for the street's cars) or, with none, till no car comes that could not stop; given up
    // after so long. Over it a little briskly
    static final float CROSS_CORNER = 0.18f, CROSS_MID = 0.35f, CROSS_PATIENCE = 14f, CROSS_PACE = 1.35f;
    // a car or a pickup coming at one: looked for so far ahead in time, as near as this it would
    // pass; aside so fast for so long, no farther than this; back to the way after so long clear
    static final float DODGE_LOOK = 1.5f, DODGE_NEAR = 1.9f, DODGE_RUN = 4.6f, DODGE_FOR = 0.6f, DODGE_MOST = 3.6f,
            DODGE_BACK = 0.9f, HURRY = 4f;
    private static final int WAITS = 0, WALKS = 1;
    // off at a run: from where, how long now, how long the stare first, how fast; gone after so long
    private final float[] fromX = new float[MOST], fromZ = new float[MOST], fleeFor = new float[MOST],
            stare = new float[MOST], runs = new float[MOST], turned = new float[MOST];
    static final float FLEE_GONE = 7f, FLEE_FAR = 45f, STARTLED = 1.3f;

    private final Random rnd = new Random(0x5EE7L);
    private final int[] bi = new int[MOST], bj = new int[MOST], top = new int[MOST], legs = new int[MOST],
            skin = new int[MOST], hair = new int[MOST], kind = new int[MOST], model = new int[MOST];
    private final Stroll.Way[] way = new Stroll.Way[MOST];
    private final float[] u = new float[MOST], v = new float[MOST], dir = new float[MOST], ph = new float[MOST],
            size = new float[MOST], aside = new float[MOST];
    final float[] x = new float[MOST], z = new float[MOST], yaw = new float[MOST];
    // the way out from where each walks toward the road, level
    final float[] outX = new float[MOST], outZ = new float[MOST];
    private final boolean[] hat = new boolean[MOST], longHair = new boolean[MOST], bag = new boolean[MOST],
            live = new boolean[MOST];
    private final int[] mode = new int[MOST];
    // a square's walker: the middle it goes round (or the path's middle), how far out, where on
    // the way round (or along), the path along x or not; one sitting: leaning back, turning the head
    private final float[] mx = new float[MOST], mz = new float[MOST], far = new float[MOST], at = new float[MOST],
            lean = new float[MOST], look = new float[MOST];
    private final boolean[] alongX = new boolean[MOST];
    // what one stands on (or, sitting, sits on): the pavement, a bench, the sand
    private final float[] gy = new float[MOST];
    private final float[] seatBuf = new float[3 * 96];
    // crossing: from (cax, caz) over to (cbx, cbz), so far now of so far; the zebra's middle; waiting or
    // walking, how long waited; the light it waits for (its crossing, the street's cars along x or
    // not), if any; and the block, the way round it and the direction they go on in after
    private final float[] cax = new float[MOST], caz = new float[MOST], cbx = new float[MOST], cbz = new float[MOST],
            cAt = new float[MOST], cLen = new float[MOST], zx = new float[MOST], zz = new float[MOST], cWaited = new float[MOST],
            nu = new float[MOST], ndir = new float[MOST];
    private final int[] cPhase = new int[MOST], cLi = new int[MOST], cLj = new int[MOST], nbi = new int[MOST], nbj = new int[MOST];
    private final boolean[] cLit = new boolean[MOST], cLitX = new boolean[MOST], zAlongX = new boolean[MOST];
    // stepped out of the way of a car: so far off where they walk (x, z), going aside so fast for so
    // long more, and how long since the last one came at them
    private final float[] ox = new float[MOST], oz = new float[MOST], dvx = new float[MOST], dvz = new float[MOST],
            dodgeFor = new float[MOST], calm = new float[MOST];
    /** The traffic, for the people to keep out of its way and to cross between; none, none. */
    Cars cars;
    private final float[] threat = new float[2];
    private float retry;

    private static float side() {
        return City.CELL - 2f * RING;
    }

    private boolean started;

    /** Only those going round the blocks (no town to sit or walk a square in: the opening's). */
    void update(float dt, Runner r, Chase ch) {
        update(dt, r, ch, null);
    }

    void update(float dt, Runner r, Chase ch, City city) {
        float len = side() * 4f;
        retry -= dt;
        boolean look4 = retry <= 0f;
        if (look4) retry = 1f;
        for (int i = 0; i < MOST; i++) {
            float rx = x[i] - r.x, rz = z[i] - r.z;
            // those sitting and walking the squares are kept nearer her, so that where she is there are some
            float gone = i < ROUNDERS ? GONE : KEPT;
            if (!live[i] || rx * rx + rz * rz > gone * gone) {
                if (i < ROUNDERS) spawn(i, r, !started);
                else if (city != null && (look4 || !started)) {
                    live[i] = false;
                    if (i < ROUNDERS + SITTERS) seat(i, r, city, !started);
                    else if (i < FIRST_LOUNGER) square(i, r, city, !started);
                    else if (i < FIRST_BATHER) lounge(i, r, city, !started);
                    else bathe(i, r, !started);
                }
                continue;
            }
            turned[i] -= dt;
            if (mode[i] == FLEE) {
                flee(i, dt, r, city);
                continue;
            }
            // a burning brum running by, or she running into them: off at a run
            if (ch.speed > 3f && dist(i, ch.x, ch.z) < 5f) {
                alarm(i, ch.x, ch.z);
                continue;
            }
            float near = dist(i, r.x, r.z);
            if (near < STARTLED && r.speed > 5f) {
                alarm(i, r.x, r.z);
                continue;
            }
            if (mode[i] == CROSS) {
                cross(i, dt, city);
                continue;
            }
            if (mode[i] != ROUND) {
                // a car coming at one sitting or walking a square: up and off out of its way
                if (coming(x[i], z[i])) {
                    alarm(i, x[i] - threat[0], z[i] - threat[1]);
                    continue;
                }
                elsewhere(i, dt, r, ch);
                continue;
            }
            // someone running at them: they step aside and slow; one standing in the way, they turn back from
            boolean shy = near < SHY && r.speed > 2f;
            if (near < 1.3f && turned[i] <= 0f) {
                dir[i] = -dir[i];
                turned[i] = 3f;
            }
            float want = near < SHY ? 0.55f : 0f;
            aside[i] += (want - aside[i]) * Math.min(1f, dt * 5f);
            float pace = shy ? 0.4f : 1f;
            float was = u[i];
            u[i] = (u[i] + dir[i] * v[i] * pace * dt + len) % len;
            ph[i] += dt * v[i] * pace * 3.2f;
            place(i);
            if (city != null && dodgeFor[i] <= 0f && ox[i] == 0f && oz[i] == 0f && crossing(i, was, city)) continue;
            stepAside(i, dt, city);
        }
        started = true;
    }

    /**
     * A car or pickup on the move about to run into someone at (px, pz): the way out of its path
     * (away across it, on the side they already are) into threat; false if none comes at them.
     */
    private boolean coming(float px, float pz) {
        if (cars == null) return false;
        int n = Cars.MOST + cars.threatCount();
        for (int k = 0; k < n; k++) {
            float vx, vz, qx, qz;
            if (k < Cars.MOST) {
                if (!cars.live(k) || cars.wrecked(k) || cars.speed[k] < 2.5f) continue;
                vx = (float) Math.sin(cars.heading[k]) * cars.speed[k];
                vz = (float) Math.cos(cars.heading[k]) * cars.speed[k];
                qx = cars.x[k];
                qz = cars.z[k];
            } else {
                Pickup p = cars.threat(k - Cars.MOST);
                vx = (float) Math.sin(p.heading) * p.speed;
                vz = (float) Math.cos(p.heading) * p.speed;
                qx = p.x;
                qz = p.z;
            }
            float rx = px - qx, rz = pz - qz;
            if (rx * rx + rz * rz > 30f * 30f) continue;
            float vv = vx * vx + vz * vz;
            if (vv < 4f) continue;
            float t = (rx * vx + rz * vz) / vv;
            if (t < 0f || t > DODGE_LOOK) continue;
            float mx = qx + vx * t - px, mz = qz + vz * t - pz;
            if (mx * mx + mz * mz > DODGE_NEAR * DODGE_NEAR) continue;
            float vl = (float) Math.sqrt(vv), nx = -vz / vl, nz = vx / vl;
            float sd = rx * nx + rz * nz >= 0f ? 1f : -1f;
            threat[0] = nx * sd;
            threat[1] = nz * sd;
            return true;
        }
        return false;
    }

    /**
     * One going round a block: out of the way of a car coming at them, quick and aside; back to
     * where they walk once none has come a while. Kept out of the walls.
     */
    private void stepAside(int i, float dt, City city) {
        calm[i] += dt;
        if (coming(x[i], z[i])) {
            if (dodgeFor[i] <= 0f) {
                dvx[i] = threat[0] * DODGE_RUN;
                dvz[i] = threat[1] * DODGE_RUN;
            }
            dodgeFor[i] = DODGE_FOR;
            calm[i] = 0f;
        }
        if (dodgeFor[i] > 0f) {
            dodgeFor[i] -= dt;
            ox[i] += dvx[i] * dt;
            oz[i] += dvz[i] * dt;
            float l = (float) Math.sqrt(ox[i] * ox[i] + oz[i] * oz[i]);
            if (l > DODGE_MOST) {
                ox[i] *= DODGE_MOST / l;
                oz[i] *= DODGE_MOST / l;
            }
            yaw[i] = (float) Math.atan2(dvx[i], dvz[i]);
            ph[i] += dt * DODGE_RUN * 3.2f;
        } else if (calm[i] > 0.8f && (ox[i] != 0f || oz[i] != 0f)) {
            float l = (float) Math.sqrt(ox[i] * ox[i] + oz[i] * oz[i]), step = DODGE_BACK * dt;
            if (l <= step) ox[i] = oz[i] = 0f;
            else {
                ox[i] -= ox[i] / l * step;
                oz[i] -= oz[i] / l * step;
            }
        }
        if (ox[i] == 0f && oz[i] == 0f) return;
        float bx = x[i], bz = z[i];
        float[] p = {bx + ox[i], bz + oz[i]};
        if (city != null) city.push(p, 0.3f, City.WALK_TOP);
        ox[i] = p[0] - bx;
        oz[i] = p[1] - bz;
        x[i] = p[0];
        z[i] = p[1];
    }

    // which street the side e of block (bi, bj) is on: the cell owning it, running north or not;
    // and the block across it
    private static int sideI(int bi, int e) {
        return e == 1 ? bi + 1 : bi;
    }

    private static int sideJ(int bj, int e) {
        return e == 2 ? bj + 1 : bj;
    }

    private static boolean sideNorth(int e) {
        return e == 1 || e == 3;
    }

    private static int acrossI(int bi, int e) {
        return e == 1 ? bi + 1 : e == 3 ? bi - 1 : bi;
    }

    private static int acrossJ(int bj, int e) {
        return e == 0 ? bj - 1 : e == 2 ? bj + 1 : bj;
    }

    /** Whether block row j is one people go round: in the town, not the sea, not past its end. */
    private static boolean block(int j) {
        return !City.sea(j) && !City.country(j);
    }

    /**
     * One going round their block has come from u = was to where they are now: at a corner, or
     * by a zebra before a place's way in, they may make to cross the street there. True if they do.
     */
    private boolean crossing(int i, float was, City city) {
        float s = side(), len = s * 4f;
        int e0 = (int) (was / s) % 4, e1 = (int) (u[i] / s) % 4;
        float du = u[i] - was;
        if (du > len * 0.5f) du -= len;
        if (du < -len * 0.5f) du += len;
        if (e0 != e1 && Math.abs(du) < s) {
            // round a corner: straight on over the street ahead
            if (rnd.nextFloat() > CROSS_CORNER) return false;
            int e = e0, over = dir[i] > 0f ? (e + 1) % 4 : (e + 3) % 4;
            int ni = acrossI(bi[i], over), nj = acrossJ(bj[i], over);
            if (!block(nj)) return false;
            // the corner they are at: the start of the side going on (or of their own, going back)
            int q = dir[i] > 0f ? (e + 1) % 4 : e;
            float x0 = bi[i] * City.CELL + RING, z0 = bj[i] * City.CELL + RING;
            float cx = q == 1 || q == 2 ? x0 + s : x0, cz = q >= 2 ? z0 + s : z0;
            float t = sideNorth(over) ? cz - bj[i] * City.CELL : cx - bi[i] * City.CELL;
            if (!Crossings.zebraAt(city, sideI(bi[i], over), sideJ(bj[i], over), sideNorth(over), t)) return false;
            // on in the block across: the same side, at its start or end, the same way
            float landU = dir[i] > 0f ? e * s + 0.01f : (e + 1) * s - 0.01f;
            startCross(i, cx, cz, over, ni, nj, landU, dir[i]);
            return true;
        }
        if (e0 != e1) return false;
        // along a side: past its zebra before a place's way in
        int e = e1;
        float k0 = was - e * s, k1 = u[i] - e * s;
        float t0 = e < 2 ? RING + k0 : RING + s - k0, t1 = e < 2 ? RING + k1 : RING + s - k1;
        float m = Crossings.mid(city, sideI(bi[i], e), sideJ(bj[i], e), sideNorth(e));
        if (m < 0f || (t0 - m) * (t1 - m) > 0f || rnd.nextFloat() > CROSS_MID) return false;
        int ni = acrossI(bi[i], e), nj = acrossJ(bj[i], e);
        if (!block(nj)) return false;
        float x0 = bi[i] * City.CELL, z0 = bj[i] * City.CELL;
        float cx = e == 0 || e == 2 ? x0 + m : e == 1 ? x0 + City.CELL - RING : x0 + RING;
        float cz = e == 1 || e == 3 ? z0 + m : e == 2 ? z0 + City.CELL - RING : z0 + RING;
        // on the far side, the side facing back; which way round, as it falls
        int back = (e + 2) % 4;
        float nx0 = ni * City.CELL + RING, nz0 = nj * City.CELL + RING;
        float lx = cx + (e == 1 ? 2f * RING : e == 3 ? -2f * RING : 0f), lz = cz + (e == 2 ? 2f * RING : e == 0 ? -2f * RING : 0f);
        float k = back == 0 ? lx - nx0 : back == 1 ? lz - nz0 : back == 2 ? nx0 + s - lx : nz0 + s - lz;
        startCross(i, cx, cz, e, ni, nj, back * s + Math.max(0.01f, Math.min(s - 0.01f, k)), rnd.nextBoolean() ? 1f : -1f);
        return true;
    }

    /** Person i makes to cross from (ax, az) over the street on side e of their block, into block (ni, nj) at u, going on so. */
    private void startCross(int i, float ax, float az, int e, int ni, int nj, float landU, float goes) {
        float nx = e == 1 ? 1f : e == 3 ? -1f : 0f, nz = e == 2 ? 1f : e == 0 ? -1f : 0f;
        mode[i] = CROSS;
        cPhase[i] = WAITS;
        cWaited[i] = 0f;
        // not all on one line: a little to one side or the other along the zebra
        float spread = ((i % 3) - 1) * 0.45f;
        cax[i] = ax + nz * spread;
        caz[i] = az + nx * spread;
        cbx[i] = cax[i] + nx * 2f * RING;
        cbz[i] = caz[i] + nz * 2f * RING;
        cLen[i] = 2f * RING;
        cAt[i] = 0f;
        zx[i] = ax + nx * RING;
        zz[i] = az + nz * RING;
        zAlongX[i] = nx != 0f;
        // its light, if the zebra is by a crossing that has one: the street's cars run along z if they cross along x
        int li = Math.round(zx[i] / City.CELL), lj = Math.round(zz[i] / City.CELL);
        float ddx = zx[i] - li * City.CELL, ddz = zz[i] - lj * City.CELL;
        cLit[i] = ddx * ddx + ddz * ddz < 12f * 12f && Crossings.working(li, lj);
        cLi[i] = li;
        cLj[i] = lj;
        cLitX[i] = nx == 0f;
        nbi[i] = ni;
        nbj[i] = nj;
        nu[i] = landU;
        ndir[i] = goes;
        ox[i] = oz[i] = 0f;
        x[i] = cax[i];
        z[i] = caz[i];
        yaw[i] = (float) Math.atan2(nx, nz);
        aside[i] = 0f;
    }

    /** Whether a car comes on the street that could not stop before the zebra of person i. */
    private boolean carsComing(int i) {
        if (cars == null) return false;
        int n = Cars.MOST + cars.threatCount();
        for (int k = 0; k < n; k++) {
            float hx, hz, sp, px, pz;
            if (k < Cars.MOST) {
                if (!cars.live(k)) continue;
                hx = (float) Math.sin(cars.heading[k]);
                hz = (float) Math.cos(cars.heading[k]);
                sp = cars.wrecked(k) ? 0f : cars.speed[k];
                px = cars.x[k];
                pz = cars.z[k];
            } else {
                Pickup p = cars.threat(k - Cars.MOST);
                hx = (float) Math.sin(p.heading) * Math.signum(p.speed);
                hz = (float) Math.cos(p.heading) * Math.signum(p.speed);
                sp = Math.abs(p.speed);
                px = p.x;
                pz = p.z;
            }
            // only those going along the street the zebra is across
            if (zAlongX[i] ? Math.abs(hz) < 0.7f : Math.abs(hx) < 0.7f) continue;
            float rx = zx[i] - px, rz = zz[i] - pz;
            float along = rx * hx + rz * hz, aside = Math.abs(rx * hz - rz * hx);
            if (aside > City.ROAD + 1f || along < -2.6f || along > 45f) continue;
            if (along < 2.6f || along < sp * sp / 16f + 2.6f + sp * 0.6f) return true;
        }
        return false;
    }

    /** One crossing a street: waiting at the curb, then over it; out of the way of what comes at them; then on round the block across. */
    private void cross(int i, float dt, City city) {
        float nx = (cbx[i] - cax[i]) / cLen[i], nz = (cbz[i] - caz[i]) / cLen[i];
        if (cPhase[i] == WAITS) {
            cWaited[i] += dt;
            if (cLit[i] && !Crossings.working(cLi[i], cLj[i])) cLit[i] = false;
            boolean go = cLit[i] ? Crossings.light(cLi[i], cLj[i], cLitX[i]) == Crossings.HALT && !carsComing(i) : !carsComing(i);
            if (go) cPhase[i] = WALKS;
            else if (cWaited[i] > CROSS_PATIENCE) {
                // given up: on round their own block
                mode[i] = ROUND;
                return;
            }
            x[i] = cax[i];
            z[i] = caz[i];
            yaw[i] = (float) Math.atan2(nx, nz);
            aside[i] = 1f;
            return;
        }
        aside[i] = 0f;
        float pace = v[i] * CROSS_PACE;
        // something coming at them in the road: on at a run to the nearer curb ahead, or back
        float px = cax[i] + nx * cAt[i], pz = caz[i] + nz * cAt[i];
        int way = 1;
        if (coming(px, pz)) {
            way = threat[0] * nx + threat[1] * nz >= 0f ? 1 : -1;
            pace = HURRY;
        }
        cAt[i] += way * pace * dt;
        ph[i] += pace * dt * 3.2f;
        if (cAt[i] <= 0f) {
            // back on the curb: wait again
            cAt[i] = 0f;
            cPhase[i] = WAITS;
            cWaited[i] = 0f;
        }
        if (cAt[i] >= cLen[i]) {
            // over: on round the block across
            bi[i] = nbi[i];
            bj[i] = nbj[i];
            u[i] = nu[i];
            dir[i] = ndir[i];
            mode[i] = ROUND;
            place(i);
            return;
        }
        x[i] = cax[i] + nx * cAt[i];
        z[i] = caz[i] + nz * cAt[i];
        yaw[i] = (float) Math.atan2(nx * way, nz * way);
    }

    /**
     * How fast a car at (px, pz) going along (fx, fz) at speed sp may go for the people at a zebra
     * ahead of it: crossing on it, or waiting by it where there is no light, if it can still stop.
     */
    float zebraAhead(float px, float pz, float fx, float fz, float sp) {
        float lim = Cars.CRUISE * 2f;
        for (int i = 0; i < ROUNDERS; i++) {
            if (!live[i] || mode[i] != CROSS) continue;
            if (cPhase[i] == WAITS && cLit[i]) continue;
            if (zAlongX[i] ? Math.abs(fz) < 0.7f : Math.abs(fx) < 0.7f) continue;
            float rx = zx[i] - px, rz = zz[i] - pz;
            float along = rx * fx + rz * fz, aside = Math.abs(rx * fz - rz * fx);
            if (aside > City.ROAD + 1f || along < -1f || along > 40f) continue;
            float stop = along - Crossings.HALF - Crossings.NOSE;
            if (cPhase[i] == WAITS && (stop < sp * sp / 16f || stop > 25f)) continue;
            if (stop < -0.5f) continue;
            if (cPhase[i] == WALKS) {
                // over: past the car's side of the road, or not yet near it, coming the other way
                float side = (x[i] - px) * fz - (z[i] - pz) * fx;
                float nx = cbx[i] - cax[i], nz = cbz[i] - caz[i];
                float toward = -(nx * fz - nz * fx) * Math.signum(side);
                if (Math.abs(side) > 2.2f && (toward <= 0f || Math.abs(side) > 4.8f)) continue;
            }
            lim = Math.min(lim, Math.max(0f, stop * 1.1f));
        }
        return lim;
    }

    /** How fast a car at (px, pz) going along (fx, fz) may go for anyone in the road before it: no faster than it can stop short. */
    float inRoadAhead(float px, float pz, float fx, float fz) {
        float lim = Cars.CRUISE * 2f;
        for (int i = 0; i < MOST; i++) {
            if (!live[i] || mode[i] == SITS) continue;
            float rx = x[i] - px, rz = z[i] - pz;
            float along = rx * fx + rz * fz;
            if (along < 0f || along > 16f) continue;
            float aside = Math.abs(rx * fz - rz * fx);
            if (aside > 2.1f) continue;
            // in the road: nearer a street's middle than its asphalt's edge
            float sx = Math.abs(x[i] - Math.round(x[i] / City.CELL) * City.CELL), sz = Math.abs(z[i] - Math.round(z[i] / City.CELL) * City.CELL);
            if (Math.min(sx, sz) > City.ROAD + 0.2f) continue;
            lim = Math.min(lim, Math.max(0f, (along - 3.4f) * 1.1f));
        }
        return lim;
    }

    /** For the desktop stand: each one at a zebra, told: where, waiting or crossing, how far over, how long waited, by a light. */
    String toldCrossing() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ROUNDERS; i++)
            if (live[i] && mode[i] == CROSS)
                b.append(String.format(" [%d %.0f,%.0f %s %.1f/%.1f w%.1f%s]", i, x[i], z[i], cPhase[i] == WALKS ? "walks" : "waits",
                        cAt[i], cLen[i], cWaited[i], cLit[i] ? " lit" : ""));
        return b.toString();
    }

    /** For the desktop stand: how many are waiting at a zebra, and how many crossing on one. */
    int crossingNow(boolean walking) {
        int n = 0;
        for (int i = 0; i < ROUNDERS; i++)
            if (live[i] && mode[i] == CROSS && (cPhase[i] == WALKS) == walking) n++;
        return n;
    }

    /** One sitting, or walking a square: the walk along its way, stopping short of someone running at them. */
    private void elsewhere(int i, float dt, Runner r, Chase ch) {
        if (mode[i] == SITS) {
            look[i] = 0.5f * (float) Math.sin(ph[i] += dt * 0.3f);
            return;
        }
        // someone running at them: slower; one standing in the way: they turn back
        float near = dist(i, r.x, r.z);
        boolean shy = near < SHY && r.speed > 2f;
        if (near < 1.3f && turned[i] <= 0f) {
            dir[i] = -dir[i];
            turned[i] = 3f;
        }
        float step = (shy ? 0.4f : 1f) * v[i] * dt;
        ph[i] += step * 3.2f;
        if (mode[i] == FOUNTAIN) {
            at[i] += dir[i] * step / far[i];
            x[i] = mx[i] + (float) Math.cos(at[i]) * far[i];
            z[i] = mz[i] + (float) Math.sin(at[i]) * far[i];
            // going round: across the way out, the one way or the other
            float tx = -(float) Math.sin(at[i]) * dir[i], tz = (float) Math.cos(at[i]) * dir[i];
            yaw[i] = (float) Math.atan2(tx, tz);
        } else {
            at[i] += dir[i] * step;
            if (Math.abs(at[i]) > far[i]) {
                at[i] = Math.signum(at[i]) * far[i];
                dir[i] = -dir[i];
            }
            float off = 0.7f * (i % 2 == 0 ? 1f : -1f) * Math.signum(dir[i]);
            x[i] = mx[i] + (alongX[i] ? at[i] : off);
            z[i] = mz[i] + (alongX[i] ? off : at[i]);
            yaw[i] = alongX[i] ? (dir[i] > 0f ? (float) (Math.PI / 2) : (float) (-Math.PI / 2)) : (dir[i] > 0f ? 0f : (float) Math.PI);
        }
    }

    /**
     * Whoever is within reach of (ax, az) off at a run away from it: a mallet flying past, a blow
     * struck, a car hit. Those sitting get up and go too.
     */
    void alarm(float ax, float az, float reach) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i] || mode[i] == FLEE) continue;
            if (dist(i, ax, az) < reach) alarm(i, ax, az);
        }
    }

    private void alarm(int i, float ax, float az) {
        // up from the seat onto their feet
        if (mode[i] == SITS) gy[i] -= i >= FIRST_LOUNGER ? LOUNGE_SEAT : SEAT;
        mode[i] = FLEE;
        fromX[i] = ax;
        fromZ[i] = az;
        fleeFor[i] = 0f;
        // one in five turns and stares first, as at whoever did it
        stare[i] = rnd.nextInt(5) == 0 ? 0.9f + rnd.nextFloat() * 0.8f : 0f;
        runs[i] = 3.8f + rnd.nextFloat() * 1.4f;
        aside[i] = 0f;
    }

    /** Off at a run, away from what frightened them and from her, round what stands in the way; gone after a while. */
    private void flee(int i, float dt, Runner r, City city) {
        fleeFor[i] += dt;
        if (fleeFor[i] < stare[i]) {
            // a stare at her, standing
            yaw[i] = (float) Math.atan2(r.x - x[i], r.z - z[i]);
            aside[i] = 1f;
            return;
        }
        aside[i] = 0f;
        float ax = x[i] - fromX[i], az = z[i] - fromZ[i], al = (float) Math.sqrt(ax * ax + az * az) + 1e-3f;
        float bx = x[i] - r.x, bz = z[i] - r.z, bl = (float) Math.sqrt(bx * bx + bz * bz) + 1e-3f;
        float wx = ax / al + 0.6f * bx / bl, wz = az / al + 0.6f * bz / bl, wl = (float) Math.sqrt(wx * wx + wz * wz) + 1e-3f;
        float want = (float) Math.atan2(wx / wl, wz / wl), d = want - yaw[i];
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        yaw[i] += Math.max(-6f * dt, Math.min(6f * dt, d));
        float step = runs[i] * Math.min(1f, (fleeFor[i] - stare[i]) * 3f) * dt;
        float[] p = {x[i] + (float) Math.sin(yaw[i]) * step, z[i] + (float) Math.cos(yaw[i]) * step};
        if (city != null && city.push(p, 0.35f, City.WALK_TOP)) yaw[i] += 1.2f * dt * (i % 2 == 0 ? 1f : -1f) * 4f;
        x[i] = p[0];
        z[i] = p[1];
        ph[i] += step * 3.2f;
        if (fleeFor[i] > FLEE_GONE || bl > FLEE_FAR) live[i] = false;
    }

    /** One put on a bench near her, not before her eyes (but anywhere at first), where no one sits yet. */
    private void seat(int i, Runner r, City city, boolean anywhere) {
        int n = city.seats(r.x, r.z, SEATED, seatBuf, 96);
        if (n == 0) return;
        for (int tries = 0; tries < 12; tries++) {
            int k = rnd.nextInt(n);
            // the squares' benches sat on more than the streets'
            if (!city.square(City.index(seatBuf[k * 3]), City.index(seatBuf[k * 3 + 1])) && rnd.nextInt(3) != 0) continue;
            float sx = seatBuf[k * 3], sz = seatBuf[k * 3 + 1];
            float dx = sx - r.x, dz = sz - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < (anywhere ? 6f : BORN_NEAR)) continue;
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if (!anywhere && (dx * fx + dz * fz) / dd > 0.7f && dd < 60f) continue;
            boolean taken = false;
            for (int o = 0; o < MOST && !taken; o++)
                if (o != i && live[o] && mode[o] == SITS && Math.abs(x[o] - sx) < 0.3f && Math.abs(z[o] - sz) < 0.3f) taken = true;
            if (taken) continue;
            looks(i);
            mode[i] = SITS;
            gy[i] = City.WALK_TOP + SEAT;
            lean[i] = 0f;
            x[i] = sx;
            z[i] = sz;
            yaw[i] = seatBuf[k * 3 + 2];
            lean[i] = 0.05f + rnd.nextFloat() * 0.2f;
            ph[i] = rnd.nextFloat() * 20f;
            live[i] = true;
            return;
        }
    }

    /** One put walking a square near her: round its fountain, or to and fro along its garden's paths. */
    private void square(int i, Runner r, City city, boolean anywhere) {
        // the nearest square within reach
        int ci = City.index(r.x), cj = City.index(r.z), si = 0, sj = 0;
        float bd = KEPT * KEPT;
        boolean found = false;
        for (int j = cj - 2; j <= cj + 2; j++)
            for (int k = ci - 2; k <= ci + 2; k++) {
                if (!city.square(k, j)) continue;
                float qx = (k + 0.5f) * City.CELL - r.x, qz = (j + 0.5f) * City.CELL - r.z, dd = qx * qx + qz * qz;
                if (dd < bd) {
                    bd = dd;
                    si = k;
                    sj = j;
                    found = true;
                }
            }
        if (!found) return;
        float cx = (si + 0.5f) * City.CELL, cz = (sj + 0.5f) * City.CELL;
        boolean garden = city.garden(si, sj);
        looks(i);
        gy[i] = City.WALK_TOP;
        mx[i] = cx;
        mz[i] = cz;
        dir[i] = rnd.nextBoolean() ? 1f : -1f;
        if (garden) {
            mode[i] = PATH;
            alongX[i] = rnd.nextBoolean();
            far[i] = 15.5f;
            at[i] = (rnd.nextFloat() * 2f - 1f) * far[i];
        } else {
            mode[i] = FOUNTAIN;
            far[i] = 10f + rnd.nextFloat() * 2.5f;
            at[i] = rnd.nextFloat() * 6.283f;
        }
        live[i] = true;
        elsewhere(i, 0f, r, dummy);
        // not in sight, close by, as it comes (but anywhere at first)
        float dx = x[i] - r.x, dz = z[i] - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
        float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
        if (!anywhere && (dd < 12f || (dx * fx + dz * fz) / Math.max(1e-3f, dd) > 0.7f && dd < 50f)) live[i] = false;
    }

    private final float[] loungeBuf = new float[2 * 40];

    /** One lying on a lounger on the beach near her, looking out to sea, where no one lies yet; none far inland. */
    private void lounge(int i, Runner r, City city, boolean anywhere) {
        if (r.z > City.SHORE * City.CELL + BEACH_NEAR) return;
        int n = city.loungers(r.x, r.z, KEPT, loungeBuf, 40);
        for (int tries = 0; tries < 10 && n > 0; tries++) {
            int k = rnd.nextInt(n);
            float lx = loungeBuf[k * 2], lz = loungeBuf[k * 2 + 1];
            float dx = lx - r.x, dz = lz - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < (anywhere ? 6f : 20f)) continue;
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if (!anywhere && (dx * fx + dz * fz) / dd > 0.7f && dd < 50f) continue;
            boolean taken = false;
            for (int o = FIRST_LOUNGER; o < FIRST_BATHER && !taken; o++)
                if (o != i && live[o] && Math.abs(x[o] - lx) < 0.3f && Math.abs(z[o] - lz) < 0.3f) taken = true;
            if (taken) continue;
            looks(i);
            mode[i] = SITS;
            gy[i] = City.SAND + LOUNGE_SEAT;
            x[i] = lx;
            z[i] = lz;
            yaw[i] = (float) Math.PI;
            // lying well back, the head turned a little
            lean[i] = 0.55f + rnd.nextFloat() * 0.25f;
            ph[i] = rnd.nextFloat() * 20f;
            live[i] = true;
            return;
        }
    }

    /**
     * One strolling along the beach near her, to and fro: most on the wet sand by the water, some
     * on the dry sand under the quay; none far inland.
     */
    private void bathe(int i, Runner r, boolean anywhere) {
        if (r.z > City.SHORE * City.CELL + BEACH_NEAR) return;
        boolean wet = rnd.nextInt(3) != 0;
        float base = City.SHORE * City.CELL;
        looks(i);
        mode[i] = PATH;
        alongX[i] = true;
        mx[i] = r.x + (rnd.nextFloat() * 2f - 1f) * 70f;
        mz[i] = base + (wet ? City.WATERLINE + 1.6f + rnd.nextFloat() * 2.4f : City.PARAPET - 3.2f - rnd.nextFloat() * 0.8f);
        gy[i] = wet ? City.WET : City.SAND;
        far[i] = 12f + rnd.nextFloat() * 14f;
        at[i] = (rnd.nextFloat() * 2f - 1f) * far[i];
        dir[i] = rnd.nextBoolean() ? 1f : -1f;
        v[i] *= 0.8f;
        live[i] = true;
        elsewhere(i, 0f, r, dummy);
        float dx = x[i] - r.x, dz = z[i] - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
        float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
        if (!anywhere && (dd < 12f || (dx * fx + dz * fz) / Math.max(1e-3f, dd) > 0.7f && dd < 50f)) live[i] = false;
    }

    private final Chase dummy = new Chase();

    /** What one looks like, and how they walk, newly drawn. */
    private void looks(int i) {
        v[i] = 1.2f + rnd.nextFloat() * 0.55f;
        ph[i] = rnd.nextFloat() * 60f;
        size[i] = rnd.nextInt(7) == 0 ? 0.7f : 0.92f + rnd.nextFloat() * 0.14f;
        top[i] = TOPS[rnd.nextInt(TOPS.length)];
        legs[i] = LEGS[rnd.nextInt(LEGS.length)];
        skin[i] = SKINS[rnd.nextInt(SKINS.length)];
        hair[i] = HAIR[rnd.nextInt(HAIR.length)];
        kind[i] = rnd.nextInt(3);
        longHair[i] = kind[i] == SKIRT || rnd.nextInt(5) == 0;
        hat[i] = rnd.nextInt(4) == 0;
        bag[i] = rnd.nextInt(3) == 0;
        model[i] = pick();
        way[i] = Stroll.way(rnd.nextLong(), FolkPack.town != null && FolkPack.town.woman(model[i] % FolkPack.town.count));
        aside[i] = 0f;
    }

    private float dist(int i, float px, float pz) {
        float dx = x[i] - px, dz = z[i] - pz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    /** Where person i stands and which way they face, from how far round their block they are. */
    private void place(int i) {
        float s = side(), w = u[i];
        int edge = (int) (w / s);
        float k = w - edge * s;
        float x0 = bi[i] * City.CELL + RING, z0 = bj[i] * City.CELL + RING;
        float px, pz, ax, az;
        // the four sides in turn, going round anticlockwise seen from above: south, east, north, west
        if (edge == 0) { px = x0 + k; pz = z0; ax = 1f; az = 0f; }
        else if (edge == 1) { px = x0 + s; pz = z0 + k; ax = 0f; az = 1f; }
        else if (edge == 2) { px = x0 + s - k; pz = z0 + s; ax = -1f; az = 0f; }
        else { px = x0; pz = z0 + s - k; ax = 0f; az = -1f; }
        ax *= dir[i];
        az *= dir[i];
        // aside: toward the houses, which are on the inner side of the ring
        float cx = bi[i] * City.CELL + City.CELL * 0.5f, cz = bj[i] * City.CELL + City.CELL * 0.5f;
        float ix = cx - px, iz = cz - pz, il = (float) Math.sqrt(ix * ix + iz * iz) + 1e-4f;
        // only the part of "inward" across the way they go
        float dot = ix * ax + iz * az;
        ix -= dot * ax;
        iz -= dot * az;
        il = (float) Math.sqrt(ix * ix + iz * iz) + 1e-4f;
        outX[i] = -ix / il;
        outZ[i] = -iz / il;
        x[i] = px + ix / il * aside[i];
        z[i] = pz + iz / il * aside[i];
        yaw[i] = (float) Math.atan2(ax, az);
    }

    /** Someone put round the runner: out of her sight, or, the first time, anywhere not too near. */
    private void spawn(int i, Runner r, boolean anywhere) {
        for (int tries = 0; tries < 20; tries++) {
            bi[i] = City.index(r.x) + rnd.nextInt(5) - 2;
            bj[i] = City.index(r.z) + rnd.nextInt(5) - 2;
            if (City.sea(bj[i]) || City.country(bj[i])) continue;
            u[i] = rnd.nextFloat() * side() * 4f;
            dir[i] = rnd.nextBoolean() ? 1f : -1f;
            aside[i] = 0f;
            place(i);
            float dx = x[i] - r.x, dz = z[i] - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < (anywhere ? 8f : BORN_NEAR) || dd > BORN_FAR) continue;
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if (!anywhere && (dx * fx + dz * fz) / dd > 0.7f && dd < 60f) continue;
            looks(i);
            gy[i] = City.WALK_TOP;
            ox[i] = oz[i] = 0f;
            dodgeFor[i] = 0f;
            mode[i] = ROUND;
            live[i] = true;
            return;
        }
        live[i] = false;
    }

    /** Whether person i is about, walking (a dog on a lead keeps to someone so). */
    boolean walking(int i) {
        return live[i];
    }

    /** Someone about, at random, not walking a dog already; -1 if none is found. */
    int free(Random r, Pets pets) {
        for (int tries = 0; tries < 8; tries++) {
            int i = r.nextInt(ROUNDERS);
            if (live[i] && size[i] > 0.8f && !pets.leads(i)) return i;
        }
        return -1;
    }

    /** Which of the town's meshes a newcomer is: now and then the police, very seldom the dancer. */
    private int pick() {
        FolkPack t = FolkPack.town;
        if (t == null) return 0;
        int roll = rnd.nextInt(100);
        boolean police = roll < 6, dancer = roll == 99;
        for (int tries = 0; tries < 64; tries++) {
            int m = rnd.nextInt(t.count), k = t.kind[m];
            boolean isPolice = k == FolkPack.POLICEMAN || k == FolkPack.POLICEWOMAN, isDancer = k == FolkPack.DANCER;
            if (isPolice == police && isDancer == dancer) return m;
        }
        return rnd.nextInt(t.count);
    }

    /** Pushes a round body of radius rr out of anyone it went into; true if it touched someone. */
    boolean push(float[] p, float r, float lift) {
        return lift > 1.6f ? false : push(p, r);
    }

    boolean push(float[] p, float rr) {
        boolean hit = false;
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            float dx = p[0] - x[i], dz = p[1] - z[i], dd = dx * dx + dz * dz, m = rr + 0.3f;
            if (dd >= m * m || dd < 1e-8f) continue;
            float l = (float) Math.sqrt(dd);
            p[0] = x[i] + dx / l * m;
            p[1] = z[i] + dz / l * m;
            hit = true;
        }
        return hit;
    }

    /** One of them, as dressed, drawn somewhere else (in a set of its own): at (x, y, z), walking. */
    void figure(DrawList d, int i, float x, float y, float z, float yaw, float phase) {
        person(d, x, y, z, yaw, phase, i, false);
    }

    /** The same, standing still where it is. */
    void standing(DrawList d, int i, float x, float y, float z, float yaw) {
        person(d, x, y, z, yaw, 0f, i, true);
    }

    void draw(DrawList d, float ex, float ez, float fx, float fz) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            float dx = x[i] - ex, dz = z[i] - ez;
            if (dx * fx + dz * fz < -2f) continue;
            if (mode[i] == SITS) {
                if (FolkPack.town == null || !Frida.ready()) continue;
                int m = model[i] % FolkPack.town.count;
                // the hips on the seat
                float y = gy[i] - HIPS * FolkPack.town.size[m];
                FolkPack.sitter(d, m, x[i], y, z[i], yaw[i] + (float) Math.PI, 1f, lean[i], look[i]);
                continue;
            }
            person(d, x[i], gy[i], z[i], yaw[i], ph[i] + i, i, aside[i] > 0.3f);
        }
    }

    // the person being drawn: where, turned how (forward along local +z), how big
    private float px, py, pz, c, sn, s;
    private final float[] rot = new float[9], a = new float[3], b = new float[3];

    private void person(DrawList d, float x, float y, float z, float yaw, float phase, int i, boolean still) {
        if (FolkPack.town != null && Frida.ready() && way[i] != null) {
            // the skeleton faces -z at no turn: turned half round, they walk the way they are headed;
            // the stride follows the metres walked, so the feet do not slide
            FolkPack.walker(d, model[i] % FolkPack.town.count, x, y, z, yaw + (float) Math.PI, 1f, way[i],
                    phase / 3.2f, still ? 0f : 1f);
            return;
        }
        px = x;
        py = y;
        pz = z;
        c = (float) Math.cos(yaw);
        sn = (float) Math.sin(yaw);
        s = size[i];
        float sw = still ? 0f : (float) Math.sin(phase);
        float step = sw * 0.28f, bob = Math.abs(sw) * 0.025f;
        float hip = 0.9f + bob, sh = 1.4f + bob;
        int sk = skin[i], tp = top[i], lg = legs[i];
        for (int side = -1; side <= 1; side += 2) {
            float fz = step * side, lx = side * 0.085f;
            if (kind[i] == SHORTS) {
                limb(d, lx, hip, 0f, lx, hip * 0.55f, fz * 0.45f, 0.08f, lg);
                limb(d, lx, hip * 0.55f, fz * 0.45f, lx, 0.08f, fz, 0.055f, sk);
            } else if (kind[i] == TROUSERS) {
                limb(d, lx, hip, 0f, lx, hip * 0.5f, fz * 0.45f, 0.082f, lg);
                limb(d, lx, hip * 0.5f, fz * 0.45f, lx, 0.08f, fz, 0.07f, lg);
            } else {
                limb(d, lx, hip * 0.6f, fz * 0.4f, lx, 0.08f, fz, 0.05f, sk);
            }
            ball(d, lx, 0.055f, fz + 0.05f, 0.065f, 0.05f, 0.1f, SHOE);
        }
        if (kind[i] == SKIRT) ball(d, 0f, hip * 0.8f, 0f, 0.24f, hip * 0.24f, 0.2f, lg);
        ball(d, 0f, (hip + sh) * 0.5f, 0f, 0.19f, (sh - hip) * 0.5f + 0.05f, 0.14f, tp);
        if (bag[i]) ball(d, ((i & 1) == 0 ? 1f : -1f) * 0.2f, hip + 0.05f, 0.02f, 0.06f, 0.12f, 0.13f, BAG);
        for (int side = -1; side <= 1; side += 2) {
            float arm = side * 0.22f, hz = -step * side * 0.8f;
            limb(d, arm, sh - 0.06f, 0f, arm * 1.1f, hip + 0.14f, hz * 0.5f, 0.06f, tp);
            limb(d, arm * 1.1f, hip + 0.14f, hz * 0.5f, arm * 1.12f, hip - 0.1f, hz, 0.048f, sk);
        }
        limb(d, 0f, sh - 0.04f, 0f, 0f, sh + 0.08f, 0f, 0.055f, sk);
        float hy = sh + 0.21f;
        ball(d, 0f, hy, 0.01f, 0.11f, 0.125f, 0.115f, sk);
        ball(d, 0f, hy + 0.045f, -0.035f, 0.118f, 0.11f, 0.118f, hair[i]);
        if (longHair[i]) limb(d, 0f, hy, -0.07f, 0f, hy - 0.26f, -0.09f, 0.1f, hair[i]);
        if (hat[i]) {
            ball(d, 0f, hy + 0.1f, 0f, 0.22f, 0.018f, 0.22f, STRAW);
            ball(d, 0f, hy + 0.15f, 0f, 0.11f, 0.07f, 0.11f, STRAW);
        }
    }

    private void ball(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int rgb) {
        rot[0] = c; rot[1] = 0f; rot[2] = sn;
        rot[3] = 0f; rot[4] = 1f; rot[5] = 0f;
        rot[6] = -sn; rot[7] = 0f; rot[8] = c;
        lx *= s;
        ly *= s;
        lz *= s;
        d.ellipsoid(px + c * lx + sn * lz, py + ly, pz - sn * lx + c * lz, hx * s, hy * s, hz * s, rot, rgb, DrawList.MATTE);
    }

    private void limb(DrawList d, float x0, float y0, float z0, float x1, float y1, float z1, float r, int rgb) {
        world(x0, y0, z0, a);
        world(x1, y1, z1, b);
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float len = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (len < 1e-4f) return;
        ux /= len;
        uy /= len;
        uz /= len;
        float rx = c, ry = 0f, rz = -sn;
        float dot = rx * ux + ry * uy + rz * uz;
        rx -= dot * ux;
        ry -= dot * uy;
        rz -= dot * uz;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx /= rl;
        ry /= rl;
        rz /= rl;
        float fx = ry * uz - rz * uy, fy = rz * ux - rx * uz, fz = rx * uy - ry * ux;
        rot[0] = rx; rot[1] = ux; rot[2] = fx;
        rot[3] = ry; rot[4] = uy; rot[5] = fy;
        rot[6] = rz; rot[7] = uz; rot[8] = fz;
        float rr = r * s;
        d.ellipsoid((a[0] + b[0]) * 0.5f, (a[1] + b[1]) * 0.5f, (a[2] + b[2]) * 0.5f, rr, len * 0.5f + rr * 0.6f, rr, rot, rgb,
                DrawList.MATTE);
    }

    private void world(float lx, float ly, float lz, float[] out) {
        lx *= s;
        ly *= s;
        lz *= s;
        out[0] = px + c * lx + sn * lz;
        out[1] = py + ly;
        out[2] = pz - sn * lx + c * lz;
    }
}
