# Skydysh

A game for Android: races through an open town, a southern seaside town on a summer evening
seen as town games were drawn early in the century,
its streets grown from a seed without end as they are come to, cars going about them,
people out on the pavements with their dogs, cats by the walls (who mind what she does: a mallet
thrown near them and they are off); squares with fountains or
gardens, trees, benches and pots of flowers along the streets. For now on foot: a brum, a giant of
brick and stone in a maid's dress runs, turns, jumps and slides through it; a car, a boat and
a plane are to come.

The town is lived in by humans and brums. A brum is a shell of brick and stone made to stand
and walk; she is one, and so are the ones she hunts.

It opens, at every start, with a flight in from the sea over the beach and the square to home,
where she stands in the shore road with a golden mallet and brings it down into the glass of the
screen, and the name is typed in the letters of the dispatcher's tube; a touch skips it. The game
begins before home. Then the
dispatcher's room, which is her head: walls of her own brick round the street seen through her
eyes, what is to be done typed over it; a touch, her mallet knocks a key, the walls draw back
and the street goes on. When it is done, back into her head for the report.

There are two ways to play, chosen under the pause button and kept: one finger, as it was from
the first (down runs her, sideways turns her, flicks jump and slide, a tap is the mallet's; at
the wheel, down drives, sideways steers, drawn down brakes), and two thumbs.

With two thumbs, the left half of the glass is a stick: where the thumb comes down is its
middle, and pushed a little she walks, further she jogs, all the way she runs, the way it is
pushed being the way she goes as the eye sees it (up the glass into the frame, down it toward
the eye); let go, she stops in a step or two. The right half turns the eye round her; left alone
a while, it comes round behind her again as she goes. Low on the right are her buttons: the
jump (pressed again in the air, she goes on up, over a car or onto what stands a storey up;
gliding, it beats her arms and lifts her, four times a flight), the slide over it, and the
mallet's beside it; a flick up or down on the right half jumps or slides her too. Falling from
high enough, she glides down by herself; run at a ladder, she takes hold and climbs it hand over
hand, and pulled down, the stick lets her go. The mallet, by its button or a tap on the right
half: a brum within reach she strikes, one ahead and not too far she throws at; with none, she
throws it ahead and it comes back. Half the blocks
have a yard open to the street by an arch; the others are shut in, with a fire escape up the back
of a house; the fire escapes are all in the yards. Some houses have a way in, a hall through to
the yard; a few have a lift. The
door of a house with a lift opens by itself when she comes to it or stands at it: inside is
its hall, made from the house's seed, and in its side wall the lift, which is called, opens,
takes her up and lets her out on the roof by itself. On the roof, by the hut's door, a button
shows; pressed, the lift takes her down and the cabin opens onto the hall again. Signs of glass
hang over the ways in and up near her: the lifts' doors, the fire escapes, the ladders from roof
to roof; touched, a sign sends her there by herself, up the ladder, or in and up (or down and
out) by the lift. A button high on the left pauses the game.

At the wheel with two thumbs, a finger down anywhere drives and steers as one finger does, the
driver picking the speed; the pedals low on the right have the say over him while held: the
throttle flat out, the brake (held standing, it backs).

The town ends at the sea: along its southmost street a promenade with palms and a parapet, steps
down to a beach with sunshades, and the water. Into the sea she wades, slowly, and where it is
deep enough she swims, her head up, as far out as the open sea lets her; back in the shallows
she stands up and wades out. Home is there, where the game begins: one large house with a tower and a lamp on
top, the same in every town; its sign over the door takes her in.

Duty pickups stand at the curbs here and there (not in a hunt). Touch the wheel over one, or
stand at its door, and she gets in. Driving is the easy way about the town, and the fast one:
a finger down drives it, up to some 130 kilometers an hour; sideways from where the finger came
down turns the wheel, and the pickup goes where its nose points, as sharply as the wheel says,
without sliding; run into a wall, it runs along it. Drawn down, the finger brakes, and held, it
backs; lifted, it rolls to a stop. How fast it goes is shown low on the left. The button low on the right lets her out. She stands on
whatever she comes down on, the roofs too. When a mission is done the game keeps where it is.

For now, to try: a hunt. Five brums on fire that should not be are loose in the town, each
waiting somewhere far off on a pavement of its own; come near one and it runs. Run it down
close enough and she stops and throws her mallet by itself (a tap throws sooner, or, within
reach, strikes); it knocks the plate off its brow, and the brum goes out and falls apart.
At the top, how far off the nearest still loose is, in meters; in the corners, the five plates
as they are won, and the time left; at the edge of the frame, a burning mark
the way each one out of sight is. A hunt has five minutes: when the last is caught in time, five
more are let loose; when the time runs out first, five again.

The second mission, the folder: dropped from a helicopter somewhere on the roofs, a reward for
it, and brums after it too, each climbing a house of its row and running along the roofs.
Up by a lift or a fire escape in a house of the row, then roof to roof: up the iron ladders
where the next roof stands higher, down onto the lower ones, leaping, gliding, beating the air
to rise; taken, she goes down to the street with it by herself. If a brum gets there first,
the mission is played again.

The third mission, the delivery: a load waits at the factory with the chimney, west along the
shore road, and is wanted at the store, east along it. A duty pickup to the factory's gate,
stood in the bay painted before it while the crates go on; to the store's, while they come off;
then home. The missions come in turn: the hunt, the folder, the delivery, and round again; or
come home, touch its sign, and in her head the screen asks which mission now: a touch on the
hunt, the folder or the delivery begins it there and then, a touch on the last line goes on with
the one now.

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
- The cars are the game's own: their shapes are boxes in the code, and each wears a skin
  painted for it on its five flat views (from the left, the right, the front, the back and
  above), all in `assets/cars.png`, where each lies in `assets/cars.txt`. The skin is looked up
  by the way a face looks; where it is paint, the car's own paint is laid on in its place.
  The wheels are round, drawn as balls are. The skins are under the same license as the rest.
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
