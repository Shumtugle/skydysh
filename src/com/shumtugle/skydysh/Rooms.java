package com.shumtugle.skydysh;

import java.util.Random;

/**
 * Places indoors, made from a seed as the town is: the same house always has the same hall.
 * One plan so far, a house's hall (see Indoors): its shell, the lift and its cabin, and what
 * stands along the walls, chosen and laid out by the seed. Other plans for other missions go
 * here beside it, made of the same parts.
 */
final class Rooms {
    private Rooms() {
    }

    // what a hall may be made of: the tiles' light tone, the walls, the panelling below the rail
    private static final int[] TILE = {0xEDE3CF, 0xE6D3B3, 0xDCD6C6, 0xD4DDD2, 0xEAD8C8};
    private static final int[] WALL = {0xE9DCC4, 0xD9E1D3, 0xEBD3BC, 0xF0E7D2, 0xD3DAE0, 0xE8CFC0};
    private static final int[] LOW = {0x6A4A33, 0x6F7E66, 0x8A5A44, 0x56656E, 0x7A3E34};
    // what may stand along a wall
    private static final int MAILBOXES = 0, BENCH = 1, PLANT = 2, RADIATOR = 3, PICTURE = 4, BOARD = 5;

    /**
     * The hall of a house (as City keeps it: middle x, z; half sizes x, z; roof top; the way its
     * front faces, x, z; whether it has a lift), from the front door through to the yard (or,
     * when the back looks on no yard, shut at the back), with its lift in one side wall; or, in a
     * house with only a way in, no lift, the side wall whole.
     */
    /** The secretariat's counter in the town hall: its top over the floor. */
    static final float DESK_TOP = 1.05f;
    // the town hall's hall: half its width, its depth, its height; the gallery round it, its floor
    static final float TH_HALF = 8f, TH_DEEP = 24f, TH_HIGH = 11f, GALLERY = 5.2f;
    // the secretariat, a room of its own at the front on the left: its wall's place across, its
    // front and back, the doorway in its wall (from, to, in), where the clerk who gives out the
    // folders stands
    static final float SEC_U = 4.2f, SEC_FRONT = 2f, SEC_BACK = 10.5f, DOOR_V0 = 5.4f, DOOR_V1 = 7.6f, CLERK_U = 5.1f, CLERK_V = 6.5f;
    // the stairs: the foot of the middle flight, its top and the landing's back; the side
    // flights' inner and outer edges; the galleries' inner edge across, and where they begin
    static final float STAIR_V0 = 13.5f, STAIR_V1 = 18f, LANDING_V = 21f, MID_HALF = 1.8f, SIDE_IN = 3.4f, SIDE_OUT = 5.4f, GALLERY_V0 = 11.5f;

