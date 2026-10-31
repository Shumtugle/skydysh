# Changelog

## 0.24

- Fixed: a house at a corner of its block backs onto the end of the row across, but it had a
  back door all the same. Out of it, she was put into the gap by that wall, in a yard shut in
  on every side, and the place before the door was inside the other house, out of reach: no
  way back. Such a house has no back door now, outside or in its hall (the hall is walled up
  at the back).
- Out of a door, she stands a couple of steps clear of it, and may walk straight back in.
- With her back to a wall the eye no longer sits inside her: it rises and looks down on her,
  as far as a ceiling lets it.
- Stand: `yardhouse`, `where`, `doorlog S`; scene `yard.txt` (through the hall into the yard and
  straight back in).

## 0.23

- Fixed: a game that began with the folder's mission (the last one having ended with the folder
  lost) stopped at once. The hunt's count of golems caught was asked every frame, before any
  hunt had let its golems loose. It now counts none until there are some.
- Stand: `fuzz S SEED` (a careless finger at random: down, about, up, taps, flicks); scene
  `coldfolder.txt` (a game that begins with the folder, lost, and played again).

## 0.22

- Indoors. The door of a house with a lift (at the front, or at the back on the yard) opens for
  her as before, and now she goes in: a moment of dark, and she is in the house's hall.
- The hall is made from the house's seed, the same house always the same: a floor of tiles in
  two tones, square or on the diagonal; walls in one of several colours, panelling up to a rail;
  lamps down the ceiling; glass doors front and back that slide open as she comes to them, the
  light of the street or the yard beyond; and along the walls, laid out by the seed, a bank of
  letter boxes, a bench, a plant in a clay pot, a radiator, a picture, a notice board, a runner
  of carpet. Through either glass door she is out again.
- The lift is in one side wall, in a frame of brass or steel, a row of lamps over it, a lamp a
  floor. Come near and its button lights by itself; the cabin is called and its doors open. In
  the cabin she turns to the doors, the top floor's button lights, the doors close, the lamps
  count the floors up, and she comes out of the hut on the roof. Into the hut, and she is in the
  cabin going down; the doors open onto the hall.
- While she rides, the eye is held across the hall on the lift's doors; walking, it is close
  behind her, under the ceiling. Indoors the light comes from overhead, pooled in the middle; she
  and what stands about cast their shadows on the floor.
- The eye now backs away from her only as far as nothing is in the way, so it is never caught
  inside a house after a door.
- The mallet is not used indoors. The rooms are places of their own: the town is not drawn while
  she is in one, and the clock and the rivals run on.
- Stand: `onhut`, `walk S E P` (indoors, a finger that knows the way); the folder bot goes in and
  through the hall. Scenes `hall.txt`, `down.txt` (in place of `lift.txt`).

## 0.21

- New controls. A finger on the glass still runs her and turns her. A flick up jumps; a second
  flick up while she is in the air takes her on up as high as a leap would have, over a car or a
  storey up. A flick just after running off an edge is still a jump. She glides by herself once
  she is falling high over what is below; no flick for it any more. A flick down slides.
- A tap is the mallet's. A golem within reach she strikes where she stands; one ahead of the eye
  and within 20 m she stops and throws at, and the mallet flies after its head as before. With
  none, she throws it straight ahead and it comes back empty. Reaching a golem no longer throws
  by itself, and a golem reached is no longer taken by itself after a while.
- Lifts open by themselves: come to the door, or stand at it a moment, and the doors take her.
  Running by along the front, they do not.
- The folder is marked green: a ring round it in sight, a green mark at the edge of the frame
  when it is not, kept clear of the rivals' marks in the corner.
- The rivals set off later (60, 80 and 100 s) and climb slower; with no one after it, the first
  gets to the folder at about two minutes.
- Stand: `swipe X Y`; `nearfolder [METERS [RADIANS]]`; the hunting bot taps for the mallet, the
  folder bot jumps with two flicks. Scene `liftstand.txt`.

