package com.shumtugle.skydysh;

/**
 * The mountain to the north-west, past the town's last street: a cone as even as a volcano's, its
 * snow high up, cut off flat at the top; on the flat, the laboratory's tower. A road winds up it
 * from the town's last street, twice round, as the rings go round a snail's shell: laid flat across
 * on the slope, its inner edge on the mountain's face, its outer on a wall of stone, a rail along
 * it. From the top a cable way goes straight down over the town to the beach: a station by the
 * tower, a pylon in the first block from the sea, a station on the sand; two cabins on it, one up
 * as the other goes down.
 *
 * All of it is worked out, not built of tiles: how high the ground is anywhere on it (the road, or
 * the slope), where the rails stand; the town asks it (see City.ground, City.country).
 */
final class Mountain {
    private Mountain() {
    }

    // the mountain: its middle, its foot's reach, the flat on top (how high, how far across)
    static final float MX = -480f, MZ = 930f, R = 270f, TOP = 150f, PLATEAU = 30f;
    // the road: half its width, how many times round, where it starts (on the south side, a
    // heading as the town's go: 0 along +z), so far a piece
    static final float HALF = 4.5f, TURNS = 2f, A0 = (float) Math.PI, PIECE = 6f;
    static final float SPAN = TURNS * 2f * (float) Math.PI;
    // the rails along it, so far apart, how high
    static final float POST_EVERY = 1.4f, POST_HALF = 0.85f, RAIL_HIGH = 1.1f;

    /** How high the road is so far round (a heading from A0 on). */
    static float rise(float t) {
        return TOP * (t - A0) / SPAN;
    }

    /** How far from the middle the slope is at height y. */
    static float coneR(float y) {
        return R - (R - PLATEAU) * y / TOP;
    }

    /** How far from the middle the road's middle is, so far round: its inner edge on the slope. */
    static float roadR(float t) {
        return coneR(rise(t)) + HALF;
    }

    /** The slope's height at (x, z): nothing past the foot, the flat on top. */
    static float cone(float x, float z) {
        float dx = x - MX, dz = z - MZ, r = (float) Math.sqrt(dx * dx + dz * dz);
        if (r >= R) return 0f;
        if (r <= PLATEAU) return TOP;
        return TOP * (R - r) / (R - PLATEAU);
    }

    /** The road's foot: straight up from the town's last street to where it starts round. */
    static float footZ() {
        return MZ - roadR(A0);
    }

    /** The road's height at (x, z); NaN if (x, z) is not on it. */
    static float road(float x, float z) {
        float dx = x - MX, dz = z - MZ;
        if (Math.abs(dx) < HALF && z > City.EDGE_J * City.CELL && z < footZ() + HALF) return 0f;
        float r = (float) Math.sqrt(dx * dx + dz * dz);
        if (r > R + 2f * HALF + 1f || r < PLATEAU - 1f) return Float.NaN;
        float t = (float) Math.atan2(dx, dz);
        float two = 2f * (float) Math.PI;
        while (t < A0) t += two;
        while (t >= A0 + two) t -= two;
        for (int k = 0; k <= TURNS; k++, t += two) {
            if (t > A0 + SPAN) break;
            if (Math.abs(r - roadR(t)) < HALF) return rise(t);
        }
        return Float.NaN;
    }

    /** Whether (x, z) is anywhere on the mountain or its road. */
    static boolean near(float x, float z) {
        float dx = x - MX, dz = z - MZ;
        return dx * dx + dz * dz < (R + 2f * HALF + 2f) * (R + 2f * HALF + 2f) || Math.abs(dx) < HALF && z < MZ;
    }

    /** How high the ground is at (x, z): the road where it is, else the slope. */
    static float ground(float x, float z) {
        if (!near(x, z)) return 0f;
        float g = road(x, z);
        return Float.isNaN(g) ? cone(x, z) : g;
    }