    /**
     * The town hall's hall, behind its middle door: two storeys high, its walls cream over a
     * marble skirting, a floor of marble laid on the diagonal, a red carpet from the door to the
     * stairs. Columns of pale marble with gilt heads along the way in; two chandeliers of crystal.
     * At the far end the grand stair: one wide flight up the middle to a landing, and from it two
     * flights turning back up either side to the gallery that runs round the hall at the upper
     * floor, its balustrade gilt; over the stair a skylight of coloured glass. The lift on the
     * right; on the left at the front the secretariat, a room of its own behind a doorway in a
     * gilt frame, a counter along its far wall and shelves of folders.
     */
    static Indoors townHall(float[] house) {
        float cx = house[0], cz = house[1], hx = house[2], hz = house[3], nx = house[5], nz = house[6];
        float halfN = Math.abs(nx) * hx + Math.abs(nz) * hz;
        float half = TH_HALF, deep = TH_DEEP;
        // the lift on her right as she comes in, the secretariat on her left
        int side = -1;
        float l = -side;
        Indoors in = new Indoors(cx + nx * halfN, cz + nz * halfN, nx, nz, half, deep, side, 8.5f, 5);
        in.backDoor = false;
        in.hasLift = true;
        in.townHall = true;
        in.high = TH_HIGH;
        in.frameColor = 0xC9A15A;
        in.frameMat = DrawList.GOLD;
        in.cabColor = 0x7A5236;
        int cream = 0xEFE3CC, marble = 0xE9E2D6, rose = 0xB8988A, gold = 0xC9A15A, wood = 0x4A3226, red = 0x9A2A2E;
        shell(in, 0xE6DCCB, true, cream, rose, DrawList.STONE);
        cabin(in);
        DrawList d = in.still();
        float f = Indoors.FLOOR, h = in.high, tall = f + h + 1f, g = f + GALLERY;
        // the floor of pale marble, a band of rose round it and a star of it in the middle of the way in
        in.box(d, 0f, f + 0.003f, deep * 0.5f, half, 0.003f, deep * 0.5f, 0xEAE2D4, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2) {
            in.box(d, e * (half - 0.7f), f + 0.005f, deep * 0.5f, 0.18f, 0.003f, deep * 0.5f - 0.7f, rose, DrawList.STONE);
            in.box(d, 0f, f + 0.005f, e < 0 ? 0.7f : deep - 0.7f, half - 0.7f, 0.003f, 0.18f, rose, DrawList.STONE);
        }
        for (float v = 2.5f; v < STAIR_V0 - 1f; v += 3.5f)
            for (int e = -1; e <= 1; e += 2) {
                float[] turn = {0.7071f, 0f, 0.7071f, 0f, 1f, 0f, -0.7071f, 0f, 0.7071f};
                // a rose diamond on either side of the carpet
                float u = e * 1.9f;
                d.box(in.wx(u, v), f + 0.005f, in.wz(u, v), 0.35f, 0.003f, 0.35f, turn, rose, DrawList.STONE);
            }
        // the carpet from the door to the stairs
        in.box(d, 0f, f + 0.006f, STAIR_V0 * 0.5f, 1.2f, 0.006f, STAIR_V0 * 0.5f - 0.3f, red, DrawList.GRAIN);
        in.box(d, 0f, f + 0.008f, STAIR_V0 * 0.5f, 1.25f, 0.004f, STAIR_V0 * 0.5f - 0.25f, gold, DrawList.GRAIN);
        in.box(d, 0f, f + 0.01f, STAIR_V0 * 0.5f, 1.1f, 0.006f, STAIR_V0 * 0.5f - 0.35f, red, DrawList.GRAIN);
        // pilasters along the walls, gilt, up to the cornice
        for (float v = 2f; v < deep - 1f; v += 3.5f)
            for (int e = -1; e <= 1; e += 2) {
                if (e == side && Math.abs(v - in.liftV) < 1.8f) continue;
                in.box(d, e * (half - 0.07f), f + h * 0.5f, v, 0.08f, h * 0.5f, 0.28f, 0xE2D6BE, DrawList.STONE);
                in.box(d, e * (half - 0.12f), f + h - 0.4f, v, 0.12f, 0.2f, 0.36f, gold, DrawList.GOLD);
            }
        for (int e = -1; e <= 1; e += 2)
            in.box(d, e * (half - 0.1f), f + h - 0.15f, deep * 0.5f, 0.15f, 0.15f, deep * 0.5f, gold, DrawList.GOLD);
        // the columns along the way in: marble shafts, gilt heads and feet; none across from the
        // secretariat's door
        for (float v = 3.5f; v < 11f; v += 3.5f)
            for (int e = -1; e <= 1; e += 2) {
                if (v > DOOR_V0 - 1f && v < DOOR_V1 + 1f) continue;
                float u = e * 3f;
                column(in, d, u, v, f, g - 0.3f, marble, gold);
                in.solid(u - 0.4f, v - 0.4f, u + 0.4f, v + 0.4f, tall);
            }
        // the grand stair: the middle flight
        int steps = 12;
        float rise = (GALLERY * 0.5f) / steps, run = (STAIR_V1 - STAIR_V0) / steps;
        for (int k = 0; k < steps; k++) {
            float top = f + rise * (k + 1), v = STAIR_V0 + run * (k + 0.5f);
            in.box(d, 0f, (f + top) * 0.5f, v, MID_HALF, (top - f) * 0.5f, run * 0.5f + 0.01f, marble, DrawList.STONE);
            in.box(d, 0f, top + 0.006f, v, MID_HALF - 0.35f, 0.006f, run * 0.5f, red, DrawList.GRAIN);
        }
        // the landing, the full width of the stair, against the back of it
        float land = f + GALLERY * 0.5f;
        in.box(d, 0f, (f + land) * 0.5f, (STAIR_V1 + LANDING_V) * 0.5f, SIDE_OUT, (land - f) * 0.5f, (LANDING_V - STAIR_V1) * 0.5f, marble, DrawList.STONE);
        in.box(d, 0f, land + 0.006f, (STAIR_V1 + LANDING_V) * 0.5f, SIDE_OUT - 0.4f, 0.006f, (LANDING_V - STAIR_V1) * 0.5f - 0.3f, red, DrawList.GRAIN);
        // the two side flights, back toward the door up to the gallery
        for (int e = -1; e <= 1; e += 2) {
            float u = e * (SIDE_IN + SIDE_OUT) * 0.5f, hu = (SIDE_OUT - SIDE_IN) * 0.5f;
            for (int k = 0; k < steps; k++) {
                float top = land + rise * (k + 1), v = STAIR_V1 - run * (k + 0.5f);
                in.box(d, u, (f + top) * 0.5f, v, hu, (top - f) * 0.5f, run * 0.5f + 0.01f, marble, DrawList.STONE);
                in.box(d, u, top + 0.006f, v, hu - 0.3f, 0.006f, run * 0.5f, red, DrawList.GRAIN);
            }
            // the balustrade up the inner edge of the side flight, and along the middle flight
            rail(in, d, e * SIDE_IN, STAIR_V1, land, STAIR_V0, g, gold, marble);
            rail(in, d, e * MID_HALF, STAIR_V0, f, STAIR_V1, land, gold, marble);
            // a lamp on a post at the foot of the middle flight
            float pu = e * (MID_HALF + 0.15f);
            in.box(d, pu, f + 0.6f, STAIR_V0 + 0.15f, 0.18f, 0.6f, 0.18f, marble, DrawList.STONE);
            in.box(d, pu, f + 1.5f, STAIR_V0 + 0.15f, 0.04f, 0.3f, 0.04f, gold, DrawList.GOLD);
            in.box(d, pu, f + 1.9f, STAIR_V0 + 0.15f, 0.2f, 0.2f, 0.2f, 0xFFE9BE, DrawList.GLOW);
        }
        in.solid(-SIDE_OUT, STAIR_V0, SIDE_OUT, LANDING_V, tall);
        in.solid(-MID_HALF - 0.4f, STAIR_V0 - 0.4f, -MID_HALF + 0.1f, STAIR_V0 + 0.1f, tall);
        in.solid(MID_HALF - 0.1f, STAIR_V0 - 0.4f, MID_HALF + 0.4f, STAIR_V0 + 0.1f, tall);
        // the gallery round the upper floor: its floor at either side and at the back, its front
        // faced in marble, a gilt rail on balusters, columns under its edge
        for (int e = -1; e <= 1; e += 2) {
            float u = e * (SIDE_OUT + half) * 0.5f, hu = (half - SIDE_OUT) * 0.5f;
            in.box(d, u, g - 0.2f, (GALLERY_V0 + deep) * 0.5f, hu, 0.2f, (deep - GALLERY_V0) * 0.5f, marble, DrawList.STONE);
            in.box(d, u, g + 0.006f, (GALLERY_V0 + deep) * 0.5f, hu - 0.2f, 0.006f, (deep - GALLERY_V0) * 0.5f, red, DrawList.GRAIN);
            rail(in, d, e * SIDE_OUT, GALLERY_V0, g, STAIR_V0, g, gold, marble);
            for (float v = 12f; v < deep - 1f; v += 4f) {
                if (v > STAIR_V0 - 0.5f && v < LANDING_V + 0.5f) continue;
                column(in, d, e * (SIDE_OUT + 0.3f), v, f, g - 0.4f, marble, gold);
                in.solid(e * (SIDE_OUT + 0.3f) - 0.4f, v - 0.4f, e * (SIDE_OUT + 0.3f) + 0.4f, v + 0.4f, tall);
            }
            in.box(d, e * (SIDE_OUT + 0.15f), g - 0.2f, (GALLERY_V0 + deep) * 0.5f, 0.15f, 0.25f, (deep - GALLERY_V0) * 0.5f, gold, DrawList.GOLD);
            // doors of dark wood along the gallery's wall, a gilt frame each
            for (float v = 13f; v < deep - 1f; v += 4f) {
                in.box(d, e * (half - 0.05f), g + 1.3f, v, 0.05f, 1.3f, 0.7f, wood, DrawList.WOOD);
                in.box(d, e * (half - 0.08f), g + 2.7f, v, 0.05f, 0.1f, 0.85f, gold, DrawList.GOLD);
            }
        }
        in.box(d, 0f, g - 0.2f, (LANDING_V + deep) * 0.5f, SIDE_OUT, 0.2f, (deep - LANDING_V) * 0.5f, marble, DrawList.STONE);
        in.box(d, 0f, g + 0.006f, (LANDING_V + deep) * 0.5f, SIDE_OUT, 0.006f, (deep - LANDING_V) * 0.5f - 0.2f, red, DrawList.GRAIN);
        // the back wall over the landing: a great window of pale glass in a gilt arch, the town's shield in it
        in.box(d, 0f, g + 2.4f, deep - 0.06f, 3f, 2.2f, 0.04f, 0xF4EEDC, DrawList.GLOW);
        in.box(d, 0f, g + 4.7f, deep - 0.1f, 3.2f, 0.15f, 0.06f, gold, DrawList.GOLD);
        for (int e = -1; e <= 1; e += 2) in.box(d, e * 3.1f, g + 2.4f, deep - 0.1f, 0.12f, 2.3f, 0.06f, gold, DrawList.GOLD);
        in.box(d, 0f, g + 2.4f, deep - 0.12f, 0.9f, 1.1f, 0.03f, 0x2E6A64, DrawList.PAINT);
        in.box(d, 0f, g + 2.2f, deep - 0.14f, 0.75f, 0.85f, 0.03f, 0xE6DCC6, DrawList.PAINT);
        // the skylight over the stair: pale amber glass in fine leading, a border of colour round it
        // and a round medallion in its middle
        float sv0 = STAIR_V0 - 0.5f, sv1 = deep - 1f, su = 4.5f;
        int nu = 9, nv = 11;
        float pu = 2f * su / nu, pv = (sv1 - sv0) / nv, mu = 0f, mv = (sv0 + sv1) * 0.5f;
        for (int a = 0; a < nu; a++)
            for (int b = 0; b < nv; b++) {
                float u = -su + (a + 0.5f) * pu, v = sv0 + (b + 0.5f) * pv;
                boolean rim = a == 0 || b == 0 || a == nu - 1 || b == nv - 1;
                float r = (float) Math.hypot((u - mu) / pu, (v - mv) / pv);
                int c = rim ? ((a + b) % 2 == 0 ? 0x9AB4D8 : 0xE0A8A8) : r < 1.2f ? 0xF0C878 : r < 2.2f ? ((a + b) % 2 == 0 ? 0xA8C8A0 : 0xE0A8A8) : 0xF4E6C4;
                in.box(d, u, f + h - 0.04f, v, pu * 0.5f - 0.04f, 0.02f, pv * 0.5f - 0.04f, c, DrawList.GLOW);
            }
        in.box(d, 0f, f + h - 0.02f, (sv0 + sv1) * 0.5f, su + 0.25f, 0.015f, (sv1 - sv0) * 0.5f + 0.25f, 0x5A4A32, DrawList.PLAIN);
        in.box(d, 0f, f + h - 0.06f, (sv0 + sv1) * 0.5f, su + 0.4f, 0.03f, (sv1 - sv0) * 0.5f + 0.4f, gold, DrawList.GOLD);
        // two chandeliers of crystal over the way in
        for (int k = 0; k < 2; k++) chandelier(in, d, 0f, 4.5f + k * 5f, f + h - 3.2f, gold);
        // the secretariat: its wall along the hall, dark wood panelling low and over it windows
        // open onto the hall (posts between the panes, a gilt sill and head), so the room and who
        // is in it are seen from the hall; the doorway between them; its front; its ceiling
        float wu = l * SEC_U, t = 0.12f, sh = 4.2f, sill = 1.05f, head = 2.9f;
        for (int k = 0; k < 2; k++) {
            float v0 = k == 0 ? SEC_FRONT : DOOR_V1, v1 = k == 0 ? DOOR_V0 : SEC_BACK, vm = (v0 + v1) * 0.5f, vh = (v1 - v0) * 0.5f;
            in.box(d, wu, f + sill * 0.5f, vm, t, sill * 0.5f, vh, wood, DrawList.WOOD);
            in.box(d, wu, f + (head + sh) * 0.5f, vm, t, (sh - head) * 0.5f, vh, cream, DrawList.PLAIN);
            in.box(d, wu, f + sill + 0.04f, vm, t + 0.08f, 0.04f, vh, gold, DrawList.GOLD);
            in.box(d, wu, f + head - 0.05f, vm, t + 0.04f, 0.06f, vh, gold, DrawList.GOLD);
            int panes = Math.max(2, Math.round((v1 - v0) / 1.2f));
            for (int q = 0; q <= panes; q++)
                in.box(d, wu, f + (sill + head) * 0.5f, v0 + (v1 - v0) * q / panes, t, (head - sill) * 0.5f, 0.07f, wood, DrawList.WOOD);
            in.solid(Math.min(wu - t, wu + t), v0, Math.max(wu - t, wu + t), v1, tall);
        }
        in.box(d, wu, f + 3f + (sh - 3f) * 0.5f, (DOOR_V0 + DOOR_V1) * 0.5f, t, (sh - 3f) * 0.5f, (DOOR_V1 - DOOR_V0) * 0.5f, cream, DrawList.PLAIN);
        float mid = (SEC_U + half) * 0.5f, hw = (half - SEC_U) * 0.5f;
        for (int k = 0; k < 2; k++) {
            float v = k == 0 ? SEC_FRONT : SEC_BACK;
            in.box(d, l * mid, f + sh * 0.5f, v, hw, sh * 0.5f, t, cream, DrawList.PLAIN);
            in.solid(Math.min(l * SEC_U, l * half), v - t, Math.max(l * SEC_U, l * half), v + t, tall);
        }
        in.box(d, l * mid, f + sh + 0.1f, (SEC_FRONT + SEC_BACK) * 0.5f, hw, 0.1f, (SEC_BACK - SEC_FRONT) * 0.5f, 0xF4EEDC, DrawList.PLAIN);
        in.box(d, l * mid, f + sh + 0.25f, (SEC_FRONT + SEC_BACK) * 0.5f, hw + 0.05f, 0.06f, (SEC_BACK - SEC_FRONT) * 0.5f + 0.05f, gold, DrawList.GOLD);
        // a lamp hung in its middle: a chain, a shade of warm glass
        float lv = (SEC_FRONT + SEC_BACK) * 0.5f;
        in.box(d, l * mid, f + sh - 0.35f, lv, 0.02f, 0.35f, 0.02f, gold, DrawList.GOLD);
        in.box(d, l * mid, f + sh - 0.8f, lv, 0.42f, 0.12f, 0.42f, 0xFFE2B0, DrawList.GLOW);
        in.box(d, l * mid, f + sh - 0.66f, lv, 0.46f, 0.03f, 0.46f, gold, DrawList.GOLD);
        // its own look: a parquet floor and a rug on it; its walls green over dark wood panelling
        float su0 = SEC_U + t, su1 = half, smu = l * (su0 + su1) * 0.5f, shu = (su1 - su0) * 0.5f;
        float rv0 = SEC_FRONT + t, rv1 = SEC_BACK - t, smv = (rv0 + rv1) * 0.5f, shv = (rv1 - rv0) * 0.5f;
        int green = 0x3F5E4E, panel = 0x5A3B28;
        in.box(d, smu, f + 0.012f, smv, shu, 0.004f, shv, 0x8A5A3A, DrawList.GRAIN);
        in.box(d, l * (SEC_U + 1.5f), f + 0.02f, smv, 0.9f, 0.004f, 2.6f, 0x7A2A30, DrawList.GRAIN);
        in.box(d, l * (SEC_U + 1.5f), f + 0.022f, smv, 0.75f, 0.004f, 2.45f, 0xB08A4A, DrawList.GRAIN);
        in.box(d, l * (SEC_U + 1.5f), f + 0.024f, smv, 0.68f, 0.004f, 2.38f, 0x7A2A30, DrawList.GRAIN);
        for (int k = 0; k < 2; k++) {
            float v = k == 0 ? rv0 + 0.01f : rv1 - 0.01f;
            in.box(d, smu, f + 0.55f, v, shu, 0.55f, 0.01f, panel, DrawList.WOOD);
            in.box(d, smu, f + (1.1f + sh) * 0.5f, v, shu, (sh - 1.1f) * 0.5f, 0.01f, green, DrawList.PAINT);
            in.box(d, smu, f + 1.12f, v, shu, 0.03f, 0.02f, gold, DrawList.GOLD);
        }
        in.box(d, l * (half - 0.02f), f + 0.55f, smv, 0.01f, 0.55f, shv, panel, DrawList.WOOD);
        in.box(d, l * (half - 0.02f), f + (1.1f + sh) * 0.5f, smv, 0.01f, (sh - 1.1f) * 0.5f, shv, green, DrawList.PAINT);
        // a clock on its front wall; a palm in a pot in the corner; two chairs for callers
        in.box(d, l * (SEC_U + 1.9f), f + 3.1f, rv0 + 0.04f, 0.32f, 0.32f, 0.03f, 0xF4EEDC, DrawList.PLAIN);
        in.box(d, l * (SEC_U + 1.9f), f + 3.1f, rv0 + 0.03f, 0.37f, 0.37f, 0.02f, gold, DrawList.GOLD);
        in.box(d, l * (SEC_U + 1.9f), f + 3.2f, rv0 + 0.08f, 0.02f, 0.13f, 0.01f, 0x23262C, DrawList.PLAIN);
        in.box(d, l * (SEC_U + 1.98f), f + 3.1f, rv0 + 0.08f, 0.09f, 0.02f, 0.01f, 0x23262C, DrawList.PLAIN);
        float plu = l * (half - 0.55f), plv = rv0 + 0.55f;
        in.box(d, plu, f + 0.3f, plv, 0.28f, 0.3f, 0.28f, 0x9A5A3A, DrawList.STONE);
        for (int k = 0; k < 5; k++) {
            float a = k * 1.256f;
            in.box(d, plu + (float) Math.sin(a) * 0.35f, f + 1.15f + (k % 2) * 0.2f, plv + (float) Math.cos(a) * 0.35f,
                    0.28f, 0.05f, 0.12f, 0x3E7A3A, DrawList.PAINT);
        }
        in.box(d, plu, f + 0.85f, plv, 0.05f, 0.35f, 0.05f, 0x5A4030, DrawList.WOOD);
        in.solid(Math.min(plu - 0.35f, plu + 0.35f), plv - 0.35f, Math.max(plu - 0.35f, plu + 0.35f), plv + 0.35f, f + 1.4f);
        for (int k = 0; k < 2; k++) {
            float cu = l * (half - 1.9f), cv = k == 0 ? 4.4f : 8.4f;
            in.box(d, cu, f + 0.48f, cv, 0.26f, 0.04f, 0.26f, 0x7A2A30, DrawList.GRAIN);
            in.box(d, cu - l * 0.24f, f + 0.85f, cv, 0.03f, 0.35f, 0.26f, wood, DrawList.WOOD);
            for (int a = -1; a <= 1; a += 2)
                for (int b = -1; b <= 1; b += 2)
                    in.box(d, cu + a * 0.22f, f + 0.22f, cv + b * 0.22f, 0.025f, 0.22f, 0.025f, wood, DrawList.WOOD);
        }
        // the doorway's gilt frame and the plate over it
        for (int e = 0; e < 2; e++)
            in.box(d, wu - l * 0.14f, f + 1.5f, e == 0 ? DOOR_V0 : DOOR_V1, 0.05f, 1.5f, 0.07f, gold, DrawList.GOLD);
        in.box(d, wu - l * 0.14f, f + 3.05f, (DOOR_V0 + DOOR_V1) * 0.5f, 0.05f, 0.07f, (DOOR_V1 - DOOR_V0) * 0.5f + 0.07f, gold, DrawList.GOLD);
        in.box(d, wu - l * 0.13f, f + 3.5f, (DOOR_V0 + DOOR_V1) * 0.5f, 0.02f, 0.25f, 0.78f, gold, DrawList.GOLD);
        in.box(d, wu - l * 0.15f, f + 3.5f, (DOOR_V0 + DOOR_V1) * 0.5f, 0.02f, 0.2f, 0.7f, 0x2B3A52, DrawList.PAINT);
        // in it: the counter along its far wall, shelves of folders over it
        float cu = l * (half - 0.9f), cv = (SEC_FRONT + SEC_BACK) * 0.5f, len = 2.6f;
        in.box(d, cu, f + DESK_TOP * 0.5f, cv, 0.35f, DESK_TOP * 0.5f, len, wood, DrawList.WOOD);
        in.box(d, cu - l * 0.05f, f + DESK_TOP + 0.03f, cv, 0.45f, 0.03f, len + 0.08f, 0x6A4A36, DrawList.WOOD);
        in.solid(Math.min(cu - 0.4f, cu + 0.4f), cv - len - 0.1f, Math.max(cu - 0.4f, cu + 0.4f), cv + len + 0.1f, f + 1.2f);
        // on it, a lamp of green glass, a stack of papers, an inkstand
        in.box(d, cu, f + DESK_TOP + 0.2f, cv - 1.4f, 0.02f, 0.14f, 0.02f, gold, DrawList.GOLD);
        in.box(d, cu, f + DESK_TOP + 0.36f, cv - 1.4f, 0.12f, 0.06f, 0.2f, 0x2E7A4E, DrawList.GLASS);
        in.box(d, cu, f + DESK_TOP + 0.3f, cv - 1.4f, 0.09f, 0.02f, 0.16f, 0xFFE9BE, DrawList.GLOW);
        in.box(d, cu, f + DESK_TOP + 0.1f, cv + 0.9f, 0.18f, 0.04f, 0.24f, 0xEFE6D2, DrawList.PLAIN);
        in.box(d, cu, f + DESK_TOP + 0.09f, cv + 1.6f, 0.08f, 0.03f, 0.12f, 0x23262C, DrawList.POLISHED);
        int[] card = {0x5C7088, 0x8A6A4A, 0x6E7F92, 0x7A5A3A, 0x56687C, 0xB0302A};
        float wallU = l * half;
        for (int r = 0; r < 3; r++) {
            float sy = f + 1.4f + r * 0.55f;
            in.box(d, wallU - l * 0.2f, sy, cv, 0.2f, 0.025f, len + 0.4f, wood, DrawList.WOOD);
            for (int k = 0; k < 16; k++)
                in.box(d, wallU - l * 0.2f, sy + 0.22f, cv - len - 0.2f + k * 0.33f, 0.16f, 0.2f, 0.06f, card[(k + r * 2) % card.length], DrawList.PAINT);
        }
        // a bench by the right wall at the front; the town's flag by the door
        float bu = side * (half - 0.45f);
        in.box(d, bu, f + 0.45f, 3.6f, 0.25f, 0.04f, 1.1f, wood, DrawList.WOOD);
        in.box(d, bu + side * 0.22f, f + 0.75f, 3.6f, 0.03f, 0.3f, 1.1f, wood, DrawList.WOOD);
        for (int e = -1; e <= 1; e += 2) in.box(d, bu, f + 0.21f, 3.6f + e * 0.95f, 0.22f, 0.21f, 0.05f, 0x2A2622, DrawList.PLAIN);
        in.solid(Math.min(bu - 0.3f, bu + 0.3f), 2.5f, Math.max(bu - 0.3f, bu + 0.3f), 4.7f, f + 0.9f);
        in.box(d, side * (half - 0.4f), f + 1.6f, 1.1f, 0.04f, 1.6f, 0.04f, gold, DrawList.GOLD);
        in.box(d, side * (half - 0.4f), f + 2.6f, 1.55f, 0.02f, 0.5f, 0.42f, 0x3E5872, DrawList.PAINT);
        in.deskU = l * CLERK_U;
        in.deskV = CLERK_V;
        in.folkAt = new float[WALKS.length];
        in.folkGo = new float[WALKS.length];
        java.util.Arrays.fill(in.folkGo, 1f);
        in.done();
        return in;
    }

