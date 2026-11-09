package com.shumtugle.skydysh;

import java.util.Random;

/**
 * Out to sea, south of the beach: a spit of sand winding out from the water's edge to an island,
 * tall and green, rising in ledges: a ring of sand, then rock and grass, rock and grass, up to a
 * round green top, trees on every ledge, a path winding up. Off its north-east shore a pier on
 * posts, yachts moored along it, rising and falling on the swell.
 *
 * All of it worked out, as the mountain is: how high the ground is anywhere on the spit or the
 * island (the town asks it, see City.ground), the pier and the yachts' decks solid.
 */
final class Island {
    private Island() {
    }

    // the island: its middle, and its profile from the sea bed in to the top: so far out (before
    // its shore wobbles), so high; in turn: the bed, the shallows, the sand, rock and a ledge of
    // grass three times over, the top
    static final float IX = 10f, IZ = -365f;
    private static final float[] PR = {140f, 125f, 118f, 108f, 102f, 96f, 84f, 78f, 64f, 58f, 44f, 38f, 24f, 0f};
    private static final float[] PH = {-7f, -2.1f, -1.15f, 0.4f, 3f, 16f, 18f, 36f, 38f, 56f, 58f, 76f, 80f, 84f};
    static final float REACH = PR[0];

    // the spit: from the water's edge out to the island's shore; its crest, half its width there,
    // how far its sides run down to the bed
    static final float SPIT_FROM = City.SHORE * City.CELL + City.WATERLINE, SPIT_TO = IZ + 112f;
    static final float CREST = -1.05f, CREST_HALF = 3.5f, SIDE = 3.5f, FOOT = 8f;

    /** The spit's middle across, along it at z: out from the beach, winding twice, to the island. */
    static float spitX(float z) {
        float t = Math.max(0f, Math.min(1f, (SPIT_FROM - z) / (SPIT_FROM - SPIT_TO)));
        return 24f - 14f * t + 16f * (float) Math.sin(t * 6.6f) * (1f - 0.4f * t);
    }

    /** How the shore wobbles round: the profile is drawn out so much farther at a heading a. */
    static float wob(float a) {
        return 1f + 0.07f * (float) Math.sin(3f * a + 0.4f) + 0.045f * (float) Math.sin(7f * a + 1.3f);
    }

    /** The island's profile at so far in from its middle (before the wobble); NONE past its foot. */
    private static float profile(float r) {
        if (r >= PR[0]) return NONE;
        for (int k = 1; k < PR.length; k++)
            if (r >= PR[k]) return PH[k] + (PH[k - 1] - PH[k]) * (r - PR[k]) / (PR[k - 1] - PR[k]);
        return PH[PH.length - 1];
    }

    static final float NONE = -1000f;

    /** How high the spit or the island is at (x, z), under the water or over it; NONE away from both. */
    static float height(float x, float z) {
        float h = NONE;
        if (z < SPIT_FROM + 2f && z > SPIT_TO - 30f) {
            float d = Math.abs(x - spitX(z));
            if (d < CREST_HALF) h = CREST;
            else if (d < CREST_HALF + SIDE) h = CREST + (City.WATER - 0.6f - CREST) * (d - CREST_HALF) / SIDE;
            else if (d < CREST_HALF + SIDE + FOOT) h = City.WATER - 0.6f + (PH[0] - City.WATER + 0.6f) * (d - CREST_HALF - SIDE) / FOOT;
        }
        float dx = x - IX, dz = z - IZ, r2 = dx * dx + dz * dz;
        if (r2 < REACH * REACH * 1.3f) {
            float r = (float) Math.sqrt(r2), a = (float) Math.atan2(dx, dz);
            h = Math.max(h, profile(r / wob(a)));
        }
        return h;
    }

    /** Whether (x, z) is near enough the spit or the island to swim about it (the open sea ends elsewhere). */
    static boolean near(float x, float z, float more) {
        float dx = x - IX, dz = z - IZ, reach = REACH * 1.2f + more;
        if (dx * dx + dz * dz < reach * reach) return true;
        return z < SPIT_FROM && z > SPIT_TO - 30f && Math.abs(x - spitX(z)) < CREST_HALF + SIDE + FOOT + more;
    }

    // ---- the pier and the yachts: at a heading off the middle, from the shore so far out ----
    static final float PIER_A = 1.1f, PIER_FROM = 110f, PIER_LONG = 46f, PIER_HALF = 1.6f, DECK = City.WATER + 0.9f;
    static final int YACHTS = 4;
    // a yacht: half its length, half its beam, its deck over the water, its mast; how far from the
    // pier's middle it lies; how far apart along it
    static final float HULL_HALF = 5f, BEAM = 1.5f, YACHT_DECK = City.WATER + 0.85f, MAST = 12f, OFF = PIER_HALF + BEAM + 0.7f;
    private static final int[] STRIPES = {0x22406E, 0xE0644A, 0x2E9C9A, 0x22406E};

    /** The pier's foot on the shore, its way out (x, z each), and the way across it. */
    private static float[] pier() {
        float a = PIER_A, w = wob(a), fx = (float) Math.sin(a), fz = (float) Math.cos(a);
        return new float[]{IX + fx * PIER_FROM * w, IZ + fz * PIER_FROM * w, fx, fz, fz, -fx};
    }