    /** Whether (x, z) is up on the mountain's side of the town's last street: its road's foot, its slope, its top. */
    static boolean onHill(float x, float z) {
        return z > City.EDGE_J * City.CELL + City.CURB && near(x, z);
    }

    /**
     * The way down from (x, z) on the mountain to the town's last street, as points into out (x, z
     * each), from the top's end of the road (or the nearest bit of road) down round to its foot and
     * the street; how many. Read backward, the way up.
     */
    static int down(float x, float z, float[] out) {
        float dx = x - MX, dz = z - MZ, r = (float) Math.sqrt(dx * dx + dz * dz);
        int n = 0;
        if (!(Math.abs(dx) < HALF + 2f && z < footZ() + HALF)) {
            float t;
            if (r <= PLATEAU + 2f) t = A0 + SPAN;
            else {
                // the nearest round of the road to her
                float two = 2f * (float) Math.PI, a = (float) Math.atan2(dx, dz);
                while (a < A0) a += two;
                while (a >= A0 + two) a -= two;
                t = a;
                float best = Math.abs(r - roadR(a));
                for (float u = a + two; u <= A0 + SPAN; u += two)
                    if (Math.abs(r - roadR(u)) < best) {
                        best = Math.abs(r - roadR(u));
                        t = u;
                    }
            }
            while (t > A0 && n * 2 + 2 < out.length - 4) {
                float rr = roadR(t);
                out[n * 2] = MX + rr * (float) Math.sin(t);
                out[n * 2 + 1] = MZ + rr * (float) Math.cos(t);
                n++;
                t -= 12f / rr;
            }
        }
        out[n * 2] = MX;
        out[n * 2 + 1] = footZ();
        n++;
        out[n * 2] = MX;
        out[n * 2 + 1] = City.EDGE_J * City.CELL;
        n++;
        return n;
    }

    // ---- the rails: posts along both edges of the road, each a footing nothing passes ----
    private static float[] posts;
    private static int postN;

    private static void posts() {
        if (posts != null) return;
        float[] p = new float[3 * 4096];
        int n = 0;
        float t = A0;
        while (t < A0 + SPAN) {
            float rr = roadR(t), y = rise(t), s = (float) Math.sin(t), c = (float) Math.cos(t);
            for (int e = -1; e <= 1; e += 2) {
                if (n * 3 + 3 > p.length) p = java.util.Arrays.copyOf(p, p.length * 2);
                float rp = rr + e * (HALF + 0.35f);
                // none across the foot of the road, where it comes in from the town
                if (e > 0 && t < A0 + 0.04f) continue;
                p[n * 3] = MX + rp * s;
                p[n * 3 + 1] = MZ + rp * c;
                p[n * 3 + 2] = y + RAIL_HIGH;
                n++;
            }
            t += POST_EVERY / rr;
        }
        posts = p;
        postN = n;
    }

    /** The rails' posts in the cell (i, j), as its walls. */
    static void walls(City.Cell c, int i, int j) {
        posts();
        float x0 = i * City.CELL, z0 = j * City.CELL;
        for (int k = 0; k < postN; k++) {
            float x = posts[k * 3], z = posts[k * 3 + 1];
            if (x < x0 || x >= x0 + City.CELL || z < z0 || z >= z0 + City.CELL) continue;
            c.wall(x - POST_HALF, z - POST_HALF, x + POST_HALF, z + POST_HALF, posts[k * 3 + 2], -2f);
        }
    }

    // the laboratory's podium (half across, how high), the steps up to it on the town's side (how
    // many, half across, how deep each, each so much higher), its stem (half across, how high)
    static final float PODIUM = 13f, PODIUM_HIGH = 2f;
    // (each no higher than she steps up without a jump, City.LOW)
    static final int STAIRS = 5;
    static final float STAIR_HALF = 3.5f, STAIR_DEEP = 0.8f, STAIR_RISE = PODIUM_HIGH / (STAIRS + 1);
    static final float STEM = 4.6f, STEM_HIGH = 26f;
    // the cable way's halls: half across, half deep; how high each stands (its roof)
    static final float HALL_HALF = 5f, TOP_HALL_HIGH = 7.6f, LOW_HALL_DEEP = 5.5f, LOW_HALL_HIGH = 7f;

