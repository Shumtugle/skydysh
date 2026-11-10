package com.shumtugle.skydysh;

/**
 * Where people cross the town's streets, and which crossings have lights.
 *
 * Zebras: a street between two crossings has one at each end, just off the crossing, where the
 * pavements' corners are. A street along a place people go to has one only, before the way in:
 * along a square or a garden, the old town's lanes, the doors the town keeps the same in every
 * game (the old house, the town hall, the vets', the concert hall, home and the shop across from
 * it, the archive) and the workshop's gate.
 *
 * Lights: at some crossings, not all; for the cars. The two streets through a crossing are green
 * in turn, amber between; the people cross a street while its cars stand at red.
 *
 * A street is named by the cell that owns it: cell (i, j) owns the street along its west edge,
 * running north (x = i * CELL, z from j * CELL), and the one along its south edge, running east
 * (z = j * CELL, x from i * CELL). Along it, t goes from its south or west crossing, 0 to CELL.
 */
final class Crossings {
    /** A zebra's middle at an end, from its crossing's middle; half its width along the street. */
    static final float END = Walkers.RING, HALF = 1.5f;
    /** Before a place's way in: the street's middle, mostly. */
    static final float MID = City.CELL * 0.5f;
    // the bars: half their width across the street, how far apart; how high over the asphalt
    static final float BAR = 0.3f, PITCH = 1.1f, PAINT_TOP = 0.03f;
    /** A car stops with its middle so far before a zebra's edge (its nose a step short of it). */
    static final float NOSE = 3.3f;
    /** Where a car stops at a red light, its middle: so far from the crossing's middle (before the zebra). */
    static final float STOP = END + HALF + NOSE;
    // the cycle of the lights: green, amber, then the other street's green and amber
    static final float GREEN = 9f, AMBER = 2.4f, CYCLE = 2f * (GREEN + AMBER);
    static final int GO = 0, WARN = 1, HALT = 2;
    // the post of a light: so far out from the street's middle (on the pavement), so far back from
    // the crossing's middle; its head this high
    static final float POST_OUT = City.CURB + 0.45f, POST_BACK = END + HALF + 0.9f, HEAD_Y = City.WALK_TOP + 3.1f;

    /** The town's clock for its lights, seconds. */
    static float time;

    private Crossings() {
    }

    private static int mix(int i, int j, int salt) {
        int h = i * 0x27D4EB2D ^ j * 0x165667B1 ^ salt * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return h;
    }

    /** Whether the crossing at (i * CELL, j * CELL) has lights: about one in three, in the town's rows. */
    static boolean lit(int i, int j) {
        if (j < City.SHORE || j >= City.EDGE_J) return false;
        return ((mix(i, j, 3) >>> 8) % 8) < 3;
    }

    /** How the light stands for the cars going along x (alongX) or along z through crossing (i, j): GO, WARN or HALT. */
    static int light(int i, int j, boolean alongX) {
        float t = time + ((mix(i, j, 4) >>> 8) % 1000) * 0.001f * CYCLE;
        t %= CYCLE;
        // the street along z first, then the one along x
        if (!alongX) return t < GREEN ? GO : t < GREEN + AMBER ? WARN : HALT;
        t -= GREEN + AMBER;
        return t < 0f ? HALT : t < GREEN ? GO : t < GREEN + AMBER ? WARN : HALT;
    }

    /** Whether the street owned by cell (i, j) is in the town: north (along its west edge) or east. */
    static boolean street(int i, int j, boolean north) {
        if (j < City.SHORE) return false;
        return north ? j + 1 <= City.EDGE_J : j < City.EDGE_J;
    }

    /** Whether cell (i, j) is a block of the town at all (not the sea, not past its end). */
    private static boolean town(int j) {
        return !City.sea(j) && !City.country(j);
    }

