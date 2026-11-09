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
        if (z < SPIT_FROM + 2f && z > SPIT_TO - 30f && Math.abs(z - GAP_Z) > GAP_HALF) {
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

    // ---- the way up, for the navigator: from the pier's foot through the tunnel, up the
    // right-hand flight, across the top to the lighthouse's door; its points so far apart ----
    static final float CLIMB_STEP = 6f;
    private static float[] climb;
    private static int climbN;

    private static void climb() {
        if (climb != null) return;
        float[] p = pier();
        Tunnel t = tunnel();
        float[] pts = flights()[0];
        int every = Math.max(1, Math.round(CLIMB_STEP / TREAD)), fn = (pts.length / 2 - 1) / every + 1;
        int tn = (int) Math.ceil((t.len + 5f) / CLIMB_STEP);
        climbN = 1 + tn + 1 + fn + 1;
        climb = new float[climbN * 2];
        int n = 0;
        climb[n++] = p[0];
        climb[n++] = p[1];
        // through the tunnel, a point every so far, from just outside its mouth to just past the far one
        float[] q = new float[2];
        for (int k = 0; k <= tn; k++) {
            t.at(Math.min(t.len + 3f, -2f + k * CLIMB_STEP), 0f, q);
            climb[n++] = q[0];
            climb[n++] = q[1];
        }
        for (int k = 0; k < fn; k++) {
            int i = Math.min(pts.length / 2 - 1, k * every);
            climb[n++] = pts[i * 2];
            climb[n++] = pts[i * 2 + 1];
        }
        float[] at = new float[3];
        door(at);
        climb[n++] = at[0];
        climb[n] = at[2];
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
        // the lighthouse: its plinth as wide as it is drawn, the tower over it up to the gallery,
        // the gallery's deck to stand on (as wide as its flats), the lantern and the roof over it
        square(c, x0, z0, x1, z1, IX, IZ, PLINTH, LH_FOOT + PLINTH_HIGH, 0f);
        square(c, x0, z0, x1, z1, IX, IZ, TOWER_LOW, GALLERY_Y - 1f, 0f);
        square(c, x0, z0, x1, z1, IX, IZ, GALLERY_R * 0.92f, GALLERY_Y, GALLERY_Y - 1f);
        square(c, x0, z0, x1, z1, IX, IZ, LANTERN_R + 0.2f, LH_TOP, GALLERY_Y);
        // the bridge's leaf, down: a deck across the channel; its banks either side
        if (bridgeUp == 0f) {
            float gx = spitX(GAP_Z);
            for (float z = GAP_Z - GAP_HALF - 2f * ABUT; z < GAP_Z + GAP_HALF + 2f * ABUT; z += 2f)
                square(c, x0, z0, x1, z1, gx, z + 1f, CREST_HALF, BRIDGE_DECK, PH[0]);
        } else {
            float gx = spitX(GAP_Z);
            for (int e = -1; e <= 1; e += 2)
                square(c, x0, z0, x1, z1, gx, GAP_Z + e * (GAP_HALF + ABUT), CREST_HALF, BRIDGE_DECK, PH[0]);
        }
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
        tunnelDrawn(far);
        pierDrawn(far);
        lighthouse(far);
    }

    // ---- the lighthouse on the top: its foot (how far round the plinth, how high), its tower
    // (round at the foot, at the top, how high over the plinth), the gallery, the lantern, the roof ----
    static final float LH_FOOT = PH[PH.length - 1] - 0.8f, PLINTH = 4f, PLINTH_HIGH = 1.6f;
    static final float TOWER_LOW = 3.2f, TOWER_TOP = 2.3f, TOWER_HIGH = 16f;
    static final float GALLERY_R = 3.4f, LANTERN_R = 1.5f, LANTERN_HIGH = 2.2f;
    /** The gallery's deck, to stand on. */
    static final float GALLERY_Y = LH_FOOT + PLINTH_HIGH + TOWER_HIGH + 0.3f;
    static final float LH_TOP = LH_FOOT + PLINTH_HIGH + TOWER_HIGH + 0.3f + LANTERN_HIGH + 2.4f;

    /** The way the door looks: toward the path's end on the top, square to the flat nearest it. */
    /** Whether one at (x, y, z) heading h is at the lighthouse's door, going to it. */
    static boolean atDoor(float x, float y, float z, float h) {
        float[] at = new float[3];
        door(at);
        float dx = x - at[0], dz = z - at[2];
        if (dx * dx + dz * dz > DOOR_NEAR * DOOR_NEAR || Math.abs(y - at[1]) > 1.5f) return false;
        float ix = IX - x, iz = IZ - z, il = Math.max(1e-3f, (float) Math.sqrt(ix * ix + iz * iz));
        return ((float) Math.sin(h) * ix + (float) Math.cos(h) * iz) / il > 0.3f;
    }

    // so near the lighthouse's door she goes in
    static final float DOOR_NEAR = 2.2f;

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
            float za = Math.min(z, z > GAP_Z ? z : GAP_Z - GAP_HALF), zb = z - step, xa, xb;
            // the channel cut through it for the bridge: the pieces stop at its banks
            if (za > GAP_Z - GAP_HALF && zb < GAP_Z + GAP_HALF) {
                if (za > GAP_Z + GAP_HALF) zb = GAP_Z + GAP_HALF;
                else continue;
            }
            xa = spitX(za);
            xb = spitX(zb);
            float step0 = za - zb;
            float mx = (xa + xb) * 0.5f, mz = (za + zb) * 0.5f, len = (float) Math.sqrt((xb - xa) * (xb - xa) + step0 * step0);
            float[] t = yaw((float) Math.atan2(xb - xa, zb - za));
            float low = PH[0];
            d.box(mx, (City.WATER - 0.6f + low) * 0.5f, mz, CREST_HALF + SIDE, (City.WATER - 0.6f - low) * 0.5f, len * 0.5f + 0.2f, t, 0xC9A97C,
                    DrawList.GRAIN);
            d.box(mx, (CREST + City.WATER - 0.6f) * 0.5f, mz, CREST_HALF + SIDE * 0.5f, (CREST - City.WATER + 0.6f) * 0.5f, len * 0.5f + 0.2f, t,
                    0xDABF8E, DrawList.GRAIN);
            d.box(mx, CREST - 0.05f, mz, CREST_HALF, 0.05f, len * 0.5f + 0.2f, t, 0xE6CC9C, DrawList.GRAIN);
        }
        // the channel's banks: walls of dressed stone, from the bed up to the crest, a step over it
        float gx = spitX(GAP_Z);
        for (int e = -1; e <= 1; e += 2) {
            float bz = GAP_Z + e * (GAP_HALF + ABUT);
            d.box(gx, (PH[0] + BRIDGE_DECK) * 0.5f, bz, CREST_HALF + SIDE, (BRIDGE_DECK - PH[0]) * 0.5f, ABUT, null, 0xA89C88, DrawList.STONE);
            d.box(gx, BRIDGE_DECK + 0.02f, bz, CREST_HALF + 0.2f, 0.02f, ABUT, null, 0x8C7F70, DrawList.STONE);
        }
        // the bridge's frame on the island's bank: two posts and a beam over, where its leaf hangs
        float hz = GAP_Z - GAP_HALF - ABUT;
        for (int e = -1; e <= 1; e += 2)
            d.box(gx + e * (CREST_HALF + 0.5f), BRIDGE_DECK + 3f, hz, 0.35f, 3f, 0.35f, null, 0x55585C, DrawList.POLISHED);
        d.box(gx, BRIDGE_DECK + 6.1f, hz, CREST_HALF + 0.9f, 0.25f, 0.35f, null, 0x55585C, DrawList.POLISHED);
    }

    // ---- the bridge over the channel in the spit near the island: where the channel is along
    // the spit (a share of the way from the beach), half as wide; the banks' stone, so deep; the
    // deck's top, a step over the crest ----
    static final float GAP_Z = SPIT_FROM - 0.72f * (SPIT_FROM - SPIT_TO), GAP_HALF = 5f, ABUT = 1f, BRIDGE_DECK = CREST + 0.3f;
    // how far up the leaf is now (0 down, 1 up), and where it is going; how long it takes, seconds
    static float bridgeUp, bridgeTo;
    static final float RAISE_S = 5f;
    // the leaf stands up so steeply at most, radians
    static final float LEAF_UP = 1.35f;

    /** Whether (x, z) is on the bridge's leaf (down) or its banks. */
    static boolean onBridge(float x, float z) {
        return Math.abs(z - GAP_Z) < GAP_HALF + 2f * ABUT && Math.abs(x - spitX(GAP_Z)) < CREST_HALF + 0.4f;
    }

    /** The bridge's leaf going up or down; true the moment it leaves the deck or comes down on it. */
    static boolean bridgeStep(float dt) {
        float was = bridgeUp;
        if (bridgeUp < bridgeTo) bridgeUp = Math.min(bridgeTo, bridgeUp + dt / RAISE_S);
        else if (bridgeUp > bridgeTo) bridgeUp = Math.max(bridgeTo, bridgeUp - dt / RAISE_S);
        return (was == 0f) != (bridgeUp == 0f);
    }

    /**
     * The bridge's leaf: planks between two steel stringers, rails along both sides, hinged at the
     * island's bank; down, it lies across the channel; up, it stands tilted back over the bank.
     */
    static void bridge(DrawList d) {
        float gx = spitX(GAP_Z), hz = GAP_Z - GAP_HALF - ABUT, len = 2f * (GAP_HALF + ABUT);
        float a = bridgeUp * bridgeUp * (3f - 2f * bridgeUp) * LEAF_UP;
        float tz = hz + (float) Math.cos(a) * len, ty = BRIDGE_DECK + (float) Math.sin(a) * len;
        float[] t = along(gx, BRIDGE_DECK, hz, gx, ty, tz);
        float mz = (hz + tz) * 0.5f, my = (BRIDGE_DECK + ty) * 0.5f;
        // the planks, a little under the deck's top, the stringers under their edges
        float ny = (float) Math.cos(a), nz = -(float) Math.sin(a);
        d.box(gx, my - ny * 0.12f, mz - nz * 0.12f, CREST_HALF, 0.12f, len * 0.5f, t, 0x9A6B43, DrawList.material(DrawList.WOOD, 3));
        for (int e = -1; e <= 1; e += 2) {
            d.box(gx + e * (CREST_HALF - 0.1f), my - ny * 0.4f, mz - nz * 0.4f, 0.12f, 0.25f, len * 0.5f, t, 0x3C4148, DrawList.POLISHED);
            d.box(gx + e * (CREST_HALF - 0.05f), my + ny * 0.9f, mz + nz * 0.9f, 0.05f, 0.05f, len * 0.5f, t, 0xC24A3B, DrawList.PAINT);
            for (float s = -len * 0.5f + 0.5f; s < len * 0.5f; s += 2f) {
                float py = my + ny * 0.45f + (float) Math.sin(a) * s, pz = mz + nz * 0.45f + (float) Math.cos(a) * s;
                d.box(gx + e * (CREST_HALF - 0.05f), py, pz, 0.04f, 0.45f, 0.04f, t, 0x55585C, DrawList.RAIL);
            }
        }
    }

    /** The island shut: the bridge up for good, the yachts wrecked, the brums left on it. */
    static boolean shut;

    // the brums left on the island, standing about on its sand by the pier: so many, round from
    // here, so far apart round, so far out
    static final float KEPT_FROM = 0.75f, KEPT_STEP = 0.045f, KEPT_R = 104.5f;

    /** The brums left on the island once it is shut: standing about on the sand by the pier, ticking, near the eye. */
    static void kept(DrawList d, float time, float ex, float ez) {
        if (!shut) return;
        Frida.hot = true;
        for (int k = 0; k < Herd.ALL; k++) {
            float a = KEPT_FROM + k * KEPT_STEP, r = (KEPT_R + (k % 3) * 1.6f) * wob(a);
            float x = IX + (float) Math.sin(a) * r, z = IZ + (float) Math.cos(a) * r;
            if ((x - ex) * (x - ex) + (z - ez) * (z - ez) > 160f * 160f) continue;
            Frida.draw(d, x, height(x, z), z, a + (float) Math.PI + 0.6f * (float) Math.sin(time * 0.3f + k), Chase.SIZE, "idle", time + k, false);
        }
        Frida.hot = false;
    }

    // the slopes go round in so many pieces
    static final int SECTORS = 64;

    /** The heading of the middle of the slopes' piece s. */
    static float sectorA(int s) {
        return (s + 0.5f) * 2f * (float) Math.PI / SECTORS;
    }

    /** How far out the profile's ring k is (before the wobble). */
    static float ring(int k) {
        return PR[k];
    }

    private static Tunnel tunnel;

    /** The tunnel through the island. */
    static Tunnel tunnel() {
        if (tunnel == null) tunnel = new Tunnel();
        return tunnel;
    }

    /** The island in facets, ring on ring of its profile, round in sectors: sand, rock, grass. */
    private static void slopes(DrawList d) {
        int sectors = SECTORS;
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
                // where the tunnel's mouths are, the rock's piece is left out: the portal stands there
                if (s == Tunnel.SECTOR && k == Tunnel.IN_FACE || s == (Tunnel.SECTOR + sectors / 2) % sectors && k == Tunnel.OUT_FACE) continue;
                d.box(cx, cy, cz, wide, thick, longHalf, turn, color, mat);
            }
        }
    }

    // ---- the stairs: from the tunnel's far mouth, right and left, round and up to the top;
    // how far out they start, how far in they end, how far round each goes; how wide, half; a
    // tread how deep; how high a rail, its posts so far apart ----
    static final float FLIGHT_FROM = 70f, FLIGHT_TO = 30f, FLIGHT_ROUND = 2.618f;
    static final float STAIRS_HALF = 1.8f, TREAD = 0.8f, RAIL_HIGH = 1.1f, POST_EVERY = 4f;
    private static float[][] flights;

    /** The two flights' points (x, z, x, z ...), a tread apart: the first round to the right, the second to the left. */
    static float[][] flights() {
        if (flights != null) return flights;
        flights = new float[2][];
        float a0 = sectorA(Tunnel.SECTOR) + (float) Math.PI;
        for (int f = 0; f < 2; f++) {
            float e = f == 0 ? 1f : -1f;
            // so many treads that each is about one long
            float mid = (FLIGHT_FROM + FLIGHT_TO) * 0.5f, far = (float) Math.hypot(mid * FLIGHT_ROUND, FLIGHT_FROM - FLIGHT_TO);
            int n = (int) Math.ceil(far / TREAD);
            float[] pts = new float[(n + 1) * 2];
            for (int k = 0; k <= n; k++) {
                float u = k / (float) n, a = a0 + e * u * FLIGHT_ROUND, r = (FLIGHT_FROM + (FLIGHT_TO - FLIGHT_FROM) * u) * wob(a);
                pts[k * 2] = IX + (float) Math.sin(a) * r;
                pts[k * 2 + 1] = IZ + (float) Math.cos(a) * r;
            }
            flights[f] = pts;
        }
        return flights;
    }

    /** Whether (x, z) is within so far of either of the tunnel's mouths. */
    static boolean nearPortal(float x, float z, float within) {
        Tunnel t = tunnel();
        float s = t.along(x, z), c = Math.abs(t.across(x, z));
        return c < Tunnel.HALF + within && (Math.abs(s) < within || Math.abs(s - t.len) < within);
    }

    /** Whether (x, z) is on one of the flights, or within so far of it. */
    static boolean onStairs(float x, float z, float within) {
        float lim = (STAIRS_HALF + within) * (STAIRS_HALF + within);
        for (float[] pts : flights())
            for (int k = 0; k < pts.length; k += 2) {
                float dx = pts[k] - x, dz = pts[k + 1] - z;
                if (dx * dx + dz * dz < lim) return true;
            }
        return false;
    }

    /**
     * The stairs: each flight up the rock in stone steps, a tread at a time, each as high as the
     * rock rises over it; across each ledge a strip of trodden gravel; wooden rails on posts both
     * sides all the way; at the top, gravel on to the lighthouse's door. From the far mouth to the
     * flights' feet, gravel too.
     */
    private static void stairs(DrawList d) {
        for (float[] pts : flights()) {
            flight(d, pts);
            float[] at = new float[3];
            door(at);
            int last = pts.length - 2;
            gravel(d, pts[last], pts[last + 1], at[0], at[2], 12);
        }
        Tunnel t = tunnel();
        float[] q = new float[2];
        t.at(t.len, 0f, q);
        float[] f0 = flights()[0];
        gravel(d, q[0], q[1], f0[0], f0[1], 4);
    }

    /** A strip of gravel from (x0, z0) to (x1, z1), in so many pieces, each on the ground. */
    private static void gravel(DrawList d, float x0, float z0, float x1, float z1, int pieces) {
        float qx = x0, qz = z0, qy = height(x0, z0) + 0.06f;
        for (int k = 1; k <= pieces; k++) {
            float u = k / (float) pieces, x = x0 + (x1 - x0) * u, z = z0 + (z1 - z0) * u, y = height(x, z) + 0.06f;
            float len = (float) Math.sqrt((x - qx) * (x - qx) + (y - qy) * (y - qy) + (z - qz) * (z - qz));
            d.box((x + qx) * 0.5f, (y + qy) * 0.5f, (z + qz) * 0.5f, STAIRS_HALF, 0.06f, len * 0.5f + 0.1f, along(qx, qy, qz, x, y, z), 0xD9C08E,
                    DrawList.GRAVEL);
            qx = x;
            qy = y;
            qz = z;
        }
    }

    /** One flight along its points: steps where the rock rises, gravel where it is level, rails both sides. */
    private static void flight(DrawList d, float[] pts) {
        int n = pts.length / 2;
        for (int k = 0; k + 1 < n; k++) {
            float x0 = pts[k * 2], z0 = pts[k * 2 + 1], x1 = pts[k * 2 + 2], z1 = pts[k * 2 + 3];
            float h0 = height(x0, z0), h1 = height(x1, z1), xm = (x0 + x1) * 0.5f, zm = (z0 + z1) * 0.5f;
            float run = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (z1 - z0) * (z1 - z0)), half = run * 0.5f + 0.02f;
            float[] t = yaw((float) Math.atan2(x1 - x0, z1 - z0));
            float rise = h1 - h0;
            if (Math.abs(rise) > 0.2f) {
                // a stone step: its top a little over the rock's middle, down to the foot of the rise
                float hi = Math.max(h0, h1), lo = Math.min(h0, h1);
                float top = (h0 + h1) * 0.5f + Math.abs(rise) * 0.25f + 0.05f, low = lo - 0.3f;
                d.box(xm, (top + low) * 0.5f, zm, STAIRS_HALF, (top - low) * 0.5f, half, t, 0xC9BFAE, DrawList.STONE);
                float ex = rise > 0f ? -1f : 1f, sx = (x1 - x0) / run, sz = (z1 - z0) / run;
                d.box(xm + ex * sx * (half - 0.06f), top + 0.01f, zm + ex * sz * (half - 0.06f), STAIRS_HALF, 0.02f, 0.06f, t, 0xA89C88, DrawList.STONE);
                if (hi > top + 0.5f) d.box(xm, (hi + top) * 0.5f, zm, STAIRS_HALF, (hi - top) * 0.5f, half * 0.5f, t, 0xB8AC98, DrawList.STONE);
            } else {
                float y = Math.max(h0, h1) + 0.06f;
                d.box(xm, y, zm, STAIRS_HALF, 0.06f, half + 0.05f, t, 0xD9C08E, DrawList.GRAVEL);
            }
        }
        // the rails: posts both sides, a rail from post to post
        int every = Math.max(1, Math.round(POST_EVERY / TREAD));
        for (int e = -1; e <= 1; e += 2) {
            float px = 0f, py = 0f, pz = 0f;
            boolean first = true;
            for (int k = 0; k < n; k += every) {
                int k2 = Math.min(n - 1, k + 1), k1 = k2 == k ? k - 1 : k;
                float dx = pts[k2 * 2] - pts[k1 * 2], dz = pts[k2 * 2 + 1] - pts[k1 * 2 + 1], dl = (float) Math.sqrt(dx * dx + dz * dz);
                float ox = dz / dl, oz = -dx / dl;
                float x = pts[k * 2] + ox * e * (STAIRS_HALF + 0.15f), z = pts[k * 2 + 1] + oz * e * (STAIRS_HALF + 0.15f);
                float g = height(x, z), y = g + RAIL_HIGH;
                d.box(x, g + RAIL_HIGH * 0.5f, z, 0.08f, RAIL_HIGH * 0.5f + 0.3f, 0.08f, null, 0x6B4A30, DrawList.WOOD);
                if (!first && Math.abs(y - py) < 6f) {
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
    }

    // ---- the tunnel drawn: its floor, walls and roof piece by piece, lamps on its roof and
    // walls; a portal of dressed stone round each mouth, as wide as the rock's piece left out
    // there and as high as the rock face, so far deep ----
    static final float TUNNEL_PIECE = 3f, LAMP_EVERY = 12f, PORTAL_DEEP = 1.8f;

    private static void tunnelDrawn(DrawList d) {
        Tunnel t = tunnel();
        float h = Tunnel.HIGH, w = Tunnel.HALF;
        float[] p0 = new float[2], p1 = new float[2];
        for (float s = -1f; s < t.len + 1f; s += TUNNEL_PIECE) {
            float s1 = Math.min(t.len + 1f, s + TUNNEL_PIECE), f0 = t.floor(s), f1 = t.floor(s1);
            t.at(s, 0f, p0);
            t.at(s1, 0f, p1);
            float[] fr = along(p0[0], f0, p0[1], p1[0], f1, p1[1]);
            float len = (float) Math.sqrt((s1 - s) * (s1 - s) + (f1 - f0) * (f1 - f0)) * 0.5f + 0.05f;
            float mx = (p0[0] + p1[0]) * 0.5f, mz = (p0[1] + p1[1]) * 0.5f, fm = (f0 + f1) * 0.5f;
            d.box(mx, fm - 0.15f, mz, w + 0.4f, 0.15f, len, fr, 0x6E655C, DrawList.STONE);
            d.box(mx, fm + h + 0.25f, mz, w + 0.4f, 0.25f, len, fr, 0x5A524B, DrawList.STONE);
            for (int e = -1; e <= 1; e += 2) {
                float wx = mx + t.vx * e * (w + 0.2f), wz = mz + t.vz * e * (w + 0.2f);
                d.box(wx, fm + h * 0.5f, wz, 0.2f, h * 0.5f + 0.3f, len, fr, 0x8C7F70, DrawList.STONE);
            }
        }
        // the lamps: one in the roof, one on each wall between, warm
        for (float s = LAMP_EVERY * 0.5f; s < t.len; s += LAMP_EVERY) {
            float f = t.floor(s);
            t.at(s, 0f, p0);
            d.box(p0[0], f + h - 0.06f, p0[1], 0.5f, 0.06f, 0.5f, null, 0xFFE2A0, DrawList.GLOW);
            for (int e = -1; e <= 1; e += 2) {
                t.at(s + LAMP_EVERY * 0.5f, e * (w - 0.05f), p0);
                d.box(p0[0], t.floor(s + LAMP_EVERY * 0.5f) + 2.6f, p0[1], 0.12f, 0.2f, 0.12f, null, 0xFFC870, DrawList.GLOW);
            }
        }
        portal(d, t, 0f, -1f, IN_FACE_TOP());
        portal(d, t, t.len, 1f, OUT_FACE_TOP());
    }

    private static float IN_FACE_TOP() {
        return PH[Tunnel.IN_FACE] + 0.5f;
    }

    private static float OUT_FACE_TOP() {
        return PH[Tunnel.OUT_FACE] + 0.5f;
    }

    /**
     * A portal at so far in (s), facing out (out: -1 at the pier's mouth, 1 at the far one): dressed
     * stone up to the top of the rock face, as wide as the rock's piece left out there, the mouth
     * open at its foot, a lintel over it, a band of darker stone round it.
     */
    private static void portal(DrawList d, Tunnel t, float s, float out, float top) {
        float f = t.floor(s), w = Tunnel.HALF, h = Tunnel.HIGH;
        float r = (float) Math.hypot(t.ex + t.ux * s - IX, t.ez + t.uz * s - IZ);
        float wide = (float) Math.PI * r / SECTORS * 1.15f;
        float mid = s - out * (PORTAL_DEEP - 0.3f);
        float[] fr = yaw((float) Math.atan2(t.ux, t.uz)), p = new float[2];
        int stone = 0xB3A690, dark = 0x7C6F60;
        // the sides of the mouth, from its edge out to the rock's on either side
        for (int e = -1; e <= 1; e += 2) {
            float c = e * (w + (wide - w) * 0.5f);
            t.at(mid, c, p);
            d.box(p[0], (top + f - 1f) * 0.5f, p[1], (wide - w) * 0.5f, (top - f + 1f) * 0.5f, PORTAL_DEEP, fr, stone, DrawList.STONE);
            t.at(mid + out * (PORTAL_DEEP - 0.1f), e * (w + 0.3f), p);
            d.box(p[0], f + h * 0.5f, p[1], 0.3f, h * 0.5f, 0.12f, fr, dark, DrawList.STONE);
        }
        // over the mouth, to the top
        t.at(mid, 0f, p);
        d.box(p[0], (top + f + h) * 0.5f, p[1], w + 0.02f, (top - f - h) * 0.5f, PORTAL_DEEP, fr, stone, DrawList.STONE);
        t.at(mid + out * (PORTAL_DEEP - 0.1f), 0f, p);
        d.box(p[0], f + h + 0.3f, p[1], w + 0.6f, 0.3f, 0.12f, fr, dark, DrawList.STONE);
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
            if (onPath(x, z, 2.2f) || onStairs(x, z, 3f) || nearPortal(x, z, 10f)) continue;
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
