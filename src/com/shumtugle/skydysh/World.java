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
    static final String VERSION = "0.54";    // shown on screen; keep in step with versionName
    static final int DOWN = 0, MOVE = 1, UP = 2, BACK = 3, LEFT = 4;

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
    Delivery delivery;                       // the mission now, when it is a delivery
    private static final Chase[] NONE = new Chase[0];
    // in a delivery no brum is about: one far off and still, for those who keep an eye on the nearest
    private final Chase nobody = new Chase(1L, 100000, 100000, 0);
    private int beatsWere = Runner.BEATS;    // her beats left, as the last frame left them
    private boolean again;                   // the folder's mission once more, the rivals were first
    final Toss toss = new Toss();            // her hammer
    private Opening opening;                 // what the game opens with, while it plays
    private Dispatch dispatch;               // the dispatcher's room, while she is in it
    private float overFor;                   // how long since the last of the five was caught
    // kept between games, saved when a mission is done
    static final String SAVE_MISSION = "mission", SAVE_PLATES = "plates", SAVE_MONEY = "money", SAVE_CONTROLS = "foot";
    int plates, money;
    static final float BACK_TO_ROOM = 3f;
    private float whiteOut;                  // the white of the opening going off the game, seconds left
    final Cars cars = new Cars();
    final Walkers walkers = new Walkers();
    final Pets pets = new Pets();            // dogs on leads and on their own, cats
    final Duty duty = new Duty();            // the duty pickups at the curbs
    /** The pickup she is driving; null on foot. */
    Pickup driving;
    private int caughtSeen, struckSeen;
    private boolean lateSeen, swimSeen;
    private final float[] at = new float[2];
    private final Fliers fliers = new Fliers();                     // far off in the sky, seldom
    private Px flatPx;

    World(Host host) {
        this.host = host;
        cars.duty = duty;
        cars.city = city;
        byte[] moves = host.asset("moves.bin");
        if (moves != null) Frida.use(Moves.read(moves));
        if (FolkPack.town == null) FolkPack.town = FolkPack.read(host.asset("folk.bin"));
        Lang.use(host, host.language());
        // where the last game left off: the mission to play next, and what has been won
        int next = host.loadInt(SAVE_MISSION);
        if (next > 1) mission.round = next - 1;
        // the way to play on foot: one finger, unless two thumbs are offered and were chosen (kept as 2)
        Runner.classic = !TWO_THUMBS_OFFERED || host.loadInt(SAVE_CONTROLS) != 2;
        plates = host.loadInt(SAVE_PLATES);
        money = host.loadInt(SAVE_MONEY);
        // every game begins at home: on the promenade across the shore road from its door, facing it
        runner.put(City.HOME_DOOR_X, City.WALK_TOP, City.SHORE * City.CELL - 5.5f, 0f);
        begin(runner.x, runner.z);
        opening = new Opening(host, city);
    }

    /** The next mission, round (x, z): in turn five brums loose, the folder, a delivery. */
    private void begin(float x, float z) {
        if (driving != null) getOut();
        free = false;
        lost = false;
        // where it began, for a try again to begin there too
        startX = x;
        startZ = z;
        startHome = indoors != null && indoors.home;
        int n = mission.round + 1;
        delivery = null;
        if (Delivery.its(n)) {
            mission.round = n;
            folder = null;
            delivery = new Delivery();
            chase = nobody;
        } else if (Folder.its(n)) {
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

    private int thrownSeen, struckBySeen;
    private final float[] flyingAt = new float[3];

    /**
     * The town minds the mallet: those it flies past, people, dogs and cats, are off at a run, and the
     * cars drive off fast; a blow struck, or one thrown, frightens those about her too.
     */
    private void frighten() {
        if (toss.thrown != thrownSeen || toss.struck != struckBySeen) {
            thrownSeen = toss.thrown;
            struckBySeen = toss.struck;
            walkers.alarm(runner.x, runner.z, 7f);
            pets.alarm(runner.x, runner.z, 8f);
            cars.alarm(runner.x, runner.z, 10f);
        }
        if (toss.flying(flyingAt)) {
            walkers.alarm(flyingAt[0], flyingAt[2], 4.5f);
            pets.alarm(flyingAt[0], flyingAt[2], 5f);
            if (cars.alarm(flyingAt[0], flyingAt[2], 5f) > 0) host.sound(Synth.ALARM, 0.4f, 1f);
        }
    }

    /** Home: in her head the screen asks which mission now; the one now goes on if none is picked. */
    private void atHome() {
        if (driving != null) getOut();
        errand = NO_ERRAND;
        runner.stopLead();
        host.sound(Synth.DOOR, 0.45f, 1f);
        // out again, she stands where every game begins: on the promenade across from the door
        runner.put(City.HOME_DOOR_X, City.WALK_TOP, City.SHORE * City.CELL - 5.5f, 0f);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        dispatch = new Dispatch(host, Dispatch.home(free ? mission.round + 1 : mission.round)).choices(Dispatch.HOME_PICKS);
        letGo();
    }

    /**
     * Mission of a kind (the hunt 1, the folder 2, the delivery 3) begun now where she is: the next
     * number of that kind on from the one now, or the one now again, if it is of that kind (free,
     * on from the one last played).
     */
    private void chooseMission(int kind) {
        int n = Math.max(1, free ? mission.round + 1 : mission.round);
        while ((Delivery.its(n) ? 3 : Folder.its(n) ? 2 : 1) != kind) n++;
        mission.round = n - 1;
        again = false;
        // what was left lying about from the one now: no load waits on any pickup
        for (Pickup p : duty.all) p.load = 0;
        begin(runner.x, runner.z);
        overFor = 0f;
        caughtSeen = 0;
        nextSlot = 0;
        struckSeen = 0;
        host.saveInt(SAVE_MISSION, n);
    }

    /** For the desktop stand: home's choice on the screen, as if she had come in at its door. */
    void standHome() {
        atHome();
    }

    /** For the desktop stand: a touch on the line of home's choice that picks c; false if it is not on the screen. */
    boolean standPick(int c) {
        float[] at = new float[2];
        if (dispatch == null || !dispatch.rowOf(c, at)) return false;
        // a first touch may only type the rest
        for (int k = 0; k < 2 && dispatch.choosing(); k++) {
            touch(DOWN, 0, at[0], at[1]);
            touch(UP, 0, at[0], at[1]);
        }
        return true;
    }

    /**
     * No mission now: the last one done with, the town hers till she goes home for the next. The
     * mission's number is the one last played (one less, if it is to be played again).
     */
    boolean free;
    /** The mission last played was lost: the one offered at home is the same again. */
    private boolean lost;
    /** The report read, she is to be brought home onto the sofa. */
    private boolean homeAfter;
    /** Where the mission now began: home's room, or out of doors at (x, z). */
    private float startX, startZ;
    private boolean startHome;
    static final String SAVE_BEST = "best.";

    /** How long the mission just done took, seconds. */
    private float doneTime() {
        if (delivery != null) return delivery.doneAt;
        if (folder != null) return folder.doneAt;
        return mission.doneAt;
    }

    /**
     * Mission n done in so many seconds: kept as its best if it is (in tenths of a second); the
     * best before, seconds (0: none before).
     */
    private float record(int n, float seconds) {
        int was = host.loadInt(SAVE_BEST + n), now = Math.max(1, Math.round(seconds * 10f));
        if (was <= 0 || now < was) host.saveInt(SAVE_BEST + n, now);
        return was / 10f;
    }

    /** Home, after a mission: in home's room, sitting on the sofa, where the next is offered. */
    private void bringHome() {
        if (driving != null) getOut();
        errand = NO_ERRAND;
        runner.stopLead();
        if (indoors != null) leave();
        indoors = Rooms.home(City.HOME_DOOR_X, City.HOME_DOOR_Z);
        indoors.enter(runner, false);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        whiteOut = WHITE_OUT;
        sitDown();
        letGo();
    }

    /** The brums of the mission now. */
    Chase[] brums() {
        if (free || delivery != null) return NONE;
        return folder != null ? folder.rivals : mission.brums;
    }

    /** Whether the mission now is a hunt. */
    boolean hunt() {
        return !free && folder == null && delivery == null;
    }

    /** Whether the mission now is done with, won or not. */
    boolean over() {
        if (delivery != null) return delivery.over();
        if (folder == null) return mission.over();
        return folder.theirs || folder.hers && runner.landed;
    }

    /** The briefing for the mission now. */
    private String[] briefing() {
        return again ? Dispatch.again(mission.round) : Dispatch.briefing(mission.round);
    }

    /** The report on the mission just done. */
    private String[] report() {
        if (delivery != null) return Dispatch.delivered(mission.round, clock(delivery.doneAt), money);
        if (folder == null && mission.late) return Dispatch.late(mission.round, mission.caught());
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
        Frida.eyeSize = 1f;
        Frida.eyeHeat = 0f;
    }

    /** The mission done: the next one to play and what has been won, kept for the next game. */
    private void save() {
        boolean lost = hunt() ? mission.late : folder != null && folder.theirs;
        if (delivery != null) money += Delivery.REWARD;
        else if (folder == null) plates += mission.caught();
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
        Frida.eyeSize = 1f;
        Frida.eyeHeat = 0f;
        runner.update(0f, city, cars, walkers);
        dispatch = new Dispatch(host, done ? report() : briefing());
    }

    /** No mission now, the town hers (the desktop stand). */
    void standFree() {
        free = true;
        folder = null;
        delivery = null;
        chase = nobody;
    }

    /** Mission n now, round where she is (the desktop stand). */
    void standMission(int n) {
        mission.round = n - 1;
        begin(runner.x, runner.z);
    }

    /** For the desktop stand: her at the wheel at (x, z), facing so: the pickup she drives moved there, or a duty pickup put there. */
    void standDrive(float x, float z, float heading) {
        Pickup p = driving;
        if (p == null) {
            p = new Pickup(x, z, heading, 0x5747L);
            duty.all.add(p);
        }
        p.x = x;
        p.z = z;
        p.heading = heading;
        p.speed = 0f;
        p.y = city.ground(x, z);
        if (driving == null) getIn(p);
        drive(0f);
        runner.recenter();
    }

    /** The opening from its start (the desktop stand). */
    void startOpening() {
        opening = new Opening(host, city);
    }

    void resize(int screenW, int screenH) {
        h = Math.max(1, Math.round(W * (float) screenH / Math.max(1, screenW)));
    }

    /** A touch by the one finger (the desktop stand's scenes): the finger numbered 0. */
    void touch(int type, float x, float y) {
        touch(type, 0, x, y);
    }

    // ---- the fingers: each down is the stick's, the eye's, or a button's till it is lifted ----
    static final int NOBODY = 0, STICK = 1, LOOK = 2, BUTTONED = 3, WHEEL_FINGER = 4, GAS_FINGER = 5, BRAKE_FINGER = 6;
    private static final int FINGERS = 10;
    private final int[] role = new int[FINGERS];
    private final float[] fingerX = new float[FINGERS], fingerY = new float[FINGERS], fingerAt = new float[FINGERS],
            fingerMoved = new float[FINGERS], flickY = new float[FINGERS], flickAt = new float[FINGERS];
    private final boolean[] flicked = new boolean[FINGERS];
    // the stick: where its middle is (it comes down where the thumb does, and follows it out past
    // its rim), and where the thumb is; how far out its rim is, frame pixels
    static final float STICK_R = 34f, KNOB = 15f;
    private float stickCX, stickCY, stickTX, stickTY;
    private int stickFinger = -1;
    // a tap on the eye's side, and a flick there
    static final float TAP_S = 0.22f, TAP_PX = 14f, FLICK_PX = 26f, FLICK_S = 0.16f;
    // a button just pressed, lit a moment: which, and when
    private int pressedButton = -1;
    private float pressedAt = -9f;

    void touch(int type, int id, float x, float y) {
        if (type == BACK) return;
        if (type == LEFT) {
            // the game left and come back to: paused, every finger let go
            if (opening == null && dispatch == null) paused = true;
            letGo();
            return;
        }
        if (id < 0 || id >= FINGERS) return;
        if (opening != null) {
            if (type == DOWN) opening.skip();
            return;
        }
        if (dispatch != null) {
            if (type == DOWN && dispatch.choosing()) {
                int c = dispatch.pickAt(x, y);
                if (c > 0 && c < Dispatch.REST) {
                    chooseMission(c);
                    dispatch.then(briefing());
                } else if (c == 0 && free) {
                    // on to the next, whatever its kind; lost, the same again, from where it began
                    again = lost;
                    if (lost && !startHome) runner.put(startX, City.WALK_TOP, startZ, runner.heading);
                    begin(runner.x, runner.z);
                    overFor = 0f;
                    caughtSeen = 0;
                    nextSlot = 0;
                    struckSeen = 0;
                    dispatch.then(briefing());
                    again = false;
                }
            } else if (type == DOWN) dispatch.touch();
            return;
        }
        if (seated()) {
            if (type == DOWN && sofaAsked) standUp();
            leadFinger = -1;
            return;
        }
        if (Runner.classic) {
            // the first finger down leads; any other only presses what is under it
            if (type == DOWN) {
                if (leadFinger < 0) {
                    leadFinger = id;
                    classicTouch(DOWN, x, y);
                } else otherFinger(x, y);
            } else if (id == leadFinger) {
                classicTouch(type, x, y);
                if (type == UP) leadFinger = -1;
            }
            return;
        }
        if (type == DOWN) down(id, x, y);
        else if (type == MOVE) moved(id, x, y);
        else if (type == UP) lifted(id, x, y);
    }

    private void down(int id, float x, float y) {
        role[id] = BUTTONED;
        fingerX[id] = x;
        fingerY[id] = y;
        fingerAt[id] = time;
        fingerMoved[id] = 0f;
        flickY[id] = y;
        flickAt[id] = time;
        flicked[id] = false;
        // paused: the way to play changed, or a touch anywhere plays on
        if (paused) {
            if (onToggle(x, y)) toggle();
            else paused = false;
            role[id] = NOBODY;
            return;
        }
        if (onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            letGoAll();
            return;
        }
        if (driving != null) {
            if (onLiftButton(x, y)) {
                getOut();
                return;
            }
            if (wheelFinger >= 0) return;
            wheelFinger = id;
            role[id] = WHEEL_FINGER;
            drivingTouch(DOWN, x, y);
            return;
        }
        // a sign: she goes where it says by herself
        if (indoors == null) {
            int sgn = signAt(x, y);
            if (sgn >= 0) {
                errand(sgn);
                return;
            }
        }
        errand = NO_ERRAND;
        if (liftOffer && onLiftButton(x, y)) {
            liftPressed = true;
            return;
        }
        // her own buttons, under the right thumb
        int b = footButton(x, y);
        if (b >= 0) {
            press(b);
            return;
        }
        if (x < W * 0.5f && stickFinger < 0) {
            role[id] = STICK;
            stickFinger = id;
            // never so near the edge that the stick has no room to be pushed that way
            stickCX = Math.max(STICK_R + 4f, Math.min(W * 0.5f - STICK_R * 0.5f, x));
            stickCY = Math.max(h * 0.3f, Math.min(h - STICK_R - 6f, y));
            stickTX = x;
            stickTY = y;
            runner.stick((x - stickCX) / STICK_R, (y - stickCY) / STICK_R, true);
            return;
        }
        role[id] = LOOK;
    }

    private void moved(int id, float x, float y) {
        float dx = x - fingerX[id], dy = y - fingerY[id];
        fingerMoved[id] += Math.abs(dx) + Math.abs(dy);
        switch (role[id]) {
            case STICK: {
                stickTX = x;
                stickTY = y;
                // past its rim, the stick's middle follows the thumb out
                float ox = x - stickCX, oy = y - stickCY, m = (float) Math.sqrt(ox * ox + oy * oy);
                if (m > STICK_R) {
                    stickCX = x - ox / m * STICK_R;
                    stickCY = y - oy / m * STICK_R;
                }
                runner.stick((x - stickCX) / STICK_R, (y - stickCY) / STICK_R, true);
                break;
            }
            case LOOK:
                runner.look(dx);
                // a flick up or down on the eye's side: a jump, a slide
                if (flicked[id]) {
                    if (Math.abs(y - flickY[id]) < 3f) flicked[id] = false;
                    flickY[id] = y;
                    flickAt[id] = time;
                } else if (time - flickAt[id] > FLICK_S) {
                    flickY[id] = y;
                    flickAt[id] = time;
                } else if (flickY[id] - y > FLICK_PX && Math.abs(dx) < Math.abs(dy) * 2f + 2f) {
                    runner.jumpButton();
                    flicked[id] = true;
                    flickY[id] = y;
                } else if (y - flickY[id] > FLICK_PX && Math.abs(dx) < Math.abs(dy) * 2f + 2f) {
                    runner.slideButton();
                    flicked[id] = true;
                    flickY[id] = y;
                }
                break;
            case WHEEL_FINGER:
                drivingTouch(MOVE, x, y);
                break;
            default:
        }
        fingerX[id] = x;
        fingerY[id] = y;
    }

    private void lifted(int id, float x, float y) {
        switch (role[id]) {
            case STICK:
                runner.stick(0f, 0f, false);
                stickFinger = -1;
                break;
            case LOOK:
                if (time - fingerAt[id] < TAP_S && fingerMoved[id] < TAP_PX) runner.tap();
                break;
            case WHEEL_FINGER:
                drivingTouch(UP, x, y);
                wheelFinger = -1;
                break;
            case GAS_FINGER:
                gasHeld = false;
                break;
            case BRAKE_FINGER:
                brakeHeld = false;
                break;
            default:
        }
        role[id] = NOBODY;
    }

    /** Every finger let go of at once: paused, in or out of a car. */
    private void letGoAll() {
        for (int i = 0; i < FINGERS; i++) role[i] = NOBODY;
        stickFinger = -1;
        runner.stick(0f, 0f, false);
        if (wheelFinger >= 0) {
            drivingTouch(UP, 0f, 0f);
            wheelFinger = -1;
        }
        gasHeld = brakeHeld = false;
    }

    /**
     * Whatever the fingers were doing, forgotten, those still on the glass heard no more till they
     * are lifted: when she gets into the pickup or out of it, goes into her head, is left. A finger
     * held running does not become the throttle.
     */
    private void letGo() {
        letGoAll();
        pedal = false;
        wheelIn = brakeIn = 0f;
        if (classicDown) {
            runner.touch(UP, 0f, 0f);
            buttonHeld = true;
            classicDown = false;
        }
    }

    // ---- the way to play, chosen while paused: one finger or two thumbs ----
    /** Two thumbs, offered under the pause or not: for now not, one finger plays, the rest kept for later. */
    static final boolean TWO_THUMBS_OFFERED = false;
    static final float TOGGLE_HALF = 24f;

    float toggleX() {
        return W * 0.5f;
    }

    float toggleY() {
        return h * 0.46f + 86f;
    }

    private boolean onToggle(float x, float y) {
        if (!TWO_THUMBS_OFFERED) return false;
        float m = TOGGLE_HALF + 8f, dx = x - toggleX(), dy = y - toggleY();
        return dx * dx + dy * dy < m * m;
    }

    /** At the wheel, low on the left: how fast, in kilometers an hour. */
    private void speedFlat(Px f) {
        if (driving == null || dispatch != null) return;
        String v = String.valueOf(Math.round(Math.abs(driving.speed) * 3.6f));
        int ink = 0xFF232A4A, y = h - 44;
        shadowed(f, v, 12, y, 3, driving.speed > Pickup.VMAX * 0.8f ? 0xFFFFD45A : 0xFFF4E8D2, ink);
        shadowed(f, "KM/H", 14 + PixelFont.width(v, 3), y + 14, 1, 0xFFF4E8D2, ink);
    }

    /** Under the way-to-play button, paused: what it is now, in words. */
    private void toggleFlat(Px f) {
        String w = Lang.lines(Runner.classic ? "controls.one" : "controls.two", mission.round, null)[0];
        shadowed(f, w, Math.round(toggleX() - PixelFont.width(w, 1) * 0.5f), Math.round(toggleY() + TOGGLE_HALF + 6f), 1,
                0xFFF4E8D2, 0xFF232A4A);
    }

    /** One finger to two thumbs, or back; kept for the next game. */
    private void toggle() {
        Runner.classic = !Runner.classic;
        host.saveInt(SAVE_CONTROLS, Runner.classic ? 1 : 2);
        letGo();
        host.buzz(20);
    }

    // ---- the pedals at the wheel, two thumbs: the throttle and the brake, low on the right ----
    static final int GAS_PEDAL = 0, BRAKE_PEDAL = 1;
    static final float GAS_HALF = 25f, BRAKE_HALF = 21f;
    private boolean gasHeld, brakeHeld, classicDown;

    float pedalX(int k) {
        return k == GAS_PEDAL ? W - 40f : W - 100f;
    }

    float pedalY(int k) {
        return k == GAS_PEDAL ? h - 66f : h - 42f;
    }

    private boolean onPedal(int k, float x, float y) {
        float m = (k == GAS_PEDAL ? GAS_HALF : BRAKE_HALF) + 7f, dx = x - pedalX(k), dy = y - pedalY(k);
        return dx * dx + dy * dy < m * m;
    }

    private int wheelFinger = -1;

    // ---- her own buttons, low on the right: jump, slide, the mallet ----
    static final int JUMP_BUTTON = 0, SLIDE_BUTTON = 1, MALLET_BUTTON = 2;
    static final float JUMP_HALF = 25f, SLIDE_HALF = 17f, MALLET_HALF = 20f;

    /** Whether her buttons are out: on foot, in the town or indoors, nothing taking her anywhere. */
    boolean footButtons() {
        return driving == null && swapT < 0f && !paused;
    }

    float buttonX(int b) {
        return b == JUMP_BUTTON ? W - 40f : b == SLIDE_BUTTON ? W - 36f : W - 98f;
    }

    float buttonY(int b) {
        return b == JUMP_BUTTON ? h - 62f : b == SLIDE_BUTTON ? h - 128f : h - 40f;
    }

    private float buttonHalf(int b) {
        return b == JUMP_BUTTON ? JUMP_HALF : b == SLIDE_BUTTON ? SLIDE_HALF : MALLET_HALF;
    }

    /** Her button under a finger at (x, y), a little wider than it is drawn; -1 if none. */
    private int footButton(float x, float y) {
        if (!footButtons()) return -1;
        int best = -1;
        float bd = 1e9f;
        for (int b = 0; b < 3; b++) {
            float m = buttonHalf(b) + 7f, dx = x - buttonX(b), dy = y - buttonY(b), dd = dx * dx + dy * dy;
            if (dd < m * m && dd < bd) {
                bd = dd;
                best = b;
            }
        }
        return best;
    }

    private void press(int b) {
        pressedButton = b;
        pressedAt = time;
        if (b == JUMP_BUTTON) runner.jumpButton();
        else if (b == SLIDE_BUTTON) runner.slideButton();
        else runner.tap();
    }

    /** The finger that leads, one finger to play: the first down, till it is lifted (-1: none). */
    private int leadFinger = -1;

    /**
     * Another finger down while the first leads: it presses what it comes down on, the pause, the
     * way out of the pickup, the button by a lift, a sign; on foot, on bare glass, the mallet.
     */
    private void otherFinger(float x, float y) {
        if (paused) return;
        if (onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            letGo();
            return;
        }
        if (driving != null) {
            if (Math.abs(x - liftButtonX()) < BUTTON + 8 && Math.abs(y - liftButtonY()) < BUTTON + 8) getOut();
            return;
        }
        if (liftOffer && onLiftButton(x, y)) {
            liftPressed = true;
            return;
        }
        if (indoors == null) {
            int sgn = signAt(x, y);
            if (sgn >= 0) {
                errand(sgn);
                return;
            }
        }
        runner.tap();
    }

    /** One finger to play (the stand's bots play so too). */
    private void classicTouch(int type, float x, float y) {
        if (type == DOWN) classicDown = true;
        else if (type == UP) classicDown = false;
        // paused: the way to play changed, or a touch anywhere plays on, the finger the button's till it is lifted
        if (paused) {
            if (type == DOWN) {
                if (onToggle(x, y)) toggle();
                else paused = false;
                buttonHeld = true;
            }
            return;
        }
        if (type == DOWN && onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            buttonHeld = true;
            runner.touch(UP, x, y);
            return;
        }
        if (driving != null) {
            drivingTouch(type, x, y);
            return;
        }
        // a sign: she goes where it says by herself
        if (type == DOWN && indoors == null) {
            int sgn = signAt(x, y);
            if (sgn >= 0) {
                errand(sgn);
                buttonHeld = true;
                return;
            }
        }
        if (type == DOWN) errand = NO_ERRAND;
        // the button down by the lift on a roof: pressed, the finger is the button's till it is lifted
        if (type == DOWN && liftOffer && onLiftButton(x, y)) {
            liftPressed = true;
            buttonHeld = true;
            return;
        }
        if (buttonHeld) {
            if (type == UP) buttonHeld = false;
            return;
        }
        runner.touch(type, x, y);
    }

    /** Paused: the game stands still, the buttons say how to play on. */
    boolean paused;

    void update(float dt) {
        if (paused && opening == null && dispatch == null) return;
        time += dt;
        if (opening != null) {
            opening.update(dt);
            if (opening.done) {
                // out of the white into the street, and the room in her head closing round it, the
                // first mission on its screen
                opening = null;
                Frida.eyes = 1f;
        Frida.eyeSize = 1f;
        Frida.eyeHeat = 0f;
                whiteOut = WHITE_OUT;
                runner.update(0f, city, cars, walkers);      // the eye put behind her
                dispatch = new Dispatch(host, briefing()).afterBlow();
                letGo();
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
                if (homeAfter) {
                    homeAfter = false;
                    bringHome();
                } else if (seated() && !free) standUp();
            }
            return;
        }
        if (!free && over() && (overFor += dt) > BACK_TO_ROOM) {
            // indoors when it ends: out at the front door first, the room in her head is out of doors
            if (indoors != null) {
                swapTo = TO_STREET;
                swap();
                swapT = -1f;
            }
            save();
            String[] said = report();
            // lost, the same again when she next takes one at home, from where it began; won, the
            // next, and its time kept against the best. The report, and then she is brought home,
            // onto the sofa, where the next is offered
            again = hunt() ? mission.late : folder != null && folder.theirs;
            if (!again) said = Dispatch.timed(said, record(mission.round, doneTime()), doneTime());
            lost = again;
            if (again) mission.round--;
            again = false;
            free = true;
            folder = null;
            delivery = null;
            chase = nobody;
            dispatch = new Dispatch(host, Dispatch.freed(said));
            homeAfter = true;
            letGo();
            overFor = 0f;
            return;
        }
        whiteOut = Math.max(0f, whiteOut - dt);
        if (dutyOut()) duty.update(runner.x, runner.z, city, driving);
        else duty.all.clear();
        if (driving != null) drive(dt);
        else {
            ladders(dt);
            doors(dt);
            errands(dt);
            getIn(dt);
        }
        // a beat of the arms (a flick, between frames): the air through them
        if (runner.beatsLeft() < beatsWere) host.sound(Synth.WHOOSH, 0.5f, 0.8f);
        if (swapT < 0f && driving == null && !seated()) runner.update(dt, city, cars, walkers);
        // into deep water: a splash, and she swims
        boolean swims = runner.swimming();
        if (swims && !swimSeen) host.sound(Synth.SPLASH, 0.8f, 1f);
        swimSeen = swims;
        beatsWere = runner.beatsLeft();
        int round = mission.round;
        if (free) {
            chase = nobody;
        } else if (delivery != null) {
            delivery.update(dt, duty, driving, runner, host);
        } else if (folder != null) {
            boolean had = folder.hers;
            folder.update(dt, runner, city, cars);
            if (folder.hers && !had) took();
            Chase n = folder.nearest();
            chase = n != null ? n : chase;
        } else {
            mission.update(dt, runner, city, cars);
            chase = mission.target(chase);
        }
        toss.update(dt, runner, brums(), city, runner.tapped(), hunt());
        frighten();
        cars.update(dt, runner, brums());
        walkers.update(dt, runner, chase, city);
        pets.update(dt, runner, walkers, city);
        int caught = mission.caught(), struck = 0;
        if (!hunt()) caught = 0;
        for (Chase g : brums()) struck += g.struck;
        if (round != mission.round) {
            caughtSeen = 0;
            nextSlot = 0;
            struckSeen = struck;
        }
        if (hunt() && mission.late && !lateSeen) host.buzz(300);
        lateSeen = hunt() && mission.late;
        if (hunt() && caught != caughtSeen) {
            caughtSeen = caught;
            host.buzz(caught == Mission.FIVE ? 160 : 60);
        }
        if (struck != struckSeen) {
            struckSeen = struck;
            host.buzz(30);
        }
        fliers.update(dt, true);
        felled.update(dt, runner.x, runner.z, city);
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
    static final int TO_HALL = 1, TO_CABIN = 2, TO_ROOF = 3, TO_STREET = 4, TO_YARD = 5, TO_HOME = 6;
    /** The place indoors she is in; null out of doors. */
    Indoors indoors;
    private float swapT = -1f;
    private int swapTo;
    private boolean liftArmed = true;
    private float liftWait;                  // how long she has stood at a door
    // near a door: so near; standing so long there, or coming at it, it takes her in;
    // running by faster than this, along the front, it does not
    static final float LIFT_NEAR = 1.7f, LIFT_STAND = 0.4f, LIFT_PAST = 5f;
    static final float OUT_STEP = 1.9f;      // out of a door, she stands so far past the place before it
    private final float[] liftNow = new float[11], houseNow = new float[8];
    private final float[] liftOf = new float[11], houseOf = new float[8], doorAt = new float[11];

    /**
     * The doors: at the door of a house with a lift, or the hut on its roof, coming to it or
     * standing at it, she goes in (not running by); indoors, the place has its say; between, a
     * moment of dark while one is swapped for the other.
     */
    private void doors(float dt) {
        Runner r = runner;
        boolean offered = liftOffer;
        liftOffer = false;
        if (swapT >= 0f) {
            float was = swapT;
            swapT += dt;
            if (was < SWAP && swapT >= SWAP) swap();
            if (swapT >= 2f * SWAP) swapT = -1f;
            return;
        }
        if (indoors != null && indoors.home && sofa(dt)) return;
        if (indoors != null) {
            int e = indoors.update(dt, r, host);
            if (e == Indoors.UP) go(TO_ROOF);
            else if (e == Indoors.OUT_FRONT) go(TO_STREET);
            else if (e == Indoors.OUT_BACK) go(TO_YARD);
            return;
        }
        if (r.inAir() || r.climbing() || r.throwing() >= 0f || (folder != null && folder.hers)) return;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        // in a hunt the lifts are shut (the streets are hers to learn); the ways in are open
        boolean near = city.liftNear(r.x, r.z, r.y, 3f, liftNow, houseNow, liftsOpen());
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
        if (!street) {
            // on a roof the way down is taken by the button only: a lift run into by chance in the
            // race would cost the race
            if (dx * dx + dz * dz > LIFT_OFFER * LIFT_OFFER) {
                liftPressed = false;
                return;
            }
            liftOffer = true;
            if (!offered) liftPressed = false;
            if (!liftPressed) return;
            liftPressed = false;
            System.arraycopy(l, 0, liftOf, 0, 11);
            System.arraycopy(houseNow, 0, houseOf, 0, 8);
            host.sound(Synth.DOOR, 0.45f, 1f);
            go(TO_CABIN);
            return;
        }
        if (dx * dx + dz * dz > LIFT_NEAR * LIFT_NEAR) {
            liftWait = 0f;
            return;
        }
        // at the door: coming at it, or standing there a moment; running past, not
        boolean at = fx * wx + fz * wz > 0.3f && r.speed > 0.5f;
        liftWait = r.speed < 1.5f ? liftWait + dt : 0f;
        if (!(at && r.speed < LIFT_PAST || at && fx * wx + fz * wz > 0.7f || liftWait > LIFT_STAND)) return;
        liftWait = 0f;
        // home's front door: into home's room
        if (City.home(houseNow[0], houseNow[1]) && l[1] < houseNow[1]) {
            liftArmed = false;
            if (driving != null) getOut();
            host.sound(Synth.DOOR, 0.45f, 1f);
            go(TO_HOME);
            return;
        }
        System.arraycopy(l, 0, liftOf, 0, 11);
        System.arraycopy(houseNow, 0, houseOf, 0, 8);
        host.sound(Synth.DOOR, 0.45f, 1f);
        go(TO_HALL);
    }

    // ---- the button for the lift down from a roof ----
    static final float LIFT_OFFER = 2.6f;    // so near the hut's door, the button is offered
    static final int BUTTON = 22;            // half its size, frame units
    /** Near the door of a lift's hut on a roof: the button for going down is shown. */
    boolean liftOffer;
    private boolean liftPressed, buttonHeld;

    /** Where the button's middle is: low in the frame, to the right, under the thumb. */
    int liftButtonX() {
        return W - 44;
    }

    int liftButtonY() {
        // on foot it sits over her own buttons; at the wheel, where the jump would be (over the
        // pedals, with two thumbs)
        return driving != null ? h - 70 : h - 190;
    }

    private boolean onLiftButton(float x, float y) {
        float m = BUTTON + 8;
        return Math.abs(x - liftButtonX()) < m && Math.abs(y - liftButtonY()) < m;
    }

    // the pause button, high on the left under the marks
    static final int PAUSE_HALF = 15;

    int pauseX() {
        return 24;
    }

    int pauseY() {
        return 68;
    }

    private boolean onButton(float x, float y, int cx, int cy) {
        float m = PAUSE_HALF + 6;
        return Math.abs(x - cx) < m && Math.abs(y - cy) < m;
    }

    /**
     * The buttons over the frame: pause (or play, paused, and a large play in the middle), and
     * the lift's by its hut on a roof, breathing, lit while pressed.
     */
    /** For the desktop stand: every button and sign there is, in a grid over the frame. */
    boolean showAll;
    int showFrom;

    private void buttons(DrawList d) {
        if (showAll) {
            d.badgeN = 0;
            for (int k = 0; k < DrawList.BADGE_MAX && k + showFrom < Badges.COUNT; k++)
                d.badge(W * (0.2f + 0.3f * (k % 3)), h * 0.18f + (k / 3) * 70f, 28f, k + showFrom, 1f, 0f);
            return;
        }
        if (paused) {
            d.badge(W * 0.5f, h * 0.46f, 40f, Badges.PLAY, 0.95f, 0.15f + 0.15f * (float) Math.sin(time * 3f));
            if (TWO_THUMBS_OFFERED)
                d.badge(toggleX(), toggleY(), TOGGLE_HALF, Runner.classic ? Badges.ONE_FINGER : Badges.TWO_THUMBS, 0.9f, 0f);
            d.badge(pauseX(), pauseY(), PAUSE_HALF, Badges.PLAY, 0.9f, 0f);
            return;
        }
        d.badge(pauseX(), pauseY(), PAUSE_HALF, Badges.PAUSE, 0.75f, 0f);
        if (driving != null) d.badge(liftButtonX(), liftButtonY(), BUTTON + 4, Badges.EXIT, 0.95f, 0f);
        if (driving != null) {
            // the wheel: a steering wheel under the finger while it is down, turned as the pickup's is;
            // a faint one where it would go
            float turn = driving.steer * WHEEL_TURNS;
            if (pedal) d.badge(carCX, carCY, WHEEL_HALF, Badges.STEER, 0.85f, brakeIn > 0.05f ? 0.7f : 0.1f, ringTint(), turn);
            else d.badge(W * 0.5f, h - WHEEL_HALF - 12f, WHEEL_HALF, Badges.STEER, 0.35f, 0f, ringTint(), turn);
        }
        if (footButtons() && !Runner.classic) {
            for (int b = 0; b < 3; b++) {
                float lit = b == pressedButton ? Math.max(0f, 1f - (time - pressedAt) / 0.3f) : 0f;
                int sign = b == JUMP_BUTTON ? Badges.UP : b == SLIDE_BUTTON ? Badges.DOWN : Badges.MALLET;
                // gliding, the jump is a beat of the arms: it says so while there are beats left
                if (b == JUMP_BUTTON && runner.gliding && runner.beatsLeft() > 0) lit = Math.max(lit, 0.25f);
                d.badge(buttonX(b), buttonY(b), buttonHalf(b), sign, 0.8f, lit);
            }
            // the stick: under the thumb while it is down; a faint one where the thumb would go
            if (stickFinger >= 0) {
                float ox = stickTX - stickCX, oy = stickTY - stickCY, m = (float) Math.sqrt(ox * ox + oy * oy);
                if (m > STICK_R) {
                    ox *= STICK_R / m;
                    oy *= STICK_R / m;
                }
                d.badge(stickCX, stickCY, STICK_R + 6f, Badges.BLANK, 0.55f, 0f);
                d.badge(stickCX + ox, stickCY + oy, KNOB, Badges.KNOB, 0.9f, runner.pushed() >= Runner.JOG_TO ? 0.6f : 0f);
            } else {
                d.badge(58f, h - 92f, STICK_R + 6f, Badges.BLANK, 0.22f, 0f);
                d.badge(58f, h - 92f, KNOB, Badges.KNOB, 0.3f, 0f);
            }
        }
        if (liftOffer) {
            float breathe = 0.5f + 0.5f * (float) Math.sin(time * 4f);
            d.badge(liftButtonX(), liftButtonY(), BUTTON + 4, Badges.LIFT, 1f, liftPressed ? 1f : 0.12f * breathe);
        }
    }

    /** Whether the lifts take her: not in a hunt, which is to be run in the streets. */
    boolean liftsOpen() {
        return !hunt();
    }

    // ---- signs at the ways in and up: over a lift's doors, a fire escape's foot, a ladder up a wall ----
    private final float[] marks = new float[6 * 64], markAt = new float[3];
    // the signs found this frame: on screen x, y, size, which, how much shown; and where in the
    // world, the way its wall faces, its kind
    private final float[] signs = new float[10 * 16];
    // the signs drawn last frame, to be touched: the same ten floats each
    private final float[] shown = new float[10 * DrawList.BADGE_MAX];
    private int shownN;
    static final float SIGN_FAR = 34f;       // seen no farther off than this

    /**
     * The signs where she can go in or up near her, those that face the eye: the lift's over its
     * doors and its hut's, a ladder's at the foot of a fire escape, an arrow up at a ladder from
     * one roof to the next; only those on her level (the street, or the roofs about her), the
     * nearest few, fading with distance, rocking a little.
     */
    private void signs(DrawList d) {
        Runner r = runner;
        int n = city.marks(r.x, r.z, SIGN_FAR, marks, 64), k = 0;
        for (int i = 0; i < n && k < 16; i++) {
            int o = i * 6;
            float x = marks[o], y = marks[o + 1], z = marks[o + 2], nx = marks[o + 3], nz = marks[o + 4];
            int kind = (int) marks[o + 5];
            if (!liftsOpen() && (kind == City.MARK_STREET_LIFT || kind == City.MARK_HUT)) continue;
            // on her level: a lift's or a fire escape's in the street while she is in the street; a
            // ladder's up, or a hut's, while she is up there near its foot
            float level = kind == City.MARK_STREET_LIFT || kind == City.MARK_ESCAPE || kind == City.MARK_DOOR || kind == City.MARK_HOME ? City.WALK_TOP : marks[o + 1] - (kind == City.MARK_ROOF_LADDER ? 1.9f : City.HUT_HIGH + 0.8f);
            if (Math.abs(r.y - level) > 2.6f) continue;
            float ox = r.eyeX - x, oz = r.eyeZ - z;
            if (ox * nx + oz * nz <= 0.2f) continue;
            float dist = (float) Math.sqrt(ox * ox + oz * oz + (r.eyeY - y) * (r.eyeY - y));
            float shown = Math.min(1f, (SIGN_FAR - dist) / 10f) * Math.min(1f, Math.max(0f, (dist - 2.5f) / 3f));
            if (shown <= 0.02f) continue;
            float bob = 0.08f * (float) Math.sin(time * 2.2f + x * 0.7f + z * 0.3f);
            if (!d.project(x, y + bob, z, markAt) || markAt[2] < 1f) continue;
            if (markAt[0] < -20f || markAt[0] > W + 20f || markAt[1] < -20f || markAt[1] > h + 20f) continue;
            int s = k * 10;
            signs[s] = markAt[0];
            signs[s + 1] = markAt[1];
            signs[s + 2] = Math.max(7f, Math.min(18f, W * 1.6f * 0.75f / markAt[2]));
            signs[s + 3] = kind == City.MARK_ROOF_LADDER ? Badges.UP : kind == City.MARK_ESCAPE ? Badges.LADDER
                    : kind == City.MARK_DOOR ? Badges.ENTER : kind == City.MARK_HOME ? Badges.HOME : Badges.LIFT;
            signs[s + 4] = shown;
            signs[s + 5] = x;
            signs[s + 6] = z;
            signs[s + 7] = nx;
            signs[s + 8] = nz;
            signs[s + 9] = kind;
            k++;
        }
        // the duty pickups: the wheel over each, from any side
        for (int i = 0; i < duty.all.size() && k < 16; i++) {
            Pickup p = duty.all.get(i);
            if (Math.abs(r.y - p.y) > 2.6f) continue;
            float ox = r.eyeX - p.x, oz = r.eyeZ - p.z;
            float dist = (float) Math.sqrt(ox * ox + oz * oz);
            float shown = Math.min(1f, (SIGN_FAR - dist) / 10f) * Math.min(1f, Math.max(0f, (dist - 2.5f) / 3f));
            if (shown <= 0.02f) continue;
            float bob = 0.08f * (float) Math.sin(time * 2.2f + p.x * 0.7f);
            if (!d.project(p.x, p.y + 2.7f + bob, p.z, markAt) || markAt[2] < 1f) continue;
            if (markAt[0] < -20f || markAt[0] > W + 20f || markAt[1] < -20f || markAt[1] > h + 20f) continue;
            int o = k * 10;
            signs[o] = markAt[0];
            signs[o + 1] = markAt[1];
            signs[o + 2] = Math.max(7f, Math.min(18f, W * 1.6f * 0.75f / markAt[2]));
            signs[o + 3] = Badges.WHEEL;
            signs[o + 4] = shown;
            signs[o + 5] = p.x;
            signs[o + 6] = p.z;
            signs[o + 7] = 0f;
            signs[o + 8] = 0f;
            signs[o + 9] = MARK_PICKUP;
            k++;
        }
        // the nearest first, as many as there is room for beside the buttons; kept for the finger
        shownN = 0;
        for (int a = 0; a < k && d.badgeN < DrawList.BADGE_MAX - 3; a++) {
            int best = -1;
            float bs = -1f;
            for (int b2 = 0; b2 < k; b2++) if (signs[b2 * 10 + 2] > bs) {
                bs = signs[b2 * 10 + 2];
                best = b2;
            }
            int s = best * 10;
            boolean mine = errand != NO_ERRAND && Math.abs(signs[s + 5] - errandX) < 0.1f && Math.abs(signs[s + 6] - errandZ) < 0.1f;
            d.badge(signs[s], signs[s + 1], signs[s + 2], (int) signs[s + 3], signs[s + 4] * 0.9f,
                    mine ? 0.6f + 0.4f * (float) Math.sin(time * 8f) : 0f);
            System.arraycopy(signs, s, shown, shownN++ * 10, 10);
            signs[s + 2] = -2f;
        }
    }

    // ---- the duty pickups: in, driven, out ----
    static final float GET_IN = 1.3f, GET_IN_STAND = 0.5f;
    private boolean pedal;
    private float wheelIn, brakeIn, doorWait;
    private boolean doorArmed = true;         // not got in again until she has been away from the door
    private final float[] seatAt = new float[3], doorAt2 = new float[2];

    /** Whether the duty pickups are out: in every mission but a hunt, which is run on foot. */
    boolean dutyOut() {
        return !hunt();
    }

    private Pickup pickupAt(float x, float z) {
        Pickup best = null;
        float bd = 4f;
        for (Pickup p : duty.all) {
            float dd = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
            if (dd < bd) {
                bd = dd;
                best = p;
            }
        }
        return best;
    }

    /** Standing at a pickup's door a moment, she gets in. */
    private void getIn(float dt) {
        Runner r = runner;
        if (!dutyOut() || indoors != null || swapT >= 0f || r.inAir() || r.climbing() || r.speed > 1f) {
            doorWait = 0f;
            return;
        }
        for (Pickup p : duty.all) {
            p.door(0.9f, doorAt2);
            float dx = doorAt2[0] - r.x, dz = doorAt2[1] - r.z;
            if (dx * dx + dz * dz < GET_IN * GET_IN && Math.abs(r.y - p.y) < 1f) {
                if (doorArmed && (doorWait += dt) > GET_IN_STAND) getIn(p);
                return;
            }
        }
        doorWait = 0f;
        doorArmed = true;
    }

    private void getIn(Pickup p) {
        if (p == null || driving != null) return;
        letGo();
        driving = p;
        p.driven = true;
        p.speed = 0f;
        duty.driven(p);
        pedal = false;
        wheelIn = brakeIn = 0f;
        doorWait = 0f;
        errand = NO_ERRAND;
        runner.stopLead();
        host.sound(Synth.DOOR, 0.5f, 0.9f);
        host.buzz(30);
        drive(0f);
    }

    /** Out of the pickup, by its door, or the other one if that side is shut in; it stays where it is. */
    private void getOut() {
        Pickup p = driving;
        if (p == null) return;
        letGo();
        p.cap = -1f;
        driving = null;
        p.driven = false;
        p.speed = 0f;
        p.steer = 0f;
        float[] q = {0f, 0f};
        p.door(0.9f, q);
        float ox = q[0], oz = q[1];
        if (city.push(q, 0.5f, p.y) || Math.abs(q[0] - ox) > 0.05f) {
            // the far side: as far out on the right
            float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading);
            q[0] = p.x + c * (Pickup.HALF_WIDE + 0.9f) + s * Pickup.SEAT_Z;
            q[1] = p.z - s * (Pickup.HALF_WIDE + 0.9f) + c * Pickup.SEAT_Z;
        }
        runner.getOut(q[0], city.ground(q[0], q[1]), q[1], p.heading);
        doorArmed = false;
        runner.recenter();
        buttonHeld = true;
        host.sound(Synth.DOOR, 0.5f, 1.05f);
    }

    /**
     * A frame at the wheel: the pickup driven as the finger has it, she carried in it. While a
     * finger is down, it goes, as fast as the way ahead lets it; the finger moved aside from where
     * it came down turns the wheel, gently near the middle, full over at the ring's edge; drawn down
     * past the ring's middle, it brakes, and held so standing, it backs. Lifted, the pickup slows to
     * a stop. With the wheel let be, it settles along the street it is nearly along.
     */
    private void drive(float dt) {
        Pickup p = driving;
        float sx = pedal ? (carTX - carCX) / CAR_R : 0f, sy = pedal ? (carTY - carCY) / CAR_R : 0f;
        float wheel = 0f;
        brakeIn = 0f;
        if (pedal) {
            float u = Math.min(1f, Math.abs(sx));
            if (u > CAR_DEAD) wheel = Math.signum(sx) * (float) Math.pow((u - CAR_DEAD) / (1f - CAR_DEAD), CAR_CURVE);
            if (sy > CAR_BRAKE) brakeIn = Math.min(1f, 0.35f + 0.65f * (sy - CAR_BRAKE) / (1f - CAR_BRAKE));
        }
        boolean go = pedal && brakeIn == 0f;
        p.cap = -1f;
        wheelIn = wheel;
        // a street lamp in the way, going fast enough: down it goes, the pickup hardly slowed
        if (Math.abs(p.speed) > Cars.RAM_FROM) {
            float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), sg = Math.signum(p.speed);
            float reach = Pickup.ENDS + Pickup.END_R * 0.6f + Math.abs(p.speed) * dt;
            if (city.fell(p.x + fx * sg * reach, p.z + fz * sg * reach, Pickup.END_R + 0.25f, fellAt)) {
                felled.fell(fellAt[0], fellAt[1], fx * sg, fz * sg, city);
                p.speed *= 0.93f;
                host.sound(Synth.CLANG, 0.8f, 0.8f);
                host.sound(Synth.GLASS, 0.4f, 1.2f);
                host.buzz(40);
                walkers.alarm(fellAt[0], fellAt[1], 8f);
                pets.alarm(fellAt[0], fellAt[1], 8f);
            }
        }
        int ramsWere = cars.rams;
        p.drive(dt, go, wheel, brakeIn, false, city, cars, walkers);
        if (cars.rams != ramsWere) {
            // a car rammed: a jolt in the hand, the people off; the pickup, not a scratch
            p.speed *= 0.9f;
            host.buzz(60 + Math.round(60f * cars.ramHard));
            walkers.alarm(cars.ramX, cars.ramZ, 10f);
            pets.alarm(cars.ramX, cars.ramZ, 10f);
            cars.alarm(cars.ramX, cars.ramZ, 14f);
        }
        // the wheel let be, or nearly: along the street it is nearly along, the less the more it is turned
        float let = 1f - Math.min(1f, Math.abs(p.steer) / SETTLE_WHEEL);
        if (let > 0f && Math.abs(p.speed) > 3f) {
            float q = (float) Math.PI * 0.5f, d = Math.round(p.heading / q) * q - p.heading;
            if (Math.abs(d) < SETTLE_WITHIN) p.heading += d * Math.min(1f, dt * SETTLE_RATE * let);
        }
        p.seat(seatAt);
        runner.ride(dt, city, p.x, p.y, p.z, p.heading, p.speed, seatAt[0], seatAt[1], seatAt[2], p.steer);
    }

    /** The ring at the wheel, coloured for what she is about: the hunt coral, the folder turquoise, a delivery amber, free cream. */
    private int ringTint() {
        if (free) return DrawList.TINT_CREAM;
        if (delivery != null) return DrawList.TINT_AMBER;
        if (folder != null) return DrawList.TINT_TURQUOISE;
        return DrawList.TINT_CORAL;
    }

    // the finger at the wheel, as parts of the ring's radius from where it came down: aside, no
    // turn within so far, then more and more (so steep a curve) to full over at the edge; drawn
    // down past so far, the brake
    static final float CAR_DEAD = 0.3f, CAR_CURVE = 1.3f, CAR_BRAKE = 0.55f;
    static final float CAR_R = 44f;
    // the steering wheel drawn under the finger: half its size, frame pixels; turned so far at full lock (radians)
    static final float WHEEL_HALF = 66f, WHEEL_TURNS = 2.2f;            // the ring at the wheel, its radius: wider than her stick on foot
    static final float SETTLE_RATE = 3.5f;     // how fast it settles along the street, the wheel let be
    final Felled felled = new Felled();
    private final float[] fellAt = new float[2];
    static final float SETTLE_WITHIN = 0.5f;   // radians off a street's way, at most, for the pickup to settle along it
    static final float SETTLE_WHEEL = 0.25f;   // turned so far or more, it is let go its own way
    private float carCX, carCY, carTX, carTY;

    /** The finger at the wheel: where it comes down, the ring's middle, fixed till it is lifted; the button low on the right: out. */
    private void drivingTouch(int type, float x, float y) {
        if (type == DOWN && Math.abs(x - liftButtonX()) < BUTTON + 8 && Math.abs(y - liftButtonY()) < BUTTON + 8) {
            getOut();
            return;
        }
        if (buttonHeld) {
            if (type == UP) buttonHeld = false;
            return;
        }
        if (type == DOWN) {
            pedal = true;
            // the wheel's middle where the finger came down, as near the edges as it can be drawn whole
            float m = WHEEL_HALF * 0.85f;
            carCX = Math.max(m, Math.min(W - m, x));
            carCY = Math.max(h * 0.3f, Math.min(h - m, y));
            carTX = x;
            carTY = y;
        } else if (type == MOVE && pedal) {
            carTX = x;
            carTY = y;
        } else if (type == UP) {
            pedal = false;
        }
    }

    // ---- errands: a sign touched, she goes where it says by herself ----
    static final int NO_ERRAND = 0, ERRAND_CLIMB = 1, ERRAND_UP = 2, ERRAND_DOWN = 3, ERRAND_DRIVE = 4, ERRAND_THROUGH = 5;
    private boolean errandIn;                  // through a house: she has been inside
    static final int MARK_PICKUP = 4;          // a sign's kind beside the town's (see City.marks)
    private Pickup errandPickup;
    private int errand;
    private float errandX, errandZ, errandFor;
    private final float[] errandLadder = new float[6], way = new float[2];

    /** For the desktop stand: where on the glass the nearest sign of a kind was drawn (x, y); false if none was. */
    boolean signOnGlass(int which, float[] out) {
        for (int i = 0; i < shownN; i++)
            if ((int) shown[i * 10 + 3] == which) {
                out[0] = shown[i * 10];
                out[1] = shown[i * 10 + 1];
                return true;
            }
        return false;
    }

    /** Whether she is on an errand a sign sent her on. */
    boolean onErrand() {
        return errand != NO_ERRAND;
    }

    /** The sign drawn last frame under a finger at (x, y), as its place in shown; -1 if none. */
    private int signAt(float x, float y) {
        int best = -1;
        float bd = 1e9f;
        for (int i = 0; i < shownN; i++) {
            int o = i * 10;
            float reach = Math.max(shown[o + 2], 14f) + 4f, dx = x - shown[o], dy = y - shown[o + 1], dd = dx * dx + dy * dy;
            if (dd < reach * reach && dd < bd) {
                bd = dd;
                best = i;
            }
        }
        return best;
    }

    /**
     * A sign touched: up a ladder (to its foot and up it), up by a lift (to its door, in, across
     * the hall into the cabin, and up), down by a lift (to the hut's door, in, down, across the
     * hall and out at the front).
     */
    private void errand(int i) {
        Runner r = runner;
        int o = i * 10;
        float mx = shown[o + 5], mz = shown[o + 6], nx = shown[o + 7], nz = shown[o + 8];
        int kind = (int) shown[o + 9];
        float facing = (float) Math.atan2(-nx, -nz);
        errandX = mx;
        errandZ = mz;
        errandFor = 0f;
        if (kind == MARK_PICKUP) {
            Pickup p = pickupAt(mx, mz);
            if (p == null) return;
            p.door(0.9f, way);
            r.lead(way[0], way[1], (float) Math.atan2(p.x - way[0], p.z - way[1]));
            errandPickup = p;
            errand = ERRAND_DRIVE;
        } else if (kind == City.MARK_ESCAPE || kind == City.MARK_ROOF_LADDER) {
            // the sign hangs 0.6 off the wall, the foot is 0.55 off it
            float fx = mx - nx * 0.05f, fz = mz - nz * 0.05f;
            if (!city.ladderNear(fx, fz, r.y, 1f, errandLadder)) return;
            r.lead(errandLadder[0], errandLadder[1], facing);
            errand = ERRAND_CLIMB;
        } else if (kind == City.MARK_STREET_LIFT || kind == City.MARK_DOOR || kind == City.MARK_HOME) {
            liftArmed = true;
            r.lead(mx + nx * 0.45f, mz + nz * 0.45f, facing);
            errand = kind == City.MARK_STREET_LIFT ? ERRAND_UP : ERRAND_THROUGH;
            errandIn = false;
        } else {
            liftArmed = true;
            r.lead(mx + nx * 0.45f, mz + nz * 0.45f, facing);
            errand = ERRAND_DOWN;
        }
        host.buzz(20);
    }

    /** The errand going on: led on indoors, the ladder taken at its foot, the button pressed at the hut; done or given up, none. */
    private void errands(float dt) {
        if (errand == NO_ERRAND) return;
        Runner r = runner;
        errandFor += dt;
        boolean arrived = r.arrived();
        if (errandFor > 45f || swapT >= 0f) {
            if (errandFor > 45f) errand = NO_ERRAND;
            return;
        }
        switch (errand) {
            case ERRAND_CLIMB:
                if (r.climbing()) {
                    errand = NO_ERRAND;
                    r.stopLead();
                }
                else if (arrived) {
                    float[] l = errandLadder;
                    r.climb(l[0], l[1], l[2], l[3], l[4], l[5]);
                    host.sound(Synth.CLANG, 0.25f, 1.5f);
                    errand = NO_ERRAND;
                } else if (!r.led()) errand = NO_ERRAND;
                break;
            case ERRAND_DRIVE:
                if (arrived) {
                    getIn(errandPickup);
                    errand = NO_ERRAND;
                } else if (!r.led()) errand = NO_ERRAND;
                break;
            case ERRAND_THROUGH:
                if (indoors != null) {
                    errandIn = true;
                    indoors.goal(r, way);
                    r.lead(way[0], way[1], (float) Math.atan2(way[0] - r.x, way[1] - r.z));
                } else if (errandIn) {
                    errand = NO_ERRAND;
                    r.stopLead();
                } else if (!r.led() && !arrived && errandFor > 1f && liftWait <= 0f) errand = NO_ERRAND;
                break;
            case ERRAND_UP:
                if (indoors != null) {
                    if (!r.held) {
                        indoors.goal(r, way);
                        r.lead(way[0], way[1], (float) Math.atan2(way[0] - r.x, way[1] - r.z));
                    }
                } else if (r.y > 2f) {
                    errand = NO_ERRAND;
                    r.stopLead();
                } else if (!r.led() && !arrived && errandFor > 1f && liftWait <= 0f) errand = NO_ERRAND;
                break;
            default:
                if (indoors != null) {
                    if (!r.held) {
                        indoors.goal(r, way);
                        r.lead(way[0], way[1], (float) Math.atan2(way[0] - r.x, way[1] - r.z));
                    }
                } else if (r.y < 1f) {
                    errand = NO_ERRAND;
                    r.stopLead();
                } else if (liftOffer && !r.led()) liftPressed = true;
                else if (!r.led() && !liftOffer) errand = NO_ERRAND;
        }
    }

    // ---- ladders: up a fire escape from the street, or from one roof up onto a taller one ----
    private final float[] ladderNow = new float[6];
    private float ladderWait;
    static final float LADDER_NEAR = 1.1f, LADDER_STAND = 0.35f, LADDER_GRAB = 1.3f;

    /**
     * At the foot of a ladder, coming at its wall or standing there a moment, she climbs it; in
     * the air (gliding down, falling, leaping at it), coming at its wall near enough, she takes
     * hold of it where she is and climbs on.
     */
    private void ladders(float dt) {
        Runner r = runner;
        if (indoors != null || swapT >= 0f || r.climbing() || r.throwing() >= 0f || (folder != null && folder.hers)) return;
        float[] l = ladderNow;
        if (r.inAir()) {
            if (r.sinceLetGo() < 0.8f || !city.ladderAt(r.x, r.z, r.y, LADDER_GRAB, l)) return;
            float into = -((float) Math.sin(r.heading) * l[4] + (float) Math.cos(r.heading) * l[5]);
            if (into < 0.3f) return;
            r.climb(l[0], l[1], l[2], l[3], l[4], l[5]);
            host.sound(Synth.CLANG, 0.3f, 1.3f);
            return;
        }
        if (!city.ladderNear(r.x, r.z, r.y, LADDER_NEAR, l)) {
            ladderWait = 0f;
            return;
        }
        float into = -((float) Math.sin(r.heading) * l[4] + (float) Math.cos(r.heading) * l[5]);
        boolean at = into > 0.5f && r.speed > 0.5f;
        ladderWait = r.speed < 1.2f ? ladderWait + dt : 0f;
        if (!at && ladderWait < LADDER_STAND) return;
        ladderWait = 0f;
        r.climb(l[0], l[1], l[2], l[3], l[4], l[5]);
        host.sound(Synth.CLANG, 0.25f, 1.5f);
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
                indoors = Rooms.hall(houseOf, seed, city.backClear(houseOf, 0), houseOf[7] == City.LIFT);
                indoors.enter(r, l[10] > 0.5f);
                break;
            case TO_CABIN:
                indoors = Rooms.hall(houseOf, seed, city.backClear(houseOf, 0), true);
                indoors.comeDown(r);
                break;
            case TO_HOME:
                indoors = Rooms.home(City.HOME_DOOR_X, City.HOME_DOOR_Z);
                indoors.enter(r, false);
                break;
            case TO_ROOF:
                leave();
                r.put(l[4], l[5], l[6], l[7]);
                host.sound(Synth.DOOR, 0.35f, 1.2f);
                break;
            default: {
                if (indoors != null && indoors.home) {
                    // out of home's door onto the promenade side of the pavement, facing the sea
                    leave();
                    r.put(City.HOME_DOOR_X, City.WALK_TOP, City.HOME_DOOR_Z - OUT_STEP - 0.6f, (float) Math.PI);
                    liftArmed = false;
                    host.sound(Synth.DOOR, 0.35f, 1.1f);
                    break;
                }
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
                // a couple of steps clear of the door, so the way back in is open at once
                px += nx * OUT_STEP;
                pz += nz * OUT_STEP;
                r.put(px, swapTo == TO_YARD ? city.ground(px, pz) : City.WALK_TOP, pz, (float) Math.atan2(nx, nz));
                liftArmed = true;
                host.sound(Synth.DOOR, 0.35f, 1.1f);
            }
        }
        r.recenter();
        // the eye put where it is to be at once, behind her in the new place
        r.update(0f, city, cars, walkers);
    }

    // ---- home's sofa: come up to it, she sits; it is kept, and the screen asks which mission ----
    private float seatedFor;
    private boolean sofaAsked;
    private final float[] sofaAt = new float[3];
    static final float SIT_FOR = 1.3f;       // sat so long, the screen comes on

    /** Seated, a frame of it (true); or come up to it, down she sits (true); or not near it (false). */
    private boolean sofa(float dt) {
        Indoors in = indoors;
        Runner r = runner;
        if (!in.seated) {
            // up from it, she sits again only once she has gone from it
            if (!in.atSofa(r)) in.sofaArmed = true;
            if (!in.sofaArmed || r.inAir() || r.led() && errand != ERRAND_THROUGH || !in.atSofa(r)) return false;
            sitDown();
        }
        seatedFor += dt;
        r.lounging = true;
        in.seat(sofaAt);
        r.ride(dt, in, sofaAt[0], sofaAt[1], sofaAt[2], in.seatHeading(), 0f, sofaAt[0], sofaAt[1] - 0.03f, sofaAt[2], 0f);
        // the eye where the room is seen from while she sits (the screen in her head comes on from there)
        if (in.shot(eyeNow)) r.eyeAt(eyeNow);
        if (!sofaAsked && seatedFor > SIT_FOR) {
            sofaAsked = true;
            dispatch = new Dispatch(host, Dispatch.sofa(free ? mission.round + 1 : mission.round, free && lost)).choices(Dispatch.SOFA_PICKS);
            letGo();
        }
        return true;
    }

    /** Down on the sofa: the game kept, the screen to come on in a moment. */
    private void sitDown() {
        Indoors in = indoors;
        in.seated = true;
        seatedFor = 0f;
        sofaAsked = false;
        errand = NO_ERRAND;
        runner.stopLead();
        letGo();
        // kept: the mission to play next, what has been won
        host.saveInt(SAVE_MISSION, free ? mission.round + 1 : mission.round);
        host.saveInt(SAVE_PLATES, plates);
        host.saveInt(SAVE_MONEY, money);
        host.sound(Synth.WOOD, 0.5f, 0.7f);
        host.buzz(40);
    }

    /** Up from the sofa, a step before it, facing the door. */
    private void standUp() {
        Indoors in = indoors;
        if (in == null || !in.seated) return;
        in.seated = false;
        in.sofaArmed = false;
        in.beforeSofa(sofaAt);
        runner.getOut(sofaAt[0], Indoors.FLOOR, sofaAt[1], in.seatHeading());
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        letGo();
    }

    /** Whether she sits on home's sofa. */
    boolean seated() {
        return indoors != null && indoors.seated;
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
        for (Chase g : mission.brums) {
            if (g.done() && g.slot < 0) g.slot = nextSlot++;
            if (g.slot >= 0 && g.sinceTaken() >= PLATE_POP + PLATE_FLY) slots |= 1 << g.slot;
        }
        for (int k = 0; k < Mission.FIVE; k++) {
            boolean lit = (slots & (1 << k)) != 0;
            plate(f, 13 + k * 13, 18, 5f, 1f, lit ? 0xFF5FE0C4 : 0xFF3A4660, lit ? 0xFF1E3A40 : 0xFF2A3350, ink);
        }
        // a plate just knocked off: up off the brow, then across the frame into its slot
        for (Chase g : mission.brums) {
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
        // the time the hunt still has, in the corner: in fire for its last minute, blinking for its last seconds
        float left = mission.left();
        String rest = clock(left);
        boolean blink = left > 0f && left < 10f && (time * 2f) % 1f > 0.5f;
        if (!blink) shadowed(f, rest, W - PixelFont.width(rest, 2) - 8, 14, 2, left < 60f ? fire : cream, ink);
        String clock = clock(mission.doneAt);
        if (mission.late) {
            String w = Lang.lines("late.short", mission.round, null)[0];
            shadowed(f, w, (W - PixelFont.width(w, 3)) / 2, h / 2 - 60, 3, fire, ink);
            String cc = got + "/" + Mission.FIVE;
            shadowed(f, cc, (W - PixelFont.width(cc, 4)) / 2, h / 2 - 10, 4, cream, ink);
            return;
        }
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
        for (Chase g : mission.brums) {
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

    private final float[] goal = new float[3];

    /**
     * Over the frame, a delivery: how far off where the way goes now is, in meters, at the top, and
     * under it what it is (a pickup, the factory, the store, home); a green ring round it in sight,
     * a green mark at the edge of the frame the way it is while it is not; over a bay she is to
     * stand in, its sign (a crate taken up at the factory, put down at the store); the pay and the
     * clock in the corner, and the crates on the pickup she drives. Done, the pay large.
     */
    private void deliveryFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        Delivery dv = delivery;
        String clock = clock(dv.over() ? dv.doneAt : dv.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        String sum = money(Delivery.REWARD);
        shadowed(f, sum, W - PixelFont.width(sum, 1) - 8, 32, 1, gold, ink);
        // the crates on the pickup she drives, in a row of little squares
        Pickup p = driving != null ? driving : dv.carrying;
        for (int k = 0; k < Pickup.CRATES; k++) {
            boolean on = p != null && k < p.load;
            f.rect(10 + k * 11, 12, 19 + k * 11, 21, ink);
            f.rect(11 + k * 11, 13, 18 + k * 11, 20, on ? 0xFFC79B5E : 0xFF3A4660);
        }
        if (dv.over()) {
            String big = "+" + sum;
            shadowed(f, big, (W - PixelFont.width(big, 4)) / 2, h / 2 - 40, 4, gold, ink);
            return;
        }
        if (!dv.goal(duty, driving, runner, goal)) return;
        float gx = goal[0] - runner.x, gz = goal[2] - runner.z;
        String m = String.valueOf(Math.round((float) Math.sqrt(gx * gx + gz * gz)));
        shadowed(f, m, (W - PixelFont.width(m, 3)) / 2, 14, 3, cream, ink);
        String w = Lang.lines(dv.goalWord(), mission.round, null)[0];
        shadowed(f, w, (W - PixelFont.width(w, 1)) / 2, 38, 1, green, ink);
        boolean bay = dv.step == Delivery.FACTORY || dv.step == Delivery.STORE && (dv.carrying == null || dv.carrying == driving);
        if (bay && d.project(goal[0], City.WALK_TOP + 2.6f, goal[2], at) && at[0] > 0 && at[0] < W && at[1] > 30 && at[1] < h) {
            // the bay's sign: what is done there
            float near = (float) Math.sqrt(gx * gx + gz * gz);
            float shown = Math.min(1f, Math.max(0f, (60f - near) / 15f));
            d.badge(at[0], at[1], Math.max(8f, Math.min(16f, 300f / Math.max(1f, near))),
                    dv.step == Delivery.FACTORY ? Badges.PICK_UP : Badges.DOWN, shown, 0.5f + 0.5f * (float) Math.sin(time * 4f));
        }
        goalMark(d, f, goal[0], goal[1], goal[2], green, ink);
    }

    /** Free: what has been won, and where home is (the next mission is taken there), quietly. */
    private void freeFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        String sum = money(money);
        shadowed(f, sum, W - PixelFont.width(sum, 1) - 8, 14, 1, gold, ink);
        if (indoors != null) return;
        float gx = City.HOME_DOOR_X - runner.x, gz = City.HOME_DOOR_Z - runner.z;
        float far = (float) Math.sqrt(gx * gx + gz * gz);
        if (far < 12f) return;
        String w = Lang.lines("goal.free", mission.round, null)[0];
        shadowed(f, w, (W - PixelFont.width(w, 1)) / 2, 14, 1, cream, ink);
        String m = String.valueOf(Math.round(far));
        shadowed(f, m, (W - PixelFont.width(m, 2)) / 2, 26, 2, green, ink);
        goalMark(d, f, City.HOME_DOOR_X, City.WALK_TOP + 1.5f, City.HOME_DOOR_Z, green, ink);
    }

    /** Where the way goes: a green ring round it while it is in sight, a green mark at the edge the way it is while not. */
    private void goalMark(DrawList d, Px f, float x, float y, float z, int green, int ink) {
        boolean seen = d.project(x, y, z, at) && at[0] > 6 && at[0] < W - 6 && at[1] > 50 && at[1] < h - 6;
        if (seen) {
            float rr = 6.5f + 1.2f * (float) Math.sin(time * 5f);
            ring(f, at[0], at[1], rr + 1f, rr - 3f, ink);
            ring(f, at[0], at[1], rr, rr - 2f, green);
            return;
        }
        float ax = (float) Math.sin(runner.eyeYaw), az = (float) Math.cos(runner.eyeYaw);
        float vx = x - runner.x, vz = z - runner.z;
        float ang = (float) Math.atan2(vx * az - vz * ax, vx * ax + vz * az);
        float ex = (float) Math.sin(ang), ey = -(float) Math.cos(ang);
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

    /** Gliding: the beats of the arms left in this flight, as small wings low in the middle of the frame. */
    private void beatsFlat(Px f) {
        Runner r = runner;
        if (!r.inAir() || !r.gliding) return;
        int ink = 0xFF232A4A, lit = 0xFFF4E8D2, spent = 0xFF5A6070;
        int n = Runner.BEATS, left = r.beatsLeft(), y = h - 34, x0 = W / 2 - (n * 14) / 2 + 2;
        for (int k = 0; k < n; k++) {
            int c = k < left ? lit : spent, x = x0 + k * 14;
            // a wing: three feathers, longer to the outside
            for (int s = 0; s < 3; s++) {
                f.rect(x + s * 3 - 1, y - s - 1, x + s * 3 + 3, y + 3, ink);
                f.rect(x + s * 3, y - s, x + s * 3 + 2, y + 2, c);
            }
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
    /** Seconds as minutes and seconds (the screen's words). */
    static String clockOf(float seconds) {
        return clock(seconds);
    }

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
        if (dispatch != null) {
            // the room in her head, or the eye on its way there
            dispatch.eye(r, eyeNow);
            d.camera(eyeNow[0], eyeNow[1], eyeNow[2], eyeNow[3], eyeNow[4], W * 1.2f, 0.03f, 80f);
        } else if (indoors.shot(eyeNow)) d.camera(eyeNow[0], eyeNow[1], eyeNow[2], eyeNow[3], eyeNow[4], W * 1.25f, 0.03f, 80f);
        else d.camera(r.eyeX, r.eyeY, r.eyeZ, r.eyeYaw, r.eyePitch, W * 1.2f, 0.03f, 80f);
        // the room, unless the eye has gone back into her head past its walls (she sits against one)
        if (dispatch == null || indoors.holds(eyeNow[0], eyeNow[2])) indoors.draw(d);
        if (!r.hidden && (dispatch == null || dispatch.showsHer())) {
            r.draw(d);
            toss.draw(d, r);
        }
        if (dispatch != null) dispatch.draw(d, f, h);
        else if (free) freeFlat(d, f);
        else if (delivery != null) deliveryFlat(d, f);
        else if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        beatsFlat(f);
        speedFlat(f);
        if (dispatch == null) buttons(d);
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
        felled.draw(d, eyeX, eyeZ, ex, ez);
        duty.draw(d, eyeX, eyeZ, ex, ez, time);
        walkers.draw(d, eyeX, eyeZ, ex, ez);
        pets.draw(d, eyeX, eyeZ, ex, ez, walkers);
        if ((dispatch == null || dispatch.showsHer()) && !r.hidden) {
            r.draw(d);
            toss.draw(d, r);
        }
        if (folder != null) folder.draw(d, r);
        // the one nearest that still burns gives off light and glow; the others only burn
        Chase lit = null;
        Chase[] brums = brums();
        for (Chase g : brums) if (g.burning() && (lit == null || g.apart < lit.apart)) lit = g;
        for (Chase g : brums) {
            float gx = g.x - eyeX, gz = g.z - eyeZ;
            if (gx * ex + gz * ez < -6f || gx * gx + gz * gz > 200f * 200f) continue;
            g.draw(d, g == lit);
        }
        if (dispatch != null) dispatch.draw(d, f, h);
        else if (free) freeFlat(d, f);
        else if (delivery != null) deliveryFlat(d, f);
        else if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        beatsFlat(f);
        speedFlat(f);
        if (dispatch == null) {
            if (driving == null) signs(d);
            buttons(d);
            if (paused && TWO_THUMBS_OFFERED) toggleFlat(f);
        }
        swapFlat(f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // the white the opening ended on, going off
        if (whiteOut > 0f) Opening.wash(f, whiteOut / WHITE_OUT);
        meter(f);
    }
}