    // ---- the way on foot, for the navigator: from the promenade down the steps nearest the
    // spit to the beach, to the spit's root, out along its crest, then round the island's ring of
    // sand to the pier's foot ----
    // so far apart along the spit; the ring's step, its distance in from the shore's wobble
    static final float LINE_STEP = 8f, RING_STEP = 0.08f, RING_R = 105f;
    private static float[] line;
    private static int lineN;

    private static void line() {
        if (line != null) return;
        java.util.ArrayList<Float> pts = new java.util.ArrayList<Float>();
        // the steps of the promenade's cell the spit leaves from: their top, their foot
        float root = spitX(SPIT_FROM), sx = (float) Math.floor(root / City.CELL) * City.CELL + City.STAIRS_AT;
        float pz = City.SHORE * City.CELL + City.PARAPET;
        pts.add(sx);
        pts.add(pz + 4f);
        // well past the last step, so that the way leaves the steps straight, not over their side
        pts.add(sx);
        pts.add(pz - 4f * City.STAIR - 3f);
        for (float z = SPIT_FROM; z > SPIT_TO; z -= LINE_STEP) {
            pts.add(spitX(z));
            pts.add(z);
        }
        float ex = spitX(SPIT_TO), a0 = (float) Math.atan2(ex - IX, SPIT_TO - IZ);
        for (float a = a0; a < PIER_A; a += RING_STEP) {
            float r = RING_R * wob(a);
            pts.add(IX + (float) Math.sin(a) * r);
            pts.add(IZ + (float) Math.cos(a) * r);
        }
        float[] p = pier();
        pts.add(p[0]);
        pts.add(p[1]);
        line = new float[pts.size()];
        for (int k = 0; k < line.length; k++) line[k] = pts.get(k);
        lineN = line.length / 2;
    }

    /** Whether (x, z) is down on the beach or out past it, on the spit, the island or the sea about them: the way there goes by the steps and the spit. */
    static boolean off(float x, float z) {
        return z < City.SHORE * City.CELL + City.PARAPET - City.PARAPET_HALF - 0.1f;
    }

