package com.shumtugle.skydysh;

import java.util.Random;

/**
 * What the game opens with, some seven and a half seconds; the one word in it is the game's name.
 *
 * The eye comes in from far out over the sea, low over the beach and the promenade with its palms,
 * across the shore road to the square with its fountain, and turns along the shore to home. It
 * stops before her: a giant, two and a half times her own height, standing in the shore road
 * before home's door with a golden mallet in her right hand. Her eyes light, and a low blow of
 * metal sounds. The town goes dark; one light is left, on her, and the mallet glows of itself.
 * Over the frame the name is typed as the dispatcher's screen types, in the letters of its tube,
 * a white cursor before them; a high chime. Out of the road before her rises a pedestal, orange,
 * a speaker's stand, as tall as a speaker's is to the speaker, a golden button on its top, and at
 * once she swings the mallet back over her head and brings it down on the button: a clang, the
 * boom of the ground, a flash. The frame goes white, and at its whitest the game begins, in the
 * same town, before home. Over the last three seconds before the blow, up in the frame's corner,
 * small, an old film's count down: three, two, one, a ring and a ring in it in the name's gold,
 * the cross of its lines, the hand sweeping round, the numeral in the name's cream; nothing waits
 * for it.
 *
 * A touch at any moment skips to the white.
 */
final class Opening {
    // when things happen: the eye lands before her, her eyes light, the dark; the name typed; the
    // pedestal rising, risen; the mallet back, down, on the button; the white, the end. Nothing
    // waits for the count down: it is laid over the last three seconds before the blow
    static final float LAND = 4.6f, EYES = 4.6f, DARK = 5.1f, NAME = 5.3f, RISE = 5.2f, RISEN = 6f;
    static final float SWING = 6.1f, STRIKE = 6.72f, HIT = 7f, COUNT = HIT - 3f;
    static final float WHITE_AT = HIT + 0.4f, LENGTH = WHITE_AT + 0.25f;
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

