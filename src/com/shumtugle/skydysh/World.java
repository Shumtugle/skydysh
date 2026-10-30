package com.shumtugle.skydysh;

/**
 * The game, apart from the phone: what is where, what moves, what the frame should hold.
 * It never draws: each frame it fills a DrawList, and a renderer draws that. The shell
 * calls, always from one thread: touch() for each queued touch, update(dt), then draw().
 * Coordinates of touches are frame pixels, W across.
 *
 * A southern town by the sea on a summer evening, its streets grown without end as they are
 * come to (see City), and Freedie running through it (see Runner).
 */
final class World {
    static final int W = 274;                // frame units across; the height follows the screen
    static final String VERSION = "0.4";     // shown on screen; keep in step with versionName
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
    private final Fliers fliers = new Fliers();                     // far off in the sky, seldom
    private Px flatPx;

    World(Host host) {
        this.host = host;
        byte[] moves = host.asset("moves.bin");
        if (moves != null) Frida.use(Moves.read(moves));
    }

    void resize(int screenW, int screenH) {
        h = Math.max(1, Math.round(W * (float) screenH / Math.max(1, screenW)));
    }

    void touch(int type, float x, float y) {
        if (type == BACK) return;
        runner.touch(type, x, y);
    }

    void update(float dt) {
        time += dt;
        runner.update(dt, city);
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

    void draw(DrawList d) {
        d.begin(W, h);
        d.time = time;
        Runner r = runner;
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
        city.draw(d, r.eyeX, r.eyeZ, (float) Math.sin(r.eyeYaw), (float) Math.cos(r.eyeYaw));
        r.draw(d);
        PixelFont.draw(f, VERSION, W - PixelFont.width(VERSION, 1) - 3, h - 10, 1, 0x88FFFFFF);
        // while the engine is young: how long a frame takes, in milliseconds, and how finely it is drawn
        if (frameMs > 0f) {
            int qs = Math.round(drawScale * 100f);
            String meter = Math.round(frameMs) + " " + (qs / 100) + "." + (qs % 100 < 10 ? "0" : "") + (qs % 100);
            PixelFont.draw(f, meter, 3, h - 10, 1, 0x88FFFFFF);
        }
    }
}
