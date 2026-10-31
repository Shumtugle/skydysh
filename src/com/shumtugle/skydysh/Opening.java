package com.shumtugle.skydysh;

import java.util.Random;

/**
 * What the game opens with, some six and a half seconds; the one word in it is the game's name.
 *
 * The eye comes down out of the sky over a town by the sea, over the promenade with its palms
 * and parapet, onto a square paved in rings, houses round it, roads behind and to the right with
 * cars going both ways, people far off on the pavements. It stops before her: a giant, two and a
 * half times her own height, standing in the middle of the square with a golden mallet in her
 * right hand. Her eyes light, and a low blow of metal sounds. The town goes dark; one light is
 * left, on her, and the mallet glows of itself. She swings it back over her head, slowly, then
 * down at the eye, faster and faster, the eye stepping up to meet it; it strikes the glass of the
 * screen: thunder, the ground's boom, glass, a white flash, the frame shaking. Eight white cracks
 * run out from the blow and branch, and she falls to pieces, every block of her flung off away
 * from the blow, turning, knocking on the stones; the mallet falls, its handle and its burning
 * head apart. The eye draws back and down to see her come down; the cracks melt away. The light
 * leaves the heap and goes up to the name, which lights dot by dot from the left, each dot white
 * and then, a third of a second on, a bar of gold; a high chime. The frame goes white, and at its
 * whitest the game begins.
 *
 * A touch at any moment skips to the white.
 */
final class Opening {
    static final float LENGTH = 6.45f, WHITE_AT = 6.2f;
    static final float LAND = 2.5f, EYES = 2.5f, DARK = 3.0f, SWING = 3.6f, STRIKE = 4.05f, BLOW = 4.3f, NAME = 5.2f;
    static final float SIZE = Runner.SIZE * 2.5f, MALLET = Toss.SCALE / Runner.SIZE * SIZE;
    static final float FLOOR = 0.14f;                 // the square's stones
    static final String WORD = "SKYDYSH";

    float t;
    boolean done;
    private final World.Host host;
    private final DrawList set = new DrawList();
    private final Walkers people = new Walkers();
    private final Random rnd = new Random(0x0BE5L);
    private int heard;                                // the sounds already played, one bit each

    // the cars on the two roads: lane, where along it at the start, colour
    private static final int CARS = 14;
    private final int[] lane = new int[CARS], paint = new int[CARS];
    private final float[] start = new float[CARS];

    // her blocks once she is broken, and the mallet's two pieces
    private int bn;
    private final float[] bp = new float[600], bv = new float[600], bt = new float[1800], bh = new float[600];
    private final float[] axis = new float[600], spin = new float[200], rate = new float[200];
    private final int[] bc = new int[200], bm = new int[200];
    private final float[] piece = new float[6], pieceV = new float[6], pieceTurn = new float[18], pieceAxis = new float[6];
    private final float[] pieceSpin = new float[2], pieceRate = new float[2];
    private boolean broken;
    private float lastKnock;

    // the cracks in the glass: segments in frame pixels, x0 y0 x1 y1 and how bright
    private float[] cracks;
    private int crackN;
    private float shake;

    // the eye where the blow came, and where it draws back to
    private final float[] blowEye = new float[3], blowAt = new float[3];
    private final float[] tmp = new float[3], hand = new float[3], elbow = new float[3], turn = new float[9];
    private final float[] at = new float[3];

    Opening(World.Host host) {
        this.host = host;
        build(set, new Random(31));
        // dress the people once, as the town does
        people.update(0f, new Runner(), new Chase());
        for (int c = 0; c < CARS; c++) {
            lane[c] = c % 4;
            paint[c] = Cars.PAINT[rnd.nextInt(Cars.PAINT.length)];
            start[c] = rnd.nextFloat() * 160f;
        }
        Frida.eyes = 0f;
    }

    /** Straight to the white. */
    void skip() {
        if (t < WHITE_AT) {
            t = WHITE_AT;
            skipped = true;
        }
    }

    private boolean skipped;

