package com.shumtugle.skydysh;

/**
 * Freedie on foot, and the eye that follows her. She is led by a stick under the left thumb (see
 * World): pushed a little, she walks; further, she jogs; all the way, she runs; the way it is
 * pushed is the way she goes as the eye sees it, up the glass into the frame, down it toward the
 * eye; let go, she stops in a step or two. The right thumb turns the eye round her; left alone
 * a while, it comes round behind her again as she goes. A press of the jump button jumps her,
 * a second in the air takes her on up as high as a leap would have, over a car or onto what is a
 * storey up; the slide button slides her. Falling from high enough, she puts her arms out and
 * glides down by herself. The mallet's button, or a tap where the eye is turned, is the mallet's
 * (see Toss).
 *
 * The old way, kept for the stand's bots: one finger runs her while it is down, sideways turns
 * her by as much as it moved, a flick up jumps, a flick down slides (see touch).
 * Houses, lamp posts, palms, cars and people stop her, unless she is over them; she stands on
 * whatever she comes down on, the roofs too, and runs off their edges into the air; the curb is
 * a step she takes in her stride.
 *
 * Her heading is the way she runs, in the eye's own terms (0 along +z, growing toward +x); the
 * eye sits behind her and a little above, turning after her a moment late.
 */
final class Runner {
    static final float SIZE = 1.3f, RADIUS = 0.55f;
    static final float TOP_SPEED = 11.5f, PICK_UP = 1.1f, BRAKE = 12f;
    static final float GRAVITY = 22f, JUMP = 8f, LEAP = 12.6f, SINK = 2.2f;
    static final float GLIDE_POSE = 1.2f;              // where in the jump's motion her arms are out
    static final float GLIDE_ABOVE = 4f;               // falling higher than this over what is below, she glides
    // a tap: so short and so still; a lift that keeps her running
    static final float TAP_S = 0.22f, TAP_PX = 14f, GRACE = 0.3f;
    // the finger: frame pixels moved sideways for a quarter turn; how far and how fast a flick goes
    static final float QUARTER = 110f, FLICK_PX = 26f, FLICK_S = 0.16f;
    // the eye: so far behind her, so high, looking so far ahead of her; indoors, nearer
    static final float BACK = 6f, HIGH = 3.4f, AHEAD = 16f, LOOK_UP = 2.4f;
    static final float BACK_IN = 3.4f, HIGH_IN = 2.3f, AHEAD_IN = 8f, LOOK_UP_IN = 1.7f;
    static final float HEAD = 2.45f;                    // her height, for the ceilings indoors

    float x = -1.9f, z = 6f, y, vy;
    float heading;                  // radians: 0 runs along +z
    float speed;
    boolean holding;
    // what she is doing, and since when (seconds of her own clock)
    static final int RUN = 0, AIR = 1, SLIDE = 2, CLIMB = 3, SWIM = 4;
    // swimming: how deep the water must be, how fast she swims, how far under the water's top her
    // middle is, how many strokes a meter
    static final float SWIM_DEEP = 1.4f, SWIM_TOP = 3.4f, SWIM_LOW = 0.36f, STROKE = 0.42f;
    // still in the water: how much lower she settles at last, and in how many seconds
    static final float SWIM_SETTLE = 0.16f, SETTLE_S = 6f;
    private float settled;
    private float strokes;
    static final float WADE = 2.6f;
    // gliding, a flick up beats the arms: so fast up, so many times a flight
    static final float BEAT = 7.5f;
    static final int BEATS = 4;
    // up a ladder so fast, meters a second
    static final float CLIMB_UP = 3f;
    private int beats;
    private float beatAt = -9f;
    // the ladder she is on: its top, the way its wall faces
    private float climbTop, climbNX, climbNZ;
    int state = RUN;
    float stateAt, clock, stride;
    // the eye
    float eyeX, eyeY, eyeZ, eyeYaw, eyePitch;
    private boolean placed;
    // throwing: she stops, turns to (faceX, faceZ) and throws, the finger not heard till it is over
    private float lockAt = -1f, lockFor, faceX, faceZ;

    // the finger: where it was last, and where it was a moment ago, for a flick
    private float lastX, lastY, flickY, flickAt;
    private float downAt = -9f, upAt = -9f, moved;
    // a flick done with: none heard again till the finger has stopped or been lifted
    private boolean flicked;
    // the jump now: where it set off from, and whether it has been flicked on up already
    private float jumpFrom, offEdgeAt = -9f;
    private boolean boosted;
    // a jump just taken from the ground: looked at what is before her, to clear it
    private boolean vault;
    static final float VAULT_LOOK = 1.4f, VAULT_OVER = 0.7f;
    static final float LATE = 0.2f;                    // a flick so soon after running off an edge is still a jump
    /** A tap not yet heard by the mallet. */
    private boolean tapped;
    /** Gliding: falling slowly, arms out, till she lands. */
    boolean gliding;
    /** Hidden: not drawn. */
    boolean hidden;
    /** Indoors: where she is instead of the town; null out of doors. */
    Ground room;
    /** Held still where she stands, the finger not heard (in a lift on its way). */
    boolean held;

