package com.shumtugle.skydysh;

/**
 * The navigator: a small map in the corner, turned the way the eye looks, the streets drawn from
 * the town itself, and over them a line the way to go to the place the mission points at, along
 * the streets by the shortest way through the crossings, and on out along the road. Off the line,
 * a new one is found from where she is, and the line shows it, flashing. In the air, the line
 * goes straight. Under the map, the next turn and how far it is, or the place, if there is none.
 *
 * The streets are a grid (see City): crossings in rows from the shore road to the town's end,
 * every column of them, each a node; the road out a string of nodes from the crossing it leaves
 * by to the village. The way is found among the columns from a little west of the westmost of her
 * and the place to a little east of the eastmost.
 */
final class Nav {
    // the map: its side, in frame pixels; the strip under it; so many meters to a pixel, standing,
    // and more for every meter a second (the faster, the farther it shows); where she is on it,
    // across and down, as parts of its side
    static final int SIDE = 100, STRIP = 16, TALL = SIDE + STRIP;
    static final float SCALE = 2.2f, SCALE_SPEED = 0.06f, AT_ACROSS = 0.5f, AT_DOWN = 0.68f;
    // how often the way is found again (seconds); a turn counted as one past so much (radians)
    static final float AGAIN = 0.4f, TURN = 0.6f;
    // the road out, in nodes so far apart (meters)
    static final float ROAD_STEP = 32f;
    // columns to either side of her and the place, and at most so many in all
    static final int MARGIN = 2, COLUMNS = 160;
    // how long the line flashes once it is found anew off the old one (seconds)
    static final float FLASH = 1.2f;

    static final int SNOW = 0xE6C9D2DA, ROCK = 0xE63A3B33, SAND = 0xE6403E37, SEA = 0xE6203B5E, BLOCK = 0xE61A2234, FIELD = 0xE61C2B28, STREET = 0xE6586283, EDGE = 0xF00B0F1A;
    static final int OTHER = 0xFFF2A03A, LINE = 0xFF4FD3F2, LINE_FLASH = 0xFFF4F8FF, ME = 0xFF3F72E6, ME_RIM = 0xFFF4F8FF, PLACE = Marks.OPEN;
    static final int BAND = 0xF0214E53, TEXT = 0xFFF4E8D2, INK = 0xFF0B1418;

    private float goalX, goalZ, again, flash, scale = SCALE;
    private boolean aimed, aimedNow;
    // the way: so many points, x and z, from where she is to the place
    private float[] way = new float[256];
    private int wayN;
    // the nodes it went through last time (their numbers), to tell a new way from the same one shortened
    private int[] went = new int[0], wentNow = new int[0];
    private int wentN;
    /** How far it is along the way to the place, and to its next turn (-1, none); which way it turns: -1 left, 1 right. */
    float left, toTurn = -1f;
    int turn;
    /** Whether the way goes off behind her: turn round first. */
    boolean back;

    /** This frame's place to go to: the mission's mark. */
    void aim(float x, float z) {
        if (!aimed || Math.abs(x - goalX) > 1f || Math.abs(z - goalZ) > 1f) again = 0f;
        goalX = x;
        goalZ = z;
        aimedNow = true;
    }

    /** Whether there is a place to go to now, and a way to it. */
    boolean led() {
        return aimed && wayN >= 2;
    }

    /** The end of a frame's marks: no place aimed at this frame, none. */
    void update(float dt, float x, float z, boolean air, float heading) {
        aimed = aimedNow;
        aimedNow = false;
        flash = Math.max(0f, flash - dt);
        if (!aimed) {
            wayN = 0;
            wentN = 0;
            return;
        }
        if (air) {
            wayN = 0;
            put(x, z);
            put(goalX, goalZ);
            wentN = 0;
            measure();
            return;
        }
        again -= dt;
        if (again <= 0f || wayN < 2) {
            again = AGAIN;
            find(x, z);
        } else {
            // between findings, the way starts where she is
            way[0] = x;
            way[1] = z;
        }
        measure();
        // the first street of the way: if it goes off behind her, she is to turn round
        back = false;
        if (wayN >= 3) {
            float dx = way[4] - way[2], dz = way[5] - way[3], l = (float) Math.sqrt(dx * dx + dz * dz);
            if (l > 2f) back = (dx * (float) Math.sin(heading) + dz * (float) Math.cos(heading)) / l < -0.5f;
        }
    }

