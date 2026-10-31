package com.shumtugle.skydysh;

import java.util.HashMap;
import java.util.Random;

/**
 * The town, without end: a grid of streets, and between them blocks of old seaside houses, now
 * and then a square with palms. It is grown from a seed one cell at a time, only where the
 * runner is: a cell is built when it comes near and forgotten when it is left far behind, and
 * built again the same when it is come back to.
 *
 * A cell is CELL meters square. Its streets run along its west edge (x = i * CELL, north and
 * south) and its south edge (z = j * CELL, east and west), crossing at its south-west corner;
 * the rest of it is the block those streets go round. Space as in DrawList: x to the right,
 * y up, the road at y = 0.
 */
final class City implements Ground {
    static final float PARKED = 3.3f;            // a parked car's middle, from the road's middle
    static final float CELL = 64f;
    // across a street from its middle: the asphalt, the curb, the pavement up to the house fronts
    static final float ROAD = 4.05f, CURB = 4.3f, FRONT = 7f;
    static final float WALK_TOP = 0.14f, CURB_TOP = 0.15f;
    // how deep the houses are, from the front back into the block
    static final float DEPTH = 12f;
    // the cells drawn round the eye, each way
    static final int REACH = 3;

    private final long seed;
    private final HashMap<Long, Cell> cells = new HashMap<Long, Cell>();

    City(long seed) {
        this.seed = seed;
    }

    /** One cell, as built: its boxes, and what cannot be walked through, as boxes on the ground. */
    private static final class Cell {
        final DrawList boxes = new DrawList();
        // the small things (palms, lamp posts, chimneys): drawn only near
        final DrawList detail = new DrawList();
        // the parked cars: nearer still
        final DrawList parked = new DrawList();
        // x0, z0, x1, z1, and how high its top is, for each
        float[] walls = new float[80];
        int wallN;
        int used;
        // the houses: middle x, z, half sizes x, z, the top of the roof, the way the front faces
        // (x, z), and the way up it from the street: none (0), a lift (LIFT), a fire escape (ESCAPE)
        static final int HOUSE = 8;
        float[] houses = new float[HOUSE * 16];
        int houseN;
        // the ladders: where the ladder's foot is on its wall (x, z), the bottom and top of the
        // climb, the way the wall faces (x, z): seven floats each
        static final int LADDER = 6;
        float[] ladders = new float[LADDER * 8];
        int ladderN;

        void ladder(float x, float z, float bottom, float top, float nx, float nz) {
            if ((ladderN + 1) * LADDER > ladders.length) ladders = java.util.Arrays.copyOf(ladders, ladders.length * 2);
            float[] l = {x, z, bottom, top, nx, nz};
            System.arraycopy(l, 0, ladders, ladderN * LADDER, LADDER);
            ladderN++;
        }

        /** The house standing at (x, z), as its place in houses, or -1. */
        int houseAt(float x, float z) {
            for (int k = 0; k < houseN; k++) {
                int o = k * HOUSE;
                if (Math.abs(x - houses[o]) < houses[o + 2] && Math.abs(z - houses[o + 1]) < houses[o + 3]) return o;
            }
            return -1;
        }

        void house(float cx, float cz, float hx, float hz, float top, float nx, float nz, float up) {
            if ((houseN + 1) * HOUSE > houses.length) houses = java.util.Arrays.copyOf(houses, houses.length * 2);
            float[] h = {cx, cz, hx, hz, top, nx, nz, up};
            System.arraycopy(h, 0, houses, houseN * HOUSE, HOUSE);
            houseN++;
        }

        void wall(float x0, float z0, float x1, float z1) {
            wall(x0, z0, x1, z1, TALL);
        }

        void wall(float x0, float z0, float x1, float z1, float top) {
            if ((wallN + 1) * 5 > walls.length) walls = java.util.Arrays.copyOf(walls, walls.length * 2);
            walls[wallN * 5] = Math.min(x0, x1);
            walls[wallN * 5 + 1] = Math.min(z0, z1);
            walls[wallN * 5 + 2] = Math.max(x0, x1);
            walls[wallN * 5 + 3] = Math.max(z0, z1);
            walls[wallN * 5 + 4] = top;
            wallN++;
        }
    }

    private int frame;

    private static long key(int i, int j) {
        return ((long) i << 32) ^ (j & 0xFFFFFFFFL);
    }

    private Cell cell(int i, int j) {
        long k = key(i, j);
        Cell c = cells.get(k);
        if (c == null) {
            c = build(i, j);
            cells.put(k, c);
        }
        c.used = frame;
        return c;
    }

    static int index(float v) {
        return (int) Math.floor(v / CELL);
    }

