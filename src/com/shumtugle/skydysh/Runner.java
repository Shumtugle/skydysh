package com.shumtugle.skydysh;

/**
 * Freedie on foot, and the eye that follows her: she runs while a finger is on the glass and
 * slows to a stop when it lifts (not at once: a quick lift and touch again keeps her going); the
 * finger leads her: moved sideways, she turns by as much as it moved, wherever it lies on the
 * glass. Flicked up, she jumps; flicked up again while she is in the air, she goes on up as high
 * as a leap would have taken her, over a car or onto what is a storey up; flicked down, she
 * slides. Falling from high enough, she puts her arms out and glides down by herself. A tap is
 * not hers: it is the mallet's (see Toss).
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
    static final int RUN = 0, AIR = 1, SLIDE = 2;
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

    void touch(int type, float fx, float fy) {
        if (auto || held) return;
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
                slide();
                flicked = true;
                flickY = fy;
            }
        } else if (type == World.UP) {
            holding = false;
            upAt = clock;
            if (clock - downAt < TAP_S && moved < TAP_PX) tapped = true;
        }
    }

    /** Up off the ground; in the air, once, on up as high as a leap from where she set off. */
    private void jump() {
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
        tapped = false;
    }

    /** Whether her feet are off whatever is under her. */
    boolean inAir() {
        return state == AIR;
    }

    private void slide() {
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
        if (throwing() >= 0f) {
            // stopped short, turned to what she throws at
            speed = Math.max(0f, speed - 40f * dt);
            float want = (float) Math.atan2(faceX - x, faceZ - z), d = want - heading;
            while (d > Math.PI) d -= 2f * (float) Math.PI;
            while (d < -Math.PI) d += 2f * (float) Math.PI;
            heading += Math.max(-10f * dt, Math.min(10f * dt, d));
        } else if (auto) {
            steer(dt);
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
        float fx = (float) Math.sin(heading), fz = (float) Math.cos(heading);
        x += fx * speed * dt;
        z += fz * speed * dt;
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
        if (state == AIR) {
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
        float ceiling = g.ceiling(x, z);
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

    private String motion() {
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
        eyeYaw += d * Math.min(1f, dt * 4.5f);
        float fx = (float) Math.sin(eyeYaw), fz = (float) Math.cos(eyeYaw);
        boolean in = room != null;
        float back = in ? BACK_IN : BACK, high = in ? HIGH_IN : HIGH;
        // back from her along the way she looks, as far as nothing is in the way
        float r = in ? 0.3f : 0.4f, feet = y + high - 1f;
        float[] p = {x, z};
        for (float t = EYE_STEP; ; t += EYE_STEP) {
            float tt = Math.min(t, back), qx = x - fx * tt, qz = z - fz * tt;
            eyeTry[0] = qx;
            eyeTry[1] = qz;
            g.push(eyeTry, r, feet);
            float mx = eyeTry[0] - qx, mz = eyeTry[1] - qz;
            if (mx * mx + mz * mz > 0.04f * 0.04f && tt > EYE_STEP) break;
            p[0] = eyeTry[0];
            p[1] = eyeTry[1];
            if (tt >= back) break;
        }
        g.keepEye(p);
        eyeX = p[0];
        eyeZ = p[1];
        eyeY = Math.min(y + high, g.ceiling(eyeX, eyeZ) - 0.25f);
        float ahead = in ? AHEAD_IN : AHEAD;
        float tx = x + fx * ahead, tz = z + fz * ahead, ty = y + (in ? LOOK_UP_IN : LOOK_UP);
        float dx = tx - eyeX, dz = tz - eyeZ;
        eyePitch = (float) Math.atan2(ty - eyeY, Math.sqrt(dx * dx + dz * dz));
    }

    private static final float EYE_STEP = 0.3f;
    private final float[] eyeTry = new float[2];

    /** Where one of her bones is now, into out (x, y, z). */
    void bone(String name, float[] out) {
        Frida.place(name, x, y, z, heading + (float) Math.PI, SIZE, motion(), motionTime(), out);
    }

    void draw(DrawList d) {
        if (hidden) return;
        // the figure faces -z at no turn: turned half round, she runs the way she is headed
        Frida.draw(d, x, y, z, heading + (float) Math.PI, SIZE, motion(), motionTime());
    }
}