    /**
     * What stands up solid where it is drawn, where it stands in cell (i, j): the laboratory's
     * podium and the steps up to it, its stem; the cable way's hall on the top and its hall on the
     * sand. Without them she would stand in the podium to her chest, and walk through all of them.
     */
    static void solids(City.Cell c, int i, int j) {
        float x0 = i * City.CELL, z0 = j * City.CELL, x1 = x0 + City.CELL, z1 = z0 + City.CELL;
        solid(c, x0, z0, x1, z1, MX - PODIUM, MZ - PODIUM, MX + PODIUM, MZ + PODIUM, TOP + PODIUM_HIGH);
        for (int k = 1; k <= STAIRS; k++)
            solid(c, x0, z0, x1, z1, MX - STAIR_HALF, MZ - PODIUM - k * STAIR_DEEP, MX + STAIR_HALF, MZ - PODIUM - (k - 1) * STAIR_DEEP,
                    TOP + PODIUM_HIGH - k * STAIR_RISE);
        solid(c, x0, z0, x1, z1, MX - STEM, MZ - STEM, MX + STEM, MZ + STEM, TOP + STEM_HIGH);
        float tz = TOP_Z + 6f, lz = LOW_Z - 4f;
        solid(c, x0, z0, x1, z1, CABLE_X - HALL_HALF, tz - HALL_HALF, CABLE_X + HALL_HALF, tz + HALL_HALF, TOP + TOP_HALL_HIGH);
        solid(c, x0, z0, x1, z1, CABLE_X - HALL_HALF, lz - LOW_HALL_DEEP, CABLE_X + HALL_HALF, lz + LOW_HALL_DEEP, City.SAND + LOW_HALL_HIGH);
    }

    /** A solid block (ax, az)–(bx, bz) up to top, the part of it inside the cell (x0, z0)–(x1, z1). */
    private static void solid(City.Cell c, float x0, float z0, float x1, float z1, float ax, float az, float bx, float bz, float top) {
        float l = Math.max(x0, ax), r = Math.min(x1, bx), n = Math.max(z0, az), f = Math.min(z1, bz);
        if (l < r && n < f) c.wall(l, n, r, f, top);
    }

    // ---- drawn ----
    private static DrawList far, close;

    /** The mountain: the slope, the tower and the cable way always; the road on it only near. */
    static void draw(DrawList d, float x, float z) {
        if (far == null) build();
        d.append(far);
        float dx = x - MX, dz = z - MZ;
        if (dx * dx + dz * dz < (R + 260f) * (R + 260f)) d.append(close);
    }

    /** A turn with these as its own x, y and z (each a direction in the world). */
    private static float[] frame(float xx, float xy, float xz, float yx, float yy, float yz, float zx, float zy, float zz) {
        return new float[]{xx, yx, zx, xy, yy, zy, xz, yz, zz};
    }

    /** A turn whose z runs from a to b (its x level, across), for a piece laid from a to b. */
    private static float[] along(float ax, float ay, float az, float bx, float by, float bz) {
        float zx = bx - ax, zy = by - ay, zz = bz - az, l = (float) Math.sqrt(zx * zx + zy * zy + zz * zz);
        zx /= l;
        zy /= l;
        zz /= l;
        float xx = zz, xz = -zx, xl = (float) Math.sqrt(xx * xx + xz * xz);
        if (xl < 1e-4f) {
            xx = 1f;
            xz = 0f;
            xl = 1f;
        }
        xx /= xl;
        xz /= xl;
        // y = z cross x
        float yx = zy * xz - zz * 0f, yy = zz * xx - zx * xz, yz = zx * 0f - zy * xx;
        return frame(xx, 0f, xz, yx, yy, yz, zx, zy, zz);
    }

