package com.shumtugle.skydysh;

/**
 * What lies behind a mission, and what it asks: a page over the whole frame, opened from the sign
 * of a question on the dispatch's screen, or from under the pause. Nothing moves on it but the
 * page itself: a still ground of deep blue going to violet at the foot, the words in the letters of
 * the dispatch, but calm, without the tube's beam or its flicker.
 *
 * Its words are lines of the language (see Lang): a line begun with '#' is the title, one begun
 * with '=' the head of a part; an empty line is a gap between paragraphs. Long lines are broken
 * to the page's width at spaces. A page longer than the frame is drawn up and down by a finger;
 * a touch that does not move closes it.
 */
final class Story {
    static final float FADE = 0.2f;              // the page coming up, seconds
    static final int MARGIN = 14;                // either side of the words, frame pixels
    static final float TAP_PX = 10f;             // a finger moved less than this is a touch, not a drag

    private final String[] lines;
    private float t, scroll, most;
    private float downY = -1f, downScroll, moved;
    boolean closed;

    // the page as laid out last: the lines broken to its width, and what each is
    private String[] laid;
    private int[] kind;
    private int laidFor = -1;
    static final int BODY = 0, TITLE = 1, HEAD = 2, GAP = 3, CALL = 4;

    Story(String[] lines) {
        this.lines = lines;
    }

    /** The story of a mission of the kind (the hunt 1, the folder 2, the delivery 3, the call out 4). */
    static Story of(int kind) {
        String[] body = Lang.lines("story." + kind, 0, null);
        String[] call = Lang.lines("story.back", 0, null);
        String[] all = new String[body.length + 2];
        System.arraycopy(body, 0, all, 0, body.length);
        all[body.length] = "";
        all[body.length + 1] = "~" + call[0];
        return new Story(all);
    }

    void update(float dt) {
        t += dt;
    }

    /** A finger on the page: dragged, it moves the page; let go where it came down, the page closes. */
    void touch(int type, float x, float y) {
        if (type == World.DOWN) {
            downY = y;
            downScroll = scroll;
            moved = 0f;
        } else if (type == World.MOVE && downY >= 0f) {
            moved = Math.max(moved, Math.abs(y - downY));
            scroll = Math.max(0f, Math.min(most, downScroll - (y - downY)));
        } else if (type == World.UP && downY >= 0f) {
            if (moved < TAP_PX && t > FADE) closed = true;
            downY = -1f;
        }
    }

    /** The lines broken to so many pixels across, each with what it is. */
    private void lay(TubeFont font, int width) {
        if (laidFor == width) return;
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        java.util.ArrayList<Integer> what = new java.util.ArrayList<Integer>();
        for (String l : lines) {
            int k = BODY;
            if (l.isEmpty()) k = GAP;
            else if (l.startsWith("#")) k = TITLE;
            else if (l.startsWith("=")) k = HEAD;
            else if (l.startsWith("~")) k = CALL;
            String s = k == BODY || k == GAP ? l : l.substring(1).trim();
            if (k == GAP) {
                out.add("");
                what.add(GAP);
                continue;
            }
            // broken at spaces to the width
            String[] words = s.split(" ");
            StringBuilder row = new StringBuilder();
            for (String w : words) {
                String tryRow = row.length() == 0 ? w : row + " " + w;
                if (row.length() > 0 && font.width(tryRow) > width) {
                    out.add(row.toString());
                    what.add(k);
                    row = new StringBuilder(w);
                } else {
                    row = new StringBuilder(tryRow);
                }
            }
            out.add(row.toString());
            what.add(k);
        }
        laid = out.toArray(new String[0]);
        kind = new int[what.size()];
        for (int i = 0; i < kind.length; i++) kind[i] = what.get(i);
        laidFor = width;
    }

    static final int GROUND_TOP = 0xFF101C2A, GROUND_LOW = 0xFF1E1631;
    static final int INK_TITLE = 0xFF7FE6D2, INK_HEAD = 0xFFF2C46A, INK_BODY = 0xFFE9DDB0, INK_CALL = 0xFF9AA6B8;

    /** The page over the whole frame (w by h), the words in the font. */
    void draw(Px f, TubeFont font) {
        float up = Math.min(1f, t / FADE);
        // the ground: still, deep blue at the top going to violet at the foot
        for (int y = 0; y < f.h; y++) {
            int c = Px.mix(GROUND_TOP, GROUND_LOW, y / (float) Math.max(1, f.h - 1));
            int a = Math.round(255f * up);
            c = (a << 24) | (c & 0xFFFFFF);
            int o = y * f.w;
            for (int x = 0; x < f.w; x++) f.a[o + x] = c;
        }
        if (font == null || up < 0.6f) return;
        lay(font, f.w - 2 * MARGIN);
        int lineH = font.rows + 1, gap = font.rows / 2;
        int total = 0;
        for (int i = 0; i < laid.length; i++) total += kind[i] == GAP ? gap : kind[i] == TITLE ? lineH + 4 : lineH;
        int top = 40;
        most = Math.max(0f, total + top + 30 - f.h);
        scroll = Math.min(scroll, most);
        int y = top - Math.round(scroll);
        // a thin rule under the title, as on a page
        for (int i = 0; i < laid.length; i++) {
            int k = kind[i];
            if (k == GAP) {
                y += gap;
                continue;
            }
            int col = k == TITLE ? INK_TITLE : k == HEAD ? INK_HEAD : k == CALL ? INK_CALL : INK_BODY;
            int x = k == CALL ? (f.w - font.width(laid[i])) / 2 : MARGIN;
            if (y > -lineH && y < f.h) font.draw(f, laid[i], x, y, col, null);
            if (k == TITLE) {
                int ry = y + font.rows + 2;
                if (ry >= 0 && ry < f.h) f.rect(MARGIN, ry, f.w - MARGIN, ry + 1, Px.mix(GROUND_TOP, INK_TITLE, 0.35f));
                y += 4;
            }
            y += lineH;
        }
        // more below, or above: a faint shade at that edge
        if (scroll < most - 1f) shade(f, f.h - 24, f.h, false);
        if (scroll > 1f) shade(f, 0, 24, true);
    }

    private static void shade(Px f, int y0, int y1, boolean fromTop) {
        for (int y = Math.max(0, y0); y < Math.min(f.h, y1); y++) {
            float k = (y - y0) / (float) Math.max(1, y1 - y0);
            if (fromTop) k = 1f - k;
            int a = Math.round(200f * k), o = y * f.w;
            for (int x = 0; x < f.w; x++) f.a[o + x] = Px.mix(f.a[o + x], 0xFF1E1631, a / 255f);
        }
    }
}
