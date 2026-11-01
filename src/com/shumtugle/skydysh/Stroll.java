package com.shumtugle.skydysh;

import java.util.Random;

/**
 * A person's walk, worked out on the skeleton instead of played from a motion: the heroine's
 * motions are a giant's, and townspeople should not walk like her. Upright, the legs swinging
 * from the hips, the knee bending only as the leg comes through, the foot set down flat and
 * rolled off the toes, the arms hanging and swinging against the legs, the hips turning and
 * dropping a little with each step, the head kept level. Each person walks their own way:
 * longer or shorter steps, more or less swing in the arms and the hips, a stoop.
 *
 * Every bone is turned from the rest pose by a turn of its own, about the rest pose's axes (x
 * to the right, y up, the figure facing -z), added to its parent's; the feet are kept on the
 * ground by lowering the whole figure until the lower foot touches it.
 */
final class Stroll {
    private Stroll() {
    }

    /** A person's own way of walking. */
    static final class Way {
        float stride;       // how far the thighs swing, radians
        float arms;         // how far the arms swing, radians
        float hips;         // how much the hips sway and turn
        float stoop;        // a lean forward of the back, radians
        float elbows;       // how bent the elbows are
        float bounce;       // how springy the step
    }

    /** The way of walking for a seed: women with a little more sway in the hips. */
    static Way way(long seed, boolean woman) {
        Random r = new Random(seed * 0x2545F4914F6CDD1DL + 7L);
        Way w = new Way();
        w.stride = 0.3f + r.nextFloat() * 0.12f;
        w.arms = 0.18f + r.nextFloat() * 0.22f;
        w.hips = (woman ? 0.8f : 0.4f) + r.nextFloat() * 0.4f;
        w.stoop = r.nextInt(6) == 0 ? 0.12f + r.nextFloat() * 0.12f : r.nextFloat() * 0.05f;
        w.elbows = 0.15f + r.nextFloat() * 0.25f;
        w.bounce = 0.5f + r.nextFloat() * 0.6f;
        return w;
    }

    /** How far one whole stride (two steps) carries a person of the skeleton's size walking this way, meters. */
    static float cycle(Way w) {
        return 4f * LEG * (float) Math.sin(w.stride);
    }

    static final float LEG = 0.83f;          // hip to ankle on the skeleton, meters
    static final float LEAN = 0.09f;         // the lean forward of a person walking, radians

    // the bones, by their places in the skeleton
    private static final int PELVIS = 1, SPINE1 = 2, SPINE2 = 3, SPINE3 = 4, NECK = 5, HEAD = 6,
            ARM_L = 8, FORE_L = 9, ARM_R = 12, FORE_R = 13,
            THIGH_L = 15, CALF_L = 16, FOOT_L = 17, BALL_L = 18, THIGH_R = 19, CALF_R = 20, FOOT_R = 21, BALL_R = 22;

    private static float[] turn, g, pos;