    private static float noise(int k, int salt) {
        int h = k * 73856093 ^ salt * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535f;
    }

    private static void build() {
        far = new DrawList();
        close = new DrawList();
        slope(far);
        top(far);
        laboratory(far);
        cableWay(far);
        road(close);
    }

    /** The slope in facets, ring on ring: grass low, then scree and rock, snow high up, ragged at its edge. */
    private static void slope(DrawList d) {
        int sectors = 56, rings = 9;
        float k = TOP / (R - PLATEAU), norm = (float) Math.sqrt(1f + k * k);
        for (int a = 0; a < sectors; a++) {
            float t = (a + 0.5f) * 2f * (float) Math.PI / sectors, s = (float) Math.sin(t), c = (float) Math.cos(t);
            float snowLine = 96f + noise(a, 3) * 22f + (float) Math.sin(t * 3f) * 6f;
            for (int g = 0; g < rings; g++) {
                float r0 = R - (R - PLATEAU) * g / rings, r1 = R - (R - PLATEAU) * (g + 1) / rings, rm = (r0 + r1) * 0.5f;
                float ym = TOP * (R - rm) / (R - PLATEAU);
                float wide = (float) Math.PI * r0 / sectors * 1.08f, longHalf = (r0 - r1) * 0.5f * norm * 1.02f, thick = 0.6f;
                // its own x along the ring, its z down the slope, its y out of it
                float zx = s / norm, zy = -k / norm, zz = c / norm;
                float yx = k * s / norm, yy = 1f / norm, yz = k * c / norm;
                float[] turn = frame(c, 0f, -s, yx, yy, yz, zx, zy, zz);
                float cx = MX + rm * s - yx * thick, cy = ym - yy * thick, cz = MZ + rm * c - yz * thick;
                int color;
                float mat;
                if (ym > snowLine) {
                    color = Street.mix(0xEEF1F2, 0xDDE4EA, noise(a * 13 + g, 5));
                    mat = DrawList.STONE;
                } else if (ym > snowLine - 40f) {
                    color = Street.mix(0x8A8276, 0x9C9284, noise(a * 7 + g, 6));
                    mat = DrawList.STONE;
                } else {
                    color = Street.mix(0x6E9447, 0x7E9E52, noise(a * 5 + g, 7));
                    mat = DrawList.GRASS;
                }
                d.box(cx, cy, cz, wide, thick, longHalf, turn, color, mat);
            }
        }
    }

    /** The flat on top: paved, a low wall round its edge but where the road comes up. */
    private static void top(DrawList d) {
        float[] diag = {0.7071f, 0f, 0.7071f, 0f, 1f, 0f, -0.7071f, 0f, 0.7071f};
        float h = PLATEAU * 0.93f;
        d.box(MX, TOP - 0.5f, MZ, h, 0.5f, h, null, 0xB9B2A4, DrawList.PAVING);
        d.box(MX, TOP - 0.5f, MZ, h, 0.5f, h, diag, 0xB9B2A4, DrawList.PAVING);
    }

