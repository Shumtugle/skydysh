package com.shumtugle.skydysh;

/**
 * The marks of places where the way goes: flat little pictures over the frame, half see-through,
 * not glass (the glass is for what is touched). Each is drawn from a picture of its own in
 * squares, a thin dark edge round it so it reads against the sky and the walls. Green, the place
 * is open to her; red, it is not, for now.
 */
final class Marks {
    static final int HOUSE = 0, PAW = 1, FACTORY = 2, WAREHOUSE = 3, TRUCK = 4, PASS = 5, VILLAGE = 6, CLINIC = 7, QUARRY = 8, WORKSHOP = 9;
    static final int OPEN = 0xFF46D27E, SHUT = 0xFFF0583C;
    /** The pictures' squares: each this many frame pixels. */
    static final int SCALE = 1;
    static final int SIDE = 13;
    // how much of the picture shows, and of its edge
    static final float BODY = 0.78f, EDGE = 0.45f;

    private static final String[][] PICTURES = {
        { // a house: a roof to a point, a door
            "......#......",
            ".....###.....",
            "....#####....",
            "...#######...",
            "..#########..",
            ".###########.",
            "#############",
            "..#########..",
            "..#########..",
            "..####.####..",
            "..###...###..",
            "..###...###..",
            "..###...###.."},
        { // a paw: four toes over a pad
            "..##.....##..",
            ".####...####.",
            ".####...####.",
            "..##.....##..",
            "##..........#",
            "###.......###",
            "##...###...##",
            "....#####....",
            "...#######...",
            "..#########..",
            "..#########..",
            "...#######...",
            "....##.##...."},
        { // a factory: saw-tooth roofs, a chimney
            "..........##.",
            "..........##.",
            "..........##.",
            "#....#....##.",
            "##...##...##.",
            "###..###..##.",
            "#############",
            "#############",
            "#.##.##.##.##",
            "#############",
            "#.##.##.##.##",
            "#############",
            "#############"},
        { // a warehouse: a barrel roof, a wide gate
            ".............",
            "....#####....",
            "..#########..",
            ".###########.",
            "#############",
            "#############",
            "#############",
            "##.........##",
            "##.#######.##",
            "##.........##",
            "##.#######.##",
            "##.........##",
            "#############"},
        { // a pickup: a cab, an open bed, two wheels
            ".............",
            ".............",
            ".......####..",
            ".......#..##.",
            ".......#...##",
            "#############",
            "#############",
            "#############",
            "#############",
            "..###...###..",
            ".#####.#####.",
            ".##.##.##.##.",
            "..###...###.."},
        { // a pass: two peaks, the road down between
            ".............",
            "..#.......#..",
            ".###.....###.",
            ".###.....###.",
            "#####...#####",
            "#####...#####",
            "######.######",
            "######.######",
            "######.######",
            "#####...#####",
            "#####...#####",
            "####.....####",
            "####.....####"},
        { // a village: a little house and a bigger one beside it
            ".........#...",
            "..#.....###..",
            ".###...#####.",
            "#####.#######",
            ".###...#####.",
            ".###...#####.",
            ".#.#...##.##.",
            ".#.#...##.##.",
            ".............",
            "#############",
            ".............",
            ".............",
            "............."},
        { // the vets': a house, a cross on it
            "......#......",
            ".....###.....",
            "....#####....",
            "...#######...",
            "..#########..",
            ".###########.",
            "#############",
            "..####.####..",
            "..###...###..",
            "..####.####..",
            "..#########..",
            "..#########..",
            "..#########.."},
        { // a quarry: steps cut down into the ground
            ".............",
            ".............",
            "#############",
            "#...........#",
            "##.........##",
            "##.........##",
            "###.......###",
            "###.......###",
            "####.....####",
            "####.....####",
            "#############",
            ".............",
            "............."},
        { // the workshop: a hammer over a gate
            ".....####....",
            ".....####....",
            ".......#.....",
            ".......#.....",
            ".......#.....",
            "######.######",
            "#...........#",
            "#.#########.#",
            "#.#########.#",
            "#.#########.#",
            "#.#########.#",
            "#.#########.#",
            "#############"},
    };

    /** A picture centred at (cx, cy) over the frame, in a colour (OPEN or SHUT), so much of it showing (0 to 1). */
    static void draw(Px f, int which, float cx, float cy, int color, float shown) {
        String[] p = PICTURES[which];
        int x0 = Math.round(cx - SIDE * SCALE * 0.5f), y0 = Math.round(cy - SIDE * SCALE * 0.5f);
        for (int r = -1; r <= SIDE; r++)
            for (int c = -1; c <= SIDE; c++) {
                boolean in = on(p, r, c);
                boolean edge = !in && (on(p, r - 1, c) || on(p, r + 1, c) || on(p, r, c - 1) || on(p, r, c + 1));
                if (!in && !edge) continue;
                int rgb = in ? color : 0xFF10202A;
                float a = (in ? BODY : EDGE) * shown;
                for (int y = 0; y < SCALE; y++)
                    for (int x = 0; x < SCALE; x++) put(f, x0 + c * SCALE + x, y0 + r * SCALE + y, rgb, a);
            }
    }

    private static boolean on(String[] p, int r, int c) {
        return r >= 0 && r < SIDE && c >= 0 && c < SIDE && p[r].charAt(c) == '#';
    }

    /** A pixel of the flat layer laid over with a colour, so much of it. */
    private static void put(Px f, int x, int y, int rgb, float a) {
        if (x < 0 || y < 0 || x >= f.w || y >= f.h || a <= 0f) return;
        int o = y * f.w + x, old = f.a[o], oa = old >>> 24;
        if (oa == 0) {
            f.a[o] = (Math.round(255f * Math.min(1f, a)) << 24) | (rgb & 0xFFFFFF);
            return;
        }
        int mixed = Px.mix(old, rgb | 0xFF000000, a);
        int na = Math.max(oa, Math.round(255f * Math.min(1f, a)));
        f.a[o] = (na << 24) | (mixed & 0xFFFFFF);
    }
}