    // going down by herself: across the roof to (autoX, autoZ) past its edge, off it, then gliding
    // down along the street (autoH); landed, she is done
    static final float AUTO_GLIDE = 5.5f, AUTO_SINK = 3.4f;
    private boolean auto;
    private float autoX, autoZ, autoH;
    /** Down in the street again, after going down by herself. */
    boolean landed;

    /** Down to the street by herself: over the edge at (ex, ez), then gliding on along heading h. */
    void auto(float ex, float ez, float h) {
        auto = true;
        landed = false;
        autoX = ex;
        autoZ = ez;
        autoH = h;
        holding = false;
    }

    // led where a sign says (see World): to (leadX, leadZ) at a run slowing to a walk, there
    // stopped and turned to leadH; a finger on the glass takes her back
    private boolean led, arrived;
    private float leadX, leadZ, leadH, leadBest, leadSince, leadAside;
    private int leadTries;
    private float leadSide = 1f, leadCommit, leadFlip;

    /** Led by herself to (tx, tz), to stop there facing h. */
    void lead(float tx, float tz, float h) {
        led = true;
        arrived = false;
        leadX = tx;
        leadZ = tz;
        leadH = h;
        leadBest = 1e9f;
        leadSince = 0f;
        leadAside = 0f;
        leadTries = 0;
        leadCommit = 0f;
    }

    void stopLead() {
        led = false;
    }

    /** Whether she is on her way somewhere by herself. */
    boolean led() {
        return led;
    }

    /** Whether she has got where she was led since this was last asked; asked, it is forgotten. */
    boolean arrived() {
        boolean a = arrived;
        arrived = false;
        return a;
    }

    /** On her way where she was led: turned toward it, slowing as she comes; there, turned as she was told. */
    private final float[] probe = new float[2];

    /** Whether the way from her a few steps along heading a is clear of walls and cars (so far, at most). */
    private boolean clear(float a, float far, Ground g, Cars cars) {
        float fx = (float) Math.sin(a), fz = (float) Math.cos(a);
        for (float t = 0.9f; t <= far; t += 0.6f) {
            probe[0] = x + fx * t;
            probe[1] = z + fz * t;
            if (g.push(probe, RADIUS * 0.8f, y)) return false;
            if (cars != null && cars.push(probe, RADIUS * 0.8f, 0f)) return false;
        }
        return true;
    }

    private void guide(float dt, Ground g, Cars cars) {
        float dx = leadX - x, dz = leadZ - z, d = (float) Math.sqrt(dx * dx + dz * dz);
        // the way straight to it blocked a little ahead: the nearest way round that is clear
        if (d > 0.6f) {
            float straight = (float) Math.atan2(dx, dz), far = Math.min(d, 3f);
            leadCommit -= dt;
            if (!clear(straight, far, g, cars)) {
                // round it on the side taken, as long as that side has a way; the other only when it has none
                boolean found = false;
                for (int pass = 0; pass < 2 && !found; pass++) {
                    float side = pass == 0 ? leadSide : -leadSide;
                    if (pass == 1 && leadCommit > 0f) break;
                    for (int k = 1; k <= 7; k++) {
                        float a1 = straight + k * 0.35f * side;
                        if (clear(a1, far, g, cars)) {
                            dx = (float) Math.sin(a1);
                            dz = (float) Math.cos(a1);
                            if (pass == 1 || leadCommit <= 0f) {
                                leadSide = side;
                                leadCommit = 2.5f;
                            }
                            found = true;
                            break;
                        }
                    }
                }
            }
        }
        if (d < leadBest - 0.3f) {
            leadBest = d;
            leadSince = 0f;
            leadFlip = 0f;
        } else {
            leadSince += dt;
            leadFlip += dt;
        }
        // no nearer a while: round the other side; no nearer for a good while: she gives it up
        if (leadFlip > 1.6f) {
            leadSide = -leadSide;
            leadCommit = 2.5f;
            leadFlip = 0f;
        }
        if (leadSince > 7f && d > 0.5f) {
            led = false;
            return;
        }
        float want = d < 0.3f ? leadH : (float) Math.atan2(dx, dz);
        // (dx, dz) may now be the way round rather than to it; d is still the distance to it

        float a = want - heading;
        while (a > Math.PI) a -= 2f * (float) Math.PI;
        while (a < -Math.PI) a += 2f * (float) Math.PI;
        heading += Math.max(-6f * dt, Math.min(6f * dt, a));
        if (d < 0.3f) {
            speed = Math.max(0f, speed - 30f * dt);
            if (speed < 0.2f && Math.abs(a) < 0.15f) {
                led = false;
                arrived = true;
            }
            return;
        }
        // facing away, she turns before she runs
        float top = Math.min(TOP_SPEED * 0.7f, 0.9f + d * 2.2f) * Math.max(0.2f, (float) Math.cos(Math.min(1.5f, Math.abs(a))));
        speed += (top - speed) * Math.min(1f, dt * 4f);

    }