    // a letter, five dots across and seven down, a row a string ('#' lit)
    private static final String[][] LETTERS = {
        {"#....", "#....", "#....", "#....", "#....", "#....", "#####"},
        {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"},
        {"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."},
        {".....", ".....", ".....", ".....", ".....", ".....", "....."},
        {"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"},
        {"#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"},
        {".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."},
        {".....", ".....", ".....", ".....", ".....", "..##.", "..##."},
    };

    /**
     * The laboratory: a podium, a stem of pale concrete with a lit slit up its front and its door
     * at the foot; on it a ring of dark glass, lit in a band, its name lit on its front in
     * turquoise; over the ring a dome of copper gone green in bands; a mast on the dome, a light
     * at its tip, a dish on it turned to the sky.
     */
    private static void laboratory(DrawList d) {
        int concrete = 0xC9C3B6, copper = 0xB8733A, verdigris = 0x5FA899, glass = 0x24484E, neon = 0x7FF6E8;
        float y0 = TOP;
        d.box(MX, y0 + PODIUM_HIGH * 0.5f, MZ, PODIUM, PODIUM_HIGH * 0.5f, PODIUM, null, 0xA9A397, DrawList.STONE);
        // the steps up to it on the town's side, toward the road's end
        for (int k = 1; k <= STAIRS; k++) {
            float hy = (PODIUM_HIGH - k * STAIR_RISE) * 0.5f;
            d.box(MX, y0 + hy, MZ - PODIUM - (k - 0.5f) * STAIR_DEEP, STAIR_HALF, hy, STAIR_DEEP * 0.5f, null,
                    Street.mix(0xA9A397, 0xB9B2A4, k / (float) STAIRS), DrawList.STONE);
        }
        float s0 = y0 + 2f, s1 = y0 + 26f;
        octagon(d, MX, (s0 + s1) * 0.5f, MZ, 5f, (s1 - s0) * 0.5f, concrete, DrawList.STONE);
        d.box(MX, (s0 + 6f + s1 - 2f) * 0.5f, MZ - 5.05f, 0.5f, (s1 - s0 - 8f) * 0.5f, 0.05f, null, 0xFFE9BE, DrawList.GLOW);
        // the door
        d.box(MX, s0 + 1.6f, MZ - 5.08f, 1.5f, 1.6f, 0.06f, null, 0x1D2730, DrawList.GLASS);
        d.box(MX, s0 + 3.35f, MZ - 5.1f, 1.7f, 0.12f, 0.08f, null, neon, DrawList.GLOW);
        // the ring
        float r0 = s1, r1 = s1 + 6f;
        octagon(d, MX, (r0 + r1) * 0.5f, MZ, 11f, (r1 - r0) * 0.5f, glass, DrawList.GLASS);
        octagon(d, MX, r0 + 0.3f, MZ, 11.3f, 0.3f, copper, DrawList.GOLD);
        octagon(d, MX, r1 - 0.2f, MZ, 11.4f, 0.2f, copper, DrawList.GOLD);
        octagon(d, MX, r0 + 1.1f, MZ, 11.05f, 0.2f, 0xBFF7F0, DrawList.GLOW);
        // its name on its front, toward the town
        String word = "LAB INC.";
        float dot = 0.42f, across = word.length() * 6f * dot - dot, lx = MX - across * 0.5f, ly = r1 - 1.4f, lz = MZ - 11.1f;
        for (int k = 0; k < word.length(); k++) {
            char ch = word.charAt(k);
            int g = ch == 'L' ? 0 : ch == 'A' ? 1 : ch == 'B' ? 2 : ch == 'I' ? 4 : ch == 'N' ? 5 : ch == 'C' ? 6 : ch == '.' ? 7 : 3;
            for (int row = 0; row < 7; row++)
                for (int col = 0; col < 5; col++)
                    if (LETTERS[g][row].charAt(col) == '#')
                        d.box(lx + (k * 6 + col) * dot + dot * 0.5f, ly - row * dot, lz, dot * 0.45f, dot * 0.45f, 0.05f, null, neon, DrawList.GLOW);
        }
        // the dome, in steps, copper and green
        float dy = r1;
        for (int k = 0; k < 6; k++) {
            float rr = 10.5f * (float) Math.cos(k * 0.27f), hh = 1.4f;
            int col = k % 2 == 0 ? copper : verdigris;
            octagon(d, MX, dy + hh * 0.5f, MZ, rr, hh * 0.5f, col, DrawList.GOLD);
            dy += hh;
        }
        // the mast, its light, the dish
        d.box(MX, dy + 6f, MZ, 0.18f, 6f, 0.18f, null, 0xD8D8D0, DrawList.POLISHED);
        d.box(MX, dy + 12.3f, MZ, 0.35f, 0.35f, 0.35f, null, 0xFF4A3A, DrawList.GLOW);
        float[] tilt = frame(1f, 0f, 0f, 0f, 0.766f, -0.643f, 0f, 0.643f, 0.766f);
        d.box(MX, dy + 6f, MZ - 2.2f, 2.6f, 0.15f, 2.6f, tilt, 0xE8E6DE, DrawList.PLAIN);
        d.box(MX, dy + 6f, MZ - 1.2f, 0.12f, 0.12f, 1.1f, null, 0x8A8A86, DrawList.POLISHED);
    }

    private static final float[][] EIGHTH = {
        null, {0.7071f, 0f, 0.7071f, 0f, 1f, 0f, -0.7071f, 0f, 0.7071f}};

    /**
     * Eight sides round (x, z), each so far out from the middle, standing at y, so high each way:
     * four bands crossed at an eighth of a turn, each as long as across and as wide as a side.
     */
    private static void octagon(DrawList d, float x, float y, float z, float r, float hy, int color, int material) {
        float side = r * 0.4142f;
        for (float[] t : EIGHTH) {
            d.box(x, y, z, r, hy, side, t, color, material);
            d.box(x, y, z, side, hy, r, t, color, material);
        }
    }

    // ---- the cable way ----
    // where it runs across (off the road's end, so the way from it to the tower's door is clear;
    // two ropes either side of it), the top station's front, the pylon in
    // the first block, the station on the sand; how high the ropes are at each
    static final float CABLE_X = MX + 14f, ROPES = 1.6f;
    static final float TOP_Z = MZ - PLATEAU + 2f, TOP_Y = TOP + 6f;
    static final float PYLON_Z = 30f, PYLON_Y = 41f;
    static final float LOW_Z = -24f, LOW_Y = City.SAND + 6.2f;

    /** The stations, the pylon, the ropes. */
    private static void cableWay(DrawList d) {
        int concrete = 0xC9C3B6, steel = 0x6E7378, red = 0xB0302A;
        // the top station: a hall on the flat's south edge, open to the south, the wheel in it
        float tz = TOP_Z + 6f;
        d.box(CABLE_X, TOP + 3.5f, tz, 5f, 3.5f, 5f, null, concrete, DrawList.STONE);
        d.box(CABLE_X, TOP + 7.3f, tz - 0.8f, 5.8f, 0.3f, 6f, null, red, DrawList.PAINT);
        d.box(CABLE_X, TOP + 4f, tz - 5.02f, 3.2f, 2.6f, 0.04f, null, 0x1D2730, DrawList.PLAIN);
        d.box(CABLE_X, TOP + 6.2f, tz - 4.6f, 2.6f, 0.15f, 0.3f, null, steel, DrawList.POLISHED);
        // the pylon: four legs leaning in, rungs between, a cross arm at the top for the ropes
        float base = City.WALK_TOP, top = PYLON_Y - 0.6f;
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2) {
                float x0 = CABLE_X + a * 2.4f, z0 = PYLON_Z + b * 2.4f, x1 = CABLE_X + a * 0.9f, z1 = PYLON_Z + b * 0.9f;
                d.box((x0 + x1) * 0.5f, (base + top) * 0.5f, (z0 + z1) * 0.5f, 0.16f, 0.16f, (top - base) * 0.5f,
                        along(x0, base, z0, x1, top, z1), steel, DrawList.POLISHED);
            }
        for (float y = base + 4f; y < top - 2f; y += 5f) {
            float w = 2.4f - 1.5f * (y - base) / (top - base);
            d.box(CABLE_X, y, PYLON_Z - w, w, 0.08f, 0.08f, null, steel, DrawList.POLISHED);
            d.box(CABLE_X, y, PYLON_Z + w, w, 0.08f, 0.08f, null, steel, DrawList.POLISHED);
            d.box(CABLE_X - w, y, PYLON_Z, 0.08f, 0.08f, w, null, steel, DrawList.POLISHED);
            d.box(CABLE_X + w, y, PYLON_Z, 0.08f, 0.08f, w, null, steel, DrawList.POLISHED);
        }
        d.box(CABLE_X, top + 0.3f, PYLON_Z, ROPES + 0.6f, 0.3f, 0.5f, null, red, DrawList.PAINT);
        // the station on the sand: a hall on short legs, its roof, its name lit
        float lz = LOW_Z - 4f, floor = City.SAND;
        d.box(CABLE_X, floor + 3.8f, lz, 5f, 3.2f, 5.5f, null, concrete, DrawList.STONE);
        d.box(CABLE_X, floor + 7.2f, lz, 5.6f, 0.25f, 6.1f, null, red, DrawList.PAINT);
        d.box(CABLE_X, floor + 4.6f, lz + 5.52f, 3f, 1.6f, 0.04f, null, 0x1D2730, DrawList.PLAIN);
        d.box(CABLE_X, floor + 1.6f, lz + 5.53f, 1.2f, 1.6f, 0.04f, null, 0x2A3A48, DrawList.GLASS);
        for (int a = -1; a <= 1; a += 2) d.box(CABLE_X + a * 4.6f, floor + 0.3f, lz, 0.3f, 0.3f, 5.3f, null, 0xA9A397, DrawList.STONE);
        // the ropes, two spans either side
        for (int e = -1; e <= 1; e += 2) {
            float x = CABLE_X + e * ROPES;
            rope(d, x, TOP_Y, TOP_Z, x, PYLON_Y, PYLON_Z);
            rope(d, x, PYLON_Y, PYLON_Z, x, LOW_Y, LOW_Z);
        }
    }

