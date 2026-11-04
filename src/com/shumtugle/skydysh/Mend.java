package com.shumtugle.skydysh;

/**
 * The helicopter mended and flown home: the archive job's second half, begun at the archive's
 * door the moment the folder is in. The town's helicopter stands in the archive's garden on its
 * broken skid, its pilot, the black brum, still in his seat. A duty pickup stands at the curb by
 * the archive: she drives it to the brums' workshop by the road out and stops in the bay before
 * its gate, where a new skid on its struts is put in the bed; she drives it back, gets out by the
 * archive, carries the skid to the helicopter and puts it on. Then she takes the controls herself,
 * the pilot beside her, and flies it back to the pad on the town hall's roof. A story: played once.
 * After it the helicopter is hers.
 */
final class Mend {
    // the steps: a pickup, to the workshop for the skid, back to the archive, the skid to the
    // helicopter, the mending, aboard, in the air at the controls, done
    static final int PICKUP = 0, WORKSHOP = 1, BACK = 2, CARRY = 3, MENDING = 4, BOARD = 5, FLY = 6, DONE = 7;
    // how long she stands in the bay for the skid to be put on; how near the helicopter's door she
    // has to come; how long the mending takes; how near the town hall's pad it has to come down
    static final float LOAD = 0.8f, NEAR = 2.8f, WORK = 2.6f, PAD_NEAR = 9f;
    // the pickup brought back: near enough the helicopter for her to carry the skid from it
    static final float BROUGHT = 70f, AT_BED = 6f;
    /** Down on the pad, a moment before she gets out. */
    static final float OUT_AFTER = 1.4f;
    // what a step brought
    static final int NONE = 0, LOADED = 1, TAKEN = 2, BLOW = 3, MENDED = 4, IN = 5, OUT = 6;

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
        return step == BACK && van != null;
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
                        step = BACK;
                        got = LOADED;
                    }
                } else stood = 0f;
                break;
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
                if (work >= blowAt && work < WORK) {
                    blowAt += 0.45f;
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
    void draw(DrawList d, Runner r) {
        Archive.drawCopter(d, copter, step == FLY ? Archive.BESIDE : step == DONE ? Archive.NOBODY : Archive.AT_CONTROLS);
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

    private final float[] hand = new float[3], hand2 = new float[3];
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
