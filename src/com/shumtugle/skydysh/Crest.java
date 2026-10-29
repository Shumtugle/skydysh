package com.shumtugle.skydysh;

/**
 * The top left of the frame during a level, drawn at the screen's own grain, flat and inked like
 * the street under it: a row of five stars, the ones left yellow, the ones spent gone slate; by
 * them the level's number on a cream badge; under the stars the heavy hammers in hand, one sign
 * and how many; under the badge a pip for each attempt lost at this level, five making the golden
 * hammer.
 *
 * It lives with the game. A blow on the building jolts the badge, harder the harder the blow;
 * a building felled turns the badge over like a coin, the next number on its other face; with
 * the golden hammer in hand the badge is gold. A star spent sinks and greys; a heavy hammer given
 * drops in and bounces.
 */
final class Crest {
    // what it shows, set by the level every frame
    int level, stars, heavyShown, heavyLeft;
    boolean heavyOn, goldOn, won;
    float knocked;                 // share of the building off the plinth
    float markT = -1f;             // the new heavy marks dropping in, seconds; below 0 none
    static final float MARK_FALL = 0.32f, MARK_GAP = 0.24f;
    static final int TRIES = Game.GOLD_AFTER;   // the attempts before the golden hammer
    /** How many heavy marks made a row in an older corner; kept for the most the game gives. */
    static final int ROW = 9;

    // the layer it is drawn on, and the frame's grain on it: so many layer pixels to a frame pixel
    Ink ink;
    static final int K = 4, ROWS = 66;

    private float clock;
    private final float[] burnt = {-1f, -1f, -1f, -1f, -1f};   // seconds since each star was spent
    private int failsShown;
    private float failFlash = -1f;
    private float hitT = -1f, hitK, flipT = -1f, bounceT = -1f;
    private int heavyWas = -1;

    // where things are, frame pixels
    static final float STAR_X = 21f, STAR_GAP = 26.6f, STAR_Y = 31f, STAR_R = 11f;
    static final float BADGE_X = 162f, BADGE_Y = 31f, BADGE_W = 28f, BADGE_H = 25f;

    private static final int NAVY = 0xFF232A4A, YELLOW = 0xFFF4C23C, YELLOW_LIT = 0xFFFAD970, SLATE = 0xFF7F9CA6,
            CREAM = 0xFFF1E6D2, GOLD = 0xFFF2B63A, ORANGE = 0xFFE8622E;

    void update(float dt) {
        clock += dt;
        for (int i = 0; i < burnt.length; i++) if (burnt[i] >= 0f) burnt[i] += dt;
        if (failFlash >= 0f) failFlash += dt;
        if (hitT >= 0f && (hitT += dt) > 0.6f) hitT = -1f;
        if (won && flipT < 0f) flipT = 0f;
        if (flipT >= 0f) flipT += dt;
        if (bounceT >= 0f && (bounceT += dt) > 0.5f) bounceT = -1f;
        if (heavyWas >= 0 && heavyShown > heavyWas) bounceT = 0f;
        heavyWas = heavyShown;
    }

    /** The layer was drawn over by something else: the next frame draws it whole again. */
    void dirty() {
        drawnKey = Long.MIN_VALUE;
    }

    /** A star has just been spent. */
    void burn(int i) {
        if (i >= 0 && i < burnt.length) burnt[i] = 0f;
    }

    /** A new level or a new try: the stars whole again, the badge at rest. */
    void fresh() {
        for (int i = 0; i < burnt.length; i++) burnt[i] = -1f;
        flipT = -1f;
        hitT = -1f;
    }

    /** The attempts as kept from an earlier game: shown as they are, without a flash. */
    void settle(int n) {
        failsShown = n;
    }

    /** The attempts as they now stand, shown once the street is back in sight: a new one flashes. */
    void attempts(int n) {
        if (n > failsShown) failFlash = 0f;
        failsShown = n;
    }

    /** A blow on the building, so hard (meters a second): the badge jolts. */
    void hit(float speed) {
        float k = Math.max(0f, Math.min(1f, (speed - 1f) / 9f));
        if (k <= 0.05f) return;
        if (hitT >= 0f && hitK * (float) Math.exp(-hitT * 7f) > k) return;
        hitT = 0f;
        hitK = k;
    }

    // what was last drawn, so a frame with nothing new keeps the layer as it is
    private long drawnKey = Long.MIN_VALUE;

    void draw(Px f) {
        if (ink == null) return;
        long key = key();
        if (key == drawnKey && !busy()) {
            ink.used = true;
            return;
        }
        drawnKey = key;
        ink.clear();
        stars();
        badge();
        heavy();
        ink.used = true;
    }

    private long key() {
        long k = level;
        k = k * 31 + stars;
        k = k * 31 + failsShown;
        k = k * 31 + heavyShown;
        k = k * 31 + (heavyOn ? 1 : 0);
        k = k * 31 + (goldOn ? 1 : 0);
        k = k * 31 + (won ? 1 : 0);
        return k;
    }

    private boolean busy() {
        if (hitT >= 0f || flipT >= 0f && flipT < 0.6f || bounceT >= 0f || markT >= 0f) return true;
        if (failFlash >= 0f && failFlash < 0.85f) return true;
        for (float b : burnt) if (b >= 0f && b < 0.5f) return true;
        return false;
    }

    private static float ease(float u) {
        u = Math.max(0f, Math.min(1f, u));
        return u * u * (3f - 2f * u);
    }

    // ---- the stars ----

