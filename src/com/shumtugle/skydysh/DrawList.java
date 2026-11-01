package com.shumtugle.skydysh;

/**
 * One frame, as the world wants it drawn: the eye, the sun, the sky, every box in the scene,
 * and the flat layer laid over it all. The world fills it; a renderer draws it. Nothing
 * here knows how drawing is done, so the phone and the desktop stand draw the same list
 * with the same shaders.
 *
 * Space: x to the right, y up, z away from the eye at rest; meters.
 */
final class DrawList {
    // what a box's faces are made of; the shaders turn these into patterns
    static final int PLAIN = 0, GRAIN = 1, STONE = 2, ASPHALT = 3, WOOD = 4, PAVING = 5, PAINT = 6, GOLD = 7;
    static final int FACADE = 8, FROND = 9, TRUNK = 10, ROAD = 11, GLASS = 12, GLOW = 13, SEA = 14, RAIL = 15,
            PROMENADE = 16, AWNING = 17, SQUARE = 18, LANE = 19;
    // a number a material may carry: drawn as one with what it stands on, without a line between
    static final int SEAMLESS = 252;
    // what a ball is: a polished one with finger holes, or a rubber one
    static final int POLISHED = 20, RUBBER = 21;
    // green things: a mass of leaves (its seed, if any, a color of flowers in it); a lawn; a gravel path
    static final int LEAVES = 22, GRASS = 23, GRAVEL = 24;
    // a ball of plain cloth or skin, without shine (people)
    static final int MATTE = 26;
    // clear plastic: a water bottle's skin, drawn last, over what is behind it (see Round.bottle)
    static final int CLEAR = 27;
    // drawn as balls, but round only about their own x: a rod with flat ends (a handle), and the
    // hammer's head, the same with its band of scallops
    static final int ROD = 28, HEAD = 29;
    // a burning block: hot at its heart, crusted at its edges; its seed says how hot (0 to 250)
    static final int FIRE = 30;
    // a floor of tiles in two tones; its seed past half sets them on the diagonal
    static final int TILES = 31;
    // a car's painted skin (see skin()): its seed says which car; a tyre, a ball round about its own x
    static final int CAR = 32, TYRE = 25;
    // per box: middle (3), half sizes (3), the turn as three columns (9), color (3), material (1)
    static final int STRIDE = 19;

    int w, h;                                  // the frame, in game pixels
    // the fine layer over the top of the frame (the crest): so many pixels to a frame pixel,
    // covering so many frame rows from the top; none drawn this frame, none
    int[] hud;
    int hudW, hudH, hudRows;
    boolean hudFresh;                          // drawn anew since it was last handed on
    final float[] viewProj = new float[16];    // world to clip, column after column
    final float[] lightProj = new float[16];   // world to the sun's depth map
    final float[] eye = new float[3], right = new float[3], up = new float[3], ahead = new float[3];
    float focal;                               // pixels from the eye to the frame
    final float[] sun = new float[3];          // toward the sun, unit length
    final float[] sunColor = {1f, 1f, 1f};
    final float[] skyTop = new float[3], skyHorizon = new float[3], skyGlow = new float[3];
    final float[] ambientSky = new float[3], ambientGround = new float[3];
    final float[] fog = new float[3];
    float fogStart = 8f, fogScale = 150f;      // the haze begins so far off, and thickens over so many meters
    float time;                                // seconds of game time, for what drifts in the sky
    float night;                               // on the street: lamps and windows lit, stars out, 0 to 1
    final float[] grade = {1f, 1f, 0f};        // the hour's grade: contrast, saturation, warmth
    // the aim, drawn smooth over everything: the ring's middle (frame pixels), the wind-up,
    // shown (1) or not; the finger's circle (x, y, shown); the dots of a flight, each x, y and
    // kind (0 the aim's, 1 the trail of a throw)
    final float[] ring = new float[4], finger = new float[3];
    static final int TRAIL_MAX = 32;
    final float[] trail = new float[TRAIL_MAX * 3];
    int trailN;
    // something burning: its light on what is near (x, y, z, strength; 0, none), and the glow
    // printed round it (middle x, y and reach, frame pixels; strength; how far off, meters)
    final float[] fire = new float[4], halo = new float[5];
    // the dark: a light on one place only (its middle, x y z, and its reach in meters), and how
    // far the rest has gone into the dark, 0 to 1; whatever glows of itself keeps glowing
    final float[] spot = new float[5];
    // a tube's glass over the frame (frame pixels: x0, y0, x1, y1), whose rows of the flat layer
    // are drawn as a tube draws them, a dark line under each; none when x1 is not past x0
    final float[] tube = new float[4];

