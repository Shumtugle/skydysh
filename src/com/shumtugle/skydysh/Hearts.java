package com.shumtugle.skydysh;

/**
 * The hearts: a stream of small hearts rising up the right side of the screen, as if many hands
 * were tapping at once, and the likes they count. A moment worth it pours a burst at once and a
 * tail after it; a quiet one (music in the hall) lets a few drift up all the while. The sum shows
 * at the stream's foot while likes come in and goes once they stop. Drawn in the flat layer.
 * Plain Java, no platform classes.
 */
final class Hearts {
    /** At most so many in the air at once. */
    static final int MOST = 180;
    /** Hearts let go a second while a burst is pouring. */
    static final float POUR = 70f;
    /** Rising speed, pixels a second, and how it slows with age. */
    static final float RISE_MIN = 95f, RISE_MAX = 165f, DRAG = 0.22f;
    /** How long a heart lives, seconds; the last share of it fades. */
    static final float LIFE_MIN = 2.4f, LIFE_MAX = 3.8f, FADE = 0.4f;
    /** The sum at the foot stays so long after the last like, the last second fading. */
    static final float SHOWN_FOR = 3.2f;
    /** How fast the sum shown runs up to the sum, a share of the gap a second. */
    static final float COUNT_UP = 3.5f;
    /** How strong the sum shows between jobs when nothing comes in. */
    static final float QUIET = 0.75f;

    // the shapes: a small heart (5x5) and a big one (7x6), rows top down
    private static final String[] SMALL = { ".#.#.", "#####", "#####", ".###.", "..#.." };
    private static final String[] BIG = { ".##.##.", "#######", "#######", ".#####.", "..###..", "...#..." };

    /** Their colours, red and pink most of all, a few warm and a few cool. */
    private static final int[] TINTS = {
        0xFFFF4F6E, 0xFFFF4F6E, 0xFFFF7A9C, 0xFFFF7A9C, 0xFFFF9CC2,
        0xFFFFB48A, 0xFFFFD27A, 0xFF7FE6D2, 0xFFF4E8D2, 0xFFFF5C7A
    };

    private final float[] x = new float[MOST], y = new float[MOST], vy = new float[MOST];
    private final float[] age = new float[MOST], life = new float[MOST], sway = new float[MOST], beat = new float[MOST], phase = new float[MOST];
    private final int[] tint = new int[MOST];
    /** How many pixels a cell of each heart is: 1 to 3. */
    private final int[] size = new int[MOST];
    private int count;
    private final java.util.Random rnd = new java.util.Random(0x4EA27L);

    /** Hearts still to be let go, and the part of one let go so far. */
    private float toPour, owed;
    /** Hearts a second drifting up now (set each step; nothing set, none). */
    private float drift;
    /** The likes in all, the sum shown (running up to it), and how long it has still to show. */
    private long total;
    private float shown, showFor;

    /** The likes in all to begin from (kept from the last game): shown at once, nothing poured. */
    void start(long likes) {
        total = likes;
        shown = likes;
    }

    /** Likes come in: so many more in all, and so many hearts poured for them. */
    void add(long likes, int hearts) {
        total += likes;
        if (likes > 0) showFor = SHOWN_FOR;
        toPour += hearts;
    }

    /** Hearts only, no likes with them (a report going over what was won). */
    void pour(int hearts) {
        toPour += hearts;
    }

    /** A few drifting up while this lasts: so many a second; called each step it should go on. */
    void drift(float perSecond) {
        drift = Math.max(drift, perSecond);
        if (perSecond > 0f) showFor = Math.max(showFor, 1f);
    }

    long total() {
        return total;
    }

    /** Whether any heart is in the air or still to come. */
    boolean busy() {
        return count > 0 || toPour >= 1f;
    }

    void update(float dt, int w, int h) {
        // let go what is due: the burst first, then the drift
        float rate = (toPour >= 1f ? POUR : 0f) + drift;
        owed += rate * dt;
        while (owed >= 1f) {
            owed -= 1f;
            if (toPour >= 1f) toPour -= 1f;
            spawn(w, h);
        }
        if (rate <= 0f) owed = 0f;
        drift = 0f;
        // up, slower with age, swaying
        for (int k = 0; k < count; k++) {
            age[k] += dt;
            y[k] -= vy[k] * dt;
            vy[k] *= 1f - DRAG * dt;
        }
        for (int k = 0; k < count; k++) {
            if (age[k] < life[k] && y[k] > -8f) continue;
            count--;
            copy(count, k);
            k--;
        }
        // the sum runs up to what it is; shown a while after the last like
        shown += (total - shown) * Math.min(1f, COUNT_UP * dt);
        if (Math.abs(total - shown) < 1f) shown = total;
        if (showFor > 0f) showFor -= dt;
    }

