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
                + "||Three brums are after it too.|Get there first.||Up a fire escape,|then over the roofs.||Touch to go");
        EN.put("brief.again", "Dispatch \u2014 {kind}||Replay job.||Find the folder on the roofs|before the others.||Touch to go");
        EN.put("report.folder", "Dispatch \u2014 {kind}||The folder is at the town hall, in good hands."
                + "||Time: {time}||Touch to go on");
        EN.put("delivery.car", "Dispatch \u2014 {kind}||You are at the wheel.|A thumb on the glass is the wheel:"
                + "|aside it turns, up is the throttle,|down the brake.|Stopped, hold it down,|and the pickup backs.||The button on the right gets you out.||Touch to go on");
        EN.put("delivery.factory", "Dispatch \u2014 {kind}||The factory is near.|Put the pickup where it is loaded,"
                + "|in the yellow bay at the gate,|and stand: the crates go on the bed.||Touch to go on");
        EN.put("delivery.store", "Dispatch \u2014 {kind}||The warehouse is near.|Put the pickup where it is unloaded,"
                + "|in the bay at the gate,|and stand: the crates come off.||Touch to go on");
        EN.put("heli.sticks", "Dispatch||You are at the controls.|Two rings, under your thumbs.||Left: up to climb, down to sink,"
                + "|aside to turn.|Right: up to go ahead, down to back,|aside to slide.||Let go and it hovers.||Touch to go on");
        EN.put("heli.left", "UP / DOWN|TURN");
        EN.put("heli.right", "AHEAD / BACK|ASIDE");
        EN.put("delivery.done", "Dispatch \u2014 {kind}||The job is done.|The crates are at the warehouse.||Head home.||Touch to go on");
        EN.put("puppy.done", "Dispatch \u2014 {kind}||The job is done.|The puppy is with the vets.||Head home.||Touch to go on");
        EN.put("folder.done", "Dispatch \u2014 {kind}||The job is done.|What was lost is back at the town hall."
                + "||Head home.||Touch to go on");
        EN.put("folder.safe", "Dispatch \u2014 {kind}||The folder is yours. It is safe now.||Jump down off the roof"
                + "|and take the papers back to the town hall,|to the secretariat: they are waiting.||Touch to go on");
        EN.put("folder.hand", "Found it! Thank you.|It goes back to work at once.");
        EN.put("report.lost", "Dispatch \u2014 {kind}||Another brum took the folder.||Touch to go on");
        EN.put("lost.short", "TOO LATE");
        EN.put("brief.delivery", "Dispatch \u2014 {kind}||Carry four crates|from the factory to the warehouse."
                + "||Take a duty pickup.|The factory is west of home,|the warehouse east.||At each gate stop in the bay|and wait for the crates."
                + "|Then home.||Touch to go");
        EN.put("report.delivery", "Dispatch \u2014 {kind}||Four crates in the warehouse.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pickup", "PICKUP");
        EN.put("goal.factory", "FACTORY");
        EN.put("goal.store", "WAREHOUSE");
        EN.put("goal.home", "HOME");
        EN.put("goal.free", "HOME, FOR A JOB");
        EN.put("controls.one", "ONE FINGER");
        EN.put("controls.two", "TWO THUMBS");
        EN.put("goal.done", "GO BACK HOME");
        EN.put("kind.7", "Lights in the hall");
        EN.put("goal.concert", "CONCERT HALL");
        EN.put("goal.wings", "TO THE WINGS, THE PANEL");
        EN.put("goal.chair", "TO YOUR CHAIR");
        EN.put("sofa.next", "Home. Progress saved.|The next job:");
        EN.put("home.next", "Dispatch \u2014 home||The next job:");
        EN.put("sofa.list", "Home. Progress saved.|Your jobs:");
        EN.put("home.list", "Dispatch \u2014 home||Your jobs:");
        EN.put("home.sum", "Likes: {sum}");
        EN.put("list", "Your jobs:");
        EN.put("list.stories", "Stories done");
        EN.put("list.back", "Back to the list");
        EN.put("stories.head", "Dispatch \u2014 home||Stories done:");
        EN.put("kind.panel", "Panel");
        EN.put("kind.8", "Documents to the archive");
        EN.put("brief.archive", "Dispatch \u2014 {kind}||A call from the town hall.|The town hall is north of home,|on the main street.||At the secretariat take the folder|for the archive.|Then up in the lift to the roof:|a helicopter is waiting.||Touch to go");
        EN.put("report.archive", "Dispatch \u2014 {kind}||The folder is in the archive.|The helicopter stands in its garden|on a broken skid.||Time: {time}||Touch to go on");
        EN.put("goal.townhall", "TOWN HALL");
        EN.put("goal.desk", "SECRETARIAT");
        EN.put("archive.hand", "Papers for the archive.|Carefully. The helicopter|is waiting on the roof.");
        EN.put("goal.lift", "TO THE LIFT");
        EN.put("goal.copter", "TO THE HELICOPTER");
        EN.put("goal.archive", "ARCHIVE");
        EN.put("goal.duty", "DUTY DESK");
        EN.put("goal.light", "LIGHTS");
        EN.put("goal.screwdriver", "SCREWDRIVER");
        EN.put("goal.rope", "ROPE FROM THE BED");
        EN.put("goal.tie", "TIE THE ROPE ON");
        EN.put("fault.plate", "LIGHTS");
        EN.put("fault.works", "WORKING");
        EN.put("story.8", "#Documents to the archive|=What happened|The town hall stands on the main street, north of home: pale stone, a clock over the door, a flag on the roof. The town's archive is far off, in the north-east, at the very end of the town: a low grey house with a garden behind it.||The secretariat has made up a folder of documents, and the archive wants it today. Across the whole town is a long drive, so the town's helicopter waits on the town hall's roof. Its pilot is a brum of black brick.|=The job|Go into the town hall by its middle door. The hall is busy. The secretariat is on the left, through a door in a gilt frame: a clerk waits just inside it with a red folder in her hands. Go up to her and take it.||Across the hall to the lift, on the right. Go up to the roof and get into the helicopter beside the pilot; he does the rest: in an arc over the town, across the whole of it.||Behind the archive is a garden, and in the garden a round pad. When you are down, take the folder to the archive's back door.");
        EN.put("kind.9", "Skid");
        EN.put("brief.mend", "Dispatch \u2014 {kind}||Nothing takes off on one skid.||There is a pickup by the archive.|At the brums' workshop take|a new skid.|Bring it and put it on.||Then take the controls:|the helicopter goes to the town hall's roof.||Touch to begin");
        EN.put("report.mend", "Dispatch \u2014 {kind}||The helicopter is on the town hall's roof.|It is yours now.||Time: {time}||Touch to go on");
        EN.put("goal.skid", "SKID IN THE BED");
        EN.put("goal.mending", "MENDING");
        EN.put("goal.controls", "TAKE THE CONTROLS");
        EN.put("story.9", "#Skid|=What happened|The helicopter came down in the archive's garden and its right skid gave way. It stands leaning; the pilot, the black brum, waits in the cabin. Nothing takes off on one skid.||There is a new skid at the brums' workshop, by the road out of town.|=The job|A pickup stands at the curb by the archive. Drive it to the workshop and stop on the yellow lines before its gate: the skid is put in the bed.||Drive back to the archive and get out by the pickup. You carry the skid yourself. Come to the helicopter's right side, to its door, and put it on.||Then take the controls. The pilot sits beside you.|=Flying|Two sticks. The left: up to climb, down to come down, aside to turn. The right: up to go ahead, down to go back, aside to go sideways. Let go of the left and it holds its height.||Fly to the town hall's roof, to the pad with the H. The town hall is north of home, on the main street.||After that the helicopter is yours: come to its door whenever you like and get in. It comes down anywhere level, the street or a roof. Not on the water.");
        EN.put("kind.10", "Containers");
        EN.put("brief.port", "Dispatch \u2014 {kind}||In the port, by the store,|the containers stand anyhow.||Climb up to the crane|and sort them by colour.||Touch to begin");
        EN.put("report.port", "Dispatch \u2014 {kind}||The containers are in their places.||Time: {time}||Touch to go on");
        EN.put("goal.crane", "TO THE CRANE");
        EN.put("crane.done", "DONE");
        EN.put("story.10", "#Containers|=What happened|A ship was unloaded in the port, by the store, and the containers were put down anyhow: every colour mixed in the stacks.|=The job|The port is on the waterfront, past the store. The yard with the containers is behind a fence, the crane over it. By the fence stands a tower with a ladder: climb it and you are in the cabin.||Sort the containers so that every stack is four of one colour.|=How to play|Touch a stack and the crane takes its top container. Touch another and it puts it there. It goes on an empty bay or on a container of its own colour, no more than four high.||The arrow left takes a move back. The arrow down gets you out of the cabin. Once all is in place, the box with the arrow brings new ones, one colour more.||After that the containers are in the list at home: straight into the cabin, no way to go. Or come to the port and climb the crane whenever you like.");
        EN.put("kind.11", "Navigator");
        EN.put("brief.lab", "Dispatch \u2014 {kind}||LAB Inc. up on the mountain|is trying out navigators for pickups.|One more tester is wanted.||Drive up to the laboratory;|they give out the device inside.||Touch to begin");
        EN.put("report.lab", "Dispatch \u2014 {kind}||The trial is through.|The navigator stays with you.||Time: {time}||Touch to go on");
        EN.put("goal.lab", "LAB INC.");
        EN.put("lab.parking", "PARKING");
        EN.put("lab.counter", "COUNTER");
        EN.put("lab.car", "PICKUP");
        EN.put("lab.leave", "Leave the pickup here|and go in at LAB Inc.");
        EN.put("lab.carry", "The navigator is in the box.|Take it to the pickup.");
        EN.put("lab.fit", "LAB Inc.: the device is in.|Five minutes along the line,|and it is yours.");
        EN.put("nav.quarry", "QUARRY");
        EN.put("nav.clinic", "VET CLINIC");
        EN.put("nav.concert", "CONCERT HALL");
        EN.put("nav.oldtown", "OLD TOWN");
        EN.put("nav.archive", "ARCHIVE");
        EN.put("nav.townhall", "TOWN HALL");
        EN.put("nav.workshop", "WORKSHOP");
        EN.put("nav.factory", "FACTORY");
        EN.put("story.11", "#Navigator|=What happened|Up on the mountain north-west of the town stands LAB Inc. They have made a navigator for pickups: a map in the corner of the screen, a line on it the way to go. Brums are already driving round the town trying it out.|=The job|Drive up to the laboratory. The pickup is by the house. From the town's last street, under the arch, the road goes round the mountain, twice round. At the top there is a car park: leave the pickup in a free space and go in at the tower's door. At the counter in the lobby they give you a box with the navigator in it. Take it to the pickup and unpack it: the navigator goes in by the wheel and comes on.||Then five minutes along the line. Place after place: the quarry, the vet clinic, the concert hall, the old town and on. The time on the board runs only while you drive along the line toward the place; stopped, or off the line, it waits.|=The navigator|The blue line is the way. Under the map, the next turn and how far. Off the line, it flashes and finds a new way. An arrow down: turn round. Orange dots: the other testers.||Touch the map to fold it to a square; touch again to open it.||After the trial the navigator is yours: on foot, in the pickup, in the helicopter.");
        EN.put("kind.12", "Hotheads");
        EN.put("brief.herd", "Dispatch \u2014 {kind}||At night LAB Inc. tried its collider.|A black hole was born; its beam burnt|the firmware of the night shift of brums.||Morning. Fifteen brums in the town,|by threes. They follow the brum that works:|you. Once one touches you, it goes off.||Gather all five|and lead them to the island.||Touch to begin");
        EN.put("herd.first", "Dispatch \u2014 {kind}||The first knot is after you.|Keep going.||The next is where|the navigator's line leads.||Touch to go on");
        EN.put("herd.came", "One more knot. {knots} of 5 after you.");
        EN.put("herd.lost", "A knot fell behind and went back.|{knots} of 5 after you.");
        EN.put("herd.all", "Dispatch \u2014 {kind}||All five knots are after you.||Lead them out of the town:|to the beach, along the spit|to the island, to the pier.||Touch to go on");
        EN.put("herd.blast", "Dispatch \u2014 {kind}||A brum caught you and went off.||Replay from the last knot.||Touch to go on");
        EN.put("herd.pier", "PIER");
        EN.put("herd.yacht", "Dispatch \u2014 {kind}||All fifteen are on the island.||A yacht is at the pier.|Go aboard and leave by sea.||Touch to go on");
        EN.put("herd.wreck", "Dispatch \u2014 {kind}||The yachts are gone.|No way off by sea.||New plan: up to the lighthouse.||Touch to go on");
        EN.put("herd.off", "Back to the line! The brums heat up: {s}");
        EN.put("herd.hot", "Dispatch \u2014 {kind}||You led them off the navigator's line.|The brums overheated and went off.||Replay from the last knot.||Touch to go on");
        EN.put("herd.again", "Dispatch \u2014 {kind}||A brum caught you and went off.||Replay from the pier.||Touch to go on");
        EN.put("herd.boat", "YACHT");
        EN.put("herd.light", "LIGHTHOUSE");
        EN.put("herd.tower", "Dispatch \u2014 {kind}||You are on the lighthouse. They will not come up:|the fallback only walks.||Look at the town. Over the door of home,|the secret balcony. From here you can|reach it: leap off the gallery and glide.||Behind the balcony, the study; in it a switch.|It raises the bridge in the spit|and shuts the island.||Touch to go on");
        EN.put("herd.balcony", "BALCONY");
        EN.put("herd.lever", "SWITCH");
        EN.put("herd.park", "PARK");
        EN.put("report.herd", "Dispatch \u2014 {kind}||The bridge is up. The island is shut.|Fifteen brums wait there to be mended.||Time: {time}||Touch to go on");
        EN.put("story.12", "#Hotheads|=What happened|At night up on the mountain LAB Inc. was trying its collider. In the chamber a black hole was born, the size of a cherry; from its pole a thin violet beam struck, straight through the night shift of brums on their chargers. Their firmware burnt out: fifteen brums glowing like coal, ticking in their chests. Of all their programs one was left, a fallback: follow the nearest brum that works.||In the morning they came down into the town and stood by threes where they were once set up. One took a golden mallet from the workshop.||The one brum in the town that works is Freedie. Once they see her, they come after her, the same way she went. Once one touches her, its burnt firmware gives way and it goes off. They can be mended only at LAB, once the hole is tamed. Till then: lead them where nobody is, to the island.|=The job|Go up to each knot so that it sees you and follows. A knot counts once it is after you. Then lead them all out of the town: to the beach, along the spit to the island, to the pier.||Keep going: the brums run a little slower than Freedie. Get too far ahead (in a pickup, say) and a knot loses you and goes back to its place.||Caught: a blast, and a replay from the last knot.|=On the island|The brums follow you there too. Caught once past the pier: a replay from the pier. The lighthouse is a refuge: they will not go in.||The fallback only walks: up ladders, over roofs, into houses and lifts the brums do not go; they wait below, at the door.|=The navigator|The line on the map leads to the nearest knot not yet after you: along the streets, turn after turn. Under the map, the next turn and how far. Orange dots: the brums after you.||The map folded to a square: touch it to open it.|=The board|Top right: the time, and how many brums are after you.");
        EN.put("kind.13", "Black Cherry");
        EN.put("kind.14", "Traffic Lights");
        EN.put("brief.fault", "Dispatch \u2014 {kind}||The brum from the quarry asks you|to take his job: his pickup is in|for mending, and in the town|a set of lights has gone wrong,|blinking amber on every post.||Find the crossing. On a post there|is a grey box. Take the screwdriver,|open it and join the wires.||Touch to go");
        EN.put("report.fault", "Dispatch \u2014 {kind}||The lights work again.|The brum says thank you.||Time: {time}||Touch to go on");
        EN.put("story.14", "#Traffic Lights|=What happened|The brum Freedie pulled out of the quarry was on his way to a job: a set of lights in the town has gone wrong. His pickup is in the workshop now, and he asks Freedie to take the job on.||Lights gone wrong blink amber on every post at once. The cars go through the crossing slowly, and people cross as at a zebra with no lights.|=The job|Find the crossing: the mark shows it. On the post on the right of the cars going north there is a grey box.||It is opened with the screwdriver. Over the box is a button, what to take to it. Press it and hold: a ring opens round it. Slide your finger to the screwdriver and let go. Let go in the middle, and nothing changes.||With the screwdriver in hand, go right up to the box: inside are the wires of the three lamps, red, amber and green. Join each pair of ends with a wire of its colour, drawing it with your finger from socket to socket. No two wires cross. Once all three pairs are joined, the plate says it works.");
        EN.put("brief.cherry", "Dispatch \u2014 {kind}||LAB Inc. needs help. The black hole|is still in its trap, but the collider's|ring is out of true: the hole spins,|its beam sweeps the hall.||Take the lift down from the LAB lobby|to the collider hall. Knock the loose|magnets back with the mallet.|Keep out of the beam.||Touch to go");
        EN.put("cherry.hall", "Dispatch \u2014 {kind}||The collider hall.||Six magnets are off their stands,|blinking red. Knock each one|back with the mallet.||Touch to go on");
        EN.put("cherry.set", "Magnet in place. Left: {left}");
        EN.put("cherry.burn", "The beam! Firmware heating: {p}%");
        EN.put("cherry.hot", "Dispatch \u2014 {kind}||The beam burnt your firmware.|Overheated.||Replay from the lift.|The magnets set stay set.||Touch to go on");
        EN.put("cherry.lift", "LIFT");
        EN.put("cherry.magnet", "MAGNET");
        EN.put("report.cherry", "Dispatch \u2014 {kind}||The ring holds the hole. The beam is out.||The brums on the island are on their way|to LAB, to the chargers. The bridge|on the spit is down, the island open.||Time: {time}||Touch to go on");
        EN.put("story.13", "#Black Cherry|=What happened|After that night the black hole stayed in its trap over the detector, the size of a cherry. But the ring of magnets that keeps it quiet is out of true: six magnets were torn off their stands. The hole spins, and the beam from its pole goes round the hall like a lighthouse's."
                + "||The LAB's brums cannot go down there: the beam burns firmware. Freedie is tougher; she can, if she keeps out of the beam."
                + "|=The job|Go up to LAB Inc. on the mountain. In the lobby, a lift down to the collider hall. The loose magnets blink red: go to each and strike it with the mallet (touch the screen); it goes back in place and lights green. With each one set the beam goes round slower."
                + "|=The beam|The beam runs from the hole down to the floor. By the trap it is high over your head; further out it comes down, and near the floor it can be leapt. In between, only get out of its way."
                + "||Caught in the beam, your firmware heats; away from it, it cools. Three times close together, and you overheat: replay from the lift. The magnets set stay in place."
                + "|=After|All six in place, the ring takes the hole. Then the fifteen brums on the island can be mended: the bridge on the spit comes down, the island opens.");
        EN.put("report.panel", "Panel||The panel is put right.||Time: {time}||Touch to go on");
        EN.put("panel.plate", "LIGHTING");
        EN.put("brief.concert", "Dispatch \u2014 {kind}||A technical call:|the concert hall, north of the old town.|The hall is full, and there is no light:|not one lamp burns.||The panel is in the wings.|Mend its wiring, throw the switch.|Then take your chair.||Touch to go");
        EN.put("report.concert", "Dispatch \u2014 {kind}||Morning.|Last night the hall had its lights,|and the concert went ahead.||Time: {time}||Touch to go on");
        EN.put("story.7", "#Lights in the hall|=What happened|The concert hall stands north of the old town: columns, broad steps, a gilt lyre over the doors. In the evenings a pianist plays there, and every time something new: he sits down at the piano and makes up a piece there and then, out of old tunes he remembers.||Tonight the hall is full, and the lights have not come on: not one lamp burns. The people sit in the dark, the pianist waits at the piano. The keeper has called dispatch: in the panel in the wings the wiring has come apart.||=The job|Get to the concert hall and go in by the middle doors. The door to the wings is on the right, by the stage.||In the dark a button with a torch on it comes up by Freedie: touch it. Its light goes where you look.||In the panel the wires run through squares. Touch a square, and it turns. The power comes from the feed in the middle; what it reaches glows. The three lamps are not all of it: the lights come on when the power reaches every square. Then no wire points into the frame or into nothing. While the lamps burn and the panel is not yet whole, the dead squares blink.||Then the switch goes green. Throw it.||With the lights on, go to your chair: in the back row, at its end by the left wall, and made to your size. Sit down, and the pianist begins.||After that the hall is yours to come back to any time, no call needed: the chair stays yours.");
        EN.put("kind.6", "Tow");
        EN.put("goal.quarry", "QUARRY");
        EN.put("goal.workshop", "WORKSHOP");
        EN.put("brief.tow", "Dispatch \u2014 {kind}||Past the pass, east of the road|to the village, a brum's pickup|has gone down into the quarry|and cannot get out."
                + "||There is a rope in the pickup's bed.|Tie it to the brum's pickup.|Tow it to the brums' workshop|by the town's last street,|by the road out.||Touch to go");
        EN.put("report.tow", "Dispatch \u2014 {kind}||The pickup is at the workshop, being mended.|The brum is left without wheels.||Time: {time}||Touch to go on");
        EN.put("story.6", "#A pickup in the quarry|=What happened|Past the pass, in the fields east of the road to the village, there is an old quarry: gravel at the bottom, steps of rock on three sides. A brum from the workshop drove out there for stone, went down into it, and the pickup sank in the mud to its axles.||It cannot get out by itself: the wheels only dig deeper. There is no one else to call, so the service calls Freedie.||The brums' workshop is at the town's last street, by the road out: a dark brick house, two gates and a turquoise plate over them. Brums are put together there, and their things mended.||=The job|The quarry is past the pass, east of the road to the village, in the fields. If you are not at the pass, take a duty pickup and drive north, out of town and through the pass.||Drive up to the brum's pickup, stop by it and get out. Over the brum's pickup is a button, what to take to it. Press and hold, slide to the rope and let go: Freedie goes to her own pickup's bed and takes the rope. Go to the front or back of the brum's pickup and touch the screen: the rope is tied.||Get back behind the wheel.||Now drive: the brum's pickup follows on the rope. Bring it to the workshop and stop in the yellow bay on the road before the gates.");
        EN.put("kind.5", "Puppy at sea");
        EN.put("goal.puppy", "PUPPY");
        EN.put("goal.clinic", "VETS'");
        EN.put("brief.puppy", "Dispatch \u2014 {kind}||A call from the beach east of home,|past the warehouse: a puppy at sea,|with no strength to swim back.|People are gathered at the water.||Take a duty pickup.|Swim out to the puppy|and take it to the vets':|three streets up from the sea,|a block east of home.||Touch to go");
        EN.put("brief.puppy.again", "Dispatch \u2014 {kind}||Replay job.||The puppy at sea, east of home.|Swim out, take it to the vets'.||Touch to go");
        EN.put("report.puppy", "Dispatch \u2014 {kind}||The puppy is at the vets'.|Wet, but whole.||Time: {time}||Touch to go on");
        EN.put("story.5", "#A puppy at sea|=What happened|On the beach east of home, past the warehouse, people saw a puppy in the water. It was carried out from the shore, and it can no longer swim back: it keeps afloat, but cannot make the shore.||People gathered at the water's edge. Someone called the town service. The service calls Freedie: a brum is heavier than water, but swims, and a cold sea is nothing to her.||The vets' is three streets up from the sea, a block east of home: a white house with a turquoise band and a paw over the door.||=The job|Take a duty pickup and drive east along the shore road.||Down from the promenade to the beach by the steps, or with a jump. Into the water: where it is deep, Freedie swims. Hold a finger: she swims the way she looks. Swim right up to the puppy, and she takes it.||Back to the shore, to the pickup, and take the puppy to the vets': stop in the yellow bay before the door. Freedie carries it in herself. Then the way home is yours.");
        EN.put("story.1", "#Burning plates|=What happened|Every brum has a plate on the brow. It holds the stone together as one body and lets it move. The true plates are made by the town workshop: each has a number, and lasts longer than a house stands.||Forgeries are cast somewhere in cellars. They are cheaper, and at first they work as well. Then they overheat. A brum with a burning plate does not hear the street: it runs blind and cannot stop by itself.||Knocking the plate off is the one way to stop it without real harm. The stones are gathered and taken to the workshop, where the brum is put together again and given a true plate. Where the forgeries come from, the service does not know yet.||=The job|Five brums, five minutes. Catch each one and knock its plate off with the mallet: touch the screen while a runaway is ahead, and the mallet flies by itself. Run right up to one, and it flies without a touch.||Hold a finger: run. Aside: turn. Up: jump; down: slide.||A burning brum shows from afar: it glows. The top of the screen says how far the nearest one is.");
        EN.put("story.2", "#A folder on the roofs|=What happened|The town office sends its papers by helicopter: faster than through the traffic. Now and then, over the roofs, the wind tears a folder out of the net, and it is left lying somewhere among the chimneys.||The folders hold plain papers: contracts, estimates, plans. Without them the town stands still: a building does not start, a street does not open. So the office pays whoever brings a folder back first, by the old rule of the finder.||Everyone knows the rule. As soon as the helicopter circles over a place, the brum seekers come to it: they live on what they find. Not enemies, rivals. But the roofs are tight, and a mallet is an argument too.||=The job|Find the folder before the three seekers. The helicopter circles over the place, the green mark shows the way; at the top, how far the folder is.||Up a fire escape, then over the roofs. Finger up: jump; up again in the air: higher; hold it: glide.||The seekers are violet; at the top left, how far each has still to go. A seeker can be broken with the mallet: it is out, and its stones go to the workshop.");
        EN.put("story.3", "#Glass for the lamps|=What happened|The factory with the chimney, west of home, makes the glass for the street lamps. The warehouse to the east hands it out to the town's repair crews.||There are many lamps in the town, and they fall often. Some to the wind and the lorries. Some to the duty pickup that does not break.||The crates are heavy, and glass does not like to be hurried. So it goes by pickup, which stands at the gate while it is loaded.||=The job|Take a duty pickup: stand by it a moment, and you are in. Drive to the factory, stop in the yellow bay at its gate and wait for four crates. Then to the warehouse, into its bay, till they are off. Then home.||At the wheel: the steering wheel under the thumb. Aside it turns, up is the throttle, down the brake; hold it down standing still, and the pickup backs. Let go, and it rolls to a stop.");
        EN.put("story.4", "#The pass|=What happened|Beyond the ridge there is a village. One road goes there: north up the street by home, out of town and through the pass. Bread and mail go along it, and the doctor's van.||After the rains the slope over the pass slides. Logs, stumps, stones and clay come down onto the road, and wedge fast between the rocks.||This is what Freedie was put together for: opening roads where no one else can.||=The job|Take a duty pickup and drive north, out of town. Past the town, the pickup keeps to the road's bends by itself.||The heap stands like a wall: the pickup will not get through it, and the mallet and her own weight will not stir it. It takes a charge out of the pickup's bed.||Get out: over the heap there is a button, what to take to it. Hold it down, slide the finger to the charge, let go: Freedie goes to the bed and takes a charge. The ladder and the rope are in the bed too, but they will not open the heap. Go up to the heap and stand by it a moment: the charge is set, its fuse lit. Stand clear: in a few seconds the blast throws the heap apart. Once there is a way through as wide as the pickup, the road is open.");
        EN.put("story.back", "Touch to go back");
        EN.put("pause.story", "About the job");
        EN.put("pause.skip", "Put the job by");
        EN.put("pause.leave", "Leave the replay");
        EN.put("pause.point", "From the last point");
        EN.put("report.leave", "Dispatch \u2014 {kind}||Replay left.|Everything stays as it was.||Touch to go on");
        EN.put("rescue.open", "The road is open.");
        EN.put("report.skip", "Dispatch \u2014 {kind}||The job is put by.|It waits on home's list,|not done.||Touch to go on");
        EN.put("list.undone", "{job} \u2014 not done");
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
                + "||Open a way through the pass.||Touch to go");
        EN.put("brief.rescue.again", "Dispatch \u2014 {kind}||Replay job.||Open a way through the pass.||Touch to go");
        EN.put("report.rescue", "Dispatch \u2014 {kind}||The pass is open.|The road to the village is clear.||Time: {time}"
                + "||Touch to go on");
        EN.put("goal.pass", "PASS");
        EN.put("goal.village", "VILLAGE");
        EN.put("goal.charge", "CHARGE FROM THE BED");
        EN.put("goal.heap", "THE CHARGE TO THE HEAP");
        EN.put("goal.away", "STAND CLEAR");
        EN.put("rescue.heap", "Dispatch \u2014 {kind}||The heap will not be driven through|or cleared by hand: it stands like a wall."
                + "||Get out of the pickup. Over the heap|there is a button: hold it down,|slide the finger to the charge, let go:|Freedie takes a charge from the bed."
                + "||Set it at the heap and stand by it.|Then get well clear.||Touch to go on");
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
