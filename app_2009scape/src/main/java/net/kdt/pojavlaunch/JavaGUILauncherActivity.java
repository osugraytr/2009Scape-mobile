package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.os.Vibrator;
import android.util.Log;
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

import androidx.preference.PreferenceManager;

import com.kdt.LoggerView;

public class JavaGUILauncherActivity extends  BaseActivity implements View.OnTouchListener {
    private static final int MSG_LEFT_MOUSE_BUTTON_CHECK = 1028;
    
    private AWTCanvasView mTextureView;
    private int totalMovement;

    String specialChars = "/*!@#$%^&*()\"{}_[+:;=-_]'|\\?/<>,.";
    private LoggerView loggerView;
    private boolean mouseState = false;

    private LinearLayout touchPad;
    private ImageView mousePointer;
    private GestureDetector gestureDetector;
    private long lastPress = 0;
    ScaleGestureDetector scaleGestureDetector;
    GestureDetector longTapGestureDetector;
    private long touchStart = 0;
    boolean longPressTriggered = false;
    boolean longPressShouldClick = false;

    private boolean rcState = false;

    private boolean isVirtualMouseEnabled;

    private int scaleFactor;
    public float[] scaleFactors = initScaleFactors();

    private final int fingerStillThreshold = 8;
    private int initialX;
    private int initialY;

    public class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            if (scaleFactor > 1) { //Send F4 To Zoom Out
                AWTInputBridge.sendKey((char)115,115);
            } else { //116 F5 To Zoom In
                AWTInputBridge.sendKey((char) 116, 116);
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
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.install_mod);
        Tools.updateWindowSize(this);
        Vibrator vb = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        try {
            MultiRTUtils.setRuntimeNamed(this,LauncherPreferences.PREF_DEFAULT_RUNTIME);
            gestureDetector = new GestureDetector(this, new SingleTapConfirm());
            scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());

            findViewById(R.id.keyboard).setOnTouchListener(this);
            findViewById(R.id.camera).setOnTouchListener(this);
            findViewById(R.id.mb2).setOnTouchListener(this);

