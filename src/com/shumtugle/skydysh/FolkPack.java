package com.shumtugle.skydysh;

/**
 * The townspeople as meshes, when the game has them (an asset of its own, not in the sources):
 * each a mesh standing in the skeleton's rest pose, its corners weighted to up to four of the
 * skeleton's bones, its colour from one picture shared by all. The world only needs what each
 * one is and how tall; the renderer takes the meshes themselves.
 *
 * Format, little-endian: 'FOLK', i32 version (1), i32 models, i32 picture width, height; per
 * model: i32 kind, f32 size, i32 corners V, i32 indices I; V x (3 f32 place, 3 f32 normal, 2 f32
 * place in the picture), V x 4 u8 bones, V x 4 u8 weights (summing to 255); I x u16.
 *
 * A person is drawn by the pose of the skeleton in a motion: for each bone, the turn and the
 * shift that take it from the rest pose to that pose (see pose()); the renderer moves every
 * corner by its bones' turns and shifts, weighted.
 */
final class FolkPack {
    /** What a model is. */
    static final int MAN = 0, WOMAN = 1, POLICEMAN = 2, POLICEWOMAN = 3, DANCER = 4;
    /** Floats a person takes in a DrawList: model, x, y, z, yaw, size, then per bone a turn (4) and a shift (3). */
    static final int BONES = 23, PERSON = 6 + BONES * 7;

    final int count, pictureW, pictureH;
    final int[] kind, corners, indices;
    final float[] size;
    // per model, for the renderer: corners as 8 floats each, bones and weights as 4 bytes each, indices
    final float[][] vertex;
    final byte[][] bones, weights;
    final short[][] index;

    private FolkPack(java.nio.ByteBuffer in) {
        in.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        in.position(8);
        count = in.getInt();
        pictureW = in.getInt();
        pictureH = in.getInt();
        kind = new int[count];
        corners = new int[count];
        indices = new int[count];
        size = new float[count];
        vertex = new float[count][];
        bones = new byte[count][];
        weights = new byte[count][];
        index = new short[count][];
        for (int m = 0; m < count; m++) {
            kind[m] = in.getInt();
            size[m] = in.getFloat();
            int v = corners[m] = in.getInt(), n = indices[m] = in.getInt();
            float[] vd = vertex[m] = new float[v * 8];
            in.asFloatBuffer().get(vd);
            in.position(in.position() + v * 32);
            in.get(bones[m] = new byte[v * 4]);
            in.get(weights[m] = new byte[v * 4]);
            short[] ix = index[m] = new short[n];
            in.asShortBuffer().get(ix);
            in.position(in.position() + n * 2);
        }
    }

    /** Reads the asset; null if it is missing or will not read. */
    static FolkPack read(byte[] b) {
        if (b == null || b.length < 24 || b[0] != 'F' || b[1] != 'O' || b[2] != 'L' || b[3] != 'K') return null;
        try {
            return new FolkPack(java.nio.ByteBuffer.wrap(b));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The townspeople the game has; null when it has none (the old figures are drawn instead). */
    static FolkPack town;

    // the pose being worked out: the skeleton's, and the rest pose's
    private static float[] pq, pp;

    /**
     * Person m of the town, standing at (x, y, z) turned by yaw (0: facing -z, as the skeleton),
     * so many times its own height, in a motion at a time: into the draw list.
     */
    static void person(DrawList d, int m, float x, float y, float z, float yaw, float scale, String motion, float t) {
        Moves mv = Frida.moves();
        if (town == null || mv == null || mv.bone.length != BONES) return;
        ready();
        int c = mv.motion(motion);
        if (c < 0) c = 0;
        mv.pose(c, t, pq, pp);
        put(d, m, x, y, z, yaw, scale, mv);
    }

    /**
     * Person m walking their own way (see Stroll), so far along (meters walked; the stride
     * follows the ground), walking so much (1) or standing still (0).
     */
    static void walker(DrawList d, int m, float x, float y, float z, float yaw, float scale, Stroll.Way way, float walked, float walking) {
        Moves mv = Frida.moves();
        if (town == null || mv == null || mv.bone.length != BONES) return;
        ready();
        float size = town.size[m] * scale;
        float phase = walked / (Stroll.cycle(way) * size) * 2f * (float) Math.PI;
        Stroll.pose(mv, way, phase, walking, pq, pp);
        put(d, m, x, y, z, yaw, scale, mv);
    }

    /** Whether model m is a woman. */
    boolean woman(int m) {
        return kind[m] == WOMAN || kind[m] == POLICEWOMAN || kind[m] == DANCER;
    }

    private static void ready() {
        if (pq == null) {
            pq = new float[BONES * 4];
            pp = new float[BONES * 3];
        }
    }

    /** The pose now in pq and pp, as person m, into the draw list. */
    private static void put(DrawList d, int m, float x, float y, float z, float yaw, float scale, Moves mv) {
        float[] o = d.person();
        if (o == null) return;
        int at = d.folkAt();
        o[at] = m;
        o[at + 1] = x;
        o[at + 2] = y;
        o[at + 3] = z;
        o[at + 4] = yaw;
        o[at + 5] = town.size[m] * scale;
        for (int b = 0; b < BONES; b++) {
            // the turn from rest to now (now times the rest's inverse), and the shift that keeps the bone's joint where the pose has it
            float ax = pq[b * 4], ay = pq[b * 4 + 1], az = pq[b * 4 + 2], aw = pq[b * 4 + 3];
            float bx = -mv.restQ[b * 4], by = -mv.restQ[b * 4 + 1], bz = -mv.restQ[b * 4 + 2], bw = mv.restQ[b * 4 + 3];
            float qx = aw * bx + ax * bw + ay * bz - az * by;
            float qy = aw * by - ax * bz + ay * bw + az * bx;
            float qz = aw * bz + ax * by - ay * bx + az * bw;
            float qw = aw * bw - ax * bx - ay * by - az * bz;
            float rx = mv.restP[b * 3], ry = mv.restP[b * 3 + 1], rz = mv.restP[b * 3 + 2];
            // the rest joint turned: r + 2 q x (q x r + w r)
            float cx = qy * rz - qz * ry + qw * rx, cy = qz * rx - qx * rz + qw * ry, cz = qx * ry - qy * rx + qw * rz;
            float tx = rx + 2f * (qy * cz - qz * cy), ty = ry + 2f * (qz * cx - qx * cz), tz = rz + 2f * (qx * cy - qy * cx);
            int k = at + 6 + b * 7;
            o[k] = qx;
            o[k + 1] = qy;
            o[k + 2] = qz;
            o[k + 3] = qw;
            o[k + 4] = pp[b * 3] - tx;
            o[k + 5] = pp[b * 3 + 1] - ty;
            o[k + 6] = pp[b * 3 + 2] - tz;
        }
    }
}
