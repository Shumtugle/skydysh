# Skydysh

A game for Android about knocking down buildings with a hammer, on a summer street of a
southern seaside town. One long evening: a building on the plinth in the middle of the
road, five stars to throw with, a hundred and one levels from the first light of the
evening into the night; after the last the street begins again.

## How it is made

- Java 8 and the platform alone; no build system, no libraries.
- The game lives in plain Java: rules, physics, what stands where. Each frame it fills a
  draw list: the eye, the sun, the sky and every box and ball in the scene.
- The phone draws that list with OpenGL ES 3, flat and inked like a printed picture: the
  sun's depth map; the sky and the boxes at the screen's own size, lit in two tones, each
  pixel marked with what it belongs to; then onto the screen, an ink line wherever one thing
  meets another, halftone dots in the shade, the grain of paper over it all. The shaders are
  in `assets/shaders`.
- The corner (stars and level), the aim and the dispatcher's panel between rounds are drawn
  smooth over the scene.
- The desktop stand (`stand/`) plays scenes against the same Java and writes each frame's
  draw list; `stand/web/index.html` draws it with the same shaders in a browser.

## Build

```sh
ANDROID_JAR=... KEYSTORE=... KEY_ALIAS=... KS_PASS=... ./build.sh
```

## License

Zero-Clause BSD, see `LICENSE`.
