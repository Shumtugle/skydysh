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
    static final String VERSION = "1.4.29";    // shown on screen; keep in step with versionName
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
        /** A piece of music to play now, as a standard MIDI file, through the phone's own synthesizer; null stops it. */
        void music(byte[] midi);
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
    Rescue rescue;                           // the mission now, when it is a call out
    /** A call out done with: what its blast left lies in the pass, till she is far off. */
    private Rescue debris;
    /** How far off she goes before what the blast left in the pass is cleared away, meters. */
    static final float DEBRIS_GONE = 400f;
    Puppy puppy;                             // the mission now, when it is the puppy in the sea
    Tow tow;                                 // the mission now, when it is the tow out of the quarry
    Archive archive;                         // the mission now, when it is the papers to the archive
    Mend mend;                               // the mission now, when it is the helicopter mended and flown back
    Port port;                               // the mission now, when it is the port, the first time
    Lab lab;                                 // the mission now, when it is the navigator's trial
    Herd herd;                               // the mission now, when it is the hot brums led off
    Cherry cherry;                           // the mission now, when it is the black hole in the collider hall
    Fault fault;                             // the mission now, when it is the light gone wrong
    /** A light gone wrong of itself about the town between jobs (none now, null); how long till the next. */
    Fault stray;
    private float strayIn = STRAY_FIRST;
    // the lights gone wrong between jobs: the first so long after the light's job, then every so
    // long and up to so much more; left behind farther than this, it comes right of itself; mended, so many likes
    static final float STRAY_FIRST = 60f, STRAY_EVERY = 150f, STRAY_MORE = 150f, STRAY_LEFT = 700f;
    static final int STRAY_LIKES = 3000, STRAY_HEARTS = 34;
    private final int[] faultAt = new int[2];
    /** The containers in the port and the crane over them; she at its controls or not. */
    final Quay quay;
    /** She is in the crane's cabin, working it; so from home's list, with no way to go. */
    private boolean craning, craneSolo;
    /** The containers from home's list: the crane once the walls have drawn back. */
    private boolean yardNext;
    /** The town's helicopter left where the archive job left it, leaning on its broken skid; mended, hers. */
    private Copter parked;
    /** She flies it herself, on no job (it is hers). */
    private boolean flying;
    /** Waking at home after the concert: the next story is on the screen at once. */
    private boolean morning;
    private final float[] handsAt = new float[3];
    Concert concert;                         // the mission now, when it is the concert
    /** The hall come back to after the job is done (lit, her chair kept), while she stays in it. */
    private Concert stayed;
    /** The panel taken from home's screen on its own, no way to go: open over everything, or null. */
    Panel solo;
    private float soloClock;
    private boolean soloNext;
    static final String SAVE_PANEL_BEST = "best.panel";
    /** After the concert, waking at home: the dark lifting, seconds left. */
    private float dawn;
    static final float DAWN = 2.4f;
    private final float[] chairAt = new float[3];
    /** The brum's pickup left at the workshop after a tow, till she is far from it. */
    private Tow towed;
    /** The crowd at the water's edge while the puppy is out. */
    private Crowd crowd;
    private static final Chase[] NONE = new Chase[0];
    // in a delivery no brum is about: one far off and still, for those who keep an eye on the nearest
    private final Chase nobody = new Chase(1L, 100000, 100000, 0);
    private int beatsWere = Runner.BEATS;    // her beats left, as the last frame left them
    private boolean again;                   // the folder's mission once more, the rivals were first
    final Toss toss = new Toss();            // her hammer
    final Kit kit = new Kit();               // the thing out of a pickup's bed, in her hands or set up
    private Opening opening;                 // what the game opens with, while it plays
    private Dispatch dispatch;               // the dispatcher's room, while she is in it
    private float overFor;                   // how long since the last of the five was caught
    // kept between games, saved when a mission is done
    static final String SAVE_PANELS = "panels";
    static final String SAVE_MISSION = "mission", SAVE_PLATES = "plates", SAVE_LIKES = "likes", SAVE_CONTROLS = "foot";
    /** What a game kept before likes had won, taken over as likes once. */
    static final String SAVE_MONEY = "money";
    int plates, likes;
    /** The hearts up the right side, and the likes they count. */
    final Hearts hearts = new Hearts();
    static final float BACK_TO_ROOM = 3f;
    /** After the concert, the dark down: so long before she wakes at home. */
    static final float AFTER_DUSK = 0.5f;
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
        cars.walkers = walkers;
        walkers.cars = cars;
        byte[] moves = host.asset("moves.bin");
        if (moves != null) Frida.use(Moves.read(moves));
        if (FolkPack.town == null) FolkPack.town = FolkPack.read(host.asset("folk.bin"));
        if (Rooms.globe == null) Rooms.globe = host.asset("globe.bin");
        Lang.use(host, langCode());
        // where the last game left off: the mission to play next, and what has been won
        int next = host.loadInt(SAVE_MISSION);
        if (next > 1) mission.round = next - 1;
        // the kinds won; a game kept before they were counted: those of the missions it had played
        kindsWon = host.loadInt(SAVE_KINDS);
        kindsSkipped = host.loadInt(SAVE_SKIPPED);
        told = host.loadInt(SAVE_TOLD) != 0;
        if (kindsWon == 0 && kindsSkipped == 0)
            for (int n = 1; n < next; n++) kindsWon |= 1 << kindOf(n);
        Dispatch.paidKinds = host.loadInt(SAVE_PAID);
        loadCopter();
        quay = new Quay(0x9047C0DEL + host.loadInt(SAVE_FIELDS) * 7919L);
        quay.deal(fieldColours());
        // a game kept before pay was counted: the paid kinds it had won taken as paid
        if (Dispatch.paidKinds == 0) Dispatch.paidKinds = kindsWon & (1 << 2 | 1 << 3 | 1 << 4 | 1 << 5);
        // the way to play on foot: one finger, unless two thumbs are offered and were chosen (kept as 2)
        Runner.classic = !TWO_THUMBS_OFFERED || host.loadInt(SAVE_CONTROLS) != 2;
        navShut = host.loadInt(SAVE_NAV) == 1;
        coordsShown = host.loadInt(SAVE_COORDS) == 1;
        toss.tool = Math.max(0, Math.min(Toss.TOOLS - 1, host.loadInt(SAVE_TOOL)));
        plates = host.loadInt(SAVE_PLATES);
        likes = host.loadInt(SAVE_LIKES);
        if (likes == 0) likes = host.loadInt(SAVE_MONEY);
        hearts.start(likes);
        // every game begins at home: in its forecourt, a few steps before the door, facing it
        runner.put(City.HOME_DOOR_X, City.WALK_TOP, City.HOME_DOOR_Z - START_BACK, 0f);
        // the game begins with the runaways, straight from the dispatch, till that job is won once;
        // after it the town is hers, and the jobs are at home
        if (!passed(1)) {
            mission.round = 0;
            begin(runner.x, runner.z);
        } else {
            standFree();
            resume();
        }
        opening = new Opening(host, city);
    }

    /** The next mission, round (x, z): in turn five brums loose, the folder, a delivery, a call out. */
    private void begin(float x, float z) {
        if (driving != null) getOut();
        free = false;
        lost = false;
        forgetPoint();
        // where it began, for a try again to begin there too
        startX = x;
        startZ = z;
        startHome = indoors != null && indoors.home;
        int n = mission.round + 1;
        delivery = null;
        rescue = null;
        puppy = null;
        tow = null;
        archive = null;
        mend = null;
        port = null;
        lab = null;
        labCar = null;
        labSeen = -1;
        herd = null;
        cherry = null;
        dropFault();
        kit.putBack(city);
        flying = false;
        if (concert != null) concert.stop();
        concert = null;
        city.extra = null;
        if (Fault.its(n)) {
            mission.round = n;
            folder = null;
            // a light near her gone wrong (one at a time: any of itself put right first)
            stray = null;
            if (!Fault.pick(runner.x, runner.z, 0xFA17L + n * 31L, faultAt)) {
                faultAt[0] = City.HOME_I;
                faultAt[1] = City.HOME_J;
            }
            fault = new Fault(faultAt[0], faultAt[1], 0xFA17L + host.loadInt(SAVE_PANELS) * 0x9E3779B97F4A7C15L);
            chase = nobody;
        } else if (Cherry.its(n)) {
            mission.round = n;
            folder = null;
            // played again: the island shut as the hot heads left it, the bridge going up behind the screen
            if ((kindsWon & (1 << 12)) != 0) Island.shut = true;
            cherry = new Cherry();
            cherryAim = -1;
            chase = nobody;
        } else if (Herd.its(n)) {
            mission.round = n;
            folder = null;
            // played again: the island open as it was then
            Island.shut = false;
            herd = new Herd(city);
            chase = nobody;
        } else if (Lab.its(n)) {
            mission.round = n;
            folder = null;
            lab = new Lab();
            chase = nobody;
        } else if (Port.its(n)) {
            mission.round = n;
            folder = null;
            port = new Port();
            chase = nobody;
            if (quay.solved || quay.moves > 0) quay.deal(fieldColours());
        } else if (Mend.its(n)) {
            mission.round = n;
            folder = null;
            // played again: her own helicopter put aside, the broken one in the garden as it was then
            if ((kindsWon & (1 << 9)) != 0 && aside == null) {
                aside = parked;
                parked = brokenAtArchive();
            }
            if (parked == null) parked = brokenAtArchive();
            mend = new Mend(parked);
            chase = nobody;
        } else if (Archive.its(n)) {
            mission.round = n;
            folder = null;
            // played again: whatever helicopter she has put aside; the job's own waits on the roof
            if (parked != null && aside == null) {
                aside = parked;
                parked = null;
            }
            archive = new Archive();
            chase = nobody;
        } else if (Concert.its(n)) {
            mission.round = n;
            folder = null;
            // the panel's wiring new every time: a count of them kept, mixed into its seed
            int panels = host.loadInt(SAVE_PANELS) + 1;
            host.saveInt(SAVE_PANELS, panels);
            concert = new Concert(host, 0xC0C0L + n * 7919L, panels * 0x9E3779B97F4A7C15L + 0xBADC0DEL);
            chase = nobody;
        } else if (Tow.its(n)) {
            mission.round = n;
            folder = null;
            tow = new Tow();
            chase = nobody;
        } else if (Puppy.its(n)) {
            mission.round = n;
            folder = null;
            puppy = new Puppy();
            chase = nobody;
        } else if (Rescue.its(n)) {
            mission.round = n;
            folder = null;
            rescue = new Rescue(0x5A11L + n * 7919L);
            city.extra = rescue;
            chase = nobody;
        } else if (Delivery.its(n)) {
            mission.round = n;
            folder = null;
            delivery = new Delivery();
            chase = nobody;
        } else if (Folder.its(n)) {
            mission.round = n;
            // the same folder where it was for a try again: the same task
            folder = new Folder(0xF01DL + n * 7919L);
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
        // out again, she stands where every game begins: in the forecourt before the door
        runner.put(City.HOME_DOOR_X, City.WALK_TOP, City.HOME_DOOR_Z - START_BACK, 0f);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        int[] picks = nextChoice("home.next", "home.list");
        dispatch = new Dispatch(host, keptLines).choices(picks);
        if (!listing) dispatch.listed();
        letGo();
    }

    /**
     * Mission of a kind (the hunt 1, the folder 2, the delivery 3, the call out 4) begun now where she is: the next
     * number of that kind on from the one now, or the one now again, if it is of that kind (free,
     * on from the one last played).
     */
    private void chooseMission(int kind) {
        int n = Math.max(1, free ? mission.round + 1 : mission.round);
        while (kindOf(n) != kind) n++;
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

    /** For the desktop stand: whether the dispatcher's screen is up. */
    boolean standInDispatch() {
        return dispatch != null;
    }

    /** For the desktop stand: a touch on the sign of a question by the briefing; false if it is not shown. */
    boolean standHelp() {
        float[] at = new float[2];
        if (dispatch == null || !dispatch.helpAt(at)) return false;
        touch(DOWN, 0, at[0], at[1]);
        touch(UP, 0, at[0], at[1]);
        return true;
    }

    /** For the desktop stand: a touch on the sign of the way back over a choice; false if it is not shown. */
    boolean standBack() {
        float[] at = new float[2];
        if (dispatch == null || !dispatch.backAt(at)) return false;
        touch(DOWN, 0, at[0], at[1]);
        touch(UP, 0, at[0], at[1]);
        return true;
    }

    /** For the desktop stand: a touch on the story's page where it does not move: it closes. */
    void standStoryTap() {
        touch(DOWN, 0, W * 0.5f, h * 0.5f);
        touch(UP, 0, W * 0.5f, h * 0.5f);
    }

    /** For the desktop stand: in home's room, down on the sofa. */
    void standSofa() {
        if (driving != null) getOut();
        if (indoors != null) leave();
        indoors = homeRoom();
        indoors.enter(runner, false);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        sitDown();
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
    /** The report of a mission won, waiting for her at home: read on the sofa, before the next is offered. */
    String[] waiting;
    /** The report on the screen is of a mission lost: it is played again from where it began, after it. */
    private boolean retryAfter;
    /** Where the mission now began: home's room, or out of doors at (x, z). */
    private float startX, startZ;
    private boolean startHome;
    static final String SAVE_BEST = "best.";

    /** How long the mission just done took, seconds. */
    private float doneTime() {
        if (archive != null) return archive.doneAt;
        if (mend != null) return mend.doneAt;
        if (port != null) return port.doneAt;
        if (lab != null) return lab.doneAt;
        if (herd != null) return herd.doneAt;
        if (cherry != null) return cherry.doneAt;
        if (fault != null) return fault.doneAt;
        if (concert != null) return concert.doneAt;
        if (tow != null) return tow.doneAt;
        if (puppy != null) return puppy.doneAt;
        if (rescue != null) return rescue.doneAt;
        if (delivery != null) return delivery.doneAt;
        if (folder != null) return folder.doneAt;
        return mission.doneAt;
    }

    /**
     * Mission n done in so many seconds: kept as its best if it is (in tenths of a second); the
     * best before, seconds (0: none before).
     */
    private float record(int n, float seconds) {
        int was = host.loadInt(bestKey(n)), now = Math.max(1, Math.round(seconds * 10f));
        if (was <= 0 || now < was) host.saveInt(bestKey(n), now);
        return was / 10f;
    }

    /**
     * Each kind of mission, as it is made now (the hunt 1, the folder 2, the delivery 3, the call out 4): raised
     * when what one plays changes (its targets, its way, where it begins), so that a time kept
     * before is not set against one that is no longer the same task.
     */
    static final int[] REVISION = {0, 4, 4, 3, 4, 2, 2, 4, 2, 2, 1, 3, 2, 1, 1};
    /** How many kinds of mission there are; they come in turn, round and round. */
    static final int KINDS = 14;

    /** Mission n's kind: the hunt 1, the folder 2, the delivery 3. */
    static int kindOf(int n) {
        return n < 1 ? 1 : (n - 1) % KINDS + 1;
    }

    /** Where mission n's best time is kept, as its kind is made now. */
    static String bestKey(int n) {
        return SAVE_BEST + n + ".k" + kindOf(n) + ".r" + REVISION[kindOf(n)];
    }

    // ---- under the pause: the language, and the best times ----
    static final String SAVE_LANG = "lang";
    /** The language chosen: the phone's own (0), Russian (1), English (2). */
    private int langPick;
    private boolean resultsOpen;

    private String langCode() {
        langPick = host.loadInt(SAVE_LANG);
        return langPick == 1 ? "ru" : langPick == 2 ? "en" : host.language();
    }

    float langRowY() {
        return h * 0.46f + 64f;
    }

    float resultsRowY() {
        return langRowY() + 26f;
    }

    float storyRowY() {
        return resultsRowY() + 26f;
    }

    float skipRowY() {
        return storyRowY() + 26f;
    }

    /** The row for the job taken up again at its last point: under the one to put it by, if that is shown. */
    float pointRowY() {
        return canSkip() ? skipRowY() + 26f : skipRowY();
    }

    /** Whether the job now may be taken up again at its last point from under the pause. */
    boolean canGoBack() {
        return !free && !over() && pointSeen > 0 && !aboard() && !piloting() && pointKept();
    }

    /** For the desktop stand: the job now put by, as from under the pause, if it may be. */
    String standSkip() {
        if (!canSkip()) return "not to be put by";
        skip();
        return "put by: kinds put by " + Integer.toBinaryString(kindsSkipped);
    }

    /** A touch under the pause on one of its rows: the language turned to the next, or the best times shown. */
    private boolean pauseRow(float x, float y) {
        if (Math.abs(x - W * 0.5f) > 90f) return false;
        if (Math.abs(y - langRowY()) < 11f) {
            host.saveInt(SAVE_LANG, (langPick + 1) % 3);
            Lang.use(host, langCode());
            host.buzz(20);
            return true;
        }
        if (Math.abs(y - resultsRowY()) < 11f) {
            dispatch = new Dispatch(host, results());
            resultsOpen = true;
            host.buzz(20);
            return true;
        }
        if (!free && Math.abs(y - storyRowY()) < 11f) {
            openStory(kindOf(mission.round));
            return true;
        }
        if (canSkip() && Math.abs(y - skipRowY()) < 11f) {
            skip();
            return true;
        }
        if (canGoBack() && Math.abs(y - pointRowY()) < 11f) {
            fromPoint();
            host.buzz(20);
            return true;
        }
        return false;
    }

    /** Under the pause, in words: the language, and the way to the best times. */
    private void pauseFlat(Px f) {
        String name = langPick == 1 ? Lang.own(host, "ru") : langPick == 2 ? Lang.own(host, "en") : Lang.lines("lang.system", 0, null)[0];
        String l = Lang.lines("pause.lang", 0, null)[0].replace("{x}", name);
        String r = Lang.lines("pause.results", 0, null)[0];
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, teal = 0xFF7FE6D2;
        shadowed(f, l, Math.round(W * 0.5f - PixelFont.width(l, 1) * 0.5f), Math.round(langRowY() - 4f), 1, teal, ink);
        shadowed(f, r, Math.round(W * 0.5f - PixelFont.width(r, 1) * 0.5f), Math.round(resultsRowY() - 4f), 1, cream, ink);
        if (free) return;
        String st = Lang.lines("pause.story", 0, null)[0];
        shadowed(f, st, Math.round(W * 0.5f - PixelFont.width(st, 1) * 0.5f), Math.round(storyRowY() - 4f), 1, cream, ink);
        if (canSkip()) {
            String sk = Lang.lines(replaying() ? "pause.leave" : "pause.skip", 0, null)[0];
            shadowed(f, sk, Math.round(W * 0.5f - PixelFont.width(sk, 1) * 0.5f), Math.round(skipRowY() - 4f), 1, 0xFFFFB48A, ink);
        }
        if (canGoBack()) {
            String pt = Lang.lines("pause.point", 0, null)[0];
            shadowed(f, pt, Math.round(W * 0.5f - PixelFont.width(pt, 1) * 0.5f), Math.round(pointRowY() - 4f), 1, teal, ink);
        }
    }

    /** The best times kept, mission by mission, the latest last (as many as the screen holds). */
    private String[] results() {
        java.util.ArrayList<String> rows = new java.util.ArrayList<String>();
        int last = Math.max(host.loadInt(SAVE_MISSION), mission.round + 1) + 3;
        for (int n = 1; n <= last; n++) {
            int best = host.loadInt(bestKey(n));
            if (best <= 0) continue;
            rows.add(n + "  " + Lang.lines("kind." + kindOf(n), 0, null)[0] + "  " + clock(best / 10f));
        }
        while (rows.size() > 8) rows.remove(0);
        // the latest at the top, the first ones at the foot
        java.util.Collections.reverse(rows);
        if (rows.isEmpty()) rows.add(Lang.lines("results.none", 0, null)[0]);
        String[] shell = Lang.lines("results", 0, null);
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String line : shell) {
            if (line.equals("{rows}")) out.addAll(rows);
            else out.add(line);
        }
        return out.toArray(new String[0]);
    }

    /**
     * The mission just lost, again: she is put where it began (in home's room, if it began there),
     * and it begins the same, its targets where they were.
     */
    private void retry() {
        if (driving != null) getOut();
        errand = NO_ERRAND;
        runner.stopLead();
        for (Pickup p : duty.all) p.load = 0;
        mission.round--;
        if (startHome) {
            if (indoors != null) leave();
            indoors = homeRoom();
            indoors.enter(runner, false);
        } else {
            if (indoors != null) leave();
            runner.put(startX, City.WALK_TOP, startZ, runner.heading);
        }
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        boolean home = startHome;
        float x = startX, z = startZ;
        begin(x, z);
        startHome = home;
        overFor = 0f;
        caughtSeen = 0;
        nextSlot = 0;
        struckSeen = 0;
        whiteOut = WHITE_OUT;
        letGo();
    }

    /** The brums of the mission now. */
    Chase[] brums() {
        if (free || delivery != null || rescue != null || puppy != null || tow != null || concert != null || archive != null || mend != null || port != null || lab != null || herd != null || cherry != null || fault != null) return NONE;
        return folder != null ? folder.rivals : mission.brums;
    }

    /** Whether the mission now is a hunt. */
    boolean hunt() {
        return !free && folder == null && delivery == null && rescue == null && puppy == null && tow == null && concert == null && archive == null && mend == null && port == null
                && lab == null && herd == null && cherry == null && fault == null;
    }

    /** Whether the mission now is done with, won or not. */
    boolean over() {
        if (archive != null) return archive.over();
        if (mend != null) return mend.over();
        if (port != null) return port.over();
        if (lab != null) return lab.over();
        if (herd != null) return herd.over();
        if (cherry != null) return cherry.over();
        if (fault != null) return fault.over();
        if (concert != null) return concert.over();
        if (tow != null) return tow.over();
        if (puppy != null) return puppy.over();
        if (rescue != null) return rescue.over();
        if (delivery != null) return delivery.over();
        if (folder == null) return mission.over();
        return folder.theirs || folder.delivered();
    }

    /** The briefing for the mission now. */
    private String[] briefing() {
        return again ? Dispatch.again(mission.round) : Dispatch.briefing(mission.round);
    }

    static final String SAVE_PAID = "paid";
    /** The likes the mission just done brought (0: none, a story liked before, or lost). */
    private int paidNow;

    // ---- what lies behind a mission: a page over everything, from the briefing's sign or the pause ----
    /** The page of a mission's story now open, or null. */
    Story story;

    private void openStory(int kind) {
        if (kind < 1) return;
        story = Story.of(kind);
        letGo();
        host.buzz(20);
        host.sound(Synth.TOCK, 0.3f, 1.2f);
    }

    /** The page over the frame, if one is open: nothing of the frame's own buttons shows through it. */
    private void storyFlat(DrawList d, Px f) {
        if (story == null) return;
        story.draw(f, Dispatch.font(host));
        d.badgeN = 0;
        d.tube[0] = d.tube[2] = 0f;
    }

    /** Whether the mission just over was lost: the hunt's time out, the folder taken first, the puppy got out by others. */
    private boolean failed() {
        if (puppy != null || tow != null || concert != null || archive != null || mend != null || port != null || lab != null || herd != null || cherry != null || fault != null) return false;
        return hunt() ? mission.late : folder != null && folder.theirs;
    }

    /** The report on the mission just done, with the likes it brought under what was done. */
    private String[] report() {
        return liked(reportLines(), paidNow);
    }

    /** A report as it is: the likes it brought are seen in the hearts' fountain and the sum on the screen, not told in words. */
    private String[] liked(String[] lines, int got) {
        return lines;
    }

    /** The report's own lines, before the likes. */
    private String[] reportLines() {
        if (archive != null) return Dispatch.archived(mission.round, clock(archive.doneAt));
        if (mend != null) return Dispatch.mended(mission.round, clock(mend.doneAt));
        if (port != null) return Dispatch.ported(mission.round, clock(port.doneAt), likes, paidNow);
        if (lab != null) return Dispatch.navigated(mission.round, clock(lab.doneAt), likes, paidNow);
        if (herd != null) return Dispatch.herded(mission.round, clock(herd.doneAt), likes, paidNow);
        if (fault != null) return Dispatch.faulted(mission.round, clock(fault.doneAt));
        if (cherry != null) return Dispatch.sealed(mission.round, clock(cherry.doneAt), likes, paidNow);
        if (concert != null) return Dispatch.concerted(mission.round, clock(concert.doneAt), likes, paidNow);
        if (tow != null) return Dispatch.towed(mission.round, clock(tow.doneAt));
        if (puppy != null) return Dispatch.puppied(mission.round, clock(puppy.doneAt), likes, paidNow);
        if (rescue != null) return Dispatch.rescued(mission.round, clock(rescue.doneAt), likes, paidNow);
        if (delivery != null) return Dispatch.delivered(mission.round, clock(delivery.doneAt), likes, paidNow);
        if (folder == null && mission.late) return Dispatch.late(mission.round, mission.caught());
        if (folder == null) return Dispatch.report(mission.round, clock(mission.doneAt));
        if (folder.hers) return Dispatch.paid(mission.round, clock(folder.doneAt), likes, paidNow);
        return Dispatch.lost(mission.round, clock(folder.doneAt));
    }

    /** A count with its thousands set apart: 100 000. */
    static String grouped(long m) {
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
        boolean lost = failed();
        // a story brings its likes once, the first time it is done; a game, every time it is won
        paidNow = lost ? 0 : Dispatch.due(mission.round);
        if (paidNow > 0) {
            like(paidNow, WIN_HEARTS);
            Dispatch.paidKinds |= 1 << kindOf(mission.round);
            host.saveInt(SAVE_PAID, Dispatch.paidKinds);
        }
        waitingLikes = paidNow;
        if (hunt()) plates += mission.caught();
        // lost, the same mission is played again
        host.saveInt(SAVE_MISSION, lost ? mission.round : mission.round + 1);
        host.saveInt(SAVE_PLATES, plates);
        saveLikes();
    }

    /** For the desktop stand: as if the job just done had brought so many likes (for its report). */
    void standPaid(int n) {
        paidNow = n;
    }

    /** The dispatcher's room now, with the briefing or (done) the report (the desktop stand). */
    void standDispatch(boolean done) {
        opening = null;
        Frida.eyes = 1f;
        Frida.eyeSize = 1f;
        Frida.eyeHeat = 0f;
        runner.update(0f, city, cars, walkers);
        dispatch = new Dispatch(host, done ? report() : briefing());
        if (!done) dispatch.story(kindOf(mission.round));
    }

    /** For the desktop stand: her put down in the room she is in, at (u, v) of its frame, facing in. */
    void standInRoom(float u, float v) {
        if (indoors == null) return;
        runner.put(indoors.wx(u, v), Indoors.FLOOR, indoors.wz(u, v), indoors.heading(0f, 1f));
        runner.recenter();
    }

    /** For the desktop stand: the concert's panel, if open: so many of its pieces mended; or (pieces &lt; 0) its switch touched. */
    String standPanel(int pieces) {
        Panel p = solo != null ? solo : concert != null && concert.panelOpen ? concert.panel : null;
        if (p == null) return "no panel open";
        if (pieces >= 0) {
            p.standMend(pieces);
            return p.whole ? "the panel mended" : p.lampsOnly() ? "the lamps lit, the panel not whole yet" : "the panel not whole yet";
        }
        p.standLever(goal);
        touch(DOWN, 0, goal[0], goal[1]);
        touch(UP, 0, goal[0], goal[1]);
        return p.on ? "the switch thrown" : "the switch not thrown";
    }

    /** No mission now, the town hers till she goes home for the next (a game begun past the first, and the desktop stand). */
    void standFree() {
        free = true;
        folder = null;
        delivery = null;
        rescue = null;
        puppy = null;
        crowd = null;
        tow = null;
        if (archive != null) parked = archive.copter;
        archive = null;
        mend = null;
        port = null;
        lab = null;
        herd = null;
        cherry = null;
        dropFault();
        if (concert != null) concert.stop();
        concert = null;
        chase = nobody;
    }

    /** Mission n now, round where she is (the desktop stand). */
    void standMission(int n) {
        mission.round = n - 1;
        begin(runner.x, runner.z);
    }

    /** For the desktop stand: her at the wheel at (x, z), facing so: the pickup she drives moved there, or a duty pickup put there. */
    // ---- the island's yacht: at the helm ----

    /** A frame at the helm: the yacht as the thumb on the wheel has it; aground, a jolt in the hand. */
    private void sail(float dt, Pickup p, boolean go, float wheel) {
        p.drive(dt, go, wheel, brakeIn, false, city, cars, walkers);
        if (p.bumped) {
            host.sound(Synth.WOOD, 0.7f, 0.6f);
            host.buzz(50);
        }
        p.seat(seatAt);
        runner.aboard = true;
        runner.ride(dt, city, p.x, p.y, p.z, p.heading, p.speed, seatAt[0], seatAt[1], seatAt[2], p.steer);
    }

    /**
     * Out of the yacht: onto the pier, if it lies by it; onto the sand, if it is by the shore;
     * else over the side into the water, and she swims. It stays where it is.
     */
    private void ashore(Pickup p) {
        float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading);
        float bx = 0f, bz = 0f, by = City.WATER;
        boolean found = false;
        for (int e = -1; e <= 1 && !found; e += 2)
            for (float off = 0.6f; off <= 2.8f && !found; off += 0.55f) {
                float ax = e * (Island.BEAM + off);
                float qx = p.x + c * ax + s * Pickup.HELM_Z, qz = p.z - s * ax + c * Pickup.HELM_Z;
                if (Island.onDeck(qx, qz, 0.35f)) {
                    bx = qx;
                    bz = qz;
                    by = Island.DECK;
                    found = true;
                } else if (city.ground(qx, qz) > City.WATER + 0.05f) {
                    bx = qx;
                    bz = qz;
                    by = city.ground(qx, qz);
                    found = true;
                }
            }
        if (!found) {
            // over the side, the left, into the water
            float ax = -(Island.BEAM + 0.9f);
            bx = p.x + c * ax + s * Pickup.HELM_Z;
            bz = p.z - s * ax + c * Pickup.HELM_Z;
        }
        runner.getOut(bx, by, bz, p.heading);
        host.sound(found ? Synth.WOOD : Synth.SPLASH, 0.4f, found ? 1.2f : 0.9f);
        doorArmed = false;
        runner.recenter();
        buttonHeld = true;
    }

    /** For the desktop stand: the island's yacht put at (x, z), headed so, her at its helm. */
    boolean standBoatAt(float x, float z, float heading) {
        Pickup b = duty.boat();
        if (b == null) return false;
        if (driving == b) getOut();
        b.putAt(x, z, heading);
        return standBoat();
    }

    /** For the desktop stand: her at the helm of the island's yacht, wherever it is. */
    boolean standBoat() {
        Pickup b = duty.boat();
        if (b == null) return false;
        if (driving != b) {
            getOut();
            getIn(b);
        }
        drive(0f);
        runner.recenter();
        return true;
    }

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
        fingered(type, id, x, y);
        // the leading finger lifted while something over the frame had it (a screen that came on
        // under it): lifted all the same, or every finger after it would be taken for a second one
        if (type == UP && id == leadFinger) {
            leadFinger = -1;
            buttonHeld = false;
            if (classicDown) {
                classicDown = false;
                runner.touch(UP, x, y);
            }
        }
    }

    private void fingered(int type, int id, float x, float y) {
        // walked in at the vets' door with the puppy: hers again once the job is done
        if (puppy != null && puppy.entering() && type != LEFT && type != BACK) return;
        // the torch seen close: a touch lets it go
        if (torchShowT >= 0f && opening == null && dispatch == null && story == null) {
            if (type == DOWN) torchShown();
            return;
        }
        // the ring of her tools, out of its button over her mallet: the finger is its own till lifted
        if (pieTouch(type, id, x, y)) return;
        // the navigator touched: folded or opened, wherever she is
        if (type == DOWN && !paused && onNav(x, y)) {
            navShut = !navShut;
            host.saveInt(SAVE_NAV, navShut ? 1 : 2);
            host.buzz(12);
            return;
        }
        // the compass touched: where she is shown in numbers, or not
        if (type == DOWN && !paused && onCompass(x, y)) {
            coordsShown = !coordsShown;
            host.saveInt(SAVE_COORDS, coordsShown ? 1 : 0);
            host.buzz(12);
            return;
        }
        if (type == BACK) return;
        if (type == LEFT) {
            // the game left and come back to: paused, every finger let go
            if (opening == null && dispatch == null) paused = true;
            letGo();
            return;
        }
        if (id < 0 || id >= FINGERS) return;
        if (story != null) {
            // the story of the mission over everything: the first finger turns its page or closes it
            if (id == 0 || type == DOWN) story.touch(type, x, y);
            if (story.closed) {
                story = null;
                letGo();
            }
            return;
        }
        if (concert != null && concert.panelOpen) {
            // the panel over everything: a finger down turns a piece or throws the switch
            if (type == DOWN) concert.panel.touch(x, y, host);
            return;
        }
        if (solo != null) {
            if (type == DOWN) solo.touch(x, y, host);
            return;
        }
        if (faultWires() != null) {
            faultWires().touch(type, x, y, host);
            return;
        }
        if (opening != null) {
            if (type == DOWN) opening.skip();
            return;
        }
        if (dispatch != null) {
            if (type == DOWN && dispatch.onHelp(x, y)) {
                openStory(dispatch.storyKind());
                return;
            }
            if (type == DOWN && dispatch.listAt(x, y)) {
                // the list on the screen after the key
                int[] p = listChoice("list");
                dispatch.then(keptLines, p);
                return;
            }
            if (type == DOWN && dispatch.leaveAt(x, y)) {
                // out of her head with nothing picked: up from the sofa, the town hers
                standAfter = true;
                return;
            }
            if (type == DOWN && dispatch.choosing()) {
                int c = dispatch.pickAt(x, y);
                if (c == Dispatch.STORIES) {
                    dispatch.then(storiesChoice(), storyPicks);
                } else if (c == Dispatch.TO_LIST) {
                    int[] p = listChoice("list");
                    dispatch.then(keptLines, p);
                } else if (c == Dispatch.SOLO) {
                    // the panel on its own: it opens once the walls have drawn back
                    soloNext = true;
                } else if (c == Dispatch.YARD) {
                    // the containers, no way to go: at the crane once the walls have drawn back
                    yardNext = true;
                } else if (c > 0 && c < Dispatch.REST) {
                    chooseMission(c);
                    dispatch.then(briefing()).story(kindOf(mission.round));
                } else if (c == 0 && free) {
                    // on to the next: a kind not yet won first, while there is one; then in turn.
                    // Lost, the same again, from where it began
                    again = lost;
                    if (lost && !startHome) runner.put(startX, City.WALK_TOP, startZ, runner.heading);
                    begin(runner.x, runner.z);
                    overFor = 0f;
                    caughtSeen = 0;
                    nextSlot = 0;
                    struckSeen = 0;
                    dispatch.then(briefing()).story(kindOf(mission.round));
                    again = false;
                }
            } else if (type == DOWN) dispatch.touch();
            return;
        }
        if (piloting()) {
            pilotTouch(type, id, x, y);
            return;
        }
        if (craning) {
            craneTouch(type, id, x, y);
            return;
        }
        if (driving != null) {
            leadFinger = -1;
            carTouch(type, id, x, y);
            return;
        }
        if (aboard()) {
            // a passenger: the pause as ever, nothing else to do but look
            if (type == DOWN && paused) {
                if (!pauseRow(x, y)) paused = false;
            } else if (type == DOWN && onButton(x, y, pauseX(), pauseY())) paused = true;
            leadFinger = -1;
            return;
        }
        if (inChair()) {
            // in her chair in the hall: the pause as ever; anywhere else she gets up (not on the
            // job, where the music is heard to its end)
            if (type == DOWN && paused) {
                if (!pauseRow(x, y)) paused = false;
            } else if (type == DOWN && onButton(x, y, pauseX(), pauseY())) {
                paused = true;
            } else if (type == DOWN && hall().open) offChair();
            leadFinger = -1;
            return;
        }
        if (seated()) {
            // sitting: the pause as ever, its rows under it; anywhere else, she gets up
            if (type == DOWN && paused) {
                if (!pauseRow(x, y)) paused = false;
            } else if (type == DOWN && onButton(x, y, pauseX(), pauseY())) {
                paused = true;
            } else if (type == DOWN && sofaAsked) standUp();
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
            else if (!pauseRow(x, y)) paused = false;
            role[id] = NOBODY;
            return;
        }
        if (onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            letGoAll();
            return;
        }
        // a sign: she goes where it says by herself
        int sgn = signHere(x, y);
        if (sgn >= 0) {
            errand(sgn);
            return;
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
            default:
        }
        role[id] = NOBODY;
    }

    /** Every finger let go of at once: paused, in or out of a car. */
    private void letGoAll() {
        for (int i = 0; i < FINGERS; i++) role[i] = NOBODY;
        stickFinger = -1;
        runner.stick(0f, 0f, false);
        wheelFinger = -1;
    }

    /**
     * Whatever the fingers were doing, forgotten, those still on the glass heard no more till they
     * are lifted: when she gets into the pickup or out of it, goes into her head, is left. A finger
     * held running does not become the throttle.
     */
    private void letGo() {
        letGoAll();
        leftStick = rightStick = -1;
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

    // ---- the navigator: a map in the corner up on the right, the way on it to the mission's mark ----
    final Nav nav = new Nav();
    static final String SAVE_NAV = "nav";
    private boolean navShut;
    private float navTime = -1f;
    private final float[] navDots = new float[48], testerAt = new float[3];
    /** For the desktop stand: the navigator out, whatever has been played. */
    boolean standNav;
    // the folded navigator: a small square, its side
    static final int NAV_TILE = 22;

    int navX() {
        return W - Nav.SIDE - 8;
    }

    static final int NAV_Y = 36;

    /** Whether (x, y) is on the navigator, open or folded. */
    private boolean onNav(float x, float y) {
        if (!navShows()) return false;
        if (navShut) return x > W - 8 - NAV_TILE - 6 && y > NAV_Y - 6 && y < NAV_Y + NAV_TILE + 6;
        return x > navX() - 4 && y > NAV_Y - 4 && y < NAV_Y + Nav.TALL + 4;
    }

    /** Whether the navigator is out now: on the street, the eye not elsewhere. */
    private boolean navShows() {
        return navGiven() && indoors == null && dispatch == null && !craning && story == null && opening == null && solo == null;
    }

    /** Whether the navigator is hers: fitted at the laboratory (the trial on, or done once); the stand's own. */
    boolean navGiven() {
        return standNav || (kindsWon & (1 << 11)) != 0 || lab != null && lab.fitted();
    }

    /** The navigator, over the frame: the map and its strip, or folded. */
    private void navFlat(Px f) {
        float dt = navTime < 0f ? 0f : Math.max(0f, Math.min(0.1f, time - navTime));
        navTime = time;
        float x = driving != null ? driving.x : runner.x, z = driving != null ? driving.z : runner.z;
        float heading = driving != null ? driving.heading : runner.heading;
        nav.update(dt, x, z, aboard() || piloting(), heading);
        if (compassShows()) compassFlat(f);
        if (!navShows()) return;
        if (navShut) {
            int x0 = W - 8 - NAV_TILE;
            for (int py = 0; py < NAV_TILE; py++)
                for (int px = 0; px < NAV_TILE; px++) {
                    int dx = Math.max(0, Math.max(4 - px, px - (NAV_TILE - 5))), dy = Math.max(0, Math.max(4 - py, py - (NAV_TILE - 5)));
                    if (dx * dx + dy * dy <= 16) f.set(x0 + px, NAV_Y + py, Nav.BLOCK);
                }
            Nav.point(f, x0, NAV_Y, NAV_TILE * 0.5f, NAV_TILE * 0.55f, 0f, -1f, 6f, nav.led() ? Nav.LINE : Nav.TEXT);
            return;
        }
        float speed = driving != null ? driving.speed : 0f;
        // the other brums trying theirs out: dots on it
        int dots = 0;
        if (lab != null && lab.step == Lab.TRIAL)
            for (int k = 0; k < Lab.testers(); k++) {
                Lab.tester(k, time, testerAt);
                navDots[dots * 2] = testerAt[0];
                navDots[dots * 2 + 1] = testerAt[1];
                dots++;
            }
        // the hot brums after her
        if (herd != null) dots += herd.dots(navDots, dots);
        nav.draw(f, navX(), NAV_Y, x, z, runner.eyeYaw, heading, speed, dt, navDots, dots);
        nav.strip(f, navX(), NAV_Y + Nav.SIDE);
        // the map open: where she is, in numbers, under it
        String at = coords();
        shadowed(f, at, W - 8 - PixelFont.width(at, 1), NAV_Y + Nav.TALL + 5, 1, Nav.TEXT, Nav.INK);
    }

    // ---- the compass: till the navigator is hers, a tile where it will be, north on it; touched,
    // where she is in numbers under it (to tell a place by), touched again, not ----
    static final String SAVE_COORDS = "coords";
    private boolean coordsShown;

    /** Whether the compass is out: the navigator not hers yet, on the street, the eye not elsewhere, not paused. */
    private boolean compassShows() {
        return !navGiven() && indoors == null && dispatch == null && !craning && !piloting() && !paused && story == null && opening == null
                && solo == null;
    }

    /** Whether (x, y) is on the compass. */
    private boolean onCompass(float x, float y) {
        return compassShows() && onButton(x, y, compassX(), pauseY());
    }

    /** Where she is (or what she drives), in whole meters: across, along, how high. */
    private String coords() {
        float x = driving != null ? driving.x : runner.x, z = driving != null ? driving.z : runner.z;
        float y = driving != null ? driving.y : runner.y;
        return "X " + Math.round(x) + "  Z " + Math.round(z) + "  H " + Math.round(y);
    }

    /** Under the compass, touched: where she is, in numbers, its right edge under the button's. */
    private void compassFlat(Px f) {
        if (!coordsShown) return;
        String at = coords();
        shadowed(f, at, compassX() + PAUSE_HALF - PixelFont.width(at, 1), pauseY() + PAUSE_HALF + 8, 1, Nav.TEXT, Nav.INK);
    }

    // ---- the rings: a small button floating over her mallet, and one over the bed of a pickup she
    // stands by. Held, what it holds opens in a ring round it, the first straight up and the rest
    // on round clockwise; slid onto one, it is lit; lifted there, it is chosen. Lifted in the middle,
    // nothing. A hand that knows the way slides and lifts before the ring is drawn at all.
    // Her ring: her mallet, her screwdriver, her torch, and, while a thing of a bed is out, back to its bed.
    // A bed's ring: what the bed holds, as the job has it (see Kit) ----
    static final String SAVE_TOOL = "tool";

    // ---- the torch: in her ring from the first. In a dark room its light goes out ahead of her
    // hand, a cone; without it, all but a glimmer round her is gone. There, while it is not in her
    // hand, her button pulses and a finger shows the way to it in the ring. The first time she
    // takes it up in the dark it is seen close, once: the eye beside her, the click, the light ----
    static final String SAVE_TORCH = "torch.seen";
    // the light: how far it reaches, the cosine of half its cone, how dark the rest; without it, how far her glimmer goes
    static final float TORCH_REACH = 26f, TORCH_CONE = 0.9f, TORCH_DARK = 0.93f, GLIMMER = 2.6f, GLIMMER_DARK = 0.95f;
    // the lesson: how long it goes round; when the finger is down, the ring out, the finger slid, lifted
    static final float LESSON = 3.2f, LESSON_DOWN = 0.35f, LESSON_RING = 0.6f, LESSON_SLID = 1.5f, LESSON_UP = 2f;
    // seen close: how long; when the click comes; the eye before her: so far ahead, so far to her right, so high; what it
    // looks at, her hand: so far ahead of her, so far to her right, so high
    static final float TORCH_SHOW = 2.4f, TORCH_CLICK = 0.75f, SHOW_AHEAD = 2.1f, SHOW_SIDE = 0.9f, SHOW_UP = 3f,
            SHOW_LOOK = 0.5f, SHOW_LOOK_SIDE = 0.3f, SHOW_LOOK_UP = 1.35f;
    private float torchShowT = -1f;
    private final float[] beam = new float[6], torchEye = new float[5], lessonAt = new float[2];

    /** Whether the torch's light is on: in her hand, nothing of a bed instead, and (seen close) clicked on. */
    private boolean torchOn() {
        return toss.tool == Toss.TORCH && kit.held == Kit.NONE && toss.state == Toss.HELD && driving == null
                && (torchShowT < 0f || torchShowT >= TORCH_CLICK);
    }

    /** The dark room's one light: her torch's cone, or the glimmer round her. */
    private void torchLight(DrawList d) {
        Runner r = runner;
        if (torchOn()) {
            toss.torchAt(r, beam);
            d.spot[0] = beam[0];
            d.spot[1] = beam[1];
            d.spot[2] = beam[2];
            d.spot[3] = TORCH_REACH;
            d.spot[4] = TORCH_DARK;
            d.spot[5] = beam[3];
            d.spot[6] = beam[4];
            d.spot[7] = beam[5];
            d.spot[8] = TORCH_CONE;
        } else {
            d.spot[0] = r.x;
            d.spot[1] = r.y + 1.6f;
            d.spot[2] = r.z;
            d.spot[3] = GLIMMER;
            d.spot[4] = GLIMMER_DARK;
            d.spot[8] = 0f;
        }
    }

    /**
     * A ring's way shown (its things laid as pieItems has them): its button at (bx, by) pulsing; a
     * finger down on it, the ring out faint, the finger slid onto the thing aimed at and lifted, round again.
     */
    private void ringLesson(DrawList d, float bx, float by, float half, int sign, int thing) {
        float t = time % LESSON;
        d.badge(bx, by, half, sign, 0.85f, 0.4f + 0.4f * (float) Math.sin(time * 6f));
        float cx = Math.max(PIE_R + PIE_ITEM + 4f, Math.min(W - PIE_R - PIE_ITEM - 4f, bx));
        float cy = Math.max(PIE_R + PIE_ITEM + 60f, Math.min(h - PIE_R - PIE_ITEM - 10f, by));
        int aim = 0;
        for (int k = 0; k < pieN; k++) if (pieItems[k] == thing) aim = k;
        float ring = t < LESSON_DOWN ? 0f : t < LESSON_RING ? (t - LESSON_DOWN) / (LESSON_RING - LESSON_DOWN) : t < LESSON_UP ? 1f : Math.max(0f, 1f - (t - LESSON_UP) / 0.3f);
        boolean there = t >= LESSON_SLID && t < LESSON_UP + 0.3f;
        for (int k = 0; k < pieN && ring > 0f; k++) {
            float a = pieAngle(k);
            float sx = cx + (float) Math.sin(a) * PIE_R, sy = cy - (float) Math.cos(a) * PIE_R;
            boolean hot = k == aim && there;
            d.badge(sx, sy, hot ? PIE_ITEM * 1.25f : PIE_ITEM, pieSign(pieItems[k]), (hot ? 0.95f : 0.5f) * ring, hot ? 0.45f : 0f);
        }
        // the finger: from the button to the thing, eased; there a moment, then gone
        if (t >= LESSON_UP + 0.3f) return;
        float a = pieAngle(aim);
        float tx = cx + (float) Math.sin(a) * PIE_R, ty = cy - (float) Math.cos(a) * PIE_R;
        float go = t < LESSON_RING ? 0f : Math.min(1f, (t - LESSON_RING) / (LESSON_SLID - LESSON_RING));
        go = go * go * (3f - 2f * go);
        float shown = Math.min(1f, t / 0.2f) * (t < LESSON_UP ? 1f : Math.max(0f, 1f - (t - LESSON_UP) / 0.3f));
        lessonAt[0] = bx + (tx - bx) * go;
        lessonAt[1] = by + (ty - by) * go;
        d.badge(lessonAt[0] + 5f, lessonAt[1] + 12f, 15f, Badges.ONE_FINGER, 0.95f * shown, t >= LESSON_DOWN && t < LESSON_UP ? 0.35f : 0f);
    }

    /** The torch taken up for the first time in the dark: she stands, seen close from beside her. */
    private void torchShow() {
        host.saveInt(SAVE_TORCH, 1);
        torchShowT = 0f;
        errand = NO_ERRAND;
        runner.stopLead();
        letGo();
    }

    /** Seen close a frame on: she still, the eye beside her on her hand and the dark ahead; the click; then hers again. */
    private void torchStep(float dt) {
        if (torchShowT < 0f) return;
        float was = torchShowT;
        torchShowT += dt;
        if (was < TORCH_CLICK && torchShowT >= TORCH_CLICK) {
            host.sound(Synth.LATCH, 0.45f, 1.7f);
            host.buzz(25);
        }
        if (torchShowT >= TORCH_SHOW || indoors == null) {
            torchShown();
            return;
        }
        Runner r = runner;
        r.held = true;
        r.speed = 0f;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading), sx = fz, sz = -fx;
        torchEye[0] = r.x + fx * SHOW_AHEAD + sx * SHOW_SIDE;
        torchEye[1] = r.y + SHOW_UP;
        torchEye[2] = r.z + fz * SHOW_AHEAD + sz * SHOW_SIDE;
        float lx = r.x + fx * SHOW_LOOK + sx * SHOW_LOOK_SIDE - torchEye[0], lz = r.z + fz * SHOW_LOOK + sz * SHOW_LOOK_SIDE - torchEye[2];
        torchEye[3] = (float) Math.atan2(lx, lz);
        torchEye[4] = (float) Math.atan2(r.y + SHOW_LOOK_UP - torchEye[1], Math.sqrt(lx * lx + lz * lz));
        r.eyeAt(torchEye);
    }

    /** Seen close no more: hers again, the eye behind her. */
    private void torchShown() {
        torchShowT = -1f;
        runner.held = false;
        runner.recenter();
    }
    // the buttons: half their size; the ring: how far out its things are, how big; the middle that
    // means no; how long the finger stays before the ring is drawn
    static final float PIE_HALF = 11f, BED_HALF = 17f, PIE_R = 48f, PIE_ITEM = 17f, PIE_DEAD = 16f, PIE_REVEAL = 0.26f;
    // what a ring holds: her tools, back to the bed, a bed's things (KIT + the thing)
    static final int PIE_MALLET = 0, PIE_SCREWDRIVER = 1, PIE_BACK = 2, PIE_TORCH = 3, PIE_KIT = 10;
    private int pieFinger = -1, pieHot = -1, pieN;
    float pieX, pieY;
    private float pieCx, pieCy, pieAt;
    float bedX, bedY;
    private boolean pieSeen, bedSeen, pieBed;
    private final int[] pieItems = new int[8], bedThings = new int[4];
    private final float[] pieAnchor = new float[3];
    /** The pickup whose bed's button is up now (she stands by its back), if any. */
    Pickup bedNow;

    /** The sign of each thing in a ring. */
    private static int pieSign(int k) {
        if (k >= PIE_KIT) return Kit.sign(k - PIE_KIT);
        return k == PIE_SCREWDRIVER ? Badges.SCREWDRIVER : k == PIE_TORCH ? Badges.TORCH : k == PIE_BACK ? Badges.BED : Badges.MALLET;
    }

    /**
     * Where ring thing k (its place in the ring now) stands, as an angle clockwise from up: her own
     * ring keeps each in its place (the mallet up, the screwdriver down, the torch left, back to the
     * bed right); a bed's ring, its things evenly round.
     */
    private float pieAngle(int k) {
        if (!pieBed && pieItems[k] < PIE_KIT) {
            int t = pieItems[k];
            return (float) Math.PI * (t == PIE_SCREWDRIVER ? 1f : t == PIE_TORCH ? 1.5f : t == PIE_BACK ? 0.5f : 0f);
        }
        return k * 2f * (float) Math.PI / pieN;
    }

    /** The sign on her button: what is in her hands. */
    private int handSign() {
        return kit.held != Kit.NONE ? Kit.sign(kit.held) : pieSign(pieOf(toss.tool));
    }

    /** The ring's thing for a tool of hers. */
    private static int pieOf(int tool) {
        return tool == Toss.SCREWDRIVER ? PIE_SCREWDRIVER : tool == Toss.TORCH ? PIE_TORCH : PIE_MALLET;
    }

    /** Whether the buttons are there: on foot, her hands free to choose (not at a wheel, a seat, a ladder, the water, a screen). */
    private boolean pieOffered() {
        Runner r = runner;
        return driving == null && !paused && !aboard() && !piloting() && !seated() && !inChair() && dispatch == null && story == null
                && opening == null && !r.climbing() && !r.swimming() && !r.hidden && !craning && solo == null && faultWires() == null
                && !(concert != null && concert.panelOpen) && toss.state == Toss.HELD && torchShowT < 0f
                && !(puppy != null && puppy.entering());
    }

    /** Whether she stands at the back of pickup p, near enough to take a thing out of its bed. */
    private boolean atBed(Pickup p) {
        float bx = p.x - (float) Math.sin(p.heading) * 2.4f, bz = p.z - (float) Math.cos(p.heading) * 2.4f;
        float dx = bx - runner.x, dz = bz - runner.z;
        return dx * dx + dz * dz < Kit.BED_NEAR * Kit.BED_NEAR;
    }

    // ---- the ring over a thing: where a job wants something used (the heap in the pass, the brum's
    // pickup in the quarry, a light's box), a button over it; held, the ring of what may be used on
    // it: her own things, and with a pickup near, what its bed holds. Chosen, a thing of a bed she goes
    // to the bed for; a tool of her own is in her hand at once. In the dark, the torch's button by
    // her, a touch: on ----
    static final int OBJ_NONE = 0, OBJ_HEAP = 1, OBJ_STUCK = 2, OBJ_BOX = 3;
    // how far off the button over a thing shows; how near her a pickup is for its bed to be offered
    static final float OBJ_FAR = 36f, PICKUP_NEAR = 45f;
    private int objKind;
    private final float[] objAt = new float[3];

    /** What wants a thing used on it now, near her, and where (into objAt): none, the heap, the brum's pickup, a light's box. */
    private int objWanted() {
        Runner r = runner;
        if (rescue != null && rescue.heapSaid && rescue.wanting() && kit.held != Kit.CHARGE) {
            rescue.face(r.x, r.z, objAt);
            objAt[1] = Rockfall.WALL_HIGH + 0.6f;
            return near(objAt) ? OBJ_HEAP : OBJ_NONE;
        }
        if (tow != null && tow.step == Tow.QUARRY && !kit.tied && kit.held != Kit.ROPE) {
            objAt[0] = tow.stuck.x;
            objAt[1] = tow.stuck.y + 2.6f;
            objAt[2] = tow.stuck.z;
            return near(objAt) ? OBJ_STUCK : OBJ_NONE;
        }
        Fault fl = fault != null ? fault : stray;
        if (fl != null && fl.step == Fault.GO && toss.tool != Toss.SCREWDRIVER) {
            objAt[0] = fl.boxX();
            objAt[1] = 3.2f;
            objAt[2] = fl.boxZ();
            return near(objAt) ? OBJ_BOX : OBJ_NONE;
        }
        return OBJ_NONE;
    }

    private boolean near(float[] at) {
        float dx = at[0] - runner.x, dz = at[2] - runner.z;
        return dx * dx + dz * dz < OBJ_FAR * OBJ_FAR;
    }

    /** What the thing over which the button is wants: the one right for it. */
    private int objRight(int kind) {
        return kind == OBJ_HEAP ? PIE_KIT + Kit.CHARGE : kind == OBJ_STUCK ? PIE_KIT + Kit.ROPE : PIE_SCREWDRIVER;
    }

    /** The ring over a thing: with a pickup near, its bed's things; her own tools always. */
    private void objRing() {
        pieN = 0;
        bedNow = nearestPickup(runner.x, runner.z);
        if (bedNow != null && bedNow.apart(runner.x, runner.z) > PICKUP_NEAR) bedNow = null;
        if (bedNow != null) {
            pieItems[pieN++] = PIE_KIT + Kit.CHARGE;
            pieItems[pieN++] = PIE_KIT + Kit.ROPE;
            pieItems[pieN++] = PIE_KIT + Kit.LADDER;
        }
        pieItems[pieN++] = PIE_SCREWDRIVER;
    }

    /** Whether the torch's button is up: in the dark, on foot, the torch not in her hand. */
    private boolean torchWanted() {
        return indoors != null && indoors.dark && toss.tool != Toss.TORCH && kit.held == Kit.NONE && torchShowT < 0f;
    }

    /** The torch in her hand, its light on (seen close, the first time in the dark). */
    private void takeTorch() {
        toss.tool = Toss.TORCH;
        host.saveInt(SAVE_TOOL, Toss.TORCH);
        host.sound(Synth.LATCH, 0.4f, 1.6f);
        host.buzz(15);
        if (host.loadInt(SAVE_TORCH) == 0) torchShow();
    }

    /** Her own tools back to the mallet once nothing about her wants them: the screwdriver past the box, the torch out of the dark. */
    private void toolsBack() {
        boolean dark = indoors != null && indoors.dark;
        Fault fl = fault != null ? fault : stray;
        boolean box = fl != null && (fl.step == Fault.GO || fl.step == Fault.OPEN);
        if (toss.tool == Toss.TORCH && !dark || toss.tool == Toss.SCREWDRIVER && !box) {
            toss.tool = Toss.MALLET;
            host.saveInt(SAVE_TOOL, Toss.MALLET);
        }
    }

    /** The torch's button by her in the dark, and the button over a thing that wants something; the ring while it is held. */
    private void pieButton(DrawList d) {
        pieSeen = false;
        bedSeen = false;
        if (!pieOffered() && pieFinger < 0) return;
        Runner r = runner;
        if (torchWanted()) {
            float rx = (float) Math.cos(r.heading), rz = -(float) Math.sin(r.heading);
            // beside her right hip, and up over it
            if (d.project(r.x + rx * 0.75f, r.y + 1.85f, r.z + rz * 0.75f, pieAnchor)) {
                float tx = Math.max(BED_HALF + 4f, Math.min(W - BED_HALF - 4f, pieAnchor[0]));
                float ty = Math.max(pauseY() + PAUSE_HALF + BED_HALF + 8f, Math.min(h - BED_HALF - 40f, pieAnchor[1]));
                // eased after her, so that it does not shake with her stride
                pieX += (tx - pieX) * 0.35f;
                pieY += (ty - pieY) * 0.35f;
                if (Math.abs(pieX - tx) > 80f || Math.abs(pieY - ty) > 80f) {
                    pieX = tx;
                    pieY = ty;
                }
                pieSeen = true;
            }
        }
        if (pieFinger < 0) objKind = objWanted();
        if (objKind != OBJ_NONE && pieFinger < 0 && d.project(objAt[0], objAt[1], objAt[2], pieAnchor) && pieAnchor[2] > 0.5f) {
            bedX = Math.max(BED_HALF + 4f, Math.min(W - BED_HALF - 4f, pieAnchor[0]));
            bedY = Math.max(pauseY() + PAUSE_HALF + BED_HALF + 8f, Math.min(h - BED_HALF - 40f, pieAnchor[1]));
            bedSeen = true;
        }
        if (pieFinger < 0) {
            if (pieSeen) torchButton(d);
            if (bedSeen) {
                pieBed = true;
                objRing();
                ringLesson(d, bedX, bedY, BED_HALF, Badges.BED, objRight(objKind));
            }
            return;
        }
        // held: the ring, once the finger has stayed or gone out of the middle
        boolean shown = time - pieAt > PIE_REVEAL || pieHot >= 0;
        d.badge(pieCx, pieCy, BED_HALF, Badges.BED, 0.5f, 0f);
        if (!shown) return;
        for (int k = 0; k < pieN; k++) {
            float a = pieAngle(k);
            float sx = pieCx + (float) Math.sin(a) * PIE_R, sy = pieCy - (float) Math.cos(a) * PIE_R;
            boolean hot = k == pieHot, now = pieNow(pieItems[k]);
            d.badge(sx, sy, hot ? PIE_ITEM * 1.25f : PIE_ITEM, pieSign(pieItems[k]), hot ? 1f : 0.7f, hot ? 0.45f : now ? 0.15f : 0f);
        }
    }

    /** The torch's button by her: pulsing, a finger coming down on it now and then. */
    private void torchButton(DrawList d) {
        d.badge(pieX, pieY, BED_HALF * 0.85f, Badges.TORCH, 0.9f, 0.4f + 0.4f * (float) Math.sin(time * 6f));
        float t = time % LESSON;
        if (t < LESSON_UP) {
            float press = t < LESSON_DOWN ? 1f - t / LESSON_DOWN : t > LESSON_UP - 0.3f ? (t - (LESSON_UP - 0.3f)) / 0.3f : 0f;
            d.badge(pieX + 5f, pieY + 12f + press * 10f, 15f, Badges.ONE_FINGER, 0.95f * Math.min(1f, t / 0.2f), press < 0.05f ? 0.35f : 0f);
        }
    }

    /** Whether ring thing k is what she holds now (lit a little in the ring). */
    private boolean pieNow(int k) {
        if (k >= PIE_KIT) return kit.held == k - PIE_KIT;
        if (kit.held != Kit.NONE) return false;
        return k == pieOf(toss.tool);
    }

    /** Ring thing k chosen. */
    private void pieChoose(int k) {
        if (k >= PIE_KIT) {
            int thing = k - PIE_KIT;
            if (bedNow == null) return;
            if (kit.held == thing && kit.from == bedNow) return;
            // the ladder still up somewhere, of this bed: taken down first
            if (thing == Kit.LADDER && kit.set && kit.setFrom == bedNow) {
                city.unsetLadder();
                kit.set = false;
                kit.setFrom = null;
            }
            if (thing == Kit.ROPE && kit.tied) return;
            if (toss.tool != Toss.MALLET) {
                toss.tool = Toss.MALLET;
                host.saveInt(SAVE_TOOL, Toss.MALLET);
            }
            if (!atBed(bedNow)) {
                // from far off: she goes to the bed's back by herself, and takes it there
                Pickup p = bedNow;
                float bx = p.x - (float) Math.sin(p.heading) * 2.4f, bz = p.z - (float) Math.cos(p.heading) * 2.4f;
                errand = ERRAND_BED;
                errandFor = 0f;
                errandPickup = p;
                errandThing = thing;
                runner.lead(bx, bz, p.heading);
                host.buzz(15);
                return;
            }
            kit.take(thing, bedNow);
            host.sound(Synth.WOOD, 0.4f, thing == Kit.ROPE ? 0.8f : 1.2f);
        } else {
            // a tool of her own: anything of a bed in her hands goes back of itself
            kit.held = Kit.NONE;
            kit.from = null;
            int tool = k == PIE_SCREWDRIVER ? Toss.SCREWDRIVER : Toss.MALLET;
            toss.tool = tool;
            host.saveInt(SAVE_TOOL, tool);
            host.sound(Synth.TOCK, 0.4f, tool == Toss.SCREWDRIVER ? 1.6f : 1.1f);
        }
        host.buzz(15);
    }

    /** A finger on the torch's button or a ring's, or the finger holding a ring: true if it is theirs. */
    private boolean pieTouch(int type, int id, float x, float y) {
        if (pieFinger >= 0) {
            if (id != pieFinger) return false;
            float dx = x - pieCx, dy = y - pieCy;
            if (Math.sqrt(dx * dx + dy * dy) < PIE_DEAD) pieHot = -1;
            else {
                float a = (float) Math.atan2(dx, -dy), best = 9f;
                pieHot = -1;
                for (int k = 0; k < pieN; k++) {
                    float off = Math.abs(a - pieAngle(k)) % (2f * (float) Math.PI);
                    off = Math.min(off, 2f * (float) Math.PI - off);
                    if (off < best) {
                        best = off;
                        pieHot = k;
                    }
                }
            }
            if (type == UP) {
                if (pieHot >= 0) pieChoose(pieItems[pieHot]);
                pieFinger = -1;
                pieHot = -1;
            }
            return true;
        }
        if (type != DOWN || !pieOffered()) return false;
        if (pieSeen && Math.abs(x - pieX) <= BED_HALF + 7f && Math.abs(y - pieY) <= BED_HALF + 7f) {
            takeTorch();
            letGo();
            return true;
        }
        if (!bedSeen || Math.abs(x - bedX) > BED_HALF + 7f || Math.abs(y - bedY) > BED_HALF + 7f) return false;
        pieBed = true;
        objRing();
        pieFinger = id;
        pieHot = -1;
        pieAt = time;
        // the ring's middle where the button is, kept in from the frame's edges
        pieCx = Math.max(PIE_R + PIE_ITEM + 4f, Math.min(W - PIE_R - PIE_ITEM - 4f, bedX));
        pieCy = Math.max(PIE_R + PIE_ITEM + 60f, Math.min(h - PIE_R - PIE_ITEM - 10f, bedY));
        letGo();
        host.buzz(10);
        return true;
    }

    // a thing of a bed brought up to where it goes: held there so long, it is used as a tap would
    static final float KIT_STAND = 0.5f;
    private float kitStood;

    /** Whether the thing in her hands is where it goes: the ladder at a wall before her, the rope by the brum's pickup. */
    private boolean kitReady() {
        if (kit.held == Kit.LADDER) return kit.wallBefore(runner, city);
        if (kit.held == Kit.ROPE) return tow != null && tow.nearTie(runner);
        if (kit.held == Kit.CHARGE) {
            chargeAt();
            return rescue != null && rescue.wanting() && rescue.fall.atFace(chargeAt[0], chargeAt[1]);
        }
        return false;
    }

    /** A tap with a thing of a bed in her hands: the ladder set up against the wall before her, the rope tied on. */
    /** Where a charge in her hands is set: a step before her, on the ground. */
    private void chargeAt() {
        chargeAt[0] = runner.x + (float) Math.sin(runner.heading) * CHARGE_AHEAD;
        chargeAt[1] = runner.z + (float) Math.cos(runner.heading) * CHARGE_AHEAD;
    }

    static final float CHARGE_AHEAD = 1.1f;
    private final float[] chargeAt = new float[2];

    private void kitUse() {
        if (kit.held == Kit.LADDER) {
            if (kit.setLadder(runner, city)) {
                host.sound(Synth.CLANG, 0.45f, 1.2f);
                host.buzz(30);
            } else host.sound(Synth.TOCK, 0.3f, 0.6f);
        } else if (kit.held == Kit.CHARGE) {
            chargeAt();
            if (rescue != null && rescue.setCharge(chargeAt[0], city.ground(chargeAt[0], chargeAt[1]), chargeAt[1], runner.heading)) {
                // set against the heap, its fuse lit: it does not go back to the bed
                kit.held = Kit.NONE;
                kit.from = null;
                host.sound(Synth.LATCH, 0.5f, 0.9f);
                host.sound(Synth.WHOOSH, 0.3f, 2f);
                host.buzz(40);
            } else host.sound(Synth.TOCK, 0.3f, 0.6f);
        } else if (kit.held == Kit.ROPE) {
            if (tow != null && tow.tie(kit.from, runner, host)) {
                kit.tied = true;
                kit.tiedFrom = kit.from;
                kit.held = Kit.NONE;
                kit.from = null;
            } else host.sound(Synth.TOCK, 0.3f, 0.6f);
        }
    }

    /** The compass, a button as the pause is, across from it on the right. */
    int compassX() {
        return W - pauseX();
    }

    /** The compass on the frame: a blank pane as the pause's, the needle over it turned to the north as the eye looks. */
    private void compassButton(DrawList d) {
        if (!compassShows()) return;
        d.badge(compassX(), pauseY(), PAUSE_HALF, Badges.BLANK, 0.75f, coordsShown ? 0.25f : 0f);
        // the eye's turn, to within half round either way: the needle turned back by it points north
        float yaw = runner.eyeYaw % (2f * (float) Math.PI);
        if (yaw > Math.PI) yaw -= 2f * (float) Math.PI;
        if (yaw < -Math.PI) yaw += 2f * (float) Math.PI;
        d.badge(compassX(), pauseY(), PAUSE_HALF, Badges.NEEDLE, 0.95f, 0f, DrawList.TINT_NONE, yaw == 0f ? 1e-4f : yaw);
    }

    /** At the wheel, up on the left under the pause, out from under the thumb: how fast, in kilometers an hour. */
    private void speedFlat(Px f) {
        if (driving == null || dispatch != null) return;
        String v = String.valueOf(Math.round(Math.abs(driving.speed) * 3.6f));
        int ink = 0xFF232A4A, y = Math.round(pauseY() + PAUSE_HALF + 12f);
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

    // ---- at the wheel ----
    private boolean classicDown;

    /** The finger on the steering wheel (-1, none). */
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
        if (liftOffer && onLiftButton(x, y)) {
            liftPressed = true;
            return;
        }
        int sgn = signHere(x, y);
        if (sgn >= 0) {
            errand(sgn);
            return;
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
                else if (!pauseRow(x, y)) paused = false;
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
        // a sign: she goes where it says by herself
        if (type == DOWN) {
            int sgn = signHere(x, y);
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
        hearts.update(dt, W, h);
        if (opening == null && (whereKeptAgo += dt) > WHERE_KEPT_EVERY) keepWhere();
        if (likesDirty && (likesKeptAgo += dt) > LIKES_KEPT_EVERY) saveLikes();
        if (story != null) {
            // the town stands still while its page is read
            story.update(dt);
            return;
        }
        if (stray != null && stray.open()) {
            // a light gone wrong between jobs, its box open: the panel over everything, the town still
            stray.update(dt, runner, true, true);
            if (stray.over()) strayMended();
            else if (!stray.open()) letGo();
            return;
        }
        if (fault != null && fault.open()) {
            fault.update(dt, runner, true, true);
            if (!fault.open()) letGo();
            return;
        }
        if (solo != null) {
            // the panel from home's screen, over everything; the room stands still
            solo.update(dt);
            soloClock += dt;
            if (solo.left) {
                solo = null;
                letGo();
                sofaChoice(null);
            } else if (solo.shut()) {
                solo = null;
                letGo();
                int was = host.loadInt(SAVE_PANEL_BEST), now = Math.max(1, Math.round(soloClock * 10f));
                if (was <= 0 || now < was) host.saveInt(SAVE_PANEL_BEST, now);
                like(Dispatch.PANEL_LIKES, WIN_HEARTS / 2);
                saveLikes();
                sofaChoice(liked(Dispatch.timed(Dispatch.panelDone(clockOf(soloClock)), was / 10f, soloClock), Dispatch.PANEL_LIKES));
            }
            return;
        }
        if (paused && opening == null && dispatch == null) return;
        time += dt;
        // a point kept as a screen came on: its disk waits for the screen to go
        if (dispatch != null && pointKeptAt > time - POINT_SHOWN) pointKeptAt = time;
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
                if (!free) dispatch = new Dispatch(host, briefing()).afterBlow().story(kindOf(mission.round));
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
                // the hot brums after her give her the moment she stood in her head
                if (herd != null) herd.hold();
                if (soloNext) {
                    soloNext = false;
                    openSolo();
                } else if (yardNext) {
                    yardNext = false;
                    startCrane(true);
                } else if (resultsOpen) resultsOpen = false;
                else if (standAfter) {
                    standAfter = false;
                    if (seated()) standUp();
                }
                else if (retryAfter) {
                    retryAfter = false;
                    retry();
                } else if (seated() && !free) standUp();
            }
            return;
        }
        if (!free && over() && (overFor += dt) > (concert != null ? AFTER_DUSK : BACK_TO_ROOM)) {
            forgetPoint();
            save();
            String[] said = report();
            boolean failed = failed();
            overFor = 0f;
            letGo();
            if (failed) {
                // lost: the report at once, wherever she is, and the same task again from where it
                // began, the same targets in the same places
                if (indoors != null) {
                    swapTo = TO_STREET;
                    swap();
                    swapT = -1f;
                }
                dispatch = new Dispatch(host, said).then(Dispatch.again(mission.round)).story(kindOf(mission.round));
                retryAfter = true;
                return;
            }
            // won: its time kept against the best; the report waits at home, on the sofa, and the
            // town is hers on the way there
            said = Dispatch.timed(said, record(mission.round, doneTime()), doneTime());
            // a story won before: played again, it leads on to nothing and leaves the world as it was
            boolean replay = replaying();
            kindsWon |= 1 << kindOf(mission.round);
            host.saveInt(SAVE_KINDS, kindsWon);
            kindsSkipped &= ~(1 << kindOf(mission.round));
            host.saveInt(SAVE_SKIPPED, kindsSkipped);
            lost = false;
            free = true;
            folder = null;
            delivery = null;
            // the pass open, a pickup is down in the quarry past it: the tow at once, on one screen;
            // what the blast left lies in the pass a while yet
            boolean chainTow = rescue != null && !replay && !passed(6);
            if (rescue != null) debris = rescue;
            rescue = null;
            puppy = null;
            crowd = null;
            // the brum towed asks her to take his job: the light gone wrong, at once, on one screen
            boolean chainLight = tow != null && !passed(14);
            if (tow != null) towed = tow;
            tow = null;
            // the helicopter stays where the archive job left it; its second half begins at once,
            // at the archive's door, the report and the next on one screen
            boolean chain = archive != null && !replay;
            if (archive != null) parked = archive.copter;
            archive = null;
            if (mend != null) parked = mend.copter;
            putBackAside(mend != null);
            mend = null;
            port = null;
            lab = null;
            herd = null;
            cherry = null;
            fault = null;
            kit.putBack(city);
            settleIsland();
            saveCopter();
            // after the concert the dark has come down: she wakes at home, on her sofa, in the morning
            boolean slept = concert != null;
            if (concert != null) concert.stop();
            concert = null;
            city.extra = null;
            chase = nobody;
            waiting = said;
            if (chain) {
                waiting = null;
                chooseMission(9);
                dispatch = new Dispatch(host, said).then(briefing()).story(9);
                return;
            }
            if (chainTow) {
                waiting = null;
                chooseMission(6);
                city.extra = debris;
                dispatch = new Dispatch(host, said).then(briefing()).story(6);
                return;
            }
            if (chainLight) {
                waiting = null;
                chooseMission(14);
                dispatch = new Dispatch(host, said).then(briefing()).story(14);
                return;
            }
            if (slept) {
                // no report: in the morning the screen has the next story on it at once
                waiting = null;
                wake();
            }
            return;
        }
        whiteOut = Math.max(0f, whiteOut - dt);
        dawn = Math.max(0f, dawn - dt);
        shake = Math.max(0f, shake - dt * SHAKE_DIES);
        if (!shutRead) {
            shutRead = true;
            Island.shut = shutByJobs();
            scars.load(host);
            Island.bridgeUp = Island.bridgeTo = Island.shut ? 1f : 0f;
            Island.bridgeStep(0f);
        }
        if (herd == null) wrecked(Island.shut);
        Canal.step(dt, duty.boat(), cars);
        Island.bridgeTo = Island.shut ? 1f : 0f;
        if (Island.bridgeStep(dt)) {
            float gx = Island.spitX(Island.GAP_Z);
            city.forget(gx - 20f, Island.GAP_Z - 20f, gx + 20f, Island.GAP_Z + 20f);
        }
        // on a chase the eye goes up high over her, those after her seen; once she is safe of them, not
        runner.chaseWant = herd != null && !herd.over() && !herd.safe() && herd.coming() > 0 ? 1f : 0f;
        // straight to the place: through the air from the lighthouse, and anywhere indoors
        nav.air = herd != null && herd.step == Herd.LEAP || indoors != null;
        if (indoors == null) shore.update(dt, runner.x, runner.z);
        if (indoors == null) villagers.update(dt, driving != null ? driving.x : runner.x, driving != null ? driving.z : runner.z);
        if (stayed != null) {
            boolean in = indoors != null && indoors.concert;
            stayed.update(dt, in, indoors, runner);
            if (!in) {
                stayed.stop();
                stayed = null;
            }
        }
        listened(dt);
        stunts(dt);
        // the concert hall dark till its panel is mended
        if (indoors != null && indoors.concert) indoors.dark = concert != null && !concert.lit();
        // home's own pickup at its curb, once the delivery is won
        city.homePickup = passed(3);
        if (dutyOut()) duty.update(runner.x, runner.z, city, driving, herd == null);
        else duty.clear();
        if (driving != null && roadT < 0f) roadCue();
        if (roadT >= 0f) roadStep(dt);
        else if (driving != null) drive(dt);
        // the way out waiting: she gets out once the pickup stands
        if (exitWaiting && (driving == null || Math.abs(driving.speed) <= STOPPED)) {
            if (driving != null) getOut(false);
            exitWaiting = false;
        }
        // a pickup she leapt out of rolls on, slowing, till it stands where it stops
        if (rolling != null) {
            if (rolling == driving) rolling = null;
            else if (Math.abs(rolling.speed) < 0.05f) {
                rolling.speed = 0f;
                duty.settle(rolling);
                rolling = null;
            } else rolling.drive(dt, false, 0f, 0f, false, city, cars, walkers);
        }
        else if (aboard()) ride(dt);
        else if (piloting()) fly(dt);
        else if (craning) crane(dt);
        else {
            ladders(dt);
            doors(dt);
            errands(dt);
            getIn(dt);
            board(dt);
            toCrane();
        }
        if (!craning) quay.update(dt);
        if (!piloting() && parked != null && archive == null && mend == null) parked.update(dt);
        // a beat of the arms (a flick, between frames): the air through them
        if (runner.beatsLeft() < beatsWere) host.sound(Synth.WHOOSH, 0.5f, 0.8f);
        // in the tunnel through the island: its floor, walls and roof hers
        if (indoors == null) runner.room = Island.tunnel().holds(runner.x, runner.y, runner.z) ? Island.tunnel() : null;
        if (swapT < 0f && driving == null && !seated() && !inChair() && !aboard() && !piloting() && !craning) runner.update(dt, city, cars, walkers);
        // into deep water: a splash, and she swims
        boolean swims = runner.swimming();
        if (swims && !swimSeen) host.sound(Synth.SPLASH, 0.8f, 1f);
        swimSeen = swims;
        beatsWere = runner.beatsLeft();
        int round = mission.round;
        if (free) {
            chase = nobody;
            if (dispatch == null) strayStep(dt);
            // the job at the concert hall done once: its middle door is open to her, on foot
            if ((kindsWon & (1 << 7)) != 0 && indoors == null && swapT < 0f && driving == null && concertDoor(dt)) {
                host.sound(Synth.DOOR, 0.45f, 0.9f);
                go(TO_CONCERT);
            }
        } else if (concert != null) {
            boolean in = indoors != null && indoors.concert;
            if (concert.update(dt, in, indoors, runner)) {
                // at the door to the wings: the panel opens over everything
                letGo();
                host.sound(Synth.LATCH, 0.5f, 1.3f);
                host.buzz(20);
            }
            if (concert.lit() && concert.litAt == concert.clock) {
                host.sound(Synth.CHIME, 0.4f, 0.9f);
                host.buzz(60);
            }
            // at the hall's middle door, on foot, she goes in
            if (!in && swapT < 0f && driving == null && concert.step == Concert.GO && concertDoor(dt)) {
                host.sound(Synth.DOOR, 0.45f, 0.9f);
                go(TO_CONCERT);
            }
        } else if (archive != null) {
            boolean wasHanding = archive.handing();
            int got = archive.update(dt, runner, indoors, host);
            if (archive.handing()) handOver(wasHanding);
            else if (wasHanding) runner.held = false;
            if (got == Archive.IN) {
                // in beside the pilot: the blades run up
                errand = NO_ERRAND;
                runner.stopLead();
                letGo();
                host.sound(Synth.WHOOSH, 0.5f, 0.6f);
            } else if (got == Archive.ENTER) {
                // in at the door on the garden, into the archive's foyer
                go(TO_ARCHIVE);
            } else if (got == Archive.CRACK) {
                host.sound(Synth.CLANG, 0.9f, 0.55f);
                host.sound(Synth.STONE, 0.7f, 0.7f);
                host.buzz(160);
            }
            if (got == Archive.OUT || got == Archive.CRACK && !archive.aboard()) {
                // out of the right-hand door, onto the garden
                Copter cp = archive.copter;
                cp.door(seatAt);
                runner.getOut(seatAt[0], city.ground(seatAt[0], seatAt[1]), seatAt[1], cp.heading);
                runner.recenter();
                runner.update(0f, city, cars, walkers);
                letGo();
            }
        } else if (mend != null) {
            mendStep(dt);
        } else if (lab != null) {
            labStep(dt);
        } else if (herd != null) {
            herdStep(dt);
        } else if (cherry != null) {
            cherryStep(dt);
        } else if (fault != null) {
            boolean was = fault.open();
            fault.update(dt, runner, driving == null && indoors == null, toss.tool == Toss.SCREWDRIVER);
            if (fault.open() && !was) {
                letGo();
                host.sound(Synth.LATCH, 0.5f, 1.3f);
                host.buzz(20);
            }
        } else if (port != null) {
            port.update(dt, craning, quay.solved);
        } else if (tow != null) {
            tow.update(dt, duty, driving, runner, city, host);
        } else if (puppy != null) {
            boolean had = puppy.held(), was = puppy.entering();
            puppy.update(dt, driving, runner, host);
            if (puppy.held() && !had) host.sound(Synth.SPLASH, 0.5f, 1.6f);
            if (puppy.entering()) clinicStep(dt, !was);
            if (crowd != null) crowd.update(dt, runner.x, runner.z);
        } else if (rescue != null) {
            rescueStep(dt);
        } else if (delivery != null) {
            deliveryStep(dt);
        } else if (folder != null) {
            boolean had = folder.hers;
            folder.update(dt, runner, city, cars);
            if (folder.hers && !had) took();
            if (folder.hers && had) {
                // to the town hall with it, and in the secretariat from her hands into the clerk's
                boolean wasHanding = folder.handing(), wasGiven = folder.delivered();
                folder.back(dt, runner, indoors, host);
                if (folder.handing()) handOver(wasHanding);
                else if (wasHanding) runner.held = false;
                // in the clerk's hands: the dispatcher comes on, the job done, and home now
                if (folder.delivered() && !wasGiven) {
                    host.sound(Synth.CHIME, 0.6f, 1f);
                    dispatch = new Dispatch(host, Lang.lines("folder.done", mission.round, null));
                }
            }
            Chase n = folder.nearest();
            chase = n != null ? n : chase;
        } else {
            mission.update(dt, runner, city, cars);
            chase = mission.target(chase);
        }
        points();
        toolsBack();
        torchStep(dt);
        debrisStep(dt);
        // in the collider hall the mallet is swung at the magnets there, never thrown
        toss.lent = indoors != null && indoors.collider;
        // a thing out of a pickup's bed in her hands: the tap is its, not the mallet's
        boolean tap = runner.tapped();
        if (kit.held != Kit.NONE && driving != null) kit.held = Kit.NONE;
        if (kit.held != Kit.NONE && tap) {
            kitUse();
            tap = false;
        }
        // or brought right up to where it goes and held there a moment: the same
        if (kit.held != Kit.NONE && driving == null && !runner.inAir() && runner.speed < 1.5f && kitReady()) {
            if ((kitStood += dt) > KIT_STAND) {
                kitStood = 0f;
                kitUse();
            }
        } else kitStood = 0f;
        toss.carrying = kit.held != Kit.NONE;
        toss.update(dt, runner, brums(), city, tap, hunt());
        frighten();
        int rode = riding();
        float rx0 = rode >= 0 ? cars.x[rode] : 0f, rz0 = rode >= 0 ? cars.z[rode] : 0f, rh0 = rode >= 0 ? cars.heading[rode] : 0f;
        cars.driven = driving;
        cars.update(dt, runner, brums());
        if (rode >= 0) carried(rode, rx0, rz0, rh0, dt);
        else rideFor = 0f;
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
            if (struck > struckSeen) like(STRUCK_LIKES * (struck - struckSeen), STRUCK_HEARTS);
            struckSeen = struck;
            host.buzz(30);
        }
        fliers.update(dt, true);
        felled.update(dt, runner.x, runner.z, city);
        reparkFar();
        // the mend's scenes are shown from an eye of their own, whatever else has set one
        if (mend != null && mend.scene()) mendEye();
        if (puppy != null && puppy.entering()) clinicEye();
    }

    /**
     * The folder in her hands: the dispatcher comes on to say it is safe now, and where it goes:
     * down off the roof, and to the town hall's secretariat.
     */
    private void took() {
        host.buzz(120);
        host.sound(Synth.LEATHER, 0.7f, 1.1f);
        host.sound(Synth.CHIME, 0.6f, 1f);
        dispatch = new Dispatch(host, Lang.lines("folder.safe", mission.round, null));
    }

    // ---- doors: in from the street (or the yard), or down from a roof, a place indoors; out again ----
    static final float SWAP = 0.3f;          // the dark coming down at a door, and lifting again
    static final int TO_HALL = 1, TO_CABIN = 2, TO_ROOF = 3, TO_STREET = 4, TO_YARD = 5, TO_HOME = 6, TO_CONCERT = 7, TO_SECRET = 8, TO_LOBBY = 9, TO_TOWER = 10,
            TO_COLLIDER = 11, TO_LOBBY_UP = 12, TO_OLD = 13, TO_ARCHIVE = 14;
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
        if (indoors != null && indoors.townHall) Rooms.townHallStep(indoors, dt, runner);
        if (indoors != null && indoors.home && sofa(dt)) return;
        if (indoors != null && indoors.concert && chair(dt)) return;
        if (indoors != null) {
            int e = indoors.update(dt, r, host);
            if (e == Indoors.UP) go(indoors.lobby ? TO_COLLIDER : indoors.collider ? TO_LOBBY_UP : TO_ROOF);
            else if ((e == Indoors.OUT_FRONT || e == Indoors.OUT_BACK) && indoors.home) {
                // out of home's door: no dark between, she only steps out
                swapTo = e == Indoors.OUT_FRONT ? TO_STREET : TO_YARD;
                swap();
            } else if (e == Indoors.OUT_FRONT) go(TO_STREET);
            else if (e == Indoors.OUT_BACK) go(TO_YARD);
            return;
        }
        if (r.inAir() || r.climbing() || r.throwing() >= 0f) return;
        float fx = (float) Math.sin(r.heading), fz = (float) Math.cos(r.heading);
        // on home's balcony up on the fifth storey, at its door, going to it: into the hall no one knows of
        if (driving == null && City.atSecretDoor(r.x, r.y, r.z) && fz > 0.3f) {
            host.sound(Synth.DOOR, 0.45f, 0.8f);
            go(TO_SECRET);
            return;
        }
        // on the island's top, at the lighthouse's door, going to it: in, and up to its gallery
        if (driving == null && Island.atDoor(r.x, r.y, r.z, r.heading)) {
            host.sound(Synth.DOOR, 0.45f, 0.95f);
            go(TO_TOWER);
            return;
        }
        // up on the mountain, at the laboratory's door, going to it: into its lobby
        if (driving == null && Mountain.atDoor(r.x, r.y, r.z) && fz > 0.3f) {
            host.sound(Synth.DOOR, 0.45f, 1.05f);
            go(TO_LOBBY);
            return;
        }
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
        // home's doors, the front one on the street and the back one on the yard: into home's room
        if (City.home(houseNow[0], houseNow[1])) {
            liftArmed = false;
            if (driving != null) getOut();
            System.arraycopy(l, 0, liftOf, 0, 11);
            System.arraycopy(houseNow, 0, houseOf, 0, 8);
            host.sound(Synth.DOOR, 0.45f, 1f);
            // no dark between: home's rooms are there in the town, she only steps in
            swapTo = TO_HOME;
            swap();
            return;
        }
        // the old house's doors: into its flat, where a brum lives now
        if (City.oldHome(houseNow[0], houseNow[1])) {
            liftArmed = false;
            if (driving != null) getOut();
            System.arraycopy(l, 0, liftOf, 0, 11);
            System.arraycopy(houseNow, 0, houseOf, 0, 8);
            host.sound(Synth.DOOR, 0.45f, 1f);
            go(TO_OLD);
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

    // ---- the way out at the wheel: with the wheel drawn over it, a touch there may be the thumb
    // after the wheel, so it only asks to get out, and waits for the pickup to stand (touched
    // again, not); with the wheel clear of it, the player means it: out on the go, the pickup
    // rolling on by itself ----
    /** Slower than this, meters a second, the pickup stands. */
    static final float STOPPED = 0.4f;
    private boolean exitWaiting;
    /** The pickup she leapt out of on the go, rolling on, or null. */
    private Pickup rolling;

    /** Whether the steering wheel, where it is drawn now (held or at rest), reaches over the way out. */
    private boolean wheelOverExit() {
        float cx = wheelFinger >= 0 ? wheelDrawX(carCX) : wheelDrawX(WHEEL_REST_X);
        float cy = wheelFinger >= 0 ? wheelDrawY(carCY) : wheelDrawY(h - WHEEL_REST_UP);
        float dx = liftButtonX() - cx, dy = liftButtonY() - cy, reach = WHEEL_HALF + BUTTON + 4;
        return dx * dx + dy * dy < reach * reach;
    }

    /** Where the button's middle is: low in the frame, to the right, under the thumb. */
    int liftButtonX() {
        return W - 44;
    }

    int liftButtonY() {
        // on foot it sits over her own buttons; at the wheel, up out of the thumb's way
        return driving != null ? h - 196 : h - 190;
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
        pointBadge(d);
        compassButton(d);
        pieButton(d);
        if (piloting()) {
            sticks(d);
            return;
        }
        if (craning) {
            craneButtons(d);
            return;
        }
        // the drive up or down the mountain shown: no wheel, no way out, nothing to do but watch
        if (driving != null && roadT < 0f) {
            // the way out first, half seen, so the wheel's lines lie over it; waiting for the pickup to stand, aglow
            d.badge(liftButtonX(), liftButtonY(), BUTTON + 4, Badges.EXIT, exitWaiting ? 0.7f : 0.5f,
                    exitWaiting ? 0.55f + 0.4f * (float) Math.sin(time * 6f) : 0f);
            // the steering wheel by the thumb while it is down, turned as the pickup's is, in the
            // mission's colour, green while the throttle is on, coral while the brake is; a faint
            // wheel where it would go. No knob: the thumb covers it, and the wheel's turn says the same
            // drawn turned the way the pickup turns: the thumb right, its top to the right (the badge
            // turns the other way round for a positive angle, so the sign is taken off)
            float turn = -driving.steer * WHEEL_TURNS;
            if (wheelFinger >= 0) {
                float pull = pullAxis();
                int tint = pull > 0f ? DrawList.TINT_GREEN : pull < 0f ? DrawList.TINT_CORAL : ringTint();
                d.badge(wheelDrawX(carCX), wheelDrawY(carCY), WHEEL_HALF, Badges.STEER, WHEEL_SEEN, pull != 0f ? 0.6f : 0.1f, tint, turn);
            } else d.badge(wheelDrawX(WHEEL_REST_X), wheelDrawY(h - WHEEL_REST_UP), WHEEL_HALF, Badges.STEER, WHEEL_RESTING, 0f, ringTint(), turn);
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
    // how near its sides or ends she stands to get in, and how long
    static final float BY_IT = 1f, GET_IN_STAND = 0.5f;
    /** So near it, walking at it: she gets in at once. */
    static final float BY_TOUCH = 0.55f;
    // the yacht: so much higher or lower than its deck she may be to get in (from the pier, or
    // swimming); so near the pier the place by it may be to go to the pier's edge instead
    static final float BOARD_UP = 1.6f, BOARD_FROM_PIER = 4f;
    private final float[] pierAt = new float[2];
    private float wheelIn, brakeIn, doorWait;
    private boolean doorArmed = true;         // not got in again until she has been away from the door
    private final Rail rail = new Rail();
    /** The fishermen on the rocks, the shoals in the shallows. */
    private final Shore shore = new Shore();
    /** The village's own people about its square. */
    private final Villagers villagers = new Villagers();
    private final float[] seatAt = new float[3];

    /**
     * Whether the duty pickups are out: not in a hunt, which is run on foot; and not in the streets
     * at all till the delivery, the job that brings them (the folder before it is run on foot too).
     */
    boolean dutyOut() {
        if (hunt()) return false;
        return passed(3) || !free && folder == null;
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

    /** Standing by a pickup a moment, on any side of it, she gets in. */
    private void getIn(float dt) {
        Runner r = runner;
        // with the box in her hands, or unpacking it, she does not get in: it goes in first
        if (!dutyOut() || indoors != null || swapT >= 0f || r.inAir() || r.climbing() || lab != null && (lab.carrying() || lab.unpacking())) {
            doorWait = 0f;
            return;
        }
        for (Pickup p : duty.all) {
            if (p.apart(r.x, r.z) < p.reach(BY_IT) && Math.abs(r.y - p.y) < (p.boat ? BOARD_UP : 1f)) {
                // walked up against it, from any side or end: in at once; standing by it a moment: in
                float tx = p.x - r.x, tz = p.z - r.z, tm = (float) Math.sqrt(tx * tx + tz * tz);
                boolean into = r.speed > 0.4f && p.apart(r.x, r.z) < BY_TOUCH
                        && ((float) Math.sin(r.heading) * tx + (float) Math.cos(r.heading) * tz) > 0.45f * tm;
                if (doorArmed && (into || r.speed <= 1f && (doorWait += dt) > GET_IN_STAND)) getIn(p);
                return;
            }
        }
        doorWait = 0f;
        doorArmed = true;
    }

    private void getIn(Pickup p) {
        if (p == null || driving != null || lab != null && (lab.carrying() || lab.unpacking())) return;
        letGo();
        rail.reset();
        driving = p;
        p.driven = true;
        p.speed = 0f;
        duty.driven(p);
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
        getOut(false);
    }

    /** Out of the pickup at its door; leapt out on the go (roll), it rolls on by itself till it stands. */
    private void getOut(boolean roll) {
        Pickup p = driving;
        if (p == null) return;
        letGo();
        exitWaiting = false;
        p.cap = -1f;
        driving = null;
        p.driven = false;
        if (roll) rolling = p;
        else p.speed = 0f;
        p.steer = 0f;
        if (p.boat) {
            ashore(p);
            return;
        }
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
        if (!roll) duty.settle(p);
        doorArmed = false;
        runner.recenter();
        buttonHeld = true;
        host.sound(Synth.DOOR, 0.5f, 1.05f);
    }

    /**
     * A frame at the wheel: the pickup driven as the thumb on the steering wheel has it. Aside, the
     * wheel turns, gently near the middle, full over at the edge, the less the faster it goes; up,
     * the throttle, the higher the faster; down, the brake, the lower the harder, and held so
     * standing, it backs. In the middle, or let go: it rolls on and slows, the wheel coming back
     * straight. No hand of its own on the wheel: where it goes is the driver's.
     */
    private void drive(float dt) {
        Pickup p = driving;
        boolean held = wheelFinger >= 0;
        float wheel = held ? wheelAxis() : 0f, pull = held ? pullAxis() : 0f;
        brakeIn = Math.max(0f, -pull);
        boolean go = pull > 0f;
        // in the town no faster than its streets take; out on the road, all it has
        boolean town = p.z < City.EDGE_J * City.CELL + City.CURB;
        float top = p.boat ? Pickup.SEA_TOP : town ? TOWN_TOP : Pickup.VMAX;
        p.cap = go ? top * (CAR_CRAWL + (1f - CAR_CRAWL) * pull) : top;
        wheelIn = wheel;
        runner.aboard = false;
        if (p.boat) {
            sail(dt, p, go, wheel);
            return;
        }
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
        // a car parked at the curb in the way: off it goes, as one in the traffic does
        if (Math.abs(p.speed) > Cars.RAM_FROM) {
            float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading), sg = Math.signum(p.speed);
            float reach = Pickup.ENDS + Pickup.END_R * 0.6f + Math.abs(p.speed) * dt;
            float hx = p.x + fx * sg * reach, hz = p.z + fz * sg * reach;
            if (city.unpark(hx, hz, Pickup.END_R * 0.5f, parkedAt)) {
                cars.unparked(parkedAt[0], parkedAt[1], parkedAt[2], (int) parkedAt[3], Float.floatToIntBits(parkedAt[4]),
                        fx * sg, fz * sg, Math.abs(p.speed), hx, hz);
                unparkedHere(parkedAt[0], parkedAt[1]);
            }
        }
        if (rescue != null) rescue.ram(p, dt);
        else if (debris != null) debris.ram(p, dt);
        int ramsWere = cars.rams;
        // the skid being put in its bed at the workshop: it stands
        boolean kept = mend != null && mend.holdsVan();
        if (kept) p.speed = 0f;
        p.drive(dt, go && !kept, kept ? 0f : wheel, kept ? 1f : brakeIn, false, city, cars, walkers);
        if (cars.rams != ramsWere) {
            // a car rammed: a jolt in the hand, the people off; the pickup, not a scratch
            p.speed *= 0.9f;
            host.buzz(60 + Math.round(60f * cars.ramHard));
            walkers.alarm(cars.ramX, cars.ramZ, 10f);
            pets.alarm(cars.ramX, cars.ramZ, 10f);
            cars.alarm(cars.ramX, cars.ramZ, 14f);
        }
        // the wheel straight (let go, or held in its middle): a car a little off the street's way
        // finds it, as a car's wheels do; anything more is the driver's
        float let = 1f - Math.min(1f, Math.abs(p.steer) / SETTLE_WHEEL);
        if (let > 0f && Math.abs(p.speed) > 3f) {
            float d;
            if (p.z > City.EDGE_J * City.CELL + City.CURB) {
                // out of the town: along the road out, if it is on it or near, the way it runs here
                d = 9f;
                if (Math.abs(p.x - City.routeX(p.z)) < City.ROUTE_HALF + 6f && p.z < City.VILLAGE_Z - City.SQUARE_HALF) {
                    float run = (float) Math.atan2(City.routeX(p.z + 1f) - City.routeX(p.z - 1f), 2f);
                    for (int back = 0; back < 2; back++) {
                        float to = run + back * (float) Math.PI, e = to - p.heading;
                        while (e > Math.PI) e -= 2f * (float) Math.PI;
                        while (e < -Math.PI) e += 2f * (float) Math.PI;
                        if (Math.abs(e) < Math.abs(d)) d = e;
                    }
                }
            } else {
                float q = (float) Math.PI * 0.5f;
                d = Math.round(p.heading / q) * q - p.heading;
            }
            if (Math.abs(d) < SETTLE_WITHIN) p.heading += d * Math.min(1f, dt * SETTLE_RATE * let);
        }
        p.seat(seatAt);
        runner.ride(dt, city, p.x, p.y, p.z, p.heading, p.speed, seatAt[0], seatAt[1], seatAt[2], p.steer);
    }

    // ---- the mountain road by car, shown as a scene: so long, seconds; the eye over it at its
    // highest so high, so far behind; it starts and ends this high behind the pickup, so far back;
    // the pickup rolls on out of it so fast ----
    static final float ROAD_S = 6.5f, ROAD_EYE_HIGH = 180f, ROAD_EYE_BACK = 30f, ROAD_EYE_LOW = 6f, ROAD_EYE_NEAR = 12f, ROAD_ROLL = 8f;
    /** The scene: seconds into it (less than nothing: none); up the mountain or down it. */
    float roadT = -1f;
    private boolean roadUp;
    private float roadWasX, roadWasZ;
    private final float[] roadAt = new float[4], roadEye = new float[5];

    /**
     * At the wheel: through the arch at the road's foot, going up it, or off the deck into the
     * road's top end, going down it: the scene of the drive starts.
     */
    private void roadCue() {
        Pickup p = driving;
        float wx = roadWasX, wz = roadWasZ;
        roadWasX = p.x;
        roadWasZ = p.z;
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading);
        if (Math.abs(p.x - Mountain.MX) < Mountain.HALF + 3f && wz < Mountain.ARCH_Z && p.z >= Mountain.ARCH_Z && fz > 0.3f && p.speed > 1f) {
            roadUp = true;
            roadT = 0f;
        } else if (Math.abs(p.z - Mountain.AISLE_Z) < Mountain.HALF + 1.5f && wx < Mountain.DECK_X1 - 2f && p.x >= Mountain.DECK_X1 - 2f && fx > 0.3f
                && p.speed > 1f && Math.abs(p.y - Mountain.TOP) < 3f) {
            roadUp = false;
            roadT = 0f;
        }
        if (roadT >= 0f) host.sound(Synth.CHIME, 0.25f, 0.7f);
    }

    /** The scene on: the pickup carried along the road, up or down, eased in and out; then on its own again. */
    private void roadStep(float dt) {
        Pickup p = driving;
        if (p == null) {
            roadT = -1f;
            return;
        }
        roadT += dt;
        float u = Math.min(1f, roadT / ROAD_S), e = u * u * (3f - 2f * u), len = Mountain.wayLength();
        float sNow = roadUp ? e * len : (1f - e) * len;
        Mountain.wayAt(sNow, roadAt);
        p.x = roadAt[0];
        p.y = roadAt[1];
        p.z = roadAt[2];
        p.heading = roadUp ? roadAt[3] : roadAt[3] + (float) Math.PI;
        p.speed = ROAD_ROLL;
        p.steer = 0f;
        p.seat(seatAt);
        runner.ride(dt, city, p.x, p.y, p.z, p.heading, p.speed, seatAt[0], seatAt[1], seatAt[2], 0f);
        if (u >= 1f) {
            roadT = -1f;
            roadWasX = p.x;
            roadWasZ = p.z;
            runner.recenter();
        }
    }

    /**
     * The eye on the drive: behind the pickup at first, then up and up over the mountain, looking
     * nearly straight down on it going round, the way it went lit; at the end down again behind
     * it, where it comes out.
     */
    private void roadEye() {
        Pickup p = driving;
        float u = Math.min(1f, roadT / ROAD_S), w = (float) Math.pow(Math.sin(Math.PI * u), 0.6);
        float high = ROAD_EYE_LOW + (ROAD_EYE_HIGH - ROAD_EYE_LOW) * w, back = ROAD_EYE_NEAR + (ROAD_EYE_BACK - ROAD_EYE_NEAR) * w;
        // up high the eye looks north, the map's way; low, the pickup's
        float d = 0f - p.heading;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        float yaw = p.heading + d * w;
        roadEye[0] = p.x - (float) Math.sin(yaw) * back;
        roadEye[1] = p.y + high;
        roadEye[2] = p.z - (float) Math.cos(yaw) * back;
        roadEye[3] = yaw;
        roadEye[4] = -(float) Math.atan2(high - 1.5f, back + 6f);
    }

    /** The ring at the wheel, coloured for what she is about: the hunt coral, the folder turquoise, a delivery amber, free cream. */
    private int ringTint() {
        if (free) return DrawList.TINT_CREAM;
        if (rescue != null) return DrawList.TINT_GREEN;
        if (puppy != null) return DrawList.TINT_TURQUOISE;
        if (tow != null) return DrawList.TINT_AMBER;
        if (archive != null || mend != null || port != null || lab != null || cherry != null) return DrawList.TINT_TURQUOISE;
        if (fault != null) return DrawList.TINT_AMBER;
        if (concert != null) return DrawList.TINT_CREAM;
        if (delivery != null) return DrawList.TINT_AMBER;
        if (folder != null) return DrawList.TINT_TURQUOISE;
        return DrawList.TINT_CORAL;
    }

    // the thumb on the steering wheel, as parts of so far aside from where it came down: no turn
    // within so far, then more and more (so steep a curve) to full over at the edge
    static final float CAR_DEAD = 0.08f, CAR_CURVE = 1.4f, CAR_R = 50f;
    /** Up or down, nothing within so far; the throttle barely touched, so much of the top (a crawl). */
    static final float CAR_PULL_DEAD = 0.2f, CAR_CRAWL = 0.2f;
    /** In the town, the fastest it goes (meters a second); out on the road, its top. */
    static final float TOWN_TOP = 22f;
    /** The steering wheel at rest, where the left thumb would come down: across, up from the bottom. */
    static final float WHEEL_REST_X = 78f, WHEEL_REST_UP = 96f;

    /** The steering wheel as the thumb has it aside: -1 (full left) to 1. */
    private float wheelAxis() {
        float v = Math.max(-1f, Math.min(1f, (carTX - carCX) / CAR_R)), a = Math.abs(v);
        return a < CAR_DEAD ? 0f : Math.signum(v) * (float) Math.pow((a - CAR_DEAD) / (1f - CAR_DEAD), CAR_CURVE);
    }

    /**
     * The same thumb up or down: 1 (the throttle full) to -1 (the brake full), nothing within its
     * own middle, wider than the wheel's, so a thumb turning the wheel does not touch the throttle.
     */
    private float pullAxis() {
        float v = Math.max(-1f, Math.min(1f, (carCY - carTY) / CAR_R)), a = Math.abs(v);
        return a < CAR_PULL_DEAD ? 0f : Math.signum(v) * (a - CAR_PULL_DEAD) / (1f - CAR_PULL_DEAD);
    }

    // the steering wheel drawn by the finger: half its size, frame pixels; turned so far at full lock (radians)
    static final float WHEEL_HALF = 132f, WHEEL_TURNS = 2.2f;           // the ring at the wheel, its radius: wider than her stick on foot
    /** The thumb's middle (what the wheel turns from) kept so far in from the frame's edges, room for its reach. */
    static final float STEER_MARGIN = 56f;
    /** How strongly the wheel shows at rest, the thumb off it. */
    static final float WHEEL_RESTING = 0.25f;
    /** How strongly the wheel shows while the thumb is on it: the pickup under it seen through. */
    static final float WHEEL_SEEN = 0.45f;

    /** Where the wheel is drawn, across: by the thumb's middle, kept in from the sides so most of it shows. */
    private float wheelDrawX(float x) {
        float m = WHEEL_HALF * 0.85f;
        return Math.max(Math.min(m, W * 0.5f), Math.min(Math.max(W - m, W * 0.5f), x));
    }

    /** Where the wheel is drawn, down: by the thumb's middle, kept up from the bottom so most of it shows. */
    private float wheelDrawY(float y) {
        return Math.max(h * 0.3f, Math.min(h - WHEEL_HALF * 0.85f, y));
    }
    static final float SETTLE_RATE = 3.5f;     // how fast it settles along the street, the wheel let be
    final Felled felled = new Felled();
    private boolean swungSeen;
    private int rescueHits;
    // parked cars rammed off their curbs: where each stood, put back once she is far off
    private final float[] parkedAt = new float[5], unparkedX = new float[24], unparkedZ = new float[24];
    private int unparkedN;

    private void unparkedHere(float x, float z) {
        if (unparkedN == unparkedX.length) {
            city.repark(unparkedX[0], unparkedZ[0]);
            System.arraycopy(unparkedX, 1, unparkedX, 0, unparkedN - 1);
            System.arraycopy(unparkedZ, 1, unparkedZ, 0, unparkedN - 1);
            unparkedN--;
        }
        unparkedX[unparkedN] = x;
        unparkedZ[unparkedN] = z;
        unparkedN++;
    }

    /** The parked cars rammed off put back at their curbs once she is far from where they stood. */
    private void reparkFar() {
        for (int k = unparkedN - 1; k >= 0; k--) {
            float dx = unparkedX[k] - runner.x, dz = unparkedZ[k] - runner.z;
            if (dx * dx + dz * dz < 230f * 230f) continue;
            city.repark(unparkedX[k], unparkedZ[k]);
            unparkedN--;
            unparkedX[k] = unparkedX[unparkedN];
            unparkedZ[k] = unparkedZ[unparkedN];
        }
    }
    private final float[] fellAt = new float[2];
    static final float SETTLE_WITHIN = 0.2f;   // radians off a street's way, at most, for the pickup to settle along it
    static final float SETTLE_WHEEL = 0.05f;   // turned so far or more, it is let go its own way
    private float carCX, carCY, carTX, carTY;

    /**
     * The thumb at the wheel: the steering wheel, its middle where the thumb came down, anywhere
     * but on a button; the pause; the way out.
     */
    private void carTouch(int type, int id, float x, float y) {
        if (type == DOWN && paused) {
            if (!pauseRow(x, y)) paused = false;
            return;
        }
        if (type == DOWN && onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            letGo();
            return;
        }
        if (type == DOWN && onLiftButton(x, y)) {
            boolean going = Math.abs(driving.speed) > STOPPED;
            if (exitWaiting) {
                // touched again while it waits: not after all
                exitWaiting = false;
                host.buzz(10);
                return;
            }
            if (going && wheelOverExit()) {
                // the wheel over the way out: a thumb there may be after the wheel; out once it stands
                exitWaiting = true;
                host.buzz(12);
                return;
            }
            // the wheel clear of it: out now, on the go if it goes, the pickup rolling on
            getOut(going);
            // one finger to play: this one is the lead now, so its lifting is the button's
            if (Runner.classic) leadFinger = id;
            return;
        }
        if (type == DOWN) {
            if (wheelFinger >= 0) return;
            wheelFinger = id;
            float m = STEER_MARGIN;
            carCX = Math.max(m, Math.min(W - m, x));
            carCY = Math.max(h * 0.3f, Math.min(h - m, y));
            carTX = x;
            carTY = y;
        } else if (type == MOVE) {
            if (id == wheelFinger) {
                carTX = x;
                carTY = y;
            }
        } else if (type == UP) {
            if (id == wheelFinger) wheelFinger = -1;
        }
    }

    // ---- errands: a sign touched, she goes where it says by herself ----
    static final int NO_ERRAND = 0, ERRAND_CLIMB = 1, ERRAND_UP = 2, ERRAND_DOWN = 3, ERRAND_DRIVE = 4, ERRAND_THROUGH = 5, ERRAND_SOFA = 6, ERRAND_BED = 7;
    /** The thing of a bed she goes for (see ERRAND_BED). */
    private int errandThing;
    private boolean errandIn;                  // through a house: she has been inside
    static final int MARK_PICKUP = 4;          // a sign's kind beside the town's (see City.marks)
    static final int MARK_SOFA = 7;            // the sign over home's sofa
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

    // ---- the sign over home's sofa ----
    static final float SOFA_SIGN_Y = 2.4f;   // over the floor

    /**
     * The sign over home's sofa, the game kept, drawn and kept for the finger (the only sign
     * indoors), while nothing is going on that sitting down would cut short.
     */
    private void sofaSign(DrawList d) {
        shownN = 0;
        if (!sofaOffered() || d.badgeN >= DrawList.BADGE_MAX - 3) return;
        Indoors in = indoors;
        float sx = in.wx(in.sofaU, in.sofaV), sz = in.wz(in.sofaU, in.sofaV);
        float bob = 0.08f * (float) Math.sin(time * 2.2f);
        if (!d.project(sx, Indoors.FLOOR + SOFA_SIGN_Y + bob, sz, markAt) || markAt[2] < 0.5f) return;
        if (markAt[0] < -20f || markAt[0] > W + 20f || markAt[1] < -20f || markAt[1] > h + 20f) return;
        float size = Math.max(9f, Math.min(18f, W * 1.6f * 0.75f / markAt[2]));
        boolean mine = errand == ERRAND_SOFA;
        d.badge(markAt[0], markAt[1], size, Badges.SAVE, 0.9f, mine ? 0.6f + 0.4f * (float) Math.sin(time * 8f) : 0f);
        float[] o = shown;
        o[0] = markAt[0];
        o[1] = markAt[1];
        o[2] = size;
        o[3] = Badges.SAVE;
        o[4] = 1f;
        o[5] = sx;
        o[6] = sz;
        o[7] = 0f;
        o[8] = 0f;
        o[9] = MARK_SOFA;
        shownN = 1;
    }

    /** Whether the sign over home's sofa is shown: at home, up, no mission on, nothing on the screen. */
    private boolean sofaOffered() {
        Indoors in = indoors;
        return in != null && in.home && !in.seated && free && dispatch == null && swapT < 0f && !paused;
    }

    /** The sign under a finger that can be touched where she is: any out of doors, indoors only the sofa's. */
    private int signHere(float x, float y) {
        int sgn = signAt(x, y);
        if (sgn < 0 || indoors == null) return sgn;
        return (int) shown[sgn * 10 + 9] == MARK_SOFA ? sgn : -1;
    }

    /** For the desktop stand: a touch on the sign over home's sofa, if it is there. */
    boolean standSofaSign() {
        for (int i = 0; i < shownN; i++) if ((int) shown[i * 10 + 9] == MARK_SOFA) {
            float x = shown[i * 10], y = shown[i * 10 + 1];
            touch(DOWN, 0, x, y);
            touch(UP, 0, x, y);
            return true;
        }
        return false;
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
        if (kind == MARK_SOFA) {
            if (indoors == null || !indoors.home) return;
            // up to it, as from the door: she sits once she is there
            indoors.sofaArmed = true;
            indoors.goal(r, way);
            r.lead(way[0], way[1], (float) Math.atan2(mx - way[0], mz - way[1]));
            errand = ERRAND_SOFA;
        } else if (kind == MARK_PICKUP) {
            Pickup p = pickupAt(mx, mz);
            if (p == null) return;
            // to the nearest of it, whichever side or corner: she gets in wherever she comes up to it
            p.beside(r.x, r.z, 0.7f, way);
            // the yacht from the pier: to the pier's edge by it, not off it into the water
            if (p.boat && !r.swimming() && Island.toPier(way[0], way[1], pierAt) < BOARD_FROM_PIER) {
                way[0] = pierAt[0];
                way[1] = pierAt[1];
            }
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
            case ERRAND_BED:
                // at the bed's back: the thing out of it into her hands
                if (errandPickup != null && (arrived || atBed(errandPickup))) {
                    r.stopLead();
                    kit.take(errandThing, errandPickup);
                    host.sound(Synth.WOOD, 0.4f, errandThing == Kit.ROPE ? 0.8f : 1.2f);
                    host.buzz(15);
                    errand = NO_ERRAND;
                } else if (!r.led()) errand = NO_ERRAND;
                break;
            case ERRAND_SOFA:
                if (indoors == null || !indoors.home || indoors.seated) {
                    errand = NO_ERRAND;
                    r.stopLead();
                } else if (!r.led() && !arrived) errand = NO_ERRAND;
                break;
            case ERRAND_DRIVE:
                if (arrived || errandPickup != null && errandPickup.apart(r.x, r.z) < errandPickup.reach(BY_IT) && !r.inAir()) {
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
        if (indoors != null || swapT >= 0f || r.climbing() || r.throwing() >= 0f) return;
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
                indoors = City.townHall(houseOf[0], houseOf[1]) ? Rooms.townHall(houseOf)
                        : Rooms.hall(houseOf, seed, city.backClear(houseOf, 0), houseOf[7] == City.LIFT);
                indoors.enter(r, l[10] > 0.5f);
                break;
            case TO_CABIN:
                indoors = Rooms.hall(houseOf, seed, city.backClear(houseOf, 0), true);
                indoors.comeDown(r);
                break;
            case TO_ARCHIVE:
                indoors = Rooms.archive(City.archiveDoorX(), City.archiveWallZ());
                indoors.enter(r, false);
                break;
            case TO_CONCERT:
                indoors = Rooms.concert(City.concertDoorX(), City.concertDoorZ());
                indoors.enter(r, false);
                // no job here now: the hall as she left it, lit, full, her chair waiting
                if (concert == null) stayed = Concert.open(host, 0xC0C0L + (long) (time * 1000f));
                break;
            case TO_HOME:
                indoors = homeRoom();
                indoors.enter(r, l[10] > 0.5f);
                break;
            case TO_OLD:
                indoors = Rooms.oldFlat(City.OLD_DOOR_X, City.OLD_DOOR_Z);
                indoors.enter(r, l[10] > 0.5f);
                break;
            case TO_LOBBY:
                indoors = Rooms.lobby(Mountain.MX, Mountain.DOOR_Z, colliderOpen());
                indoors.enter(r, false);
                break;
            case TO_COLLIDER:
                // down the lobby's lift: out of it into the collider hall
                indoors = Rooms.collider(cherry == null && (kindsWon & (1 << 13)) != 0);
                indoors.comeDown(r);
                break;
            case TO_LOBBY_UP:
                // up the hall's lift: out of it into the lobby
                indoors = Rooms.lobby(Mountain.MX, Mountain.DOOR_Z, true);
                indoors.comeDown(r);
                break;
            case TO_TOWER:
                // up the lighthouse's stairs in the dark: out on its gallery, on the side to the town
                r.put(Island.IX, Island.GALLERY_Y, Island.IZ + Island.TOWER_TOP + 0.65f, 0f);
                r.recenter();
                // safe up here: the eye down from on high at once, in the dark
                r.chase = 0f;
                host.sound(Synth.DOOR, 0.35f, 1.1f);
                if (herd != null && herd.step == Herd.LIGHT) {
                    herd.onTower();
                    towerSay = true;
                }
                break;
            case TO_SECRET:
                indoors = Rooms.secret(City.OLD_DOOR_X, City.OLD_DOOR_Z);
                indoors.enter(r, false);
                if (herd != null) herd.inHall();
                // found the first time: the stream goes wild
                if (host.loadInt(SAVE_SECRET) == 0) {
                    host.saveInt(SAVE_SECRET, 1);
                    like(SECRET_LIKES, SECRET_HEARTS);
                    saveLikes();
                }
                break;
            case TO_ROOF:
                leave();
                r.put(l[4], l[5], l[6], l[7]);
                host.sound(Synth.DOOR, 0.35f, 1.2f);
                break;
            default: {
                if (indoors != null && indoors.concert) {
                    // out of the hall's door, down off its portico onto the pavement, facing the street
                    leave();
                    r.put(City.concertDoorX(), City.WALK_TOP, City.CONCERT_J * City.CELL + City.FRONT - 1.2f, (float) Math.PI);
                    liftArmed = false;
                    host.sound(Synth.DOOR, 0.35f, 1.1f);
                    break;
                }
                if (indoors != null && (indoors.lobby || indoors.collider)) {
                    // out of the laboratory's door onto the podium, facing the deck and the town
                    // (from the hall under it too: the job put by, or played again)
                    leave();
                    r.put(Mountain.MX, Mountain.TOP + Mountain.PODIUM_HIGH, Mountain.DOOR_Z - OUT_STEP - 0.8f, (float) Math.PI);
                    host.sound(Synth.DOOR, 0.35f, 1.05f);
                    break;
                }
                if (indoors != null && indoors.archive) {
                    // out of the archive's door onto the garden, facing it
                    leave();
                    r.put(City.archiveDoorX(), City.WALK_TOP, City.archiveWallZ() + OUT_STEP + 1.2f, 0f);
                    host.sound(Synth.DOOR, 0.35f, 0.95f);
                    break;
                }
                if (indoors != null && indoors.secret) {
                    // out of the hall's door onto the balcony, facing the sea
                    leave();
                    r.put(City.OLD_DOOR_X, City.SECRET_Y, City.OLD_DOOR_Z - City.SECRET_DEEP + 0.6f, (float) Math.PI);
                    host.sound(Synth.DOOR, 0.35f, 0.9f);
                    break;
                }
                if (indoors != null && indoors.home && swapTo == TO_YARD) {
                    // out of home's back door into the yard, facing away from it
                    leave();
                    city.homeHouse(houseOf);
                    City.lift(houseOf, 0, doorAt);
                    float nx = doorAt[2], nz = doorAt[3];
                    float half = Math.abs(nx) * houseOf[2] + Math.abs(nz) * houseOf[3];
                    float px = doorAt[0] - nx * (2f * half + 1.4f + OUT_STEP), pz = doorAt[1] - nz * (2f * half + 1.4f + OUT_STEP);
                    r.put(px, city.ground(px, pz), pz, (float) Math.atan2(-nx, -nz));
                    liftArmed = true;
                    host.sound(Synth.DOOR, 0.35f, 1.1f);
                    break;
                }
                if (indoors != null && indoors.home) {
                    // out of home's door onto its forecourt, facing the street and the shop
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
            if (!in.sofaArmed || r.inAir() || r.led() && errand != ERRAND_THROUGH && errand != ERRAND_SOFA || !in.atSofa(r)) return false;
            sitDown();
        }
        seatedFor += dt;
        r.lounging = true;
        in.seat(sofaAt);
        r.ride(dt, in, sofaAt[0], sofaAt[1], sofaAt[2], in.seatHeading(), 0f, sofaAt[0], sofaAt[1] - 0.03f, sofaAt[2], 0f);
        // the eye where the room is seen from while she sits (the screen in her head comes on from there)
        if (in.shot(eyeNow)) r.eyeAt(eyeNow);
        if (!sofaAsked && seatedFor > SIT_FOR && morning) {
            // the morning after the concert: the next story on the screen at once
            sofaAsked = true;
            morning = false;
            chooseMission(8);
            dispatch = new Dispatch(host, briefing()).story(8);
            letGo();
            return true;
        }
        if (!sofaAsked && seatedFor > SIT_FOR) {
            sofaAsked = true;
            sofaChoice(waiting);
            if (waiting != null && waitingLikes > 0) hearts.pour(REPORT_HEARTS);
            if (waiting != null) {
                // the way home has been told once in this game: from now on she knows it
                host.saveInt(SAVE_TOLD, 1);
                told = true;
            }
            waiting = null;
        }
        return true;
    }

    // ---- in the town's helicopter, beside its pilot ----
    /** Whether she sits in the helicopter now. */
    boolean aboard() {
        return archive != null && archive.aboard();
    }

    private float flyYaw = Float.NaN;

    /** A frame of it: in the seat beside the pilot, the eye behind and over the helicopter. */
    private void ride(float dt) {
        Copter cp = archive.copter;
        Runner r = runner;
        cp.seat(false, seatAt);
        r.lounging = false;
        r.ride(dt, city, seatAt[0], seatAt[1], seatAt[2], cp.heading, 0f, seatAt[0], seatAt[1], seatAt[2], 0f);
        copterEye(cp, dt, false);
    }

    /**
     * The eye on the helicopter, following it round a little late: behind it and off to the right
     * as a passenger sees it fly (where its open side shows who sits in it); nearly straight behind,
     * looking further ahead, while she flies it herself.
     */
    private void copterEye(Copter cp, float dt, boolean hands) {
        Runner r = runner;
        if (Float.isNaN(flyYaw)) flyYaw = cp.heading;
        float d = cp.heading - flyYaw;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        flyYaw += d * Math.min(1f, dt * 1.6f);
        float fx = (float) Math.sin(flyYaw), fz = (float) Math.cos(flyYaw), rx = fz, rz = -fx;
        // behind it and off to the right, where its open side shows who sits in it; nearer and
        // higher as it comes down among the houses, so the eye stays out of them
        float low = Math.max(0f, Math.min(1f, (cp.y - City.WALK_TOP) / 20f));
        float back = FLY_BACK * (0.45f + 0.55f * low), aside = FLY_ASIDE * (0.5f + 0.5f * low), up = FLY_UP + 4f * (1f - low);
        if (hands) {
            back = FLY_BACK + 3f;
            aside = 1.5f;
            up = FLY_UP + 1.5f;
        }
        eyeNow[0] = cp.x - fx * back + rx * aside;
        eyeNow[1] = cp.y + up;
        eyeNow[2] = cp.z - fz * back + rz * aside;
        float ahead = hands ? 10f : 3f;
        float lx = cp.x + fx * ahead - eyeNow[0], lz = cp.z + fz * ahead - eyeNow[2];
        eyeNow[3] = (float) Math.atan2(lx, lz);
        eyeNow[4] = (float) Math.atan2(cp.y + 1.5f - eyeNow[1], Math.sqrt(lx * lx + lz * lz));
        r.eyeAt(eyeNow);
    }

    // the eye on the helicopter: so far behind it, so far to its right, so high over it
    static final float FLY_BACK = 12f, FLY_ASIDE = 8f, FLY_UP = 4.5f;

    // ---- the helicopter in her own hands: the mend's flight home, and after it whenever she likes ----
    static final String SAVE_COPTER = "copter";
    // the sticks: their rim's radius, frame pixels; no answer within so much of it; where each
    // sits, faint, when no thumb is on it (parts of the frame's width, frame pixels up from the foot)
    static final float HELI_R = 40f, HELI_DEAD = 0.15f, HELI_LEFT = 0.24f, HELI_RIGHT = 0.76f, HELI_UP = 110f;
    /** Standing so near its door a moment, she gets in. */
    static final float BOARD_NEAR = 2.8f, BOARD_AWAY = 3.6f;
    // the thumbs on the sticks (-1: none), where each came down and where it is
    private int leftStick = -1, rightStick = -1;
    private float lCX, lCY, lTX, lTY, rCX, rCY, rTX, rTY;
    private float copterWait;
    private boolean copterArmed = true;
    private final float[] copterDoor = new float[3];

    /** The helicopter she can be at the controls of now: the mend's, or her own. */
    private Copter heli() {
        return mend != null ? mend.copter : parked;
    }

    /** Whether she flies the helicopter now, at its controls. */
    boolean piloting() {
        return flying || mend != null && mend.aboard();
    }

    /** The helicopter as the archive job leaves it: in the archive's garden on its pad, leaning on a broken skid. */
    private static Copter brokenAtArchive() {
        Copter c = new Copter(City.archivePadX(), City.WALK_TOP, City.archivePadZ(), 0f);
        c.standAt(City.archivePadX(), City.WALK_TOP, City.archivePadZ(), (float) Math.PI * 0.5f, true);
        return c;
    }

    /** The helicopter where it was left, kept with the game: broken (1) or whole (2), its place and way. */
    private void saveCopter() {
        Copter c = parked;
        if (c == null) return;
        host.saveInt(SAVE_COPTER, c.broken ? 1 : 2);
        host.saveInt(SAVE_COPTER + ".x", Math.round(c.x * 100f));
        host.saveInt(SAVE_COPTER + ".y", Math.round(c.y * 100f));
        host.saveInt(SAVE_COPTER + ".z", Math.round(c.z * 100f));
        host.saveInt(SAVE_COPTER + ".h", Math.round(c.heading * 1000f));
    }

    /** The helicopter as the game kept it; a game kept before it was: where the jobs done left it. */
    private void loadCopter() {
        int kept = host.loadInt(SAVE_COPTER);
        if (kept == 0) {
            if ((kindsWon & (1 << 9)) != 0) {
                parked = new Copter(City.townPadX(), City.townRoof(), City.townPadZ(), 0f);
                parked.standAt(City.townPadX(), City.townRoof(), City.townPadZ(), (float) Math.PI * 0.5f, false);
            } else if ((kindsWon & (1 << 8)) != 0) parked = brokenAtArchive();
            return;
        }
        float x = host.loadInt(SAVE_COPTER + ".x") / 100f, y = host.loadInt(SAVE_COPTER + ".y") / 100f;
        float z = host.loadInt(SAVE_COPTER + ".z") / 100f, hd = host.loadInt(SAVE_COPTER + ".h") / 1000f;
        parked = new Copter(x, y, z, hd);
        parked.standAt(x, y, z, hd, kept == 1);
    }

    private final float[] mendAt = new float[3], sceneEyeNow = new float[5];

    /**
     * The eye of the mend's scenes: the skid put in the bed, from across the road, the gate and the
     * pickup both in sight; the mending, from beside the helicopter, the skid and her in sight.
     */
    // ---- in at the vets' door with the puppy ----
    // out on the pavement before the door, where she is led; so far in past it, where she goes;
    // how long she is led before she is walked in straight whatever is in the way; how long the
    // doors stand shut behind her before the job is done; how quick they slide
    static final float CLINIC_BEFORE = 1.3f, CLINIC_IN = 1.2f, CLINIC_LED_FOR = 6f, CLINIC_SHUT_FOR = 0.6f, CLINIC_SLIDE = 2.2f;
    // out again after: so far out on the pavement, clear of the awning
    static final float CLINIC_OUT = 2.3f;
    // the eye on it: so far across, out into the street, so high
    static final float CLINIC_EYE_ACROSS = 4.5f, CLINIC_EYE_OUT = 9.5f, CLINIC_EYE_UP = 2.8f;
    private int clinicPart;
    private float clinicT, clinicOpen;
    private final float[] clinicEyeNow = new float[5];

    /**
     * A frame at the vets' with the puppy: out of the pickup, led to the door, the doors sliding
     * open, in through them, and shut behind her; then the dispatcher: the job done, home now. Out
     * on the pavement again as the screen comes on: the way home is hers.
     */
    private void clinicStep(float dt, boolean begun) {
        Runner r = runner;
        float doorX = City.clinicDoorX(), doorZ = City.clinicDoorZ();
        if (begun) {
            letGo();
            errand = NO_ERRAND;
            if (driving != null) getOut();
            clinicPart = 0;
            clinicT = 0f;
            clinicOpen = 0f;
            r.lead(doorX, doorZ - CLINIC_BEFORE, 0f);
        }
        clinicT += dt;
        float dx = r.x - doorX, dz = r.z - (doorZ - CLINIC_BEFORE);
        if (clinicPart == 0 && (r.arrived() || dx * dx + dz * dz < 0.3f * 0.3f || clinicT > CLINIC_LED_FOR)) {
            // at the door: in through it, straight
            clinicPart = 1;
            clinicT = 0f;
            r.stopLead();
            r.walkIn(doorX, doorZ + CLINIC_IN);
        }
        // the doors open as she comes up, and shut once she is in
        boolean open = clinicPart == 1 || clinicPart == 0 && dx * dx + dz * dz < 2.5f * 2.5f;
        clinicOpen = Math.max(0f, Math.min(1f, clinicOpen + (open ? dt : -dt) * CLINIC_SLIDE));
        if (clinicPart == 1 && r.z > doorZ + 0.45f) {
            clinicPart = 2;
            clinicT = 0f;
            r.hidden = true;
        }
        if (clinicPart == 2 && clinicOpen <= 0f && clinicT > CLINIC_SHUT_FOR) {
            // in: the job done. She is out on the pavement again, turned along it the way home is
            puppy.step = Puppy.DONE;
            r.hidden = false;
            float home = City.HOME_DOOR_X < doorX ? -1f : 1f, outZ = doorZ - CLINIC_OUT;
            r.put(doorX + home * 1.5f, city.ground(doorX, outZ), outZ, home * (float) Math.PI * 0.5f);
            r.recenter();
            host.sound(Synth.CHIME, 0.6f, 1f);
            dispatch = new Dispatch(host, Lang.lines("puppy.done", mission.round, null));
        }
    }

    /** The vets' glass doors, slid apart so far, the dark of the hall between them. */
    private void clinicDoors(DrawList d) {
        float cx = City.clinicDoorX(), fz = City.clinicDoorZ(), o = clinicOpen;
        float leaf = 0.875f, slide = 0.85f * o;
        if (o > 0.01f) d.box(cx, 1.4f, fz - 0.115f, slide, 1.3f, 0.01f, null, 0x0E1418, DrawList.PLAIN);
        for (int e = -1; e <= 1; e += 2) {
            float lx = cx + e * (leaf * 0.5f + slide);
            d.box(lx, 1.4f, fz - 0.135f, leaf * 0.5f, 1.3f, 0.012f, null, 0x2A3E48, DrawList.GLASS);
            d.box(lx - e * leaf * 0.5f, 1.4f, fz - 0.15f, 0.035f, 1.3f, 0.015f, null, 0x1E2428, DrawList.PLAIN);
        }
    }

    /** The eye on the vets' door from across the pavement, a little aside, out toward the street. */
    private void clinicEye() {
        float tx = City.clinicDoorX(), tz = City.clinicDoorZ() - 0.8f, ty = 1.2f;
        float ex = tx + CLINIC_EYE_ACROSS, ez = tz - CLINIC_EYE_OUT, ey = CLINIC_EYE_UP;
        float dx = tx - ex, dz = tz - ez;
        clinicEyeNow[0] = ex;
        clinicEyeNow[1] = ey;
        clinicEyeNow[2] = ez;
        clinicEyeNow[3] = (float) Math.atan2(dx, dz);
        clinicEyeNow[4] = (float) Math.atan2(ty - ey, Math.sqrt(dx * dx + dz * dz));
        runner.eyeAt(clinicEyeNow);
    }

    private void mendEye() {
        Mend m = mend;
        float tx, ty, tz, ex, ey, ez;
        if (m.step == Mend.LOADING) {
            Pickup v = m.van;
            tx = (v.x + Mend.gateX()) * 0.5f;
            tz = (v.z + Mend.gateZ()) * 0.5f - 1.5f;
            ty = 1.4f;
            ex = tx + MEND_EYE_ACROSS;
            ez = tz - MEND_EYE_BACK;
            ey = MEND_EYE_UP;
        } else {
            Copter cp = m.copter;
            m.fit(mendAt);
            float c = (float) Math.cos(cp.heading), s = (float) Math.sin(cp.heading);
            tx = mendAt[0];
            ty = mendAt[1] + 0.8f;
            tz = mendAt[2];
            // out from its right side and ahead of it, low
            ex = tx + c * MEND_EYE_ACROSS * 0.7f + s * MEND_EYE_BACK * 0.5f;
            ez = tz - s * MEND_EYE_ACROSS * 0.7f + c * MEND_EYE_BACK * 0.5f;
            ey = MEND_EYE_UP * 0.75f;
        }
        float dx = tx - ex, dz = tz - ez;
        sceneEyeNow[0] = ex;
        sceneEyeNow[1] = ey;
        sceneEyeNow[2] = ez;
        sceneEyeNow[3] = (float) Math.atan2(dx, dz);
        sceneEyeNow[4] = (float) Math.atan2(ty - ey, Math.sqrt(dx * dx + dz * dz));
        runner.eyeAt(sceneEyeNow);
    }

    // the mend's scenes: the eye so far across, so far back, so high
    static final float MEND_EYE_ACROSS = 11f, MEND_EYE_BACK = 7f, MEND_EYE_UP = 4f;

    /** A frame of the mend: what its step brought, heard and done. */
    private void mendStep(float dt) {
        Mend m = mend;
        int got = m.update(dt, driving, runner, host, boarding(dt, m.copter, m.step == Mend.BOARD));
        if (m.van != null) m.van.kept = true;
        // mending: she stands at the helicopter's side, facing where the skid goes
        if (m.step == Mend.MENDING) {
            m.fit(mendAt);
            runner.held = true;
            runner.speed = 0f;
            if (m.work() < Mend.FIT_FOR) runner.heading = (float) Math.atan2(mendAt[0] - runner.x, mendAt[2] - runner.z);
        }
        switch (got) {
            case Mend.LOADED:
                // the skid put in the bed by the workshop's brums
                host.sound(Synth.CLANG, 0.6f, 0.7f);
                host.sound(Synth.WOOD, 0.5f, 0.8f);
                host.buzz(60);
                break;
            case Mend.TAKEN:
                host.sound(Synth.CLANG, 0.4f, 0.9f);
                host.buzz(30);
                break;
            case Mend.BLOW: {
                // the mallet swung at the skid where it goes
                m.fit(mendAt);
                runner.lock(Toss.STRIKE_LOCK, mendAt[0], mendAt[2]);
                host.sound(Synth.CLANG, 0.55f, 1.1f + 0.1f * (float) Math.sin(time * 7f));
                host.buzz(25);
                break;
            }
            case Mend.BAY:
                // in the bay: the workshop's brum comes out with the skid
                host.sound(Synth.DOOR, 0.4f, 0.8f);
                break;
            case Mend.MENDED:
                runner.held = false;
                host.sound(Synth.CHIME, 0.5f, 1.2f);
                host.buzz(60);
                break;
            case Mend.IN:
                takeSeat();
                break;
            case Mend.OUT:
                outOf(m.copter);
                break;
            default:
                break;
        }
    }

    /**
     * Whether she has come to the helicopter's door to get in, on foot, standing there a moment
     * (once she has been away from it since she last got out); allowed: whether getting in is open now.
     */
    private boolean boarding(float dt, Copter cp, boolean allowed) {
        Runner r = runner;
        if (cp == null || !allowed || driving != null || indoors != null || swapT >= 0f || r.climbing()) {
            copterWait = 0f;
            return false;
        }
        cp.door(copterDoor);
        float dx = r.x - copterDoor[0], dz = r.z - copterDoor[1], dd = dx * dx + dz * dz;
        if (dd > BOARD_AWAY * BOARD_AWAY) copterArmed = true;
        if (dd > BOARD_NEAR * BOARD_NEAR || r.inAir() || Math.abs(r.y - cp.y) > 1.6f || !copterArmed) {
            copterWait = 0f;
            return false;
        }
        return (copterWait += dt) > GET_IN_STAND;
    }

    /** On foot by her own helicopter's door: in at the controls. */
    private void board(float dt) {
        Copter cp = parked;
        // on a job with the helicopter in it, the job has the getting in (see mendStep)
        if (archive != null || mend != null) return;
        boolean hers = cp != null && !cp.broken && (kindsWon & (1 << 9)) != 0;
        if (!boarding(dt, cp, hers)) return;
        flying = true;
        cp.takeControls();
        takeSeat();
    }

    /** In at the controls: the blades run up. */
    private void takeSeat() {
        errand = NO_ERRAND;
        runner.stopLead();
        letGo();
        copterWait = 0f;
        flyYaw = Float.NaN;
        host.sound(Synth.DOOR, 0.4f, 1.4f);
        host.sound(Synth.WHOOSH, 0.5f, 0.6f);
        host.buzz(30);
    }

    /** Out of the helicopter by its door, onto whatever it stands on. */
    private void outOf(Copter cp) {
        cp.door(copterDoor);
        float x = copterDoor[0], z = copterDoor[1];
        float y = Math.max(city.ground(x, z), city.ground(x, z, cp.y, 0.6f));
        runner.getOut(x, y, z, cp.heading);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        letGo();
        copterArmed = false;
        host.sound(Synth.DOOR, 0.4f, 1.3f);
    }

    /** Out of her own helicopter, set down: it stays where it stands, kept with the game. */
    private void leaveCopter() {
        Copter cp = parked;
        flying = false;
        cp.leave();
        outOf(cp);
        saveCopter();
    }

    // ---- the crane in the port ----
    static final String SAVE_FIELDS = "port.fields";
    // a stack touched within so far of its middle on the frame, frame pixels
    static final float BAY_REACH = 46f;
    // the eye over the field: so high, so far back toward the shore, looking down at it
    static final float CRANE_EYE_UP = 50f, CRANE_EYE_BACK = 28f, CRANE_LENS = 2.6f, CRANE_LOOK_Z = -1f;
    private final float[] bayAt = new float[Quay.SLOTS * 2];
    private boolean baysSeen;

    /** How many colours the next field has: one more for every field done, to the most. */
    private int fieldColours() {
        return Quay.FEWEST + Math.min(Quay.MOST - Quay.FEWEST, host.loadInt(SAVE_FIELDS));
    }

    /** On foot at the top of the crane's tower: into its cabin, on the port's job or on none. */
    private void toCrane() {
        Runner r = runner;
        if (!(free || port != null) || indoors != null || r.climbing() || r.inAir()) return;
        if (r.y < Quay.TOWER_TOP - 0.6f || Math.abs(r.x - Quay.TOWER_X) > 1.6f || Math.abs(r.z - Quay.TOWER_Z) > 1.6f) return;
        startCrane(false);
    }

    /** Into the crane's cabin: the eye over the field; a field done is dealt anew. From home's list: no way to go. */
    private void startCrane(boolean solo) {
        if (solo) {
            if (driving != null) getOut();
            runner.getOut(runner.x, runner.y, runner.z, runner.heading);
            if (indoors != null) {
                indoors = null;
                swapT = -1f;
            }
        }
        craning = true;
        craneSolo = solo;
        baysSeen = false;
        errand = NO_ERRAND;
        runner.stopLead();
        letGo();
        if (quay.solved) quay.deal(fieldColours());
        crane(0f);
        host.sound(Synth.LATCH, 0.5f, 1.1f);
        host.buzz(20);
    }

    /** Out of the cabin: down the tower to its foot; from home's list, back on her sofa. */
    private void leaveCrane() {
        craning = false;
        quay.putBack();
        letGo();
        if (craneSolo) {
            craneSolo = false;
            runner.getOut(runner.x, runner.y, runner.z, runner.heading);
            indoors = homeRoom();
            indoors.enter(runner, false);
            runner.recenter();
            runner.update(0f, city, cars, walkers);
            sitDown();
            sofaChoice(null);
            return;
        }
        runner.getOut(Quay.TOWER_X - 1.2f, City.WALK_TOP, Quay.TOWER_Z - 4.2f, (float) Math.PI);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        host.sound(Synth.DOOR, 0.4f, 1.2f);
    }

    /** A frame at the crane: it on its way, what it brought heard; she in the cabin; the eye over the field. */
    private void crane(float dt) {
        Quay q = quay;
        boolean was = q.solved;
        int got = q.update(dt);
        if (got == 1) {
            host.sound(Synth.CLANG, 0.5f, 0.75f);
            host.buzz(25);
        } else if (got == 2) host.sound(Synth.CLANG, 0.3f, 1.3f);
        if (q.lastWaited == Quay.NO) {
            host.buzz(50);
            host.sound(Synth.TOCK, 0.5f, 0.6f);
        }
        q.lastWaited = Quay.NONE;
        if (q.solved && !was) {
            host.sound(Synth.CHIME, 0.6f, 1.1f);
            host.buzz(80);
            host.saveInt(SAVE_FIELDS, host.loadInt(SAVE_FIELDS) + 1);
        }
        q.seat(seatAt);
        Runner r = runner;
        r.lounging = false;
        r.ride(dt, city, seatAt[0], seatAt[1], seatAt[2], (float) Math.PI, 0f, seatAt[0], seatAt[1], seatAt[2], 0f);
        eyeNow[0] = Quay.CX;
        eyeNow[1] = City.WALK_TOP + CRANE_EYE_UP;
        eyeNow[2] = Quay.CZ + CRANE_LOOK_Z - CRANE_EYE_BACK;
        eyeNow[3] = 0f;
        eyeNow[4] = -(float) Math.atan2(CRANE_EYE_UP - 5f, CRANE_EYE_BACK);
        r.eyeAt(eyeNow);
    }

    /** Where on the frame the buttons at the crane sit: out (left), back a move (middle), a new field (right). */
    private int craneButtonY() {
        return h - 70;
    }

    /** The thumb at the crane: a stack touched; the pause; out; a move back; a new field once one is done. */
    private void craneTouch(int type, int id, float x, float y) {
        if (type != DOWN) return;
        if (paused) {
            if (!pauseRow(x, y)) paused = false;
            return;
        }
        if (onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            return;
        }
        int by = craneButtonY();
        if (onButton(x, y, 44, by)) {
            leaveCrane();
            return;
        }
        if (!quay.solved && quay.canUndo() && onButton(x, y, W / 2, by)) {
            quay.undo();
            host.sound(Synth.WOOD, 0.4f, 1.1f);
            host.buzz(15);
            return;
        }
        if (quay.solved && onButton(x, y, W - 44, by)) {
            // a new field brought in
            quay.deal(fieldColours());
            baysSeen = false;
            host.sound(Synth.WHOOSH, 0.5f, 0.8f);
            host.buzz(30);
            return;
        }
        if (!baysSeen) return;
        int best = -1;
        float bd = BAY_REACH * BAY_REACH;
        for (int b = 0; b < quay.bays; b++) {
            float dx = x - bayAt[b * 2], dy = y - bayAt[b * 2 + 1], dd = dx * dx + dy * dy;
            if (dd < bd) {
                bd = dd;
                best = b;
            }
        }
        if (best < 0) return;
        int did = quay.touch(best);
        if (did == Quay.NO) {
            host.buzz(50);
            host.sound(Synth.TOCK, 0.5f, 0.6f);
        } else if (did != Quay.NONE) host.buzz(12);
    }

    /** At the crane: the pause; out; a move back, while one can be; a new field, once this one is done. */
    private void craneButtons(DrawList d) {
        float by = craneButtonY();
        d.badge(44f, by, BUTTON + 4, Badges.DOWN, 0.95f, 0f);
        if (!quay.solved && quay.canUndo()) d.badge(W * 0.5f, by, BUTTON + 2, Badges.LEFT, 0.85f, 0f);
        if (quay.solved) d.badge(W - 44f, by, BUTTON + 4, Badges.PICK_UP, 0.95f, 0f);
    }

    /** Over the frame at the crane: where each stack is on it (for the thumb); the moves made; done, the word. */
    private void craneFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        float[] b3 = new float[3], at2 = new float[2];
        baysSeen = true;
        for (int b = 0; b < quay.bays; b++) {
            quay.bay(b, at2);
            float y = City.WALK_TOP + Math.max(1, quay.high[b]) * Quay.HIGH_EACH * 0.5f;
            if (d.project(at2[0], y, at2[1], b3)) {
                bayAt[b * 2] = b3[0];
                bayAt[b * 2 + 1] = b3[1];
            } else baysSeen = false;
        }
        String m = String.valueOf(quay.moves);
        shadowed(f, m, W - PixelFont.width(m, 2) - 8, 14, 2, cream, ink);
        if (quay.solved) wordAtTop(f, Lang.lines("crane.done", 0, null)[0], green, ink);
    }

    /** Over the frame, the port the first time: the clock; on the way, the word and the mark for the crane. */
    private void portFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        Port pt = port;
        if (craning) return;
        String clock = clock(pt.over() ? pt.doneAt : pt.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (!pt.goal(goal)) return;
        wordAtTop(f, Lang.lines("goal.crane", mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], Marks.CRANE, Marks.OPEN);
    }

    /** For the desktop stand: the crane and its field, told. */
    String standCrane() {
        Quay q = quay;
        StringBuilder sb = new StringBuilder(String.format("crane at %.1f %.1f %.1f, %s; %d colours, %d moves%s; held %d:", q.hx, q.hy, q.hz,
                craning ? "she works it" : "no one in it", q.colors, q.moves, q.solved ? ", done" : "", q.held));
        for (int b = 0; b < q.bays; b++) {
            sb.append(' ');
            for (int k = 0; k < q.high[b]; k++) sb.append((char) ('a' + q.stack[b][k]));
            if (q.high[b] == 0) sb.append('-');
        }
        return sb.toString();
    }

    /** For the desktop stand: bay b touched at the crane (its place on the frame as last drawn); or a button (out, back, load). */
    void standBay(String what) {
        float by = craneButtonY();
        float x, y;
        if (what.equals("out")) {
            x = 44f;
            y = by;
        } else if (what.equals("back")) {
            x = W * 0.5f;
            y = by;
        } else if (what.equals("load")) {
            x = W - 44f;
            y = by;
        } else {
            int b = Integer.parseInt(what);
            x = bayAt[b * 2];
            y = bayAt[b * 2 + 1];
        }
        touch(DOWN, 0, x, y);
        touch(UP, 0, x, y);
    }

    /** For the desktop stand: the field solved but for its last move, as if played so far. */
    void standNearlySolved() {
        quay.standNearly();
    }

    private float porticoWait;

    /**
     * Whether she goes in at the concert hall's doors now: on its portico anywhere, its steps or its
     * floor, walking toward the doors, or standing there a moment; through any of the three.
     */
    private boolean concertDoor(float dt) {
        Runner r = runner;
        if (r.inAir() || r.climbing() || !City.onPortico(r.x, r.z)) {
            porticoWait = 0f;
            return false;
        }
        boolean toward = r.speed > 0.3f && (float) Math.cos(r.heading) > 0.3f;
        porticoWait = r.speed < 1f ? porticoWait + dt : 0f;
        if (!toward && porticoWait < LIFT_STAND) return false;
        porticoWait = 0f;
        return true;
    }

    /** A stick's answer along one way: -1 to 1, nothing near its middle. */
    private static float stickAxis(float t, float c) {
        float v = Math.max(-1f, Math.min(1f, (t - c) / HELI_R)), a = Math.abs(v);
        return a < HELI_DEAD ? 0f : Math.signum(v) * (a - HELI_DEAD) / (1f - HELI_DEAD);
    }

    /** A frame at the controls: the sticks to the helicopter, she in the pilot's seat, the eye behind. */
    private void fly(float dt) {
        Copter cp = heli();
        float turn = leftStick >= 0 ? stickAxis(lTX, lCX) : 0f, lift = leftStick >= 0 ? -stickAxis(lTY, lCY) : 0f;
        float aside = rightStick >= 0 ? stickAxis(rTX, rCX) : 0f, ahead = rightStick >= 0 ? -stickAxis(rTY, rCY) : 0f;
        cp.pilot(dt, lift, turn, ahead, aside, city);
        cp.seat(true, seatAt);
        Runner r = runner;
        r.lounging = false;
        r.ride(dt, city, seatAt[0], seatAt[1], seatAt[2], cp.heading, 0f, seatAt[0], seatAt[1], seatAt[2], 0f);
        copterEye(cp, dt, true);
    }

    /** The thumbs at the controls: the left half of the glass the left stick, the right half the right; the pause; out, once down. */
    private void pilotTouch(int type, int id, float x, float y) {
        if (type == DOWN && paused) {
            if (!pauseRow(x, y)) paused = false;
            return;
        }
        if (type == DOWN && onButton(x, y, pauseX(), pauseY())) {
            paused = true;
            letGo();
            return;
        }
        if (type == DOWN && flying && heli().grounded && onLiftButton(x, y)) {
            leaveCopter();
            return;
        }
        float m = HELI_R + 4f;
        if (type == DOWN) {
            if (x < W * 0.5f && leftStick < 0) {
                leftStick = id;
                lCX = Math.max(m, Math.min(W * 0.5f - HELI_R * 0.5f, x));
                lCY = Math.max(h * 0.3f, Math.min(h - m, y));
                lTX = x;
                lTY = y;
            } else if (x >= W * 0.5f && rightStick < 0) {
                rightStick = id;
                rCX = Math.max(W * 0.5f + HELI_R * 0.5f, Math.min(W - m, x));
                rCY = Math.max(h * 0.3f, Math.min(h - m, y));
                rTX = x;
                rTY = y;
            }
        } else if (type == MOVE) {
            if (id == leftStick) {
                lTX = x;
                lTY = y;
            } else if (id == rightStick) {
                rTX = x;
                rTY = y;
            }
        } else if (type == UP) {
            if (id == leftStick) leftStick = -1;
            if (id == rightStick) rightStick = -1;
        }
    }

    /** The two sticks: under the thumbs while they are down, faint where they would go; out, once it is down. */
    private void sticks(DrawList d) {
        for (int k = 0; k < 2; k++) {
            int f = k == 0 ? leftStick : rightStick;
            if (f < 0) {
                d.badge(W * (k == 0 ? HELI_LEFT : HELI_RIGHT), h - HELI_UP, HELI_R + 6f, Badges.BLANK, 0.3f, 0f);
                continue;
            }
            float cx = k == 0 ? lCX : rCX, cy = k == 0 ? lCY : rCY;
            float ox = (k == 0 ? lTX : rTX) - cx, oy = (k == 0 ? lTY : rTY) - cy, m = (float) Math.sqrt(ox * ox + oy * oy);
            if (m > HELI_R) {
                ox *= HELI_R / m;
                oy *= HELI_R / m;
            }
            d.badge(cx, cy, HELI_R + 6f, Badges.BLANK, 0.55f, 0f);
            d.badge(cx + ox, cy + oy, KNOB, Badges.KNOB, 0.9f, 0f);
        }
        if (flying && heli().grounded) d.badge(liftButtonX(), liftButtonY(), BUTTON + 4, Badges.EXIT, 0.95f, 0f);
    }

    /** For the desktop stand: the helicopter she flies put at (x, y, z) in the air, facing so, still. */
    void standCopterAt(float x, float y, float z, float heading) {
        Copter c = heli();
        if (c == null) return;
        c.x = x;
        c.y = y;
        c.z = z;
        c.heading = heading;
        c.vx = c.vz = c.vy = 0f;
        c.grounded = false;
        flyYaw = Float.NaN;
    }

    /** For the desktop stand: out of the pickup, or of her own helicopter once it is down. */
    void standGetOut() {
        if (driving != null) getOut();
        else if (flying && parked.grounded) leaveCopter();
    }

    /** For the desktop stand: the helicopter, told. */
    String standCopter() {
        Copter c = heli();
        if (c == null) return "no helicopter";
        return String.format("helicopter at %.1f %.1f %.1f heading %.2f, %s%s%s, going %.1f %.1f up %.1f", c.x, c.y, c.z, c.heading,
                c.broken ? "broken" : "whole", c.grounded ? ", down" : ", in the air", piloting() ? ", she flies it" : "", c.vx, c.vz, c.vy)
                + (mend != null ? ", mend step " + mend.step : "") + String.format(", door %.1f %.1f, wait %.2f%s%s%s", copterDoor[0], copterDoor[1], copterWait,
                copterArmed ? ", armed" : "", runner.inAir() ? ", her in the air" : "", runner.climbing() ? ", climbing" : "") + ", swap " + swapT;
    }

    /** Over the frame, the mend: the clock in the corner; where the way goes now, in a word and a mark. */
    private void mendFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        Mend m = mend;
        String clock = clock(m.over() ? m.doneAt : m.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        String w = m.goalWord(driving);
        if (w == null || !m.goal(duty, driving, runner, goal)) return;
        wordAtTop(f, Lang.lines(w, mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], m.mark(driving), Marks.OPEN);
    }

    // ---- her own chair in the concert hall: come up to it with the lights on, she sits ----
    /** The hall she is in, on the job or come back to (null: neither). */
    private Concert hall() {
        return concert != null ? concert : stayed;
    }

    /** Whether she sits in her chair in the hall. */
    boolean inChair() {
        Concert co = hall();
        return co != null && co.seated && indoors != null && indoors.concert;
    }

    /** Seated, a frame of it (true); or come up to it, down she sits (true); or not near it (false). */
    private boolean chair(float dt) {
        Concert co = hall();
        Runner r = runner;
        if (co == null) return false;
        if (!co.seated) {
            if (!co.atChair(indoors, r) || r.led() || r.throwing() >= 0f) return false;
            co.sit();
            errand = NO_ERRAND;
            r.stopLead();
            letGo();
            host.sound(Synth.WOOD, 0.5f, 0.6f);
            host.buzz(40);
        }
        r.lounging = true;
        co.chair(indoors, chairAt);
        r.ride(dt, indoors, chairAt[0], chairAt[1], chairAt[2], indoors.heading(0f, 1f), 0f, chairAt[0], chairAt[1] - 0.03f, chairAt[2], 0f);
        co.shot(indoors, eyeNow);
        r.eyeAt(eyeNow);
        return true;
    }

    /** Up from her chair, behind it, facing the stage: the music plays on. */
    private void offChair() {
        Concert co = hall();
        if (co == null || !co.seated) return;
        co.rise();
        co.offChair(indoors, chairAt);
        runner.getOut(chairAt[0], Indoors.FLOOR, chairAt[1], indoors.heading(0f, 1f));
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        letGo();
    }

    /** In the dark after the concert: home, down on her sofa, the morning; the report when the dark has lifted. */
    private void wake() {
        if (driving != null) getOut();
        runner.getOut(runner.x, runner.y, runner.z, runner.heading);
        if (indoors != null) leave();
        swapT = -1f;
        indoors = homeRoom();
        indoors.enter(runner, false);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        sitDown();
        // the screen comes on once the dark has lifted and she has sat a moment
        seatedFor = -DAWN;
        dawn = DAWN;
        morning = (kindsWon & (1 << 8)) == 0;
    }

    /** The panel on its own, from home's screen: new wiring each time, as on a call out. */
    private void openSolo() {
        int panels = host.loadInt(SAVE_PANELS) + 1;
        host.saveInt(SAVE_PANELS, panels);
        solo = new Panel(Concert.GRID, panels * 0x9E3779B97F4A7C15L + 0x50105EEDL);
        soloClock = 0f;
        letGo();
        host.sound(Synth.LATCH, 0.5f, 1.3f);
        host.buzz(20);
    }

    /**
     * On the sofa, home's screen: the next job (the sign of the list over it), or the list, if no
     * story is left to come; first, if given, these lines (a report), the choice after them.
     */
    private void sofaChoice(String[] first) {
        int[] picks = nextChoice("sofa.next", "sofa.list");
        String[] lines = keptLines;
        if (first != null) {
            dispatch = new Dispatch(host, first).then(lines, picks);
            if (!listing) dispatch.listedNext();
        } else {
            dispatch = new Dispatch(host, lines).choices(picks);
            if (!listing) dispatch.listed();
        }
        letGo();
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
        saveLikes();
        host.sound(Synth.WOOD, 0.5f, 0.7f);
        host.buzz(40);
    }

    // ---- the jobs she knows: the next one to come, and what she can choose ----
    static final String SAVE_KINDS = "kinds";
    /** The kinds of mission won at least once, a bit each (1 << kind). */
    private int kindsWon;
    /** A choice's lines, as the last one was made. */
    private String[] keptLines;
    /** The last choice made is the list (no next job over it). */
    private boolean listing;
    /** The kinds that are games, played again and again; the rest are stories, played once, in their order. */
    static boolean game(int kind) {
        return kind == 1 || kind == 2 || kind == 10;
    }

    /** The next job to come: the first kind, in order, not yet done (0: none left). */
    int nextKind() {
        for (int k = 1; k <= KINDS; k++) if (open(k)) return k;
        return 0;
    }

    /** Whether a kind is still to be played in its turn: neither won nor put by; the landing gear only after the archive. */
    private boolean open(int k) {
        if (passed(k)) return false;
        // the landing gear only after the archive; the light gone wrong only after the tow
        if (k == 9) return (kindsWon & (1 << 8)) != 0;
        return k != 14 || (kindsWon & (1 << 6)) != 0;
    }

    /** Whether a kind is behind her: won, or put by to be played when she likes. */
    boolean passed(int k) {
        return ((kindsWon | kindsSkipped) & (1 << k)) != 0;
    }

    // ---- the points of a job: where it has come to that need not be played again. Each job says
    // which it has come to; on a new one the game keeps it (the job, the point, its clock, where to
    // stand, a number of the job's own), and the disk shows a moment by the pause. A game closed in
    // the job opens there; under the pause the job can be taken up there again ----
    static final String SAVE_POINT = "point", SAVE_POINT_ROUND = "point.round", SAVE_POINT_CLOCK = "point.clock",
            SAVE_POINT_X = "point.x", SAVE_POINT_Y = "point.y", SAVE_POINT_Z = "point.z", SAVE_POINT_H = "point.h",
            SAVE_POINT_DATA = "point.data";
    /** How long the disk shows by the pause once a point is kept, seconds; how far right of the pause. */
    static final float POINT_SHOWN = 1.8f, POINT_RIGHT = 36f;
    /** The point of the job now kept last (0: none yet), and when. */
    private int pointSeen;
    private float pointKeptAt = -9f;
    private final float[] pointPlace = new float[3];

    /** The point the job now has come to (0: none); each job with points says its own. */
    private int pointNow() {
        if (free || over()) return 0;
        if (folder != null) return folder.point();
        return 0;
    }

    /** Where the job is taken up again from at its point now, into out (x, z, heading): the job's own place, or where she stands. */
    private void pointWhere(float[] out) {
        if (folder != null) {
            folder.pointAt(out);
            return;
        }
        out[0] = runner.x;
        out[1] = runner.z;
        out[2] = runner.heading;
    }

    /** The job's clock at its point now: what its time is taken up again from. */
    private float pointClock() {
        if (folder != null) return folder.doneAt;
        return 0f;
    }

    /** A number of the job's own, kept with its point: whatever else it needs to be taken up again. */
    private int pointData() {
        return 0;
    }

    /** A new point come to: kept. */
    private void points() {
        int p = pointNow();
        if (p <= pointSeen) return;
        pointSeen = p;
        pointKeptAt = time;
        pointWhere(pointPlace);
        host.saveInt(SAVE_POINT, p);
        host.saveInt(SAVE_POINT_ROUND, mission.round);
        host.saveInt(SAVE_MISSION, mission.round);
        host.saveInt(SAVE_POINT_CLOCK, Float.floatToIntBits(pointClock()));
        host.saveInt(SAVE_POINT_X, Float.floatToIntBits(pointPlace[0]));
        host.saveInt(SAVE_POINT_Y, Float.floatToIntBits(City.WALK_TOP));
        host.saveInt(SAVE_POINT_Z, Float.floatToIntBits(pointPlace[1]));
        host.saveInt(SAVE_POINT_H, Float.floatToIntBits(pointPlace[2]));
        host.saveInt(SAVE_POINT_DATA, pointData());
    }

    /** No point kept for the job now: it begun, done with or put by. */
    private void forgetPoint() {
        pointSeen = 0;
        pointKeptAt = -9f;
        if (host.loadInt(SAVE_POINT) != 0) host.saveInt(SAVE_POINT, 0);
    }

    /** Whether the job now (or, between jobs, the one to play next) has a point kept. */
    boolean pointKept() {
        int n = free ? mission.round + 1 : mission.round;
        return host.loadInt(SAVE_POINT) > 0 && host.loadInt(SAVE_POINT_ROUND) == n;
    }

    /** The game opened again in a job with a point kept: taken up there; false if it has none. */
    private boolean fromKept() {
        if (!pointKept()) return false;
        fromPoint();
        return true;
    }

    /** The job now (or the one to play next) begun again at its point kept: she where it is, its clock as it was. */
    private void fromPoint() {
        int n = free ? mission.round + 1 : mission.round;
        int p = host.loadInt(SAVE_POINT);
        float clock = Float.intBitsToFloat(host.loadInt(SAVE_POINT_CLOCK));
        float x = Float.intBitsToFloat(host.loadInt(SAVE_POINT_X)), y = Float.intBitsToFloat(host.loadInt(SAVE_POINT_Y));
        float z = Float.intBitsToFloat(host.loadInt(SAVE_POINT_Z)), hd = Float.intBitsToFloat(host.loadInt(SAVE_POINT_H));
        if (!sane(x) || !sane(y) || !sane(z) || !sane(hd) || !sane(clock)) {
            forgetPoint();
            return;
        }
        paused = false;
        if (driving != null) getOut();
        if (indoors != null) leave();
        errand = NO_ERRAND;
        runner.stopLead();
        letGo();
        for (Pickup q : duty.all) q.load = 0;
        mission.round = n - 1;
        again = false;
        begin(x, z);
        overFor = 0f;
        caughtSeen = 0;
        nextSlot = 0;
        struckSeen = 0;
        host.saveInt(SAVE_MISSION, n);
        boolean taken = folder != null && folder.fromPoint(p, clock);
        if (!taken) return;
        runner.put(x, Math.max(y, city.ground(x, z, y + 0.5f, 0f)), z, hd);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        // kept still: shown again, as it was when it was come to
        pointSeen = p;
        pointKeptAt = time;
        host.saveInt(SAVE_POINT, p);
        host.saveInt(SAVE_POINT_ROUND, n);
    }

    /** The disk by the pause a moment, a point just kept. */
    private void pointBadge(DrawList d) {
        float t = time - pointKeptAt;
        if (t < 0f || t > POINT_SHOWN) return;
        float shown = Math.min(1f, Math.min(t / 0.2f, (POINT_SHOWN - t) / 0.5f));
        d.badge(pauseX() + POINT_RIGHT, pauseY(), PAUSE_HALF, Badges.SAVE, 0.85f * shown, 0.5f + 0.5f * (float) Math.sin(t * 9f));
    }

    // ---- where she was: kept now and then, so that a game the system closed opens there again.
    // Between jobs on foot or at the wheel, just there; in a job, indoors, in the air or at the
    // crane, at home on the sofa, the job's screen to come (a job is not taken up halfway) ----
    static final String SAVE_WHERE = "where", SAVE_WX = "where.x", SAVE_WY = "where.y", SAVE_WZ = "where.z", SAVE_WH = "where.h";
    static final int WHERE_NONE = 0, WHERE_FOOT = 1, WHERE_WHEEL = 2, WHERE_SOFA = 3;
    /** How often it is kept, seconds. */
    static final float WHERE_KEPT_EVERY = 2f;
    private float whereKeptAgo;

    private void keepWhere() {
        whereKeptAgo = 0f;
        if (!passed(1)) return;
        int how = !free || indoors != null || aboard() || piloting() || craning ? WHERE_SOFA : driving != null ? WHERE_WHEEL : WHERE_FOOT;
        host.saveInt(SAVE_WHERE, how);
        if (how == WHERE_SOFA) return;
        boolean wheel = driving != null;
        host.saveInt(SAVE_WX, Float.floatToIntBits(wheel ? driving.x : runner.x));
        host.saveInt(SAVE_WY, Float.floatToIntBits(wheel ? driving.y : runner.y));
        host.saveInt(SAVE_WZ, Float.floatToIntBits(wheel ? driving.z : runner.z));
        host.saveInt(SAVE_WH, Float.floatToIntBits(wheel ? driving.heading : runner.heading));
    }

    /** The game opened again: where she was kept, if she was anywhere but at home's door. */
    private void resume() {
        int how = host.loadInt(SAVE_WHERE);
        if (how == WHERE_SOFA && fromKept()) return;
        if (how == WHERE_SOFA) {
            standSofa();
            return;
        }
        float x = Float.intBitsToFloat(host.loadInt(SAVE_WX)), y = Float.intBitsToFloat(host.loadInt(SAVE_WY));
        float z = Float.intBitsToFloat(host.loadInt(SAVE_WZ)), hd = Float.intBitsToFloat(host.loadInt(SAVE_WH));
        if (!sane(x) || !sane(y) || !sane(z) || !sane(hd)) return;
        if (how == WHERE_FOOT) {
            runner.put(x, Math.max(y, city.ground(x, z, y + 0.5f, 0f)), z, hd);
            runner.recenter();
        } else if (how == WHERE_WHEEL) standDrive(x, z, hd);
    }

    private static boolean sane(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v) && Math.abs(v) < 20000f;
    }

    // ---- a ride on a car's roof: the car carries her along and round with it, and the stream
    // gives its likes all the while ----
    /** Likes a second riding, and hearts a second drifting up; how long a ride is before they come. */
    static final float RIDE_LIKES = 30f, RIDE_HEARTS = 2.5f, RIDE_FROM = 1f;
    private float rideFor, rideOwed;

    /** The car she rode last step, or -1. */
    private int rodeLast = -1;

    /**
     * The car that carries her now, on foot over its roof: she stands on it, or comes down onto
     * it, or has leapt up off it (and goes on with it, as she would, till she lands); -1 for none.
     */
    private int riding() {
        int c = driving != null || indoors != null || aboard() || piloting() || craning ? -1 : cars.over(runner.x, runner.z);
        if (c >= 0) {
            float above = runner.y - cars.roofOf(c);
            boolean air = runner.state == Runner.AIR;
            boolean on = !air && runner.state == Runner.RUN && Math.abs(above) < Cars.RIDE_SLACK;
            boolean landing = air && runner.vy <= 0f && above > -Cars.RIDE_SLACK && above < 0.6f;
            boolean leapt = air && c == rodeLast && above > -Cars.RIDE_SLACK;
            if (!on && !landing && !leapt) c = -1;
        }
        rodeLast = c;
        return c;
    }

    /** The car c moved from (x0, z0) turned h0: she goes with it, where she stood on it, turned as it turned. */
    private void carried(int c, float x0, float z0, float h0, float dt) {
        float dh = cars.heading[c] - h0, cs = (float) Math.cos(dh), sn = (float) Math.sin(dh);
        float ox = runner.x - x0, oz = runner.z - z0;
        // turned about the up axis as its heading is (0 along +z, growing toward +x)
        float nx = ox * cs + oz * sn, nz = -ox * sn + oz * cs;
        runner.x = cars.x[c] + nx;
        runner.z = cars.z[c] + nz;
        runner.heading += dh;
        runner.eyeYaw += dh;
        float moved = Math.abs(cars.speed[c]);
        if (moved < 0.5f) return;
        rideFor += dt;
        if (rideFor < RIDE_FROM) return;
        hearts.drift(RIDE_HEARTS);
        rideOwed += RIDE_LIKES * dt;
        int n = (int) rideOwed;
        if (n > 0) {
            rideOwed -= n;
            like(n, 0);
        }
    }

    // ---- the hall no one knows of: found the first time, likes as never ----
    static final String SAVE_SECRET = "secret";
    static final int SECRET_LIKES = 100000, SECRET_HEARTS = 160;

    // ---- likes: what a job brings, a moment worth it, music listened to, a long flight in the town ----
    /** Hearts poured for a job won, and again over its report read at home. */
    static final int WIN_HEARTS = 70, REPORT_HEARTS = 90;
    /** A plate struck (or a rival's): so many likes, so many hearts. */
    static final int STRUCK_LIKES = 1000, STRUCK_HEARTS = 14;
    /** The road through the pass open. */
    static final int OPEN_LIKES = 5000, OPEN_HEARTS = 40;
    /** Music in the hall, listened to: likes a second, hearts a second drifting up. */
    static final float LISTEN_LIKES = 40f, LISTEN_HEARTS = 3f;
    /**
     * In the town, on foot: a flight at least so long (a leap off a roof, a long glide) brings
     * likes, more the longer, up to so many; hearts with them. Nothing for what she knocks down.
     */
    static final float STUNT_AIR = 1.8f, STUNT_LIKES = 150f;
    static final int STUNT_MOST = 5000, STUNT_HEARTS_MOST = 30;
    /** Likes come in a little at a time are kept at most so often, seconds. */
    static final float LIKES_KEPT_EVERY = 5f;
    /** What the job just won brought, for its report read at home. */
    private int waitingLikes;
    private boolean likesDirty;
    private float likesKeptAgo, listenOwed, airFor;

    /** Likes come in: so many more, and so many hearts poured up the side for them. */
    void like(int n, int pour) {
        if (n <= 0 && pour <= 0) return;
        likes += Math.max(0, n);
        hearts.add(Math.max(0, n), pour);
        if (n > 0) likesDirty = true;
    }

    private void saveLikes() {
        host.saveInt(SAVE_LIKES, likes);
        likesDirty = false;
        likesKeptAgo = 0f;
    }

    /** In the concert hall while the music plays: likes a little at a time, a few hearts drifting up. */
    private void listened(float dt) {
        boolean music = indoors != null && indoors.concert
                && (concert != null && concert.playing() || stayed != null && stayed.playing());
        if (!music) {
            listenOwed = 0f;
            return;
        }
        hearts.drift(LISTEN_HEARTS);
        listenOwed += LISTEN_LIKES * dt;
        int n = (int) listenOwed;
        if (n > 0) {
            listenOwed -= n;
            like(n, 0);
        }
    }

    /** In the town on foot, between jobs: a long flight landed (on ground, a roof or in the sea) brings likes. */
    private void stunts(float dt) {
        if (!free || driving != null || piloting() || craning || indoors != null) {
            airFor = 0f;
            return;
        }
        if (runner.state == Runner.AIR) {
            airFor += dt;
            return;
        }
        float t = airFor;
        airFor = 0f;
        if (t < STUNT_AIR) return;
        int n = Math.min(STUNT_MOST, Math.round(STUNT_LIKES * t * t));
        like(n, Math.min(STUNT_HEARTS_MOST, 4 + Math.round(4f * t)));
    }

    // ---- a job put by: any job not won, at any time but in the air; on home's list, not done ----
    static final String SAVE_SKIPPED = "skipped";
    /** The kinds put by, not done, a bit each (1 << kind); one won later is taken off. */
    private int kindsSkipped;

    /** Whether the job now may be put by: any not won before, from its first moment; not in the air. */
    boolean canSkip() {
        if (free || aboard() || piloting()) return false;
        int k = kindOf(mission.round);
        return ((kindsWon & (1 << k)) == 0 || !game(k)) && !over();
    }

    /** Whether the job now is a story won before, played again from home's list. */
    boolean replaying() {
        int k = kindOf(mission.round);
        return !free && !game(k) && (kindsWon & (1 << k)) != 0;
    }

    /** The helicopter she had before a story played again, kept aside while it lasts (null: none). */
    private Copter aside;

    /** The helicopter put aside given back (unless the one just flown home is to stay hers). */
    private void putBackAside(boolean keepNew) {
        if (aside == null) return;
        if (!keepNew) parked = aside;
        aside = null;
    }

    /** The island as the jobs won leave it, once a story played again is over. */
    private void settleIsland() {
        boolean s = shutByJobs();
        if (s == Island.shut) return;
        if (s) shutIsland();
        else openIsland();
    }

    /** The job now put by: on home's list as not done, no pay; the town hers where she is. */
    private void skip() {
        int k = kindOf(mission.round);
        paused = false;
        if (craning) {
            craning = false;
            craneSolo = false;
            quay.putBack();
            runner.getOut(Quay.TOWER_X - 1.2f, City.WALK_TOP, Quay.TOWER_Z - 4.2f, (float) Math.PI);
        }
        if (solo != null) solo = null;
        if (indoors != null && !indoors.home) {
            swapTo = TO_STREET;
            swap();
            swapT = -1f;
        }
        // a story played again and left: nothing put by, the world as it was
        boolean replay = replaying();
        if (!replay) {
            kindsSkipped |= 1 << k;
            host.saveInt(SAVE_SKIPPED, kindsSkipped);
        }
        host.saveInt(SAVE_MISSION, mission.round + 1);
        forgetPoint();
        String[] said = Lang.lines(replay ? "report.leave" : "report.skip", mission.round, null);
        lost = false;
        free = true;
        folder = null;
        delivery = null;
        rescue = null;
        puppy = null;
        crowd = null;
        tow = null;
        if (archive != null && archive.copter != null && parked == null) parked = archive.copter;
        archive = null;
        mend = null;
        port = null;
        lab = null;
        herd = null;
        cherry = null;
        putBackAside(false);
        settleIsland();
        saveCopter();
        dropFault();
        if (concert != null) concert.stop();
        concert = null;
        city.extra = null;
        chase = nobody;
        errand = NO_ERRAND;
        runner.stopLead();
        runner.held = false;
        overFor = 0f;
        retryAfter = false;
        letGo();
        dispatch = new Dispatch(host, said);
        host.buzz(30);
    }

    /**
     * Home's screen: under its head (head, from Lang), the next job and nothing else; or, if none
     * is left to come, the list (under listHead). Its lines are left in keptLines; what each picks
     * is given back.
     */
    /** Home's head with what has been won in all under its first line, once there is anything. */
    private String[] balanced(String[] h) {
        if (likes <= 0 || h.length == 0) return h;
        String sum = Lang.lines("home.sum", 0, null)[0].replace("{sum}", grouped(likes));
        // a head of one line (what the list is): the sum over it
        if (h.length == 1) return new String[] {sum, h[0]};
        boolean gap = h.length > 1 && h[1].isEmpty();
        String[] out = new String[h.length + (gap ? 2 : 1)];
        int at = gap ? 2 : 1;
        System.arraycopy(h, 0, out, 0, at);
        out[at] = sum;
        if (gap) out[at + 1] = "";
        System.arraycopy(h, at, out, at + (gap ? 2 : 1), h.length - at);
        return out;
    }

    private int[] nextChoice(String head, String listHead) {
        int k = nextKind();
        if (k == 0) return listChoice(listHead);
        listing = false;
        String[] h = balanced(Lang.lines(head, 0, null));
        keptLines = new String[h.length + 2];
        System.arraycopy(h, 0, keptLines, 0, h.length);
        keptLines[h.length] = "";
        keptLines[h.length + 1] = Lang.lines("kind." + k, 0, null)[0];
        int[] picks = new int[keptLines.length];
        java.util.Arrays.fill(picks, -1);
        picks[h.length + 1] = k;
        return picks;
    }

    /**
     * The list, under its head: only what is hers to choose, the newest at the top and the first
     * at the bottom: the next story, if one is left; the panel on its own, once the concert hall
     * is done; the games she has played (the folder, the hunt). Nothing of what is still to come
     * after the next.
     */
    private int[] listChoice(String head) {
        listing = true;
        String[] h = balanced(Lang.lines(head, 0, null));
        java.util.ArrayList<String> l = new java.util.ArrayList<String>(java.util.Arrays.asList(h));
        java.util.ArrayList<Integer> p = new java.util.ArrayList<Integer>();
        for (int k = 0; k < h.length; k++) p.add(-1);
        l.add("");
        p.add(-1);
        int next = nextKind();
        if (next > 0 && !game(next)) {
            l.add(Lang.lines("kind." + next, 0, null)[0]);
            p.add(next);
        }
        // the stories put by, not done: to be played when she likes
        for (int k = KINDS; k >= 1; k--) {
            if (game(k) || (kindsSkipped & (1 << k)) == 0 || k == 9 && (kindsWon & (1 << 8)) == 0) continue;
            l.add(Lang.lines("list.undone", 0, null)[0].replace("{job}", Lang.lines("kind." + k, 0, null)[0]));
            p.add(k);
        }
        // the stories won, each to be played again from its beginning: by name in the list while
        // the list has room for them all, the newest at the top; past that, on a screen of their own
        int won = 0, rest = (kindsWon & (1 << 7)) != 0 ? 1 : 0;
        for (int k = 1; k <= KINDS; k++) {
            if (!game(k) && (kindsWon & (1 << k)) != 0) won++;
            if (game(k) && ((kindsWon & (1 << k)) != 0 || k == next || (kindsSkipped & (1 << k)) != 0)) rest++;
        }
        if (won > 0 && p.size() - h.length - 1 + won + rest <= LIST_ROOM) {
            for (int k = KINDS; k >= 1; k--)
                if (!game(k) && (kindsWon & (1 << k)) != 0) {
                    l.add(Lang.lines("kind." + k, 0, null)[0]);
                    p.add(k);
                }
        } else if (won > 0) {
            l.add(Lang.lines("list.stories", 0, null)[0]);
            p.add(Dispatch.STORIES);
        }
        if ((kindsWon & (1 << 7)) != 0) {
            l.add(Lang.lines("kind.panel", 0, null)[0]);
            p.add(Dispatch.SOLO);
        }
        for (int k = KINDS; k >= 1; k--) {
            boolean by = (kindsSkipped & (1 << k)) != 0;
            if (!game(k) || (kindsWon & (1 << k)) == 0 && k != next && !by) continue;
            l.add(by ? Lang.lines("list.undone", 0, null)[0].replace("{job}", Lang.lines("kind." + k, 0, null)[0])
                    : Lang.lines("kind." + k, 0, null)[0]);
            // the containers once done: at the crane from here, no way to go
            p.add(k == 10 && (kindsWon & (1 << 10)) != 0 ? Dispatch.YARD : k);
        }
        keptLines = l.toArray(new String[0]);
        int[] out = new int[p.size()];
        for (int k = 0; k < out.length; k++) out[k] = p.get(k);
        return out;
    }

    /** How many jobs home's list shows by name before the stories won go onto a screen of their own. */
    static final int LIST_ROOM = 10;

    /** What each line of the stories done picks, as the screen was last made. */
    private int[] storyPicks;

    /**
     * The stories won, the newest at the top, each to be played again from its beginning (the world
     * left as it is after it); under them the way back to the list. Its picks left in storyPicks.
     */
    private String[] storiesChoice() {
        String[] h = Lang.lines("stories.head", 0, null);
        java.util.ArrayList<String> l = new java.util.ArrayList<String>(java.util.Arrays.asList(h));
        java.util.ArrayList<Integer> p = new java.util.ArrayList<Integer>();
        for (int k = 0; k < h.length; k++) p.add(-1);
        l.add("");
        p.add(-1);
        for (int k = KINDS; k >= 1; k--) {
            if (game(k) || (kindsWon & (1 << k)) == 0) continue;
            l.add(Lang.lines("kind." + k, 0, null)[0]);
            p.add(k);
        }
        l.add("");
        p.add(-1);
        l.add(Lang.lines("list.back", 0, null)[0]);
        p.add(Dispatch.TO_LIST);
        storyPicks = new int[p.size()];
        for (int k = 0; k < storyPicks.length; k++) storyPicks[k] = p.get(k);
        return l.toArray(new String[0]);
    }

    /** For the desktop stand: a touch on the sign of the list over home's choice; false if it is not shown. */
    boolean standList() {
        float[] at = new float[2];
        if (dispatch == null || !dispatch.listSignAt(at)) return false;
        touch(DOWN, 0, at[0], at[1]);
        touch(UP, 0, at[0], at[1]);
        return true;
    }

    // ---- the way home told after a job: once in a game ----
    static final String SAVE_TOLD = "told.home";
    private boolean told;

    /** The screen let go by the sign of the way back: she gets up once the walls have drawn back. */
    private boolean standAfter;

    /** Up from the sofa, a step before it, facing the room. */
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
        darken(f, swapT < SWAP ? swapT / SWAP : Math.max(0f, 1f - (swapT - SWAP) / SWAP));
    }

    /** The dark coming down at the end of the concert, and lifting at home in the morning. */
    private void nightFlat(Px f) {
        float dark = Math.max(concert != null ? concert.dusk() : 0f, dawn / DAWN);
        if (dark > 0f) darken(f, dark * dark * (3f - 2f * dark));
    }

    /** The frame gone so far into the dark, 0 to 1. */
    private static void darken(Px f, float dark) {
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
        // the rivals: a mark each, alight while it is about, and what it has still to go (till it is hers)
        for (int k = 0; k < Folder.RIVALS && !fo.hers; k++) {
            Chase g = fo.rivals[k];
            int y = 12 + k * 13;
            boolean about = !g.done();
            f.disc(14, y + 3, 4.5f, ink);
            f.disc(14, y + 3, 3.5f, about ? 0xFF000000 | Frida.VIOLET : 0xFF3A4660);
            if (!about) continue;
            String m = Math.round(fo.toGo(g)) + "";
            shadowed(f, m, 22, y, 1, g.y > 1f ? gold : cream, ink);
        }
        if (fo.hers) {
            // the clerk's words as she takes it; else where it goes now
            if (fo.handing()) {
                caption(f, "folder.hand");
                return;
            }
            if (fo.delivered() || !fo.goal(indoors, goal)) return;
            wordAtTop(f, Lang.lines(fo.goalWord(indoors), mission.round, null)[0], green, ink);
            placeMark(d, f, goal[0], goal[1], goal[2], fo.mark(indoors), Marks.OPEN);
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
    /** Whether the dispatcher has told how a pickup is driven: once in a game, the first time she is at a wheel. */
    static final String SAVE_TOLD_CAR = "told.car";

    /** A frame of the delivery: the dispatcher at the wheel the first time, near each gate, and once it is done. */
    private void deliveryStep(float dt) {
        int got = delivery.update(dt, duty, driving, runner, host);
        String say = null;
        if (got == Delivery.IN && host.loadInt(SAVE_TOLD_CAR) == 0) {
            host.saveInt(SAVE_TOLD_CAR, 1);
            say = "delivery.car";
        } else if (got == Delivery.AT_FACTORY) say = "delivery.factory";
        else if (got == Delivery.AT_STORE) say = "delivery.store";
        else if (got == Delivery.DELIVERED) say = "delivery.done";
        if (say == null) return;
        if (got == Delivery.DELIVERED) host.sound(Synth.CHIME, 0.6f, 1f);
        letGo();
        dispatch = new Dispatch(host, Lang.lines(say, mission.round, null));
    }

    private void deliveryFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        Delivery dv = delivery;
        String clock = clock(dv.over() ? dv.doneAt : dv.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        // the crates on the pickup she drives, in a row of little squares
        Pickup p = driving != null ? driving : dv.carrying;
        for (int k = 0; k < Pickup.CRATES; k++) {
            boolean on = p != null && k < p.load;
            f.rect(10 + k * 11, 12, 19 + k * 11, 21, ink);
            f.rect(11 + k * 11, 13, 18 + k * 11, 20, on ? 0xFFC79B5E : 0xFF3A4660);
        }
        if (dv.over()) {
            return;
        }
        if (!dv.goal(duty, driving, runner, goal)) return;
        float gx = goal[0] - runner.x, gz = goal[2] - runner.z;
        // where the way goes, in a word: no meters, the mark shows the way
        String w = Lang.lines(dv.goalWord(), mission.round, null)[0];
        wordAtTop(f, w, green, ink);
        boolean bay = dv.step == Delivery.FACTORY || dv.step == Delivery.STORE && (dv.carrying == null || dv.carrying == driving);
        if (bay && d.project(goal[0], City.WALK_TOP + 2.6f, goal[2], at) && at[0] > 0 && at[0] < W && at[1] > 30 && at[1] < h) {
            // the bay's sign: what is done there
            float near = (float) Math.sqrt(gx * gx + gz * gz);
            float shown = Math.min(1f, Math.max(0f, (60f - near) / 15f));
            d.badge(at[0], at[1], Math.max(8f, Math.min(16f, 300f / Math.max(1f, near))),
                    dv.step == Delivery.FACTORY ? Badges.PICK_UP : Badges.DOWN, shown, 0.5f + 0.5f * (float) Math.sin(time * 4f));
        }
        int pic = dv.step == Delivery.PICKUP ? Marks.TRUCK : dv.step == Delivery.FACTORY ? Marks.FACTORY
                : dv.step == Delivery.STORE ? (dv.carrying != null && dv.carrying != driving ? Marks.TRUCK : Marks.WAREHOUSE) : Marks.HOUSE;
        placeMark(d, f, goal[0], goal[1], goal[2], pic, Marks.OPEN);
    }

    /** Free: where home is (the next mission is taken there), quietly; the likes are at the hearts' foot. */
    private void freeFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2;
        if (indoors != null) return;
        float gx = City.HOME_DOOR_X - runner.x, gz = City.HOME_DOOR_Z - runner.z;
        float far = (float) Math.sqrt(gx * gx + gz * gz);
        if (far < 12f) return;
        // nothing urged on her: only after the first job won, its report waiting at home, is the
        // way home said at the top, once in a game; after that she knows it
        if (waiting != null && !told) {
            String w = Lang.lines("goal.done", mission.round, null)[0];
            shadowed(f, w, (W - PixelFont.width(w, 1)) / 2, 14, 1, cream, ink);
        }
        // at the door its own sign is there to touch: no mark over it as well
        if (far > 30f) placeMark(d, f, City.HOME_DOOR_X, City.WALK_TOP + 3.5f, City.HOME_DOOR_Z, Marks.HOUSE, Marks.OPEN);
    }

    /**
     * Over the frame, the puppy: the clock in the corner; where the way goes now, in a word and a
     * mark (the puppy, the vets'); over the vets' bay, its sign.
     */
    private void puppyFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E, fire = 0xFFF0581C;
        Puppy pu = puppy;
        String clock = clock(pu.over() ? pu.doneAt : pu.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (pu.over()) {
            return;
        }
        if (!pu.goal(goal)) return;
        float gx = goal[0] - runner.x, gz = goal[2] - runner.z;
        float near = (float) Math.sqrt(gx * gx + gz * gz);
        String w = Lang.lines(pu.goalWord(), mission.round, null)[0];
        wordAtTop(f, w, green, ink);
        if (pu.held() && d.project(goal[0], City.WALK_TOP + 2.6f, goal[2], at) && at[0] > 0 && at[0] < W && at[1] > 30 && at[1] < h) {
            // the bay's sign: what is done there
            float shown = Math.min(1f, Math.max(0f, (60f - near) / 15f));
            d.badge(at[0], at[1], Math.max(8f, Math.min(16f, 300f / Math.max(1f, near))), Badges.DOWN, shown, 0.5f + 0.5f * (float) Math.sin(time * 4f));
        }
        placeMark(d, f, goal[0], goal[1], goal[2], pu.held() ? Marks.CLINIC : Marks.PAW, Marks.OPEN);
    }

    /**
     * Over the frame, the concert hall: the clock and the pay in the corner; out of the hall, its name
     * and its mark; in it, in the dark, the way to the door to the wings.
     */
    private void concertFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        Concert co = concert;
        String clock = clock(co.doneAt >= 0f ? co.doneAt : co.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (co.step >= Concert.SEATED) {
            // the pay, big, a moment after she sits; then only the music
            return;
        }
        boolean in = indoors != null && indoors.concert;
        if (!in) {
            String w = Lang.lines("goal.concert", mission.round, null)[0];
            wordAtTop(f, w, green, ink);
            placeMark(d, f, City.concertDoorX(), City.WALK_TOP + 3.5f, City.concertDoorZ(), Marks.CONCERT, Marks.OPEN);
            return;
        }
        if (co.step == Concert.LIT) {
            // the lights on: to her chair at the front
            wordAtTop(f, Lang.lines("goal.chair", mission.round, null)[0], green, ink);
            co.chair(indoors, goal);
            goal[1] += 2.6f;
            placeMark(d, f, goal[0], goal[1], goal[2], Marks.CHAIR, Marks.OPEN);
            return;
        }
        if (co.step != Concert.INSIDE || co.panelOpen) return;
        // in the dark: to the door to the wings
        wordAtTop(f, Lang.lines("goal.wings", mission.round, null)[0], green, ink);
        co.wings(indoors, goal);
        placeMark(d, f, goal[0], goal[1], goal[2], Marks.PANEL, Marks.OPEN);
    }

    /** The panel over the frame, while it is open: nothing of the frame's own buttons shows through it. */
    private void panelFlat(DrawList d, Px f) {
        Wires wires = faultWires();
        if (wires != null) {
            wires.draw(f, Lang.lines("fault.plate", 0, null)[0], Lang.lines("fault.works", 0, null)[0]);
            d.badgeN = 0;
            d.tube[0] = d.tube[2] = 0f;
            return;
        }
        Panel p = solo != null ? solo : concert != null && concert.panelOpen ? concert.panel : null;
        if (p == null) return;
        p.draw(f, Lang.lines("panel.plate", 0, null)[0]);
        d.badgeN = 0;
        d.tube[0] = d.tube[2] = 0f;
    }

    /** Over the frame, the archive: the clock in the corner; where the way goes now, in a word and a mark. */
    private void archiveFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        Archive ar = archive;
        String clock = clock(ar.over() ? ar.doneAt : ar.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (ar.handing()) {
            // what the clerk says, handing it over (over the archive's counter, nothing is said)
            if (ar.step == Archive.HAND) caption(f, "archive.hand");
            return;
        }
        String w = ar.goalWord(indoors, runner);
        if (w == null || !ar.goal(indoors, runner, goal)) return;
        wordAtTop(f, Lang.lines(w, mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], ar.mark(indoors, runner), Marks.OPEN);
    }

    /**
     * The folder handed over in the secretariat: she stands still, turned to the clerk, the
     * finger not heard; the eye off to the side, low, on the two of them, from toward the hall's back.
     * First, a stop to whatever led her.
     */
    private void handOver(boolean already) {
        Runner r = runner;
        Indoors in = indoors;
        if (in == null) return;
        if (!already) {
            errand = NO_ERRAND;
            r.stopLead();
            letGo();
        }
        r.held = true;
        r.speed = 0f;
        float gx = in.wx(in.deskU, in.deskV), gz = in.wz(in.deskU, in.deskV);
        r.heading = (float) Math.atan2(gx - r.x, gz - r.z);
        // between them, and off to the front of the hall and back toward its middle
        float mu = (in.u(r.x, r.z) + in.deskU) * 0.5f, mv = (in.v(r.x, r.z) + in.deskV) * 0.5f;
        float eu = mu - Math.signum(in.deskU) * HAND_EYE_IN, ev = mv - HAND_EYE_SIDE;
        // over the archive's counter: from the side, a little toward the corridor
        if (in.archive) {
            eu = mu + HAND_EYE_IN * 1.5f;
            ev = mv - 1f;
        }
        handEye[0] = in.wx(eu, ev);
        handEye[1] = Indoors.FLOOR + HAND_EYE_UP;
        handEye[2] = in.wz(eu, ev);
        float tx = in.wx(mu, mv) - handEye[0], tz = in.wz(mu, mv) - handEye[2];
        handEye[3] = (float) Math.atan2(tx, tz);
        handEye[4] = (float) Math.atan2(Indoors.FLOOR + 1.6f - handEye[1], Math.sqrt(tx * tx + tz * tz));
        r.eyeAt(handEye);
    }

    // the eye on the handing over: so far toward the hall's middle from the two, so far toward its front (less than none: its back, on the side of the hand she takes it in), so high
    static final float HAND_EYE_IN = 2.2f, HAND_EYE_SIDE = -2.2f, HAND_EYE_UP = 2.4f;
    private final float[] handEye = new float[5], handTo = new float[3];

    /** For the desktop stand: an eye of its own (x, y, z, yaw, pitch) for the street; null, hers. */
    float[] standEye;
    /** For the desktop stand: her alone for the icon, an eye (x, y, z, yaw, pitch, focal) and how far into her throw; or none. */
    float[] standPortrait;

    /** Words said, low on the frame, each line over a dark band. */
    private void caption(Px f, String key) {
        caption(f, key, 0.66f);
    }

    /** The same, so far down the frame. */
    private void caption(Px f, String key, float down) {
        caption(f, Lang.lines(key, mission.round, null), down);
    }

    /** Lines said over the frame, so far down it, each on a dark band. */
    private void caption(Px f, String[] say, float down) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2;
        int y = (int) (h * down);
        for (int k = 0; k < say.length; k++) {
            int wd = PixelFont.width(say[k], 1);
            for (int py = y - 3; py < y + 10; py++)
                for (int px = (W - wd) / 2 - 5; px < (W + wd) / 2 + 5; px++) f.set(px, py, 0xB0141A24);
            shadowed(f, say[k], (W - wd) / 2, y, 1, cream, ink);
            y += 13;
        }
    }

    // ---- the hot brums led off (see Herd) ----
    private float herdTick, herdUntil;
    private String[] herdSay;
    private final float[] herdAgain = new float[4];

    /**
     * A step of it: the knots that see her come after her (the first and the last told on the
     * screen, the rest over the frame); one that comes up to her goes off, and it is tried again
     * from a little before the last knot came; near them, their ticking; the town runs from them.
     */
    private void herdStep(float dt) {
        Herd hd = herd;
        // up on the gallery, the swap over: the dispatcher says what now
        if (towerSay && swapT < 0f) {
            towerSay = false;
            host.sound(Synth.CHIME, 0.6f, 1f);
            dispatch = new Dispatch(host, Lang.lines("herd.tower", mission.round, null));
            return;
        }
        // in the secret hall, at the switch: thrown, the bridge goes up; done
        if (hd.step == Herd.LEVER && indoors != null && indoors.secret && leverAt < 0f && Rooms.atLever(indoors, runner.x, runner.z)) {
            leverAt = 0f;
            host.sound(Synth.CLANG, 0.6f, 0.7f);
            host.buzz(80);
        }
        if (leverAt >= 0f && hd.step == Herd.LEVER) {
            leverAt += dt;
            if (leverAt >= LEVER_S) {
                hd.thrown();
                shutIsland();
                host.sound(Synth.CHIME, 0.6f, 1f);
                host.buzz(60);
                return;
            }
        }
        float x = driving != null ? driving.x : runner.x, y = driving != null ? driving.y : runner.y, z = driving != null ? driving.z : runner.z;
        int got = hd.update(dt, x, y, z, indoors == null, city);
        wrecked(hd.wrecked() || Island.shut);
        if (got == Herd.PIER) {
            host.sound(Synth.CHIME, 0.6f, 1f);
            host.buzz(60);
            dispatch = new Dispatch(host, Lang.lines("herd.yacht", mission.round, null));
            return;
        } else if (got == Herd.BOOM) {
            host.sound(Synth.THUNDER, 1f, 0.9f);
            host.sound(Synth.CRASH, 0.9f, 0.6f);
            host.sound(Synth.SPLASH, 0.7f, 0.7f);
            host.buzz(320);
            whiteOut = WHITE_OUT;
            shake = 1f;
        } else if (got == Herd.WRECKED) {
            dispatch = new Dispatch(host, Lang.lines("herd.wreck", mission.round, null));
            return;
        }

        if (got == Herd.CAME) {
            host.sound(Synth.CHIME, 0.45f, 1.5f);
            host.buzz(30);
            if (hd.knots() == 1) dispatch = new Dispatch(host, Lang.lines("herd.first", mission.round, null));
            else herdSaid(Lang.lines("herd.came", mission.round, null), hd.knots());
        } else if (got == Herd.ALL_CAME) {
            host.sound(Synth.CHIME, 0.55f, 1.2f);
            host.buzz(40);
            dispatch = new Dispatch(host, Lang.lines("herd.all", mission.round, null));
        } else if (got == Herd.LOST) {
            host.sound(Synth.TOCK, 0.4f, 0.7f);
            herdSaid(Lang.lines("herd.lost", mission.round, null), hd.knots());
        } else if (got == Herd.BLAST) {
            if (hd.step < Herd.LIGHT) scars.add(host, x, z);
            herdBlast(hd.step >= Herd.LIGHT ? "herd.again" : "herd.blast");
            return;
        } else if (got == Herd.WON) {
            host.sound(Synth.CHIME, 0.6f, 1f);
            host.buzz(60);
        }
        // led off the navigator's line, those after her heat; too long off it, they go off
        if (heat(dt, hd)) {
            int m = hd.spots(herdSpots);
            for (int q = 0; q < m; q++) scars.add(host, herdSpots[q * 2], herdSpots[q * 2 + 1]);
            herdBlast("herd.hot");
            return;
        }
        // near them, their ticking: quicker the nearer
        float near = hd.nearest(x, z);
        herdTick -= dt;
        if (near < 25f && herdTick <= 0f) {
            herdTick = 0.25f + near * 0.03f;
            host.sound(Synth.TOCK, 0.1f + 0.25f * (1f - near / 25f), 1.7f);
        }
        // the town runs from them: one of those after her at a time
        int k = (int) (time * 4f) % Herd.ALL;
        if (hd.state[k] == Herd.COMES) {
            walkers.alarm(hd.x[k], hd.z[k], 7f);
            pets.alarm(hd.x[k], hd.z[k], 8f);
        }
    }

    /** Gone off: the flash, the bang; out of the car; she put back where it is tried again from, the dispatcher saying so. */
    private void herdBlast(String say) {
        Herd hd = herd;
        host.sound(Synth.THUNDER, 0.9f, 1.3f);
        host.sound(Synth.CRASH, 0.8f, 0.8f);
        host.buzz(240);
        whiteOut = WHITE_OUT;
        shake = 1f;
        if (driving != null) getOut();
        hd.again(city, herdAgain);
        runner.put(herdAgain[0], herdAgain[1], herdAgain[2], herdAgain[3]);
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        herdUntil = 0f;
        hot = 0f;
        hotBest = -1f;
        dispatch = new Dispatch(host, Lang.lines(say, mission.round, null));
    }

    // off the line: how much further the way may get than the least it was, and for how long, before they go off
    static final float HOT_FAR = 80f, HOT_FOR = 10f;
    // how long they have been heating (cooling as fast once back), the least the way was, to where
    private float hot, hotBest = -1f, hotGoalX, hotGoalZ, hotSettle;
    static final float HOT_SETTLE = 0.8f;
    private final float[] herdSpots = new float[2 * Herd.KNOTS];
    private final Scars scars = new Scars();

    /** For the desktop stand: how hot those after her are, the way left, the least it was. */
    String standHot() {
        return String.format(java.util.Locale.ROOT, "hot %.1f left %.0f best %.0f", hot, nav.left, hotBest);
    }

    /** For the desktop stand: a blast's spot torn at (x, z). */
    void standScar(float x, float z) {
        scars.add(host, x, z);
    }

    /**
     * Whether those after her have gone off for her leading them away from where the navigator
     * says: through the town to the next knot or out to the pier, if the way there gets longer by
     * so much than the least it has been, they heat; back toward it, they cool.
     */
    private boolean heat(float dt, Herd hd) {
        boolean watch = (hd.step == Herd.GATHER || hd.step == Herd.ISLE) && hd.chasing() > 0 && navGiven() && nav.led() && !nav.air
                && indoors == null && roadT < 0f && hd.goal(goal);
        if (!watch) {
            hot = 0f;
            hotBest = -1f;
            nav.warn = false;
            return false;
        }
        if (hotBest < 0f || Math.abs(goal[0] - hotGoalX) > 1f || Math.abs(goal[2] - hotGoalZ) > 1f) {
            hotGoalX = goal[0];
            hotGoalZ = goal[2];
            hot = 0f;
            hotSettle = HOT_SETTLE;
        }
        // a new place: the navigator finds the way to it first
        if (hotSettle > 0f) {
            hotSettle -= dt;
            hotBest = nav.left;
            nav.warn = false;
            return false;
        }
        hotBest = Math.min(hotBest, nav.left);
        boolean off = nav.left > hotBest + HOT_FAR;
        hot = off ? hot + dt : Math.max(0f, hot - dt);
        nav.warn = hot > 0f;
        return hot >= HOT_FOR;
    }

    private final float[] colliderView = new float[4];

    /** Whether the set shows the night at LAB: the hotheads' briefing, before it has begun. */
    private boolean prologue() {
        if (cherry != null && cherry.clock <= 0f) {
            // the black cherry's briefing: the hall as it is now
            cherry.idle(time);
            Collider.view(colliderView);
            return true;
        }
        if (herd == null || herd.clock > 0f || herd.over()) return false;
        Collider.view(colliderView);
        return true;
    }

    // the eye shaken by a blast: how far at most, how fast it dies away, how fast it shakes
    static final float SHAKE_FAR = 0.9f, SHAKE_DIES = 0.9f, SHAKE_FAST = 31f;
    /** How hard the eye shakes now, 1 at a blast, dying to nothing. */
    private float shake;
    private final float[] yachtAim = new float[3];

    // the switch in the secret hall: how long it takes to throw; since it was taken hold of (less than nothing: not yet)
    static final float LEVER_S = 0.7f;
    private float leverAt = -1f;
    private boolean towerSay, shutRead;
    /** The island shut: the bridge goes up, the yachts stay wrecked, the brums stay on it. */
    private void shutIsland() {
        Island.shut = true;
        wrecked(true);
    }

    /** Whether the jobs won leave the island shut: the hot heads done, the black cherry not yet. */
    private boolean shutByJobs() {
        return (kindsWon & (1 << 12)) != 0 && (kindsWon & (1 << 13)) == 0;
    }

    /** The yachts wrecked or whole, as the job says; the pier's cells built again when that changes. */
    private void wrecked(boolean now) {
        if (now == Island.wrecked) return;
        Island.wrecked = now;
        Herd.target(yachtAim);
        city.forget(yachtAim[0] - 40f, yachtAim[2] - 40f, yachtAim[0] + 40f, yachtAim[2] + 40f);
    }

    /** Lines over the frame for a while: how many knots are after her put in. */
    private void herdSaid(String[] lines, int knots) {
        for (int k = 0; k < lines.length; k++) lines[k] = lines[k].replace("{knots}", String.valueOf(knots));
        herdSay = lines;
        herdUntil = time + 3.5f;
    }

    /**
     * Over the frame: the clock in the corner, under it how many are after her; what was said
     * a moment ago; where the way goes now, in a word and a mark.
     */
    private void herdFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E, orange = 0xFFFF8A3A;
        Herd hd = herd;
        String c = clock(hd.over() ? hd.doneAt : hd.clock);
        shadowed(f, c, W - PixelFont.width(c, 2) - 8, 14, 2, cream, ink);
        // how many after her: on the left, under the pause, clear of the navigator
        String n = hd.coming() + "/" + Herd.ALL;
        shadowed(f, n, 8, (int) (pauseY() + 22f), 2, orange, ink);
        if (hot > 0f) {
            String[] w = Lang.lines("herd.off", mission.round, null);
            String left = String.valueOf((int) Math.ceil(HOT_FOR - hot));
            for (int k = 0; k < w.length; k++) w[k] = w[k].replace("{s}", left);
            caption(f, w, 0.66f);
        } else if (herdSay != null && time < herdUntil) caption(f, herdSay, 0.66f);
        boolean aimed = hd.goal(goal);
        // in the secret hall, to the switch: its place on the wall
        if (hd.step == Herd.LEVER && indoors != null && indoors.secret) {
            float v = indoors.deep - Indoors.WALL - 0.8f;
            goal[0] = indoors.wx(Rooms.LEVER_U, v);
            goal[1] = Indoors.FLOOR + Rooms.LEVER_Y + 0.8f;
            goal[2] = indoors.wz(Rooms.LEVER_U, v);
            aimed = true;
        }
        if (!aimed) return;
        wordAtTop(f, Lang.lines(hd.goalWord(), mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], hd.mark(), Marks.OPEN);
    }

    // ---- the black cherry: the hole in the collider hall (see Cherry) ----
    /**
     * Home's rooms, built once: they are the house itself, standing in the town (their walls its
     * walls, its windows open on the street), drawn with the town while she is near, and the room
     * she is in when she is inside.
     */
    private Indoors homeRoom;

    Indoors homeRoom() {
        if (homeRoom == null) homeRoom = Rooms.home(City.HOME_DOOR_X, City.HOME_DOOR_Z);
        return homeRoom;
    }

    /** Whether home stands near enough (x, z) to be drawn with the town. */
    private static boolean nearHome(float x, float z) {
        float dx = x - City.HOME_DOOR_X, dz = z - City.HOME_DOOR_Z - City.HOME_HZ;
        return dx * dx + dz * dz < HOME_SEEN * HOME_SEEN;
    }

    static final float HOME_SEEN = 260f;

    /** Where every game begins: so far before home's door, in its forecourt. */
    static final float START_BACK = 4.5f;

    /** Where the old flat's sofa seats its brum (x, y, z). */
    private final float[] lodger = new float[3];
    static final float LODGER_LATE = 3.7f;   // his breath out of step with hers

    /** The magnet a blow is on its way to (of Cherry.KNOCKED), or -1. */
    private int cherryAim = -1;
    private float cherryUntil;
    private String[] cherrySay;
    private final float[] cherryEye = new float[5];

    /** Whether the lobby's lift goes down to the collider hall: on the job, and once it is done. */
    private boolean colliderOpen() {
        return cherry != null || (kindsWon & (1 << 13)) != 0;
    }

    /** The set's picture of the hall: the black cherry's briefing, the hall as it is; else the night it went wrong. */
    private void setDrawn(DrawList d) {
        if (cherry != null) Collider.live(d, Collider.FLOOR, time, cherry, true, false);
        else Collider.draw(d, time, dispatch.sceneTime());
    }

    /**
     * A step of it: in the hall, the dispatcher's word the first time; the jet's touch and the heat
     * (too hot: the flash, back at the lift's doors); a blow at a knocked magnet sets it; the ring
     * taking the hole, the eye held on the trap; done, the island opened.
     */
    private void cherryStep(float dt) {
        Cherry ch = cherry;
        Runner r = runner;
        Indoors in = indoors;
        boolean inHall = in != null && in.collider && swapT < 0f && in.holds(r.x, r.z);
        int was = ch.step;
        int got = ch.update(dt, inHall, r.x, r.y - Indoors.FLOOR, r.z);
        // a tap, a blow of the mallet: at the knocked magnet in reach, she turning to it (the mallet
        // swung here, not thrown); it lands at the height of the swing
        if (inHall && ch.step == Cherry.MEND && driving == null && !r.held && !r.inAir() && r.throwing() < 0f && r.tapped()) {
            cherryAim = ch.within(r.x, r.z);
            float tx = r.x + (float) Math.sin(r.heading) * 2f, tz = r.z + (float) Math.cos(r.heading) * 2f;
            if (cherryAim >= 0) {
                tx = Collider.ringX(Cherry.KNOCKED[cherryAim]);
                tz = Collider.ringZ(Cherry.KNOCKED[cherryAim]);
            }
            r.lock(Toss.STRIKE_LOCK, tx, tz);
            host.sound(Synth.WHOOSH, 0.3f, 1.3f);
        }
        if (cherryAim >= 0 && r.throwing() >= Toss.RELEASE) {
            ch.setOne(cherryAim);
            cherryAim = -1;
            host.sound(Synth.CLANG, 0.8f, 0.9f);
            host.sound(Synth.STONE, 0.5f, 0.6f);
            host.buzz(70);
            if (ch.step == Cherry.MEND) {
                String[] w = Lang.lines("cherry.set", mission.round, null);
                for (int q = 0; q < w.length; q++) w[q] = w[q].replace("{left}", String.valueOf(Cherry.KNOCKED.length - ch.setCount()));
                cherrySay = w;
                cherryUntil = time + 2.5f;
            }
        }
        if (r.throwing() < 0f) cherryAim = -1;
        if (got == Cherry.HALL) {
            host.sound(Synth.CHIME, 0.5f, 1f);
            dispatch = new Dispatch(host, Lang.lines("cherry.hall", mission.round, null));
            return;
        } else if (got == Cherry.TOUCH) {
            host.sound(Synth.HUM, 0.6f, 1.6f);
            host.buzz(140);
        } else if (got == Cherry.HOT) {
            // too hot: the flash, and she is at the lift's doors again, the magnets set as they were
            host.sound(Synth.THUNDER, 0.8f, 1.4f);
            host.buzz(260);
            whiteOut = WHITE_OUT;
            ch.again();
            float u = in.side * (in.half - 1.6f);
            r.put(in.wx(u, in.liftV), Indoors.FLOOR, in.wz(u, in.liftV), in.heading(-in.side, 0f));
            r.recenter();
            cherrySay = null;
            dispatch = new Dispatch(host, Lang.lines("cherry.hot", mission.round, null));
            return;
        } else if (got == Cherry.FLASH) {
            host.sound(Synth.thunder(2), 0.9f, 0.8f);
            host.sound(Synth.CHIME, 0.6f, 0.7f);
            host.buzz(220);
            whiteOut = WHITE_OUT;
        } else if (got == Cherry.WON) {
            openIsland();
            host.sound(Synth.CHIME, 0.6f, 1f);
            host.buzz(60);
        }
        if (ch.step == Cherry.SEAL && was == Cherry.MEND) {
            host.sound(Synth.WHOOSH, 0.6f, 0.5f);
            cherrySay = null;
        }
        // the ring taking the hole: she stands, the eye on the trap from before it and to one side
        if (ch.step == Cherry.SEAL && in != null && in.collider) {
            if (was != Cherry.SEAL) {
                errand = NO_ERRAND;
                r.stopLead();
                letGo();
            }
            r.held = true;
            r.speed = 0f;
            cherryEye[0] = Collider.HX - 7f;
            cherryEye[1] = Indoors.FLOOR + 3.2f;
            cherryEye[2] = Collider.DET_Z - 11f;
            float tx = Collider.HX - cherryEye[0], tz = Collider.DET_Z - cherryEye[2];
            cherryEye[3] = (float) Math.atan2(tx, tz);
            cherryEye[4] = (float) Math.atan2(Indoors.FLOOR + Collider.HOLE_Y - 1.2f - cherryEye[1], Math.sqrt(tx * tx + tz * tz));
            r.eyeAt(cherryEye);
        } else if (was == Cherry.SEAL) {
            r.held = false;
            r.recenter();
        }
    }

    /** The island opened: the bridge down, the yachts whole again, the brums on it taken off to be mended. */
    private void openIsland() {
        Island.shut = false;
        wrecked(false);
    }

    /**
     * Over the frame: the clock in the corner, under the pause how many magnets are set; what was
     * said a moment ago, or how hot she is; where the way goes now, in a word and a mark: up to the
     * laboratory, to the lift in its lobby, in the hall the nearest magnet still off.
     */
    private void cherryFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E, orange = 0xFFFF8A3A;
        Cherry ch = cherry;
        String c = clock(ch.over() ? ch.doneAt : ch.clock);
        shadowed(f, c, W - PixelFont.width(c, 2) - 8, 14, 2, cream, ink);
        Indoors in = indoors;
        boolean inHall = in != null && in.collider;
        if (inHall && ch.step == Cherry.MEND) {
            String n = ch.setCount() + "/" + Cherry.KNOCKED.length;
            shadowed(f, n, 8, (int) (pauseY() + 22f), 2, orange, ink);
        }
        if (ch.step != Cherry.MEND && ch.step != Cherry.UP) return;
        if (inHall && ch.heat > 0f) {
            String[] w = Lang.lines("cherry.burn", mission.round, null);
            String p = String.valueOf(Math.round(ch.heat * 100f));
            for (int k = 0; k < w.length; k++) w[k] = w[k].replace("{p}", p);
            caption(f, w, 0.66f);
        } else if (cherrySay != null && time < cherryUntil) caption(f, cherrySay, 0.66f);
        else if (in == null && driving != null && Mountain.deck(driving.x, driving.z)) caption(f, "lab.leave");
        String word;
        int pic;
        if (inHall) {
            int k = ch.nearestOff(runner.x, runner.z);
            if (k < 0) return;
            int piece = Cherry.KNOCKED[k];
            goal[0] = Collider.ringX(piece);
            goal[1] = Indoors.FLOOR + Collider.RING_Y + 1.6f;
            goal[2] = Collider.ringZ(piece);
            word = "cherry.magnet";
            pic = Marks.PIN;
        } else if (in != null && in.lobby) {
            float u = in.side * (in.half - 0.4f);
            goal[0] = in.wx(u, in.liftV);
            goal[1] = Indoors.FLOOR + 2.4f;
            goal[2] = in.wz(u, in.liftV);
            word = "cherry.lift";
            pic = Marks.LIFT;
        } else if (in == null) {
            boolean wheel = driving != null;
            int k = Mountain.freeSpace();
            goal[0] = wheel ? Mountain.spaceX(k) : Mountain.MX;
            goal[1] = Mountain.TOP + 4f;
            goal[2] = wheel ? Mountain.spaceZ(k) : Mountain.DOOR_Z - 1f;
            word = wheel ? "lab.parking" : "goal.lab";
            pic = Marks.LAB;
        } else return;
        wordAtTop(f, Lang.lines(word, mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], pic, Marks.OPEN);
    }

    /** For the desktop stand: her down the lift into the collider hall at once, out of the cabin; or, with n of 0 or more, so many magnets set. */
    String standCherry(int n) {
        if (n >= 0) {
            if (cherry == null) return "no black cherry now";
            cherry.standSet(n);
            return "set " + cherry.setCount() + " step " + cherry.step;
        }
        indoors = Rooms.collider(cherry == null && (kindsWon & (1 << 13)) != 0);
        float u = indoors.side * (indoors.half - 1.6f);
        runner.put(indoors.wx(u, indoors.liftV), Indoors.FLOOR, indoors.wz(u, indoors.liftV), indoors.heading(-indoors.side, 0f));
        runner.room = indoors;
        runner.recenter();
        runner.update(0f, city, cars, walkers);
        return "in the collider hall";
    }

    /** For the desktop stand: how the black cherry stands. */
    String standCherryState() {
        if (cherry == null) return "no black cherry now; island shut " + Island.shut;
        return String.format(java.util.Locale.ROOT, "step %d set %d heat %.2f jet %.2f reach %.1f clock %.1f", cherry.step, cherry.setCount(), cherry.heat,
                cherry.jet, cherry.reach, cherry.clock) + ", island shut " + Island.shut;
    }

    // ---- the navigator's trial ----

    /** The pickup the box goes to, and the eye on the unpacking. */
    Pickup labCar;
    private final float[] labEye = new float[5];
    private int labSeen = -1;
    private float labUntil;
    private String labSay;

    /**
     * A step of the job: up to the laboratory and in, the box handed over at the counter (the eye
     * on the two of them), out to the pickup with it and unpacked there (the eye on her and the
     * pickup), the navigator on, the places one after another.
     */
    private void labStep(float dt) {
        Lab lb = lab;
        float x = driving != null ? driving.x : runner.x, z = driving != null ? driving.z : runner.z;
        if (lb.step >= Lab.CARRY && labCar == null) labCar = labPickup();
        float cx = labCar != null ? labCar.x : 0f, cz = labCar != null ? labCar.z : 0f;
        boolean wasHanding = lb.handing(), wasUnpacking = lb.unpacking();
        int got = lb.update(dt, x, z, driving != null, nav.led() ? nav.left : -1f, indoors, cx, cz);
        if (lb.handing()) handOver(wasHanding);
        else if (wasHanding) runner.held = false;
        if (indoors != null && indoors.lobby) indoors.given = lb.step >= Lab.CARRY;
        if (lb.unpacking()) unpackAt(wasUnpacking);
        else if (wasUnpacking) {
            runner.held = false;
            runner.recenter();
        }
        if (lb.step != labSeen) {
            labSeen = lb.step;
            labSay = lb.step == Lab.CARRY ? "lab.carry" : lb.step == Lab.TRIAL ? "lab.fit" : null;
            labUntil = time + 3.5f;
        }
        if (got == Lab.ON) {
            // the navigator comes on, open
            navShut = false;
            host.sound(Synth.CHIME, 0.5f, 1.2f);
            host.buzz(40);
        } else if (got == Lab.REACHED) {
            host.sound(Synth.CHIME, 0.45f, 1.5f);
            host.buzz(30);
        } else if (got == Lab.WON) {
            host.sound(Synth.CHIME, 0.6f, 1f);
            host.buzz(60);
        } else if (got == Lab.HANDING) {
            host.sound(Synth.CHIME, 0.25f, 1.8f);
        } else if (got == Lab.GIVEN) {
            host.sound(Synth.WOOD, 0.4f, 1.3f);
            host.buzz(30);
        } else if (got == Lab.OPENING) {
            host.sound(Synth.TEAR, 0.45f, 1.1f);
        }
    }

    /** The pickup for the box: the one nearest the deck, if she left one there or near; else one of the laboratory's, put in a free space. */
    private Pickup labPickup() {
        float ox = Mountain.MX - 10f, oz = Mountain.AISLE_Z;
        Pickup best = null;
        float bd = 90f * 90f;
        for (Pickup p : duty.all) {
            float dx = p.x - ox, dz = p.z - oz, dd = dx * dx + dz * dz;
            if (dd < bd) {
                bd = dd;
                best = p;
            }
        }
        if (best != null) return best;
        // backed into it, nose to the aisle: she drives straight out
        int k = Mountain.freeSpace();
        Pickup p = new Pickup(Mountain.spaceX(k), Mountain.spaceZ(k), Mountain.spaceHeading(k) + (float) Math.PI, 0x1AB5L);
        p.y = Mountain.TOP;
        duty.all.add(p);
        return p;
    }

    // where she stands at the pickup to unpack (across from its middle, along it), where the box
    // goes in its bed (across, along, how high over the pickup), where the eye is (across, up,
    // along) and what it looks at (across, up, along); all in the pickup's own terms (x across,
    // to its left less than nothing)
    static final float UNPACK_U = -Pickup.HALF_WIDE - 0.75f, UNPACK_V = -0.7f, BED_U = -0.45f, BED_V = -1.3f, BED_Y = 1.0f;
    static final float UNPACK_EYE_U = -5.2f, UNPACK_EYE_Y = 3.6f, UNPACK_EYE_V = -5.4f, UNPACK_LOOK_U = -0.3f, UNPACK_LOOK_Y = 1.1f, UNPACK_LOOK_V = -0.6f;

    /** A point in the pickup's own terms (across, along) in the town's, into out (x, z). */
    private static void onCar(Pickup p, float u, float v, float[] out) {
        float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading);
        out[0] = p.x + c * u + s * v;
        out[1] = p.z - s * u + c * v;
    }

    private final float[] carAt = new float[2];

    /** Unpacking: she stands by the pickup's side facing it, the eye off to the side and before it. */
    private void unpackAt(boolean already) {
        Runner r = runner;
        Pickup p = labCar;
        if (p == null) return;
        if (!already) {
            errand = NO_ERRAND;
            r.stopLead();
            letGo();
            onCar(p, UNPACK_U, UNPACK_V, carAt);
            float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading);
            r.put(carAt[0], city.ground(carAt[0], carAt[1]), carAt[1], (float) Math.atan2(c, -s));
        }
        r.held = true;
        r.speed = 0f;
        onCar(p, UNPACK_EYE_U, UNPACK_EYE_V, carAt);
        labEye[0] = carAt[0];
        labEye[1] = p.y + UNPACK_EYE_Y;
        labEye[2] = carAt[1];
        onCar(p, UNPACK_LOOK_U, UNPACK_LOOK_V, carAt);
        float tx = carAt[0] - labEye[0], tz = carAt[1] - labEye[2];
        labEye[3] = (float) Math.atan2(tx, tz);
        labEye[4] = (float) Math.atan2(p.y + UNPACK_LOOK_Y - labEye[1], Math.sqrt(tx * tx + tz * tz));
        r.eyeAt(labEye);
    }

    private final float[] devAt = new float[3];

    /**
     * The box and the navigator out of doors: in her hand while she carries it; at the pickup set
     * down in its bed, opened, the navigator lifted out and over into the cab by the wheel; the box
     * left open in the bed. And in any pickup she drives (or the one it went into) once it is hers,
     * the navigator by the wheel.
     */
    private void labDrawn(DrawList d, Runner r) {
        Lab lb = lab;
        Pickup p = labCar;
        if (lb != null && p != null && lb.step >= Lab.UNPACK) {
            float t = lb.step == Lab.UNPACK ? lb.unpackT : Lab.UNPACK_TIME;
            onCar(p, BED_U, BED_V, carAt);
            float bx = carAt[0], by = p.y + BED_Y + LabTop.BOX_Y, bz = carAt[1];
            if (t < Lab.SET_END) {
                // from her hand down into the bed
                r.bone("hand_l", handTo);
                float a = t / Lab.SET_END, e = a * a * (3f - 2f * a);
                bx = handTo[0] + (bx - handTo[0]) * e;
                bz = handTo[2] + (bz - handTo[2]) * e;
                by = handTo[1] - LabTop.BOX_Y + (by - handTo[1] + LabTop.BOX_Y) * e + 0.25f * (float) Math.sin(Math.PI * a);
            }
            float open = Math.max(0f, Math.min(1f, (t - Lab.SET_END) / (Lab.OPEN_END - Lab.SET_END)));
            LabTop.box(d, bx, by, bz, p.heading, open);
            if (t >= Lab.OPEN_END && t < Lab.IN_END) {
                // up out of the box and over the cab's side to the wheel
                dash(p, devAt);
                float a = (t - Lab.OPEN_END) / (Lab.IN_END - Lab.OPEN_END), e = a * a * (3f - 2f * a);
                float x = bx + (devAt[0] - bx) * e, z = bz + (devAt[2] - bz) * e;
                float y = by + 0.1f + (devAt[1] - by - 0.1f) * e + 0.9f * (float) Math.sin(Math.PI * Math.min(1f, a * 1.2f));
                LabTop.device(d, x, y, z, p.heading + (float) Math.PI * (1f - e));
            }
        }
        if (lb != null && lb.carrying() && lb.step != Lab.UNPACK) {
            r.bone("hand_l", handTo);
            LabTop.box(d, handTo[0], handTo[1] - LabTop.BOX_Y - 0.03f, handTo[2], r.heading, 0f);
        }
        boolean inCab = lb != null && (lb.step > Lab.UNPACK || lb.step == Lab.UNPACK && lb.unpackT >= Lab.IN_END);
        Pickup q = driving != null ? driving : inCab ? labCar : null;
        if (q != null && navGiven()) {
            dash(q, devAt);
            LabTop.device(d, devAt[0], devAt[1], devAt[2], q.heading);
        }
    }

    /** Where the navigator sits by the wheel of pickup p: before the seat, a little toward the middle, up on the dash. */
    private static void dash(Pickup p, float[] out) {
        p.seat(out);
        float c = (float) Math.cos(p.heading), s = (float) Math.sin(p.heading), fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading);
        out[0] += fx * 0.72f + c * 0.3f;
        out[1] += 0.42f;
        out[2] += fz * 0.72f - s * 0.3f;
    }

    /**
     * Over the frame, the job: the clock (in the trial, the time it has still to run, green while it
     * runs, pale while it waits); what is said when a step begins, a moment; where the way goes now,
     * in a word and a mark.
     */
    private void labFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E, pale = 0xFFB8B0A0;
        Lab lb = lab;
        if (lb.step >= Lab.TRIAL) {
            String c = clock(Lab.TRIAL_TIME - lb.counted);
            shadowed(f, c, W - PixelFont.width(c, 2) - 8, 14, 2, lb.counting() || lb.over() ? green : pale, ink);
        } else {
            String c = clock(lb.clock);
            shadowed(f, c, W - PixelFont.width(c, 2) - 8, 14, 2, cream, ink);
        }
        // at the wheel on the deck: leave it and go in
        if (lb.step == Lab.GO && driving != null && Mountain.deck(driving.x, driving.z)) caption(f, "lab.leave");
        else if (labSay != null && time < labUntil) caption(f, labSay);
        boolean wheel = driving != null;
        float cx = labCar != null ? labCar.x : 0f, cz = labCar != null ? labCar.z : 0f;
        if (!lb.goal(wheel, indoors, cx, cz, goal)) return;
        String w = lb.goalWord(wheel);
        if (w != null) wordAtTop(f, Lang.lines(w, mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], lb.mark(), Marks.OPEN);
    }

    // ---- the lights gone wrong (see Fault): the job, and now and then one of itself between jobs ----

    /** The panel of a light's box, if one is open now. */
    private Wires faultWires() {
        if (fault != null && fault.open()) return fault.panel;
        if (stray != null && stray.open()) return stray.panel;
        return null;
    }

    /** The job's light no more (the job put by, played again, or over): if it is still wrong, right again. */
    private void dropFault() {
        if (fault != null && !fault.over() && stray == null) Crossings.mended();
        fault = null;
    }

    /**
     * Between jobs, once the light's job is behind her: now and then a light near her goes wrong;
     * left far behind, it comes right of itself; at its box on foot, the box opens.
     */
    private void strayStep(float dt) {
        if (!passed(14) || fault != null) return;
        if (stray == null) {
            strayIn -= dt;
            if (strayIn > 0f || indoors != null) return;
            strayIn = STRAY_EVERY + (float) Math.random() * STRAY_MORE;
            if (Fault.pick(runner.x, runner.z, (long) (time * 1000f), faultAt))
                stray = new Fault(faultAt[0], faultAt[1], (long) (time * 977f) + 0x57A7L);
            return;
        }
        float dx = stray.boxX() - runner.x, dz = stray.boxZ() - runner.z;
        if (dx * dx + dz * dz > STRAY_LEFT * STRAY_LEFT) {
            Crossings.mended();
            stray = null;
            return;
        }
        boolean was = stray.open();
        stray.update(dt, runner, driving == null && indoors == null && dispatch == null, toss.tool == Toss.SCREWDRIVER);
        if (stray.over()) {
            strayMended();
            return;
        }
        if (stray.open() && !was) {
            letGo();
            host.sound(Synth.LATCH, 0.5f, 1.3f);
            host.buzz(20);
        }
    }

    /** A light gone wrong between jobs mended: its likes, every time. */
    private void strayMended() {
        stray = null;
        letGo();
        host.sound(Synth.CHIME, 0.45f, 1.1f);
        host.buzz(60);
        like(STRAY_LIKES, STRAY_HEARTS);
        saveLikes();
    }

    /** Over the frame, the light gone wrong: the clock in the corner; the way to its box, in a word and a mark. */
    private void faultFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        Fault fl = fault;
        String clock = clock(fl.over() ? fl.doneAt : fl.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (fl.over() || fl.open()) return;
        fl.goal(goal);
        boolean wants = fl.atBox(runner) && toss.tool != Toss.SCREWDRIVER;
        wordAtTop(f, Lang.lines(wants ? "goal.screwdriver" : "goal.light", mission.round, null)[0], green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], Marks.PANEL, Marks.OPEN);
    }

    /** Over the frame, the tow: the clock in the corner; where the way goes now, in a word and a mark. */
    private void towFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, green = 0xFF46D27E;
        Tow tw = tow;
        String clock = clock(tw.over() ? tw.doneAt : tw.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (tw.over() || !tw.goal(duty, driving, runner, goal)) return;
        String w = Lang.lines(tw.goalWord(driving, runner, kit.held == Kit.ROPE), mission.round, null)[0];
        wordAtTop(f, w, green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], tw.mark(driving), Marks.OPEN);
    }

    // ---- the heap in the pass (see Rockfall) ----

    /** The call out a step on: the heap, and the mallet in hand at what lies in the pass. */
    private void rescueStep(float dt) {
        int got = rescue.update(dt, duty, driving, runner, city, host);
        if (got == Rescue.AT_HEAP) {
            letGo();
            dispatch = new Dispatch(host, Lang.lines("rescue.heap", mission.round, null));
        } else if (got == Rescue.BLAST) {
            shake = 1f;
            whiteOut = Math.max(whiteOut, 0.25f);
            host.buzz(220);
        }
        if (rescue.openedAt >= 0f && rescue.openedAt == rescue.clock) {
            letGo();
            like(OPEN_LIKES, OPEN_HEARTS);
        }
        // a blow of the mallet on foot, at what lies in the pass
        boolean swings = driving == null && runner.throwing() >= 0f;
        if (swings && !swungSeen) rescue.strike(runner.x, runner.z, (float) Math.sin(runner.heading), (float) Math.cos(runner.heading));
        swungSeen = swings;
        if (rescue.hit() != rescueHits) {
            rescueHits = rescue.hit();
            host.sound(Synth.STONE, 0.6f, 0.8f);
            host.buzz(40);
        }
    }

    /** What a blast left in the pass: still lying there, in the way, till she is far off or a call out begins anew. */
    private void debrisStep(float dt) {
        if (debris == null) return;
        float x = driving != null ? driving.x : runner.x, z = driving != null ? driving.z : runner.z;
        float dx = x - debris.fall.px, dz = z - debris.fall.pz;
        if (rescue != null || dx * dx + dz * dz > DEBRIS_GONE * DEBRIS_GONE) {
            if (city.extra == debris) city.extra = null;
            debris = null;
            return;
        }
        if (city.extra == null) city.extra = debris;
        if (city.extra == debris) debris.fall.update(dt, driving, runner, host);
    }

    /** Over the frame, a call out: the clock and the pay, how far off where the way goes is, and what it is. */
    private void rescueFlat(DrawList d, Px f) {
        int ink = 0xFF232A4A, cream = 0xFFF4E8D2, gold = 0xFFFFD45A, green = 0xFF46D27E;
        Rescue rs = rescue;
        String clock = clock(rs.over() ? rs.doneAt : rs.clock);
        shadowed(f, clock, W - PixelFont.width(clock, 2) - 8, 14, 2, cream, ink);
        if (rs.over()) {
            // the way through just opened: said while the job's last moments go
            caption(f, "rescue.open", 0.3f);
            return;
        }
        // at the heap on foot, wedged fast: the charge out of the bed, to the heap, then away from it
        if (rs.heapSaid && rs.fall.sealed && driving == null) {
            if (rs.chargeSet) {
                wordAtTop(f, Lang.lines("goal.away", mission.round, null)[0], green, ink);
                return;
            }
            if (kit.held == Kit.CHARGE) {
                rs.face(runner.x, runner.z, goal);
                wordAtTop(f, Lang.lines("goal.heap", mission.round, null)[0], green, ink);
                placeMark(d, f, goal[0], goal[1], goal[2], Marks.PASS, Marks.OPEN);
                return;
            }
            Pickup p = nearestPickup(runner.x, runner.z);
            if (p != null) {
                goal[0] = p.x - (float) Math.sin(p.heading) * 2.4f;
                goal[1] = p.y + 2.2f;
                goal[2] = p.z - (float) Math.cos(p.heading) * 2.4f;
                wordAtTop(f, Lang.lines("goal.charge", mission.round, null)[0], green, ink);
                if (!bedSeen) placeMark(d, f, goal[0], goal[1], goal[2], Marks.TRUCK, Marks.OPEN);
                return;
            }
        }
        if (!rs.goal(duty, driving, runner, goal)) return;
        String w = Lang.lines(rs.goalWord(), mission.round, null)[0];
        wordAtTop(f, w, green, ink);
        placeMark(d, f, goal[0], goal[1], goal[2], rs.step == Rescue.PICKUP ? Marks.TRUCK : Marks.PASS, Marks.OPEN);
    }

    /** The duty pickup on land nearest (x, z), or none. */
    private Pickup nearestPickup(float x, float z) {
        Pickup best = null;
        float bd = 1e18f;
        for (Pickup p : duty.all) {
            if (p.boat) continue;
            float dd = (p.x - x) * (p.x - x) + (p.z - z) * (p.z - z);
            if (dd < bd) {
                bd = dd;
                best = p;
            }
        }
        return best;
    }

    /** Where the way goes, in a word at the top: large, unless it is too long to clear the corners. */
    private void wordAtTop(Px f, String w, int color, int ink) {
        int scale = PixelFont.width(w, 2) > W - 120 ? 1 : 2;
        shadowed(f, w, (W - PixelFont.width(w, scale)) / 2, scale == 2 ? 16 : 20, scale, color, ink);
    }

    /**
     * Where the way goes, as a place: its picture (see Marks), half see-through, over the place
     * while it is in sight, floating a little; out of sight, at the edge of the frame the way it is.
     * Green, open to her; red, shut for now.
     */
    private void placeMark(DrawList d, Px f, float x, float y, float z, int pic, int color) {
        nav.aim(x, z);
        float bob = 1.5f * (float) Math.sin(time * 2.4f);
        float half = Marks.SIDE * Marks.SCALE * 0.5f + 2f;
        boolean seen = d.project(x, y, z, at) && at[0] > half && at[0] < W - half && at[1] > 60 && at[1] < h - half;
        if (seen) {
            Marks.draw(f, pic, at[0], at[1] + bob, color, 1f);
            return;
        }
        float ax = (float) Math.sin(runner.eyeYaw), az = (float) Math.cos(runner.eyeYaw);
        float vx = x - runner.x, vz = z - runner.z;
        float ang = (float) Math.atan2(vx * az - vz * ax, vx * ax + vz * az);
        float ex = (float) Math.sin(ang), ey = -(float) Math.cos(ang);
        float k = Math.min((W * 0.5f - half - 10f) / Math.max(1e-3f, Math.abs(ex)), (h * 0.5f - 76f) / Math.max(1e-3f, Math.abs(ey)));
        float mx = W * 0.5f + ex * k, my = h * 0.5f + ey * k + bob;
        Marks.draw(f, pic, mx, my, color, 0.85f);
        // off the frame: a sharp point beside it, out toward the way it lies
        chevron(f, mx + ex * (half + 2f), my + ey * (half + 2f), ex, ey, color);
    }

    // the point beside a mark off the frame: how far out its tip is, how wide (half) its base
    static final float CHEV_LONG = 7f, CHEV_WIDE = 5f;

    /** A small sharp point over the frame at (cx, cy), its tip out along (ex, ey): a dark edge, the colour within. */
    private static void chevron(Px f, float cx, float cy, float ex, float ey, int color) {
        float m = (float) Math.sqrt(ex * ex + ey * ey);
        ex /= m;
        ey /= m;
        for (int pass = 0; pass < 2; pass++) {
            float grow = pass == 0 ? 1.2f : 0f;
            int c = pass == 0 ? 0xD0232A4A : color;
            float len = CHEV_LONG + grow * 1.6f, wide = CHEV_WIDE + grow * 1.6f, back = grow * 0.8f;
            int r0 = (int) Math.ceil(len + wide);
            for (int y = -r0; y <= r0; y++)
                for (int x = -r0; x <= r0; x++) {
                    float px = x + 0.5f, py = y + 0.5f;
                    float a = px * ex + py * ey + back, b = Math.abs(-px * ey + py * ex);
                    if (a < 0f || a > len + back) continue;
                    if (b > wide * (1f - a / (len + back))) continue;
                    f.rect(Math.round(cx) + x, Math.round(cy) + y, Math.round(cx) + x + 1, Math.round(cy) + y + 1, c);
                }
        }
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
        if (indoors.dark) torchLight(d);
        boolean inHome = indoors.home;
        if (inHome) {
            // home stands in the town: the light from overhead over a wider ground, the town through
            // its windows as far off as it goes, nothing of the room's dark round its lamps
            d.sun(0.12f, 1f, 0.18f, indoors.wx(0f, indoors.deep * 0.5f), Indoors.FLOOR + 1.6f, indoors.wz(0f, indoors.deep * 0.5f), 48f, 48f, 4.6f);
            d.fogStart = 40f;
            d.fogScale = 260f;
            d.spot[3] = 2000f;
        }
        java.util.Arrays.fill(d.flat, 0);
        if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
        Px f = flatPx;
        d.hudRows = 0;
        if (dispatch != null) {
            // the room in her head, or the eye on its way there
            dispatch.sceneAt = prologue() ? colliderView : null;
            dispatch.eye(r, eyeNow);
            d.camera(eyeNow[0], eyeNow[1], eyeNow[2], eyeNow[3], eyeNow[4], dispatch.focal(W * 1.25f, W), 0.03f, 80f);
        }
        // the set showing the night at LAB: the hall, nothing of home
        boolean scene = dispatch != null && dispatch.inScene();
        // (with the dispatcher on, the eye is his, set above; else the lift's shot, or behind her)
        if (scene) setDrawn(d);
        else if (dispatch != null) {
        } else if (standEye != null) camIn(d, standEye, W * 1.2f, inHome);
        else if (indoors.shot(eyeNow)) camIn(d, eyeNow, W * 1.25f, inHome);
        else {
            eyeHere[0] = r.eyeX;
            eyeHere[1] = r.eyeY;
            eyeHere[2] = r.eyeZ;
            eyeHere[3] = r.eyeYaw;
            eyeHere[4] = r.eyePitch;
            camIn(d, eyeHere, W * 1.2f, inHome);
        }
        // in home, the town round it, seen through its windows and its doors
        if (inHome && !scene && dispatch == null) {
            float ex = (float) Math.sin(camAt[3]), ez = (float) Math.cos(camAt[3]);
            city.draw(d, camAt[0], camAt[2], ex, ez);
            cars.draw(d, camAt[0], camAt[2], ex, ez);
            Crossings.lamps(d, camAt[0], camAt[2], ex, ez);
            duty.draw(d, camAt[0], camAt[2], ex, ez, time, null);
            walkers.draw(d, camAt[0], camAt[2], ex, ez);
            pets.draw(d, camAt[0], camAt[2], ex, ez, walkers);
        }
        // in home's bathroom, with the eye there too: behind the mirror the room turned about, and
        // she in it; else a plain wall's back over the mirror's hole
        boolean mirror = inHome && !scene && dispatch == null && Rooms.inBath(indoors, camAt[0], camAt[2]);
        if (inHome) d.append(mirror ? indoors.mirrored : indoors.cover);
        if (!scene) {
        // the room, unless the eye has gone back into her head past its walls (she sits against one)
        if (dispatch == null || indoors.holds(eyeNow[0], eyeNow[2])) {
            indoors.draw(d);
            Concert on = concert != null ? concert : stayed;
            if (indoors.concert && on != null) on.draw(d, indoors);
            if (indoors.townHall) {
                // the hall's people, and the clerk with the red folder in her hands till she gives it;
                // handed over, it goes from her hands to the other's, up a little on the way
                boolean holding = archive != null && archive.giverHolds() || folder != null && folder.clerkHas();
                Rooms.townHallFolk(d, indoors, time, holding, handsAt);
                float face = indoors.heading(indoors.side, 0f) + (float) Math.PI;
                if (archive != null && archive.waiting()) Archive.folder(d, handsAt[0], handsAt[1], handsAt[2], face);
                else if (folder != null && folder.passing() >= 0f) {
                    // the folder found on the roofs: from her hands into the clerk's, up a little on the way
                    float a = folder.passing(), e = a * a * (3f - 2f * a);
                    r.bone("hand_l", handTo);
                    float x = handTo[0] + (handsAt[0] - handTo[0]) * e, z = handTo[2] + (handsAt[2] - handTo[2]) * e;
                    float y = handTo[1] - 0.05f + (handsAt[1] - handTo[1] + 0.05f) * e + 0.35f * (float) Math.sin(Math.PI * a);
                    Archive.folder(d, x, y, z, r.heading + (float) Math.PI * 0.5f + (face - r.heading - (float) Math.PI * 0.5f) * e);
                }
                else if (archive != null && archive.giving() >= 0f) {
                    float a = archive.giving(), e = a * a * (3f - 2f * a);
                    r.bone("hand_l", handTo);
                    float x = handsAt[0] + (handTo[0] - handsAt[0]) * e, z = handsAt[2] + (handTo[2] - handsAt[2]) * e;
                    float y = handsAt[1] + (handTo[1] - 0.05f - handsAt[1]) * e + 0.35f * (float) Math.sin(Math.PI * a);
                    Archive.folder(d, x, y, z, face + (r.heading + (float) Math.PI * 0.5f - face) * e);
                }
            }
        }
        // the archive: its clerks, and the one on duty taking the folders over the counter
        if (indoors.archive) {
            Rooms.archiveFolk(d, indoors, time, archive != null && archive.dutyUp(), handsAt);
            float pass = archive != null ? archive.passing() : -1f;
            if (pass >= 0f) {
                float face = indoors.heading(0f, -1f) + (float) Math.PI;
                if (pass >= 1f) Archive.folder(d, handsAt[0], handsAt[1], handsAt[2], face);
                else {
                    float e = pass * pass * (3f - 2f * pass);
                    r.bone("hand_l", handTo);
                    float x = handTo[0] + (handsAt[0] - handTo[0]) * e, z = handTo[2] + (handsAt[2] - handTo[2]) * e;
                    float y = handTo[1] - 0.05f + (handsAt[1] - handTo[1] + 0.05f) * e + 0.35f * (float) Math.sin(Math.PI * pass);
                    Archive.folder(d, x, y, z, r.heading + (float) Math.PI * 0.5f + (face - r.heading - (float) Math.PI * 0.5f) * e);
                }
            }
        }
        // the switch in the secret hall: down once the island is shut, going down as she throws it
        if (indoors.secret) Rooms.lever(d, indoors, Island.shut ? 1f : leverAt >= 0f ? Math.min(1f, leverAt / LEVER_S) : 0f);
        // the old flat: the brum who lives there, on the sofa, looking at the room
        if (indoors.oldFlat) {
            indoors.seat(lodger);
            Frida.green = true;
            Frida.draw(d, lodger[0], lodger[1] - Runner.SEAT_DROP, lodger[2], indoors.seatHeading() + (float) Math.PI, Runner.SIZE, Frida.LOUNGE, time + LODGER_LATE);
            Frida.green = false;
        }
        // the collider hall: its ring, the hole and its jet, the chargers
        if (indoors.collider) Collider.live(d, Indoors.FLOOR, time, cherry, false, indoors.shiftBack);
        if (indoors.lobby && (dispatch == null || indoors.holds(eyeNow[0], eyeNow[2]))) {
            // the lobby's brums; the box in the counter's hands while it is for her, then on its
            // way from them to hers, up a little on the way
            boolean forHer = lab != null && lab.step <= Lab.HAND;
            LabTop.lobby(d, indoors, time, forHer && lab.giving() < 1f, handsAt);
            float face = LabTop.clerkYaw(indoors);
            if (forHer && lab.giving() <= 0f) LabTop.box(d, handsAt[0], handsAt[1], handsAt[2], face, 0f);
            else if (forHer && lab.giving() < 1f) {
                float a = lab.giving(), e = a * a * (3f - 2f * a);
                r.bone("hand_l", handTo);
                float x = handsAt[0] + (handTo[0] - handsAt[0]) * e, z = handsAt[2] + (handTo[2] - handsAt[2]) * e;
                float y = handsAt[1] + (handTo[1] - LabTop.BOX_Y - 0.03f - handsAt[1]) * e + 0.3f * (float) Math.sin(Math.PI * a);
                LabTop.box(d, x, y, z, face + (r.heading - face) * e, 0f);
            }
        }
        if (!r.hidden && !inChair() && (dispatch == null || dispatch.showsHer())) {
            // heated by the jet: her face like coal
            boolean glows = cherry != null && cherry.heat > 0.3f && !Frida.hot;
            if (glows) Frida.hot = true;
            r.draw(d);
            if (glows) Frida.hot = false;
            toss.draw(d, r);
            kit.draw(d, r);
            if (mirror) {
                // and in the mirror: she as the glass shows her, turned about its plane
                Rooms.reflected(indoors, r.x, r.z, mirrorAt);
                float ox = r.x, oz = r.z, oh = r.heading;
                r.x = mirrorAt[0];
                r.z = mirrorAt[1];
                r.heading = mirrorHeading(oh);
                r.draw(d);
                toss.draw(d, r);
            kit.draw(d, r);
                r.x = ox;
                r.z = oz;
                r.heading = oh;
            }
            if (archive != null && archive.carrying()) archive.carried(d, r);
            if (folder != null && folder.carried()) folder.inHand(d, r);
            if (lab != null && lab.carrying()) {
                r.bone("hand_l", handTo);
                LabTop.box(d, handTo[0], handTo[1] - LabTop.BOX_Y - 0.03f, handTo[2], r.heading, 0f);
            }
        }

        }
        if (dispatch != null) dispatch.draw(d, f, h);
        else if (free) freeFlat(d, f);
        else if (concert != null) concertFlat(d, f);
        else if (archive != null) archiveFlat(d, f);
        else if (mend != null) mendFlat(d, f);
        else if (port != null) portFlat(d, f);
        else if (lab != null) labFlat(d, f);
        else if (herd != null) herdFlat(d, f);
        else if (cherry != null) cherryFlat(d, f);
        else if (fault != null) faultFlat(d, f);
        else if (tow != null) towFlat(d, f);
        else if (puppy != null) puppyFlat(d, f);
        else if (rescue != null) rescueFlat(d, f);
        else if (delivery != null) deliveryFlat(d, f);
        else if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        beatsFlat(f);
        speedFlat(f);
        hearts.draw(f, W, h, free && dispatch == null);
        if (dispatch == null) {
            sofaSign(d);
            buttons(d);
        }
        if (dispatch == null && paused) pauseFlat(f);
        swapFlat(f);
        nightFlat(f);
        panelFlat(d, f);
        storyFlat(d, f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // the white of the hall's flash, going off
        if (indoors.collider && whiteOut > 0f) Opening.wash(f, whiteOut / WHITE_OUT);
        meter(f);
    }

    private final float[] eyeHere = new float[5], camAt = new float[5], mirrorAt = new float[2];

    /** A heading as the mirror shows it: turned about its plane (across in the room's frame). */
    private float mirrorHeading(float h) {
        // the frame's across in the town's terms; turning about the plane across it flips that part
        float ax = indoors.wx(1f, 0f) - indoors.wx(0f, 0f), az = indoors.wz(1f, 0f) - indoors.wz(0f, 0f);
        float hx = (float) Math.sin(h), hz = (float) Math.cos(h), along = hx * ax + hz * az;
        return (float) Math.atan2(hx - 2f * along * ax, hz - 2f * along * az);
    }

    /** The eye in a room (in home, far enough to see the town through its windows), kept as camAt. */
    private void camIn(DrawList d, float[] e, float focal, boolean wide) {
        System.arraycopy(e, 0, camAt, 0, 5);
        d.camera(e[0], e[1], e[2], e[3], e[4], focal, wide ? 0.05f : 0.03f, wide ? 600f : 80f);
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
        if (standPortrait != null) {
            // the desktop stand: her alone, for the icon
            java.util.Arrays.fill(d.flat, 0);
            d.hudRows = 0;
            new Opening(host, city).portrait(d, standPortrait[6], standPortrait);
            return;
        }
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
            dispatch.sceneAt = prologue() ? colliderView : null;
            dispatch.eye(r, eyeNow);
            eyeX = eyeNow[0];
            eyeY = eyeNow[1];
            eyeZ = eyeNow[2];
            eyeYaw = eyeNow[3];
            eyePitch = eyeNow[4];
        }
        // the drive up or down the mountain: the eye over it
        if (roadT >= 0f && driving != null) {
            roadEye();
            eyeX = roadEye[0];
            eyeY = roadEye[1];
            eyeZ = roadEye[2];
            eyeYaw = roadEye[3];
            eyePitch = roadEye[4];
        }
        // shaken by a blast
        if (shake > 0f) {
            float a = shake * shake * SHAKE_FAR;
            eyeX += a * (float) Math.sin(time * SHAKE_FAST);
            eyeY += a * (float) Math.sin(time * SHAKE_FAST * 1.37f + 1f);
            eyeZ += a * (float) Math.sin(time * SHAKE_FAST * 0.83f + 2f);
            eyePitch += a * 0.03f * (float) Math.sin(time * SHAKE_FAST * 1.11f);
        }
        // the desktop stand's own eye, put where a shot wants it
        if (standEye != null) {
            eyeX = standEye[0];
            eyeY = standEye[1];
            eyeZ = standEye[2];
            eyeYaw = standEye[3];
            eyePitch = standEye[4];
        }
        // at the crane, a long lens from far off: the field nearly as drawn on a plan, the stacks' sides showing
        float focal = craning && dispatch == null ? W * CRANE_LENS : W * 1.6f;
        // far enough for the mountain over the town
        d.camera(eyeX, eyeY, eyeZ, eyeYaw, eyePitch, dispatch != null ? dispatch.focal(W * 1.6f, W) : focal, 0.05f, 1600f);
        fliers.strokes(d);
        float ex = (float) Math.sin(eyeYaw), ez = (float) Math.cos(eyeYaw);
        city.draw(d, eyeX, eyeZ, ex, ez);
        // home: its rooms are its walls
        if (nearHome(eyeX, eyeZ)) {
            homeRoom().draw(d);
            d.append(homeRoom().cover);
        }
        Mountain.cabins(d, time);
        LabTop.deck(d, time, eyeX, eyeZ);
        labDrawn(d, r);
        Island.yachts(d, time);
        Island.bridge(d);
        Canal.lifts(d, time, eyeX, eyeZ);
        // the way the pickup went on the drive, lit
        if (roadT >= 0f && driving != null) {
            float ru = Math.min(1f, roadT / ROAD_S), re = ru * ru * (3f - 2f * ru), len = Mountain.wayLength();
            if (roadUp) Mountain.wayLit(d, 0f, re * len);
            else Mountain.wayLit(d, (1f - re) * len, len);
            // over the pickup a beacon, to be seen from on high: the higher the eye, the bigger
            float big = 0.6f + 2.4f * (float) Math.pow(Math.sin(Math.PI * ru), 0.6);
            d.box(driving.x, driving.y + 3.5f + big, driving.z, big, big, big, null, 0xFFF1B0, DrawList.GLOW);
        }
        Island.kept(d, time, eyeX, eyeZ);
        scars.draw(d, city, eyeX, eyeZ);
        // the set showing the night at LAB: its hall, deep in the mountain
        if (dispatch != null && dispatch.inScene()) setDrawn(d);
        if (lab != null) lab.draw(d, time, eyeX, eyeZ);
        if (herd != null) herd.draw(d, time, eyeX, eyeZ);
        cars.draw(d, eyeX, eyeZ, ex, ez);
        Crossings.lamps(d, eyeX, eyeZ, ex, ez);
        felled.draw(d, eyeX, eyeZ, ex, ez);
        villagers.draw(d, city, eyeX, eyeZ);
        if (rescue != null) {
            rescue.draw(d, r, eyeX, eyeZ);
        } else if (debris != null) debris.draw(d, r, eyeX, eyeZ);
        if (tow != null) tow.draw(d, time);
        // what the beds of the pickups near her hold
        if (duty != null) for (Pickup p : duty.all) {
            float dx = p.x - runner.x, dz = p.z - runner.z;
            if (!p.boat && dx * dx + dz * dz < 45f * 45f) kit.inBed(d, p, tow != null, rescue != null && rescue.wanting());
        }
        shore.draw(d, city, eyeX, eyeZ, time);
        if (archive != null) archive.draw(d, r);
        else if (mend != null) mend.draw(d, r, time);
        else if (parked != null) Archive.drawCopter(d, parked, parked.broken ? Archive.AT_CONTROLS : Archive.NOBODY);
        quay.draw(d, craning);
        if (towed != null) {
            // the brum's pickup left at the workshop, till she is well away from it
            float tx = towed.stuck.x - r.x, tz = towed.stuck.z - r.z;
            if (tx * tx + tz * tz > 150f * 150f) towed = null;
            else towed.draw(d, time);
        }
        if (puppy != null) {
            if (puppy.entering()) clinicDoors(d);
            puppy.draw(d, r, driving);
            // the crowd at the water's edge, as long as the job goes on
            if (!puppy.over()) {
                if (crowd == null) crowd = new Crowd(city, Puppy.AT_X, Puppy.AT_X, Puppy.AT_Z);
                crowd.draw(d, eyeX, eyeZ);
            }
        }
        // in her head, the pickup she sits in is not seen (its lamp would be in the room)
        duty.draw(d, eyeX, eyeZ, ex, ez, time, dispatch != null && !dispatch.showsHer() ? driving : null);
        walkers.draw(d, eyeX, eyeZ, ex, ez);
        pets.draw(d, eyeX, eyeZ, ex, ez, walkers);
        if ((dispatch == null || dispatch.showsHer()) && !r.hidden) {
            r.draw(d);
            toss.draw(d, r);
            kit.draw(d, r);
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
        else if (craning) craneFlat(d, f);
        else if (free) freeFlat(d, f);
        else if (concert != null) concertFlat(d, f);
        else if (archive != null) archiveFlat(d, f);
        else if (mend != null) mendFlat(d, f);
        else if (port != null) portFlat(d, f);
        else if (lab != null) labFlat(d, f);
        else if (herd != null) herdFlat(d, f);
        else if (cherry != null) cherryFlat(d, f);
        else if (fault != null) faultFlat(d, f);
        else if (tow != null) towFlat(d, f);
        else if (puppy != null) puppyFlat(d, f);
        else if (rescue != null) rescueFlat(d, f);
        else if (delivery != null) deliveryFlat(d, f);
        else if (folder != null) folderFlat(d, f);
        else chaseFlat(d, f);
        beatsFlat(f);
        speedFlat(f);
        navFlat(f);
        hearts.draw(f, W, h, free && dispatch == null);
        if (dispatch == null) {
            if (driving == null) signs(d);
            buttons(d);
            if (paused && TWO_THUMBS_OFFERED) toggleFlat(f);
            if (paused) pauseFlat(f);
        }
        swapFlat(f);
        nightFlat(f);
        panelFlat(d, f);
        storyFlat(d, f);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // the white the opening ended on, going off
        if (whiteOut > 0f) Opening.wash(f, whiteOut / WHITE_OUT);
        meter(f);
    }
}