    /** A dot of a flight, in frame pixels. */
    void dot(float x, float y, int kind) {
        if (trailN >= TRAIL_MAX) return;
        trail[trailN * 3] = x;
        trail[trailN * 3 + 1] = y;
        trail[trailN * 3 + 2] = kind;
        trailN++;
    }

    float[] boxes = new float[STRIDE * 256];
    int count;
    // true spheres, laid out as boxes (the cube round each), drawn apart: their shader works
    // out where the eye's ray meets the sphere and how deep that is, and the boxes' does not
    float[] balls = new float[STRIDE * 16];
    int ballCount;

    int[] flat;                                // the flat layer, ARGB, w * h; clear where the scene shows

    // people drawn as meshes (see FolkPack): each so many floats, the pose of its bones among them
    float[] folk = new float[FolkPack.PERSON * 16];
    int folkN;

    /** Room for one more person; its floats start at folkAt(). Null if the list is full. */
    float[] person() {
        if (folkN >= 96) return null;
        if ((folkN + 1) * FolkPack.PERSON > folk.length) {
            float[] more = new float[folk.length * 2];
            System.arraycopy(folk, 0, more, 0, folk.length);
            folk = more;
        }
        folkN++;
        return folk;
    }

    /** Where the last person's floats start. */
    int folkAt() {
        return (folkN - 1) * FolkPack.PERSON;
    }

    // what flies far off in the sky, as strokes the sky draws with itself: per stroke its ends
    // in frame pixels (x0, y0, x1, y1), then its width and cover at each end; a dark one (a bird)
    // has its first width negative, the rest are white (a plane, its trail)
    static final int FLY_MAX = 32;
    final float[] fly = new float[FLY_MAX * 8];
    int flyN;

    void fly(float x0, float y0, float x1, float y1, float w0, float w1, float a0, float a1, boolean dark) {
        if (flyN >= FLY_MAX) return;
        int o = flyN++ * 8;
        fly[o] = x0; fly[o + 1] = y0; fly[o + 2] = x1; fly[o + 3] = y1;
        fly[o + 4] = dark ? -w0 : w0; fly[o + 5] = w1; fly[o + 6] = a0; fly[o + 7] = a1;
    }

    // the buttons and signs over the frame (see Badges): each its middle and half its size
    // (frame pixels), how much it shows (0 to 1), which of the sheet's cells, how lit (0 to 1)
    static final int BADGE_MAX = 16;
    final float[] badge = new float[BADGE_MAX * 4], badgeOf = new float[BADGE_MAX * 2];
    int badgeN;

    // a badge's own colour, laid over its rim and glass: none, coral, turquoise, amber, cream
    static final int TINT_NONE = 0, TINT_CORAL = 1, TINT_TURQUOISE = 2, TINT_AMBER = 3, TINT_CREAM = 4;

    /** A button or sign over the frame: at (x, y), so big, so much shown, lit so much. */
    void badge(float x, float y, float half, int which, float shown, float lit) {
        badge(x, y, half, which, shown, lit, TINT_NONE);
    }

    /** A button or sign over the frame, in a colour of its own (TINT_...): it rides with how lit it is, as twice the tint over it. */
    void badge(float x, float y, float half, int which, float shown, float lit, int tint) {
        badge(x, y, half, which, shown, lit, tint, 0f);
    }

    /**
     * The same, turned so far (radians, clockwise on the glass, at most half round either way): the
     * turn rides with how much it shows, as twice a step of 1 to 255 over it (none: no turn).
     */
    void badge(float x, float y, float half, int which, float shown, float lit, int tint, float turn) {
        if (badgeN >= BADGE_MAX || shown <= 0.01f) return;
        int o = badgeN * 4;
        badge[o] = x;
        badge[o + 1] = y;
        badge[o + 2] = half;
        int step = turn == 0f ? 0 : 128 + Math.round(Math.max(-1f, Math.min(1f, turn / (float) Math.PI)) * 127f);
        badge[o + 3] = Math.min(1f, shown) + 2f * step;
        badgeOf[badgeN * 2] = which;
        badgeOf[badgeN * 2 + 1] = Math.max(0f, Math.min(1f, lit)) + 2f * tint;
        badgeN++;
    }

