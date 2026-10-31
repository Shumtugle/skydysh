package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The folder: dropped out of a helicopter's cabin, it lies somewhere on the roofs, and there is a
 * reward for it. The helicopter circles over where it fell. Golems are after it too: they look
 * about a while, then run for its house, climb the front and cross the roof to it. Whoever gets
 * to it first has it.
 *
 * The house it lies on has no lift; houses near it have. Up in one of them, then across the
 * roofs: leaping, gliding. Taken, she carries it, and goes down to the street gliding.
 */
final class Folder {
    static final int RIVALS = 3, REWARD = 100000;
    // how far off it falls; how far off the rivals start; when each sets off, and how fast it runs
    static final float NEAR = 170f, SPREAD = 50f, RIVALS_FROM = 260f;
    static final float[] SETS_OFF = {60f, 80f, 100f};
    static final float RIVAL_RUN = 8.6f;
    // she has it when this near; the helicopter, over where it fell
    static final float TAKE = 1.4f, HELI_UP = 14f, HELI_ROUND = 30f, HELI_TURN = 0.22f;

    final Chase[] rivals = new Chase[RIVALS];
    float x, y, z;                       // where it lies
    float clock, doneAt = -1f;
    boolean hers, theirs;
    Chase taker;                         // the rival that got it, if one did
    // the house it is on: its middle and half sizes, its top, the way its front faces
    float houseX, houseZ, houseHX, houseHZ, top, frontX, frontZ;
    private float heliX, heliZ, heliA, rotor;
    private final Random rnd;
    private final float[] h = new float[8], hand = new float[3], turn = new float[9];

    Folder(long seed) {
        rnd = new Random(seed);
    }

    /** Whether mission n is the folder's: every other one, the second, the fourth and so on. */
    static boolean its(int n) {
        return n % 2 == 0;
    }

    /** Dropped a way off from (sx, sz) onto a house without a lift, in a block where others have one. */
    void start(float sx, float sz, City city) {
        for (int tries = 0; ; tries++) {
            float a = rnd.nextFloat() * 6.2832f, d = NEAR + rnd.nextFloat() * SPREAD;
            int ci = City.index(sx + (float) Math.sin(a) * d), cj = City.index(sz + (float) Math.cos(a) * d);
            int n = city.houses(ci, cj), lifts = 0, pick = -1, seen = 0;
            for (int k = 0; k < n; k++) {
                city.house(ci, cj, k, h);
                if (h[7] != 0f) lifts++;
                // tall enough to want a lift, and without one
                else if (h[4] > 17f && rnd.nextInt(++seen) == 0) pick = k;
            }
            // a block with lifts near it first; failing that, any house at all
            if (pick < 0 && tries > 200 && n > 0) pick = 0;
            if (pick < 0 || lifts < 2 && tries < 40) continue;
            city.house(ci, cj, pick, h);
            if (lie(city)) break;
        }
        // the helicopter over it, a little off; the rivals far round it, each from its own way
        heliX = x + (rnd.nextFloat() - 0.5f) * 20f;
        heliZ = z + (rnd.nextFloat() - 0.5f) * 20f;
        heliA = rnd.nextFloat() * 6.2832f;
        float turned = rnd.nextFloat() * 6.2832f;
        for (int k = 0; k < RIVALS; k++) {
            float a = turned + k * 6.2832f / RIVALS;
            int ci = Math.round((x + (float) Math.sin(a) * RIVALS_FROM) / City.CELL);
            int cj = Math.round((z + (float) Math.cos(a) * RIVALS_FROM) / City.CELL);
            Chase g = new Chase(rnd.nextLong(), ci, cj, rnd.nextInt(4));
            // each climbs at its own place along the front
            float along = (k - 1) * Math.min(2.4f, (Math.abs(frontX) * houseHZ + Math.abs(frontZ) * houseHX) * 0.6f);
            float half = Math.abs(frontX) * houseHX + Math.abs(frontZ) * houseHZ;
            float fx = houseX + frontX * half + frontZ * along, fz = houseZ + frontZ * half - frontX * along;
            g.rival(fx, fz, frontX, frontZ, top, x, z, SETS_OFF[k] + rnd.nextFloat() * 4f, RIVAL_RUN + rnd.nextFloat() * 0.8f);
            rivals[k] = g;
        }
    }

