package com.shumtugle.skydysh;

/**
 * A crowd at the water's edge: people standing on the wet sand in a loose bunch, looking out to
 * sea at something there, the nearest at the water, the rest behind and between. They are dressed
 * as the town's people are, but they do not walk: they stand, turned a little this way and that.
 */
final class Crowd {
    static final int MANY = 24;
    private final Walkers people = new Walkers();
    private final float[] x = new float[MANY], y = new float[MANY], z = new float[MANY], yaw = new float[MANY];
    private final int[] who = new int[MANY];

    /** A crowd on the sand round (cx, the water's edge), looking out at (sx, sz). */
    Crowd(City city, float cx, float sx, float sz) {
        // dressed once, as the town dresses its own
        people.update(0f, new Runner(), new Chase());
        java.util.Random rnd = new java.util.Random(0xC70DL);
        float edge = City.SHORE * City.CELL + City.WATERLINE + 1.2f;
        for (int k = 0; k < MANY; k++) {
            float row = (k % 4) * 1.0f + rnd.nextFloat() * 0.6f;
            x[k] = cx - 11f + k * 0.95f + (rnd.nextFloat() - 0.5f) * 0.8f;
            z[k] = edge + row;
            // looking out at it, each a little off
            yaw[k] = (float) Math.atan2(sx - x[k], sz - z[k]) + (rnd.nextFloat() - 0.5f) * 0.5f;
            who[k] = rnd.nextInt(Walkers.MOST);
            y[k] = city.ground(x[k], z[k]);
        }
    }

    void draw(DrawList d, float ex, float ez) {
        for (int k = 0; k < MANY; k++) {
            float dx = x[k] - ex, dz = z[k] - ez;
            if (dx * dx + dz * dz > 160f * 160f) continue;
            people.standing(d, who[k], x[k], y[k], z[k], yaw[k]);
        }
    }
}