    void begin(int w, int h) {
        if (flat == null || flat.length != w * h) flat = new int[w * h];
        this.w = w;
        this.h = h;
        count = 0;
        ballCount = 0;
        flyN = 0;
        trailN = 0;
        badgeN = 0;
        folkN = 0;
        ring[3] = 0f;
        finger[2] = 0f;
        fire[3] = 0f;
        halo[3] = 0f;
        spot[4] = 0f;
        tube[0] = tube[2] = 0f;
    }

    /**
     * A ball: its middle, its radius, its turn (a 3x3, row by row; the swirl and the holes
     * turn with it), color as 0xRRGGBB, and what it is: POLISHED or RUBBER.
     */
    void ball(float x, float y, float z, float r, float[] rot, int rgb, int kind) {
        float[] keep = boxes;
        int kept = count;
        boxes = balls;
        count = ballCount;
        box(x, y, z, r, r, r, rot, rgb, kind);
        balls = boxes;
        ballCount = count;
        boxes = keep;
        count = kept;
    }

    /**
     * A ball drawn out longer one way than another, as a limb or a body: its middle, its half
     * sizes along its own axes, its turn (a 3x3, row by row), color, and what it is.
     */
    void ellipsoid(float x, float y, float z, float hx, float hy, float hz, float[] rot, int rgb, int kind) {
        float[] keep = boxes;
        int kept = count;
        boxes = balls;
        count = ballCount;
        box(x, y, z, hx, hy, hz, rot, rgb, kind);
        balls = boxes;
        ballCount = count;
        boxes = keep;
        count = kept;
    }

    /**
     * The last box made part of a car's painted skin: the car's kind, where the box's middle sits
     * in the car's own frame (meters, x right, y up, z ahead; its turn must be the car's), and a
     * colour for what the skin does not cover (its inner faces), or the car's paint where the
     * skin is paint. All of it rides in the box's colour: centimeters shifted by 400, times 256,
     * plus the colour's byte.
     */
    void skin(int kind, float lx, float ly, float lz, int rgb) {
        int o = (count - 1) * STRIDE;
        boxes[o + 15] = (Math.round(lx * 100f) + 400) * 256 + ((rgb >> 16) & 0xFF);
        boxes[o + 16] = (Math.round(ly * 100f) + 400) * 256 + ((rgb >> 8) & 0xFF);
        boxes[o + 17] = (Math.round(lz * 100f) + 400) * 256 + (rgb & 0xFF);
        boxes[o + 18] = material(CAR, kind);
    }

    /** A material with a number of its own, 0 to 255, that the shader reads as a seed or a flag. */
    static float material(int material, int seed) {
        return material + (seed & 0xFF) / 256f;
    }

    /** Every box and ball of another list, added to this one: the still part of a scene, built once. */
    void append(DrawList o) {
        int n = o.count * STRIDE;
        while ((count * STRIDE) + n > boxes.length) {
            float[] more = new float[boxes.length * 2];
            System.arraycopy(boxes, 0, more, 0, boxes.length);
            boxes = more;
        }
        System.arraycopy(o.boxes, 0, boxes, count * STRIDE, n);
        count += o.count;
        n = o.ballCount * STRIDE;
        while ((ballCount * STRIDE) + n > balls.length) {
            float[] more = new float[balls.length * 2];
            System.arraycopy(balls, 0, more, 0, balls.length);
            balls = more;
        }
        System.arraycopy(o.balls, 0, balls, ballCount * STRIDE, n);
        ballCount += o.ballCount;
    }

    /** Where a point lands in the frame, in frame pixels; false behind the eye. */
    boolean project(float x, float y, float z, float[] out) {
        float[] m = viewProj;
        if (out.length > 2) out[2] = m[3] * x + m[7] * y + m[11] * z + m[15];
        float cx = m[0] * x + m[4] * y + m[8] * z + m[12];
        float cy = m[1] * x + m[5] * y + m[9] * z + m[13];
        float cw = m[3] * x + m[7] * y + m[11] * z + m[15];
        if (cw < 0.05f) return false;
        out[0] = (cx / cw * 0.5f + 0.5f) * w;
        out[1] = (0.5f - cy / cw * 0.5f) * h;
        return true;
    }

