package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The folder: dropped out of a helicopter's cabin, it lies somewhere on the roofs, and there is a
 * reward for it. The helicopter circles over where it fell. Golems are after it too: they look
 * about a while, then each runs for a house of its row, climbs the front and runs along the roofs
 * to it. Whoever gets to it first has it.
 *
 * The house it lies on has no way up, nor have the houses next to it, and there is no lift in its
 * row; a house of its row two off or more has a fire escape up its back, in the yard. Up there,
 * then along the roofs: up the ladders where the next roof stands higher, leaping, down onto
 * the lower ones, gliding, beating the air to rise. Taken, she carries it, and goes down to the
 * street gliding.
 */
final class Folder {
    static final int RIVALS = 3, REWARD = 100000;
    // how far off it falls; how far off the rivals start; when each sets off, and how fast it runs
    static final float NEAR = 170f, SPREAD = 50f, RIVALS_FROM = 200f;
    static final float[] SETS_OFF = {35f, 45f, 55f};
    // she up on the roofs this near it: the rivals still looking about see her, and are off
    static final float SEEN_UP = 8f, SEEN_NEAR = 80f;
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

    // the folder's row, wall to wall: its houses (as the cell keeps them), in order along it, and which is the folder's
    private final float[][] row = new float[12][8];
    private int rowN, mine;

    /**
     * Dropped a way off from (sx, sz) onto a house without a way up, in a row of at least three
     * where a house two off or more has a fire escape, with no lift in the row and no way up next door.
     */
    void start(float sx, float sz, City city) {
        int ci, cj;
        for (int tries = 0; ; tries++) {
            float a = rnd.nextFloat() * 6.2832f, d = NEAR + rnd.nextFloat() * SPREAD;
            ci = City.index(sx + (float) Math.sin(a) * d);
            cj = City.index(sz + (float) Math.cos(a) * d);
            int n = city.houses(ci, cj), pick = -1, seen = 0;
            for (int k = 0; k < n; k++) {
                city.house(ci, cj, k, h);
                // tall enough to want a way up, and without one
                if (h[7] == 0f && h[4] > 17f && rnd.nextInt(++seen) == 0) pick = k;
            }
            // failing all else, after long enough: any house at all
            if (pick < 0 && tries > 400 && n > 0) pick = 0;
            if (pick < 0) continue;
            city.house(ci, cj, pick, h);
            rowOf(city, ci, cj);
            // the way up is a fire escape two houses off or more, and nothing in the row is quicker:
            // no lift in it, no way up next door; the rest is over the roofs
            boolean up = false, near = false;
            for (int k = 0; k < rowN; k++) {
                if (k == mine) continue;
                if (row[k][7] == City.LIFT) near = true;
                if (row[k][7] != 0f && Math.abs(k - mine) < 2) near = true;
                if (row[k][7] == City.ESCAPE && Math.abs(k - mine) >= 2) up = true;
            }
            if ((rowN < 3 || !up || near) && tries < 400) continue;
            if (lie(city)) break;
        }
        // the helicopter over it, a little off; the rivals far round it, each from its own way
        heliX = x + (rnd.nextFloat() - 0.5f) * 20f;
        heliZ = z + (rnd.nextFloat() - 0.5f) * 20f;
        heliA = rnd.nextFloat() * 6.2832f;
        float turned = rnd.nextFloat() * 6.2832f;
        // the houses they climb: the farthest along the row from the folder's first, on both sides
        int[] from = new int[rowN - 1];
        int fn = 0;
        for (int k = 0; k < rowN; k++) if (k != mine) from[fn++] = k;
        for (int a = 1; a < fn; a++)
            for (int b = a; b > 0 && Math.abs(from[b] - mine) > Math.abs(from[b - 1] - mine); b--) {
                int t = from[b];
                from[b] = from[b - 1];
                from[b - 1] = t;
            }
        boolean alongX = Math.abs(frontZ) > 0.5f;
        for (int k = 0; k < RIVALS; k++) {
            float a = turned + k * 6.2832f / RIVALS;
            int gi = Math.round((x + (float) Math.sin(a) * RIVALS_FROM) / City.CELL);
            int gj = Math.round((z + (float) Math.cos(a) * RIVALS_FROM) / City.CELL);
            Chase g = new Chase(rnd.nextLong(), gi, gj, rnd.nextInt(4));
            int s = fn > 0 ? from[k % fn] : mine;
            float[] o = row[s];
            float cu = alongX ? o[0] : o[1], hu = alongX ? o[2] : o[3];
            float cv = alongX ? o[1] : o[0], hv = alongX ? o[3] : o[2];
            float side = alongX ? frontZ : frontX;
            // up the front, each a little apart (the fire escapes are at the backs, in the yards)
            float along = cu + (k - 1) * Math.min(1.6f, hu * 0.4f);
            float fv = cv + side * hv;
            float fx = alongX ? along : fv, fz = alongX ? fv : along;
            g.rival(fx, fz, frontX, frontZ, o[4], x, z, SETS_OFF[k] + rnd.nextFloat() * 4f, RIVAL_RUN + rnd.nextFloat() * 0.8f);
            // then along the roofs: before each wall between it and the folder's house
            int dir = Integer.signum(mine - s), n = Math.abs(mine - s);
            float[] w = new float[n * 5];
            for (int i = 0, at = s; i < n; i++, at += dir) {
                float wall = (alongX ? row[at][0] : row[at][1]) + dir * (alongX ? row[at][2] : row[at][3]);
                float stand = wall - dir * 0.6f;
                w[i * 5] = alongX ? stand : cv;
                w[i * 5 + 1] = alongX ? cv : stand;
                w[i * 5 + 2] = row[at + dir][4];
                w[i * 5 + 3] = alongX ? dir : 0f;
                w[i * 5 + 4] = alongX ? 0f : dir;
            }
            g.over(w, n);
            rivals[k] = g;
        }
    }

