package com.shumtugle.skydysh;

import java.util.Random;

/**
 * The folder: dropped out of a helicopter's cabin, it lies somewhere on the roofs, and there is a
 * reward for it. The helicopter circles over where it fell. Brums are after it too: they look
 * about a while, then each runs for a house of its row, climbs the front and runs along the roofs
 * to it. Whoever gets to it first has it.
 *
 * The house it lies on has no way up, nor have the houses next to it, and there is no lift in its
 * row; a house of its row two off or more has a fire escape up its back, in the yard. Up there,
 * then along the roofs: up the ladders where the next roof stands higher, leaping, down onto
 * the lower ones, gliding, beating the air to rise. Taken, the dispatcher says it is safe now:
 * down off the roof with it, to the town hall, where the clerk in the secretariat takes it from
 * her hands; then home.
 */
final class Folder {
    static final int RIVALS = 3;
    // how far off it falls; how far off the rivals start; when each sets off, and how fast it runs
    static final float NEAR = 170f, SPREAD = 50f, RIVALS_FROM = 200f;
    static final float[] SETS_OFF = {35f, 45f, 55f};
    // she up on the roofs this near it: the rivals still looking about see her, and are off
    static final float SEEN_UP = 8f, SEEN_NEAR = 80f;
    static final float RIVAL_RUN = 8.6f;
    // she has it when this near; the helicopter, over where it fell
    static final float TAKE = 1.4f, HELI_UP = 14f, HELI_ROUND = 30f, HELI_TURN = 0.22f;

    // after it is hers: the race, then to the town hall with it, to the secretariat's desk, handed over, done
    static final int RACE = 0, BACK = 1, DESK = 2, HAND = 3, DONE = 4;
    // handing it over: how long in all; it leaves her hands, and is in the clerk's; how near the desk
    static final float HAND_TIME = 3.2f, PASS_FROM = 0.9f, PASS_TO = 2.0f, DESK_NEAR = 1.9f;
    int step = RACE;
    float handT;

    final Chase[] rivals = new Chase[RIVALS];
    float x, y, z;                       // where it lies
    float clock, doneAt = -1f;
    boolean hers, theirs;
    Chase taker;                         // the rival that got it, if one did
    // the house it is on: its middle and half sizes, its top, the way its front faces
    float houseX, houseZ, houseHX, houseHZ, top, frontX, frontZ;
    private float heliX, heliZ, heliA, rotor;
    private final Random rnd;
    private final float[] h = new float[8], hand = new float[3], turn = new float[9], foot = new float[2];

    Folder(long seed) {
        rnd = new Random(seed);
    }

    /** Whether mission n is the folder's: the second, and every fifth after it. */
    static boolean its(int n) {
        return World.kindOf(n) == 2;
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
            boolean up = false, near = false, any = false;
            for (int k = 0; k < rowN; k++) {
                if (k == mine) continue;
                if (row[k][7] == City.LIFT) near = true;
                if (row[k][7] != 0f && Math.abs(k - mine) < 2) near = true;
                if (row[k][7] == City.ESCAPE && Math.abs(k - mine) >= 2) up = true;
                if (row[k][7] == City.ESCAPE) any = true;
            }
            if ((rowN < 3 || !up || near) && tries < 400) continue;
            // not found so: a row with a fire escape in it at least, for the rivals go up as she does
            if (!any && tries < 4000) continue;
            if (lie(city)) break;
        }
        // the helicopter over it, a little off; the rivals far round it, each from its own way
        heliX = x + (rnd.nextFloat() - 0.5f) * 20f;
        heliZ = z + (rnd.nextFloat() - 0.5f) * 20f;
        heliA = rnd.nextFloat() * 6.2832f;
        float turned = rnd.nextFloat() * 6.2832f;
        // the way they go up, as she must: into the yard behind the row (under its arch or through a
        // hall), up a fire escape of the row, along the roofs; each her own escape, the nearest
        // the folder first, and the way into the yard nearest the foot of it
        int[] esc = new int[rowN];
        int en = 0;
        for (int k = 0; k < rowN; k++) if (row[k][7] == City.ESCAPE && k != mine) esc[en++] = k;
        for (int a = 1; a < en; a++)
            for (int b = a; b > 0 && Math.abs(esc[b] - mine) < Math.abs(esc[b - 1] - mine); b--) {
                int t = esc[b];
                esc[b] = esc[b - 1];
                esc[b - 1] = t;
            }
        float[] ways = new float[8 * 7];
        int wn = city.yardWays(ci, cj, ways, 8);
        boolean alongX = Math.abs(frontZ) > 0.5f;
        float side = alongX ? frontZ : frontX;
        for (int k = 0; k < RIVALS; k++) {
            float a = turned + k * 6.2832f / RIVALS;
            int gi = Math.round((x + (float) Math.sin(a) * RIVALS_FROM) / City.CELL);
            int gj = Math.round((z + (float) Math.cos(a) * RIVALS_FROM) / City.CELL);
            if (gj < City.SHORE) gj = 2 * City.SHORE - gj;
            gi = Math.max(City.WEST_I, Math.min(City.EAST_I, gi));
            Chase g = new Chase(rnd.nextLong(), gi, gj, City.way(gi, gj, rnd.nextInt(4)));
            int s = en > 0 ? esc[k % en] : mine;
            float[] o = row[s];
            float cu = alongX ? o[0] : o[1], hu = alongX ? o[2] : o[3];
            float cv = alongX ? o[1] : o[0], hv = alongX ? o[3] : o[2];
            // the escape's foot, at the back in the yard
            city.escapeFoot(o, foot);
            float ex = foot[0], ez = foot[1];
            int pick = 0;
            float best = 1e18f;
            for (int w = 0; w < wn; w++) {
                float dx = ways[w * 7 + 2] - ex, dz = ways[w * 7 + 3] - ez, dd = dx * dx + dz * dz;
                if (dd < best) {
                    best = dd;
                    pick = w;
                }
            }
            if (wn == 0) {
                // no way into the yard at all (it should not be): the front of the escape's house, as a door
                ways[0] = alongX ? cu : cv + side * (hv + 0.9f);
                ways[1] = alongX ? cv + side * (hv + 0.9f) : cu;
                ways[2] = ex;
                ways[3] = ez;
                ways[4] = 1f;
                ways[5] = frontX;
                ways[6] = frontZ;
                wn = 1;
            }
            g.rival(ways, pick * 7, ex, ez, -frontX, -frontZ, o[4], x, z, SETS_OFF[k] + rnd.nextFloat() * 4f, RIVAL_RUN + rnd.nextFloat() * 0.8f);
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
    boolean wayUp(City city, float[] out) {
        boolean alongX = Math.abs(frontZ) > 0.5f;
        int best = -1;
        for (int k = 0; k < rowN; k++)
            if (row[k][7] == City.ESCAPE && (best < 0 || Math.abs(k - mine) < Math.abs(best - mine))) best = k;
        if (best < 0) return false;
        float[] o = row[best];
        float cu = alongX ? o[0] : o[1], hu = alongX ? o[2] : o[3], cv = alongX ? o[1] : o[0], hv = alongX ? o[3] : o[2];
        // at the back, in the yard
        city.escapeFoot(o, foot);
        out[0] = foot[0];
        out[1] = foot[1];
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
            step = BACK;
        }
    }