## 0.20

- A second kind of mission, every other one: the folder. A folder of papers has fallen out of a
  helicopter's cabin onto the roofs; there is a reward of 100 000 for it. The helicopter circles
  low over where it fell. At the top, how far off it is; at the edge of the frame a red mark the
  way it is; over it, when it is in sight, a red mark coming and going, and it glints.
- Golems are after it too: three, each setting off a while after the start from its own side of
  the town, running the streets the shortest way to the folder's house, climbing its front and
  crossing the roof. In the corner, how far each has still to go (gold once it climbs). The
  mallet takes them down as it takes the five. If one gets there first, the mission is lost; the
  report says so, and the same mission is played again.
- Lifts: one in two of the taller houses has a door in its front and at its back, a green lamp
  with an arrow over each. Run into it: the doors shut, the floors are counted in the dark, and
  she comes out of a hut on the roof. Into the hut's door, and down to the street again. The
  folder's house has none: up a house near it, then roof to roof.
- Taken, the folder goes under her arm and she goes down by herself: to the edge of the roof,
  off it in a leap and down along the street, gliding. In the street, back into her head.
- The room in her head after a mission: the report, a touch, and on the same screen the
  briefing of the next.
- The dispatch screen: small letters and capitals in the manner of an old tube, warm white,
  each row of dots with a dark line under it; each line a little brighter or weaker as the beam
  goes, now and then dimmed a moment, a brighter band passing down the glass. The texts in both
  languages are in ordinary small letters now.
- Autosave keeps the money too, and a lost folder keeps the mission to play again.
- Stand: `mission N`, `nearfolder`, `onfolder M R`, `climbing M`, `folderbot S E P` (a player
  after the folder: streets, lift, roofs), `idle S E P` (the rivals alone), `catchall`, `turn R`.
  Scenes `folder.txt`, `folderlook.txt`, `lost.txt`, `lift.txt`, `next.txt`, `tube.txt`.

## 0.19

- A tap, she jumps; two taps quickly, she leaps, high enough to clear a car or come down on
  what stands a storey up; flicked up, she glides, arms out, sinking slowly till she lands.
- Things are as high as they are: she goes over a car, a basin, a parked car or a person when
  she is above it, and comes down on top of whatever she lands on, the roofs too. Off an edge
  she runs on into the air.
- Lifted for a moment, the finger keeps her running; in the air she does not slow.
- Autosave: when a mission is done the game keeps which one is next and what has been won;
  started again, it goes on from there.
- Stand: `tap X Y`, `carahead M` (a car across her way so far ahead), `roof` (her put on the
  nearest roof), `face R`; `keep KEY VALUE` and `restart` print what is kept and where the game
  starts again. Scenes `leap.txt`, `roofs.txt`, `kept.txt`.

## 0.18

