package com.shumtugle.skydysh;

/**
 * The book the green brum reads on his sofa, in the old flat: the town's story so far and what
 * is to come, opened over the whole frame from the sign over it. A page at a time, as a printed
 * book: a cream leaf on a dark ground, the words in the dispatch's letters but in ink, a heading
 * where a chapter begins, its number at the foot. A touch on the right of the leaf, or a finger
 * drawn to the left, turns it on (the leaf goes over on its spine at the left, the next one
 * under it); on the left, or drawn to the right, back. The cross at the top closes it, as does
 * the way back.
 *
 * Its words are an asset of the language (book/xx.txt): a line "TITLE=..." for the cover, a line
 * begun with "# " for a chapter's heading, the rest paragraphs, an empty line between them.
 */
final class Book {
    // the ground and the leaf: margins of the frame, frame pixels; the words so far in from its edges
    static final int SIDE = 10, TOP = 40, FOOT = 64, INSET = 14;
    static final float FADE = 0.2f, TURN = 0.42f;
    static final float SWIPE = 28f, TAP = 10f;
    // colours: the dark ground, the leaf, its ink, the heading's and the number's, the cover
    static final int GROUND = 0xFF1F1A15, LEAF = 0xFFF4ECD8, LEAF_EDGE = 0xFFD9CCAE, INK = 0xFF2A2520,
            QUIET = 0xFF8A7858, COVER_FROM = 0xFF5A4530, COVER_TO = 0xFF2A1D12, COVER_INK = 0xFFD4B896,
            BUTTON = 0xFF4A3C2C, BUTTON_INK = 0xFFD4B896;

    private final String title;
    private final java.util.ArrayList<String> heads = new java.util.ArrayList<String>();
    private final java.util.ArrayList<String> paras = new java.util.ArrayList<String>();
    // each paragraph's chapter (an index into heads), -1 for none
    private final java.util.ArrayList<Integer> paraHead = new java.util.ArrayList<Integer>();

    // the pages as laid out last: each a list of lines and what each line is
    private java.util.ArrayList<String[]> pages;
    private java.util.ArrayList<int[]> kinds;
    private int laidW = -1, laidH = -1;
    static final int BODY = 0, HEAD = 1, GAP = 2;

    private int page;            // the page open (0: the cover)
    private int turnTo = -1;     // the page being turned to, while it turns
    private float turnT, t;
    private float downX = -1f, downY;
    boolean closed;

    private Px leafA, leafB;

    private Book(String text) {
        String ti = "";
        StringBuilder para = new StringBuilder();
        int head = -1;
        for (String raw : text.split("\n")) {
            String l = raw.trim();
            if (l.startsWith("TITLE=")) {
                ti = l.substring(6).trim();
                continue;
            }
            if (l.startsWith("# ") || l.isEmpty()) {
                if (para.length() > 0) {
                    paras.add(para.toString());
                    paraHead.add(head);
                    head = -2;
                    para.setLength(0);
                }
                if (l.startsWith("# ")) {
                    heads.add(l.substring(2).trim());
                    head = heads.size() - 1;
                }
                continue;
            }
            if (para.length() > 0) para.append(' ');
            para.append(l);
        }
        if (para.length() > 0) {
            paras.add(para.toString());
            paraHead.add(head);
        }
        title = ti;
    }