    // ---- the sticks ----
    // the stick: so far pushed, as a part of the way to its rim (x to the right, y down the glass)
    private float stickX, stickY;
    private boolean stickOn;
    // a stick pushed less than this is not heard; walking, jogging, running up to these
    static final float DEAD = 0.14f, WALK_TO = 0.5f, JOG_TO = 0.82f;
    static final float WALK = 1.7f, JOG = 5.2f;
    // how fast she turns to the way she is sent, radians a second; on the ground and in the air
    static final float TURN = 11f, TURN_AIR = 3.5f;
    // how quickly she gets up to the pace wanted, and down from it, a second
    static final float GAIN = 4.5f, LOSE = 9f;
    // the eye turned by hand: so many frame pixels a quarter turn; held so long before it comes
    // round behind her again by itself, at so much a second
    static final float LOOK_QUARTER = 120f, LOOK_HOLD = 1.6f, COME_ROUND = 1.5f;
    private float lookedAt = -9f;

    /** The stick as the left thumb has it: so far each way, as parts of the way to its rim; on or let go. */
    void stick(float sx, float sy, boolean on) {
        if (auto || held) {
            stickOn = false;
            return;
        }
        float m = (float) Math.sqrt(sx * sx + sy * sy);
        if (m > 1f) {
            sx /= m;
            sy /= m;
        }
        stickX = sx;
        stickY = sy;
        stickOn = on;
        if (on && m > DEAD) led = false;
        // on a ladder, pulled down hard: she lets go
        if (on && state == CLIMB && sy > 0.6f) letGo();
    }

    /** The eye turned round her by hand, so many frame pixels across the glass. */
    void look(float dx) {
        eyeYaw += dx / LOOK_QUARTER * (float) (Math.PI / 2);
        lookedAt = clock;
    }

    /** The jump button: up off the ground; in the air, on up; gliding, a beat of the arms. */
    void jumpButton() {
        if (auto || held) return;
        led = false;
        jump();
    }

    /** The slide button: a slide at a run; on a ladder, she lets go. */
    void slideButton() {
        if (auto || held) return;
        if (state == CLIMB) letGo();
        else slide();
    }

    /** A tap for the mallet, from its button or from the eye's side of the glass. */
    void tap() {
        if (auto || held || state == SWIM) return;
        tapped = true;
    }

    /** One finger leads her (and the stand's bots); not so, the stick and the eye by hand, two thumbs. */
    static boolean classic;

    /** How far the stick is pushed now, 0 to 1. */
    float pushed() {
        return stickOn ? (float) Math.sqrt(stickX * stickX + stickY * stickY) : 0f;
    }

    /** The pace the stick asks for: nothing, a walk, a jog or a run. */
    private float paceWanted() {
        float m = pushed();
        if (m < DEAD) return 0f;
        if (m < WALK_TO) return WALK;
        if (m < JOG_TO) return JOG;
        return TOP_SPEED;
    }

    /** A frame led by the stick: turned toward the way it is pushed (as the eye sees it), at the pace it asks. */
    private void sticks(float dt) {
        float want = paceWanted();
        if (want > 0f) {
            float to = eyeYaw + (float) Math.atan2(stickX, -stickY), a = to - heading;
            while (a > Math.PI) a -= 2f * (float) Math.PI;
            while (a < -Math.PI) a += 2f * (float) Math.PI;
            float rate = (state == AIR ? TURN_AIR : TURN) * dt;
            heading += Math.max(-rate, Math.min(rate, a));
            // sent back the way she came: she turns before she picks up again
            if (state != AIR && Math.abs(a) > 1.8f) want *= 0.25f;
        }
        // in the air the stick still sends her on (never back)
        if (state == AIR) {
            if (want > speed) speed += (want - speed) * Math.min(1f, dt * 2.5f);
            return;
        }
        if (state == SLIDE) return;
        speed += (want - speed) * Math.min(1f, dt * (want > speed ? GAIN : LOSE));
        if (want == 0f && speed < 0.15f) speed = 0f;
    }