    /** A box: its middle, its half sizes, its turn (a 3x3, row by row), color as 0xRRGGBB. */
    void box(float x, float y, float z, float hx, float hy, float hz, float[] rot, int rgb, int material) {
        box(x, y, z, hx, hy, hz, rot, rgb, (float) material);
    }

    /** The same, with a material that carries its own number (see material()). */
    void box(float x, float y, float z, float hx, float hy, float hz, float[] rot, int rgb, float material) {
        if ((count + 1) * STRIDE > boxes.length) {
            float[] more = new float[boxes.length * 2];
            System.arraycopy(boxes, 0, more, 0, boxes.length);
            boxes = more;
        }
        int o = count * STRIDE;
        float[] b = boxes;
        b[o] = x;
        b[o + 1] = y;
        b[o + 2] = z;
        b[o + 3] = hx;
        b[o + 4] = hy;
        b[o + 5] = hz;
        if (rot == null) {
            b[o + 6] = 1; b[o + 7] = 0; b[o + 8] = 0;
            b[o + 9] = 0; b[o + 10] = 1; b[o + 11] = 0;
            b[o + 12] = 0; b[o + 13] = 0; b[o + 14] = 1;
        } else {
            // columns of the turn
            b[o + 6] = rot[0]; b[o + 7] = rot[3]; b[o + 8] = rot[6];
            b[o + 9] = rot[1]; b[o + 10] = rot[4]; b[o + 11] = rot[7];
            b[o + 12] = rot[2]; b[o + 13] = rot[5]; b[o + 14] = rot[8];
        }
        b[o + 15] = ((rgb >> 16) & 0xFF) / 255f;
        b[o + 16] = ((rgb >> 8) & 0xFF) / 255f;
        b[o + 17] = (rgb & 0xFF) / 255f;
        b[o + 18] = material;
        count++;
    }

    /**
     * The eye: where it stands, which way it looks (yaw to the right of straight ahead,
     * pitch up), the focal length in frame pixels; near and far planes in meters.
     */
    void camera(float x, float y, float z, float yaw, float pitch, float focal, float near, float far) {
        float cp = (float) Math.cos(pitch);
        float fx = (float) Math.sin(yaw) * cp, fy = (float) Math.sin(pitch), fz = (float) Math.cos(yaw) * cp;
        float rl = (float) Math.sqrt(fz * fz + fx * fx);
        float rx = fz / rl, ry = 0f, rz = -fx / rl;
        // up = ahead x right
        float ux = fy * rz - fz * ry, uy = fz * rx - fx * rz, uz = fx * ry - fy * rx;
        eye[0] = x; eye[1] = y; eye[2] = z;
        right[0] = rx; right[1] = ry; right[2] = rz;
        up[0] = ux; up[1] = uy; up[2] = uz;
        ahead[0] = fx; ahead[1] = fy; ahead[2] = fz;
        this.focal = focal;
        float sx = 2f * focal / w, sy = 2f * focal / h;
        float a = (far + near) / (far - near), b = -2f * far * near / (far - near);
        float tx = -(rx * x + ry * y + rz * z), ty = -(ux * x + uy * y + uz * z), tz = -(fx * x + fy * y + fz * z);
        // clip = P * V, written out: x' = sx * (r.p + tx), y' = sy * (u.p + ty), w' = f.p + tz
        float[] m = viewProj;
        m[0] = sx * rx; m[4] = sx * ry; m[8] = sx * rz; m[12] = sx * tx;
        m[1] = sy * ux; m[5] = sy * uy; m[9] = sy * uz; m[13] = sy * ty;
        m[2] = a * fx; m[6] = a * fy; m[10] = a * fz; m[14] = a * tz + b;
        m[3] = fx; m[7] = fy; m[11] = fz; m[15] = tz;
    }