    private static void rope(DrawList d, float ax, float ay, float az, float bx, float by, float bz) {
        float l = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        d.box((ax + bx) * 0.5f, (ay + by) * 0.5f, (az + bz) * 0.5f, 0.05f, 0.05f, l * 0.5f, along(ax, ay, az, bx, by, bz), 0x2B2F3A, DrawList.PLAIN);
    }

    /** Where a cabin is, so far down (0 at the top, 1 at the sand), on the rope across at x: into out (x, y, z). */
    static void onRope(float s, float x, float[] out) {
        float l1 = (float) Math.hypot(TOP_Z - PYLON_Z, TOP_Y - PYLON_Y), l2 = (float) Math.hypot(PYLON_Z - LOW_Z, PYLON_Y - LOW_Y);
        float d = s * (l1 + l2);
        out[0] = x;
        if (d < l1) {
            float a = d / l1;
            out[1] = TOP_Y + (PYLON_Y - TOP_Y) * a;
            out[2] = TOP_Z + (PYLON_Z - TOP_Z) * a;
        } else {
            float a = (d - l1) / l2;
            out[1] = PYLON_Y + (LOW_Y - PYLON_Y) * a;
            out[2] = PYLON_Z + (LOW_Z - PYLON_Z) * a;
        }
    }

