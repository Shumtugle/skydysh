package com.shumtugle.skydysh;

/**
 * Papers to the archive: a call from the town hall. In the secretariat, a room off its hall, a
 * clerk holds a red folder of documents for the archive, far off in the north-east of the town;
 * up on the roof the town's helicopter waits, a brum of black brick at its controls. She takes
 * the folder, crosses the hall to the lift, goes up, gets in beside the pilot, and they fly across the whole town. Over the archive's garden
 * it comes straight down onto the pad there, and as it touches a skid gives way: it stands
 * leaning. She gets out and takes the folders in at the archive's back door. A story: played once.
 */
final class Archive {
    // the steps: to the town hall, to the secretariat in its hall, the folders to the lift and up,
    // on the roof to the helicopter, in the air, down and getting out, the folders to the door, done
    static final int GO = 0, DESK = 1, LIFT = 2, ROOF = 3, FLY = 4, CARRY = 5, DONE = 6;
    // how near the desk, the helicopter's door, the archive's door she has to come
    static final float DESK_NEAR = 1.9f, BOARD_NEAR = 2.6f, DOOR_NEAR = 2.2f;
    /** Down on the pad, a moment before she gets out. */
    static final float OUT_AFTER = 2f;
    // what a step brought: she has got in, she has got out, a skid has given way
    static final int NONE = 0, IN = 1, OUT = 2, CRACK = 3;

    /** Whether mission n is the archive: the eighth kind. */
    static boolean its(int n) {
        return World.kindOf(n) == 8;
    }

    int step = GO;
    float clock, doneAt = -1f;
    /** The helicopter, on the town hall's roof at first. */
    final Copter copter;
    private float downFor;

    Archive() {
        copter = new Copter(City.townPadX(), City.townRoof(), City.townPadZ(), (float) Math.PI * 0.5f);
    }

    boolean over() {
        return step == DONE;
    }

    /** Whether she has the folder with her, on foot. */
    boolean carrying() {
        return step == LIFT || step == ROOF || step == CARRY;
    }

    /** Whether she sits in the helicopter now. */
    boolean aboard() {
        return step == FLY;
    }

    /** A step on: where she is, the room she is in (null out of doors). What it brought (NONE, IN, OUT, CRACK). */
    int update(float dt, Runner r, Indoors in, World.Host host) {
        if (step == DONE) {
            copter.update(dt);
            return NONE;
        }
        clock += dt;
        int got = NONE;
        if (copter.update(dt)) got = CRACK;
        switch (step) {
            case GO:
                if (in != null && in.townHall) step = DESK;
                break;
            case DESK:
                if (in != null && in.townHall) {
                    float du = in.u(r.x, r.z) - in.deskU, dv = in.v(r.x, r.z) - in.deskV;
                    if (du * du + dv * dv < DESK_NEAR * DESK_NEAR) {
                        step = LIFT;
                        host.sound(Synth.WOOD, 0.4f, 1.6f);
                        host.sound(Synth.CHIME, 0.35f, 1.4f);
                        host.buzz(30);
                    }
                }
                break;
            case LIFT:
                if (in == null && r.y > City.townRoof() - 1f) step = ROOF;
                break;
            case ROOF: {
                copter.door(at);
                float dx = r.x - at[0], dz = r.z - at[1];
                if (in == null && Math.abs(r.y - City.townRoof()) < 1.5f && dx * dx + dz * dz < BOARD_NEAR * BOARD_NEAR && !r.inAir()) {
                    step = FLY;
                    copter.send(City.archivePadX(), City.archivePadZ(), City.WALK_TOP, true, 1f);
                    host.sound(Synth.DOOR, 0.4f, 1.4f);
                    host.buzz(30);
                    got = IN;
                }
                break;
            }
            case FLY:
                if (copter.landed()) {
                    downFor += dt;
                    if (downFor > OUT_AFTER) {
                        step = CARRY;
                        host.sound(Synth.DOOR, 0.4f, 1.3f);
                        got = got == CRACK ? CRACK : OUT;
                        if (got == CRACK) outPending = true;
                    }
                }
                break;
            case CARRY: {
                float dx = r.x - City.archiveDoorX(), dz = r.z - City.archiveDoorZ();
                if (dx * dx + dz * dz < DOOR_NEAR * DOOR_NEAR) {
                    step = DONE;
                    doneAt = clock;
                    host.sound(Synth.DOOR, 0.45f, 0.95f);
                    host.sound(Synth.CHIME, 0.5f, 1.3f);
                }
                break;
            }
            default:
                break;
        }
        if (outPending && got == NONE) {
            outPending = false;
            got = OUT;
        }
        return got;
    }

    private boolean outPending;
    private final float[] at = new float[3], hand = new float[3];

