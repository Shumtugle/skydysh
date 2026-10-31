package com.shumtugle.skydysh;

/**
 * The game, apart from the phone: what is where, what moves, what the frame should hold.
 * It never draws: each frame it fills a DrawList, and a renderer draws that. The shell
 * calls, always from one thread: touch() for each queued touch, update(dt), then draw().
 * Coordinates of touches are frame pixels, W across.
 *
 * A southern town by the sea on a summer evening, its streets grown without end as they are
 * come to (see City), its cars going about it (see Cars) and its people out for the evening
 * (see Walkers), and Freedie running through it (see Runner), after her double on fire (see
 * Chase).
 */
final class World {
    static final int W = 274;                // frame units across; the height follows the screen
    static final String VERSION = "0.16";    // shown on screen; keep in step with versionName
    static final int DOWN = 0, MOVE = 1, UP = 2, BACK = 3;

    /** What the world may ask of the phone. */
    interface Host {
        /** Plays one of the synthesizer's sounds, so loud, at this share of its own pitch. */
        void sound(int id, float gain, float rate);
        /** A short buzz of the phone, so many milliseconds. */
        void buzz(int ms);
        /** A small number kept between games, by name; 0 when never kept. */
        void saveInt(String key, int value);
        int loadInt(String key);
        /** A file shipped with the game, or null. */
        byte[] asset(String name);
        /** A note written down: kept as a plain text by this name, in a folder of its own. */
        void journal(String name, String text);
    }

    private final Host host;
    int h = 640;
    float time;
    float frameMs;                           // how long the last frame took, set by the shell
    float drawScale;                         // drawn pixels to a frame pixel, set by the shell
    final City city = new City(0x5EE7L);
    final Runner runner = new Runner();
    final Mission mission = new Mission(0x60E3L);
    Chase chase;                             // the nearest of them still loose
    final Toss toss = new Toss();            // her hammer
    private Opening opening;                 // what the game opens with, while it plays
    private Dispatch dispatch;               // the dispatcher's room, while she is in it
    private float overFor;                   // how long since the last of the five was caught
    static final float BACK_TO_ROOM = 3f;
    private float whiteOut;                  // the white of the opening going off the game, seconds left
    final Cars cars = new Cars();
    final Walkers walkers = new Walkers();
    private int caughtSeen, struckSeen;
    private final float[] at = new float[2];
    private final Fliers fliers = new Fliers();                     // far off in the sky, seldom
    private Px flatPx;

    World(Host host) {
        this.host = host;
        byte[] moves = host.asset("moves.bin");
        if (moves != null) Frida.use(Moves.read(moves));
        mission.start(0f, 0f);
        chase = mission.target(null);
        opening = new Opening(host);
    }

    /** The opening done with at once (the desktop stand, for scenes of the game itself). */
    void skipOpening() {
        opening = null;
        dispatch = null;
        Frida.eyes = 1f;
    }

    /** The dispatcher's room now, with the briefing or (done) the report (the desktop stand). */
    void standDispatch(boolean done) {
        opening = null;
        Frida.eyes = 1f;
        dispatch = new Dispatch(host, done ? Dispatch.report(mission.round, clock(mission.doneAt)) : Dispatch.briefing(mission.round), true);
    }

    /** The opening from its start (the desktop stand). */
    void startOpening() {
        opening = new Opening(host);
    }

    void resize(int screenW, int screenH) {
        h = Math.max(1, Math.round(W * (float) screenH / Math.max(1, screenW)));
    }

    void touch(int type, float x, float y) {
        if (type == BACK) return;
        if (opening != null) {
            if (type == DOWN) opening.skip();
            return;
        }
        if (dispatch != null) {
            if (type == DOWN) dispatch.touch();
            return;
        }
        runner.touch(type, x, y);
    }

    void update(float dt) {
        time += dt;
        if (opening != null) {
            opening.update(dt);
            if (opening.done) {
                // out of the white into the dispatcher's room, the first mission on its set
                opening = null;
                Frida.eyes = 1f;
                dispatch = new Dispatch(host, Dispatch.briefing(mission.round), true);
            }
            return;
        }
        if (dispatch != null) {
            dispatch.update(dt);
            if (dispatch.done()) {
                // through the glass into the street: the mission begins (the next one, after a report)
                dispatch = null;
                if (mission.over()) {
                    mission.start(runner.x, runner.z);
                    chase = mission.target(null);
                }
                overFor = 0f;
                whiteOut = WHITE_OUT;
            }
            return;
        }
        if (mission.over() && (overFor += dt) > BACK_TO_ROOM) {
            dispatch = new Dispatch(host, Dispatch.report(mission.round, clock(mission.doneAt)), true);
            overFor = 0f;
            return;
        }
        whiteOut = Math.max(0f, whiteOut - dt);
        runner.update(dt, city, cars, walkers);
        int round = mission.round;
        mission.update(dt, runner, city, cars);
        toss.update(dt, runner, mission, city);
        chase = mission.target(chase);
        cars.update(dt, runner, mission.golems);
        walkers.update(dt, runner, chase);
        int caught = mission.caught(), struck = 0;
        for (Chase g : mission.golems) struck += g.struck;
        if (round != mission.round) {
            caughtSeen = 0;
            nextSlot = 0;
            struckSeen = struck;
        }
        if (caught != caughtSeen) {
            caughtSeen = caught;
            host.buzz(caught == Mission.FIVE ? 160 : 60);
        }
        if (struck != struckSeen) {
            struckSeen = struck;
            host.buzz(30);
        }
        fliers.update(dt, true);
    }