    /** The number of the way's point nearest (x, z). */
    static int lineNear(float x, float z) {
        line();
        int best = 0;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < lineN; k++) {
            float dx = line[k * 2] - x, dz = line[k * 2 + 1] - z, d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                best = k;
            }
        }
        return best;
    }

    static int lineCount() {
        line();
        return lineN;
    }

    static float lineX(int k) {
        line();
        return line[k * 2];
    }

    static float lineZ(int k) {
        line();
        return line[k * 2 + 1];
    }

    // ---- the way up, for the navigator: from the pier's foot up the stairs, across the top to
    // the lighthouse's door; its points so far apart ----
    static final float CLIMB_STEP = 6f;
    private static float[] climb;
    private static int climbN;

    private static void climb() {
        if (climb != null) return;
        float[] p = pier();
        int steps = (int) Math.ceil((stairsFrom() - STAIRS_TO) / CLIMB_STEP);
        climbN = 1 + steps + 1 + 1;
        climb = new float[climbN * 2];
        climb[0] = p[0];
        climb[1] = p[1];
        float sa = (float) Math.sin(PIER_A), ca = (float) Math.cos(PIER_A), w = wob(PIER_A);
        for (int k = 0; k <= steps; k++) {
            float r = Math.max(STAIRS_TO, stairsFrom() - k * CLIMB_STEP) * w;
            climb[(k + 1) * 2] = IX + sa * r;
            climb[(k + 1) * 2 + 1] = IZ + ca * r;
        }
        float[] at = new float[3];
        door(at);
        climb[(climbN - 1) * 2] = at[0];
        climb[(climbN - 1) * 2 + 1] = at[2];
    }

    static int climbCount() {
        climb();
        return climbN;
    }

    static float climbX(int k) {
        climb();
        return climb[k * 2];
    }

    static float climbZ(int k) {
        climb();
        return climb[k * 2 + 1];
    }

    /** The number of the way up's point nearest (x, z), and how far it is, into far[0]. */
    static int climbNear(float x, float z, float[] far) {
        climb();
        int best = 0;
        float bd = Float.MAX_VALUE;
        for (int k = 0; k < climbN; k++) {
            float dx = climb[k * 2] - x, dz = climb[k * 2 + 1] - z, d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                best = k;
            }
        }
        far[0] = (float) Math.sqrt(bd);
        return best;
    }

    /** Whether (x, z) is up on the top, by the lighthouse. */
    static boolean top(float x, float z) {
        return (x - IX) * (x - IX) + (z - IZ) * (z - IZ) < 16f * 16f;
    }

    /** Before the lighthouse's door, where the path ends (x, y, z), into out. */
    static void door(float[] out) {
        float a = pathA(1f), r = pathR(1f) * wob(a);
        out[0] = IX + (float) Math.sin(a) * r;
        out[2] = IZ + (float) Math.cos(a) * r;
        out[1] = height(out[0], out[2]);
    }

    /** The pier's foot on the shore, (x, z), into out. */
    static void pierFoot(float[] out) {
        float[] p = pier();
        out[0] = p[0];
        out[1] = p[1];
    }

    /** How far out along the pier (x, z) is from its foot, and how far off its middle across. */
    static float alongPier(float x, float z) {
        float[] p = pier();
        return (x - p[0]) * p[2] + (z - p[1]) * p[3];
    }

    static float acrossPier(float x, float z) {
        float[] p = pier();
        return (x - p[0]) * p[4] + (z - p[1]) * p[5];
    }

    /** Whether (x, z) is on the pier's deck. */
    static boolean onPier(float x, float z) {
        float a = alongPier(x, z);
        return a > -0.5f && a < PIER_LONG + 0.5f && Math.abs(acrossPier(x, z)) < PIER_HALF + 0.2f;
    }

    /** A point out along the pier, so far from its foot and so far across (x, z), into out. */
    static void onPier(float along, float across, float[] out) {
        float[] p = pier();
        out[0] = p[0] + p[2] * along + p[4] * across;
        out[1] = p[1] + p[3] * along + p[5] * across;
    }

    /** The way out along the pier, (x, z), into out. */
    static void pierWay(float[] out) {
        float[] p = pier();
        out[0] = p[2];
        out[1] = p[3];
    }

    // the yachts' middle along the pier: between the first pair and the second
    static final float YACHTS_MIDDLE = 12f + 1.2f * HULL_HALF + 2f;

    /** Whether the yachts are wrecked: burnt out and sunk at the pier, no deck to stand on. */
    static boolean wrecked;

    /** Where yacht k lies: its middle (x, z), its heading. */
    private static void yachtAt(int k, float[] out) {
        float[] p = pier();
        float along = 12f + (k / 2) * 2.4f * HULL_HALF + 2f, side = k % 2 == 0 ? 1f : -1f;
        out[0] = p[0] + p[2] * along + p[4] * side * OFF;
        out[1] = p[1] + p[3] * along + p[5] * side * OFF;
        out[2] = (float) Math.atan2(p[2], p[3]);
    }

    /** What stands solid of the pier and the yachts' decks in cell (i, j): squares along each. */
    static void solids(City.Cell c, int i, int j) {
        float x0 = i * City.CELL, z0 = j * City.CELL, x1 = x0 + City.CELL, z1 = z0 + City.CELL;
        if (x1 < IX - REACH * 1.3f || x0 > IX + REACH * 1.3f || z1 < IZ - REACH * 1.3f || z0 > IZ + REACH * 1.3f) return;
        float[] p = pier();
        for (float s = 0f; s <= PIER_LONG; s += 1.2f) {
            float x = p[0] + p[2] * s, z = p[1] + p[3] * s;
            square(c, x0, z0, x1, z1, x, z, PIER_HALF * 0.75f, DECK, DECK - 0.5f);
        }
        // the lighthouse, solid to its roof: its plinth as wide as it is drawn, the tower over it
        square(c, x0, z0, x1, z1, IX, IZ, PLINTH, LH_FOOT + PLINTH_HIGH, 0f);
        square(c, x0, z0, x1, z1, IX, IZ, TOWER_LOW, LH_TOP, 0f);
        float[] at = new float[3];
        if (wrecked) return;
        for (int k = 0; k < YACHTS; k++) {
            yachtAt(k, at);
            float fx = (float) Math.sin(at[2]), fz = (float) Math.cos(at[2]);
            for (float s = -HULL_HALF + 1f; s <= HULL_HALF - 1f; s += 1f)
                square(c, x0, z0, x1, z1, at[0] + fx * s, at[1] + fz * s, BEAM * 0.7f, YACHT_DECK, City.WATER - 0.6f);
        }
    }

    private static void square(City.Cell c, float x0, float z0, float x1, float z1, float x, float z, float half, float top, float bottom) {
        float l = Math.max(x0, x - half), r = Math.min(x1, x + half), n = Math.max(z0, z - half), f = Math.min(z1, z + half);
        if (l < r && n < f) c.wall(l, n, r, f, top, bottom);
    }

    // ---- drawn ----
    private static DrawList far;

    /** The spit, the island, its trees, its path, the pier: seen from anywhere along the shore. */
    static void draw(DrawList d) {
        if (far == null) build();
        d.append(far);
    }

    private static void build() {
        far = new DrawList();
        spit(far);
        slopes(far);
        trees(far);
        path(far);
        stairs(far);
        pierDrawn(far);
        lighthouse(far);
    }

    // ---- the lighthouse on the top: its foot (how far round the plinth, how high), its tower
    // (round at the foot, at the top, how high over the plinth), the gallery, the lantern, the roof ----
    static final float LH_FOOT = PH[PH.length - 1] - 0.8f, PLINTH = 4f, PLINTH_HIGH = 1.6f;
    static final float TOWER_LOW = 3.2f, TOWER_TOP = 2.3f, TOWER_HIGH = 16f;
    static final float GALLERY_R = 3.4f, LANTERN_R = 1.5f, LANTERN_HIGH = 2.2f;
    static final float LH_TOP = LH_FOOT + PLINTH_HIGH + TOWER_HIGH + 0.3f + LANTERN_HIGH + 2.4f;

    /** The way the door looks: toward the path's end on the top, square to the flat nearest it. */
    private static float doorA() {
        return Math.round(pathA(1f) / 0.7854f) * 0.7854f;
    }

    /**
     * An eight-sided block at (x, y, z): so far from its middle to each flat, so high (half). Four
     * bands across it an eighth of a turn apart, each as long as the block is across and as wide
     * as a flat: together, the eight sides and nothing past them.
     */
    private static void octagon(DrawList d, float x, float y, float z, float flat, float halfHigh, int color, int mat) {
        float side = flat * 0.4142f;
        for (int k = 0; k < 4; k++)
            d.box(x, y, z, side, halfHigh, flat, yaw(k * 0.7854f), color, mat);
    }

    /** The lighthouse: a stone plinth, a tower narrowing up, cream with two red bands, a gallery with its rail, the lantern lit, a red roof. */
    private static void lighthouse(DrawList d) {
        float x = IX, z = IZ, y0 = LH_FOOT;
        int cream = 0xF0E2CC, red = 0xC24A3B, stone = 0x8E8A84, iron = 0x2B2E33;
        octagon(d, x, y0 + PLINTH_HIGH * 0.5f, z, PLINTH, PLINTH_HIGH * 0.5f, stone, DrawList.STONE);
        int rings = 14;
        float ring = TOWER_HIGH / rings;
        for (int k = 0; k < rings; k++) {
            float t = (k + 0.5f) / rings, r = TOWER_LOW + (TOWER_TOP - TOWER_LOW) * t;
            boolean band = k >= 4 && k <= 5 || k >= 9 && k <= 10;
            octagon(d, x, y0 + PLINTH_HIGH + (k + 0.5f) * ring, z, r, ring * 0.5f + 0.01f, band ? red : cream, DrawList.PAINT);
        }
        float gy = y0 + PLINTH_HIGH + TOWER_HIGH;
        // the gallery: its deck on corbels, a rail round it
        octagon(d, x, gy + 0.15f, z, GALLERY_R, 0.15f, cream, DrawList.STONE);
        for (int k = 0; k < 8; k++) {
            float a = k * 0.7854f + 0.3927f, ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            d.box(x + sa * (TOWER_TOP + 0.35f), gy - 0.3f, z + ca * (TOWER_TOP + 0.35f), 0.22f, 0.3f, 0.35f, yaw(a), cream, DrawList.STONE);
        }
        for (int k = 0; k < 16; k++) {
            float a = k * 0.3927f;
            d.box(x + (float) Math.sin(a) * (GALLERY_R - 0.1f), gy + 0.75f, z + (float) Math.cos(a) * (GALLERY_R - 0.1f), 0.04f, 0.45f, 0.04f, null,
                    iron, DrawList.RAIL);
        }
        // the rail's top, along each flat (the flats face every eighth of a turn from +z)
        for (int k = 0; k < 8; k++) {
            float a = k * 0.7854f, ap = GALLERY_R - 0.1f;
            d.box(x + (float) Math.sin(a) * ap, gy + 1.2f, z + (float) Math.cos(a) * ap, ap * 0.4142f, 0.04f, 0.04f, yaw(a), iron, DrawList.RAIL);
        }
        // the lantern: glass all round, the lamp lit in it, posts between the panes
        float ly = gy + 0.3f + LANTERN_HIGH * 0.5f;
        octagon(d, x, ly, z, LANTERN_R, LANTERN_HIGH * 0.5f, 0x3A4A55, DrawList.GLASS);
        d.box(x, ly - 0.1f, z, 0.55f, 0.6f, 0.55f, null, 0xFFD45A, DrawList.GLOW);
        for (int k = 0; k < 8; k++) {
            float a = k * 0.7854f + 0.3927f, pr = LANTERN_R + 0.02f;
            d.box(x + (float) Math.sin(a) * pr, ly, z + (float) Math.cos(a) * pr, 0.05f, LANTERN_HIGH * 0.5f, 0.05f, null, 0x6A3A2E, DrawList.PAINT);
        }
        // the roof: red, narrowing up in steps, a ball and a spike on top
        float ry = gy + 0.3f + LANTERN_HIGH;
        for (int k = 0; k < 4; k++)
            octagon(d, x, ry + 0.25f + k * 0.45f, z, 1.95f - k * 0.45f, 0.25f, red, DrawList.PAINT);
        d.box(x, ry + 2.1f, z, 0.22f, 0.22f, 0.22f, yaw(0.7854f), red, DrawList.PAINT);
        d.box(x, ry + 2.6f, z, 0.04f, 0.3f, 0.04f, null, iron, DrawList.RAIL);
        // the door at the foot, toward the path's end, two small windows over it
        float a = doorA(), sa = (float) Math.sin(a), ca = (float) Math.cos(a);
        float[] t = yaw(a);
        d.box(x + sa * PLINTH, y0 + 1.15f, z + ca * PLINTH, 0.62f, 1.15f, 0.06f, t, 0x6B4428, DrawList.material(DrawList.WOOD, 7));
        d.box(x + sa * (PLINTH + 0.02f), y0 + 2.3f, z + ca * (PLINTH + 0.02f), 0.72f, 0.08f, 0.07f, t, cream, DrawList.STONE);
        for (int k = 0; k < 2; k++) {
            float wy = y0 + PLINTH_HIGH + 3.5f + k * 4.2f, r = TOWER_LOW + (TOWER_TOP - TOWER_LOW) * ((wy - y0 - PLINTH_HIGH) / TOWER_HIGH);
            d.box(x + sa * r, wy, z + ca * r, 0.2f, 0.42f, 0.05f, t, 0x24323A, DrawList.GLASS);
        }
    }

    /** The spit: a crest of dry sand on a wider bank of wet, laid in pieces along its winding. */
    private static void spit(DrawList d) {
        float step = 4f;
        for (float z = SPIT_FROM + 1f; z > SPIT_TO - 26f; z -= step) {
            float za = z, zb = z - step, xa = spitX(za), xb = spitX(zb);
            float mx = (xa + xb) * 0.5f, mz = (za + zb) * 0.5f, len = (float) Math.sqrt((xb - xa) * (xb - xa) + step * step);
            float[] t = yaw((float) Math.atan2(xb - xa, zb - za));
            float low = PH[0];
            d.box(mx, (City.WATER - 0.6f + low) * 0.5f, mz, CREST_HALF + SIDE, (City.WATER - 0.6f - low) * 0.5f, len * 0.5f + 0.2f, t, 0xC9A97C,
                    DrawList.GRAIN);
            d.box(mx, (CREST + City.WATER - 0.6f) * 0.5f, mz, CREST_HALF + SIDE * 0.5f, (CREST - City.WATER + 0.6f) * 0.5f, len * 0.5f + 0.2f, t,
                    0xDABF8E, DrawList.GRAIN);
            d.box(mx, CREST - 0.05f, mz, CREST_HALF, 0.05f, len * 0.5f + 0.2f, t, 0xE6CC9C, DrawList.GRAIN);
        }
    }

    /** The island in facets, ring on ring of its profile, round in sectors: sand, rock, grass. */
    private static void slopes(DrawList d) {
        int sectors = 64;
        for (int s = 0; s < sectors; s++) {
            float a = (s + 0.5f) * 2f * (float) Math.PI / sectors, w = wob(a);
            float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
            for (int k = 1; k < PR.length; k++) {
                float r0 = PR[k - 1] * w, r1 = PR[k] * w, h0 = PH[k - 1], h1 = PH[k];
                float run = r0 - r1, rise = h1 - h0, slope = rise / run, norm = (float) Math.sqrt(1f + slope * slope);
                float rm = (r0 + r1) * 0.5f, ym = (h0 + h1) * 0.5f, thick = 0.6f;
                float wide = (float) Math.PI * r0 / sectors * 1.12f, longHalf = run * 0.5f * norm * 1.03f;
                // its own x along the ring, its z down the slope, its y out of it
                float zx = sn / norm, zy = -slope / norm, zz = cs / norm;
                float yx = slope * sn / norm, yy = 1f / norm, yz = slope * cs / norm;
                float[] turn = frame(cs, 0f, -sn, yx, yy, yz, zx, zy, zz);
                float cx = IX + rm * sn - yx * thick, cy = ym - yy * thick, cz = IZ + rm * cs - yz * thick;
                int color;
                int mat;
                float n = noise(s, k);
                if (ym < City.WATER) {
                    color = Street.mix(0xB89A6E, 0xC4A679, n);
                    mat = DrawList.GRAIN;
                } else if (k <= 3) {
                    color = Street.mix(0xE0C597, 0xE8CFA2, n);
                    mat = DrawList.GRAIN;
                } else if (slope > 1f) {
                    color = Street.mix(0xB07A55, 0xC28A60, n);
                    mat = DrawList.STONE;
                } else {
                    color = Street.mix(0x6E9447, 0x86A953, n);
                    mat = DrawList.GRASS;
                }
                d.box(cx, cy, cz, wide, thick, longHalf, turn, color, mat);
            }
        }
    }

    // ---- the stairs: straight up from the pier's foot to the top, in from the sand (as far out
    // as the profile is drawn before the wobble) to the top's edge; how wide, half; a tread how
    // deep; how high a rail, its posts so far apart ----
    static final float STAIRS_TO = PR[12] - 2f, STAIRS_HALF = 1.8f, TREAD = 0.8f, RAIL_HIGH = 1.1f, POST_EVERY = 4f;

    private static float stairsFrom() {
        return PR[4] + 1f;
    }

    /** Whether (x, z) is on the stairs, or within so far of them. */
    static boolean onStairs(float x, float z, float within) {
        float dx = x - IX, dz = z - IZ, sa = (float) Math.sin(PIER_A), ca = (float) Math.cos(PIER_A);
        float along = dx * sa + dz * ca, across = dx * ca - dz * sa, w = wob(PIER_A);
        return along > (STAIRS_TO - within) * w && along < (PR[3] + within) * w && Math.abs(across) < STAIRS_HALF + within;
    }

    /**
     * The stairs: up the rock in stone steps, a tread at a time, each as high as the rock rises
     * over it; across each ledge a strip of trodden gravel; wooden rails on posts both sides all
     * the way, to be seen from the pier and from the top.
     */
    private static void stairs(DrawList d) {
        float sa = (float) Math.sin(PIER_A), ca = (float) Math.cos(PIER_A), w = wob(PIER_A);
        float[] t = yaw(PIER_A);
        // across, to the right going up
        float ox = -ca, oz = sa;
        float from = PR[3], to = STAIRS_TO;
        for (float r = from; r > to; r -= TREAD) {
            float r0 = r * w, r1 = Math.max(to, r - TREAD) * w, rm = (r0 + r1) * 0.5f;
            float x0 = IX + sa * r0, z0 = IZ + ca * r0, x1 = IX + sa * r1, z1 = IZ + ca * r1;
            float h0 = height(x0, z0), h1 = height(x1, z1), xm = IX + sa * rm, zm = IZ + ca * rm;
            float rise = h1 - h0, half = (r0 - r1) * 0.5f;
            if (rise > 0.2f) {
                // a stone step: its top a little over the rock's middle, down to the foot of the rise
                float top = (h0 + h1) * 0.5f + rise * 0.25f + 0.05f, low = h0 - 0.3f;
                d.box(xm, (top + low) * 0.5f, zm, STAIRS_HALF, (top - low) * 0.5f, half, t, 0xC9BFAE, DrawList.STONE);
                d.box(xm - sa * (half - 0.06f), top + 0.01f, zm - ca * (half - 0.06f), STAIRS_HALF, 0.02f, 0.06f, t, 0xA89C88, DrawList.STONE);
            } else {
                float y = Math.max(h0, h1) + 0.06f;
                d.box(xm, y, zm, STAIRS_HALF, 0.06f, half + 0.05f, t, 0xD9C08E, DrawList.GRAVEL);
            }
        }
        // the rails: posts both sides, a rail from post to post
        for (int e = -1; e <= 1; e += 2) {
            float px = 0f, py = 0f, pz = 0f;
            boolean first = true;
            for (float r = from; r >= to - 0.01f; r -= POST_EVERY) {
                float rr = Math.max(to, r) * w, x = IX + sa * rr + ox * e * (STAIRS_HALF + 0.15f), z = IZ + ca * rr + oz * e * (STAIRS_HALF + 0.15f);
                float g = height(x, z), y = g + RAIL_HIGH;
                d.box(x, g + RAIL_HIGH * 0.5f, z, 0.08f, RAIL_HIGH * 0.5f + 0.3f, 0.08f, null, 0x6B4A30, DrawList.WOOD);
                if (!first) {
                    float len = (float) Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
                    d.box((x + px) * 0.5f, (y + py) * 0.5f, (z + pz) * 0.5f, 0.06f, 0.06f, len * 0.5f + 0.06f, along(px, py, pz, x, y, z), 0x8A5F3C,
                            DrawList.WOOD);
                }
                first = false;
                px = x;
                py = y;
                pz = z;
            }
        }
        // across the top to the lighthouse's door: a strip of gravel, piece by piece on the ground
        float[] at = new float[3];
        door(at);
        float sx = IX + sa * to * w, sz = IZ + ca * to * w;
        int pieces = 12;
        float qx = sx, qz = sz, qy = height(sx, sz) + 0.06f;
        for (int k = 1; k <= pieces; k++) {
            float u = k / (float) pieces, x = sx + (at[0] - sx) * u, z = sz + (at[2] - sz) * u, y = height(x, z) + 0.06f;
            float len = (float) Math.sqrt((x - qx) * (x - qx) + (y - qy) * (y - qy) + (z - qz) * (z - qz));
            d.box((x + qx) * 0.5f, (y + qy) * 0.5f, (z + qz) * 0.5f, STAIRS_HALF, 0.06f, len * 0.5f + 0.1f, along(qx, qy, qz, x, y, z), 0xD9C08E,
                    DrawList.GRAVEL);
            qx = x;
            qy = y;
            qz = z;
        }
    }

    /** Trees on the ledges, thick on them, none on the rock or the sand, the stairs or the top. */
    private static void trees(DrawList d) {
        Random rnd = new Random(0x15A1DL);
        for (int k = 0; k < 1100; k++) {
            float a = rnd.nextFloat() * 6.2832f, r = rnd.nextFloat() * PR[4] * 0.98f;
            float w = wob(a), x = IX + (float) Math.sin(a) * r * w, z = IZ + (float) Math.cos(a) * r * w;
            // on a ledge or the top: where the ground is nearly level
            float h = height(x, z), hx = height(x + 1f, z), hz = height(x, z + 1f);
            if (Math.abs(hx - h) > 0.6f || Math.abs(hz - h) > 0.6f || h < 1f) continue;
            if (onPath(x, z, 2.2f) || onStairs(x, z, 3f)) continue;
            // none on the top (the last ledge and the round over it): a clear place round the
            // lighthouse, open to the town
            if (r < PR[11] + 2f) continue;
            Garden.tree(d, rnd, x, h, z, 5f + rnd.nextFloat() * 4f);
        }
    }

    // the path: from the spit's end round and up to the top, so far round, so many pieces
    private static final float PATH_TURNS = 1.15f;
    private static final int PATH_PIECES = 90;

    private static float pathA(float u) {
        float a0 = (float) Math.atan2(spitX(SPIT_TO) - IX, SPIT_TO - IZ);
        return a0 + u * PATH_TURNS * 2f * (float) Math.PI;
    }

    /** How far out the path is at u: from the shore in to the lighthouse's door. */
    private static float pathR(float u) {
        return PR[3] * (1f - u) + (PLINTH + 1.5f) * u;
    }

    /** Whether (x, z) is within so far of the path. */
    private static boolean onPath(float x, float z, float within) {
        for (int k = 0; k <= PATH_PIECES; k++) {
            float u = k / (float) PATH_PIECES, a = pathA(u), r = pathR(u) * wob(a);
            float px = IX + (float) Math.sin(a) * r, pz = IZ + (float) Math.cos(a) * r;
            if ((px - x) * (px - x) + (pz - z) * (pz - z) < within * within) return true;
        }
        return false;
    }

    /** The path: a strip of trodden sand on the ground, round and up from the shore to the top. */
    private static void path(DrawList d) {
        float px = 0f, py = 0f, pz = 0f;
        for (int k = 0; k <= PATH_PIECES; k++) {
            float u = k / (float) PATH_PIECES, a = pathA(u), r = pathR(u) * wob(a);
            float x = IX + (float) Math.sin(a) * r, z = IZ + (float) Math.cos(a) * r, y = height(x, z) + 0.1f;
            if (k > 0) {
                float len = (float) Math.sqrt((x - px) * (x - px) + (y - py) * (y - py) + (z - pz) * (z - pz));
                d.box((x + px) * 0.5f, (y + py) * 0.5f, (z + pz) * 0.5f, 1.1f, 0.08f, len * 0.5f + 0.15f, along(px, py, pz, x, y, z), 0xD9C08E,
                        DrawList.GRAVEL);
            }
            px = x;
            py = y;
            pz = z;
        }
    }

    /** The pier: a deck of planks on posts, out from the shore, a cross piece at its end. */
    private static void pierDrawn(DrawList d) {
        float[] p = pier();
        float[] t = yaw((float) Math.atan2(p[2], p[3]));
        float mx = p[0] + p[2] * PIER_LONG * 0.5f, mz = p[1] + p[3] * PIER_LONG * 0.5f;
        d.box(mx, DECK - 0.12f, mz, PIER_HALF, 0.12f, PIER_LONG * 0.5f + 2f, t, 0x9A6B43, DrawList.material(DrawList.WOOD, 3));
        float ex = p[0] + p[2] * PIER_LONG, ez = p[1] + p[3] * PIER_LONG;
        d.box(ex, DECK - 0.12f, ez, PIER_HALF * 3f, 0.12f, PIER_HALF, t, 0x9A6B43, DrawList.material(DrawList.WOOD, 5));
        for (float s = 0f; s <= PIER_LONG; s += 4f)
            for (int e = -1; e <= 1; e += 2) {
                float x = p[0] + p[2] * s + p[4] * e * (PIER_HALF - 0.2f), z = p[1] + p[3] * s + p[5] * e * (PIER_HALF - 0.2f);
                float bed = Math.max(PH[0], height(x, z));
                d.box(x, (DECK + bed) * 0.5f, z, 0.16f, (DECK - bed) * 0.5f, 0.16f, null, 0x6B4A30, DrawList.WOOD);
            }
        // bollards along it, where the yachts are tied
        for (int k = 0; k < YACHTS; k++) {
            float along = 12f + (k / 2) * 2.4f * HULL_HALF + 2f, e = k % 2 == 0 ? 1f : -1f;
            float x = p[0] + p[2] * along + p[4] * e * (PIER_HALF - 0.3f), z = p[1] + p[3] * along + p[5] * e * (PIER_HALF - 0.3f);
            d.box(x, DECK + 0.25f, z, 0.18f, 0.25f, 0.18f, null, 0x2B2E33, DrawList.POLISHED);
        }
    }

    private static final float[] YACHT = new float[3];

    /** The yachts at the pier, each rising and falling a little on the swell, as time goes; or their wrecks. */
    static void yachts(DrawList d, float time) {
        if (wrecked) {
            wrecks(d, time);
            return;
        }
        for (int k = 0; k < YACHTS; k++) {
            yachtAt(k, YACHT);
            float x = YACHT[0], z = YACHT[1], h = YACHT[2], bob = 0.09f * (float) Math.sin(time * 1.1f + k * 1.7f);
            float fx = (float) Math.sin(h), fz = (float) Math.cos(h);
            float[] t = yaw(h);
            float deck = YACHT_DECK + bob, keel = City.WATER - 0.7f + bob;
            // the hull: white, a band of colour along it; the bow drawn to a point
            d.box(x - fx * 0.6f, (deck + keel) * 0.5f, z - fz * 0.6f, BEAM, (deck - keel) * 0.5f, HULL_HALF - 0.6f, t, 0xF2F0EA, DrawList.PAINT);
            float[] bow = yaw(h + 0.785f);
            d.box(x + fx * (HULL_HALF - 1.65f), (deck + keel) * 0.5f, z + fz * (HULL_HALF - 1.65f), 1.06f, (deck - keel) * 0.5f, 1.06f, bow, 0xF2F0EA,
                    DrawList.PAINT);
            d.box(x - fx * 0.6f, deck - 0.32f, z - fz * 0.6f, BEAM + 0.02f, 0.08f, HULL_HALF - 0.6f + 0.02f, t, STRIPES[k], DrawList.PAINT);
            // the deck of teak, the cabin with its windows, the mast and its boom
            d.box(x - fx * 0.6f, deck + 0.03f, z - fz * 0.6f, BEAM - 0.15f, 0.03f, HULL_HALF - 0.8f, t, 0xB07A4A, DrawList.WOOD);
            d.box(x - fx * 0.4f, deck + 0.45f, z - fz * 0.4f, BEAM * 0.65f, 0.42f, 1.6f, t, 0xF7F5F0, DrawList.PAINT);
            d.box(x - fx * 0.4f, deck + 0.55f, z - fz * 0.4f, BEAM * 0.66f, 0.12f, 1.4f, t, 0x1D2730, DrawList.GLASS);
            float mx = x + fx * 1.4f, mz = z + fz * 1.4f;
            d.box(mx, deck + MAST * 0.5f, mz, 0.08f, MAST * 0.5f, 0.08f, null, 0xD9DCDF, DrawList.POLISHED);
            d.box(mx - fx * 1.8f, deck + 1.5f, mz - fz * 1.8f, 0.07f, 0.07f, 1.8f, t, 0xD9DCDF, DrawList.POLISHED);
            // the sail furled on the boom, under its cover
            d.box(mx - fx * 1.8f, deck + 1.7f, mz - fz * 1.8f, 0.16f, 0.14f, 1.7f, t, STRIPES[k], DrawList.MATTE);
        }
    }

    /**
     * The yachts gone up: each broken in two at its middle, the stern sunk to its rail, the bow
     * reared out of the water; burnt black, the band gone dark; the mast snapped, its stump on the
     * stern, the rest down across the water.
     */
    private static void wrecks(DrawList d, float time) {
        int char0 = 0x4A4542, char1 = 0x2E2B2A, teak = 0x3A2A20;
        for (int k = 0; k < YACHTS; k++) {
            yachtAt(k, YACHT);
            float x = YACHT[0], z = YACHT[1], h = YACHT[2], fx = (float) Math.sin(h), fz = (float) Math.cos(h);
            float side = k % 2 == 0 ? 1f : -1f, ox = fz * side, oz = -fx * side;
            float bob = 0.05f * (float) Math.sin(time * 0.9f + k * 1.7f), w = City.WATER + bob;
            // the stern: from its end, a little under the water, down at the break
            float sx = x - fx * HULL_HALF, sz = z - fz * HULL_HALF, bx = x + fx * 0.2f + ox * 0.3f, bz = z + fz * 0.2f + oz * 0.3f;
            float[] t = along(sx, w + 0.25f, sz, bx, w - 0.9f, bz);
            d.box((sx + bx) * 0.5f, w - 0.55f, (sz + bz) * 0.5f, BEAM, 0.75f, HULL_HALF * 0.5f, t, char0, DrawList.MATTE);
            d.box((sx + bx) * 0.5f, w + 0.05f, (sz + bz) * 0.5f, BEAM - 0.2f, 0.05f, HULL_HALF * 0.5f - 0.2f, t, teak, DrawList.WOOD);
            // the bow: reared up from the break, turned a little off
            float ex = x + fx * (HULL_HALF + 0.6f) + ox * 0.9f, ez = z + fz * (HULL_HALF + 0.6f) + oz * 0.9f;
            float[] u = along(bx + fx * 0.6f, w - 1.1f, bz + fz * 0.6f, ex, w + 1.6f, ez);
            d.box((bx + ex) * 0.5f + fx * 0.3f, w + 0.15f, (bz + ez) * 0.5f + fz * 0.3f, BEAM * 0.9f, 0.65f, HULL_HALF * 0.5f, u, char1, DrawList.MATTE);
            d.box((bx + ex) * 0.5f + fx * 0.3f, w - 0.15f, (bz + ez) * 0.5f + fz * 0.3f, BEAM * 0.92f, 0.07f, HULL_HALF * 0.5f + 0.02f, u, 0x241F1E, DrawList.MATTE);
            // the mast: a stump on the stern, the rest fallen across the water, out from the pier
            float mx = x - fx * 1.6f, mz = z - fz * 1.6f;
            d.box(mx, w + 1.3f, mz, 0.09f, 1.4f, 0.09f, null, 0x55585C, DrawList.POLISHED);
            float lx = mx + ox * 7.5f + fx * 2f, lz = mz + oz * 7.5f + fz * 2f;
            d.box((mx + lx) * 0.5f, w + 0.9f, (mz + lz) * 0.5f, 0.08f, 0.08f, 4f, along(mx, w + 2.6f, mz, lx, w - 0.8f, lz), 0x55585C, DrawList.POLISHED);
        }
    }

    // ---- turns ----
    /** A turn whose z runs level along a heading (0 along +z, growing toward +x). */
    private static float[] yaw(float a) {
        return along(0f, 0f, 0f, (float) Math.sin(a), 0f, (float) Math.cos(a));
    }

    private static float[] frame(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz) {
        return new float[]{xx, yx, zx, xy, yy, zy, xz, yz, zz};
    }

    private static float[] along(float ax, float ay, float az, float bx, float by, float bz) {
        float zx = bx - ax, zy = by - ay, zz = bz - az, l = (float) Math.sqrt(zx * zx + zy * zy + zz * zz);
        zx /= l;
        zy /= l;
        zz /= l;
        float xx = zz, xz = -zx, xl = (float) Math.sqrt(xx * xx + xz * xz);
        xx /= xl;
        xz /= xl;
        float yx = zy * xz, yy = zz * xx - zx * xz, yz = -zy * xx;
        return frame(xx, 0f, xz, yx, yy, yz, zx, zy, zz);
    }

    private static float noise(int k, int salt) {
        int h = k * 73856093 ^ salt * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535f;
    }
}
