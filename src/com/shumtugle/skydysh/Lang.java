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
        EN.put("brief.first", "Dispatch \u2014 {kind}||Five brums are out in the streets|with their plates on fire.|Forged plates, overheated."
                + "||Catch each one.|Knock the plate off with the mallet.||Five minutes for the job.||Touch to go");
        EN.put("brief.more", "Dispatch \u2014 {kind}||Five more brums|with overheated plates.||Catch each one."
                + "|Knock the plate off with the mallet.||Five minutes for the job.||Touch to go");
        EN.put("brief.late", "Dispatch \u2014 {kind}||Replay job.||Five brums. Five plates.|Five minutes.||Touch to go");
        EN.put("report.late", "Dispatch \u2014 {kind}||Time is up.|Plates off: {caught} of 5.||Touch to go on");
        EN.put("late.short", "TIME UP");
        EN.put("report", "Dispatch \u2014 {kind}||All five plates are off.|The brums are already being|put back together.||Time: {time}"
                + "||Touch to go on");
        EN.put("brief.folder", "Dispatch \u2014 {kind}||Find the folder on the roofs.|The helicopter circles over the place."
                + "||Three brums are after it too.|Get there first.||Up a fire escape,|then over the roofs.||Reward: {reward}.||Touch to go");
        EN.put("brief.again", "Dispatch \u2014 {kind}||Replay job.||Find the folder on the roofs|before the others.||Reward: {reward}.||Touch to go");
        EN.put("report.folder", "Dispatch \u2014 {kind}||The folder is ours.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("report.lost", "Dispatch \u2014 {kind}||Another brum took the folder.||Touch to go on");
        EN.put("lost.short", "TOO LATE");
        EN.put("brief.delivery", "Dispatch \u2014 {kind}||Carry four crates|from the factory to the warehouse."
                + "||Take a duty pickup.|The factory is west of home,|the warehouse east.||At each gate stop in the bay|and wait for the crates."
                + "|Then home.||Pay: {reward}.||Touch to go");
        EN.put("report.delivery", "Dispatch \u2014 {kind}||Four crates in the warehouse.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pickup", "PICKUP");
        EN.put("goal.factory", "FACTORY");
        EN.put("goal.store", "WAREHOUSE");
        EN.put("goal.home", "HOME");
        EN.put("goal.free", "HOME, FOR A JOB");
        EN.put("controls.one", "ONE FINGER");
        EN.put("controls.two", "TWO THUMBS");
        EN.put("home", "Dispatch \u2014 home||Which job now?||On|Puppy at sea|Call out|The delivery|The folder|The hunt");
        EN.put("sofa", "Home. Progress saved.|Which job now?||On|Puppy at sea|Call out|The delivery|The folder|The hunt");
        EN.put("sofa.again", "Home. Progress saved.|Which job now?||Again|Puppy at sea|Call out|The delivery|The folder|The hunt");
        EN.put("goal.done", "DONE. HOME");
        EN.put("kind.5", "Puppy at sea");
        EN.put("goal.puppy", "PUPPY");
        EN.put("goal.clinic", "VETS'");
        EN.put("puppy.strength", "PUPPY HOLDS OUT");
        EN.put("puppy.late.short", "TOO LATE");
        EN.put("brief.puppy", "Dispatch \u2014 {kind}||A call from the beach east of home,|past the warehouse: a puppy at sea,|with no strength to swim back.|People are gathered at the water.||Take a duty pickup.|Swim out to the puppy|and take it to the vets':|three streets up from the sea,|a block east of home.||Pay: {reward}.||Touch to go");
        EN.put("brief.puppy.again", "Dispatch \u2014 {kind}||Replay job.||The puppy at sea, east of home.|Swim out, take it to the vets'.||Pay: {reward}.||Touch to go");
        EN.put("report.puppy", "Dispatch \u2014 {kind}||The puppy is at the vets'.|Wet, but whole.|Paid: {reward}.|Balance: {sum}.||Time: {time}||Touch to go on");
        EN.put("report.puppy.late", "Dispatch \u2014 {kind}||The puppy held out no longer.|People from the shore got it out.||Touch to go on");
        EN.put("story.5", "#A puppy at sea|=What happened|On the beach east of home, past the warehouse, people saw a puppy in the water. It was carried out from the shore, and it can no longer swim back: it holds on, but its strength is going.||People gathered at the water's edge. Someone called the town service. The service calls Freedie: a brum is heavier than water, but swims, and a cold sea is nothing to her.||The vets' is three streets up from the sea, a block east of home: a white house with a turquoise band and a paw over the door.||=The job|Take a duty pickup and drive east along the shore road. At the top left, how long the puppy still holds out.||Down from the promenade to the beach by the steps, or with a jump. Into the water: where it is deep, Freedie swims. Hold a finger: she swims the way she looks. Swim right up to the puppy, and she takes it.||Back to the shore, to the pickup, and take the puppy to the vets': stop in the yellow bay before the door. Running there will do too, up to the door itself.||Too late, and people from the shore get it out, and the job begins again. Paid once, for the first puppy.");
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
        EN.put("brief.rescue", "Dispatch \u2014 {kind}||The road to the village over the hills|is blocked in the pass:|rocks, fallen trunks."
                + "||Take a duty pickup.|The road goes north,|up the street by home and out of town."
                + "||Clear the way \u2014|with the pickup or the mallet.|Get to the village square.||Pay: {reward}.||Touch to go");
        EN.put("brief.rescue.again", "Dispatch \u2014 {kind}||Replay job.||Clear the pass|and get to the village.||Pay: {reward}.||Touch to go");
        EN.put("report.rescue", "Dispatch \u2014 {kind}||The pass is clear.|The road to the village is open.|Paid: {reward}.|Balance: {sum}.||Time: {time}"
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
        // what job it is by its kind, not its number: the number is the game's count, not the player's
        if (s.contains("{kind}")) s = s.replace("{kind}", lines("kind." + World.kindOf(n), 0, null)[0]);
        s = s.replace("{n}", String.valueOf(n)).replace("{time}", time == null ? "" : time);
        return s.split("\\|", -1);
    }
}