    // the pedestal: half its width across and its depth, its top a little wider; its button (how
    // wide, how high), the gold collar round it; how high it stands, as a part of the way from her
    // hips to her chest (a speaker's stand to the speaker)
    static final float PED_HALF = 0.85f, PED_DEEP = 0.6f, PED_LIP = 0.14f, BUTTON_R = 0.5f, BUTTON_HIGH = 0.3f, COLLAR = 0.66f;
    static final float PED_TALL = 0.55f;
    private boolean struck;
    private float shake;

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
        cue(6, NAME + TYPED, Synth.CHIME, 0.7f, 1.6f);
        // the pedestal coming up out of the road: stone grinding
        cue(2, RISE, Synth.GROUND, 0.7f, 1.3f);
        // the count down: a tick a number, the old projector's
        cue(9, COUNT, Synth.TOCK, 0.8f, 0.9f);
        cue(10, COUNT + 1f, Synth.TOCK, 0.8f, 0.9f);
        cue(11, COUNT + 2f, Synth.TOCK, 0.8f, 0.9f);
        cue(3, SWING + 0.2f, Synth.WHOOSH, 0.8f, 0.7f);
        // the mallet on the button: a clang, the ground's boom, a bell
        if (t >= HIT && !struck && !skipped) {
            struck = true;
            shake = 0.8f;
        }
        cue(4, HIT, Synth.CLANG, 1f, 0.6f);
        cue(5, HIT, Synth.GROUND, 1f, 0.8f);
        cue(7, HIT + 0.02f, Synth.THUNDER, 0.7f, 0.7f);
        cue(8, HIT + 0.05f, Synth.CHIME, 0.8f, 1.2f);
        // the name typed, a tick a letter
        int typed = typedNow();
        if (typed > ticks && t < WHITE_AT) {
            ticks = typed;
            // each letter struck in: a knock of stone
            host.sound(Synth.STONE, 0.85f, 0.85f + 0.08f * (typed % 2));
        }
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
        place();
        if (t < SWING) return t;
        if (t < STRIKE) {
            // back over her head, slowly, gathering
            float u = clamp((t - SWING) / (STRIKE - SWING));
            return 0.27f * u * u;
        }
        // down, faster and faster, onto the button; there it stays
        float u = clamp((t - STRIKE) / (HIT - STRIKE));
        return 0.27f + (hitMt - 0.27f) * u * (0.6f + 0.4f * u);
    }

    /** How far into her swing the head of the mallet comes down onto the button (found once). */
    private float hitMt = 0.40f;

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

    /**
     * For the icon (the desktop stand): her alone on a plain floor under the evening sky, so far
     * into her throw (the mallet going back over her shoulder), seen from eye (x, y, z, yaw,
     * pitch, focal as a share of the width).
     */
    void portrait(DrawList d, float mt, float[] eye) {
        d.time = 0f;
        Daylight.light(d, 1f);
        d.fogStart = 60f;
        d.fogScale = 400f;
        double el = Math.toRadians(38), tu = Math.toRadians(-155);
        d.sun((float) (Math.cos(el) * Math.sin(tu)), (float) Math.sin(el), (float) (Math.cos(el) * Math.cos(tu)),
                0f, 6f, 10f, 130f, 80f, 200f);
        d.camera(eye[0], eye[1], eye[2], eye[3], eye[4], World.W * eye[5], 0.1f, 500f);
        if (eye[10] <= 0f) {
            Frida.draw(d, OX, FLOOR, OZ, 0f, SIZE, "throw", mt);
            grip("throw", mt, 1f);
            Mallet.draw(d, turn, hand[0], hand[1], hand[2], MALLET, 0f);
            return;
        }
        // at rest, the mallet over her right shoulder: the hand at the handle's foot, its head
        // behind the shoulder, the handle along the arm
        String m = mt < 0f ? "idle" : "throw";
        float t = Math.max(0f, mt);
        Frida.draw(d, OX, FLOOR, OZ, 0f, SIZE, m, t);
        Frida.place("hand_r", OX, FLOOR, OZ, 0f, SIZE, m, t, hand);
        Frida.place("upperarm_r", OX, FLOOR, OZ, 0f, SIZE, m, t, elbow);
        float sx = elbow[0] + eye[7], sy = elbow[1] + eye[8], sz = elbow[2] + eye[9];
        float hx = sx - hand[0], hy = sy - hand[1], hz = sz - hand[2];
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
        // as big as it takes for the head to come past the shoulder
        float scale = (l + eye[10]) / Mallet.HEAD_MIDDLE;
        Mallet.draw(d, turn, hand[0], hand[1], hand[2], scale, 0f);
    }

    /** Where the mallet's head is, with her at a motion and time. */
    private void headAt(String motion, float mt, float[] out) {
        grip(motion, mt, motion.equals("throw") ? 1f : 0f);
        float s = Mallet.HEAD_MIDDLE * MALLET;
        out[0] = hand[0] + turn[0] * s;
        out[1] = hand[1] + turn[3] * s;
        out[2] = hand[2] + turn[6] * s;
    }

    // ---- the pedestal ----

    /** Where the button's top is: under the head of the mallet as it comes down (into out). */
    private final float[] button = new float[3];
    private boolean placed;

    private void place() {
        if (placed) return;
        placed = true;
        // how high the stand's button is to be: between her hips and her chest
        Frida.place("pelvis", OX, FLOOR, OZ, 0f, SIZE, "idle", 0f, tmp);
        float hips = tmp[1];
        Frida.place("spine_03", OX, FLOOR, OZ, 0f, SIZE, "idle", 0f, tmp);
        float want = hips + (tmp[1] - hips) * PED_TALL + BUTTON_HIGH;
        // the swing down, till the head's far end comes down to it
        float e = Mallet.HEAD_END * MALLET;
        for (float mt = 0.27f; mt <= 0.45f; mt += 0.002f) {
            headAt("throw", mt, button);
            button[0] += turn[0] * e;
            button[1] += turn[3] * e;
            button[2] += turn[6] * e;
            hitMt = mt;
            if (button[1] <= want) break;
        }
    }

    /**
     * The pedestal, risen so far (0 under the road, 1 up): a plinth, the orange body, a gold band
     * round its top, the gold button on it, pressed in once struck and lit a moment.
     */
    private void pedestal(DrawList d) {
        place();
        float up = ease((t - RISE) / (RISEN - RISE));
        if (up <= 0f) return;
        float top = button[1] - BUTTON_HIGH, h = top - FLOOR, sink = (1f - up) * (h + BUTTON_HIGH + 0.2f);
        float x = button[0], z = button[2], y0 = FLOOR - sink;
        int orange = 0xE0702A, deep = 0xB4561E, gold = 0xD8A84A;
        // the foot, the body, a gold band under the top, the top a little wider, the plaque on its front
        d.box(x, y0 + 0.12f, z, PED_HALF + 0.12f, 0.12f, PED_DEEP + 0.12f, null, deep, DrawList.PAINT);
        d.box(x, y0 + h * 0.5f, z, PED_HALF, h * 0.5f, PED_DEEP, null, orange, DrawList.PAINT);
        d.box(x, y0 + h - 0.22f, z, PED_HALF + 0.03f, 0.05f, PED_DEEP + 0.03f, null, gold, DrawList.GOLD);
        d.box(x, y0 + h - 0.08f, z, PED_HALF + PED_LIP, 0.08f, PED_DEEP + PED_LIP, null, deep, DrawList.PAINT);
        d.box(x, y0 + h * 0.6f, z - PED_DEEP - 0.02f, 0.3f, 0.3f, 0.02f, null, gold, DrawList.GOLD);
        d.box(x, y0 + h * 0.6f, z - PED_DEEP - 0.035f, 0.18f, 0.18f, 0.02f, null, deep, DrawList.PAINT);
        // the button: a collar of gold, the button on it, pressed in once struck and lit a moment
        d.box(x, y0 + h + 0.03f, z, COLLAR, 0.03f, COLLAR, null, gold, DrawList.GOLD);
        float press = struck ? 0.14f : 0f;
        d.box(x, y0 + h + BUTTON_HIGH * 0.5f - press, z, BUTTON_R, BUTTON_HIGH * 0.5f, BUTTON_R, null, 0xF0C050, DrawList.GOLD);
        d.box(x, y0 + h + BUTTON_HIGH - press, z, BUTTON_R * 0.7f, 0.04f, BUTTON_R * 0.7f, null, 0xF8D878, DrawList.GOLD);
        if (struck && t - HIT < 0.35f)
            d.box(x, y0 + h + BUTTON_HIGH - press + 0.05f, z, BUTTON_R * 0.75f, 0.02f, BUTTON_R * 0.75f, null, 0xFFF0B0, DrawList.GLOW);
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
    // where the eye draws back to as the pedestal rises, and what it looks at there
    private static final float[] BACK_EYE = {OX + 1.6f, 4.4f, OZ - 15.5f}, BACK_LOOK = {OX - 0.6f, 2.3f, OZ - 2f};

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
        // as the pedestal rises, back and up a little, so she and it are both in the frame
        float back = ease((t - RISE) / (RISEN - RISE));
        if (back > 0f) {
            float[] p3 = FLIGHT[FLIGHT.length - 1];
            for (int k = 0; k < 3; k++) {
                eye[k] = p3[k] + (BACK_EYE[k] - p3[k]) * back;
                look[k] = p3[k + 3] + (BACK_LOOK[k] - p3[k + 3]) * back;
            }
        }
        if (shake > 0f) {
            float s = shake * shake * 0.6f;
            eye[0] += (rnd.nextFloat() - 0.5f) * s;
            eye[1] += (rnd.nextFloat() - 0.5f) * s;
        }
    }

    // ---- drawing ----

    void draw(DrawList d, Px f, int h) {
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
            // the light on her, closing in, and on the pedestal before her
            d.spot[0] = OX;
            d.spot[1] = 3f;
            d.spot[2] = OZ - 1.5f;
            d.spot[3] = 24f - 13f * ease((t - DARK) / 0.5f);
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
        pedestal(d);
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

    /** Over the frame: the name, the count down, the flash of the blow, the white at the end. */
    private void flat(DrawList d, Px f, int h) {
        if (t >= NAME) name(d, f, h);
        if (t >= COUNT && t < HIT) leader(f, h);
        float white = 0f;
        if (t >= HIT) white = Math.max(white, 1f - (t - HIT) / 0.15f) * 0.85f;
        if (t >= WHITE_AT) white = Math.max(white, clamp((t - WHITE_AT) / (LENGTH - WHITE_AT)));
        if (white > 0f) wash(f, white);
    }

    // the count down's circle: in the frame's upper corner, its rings' radii (frame pixels)
    static final float LEADER_IN = 40f, LEADER_DOWN = 0.12f, RING = 27f, RING_IN = 22f;

    /**
     * The count down of an old film, small, up in the frame's corner by the name: a dark disc, see
     * through, the part the hand has swept round from the top lit in the name's gold, two rings of
     * gold, the cross of its lines, the hand, the numeral in the name's cream with a dark edge.
     */
    private void leader(Px f, int h) {
        float since = t - COUNT;
        int n = 3 - (int) since;
        float sweep = since - (int) since;
        float cx = World.W - LEADER_IN, cy = h * LEADER_DOWN, a0 = (float) (sweep * Math.PI * 2.0);
        int gold = 0xF2C46A, cream = 0xF2E6C8, ink = 0x1A1622;
        int r0 = (int) RING + 2;
        for (int y = (int) (cy - r0); y <= (int) (cy + r0); y++)
            for (int x = (int) (cx - r0); x <= (int) (cx + r0); x++) {
                float dx = x + 0.5f - cx, dy = y + 0.5f - cy, rr = (float) Math.sqrt(dx * dx + dy * dy);
                if (rr > RING + 1.2f) continue;
                float a = (float) Math.atan2(dx, -dy);
                if (a < 0f) a += (float) (Math.PI * 2.0);
                int c = a < a0 ? 0x6A4A22 : ink;
                float al = a < a0 ? 0.75f : 0.6f;
                if (Math.abs(rr - RING) < 1f || Math.abs(rr - RING_IN) < 0.8f) {
                    c = gold;
                    al = 0.95f;
                } else if (Math.abs(dx) < 0.5f || Math.abs(dy) < 0.5f) {
                    c = gold;
                    al = 0.35f;
                }
                blend(f, x, y, c, al);
            }
        // the hand
        for (float k = 0f; k < RING_IN; k += 0.5f)
            blend(f, Math.round(cx + (float) Math.sin(a0) * k), Math.round(cy - (float) Math.cos(a0) * k), gold, 0.9f);
        // the numeral
        String s = String.valueOf(n);
        int scale = 4, w = PixelFont.width(s, scale);
        int nx = Math.round(cx - w * 0.5f), ny = Math.round(cy - 7 * scale * 0.5f);
        Px num = new Px(w + 2, 7 * scale + 2, new int[(w + 2) * (7 * scale + 2)]);
        PixelFont.draw(num, s, 1, 1, scale, 0xFF000000);
        for (int y = -1; y <= num.h; y++)
            for (int x = -1; x <= num.w; x++) {
                if (num.get(x, y) != 0) continue;
                if (num.get(x - 1, y) != 0 || num.get(x + 1, y) != 0 || num.get(x, y - 1) != 0 || num.get(x, y + 1) != 0)
                    blend(f, nx + x - 1, ny + y - 1, ink, 0.9f);
            }
        for (int y = 0; y < num.h; y++)
            for (int x = 0; x < num.w; x++)
                if (num.a[y * num.w + x] != 0) blend(f, nx + x - 1, ny + y - 1, cream, 1f);
    }

    /** A pixel of the frame laid over with a colour, so much of it (what is under shows through). */
    private static void blend(Px f, int x, int y, int rgb, float a) {
        if (x < 0 || y < 0 || x >= f.w || y >= f.h || a <= 0f) return;
        int o = y * f.w + x, old = f.a[o], oa = old >>> 24;
        int na = Math.round(255f * Math.min(1f, a));
        if (oa == 0) {
            f.a[o] = (na << 24) | (rgb & 0xFFFFFF);
            return;
        }
        int mixed = Px.mix(old, rgb | 0xFF000000, a);
        f.a[o] = (Math.max(oa, na) << 24) | (mixed & 0xFFFFFF);
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
    }}
