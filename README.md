# Skydysh

A game for Android: races through an open town, a southern seaside town on a summer evening,
its streets grown from a seed without end as they are come to, cars going about them and
people out on the pavements. For now on foot: a giant of
brick and stone in a maid's dress runs, turns, jumps and slides through it; a car, a boat and
a plane are to come.

A finger on the glass makes her run; moved sideways, it turns her by as much as it moved;
flicked up, she jumps; flicked down, she slides; lifted, she slows to a stop.

For now, to try: a hunt. Five golems on fire that should not be are loose in the town, each
waiting somewhere far off on a pavement of its own; come near one and it runs. Near enough, her
hammer flies at it by itself and knocks the plate off its brow; it goes out and falls apart.
At the top, how far off the nearest still loose is, in meters; in the corners, the five plates
as they are won, and the clock; at the edge of the frame, a burning mark
the way each one out of sight is. When the last is caught, five more are let loose.

What is to come is in `PLAN.md`.

## How it is made

- Java 8 and the platform alone; no build system, no libraries.
- The game lives in plain Java: rules, the town, what stands where. Each frame it fills a
  draw list: the eye, the sun, the sky and every box and ball in the scene. The runner's
  figure is blocks riding on the bones of a humanoid skeleton whose motions are in
  `assets/moves.bin` (see `CREDITS`).
- The phone draws that list with OpenGL ES 3, flat and inked like a printed picture: the
  sun's depth map; the sky and the boxes at a few pixels to a frame pixel, lit in two tones, each
  pixel marked with what it belongs to; then onto the screen, an ink line wherever one thing
  meets another, halftone dots in the shade, the grain of paper over it all. The shaders are
  in `assets/shaders`.
- The desktop stand (`stand/`) plays scenes against the same Java and writes each frame's
  draw list; `stand/web/index.html` draws it with the same shaders in a browser.

## Build

```sh
ANDROID_JAR=... KEYSTORE=... KEY_ALIAS=... KS_PASS=... ./build.sh
```

## License

Zero-Clause BSD, see `LICENSE`.