    // ---- the streets as a graph ----
    private int lo, cols, rows, roadN, nodes;
    private float[] dist = new float[0];
    private int[] from = new int[0];
    private boolean[] done = new boolean[0];

    private float nodeX(int n) {
        if (n < cols * rows) return (lo + n / rows) * City.CELL;
        return City.routeX(roadZ(n - cols * rows));
    }

    private float nodeZ(int n) {
        if (n < cols * rows) return (n % rows) * City.CELL;
        return roadZ(n - cols * rows);
    }

    private static float roadZ(int k) {
        return Math.min(City.VILLAGE_Z, City.EDGE_J * City.CELL + k * ROAD_STEP);
    }

    private int town(int i, int j) {
        return (i - lo) * rows + j;
    }

    /** The node the road out leaves the town by: the crossing it starts at. */
    private int roadStart() {
        int i = Math.round(City.ROUTE_X / City.CELL);
        return i >= lo && i < lo + cols ? town(i, City.EDGE_J) : -1;
    }

    /** Every edge, as its two nodes, by number e: town east-west, town north-south, the road out. */
    private int edges() {
        return (cols - 1) * rows + cols * (rows - 1) + Math.max(0, roadN - 1) + (roadN > 0 && roadStart() >= 0 ? 1 : 0);
    }

    private int edgeA(int e) {
        int h = (cols - 1) * rows;
        if (e < h) return e;
        e -= h;
        int v = cols * (rows - 1);
        if (e < v) return (e / (rows - 1)) * rows + e % (rows - 1);
        e -= v;
        if (e < roadN - 1) return cols * rows + e;
        return roadStart();
    }

    private int edgeB(int e) {
        int h = (cols - 1) * rows;
        if (e < h) return e + rows;
        e -= h;
        int v = cols * (rows - 1);
        if (e < v) return (e / (rows - 1)) * rows + e % (rows - 1) + 1;
        e -= v;
        if (e < roadN - 1) return cols * rows + e + 1;
        return cols * rows;
    }

    // the nearest edge to a point: its number, where on it (0 at a, 1 at b), how far off
    private int nearE;
    private float nearT, nearD;

    private void nearest(float x, float z) {
        nearE = -1;
        nearD = Float.MAX_VALUE;
        int m = edges();
        for (int e = 0; e < m; e++) {
            int a = edgeA(e), b = edgeB(e);
            float ax = nodeX(a), az = nodeZ(a), bx = nodeX(b), bz = nodeZ(b);
            float dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
            float t = l2 < 1e-4f ? 0f : Math.max(0f, Math.min(1f, ((x - ax) * dx + (z - az) * dz) / l2));
            float px = ax + dx * t - x, pz = az + dz * t - z, d = px * px + pz * pz;
            if (d < nearD) {
                nearD = d;
                nearE = e;
                nearT = t;
            }
        }
    }

    private float edgeLength(int e) {
        int a = edgeA(e), b = edgeB(e);
        return (float) Math.hypot(nodeX(b) - nodeX(a), nodeZ(b) - nodeZ(a));
    }

    // the mountain's road, down from where she (or the place) is: so many points
    private final float[] hill = new float[2 * 1200];

    /**
     * The way from (x, z) to the place, found anew: along the streets by the shortest way; from up
     * on the mountain, first down its road to the town's last street, and to a place up on it, up
     * its road from there.
     */
    private void find(float x, float z) {
        boolean fromHill = Mountain.onHill(x, z), toHill = Mountain.onHill(goalX, goalZ);
        wayN = 0;
        put(x, z);
        if (fromHill && toHill) {
            put(goalX, goalZ);
            wentN = 0;
            return;
        }
        float sx = x, sz = z, gx = goalX, gz = goalZ;
        if (fromHill) {
            int n = Mountain.down(x, z, hill);
            for (int k = 0; k < n; k++) put(hill[k * 2], hill[k * 2 + 1]);
            sx = hill[n * 2 - 2];
            sz = hill[n * 2 - 1];
        }
        int up = 0;
        if (toHill) {
            up = Mountain.down(goalX, goalZ, hill);
            gx = hill[up * 2 - 2];
            gz = hill[up * 2 - 1];
        }
        town(sx, sz, gx, gz, fromHill);
        for (int k = up - 1; k >= 0; k--) put(hill[k * 2], hill[k * 2 + 1]);
        if (toHill || fromHill) put(goalX, goalZ);
    }

