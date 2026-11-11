package com.shumtugle.skydysh;

/**
 * The canal: a loop of water out of the sea and back into it. From the beach west of the town's
 * middle it goes up a column of the town in place of its blocks, between the two streets either
 * side, its quays paved along them; past the town's last street it turns east through the fields
 * behind the town, under a high bridge on the road out, and at the east woods turns down through
 * them to the wild shore and the sea. Stone quays the whole way, a coping along their top, a
 * railing. Cars cross it in the town only on two bridges (see DRAW_J), which will lift for a yacht;
 * the other streets come to the quay and stop at a barrier.
 *
 * Its line is a few straight pieces; the water is whatever lies within HALF of it.
 */
final class Canal {
    // its line, from the west mouth round to the east mouth: these corners, each bend rounded on
    // an arc so wide, in so many pieces
    static final float[] CX = {-288f, -288f, -226f, 690f, 736f, 736f};
    static final float[] CZ = {-52f, 700f, 770f, 770f, 724f, -52f};
    static final float ROUND = 34f;
    static final int ARC = 4;
    static final float[] PX, PZ;
    static final int N;

    static {
        int n = 2 + (CX.length - 2) * (ARC + 1);
        float[] px = new float[n], pz = new float[n];
        int m = 0;
        px[m] = CX[0];
        pz[m++] = CZ[0];
        for (int k = 1; k + 1 < CX.length; k++) {
            float ix = CX[k] - CX[k - 1], iz = CZ[k] - CZ[k - 1], il = (float) Math.sqrt(ix * ix + iz * iz);
            float ox = CX[k + 1] - CX[k], oz = CZ[k + 1] - CZ[k], ol = (float) Math.sqrt(ox * ox + oz * oz);
            ix /= il;
            iz /= il;
            ox /= ol;
            oz /= ol;
            float turn = (float) Math.acos(Math.max(-1f, Math.min(1f, ix * ox + iz * oz)));
            float back = ROUND * (float) Math.tan(turn * 0.5f);
            // from where the arc leaves the way in to where it joins the way out, along a curve
            float sx = CX[k] - ix * back, sz = CZ[k] - iz * back, ex = CX[k] + ox * back, ez = CZ[k] + oz * back;
            for (int q = 0; q <= ARC; q++) {
                float t = q / (float) ARC, u = 1f - t;
                // a quadratic through the corner: near enough an arc for so small a turn
                px[m] = u * u * sx + 2f * u * t * CX[k] + t * t * ex;
                pz[m++] = u * u * sz + 2f * u * t * CZ[k] + t * t * ez;
            }
        }
        px[m] = CX[CX.length - 1];
        pz[m++] = CZ[CX.length - 1];
        PX = px;
        PZ = pz;
        N = m;
    }
    // the water's half width; its bed; its walls' thickness; the coping's width along their top,
    // its top; the railing, how far in from the coping's outer edge and how high
    static final float HALF = 10f, BED = City.WATER - 2.6f, WALL = 0.4f, COPING = 1.4f, TOP = City.WALK_TOP;
    static final float RAIL_IN = 0.25f, RAIL_HIGH = 1f;
    /** The town's column it runs up in place of the blocks, and its middle there. */
    static final int COLUMN = -5;
    static final float X_W = CX[0];
    /** The rows whose streets cross it on a bridge for cars (the shore road, and one in the middle). */
    static final int[] DRAW_J = {City.SHORE, City.SHORE + 5};
    // how far round the water what was built there is taken away: whole, small things; ground cut back to
    static final float CUT_SMALL = HALF + COPING + 1.2f, CUT_GROUND = HALF + 0.7f;

    // the high bridge on the road out: where it crosses, the deck's top, how far its ramps run
    // south and north of the water, how far the deck runs flat past the water either side
    static final float ROAD_Z = 770f, DECK_TOP = Island.YACHT_DECK + Island.MAST + 2.4f;
    static final float RAMP_S = 105f, RAMP_N = 72f, FLAT = 4f;

