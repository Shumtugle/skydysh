package com.shumtugle.skydysh;

/**
 * A trial: the game played in a sandbox, for testing. It stands between the world and the real
 * host: what the world keeps goes into a table of its own, forgotten when the trial is left, so
 * the real game (what was won, the likes, where she was, the best times) is never touched.
 *
 * The world in it begins as if every job before the one chosen had been won in its turn, and
 * nothing else had ever happened: the dispatcher's first words, the lessons, the first sights all
 * come again as the first time. Only how the player plays (the language, the controls, the map
 * folded or not, the coordinates shown) is read through from the real game.
 */
final class Trial implements World.Host {
    /** The real host under it. */
    final World.Host real;
    /** The job the trial began with. */
    final int kind;
    private final java.util.HashMap<String, Integer> kept = new java.util.HashMap<String, Integer>();

    /** The settings read through from the real game: how the player plays, not what was played. */
    private static final String[] THROUGH = {World.SAVE_LANG, World.SAVE_CONTROLS, World.SAVE_NAV, World.SAVE_COORDS};

    Trial(World.Host real, int kind) {
        this.real = real;
        this.kind = kind;
        kept.put(World.SAVE_KINDS, before(kind));
    }

    /** The kinds that come before this one in the game's order, a bit each, as if all were won. */
    static int before(int kind) {
        int won = 0;
        for (int k : World.ORDER) {
            if (k == kind) break;
            won |= 1 << k;
        }
        return won;
    }

    @Override
    public void sound(int id, float gain, float rate) {
        real.sound(id, gain, rate);
    }

    @Override
    public void buzz(int ms) {
        real.buzz(ms);
    }

    @Override
    public void saveInt(String key, int value) {
        for (String t : THROUGH)
            if (t.equals(key)) {
                real.saveInt(key, value);
                return;
            }
        kept.put(key, value);
    }

    @Override
    public int loadInt(String key) {
        for (String t : THROUGH) if (t.equals(key)) return real.loadInt(key);
        Integer v = kept.get(key);
        return v == null ? 0 : v;
    }

    @Override
    public byte[] asset(String name) {
        return real.asset(name);
    }

    @Override
    public String language() {
        return real.language();
    }

    /** Notes are not written down in a trial: nothing of it is kept. */
    @Override
    public void journal(String name, String text) {
    }

    @Override
    public void music(byte[] midi) {
        real.music(midi);
    }
}