    void update(float dt) {
        t += dt;
        Frida.eyes = clamp((t - EYES) / 0.4f);
        cue(0, 0f, Synth.WIND, 0.55f, 1f);
        cue(1, EYES + 0.05f, Synth.CLANG, 0.9f, 0.45f);
        cue(2, SWING, Synth.WHOOSH, 0.8f, 0.7f);
        if (t >= BLOW && !broken && !skipped) blow();
        cue(3, BLOW, Synth.THUNDER, 1f, 0.85f);
        cue(4, BLOW, Synth.GROUND, 1f, 0.8f);
        cue(5, BLOW, Synth.GLASS, 1f, 0.9f);
        cue(6, NAME, Synth.CHIME, 0.7f, 1.6f);
        if (broken) fall(dt);
        shake = Math.max(0f, shake - dt * 2f);
        if (t >= LENGTH) done = true;
    }

    /** A sound, once, when its time has come (and not if the time was skipped past). */
    private void cue(int bit, float when, int id, float gain, float pitch) {
        if ((heard & (1 << bit)) != 0 || t < when) return;
        heard |= 1 << bit;
        if (t - when < 0.25f) host.sound(id, gain, pitch);
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    private static float ease(float u) {
        u = clamp(u);
        return u * u * (3f - 2f * u);
    }

    // ---- her, and the mallet in her hand ----

    /** What she is doing and how far into it. */
    private String motion() {
        return t < SWING ? "idle" : "throw";
    }

    private float motionTime() {
        if (t < SWING) return t;
        if (t < STRIKE) {
            // back over her head, slowly, gathering
            float u = clamp((t - SWING) / (STRIKE - SWING));
            return 0.27f * u * u;
        }
        // down, faster and faster
        float u = clamp((t - STRIKE) / (BLOW - STRIKE));
        return 0.27f + 0.13f * u * (0.6f + 0.4f * u);
    }

    /** The mallet in her fist: its turn into turn, the grip into hand. */
    private void grip(String motion, float mt) {
        grip(motion, mt, clamp((t - SWING) / 0.15f));
    }

    /** The same, swung so far (0 hanging, 1 running on from her forearm). */
    private void grip(String motion, float mt, float k) {
        Frida.place("hand_r", 0f, FLOOR, 0f, 0f, SIZE, motion, mt, hand);
        Frida.place("lowerarm_r", 0f, FLOOR, 0f, 0f, SIZE, motion, mt, elbow);
        float fx = hand[0] - elbow[0], fy = hand[1] - elbow[1], fz = hand[2] - elbow[2];
        float fl = Math.max(1e-3f, (float) Math.sqrt(fx * fx + fy * fy + fz * fz));
        // at rest it hangs, its head on the stones beside her; swung, it runs on from her forearm
        float hx = 0.18f * (1f - k) + fx / fl * k, hy = -1f * (1f - k) + fy / fl * k, hz = -0.12f * (1f - k) + fz / fl * k;
        float l = Math.max(1e-3f, (float) Math.sqrt(hx * hx + hy * hy + hz * hz));
        hx /= l;
        hy /= l;
        hz /= l;
        float bx = -hz, bz = hx, bl = Math.max(1e-3f, (float) Math.sqrt(bx * bx + bz * bz));
        bx /= bl;
        bz /= bl;
        float ux = -bz * hy, uy = bz * hx - bx * hz, uz = bx * hy;
        turn[0] = hx; turn[1] = ux; turn[2] = bx;
        turn[3] = hy; turn[4] = uy; turn[5] = 0f;
        turn[6] = hz; turn[7] = uz; turn[8] = bz;
    }

    /** Where the mallet's head is, with her at a motion and time. */
    private void headAt(String motion, float mt, float[] out) {
        grip(motion, mt, motion.equals("throw") ? 1f : 0f);
        float s = Mallet.HEAD_MIDDLE * MALLET;
        out[0] = hand[0] + turn[0] * s;
        out[1] = hand[1] + turn[3] * s;
        out[2] = hand[2] + turn[6] * s;
    }

    // ---- the blow ----

    private void blow() {
        broken = true;
        shake = 1f;
        headAt("throw", 0.40f, blowAt);
        // her blocks, flung from her middle and away from the blow
        bn = Frida.capture(0f, FLOOR, 0f, 0f, SIZE, "throw", 0.40f, bp, bt, bh, bc, bm);
        float cx = 0f, cy = FLOOR + 0.95f * SIZE, cz = 0f;
        for (int i = 0; i < bn; i++) {
            float ox = bp[i * 3] - cx, oy = bp[i * 3 + 1] - cy, oz = bp[i * 3 + 2] - cz;
            float ol = Math.max(0.1f, (float) Math.sqrt(ox * ox + oy * oy + oz * oz));
            float ax = bp[i * 3] - blowAt[0], ay = bp[i * 3 + 1] - blowAt[1], az = bp[i * 3 + 2] - blowAt[2];
            float al = Math.max(0.1f, (float) Math.sqrt(ax * ax + ay * ay + az * az));
            float out = 2.5f + rnd.nextFloat() * 3f, away = 2f + rnd.nextFloat() * 3f;
            bv[i * 3] = ox / ol * out + ax / al * away;
            bv[i * 3 + 1] = oy / ol * out + ay / al * away + 2f + rnd.nextFloat() * 4f;
            bv[i * 3 + 2] = oz / ol * out + az / al * away;
            float rx = rnd.nextFloat() - 0.5f, ry = rnd.nextFloat() - 0.5f, rz = rnd.nextFloat() - 0.5f;
            float rl = Math.max(1e-3f, (float) Math.sqrt(rx * rx + ry * ry + rz * rz));
            axis[i * 3] = rx / rl;
            axis[i * 3 + 1] = ry / rl;
            axis[i * 3 + 2] = rz / rl;
            spin[i] = 0f;
            rate[i] = 3f + rnd.nextFloat() * 8f;
        }
        // the mallet: the handle and the burning head, each its own way
        grip("throw", 0.40f, 1f);
        for (int k = 0; k < 2; k++) {
            float s = (k == 0 ? Mallet.HANDLE_MIDDLE : Mallet.HEAD_MIDDLE) * MALLET;
            piece[k * 3] = hand[0] + turn[0] * s;
            piece[k * 3 + 1] = hand[1] + turn[3] * s;
            piece[k * 3 + 2] = hand[2] + turn[6] * s;
            System.arraycopy(turn, 0, pieceTurn, k * 9, 9);
            pieceV[k * 3] = (k == 0 ? -1.5f : 1.2f);
            pieceV[k * 3 + 1] = k == 0 ? 3f : 1.5f;
            pieceV[k * 3 + 2] = k == 0 ? 1.5f : -0.5f;
            pieceAxis[k * 3] = k == 0 ? 0.3f : 1f;
            pieceAxis[k * 3 + 1] = 0.2f;
            pieceAxis[k * 3 + 2] = k == 0 ? 0.93f : 0.1f;
            float l = (float) Math.sqrt(pieceAxis[k * 3] * pieceAxis[k * 3] + 0.04f + pieceAxis[k * 3 + 2] * pieceAxis[k * 3 + 2]);
            for (int j = 0; j < 3; j++) pieceAxis[k * 3 + j] /= l;
            pieceRate[k] = k == 0 ? 7f : 4f;
        }
        cracksFrom(World.W * 0.5f, 0f);
    }

    /** Her blocks and the mallet's pieces falling, bouncing on the stones, knocking as they land. */
    private void fall(float dt) {
        for (int i = 0; i < bn; i++) {
            float h = Math.min(bh[i * 3], Math.min(bh[i * 3 + 1], bh[i * 3 + 2]));
            bv[i * 3 + 1] -= 22f * dt;
            for (int k = 0; k < 3; k++) bp[i * 3 + k] += bv[i * 3 + k] * dt;
            spin[i] += rate[i] * dt;
            if (bp[i * 3 + 1] - h < FLOOR) {
                bp[i * 3 + 1] = FLOOR + h;
                float hit = -bv[i * 3 + 1];
                if (hit > 0f) {
                    bv[i * 3 + 1] = hit * 0.3f;
                    if (hit > 3f) knock(bm[i] == DrawList.WOOD ? Synth.WOOD : Synth.STONE, hit);
                }
                bv[i * 3] *= 0.6f;
                bv[i * 3 + 2] *= 0.6f;
                rate[i] *= 0.5f;
            }
        }
        for (int k = 0; k < 2; k++) {
            pieceV[k * 3 + 1] -= 22f * dt;
            for (int j = 0; j < 3; j++) piece[k * 3 + j] += pieceV[k * 3 + j] * dt;
            pieceSpin[k] += pieceRate[k] * dt;
            float h = (k == 0 ? 0.04f : 0.11f) * MALLET;
            if (piece[k * 3 + 1] - h < FLOOR) {
                piece[k * 3 + 1] = FLOOR + h;
                float hit = -pieceV[k * 3 + 1];
                if (hit > 0f) {
                    pieceV[k * 3 + 1] = hit * 0.35f;
                    if (hit > 2.5f) {
                        if (k == 1) host.sound(Synth.CLANG, Math.min(1f, hit / 10f), 1.5f);
                        else knock(Synth.WOOD, hit);
                    }
                }
                pieceV[k * 3] *= 0.5f;
                pieceV[k * 3 + 2] *= 0.5f;
                pieceRate[k] *= 0.4f;
            }
        }
    }

    /** A block landing: a knock, not too many at once. */
    private void knock(int id, float hit) {
        if (t - lastKnock < 0.035f) return;
        lastKnock = t;
        host.sound(id, Math.min(0.8f, hit / 14f), 0.8f + rnd.nextFloat() * 0.4f);
    }

    /** Eight cracks out from (x, y) of the frame (y from the middle), each wandering a little and branching. */
    private void cracksFrom(float x, float y0) {
        cracks = new float[5 * 400];
        crackN = 0;
        float cy = y0;
        for (int r = 0; r < 8; r++) {
            float a = r * (float) Math.PI / 4f + (rnd.nextFloat() - 0.5f) * 0.5f;
            crack(x, cy, a, 0.9f + rnd.nextFloat() * 0.5f, 1f, 2);
        }
    }

    private void crack(float x, float y, float a, float reach, float bright, int depth) {
        float len = 0f, most = reach * 520f;
        while (len < most && crackN < 400) {
            float step = 10f + rnd.nextFloat() * 18f;
            a += (rnd.nextFloat() - 0.5f) * 0.45f;
            float nx = x + (float) Math.cos(a) * step, ny = y + (float) Math.sin(a) * step;
            int o = crackN++ * 5;
            cracks[o] = x;
            cracks[o + 1] = y;
            cracks[o + 2] = nx;
            cracks[o + 3] = ny;
            cracks[o + 4] = bright;
            x = nx;
            y = ny;
            len += step;
            if (depth > 0 && rnd.nextFloat() < 0.18f)
                crack(x, y, a + (rnd.nextBoolean() ? 1f : -1f) * (0.45f + rnd.nextFloat() * 0.45f),
                        reach * (0.25f + rnd.nextFloat() * 0.25f), bright * 0.8f, depth - 1);
        }
    }

    // ---- the eye ----

    /** Where the eye is and what it looks at, into eye (x y z) and look (x y z). */
    private void eye(float[] eye, float[] look) {
        // down out of the sky in an arc, over the sea and the promenade, onto the square before her
        float u = ease(t / LAND);
        float a = 1f - u;
        float[] p0 = {-45f, 62f, -125f}, p1 = {-12f, 34f, -70f}, p2 = {0f, 8f, -30f}, p3 = {0f, 3.3f, -10.5f};
        for (int k = 0; k < 3; k++)
            eye[k] = a * a * a * p0[k] + 3f * a * a * u * p1[k] + 3f * a * u * u * p2[k] + u * u * u * p3[k];
        look[0] = 0f;
        look[1] = 2f + 2.7f * u;
        look[2] = 20f * (1f - u);
        if (t >= STRIKE && t < BLOW) {
            // a step toward her, up to where the head of the mallet will come
            float v = clamp((t - STRIKE) / (BLOW - STRIKE));
            v = v * v;
            float[] end = {blowAim[0], blowAim[1] + 0.2f, blowAim[2] - 2.4f};
            for (int k = 0; k < 3; k++) eye[k] += (end[k] - p3[k]) * v;
            look[1] += (blowAim[1] - look[1]) * v;
        }
        if (t >= BLOW) {
            // back and down, to see her come down; then up a little to the name
            float v = ease((t - BLOW) / 0.9f);
            float[] from = {blowAim[0], blowAim[1] + 0.2f, blowAim[2] - 2.4f};
            float[] to = {0f, 3.6f, -17f};
            for (int k = 0; k < 3; k++) eye[k] = from[k] + (to[k] - from[k]) * v;
            float n = ease((t - NAME) / 0.6f);
            look[0] = 0f;
            look[1] = 1.5f + 4.6f * n;
            look[2] = 0f;
        }
        if (shake > 0f) {
            float s = shake * shake * 0.35f;
            eye[0] += (rnd.nextFloat() - 0.5f) * s;
            eye[1] += (rnd.nextFloat() - 0.5f) * s;
        }
    }

    private final float[] blowAim = new float[3];
    private boolean aimed;

    // ---- drawing ----

    void draw(DrawList d, Px f, int h) {
        if (!aimed) {
            headAt("throw", 0.40f, blowAim);
            aimed = true;
        }
        float[] e = new float[3], l = new float[3];
        eye(e, l);
        float dx = l[0] - e[0], dy = l[1] - e[1], dz = l[2] - e[2];
        float yaw = (float) Math.atan2(dx, dz), pitch = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        d.time = t;
        Daylight.light(d, 1f);
        // the haze thinner than in the street, so the town shows from high up
        d.fogStart = 30f;
        d.fogScale = 320f;
        double el = Math.toRadians(34), tu = Math.toRadians(-150);
        d.sun((float) (Math.cos(el) * Math.sin(tu)), (float) Math.sin(el), (float) (Math.cos(el) * Math.cos(tu)),
                0f, 6f, 10f, 130f, 80f, 200f);
        d.camera(e[0], e[1], e[2], yaw, pitch, World.W * 1.2f, 0.1f, 500f);
        // the dark, and the one light left: on her, then up on the name
        float dark = clamp((t - DARK) / 0.35f);
        if (dark > 0f) {
            float n = ease((t - NAME) / 0.45f);
            d.spot[0] = 0f;
            d.spot[1] = 3f + 4.5f * n;
            d.spot[2] = 1f * n;
            d.spot[3] = (24f - 15f * ease((t - DARK) / 0.5f)) * (1f - n) + 8.5f * n;
            d.spot[4] = dark;
        }
        if (dark < 0.995f) {
            d.append(set);
            cars(d);
            walkers(d);
        } else {
            // only the stones under her
            d.box(0f, FLOOR - 0.12f, 0f, 17f, 0.12f, 17f, null, 0xD8CCB6, DrawList.SQUARE);
        }
        if (!broken) {
            String m = motion();
            float mt = motionTime();
            Frida.draw(d, 0f, FLOOR, 0f, 0f, SIZE, m, mt);
            grip(m, mt);
            Mallet.draw(d, turn, hand[0], hand[1], hand[2], MALLET, 0f, true, 0, Mallet.ALL);
            // the mallet's head lights what is near it
            headAt(m, mt, tmp);
            d.fire[0] = tmp[0];
            d.fire[1] = tmp[1];
            d.fire[2] = tmp[2];
            d.fire[3] = 0.9f;
        } else {
            rubble(d);
        }
        if (t >= NAME) name(d);
        flat(d, f, h);
    }

    private void cars(DrawList d) {
        for (int c = 0; c < CARS; c++) {
            float along = (start[c] + t * 9f) % 160f - 80f;
            float x, z, yaw;
            switch (lane[c]) {
                case 0: x = along; z = 22.5f; yaw = (float) Math.PI * 0.5f; break;          // behind the square, going right
                case 1: x = -along; z = 25.5f; yaw = -(float) Math.PI * 0.5f; break;        // and coming back
                case 2: x = 25.5f; z = along + 40f; yaw = 0f; break;                        // to the right of it, going away
                default: x = 22.5f; z = 40f - along; yaw = (float) Math.PI; break;          // and coming back
            }
            Cars.car(d, x, z, yaw, paint[c], false);
        }
    }

    private void walkers(DrawList d) {
        // along the promenade, and on the far pavements: never near the eye
        for (int i = 0; i < 16; i++) {
            float sp = 1.1f + (i % 5) * 0.12f, dir = (i & 1) == 0 ? 1f : -1f;
            float along = ((i * 37f + t * sp * dir) % 120f + 120f) % 120f - 60f;
            float x, z, yaw;
            if (i < 8) {
                x = along;
                z = -24f + (i % 3) * 2.2f;
                yaw = dir > 0 ? (float) Math.PI * 0.5f : -(float) Math.PI * 0.5f;
            } else if (i < 12) {
                x = along;
                z = 29.2f;
                yaw = dir > 0 ? (float) Math.PI * 0.5f : -(float) Math.PI * 0.5f;
            } else {
                x = 29.2f;
                z = along + 50f;
                yaw = dir > 0 ? 0f : (float) Math.PI;
            }
            people.figure(d, i, x, FLOOR, z, yaw, t * sp * 3.2f + i);
        }
    }

    private void rubble(DrawList d) {
        float[] r = new float[9];
        for (int i = 0; i < bn; i++) {
            Chase.spun(axis, i * 3, spin[i], bt, i * 9, r);
            d.box(bp[i * 3], bp[i * 3 + 1], bp[i * 3 + 2], bh[i * 3], bh[i * 3 + 1], bh[i * 3 + 2], r, bc[i], bm[i]);
        }
        for (int k = 0; k < 2; k++) {
            Chase.spun(pieceAxis, k * 3, pieceSpin[k], pieceTurn, k * 9, r);
            float pivot = k == 0 ? Mallet.HANDLE_MIDDLE : Mallet.HEAD_MIDDLE;
            Mallet.draw(d, r, piece[k * 3], piece[k * 3 + 1], piece[k * 3 + 2], MALLET, pivot, true,
                    k == 0 ? 0 : Mallet.HEAD_FROM, k == 0 ? Mallet.HEAD_FROM : Mallet.ALL);
        }
        // the burning head still lights the stones round it
        d.fire[0] = piece[3];
        d.fire[1] = piece[4];
        d.fire[2] = piece[5];
        d.fire[3] = 0.9f;
    }

    /** The name in dots, lit one column after another, each white, then gold. */
    private void name(DrawList d) {
        int cols = PixelFont.width(WORD, 1) + 1;
        float pitch = 0.3f, x0 = -(cols - 1) * pitch * 0.5f, top = 9.2f, z = 2f;
        int col = 0;
        for (int i = 0; i < WORD.length(); i++) {
            char c = WORD.charAt(i);
            for (int gx = 0; gx < 5; gx++, col++) {
                float lit = t - (NAME + 0.6f * col / cols);
                if (lit < 0f) continue;
                for (int gy = 0; gy < 7; gy++) {
                    if (!PixelFont.dot(c, gx, gy)) continue;
                    float x = x0 + col * pitch, y = top - gy * pitch;
                    if (lit < 0.33f) d.box(x, y, z, 0.12f, 0.12f, 0.12f, null, 0xFFFFFF, DrawList.GLOW);
                    else d.box(x, y, z, 0.125f, 0.12f, 0.22f, null, 0xF2B63A, DrawList.material(DrawList.FIRE, 244));
                }
            }
            col++;
        }
    }

    /** Over the frame: the cracks in the glass, the flash of the blow, the white at the end. */
    private void flat(DrawList d, Px f, int h) {
        if (crackN > 0) {
            float fade = 1f - clamp((t - BLOW - 0.4f) / 1.1f);
            if (fade > 0f) {
                // the cracks stay on the glass where the blow struck it
                // (the head of the mallet comes straight at the eye: the middle of the glass)
                float ox = 0f, oy = h * 0.46f;
                for (int i = 0; i < crackN; i++) {
                    int o = i * 5;
                    int a = Math.round(255f * fade * cracks[o + 4]);
                    line(f, cracks[o] + ox, cracks[o + 1] + oy + 1f, cracks[o + 2] + ox, cracks[o + 3] + oy + 1f, (a / 3) << 24);
                    line(f, cracks[o] + ox, cracks[o + 1] + oy, cracks[o + 2] + ox, cracks[o + 3] + oy, (a << 24) | 0xFFFFFF);
                }
            }
        }
        float white = 0f;
        if (t >= BLOW) white = Math.max(white, 1f - (t - BLOW) / 0.15f);
        if (t >= WHITE_AT) white = Math.max(white, clamp((t - WHITE_AT) / (LENGTH - WHITE_AT)));
        if (white > 0f) wash(f, white);
    }

    /** The whole frame washed white, so far. */
    static void wash(Px f, float white) {
        float w = clamp(white);
        int a = Math.round(255f * w);
        int c = (a << 24) | 0xFFFFFF;
        for (int i = 0; i < f.a.length; i++) {
            int old = f.a[i], oa = old >>> 24;
            if (oa == 0) {
                f.a[i] = c;
                continue;
            }
            // what is already over the frame goes white with it
            int r = (int) (((old >> 16) & 0xFF) * (1f - w) + 255f * w), g = (int) (((old >> 8) & 0xFF) * (1f - w) + 255f * w),
                    b = (int) ((old & 0xFF) * (1f - w) + 255f * w);
            f.a[i] = (Math.max(oa, a) << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /** A line a pixel wide. */
    private static void line(Px f, float x0, float y0, float x1, float y1, int c) {
        float dx = x1 - x0, dy = y1 - y0;
        int n = (int) Math.max(Math.abs(dx), Math.abs(dy)) + 1;
        for (int i = 0; i <= n; i++) f.set(Math.round(x0 + dx * i / n), Math.round(y0 + dy * i / n), c);
    }

    // ---- the set ----

    /**
     * The square and what is round it, once: the setts in rings, the pavements, the promenade
     * with its palms and the parapet, the sea; the roads behind and to the right; houses on three
     * sides and the town beyond them.
     */
    private static void build(DrawList d, Random rnd) {
        d.box(0f, FLOOR - 0.12f, 0f, 17f, 0.12f, 17f, null, 0xD8CCB6, DrawList.SQUARE);
        // pavements round it
        d.box(18.5f, (FLOOR - 0.25f) * 0.5f, 6f, 1.5f, (FLOOR + 0.25f) * 0.5f, 23f, null, 0xCDBFA6, DrawList.PAVING);
        d.box(-0.5f, (FLOOR - 0.25f) * 0.5f, 18.5f, 18.5f, (FLOOR + 0.25f) * 0.5f, 1.5f, null, 0xCDBFA6, DrawList.PAVING);
        d.box(-18f, (FLOOR - 0.25f) * 0.5f, 6f, 1f, (FLOOR + 0.25f) * 0.5f, 23f, null, 0xCDBFA6, DrawList.PAVING);
        // the roads: behind the square along x, to the right of it along z, asphalt where they cross
        d.box(-50f, -0.25f, 24f, 70f, 0.25f, 4f, null, 0x5E4844, DrawList.ROAD);
        d.box(24f, -0.25f, -0.5f, 4f, 0.25f, 16.5f, null, 0x5E4844, DrawList.ROAD);
        d.box(24f, -0.25f, 80f, 4f, 0.25f, 52f, null, 0x5E4844, DrawList.ROAD);
        d.box(24f, -0.26f, 24f, 4f, 0.25f, 4f, null, 0x5E4844, DrawList.ASPHALT);
        d.box(80f, -0.25f, 24f, 52f, 0.25f, 4f, null, 0x5E4844, DrawList.ROAD);
        d.box(29f, (FLOOR - 0.25f) * 0.5f, 60f, 1f, (FLOOR + 0.25f) * 0.5f, 90f, null, 0xCDBFA6, DrawList.PAVING);
        d.box(0f, (FLOOR - 0.25f) * 0.5f, 29f, 140f, (FLOOR + 0.25f) * 0.5f, 1f, null, 0xCDBFA6, DrawList.PAVING);
        // the promenade, the parapet, the sea
        d.box(0f, (FLOOR - 1.5f) * 0.5f, -24.5f, 160f, (FLOOR + 1.5f) * 0.5f, 7.5f, null, 0xDCCDB2, DrawList.PROMENADE);
        d.box(0f, -0.3f, -32.3f, 160f, 1.2f, 0.3f, null, 0xE2D4BA, DrawList.STONE);
        d.box(0f, 0.95f, -32.3f, 160f, 0.06f, 0.38f, null, 0xEDE2CC, DrawList.PLAIN);
        d.box(0f, -0.85f, -330f, 600f, 0.25f, 298f, null, 0x2D6990, DrawList.SEA);
        for (int i = 0; i < 18; i++) {
            float x = -76f + i * 9f;
            if (Math.abs(x) < 6f) continue;
            Street.palm(d, rnd, x, FLOOR, -28f + (i % 2) * 1.2f, 7f + rnd.nextFloat() * 2f, (rnd.nextFloat() - 0.5f) * 0.3f, false);
        }
        // the houses round the square: to the left, facing it; behind and to the right, past the roads
        int seed = 300;
        seed = row(d, rnd, false, -19f, -1, -17f, 46f, seed, true);
        seed = row(d, rnd, true, 30f, 1, -80f, 120f, seed, true);
        seed = row(d, rnd, false, 30f, 1, -17f, 140f, seed, true);
        // the town beyond, seen from above as the eye comes down
        for (int r = 0; r < 4; r++) {
            seed = row(d, rnd, true, 52f + r * 22f, 1, -90f, 150f, seed, false);
            seed = row(d, rnd, false, 52f + r * 22f, 1, -17f, 46f, seed, false);
            seed = row(d, rnd, false, -41f - r * 22f, -1, -17f, 160f, seed, false);
        }
    }

    /**
     * A row of houses: their fronts on the line wall (along x, or along z), deep the other way
     * (back is +1 or -1, the side away from the street), from one end to the other; balconies and
     * awnings on the near ones. The next seed.
     */
    private static int row(DrawList d, Random rnd, boolean alongX, float wall, int back, float from, float to, int seed,
                           boolean near) {
        float at = from;
        while (at < to) {
            int bays = 3 + rnd.nextInt(3);
            float len = bays * 2.9f;
            int storeys = 3 + rnd.nextInt(3);
            float height = 3.3f * (storeys + 1) + 0.5f;
            int tint = Street.WALLS[rnd.nextInt(Street.WALLS.length)];
            float mid = at + len * 0.5f, deep = wall + back * 7f;
            float bx = alongX ? mid : deep, bz = alongX ? deep : mid;
            float hx = alongX ? len * 0.5f : 7f, hz = alongX ? 7f : len * 0.5f;
            d.box(bx, height * 0.5f, bz, hx, height * 0.5f, hz, null, tint, DrawList.material(DrawList.FACADE, seed & 255));
            float cx = alongX ? mid : wall - back * 0.15f, cz = alongX ? wall - back * 0.15f : mid;
            d.box(cx, height - 0.2f, cz, alongX ? hx : 0.25f, 0.2f, alongX ? 0.25f : hz, null, Street.scale(tint, 1.05f), DrawList.PLAIN);
            Street.roof(d, bx, height, bz, hx, hz, seed);
            if (near) Street.fronts(d, alongX, wall, -back, at, bays, storeys, seed);
            at += len;
            seed++;
        }
        return seed;
    }
}