            // Righthanded Flip UI
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            if(Boolean.parseBoolean(preferences.getString("righthanded",""))){
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) findViewById(R.id.keyboard).getLayoutParams();
                lp.addRule(RelativeLayout.ALIGN_PARENT_END);
                RelativeLayout.LayoutParams lp1 = (RelativeLayout.LayoutParams) findViewById(R.id.mb2).getLayoutParams();
                lp1.addRule(RelativeLayout.LEFT_OF,R.id.keyboard);
                lp1.addRule(RelativeLayout.RIGHT_OF); // Clear right of prop
                RelativeLayout.LayoutParams lp2 = (RelativeLayout.LayoutParams) findViewById(R.id.main_toggle_mouse).getLayoutParams();
                lp2.addRule(RelativeLayout.LEFT_OF,R.id.keyboard);
                RelativeLayout.LayoutParams lp3 = (RelativeLayout.LayoutParams) findViewById(R.id.installmod_scale_down).getLayoutParams();
                lp3.addRule(RelativeLayout.LEFT_OF,R.id.keyboard);
                RelativeLayout.LayoutParams lp4 = (RelativeLayout.LayoutParams) findViewById(R.id.menu).getLayoutParams();
                lp4.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            } else {
                // Just a hack to get the settings icon to align to right by default.
                RelativeLayout.LayoutParams lp4 = (RelativeLayout.LayoutParams) findViewById(R.id.menu).getLayoutParams();
                lp4.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            }


            // Launch Settings Popup
            findViewById(R.id.menu).setOnTouchListener((view, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    startActivity(new Intent(JavaGUILauncherActivity.this, SettingsMenu.class));
                }
                return false;
            });

            this.touchPad = findViewById(R.id.main_touchpad);
            touchPad.setVisibility(View.VISIBLE);

            this.mousePointer = findViewById(R.id.main_mouse_pointer);
            this.mousePointer.post(() -> {
                ViewGroup.LayoutParams params = mousePointer.getLayoutParams();
                params.width = (int) (36 / 100f * LauncherPreferences.PREF_MOUSESCALE);
                params.height = (int) (54 / 100f * LauncherPreferences.PREF_MOUSESCALE);
            });

            // Just to get the scaled mouse on startup.
            ViewGroup.LayoutParams params = mousePointer.getLayoutParams();
            params.width = (int) (36 / 100f * LauncherPreferences.PREF_MOUSESCALE);
            params.height = (int) (54 / 100f * LauncherPreferences.PREF_MOUSESCALE);

            touchPad.setOnTouchListener(new OnTouchListener(){
                    private float prevX, prevY;
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getActionMasked();

                        if(action == 0){
                            // Reset checks because this is a new tap
                            longPressTriggered = false;
                            totalMovement = 0;
                            touchStart = System.currentTimeMillis();
                        }

                        float x = event.getX();
                        float y = event.getY();
                        float mouseX = mousePointer.getX();
                        float mouseY = mousePointer.getY();

                        if(event.getHistorySize() > 0) {
                            prevX = event.getHistoricalX(0);
                            prevY = event.getHistoricalY(0);
                        }else{
                            prevX = x;
                            prevY = y;
                        }

                        // Long press
                        totalMovement += Math.abs(x - prevX) + Math.abs(y - prevX);
                        if(!longPressTriggered &&
                            System.currentTimeMillis() - touchStart > 1500 &&
                            totalMovement < 8000
                        ){
                            longPressTriggered = true;
                            AWTInputBridge.sendKey((char)118,118);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vb.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                //deprecated in API 26
                                vb.vibrate(100);
                            }
                            return true;
                        }

                        if (gestureDetector.onTouchEvent(event)) {
                            sendScaledMousePosition(mouseX,mouseY);
                            AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                            clearRC();
                        } else {
                            switch (action) {
                                case MotionEvent.ACTION_POINTER_DOWN: //Second finger right clicking
                                    AWTInputBridge.sendKey((char)122,122);
                                    AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                                case MotionEvent.ACTION_UP: // 1
                                    if(longPressTriggered){
                                        // Currently a click is needed to end the drag. MB1
                                        // is not good for banking so MB2 sending instead. Annoying but
                                        // better than the alternative. other buttons 'MB3' ect same outcome
                                        // probably should null the click on client if isHeld is true.
                                        AWTInputBridge.sendKey((char)122,122);
                                        AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK);
                                    }
                                    break;
                                case MotionEvent.ACTION_CANCEL: // 3
                                case MotionEvent.ACTION_POINTER_UP: // 6
                                    break;
                                case MotionEvent.ACTION_MOVE: // 2
                                    mouseX = Math.max(0, Math.min(CallbackBridge.physicalWidth, mouseX + x - prevX));
                                    mouseY = Math.max(0, Math.min(CallbackBridge.physicalHeight, mouseY + y - prevY));
                                    placeMouseAt(mouseX, mouseY);
                                    sendScaledMousePosition(mouseX,mouseY);
                                    break;
                            }
                        }
                        // debugText.setText(CallbackBridge.DEBUG_STRING.toString());
                        //CallbackBridge.DEBUG_STRING.setLength(0);
                        return true;
                    }
                });
                
            placeMouseAt(CallbackBridge.physicalWidth / 2, CallbackBridge.physicalHeight / 2);

            final File miniclient = (File) getIntent().getExtras().getSerializable("miniclient");
            final File config = new File(getFilesDir(), "config.json");
            final String javaArgs = getIntent().getExtras().getString("javaArgs");

            mTextureView = findViewById(R.id.installmod_surfaceview);
            mTextureView.setOnTouchListener((v, event) -> {
                scaleGestureDetector.onTouchEvent(event);
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
            new Thread(() -> {
                try {
                    launchJavaRuntime(miniclient, javaArgs,config);
                } catch (Throwable e) {
                    Tools.showError(JavaGUILauncherActivity.this, e);
                }
            }, "2009Scape").start();
        } catch (Throwable th) {
            Tools.showError(this, th, true);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent e) {
        long time = System.currentTimeMillis();
        if (time > lastPress + 500) {
            switch (v.getId()) {
                case R.id.keyboard:
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                    break;
                case R.id.mb2:
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
                    if(!mouseState){ // Camera Mode On
                        AWTInputBridge.sendKey((char)120,120);
                        v.setBackground(getResources().getDrawable( R.drawable.control_button_pressed ));
                        mouseState = true;
                    }
                    else{ // Camera Mode off
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
            if(event.getKeyCode() == 67){
                // Backspace
                AWTInputBridge.sendKey((char)0x08,0x08);
            } else if(specialChars.contains(""+(char)event.getUnicodeChar())){
                // Send special character to client
                char c = (char)event.getUnicodeChar();
                switch(c){
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
                //System.out.println("I SEE A "+(char)event.getUnicodeChar());
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

    private void clearRC(){
        rcState = false;
        AWTInputBridge.sendKey((char)121,121);
        findViewById(R.id.mb2).setBackground(getResources().getDrawable( R.drawable.control_button ));
    }

    private void activateRC(){
        rcState = true;
        AWTInputBridge.sendKey((char)122,122);
        findViewById(R.id.mb2).setBackground(getResources().getDrawable( R.drawable.control_button_pressed ));
    }

    public void placeMouseAt(float x, float y) {
        this.mousePointer.setX(x);
        this.mousePointer.setY(y);
    }

    void sendScaledMousePosition(float x, float y){
        AWTInputBridge.sendMousePos((int) map(x,0,CallbackBridge.physicalWidth, scaleFactors[0], scaleFactors[2]),
                (int) map(y,0,CallbackBridge.physicalHeight, scaleFactors[1], scaleFactors[3]));
    }

    public void toggleVirtualMouse(View v) {
        isVirtualMouseEnabled = !isVirtualMouseEnabled;
        touchPad.setVisibility(isVirtualMouseEnabled ? View.GONE : View.VISIBLE);
        ImageView view = findViewById(R.id.mouse_button_img);
        if(isVirtualMouseEnabled){
            view.setImageResource(R.drawable.touch);
        } else{
            view.setImageResource(R.drawable.ic_mouse3);
        }
        touchPad.setVisibility(isVirtualMouseEnabled ? View.GONE : View.VISIBLE);
    }

    public int launchJavaRuntime(File miniclient, String javaArgs,File config) {
        JREUtils.redirectAndPrintJRELog(this);

        // Load saved username and password
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String name = preferences.getString("username","");
        String pass= preferences.getString("password","");

        try {
            JREUtils.jreReleaseList = JREUtils.readJREReleaseProperties();
            
            List<String> javaArgList = new ArrayList<String>();

            // Enable Caciocavallo
            Tools.getCacioJavaArgs(javaArgList,false);
            
            if (javaArgs != null) {
                javaArgList.addAll(Arrays.asList(javaArgs.split(" ")));
            } else {
                javaArgList.add("-jar");
                javaArgList.add(miniclient.getAbsolutePath());
                javaArgList.add(config.getAbsolutePath()); // Pass client config as arg0
                javaArgList.add("username=miniclientparam:"+name);
                javaArgList.add("password=miniclientparam:"+pass);
            }

            Logger.getInstance().appendToLog("Info: Java arguments: " + Arrays.toString(javaArgList.toArray(new String[0])));
            Log.i("Info: Java arguments: ",Arrays.toString(javaArgList.toArray(new String[0])));
            
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

    float[] initScaleFactors(){
        return initScaleFactors(true);
    }

    float[] initScaleFactors(boolean autoScale){
        //Could be optimized

        if(autoScale) { //Auto scale
            int minDimension = Math.min(CallbackBridge.physicalHeight, CallbackBridge.physicalWidth);
            scaleFactor = Math.max(((3 * minDimension) / 1080) - 1, 1);
        }

        float[] scales = new float[4]; //Left, Top, Right, Bottom

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
