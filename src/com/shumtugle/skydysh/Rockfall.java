package com.shumtugle.skydysh;

import java.util.ArrayList;
import java.util.Random;

/**
 * The rockfall in the pass, and a game made of it. On the way out to the village, short of the
 * pass the eye leaves her: from high and far off it comes down on the pickup as it runs up the
 * road; the pickup stops short, she gets out, and down the slopes on either side of the pass come
 * logs, posts, planks, stones and lumps of clay, built across the road into a figure: a gate, log
 * towers, a wood pile, a shed, a boulder on a table, slabs on end, a pyramid, every fifth a fort;
 * each standing on one piece, its key. From a line so far short of it she has three throws of the
 * mallet to break it: most of its weight knocked off where it lay, or knocked flat. Then the pickup and on, or another heap down the slopes, as
 * many as she likes. Each heap comes of a seed: a new one every time.
 */
final class Rockfall implements Physics.Listener {
    // the phases: not yet; the pickup brought in under the eye; stopped; getting out; the heap
    // coming down; throwing; gone on (the pieces lie where they lie)
    static final int WAIT = 0, ARRIVE = 1, HALT = 2, OUT = 3, FALL = 4, PLAY = 5, LEFT = 6;
    // what a step brought, for the world to do: nothing; she is to get out; she is to stand and
    // throw; the heap is broken
    static final int NONE = 0, GET_OUT = 1, READY = 2, BROKE = 3, MISSED = 4;
    // three throws at a figure; after the last is back and all lies still, it is missed
    static final int THROWS = 3;
    static final float MISS_WAIT = 1.2f, MISS_MOST = 3.5f;
    // a piece struck so hard by the mallet splits in two along its length, or by another piece
    // harder still; never into pieces smaller than so much (half), nor past so many in all
    static final float SPLIT_SPEED = 5.5f, SPLIT_CRASH = 9f, SPLIT_MIN = 0.22f;
    static final int MOST = 170;

    // the heap so far beyond the line she throws from; the pickup stopped so far short of the line
    static final float THROW = 28f, STOP_BACK = 6f;
    // the eye takes the pickup over so far short of where it stops; how fast it is brought in,
    // and how hard it stops
    static final float FROM = 62f, CRUISE = 15f, BRAKE = 17f;
    // how long it stands before she gets out; how long after that the slopes start to come down
    static final float HALT_TIME = 0.9f, SLIDE_AFTER = 0.7f;
    // the pieces coming down: one after another so far apart, each so long in the air
    static final float FALL_EACH = 0.1f, FALL_FLIGHT = 0.95f;
    // the mallet: how fast it leaves her hand, how much heavier than its size, how long it may be
    // out before it is called back, how long it takes coming back
    static final float SPEED = 20f, HEAVY = 4f, OUT_MOST = 1.6f, BACK_TIME = 0.6f;
    // broken: so much of the weight moved so far from where it lay, or nothing left standing over the lowest row
    static final float CLEAR = 0.55f, MOVED = 0.6f;
    // the eye closing in on the heap while she aims and the mallet is out: so much narrower, so quickly
    static final float LENS = 2f, ZOOM_T = 0.3f;
    // a slot of the heap, across: each piece in one, a beam across two
    static final float SLOT = 1.05f;
    // the kinds of piece
    static final int LOG = 0, STUMP = 1, STONE = 2, CLAY = 3, BEAM = 4, PLANK = 5, SLAB = 6, BOULDER = 7, POST = 8;

    /** Where the heap stands; the line she throws from; where she stands on it. */
    final float px, pz, tz, standX;
    int phase = WAIT;
    /** Seconds into the phase. */
    float t;
    /** Heaps broken, throws at this one, throws in all. */
    int broken, throwsHere, throwsAll;
    /** Whether this heap is broken. */
    boolean down;
    /** The heap now, its seed's count. */
    int figure;
    /** Whether she came on foot (no pickup brought in). */
    boolean onFoot;
    /** Whether the three throws are spent on this figure and it stands; how long since the last came back. */
    boolean missed;
    private float missT;
    /** Stars for the figure broken: three at the first throw, two at the second, one at the last. */
    int stars;
    /** The plan of the figure before this one, so that the next is another (and a try again the same). */
    private int planBefore = -1;
    // a blow felt: the world held still so long; the eye shaken so much
    private float stop, shake, clock;
    private final ArrayList<Body> toSplit = new ArrayList<Body>();
    private boolean keyStruck;

    final Physics world = new Physics();
    private final ArrayList<Piece> pieces = new ArrayList<Piece>();
    private final ArrayList<Piece> old = new ArrayList<Piece>();
    private float oldT;
    private final Body mallet, car;
    private Pickup pickup;
    private World.Host host;
    private int seed = 0x5EED;

    /** One piece of the heap: its body, kind, colours; where it came down from, and when. */
    private static final class Piece {
        Body b;
        int kind, color, cap;
        float sx, sy, sz, ax, ay, az, angle, at, shown = 1f;
        boolean landed;
        // given up its raised inertia (struck, split): it topples freely
        boolean free;
    }

    Rockfall() {
        pz = City.PASS_Z;
        px = City.routeX(pz);
        tz = pz - THROW;
        standX = px - 0.6f;
        mallet = new Body(Mallet.HEAD_HALF_X * Toss.SCALE, Mallet.HEAD_HALF_Y * Toss.SCALE, Mallet.HEAD_HALF_Z * Toss.SCALE, Body.HAMMER, 0xE2B270);
        mallet.scaleMass(HEAVY);
        mallet.ghost = true;
        car = new Body(Pickup.HALF_WIDE, Pickup.TOP * 0.5f, Pickup.HALF_LONG, Body.CAR, 0);
        car.ghost = true;
        world.listener = this;
        rebuild();
    }

    /** The physics afresh: the rock faces of the pass, the pickup, the mallet, the pieces. */
    private void rebuild() {
        world.clear();
        for (int e = -1; e <= 1; e += 2) {
            Body face = new Body(4f, 10f, 30f, Body.PLINTH, 0);
            face.setPose(px + e * (City.CLIFF_FOOT + 4f), 10f, pz, 1f, 0f, 0f, 0f);
            world.add(face);
        }
        world.add(car);
        world.add(mallet);
        for (Piece p : pieces) world.add(p.b);
    }

    // ---- the heap ----

    // the figures: a pyramid; a gate with a load on its lintel; log towers; a wood pile; a shed
    // weighed down with stones; a boulder on a table; slabs on end in rows; and every fifth a fort
    static final int PYRAMID = 0, GATE = 1, TOWERS = 2, PILE = 3, SHED = 4, PERCH = 5, SLABS = 6, FORT = 7;
    static final int PLANS = 8;
    /** The figure now. */
    int plan;
    /** How high the figure stood when it was built. */
    private float height0 = 1f;
    /** The top of the wood pile last laid. */
    private float pileTop;
    /** The piece that holds the figure up: struck, the whole of it goes. */
    private Piece key;
    // the slopes bring a figure down in so long, however many pieces it has
    static final float FALL_ALL = 3.2f;