    /** The hearts and, while likes come in (or always, if asked), their sum at the stream's foot. */
    void draw(Px f, int w, int h, boolean always) {
        for (int k = 0; k < count; k++) {
            float t = age[k] / life[k];
            // in: grown from small in the first moment; out: faded over the last share of its life
            float a = t > 1f - FADE ? (1f - t) / FADE : 1f;
            if (a <= 0f) continue;
            // born small, grown to its size in the first moment
            int sz = age[k] < 0.15f ? 1 : size[k];
            float px = x[k] + sway[k] * (float) Math.sin(phase[k] + beat[k] * age[k]);
            heart(f, Math.round(px), Math.round(y[k]), sz > 1 ? BIG : SMALL, sz, tint[k], a);
        }
        // the sum: while likes come in; between jobs, always, a little quieter
        if (total <= 0 || showFor <= 0f && !always) return;
        float a = Math.max(Math.min(1f, showFor), always ? QUIET : 0f);
        String s = World.grouped(Math.round(shown));
        int fy = footY(h), hx = w - 8 - 7;
        heart(f, hx, fy, BIG, 1, 0xFFFF4F6E, a);
        int tx = hx - 3 - PixelFont.width(s, 1);
        PixelFont.draw(f, s, tx + 1, fy + 1, 1, fade(0xFF232A4A, a * 0.8f));
        PixelFont.draw(f, s, tx, fy, 1, fade(0xFFF4E8D2, a));
    }

    /** Where the stream rises from: low on the right, over the version's line. */
    static int footY(int h) {
        return h - 34;
    }

    private void spawn(int w, int h) {
        if (count == MOST) {
            // the oldest makes room
            copy(--count, 0);
        }
        int k = count++;
        x[k] = w - 26f + (rnd.nextFloat() - 0.5f) * 14f;
        y[k] = footY(h) - 6f + rnd.nextFloat() * 4f;
        vy[k] = RISE_MIN + rnd.nextFloat() * (RISE_MAX - RISE_MIN);
        age[k] = 0f;
        life[k] = LIFE_MIN + rnd.nextFloat() * (LIFE_MAX - LIFE_MIN);
        sway[k] = 4f + rnd.nextFloat() * 12f;
        beat[k] = 2f + rnd.nextFloat() * 2.5f;
        phase[k] = rnd.nextFloat() * 6.283f;
        tint[k] = TINTS[rnd.nextInt(TINTS.length)];
        float r = rnd.nextFloat();
        size[k] = r < 0.25f ? 1 : r < 0.75f ? 2 : 3;
    }

    private void copy(int from, int to) {
        x[to] = x[from];
        y[to] = y[from];
        vy[to] = vy[from];
        age[to] = age[from];
        life[to] = life[from];
        sway[to] = sway[from];
        beat[to] = beat[from];
        phase[to] = phase[from];
        tint[to] = tint[from];
        size[to] = size[from];
    }

    /** One heart, its top left at (x0, y0), each cell scale pixels across, a dark edge under it and a light fleck. */
    private static void heart(Px f, int x0, int y0, String[] rows, int scale, int c, float a) {
        int edge = fade(Px.scale(c, 0.35f), a * 0.7f), fill = fade(c, a), fleck = fade(Px.mix(c, 0xFFFFFFFF, 0.6f), a);
        for (int r = 0; r < rows.length; r++)
            for (int q = 0; q < rows[r].length(); q++)
                if (rows[r].charAt(q) == '#') f.rect(x0 + q * scale + 1, y0 + r * scale + 1, x0 + (q + 1) * scale + 1, y0 + (r + 1) * scale + 1, edge);
        for (int r = 0; r < rows.length; r++)
            for (int q = 0; q < rows[r].length(); q++)
                if (rows[r].charAt(q) == '#') f.rect(x0 + q * scale, y0 + r * scale, x0 + (q + 1) * scale, y0 + (r + 1) * scale, fill);
        // the shine on the left lobe
        f.rect(x0 + scale, y0 + scale, x0 + 2 * scale, y0 + 2 * scale, fleck);
    }

    /** A colour with its alpha brought down by a. */
    private static int fade(int c, float a) {
        int al = Math.round(((c >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (c & 0xFFFFFF);
    }
}
