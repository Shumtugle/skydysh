package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A crowd at the water's edge, there for the puppy out at sea: not a row of posts but people about
 * something. Some shift where they stand and step a little this way and that, looking out; some
 * walk up and down along the water; some stand in twos talking, turning now and then to look out
 * again; some call out to it over the water, hands cupped at the mouth; a few wade in to the knee
 * and stand there, and come out again. All of it as long as the puppy is out there.
 */
final class Crowd {
    static final int MANY = 34;
    // what one does: shifts where they stand; up and down the water's edge; in twos, talking;
    // calls out over the water; wades in and out again
    static final int SHIFT = 0, STROLL = 1, TALK = 2, CALL = 3, WADE = 4;
    // how fast they walk, and step about where they stand; how far they step about
    static final float PACE = 1.05f, SHUFFLE = 0.55f, ABOUT = 0.7f;
    // the water at the knee: so far under the water's top the sea's bed is where a wader stops
    static final float KNEE = 0.45f;
    // seen and minded only so near
    static final float NEAR = 170f;

    private final City city;
    private final float sx, sz;
    private final int[] role = new int[MANY], model = new int[MANY], phase = new int[MANY], mate = new int[MANY];
    private final Stroll.Way[] way = new Stroll.Way[MANY];
    private final float[] x = new float[MANY], z = new float[MANY], yaw = new float[MANY];
    private final float[] hx = new float[MANY], hz = new float[MANY], tx = new float[MANY], tz = new float[MANY];
    private final float[] ax = new float[MANY], bx = new float[MANY], wz = new float[MANY];
    private final float[] walked = new float[MANY], walking = new float[MANY], wait = new float[MANY], call = new float[MANY];
    private final Random rnd = new Random(0xC70DL);
    private boolean dressed;
    private float clock;
    // the old figures, where the town has no people of its own to dress them from
    private final Walkers people = new Walkers();

    /** A crowd on the sand round (cx, the water's edge), there for what is out at (sx, sz). */
    Crowd(City city, float cx, float sx, float sz) {
        this.city = city;
        this.sx = sx;
        this.sz = sz;
        people.update(0f, new Runner(), new Chase());
        float edge = City.SHORE * City.CELL + City.WATERLINE + 1.2f;
        int n = 0;
        // in twos, talking: four pairs here and there among the rest
        for (int k = 0; k < 4; k++) {
            float px = cx - 12f + k * 7.5f + (rnd.nextFloat() - 0.5f) * 2f, pz = edge + 1.2f + rnd.nextFloat() * 2.2f;
            float t = rnd.nextFloat() * 6.283f;
            for (int e = -1; e <= 1; e += 2, n++) {
                role[n] = TALK;
                hx[n] = px + (float) Math.sin(t) * 0.5f * e;
                hz[n] = pz + (float) Math.cos(t) * 0.5f * e;
                mate[n] = e < 0 ? n + 1 : n - 1;
            }
        }
        // up and down along the water
        for (int k = 0; k < 6; k++, n++) {
            role[n] = STROLL;
            float mid = cx - 9f + k * 3.6f, half = 4f + rnd.nextFloat() * 5f;
            ax[n] = mid - half;
            bx[n] = mid + half;
            hx[n] = mid + (rnd.nextFloat() - 0.5f) * half;
            hz[n] = edge + 0.3f + rnd.nextFloat() * 2.4f;
            phase[n] = rnd.nextInt(2);
        }
        // calling out to it, at the front
        for (int k = 0; k < 6; k++, n++) {
            role[n] = CALL;
            hx[n] = cx - 10f + k * 4f + (rnd.nextFloat() - 0.5f) * 1.5f;
            hz[n] = edge + rnd.nextFloat() * 0.6f;
        }
        // into the water to the knee, and out again
        for (int k = 0; k < 3; k++, n++) {
            role[n] = WADE;
            hx[n] = cx - 6f + k * 6f + (rnd.nextFloat() - 0.5f) * 2f;
            hz[n] = edge + 0.2f;
            float q = hz[n];
            while (q > edge - 30f && city.ground(hx[n], q) > City.WATER - KNEE) q -= 0.25f;
            wz[n] = q;
        }
        // the rest, standing about in a loose bunch, shifting where they stand
        for (; n < MANY; n++) {
            role[n] = SHIFT;
            hx[n] = cx - 13f + rnd.nextFloat() * 26f;
            hz[n] = edge + 0.4f + rnd.nextFloat() * 3.6f;
        }
        for (int i = 0; i < MANY; i++) {
            x[i] = tx[i] = hx[i];
            z[i] = tz[i] = hz[i];
            yaw[i] = role[i] == TALK ? toward(i, x[mate[i]], z[mate[i]]) : out(i);
            wait[i] = rnd.nextFloat() * 4f;
            model[i] = rnd.nextInt(1 << 16);
        }
        // the talkers' mates: each faces the other
        for (int i = 0; i < MANY; i++)
            if (role[i] == TALK) yaw[i] = toward(i, x[mate[i]], z[mate[i]]);
    }

    /** Dressed once the town's people are there to dress them from. */
    private void dress() {
        if (dressed || FolkPack.town == null) return;
        dressed = true;
        Random r = new Random(0xBEAC4L);
        for (int i = 0; i < MANY; i++) {
            model[i] = model[i] % FolkPack.town.count;
            way[i] = Stroll.way(r.nextLong(), FolkPack.town.woman(model[i]));
        }
    }

    /** The way to look from i out at what is in the water, each a little off. */
    private float out(int i) {
        return toward(i, sx, sz) + ((i * 0.37f) % 0.6f - 0.3f);
    }

