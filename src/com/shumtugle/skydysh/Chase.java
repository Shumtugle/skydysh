package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A chase: Freedie on fire runs off through the town and the player runs after her. She keeps
 * to the streets, on the right of the road, and at every crossing goes on, or turns left or
 * right, as it falls out; she never turns back. Near, she bolts; far behind her, she slows and
 * lets the chase come up; left very far behind, she stops, folds her arms and waits.
 *
 * Caught (the runner within reach of her), she is jolted, counted, and breaks loose: for a few
 * seconds she runs faster than anyone can and cannot be caught, and the chase goes on.
 *
 * The cars do not stop for her: she runs in their lane. Struck by one, she is thrown the way it
 * was going, falls on her back and lies a while; then she gets up and runs on. Caught lying
 * there, she is counted all the same, gets up and breaks loose.
 */
final class Chase {
    static final float SIZE = 1.3f;
    static final float LANE = 1.6f;            // right of the road's middle
    static final float RUN = 8.6f, BOLT = 9.6f, LAG = 6.5f, BREAK = 12.5f;
    static final float NEAR = 10f, FAR = 70f, WAIT = 160f, RESUME = 50f, REACH = 2.2f;
    static final float BREAK_TIME = 3f, JOLT = 0.8f;
    // struck: the fall begun past its first stagger and played faster; lying; getting up again
    static final float FALL_FROM = 0.5f, FALL_PACE = 1.5f, FALL_END = 1.25f, LIE = 1.6f, RISE_PACE = 1.3f;

    static final int FLEE = 0, JOLTED = 1, LOOSE = 2, WAITING = 3, DOWN = 4, RISING = 5;

    float x = LANE, z = 34f, y, heading, speed;
    int state = FLEE;
    float stateAt, clock;
    int caught, struck;
    // thrown by a car: sliding along the ground, off it a little
    private float vx, vz, lift, vy;
    private boolean caughtDown;
    // the crossing she runs to, and the way she is going (one of the four, as cell steps)
    private int ti = 0, tj = 1, di = 0, dj = 1;
    private final Random rnd = new Random(0xF1AEL);
    private float stride;

    /** The distance to the runner, as last worked out. */
    float apart;

    void update(float dt, Runner r, City city, Cars cars) {
        clock += dt;
        float dx = r.x - x, dz = r.z - z;
        apart = (float) Math.sqrt(dx * dx + dz * dz);
        float want;
        switch (state) {
            case JOLTED:
                want = 0f;
                if (clock - stateAt > JOLT) set(LOOSE);
                break;
            case LOOSE:
                want = BREAK;
                if (clock - stateAt > BREAK_TIME) set(FLEE);
                break;
            case WAITING:
                want = 0f;
                if (apart < RESUME) set(FLEE);
                break;
            case DOWN:
                want = 0f;
                if (apart < REACH && !caughtDown) {
                    caught++;
                    caughtDown = true;
                }
                if (caughtDown || clock - stateAt > (FALL_END - FALL_FROM) / FALL_PACE + LIE) set(RISING);
                break;
            case RISING:
                want = 0f;
                if (clock - stateAt > FALL_END / RISE_PACE) {
                    set(caughtDown ? LOOSE : FLEE);
                    onWay();
                }
                break;
            default:
                if (apart < REACH) {
                    caught++;
                    set(JOLTED);
                    want = 0f;
                } else if (apart > WAIT) {
                    set(WAITING);
                    want = 0f;
                } else {
                    // near, she bolts; far ahead, she lets the chase come up
                    float k = Math.max(0f, Math.min(1f, (apart - NEAR) / (FAR - NEAR)));
                    want = apart < NEAR ? BOLT : RUN + (LAG - RUN) * k;
                }
        }
        if (state != DOWN && state != RISING && state != JOLTED) {
            int c = cars.striking(x, z, 0.55f);
            if (c >= 0) strike(cars, c);
        }
        if (state == DOWN || state == RISING) {
            lying(dt, city, cars);
            return;
        }
        speed += (want - speed) * Math.min(1f, dt * 3f);
        // toward the next crossing, kept on her side of the road
        float rx = (float) dj, rz = (float) -di;
        float tx = ti * City.CELL + rx * LANE, tz = tj * City.CELL + rz * LANE;
        float ex = tx - x, ez = tz - z;
        if (ex * ex + ez * ez < 2.5f * 2.5f) {
            turn();
            rx = dj;
            rz = -di;
            tx = ti * City.CELL + rx * LANE;
            tz = tj * City.CELL + rz * LANE;
            ex = tx - x;
            ez = tz - z;
        }
        float aim = (float) Math.atan2(ex, ez);
        float d = aim - heading;
        while (d > Math.PI) d -= 2f * (float) Math.PI;
        while (d < -Math.PI) d += 2f * (float) Math.PI;
        float most = 5f * dt;
        heading += Math.max(-most, Math.min(most, d));
        x += (float) Math.sin(heading) * speed * dt;
        z += (float) Math.cos(heading) * speed * dt;
        float[] p = {x, z};
        city.push(p, 0.55f);
        cars.push(p, 0.55f);
        x = p[0];
        z = p[1];
        y += (city.ground(x, z) - y) * Math.min(1f, dt * 18f);
        stride += dt * Math.max(0.3f, speed) / (speed > 6f ? 7f : 3.3f);
    }

