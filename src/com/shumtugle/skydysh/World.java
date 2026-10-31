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
    static final String VERSION = "0.22";    // shown on screen; keep in step with versionName
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
        /** The language the game is to speak, by its two-letter code. */
        String language();
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
    Folder folder;                           // the mission now, when it is the folder's
    private boolean again;                   // the folder's mission once more, the rivals were first
    final Toss toss = new Toss();            // her hammer
    private Opening opening;                 // what the game opens with, while it plays
    private Dispatch dispatch;               // the dispatcher's room, while she is in it
    private float overFor;                   // how long since the last of the five was caught
    // kept between games, saved when a mission is done
    static final String SAVE_MISSION = "mission", SAVE_PLATES = "plates", SAVE_MONEY = "money";
    int plates, money;
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
        Lang.use(host, host.language());
        // where the last game left off: the mission to play next, and what has been won
        int next = host.loadInt(SAVE_MISSION);
        if (next > 1) mission.round = next - 1;
        plates = host.loadInt(SAVE_PLATES);
        money = host.loadInt(SAVE_MONEY);
        begin(0f, 0f);
        opening = new Opening(host);
    }

    /** The next mission, round (x, z): five golems loose, or, every other time, the folder. */
    private void begin(float x, float z) {
        int n = mission.round + 1;
        if (Folder.its(n)) {
            mission.round = n;
            folder = new Folder(0xF01DL + n * 7919L + (again ? 31L : 0L));
            folder.start(x, z, city);
            chase = folder.rivals[0];
        } else {
            folder = null;
            mission.start(x, z);
            chase = mission.target(null);
        }
    }

    /** The golems of the mission now. */
    Chase[] golems() {
        return folder != null ? folder.rivals : mission.golems;
    }

    /** Whether the mission now is done with, won or not. */
    boolean over() {
        if (folder == null) return mission.over();
        return folder.theirs || folder.hers && runner.landed;
    }

    /** The briefing for the mission now. */
    private String[] briefing() {
        return again ? Dispatch.again(mission.round) : Dispatch.briefing(mission.round);
    }

    /** The report on the mission just done. */
    private String[] report() {
        if (folder == null) return Dispatch.report(mission.round, clock(mission.doneAt));
        if (folder.hers) return Dispatch.paid(mission.round, clock(folder.doneAt), money);
        return Dispatch.lost(mission.round, clock(folder.doneAt));
    }

    /** A sum of money with its thousands set apart: 100 000. */
    static String money(int m) {
        String s = String.valueOf(m), out = "";
        for (int k = s.length(); k > 0; k -= 3) out = s.substring(Math.max(0, k - 3), k) + (out.isEmpty() ? "" : " " + out);
        return out;
    }

    /** The opening done with at once (the desktop stand, for scenes of the game itself). */
    void skipOpening() {
        opening = null;
        dispatch = null;
        Frida.eyes = 1f;
    }

    /** The mission done: the next one to play and what has been won, kept for the next game. */
    private void save() {
        boolean lost = folder != null && folder.theirs;
        if (folder == null) plates += Mission.FIVE;
        else if (folder.hers) money += Folder.REWARD;
        // lost, the same mission is played again
        host.saveInt(SAVE_MISSION, lost ? mission.round : mission.round + 1);
        host.saveInt(SAVE_PLATES, plates);
        host.saveInt(SAVE_MONEY, money);
    }

    /** The dispatcher's room now, with the briefing or (done) the report (the desktop stand). */
    void standDispatch(boolean done) {
        opening = null;
        Frida.eyes = 1f;
        runner.update(0f, city, cars, walkers);
        dispatch = new Dispatch(host, done ? report() : briefing());
    }

    /** Mission n now, round where she is (the desktop stand). */
    void standMission(int n) {
        mission.round = n - 1;
        begin(runner.x, runner.z);
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
                // out of the white into the street, and the room in her head closing round it, the
                // first mission on its screen
                opening = null;
                Frida.eyes = 1f;
                whiteOut = WHITE_OUT;
                runner.update(0f, city, cars, walkers);      // the eye put behind her
                dispatch = new Dispatch(host, briefing());
            }
            return;
        }
        if (dispatch != null) {
            // the street stands still while she is in her head
            whiteOut = Math.max(0f, whiteOut - dt);
            dispatch.update(dt);
            if (dispatch.done()) {
                // the walls drawn back: the street goes on (the next mission, after a report)
                dispatch = null;
                overFor = 0f;
            }
            return;
        }
        if (over() && (overFor += dt) > BACK_TO_ROOM) {
            // indoors when it ends: out at the front door first, the room in her head is out of doors
            if (indoors != null) {
                swapTo = TO_STREET;
                swap();
                swapT = -1f;
            }
            save();
            String[] said = report();
            // lost, the same again, as its briefing says; won, the next; the report, then the briefing
            again = folder != null && folder.theirs;
            if (again) mission.round--;
            begin(runner.x, runner.z);
            dispatch = new Dispatch(host, said).then(briefing());
            again = false;
            overFor = 0f;
            return;
        }
        whiteOut = Math.max(0f, whiteOut - dt);
        doors(dt);
        if (swapT < 0f) runner.update(dt, city, cars, walkers);
        int round = mission.round;
        if (folder != null) {
            boolean had = folder.hers;
            folder.update(dt, runner, city, cars);
            if (folder.hers && !had) took();
            Chase n = folder.nearest();
            chase = n != null ? n : chase;
        } else {
            mission.update(dt, runner, city, cars);
            chase = mission.target(chase);
        }
        toss.update(dt, runner, golems(), city, runner.tapped());
        cars.update(dt, runner, golems());
        walkers.update(dt, runner, chase);
        int caught = mission.caught(), struck = 0;
        if (folder != null) caught = 0;
        for (Chase g : golems()) struck += g.struck;
        if (round != mission.round) {
            caughtSeen = 0;
            nextSlot = 0;
            struckSeen = struck;
        }
        if (folder == null && caught != caughtSeen) {
            caughtSeen = caught;
            host.buzz(caught == Mission.FIVE ? 160 : 60);
        }
        if (struck != struckSeen) {
            struckSeen = struck;
            host.buzz(30);
        }
        fliers.update(dt, true);
    }

    /** The folder in her hands: over the edge of the roof to the street, down along it, gliding. */
    private void took() {
        host.buzz(120);
        host.sound(Synth.LEATHER, 0.7f, 1.1f);
        Folder f = folder;
        float half = Math.abs(f.frontX) * f.houseHX + Math.abs(f.frontZ) * f.houseHZ;
        float ex = f.houseX + f.frontX * (half + 0.6f), ez = f.houseZ + f.frontZ * (half + 0.6f);
        // the edge straight out from where she is along the front, and then round along the street
        float along = (runner.x - f.houseX) * f.frontZ - (runner.z - f.houseZ) * f.frontX;
        ex += f.frontZ * along * 0.6f;
        ez -= f.frontX * along * 0.6f;
        float ax = f.frontZ, az = -f.frontX;
        if (ax * (float) Math.sin(runner.heading) + az * (float) Math.cos(runner.heading) < 0f) {
            ax = -ax;
            az = -az;
        }
        runner.auto(ex, ez, (float) Math.atan2(ax, az));
    }

    // ---- doors: in from the street (or the yard), or down from a roof, a place indoors; out again ----
    static final float SWAP = 0.3f;          // the dark coming down at a door, and lifting again
    static final int TO_HALL = 1, TO_CABIN = 2, TO_ROOF = 3, TO_STREET = 4, TO_YARD = 5;
    /** The place indoors she is in; null out of doors. */
    Indoors indoors;
    private float swapT = -1f;
    private int swapTo;
    private boolean liftArmed = true;
    private float liftWait;                  // how long she has stood at a door
    // near a door: so near; standing so long there, or coming at it, it takes her in;
    // running by faster than this, along the front, it does not
    static final float LIFT_NEAR = 1.7f, LIFT_STAND = 0.4f, LIFT_PAST = 5f;
    private final float[] liftNow = new float[11], houseNow = new float[8];
    private final float[] liftOf = new float[11], houseOf = new float[8], doorAt = new float[11];

    /**
     * The doors: at the door of a house with a lift, or the hut on its roof, coming to it or
     * standing at it, she goes in (not running by); indoors, the place has its say; between, a
     * moment of dark while one is swapped for the other.
     */
    private void doors(float dt) {
        Runner r = runner;
        if (swapT >= 0f) {
            float was = swapT;
            swapT += dt;
            if (was < SWAP && swapT >= SWAP) swap();
            if (swapT >= 2f * SWAP) swapT = -1f;
            return;
        }
        if (indoors != null) {
            int e = indoors.update(dt, r, host);
            if (e == Indoors.UP) go(TO_ROOF);
            else if (e == Indoors.OUT_FRONT) go(TO_STREET);
            else if (e == Indoors.OUT_BACK) go(TO_YARD);
            return;
        }
        if (r.inAir() || r.throwing() >= 0f || (folder != null && folder.hers)) return;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        boolean near = city.liftNear(r.x, r.z, r.y, 3f, liftNow, houseNow);
        if (!near) {
            liftArmed = true;
            return;
        }
        if (!liftArmed) return;
        float[] l = liftNow;
        boolean street = r.y < 1f;
        float px = street ? l[0] : l[8], pz = street ? l[1] : l[9];
        float wx = street ? -l[2] : l[2], wz = street ? -l[3] : l[3];
        float dx = px - r.x, dz = pz - r.z;
        if (dx * dx + dz * dz > LIFT_NEAR * LIFT_NEAR) {
            liftWait = 0f;
            return;
        }
        // at the door: coming at it, or standing there a moment; running past, not
        boolean at = fx * wx + fz * wz > 0.3f && r.speed > 0.5f;
        liftWait = r.speed < 1.5f ? liftWait + dt : 0f;
        if (!(at && r.speed < LIFT_PAST || at && fx * wx + fz * wz > 0.7f || liftWait > LIFT_STAND)) return;
        liftWait = 0f;
        System.arraycopy(l, 0, liftOf, 0, 11);
        System.arraycopy(houseNow, 0, houseOf, 0, 8);
        host.sound(Synth.DOOR, 0.45f, 1f);
        go(street ? TO_HALL : TO_CABIN);
    }

    private void go(int to) {
        swapTo = to;
        swapT = 0f;
    }

    /** In the dark: the town for a place indoors, or the other way. */
    private void swap() {
        Runner r = runner;
        float[] l = liftOf;
        long seed = ((long) Math.floor(houseOf[0] * 4f) * 73856093L) ^ ((long) Math.floor(houseOf[1] * 4f) * 19349663L);
        switch (swapTo) {
            case TO_HALL:
                indoors = Rooms.hall(houseOf, seed);
                indoors.enter(r, l[10] > 0.5f);
                break;
            case TO_CABIN:
                indoors = Rooms.hall(houseOf, seed);
                indoors.comeDown(r);
                break;
            case TO_ROOF:
                leave();
                r.put(l[4], l[5], l[6], l[7]);
                host.sound(Synth.DOOR, 0.35f, 1.2f);
                break;
            default: {
                // out at the front onto the pavement, or at the back into the yard, facing away from the door
                leave();
                City.lift(houseOf, 0, doorAt);
                float nx = doorAt[2], nz = doorAt[3], px = doorAt[0], pz = doorAt[1];
                if (swapTo == TO_YARD) {
                    float half = Math.abs(nx) * houseOf[2] + Math.abs(nz) * houseOf[3];
                    px -= nx * (2f * half + 1.4f);
                    pz -= nz * (2f * half + 1.4f);
                    nx = -nx;
                    nz = -nz;
                }
                r.put(px, swapTo == TO_YARD ? city.ground(px, pz) : City.WALK_TOP, pz, (float) Math.atan2(nx, nz));
                host.sound(Synth.DOOR, 0.35f, 1.1f);
            }
        }
        r.recenter();
        // the eye put where it is to be at once, behind her in the new place
        r.update(0f, city, cars, walkers);
    }

    /** Out of doors again. */
    private void leave() {
        indoors = null;
        runner.room = null;
        runner.held = false;
        liftArmed = false;
    }

    /** The dark of a door over the frame, coming down and lifting. */
    private void swapFlat(Px f) {
        if (swapT < 0f) return;
        float dark = swapT < SWAP ? swapT / SWAP : Math.max(0f, 1f - (swapT - SWAP) / SWAP);
        int a = Math.round(255f * Math.min(1f, dark));
        for (int i = 0; i < f.a.length; i++) {
            int old = f.a[i], oa = old >>> 24;
            if (oa == 0) f.a[i] = (a << 24) | 0x0B0A0E;
            else f.a[i] = Px.mix(old, 0xFF0B0A0E, dark);
        }
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

    /**
     * Over the frame, the folder's mission: how far off it is, in meters, at the top; a green ring
     * round it when it is in sight, and at the edge of the frame a green mark the way it is while
     * it is not; the reward and the
     * clock in one corner; in the other, for each rival, a burning mark and how far it still has
     * to go, rising as it climbs. Taken, the reward large in the middle; lost, a word for it.
     */
    private void folderFlat(DrawList d, Px f) {
        int fire = 0xFFF0581C, ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        Folder fo = folder;
        String clock = clock(fo.over() ? fo.doneAt : fo.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        String sum = money(Folder.REWARD);
        shadowed(f, sum, W - PixelFont.width(sum, 1) - 8, 32, 1, gold, ink);
        // the rivals: a mark each, alight while it is about, and what it has still to go
        for (int k = 0; k < Folder.RIVALS; k++) {
            Chase g = fo.rivals[k];
            int y = 12 + k * 13;
            boolean about = !g.done();
            f.disc(14, y + 3, 4.5f, ink);
            f.disc(14, y + 3, 3.5f, about ? fire : 0xFF3A4660);
            if (!about) continue;
            String m = Math.round(fo.toGo(g)) + "";
            shadowed(f, m, 22, y, 1, g.y > 1f ? gold : cream, ink);
        }
        if (fo.over() && fo.hers) {
            String big = "+" + sum;
            shadowed(f, big, (W - PixelFont.width(big, 4)) / 2, h / 2 - 40, 4, gold, ink);
            return;
        }
        if (fo.theirs) {
            String w = Lang.lines("lost.short", mission.round, null)[0];
            shadowed(f, w, (W - PixelFont.width(w, 3)) / 2, h / 2 - 40, 3, fire, ink);
            return;
        }
        if (fo.hers) return;
        String m = String.valueOf(Math.round(fo.apart(runner)));
        shadowed(f, m, (W - PixelFont.width(m, 3)) / 2, 14, 3, cream, ink);
        // where it is: a green ring round it in sight, a green mark at the edge out of sight
        boolean seen = d.project(fo.x, fo.y + 0.3f, fo.z, at) && at[0] > 6 && at[0] < W - 6 && at[1] > 30 && at[1] < h - 6;
        if (seen) {
            // the ring breathes a little, so the eye finds it
            float rr = 6.5f + 1.2f * (float) Math.sin(time * 5f);
            ring(f, at[0], at[1], rr + 1f, rr - 3f, ink);
            ring(f, at[0], at[1], rr, rr - 2f, green);
            return;
        }
        float ax = (float) Math.sin(runner.eyeYaw), az = (float) Math.cos(runner.eyeYaw);
        float vx = fo.x - runner.x, vz = fo.z - runner.z;
        float ang = (float) Math.atan2(vx * az - vz * ax, vx * ax + vz * az);
        float ex = (float) Math.sin(ang), ey = -(float) Math.cos(ang);
        // kept clear of the rivals' marks in the corner above
        float k = Math.min((W * 0.5f - 12f) / Math.max(1e-3f, Math.abs(ex)), (h * 0.5f - 60f) / Math.max(1e-3f, Math.abs(ey)));
        float px = W * 0.5f + ex * k, py = h * 0.5f + ey * k;
        f.disc(px, py, 6.5f, ink);
        f.disc(px, py, 5.5f, green);
        f.disc(px, py, 2.2f, 0xFFD9FFE6);
    }

    /** A ring over the frame round (cx, cy): between radii in and out, in one colour. */
    private static void ring(Px f, float cx, float cy, float out, float in, int c) {
        int r0 = (int) Math.ceil(out);
        float oo = out * out + out * 0.6f, ii = in * in + in * 0.6f;
        for (int y = -r0; y <= r0; y++)
            for (int x = -r0; x <= r0; x++) {
                int q = x * x + y * y;
                if (q <= oo && q > ii) f.rect(Math.round(cx) + x, Math.round(cy) + y, Math.round(cx) + x + 1, Math.round(cy) + y + 1, c);
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

    /** A frame indoors: the place and her in it, the eye behind her or held across the hall; over it, the mission's marks. */
    private void drawIndoors(DrawList d) {
        Runner r = runner;
        indoors.light(d);
        java.util.Arrays.fill(d.flat, 0);
        if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
        Px f = flatPx;
        d.hudRows = 0;
        if (indoors.shot(eyeNow)) d.camera(eyeNow[0], eyeNow[1], eyeNow[2], eyeNow[3], eyeNow[4], W * 1.25f, 0.03f, 80f);
        else d.camera(r.eyeX, r.eyeY, r.eyeZ, r.eyeYaw, r.eyePitch, W * 1.2f, 0.03f, 80f);
        indoors.draw(d);
        if (!r.hidden) {
            r.draw(d);
            toss.draw(d, r);
        }
        if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        swapFlat(f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        meter(f);
    }

    /** While the engine is young: how long a frame takes, in milliseconds, and how finely it is drawn. */
    private void meter(Px f) {
        if (frameMs <= 0f) return;
        int qs = Math.round(drawScale * 100f);
        String m = Math.round(frameMs) + " " + (qs / 100) + "." + (qs % 100 < 10 ? "0" : "") + (qs % 100);
        PixelFont.draw(f, m, 3, h - 10, 1, 0x88FFFFFF);
    }

    static final float WHITE_OUT = 0.35f;
    private final float[] eyeNow = new float[5];

    void draw(DrawList d) {
        d.begin(W, h);
        d.time = time;
        if (opening != null) {
            java.util.Arrays.fill(d.flat, 0);
            if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
            d.hudRows = 0;
            opening.draw(d, flatPx, h);
            return;
        }
        Runner r = runner;
        if (indoors != null) {
            drawIndoors(d);
            return;
        }
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
        // the eye: behind her, or in the room in her head, or on its way between
        float eyeX = r.eyeX, eyeY = r.eyeY, eyeZ = r.eyeZ, eyeYaw = r.eyeYaw, eyePitch = r.eyePitch;
        if (dispatch != null) {
            dispatch.eye(r, eyeNow);
            eyeX = eyeNow[0];
            eyeY = eyeNow[1];
            eyeZ = eyeNow[2];
            eyeYaw = eyeNow[3];
            eyePitch = eyeNow[4];
        }
        d.camera(eyeX, eyeY, eyeZ, eyeYaw, eyePitch, W * 1.6f, 0.03f, 500f);
        fliers.strokes(d);
        float ex = (float) Math.sin(eyeYaw), ez = (float) Math.cos(eyeYaw);
        city.draw(d, eyeX, eyeZ, ex, ez);
        cars.draw(d, eyeX, eyeZ, ex, ez);
        walkers.draw(d, eyeX, eyeZ, ex, ez);
        if ((dispatch == null || dispatch.showsHer()) && !r.hidden) {
            r.draw(d);
            toss.draw(d, r);
        }
        if (folder != null) folder.draw(d, r);
        // the one nearest that still burns gives off light and glow; the others only burn
        Chase lit = null;
        Chase[] golems = golems();
        for (Chase g : golems) if (g.burning() && (lit == null || g.apart < lit.apart)) lit = g;
        for (Chase g : golems) {
            float gx = g.x - eyeX, gz = g.z - eyeZ;
            if (gx * ex + gz * ez < -6f || gx * gx + gz * gz > 200f * 200f) continue;
            g.draw(d, g == lit);
        }
        if (dispatch != null) dispatch.draw(d, f, h);
        else if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        swapFlat(f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // the white the opening ended on, going off
        if (whiteOut > 0f) Opening.wash(f, whiteOut / WHITE_OUT);
        meter(f);
    }
}
