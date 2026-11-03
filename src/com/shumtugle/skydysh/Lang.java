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
                + "||Three brums are after it too.|Get there first.||Up a fire escape,|then over the roofs.||Reward: {reward}.||Touch to go");
        EN.put("brief.again", "Dispatch \u2014 job {n}||Replay job.||Find the folder on the roofs|before the others.||Reward: {reward}.||Touch to go");
        EN.put("report.folder", "Dispatch \u2014 job {n}||The folder is ours.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("report.lost", "Dispatch \u2014 job {n}||Another brum took the folder.||Touch to go on");
        EN.put("lost.short", "TOO LATE");
        EN.put("brief.delivery", "Dispatch \u2014 job {n}||Carry four crates|from the factory to the warehouse."
                + "||Take a duty pickup.|The factory is west of home,|the warehouse east.||At each gate stop in the bay|and wait for the crates."
                + "|Then home.||Pay: {reward}.||Touch to go");
        EN.put("report.delivery", "Dispatch \u2014 job {n}||Four crates in the warehouse.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pickup", "PICKUP");
        EN.put("goal.factory", "FACTORY");
        EN.put("goal.store", "WAREHOUSE");
        EN.put("goal.home", "HOME");
        EN.put("goal.free", "HOME, FOR A JOB");
        EN.put("controls.one", "ONE FINGER");
        EN.put("controls.two", "TWO THUMBS");
        EN.put("home", "Dispatch \u2014 home||Which job now?||1  The hunt|2  The folder|3  The delivery|4  Call out||Or on: job {n}");
        EN.put("sofa", "Home. Progress saved.|Which job now?||1  The hunt|2  The folder|3  The delivery|4  Call out|On: job {n}|Just rest");
        EN.put("sofa.again", "Home. Progress saved.|Which job now?||1  The hunt|2  The folder|3  The delivery|4  Call out|Again: job {n}|Just rest");
        EN.put("goal.done", "DONE. HOME");
        EN.put("story.1", "#Burning plates|=What happened|Every brum has a plate on the brow. It holds the stone together as one body and lets it move. The true plates are made by the town workshop: each has a number, and lasts longer than a house stands.||Forgeries are cast somewhere in cellars. They are cheaper, and at first they work as well. Then they overheat. A brum with a burning plate does not hear the street: it runs blind and cannot stop by itself.||Knocking the plate off is the one way to stop it without real harm. The stones are gathered and taken to the workshop, where the brum is put together again and given a true plate. Where the forgeries come from, the service does not know yet.||=The job|Five brums, five minutes. Catch each one and knock its plate off with the mallet: touch the screen while a runaway is ahead, and the mallet flies by itself. Run right up to one, and it flies without a touch.||Hold a finger: run. Aside: turn. Up: jump; down: slide.||A burning brum shows from afar: it glows. The top of the screen says how far the nearest one is.");
        EN.put("story.2", "#A folder on the roofs|=What happened|The town office sends its papers by helicopter: faster than through the traffic. Now and then, over the roofs, the wind tears a folder out of the net, and it is left lying somewhere among the chimneys.||The folders hold plain papers: contracts, estimates, plans. Without them the town stands still: a building does not start, a street does not open. So the office pays whoever brings a folder back first, by the old rule of the finder.||Everyone knows the rule. As soon as the helicopter circles over a place, the brum seekers come to it: they live on what they find. Not enemies, rivals. But the roofs are tight, and a mallet is an argument too.||=The job|Find the folder before the three seekers. The helicopter circles over the place, the green mark shows the way; at the top, how far the folder is.||Up a fire escape, then over the roofs. Finger up: jump; up again in the air: higher; hold it: glide.||The seekers are violet; at the top left, how far each has still to go. A seeker can be broken with the mallet: it is out, and its stones go to the workshop.||The reward is paid once, for the first folder. After that, only the time counts.");
        EN.put("story.3", "#Glass for the lamps|=What happened|The factory with the chimney, west of home, makes the glass for the street lamps. The warehouse to the east hands it out to the town's repair crews.||There are many lamps in the town, and they fall often. Some to the wind and the lorries. Some to the duty pickup that does not break. The town remembers, and does not say it aloud.||The crates are heavy, and glass does not like to be hurried. So it goes by pickup, which stands at the gate while it is loaded.||=The job|Take a duty pickup: stand by it a moment, and you are in. Drive to the factory, stop in the yellow bay at its gate and wait for four crates. Then to the warehouse, into its bay, till they are off. Then home.||At the wheel: a finger on the glass is the throttle, aside is the wheel, down is the brake, and standing still, reverse. Lift the finger, and the pickup stops.||Paid once, for the first delivery. After that, only the time counts.");
        EN.put("story.4", "#The pass|=What happened|Beyond the ridge there is a village. One road goes there: north up the street by home, out of town and through the pass. Bread and mail go along it, and the doctor's van.||After the rains the slope over the pass slides. This time pines came down with the rocks: the road is shut from wall to wall.||This is what Freedie was put together for: clearing what has fallen, lifting what no one else can. The pickup does not break, and the mallet moves a rock a winch would not.||=The job|Take a duty pickup and drive north, out of town. Past the town, the pickup keeps to the road's bends by itself.||Ram what blocks the pass with the pickup, or get out and knock it aside with the mallet. Then on to the village square.||There is no losing it: only the time counts. Paid once, for the first call out.");
        EN.put("story.back", "Touch to go back");
        EN.put("pause.story", "About the job");
        EN.put("paid.before", "Already paid.");
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
        EN.put("kind.4", "Call out");
        EN.put("brief.rescue", "Dispatch \u2014 job {n}||The road to the village over the hills|is blocked in the pass:|rocks, fallen trunks."
                + "||Take a duty pickup.|The road goes north,|up the street by home and out of town."
                + "||Clear the way \u2014|with the pickup or the mallet.|Get to the village square.||Pay: {reward}.||Touch to go");
        EN.put("brief.rescue.again", "Dispatch \u2014 job {n}||Replay job.||Clear the pass|and get to the village.||Pay: {reward}.||Touch to go");
        EN.put("report.rescue", "Dispatch \u2014 job {n}||The pass is clear.|The road to the village is open.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pass", "PASS");
        EN.put("goal.village", "VILLAGE");
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
