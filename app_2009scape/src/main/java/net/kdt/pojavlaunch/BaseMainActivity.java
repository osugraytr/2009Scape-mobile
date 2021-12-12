package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.ARCH_X86;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;

import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;
import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.drawerlayout.widget.*;
import com.google.android.material.navigation.*;
import com.kdt.LoggerView;

import java.io.*;
import java.util.*;

import net.kdt.pojavlaunch.prefs.*;
import org.lwjgl.glfw.*;

public class BaseMainActivity extends BaseActivity {
    public static volatile ClipboardManager GLOBAL_CLIPBOARD;

    volatile public static boolean isInputStackCall;

    public float scaleFactor = 1;

    private boolean mIsResuming = false;

    private static Touchpad touchpad;    private LoggerView loggerView;
    
    private DrawerLayout drawerLayout;
    private NavigationView navDrawer;

    private NavigationView.OnNavigationItemSelectedListener gameActionListener;
    public NavigationView.OnNavigationItemSelectedListener ingameControlsEditorListener;


    protected void initLayout(int resId) {
        setContentView(resId);

        try {
            Logger.getInstance().reset();
            // FIXME: is it safe fot multi thread?
            GLOBAL_CLIPBOARD = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            loggerView = findViewById(R.id.mainLoggerView);


            Tools.getDisplayMetrics(this);
            windowWidth = Tools.getDisplayFriendlyRes(currentDisplayMetrics.widthPixels, scaleFactor);
            windowHeight = Tools.getDisplayFriendlyRes(currentDisplayMetrics.heightPixels, scaleFactor);
            System.out.println("WidthHeight: " + windowWidth + ":" + windowHeight);


            // Menu
            drawerLayout = findViewById(R.id.main_drawer_options);

            navDrawer = findViewById(R.id.main_navigation_view);
            gameActionListener = menuItem -> {
                switch (menuItem.getItemId()) {
                    case R.id.nav_forceclose:
                        //dialogForceClose(BaseMainActivity.this);
                        break;
                    case R.id.nav_viewlog:
                        //openLogOutput();
                        break;
                    //case R.id.nav_debug: minecraftGLView.togglepointerDebugging();
                    //   break;
                    case R.id.nav_customkey:
                        //dialogSendCustomKey();
                        break;
                    case R.id.nav_mousespd:
                        //adjustMouseSpeedLive();
                        break;
                    case R.id.nav_customctrl:
                        //openCustomControls();
                        break;
                }

                drawerLayout.closeDrawers();
                return true;
            };
            navDrawer.setNavigationItemSelectedListener(gameActionListener);

            touchpad = findViewById(R.id.main_touchpad);

            //this.minecraftGLView = findViewById(R.id.main_game_render_view);
            //this.drawerLayout.closeDrawers();

            //minecraftGLView.setSurfaceReadyListener(() -> {
                try {
                    //runCraft();
                }catch (Throwable e){
                    Tools.showError(getApplicationContext(), e, true);
                }

            //minecraftGLView.start();

        } catch(Exception e){

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsResuming = true;
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onPause() {
        if (CallbackBridge.isGrabbing()){
            sendKeyPress(LWJGLGLFWKeycode.GLFW_KEY_ESCAPE);
        }
        mIsResuming = false;
        super.onPause();
    }

    public static void fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static boolean isAndroid8OrHigher() {
        return Build.VERSION.SDK_INT >= 26; 
    }

    private void checkLWJGL3Installed() {
        File lwjgl3dir = new File(Tools.DIR_GAME_HOME, "lwjgl3");
        if (!lwjgl3dir.exists() || lwjgl3dir.isFile() || lwjgl3dir.list().length == 0) {
            Logger.getInstance().appendToLog("Error: LWJGL3 was not installed!");
            throw new RuntimeException(getString(R.string.mcn_check_fail_lwjgl));
        } else {
            Logger.getInstance().appendToLog("Info: LWJGL3 directory: " + Arrays.toString(lwjgl3dir.list()));
        }
    }

    private void checkVulkanZinkIsSupported() {
        if (Tools.DEVICE_ARCHITECTURE == ARCH_X86
         || Build.VERSION.SDK_INT < 25
         || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
         || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            Logger.getInstance().appendToLog("Error: Vulkan Zink renderer is not supported!");
            throw new RuntimeException(getString(R.string. mcn_check_fail_vulkan_support));
        }
    }
    
    public void printStream(InputStream stream) {
        try {
            BufferedReader buffStream = new BufferedReader(new InputStreamReader(stream));
            String line = null;
            while ((line = buffStream.readLine()) != null) {
                Logger.getInstance().appendToLog(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String fromArray(List<String> arr) {
        StringBuilder s = new StringBuilder();
        for (String exec : arr) {
            s.append(" ").append(exec);
        }
        return s.toString();
    }

    boolean isInEditor;

    private void openLogOutput() {
        loggerView.setVisibility(View.VISIBLE);
        mIsResuming = false;
    }

    public void closeLogOutput(View view) {
        loggerView.setVisibility(View.GONE);
        mIsResuming = true;
    }

    public void toggleMenu(View v) {
        drawerLayout.openDrawer(Gravity.RIGHT);
    }

    public static void toggleMouse(Context ctx) {
        if (CallbackBridge.isGrabbing()) return;

        Toast.makeText(ctx, touchpad.switchState()
                 ? R.string.control_mouseon : R.string.control_mouseoff,
                Toast.LENGTH_SHORT).show();
    }

    public static void dialogForceClose(Context ctx) {
        new AlertDialog.Builder(ctx)
            .setMessage(R.string.mcn_exit_confirm)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, (p1, p2) -> {
                try {
                    fullyExit();
                } catch (Throwable th) {
                    Log.w(Tools.APP_NAME, "Could not enable System.exit() method!", th);
                }
            }).show();
    }


    int tmpMouseSpeed;
    public void adjustMouseSpeedLive() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.mcl_setting_title_mousespeed);
        View v = LayoutInflater.from(this).inflate(R.layout.live_mouse_speed_editor,null);
        final SeekBar sb = v.findViewById(R.id.mouseSpeed);
        final TextView tv = v.findViewById(R.id.mouseSpeedTV);
        sb.setMax(275);
        tmpMouseSpeed = (int) ((LauncherPreferences.PREF_MOUSESPEED*100));
        sb.setProgress(tmpMouseSpeed-25);
        tv.setText(tmpMouseSpeed +" %");
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tmpMouseSpeed = i+25;
                tv.setText(tmpMouseSpeed +" %");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        b.setView(v);
        b.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            LauncherPreferences.PREF_MOUSESPEED = ((float)tmpMouseSpeed)/100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("mousespeed",tmpMouseSpeed).commit();
            dialogInterface.dismiss();
            System.gc();
        });
        b.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            System.gc();
        });
        b.show();
    }
}
