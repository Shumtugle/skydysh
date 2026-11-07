package com.shumtugle.skydysh;

import java.util.ArrayList;
import java.util.Random;

/**
 * The rockfall in the pass, and a game made of it. On the way out to the village, short of the
 * pass the eye leaves her: from high and far off it comes down on the pickup as it runs up the
 * road; the pickup stops short, she gets out, and down the slopes on either side of the pass come
 * logs, stumps, stones and lumps of clay, piling up across the road into a heap built like a
 * pyramid, row on row, each piece on the two under it, nothing under the lowest but the road.
 * From a line so far short of it she throws the mallet at it till it is broken: most of its
 * weight knocked off where it lay. Then the pickup and on, or another heap down the slopes, as
 * many as she likes. Each heap comes of a seed: a new one every time.
 */
final class Rockfall implements Physics.Listener {
    // the phases: not yet; the pickup brought in under the eye; stopped; getting out; the heap
    // coming down; throwing; gone on (the pieces lie where they lie)
    static final int WAIT = 0, ARRIVE = 1, HALT = 2, OUT = 3, FALL = 4, PLAY = 5, LEFT = 6;
    // what a step brought, for the world to do: nothing; she is to get out; she is to stand and
    // throw; the heap is broken
    static final int NONE = 0, GET_OUT = 1, READY = 2, BROKE = 3;

    // the heap so far beyond the line she throws from; the pickup stopped so far short of the line
    static final float THROW = 28f, STOP_BACK = 6f;
    // the eye takes the pickup over so far short of where it stops; how fast it is brought in,
    // and how hard it stops
    static final float FROM = 62f, CRUISE = 15f, BRAKE = 17f;
    // how long it stands before she gets out; how long after that the slopes start to come down
    static final float HALT_TIME = 0.9f, SLIDE_AFTER = 0.7f;
    // the pieces coming down: one after another so far apart, each so long in the air
    static final float FALL_EACH = 0.1f, FALL_FLIGHT = 0.95f;
    // the mallet: how fast it leaves her hand, how much heavier than its size, how long it may be
    // out before it is called back, how long it takes coming back
    static final float SPEED = 20f, HEAVY = 4f, OUT_MOST = 1.6f, BACK_TIME = 0.6f;
    // broken: so much of the weight moved so far from where it lay, or nothing left standing over the lowest row
    static final float CLEAR = 0.55f, MOVED = 0.6f;
    // the eye closing in on the heap while she aims and the mallet is out: so much narrower, so quickly
    static final float LENS = 2f, ZOOM_T = 0.3f;
    // a slot of the heap, across: each piece in one, a beam across two
    static final float SLOT = 1.05f;
    // the kinds of piece
    static final int LOG = 0, STUMP = 1, STONE = 2, CLAY = 3, BEAM = 4;

    /** Where the heap stands; the line she throws from; where she stands on it. */
    final float px, pz, tz, standX;
    int phase = WAIT;
    /** Seconds into the phase. */
    float t;
    /** Heaps broken, throws at this one, throws in all. */
    int broken, throwsHere, throwsAll;
    /** Whether this heap is broken. */
    boolean down;
    /** The heap now, its seed's count. */
    int figure;
    /** Whether she came on foot (no pickup brought in). */
    boolean onFoot;

    final Physics world = new Physics();
    private final ArrayList<Piece> pieces = new ArrayList<Piece>();
    private final ArrayList<Piece> old = new ArrayList<Piece>();
    private float oldT;
    private final Body mallet, car;
    private Pickup pickup;
    private World.Host host;
    private int seed = 0x5EED;

    /** One piece of the heap: its body, kind, colours; where it came down from, and when. */
    private static final class Piece {
        Body b;
        int kind, color, cap;
        float sx, sy, sz, ax, ay, az, angle, at, shown = 1f;
        boolean landed;
    }

    Rockfall() {
        pz = City.PASS_Z;
        px = City.routeX(pz);
        tz = pz - THROW;
        standX = px - 0.6f;
        mallet = new Body(Mallet.HEAD_HALF_X * Toss.SCALE, Mallet.HEAD_HALF_Y * Toss.SCALE, Mallet.HEAD_HALF_Z * Toss.SCALE, Body.HAMMER, 0xE2B270);
        mallet.scaleMass(HEAVY);
        mallet.ghost = true;
        car = new Body(Pickup.HALF_WIDE, Pickup.TOP * 0.5f, Pickup.HALF_LONG, Body.CAR, 0);
        car.ghost = true;
        world.listener = this;
        rebuild();
    }

    /** The physics afresh: the rock faces of the pass, the pickup, the mallet, the pieces. */
    private void rebuild() {
        world.clear();
        for (int e = -1; e <= 1; e += 2) {
            Body face = new Body(4f, 10f, 30f, Body.PLINTH, 0);
            face.setPose(px + e * (City.CLIFF_FOOT + 4f), 10f, pz, 1f, 0f, 0f, 0f);
            world.add(face);
        }
        world.add(car);
        world.add(mallet);
        for (Piece p : pieces) world.add(p.b);
    }

    // ---- the heap ----

