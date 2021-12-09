package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import java.io.*;
import java.util.*;

import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.utils.*;
import org.lwjgl.glfw.*;

import static net.kdt.pojavlaunch.utils.MathUtils.map;

import com.kdt.LoggerView;

public class JavaGUILauncherActivity extends  BaseActivity implements View.OnTouchListener {
    private static final int MSG_LEFT_MOUSE_BUTTON_CHECK = 1028;
    
    private AWTCanvasView mTextureView;

    String specialChars = "/*!@#$%^&*()\"{}_[+=-_]\'|\\?/<>,.";
    private LoggerView loggerView;
    private boolean mouseState = false;
    private int mode = 0;

    private LinearLayout touchPad;
    private ImageView mousePointer;
    private GestureDetector gestureDetector;
    private long lastPress = 0;
    ScaleGestureDetector scaleGestureDetector;
    SimpleGestureListener longTapGestureDetector;

    private boolean rcState = false;

    private boolean mSkipDetectMod, isVirtualMouseEnabled;

    private int scaleFactor;
    private int[] scaleFactors = initScaleFactors();

    private final int fingerStillThreshold = 8;
    private int initialX;
    private int initialY;
    @SuppressLint("HandlerLeak")
    private Handler theHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LEFT_MOUSE_BUTTON_CHECK: {
                        float x = CallbackBridge.mouseX;
                        float y = CallbackBridge.mouseY;
                        if (CallbackBridge.isGrabbing() &&
                            Math.abs(initialX - x) < fingerStillThreshold &&
                            Math.abs(initialY - y) < fingerStillThreshold) {
                            boolean triggeredLeftMouseButton = true;
                            AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK, true);
                        }
                    } break;
            }
        }
    };

    public class MyOnScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            switch(mode){
                case 0: // Pinch to Zoom
                    if (scaleFactor > 1) {
                        //Send F4 To Zoom Out
                        AWTInputBridge.sendKey((char)115,115);
                    } else {
                        //116
                        AWTInputBridge.sendKey((char)116,116);
                    }
                    break;
                case 1: // Right click
                    AWTInputBridge.sendKey((char)122,122);
                    AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                    break;


            }

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }

    class SimpleGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent event) {
            //...
            return super.onDown(event);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            //...
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            System.out.println("We got a long tap!");
            super.onLongPress(event);
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        setContentView(R.layout.install_mod);
        Tools.updateWindowSize(this);
        Logger.getInstance().reset();

        scaleGestureDetector = new ScaleGestureDetector(this, new MyOnScaleGestureListener());
        longTapGestureDetector = new SimpleGestureListener();

        try {
            loggerView = findViewById(R.id.launcherLoggerView);
            MultiRTUtils.setRuntimeNamed(this,LauncherPreferences.PREF_DEFAULT_RUNTIME);
            gestureDetector = new GestureDetector(this, new SingleTapConfirm());

            findViewById(R.id.installmod_mouse_pri).setOnTouchListener(this);
            findViewById(R.id.installmod_mouse_sec).setOnTouchListener(this);

            findViewById(R.id.camera).setOnTouchListener(this);

            this.touchPad = findViewById(R.id.main_touchpad);
            touchPad.setFocusable(false);
            touchPad.setVisibility(View.GONE);

            this.mousePointer = findViewById(R.id.main_mouse_pointer);
            this.mousePointer.post(() -> {
                ViewGroup.LayoutParams params = mousePointer.getLayoutParams();
                params.width = (int) (36 / 100f * LauncherPreferences.PREF_MOUSESCALE);
                params.height = (int) (54 / 100f * LauncherPreferences.PREF_MOUSESCALE);
            });

            touchPad.setOnTouchListener(new OnTouchListener(){
                    private float prevX, prevY;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        mode = 1;
                       //longTapGestureDetector.onLongPress(event);
                        // MotionEvent reports input details from the touch screen
                        // and other input controls. In this case, you are only
                        // interested in events where the touch position changed.
                        // int index = event.getActionIndex();

                        //System.out.println("sending pos: "+prevX+","+prevY);
                        //sendScaledMousePosition(prevX,prevY);
                        //AWTInputBridge.sendMousePress(AWTInputEvent.NOBUTTON);

                        int action = event.getActionMasked();

                        float x = event.getX();
                        float y = event.getY();
                        if(event.getHistorySize() > 0) {
                            prevX = event.getHistoricalX(0);
                            prevY = event.getHistoricalY(0);
                        }else{
                            prevX = x;
                            prevY = y;
                        }
                        float mouseX = mousePointer.getX();
                        float mouseY = mousePointer.getY();
                        if (gestureDetector.onTouchEvent(event)) {
                            sendScaledMousePosition(mouseX,mouseY);
                            AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                            clearRC();
                        } else {
                            switch (action) {
                                case MotionEvent.ACTION_UP: // 1
                                case MotionEvent.ACTION_CANCEL: // 3
                                case MotionEvent.ACTION_POINTER_UP: // 6
                                    break;
                                case MotionEvent.ACTION_MOVE: // 2
                                    mouseX = Math.max(0, Math.min(CallbackBridge.physicalWidth, mouseX + x - prevX));
                                    mouseY = Math.max(0, Math.min(CallbackBridge.physicalHeight, mouseY + y - prevY));
                                    placeMouseAt(mouseX, mouseY);

                                    sendScaledMousePosition(mouseX,mouseY);
                                    /*
                                     if (!CallbackBridge.isGrabbing()) {
                                     CallbackBridge.sendMouseKeycode(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_LEFT, 0, isLeftMouseDown);
                                     CallbackBridge.sendMouseKeycode(LWJGLGLFWKeycode.GLFW_MOUSE_BUTTON_RIGHT, 0, isRightMouseDown);
                                     }
                                     */
                                    break;
                            }
                        }

                        scaleGestureDetector.onTouchEvent(event);

                        // debugText.setText(CallbackBridge.DEBUG_STRING.toString());
                        CallbackBridge.DEBUG_STRING.setLength(0);

                        return true;
                    }
                });
                
            placeMouseAt(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2);

            // this.textLogBehindGL = (TextView) findViewById(R.id.main_log_behind_GL);
            // this.textLogBehindGL.setTypeface(Typeface.MONOSPACE);


            final File modFile = (File) getIntent().getExtras().getSerializable("modFile");
            final String javaArgs = getIntent().getExtras().getString("javaArgs");

            mTextureView = findViewById(R.id.installmod_surfaceview);
            mTextureView.setOnTouchListener((v, event) -> {
                mode = 0;
                if(scaleGestureDetector.onTouchEvent(event)){
                }
                float x = event.getX();
                float y = event.getY();
                if (gestureDetector.onTouchEvent(event)) {
                    sendScaledMousePosition(x, y);
                    AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                    return true;
                }

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_UP: // 1
                    case MotionEvent.ACTION_CANCEL: // 3
                    case MotionEvent.ACTION_POINTER_UP: // 6
                        break;
                    case MotionEvent.ACTION_MOVE: // 2
                        sendScaledMousePosition(x, y);
                        break;
                }
                return true;
            });
           
            mSkipDetectMod = getIntent().getExtras().getBoolean("skipDetectMod", false);
            if (mSkipDetectMod) {
                new Thread(() -> launchJavaRuntime(modFile, javaArgs), "JREMainThread").start();
                return;
            }
            // No skip detection
            //openLogOutput(null);
            new Thread(() -> {
                try {
                    final int exit = doCustomInstall(modFile, javaArgs);
                    Logger.getInstance().appendToLog(getString(R.string.toast_optifine_success));
                    if (exit != 0) return;
                    runOnUiThread(() -> {
                        Toast.makeText(JavaGUILauncherActivity.this, R.string.toast_optifine_success, Toast.LENGTH_SHORT).show();
                        MainActivity.fullyExit();
                    });

                } catch (Throwable e) {
                    Logger.getInstance().appendToLog("Install failed:");
                    Logger.getInstance().appendToLog(Log.getStackTraceString(e));
                    Tools.showError(JavaGUILauncherActivity.this, e);
                }
            }, "Installer").start();
        } catch (Throwable th) {
            Tools.showError(this, th, true);
        }
        //scaleUp(mTextureView);
    }

    private void clearRC(){
        rcState = false;
        AWTInputBridge.sendKey((char)121,121);
        findViewById(R.id.installmod_mouse_sec).setBackground(getResources().getDrawable( R.drawable.control_button ));
    }

    private void activateRC(){
        rcState = true;
        AWTInputBridge.sendKey((char)122,122);
        findViewById(R.id.installmod_mouse_sec).setBackground(getResources().getDrawable( R.drawable.control_button_pressed ));
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        long time = System.currentTimeMillis();
        if (time > lastPress + 500) {
            switch (v.getId()) {
                case R.id.installmod_mouse_pri:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                    break;
                case R.id.installmod_mouse_sec:
                    if (!rcState) {
                        System.out.println("Sending F11");
                        activateRC();
                    } else {
                        System.out.println("Sending F10");
                        clearRC();
                    }
                    System.out.println("Time:" + time + " Last " + lastPress);
                    break;
                case R.id.camera:
                    // Camera Mode On
                    if(!mouseState){
                        AWTInputBridge.sendKey((char)120,120);
                        v.setBackground(getResources().getDrawable( R.drawable.control_button_pressed ));
                        mouseState = true;
                    }
                    else{
                        AWTInputBridge.sendKey((char)119,119);
                        v.setBackground(getResources().getDrawable( R.drawable.control_button_normal ));
                        mouseState = false;
                    }
                    break;
            }
            lastPress = time;
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            //Log.i("key getKeycode", String.valueOf(event.getKeyCode()));
            //Log.i("key unicode", String.valueOf((char)event.getUnicodeChar()));
            //Log.i("key unicode int", String.valueOf(event.getUnicodeChar()));

            if(event.getKeyCode() == 67){
                // Backspace
                AWTInputBridge.sendKey((char)0x08,0x08);
            } else if(specialChars.contains(""+(char)event.getUnicodeChar())){
                // Send special character to client
                char c = (char)event.getUnicodeChar();
                switch((char)event.getUnicodeChar()){
                    case '!':
                        c = '1';
                        break;
                    case '@':
                        c = '2';
                        break;
                    case '#':
                        c = '3';
                        break;
                    case '$':
                        c = '4';
                        break;
                    case '%':
                        c = '5';
                        break;
                    case '^':
                        c = '6';
                        break;
                    case '&':
                        c = '7';
                        break;
                    case '*':
                        c = '8';
                        break;
                    case '(':
                        c = '9';
                        break;
                    case ')':
                        c = '0';
                        break;
                    case '_':
                        c = '-';
                        break;
                    case '+':
                        c = '=';
                        break;
                    case '{':
                        c = '[';
                        break;
                    case '}':
                        c = ']';
                        break;
                    case ':':
                        c = ';';
                        break;
                    case '"':
                        c = '\'';
                        break;
                    case '<':
                        c = ',';
                        break;
                    case '>':
                        c = '.';
                        break;
                    case '?':
                        c = '/';
                        break;
                    case '|':
                        c = '\\';
                        break;
                }
                System.out.println("I SEE A "+(char)event.getUnicodeChar());
                if(c != (char)event.getUnicodeChar()){
                    System.out.println("REPLACED with "+(char)c);
                    AWTInputBridge.sendKey((char)123,123);
                }
                AWTInputBridge.sendKey((char)c,c);

            } else if(Character.isDigit((char)event.getUnicodeChar())){
                System.out.println("I SEE A "+(char)event.getUnicodeChar());
                AWTInputBridge.sendKey((char)event.getUnicodeChar(),(char)event.getUnicodeChar());
            } else if ((char)event.getUnicodeChar() == Character.toUpperCase((char)event.getUnicodeChar())){
                // We send a '`' (keycode) 192 to avoid needing to worry about shift. The RS client takes this modifier
                // and does a toUpperCase(). Special character mapping will also need to be provided.
                AWTInputBridge.sendKey((char)123,123);
                AWTInputBridge.sendKey((char)Character.toUpperCase(event.getUnicodeChar()),(char)Character.toUpperCase(event.getUnicodeChar()));

                // Send shift key.. only problem is then you're stuck shifted.
                // AWTInputBridge.sendKey((char)0x10,(char)Character.toUpperCase(event.getUnicodeChar()),0,0x10);
            } else if((char)event.getUnicodeChar() == Character.toLowerCase((char)event.getUnicodeChar())){
                AWTInputBridge.sendKey((char)Character.toUpperCase(event.getUnicodeChar()),(char)Character.toUpperCase(event.getUnicodeChar()));
            } else {
                AWTInputBridge.sendKey((char)event.getUnicodeChar(),event.getUnicodeChar());
            }
        }
        return true;
    }

    public void placeMouseAdd(float x, float y) {
        this.mousePointer.setX(mousePointer.getX() + x);
        this.mousePointer.setY(mousePointer.getY() + y);
    }

    public void placeMouseAt(float x, float y) {
        this.mousePointer.setX(x);
        this.mousePointer.setY(y);
    }

    void sendScaledMousePosition(float x, float y){
        AWTInputBridge.sendMousePos((int) map(x,0,CallbackBridge.physicalWidth, scaleFactors[0], scaleFactors[2]),
                (int) map(y,0,CallbackBridge.physicalHeight, scaleFactors[1], scaleFactors[3]));
    }

    public void forceClose(View v) {
        BaseMainActivity.dialogForceClose(this);
    }

    public void openLogOutput(View v) {
        loggerView.setVisibility(View.VISIBLE);
    }

    public void closeLogOutput(View view) {
        if (mSkipDetectMod) {
            loggerView.setVisibility(View.GONE);
        } else {
            forceClose(null);
        }
    }

    public void toggleVirtualMouse(View v) {
        isVirtualMouseEnabled = !isVirtualMouseEnabled;
        touchPad.setVisibility(isVirtualMouseEnabled ? View.VISIBLE : View.GONE);
        Toast.makeText(this,
                isVirtualMouseEnabled ? R.string.control_mouseon : R.string.control_mouseoff,
                Toast.LENGTH_SHORT).show();
    }
    
    private int doCustomInstall(File modFile, String javaArgs) throws IOException {
        mSkipDetectMod = true;
        return launchJavaRuntime(modFile, javaArgs);
    }

    public int launchJavaRuntime(File modFile, String javaArgs) {
        JREUtils.redirectAndPrintJRELog(this);
        try {
            JREUtils.jreReleaseList = JREUtils.readJREReleaseProperties();
            
            // Fail immediately when Java 8 is not selected
            // TODO: auto override Java 8 if installed
            if (!JREUtils.jreReleaseList.get("JAVA_VERSION").equals("1.8.0")) {
                throw new RuntimeException("Cannot use the mod installer. In order to use the mod installer, you need to install Java 8 and specify it in the Preferences menu.");
            }
            
            List<String> javaArgList = new ArrayList<String>();

            // Enable Caciocavallo
            Tools.getCacioJavaArgs(javaArgList,false);
            
            if (javaArgs != null) {
                javaArgList.addAll(Arrays.asList(javaArgs.split(" ")));
            } else {
                javaArgList.add("-jar");
                javaArgList.add(modFile.getAbsolutePath());
            }

            Logger.getInstance().appendToLog("Info: Java arguments: " + Arrays.toString(javaArgList.toArray(new String[0])));
            
            // Run java on sandbox, non-overrideable.
            Collections.reverse(javaArgList);
            javaArgList.add("-Xbootclasspath/a:" + Tools.DIR_DATA + "/pro-grade.jar");
            javaArgList.add("-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM");
            javaArgList.add("-Djava.security.policy=" + Tools.DIR_DATA + "/java_sandbox.policy");
            Collections.reverse(javaArgList);

            return JREUtils.launchJavaVM(this, javaArgList);
        } catch (Throwable th) {
            Tools.showError(this, th, true);
            return -1;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    int[] initScaleFactors(){
        return initScaleFactors(true);
    }

    int[] initScaleFactors(boolean autoScale){
        //Could be optimized

        if(autoScale) { //Auto scale
            int minDimension = Math.min(CallbackBridge.physicalHeight, CallbackBridge.physicalWidth);
            scaleFactor = Math.max(((3 * minDimension) / 1080) - 1, 1);
        }

        int[] scales = new int[4]; //Left, Top, Right, Bottom

        scales[0] = (CallbackBridge.physicalWidth/2);
        scales[0] -= scales[0]/scaleFactor;

        scales[1] = (CallbackBridge.physicalHeight/2);
        scales[1] -= scales[1]/scaleFactor;

        scales[2] = (CallbackBridge.physicalWidth/2);
        scales[2] += scales[2]/scaleFactor;

        scales[3] = (CallbackBridge.physicalHeight/2);
        scales[3] += scales[3]/scaleFactor;

        return scales;
    }

    public void scaleDown(View view) {
        scaleFactor = Math.max(scaleFactor - 1, 1);
        scaleFactors = initScaleFactors(false);
        mTextureView.initScaleFactors(scaleFactor);
        sendScaledMousePosition(mousePointer.getX(),mousePointer.getY());
    }

    public void scaleUp(View view) {
        scaleFactor = Math.min(scaleFactor + 1, 6);
        scaleFactors = initScaleFactors(false);
        mTextureView.initScaleFactors(scaleFactor);
        sendScaledMousePosition(mousePointer.getX(),mousePointer.getY());
    }
}