    /**
     * The cells round (x, z) that the eye looking along (fx, fz) can see, into the list, their
     * small things only near; the ones long out of reach forgotten.
     */
    void draw(DrawList d, float x, float z, float fx, float fz) {
        frame++;
        int ci = index(x), cj = index(z);
        for (int j = cj - REACH; j <= cj + REACH; j++)
            for (int i = ci - REACH; i <= ci + REACH; i++) {
                // the cell's middle from the eye, and how far it reaches round it
                float dx = (i + 0.5f) * CELL - x, dz = (j + 0.5f) * CELL - z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz), reach = CELL * 0.75f;
                if (dist > reach && (dx * fx + dz * fz) < -reach) continue;
                if (dist > (REACH + 0.5f) * CELL) continue;
                Cell c = cell(i, j);
                d.append(c.boxes);
                if (dist < CELL * 1.6f) d.append(c.detail);
                if (dist < CELL * 1.2f) d.append(c.parked);
            }
        if (cells.size() > (2 * REACH + 3) * (2 * REACH + 3)) {
            java.util.Iterator<Cell> it = cells.values().iterator();
            while (it.hasNext()) if (frame - it.next().used > 120) it.remove();
        }
    }

    // what has no top to stand on (a palm); how high a step is taken in the stride, or pushed against
    static final float TALL = 1000f, STEP = 0.6f, LOW = 0.35f;

    /** How high the ground is here: the road, or the pavement a step up. */
    float ground(float x, float z) {
        float ox = x - Math.round(x / CELL) * CELL, oz = z - Math.round(z / CELL) * CELL;
        return Math.abs(ox) < CURB || Math.abs(oz) < CURB ? 0f : WALK_TOP;
    }

    /**
     * For the desktop stand and for missions: the roof of a house near (x, z), whose middle and
     * top go into out (x, top, z) and whose half sizes into half (x, z); false if none is near.
     */
    boolean roofNear(float x, float z, float[] out, float[] half) {
        float best = 1e9f;
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * 5;
                    float top = c.walls[o + 4];
                    if (top < 10f || top > 40f) continue;
                    float w = c.walls[o + 2] - c.walls[o], dpt = c.walls[o + 3] - c.walls[o + 1];
                    if (w < 5f || dpt < 5f) continue;
                    float mx = (c.walls[o] + c.walls[o + 2]) * 0.5f, mz = (c.walls[o + 1] + c.walls[o + 3]) * 0.5f;
                    float dd = (mx - x) * (mx - x) + (mz - z) * (mz - z);
                    if (dd < best) {
                        best = dd;
                        out[0] = mx;
                        out[1] = top;
                        out[2] = mz;
                        half[0] = w * 0.5f;
                        half[1] = dpt * 0.5f;
                    }
                }
            }
        return best < 1e9f;
    }

    /**
     * The highest thing under (x, z) that one whose feet are at feet can stand on: the street, or
     * the top of whatever is there no higher than reach above the feet (a roof, a car, a step).
     */
    public float ground(float x, float z, float feet, float reach) {
        float g = ground(x, z);
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * 5;
                    float top = c.walls[o + 4];
                    if (top <= g || top > feet + reach) continue;
                    if (x >= c.walls[o] && x <= c.walls[o + 2] && z >= c.walls[o + 1] && z <= c.walls[o + 3]) g = top;
                }
            }
        return g;
    }

    /** Out of doors the eye goes where it likes, pushed out of the houses (see push). */
    public void keepEye(float[] p) {
    }

    /** Out of doors the sky is the ceiling. */
    public float ceiling(float x, float z) {
        return TALL;
    }

    /**
     * Pushes a round body of radius r standing at (p[0], p[1]) out of whatever it went into;
     * true if it touched something.
     */
    boolean push(float[] p, float r) {
        return push(p, r, 0f);
    }

    /** The same, for one whose feet are at feet: what is lower than a step under them is not in the way. */
    public boolean push(float[] p, float r, float feet) {
        boolean hit = false;
        int ci = index(p[0]), cj = index(p[1]);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * 5;
                    if (c.walls[o + 4] <= feet + LOW) continue;
                    float x0 = c.walls[o], z0 = c.walls[o + 1], x1 = c.walls[o + 2], z1 = c.walls[o + 3];
                    float nx = Math.max(x0, Math.min(p[0], x1)), nz = Math.max(z0, Math.min(p[1], z1));
                    float dx = p[0] - nx, dz = p[1] - nz, dd = dx * dx + dz * dz;
                    if (dd >= r * r) continue;
                    hit = true;
                    if (dd > 1e-8f) {
                        float l = (float) Math.sqrt(dd);
                        p[0] = nx + dx / l * r;
                        p[1] = nz + dz / l * r;
                    } else {
                        // inside: out by the nearest side
                        float l = p[0] - x0, rr = x1 - p[0], b = p[1] - z0, t = z1 - p[1];
                        float m = Math.min(Math.min(l, rr), Math.min(b, t));
                        if (m == l) p[0] = x0 - r;
                        else if (m == rr) p[0] = x1 + r;
                        else if (m == b) p[1] = z0 - r;
                        else p[1] = z1 + r;
                    }
                }
            }
        return hit;
    }

    // ---- growing a cell ----

    private long hash(int i, int j, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL + j * 0x165667B19E3779F9L + salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h;
    }

    /** What a cell's block is: houses, mostly; now and then a square. The one the runner starts by is houses. */
    boolean square(int i, int j) {
        if (i == 0 && j == 0 || i == -1 && j == 0) return false;
        return (hash(i, j, 1) & 0xFF) < 30;
    }

    /** Whether the street along the cell's west edge has palms down its pavements. */
    private boolean palmy(int i, int j) {
        return (hash(i, j, 2) & 3) == 0;
    }

    /** Whether one of the cell's streets has trees down it (by its salt), one in two. */
    private boolean leafy(int i, int j, int salt) {
        return (hash(i, j, salt) & 3) < 2;
    }

    /** Whether a square is a garden within an iron fence rather than paving round a fountain. */
    boolean garden(int i, int j) {
        return (hash(i, j, 5) & 1) == 0;
    }

    /** A tree in the pavement at (x, z), in a grating of iron. */
    private static void street(Cell c, Random rnd, float x, float z) {
        c.detail.box(x, WALK_TOP + 0.005f, z, 0.6f, 0.01f, 0.6f, null, 0x3A3530, DrawList.RAIL);
        Garden.tree(c.detail, rnd, x, WALK_TOP, z, 7f + rnd.nextFloat() * 2.5f);
        c.wall(x - 0.2f, z - 0.2f, x + 0.2f, z + 0.2f);
    }

    /** Benches along the curbs of the block, between the lamp posts, looking out at the street. */
    private void benches(Cell c, float x0, float z0) {
        float in = CURB + 0.85f;
        for (float t = 23f; t < CELL - 8f; t += 18f) {
            Garden.bench(c.detail, x0 + in, WALK_TOP, z0 + t, (float) (-Math.PI / 2));
            c.wall(x0 + in - 0.3f, z0 + t - 0.9f, x0 + in + 0.3f, z0 + t + 0.9f, WALK_TOP + 0.48f);
            Garden.bench(c.detail, x0 + t, WALK_TOP, z0 + in, (float) Math.PI);
            c.wall(x0 + t - 0.9f, z0 + in - 0.3f, x0 + t + 0.9f, z0 + in + 0.3f, WALK_TOP + 0.48f);
        }
    }

    private Cell build(int i, int j) {
        Cell c = new Cell();
        DrawList d = c.boxes;
        Random rnd = new Random(hash(i, j, 0));
        float x0 = i * CELL, z0 = j * CELL, half = CELL * 0.5f;
        // the crossing, then the two streets out of it, north and east
        d.box(x0, -0.25f, z0, ROAD, 0.25f, ROAD, null, 0x5E4844, DrawList.ASPHALT);
        d.box(x0, -0.25f, z0 + half, ROAD, 0.25f, half - ROAD, null, 0x5E4844, DrawList.ROAD);
        d.box(x0 + half, -0.25f, z0, half - ROAD, 0.25f, ROAD, null, 0x5E4844, DrawList.ROAD);
        // the block: curbs round it, the pavement within
        float b0x = x0 + ROAD, b1x = x0 + CELL - ROAD, b0z = z0 + ROAD, b1z = z0 + CELL - ROAD;
        float cw = CURB - ROAD, cy = (CURB_TOP - 0.25f) * 0.5f, ch = (CURB_TOP + 0.25f) * 0.5f;
        int curb = 0xC9BFAE;
        d.box((b0x + b1x) * 0.5f, cy, b0z + cw * 0.5f, (b1x - b0x) * 0.5f, ch, cw * 0.5f, null, curb, DrawList.STONE);
        d.box((b0x + b1x) * 0.5f, cy, b1z - cw * 0.5f, (b1x - b0x) * 0.5f, ch, cw * 0.5f, null, curb, DrawList.STONE);
        d.box(b0x + cw * 0.5f, cy, (b0z + b1z) * 0.5f, cw * 0.5f, ch, (b1z - b0z) * 0.5f - cw, null, curb, DrawList.STONE);
        d.box(b1x - cw * 0.5f, cy, (b0z + b1z) * 0.5f, cw * 0.5f, ch, (b1z - b0z) * 0.5f - cw, null, curb, DrawList.STONE);
        boolean sq = square(i, j);
        float p0x = b0x + cw, p1x = b1x - cw, p0z = b0z + cw, p1z = b1z - cw;
        d.box((p0x + p1x) * 0.5f, (WALK_TOP - 0.25f) * 0.5f, (p0z + p1z) * 0.5f, (p1x - p0x) * 0.5f, (WALK_TOP + 0.25f) * 0.5f,
                (p1z - p0z) * 0.5f, null, sq ? 0xDCCDB2 : 0xCDBFA6, sq ? DrawList.PROMENADE : DrawList.PAVING);
        // what furnishes the streets and squares has its own seed, so the houses stay as they were
        Random deco = new Random(hash(i, j, 6));
        if (sq) plaza(c, rnd, deco, x0, z0, garden(i, j));
        else block(c, rnd, x0, z0, i, j);
        lamps(c, x0, z0);
        benches(c, x0, z0);
        parked(c, rnd, x0, z0);
        if (!palmy(i, j) && leafy(i, j, 3)) {
            // plane trees down the street along the west edge, on both its pavements
            for (float t = 20f; t < CELL - 10f; t += 26f)
                for (int s = -1; s <= 1; s += 2) street(c, deco, x0 + s * (CURB + 0.9f), z0 + t);
        }
        if (leafy(i, j, 4)) {
            // and down the one along the south edge
            for (float t = 20f; t < CELL - 10f; t += 26f)
                for (int s = -1; s <= 1; s += 2) street(c, deco, x0 + t, z0 + s * (CURB + 0.9f));
        }
        if (palmy(i, j)) {
            // down the street along the west edge, on both its pavements
            for (float z = z0 + 10f; z < z0 + CELL - 6f; z += 12f)
                for (int s = -1; s <= 1; s += 2) {
                    float px = x0 + s * (CURB + 1.1f);
                    Street.palm(c.detail, rnd, px, WALK_TOP, z, 6.5f + rnd.nextFloat() * 2f, (rnd.nextFloat() - 0.5f) * 0.3f, false);
                    c.wall(px - 0.25f, z - 0.25f, px + 0.25f, z + 0.25f);
                }
        }
        return c;
    }

    /** A block of houses round its four sides, their fronts on the streets, a yard hidden in the middle. */
    private void block(Cell c, Random rnd, float x0, float z0, int i, int j) {
        float a = FRONT, b = CELL - FRONT;
        // the south and north rows run the whole way; the west and east rows fit between them
        row(c, rnd, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH, true, -1);
        row(c, rnd, x0 + a, x0 + b, z0 + b - DEPTH, z0 + b, true, 1);
        row(c, rnd, z0 + a + DEPTH, z0 + b - DEPTH, x0 + a, x0 + a + DEPTH, false, -1);
        row(c, rnd, z0 + a + DEPTH, z0 + b - DEPTH, x0 + b - DEPTH, x0 + b, false, 1);
    }

    /**
     * A row of houses from u0 to u1 along x (alongX) or z, from v0 to v1 the other way; its
     * fronts face toward smaller v (side -1) or larger (side 1).
     */
    private void row(Cell c, Random rnd, float u0, float u1, float v0, float v1, boolean alongX, int side) {
        DrawList d = c.boxes;
        float u = u0;
        while (u < u1 - 0.5f) {
            int bays = 3 + rnd.nextInt(3);
            float len = bays * 2.9f;
            if (u1 - (u + len) < 6f) len = u1 - u;
            int storeys = 3 + rnd.nextInt(4);
            float height = 3.3f * (storeys + 1) + 0.5f;
            int tint = Street.WALLS[rnd.nextInt(Street.WALLS.length)];
            int seed = rnd.nextInt(1 << 16);
            float cu = u + len * 0.5f, cv = (v0 + v1) * 0.5f, hu = len * 0.5f, hv = (v1 - v0) * 0.5f;
            float cx = alongX ? cu : cv, cz = alongX ? cv : cu, hx = alongX ? hu : hv, hz = alongX ? hv : hu;
            d.box(cx, height * 0.5f, cz, hx, height * 0.5f, hz, null, tint, DrawList.material(DrawList.FACADE, seed));
            // the cornice along the front
            float fv = side < 0 ? v0 : v1;
            float kx = alongX ? cu : fv + side * 0.15f, kz = alongX ? fv + side * 0.15f : cu;
            d.box(kx, height - 0.2f, kz, alongX ? hu : 0.25f, 0.2f, alongX ? 0.25f : hu, null, Street.scale(tint, 1.05f), DrawList.PLAIN);
            // the taller houses, one in two, have a lift: a door in the front, a hut on the roof
            // (and no chimneys in its way)
            boolean lift = storeys >= 4 && ((seed >>> 3) & 1) == 0;
            // of the rest, one in three has a fire escape up its front
            boolean escape = !lift && ((seed >>> 5) % 3) == 0;
            roof(c, d, lift ? null : c.detail, cx, height, cz, hx, hz, seed);
            // the house, and its roof to stand on: the tiles, a step up in the middle
            c.wall(cx - hx, cz - hz, cx + hx, cz + hz, height + 0.5f);
            c.wall(cx - hx + 1.3f, cz - Math.max(0.4f, hz - 1.3f), cx + hx - 1.3f, cz + Math.max(0.4f, hz - 1.3f), height + 0.75f);
            float nx = alongX ? 0f : side, nz = alongX ? side : 0f;
            if (lift) lift(c, d, cu, fv, alongX, side, height, tint);
            if (escape) escape(c, u + 1.1f, fv, alongX, side, height + 0.5f);
            // ladders up from a lower roof next door, or down onto it from this one: on the
            // taller house's wall, at the middle of the row, from the one roof to the other
            for (int end = 0; end < 2; end++) {
                float bu = end == 0 ? u : u + len, look = end == 0 ? -0.4f : 0.4f;
                int o = c.houseAt(alongX ? bu + look : cv, alongX ? cv : bu + look);
                if (o < 0) continue;
                float other = c.houses[o + 4], mine = height + 0.5f;
                if (Math.abs(other - mine) < 1f) continue;
                // facing from the taller toward the lower
                float dir = (mine > other ? 1f : -1f) * (end == 0 ? -1f : 1f);
                float lx = alongX ? bu : cv, lz = alongX ? cv : bu;
                ladder(c, lx, lz, Math.min(mine, other), Math.max(mine, other), alongX ? dir : 0f, alongX ? 0f : dir, false);
            }
            // two pots of flowers either side of the middle of the front, now and then; ivy up its far end
            Random deco = new Random(seed * 0x5DEECE66DL + 11L);
            if (((seed >>> 11) & 1) == 0)
                for (int e = -1; e <= 1; e += 2) {
                    float pu = cu + e * 1.55f, pv = fv + side * 0.4f;
                    float px = alongX ? pu : pv, pz = alongX ? pv : pu;
                    Garden.pot(c.detail, deco, px, WALK_TOP, pz, 0.27f);
                    c.wall(px - 0.24f, pz - 0.24f, px + 0.24f, pz + 0.24f, WALK_TOP + 0.6f);
                }
            if (((seed >>> 12) & 3) == 0) {
                float iu = u + len - 1.8f, top = WALK_TOP + 3.3f * (1 + ((seed >>> 14) & 1)) + 0.8f;
                Garden.ivy(c.detail, deco, alongX ? iu : fv, alongX ? fv : iu, nx, nz, 1.4f, WALK_TOP, Math.min(top, height));
            }
            c.house(cx, cz, hx, hz, height + 0.5f, nx, nz, lift ? LIFT : escape ? ESCAPE : 0f);
            u += len;
        }
    }

    /** What a house has to go up it by: a lift, or a fire escape. */
    static final float LIFT = 1f, ESCAPE = 2f;
    // a ladder: half its width; how far apart its rungs; its iron
    static final float LADDER_HALF = 0.32f, RUNG = 0.36f;
    static final int IRON = 0x2C2B2E;

    /**
     * A ladder on a wall at (x, z) facing (nx, nz), from bottom to top: iron rails and rungs, a hoop
     * over its top, and its place kept for climbing (see ladderNear); a fire escape has landings
     * with railings every two storeys.
     */
    private static void ladder(Cell c, float x, float z, float bottom, float top, float nx, float nz, boolean escape) {
        DrawList d = c.detail;
        float h = top - bottom, my = (top + bottom) * 0.5f;
        float ax = -nz, az = nx;                          // across the ladder
        float ox = x + nx * 0.14f, oz = z + nz * 0.14f;   // off the wall
        for (int e = -1; e <= 1; e += 2)
            d.box(ox + ax * e * LADDER_HALF, my + 0.5f, oz + az * e * LADDER_HALF, Math.abs(nx) * 0.03f + Math.abs(nz) * 0.025f,
                    h * 0.5f + 0.5f, Math.abs(nz) * 0.03f + Math.abs(nx) * 0.025f, null, IRON, DrawList.PLAIN);
        for (float y = bottom + 0.3f; y < top + 0.9f; y += RUNG)
            d.box(ox, y, oz, Math.abs(nz) * LADDER_HALF + 0.02f, 0.018f, Math.abs(nx) * LADDER_HALF + 0.02f, null, IRON, DrawList.PLAIN);
        // the brackets holding it to the wall, now and then
        for (float y = bottom + 1.5f; y < top; y += 3.3f)
            for (int e = -1; e <= 1; e += 2)
                d.box(x + nx * 0.07f + ax * e * LADDER_HALF, y, z + nz * 0.07f + az * e * LADDER_HALF, Math.abs(nx) * 0.07f + 0.02f, 0.02f,
                        Math.abs(nz) * 0.07f + 0.02f, null, IRON, DrawList.PLAIN);
        if (escape) {
            // landings every two storeys, a railing round each
            for (float y = bottom + 3.3f * 2f - 0.1f; y < top - 2f; y += 3.3f * 2f) {
                float px = x + nx * 0.55f, pz = z + nz * 0.55f;
                d.box(px, y, pz, Math.abs(nz) * 0.9f + Math.abs(nx) * 0.55f, 0.03f, Math.abs(nx) * 0.9f + Math.abs(nz) * 0.55f, null, IRON, DrawList.PLAIN);
                d.box(x + nx * 1.08f, y + 0.5f, z + nz * 1.08f, Math.abs(nz) * 0.9f + 0.02f, 0.5f, Math.abs(nx) * 0.9f + 0.02f, null, IRON, DrawList.RAIL);
                for (int e = -1; e <= 1; e += 2)
                    d.box(px + ax * e * 0.9f, y + 0.5f, pz + az * e * 0.9f, Math.abs(nx) * 0.55f + 0.02f, 0.5f, Math.abs(nz) * 0.55f + 0.02f, null, IRON, DrawList.RAIL);
            }
        }
        c.ladder(x, z, bottom, top, nx, nz);
    }

    /** A fire escape up the front at u along the row, the front at fv facing side, from the pavement to the roof's edge. */
    private static void escape(Cell c, float u, float fv, boolean alongX, int side, float top) {
        float x = alongX ? u : fv, z = alongX ? fv : u;
        ladder(c, x, z, WALK_TOP, top, alongX ? 0f : side, alongX ? side : 0f, true);
    }

    /**
     * The ladder whose foot is within reach of one at (x, z) with feet at feet (near its bottom):
     * into out where to stand to climb it (x, z), its bottom and top, the way its wall faces (x,
     * z); false if none is.
     */
    boolean ladderNear(float x, float z, float feet, float reach, float[] out) {
        float best = reach * reach;
        boolean found = false;
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.ladderN; k++) {
                    int o = k * Cell.LADDER;
                    float bottom = c.ladders[o + 2];
                    if (feet < bottom - 0.6f || feet > bottom + 1.2f) continue;
                    float fx = c.ladders[o] + c.ladders[o + 4] * 0.55f, fz = c.ladders[o + 1] + c.ladders[o + 5] * 0.55f;
                    float dd = (fx - x) * (fx - x) + (fz - z) * (fz - z);
                    if (dd < best) {
                        best = dd;
                        out[0] = fx;
                        out[1] = fz;
                        out[2] = bottom;
                        out[3] = c.ladders[o + 3];
                        out[4] = c.ladders[o + 4];
                        out[5] = c.ladders[o + 5];
                        found = true;
                    }
                }
            }
        return found;
    }

    // a lift's door: half its width, its height; its hut on the roof: in from the front, half sizes, height
    static final float DOOR_HALF = 0.85f, DOOR_HIGH = 2.6f, HUT_IN = 2.6f, HUT_HALF = 1.3f, HUT_DEEP = 1.1f, HUT_HIGH = 2.2f;
    static final int LIFT_LIGHT = 0x8FF0C8;

    /**
     * A lift: in the front at u (along the row), the front at v facing side, a door of dark glass
     * in a stone frame, a green lamp over it, and the same at the back, on the yard; on the roof
     * the hut it comes up into, its door facing in, away from the street.
     */
    private static void lift(Cell c, DrawList d, float u, float fv, boolean alongX, int side, float height, int tint) {
        liftDoor(d, u, fv, alongX, side);
        liftDoor(d, u, fv - side * DEPTH, alongX, -side);
        // the hut on the roof, and its door on the side away from the street
        float top = height + 0.5f, hv = fv - side * (HUT_IN + HUT_DEEP);
        boxUV(d, u, top + HUT_HIGH * 0.5f, hv, HUT_HALF, HUT_HIGH * 0.5f, HUT_DEEP, alongX, Street.scale(tint, 0.95f), DrawList.PLAIN);
        boxUV(d, u, top + HUT_HIGH + 0.08f, hv, HUT_HALF + 0.12f, 0.08f, HUT_DEEP + 0.12f, alongX, 0xA8553A, DrawList.GRAIN);
        float back = hv - side * (HUT_DEEP + 0.03f);
        boxUV(d, u, top + 1.05f, back, 0.55f, 1.05f, 0.04f, alongX, 0x1D2730, DrawList.PLAIN);
        boxUV(d, u, top + 1.95f, back - side * 0.02f, 0.18f, 0.1f, 0.04f, alongX, LIFT_LIGHT, DrawList.GLOW);
        float x0 = alongX ? u - HUT_HALF : hv - HUT_DEEP, z0 = alongX ? hv - HUT_DEEP : u - HUT_HALF;
        c.wall(x0, z0, x0 + 2f * (alongX ? HUT_HALF : HUT_DEEP), z0 + 2f * (alongX ? HUT_DEEP : HUT_HALF), top + HUT_HIGH + 0.16f);
    }

    /** A lift's door in a wall at v facing side: dark glass in a stone frame, a green lamp over it. */
    private static void liftDoor(DrawList d, float u, float fv, boolean alongX, int side) {
        float out = fv + side * 0.06f;
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out, DOOR_HALF + 0.22f, DOOR_HIGH * 0.5f + 0.15f, 0.07f, alongX, 0xD8CDB8, DrawList.STONE);
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out + side * 0.03f, DOOR_HALF, DOOR_HIGH * 0.5f, 0.06f, alongX, 0x1D2730, DrawList.PLAIN);
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out + side * 0.05f, 0.03f, DOOR_HIGH * 0.5f, 0.05f, alongX, 0x9AA3A8, DrawList.PLAIN);
        // the lamp: a pale green pane with an arrow up in it
        float ly = WALK_TOP + DOOR_HIGH + 0.75f;
        boxUV(d, u, ly, out + side * 0.04f, 0.36f, 0.3f, 0.05f, alongX, LIFT_LIGHT, DrawList.GLOW);
        boxUV(d, u, ly - 0.08f, out + side * 0.09f, 0.04f, 0.13f, 0.01f, alongX, 0x1D3A32, DrawList.PLAIN);
        for (int k = 0; k < 3; k++)
            boxUV(d, u, ly + 0.17f - k * 0.06f, out + side * 0.09f, 0.04f + k * 0.055f, 0.03f, 0.01f, alongX, 0x1D3A32, DrawList.PLAIN);
    }

    /** A box placed by u along a row and v across it, so wide, high and deep. */
    private static void boxUV(DrawList d, float u, float y, float v, float hu, float hy, float hv, boolean alongX, int color, int material) {
        if (alongX) d.box(u, y, v, hu, hy, hv, null, color, material);
        else d.box(v, y, u, hv, hy, hu, null, color, material);
    }

    /**
     * A lift whose door is within reach of (x, z): into out the place before its door on the
     * pavement (x, z), the way its door faces (x, z), where she is when she has come out of its
     * hut on the roof, a few steps on (x, y, z), the way she faces there, as a heading, and the
     * place before the hut's door (x, z); false if none is so near.
     */
    boolean liftAt(float x, float z, float reach, float[] out) {
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN; k++) {
                    int o = k * Cell.HOUSE;
                    if (c.houses[o + 7] != LIFT) continue;
                    lift(c.houses, o, out);
                    float dx = out[0] - x, dz = out[1] - z;
                    if (dx * dx + dz * dz < reach * reach) return true;
                }
            }
        return false;
    }

    /**
     * The nearest lift to one at (x, z) with feet at feet: in the street, by its door in the
     * front or at the back; on a roof, by its hut's door; as liftAt() tells it (the door being the
     * one near), and out[10] 1 if it is the one at the back; its house into house as the cell
     * keeps it (see Cell.house); false if none is within reach.
     */
    boolean liftNear(float x, float z, float feet, float reach, float[] out, float[] house) {
        float best = reach * reach;
        boolean found = false;
        float[] l = new float[10];
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN; k++) {
                    int o = k * Cell.HOUSE;
                    if (c.houses[o + 7] != LIFT) continue;
                    lift(c.houses, o, l);
                    boolean up = Math.abs(feet - l[5]) < 1.5f;
                    if (!up && feet > 1f) continue;
                    // in the street, its door in the front or the one at the back, on the yard (if
                    // the back looks on the yard and not on the end of the next row)
                    int doors = up ? 1 : backClear(c.houses, o) ? 2 : 1;
                    for (int back = 0; back < doors; back++) {
                        if (back == 1) {
                            float half = Math.abs(l[2]) * c.houses[o + 2] + Math.abs(l[3]) * c.houses[o + 3];
                            l[0] -= l[2] * (2f * half + 1.4f);
                            l[1] -= l[3] * (2f * half + 1.4f);
                            l[2] = -l[2];
                            l[3] = -l[3];
                        }
                        float dx = (up ? l[8] : l[0]) - x, dz = (up ? l[9] : l[1]) - z, dd = dx * dx + dz * dz;
                        if (dd < best) {
                            best = dd;
                            System.arraycopy(l, 0, out, 0, 10);
                            out[10] = back;
                            System.arraycopy(c.houses, o, house, 0, Cell.HOUSE);
                            found = true;
                        }
                    }
                }
            }
        return found;
    }

    /**
     * Whether the back of the house at o looks on the yard: nothing of another house stands
     * just behind the middle of its back (a house at a corner of its block backs onto the end
     * of the row across, and has no door there).
     */
    boolean backClear(float[] h, int o) {
        float cx = h[o], cz = h[o + 1], nx = h[o + 5], nz = h[o + 6];
        float half = Math.abs(nx) * h[o + 2] + Math.abs(nz) * h[o + 3];
        float px = cx - nx * (half + 1.2f), pz = cz - nz * (half + 1.2f), r = 0.9f;
        int ci = index(px), cj = index(pz);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int w = k * 5;
                    if (c.walls[w + 4] < 1f) continue;
                    if (px + r > c.walls[w] && px - r < c.walls[w + 2] && pz + r > c.walls[w + 1] && pz - r < c.walls[w + 3]) return false;
                }
            }
        return true;
    }

    /** The lift of the house at o: as liftAt() tells it. */
    static void lift(float[] h, int o, float[] out) {
        float cx = h[o], cz = h[o + 1], hx = h[o + 2], hz = h[o + 3], top = h[o + 4], nx = h[o + 5], nz = h[o + 6];
        float half = Math.abs(nx) * hx + Math.abs(nz) * hz;
        // the front's middle, and a little before it
        out[0] = cx + nx * (half + 0.7f);
        out[1] = cz + nz * (half + 0.7f);
        out[2] = nx;
        out[3] = nz;
        // out of the hut's door and a few steps on into the roof, clear of the hut for the eye
        float door = half - HUT_IN - 2f * HUT_DEEP - 0.7f, in = door - 3.3f;
        out[4] = cx + nx * in;
        out[5] = top;
        out[6] = cz + nz * in;
        out[7] = (float) Math.atan2(-nx, -nz);
        out[8] = cx + nx * door;
        out[9] = cz + nz * door;
    }

    // the kinds of sign at the ways in and up (see marks)
    static final int MARK_STREET_LIFT = 0, MARK_HUT = 1, MARK_ESCAPE = 2, MARK_ROOF_LADDER = 3;

    /**
     * The places for signs at the ways in and up within reach of (x, z), at most max of them,
     * six floats each into out: where the sign hangs (x, y, z), the way the wall it is on faces
     * (x, z), and its kind: over a lift's front door, over the door of its hut on the roof, at
     * the foot of a fire escape, at the foot of a ladder from one roof up to the next. How many.
     */
    int marks(float x, float z, float reach, float[] out, int max) {
        int n = 0, ci = index(x), cj = index(z);
        float[] l = new float[10];
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN && n < max - 1; k++) {
                    int o = k * Cell.HOUSE;
                    if (c.houses[o + 7] != LIFT) continue;
                    float nx = c.houses[o + 5], nz = c.houses[o + 6], top = c.houses[o + 4];
                    float half = Math.abs(nx) * c.houses[o + 2] + Math.abs(nz) * c.houses[o + 3];
                    float fx = c.houses[o] + nx * (half + 0.25f), fz = c.houses[o + 1] + nz * (half + 0.25f);
                    if (near(fx, fz, x, z, reach)) n = mark(out, n, fx, WALK_TOP + 4.3f, fz, nx, nz, MARK_STREET_LIFT);
                    lift(c.houses, o, l);
                    float hx = l[8] + nx * 0.45f, hz = l[9] + nz * 0.45f;
                    if (near(hx, hz, x, z, reach)) n = mark(out, n, hx, top + HUT_HIGH + 0.8f, hz, -nx, -nz, MARK_HUT);
                }
                for (int k = 0; k < c.ladderN && n < max; k++) {
                    int o = k * Cell.LADDER;
                    float lx = c.ladders[o], lz = c.ladders[o + 1], bottom = c.ladders[o + 2], nx = c.ladders[o + 4], nz = c.ladders[o + 5];
                    float sx = lx + nx * 0.6f, sz = lz + nz * 0.6f;
                    if (!near(sx, sz, x, z, reach)) continue;
                    boolean street = bottom < 1f;
                    n = mark(out, n, sx, bottom + (street ? 2.6f : 1.9f), sz, nx, nz, street ? MARK_ESCAPE : MARK_ROOF_LADDER);
                }
            }
        return n;
    }

    private static boolean near(float ax, float az, float x, float z, float reach) {
        return (ax - x) * (ax - x) + (az - z) * (az - z) < reach * reach;
    }

    private static int mark(float[] out, int n, float x, float y, float z, float nx, float nz, int kind) {
        int o = n * 6;
        out[o] = x;
        out[o + 1] = y;
        out[o + 2] = z;
        out[o + 3] = nx;
        out[o + 4] = nz;
        out[o + 5] = kind;
        return n + 1;
    }

    /** How many houses cell (i, j) has (none, if it is a square). */
    int houses(int i, int j) {
        return square(i, j) ? 0 : cell(i, j).houseN;
    }

    /** House k of cell (i, j), into out as the cell keeps it (see Cell.house). */
    void house(int i, int j, int k, float[] out) {
        System.arraycopy(cell(i, j).houses, k * Cell.HOUSE, out, 0, Cell.HOUSE);
    }

    /** A roof of red tiles in two steps; its chimneys among the small things (none, if small is null). */
    private static void roof(Cell cell, DrawList d, DrawList small, float x, float top, float z, float hx, float hz, int seed) {
        d.box(x, top + 0.25f, z, hx - 0.3f, 0.25f, hz - 0.3f, null, 0xC4673E, DrawList.GRAIN);
        d.box(x, top + 0.6f, z, hx - 1.3f, 0.15f, Math.max(0.4f, hz - 1.3f), null, 0xA8553A, DrawList.GRAIN);
        if (small == null) return;
        int h = seed * 0x2545F491;
        for (int c = 0; c <= ((h >>> 7) & 1); c++) {
            float cx = x + ((((h >>> (9 + c * 5)) & 31) / 31f) - 0.5f) * hx;
            float cz = z + ((((h >>> (19 + c * 5)) & 31) / 31f) - 0.5f) * hz;
            small.box(cx, top + 1.1f, cz, 0.3f, 0.6f, 0.3f, null, 0xBE9C84, DrawList.STONE);
            cell.wall(cx - 0.3f, cz - 0.3f, cx + 0.3f, cz + 0.3f, top + 1.7f);
        }
    }

    /**
     * A square: big pale paving, palms round it; within them either a fountain amid benches,
     * flower beds, pots and lamps, or a garden in a low iron fence: lawns, gravel paths across it,
     * hedges along them, trees and bushes, benches, beds of flowers round a small fountain.
     */
    private void plaza(Cell c, Random rnd, Random deco, float x0, float z0, boolean garden) {
        DrawList d = c.detail;
        float mx = x0 + CELL * 0.5f, mz = z0 + CELL * 0.5f, y = WALK_TOP;
        for (int a = -2; a <= 2; a++)
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(a) < 2 && Math.abs(b) < 2) continue;
                float px = mx + a * 10f, pz = mz + b * 10f;
                Street.palm(c.detail, rnd, px, WALK_TOP, pz, 6f + rnd.nextFloat() * 2.5f, (rnd.nextFloat() - 0.5f) * 0.35f, false);
                c.wall(px - 0.25f, pz - 0.25f, px + 0.25f, pz + 0.25f);
            }
        if (garden) {
            park(c, deco, mx, mz);
            return;
        }
        // the fountain, and round it benches looking at it, pots between them
        float rim = Garden.fountain(c.boxes, mx, y, mz, 4.2f);
        c.wall(mx - 3.9f, mz - 3.9f, mx + 3.9f, mz + 3.9f, rim);
        for (int k = 0; k < 8; k++) {
            float a = k * (float) (Math.PI / 4) + (float) (Math.PI / 8), ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float bx = mx + ca * 8f, bz = mz + sa * 8f;
            Garden.bench(d, bx, y, bz, (float) Math.atan2(-ca, -sa));
            c.wall(bx - 0.75f, bz - 0.75f, bx + 0.75f, bz + 0.75f, y + 0.48f);
            float pa = a + (float) (Math.PI / 8), px = mx + (float) Math.cos(pa) * 8.4f, pz = mz + (float) Math.sin(pa) * 8.4f;
            Garden.pot(d, deco, px, y, pz, 0.32f);
            c.wall(px - 0.28f, pz - 0.28f, px + 0.28f, pz + 0.28f, y + 0.7f);
        }
        // lamps on the diagonals, flower beds on the four ways out
        for (int k = 0; k < 4; k++) {
            float a = k * (float) (Math.PI / 2) + (float) (Math.PI / 4);
            float lx = mx + (float) Math.cos(a) * 12f, lz = mz + (float) Math.sin(a) * 12f;
            Garden.lamp(d, lx, y, lz, a);
            c.wall(lx - 0.2f, lz - 0.2f, lx + 0.2f, lz + 0.2f);
            float bx = k % 2 == 0 ? (k == 0 ? 14.5f : -14.5f) : 0f, bz = k % 2 == 1 ? (k == 1 ? 14.5f : -14.5f) : 0f;
            float hx = bx != 0f ? 0.8f : 2.6f, hz = bx != 0f ? 2.6f : 0.8f;
            for (int e = -1; e <= 1; e += 2) {
                float cx = mx + bx + (bx == 0f ? e * 4f : 0f), cz = mz + bz + (bz == 0f ? e * 4f : 0f);
                Garden.bed(d, deco, cx - hx, cz - hz, cx + hx, cz + hz, y);
                c.wall(cx - hx, cz - hz, cx + hx, cz + hz, y + 0.55f);
            }
        }
    }

    /** The garden within a square's palms: its fence, lawns, paths, hedges, trees, benches and middle. */
    private void park(Cell c, Random rnd, float mx, float mz) {
        DrawList d = c.detail;
        float y = WALK_TOP, edge = 17f, path = 1.5f;
        // the gravel paths across, and the lawns between them
        c.boxes.box(mx, y + 0.008f, mz, edge, 0.008f, path, null, 0xD9C7A2, DrawList.GRAVEL);
        c.boxes.box(mx, y + 0.008f, mz, path, 0.008f, edge, null, 0xD9C7A2, DrawList.GRAVEL);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                float lx = mx + sx * (edge + path) * 0.5f, lz = mz + sz * (edge + path) * 0.5f, lh = (edge - path) * 0.5f;
                c.boxes.box(lx, y + 0.02f, lz, lh, 0.02f, lh, null, 0x6E9A44, DrawList.GRASS);
                // low hedges along the paths, a gap half way for the lawn
                float a0 = path + 0.6f, a1 = edge - 0.4f, gap = (a0 + a1) * 0.5f;
                for (int h = 0; h < 2; h++) {
                    float f0 = h == 0 ? a0 : gap + 1f, f1 = h == 0 ? gap - 1f : a1;
                    float hx0 = mx + sx * f0, hx1 = mx + sx * f1, hz = mz + sz * (path + 0.35f);
                    Garden.hedge(d, Math.min(hx0, hx1), hz - 0.3f, Math.max(hx0, hx1), hz + 0.3f, y, 0.75f);
                    c.wall(Math.min(hx0, hx1), hz - 0.3f, Math.max(hx0, hx1), hz + 0.3f, y + 0.75f);
                    float vz0 = mz + sz * f0, vz1 = mz + sz * f1, vx = mx + sx * (path + 0.35f);
                    Garden.hedge(d, vx - 0.3f, Math.min(vz0, vz1), vx + 0.3f, Math.max(vz0, vz1), y, 0.75f);
                    c.wall(vx - 0.3f, Math.min(vz0, vz1), vx + 0.3f, Math.max(vz0, vz1), y + 0.75f);
                }
                // trees and bushes on the lawn
                int trees = 2 + rnd.nextInt(2);
                for (int k = 0; k < trees; k++) {
                    float tx = mx + sx * (5f + rnd.nextFloat() * 9f), tz = mz + sz * (5f + rnd.nextFloat() * 9f);
                    Garden.tree(d, rnd, tx, y, tz, 7.5f + rnd.nextFloat() * 3f);
                    c.wall(tx - 0.2f, tz - 0.2f, tx + 0.2f, tz + 0.2f);
                }
                for (int k = 0; k < 3; k++) {
                    float bx = mx + sx * (3.5f + rnd.nextFloat() * 12f), bz = mz + sz * (3.5f + rnd.nextFloat() * 12f);
                    float r = 0.6f + rnd.nextFloat() * 0.5f;
                    Garden.bush(d, rnd, bx, y, bz, r);
                    c.wall(bx - r * 0.7f, bz - r * 0.7f, bx + r * 0.7f, bz + r * 0.7f, y + r * 1.2f);
                }
                // benches on the paths, their backs to the hedges
                float bt = 9.5f;
                Garden.bench(d, mx + sx * bt, y, mz + sz * 1.15f, sz > 0 ? (float) Math.PI : 0f);
                c.wall(mx + sx * bt - 0.9f, mz + sz * 1.15f - 0.3f, mx + sx * bt + 0.9f, mz + sz * 1.15f + 0.3f, y + 0.48f);
            }
        // the fence round it, open where the paths go out
        for (int e = -1; e <= 1; e += 2) {
            for (int h = -1; h <= 1; h += 2) {
                float f0 = h * (path + 0.4f), f1 = h * edge;
                Garden.fence(d, mx + Math.min(f0, f1), mz + e * edge, mx + Math.max(f0, f1), mz + e * edge, y);
                c.wall(mx + Math.min(f0, f1), mz + e * edge - 0.05f, mx + Math.max(f0, f1), mz + e * edge + 0.05f, y + 0.95f);
                Garden.fence(d, mx + e * edge, mz + Math.min(f0, f1), mx + e * edge, mz + Math.max(f0, f1), y);
                c.wall(mx + e * edge - 0.05f, mz + Math.min(f0, f1), mx + e * edge + 0.05f, mz + Math.max(f0, f1), y + 0.95f);
            }
        }
        // in the middle, where the paths cross: a small fountain, flower beds round it, lamps
        float rim = Garden.fountain(c.boxes, mx, y, mz, 2.3f);
        c.wall(mx - 2.1f, mz - 2.1f, mx + 2.1f, mz + 2.1f, rim);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                float bx = mx + sx * 3.3f, bz = mz + sz * 3.3f;
                Garden.pot(d, rnd, bx, y, bz, 0.4f);
                c.wall(bx - 0.35f, bz - 0.35f, bx + 0.35f, bz + 0.35f, y + 0.85f);
                Garden.lamp(d, mx + sx * 2.2f, y, mz + sz * (edge - 1.5f), 0f);
                c.wall(mx + sx * 2.2f - 0.2f, mz + sz * (edge - 1.5f) - 0.2f, mx + sx * 2.2f + 0.2f, mz + sz * (edge - 1.5f) + 0.2f);
            }
    }

    /**
     * Cars left along the curbs of the two streets the cell owns, each the way its side of the
     * road goes, now and then a gap; none near the crossings.
     */
    private void parked(Cell c, Random rnd, float x0, float z0) {
        float off = PARKED;
        for (float t = 13f; t < CELL - 13f; t += 5.6f)
            for (int s = -1; s <= 1; s += 2) {
                if (rnd.nextInt(10) < 7) continue;
                int paint = Cars.PAINT[rnd.nextInt(Cars.PAINT.length)];
                // its kind from a seed of its own, so the town is laid out as it was
                Random own = new Random(paint * 31L + (long) (t * 7f) + s * 1013L + (long) x0 * 17L + (long) z0 * 131L);
                int kind = Cars.kind(own);
                if (kind != Cars.SEDAN) paint = Cars.paint(own, kind);
                float len = Cars.half(kind) + 0.1f, top = Cars.top(kind);
                // the street running north: on its east side they face north, on its west side south
                if (rnd.nextBoolean()) {
                    float x = x0 + s * off, z = z0 + t;
                    Cars.car(c.parked, x, z, s > 0 ? 0f : (float) Math.PI, paint, true, kind);
                    c.wall(x - 0.95f, z - len, x + 0.95f, z + len, top);
                } else {
                    // the street running east: on its south side they face east, on its north side west
                    float x = x0 + t, z = z0 + s * off;
                    Cars.car(c.parked, x, z, s < 0 ? (float) (Math.PI / 2) : (float) (-Math.PI / 2), paint, true, kind);
                    c.wall(x - len, z - 0.95f, x + len, z + 0.95f, top);
                }
            }
    }

    /** Lamp posts along the curbs of the block, every so often. */
    private void lamps(Cell c, float x0, float z0) {
        DrawList d = c.detail;
        float in = CURB + 0.5f;
        for (float t = 14f; t < CELL - 8f; t += 18f) {
            lamp(c, d, x0 + in, z0 + t);
            lamp(c, d, x0 + t, z0 + in);
        }
    }

    private static void lamp(Cell c, DrawList d, float x, float z) {
        int iron = 0x2B2F3A;
        d.box(x, WALK_TOP + 2.2f, z, 0.07f, 2.2f, 0.07f, null, iron, DrawList.PLAIN);
        d.box(x, WALK_TOP + 0.25f, z, 0.14f, 0.25f, 0.14f, null, iron, DrawList.PLAIN);
        d.box(x, WALK_TOP + 4.4f, z, 0.135f, 0.185f, 0.135f, null, 0xF3E2B4, DrawList.GLOW);
        d.box(x, WALK_TOP + 4.62f, z, 0.2f, 0.04f, 0.2f, null, iron, DrawList.PLAIN);
        c.wall(x - 0.12f, z - 0.12f, x + 0.12f, z + 0.12f, WALK_TOP + 4.7f);
    }
}
