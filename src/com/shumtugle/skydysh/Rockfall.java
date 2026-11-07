package com.shumtugle.skydysh;

import java.util.ArrayList;
import java.util.Random;

/**
 * The rockfall in the pass: after the rains the slopes came down across the road, and the pass
 * is blocked by a heap of what fell, logs, stumps, posts, stones, a boulder or two and lumps of
 * clay, lying every which way. She clears it as she likes: blows of the mallet (the big pieces
 * split under them, the rest fly off), shoving with her own weight (she is stone: what is not too
 * heavy goes before her), the pickup driven into it. Once a way through as wide as the pickup is
 * open from one side of the heap to the other, the road to the village is open.
 */
final class Rockfall implements Physics.Listener {
    // the kinds of piece
    static final int LOG = 0, STUMP = 1, STONE = 2, CLAY = 3, BEAM = 4, PLANK = 5, SLAB = 6, BOULDER = 7, POST = 8;
    // the heap's layers: so many, each so much narrower and shallower than the one under it
    static final int LAYERS = 5;
    // a piece struck so hard by another splits in two; a blow of the mallet splits those so long
    // and more (half); never past so many pieces in all
    static final float SPLIT_CRASH = 9f, SPLIT_LONG = 0.6f, SPLIT_MIN = 0.22f;
    static final int MOST = 140;
    // her weight against a piece: it goes before her so fast, the heavier the slower
    static final float SHOVE = 3.8f, SHOVE_MASS = 800f;
    // a blow of the mallet on foot: what lies within so much of her is knocked off so fast
    static final float BLOW_REACH = 2.4f, BLOW = 6.2f, BLOW_MASS = 3000f;
    // the way through: so wide at least, nothing in it taller than so much, across so deep a stretch
    static final float LANE = 2.8f, LOW = 0.55f, DEPTH = 9f;

    /** Where the heap lies: its middle on the road in the pass. */
    final float px, pz;
    final Physics world = new Physics();
    private final ArrayList<Piece> pieces = new ArrayList<Piece>();
    private World.Host host;

    /** One piece of the heap: its body, kind, colours. */
    private static final class Piece {
        Body b;
        int kind, color, cap;
        // given up its raised inertia (struck, split): it topples freely
        boolean free;
    }

    Rockfall(long seed) {
        pz = City.PASS_Z;
        px = City.routeX(pz);
        world.listener = this;
        // the rock faces of the pass, either side
        for (int e = -1; e <= 1; e += 2) {
            Body face = new Body(4f, 10f, 30f, Body.PLINTH, 0);
            face.setPose(px + e * (City.CLIFF_FOOT + 4f), 10f, pz, 1f, 0f, 0f, 0f);
            world.add(face);
        }
        build(new Random(seed));
        for (Piece p : pieces) world.add(p.b);
    }

    /** How many pieces it is in now. */
    int count() {
        return pieces.size();
    }

    // ---- the heap: layer on layer, each narrower, pieces of every kind turned every way ----

    private void build(Random rnd) {
        float y = 0f;
        ArrayList<float[]> taken = new ArrayList<float[]>();
        for (int l = 0; l < LAYERS; l++) {
            float k = 1f - l / (float) LAYERS;
            float hx = (City.CLIFF_FOOT - 0.9f) * (0.35f + 0.65f * k), hz = 3.2f * (0.3f + 0.7f * k);
            int tries = l == LAYERS - 1 ? 3 : Math.round(70 * k);
            for (int t = 0; t < tries; t++) {
                float x = px + (rnd.nextFloat() * 2f - 1f) * hx, z = pz + (rnd.nextFloat() * 2f - 1f) * hz;
                int kind = l == LAYERS - 1 || rnd.nextInt(14) == 0 ? BOULDER : pick(rnd);
                float[] size = size(rnd, kind);
                float yaw = rnd.nextFloat() * 6.283f;
                float r = (float) Math.sqrt(size[0] * size[0] + size[2] * size[2]);
                // not into one already laid, in this layer or standing up into it from under
                boolean clear = true;
                float bottom = y + 0.03f, top = bottom + size[1] * 2f;
                for (float[] o : taken) {
                    float dx = o[0] - x, dz = o[1] - z, m = o[2] + r + 0.04f;
                    if (dx * dx + dz * dz < m * m && o[3] < top && o[4] > bottom) {
                        clear = false;
                        break;
                    }
                }
                if (!clear) continue;
                taken.add(new float[]{x, z, r, bottom, top});
                put(rnd, kind, x, y + size[1] + 0.03f, z, size[0], size[1], size[2], yaw);
            }
            y += 0.95f;
        }
    }