    /**
     * The pose at a point of the stride (phase, radians: 0, the left thigh coming through
     * upright), walking so much (1) or standing (0): for every bone its turn into q and its
     * place into p, in the figure's own space, as Moves.pose gives them.
     */
    static void pose(Moves mv, Way w, float phase, float walking, float[] q, float[] p) {
        int n = mv.bone.length;
        if (turn == null || turn.length != n * 4) {
            turn = new float[n * 4];
            g = new float[n * 4];
            pos = new float[n * 3];
        }
        for (int b = 0; b < n; b++) set(b, 0f, 0f, 0f, 1f);
        float a = walking;
        float sl = (float) Math.sin(phase), cl = (float) Math.cos(phase);
        // the legs: left at the phase, right half a stride on
        leg(THIGH_L, CALF_L, FOOT_L, BALL_L, phase, w, a);
        leg(THIGH_R, CALF_R, FOOT_R, BALL_R, phase + (float) Math.PI, w, a);
        // the hips turn with the leg coming forward and drop on the side of the swinging leg;
        // the back turns against them and leans as the person stoops; the head stays level
        axis(PELVIS, 0f, 1f, 0f, -0.07f * w.hips * sl * a);
        then(PELVIS, 0f, 0f, 1f, 0.035f * w.hips * cl * a);
        // a person walking leans a little into the walk
        axis(SPINE1, 1f, 0f, 0f, -w.stoop * 0.5f - LEAN * (0.4f + 0.6f * a));
        axis(SPINE2, 0f, 1f, 0f, 0.05f * w.hips * sl * a);
        then(SPINE2, 1f, 0f, 0f, -w.stoop * 0.5f);
        axis(SPINE3, 0f, 1f, 0f, 0.04f * sl * a);
        axis(NECK, 1f, 0f, 0f, w.stoop * 0.6f + LEAN * 0.7f * (0.4f + 0.6f * a));
        axis(HEAD, 0f, 1f, 0f, -0.05f * sl * a);
        then(HEAD, 1f, 0f, 0f, w.stoop * 0.3f);
        // the arms hang down from the shoulders and swing against the legs, the elbows a little bent
        float swing = w.arms * a * sl;
        arm(ARM_L, FORE_L, 1f, -swing, w);
        arm(ARM_R, FORE_R, -1f, swing, w);
        chain(mv, w.hips * sl * a);
        // down onto the ground: the lower foot (heel or toes) where it is at rest
        float drop = Float.MAX_VALUE;
        drop = Math.min(drop, pos[FOOT_L * 3 + 1] - mv.restP[FOOT_L * 3 + 1]);
        drop = Math.min(drop, pos[BALL_L * 3 + 1] - mv.restP[BALL_L * 3 + 1]);
        drop = Math.min(drop, pos[FOOT_R * 3 + 1] - mv.restP[FOOT_R * 3 + 1]);
        drop = Math.min(drop, pos[BALL_R * 3 + 1] - mv.restP[BALL_R * 3 + 1]);
        // and a little spring in the step at mid-stance
        drop -= 0.008f * w.bounce * a * (float) Math.cos(2f * phase);
        for (int b = 0; b < n; b++) {
            // the turn from rest to now, times the rest turn: the bone's turn in the figure
            mul(g, b, mv.restQ, b, q, b);
            p[b * 3] = pos[b * 3];
            p[b * 3 + 1] = pos[b * 3 + 1] - drop;
            p[b * 3 + 2] = pos[b * 3 + 2];
        }
    }

    /**
     * Down the bones from the root: each one's turn added to its parent's (into g), its joint
     * carried (into pos); the pelvis swayed so far along x.
     */
    private static void chain(Moves mv, float sway) {
        int n = mv.bone.length;
        for (int b = 0; b < n; b++) {
            int par = mv.parent[b];
            if (par < 0) {
                copy(turn, b, g, b);
                pos[b * 3] = mv.restP[b * 3];
                pos[b * 3 + 1] = mv.restP[b * 3 + 1];
                pos[b * 3 + 2] = mv.restP[b * 3 + 2];
                continue;
            }
            mul(g, par, turn, b, g, b);
            float dx = mv.restP[b * 3] - mv.restP[par * 3], dy = mv.restP[b * 3 + 1] - mv.restP[par * 3 + 1],
                    dz = mv.restP[b * 3 + 2] - mv.restP[par * 3 + 2];
            rotate(g, par, dx, dy, dz);
            pos[b * 3] = pos[par * 3] + out[0];
            pos[b * 3 + 1] = pos[par * 3 + 1] + out[1];
            pos[b * 3 + 2] = pos[par * 3 + 2] + out[2];
            // the hips swaying over the leg that bears the weight
            if (b == PELVIS) pos[b * 3] += 0.018f * sway;
        }
    }

    // ---- up a ladder by the arms alone ----

