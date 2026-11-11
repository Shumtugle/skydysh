package com.shumtugle.skydysh;

/**
 * The helicopter mended and flown home: the archive job's second half, begun at the archive's
 * counter the moment the folder is in. The town's helicopter stands in the archive's garden,
 * leaning where its skid broke off, its pilot, the black brum, still in his seat. A duty pickup
 * stands at the curb by the archive: she drives it to the brums' workshop by the road out and
 * stops in the bay before its gate; a brum of the workshop comes out with a new skid on its struts
 * in his arms, puts it in the bed and goes back in, seen from across the road. She drives it back,
 * gets out by the archive, carries the skid to the helicopter, holds it up to the stumps of the old
 * struts and knocks it home with the mallet, seen from beside it; the helicopter rights itself.
 * Then she takes the controls herself, the pilot beside her, and flies it back to the pad on the
 * town hall's roof. A story: played once. After it the helicopter is hers.
 */
final class Mend {
    // the steps: a pickup, to the workshop for the skid, back to the archive, the skid to the
    // helicopter, the mending, aboard, in the air at the controls, done
    static final int PICKUP = 0, WORKSHOP = 1, BACK = 2, CARRY = 3, MENDING = 4, BOARD = 5, FLY = 6, DONE = 7;
    /** At the workshop's gate, the pickup standing in the bay: the skid brought out and put in the bed. */
    static final int LOADING = 8;
    // the workshop's brum: how fast he walks; how long he lifts the skid over the side into the bed
    static final float WORKER_PACE = 1.6f, PUT_FOR = 1.3f;
    // the mending: the skid held up into its place so long, then the blows, so many seconds apart
    static final float FIT_FOR = 1.2f, BLOW_EVERY = 0.5f;
    // how long she stands in the bay for the skid to be put on; how near the helicopter's door she
    // has to come; how long the mending takes; how near the town hall's pad it has to come down
    static final float LOAD = 0.5f, NEAR = 2.8f, WORK = 4.4f, PAD_NEAR = 9f;
    // the pickup brought back: near enough the helicopter for her to carry the skid from it
    static final float BROUGHT = 70f, AT_BED = 6f;
    /** Down on the pad, a moment before she gets out. */
    static final float OUT_AFTER = 1.4f;
    // what a step brought
    static final int NONE = 0, LOADED = 1, TAKEN = 2, BLOW = 3, MENDED = 4, IN = 5, OUT = 6, BAY = 7;

    /** Whether mission n is the helicopter mended: the ninth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 9;
    }

    int step;
    float clock, doneAt = -1f;
    /** The town's helicopter. */
    final Copter copter;
    /** The pickup with the skid in its bed (null till there is one). */
    Pickup van;
    private float stood, work, downFor, blowAt;
    /** At the workshop: how long the loading has gone on; his walk out, in seconds. */
    float loadT;
    private float walkFor;
    private final float[] at = new float[3];

    Mend(Copter copter) {
        this.copter = copter;
        step = copter.broken ? PICKUP : BOARD;
    }

    boolean over() {
        return step == DONE;
    }

    /** Whether she carries the skid now, on foot. */
    boolean carrying() {
        return step == CARRY;
    }

    /** Whether she is at the controls now. */
    boolean aboard() {
        return step == FLY;
    }

    /** Whether the skid lies in the pickup's bed. */
    boolean loaded() {
        return step == BACK && van != null || step == LOADING && loadT >= walkFor + PUT_FOR;
    }

    /** Whether the pickup is kept standing where it is (the skid being put in it). */
    boolean holdsVan() {
        return step == LOADING;
    }

    /** Whether the world is to be shown from an eye of its own now: the loading, the mending. */
    boolean scene() {
        return step == LOADING || step == MENDING;
    }

    /** How far through the mending (seconds). */
    float work() {
        return work;
    }

    // the workshop's south gate, where its brum comes out, on its front on the road out
    static float gateX() {
        return -City.FRONT + 0.4f;
    }

    static float gateZ() {
        return City.workshopZ() - 4.9f;
    }

