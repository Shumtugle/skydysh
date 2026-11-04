package com.shumtugle.skydysh;

/**
 * The tow: out past the town, at the foot of the ridge west of the road to the pass, a brum's
 * pickup has gone down into the quarry and sits in the mud at its bottom, and cannot get out.
 * She takes a duty pickup there and stops by it; a rope goes from one to the other; she tows it
 * out and back to the brums' workshop at the town's last street, by the road out, and leaves it in
 * the bay painted before the workshop's gate. Then home.
 *
 * The pickup towed follows the one towing on its rope: slack, it stands; taut, it is drawn after
 * it by the end the rope is tied to, turning to the pull.
 */
final class Tow {
    // the steps: a pickup to be found, to the quarry and the rope, the tow to the workshop, done
    static final int PICKUP = 0, QUARRY = 1, TOWING = 2, DONE = 3;
    // the pickup in the quarry: where it sits, the way it faces (in, to the rock face)
    static final float STUCK_X = City.QUARRY_X, STUCK_Z = City.QUARRY_Z + 4f, STUCK_HEADING = 0f;
    // how deep it sits in the mud; how near her pickup's end must stand to its own, and how long
    static final float SUNK = 0.32f, REACH = 10f, TIE = 0.8f;
    // the rope's length, end to end
    static final float ROPE = 5f;

    /** Whether mission n is the tow: the sixth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 6;
    }

    int step = PICKUP;
    float clock, doneAt = -1f;
    /** The brum's pickup. */
    final Pickup stuck = new Pickup(STUCK_X, STUCK_Z, STUCK_HEADING, -1L);
    /** The pickup towing it, once the rope is on. */
    Pickup tug;
    // the ends the rope is tied to: +1 the front, -1 the back; of the tug, and of the one towed
    private int tugEnd, stuckEnd;
    private float stood;
    private final float[] a = new float[2], b = new float[2];

    boolean over() {
        return step == DONE;
    }

    void update(float dt, Duty duty, Pickup driving, Runner r, City city, World.Host host) {
        if (step == DONE) return;
        clock += dt;
        if (step == PICKUP && driving != null) step = QUARRY;
        if (step == QUARRY) {
            // stood near it, at the wheel and still a moment: the rope goes on, the nearer ends tied
            boolean near = false;
            if (driving != null && Math.abs(driving.speed) < 0.6f) {
                int te = nearerEnd(driving, stuck.x, stuck.z), se = nearerEnd(stuck, driving.x, driving.z);
                end(driving, te, a);
                end(stuck, se, b);
                float dx = a[0] - b[0], dz = a[1] - b[1];
                if (dx * dx + dz * dz < REACH * REACH) {
                    near = true;
                    stood += dt;
                    if (stood > TIE) {
                        tug = driving;
                        tugEnd = te;
                        stuckEnd = se;
                        step = TOWING;
                        host.sound(Synth.LEATHER, 0.7f, 0.8f);
                        host.buzz(80);
                    }
                }
            }
            if (!near) stood = 0f;
        }
        if (step == TOWING) {
            follow(dt, city);
            boolean in = driving == tug && Math.abs(tug.speed) < 0.6f && City.inWorkshopBay(tug.x, tug.z);
            if (in) {
                step = DONE;
                doneAt = clock;
                host.sound(Synth.CHIME, 0.5f, 1.3f);
            }
        }
    }