    /**
     * For the desktop stand: the foot of the fire escape of the folder's row nearest the folder
     * (x, z), the way its wall faces (x, z), and the middle line of the row across (along x, its
     * z; along z, its x); false if the row has none.
     */
    boolean wayUp(float[] out) {
        boolean alongX = Math.abs(frontZ) > 0.5f;
        int best = -1;
        for (int k = 0; k < rowN; k++)
            if (row[k][7] == City.ESCAPE && (best < 0 || Math.abs(k - mine) < Math.abs(best - mine))) best = k;
        if (best < 0) return false;
        float[] o = row[best];
        float cu = alongX ? o[0] : o[1], hu = alongX ? o[2] : o[3], cv = alongX ? o[1] : o[0], hv = alongX ? o[3] : o[2];
        // at the back, in the yard
        float side = alongX ? frontZ : frontX, fv = cv - side * (hv + 0.55f), u = cu - hu + 1.1f;
        out[0] = alongX ? u : fv;
        out[1] = alongX ? fv : u;
        out[2] = -frontX;
        out[3] = -frontZ;
        out[4] = cv;
        return true;
    }

    /** The row of the house in h, in cell (ci, cj): the houses wall to wall with it along its front. */
    private void rowOf(City city, int ci, int cj) {
        boolean alongX = Math.abs(h[6]) > 0.5f;
        float cv = alongX ? h[1] : h[0];
        float[] o = new float[8];
        rowN = 0;
        // every house of the cell facing the same way along the same line, in order along it
        for (int k = 0, n = city.houses(ci, cj); k < n && rowN < row.length; k++) {
            city.house(ci, cj, k, o);
            if (o[5] != h[5] || o[6] != h[6] || Math.abs((alongX ? o[1] : o[0]) - cv) > 0.5f) continue;
            int at = rowN++;
            float u = alongX ? o[0] : o[1];
            while (at > 0 && (alongX ? row[at - 1][0] : row[at - 1][1]) > u) {
                System.arraycopy(row[at - 1], 0, row[at], 0, 8);
                at--;
            }
            System.arraycopy(o, 0, row[at], 0, 8);
        }
        mine = 0;
        for (int k = 0; k < rowN; k++) if (row[k][0] == h[0] && row[k][1] == h[1]) mine = k;
        // only those wall to wall with it: cut where there is a gap
        int lo = mine, hi = mine;
        while (lo > 0 && gap(row[lo - 1], row[lo], alongX) < 0.3f) lo--;
        while (hi < rowN - 1 && gap(row[hi], row[hi + 1], alongX) < 0.3f) hi++;
        for (int k = lo; k <= hi; k++) System.arraycopy(row[k], 0, row[k - lo], 0, 8);
        rowN = hi - lo + 1;
        mine -= lo;
    }

    private static float gap(float[] a, float[] b, boolean alongX) {
        return alongX ? (b[0] - b[2]) - (a[0] + a[2]) : (b[1] - b[3]) - (a[1] + a[3]);
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
        float sx = r.x - x, sz = r.z - z;
        if (r.y > SEEN_UP && sx * sx + sz * sz < SEEN_NEAR * SEEN_NEAR)
            for (int k = 0; k < RIVALS; k++) rivals[k].hurry(1f + k * 2f);
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
