package com.shumtugle.skydysh;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * The game's words. English is kept here; another language comes as a file of its own among the
 * assets (lang/<code>.txt: one key=value a line, the lines of a text parted by a bar, {n} and
 * {time} filled in), and is used when the phone, or the game alone, is set to it.
 */
final class Lang {
    private static final HashMap<String, String> EN = new HashMap<String, String>();
    private static HashMap<String, String> words = EN;

    static {
        EN.put("brief.first", "DISPATCH - MISSION {n}||AT SUNSET FIVE GOLEMS|BROKE OUT OF THE FOUNDRY.|THEIR PLATES ARE FORGED."
                + "|FORGED PLATES BURN:|THE GOLEMS BURN WITH THEM|AND SO WILL THE TOWN.||THEY HIDE ABOUT THE STREETS."
                + "|FIND THEM. RUN THEM DOWN.|KNOCK EVERY PLATE OFF.||TOUCH TO GO");
        EN.put("brief.more", "DISPATCH - MISSION {n}||FIVE MORE HAVE BROKEN OUT|WITH FORGED PLATES.||FIND THEM. RUN THEM DOWN."
                + "|KNOCK EVERY PLATE OFF.||TOUCH TO GO");
        EN.put("report", "DISPATCH - MISSION {n}||ALL FIVE PLATES ARE IN.|THE TOWN IS SAFE|FOR TONIGHT.||TIME  {time}"
                + "||MORE ARE LOOSE.||TOUCH TO GO ON");
    }

    private Lang() {
    }

    /** The language by its code (two letters): its file, if the game has one; English otherwise. */
    static void use(World.Host host, String code) {
        words = EN;
        if (code == null || code.equals("en")) return;
        byte[] b = host.asset("lang/" + code + ".txt");
        if (b == null) return;
        HashMap<String, String> m = new HashMap<String, String>();
        for (String line : new String(b, StandardCharsets.UTF_8).split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq > 0) m.put(line.substring(0, eq).trim(), line.substring(eq + 1));
        }
        words = m;
    }

    /** A text as its lines, with the mission's number and a time filled in. */
    static String[] lines(String key, int n, String time) {
        String s = words.get(key);
        if (s == null) s = EN.get(key);
        s = s.replace("{n}", String.valueOf(n)).replace("{time}", time == null ? "" : time);
        return s.split("\\|", -1);
    }
}