- Russian: the dispatches typed in her head are in Russian when the phone speaks it, or when
  Russian is chosen for the game alone (in the system's settings for the app, on newer phones).
- The pixel font draws the Cyrillic capitals; the ones shaped as Latin letters are drawn with
  those.
- The game's words live in one place, English kept in the code, every other language in a file
  of its own among the assets (`assets/lang/ru.txt`).
- Stand: `lang ru` makes the game speak Russian from then on.

## 0.17

- The dispatcher's room is her head. She is a golem too; where the forged golems have a plate on
  the brow, she has a room inside her head, walled in courses of her own brick and stone, a
  ceiling of planks as her cap is, a floor of slab. In its front wall the screen, her eyes:
  through it the street is seen as she sees it, standing still while she is in there; over it
  her eyes, two lamps, lit. Under it a shelf of planks with keys, the red key, and a clock with
  the time and the date.
- The room stands in the world where her head is, turned as she is. Coming in, its walls close
  round the street from every side until the street is left only on the screen; going out, they
  draw back past the edges of the frame and the eye is behind her again, and the street goes on.
- The dispatch is typed over what she sees, on a dark veil with the lines of a tube.
- At the touch that goes on, her mallet, the same and as big as in the street, comes up from
  below on the right and knocks the red key in.
- The office with the window, the desk and the set is gone.

## 0.16

- A golem run down no longer stops: it runs on, and she stops, turns after it as she swings and
  throws; the mallet catches it up on the run and knocks the plate off. The mallet flies faster.
- Everyone a little quicker, by about a sixth: she runs to 11.5 meters a second, the golems run
  and bolt quicker to match, the cars go at 10.5, the people walk a little brisker.
- In the dispatcher's room the set comes on out of a bright line across its middle, as old sets
  did; the clock shows the date under the time; the red key, now under the set, goes in with a
  click and lights at the touch that goes into the glass.

## 0.15

- The dispatcher's room, made anew in the new look: a room high over the town, the sea in its
  window at evening; on a wooden desk an old television, a panel of keys before it, a clock on
  the set with the real time, papers, a pencil, a mug; a lamp's warm light.
- Before a mission the dispatch is typed out on the glass of the set, ticking as it types: five
  golems broke out of the foundry at sunset with forged plates; forged plates burn, the golems
  burn with them and so will the town; they hide about the streets; find them, run them down,
  knock every plate off. A touch types the rest at once; another, and the eye goes into the
  glass and the mission begins in the street.
- The opening ends in the room, the eye coming out of the white glass of the set.
- When the last of the five is caught, three seconds on the eye comes back out of the glass into
  the room: the set reports the mission done and its time; a touch, and the next five are loose.
- The old flat panel stays in the code, unused.
- Stand: `dispatch` opens the room on the briefing, `dispatch done` on the report.

## 0.14

- An opening, some six and a half seconds, at every start. The eye comes down out of the sky
  over the town by the sea, over the promenade with its palms and parapet, onto a square paved
  in rings, houses round it, roads behind and to the right with cars both ways, people far off.
  It stops before her, a giant two and a half times her height, a golden mallet in her right
  hand; her eyes light to a low blow of metal. The town goes dark, one light left on her, the
  mallet glowing of itself. She swings it back over her head, slowly, then down at the eye,
  faster, the eye stepping up to meet it: it strikes the glass of the screen, with thunder, the
  ground's boom and glass, a white flash and the frame shaking. Eight white cracks run out and
  branch; she falls to pieces, every block flung off away from the blow, turning and knocking on
  the stones, the mallet's handle and burning head falling apart; the eye draws back and down,
  the cracks melt away. The light goes up to the name, lit dot by dot from the left, each dot
  white and then a bar of gold, to a high chime; the frame goes white and the game begins.
  A touch skips to the white.
- The renderer can put the world in the dark with one light left on one place, whatever glows
  of itself glowing on.
- The sea made over: long soft swells, a scatter of light toward the sun.
- Stand: the scenes of the game skip the opening; `opening` plays it, `skip` touches past it.

## 0.13

- A new look: the printed one is gone, no ink lines and no halftone. Faces are lit smoothly by
  a low warm sun and the open sky, shadows soft at their edges; a soft bloom where the frame is
  brightest; the shadows a touch cooler, the lights a touch warmer; the corners a little darker.
- Worked surfaces: the plaster blotched and weathered, darker toward the street; a dressed stone
  base under the shops; windows in pale frames with their bars, sills, slatted shutters in teal,
  blue or coral, a balcony rail under some; asphalt grained and patched, darker in the tracks,
  its paint worn thin; paving and promenade tiles each a shade of their own; the square's setts
  laid in rings; planks with their grain; stone speckled; painted things a little uneven.
- A soft sheen where the sun glances off painted metal and glass.
- The sky smooth from teal to peach, soft streaks of cloud coral underneath, a warm haze round
  the low sun and thick sea haze along the horizon.
- The paper grain is gone; a faint grain stays only so the gradients do not band.
- A new icon: the plank mallet in the round, lit warm, on teal going to peach, the plate it has
  knocked off alight beside it.

## 0.12

- The chase runs to its end again: a golem is to be run down until she is within reach of it.
  Then it is held where it is, startled (or lying, if a car had knocked it down), and she
  stops short, turns to it and throws: a wind-up, and the mallet flies at its head, knocks the
  plate off its brow and comes back to her hand. The finger is not heard while she throws.
- Her mallet: a big head of pale planks bound with two bands of iron, a square handle of
  darker wood, a red-brown cap on its end. The maul is gone.
- The icon redrawn with the mallet.
- Stand: the bot goes round by the streets to a golem that waits until it is down the same
  street, and steps aside to either hand by turns when held up.

## 0.11

- Her hammer is now a stonemason's maul: a long ash handle, a navy band, a block of dark steel
  for a head with its two faces worn bright and a wedge in its top. She carries it hanging from
  her fist, trailing behind her with its head low; thrown, it turns end over end about its
  middle.
- The icon redrawn with the maul.

## 0.10

- The hammer flies from nearer: within 10 meters of a golem instead of 18.

## 0.9

- She carries a hammer in her right hand. When a golem still loose is within 18 meters, before
  her and with no house between, the hammer flies at it by itself, end over end, finding its
  head wherever it has run to, and comes back to her hand.
- Every golem wears a plate on its brow, alight. The hammer knocks it off: the plate flies up
  turning over, then across the frame into its slot in the corner, and the golem falls apart.
  Running into one still pulls its plate off, as before.
- In the corner, five slots for the five plates, dark until each is won, instead of a count.
- A new icon in two layers, the evening street behind and the hammer with the plate it has
  knocked loose in front, so the launcher shapes it as it likes and frames it in nothing.

## 0.8

- The first mission, to try: five golems on fire that should not be are loose in the town,
  each waiting with its arms folded on a pavement of its own, round the start one a way round
  the compass, from 150 to 420 meters off. Come within 50 meters of one and it runs; left far
  behind, it stops and waits again.
- Caught, a golem is done with for good: it goes out where it stands and falls apart, its
  blocks flying off it still burning, going out to a dark crust, lying on the road as cold
  stone, and at last sinking into it.
- Over the frame: how many of the five are caught, the clock, how far off the nearest still
  loose is, and a burning mark at the edge of the frame for every one out of sight, smaller
  the further. The last one caught, the count and the time large in the middle; ten seconds
  on, five more are let loose round wherever she is. The phone buzzes long at the last.
- Only the nearest golem still burning gives off light and glow; the others only burn.
- Cars stop for a golem that stands in the road.
- Stand: the bot goes after the nearest one, along the streets while it is far and on its
  trail once it runs, keeping to its side of the road and stepping aside when held up; scenes
  for the whole hunt and for one falling apart close up.

## 0.7

- The cars no longer stop for the double on fire, and she runs in their lane. Struck by one,
  she is flung the way it was going and off to the side, toward the curb, falls on her back,
  lies a while and gets up again; the car jolts to a stop and stands a moment, and while she
  is down the cars slow for her. Caught lying there, she counts all the same, and gets up to
  break loose. The phone gives a short buzz at the blow.
- A stand scene for it: a car run into her from behind.

## 0.6

- The town comes alive. Cars go about the streets on the right of the road, on at the
  crossings or turning now and then, never back; they slow for a turn, keep back from the car
  ahead and stop short of anyone in the road. They come from out of sight and go out of it.
- Cars parked along the curbs, the way their side of the road goes, never near a crossing;
  drawn lean, and only near.
- People out for the evening on the pavements, each going round a block one way or the
  other; someone running at them makes them stop and step aside. Shirts, trousers, shorts or
  skirts, hats and bags; now and then a child.
- The runner and her double are stopped by cars and people as by houses.
- The double on fire burns through instead of giving off sparks: every block hot at its
  heart and crusted dark at its edges, the dark ones low and red, the light ones gold, the heat
  running up her in waves, the eyes white hot. Her warm light falls on the street, the cars,
  the houses and the people round her, and a glow is printed round her where nothing nearer
  stands, its dots in hot orange.
- Stand scenes for the town and for the double close up.

## 0.5

- A chase, to try: her double on fire runs off through the town ahead of her. She keeps to
  the streets, on the right of the road, and at each crossing goes on or turns as it falls out;
  near, she bolts; far ahead, she lets the chase come up; left far behind, she folds her arms
  and waits. Caught, she is jolted, counted, and breaks loose for a few seconds.
- Over the frame: how far off she is, in meters, at the top; how many times she was caught, in
  the corner; a burning mark at the edge of the frame the way she is, when she is out of sight.
  The phone buzzes at a catch.
- The figure can be drawn on fire: the dark blocks smoulder, the light ones glow from within;
  flames rise off her shoulders.
- The eye a little higher behind the runner, to see over her.
- The dispatcher's panel kept in the code for later, unused.
- A plan of what is to come, in PLAN.md.

## 0.4

- The game turns into races through an open town; the throwing game is gone: the plinth,
  the hammer, the stars and the badge, the panel between rounds.
- The town grows without end from a seed, a cell of 64 m at a time, only where the runner is:
  a grid of streets with their crossings, curbs and pavements, blocks of old seaside houses
  round all four sides, now and then a square with palms and a basin; some streets have
  palms down both pavements; lamp posts along the curbs. Cells out of sight are not drawn, and
  the small things only near.
- Roads are painted in their own frame, so they can run either way.
- The runner: the giant from the earlier game, on her skeleton, running through it. A finger
  down: she runs, up to 10 m/s; moved sideways: she turns by as much as it moved; flicked up:
  she jumps; flicked down: she slides; lifted: she slows to a stop. Houses, lamp posts and
  palms stop her; the curb is a step.
- The eye follows behind her and a little above, turning after her a moment late, kept out
  of the walls.
- The sun higher, from the left and behind, its depth map laid round where she is heading.

## 0.3

- The corner no longer draws itself afresh while it moves: the two stars (yellow and slate) and
  each badge face are drawn once and then only laid down; a spent star fades from the one to
  the other, a jolted badge is the same face squashed.
- The hammer on the panel is drawn once lying flat, with its shade, and turned into place every
  frame instead of being drawn part by part twice a frame.
- On the desktop stand a frame's own work drops from about 12 ms to 1 to 2 while a building
  falls, and from 13 to 19 ms to 2 to 7 while the panel is closed.

## 0.2

- Drawn at the size the phone keeps up with, not at the screen's: the street, its ink and its
  dots are drawn at two pixels to a frame pixel and scaled up smoothly; the aim, the corner and
  the paper grain stay at the screen's own size.
- The drawing size settles by itself: down a step while frames come slow, back up (to three) while
  they come quick.
- The aim and the trail are worked out only near where they are, not over the whole screen.
- A small meter in the lower left corner while the engine is young: how long a frame takes, in
  milliseconds, and the drawing size.

## 0.1

- The first build: the summer street alone, levels 1 to 101, then round again from the 2nd.
  No opening, no mini-games.
- Drawn flat and inked: two-tone light, ink lines where things meet, halftone in the shade,
  paper grain; a sunset sky in flat bands with long bars of cloud; warm asphalt.
- A new hammer, held in profile: a red barrel with navy caps and a band of scallops, a bulb,
  a ribbon; the heavy one in dark stone, the golden one in gold.
- The corner: five stars and the level on a badge; pips for tries lost; the heavy hammers
  under the stars.
- The aim drawn smooth: a white ring with an orange wind-up arc, the finger's own circle, the
  flight in dots; a trail of orange dots behind a thrown hammer.
- The dispatcher's panel between rounds: a clock and the date, the set with the street on
  it, the level in one big number, a key that goes on, a key for the hammer given, a key back,
  a speaker; the hammer knocks the keys, drawn flat on the panel.
- Five lost tries at one building give the golden hammer straight away.
