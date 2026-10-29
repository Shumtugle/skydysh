package com.shumtugle.skydysh;

/**
 * The game, apart from the phone: what is where, what moves, what the frame should hold.
 * It never draws: each frame it fills a DrawList, and a renderer draws that. The shell
 * calls, always from one thread: touch() for each queued touch, update(dt), then draw().
 * Coordinates of touches are frame pixels, W across.
 *
 * The street of a southern town by the sea, one long summer evening: houses, palms, the sea
 * beyond the promenade, cars parked and cars coming, people about; and on the plinth in the
 * middle of the road a building, with the hammer in hand to knock it down (see Game).
 */
final class World {
    static final int W = 274;                // frame units across; the height follows the screen
    static final String VERSION = "0.2";     // shown on screen; keep in step with versionName
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
        /** A round written down (see Journal): kept as a plain text by this name, in a folder of its own. */
        void journal(String name, String text);
    }

    // the eye: left of the plinth, at a person's height
    static final float CAM_X = -2.3f, CAM_Y = 1.8f, CAM_Z = 0f;
    static final float PLINTH_Z = 11.5f;

    private final Host host;
    int h = 640;
    float time;
    // the time the street keeps: it stands still while the room between rounds is closed over it
    float sceneTime;
    float frameMs;                           // how long the last frame took, set by the shell
    float drawScale;                         // drawn pixels to a frame pixel, set by the shell
    private final Game game;                  // the building on the plinth, and the hammer
    private Ink hudInk, roomInk;
    private final People people = new People(20, 0x5EE7L, 1.2f);   // up and down the street, and right by the eye
    private final Fliers fliers = new Fliers();                     // far off in the sky, seldom

    private final DrawList still = new DrawList();   // the street, built once
    private boolean built;
    private final Traffic traffic = new Traffic(PLINTH_Z);
    // what cannot be walked or driven through: the street's own things, and what lies about
    private final Solid solid = new Solid();
    private final float[] view = new float[3];
    private float lightPlate = -1f, frameDt;

    // the level being played, from 0
    int level;

    World(Host host) {
        this.host = host;
        game = new Game(host, host.loadInt("level"));
        game.traffic = traffic;
        game.solid = solid;
        people.solid = solid;
        level = game.level;
        // along the pavement by the curb and the promenade's near edge, up and down the whole
        // street and right past the eye; nobody on the road
        java.util.Random r = people.random();
        for (int i = 0; i < 20; i++) {
            float side = (i & 1) == 0 ? 1f : -1f;
            float x = side * (4.8f + r.nextFloat() * (side > 0 ? 4.6f : 2.0f));
            float z0 = -4f + r.nextFloat() * 40f, z1 = Math.min(82f, z0 + 30f + r.nextFloat() * 30f);
            people.walk(x, z0, x + (r.nextFloat() - 0.5f) * 0.8f, z1, Street.WALK_TOP);
        }
    }

    void resize(int screenW, int screenH) {
        h = Math.max(1, Math.round(W * (float) screenH / Math.max(1, screenW)));
    }

    void touch(int type, float x, float y) {
        if (type == BACK) return;
        game.touch(type, x, y);
    }

    void update(float dt) {
        time += dt;
        frameDt = dt;
        // the street on the set is a still: while the room is all closed in, nothing on it moves
        boolean still = game.tilt() >= 1f;
        if (!still) {
            sceneTime += dt;
            traffic.update(dt);
        }
        // birds and planes keep their own time, counted only while the street is in sight
        fliers.update(dt, game.tilt() == 0f);
        game.update(dt);
        level = game.level;
    }

    /** Whether the back key means something here, rather than leaving the game. */
    boolean wantsBack() {
        return false;
    }

    /** For the desktop stand: the cars on the move, the people's solids. */
    Traffic standTraffic() {
        return traffic;
    }

    /** For the desktop stand: whether the hammer is in hand. */
    boolean standHandFree() {
        return game.hs == Game.HAND;
    }

    /** For the desktop stand: a flock over the street now (1), or a plane that has flown so many seconds. */
    void standFliers(int what, float seconds) {
        if (what == 1) fliers.standFlock();
        else fliers.standPlane(seconds);
    }

    /** For the desktop stand: the room between rounds at once, as after a round with that verdict. */
    void standDeck(int verdict) {
        game.standDeck(verdict);
    }

    /** For the desktop stand: every mattress torn, every suitcase burst open. */
    void standTear() {
        game.standTear();
    }

    void draw(DrawList d) {
        if (!built) {
            Street.build(still, solid);
            built = true;
        }
        d.begin(W, h);
        d.time = sceneTime;
        light(d);
        // the hour of the evening the level is played at
        Daylight.light(d, plate());
        java.util.Arrays.fill(d.flat, 0);
        Px f = flat(d);
        // the fine layer over the top, for the corner: kept from frame to frame (redrawn only when
        // something on it changes), handed over only if it is shown
        if (hudInk == null || hudInk.w != W * Crest.K) hudInk = new Ink(W * Crest.K, Crest.ROWS * Crest.K);
        hudInk.used = false;
        d.hud = hudInk.a;
        d.hudW = hudInk.w;
        d.hudH = hudInk.h;
        d.hudRows = 0;
        game.hudInk = hudInk;
        if (roomInk == null || roomInk.w != W * Console.Q || roomInk.h != h * Console.Q) roomInk = new Ink(W * Console.Q, h * Console.Q);
        roomInk.used = false;
        game.roomInk = roomInk;
        float yaw = (float) Math.atan2(-CAM_X, PLINTH_Z);
        // as the room closes in, the eye drops a little to the plinth, so it sits on the set
        // (a little down at rest, so the road fills the lower half of the frame)
        float pitch = -0.07f - 0.2f * game.tilt();
        // far plinths: under an aim the view closes in on the building
        game.view(yaw, pitch, view);
        yaw = view[0];
        pitch = view[1];
        // as the room closes in over the street, the street draws back a little into the set
        float wide = 1f - 0.15f * game.tilt();
        d.camera(CAM_X, CAM_Y, CAM_Z, yaw, pitch, W * 1.9f * view[2] * wide, 0.1f, 500f);
        // closed in, the haze thins as if the building had come nearer: it stays readable far off
        d.fogStart *= view[2];
        d.fogScale *= view[2];
        game.frame(d);
        fliers.strokes(d);
        d.append(still);
        if (d.night > 0f) lamps(d);
        traffic.draw(d);
        people.draw(d, sceneTime, CAM_X, CAM_Y, CAM_Z);
        game.draw(d);
        // the flat layer: the aim, the version in the corner
        game.flat(f);
        game.aim(d);
        if (roomInk.used) {
            // the room between rounds, on the fine layer over all of the frame
            d.hud = roomInk.a;
            d.hudW = roomInk.w;
            d.hudH = roomInk.h;
            d.hudRows = h;
            d.hudFresh = true;
        } else if (hudInk.used) {
            d.hudRows = Crest.ROWS;
            d.hudFresh = hudInk.fresh;
            hudInk.fresh = false;
        }
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // while the engine is young: how long a frame takes, in milliseconds, and how finely it is drawn
        if (frameMs > 0f) {
            int qs = Math.round(drawScale * 100f);
            String meter = Math.round(frameMs) + " " + (qs / 100) + "." + (qs % 100 < 10 ? "0" : "") + (qs % 100);
            PixelFont.draw(f, meter, 3, h - 10, 1, 0x88FFFFFF);
        }
    }

    /** The number on the plate of the level being played, eased while one turns into the next, for the light. */
    private float plate() {
        float target = game.level + 1;
        if (lightPlate < 0f || Math.abs(target - lightPlate) > 3f) lightPlate = target;
        // one level to the next, the light moves on in a couple of seconds rather than at once
        lightPlate += (target - lightPlate) * Math.min(1f, frameDt * 1.2f);
        return lightPlate;
    }

    /** The street lamps lit, as night comes: their glass heads glowing warm. */
    private void lamps(DrawList d) {
        int c = Street.mix(0xF3E2B4, 0xFFC878, Math.min(1f, d.night * 1.4f));
        for (float z = -12f; z < 120f; z += 14f)
            for (int side = -1; side <= 1; side += 2)
                d.box(side * Street.LAMP_X, Street.WALK_TOP + 4.4f, z, 0.135f, 0.185f, 0.135f, null, c, DrawList.GLOW);
    }

    /** The evening: a low sun from the left and a little behind, the sky, the haze. */
    private static void light(DrawList d) {
        d.sun(-0.80f, 0.30f, -0.10f, -1f, 4f, 42f, 100f, 36f, 140f);
        DrawList.rgb(d.skyTop, 0x3C73C0);
        DrawList.rgb(d.skyHorizon, 0xF1DCC4);
        DrawList.rgb(d.skyGlow, 0xF6C08A);
        d.sunColor[0] = 1.6f; d.sunColor[1] = 1.45f; d.sunColor[2] = 1.22f;
        d.ambientSky[0] = 0.35f; d.ambientSky[1] = 0.39f; d.ambientSky[2] = 0.49f;
        d.ambientGround[0] = 0.26f; d.ambientGround[1] = 0.22f; d.ambientGround[2] = 0.19f;
        DrawList.rgb(d.fog, 0xE9D6BE);
        d.fogStart = 8f;
        d.fogScale = 220f;
        d.night = 0f;
        d.grade[0] = 1f;
        d.grade[1] = 1f;
        d.grade[2] = 0f;
    }

    private Px flatPx;

    private Px flat(DrawList d) {
        if (flatPx == null || flatPx.a != d.flat) flatPx = new Px(d.w, d.h, d.flat);
        return flatPx;
    }
}
