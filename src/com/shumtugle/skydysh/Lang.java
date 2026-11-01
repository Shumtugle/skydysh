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
        EN.put("brief.first", "Dispatch \u2014 mission {n}||At sunset five brums|broke out of the foundry.|Their forged plates burn:"
                + "|the brums burn with them,|and so will the town."
                + "||Find them. Run them down.|Knock every plate off.|You have five minutes,|or the mission fails.||Touch to go");
        EN.put("brief.more", "Dispatch \u2014 mission {n}||Five more have broken out|with forged plates.||Find them. Run them down."
                + "|Knock every plate off.|You have five minutes,|or the mission fails.||Touch to go");
        EN.put("brief.late", "Dispatch \u2014 mission {n}||The rest got away.|Five are loose again.||Five minutes once more."
                + "|Find them. Run them down.|Knock every plate off.||Touch to go");
        EN.put("report.late", "Dispatch \u2014 mission {n}||Five minutes are up.|Plates in: {caught} of 5.|The rest got away"
                + "|into the streets.||Mission failed.||Touch to go on");
        EN.put("late.short", "TIME UP");
        EN.put("brief.delivery", "Dispatch \u2014 mission {n}||A load is waiting at the|factory with the chimney,|west along the shore."
                + "|It is wanted at the store,|east along the shore.||Take a duty pickup.|Stand in the bay at each gate."
                + "|Then home.||The pay: 50 000.||Touch to go");
        EN.put("report.delivery", "Dispatch \u2014 mission {n}||The load is in the store.|The pay: {reward}.|In all: {sum}.||Time  {time}"
                + "||Touch to go on");
        EN.put("goal.pickup", "PICKUP");
        EN.put("goal.factory", "FACTORY");
        EN.put("goal.store", "STORE");
        EN.put("goal.home", "HOME");
        EN.put("home", "Dispatch \u2014 home||Which mission now?||1  The hunt|2  The folder|3  The delivery||Or go on: mission {n}");
        EN.put("controls.one", "ONE FINGER");
        EN.put("controls.two", "TWO THUMBS");
        EN.put("report", "Dispatch \u2014 mission {n}||All five plates are in.|The town is safe|for tonight.||Time  {time}"
                + "||Touch to go on");
        EN.put("brief.folder", "Dispatch \u2014 mission {n}||A folder of papers fell|out of a helicopter's cabin.|It lies somewhere on the roofs."
                + "||The reward: 100 000.||Brums are after it too.|Get there first: up a fire|escape, then over the roofs.|In the air, grab a ladder;|flick up to rise higher."
                + "||Touch to go");
        EN.put("brief.again", "Dispatch \u2014 mission {n}||The brum dropped the folder|running over the roofs.|The helicopter saw where."
                + "||One more try.|The reward is still 100 000.||Touch to go");
        EN.put("report.folder", "Dispatch \u2014 mission {n}||The folder is ours.|The reward: {reward}.|In all: {sum}.||Time  {time}"
                + "||Touch to go on");
        EN.put("report.lost", "Dispatch \u2014 mission {n}||A brum got there first.|Not for long: it is running|over the roofs with it."
                + "||Touch to go on");
        EN.put("lost.short", "TOO LATE");
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