    /** A column of marble from y0 to y1 at (u, v): a square foot and a gilt ring over it, the shaft, a gilt head. */
    private static void column(Indoors in, DrawList d, float u, float v, float y0, float y1, int marble, int gold) {
        in.box(d, u, y0 + 0.2f, v, 0.42f, 0.2f, 0.42f, marble, DrawList.STONE);
        in.box(d, u, y0 + 0.43f, v, 0.36f, 0.03f, 0.36f, gold, DrawList.GOLD);
        in.box(d, u, (y0 + y1) * 0.5f, v, 0.3f, (y1 - y0) * 0.5f - 0.4f, 0.3f, marble, DrawList.STONE);
        in.box(d, u, y1 - 0.25f, v, 0.4f, 0.12f, 0.4f, gold, DrawList.GOLD);
        in.box(d, u, y1 - 0.08f, v, 0.46f, 0.08f, 0.46f, marble, DrawList.STONE);
    }

    /** A balustrade at u from (v0, y0) to (v1, y1): balusters of marble, a gilt rail on them. */
    private static void rail(Indoors in, DrawList d, float u, float v0, float y0, float v1, float y1, int gold, int marble) {
        int n = Math.max(2, Math.round(Math.abs(v1 - v0) / 0.45f));
        for (int k = 0; k <= n; k++) {
            float a = k / (float) n, v = v0 + (v1 - v0) * a, y = y0 + (y1 - y0) * a;
            in.box(d, u, y + 0.45f, v, 0.06f, 0.45f, 0.06f, marble, DrawList.STONE);
        }
        // the rail as short pieces, stepping with the flight
        for (int k = 0; k < n; k++) {
            float a = (k + 0.5f) / n, v = v0 + (v1 - v0) * a, y = y0 + (y1 - y0) * a;
            in.box(d, u, y + 0.95f, v, 0.08f, 0.05f, Math.abs(v1 - v0) / n * 0.5f + 0.02f, gold, DrawList.GOLD);
        }
    }

    /** A chandelier of crystal hung at (u, v), its middle at y: a chain, gilt rings, lights and drops. */
    private static void chandelier(Indoors in, DrawList d, float u, float v, float y, int gold) {
        float top = Indoors.FLOOR + in.high;
        in.box(d, u, (y + top) * 0.5f, v, 0.03f, (top - y) * 0.5f, 0.03f, gold, DrawList.GOLD);
        for (int r = 0; r < 3; r++) {
            float rr = 1.3f - r * 0.4f, ry = y - 0.4f + r * 0.45f;
            int n = 10 - r * 2;
            for (int k = 0; k < n; k++) {
                double a = k * 2 * Math.PI / n + r * 0.3f;
                float px = u + (float) Math.sin(a) * rr, pv = v + (float) Math.cos(a) * rr;
                in.box(d, px, ry, pv, 0.06f, 0.02f, 0.06f, gold, DrawList.GOLD);
                in.box(d, px, ry + 0.1f, pv, 0.05f, 0.08f, 0.05f, 0xFFF1D0, DrawList.GLOW);
                in.box(d, px, ry - 0.2f, pv, 0.025f, 0.12f, 0.025f, 0xDDE8F0, DrawList.GLASS);
            }
            in.box(d, u, ry, v, rr, 0.02f, 0.03f, gold, DrawList.GOLD);
            in.box(d, u, ry, v, 0.03f, 0.02f, rr, gold, DrawList.GOLD);
        }
        in.box(d, u, y - 0.75f, v, 0.12f, 0.3f, 0.12f, 0xDDE8F0, DrawList.GLASS);
    }

    // the people walking about the town hall's hall: each a loop of points (across, in), the
    // floor they walk on (0 the hall's, 1 the gallery's), so far along it at first, their pace
    private static final float[][] WALKS = {
        {-2f, 1.8f, -2f, 12f, 2f, 12f, 2f, 1.8f},
        {2f, 12f, 2f, 1.8f, -2f, 1.8f, -2f, 12f},
        {-4.4f, 2.4f, -4.4f, 12.6f, -6.6f, 12.6f, -6.6f, 5.8f},
        {-1.4f, 12.6f, 2.4f, 12.6f, 2.4f, 10.6f, -1.4f, 10.6f},
        {-6.6f, 22.6f, 6.6f, 22.6f},
        {6.7f, 12.2f, 6.7f, 22.6f},
        {-6.7f, 22f, -6.7f, 12.2f},
    };
    private static final float[] WALK_LEVEL = {0f, 0f, 0f, 0f, 1f, 1f, 1f};
    private static final float[] WALK_FROM = {0f, 9f, 3f, 1f, 4f, 2f, 6f};
    private static final float[] WALK_PACE = {1.15f, 1.0f, 1.25f, 0.8f, 0.9f, 1.05f, 0.95f};
    private static Stroll.Way[] ways;
    // on the bench, a little way along it
    private static final float[] ON_BENCH = {3.1f, 4.1f};

    /**
     * A step of the town hall's people: each walks on round their loop, but stands a moment when
     * she is near (where the runner is, across and in, and whether up on the gallery).
     */
    static void townHallStep(Indoors in, float dt, Runner r) {
        if (in.folkAt == null) return;
        float ru = in.u(r.x, r.z), rv = in.v(r.x, r.z);
        for (int k = 0; k < WALKS.length; k++) {
            walkAt(k, in.folkAt[k] + WALK_FROM[k], pt);
            boolean near = WALK_LEVEL[k] == 0f && (pt[0] - ru) * (pt[0] - ru) + (pt[1] - rv) * (pt[1] - rv) < 2.2f * 2.2f;
            float want = near ? 0f : 1f;
            in.folkGo[k] += (want - in.folkGo[k]) * Math.min(1f, dt * 4f);
            in.folkAt[k] += dt * WALK_PACE[k] * in.folkGo[k];
        }
    }

    private static final float[] pt = new float[3];