    /** The streets' part of the way, from (x, z) to (gx, gz), onto the way so far; (x, z) itself put if not already. */
    private void town(float x, float z, float goalX, float goalZ, boolean already) {
        int i0 = (int) Math.floor(Math.min(x, goalX) / City.CELL) - MARGIN;
        int i1 = (int) Math.ceil(Math.max(x, goalX) / City.CELL) + MARGIN;
        if (i1 - i0 + 1 > COLUMNS) {
            // too wide apart: only the columns round her, the rest straight
            i0 = (int) Math.floor(x / City.CELL) - COLUMNS / 2;
            i1 = i0 + COLUMNS - 1;
        }
        lo = i0;
        cols = i1 - i0 + 1;
        rows = City.EDGE_J + 1;
        roadN = 1 + (int) Math.ceil((City.VILLAGE_Z - City.EDGE_J * City.CELL) / ROAD_STEP);
        if (roadStart() < 0) roadN = 0;
        nodes = cols * rows + roadN;
        if (dist.length < nodes + 2) {
            dist = new float[nodes + 2];
            from = new int[nodes + 2];
            done = new boolean[nodes + 2];
        }
        nearest(x, z);
        int se = nearE;
        float st = nearT;
        nearest(goalX, goalZ);
        int ge = nearE;
        float gt = nearT, gd = (float) Math.sqrt(nearD);
        int[] list = wentNow.length >= nodes ? wentNow : (wentNow = new int[nodes]);
        int n = 0;
        if (se >= 0 && ge >= 0 && se != ge) {
            // the shortest way, from both ends of her edge to either end of the place's
            for (int k = 0; k < nodes; k++) {
                dist[k] = Float.MAX_VALUE;
                from[k] = -1;
                done[k] = false;
            }
            float sl = edgeLength(se), gl = edgeLength(ge);
            int sa = edgeA(se), sb = edgeB(se), ga = edgeA(ge), gb = edgeB(ge);
            dist[sa] = st * sl;
            dist[sb] = (1f - st) * sl;
            while (true) {
                int u = -1;
                float best = Float.MAX_VALUE;
                for (int k = 0; k < nodes; k++)
                    if (!done[k] && dist[k] < best) {
                        best = dist[k];
                        u = k;
                    }
                if (u < 0) break;
                done[u] = true;
                if (done[ga] && done[gb]) break;
                relaxFrom(u);
            }
            // the place's end that comes out shorter in all
            float viaA = dist[ga] + gt * gl, viaB = dist[gb] + (1f - gt) * gl;
            int end = viaA <= viaB ? ga : gb;
            for (int k = end; k >= 0; k = from[k]) list[n++] = k;
            // reversed: from her end to the place's
            for (int a = 0, b = n - 1; a < b; a++, b--) {
                int t = list[a];
                list[a] = list[b];
                list[b] = t;
            }
        }
        // a new way, not the old one shortened: it flashes
        if (wentN > 0 && n > 0 && !contains(went, wentN, list[0]) && !contains(went, wentN, n > 1 ? list[1] : list[0])) flash = FLASH;
        int[] t = went;
        went = list;
        wentNow = t;
        wentN = n;
        if (!already) put(x, z);
        if (se >= 0) {
            int a = edgeA(se), b = edgeB(se);
            put(nodeX(a) + (nodeX(b) - nodeX(a)) * st, nodeZ(a) + (nodeZ(b) - nodeZ(a)) * st);
        }
        for (int k = 0; k < n; k++) put(nodeX(went[k]), nodeZ(went[k]));
        if (ge >= 0) {
            int a = edgeA(ge), b = edgeB(ge);
            put(nodeX(a) + (nodeX(b) - nodeX(a)) * gt, nodeZ(a) + (nodeZ(b) - nodeZ(a)) * gt);
        }
        if (gd > 1f || ge < 0) put(goalX, goalZ);
    }

    private static boolean contains(int[] a, int n, int v) {
        for (int k = 0; k < n; k++) if (a[k] == v) return true;
        return false;
    }

