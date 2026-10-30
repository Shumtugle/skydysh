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
 */
final class Chase {
    static final float SIZE = 1.3f;
    static final float LANE = 1.6f;            // right of the road's middle
    static final float RUN = 8.6f, BOLT = 9.6f, LAG = 6.5f, BREAK = 12.5f;
    static final float NEAR = 10f, FAR = 70f, WAIT = 160f, RESUME = 50f, REACH = 2.2f;
    static final float BREAK_TIME = 3f, JOLT = 0.8f;

    static final int FLEE = 0, JOLTED = 1, LOOSE = 2, WAITING = 3;

    float x = LANE, z = 34f, y, heading, speed;
    int state = FLEE;
    float stateAt, clock;
    int caught;
    // the crossing she runs to, and the way she is going (one of the four, as cell steps)
    private int ti = 0, tj = 1, di = 0, dj = 1;
    private final Random rnd = new Random(0xF1AEL);
    private float stride;

    /** The distance to the runner, as last worked out. */
    float apart;

    void update(float dt, Runner r, City city) {
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
        x = p[0];
        z = p[1];
        y += (city.ground(x, z) - y) * Math.min(1f, dt * 18f);
        stride += dt * Math.max(0.3f, speed) / (speed > 6f ? 7f : 3.3f);
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
        if (state == JOLTED) return "knockback";
        if (state == WAITING) return speed < 0.4f ? "arms_folded" : "walk";
        if (speed > 6f) return "sprint";
        if (speed > 2.4f) return "jog";
        if (speed > 0.4f) return "walk";
        return "idle";
    }

    private float motionTime() {
        if (state == JOLTED) return clock - stateAt;
        if (state == WAITING || speed <= 0.4f) return clock - stateAt;
        return stride * (speed > 6f ? 0.6667f : speed > 2.4f ? 0.9333f : 1.3333f);
    }

    void draw(DrawList d) {
        Frida.draw(d, x, y, z, heading + (float) Math.PI, SIZE, motion(), motionTime(), true);
        // flames off her shoulders and head, rising and dying away
        for (int k = 0; k < 8; k++) {
            float ph = (clock * 1.7f + k / 8f) % 1f;
            float ox = (float) Math.sin(k * 2.1f) * 0.35f, oz = (float) Math.cos(k * 1.7f) * 0.25f;
            float s = 0.13f * (1f - ph);
            int c = Street.mix(0xFFE27A, 0xE8401A, ph);
            d.box(x + ox, y + 2.1f + ph * 1.4f, z + oz, s, s * 1.4f, s, null, c, DrawList.GLOW);
        }
    }
}
