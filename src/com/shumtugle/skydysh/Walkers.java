package com.shumtugle.skydysh;

import java.util.Random;

/**
 * People out for the evening on the pavements: each goes round a block, one way or the other,
 * along the middle of the pavement. Someone running at them makes them stop and step aside
 * toward the houses. Like the traffic, they come and go out of sight round the runner.
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
    static final float RING = 6.2f;
    // a bench's seat over the pavement; the hips over the feet on the skeleton
    static final float SEAT = 0.5f, HIPS = 0.93f;
    static final float BORN_NEAR = 28f, BORN_FAR = 110f, GONE = 140f, SHY = 3.2f, KEPT = 80f, SEATED = 65f;
    // so many go round the blocks; so many sit on the benches; so many walk about the squares
    static final int ROUNDERS = 54, SITTERS = 24, SQUARERS = 14, MOST = ROUNDERS + SITTERS + SQUARERS;
    // what one is doing: going round a block, sitting on a bench, going round a fountain, to and
    // fro along a garden's path
    static final int ROUND = 0, SITS = 1, FOUNTAIN = 2, PATH = 3;

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
    private final float[] seatBuf = new float[3 * 96];
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
                    else square(i, r, city, !started);
                }
                continue;
            }
            if (mode[i] != ROUND) {
                elsewhere(i, dt, r, ch);
                continue;
            }
            // someone running at them: they stop and step aside
            float near = Math.min(dist(i, r.x, r.z), dist(i, ch.x, ch.z));
            boolean shy = near < SHY;
            float want = shy ? 0.55f : 0f;
            aside[i] += (want - aside[i]) * Math.min(1f, dt * 5f);
            if (!shy) {
                u[i] = (u[i] + dir[i] * v[i] * dt + len) % len;
                ph[i] += dt * v[i] * 3.2f;
            }
            place(i);
        }
        started = true;
    }

    /** One sitting, or walking a square: the walk along its way, stopping short of someone running at them. */
    private void elsewhere(int i, float dt, Runner r, Chase ch) {
        if (mode[i] == SITS) {
            look[i] = 0.5f * (float) Math.sin(ph[i] += dt * 0.3f);
            return;
        }
        boolean shy = Math.min(dist(i, r.x, r.z), dist(i, ch.x, ch.z)) < SHY;
        float step = shy ? 0f : v[i] * dt;
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
            if (City.sea(bj[i])) continue;
            u[i] = rnd.nextFloat() * side() * 4f;
            dir[i] = rnd.nextBoolean() ? 1f : -1f;
            aside[i] = 0f;
            place(i);
            float dx = x[i] - r.x, dz = z[i] - r.z, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd < (anywhere ? 8f : BORN_NEAR) || dd > BORN_FAR) continue;
            float fx = (float) Math.sin(r.eyeYaw), fz = (float) Math.cos(r.eyeYaw);
            if (!anywhere && (dx * fx + dz * fz) / dd > 0.7f && dd < 60f) continue;
            looks(i);
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

    void draw(DrawList d, float ex, float ez, float fx, float fz) {
        for (int i = 0; i < MOST; i++) {
            if (!live[i]) continue;
            float dx = x[i] - ex, dz = z[i] - ez;
            if (dx * fx + dz * fz < -2f) continue;
            if (mode[i] == SITS) {
                if (FolkPack.town == null || !Frida.ready()) continue;
                int m = model[i] % FolkPack.town.count;
                // the hips on the seat
                float y = City.WALK_TOP + SEAT - HIPS * FolkPack.town.size[m];
                FolkPack.sitter(d, m, x[i], y, z[i], yaw[i] + (float) Math.PI, 1f, lean[i], look[i]);
                continue;
            }
            person(d, x[i], City.WALK_TOP, z[i], yaw[i], ph[i] + i, i, aside[i] > 0.3f);
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