    private static int pick(Random rnd) {
        int[] kinds = {LOG, LOG, LOG, BEAM, STUMP, STUMP, STONE, STONE, STONE, CLAY, CLAY, POST, PLANK, SLAB};
        return kinds[rnd.nextInt(kinds.length)];
    }

    /** A piece's half sizes for its kind: x, y, z. */
    private static float[] size(Random rnd, int kind) {
        float u = rnd.nextFloat();
        switch (kind) {
            case LOG: {
                float r = 0.22f + u * 0.12f;
                return new float[]{r, r, 0.8f + rnd.nextFloat() * 0.6f};
            }
            case BEAM: {
                float r = 0.2f + u * 0.1f;
                return new float[]{1.0f + rnd.nextFloat() * 0.6f, r, r};
            }
            case POST: {
                // a post fallen: lying along its length, round
                float r = 0.16f + u * 0.06f;
                return new float[]{r, r, 1.1f + rnd.nextFloat() * 0.5f};
            }
            case STUMP: {
                float r = 0.32f + u * 0.12f;
                return new float[]{r, 0.3f + rnd.nextFloat() * 0.15f, r};
            }
            case PLANK:
                return new float[]{0.9f + u * 0.5f, 0.07f, 0.28f};
            case SLAB:
                return new float[]{0.5f + u * 0.3f, 0.14f, 0.45f + rnd.nextFloat() * 0.2f};
            case BOULDER: {
                float r = 0.75f + u * 0.4f;
                return new float[]{r, r * 0.85f, r * 0.92f};
            }
            case STONE:
                return new float[]{0.32f + u * 0.18f, 0.26f + rnd.nextFloat() * 0.12f, 0.34f + rnd.nextFloat() * 0.18f};
            default:
                return new float[]{0.3f + u * 0.16f, 0.24f + rnd.nextFloat() * 0.1f, 0.32f + rnd.nextFloat() * 0.14f};
        }
    }

    /** One piece, of a kind, its middle at (x, y, z), so big (half sizes), turned so far about the upright. */
    private Piece put(Random rnd, int kind, float x, float y, float z, float hx, float hy, float hz, float yaw) {
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
        p.kind = kind == POST ? LOG : kind;
        p.color = color;
        p.cap = cap;
        p.b = new Body(hx, hy, hz, material, color);
        if (kind == CLAY) p.b.scaleMass(0.8f);
        p.b.setPose(x, y, z, (float) Math.cos(yaw * 0.5f), 0f, (float) Math.sin(yaw * 0.5f), 0f);
        p.b.savePrev();
        pieces.add(p);
        return p;
    }

    // ---- a step ----

