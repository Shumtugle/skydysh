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
        EN.put("brief.first", "Dispatch \u2014 job {n}||Five brums are out in the streets|with their plates on fire.|Forged plates, overheated."
                + "||Catch each one.|Knock the plate off with the mallet.||Five minutes for the job.||Touch to go");
        EN.put("brief.more", "Dispatch \u2014 job {n}||Five more brums|with overheated plates.||Catch each one."
                + "|Knock the plate off with the mallet.||Five minutes for the job.||Touch to go");
        EN.put("brief.late", "Dispatch \u2014 job {n}||Replay job.||Five brums. Five plates.|Five minutes.||Touch to go");
        EN.put("report.late", "Dispatch \u2014 job {n}||Time is up.|Plates off: {caught} of 5.||Touch to go on");
        EN.put("late.short", "TIME UP");
        EN.put("report", "Dispatch \u2014 job {n}||All five plates are off.|The brums are already being|put back together.||Time: {time}"
                + "||Touch to go on");
        EN.put("brief.folder", "Dispatch \u2014 job {n}||Find the folder on the roofs.|The helicopter circles over the place."
                + "||Three brums are after it too.|Get there first.||Up a fire escape,|then over the roofs.||Reward: 100 000.||Touch to go");
        EN.put("brief.again", "Dispatch \u2014 job {n}||Replay job.||Find the folder on the roofs|before the others.||Reward: 100 000.||Touch to go");
        EN.put("report.folder", "Dispatch \u2014 job {n}||The folder is ours.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("report.lost", "Dispatch \u2014 job {n}||Another brum took the folder.||Touch to go on");
        EN.put("lost.short", "TOO LATE");
        EN.put("brief.delivery", "Dispatch \u2014 job {n}||Carry four crates|from the factory to the warehouse."
                + "||Take a duty pickup.|The factory is west of home,|the warehouse east.||At each gate stop in the bay|and wait for the crates."
                + "|Then home.||Pay: 50 000.||Touch to go");
        EN.put("report.delivery", "Dispatch \u2014 job {n}||Four crates in the warehouse.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pickup", "PICKUP");
        EN.put("goal.factory", "FACTORY");
        EN.put("goal.store", "WAREHOUSE");
        EN.put("goal.home", "HOME");
        EN.put("goal.free", "HOME, FOR A JOB");
        EN.put("controls.one", "ONE FINGER");
        EN.put("controls.two", "TWO THUMBS");
        EN.put("home", "Dispatch \u2014 home||Which job now?||1  The hunt|2  The folder|3  The delivery||Or on: job {n}");
        EN.put("sofa", "Home. Progress saved.|Which job now?||1  The hunt|2  The folder|3  The delivery|On: job {n}|Just rest");
        EN.put("sofa.again", "Home. Progress saved.|Which job now?||1  The hunt|2  The folder|3  The delivery|Again: job {n}|Just rest");
        EN.put("free.after", "Now, home.|");
        EN.put("best.first", "Best time.|");
        EN.put("best.new", "New record. Was: {time}|");
        EN.put("best", "Best time: {time}|");
        EN.put("pause.lang", "Language: {x}");
        EN.put("lang.system", "system");
        EN.put("pause.results", "Results");
        EN.put("results", "Dispatch \u2014 results||{rows}||Touch to go back");
        EN.put("results.none", "No times yet.");
        EN.put("kind.1", "The hunt");
        EN.put("kind.2", "The folder");
        EN.put("kind.3", "The delivery");
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

    /** A language's name in its own words (as its file gives it; English's, here). */
    static String own(World.Host host, String code) {
        if (code.equals("en")) return "English";
        byte[] b = host.asset("lang/" + code + ".txt");
        if (b != null)
            for (String line : new String(b, StandardCharsets.UTF_8).split("\n"))
                if (line.startsWith("lang.self=")) return line.substring(10).trim();
        return code;
    }

    /** A text as its lines, with the mission's number and a time filled in. */
    static String[] lines(String key, int n, String time) {
        String s = words.get(key);
        if (s == null) s = EN.get(key);
        s = s.replace("{n}", String.valueOf(n)).replace("{time}", time == null ? "" : time);
        return s.split("\\|", -1);
    }
}
