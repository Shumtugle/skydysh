package com.shumtugle.skydysh;

/**
 * The puppy in the sea: off the beach east of home, past the store, a puppy is out in the water
 * with no strength left to swim back, and a crowd of people stands at the water's edge looking
 * out at it. She gets there (a duty pickup is the quick way, along the shore road), swims out to
 * it and takes it; then to the vets' on the street north of home's row, three streets up, where it
 * is handed in at the door, in the bay painted on the road before it. The puppy holds out so long:
 * if she is not at it by then, the people from the shore get it out themselves, and the job is
 * played again. Once she has it, there is no hurry but the clock.
 */
final class Puppy {
    static final int REWARD = 70000;
    // the steps: to the puppy, to the vets', done
    static final int REACH = 0, CLINIC = 1, DONE = 2;
    /** How long the puppy holds out in the water, seconds. */
    static final float STRENGTH = 150f;
    // where it is: so far east of home, so far out from the water's edge; how near she takes it
    static final float AT_X = 8 * City.CELL + 32f, AT_Z = City.SHORE * City.CELL + City.WATERLINE - 22f, TAKE = 2.6f;
    // how near the vets' door is the vets', on foot
    static final float DOOR_NEAR = 4.5f;

    /** Whether mission n is the puppy: the fifth, and every fifth after it. */
    static boolean its(int n) {
        return World.kindOf(n) == 5;
    }

    int step = REACH;
    float clock, doneAt = -1f;
    /** Too late: the people on the shore got it out themselves. */
    boolean late;
    // the puppy in the water: where, the way it faces, its paddling
    float x = AT_X, z = AT_Z, yaw = 0.4f, paddle;

    boolean over() {
        return step == DONE || late;
    }

    /** Whether she carries it. */
    boolean held() {
        return step == CLINIC;
    }

    void update(float dt, Pickup driving, Runner r, World.Host host) {
        if (over()) return;
        clock += dt;
        paddle += dt * (step == REACH ? 2.6f : 0f);
        if (step == REACH) {
            // paddling where it is, turning a little, drifting a little out and back
            yaw = 0.4f + 0.5f * (float) Math.sin(clock * 0.21f);
            x = AT_X + 1.2f * (float) Math.sin(clock * 0.13f);
            z = AT_Z - 0.8f * (float) Math.sin(clock * 0.09f + 1f);
            float dx = r.x - x, dz = r.z - z;
            if (driving == null && dx * dx + dz * dz < TAKE * TAKE && r.y < City.WATER + 1.5f) {
                step = CLINIC;
                host.sound(Synth.CHIME, 0.6f, 1.4f);
                host.buzz(120);
            } else if (clock > STRENGTH) {
                late = true;
                doneAt = clock;
            }
            return;
        }
        if (step == CLINIC) {
            boolean in = driving != null ? Math.abs(driving.speed) < 0.6f && City.inClinicBay(driving.x, driving.z)
                    : near(r.x, r.z, City.clinicDoorX(), City.clinicDoorZ(), DOOR_NEAR) || City.inClinicBay(r.x, r.z);
            if (in) {
                step = DONE;
                doneAt = clock;
                host.sound(Synth.CHIME, 0.5f, 1.3f);
            }
        }
    }

    private static boolean near(float ax, float az, float bx, float bz, float r) {
        float dx = ax - bx, dz = az - bz;
        return dx * dx + dz * dz < r * r;
    }

    /** How long it still holds out, seconds (none, once she has it). */
    float strength() {
        return step == REACH ? Math.max(0f, STRENGTH - clock) : 0f;
    }

    /** Where the way goes now, into out (x, y, z): the puppy, or the vets' bay. */
    boolean goal(float[] out) {
        if (step == REACH) {
            out[0] = x;
            out[1] = City.WATER + 0.6f;
            out[2] = z;
            return true;
        }
        if (step == CLINIC) {
            out[0] = City.clinicDoorX();
            out[1] = 1.2f;
            out[2] = City.clinicBayZ();
            return true;
        }
        return false;
    }

    /** The word for where the way goes now, as Lang keeps it. */
    String goalWord() {
        return step == REACH ? "goal.puppy" : "goal.clinic";
    }

