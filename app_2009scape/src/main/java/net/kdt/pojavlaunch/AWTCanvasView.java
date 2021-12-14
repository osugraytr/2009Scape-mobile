package net.kdt.pojavlaunch;

import android.content.*;
import android.graphics.*;
import android.text.*;
import android.util.*;
import android.view.*;

import java.util.*;
import net.kdt.pojavlaunch.utils.*;
import org.lwjgl.glfw.*;

public class AWTCanvasView extends TextureView implements TextureView.SurfaceTextureListener, Runnable {
    private float mScaleFactor;
    private float[] mScales;
    public int offsetX = 0;
    public int offsetY = 0;
    private float[] stretchOffsets;
    private float[] stretchScales;

    private int mWidth, mHeight;
    private boolean mIsDestroyed = false;
    
    private TextPaint fpsPaint;
    private boolean attached = false;

    // Temporary count fps https://stackoverflow.com/a/13729241
    private LinkedList<Long> times = new LinkedList<Long>(){{add(System.nanoTime());}};
    private final int MAX_SIZE = 100;
    private final double NANOS = 1000000000.0;

    public int getmWidth(){
        return mWidth;
    }
    public int getmHeight(){
        return mHeight;
    }

    /** Calculates and returns frames per second */
    private double fps() {
        long lastTime = System.nanoTime();
        double difference = (lastTime - times.getFirst()) / NANOS;
        times.addLast(lastTime);
        int size = times.size();
        if (size > MAX_SIZE) {
            times.removeFirst();
        }
        return difference > 0 ? times.size() / difference : 0.0;
    }

    /** Computes the scale to better fit the screen */
    void initScaleFactors(){
        initScaleFactors(0);
    }

    void initScaleFactors(float forcedScale){
        //Could be optimized
        if(forcedScale < 1) { //Auto scale
            int minDimension = Math.min(CallbackBridge.physicalHeight, CallbackBridge.physicalWidth);
            mScaleFactor = 2;
        }else{
            mScaleFactor = forcedScale;
        }

        float[] scales = new float[2]; //Left, Top

        scales[0] = (CallbackBridge.physicalWidth/2);
        scales[0] -= scales[0]/mScaleFactor;

        scales[1] = (CallbackBridge.physicalHeight/2);
        scales[1] -= scales[1]/mScaleFactor;

        mScales = scales;
    }

    void initStretchedModes(){

        float[] offsets = new float[2];
        float[] scales = new float[4];
        // Full screen stretched
        offsets[0] -= mScales[0]*1.33F;
        offsets[1] -= mScales[1]*1.02F;

        // 16:9 Stretched



        // Fullscreen stretched
        scales[0] = (CallbackBridge.physicalWidth/2)/765;
        scales[1] = (CallbackBridge.physicalHeight/2)/503;

        // 16:9 stretched
        scales[2] = (CallbackBridge.physicalWidth/2)/894.22F;
        scales[3] = (CallbackBridge.physicalHeight/2)/503;

        stretchScales = scales;
        stretchOffsets = offsets;

    }
    
    public AWTCanvasView(Context ctx) {
        this(ctx, null);
    }
    
    public AWTCanvasView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        // setWillNotDraw(false);
        
        fpsPaint = new TextPaint();
        fpsPaint.setColor(Color.WHITE);
        fpsPaint.setTextSize(20);
        
        setSurfaceTextureListener(this);
        initScaleFactors();
        initStretchedModes();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int w, int h) {
        mWidth = w;
        mHeight = h;
        
        mIsDestroyed = false;
        new Thread(this, "AndroidAWTRenderer").start();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
        mIsDestroyed = true;
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture texture) {
    }
    
    private boolean mDrawing;
    private Surface mSurface;
    @Override
    public void run() {
        Canvas canvas;
        mSurface = new Surface(getSurfaceTexture());

        try {
            while (!mIsDestroyed && mSurface.isValid()) {
                canvas = mSurface.lockCanvas(null);
                canvas.drawRGB(0, 0, 0);

                if (!attached) {
                    attached = CallbackBridge.nativeAttachThreadToOther(true, BaseMainActivity.isInputStackCall);
                } else {
                    int[] rgbArray = JREUtils.renderAWTScreenFrame(/* canvas, mWidth, mHeight */);
                    mDrawing = rgbArray != null;
                    if (rgbArray != null) {


                        canvas.save();
                        // 16:9~ Scaled
                        //System.out.println("Factor: "+mScaleFactor);
                        //canvas.scale(mScaleFactor*1.17F, mScaleFactor);
                        //canvas.translate(-mScales[0]*1.17F,-mScales[1]);
                        //
                        canvas.scale(mScaleFactor, mScaleFactor);
                        canvas.translate(-mScales[0],-mScales[1]);
                        canvas.drawBitmap(rgbArray, 0, CallbackBridge.physicalWidth, offsetX, offsetY, CallbackBridge.physicalWidth, CallbackBridge.physicalHeight, true, null);
                        canvas.restore();

                    }
                    rgbArray = null;
                    // System.gc();
                }
                canvas.drawText("FPS: " + (Math.round(fps() * 10) / 10) + ", attached=" + attached + ", drawing=" + mDrawing, 50, 50, fpsPaint);
                mSurface.unlockCanvasAndPost(canvas);
            }
        } catch (Throwable th) {
            Tools.showError(getContext(), th);
        }
    }
}