    private float toward(int i, float px, float pz) {
        return (float) Math.atan2(px - x[i], pz - z[i]);
    }

    /** A step on, her (at ex, ez) far or near: they shift and walk and talk and call. */
    void update(float dt, float ex, float ez) {
        float dx0 = ex - sx, dz0 = ez - sz;
        if (dx0 * dx0 + dz0 * dz0 > NEAR * NEAR * 2f) return;
        dress();
        clock += dt;
        for (int i = 0; i < MANY; i++) {
            wait[i] -= dt;
            call[i] = Math.max(0f, call[i] - dt);
            switch (role[i]) {
                case SHIFT:
                    // a step or two this way or that, then standing a while, looking out
                    if (walkOn(i, dt, SHUFFLE)) break;
                    face(i, out(i) + 0.25f * (float) Math.sin(clock * 0.4f + i), dt, 1.5f);
                    if (wait[i] < 0f) {
                        wait[i] = 2f + rnd.nextFloat() * 4f;
                        tx[i] = hx[i] + (rnd.nextFloat() - 0.5f) * 2f * ABOUT;
                        tz[i] = hz[i] + (rnd.nextFloat() - 0.5f) * 2f * ABOUT;
                    }
                    break;
                case STROLL:
                    // to one end, a look out there, back to the other
                    if (walkOn(i, dt, PACE)) break;
                    face(i, out(i), dt, 2f);
                    if (wait[i] < 0f) {
                        phase[i] ^= 1;
                        tx[i] = phase[i] == 0 ? ax[i] : bx[i];
                        tz[i] = hz[i] + (rnd.nextFloat() - 0.5f) * 1.2f;
                        wait[i] = 1.5f + rnd.nextFloat() * 3f;
                    }
                    break;
                case TALK: {
                    // to each other, turning now and then to look out; a little sway as they talk
                    float to = toward(i, x[mate[i]], z[mate[i]]);
                    if (wait[i] < 0f) {
                        phase[i] ^= 1;
                        wait[i] = phase[i] == 1 ? 1.2f + rnd.nextFloat() * 1.5f : 3f + rnd.nextFloat() * 4f;
                    }
                    walking[i] = Math.max(0f, walking[i] - dt * 3f);
                    face(i, phase[i] == 1 ? out(i) : to + 0.12f * (float) Math.sin(clock * 1.3f + i * 2f), dt, 2.2f);
                    break;
                }
                case CALL:
                    // cupped hands, a call out over the water; a breath; again
                    walking[i] = Math.max(0f, walking[i] - dt * 3f);
                    face(i, out(i), dt, 2f);
                    if (wait[i] < 0f) {
                        call[i] = 1.1f + rnd.nextFloat() * 0.9f;
                        wait[i] = call[i] + 1.4f + rnd.nextFloat() * 2.5f;
                    }
                    break;
                case WADE:
                    // in to the knee, standing there calling a while, out again, a while on the sand
                    if (walkOn(i, dt, PACE * 0.7f)) break;
                    face(i, out(i), dt, 2f);
                    if (phase[i] == 1 && call[i] <= 0f && wait[i] > 1.5f && (int) (clock * 0.7f + i) % 3 == 0) call[i] = 1.4f;
                    if (wait[i] < 0f) {
                        phase[i] ^= 1;
                        tx[i] = hx[i] + (rnd.nextFloat() - 0.5f) * 1.5f;
                        tz[i] = phase[i] == 1 ? wz[i] : hz[i];
                        wait[i] = phase[i] == 1 ? 6f + rnd.nextFloat() * 5f : 4f + rnd.nextFloat() * 6f;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** On the way to (tx, tz) at so much of a walk, if not there yet: whether still on it. */
    private boolean walkOn(int i, float dt, float pace) {
        float ex = tx[i] - x[i], ez = tz[i] - z[i], l = (float) Math.sqrt(ex * ex + ez * ez);
        if (l < 0.08f) {
            walking[i] = Math.max(0f, walking[i] - dt * 3f);
            return false;
        }
        walking[i] = Math.min(1f, walking[i] + dt * 3f);
        float step = Math.min(l, pace * walking[i] * dt);
        x[i] += ex / l * step;
        z[i] += ez / l * step;
        walked[i] += step;
        face(i, (float) Math.atan2(ex, ez), dt, 5f);
        // not counted standing, on the way
        if (wait[i] < 0.5f) wait[i] = 0.5f;
        return true;
    }

    private void face(int i, float to, float dt, float rate) {
        float d = (float) Math.atan2(Math.sin(to - yaw[i]), Math.cos(to - yaw[i]));
        yaw[i] += Math.max(-rate * dt, Math.min(rate * dt, d));
    }

    void draw(DrawList d, float ex, float ez) {
        boolean folk = dressed && Frida.ready();
        for (int i = 0; i < MANY; i++) {
            float dx = x[i] - ex, dz = z[i] - ez;
            if (dx * dx + dz * dz > NEAR * NEAR) continue;
            float gy = city.ground(x[i], z[i]);
            if (!folk) {
                people.standing(d, model[i] % Walkers.MOST, x[i], gy, z[i], yaw[i]);
                continue;
            }
            // the skeleton faces -z at no turn: turned half round, they face the way they look
            if (call[i] > 0f && walking[i] < 0.2f) {
                float lean = (float) Math.sin(Math.min(1f, call[i] * 2.5f) * Math.PI * 0.5f);
                FolkPack.caller(d, model[i], x[i], gy, z[i], yaw[i] + (float) Math.PI, 1f, lean);
            } else {
                FolkPack.walker(d, model[i], x[i], gy, z[i], yaw[i] + (float) Math.PI, 1f, way[i], walked[i], walking[i]);
            }
        }
    }
}