    /** Which end of p (+1 the front, -1 the back) is nearer (x, z). */
    private static int nearerEnd(Pickup p, float x, float z) {
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading);
        return (x - p.x) * fx + (z - p.z) * fz >= 0f ? 1 : -1;
    }

    /** Where an end of p is (x, z into out). */
    private static void end(Pickup p, int e, float[] out) {
        float fx = (float) Math.sin(p.heading), fz = (float) Math.cos(p.heading);
        out[0] = p.x + fx * e * Pickup.HALF_LONG;
        out[1] = p.z + fz * e * Pickup.HALF_LONG;
    }

    /** The towed pickup drawn after the tug: its tied end kept within the rope's length of the tug's. */
    private void follow(float dt, City city) {
        end(tug, tugEnd, a);
        end(stuck, stuckEnd, b);
        float dx = a[0] - b[0], dz = a[1] - b[1], d = (float) Math.sqrt(dx * dx + dz * dz);
        if (d > ROPE) {
            // taut: its tied end along the rope, toward the tug, and it turns to the pull
            float ux = dx / d, uz = dz / d;
            float want = (float) Math.atan2(ux * stuckEnd, uz * stuckEnd);
            float turn = want - stuck.heading;
            while (turn > Math.PI) turn -= 2f * (float) Math.PI;
            while (turn < -Math.PI) turn += 2f * (float) Math.PI;
            stuck.heading += turn * Math.min(1f, dt * 2.5f);
            float fx = (float) Math.sin(stuck.heading), fz = (float) Math.cos(stuck.heading);
            float ex = a[0] - ux * ROPE, ez = a[1] - uz * ROPE;
            stuck.x = ex - fx * stuckEnd * Pickup.HALF_LONG;
            stuck.z = ez - fz * stuckEnd * Pickup.HALF_LONG;
        }
        stuck.y = city.ground(stuck.x, stuck.z);
    }

    /** Where the way goes now, into out (x, y, z): a pickup, the brum's in the quarry, the workshop's bay, or the tug she left. */
    boolean goal(Duty duty, Pickup driving, Runner r, float[] out) {
        switch (step) {
            case PICKUP: {
                Pickup best = null;
                float bd = 1e18f;
                for (Pickup p : duty.all) {
                    float dd = (p.x - r.x) * (p.x - r.x) + (p.z - r.z) * (p.z - r.z);
                    if (dd < bd) {
                        bd = dd;
                        best = p;
                    }
                }
                if (best == null) return false;
                set(out, best.x, best.y + 2.2f, best.z);
                return true;
            }
            case QUARRY:
                set(out, stuck.x, 2.4f, stuck.z);
                return true;
            case TOWING:
                if (driving != tug) {
                    set(out, tug.x, tug.y + 2.2f, tug.z);
                    return true;
                }
                set(out, City.WORKSHOP_BAY_X, 1.2f, City.workshopZ());
                return true;
            default:
                return false;
        }
    }

    private static void set(float[] out, float x, float y, float z) {
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord(Pickup driving) {
        if (step == PICKUP || step == TOWING && driving != tug) return "goal.pickup";
        return step == QUARRY ? "goal.quarry" : "goal.workshop";
    }

    /** The mark of where the way goes now (see Marks). */
    int mark(Pickup driving) {
        if (step == PICKUP || step == TOWING && driving != tug) return Marks.TRUCK;
        return step == QUARRY ? Marks.QUARRY : Marks.WORKSHOP;
    }

    // ---- drawing ----
    private final float[] seat = new float[3];
    private static final float[] turn = new float[9];

    /** The brum's pickup, the brum at its wheel, the mud it sat in, and the rope while it is on. */
    void draw(DrawList d, float time) {
        float sink = step == QUARRY || step == PICKUP ? SUNK : 0f;
        float y0 = stuck.y;
        stuck.y = y0 - sink;
        stuck.draw(d, time);
        // the brum at its wheel, slate grey where she is brick
        stuck.seat(seat);
        Frida.slate = true;
        Frida.draw(d, seat[0], seat[1] - Runner.SEAT_DROP, seat[2], stuck.heading + (float) Math.PI, Chase.SIZE, Frida.SIT, 0f);
        Frida.slate = false;
        stuck.y = y0;
        if (sink > 0f)
            d.ellipsoid(stuck.x, y0 + 0.02f, stuck.z, 2.2f, 0.04f, 3.4f, null, 0x5A4632, DrawList.MATTE);
        if (step == TOWING && tug != null) rope(d);
    }

    /** The rope from end to end, a little low in the middle while it is slack. */
    private void rope(DrawList d) {
        end(tug, tugEnd, a);
        end(stuck, stuckEnd, b);
        float ay = tug.y + 0.65f, by = stuck.y + 0.65f;
        float dx = b[0] - a[0], dz = b[1] - a[1], len = (float) Math.sqrt(dx * dx + dz * dz);
        float slack = Math.max(0f, ROPE - len) * 0.35f;
        int n = 8;
        float yaw = (float) Math.atan2(dx, dz), c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        turn[0] = c; turn[1] = 0f; turn[2] = s;
        turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
        turn[6] = -s; turn[7] = 0f; turn[8] = c;
        for (int k = 0; k < n; k++) {
            float u = (k + 0.5f) / n;
            float x = a[0] + dx * u, z = a[1] + dz * u;
            float y = ay + (by - ay) * u - slack * 4f * u * (1f - u);
            d.box(x, y, z, 0.035f, 0.035f, len / n * 0.55f, turn, 0xC9A86A, DrawList.PLAIN);
        }
    }
}