    private Canal() {
    }

    /** How far (x, z) is from the canal's line. */
    static float dist(float x, float z) {
        float best = Float.MAX_VALUE;
        for (int k = 0; k + 1 < N; k++) {
            float ax = PX[k], az = PZ[k], dx = PX[k + 1] - ax, dz = PZ[k + 1] - az;
            float l2 = dx * dx + dz * dz;
            float t = Math.max(0f, Math.min(1f, ((x - ax) * dx + (z - az) * dz) / l2));
            float ex = ax + dx * t - x, ez = az + dz * t - z, d = ex * ex + ez * ez;
            if (d < best) best = d;
        }
        return (float) Math.sqrt(best);
    }

    /** Whether (x, z) is over the canal's water. */
    static boolean water(float x, float z) {
        if (x < CX[0] - HALF - 1f || x > CX[4] + HALF + 1f || z < PZ[0] - 1f || z > ROAD_Z + HALF + 1f) return false;
        return dist(x, z) < HALF;
    }

    /** Whether (x, z) is within m of the water. */
    static boolean near(float x, float z, float m) {
        if (x < CX[0] - HALF - m || x > CX[4] + HALF + m || z < PZ[0] - m || z > ROAD_Z + HALF + m) return false;
        return dist(x, z) < HALF + m;
    }

    /** Whether the canal passes through cell (i, j), or near it. */
    static boolean crosses(int i, int j) {
        float x0 = i * City.CELL, z0 = j * City.CELL, c = City.CELL * 0.5f;
        float r = c * 1.42f + CUT_SMALL + 2f;
        return near(x0 + c, z0 + c, r - HALF);
    }

    /** Whether the street of row j crosses here on a bridge for cars. */
    static boolean drawRow(int j) {
        for (int d : DRAW_J) if (d == j) return true;
        return false;
    }

    /** Whether (x, z) is on the deck of a bridge for cars over the canal in the town. */
    static boolean deck(float x, float z) {
        if (Math.abs(x - X_W) > HALF + COPING) return false;
        int j = Math.round(z / City.CELL);
        return drawRow(j) && Math.abs(z - j * City.CELL) < City.ROAD;
    }

    /** Whether a street from crossing (i, j) a step along di (east-west) crosses the canal. */
    static boolean over(int i, int di) {
        return Math.min(i, i + di) == COLUMN && Math.max(i, i + di) == COLUMN + 1;
    }

    // ---- the road out's high bridge ----

    /** How high the road out stands at z, over the fields: its ramps up to the bridge and down. */
    static float rise(float z) {
        float s0 = ROAD_Z - HALF - COPING - FLAT, n0 = ROAD_Z + HALF + COPING + FLAT;
        if (z <= s0 - RAMP_S || z >= n0 + RAMP_N) return 0f;
        if (z < s0) return DECK_TOP * smooth((z - (s0 - RAMP_S)) / RAMP_S);
        if (z > n0) return DECK_TOP * smooth(1f - (z - n0) / RAMP_N);
        return DECK_TOP;
    }

    private static float smooth(float t) {
        // a little eased at its foot and at its top, straight between
        t = Math.max(0f, Math.min(1f, t));
        float e = 0.12f;
        if (t < e) return t * t / (2f * e) / (1f - e);
        if (t > 1f - e) return 1f - (1f - t) * (1f - t) / (2f * e) / (1f - e);
        return (t - e * 0.5f) / (1f - e);
    }

    /** The road out's surface at (x, z) if (x, z) is on its ramps or its bridge; NaN if not. */
    static float onRoad(float x, float z) {
        float r = rise(z);
        if (r <= 0f) return Float.NaN;
        if (Math.abs(x - City.routeX(z)) > City.ROUTE_HALF + 0.7f) return Float.NaN;
        return r;
    }

