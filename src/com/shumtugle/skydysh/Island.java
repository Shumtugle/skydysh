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
        float[] at = new float[3];
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
        pierDrawn(far);
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

    /** Trees on the ledges and the top, thick on them, none on the rock or the sand. */
    private static void trees(DrawList d) {
        Random rnd = new Random(0x15A1DL);
        for (int k = 0; k < 1100; k++) {
            float a = rnd.nextFloat() * 6.2832f, r = rnd.nextFloat() * PR[4] * 0.98f;
            float w = wob(a), x = IX + (float) Math.sin(a) * r * w, z = IZ + (float) Math.cos(a) * r * w;
            // on a ledge or the top: where the ground is nearly level
            float h = height(x, z), hx = height(x + 1f, z), hz = height(x, z + 1f);
            if (Math.abs(hx - h) > 0.6f || Math.abs(hz - h) > 0.6f || h < 1f) continue;
            if (onPath(x, z, 2.2f)) continue;
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

    private static float pathR(float u) {
        return PR[3] * (1f - u) + 2f;
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

    /** The yachts at the pier, each rising and falling a little on the swell, as time goes. */
    static void yachts(DrawList d, float time) {
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