    private void stars() {
        for (int i = 0; i < Game.STARS; i++) {
            float cx = (STAR_X + i * STAR_GAP) * K, cy = STAR_Y * K;
            boolean left = i < stars;
            float u = !left && burnt[i] >= 0f ? Math.min(1f, burnt[i] / 0.45f) : 1f;
            // spent: it sinks a little and greys, and comes back up to its place as slate
            float dip = left ? 0f : (float) Math.sin(u * Math.PI) * 3f * K;
            int fill = left ? YELLOW : Px.mix(YELLOW, SLATE, u);
            int lit = left ? YELLOW_LIT : Px.mix(YELLOW_LIT, SLATE, u);
            star(cx, cy + dip, STAR_R * K, fill, lit);
        }
    }

    /** A five-pointed star, its points a little rounded: a thick ink edge, the fill, a lighter half toward the light. */
    private void star(float cx, float cy, float ro, int fill, int lit) {
        Ink g = ink;
        float ri = ro * 0.47f;
        g.starShape(cx, cy, ro + 1.9f * K, ri + 1.3f * K, 0f);
        g.polyFill(NAVY, NAVY, 1f, 1f);
        g.starShape(cx, cy, ro, ri, 0f);
        g.polyFill(fill, fill, 1f, 1f);
        // the upper left half lighter: a facet
        g.shape(cx, cy - ro, cx, cy, cx - ro * 0.95f, cy - ro * 0.31f);
        g.polyFill(lit, lit, 1f, 0.6f);
    }

    // ---- the badge ----

    private void badge() {
        Ink g = ink;
        float mx = BADGE_X * K, my = BADGE_Y * K, hw = BADGE_W * 0.5f * K, hh = BADGE_H * 0.5f * K;
        float across = 1f, down = 1f;
        int shown = level + 1;
        if (flipT >= 0f) {
            float an = (float) Math.PI * ease(flipT / 0.5f);
            across = Math.max(0.04f, Math.abs((float) Math.cos(an)));
            if (an > Math.PI / 2) shown = level + 2;
        }
        if (hitT >= 0f) {
            float jolt = hitK * 0.2f * (float) Math.exp(-hitT * 9f) * (float) Math.cos(hitT * 30f);
            across *= 1f + jolt;
            down *= 1f - jolt;
        }
        g.squash(mx, my, across, down);
        float r = 7f * K, e = 1.9f * K;
        g.box(mx - hw - e, my - hh - e, mx + hw + e, my + hh + e, r + e, NAVY, NAVY, 1f, 1f);
        int face = goldOn ? GOLD : CREAM;
        g.box(mx - hw, my - hh, mx + hw, my + hh, r, face, face, 1f, 1f);
        String lv = String.valueOf(shown);
        float th = 15f * K;
        float tw = Ink.digitsWidth(lv, th);
        // wide numbers drawn a little narrower, to fit
        float fit = Math.min(1f, (BADGE_W - 6f) * K / tw);
        g.unsquash();
        g.squash(mx, my, across * fit, down);
        g.digits(lv, mx - tw * 0.5f, my - th * 0.5f, th, NAVY, NAVY, 0.4f * K, 1f);
        g.unsquash();
        // the attempts lost here, as pips under it; the newest flashes as it comes
        for (int s = 0; s < TRIES - 1 && s < failsShown; s++) {
            float px = mx + (s - (TRIES - 2) * 0.5f) * 5.5f * K, py = my + hh + 6f * K;
            boolean fresh = s == failsShown - 1 && failFlash >= 0f && failFlash < 0.8f;
            g.disc(px, py, 2.2f * K, NAVY, 1f, 1f);
            g.disc(px, py, 1.4f * K, fresh && ((int) (failFlash * 8f) & 1) == 0 ? 0xFFFFFFFF : ORANGE, 1f, 1f);
        }
    }

    // ---- the heavy hammers ----

    private void heavy() {
        int n = heavyShown;
        boolean falling = markT >= 0f && heavyLeft > heavyShown;
        if (n <= 0 && !falling) return;
        Ink g = ink;
        float hx = (STAR_X + 2f) * K, hy = (STAR_Y + 21f) * K;
        float b = bounceT >= 0f ? -3f * K * (float) Math.abs(Math.sin(bounceT * 12f)) * (float) Math.exp(-bounceT * 8f) : 0f;
        if (n > 0) {
            if (heavyOn) g.box(hx - 11f * K, hy - 11f * K + b, hx + 11f * K, hy + 11f * K + b, 5f * K, 0x55FFFFFF, 0x55FFFFFF, 1f, 1f);
            sign(hx, hy + b, 1f);
            String c = "x" + n;
            float th = 9f * K;
            g.digits(c, hx + 13f * K, hy - th * 0.5f, th, heavyOn ? YELLOW_LIT : 0xFFFFFFFF, NAVY, 1.2f * K, 1f);
        }
        if (falling)
            for (int i = 0; i < heavyLeft - heavyShown; i++) {
                float tk = markT - i * MARK_GAP;
                if (tk <= 0f || tk >= MARK_FALL) continue;
                float u = tk / MARK_FALL;
                sign(hx, hy - 30f * K * (1f - u * u), 0.9f);
            }
    }

    /** A little heavy hammer: an ink shape of it a touch larger, then the hammer itself. */
    private void sign(float hx, float hy, float cover) {
        Hammer.icon(ink, hx, hy, 24f * K, Hammer.HEAVY_KIND, -2, cover);
    }
}
