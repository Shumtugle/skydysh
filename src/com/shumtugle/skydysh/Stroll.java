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
        // down the bones from the pelvis: each one's turn added to its parent's, its joint carried
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
            if (b == PELVIS) {
                // the hips swaying over the leg that bears the weight
                pos[b * 3] += 0.018f * w.hips * sl * a;
            }
        }
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
