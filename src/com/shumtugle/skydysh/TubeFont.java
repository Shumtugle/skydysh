package com.shumtugle.skydysh;

/**
 * The font of the dispatch screen: small letters and capitals, Latin and Cyrillic, each its own
 * width, as an old tube shows them. Read from a file among the assets (tube.bin):
 * 'TUBE', u8 version, u8 rows, u8 baseline (rows above it), u16 glyphs; each glyph: u16 code,
 * u8 advance, i8 left, u8 width, then for each row ceil(width / 8) bytes, the high bit first.
 */
final class TubeFont {
    final int rows, base;
    private final java.util.HashMap<Character, int[]> glyphs = new java.util.HashMap<Character, int[]>();
    private final int[] blank;

    private TubeFont(byte[] b) {
        java.nio.ByteBuffer in = java.nio.ByteBuffer.wrap(b);
        in.position(5);
        rows = in.get() & 0xFF;
        base = in.get() & 0xFF;
        int n = in.getShort() & 0xFFFF;
        for (int g = 0; g < n; g++) {
            char c = (char) (in.getShort() & 0xFFFF);
            int adv = in.get() & 0xFF, left = in.get(), width = in.get() & 0xFF, bytes = (width + 7) / 8;
            // advance, left, width, then a row of bits each, the leftmost the highest
            int[] gl = new int[3 + rows];
            gl[0] = adv;
            gl[1] = left;
            gl[2] = width;
            for (int y = 0; y < rows && width > 0; y++) {
                int row = 0;
                for (int k = 0; k < bytes; k++) row = (row << 8) | (in.get() & 0xFF);
                gl[3 + y] = row >>> (bytes * 8 - width);
            }
            glyphs.put(c, gl);
        }
        int[] q = glyphs.get('?');
        blank = q != null ? q : new int[3 + rows];
    }

    /** The font from its file, or null if there is none. */
    static TubeFont read(byte[] b) {
        if (b == null || b.length < 9 || b[0] != 'T' || b[1] != 'U' || b[2] != 'B' || b[3] != 'E') return null;
        return new TubeFont(b);
    }

    private int[] glyph(char c) {
        int[] g = glyphs.get(c);
        return g != null ? g : blank;
    }

    /** How wide a line is, frame pixels. */
    int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += glyph(s.charAt(i))[0];
        return w;
    }

    /**
     * A line at (x, y), its top row there, in a color; each row of the letters lit by its own
     * share of it (rowLight, one value a row of the font, from the top), so a beam brighter or
     * weaker can pass over it.
     */
    void draw(Px f, String s, int x, int y, int color, float[] rowLight) {
        for (int i = 0; i < s.length(); i++) {
            int[] g = glyph(s.charAt(i));
            int w = g[2], gx = x + g[1];
            for (int r = 0; r < rows; r++) {
                int bits = g[3 + r];
                if (bits == 0) continue;
                int py = y + r;
                if (py < 0 || py >= f.h) continue;
                int c = color;
                if (rowLight != null && r < rowLight.length) c = Px.scale(color, rowLight[r]);
                for (int k = 0; k < w; k++)
                    if ((bits >>> (w - 1 - k) & 1) != 0) {
                        int px = gx + k;
                        if (px >= 0 && px < f.w) f.a[py * f.w + px] = c;
                    }
            }
            x += g[0];
        }
    }
}