    void touch(int type, float fx, float fy) {
        if (auto || held) return;
        if (type == World.DOWN) led = false;
        if (type == World.DOWN) {
            holding = true;
            lastX = fx;
            lastY = fy;
            flickY = fy;
            flickAt = clock;
            downAt = clock;
            moved = 0f;
            flicked = false;
        } else if (type == World.MOVE && holding) {
            heading += (fx - lastX) / QUARTER * (float) (Math.PI / 2);
            moved += Math.abs(fx - lastX) + Math.abs(fy - lastY);
            lastX = fx;
            lastY = fy;
            // a flick: far enough, quickly enough, up or down; one at a time
            if (flicked) {
                if (Math.abs(fy - flickY) < 3f) flicked = false;
                flickY = fy;
                flickAt = clock;
            } else if (clock - flickAt > FLICK_S) {
                flickY = fy;
                flickAt = clock;
            } else if (flickY - fy > FLICK_PX) {
                jump();
                flicked = true;
                flickY = fy;
            } else if (fy - flickY > FLICK_PX) {
                if (state == CLIMB) letGo();
                else slide();
                flicked = true;
                flickY = fy;
            }
        } else if (type == World.UP) {
            holding = false;
            upAt = clock;
            if (clock - downAt < TAP_S && moved < TAP_PX) tapped = true;
        }
    }

    /**
     * Up off the ground; in the air, once, on up as high as a leap from where she set off;
     * gliding, a beat of the arms that takes her up again (so many a flight).
     */
    private void jump() {
        if (state == SWIM) return;
        if (state == CLIMB) return;
        if (state == AIR && (gliding || boosted && clock - stateAt > 0.3f)) {
            // gliding, or the jump and the leap from it already spent: the arms out, and a beat of them
            if (beats >= BEATS) return;
            gliding = true;
            beats++;
            beatAt = clock;
            vy = Math.max(vy, BEAT);
            return;
        }
        if (state == AIR && clock - offEdgeAt < LATE) {
            // just off an edge: a jump all the same
            offEdgeAt = -9f;
            jumpFrom = y;
            boosted = false;
            vy = Math.max(vy, JUMP);
            return;
        }
        if (state == AIR) {
            if (boosted || gliding) return;
            boosted = true;
            float left = LEAP * LEAP - 2f * GRAVITY * Math.max(0f, y - jumpFrom);
            vy = Math.max(vy, (float) Math.sqrt(Math.max(0f, left)));
            return;
        }
        state = AIR;
        stateAt = clock;
        jumpFrom = y;
        boosted = false;
        vy = JUMP;
        vault = true;
        // pushing on, she goes on: a jump keeps the way she was being sent, even pressed against a car
        if (!classic && stickOn) speed = Math.max(speed, paceWanted() * 0.9f);
        else if (classic && holding) speed = Math.max(speed, TOP_SPEED * 0.6f);
    }

    /** High, at once: from the ground or on up from a jump (going down by herself). */
    private void leap() {
        if (state != AIR) {
            state = AIR;
            stateAt = clock;
        }
        jumpFrom = y;
        boosted = true;
        vy = LEAP;
    }

    /** Whether there was a tap since this was last asked; asked, it is forgotten. */
    boolean tapped() {
        boolean t = tapped;
        tapped = false;
        return t;
    }

    /** For the desktop stand and for missions: her put down here, facing so. */
    void put(float px, float py, float pz, float h) {
        x = px;
        y = py;
        z = pz;
        heading = h;
        speed = 0f;
        vy = 0f;
        state = RUN;
        gliding = false;
        placed = false;
        auto = false;
        led = false;
        riding = false;
        tapped = false;
        beats = 0;
        stickOn = false;
    }

    /** Onto a ladder: standing at (fx, fz) before its wall (facing nx, nz), up from bottom to top. */
    void climb(float fx, float fz, float bottom, float top, float nx, float nz) {
        led = false;
        boosted = false;
        beats = 0;
        state = CLIMB;
        stateAt = clock;
        x = fx;
        z = fz;
        y = Math.max(y, bottom);
        climbTop = top;
        climbNX = nx;
        climbNZ = nz;
        heading = (float) Math.atan2(-nx, -nz);
        speed = 0f;
        vy = 0f;
        gliding = false;
    }

    /** When she last let go of a ladder (she does not take hold again at once). */
    float letGoAt = -9f;

    /** Off the ladder, back from it, falling. */
    private void letGo() {
        letGoAt = clock;
        state = AIR;
        stateAt = clock;
        x += climbNX * 0.4f;
        z += climbNZ * 0.4f;
        vy = 0f;
        boosted = true;
        jumpFrom = y;
    }

    /** Seconds of her own clock since she let go of a ladder. */
    float sinceLetGo() {
        return clock - letGoAt;
    }

    /** Whether she is on a ladder. */
    boolean climbing() {
        return state == CLIMB;
    }

    /** How many beats of the arms she has left in this flight. */
    int beatsLeft() {
        return BEATS - beats;
    }

    /** Whether her feet are off whatever is under her. */
    boolean inAir() {
        return state == AIR;
    }