    /**
     * The sun: its direction (toward it), and the stretch its shadows are worked out over,
     * around a middle point: so many meters across the sun's beams level with the ground,
     * so many up and down them, so many deep along them.
     */
    void sun(float dx, float dy, float dz, float cx, float cy, float cz, float across, float upDown, float depth) {
        float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= l; dy /= l; dz /= l;
        sun[0] = dx; sun[1] = dy; sun[2] = dz;
        // the sun's own frame: it looks along -d
        float fx = -dx, fy = -dy, fz = -dz;
        float rx = fz, ry = 0f, rz = -fx;
        float rl = (float) Math.sqrt(rx * rx + rz * rz);
        if (rl < 1e-4f) { rx = 1f; rz = 0f; rl = 1f; }
        rx /= rl; rz /= rl;
        float ux = fy * rz - fz * ry, uy = fz * rx - fx * rz, uz = fx * ry - fy * rx;
        float ex = cx - fx * depth * 0.5f, ey = cy - fy * depth * 0.5f, ez = cz - fz * depth * 0.5f;
        float s = 2f / across, t = 2f / upDown, k = 2f / depth;
        float[] m = lightProj;
        m[0] = s * rx; m[4] = s * ry; m[8] = s * rz; m[12] = -s * (rx * ex + ry * ey + rz * ez);
        m[1] = t * ux; m[5] = t * uy; m[9] = t * uz; m[13] = -t * (ux * ex + uy * ey + uz * ez);
        m[2] = k * fx; m[6] = k * fy; m[10] = k * fz; m[14] = -k * (fx * ex + fy * ey + fz * ez) - 1f;
        m[3] = 0; m[7] = 0; m[11] = 0; m[15] = 1;
    }

    static void rgb(float[] out, int c) {
        out[0] = ((c >> 16) & 0xFF) / 255f;
        out[1] = ((c >> 8) & 0xFF) / 255f;
        out[2] = (c & 0xFF) / 255f;
    }

    /** The whole list as text, for the desktop stand's renderer. The flat layer is written apart. */
    String json() {
        StringBuilder s = new StringBuilder(64 + count * STRIDE * 10);
        s.append("{\"w\":").append(w).append(",\"h\":").append(h).append(",\"focal\":").append(focal);
        s.append(",\"hudRows\":").append(hudRows);
        arr(s, "viewProj", viewProj);
        arr(s, "lightProj", lightProj);
        arr(s, "eye", eye);
        arr(s, "right", right);
        arr(s, "up", up);
        arr(s, "ahead", ahead);
        arr(s, "sun", sun);
        arr(s, "sunColor", sunColor);
        arr(s, "skyTop", skyTop);
        arr(s, "skyHorizon", skyHorizon);
        arr(s, "skyGlow", skyGlow);
        arr(s, "ambientSky", ambientSky);
        arr(s, "ambientGround", ambientGround);
        arr(s, "fog", fog);
        s.append(",\"fogStart\":").append(fogStart).append(",\"fogScale\":").append(fogScale).append(",\"time\":").append(time);
        s.append(",\"night\":").append(night);
        arr(s, "grade", grade);
        arr(s, "ring", ring);
        arr(s, "finger", finger);
        arr(s, "fire", fire);
        arr(s, "halo", halo);
        arr(s, "spot", spot);
        arr(s, "tube", tube);
        s.append(",\"badgeN\":").append(badgeN);
        arr(s, "badge", badge);
        arr(s, "badgeOf", badgeOf);
        s.append(",\"trailN\":").append(trailN);
        arr(s, "trail", trail);
        s.append(",\"flyN\":").append(flyN).append(",\"fly\":[");
        for (int i = 0; i < flyN * 8; i++) {
            if (i > 0) s.append(',');
            s.append(fly[i]);
        }
        s.append(']');
        s.append(",\"folkN\":").append(folkN).append(",\"folk\":[");
        for (int i = 0; i < folkN * FolkPack.PERSON; i++) {
            if (i > 0) s.append(',');
            s.append(folk[i]);
        }
        s.append(']');
        s.append(",\"count\":").append(count).append(",\"boxes\":[");
        for (int i = 0; i < count * STRIDE; i++) {
            if (i > 0) s.append(',');
            s.append(boxes[i]);
        }
        s.append("],\"ballCount\":").append(ballCount).append(",\"balls\":[");
        for (int i = 0; i < ballCount * STRIDE; i++) {
            if (i > 0) s.append(',');
            s.append(balls[i]);
        }
        s.append("]}");
        return s.toString();
    }

    private static void arr(StringBuilder s, String name, float[] a) {
        s.append(",\"").append(name).append("\":[");
        for (int i = 0; i < a.length; i++) {
            if (i > 0) s.append(',');
            s.append(a[i]);
        }
        s.append(']');
    }
}
