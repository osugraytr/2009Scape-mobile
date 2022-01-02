package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.ARCH_X86;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;

import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;
import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
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

    @Override
    public void onResume() {
        super.onResume();
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
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

    public static String fromArray(List<String> arr) {
        StringBuilder s = new StringBuilder();
        for (String exec : arr) {
            s.append(" ").append(exec);
        }
        return s.toString();
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
}