    /** A new heap from the next seed: built where it will stand, each piece put back up a slope to come down. */
    private void build() {
        figure++;
        Random rnd = new Random(seed * 7919L + figure * 104729L);
        pieces.clear();
        int base = 5 + rnd.nextInt(3);
        float y = 0f;
        lowest = -1f;
        for (int row = 0; row < base; row++) {
            int n = base - row;
            int kind = rnd.nextInt(5);
            if (row == 0 && kind == BEAM) kind = STONE;
            // the row's height: every piece in it as tall, so each lies flat on the two under it
            float hy = kind == STUMP ? 0.42f + rnd.nextFloat() * 0.08f
                    : kind == STONE ? 0.36f + rnd.nextFloat() * 0.06f
                    : kind == CLAY ? 0.31f + rnd.nextFloat() * 0.05f
                    : 0.3f + rnd.nextFloat() * 0.06f;
            if (kind == BEAM && n >= 2) {
                // logs lying across, each over two slots; one left over at an end, a stone
                int beams = n / 2;
                boolean leftOver = n % 2 == 1, atLeft = rnd.nextBoolean();
                int first = leftOver && atLeft ? 1 : 0;
                for (int k = 0; k < beams; k++) {
                    float cx = slotX(first + k * 2, n) + SLOT * 0.5f;
                    add(rnd, BEAM, cx, y + hy, hy, row);
                }
                if (leftOver) add(rnd, STONE, slotX(atLeft ? 0 : n - 1, n), y + hy, hy, row);
            } else {
                if (kind == BEAM) kind = LOG;
                for (int i = 0; i < n; i++) {
                    // mostly the row's own kind; now and then another as tall
                    int k = rnd.nextFloat() < 0.7f ? kind : rnd.nextInt(4);
                    add(rnd, k, slotX(i, n), y + hy, hy, row);
                }
            }
            y += hy * 2f;
            if (lowest < 0f) lowest = y;
        }
        // down the slopes in order: the lowest row first, across it at random
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            p.at = i * FALL_EACH + rnd.nextFloat() * 0.05f;
        }
        rebuild();
    }

    private float slotX(int i, int n) {
        return px + (i - (n - 1) * 0.5f) * SLOT;
    }

    /** One piece, of a kind, its middle at (x, y) across the road on the heap's line, so tall (half). */
    private void add(Random rnd, int kind, float x, float y, float hy, int row) {
        float hx, hz;
        int color, cap;
        int material = Body.WOOD;
        switch (kind) {
            case LOG:
                // end on to her: a square of bark, its sawn ends pale
                hx = hy;
                hz = 0.8f + rnd.nextFloat() * 0.35f;
                color = Street.mix(0x5E4A38, 0x75604A, rnd.nextFloat());
                cap = Street.mix(0xC9A57A, 0xDDBB8C, rnd.nextFloat());
                break;
            case BEAM:
                // a log lying across, over two slots
                hx = SLOT - 0.08f;
                hz = hy;
                color = Street.mix(0x5A4634, 0x6E5843, rnd.nextFloat());
                cap = Street.mix(0xC9A57A, 0xDDBB8C, rnd.nextFloat());
                break;
            case STUMP:
                hx = hz = Math.min(0.46f, 0.36f + rnd.nextFloat() * 0.08f);
                color = Street.mix(0x47372A, 0x584434, rnd.nextFloat());
                cap = Street.mix(0xB88E5E, 0xCCA273, rnd.nextFloat());
                break;
            case STONE:
                hx = 0.4f + rnd.nextFloat() * 0.08f;
                hz = 0.45f + rnd.nextFloat() * 0.15f;
                color = Street.mix(0x8A8174, 0xA79C8B, rnd.nextFloat());
                cap = Street.scale(color, 1.08f);
                material = Body.STONE;
                break;
            default:
                hx = 0.42f + rnd.nextFloat() * 0.06f;
                hz = 0.45f + rnd.nextFloat() * 0.1f;
                color = Street.mix(0x9E5E3A, 0xB8774A, rnd.nextFloat());
                cap = Street.scale(color, 0.88f);
                material = Body.STONE;
                break;
        }
        Piece p = new Piece();
        p.kind = kind;
        p.color = color;
        p.cap = cap;
        p.b = new Body(hx, hy, hz, material, color);
        if (kind == CLAY) p.b.scaleMass(0.8f);
        float yaw = (rnd.nextFloat() - 0.5f) * 0.1f;
        x += (rnd.nextFloat() - 0.5f) * 0.06f;
        p.b.setPose(x, y, pz + (rnd.nextFloat() - 0.5f) * 0.2f, (float) Math.cos(yaw * 0.5f), 0f, (float) Math.sin(yaw * 0.5f), 0f);
        p.b.homeX = p.b.px;
        p.b.homeY = p.b.py;
        p.b.homeZ = p.b.pz;
        p.b.homeQw = p.b.qw;
        p.b.homeQy = p.b.qy;
        p.b.ghost = true;
        // up the slope on its own side of the road, high: it comes down tumbling
        float side = x < px ? -1f : 1f;
        if (rnd.nextFloat() < 0.25f) side = -side;
        p.sx = px + side * (City.CLIFF_FOOT + 1f + rnd.nextFloat() * 3f);
        p.sy = 10f + rnd.nextFloat() * 12f + row * 0.6f;
        p.sz = pz + (rnd.nextFloat() - 0.5f) * 12f;
        float ax = rnd.nextFloat() - 0.5f, ay = rnd.nextFloat() - 0.5f, az = rnd.nextFloat() - 0.5f, al = (float) Math.sqrt(ax * ax + ay * ay + az * az) + 1e-4f;
        p.ax = ax / al;
        p.ay = ay / al;
        p.az = az / al;
        p.angle = (float) Math.PI * (2f + rnd.nextFloat() * 2f);
        pieces.add(p);
    }

    /** The heap coming down: each piece on its way from its slope, or landed and solid. */
    private boolean coming(float dt) {
        boolean all = true;
        for (Piece p : pieces) {
            if (p.landed) continue;
            float u = (t - p.at) / FALL_FLIGHT;
            Body b = p.b;
            if (u < 0f) {
                all = false;
                continue;
            }
            if (u >= 1f) {
                b.setPose(b.homeX, b.homeY, b.homeZ, b.homeQw, 0f, b.homeQy, 0f);
                b.ghost = false;
                b.asleep = true;
                b.savePrev();
                p.landed = true;
                if (host != null) {
                    host.sound(p.b.material == Body.STONE ? Synth.STONE : Synth.WOOD, 0.35f + 0.25f * Math.min(1f, b.mass / 900f), 0.6f + (p.at * 7.3f % 1f) * 0.4f);
                    host.buzz(10);
                }
                continue;
            }
            all = false;
            // falling: down from the slope toward where it lies, faster as it comes, turning over
            float e = u * u;
            float x = p.sx + (b.homeX - p.sx) * u, z = p.sz + (b.homeZ - p.sz) * u;
            float y = p.sy + (b.homeY - p.sy) * e + (float) Math.sin(Math.PI * u) * 1.2f;
            float a = (1f - u) * p.angle * 0.5f, s = (float) Math.sin(a);
            // home turned by what is left of the tumble: q = home x spin
            float hw = b.homeQw, hy = b.homeQy, sw = (float) Math.cos(a), sx = p.ax * s, sy = p.ay * s, sz = p.az * s;
            float qw = hw * sw - hy * sy, qx = hw * sx + hy * sz, qy = hw * sy + hy * sw, qz = hw * sz - hy * sx;
            b.setPose(x, y, z, qw, qx, qy, qz);
        }
        return all;
    }

    /** How high the lowest row comes. */
    private float lowest = 1f;

    /** Whether nothing stands over the lowest row any more: knocked flat. */
    boolean flat() {
        for (Piece p : pieces) if (!p.landed || p.b.py > lowest + 0.4f) return false;
        return true;
    }

    /** The share of the heap's weight moved off where it lay. */
    float moved() {
        float m = 0f, all = 0f;
        for (Piece p : pieces) {
            Body b = p.b;
            all += b.mass;
            if (!p.landed) continue;
            float dx = b.px - b.homeX, dy = b.py - b.homeY, dz = b.pz - b.homeZ;
            if (dx * dx + dy * dy + dz * dz > MOVED * MOVED) m += b.mass;
        }
        return all > 0f ? m / all : 0f;
    }

    // ---- the phases ----

    /** Whether it has her now: the eye its own, the fingers its own. */
    boolean holds() {
        return phase >= ARRIVE && phase <= PLAY;
    }

    /** Whether the pickup is its own to bring in (not the driver's). */
    boolean drives() {
        return phase == ARRIVE || phase == HALT;
    }

    /** Whether she is at the line, the mallet hers to throw. */
    boolean playing() {
        return phase == PLAY;
    }

    /** Whether the game has gone past the first heap: the mission's clock stands meanwhile. */
    boolean extra() {
        return holds() && (broken > 0 || figure > 1);
    }

    /** The heap, the mallet and the pickup a step on; what it brought. */
    int update(float dt, Pickup driving, Runner r, World.Host h) {
        host = h;
        t += dt;
        if (old.size() > 0 && (oldT += dt) > 0.5f) old.clear();
        int got = NONE;
        switch (phase) {
            case WAIT:
                if (driving != null && driving.z > tz - STOP_BACK - FROM && driving.z < pz) {
                    pickup = driving;
                    arrive(driving);
                } else if (driving == null && r.z > tz - 6f && r.z < pz) {
                    // on foot: no pickup to bring in; the slopes come down as she comes up
                    onFoot = true;
                    to(OUT);
                    t = SLIDE_AFTER;
                    build();
                    eyeFrom(r.eyeX, r.eyeY, r.eyeZ, r.eyeX + (float) Math.sin(r.eyeYaw), r.eyeY + (float) Math.sin(r.eyePitch), r.eyeZ + (float) Math.cos(r.eyeYaw));
                    if (host != null) host.sound(Synth.GROUND, 0.7f, 0.5f);
                    got = GET_OUT;
                }
                break;
            case HALT:
                if (t >= HALT_TIME) {
                    to(OUT);
                    build();
                    snapshotEye();
                    got = GET_OUT;
                }
                break;
            case OUT:
                if (t >= SLIDE_AFTER) {
                    to(FALL);
                    if (host != null) {
                        host.sound(Synth.GROUND, 0.8f, 0.45f);
                        host.sound(Synth.ROLL, 0.5f, 0.6f);
                        host.buzz(60);
                    }
                }
                break;
            case FALL:
                if (coming(dt) && t > 0.4f) {
                    to(PLAY);
                    snapshotEye();
                    throwsHere = 0;
                    down = false;
                    got = READY;
                }
                break;
            case PLAY:
                if (!down && (moved() >= CLEAR || flat())) {
                    down = true;
                    broken++;
                    got = BROKE;
                }
                break;
            default:
                break;
        }
        hammer(dt, r);
        boolean close = phase == PLAY && (aiming || pending || hs != HAND);
        zoom = Math.max(0f, Math.min(1f, zoom + (close ? dt : -dt) / ZOOM_T));
        placeCar();
        if (phase >= FALL && (phase != LEFT || near(r, driving))) {
            world.step(Math.min(dt, 1f / 30f));
            tame();
        }
        if (holds()) eye(dt, r);
        return got;
    }

    private boolean near(Runner r, Pickup driving) {
        float x = driving != null ? driving.x : r.x, z = driving != null ? driving.z : r.z;
        return Math.abs(x - px) < 60f && Math.abs(z - pz) < 90f;
    }

    /** Nothing flies off faster than so much: a shove by the pickup, put right all at once, is no shot. */
    private void tame() {
        for (Piece p : pieces) {
            Body b = p.b;
            float s2 = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz;
            if (s2 > 22f * 22f) {
                float k = 22f / (float) Math.sqrt(s2);
                b.vx *= k;
                b.vy *= k;
                b.vz *= k;
            }
        }
    }

    private void to(int ph) {
        phase = ph;
        t = 0f;
    }

    /** Another heap down the slopes, the last one gone. */
    void again() {
        old.clear();
        old.addAll(pieces);
        oldT = 0f;
        for (Piece p : old) p.shown = 1f;
        build();
        to(FALL);
        down = false;
        throwsHere = 0;
        if (host != null) host.sound(Synth.GROUND, 0.7f, 0.5f);
    }

    /** She goes on: the pickup hers again, the pieces left lying where they are. */
    void leave() {
        to(LEFT);
        aiming = false;
        if (hs != HAND) {
            hs = HAND;
            mallet.ghost = true;
        }
    }

    // ---- the pickup brought in ----

    private float carX0, carZ0, carH0, carZ, carV, carZStop, carT;

    private void arrive(Pickup p) {
        to(ARRIVE);
        carX0 = p.x;
        carZ0 = p.z;
        carH0 = p.heading;
        carZ = p.z;
        carV = Math.max(9f, Math.min(22f, Math.abs(p.speed)));
        carZStop = tz - STOP_BACK;
        carT = 0f;
    }

    /** The pickup a step on under its own hand: up the road, slowing hard, stopped short of the line. */
    void carry(Pickup p, float dt) {
        if (phase == ARRIVE) {
            carT += dt;
            float left = carZStop - carZ;
            if (left <= carV * carV / (2f * BRAKE) + 0.05f) carV = Math.max(0f, carV - BRAKE * dt);
            else carV += (CRUISE - carV) * Math.min(1f, dt * 1.5f);
            carZ = Math.min(carZStop, carZ + carV * dt);
            if (carZ >= carZStop - 0.02f || carV <= 0f && left < 1f) {
                carZ = carZStop;
                carV = 0f;
                to(HALT);
                if (host != null) {
                    host.sound(Synth.RUBBER, 0.6f, 0.8f);
                    host.buzz(40);
                }
            }
            float k = Math.min(1f, carT / 1.6f), e = k * k * (3f - 2f * k);
            float run = (float) Math.atan2(City.routeX(carZ + 1f) - City.routeX(carZ - 1f), 2f);
            float d = run - carH0;
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            p.x = carX0 + (City.routeX(carZ) - carX0) * e;
            p.z = carZ;
            p.heading = carH0 + d * e;
            p.speed = carV;
        } else {
            p.speed = 0f;
        }
        p.steer = 0f;
        p.y = 0f;
    }

    /** The pickup in the physics, where it is now: what it runs into, it shoves. */
    private void placeCar() {
        Pickup p = pickup;
        if (p == null) {
            car.ghost = true;
            return;
        }
        car.ghost = false;
        float a = p.heading * 0.5f;
        car.setPose(p.x, Pickup.TOP * 0.5f, p.z, (float) Math.cos(a), 0f, (float) Math.sin(a), 0f);
        car.savePrev();
    }

    /** The pickup she has, the one brought in (or null on foot). */
    Pickup pickup() {
        return pickup;
    }

    // ---- the eye ----

    /** The eye now: where, looking where; and where it set off from in a glide. */
    final float[] eye = new float[5];
    private float ex, ey, ez, lx, ly, lz, ex0, ey0, ez0, lx0, ly0, lz0, glide = 1f;
    private float tx, ty, tzz, ux, uy, uz;

    private void eyeFrom(float x, float y, float z, float a, float b, float c) {
        ex = x;
        ey = y;
        ez = z;
        lx = a;
        ly = b;
        lz = c;
        snapshotEye();
    }

    private void snapshotEye() {
        ex0 = ex;
        ey0 = ey;
        ez0 = ez;
        lx0 = lx;
        ly0 = ly;
        lz0 = lz;
        glide = 0f;
    }

    // the eye at the line: so far to her right and behind her, so high; looking at so high on the heap
    static final float PLAY_RIGHT = 1.5f, PLAY_BACK = 4.6f, PLAY_UP = 3.1f, PLAY_LOOK = 1.9f;

    private void eye(float dt, Runner r) {
        Pickup p = pickup;
        boolean glides = false;
        float dur = 1f;
        switch (phase) {
            case ARRIVE: {
                // from high and far off, behind and to the side, down onto the pickup
                float k = Math.min(1f, Math.max(0f, (carZ - carZ0) / Math.max(1f, carZStop - carZ0)));
                float e = k * k * (3f - 2f * k);
                float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), rx = fz, rz = -fx;
                float back = 75f + (13f - 75f) * e, up = 50f + (6.5f - 50f) * e, side = 35f + (3f - 35f) * e;
                tx = p.x - fx * back + rx * side;
                ty = up;
                tzz = p.z - fz * back + rz * side;
                float ahead = 6f + 8f * e;
                ux = p.x + fx * ahead;
                uy = 1f;
                uz = p.z + fz * ahead;
                break;
            }
            case HALT:
            case OUT:
                if (p == null) {
                    fallEye();
                    glides = true;
                    break;
                }
                closeEye(p);
                break;
            case FALL:
                if (figure > 1) {
                    playEye();
                    glides = true;
                    break;
                }
                // behind her side of the pickup a moment yet, her out of it; then back behind the
                // line, high, the slopes coming down on either side
                if (p != null && t < FALL_STAY) {
                    closeEye(p);
                    snapshotEye();
                    break;
                }
                fallEye();
                glides = true;
                dur = 2.2f;
                break;
            default:
                playEye();
                glides = true;
                dur = 1.1f;
                break;
        }
        if (glides && glide < 1f) {
            glide = Math.min(1f, glide + dt / dur);
            float e = glide * glide * (3f - 2f * glide);
            ex = ex0 + (tx - ex0) * e;
            ey = ey0 + (ty - ey0) * e;
            ez = ez0 + (tzz - ez0) * e;
            lx = lx0 + (ux - lx0) * e;
            ly = ly0 + (uy - ly0) * e;
            lz = lz0 + (uz - lz0) * e;
        } else {
            ex = tx;
            ey = ty;
            ez = tzz;
            lx = ux;
            ly = uy;
            lz = uz;
        }
        float dx = lx - ex, dy = ly - ey, dz = lz - ez;
        eye[0] = ex;
        eye[1] = ey;
        eye[2] = ez;
        eye[3] = (float) Math.atan2(dx, dz);
        eye[4] = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
    }

    // after the slopes start to come down, the eye stays by the pickup so long
    static final float FALL_STAY = 0.9f;

    /** Close behind the pickup and off to her side of it, a little up, looking on at the pass. */
    private void closeEye(Pickup p) {
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), rx = fz, rz = -fx;
        tx = p.x - fx * 7f - rx * 2.2f;
        ty = 3.3f;
        tzz = p.z - fz * 7f - rz * 2.2f;
        ux = px;
        uy = 3.5f;
        uz = pz;
    }

    /** Well back behind the line and up: the road, the pass, the slopes on either side. */
    private void fallEye() {
        tx = standX + 1.2f;
        ty = 5.2f;
        tzz = tz - 13f;
        ux = px;
        uy = 4.5f;
        uz = pz;
    }

    private void playEye() {
        tx = standX + PLAY_RIGHT;
        ty = PLAY_UP;
        tzz = tz - PLAY_BACK;
        ux = px;
        uy = PLAY_LOOK;
        uz = pz;
    }

    /** Which way she faces at the line: at the heap. */
    float facing() {
        return (float) Math.atan2(px - standX, pz - tz);
    }

    // ---- the mallet ----

    // where it is: in her hand, on its way, coming back
    static final int HAND = 0, FLY = 1, BACK = 2;
    int hs = HAND;
    private float hsT, flight, retX, retY, retZ;
    /** A throw asked for, to be let go at the height of her swing; where at. */
    private boolean pending;
    private final float[] aimAt = new float[3], hand = new float[3];

    /** Whether the mallet is out of her hand (the one she holds not to be drawn). */
    boolean away() {
        return hs != HAND;
    }

    /** Whether it is hers to throw now. */
    boolean ready() {
        return phase == PLAY && hs == HAND && !pending;
    }

    private void hammer(float dt, Runner r) {
        hsT += dt;
        if (pending && r.throwing() >= Toss.RELEASE) {
            pending = false;
            launch(r);
        } else if (pending && r.throwing() < 0f && hsT > 1f) pending = false;
        if (hs == FLY) {
            Body m = mallet;
            if (hsT > flight + 0.6f && (m.speed2() < 4f || m.py < 0.4f) || hsT > flight + OUT_MOST) {
                hs = BACK;
                hsT = 0f;
                retX = m.px;
                retY = m.py;
                retZ = m.pz;
                m.ghost = true;
            }
        } else if (hs == BACK) {
            float k = Math.min(1f, hsT / BACK_TIME), e = k * k * (3f - 2f * k);
            r.bone("hand_r", hand);
            Body m = mallet;
            m.px = retX + (hand[0] - retX) * e;
            m.py = retY + (hand[1] - retY) * e + (float) Math.sin(Math.PI * k) * 1.4f;
            m.pz = retZ + (hand[2] - retZ) * e;
            m.rotateBy(0f, 0f, -14f * dt);
            m.normalize();
            m.updateR();
            if (k >= 1f) {
                hs = HAND;
                if (host != null) {
                    host.sound(Synth.CATCH, 0.7f, 1f);
                    host.buzz(20);
                }
            }
        }
    }

    /** Out of her hand at the height of the swing, to land on the point aimed at. */
    private void launch(Runner r) {
        r.bone("hand_r", hand);
        float sx = hand[0], sy = hand[1], sz = hand[2];
        float dx = aimAt[0] - sx, dy = aimAt[1] - sy, dz = aimAt[2] - sz;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float tt = Math.max(0.3f, dist / SPEED);
        float fl = Math.max(1e-3f, (float) Math.sqrt(dx * dx + dz * dz)), fx = dx / fl, fz = dz / fl;
        // the handle along the way it goes, the head ahead; it turns end over end about the head's length
        float a = (float) Math.atan2(-fz, fx) * 0.5f;
        Body m = mallet;
        m.setPose(sx, sy, sz, (float) Math.cos(a), 0f, (float) Math.sin(a), 0f);
        m.ghost = false;
        m.asleep = false;
        m.vx = dx / tt;
        m.vy = dy / tt - 0.5f * Physics.G * tt;
        m.vz = dz / tt;
        m.wx = -fz * -13f;
        m.wy = 0f;
        m.wz = fx * -13f;
        m.savePrev();
        flight = tt;
        hs = FLY;
        hsT = 0f;
        throwsHere++;
        throwsAll++;
        if (host != null) {
            host.sound(Synth.WHOOSH, 0.9f, 0.95f);
            host.buzz(12);
        }
    }

    // ---- the aim: a ring on the frame, put down on the heap, moved by the finger ----

    private float zoom;

    /** How much narrower the eye sees now: closing in while she aims and throws. */
    float lens() {
        float e = zoom * zoom * (3f - 2f * zoom);
        return 1f + (LENS - 1f) * e;
    }

    boolean aiming;
    /** The point aimed at, on the heap or the upright plane through it: the ring stays on it however the eye closes in. */
    private final float[] aimP = new float[3];
    private final float[] camEye = new float[3], camRight = new float[3], camUp = new float[3], camAhead = new float[3];
    private float camFocal = 1f;
    private int camW = 1, camH = 1;
    private final float[] scr = new float[3], dir = new float[3];

    /** The eye as the frame has just been drawn: kept, to turn the ring into a point aimed at. */
    void frame(DrawList d) {
        System.arraycopy(d.eye, 0, camEye, 0, 3);
        System.arraycopy(d.right, 0, camRight, 0, 3);
        System.arraycopy(d.up, 0, camUp, 0, 3);
        System.arraycopy(d.ahead, 0, camAhead, 0, 3);
        camFocal = d.focal;
        camW = d.w;
        camH = d.h;
    }

    private boolean project(float x, float y, float z, float[] out) {
        float rx = x - camEye[0], ry = y - camEye[1], rz = z - camEye[2];
        float a = rx * camAhead[0] + ry * camAhead[1] + rz * camAhead[2];
        if (a < 0.1f) return false;
        float u = rx * camRight[0] + ry * camRight[1] + rz * camRight[2], v = rx * camUp[0] + ry * camUp[1] + rz * camUp[2];
        out[0] = camW * 0.5f + camFocal * u / a;
        out[1] = camH * 0.5f - camFocal * v / a;
        return true;
    }

    /** A finger down: the ring on the heap's middle (by its weight, as it is now). */
    void aimDown() {
        if (!ready()) return;
        float sx = 0f, sy = 0f, sz = 0f, m = 0f;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed) continue;
            sx += b.px * b.mass;
            sy += b.py * b.mass;
            sz += b.pz * b.mass;
            m += b.mass;
        }
        if (m > 0f) {
            aimP[0] = sx / m;
            aimP[1] = sy / m;
            aimP[2] = sz / m;
        } else {
            aimP[0] = px;
            aimP[1] = 1.5f;
            aimP[2] = pz;
        }
        aiming = true;
    }

    /** The finger moved: the ring with it, as far across the frame (as the eye sees it now). */
    void aimBy(float dx, float dy) {
        if (!aiming) return;
        float rx = aimP[0] - camEye[0], ry = aimP[1] - camEye[1], rz = aimP[2] - camEye[2];
        float a = Math.max(1f, rx * camAhead[0] + ry * camAhead[1] + rz * camAhead[2]), k = a / camFocal;
        for (int i = 0; i < 3; i++) aimP[i] += camRight[i] * dx * k - camUp[i] * dy * k;
        aimP[0] = Math.max(px - 9f, Math.min(px + 9f, aimP[0]));
        aimP[1] = Math.max(0.2f, Math.min(10f, aimP[1]));
    }

    /** Where the ring is on the frame now, into out; false if not to be seen. */
    boolean ring(float[] out) {
        return aiming && project(aimP[0], aimP[1], aimP[2], out);
    }

    /** The finger lifted: she throws at what is under the ring. */
    void aimUp(Runner r) {
        if (!aiming) return;
        aiming = false;
        if (!ready()) return;
        if (project(aimP[0], aimP[1], aimP[2], scr)) target(scr[0], scr[1], aimAt);
        else System.arraycopy(aimP, 0, aimAt, 0, 3);
        r.lock(Toss.LOCK, aimAt[0], aimAt[2]);
        pending = true;
        hsT = 0f;
    }

    /** The world point under a frame point: the nearest piece, or the upright plane across the road through the heap. */
    private void target(float sx, float sy, float[] out) {
        float u = (sx - camW * 0.5f) / camFocal, v = (camH * 0.5f - sy) / camFocal;
        for (int k = 0; k < 3; k++) dir[k] = camAhead[k] + camRight[k] * u + camUp[k] * v;
        float best = 1e9f;
        for (Piece p : pieces) {
            if (!p.landed) continue;
            float tt = rayBox(p.b, camEye[0], camEye[1], camEye[2], dir[0], dir[1], dir[2]);
            if (tt > 0f && tt < best) best = tt;
        }
        if (best > 1e8f) {
            best = Math.abs(dir[2]) > 1e-4f ? (pz - camEye[2]) / dir[2] : 30f;
            if (best <= 0f) best = 30f;
        }
        for (int k = 0; k < 3; k++) out[k] = camEye[k] + dir[k] * best;
        if (out[1] < 0.2f) out[1] = 0.2f;
    }

    static float rayBox(Body b, float ox, float oy, float oz, float dx, float dy, float dz) {
        float qx = ox - b.px, qy = oy - b.py, qz = oz - b.pz;
        float o0 = b.r00 * qx + b.r10 * qy + b.r20 * qz, o1 = b.r01 * qx + b.r11 * qy + b.r21 * qz, o2 = b.r02 * qx + b.r12 * qy + b.r22 * qz;
        float d0 = b.r00 * dx + b.r10 * dy + b.r20 * dz, d1 = b.r01 * dx + b.r11 * dy + b.r21 * dz, d2 = b.r02 * dx + b.r12 * dy + b.r22 * dz;
        float t0 = -1e9f, t1 = 1e9f;
        for (int i = 0; i < 3; i++) {
            float o = i == 0 ? o0 : i == 1 ? o1 : o2, d = i == 0 ? d0 : i == 1 ? d1 : d2, h = i == 0 ? b.hx : i == 1 ? b.hy : b.hz;
            if (Math.abs(d) < 1e-9f) {
                if (Math.abs(o) > h) return -1f;
                continue;
            }
            float a = (-h - o) / d, c = (h - o) / d;
            if (a > c) {
                float s = a;
                a = c;
                c = s;
            }
            t0 = Math.max(t0, a);
            t1 = Math.min(t1, c);
            if (t0 > t1) return -1f;
        }
        return t0 > 0f ? t0 : (t1 > 0f ? 0.001f : -1f);
    }

    // ---- what the pickup and she run into, after ----

    /** The pickup about to move on: what it is going into fast enough is shoved off ahead and aside. */
    void ram(Pickup pk, float dt) {
        if (Math.abs(pk.speed) < 2.5f) return;
        float sg = Math.signum(pk.speed), fx = (float) Math.sin(pk.heading) * sg, fz = (float) Math.cos(pk.heading) * sg;
        float v = Math.abs(pk.speed), reach = Pickup.ENDS + v * dt;
        float cx = pk.x + fx * reach, cz = pk.z + fz * reach;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed || b.py > 2.5f) continue;
            float r = Math.max(b.hx, b.hz);
            float dx = b.px - cx, dz = b.pz - cz, m = r + Pickup.END_R + 0.3f, dd = dx * dx + dz * dz;
            if (dd >= m * m) continue;
            float l = (float) Math.sqrt(dd) + 1e-4f;
            float push = Math.max(v * 0.9f, 4f), across = dx * fz - dz * fx;
            float side = Math.abs(across) > 0.2f ? Math.signum(across) : (b.id % 2 == 0 ? 1f : -1f);
            b.px += dx / l * (m - l) * 0.5f;
            b.pz += dz / l * (m - l) * 0.5f;
            b.updateR();
            world.wake(b);
            b.vx = fx * push * 0.45f + fz * side * push * 0.8f;
            b.vz = fz * push * 0.45f - fx * side * push * 0.8f;
            b.vy = 2f;
            b.wy = across / l * push * 0.3f;
            hit++;
        }
    }

    /** How many times something has been struck by the pickup or the mallet in hand. */
    int hit;

    /** A blow of the mallet on foot at (bx, bz), facing (fx, fz): what lies within reach before her is knocked off. */
    void strike(float bx, float bz, float fx, float fz) {
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed) continue;
            float dx = b.px - bx, dz = b.pz - bz, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd > Math.max(b.hx, b.hz) + 2.2f || dx * fx + dz * fz < -0.3f) continue;
            float l = dd + 1e-4f;
            world.wake(b);
            b.vx = fx * 5.4f + dx / l * 5.4f;
            b.vz = fz * 5.4f + dz / l * 5.4f;
            b.vy = 2.5f;
            hit++;
        }
    }

    /** What lies on the road, in her way and the pickup's (see City.Extra): each piece low enough, a round. */
    boolean push(float[] q, float rr, float feet) {
        boolean any = false;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed || feet > b.maxY - 0.15f || b.minY > feet + 1.6f) continue;
            float r = (Math.max(b.hx, b.hz) + Math.min(b.hx, b.hz)) * 0.5f;
            float dx = q[0] - b.px, dz = q[1] - b.pz, m = rr + r, dd = dx * dx + dz * dz;
            if (dd >= m * m || dd < 1e-8f) continue;
            float l = (float) Math.sqrt(dd);
            q[0] = b.px + dx / l * m;
            q[1] = b.pz + dz / l * m;
            any = true;
        }
        return any;
    }

    /** Whether the road through is clear: nothing left lying across it near the pass. */
    boolean cleared() {
        for (Piece p : pieces) {
            Body b = p.b;
            if (Math.abs(b.pz - pz) > 14f || b.py > 3f) continue;
            if (Math.abs(b.px - City.routeX(b.pz)) < City.ROUTE_HALF) return false;
        }
        return true;
    }

    @Override
    public void knock(Body a, Body b, float speed, float x, float y, float z) {
        if (host == null || speed < 1.2f) return;
        float v = Math.min(1f, speed / 9f);
        boolean metal = a == mallet || b == mallet;
        int s = a.material == Body.STONE || b != null && b.material == Body.STONE ? Synth.STONE : Synth.WOOD;
        host.sound(s, 0.25f + 0.55f * v, 0.7f + 0.3f * ((a.id * 37 % 10) / 10f));
        if (metal && speed > 4f) host.buzz(25);
    }

    // ---- drawing ----

    private final float[] turn = new float[9], spin = new float[9];

    void draw(DrawList d, Runner r, float eyeX, float eyeZ) {
        for (Piece p : pieces) piece(d, p, 1f);
        if (old.size() > 0) {
            float k = Math.max(0f, 1f - oldT / 0.5f);
            for (Piece p : old) piece(d, p, k);
        }
        if (hs != HAND) {
            Body m = mallet;
            rot(m, turn);
            Mallet.draw(d, turn, m.px, m.py, m.pz, Toss.SCALE, Mallet.HEAD_MIDDLE);
        }
    }

    private static void rot(Body b, float[] out) {
        out[0] = b.r00; out[1] = b.r01; out[2] = b.r02;
        out[3] = b.r10; out[4] = b.r11; out[5] = b.r12;
        out[6] = b.r20; out[7] = b.r21; out[8] = b.r22;
    }

    /** A piece where its body is, so big (shrinking away, an old heap gone). */
    private void piece(DrawList d, Piece p, float k) {
        Body b = p.b;
        // not yet on its way down: up the slope, out of sight
        if (!p.landed && (phase < FALL || t < p.at)) return;
        rot(b, turn);
        float hx = b.hx * k, hy = b.hy * k, hz = b.hz * k;
        switch (p.kind) {
            case LOG:
                // round, end on to her: bark, and its sawn ends pale with a darker heart
                round(d, b, 2, hx, hz, k, p);
                break;
            case BEAM:
                round(d, b, 0, hy, hx, k, p);
                break;
            case STUMP:
                round(d, b, 1, hx, hy, k, p);
                // a root or two out at the foot
                at(d, b, hx * 0.55f, -hy * 0.75f, 0f, hx * 0.45f, hy * 0.2f, hz * 0.3f, p.color, DrawList.WOOD);
                at(d, b, 0f, -hy * 0.75f, -hz * 0.55f, hx * 0.3f, hy * 0.2f, hz * 0.45f, p.color, DrawList.WOOD);
                break;
            case STONE: {
                d.box(b.px, b.py, b.pz, hx, hy * 0.86f, hz, turn, p.color, DrawList.STONE);
                // its corners broken off: a second block turned an eighth about the upright
                float c = 0.7071f;
                for (int i = 0; i < 9; i++) spin[i] = turn[i];
                spin[0] = turn[0] * c - turn[2] * c;
                spin[2] = turn[0] * c + turn[2] * c;
                spin[3] = turn[3] * c - turn[5] * c;
                spin[5] = turn[3] * c + turn[5] * c;
                spin[6] = turn[6] * c - turn[8] * c;
                spin[8] = turn[6] * c + turn[8] * c;
                float s = Math.min(hx, hz) * 0.7f;
                d.box(b.px, b.py, b.pz, s, hy, s, spin, p.cap, DrawList.STONE);
                break;
            }
            default:
                // clay: a lump, its top slumped and darker
                d.box(b.px, b.py, b.pz, hx, hy, hz, turn, p.color, DrawList.MATTE);
                at(d, b, 0f, hy * 0.6f, 0f, hx * 0.8f, hy * 0.45f, hz * 0.8f, p.cap, DrawList.MATTE);
                break;
        }
    }

    private final float[] band = new float[9];

    /**
     * A log or a stump: eight sides round its own axis (0, 1, 2: x, y, z), so far out, so long
     * each way; its ends sawn, pale, the heart darker. Bands crossed at an eighth of a turn.
     */
    private void round(DrawList d, Body b, int axis, float r, float len, float k, Piece p) {
        int u = axis == 0 ? 1 : 0, v = axis == 2 ? 1 : 2;
        float side = r * 0.4142f, bark = DrawList.material(DrawList.TRUNK, 40 + b.id * 23 % 160);
        for (int q = 0; q < 2; q++) {
            float c = q == 0 ? 1f : 0.7071f, s = q == 0 ? 0f : 0.7071f;
            // the frame turned about the axis: its two other columns turned so far
            for (int row = 0; row < 3; row++) {
                band[row * 3 + axis] = turn[row * 3 + axis];
                band[row * 3 + u] = turn[row * 3 + u] * c + turn[row * 3 + v] * s;
                band[row * 3 + v] = -turn[row * 3 + u] * s + turn[row * 3 + v] * c;
            }
            for (int w = 0; w < 2; w++) {
                float hu = w == 0 ? r : side, hv = w == 0 ? side : r;
                float bx = axis == 0 ? len : u == 0 ? hu : hv, by = axis == 1 ? len : u == 1 ? hu : hv, bz = axis == 2 ? len : v == 2 ? hv : hu;
                d.box(b.px, b.py, b.pz, bx, by, bz, band, p.color, bark);
                // the sawn ends, a little in from the bark
                for (int e = -1; e <= 1; e += 2) {
                    if (axis == 1 && e < 0) continue;
                    float off = e * (len + 0.012f * k);
                    float ox = band[axis] * off, oy = band[3 + axis] * off, oz = band[6 + axis] * off;
                    float cx = axis == 0 ? 0.02f * k : bx * 0.88f, cy = axis == 1 ? 0.02f * k : by * 0.88f, cz = axis == 2 ? 0.02f * k : bz * 0.88f;
                    d.box(b.px + ox, b.py + oy, b.pz + oz, cx, cy, cz, band, p.cap, DrawList.WOOD);
                }
            }
        }
        // the heart at each end
        for (int e = -1; e <= 1; e += 2) {
            if (axis == 1 && e < 0) continue;
            float off = e * (len + 0.03f * k), hr = r * 0.22f;
            float ox = turn[axis] * off, oy = turn[3 + axis] * off, oz = turn[6 + axis] * off;
            d.box(b.px + ox, b.py + oy, b.pz + oz, axis == 0 ? 0.01f : hr, axis == 1 ? 0.01f : hr, axis == 2 ? 0.01f : hr, turn,
                    Street.scale(p.cap, 0.78f), DrawList.WOOD);
        }
    }

    /** A box at (ox, oy, oz) in the piece's own frame, turned with it. */
    private void at(DrawList d, Body b, float ox, float oy, float oz, float hx, float hy, float hz, int color, int material) {
        float x = b.px + turn[0] * ox + turn[1] * oy + turn[2] * oz;
        float y = b.py + turn[3] * ox + turn[4] * oy + turn[5] * oz;
        float z = b.pz + turn[6] * ox + turn[7] * oy + turn[8] * oz;
        d.box(x, y, z, hx, hy, hz, turn, color, material);
    }

    /** The heap's top, where its button floats, into out (x, y, z). */
    void top(float[] out) {
        float top = 0f;
        for (Piece p : pieces) if (p.landed) top = Math.max(top, p.b.maxY);
        out[0] = px;
        out[1] = Math.max(2f, top) + 1.6f;
        out[2] = pz;
    }

    /** Over the frame: the ring, and the way the mallet would go from it. */
    void flat(Px f, Runner r) {
        if (!ready() || !ring(ring)) return;
        float ringX = ring[0], ringY = ring[1];
        target(ringX, ringY, aimAt);
        r.bone("hand_r", hand);
        float sx = hand[0], sy = hand[1], sz = hand[2];
        float dx = aimAt[0] - sx, dy = aimAt[1] - sy, dz = aimAt[2] - sz;
        float tt = Math.max(0.3f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / SPEED);
        float vx = dx / tt, vy = dy / tt - 0.5f * Physics.G * tt, vz = dz / tt;
        for (int i = 3; i < 20; i++) {
            float q = tt * i / 21f;
            if (project(sx + vx * q, sy + vy * q + 0.5f * Physics.G * q * q, sz + vz * q, scr)) {
                int x = Math.round(scr[0]), y = Math.round(scr[1]);
                f.rect(x - 1, y - 1, x + 1, y + 1, 0xCCFFF4D0);
            }
        }
        ring(f, ringX, ringY, 9f, 0xFFFFD45A);
        ring(f, ringX, ringY, 10.5f, 0x88232A4A);
    }

    private final float[] ring = new float[3];

    private static void ring(Px f, float cx, float cy, float r, int c) {
        int n = (int) (r * 7f);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = (int) Math.round(cx + Math.cos(a) * r), y = (int) Math.round(cy + Math.sin(a) * r);
            f.set(x, y, c);
            f.set(x + 1, y, c);
        }
    }
}