    /** The heap a step on, while she or the pickup is near enough to see it. */
    void update(float dt, Pickup driving, Runner r, World.Host h) {
        host = h;
        float x = driving != null ? driving.x : r.x, z = driving != null ? driving.z : r.z;
        if (Math.abs(x - px) < 70f && Math.abs(z - pz) < 110f) {
            world.step(Math.min(dt, 1f / 30f));
            tame();
            afterStep();
        }
        specks(dt);
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

    // ---- the way through ----

    /**
     * Whether the road is open: across the road over the heap's stretch a gap as wide as LANE with
     * nothing in it standing higher than LOW (what is lower the pickup goes over).
     */
    boolean open() {
        float edge = City.ROUTE_HALF + 0.8f;
        float[] lo = new float[pieces.size()], hi = new float[pieces.size()];
        int n = 0;
        for (Piece p : pieces) {
            Body b = p.b;
            if (Math.abs(b.pz - pz) > DEPTH || b.maxY < LOW) continue;
            float mid = City.routeX(b.pz), a = b.minX - mid, c = b.maxX - mid;
            if (c < -edge || a > edge) continue;
            lo[n] = a;
            hi[n] = c;
            n++;
        }
        // the gaps between what stands, sorted across
        for (int i = 1; i < n; i++)
            for (int j = i; j > 0 && lo[j] < lo[j - 1]; j--) {
                float t = lo[j]; lo[j] = lo[j - 1]; lo[j - 1] = t;
                t = hi[j]; hi[j] = hi[j - 1]; hi[j - 1] = t;
            }
        float at = -edge;
        gap = 0f;
        for (int i = 0; i < n; i++) {
            gap = Math.max(gap, lo[i] - at);
            at = Math.max(at, hi[i]);
        }
        gap = Math.max(gap, edge - at);
        standing = n;
        return gap >= LANE;
    }

    /** The widest way through, as last measured; how many pieces stood in the road then. */
    float gap;
    int standing;

    // ---- what the pickup and she do to it ----

    /** The pickup about to move on: what it is going into fast enough is shoved off ahead and aside. */
    void ram(Pickup pk, float dt) {
        if (Math.abs(pk.speed) < 2.5f) return;
        float sg = Math.signum(pk.speed), fx = (float) Math.sin(pk.heading) * sg, fz = (float) Math.cos(pk.heading) * sg;
        float v = Math.abs(pk.speed), reach = Pickup.ENDS + v * dt;
        float cx = pk.x + fx * reach, cz = pk.z + fz * reach;
        for (Piece p : pieces) {
            Body b = p.b;
            if (b.py > 2.5f) continue;
            float r = Math.max(b.hx, b.hz);
            float dx = b.px - cx, dz = b.pz - cz, m = r + Pickup.END_R + 0.3f, dd = dx * dx + dz * dz;
            if (dd >= m * m) continue;
            float l = (float) Math.sqrt(dd) + 1e-4f;
            float push = Math.max(v * 0.9f, 4f) * Math.min(1f, (float) Math.sqrt(BLOW_MASS / b.mass)), across = dx * fz - dz * fx;
            float side = Math.abs(across) > 0.2f ? Math.signum(across) : (b.id % 2 == 0 ? 1f : -1f);
            b.px += dx / l * (m - l) * 0.5f;
            b.pz += dz / l * (m - l) * 0.5f;
            b.updateR();
            world.wake(b);
            free(p);
            b.vx = fx * push * 0.45f + fz * side * push * 0.8f;
            b.vz = fz * push * 0.45f - fx * side * push * 0.8f;
            b.vy = 2f;
            b.wy = across / l * push * 0.3f;
            hit++;
        }
    }

    /** How many times something has been struck by the pickup or the mallet in hand. */
    int hit;

    /**
     * A blow of the mallet on foot at (bx, bz), facing (fx, fz): what lies within reach before her
     * is knocked off, the heavier the slower; the long pieces split under it.
     */
    void strike(float bx, float bz, float fx, float fz) {
        Piece big = null;
        float bigD = 1e9f;
        for (int i = pieces.size() - 1; i >= 0; i--) {
            Piece p = pieces.get(i);
            Body b = p.b;
            float dx = b.px - bx, dz = b.pz - bz, dd = (float) Math.sqrt(dx * dx + dz * dz);
            if (dd > Math.max(b.hx, b.hz) + BLOW_REACH || dx * fx + dz * fz < -0.3f || b.minY > 3.2f) continue;
            float l = dd + 1e-4f, k = BLOW * Math.min(1f, (float) Math.sqrt(BLOW_MASS / b.mass));
            world.wake(b);
            free(p);
            // off the way she faces and out from her, and off to the road's side it lies on
            float side = aside(b);
            b.vx = fx * k * 0.6f + dx / l * k * 0.6f + side * k * 1.3f;
            b.vz = fz * k * 0.7f + dz / l * k * 0.7f;
            b.vy = 2.5f;
            dust(b.px, b.py, b.pz, p.color, 5, true);
            if (dd < bigD && Math.max(b.hx, Math.max(b.hy, b.hz)) >= SPLIT_LONG) {
                bigD = dd;
                big = p;
            }
            hit++;
        }
        // the nearest long piece splits under the blow
        if (big != null) split(big);
    }

    /**
     * What lies on the road, in her way and the pickup's (see City.Extra): each piece low enough, a
     * round. Pressed against, a piece goes before what presses it: she is stone, and heavy.
     */
    public boolean push(float[] q, float rr, float feet) {
        boolean any = false;
        for (Piece p : pieces) {
            Body b = p.b;
            if (feet > b.maxY - 0.15f || b.minY > feet + 1.6f) continue;
            float r = (Math.max(b.hx, b.hz) + Math.min(b.hx, b.hz)) * 0.5f;
            float dx = q[0] - b.px, dz = q[1] - b.pz, m = rr + r, dd = dx * dx + dz * dz;
            if (dd >= m * m || dd < 1e-8f) continue;
            float l = (float) Math.sqrt(dd);
            // the piece shoved off the way it is pressed, unless it goes so already
            float ux = -dx / l, uz = -dz / l, k = SHOVE * Math.min(1f, SHOVE_MASS / b.mass);
            // and off toward the road's side it lies on, so that what she goes through is left behind her
            ux += aside(b) * 1.6f;
            float ul = (float) Math.sqrt(ux * ux + uz * uz);
            ux /= ul;
            uz /= ul;
            if (k > 0.3f && b.vx * ux + b.vz * uz < k) {
                world.wake(b);
                free(p);
                b.vx = ux * k;
                b.vz = uz * k;
                b.vy = Math.max(b.vy, 0.6f);
                b.wx += uz * k * 0.6f;
                b.wz -= ux * k * 0.6f;
            }
            q[0] = b.px + dx / l * m;
            q[1] = b.pz + dz / l * m;
            any = true;
        }
        return any;
    }

    /** Which way across is off the road for a piece: toward the side of the road it lies on. */
    private static float aside(Body b) {
        float d = b.px - City.routeX(b.pz);
        return Math.abs(d) < 0.15f ? (b.id % 2 == 0 ? 1f : -1f) : Math.signum(d);
    }

    @Override
    public void knock(Body a, Body b, float speed, float x, float y, float z) {
        if (speed < 1.2f) return;
        float v = Math.min(1f, speed / 9f);
        if (host != null) {
            int s = a.material == Body.STONE || b != null && b.material == Body.STONE ? Synth.STONE : Synth.WOOD;
            host.sound(s, 0.2f + 0.5f * v, 0.7f + 0.3f * ((a.id * 37 % 10) / 10f));
        }
        if (speed > 3f) {
            Piece p = pieceOf(a);
            if (p != null) dust(x, y, z, p.color, 1 + Math.min(4, (int) (speed * 0.4f)), speed > 6f);
            if (speed > SPLIT_CRASH) {
                if (p != null && !toSplit.contains(p.b)) toSplit.add(p.b);
                Piece q = pieceOf(b);
                if (q != null && !toSplit.contains(q.b)) toSplit.add(q.b);
            }
        }
    }

    private final ArrayList<Body> toSplit = new ArrayList<Body>();

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

    /** After a step: the pieces that crashed hard enough, split. */
    private void afterStep() {
        for (int i = 0; i < toSplit.size() && i < 4; i++) split(pieceOf(toSplit.get(i)));
        toSplit.clear();
    }

    /** A piece split in two along its length (a round one along its own axis), the halves flying apart. */
    private void split(Piece p) {
        if (p == null || pieces.size() >= MOST) return;
        Body b = p.b;
        int ax;
        switch (p.kind) {
            case LOG: ax = 2; break;
            case BEAM: case PLANK: ax = 0; break;
            case STUMP: ax = 1; break;
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
        for (int e = -1; e <= 1; e += 2) {
            Body n = new Body(ax == 0 ? h * 0.5f : b.hx, ax == 1 ? h * 0.5f : b.hy, ax == 2 ? h * 0.5f : b.hz, b.material, b.tint);
            float o = e * h * 0.5f;
            n.setPose(b.px + dx * o, b.py + dy * o, b.pz + dz * o, b.qw, b.qx, b.qy, b.qz);
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

    static final int SPECKS = 300;
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
            sp[o + 5] = (float) Math.sin(a) * sp0;
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
        float dx = px - eyeX, dz = pz - eyeZ;
        if (dx * dx + dz * dz > 400f * 400f) return;
        for (Piece p : pieces) piece(d, p);
        drawSpecks(d);
    }

    private static void rot(Body b, float[] out) {
        out[0] = b.r00; out[1] = b.r01; out[2] = b.r02;
        out[3] = b.r10; out[4] = b.r11; out[5] = b.r12;
        out[6] = b.r20; out[7] = b.r21; out[8] = b.r22;
    }

    /** A piece where its body is. */
    private void piece(DrawList d, Piece p) {
        Body b = p.b;
        float k = 1f;
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

}