    /**
     * Struck by car c: thrown off her feet the way it goes and off to the side she was on (the
     * curb side, if she was dead ahead), turned so that she falls on her back.
     */
    private void strike(Cars cars, int c) {
        float h = cars.heading[c], v = cars.speed[c];
        float fx = (float) Math.sin(h), fz = (float) Math.cos(h), rx = fz, rz = -fx;
        float off = (x - cars.x[c]) * rx + (z - cars.z[c]) * rz;
        float side = off < -0.3f ? -1f : 1f;
        float tx = fx * 0.8f + rx * side * 0.6f, tz = fz * 0.8f + rz * side * 0.6f;
        heading = (float) Math.atan2(tx, tz) + (float) Math.PI;
        float flung = v * 0.8f + 1.5f;
        vx = tx * flung;
        vz = tz * flung;
        vy = 2.5f + v * 0.18f;
        speed = 0f;
        caughtDown = false;
        struck++;
        cars.struck(c);
        set(DOWN);
    }

    /** Thrown and down: she slides to a stop on the ground, kept out of the houses and the cars. */
    private void lying(float dt, City city, Cars cars) {
        float drag = Math.max(0f, 1f - dt * (lift > 0f ? 0.6f : 5f));
        vx *= drag;
        vz *= drag;
        x += vx * dt;
        z += vz * dt;
        vy -= 22f * dt;
        lift = Math.max(0f, lift + vy * dt);
        if (lift == 0f) vy = 0f;
        float[] p = {x, z};
        city.push(p, 0.55f);
        cars.push(p, 0.55f);
        x = p[0];
        z = p[1];
        y += (city.ground(x, z) - y) * Math.min(1f, dt * 18f);
    }

    /** Up again: if she was thrown past the crossing she was running to, she runs on from it. */
    private void onWay() {
        float tx = ti * City.CELL, tz = tj * City.CELL;
        if ((tx - x) * di + (tz - z) * dj < 0f) turn();
    }

    /** Whether she is on the ground or getting up: the cars stop for her then. */
    boolean down() {
        return state == DOWN || state == RISING || state == JOLTED;
    }

    private void set(int s) {
        state = s;
        stateAt = clock;
    }

    /** At a crossing: on, left or right, as it falls out; then on to the next one that way. */
    private void turn() {
        int roll = rnd.nextInt(4);
        int ni = di, nj = dj;
        if (roll == 2) {
            ni = -dj;
            nj = di;
        } else if (roll == 3) {
            ni = dj;
            nj = -di;
        }
        di = ni;
        dj = nj;
        ti += di;
        tj += dj;
    }

    private String motion() {
        if (state == DOWN || state == RISING) return "fall";
        if (state == JOLTED) return "knockback";
        if (state == WAITING) return speed < 0.4f ? "arms_folded" : "walk";
        if (speed > 6f) return "sprint";
        if (speed > 2.4f) return "jog";
        if (speed > 0.4f) return "walk";
        return "idle";
    }

    private float motionTime() {
        if (state == DOWN) return FALL_FROM + (clock - stateAt) * FALL_PACE;
        // getting up: the fall played back
        if (state == RISING) return Math.max(0f, FALL_END - (clock - stateAt) * RISE_PACE);
        if (state == JOLTED) return clock - stateAt;
        if (state == WAITING || speed <= 0.4f) return clock - stateAt;
        return stride * (speed > 6f ? 0.6667f : speed > 2.4f ? 0.9333f : 1.3333f);
    }

    private final float[] at = new float[3];

    /**
     * She burns: her light on the street and the houses round her, and, where the eye can see
     * her, a glow printed round her over the frame.
     */
    void draw(DrawList d) {
        Frida.draw(d, x, y + lift, z, heading + (float) Math.PI, SIZE, motion(), motionTime(), true);
        float flick = 0.92f + 0.08f * (float) Math.sin(clock * 11f) * (float) Math.sin(clock * 4.3f + 1f);
        d.fire[0] = x;
        d.fire[1] = y + 1.6f;
        d.fire[2] = z;
        d.fire[3] = 1.3f * flick;
        if (d.project(x, y + 1.45f, z, at)) {
            d.halo[0] = at[0];
            d.halo[1] = at[1];
            d.halo[2] = d.focal * 1.0f / Math.max(1f, at[2]);
            d.halo[4] = at[2];
            // fainter far off, as the haze takes her
            d.halo[3] = flick * Math.max(0f, Math.min(1f, 1.2f - at[2] / 120f));
        }
    }
}
