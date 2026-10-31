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
        } else if (c.equals("swipe")) {
            // swipe X Y: a finger down, flicked up quickly, and lifted
            flick(f(p[1]), f(p[2]));
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
        } else if (c.equals("turn")) {
            // turn RADIANS: her turned so much from where she faces
            Runner r = world.runner;
            r.put(r.x, r.y, r.z, r.heading + f(p[1]));
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
                if (world.toss.state == Toss.HELD && !r.inAir() && Toss.pick(r, world.golems()) == ch
                        && ch.apart < THROW_AT && !ch.done()) {
                    // near enough and ahead: a tap, and the finger back down
                    world.touch(World.UP, fx, fy);
                    step();
                    world.touch(World.DOWN, fx, fy);
                    step();
                    world.touch(World.UP, fx, fy);
                    step();
                    world.touch(World.DOWN, fx, fy);
                    System.out.println(String.format("%7.2fs  tap: the mallet, %.1f m off", time, ch.apart));
                    // shots from the moment of the tap: the stop, the swing, the mallet out
                    if (thrownAt < 0f) thrownAt = time + 0.33f;
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
                    System.out.println(String.format("%7.2fs  target %.0f m, state %d, speed %.1f, runner %.1f %s at %.1f %.1f y %.1f wp %d %d h %.2f",
                            time, world.chase.apart, world.chase.state, world.chase.speed, world.runner.speed, routing ? "routing" : "trail", world.runner.x, world.runner.z, world.runner.y, wi, wj, world.runner.heading));
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
        } else if (c.equals("mission")) {
            // mission N: mission N now, round where she is
            world.standMission(Integer.parseInt(p[1]));
            Folder fo = world.folder;
            if (fo != null)
                System.out.println(String.format("%7.2fs  mission %d: the folder at %.0f %.1f %.0f, %.0f m off", time,
                        world.mission.round, fo.x, fo.y, fo.z, fo.apart(world.runner)));
        } else if (c.equals("nearfolder")) {
            // nearfolder [METERS [RADIANS]]: her put on the street before the lift nearest the folder,
            // so far out (6 m), facing its door, or turned so much from it
            float[] l = doorNearFolder();
            float out = p.length > 1 ? f(p[1]) : 6f, turn = p.length > 2 ? f(p[2]) : 0f;
            world.runner.put(l[0] + l[2] * out, City.WALK_TOP, l[1] + l[3] * out, (float) Math.atan2(-l[2], -l[3]) + turn);
            step();
        } else if (c.equals("fuzz")) {
            // fuzz SECONDS SEED: a careless finger: down, about, up, taps and flicks at random
            java.util.Random fr = new java.util.Random(Long.parseLong(p[2]));
            int n = Math.round(f(p[1]) / DT);
            float fx = World.W * 0.5f, fy = 500f;
            boolean down = false;
            for (int i = 0; i < n; i++) {
                int what = fr.nextInt(40);
                if (!down && what < 3) {
                    fx = 20f + fr.nextFloat() * (World.W - 40f);
                    fy = 100f + fr.nextFloat() * 500f;
                    world.touch(World.DOWN, fx, fy);
                    down = true;
                } else if (down && what < 2) {
                    world.touch(World.UP, fx, fy);
                    down = false;
                } else if (down && what < 14) {
                    fx += (fr.nextFloat() - 0.5f) * 30f;
                    fy += (fr.nextFloat() - 0.6f) * 40f;
                    world.touch(World.MOVE, fx, fy);
                }
                step();
            }
            if (down) world.touch(World.UP, fx, fy);
            System.out.println(String.format("%7.2fs  fuzz done, mission %d, her %.0f %.1f %.0f", time, world.mission.round,
                    world.runner.x, world.runner.y, world.runner.z));
        } else if (c.equals("doorlog")) {
            // doorlog SECONDS: the lift door nearest her told every tenth of a second (how far, which)
            int n = Math.round(f(p[1]) / DT);
            float[] l = new float[11], h = new float[8];
            for (int i = 0; i < n; i++) {
                step();
                if (i % 6 != 0) continue;
                Runner r = world.runner;
                boolean near = world.city.liftNear(r.x, r.z, r.y, 3f, l, h);
                System.out.println(String.format("%7.2fs  her %.2f %.2f %.2f h %.2f v %.2f air %b near %b door %.2f %.2f back %.0f face %.0f %.0f %s",
                        time, r.x, r.y, r.z, r.heading, r.speed, r.inAir(), near, l[0], l[1], l[10], l[2], l[3], world.indoors != null ? "IN" : ""));
            }
        } else if (c.equals("yardhouse")) {
            // yardhouse: her standing at the front door of the nearest house with a lift whose back looks on its yard
            Runner r = world.runner;
            float[] h = new float[8], l = new float[10], best = null;
            float bd = 1e9f;
            int ci = City.index(r.x), cj = City.index(r.z);
            for (int j = cj - 2; j <= cj + 2; j++)
                for (int i = ci - 2; i <= ci + 2; i++)
                    for (int k = 0; k < world.city.houses(i, j); k++) {
                        world.city.house(i, j, k, h);
                        if (h[7] != City.LIFT || !world.city.backClear(h, 0)) continue;
                        City.lift(h, 0, l);
                        float dd = (l[0] - r.x) * (l[0] - r.x) + (l[1] - r.z) * (l[1] - r.z);
                        if (dd < bd) {
                            bd = dd;
                            best = l.clone();
                        }
                    }
            r.put(best[0] + best[2] * 0.6f, City.WALK_TOP, best[1] + best[3] * 0.6f, (float) Math.atan2(-best[2], -best[3]) + 1.5708f);
            step();
        } else if (c.equals("folkrow")) {
            if (p[1].equals("off")) rowFirst = -1;
            else {
                rowFirst = Integer.parseInt(p[1]);
                rowMotion = p.length > 2 ? p[2] : "idle";
                rowT = p.length > 3 ? f(p[3]) : 0f;
                rowYaw = p.length > 4 ? f(p[4]) : (float) Math.PI * 0.5f - 0.35f;
            }
        } else if (c.equals("ladder")) {
            // ladder street|roof METERS: her put so far back from the nearest ladder whose foot is
            // on the pavement (a fire escape) or on a roof (up to the next roof), facing it
            Runner r = world.runner;
            float[] l = new float[6], best = null;
            float bd = 1e9f;
            boolean street = p[1].equals("street");
            for (float feet = street ? City.WALK_TOP : 5f; feet <= (street ? City.WALK_TOP : 40f); feet += 0.5f)
                if (world.city.ladderNear(r.x, r.z, feet, 120f, l)) {
                    float dd = (l[0] - r.x) * (l[0] - r.x) + (l[1] - r.z) * (l[1] - r.z);
                    if (dd < bd) {
                        bd = dd;
                        best = l.clone();
                    }
                }
            float back = f(p[2]);
            r.put(best[0] + best[4] * back, best[2], best[1] + best[5] * back, (float) Math.atan2(-best[4], -best[5]));
            System.out.println(String.format("%7.2fs  a ladder at %.1f %.1f, from %.1f to %.1f m, facing %.0f %.0f", time,
                    best[0], best[1], best[2], best[3], best[4], best[5]));
            step();
        } else if (c.equals("where")) {
            // where: her place, the eye's, and whether she is indoors, told
            Runner r = world.runner;
            System.out.println(String.format("%7.2fs  her %.1f %.1f %.1f, eye %.1f %.1f %.1f, %s", time, r.x, r.y, r.z,
                    r.eyeX, r.eyeY, r.eyeZ, world.indoors != null ? "indoors" : "out of doors") + (r.inAir() ? ", beats left " + r.beatsLeft() + (r.gliding ? ", gliding" : "") : ""));
        } else if (c.equals("onhut")) {
            // onhut: her on the roof of the lift's house nearest the folder, standing at its hut's door
            float[] l = doorNearFolder();
            world.runner.put(l[8], l[5], l[9], (float) Math.atan2(l[2], l[3]));
            step();
        } else if (c.equals("walk")) {
            // walk SECONDS EVERY PREFIX: indoors, a finger that goes where the place says to (to the
            // lift and in; come down, out at the front), shot so often
            float secs = f(p[1]), every = f(p[2]), next = 0f;
            int n = Math.round(secs / DT), k = 0;
            float[] way = new float[2];
            for (int i = 0; i < n; i++) {
                Runner r = world.runner;
                if (r.held || world.indoors == null) {
                    if (bdown) {
                        world.touch(World.UP, bx, by);
                        bdown = false;
                    }
                } else {
                    world.indoors.goal(r, way);
                    lead(way[0], way[1], 10f);
                }
                step();
                if (System.getenv("WALKLOG") != null && i % 15 == 0) {
                    Runner rr = world.runner;
                    System.out.println(String.format("%7.2fs  her %.1f %.1f %.1f h %.2f speed %.1f eye %.1f %.1f %.1f yaw %.2f %s",
                            time, rr.x, rr.y, rr.z, rr.heading, rr.speed, rr.eyeX, rr.eyeY, rr.eyeZ, rr.eyeYaw, world.indoors != null ? "in" : "out"));
                }
                if (every > 0f && i * DT >= next) {
                    shot(p[3] + (k++));
                    next += every;
                }
            }
            if (bdown) world.touch(World.UP, bx, by);
            bdown = false;
        } else if (c.equals("onfolder")) {
            // onfolder METERS RADIANS: her put on the folder's roof so far from it, the way given round it, facing it
            Folder fo = world.folder;
            float d = f(p[1]), a = f(p[2]);
            float x = fo.x + (float) Math.sin(a) * d, z = fo.z + (float) Math.cos(a) * d;
            world.runner.put(x, world.city.ground(x, z, fo.y + 1f, 2f), z, a + (float) Math.PI);
            step();
        } else if (c.equals("climbing")) {
            // climbing METERS: the first rival so far up the front of the folder's house, she across the street watching
            Folder fo = world.folder;
            Chase g = fo.rivals[1];
            g.standClimb(f(p[1]));
            float nx = fo.frontX, nz = fo.frontZ;
            float px = g.x + nx * 5f + nz * 16f, pz = g.z + nz * 5f - nx * 16f;
            world.runner.put(px, City.WALK_TOP, pz, (float) Math.atan2(g.x - px, g.z - pz));
            step();
        } else if (c.equals("catchall")) {
            // catchall: every golem of the hunt knocked down at once, as if the mallet had got them all
            for (Chase g : world.mission.golems) g.take(world.runner, world.city, 1f, 0f);
            step();
        } else if (c.equals("idle")) {
            // idle SECONDS EVERY PREFIX: no one playing; the rivals' ways told, shot so often, till the folder is got
            float secs = f(p[1]), every = f(p[2]), next = every;
            Folder fo = world.folder;
            int n = Math.round(secs / DT), k = 0;
            int[] was = new int[Folder.RIVALS];
            for (int i = 0; i < n && !fo.over(); i++) {
                step();
                for (int g = 0; g < Folder.RIVALS; g++)
                    if (fo.rivals[g].state != was[g]) {
                        was[g] = fo.rivals[g].state;
                        System.out.println(String.format("%7.2fs  rival %d: state %d, %.0f m to go", time, g, was[g], fo.toGo(fo.rivals[g])));
                    }
                if (System.getenv("FOLDERLOG") != null && i % 120 == 0)
                    for (int g = 0; g < Folder.RIVALS; g++) {
                        Chase r = fo.rivals[g];
                        if (r.above()) System.out.println(String.format("%7.2fs  rival %d at %.1f %.1f %.1f speed %.1f", time, g, r.x, r.y, r.z, r.speed));
                    }
                if (every > 0f && i * DT >= next) {
                    shot(p[3] + (k++));
                    next += every;
                }
            }
            if (fo.theirs) System.out.println(String.format("%7.2fs  a rival got it (clock %.1f)", time, fo.clock));
        } else if (c.equals("folderbot")) {
            folderBot(f(p[1]), f(p[2]), p[3]);
        } else if (c.equals("shot")) {
            shot(p[1]);
        } else {
            throw new IllegalArgumentException("unknown command: " + c);
        }
    }

    /** The lift in the folder's block whose door is nearest the folder, as City.liftAt() tells it. */
    private float[] doorNearFolder() {
        Folder fo = world.folder;
        int ci = City.index(fo.x), cj = City.index(fo.z);
        float[] h = new float[8], l = new float[10], best = null;
        float bd = 1e9f;
        for (int k = 0; k < world.city.houses(ci, cj); k++) {
            world.city.house(ci, cj, k, h);
            if (h[7] != City.LIFT) continue;
            City.lift(h, 0, l);
            float dd = (l[4] - fo.x) * (l[4] - fo.x) + (l[6] - fo.z) * (l[6] - fo.z);
            if (dd < bd) {
                bd = dd;
                best = l.clone();
            }
        }
        return best;
    }

    /** A golem this near and ahead, the hunting finger taps for the mallet. */
    static final float THROW_AT = 12f;

    /** A finger put down at (x, y), flicked up quickly and lifted, a step a frame. */
    private void flick(float x, float y) {
        world.touch(World.DOWN, x, y);
        step();
        for (int t = 1; t <= 4; t++) {
            world.touch(World.MOVE, x, y - t * 12f);
            step();
        }
        world.touch(World.UP, x, y - 48f);
        step();
    }

    // the bot's finger: where it is on the glass, and whether it is down
    private float bx = World.W * 0.5f, by = 600f;
    private boolean bdown;

    /** The finger turned toward (gx, gz), a little a frame, as a player's would. */
    private void lead(float gx, float gz, float most) {
        Runner r = world.runner;
        if (!bdown) {
            bx = World.W * 0.5f;
            world.touch(World.DOWN, bx, by);
            bdown = true;
        }
        float want = (float) Math.atan2(gx - r.x, gz - r.z), dd = want - r.heading;
        while (dd > Math.PI) dd -= 2f * (float) Math.PI;
        while (dd < -Math.PI) dd += 2f * (float) Math.PI;
        bx += Math.max(-most, Math.min(most, dd / (float) (Math.PI / 2) * Runner.QUARTER));
        if (bx < 20f || bx > World.W - 20f) {
            world.touch(World.UP, bx, by);
            bx = World.W * 0.5f;
            world.touch(World.DOWN, bx, by);
        }
        world.touch(World.MOVE, bx, by);
    }

    /**
     * folderbot SECONDS EVERY PREFIX: a player after the folder: along the streets to the lift
     * nearest it, in and up, across the roofs to it (leaping when a roof stands higher ahead,
     * gliding when it falls away), then down as she goes by herself; shot so often.
     */
    private void folderBot(float secs, float every, String prefix) throws IOException {
        Folder fo = world.folder;
        float[] door = doorNearFolder();
        System.out.println(String.format("%7.2fs  the lift: door %.1f %.1f facing %.0f %.0f, roof %.1f", time, door[0], door[1], door[2], door[3], door[5]));
        int wi = Math.round(world.runner.x / City.CELL), wj = Math.round(world.runner.z / City.CELL);
        int n = Math.round(secs / DT), k = 0;
        float next = every, stuck = 0f, leapAt = -9f;
        boolean wasUp = false, wasHers = false, wasHidden = false, wasGliding = false, wasHeld = false;
        float[] way = new float[2];
        int phase = 0;
        for (int i = 0; i < n; i++) {
            Runner r = world.runner;
            if (fo.over() || fo.hers) {
                if (bdown) {
                    world.touch(World.UP, bx, by);
                    bdown = false;
                }
            } else if (r.held) {
                // in the lift on its way: the finger off the glass
                if (bdown) {
                    world.touch(World.UP, bx, by);
                    bdown = false;
                }
            } else if (r.room != null) {
                // indoors: to the lift, and in; come down, out of the front door
                world.indoors.goal(r, way);
                lead(way[0], way[1], 10f);
            } else if (r.y < 1f) {
                // the street: crossing to crossing to the crossing by the door, then to the door
                float dx = door[0] - r.x, dz = door[1] - r.z;
                // near: in the street before its front, not round the corner
                float out = -(dx * door[2] + dz * door[3]), side = Math.abs(dx * door[3] - dz * door[2]);
                boolean near = out > -1f && out < 9f && side < 30f;
                float gx, gz;
                if (near) {
                    // to the door: straight before it, then in
                    float ax = door[0] + door[2] * 2.5f, az = door[1] + door[3] * 2.5f;
                    boolean before = (r.x - door[0]) * door[2] + (r.z - door[1]) * door[3] < 3f
                            && Math.abs((r.x - door[0]) * door[3] - (r.z - door[1]) * door[2]) < 0.8f;
                    gx = before ? door[0] - door[2] : ax;
                    gz = before ? door[1] - door[3] : az;
                } else {
                    float cx = wi * City.CELL, cz = wj * City.CELL;
                    if ((cx - r.x) * (cx - r.x) + (cz - r.z) * (cz - r.z) < 36f) {
                        int ti = Math.round(door[0] / City.CELL), tj = Math.round(door[1] / City.CELL);
                        if (ti != wi && (Math.abs(ti - wi) >= Math.abs(tj - wj) || tj == wj)) wi += Integer.signum(ti - wi);
                        else if (tj != wj) wj += Integer.signum(tj - wj);
                    }
                    cx = wi * City.CELL;
                    cz = wj * City.CELL;
                    if (Math.abs(cx - r.x) > Math.abs(cz - r.z)) {
                        gx = r.x + Math.max(-10f, Math.min(10f, cx - r.x));
                        gz = cz - Math.signum(cx - r.x) * 1.6f;
                    } else {
                        gx = cx + Math.signum(cz - r.z) * 1.6f;
                        gz = r.z + Math.max(-10f, Math.min(10f, cz - r.z));
                    }
                    if (Math.abs(cx - r.x) < 8f && Math.abs(cz - r.z) < 8f) {
                        gx = cx;
                        gz = cz;
                    }
                }
                lead(gx, gz, 6f);
            } else {
                // the roofs: to the folder; a roof higher ahead, a leap; falling, arms out
                lead(fo.x, fo.z, 8f);
                stuck = r.speed < 2f && !r.inAir() ? stuck + DT : 0f;
                float ax = r.x + (float) Math.sin(r.heading) * 1.6f, az = r.z + (float) Math.cos(r.heading) * 1.6f;
                float ahead = world.city.ground(ax, az, r.y, 5f);
                if (!r.inAir() && (stuck > 0.25f || ahead > r.y + 0.6f) && time - leapAt > 0.8f) {
                    // two flicks up: a jump, and on up from it
                    world.touch(World.UP, bx, by);
                    flick(bx, by);
                    flick(bx, by);
                    world.touch(World.DOWN, bx, by);
                    leapAt = time;
                    stuck = 0f;
                    System.out.println(String.format("%7.2fs  leap, %.1f m up ahead", time, ahead - r.y));
                }
                if (r.inAir() && r.gliding && !wasGliding)
                    System.out.println(String.format("%7.2fs  gliding, %.1f m up", time, r.y));
                wasGliding = r.inAir() && r.gliding;
            }
            step();
            boolean inside = r.room != null;
            if (inside != wasHidden) {
                wasHidden = inside;
                System.out.println(String.format("%7.2fs  %s", time, inside ? "indoors" : "out of doors"));
            }
            if (r.held != wasHeld) {
                wasHeld = r.held;
                System.out.println(String.format("%7.2fs  %s", time, r.held ? "in the lift" : "out of the lift"));
            }
            boolean up = r.y > 3f;
            if (up != wasUp) {
                wasUp = up;
                System.out.println(String.format("%7.2fs  %s at %.1f m", time, up ? "on the roofs" : "in the street", r.y));
            }
            if (fo.hers != wasHers) {
                wasHers = true;
                System.out.println(String.format("%7.2fs  she has the folder (clock %.1f)", time, fo.clock));
            }
            if (System.getenv("FOLDERLOG") != null && i % 120 == 0) {
                StringBuilder b = new StringBuilder();
                for (Chase g : fo.rivals) b.append(String.format(" [%d %.0f]", g.state, fo.toGo(g)));
                System.out.println(String.format("%7.2fs  her %.0f %.1f %.0f speed %.1f, folder %.0f m;%s", time, r.x, r.y, r.z, r.speed, fo.apart(r), b));
            }
            if (every > 0f && i * DT >= next) {
                shot(prefix + (k++));
                next += every;
            }
            if (world.over() && fo.theirs) {
                System.out.println(String.format("%7.2fs  a rival got it first (clock %.1f)", time, fo.clock));
                break;
            }
            if (r.landed) {
                System.out.println(String.format("%7.2fs  down in the street with it", time));
                shot(prefix + "end");
                break;
            }
        }
        if (bdown) world.touch(World.UP, bx, by);
        bdown = false;
    }

    // folkrow FIRST MOTION T [YAW]: the shots from now on look at four of the town's people in a
    // row on the pavement beside the start (models FIRST on), in a motion at a time (stroll: their
    // own walk, T meters along), turned so; folkrow off
    private int rowFirst = -1;
    private String rowMotion = "idle";
    private float rowT, rowYaw = (float) Math.PI * 0.5f - 0.35f;

    private void folkRow(DrawList d) {
        float px = 5.6f, z0 = 20f, step = 0.95f;
        int n = 4;
        for (int k = 0; k < n; k++) {
            int m = (rowFirst + k) % Math.max(1, FolkPack.town == null ? 1 : FolkPack.town.count);
            if (rowMotion.equals("stroll")) {
                Stroll.Way w = Stroll.way(1000L + m, FolkPack.town.woman(m));
                FolkPack.walker(d, m, px, City.WALK_TOP, z0 + k * step, rowYaw, 1f, w, rowT + k * 0.37f, 1f);
            } else FolkPack.person(d, m, px, City.WALK_TOP, z0 + k * step, rowYaw, 1f, rowMotion, rowT + k * 0.2f);
        }
        d.camera(px - 4.2f, 1.35f, z0 + (n - 1) * step * 0.5f, (float) Math.PI * 0.5f, -0.06f, World.W * 1.25f, 0.03f, 300f);
    }

    private void shot(String name) throws IOException {
        world.draw(list);
        if (rowFirst >= 0) folkRow(list);
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