    /** How far the body rises in one pull, on the skeleton, meters: a hand from over the head to the chest. */
    static final float PULL_RISE = 0.75f;
    private static final float PULL_PART = 0.55f;          // of each pull, the share the body rises in
    // a hand at a rung: so far out from the middle, so far before the shoulders; its height over
    // the shoulder, at the chest and reached up
    private static final float GRIP_OUT = 0.18f, GRIP_FORE = 0.27f, GRIP_LOW = -0.3f;
    private static final int HAND_L = 10, HAND_R = 14;

    private static float ease(float u) {
        u = Math.max(0f, Math.min(1f, u));
        return u * u * (3f - 2f * u);
    }

    /**
     * Hanging from a ladder and pulling up hand over hand, the legs hanging still under her: at
     * pulls (one a pull; the left hand pulls first), for every bone its turn into q and its place
     * into p, as pose() gives them. The figure rises a pull's height in jerks, still while the
     * free hand reaches up and quick while the other pulls; over a whole pull it rises no more
     * than evenly, so it is put where an even climb puts it.
     */
    static void pull(Moves mv, float pulls, float[] q, float[] p) {
        int n = mv.bone.length;
        if (turn == null || turn.length != n * 4) {
            turn = new float[n * 4];
            g = new float[n * 4];
            pos = new float[n * 3];
        }
        for (int b = 0; b < n; b++) set(b, 0f, 0f, 0f, 1f);
        int k = (int) Math.floor(pulls);
        float u = pulls - k;
        boolean leftPulls = (k & 1) == 0;
        float rise = ease(u / PULL_PART), reach = ease((u - PULL_PART) / (1f - PULL_PART));
        // the pulling hand comes down as the body goes up past it; the other reaches up after
        float hl = leftPulls ? 1f - rise : reach, hr = leftPulls ? reach : 1f - rise;
        float sw = leftPulls ? 1f : -1f;
        // the legs hang, a little bent, the toes down, swinging a little with the pulls
        float swing = 0.07f * (float) Math.sin(Math.PI * pulls);
        for (int e = 0; e < 2; e++) {
            int thigh = e == 0 ? THIGH_L : THIGH_R, calf = e == 0 ? CALF_L : CALF_R, foot = e == 0 ? FOOT_L : FOOT_R;
            axis(thigh, 1f, 0f, 0f, 0.1f + swing);
            then(thigh, 0f, 0f, 1f, (e == 0 ? 1f : -1f) * 0.03f);
            axis(calf, 1f, 0f, 0f, -0.18f - 0.5f * swing);
            axis(foot, 1f, 0f, 0f, -0.55f);
        }
        // the body hangs from the hands, turned a little toward the one pulling; the head up
        axis(PELVIS, 0f, 0f, 1f, 0.05f * sw * (rise - reach));
        axis(SPINE2, 0f, 1f, 0f, 0.08f * sw * (1f - reach));
        axis(SPINE3, 1f, 0f, 0f, -0.06f);
        axis(NECK, 1f, 0f, 0f, -0.25f);
        axis(HEAD, 1f, 0f, 0f, -0.2f);
        chain(mv, 0f);
        // the arms reach the rungs: each hand where it holds on, the elbow out to the side
        arm(mv, ARM_L, FORE_L, HAND_L, -1f, hl);
        arm(mv, ARM_R, FORE_R, HAND_R, 1f, hr);
        // up in jerks: ahead of an even climb while pulling, back to it by the end of the pull
        float lift = PULL_RISE * (rise - u);
        for (int b = 0; b < n; b++) {
            mul(g, b, mv.restQ, b, q, b);
            p[b * 3] = pos[b * 3];
            p[b * 3 + 1] = pos[b * 3 + 1] + lift;
            p[b * 3 + 2] = pos[b * 3 + 2];
        }
    }

    // ---- swimming ----

