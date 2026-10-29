# Changelog

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