    /** Beside the pickup's bed, on the side toward the gate, where the brum stands to put the skid in: into out (x, z). */
    void bed(float[] out) {
        float fx = (float) Math.sin(van.heading), fz = (float) Math.cos(van.heading);
        float rx = fz, rz = -fx;
        float bx = van.x - fx * 1.55f, bz = van.z - fz * 1.55f;
        float side = (gateX() - van.x) * rx + (gateZ() - van.z) * rz >= 0f ? 1f : -1f;
        out[0] = bx + rx * side * 1.6f;
        out[1] = bz + rz * side * 1.6f;
    }

    /** Where the new skid goes on the helicopter: under its right side (x, y, z). */
    void fit(float[] out) {
        copter.point(Copter.SKID_X, 0.35f, 0.565f, out);
    }

    /**
     * A step on: the pickup she drives (null on foot), her; whether she has come to the
     * helicopter's door to get in. What it brought (NONE to OUT).
     */
    int update(float dt, Pickup driving, Runner r, World.Host host, boolean boarding) {
        if (step == DONE) return NONE;
        clock += dt;
        int got = NONE;
        switch (step) {
            case PICKUP:
                if (driving != null) {
                    van = driving;
                    step = WORKSHOP;
                }
                break;
            case WORKSHOP:
                // the skid goes in the bed of whichever pickup she brings
                if (driving != null && Math.abs(driving.speed) < 0.6f && City.inWorkshopBay(driving.x, driving.z)) {
                    stood += dt;
                    if (stood > LOAD) {
                        van = driving;
                        step = LOADING;
                        loadT = 0f;
                        float[] b = new float[2];
                        bed(b);
                        float dx = b[0] - gateX(), dz = b[1] - gateZ();
                        walkFor = Math.max(1.2f, (float) Math.sqrt(dx * dx + dz * dz) / WORKER_PACE);
                        got = BAY;
                    }
                } else stood = 0f;
                break;
            case LOADING: {
                float was = loadT;
                loadT += dt;
                // in the bed: the clang of it
                if (was < walkFor + PUT_FOR && loadT >= walkFor + PUT_FOR) got = LOADED;
                if (loadT >= 2f * walkFor + PUT_FOR) step = BACK;
                break;
            }
            case BACK:
                // got out by the pickup, the pickup brought near enough: the skid taken out of its bed
                if (driving == null && van != null && near(van.x, van.z, copter.x, copter.z, BROUGHT)
                        && near(r.x, r.z, van.x, van.z, AT_BED)) {
                    step = CARRY;
                    got = TAKEN;
                }
                break;
            case CARRY:
                copter.door(at);
                if (driving == null && !r.inAir() && near(r.x, r.z, at[0], at[1], NEAR)) {
                    step = MENDING;
                    work = 0f;
                    blowAt = 0f;
                }
                break;
            case MENDING:
                work += dt;
                if (work >= FIT_FOR && work >= blowAt && work < WORK - 0.3f) {
                    blowAt = Math.max(blowAt, FIT_FOR) + BLOW_EVERY;
                    got = BLOW;
                }
                if (work >= WORK) {
                    copter.mend();
                    step = BOARD;
                    got = MENDED;
                }
                break;
            case BOARD:
                if (boarding) {
                    step = FLY;
                    copter.takeControls();
                    got = IN;
                }
                break;
            case FLY:
                if (copter.grounded && near(copter.x, copter.z, City.townPadX(), City.townPadZ(), PAD_NEAR)
                        && Math.abs(copter.y - City.townRoof()) < 1.5f) {
                    downFor += dt;
                    if (downFor > OUT_AFTER) {
                        step = DONE;
                        doneAt = clock;
                        copter.leave();
                        got = OUT;
                    }
                } else downFor = 0f;
                break;
            default:
                break;
        }
        return got;
    }

    private static boolean near(float ax, float az, float bx, float bz, float d) {
        float dx = ax - bx, dz = az - bz;
        return dx * dx + dz * dz < d * d;
    }