    /**
     * Where the street owned by cell (i, j) has its one zebra before a place's way in, as t along
     * it; -1 if it has none (and a zebra at each end instead).
     */
    static float mid(City city, int i, int j, boolean north) {
        if (!street(i, j, north)) return -1f;
        if (north) {
            // between cell (i - 1, j) on its west and cell (i, j) on its east
            if (i == 0 && j == City.WORKSHOP_J) return City.CELL * 0.72f;
            if (open(city, i - 1, j) || open(city, i, j)) return city.seeded(i, j) ? City.CELL * 0.6f : MID;
            return -1f;
        }
        // between cell (i, j - 1) on its south and cell (i, j) on its north; the doors look south
        if (i == City.CLINIC_I && j == City.CLINIC_J) return City.CELL * 0.34f;
        if (j == City.OLD_J && i == City.OLD_I || j == City.TOWNHALL_J && i == City.TOWNHALL_I
                || j == City.CONCERT_J && i == City.CONCERT_I || j == City.HOME_J && i == City.HOME_I
                || j == City.ARCHIVE_J && i == City.ARCHIVE_I) return MID;
        if (open(city, i, j - 1) || open(city, i, j)) return MID;
        return -1f;
    }

    /** Whether people go into cell (i, j) on every side: a square or a garden, the old town's lanes. */
    private static boolean open(City city, int i, int j) {
        if (!town(j)) return false;
        return city.square(i, j) || City.oldTown(i, j);
    }

    /** Whether the street owned by cell (i, j) has a zebra with its middle at t (within a little). */
    static boolean zebraAt(City city, int i, int j, boolean north, float t) {
        if (!street(i, j, north)) return false;
        float m = mid(city, i, j, north);
        if (m >= 0f) return Math.abs(t - m) < 0.5f;
        return Math.abs(t - END) < 0.5f || Math.abs(t - (City.CELL - END)) < 0.5f;
    }

    /** Whether a car parked with its middle at t along the street, so long each way, would stand on a zebra. */
    static boolean onZebra(City city, int i, int j, boolean north, float t, float len) {
        if (!street(i, j, north)) return false;
        float m = mid(city, i, j, north), reach = HALF + len + 0.3f;
        if (m >= 0f) return Math.abs(t - m) < reach;
        return Math.abs(t - END) < reach || Math.abs(t - (City.CELL - END)) < reach;
    }

    /** The zebras of the streets cell (i, j) owns, painted on its asphalt; and the posts of its crossing's lights. */
    static void build(City city, City.Cell c, int i, int j) {
        float x0 = i * City.CELL, z0 = j * City.CELL;
        for (int k = 0; k < 2; k++) {
            boolean north = k == 0;
            if (!street(i, j, north)) continue;
            float m = mid(city, i, j, north);
            if (m >= 0f) zebra(c.boxes, x0, z0, north, m);
            else {
                zebra(c.boxes, x0, z0, north, END);
                zebra(c.boxes, x0, z0, north, City.CELL - END);
            }
        }
        if (lit(i, j)) posts(c, x0, z0, i, j);
    }

    private static void zebra(DrawList d, float x0, float z0, boolean north, float t) {
        int white = 0xEEEAE0;
        float hy = PAINT_TOP * 0.5f;
        for (float a = -City.ROAD + 0.55f; a <= City.ROAD - 0.5f; a += PITCH) {
            if (north) d.box(x0 + a, hy, z0 + t, BAR, hy, HALF, null, white, DrawList.PAINT);
            else d.box(x0 + t, hy, z0 + a, HALF, hy, BAR, null, white, DrawList.PAINT);
        }
    }

