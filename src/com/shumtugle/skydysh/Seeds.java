package com.shumtugle.skydysh;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The seed bank: short phrases cut from free keyboard music, each a melody
 * and a bass line of one to four bars. Times are in 24ths of a quarter note,
 * pitches in semitones from the tonic nearest middle C, so a phrase can be
 * moved to any key without knowing where it came from.
 */
final class Seeds {
    static final int UNIT = 24;

    static final class Note {
        final int on, length, pitch, velocity;

        Note(int on, int length, int pitch, int velocity) {
            this.on = on;
            this.length = length;
            this.pitch = pitch;
            this.velocity = velocity;
        }
    }

    static final class Seed {
        int piece, beats, beatUnit, bars, span, tonic, bpm;
        boolean minor;
        Note[] melody, bass;
    }

    final String[] composers, titles;
    final Seed[] seeds;

    private Seeds(String[] composers, String[] titles, Seed[] seeds) {
        this.composers = composers;
        this.titles = titles;
        this.seeds = seeds;
    }

    int size() {
        return seeds.length;
    }

    /** What the phrase was cut from, as it should be shown: composer and work. */
    String origin(Seed s) {
        return composers[s.piece] + " \u2014 " + titles[s.piece];
    }

    static Seeds read(InputStream raw) throws IOException {
        DataInputStream in = new DataInputStream(raw);
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (magic[0] != 'C' || magic[1] != 'P' || magic[2] != 'S' || magic[3] != 'D') {
            throw new IOException("not a seed bank");
        }
        int version = in.readUnsignedByte();
        if (version != 1) throw new IOException("seed bank version " + version);
        int pieces = in.readUnsignedShort();
        String[] composers = new String[pieces], titles = new String[pieces];
        for (int i = 0; i < pieces; i++) {
            composers[i] = text(in);
            titles[i] = text(in);
        }
        int count = in.readUnsignedShort();
        Seed[] seeds = new Seed[count];
        for (int i = 0; i < count; i++) {
            Seed s = new Seed();
            s.piece = in.readUnsignedShort();
            if (s.piece >= pieces) throw new IOException("seed " + i + " has no piece");
            s.beats = in.readUnsignedByte();
            s.beatUnit = in.readUnsignedByte();
            s.bars = in.readUnsignedByte();
            s.span = in.readUnsignedShort();
            s.tonic = in.readUnsignedByte();
            s.minor = in.readUnsignedByte() != 0;
            s.bpm = in.readUnsignedByte();
            int melody = in.readUnsignedByte(), bass = in.readUnsignedByte();
            s.melody = notes(in, melody);
            s.bass = notes(in, bass);
            seeds[i] = s;
        }
        return new Seeds(composers, titles, seeds);
    }

    private static Note[] notes(DataInputStream in, int count) throws IOException {
        Note[] out = new Note[count];
        for (int i = 0; i < count; i++) {
            int on = in.readUnsignedShort(), length = in.readUnsignedShort();
            int pitch = in.readByte(), velocity = in.readUnsignedByte();
            out[i] = new Note(on, length, pitch, velocity);
        }
        return out;
    }

    private static String text(DataInputStream in) throws IOException {
        byte[] b = new byte[in.readUnsignedByte()];
        in.readFully(b);
        return new String(b, "UTF-8");
    }
}
