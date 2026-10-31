# Skydysh

A game for Android: races through an open town, a southern seaside town on a summer evening
seen as town games were drawn early in the century,
its streets grown from a seed without end as they are come to, cars going about them,
people out on the pavements with their dogs, cats by the walls; squares with fountains or
gardens, trees, benches and pots of flowers along the streets. For now on foot: a giant of
brick and stone in a maid's dress runs, turns, jumps and slides through it; a car, a boat and
a plane are to come.

It opens, at every start, with a flight down onto a square by the sea, where she stands with a
golden mallet and brings it down into the glass of the screen; a touch skips it. Then the
dispatcher's room, which is her head: walls of her own brick round the street seen through her
eyes, what is to be done typed over it; a touch, her mallet knocks a key, the walls draw back
and the street goes on. When it is done, back into her head for the report.

A finger on the glass makes her run; moved sideways, it turns her by as much as it moved;
flicked up, she jumps; flicked up again in the air, she goes on up, over a car or onto what
stands a storey up; falling from high enough, she glides down by herself, and gliding, a flick
up beats her arms and lifts her (four times a flight, the wings at the bottom of the glass
telling how many are left); run at a ladder, she takes hold and climbs it hand over hand; flicked down, she
slides; lifted, she slows to a stop. A tap is the mallet's: a golem within reach she strikes,
one ahead and not too far she throws at; with none, she throws it ahead and it comes back. The
door of a house with a lift opens by itself when she comes to it or stands at it: inside is
its hall, made from the house's seed, and in its side wall the lift, which is called, opens,
takes her up and lets her out on the roof by itself. On the roof, by the hut's door, a button
shows; pressed, the lift takes her down and the cabin opens onto the hall again. Signs of glass
hang over the ways in and up near her: the lifts' doors, the fire escapes, the ladders from roof
to roof. A button high on the left pauses the game. She stands on
whatever she comes down on, the roofs too. When a mission is done the game keeps where it is.

For now, to try: a hunt. Five golems on fire that should not be are loose in the town, each
waiting somewhere far off on a pavement of its own; come near one and it runs. Run it down
close enough and she stops and throws her mallet by itself (a tap throws sooner, or, within
reach, strikes); it knocks the plate off its brow, and the golem goes out and falls apart.
At the top, how far off the nearest still loose is, in meters; in the corners, the five plates
as they are won, and the clock; at the edge of the frame, a burning mark
the way each one out of sight is. When the last is caught, five more are let loose.

Every other mission, the folder: dropped from a helicopter somewhere on the roofs, a reward for
it, and golems after it too, each climbing a house of its row and running along the roofs.
Up by a lift or a fire escape in a house of the row, then roof to roof: up the iron ladders
where the next roof stands higher, down onto the lower ones, leaping, gliding, beating the air
to rise; taken, she goes down to the street with it by herself. If a golem gets there first,
the mission is played again.

What is to come is in `PLAN.md`.

## How it is made

- Java 8 and the platform alone; no build system, no libraries.
- The game lives in plain Java: rules, the town, what stands where. Each frame it fills a
  draw list: the eye, the sun, the sky and every box and ball in the scene. The runner's
  figure is blocks riding on the bones of a humanoid skeleton whose motions are in
  `assets/moves.bin` (see `CREDITS`). The townspeople are textured meshes on the same
  skeleton, when the game has them: they are an asset of their own (`assets/folk.bin` and
  `assets/folk.png`), not part of these sources and not under their license; without them the
  game draws simple figures of its own instead.
- The phone draws that list with OpenGL ES 3: the sun's depth map; the sky and the boxes at a
  few pixels to a frame pixel, each material its own worked surface made in the shader, lit by a
  low sun and the sky with soft shadows; then a finishing pass (a soft bloom, the hour's grade,
  the glow of fire) and onto the screen. Meshes go beside the boxes, moved by their bones on the
  graphics chip. The shaders are in `assets/shaders`.
- The desktop stand (`stand/`) plays scenes against the same Java and writes each frame's
  draw list; `stand/web/index.html` draws it with the same shaders in a browser.

## Build

```sh
ANDROID_JAR=... KEYSTORE=... KEY_ALIAS=... KS_PASS=... ./build.sh
```

## License

Zero-Clause BSD, see `LICENSE`.