    /**
     * Swimming, the head up out of the water, in strokes (t, one a stroke): the body lying along the
     * water, the hands out ahead together, sweeping out and back to the chest, then forward again;
     * the legs drawn up and kicked out behind as the hands come forward. For every bone its turn
     * into q and its place into p, as pose() gives them, the pelvis at the figure's origin.
     */
    static void swim(Moves mv, float strokes, float[] q, float[] p) {
        int n = mv.bone.length;
        if (turn == null || turn.length != n * 4) {
            turn = new float[n * 4];
            g = new float[n * 4];
            pos = new float[n * 3];
        }
        for (int b = 0; b < n; b++) set(b, 0f, 0f, 0f, 1f);
        float u = strokes - (float) Math.floor(strokes);
        // the pull (the hands out and back) the first half, the kick (the legs) the second
        float pull = (float) Math.sin(Math.PI * Math.min(1f, u * 2f)), kick = u < 0.5f ? 0f : (float) Math.sin(Math.PI * (u - 0.5f) * 2f);
        float draw = u < 0.5f ? u * 2f : 1f - (u - 0.5f) * 2f;
        axis(PELVIS, 1f, 0f, 0f, 1.08f + 0.08f * pull);
        axis(SPINE3, 1f, 0f, 0f, -0.15f);
        axis(NECK, 1f, 0f, 0f, -0.55f);
        axis(HEAD, 1f, 0f, 0f, -0.45f);
        for (int e = 0; e < 2; e++) {
            int thigh = e == 0 ? THIGH_L : THIGH_R, calf = e == 0 ? CALF_L : CALF_R, foot = e == 0 ? FOOT_L : FOOT_R;
            float side = e == 0 ? 1f : -1f;
            axis(thigh, 1f, 0f, 0f, 0.15f + 0.75f * draw * (1f - kick));
            then(thigh, 0f, 0f, 1f, side * (0.1f + 0.35f * draw));
            axis(calf, 1f, 0f, 0f, -0.1f - 1.4f * draw * (1f - kick));
            axis(foot, 1f, 0f, 0f, -0.9f);
        }
        chain(mv, 0f);
        // the hands: out ahead, together; out and back to the chest; forward again
        for (int e = -1; e <= 1; e += 2) {
            int up = e < 0 ? ARM_L : ARM_R;
            float sx = pos[up * 3], sy = pos[up * 3 + 1], sz = pos[up * 3 + 2];
            float spread = 0.12f + 0.42f * pull, ahead = 0.62f - 0.5f * draw, low = -0.12f - 0.12f * pull;
            if (e < 0) reach(mv, ARM_L, FORE_L, HAND_L, -1f, sx - spread, sy + low, sz - ahead);
            else reach(mv, ARM_R, FORE_R, HAND_R, 1f, sx + spread, sy + low, sz - ahead);
        }
        float py = pos[PELVIS * 3 + 1];
        for (int b = 0; b < n; b++) {
            mul(g, b, mv.restQ, b, q, b);
            p[b * 3] = pos[b * 3];
            p[b * 3 + 1] = pos[b * 3 + 1] - py;
            p[b * 3 + 2] = pos[b * 3 + 2];
        }
    }

    // ---- at the wheel ----

    /** Where the wheel is before her sitting, in the figure: so far over the seat, so far ahead; its half width. */
    private static final float WHEEL_UP = 1.16f, WHEEL_FORE = -0.4f, WHEEL_HALF = 0.17f;

    /**
     * Sitting at a wheel, the thighs level, the shins down, the back upright, both hands on the
     * wheel and turned with it so far (-1 hard left, 1 hard right): for every bone its turn into q
     * and its place into p, as pose() gives them, the figure as it stands (not lowered: the seat
     * is the caller's).
     */
    static void sit(Moves mv, float wheel, float[] q, float[] p) {
        sit(mv, wheel, true, 0f, q, p);
    }