    // ---- drawing ----

    static final int COAT = 0xC8955E, PATCH = 0xF2E6D0, NOSE = 0x1A1614;
    private final float[] rot = new float[9], hand = new float[3];
    private float px, py, pz, c, sn, s;

    /**
     * The puppy: in the water, its head and back over it, paddling; carried, under her arm; at the
     * wheel, on the seat beside her.
     */
    void draw(DrawList d, Runner r, Pickup driving) {
        if (step == DONE || late) return;
        if (step == REACH) {
            float bob = 0.05f * (float) Math.sin(paddle * 1.7f);
            begin(x, City.WATER - 0.12f + bob, z, yaw, 1f);
            body(d, true);
            // the water broken round it
            d.ellipsoid(x, City.WATER - 0.02f, z, 0.42f, 0.03f, 0.5f, rot, 0xD8F0F0, DrawList.MATTE);
            return;
        }
        if (driving != null) {
            // on the seat beside her: the seat's place, across to the other side
            driving.seat(hand);
            float cy = (float) Math.cos(driving.heading), sy = (float) Math.sin(driving.heading);
            float across = -2f * Pickup.SEAT_X;
            begin(hand[0] + cy * across, hand[1] + 0.05f, hand[2] - sy * across, driving.heading, 1f);
            body(d, false);
            return;
        }
        r.bone("hand_l", hand);
        begin(hand[0], hand[1] - 0.25f, hand[2], r.heading, 1f);
        body(d, false);
    }

    private void begin(float x, float y, float z, float yaw, float size) {
        px = x;
        py = y;
        pz = z;
        c = (float) Math.cos(yaw);
        sn = (float) Math.sin(yaw);
        s = size;
        rot[0] = c; rot[1] = 0f; rot[2] = sn;
        rot[3] = 0f; rot[4] = 1f; rot[5] = 0f;
        rot[6] = -sn; rot[7] = 0f; rot[8] = c;
    }

    /** A small round dog: the body, a big head, floppy ears, the paws paddling (swimming) or tucked. */
    private void body(DrawList d, boolean swims) {
        float hip = 0.16f;
        ball(d, 0f, hip, 0f, 0.1f, 0.09f, 0.19f, COAT);
        ball(d, 0f, hip - 0.03f, 0.05f, 0.08f, 0.07f, 0.1f, PATCH);
        float hy = hip + 0.12f, hz = 0.2f;
        ball(d, 0f, hy, hz, 0.085f, 0.08f, 0.085f, COAT);
        ball(d, 0f, hy - 0.03f, hz + 0.08f, 0.045f, 0.04f, 0.055f, PATCH);
        ball(d, 0f, hy - 0.01f, hz + 0.13f, 0.02f, 0.018f, 0.016f, NOSE);
        for (int e = -1; e <= 1; e += 2) {
            ball(d, e * 0.075f, hy - 0.02f, hz - 0.02f, 0.025f, 0.07f, 0.04f, Street.scale(COAT, 0.75f));
            ball(d, e * 0.035f, hy + 0.03f, hz + 0.07f, 0.012f, 0.012f, 0.008f, NOSE);
        }
        for (int e = -1; e <= 1; e += 2)
            for (int end = -1; end <= 1; end += 2) {
                float sw = swims ? 0.07f * (float) Math.sin(paddle * 6f + (e * end > 0 ? 0f : 3.1f)) : 0f;
                ball(d, e * 0.06f, hip - 0.1f, end * 0.11f + sw, 0.03f, 0.05f, 0.03f, COAT);
            }
        ball(d, 0f, hip + 0.06f, -0.2f, 0.02f, 0.02f, 0.06f, COAT);
    }

    private void ball(DrawList d, float lx, float ly, float lz, float hx, float hy, float hz, int rgb) {
        lx *= s;
        ly *= s;
        lz *= s;
        d.ellipsoid(px + c * lx + sn * lz, py + ly, pz - sn * lx + c * lz, hx * s, hy * s, hz * s, rot, rgb, DrawList.MATTE);
    }
}