    /** Whether the back key means something here, rather than leaving the game. */
    boolean wantsBack() {
        return false;
    }

    /** For the desktop stand: a flock in the sky now (1), or a plane that has flown so many seconds. */
    void standFliers(int what, float seconds) {
        if (what == 1) fliers.standFlock();
        else fliers.standPlane(seconds);
    }

    /**
     * Over the frame: the five plates, alight as each is knocked off, in the corner; the clock in the other;
     * how far off the nearest one still loose is, in meters, at the top; and for every one out
     * of sight a burning mark at the edge the way it is, smaller the further. When the last is
     * caught, the count and the time large in the middle.
     */
    private void chaseFlat(DrawList d, Px f) {
        int fire = 0xFFF0581C, ink = 0xFF232A4A, cream = 0xFFF4E8D2;
        int got = mission.caught();
        String c = got + "/" + Mission.FIVE;
        // the plates knocked off, alight in their slots once they have flown there; the empty slots dark
        int slots = 0;
        for (Chase g : mission.golems) {
            if (g.done() && g.slot < 0) g.slot = nextSlot++;
            if (g.slot >= 0 && g.sinceTaken() >= PLATE_POP + PLATE_FLY) slots |= 1 << g.slot;
        }
        for (int k = 0; k < Mission.FIVE; k++) {
            boolean lit = (slots & (1 << k)) != 0;
            plate(f, 13 + k * 13, 18, 5f, 1f, lit ? 0xFF5FE0C4 : 0xFF3A4660, lit ? 0xFF1E3A40 : 0xFF2A3350, ink);
        }
        // a plate just knocked off: up off the brow, then across the frame into its slot
        for (Chase g : mission.golems) {
            float t = g.sinceTaken();
            if (t < 0f || t >= PLATE_POP + PLATE_FLY || g.slot < 0) continue;
            g.plateAt(at3);
            float sx = W * 0.5f, sy = h * 0.3f;
            if (d.project(at3[0], at3[1], at3[2], at)) {
                sx = at[0];
                sy = at[1];
            }
            float u = Math.max(0f, (t - PLATE_POP) / PLATE_FLY);
            float e = u * u * (3f - 2f * u);
            float tx = 13 + g.slot * 13, ty = 18;
            float px = sx + (tx - sx) * e, py = sy + (ty - sy) * e - (float) Math.sin(Math.PI * e) * 30f;
            // big as it leaves her, small as it lands; turning over as it flies
            float size = (t < PLATE_POP ? 4f + 14f * t / PLATE_POP : 18f) * (1f - e) + 5f * e;
            float flip = Math.abs((float) Math.cos(g.plateSpin() * 0.5f)) * (1f - e) + e;
            plate(f, px, py, size, Math.max(0.15f, flip), 0xFF8FF5DF, 0xFF1E3A40, ink);
        }
        String clock = clock(mission.over() ? mission.doneAt : mission.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (mission.over()) {
            int tw = PixelFont.width(c, 6);
            shadowed(f, c, (W - tw) / 2, h / 2 - 60, 6, fire, ink);
            tw = PixelFont.width(clock, 4);
            shadowed(f, clock, (W - tw) / 2, h / 2, 4, cream, ink);
            return;
        }
        String m = String.valueOf(Math.round(chase.apart));
        int tw = PixelFont.width(m, 3);
        shadowed(f, m, (W - tw) / 2, 14, 3, cream, ink);
        for (Chase g : mission.golems) {
            if (g.done()) continue;
            boolean seen = d.project(g.x, g.y + 1.5f, g.z, at)
                    && at[0] > 6 && at[0] < W - 6 && at[1] > 30 && at[1] < h - 6;
            if (seen) continue;
            // the way to it, from the middle of the frame, cut at its edge
            float ax = (float) Math.sin(runner.eyeYaw), az = (float) Math.cos(runner.eyeYaw);
            float vx = g.x - runner.x, vz = g.z - runner.z;
            float across = vx * az - vz * ax, along = vx * ax + vz * az;
            float ang = (float) Math.atan2(across, along);
            float ex = (float) Math.sin(ang), ey = -(float) Math.cos(ang);
            float cx = W * 0.5f, cy = h * 0.5f, k = Math.min((W * 0.5f - 12f) / Math.max(1e-3f, Math.abs(ex)),
                    (h * 0.5f - 34f) / Math.max(1e-3f, Math.abs(ey)));
            float px = cx + ex * k, py = cy + ey * k;
            float rr = 3.5f + 2.5f * Math.max(0f, Math.min(1f, 1f - (g.apart - 40f) / 360f));
            f.disc(px, py, rr + 1.5f, ink);
            f.disc(px, py, rr, fire);
            f.disc(px + ex * rr * 0.33f, py + ey * rr * 0.33f, rr * 0.42f, 0xFFFFD45A);
        }
    }

    static final float PLATE_POP = 0.45f, PLATE_FLY = 0.75f;
    private int nextSlot;
    private final float[] at3 = new float[3];

    /** A plate over the frame: so wide either side of (x, y), squeezed to flip across, its sign down the middle. */
    private static void plate(Px f, float x, float y, float half, float flip, int face, int sign, int ink) {
        float hw = half * flip, hh = half * 0.62f;
        int x0 = Math.round(x - hw), x1 = Math.round(x + hw), y0 = Math.round(y - hh), y1 = Math.round(y + hh);
        f.rect(x0 - 1, y0 - 1, x1 + 1, y1 + 1, ink);
        f.rect(x0, y0, x1, y1, face);
        int sw = Math.max(1, Math.round(hw * 0.2f));
        f.rect(Math.round(x) - sw, y0 + 1, Math.round(x) + sw, y1 - 1, sign);
    }

    private static void shadowed(Px f, String s, int x, int y, int scale, int color, int shadow) {
        PixelFont.draw(f, s, x + 1, y + 1, scale, shadow);
        PixelFont.draw(f, s, x, y, scale, color);
    }

    /** Seconds as minutes and seconds, m:ss. */
    private static String clock(float seconds) {
        int s = (int) seconds;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    static final float WHITE_OUT = 0.35f;

    void draw(DrawList d) {
        d.begin(W, h);
        d.time = time;
        if (opening != null || dispatch != null) {
            java.util.Arrays.fill(d.flat, 0);
            if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
            d.hudRows = 0;
            if (opening != null) opening.draw(d, flatPx, h);
            else dispatch.draw(d, flatPx, h);
            return;
        }
        Runner r = runner;
        d.fogStart = 8f;
        d.fogScale = 150f;
        // the sun's depth map laid round the ground she is heading into
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        Daylight.light(d, 1f);
        // the town is open on every side: the sun higher than over the old street, from the left
        // and behind, so a street running away from it is lit down its length
        double e = Math.toRadians(40), t = Math.toRadians(-140);
        d.sun((float) (Math.cos(e) * Math.sin(t)), (float) Math.sin(e), (float) (Math.cos(e) * Math.cos(t)),
                r.x + fx * 30f, 6f, r.z + fz * 30f, 110f, 70f, 170f);
        java.util.Arrays.fill(d.flat, 0);
        if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
        Px f = flatPx;
        d.hudRows = 0;
        d.camera(r.eyeX, r.eyeY, r.eyeZ, r.eyeYaw, r.eyePitch, W * 1.6f, 0.1f, 500f);
        fliers.strokes(d);
        float ex = (float) Math.sin(r.eyeYaw), ez = (float) Math.cos(r.eyeYaw);
        city.draw(d, r.eyeX, r.eyeZ, ex, ez);
        cars.draw(d, r.eyeX, r.eyeZ, ex, ez);
        walkers.draw(d, r.eyeX, r.eyeZ, ex, ez);
        r.draw(d);
        toss.draw(d, r);
        // the one nearest that still burns gives off light and glow; the others only burn
        Chase lit = null;
        for (Chase g : mission.golems) if (g.burning() && (lit == null || g.apart < lit.apart)) lit = g;
        for (Chase g : mission.golems) {
            float gx = g.x - r.eyeX, gz = g.z - r.eyeZ;
            if (gx * ex + gz * ez < -6f || gx * gx + gz * gz > 200f * 200f) continue;
            g.draw(d, g == lit);
        }
        chaseFlat(d, f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // the white the opening ended on, going off
        if (whiteOut > 0f) Opening.wash(f, whiteOut / WHITE_OUT);
        // while the engine is young: how long a frame takes, in milliseconds, and how finely it is drawn
        if (frameMs > 0f) {
            int qs = Math.round(drawScale * 100f);
            String meter = Math.round(frameMs) + " " + (qs / 100) + "." + (qs % 100 < 10 ? "0" : "") + (qs % 100);
            PixelFont.draw(f, meter, 3, h - 10, 1, 0x88FFFFFF);
        }
    }
}