    /** Where on loop k one is, so far along it: (across, in, the heading's across and in, as the way goes) into out. */
    private static void walkAt(int k, float s, float[] out) {
        float[] w = WALKS[k];
        int n = w.length / 2;
        boolean back = n == 2;
        float total = 0f;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            if (back && i == 1) j = 0;
            total += (float) Math.hypot(w[j * 2] - w[i * 2], w[j * 2 + 1] - w[i * 2 + 1]);
        }
        s = s % total;
        if (s < 0f) s += total;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            float dx = w[j * 2] - w[i * 2], dz = w[j * 2 + 1] - w[i * 2 + 1], len = (float) Math.hypot(dx, dz);
            if (s <= len || i == n - 1) {
                float a = Math.min(1f, s / Math.max(len, 1e-3f));
                out[0] = w[i * 2] + dx * a;
                out[1] = w[i * 2 + 1] + dz * a;
                out[2] = (float) Math.atan2(dx, dz);
                return;
            }
            s -= len;
        }
    }

    /**
     * The town hall's people: walking about the hall and the gallery, two on the bench, a clerk
     * sitting behind the secretariat's counter, and just inside its door the one who gives out
     * the folders, holding one in both hands while she has it (holding); where her hands are into
     * hands.
     */
    static void townHallFolk(DrawList d, Indoors in, float time, boolean holding, float[] hands) {
        if (FolkPack.town == null || !Frida.ready() || in.folkAt == null) return;
        int n = FolkPack.town.count;
        float f = Indoors.FLOOR, l = -in.side;
        if (ways == null) {
            ways = new Stroll.Way[WALKS.length];
            for (int k = 0; k < WALKS.length; k++) ways[k] = Stroll.way(0x7A11L + k * 31L, FolkPack.town.woman((k * 5 + 2) % n));
        }
        for (int k = 0; k < WALKS.length; k++) {
            float s = in.folkAt[k] + WALK_FROM[k];
            walkAt(k, s, pt);
            int m = (k * 5 + 2) % n;
            float y = f + WALK_LEVEL[k] * GALLERY;
            float yaw = (float) Math.atan2(in.wx(pt[0] + (float) Math.sin(pt[2]), pt[1] + (float) Math.cos(pt[2])) - in.wx(pt[0], pt[1]),
                    in.wz(pt[0] + (float) Math.sin(pt[2]), pt[1] + (float) Math.cos(pt[2])) - in.wz(pt[0], pt[1]));
            FolkPack.walker(d, m, in.wx(pt[0], pt[1]), y, in.wz(pt[0], pt[1]), yaw + (float) Math.PI, 1f, ways[k], s, in.folkGo[k]);
        }
        float bu = in.side * (in.half - 0.55f);
        for (int k = 0; k < ON_BENCH.length; k++) {
            int m = (k * 7 + 4) % n;
            float y = f + 0.5f - Walkers.HIPS * FolkPack.town.size[m];
            // talking: the heads turning to each other and away now and then
            float look = (k == 0 ? 0.35f : -0.35f) + 0.25f * (float) Math.sin(time * (0.6f + k * 0.17f) + k * 2f);
            FolkPack.sitter(d, m, in.wx(bu, ON_BENCH[k]), y, in.wz(bu, ON_BENCH[k]), in.heading(-in.side, 0f) + (float) Math.PI, 1f,
                    0.05f + 0.03f * (float) Math.sin(time * 0.8f + k), look);
        }
        // the clerk behind the counter, sitting, facing the room, looking up and down at her papers
        int c = 11 % n;
        float cu = l * (in.half - 0.35f), cv = (SEC_FRONT + SEC_BACK) * 0.5f;
        float y = f + 0.5f - Walkers.HIPS * FolkPack.town.size[c];
        FolkPack.sitter(d, c, in.wx(cu, cv), y, in.wz(cu, cv), in.heading(-l, 0f) + (float) Math.PI, 1f, 0.02f, 0.3f * (float) Math.sin(time * 0.5f));
        // the one who gives out the folders, just inside the door, facing it
        int w = FolkPack.womanFrom(6);
        float a = in.heading(-l, 0f) + (float) Math.PI;
        float wx = in.wx(in.deskU, in.deskV), wz = in.wz(in.deskU, in.deskV);
        if (holding) FolkPack.holder(d, w, wx, f, wz, a, 1f, hands);
        else FolkPack.walker(d, w, wx, f, wz, a, 1f, Stroll.way(0x5EC1L, true), 0f, 0f);
    }

    static Indoors hall(float[] house, long seed, boolean backDoor, boolean lift) {
        Random rnd = new Random(seed);
        float cx = house[0], cz = house[1], hx = house[2], hz = house[3], top = house[4], nx = house[5], nz = house[6];
        float halfN = Math.abs(nx) * hx + Math.abs(nz) * hz;
        float deep = 2f * halfN;
        float half = 2.3f + rnd.nextFloat() * 0.8f;
        int side = rnd.nextBoolean() ? 1 : -1;
        float liftV = deep * 0.5f + (rnd.nextFloat() - 0.5f) * 1.6f;
        int floors = Math.max(2, Math.round((top - 0.5f) / 3.3f) - 1);
        Indoors in = new Indoors(cx + nx * halfN, cz + nz * halfN, nx, nz, half, deep, side, liftV, floors);
        in.backDoor = backDoor;
        in.hasLift = lift;
        boolean chrome = rnd.nextInt(3) == 0;
        if (chrome) {
            in.frameColor = 0xAEB4B6;
            in.frameMat = DrawList.PLAIN;
        }
        in.cabColor = rnd.nextBoolean() ? 0x7A5236 : 0x5E4030;
        int tile = TILE[rnd.nextInt(TILE.length)], wall = WALL[rnd.nextInt(WALL.length)], low = LOW[rnd.nextInt(LOW.length)];
        float lowMat = rnd.nextInt(3) == 0 ? DrawList.PAINT : DrawList.WOOD;
        shell(in, tile, rnd.nextBoolean(), wall, low, lowMat);
        if (lift) cabin(in);
        lamps(in, 2 + rnd.nextInt(2));
        if (rnd.nextBoolean()) {
            // a runner of carpet along the hall
            in.box(in.still(), 0f, Indoors.FLOOR + 0.006f, deep * 0.5f, 0.75f, 0.006f, deep * 0.5f - 2.2f,
                    rnd.nextBoolean() ? 0x8A2E2A : 0x2E4A5A, DrawList.GRAIN);
        }
        furnish(in, rnd, low);
        in.done();
        return in;
    }

    /**
     * The concert hall's room, behind its doors on the street: a long hall under a high ceiling,
     * cream walls with gilt pilasters, a carpet of wine red, three lamps hung from the ceiling;
     * rows of seats in three blocks with an aisle between each, a narrow aisle along each wall; at the
     * far end a stage of boards a step high, red curtains behind it, a grand piano on it, black,
     * its lid up, and the bench before its keys. The seats' rows and the stage stand in her way.
     */
    static Indoors concert(float doorX, float doorZ) {
        Indoors in = new Indoors(doorX, doorZ, 0f, -1f, Concert.HALF, Concert.DEEP, 1, 4f, 1);
        in.backDoor = false;
        in.hasLift = false;
        in.concert = true;
        in.high = Concert.HIGH;
        in.frameColor = 0xC9A15A;
        in.frameMat = DrawList.GOLD;
        int cream = 0xEFE3CC, wine = 0x7E2A2A;
        shell(in, wine, false, cream, 0x5E1E22, DrawList.PAINT);
        DrawList d = in.still();
        float f = Indoors.FLOOR, w = Indoors.WALL, half = in.half, deep = in.deep, top = f + in.high;
        // the carpet over the tiles; down each of the two aisles a lighter runner edged with gilt,
        // and one across behind the back row, from the door
        in.box(d, 0f, f + 0.004f, deep * 0.5f, half, 0.004f, deep * 0.5f - w, wine, DrawList.GRAIN);
        float rv0 = Concert.ROW0 - 1.2f, rv1 = Concert.STAGE;
        for (int e = -1; e <= 1; e += 2) {
            float au = e * Concert.AISLE_U;
            in.box(d, au, f + 0.008f, (rv0 + rv1) * 0.5f, 0.62f, 0.004f, (rv1 - rv0) * 0.5f, 0xA8423E, DrawList.GRAIN);
            for (int g = -1; g <= 1; g += 2)
                in.box(d, au + g * 0.64f, f + 0.01f, (rv0 + rv1) * 0.5f, 0.03f, 0.004f, (rv1 - rv0) * 0.5f, 0xC9A15A, DrawList.GOLD);
        }
        in.box(d, 0f, f + 0.008f, rv0 - 0.6f, Concert.AISLE_U + 0.62f, 0.004f, 0.6f, 0xA8423E, DrawList.GRAIN);
        // gilt pilasters along the walls, a cornice under the ceiling
        for (float v = 3f; v < deep - 1f; v += 4f)
            for (int e = -1; e <= 1; e += 2)
                in.box(d, e * (half - 0.06f), f + in.high * 0.5f, v, 0.08f, in.high * 0.5f, 0.25f, 0xC9A15A, DrawList.GOLD);
        for (int e = -1; e <= 1; e += 2)
            in.box(d, e * (half - 0.1f), top - 0.3f, deep * 0.5f, 0.12f, 0.15f, deep * 0.5f - w, 0xC9A15A, DrawList.GOLD);
        // the lamps hung from the ceiling, their glass out (it glows once the lights are on: see Concert)
        for (int k = 0; k < Concert.LAMPS; k++) {
            float v = Concert.LAMP0 + k * Concert.LAMP_STEP;
            in.box(d, 0f, top - 1.1f, v, 0.03f, 1.1f, 0.03f, 0xC9A15A, DrawList.GOLD);
            in.box(d, 0f, top - 2.3f, v, 0.75f, 0.12f, 0.75f, 0xC9A15A, DrawList.GOLD);
            in.box(d, 0f, top - 2.16f, v, 0.6f, 0.1f, 0.6f, 0x6E6A60, DrawList.PLAIN);
        }
        // the one lamp that burns, on its own line: a gilt arm off the right wall, a shade alight
        float lv = Concert.SCONCE_V, ly = f + Concert.SCONCE_Y, rs = Concert.RIGHT, wallU = rs * (half - 0.06f);
        in.box(d, wallU - rs * 0.1f, ly - 0.25f, lv, 0.1f, 0.18f, 0.1f, 0xC9A15A, DrawList.GOLD);
        in.box(d, wallU - rs * 0.3f, ly - 0.1f, lv, 0.22f, 0.03f, 0.03f, 0xC9A15A, DrawList.GOLD);
        in.box(d, wallU - rs * 0.45f, ly + 0.12f, lv, 0.16f, 0.2f, 0.16f, 0xFFE2B0, DrawList.GLOW);
        in.dark = true;
        in.lampU = wallU - rs * 0.45f;
        in.lampV = lv;
        in.lampY = Concert.SCONCE_Y;
        in.lampReach = 9f;
        // the door to the wings, in the same wall before the stage: plain boards, a steel handle, a plate
        float wv = Concert.WINGS_V, wh = Concert.WINGS_HIGH;
        in.box(d, wallU - rs * 0.02f, f + wh * 0.5f + 0.06f, wv, 0.04f, wh * 0.5f + 0.06f, Concert.WINGS_HALF + 0.08f, 0x2A1E18, DrawList.WOOD);
        in.box(d, wallU - rs * 0.05f, f + wh * 0.5f, wv, 0.04f, wh * 0.5f, Concert.WINGS_HALF, 0x5A4232, DrawList.WOOD);
        in.box(d, wallU - rs * 0.1f, f + 1.05f, wv - Concert.WINGS_HALF + 0.12f, 0.04f, 0.03f, 0.1f, 0x9AA3A8, DrawList.POLISHED);
        in.box(d, wallU - rs * 0.1f, f + 1.75f, wv, 0.02f, 0.08f, 0.2f, 0xE8E1CC, DrawList.PLAIN);
        // the seats: rows in three blocks, the middle and one by each wall, an aisle between each;
        // each seat a red cushion and a back, gilt on its arms; each block's row stands in her way
        for (int r = 0; r < Concert.ROWS; r++) {
            float v = Concert.rowV(r);
            float[] lo = {1e9f, 1e9f, 1e9f}, hi = {-1e9f, -1e9f, -1e9f};
            for (int k = 0; k < Concert.SEATS; k++) {
                if (Concert.underChair(r, k)) continue;
                float u = Concert.seatU(k);
                in.box(d, u, f + 0.22f, v, 0.38f, 0.22f, 0.36f, 0x3A2A24, DrawList.WOOD);
                in.box(d, u, f + 0.47f, v, 0.36f, 0.05f, 0.34f, 0xA8323A, DrawList.GRAIN);
                // the back on the side of the doors, the stage before whoever sits there
                in.box(d, u, f + 0.75f, v - 0.33f, 0.36f, 0.32f, 0.05f, 0xA8323A, DrawList.GRAIN);
                in.box(d, u + 0.42f, f + 0.6f, v, 0.04f, 0.03f, 0.3f, 0xC9A15A, DrawList.GOLD);
                int b = Math.abs(u) < Concert.MID_HALF ? 1 : u > 0f ? 2 : 0;
                lo[b] = Math.min(lo[b], u - 0.45f);
                hi[b] = Math.max(hi[b], u + 0.45f);
            }
            for (int b = 0; b < 3; b++) if (hi[b] > lo[b]) in.solid(lo[b], v - 0.45f, hi[b], v + 0.45f, f + 1.1f);
        }
        // her chair, in the back row at its left end, in her way as the rows are
        Concert.drawChair(d, in);
        in.solid(Concert.CHAIR_U - Concert.CHAIR_HALF, Concert.CHAIR_V - Concert.CHAIR_DEEP, Concert.CHAIR_U + Concert.CHAIR_HALF,
                Concert.CHAIR_V + Concert.CHAIR_DEEP, f + 1.2f);
        // the stage: boards a step high the width of the hall, its edge gilt; the curtains behind
        float st = Concert.STAGE, sy = f + Concert.STAGE_HIGH;
        in.box(d, 0f, (f + sy) * 0.5f, (st + deep - w) * 0.5f, half, (sy - f) * 0.5f, (deep - w - st) * 0.5f, 0x8A5A34, DrawList.WOOD);
        in.box(d, 0f, sy - 0.04f, st + 0.03f, half, 0.04f, 0.04f, 0xC9A15A, DrawList.GOLD);
        in.solid(-half, st, half, deep, sy);
        for (float u = -half + 0.6f; u < half; u += 1.2f)
            in.box(d, u, (sy + top) * 0.5f, deep - w - 0.25f, 0.55f, (top - sy) * 0.5f, 0.12f, (Math.round(u) & 1) == 0 ? 0x8E1E24 : 0x7A1A20, DrawList.GRAIN);
        // the grand piano: its body, black, the curve of its tail, its legs; the keys; the lid up; the bench
        float pu = Concert.PIANO_U, pv = Concert.PIANO_V, py = sy + 0.95f;
        in.box(d, pu, py, pv, 0.75f, 0.17f, 1.1f, 0x111114, DrawList.POLISHED);
        in.box(d, pu + 0.25f, py, pv + 1.25f, 0.5f, 0.17f, 0.3f, 0x111114, DrawList.POLISHED);
        for (int k = 0; k < 3; k++)
            in.box(d, pu + (k == 2 ? 0.3f : (k == 0 ? -0.6f : 0.6f)), (sy + py) * 0.5f, pv + (k == 2 ? 1.2f : -0.9f), 0.05f, (py - sy) * 0.5f, 0.05f, 0x111114, DrawList.POLISHED);
        in.box(d, pu, py + 0.02f, pv - 1.17f, 0.72f, 0.05f, 0.1f, 0xF2EEE4, DrawList.PLAIN);
        for (float u = -0.66f; u < 0.7f; u += 0.13f)
            in.box(d, pu + u, py + 0.08f, pv - 1.14f, 0.025f, 0.02f, 0.06f, 0x111114, DrawList.PLAIN);
        in.box(d, pu + 0.73f, py + 0.75f, pv + 0.1f, 0.03f, 0.6f, 1.0f, 0x111114, DrawList.POLISHED);
        in.box(d, pu, sy + 0.24f, pv - 1.85f, 0.45f, 0.24f, 0.2f, 0x111114, DrawList.POLISHED);
        return in;
    }

    /**
     * Home's room, behind its front door: warm walls over teal panelling, a floor of boards, a
     * coral rug; a back door of glass on the yard, so the room is a way through from the street
     * to the yard as the house is from outside; against one side wall, wide enough for her, a
     * teal sofa with cream cushions, a lamp on a little table at either end, a picture of the sea
     * over it; a shelf of books on the other side wall; plants in the corners by the back door.
     */
    static Indoors home(float doorX, float doorZ) {
        Indoors in = new Indoors(doorX, doorZ, 0f, -1f, 4.8f, 8.6f, 1, 4f, 2);
        in.backDoor = true;
        in.hasLift = false;
        in.home = true;
        in.frameColor = 0xC9A15A;
        in.frameMat = DrawList.GOLD;
        int wall = 0xF1E2C8, low = 0x2F8A86;
        shell(in, 0xD9B48A, false, wall, low, DrawList.PAINT);
        DrawList d = in.still();
        float f = Indoors.FLOOR, w = Indoors.WALL, half = in.half, deep = in.deep;
        // boards over the tiles, and the rug
        in.box(d, 0f, f + 0.004f, deep * 0.5f, half, 0.004f, deep * 0.5f - w, 0xA8754A, DrawList.WOOD);
        in.box(d, 0f, f + 0.01f, deep * 0.5f, 2.4f, 0.006f, 1.9f, 0xE0644A, DrawList.GRAIN);
        in.box(d, 0f, f + 0.012f, deep * 0.5f, 2.0f, 0.006f, 1.5f, 0xF1C6A0, DrawList.GRAIN);
        lamps(in, 2);
        // the sofa, against the side wall across (+u), facing the shelf of books: its base, the
        // seat cushions, the back, the arms
        float sv = deep * 0.5f, su = half - 0.62f, sh = Indoors.SOFA_HALF;
        in.sofaU = su;
        in.sofaV = sv;
        in.sofaFaceU = -1f;
        in.sofaFaceV = 0f;
        in.sofaEyeU = -(half - 1.3f);
        in.sofaEyeV = sv - 2.0f;
        int teal = 0x2F8A86, cream = 0xF1E8D4;
        in.box(d, su, f + 0.22f, sv, 0.55f, 0.22f, sh, teal, DrawList.GRAIN);
        for (int k = -1; k <= 1; k++)
            in.box(d, su - 0.05f, f + 0.5f, sv + k * sh * 0.66f, 0.48f, 0.07f, sh * 0.32f, cream, DrawList.GRAIN);
        in.box(d, half - 0.14f, f + 0.85f, sv, 0.14f, 0.42f, sh, teal, DrawList.GRAIN);
        for (int k = -1; k <= 1; k += 2)
            in.box(d, su, f + 0.4f, sv + k * (sh + 0.14f), 0.6f, 0.4f, 0.14f, Street.scale(teal, 0.85f), DrawList.GRAIN);
        in.box(d, half - 0.34f, f + 0.82f, sv - sh * 0.6f, 0.07f, 0.22f, 0.28f, 0xE0644A, DrawList.GRAIN);
        in.box(d, half - 0.34f, f + 0.8f, sv + sh * 0.55f, 0.07f, 0.2f, 0.26f, 0xF6B54A, DrawList.GRAIN);
        in.solid(half - 1.2f, sv - sh - 0.3f, half, sv + sh + 0.3f, f + 0.62f);
        // the little tables and their lamps
        for (int k = -1; k <= 1; k += 2) {
            float tv = sv + k * (sh + 0.75f);
            in.box(d, su, f + 0.3f, tv, 0.3f, 0.3f, 0.3f, 0x7A5236, DrawList.WOOD);
            in.box(d, su, f + 0.72f, tv, 0.04f, 0.12f, 0.04f, 0xC9A15A, DrawList.GOLD);
            in.box(d, su, f + 0.98f, tv, 0.2f, 0.14f, 0.2f, 0xFFE2B0, DrawList.GLOW);
            in.solid(su - 0.32f, tv - 0.32f, su + 0.32f, tv + 0.32f, f + 0.62f);
        }
        // over the sofa, the sea in a frame: sky, water, a sail
        float pu = half - 0.03f, py = f + 2.45f;
        in.box(d, pu, py, sv, 0.02f, 0.6f, 1.1f, 0x4A3220, DrawList.WOOD);
        in.box(d, pu - 0.02f, py + 0.18f, sv, 0.01f, 0.34f, 1.0f, 0xF2C79A, DrawList.PLAIN);
        in.box(d, pu - 0.02f, py - 0.3f, sv, 0.01f, 0.2f, 1.0f, 0x3F7F94, DrawList.PLAIN);
        in.box(d, pu - 0.03f, py - 0.05f, sv - 0.3f, 0.01f, 0.24f, 0.1f, 0xFAF3E6, DrawList.PLAIN);
        // a shelf of books along the other side wall
        float bu = -(half - 0.22f), bv = deep * 0.5f;
        in.box(d, bu, f + 1.1f, bv, 0.2f, 1.1f, 1.1f, 0x6A4A33, DrawList.WOOD);
        int[] spines = {0xE0644A, 0x2F8A86, 0xF6B54A, 0xF1E8D4, 0x3A4660, 0x8A2E2A};
        for (int r = 0; r < 3; r++)
            for (int k = 0; k < 9; k++)
                in.box(d, bu + 0.05f, f + 0.42f + r * 0.62f, bv - 0.85f + k * 0.21f, 0.15f, 0.2f + (k * 7 + r * 3) % 4 * 0.02f, 0.08f,
                        spines[(k + r * 2) % spines.length], DrawList.PLAIN);
        in.solid(-half, bv - 1.15f, -(half - 0.45f), bv + 1.15f, f + 4f);
        Random rnd = new Random(0x40AEL);
        // plants in the two corners by the back door
        piece(in, rnd, PLANT, -1, deep - w - 0.6f, low);
        piece(in, rnd, PLANT, 1, deep - w - 0.6f, low);
        in.done();
        return in;
    }

    /**
     * The hall no one knows of, behind the door of home's balcony on the fifth storey: dark green
     * walls railed in gold, a dark floor, a red runner to the end wall; on that wall, on a dark
     * board under lamps, the golden mallet; along each side, shelves of golden things won (little
     * gold figures on stands, cups), and over them gold discs in frames.
     */
    // the lobby: half across, deep, high; where its counter stands (along, half across); where
    // she stands at it to be given the box
    static final float LOBBY_HALF = 11f, LOBBY_DEEP = 22f, LOBBY_HIGH = 7f, COUNTER_V = 15f, COUNTER_HALF = 4.2f, AT_COUNTER = 1.3f;

    /**
     * The laboratory's lobby, behind its door at the stem's foot: a floor of pale stone, a
     * turquoise runner from the door to the long white counter across the hall, a band of light
     * along the counter's front; behind it, on the end wall, the name lit large and a wall of dark
     * glass with the town's streets in light on it; round pillars, benches along the side walls, a
     * glass case with the navigator in it, plants. Its people (brums at work, waiting, coming and
     * going, the one at the counter) are drawn as it goes on (see LabTop).
     */
    static Indoors lobby(float doorX, float doorZ) {
        Indoors in = new Indoors(doorX, doorZ, 0f, -1f, LOBBY_HALF, LOBBY_DEEP, 1, 4f, 1);
        in.backDoor = false;
        in.hasLift = false;
        in.lobby = true;
        in.high = LOBBY_HIGH;
        in.frameColor = 0x7FB8B0;
        in.frameMat = DrawList.POLISHED;
        int stone = 0xDCD8CE, wall = 0xE9E6DE, low = 0x2F6E6A, teal = 0x2E9C9A, neon = 0x7FF6E8, white = 0xF4F2EC;
        shell(in, stone, true, wall, low, DrawList.PAINT);
        DrawList d = in.still();
        float f = Indoors.FLOOR, w = Indoors.WALL, half = in.half, deep = in.deep, top = f + in.high;
        // the runner from the door to the counter
        in.box(d, 0f, f + 0.006f, (COUNTER_V - 1f) * 0.5f + 0.4f, 1.3f, 0.006f, (COUNTER_V - 1f) * 0.5f - 0.4f, teal, DrawList.GRAIN);
        // round pillars, two rows
        for (int e = -1; e <= 1; e += 2)
            for (float v = 6f; v < 15f; v += 8f) {
                in.box(d, e * 6.5f, f + in.high * 0.5f, v, 0.5f, in.high * 0.5f, 0.5f, white, DrawList.STONE);
                in.box(d, e * 6.5f, f + in.high * 0.5f, v, 0.35f, in.high * 0.5f, 0.6f, white, DrawList.STONE);
                in.box(d, e * 6.5f, f + in.high * 0.5f, v, 0.6f, in.high * 0.5f, 0.35f, white, DrawList.STONE);
                in.solid(e * 6.5f - 0.6f, v - 0.6f, e * 6.5f + 0.6f, v + 0.6f, f + in.high);
            }
        // the counter across the hall: white, its top pale, a band of light down its front
        float cv = COUNTER_V, ch = 1.1f;
        in.box(d, 0f, f + ch * 0.5f, cv, COUNTER_HALF, ch * 0.5f, 0.45f, white, DrawList.PLAIN);
        in.box(d, 0f, f + ch + 0.03f, cv, COUNTER_HALF + 0.1f, 0.03f, 0.55f, 0xC9C3B6, DrawList.POLISHED);
        in.box(d, 0f, f + 0.55f, cv - 0.47f, COUNTER_HALF - 0.2f, 0.06f, 0.02f, neon, DrawList.GLOW);
        in.solid(-COUNTER_HALF, cv - 0.5f, COUNTER_HALF, cv + 0.5f, f + ch + 0.1f);
        // a screen on the counter, toward the clerk
        in.box(d, 2.2f, f + ch + 0.3f, cv + 0.15f, 0.45f, 0.28f, 0.03f, 0x1D2730, DrawList.GLASS);
        // the end wall: the name lit, large; under it a wall of dark glass, the town's streets lit on it
        float ev = deep - w - 0.05f;
        in.box(d, 0f, f + 2.6f, ev, 7f, 2.1f, 0.04f, 0x16232A, DrawList.GLASS);
        for (int k = -6; k <= 6; k++) in.box(d, k * 1.05f, f + 2.6f, ev - 0.03f, 0.025f, 2f, 0.01f, 0x3F8FB0, DrawList.GLOW);
        for (int k = -3; k <= 3; k++) in.box(d, 0f, f + 2.6f + k * 0.62f, ev - 0.03f, 6.9f, 0.025f, 0.01f, 0x3F8FB0, DrawList.GLOW);
        in.box(d, -1.2f, f + 2.3f, ev - 0.04f, 3.2f, 0.05f, 0.01f, 0x4FD3F2, DrawList.GLOW);
        in.box(d, 2f, f + 3.2f, ev - 0.04f, 0.05f, 0.95f, 0.01f, 0x4FD3F2, DrawList.GLOW);
        String word = "LAB INC.";
        float dot = 0.16f, across = word.length() * 6f * dot - dot;
        for (int k = 0; k < word.length(); k++) {
            int g = Mountain.glyph(word.charAt(k));
            for (int row = 0; row < 7; row++)
                for (int col = 0; col < 5; col++)
                    if (Mountain.lit(g, row, col))
                        in.box(d, across * 0.5f - (k * 6 + col) * dot - dot * 0.5f, top - 1.2f - row * dot, ev - 0.03f, dot * 0.42f, dot * 0.42f, 0.03f, neon, DrawList.GLOW);
        }
        // benches along the side walls
        for (int e = -1; e <= 1; e += 2)
            for (float v = 4f; v < 11f; v += 5f) {
                float bu = e * (half - 0.6f);
                in.box(d, bu, f + 0.45f, v, 0.4f, 0.06f, 1.6f, 0x7A5236, DrawList.WOOD);
                in.box(d, bu, f + 0.2f, v, 0.32f, 0.2f, 1.4f, 0x4A4E54, DrawList.POLISHED);
                in.solid(bu - 0.45f, v - 1.65f, bu + 0.45f, v + 1.65f, f + 0.5f);
            }
        // the glass case with the navigator in it, by the left of the counter
        float gu = -8f, gv = 17f;
        in.box(d, gu, f + 0.5f, gv, 0.7f, 0.5f, 0.7f, white, DrawList.PLAIN);
        in.box(d, gu, f + 1.45f, gv, 0.66f, 0.45f, 0.66f, 0x9FD8E0, DrawList.GLASS);
        in.box(d, gu, f + 1.2f, gv, 0.22f, 0.14f, 0.03f, 0x23282E, DrawList.PLAIN);
        in.box(d, gu, f + 1.2f, gv - 0.035f, 0.18f, 0.11f, 0.01f, 0x4FD3F2, DrawList.GLOW);
        in.solid(gu - 0.75f, gv - 0.75f, gu + 0.75f, gv + 0.75f, f + 2f);
        // plants by the door and by the end wall
        Random rnd = new Random(0x10BBL);
        piece(in, rnd, PLANT, -1, 1.4f, low);
        piece(in, rnd, PLANT, 1, 1.4f, low);
        piece(in, rnd, PLANT, -1, deep - w - 0.8f, low);
        piece(in, rnd, PLANT, 1, deep - w - 0.8f, low);
        lamps(in, 4);
        in.deskU = 0f;
        in.deskV = cv - 0.5f - AT_COUNTER;
        in.done();
        return in;
    }

    static Indoors secret(float doorX, float doorZ) {
        Indoors in = new Indoors(doorX, doorZ, 0f, -1f, 5.4f, 11f, 1, 4f, 2);
        in.backDoor = false;
        in.hasLift = false;
        in.secret = true;
        in.high = 5f;
        in.frameColor = 0xD9A93A;
        in.frameMat = DrawList.GOLD;
        int wall = 0x1F4A45, low = 0x163632, gold = 0xE2B54A, deep_ = 0xC8962E, dark = 0x1B1F22;
        shell(in, 0x3A2A20, false, wall, low, DrawList.PAINT);
        DrawList d = in.still();
        float f = Indoors.FLOOR, w = Indoors.WALL, half = in.half, deep = in.deep;
        // the floor, the runner up the middle, a gold rail round the walls at hand's height
        in.box(d, 0f, f + 0.004f, deep * 0.5f, half, 0.004f, deep * 0.5f - w, 0x3B2416, DrawList.WOOD);
        in.box(d, 0f, f + 0.01f, deep * 0.5f + 0.4f, 1.1f, 0.006f, deep * 0.5f - w - 0.6f, 0x8E1F2A, DrawList.GRAIN);
        in.box(d, 0f, f + 0.011f, deep * 0.5f + 0.4f, 0.95f, 0.006f, deep * 0.5f - w - 0.75f, 0xA82A33, DrawList.GRAIN);
        for (int e = -1; e <= 1; e += 2)
            in.box(d, e * (half - 0.03f), f + 1.0f, deep * 0.5f, 0.03f, 0.04f, deep * 0.5f - w, gold, DrawList.GOLD);
        in.box(d, 0f, f + 1.0f, deep - w - 0.03f, half, 0.04f, 0.03f, gold, DrawList.GOLD);
        lamps(in, 3);
        // the end wall: a dark board, the mallet on it, head across, handle down; lamps over it
        float bv = deep - w - 0.05f, by = f + 2.8f;
        in.box(d, 0f, by, bv, 1.9f, 1.5f, 0.05f, dark, DrawList.PAINT);
        in.box(d, 0f, by, bv - 0.02f, 1.98f, 1.58f, 0.04f, deep_, DrawList.GOLD);
        in.box(d, 0f, by, bv - 0.03f, 1.9f, 1.5f, 0.04f, dark, DrawList.PAINT);
        float hv = bv - 0.4f;
        in.box(d, 0f, by + 0.55f, hv, 0.95f, 0.42f, 0.36f, gold, DrawList.GOLD);
        for (int e = -1; e <= 1; e += 2)
            in.box(d, e * 0.62f, by + 0.55f, hv, 0.08f, 0.45f, 0.39f, deep_, DrawList.GOLD);
        in.box(d, 0f, by - 0.55f, hv, 0.11f, 0.75f, 0.11f, gold, DrawList.GOLD);
        in.box(d, 0f, by - 1.32f, hv, 0.14f, 0.06f, 0.14f, deep_, DrawList.GOLD);
        for (int e = -1; e <= 1; e += 2) {
            in.box(d, e * 1.5f, f + in.high - 0.2f, bv - 0.5f, 0.12f, 0.1f, 0.18f, dark, DrawList.PAINT);
            in.box(d, e * 1.5f, f + in.high - 0.32f, bv - 0.5f, 0.09f, 0.03f, 0.13f, 0xFFF0C8, DrawList.GLOW);
        }
        // along each side: shelves of things won, gold discs in frames over them
        for (int e = -1; e <= 1; e += 2) {
            float su = e * (half - 0.32f);
            for (int r = 0; r < 3; r++) {
                float sy = f + 0.55f + r * 0.75f;
                in.box(d, su, sy, deep * 0.5f, 0.3f, 0.025f, deep * 0.5f - w - 1.2f, 0x2A1A12, DrawList.WOOD);
                for (int k = 0; k < 7; k++) {
                    float sv = 1.7f + k * ((deep - 3.4f) / 6f);
                    if ((k + r + (e > 0 ? 1 : 0)) % 2 == 0) figure(in, d, su, sy + 0.025f, sv, gold);
                    else cup(in, d, su, sy + 0.025f, sv, gold, deep_);
                }
            }
            in.solid(e > 0 ? half - 0.65f : -half, w, e > 0 ? half : -(half - 0.65f), deep - w, f + 2.4f);
            for (int k = 0; k < 4; k++) {
                float dv = 1.9f + k * ((deep - 3.8f) / 3f), dy = f + 3.35f, du = e * (half - 0.04f);
                in.box(d, du, dy, dv, 0.03f, 0.48f, 0.48f, 0x2A1A12, DrawList.WOOD);
                in.box(d, du - e * 0.02f, dy, dv, 0.02f, 0.42f, 0.42f, 0xEDE6D6, DrawList.PLAIN);
                in.box(d, du - e * 0.04f, dy + 0.02f, dv, 0.012f, 0.32f, 0.32f, gold, DrawList.ROD);
                in.box(d, du - e * 0.055f, dy + 0.02f, dv, 0.008f, 0.07f, 0.07f, 0x2B2A2C, DrawList.ROD);
                in.box(d, du - e * 0.03f, dy - 0.36f, dv, 0.012f, 0.03f, 0.2f, deep_, DrawList.GOLD);
            }
        }
        globe(in, d, 0f, f, deep * 0.52f, 0.55f, gold, deep_);
        in.done();
        return in;
    }

    // the switch in the secret hall, on the end wall right of the mallet's board: so far across,
    // its middle so high; how far it travels down; so near it she throws it
    static final float LEVER_U = 3.6f, LEVER_Y = 1.6f, LEVER_TRAVEL = 0.6f, LEVER_NEAR = 1.6f;

    /** Whether one at (x, z) in the secret hall is at its switch. */
    static boolean atLever(Indoors in, float x, float z) {
        float u = in.u(x, z), v = in.v(x, z);
        return Math.abs(u - LEVER_U) < 1.1f && v > in.deep - Indoors.WALL - LEVER_NEAR;
    }

    /**
     * The switch: an iron plate on the wall, a slot down it, the handle red, so far down (0 up,
     * 1 down); a lamp over it, amber, red once it is down: the island shut.
     */
    static void lever(DrawList d, Indoors in, float down) {
        float f = Indoors.FLOOR, v = in.deep - Indoors.WALL - 0.03f, y = f + LEVER_Y;
        in.box(d, LEVER_U, y, v, 0.36f, 0.6f, 0.03f, 0x2B2E33, DrawList.POLISHED);
        in.box(d, LEVER_U, y, v - 0.035f, 0.05f, 0.42f, 0.01f, 0x0E0F11, DrawList.PAINT);
        float hy = y + LEVER_TRAVEL * 0.5f - down * LEVER_TRAVEL;
        in.box(d, LEVER_U, hy, v - 0.12f, 0.05f, 0.05f, 0.1f, 0x55585C, DrawList.POLISHED);
        in.box(d, LEVER_U, hy, v - 0.25f, 0.26f, 0.06f, 0.06f, 0xC0392B, DrawList.PAINT);
        in.box(d, LEVER_U, y + 0.8f, v - 0.03f, 0.08f, 0.08f, 0.03f, down >= 1f ? 0xFF3A2A : 0xFFB347, DrawList.GLOW);
    }

    /** The globe's picture, when the game has it: one byte N, then N by N cells of red, green, blue, rows down. */
    static byte[] globe;

    /**
     * A glass globe on a gold stand in the middle of the floor: its picture laid on the sphere's front
     * as columns of little cells running through to the back; a gold ring round it. Nothing without the picture.
     */
    private static void globe(Indoors in, DrawList d, float u, float f, float v, float r, int gold, int deep) {
        byte[] g = globe;
        if (g == null || g.length < 1) return;
        int n = g[0] & 0xFF;
        if (n < 2 || g.length < 1 + n * n * 3) return;
        float stand = 0.95f, cy = f + stand + 0.06f + r, cell = 2f * r / n;
        in.box(d, u, f + 0.04f, v, 0.32f, 0.04f, 0.32f, 0x1B1F22, DrawList.POLISHED);
        in.box(d, u, f + 0.1f, v, 0.24f, 0.02f, 0.24f, deep, DrawList.GOLD);
        in.box(d, u, f + 0.5f, v, 0.06f, 0.4f, 0.06f, gold, DrawList.GOLD);
        in.box(d, u, f + stand, v, 0.16f, 0.03f, 0.16f, gold, DrawList.GOLD);
        in.box(d, u, f + stand + 0.05f, v, 0.1f, 0.03f, 0.1f, deep, DrawList.GOLD);
        for (int j = 0; j < n; j++) {
            float y = r - (j + 0.5f) * cell;
            for (int i = 0; i < n; i++) {
                float x = (i + 0.5f) * cell - r, q = r * r - x * x - y * y;
                if (q <= 0f) continue;
                int o = 1 + (j * n + i) * 3;
                int c = ((g[o] & 0xFF) << 16) | ((g[o + 1] & 0xFF) << 8) | (g[o + 2] & 0xFF);
                float z = (float) Math.sqrt(q);
                in.box(d, u + x, cy + y, v, cell * 0.5f, cell * 0.5f, z, c, DrawList.GLOW);
            }
        }
        // the ring, upright round the sphere as one sees it from the door
        int k = 48;
        float rr = r + 0.05f;
        for (int e = 0; e < k; e++) {
            double a = (e + 0.5) * 2 * Math.PI / k;
            float ex = (float) Math.sin(a) * rr, ey = (float) Math.cos(a) * rr;
            if (ey < -r * 0.9f) continue;
            in.box(d, u + ex, cy + ey, v, 0.025f + Math.abs(ey) / rr * 0.02f, 0.025f + Math.abs(ex) / rr * 0.02f, 0.025f, gold, DrawList.GOLD);
        }
        in.solid(u - 0.4f, v - 0.4f, u + 0.4f, v + 0.4f, cy + r);
    }

    /** A little gold figure on a black stand, won for something. */
    private static void figure(Indoors in, DrawList d, float u, float y, float v, int gold) {
        in.box(d, u, y + 0.06f, v, 0.07f, 0.06f, 0.07f, 0x1B1F22, DrawList.POLISHED);
        in.box(d, u, y + 0.13f, v, 0.045f, 0.012f, 0.045f, gold, DrawList.GOLD);
        in.box(d, u, y + 0.26f, v, 0.028f, 0.12f, 0.022f, gold, DrawList.GOLD);
        in.box(d, u, y + 0.41f, v, 0.025f, 0.03f, 0.025f, gold, DrawList.GOLD);
    }

    /** A cup on a stand, its two handles. */
    private static void cup(Indoors in, DrawList d, float u, float y, float v, int gold, int deep) {
        in.box(d, u, y + 0.04f, v, 0.09f, 0.04f, 0.09f, 0x2A1A12, DrawList.WOOD);
        in.box(d, u, y + 0.12f, v, 0.05f, 0.012f, 0.05f, deep, DrawList.GOLD);
        in.box(d, u, y + 0.18f, v, 0.016f, 0.05f, 0.016f, gold, DrawList.GOLD);
        in.box(d, u, y + 0.32f, v, 0.085f, 0.09f, 0.085f, gold, DrawList.GOLD);
        in.box(d, u, y + 0.42f, v, 0.095f, 0.012f, 0.095f, deep, DrawList.GOLD);
        in.box(d, u, y + 0.33f, v - 0.12f, 0.015f, 0.06f, 0.025f, gold, DrawList.GOLD);
        in.box(d, u, y + 0.33f, v + 0.12f, 0.015f, 0.06f, 0.025f, gold, DrawList.GOLD);
    }

    /** Floor, ceiling and walls, the glass doors' openings front and back and the lift's in one side. */
    private static void shell(Indoors in, int tile, boolean diagonal, int wall, int low, float lowMat) {
        DrawList d = in.still();
        float f = Indoors.FLOOR, h = in.high, w = Indoors.WALL, half = in.half, deep = in.deep;
        float tall = f + h + 1f, mid = f + h * 0.5f, dh = Indoors.DOOR_HALF, lh = Indoors.LIFT_HALF;
        int s = in.side;
        in.box(d, 0f, f - 0.15f, deep * 0.5f, half + w, 0.15f, deep * 0.5f, tile, DrawList.material(DrawList.TILES, diagonal ? 200 : 40));
        in.box(d, 0f, f + h + 0.15f, deep * 0.5f, half + w, 0.15f, deep * 0.5f, Street.scale(wall, 1.04f), DrawList.PLAIN);
        // the wall across from the lift
        in.box(d, -s * (half + w * 0.5f), mid, deep * 0.5f, w * 0.5f, h * 0.5f, deep * 0.5f, wall, DrawList.PLAIN);
        in.solid(Math.min(-s * half, -s * (half + w)), 0f, Math.max(-s * half, -s * (half + w)), deep, tall);
        // the lift's wall, in two, and over the lift's doors (whole, without a lift)
        float u0 = s * half, u1 = s * (half + w), uc = s * (half + w * 0.5f);
        float a = in.liftV - lh, b = in.liftV + lh;
        if (!in.hasLift) {
            in.box(d, uc, mid, deep * 0.5f, w * 0.5f, h * 0.5f, deep * 0.5f, wall, DrawList.PLAIN);
            in.solid(Math.min(u0, u1), 0f, Math.max(u0, u1), deep, tall);
            a = b = deep * 0.5f;
        } else {
            in.box(d, uc, mid, a * 0.5f, w * 0.5f, h * 0.5f, a * 0.5f, wall, DrawList.PLAIN);
            in.box(d, uc, mid, (b + deep) * 0.5f, w * 0.5f, h * 0.5f, (deep - b) * 0.5f, wall, DrawList.PLAIN);
            float over = f + Indoors.LIFT_HIGH;
            in.box(d, uc, (over + f + h) * 0.5f, in.liftV, w * 0.5f, (f + h - over) * 0.5f, lh, wall, DrawList.PLAIN);
            in.solid(Math.min(u0, u1), 0f, Math.max(u0, u1), a, tall);
            in.solid(Math.min(u0, u1), b, Math.max(u0, u1), deep, tall);
        }
        // the front and the back, each with its doorway in the middle (the back walled up when it has none)
        if (!in.backDoor) {
            in.box(d, 0f, mid, deep - w * 0.5f, half + w, h * 0.5f, w * 0.5f, wall, DrawList.PLAIN);
            in.solid(-(half + w), deep - w, half + w, deep, tall);
        }
        for (int k = 0; k < (in.backDoor ? 2 : 1); k++) {
            float v = k == 0 ? w * 0.5f : deep - w * 0.5f;
            float hw = (half + w - dh) * 0.5f, cu = dh + hw;
            for (int e = -1; e <= 1; e += 2) {
                in.box(d, e * cu, mid, v, hw, h * 0.5f, w * 0.5f, wall, DrawList.PLAIN);
                in.solid(Math.min(e * dh, e * (half + w)), v - w * 0.5f, Math.max(e * dh, e * (half + w)), v + w * 0.5f, tall);
            }
            float dtop = f + Indoors.DOOR_HIGH;
            in.box(d, 0f, (dtop + f + h) * 0.5f, v, dh, (f + h - dtop) * 0.5f, w * 0.5f, wall, DrawList.PLAIN);
            // the doorway's stone frame, inside
            float iv = k == 0 ? w + 0.03f : deep - w - 0.03f;
            for (int e = -1; e <= 1; e += 2)
                in.box(d, e * (dh + 0.07f), f + Indoors.DOOR_HIGH * 0.5f, iv, 0.07f, Indoors.DOOR_HIGH * 0.5f + 0.07f, 0.03f, 0xD8CDB8, DrawList.STONE);
            in.box(d, 0f, dtop + 0.07f, iv, dh + 0.14f, 0.07f, 0.03f, 0xD8CDB8, DrawList.STONE);
            // outside: the light of the street, or of the yard, soft (not a white blaze in the doorway)
            float ov = k == 0 ? -0.35f : deep + 0.35f;
            in.box(d, 0f, f + Indoors.DOOR_HIGH * 0.5f, ov, dh + 0.4f, Indoors.DOOR_HIGH * 0.5f + 0.3f, 0.05f,
                    k == 0 ? 0xA8977C : 0x8E9FA6, DrawList.GLOW);
        }
        // the panelling below a rail, round the hall, broken by the doorways
        float rail = 1.15f, ph = rail * 0.5f;
        panel(in, -s * (half - 0.02f), w, deep - w, true, f, ph, low, lowMat);
        panel(in, s * (half - 0.02f), w, a, true, f, ph, low, lowMat);
        panel(in, s * (half - 0.02f), b, deep - w, true, f, ph, low, lowMat);
        for (int e = -1; e <= 1; e += 2) {
            panel(in, w + 0.02f, e * dh, e * half, false, f, ph, low, lowMat);
            if (in.backDoor) panel(in, deep - w - 0.02f, e * dh, e * half, false, f, ph, low, lowMat);
        }
        if (!in.backDoor) panel(in, deep - w - 0.02f, -half, half, false, f, ph, low, lowMat);
        if (!in.hasLift) {
            in.box(d, 0f, f + 0.006f, w + 0.75f, 0.85f, 0.006f, 0.45f, 0x4A3628, DrawList.GRAIN);
            return;
        }
        // the lift's doors in a frame of brass (or steel)
        float fu = s * (half - 0.03f);
        for (int e = -1; e <= 1; e += 2)
            in.box(d, fu, f + Indoors.LIFT_HIGH * 0.5f, in.liftV + e * (lh + 0.07f), 0.03f, Indoors.LIFT_HIGH * 0.5f + 0.07f, 0.07f, in.frameColor, in.frameMat);
        in.box(d, fu, f + Indoors.LIFT_HIGH + 0.07f, in.liftV, 0.03f, 0.07f, lh + 0.14f, in.frameColor, in.frameMat);
        // the button's plate beside them, and the plate the floor lamps sit on
        in.box(d, s * (half - 0.015f), f + 1.3f, in.liftV + lh + 0.42f, 0.015f, 0.14f, 0.09f, in.frameColor, in.frameMat);
        in.box(d, s * (half - 0.015f), f + Indoors.LIFT_HIGH + 0.42f, in.liftV, 0.015f, 0.1f, in.floors * 0.1f + 0.12f, 0x2A2420, DrawList.PLAIN);
        // a mat inside the front door
        in.box(d, 0f, f + 0.006f, w + 0.75f, 0.85f, 0.006f, 0.45f, 0x4A3628, DrawList.GRAIN);
    }

    /** Panelling up to the rail on a wall at a (across if alongV, else in), from b0 to b1 along it. */
    private static void panel(Indoors in, float at, float b0, float b1, boolean alongV, float f, float ph, int low, float mat) {
        float lo = Math.min(b0, b1), hi = Math.max(b0, b1), mid = (lo + hi) * 0.5f, len = (hi - lo) * 0.5f;
        if (len < 0.05f) return;
        DrawList d = in.still();
        if (alongV) {
            in.box(d, at, f + ph, mid, 0.02f, ph, len, low, mat);
            in.box(d, at, f + ph * 2f + 0.02f, mid, 0.04f, 0.025f, len, Street.scale(low, 0.8f), DrawList.WOOD);
        } else {
            in.box(d, mid, f + ph, at, len, ph, 0.02f, low, mat);
            in.box(d, mid, f + ph * 2f + 0.02f, at, len, 0.025f, 0.04f, Street.scale(low, 0.8f), DrawList.WOOD);
        }
    }

    /** The cabin past the lift's doors: panelled walls, a mirror and a rail at the back, a lamp, the button plate. */
    private static void cabin(Indoors in) {
        DrawList d = in.still();
        float f = Indoors.FLOOR, w = Indoors.WALL, ch = Indoors.CAB_HIGH, cd = Indoors.CAB_DEEP, chf = Indoors.CAB_HALF;
        int s = in.side;
        float door = in.half + w, mid = door + cd * 0.5f, backU = door + cd;
        float tall = f + in.high + 1f;
        in.box(d, s * mid, f - 0.15f, in.liftV, cd * 0.5f + w, 0.15f, chf + w, 0x4A3A2E, DrawList.WOOD);
        in.box(d, s * mid, f + ch + 0.12f, in.liftV, cd * 0.5f + w, 0.12f, chf + w, 0x3A302A, DrawList.PLAIN);
        in.box(d, s * (backU + w * 0.5f), f + ch * 0.5f, in.liftV, w * 0.5f, ch * 0.5f, chf + w, in.cabColor, DrawList.WOOD);
        for (int e = -1; e <= 1; e += 2)
            in.box(d, s * (mid - 0.2f), f + ch * 0.5f, in.liftV + e * (chf + w * 0.5f), cd * 0.5f + 0.2f + w, ch * 0.5f, w * 0.5f, in.cabColor, DrawList.WOOD);
        // what she cannot go through: the back and the two sides
        in.solid(Math.min(s * backU, s * (backU + w)), in.liftV - chf - w, Math.max(s * backU, s * (backU + w)), in.liftV + chf + w, tall);
        for (int e = -1; e <= 1; e += 2) {
            float v0 = in.liftV + e * chf, v1 = in.liftV + e * (chf + w);
            in.solid(Math.min(s * in.half, s * backU), Math.min(v0, v1), Math.max(s * in.half, s * backU), Math.max(v0, v1), tall);
        }
        // the mirror and the rail on the back wall, the lamp overhead
        in.box(d, s * (backU - 0.02f), f + 1.65f, in.liftV, 0.02f, 0.75f, chf * 0.7f, 0x9FB4BC, DrawList.GLASS);
        in.box(d, s * (backU - 0.09f), f + 0.95f, in.liftV, 0.03f, 0.03f, chf * 0.85f, in.frameColor, in.frameMat);
        in.box(d, s * mid, f + ch - 0.03f, in.liftV, cd * 0.3f, 0.03f, chf * 0.55f, 0xFFF1D6, DrawList.GLOW);
        // the plate for the buttons, on the side wall by the doors
        in.box(d, s * (door + 0.35f), f + 1.05f + in.floors * 0.055f, in.liftV + chf - 0.02f, 0.11f, in.floors * 0.055f + 0.12f, 0.015f, 0x2A2622, DrawList.PLAIN);
    }

    /** So many lamps down the middle of the ceiling. */
    private static void lamps(Indoors in, int n) {
        DrawList d = in.still();
        float top = Indoors.FLOOR + in.high;
        for (int k = 0; k < n; k++) {
            float v = in.deep * (k + 1f) / (n + 1f);
            in.box(d, 0f, top - 0.05f, v, 0.36f, 0.03f, 0.36f, in.frameColor, in.frameMat);
            in.box(d, 0f, top - 0.1f, v, 0.3f, 0.03f, 0.3f, 0xFFE9BE, DrawList.GLOW);
        }
    }

    /**
     * What stands along the walls: laid along the wall across from the lift, and beside the lift
     * clear of its doors, a little apart, none in the way of the doorways.
     */
    private static void furnish(Indoors in, Random rnd, int low) {
        int[] kinds = {MAILBOXES, BENCH, PLANT, RADIATOR, PICTURE, BOARD};
        for (int k = kinds.length - 1; k > 0; k--) {
            int j = rnd.nextInt(k + 1), t = kinds[k];
            kinds[k] = kinds[j];
            kinds[j] = t;
        }
        // across from the lift: as many as fit
        float v = 1.9f + rnd.nextFloat() * 0.6f, end = in.deep - 1.9f;
        int s = -in.side;
        for (int kind : kinds) {
            float len = length(kind);
            if (v + len > end) break;
            piece(in, rnd, kind, s, v + len * 0.5f, low);
            v += len + 0.5f + rnd.nextFloat() * 0.8f;
        }
        // beside the lift: a plant on one side of it, a picture on the other, if there is room
        float a = in.liftV - Indoors.LIFT_HALF - 1.1f, b = in.liftV + Indoors.LIFT_HALF + 1.2f;
        if (a > 2.2f) piece(in, rnd, PLANT, in.side, a - 0.4f, low);
        if (b < in.deep - 2.2f) piece(in, rnd, rnd.nextBoolean() ? PICTURE : RADIATOR, in.side, b + 0.6f, low);
    }

    private static float length(int kind) {
        switch (kind) {
            case MAILBOXES: return 1.75f;
            case BENCH: return 1.6f;
            case PLANT: return 0.8f;
            case RADIATOR: return 1.1f;
            case PICTURE: return 1f;
            default: return 1.2f;
        }
    }

    /** One thing against the wall on side s (across), its middle so far in. */
    private static void piece(Indoors in, Random rnd, int kind, int s, float v, int low) {
        DrawList d = in.still();
        float f = Indoors.FLOOR, wallU = s * in.half;
        switch (kind) {
            case MAILBOXES: {
                // a bank of letter boxes, three rows, a slot and a name card each
                int cols = 5;
                float cw = 0.34f, rh = 0.24f, u = wallU - s * 0.08f;
                in.box(d, u - s * 0.0f, f + 1.62f, v, 0.08f, rh * 1.5f + 0.04f, cols * cw * 0.5f + 0.04f, 0x5A4032, DrawList.WOOD);
                for (int r = 0; r < 3; r++)
                    for (int c = 0; c < cols; c++) {
                        float bv = v + (c - (cols - 1) * 0.5f) * cw, by = f + 1.62f + (r - 1) * rh;
                        in.box(d, u - s * 0.07f, by, bv, 0.02f, rh * 0.5f - 0.02f, cw * 0.5f - 0.02f, in.frameColor, in.frameMat);
                        in.box(d, u - s * 0.09f, by + 0.05f, bv, 0.01f, 0.012f, cw * 0.3f, 0x1E1A16, DrawList.PLAIN);
                        in.box(d, u - s * 0.09f, by - 0.05f, bv, 0.01f, 0.025f, cw * 0.22f, 0xF2EBDA, DrawList.PLAIN);
                    }
                in.solid(Math.min(wallU, wallU - s * 0.2f), v - 0.9f, Math.max(wallU, wallU - s * 0.2f), v + 0.9f, f + 4f);
                break;
            }
            case BENCH: {
                float u = wallU - s * 0.28f;
                in.box(d, u, f + 0.48f, v, 0.24f, 0.04f, 0.78f, 0x8A5E3C, DrawList.WOOD);
                for (int e = -1; e <= 1; e += 2)
                    in.box(d, u, f + 0.22f, v + e * 0.66f, 0.2f, 0.22f, 0.04f, 0x3A2E26, DrawList.PLAIN);
                in.box(d, wallU - s * 0.05f, f + 0.85f, v, 0.03f, 0.2f, 0.78f, 0x8A5E3C, DrawList.WOOD);
                in.solid(Math.min(wallU, wallU - s * 0.52f), v - 0.8f, Math.max(wallU, wallU - s * 0.52f), v + 0.8f, f + 0.52f);
                break;
            }
            case PLANT: {
                // a clay pot, and a round bush in it
                float u = wallU - s * 0.42f;
                in.box(d, u, f + 0.3f, v, 0.28f, 0.3f, 0.28f, 0xB0613E, DrawList.STONE);
                in.box(d, u, f + 0.62f, v, 0.31f, 0.03f, 0.31f, 0x9A5234, DrawList.STONE);
                in.ball(d, u, f + 1.12f, v, 0.5f + rnd.nextFloat() * 0.12f, rnd.nextBoolean() ? 0x4F7A3A : 0x3E6B48, DrawList.MATTE);
                in.solid(Math.min(wallU, u - s * 0.4f), v - 0.4f, Math.max(wallU, u - s * 0.4f), v + 0.4f, f + 4f);
                break;
            }
            case RADIATOR: {
                float u = wallU - s * 0.08f;
                for (int k = 0; k < 9; k++)
                    in.box(d, u, f + 0.5f, v + (k - 4) * 0.11f, 0.05f, 0.34f, 0.04f, 0xEDE6D6, DrawList.PAINT);
                in.solid(Math.min(wallU, wallU - s * 0.16f), v - 0.5f, Math.max(wallU, wallU - s * 0.16f), v + 0.5f, f + 0.86f);
                break;
            }
            case PICTURE: {
                // a framed picture over the rail: a sea, a sky and a sail, in a few blocks
                float u = wallU - s * 0.03f, y = f + 2.15f;
                in.box(d, u, y, v, 0.02f, 0.42f, 0.5f, 0x4A3220, DrawList.WOOD);
                in.box(d, u - s * 0.02f, y + 0.12f, v, 0.01f, 0.24f, 0.42f, 0xF2C79A, DrawList.PLAIN);
                in.box(d, u - s * 0.02f, y - 0.2f, v, 0.01f, 0.14f, 0.42f, 0x3F7F94, DrawList.PLAIN);
                in.box(d, u - s * 0.025f, y - 0.03f, v + 0.1f, 0.01f, 0.16f, 0.07f, 0xFAF3E6, DrawList.PLAIN);
                break;
            }
            default: {
                // a notice board of cork with papers pinned to it
                float u = wallU - s * 0.03f, y = f + 1.75f;
                in.box(d, u, y, v, 0.02f, 0.38f, 0.55f, 0xB98B5A, DrawList.GRAIN);
                for (int k = 0; k < 4; k++)
                    in.box(d, u - s * 0.02f, y + (rnd.nextFloat() - 0.5f) * 0.36f, v + (k - 1.5f) * 0.26f, 0.008f,
                            0.1f + rnd.nextFloat() * 0.05f, 0.08f, k % 2 == 0 ? 0xF4EEDF : 0xF6E6A8, DrawList.PLAIN);
                break;
            }
        }
    }
}