    /**
     * Sitting on a bench, at ease: the hands on the thighs, leaning back so much (a person's own),
     * the head turned so much: as sit() gives it.
     */
    static void seated(Moves mv, float lean, float look, float[] q, float[] p) {
        sit(mv, look, false, lean, q, p);
    }

    private static void sit(Moves mv, float wheel, boolean atWheel, float lean, float[] q, float[] p) {
        int n = mv.bone.length;
        if (turn == null || turn.length != n * 4) {
            turn = new float[n * 4];
            g = new float[n * 4];
            pos = new float[n * 3];
        }
        for (int b = 0; b < n; b++) set(b, 0f, 0f, 0f, 1f);
        for (int e = 0; e < 2; e++) {
            int thigh = e == 0 ? THIGH_L : THIGH_R, calf = e == 0 ? CALF_L : CALF_R, foot = e == 0 ? FOOT_L : FOOT_R;
            axis(thigh, 1f, 0f, 0f, 1.5f);
            then(thigh, 0f, 0f, 1f, (e == 0 ? 1f : -1f) * 0.08f);
            axis(calf, 1f, 0f, 0f, -1.4f);
        }
        axis(SPINE1, 1f, 0f, 0f, atWheel ? 0.06f : -lean);
        axis(SPINE3, 0f, 1f, 0f, -0.12f * wheel);
        axis(HEAD, 0f, 1f, 0f, -0.25f * wheel);
        chain(mv, 0f);
        if (!atWheel) {
            // the hands resting on the thighs, half way to the knees
            float ty = pos[THIGH_L * 3 + 1] + 0.08f, tz = pos[THIGH_L * 3 + 2] - 0.22f;
            reach(mv, ARM_L, FORE_L, HAND_L, -1f, -0.13f, ty, tz);
            reach(mv, ARM_R, FORE_R, HAND_R, 1f, 0.13f, ty, tz);
        }
        // the hands on the rim of the wheel, a quarter round from the top either side, turned with it
        float a = 0.9f + wheel * 0.9f;
        for (int e = -1; e <= 1 && atWheel; e += 2) {
            float ang = e * a;
            float hx = (float) Math.sin(ang) * WHEEL_HALF, hy = (float) Math.cos(ang) * WHEEL_HALF;
            if (e < 0) reach(mv, ARM_L, FORE_L, HAND_L, -1f, hx, WHEEL_UP + hy, WHEEL_FORE);
            else reach(mv, ARM_R, FORE_R, HAND_R, 1f, hx, WHEEL_UP + hy, WHEEL_FORE);
        }
        for (int b = 0; b < n; b++) {
            mul(g, b, mv.restQ, b, q, b);
            p[b * 3] = pos[b * 3];
            p[b * 3 + 1] = pos[b * 3 + 1];
            p[b * 3 + 2] = pos[b * 3 + 2];
        }
    }

    private static final float[] dir = new float[3], pole = new float[3], elbow = new float[3], hand = new float[3];

    /**
     * An arm (side -1 left, 1 right) reaching to its hold, high (1, over the head) or low (0, at
     * the chest): the shoulder where the body put it, the hand at the hold or as near as the arm
     * reaches, the elbow bent out and down; the upper arm and forearm turned from rest to point so.
     */
    private static void arm(Moves mv, int upper, int fore, int handBone, float side, float high) {
        float sy = pos[upper * 3 + 1];
        reach(mv, upper, fore, handBone, side, side * GRIP_OUT, sy + GRIP_LOW + PULL_RISE * high, -GRIP_FORE);
    }