    // a trip, how long, and how long a cabin waits in a station
    static final float TRIP = 140f, WAIT = 10f;
    private static final float[] at = new float[3];

    /** The two cabins at time: one going down as the other comes up, each waiting at the ends. */
    static void cabins(DrawList d, float time) {
        float cycle = 2f * (TRIP + WAIT), c = time % cycle, s;
        if (c < WAIT) s = 0f;
        else if (c < WAIT + TRIP) s = ease((c - WAIT) / TRIP);
        else if (c < 2f * WAIT + TRIP) s = 1f;
        else s = 1f - ease((c - 2f * WAIT - TRIP) / TRIP);
        cabin(d, s, CABLE_X - ROPES);
        cabin(d, 1f - s, CABLE_X + ROPES);
    }

    private static float ease(float a) {
        float e = 0.06f;
        if (a < e) return a * a / (2f * e) / (1f - e);
        if (a > 1f - e) return 1f - (1f - a) * (1f - a) / (2f * e) / (1f - e);
        return (a - e * 0.5f) / (1f - e);
    }

    /** A cabin hung from its rope: the wheels on it, the arm down, the cabin red with its windows round. */
    private static void cabin(DrawList d, float s, float x) {
        onRope(s, x, at);
        float y = at[1], z = at[2];
        d.box(x, y + 0.15f, z, 0.25f, 0.2f, 0.9f, null, 0x4A4E54, DrawList.POLISHED);
        d.box(x, y - 1.2f, z, 0.07f, 1.2f, 0.07f, null, 0x4A4E54, DrawList.POLISHED);
        float cy = y - 3.6f;
        d.box(x, cy, z, 1.3f, 1.2f, 1.9f, null, 0xC0392B, DrawList.PAINT);
        d.box(x, cy + 0.35f, z, 1.32f, 0.5f, 1.92f, null, 0x2A3A48, DrawList.GLASS);
        d.box(x, cy + 1.3f, z, 1.4f, 0.1f, 2f, null, 0xEEEAE0, DrawList.PLAIN);
    }

