package com.shumtugle.skydysh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Writes a new piece from the seed bank each time it is asked.
 *
 * A work is chosen first and a phrase from it second, so that the composers
 * with many works in the bank do not drown the others. The phrase keeps its
 * rhythm and its outline and is moved to a key of the piece's own, sometimes
 * to the other mode. From it a section grows: a one-bar phrase is answered by
 * itself a step higher or lower, varied, and brought to a close; a longer
 * phrase is followed by its varied close. A second phrase from the same work,
 * where there is one, makes the contrasting section. The form is A A' B B' A'',
 * the first of each pair ending open and the second at home. The bass comes
 * with the phrase; where a phrase has none, one is found from the melody.
 */
final class Composer {
    private static final int[] MAJOR = {0, 2, 4, 5, 7, 9, 11};
    private static final int[] MINOR = {0, 2, 3, 5, 7, 8, 10};

    private final Seeds bank;
    private final Random rnd;
    private final List<List<Integer>> byPiece = new ArrayList<List<Integer>>();
    private final List<List<Integer>> byComposer = new ArrayList<List<Integer>>();
    private final List<String> names = new ArrayList<String>();

    Composer(Seeds bank, Random rnd) {
        this.bank = bank;
        this.rnd = rnd;
        Map<String, Integer> composers = new HashMap<String, Integer>();
        for (int p = 0; p < bank.titles.length; p++) byPiece.add(new ArrayList<Integer>());
        for (int i = 0; i < bank.seeds.length; i++) byPiece.get(bank.seeds[i].piece).add(i);
        for (int p = 0; p < bank.titles.length; p++) {
            if (byPiece.get(p).isEmpty()) continue;
            Integer c = composers.get(bank.composers[p]);
            if (c == null) {
                c = byComposer.size();
                composers.put(bank.composers[p], c);
                byComposer.add(new ArrayList<Integer>());
                names.add(bank.composers[p]);
            }
            byComposer.get(c).add(p);
        }
    }

    // ---- a phrase as notes on a scale

    /** One note of a phrase: when, how long, which degree of the scale and how far off it. */
    private static final class Tone {
        int on, length, degree, off, velocity;
        boolean kept;
        int pitch;

        Tone copy() {
            Tone t = new Tone();
            t.on = on;
            t.length = length;
            t.degree = degree;
            t.off = off;
            t.velocity = velocity;
            t.kept = kept;
            t.pitch = pitch;
            return t;
        }
    }

    private static final class Phrase {
        int span;
        List<Tone> melody = new ArrayList<Tone>(), bass = new ArrayList<Tone>();

        Phrase copy() {
            Phrase p = new Phrase();
            p.span = span;
            for (Tone t : melody) p.melody.add(t.copy());
            for (Tone t : bass) p.bass.add(t.copy());
            return p;
        }
    }

    /** Notes from the tonic onwards, as degrees: on the scale where they can be, else kept as they are. */
    private static Tone tone(Seeds.Note n, int[] scale) {
        Tone t = new Tone();
        t.on = n.on;
        t.length = n.length;
        t.velocity = n.velocity;
        int octave = Math.floorDiv(n.pitch, 12), pc = Math.floorMod(n.pitch, 12);
        int degree = 0;
        for (int d = 0; d < 7; d++) if (scale[d] <= pc) degree = d;
        t.degree = octave * 7 + degree;
        t.off = pc - scale[degree];
        t.pitch = n.pitch;
        return t;
    }

    private static Phrase phrase(Seeds.Seed s) {
        int[] scale = s.minor ? MINOR : MAJOR;
        Phrase p = new Phrase();
        p.span = s.span;
        for (Seeds.Note n : s.melody) p.melody.add(tone(n, scale));
        for (Seeds.Note n : s.bass) p.bass.add(tone(n, scale));
        return p;
    }

    private static int pitchOf(Tone t, int[] scale, boolean sameMode) {
        // a note off the scale keeps its own place when the mode changes
        if (t.off != 0 && !sameMode && !t.kept) return t.pitch;
        int octave = Math.floorDiv(t.degree, 7), d = Math.floorMod(t.degree, 7);
        return octave * 12 + scale[d] + t.off;
    }

    // ---- ways to change a phrase

    /** The phrase a number of steps up or down the scale. */
    private static Phrase shift(Phrase p, int steps) {
        Phrase q = p.copy();
        for (Tone t : q.melody) { t.degree += steps; t.kept = true; }
        for (Tone t : q.bass) { t.degree += steps; t.kept = true; }
        return q;
    }

