# Skydysh

A game for Android: races through an open town, a southern seaside town on a summer evening
seen as town games were drawn early in the century,
its streets grown from a seed without end as they are come to, cars going about them and
people out on the pavements. For now on foot: a giant of
brick and stone in a maid's dress runs, turns, jumps and slides through it; a car, a boat and
a plane are to come.

It opens, at every start, with a flight down onto a square by the sea, where she stands with a
golden mallet and brings it down into the glass of the screen; a touch skips it. Then the
dispatcher's room, which is her head: walls of her own brick round the street seen through her
eyes, what is to be done typed over it; a touch, her mallet knocks a key, the walls draw back
and the street goes on. When it is done, back into her head for the report.

A finger on the glass makes her run; moved sideways, it turns her by as much as it moved;
a tap, she jumps; two taps quickly, she leaps over a car or up onto what stands a storey up;
flicked up, she glides; flicked down, she slides; lifted, she slows to a stop. She stands on
whatever she comes down on, the roofs too. When a mission is done the game keeps where it is.

For now, to try: a hunt. Five golems on fire that should not be are loose in the town, each
waiting somewhere far off on a pavement of its own; come near one and it runs. Run it down to
within reach, and she stops and throws her mallet; it knocks the plate off its brow, and the
golem goes out and falls apart.
At the top, how far off the nearest still loose is, in meters; in the corners, the five plates
as they are won, and the clock; at the edge of the frame, a burning mark
the way each one out of sight is. When the last is caught, five more are let loose.

Every other mission, the folder: dropped from a helicopter somewhere on the roofs, a reward for
it, and golems after it too, climbing the fronts. Up by a lift in a house near it, then roof to
roof, leaping and gliding; taken, she goes down to the street with it by herself. If a golem
gets there first, the mission is played again.

What is to come is in `PLAN.md`.

## How it is made

- Java 8 and the platform alone; no build system, no libraries.
- The game lives in plain Java: rules, the town, what stands where. Each frame it fills a
  draw list: the eye, the sun, the sky and every box and ball in the scene. The runner's
  figure is blocks riding on the bones of a humanoid skeleton whose motions are in
  `assets/moves.bin` (see `CREDITS`).
- The phone draws that list with OpenGL ES 3: the sun's depth map; the sky and the boxes at a
  few pixels to a frame pixel, each material its own worked surface made in the shader, lit by a
  low sun and the sky with soft shadows; then a finishing pass (a soft bloom, the hour's grade,
  the glow of fire) and onto the screen. The shaders are in `assets/shaders`.
- The desktop stand (`stand/`) plays scenes against the same Java and writes each frame's
  draw list; `stand/web/index.html` draws it with the same shaders in a browser.

## Build

```sh
ANDROID_JAR=... KEYSTORE=... KEY_ALIAS=... KS_PASS=... ./build.sh
```

## License

Zero-Clause BSD, see `LICENSE`.