    /** It comes down somewhere on the house h: on the tiles, clear of the chimneys. */
    private boolean lie(City city) {
        houseX = h[0];
        houseZ = h[1];
        houseHX = h[2];
        houseHZ = h[3];
        top = h[4];
        frontX = h[5];
        frontZ = h[6];
        for (int k = 0; k < 20; k++) {
            float px = houseX + (rnd.nextFloat() - 0.5f) * 2f * (houseHX - 1.8f);
            float pz = houseZ + (rnd.nextFloat() - 0.5f) * 2f * (houseHZ - 1.8f);
            float g = city.ground(px, pz, top + 10f, 20f);
            if (g > top + 0.4f) continue;
            x = px;
            z = pz;
            y = g;
            return true;
        }
        return false;
    }

    void update(float dt, Runner r, City city, Cars cars) {
        if (doneAt < 0f) clock += dt;
        rotor += dt * 28f;
        heliA += dt * HELI_TURN;
        for (Chase g : rivals) g.update(dt, r, city, cars);
        if (doneAt >= 0f) return;
        for (Chase g : rivals)
            if (g.got && !g.done()) {
                theirs = true;
                taker = g;
                doneAt = clock;
                return;
            }
        float dx = r.x - x, dz = r.z - z;
        if (dx * dx + dz * dz < TAKE * TAKE && Math.abs(r.y - y) < 1.5f) {
            hers = true;
            doneAt = clock;
        }
    }

    boolean over() {
        return doneAt >= 0f;
    }

    /** How far she is from it, meters. */
    float apart(Runner r) {
        float dx = r.x - x, dy = r.y - y, dz = r.z - z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The nearest rival still about; null when none is. */
    Chase nearest() {
        Chase best = null;
        for (Chase g : rivals) if (!g.done() && (best == null || g.apart < best.apart)) best = g;
        return best;
    }

    /** How far a rival still has to go to it, along the streets and up, roughly, meters. */
    float toGo(Chase g) {
        float dx = x - g.x, dz = z - g.z;
        return Math.abs(dx) + Math.abs(dz) + Math.max(0f, top - g.y);
    }

    /** The helicopter's place now, into out (x, y, z), and its heading as the result. */
    float heli(float[] out) {
        out[0] = heliX + (float) Math.sin(heliA) * HELI_ROUND;
        out[1] = top + HELI_UP + (float) Math.sin(heliA * 2.3f) * 1.5f;
        out[2] = heliZ + (float) Math.cos(heliA) * HELI_ROUND;
        return heliA + (float) (Math.PI / 2);
    }

    private final float[] at = new float[3];

    void draw(DrawList d, Runner r) {
        // the folder: lying where it fell, or under her arm, or in the rival's hands
        if (hers) {
            r.bone("hand_l", hand);
            yaw(r.heading + (float) Math.PI * 0.5f);
            folder(d, hand[0], hand[1] - 0.05f, hand[2], true);
        } else if (theirs && taker != null && !taker.done()) {
            taker.bone("hand_r", hand);
            yaw(taker.heading);
            folder(d, hand[0], hand[1], hand[2], false);
        } else if (!theirs) {
            yaw(0.6f);
            folder(d, x, y, z, false);
            papers(d);
        }
        heliDraw(d);
    }

    private void yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        turn[0] = c; turn[1] = 0f; turn[2] = s;
        turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
        turn[6] = -s; turn[7] = 0f; turn[8] = c;
    }