    /** From node u, every street out of it: the next crossing each way, and the road out. */
    private void relaxFrom(int u) {
        if (u < cols * rows) {
            int i = lo + u / rows, j = u % rows;
            if (i > lo) relax(u, town(i - 1, j), City.CELL);
            if (i < lo + cols - 1) relax(u, town(i + 1, j), City.CELL);
            if (j > 0) relax(u, town(i, j - 1), City.CELL);
            if (j < rows - 1) relax(u, town(i, j + 1), City.CELL);
            if (roadN > 0 && u == roadStart()) relax(u, cols * rows, 0f);
        } else {
            int k = u - cols * rows;
            float zk = roadZ(k);
            if (k > 0) relax(u, u - 1, zk - roadZ(k - 1));
            else relax(u, roadStart(), 0f);
            if (k < roadN - 1) relax(u, u + 1, roadZ(k + 1) - zk);
        }
    }

    private void relax(int u, int v, float l) {
        if (v < 0 || done[v]) return;
        float d = dist[u] + l;
        if (d < dist[v]) {
            dist[v] = d;
            from[v] = u;
        }
    }

    private void put(float x, float z) {
        if (wayN >= 2 && Math.abs(way[wayN * 2 - 2] - x) < 0.5f && Math.abs(way[wayN * 2 - 1] - z) < 0.5f) return;
        if (wayN * 2 + 2 > way.length) {
            float[] more = new float[way.length * 2];
            System.arraycopy(way, 0, more, 0, way.length);
            way = more;
        }
        way[wayN * 2] = x;
        way[wayN * 2 + 1] = z;
        wayN++;
    }

    /** How far along the way, and its next turn: the first bend past TURN, not counting the last leg off the street. */
    private void measure() {
        left = 0f;
        toTurn = -1f;
        turn = 0;
        for (int k = 1; k < wayN; k++) {
            float dx = way[k * 2] - way[k * 2 - 2], dz = way[k * 2 + 1] - way[k * 2 - 1];
            float l = (float) Math.sqrt(dx * dx + dz * dz);
            // the first leg (from her onto the street) is no street of the way's, nor the last (off it to the place)
            if (toTurn < 0f && k >= 2 && k + 1 < wayN - 1 && l > 0.5f) {
                float ex = way[k * 2 + 2] - way[k * 2], ez = way[k * 2 + 3] - way[k * 2 + 1];
                float cross = dx * ez - dz * ex, dot = dx * ex + dz * ez;
                float a = (float) Math.atan2(cross, dot);
                if (Math.abs(a) > TURN) {
                    toTurn = left + l;
                    // x to the right, z ahead: turning from +z toward +x is a turn right, the cross negative
                    turn = a < 0f ? 1 : -1;
                }
            }
            left += l;
        }
    }

    // ---- the map ----

    /**
     * The map at (x0, y0), turned so the eye's way is up: she at (x, z), the eye along yaw, going
     * so fast (the faster, the farther it shows); her own way, for the mark of her.
     */
    void draw(Px f, int x0, int y0, float x, float z, float yaw, float heading, float speed, float dt, float[] dots, int dotN) {
        float want = SCALE + SCALE_SPEED * Math.abs(speed);
        scale += (want - scale) * Math.min(1f, dt * 1.5f);
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        float cx = SIDE * AT_ACROSS, cy = SIDE * AT_DOWN;
        // the ground, pixel by pixel: from the map back to the world
        for (int py = 0; py < SIDE; py++) {
            float fwd = (cy - py - 0.5f) * scale;
            for (int px = 0; px < SIDE; px++) {
                if (corner(px, py, SIDE, TALL)) continue;
                float rt = (px + 0.5f - cx) * scale;
                float wx = x + rt * c + fwd * s, wz = z - rt * s + fwd * c;
                f.set(x0 + px, y0 + py, ground(wx, wz));
            }
        }
        // the way
        int line = flash > 0f && ((int) (flash * 8f) & 1) == 0 ? LINE_FLASH : LINE;
        for (int k = 1; k < wayN; k++) {
            float ax = way[k * 2 - 2] - x, az = way[k * 2 - 1] - z, bx = way[k * 2] - x, bz = way[k * 2 + 1] - z;
            float ux = cx + (ax * c - az * s) / scale, uy = cy - (ax * s + az * c) / scale;
            float vx = cx + (bx * c - bz * s) / scale, vy = cy - (bx * s + bz * c) / scale;
            thick(f, x0, y0, ux, uy, vx, vy, 1.6f, line);
        }
        // others on their ways: small dots, where they are on the map
        for (int k = 0; k < dotN; k++) {
            float ox = dots[k * 2] - x, oz = dots[k * 2 + 1] - z;
            float ux = cx + (ox * c - oz * s) / scale, uy = cy - (ox * s + oz * c) / scale;
            if (ux < 2f || uy < 2f || ux > SIDE - 2f || uy > SIDE - 2f) continue;
            disc(f, x0, y0, ux, uy, 2.4f, EDGE);
            disc(f, x0, y0, ux, uy, 1.7f, OTHER);
        }
        // the place: a dot, kept at the edge when it is off the map
        if (aimed) {
            float gx = goalX - x, gz = goalZ - z;
            float ux = cx + (gx * c - gz * s) / scale, uy = cy - (gx * s + gz * c) / scale;
            ux = Math.max(5f, Math.min(SIDE - 5f, ux));
            uy = Math.max(5f, Math.min(SIDE - 5f, uy));
            disc(f, x0, y0, ux, uy, 4.2f, EDGE);
            disc(f, x0, y0, ux, uy, 3.2f, PLACE);
        }
        // she: a blue dot, a white point on it her own way
        disc(f, x0, y0, cx, cy, 5.5f, ME_RIM);
        disc(f, x0, y0, cx, cy, 4.5f, ME);
        float rel = heading - yaw, hx = (float) Math.sin(rel), hy = -(float) Math.cos(rel);
        point(f, x0, y0, cx + hx * 1.5f, cy + hy * 1.5f, hx, hy, 4f, ME_RIM);
    }

