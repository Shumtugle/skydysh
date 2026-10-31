package com.shumtugle.skydysh;

/**
 * Where she can go: what she stands on, what is in her way, and what is over her head. The town
 * is one such place (see City); a place indoors is another (see Indoors).
 */
interface Ground {
    /** The highest thing under (x, z) that one whose feet are at feet can stand on, no higher than reach above them. */
    float ground(float x, float z, float feet, float reach);

    /** A round body of radius r at (p[0], p[1]), feet at feet, pushed out of what it went into; true if it touched. */
    boolean push(float[] p, float r, float feet);

    /** How high the ceiling is over (x, z); very high where there is none. */
    float ceiling(float x, float z);

    /** The eye at (p[0], p[1]) kept where it can see from: indoors, within the walls. */
    void keepEye(float[] p);
}