    /**
     * The road up, in pieces: the asphalt, a dashed line on it, the stone wall under its outer
     * edge down to the slope, the rail along that edge, a low kerb of rock along the inner; and the
     * foot of it, straight from the town's last street.
     */
    private static void road(DrawList d) {
        int asphalt = 0x5E4844, stone = 0x9A8F80, rail = 0xD9D9D2, kerb = 0x8A8276;
        float fz0 = City.EDGE_J * City.CELL + City.ROAD, fz1 = footZ();
        d.box(MX, -0.12f, (fz0 + fz1) * 0.5f, HALF, 0.12f, (fz1 - fz0) * 0.5f + HALF, null, asphalt, DrawList.ASPHALT);
        float t = A0;
        int n = 0;
        while (t < A0 + SPAN) {
            float rr = roadR(t), t1 = Math.min(A0 + SPAN, t + PIECE / rr), r1 = roadR(t1);
            float ax = MX + rr * (float) Math.sin(t), ay = rise(t), az = MZ + rr * (float) Math.cos(t);
            float bx = MX + r1 * (float) Math.sin(t1), by = rise(t1), bz = MZ + r1 * (float) Math.cos(t1);
            float[] turn = along(ax, ay, az, bx, by, bz);
            float l = (float) Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
            float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
            // which way across is out from the mountain
            float ox = (mx - MX) / rr, oz = (mz - MZ) / rr, side = turn[0] * ox + turn[6] * oz > 0f ? 1f : -1f;
            float xx = turn[0] * side, xz = turn[6] * side;
            d.box(mx, my - 0.15f, mz, HALF, 0.15f, l * 0.5f + 0.12f, turn, asphalt, DrawList.ASPHALT);
            if (n % 2 == 0) d.box(mx, my + 0.005f, mz, 0.07f, 0.01f, l * 0.3f, turn, 0xF2EEDF, DrawList.PAINT);
            float wy = my - 3.4f;
            d.box(mx + xx * (HALF + 0.3f), wy, mz + xz * (HALF + 0.3f), 0.3f, 3.4f, l * 0.5f + 0.15f, turn, stone, DrawList.STONE);
            d.box(mx + xx * (HALF + 0.3f), my + 0.5f, mz + xz * (HALF + 0.3f), 0.05f, 0.5f, l * 0.5f + 0.05f, turn, rail,
                    DrawList.RAIL);
            d.box(mx - xx * (HALF + 0.4f), my + 0.25f, mz - xz * (HALF + 0.4f), 0.4f, 0.4f, l * 0.5f, turn, kerb, DrawList.STONE);
            t = t1;
            n++;
        }
    }
}