    /** A few notes moved a step, and now and then a long note split in two. */
    private Phrase vary(Phrase p) {
        Phrase q = p.copy();
        int n = q.melody.size();
        int changes = 1 + rnd.nextInt(3);
        for (int k = 0; k < changes && n > 2; k++) {
            Tone t = q.melody.get(1 + rnd.nextInt(n - 2));
            t.degree += rnd.nextBoolean() ? 1 : -1;
            t.off = 0;
            t.kept = true;
        }
        if (rnd.nextFloat() < 0.4f) {
            for (int i = 0; i < q.melody.size() - 1; i++) {
                Tone t = q.melody.get(i);
                if (t.length >= Seeds.UNIT && t.length % 2 == 0 && rnd.nextFloat() < 0.5f) {
                    Tone u = t.copy();
                    t.length /= 2;
                    u.on = t.on + t.length;
                    u.length = t.length;
                    u.degree += rnd.nextBoolean() ? 1 : -1;
                    u.off = 0;
                    u.kept = true;
                    q.melody.add(i + 1, u);
                    break;
                }
            }
        }
        return q;
    }

    /**
     * The end of a section: the last melody note comes to rest, at home or,
     * for an open end, on the fifth; the bass of the last bar moves from the
     * fifth to home, or rests on the fifth.
     */
    private static Phrase close(Phrase p, int bar, boolean home) {
        Phrase q = p.copy();
        if (q.melody.isEmpty()) return q;
        Tone last = q.melody.get(q.melody.size() - 1);
        int target = home ? 0 : 4;
        int best = last.degree, gap = Integer.MAX_VALUE;
        for (int d = last.degree - 4; d <= last.degree + 4; d++) {
            if (Math.floorMod(d, 7) == target && Math.abs(d - last.degree) < gap) {
                gap = Math.abs(d - last.degree);
                best = d;
            }
        }
        last.degree = best;
        last.off = 0;
        last.kept = true;
        last.length = q.span - last.on;
        int start = Math.max(0, q.span - bar);
        List<Tone> bass = new ArrayList<Tone>();
        for (Tone t : q.bass) {
            if (t.on < start) {
                t.length = Math.min(t.length, start - t.on);
                bass.add(t);
            }
        }
        int root = -7;
        if (home && bar >= Seeds.UNIT * 2) {
            bass.add(bassTone(start, bar / 2, root + 4));
            bass.add(bassTone(start + bar / 2, bar - bar / 2, root));
        } else {
            bass.add(bassTone(start, bar, home ? root : root + 4));
        }
        q.bass = bass;
        return q;
    }

    private static Tone bassTone(int on, int length, int degree) {
        Tone t = new Tone();
        t.on = on;
        t.length = length;
        t.degree = degree;
        t.velocity = 70;
        t.kept = true;
        return t;
    }

    /** A bass for a phrase that came without one: the chord each bar's melody fits best, by its root. */
    private static void harmonize(Phrase p, int bar) {
        if (!p.bass.isEmpty()) return;
        int[] roots = {0, 3, 4, 5};
        for (int start = 0; start < p.span; start += bar) {
            int best = 0, score = -1;
            for (int r : roots) {
                int s = 0;
                for (Tone t : p.melody) {
                    if (t.on < start || t.on >= start + bar) continue;
                    int d = Math.floorMod(t.degree - r, 7);
                    if (d == 0 || d == 2 || d == 4) s += t.length;
                }
                if (s > score) { score = s; best = r; }
            }
            p.bass.add(bassTone(start, Math.min(bar, p.span - start), best - 7));
        }
    }

    // ---- the whole piece

    /**
     * A work from the given composers, or from anyone if none of them is in
     * the bank: composers weighed by the square root of their works, then a
     * work at random.
     */
    private int pickPiece(String[] allowed) {
        List<List<Integer>> pool = new ArrayList<List<Integer>>();
        if (allowed != null) {
            for (int i = 0; i < names.size(); i++) {
                for (String a : allowed) if (a.equals(names.get(i))) pool.add(byComposer.get(i));
            }
        }
        if (pool.isEmpty()) pool = byComposer;
        double total = 0;
        for (List<Integer> c : pool) total += Math.sqrt(c.size());
        double r = rnd.nextDouble() * total;
        for (List<Integer> c : pool) {
            r -= Math.sqrt(c.size());
            if (r <= 0) return c.get(rnd.nextInt(c.size()));
        }
        List<Integer> c = pool.get(pool.size() - 1);
        return c.get(rnd.nextInt(c.size()));
    }

    /** A piece from anyone in the bank. */
    Piece compose() {
        return compose(null);
    }

