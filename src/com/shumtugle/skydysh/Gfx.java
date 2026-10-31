package com.shumtugle.skydysh;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * The phone's renderer: draws a DrawList with the shaders in assets, in three passes. The
 * sun's depth map; the sky and the boxes at the screen's own size, the colour with what each
 * pixel belongs to in its alpha, and the depth kept; then onto the screen, inked where things
 * meet, with halftone and grain, the flat layer and the aim over it. Balls go after the boxes
 * in each pass, with the same shaders built for them. The desktop stand's page does the same
 * passes with the same shaders, so what the stand shows is what the phone shows. GL thread only.
 */
final class Gfx {
    private static final int SHADOW_SIZE = 2048;
    private static final float[] CUBE = cube();

    private int boxP, shadowP, skyP, inkP, blitP, ballP, ballShadowP, meshP, meshShadowP;
    // the town's people as meshes, when the game has them: one buffer of corners for all, one of
    // indices, where each model's run starts and how long it is; their shared picture
    private int meshVao, meshBuf, meshIdx, pictureTex;
    private int[] meshFirst, meshCount;
    private final float[] boneQ = new float[FolkPack.BONES * 4], boneT = new float[FolkPack.BONES * 3];
    private int vao, empty, cubeBuf, instBuf, ballVao, ballBuf;
    private int shadowTex, shadowFb, sceneTex, sceneFb, depthTex, printTex, printFb, flatTex, hudTex, hudW, hudH;
    private int frameW, frameH, drawW, drawH;
    private FloatBuffer inst, ballInst;
    private IntBuffer flat, hudBuf;
    private final int[] hudTexs = new int[2], hudSizeW = new int[2], hudSizeH = new int[2];
    private int hudSlot;
    private int badgesTex;                    // the sheet of buttons and signs, sent once
    private int[] hudSwizzled;
    private int[] swizzled;

