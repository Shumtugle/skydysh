package com.shumtugle.skydysh;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * The surface: collects touches on the UI thread and hands them to the world on the GL
 * thread, which advances the world, has it fill the draw list and draws that, frame after
 * frame. The world never sees a GL call.
 */
final class Stage extends GLSurfaceView implements GLSurfaceView.Renderer {
    private final World world;
    private final DrawList list = new DrawList();
    private final Gfx gfx = new Gfx();
    private int screenW = 1, screenH = 1;
    private long last;
    // how many drawn pixels make a frame pixel: starts at two, steps down while frames come
    // slow, back up while they come quick, so the phone draws as finely as it keeps up with
    private float q = 2f;
    private float slowFor, quickFor;

    // touches waiting for the GL thread: kind, which finger, x, y in frame pixels
    private static final int Q = 4;
    private final float[] pending = new float[Q * 128];
    private int pendingCount;

    Stage(Context context, World.Host host) {
        super(context);
        world = new World(host);
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Every finger on its own: one coming down or going up is heard as such, by its own number;
     * a move is heard for every finger that is down.
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float k = World.W / (float) Math.max(1, getWidth());
        int act = e.getActionMasked();
        synchronized (pending) {
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN: {
                    int i = e.getActionIndex();
                    queue(World.DOWN, e.getPointerId(i), e.getX(i) * k, e.getY(i) * k);
                    break;
                }
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < e.getPointerCount(); i++) queue(World.MOVE, e.getPointerId(i), e.getX(i) * k, e.getY(i) * k);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP: {
                    int i = e.getActionIndex();
                    queue(World.UP, e.getPointerId(i), e.getX(i) * k, e.getY(i) * k);
                    break;
                }
                case MotionEvent.ACTION_CANCEL:
                    for (int i = 0; i < e.getPointerCount(); i++) queue(World.UP, e.getPointerId(i), e.getX(i) * k, e.getY(i) * k);
                    break;
                default:
            }
        }
        return true;
    }

    /** Left (another app over it, the screen off): the game is told at once when it comes back. */
    @Override
    public void onPause() {
        queue(World.LEFT, 0, 0f, 0f);
        super.onPause();
    }

    private void queue(int kind, int id, float x, float y) {
        if (pendingCount >= pending.length / Q) return;
        int o = pendingCount * Q;
        pending[o] = kind;
        pending[o + 1] = id;
        pending[o + 2] = x;
        pending[o + 3] = y;
        pendingCount++;
    }

    /** The back key: true when the world takes it (nothing here takes it now), false to leave the game. */
    boolean back() {
        if (!world.wantsBack()) return false;
        synchronized (pending) {
            queue(World.BACK, 0, 0f, 0f);
        }
        return true;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        gfx.create(getContext().getAssets());
        last = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        screenW = w;
        screenH = h;
        world.resize(w, h);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        long now = System.nanoTime();
        float dt = last == 0 ? 1f / 60f : Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        synchronized (pending) {
            for (int i = 0; i < pendingCount; i++)
                world.touch((int) pending[i * Q], (int) pending[i * Q + 1], pending[i * Q + 2], pending[i * Q + 3]);
            pendingCount = 0;
        }
        // the world goes on in steps no longer than a sixtieth of a second, however slow the frame
        int steps = (int) Math.ceil(dt * 60f - 1e-3f);
        for (int s = 0; s < Math.max(1, steps); s++) world.update(dt / Math.max(1, steps));
        world.drawScale = q;
        world.draw(list);
        gfx.draw(list, screenW, screenH, q);
        // how long a frame takes, frame to frame, shown in the corner while the engine is young
        world.frameMs = world.frameMs * 0.95f + dt * 1000f * 0.05f;
        slowFor = world.frameMs > 20f ? slowFor + dt : 0f;
        quickFor = world.frameMs < 13f ? quickFor + dt : 0f;
        if (slowFor > 1f && q > 1.25f) {
            q = Math.max(1.25f, q - 0.25f);
            slowFor = 0f;
        } else if (quickFor > 4f && q < 3f) {
            q = Math.min(3f, q + 0.25f);
            quickFor = 0f;
        }
    }
}