    /** An arm (side -1 left, 1 right) reaching for (tx, ty, tz) in the figure, or as near as it reaches. */
    private static void reach(Moves mv, int upper, int fore, int handBone, float side, float tx, float ty, float tz) {
        float sx = pos[upper * 3], sy = pos[upper * 3 + 1], sz = pos[upper * 3 + 2];
        float a = len(mv, upper, fore), b = len(mv, fore, handBone);
        float dx = tx - sx, dy = ty - sy, dz = tz - sz, d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dir[0] = dx / d;
        dir[1] = dy / d;
        dir[2] = dz / d;
        d = Math.min(d, (a + b) * 0.999f);
        // the elbow: off the line to the hand, out to the side and down
        pole[0] = side;
        pole[1] = -0.6f;
        pole[2] = 0.3f;
        float pd = pole[0] * dir[0] + pole[1] * dir[1] + pole[2] * dir[2];
        for (int k = 0; k < 3; k++) pole[k] -= dir[k] * pd;
        float pl = Math.max(1e-4f, (float) Math.sqrt(pole[0] * pole[0] + pole[1] * pole[1] + pole[2] * pole[2]));
        float ca = Math.max(-1f, Math.min(1f, (a * a + d * d - b * b) / (2f * a * d)));
        float sa = (float) Math.sqrt(1f - ca * ca);
        for (int k = 0; k < 3; k++) elbow[k] = pos[upper * 3 + k] + a * (ca * dir[k] + sa * pole[k] / pl);
        for (int k = 0; k < 3; k++) hand[k] = pos[upper * 3 + k] + d * dir[k];
        aim(mv, upper, fore, elbow[0] - sx, elbow[1] - sy, elbow[2] - sz);
        aim(mv, fore, handBone, hand[0] - elbow[0], hand[1] - elbow[1], hand[2] - elbow[2]);
        pos[fore * 3] = elbow[0];
        pos[fore * 3 + 1] = elbow[1];
        pos[fore * 3 + 2] = elbow[2];
        pos[handBone * 3] = hand[0];
        pos[handBone * 3 + 1] = hand[1];
        pos[handBone * 3 + 2] = hand[2];
        copy(g, fore, g, handBone);
    }

    private static float len(Moves mv, int from, int to) {
        float dx = mv.restP[to * 3] - mv.restP[from * 3], dy = mv.restP[to * 3 + 1] - mv.restP[from * 3 + 1],
                dz = mv.restP[to * 3 + 2] - mv.restP[from * 3 + 2];
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Bone b's turn in the figure: the shortest turn from its rest way (toward its child c) to (x, y, z). */
    private static void aim(Moves mv, int b, int c, float x, float y, float z) {
        float rx = mv.restP[c * 3] - mv.restP[b * 3], ry = mv.restP[c * 3 + 1] - mv.restP[b * 3 + 1],
                rz = mv.restP[c * 3 + 2] - mv.restP[b * 3 + 2];
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz), l = (float) Math.sqrt(x * x + y * y + z * z);
        rx /= rl;
        ry /= rl;
        rz /= rl;
        x /= l;
        y /= l;
        z /= l;
        float cx = ry * z - rz * y, cy = rz * x - rx * z, cz = rx * y - ry * x, w = 1f + rx * x + ry * y + rz * z;
        float ql = (float) Math.sqrt(cx * cx + cy * cy + cz * cz + w * w);
        g[b * 4] = cx / ql;
        g[b * 4 + 1] = cy / ql;
        g[b * 4 + 2] = cz / ql;
        g[b * 4 + 3] = w / ql;
    }

    /** A leg at its point of the stride: the thigh's swing, the knee, the ankle keeping the foot flat, the toes. */
    private static void leg(int thigh, int calf, int foot, int ball, float phase, Way w, float a) {
        float s = (float) Math.sin(phase), c = (float) Math.cos(phase);
        float hip = w.stride * s * a;
        // the knee bends as the leg comes through (most as the thigh passes upright going forward),
        // and a little under the weight
        float through = Math.max(0f, c);
        float knee = (0.06f + 0.95f * through * through * (0.6f + w.stride)) * a + 0.04f;
        // the foot kept nearly level, tipping up at the heel strike and down at the push off
        float ankle = -(hip - knee) * 0.85f + 0.12f * a * (float) Math.sin(phase - 0.6f);
        axis(thigh, 1f, 0f, 0f, hip);
        axis(calf, 1f, 0f, 0f, -knee);
        axis(foot, 1f, 0f, 0f, ankle);
        // the toes bend as it pushes off
        axis(ball, 1f, 0f, 0f, -0.3f * a * Math.max(0f, -s) * Math.max(0f, -c));
    }