    /** Red card, the papers' edge pale along it, a black band round it; upright when carried. */
    private void folder(DrawList d, float fx, float fy, float fz, boolean upright) {
        float w = 0.34f, l = 0.26f, t = 0.035f;
        if (upright) {
            d.box(fx, fy, fz, l, w, t, turn, 0xB0302A, DrawList.PAINT);
            d.box(fx, fy, fz, l - 0.02f, w - 0.02f, t + 0.012f, turn, 0xEFE6D2, DrawList.PLAIN);
            d.box(fx, fy, fz, l + 0.004f, 0.03f, t + 0.016f, turn, 0x1B1B20, DrawList.PLAIN);
            return;
        }
        d.box(fx, fy + t, fz, w, t, l, turn, 0xB0302A, DrawList.PAINT);
        d.box(fx, fy + t, fz, w - 0.02f, t + 0.01f, l - 0.02f, turn, 0xEFE6D2, DrawList.PLAIN);
        d.box(fx, fy + 2f * t + 0.006f, fz, w + 0.004f, 0.008f, l - 0.02f, turn, 0xB0302A, DrawList.PAINT);
        d.box(fx, fy + t, fz, 0.03f, t + 0.012f, l + 0.004f, turn, 0x1B1B20, DrawList.PLAIN);
        // the sun catching it now and then: a glint, so it is found
        float g = (float) Math.sin(clock * 3.1f);
        if (g > 0.4f) {
            float k = (g - 0.4f) / 0.6f, s = 0.05f + 0.35f * k;
            d.box(fx + 0.2f, fy + 0.2f, fz, s, 0.012f, 0.012f, null, 0xFFF3C8, DrawList.GLOW);
            d.box(fx + 0.2f, fy + 0.2f, fz, 0.012f, s, 0.012f, null, 0xFFF3C8, DrawList.GLOW);
        }
    }

    /** A few sheets blown out of it, stirring on the tiles. */
    private void papers(DrawList d) {
        for (int k = 0; k < 3; k++) {
            float a = k * 2.1f + 0.4f, rr = 0.8f + k * 0.45f;
            float px = x + (float) Math.sin(a) * rr, pz = z + (float) Math.cos(a) * rr;
            float lift = Math.max(0f, (float) Math.sin(clock * 2.2f + k * 1.7f)) * 0.06f;
            yaw(a + (float) Math.sin(clock * 1.3f + k) * 0.25f);
            d.box(px, y + 0.012f + lift, pz, 0.15f, 0.006f, 0.21f, turn, 0xF4EEE0, DrawList.PLAIN);
        }
    }

    /** The helicopter: a blue cabin with a glass nose, a boom and fin, skids, and its blades going round. */
    private void heliDraw(DrawList d) {
        float hd = heli(at), hx = at[0], hy = at[1], hz = at[2];
        // nose a little down as it goes
        yaw(hd);
        float fx = (float) Math.sin(hd), fz = (float) Math.cos(hd);
        int body = 0x2E4D7A, dark = 0x1B2333;
        d.box(hx, hy, hz, 0.75f, 0.8f, 1.5f, turn, body, DrawList.PAINT);
        d.box(hx + fx * 1.35f, hy + 0.1f, hz + fz * 1.35f, 0.72f, 0.6f, 0.5f, turn, 0x9FC4D8, DrawList.GLASS);
        d.box(hx - fx * 3.1f, hy + 0.35f, hz - fz * 3.1f, 0.18f, 0.18f, 1.8f, turn, body, DrawList.PAINT);
        d.box(hx - fx * 4.8f, hy + 0.9f, hz - fz * 4.8f, 0.06f, 0.7f, 0.35f, turn, 0xC23A2E, DrawList.PAINT);
        for (int s = -1; s <= 1; s += 2) {
            float sx = fz * s * 0.75f, sz = -fx * s * 0.75f;
            d.box(hx + sx, hy - 1.2f, hz + sz, 0.05f, 0.05f, 1.6f, turn, dark, DrawList.PLAIN);
            d.box(hx + sx * 0.8f, hy - 0.95f, hz + sz * 0.8f, 0.04f, 0.25f, 0.04f, turn, dark, DrawList.PLAIN);
        }
        d.box(hx, hy + 0.95f, hz, 0.12f, 0.15f, 0.12f, null, dark, DrawList.PLAIN);
        // the blades: two, long and thin, turning
        for (int b = 0; b < 2; b++) {
            yaw(rotor + b * (float) (Math.PI / 2));
            d.box(hx, hy + 1.12f, hz, 0.12f, 0.025f, 5.2f, turn, dark, DrawList.PLAIN);
        }
        // the tail rotor
        yaw(hd);
        d.box(hx - fx * 4.8f + fz * 0.12f, hy + 1f, hz - fz * 4.8f - fx * 0.12f, 0.02f, 0.6f, 0.06f, turn, dark, DrawList.PLAIN);
    }
}