    private void slide() {
        if (state == SWIM) return;
        if (state != RUN || speed < 3f) return;
        state = SLIDE;
        stateAt = clock;
    }

    /** She stops where she is, turns to (fx, fz) and throws, for so many seconds. */
    void lock(float seconds, float fx, float fz) {
        lockAt = clock;
        lockFor = seconds;
        faceX = fx;
        faceZ = fz;
        if (state == SLIDE) {
            state = RUN;
            stateAt = clock;
        }
    }

    /** Turns her, while she throws, to (fx, fz): what she throws at runs on. */
    void face(float fx, float fz) {
        faceX = fx;
        faceZ = fz;
    }

    /** How long she has been throwing, seconds; negative when she is not. */
    float throwing() {
        return lockAt >= 0f && clock - lockAt < lockFor ? clock - lockAt : -1f;
    }

    /** The eye put straight behind her again at once. */
    void recenter() {
        placed = false;
    }

    void update(float dt, City city, Cars cars, Walkers walkers) {
        clock += dt;
        Ground g = room != null ? room : city;
        if (held) {
            holding = false;
            speed = 0f;
        }
        if (state == CLIMB) {
            // up the ladder, hand over hand; at the top, over the edge onto the roof
            y += CLIMB_UP * dt;
            if (y >= climbTop) {
                y = climbTop;
                x -= climbNX * 1.3f;
                z -= climbNZ * 1.3f;
                state = RUN;
                stateAt = clock;
                speed = 2f;
            }
            eye(dt, g);
            return;
        }
        if (throwing() >= 0f) {
            // stopped short, turned to what she throws at
            speed = Math.max(0f, speed - 40f * dt);
            float want = (float) Math.atan2(faceX - x, faceZ - z), d = want - heading;
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            heading += Math.max(-10f * dt, Math.min(10f * dt, d));
        } else if (auto) {
            steer(dt);
        } else if (led && state != AIR && state != SLIDE) {
            guide(dt, g, room == null ? cars : null);
        } else if (!classic) {
            sticks(dt);
        } else if (holding || clock - upAt < GRACE) speed += (TOP_SPEED - speed) * Math.min(1f, dt * PICK_UP);
        else if (state != AIR) speed = Math.max(0f, speed - BRAKE * dt);
        if (state == SLIDE) {
            // a slide carries her on, a little slower, then she is up and running again
            speed = Math.max(speed - 3f * dt, 2f);
            if (clock - stateAt > 0.9f) {
                state = RUN;
                stateAt = clock;
            }
        }
        if (vault) {
            // over what stands just before her (a car, a bench, a fence, a wall no higher than a
            // storey): high enough to come down on it or past it
            vault = false;
            float fx0 = (float) Math.sin(heading), fz0 = (float) Math.cos(heading);
            float top = 0f;
            for (float d = 0.6f; d <= VAULT_LOOK; d += 0.4f) {
                float qx = x + fx0 * d, qz = z + fz0 * d;
                top = Math.max(top, g.ground(qx, qz, y, 3.2f));
                if (room == null) top = Math.max(top, cars.topAt(qx, qz));
            }
            float over = top - y + VAULT_OVER;
            if (over > 0.9f) vy = Math.max(vy, (float) Math.sqrt(2f * GRAVITY * over));
        }
        if (state == SWIM) {
            // in the water: slower, turning more slowly, strokes as she goes (slow ones treading water)
            speed = Math.min(speed, SWIM_TOP);
            strokes += dt * (0.35f + speed * STROKE);
            // still, she settles slowly lower, never under; swimming on, she comes up again
            if (speed < 0.4f) settled = Math.min(1f, settled + dt / SETTLE_S);
            else settled = Math.max(0f, settled - dt * 1.5f);
        } else if (room == null && state == RUN && City.overWater(x, z)) {
            // wading in the shallows: slow going
            speed = Math.min(speed, WADE);
        }
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        x += fx * speed * dt;
        z += fz * speed * dt;
        // the open sea has an end: no farther out than this
        if (room == null) z = Math.max(z, City.SHORE * City.CELL + City.OPEN_SEA);
        float[] p = {x, z};
        boolean hit = g.push(p, RADIUS, y);
        if (room == null) {
            float lift = y - city.ground(x, z);
            hit |= cars.push(p, RADIUS, lift) | walkers.push(p, RADIUS, lift);
        }
        if (hit) {
            // ran into something: what was across her way is lost
            float mx = (p[0] - x), mz = (p[1] - z), m = (float) Math.sqrt(mx * mx + mz * mz);
            if (m > 1e-4f) {
                float into = -(fx * mx + fz * mz) / m;
                if (into > 0f) speed *= 1f - 0.8f * into;
            }
        }
        x = p[0];
        z = p[1];
        float bed = room == null ? city.ground(x, z) : 0f;
        boolean deep = room == null && City.overWater(x, z) && bed < City.WATER - SWIM_DEEP;
        if (state == SWIM) {
            // up to the water's top, gently; where it shallows, she stands up and wades out
            y += (City.WATER - y) * Math.min(1f, dt * 4f);
            vy = 0f;
            if (!deep) {
                state = RUN;
                stateAt = clock;
                y = bed;
            }
        } else if (deep && y <= City.WATER + 0.1f) {
            // into deep water, from the shallows or from the air: she swims
            state = SWIM;
            stateAt = clock;
            vy = 0f;
            gliding = false;
            settled = 0f;
            beats = 0;
            speed = Math.min(speed, SWIM_TOP);
        } else if (state == AIR) {
            vy -= GRAVITY * dt;
            // gliding, once past the top of the jump, she sinks slowly
            if (gliding && vy < -(auto ? AUTO_SINK : SINK)) vy = -(auto ? AUTO_SINK : SINK);
            y += vy * dt;
            float ground = g.ground(x, z, y - vy * dt, 0.05f);
            // falling, high over what is below: arms out
            if (vy < 0f && y - ground > GLIDE_ABOVE) gliding = true;
            if (y <= ground && vy < 0f) {
                y = ground;
                vy = 0f;
                state = RUN;
                stateAt = clock;
                gliding = false;
                beats = 0;
                // down by herself: done in the street; on a roof on the way, on to its edge
                if (auto && y < 1f) {
                    auto = false;
                    landed = true;
                }
            }
        } else {
            float ground = g.ground(x, z, y, City.STEP);
            if (ground < y - 0.7f) {
                // off an edge: into the air
                state = AIR;
                stateAt = clock;
                jumpFrom = y;
                boosted = true;
                offEdgeAt = clock;
                vy = 0f;
            } else {
                // the step up onto the pavement taken in a moment, the step down as quick
                y += (ground - y) * Math.min(1f, dt * 18f);
            }
        }
        // a ceiling over her head: no higher than it
        float ceiling = g.ceiling(x, z, y);
        if (y + HEAD > ceiling) {
            y = ceiling - HEAD;
            if (vy > 0f) vy = 0f;
        }
        // the stride: run on in step with the ground going by
        stride += dt * Math.max(0.3f, speed) / strideSpeed();
        eye(dt, g);
    }