    /** A piece from the works of the given composers. */
    Piece compose(String[] composers) {
        int work = pickPiece(composers);
        List<Integer> seeds = byPiece.get(work);
        Seeds.Seed a = bank.seeds[seeds.get(rnd.nextInt(seeds.size()))];
        Seeds.Seed b = null;
        List<Integer> same = new ArrayList<Integer>();
        for (int i : seeds) {
            Seeds.Seed s = bank.seeds[i];
            if (s != a && s.beats == a.beats && s.beatUnit == a.beatUnit) same.add(i);
        }
        if (!same.isEmpty()) b = bank.seeds[same.get(rnd.nextInt(same.size()))];

        boolean minor = rnd.nextFloat() < 0.75f ? a.minor : !a.minor;
        int[] scale = minor ? MINOR : MAJOR;
        int tonic = rnd.nextInt(12);
        int bar = a.beats * Seeds.UNIT * 4 / a.beatUnit;

        Phrase pa = phrase(a);
        harmonize(pa, bar);
        Phrase pb = b != null ? phrase(b) : shift(vary(pa), 3);
        harmonize(pb, bar);
        boolean sameA = minor == a.minor, sameB = b == null ? sameA : minor == b.minor;

        List<Phrase> form = new ArrayList<Phrase>();
        List<int[]> marks = new ArrayList<int[]>();
        int bBars = b != null ? b.bars : a.bars;
        section(form, marks, 0, pa, a.bars, bar, false, sameA);
        section(form, marks, 1, vary(pa), a.bars, bar, true, sameA);
        section(form, marks, 2, pb, bBars, bar, false, sameB);
        section(form, marks, 3, vary(pb), bBars, bar, true, sameB);
        section(form, marks, 4, vary(pa), a.bars, bar, true, sameA);

        int base = tonic < 6 ? 60 + tonic : 48 + tonic;
        List<int[]> notes = new ArrayList<int[]>();
        int at = 0;
        for (int k = 0; k < form.size(); k++) {
            Phrase p = form.get(k);
            boolean sameMode = marks.get(k)[0] == 1;
            // the contrasting sections a little louder, the last a little softer
            int part = marks.get(k)[1];
            float shape = part == 2 || part == 3 ? 1.08f : part == 4 ? 0.92f : 1f;
            for (Tone t : p.melody) {
                int v = 60 + (t.on % bar == 0 ? 10 : (t.on % Seeds.UNIT == 0 ? 4 : 0)) + rnd.nextInt(6);
                notes.add(new int[]{at + t.on, t.length, clamp(base + pitchOf(t, scale, sameMode), 48, 96),
                        limit((int) (v * shape), 30, 110), 0});
            }
            for (Tone t : p.bass) {
                int v = 50 + (t.on % bar == 0 ? 6 : 0) + rnd.nextInt(5);
                notes.add(new int[]{at + t.on, t.length, clamp(base + pitchOf(t, scale, sameMode), 28, 72),
                        limit((int) (v * shape), 30, 100), 1});
            }
            at += p.span;
        }
        // the last chord rings on for one more bar
        int end = at + bar;
        for (int[] n : notes) if (n[0] + n[1] == at) n[1] += bar;
        java.util.Collections.sort(notes, new java.util.Comparator<int[]>() {
            @Override public int compare(int[] x, int[] y) {
                return x[0] != y[0] ? (x[0] < y[0] ? -1 : 1) : x[4] - y[4];
            }
        });
        int n = notes.size();
        int[] when = new int[n], length = new int[n], pitch = new int[n], velocity = new int[n];
        boolean[] low = new boolean[n];
        for (int i = 0; i < n; i++) {
            int[] x = notes.get(i);
            when[i] = x[0];
            length[i] = Math.max(1, x[1]);
            pitch[i] = x[2];
            velocity[i] = x[3];
            low[i] = x[4] == 1;
        }
        String origin = bank.composers[work] + " \u2014 " + bank.titles[work];
        return new Piece(when, length, pitch, velocity, low, end, a.beats, a.beatUnit, tonic, minor, origin);
    }

    /**
     * One section from a phrase: about four bars, ending open or at home.
     * A phrase of a bar is stated, answered a step away, varied and closed.
     */
    private void section(List<Phrase> form, List<int[]> marks, int part, Phrase p, int bars, int bar,
                         boolean home, boolean sameMode) {
        int[] mark = {sameMode ? 1 : 0, part};
        if (bars <= 1) {
            int step = rnd.nextBoolean() ? 1 : -1;
            form.add(p);
            form.add(shift(p, step));
            form.add(vary(p));
            form.add(close(vary(p), bar, home));
            for (int i = 0; i < 4; i++) marks.add(mark);
        } else {
            form.add(p);
            form.add(close(vary(p), bar, home));
            marks.add(mark);
            marks.add(mark);
        }
    }

    private static int limit(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** A pitch moved by octaves into a range. */
    private static int clamp(int v, int lo, int hi) {
        while (v < lo) v += 12;
        while (v > hi) v -= 12;
        return v;
    }
}
