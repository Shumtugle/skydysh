package com.shumtugle.skydysh;

/**
 * A composed piece: every note with its start, length, pitch, loudness and
 * the hand that plays it, sorted by start. Time is counted in ticks, 24 to a
 * quarter note, so the tempo can change without the piece changing.
 */
final class Piece {
    static final int TPQ = Seeds.UNIT;

    final int[] at, length, pitch, velocity;
    final boolean[] low;
    final int end, beats, beatUnit, tonic;
    final boolean minor;
    final String origin;

    Piece(int[] at, int[] length, int[] pitch, int[] velocity, boolean[] low,
          int end, int beats, int beatUnit, int tonic, boolean minor, String origin) {
        this.at = at;
        this.length = length;
        this.pitch = pitch;
        this.velocity = velocity;
        this.low = low;
        this.end = end;
        this.beats = beats;
        this.beatUnit = beatUnit;
        this.tonic = tonic;
        this.minor = minor;
        this.origin = origin;
    }

    int size() {
        return at.length;
    }

    /** Ticks for a number of milliseconds at a tempo in quarter notes a minute. */
    static int ticks(long ms, int bpm) {
        return (int) (ms * bpm * TPQ / 60000L);
    }

    static long millis(int ticks, int bpm) {
        return ticks * 60000L / ((long) bpm * TPQ);
    }

    // ---- the file the phone's own synthesizer plays

    /** A standard MIDI file of the piece from a given tick on, at a tempo, on a piano. */
    byte[] midi(int bpm, int from) {
        return midi(bpm, from, 0, 0, 40);
    }

    /** The same with instruments of the General MIDI set for the tune and the bass, and an amount of room. */
    byte[] midi(int bpm, int from, int tune, int bassVoice, int room) {
        int n = 0;
        for (int i = 0; i < at.length; i++) if (at[i] >= from) n++;
        // each note as an on and an off; at the same tick an off goes first
        long[] events = new long[n * 2];
        int k = 0;
        for (int i = 0; i < at.length; i++) {
            if (at[i] < from) continue;
            events[k++] = pack(at[i] - from, 1, i);
            events[k++] = pack(at[i] + length[i] - from, 0, i);
        }
        java.util.Arrays.sort(events);
        java.io.ByteArrayOutputStream track = new java.io.ByteArrayOutputStream();
        int tempo = 60000000 / bpm;
        meta(track, 0x51, new byte[]{(byte) (tempo >> 16), (byte) (tempo >> 8), (byte) tempo});
        meta(track, 0x58, new byte[]{(byte) beats, (byte) Integer.numberOfTrailingZeros(beatUnit), 24, 8});
        for (int channel = 0; channel < 2; channel++) {
            vlq(track, 0);
            track.write(0xC0 | channel);
            track.write((channel == 0 ? tune : bassVoice) & 0x7F);
            control(track, channel, 7, channel == 1 && bassVoice != tune ? 84 : 100);
            control(track, channel, 91, room & 0x7F);
        }
        int now = 0;
        for (long e : events) {
            int tick = (int) (e >>> 33), on = (int) (e >>> 32) & 1, i = (int) e;
            vlq(track, tick - now);
            now = tick;
            int channel = low[i] ? 1 : 0;
            track.write((on == 1 ? 0x90 : 0x80) | channel);
            track.write(pitch[i] & 0x7F);
            track.write(on == 1 ? velocity[i] & 0x7F : 0);
        }
        vlq(track, Math.max(0, end - from - now) + TPQ * 2);
        track.write(0xFF);
        track.write(0x2F);
        track.write(0);

        java.io.ByteArrayOutputStream file = new java.io.ByteArrayOutputStream();
        byte[] body = track.toByteArray();
        file.write('M'); file.write('T'); file.write('h'); file.write('d');
        int32(file, 6);
        int16(file, 0);
        int16(file, 1);
        int16(file, TPQ);
        file.write('M'); file.write('T'); file.write('r'); file.write('k');
        int32(file, body.length);
        file.write(body, 0, body.length);
        return file.toByteArray();
    }

    private static long pack(int tick, int on, int index) {
        return ((long) tick << 33) | ((long) on << 32) | index;
    }

    private static void control(java.io.ByteArrayOutputStream out, int channel, int number, int value) {
        vlq(out, 0);
        out.write(0xB0 | channel);
        out.write(number);
        out.write(value);
    }

    private static void meta(java.io.ByteArrayOutputStream out, int type, byte[] data) {
        vlq(out, 0);
        out.write(0xFF);
        out.write(type);
        vlq(out, data.length);
        out.write(data, 0, data.length);
    }

    private static void vlq(java.io.ByteArrayOutputStream out, int value) {
        int buffer = value & 0x7F;
        while ((value >>= 7) > 0) {
            buffer <<= 8;
            buffer |= ((value & 0x7F) | 0x80);
        }
        while (true) {
            out.write(buffer & 0xFF);
            if ((buffer & 0x80) != 0) buffer >>= 8;
            else break;
        }
    }

    private static void int32(java.io.ByteArrayOutputStream out, int v) {
        out.write(v >> 24); out.write(v >> 16); out.write(v >> 8); out.write(v);
    }

    private static void int16(java.io.ByteArrayOutputStream out, int v) {
        out.write(v >> 8); out.write(v);
    }

    // ---- following the music

    /** Where the player's hands should touch and lift, read off the piece as time goes. */
    interface Hands {
        void down(int pitch, int velocity, boolean low);

        void up(int pitch, boolean low);

        void lift();
    }

    /**
     * A cursor over the piece. Moved forward to a tick, it presses every
     * note that has begun and lifts every note that has ended on the way,
     * in the order they happen.
     */
    static final class Cue {
        private final Piece piece;
        private final long[] events;
        private int next;

        Cue(Piece piece) {
            this.piece = piece;
            events = new long[piece.size() * 2];
            for (int i = 0; i < piece.size(); i++) {
                events[2 * i] = pack(piece.at[i], 1, i);
                events[2 * i + 1] = pack(piece.at[i] + piece.length[i], 0, i);
            }
            java.util.Arrays.sort(events);
        }

        /** Starts again at a tick with nothing held; notes begun before it are not pressed. */
        void seek(int tick, Hands hands) {
            hands.lift();
            next = 0;
            while (next < events.length && (int) (events[next] >>> 33) < tick) next++;
            // an off at the seek point belongs to a note never pressed
            while (next < events.length && (int) (events[next] >>> 33) == tick
                    && ((events[next] >>> 32) & 1) == 0) next++;
        }

        void advance(int tick, Hands hands) {
            while (next < events.length && (int) (events[next] >>> 33) <= tick) {
                long e = events[next++];
                int i = (int) e;
                if (((e >>> 32) & 1) == 1) {
                    if (piece.at[i] + piece.length[i] > tick) hands.down(piece.pitch[i], piece.velocity[i], piece.low[i]);
                } else {
                    hands.up(piece.pitch[i], piece.low[i]);
                }
            }
        }

        boolean done() {
            return next >= events.length;
        }
    }
}