    /** Going down by herself: to the edge at a run and off it in a leap; in the air, round along the street. */
    private void steer(float dt) {
        float want;
        if (state == AIR) {
            want = autoH;
            speed += (AUTO_GLIDE - speed) * Math.min(1f, dt * 1.5f);
        } else {
            want = (float) Math.atan2(autoX - x, autoZ - z);
            speed += (TOP_SPEED * 0.8f - speed) * Math.min(1f, dt * 2f);
            float dx = autoX - x, dz = autoZ - z;
            if (dx * dx + dz * dz < 1.6f * 1.6f) {
                leap();
                gliding = true;
            }
        }
        float d = want - heading;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        heading += Math.max(-1.6f * dt, Math.min(1.6f * dt, d));
        if (state == AIR) gliding = true;
    }

    /** How fast the motion now shown carries her when played as it was taken, meters a second. */
    private float strideSpeed() {
        if (speed > 6f) return 7f;
        if (speed > 2.4f) return 3.3f;
        return 1.4f;
    }

    /** Whether she is swimming. */
    boolean swimming() {
        return state == SWIM;
    }

    private String motion() {
        if (state == SWIM) return Frida.SWIM;
        if (state == CLIMB) return Frida.PULL;
        if (state == AIR && clock - beatAt < 0.35f) return "leap_air";
        if (throwing() >= 0f && state != AIR) return "throw";
        if (state == AIR && gliding && vy < 0f) return "jump_air";
        if (state == AIR) return "leap_air";
        if (state == SLIDE) return clock - stateAt < 0.25f ? "slide_start" : clock - stateAt < 0.7f ? "slide" : "slide_end";
        if (speed > 6f) return "sprint";
        if (speed > 2.4f) return "jog";
        if (speed > 0.4f) return "walk";
        return "idle";
    }

    private float motionTime() {
        if (state == SWIM) return strokes;
        if (state == CLIMB) return (clock - stateAt) * CLIMB_UP / (Stroll.PULL_RISE * SIZE);
        if (state == AIR && clock - beatAt < 0.35f) return 0.3f + (clock - beatAt) * 1.4f;
        if (throwing() >= 0f && state != AIR) return throwing();
        if (state == AIR && gliding && vy < 0f) return GLIDE_POSE;
        if (state == AIR) return (clock - stateAt) * 1.4f + 0.3f;
        if (state == SLIDE) {
            float t = clock - stateAt;
            return t < 0.25f ? t * 2.4f : t < 0.7f ? t - 0.25f : (t - 0.7f) * 2.2f;
        }
        if (speed <= 0.4f) return clock;
        return stride * strideLength();
    }

