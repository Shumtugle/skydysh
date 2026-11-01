package com.shumtugle.skydysh;

import java.util.Random;

/**
 * What the game opens with, some eight and a half seconds; the one word in it is the game's name.
 *
 * The eye comes in from far out over the sea, low over the beach and the promenade with its palms,
 * across the shore road to the square with its fountain, and turns along the shore to home. It
 * stops before her: a giant, two and a half times her own height, standing in the shore road
 * before home's door with a golden mallet in her right hand. Her eyes light, and a low blow of metal sounds. The town goes dark; one light is
 * left, on her, and the mallet glows of itself. She swings it back over her head, slowly, then
 * down at the eye, faster and faster, the eye stepping up to meet it; it strikes the glass of the
 * screen: thunder, the ground's boom, glass, a white flash, the frame shaking. Eight white cracks
 * run out from the blow and branch, and she falls to pieces, every block of her flung off away
 * from the blow, turning, knocking on the stones; the mallet falls, its handle and its burning
 * head apart. The eye draws back and down to see her come down; the cracks melt away. The light
 * stays on the heap, and over the frame the name is typed as the dispatcher's screen types, in the
 * letters of its tube, a white cursor before them; a high chime. The frame goes white, and at its
 * whitest the game begins, in the same town, before home.
 *
 * A touch at any moment skips to the white.
 */
final class Opening {
    static final float LENGTH = 8.75f, WHITE_AT = 8.5f;
    static final float LAND = 4.6f, EYES = 4.6f, DARK = 5.1f, SWING = 5.7f, STRIKE = 6.15f, BLOW = 6.4f, NAME = 7.2f;
    static final float SIZE = Runner.SIZE * 2.5f, MALLET = Toss.SCALE / Runner.SIZE * SIZE;
    static final float FLOOR = 0f;                    // the shore road
    // where she stands: in the shore road before home's door, facing the sea
    static final float OX = City.HOME_DOOR_X, OZ = City.HOME_J * City.CELL;
    static final String WORD = "SKYDYSH";

    float t;
    boolean done;
    private final World.Host host;
    private final City city;
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