    /**
     * The four posts of a crossing's lights, one for each way in, on the right of the cars coming
     * in, before their stop; each head turned to them, its three lamps dark (the lit one is drawn
     * over it, see lamps). None where no street comes in.
     */
    private static void posts(City.Cell c, float x, float z, int i, int j) {
        DrawList d = c.detail;
        for (int w = 0; w < 4; w++) {
            if (!comes(i, j, w)) continue;
            float px = postX(x, w), pz = postZ(z, w);
            int iron = 0x2B2F3A, head = 0x1C1F26;
            d.box(px, City.WALK_TOP + 1.55f, pz, 0.065f, 1.55f, 0.065f, null, iron, DrawList.PLAIN);
            d.box(px, City.WALK_TOP + 0.2f, pz, 0.13f, 0.2f, 0.13f, null, iron, DrawList.PLAIN);
            // the head: a box facing the way the cars come from, a hood over each lamp
            float fx = faceX(w), fz = faceZ(w);
            boolean alongX = fx != 0f;
            float hx = alongX ? 0.14f : 0.2f, hz = alongX ? 0.2f : 0.14f;
            d.box(px, HEAD_Y, pz, hx, 0.52f, hz, null, head, DrawList.PLAIN);
            for (int l = 0; l < 3; l++) {
                float ly = HEAD_Y + 0.33f - l * 0.33f;
                int dark = l == 0 ? 0x4A1E1C : l == 1 ? 0x4A3A1A : 0x1E3A26;
                d.box(px + fx * (alongX ? 0.15f : 0f), ly, pz + fz * (alongX ? 0f : 0.15f), alongX ? 0.01f : 0.11f, 0.11f,
                        alongX ? 0.11f : 0.01f, null, dark, DrawList.PLAIN);
                d.box(px + fx * 0.2f, ly + 0.12f, pz + fz * 0.2f, alongX ? 0.06f : 0.13f, 0.02f, alongX ? 0.13f : 0.06f, null, head,
                        DrawList.PLAIN);
            }
            c.wall(px - 0.12f, pz - 0.12f, px + 0.12f, pz + 0.12f, City.WALK_TOP + 3.7f);
        }
    }

    // the ways into a crossing: 0 the cars coming north (from the south), 1 south, 2 east (from the west), 3 west
    private static boolean comes(int i, int j, int w) {
        if (w == 0) return City.road(j, -1);
        if (w == 1) return City.road(j, 1);
        return true;
    }

    // a post stands on the right of the cars coming in that way, before their stop
    private static float postX(float x, int w) {
        return x + (w == 0 ? POST_OUT : w == 1 ? -POST_OUT : w == 2 ? -POST_BACK : POST_BACK);
    }

    private static float postZ(float z, int w) {
        return z + (w == 0 ? -POST_BACK : w == 1 ? POST_BACK : w == 2 ? -POST_OUT : POST_OUT);
    }

    // which way its head looks: toward the cars coming in
    private static float faceX(int w) {
        return w == 2 ? -1f : w == 3 ? 1f : 0f;
    }

    private static float faceZ(int w) {
        return w == 0 ? -1f : w == 1 ? 1f : 0f;
    }

    /** The lit lamps of the lights round (ex, ez), as they stand now. */
    static void lamps(DrawList d, float ex, float ez, float fx, float fz) {
        int ci = Math.round(ex / City.CELL), cj = Math.round(ez / City.CELL);
        for (int j = cj - 2; j <= cj + 2; j++)
            for (int i = ci - 2; i <= ci + 2; i++) {
                if (!lit(i, j)) continue;
                float x = i * City.CELL, z = j * City.CELL, dx = x - ex, dz = z - ez;
                if (dx * dx + dz * dz > 150f * 150f || dx * fx + dz * fz < -20f) continue;
                for (int w = 0; w < 4; w++) {
                    if (!comes(i, j, w)) continue;
                    int s = light(i, j, w >= 2);
                    float px = postX(x, w), pz = postZ(z, w), ax = faceX(w), az = faceZ(w);
                    boolean alongX = ax != 0f;
                    // red at the top, amber, green at the bottom
                    float ly = HEAD_Y + 0.33f - (s == HALT ? 0 : s == WARN ? 1 : 2) * 0.33f;
                    int rgb = s == GO ? 0x4BF08A : s == WARN ? 0xFFB52E : 0xFF3A2E;
                    d.box(px + ax * 0.165f, ly, pz + az * 0.165f, alongX ? 0.012f : 0.1f, 0.1f, alongX ? 0.1f : 0.012f, null, rgb,
                            DrawList.GLOW);
                }
            }
    }
}