    /** Hers, a step on with it: into the town hall, up to the secretariat's desk, handed over. */
    void back(float dt, Runner r, Indoors in, World.Host host) {
        switch (step) {
            case BACK:
                if (in != null && in.townHall) step = DESK;
                break;
            case DESK:
                if (in == null || !in.townHall) {
                    step = BACK;
                    break;
                }
                float du = in.u(r.x, r.z) - in.deskU, dv = in.v(r.x, r.z) - in.deskV;
                if (du * du + dv * dv < DESK_NEAR * DESK_NEAR) {
                    step = HAND;
                    handT = 0f;
                    host.sound(Synth.CHIME, 0.25f, 1.8f);
                }
                break;
            case HAND: {
                float was = handT;
                handT += dt;
                if (was < PASS_TO && handT >= PASS_TO) {
                    host.sound(Synth.LEATHER, 0.5f, 1.2f);
                    host.sound(Synth.CHIME, 0.35f, 1.4f);
                    host.buzz(30);
                }
                if (handT >= HAND_TIME) step = DONE;
                break;
            }
            default:
                break;
        }
    }

    /** Whether it is in the clerk's hands now: the job done. */
    boolean delivered() {
        return step == DONE;
    }

    /** Whether it is being handed over now: she stands, the eye on the two of them. */
    boolean handing() {
        return step == HAND;
    }

    /** Whether she carries it still. */
    boolean carried() {
        return hers && (step == RACE || step == BACK || step == DESK || step == HAND && handT < PASS_FROM);
    }

    /** Whether the clerk reaches for it, or holds it: from the moment she is at the desk. */
    boolean clerkHas() {
        return step == HAND || step == DONE;
    }

    /** It on its way from her hands to the clerk's: so far (0 to 1); -1 before; 1 once the clerk's. */
    float passing() {
        if (step == DONE) return 1f;
        if (step != HAND || handT < PASS_FROM) return -1f;
        return Math.min(1f, (handT - PASS_FROM) / (PASS_TO - PASS_FROM));
    }

    /** Where she takes it now, into out (x, y, z): the town hall's door, the desk in its hall; false once given. */
    boolean goal(Indoors in, float[] out) {
        if (step == BACK || step == DESK && (in == null || !in.townHall)) return Archive.townDoor(out);
        if (step == DESK) {
            out[0] = in.wx(in.deskU, in.deskV);
            out[1] = Indoors.FLOOR + 2.4f;
            out[2] = in.wz(in.deskU, in.deskV);
            return true;
        }
        return false;
    }

    /** The word for where it goes now, as Lang keeps it. */
    String goalWord(Indoors in) {
        return in != null && in.townHall ? "goal.desk" : "goal.townhall";
    }

    /** The mark for where it goes now. */
    int mark(Indoors in) {
        return in != null && in.townHall ? Marks.DESK : Marks.TOWNHALL;
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
            if (carried()) inHand(d, r);
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

    /** It in her hand as she carries it (indoors as well). */
    void inHand(DrawList d, Runner r) {
        r.bone("hand_l", hand);
        yaw(r.heading + (float) Math.PI * 0.5f);
        folder(d, hand[0], hand[1] - 0.05f, hand[2], true);
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
