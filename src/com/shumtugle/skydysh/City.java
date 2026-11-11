package com.shumtugle.skydysh;

import java.util.HashMap;
import java.util.Random;

/**
 * The town, without end: a grid of streets, and between them blocks of old seaside houses, now
 * and then a square with palms. It is grown from a seed one cell at a time, only where the
 * runner is: a cell is built when it comes near and forgotten when it is left far behind, and
 * built again the same when it is come back to.
 *
 * A cell is CELL meters square. Its streets run along its west edge (x = i * CELL, north and
 * south) and its south edge (z = j * CELL, east and west), crossing at its south-west corner;
 * the rest of it is the block those streets go round. Space as in DrawList: x to the right,
 * y up, the road at y = 0.
 *
 * The town is by the sea: its southmost street, along z = SHORE * CELL, is the shore road, and
 * south of it there are no more streets, only a promenade along it, a stone parapet at its edge
 * and the water below.
 */
final class City implements Ground {
    static final float PARKED = 4.85f;           // a parked car's middle, from the road's middle
    static final float CELL = 64f;
    // across a street from its middle: the asphalt, the curb, the pavement up to the house fronts.
    // Two lanes wide enough to pass in, and a row of parked cars at each curb outside them
    static final float ROAD = 6f, CURB = 6.25f, FRONT = 8.95f;
    static final float WALK_TOP = 0.14f, CURB_TOP = 0.15f;
    // how deep the houses are, from the front back into the block
    static final float DEPTH = 12f;
    // the cells drawn round the eye, each way
    static final int REACH = 3;

    /** The row of crossings the shore road runs through; the town north of it, the sea south. */
    static final int SHORE = 0;
    // the parapet along the promenade (where its middle is, half its thickness, how high over the
    // pavement), the water's top, and how deep under it one who falls in goes
    static final float PARAPET = -18f, PARAPET_HALF = 0.3f, PARAPET_HIGH = 0.95f, WATER = -1.7f, DEEP = 1.4f;
    // the beach under the quay: its sand, the wet sand by the sea, where the wet begins and where the
    // water does (from the shore road's middle); a flight of steps down from the promenade in every cell
    static final float SAND = -1.15f, WET = -1.5f, WET_FROM = -30f, WATERLINE = -35f;
    // the bed of the sea: shelving from the water's edge down to so deep (under the water's top) so
    // far out; and how far out one may swim
    static final float BED = 4f, SHELF = 16f, OPEN_SEA = -220f;
    static final float STAIRS_AT = 28f, STAIRS_HALF = 1.4f, STAIR = 1f;

    /** Whether cell row j is out of the town: the promenade (the row next to the shore) and the sea. */
    static boolean sea(int j) {
        return j < SHORE;
    }

    /** Whether a street runs from a crossing in row j one step along dj (north 1, south -1): none out to sea. */
    static boolean road(int j, int dj) {
        return j >= SHORE && j + dj >= SHORE && j <= EDGE_J && j + dj <= EDGE_J;
    }

    // ---- the town's end inland, and the country past it ----
    /**
     * The row of crossings the town ends at, inland: its street, east and west, is the last; past
     * it, fields and hills, one road out north, a ridge with a pass in it, and a village beyond.
     */
    static final int EDGE_J = 10;
    /** The road out: it leaves the town up the street by home's west side, and wanders as it goes. */
    static final float ROUTE_X = 0f, ROUTE_SWING = 18f, ROUTE_WAVE = 70f, ROUTE_HALF = 5f;
    /** The ridge across the country (its row), the pass through it, and the village (its square's middle). */
    static final int RIDGE_J = EDGE_J + 4;
    static final float PASS_Z = RIDGE_J * CELL + 32f, PASS_HALF = 8.5f;
    static final float VILLAGE_Z = (EDGE_J + 8) * CELL + 10f, SQUARE_HALF = 16f;

    /** Whether row j is past the town's end (its first row still has the last street). */
    static boolean country(int j) {
        return j >= EDGE_J;
    }

    /** Where the road out runs at z (its middle, x). */
    static float routeX(float z) {
        float t = Math.max(0f, z - EDGE_J * CELL);
        float k = Math.min(1f, t / 40f);
        return ROUTE_X + ROUTE_SWING * k * (float) Math.sin(t / ROUTE_WAVE);
    }

    /** The village square's middle, x. */
    static float villageX() {
        return routeX(VILLAGE_Z);
    }

    /** Whether (x, z) is on the road out, or the village square. */
    static boolean onRoute(float x, float z, float margin) {
        if (z < EDGE_J * CELL - CURB) return false;
        if (Math.abs(z - VILLAGE_Z) < SQUARE_HALF + margin && Math.abs(x - villageX()) < SQUARE_HALF + margin) return true;
        return z < VILLAGE_Z && Math.abs(x - routeX(z)) < ROUTE_HALF + margin;
    }

    /** A way out of a crossing in row j (0 +x, 1 -x, 2 +z, 3 -z), one that is a street: inland, not out to sea. */
    static int way(int j, int dir) {
        return dir == 3 && !road(j, -1) ? 2 : dir;
    }

    /** Whether (x, z) is over the water, past the beach. */
    static boolean overWater(float x, float z) {
        return z < SHORE * CELL + WATERLINE && Island.height(x, z) < WATER;
    }

    /** Whether she may be at (x, z) as far as the sea goes: this side of the open sea, or about the spit and the island. */
    static boolean reachable(float x, float z) {
        return z >= SHORE * CELL + OPEN_SEA || Island.near(x, z, 40f);
    }

    /** Whether (x, z) is on the beach, under the quay. */
    static boolean beach(float x, float z) {
        return z < SHORE * CELL + PARAPET - PARAPET_HALF && !overWater(x, z);
    }

    /**
     * Where home was, and the old house still stands: the south row of the block by the shore
     * road, where the opening is seen; its balcony over the door, and in its flat now a brum who
     * lives there. Its storeys, its tower.
     */
    static final int OLD_I = 0, OLD_J = SHORE;
    static final int HOME_STOREYS = 8;
    static final float HOME_TOWER = 7f;
    /** The old house's door, in the middle of its front, on the shore road; the way out from it is -z, to the sea. */
    static final float OLD_DOOR_X = OLD_I * CELL + CELL * 0.5f, OLD_DOOR_Z = OLD_J * CELL + FRONT;
    /**
     * Where home is: its own quarter inland, four blocks: home's (see homeBlock), the park east of
     * it, the shop across the street from its door, the taxis' stand beside the shop. Home stands
     * alone in its block, back from the street behind a forecourt so deep (the pickup's place in
     * it), as wide and as deep as its rooms are (see Rooms.home), so high, the pad on its roof.
     */
    static final int HOME_I = 4, HOME_J = 4, PARK_I = 5, PARK_J = 4, SHOP_I = 4, SHOP_J = 3, TAXI_I = 5, TAXI_J = 3;
    static final float HOME_SET_BACK = 6f, HOME_HX = Rooms.HOME_HALF + 0.4f, HOME_HZ = Rooms.HOME_DEEP * 0.5f, HOME_HIGH = Indoors.FLOOR + Rooms.HOME_HIGH + 0.3f;
    /** Home's door, in the middle of its front, on its forecourt; the way out from it is -z, to the shop across the street. */
    static final float HOME_DOOR_X = HOME_I * CELL + CELL * 0.5f, HOME_DOOR_Z = HOME_J * CELL + FRONT + HOME_SET_BACK;

    /** Whether cell (i, j) is one of home's quarter built by hand: home's block, the shop's, the taxis'. */
    static boolean quarter(int i, int j) {
        return i == HOME_I && j == HOME_J || i == SHOP_I && j == SHOP_J || i == TAXI_I && j == TAXI_J;
    }
    /**
     * The balcony on the old house's sea front, over the door on the fifth storey: no way up to it but
     * through the air (a long glide, the island's top the place to leap from), and in at its door,
     * warm light through the gap, the hall no one knows of. Its floor; half across; how deep.
     */
    static final float SECRET_Y = 3.3f * 5f + 0.3f, SECRET_HALF = 2.6f, SECRET_DEEP = 1.6f;

    /** Whether (x, y, z) is on the secret balcony, near its door. */
    static boolean atSecretDoor(float x, float y, float z) {
        return Math.abs(x - OLD_DOOR_X) < SECRET_HALF - 0.3f && z > OLD_DOOR_Z - 0.9f && z < OLD_DOOR_Z && Math.abs(y - SECRET_Y) < 0.3f;
    }

    /** Whether (x, z) is kept clear of lamps, parked cars and palms: the way from the old house's door straight out to the sea, home's way in, the bays. */
    static boolean keptClear(float x, float z) {
        if (Math.abs(x - OLD_DOOR_X) < 4.5f && z < OLD_DOOR_Z && z > OLD_J * CELL + PARAPET) return true;
        // the way from home's forecourt across the street to the shop, and the pickup's way in
        if (Math.abs(x - HOME_DOOR_X) < 4.5f && z > HOME_J * CELL - CURB - 0.5f && z < HOME_J * CELL + FRONT) return true;
        if (Math.abs(x - HOME_PICKUP_X) < 4f && z > HOME_J * CELL - 0.5f && z < HOME_J * CELL + FRONT) return true;
        // and the bays before the factory's and the store's gates, and the vets' door
        for (int k = 0; k < 2; k++)
            if (Math.abs(x - gateX(k == 0)) < 7f && z > SHORE * CELL - 0.5f && z < SHORE * CELL + FRONT) return true;
        if (Math.abs(x - clinicDoorX()) < 7f && z > CLINIC_J * CELL - 0.5f && z < CLINIC_J * CELL + FRONT) return true;
        if (Math.abs(z - workshopZ()) < 9f && x < 0.5f && x > -FRONT) return true;
        // the town hall's steps and door, from the curb
        if (Math.abs(x - (TOWNHALL_I * CELL + CELL * 0.5f)) < 6f && z > TOWNHALL_J * CELL - 0.5f && z < TOWNHALL_J * CELL + FRONT) return true;
        return false;
    }

    /**
     * The factory and the store, built by hand and the same in every town, on the shore road: the
     * factory west of home, the store east of it, each the south row of its block, a gate in its
     * middle and a bay painted on the road before it where a pickup stands to be loaded or unloaded.
     */
    static final int FACTORY_I = OLD_I - 4, STORE_I = OLD_I + 5;
    /** The port: the container yard by the store, its crane over it. */
    static final int PORT_I = STORE_I + 1, PORT_J = SHORE;
    // the bay: half its length along the road, and its middle across it (in the lane by the gate)
    static final float BAY_HALF = 4.5f, BAY_Z = 3.4f, BAY_DEEP = 1.9f;

    /** The middle of the factory's gate (true) or the store's (false), along the shore road. */
    static float gateX(boolean factory) {
        return (factory ? FACTORY_I : STORE_I) * CELL + CELL * 0.5f;
    }

    /** Whether (x, z) is in the bay before the factory's gate (true) or the store's (false). */
    static boolean inBay(boolean factory, float x, float z) {
        return Math.abs(x - gateX(factory)) < BAY_HALF && Math.abs(z - (SHORE * CELL + BAY_Z)) < BAY_DEEP;
    }

    /**
     * The vets': built by hand, the same in every town, the south row of a block three streets up
     * from the shore road, one block east of home's; a bay painted on the street before its door,
     * where a pickup stands to hand in what it brings.
     */
    static final int CLINIC_I = OLD_I + 1, CLINIC_J = SHORE + 3;

    static float clinicDoorX() {
        return CLINIC_I * CELL + CELL * 0.5f;
    }

    static float clinicDoorZ() {
        return CLINIC_J * CELL + FRONT;
    }

    static float clinicBayZ() {
        return CLINIC_J * CELL + BAY_Z;
    }

    /** Whether (x, z) is in the bay before the vets' door. */
    static boolean inClinicBay(float x, float z) {
        return Math.abs(x - clinicDoorX()) < BAY_HALF && Math.abs(z - clinicBayZ()) < BAY_DEEP;
    }

    /**
     * The quarry: cut into the foot of the ridge west of the road to the pass, a floor of gravel
     * open to the south and faces of rock stepped up round it on the other three sides.
     */
    static final float QUARRY_X = -100f, QUARRY_Z = 866f, QUARRY_HX = 22f, QUARRY_HZ = 18f;

    /** Whether (x, z) is in the quarry, or within so far of it. */
    static boolean inQuarry(float x, float z, float margin) {
        return Math.abs(x - QUARRY_X) < QUARRY_HX + margin && Math.abs(z - QUARRY_Z) < QUARRY_HZ + margin;
    }

    /**
     * The brums' workshop, where brums are put back together and their things mended: built by hand
     * at the town's last street, the east row of the block west of the road out, its gate on that
     * road; a bay painted on the road before it, on its side.
     */
    static final int WORKSHOP_I = -1, WORKSHOP_J = EDGE_J - 1;
    static final float WORKSHOP_BAY_X = -BAY_Z;

    static float workshopZ() {
        return WORKSHOP_J * CELL + CELL * 0.5f;
    }

    /** Whether (x, z) is in the bay before the workshop's gate. */
    static boolean inWorkshopBay(float x, float z) {
        return Math.abs(x - WORKSHOP_BAY_X) < BAY_DEEP + 0.7f && Math.abs(z - workshopZ()) < BAY_HALF + 3.5f;
    }

    /**
     * The concert hall: built by hand, the same in every town, the south row of a block north of
     * the old town; a portico of columns on broad steps, three tall doors behind it, the middle
     * one the way in (see Concert).
     */
    static final int CONCERT_I = -2, CONCERT_J = 4;

    /**
     * The town hall: built by hand, on the main way north from home, two rows inland; pale stone,
     * five storeys, a lift behind its middle door up to the roof, and on the roof a pad for a
     * helicopter, a flag over it. In its hall, the secretariat (see Rooms.townHall).
     */
    static final int TOWNHALL_I = 0, TOWNHALL_J = 2;
    static final float TOWNHALL_HIGH = 17f;

    /** Where on the town hall's roof the helicopter stands: east of the hut, the roof's middle (x, z). */
    static float townPadX() {
        return TOWNHALL_I * CELL + CELL * 0.5f + 10.5f;
    }

    static float townPadZ() {
        return TOWNHALL_J * CELL + FRONT + DEPTH * 0.5f;
    }

    /** The town hall's roof, its top. */
    static float townRoof() {
        return TOWNHALL_HIGH + 0.5f;
    }

    /** Whether the house whose middle is at (x, z) is the town hall. */
    static boolean townHall(float x, float z) {
        return index(x) == TOWNHALL_I && index(z) == TOWNHALL_J && z < TOWNHALL_J * CELL + FRONT + DEPTH + 1f;
    }

    /**
     * The archive: built by hand, far to the north-east, near the town's end; a low, heavy house of
     * grey stone, small windows, the south row of its block. Behind it the yard is a garden,
     * hedges and beds, and in the garden's middle a round pad for a helicopter. A way through the
     * north row into the yard, under an arch; the archive's own back door on the garden.
     */
    static final int ARCHIVE_I = 3, ARCHIVE_J = 9;
    static final float ARCHIVE_HIGH = 11f, PAD_R = 6.2f;

    /** The pad in the archive's garden, its middle (x, z). */
    static float archivePadX() {
        return ARCHIVE_I * CELL + CELL * 0.5f;
    }

    static float archivePadZ() {
        return ARCHIVE_J * CELL + CELL * 0.5f + 1f;
    }

    /** The archive's back door on the garden, a step before it (x, z). */
    static float archiveDoorX() {
        return ARCHIVE_I * CELL + CELL * 0.5f;
    }

    /** The archive's back wall, on the garden, z (its door in its middle). */
    static float archiveWallZ() {
        return ARCHIVE_J * CELL + FRONT + DEPTH;
    }

    static float archiveDoorZ() {
        return ARCHIVE_J * CELL + FRONT + DEPTH + 1.2f;
    }

    static float concertDoorX() {
        return CONCERT_I * CELL + CELL * 0.5f;
    }

    static float concertDoorZ() {
        return CONCERT_J * CELL + FRONT + 3f;
    }

    /**
     * Whether (x, z) is on the concert hall's portico, its steps or its floor before the doors, the
     * whole width of it: the way in is anywhere there, through any of its three doors.
     */
    static boolean onPortico(float x, float z) {
        float half = (CELL * 0.5f - FRONT) * 0.55f, front = CONCERT_J * CELL + FRONT;
        return Math.abs(x - concertDoorX()) < half + 0.4f && z > front - 1.4f && z < concertDoorZ() + 0.3f;
    }

    /** Whether (x, z) is within home's walls. */
    static boolean home(float x, float z) {
        return Math.abs(x - HOME_DOOR_X) < HOME_HX && z > HOME_DOOR_Z && z < HOME_DOOR_Z + 2f * HOME_HZ;
    }

    /** Whether (x, z) is within the old house's walls. */
    static boolean oldHome(float x, float z) {
        float x0 = OLD_I * CELL + FRONT, z0 = OLD_J * CELL + FRONT;
        return x > x0 && x < x0 + CELL - 2f * FRONT && z > z0 && z < z0 + DEPTH;
    }

    private final long seed;
    private final HashMap<Long, Cell> cells = new HashMap<Long, Cell>();

    City(long seed) {
        this.seed = seed;
    }

    /** One cell, as built: its boxes, and what cannot be walked through, as boxes on the ground. */
    static final class Cell {
        final DrawList boxes = new DrawList();
        // the small things (palms, lamp posts, chimneys): drawn only near
        final DrawList detail = new DrawList();
        // the parked cars: nearer still
        final DrawList parked = new DrawList();
        // x0, z0, x1, z1, how high its top is, how high its bottom (0, on the ground), for each
        static final int WALL = 6;
        float[] walls = new float[96];
        int wallN;
        int used;
        // the houses: middle x, z, half sizes x, z, the top of the roof, the way the front faces
        // (x, z), and the way up it from the street: none (0), a lift (LIFT), a fire escape (ESCAPE)
        static final int HOUSE = 8;
        float[] houses = new float[HOUSE * 16];
        int houseN;
        // the ladders: where the ladder's foot is on its wall (x, z), the bottom and top of the
        // climb, the way the wall faces (x, z): seven floats each
        static final int LADDER = 6;
        float[] ladders = new float[LADDER * 8];
        int ladderN;

        // the benches: middle x, z, the way one sitting on it faces (a heading)
        float[] benches = new float[3 * 8];
        // the loungers on its beach, if it has one: where one lies (x, z), facing the sea
        float[] loungers = new float[2 * 8];
        int loungerN;
        // the crowns of its trees and palms, round masses of leaves (x, y, z, radius): the eye keeps out of them
        float[] crowns = new float[4 * 8];
        int crownN;
        // its parked cars: where (x, z), the way it faces, its kind, its paint; one rammed is moved off
        float[] parkedCars = new float[5 * 8];
        int parkedN;
        // its street lamps, where each stands (x, z): one rammed is felled
        float[] lamps = new float[2 * 8];
        int lampN;
        int benchN;

        void ladder(float x, float z, float bottom, float top, float nx, float nz) {
            if ((ladderN + 1) * LADDER > ladders.length) ladders = java.util.Arrays.copyOf(ladders, ladders.length * 2);
            float[] l = {x, z, bottom, top, nx, nz};
            System.arraycopy(l, 0, ladders, ladderN * LADDER, LADDER);
            ladderN++;
        }

        /** The house standing at (x, z), as its place in houses, or -1. */
        int houseAt(float x, float z) {
            for (int k = 0; k < houseN; k++) {
                int o = k * HOUSE;
                if (Math.abs(x - houses[o]) < houses[o + 2] && Math.abs(z - houses[o + 1]) < houses[o + 3]) return o;
            }
            return -1;
        }

        void house(float cx, float cz, float hx, float hz, float top, float nx, float nz, float up) {
            if ((houseN + 1) * HOUSE > houses.length) houses = java.util.Arrays.copyOf(houses, houses.length * 2);
            float[] h = {cx, cz, hx, hz, top, nx, nz, up};
            System.arraycopy(h, 0, houses, houseN * HOUSE, HOUSE);
            houseN++;
        }

        void wall(float x0, float z0, float x1, float z1) {
            wall(x0, z0, x1, z1, TALL);
        }

        void wall(float x0, float z0, float x1, float z1, float top) {
            wall(x0, z0, x1, z1, top, 0f);
        }

        /** The same, standing on nothing below bottom: over an arch, on a roof. */
        void wall(float x0, float z0, float x1, float z1, float top, float bottom) {
            if ((wallN + 1) * WALL > walls.length) walls = java.util.Arrays.copyOf(walls, walls.length * 2);
            int o = wallN * WALL;
            walls[o] = Math.min(x0, x1);
            walls[o + 1] = Math.min(z0, z1);
            walls[o + 2] = Math.max(x0, x1);
            walls[o + 3] = Math.max(z0, z1);
            walls[o + 4] = top;
            walls[o + 5] = bottom;
            wallN++;
        }
    }

    private int frame;

    private static long key(int i, int j) {
        return ((long) i << 32) ^ (j & 0xFFFFFFFFL);
    }

    Cell cell(int i, int j) {
        long k = key(i, j);
        Cell c = cells.get(k);
        if (c == null) {
            c = build(i, j);
            cells.put(k, c);
        }
        c.used = frame;
        return c;
    }

    static int index(float v) {
        return (int) Math.floor(v / CELL);
    }

    /** The cells over (x0, z0)-(x1, z1) built again when next asked: what stands there has changed. */
    void forget(float x0, float z0, float x1, float z1) {
        for (int i = index(x0); i <= index(x1); i++)
            for (int j = index(z0); j <= index(z1); j++) cells.remove(key(i, j));
    }

    /**
     * The cells round (x, z) that the eye looking along (fx, fz) can see, into the list, their
     * small things only near; the ones long out of reach forgotten.
     */
    void draw(DrawList d, float x, float z, float fx, float fz) {
        frame++;
        int ci = index(x), cj = index(z);
        for (int j = cj - REACH; j <= cj + REACH; j++)
            for (int i = ci - REACH; i <= ci + REACH; i++) {
                // the cell's middle from the eye, and how far it reaches round it
                float dx = (i + 0.5f) * CELL - x, dz = (j + 0.5f) * CELL - z;
                float dist = (float) Math.sqrt(dx * dx + dz * dz), reach = CELL * 0.75f;
                if (dist > reach && (dx * fx + dz * fz) < -reach) continue;
                if (dist > (REACH + 0.5f) * CELL) continue;
                Cell c = cell(i, j);
                d.append(c.boxes);
                if (dist < CELL * 1.6f) d.append(c.detail);
                if (dist < CELL * 1.2f) d.append(c.parked);
            }
        // the mountain, seen from anywhere; the spit and the island out to sea
        Mountain.draw(d, x, z);
        Island.draw(d);
        // the sea, one sheet out to the haze from the edge of the beach
        float edge = SHORE * CELL + WATERLINE;
        if (z - edge < (REACH + 2) * CELL) {
            float mx = Math.round(x / CELL) * CELL, far = 1500f;
            d.box(mx, WATER - 0.25f, edge - far * 0.5f, far, 0.25f, far * 0.5f, null, 0x2D6990, DrawList.material(DrawList.SEA, 1));
        }
        if (cells.size() > (2 * REACH + 3) * (2 * REACH + 3)) {
            java.util.Iterator<Cell> it = cells.values().iterator();
            while (it.hasNext()) if (frame - it.next().used > 120) it.remove();
        }
    }

    // what has no top to stand on (a palm); how high a step is taken in the stride, or pushed against
    static final float TALL = 1000f, STEP = 0.6f, LOW = 0.35f;
    // how much room under something over her she needs to pass: her height, and a little
    static final float HEADROOM = 2.4f;

    /** How high the ground is here: the road, or the pavement a step up. */
    float ground(float x, float z) {
        if (z < SHORE * CELL - CURB) {
            if (z < SHORE * CELL + WATERLINE) {
                // the sea's bed, shelving from the wet sand down; the spit and the island over it
                float out = Math.min(1f, (SHORE * CELL + WATERLINE - z) / SHELF);
                return Math.max(WET + (WATER - BED - WET) * out, Island.height(x, z));
            }
            if (!beach(x, z)) return WALK_TOP;
            return z > SHORE * CELL + WET_FROM ? SAND : WET;
        }
        // past the town's last street, the country: grass, the road out, the village square, all
        // level; but for the mountain and the road up it
        if (z > EDGE_J * CELL + CURB) return Math.max(Mountain.ground(x, z), ridge(x, z));
        float ox = x - Math.round(x / CELL) * CELL, oz = z - Math.round(z / CELL) * CELL;
        return Math.abs(ox) < CURB || Math.abs(oz) < CURB ? 0f : WALK_TOP;
    }

