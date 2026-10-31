package com.shumtugle.skydysh;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * The desktop stand: plays a scene file against the world on an ordinary JVM, and for each
 * shot writes the frame's draw list (name.json) and its flat layer (name.flat.png). The
 * page in stand/web draws them with the game's own shaders.
 *
 * Scene commands, one a line: screen W H · wait SECONDS · down X Y · move X Y · up X Y ·
 * drag X0 Y0 X1 Y1 SECONDS · shot NAME · skip (kept for old scenes: there is no opening) ·
 * sounds (every sound of the synthesizer in turn, each after the last has died away) ·
 * keep KEY VALUE (as kept by an earlier game) · back (the phone's back key) · restart (the
 * game started again, with what it kept) · deck VERDICT (the room between rounds at once).
 * Coordinates are frame pixels. # starts a comment. Every sound the world asks for is
 * mixed, as the phone would mix it, into sound.wav.
 */
public final class Stand implements World.Host {
    private static final float DT = 1f / 60f;
    private final java.util.ArrayList<float[]> sounds = new java.util.ArrayList<float[]>();
    private final java.util.HashMap<String, Integer> kept = new java.util.HashMap<String, Integer>();
    private World world = fresh();

    /** A new game, the opening skipped: the scenes are of the game itself unless they ask for it. */
    private World fresh() {
        World w = new World(this);
        w.skipOpening();
        return w;
    }
    private final DrawList list = new DrawList();
    private final File out;
    private float time;
    private String shotDown;

    private Stand(File out) {
        this.out = out;
        world.resize(1096, 2560);
    }

