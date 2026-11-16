package com.shumtugle.skydysh;

/**
 * The trials' own screen, over the whole frame: every job in the game's order, each to be
 * begun as the first time in a sandbox (see Trial). Hidden from the player: it opens after a
 * run of touches on the version in the corner. Under the jobs, inside a trial, the way out of it;
 * at the foot, the way back to the game as it was.
 */
final class Trials {
    /** What touching a row picks, besides a kind: leave the trial, or shut this screen. */
    static final int LEAVE = -1, SHUT = -2;

    static final int GROUND = 0xF2141A2E, HEAD = 0xFFF4E8D2, NOTE = 0xFF8A93B8, ROW = 0xFF7FE6D2,
            NOW = 0xFFFFD27F, OUT = 0xFFFFB48A, FAINT = 0xFF2A3354;
    /** Where the rows begin, and how far apart they are beyond the font's own height. */
    static final int TOP = 54, GAP = 9, SIDE = 22;

    /** The kind of the trial now, or 0 when the game is the real one. */
    private final int now;
    private final int[] pick;
    private final String[] text;
    private final int[] rowTop;
    private int rowH = 20;
    boolean closed;
    private float t;

    Trials(int now) {
        this.now = now;
        int n = World.ORDER.length + (now != 0 ? 2 : 1) + 1;
        pick = new int[n];
        text = new String[n];
        rowTop = new int[n];
        int r = 0;
        for (int k = 0; k < World.ORDER.length; k++) {
            pick[r] = World.ORDER[k];
            text[r++] = (k + 1) + "  " + Lang.lines("kind." + World.ORDER[k], 0, null)[0];
        }
        pick[r] = 0;
        text[r++] = "";
        if (now != 0) {
            pick[r] = LEAVE;
            text[r++] = Lang.lines("trials.leave", 0, null)[0];
        }
        pick[r] = SHUT;
        text[r] = Lang.lines("trials.shut", 0, null)[0];
    }

    void update(float dt) {
        t += dt;
    }

    /** The screen over the frame: nothing of the frame shows through but a shade. */
    void draw(Px f, TubeFont font) {
        java.util.Arrays.fill(f.a, GROUND);
        if (font == null) return;
        rowH = font.rows + GAP;
        String head = Lang.lines("trials.head", 0, null)[0];
        font.draw(f, head, (f.w - font.width(head)) / 2, 18, HEAD, null);
        String note = Lang.lines("trials.note", 0, null)[0];
        font.draw(f, note, (f.w - font.width(note)) / 2, 18 + font.rows + 4, NOTE, null);
        int y = TOP + font.rows;
        for (int k = 0; k < text.length; k++) {
            rowTop[k] = y;
            if (pick[k] > 0 && k % 2 == 1) f.rect(SIDE - 6, y - 4, f.w - SIDE + 6, y + font.rows + 4, FAINT);
            int c = pick[k] == LEAVE || pick[k] == SHUT ? OUT : pick[k] == now ? NOW : ROW;
            font.draw(f, text[k], SIDE, y, c, null);
            y += rowH;
        }
    }

    /** A finger down at (x, y): what the row under it picks (a kind, LEAVE, SHUT), or 0 for none. */
    int touch(float x, float y) {
        if (t < 0.15f) return 0;
        for (int k = 0; k < text.length; k++)
            if (pick[k] != 0 && y >= rowTop[k] - GAP / 2 && y < rowTop[k] + rowH - GAP / 2) return pick[k];
        return 0;
    }
}