    private float strideLength() {
        if (speed > 6f) return 0.6667f;
        if (speed > 2.4f) return 0.9333f;
        return 1.3333f;
    }

    /** The eye: behind her, a little above, turning after her a moment late, kept out of walls. */
    private void eye(float dt, Ground g) {
        if (!placed) {
            eyeYaw = heading;
            placed = true;
        }
        float d = heading - eyeYaw;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        // led by the sticks, the eye stays where the hand turned it a while; then, as she goes, it
        // comes round behind her, unhurried; standing, it stays. Led by herself, driving, on a
        // ladder, it is behind her as it always was
        boolean byHand = !classic && !riding && !auto && !led && state != CLIMB;
        // driving, it follows behind a beat late: the car is seen to turn, the street does not swing with every touch
        if (!byHand) eyeYaw += d * Math.min(1f, dt * (riding ? 4f : 4.5f));
        else if (clock - lookedAt > LOOK_HOLD && speed > 1.2f)
            eyeYaw += d * Math.min(1f, dt * COME_ROUND * Math.min(1f, speed / JOG));
        while (eyeYaw - heading > Math.PI) eyeYaw -= 2f * (float) Math.PI;
        while (heading - eyeYaw > Math.PI) eyeYaw += 2f * (float) Math.PI;
        float fx = (float) Math.sin(eyeYaw), fz = (float) Math.cos(eyeYaw);
        boolean in = room != null;
        float back = in ? BACK_IN : BACK, high = in ? HIGH_IN : HIGH;
        // on a ladder: farther back and lower, looking up past her at the wall she climbs
        boolean up = state == CLIMB;
        if (up) {
            back = CLIMB_BACK;
            high = CLIMB_HIGH;
        }
        boolean swims = state == SWIM && !riding;
        if (swims) {
            // swimming: nearer and looking down on her a little, her shoulders and hands in the frame
            back = SWIM_BACK;
            high = SWIM_HIGH;
        }
        if (riding) {
            // farther back and higher the faster it goes, so what comes is seen in time
            float fast = Math.min(1f, Math.abs(speed) / Pickup.VMAX);
            back = RIDE_BACK + 5f * fast;
            high = RIDE_HIGH + 1.5f * fast;
        }
        // back from her along the way she looks, as far as nothing is in the way
        // what the eye bumps into: what stands round it, not what is low under it or high over it
        float r = in ? 0.3f : 0.4f, feet = y + high - (in ? 1f : City.HEADROOM - 0.3f);
        float[] p = {x, z};
        float free = 0f;
        for (float t = EYE_STEP; ; t += EYE_STEP) {
            float tt = Math.min(t, back), qx = x - fx * tt, qz = z - fz * tt;
            eyeTry[0] = qx;
            eyeTry[1] = qz;
            g.push(eyeTry, r, feet);
            float mx = eyeTry[0] - qx, mz = eyeTry[1] - qz;
            if (mx * mx + mz * mz > 0.04f * 0.04f && tt > EYE_STEP) break;
            p[0] = eyeTry[0];
            p[1] = eyeTry[1];
            free = tt;
            if (tt >= back) break;
        }
        g.keepEye(p);
        eyeX = p[0];
        eyeZ = p[1];
        // with her back to a wall the eye has no room behind her: it rises and looks down on her
        // instead, as far as a ceiling lets it
        float short_ = Math.max(0f, Math.min(back, NEAR_BACK) - free) / NEAR_BACK;
        eyeY = Math.min(y + high + short_ * RISE, g.ceiling(eyeX, eyeZ, y + 0.5f) - 0.25f);
        // and looks down at her rather than over her, so that she stays in the frame
        float ahead = up ? 0f : (in ? AHEAD_IN : riding ? RIDE_AHEAD : swims ? SWIM_AHEAD : AHEAD) * (1f - short_);
        float tx = x + fx * ahead, tz = z + fz * ahead;
        float ty = y + ((in ? LOOK_UP_IN : swims ? SWIM_LOOK : LOOK_UP) + (up ? CLIMB_LOOK : 0f)) * (1f - short_) + 0.8f * short_;
        float dx = tx - eyeX, dz = tz - eyeZ;
        eyePitch = (float) Math.atan2(ty - eyeY, Math.sqrt(dx * dx + dz * dz));
        // never so high over her, looking on, that she drops out of the bottom of the frame
        float hx = x - eyeX, hz = z - eyeZ;
        float below = (float) Math.atan2(eyeY - (y + 1f), Math.max(0.3f, (float) Math.sqrt(hx * hx + hz * hz)));
        eyePitch = Math.min(eyePitch, -(below - KEEP_IN));
    }

