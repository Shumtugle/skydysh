package com.shumtugle.skydysh;

import java.util.Random;

/**
 * Life at the water's edge: the fishermen on the rocks (see City.rocks), each sitting on the
 * highest rock with his rod out over the sea, the tip nodding, the line down into the water, now
 * and then the rod lifted and let down again; and, now and then, a shoal of little fish in the
 * clear shallows where she wades, a dark flicker under the water, turning together, darting off
 * when she comes near, and gone after a while.
 */
final class Shore {
    static final int SHOALS = 2, FISH = 11;
    // a shoal: how long it stays, and the wait before another; how far from her it comes, at most and least
    static final float STAYS = 16f, WAITS = 22f, FROM = 9f, TO = 24f;
    // the shallows they keep to: between the water's edge and so far out from it
    static final float BAND0 = 1.2f, BAND1 = 9f;

    private final float[] cx = new float[SHOALS], cz = new float[SHOALS], vx = new float[SHOALS], vz = new float[SHOALS],
            life = new float[SHOALS], wait = {4f, 13f}, phase = new float[SHOALS];
    private final Random rnd = new Random(0xF15L);
    private final float[] at = new float[4], turn = new float[9];

    /** A step on: the shoals swim, flee, go, and come again near her (x, z), if she is by the sea. */
    void update(float dt, float x, float z) {
        float edge = City.SHORE * City.CELL + City.WATERLINE;
        boolean bySea = z < City.SHORE * City.CELL + City.PARAPET + 6f && z > edge - 40f;
        for (int k = 0; k < SHOALS; k++) {
            if (life[k] <= 0f) {
                wait[k] -= dt;
                if (wait[k] > 0f || !bySea) continue;
                // a new one, out of the way of her, in the shallows
                float a = rnd.nextFloat() * 6.2832f, r = FROM + rnd.nextFloat() * (TO - FROM);
                cx[k] = x + (float) Math.cos(a) * r;
                cz[k] = edge - BAND0 - rnd.nextFloat() * (BAND1 - BAND0);
                float dir = rnd.nextBoolean() ? 1f : -1f;
                vx[k] = dir * 0.7f;
                vz[k] = 0f;
                life[k] = STAYS * (0.7f + rnd.nextFloat() * 0.6f);
                phase[k] = rnd.nextFloat() * 10f;
                continue;
            }
            life[k] -= dt;
            if (life[k] <= 0f) {
                wait[k] = WAITS * (0.6f + rnd.nextFloat() * 0.9f);
                continue;
            }
            phase[k] += dt;
            // wandering along the shore, turning now and then, all together
            float wander = (float) Math.sin(phase[k] * 0.5f) * 0.6f;
            float wantX = Math.signum(vx[k]) * 0.8f, wantZ = wander * 0.5f;
            float dx = cx[k] - x, dz = cz[k] - z, d = (float) Math.sqrt(dx * dx + dz * dz);
            if (d < 4.5f) {
                // she is near: off, fast, away from her
                wantX = dx / Math.max(d, 0.1f) * 3.2f;
                wantZ = dz / Math.max(d, 0.1f) * 3.2f;
            }
            vx[k] += (wantX - vx[k]) * Math.min(1f, dt * 2f);
            vz[k] += (wantZ - vz[k]) * Math.min(1f, dt * 2f);
            cx[k] += vx[k] * dt;
            cz[k] += vz[k] * dt;
            // in the band: off its edges they turn back
            float in = edge - cz[k];
            if (in < BAND0) cz[k] = edge - BAND0;
            if (in > BAND1) cz[k] = edge - BAND1;
        }
    }