    /** The strip under the map: the next turn (or the place ahead) and how far it is. */
    void strip(Px f, int x0, int y0) {
        for (int py = 0; py < STRIP; py++)
            for (int px = 0; px < SIDE; px++)
                if (!corner(px, py + SIDE, SIDE, TALL)) f.set(x0 + px, y0 + py, BAND);
        // the edge round map and strip
        int top = y0 - SIDE;
        for (int p = 0; p < SIDE; p++) {
            edge(f, x0 + p, top, x0, top);
            edge(f, x0 + p, top + TALL - 1, x0, top);
        }
        for (int p = 0; p < TALL; p++) {
            edge(f, x0, top + p, x0, top);
            edge(f, x0 + SIDE - 1, top + p, x0, top);
        }
        if (!led()) return;
        float d = toTurn >= 0f ? toTurn : left;
        // the turn's sign: a point left, right or ahead
        float ax = 9f, ay = STRIP * 0.5f;
        if (back) {
            // round: a point down, a stroke up from it
            for (int py = 3; py < 9; py++) {
                f.set(x0 + 8, y0 + py, TEXT);
                f.set(x0 + 9, y0 + py, TEXT);
            }
            point(f, x0, y0, ax, ay + 1f, 0f, 1f, 5f, TEXT);
            d = left;
        } else if (turn < 0 && toTurn >= 0f) point(f, x0, y0, ax, ay, -1f, 0f, 5f, TEXT);
        else if (turn > 0 && toTurn >= 0f) point(f, x0, y0, ax, ay, 1f, 0f, 5f, TEXT);
        else point(f, x0, y0, ax, ay, 0f, -1f, 5f, TEXT);
        String t = d >= 1000f ? String.format(java.util.Locale.ROOT, "%.1f KM", d / 1000f) : (Math.round(d / 10f) * 10) + " M";
        PixelFont.draw(f, t, x0 + 20, y0 + 5, 1, INK);
        PixelFont.draw(f, t, x0 + 19, y0 + 4, 1, TEXT);
    }

    /** What the map shows at (x, z): the sea, a street, a block in the town, the fields. */
    static int ground(float x, float z) {
        if (City.overWater(x, z)) return SEA;
        if (z < -City.CURB) return SAND;
        float top = City.EDGE_J * City.CELL;
        if (z > -City.ROAD && z < top + City.ROAD) {
            float gx = x - Math.round(x / City.CELL) * City.CELL;
            float gz = z - Math.round(z / City.CELL) * City.CELL;
            if (Math.abs(gz) < City.ROAD * 0.75f) return STREET;
            if (Math.abs(gx) < City.ROAD * 0.75f && z > 0f && z < top) return STREET;
        }
        if (City.onRoute(x, z, -1f)) return STREET;
        if (z > top + City.CURB && Mountain.near(x, z)) {
            if (!Float.isNaN(Mountain.road(x, z))) return STREET;
            float h = Mountain.cone(x, z);
            if (h > 0f) return h > 100f ? SNOW : ROCK;
        }
        return z < top + City.CURB ? BLOCK : FIELD;
    }