    /** The book in the language now, from its asset (null: there is none). */
    static Book open(World.Host host, String lang) {
        byte[] b = host.asset("book/" + lang + ".txt");
        if (b == null) b = host.asset("book/en.txt");
        if (b == null) return null;
        try {
            return new Book(new String(b, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    int pages() {
        return pages == null ? 1 : pages.size() + 1;
    }

    int page() {
        return page;
    }

    void update(float dt) {
        t += dt;
        if (turnTo >= 0) {
            turnT += dt;
            if (turnT >= TURN) {
                page = turnTo;
                turnTo = -1;
            }
        }
    }

    /** Open at page n at once (as kept from the last time), if the book has it. */
    void turnTo(int n) {
        page = Math.max(0, n);
        clampAfterLay = true;
    }

    private boolean clampAfterLay;

    /** On a page, or back, if there is one and none is turning. */
    void turn(int by) {
        if (turnTo >= 0) {
            page = turnTo;
            turnTo = -1;
        }
        int to = Math.max(0, Math.min(pages() - 1, page + by));
        if (to == page) return;
        turnTo = to;
        turnT = 0f;
    }

    /** A finger on the book (frame pixels, the frame w by h): the cross, the buttons, the leaf. */
    void touch(int type, float x, float y, int w, int h) {
        if (type == World.DOWN) {
            downX = x;
            downY = y;
            return;
        }
        if (type != World.UP || downX < 0f) return;
        float dx = x - downX, dy = y - downY;
        downX = -1f;
        if (t < FADE) return;
        if (Math.abs(dx) > SWIPE && Math.abs(dx) > Math.abs(dy)) {
            turn(dx < 0f ? 1 : -1);
            return;
        }
        if (Math.abs(dx) > TAP || Math.abs(dy) > TAP) return;
        // the cross at the top right
        if (x > w - 40 && y < TOP - 4) {
            closed = true;
            return;
        }
        // the buttons at the foot: back on the left, on at the right
        int by = h - FOOT + 14;
        if (y > by - 8) {
            if (x < w * 0.3f) turn(-1);
            else if (x > w * 0.7f) turn(1);
            return;
        }
        turn(x < w * 0.5f ? -1 : 1);
    }

    /** The words laid out to a leaf of w by h (inside its insets): lines broken at spaces, pages of them. */
    private void lay(TubeFont font, int w, int h) {
        if (laidW == w && laidH == h) return;
        laidW = w;
        laidH = h;
        pages = new java.util.ArrayList<String[]>();
        kinds = new java.util.ArrayList<int[]>();
        int lineH = font.rows + 3, gap = lineH / 2, headH = lineH + 10;
        java.util.ArrayList<String> lines = new java.util.ArrayList<String>();
        java.util.ArrayList<Integer> what = new java.util.ArrayList<Integer>();
        int used = 0;
        String indent = "   ";
        for (int i = 0; i < paras.size(); i++) {
            int head = paraHead.get(i);
            if (head >= 0) {
                // a chapter begins a page of its own
                if (!lines.isEmpty()) {
                    flush(lines, what);
                    used = 0;
                }
                lines.add(heads.get(head));
                what.add(HEAD);
                used += headH;
            } else if (!lines.isEmpty()) {
                if (used + gap + lineH > h) {
                    flush(lines, what);
                    used = 0;
                } else {
                    lines.add("");
                    what.add(GAP);
                    used += gap;
                }
            }
            // the first paragraph of a chapter not indented, the rest so
            String[] words = paras.get(i).split(" ");
            StringBuilder row = new StringBuilder(head >= 0 ? "" : indent);
            boolean fresh = true;
            for (String wd : words) {
                String tryRow = fresh ? row + wd : row + " " + wd;
                if (!fresh && font.width(tryRow) > w) {
                    if (used + lineH > h) {
                        flush(lines, what);
                        used = 0;
                    }
                    lines.add(row.toString());
                    what.add(BODY);
                    used += lineH;
                    row = new StringBuilder(wd);
                } else row = new StringBuilder(tryRow);
                fresh = false;
            }
            if (used + lineH > h) {
                flush(lines, what);
                used = 0;
            }
            lines.add(row.toString());
            what.add(BODY);
            used += lineH;
        }
        if (!lines.isEmpty()) flush(lines, what);
        leafA = leafB = null;
        if (clampAfterLay) page = Math.min(page, pages() - 1);
    }

    private void flush(java.util.ArrayList<String> lines, java.util.ArrayList<Integer> what) {
        pages.add(lines.toArray(new String[0]));
        int[] k = new int[what.size()];
        for (int i = 0; i < k.length; i++) k[i] = what.get(i);
        kinds.add(k);
        lines.clear();
        what.clear();
    }

    // the book in the green brum's hands: each leaf so wide, so high, so thick; opened so far
    // either side of the spine, leaning back so much
    static final float HALF_W = 0.24f, HALF_H = 0.32f, THICK = 0.018f, SPREAD = 0.42f, LEAN = 0.55f;
    // its cover, seen from across the room: oxblood, apart from the brick of whoever holds it
    static final int HELD_COVER = 0x7A2430;
    private static final float[] m = new float[9], ry = new float[9], rx = new float[9], rh = new float[9], tmp = new float[9];

    /** The book held open at (x, y, z), its reader turned by yaw: the two leaves, the cover under them. */
    static void held(DrawList d, float x, float y, float z, float yaw) {
        turnY(rh, yaw);
        turnX(rx, -LEAN);
        mul(rh, rx, tmp);
        for (int side = -1; side <= 1; side += 2) {
            turnY(ry, -side * SPREAD);
            mul(tmp, ry, m);
            float lx = side * HALF_W * (float) Math.cos(SPREAD), lz = HALF_W * (float) Math.sin(SPREAD);
            // from the book's frame to the world: leaning, then turned with its reader
            float wx = tmp[0] * lx + tmp[2] * lz, wy = tmp[3] * lx + tmp[5] * lz, wz = tmp[6] * lx + tmp[8] * lz;
            float nx = m[2], ny = m[5], nz = m[8];
            d.box(x + wx, y + wy, z + wz, HALF_W, HALF_H, THICK, m, HELD_COVER, DrawList.PAINT);
            // the leaves, a block of them thick enough to show cream over the cover's edge
            float o = THICK * 2.2f;
            d.box(x + wx + nx * o, y + wy + ny * o, z + wz + nz * o, HALF_W - 0.015f, HALF_H - 0.015f, THICK * 1.6f, m, LEAF & 0xFFFFFF, DrawList.PLAIN);
        }
    }

    private static void turnY(float[] r, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        r[0] = c; r[1] = 0f; r[2] = s;
        r[3] = 0f; r[4] = 1f; r[5] = 0f;
        r[6] = -s; r[7] = 0f; r[8] = c;
    }

    private static void turnX(float[] r, float a) {
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        r[0] = 1f; r[1] = 0f; r[2] = 0f;
        r[3] = 0f; r[4] = c; r[5] = -s;
        r[6] = 0f; r[7] = s; r[8] = c;
    }

    private static void mul(float[] a, float[] b, float[] out) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                out[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
    }

    /** The book over the whole frame, the words in the font. */
    void draw(Px f, TubeFont font) {
        float up = Math.min(1f, t / FADE);
        int a = Math.round(255f * up);
        int ground = (a << 24) | (GROUND & 0xFFFFFF);
        java.util.Arrays.fill(f.a, ground);
        if (font == null || up < 0.6f) return;
        int lw = f.w - 2 * SIDE, lh = f.h - TOP - FOOT;
        lay(font, lw - 2 * INSET, lh - 2 * INSET - font.rows - 6);
        if (leafA == null || leafA.w != lw || leafA.h != lh) {
            leafA = new Px(lw, lh);
            leafB = new Px(lw, lh);
        }
        if (turnTo < 0) {
            leaf(leafA, page, font);
            f.blit(leafA, SIDE, TOP);
        } else {
            // turning on: the next leaf under, this one going over on its spine; back: the one
            // before coming over onto this one
            float p = Math.min(1f, turnT / TURN), e = p * p * (3f - 2f * p);
            boolean on = turnTo > page;
            leaf(leafA, on ? turnTo : page, font);
            leaf(leafB, on ? page : turnTo, font);
            f.blit(leafA, SIDE, TOP);
            float width = on ? (float) Math.cos(e * Math.PI * 0.5f) : (float) Math.sin(e * Math.PI * 0.5f);
            squeezed(f, leafB, SIDE, TOP, width);
        }
        // the cross at the top right
        int cx = f.w - 22, cy = TOP / 2;
        for (int k = -6; k <= 6; k++) {
            for (int s = 0; s < 2; s++) {
                f.set(cx + k + s, cy + k, COVER_INK);
                f.set(cx + k + s, cy - k, COVER_INK);
            }
        }
        // the buttons at the foot, and where the reader is between them
        int by = f.h - FOOT + 14;
        button(f, font, SIDE, by, "<", page > 0);
        button(f, font, f.w - SIDE - 56, by, ">", page < pages() - 1);
        String at = (page + 1) + " / " + pages();
        font.draw(f, at, (f.w - font.width(at)) / 2, by + 10, QUIET, null);
    }

    private void button(Px f, TubeFont font, int x, int y, String s, boolean live) {
        int c = live ? BUTTON : Px.mix(GROUND, BUTTON, 0.4f);
        f.rect(x, y, x + 56, y + 34, c);
        font.draw(f, s, x + (56 - font.width(s)) / 2, y + (34 - font.rows) / 2, live ? BUTTON_INK : Px.mix(c, BUTTON_INK, 0.35f), null);
    }

    /** A leaf drawn onto out, from its left edge (the spine) only so wide a share of it, shaded as it goes over. */
    private static void squeezed(Px f, Px leaf, int x0, int y0, float share) {
        int w = Math.round(leaf.w * Math.max(0f, Math.min(1f, share)));
        if (w <= 0) return;
        float dark = 0.45f * (1f - share);
        for (int x = 0; x < w; x++) {
            int sx = Math.min(leaf.w - 1, (int) (x / (float) w * leaf.w));
            float edge = dark + 0.25f * (x / (float) w) * (1f - share);
            for (int y = 0; y < leaf.h; y++) {
                int c = leaf.a[y * leaf.w + sx];
                f.set(x0 + x, y0 + y, Px.mix(c, 0xFF000000, Math.min(0.8f, edge)));
            }
        }
        // the shadow it casts on the leaf under it, past its edge
        for (int x = w; x < Math.min(leaf.w, w + 10); x++) {
            float k = 0.3f * (1f - (x - w) / 10f) * (1f - share);
            for (int y = 0; y < leaf.h; y++) {
                int o = (y0 + y) * f.w + x0 + x;
                if (o >= 0 && o < f.a.length) f.a[o] = Px.mix(f.a[o], 0xFF000000, k);
            }
        }
    }

    /** Page n drawn onto a leaf: the cover (0) or a page of the words, its number at the foot. */
    private void leaf(Px p, int n, TubeFont font) {
        if (n == 0) {
            for (int y = 0; y < p.h; y++) {
                for (int x = 0; x < p.w; x++) p.a[y * p.w + x] = Px.mix(COVER_FROM, COVER_TO, (x + y) / (float) (p.w + p.h));
            }
            // a thin frame inside the edge
            int m = 12;
            p.rect(m, m, p.w - m, m + 1, COVER_INK);
            p.rect(m, p.h - m - 1, p.w - m, p.h - m, COVER_INK);
            p.rect(m, m, m + 1, p.h - m, COVER_INK);
            p.rect(p.w - m - 1, m, p.w - m, p.h - m, COVER_INK);
            // the title broken to the width, in the middle
            java.util.ArrayList<String> rows = new java.util.ArrayList<String>();
            StringBuilder row = new StringBuilder();
            for (String wd : title.toUpperCase().split(" ")) {
                String tryRow = row.length() == 0 ? wd : row + " " + wd;
                if (row.length() > 0 && font.width(tryRow) * 2 > p.w - 4 * m) {
                    rows.add(row.toString());
                    row = new StringBuilder(wd);
                } else row = new StringBuilder(tryRow);
            }
            rows.add(row.toString());
            // twice the size of the words inside: each row drawn small, then set down doubled
            int lineH = (font.rows + 4) * 2, y = p.h / 2 - rows.size() * lineH / 2 - 10;
            for (String r : rows) {
                int rw = font.width(r);
                Px small = new Px(Math.max(1, rw), font.rows);
                font.draw(small, r, 0, 0, COVER_INK, null);
                int x0 = (p.w - rw * 2) / 2;
                for (int sy = 0; sy < small.h; sy++)
                    for (int sx = 0; sx < small.w; sx++) {
                        int c = small.a[sy * small.w + sx];
                        if ((c >>> 24) == 0) continue;
                        p.rect(x0 + sx * 2, y + sy * 2, x0 + sx * 2 + 2, y + sy * 2 + 2, c);
                    }
                y += lineH;
            }
            int ry = y + 8;
            p.rect(p.w / 2 - 20, ry, p.w / 2 + 20, ry + 1, COVER_INK);
            return;
        }
        java.util.Arrays.fill(p.a, LEAF);
        p.rect(0, 0, 2, p.h, LEAF_EDGE);
        String[] lines = pages.get(n - 1);
        int[] k = kinds.get(n - 1);
        int lineH = font.rows + 3, gap = lineH / 2;
        int y = INSET;
        for (int i = 0; i < lines.length; i++) {
            if (k[i] == GAP) {
                y += gap;
                continue;
            }
            if (k[i] == HEAD) {
                String s = lines[i].toUpperCase();
                font.draw(p, s, (p.w - font.width(s)) / 2, y, QUIET, null);
                p.rect(p.w / 2 - 16, y + font.rows + 4, p.w / 2 + 16, y + font.rows + 5, QUIET);
                y += lineH + 10;
                continue;
            }
            font.draw(p, lines[i], INSET, y, INK, null);
            y += lineH;
        }
        String num = String.valueOf(n);
        font.draw(p, num, (p.w - font.width(num)) / 2, p.h - INSET - font.rows + 4, QUIET, null);
    }
}
