package com.shumtugle.skydysh;

/**
 * The hearts: a stream of red hearts rising up the right side of the screen in a thin column, as if
 * many hands were tapping at once, fading as they go higher, and the likes they count. A moment worth it pours a burst at once and a
 * tail after it; a quiet one (music in the hall) lets a few drift up all the while. The sum shows
 * at the stream's foot while likes come in and goes once they stop. Drawn in the flat layer.
 * Plain Java, no platform classes.
 */
final class Hearts {
    /** At most so many in the air at once. */
    static final int MOST = 180;
    /** Hearts let go a second while a burst is pouring. */
    static final float POUR = 7f;
    /** At most so many waiting to be let go: a burst is a short stream, not a long one. */
    static final float POUR_MOST = 26f;
    /** Rising speed, pixels a second, and how it slows with age. */
    static final float RISE_MIN = 125f, RISE_MAX = 165f, DRAG = 0.12f;
    /** How long a heart lives, seconds; it comes in over the first share of it, then fades as it rises. */
    static final float LIFE_MIN = 3.2f, LIFE_MAX = 4f, FADE_IN = 0.06f;
    /** The sum at the foot stays so long after the last like, the last second fading. */
    static final float SHOWN_FOR = 3.2f;
    /** How fast the sum shown runs up to the sum, a share of the gap a second. */
    static final float COUNT_UP = 3.5f;
    /** How strong the sum shows between jobs when nothing comes in. */
    static final float QUIET = 0.75f;

    // the shapes, rows top down: the sum's mark (7x6); the stream's heart, rounder (9x8)
    private static final String[] BIG = { ".##.##.", "#######", "#######", ".#####.", "..###..", "...#..." };
    private static final String[] ROUND = { ".##...##.", "####.####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...." };
    /** The stream's heart: so many pixels a cell (born at one, grown to it in the first moment). */
    static final int CELL = 2;

    /** Their colours: red, a few deeper, a few lighter. */
    private static final int[] TINTS = { 0xFFFF2D4B, 0xFFFF2D4B, 0xFFFF2D4B, 0xFFE8203C, 0xFFE8203C, 0xFFC81830, 0xFFFF4A62 };

    private final float[] x = new float[MOST], y = new float[MOST], vy = new float[MOST];
    private final float[] age = new float[MOST], life = new float[MOST], sway = new float[MOST], beat = new float[MOST], phase = new float[MOST];
    private final int[] tint = new int[MOST];
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
        toPour = Math.min(POUR_MOST, toPour + hearts);
    }

    /** Hearts only, no likes with them (a report going over what was won). */
    void pour(int hearts) {
        toPour = Math.min(POUR_MOST, toPour + hearts);
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
            // in over the first moment; fading as it rises, the higher the fainter
            float a = Math.min(1f, t / FADE_IN) * (float) Math.pow(Math.max(0f, 1f - t), 0.8f);
            if (a <= 0.02f) continue;
            // born small, grown to its size in the first moment
            int sz = age[k] < 0.12f ? 1 : CELL;
            float px = x[k] + sway[k] * (float) Math.sin(phase[k] + beat[k] * age[k]) - ROUND[0].length() * sz * 0.5f;
            heart(f, Math.round(px), Math.round(y[k]), ROUND, sz, tint[k], a, false);
        }
        // the sum: while likes come in; between jobs, always, a little quieter
        if (total <= 0 || showFor <= 0f && !always) return;
        float a = Math.max(Math.min(1f, showFor), always ? QUIET : 0f);
        String s = World.grouped(Math.round(shown));
        int fy = footY(h), hx = w - 8 - 7;
        heart(f, hx, fy, BIG, 1, 0xFFFF4F6E, a, true);
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
        x[k] = w - 20f + (rnd.nextFloat() - 0.5f) * 12f;
        y[k] = footY(h) - 22f + rnd.nextFloat() * 4f;
        vy[k] = RISE_MIN + rnd.nextFloat() * (RISE_MAX - RISE_MIN);
        age[k] = 0f;
        life[k] = LIFE_MIN + rnd.nextFloat() * (LIFE_MAX - LIFE_MIN);
        sway[k] = 3f + rnd.nextFloat() * 6f;
        beat[k] = 1.5f + rnd.nextFloat() * 1.5f;
        phase[k] = rnd.nextFloat() * 6.283f;
        tint[k] = TINTS[rnd.nextInt(TINTS.length)];
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
    }

    /** A heart standing for likes, as big as asked (its cells so many pixels across), its top left at (x, y). */
    static void mark(Px f, int x, int y, int scale, int color) {
        heart(f, x, y, BIG, scale, color, 1f, true);
    }

    /** One heart, its top left at (x0, y0), each cell scale pixels across; marked: a dark edge under it and a light fleck. */
    private static void heart(Px f, int x0, int y0, String[] rows, int scale, int c, float a, boolean marked) {
        int edge = fade(Px.scale(c, 0.35f), a * 0.7f), fill = fade(c, a), fleck = fade(Px.mix(c, 0xFFFFFFFF, 0.6f), a);
        if (marked) for (int r = 0; r < rows.length; r++)
            for (int q = 0; q < rows[r].length(); q++)
                if (rows[r].charAt(q) == '#') f.rect(x0 + q * scale + 1, y0 + r * scale + 1, x0 + (q + 1) * scale + 1, y0 + (r + 1) * scale + 1, edge);
        for (int r = 0; r < rows.length; r++)
            for (int q = 0; q < rows[r].length(); q++)
                if (rows[r].charAt(q) == '#') f.rect(x0 + q * scale, y0 + r * scale, x0 + (q + 1) * scale, y0 + (r + 1) * scale, fill);
        // the shine on the left lobe
        if (marked) f.rect(x0 + scale, y0 + scale, x0 + 2 * scale, y0 + 2 * scale, fleck);
    }

    /** A colour with its alpha brought down by a. */
    private static int fade(int c, float a) {
        int al = Math.round(((c >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, a)));
        return (al << 24) | (c & 0xFFFFFF);
    }
}
