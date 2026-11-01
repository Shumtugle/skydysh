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
     * Home's room, behind its front door: warm walls over teal panelling, a floor of boards, a
     * coral rug; against the back wall, wide enough for her, a teal sofa with cream cushions, a
     * lamp on a little table at either end; a picture of the sea over it; plants in the corners,
     * a shelf of books on one side wall and a radiator under a picture on the other. No way
     * through: the back is walled up.
     */
    static Indoors home(float doorX, float doorZ) {
        Indoors in = new Indoors(doorX, doorZ, 0f, -1f, 4.8f, 8.6f, 1, 4f, 2);
        in.backDoor = false;
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
        in.box(d, 0f, f + 0.01f, deep * 0.62f, 2.4f, 0.006f, 1.9f, 0xE0644A, DrawList.GRAIN);
        in.box(d, 0f, f + 0.012f, deep * 0.62f, 2.0f, 0.006f, 1.5f, 0xF1C6A0, DrawList.GRAIN);
        lamps(in, 2);
        // the sofa: its base, the seat cushions, the back, the arms
        float sv = deep - w - 0.62f, sh = Indoors.SOFA_HALF;
        in.sofaV = sv;
        int teal = 0x2F8A86, cream = 0xF1E8D4;
        in.box(d, 0f, f + 0.22f, sv, sh, 0.22f, 0.55f, teal, DrawList.GRAIN);
        for (int k = -1; k <= 1; k++)
            in.box(d, k * sh * 0.66f, f + 0.5f, sv - 0.05f, sh * 0.32f, 0.07f, 0.48f, cream, DrawList.GRAIN);
        in.box(d, 0f, f + 0.85f, deep - w - 0.14f, sh, 0.42f, 0.14f, teal, DrawList.GRAIN);
        for (int k = -1; k <= 1; k += 2)
            in.box(d, k * (sh + 0.14f), f + 0.4f, sv, 0.14f, 0.4f, 0.6f, Street.scale(teal, 0.85f), DrawList.GRAIN);
        in.box(d, -sh * 0.6f, f + 0.82f, deep - w - 0.34f, 0.28f, 0.22f, 0.07f, 0xE0644A, DrawList.GRAIN);
        in.box(d, sh * 0.55f, f + 0.8f, deep - w - 0.34f, 0.26f, 0.2f, 0.07f, 0xF6B54A, DrawList.GRAIN);
        in.solid(-sh - 0.3f, deep - w - 1.2f, sh + 0.3f, deep - w, f + 0.62f);
        // the little tables and their lamps
        for (int k = -1; k <= 1; k += 2) {
            float tu = k * (sh + 0.75f);
            in.box(d, tu, f + 0.3f, sv, 0.3f, 0.3f, 0.3f, 0x7A5236, DrawList.WOOD);
            in.box(d, tu, f + 0.72f, sv, 0.04f, 0.12f, 0.04f, 0xC9A15A, DrawList.GOLD);
            in.box(d, tu, f + 0.98f, sv, 0.2f, 0.14f, 0.2f, 0xFFE2B0, DrawList.GLOW);
            in.solid(tu - 0.32f, sv - 0.32f, tu + 0.32f, sv + 0.32f, f + 0.62f);
        }
        // over the sofa, the sea in a frame: sky, water, a sail
        float pv = deep - w - 0.03f, py = f + 2.45f;
        in.box(d, 0f, py, pv, 1.1f, 0.6f, 0.02f, 0x4A3220, DrawList.WOOD);
        in.box(d, 0f, py + 0.18f, pv - 0.02f, 1.0f, 0.34f, 0.01f, 0xF2C79A, DrawList.PLAIN);
        in.box(d, 0f, py - 0.3f, pv - 0.02f, 1.0f, 0.2f, 0.01f, 0x3F7F94, DrawList.PLAIN);
        in.box(d, 0.3f, py - 0.05f, pv - 0.03f, 0.1f, 0.24f, 0.01f, 0xFAF3E6, DrawList.PLAIN);
        // a shelf of books along one side wall
        float bu = -(half - 0.22f), bv = deep * 0.45f;
        in.box(d, bu, f + 1.1f, bv, 0.2f, 1.1f, 1.1f, 0x6A4A33, DrawList.WOOD);
        int[] spines = {0xE0644A, 0x2F8A86, 0xF6B54A, 0xF1E8D4, 0x3A4660, 0x8A2E2A};
        for (int r = 0; r < 3; r++)
            for (int k = 0; k < 9; k++)
                in.box(d, bu + 0.05f, f + 0.42f + r * 0.62f, bv - 0.85f + k * 0.21f, 0.15f, 0.2f + (k * 7 + r * 3) % 4 * 0.02f, 0.08f,
                        spines[(k + r * 2) % spines.length], DrawList.PLAIN);
        in.solid(-half, bv - 1.15f, -(half - 0.45f), bv + 1.15f, f + 4f);
        // the radiator, and a picture over it, on the other side wall
        Random rnd = new Random(0x40AEL);
        piece(in, rnd, RADIATOR, 1, deep * 0.45f, low);
        piece(in, rnd, PICTURE, 1, deep * 0.45f, low);
        // plants in the two corners by the sofa's ends
        piece(in, rnd, PLANT, -1, deep - w - 0.6f, low);
        piece(in, rnd, PLANT, 1, deep - w - 0.6f, low);
        in.done();
        return in;
    }

    /** Floor, ceiling and walls, the glass doors' openings front and back and the lift's in one side. */
    private static void shell(Indoors in, int tile, boolean diagonal, int wall, int low, float lowMat) {
        DrawList d = in.still();
        float f = Indoors.FLOOR, h = Indoors.HALL_HIGH, w = Indoors.WALL, half = in.half, deep = in.deep;
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
        float tall = f + Indoors.HALL_HIGH + 1f;
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
        float top = Indoors.FLOOR + Indoors.HALL_HIGH;
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