    private static final float EYE_STEP = 0.3f;
    // the eye with less room behind her than this rises so much higher at most (none at all)
    static final float NEAR_BACK = 2.4f, RISE = 4.5f;
    // how far below the middle of the frame she may be at most, radians
    static final float KEEP_IN = 0.45f;
    // the eye with her on a ladder: so far back, so high over her feet, looking so much higher than it would
    static final float CLIMB_BACK = 10f, CLIMB_HIGH = 1.2f, CLIMB_LOOK = 2f;
    private final float[] eyeTry = new float[2];

    /** Where one of her bones is now, into out (x, y, z). */
    // ---- at the wheel of a pickup: carried with it, the eye farther back and higher ----
    /** In a pickup: where it is, she is; drawn sitting at its wheel. */
    boolean riding;
    private float seatX, seatY, seatZ, wheel;
    static final float RIDE_BACK = 10f, RIDE_HIGH = 4.4f, RIDE_AHEAD = 20f;
    // the eye on her swimming: so far back, so high over the water, looking so far on and so high there
    static final float SWIM_BACK = 4.6f, SWIM_HIGH = 3.1f, SWIM_AHEAD = 9f, SWIM_LOOK = 0.4f;
    // the seat, from the hips down: the figure stands so much lower than the seat's top
    static final float SEAT_DROP = 1.12f;

    /** Carried with the pickup: it at (px, py, pz) going along h, her seat at (sx, sy, sz), its wheel turned so. */
    void ride(float dt, Ground city, float px, float py, float pz, float h, float v, float sx, float sy, float sz, float turn) {
        if (!riding) {
            riding = true;
            state = RUN;
            stateAt = clock;
            led = false;
            holding = false;
            gliding = false;
            vy = 0f;
        }
        clock += dt;
        x = px;
        y = py;
        z = pz;
        heading = h;
        speed = v;
        seatX = sx;
        seatY = sy;
        seatZ = sz;
        wheel = turn;
        eye(dt, city);
    }

    /** The eye put where it is told (x, y, z, yaw, pitch): a fixed view, while she sits. */
    void eyeAt(float[] e) {
        eyeX = e[0];
        eyeY = e[1];
        eyeZ = e[2];
        eyeYaw = e[3];
        eyePitch = e[4];
    }

    /** Sitting at ease (on a sofa), not at a wheel. */
    boolean lounging;

    /** Out of the pickup, standing at (px, pz) facing h. */
    void getOut(float px, float py, float pz, float h) {
        riding = false;
        lounging = false;
        put(px, py, pz, h);
    }

    void bone(String name, float[] out) {
        if (riding) {
            Frida.place(name, seatX, seatY - SEAT_DROP, seatZ, heading + (float) Math.PI, SIZE, lounging ? Frida.LOUNGE : Frida.SIT,
                    lounging ? clock : wheel, out);
            return;
        }
        Frida.place(name, x, y, z, heading + (float) Math.PI, SIZE, motion(), motionTime(), out);
    }

    void draw(DrawList d) {
        if (hidden) return;
        // the figure faces -z at no turn: turned half round, she runs the way she is headed
        if (riding) {
            if (lounging) Frida.draw(d, seatX, seatY - SEAT_DROP, seatZ, heading + (float) Math.PI, SIZE, Frida.LOUNGE, clock);
            else Frida.draw(d, seatX, seatY - SEAT_DROP, seatZ, heading + (float) Math.PI, SIZE, Frida.SIT, wheel);
            return;
        }
        if (state == SWIM) {
            // low in the water, the water broken round her and in a wake behind as she swims
            // upright, bobbing a little with each stroke
            float rise = 0.04f * (float) Math.sin(strokes * 6.28f - 1f);
            float low = SWIM_LOW + SWIM_SETTLE * settled * settled * (3f - 2f * settled);
            Frida.draw(d, x, y - low * SIZE + rise, z, heading + (float) Math.PI, SIZE, Frida.SWIM, strokes);
            float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
            for (int k = 0; k < 4; k++) {
                float back = 2f + k * 0.8f, wide = 0.7f + k * 0.3f, f = 1f - k / 4f;
                if (k > 0 && speed < 0.5f) break;
                float bob = 0.05f * (float) Math.sin(strokes * 6.28f + k);
                for (int e = -1; e <= 1; e += 2) {
                    float wx = x - fx * back + fz * e * (wide + bob), wz = z - fz * back - fx * e * (wide + bob);
                    d.box(wx, y + 0.01f, wz, 0.05f * f + 0.03f, 0.008f, 0.09f * f + 0.04f, null, 0xBCD9D8, DrawList.PLAIN);
                }
            }
            return;
        }
        Frida.draw(d, x, y, z, heading + (float) Math.PI, SIZE, motion(), motionTime());
    }
}