    /** A new figure from the next seed: built where it will stand, each piece put back up a slope to come down. */
    private void build() {
        planBefore = plan;
        figure++;
        Random rnd = new Random(seed * 7919L + figure * 104729L);
        pieces.clear();
        key = null;
        // the figures come round in a shuffled order, every one before any comes again; every fifth a fort
        if (figure % 5 == 0) plan = FORT;
        else {
            int n = figure - 1 - (figure - 1) / 5, round = n / (PLANS - 1);
            int[] deck = new int[PLANS - 1];
            for (int i = 0; i < deck.length; i++) deck[i] = i;
            Random shuffle = new Random(seed * 31L + round * 7907L);
            for (int i = deck.length - 1; i > 0; i--) {
                int j = shuffle.nextInt(i + 1), x = deck[i];
                deck[i] = deck[j];
                deck[j] = x;
            }
            // the first a gate, always
            if (round == 0)
                for (int i = 0; i < deck.length; i++)
                    if (deck[i] == GATE) {
                        deck[i] = deck[0];
                        deck[0] = GATE;
                    }
            plan = deck[n % deck.length];
        }
        switch (plan) {
            case GATE: gate(rnd, 0f, 0f); break;
            case TOWERS: towers(rnd); break;
                case PILE: pile(rnd, 3.8f + rnd.nextFloat() * 0.8f, 5 + rnd.nextInt(3), 0f, true); break;
            case SHED: shed(rnd); break;
            case PERCH: perch(rnd, 0f, 0f); break;
            case SLABS: slabs(rnd); break;
            case FORT: fort(rnd); break;
            default: pyramid(rnd); break;
        }
        height0 = 0.5f;
        for (Piece p : pieces) height0 = Math.max(height0, p.b.py + p.b.hy);
        // down the slopes the lowest first, so that each lands on what is to hold it
        java.util.Collections.sort(pieces, new java.util.Comparator<Piece>() {
            public int compare(Piece a, Piece b) {
                return Float.compare(a.b.py - a.b.hy, b.b.py - b.b.hy);
            }
        });
        float each = Math.min(FALL_EACH, FALL_ALL / Math.max(1, pieces.size()));
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            p.at = i * each + rnd.nextFloat() * 0.04f;
        }
        rebuild();
    }

    /** The heap of before: rows each on the two under it, each row of one kind. */
    private void pyramid(Random rnd) {
        int base = 7 + rnd.nextInt(2);
        float y = 0f;
        for (int row = 0; row < base; row++) {
            int n = base - row;
            int kind = rnd.nextInt(5);
            if (row == 0 && kind == BEAM) kind = STONE;
            float hy = kind == STUMP ? 0.42f : kind == STONE ? 0.38f : kind == CLAY ? 0.33f : 0.32f;
            for (int i = 0; i < n; i++) {
                float x = px + (i - (n - 1) * 0.5f) * SLOT;
                int k = kind == BEAM ? LOG : rnd.nextFloat() < 0.7f ? kind : rnd.nextInt(4);
                if (k == BEAM) k = LOG;
                Piece p = kindAt(rnd, k, x, y + hy, pz, hy);
                if (row == 0 && i == n / 2) key = p;
            }
            y += hy * 2f;
        }
    }

    /** A piece of a kind standing on so high, its own sizes picked to fit a slot SLOT wide. */
    private Piece kindAt(Random rnd, int kind, float x, float y, float z, float hy) {
        switch (kind) {
            case LOG: return put(rnd, LOG, x, y, z, hy, hy, 0.8f + rnd.nextFloat() * 0.3f);
            case STUMP: return put(rnd, STUMP, x, y, z, Math.min(0.46f, hy), hy, Math.min(0.46f, hy));
            case STONE: return put(rnd, STONE, x, y, z, 0.44f + rnd.nextFloat() * 0.06f, hy, 0.48f + rnd.nextFloat() * 0.12f);
            default: return put(rnd, CLAY, x, y, z, 0.44f + rnd.nextFloat() * 0.06f, hy, 0.48f + rnd.nextFloat() * 0.1f);
        }
    }

    /** A gate across the road at (u across from the middle, raised so high): two pillars, a lintel, a load on it; low walls out to the cliffs. */
    private void gate(Random rnd, float u, float y0) {
        float g = 2.0f + rnd.nextFloat() * 0.6f, h = 3.0f + rnd.nextFloat() * 1.0f;
        boolean posts = rnd.nextBoolean();
        Piece foot = null;
        for (int e = -1; e <= 1; e += 2) {
            float x = px + u + e * g;
            if (posts) {
                Piece p = put(rnd, POST, x, y0 + h * 0.5f, pz, 0.34f, h * 0.5f, 0.34f);
                if (e < 0) foot = p;
            } else {
                int n = Math.max(3, Math.round(h / 0.62f));
                float hy = h / n * 0.5f;
                for (int k = 0; k < n; k++) {
                    Piece p = put(rnd, k % 2 == 0 ? STONE : CLAY, x, y0 + hy + k * hy * 2f, pz, 0.44f, hy, 0.5f);
                    if (e < 0 && k == 1) foot = p;
                }
            }
        }
        key = foot;
        float r = 0.3f, ly = y0 + h + r;
        put(rnd, BEAM, px + u, ly, pz, g + 0.5f, r, r);
        // on the lintel: logs end on, row on row, each on the two under it
        float lr = 0.26f;
        int base = Math.min(6, (int) ((2f * g + 0.6f) / (2f * lr + 0.06f)));
        float y = ly + r;
        for (int row = 0; row < base; row++) {
            int n = base - row;
            for (int i = 0; i < n; i++)
                put(rnd, rnd.nextInt(5) == 0 ? STONE : LOG, px + u + (i - (n - 1) * 0.5f) * (2f * lr + 0.06f), y + lr, pz, lr, lr, 0.62f);
            y += lr * 2f;
        }
        if (y0 > 0f) return;
        // the walls out to the cliffs, two blocks high
        for (int e = -1; e <= 1; e += 2)
            for (float x = g + 1.15f; x < City.CLIFF_FOOT - 0.6f; x += 1.0f)
                for (int k = 0; k < 2; k++)
                    put(rnd, rnd.nextInt(3) == 0 ? CLAY : STONE, px + u + e * x, 0.34f + k * 0.68f, pz, 0.46f, 0.33f, 0.5f);
    }

    /** One or two log towers, each laid like a log house: two logs across, two along, over and over; a plank between, stones on it. */
    private void towers(Random rnd) {
        boolean three = rnd.nextBoolean(), two = !three;
        int layers = 11 + rnd.nextInt(5);
        float c = two ? 2.7f + rnd.nextFloat() * 0.4f : 2.9f;
        Piece low = null;
        for (int e = -1; e <= 1; e += two ? 2 : 1) {
            float x0 = px + e * c, half = 0.95f, r = 0.2f;
            int ls = e == 0 ? layers + 3 : e > 0 ? layers - rnd.nextInt(2) * 2 : layers;
            for (int k = 0; k < ls; k++) {
                float y = r + k * r * 2f;
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    Piece p = k % 2 == 0 ? put(rnd, BEAM, x0, y, pz + s2 * (half - r), half + 0.12f, r, r)
                            : put(rnd, LOG, x0 + s2 * (half - r), y, pz, r, r, half + 0.12f);
                    if (k == 1 && s2 < 0 && e == -1 && low == null) low = p;
                }
            }
            // a stone on top of each
            put(rnd, STONE, x0, ls * r * 2f + 0.36f, pz, 0.5f, 0.36f, 0.5f);
        }
        key = low;
        if (!two) return;
        // between them at their feet, a little heap
        for (int row = 0; row < 3; row++)
            for (int i = 0; i < 3 - row; i++)
                kindAt(rnd, rnd.nextInt(4), px + (i - (2 - row) * 0.5f) * SLOT, 0.36f + row * 0.72f, pz, 0.36f);
    }

    /** A wood pile: logs end on, row on row, each row set over the gaps of the one under it; a post at either end. */
    private Piece pile(Random rnd, float half, int rows, float y0, boolean posts) {
        float r = 0.24f + rnd.nextFloat() * 0.04f, step = 2f * r + 0.02f, hz = 0.55f + rnd.nextFloat() * 0.15f;
        int n = (int) (half * 2f / step);
        Piece mid = null;
        for (int row = 0; row < rows; row++) {
            int m = row % 2 == 0 ? n : n - 1;
            for (int i = 0; i < m; i++) {
                float x = px + (i - (m - 1) * 0.5f) * step;
                int k = rnd.nextInt(12) == 0 ? STONE : LOG;
                Piece p = put(rnd, k, x, y0 + r + row * 2f * r, pz, k == STONE ? r * 0.98f : r, r, hz);
                if (row == 0 && i == m / 2) mid = p;
            }
        }
        pileTop = y0 + rows * 2f * r;
        if (posts) {
            float h = rows * 2f * r + 0.35f;
            Piece first = null;
            for (int e = -1; e <= 1; e += 2) {
                Piece p = put(rnd, POST, px + e * ((n - 1) * 0.5f * step + r + 0.24f), y0 + h * 0.5f, pz, 0.2f, h * 0.5f, 0.2f);
                if (first == null) first = p;
            }
            key = first;
        }
        return mid;
    }

    /** A shed: four posts, planks across their tops, stones and clay on the planks; a barrier in front on two stumps. */
    private void shed(Random rnd) {
        float w = 1.7f + rnd.nextFloat() * 0.5f, d = 0.9f, h = 3.0f + rnd.nextFloat() * 0.6f;
        Piece front = null;
        for (int e = -1; e <= 1; e += 2)
            for (int f = -1; f <= 1; f += 2) {
                Piece p = put(rnd, POST, px + e * w, h * 0.5f, pz + f * d, 0.17f, h * 0.5f, 0.17f);
                if (e < 0 && f < 0) front = p;
            }
        key = front;
        for (int k = -1; k <= 1; k++)
            put(rnd, PLANK, px, h + 0.08f, pz + k * 0.62f, w + 0.4f, 0.08f, 0.3f);
        // the load: a row of stones, clay between, a second row on them
        int n = w > 1.85f ? 4 : 3;
        for (int row = 0; row < 3 && row < n; row++)
            for (int i = 0; i < n - row; i++)
                kindAt(rnd, row == 0 ? (i % 2 == 0 ? STONE : CLAY) : STONE, px + (i - (n - row - 1) * 0.5f) * SLOT, h + 0.16f + 0.34f + row * 0.68f, pz, 0.34f);
        // the barrier in front, on two stumps, painted
        float bz = pz - 2.4f, sx = w + 1.6f;
        for (int e = -1; e <= 1; e += 2) put(rnd, STUMP, px + e * sx, 0.45f, bz, 0.4f, 0.45f, 0.4f);
        Piece bar = put(rnd, BEAM, px, 0.9f + 0.17f, bz, sx + 0.5f, 0.17f, 0.17f);
        bar.color = 0xC8463A;
        bar.cap = 0xF2EEDF;
        // crates of stone at the shed's sides
        for (int e = -1; e <= 1; e += 2)
            for (int k = 0; k < 3; k++)
                put(rnd, k == 2 ? CLAY : STONE, px + e * (w + 1.2f), 0.34f + k * 0.68f, pz, 0.42f, 0.33f, 0.46f);
    }

    /** A boulder on a table of four posts and two planks; small stuff about its feet. */
    private void perch(Random rnd, float u, float y0) {
        float w = 1.2f, d = 0.8f, h = (y0 > 0f ? 1.9f : 2.6f) + rnd.nextFloat() * 0.6f;
        Piece front = null;
        for (int e = -1; e <= 1; e += 2)
            for (int f = -1; f <= 1; f += 2) {
                Piece p = put(rnd, POST, px + u + e * w, y0 + h * 0.5f, pz + f * d, 0.18f, h * 0.5f, 0.18f);
                if (e > 0 && f < 0) front = p;
            }
        key = front;
        for (int f = -1; f <= 1; f += 2) put(rnd, PLANK, px + u, y0 + h + 0.1f, pz + f * 0.52f, w + 0.4f, 0.1f, 0.5f);
        float br = (y0 > 0f ? 0.95f : 1.2f) + rnd.nextFloat() * 0.25f;
        Piece b = put(rnd, BOULDER, px + u, y0 + h + 0.2f + br * 0.9f, pz, br, br * 0.9f, br * 0.95f);
        b.b.scaleMass(1.6f);
        if (y0 > 0f) return;
        // under it and at its sides: clay and stones, a few stumps
        for (int i = -1; i <= 1; i++) kindAt(rnd, i == 0 ? CLAY : STUMP, px + u + i * 0.62f, 0.4f, pz, 0.4f);
        for (int e = -1; e <= 1; e += 2)
            for (int row = 0; row < 3; row++)
                for (int i = 0; i < 3 - row; i++)
                    kindAt(rnd, rnd.nextInt(4), px + u + e * (w + 1.9f) + (i - (2 - row) * 0.5f) * SLOT, 0.36f + row * 0.72f, pz, 0.36f);
    }

    /** Slabs on end in rows one behind the other, each row taller: the first falls on the next. */
    private void slabs(Random rnd) {
        int rows = 4 + rnd.nextInt(2);
        Piece first = null;
        for (int k = 0; k < rows; k++) {
            float hy = 1.2f + k * 0.2f, z = pz - 2.2f + k * 1.5f;
            float off = k % 2 == 0 ? 0f : 0.7f;
            for (float x = -4.2f + off; x <= 4.2f; x += 1.4f) {
                Piece p = put(rnd, SLAB, px + x, hy, z, 0.6f, hy, 0.13f);
                if (k == 0 && Math.abs(x) < 0.8f) first = p;
            }
        }
        key = first;
        // a log laid across the last row's tops
        float last = 1.2f + (rows - 1) * 0.2f;
        put(rnd, BEAM, px, last * 2f + 0.22f, pz - 2.2f + (rows - 1) * 1.5f, 3.9f, 0.22f, 0.22f);
    }

    /** Every fifth: a fort. A wood pile right across, two log towers on it, a boulder on a gate between them. */
    private void fort(Random rnd) {
        int rows = 3;
        float r = 0.26f;
        pile(rnd, 4.9f, rows, 0f, false);
        float top = pileTop + 0.005f;
        for (int e = -1; e <= 1; e += 2) {
            float x0 = px + e * 3.4f, half = 0.85f, lr = 0.19f;
            for (int k = 0; k < 7; k++) {
                float y = top + lr + k * lr * 2f;
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    if (k % 2 == 0) put(rnd, BEAM, x0, y, pz + s2 * (half - lr) * 0.6f, half + 0.1f, lr, lr);
                    else put(rnd, LOG, x0 + s2 * (half - lr), y, pz, lr, lr, 0.6f);
                }
            }
            put(rnd, STONE, x0, top + 14 * lr + 0.36f, pz, 0.45f, 0.36f, 0.45f);
        }
        perch(rnd, 0f, top);
        Piece low = null;
        for (Piece p : pieces) if (p.kind == LOG && Math.abs(p.b.px - px) < 0.4f && p.b.py < 0.5f) low = p;
        if (low != null) key = low;
    }

    /** One piece, of a kind, its middle at (x, y, z), so big (half sizes): its colours, and where on the slopes it comes down from. */
    private Piece put(Random rnd, int kind, float x, float y, float z, float hx, float hy, float hz) {
        int color, cap;
        int material = Body.WOOD;
        switch (kind) {
            case LOG:
            case BEAM:
            case POST:
                color = Street.mix(0x5A4634, 0x75604A, rnd.nextFloat());
                cap = Street.mix(0xC9A57A, 0xDDBB8C, rnd.nextFloat());
                break;
            case STUMP:
                color = Street.mix(0x47372A, 0x584434, rnd.nextFloat());
                cap = Street.mix(0xB88E5E, 0xCCA273, rnd.nextFloat());
                break;
            case PLANK:
                color = Street.mix(0x8E6A45, 0xA77E55, rnd.nextFloat());
                cap = Street.scale(color, 1.15f);
                break;
            case STONE:
            case SLAB:
            case BOULDER:
                color = Street.mix(0x8A8174, 0xA79C8B, rnd.nextFloat());
                cap = Street.scale(color, 1.08f);
                material = Body.STONE;
                break;
            default:
                color = Street.mix(0x9E5E3A, 0xB8774A, rnd.nextFloat());
                cap = Street.scale(color, 0.88f);
                material = Body.STONE;
                break;
        }
        Piece p = new Piece();
        p.kind = kind;
        p.color = color;
        p.cap = cap;
        p.b = new Body(hx, hy, hz, material, color);
        if (kind == CLAY) p.b.scaleMass(0.8f);
        float yaw = (rnd.nextFloat() - 0.5f) * 0.04f;
        p.b.setPose(x, y, z, (float) Math.cos(yaw * 0.5f), 0f, (float) Math.sin(yaw * 0.5f), 0f);
        home(p.b);
        p.b.ghost = true;
        // up the slope on its own side of the road, high: it comes down tumbling
        float side = x < px ? -1f : 1f;
        if (rnd.nextFloat() < 0.25f) side = -side;
        p.sx = px + side * (City.CLIFF_FOOT + 1f + rnd.nextFloat() * 3f);
        p.sy = 10f + rnd.nextFloat() * 12f + y * 0.6f;
        p.sz = z + (rnd.nextFloat() - 0.5f) * 12f;
        float ax = rnd.nextFloat() - 0.5f, ay = rnd.nextFloat() - 0.5f, az = rnd.nextFloat() - 0.5f, al = (float) Math.sqrt(ax * ax + ay * ay + az * az) + 1e-4f;
        p.ax = ax / al;
        p.ay = ay / al;
        p.az = az / al;
        p.angle = (float) Math.PI * (2f + rnd.nextFloat() * 2f);
        pieces.add(p);
        return p;
    }

    private static void home(Body b) {
        b.homeX = b.px;
        b.homeY = b.py;
        b.homeZ = b.pz;
        b.homeQw = b.qw;
        b.homeQy = b.qy;
    }

    /** The heap coming down: each piece on its way from its slope, or landed and solid. */
    private boolean coming(float dt) {
        boolean all = true;
        for (Piece p : pieces) {
            if (p.landed) continue;
            float u = (t - p.at) / FALL_FLIGHT;
            Body b = p.b;
            if (u < 0f) {
                all = false;
                continue;
            }
            if (u >= 1f) {
                b.setPose(b.homeX, b.homeY, b.homeZ, b.homeQw, 0f, b.homeQy, 0f);
                b.ghost = false;
                b.asleep = true;
                b.savePrev();
                p.landed = true;
                if (spRnd.nextInt(3) == 0) dust(b.px, Math.max(0.1f, b.py - b.hy), b.pz, p.color, 2, false);
                if (host != null) {
                    host.sound(p.b.material == Body.STONE ? Synth.STONE : Synth.WOOD, 0.35f + 0.25f * Math.min(1f, b.mass / 900f), 0.6f + (p.at * 7.3f % 1f) * 0.4f);
                    host.buzz(10);
                }
                continue;
            }
            all = false;
            // falling: down from the slope toward where it lies, faster as it comes, turning over
            float e = u * u;
            float x = p.sx + (b.homeX - p.sx) * u, z = p.sz + (b.homeZ - p.sz) * u;
            float y = p.sy + (b.homeY - p.sy) * e + (float) Math.sin(Math.PI * u) * 1.2f;
            float a = (1f - u) * p.angle * 0.5f, s = (float) Math.sin(a);
            // home turned by what is left of the tumble: q = home x spin
            float hw = b.homeQw, hy = b.homeQy, sw = (float) Math.cos(a), sx = p.ax * s, sy = p.ay * s, sz = p.az * s;
            float qw = hw * sw - hy * sy, qx = hw * sx + hy * sz, qy = hw * sy + hy * sw, qz = hw * sz - hy * sx;
            b.setPose(x, y, z, qw, qx, qy, qz);
        }
        return all;
    }

    /** How high the lowest row comes. */
    /** Whether it is knocked flat: nothing left standing a third as high as the figure stood. */
    boolean flat() {
        float most = Math.max(0.75f, height0 * 0.33f);
        for (Piece p : pieces) if (!p.landed || p.b.py > most) return false;
        return true;
    }

    /** The share of the heap's weight moved off where it lay. */
    float moved() {
        float m = 0f, all = 0f;
        for (Piece p : pieces) {
            Body b = p.b;
            all += b.mass;
            if (!p.landed) continue;
            float dx = b.px - b.homeX, dy = b.py - b.homeY, dz = b.pz - b.homeZ;
            if (dx * dx + dy * dy + dz * dz > MOVED * MOVED) m += b.mass;
        }
        return all > 0f ? m / all : 0f;
    }

    // ---- the phases ----

    /** Whether it has her now: the eye its own, the fingers its own. */
    boolean holds() {
        return phase >= ARRIVE && phase <= PLAY;
    }

    /** Whether the pickup is its own to bring in (not the driver's). */
    boolean drives() {
        return phase == ARRIVE || phase == HALT;
    }

    /** Whether she is at the line, the mallet hers to throw. */
    boolean playing() {
        return phase == PLAY;
    }

    /** Whether the game has gone past the first heap: the mission's clock stands meanwhile. */
    boolean extra() {
        return holds() && (broken > 0 || figure > 1);
    }

    /** The heap, the mallet and the pickup a step on; what it brought. */
    int update(float dt, Pickup driving, Runner r, World.Host h) {
        host = h;
        t += dt;
        if (old.size() > 0 && (oldT += dt) > 0.5f) old.clear();
        int got = NONE;
        switch (phase) {
            case WAIT:
                if (driving != null && driving.z > tz - STOP_BACK - FROM && driving.z < pz) {
                    pickup = driving;
                    arrive(driving);
                } else if (driving == null && r.z > tz - 6f && r.z < pz) {
                    // on foot: no pickup to bring in; the slopes come down as she comes up
                    onFoot = true;
                    to(OUT);
                    t = SLIDE_AFTER;
                    build();
                    eyeFrom(r.eyeX, r.eyeY, r.eyeZ, r.eyeX + (float) Math.sin(r.eyeYaw), r.eyeY + (float) Math.sin(r.eyePitch), r.eyeZ + (float) Math.cos(r.eyeYaw));
                    if (host != null) host.sound(Synth.GROUND, 0.7f, 0.5f);
                    got = GET_OUT;
                }
                break;
            case HALT:
                if (t >= HALT_TIME) {
                    to(OUT);
                    build();
                    snapshotEye();
                    got = GET_OUT;
                }
                break;
            case OUT:
                if (t >= SLIDE_AFTER) {
                    to(FALL);
                    if (host != null) {
                        host.sound(Synth.GROUND, 0.8f, 0.45f);
                        host.sound(Synth.ROLL, 0.5f, 0.6f);
                        host.buzz(60);
                    }
                }
                break;
            case FALL:
                if (coming(dt) && t > 0.4f) {
                    to(PLAY);
                    snapshotEye();
                    throwsHere = 0;
                    down = false;
                    got = READY;
                }
                break;
            case PLAY:
                if (!down && !missed && (moved() >= CLEAR || flat())) {
                    down = true;
                    broken++;
                    stars = Math.max(1, THROWS + 1 - throwsHere);
                    got = BROKE;
                } else if (!down && !missed && throwsHere >= THROWS && hs == HAND && !pending) {
                    // the last throw back in her hand: once all lies still, the figure stands
                    missT += dt;
                    if (missT > MISS_WAIT && world.settled() || missT > MISS_MOST) {
                        missed = true;
                        got = MISSED;
                        if (host != null) host.sound(Synth.GROUND, 0.5f, 0.6f);
                    }
                } else missT = 0f;
                break;
            default:
                break;
        }
        hammer(dt, r);
        boolean close = phase == PLAY && (aiming || pending || hs != HAND);
        zoom = Math.max(0f, Math.min(1f, zoom + (close ? dt : -dt) / ZOOM_T));
        placeCar();
        clock += dt;
        if (phase >= FALL && (phase != LEFT || near(r, driving))) {
            // a hard blow holds the world still a moment: it is felt
            if (stop > 0f) stop -= dt;
            else {
                world.step(Math.min(dt, 1f / 30f));
                tame();
                afterStep();
            }
        }
        shake = Math.max(0f, shake - dt * 2.4f);
        specks(dt);
        if (holds()) eye(dt, r);
        return got;
    }

    private boolean near(Runner r, Pickup driving) {
        float x = driving != null ? driving.x : r.x, z = driving != null ? driving.z : r.z;
        return Math.abs(x - px) < 60f && Math.abs(z - pz) < 90f;
    }

    /** Nothing flies off faster than so much: a shove by the pickup, put right all at once, is no shot. */
    private void tame() {
        for (Piece p : pieces) {
            Body b = p.b;
            float s2 = b.vx * b.vx + b.vy * b.vy + b.vz * b.vz;
            if (s2 > 22f * 22f) {
                float k = 22f / (float) Math.sqrt(s2);
                b.vx *= k;
                b.vy *= k;
                b.vz *= k;
            }
        }
    }

    private void to(int ph) {
        phase = ph;
        t = 0f;
    }

    /** The same figure again, from the slopes: the three throws afresh. */
    void retry() {
        plan = planBefore;
        figure--;
        again();
    }

    /** Another heap down the slopes, the last one gone. */
    void again() {
        missed = false;
        missT = 0f;
        stars = 0;
        toSplit.clear();
        keyStruck = false;
        old.clear();
        old.addAll(pieces);
        oldT = 0f;
        for (Piece p : old) p.shown = 1f;
        build();
        to(FALL);
        down = false;
        throwsHere = 0;
        if (host != null) host.sound(Synth.GROUND, 0.7f, 0.5f);
    }

    /** She goes on: the pickup hers again, the pieces left lying where they are. */
    void leave() {
        to(LEFT);
        aiming = false;
        if (hs != HAND) {
            hs = HAND;
            mallet.ghost = true;
        }
    }

    // ---- the pickup brought in ----

    private float carX0, carZ0, carH0, carZ, carV, carZStop, carT;

    private void arrive(Pickup p) {
        to(ARRIVE);
        carX0 = p.x;
        carZ0 = p.z;
        carH0 = p.heading;
        carZ = p.z;
        carV = Math.max(9f, Math.min(22f, Math.abs(p.speed)));
        carZStop = tz - STOP_BACK;
        carT = 0f;
    }

    /** The pickup a step on under its own hand: up the road, slowing hard, stopped short of the line. */
    void carry(Pickup p, float dt) {
        if (phase == ARRIVE) {
            carT += dt;
            float left = carZStop - carZ;
            if (left <= carV * carV / (2f * BRAKE) + 0.05f) carV = Math.max(0f, carV - BRAKE * dt);
            else carV += (CRUISE - carV) * Math.min(1f, dt * 1.5f);
            carZ = Math.min(carZStop, carZ + carV * dt);
            if (carZ >= carZStop - 0.02f || carV <= 0f && left < 1f) {
                carZ = carZStop;
                carV = 0f;
                to(HALT);
                if (host != null) {
                    host.sound(Synth.RUBBER, 0.6f, 0.8f);
                    host.buzz(40);
                }
            }
            float k = Math.min(1f, carT / 1.6f), e = k * k * (3f - 2f * k);
            float run = (float) Math.atan2(City.routeX(carZ + 1f) - City.routeX(carZ - 1f), 2f);
            float d = run - carH0;
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            p.x = carX0 + (City.routeX(carZ) - carX0) * e;
            p.z = carZ;
            p.heading = carH0 + d * e;
            p.speed = carV;
        } else {
            p.speed = 0f;
        }
        p.steer = 0f;
        p.y = 0f;
    }

    /** The pickup in the physics, where it is now: what it runs into, it shoves. */
    private void placeCar() {
        Pickup p = pickup;
        if (p == null) {
            car.ghost = true;
            return;
        }
        car.ghost = false;
        float a = p.heading * 0.5f;
        car.setPose(p.x, Pickup.TOP * 0.5f, p.z, (float) Math.cos(a), 0f, (float) Math.sin(a), 0f);
        car.savePrev();
    }

    /** The pickup she has, the one brought in (or null on foot). */
    Pickup pickup() {
        return pickup;
    }

    // ---- the eye ----

    /** The eye now: where, looking where; and where it set off from in a glide. */
    final float[] eye = new float[5];
    private float ex, ey, ez, lx, ly, lz, ex0, ey0, ez0, lx0, ly0, lz0, glide = 1f;
    private float tx, ty, tzz, ux, uy, uz;

    private void eyeFrom(float x, float y, float z, float a, float b, float c) {
        ex = x;
        ey = y;
        ez = z;
        lx = a;
        ly = b;
        lz = c;
        snapshotEye();
    }

    private void snapshotEye() {
        ex0 = ex;
        ey0 = ey;
        ez0 = ez;
        lx0 = lx;
        ly0 = ly;
        lz0 = lz;
        glide = 0f;
    }

    // the eye at the line: so far to her right and behind her, so high; looking at so high on the heap
    static final float PLAY_RIGHT = 1.5f, PLAY_BACK = 4.6f, PLAY_UP = 3.1f, PLAY_LOOK = 1.9f;

    private void eye(float dt, Runner r) {
        Pickup p = pickup;
        boolean glides = false;
        float dur = 1f;
        switch (phase) {
            case ARRIVE: {
                // from high and far off, behind and to the side, down onto the pickup
                float k = Math.min(1f, Math.max(0f, (carZ - carZ0) / Math.max(1f, carZStop - carZ0)));
                float e = k * k * (3f - 2f * k);
                float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), rx = fz, rz = -fx;
                float back = 75f + (13f - 75f) * e, up = 50f + (6.5f - 50f) * e, side = 35f + (3f - 35f) * e;
                tx = p.x - fx * back + rx * side;
                ty = up;
                tzz = p.z - fz * back + rz * side;
                float ahead = 6f + 8f * e;
                ux = p.x + fx * ahead;
                uy = 1f;
                uz = p.z + fz * ahead;
                break;
            }
            case HALT:
            case OUT:
                if (p == null) {
                    fallEye();
                    glides = true;
                    break;
                }
                closeEye(p);
                break;
            case FALL:
                if (figure > 1) {
                    playEye();
                    glides = true;
                    break;
                }
                // behind her side of the pickup a moment yet, her out of it; then back behind the
                // line, high, the slopes coming down on either side
                if (p != null && t < FALL_STAY) {
                    closeEye(p);
                    snapshotEye();
                    break;
                }
                fallEye();
                glides = true;
                dur = 2.2f;
                break;
            default:
                playEye();
                glides = true;
                dur = 1.1f;
                break;
        }
        if (glides && glide < 1f) {
            glide = Math.min(1f, glide + dt / dur);
            float e = glide * glide * (3f - 2f * glide);
            ex = ex0 + (tx - ex0) * e;
            ey = ey0 + (ty - ey0) * e;
            ez = ez0 + (tzz - ez0) * e;
            lx = lx0 + (ux - lx0) * e;
            ly = ly0 + (uy - ly0) * e;
            lz = lz0 + (uz - lz0) * e;
        } else {
            ex = tx;
            ey = ty;
            ez = tzz;
            lx = ux;
            ly = uy;
            lz = uz;
        }
        float dx = lx - ex, dy = ly - ey, dz = lz - ez;
        float a = shake * shake * 0.3f;
        eye[0] = ex + a * (float) Math.sin(clock * 61f);
        eye[1] = ey + a * (float) Math.sin(clock * 47f + 1f);
        eye[2] = ez + a * (float) Math.sin(clock * 53f + 2f);
        eye[3] = (float) Math.atan2(dx, dz);
        eye[4] = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
    }

    // after the slopes start to come down, the eye stays by the pickup so long
    static final float FALL_STAY = 0.9f;

    /** Close behind the pickup and off to her side of it, a little up, looking on at the pass. */
    private void closeEye(Pickup p) {
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), rx = fz, rz = -fx;
        tx = p.x - fx * 7f - rx * 2.2f;
        ty = 3.3f;
        tzz = p.z - fz * 7f - rz * 2.2f;
        ux = px;
        uy = 3.5f;
        uz = pz;
    }

    /** Well back behind the line and up: the road, the pass, the slopes on either side. */
    private void fallEye() {
        tx = standX + 1.2f;
        ty = 5.2f;
        tzz = tz - 13f;
        ux = px;
        uy = 4.5f;
        uz = pz;
    }

    private void playEye() {
        tx = standX + PLAY_RIGHT;
        ty = PLAY_UP;
        tzz = tz - PLAY_BACK;
        ux = px;
        uy = PLAY_LOOK;
        uz = pz;
    }

    /** Which way she faces at the line: at the heap. */
    float facing() {
        return (float) Math.atan2(px - standX, pz - tz);
    }

    // ---- the mallet ----

    // where it is: in her hand, on its way, coming back
    static final int HAND = 0, FLY = 1, BACK = 2;
    int hs = HAND;
    private float hsT, flight, retX, retY, retZ;
    /** A throw asked for, to be let go at the height of her swing; where at. */
    private boolean pending;
    private final float[] aimAt = new float[3], hand = new float[3];

    /** Whether the mallet is out of her hand (the one she holds not to be drawn). */
    boolean away() {
        return hs != HAND;
    }

    /** Whether it is hers to throw now. */
    boolean ready() {
        return phase == PLAY && hs == HAND && !pending && !missed && (down || throwsHere < THROWS);
    }

    private void hammer(float dt, Runner r) {
        hsT += dt;
        if (pending && r.throwing() >= Toss.RELEASE) {
            pending = false;
            launch(r);
        } else if (pending && r.throwing() < 0f && hsT > 1f) pending = false;
        if (hs == FLY) {
            Body m = mallet;
            if (hsT > flight + 0.6f && (m.speed2() < 4f || m.py < 0.4f) || hsT > flight + OUT_MOST) {
                hs = BACK;
                hsT = 0f;
                retX = m.px;
                retY = m.py;
                retZ = m.pz;
                m.ghost = true;
            }
        } else if (hs == BACK) {
            float k = Math.min(1f, hsT / BACK_TIME), e = k * k * (3f - 2f * k);
            r.bone("hand_r", hand);
            Body m = mallet;
            m.px = retX + (hand[0] - retX) * e;
            m.py = retY + (hand[1] - retY) * e + (float) Math.sin(Math.PI * k) * 1.4f;
            m.pz = retZ + (hand[2] - retZ) * e;
            m.rotateBy(0f, 0f, -14f * dt);
            m.normalize();
            m.updateR();
            if (k >= 1f) {
                hs = HAND;
                if (host != null) {
                    host.sound(Synth.CATCH, 0.7f, 1f);
                    host.buzz(20);
                }
            }
        }
    }

    /** Out of her hand at the height of the swing, to land on the point aimed at. */
    private void launch(Runner r) {
        r.bone("hand_r", hand);
        float sx = hand[0], sy = hand[1], sz = hand[2];
        float dx = aimAt[0] - sx, dy = aimAt[1] - sy, dz = aimAt[2] - sz;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float tt = Math.max(0.3f, dist / SPEED);
        float fl = Math.max(1e-3f, (float) Math.sqrt(dx * dx + dz * dz)), fx = dx / fl, fz = dz / fl;
        // the handle along the way it goes, the head ahead; it turns end over end about the head's length
        float a = (float) Math.atan2(-fz, fx) * 0.5f;
        Body m = mallet;
        m.setPose(sx, sy, sz, (float) Math.cos(a), 0f, (float) Math.sin(a), 0f);
        m.ghost = false;
        m.asleep = false;
        m.vx = dx / tt;
        m.vy = dy / tt - 0.5f * Physics.G * tt;
        m.vz = dz / tt;
        m.wx = -fz * -13f;
        m.wy = 0f;
        m.wz = fx * -13f;
        m.savePrev();
        flight = tt;
        hs = FLY;
        hsT = 0f;
        throwsHere++;
        throwsAll++;
        if (host != null) {
            host.sound(Synth.WHOOSH, 0.9f, 0.95f);
            host.buzz(12);
        }
    }

    // ---- the aim: a ring on the frame, put down on the heap, moved by the finger ----

    private float zoom;

    /** How much narrower the eye sees now: closing in while she aims and throws. */
    float lens() {
        float e = zoom * zoom * (3f - 2f * zoom);
        return 1f + (LENS - 1f) * e;
    }

    boolean aiming;
    /** The point aimed at, on the heap or the upright plane through it: the ring stays on it however the eye closes in. */
    private final float[] aimP = new float[3];
    private final float[] camEye = new float[3], camRight = new float[3], camUp = new float[3], camAhead = new float[3];
    private float camFocal = 1f;
    private int camW = 1, camH = 1;
    private final float[] scr = new float[3], dir = new float[3];

    /** The eye as the frame has just been drawn: kept, to turn the ring into a point aimed at. */
    void frame(DrawList d) {
        System.arraycopy(d.eye, 0, camEye, 0, 3);
        System.arraycopy(d.right, 0, camRight, 0, 3);
        System.arraycopy(d.up, 0, camUp, 0, 3);
        System.arraycopy(d.ahead, 0, camAhead, 0, 3);
        camFocal = d.focal;
        camW = d.w;
        camH = d.h;
    }

    private boolean project(float x, float y, float z, float[] out) {
        float rx = x - camEye[0], ry = y - camEye[1], rz = z - camEye[2];
        float a = rx * camAhead[0] + ry * camAhead[1] + rz * camAhead[2];
        if (a < 0.1f) return false;
        float u = rx * camRight[0] + ry * camRight[1] + rz * camRight[2], v = rx * camUp[0] + ry * camUp[1] + rz * camUp[2];
        out[0] = camW * 0.5f + camFocal * u / a;
        out[1] = camH * 0.5f - camFocal * v / a;
        return true;
    }

    /** A finger down: the ring on the heap's middle (by its weight, as it is now). */
    void aimDown() {
        if (!ready()) return;
        float sx = 0f, sy = 0f, sz = 0f, m = 0f;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed) continue;
            sx += b.px * b.mass;
            sy += b.py * b.mass;
            sz += b.pz * b.mass;
            m += b.mass;
        }
        if (m > 0f) {
            aimP[0] = sx / m;
            aimP[1] = sy / m;
            aimP[2] = sz / m;
        } else {
            aimP[0] = px;
            aimP[1] = 1.5f;
            aimP[2] = pz;
        }
        aiming = true;
    }

    /** The finger moved: the ring with it, as far across the frame (as the eye sees it now). */
    void aimBy(float dx, float dy) {
        if (!aiming) return;
        float rx = aimP[0] - camEye[0], ry = aimP[1] - camEye[1], rz = aimP[2] - camEye[2];
        float a = Math.max(1f, rx * camAhead[0] + ry * camAhead[1] + rz * camAhead[2]), k = a / camFocal;
        for (int i = 0; i < 3; i++) aimP[i] += camRight[i] * dx * k - camUp[i] * dy * k;
        aimP[0] = Math.max(px - 9f, Math.min(px + 9f, aimP[0]));
        aimP[1] = Math.max(0.2f, Math.min(10f, aimP[1]));
    }

    /** Where the ring is on the frame now, into out; false if not to be seen. */
    boolean ring(float[] out) {
        return aiming && project(aimP[0], aimP[1], aimP[2], out);
    }

    /** The finger lifted: she throws at what is under the ring. */
    void aimUp(Runner r) {
        if (!aiming) return;
        aiming = false;
        if (!ready()) return;
        if (project(aimP[0], aimP[1], aimP[2], scr)) target(scr[0], scr[1], aimAt);
        else System.arraycopy(aimP, 0, aimAt, 0, 3);
        r.lock(Toss.LOCK, aimAt[0], aimAt[2]);
        pending = true;
        hsT = 0f;
    }

    /** The world point under a frame point: the nearest piece, or the upright plane across the road through the heap. */
    private void target(float sx, float sy, float[] out) {
        float u = (sx - camW * 0.5f) / camFocal, v = (camH * 0.5f - sy) / camFocal;
        for (int k = 0; k < 3; k++) dir[k] = camAhead[k] + camRight[k] * u + camUp[k] * v;
        float best = 1e9f;
        for (Piece p : pieces) {
            if (!p.landed) continue;
            float tt = rayBox(p.b, camEye[0], camEye[1], camEye[2], dir[0], dir[1], dir[2]);
            if (tt > 0f && tt < best) best = tt;
        }
        if (best > 1e8f) {
            best = Math.abs(dir[2]) > 1e-4f ? (pz - camEye[2]) / dir[2] : 30f;
            if (best <= 0f) best = 30f;
        }
        for (int k = 0; k < 3; k++) out[k] = camEye[k] + dir[k] * best;
        if (out[1] < 0.2f) out[1] = 0.2f;
    }

    static float rayBox(Body b, float ox, float oy, float oz, float dx, float dy, float dz) {
        float qx = ox - b.px, qy = oy - b.py, qz = oz - b.pz;
        float o0 = b.r00 * qx + b.r10 * qy + b.r20 * qz, o1 = b.r01 * qx + b.r11 * qy + b.r21 * qz, o2 = b.r02 * qx + b.r12 * qy + b.r22 * qz;
        float d0 = b.r00 * dx + b.r10 * dy + b.r20 * dz, d1 = b.r01 * dx + b.r11 * dy + b.r21 * dz, d2 = b.r02 * dx + b.r12 * dy + b.r22 * dz;
        float t0 = -1e9f, t1 = 1e9f;
        for (int i = 0; i < 3; i++) {
            float o = i == 0 ? o0 : i == 1 ? o1 : o2, d = i == 0 ? d0 : i == 1 ? d1 : d2, h = i == 0 ? b.hx : i == 1 ? b.hy : b.hz;
            if (Math.abs(d) < 1e-9f) {
                if (Math.abs(o) > h) return -1f;
                continue;
            }
            float a = (-h - o) / d, c = (h - o) / d;
            if (a > c) {
                float s = a;
                a = c;
                c = s;
            }
            t0 = Math.max(t0, a);
            t1 = Math.min(t1, c);
            if (t0 > t1) return -1f;
        }
        return t0 > 0f ? t0 : (t1 > 0f ? 0.001f : -1f);
    }

    // ---- what the pickup and she run into, after ----

    /** The pickup about to move on: what it is going into fast enough is shoved off ahead and aside. */
    void ram(Pickup pk, float dt) {
        if (Math.abs(pk.speed) < 2.5f) return;
        float sg = Math.signum(pk.speed), fx = (float) Math.sin(pk.heading) * sg, fz = (float) Math.cos(pk.heading) * sg;
        float v = Math.abs(pk.speed), reach = Pickup.ENDS + v * dt;
        float cx = pk.x + fx * reach, cz = pk.z + fz * reach;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed || b.py > 2.5f) continue;
            float r = Math.max(b.hx, b.hz);
            float dx = b.px - cx, dz = b.pz - cz, m = r + Pickup.END_R + 0.3f, dd = dx * dx + dz * dz;
            if (dd >= m * m) continue;
            float l = (float) Math.sqrt(dd) + 1e-4f;
            float push = Math.max(v * 0.9f, 4f), across = dx * fz - dz * fx;
            float side = Math.abs(across) > 0.2f ? Math.signum(across) : (b.id % 2 == 0 ? 1f : -1f);
            b.px += dx / l * (m - l) * 0.5f;
            b.pz += dz / l * (m - l) * 0.5f;
            b.updateR();
            world.wake(b);
            b.vx = fx * push * 0.45f + fz * side * push * 0.8f;
            b.vz = fz * push * 0.45f - fx * side * push * 0.8f;
            b.vy = 2f;
            b.wy = across / l * push * 0.3f;
            hit++;
        }
    }

    /** How many times something has been struck by the pickup or the mallet in hand. */
    int hit;

    /** A blow of the mallet on foot at (bx, bz), facing (fx, fz): what lies within reach before her is knocked off. */
    void strike(float bx, float bz, float fx, float fz) {
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed) continue;
            float dx = b.px - bx, dz = b.pz - bz, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd > Math.max(b.hx, b.hz) + 2.2f || dx * fx + dz * fz < -0.3f) continue;
            float l = dd + 1e-4f;
            world.wake(b);
            b.vx = fx * 5.4f + dx / l * 5.4f;
            b.vz = fz * 5.4f + dz / l * 5.4f;
            b.vy = 2.5f;
            hit++;
        }
    }

    /** What lies on the road, in her way and the pickup's (see City.Extra): each piece low enough, a round. */
    boolean push(float[] q, float rr, float feet) {
        boolean any = false;
        for (Piece p : pieces) {
            Body b = p.b;
            if (!p.landed || feet > b.maxY - 0.15f || b.minY > feet + 1.6f) continue;
            float r = (Math.max(b.hx, b.hz) + Math.min(b.hx, b.hz)) * 0.5f;
            float dx = q[0] - b.px, dz = q[1] - b.pz, m = rr + r, dd = dx * dx + dz * dz;
            if (dd >= m * m || dd < 1e-8f) continue;
            float l = (float) Math.sqrt(dd);
            q[0] = b.px + dx / l * m;
            q[1] = b.pz + dz / l * m;
            any = true;
        }
        return any;
    }

    /** Whether the road through is clear: nothing left lying across it near the pass. */
    boolean cleared() {
        for (Piece p : pieces) {
            Body b = p.b;
            if (Math.abs(b.pz - pz) > 14f || b.py > 3f) continue;
            if (Math.abs(b.px - City.routeX(b.pz)) < City.ROUTE_HALF) return false;
        }
        return true;
    }

    @Override
    public void knock(Body a, Body b, float speed, float x, float y, float z) {
        if (speed < 1.2f) return;
        float v = Math.min(1f, speed / 9f);
        boolean metal = a == mallet || b == mallet;
        if (host != null) {
            int s = a.material == Body.STONE || b != null && b.material == Body.STONE ? Synth.STONE : Synth.WOOD;
            host.sound(s, 0.25f + 0.55f * v, 0.7f + 0.3f * ((a.id * 37 % 10) / 10f));
            if (metal && speed > 4f) host.buzz(25 + Math.round(v * 35f));
        }
        if (metal) {
            Piece p = pieceOf(a == mallet ? b : a);
            if (p == null) return;
            if (speed > 3f) {
                stop = Math.max(stop, Math.min(0.11f, 0.035f + speed * 0.006f));
                shake = Math.min(1f, shake + speed * 0.05f);
            }
            dust(x, y, z, p.color, 3 + Math.min(12, (int) speed), true);
            free(p);
            if (p == key && speed > 2.5f && !down && !missed) keyStruck = true;
            if (speed > SPLIT_SPEED && !toSplit.contains(p.b)) toSplit.add(p.b);
        } else if (speed > 3f) {
            Piece p = pieceOf(a);
            if (p != null) dust(x, y, z, p.color, 1 + Math.min(4, (int) (speed * 0.4f)), speed > 6f);
            if (speed > SPLIT_CRASH) {
                if (p != null && !toSplit.contains(p.b)) toSplit.add(p.b);
                Piece q = b == null ? null : pieceOf(b);
                if (q != null && !toSplit.contains(q.b)) toSplit.add(q.b);
            }
        }
    }

    /** How many pieces the figure is in now. */
    int count() {
        return pieces.size();
    }

    private Piece pieceOf(Body b) {
        if (b == null) return null;
        for (Piece p : pieces) if (p.b == b) return p;
        return null;
    }

    /** The raised inertia given up: it topples as freely as a real block from now on. */
    private void free(Piece p) {
        if (p.free) return;
        p.free = true;
        p.b.trueInertia();
    }

    /** After a step: the key gone (all of it given up, a shove up through what it held), the pieces split. */
    private void afterStep() {
        if (keyStruck) {
            keyStruck = false;
            Piece k = key;
            key = null;
            float ky = k != null ? k.b.py : 0f;
            Random rnd = new Random(figure * 31L + throwsAll);
            for (Piece p : pieces) {
                if (!p.landed) continue;
                free(p);
                world.wake(p.b);
                if (p.b.py < ky - 0.2f) continue;
                // what it held loosens: a nudge each, this way and that
                p.b.vx += (rnd.nextFloat() - 0.5f) * 2.2f;
                p.b.vy += 0.6f + rnd.nextFloat() * 1.2f;
                p.b.vz += 0.4f + rnd.nextFloat() * 1.4f;
                p.b.wx += (rnd.nextFloat() - 0.5f) * 2f;
                p.b.wz += (rnd.nextFloat() - 0.5f) * 2f;
            }
            if (k != null) {
                dust(k.b.px, k.b.py, k.b.pz, 0xFFD45A, 16, true);
                if (!toSplit.contains(k.b)) toSplit.add(k.b);
            }
            stop = Math.max(stop, 0.14f);
            shake = 1f;
            if (host != null) {
                host.sound(Synth.GROUND, 0.9f, 0.5f);
                host.sound(Synth.CHIME, 0.4f, 0.8f);
                host.buzz(120);
            }
        }
        for (int i = 0; i < toSplit.size() && i < 6; i++) split(pieceOf(toSplit.get(i)));
        toSplit.clear();
    }

    /** A piece split in two along its length (a round one along its own axis), the halves flying apart. */
    private void split(Piece p) {
        if (p == null || pieces.size() >= MOST || !p.landed) return;
        Body b = p.b;
        int ax;
        switch (p.kind) {
            case LOG: ax = 2; break;
            case BEAM: case PLANK: ax = 0; break;
            case POST: case STUMP: ax = 1; break;
            default: ax = b.hx >= b.hy && b.hx >= b.hz ? 0 : b.hy >= b.hz ? 1 : 2; break;
        }
        float h = ax == 0 ? b.hx : ax == 1 ? b.hy : b.hz;
        if (h < SPLIT_MIN) {
            dust(b.px, b.py, b.pz, p.color, 6, true);
            return;
        }
        float dx = ax == 0 ? b.r00 : ax == 1 ? b.r01 : b.r02;
        float dy = ax == 0 ? b.r10 : ax == 1 ? b.r11 : b.r12;
        float dz = ax == 0 ? b.r20 : ax == 1 ? b.r21 : b.r22;
        world.remove(b);
        pieces.remove(p);
        if (key == p) key = null;
        for (int e = -1; e <= 1; e += 2) {
            Body n = new Body(ax == 0 ? h * 0.5f : b.hx, ax == 1 ? h * 0.5f : b.hy, ax == 2 ? h * 0.5f : b.hz, b.material, b.tint);
            float o = e * h * 0.5f;
            n.setPose(b.px + dx * o, b.py + dy * o, b.pz + dz * o, b.qw, b.qx, b.qy, b.qz);
            n.homeX = b.homeX;
            n.homeY = b.homeY;
            n.homeZ = b.homeZ;
            n.homeQw = b.homeQw;
            n.homeQy = b.homeQy;
            n.savePrev();
            n.vx = b.vx + dx * e * 1.4f;
            n.vy = b.vy + dy * e * 1.4f + 0.6f;
            n.vz = b.vz + dz * e * 1.4f;
            n.wx = b.wx + e * 1.5f;
            n.wy = b.wy;
            n.wz = b.wz - e * 1.5f;
            Piece q = new Piece();
            q.b = n;
            q.kind = p.kind == BOULDER && h < 0.6f ? STONE : p.kind;
            q.color = p.color;
            q.cap = p.cap;
            q.landed = true;
            q.free = true;
            n.trueInertia();
            world.add(n);
            pieces.add(q);
        }
        dust(b.px, b.py, b.pz, p.cap, 10, true);
        if (host != null) {
            host.sound(b.material == Body.STONE ? Synth.STONE : Synth.WOOD, 0.9f, 0.55f);
            host.buzz(40);
        }
    }

    // ---- the specks: chips off what is struck, dust where it lands ----

    static final int SPECKS = 360;
    // x, y, z, vx, vy, vz, life left, life in all
    private final float[] sp = new float[SPECKS * 8];
    private final int[] spColor = new int[SPECKS];
    private final boolean[] spChip = new boolean[SPECKS];
    private int spN, spNext;
    private final Random spRnd = new Random(0xD057);

    /** So many specks at (x, y, z): chips of the colour, flying, or a puff of pale dust. */
    private void dust(float x, float y, float z, int color, int n, boolean chips) {
        for (int i = 0; i < n; i++) {
            int k = spNext;
            spNext = (spNext + 1) % SPECKS;
            spN = Math.max(spN, spNext == 0 ? SPECKS : spNext);
            boolean chip = chips && i % 2 == 0;
            float a = spRnd.nextFloat() * 6.283f, up = spRnd.nextFloat();
            float sp0 = chip ? 2.5f + spRnd.nextFloat() * 4f : 0.4f + spRnd.nextFloat() * 0.8f;
            int o = k * 8;
            sp[o] = x;
            sp[o + 1] = y;
            sp[o + 2] = z;
            sp[o + 3] = (float) Math.cos(a) * sp0;
            sp[o + 4] = chip ? 1.5f + up * 4f : 0.3f + up * 0.6f;
            sp[o + 5] = (float) Math.sin(a) * sp0 - (chip ? 1f : 0f);
            float life = chip ? 0.7f + spRnd.nextFloat() * 0.7f : 0.9f + spRnd.nextFloat() * 0.8f;
            sp[o + 6] = life;
            sp[o + 7] = life;
            spColor[k] = chip ? color : Street.mix(0xD9C9A6, 0xC4B08A, spRnd.nextFloat());
            spChip[k] = chip;
        }
    }

    private void specks(float dt) {
        for (int k = 0; k < spN; k++) {
            int o = k * 8;
            if (sp[o + 6] <= 0f) continue;
            sp[o + 6] -= dt;
            if (spChip[k]) {
                sp[o + 4] += Physics.G * dt;
                if (sp[o + 1] < 0.04f && sp[o + 4] < 0f) {
                    sp[o + 1] = 0.04f;
                    sp[o + 4] *= -0.3f;
                    sp[o + 3] *= 0.6f;
                    sp[o + 5] *= 0.6f;
                }
            } else {
                float drag = (float) Math.exp(-2.5f * dt);
                sp[o + 3] *= drag;
                sp[o + 5] *= drag;
                sp[o + 4] = sp[o + 4] * drag + 0.2f * dt;
            }
            sp[o] += sp[o + 3] * dt;
            sp[o + 1] += sp[o + 4] * dt;
            sp[o + 2] += sp[o + 5] * dt;
        }
    }

    private void drawSpecks(DrawList d) {
        for (int k = 0; k < spN; k++) {
            int o = k * 8;
            float life = sp[o + 6];
            if (life <= 0f) continue;
            float u = life / sp[o + 7];
            if (spChip[k]) {
                float s = 0.07f * Math.min(1f, u * 2f);
                d.box(sp[o], sp[o + 1], sp[o + 2], s, s, s, null, spColor[k], DrawList.STONE);
            } else {
                // a puff swells and thins away
                float s = (0.12f + 0.45f * (1f - u)) * Math.min(1f, u * 3f);
                d.box(sp[o], sp[o + 1], sp[o + 2], s, s * 0.8f, s, null, spColor[k], DrawList.MATTE);
            }
        }
    }

    // ---- drawing ----

    private final float[] turn = new float[9], spin = new float[9];

    void draw(DrawList d, Runner r, float eyeX, float eyeZ) {
        for (Piece p : pieces) piece(d, p, 1f);
        drawSpecks(d);
        if (old.size() > 0) {
            float k = Math.max(0f, 1f - oldT / 0.5f);
            for (Piece p : old) piece(d, p, k);
        }
        if (hs != HAND) {
            Body m = mallet;
            rot(m, turn);
            Mallet.draw(d, turn, m.px, m.py, m.pz, Toss.SCALE, Mallet.HEAD_MIDDLE);
        }
    }

    private static void rot(Body b, float[] out) {
        out[0] = b.r00; out[1] = b.r01; out[2] = b.r02;
        out[3] = b.r10; out[4] = b.r11; out[5] = b.r12;
        out[6] = b.r20; out[7] = b.r21; out[8] = b.r22;
    }

    /** A piece where its body is, so big (shrinking away, an old heap gone). */
    private void piece(DrawList d, Piece p, float k) {
        Body b = p.b;
        // not yet on its way down: up the slope, out of sight
        if (!p.landed && (phase < FALL || t < p.at)) return;
        rot(b, turn);
        float hx = b.hx * k, hy = b.hy * k, hz = b.hz * k;
        switch (p.kind) {
            case LOG:
                // round, end on to her: bark, and its sawn ends pale with a darker heart
                round(d, b, 2, hx, hz, k, p);
                break;
            case BEAM:
                round(d, b, 0, hy, hx, k, p);
                break;
            case STUMP:
                round(d, b, 1, hx, hy, k, p);
                // a root or two out at the foot
                at(d, b, hx * 0.55f, -hy * 0.75f, 0f, hx * 0.45f, hy * 0.2f, hz * 0.3f, p.color, DrawList.WOOD);
                at(d, b, 0f, -hy * 0.75f, -hz * 0.55f, hx * 0.3f, hy * 0.2f, hz * 0.45f, p.color, DrawList.WOOD);
                break;
            case POST:
                round(d, b, 1, hx, hy, k, p);
                break;
            case PLANK:
                d.box(b.px, b.py, b.pz, hx, hy, hz, turn, p.color, DrawList.material(DrawList.WOOD, 30 + b.id * 17 % 150));
                // the boards it is made of: a darker line between them
                at(d, b, 0f, 0f, 0f, hx + 0.005f, hy * 0.7f, 0.012f, Street.scale(p.color, 0.7f), DrawList.WOOD);
                break;
            case SLAB:
                d.box(b.px, b.py, b.pz, hx, hy, hz, turn, p.color, DrawList.STONE);
                at(d, b, 0f, hy * 0.98f, 0f, hx * 0.92f, 0.03f, hz * 0.92f, p.cap, DrawList.STONE);
                break;
            case STONE:
            case BOULDER: {
                d.box(b.px, b.py, b.pz, hx, hy * 0.86f, hz, turn, p.color, DrawList.STONE);
                // its corners broken off: a second block turned an eighth about the upright
                float c = 0.7071f;
                for (int i = 0; i < 9; i++) spin[i] = turn[i];
                spin[0] = turn[0] * c - turn[2] * c;
                spin[2] = turn[0] * c + turn[2] * c;
                spin[3] = turn[3] * c - turn[5] * c;
                spin[5] = turn[3] * c + turn[5] * c;
                spin[6] = turn[6] * c - turn[8] * c;
                spin[8] = turn[6] * c + turn[8] * c;
                float s = Math.min(hx, hz) * 0.7f;
                d.box(b.px, b.py, b.pz, s, hy, s, spin, p.cap, DrawList.STONE);
                if (p.kind == BOULDER) {
                    // and turned across the other way too: rounder
                    d.box(b.px, b.py, b.pz, hx * 0.8f, hy, hz * 0.8f, turn, Street.scale(p.color, 0.96f), DrawList.STONE);
                    d.box(b.px, b.py, b.pz, hx * 0.86f, hy * 0.7f, hz * 1.04f, turn, p.color, DrawList.STONE);
                }
                break;
            }
            default:
                // clay: a lump, its top slumped and darker
                d.box(b.px, b.py, b.pz, hx, hy, hz, turn, p.color, DrawList.MATTE);
                at(d, b, 0f, hy * 0.6f, 0f, hx * 0.8f, hy * 0.45f, hz * 0.8f, p.cap, DrawList.MATTE);
                break;
        }
        // the piece that holds it up: a band of warm light round it, faint, breathing; to be found, not shown
        if (p == key && phase == PLAY && !down) {
            float glow = 0.5f + 0.5f * (float) Math.sin(t * 3.1f);
            int c = Street.mix(0xC8902E, 0xFFD45A, glow);
            float bx = hx + 0.015f, by = Math.min(hy, 0.09f), bz = hz + 0.015f;
            if (p.kind == POST) {
                bx = hx + 0.015f;
                bz = hz + 0.015f;
                by = 0.05f;
            }
            at(d, b, 0f, p.kind == POST ? -hy * 0.35f : 0f, 0f, bx, by, bz, c, DrawList.GLOW);
        }
    }

    private final float[] band = new float[9];

    /**
     * A log or a stump: eight sides round its own axis (0, 1, 2: x, y, z), so far out, so long
     * each way; its ends sawn, pale, the heart darker. Bands crossed at an eighth of a turn.
     */
    private void round(DrawList d, Body b, int axis, float r, float len, float k, Piece p) {
        int u = axis == 0 ? 1 : 0, v = axis == 2 ? 1 : 2;
        float side = r * 0.4142f, bark = DrawList.material(DrawList.TRUNK, 40 + b.id * 23 % 160);
        for (int q = 0; q < 2; q++) {
            float c = q == 0 ? 1f : 0.7071f, s = q == 0 ? 0f : 0.7071f;
            // the frame turned about the axis: its two other columns turned so far
            for (int row = 0; row < 3; row++) {
                band[row * 3 + axis] = turn[row * 3 + axis];
                band[row * 3 + u] = turn[row * 3 + u] * c + turn[row * 3 + v] * s;
                band[row * 3 + v] = -turn[row * 3 + u] * s + turn[row * 3 + v] * c;
            }
            for (int w = 0; w < 2; w++) {
                float hu = w == 0 ? r : side, hv = w == 0 ? side : r;
                float bx = axis == 0 ? len : u == 0 ? hu : hv, by = axis == 1 ? len : u == 1 ? hu : hv, bz = axis == 2 ? len : v == 2 ? hv : hu;
                d.box(b.px, b.py, b.pz, bx, by, bz, band, p.color, bark);
                // the sawn ends, a little in from the bark
                for (int e = -1; e <= 1; e += 2) {
                    if (axis == 1 && e < 0) continue;
                    float off = e * (len + 0.012f * k);
                    float ox = band[axis] * off, oy = band[3 + axis] * off, oz = band[6 + axis] * off;
                    float cx = axis == 0 ? 0.02f * k : bx * 0.88f, cy = axis == 1 ? 0.02f * k : by * 0.88f, cz = axis == 2 ? 0.02f * k : bz * 0.88f;
                    d.box(b.px + ox, b.py + oy, b.pz + oz, cx, cy, cz, band, p.cap, DrawList.WOOD);
                }
            }
        }
        // the heart at each end
        for (int e = -1; e <= 1; e += 2) {
            if (axis == 1 && e < 0) continue;
            float off = e * (len + 0.03f * k), hr = r * 0.22f;
            float ox = turn[axis] * off, oy = turn[3 + axis] * off, oz = turn[6 + axis] * off;
            d.box(b.px + ox, b.py + oy, b.pz + oz, axis == 0 ? 0.01f : hr, axis == 1 ? 0.01f : hr, axis == 2 ? 0.01f : hr, turn,
                    Street.scale(p.cap, 0.78f), DrawList.WOOD);
        }
    }

    /** A box at (ox, oy, oz) in the piece's own frame, turned with it. */
    private void at(DrawList d, Body b, float ox, float oy, float oz, float hx, float hy, float hz, int color, int material) {
        float x = b.px + turn[0] * ox + turn[1] * oy + turn[2] * oz;
        float y = b.py + turn[3] * ox + turn[4] * oy + turn[5] * oz;
        float z = b.pz + turn[6] * ox + turn[7] * oy + turn[8] * oz;
        d.box(x, y, z, hx, hy, hz, turn, color, material);
    }

    /** The heap's top, where its button floats, into out (x, y, z). */
    void top(float[] out) {
        float top = 0f;
        for (Piece p : pieces) if (p.landed) top = Math.max(top, p.b.maxY);
        out[0] = px;
        out[1] = Math.max(2f, top) + 1.6f;
        out[2] = pz;
    }

    /** Over the frame: the ring, and the way the mallet would go from it. */
    void flat(Px f, Runner r) {
        if (!ready() || !ring(ring)) return;
        float ringX = ring[0], ringY = ring[1];
        target(ringX, ringY, aimAt);
        r.bone("hand_r", hand);
        float sx = hand[0], sy = hand[1], sz = hand[2];
        float dx = aimAt[0] - sx, dy = aimAt[1] - sy, dz = aimAt[2] - sz;
        float tt = Math.max(0.3f, (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / SPEED);
        float vx = dx / tt, vy = dy / tt - 0.5f * Physics.G * tt, vz = dz / tt;
        for (int i = 3; i < 20; i++) {
            float q = tt * i / 21f;
            if (project(sx + vx * q, sy + vy * q + 0.5f * Physics.G * q * q, sz + vz * q, scr)) {
                int x = Math.round(scr[0]), y = Math.round(scr[1]);
                f.rect(x - 1, y - 1, x + 1, y + 1, 0xCCFFF4D0);
            }
        }
        ring(f, ringX, ringY, 9f, 0xFFFFD45A);
        ring(f, ringX, ringY, 10.5f, 0x88232A4A);
    }

    private final float[] ring = new float[3];

    private static void ring(Px f, float cx, float cy, float r, int c) {
        int n = (int) (r * 7f);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            int x = (int) Math.round(cx + Math.cos(a) * r), y = (int) Math.round(cy + Math.sin(a) * r);
            f.set(x, y, c);
            f.set(x + 1, y, c);
        }
    }
}