    /**
     * Where the way goes now, into out (x, y, z); false if nowhere (in the air). In: the room she
     * is in, if any.
     */
    boolean goal(Indoors in, Runner r, float[] out) {
        switch (step) {
            case GO:
                return door(out);
            case DESK:
                if (in == null || !in.townHall) return door(out);
                out[0] = in.wx(in.deskU, in.deskV);
                out[1] = Indoors.FLOOR + 2.4f;
                out[2] = in.wz(in.deskU, in.deskV);
                return true;
            case LIFT:
                if (in == null || !in.townHall) return door(out);
                out[0] = in.wx(in.side * in.half, in.liftV);
                out[1] = Indoors.FLOOR + 3f;
                out[2] = in.wz(in.side * in.half, in.liftV);
                return true;
            case ROOF:
                if (in != null || r.y < City.townRoof() - 1f) return door(out);
                out[0] = copter.x;
                out[1] = copter.y + Copter.ROOF + 1.6f;
                out[2] = copter.z;
                return true;
            case CARRY:
                out[0] = City.archiveDoorX();
                out[1] = City.WALK_TOP + 3.6f;
                out[2] = City.archiveDoorZ() - 1f;
                return true;
            default:
                return false;
        }
    }

    /** The town hall's door from the street. */
    private static boolean door(float[] out) {
        out[0] = City.TOWNHALL_I * City.CELL + City.CELL * 0.5f;
        out[1] = City.WALK_TOP + 3.6f;
        out[2] = City.TOWNHALL_J * City.CELL + City.FRONT - 0.5f;
        return true;
    }

    /** The word for where the way goes now, as Lang keeps it; null for none. */
    String goalWord(Indoors in, Runner r) {
        switch (step) {
            case GO: return "goal.townhall";
            case DESK: return in != null && in.townHall ? "goal.desk" : "goal.townhall";
            case LIFT: return in != null && in.townHall ? "goal.lift" : "goal.townhall";
            case ROOF: return in == null && r.y > City.townRoof() - 1f ? "goal.copter" : "goal.townhall";
            case CARRY: return "goal.archive";
            default: return null;
        }
    }

    /** The mark for where the way goes now. */
    int mark(Indoors in, Runner r) {
        switch (step) {
            case DESK: return in != null && in.townHall ? Marks.DESK : Marks.TOWNHALL;
            case LIFT: return in != null && in.townHall ? Marks.LIFT : Marks.TOWNHALL;
            case ROOF: return in == null && r.y > City.townRoof() - 1f ? Marks.COPTER : Marks.TOWNHALL;
            case CARRY: return Marks.ARCHIVE;
            default: return Marks.TOWNHALL;
        }
    }

    // ---- drawing ----

    /** The helicopter and its pilot; the folders under her arm while she carries them. */
    void draw(DrawList d, Runner r) {
        drawCopter(d, copter);
        if (carrying()) carried(d, r);
    }

    // where the black brum sits in it: nowhere (it is hers), at the controls, beside her as she flies it
    static final int NOBODY = 0, AT_CONTROLS = 1, BESIDE = 2;

    /** The helicopter, its pilot at the controls, a brum of black brick. */
    static void drawCopter(DrawList d, Copter cp) {
        drawCopter(d, cp, AT_CONTROLS);
    }

    /** The helicopter, the black brum where he sits in it (NOBODY, AT_CONTROLS, BESIDE). */
    static void drawCopter(DrawList d, Copter cp, int pilot) {
        cp.draw(d);
        if (pilot == NOBODY) return;
        cp.seat(pilot == AT_CONTROLS, pilotAt);
        Frida.black = true;
        Frida.draw(d, pilotAt[0], pilotAt[1] - Runner.SEAT_DROP, pilotAt[2], cp.heading + (float) Math.PI, Chase.SIZE, Frida.SIT, 0f);
        Frida.black = false;
    }

    private static final float[] pilotAt = new float[3];

    /** The red folder under her arm. */
    void carried(DrawList d, Runner r) {
        r.bone("hand_l", hand);
        folder(d, hand[0], hand[1] - 0.05f, hand[2], r.heading + (float) Math.PI * 0.5f);
    }

    /** Whether the clerk in the secretariat still has the folder to give. */
    boolean waiting() {
        return step <= DESK;
    }

    private static final float[] turn = new float[9];

    /** The red folder, upright, its face turned so (a heading, as the town's people turn): red card, the papers' edge, a black band. */
    static void folder(DrawList d, float x, float y, float z, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        turn[0] = c; turn[1] = 0f; turn[2] = s;
        turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
        turn[6] = -s; turn[7] = 0f; turn[8] = c;
        float w = 0.3f, l = 0.23f, t = 0.035f;
        d.box(x, y, z, w, l, t, turn, 0xB0302A, DrawList.PAINT);
        d.box(x, y, z, w - 0.02f, l - 0.02f, t + 0.012f, turn, 0xEFE6D2, DrawList.PLAIN);
        d.box(x, y, z, 0.03f, l + 0.004f, t + 0.016f, turn, 0x1B1B20, DrawList.PLAIN);
    }
}