    /** A frame whose own z runs along (dx, dy, dz), its x level across it (row by row, a fresh array). */
    static float[] along(float dx, float dy, float dz) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float zx = dx / l, zy = dy / l, zz = dz / l;
        float h = (float) Math.sqrt(zz * zz + zx * zx);
        float xx = zz / h, xy = 0f, xz = -zx / h;
        // y = z cross x
        float yx = zy * xz - zz * xy, yy = zz * xx - zx * xz, yz = zx * xy - zy * xx;
        return new float[]{xx, yx, zx, xy, yy, zy, xz, yz, zz};
    }

    // ---- taking away what was built where the water goes ----

    /**
     * Out of a cell just built, what stands where the canal goes: the ground cut back from the
     * water in strips; small things (a tree, a lamp, a bench, a sunshade) taken away whole if
     * they stand by it; walls likewise; the crowns, lamps, benches and loungers kept for others.
     */
    static void carve(City.Cell c) {
        carve(c.boxes);
        carve(c.detail);
        carve(c.parked);
        carveWalls(c);
        c.crownN = keepAway(c.crowns, c.crownN, 4);
        c.benchN = keepAway(c.benches, c.benchN, 3);
        c.loungerN = keepAway(c.loungers, c.loungerN, 2);
        c.lampN = keepAway(c.lamps, c.lampN, 2);
        c.parkedN = keepAway(c.parkedCars, c.parkedN, 5);
    }

    private static int keepAway(float[] a, int n, int size) {
        int m = 0;
        for (int k = 0; k < n; k++) {
            int o = k * size;
            if (near(a[o], a[o + 1], CUT_SMALL - HALF)) continue;
            if (m != k) System.arraycopy(a, o, a, m * size, size);
            m++;
        }
        return m;
    }

    private static final DrawList KEEP = new DrawList();

    private static void carve(DrawList d) {
        int n = d.count;
        float[] b = d.boxes;
        boolean any = false;
        for (int k = 0; k < n && !any; k++) {
            int o = k * DrawList.STRIDE;
            float r = (float) Math.sqrt(b[o + 3] * b[o + 3] + b[o + 5] * b[o + 5]);
            if (near(b[o], b[o + 2], CUT_SMALL - HALF + r)) any = true;
        }
        if (!any) return;
        KEEP.count = 0;
        for (int k = 0; k < n; k++) {
            int o = k * DrawList.STRIDE;
            float x = b[o], y = b[o + 1], z = b[o + 2], hx = b[o + 3], hy = b[o + 4], hz = b[o + 5];
            float r = (float) Math.sqrt(hx * hx + hz * hz);
            if (!near(x, z, CUT_SMALL - HALF + r)) {
                copy(b, o);
                continue;
            }
            boolean plain = b[o + 6] == 1f && b[o + 10] == 1f && b[o + 14] == 1f;
            if (plain && (hx > 1.6f || hz > 1.6f)) {
                // ground or a long thing on it: cut back from the water in strips, along its length
                boolean alongX = hx >= hz;
                float len = alongX ? hx : hz, wide = alongX ? hz : hx;
                int strips = Math.max(1, (int) Math.ceil(wide * 2f / 1f));
                float sw = wide / strips;
                for (int s = 0; s < strips; s++) {
                    float mid = -wide + (s * 2 + 1) * sw;
                    int steps = Math.max(2, (int) Math.ceil(len * 2f / 0.5f));
                    float step = len * 2f / steps;
                    float from = Float.NaN;
                    for (int q = 0; q <= steps; q++) {
                        float t = -len + q * step;
                        float px = alongX ? x + t : x + mid, pz = alongX ? z + mid : z + t;
                        boolean out = q < steps && !near(px, pz, CUT_GROUND - HALF);
                        if (out && Float.isNaN(from)) from = t;
                        if (!out && !Float.isNaN(from)) {
                            float a = from, e = t;
                            float cx = (a + e) * 0.5f, ch = (e - a) * 0.5f;
                            if (alongX) KEEP.box(x + cx, y, z + mid, ch, hy, sw, null, rgb(b, o), b[o + 18]);
                            else KEEP.box(x + mid, y, z + cx, sw, hy, ch, null, rgb(b, o), b[o + 18]);
                            from = Float.NaN;
                        }
                    }
                }
                continue;
            }
            // a small thing, or a turned one: gone if it stands by the water
            if (near(x, z, CUT_SMALL - HALF)) continue;
            copy(b, o);
        }
        float[] kb = KEEP.boxes;
        if (d.boxes.length < kb.length) d.boxes = new float[kb.length];
        System.arraycopy(kb, 0, d.boxes, 0, KEEP.count * DrawList.STRIDE);
        d.count = KEEP.count;
    }

    private static void copy(float[] b, int o) {
        KEEP.box(b[o], b[o + 1], b[o + 2], b[o + 3], b[o + 4], b[o + 5], null, 0, 0);
        System.arraycopy(b, o, KEEP.boxes, (KEEP.count - 1) * DrawList.STRIDE, DrawList.STRIDE);
    }

    private static int rgb(float[] b, int o) {
        return Math.round(b[o + 15] * 255f) << 16 | Math.round(b[o + 16] * 255f) << 8 | Math.round(b[o + 17] * 255f);
    }

    private static void carveWalls(City.Cell c) {
        float[] w = c.walls;
        int n = c.wallN, m = 0;
        float[] out = new float[Math.max(w.length, (n + 64) * City.Cell.WALL * 3)];
        for (int k = 0; k < n; k++) {
            int o = k * City.Cell.WALL;
            float x0 = w[o], z0 = w[o + 1], x1 = w[o + 2], z1 = w[o + 3];
            float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
            float r = (float) Math.sqrt(hx * hx + hz * hz);
            if (!near(cx, cz, CUT_SMALL - HALF + r)) {
                System.arraycopy(w, o, out, m++ * City.Cell.WALL, City.Cell.WALL);
                continue;
            }
            if (hx > 1.6f || hz > 1.6f) {
                // a long wall: cut where it goes over the water, the rest kept
                boolean alongX = hx >= hz;
                float len = alongX ? hx : hz;
                int steps = Math.max(2, (int) Math.ceil(len * 2f / 0.5f));
                float step = len * 2f / steps, from = Float.NaN;
                for (int q = 0; q <= steps; q++) {
                    float t = -len + q * step;
                    float px = alongX ? cx + t : cx, pz = alongX ? cz : cz + t;
                    boolean keep = q < steps && !near(px, pz, COPING + 0.4f);
                    if (keep && Float.isNaN(from)) from = t;
                    if (!keep && !Float.isNaN(from)) {
                        int p = m++ * City.Cell.WALL;
                        if (p + City.Cell.WALL > out.length) break;
                        out[p] = alongX ? cx + from : x0;
                        out[p + 1] = alongX ? z0 : cz + from;
                        out[p + 2] = alongX ? cx + t : x1;
                        out[p + 3] = alongX ? z1 : cz + t;
                        out[p + 4] = w[o + 4];
                        out[p + 5] = w[o + 5];
                        from = Float.NaN;
                    }
                }
                continue;
            }
            if (near(cx, cz, CUT_SMALL - HALF)) continue;
            System.arraycopy(w, o, out, m++ * City.Cell.WALL, City.Cell.WALL);
        }
        c.walls = out;
        c.wallN = m;
    }

    // ---- the canal itself ----
    static final float PIECE = 8f;
    static final int STONE = 0xB9AC94, CAP = 0xDCD2BE, BEDC = 0x4F5A48, IRON = 0x2C2B2E;

    /**
     * What of the canal stands in cell (i, j): each stretch of it whose middle is in the cell —
     * its bed, its water, its walls and their coping, the railing along them (and the walls kept
     * for it); the bridge for cars, the barrier where a street stops at it; the road out's bridge.
     */
    static void build(City city, City.Cell c, int i, int j) {
        DrawList d = c.boxes, small = c.detail;
        float cx0 = i * City.CELL, cz0 = j * City.CELL, cx1 = cx0 + City.CELL, cz1 = cz0 + City.CELL;
        for (int k = 0; k + 1 < N; k++) {
            float ax = PX[k], az = PZ[k], dx = PX[k + 1] - ax, dz = PZ[k + 1] - az;
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            float fx = dx / len, fz = dz / len, sx = fz, sz = -fx;
            float[] t = along(dx, 0f, dz);
            int pieces = Math.max(1, Math.round(len / PIECE));
            float pl = len / pieces;
            // past the ends of a bend the pieces run on a little, so that the bend has no gap
            float overLo = k > 0 ? joint(k) : 0f, overHi = k + 2 < N ? joint(k + 1) : 0f;
            // at a bend the outer side runs on past the joint and the inner side stops short of it
            int outLo = 0, outHi = 0;
            if (k > 0) {
                float px0 = PX[k] - PX[k - 1], pz0 = PZ[k] - PZ[k - 1], pl0 = (float) Math.sqrt(px0 * px0 + pz0 * pz0);
                outLo = (fx * pz0 / pl0 - fz * px0 / pl0) > 0f ? -1 : 1;
            }
            if (k + 2 < N) {
                float nx = PX[k + 2] - PX[k + 1], nz = PZ[k + 2] - PZ[k + 1], nl = (float) Math.sqrt(nx * nx + nz * nz);
                outHi = (nx / nl * sx + nz / nl * sz) > 0f ? -1 : 1;
            }
            for (int p = 0; p < pieces; p++) {
                float m = (p + 0.5f) * pl, mx = ax + fx * m, mz = az + fz * m;
                if (mx < cx0 || mx >= cx1 || mz < cz0 || mz >= cz1) continue;
                boolean first = p == 0 && k > 0, last = p == pieces - 1 && k + 2 < N;
                // the water and its bed run on past a joint on both sides (the ground hides them)
                float lo = first ? overLo : 0f, hi = last ? overHi : 0f;
                float hl = pl * 0.5f + (lo + hi) * 0.5f, sh = (hi - lo) * 0.5f;
                float px = mx + fx * sh, pz = mz + fz * sh;
                d.box(px, BED - 0.3f, pz, HALF + WALL, 0.3f, hl, t, BEDC, DrawList.STONE);
                d.box(px, City.WATER - 0.25f, pz, HALF, 0.25f, hl, t, 0x2D6990, DrawList.SEA);
                for (int e = -1; e <= 1; e += 2) {
                    float elo = first ? (e == outLo ? overLo : -overLo) : 0f, ehi = last ? (e == outHi ? overHi : -overHi) : 0f;
                    float wl = pl * 0.5f + (elo + ehi) * 0.5f, ws = (ehi - elo) * 0.5f;
                    float qx0 = mx + fx * ws, qz0 = mz + fz * ws;
                    float wx = qx0 + sx * e * (HALF + WALL * 0.5f), wz = qz0 + sz * e * (HALF + WALL * 0.5f);
                    d.box(wx, (BED + TOP) * 0.5f, wz, WALL * 0.5f, (TOP - BED) * 0.5f, wl, t, STONE, DrawList.STONE);
                    float qx = qx0 + sx * e * (HALF + COPING * 0.5f), qz = qz0 + sz * e * (HALF + COPING * 0.5f);
                    d.box(qx, TOP - 0.02f, qz, COPING * 0.5f, 0.06f, wl, t, CAP, DrawList.PLAIN);
                    rail(city, c, small, qx0, qz0, fx, fz, sx * e, sz * e, wl);
                }
            }
        }
        // where the town's streets come to it
        if (i == COLUMN && j >= City.SHORE && j <= City.EDGE_J) street(c, d, small, j);
        // the road out's bridge
        float rx = City.routeX(ROAD_Z);
        if (rx >= cx0 && rx < cx1 && ROAD_Z >= cz0 && ROAD_Z < cz1) highBridge(c, d, small);
    }

    /** How far past joint k (between pieces k-1 and k of the line) a side runs on, or stops short: half the width times the tangent of half the turn. */
    private static float joint(int k) {
        float ix = PX[k] - PX[k - 1], iz = PZ[k] - PZ[k - 1], ox = PX[k + 1] - PX[k], oz = PZ[k + 1] - PZ[k];
        float c = (ix * ox + iz * oz) / (float) Math.sqrt((ix * ix + iz * iz) * (ox * ox + oz * oz));
        float turn = (float) Math.acos(Math.max(-1f, Math.min(1f, c)));
        return (HALF + COPING) * (float) Math.tan(turn * 0.5f);
    }

    /** The railing along one side of a stretch, its posts and rail; kept as walls in short lengths. */
    private static void rail(City city, City.Cell c, DrawList small, float px, float pz, float fx, float fz, float ox, float oz, float hl) {
        float off = HALF + COPING - RAIL_IN;
        float[] t = along(fx, 0f, fz);
        for (float s = -hl; s < hl - 0.01f; s += 1f) {
            float a = s, b = Math.min(hl, s + 1f), m = (a + b) * 0.5f;
            float x = px + fx * m + ox * off, z = pz + fz * m + oz * off;
            // none on a bridge for cars, nor where the beach is lower than the quay (the mouths' piers keep theirs)
            if (deck(x, z)) continue;
            float hb = (b - a) * 0.5f;
            small.box(x, TOP + RAIL_HIGH - 0.04f, z, 0.04f, 0.04f, hb, t, IRON, DrawList.PAINT);
            small.box(x, TOP + RAIL_HIGH * 0.5f, z, 0.025f, 0.025f, hb, t, IRON, DrawList.PAINT);
            if (((int) Math.floor(s + 1000f)) % 2 == 0)
                small.box(x - fx * hb, TOP + RAIL_HIGH * 0.5f, z - fz * hb, 0.04f, RAIL_HIGH * 0.5f, 0.04f, null, IRON, DrawList.PAINT);
            float ex = Math.abs(fx) * hb + 0.06f, ez = Math.abs(fz) * hb + 0.06f;
            // from its rail down to half way: the quay's walk is fenced, the water under it is not
            c.wall(x - ex, z - ez, x + ex, z + ez, TOP + RAIL_HIGH, TOP + RAIL_HIGH * 0.45f);
        }
    }

    /**
     * Where the street of row j meets the canal in the town: on the two rows that cross, the
     * bridge for cars (its deck, its stone sides, its lamps); on the others, the street stopping
     * at the quay on either side, a red and white barrier across it.
     */
    private static void street(City.Cell c, DrawList d, DrawList small, int j) {
        float z = j * City.CELL;
        if (drawRow(j)) {
            float hx = HALF + COPING;
            d.box(X_W, -0.25f, z, hx, 0.25f, City.ROAD, null, 0x5E4844, DrawList.ROAD);
            d.box(X_W, -0.7f, z, hx, 0.2f, City.ROAD + 0.4f, null, 0x7A6E62, DrawList.STONE);
            c.wall(X_W - HALF, z - City.ROAD, X_W + HALF, z + City.ROAD, 0.02f, -0.9f);
            for (int e = -1; e <= 1; e += 2) {
                float sz = z + e * (City.ROAD + 0.25f);
                d.box(X_W, 0.45f, sz, hx, 0.45f, 0.25f, null, STONE, DrawList.STONE);
                d.box(X_W, 0.94f, sz, hx + 0.05f, 0.05f, 0.32f, null, CAP, DrawList.PLAIN);
                c.wall(X_W - hx, sz - 0.25f, X_W + hx, sz + 0.25f, 0.99f, -0.9f);
            }
            return;
        }
        for (int e = -1; e <= 1; e += 2) {
            float bx = X_W + e * (HALF + COPING + 0.6f);
            // two posts, the boom between them in red and white
            for (int q = -1; q <= 1; q += 2)
                small.box(bx, 0.55f, z + q * (City.ROAD - 0.3f), 0.08f, 0.55f, 0.08f, null, 0xEDE2CC, DrawList.PAINT);
            for (int q = 0; q < 6; q++) {
                float zz = z - City.ROAD + 0.3f + (q + 0.5f) * (2f * City.ROAD - 0.6f) / 6f;
                small.box(bx, 0.9f, zz, 0.06f, 0.09f, (City.ROAD - 0.3f) / 6f, null, q % 2 == 0 ? 0xD8402E : 0xF2EEDF, DrawList.PAINT);
            }
            c.wall(bx - 0.15f, z - City.ROAD, bx + 0.15f, z + City.ROAD, 1f);
        }
    }

    /** The road out's bridge over the canal: its deck, the girders under it, railings, its abutments on the banks. */
    private static void highBridge(City.Cell c, DrawList d, DrawList small) {
        float z0 = ROAD_Z - HALF - COPING - FLAT, z1 = ROAD_Z + HALF + COPING + FLAT;
        float x0 = City.routeX(z0), x1 = City.routeX(z1);
        float mx = (x0 + x1) * 0.5f, mz = (z0 + z1) * 0.5f, hl = (z1 - z0) * 0.5f + 0.2f;
        float[] t = along(x1 - x0, 0f, z1 - z0);
        float w = City.ROUTE_HALF + 0.6f;
        d.box(mx, DECK_TOP - 0.12f, mz, City.ROUTE_HALF, 0.12f, hl, t, 0x5E4844, DrawList.ASPHALT);
        d.box(mx, DECK_TOP - 0.6f, mz, w, 0.36f, hl, t, 0x8A8378, DrawList.STONE);
        // two girders under it, steel, deep in the middle
        float ax = (float) Math.cos(Math.atan2(x1 - x0, z1 - z0)), az = -(float) Math.sin(Math.atan2(x1 - x0, z1 - z0));
        for (int e = -1; e <= 1; e += 2)
            d.box(mx + ax * e * (w - 1f), DECK_TOP - 1.5f, mz + az * e * (w - 1f), 0.25f, 0.6f, hl, t, 0x4A5A60, DrawList.PAINT);
        // its railings
        for (int e = -1; e <= 1; e += 2) {
            float rx = mx + ax * e * (w - 0.1f), rz = mz + az * e * (w - 0.1f);
            small.box(rx, DECK_TOP + 0.95f, rz, 0.05f, 0.05f, hl, t, IRON, DrawList.PAINT);
            small.box(rx, DECK_TOP + 0.45f, rz, 0.1f, 0.45f, hl, t, 0x9A9286, DrawList.STONE);
            float ex0 = Math.min(x0, x1) + ax * e * (w - 0.1f), ex1 = Math.max(x0, x1) + ax * e * (w - 0.1f);
            c.wall(ex0 - 0.3f, z0 - 0.2f, ex1 + 0.3f, z1 + 0.2f, DECK_TOP + 0.95f, DECK_TOP - 0.6f);
        }
        // the abutments: stone, from the bank up under the deck's ends
        for (int e = -1; e <= 1; e += 2) {
            float bz = ROAD_Z + e * (HALF + COPING + FLAT * 0.5f), bx = City.routeX(bz);
            d.box(bx, (TOP + DECK_TOP - 0.9f) * 0.5f, bz, w + 0.4f, (DECK_TOP - 0.9f - TOP) * 0.5f, FLAT * 0.5f + 0.4f, t, STONE, DrawList.STONE);
        }
    }
}