    public static void main(String[] args) throws IOException {
        File scene = new File(args.length > 0 ? args[0] : "stand/scene.txt");
        File out = new File(args.length > 1 ? args[1] : "out/stand");
        out.mkdirs();
        Stand s = new Stand(out);
        List<String> lines = Files.readAllLines(scene.toPath(), StandardCharsets.UTF_8);
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            s.exec(line.split("\\s+"));
        }
        s.writeSound();
        System.out.println("done: " + String.format("%.1f", s.time) + "s of game time, " + s.sounds.size() + " sounds");
    }

    @Override
    public void sound(int id, float gain, float rate) {
        sounds.add(new float[]{time, id, gain, rate});
    }


    @Override
    public void buzz(int ms) {
    }

    @Override
    public void saveInt(String key, int value) {
        kept.put(key, value);
        System.out.println(String.format("%7.2fs  kept %s = %d", time, key, value));
    }

    @Override
    public int loadInt(String key) {
        Integer v = kept.get(key);
        return v == null ? 0 : v;
    }

    @Override
    public void journal(String name, String text) {
        try {
            Files.write(new File(out, name).toPath(), text.getBytes(StandardCharsets.UTF_8));
            System.out.println(String.format("%7.2fs  journal %s", time, name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String lang = "en";

    @Override
    public String language() {
        return lang;
    }

    @Override
    public byte[] asset(String name) {
        try {
            return Files.readAllBytes(new File("assets", name).toPath());
        } catch (IOException e) {
            return null;
        }
    }

    /** All the sounds of the scene, mixed as the phone would, into sound.wav. */
    private void writeSound() throws IOException {
        Synth syn = new Synth();
        int total = (int) ((time + 2.5f) * Synth.RATE);
        short[] pcm = new short[total];
        short[] chunk = new short[256];
        int si = 0;
        for (int pos = 0; pos < total; pos += chunk.length) {
            float t = pos / (float) Synth.RATE;
            while (si < sounds.size() && sounds.get(si)[0] <= t) {
                float[] e = sounds.get(si++);
                syn.play((int) e[1], e[2], e[3]);
            }
            int n = Math.min(chunk.length, total - pos);
            syn.mix(chunk, n);
            System.arraycopy(chunk, 0, pcm, pos, n);
        }
        java.io.DataOutputStream o = new java.io.DataOutputStream(new java.io.FileOutputStream(new File(out, "sound.wav")));
        int bytes = total * 2;
        o.writeBytes("RIFF");
        o.writeInt(Integer.reverseBytes(36 + bytes));
        o.writeBytes("WAVEfmt ");
        o.writeInt(Integer.reverseBytes(16));
        o.writeShort(Short.reverseBytes((short) 1));
        o.writeShort(Short.reverseBytes((short) 1));
        o.writeInt(Integer.reverseBytes(Synth.RATE));
        o.writeInt(Integer.reverseBytes(Synth.RATE * 2));
        o.writeShort(Short.reverseBytes((short) 2));
        o.writeShort(Short.reverseBytes((short) 16));
        o.writeBytes("data");
        o.writeInt(Integer.reverseBytes(bytes));
        for (short v : pcm) o.writeShort(Short.reverseBytes(v));
        o.close();
    }

    private void step() {
        world.update(DT);
        // drawn every step, as the phone does: the world learns from it where things are on screen
        world.draw(list);
        time += DT;
    }

    private void run(float seconds) {
        int n = Math.round(seconds / DT);
        for (int i = 0; i < n; i++) step();
    }

    private static float f(String s) {
        return Float.parseFloat(s);
    }

    private void exec(String[] p) throws IOException {
        String c = p[0];
        if (c.equals("screen")) {
            world.resize(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        } else if (c.equals("wait")) {
            run(f(p[1]));
        } else if (c.equals("down")) {
            world.touch(World.DOWN, f(p[1]), f(p[2]));
            step();
        } else if (c.equals("move")) {
            world.touch(World.MOVE, f(p[1]), f(p[2]));
            step();
        } else if (c.equals("up")) {
            world.touch(World.UP, f(p[1]), f(p[2]));
            step();
        } else if (c.equals("drag")) {
            float x0 = f(p[1]), y0 = f(p[2]), x1 = f(p[3]), y1 = f(p[4]);
            int n = Math.max(2, Math.round(f(p[5]) / DT));
            world.touch(World.DOWN, x0, y0);
            step();
            for (int i = 1; i <= n; i++) {
                world.touch(World.MOVE, x0 + (x1 - x0) * i / n, y0 + (y1 - y0) * i / n);
                step();
            }
            world.touch(World.UP, x1, y1);
            step();
        } else if (c.equals("sounds")) {
            Synth syn = new Synth();
            for (int id = 0; id < syn.bank.length; id++) {
                if (syn.bank[id] == null) continue;
                sound(id, 0.9f, 1f);
                System.out.println(String.format("%7.2fs  sound %d", time, id));
                run(syn.bank[id].length / (float) Synth.RATE + 0.45f);
            }
        } else if (c.equals("keep")) {
            // keep KEY VALUE: as if a game before had kept it
            saveInt(p[1], Integer.parseInt(p[2]));
        } else if (c.equals("restart")) {
            // the game started again, with what it kept
            world = new World(this);
            world.skipOpening();
            System.out.println(String.format("%7.2fs  started again at mission %d", time, world.mission.round));
            world.resize(1096, 2560);
        } else if (c.equals("back")) {
            world.touch(World.BACK, 0f, 0f);
            step();
        } else if (c.equals("tap")) {
            // tap X Y: a finger down and up again at once
            world.touch(World.DOWN, f(p[1]), f(p[2]));
            step();
            world.touch(World.UP, f(p[1]), f(p[2]));
            step();
        } else if (c.equals("carahead")) {
            // carahead METERS: a car standing across her way so far ahead
            world.cars.standAcross(world.runner, f(p[1]));
        } else if (c.equals("roof")) {
            // roof: her put on the roof of the house nearest, at its middle, facing along its long side
            float[] at = new float[3], half = new float[2];
            Runner r = world.runner;
            if (world.city.roofNear(r.x, r.z, at, half))
                r.put(at[0], at[1] + 0.5f, at[2], half[0] > half[1] ? (float) Math.PI * 0.5f : 0f);
            step();
        } else if (c.equals("face")) {
            // face RADIANS: her turned so
            Runner r = world.runner;
            r.put(r.x, r.y, r.z, f(p[1]));
            step();
        } else if (c.equals("lang")) {
            // lang CODE: the game speaks this language from now on
            lang = p[1];
            Lang.use(this, lang);
        } else if (c.equals("dispatch")) {
            // dispatch [done]: the dispatcher's room now, with the briefing, or the report
            world.standDispatch(p.length > 1 && p[1].equals("done"));
        } else if (c.equals("opening")) {
            // the opening played from its start
            world.startOpening();
        } else if (c.equals("skip")) {
            // the opening skipped, as a touch does
            world.touch(World.DOWN, 1f, 1f);
            world.touch(World.UP, 1f, 1f);
        } else if (c.equals("birds")) {
            // a flock across the far end of the street now
            world.standFliers(1, 0f);
            step();
        } else if (c.equals("plane")) {
            // plane SECONDS: a plane high up that has flown so long, its trail behind it
            world.standFliers(2, f(p[1]));
            step();
        } else if (c.equals("chasebot")) {
            // chasebot SECONDS [SHOTEVERY PREFIX [once]]: a finger that runs after the nearest golem as a player
            // would, turning toward her by sideways moves of its own, never more than a hand's
            // width a frame
            float secs = f(p[1]), every = p.length > 2 ? f(p[2]) : 0f;
            String prefix = p.length > 3 ? p[3] : "b";
            float fx = World.W * 0.5f, fy = 600f, next = every;
            world.touch(World.DOWN, fx, fy);
            int n = Math.round(secs / DT), k = 0;
            // the way she went, dropped behind her as she runs; the finger follows it, crumb by crumb
            java.util.ArrayDeque<float[]> crumbs = new java.util.ArrayDeque<float[]>();
            float dropAt = 0f;
            Chase after = null;
            int caughtWas = world.mission.caught();
            int wi = 0, wj = 0;
            boolean routing = false;
            float stuck = 0f, dodge = 0f, firstAt = -1f, lastAt = -1f, dodgeSide = 1f;
            int dodges = 0;
            int nextAfter = 0, nextThrow = 0;
            float thrownAt = -1f;
            int thrownSeen = world.toss.thrown;
            // chasebot ... once: the finger lifted as soon as one is caught
            boolean once = p.length > 4 && p[4].equals("once");
            boolean endShot = false;
            for (int i = 0; i < n; i++) {
                Runner r = world.runner;
                Chase ch = world.chase;
                if (ch != after) {
                    // another one to go after: its trail, not the last one's
                    crumbs.clear();
                    after = ch;
                    routing = false;
                }
                if (i * DT >= dropAt) {
                    crumbs.add(new float[]{ch.x, ch.z});
                    dropAt += 0.25f;
                }
                while (crumbs.size() > 1) {
                    float[] c0 = crumbs.peek();
                    float ddx = c0[0] - r.x, ddz = c0[1] - r.z;
                    if (ddx * ddx + ddz * ddz < 16f) crumbs.poll();
                    else break;
                }
                float[] goal;
                // one waiting is gone to along the streets until it is down the same street as her
                boolean sameStreet = Math.abs(r.x - ch.x) < 8f || Math.abs(r.z - ch.z) < 8f;
                if (!ch.done() && (ch.apart > 60f || (ch.state == Chase.WAITING && !sameStreet))) {
                    // far off: along the streets, crossing to crossing, the longer way first
                    if (!routing) {
                        wi = Math.round(r.x / City.CELL);
                        wj = Math.round(r.z / City.CELL);
                        routing = true;
                    }
                    float gx = wi * City.CELL, gz = wj * City.CELL;
                    if ((gx - r.x) * (gx - r.x) + (gz - r.z) * (gz - r.z) < 36f) {
                        int ti = Math.round(ch.x / City.CELL), tj = Math.round(ch.z / City.CELL);
                        if (Math.abs(ti - wi) >= Math.abs(tj - wj)) wi += Integer.signum(ti - wi);
                        else wj += Integer.signum(tj - wj);
                    }
                    // down its own side of the street, as the traffic goes, clear of the parked cars
                    gx = wi * City.CELL;
                    gz = wj * City.CELL;
                    if (Math.abs(gx - r.x) > Math.abs(gz - r.z)) {
                        float sx = Math.signum(gx - r.x);
                        goal = new float[]{r.x + Math.max(-10f, Math.min(10f, gx - r.x)), gz - sx * 1.6f};
                    } else {
                        float sz = Math.signum(gz - r.z);
                        goal = new float[]{gx + sz * 1.6f, r.z + Math.max(-10f, Math.min(10f, gz - r.z))};
                    }
                    crumbs.clear();
                } else {
                    routing = false;
                    goal = ch.apart < 12f || crumbs.isEmpty() ? new float[]{ch.x, ch.z} : crumbs.peek();
                }
                // held up (a car, a lamp post): a step aside to the right for a moment
                stuck = r.speed < 1f ? stuck + DT : 0f;
                if (stuck > 0.6f) {
                    // a step aside, to the right and the left by turns, longer each time it fails
                    dodge = 1.2f + 0.4f * (dodges % 4);
                    dodgeSide = (dodges++ % 2 == 0) ? 1f : -1f;
                    stuck = 0f;
                }
                if (dodge > 0f) {
                    dodge -= DT;
                    goal = new float[]{r.x + dodgeSide * (float) Math.cos(r.heading) * 4f + (float) Math.sin(r.heading),
                            r.z - dodgeSide * (float) Math.sin(r.heading) * 4f + (float) Math.cos(r.heading)};
                } else if (r.speed > 5f) dodges = 0;
                float want = (float) Math.atan2(goal[0] - r.x, goal[1] - r.z);
                float dd = want - r.heading;
                while (dd > Math.PI) dd -= 2f * (float) Math.PI;
                while (dd < -Math.PI) dd += 2f * (float) Math.PI;
                float px = Math.max(-6f, Math.min(6f, dd / (float) (Math.PI / 2) * Runner.QUARTER));
                fx += px;
                if (fx < 20f || fx > World.W - 20f) {
                    // the edge of the glass: lift and put the finger back in the middle
                    world.touch(World.UP, fx, fy);
                    fx = World.W * 0.5f;
                    world.touch(World.DOWN, fx, fy);
                }
                world.touch(World.MOVE, fx, fy);
                int was = world.chase.struck;
                step();
                int got = world.mission.caught();
                if (world.chase.struck != was)
                    System.out.println(String.format("%7.2fs  struck by a car, %.0f m off", time, world.chase.apart));
                if (world.toss.thrown != thrownSeen) {
                    thrownSeen = world.toss.thrown;
                    System.out.println(String.format("%7.2fs  hammer flies, %.1f m", time, world.chase.apart));
                }
                if (thrownAt < 0f && world.chase.seized()) {
                    // shots from the moment one is held: the stop, the swing, the mallet out
                    thrownAt = time + 0.33f;
                    System.out.println(String.format("%7.2fs  hammer thrown, %.1f m off", time, world.chase.apart));
                }
                float[] afterThrow = {-0.3f, -0.12f, 0.06f};
                if (thrownAt >= 0f && nextThrow < afterThrow.length && time - thrownAt >= afterThrow[nextThrow])
                    shot(prefix + "t" + nextThrow++);
                if (once && got > 0 && nextThrow >= afterThrow.length) {
                    caughtWas = got;
                    break;
                }
                if (got != caughtWas) {
                    System.out.println(String.format("%7.2fs  caught %d of %d (clock %.1f)", time, got, Mission.FIVE, world.mission.clock));
                    if (got == 1) firstAt = time;
                    if (got == Mission.FIVE) lastAt = time;
                }
                // the first one caught looked at as it falls apart, and the end of the hunt
                float[] after1 = {0.1f, 0.4f, 1.0f, 2.2f, 5f};
                if (firstAt >= 0f && nextAfter < after1.length && time - firstAt >= after1[nextAfter])
                    shot(prefix + "c" + nextAfter++);
                if (lastAt >= 0f && !endShot && time - lastAt >= 1.5f) {
                    shot(prefix + "end");
                    endShot = true;
                }
                caughtWas = got;
                if (System.getenv("HUNTLOG") != null && i % 300 == 0)
                    System.out.println(String.format("%7.2fs  target %.0f m, state %d, speed %.1f, runner %.1f %s at %.1f %.1f wp %d %d h %.2f",
                            time, world.chase.apart, world.chase.state, world.chase.speed, world.runner.speed, routing ? "routing" : "trail", world.runner.x, world.runner.z, wi, wj, world.runner.heading));
                if (every > 0f && i * DT >= next) {
                    shot(prefix + (k++));
                    next += every;
                }
            }
            world.touch(World.UP, fx, fy);
        } else if (c.equals("golemahead")) {
            // the nearest golem put running down the street ahead of the start, as the chase once began
            world.chase.standAhead();
            step();
        } else if (c.equals("carbehind")) {
            // carbehind METERS SPEED SECONDS EVERY PREFIX: a car put behind her in her lane and
            // driven on at SPEED until it strikes her; the scene run on so long, shot so often
            world.cars.standBehind(world.chase, f(p[1]));
            float rush = f(p[2]), secs = f(p[3]), every = f(p[4]), next = 0f;
            int was = world.chase.struck, n = Math.round(secs / DT), k = 0;
            for (int i = 0; i < n; i++) {
                if (world.chase.struck == was) world.cars.speed[0] = rush;
                step();
                if (world.chase.struck != was && next == 0f) {
                    System.out.println(String.format("%7.2fs  struck by a car, %.0f m off", time, world.chase.apart));
                    next = 0.001f;
                }
                if (next > 0f && (next += DT) >= every) {
                    shot(p[5] + (k++));
                    next = 0.001f;
                }
            }
        } else if (c.equals("shot")) {
            shot(p[1]);
        } else {
            throw new IllegalArgumentException("unknown command: " + c);
        }
    }

    private void shot(String name) throws IOException {
        world.draw(list);
        Files.write(new File(out, name + ".json").toPath(), list.json().getBytes(StandardCharsets.UTF_8));
        BufferedImage img = new BufferedImage(list.w, list.h, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, list.w, list.h, list.flat, 0, list.w);
        ImageIO.write(img, "png", new File(out, name + ".flat.png"));
        // the fine layer over the top (the crest), when the frame has one: its pixels as they are, premultiplied
        if (list.hudRows > 0 && list.hud != null) {
            BufferedImage hi = new BufferedImage(list.hudW, list.hudH, BufferedImage.TYPE_INT_ARGB);
            hi.setRGB(0, 0, list.hudW, list.hudH, list.hud, 0, list.hudW);
            ImageIO.write(hi, "png", new File(out, name + ".hud.png"));
        }
        System.out.println(String.format("%7.2fs  %s (%d boxes)", time, name, list.count));
    }
}