    /**
     * For the desktop stand and for missions: the roof of a house near (x, z), whose middle and
     * top go into out (x, top, z) and whose half sizes into half (x, z); false if none is near.
     */
    boolean roofNear(float x, float z, float[] out, float[] half) {
        float best = 1e9f;
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * Cell.WALL;
                    float top = c.walls[o + 4];
                    if (top < 10f || top > 40f || c.walls[o + 5] > 0f) continue;
                    float w = c.walls[o + 2] - c.walls[o], dpt = c.walls[o + 3] - c.walls[o + 1];
                    if (w < 5f || dpt < 5f) continue;
                    float mx = (c.walls[o] + c.walls[o + 2]) * 0.5f, mz = (c.walls[o + 1] + c.walls[o + 3]) * 0.5f;
                    float dd = (mx - x) * (mx - x) + (mz - z) * (mz - z);
                    if (dd < best) {
                        best = dd;
                        out[0] = mx;
                        out[1] = top;
                        out[2] = mz;
                        half[0] = w * 0.5f;
                        half[1] = dpt * 0.5f;
                    }
                }
            }
        return best < 1e9f;
    }

    /**
     * The highest thing under (x, z) that one whose feet are at feet can stand on: the street, or
     * the top of whatever is there no higher than reach above the feet (a roof, a car, a step).
     */
    public float ground(float x, float z, float feet, float reach) {
        float g = ground(x, z);
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * Cell.WALL;
                    float top = c.walls[o + 4];
                    if (top <= g || top > feet + reach) continue;
                    if (x >= c.walls[o] && x <= c.walls[o + 2] && z >= c.walls[o + 1] && z <= c.walls[o + 3]) g = top;
                }
            }
        return g;
    }

    /**
     * A wall standing over (x, z) for one whose feet are at feet: the box of a house or such, rising
     * past her head from no higher than her knees; its sides and top into out (x0, z0, x1, z1, top).
     */
    boolean wallAt(float x, float z, float feet, float[] out) {
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * Cell.WALL;
                    if (c.walls[o + 4] < feet + HEADROOM || c.walls[o + 5] > feet + 0.5f) continue;
                    if (x < c.walls[o] || x > c.walls[o + 2] || z < c.walls[o + 1] || z > c.walls[o + 3]) continue;
                    for (int q = 0; q < 5; q++) out[q] = c.walls[o + q];
                    return true;
                }
            }
        return false;
    }

    /** Out of doors the eye goes where it likes, pushed out of the houses (see push). */
    public void keepEye(float[] p) {
    }

    /** Out of doors the sky is the ceiling, but for what stands over one whose feet are at feet: an arch. */
    public float ceiling(float x, float z, float feet) {
        float c0 = TALL;
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * Cell.WALL;
                    float b = c.walls[o + 5];
                    if (b <= feet || b >= c0) continue;
                    if (x >= c.walls[o] && x <= c.walls[o + 2] && z >= c.walls[o + 1] && z <= c.walls[o + 3]) c0 = b;
                }
            }
        return c0;
    }

    /**
     * Pushes a round body of radius r standing at (p[0], p[1]) out of whatever it went into;
     * true if it touched something.
     */
    boolean push(float[] p, float r) {
        return push(p, r, 0f);
    }

    /** The same, for one whose feet are at feet: what is lower than a step under them is not in the way. */
    /** Something more in the way than the town's own walls (what blocks the pass on a call out); none, none. */
    interface Extra {
        boolean push(float[] p, float r, float feet);
    }

    Extra extra;

    public boolean push(float[] p, float r, float feet) {
        boolean hit = extra != null && extra.push(p, r, feet);
        int ci = index(p[0]), cj = index(p[1]);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int o = k * Cell.WALL;
                    // what is lower than a step under the feet, or high enough over the head to pass under
                    if (c.walls[o + 4] <= feet + LOW || c.walls[o + 5] > feet + HEADROOM) continue;
                    float x0 = c.walls[o], z0 = c.walls[o + 1], x1 = c.walls[o + 2], z1 = c.walls[o + 3];
                    float nx = Math.max(x0, Math.min(p[0], x1)), nz = Math.max(z0, Math.min(p[1], z1));
                    float dx = p[0] - nx, dz = p[1] - nz, dd = dx * dx + dz * dz;
                    if (dd >= r * r) continue;
                    hit = true;
                    if (dd > 1e-8f) {
                        float l = (float) Math.sqrt(dd);
                        p[0] = nx + dx / l * r;
                        p[1] = nz + dz / l * r;
                    } else {
                        // inside: out by the nearest side
                        float l = p[0] - x0, rr = x1 - p[0], b = p[1] - z0, t = z1 - p[1];
                        float m = Math.min(Math.min(l, rr), Math.min(b, t));
                        if (m == l) p[0] = x0 - r;
                        else if (m == rr) p[0] = x1 + r;
                        else if (m == b) p[1] = z0 - r;
                        else p[1] = z1 + r;
                    }
                }
            }
        return hit;
    }

    // ---- growing a cell ----

    private long hash(int i, int j, int salt) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL + j * 0x165667B19E3779F9L + salt * 0x27D4EB2F165667C5L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return h;
    }

    /** What a cell's block is: houses, mostly; now and then a square. The one the runner starts by is houses. */
    boolean square(int i, int j) {
        if (sea(j) || oldTown(i, j)) return false;
        // the park with the water tower, a landmark seen from far off
        if (i == TOWER_I && j == TOWER_J) return true;
        // across the street from the old house, a square with a fountain; the old house's own block, houses
        if (i == OLD_I - 1 && j == OLD_J) return true;
        if (i == 0 && j == 0) return false;
        // home's quarter: the park, and the blocks built by hand
        if (i == PARK_I && j == PARK_J) return true;
        if (quarter(i, j)) return false;
        if (j == SHORE && (i == FACTORY_I || i == STORE_I)) return false;
        if (i == TOWNHALL_I && j == TOWNHALL_J || i == ARCHIVE_I && j == ARCHIVE_J) return false;
        return (hash(i, j, 1) & 0xFF) < 30;
    }

    /** Whether the street along the cell's west edge has palms down its pavements. */
    private boolean palmy(int i, int j) {
        return (hash(i, j, 2) & 3) == 0;
    }

    /** Whether a duty pickup stands at the curb in cell (i, j): one cell in five, not by the start. */
    boolean duty(int i, int j) {
        if (i == HOME_I && j == HOME_J) return homePickup;
        return seeded(i, j);
    }

    /**
     * Home's own duty pickup, at the curb a few steps from its door: out once the third mission is
     * done, so that the missions taken at home after it have one to hand.
     */
    boolean homePickup;
    static final float HOME_PICKUP_X = HOME_DOOR_X + 9f;

    /** Whether cell (i, j) has a duty pickup of the seed's: one cell in five, none round home. */
    boolean seeded(int i, int j) {
        if (sea(j) || country(j)) return false;
        if (Math.abs(i) <= 1 && Math.abs(j) <= 1) return false;
        if (Math.abs(i - HOME_I) <= 1 && Math.abs(j - HOME_J) <= 1) return false;
        // one always at the curb by the archive
        if (i == ARCHIVE_I && j == ARCHIVE_J) return true;
        return ((hash(i, j, 7) >>> 24) % 5) == 0;
    }

    // a duty pickup's place: on the east side of the street along the cell's west edge, facing
    // north, so far up from the crossing (a parked car's own place, kept free for it)
    static final float DUTY_ALONG = 29.8f;

    /** Where cell (i, j)'s duty pickup stands: x, z, heading. */
    static void dutyPlace(int i, int j, float[] out) {
        if (i == HOME_I && j == HOME_J) {
            // home's: in its place in the forecourt, beside the door, facing west
            out[0] = HOME_PICKUP_X;
            out[1] = HOME_J * CELL + FRONT + HOME_SET_BACK * 0.5f;
            out[2] = (float) (-Math.PI / 2);
            return;
        }
        out[0] = i * CELL + PARKED;
        out[1] = j * CELL + DUTY_ALONG;
        out[2] = 0f;
    }

    /** A bench, drawn among the small things and kept, for those who sit on it. */
    private static void bench(Cell c, float x, float y, float z, float yaw) {
        Garden.bench(c.detail, x, y, z, yaw);
        if ((c.benchN + 1) * 3 > c.benches.length) c.benches = java.util.Arrays.copyOf(c.benches, c.benches.length * 2);
        c.benches[c.benchN * 3] = x;
        c.benches[c.benchN * 3 + 1] = z;
        c.benches[c.benchN * 3 + 2] = yaw;
        c.benchN++;
    }

    /**
     * The places to sit within reach of (x, z): two to a bench, at most max of them, three floats
     * each into out: where (x, z), and the way one sitting there faces (a heading). How many.
     */
    int seats(float x, float z, float reach, float[] out, int max) {
        int n = 0, ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.benchN && n < max; k++) {
                    float bx = c.benches[k * 3], bz = c.benches[k * 3 + 1], yaw = c.benches[k * 3 + 2];
                    if ((bx - x) * (bx - x) + (bz - z) * (bz - z) > reach * reach) continue;
                    float co = (float) Math.cos(yaw), si = (float) Math.sin(yaw);
                    for (int e = -1; e <= 1 && n < max; e += 2) {
                        out[n * 3] = bx + co * e * 0.42f + si * 0.04f;
                        out[n * 3 + 1] = bz - si * e * 0.42f + co * 0.04f;
                        out[n * 3 + 2] = yaw;
                        n++;
                    }
                }
            }
        return n;
    }

    // ---- the districts: the old town, the park with the water tower ----

    /** The old town: west of home, three blocks by three, from the second row up from the shore. */
    static final int OLD_I0 = -3, OLD_I1 = -1, OLD_J0 = SHORE + 1, OLD_J1 = SHORE + 3;
    /** Its market, with the clock tower: the middle block. */
    static final int MARKET_I = -2, MARKET_J = SHORE + 2;
    /** The park with the water tower, north-east of home. */
    static final int TOWER_I = 3, TOWER_J = SHORE + 6;
    // the old town's lanes: half their width; its houses' walls, warm plaster
    static final float LANE_HALF = 2.4f, POST_GAP = 1.6f, MARKET_HALF = 9f, MARKET_DEEP = 13f;
    private static final int[] OLD_WALLS = {0xE9C9A0, 0xE7B48E, 0xF0D8B0, 0xDDA27E, 0xEBD2A6, 0xD9B48C};
    private int lowRise;

    static boolean oldTown(int i, int j) {
        return i >= OLD_I0 && i <= OLD_I1 && j >= OLD_J0 && j <= OLD_J1;
    }

    /**
     * A block of the old town: four little blocks of low warm houses, packed tight, with lanes
     * between them crossing in the middle, paved in cobbles; posts at the lanes' mouths, too close
     * for a car, wide enough for her. The market block keeps a square where the lanes cross, a
     * market roof on posts and the clock tower over it.
     */
    private void oldTown(Cell c, Random rnd, float x0, float z0, int i, int j) {
        DrawList d = c.boxes;
        float a = FRONT, b = CELL - FRONT, mid = CELL * 0.5f;
        boolean market = i == MARKET_I && j == MARKET_J;
        float open = LANE_HALF;
        // the cobbles of the lanes, darker than the pavements; the market square in lighter stone
        int cobble = 0xA8957A;
        d.box(x0 + mid, WALK_TOP + 0.01f, z0 + mid, (b - a) * 0.5f, 0.01f, open, null, cobble, DrawList.PAVING);
        d.box(x0 + mid, WALK_TOP + 0.012f, z0 + mid, open, 0.01f, (b - a) * 0.5f, null, cobble, DrawList.PAVING);
        if (market) d.box(x0 + mid, WALK_TOP + 0.014f, z0 + mid, MARKET_HALF, 0.01f, MARKET_DEEP, null, 0xD9C9AC, DrawList.PROMENADE);
        // the four little blocks, two rows of houses each, back to back, low; in the market block
        // the inner rows stop short of the square
        lowRise = 3;
        for (int qx = 0; qx < 2; qx++)
            for (int qz = 0; qz < 2; qz++) {
                float u0 = qx == 0 ? a : mid + open, u1 = qx == 0 ? mid - open : b;
                float v0 = qz == 0 ? a : mid + open, v1 = qz == 0 ? mid - open : b;
                float vm = (v0 + v1) * 0.5f + (rnd.nextFloat() - 0.5f) * 2f;
                float w0 = u0, w1 = u1;
                if (market) { if (qx == 0) w1 = mid - MARKET_HALF; else w0 = mid + MARKET_HALF; }
                if (qz == 0) {
                    row(c, rnd, x0 + u0, x0 + u1, z0 + v0, z0 + vm, true, -1, false);
                    if (w1 - w0 > 4f) row(c, rnd, x0 + w0, x0 + w1, z0 + vm, z0 + v1, true, 1, false);
                } else {
                    if (w1 - w0 > 4f) row(c, rnd, x0 + w0, x0 + w1, z0 + v0, z0 + vm, true, -1, false);
                    row(c, rnd, x0 + u0, x0 + u1, z0 + vm, z0 + v1, true, 1, false);
                }
            }
        lowRise = 0;
        // posts across each lane's mouth, three to a mouth: the gaps let her through, never a car
        for (int e = 0; e < 4; e++) {
            float mx = e == 0 ? a : e == 1 ? b : mid, mz = e < 2 ? mid : e == 2 ? a : b;
            for (int s = -1; s <= 1; s++) {
                float px = x0 + mx + (e < 2 ? 0f : s * POST_GAP), pz = z0 + mz + (e < 2 ? s * POST_GAP : 0f);
                d.box(px, WALK_TOP + 0.45f, pz, 0.12f, 0.45f, 0.12f, null, 0x3A3530, DrawList.PLAIN);
                c.wall(px - 0.12f, pz - 0.12f, px + 0.12f, pz + 0.12f, WALK_TOP + 0.9f);
            }
        }
        if (market) marketHall(c, x0 + mid, z0 + mid);
        // a lamp in the lanes' crossing, a bench or two along them
        if (!market) {
            float lx = x0 + mid + LANE_HALF - 0.4f, lz = z0 + mid + LANE_HALF - 0.4f;
            c.detail.box(lx, WALK_TOP + 1.8f, lz, 0.05f, 1.8f, 0.05f, null, 0x2B2F3A, DrawList.PLAIN);
            c.detail.box(lx, WALK_TOP + 3.65f, lz, 0.12f, 0.16f, 0.12f, null, 0xF3E2B4, DrawList.GLOW);
            c.wall(lx - 0.08f, lz - 0.08f, lx + 0.08f, lz + 0.08f, WALK_TOP + 3.8f);
        }
    }

    /** The market: a tiled roof on eight posts over stalls, and the clock tower at its north side. */
    private void marketHall(Cell c, float mx, float mz) {
        DrawList d = c.boxes;
        float hx = 5.5f, hz = 4f, top = WALK_TOP + 3.6f;
        for (int k = 0; k < 8; k++) {
            float px = mx + (k % 4 - 1.5f) * (hx * 2f / 3f), pz = mz + (k < 4 ? -hz : hz);
            d.box(px, WALK_TOP + 1.8f, pz, 0.14f, 1.8f, 0.14f, null, 0x6A4A33, DrawList.WOOD);
            c.wall(px - 0.14f, pz - 0.14f, px + 0.14f, pz + 0.14f, top);
        }
        for (int e = -1; e <= 1; e += 2) {
            float t = 0.42f * e, ct = (float) Math.cos(t), st = (float) Math.sin(t);
            float[] r = {1f, 0f, 0f, 0f, ct, -st, 0f, st, ct};
            d.box(mx, top + 0.6f, mz + e * hz * 0.5f, hx + 0.5f, 0.1f, hz * 0.6f, r, 0xB24A36, DrawList.TILES);
        }
        // the stalls under it: awnings of coral and teal, crates of fruit
        for (int k = 0; k < 3; k++) {
            float sx = mx + (k - 1) * 3.4f;
            c.detail.box(sx, WALK_TOP + 0.45f, mz, 1.2f, 0.45f, 0.6f, null, 0x8A5E3C, DrawList.WOOD);
            c.detail.box(sx, WALK_TOP + 0.95f, mz, 1.1f, 0.06f, 0.5f, null, k % 2 == 0 ? 0xF6B54A : 0xE0644A, DrawList.PLAIN);
            c.wall(sx - 1.2f, mz - 0.6f, sx + 1.2f, mz + 0.6f, WALK_TOP + 1f);
        }
        // the clock tower: stone, a clock on each face, a little tiled cap; seen down the lanes
        float tx = mx, tz = mz + hz + 3.2f, th = 21f;
        d.box(tx, WALK_TOP + th * 0.5f, tz, 1.6f, th * 0.5f, 1.6f, null, 0xE2CFA8, DrawList.STONE);
        d.box(tx, WALK_TOP + th + 1.2f, tz, 1.9f, 1.2f, 1.9f, null, 0xD8C29A, DrawList.STONE);
        for (int e = 0; e < 4; e++) {
            float fx = e == 0 ? 1f : e == 1 ? -1f : 0f, fz = e == 2 ? 1f : e == 3 ? -1f : 0f;
            c.detail.box(tx + fx * 1.92f, WALK_TOP + th + 1.2f, tz + fz * 1.92f, fz != 0f ? 0.75f : 0.02f, 0.75f, fx != 0f ? 0.75f : 0.02f,
                    null, 0xFFF4D6, DrawList.GLOW);
        }
        d.box(tx, WALK_TOP + th + 3.1f, tz, 1.4f, 0.7f, 1.4f, null, 0xB24A36, DrawList.TILES);
        c.wall(tx - 1.6f, tz - 1.6f, tx + 1.6f, tz + 1.6f, WALK_TOP + th + 2.4f);
    }

    /** The water tower in the park: four iron legs braced across, a tank of pale blue on them, a ladder; 30 m to its top. */
    private void waterTower(Cell c, float x, float z) {
        DrawList d = c.boxes;
        float legs = 22f, span = 2.6f;
        for (int k = 0; k < 4; k++) {
            float lx = x + (k % 2 == 0 ? -span : span), lz = z + (k < 2 ? -span : span);
            d.box(lx, WALK_TOP + legs * 0.5f, lz, 0.2f, legs * 0.5f, 0.2f, null, 0x5C6670, DrawList.PLAIN);
            c.wall(lx - 0.2f, lz - 0.2f, lx + 0.2f, lz + 0.2f, WALK_TOP + legs);
        }
        for (float y = 5f; y < legs; y += 5.5f) {
            d.box(x, WALK_TOP + y, z - span, span, 0.08f, 0.08f, null, 0x5C6670, DrawList.PLAIN);
            d.box(x, WALK_TOP + y, z + span, span, 0.08f, 0.08f, null, 0x5C6670, DrawList.PLAIN);
            d.box(x - span, WALK_TOP + y, z, 0.08f, 0.08f, span, null, 0x5C6670, DrawList.PLAIN);
            d.box(x + span, WALK_TOP + y, z, 0.08f, 0.08f, span, null, 0x5C6670, DrawList.PLAIN);
        }
        // the tank: a drum of blue-white, a rim, a cap
        d.box(x, WALK_TOP + legs + 0.15f, z, span + 0.6f, 0.15f, span + 0.6f, null, 0x5C6670, DrawList.PLAIN);
        d.box(x, WALK_TOP + legs + 3.3f, z, span + 0.3f, 3f, span + 0.3f, null, 0xBFD8E2, DrawList.PAINT);
        d.box(x, WALK_TOP + legs + 6.6f, z, span - 0.2f, 0.4f, span - 0.2f, null, 0x8FB2C0, DrawList.PAINT);
        d.box(x, WALK_TOP + legs + 7.4f, z, 0.9f, 0.5f, 0.9f, null, 0x8FB2C0, DrawList.PAINT);
        c.wall(x - span - 0.3f, z - span - 0.3f, x + span + 0.3f, z + span + 0.3f, WALK_TOP + legs + 6.4f, WALK_TOP + legs);
        // the ladder up one leg
        c.detail.box(x - span - 0.35f, WALK_TOP + legs * 0.5f + 1f, z, 0.03f, legs * 0.5f + 1f, 0.3f, null, 0x2B2F3A, DrawList.RAIL);
    }

    // ---- the country past the town ----

    /**
     * A cell of the country: in the first row, the town's last street along its south edge; then
     * grass, the road out where it runs, trees about the fields, low stone walls; in the ridge's
     * row, hills shoulder to shoulder with the pass cut through them for the road; in the village's
     * rows, its houses round a square.
     */
    private void country(Cell c, int i, int j) {
        DrawList d = c.boxes;
        Random rnd = new Random(hash(i, j, 11));
        float x0 = i * CELL, z0 = j * CELL, half = CELL * 0.5f;
        int grass = 0x7FA052, asphalt = 0x5E4844;
        float g0 = z0;
        if (j == EDGE_J) {
            // the town's last street, its crossing, a curb and a low wall along its north side
            d.box(x0, -0.25f, z0, ROAD, 0.25f, ROAD, null, asphalt, DrawList.ASPHALT);
            d.box(x0 + half, -0.25f, z0, half - ROAD, 0.25f, ROAD, null, asphalt, DrawList.ROAD);
            float cz = z0 + ROAD + (CURB - ROAD) * 0.5f;
            d.box(x0 + half, (CURB_TOP - 0.25f) * 0.5f, cz, half, (CURB_TOP + 0.25f) * 0.5f, (CURB - ROAD) * 0.5f, null, 0xC9BFAE, DrawList.STONE);
            g0 = z0 + CURB;
            float wz = z0 + CURB + 1.2f;
            // the wall, broken where the road out goes through, and where the road up the mountain does
            float r0 = routeX(wz) - ROUTE_HALF - 1.5f, r1 = routeX(wz) + ROUTE_HALF + 1.5f;
            float m0 = Mountain.MX - Mountain.HALF - 1.5f, m1 = Mountain.MX + Mountain.HALF + 1.5f;
            float[] cut = r0 < m0 ? new float[]{r0, r1, m0, m1} : new float[]{m0, m1, r0, r1};
            float a = x0 + ROAD + 0.5f;
            for (int k = 0; k <= 2; k++) {
                float b = k < 2 ? cut[k * 2] : x0 + CELL - ROAD - 0.5f;
                float aa = Math.max(a, x0 + 0.5f), bb = Math.min(b, x0 + CELL - 0.5f);
                if (bb - aa >= 0.5f) {
                    d.box((aa + bb) * 0.5f, 0.45f, wz, (bb - aa) * 0.5f, 0.45f, 0.3f, null, 0xCBB89A, DrawList.STONE);
                    c.wall(aa, wz - 0.3f, bb, wz + 0.3f, 0.9f);
                }
                if (k < 2) a = Math.max(a, cut[k * 2 + 1]);
            }
        }
        // the grass, a little under the road
        d.box(x0 + half, -0.3f, (g0 + z0 + CELL) * 0.5f, half, 0.28f, (z0 + CELL - g0) * 0.5f, null, grass, DrawList.GRASS);
        // the road out, in short straight pieces along its curve, a dashed line down its middle
        if (z0 + CELL > EDGE_J * CELL + ROAD && z0 < VILLAGE_Z) {
            float from = Math.max(z0, EDGE_J * CELL + ROAD), to = Math.min(z0 + CELL, VILLAGE_Z - SQUARE_HALF);
            for (float z = from; z < to; z += 4f) {
                float za = z, zb = Math.min(to, z + 4f), xa = routeX(za), xb = routeX(zb);
                if (Math.abs((xa + xb) * 0.5f - x0 - half) > half + ROUTE_HALF + 4f) continue;
                float dx = xb - xa, dz = zb - za, len = (float) Math.sqrt(dx * dx + dz * dz);
                float[] f = yawFrame((float) Math.atan2(dx, dz));
                d.box((xa + xb) * 0.5f, -0.12f, (za + zb) * 0.5f, ROUTE_HALF, 0.12f, len * 0.5f + 0.06f, f, asphalt, DrawList.ASPHALT);
                if (((int) (z / 4f)) % 2 == 0)
                    c.detail.box((xa + xb) * 0.5f, 0.005f, (za + zb) * 0.5f, 0.07f, 0.01f, len * 0.3f, f, 0xF2EEDF, DrawList.PAINT);
                // the verges: pale gravel either side
                for (int e = -1; e <= 1; e += 2) {
                    float ox = (float) Math.cos(Math.atan2(dx, dz)) * e * (ROUTE_HALF + 0.6f), oz = -(float) Math.sin(Math.atan2(dx, dz)) * e * (ROUTE_HALF + 0.6f);
                    d.box((xa + xb) * 0.5f + ox, -0.1f, (za + zb) * 0.5f + oz, 0.6f, 0.1f, len * 0.5f + 0.06f, f, 0xCDBB98, DrawList.GRAVEL);
                }
            }
        }
        if (j == RIDGE_J) ridge(c, rnd, x0, z0);
        // past the ridge the walls of the pass go on along the road a way, so that it reads as a cut
        if (j == RIDGE_J + 1) cliffs(c, x0, (RIDGE_J + 1) * CELL - 5f, (RIDGE_J + 1) * CELL + CLIFF_ON, 1);
        else if (j == EDGE_J + 7 || j == EDGE_J + 8) village(c, rnd, x0, z0, j);
        else fields(c, rnd, x0, z0, j);
        if (inQuarry(x0 + CELL * 0.5f, z0 + CELL * 0.5f, CELL * 0.5f)) quarry(c, x0, z0);
        // the rails along the road up the mountain, where they stand in this cell
        Mountain.walls(c, i, j);
    }

    /** A turn about the up axis, row by row (a fresh array: boxes keep their own). */
    private static float[] yawFrame(float a) {
        float cy = (float) Math.cos(a), sy = (float) Math.sin(a);
        return new float[]{cy, 0f, sy, 0f, 1f, 0f, -sy, 0f, cy};
    }

    /** Fields: trees here and there, a boulder now and then; never on the road. */
    private void fields(Cell c, Random rnd, float x0, float z0, int j) {
        int trees = 5 + rnd.nextInt(6);
        for (int k = 0; k < trees; k++) {
            float tx = x0 + 4f + rnd.nextFloat() * (CELL - 8f), tz = z0 + 6f + rnd.nextFloat() * (CELL - 12f);
            float h = 6.5f + rnd.nextFloat() * 3.5f;
            if (onRoute(tx, tz, 6f) || j == RIDGE_J + 1 && tz < z0 + CLIFF_ON + 3f && Math.abs(tx - routeX(tz)) < CLIFF_REACH + 3f || j == EDGE_J && tz < z0 + 10f || inQuarry(tx, tz, 3f) || Mountain.near(tx, tz)) continue;
            // on the ridge, up its slope where it stands
            tree(c, c.detail, rnd, tx, ridge(tx, tz) - 0.1f, tz, h);
            c.wall(tx - 0.2f, tz - 0.2f, tx + 0.2f, tz + 0.2f);
        }
        if (rnd.nextInt(2) == 0) {
            float bx = x0 + 6f + rnd.nextFloat() * (CELL - 12f), bz = z0 + 8f + rnd.nextFloat() * (CELL - 16f);
            float r = 0.8f + rnd.nextFloat() * 1.2f;
            if (!onRoute(bx, bz, 4f) && !(j == EDGE_J && bz < z0 + 10f) && !inQuarry(bx, bz, 3f) && !Mountain.near(bx, bz) && ridge(bx, bz) < 0.2f) {
                c.detail.box(bx, r * 0.55f, bz, r, r * 0.6f, r * 0.85f, yawFrame(rnd.nextFloat() * 3f), 0x9C968A, DrawList.STONE);
                c.wall(bx - r * 0.8f, bz - r * 0.7f, bx + r * 0.8f, bz + r * 0.7f, r * 1.1f);
            }
        }
    }

    // ---- the ridge: one long hill across the country, its crest wandering a little, rising and
    // falling as it goes; a slope she walks up and over, as on the mountain; falling away to
    // nothing by the pass, where the cliffs stand, and by the mountain ----
    /** How far the ridge's slopes reach from its crest, north and south; its tallest. */
    static final float RIDGE_DEPTH = CELL * 0.5f - 5f, RIDGE_MOST = 24f;
    // how far from the road the slopes begin to rise beside the cliffs, and over how far they rise
    // to their height; the same by the mountain
    static final float RIDGE_FROM_PASS = 36f, RIDGE_RISE = 12f, RIDGE_FROM_MOUNTAIN = 25f;
    // the slices it is drawn in across, and the pieces down each slice, crest to foot
    static final float RIDGE_SLICE = 4f;
    static final int RIDGE_PIECES = 12;

    /** Where the ridge's crest is at x, z. */
    static float ridgeMid(float x) {
        return RIDGE_J * CELL + CELL * 0.5f + (float) Math.sin(x * 0.05f) * 4f;
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    /** How tall the ridge's crest is at x: as the hills were, low by the pass and the mountain. */
    static float ridgeTall(float x) {
        float h = Math.min(RIDGE_MOST, 10f + (float) Math.abs(Math.sin(x * 0.037f) * 7f + Math.sin(x * 0.013f + 1f) * 5f));
        h *= smooth((Math.abs(x - routeX(PASS_Z)) - RIDGE_FROM_PASS) / RIDGE_RISE);
        float mx = x - Mountain.MX, mz = ridgeMid(x) - Mountain.MZ;
        h *= smooth(((float) Math.sqrt(mx * mx + mz * mz) - Mountain.CLEAR) / RIDGE_FROM_MOUNTAIN);
        return h;
    }

    /** How high the ridge's slope is at (x, z): 0 off it. */
    static float ridge(float x, float z) {
        float t = (z - ridgeMid(x)) / RIDGE_DEPTH;
        if (t <= -1f || t >= 1f) return 0f;
        float h = ridgeTall(x);
        return h <= 0f ? 0f : h * (0.5f + 0.5f * (float) Math.cos(Math.PI * t));
    }

    /** Whether a pickup going from (ox, oz) to (x, z) would drive up the ridge's slope: it does not. */
    static boolean ridgeHolds(float ox, float oz, float x, float z) {
        float h = ridge(x, z);
        return h > Mountain.CLIMB && h > ridge(ox, oz) + 0.02f;
    }

    /**
     * The ridge in this cell: slices across it, each pieces of slope from foot to crest and down
     * again, rock low down, grass above, a lighter green high up; the cliffs of the pass by the road.
     */
    private void ridge(Cell c, Random rnd, float x0, float z0) {
        DrawList d = c.boxes;
        float mid = z0 + CELL * 0.5f, depth = RIDGE_DEPTH;
        for (float sx = x0 + RIDGE_SLICE * 0.5f; sx < x0 + CELL; sx += RIDGE_SLICE) {
            float tall = ridgeTall(sx);
            if (tall < 0.3f) continue;
            float zc = ridgeMid(sx);
            int green = Street.mix(0x6E9447, 0x86A85A, (float) Math.abs(Math.sin(sx * 0.07f)));
            for (int k = 0; k < RIDGE_PIECES; k++) {
                float za = zc - depth + 2f * depth * k / RIDGE_PIECES, zb = zc - depth + 2f * depth * (k + 1) / RIDGE_PIECES;
                float ha = ridge(sx, za), hb = ridge(sx, zb);
                float dz = zb - za, dh = hb - ha, len = (float) Math.sqrt(dz * dz + dh * dh);
                float cs = dz / len, sn = dh / len, thick = 0.4f;
                // its x across, its z up or down the slope, its y out of it
                float[] turn = {1f, 0f, 0f, 0f, cs, sn, 0f, -sn, cs};
                float cy = (ha + hb) * 0.5f - cs * thick, cz = (za + zb) * 0.5f + sn * thick;
                float hm = (ha + hb) * 0.5f;
                int color = hm < 2.4f ? 0x9A8F80 : hm > tall * 0.8f ? Street.scale(green, 1.08f) : Street.scale(green, 0.94f + 0.04f * (k % 3));
                d.box(sx, cy, cz, RIDGE_SLICE * 0.5f + 0.02f, thick, len * 0.5f + 0.05f, turn, color,
                        hm < 2.4f ? DrawList.STONE : DrawList.GRASS);
            }
        }
        cliffs(c, x0, mid - depth, mid + depth, 0);
    }

    // the cliffs either side of the pass: how far out from the road they reach at the foot, how
    // many slices along the road, how far each tier stands back from the one under it
    static final float CLIFF_REACH = 44f, CLIFF_STEP = 0.6f;
    /** How far from the road's middle the cliffs' foot stands: just past its gravel. */
    static final float CLIFF_FOOT = ROUTE_HALF + 1.6f;
    static final int CLIFF_SLICES = 9;
    /** How far past the ridge's row the pass's walls go on. */
    static final float CLIFF_ON = 42f;

    /**
     * The pass between two walls of rock: on each side of the road slices of cliff along it, each
     * a pillar of tiers standing back a little as they go up, grass on the ledges, a fir here and
     * there; the slices of unlike height and depth, so that the faces break into columns. On the
     * east face the pale scar of the slope that came down. Only what falls in this cell is drawn.
     */
    private void cliffs(Cell c, float x0, float from, float to, int salt) {
        DrawList d = c.boxes, small = c.detail;
        float len = (to - from) / CLIFF_SLICES;
        float x1 = x0 + CELL;
        for (int e = -1; e <= 1; e += 2) {
            for (int s = 0; s < CLIFF_SLICES; s++) {
                Random rnd = new Random(0xC11FFL + s * 7919L + e * 104729L + salt * 31L);
                float za = from + s * len, zb = za + len;
                // the ends ragged: the first and last slices short of the ridge's edge
                if (s == 0 && salt == 0) za += rnd.nextFloat() * 3f;
                if (s == CLIFF_SLICES - 1) zb -= rnd.nextFloat() * 3f;
                float rx = routeX((za + zb) * 0.5f);
                float face = CLIFF_FOOT + rnd.nextFloat() * 1.1f + (s == 0 || s == CLIFF_SLICES - 1 ? 1.5f : 0f);
                int tiers = 4 + rnd.nextInt(2);
                float y = 0f, top = 0f;
                for (int k = 0; k < tiers; k++) {
                    float th = k == 0 ? 6f + rnd.nextFloat() * 3f : 7f + rnd.nextFloat() * 5f;
                    float in = face + k * (CLIFF_STEP + rnd.nextFloat() * 0.9f);
                    float out = CLIFF_REACH - k * 6f;
                    // at the ends of the pass the tiers step back from the ridge's front too
                    float ka = za + (s == 0 ? k * 1.8f : 0f), kb = zb - (s == CLIFF_SLICES - 1 ? k * 1.8f : 0f);
                    float a = rx + e * in, b = rx + e * out;
                    float lo = Math.max(Math.min(a, b), x0), hi = Math.min(Math.max(a, b), x1);
                    int rock = Street.mix(0x928879, 0xB2A794, rnd.nextFloat());
                    if (hi - lo > 0.2f) {
                        d.box((lo + hi) * 0.5f, y + th * 0.5f, (ka + kb) * 0.5f, (hi - lo) * 0.5f, th * 0.5f, (kb - ka) * 0.5f, null,
                                rock, DrawList.STONE);
                        // a ledge of grass along the face, where the next tier stands back
                        float lip = 0.9f + rnd.nextFloat() * 0.8f;
                        float ga = rx + e * in, gb = rx + e * (in + lip + (k == tiers - 1 ? out - in : 0f));
                        float glo = Math.max(Math.min(ga, gb), x0), ghi = Math.min(Math.max(ga, gb), x1);
                        if (ghi - glo > 0.1f)
                            d.box((glo + ghi) * 0.5f, y + th + 0.12f, (ka + kb) * 0.5f, (ghi - glo) * 0.5f, 0.14f, (kb - ka) * 0.5f - 0.1f, null,
                                    Street.mix(0x7FA048, 0x9DB955, rnd.nextFloat()), DrawList.GRASS);
                        // now and then a fir on the ledge
                        float fx = rx + e * (in + lip * 0.5f), fz = ka + 1f + rnd.nextFloat() * (kb - ka - 2f);
                        if (k > 0 && rnd.nextInt(3) == 0 && fx > x0 && fx < x1)
                            fir(small, rnd, fx, y + th + 0.2f, fz, 2.6f + rnd.nextFloat() * 2.2f);
                    }
                    y += th;
                    top = y;
                }
                float wa = Math.max(Math.min(rx + e * face, rx + e * CLIFF_REACH), x0), wb = Math.min(Math.max(rx + e * face, rx + e * CLIFF_REACH), x1);
                if (wb - wa > 0.2f) c.wall(wa, za, wb, zb, top);
                // the scar: where the slope came down, pale earth down the face to the road
                if (salt == 0 && e > 0 && (s == 3 || s == 4)) {
                    float sx = rx + face - 0.25f;
                    if (sx > x0 && sx < x1)
                        for (int k = 0; k < 4; k++) {
                            float sy = 1f + k * 6f, sw = len * (0.48f - k * 0.08f);
                            d.box(sx + k * 0.9f, sy + 3f, (za + zb) * 0.5f + (s == 3 ? 0.6f : -0.6f) * len * 0.3f, 0.3f, 3.2f, sw, null,
                                    Street.mix(0xC9935A, 0xDDA86A, k * 0.25f), DrawList.GRAVEL);
                        }
                }
            }
        }
    }

    /** A fir: a short trunk, three cones of dark needles one over another, smaller as they go up. */
    private static void fir(DrawList d, Random rnd, float x, float y, float z, float height) {
        d.box(x, y + height * 0.12f, z, 0.14f, height * 0.12f, 0.14f, null, 0x5A4232, DrawList.TRUNK);
        int g = Street.mix(0x2F5A33, 0x3E6E3C, rnd.nextFloat());
        for (int k = 0; k < 3; k++) {
            float r = height * (0.26f - k * 0.065f), hy = height * 0.16f;
            float cy = y + height * (0.36f + k * 0.22f);
            float a = rnd.nextFloat() * 1.5f;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            d.box(x, cy, z, r, hy, r, new float[]{ca, 0f, sa, 0f, 1f, 0f, -sa, 0f, ca}, Street.scale(g, 1f + k * 0.06f), DrawList.LEAVES);
        }
    }

    /**
     * The village: a square of beaten earth where the road ends, a well in it, little houses of
     * two storeys round it under red tile roofs, gardens behind low walls, a tree or two.
     */
    private void village(Cell c, Random rnd, float x0, float z0, int j) {
        DrawList d = c.boxes;
        float sx = villageX(), sz = VILLAGE_Z;
        boolean hasSquare = sx + SQUARE_HALF > x0 && sx - SQUARE_HALF < x0 + CELL && sz + SQUARE_HALF > z0 && sz - SQUARE_HALF < z0 + CELL;
        if (hasSquare) {
            float a = Math.max(x0, sx - SQUARE_HALF), b = Math.min(x0 + CELL, sx + SQUARE_HALF);
            float p = Math.max(z0, sz - SQUARE_HALF), q = Math.min(z0 + CELL, sz + SQUARE_HALF);
            d.box((a + b) * 0.5f, -0.1f, (p + q) * 0.5f, (b - a) * 0.5f, 0.1f, (q - p) * 0.5f, null, 0xC9AE86, DrawList.GRAVEL);
            if (sx > x0 && sx < x0 + CELL && sz > z0 && sz < z0 + CELL) {
                // the well
                d.box(sx, 0.45f, sz, 0.9f, 0.45f, 0.9f, null, 0xB8AA92, DrawList.STONE);
                d.box(sx, 1.6f, sz, 0.08f, 0.7f, 0.9f, null, 0x6A4A33, DrawList.WOOD);
                d.box(sx, 2.35f, sz, 0.7f, 0.08f, 1.05f, null, 0xA8463A, DrawList.TILES);
                c.wall(sx - 0.9f, sz - 0.9f, sx + 0.9f, sz + 0.9f, 0.9f);
            }
        }
        // houses round the square, in a ring at its edge, facing it; and a few more along the road in
        int[] walls = {0xF0E2C8, 0xEFC9B4, 0xE9D9A8, 0xDCCDB8, 0xF2D9C4};
        for (int k = 0; k < 12; k++) {
            float ang = k * 6.2832f / 12f + 0.13f;
            float hx = sx + (float) Math.sin(ang) * (SQUARE_HALF + 7f), hz = sz + (float) Math.cos(ang) * (SQUARE_HALF + 7f);
            // the road comes in from the south: no house across it
            if (Math.abs(ang - (float) Math.PI) < 0.45f) continue;
            if (hx < x0 || hx >= x0 + CELL || hz < z0 || hz >= z0 + CELL) continue;
            Random own = new Random(hash(Math.round(hx), Math.round(hz), 12));
            house(c, own, hx, hz, ang + (float) Math.PI, walls[own.nextInt(walls.length)]);
        }
        for (int k = 0; k < 6; k++) {
            float hz = sz - SQUARE_HALF - 14f - k * 13f;
            for (int e = -1; e <= 1; e += 2) {
                float hx = routeX(hz) + e * (ROUTE_HALF + 7.5f);
                if (hx < x0 || hx >= x0 + CELL || hz < z0 || hz >= z0 + CELL) continue;
                Random own = new Random(hash(Math.round(hx), Math.round(hz), 13));
                if (own.nextInt(3) == 0) continue;
                house(c, own, hx, hz, e > 0 ? (float) (-Math.PI / 2) : (float) (Math.PI / 2), walls[own.nextInt(walls.length)]);
            }
        }
        fields(c, rnd, x0, z0, j);
    }

    /** A village house at (x, z), its front facing the way yaw: two storeys, a tile roof, a door, windows. */
    private void house(Cell c, Random rnd, float x, float z, float yaw, int wall) {
        DrawList d = c.boxes, small = c.detail;
        float w = 3.2f + rnd.nextFloat() * 1.2f, dp = 3f + rnd.nextFloat() * 0.8f, h = 5.6f + rnd.nextFloat() * 1.2f;
        float[] f = yawFrame(yaw);
        d.box(x, h * 0.5f, z, w, h * 0.5f, dp, f, wall, DrawList.PAINT);
        // the roof: two slopes of red tile meeting along the ridge
        for (int e = -1; e <= 1; e += 2) {
            float tilt = e * 0.55f, cy = h + 0.9f;
            float[] t = yawFrame(yaw);
            float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
            // tipped about the house's own x (across its front): its slopes run front and back
            float[] r = {t[0], t[1] * ct + t[2] * st, -t[1] * st + t[2] * ct, t[3], t[4] * ct + t[5] * st, -t[4] * st + t[5] * ct,
                    t[6], t[7] * ct + t[8] * st, -t[7] * st + t[8] * ct};
            float off = e * dp * 0.5f;
            d.box(x + f[2] * off, cy, z + f[8] * off, w + 0.3f, 0.12f, dp * 0.62f, r, 0xB24A36, DrawList.TILES);
        }
        // the door and two windows on the front, a window above
        float fx = f[2], fz = f[8], ax = f[0], az = f[6];
        float front = dp + 0.03f;
        small.box(x + fx * front, 1.1f, z + fz * front, 0.55f, 1.1f, 0.03f, f, 0x5A3B2A, DrawList.WOOD);
        for (int e = -1; e <= 1; e += 2) {
            small.box(x + fx * front + ax * e * w * 0.55f, 1.6f, z + fz * front + az * e * w * 0.55f, 0.45f, 0.55f, 0.03f, f, 0x3E5560, DrawList.GLASS);
            small.box(x + fx * front + ax * e * w * 0.45f, 4.3f, z + fz * front + az * e * w * 0.45f, 0.45f, 0.55f, 0.03f, f, 0x3E5560, DrawList.GLASS);
        }
        // what she cannot go through: the house, as the box round it
        float ex = Math.abs(f[0]) * w + Math.abs(f[2]) * dp, ez = Math.abs(f[6]) * w + Math.abs(f[8]) * dp;
        c.wall(x - ex, z - ez, x + ex, z + ez, h + 1.8f);
    }

    // ---- the crowns of the trees and palms, which the eye keeps out of ----

    private static void crown(Cell c, float x, float y, float z, float r) {
        if ((c.crownN + 1) * 4 > c.crowns.length) c.crowns = java.util.Arrays.copyOf(c.crowns, c.crowns.length * 2);
        int o = c.crownN++ * 4;
        c.crowns[o] = x;
        c.crowns[o + 1] = y;
        c.crowns[o + 2] = z;
        c.crowns[o + 3] = r;
    }

    /** A tree, its crown kept. */
    private static void tree(Cell c, DrawList d, Random rnd, float x, float y, float z, float height) {
        Garden.tree(d, rnd, x, y, z, height);
        crown(c, x, y + height * 0.66f, z, height * 0.33f);
    }

    /** A palm, its crown kept. */
    private static void palm(Cell c, Random rnd, float x, float y0, float z, float height, float lean, boolean far) {
        Street.palm(c.detail, rnd, x, y0, z, height, lean, far);
        crown(c, x + lean, y0 + height - 0.6f, z, 2.6f);
    }

    /**
     * How far along the way from (ax, ay, az) to (bx, by, bz) it first goes into the crown of a
     * tree or a palm, 0 to 1 (1: it does not); the crown's bottom into low[0], if it does.
     */
    float leavesOn(float ax, float ay, float az, float bx, float by, float bz, float[] low) {
        float best = 1f;
        float dx = bx - ax, dy = by - ay, dz = bz - az, ll = dx * dx + dy * dy + dz * dz;
        if (ll < 1e-6f) return 1f;
        int i0 = index(Math.min(ax, bx)), i1 = index(Math.max(ax, bx)), j0 = index(Math.min(az, bz)), j1 = index(Math.max(az, bz));
        for (int j = j0; j <= j1; j++)
            for (int i = i0; i <= i1; i++) {
                Cell c = cells.get(key(i, j));
                if (c == null) continue;
                for (int k = 0; k < c.crownN; k++) {
                    int o = k * 4;
                    float cx = c.crowns[o], cy = c.crowns[o + 1], cz = c.crowns[o + 2], r = c.crowns[o + 3];
                    // where the line comes nearest the crown's middle, and whether it passes within it
                    float t = ((cx - ax) * dx + (cy - ay) * dy + (cz - az) * dz) / ll;
                    float ex = ax + dx * t - cx, ey = ay + dy * t - cy, ez = az + dz * t - cz;
                    float miss = ex * ex + ey * ey + ez * ez;
                    if (miss >= r * r) continue;
                    float in = t - (float) Math.sqrt((r * r - miss) / ll);
                    if (in < best && t > 0f) {
                        best = Math.max(0f, in);
                        low[0] = cy - r;
                    }
                }
            }
        return best;
    }

    // ---- the parked cars, rammed off the curb by the pickup she drives ----
    private final java.util.HashSet<Long> moved = new java.util.HashSet<Long>();

    private static void keepParked(Cell c, float x, float z, float yaw, int kind, int paint) {
        if ((c.parkedN + 1) * 5 > c.parkedCars.length) c.parkedCars = java.util.Arrays.copyOf(c.parkedCars, c.parkedCars.length * 2);
        int o = c.parkedN++ * 5;
        c.parkedCars[o] = x;
        c.parkedCars[o + 1] = z;
        c.parkedCars[o + 2] = yaw;
        c.parkedCars[o + 3] = kind;
        c.parkedCars[o + 4] = Float.intBitsToFloat(paint);
    }

    /**
     * The parked car a round body of radius r at (x, z) runs into, taken off the curb (its cell
     * built again without it): where it stood, its heading, kind and paint into out. False if none.
     */
    boolean unpark(float x, float z, float r, float[] out) {
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cells.get(key(i, j));
                if (c == null) continue;
                for (int k = 0; k < c.parkedN; k++) {
                    int o = k * 5;
                    float px = c.parkedCars[o], pz = c.parkedCars[o + 1], yaw = c.parkedCars[o + 2];
                    int kind = (int) c.parkedCars[o + 3];
                    float fx = (float) Math.sin(yaw), fz = (float) Math.cos(yaw), ox = x - px, oz = z - pz;
                    if (Math.abs(ox * fx + oz * fz) > Cars.half(kind) + r || Math.abs(ox * fz - oz * fx) > 0.95f + r) continue;
                    moved.add(lampKey(px, pz));
                    cells.remove(key(i, j));
                    out[0] = px;
                    out[1] = pz;
                    out[2] = yaw;
                    out[3] = kind;
                    out[4] = c.parkedCars[o + 4];
                    return true;
                }
            }
        return false;
    }

    /** The parked car taken from (x, z) put back at its curb (she is far off). */
    void repark(float x, float z) {
        if (moved.remove(lampKey(x, z))) cells.remove(key(index(x), index(z)));
    }

    // ---- the street lamps, felled by the pickup she drives ----
    private final java.util.HashSet<Long> felled = new java.util.HashSet<Long>();

    private static long lampKey(float x, float z) {
        return key(Math.round(x * 4f), Math.round(z * 4f));
    }

    /**
     * The lamp standing within reach of (x, z), felled: gone from the town (its cell built again
     * without it), its place into out (x, z). False if none stands there.
     */
    boolean fell(float x, float z, float reach, float[] out) {
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cells.get(key(i, j));
                if (c == null) continue;
                for (int k = 0; k < c.lampN; k++) {
                    float lx = c.lamps[k * 2], lz = c.lamps[k * 2 + 1];
                    if ((lx - x) * (lx - x) + (lz - z) * (lz - z) > reach * reach) continue;
                    felled.add(lampKey(lx, lz));
                    cells.remove(key(i, j));
                    out[0] = lx;
                    out[1] = lz;
                    return true;
                }
            }
        return false;
    }

    /** The lamp felled at (x, z) put up again (she is far off): its cell built again with it. */
    void raise(float x, float z) {
        if (felled.remove(lampKey(x, z))) cells.remove(key(index(x), index(z)));
    }

    /** The loungers on the beach within reach of (x, z): two floats each into out (where), at most max. How many. */
    int loungers(float x, float z, float reach, float[] out, int max) {
        int n = 0, ci = index(x);
        for (int i = ci - 2; i <= ci + 2; i++) {
            Cell c = cell(i, SHORE - 1);
            for (int k = 0; k < c.loungerN && n < max; k++) {
                float lx = c.loungers[k * 2], lz = c.loungers[k * 2 + 1];
                if ((lx - x) * (lx - x) + (lz - z) * (lz - z) > reach * reach) continue;
                out[n * 2] = lx;
                out[n * 2 + 1] = lz;
                n++;
            }
        }
        return n;
    }

    /** Whether one of the cell's streets has trees down it (by its salt), one in two. */
    private boolean leafy(int i, int j, int salt) {
        return (hash(i, j, salt) & 3) < 2;
    }

    /** Whether a square is a garden within an iron fence rather than paving round a fountain. */
    boolean garden(int i, int j) {
        if (i == TOWER_I && j == TOWER_J) return true;
        if (i == OLD_I - 1 && j == OLD_J) return false;
        if (i == PARK_I && j == PARK_J) return true;
        return (hash(i, j, 5) & 1) == 0;
    }

    /** A tree in the pavement at (x, z), in a grating of iron. */
    private static void street(Cell c, Random rnd, float x, float z) {
        c.detail.box(x, WALK_TOP + 0.005f, z, 0.6f, 0.01f, 0.6f, null, 0x3A3530, DrawList.RAIL);
        tree(c, c.detail, rnd, x, WALK_TOP, z, 7f + rnd.nextFloat() * 2.5f);
        c.wall(x - 0.2f, z - 0.2f, x + 0.2f, z + 0.2f);
    }

    /** Benches along the curbs of the block, between the lamp posts, looking out at the street. */
    private void benches(Cell c, float x0, float z0) {
        float in = CURB + 0.85f;
        for (float t = 23f; t < CELL - 8f; t += 18f) {
            bench(c, x0 + in, WALK_TOP, z0 + t, (float) (-Math.PI / 2));
            c.wall(x0 + in - 0.3f, z0 + t - 0.9f, x0 + in + 0.3f, z0 + t + 0.9f, WALK_TOP + 0.48f);
            bench(c, x0 + t, WALK_TOP, z0 + in, (float) Math.PI);
            c.wall(x0 + t - 0.9f, z0 + in - 0.3f, x0 + t + 0.9f, z0 + in + 0.3f, WALK_TOP + 0.48f);
        }
    }

    private Cell build(int i, int j) {
        Cell c = buildCell(i, j);
        // what the mountain and its cable way stand solid with, wherever it falls
        Mountain.solids(c, i, j);
        Island.solids(c, i, j);
        return c;
    }

    private Cell buildCell(int i, int j) {
        Cell c = new Cell();
        if (j == SHORE - 1) promenade(c, i * CELL, j * CELL);
        if (sea(j)) return c;
        if (country(j)) {
            country(c, i, j);
            return c;
        }
        DrawList d = c.boxes;
        Random rnd = new Random(hash(i, j, 0));
        float x0 = i * CELL, z0 = j * CELL, half = CELL * 0.5f;
        // the crossing, then the two streets out of it, north and east
        d.box(x0, -0.25f, z0, ROAD, 0.25f, ROAD, null, 0x5E4844, DrawList.ASPHALT);
        d.box(x0, -0.25f, z0 + half, ROAD, 0.25f, half - ROAD, null, 0x5E4844, DrawList.ROAD);
        d.box(x0 + half, -0.25f, z0, half - ROAD, 0.25f, ROAD, null, 0x5E4844, DrawList.ROAD);
        // the block: curbs round it, the pavement within
        float b0x = x0 + ROAD, b1x = x0 + CELL - ROAD, b0z = z0 + ROAD, b1z = z0 + CELL - ROAD;
        float cw = CURB - ROAD, cy = (CURB_TOP - 0.25f) * 0.5f, ch = (CURB_TOP + 0.25f) * 0.5f;
        int curb = 0xC9BFAE;
        d.box((b0x + b1x) * 0.5f, cy, b0z + cw * 0.5f, (b1x - b0x) * 0.5f, ch, cw * 0.5f, null, curb, DrawList.STONE);
        d.box((b0x + b1x) * 0.5f, cy, b1z - cw * 0.5f, (b1x - b0x) * 0.5f, ch, cw * 0.5f, null, curb, DrawList.STONE);
        d.box(b0x + cw * 0.5f, cy, (b0z + b1z) * 0.5f, cw * 0.5f, ch, (b1z - b0z) * 0.5f - cw, null, curb, DrawList.STONE);
        d.box(b1x - cw * 0.5f, cy, (b0z + b1z) * 0.5f, cw * 0.5f, ch, (b1z - b0z) * 0.5f - cw, null, curb, DrawList.STONE);
        boolean sq = square(i, j);
        float p0x = b0x + cw, p1x = b1x - cw, p0z = b0z + cw, p1z = b1z - cw;
        d.box((p0x + p1x) * 0.5f, (WALK_TOP - 0.25f) * 0.5f, (p0z + p1z) * 0.5f, (p1x - p0x) * 0.5f, (WALK_TOP + 0.25f) * 0.5f,
                (p1z - p0z) * 0.5f, null, sq ? 0xDCCDB2 : 0xCDBFA6, sq ? DrawList.PROMENADE : DrawList.PAVING);
        // what furnishes the streets and squares has its own seed, so the houses stay as they were
        Random deco = new Random(hash(i, j, 6));
        if (oldTown(i, j)) oldTown(c, rnd, x0, z0, i, j);
        else if (sq) plaza(c, rnd, deco, x0, z0, garden(i, j));
        else block(c, rnd, x0, z0, i, j);
        // in the park's north-east quarter, off its fountain
        if (i == TOWER_I && j == TOWER_J) waterTower(c, x0 + CELL * 0.5f + 12f, z0 + CELL * 0.5f + 12f);
        lamps(c, x0, z0);
        benches(c, x0, z0);
        parked(c, rnd, x0, z0, seeded(i, j));
        if (!palmy(i, j) && leafy(i, j, 3)) {
            // plane trees down the street along the west edge, on both its pavements
            for (float t = 20f; t < CELL - 10f; t += 26f)
                for (int s = -1; s <= 1; s += 2) street(c, deco, x0 + s * (CURB + 0.9f), z0 + t);
        }
        if (leafy(i, j, 4)) {
            // and down the one along the south edge
            for (float t = 20f; t < CELL - 10f; t += 26f)
                for (int s = -1; s <= 1; s += 2) street(c, deco, x0 + t, z0 + s * (CURB + 0.9f));
        }
        if (palmy(i, j)) {
            // down the street along the west edge, on both its pavements
            for (float z = z0 + 10f; z < z0 + CELL - 6f; z += 12f)
                for (int s = -1; s <= 1; s += 2) {
                    float px = x0 + s * (CURB + 1.1f);
                    palm(c, rnd, px, WALK_TOP, z, 6.5f + rnd.nextFloat() * 2f, (rnd.nextFloat() - 0.5f) * 0.3f, false);
                    c.wall(px - 0.25f, z - 0.25f, px + 0.25f, z + 0.25f);
                }
        }
        // the zebras on its two streets, and the lights at its crossing if it has them
        Crossings.build(this, c, i, j);
        return c;
    }

    /**
     * The promenade along the shore road, a cell of it: the curb, the big pale stones out to the
     * parapet, the parapet on the stone face of the quay; a flight of steps down through a gap in it
     * to the beach, sand and then wet sand out to the water, a sunshade or two with its loungers;
     * two rows of palms, lamps along the parapet, benches by it looking out to sea.
     */
    // the rocks at the water's edge: along the cell from its west street, the big rock's middle
    // out from the shore road; each rock across, out, its half sizes, its top, its turn
    static final float ROCKS_AT = 48f, ROCKS_Z = WATERLINE - 1.5f;
    private static final float[][] ROCKS = {
        {0f, 0f, 1.4f, 1.2f, -0.2f, 0.3f}, {-1.9f, 0.9f, 0.9f, 0.8f, -0.75f, -0.5f}, {1.7f, -0.8f, 1f, 0.9f, -0.8f, 0.9f},
        {0.6f, 1.8f, 0.75f, 0.6f, -1.05f, 0.2f}, {-0.9f, -1.8f, 0.8f, 0.7f, -1.2f, -0.2f}, {2.6f, 1.1f, 0.6f, 0.5f, -1.15f, 0.6f}};

    /** Whether the beach of the cell along column i has its rocks and its fisherman. */
    boolean rocky(int i) {
        return Math.floorMod(hash(i, -1, 23), 3) == 0 && i != OLD_I;
    }

    /**
     * Where the fisherman on the rocks of column i sits: x, the top of the rock, z (into out),
     * and which way he faces out to sea; false if the cell has no rocks.
     */
    boolean fisher(int i, float[] out) {
        if (!rocky(i)) return false;
        out[0] = i * CELL + ROCKS_AT + 0.2f;
        out[1] = ROCKS[0][4];
        out[2] = SHORE * CELL + ROCKS_Z + 0.2f;
        out[3] = (float) Math.PI + 0.15f * ((hash(i, -1, 24) & 3) - 1.5f);
        return true;
    }

    /** The rocks: grey boulders turned this way and that, half in the water, each a footing. */
    private void rocks(Cell c, int i) {
        float bx = i * CELL + ROCKS_AT, bz = SHORE * CELL + ROCKS_Z, low = WATER - BED;
        int[] grey = {0x7E7A72, 0x8C877C, 0x6E6A62, 0x837C70};
        float[] turn = new float[9];
        for (int k = 0; k < ROCKS.length; k++) {
            float[] r = ROCKS[k];
            float x = bx + r[0], z = bz + r[1], top = r[4], cos = (float) Math.cos(r[5]), sin = (float) Math.sin(r[5]);
            turn[0] = cos; turn[1] = 0f; turn[2] = sin;
            turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
            turn[6] = -sin; turn[7] = 0f; turn[8] = cos;
            c.boxes.box(x, (top + low) * 0.5f, z, r[2], (top - low) * 0.5f, r[3], turn, grey[k % grey.length], DrawList.STONE);
            // a weathered top, a little lighter
            c.boxes.box(x, top - 0.06f, z, r[2] * 0.8f, 0.08f, r[3] * 0.8f, turn, 0x9C978A, DrawList.STONE);
            float hx = Math.abs(cos) * r[2] + Math.abs(sin) * r[3], hz = Math.abs(sin) * r[2] + Math.abs(cos) * r[3];
            c.wall(x - hx * 0.85f, z - hz * 0.85f, x + hx * 0.85f, z + hz * 0.85f, top, low);
        }
        // his bucket and his bag beside him
        c.detail.box(bx + 0.9f, ROCKS[0][4] + 0.18f, bz + 0.5f, 0.17f, 0.18f, 0.17f, null, 0x3E6A8A, DrawList.PAINT);
        c.detail.box(bx - 0.8f, ROCKS[0][4] + 0.12f, bz + 0.4f, 0.28f, 0.12f, 0.18f, null, 0x5A6A3A, DrawList.GRAIN);
    }

    private void promenade(Cell c, float x0, float z0) {
        DrawList d = c.boxes;
        Random deco = new Random(hash(Math.round(x0 / CELL), Math.round(z0 / CELL), 10));
        float top = z0 + CELL, cw = CURB - ROAD, cy = (CURB_TOP - 0.25f) * 0.5f, ch = (CURB_TOP + 0.25f) * 0.5f;
        float mx = x0 + CELL * 0.5f, hx = CELL * 0.5f;
        d.box(mx, cy, top - ROAD - cw * 0.5f, hx, ch, cw * 0.5f, null, 0xC9BFAE, DrawList.STONE);
        float p0 = z0 + CELL + PARAPET + PARAPET_HALF, p1 = top - CURB;
        d.box(mx, (WALK_TOP - 0.25f) * 0.5f, (p0 + p1) * 0.5f, hx, (WALK_TOP + 0.25f) * 0.5f, (p1 - p0) * 0.5f,
                null, 0xDCCDB2, DrawList.PROMENADE);
        // the parapet: the quay's stone face from the sand up past the pavement, a cap on it; a gap in
        // it where the steps go down, the face there only as high as the pavement
        float pz = z0 + CELL + PARAPET, low = SAND - 0.4f, high = WALK_TOP + PARAPET_HIGH;
        float g0 = x0 + STAIRS_AT - STAIRS_HALF, g1 = x0 + STAIRS_AT + STAIRS_HALF;
        float[][] parts = {{x0, g0}, {g1, x0 + CELL}};
        for (float[] q : parts) {
            float qm = (q[0] + q[1]) * 0.5f, qh = (q[1] - q[0]) * 0.5f;
            d.box(qm, (low + high) * 0.5f, pz, qh, (high - low) * 0.5f, PARAPET_HALF, null, 0xD2C2A4, DrawList.STONE);
            d.box(qm, high + 0.06f, pz, qh, 0.06f, PARAPET_HALF + 0.08f, null, 0xEDE2CC, DrawList.PLAIN);
            c.wall(q[0], pz - PARAPET_HALF, q[1], pz + PARAPET_HALF, high + 0.12f, low);
        }
        d.box(x0 + STAIRS_AT, (low + WALK_TOP) * 0.5f, pz, STAIRS_HALF, (WALK_TOP - low) * 0.5f, PARAPET_HALF, null, 0xDCCDB2, DrawList.STONE);
        // the steps down, away from the quay, each a stride deep
        float face = pz - PARAPET_HALF, rise = (WALK_TOP - SAND) / 5f;
        for (int k = 0; k < 4; k++) {
            float st = WALK_TOP - (k + 1) * rise, s0 = face - (k + 1) * STAIR, s1 = face - k * STAIR;
            d.box(x0 + STAIRS_AT, (st + low) * 0.5f, (s0 + s1) * 0.5f, STAIRS_HALF, (st - low) * 0.5f, STAIR * 0.5f,
                    null, 0xD6C7AA, DrawList.STONE);
            c.wall(g0, s0, g1, s1, st, low);
        }
        // the beach: sand from the quay to the wet, the wet sand to the water
        float wet = z0 + CELL + WET_FROM, sea = z0 + CELL + WATERLINE;
        d.box(mx, (SAND + low) * 0.5f, (face + wet) * 0.5f, hx, (SAND - low) * 0.5f, (face - wet) * 0.5f, null, 0xD8BC8A, DrawList.GRAIN);
        d.box(mx, (WET + low) * 0.5f, (wet + sea) * 0.5f, hx, (WET - low) * 0.5f, (wet - sea) * 0.5f, null, 0xBFA27A, DrawList.GRAIN);
        // now and then a run of rocks out into the water, a fisherman on the highest
        int ci = Math.round(x0 / CELL);
        if (rocky(ci)) rocks(c, ci);
        // a sunshade now and then, striped, two loungers under it looking out to sea
        for (float t = 10f; t < CELL - 4f; t += 22f) {
            if (rocky(ci) && t > ROCKS_AT - 12f) continue;
            if (deco.nextInt(3) == 0) continue;
            float ux = x0 + t + deco.nextFloat() * 6f, uz = face - 6f - deco.nextFloat() * 4f;
            int cloth = deco.nextBoolean() ? 0xE9765B : 0x3FAFA6;
            c.detail.box(ux, SAND + 1.2f, uz, 0.04f, 1.2f, 0.04f, null, 0xEDE2CC, DrawList.PLAIN);
            c.detail.box(ux, SAND + 2.3f, uz, 1.35f, 0.05f, 1.35f, null, cloth, DrawList.material(DrawList.AWNING, 1));
            c.wall(ux - 0.1f, uz - 0.1f, ux + 0.1f, uz + 0.1f, TALL, low);
            for (int e = -1; e <= 1; e += 2) {
                float lx = ux + e * 0.75f;
                c.detail.box(lx, SAND + 0.22f, uz - 0.3f, 0.32f, 0.05f, 0.95f, null, 0xF4E8D2, DrawList.WOOD);
                c.detail.box(lx, SAND + 0.45f, uz + 0.55f, 0.32f, 0.05f, 0.3f, null, 0xF4E8D2, DrawList.WOOD);
                c.wall(lx - 0.32f, uz - 1.25f, lx + 0.32f, uz + 0.85f, SAND + 0.3f, low);
                if ((c.loungerN + 1) * 2 > c.loungers.length) c.loungers = java.util.Arrays.copyOf(c.loungers, c.loungers.length * 2);
                c.loungers[c.loungerN * 2] = lx;
                c.loungers[c.loungerN * 2 + 1] = uz + 0.1f;
                c.loungerN++;
            }
        }
        // palms in two rows down the promenade, a little out of step
        for (int row = 0; row < 2; row++) {
            float rz = row == 0 ? p1 - 3.2f : p0 + 4.6f;
            for (float t = row * 6f + 3f; t < CELL; t += 12f) {
                float px = x0 + t, h = 6.8f + deco.nextFloat() * 2f, lean = (deco.nextFloat() - 0.5f) * 0.3f;
                if (keptClear(px, rz) || keptClear(px + 1.5f, rz) || keptClear(px - 1.5f, rz)) continue;
                palm(c, deco, px, WALK_TOP, rz, h, lean, false);
                c.wall(px - 0.25f, rz - 0.25f, px + 0.25f, rz + 0.25f);
            }
        }
        // lamps along the parapet, benches between them looking out to sea
        for (float t = 8f; t < CELL; t += 16f) {
            lamp(c, c.detail, x0 + t, pz + PARAPET_HALF + 0.6f);
            float bx = x0 + t + 8f, bz = pz + PARAPET_HALF + 1.3f;
            bench(c, bx, WALK_TOP, bz, (float) Math.PI);
            c.wall(bx - 0.9f, bz - 0.3f, bx + 0.9f, bz + 0.3f, WALK_TOP + 0.48f);
        }
    }

    /** A block of houses round its four sides, their fronts on the streets, a yard hidden in the middle. */
    private void block(Cell c, Random rnd, float x0, float z0, int i, int j) {
        float a = FRONT, b = CELL - FRONT;
        // the south and north rows run the whole way; the west and east rows fit between them
        // the yard: open, an arch through a house of the south or the north row, or shut in
        // the old house's block has its arch, if any, on the north
        boolean mine = i == OLD_I && j == OLD_J;
        boolean open = arched(i, j), north = mine || ((hash(i, j, 8) >>> 9) & 1) == 0;
        boolean works = j == SHORE && (i == FACTORY_I || i == STORE_I);
        if (works) north = true;
        if (i == PORT_I && j == PORT_J) {
            port(c, rnd, x0, z0);
            return;
        }
        if (i == HOME_I && j == HOME_J) {
            homeBlock(c, rnd, x0, z0);
            return;
        }
        // the shop and the taxis' stand: their own north row, the yard behind shut in
        boolean shop = i == SHOP_I && j == SHOP_J, taxis = i == TAXI_I && j == TAXI_J;
        if (shop || taxis) open = false;
        // the archive's garden: a way in under an arch through the north row
        boolean archive = i == ARCHIVE_I && j == ARCHIVE_J;
        if (archive) {
            open = true;
            north = true;
        }
        if (mine) oldHouse(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (works && i == FACTORY_I) factory(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (works) store(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (i == CLINIC_I && j == CLINIC_J) clinic(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (i == CONCERT_I && j == CONCERT_J) concertHall(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (i == TOWNHALL_I && j == TOWNHALL_J) townHall(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else if (archive) archive(c, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH);
        else row(c, rnd, x0 + a, x0 + b, z0 + a, z0 + a + DEPTH, true, -1, open && !north);
        if (shop) shop(c, x0 + a, x0 + b, z0 + b - DEPTH, z0 + b);
        else if (taxis) taxis(c, x0 + a, x0 + b, z0 + b - DEPTH, z0 + b);
        else if (archive) archiveFront(c, x0 + a, x0 + b, z0 + b - DEPTH, z0 + b);
        else row(c, rnd, x0 + a, x0 + b, z0 + b - DEPTH, z0 + b, true, 1, open && north);
        row(c, rnd, z0 + a + DEPTH, z0 + b - DEPTH, x0 + a, x0 + a + DEPTH, false, -1, false);
        if (i == WORKSHOP_I && j == WORKSHOP_J) workshop(c, z0 + a + DEPTH, z0 + b - DEPTH, x0 + b - DEPTH, x0 + b);
        else row(c, rnd, z0 + a + DEPTH, z0 + b - DEPTH, x0 + b - DEPTH, x0 + b, false, 1, false);
        yard(c, open, new Random(hash(i, j, 9)));
        if (archive) garden(c, x0 + a + DEPTH, x0 + b - DEPTH, z0 + a + DEPTH, z0 + b - DEPTH);
    }

    /**
     * The port: no houses, a yard of concrete. In its middle the field of the containers' bays,
     * fenced in; over it the crane, a gantry on two rails (drawn with the puzzle, see Quay); at
     * the fence's south-west corner, toward the shore road, a steel tower with a ladder up its face
     * and a catwalk at its top over to the crane's rail end; round the fence, along the yard's
     * north and east edges, containers stacked one to three high, waiting.
     */
    private void port(Cell c, Random rnd, float x0, float z0) {
        DrawList d = c.boxes, small = c.detail;
        float cx = Quay.CX, cz = Quay.CZ;
        float in0x = x0 + CURB, in1x = x0 + CELL - CURB, in0z = z0 + CURB, in1z = z0 + CELL - CURB;
        // the concrete, a shade darker than the pavements, its joints
        d.box((in0x + in1x) * 0.5f, WALK_TOP + 0.005f, (in0z + in1z) * 0.5f, (in1x - in0x) * 0.5f, 0.006f, (in1z - in0z) * 0.5f, null, 0xA9A49A, DrawList.PAVING);
        for (float z = in0z + 6f; z < in1z; z += 6f)
            small.box((in0x + in1x) * 0.5f, WALK_TOP + 0.013f, z, (in1x - in0x) * 0.5f, 0.003f, 0.03f, null, 0x8C877E, DrawList.PLAIN);
        // the rails the crane runs on
        for (int e = -1; e <= 1; e += 2)
            for (int r = -1; r <= 1; r += 2)
                small.box(cx + e * Quay.GANTRY_HALF + r * 0.5f, WALK_TOP + 0.06f, cz, 0.06f, 0.05f, Quay.RAILS_HALF, null, 0x3E3A36, DrawList.POLISHED);
        // the fence round the field: posts, a dark mesh, a yellow rail along its top; on the walls
        float fx = Quay.FENCE_X, fz = Quay.FENCE_Z, fh = 3.2f;
        int post = 0x5A5E62, mesh = 0x3A3F44, yellow = 0xE8B830;
        for (int side = 0; side < 4; side++) {
            boolean alongX = side < 2;
            float v = side == 0 ? cz - fz : side == 1 ? cz + fz : side == 2 ? cx - fx : cx + fx;
            float u0 = alongX ? cx - fx : cz - fz, u1 = alongX ? cx + fx : cz + fz, um = (u0 + u1) * 0.5f, uh = (u1 - u0) * 0.5f;
            float bx = alongX ? um : v, bz = alongX ? v : um;
            small.box(bx, WALK_TOP + fh * 0.5f, bz, alongX ? uh : 0.02f, fh * 0.5f, alongX ? 0.02f : uh, null, mesh, DrawList.RAIL);
            small.box(bx, WALK_TOP + fh, bz, alongX ? uh : 0.05f, 0.05f, alongX ? 0.05f : uh, null, yellow, DrawList.PAINT);
            for (float u = u0; u <= u1 + 0.01f; u += 3f)
                small.box(alongX ? u : v, WALK_TOP + fh * 0.5f, alongX ? v : u, 0.05f, fh * 0.5f, 0.05f, null, post, DrawList.PLAIN);
            c.wall(alongX ? u0 : v - 0.15f, alongX ? v - 0.15f : u0, alongX ? u1 : v + 0.15f, alongX ? v + 0.15f : u1, fh + 0.3f);
        }
        // the tower at the fence's south-west corner: four yellow legs braced, a platform at the
        // top with its railing, the ladder up its south face, a catwalk east to the crane's rail end
        float tx = Quay.TOWER_X, tz = Quay.TOWER_Z, th = Quay.TOWER_TOP, half = 1.2f;
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2)
                d.box(tx + a * (half - 0.12f), th * 0.5f, tz + b * (half - 0.12f), 0.12f, th * 0.5f, 0.12f, null, yellow, DrawList.PAINT);
        for (float y = 2.5f; y < th - 1f; y += 3f)
            for (int a = -1; a <= 1; a += 2) {
                small.box(tx + a * (half - 0.12f), y, tz, 0.06f, 0.06f, half, null, yellow, DrawList.PAINT);
                small.box(tx, y + 1.5f, tz + a * (half - 0.12f), half, 0.06f, 0.06f, null, yellow, DrawList.PAINT);
            }
        d.box(tx, th - 0.1f, tz, half + 0.3f, 0.1f, half + 0.3f, null, 0x6A6E72, DrawList.PLAIN);
        for (int a = -1; a <= 1; a += 2) {
            small.box(tx + a * (half + 0.28f), th + 0.55f, tz, 0.04f, 0.55f, half + 0.3f, null, yellow, DrawList.RAIL);
            small.box(tx, th + 0.55f, tz + (half + 0.28f), half + 0.3f, 0.55f, 0.04f, null, yellow, DrawList.RAIL);
        }
        c.wall(tx - half - 0.3f, tz - half - 0.3f, tx + half + 0.3f, tz + half + 0.3f, th);
        ladder(c, tx, tz - half - 0.3f, WALK_TOP, th, 0f, -1f, false);
        float cw0 = tx + half + 0.3f, cw1 = cx - Quay.GANTRY_HALF;
        d.box((cw0 + cw1) * 0.5f, th - 0.1f, tz, (cw1 - cw0) * 0.5f, 0.08f, 0.6f, null, 0x6A6E72, DrawList.PLAIN);
        for (int b = -1; b <= 1; b += 2)
            small.box((cw0 + cw1) * 0.5f, th + 0.55f, tz + b * 0.6f, (cw1 - cw0) * 0.5f, 0.55f, 0.03f, null, yellow, DrawList.RAIL);
        // the containers waiting round the edges, along the north and the east
        int[] cols = Quay.COLORS;
        for (float x = in0x + 2f; x < in1x - 3f; x += 2.9f) {
            if (rnd.nextInt(5) == 0) continue;
            float z = in1z - 3.4f;
            int n = 1 + rnd.nextInt(3);
            for (int k = 0; k < n; k++) Quay.box(d, small, x + 1.22f, WALK_TOP + k * Quay.HIGH_EACH, z, false, cols[rnd.nextInt(cols.length)]);
            c.wall(x, z - Quay.HALF_LONG, x + 2.44f, z + Quay.HALF_LONG, WALK_TOP + n * Quay.HIGH_EACH);
        }
        for (float z = in0z + 3f; z < cz + fz - 3f; z += 2.9f) {
            if (rnd.nextInt(5) == 0) continue;
            float x = in1x - 3.4f;
            int n = 1 + rnd.nextInt(3);
            for (int k = 0; k < n; k++) Quay.box(d, small, x, WALK_TOP + k * Quay.HIGH_EACH, z + 1.22f, true, cols[rnd.nextInt(cols.length)]);
            c.wall(x - Quay.HALF_LONG, z, x + Quay.HALF_LONG, z + 2.44f, WALK_TOP + n * Quay.HIGH_EACH);
        }
    }

    /**
     * The town hall: pale warm stone, five storeys over a ground floor of rusticated blocks; in the
     * middle the front stands out, and on it the great balcony at the first floor: a deep stone
     * floor on brackets, a balustrade, a row of columns carrying a roof over it, the town's shield
     * on the roof's front. Over the middle the tower: the clock on four faces, an open belfry of
     * arches, a copper dome and a lantern. At the two ends round towers rising a storey over the
     * roof, copper domes and lanterns. The lift behind the middle door; on the roof the lift's
     * hut and the helicopter's pad.
     */
    private void townHall(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = TOWNHALL_HIGH, top = high + 0.5f;
        int stone = 0xE8D8BC, band = 0xC9B48E, dark = 0xB09A74, glass = 0x2A3A48, gold = 0xC9A15A, copper = 0x5E9A84, roofCol = 0x9A8A78;
        d.box(cx, top * 0.5f, cz, hx, top * 0.5f, hz, null, stone, DrawList.STONE);
        // the ground floor in blocks, a band over it, a cornice under the roof
        d.box(cx, 2.6f, z0 - 0.08f, hx, 2.6f, 0.1f, null, band, DrawList.STONE);
        for (float y = 0.9f; y < 5f; y += 0.9f) d.box(cx, y, z0 - 0.19f, hx, 0.03f, 0.02f, null, dark, DrawList.STONE);
        d.box(cx, 5.35f, z0 - 0.2f, hx + 0.1f, 0.15f, 0.25f, null, dark, DrawList.STONE);
        // the cornice round the roof's edge, under it: the roof itself is the top she stands on
        d.box(cx, top - 0.27f, cz, hx + 0.35f, 0.25f, hz + 0.35f, null, band, DrawList.STONE);
        // the windows of the upper floors in stone frames, a pediment over the first floor's
        for (int fl = 0; fl < 4; fl++) {
            float wy = 7f + fl * 2.9f;
            for (float u = x0 + 3.5f; u < x1 - 3f; u += 3f) {
                if (Math.abs(u - cx) < 8.5f && fl < 2) continue;
                if (Math.abs(u - cx) < 3.2f) continue;
                d.box(u, wy, z0 - 0.05f, 0.62f, 0.95f, 0.04f, null, glass, DrawList.GLASS);
                small.box(u, wy, z0 - 0.08f, 0.78f, 1.08f, 0.03f, null, band, DrawList.STONE);
                if (fl == 0) small.box(u, wy + 1.3f, z0 - 0.12f, 0.9f, 0.12f, 0.1f, null, dark, DrawList.STONE);
            }
        }
        // the ground floor's tall arched openings either side of the door
        for (float u = x0 + 3.5f; u < x1 - 3f; u += 3f) {
            if (Math.abs(u - cx) < 3.5f) continue;
            d.box(u, 2.4f, z0 - 0.2f, 0.7f, 1.6f, 0.03f, null, glass, DrawList.GLASS);
            small.box(u, 4.1f, z0 - 0.22f, 0.85f, 0.15f, 0.04f, null, dark, DrawList.STONE);
        }
        // the middle stood out, the great balcony on it at the first floor
        float bz = z0 - 3f, by = 5.6f, bw = 8f;
        d.box(cx, top * 0.5f, z0 - 0.4f, 8.6f, top * 0.5f, 0.4f, null, stone, DrawList.STONE);
        d.box(cx, by, (bz + z0) * 0.5f, bw, 0.22f, (z0 - bz) * 0.5f, null, band, DrawList.STONE);
        for (float u = cx - bw + 1f; u <= cx + bw - 0.9f; u += 2.3f)
            small.box(u, by - 0.55f, bz + 0.6f, 0.2f, 0.4f, 0.6f, null, dark, DrawList.STONE);
        // its balustrade: balusters and a rail along the front and the ends
        for (float u = cx - bw + 0.3f; u <= cx + bw - 0.2f; u += 0.45f)
            small.box(u, by + 0.6f, bz + 0.2f, 0.08f, 0.38f, 0.08f, null, stone, DrawList.STONE);
        d.box(cx, by + 1.02f, bz + 0.2f, bw, 0.07f, 0.16f, null, band, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2) d.box(cx + e * (bw - 0.15f), by + 0.6f, (bz + z0) * 0.5f, 0.15f, 0.45f, (z0 - bz) * 0.5f, null, band, DrawList.STONE);
        // the columns along it, three and three, the way to the door wide between them; the roof
        // they carry, the shield on its front
        for (int k = 0; k < 8; k++) {
            if (k == 3 || k == 4) continue;
            float u = cx - bw + 0.8f + k * (2f * bw - 1.6f) / 7f;
            d.box(u, by + 0.35f, bz + 0.6f, 0.32f, 0.12f, 0.32f, null, band, DrawList.STONE);
            d.box(u, by + 2.3f, bz + 0.6f, 0.24f, 1.9f, 0.24f, null, stone, DrawList.STONE);
            d.box(u, by + 4.3f, bz + 0.6f, 0.34f, 0.12f, 0.34f, null, band, DrawList.STONE);
        }
        float ry = by + 4.75f;
        d.box(cx, ry, (bz + z0) * 0.5f, bw + 0.2f, 0.32f, (z0 - bz) * 0.5f + 0.1f, null, band, DrawList.STONE);
        d.box(cx, ry + 0.5f, bz + 0.15f, bw + 0.25f, 0.18f, 0.2f, null, dark, DrawList.STONE);
        small.box(cx, ry + 1.15f, bz + 0.2f, 1.1f, 0.85f, 0.08f, null, 0x2E6A64, DrawList.PAINT);
        small.box(cx, ry + 1.05f, bz + 0.16f, 0.85f, 0.6f, 0.05f, null, 0xE6DCC6, DrawList.PAINT);
        small.box(cx, ry + 1.15f, bz + 0.24f, 1.2f, 0.95f, 0.04f, null, gold, DrawList.GOLD);
        // the window doors out onto the balcony
        for (int k = -2; k <= 2; k++)
            d.box(cx + k * 3f, by + 1.85f, z0 - 0.83f, 0.7f, 1.6f, 0.04f, null, glass, DrawList.GLASS);
        // the great door in the middle, on the face stood out: a frame of stone, two leaves of dark
        // wood with lit glass in their tops and gilt down their meeting, a lit fan over them under
        // the arch, a lantern either side; two reliefs beyond
        float fz = z0 - 0.8f;
        int wood = 0x4A3226;
        d.box(cx, WALK_TOP + 2.05f, fz - 0.04f, 1.75f, 2.05f, 0.05f, null, band, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2) {
            float lu = cx + e * 0.68f;
            d.box(lu, WALK_TOP + 1.65f, fz - 0.1f, 0.66f, 1.65f, 0.05f, null, wood, DrawList.WOOD);
            small.box(lu, WALK_TOP + 2.45f, fz - 0.16f, 0.42f, 0.55f, 0.02f, null, 0xFFD9A0, DrawList.GLOW);
            small.box(lu, WALK_TOP + 0.85f, fz - 0.16f, 0.42f, 0.5f, 0.02f, null, 0x3A281E, DrawList.WOOD);
            small.box(cx + e * 0.12f, WALK_TOP + 1.5f, fz - 0.2f, 0.035f, 0.22f, 0.035f, null, gold, DrawList.GOLD);
            small.box(cx + e * 1.95f, WALK_TOP + 2.9f, fz - 0.22f, 0.16f, 0.24f, 0.16f, null, 0xFFE9BE, DrawList.GLOW);
            small.box(cx + e * 1.95f, WALK_TOP + 3.2f, fz - 0.22f, 0.2f, 0.05f, 0.2f, null, 0x2B2F3A, DrawList.PLAIN);
            small.box(cx + e * 1.95f, WALK_TOP + 2.6f, fz - 0.14f, 0.05f, 0.08f, 0.12f, null, 0x2B2F3A, DrawList.PLAIN);
        }
        small.box(cx, WALK_TOP + 1.65f, fz - 0.17f, 0.04f, 1.65f, 0.03f, null, gold, DrawList.GOLD);
        small.box(cx, WALK_TOP + 3.65f, fz - 0.1f, 1.3f, 0.3f, 0.03f, null, 0xFFD9A0, DrawList.GLOW);
        small.box(cx, WALK_TOP + 4.15f, fz - 0.06f, 1.9f, 0.22f, 0.08f, null, dark, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2) small.box(cx + e * 3.2f, 2.6f, z0 - 0.84f, 0.9f, 1.3f, 0.05f, null, 0xF2E8D4, DrawList.STONE);
        // the steps up to the door
        for (int k = 0; k < 2; k++)
            d.box(cx, WALK_TOP + 0.08f * (k + 0.5f), z0 - 1.6f + k * 0.45f, 3.4f - k * 0.3f, 0.08f * (k + 0.5f), 0.9f - k * 0.3f, null, band, DrawList.STONE);
        // the tower over the middle: its body, the clock on its faces, the belfry, the dome
        float tz0 = z0 - 0.8f, tz1 = z0 + 2.4f, tcz = (tz0 + tz1) * 0.5f, thz = (tz1 - tz0) * 0.5f, thx = 2.6f;
        float t0 = top, t1 = top + 7f;
        d.box(cx, (t0 + t1) * 0.5f, tcz, thx, (t1 - t0) * 0.5f, thz, null, stone, DrawList.STONE);
        d.box(cx, t1 + 0.15f, tcz, thx + 0.2f, 0.15f, thz + 0.2f, null, band, DrawList.STONE);
        float clockY = t1 - 2.2f;
        small.box(cx, clockY, tz0 - 0.03f, 1.4f, 1.4f, 0.04f, null, 0xF4EEDC, DrawList.PLAIN);
        small.box(cx, clockY, tz0 - 0.06f, 1.55f, 1.55f, 0.03f, null, gold, DrawList.GOLD);
        small.box(cx, clockY + 0.45f, tz0 - 0.08f, 0.07f, 0.55f, 0.02f, null, 0x23262C, DrawList.PLAIN);
        small.box(cx + 0.3f, clockY, tz0 - 0.08f, 0.38f, 0.06f, 0.02f, null, 0x23262C, DrawList.PLAIN);
        for (int e = -1; e <= 1; e += 2) {
            small.box(cx + e * (thx + 0.03f), clockY, tcz, 0.04f, 1.1f, 1.1f, null, 0xF4EEDC, DrawList.PLAIN);
            small.box(cx + e * (thx + 0.06f), clockY, tcz, 0.03f, 1.22f, 1.22f, null, gold, DrawList.GOLD);
        }
        // the belfry: four piers and the arches between, the bell hung in it
        float b0 = t1 + 0.3f, b1 = b0 + 3.2f;
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2)
                d.box(cx + a * (thx - 0.4f), (b0 + b1) * 0.5f, tcz + b * (thz - 0.35f), 0.35f, (b1 - b0) * 0.5f, 0.35f, null, stone, DrawList.STONE);
        d.box(cx, b1 + 0.2f, tcz, thx, 0.2f, thz, null, band, DrawList.STONE);
        small.box(cx, b0 + 1.9f, tcz, 0.5f, 0.6f, 0.5f, null, 0xA8823A, DrawList.GOLD);
        small.box(cx, b0 + 2.6f, tcz, 0.08f, 0.25f, 0.08f, null, 0x3A3A3A, DrawList.PLAIN);
        dome(d, small, cx, b1 + 0.4f, tcz, thx - 0.1f, copper, gold);
        // solid, up from the roof to the belfry's top: no walking through it
        c.wall(cx - thx, tz0, cx + thx, tz1, b1 + 0.4f);
        // the round towers at the two ends, a storey over the roof
        for (int e = -1; e <= 1; e += 2) {
            float rx = cx + e * (hx - 2.6f), rz = z0 + 1.6f, rr = 2.6f, rt = top + 3.6f;
            octagon(d, rx, rt * 0.5f, rz, rr, rt * 0.5f, stone, DrawList.STONE);
            octagon(d, rx, rt + 0.15f, rz, rr + 0.2f, 0.15f, band, DrawList.STONE);
            for (int fl = 0; fl < 4; fl++)
                small.box(rx, 7f + fl * 2.9f, rz - rr - 0.03f, 0.5f, 0.85f, 0.04f, null, glass, DrawList.GLASS);
            dome(d, small, rx, rt + 0.3f, rz, rr - 0.1f, copper, gold);
            c.wall(rx - rr * 0.95f, rz - rr * 0.95f, rx + rr * 0.95f, rz + rr * 0.95f, rt + 0.3f);
        }
        // the lift: its door in the front, the hut on the roof
        lift(c, d, cx, z0, true, -1, high, stone);
        // the pad on the roof: a ring and a letter, yellow on the roof's grey
        d.box(townPadX(), top + 0.01f, townPadZ(), 5f, 0.01f, 5f, null, roofCol, DrawList.STONE);
        pad(small, townPadX(), top + 0.03f, townPadZ(), 4.4f, 0xF2C230);
        // the flag on the tower's dome
        small.box(cx, b1 + 5.6f, tcz, 0.05f, 1.6f, 0.05f, null, 0xC9CCD0, DrawList.POLISHED);
        small.box(cx + 0.9f, b1 + 6.6f, tcz, 0.85f, 0.5f, 0.02f, null, 0x3E5872, DrawList.PAINT);
        small.box(cx + 0.9f, b1 + 6.6f, tcz - 0.01f, 0.85f, 0.16f, 0.02f, null, 0xF2EAD6, DrawList.PAINT);
        c.wall(x0, z0, x1, z1, top);
        c.house(cx, cz, hx, hz, top, 0f, -1f, LIFT);
    }

    /** A prism of eight sides round (x, z), so far out to its faces, standing at y, so high each way: two squares a quarter turned. */
    private static void octagon(DrawList d, float x, float y, float z, float r, float hy, int color, int material) {
        float[] turn = {0.7071f, 0f, 0.7071f, 0f, 1f, 0f, -0.7071f, 0f, 0.7071f};
        d.box(x, y, z, r, hy, r, null, color, material);
        d.box(x, y, z, r, hy, r, turn, color, material);
    }

    /** A dome of copper from y on (x, z), so wide at its foot: tiers of eight sides, a lantern, a spike. */
    private static void dome(DrawList d, DrawList small, float x, float y, float z, float r, int copper, int gold) {
        float[] k = {1f, 0.92f, 0.78f, 0.58f, 0.32f};
        float tier = r * 0.32f;
        for (int i = 0; i < k.length; i++)
            octagon(d, x, y + tier * (i + 0.5f), z, r * k[i], tier * 0.5f, copper, DrawList.POLISHED);
        float ly = y + tier * k.length;
        octagon(small, x, ly + 0.5f, z, r * 0.2f, 0.5f, 0xE8D8BC, DrawList.STONE);
        octagon(small, x, ly + 1.1f, z, r * 0.24f, 0.1f, copper, DrawList.POLISHED);
        small.box(x, ly + 1.7f, z, 0.06f, 0.5f, 0.06f, null, gold, DrawList.GOLD);
    }

    /** A helicopter pad on a flat top at y: a ring of so many parts, the letter H across it. */
    static void pad(DrawList d, float x, float y, float z, float r, int color) {
        int parts = 16;
        float w = (float) (Math.PI * r / parts) + 0.05f;
        float[] turn = new float[9];
        for (int k = 0; k < parts; k++) {
            double a = k * 2 * Math.PI / parts;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            // its length along the ring, across the way out from the middle
            turn[0] = -s; turn[1] = 0f; turn[2] = c;
            turn[3] = 0f; turn[4] = 1f; turn[5] = 0f;
            turn[6] = -c; turn[7] = 0f; turn[8] = -s;
            d.box(x + s * r, y, z + c * r, 0.22f, 0.012f, w, turn, color, DrawList.PAINT);
        }
        d.box(x - 1.2f, y, z, 0.25f, 0.012f, 1.9f, null, color, DrawList.PAINT);
        d.box(x + 1.2f, y, z, 0.25f, 0.012f, 1.9f, null, color, DrawList.PAINT);
        d.box(x, y, z, 1.2f, 0.012f, 0.25f, null, color, DrawList.PAINT);
    }

    /**
     * The archive: grey stone, heavy, three storeys; small deep windows; a door of dark wood in the
     * middle of the street front, a board over it; the same door at the back, on the garden.
     */
    private void archive(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float top = ARCHIVE_HIGH + 0.5f;
        int stone = 0x8F918F, dark = 0x6E706E, wood = 0x3A2A20, glass = 0x1E2830;
        d.box(cx, top * 0.5f, cz, hx, top * 0.5f, hz, null, stone, DrawList.STONE);
        // the cornice, under the roof's top (the top is what she stands on)
        d.box(cx, top - 0.32f, cz, hx + 0.3f, 0.3f, hz + 0.3f, null, dark, DrawList.STONE);
        d.box(cx, 0.6f, cz, hx + 0.15f, 0.6f, hz + 0.15f, null, dark, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2) {
            float fz = e < 0 ? z0 : z1;
            for (int fl = 0; fl < 3; fl++)
                for (float u = x0 + 2.4f; u < x1 - 2f; u += 3.4f) {
                    if (Math.abs(u - cx) < 2.4f && fl == 0) continue;
                    d.box(u, 3f + fl * 3.3f, fz + e * 0.04f, 0.45f, 0.7f, 0.04f, null, glass, DrawList.GLASS);
                }
            // the door, a board over it
            d.box(cx, WALK_TOP + 1.4f, fz + e * 0.05f, 1.1f, 1.4f, 0.05f, null, wood, DrawList.WOOD);
            d.box(cx, WALK_TOP + 3.1f, fz + e * 0.07f, 1.8f, 0.3f, 0.04f, null, 0x2B3A32, DrawList.PAINT);
            small.box(cx, WALK_TOP + 3.1f, fz + e * 0.1f, 1.5f, 0.06f, 0.02f, null, 0xC9A15A, DrawList.GOLD);
        }
        c.wall(x0, z0, x1, z1, top);
        c.house(cx, cz, hx, hz, top, 0f, -1f, 0f);
    }

    /** The archive's yard, a garden: hedges along its sides, beds of flowers, the pad of gravel and paint in the middle. */
    private void garden(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float px = archivePadX(), pz = archivePadZ(), y = WALK_TOP;
        int hedge = 0x3E6B34, grass = 0x5E8A44, bed = 0x6A4632;
        d.box((x0 + x1) * 0.5f, y + 0.01f, (z0 + z1) * 0.5f, (x1 - x0) * 0.5f, 0.01f, (z1 - z0) * 0.5f, null, grass, DrawList.GRASS);
        // the hedge along the south, a gap in it for the door; the north open to the forecourt
        for (int e = -1; e <= 1; e += 2) {
            float hz = e < 0 ? z0 + 0.6f : z1 - 0.6f;
            if (e > 0) continue;
            for (int s = -1; s <= 1; s += 2) {
                float a = px + s * 2f, b = s < 0 ? x0 + 0.5f : x1 - 0.5f;
                d.box((a + b) * 0.5f, y + 0.45f, hz, Math.abs(b - a) * 0.5f, 0.45f, 0.5f, null, hedge, DrawList.LEAVES);
            }
            float hx = e < 0 ? x0 + 0.6f : x1 - 0.6f;
            d.box(hx, y + 0.45f, (z0 + z1) * 0.5f, 0.5f, 0.45f, (z1 - z0) * 0.5f - 1.1f, null, hedge, DrawList.LEAVES);
        }
        // beds of flowers in the corners
        int[] flowers = {0xE0506A, 0xF2C230, 0xF2EAD6, 0x9A6AD0};
        for (int k = 0; k < 4; k++) {
            float bx = (k & 1) == 0 ? x0 + 3f : x1 - 3f, bz = (k & 2) == 0 ? z0 + 3f : z1 - 3f;
            d.box(bx, y + 0.12f, bz, 1.6f, 0.12f, 1.6f, null, bed, DrawList.GRAIN);
            for (int f = 0; f < 6; f++)
                small.box(bx - 1.1f + (f % 3) * 1.1f, y + 0.36f, bz - 0.6f + (f / 3) * 1.2f, 0.28f, 0.14f, 0.28f, null, flowers[(k + f) % 4], DrawList.LEAVES);
        }
        // the pad: gravel, a white ring and letter
        d.box(px, y + 0.02f, pz, PAD_R, 0.02f, PAD_R, null, 0x9C968A, DrawList.GRAVEL);
        pad(small, px, y + 0.05f, pz, PAD_R - 1.2f, 0xF2EAD6);
    }

    /**
     * The archive's garden open to the street north of it: where the north row of houses stood, a
     * forecourt of lawn with a paved way down its middle from the gate to the pad; along the
     * pavement an iron railing on a low stone wall, a wide gate in its middle between two stone
     * piers with lanterns on them, its two leaves standing open against the railing.
     */
    private void archiveFront(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float y = WALK_TOP, mx = (x0 + x1) * 0.5f, fz = z1 - 0.35f, gate = 6f;
        int grass = 0x5E8A44, pave = 0xC8C2B4, stone = 0x8F918F, iron = 0x23262C, gold = 0xC9A15A;
        d.box(mx, y + 0.01f, (z0 + z1) * 0.5f, (x1 - x0) * 0.5f, 0.01f, (z1 - z0) * 0.5f, null, grass, DrawList.GRASS);
        d.box(mx, y + 0.02f, (z0 + z1) * 0.5f - 1f, 2.2f, 0.01f, (z1 - z0) * 0.5f + 1f, null, pave, DrawList.PAVING);
        // the low wall and the railing on it, both sides of the gate
        for (int e = -1; e <= 1; e += 2) {
            float a = mx + e * gate, b = e < 0 ? x0 : x1, cx = (a + b) * 0.5f, hx = Math.abs(b - a) * 0.5f;
            d.box(cx, y + 0.3f, fz, hx, 0.3f, 0.25f, null, stone, DrawList.STONE);
            small.box(cx, y + 1.75f, fz, hx, 0.03f, 0.03f, null, iron, DrawList.PLAIN);
            small.box(cx, y + 0.72f, fz, hx, 0.03f, 0.03f, null, iron, DrawList.PLAIN);
            for (float t = Math.min(a, b) + 0.12f; t < Math.max(a, b); t += 0.24f)
                small.box(t, y + 1.2f, fz, 0.015f, 0.6f, 0.015f, null, iron, DrawList.PLAIN);
            for (float t = Math.min(a, b) + 0.12f; t < Math.max(a, b); t += 0.24f)
                small.box(t, y + 1.83f, fz, 0.025f, 0.06f, 0.025f, null, gold, DrawList.GOLD);
            c.wall(Math.min(a, b), fz - 0.25f, Math.max(a, b), fz + 0.25f, y + 1.85f);
            // the piers either side of the gate, a lantern on each
            float px = mx + e * (gate + 0.45f);
            d.box(px, y + 1.4f, fz, 0.45f, 1.4f, 0.45f, null, stone, DrawList.STONE);
            d.box(px, y + 2.86f, fz, 0.55f, 0.08f, 0.55f, null, Street.scale(stone, 0.85f), DrawList.STONE);
            small.box(px, y + 3.2f, fz, 0.2f, 0.26f, 0.2f, null, 0xFFE9BE, DrawList.GLOW);
            small.box(px, y + 3.5f, fz, 0.26f, 0.04f, 0.26f, null, iron, DrawList.PLAIN);
            c.wall(px - 0.45f, fz - 0.45f, px + 0.45f, fz + 0.45f, y + 2.95f);
            // the gate's leaf, open inward against the railing
            float lx = mx + e * (gate - 0.2f) - e * 0.05f, lz = fz - 1.6f;
            small.box(lx, y + 1.1f, lz, 0.03f, 1f, 1.5f, null, iron, DrawList.PLAIN);
            for (float t = -1.35f; t <= 1.36f; t += 0.27f) small.box(lx, y + 1.1f, lz + t, 0.04f, 0.95f, 0.012f, null, iron, DrawList.PLAIN);
            small.box(lx, y + 2.15f, lz, 0.05f, 0.05f, 1.5f, null, gold, DrawList.GOLD);
            c.wall(lx - 0.1f, lz - 1.5f, lx + 0.1f, lz + 1.5f, y + 2.2f);
        }
        // over the gate a band of iron between the piers, the archive's plaque in it
        small.box(mx, y + 3.6f, fz, gate, 0.05f, 0.05f, null, iron, DrawList.PLAIN);
        small.box(mx, y + 3.95f, fz, 1.6f, 0.35f, 0.04f, null, 0x2E3A44, DrawList.PAINT);
        small.box(mx, y + 3.95f, fz - 0.02f, 1.7f, 0.42f, 0.03f, null, gold, DrawList.GOLD);
        // trees in the lawn either side of the way
        Random rnd = new Random(0xA2C4L);
        for (int e = -1; e <= 1; e += 2)
            for (int k = 0; k < 2; k++) {
                float tx = mx + e * (9f + k * 8f), tz = (z0 + z1) * 0.5f + (k == 0 ? -1.5f : 1.5f);
                tree(c, small, rnd, tx, y, tz, 6.5f + rnd.nextFloat() * 2f);
                c.wall(tx - 0.2f, tz - 0.2f, tx + 0.2f, tz + 0.2f);
            }
    }

    /** A bay painted on the road before a gate: an amber frame, a stripe across it now and then. */
    private static void bay(DrawList d, float gx, float z0) {
        int amber = 0xF0B03C;
        float bz = z0 + BAY_Z, y = 0.006f;
        for (int e = -1; e <= 1; e += 2) {
            d.box(gx, y, bz + e * BAY_DEEP, BAY_HALF, 0.006f, 0.09f, null, amber, DrawList.PAINT);
            d.box(gx + e * BAY_HALF, y, bz, 0.09f, 0.006f, BAY_DEEP, null, amber, DrawList.PAINT);
        }
        for (float u = -BAY_HALF + 1.2f; u < BAY_HALF - 0.5f; u += 1.5f)
            d.box(gx + u, y, bz, 0.12f, 0.005f, BAY_DEEP - 0.3f, null, amber, DrawList.PAINT);
    }

    /** A big gate in a front facing -z at (gx, fz): a dark way in, a striped frame, a door rolled half up. */
    private static void gate(DrawList d, float gx, float fz, float half, float high, int door) {
        d.box(gx, high * 0.5f, fz - 0.02f, half, high * 0.5f, 0.04f, null, 0x1E2226, DrawList.PLAIN);
        float rolled = high * 0.42f;
        d.box(gx, high - rolled * 0.5f, fz - 0.06f, half, rolled * 0.5f, 0.05f, null, door, DrawList.PAINT);
        for (float y = high - rolled + 0.15f; y < high; y += 0.3f)
            d.box(gx, y, fz - 0.12f, half, 0.025f, 0.02f, null, Street.scale(door, 0.8f), DrawList.PLAIN);
        // the frame: stripes of amber and black up its sides and along its top
        for (int e = -1; e <= 1; e += 2)
            for (float y = 0.2f; y < high; y += 0.5f)
                d.box(gx + e * (half + 0.2f), y + 0.12f, fz - 0.08f, 0.2f, 0.12f, 0.06f, null,
                        ((int) (y * 2f) & 1) == 0 ? 0xF0B03C : 0x26262A, DrawList.PAINT);
        d.box(gx, high + 0.25f, fz - 0.08f, half + 0.4f, 0.25f, 0.06f, null, 0xF0B03C, DrawList.PAINT);
    }

    /**
     * The factory: a hall of red brick the length of the row, its roof in teeth with glass on their
     * steep sides, tall windows high up, a great gate in the middle and a tall brick chimney at its
     * back corner with white bands near the top, seen from far off.
     */
    private void factory(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = 10.5f, fz = z0;
        int brick = 0xA8573F, dark = 0x7E3F2E, glassC = 0x2A3A44;
        d.box(cx, high * 0.5f, cz, hx, high * 0.5f, hz, null, brick, DrawList.STONE);
        // a darker plinth, pilasters between the bays, a band under the eaves
        d.box(cx, 0.6f, fz - 0.05f, hx + 0.05f, 0.6f, 0.07f, null, dark, DrawList.STONE);
        for (float u = x0; u <= x1 + 0.01f; u += (x1 - x0) / 8f)
            if (Math.abs(u - cx) > 4f) d.box(u, high * 0.5f, fz - 0.1f, 0.35f, high * 0.5f, 0.1f, null, dark, DrawList.STONE);
        d.box(cx, high - 0.35f, fz - 0.12f, hx + 0.1f, 0.35f, 0.14f, null, dark, DrawList.STONE);
        // tall windows high in each bay, but over the gate
        float bayW = (x1 - x0) / 8f;
        for (int k = 0; k < 8; k++) {
            float u = x0 + (k + 0.5f) * bayW;
            if (Math.abs(u - cx) < 4f) continue;
            d.box(u, 6.3f, fz - 0.04f, bayW * 0.3f, 2.4f, 0.04f, null, glassC, DrawList.GLASS);
            for (float y = 4.5f; y < 8.5f; y += 1.2f)
                d.box(u, y, fz - 0.08f, bayW * 0.3f, 0.03f, 0.03f, null, 0x2B2A2C, DrawList.PLAIN);
        }
        gate(d, cx, fz, 3f, 5.2f, 0xD9623E);
        // the roof in teeth along the row, glass on the steep side of each, facing the yard
        int teeth = 3;
        float tooth = (z1 - z0) / teeth;
        for (int k = 0; k < teeth; k++) {
            float tz0 = z0 + k * tooth;
            for (int st = 0; st < 3; st++) {
                float h = 0.7f * (st + 1), zz = tz0 + tooth * (st / 3f);
                d.box(cx, high + h * 0.5f, zz + tooth * 0.5f / 3f + tooth * (2 - st) / 6f, hx, h * 0.5f, tooth * (3 - st) / 6f,
                        null, 0x6D6A66, DrawList.PLAIN);
            }
            d.box(cx, high + 1.05f, tz0 + tooth - 0.05f, hx - 0.3f, 1.05f, 0.05f, null, glassC, DrawList.GLASS);
        }
        // the chimney, at the back on the west, banded near its top
        float chx = x0 + 4f, chz = z1 - 3f, top = 34f;
        d.box(chx, top * 0.5f, chz, 1.3f, top * 0.5f, 1.3f, null, brick, DrawList.STONE);
        d.box(chx, top - 3f, chz, 1.42f, 0.4f, 1.42f, null, 0xEDE2CC, DrawList.PLAIN);
        d.box(chx, top - 1.5f, chz, 1.42f, 0.25f, 1.42f, null, 0xE9765B, DrawList.PAINT);
        d.box(chx, top + 0.2f, chz, 1.55f, 0.2f, 1.55f, null, dark, DrawList.STONE);
        c.wall(chx - 1.3f, chz - 1.3f, chx + 1.3f, chz + 1.3f, top + 0.4f);
        c.wall(x0, z0, x1, z1, high + 0.5f);
        c.wall(x0 + 0.5f, z0 + 0.5f, x1 - 0.5f, z1 - 0.5f, high + 2.1f, high);
        c.house(cx, cz, hx, hz, high + 0.5f, 0f, -1f, 0f);
        bay(d, cx, SHORE * CELL);
    }

    /**
     * The store: a long low warehouse in pale grey-blue, its roof a barrel of steps, a coral band
     * along its front, three great turquoise doors, the middle one its gate.
     */
    private void store(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = 8f, fz = z0;
        int wall = 0xB9C3C4, teal = 0x3FAFA6, coral = 0xE9765B;
        d.box(cx, high * 0.5f, cz, hx, high * 0.5f, hz, null, wall, DrawList.PLAIN);
        d.box(cx, 0.5f, fz - 0.05f, hx + 0.05f, 0.5f, 0.07f, null, 0x8E9A9C, DrawList.STONE);
        d.box(cx, 6.6f, fz - 0.1f, hx + 0.08f, 0.45f, 0.1f, null, coral, DrawList.PAINT);
        // the barrel roof, in steps from the eaves to the ridge
        int steps = 5;
        for (int k = 0; k < steps; k++) {
            float a = (k + 1f) / steps * (float) Math.PI * 0.5f;
            float half = hz * (float) Math.cos(a - (float) Math.PI * 0.5f / steps * 0.5f), y = high + 3.2f * (float) Math.sin(a);
            d.box(cx, y - 0.35f, cz, hx + 0.3f, 0.35f, Math.max(0.6f, half), null, 0x7F9A9A, DrawList.PLAIN);
        }
        // the three doors, the middle one the gate
        for (int k = -1; k <= 1; k++) {
            float gx = cx + k * 15f;
            if (k == 0) gate(d, gx, fz, 3f, 5.2f, teal);
            else {
                d.box(gx, 2.4f, fz - 0.04f, 2.6f, 2.4f, 0.04f, null, teal, DrawList.PAINT);
                for (float y = 0.3f; y < 4.8f; y += 0.3f)
                    d.box(gx, y, fz - 0.09f, 2.6f, 0.025f, 0.02f, null, Street.scale(teal, 0.8f), DrawList.PLAIN);
            }
        }
        // small square windows high along the front
        for (float u = x0 + 3f; u < x1 - 2f; u += 3.5f) {
            if (Math.abs(u - cx) < 4.5f || Math.abs(u - cx - 15f) < 3.5f || Math.abs(u - cx + 15f) < 3.5f) continue;
            d.box(u, 4.6f, fz - 0.04f, 0.6f, 0.6f, 0.04f, null, 0x2A3A44, DrawList.GLASS);
        }
        c.wall(x0, z0, x1, z1, high + 0.5f);
        c.wall(x0 + 0.5f, cz - hz * 0.4f, x1 - 0.5f, cz + hz * 0.4f, high + 3.2f, high);
        c.house(cx, cz, hx, hz, high + 0.5f, 0f, -1f, 0f);
        bay(d, cx, SHORE * CELL);
    }

    /**
     * The quarry, the part of it in this cell: a gravel floor; to the north, east and west faces of
     * rock in three steps, each a little further back and higher; a pile of broken stone by a face.
     */
    private void quarry(Cell c, float x0, float z0) {
        DrawList d = c.boxes, small = c.detail;
        float qx0 = QUARRY_X - QUARRY_HX, qx1 = QUARRY_X + QUARRY_HX, qz0 = QUARRY_Z - QUARRY_HZ, qz1 = QUARRY_Z + QUARRY_HZ;
        float ax = Math.max(qx0, x0), bx = Math.min(qx1, x0 + CELL), az = Math.max(qz0, z0), bz = Math.min(qz1, z0 + CELL);
        if (ax >= bx || az >= bz) return;
        d.box((ax + bx) * 0.5f, 0.02f, (az + bz) * 0.5f, (bx - ax) * 0.5f, 0.02f, (bz - az) * 0.5f, null, 0xA89E8C, DrawList.GRAVEL);
        int[] rock = {0x8F8576, 0x9E9483, 0x847A6C};
        for (int k = 0; k < 3; k++) {
            float th = 2.6f + k * 0.4f, top = 3.2f * (k + 1), back = 3f + k * 3f;
            int col = rock[k % 3];
            // the north face, the length of the floor and past it
            float nz = qz1 + back;
            clipBox(c, d, qx0 - back - 3f, qx1 + back + 3f, nz - th * 0.5f, nz + th * 0.5f, top, col, x0, z0);
            // the west and east faces
            for (int s = -1; s <= 1; s += 2) {
                float fx = s < 0 ? qx0 - back : qx1 + back;
                clipBox(c, d, fx - th * 0.5f, fx + th * 0.5f, qz0 + 6f + k * 3f, nz + th * 0.5f, top, col, x0, z0);
            }
        }
        // broken stone heaped by the west face
        float hx = qx0 + 5f, hz = qz1 - 6f;
        if (hx > x0 && hx < x0 + CELL && hz > z0 && hz < z0 + CELL)
            for (int k = 0; k < 5; k++) {
                float r = 0.6f + k * 0.15f;
                small.box(hx + (k % 3) * 1.1f, r * 0.5f, hz + (k / 3) * 1.2f, r, r * 0.55f, r * 0.8f, yawFrame(k * 1.3f), 0x9C968A, DrawList.STONE);
            }
        if (hx > x0 && hx < x0 + CELL && hz > z0 && hz < z0 + CELL) c.wall(hx - 1f, hz - 1f, hx + 3.2f, hz + 2.4f, 1.2f);
    }

    /** A box from x0 to x1, z0 to z1, so high, clipped to the cell at (cx0, cz0), a wall too. */
    private static void clipBox(Cell c, DrawList d, float x0, float x1, float z0, float z1, float top, int col, float cx0, float cz0) {
        float ax = Math.max(x0, cx0), bx = Math.min(x1, cx0 + CELL), az = Math.max(z0, cz0), bz = Math.min(z1, cz0 + CELL);
        if (ax >= bx || az >= bz) return;
        d.box((ax + bx) * 0.5f, top * 0.5f, (az + bz) * 0.5f, (bx - ax) * 0.5f, top * 0.5f, (bz - az) * 0.5f, null, col, DrawList.STONE);
        c.wall(ax, az, bx, bz, top);
    }

    /**
     * The brums' workshop: a house of dark brick facing east onto the road out, two wide gates under a
     * band of slate grey, high windows in steel frames, a board over the gates with a brum's plate on
     * it, turquoise; a bay painted on the road before it.
     */
    private void workshop(Cell c, float z0, float z1, float x0, float x1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = 9f, fx = x1;
        int brick = 0x7E4A3A, dark = 0x5E3529, slate = 0x6F8A94, glass = 0x2A3A44;
        d.box(cx, high * 0.5f, cz, hx, high * 0.5f, hz, null, brick, DrawList.STONE);
        d.box(fx + 0.05f, 0.6f, cz, 0.07f, 0.6f, hz + 0.05f, null, dark, DrawList.STONE);
        d.box(fx + 0.08f, 5.6f, cz, 0.1f, 0.4f, hz + 0.08f, null, slate, DrawList.PAINT);
        d.box(cx, high + 0.25f, cz, hx + 0.2f, 0.25f, hz + 0.2f, null, 0x4E4A48, DrawList.STONE);
        // two gates, rolled half up, dark inside
        for (int s = -1; s <= 1; s += 2) {
            float gz = cz + s * hz * 0.45f;
            d.box(fx + 0.02f, 2.4f, gz, 0.04f, 2.4f, 2.8f, null, 0x1E2226, DrawList.PLAIN);
            d.box(fx + 0.07f, 3.8f, gz, 0.05f, 1f, 2.8f, null, slate, DrawList.PAINT);
            for (float y = 2.9f; y < 4.8f; y += 0.3f)
                d.box(fx + 0.1f, y, gz, 0.02f, 0.025f, 2.8f, null, Street.scale(slate, 0.8f), DrawList.PLAIN);
        }
        // high windows along the front, between and beside the gates
        for (float z = z0 + 1.5f; z < z1 - 1f; z += 2.6f)
            d.box(fx + 0.04f, 7.2f, z, 0.04f, 0.8f, 0.9f, null, glass, DrawList.GLASS);
        // the board over the middle: a brum's plate, turquoise, the sign cut in it
        d.box(fx + 0.12f, 6.9f, cz, 0.08f, 0.75f, 1.6f, null, dark, DrawList.PLAIN);
        small.box(fx + 0.2f, 6.9f, cz, 0.03f, 0.5f, 0.85f, null, Frida.SHEM, DrawList.GLOW);
        small.box(fx + 0.24f, 6.9f, cz, 0.02f, 0.32f, 0.16f, null, Frida.SIGN, DrawList.PLAIN);
        c.wall(x0, z0, x1, z1, high + 0.5f);
        c.house(cx, cz, hx, hz, high + 0.5f, 1f, 0f, 0f);
        // the bay on the road before it, along the road
        int amber = 0xF0B03C;
        float bx = WORKSHOP_BAY_X + (WORKSHOP_I + 1) * CELL, bz = workshopZ(), y = 0.006f, half = BAY_HALF + 2.5f;
        for (int e = -1; e <= 1; e += 2) {
            d.box(bx + e * BAY_DEEP, y, bz, 0.09f, 0.006f, half, null, amber, DrawList.PAINT);
            d.box(bx, y, bz + e * half, BAY_DEEP, 0.006f, 0.09f, null, amber, DrawList.PAINT);
        }
        for (float u = -half + 1.2f; u < half - 0.5f; u += 1.5f)
            d.box(bx, y, bz + u, BAY_DEEP - 0.3f, 0.005f, 0.12f, null, amber, DrawList.PAINT);
    }

    /**
     * The concert hall: pale stone, high, its cornice and a pediment over the middle; before it a
     * portico of eight columns on three broad steps; three tall doors of dark wood and glass, gilt
     * framed; boards with notes on them either side; its name over the doors in gilt letters, a
     * lyre on the pediment.
     */
    private void concertHall(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = 16f, fz = z0;
        int stone = 0xEDE4D2, shade = 0xD8CDB6, gold = 0xC9A15A, wood = 0x4A2E22, glass = 0x2A3E48;
        d.box(cx, high * 0.5f, cz + 1.5f, hx, high * 0.5f, hz - 1.5f, null, stone, DrawList.material(DrawList.FACADE, 0x30));
        d.box(cx, high + 0.3f, cz + 1.5f, hx + 0.3f, 0.3f, hz - 1.2f, null, shade, DrawList.STONE);
        // the steps the length of the portico, the portico's floor, its columns and the roof on them
        float pf = fz + 3f, half = hx * 0.55f;
        for (int k = 0; k < 3; k++)
            d.box(cx, WALK_TOP + 0.12f * (k + 0.5f), fz + 0.4f + k * 0.5f + 1f, half + 0.6f - k * 0.2f, 0.12f * (k + 0.5f), 1.4f - k * 0.45f, null, shade, DrawList.STONE);
        for (int k = 0; k < 8; k++) {
            float u = cx - half + 1f + k * (2f * half - 2f) / 7f;
            d.box(u, 0.6f + 5.2f, fz + 0.9f, 0.45f, 5.2f, 0.45f, null, stone, DrawList.STONE);
            d.box(u, 11.2f, fz + 0.9f, 0.6f, 0.2f, 0.6f, null, shade, DrawList.STONE);
            c.wall(u - 0.45f, fz + 0.45f, u + 0.45f, fz + 1.35f, 11.4f);
        }
        d.box(cx, 11.8f, fz + 1.6f, half + 0.6f, 0.4f, 1.6f, null, shade, DrawList.STONE);
        for (int k = 0; k < 4; k++)
            d.box(cx, 12.4f + k * 0.5f, fz + 0.4f, half * (1f - k * 0.24f), 0.25f, 0.3f, null, stone, DrawList.STONE);
        // the lyre on the pediment: two arms and a bar, gilt
        for (int e = -1; e <= 1; e += 2)
            small.box(cx + e * 0.35f, 13.4f, fz + 0.05f, 0.08f, 0.55f, 0.04f, null, gold, DrawList.GOLD);
        small.box(cx, 13.85f, fz + 0.05f, 0.45f, 0.06f, 0.04f, null, gold, DrawList.GOLD);
        small.box(cx, 13.05f, fz + 0.05f, 0.3f, 0.12f, 0.04f, null, gold, DrawList.GOLD);
        // the three doors, tall, under gilt frames; the name over the middle one in gilt
        for (int k = -1; k <= 1; k++) {
            float u = cx + k * 4f;
            d.box(u, WALK_TOP + 2.4f, pf - 0.02f, 1.3f, 2.4f, 0.05f, null, wood, DrawList.WOOD);
            d.box(u, WALK_TOP + 2.2f, pf - 0.06f, 0.95f, 1.9f, 0.02f, null, glass, DrawList.GLASS);
            d.box(u, WALK_TOP + 4.95f, pf - 0.08f, 1.45f, 0.12f, 0.06f, null, gold, DrawList.GOLD);
        }
        small.box(cx, WALK_TOP + 6.2f, pf - 0.08f, 3.8f, 0.3f, 0.03f, null, gold, DrawList.GOLD);
        // tall windows along the rest of the front, boards with notes either side of the portico
        for (float u = x0 + 2.5f; u < x1 - 2f; u += 3.2f) {
            if (Math.abs(u - cx) < half + 1.5f) continue;
            d.box(u, 6f, pf - 0.04f, 0.7f, 3f, 0.04f, null, glass, DrawList.GLASS);
            d.box(u, 9.2f, pf - 0.06f, 0.85f, 0.1f, 0.05f, null, shade, DrawList.STONE);
        }
        for (int e = -1; e <= 1; e += 2) {
            float u = cx + e * (half + 1.6f);
            small.box(u, WALK_TOP + 1.6f, fz + 1.2f, 0.7f, 1f, 0.05f, null, 0x2B2F3A, DrawList.PLAIN);
            small.box(u, WALK_TOP + 1.6f, fz + 1.15f, 0.6f, 0.88f, 0.02f, null, 0xF2E6C8, DrawList.PLAIN);
            for (int n = 0; n < 3; n++) {
                small.box(u - 0.25f + n * 0.25f, WALK_TOP + 1.5f + (n % 2) * 0.2f, fz + 1.12f, 0.06f, 0.05f, 0.01f, null, 0x2B2F3A, DrawList.PLAIN);
                small.box(u - 0.2f + n * 0.25f, WALK_TOP + 1.75f + (n % 2) * 0.2f, fz + 1.12f, 0.015f, 0.22f, 0.01f, null, 0x2B2F3A, DrawList.PLAIN);
            }
            c.wall(u - 0.7f, fz + 1.1f, u + 0.7f, fz + 1.3f, WALK_TOP + 2.6f);
        }
        c.wall(x0, pf, x1, z1, high + 0.6f);
        c.house(cx, cz, hx, hz, high + 0.6f, 0f, -1f, 0f);
    }

    /**
     * The vets': a white house of two storeys, low and long, a turquoise band along it and a flat
     * roof; in its middle glass doors under a turquoise awning, over them a board with a paw on it;
     * wide windows either side, a bench by the door. The bay before it on the street.
     */
    private void clinic(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float high = 7.4f, fz = z0;
        int white = 0xEEF0EA, teal = 0x2FA39A, glass = 0x2A3E48, plinth = 0xC9CEC8;
        d.box(cx, high * 0.5f, cz, hx, high * 0.5f, hz, null, white, DrawList.PLAIN);
        d.box(cx, 0.4f, fz - 0.05f, hx + 0.05f, 0.4f, 0.07f, null, plinth, DrawList.STONE);
        d.box(cx, 3.55f, fz - 0.06f, hx + 0.06f, 0.18f, 0.08f, null, teal, DrawList.PAINT);
        d.box(cx, high + 0.2f, cz, hx + 0.15f, 0.2f, hz + 0.15f, null, 0xD6DAD4, DrawList.STONE);
        // the doors, glass in a dark frame, the awning over them
        d.box(cx, 1.4f, fz - 0.03f, 1.9f, 1.4f, 0.04f, null, 0x1E2428, DrawList.PLAIN);
        d.box(cx, 1.4f, fz - 0.07f, 1.75f, 1.3f, 0.03f, null, glass, DrawList.GLASS);
        d.box(cx, 1.4f, fz - 0.09f, 0.04f, 1.3f, 0.02f, null, 0x1E2428, DrawList.PLAIN);
        d.box(cx, 3.05f, fz - 0.9f, 2.6f, 0.08f, 0.9f, null, teal, DrawList.AWNING);
        // the board over the door: white, a turquoise rim, a paw on it
        float by = 5.4f;
        d.box(cx, by, fz - 0.12f, 1.6f, 1.05f, 0.08f, null, teal, DrawList.PAINT);
        d.box(cx, by, fz - 0.2f, 1.45f, 0.9f, 0.03f, null, 0xFAFAF4, DrawList.PLAIN);
        small.ellipsoid(cx, by - 0.2f, fz - 0.24f, 0.38f, 0.32f, 0.02f, null, teal, DrawList.MATTE);
        for (int k = 0; k < 4; k++) {
            float a = -0.95f + k * 0.63f;
            small.ellipsoid(cx + (float) Math.sin(a) * 0.55f, by + 0.15f + (float) Math.cos(a) * 0.32f, fz - 0.24f,
                    0.14f, 0.17f, 0.02f, null, teal, DrawList.MATTE);
        }
        // wide windows along both floors, either side of the door
        for (float u = x0 + 3.2f; u < x1 - 2.5f; u += 5f) {
            if (Math.abs(u - cx) < 4.2f) continue;
            for (int fl = 0; fl < 2; fl++) {
                float wy = 1.7f + fl * 3.5f;
                d.box(u, wy, fz - 0.04f, 1.9f, 0.85f, 0.04f, null, glass, DrawList.GLASS);
                d.box(u, wy - 0.95f, fz - 0.09f, 2.05f, 0.06f, 0.08f, null, plinth, DrawList.STONE);
            }
        }
        // a bench by the door, for those who wait
        bench(c, cx + 4.6f, WALK_TOP, fz - 0.9f, (float) Math.PI);
        c.wall(cx + 4.6f - 0.9f, fz - 1.2f, cx + 4.6f + 0.9f, fz - 0.6f, WALK_TOP + 0.48f);
        c.wall(x0, z0, x1, z1, high + 0.4f);
        c.house(cx, cz, hx, hz, high + 0.4f, 0f, -1f, 0f);
        bay(d, cx, CLINIC_J * CELL);
    }

    /**
     * Home, built by hand and never grown, the same in every town: one large house the length of the
     * row, looking over the shore road to the sea. Cream stone, eight storeys on a plinth of darker
     * stone with tall dark windows, a coral band over the ground floor, a turquoise cornice. In the
     * middle, over the door, a tower two storeys higher, a balcony across it, a stepped turquoise cap
     * and an amber lamp on top. A way in at the front under a coral awning, a hall through to the yard.
     */
    private void oldHouse(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f;
        float height = 3.3f * (HOME_STOREYS + 1) + 0.5f, fz = z0;
        int cream = 0xF1E6CF, plinth = 0xD9CBB0, coral = 0xE9765B, teal = 0x3FAFA6, amber = 0xFFB347;
        d.box(cx, height * 0.5f, cz, hx, height * 0.5f, hz, null, cream, DrawList.material(DrawList.FACADE, 0x4D));
        // the plinth, the ground floor's tall windows in it, either side of the door
        float pl = 4.2f;
        d.box(cx, pl * 0.5f, fz - 0.06f, hx + 0.06f, pl * 0.5f, 0.08f, null, plinth, DrawList.STONE);
        for (float u = x0 + 3f; u < x1 - 2f; u += 4.2f) {
            if (Math.abs(u - cx) < 3.4f) continue;
            d.box(u, 2.15f, fz - 0.15f, 1f, 1.55f, 0.04f, null, 0x24323A, DrawList.GLASS);
            d.box(u, 3.85f, fz - 0.17f, 1.15f, 0.12f, 0.05f, null, cream, DrawList.STONE);
        }
        d.box(cx, pl + 0.22f, fz - 0.16f, hx + 0.08f, 0.22f, 0.12f, null, coral, DrawList.PAINT);
        // pilasters at the corners, the turquoise cornice
        for (int e = -1; e <= 1; e += 2)
            d.box(cx + e * (hx - 0.35f), height * 0.5f, fz - 0.1f, 0.4f, height * 0.5f, 0.12f, null, plinth, DrawList.STONE);
        d.box(cx, height - 0.3f, fz - 0.2f, hx + 0.12f, 0.3f, 0.26f, null, teal, DrawList.PLAIN);
        roof(c, d, null, cx, height, cz, hx, hz, 0x4D);
        // the tower over the door: two storeys more, a balcony across it, a stepped cap, the lamp
        float tw = 5.6f, td = 4.4f, tz = fz + td, top = height + HOME_TOWER;
        d.box(cx, (height + top) * 0.5f, tz, tw, (top - height) * 0.5f, td, null, 0xE8DBC1, DrawList.PLAIN);
        // its windows, two storeys of them: three tall ones across the front, two down each side
        for (int st = 0; st < 2; st++) {
            float wy = height + 1.9f + st * 3.3f;
            for (int k = -1; k <= 1; k++) {
                d.box(cx + k * 3.4f, wy, fz - 0.03f, 0.7f, 1.15f, 0.04f, null, 0x24323A, DrawList.GLASS);
                d.box(cx + k * 3.4f, wy + 1.3f, fz - 0.07f, 0.85f, 0.12f, 0.06f, null, plinth, DrawList.STONE);
                for (int e = -1; e <= 1; e += 2)
                    d.box(cx + k * 3.4f + e * 1.05f, wy, fz - 0.08f, 0.3f, 1.1f, 0.05f, null, teal, DrawList.PAINT);
            }
            for (int e = -1; e <= 1; e += 2)
                for (int k = -1; k <= 1; k += 2)
                    d.box(cx + e * (tw + 0.03f), wy, tz + k * 1.9f, 0.04f, 1.15f, 0.6f, null, 0x24323A, DrawList.GLASS);
        }
        d.box(cx, top - 0.3f, fz - 0.2f, tw + 0.12f, 0.3f, 0.26f, null, teal, DrawList.PLAIN);
        float cap = top + 0.5f;
        d.box(cx, cap + 0.35f, tz, tw + 0.3f, 0.35f, td + 0.3f, null, teal, DrawList.TILES);
        d.box(cx, cap + 1.2f, tz, tw - 1.2f, 0.5f, td - 1.2f, null, teal, DrawList.TILES);
        d.box(cx, cap + 2.0f, tz, tw - 2.6f, 0.3f, td - 2.6f, null, teal, DrawList.TILES);
        d.box(cx, cap + 2.75f, tz, 0.55f, 0.45f, 0.55f, null, amber, DrawList.GLOW);
        d.box(cx, cap + 3.3f, tz, 0.75f, 0.1f, 0.75f, null, 0x2B2F3A, DrawList.PLAIN);
        // the balcony on the second storey: a slab and an iron rail
        float by = 3.3f * 2f + 1.1f;
        small.box(cx, by, fz - 0.7f, tw, 0.08f, 0.7f, null, plinth, DrawList.STONE);
        small.box(cx, by + 1.0f, fz - 1.35f, tw, 0.03f, 0.03f, null, 0x2B2A2C, DrawList.RAIL);
        for (float u = -tw; u <= tw + 0.01f; u += 0.5f)
            small.box(cx + u, by + 0.5f, fz - 1.35f, 0.02f, 0.5f, 0.02f, null, 0x2B2A2C, DrawList.RAIL);
        // the balcony on the fifth storey: a deep slab on brackets, an iron rail, its door left ajar,
        // gold light through the gap
        float sy = SECRET_Y, sh = SECRET_HALF, sd = SECRET_DEEP;
        d.box(cx, sy - 0.12f, fz - sd * 0.5f, sh, 0.12f, sd * 0.5f, null, plinth, DrawList.STONE);
        for (int e = -1; e <= 1; e += 2)
            d.box(cx + e * (sh - 0.4f), sy - 0.55f, fz - 0.45f, 0.14f, 0.32f, 0.45f, null, plinth, DrawList.STONE);
        small.box(cx, sy + 1.0f, fz - sd + 0.04f, sh, 0.03f, 0.03f, null, 0x2B2A2C, DrawList.RAIL);
        for (int e = -1; e <= 1; e += 2)
            small.box(cx + e * (sh - 0.03f), sy + 1.0f, fz - sd * 0.5f, 0.03f, 0.03f, sd * 0.5f, null, 0x2B2A2C, DrawList.RAIL);
        for (float u = -sh; u <= sh + 0.01f; u += 0.42f)
            small.box(cx + u, sy + 0.5f, fz - sd + 0.04f, 0.02f, 0.5f, 0.02f, null, 0x2B2A2C, DrawList.RAIL);
        d.box(cx, sy + 1.25f, fz - 0.03f, 0.95f, 1.25f, 0.04f, null, 0x24323A, DrawList.GLASS);
        d.box(cx + 0.18f, sy + 1.2f, fz - 0.05f, 0.14f, 1.15f, 0.03f, null, 0xFFC95A, DrawList.GLOW);
        d.box(cx - 0.42f, sy + 1.2f, fz - 0.22f, 0.5f, 1.15f, 0.04f, yawFrame(-0.6f), 0x2F8A86, DrawList.PAINT);
        d.box(cx, sy + 2.62f, fz - 0.1f, 1.15f, 0.12f, 0.1f, null, plinth, DrawList.STONE);
        c.wall(cx - sh, fz - sd, cx + sh, fz, sy, sy - 0.3f);
        // the door: a frame of stone, the coral awning over it, big pots either side
        liftDoor(d, cx, fz, true, -1);
        liftDoor(d, cx, z1, true, 1);
        Street.awning(small, true, fz, -1, 3.4f, cx, 1.8f, coral, true);
        // an overhang, as the balcony is: she walks under it, the eye behind her stops short of it
        c.wall(cx - 1.8f, fz - 1.05f, cx + 1.8f, fz, 3.4f, 2.75f);
        Random deco = new Random(0x4D0EL);
        for (int e = -1; e <= 1; e += 2) {
            float px = cx + e * 2.6f, pz = fz - 0.55f;
            Garden.pot(small, deco, px, WALK_TOP, pz, 0.42f);
            c.wall(px - 0.4f, pz - 0.4f, px + 0.4f, pz + 0.4f, WALK_TOP + 0.9f);
        }
        // to stand on: the house, its roof, the tower
        c.wall(x0, z0, x1, z1, height + 0.5f);
        c.wall(x0 + 1.3f, cz - Math.max(0.4f, hz - 1.3f), x1 - 1.3f, cz + Math.max(0.4f, hz - 1.3f), height + 0.75f, height);
        c.wall(cx - tw, tz - td, cx + tw, tz + td, cap + 0.7f, height);
        c.house(cx, cz, hx, hz, height + 0.5f, 0f, -1f, DOOR);
    }

    /**
     * Home's block: no rows of houses round it; home stands alone, back from the street behind its
     * forecourt (paving, a low hedge along the pavement with a gap for the way in and a wider one
     * for the pickup, its place marked out), paths of paving down either side, and behind it a
     * garden to the next street (a lawn, hedges along its sides, trees, beds, benches, a gap in
     * its north hedge for the way in from that street).
     */
    private void homeBlock(Cell c, Random rnd, float x0, float z0) {
        DrawList d = c.boxes, small = c.detail;
        float y = WALK_TOP, bx0 = x0 + FRONT, bx1 = x0 + CELL - FRONT, bz0 = z0 + FRONT, bz1 = z0 + CELL - FRONT;
        float hx0 = HOME_DOOR_X - HOME_HX, hx1 = HOME_DOOR_X + HOME_HX, hz0 = HOME_DOOR_Z, hz1 = HOME_DOOR_Z + 2f * HOME_HZ;
        int paving = 0xC9BFAE, grass = 0x5E8A44, white = 0xF4EEE2;
        // the forecourt and the paths either side of the house
        d.box((bx0 + bx1) * 0.5f, y + 0.005f, (bz0 + hz0) * 0.5f, (bx1 - bx0) * 0.5f, 0.006f, (hz0 - bz0) * 0.5f, null, paving, DrawList.PAVING);
        for (int e = -1; e <= 1; e += 2) {
            float a = e < 0 ? bx0 : hx1, b = e < 0 ? hx0 : bx1;
            d.box((a + b) * 0.5f, y + 0.005f, (hz0 + hz1) * 0.5f, (b - a) * 0.5f, 0.006f, (hz1 - hz0) * 0.5f, null, paving, DrawList.PAVING);
        }
        // the pickup's place: a white frame on the paving
        float px = HOME_PICKUP_X, pz = HOME_J * CELL + FRONT + HOME_SET_BACK * 0.5f;
        for (int e = -1; e <= 1; e += 2) {
            small.box(px + e * 2.9f, y + 0.014f, pz, 0.06f, 0.004f, 1.5f, null, white, DrawList.PAINT);
            small.box(px, y + 0.014f, pz + e * 1.5f, 2.9f, 0.004f, 0.06f, null, white, DrawList.PAINT);
        }
        // the hedge along the pavement: gaps for the door's way and for the pickup
        float hedgeZ = bz0 + 0.45f, gap = 2.2f, wide = 3.6f;
        float[] cuts = {bx0, HOME_DOOR_X - gap, HOME_DOOR_X + gap, px - wide, px + wide, bx1};
        for (int k = 0; k < cuts.length; k += 2) {
            if (cuts[k + 1] - cuts[k] < 0.3f) continue;
            Garden.hedge(d, cuts[k], hedgeZ - 0.4f, cuts[k + 1], hedgeZ + 0.4f, y, 0.9f);
            c.wall(cuts[k], hedgeZ - 0.4f, cuts[k + 1], hedgeZ + 0.4f, y + 0.9f);
        }
        // the garden: the lawn; hedges along its sides and its north edge (a gap in the middle)
        d.box((bx0 + bx1) * 0.5f, y + 0.008f, (hz1 + bz1) * 0.5f, (bx1 - bx0) * 0.5f, 0.008f, (bz1 - hz1) * 0.5f, null, grass, DrawList.GRASS);
        for (int e = -1; e <= 1; e += 2) {
            float hx = e < 0 ? bx0 + 0.45f : bx1 - 0.45f;
            Garden.hedge(d, hx - 0.4f, hz0, hx + 0.4f, bz1, y, 1.1f);
            c.wall(hx - 0.4f, hz0, hx + 0.4f, bz1, y + 1.1f);
            float a = HOME_DOOR_X + e * 2.2f, b = e < 0 ? bx0 : bx1;
            Garden.hedge(d, Math.min(a, b), bz1 - 0.85f, Math.max(a, b), bz1 - 0.05f, y, 1.1f);
            c.wall(Math.min(a, b), bz1 - 0.85f, Math.max(a, b), bz1 - 0.05f, y + 1.1f);
        }
        // a path of paving from the back door to the gap, trees and beds either side, benches
        d.box(HOME_DOOR_X, y + 0.012f, (hz1 + bz1) * 0.5f, 1.2f, 0.006f, (bz1 - hz1) * 0.5f, null, paving, DrawList.PAVING);
        Random deco = new Random(0x40EEL);
        for (int e = -1; e <= 1; e += 2) {
            for (int k = 0; k < 2; k++) {
                float tx = HOME_DOOR_X + e * (9f + k * 9f), tz = hz1 + 6f + (k & 1) * 4f;
                tree(c, small, deco, tx, y, tz, 7.5f + deco.nextFloat() * 2f);
                c.wall(tx - 0.3f, tz - 0.3f, tx + 0.3f, tz + 0.3f);
            }
            float bx = HOME_DOOR_X + e * 5f;
            Garden.bed(d, deco, bx - 1.8f, hz1 + 2f, bx + 1.8f, hz1 + 3.6f, y);
            c.wall(bx - 1.8f, hz1 + 2f, bx + 1.8f, hz1 + 3.6f, y + 0.4f);
            bench(c, HOME_DOOR_X + e * 3.2f, y, bz1 - 4f, e < 0 ? (float) (Math.PI / 2) : (float) (-Math.PI / 2));
            c.wall(HOME_DOOR_X + e * 3.2f - 0.3f, bz1 - 4.9f, HOME_DOOR_X + e * 3.2f + 0.3f, bz1 - 3.1f, y + 0.48f);
        }
        villa(c, hx0, hx1, hz0, hz1);
    }

    /**
     * Home from the town: its walls are its rooms' own (see Rooms.home; the town draws them from
     * World, the rooms in them), so here only what dresses them outside: a plinth of stone round
     * its foot, stone pilasters at its corners, a teal cornice round its top, a parapet round its
     * flat roof and on the roof the pad (a ring and its letter, lamps round it) for a copter to come
     * down on; round each window (where the rooms have theirs, open) a white frame, teal shutters,
     * a stone sill; the doorways' stone frames, a coral awning over the front one, pots either
     * side, a balcony over it.
     */
    private void villa(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f, h = HOME_HIGH;
        int plinth = 0xD9CBB0, coral = 0xE9765B, teal = 0x3FAFA6, rail = 0x2B2A2C, white = 0xF4EEE2, stone = 0xD8CDB8;
        float dh = Indoors.DOOR_HALF + 0.1f;
        // the plinth round its foot, broken by the doorways; the cornice round its top
        for (int e = -1; e <= 1; e += 2) {
            float fz = e < 0 ? z0 : z1;
            for (int s = -1; s <= 1; s += 2) {
                float a = cx + s * dh, b = cx + s * (hx + 0.06f);
                d.box((a + b) * 0.5f, 0.45f, fz + e * 0.04f, Math.abs(b - a) * 0.5f, 0.45f, 0.06f, null, plinth, DrawList.STONE);
            }
            d.box(cx + e * (hx + 0.04f), 0.45f, cz, 0.06f, 0.45f, hz + 0.06f, null, plinth, DrawList.STONE);
            d.box(cx, h - 0.3f, fz + e * 0.1f, hx + 0.16f, 0.3f, 0.12f, null, teal, DrawList.PLAIN);
            d.box(cx + e * (hx + 0.1f), h - 0.3f, cz, 0.12f, 0.3f, hz + 0.16f, null, teal, DrawList.PLAIN);
            d.box(cx, 5.25f, fz + e * 0.06f, hx + 0.08f, 0.08f, 0.08f, null, plinth, DrawList.STONE);
            d.box(cx + e * (hx + 0.06f), 5.25f, cz, 0.08f, 0.08f, hz + 0.08f, null, plinth, DrawList.STONE);
        }
        for (int a = -1; a <= 1; a += 2)
            for (int b = -1; b <= 1; b += 2) d.box(cx + a * (hx - 0.2f), h * 0.5f, cz + b * (hz - 0.2f), 0.32f, h * 0.5f, 0.32f, null, plinth, DrawList.STONE);
        // the roof: gravel over the rooms' ceiling, the parapet round it, the pad in its middle
        d.box(cx, h + 0.02f, cz, hx - 0.1f, 0.02f, hz - 0.1f, null, 0x8C8A84, DrawList.GRAVEL);
        for (int e = -1; e <= 1; e += 2) {
            d.box(cx, h + 0.45f, cz + e * (hz - 0.1f), hx, 0.45f, 0.12f, null, plinth, DrawList.STONE);
            d.box(cx + e * (hx - 0.1f), h + 0.45f, cz, 0.12f, 0.45f, hz, null, plinth, DrawList.STONE);
            c.wall(x0, cz + e * (hz - 0.1f) - 0.12f, x1, cz + e * (hz - 0.1f) + 0.12f, h + 0.9f, h);
            c.wall(cx + e * (hx - 0.1f) - 0.12f, z0, cx + e * (hx - 0.1f) + 0.12f, z1, h + 0.9f, h);
        }
        d.box(cx, h + 0.05f, cz, 7.2f, 0.02f, 7.2f, null, 0x3E4246, DrawList.PAINT);
        pad(small, cx, h + 0.08f, cz, 6f, 0xF2EAD6);
        for (int k = 0; k < 12; k++) {
            float a = k * 0.5236f;
            small.box(cx + (float) Math.sin(a) * 6.9f, h + 0.12f, cz + (float) Math.cos(a) * 6.9f, 0.12f, 0.06f, 0.12f, null, 0xFFB347, DrawList.GLOW);
        }
        // round each window, outside: in the rooms' frame across runs from +x to -x, in runs +z
        for (float[] k : Rooms.HOME_WINDOWS) {
            int on = (int) k[0];
            float y0 = Indoors.FLOOR + k[3], y1 = Indoors.FLOOR + k[4], hw = k[2];
            if (on == Rooms.ON_FRONT) window(d, cx - k[1], z0, true, -1, y0, y1, hw);
            else if (on == Rooms.ON_BACK) window(d, cx - k[1], z1, true, 1, y0, y1, hw);
            else if (on == Rooms.ON_LEFT) window(d, z0 + k[1], x1, false, 1, y0, y1, hw);
            else window(d, z0 + k[1], x0, false, -1, y0, y1, hw);
        }
        // a balcony over the door, its slab and its rail
        small.box(cx, 5.45f, z0 - 0.75f, 3.2f, 0.08f, 0.75f, null, plinth, DrawList.STONE);
        small.box(cx, 6.4f, z0 - 1.45f, 3.2f, 0.03f, 0.03f, null, rail, DrawList.RAIL);
        for (float u = -3.2f; u <= 3.21f; u += 0.4f) small.box(cx + u, 5.95f, z0 - 1.45f, 0.02f, 0.45f, 0.02f, null, rail, DrawList.RAIL);
        // the doorways' stone frames, outside (the doors are the rooms', glass, that open as she comes)
        for (int e = -1; e <= 1; e += 2) {
            float fz = e < 0 ? z0 : z1;
            for (int s = -1; s <= 1; s += 2)
                d.box(cx + s * (Indoors.DOOR_HALF + 0.12f), WALK_TOP + DOOR_HIGH * 0.5f + 0.1f, fz + e * 0.07f, 0.12f, DOOR_HIGH * 0.5f + 0.25f, 0.07f, null, stone, DrawList.STONE);
            d.box(cx, WALK_TOP + DOOR_HIGH + 0.45f, fz + e * 0.07f, Indoors.DOOR_HALF + 0.24f, 0.12f, 0.07f, null, stone, DrawList.STONE);
        }
        Street.awning(small, true, z0, -1, 3.4f, cx, 1.8f, coral, true);
        c.wall(cx - 1.8f, z0 - 1.05f, cx + 1.8f, z0, 3.4f, 2.75f);
        Random deco = new Random(0x4D1EL);
        for (int e = -1; e <= 1; e += 2) {
            float qx = cx + e * 2.6f, qz = z0 - 0.55f;
            Garden.pot(small, deco, qx, WALK_TOP, qz, 0.42f);
            c.wall(qx - 0.4f, qz - 0.4f, qx + 0.4f, qz + 0.4f, WALK_TOP + 0.9f);
        }
        // to stand on: the house, its roof; its doors
        c.wall(x0, z0, x1, z1, h + 0.05f);
        c.house(cx, cz, hx, hz, h + 0.05f, 0f, -1f, DOOR);
    }

    /**
     * Round a window in a wall at v (a front if alongX, else a side), its middle at u along the
     * wall, facing side, open (the glass is the rooms' to have or not): a white frame round the
     * hole, teal shutters either side, a stone sill.
     */
    private static void window(DrawList d, float u, float v, boolean alongX, int side, float y0, float y1, float hw) {
        float yc = (y0 + y1) * 0.5f, hy = (y1 - y0) * 0.5f, out = v + side * 0.03f;
        for (int e = -1; e <= 1; e += 2) {
            boxUV(d, u + e * (hw + 0.07f), yc, out, 0.07f, hy + 0.14f, 0.04f, alongX, 0xF4EEE2, DrawList.PLAIN);
            boxUV(d, u + e * (hw + 0.5f), yc, out, 0.3f, hy, 0.03f, alongX, 0x3FAFA6, DrawList.PAINT);
        }
        boxUV(d, u, y1 + 0.07f, out, hw + 0.14f, 0.07f, 0.04f, alongX, 0xF4EEE2, DrawList.PLAIN);
        boxUV(d, u, y0 - 0.07f, v + side * 0.12f, hw + 0.2f, 0.07f, 0.14f, alongX, 0xD9CBB0, DrawList.STONE);
    }

    /**
     * The shop: the north row of its block, across the street from home's door: one tall storey of
     * glass on the street in a frame of white, shelves and their goods seen through it, the doors in
     * the middle, a deep orange band over the front with a great basket on it in white; trolleys
     * in a row by the door, a bench, planters.
     */
    private void shop(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, cz = (z0 + z1) * 0.5f, hx = (x1 - x0) * 0.5f, hz = (z1 - z0) * 0.5f, h = 7f, y = WALK_TOP, fz = z1;
        int white = 0xF2EEE6, orange = 0xF08A2E, glass = 0x2A3A44, steel = 0xB2BAC0;
        d.box(cx, h * 0.5f, cz, hx, h * 0.5f, hz, null, white, DrawList.PLAIN);
        roof(c, d, null, cx, h, cz, hx, hz, 0x2F);
        // the glass front, its posts; the goods on shelves behind it
        float gh = 4.2f;
        d.box(cx, y + gh * 0.5f, fz + 0.05f, hx - 1f, gh * 0.5f, 0.04f, null, glass, DrawList.GLASS);
        for (float u = -hx + 1f; u <= hx - 0.99f; u += 3.5f) d.box(cx + u, y + gh * 0.5f, fz + 0.09f, 0.08f, gh * 0.5f, 0.05f, null, white, DrawList.PLAIN);
        int[] goods = {0xE0644A, 0xF6B54A, 0x2F8A86, 0xF1E8D4, 0x6E9DB0, 0x9DBB8E, 0xB5352C};
        for (float u = -hx + 2.5f; u < hx - 2f; u += 3.5f) {
            if (Math.abs(u) < 3f) continue;
            for (int r = 0; r < 3; r++) d.box(cx + u, y + 0.8f + r * 1.0f, fz - 1.2f, 1.4f, 0.3f, 0.35f, null, goods[(int) Math.abs(u * 3 + r) % goods.length], DrawList.PLAIN);
        }
        // the doors, the band over the front, the basket on it
        d.box(cx, y + 1.4f, fz + 0.1f, 1.6f, 1.4f, 0.05f, null, 0x1D2730, DrawList.GLASS);
        d.box(cx, y + gh + 0.9f, fz + 0.25f, hx + 0.1f, 0.9f, 0.25f, null, orange, DrawList.PAINT);
        float by = y + gh + 0.9f, bz = fz + 0.52f;
        small.box(cx, by - 0.2f, bz, 1.1f, 0.35f, 0.03f, null, white, DrawList.PAINT);
        small.box(cx, by + 0.3f, bz, 1.35f, 0.08f, 0.03f, null, white, DrawList.PAINT);
        for (int e = -1; e <= 1; e += 2) small.box(cx + e * 0.75f, by + 0.5f, bz, 0.06f, 0.25f, 0.03f, yawFrame(0f), white, DrawList.PAINT);
        small.box(cx, by + 0.72f, bz, 0.8f, 0.06f, 0.03f, null, white, DrawList.PAINT);
        for (int e = -1; e <= 1; e += 2) small.box(cx + e * 0.6f, by - 0.7f, bz, 0.14f, 0.14f, 0.03f, null, white, DrawList.PAINT);
        // the trolleys in a row by the door, nested
        for (int k = 0; k < 5; k++) {
            float tx = cx + 3.4f + k * 0.35f, tz = fz + 1.4f;
            small.box(tx, y + 0.75f, tz, 0.3f, 0.3f, 0.45f, null, steel, DrawList.RAIL);
            small.box(tx, y + 1.1f, tz + 0.5f, 0.3f, 0.03f, 0.03f, null, 0xE0644A, DrawList.PAINT);
            small.box(tx, y + 0.2f, tz, 0.25f, 0.06f, 0.4f, null, 0x3A3B40, DrawList.PLAIN);
        }
        c.wall(cx + 3.0f, fz + 0.8f, cx + 5.2f, fz + 2.1f, y + 1.2f);
        bench(c, cx - 5f, y, fz + 1.5f, 0f);
        c.wall(cx - 5.9f, fz + 1.2f, cx - 4.1f, fz + 1.8f, y + 0.48f);
        Random deco = new Random(0x5409L);
        for (int e = -1; e <= 1; e += 2) {
            float qx = cx + e * (hx - 2f), qz = fz + 1.1f;
            Garden.pot(small, deco, qx, y, qz, 0.5f);
            c.wall(qx - 0.5f, qz - 0.5f, qx + 0.5f, qz + 0.5f, y + 1f);
        }
        c.wall(x0, z0, x1, z1, h + 0.5f);
        c.house(cx, cz, hx, hz, h + 0.5f, 0f, 1f, 0f);
    }

    /**
     * The taxis' stand: the north row of its block left open, a lot on the street, paved, its
     * bays painted, the taxis waiting in them, a long shelter of glass on posts along its back,
     * the dispatcher's booth at its end, its roof a sign in black and yellow squares, a bench.
     */
    private void taxis(Cell c, float x0, float x1, float z0, float z1) {
        DrawList d = c.boxes, small = c.detail;
        float cx = (x0 + x1) * 0.5f, hx = (x1 - x0) * 0.5f, y = WALK_TOP;
        int white = 0xF4EEE2, yellow = Cars.TAXI_YELLOW, black = 0x1E1E22, steel = 0xB2BAC0;
        // the lot paved as the pavement is, a wall along its back shutting the yard in
        d.box(cx, 1.4f, z0 + 0.2f, hx, 1.4f, 0.2f, null, 0xD9CBB0, DrawList.STONE);
        c.wall(x0, z0, x1, z0 + 0.4f, 2.8f);
        // the bays, the taxis in them facing the street
        for (int k = 0; k < 7; k++) {
            float bx = x0 + 4f + k * 5.4f;
            small.box(bx - 2.7f, y + 0.012f, z0 + 7f, 0.06f, 0.004f, 3f, null, white, DrawList.PAINT);
            if (k == 6) small.box(bx + 2.7f, y + 0.012f, z0 + 7f, 0.06f, 0.004f, 3f, null, white, DrawList.PAINT);
            if (k == 2 || k == 5) continue;
            Cars.car(c.parked, bx, z0 + 7f, 0f, black, true, Cars.TAXI);
            c.wall(bx - 0.95f, z0 + 7f - Cars.half(Cars.TAXI) - 0.1f, bx + 0.95f, z0 + 7f + Cars.half(Cars.TAXI) + 0.1f, Cars.top(Cars.TAXI));
        }
        // the shelter: posts, a glass roof, a bench under it
        float sz = z0 + 1.6f, sh = 2.8f;
        for (float u = -hx + 3f; u <= 0.01f; u += 4f) {
            small.box(cx + u, sh * 0.5f, sz + 0.8f, 0.06f, sh * 0.5f, 0.06f, null, steel, DrawList.POLISHED);
            c.wall(cx + u - 0.1f, sz + 0.7f, cx + u + 0.1f, sz + 0.9f, sh);
        }
        small.box(cx - hx * 0.5f + 1.5f, sh, sz, hx * 0.5f + 1.6f, 0.04f, 1.1f, null, 0x9FD8E0, DrawList.GLASS);
        bench(c, cx - 8f, y - 0.14f, sz - 0.3f, 0f);
        c.wall(cx - 8.9f, sz - 0.6f, cx - 7.1f, sz, 0.48f);
        // the booth at the lot's east end: white, a window on the lot, its roof a chequered sign
        float kx = x1 - 3f, kz = z0 + 2.4f;
        d.box(kx, 1.4f, kz, 1.6f, 1.4f, 1.4f, null, white, DrawList.PLAIN);
        d.box(kx - 0.4f, 1.6f, kz + 1.42f, 0.8f, 0.6f, 0.03f, null, 0x24323A, DrawList.GLASS);
        d.box(kx, 2.95f, kz, 1.8f, 0.15f, 1.6f, null, black, DrawList.PLAIN);
        for (int a = -2; a < 2; a++)
            for (int e = -1; e <= 1; e += 2) {
                small.box(kx + (a + 0.5f) * 0.9f, 3.35f, kz + e * 1.62f, 0.45f, 0.25f, 0.02f, null, ((a + e) & 1) == 0 ? yellow : black, DrawList.PAINT);
                small.box(kx + (a + 0.5f) * 0.9f, 3.85f, kz + e * 1.62f, 0.45f, 0.25f, 0.02f, null, ((a + e) & 1) == 0 ? black : yellow, DrawList.PAINT);
            }
        d.box(kx, 3.6f, kz, 1.8f, 0.5f, 1.6f, null, yellow, DrawList.PAINT);
        c.wall(kx - 1.6f, kz - 1.4f, kx + 1.6f, kz + 1.4f, 3.1f);
    }

    /** Whether the yard of cell (i, j)'s block is open to the street by an arch: one in two. */
    boolean arched(int i, int j) {
        return ((hash(i, j, 8) >>> 17) & 1) == 0;
    }

    /**
     * The ways into the yard of the block in cell (i, j) from its streets, at most max of them,
     * seven floats each into out: where to stand on the pavement before it (x, z), where it comes
     * out in the yard (x, z), whether it is through a house's hall (1) or under an arch (0), and the
     * way the front it is in faces (x, z). Through a hall: a house with its door on the street and
     * another on the yard (a lift's or a hall's). How many; none for a cell that is no block.
     */
    int yardWays(int i, int j, float[] out, int max) {
        if (sea(j) || country(j) || oldTown(i, j) || square(i, j) || i == PORT_I && j == PORT_J) return 0;
        int n = 0;
        float x0 = i * CELL, z0 = j * CELL, mid = CELL * 0.5f, a = FRONT, b = CELL - FRONT;
        // the arch, as block makes it: through the north row, or the south one
        boolean mine = i == OLD_I && j == OLD_J, archive = i == ARCHIVE_I && j == ARCHIVE_J, home = i == HOME_I && j == HOME_J;
        boolean works = j == SHORE && (i == FACTORY_I || i == STORE_I);
        boolean open = arched(i, j) || archive || home, north = mine || works || archive || home || ((hash(i, j, 8) >>> 9) & 1) == 0;
        // the shop's and the taxis' yards shut in; home's garden open to the street north of it
        if (i == SHOP_I && j == SHOP_J || i == TAXI_I && j == TAXI_J) open = false;
        boolean ownSouth = mine || works || home || i == CLINIC_I && j == CLINIC_J || i == CONCERT_I && j == CONCERT_J
                || i == TOWNHALL_I && j == TOWNHALL_J || archive;
        if (open && (north || !ownSouth) && n < max) {
            float s = north ? 1f : -1f, front = north ? z0 + b : z0 + a, back = north ? z0 + b - DEPTH : z0 + a + DEPTH;
            n = way(out, n, x0 + mid, front + s * 1.2f, x0 + mid, back - s * 1.5f, 0f, 0f, s);
        }
        Cell c = cell(i, j);
        for (int k = 0; k < c.houseN && n < max; k++) {
            int o = k * Cell.HOUSE;
            float kind = c.houses[o + 7];
            if (kind != LIFT && kind != DOOR || !backClear(c.houses, o)) continue;
            float nx = c.houses[o + 5], nz = c.houses[o + 6];
            float half = Math.abs(nx) * c.houses[o + 2] + Math.abs(nz) * c.houses[o + 3];
            float cx = c.houses[o], cz = c.houses[o + 1];
            n = way(out, n, cx + nx * (half + 0.9f), cz + nz * (half + 0.9f), cx - nx * (half + 1.2f), cz - nz * (half + 1.2f), 1f, nx, nz);
        }
        return n;
    }

    private static int way(float[] out, int n, float sx, float sz, float yx, float yz, float hall, float nx, float nz) {
        int o = n * 7;
        out[o] = sx;
        out[o + 1] = sz;
        out[o + 2] = yx;
        out[o + 3] = yz;
        out[o + 4] = hall;
        out[o + 5] = nx;
        out[o + 6] = nz;
        return n + 1;
    }

    /**
     * Fire escapes in the yard, up the backs of houses that look on it: a yard shut in has one at
     * least (no one is shut in there), an open one now and then.
     */
    private void yard(Cell c, boolean open, Random rnd) {
        int first = -1, made = 0;
        for (int k = 0; k < c.houseN; k++) {
            int o = k * Cell.HOUSE;
            if (c.houses[o + 7] != 0f) continue;
            float cx = c.houses[o], cz = c.houses[o + 1], hx = c.houses[o + 2], hz = c.houses[o + 3];
            float nx = c.houses[o + 5], nz = c.houses[o + 6];
            float half = Math.abs(nx) * hx + Math.abs(nz) * hz;
            // its back on the yard, not on the end of another row
            if (c.houseAt(cx - nx * (half + 1.2f), cz - nz * (half + 1.2f)) >= 0) continue;
            if (first < 0) first = o;
            if (rnd.nextInt(open ? 6 : 3) != 0) continue;
            backEscape(c, o);
            made++;
        }
        if (made == 0 && !open && first >= 0) backEscape(c, first);
    }

    /** A fire escape up the back of the house at o, near one end of it, from the yard to the roof. */
    private static void backEscape(Cell c, int o) {
        float[] at = new float[2];
        escapeAt(c, c.houses, o, at);
        ladder(c, at[0], at[1], WALK_TOP, c.houses[o + 4], -c.houses[o + 5], -c.houses[o + 6], true);
        c.houses[o + 7] = ESCAPE;
    }

    /**
     * Where the fire escape of the house at o in h goes (on the wall, x, z into out): at its back,
     * near the end of it that looks on the yard. A house at the end of a row has its back's one
     * end behind the next row's corner house: not that end.
     */
    private static void escapeAt(Cell c, float[] h, int o, float[] out) {
        float cx = h[o], cz = h[o + 1], hx = h[o + 2], hz = h[o + 3], nx = h[o + 5], nz = h[o + 6];
        boolean alongX = Math.abs(nz) > 0.5f;
        for (int e = -1; e <= 1; e += 2) {
            float x = alongX ? cx + e * (hx - 1.1f) : cx - nx * hx, z = alongX ? cz - nz * hz : cz + e * (hz - 1.1f);
            out[0] = x;
            out[1] = z;
            if (c.houseAt(x - nx * 1.2f, z - nz * 1.2f) < 0) return;
        }
        out[0] = alongX ? cx - hx + 1.1f : cx - nx * hx;
        out[1] = alongX ? cz - nz * hz : cz - hz + 1.1f;
    }

    /** The foot of the fire escape of house h (as house tells it), where one stands to climb it: into out (x, z). */
    void escapeFoot(float[] h, float[] out) {
        escapeAt(cell(index(h[0]), index(h[1])), h, 0, out);
        out[0] -= h[5] * 0.55f;
        out[1] -= h[6] * 0.55f;
    }

    /**
     * A row of houses from u0 to u1 along x (alongX) or z, from v0 to v1 the other way; its
     * fronts face toward smaller v (side -1) or larger (side 1).
     */
    private void row(Cell c, Random rnd, float u0, float u1, float v0, float v1, boolean alongX, int side, boolean arch) {
        DrawList d = c.boxes;
        float u = u0, mid = (u0 + u1) * 0.5f;
        while (u < u1 - 0.5f) {
            int bays = 3 + rnd.nextInt(3);
            float len = bays * 2.9f;
            if (u1 - (u + len) < 6f) len = u1 - u;
            int storeys = lowRise > 0 ? 1 + rnd.nextInt(lowRise) : 3 + rnd.nextInt(4);
            float height = 3.3f * (storeys + 1) + 0.5f;
            int tint = lowRise > 0 ? OLD_WALLS[rnd.nextInt(OLD_WALLS.length)] : Street.WALLS[rnd.nextInt(Street.WALLS.length)];
            int seed = rnd.nextInt(1 << 16);
            float cu = u + len * 0.5f, cv = (v0 + v1) * 0.5f, hu = len * 0.5f, hv = (v1 - v0) * 0.5f;
            float cx = alongX ? cu : cv, cz = alongX ? cv : cu, hx = alongX ? hu : hv, hz = alongX ? hv : hu;
            // the house the row's middle falls in, in a row given the arch: a way through it to the yard
            boolean archHere = arch && u <= mid && u + len > mid && len > 2f * ARCH_HALF + 3f;
            float facade = DrawList.material(DrawList.FACADE, seed);
            if (archHere) {
                float a0 = cu - ARCH_HALF, a1 = cu + ARCH_HALF;
                boxUV(d, (u + a0) * 0.5f, height * 0.5f, cv, (a0 - u) * 0.5f, height * 0.5f, hv, alongX, tint, facade);
                boxUV(d, (a1 + u + len) * 0.5f, height * 0.5f, cv, (u + len - a1) * 0.5f, height * 0.5f, hv, alongX, tint, facade);
                boxUV(d, cu, (ARCH_HIGH + height) * 0.5f, cv, ARCH_HALF, (height - ARCH_HIGH) * 0.5f, hv, alongX, tint, facade);
                // the passage's own vault, darker, and a frame of stone at either end
                boxUV(d, cu, ARCH_HIGH + 0.05f, cv, ARCH_HALF, 0.05f, hv - 0.02f, alongX, Street.scale(tint, 0.7f), DrawList.PLAIN);
                for (int end = -1; end <= 1; end += 2) {
                    float ev = cv + end * (hv + 0.04f);
                    for (int e = -1; e <= 1; e += 2)
                        boxUV(d, cu + e * (ARCH_HALF + 0.12f), ARCH_HIGH * 0.5f, ev, 0.14f, ARCH_HIGH * 0.5f, 0.06f, alongX, 0xD8CDB8, DrawList.STONE);
                    boxUV(d, cu, ARCH_HIGH + 0.18f, ev, ARCH_HALF + 0.26f, 0.2f, 0.06f, alongX, 0xD8CDB8, DrawList.STONE);
                }
            } else d.box(cx, height * 0.5f, cz, hx, height * 0.5f, hz, null, tint, facade);
            // the cornice along the front
            float fv = side < 0 ? v0 : v1;
            float kx = alongX ? cu : fv + side * 0.15f, kz = alongX ? fv + side * 0.15f : cu;
            d.box(kx, height - 0.2f, kz, alongX ? hu : 0.25f, 0.2f, alongX ? 0.25f : hu, null, Street.scale(tint, 1.05f), DrawList.PLAIN);
            // a lift is rare: one in ten of the taller houses has one, a door in the front, a hut on
            // the roof (and no chimneys in its way); the roofs are to be got about by the roofs
            boolean lift = !archHere && storeys >= 4 && ((seed >>> 3) % 10) == 0;
            // of the rest, one in four has a way in at the front, a hall through to the yard (no
            // way up); the fire escapes are in the yards (see yard)
            boolean door = !lift && !archHere && ((seed >>> 9) % 4) == 0;
            roof(c, d, lift ? null : c.detail, cx, height, cz, hx, hz, seed);
            // the house, and its roof to stand on: the tiles, a step up in the middle (a wall only
            // on the roof); a house with an arch stands on either side of it and over it
            if (archHere) {
                float a0 = cu - ARCH_HALF, a1 = cu + ARCH_HALF;
                wallUV(c, u, a0, v0, v1, alongX, height + 0.5f, 0f);
                wallUV(c, a1, u + len, v0, v1, alongX, height + 0.5f, 0f);
                wallUV(c, a0, a1, v0, v1, alongX, height + 0.5f, ARCH_HIGH);
            } else c.wall(cx - hx, cz - hz, cx + hx, cz + hz, height + 0.5f);
            c.wall(cx - hx + 1.3f, cz - Math.max(0.4f, hz - 1.3f), cx + hx - 1.3f, cz + Math.max(0.4f, hz - 1.3f), height + 0.75f, height);
            float nx = alongX ? 0f : side, nz = alongX ? side : 0f;
            if (lift) lift(c, d, cu, fv, alongX, side, height, tint);
            if (door) {
                liftDoor(d, cu, fv, alongX, side);
                liftDoor(d, cu, fv - side * DEPTH, alongX, -side);
            }
            // ladders up from a lower roof next door, or down onto it from this one: on the
            // taller house's wall, at the middle of the row, from the one roof to the other
            for (int end = 0; end < 2; end++) {
                float bu = end == 0 ? u : u + len, look = end == 0 ? -0.4f : 0.4f;
                int o = c.houseAt(alongX ? bu + look : cv, alongX ? cv : bu + look);
                if (o < 0) continue;
                float other = c.houses[o + 4], mine = height + 0.5f;
                if (Math.abs(other - mine) < 1f) continue;
                // facing from the taller toward the lower
                float dir = (mine > other ? 1f : -1f) * (end == 0 ? -1f : 1f);
                float lx = alongX ? bu : cv, lz = alongX ? cv : bu;
                ladder(c, lx, lz, Math.min(mine, other), Math.max(mine, other), alongX ? dir : 0f, alongX ? 0f : dir, false);
            }
            // two pots of flowers either side of the middle of the front, now and then; ivy up its far end
            Random deco = new Random(seed * 0x5DEECE66DL + 11L);
            if (((seed >>> 11) & 1) == 0 && !archHere)
                for (int e = -1; e <= 1; e += 2) {
                    float pu = cu + e * 1.55f, pv = fv + side * 0.4f;
                    float px = alongX ? pu : pv, pz = alongX ? pv : pu;
                    Garden.pot(c.detail, deco, px, WALK_TOP, pz, 0.27f);
                    c.wall(px - 0.24f, pz - 0.24f, px + 0.24f, pz + 0.24f, WALK_TOP + 0.6f);
                }
            if (((seed >>> 12) & 3) == 0 && !archHere) {
                float iu = u + len - 1.8f, top = WALK_TOP + 3.3f * (1 + ((seed >>> 14) & 1)) + 0.8f;
                Garden.ivy(c.detail, deco, alongX ? iu : fv, alongX ? fv : iu, nx, nz, 1.4f, WALK_TOP, Math.min(top, height));
            }
            c.house(cx, cz, hx, hz, height + 0.5f, nx, nz, lift ? LIFT : door ? DOOR : archHere ? ARCH : 0f);
            u += len;
        }
    }

    /**
     * What a house has: a lift (a hall, and up to the roof), a fire escape up its back from the
     * yard, a way in at the front (a hall through to the yard, no way up), an arch through it.
     */
    static final float LIFT = 1f, ESCAPE = 2f, DOOR = 3f, ARCH = 4f;
    // the arch through a house: half its width, how high its vault
    static final float ARCH_HALF = 1.9f, ARCH_HIGH = 4.6f;

    /** A wall placed by u along a row and v across it, from u0 to u1 and v0 to v1. */
    private static void wallUV(Cell c, float u0, float u1, float v0, float v1, boolean alongX, float top, float bottom) {
        if (alongX) c.wall(u0, v0, u1, v1, top, bottom);
        else c.wall(v0, u0, v1, u1, top, bottom);
    }
    // a ladder: half its width; how far apart its rungs; its iron
    static final float LADDER_HALF = 0.32f, RUNG = 0.36f;
    static final int IRON = 0x2C2B2E;

    /**
     * A ladder on a wall at (x, z) facing (nx, nz), from bottom to top: iron rails and rungs, a hoop
     * over its top, and its place kept for climbing (see ladderNear); a fire escape has landings
     * with railings every two storeys.
     */
    private static void ladder(Cell c, float x, float z, float bottom, float top, float nx, float nz, boolean escape) {
        DrawList d = c.detail;
        float h = top - bottom, my = (top + bottom) * 0.5f;
        float ax = -nz, az = nx;                          // across the ladder
        float ox = x + nx * 0.14f, oz = z + nz * 0.14f;   // off the wall
        for (int e = -1; e <= 1; e += 2)
            d.box(ox + ax * e * LADDER_HALF, my + 0.5f, oz + az * e * LADDER_HALF, Math.abs(nx) * 0.03f + Math.abs(nz) * 0.025f,
                    h * 0.5f + 0.5f, Math.abs(nz) * 0.03f + Math.abs(nx) * 0.025f, null, IRON, DrawList.PLAIN);
        for (float y = bottom + 0.3f; y < top + 0.9f; y += RUNG)
            d.box(ox, y, oz, Math.abs(nz) * LADDER_HALF + 0.02f, 0.018f, Math.abs(nx) * LADDER_HALF + 0.02f, null, IRON, DrawList.PLAIN);
        // the brackets holding it to the wall, now and then
        for (float y = bottom + 1.5f; y < top; y += 3.3f)
            for (int e = -1; e <= 1; e += 2)
                d.box(x + nx * 0.07f + ax * e * LADDER_HALF, y, z + nz * 0.07f + az * e * LADDER_HALF, Math.abs(nx) * 0.07f + 0.02f, 0.02f,
                        Math.abs(nz) * 0.07f + 0.02f, null, IRON, DrawList.PLAIN);
        if (escape) {
            // landings every two storeys, a railing round each
            for (float y = bottom + 3.3f * 2f - 0.1f; y < top - 2f; y += 3.3f * 2f) {
                float px = x + nx * 0.55f, pz = z + nz * 0.55f;
                d.box(px, y, pz, Math.abs(nz) * 0.9f + Math.abs(nx) * 0.55f, 0.03f, Math.abs(nx) * 0.9f + Math.abs(nz) * 0.55f, null, IRON, DrawList.PLAIN);
                d.box(x + nx * 1.08f, y + 0.5f, z + nz * 1.08f, Math.abs(nz) * 0.9f + 0.02f, 0.5f, Math.abs(nx) * 0.9f + 0.02f, null, IRON, DrawList.RAIL);
                for (int e = -1; e <= 1; e += 2)
                    d.box(px + ax * e * 0.9f, y + 0.5f, pz + az * e * 0.9f, Math.abs(nx) * 0.55f + 0.02f, 0.5f, Math.abs(nz) * 0.55f + 0.02f, null, IRON, DrawList.RAIL);
            }
        }
        c.ladder(x, z, bottom, top, nx, nz);
    }

    /**
     * The ladder whose foot is within reach of one at (x, z) with feet at feet (near its bottom):
     * into out where to stand to climb it (x, z), its bottom and top, the way its wall faces (x,
     * z); false if none is.
     */
    // the ladder off a pickup set up against a wall (see Kit), if one is: its foot, bottom, top, the wall's way out
    private boolean setOn;
    private final float[] setLadder = new float[6];

    void setLadder(float x, float z, float bottom, float top, float nx, float nz) {
        setLadder[0] = x;
        setLadder[1] = z;
        setLadder[2] = bottom;
        setLadder[3] = top;
        setLadder[4] = nx;
        setLadder[5] = nz;
        setOn = true;
    }

    void unsetLadder() {
        setOn = false;
    }

    /** The ladder set up off a pickup, as ladderNear and ladderAt find the town's: nearer than best? into out. */
    private boolean setLadderNear(float x, float z, float feet, float best, boolean high, float[] out) {
        if (!setOn) return false;
        float[] l = setLadder;
        if (feet < l[2] - 0.6f || feet > (high ? l[3] - 0.6f : l[2] + 1.2f)) return false;
        float fx = l[0] + l[4] * 0.55f, fz = l[1] + l[5] * 0.55f;
        if ((fx - x) * (fx - x) + (fz - z) * (fz - z) >= best) return false;
        out[0] = fx;
        out[1] = fz;
        out[2] = l[2];
        out[3] = l[3];
        out[4] = l[4];
        out[5] = l[5];
        return true;
    }

    boolean ladderNear(float x, float z, float feet, float reach, float[] out) {
        float best = reach * reach;
        boolean found = setLadderNear(x, z, feet, best, false, out);
        if (found) best = (out[0] - x) * (out[0] - x) + (out[1] - z) * (out[1] - z);
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.ladderN; k++) {
                    int o = k * Cell.LADDER;
                    float bottom = c.ladders[o + 2];
                    if (feet < bottom - 0.6f || feet > bottom + 1.2f) continue;
                    float fx = c.ladders[o] + c.ladders[o + 4] * 0.55f, fz = c.ladders[o + 1] + c.ladders[o + 5] * 0.55f;
                    float dd = (fx - x) * (fx - x) + (fz - z) * (fz - z);
                    if (dd < best) {
                        best = dd;
                        out[0] = fx;
                        out[1] = fz;
                        out[2] = bottom;
                        out[3] = c.ladders[o + 3];
                        out[4] = c.ladders[o + 4];
                        out[5] = c.ladders[o + 5];
                        found = true;
                    }
                }
            }
        return found;
    }

    /**
     * A ladder one in the air at (x, z) with feet at feet can take hold of: within reach of the
     * place before it, the feet between its bottom and a little under its top; as ladderNear
     * tells it. False if none.
     */
    boolean ladderAt(float x, float z, float feet, float reach, float[] out) {
        float best = reach * reach;
        boolean found = setLadderNear(x, z, feet, best, true, out);
        if (found) best = (out[0] - x) * (out[0] - x) + (out[1] - z) * (out[1] - z);
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.ladderN; k++) {
                    int o = k * Cell.LADDER;
                    float bottom = c.ladders[o + 2], top = c.ladders[o + 3];
                    if (feet < bottom - 0.6f || feet > top - 0.6f) continue;
                    float fx = c.ladders[o] + c.ladders[o + 4] * 0.55f, fz = c.ladders[o + 1] + c.ladders[o + 5] * 0.55f;
                    float dd = (fx - x) * (fx - x) + (fz - z) * (fz - z);
                    if (dd < best) {
                        best = dd;
                        out[0] = fx;
                        out[1] = fz;
                        out[2] = bottom;
                        out[3] = top;
                        out[4] = c.ladders[o + 4];
                        out[5] = c.ladders[o + 5];
                        found = true;
                    }
                }
            }
        return found;
    }

    // a lift's door: half its width, its height; its hut on the roof: in from the front, half sizes, height
    static final float DOOR_HALF = 0.85f, DOOR_HIGH = 2.6f, HUT_IN = 2.6f, HUT_HALF = 1.3f, HUT_DEEP = 1.1f, HUT_HIGH = 2.2f;
    static final int LIFT_LIGHT = 0x8FF0C8;

    /**
     * A lift: in the front at u (along the row), the front at v facing side, a door of dark glass
     * in a stone frame (the sign over it is the game's own, see World), and the same at the back, on the yard; on the roof
     * the hut it comes up into, its door facing in, away from the street.
     */
    private static void lift(Cell c, DrawList d, float u, float fv, boolean alongX, int side, float height, int tint) {
        liftDoor(d, u, fv, alongX, side);
        liftDoor(d, u, fv - side * DEPTH, alongX, -side);
        // the hut on the roof, and its door on the side away from the street
        float top = height + 0.5f, hv = fv - side * (HUT_IN + HUT_DEEP);
        boxUV(d, u, top + HUT_HIGH * 0.5f, hv, HUT_HALF, HUT_HIGH * 0.5f, HUT_DEEP, alongX, Street.scale(tint, 0.95f), DrawList.PLAIN);
        boxUV(d, u, top + HUT_HIGH + 0.08f, hv, HUT_HALF + 0.12f, 0.08f, HUT_DEEP + 0.12f, alongX, 0xA8553A, DrawList.GRAIN);
        float back = hv - side * (HUT_DEEP + 0.03f);
        boxUV(d, u, top + 1.05f, back, 0.55f, 1.05f, 0.04f, alongX, 0x1D2730, DrawList.PLAIN);
        float x0 = alongX ? u - HUT_HALF : hv - HUT_DEEP, z0 = alongX ? hv - HUT_DEEP : u - HUT_HALF;
        c.wall(x0, z0, x0 + 2f * (alongX ? HUT_HALF : HUT_DEEP), z0 + 2f * (alongX ? HUT_DEEP : HUT_HALF), top + HUT_HIGH + 0.16f);
    }

    /** A lift's door in a wall at v facing side: dark glass in a stone frame. */
    private static void liftDoor(DrawList d, float u, float fv, boolean alongX, int side) {
        float out = fv + side * 0.06f;
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out, DOOR_HALF + 0.22f, DOOR_HIGH * 0.5f + 0.15f, 0.07f, alongX, 0xD8CDB8, DrawList.STONE);
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out + side * 0.03f, DOOR_HALF, DOOR_HIGH * 0.5f, 0.06f, alongX, 0x1D2730, DrawList.PLAIN);
        boxUV(d, u, DOOR_HIGH * 0.5f + WALK_TOP, out + side * 0.05f, 0.03f, DOOR_HIGH * 0.5f, 0.05f, alongX, 0x9AA3A8, DrawList.PLAIN);
    }

    private static void boxUV(DrawList d, float u, float y, float v, float hu, float hy, float hv, boolean alongX, int color, float material) {
        if (alongX) d.box(u, y, v, hu, hy, hv, null, color, material);
        else d.box(v, y, u, hv, hy, hu, null, color, material);
    }

    /** A box placed by u along a row and v across it, so wide, high and deep. */
    private static void boxUV(DrawList d, float u, float y, float v, float hu, float hy, float hv, boolean alongX, int color, int material) {
        if (alongX) d.box(u, y, v, hu, hy, hv, null, color, material);
        else d.box(v, y, u, hv, hy, hu, null, color, material);
    }

    /**
     * A lift whose door is within reach of (x, z): into out the place before its door on the
     * pavement (x, z), the way its door faces (x, z), where she is when she has come out of its
     * hut on the roof, a few steps on (x, y, z), the way she faces there, as a heading, and the
     * place before the hut's door (x, z); false if none is so near.
     */
    boolean liftAt(float x, float z, float reach, float[] out) {
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN; k++) {
                    int o = k * Cell.HOUSE;
                    if (c.houses[o + 7] != LIFT) continue;
                    lift(c.houses, o, out);
                    float dx = out[0] - x, dz = out[1] - z;
                    if (dx * dx + dz * dz < reach * reach) return true;
                }
            }
        return false;
    }

    /**
     * The nearest lift to one at (x, z) with feet at feet: in the street, by its door in the
     * front or at the back; on a roof, by its hut's door; as liftAt() tells it (the door being the
     * one near), and out[10] 1 if it is the one at the back; its house into house as the cell
     * keeps it (see Cell.house); false if none is within reach.
     */
    boolean liftNear(float x, float z, float feet, float reach, float[] out, float[] house) {
        return liftNear(x, z, feet, reach, out, house, true);
    }

    /**
     * The same, the houses with a way in at the front too (their doors, front and back, as a
     * lift's are told; no hut); the lifts left out when lifts is false.
     */
    boolean liftNear(float x, float z, float feet, float reach, float[] out, float[] house, boolean lifts) {
        float best = reach * reach;
        boolean found = false;
        float[] l = new float[10];
        int ci = index(x), cj = index(z);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN; k++) {
                    int o = k * Cell.HOUSE;
                    float kind = c.houses[o + 7];
                    if (kind != DOOR && (kind != LIFT || !lifts)) continue;
                    lift(c.houses, o, l);
                    boolean up = kind == LIFT && Math.abs(feet - l[5]) < 1.5f;
                    if (!up && feet > 1f) continue;
                    // in the street, its door in the front or the one at the back, on the yard (if
                    // the back looks on the yard and not on the end of the next row)
                    int doors = up ? 1 : backClear(c.houses, o) ? 2 : 1;
                    for (int back = 0; back < doors; back++) {
                        if (back == 1) {
                            float half = Math.abs(l[2]) * c.houses[o + 2] + Math.abs(l[3]) * c.houses[o + 3];
                            l[0] -= l[2] * (2f * half + 1.4f);
                            l[1] -= l[3] * (2f * half + 1.4f);
                            l[2] = -l[2];
                            l[3] = -l[3];
                        }
                        float dx = (up ? l[8] : l[0]) - x, dz = (up ? l[9] : l[1]) - z, dd = dx * dx + dz * dz;
                        if (dd < best) {
                            best = dd;
                            System.arraycopy(l, 0, out, 0, 10);
                            out[10] = back;
                            System.arraycopy(c.houses, o, house, 0, Cell.HOUSE);
                            found = true;
                        }
                    }
                }
            }
        return found;
    }

    /**
     * Whether the back of the house at o looks on the yard: nothing of another house stands
     * just behind the middle of its back (a house at a corner of its block backs onto the end
     * of the row across, and has no door there).
     */
    boolean backClear(float[] h, int o) {
        float cx = h[o], cz = h[o + 1], nx = h[o + 5], nz = h[o + 6];
        float half = Math.abs(nx) * h[o + 2] + Math.abs(nz) * h[o + 3];
        float px = cx - nx * (half + 1.2f), pz = cz - nz * (half + 1.2f), r = 0.9f;
        int ci = index(px), cj = index(pz);
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                Cell c = cell(i, j);
                for (int k = 0; k < c.wallN; k++) {
                    int w = k * Cell.WALL;
                    if (c.walls[w + 4] < 1f || c.walls[w + 5] > 1f) continue;
                    if (px + r > c.walls[w] && px - r < c.walls[w + 2] && pz + r > c.walls[w + 1] && pz - r < c.walls[w + 3]) return false;
                }
            }
        return true;
    }

    /** Home's house as its cell keeps it (see Cell.house), into house. */
    void homeHouse(float[] house) {
        Cell c = cell(HOME_I, HOME_J);
        for (int k = 0; k < c.houseN; k++) {
            int o = k * Cell.HOUSE;
            if (home(c.houses[o], c.houses[o + 1])) {
                System.arraycopy(c.houses, o, house, 0, Cell.HOUSE);
                return;
            }
        }
    }

    /** The lift of the house at o: as liftAt() tells it. */
    static void lift(float[] h, int o, float[] out) {
        float cx = h[o], cz = h[o + 1], hx = h[o + 2], hz = h[o + 3], top = h[o + 4], nx = h[o + 5], nz = h[o + 6];
        float half = Math.abs(nx) * hx + Math.abs(nz) * hz;
        // the front's middle, and a little before it
        out[0] = cx + nx * (half + 0.7f);
        out[1] = cz + nz * (half + 0.7f);
        out[2] = nx;
        out[3] = nz;
        // out of the hut's door and a few steps on into the roof, clear of the hut for the eye
        float door = half - HUT_IN - 2f * HUT_DEEP - 0.7f, in = door - 3.3f;
        out[4] = cx + nx * in;
        out[5] = top;
        out[6] = cz + nz * in;
        out[7] = (float) Math.atan2(-nx, -nz);
        out[8] = cx + nx * door;
        out[9] = cz + nz * door;
    }

    // the kinds of sign at the ways in and up (see marks)
    static final int MARK_STREET_LIFT = 0, MARK_HUT = 1, MARK_ESCAPE = 2, MARK_ROOF_LADDER = 3, MARK_DOOR = 5, MARK_HOME = 6;

    /**
     * The places for signs at the ways in and up within reach of (x, z), at most max of them,
     * six floats each into out: where the sign hangs (x, y, z), the way the wall it is on faces
     * (x, z), and its kind: over a lift's front door, over the door of its hut on the roof, at
     * the foot of a fire escape, at the foot of a ladder from one roof up to the next. How many.
     */
    int marks(float x, float z, float reach, float[] out, int max) {
        int n = 0, ci = index(x), cj = index(z);
        float[] l = new float[10];
        for (int j = cj - 1; j <= cj + 1; j++)
            for (int i = ci - 1; i <= ci + 1; i++) {
                if (square(i, j)) continue;
                Cell c = cell(i, j);
                for (int k = 0; k < c.houseN && n < max - 2; k++) {
                    int o = k * Cell.HOUSE;
                    float kind = c.houses[o + 7];
                    if (kind != LIFT && kind != DOOR) continue;
                    int sign = kind == LIFT ? MARK_STREET_LIFT : MARK_DOOR;
                    boolean mine = home(c.houses[o], c.houses[o + 1]);
                    float nx = c.houses[o + 5], nz = c.houses[o + 6], top = c.houses[o + 4];
                    float half = Math.abs(nx) * c.houses[o + 2] + Math.abs(nz) * c.houses[o + 3];
                    float fx = c.houses[o] + nx * (half + 0.25f), fz = c.houses[o + 1] + nz * (half + 0.25f);
                    if (near(fx, fz, x, z, reach)) n = mark(out, n, fx, WALK_TOP + 3.5f, fz, nx, nz, mine ? MARK_HOME : sign);
                    // the door at the back, on the yard, if it looks on one
                    float bx = c.houses[o] - nx * (half + 0.25f), bz = c.houses[o + 1] - nz * (half + 0.25f);
                    if (n < max - 1 && near(bx, bz, x, z, reach) && backClear(c.houses, o))
                        n = mark(out, n, bx, WALK_TOP + 3.5f, bz, -nx, -nz, mine ? MARK_HOME : sign);
                    if (kind != LIFT) continue;
                    lift(c.houses, o, l);
                    float hx = l[8] + nx * 0.45f, hz = l[9] + nz * 0.45f;
                    if (near(hx, hz, x, z, reach)) n = mark(out, n, hx, top + HUT_HIGH + 0.8f, hz, -nx, -nz, MARK_HUT);
                }
                for (int k = 0; k < c.ladderN && n < max; k++) {
                    int o = k * Cell.LADDER;
                    float lx = c.ladders[o], lz = c.ladders[o + 1], bottom = c.ladders[o + 2], nx = c.ladders[o + 4], nz = c.ladders[o + 5];
                    float sx = lx + nx * 0.6f, sz = lz + nz * 0.6f;
                    if (!near(sx, sz, x, z, reach)) continue;
                    boolean street = bottom < 1f;
                    n = mark(out, n, sx, bottom + (street ? 2.6f : 1.9f), sz, nx, nz, street ? MARK_ESCAPE : MARK_ROOF_LADDER);
                }
            }
        return n;
    }

    private static boolean near(float ax, float az, float x, float z, float reach) {
        return (ax - x) * (ax - x) + (az - z) * (az - z) < reach * reach;
    }

    private static int mark(float[] out, int n, float x, float y, float z, float nx, float nz, int kind) {
        int o = n * 6;
        out[o] = x;
        out[o + 1] = y;
        out[o + 2] = z;
        out[o + 3] = nx;
        out[o + 4] = nz;
        out[o + 5] = kind;
        return n + 1;
    }

    /** How many houses cell (i, j) has (none, if it is a square). */
    int houses(int i, int j) {
        return square(i, j) ? 0 : cell(i, j).houseN;
    }

    /** House k of cell (i, j), into out as the cell keeps it (see Cell.house). */
    void house(int i, int j, int k, float[] out) {
        System.arraycopy(cell(i, j).houses, k * Cell.HOUSE, out, 0, Cell.HOUSE);
    }

    /** A roof of red tiles in two steps; its chimneys among the small things (none, if small is null). */
    private static void roof(Cell cell, DrawList d, DrawList small, float x, float top, float z, float hx, float hz, int seed) {
        d.box(x, top + 0.25f, z, hx - 0.3f, 0.25f, hz - 0.3f, null, 0xC4673E, DrawList.GRAIN);
        d.box(x, top + 0.6f, z, hx - 1.3f, 0.15f, Math.max(0.4f, hz - 1.3f), null, 0xA8553A, DrawList.GRAIN);
        if (small == null) return;
        int h = seed * 0x2545F491;
        for (int c = 0; c <= ((h >>> 7) & 1); c++) {
            float cx = x + ((((h >>> (9 + c * 5)) & 31) / 31f) - 0.5f) * hx;
            float cz = z + ((((h >>> (19 + c * 5)) & 31) / 31f) - 0.5f) * hz;
            small.box(cx, top + 1.1f, cz, 0.3f, 0.6f, 0.3f, null, 0xBE9C84, DrawList.STONE);
            cell.wall(cx - 0.3f, cz - 0.3f, cx + 0.3f, cz + 0.3f, top + 1.7f, top);
        }
    }

    /**
     * A square: big pale paving, palms round it; within them either a fountain amid benches,
     * flower beds, pots and lamps, or a garden in a low iron fence: lawns, gravel paths across it,
     * hedges along them, trees and bushes, benches, beds of flowers round a small fountain.
     */
    private void plaza(Cell c, Random rnd, Random deco, float x0, float z0, boolean garden) {
        DrawList d = c.detail;
        float mx = x0 + CELL * 0.5f, mz = z0 + CELL * 0.5f, y = WALK_TOP;
        for (int a = -2; a <= 2; a++)
            for (int b = -2; b <= 2; b++) {
                if (Math.abs(a) < 2 && Math.abs(b) < 2) continue;
                float px = mx + a * 10f, pz = mz + b * 10f;
                palm(c, rnd, px, WALK_TOP, pz, 6f + rnd.nextFloat() * 2.5f, (rnd.nextFloat() - 0.5f) * 0.35f, false);
                c.wall(px - 0.25f, pz - 0.25f, px + 0.25f, pz + 0.25f);
            }
        if (garden) {
            park(c, deco, mx, mz);
            return;
        }
        // the fountain, and round it benches looking at it, pots between them
        float rim = Garden.fountain(c.boxes, mx, y, mz, 4.2f);
        c.wall(mx - 3.9f, mz - 3.9f, mx + 3.9f, mz + 3.9f, rim);
        for (int k = 0; k < 8; k++) {
            float a = k * (float) (Math.PI / 4) + (float) (Math.PI / 8), ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float bx = mx + ca * 8f, bz = mz + sa * 8f;
            bench(c, bx, y, bz, (float) Math.atan2(-ca, -sa));
            c.wall(bx - 0.75f, bz - 0.75f, bx + 0.75f, bz + 0.75f, y + 0.48f);
            float pa = a + (float) (Math.PI / 8), px = mx + (float) Math.cos(pa) * 8.4f, pz = mz + (float) Math.sin(pa) * 8.4f;
            Garden.pot(d, deco, px, y, pz, 0.32f);
            c.wall(px - 0.28f, pz - 0.28f, px + 0.28f, pz + 0.28f, y + 0.7f);
        }
        // lamps on the diagonals, flower beds on the four ways out
        for (int k = 0; k < 4; k++) {
            float a = k * (float) (Math.PI / 2) + (float) (Math.PI / 4);
            float lx = mx + (float) Math.cos(a) * 12f, lz = mz + (float) Math.sin(a) * 12f;
            Garden.lamp(d, lx, y, lz, a);
            c.wall(lx - 0.2f, lz - 0.2f, lx + 0.2f, lz + 0.2f);
            float bx = k % 2 == 0 ? (k == 0 ? 14.5f : -14.5f) : 0f, bz = k % 2 == 1 ? (k == 1 ? 14.5f : -14.5f) : 0f;
            float hx = bx != 0f ? 0.8f : 2.6f, hz = bx != 0f ? 2.6f : 0.8f;
            for (int e = -1; e <= 1; e += 2) {
                float cx = mx + bx + (bx == 0f ? e * 4f : 0f), cz = mz + bz + (bz == 0f ? e * 4f : 0f);
                Garden.bed(d, deco, cx - hx, cz - hz, cx + hx, cz + hz, y);
                c.wall(cx - hx, cz - hz, cx + hx, cz + hz, y + 0.55f);
            }
        }
    }

    /** The garden within a square's palms: its fence, lawns, paths, hedges, trees, benches and middle. */
    private void park(Cell c, Random rnd, float mx, float mz) {
        DrawList d = c.detail;
        float y = WALK_TOP, edge = 17f, path = 1.5f;
        // the gravel paths across, and the lawns between them
        c.boxes.box(mx, y + 0.008f, mz, edge, 0.008f, path, null, 0xD9C7A2, DrawList.GRAVEL);
        c.boxes.box(mx, y + 0.008f, mz, path, 0.008f, edge, null, 0xD9C7A2, DrawList.GRAVEL);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                float lx = mx + sx * (edge + path) * 0.5f, lz = mz + sz * (edge + path) * 0.5f, lh = (edge - path) * 0.5f;
                c.boxes.box(lx, y + 0.02f, lz, lh, 0.02f, lh, null, 0x6E9A44, DrawList.GRASS);
                // low hedges along the paths, a gap half way for the lawn
                float a0 = path + 0.6f, a1 = edge - 0.4f, gap = (a0 + a1) * 0.5f;
                for (int h = 0; h < 2; h++) {
                    float f0 = h == 0 ? a0 : gap + 1f, f1 = h == 0 ? gap - 1f : a1;
                    float hx0 = mx + sx * f0, hx1 = mx + sx * f1, hz = mz + sz * (path + 0.35f);
                    Garden.hedge(d, Math.min(hx0, hx1), hz - 0.3f, Math.max(hx0, hx1), hz + 0.3f, y, 0.75f);
                    c.wall(Math.min(hx0, hx1), hz - 0.3f, Math.max(hx0, hx1), hz + 0.3f, y + 0.75f);
                    float vz0 = mz + sz * f0, vz1 = mz + sz * f1, vx = mx + sx * (path + 0.35f);
                    Garden.hedge(d, vx - 0.3f, Math.min(vz0, vz1), vx + 0.3f, Math.max(vz0, vz1), y, 0.75f);
                    c.wall(vx - 0.3f, Math.min(vz0, vz1), vx + 0.3f, Math.max(vz0, vz1), y + 0.75f);
                }
                // trees and bushes on the lawn
                int trees = 2 + rnd.nextInt(2);
                for (int k = 0; k < trees; k++) {
                    float tx = mx + sx * (5f + rnd.nextFloat() * 9f), tz = mz + sz * (5f + rnd.nextFloat() * 9f);
                    tree(c, d, rnd, tx, y, tz, 7.5f + rnd.nextFloat() * 3f);
                    c.wall(tx - 0.2f, tz - 0.2f, tx + 0.2f, tz + 0.2f);
                }
                for (int k = 0; k < 3; k++) {
                    float bx = mx + sx * (3.5f + rnd.nextFloat() * 12f), bz = mz + sz * (3.5f + rnd.nextFloat() * 12f);
                    float r = 0.6f + rnd.nextFloat() * 0.5f;
                    Garden.bush(d, rnd, bx, y, bz, r);
                    c.wall(bx - r * 0.7f, bz - r * 0.7f, bx + r * 0.7f, bz + r * 0.7f, y + r * 1.2f);
                }
                // benches on the paths, their backs to the hedges
                float bt = 9.5f;
                bench(c, mx + sx * bt, y, mz + sz * 1.15f, sz > 0 ? (float) Math.PI : 0f);
                c.wall(mx + sx * bt - 0.9f, mz + sz * 1.15f - 0.3f, mx + sx * bt + 0.9f, mz + sz * 1.15f + 0.3f, y + 0.48f);
            }
        // the fence round it, open where the paths go out
        for (int e = -1; e <= 1; e += 2) {
            for (int h = -1; h <= 1; h += 2) {
                float f0 = h * (path + 0.4f), f1 = h * edge;
                Garden.fence(d, mx + Math.min(f0, f1), mz + e * edge, mx + Math.max(f0, f1), mz + e * edge, y);
                c.wall(mx + Math.min(f0, f1), mz + e * edge - 0.05f, mx + Math.max(f0, f1), mz + e * edge + 0.05f, y + 0.95f);
                Garden.fence(d, mx + e * edge, mz + Math.min(f0, f1), mx + e * edge, mz + Math.max(f0, f1), y);
                c.wall(mx + e * edge - 0.05f, mz + Math.min(f0, f1), mx + e * edge + 0.05f, mz + Math.max(f0, f1), y + 0.95f);
            }
        }
        // in the middle, where the paths cross: a small fountain, flower beds round it, lamps
        float rim = Garden.fountain(c.boxes, mx, y, mz, 2.3f);
        c.wall(mx - 2.1f, mz - 2.1f, mx + 2.1f, mz + 2.1f, rim);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                float bx = mx + sx * 3.3f, bz = mz + sz * 3.3f;
                Garden.pot(d, rnd, bx, y, bz, 0.4f);
                c.wall(bx - 0.35f, bz - 0.35f, bx + 0.35f, bz + 0.35f, y + 0.85f);
                Garden.lamp(d, mx + sx * 2.2f, y, mz + sz * (edge - 1.5f), 0f);
                c.wall(mx + sx * 2.2f - 0.2f, mz + sz * (edge - 1.5f) - 0.2f, mx + sx * 2.2f + 0.2f, mz + sz * (edge - 1.5f) + 0.2f);
            }
    }

    /**
     * Cars left along the curbs of the two streets the cell owns, each the way its side of the
     * road goes, now and then a gap; none near the crossings.
     */
    private void parked(Cell c, Random rnd, float x0, float z0, boolean duty) {
        float off = PARKED;
        for (float t = 13f; t < CELL - 13f; t += 5.6f)
            for (int s = -1; s <= 1; s += 2) {
                if (rnd.nextInt(10) < 7) continue;
                // the duty pickup's place, and the places either side of it, kept free (the dice
                // thrown all the same, so the rest of the town stays as it was)
                boolean keep = duty && s > 0 && Math.abs(t - DUTY_ALONG) < 6f;
                int paint = Cars.PAINT[rnd.nextInt(Cars.PAINT.length)];
                // its kind from a seed of its own, so the town is laid out as it was
                Random own = new Random(paint * 31L + (long) (t * 7f) + s * 1013L + (long) x0 * 17L + (long) z0 * 131L);
                int kind = Cars.kind(own);
                if (kind != Cars.SEDAN) paint = Cars.paint(own, kind);
                float len = Cars.half(kind) + 0.1f, top = Cars.top(kind);
                // the street running north: on its east side they face north, on its west side south
                if (rnd.nextBoolean()) {
                    float x = x0 + s * off, z = z0 + t;
                    if (Crossings.onZebra(this, index(x0 + 1f), index(z0 + 1f), true, t, len)) continue;
                    if (keep || keptClear(x, z) || keptClear(x, z + 2f) || keptClear(x, z - 2f) || moved.contains(lampKey(x, z))) continue;
                    Cars.car(c.parked, x, z, s > 0 ? 0f : (float) Math.PI, paint, true, kind);
                    c.wall(x - 0.95f, z - len, x + 0.95f, z + len, top);
                    keepParked(c, x, z, s > 0 ? 0f : (float) Math.PI, kind, paint);
                } else {
                    // the street running east: on its south side they face east, on its north side west
                    float x = x0 + t, z = z0 + s * off;
                    if (Crossings.onZebra(this, index(x0 + 1f), index(z0 + 1f), false, t, len)) continue;
                    if (keptClear(x, z) || keptClear(x + 2f, z) || keptClear(x - 2f, z) || moved.contains(lampKey(x, z))) continue;
                    Cars.car(c.parked, x, z, s < 0 ? (float) (Math.PI / 2) : (float) (-Math.PI / 2), paint, true, kind);
                    c.wall(x - len, z - 0.95f, x + len, z + 0.95f, top);
                    keepParked(c, x, z, s < 0 ? (float) (Math.PI / 2) : (float) (-Math.PI / 2), kind, paint);
                }
            }
    }

    /** Lamp posts along the curbs of the block, every so often. */
    private void lamps(Cell c, float x0, float z0) {
        DrawList d = c.detail;
        float in = CURB + 0.5f;
        for (float t = 14f; t < CELL - 8f; t += 18f) {
            lamp(c, d, x0 + in, z0 + t);
            if (!keptClear(x0 + t, z0 + in)) lamp(c, d, x0 + t, z0 + in);
        }
    }

    private void lamp(Cell c, DrawList d, float x, float z) {
        if (felled.contains(lampKey(x, z))) return;
        if ((c.lampN + 1) * 2 > c.lamps.length) c.lamps = java.util.Arrays.copyOf(c.lamps, c.lamps.length * 2);
        c.lamps[c.lampN * 2] = x;
        c.lamps[c.lampN * 2 + 1] = z;
        c.lampN++;
        int iron = 0x2B2F3A;
        d.box(x, WALK_TOP + 2.2f, z, 0.07f, 2.2f, 0.07f, null, iron, DrawList.PLAIN);
        d.box(x, WALK_TOP + 0.25f, z, 0.14f, 0.25f, 0.14f, null, iron, DrawList.PLAIN);
        d.box(x, WALK_TOP + 4.4f, z, 0.135f, 0.185f, 0.135f, null, 0xF3E2B4, DrawList.GLOW);
        d.box(x, WALK_TOP + 4.62f, z, 0.2f, 0.04f, 0.2f, null, iron, DrawList.PLAIN);
        c.wall(x - 0.12f, z - 0.12f, x + 0.12f, z + 0.12f, WALK_TOP + 4.7f);
    }
}