    /** The shoals, as dark shapes just under the water's top; the fishermen and their rods. */
    void draw(DrawList d, City city, float ex, float ez, float time) {
        float y = City.WATER + 0.015f;
        for (int k = 0; k < SHOALS; k++) {
            if (life[k] <= 0f) continue;
            // coming and going: fewer at the start and the end
            float show = Math.min(1f, Math.min(life[k], STAYS - life[k] + 1f) / 2f);
            int n = Math.round(FISH * Math.max(0f, show));
            float h = (float) Math.atan2(vx[k], vz[k]);
            for (int f = 0; f < n; f++) {
                // set about the middle in a loose oval, each a little on its own
                float a = f * 2.39996f, r = 0.3f + 0.3f * (float) Math.sqrt(f);
                float ox = (float) Math.cos(a) * r * 1.4f, oz = (float) Math.sin(a) * r * 0.8f;
                float wob = (float) Math.sin(time * 6f + f * 1.7f) * 0.06f;
                float fx = cx[k] + ox * (float) Math.cos(h) + oz * (float) Math.sin(h) + wob;
                float fz = cz[k] - ox * (float) Math.sin(h) + oz * (float) Math.cos(h);
                float fh = h + (float) Math.sin(time * 9f + f * 2.3f) * 0.25f;
                yaw(fh);
                d.box(fx, y, fz, 0.06f, 0.004f, 0.17f, turn, 0x1C3E3C, DrawList.material(DrawList.PLAIN, 252));
            }
        }
        // the fishermen on the rocks of the cells about the eye
        int ci = City.index(ex);
        for (int i = ci - City.REACH; i <= ci + City.REACH; i++) {
            if (!city.fisher(i, at)) continue;
            float dx = at[0] - ex, dz = at[2] - ez;
            if (dx * dx + dz * dz > 160f * 160f) continue;
            fisher(d, i, at[0], at[1], at[2], at[3], time);
        }
    }

    /** A fisherman on his rock at (x, top, z), facing h: sitting, the rod out before him, its line down to the water. */
    private void fisher(DrawList d, int i, float x, float top, float z, float h, float time) {
        if (FolkPack.town == null || !Frida.ready()) return;
        int m = (i * 7 + 3 & 0x7FFFFFFF) % FolkPack.town.count;
        float y = top - Walkers.HIPS * FolkPack.town.size[m];
        FolkPack.sitter(d, m, x, y, z, h + (float) Math.PI, 1f, 0.08f, 0.2f * (float) Math.sin(time * 0.3f + i));
        // the rod: from his hands out and up over the water; the tip nodding, now and then lifted
        float fx = (float) Math.sin(h), fz = (float) Math.cos(h);
        float lift = Math.max(0f, (float) Math.sin(time * 0.21f + i * 1.3f) - 0.85f) * 4f;
        float nod = 0.03f * (float) Math.sin(time * 2.7f + i);
        float bx = x + fx * 0.35f, by = top + 0.75f, bz = z + fz * 0.35f;
        float len = 3.6f, up = 0.45f + nod + lift * 0.5f;
        float tx = bx + fx * len * (float) Math.cos(up), ty = by + len * (float) Math.sin(up), tz = bz + fz * len * (float) Math.cos(up);
        stick(d, bx, by, bz, tx, ty, tz, 0.022f, 0x3A2A1E);
        // the line, straight down from the tip into the sea a little further out
        float lx = tx + fx * 0.8f, lz = tz + fz * 0.8f;
        stick(d, tx, ty, tz, lx, City.WATER, lz, 0.006f, 0xE8E8E0);
        d.box(lx, City.WATER + 0.03f, lz, 0.04f, 0.04f, 0.04f, null, 0xE0503A, DrawList.PAINT);
    }

    /** A thin stick from (x0, y0, z0) to (x1, y1, z1), so thick. */
    private void stick(DrawList d, float x0, float y0, float z0, float x1, float y1, float z1, float r, int color) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0, len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-3f) return;
        // its length along local z: z the way it goes, x level across it, y the rest
        float zx = dx / len, zy = dy / len, zz = dz / len;
        float xx = zz, xy = 0f, xz = -zx, xl = (float) Math.sqrt(xx * xx + xz * xz);
        if (xl < 1e-4f) {
            xx = 1f;
            xz = 0f;
            xl = 1f;
        }
        xx /= xl;
        xz /= xl;
        float yx = zy * xz - zz * xy, yy = zz * xx - zx * xz, yz = zx * xy - zy * xx;
        turn[0] = xx; turn[1] = yx; turn[2] = zx;
        turn[3] = xy; turn[4] = yy; turn[5] = zy;
        turn[6] = xz; turn[7] = yz; turn[8] = zz;
        d.box((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, (z0 + z1) * 0.5f, r, r, len * 0.5f, turn, color, DrawList.PLAIN);
    }

    private void yaw(float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        turn[0] = c; turn[1] = 0f; turn[2] = s;
        turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
        turn[6] = -s; turn[7] = 0f; turn[8] = c;
    }
}