    Opening(World.Host host, City city) {
        this.host = host;
        this.city = city;
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
    private int ticks;
    static final float TYPED = 1.05f;                  // how long the name takes to type

    /** How many letters of the name are typed by now. */
    private int typedNow() {
        if (t < NAME) return 0;
        return Math.min(WORD.length(), 1 + (int) ((t - NAME) / TYPED * WORD.length()));
    }

    void update(float dt) {
        t += dt;
        Frida.eyes = clamp((t - EYES) / 0.4f);
        // in the dark only the light on her and her eyes are left: made bigger then, as coals are
        // seen bigger in the dark, so they are not lost to the frame's coarseness
        float dark = clamp((t - DARK) / 0.35f);
        Frida.eyeSize = 1f + 0.4f * dark;
        Frida.eyeHeat = dark;
        cue(0, 0f, Synth.WIND, 0.55f, 1f);
        cue(1, EYES + 0.05f, Synth.CLANG, 0.9f, 0.45f);
        cue(2, SWING, Synth.WHOOSH, 0.8f, 0.7f);
        if (t >= BLOW && !broken && !skipped) blow();
        cue(3, BLOW, Synth.THUNDER, 1f, 0.85f);
        cue(4, BLOW, Synth.GROUND, 1f, 0.8f);
        cue(5, BLOW, Synth.GLASS, 1f, 0.9f);
        // under it all, a low blow felt more than heard
        cue(7, BLOW + 0.02f, Synth.GROUND, 1f, 0.6f);
        cue(8, BLOW + 0.04f, Synth.THUNDER, 0.8f, 0.55f);
        cue(6, NAME + TYPED, Synth.CHIME, 0.7f, 1.6f);
        // the name typed, a tick a letter
        int typed = typedNow();
        if (typed > ticks && t < WHITE_AT) {
            ticks = typed;
            // each letter struck in, as the blocks came down: a knock of stone, the eye jolted
            host.sound(Synth.STONE, 0.95f, 0.85f + 0.08f * (typed % 2));
            shake = Math.max(shake, 0.32f);
        }
        if (broken) fall(dt);
        shake = Math.max(0f, shake - dt * 3.2f);
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
        Frida.place("hand_r", OX, FLOOR, OZ, 0f, SIZE, motion, mt, hand);
        Frida.place("lowerarm_r", OX, FLOOR, OZ, 0f, SIZE, motion, mt, elbow);
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
        bn = Frida.capture(OX, FLOOR, OZ, 0f, SIZE, "throw", 0.40f, bp, bt, bh, bc, bm);
        float cx = OX, cy = FLOOR + 0.95f * SIZE, cz = OZ;
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

    // the flight in: where the eye is at each of these points, what it looks at there, and how far
    // into the flight (eased) it is there; from far out at sea, over the beach and the promenade, over
    // the square, along the shore to her
    private static final float[][] FLIGHT = {
            {-120f, 58f, -250f, -20f, 6f, 30f},
            {-62f, 24f, -78f, -32f, 3f, 30f},
            {-30f, 12f, -8f, -24f, 2f, 34f},
            {8f, 11f, -1f, OX, 4f, OZ + 2f},
            {OX, 3.3f, OZ - 10.5f, OX, 4.7f, OZ}};
    private static final float[] KNOTS = {0f, 0.34f, 0.62f, 0.84f, 1f};

    /** One of the six values of the flight, u (0 to 1) of the way along it, through its points. */
    private static float flight(int k, float u) {
        int n = FLIGHT.length, s = 0;
        while (s < n - 2 && u > KNOTS[s + 1]) s++;
        float v = clamp((u - KNOTS[s]) / (KNOTS[s + 1] - KNOTS[s]));
        float p0 = FLIGHT[Math.max(0, s - 1)][k], p1 = FLIGHT[s][k], p2 = FLIGHT[s + 1][k], p3 = FLIGHT[Math.min(n - 1, s + 2)][k];
        // a curve through the points, smooth from one into the next
        return 0.5f * (2f * p1 + (p2 - p0) * v + (2f * p0 - 5f * p1 + 4f * p2 - p3) * v * v + (3f * p1 - p0 - 3f * p2 + p3) * v * v * v);
    }

    /** Where the eye is and what it looks at, into eye (x y z) and look (x y z). */
    private void eye(float[] eye, float[] look) {
        float u = ease(t / LAND);
        for (int k = 0; k < 3; k++) {
            eye[k] = flight(k, u);
            look[k] = flight(k + 3, u);
        }
        float[] p3 = FLIGHT[FLIGHT.length - 1];
        if (t >= STRIKE && t < BLOW) {
            // a step toward her, up to where the head of the mallet will come
            float v = clamp((t - STRIKE) / (BLOW - STRIKE));
            v = v * v;
            float[] end = {blowAim[0], blowAim[1] + 0.2f, blowAim[2] - 2.4f};
            for (int k = 0; k < 3; k++) eye[k] += (end[k] - p3[k]) * v;
            look[1] += (blowAim[1] - look[1]) * v;
        }
        if (t >= BLOW) {
            // back and down, to see her come down
            float v = ease((t - BLOW) / 0.9f);
            float[] from = {blowAim[0], blowAim[1] + 0.2f, blowAim[2] - 2.4f};
            float[] to = {OX, 3.6f, OZ - 15f};
            for (int k = 0; k < 3; k++) eye[k] = from[k] + (to[k] - from[k]) * v;
            look[0] = OX;
            look[1] = 1.5f;
            look[2] = OZ;
        }
        if (shake > 0f) {
            float s = shake * shake * 0.6f;
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
            // the light on her, closing in; on the heap, a little wider, once she is down
            float n = ease((t - NAME) / 0.6f);
            d.spot[0] = OX;
            d.spot[1] = 3f - 1.5f * n;
            d.spot[2] = OZ;
            d.spot[3] = (24f - 15f * ease((t - DARK) / 0.5f)) * (1f - n) + 10f * n;
            d.spot[4] = dark;
        }
        if (dark < 0.995f) {
            // the town itself, round home, the whole of it at once
            city.draw(d, OX, OZ + 20f, 0f, 0f);
            cars(d);
            walkers(d);
        } else {
            // only the road under her
            d.box(OX, FLOOR - 0.25f, OZ, 17f, 0.25f, 17f, null, 0x5E4844, DrawList.ASPHALT);
        }
        if (!broken) {
            String m = motion();
            float mt = motionTime();
            Frida.draw(d, OX, FLOOR, OZ, 0f, SIZE, m, mt);
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
        flat(d, f, h);
    }

    /** Cars up and down the two streets either side of the square, both ways; none in the shore road before her. */
    private void cars(DrawList d) {
        for (int c = 0; c < CARS; c++) {
            float along = (start[c] + t * 9f) % 200f;
            float x = (lane[c] < 2 ? 0f : -City.CELL), lane1 = lane[c] % 2 == 0 ? 1.6f : -1.6f;
            float z = lane[c] % 2 == 0 ? 6f + along : 206f - along;
            Cars.car(d, x + lane1, z, lane[c] % 2 == 0 ? 0f : (float) Math.PI, paint[c], false);
        }
    }

    /** People along the promenade, both ways, and on the far side of the square: never near her. */
    private void walkers(DrawList d) {
        for (int i = 0; i < 16; i++) {
            float sp = 1.1f + (i % 5) * 0.12f, dir = (i & 1) == 0 ? 1f : -1f;
            float along = ((i * 37f + t * sp * dir) % 160f + 160f) % 160f - 140f;
            float x, z, yaw = dir > 0 ? (float) Math.PI * 0.5f : -(float) Math.PI * 0.5f;
            if (i < 10) {
                x = along;
                z = OZ - 8.5f - (i % 3) * 2f;
            } else {
                x = -City.CELL + 8f + ((along + 140f) % 48f);
                z = City.CELL - 6.5f;
            }
            people.figure(d, i, x, City.WALK_TOP, z, yaw, t * sp * 3.2f + i);
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

    private static TubeFont font;
    private Px word;
    static final int NAME_SCALE = 4;

    /**
     * The name over the frame, typed as the dispatcher's screen types it, in its tube's letters
     * made big, cream, the letter just typed white, a cursor after the last; the frame's rows
     * there drawn as the tube draws them.
     */
    private void name(DrawList d, Px f, int h) {
        if (font == null) font = TubeFont.read(host.asset("tube.bin"));
        if (font == null) return;
        int n = typedNow();
        String shown = WORD.substring(0, n);
        int ww = font.width(WORD) + 4;
        if (word == null) word = new Px(ww, font.rows);
        java.util.Arrays.fill(word.a, 0);
        font.draw(word, shown, 0, 0, 0xFFF2E6C8, null);
        // the letter just typed, white a moment
        float since = t - NAME - (n - 1) * TYPED / WORD.length();
        if (n > 0 && since < 0.12f) font.draw(word, WORD.substring(n - 1, n), font.width(WORD.substring(0, n - 1)), 0, 0xFFFFFFFF, null);
        // the cursor, blinking once all is typed
        boolean all = n == WORD.length();
        if (!all || (int) ((t - NAME) * 2.2f) % 2 == 0) {
            int cx = font.width(shown) + 1;
            for (int y = 2; y < font.base; y++) for (int x = cx; x < cx + 3 && x < ww; x++) word.a[y * ww + x] = 0xFFF2C46A;
        }
        int s = NAME_SCALE, x0 = (World.W - font.width(WORD) * s) / 2, y0 = Math.round(h * 0.26f);
        for (int y = 0; y < word.h; y++)
            for (int x = 0; x < ww; x++) {
                int c = word.a[y * ww + x];
                if (c == 0) continue;
                for (int dy = 0; dy < s; dy++)
                    for (int dx = 0; dx < s; dx++) {
                        int px = x0 + x * s + dx, py = y0 + y * s + dy;
                        if (px >= 0 && px < f.w && py >= 0 && py < f.h) f.a[py * f.w + px] = c;
                    }
            }
        d.tube[0] = x0 - 6;
        d.tube[1] = y0 - 4;
        d.tube[2] = x0 + ww * s + 6;
        d.tube[3] = y0 + font.rows * s + 4;
    }

    /** Over the frame: the cracks in the glass, the flash of the blow, the white at the end. */
    private void flat(DrawList d, Px f, int h) {
        if (t >= NAME) name(d, f, h);
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
}