    /** Where the way goes now, into out (x, y, z); false if nowhere. */
    boolean goal(Duty duty, Pickup driving, Runner r, float[] out) {
        switch (step) {
            case PICKUP: {
                Pickup best = null;
                float bd = 1e18f;
                for (Pickup p : duty.all) {
                    if (p.boat) continue;
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
            case WORKSHOP:
                if (driving == null && van != null) {
                    set(out, van.x, van.y + 2.2f, van.z);
                    return true;
                }
                set(out, City.WORKSHOP_BAY_X, 1.2f, City.workshopZ());
                return true;
            case BACK:
                if (driving == null && van != null && !near(van.x, van.z, copter.x, copter.z, BROUGHT)) {
                    set(out, van.x, van.y + 2.2f, van.z);
                    return true;
                }
                if (driving == null && van != null) {
                    // brought: to its bed, for the skid
                    set(out, van.x, van.y + 2.2f, van.z);
                    return true;
                }
                set(out, City.archivePadX(), City.WALK_TOP + 3f, City.archivePadZ());
                return true;
            case CARRY:
            case MENDING:
            case BOARD:
                set(out, copter.x, copter.y + Copter.ROOF + 1.6f, copter.z);
                return true;
            case FLY:
                set(out, City.townPadX(), City.townRoof() + 1.5f, City.townPadZ());
                return true;
            default:
                return false;
        }
    }

    /** The word for where the way goes now, as Lang keeps it; null for none. */
    String goalWord(Pickup driving) {
        switch (step) {
            case PICKUP: return "goal.pickup";
            case WORKSHOP: return driving == null && van != null ? "goal.pickup" : "goal.workshop";
            case BACK: return driving == null && van != null ? "goal.skid" : "goal.archive";
            case CARRY: return "goal.copter";
            case MENDING: return "goal.mending";
            case BOARD: return "goal.controls";
            case FLY: return "goal.townhall";
            default: return null;
        }
    }

    /** The mark for where the way goes now. */
    int mark(Pickup driving) {
        switch (step) {
            case PICKUP: return Marks.TRUCK;
            case WORKSHOP: return driving == null && van != null ? Marks.TRUCK : Marks.WORKSHOP;
            case BACK: return driving == null && van != null ? Marks.TRUCK : Marks.ARCHIVE;
            case FLY: return Marks.TOWNHALL;
            default: return Marks.COPTER;
        }
    }

    private static void set(float[] out, float x, float y, float z) {
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    // ---- drawing ----

    /** The helicopter, the pilot in his seat (or beside her, while she flies it); the skid in the bed, or in her arms. */
    void draw(DrawList d, Runner r, float time) {
        Archive.drawCopter(d, copter, step == FLY ? Archive.BESIDE : step == DONE ? Archive.NOBODY : Archive.AT_CONTROLS);
        if (step == LOADING) worker(d, time);
        if (step == MENDING) {
            // held up from her arms into its place, then there while she knocks it home; a spray of
            // sparks at each blow
            fit(spot);
            float a = Math.min(1f, work / FIT_FOR), e = a * a * (3f - 2f * a);
            r.bone("hand_l", hand);
            r.bone("hand_r", hand2);
            float hx = (hand[0] + hand2[0]) * 0.5f, hy = (hand[1] + hand2[1]) * 0.5f - 0.1f, hz = (hand[2] + hand2[2]) * 0.5f;
            float yaw0 = r.heading + (float) Math.PI * 0.5f, yaw1 = copter.heading;
            skid(d, hx + (spot[0] - hx) * e, hy + (spot[1] - hy) * e, hz + (spot[2] - hz) * e, yaw0 + wrap(yaw1 - yaw0) * e);
            float since = work >= FIT_FOR ? (work - FIT_FOR) % BLOW_EVERY : 1f;
            if (since < 0.22f) {
                float k = since / 0.22f, sz = 0.05f * (1f - k) + 0.01f;
                for (int q = 0; q < 6; q++) {
                    float ang = q * 1.047f + work * 3f, out = 0.2f + k * 0.9f;
                    d.box(spot[0] + (float) Math.cos(ang) * out, spot[1] + 0.5f + k * 0.6f - k * k * 0.9f, spot[2] + (float) Math.sin(ang) * out,
                            sz, sz, sz, null, 0xFFC860, DrawList.GLOW);
                }
            }
        }
        if (loaded()) {
            // in the pickup's bed, its end out over the tailgate
            float c = (float) Math.cos(van.heading), s = (float) Math.sin(van.heading);
            float lx = 0.1f, lz = -1.55f;
            skid(d, van.x + c * lx + s * lz, van.y + 1.1f, van.z - s * lx + c * lz, van.heading);
        }
        if (carrying()) {
            // across her arms, before her chest
            r.bone("hand_l", hand);
            r.bone("hand_r", hand2);
            skid(d, (hand[0] + hand2[0]) * 0.5f, (hand[1] + hand2[1]) * 0.5f - 0.1f, (hand[2] + hand2[2]) * 0.5f, r.heading + (float) Math.PI * 0.5f);
        }
    }

    private final float[] hand = new float[3], hand2 = new float[3], spot = new float[3], bedAt = new float[2], held = new float[3];

    private static float wrap(float a) {
        while (a > Math.PI) a -= 2f * (float) Math.PI;
        while (a < -Math.PI) a += 2f * (float) Math.PI;
        return a;
    }

    /**
     * The workshop's brum, slate grey: out of the gate with the skid across his arms to beside the
     * pickup's bed, lifting it over the side into the bed, and back in at the gate.
     */
    private void worker(DrawList d, float time) {
        bed(bedAt);
        float gx = gateX(), gz = gateZ(), t = loadT;
        float x, z, yaw, phase;
        String motion;
        boolean carries;
        if (t < walkFor) {
            float a = t / walkFor;
            x = gx + (bedAt[0] - gx) * a;
            z = gz + (bedAt[1] - gz) * a;
            yaw = (float) Math.atan2(bedAt[0] - gx, bedAt[1] - gz);
            motion = "walk";
            phase = t * 1.33f;
            carries = true;
        } else if (t < walkFor + PUT_FOR) {
            x = bedAt[0];
            z = bedAt[1];
            float bx = van.x - (float) Math.sin(van.heading) * 1.55f, bz = van.z - (float) Math.cos(van.heading) * 1.55f;
            yaw = (float) Math.atan2(bx - x, bz - z);
            motion = Frida.HOLD;
            phase = time;
            carries = false;
        } else {
            float a = Math.min(1f, (t - walkFor - PUT_FOR) / walkFor);
            x = bedAt[0] + (gx - bedAt[0]) * a;
            z = bedAt[1] + (gz - bedAt[1]) * a;
            yaw = (float) Math.atan2(gx - bedAt[0], gz - bedAt[1]);
            motion = "walk";
            phase = t * 1.33f;
            carries = false;
        }
        Frida.slate = true;
        Frida.draw(d, x, City.WALK_TOP, z, yaw + (float) Math.PI, Chase.SIZE, motion, phase);
        Frida.slate = false;
        float fx = (float) Math.sin(yaw), fz = (float) Math.cos(yaw);
        if (carries) {
            // across his arms before his chest
            skid(d, x + fx * 0.45f, City.WALK_TOP + 1.05f, z + fz * 0.45f, yaw + (float) Math.PI * 0.5f);
        } else if (t < walkFor + PUT_FOR) {
            // up out of his hands and over the side into the bed
            float a = (t - walkFor) / PUT_FOR, e = a * a * (3f - 2f * a);
            Frida.hands(x, City.WALK_TOP, z, yaw + (float) Math.PI, Chase.SIZE, held);
            float c = (float) Math.cos(van.heading), s = (float) Math.sin(van.heading);
            float lx = 0.1f, lz = -1.55f;
            float tx = van.x + c * lx + s * lz, ty = van.y + 1.1f, tz = van.z - s * lx + c * lz;
            skid(d, held[0] + (tx - held[0]) * e, held[1] + (ty - held[1]) * e + 0.6f * (float) Math.sin(Math.PI * a),
                    held[2] + (tz - held[2]) * e, yaw + (float) Math.PI * 0.5f + wrap(van.heading - yaw - (float) Math.PI * 0.5f) * e);
        }
    }
    private static final float[] turn = new float[9];

    /** A new skid on its two struts, lying along heading a: a dark tube, the struts up off it, the bolts bright. */
    static void skid(DrawList d, float x, float y, float z, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        turn[0] = c; turn[1] = 0f; turn[2] = s;
        turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
        turn[6] = -s; turn[7] = 0f; turn[8] = c;
        d.box(x, y, z, 0.09f, 0.1f, 1.35f, turn, Copter.DARK, DrawList.POLISHED);
        for (int k = -1; k <= 1; k += 2) {
            float lz = k * 0.75f;
            d.box(x + s * lz, y + 0.22f, z + c * lz, 0.07f, 0.22f, 0.08f, turn, Copter.DARK, DrawList.POLISHED);
            d.box(x + s * lz, y + 0.45f, z + c * lz, 0.1f, 0.03f, 0.1f, turn, 0xD8D2C4, DrawList.POLISHED);
        }
    }
}