    /** The corners rounded: whether (px, py) of a w by h square is outside them. */
    private static boolean corner(int px, int py, int w, int h) {
        int r = 5;
        int dx = px < r ? r - px : px >= w - r ? px - (w - r - 1) : 0;
        int dy = py < r ? r - py : py >= h - r ? py - (h - r - 1) : 0;
        return dx * dx + dy * dy > r * r;
    }

    private static void edge(Px f, int x, int y, int x0, int y0) {
        if (!corner(x - x0, y - y0, SIDE, TALL)) f.set(x, y, EDGE);
    }

    private static boolean inside(float px, float py) {
        return px >= 0f && py >= 0f && px < SIDE && py < SIDE && !corner((int) px, (int) py, SIDE, TALL);
    }

    /** A line on the map from (ax, ay) to (bx, by), so thick (half), kept within it. */
    private static void thick(Px f, int x0, int y0, float ax, float ay, float bx, float by, float half, int color) {
        float minX = Math.max(0f, Math.min(ax, bx) - half - 1f), maxX = Math.min(SIDE - 1f, Math.max(ax, bx) + half + 1f);
        float minY = Math.max(0f, Math.min(ay, by) - half - 1f), maxY = Math.min(SIDE - 1f, Math.max(ay, by) + half + 1f);
        float dx = bx - ax, dy = by - ay, l2 = dx * dx + dy * dy;
        for (int py = (int) minY; py <= (int) maxY; py++)
            for (int px = (int) minX; px <= (int) maxX; px++) {
                float qx = px + 0.5f - ax, qy = py + 0.5f - ay;
                float t = l2 < 1e-4f ? 0f : Math.max(0f, Math.min(1f, (qx * dx + qy * dy) / l2));
                float ex = qx - dx * t, ey = qy - dy * t;
                if (ex * ex + ey * ey <= half * half && inside(px, py)) f.set(x0 + px, y0 + py, color);
            }
    }

    private static void disc(Px f, int x0, int y0, float cx, float cy, float r, int color) {
        for (int py = (int) (cy - r - 1); py <= (int) (cy + r + 1); py++)
            for (int px = (int) (cx - r - 1); px <= (int) (cx + r + 1); px++) {
                float dx = px + 0.5f - cx, dy = py + 0.5f - cy;
                if (dx * dx + dy * dy <= r * r && inside(px, py)) f.set(x0 + px, y0 + py, color);
            }
    }

    /** A filled point at (cx, cy), its tip out along (ex, ey), so long. */
    static void point(Px f, int x0, int y0, float cx, float cy, float ex, float ey, float size, int color) {
        float m = (float) Math.sqrt(ex * ex + ey * ey);
        ex /= m;
        ey /= m;
        float tx = cx + ex * size, ty = cy + ey * size;
        float lx = cx - ex * size * 0.6f - ey * size * 0.8f, ly = cy - ey * size * 0.6f + ex * size * 0.8f;
        float rx = cx - ex * size * 0.6f + ey * size * 0.8f, ry = cy - ey * size * 0.6f - ex * size * 0.8f;
        int a = (int) Math.floor(Math.min(tx, Math.min(lx, rx))), b = (int) Math.ceil(Math.max(tx, Math.max(lx, rx)));
        int p = (int) Math.floor(Math.min(ty, Math.min(ly, ry))), q = (int) Math.ceil(Math.max(ty, Math.max(ly, ry)));
        for (int py = p; py <= q; py++)
            for (int px = a; px <= b; px++) {
                float x = px + 0.5f, y = py + 0.5f;
                float d1 = (x - lx) * (ty - ly) - (y - ly) * (tx - lx);
                float d2 = (x - tx) * (ry - ty) - (y - ty) * (rx - tx);
                float d3 = (x - rx) * (ly - ry) - (y - ry) * (lx - rx);
                boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
                if (!(neg && pos)) f.set(x0 + px, y0 + py, color);
            }
    }
}