    /** An arm hanging from the shoulder (side 1 left, -1 right), swung forward so far, the elbow a little bent. */
    private static void arm(int upper, int fore, float side, float swing, Way w) {
        // down from straight out to the side, then swung about the shoulder
        axis(upper, 0f, 0f, 1f, side * 1.32f);
        before(upper, 1f, 0f, 0f, swing);
        // the elbow bends the hand forward, more when the arm swings forward
        axis(fore, 0f, 1f, 0f, -side * (w.elbows + 0.4f * Math.max(0f, swing)));
    }

    // ---- turns as quaternions (x y z w), each bone's own in turn[] ----

    private static final float[] out = new float[3], tmp = new float[4];

    private static void set(int b, float x, float y, float z, float w) {
        turn[b * 4] = x;
        turn[b * 4 + 1] = y;
        turn[b * 4 + 2] = z;
        turn[b * 4 + 3] = w;
    }

    /** Bone b's own turn: so much about an axis. */
    private static void axis(int b, float x, float y, float z, float angle) {
        float s = (float) Math.sin(angle * 0.5f);
        set(b, x * s, y * s, z * s, (float) Math.cos(angle * 0.5f));
    }

    /** Bone b's own turn, then so much more about an axis (after it). */
    private static void before(int b, float x, float y, float z, float angle) {
        float s = (float) Math.sin(angle * 0.5f);
        tmp[0] = x * s;
        tmp[1] = y * s;
        tmp[2] = z * s;
        tmp[3] = (float) Math.cos(angle * 0.5f);
        mul(tmp, 0, turn, b, turn, b);
    }

    /** Bone b's own turn with so much about an axis done first. */
    private static void then(int b, float x, float y, float z, float angle) {
        float s = (float) Math.sin(angle * 0.5f);
        tmp[0] = x * s;
        tmp[1] = y * s;
        tmp[2] = z * s;
        tmp[3] = (float) Math.cos(angle * 0.5f);
        mul(turn, b, tmp, 0, turn, b);
    }

    private static void copy(float[] a, int ia, float[] o, int io) {
        System.arraycopy(a, ia * 4, o, io * 4, 4);
    }

    /** o[io] = a[ia] times b[ib] (b first); o may be a or b. */
    private static void mul(float[] a, int ia, float[] b, int ib, float[] o, int io) {
        float ax = a[ia * 4], ay = a[ia * 4 + 1], az = a[ia * 4 + 2], aw = a[ia * 4 + 3];
        float bx = b[ib * 4], by = b[ib * 4 + 1], bz = b[ib * 4 + 2], bw = b[ib * 4 + 3];
        o[io * 4] = aw * bx + ax * bw + ay * bz - az * by;
        o[io * 4 + 1] = aw * by - ax * bz + ay * bw + az * bx;
        o[io * 4 + 2] = aw * bz + ax * by - ay * bx + az * bw;
        o[io * 4 + 3] = aw * bw - ax * bx - ay * by - az * bz;
    }

    /** (x, y, z) turned by q[i], into out. */
    private static void rotate(float[] q, int i, float x, float y, float z) {
        float qx = q[i * 4], qy = q[i * 4 + 1], qz = q[i * 4 + 2], qw = q[i * 4 + 3];
        float cx = qy * z - qz * y + qw * x, cy = qz * x - qx * z + qw * y, cz = qx * y - qy * x + qw * z;
        out[0] = x + 2f * (qy * cz - qz * cy);
        out[1] = y + 2f * (qz * cx - qx * cz);
        out[2] = z + 2f * (qx * cy - qy * cx);
    }
}