    /** Everything that belongs to a GL context; called again whenever the context is new. */
    void create(AssetManager assets) {
        boxP = program(read(assets, "box.vert"), read(assets, "box.frag"));
        shadowP = program(read(assets, "shadow.vert"), read(assets, "shadow.frag"));
        skyP = program(read(assets, "full.vert"), read(assets, "sky.frag"));
        inkP = program(read(assets, "full.vert"), read(assets, "ink.frag"));
        blitP = program(read(assets, "full.vert"), read(assets, "blit.frag"));
        ballP = program(sphere(read(assets, "box.vert")), sphere(read(assets, "box.frag")));
        ballShadowP = program(sphere(read(assets, "shadow.vert")), sphere(read(assets, "shadow.frag")));
        meshP = program(read(assets, "mesh.vert"), read(assets, "mesh.frag"));
        meshShadowP = program(define(read(assets, "mesh.vert"), "SHADOW"), read(assets, "meshshadow.frag"));
        meshes(assets);
        int[] ids = new int[3];
        GLES30.glGenVertexArrays(3, ids, 0);
        vao = ids[0];
        empty = ids[1];
        ballVao = ids[2];
        GLES30.glGenBuffers(3, ids, 0);
        cubeBuf = ids[0];
        instBuf = ids[1];
        ballBuf = ids[2];
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeBuf);
        FloatBuffer cb = floats(CUBE.length);
        cb.put(CUBE).position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, CUBE.length * 4, cb, GLES30.GL_STATIC_DRAW);
        instances(vao, instBuf);
        instances(ballVao, ballBuf);

        // the sun's depth map, compared on lookup, smoothed by the hardware
        GLES30.glGenTextures(1, ids, 0);
        shadowTex = ids[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex);
        GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_DEPTH_COMPONENT24, SHADOW_SIZE, SHADOW_SIZE);
        params(GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_MODE, GLES30.GL_COMPARE_REF_TO_TEXTURE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_COMPARE_FUNC, GLES30.GL_LEQUAL);
        GLES30.glGenFramebuffers(1, ids, 0);
        shadowFb = ids[0];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFb);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, shadowTex, 0);
        GLES30.glDrawBuffers(1, new int[]{GLES30.GL_NONE}, 0);
        GLES30.glReadBuffer(GLES30.GL_NONE);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        frameW = frameH = drawW = drawH = 0;
        hudTex = 0;
        badgesTex = 0;
        hudTexs[0] = hudTexs[1] = 0;
        hudSizeW[0] = hudSizeW[1] = hudSizeH[0] = hudSizeH[1] = 0;
    }

    /** A vertex array: the cube's corners, and one box after another from the given buffer. */
    private void instances(int array, int buf) {
        GLES30.glBindVertexArray(array);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeBuf);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buf);
        int stride = DrawList.STRIDE * 4;
        int[][] attrs = {{2, 3, 0}, {3, 3, 3}, {4, 3, 6}, {5, 3, 9}, {6, 3, 12}, {7, 4, 15}};
        for (int[] a : attrs) {
            GLES30.glEnableVertexAttribArray(a[0]);
            GLES30.glVertexAttribPointer(a[0], a[1], GLES30.GL_FLOAT, false, stride, a[2] * 4);
            GLES30.glVertexAttribDivisor(a[0], 1);
        }
        GLES30.glBindVertexArray(0);
    }

    /**
     * The drawn scene and its print at the size it is drawn at (a few times the frame, well under
     * the screen), and the flat layer at the frame's.
     */
    private void size(int w, int h, int sw, int sh) {
        int[] ids = new int[1];
        if (sw != drawW || sh != drawH) {
            if (drawW != 0) {
                GLES30.glDeleteTextures(3, new int[]{sceneTex, depthTex, printTex}, 0);
                GLES30.glDeleteFramebuffers(2, new int[]{sceneFb, printFb}, 0);
            }
            drawW = sw;
            drawH = sh;
            GLES30.glGenTextures(1, ids, 0);
            sceneTex = ids[0];
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTex);
            GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, sw, sh);
            params(GLES30.GL_NEAREST);
            GLES30.glGenTextures(1, ids, 0);
            depthTex = ids[0];
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTex);
            GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_DEPTH_COMPONENT24, sw, sh);
            params(GLES30.GL_NEAREST);
            GLES30.glGenFramebuffers(1, ids, 0);
            sceneFb = ids[0];
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFb);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, sceneTex, 0);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_DEPTH_ATTACHMENT, GLES30.GL_TEXTURE_2D, depthTex, 0);
            // the print: scaled up to the screen smoothly
            GLES30.glGenTextures(1, ids, 0);
            printTex = ids[0];
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, printTex);
            GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, sw, sh);
            params(GLES30.GL_LINEAR);
            GLES30.glGenFramebuffers(1, ids, 0);
            printFb = ids[0];
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, printFb);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, printTex, 0);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        }
        if (w == frameW && h == frameH) return;
        if (frameW != 0) GLES30.glDeleteTextures(1, new int[]{flatTex}, 0);
        frameW = w;
        frameH = h;
        GLES30.glGenTextures(1, ids, 0);
        flatTex = ids[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, flatTex);
        GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, w, h);
        params(GLES30.GL_NEAREST);
        flat = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        swizzled = new int[w * h];
    }

    /**
     * Draws a frame. {@code q} is how many drawn pixels make a frame pixel: the scene, its ink
     * and its dots are drawn at that size and then scaled up to the screen.
     */
    void draw(DrawList d, int screenW, int screenH, float q) {
        float screenScale = screenW / (float) d.w;
        q = Math.max(1f, Math.min(q, screenScale));
        int sceneW = Math.max(1, Math.round(d.w * q)), sceneH = Math.max(1, Math.round(d.h * q));
        float scale = sceneW / (float) d.w;
        size(d.w, d.h, sceneW, sceneH);
        int n = d.count;
        if (inst == null || inst.capacity() < n * DrawList.STRIDE) inst = floats(Math.max(n, 256) * DrawList.STRIDE * 2);
        inst.clear();
        inst.put(d.boxes, 0, n * DrawList.STRIDE).position(0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, instBuf);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, n * DrawList.STRIDE * 4, inst, GLES30.GL_DYNAMIC_DRAW);
        int nb = d.ballCount;
        if (nb > 0) {
            if (ballInst == null || ballInst.capacity() < nb * DrawList.STRIDE) ballInst = floats(Math.max(nb, 16) * DrawList.STRIDE * 2);
            ballInst.clear();
            ballInst.put(d.balls, 0, nb * DrawList.STRIDE).position(0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ballBuf);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, nb * DrawList.STRIDE * 4, ballInst, GLES30.GL_DYNAMIC_DRAW);
        }
        // the flat layer: ARGB in, bytes in R, G, B, A order out
        int[] src = d.flat, dst = swizzled;
        for (int i = 0; i < src.length; i++) {
            int c = src[i];
            dst[i] = (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
        }
        flat.clear();
        flat.put(dst).position(0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, flatTex);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4);
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, d.w, d.h, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, flat);
        // the fine layer over the frame, when there is one: smooth, premultiplied. Two are kept,
        // one for each size it comes in (the crest over the top, the room over all of it), so
        // going from one to the other between rounds makes nothing new
        boolean hudNew = false;
        if (d.hudRows > 0 && d.hud != null) {
            int slot = hudSizeW[0] == d.hudW && hudSizeH[0] == d.hudH && hudTexs[0] != 0 ? 0
                    : hudSizeW[1] == d.hudW && hudSizeH[1] == d.hudH && hudTexs[1] != 0 ? 1 : -1;
            boolean newHud = slot < 0;
            if (newHud) {
                slot = hudTexs[0] == 0 ? 0 : hudTexs[1] == 0 ? 1 : 1 - hudSlot;
                if (hudTexs[slot] != 0) GLES30.glDeleteTextures(1, new int[]{hudTexs[slot]}, 0);
                int[] t = new int[1];
                GLES30.glGenTextures(1, t, 0);
                hudTexs[slot] = t[0];
                hudSizeW[slot] = d.hudW;
                hudSizeH[slot] = d.hudH;
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, t[0]);
                GLES30.glTexStorage2D(GLES30.GL_TEXTURE_2D, 1, GLES30.GL_RGBA8, d.hudW, d.hudH);
                params(GLES30.GL_LINEAR);
            }
            hudNew = newHud;
            hudSlot = slot;
            hudTex = hudTexs[slot];
            hudW = d.hudW;
            hudH = d.hudH;
        }
        if (d.hudRows > 0 && d.hud != null && (d.hudFresh || hudNew)) {
            int need = hudW * hudH;
            if (hudSwizzled == null || hudSwizzled.length < need) {
                hudBuf = ByteBuffer.allocateDirect(need * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
                hudSwizzled = new int[need];
            }
            int[] hs = d.hud, hd = hudSwizzled;
            for (int i = 0; i < need; i++) {
                int c = hs[i];
                hd[i] = (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
            }
            hudBuf.clear();
            hudBuf.put(hd, 0, need).position(0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hudTex);
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4);
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, hudW, hudH, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, hudBuf);
        }

        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_CULL_FACE);
        GLES30.glDisable(GLES30.GL_BLEND);

        // 1. the sun's depth map
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, shadowFb);
        GLES30.glViewport(0, 0, SHADOW_SIZE, SHADOW_SIZE);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glUseProgram(shadowP);
        GLES30.glUniformMatrix4fv(u(shadowP, "uLight"), 1, false, d.lightProj, 0);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, n);
        if (nb > 0) {
            GLES30.glUseProgram(ballShadowP);
            GLES30.glUniformMatrix4fv(u(ballShadowP, "uLight"), 1, false, d.lightProj, 0);
            GLES30.glUniform3fv(u(ballShadowP, "uSun"), 1, d.sun, 0);
            GLES30.glBindVertexArray(ballVao);
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, nb);
        }
        if (meshFirst != null && d.folkN > 0) {
            GLES30.glUseProgram(meshShadowP);
            GLES30.glUniformMatrix4fv(u(meshShadowP, "uLight"), 1, false, d.lightProj, 0);
            people(meshShadowP, d);
        }

        // 2. the sky, then the boxes, at the drawing size
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, sceneFb);
        GLES30.glViewport(0, 0, sceneW, sceneH);
        GLES30.glClearColor(0f, 0f, 0f, 0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glUseProgram(skyP);
        GLES30.glUniform3fv(u(skyP, "uRight"), 1, d.right, 0);
        GLES30.glUniform3fv(u(skyP, "uUp"), 1, d.up, 0);
        GLES30.glUniform3fv(u(skyP, "uAhead"), 1, d.ahead, 0);
        GLES30.glUniform2f(u(skyP, "uFrame"), sceneW, sceneH);
        GLES30.glUniform1f(u(skyP, "uFocal"), d.focal * scale);
        GLES30.glUniform1f(u(skyP, "uScale"), scale);
        GLES30.glUniform3fv(u(skyP, "uTop"), 1, d.skyTop, 0);
        GLES30.glUniform3fv(u(skyP, "uHorizon"), 1, d.skyHorizon, 0);
        GLES30.glUniform3fv(u(skyP, "uGlow"), 1, d.skyGlow, 0);
        GLES30.glUniform3fv(u(skyP, "uSun"), 1, d.sun, 0);
        GLES30.glUniform1f(u(skyP, "uTime"), d.time);
        GLES30.glUniform1f(u(skyP, "uNight"), d.night);
        GLES30.glUniform1f(u(skyP, "uDark"), d.spot[4]);
        GLES30.glUniform1i(u(skyP, "uFlyN"), d.flyN);
        if (d.flyN > 0) GLES30.glUniform4fv(u(skyP, "uFly"), d.flyN * 2, d.fly, 0);
        GLES30.glBindVertexArray(empty);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        lit(boxP, d);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, n);
        if (meshFirst != null && d.folkN > 0) {
            lit(meshP, d);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pictureTex);
            GLES30.glUniform1i(u(meshP, "uPicture"), 1);
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            people(meshP, d);
        }
        if (nb > 0) {
            lit(ballP, d);
            GLES30.glUniform1f(u(ballP, "uClear"), 0f);
            GLES30.glBindVertexArray(ballVao);
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, nb);
            // then clear plastic, laid over all of it: tested against the depth, not written to
            // it, and leaving what the pixel belongs to as it was
            GLES30.glUniform1f(u(ballP, "uClear"), 1f);
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE_MINUS_SRC_ALPHA);
            GLES30.glDepthMask(false);
            GLES30.glColorMask(true, true, true, false);
            GLES30.glDrawArraysInstanced(GLES30.GL_TRIANGLES, 0, 36, nb);
            GLES30.glColorMask(true, true, true, true);
            GLES30.glDepthMask(true);
            GLES30.glDisable(GLES30.GL_BLEND);
            GLES30.glUniform1f(u(ballP, "uClear"), 0f);
        }

        // 3. the print: grade, dots and ink, at the drawing size
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, printFb);
        GLES30.glViewport(0, 0, sceneW, sceneH);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glUseProgram(inkP);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sceneTex);
        GLES30.glUniform1i(u(inkP, "uScene"), 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTex);
        GLES30.glUniform1i(u(inkP, "uDepth"), 3);
        GLES30.glUniform3fv(u(inkP, "uGrade"), 1, d.grade, 0);
        GLES30.glUniform1f(u(inkP, "uScale"), scale);
        GLES30.glUniform1f(u(inkP, "uNight"), d.night);
        GLES30.glUniform4fv(u(inkP, "uHalo"), 1, d.halo, 0);
        GLES30.glUniform1f(u(inkP, "uHaloDepth"), d.halo[4]);
        GLES30.glBindVertexArray(empty);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);

        // 4. onto the screen: the print scaled up, the layers and the aim over it
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glViewport(0, 0, screenW, screenH);
        GLES30.glUseProgram(blitP);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, printTex);
        GLES30.glUniform1i(u(blitP, "uPrint"), 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, flatTex);
        GLES30.glUniform1i(u(blitP, "uFlat"), 1);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hudTex);
        GLES30.glUniform1i(u(blitP, "uHud"), 2);
        GLES30.glUniform1f(u(blitP, "uHudRows"), hudTex != 0 ? d.hudRows : 0f);
        if (d.badgeN > 0 && badgesTex == 0) badges();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, badgesTex);
        GLES30.glUniform1i(u(blitP, "uBadges"), 3);
        GLES30.glUniform1i(u(blitP, "uBadgeN"), badgesTex != 0 ? d.badgeN : 0);
        if (d.badgeN > 0) {
            GLES30.glUniform4fv(u(blitP, "uBadge"), d.badgeN, d.badge, 0);
            GLES30.glUniform2fv(u(blitP, "uBadgeOf"), d.badgeN, d.badgeOf, 0);
        }
        GLES30.glUniform2f(u(blitP, "uFrameSize"), d.w, d.h);
        GLES30.glUniform2f(u(blitP, "uScreen"), screenW, screenH);
        GLES30.glUniform1f(u(blitP, "uScale"), screenScale);
        GLES30.glUniform1f(u(blitP, "uTime"), d.time);
        GLES30.glUniform4fv(u(blitP, "uTube"), 1, d.tube, 0);
        GLES30.glUniform4fv(u(blitP, "uRing"), 1, d.ring, 0);
        GLES30.glUniform3fv(u(blitP, "uFinger"), 1, d.finger, 0);
        GLES30.glUniform1i(u(blitP, "uTrailN"), d.trailN);
        if (d.trailN > 0) {
            GLES30.glUniform3fv(u(blitP, "uTrail"), d.trailN, d.trail, 0);
            float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
            for (int i = 0; i < d.trailN; i++) {
                float x = d.trail[i * 3], y = d.trail[i * 3 + 1];
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
            }
            GLES30.glUniform4f(u(blitP, "uTrailBox"), x0, y0, x1, y1);
        }
        GLES30.glBindVertexArray(empty);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
    }

    /** The sheet of buttons and signs into a texture, smooth, with its smaller sizes for the far ones. */
    private void badges() {
        int[] src = Badges.sheet();
        int side = Badges.CELLS * Badges.SIZE;
        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            int c = src[i];
            dst[i] = (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
        }
        IntBuffer b = ByteBuffer.allocateDirect(dst.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        b.put(dst).position(0);
        int[] t = new int[1];
        GLES30.glGenTextures(1, t, 0);
        badgesTex = t[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, badgesTex);
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, side, side, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, b);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    /** The town's people into buffers, and their picture into a texture; nothing if the game has none. */
    private void meshes(AssetManager assets) {
        meshFirst = null;
        FolkPack pack;
        android.graphics.Bitmap picture;
        try (InputStream in = assets.open("folk.bin")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int k;
            while ((k = in.read(buf)) > 0) out.write(buf, 0, k);
            pack = FolkPack.read(out.toByteArray());
        } catch (IOException e) {
            return;
        }
        try (InputStream in = assets.open("folk.png")) {
            picture = android.graphics.BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            return;
        }
        if (pack == null || picture == null) return;
        int corners = 0, indices = 0;
        for (int m = 0; m < pack.count; m++) {
            corners += pack.corners[m];
            indices += pack.indices[m];
        }
        if (corners > 65535) return;
        // a corner: place, normal, uv (8 floats), then its bones and weights (4 bytes each)
        ByteBuffer v = ByteBuffer.allocateDirect(corners * 40).order(ByteOrder.nativeOrder());
        ByteBuffer ix = ByteBuffer.allocateDirect(indices * 2).order(ByteOrder.nativeOrder());
        meshFirst = new int[pack.count];
        meshCount = new int[pack.count];
        int base = 0, at = 0;
        for (int m = 0; m < pack.count; m++) {
            float[] vd = pack.vertex[m];
            for (int c = 0; c < pack.corners[m]; c++) {
                for (int k = 0; k < 8; k++) v.putFloat(vd[c * 8 + k]);
                v.put(pack.bones[m], c * 4, 4);
                v.put(pack.weights[m], c * 4, 4);
            }
            meshFirst[m] = at;
            meshCount[m] = pack.indices[m];
            for (short i : pack.index[m]) ix.putShort((short) ((i & 0xFFFF) + base));
            base += pack.corners[m];
            at += pack.indices[m];
        }
        v.position(0);
        ix.position(0);
        int[] ids = new int[2];
        GLES30.glGenVertexArrays(1, ids, 0);
        meshVao = ids[0];
        GLES30.glGenBuffers(2, ids, 0);
        meshBuf = ids[0];
        meshIdx = ids[1];
        GLES30.glBindVertexArray(meshVao);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, meshBuf);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, corners * 40, v, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 40, 0);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 40, 12);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 40, 24);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_UNSIGNED_BYTE, false, 40, 32);
        GLES30.glEnableVertexAttribArray(4);
        GLES30.glVertexAttribPointer(4, 4, GLES30.GL_UNSIGNED_BYTE, true, 40, 36);
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, meshIdx);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices * 2, ix, GLES30.GL_STATIC_DRAW);
        GLES30.glBindVertexArray(0);
        int[] t = new int[1];
        GLES30.glGenTextures(1, t, 0);
        pictureTex = t[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, pictureTex);
        android.opengl.GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, picture, 0);
        picture.recycle();
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    /** The people of the list, one draw each, with program p (already in use and made ready). */
    private void people(int p, DrawList d) {
        if (meshFirst == null || d.folkN == 0) return;
        int locQ = u(p, "uBoneQ"), locT = u(p, "uBoneT"), locPlace = u(p, "uPlace"), locSize = u(p, "uSize"), locId = u(p, "uIdent");
        GLES30.glBindVertexArray(meshVao);
        float[] f = d.folk;
        for (int i = 0; i < d.folkN; i++) {
            int o = i * FolkPack.PERSON, m = (int) f[o];
            if (m < 0 || m >= meshFirst.length) continue;
            for (int b = 0; b < FolkPack.BONES; b++) {
                int k = o + 6 + b * 7;
                boneQ[b * 4] = f[k];
                boneQ[b * 4 + 1] = f[k + 1];
                boneQ[b * 4 + 2] = f[k + 2];
                boneQ[b * 4 + 3] = f[k + 3];
                boneT[b * 3] = f[k + 4];
                boneT[b * 3 + 1] = f[k + 5];
                boneT[b * 3 + 2] = f[k + 6];
            }
            GLES30.glUniform4fv(locQ, FolkPack.BONES, boneQ, 0);
            GLES30.glUniform3fv(locT, FolkPack.BONES, boneT, 0);
            GLES30.glUniform4f(locPlace, f[o + 1], f[o + 2], f[o + 3], f[o + 4]);
            GLES30.glUniform1f(locSize, f[o + 5]);
            if (locId >= 0) GLES30.glUniform1f(locId, (1f + ((i * 37 + m * 11) % 254)) / 255f);
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, meshCount[m], GLES30.GL_UNSIGNED_SHORT, meshFirst[m] * 2);
        }
        GLES30.glBindVertexArray(0);
    }

    /** A program that draws lit boxes, made ready: the eye, the sun and its depth map, the haze. */
    private void lit(int p, DrawList d) {
        GLES30.glUseProgram(p);
        GLES30.glUniformMatrix4fv(u(p, "uViewProj"), 1, false, d.viewProj, 0);
        GLES30.glUniformMatrix4fv(u(p, "uLight"), 1, false, d.lightProj, 0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, shadowTex);
        GLES30.glUniform1i(u(p, "uShadow"), 0);
        GLES30.glUniform1f(u(p, "uShadowTexel"), 1f / SHADOW_SIZE);
        GLES30.glUniform3fv(u(p, "uSun"), 1, d.sun, 0);
        GLES30.glUniform3fv(u(p, "uSunColor"), 1, d.sunColor, 0);
        GLES30.glUniform3fv(u(p, "uAmbSky"), 1, d.ambientSky, 0);
        GLES30.glUniform3fv(u(p, "uAmbGround"), 1, d.ambientGround, 0);
        GLES30.glUniform3fv(u(p, "uEye"), 1, d.eye, 0);
        GLES30.glUniform3fv(u(p, "uFog"), 1, d.fog, 0);
        GLES30.glUniform1f(u(p, "uFogStart"), d.fogStart);
        GLES30.glUniform1f(u(p, "uFogScale"), d.fogScale);
        GLES30.glUniform1f(u(p, "uTime"), d.time);
        GLES30.glUniform1f(u(p, "uNight"), d.night);
        GLES30.glUniform4fv(u(p, "uFire"), 1, d.fire, 0);
        GLES30.glUniform4fv(u(p, "uSpot"), 1, d.spot, 0);
        GLES30.glUniform1f(u(p, "uDark"), d.spot[4]);
    }

    // ---- helpers ----

    /** The same shader with a word defined after its first line. */
    private static String define(String src, String word) {
        int eol = src.indexOf('\n');
        return src.substring(0, eol + 1) + "#define " + word + "\n" + src.substring(eol + 1);
    }

    /** The same shader, built for balls. */
    private static String sphere(String src) {
        int eol = src.indexOf('\n');
        return src.substring(0, eol + 1) + "#define SPHERE\n" + src.substring(eol + 1);
    }

    private static int u(int program, String name) {
        return GLES30.glGetUniformLocation(program, name);
    }

    private static void params(int filter) {
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, filter);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    }

    private static FloatBuffer floats(int n) {
        return ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    private static String read(AssetManager assets, String name) {
        try (InputStream in = assets.open("shaders/" + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int k;
            while ((k = in.read(buf)) > 0) out.write(buf, 0, k);
            return out.toString("UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("shader " + name + " missing", e);
        }
    }

    private static int program(String vs, String fs) {
        int p = GLES30.glCreateProgram();
        GLES30.glAttachShader(p, shader(GLES30.GL_VERTEX_SHADER, vs));
        GLES30.glAttachShader(p, shader(GLES30.GL_FRAGMENT_SHADER, fs));
        GLES30.glLinkProgram(p);
        int[] ok = new int[1];
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new RuntimeException("link: " + GLES30.glGetProgramInfoLog(p));
        return p;
    }

    private static int shader(int type, String src) {
        int s = GLES30.glCreateShader(type);
        GLES30.glShaderSource(s, src);
        GLES30.glCompileShader(s);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) throw new RuntimeException("compile: " + GLES30.glGetShaderInfoLog(s));
        return s;
    }

    /** The unit cube, -1..1: six faces, two triangles each; position, then normal. */
    private static float[] cube() {
        float[][][] faces = {
                {{1, 0, 0}, {1, -1, -1}, {1, 1, -1}, {1, 1, 1}, {1, -1, 1}},
                {{-1, 0, 0}, {-1, -1, 1}, {-1, 1, 1}, {-1, 1, -1}, {-1, -1, -1}},
                {{0, 1, 0}, {-1, 1, -1}, {-1, 1, 1}, {1, 1, 1}, {1, 1, -1}},
                {{0, -1, 0}, {-1, -1, 1}, {-1, -1, -1}, {1, -1, -1}, {1, -1, 1}},
                {{0, 0, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}, {-1, -1, 1}},
                {{0, 0, -1}, {-1, -1, -1}, {-1, 1, -1}, {1, 1, -1}, {1, -1, -1}}};
        int[] order = {1, 2, 3, 1, 3, 4};
        float[] out = new float[6 * 6 * 6];
        int o = 0;
        for (float[][] f : faces)
            for (int i : order) {
                out[o++] = f[i][0];
                out[o++] = f[i][1];
                out[o++] = f[i][2];
                out[o++] = f[0][0];
                out[o++] = f[0][1];
                out[o++] = f[0][2];
            }
        return out;
    }
}
