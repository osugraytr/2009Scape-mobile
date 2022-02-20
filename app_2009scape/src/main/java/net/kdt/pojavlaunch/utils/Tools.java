package net.kdt.pojavlaunch.utils;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.*;
import android.os.*;
import android.util.*;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.R;

import org.lwjgl.glfw.*;
import android.view.*;

import static android.os.Build.VERSION.SDK_INT;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_NOTCH_SIZE;

public final class Tools {
    public static final boolean ENABLE_DEV_FEATURES = BuildConfig.DEBUG;

    public static String APP_NAME = "null";
    
    public static final Gson GLOBAL_GSON = new GsonBuilder().setPrettyPrinting().create();
    
    public static final String URL_HOME = "https://pojavlauncherteam.github.io/PojavLauncher";

    public static String DIR_DATA; //Initialized later to get context
    public static String MULTIRT_HOME;
    public static String LOCAL_RENDERER = null;
    public static int DEVICE_ARCHITECTURE;

    // New since 3.3.1
    public static String DIR_ACCOUNT_NEW;
    public static String DIR_ACCOUNT_OLD;
    public static String DIR_GAME_HOME = Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/PojavLauncher";
    public static String DIR_GAME_NEW;
    public static String DIR_GAME_OLD = Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/.minecraft";
    
    // New since 3.0.0
    public static String DIR_HOME_JRE;
    public static String DIRNAME_HOME_JRE = "lib";

    // New since 2.4.2
    public static String DIR_HOME_VERSION;
    public static String DIR_HOME_LIBRARY;

    public static String DIR_HOME_CRASH;

    public static String ASSETS_PATH;
    public static String OBSOLETE_RESOURCES_PATH;
    public static String CTRLMAP_PATH;
    public static String CTRLDEF_FILE;
    
    public static final String LIBNAME_OPTIFINE = "optifine:OptiFine";

    /**
     * Since some constant requires the use of the Context object
     * You can call this function to initialize them.
     * Any value (in)directly dependant on DIR_DATA should be set only here.
     */
    public static void initContextConstants(Context ctx){
        DIR_DATA = ctx.getFilesDir().getParent();
        MULTIRT_HOME = DIR_DATA+"/runtimes";
        if(SDK_INT >= 29) {
            DIR_GAME_HOME = ctx.getExternalFilesDir(null).getAbsolutePath();
        }else{
            DIR_GAME_HOME = new File(Environment.getExternalStorageDirectory(),"games/PojavLauncher").getAbsolutePath();
        }
        DIR_GAME_NEW = DIR_GAME_HOME + "/.minecraft";
        DIR_HOME_VERSION = DIR_GAME_NEW + "/versions";
        DIR_HOME_LIBRARY = DIR_GAME_NEW + "/libraries";
        DIR_HOME_CRASH = DIR_GAME_NEW + "/crash-reports";
        ASSETS_PATH = DIR_GAME_NEW + "/assets";
        OBSOLETE_RESOURCES_PATH= DIR_GAME_NEW + "/resources";
        CTRLMAP_PATH = DIR_GAME_HOME + "/controlmap";
        CTRLDEF_FILE = DIR_GAME_HOME + "/controlmap/default.json";
    }
    
    public static void getCacioJavaArgs(List<String> javaArgList, boolean isHeadless) {
        javaArgList.add("-Djava.awt.headless="+isHeadless);
        // Caciocavallo config AWT-enabled version
        javaArgList.add("-Dcacio.managed.screensize=" + CallbackBridge.physicalWidth + "x" + CallbackBridge.physicalHeight);
        // javaArgList.add("-Dcacio.font.fontmanager=net.java.openjdk.cacio.ctc.CTCFontManager");
        javaArgList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager");
        javaArgList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler");
        javaArgList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel");
        javaArgList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit");
        javaArgList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment");

        StringBuilder cacioClasspath = new StringBuilder();
        cacioClasspath.append("-Xbootclasspath/p");
        File cacioDir = new File(DIR_GAME_HOME + "/caciocavallo");
        if (cacioDir.exists() && cacioDir.isDirectory()) {
            for (File file : cacioDir.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    cacioClasspath.append(":" + file.getAbsolutePath());
                }
            }
        }
        javaArgList.add(cacioClasspath.toString());
    }

    public static String fromStringArray(String[] strArr) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strArr.length; i++) {
            if (i > 0) builder.append(" ");
            builder.append(strArr[i]);
        }

        return builder.toString();
    }

    private static String[] splitAndFilterEmpty(String argStr) {
        List<String> strList = new ArrayList<String>();
        for (String arg : argStr.split(" ")) {
            if (!arg.isEmpty()) {
                strList.add(arg);
            }
        }
        strList.add("--fullscreen");
        return strList.toArray(new String[0]);
    }

    public static String artifactToPath(String group, String artifact, String version) {
        return group.replaceAll("\\.", "/") + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    public static String getPatchedFile(String version) {
        return DIR_HOME_VERSION + "/" + version + "/" + version + ".jar";
    }

    private static String getLWJGL3ClassPath() {
        StringBuilder libStr = new StringBuilder();
        File lwjgl3Folder = new File(Tools.DIR_GAME_HOME, "lwjgl3");
        if (/* info.arguments != null && */ lwjgl3Folder.exists()) {
            for (File file: lwjgl3Folder.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    libStr.append(file.getAbsolutePath() + ":");
                }
            }
        }
        // Remove the ':' at the end
        libStr.setLength(libStr.length() - 1);
        return libStr.toString();
    }

    private static boolean isClientFirst = false;

    public static DisplayMetrics getDisplayMetrics(Activity activity) {
        DisplayMetrics displayMetrics = new DisplayMetrics();

        if(SDK_INT >= Build.VERSION_CODES.N && (activity.isInMultiWindowMode() || activity.isInPictureInPictureMode())
        || PREF_NOTCH_SIZE == -1 ){
            //For devices with free form/split screen, we need window size, not screen size.
            displayMetrics = activity.getResources().getDisplayMetrics();
        }else{
            if (SDK_INT >= Build.VERSION_CODES.R) {
                activity.getDisplay().getRealMetrics(displayMetrics);
            } else {
                 activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            }
            if(!PREF_IGNORE_NOTCH){
                //Remove notch width when it isn't ignored.
                displayMetrics.widthPixels -= PREF_NOTCH_SIZE;
            }
        }
        currentDisplayMetrics = displayMetrics;

        return displayMetrics;
    }

    public static void setFullscreen(Activity act) {
        final View decorView = act.getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                    }
                }
            });
    }

    public static DisplayMetrics currentDisplayMetrics;

    public static void updateWindowSize(Activity ctx) {
        currentDisplayMetrics = getDisplayMetrics(ctx);

        CallbackBridge.physicalWidth = (int) (currentDisplayMetrics.widthPixels);
        CallbackBridge.physicalHeight = (int) (currentDisplayMetrics.heightPixels);
    }

    public static float dpToPx(float dp) {
        //Better hope for the currentDisplayMetrics to be good
        return dp * currentDisplayMetrics.density;
    }

    public static float pxToDp(float px){
        //Better hope for the currentDisplayMetrics to be good
        return px / currentDisplayMetrics.density;
    }

    public static void copyAssetFile(Context ctx, String fileName, String output, boolean overwrite) throws IOException {
        copyAssetFile(ctx, fileName, output, new File(fileName).getName(), overwrite);
    }

    public static void copyAssetFile(Context ctx, String fileName, String output, String outputName, boolean overwrite) throws IOException
    {
        File file = new File(output);
        if(!file.exists()) {
            file.mkdirs();
        }
        File file2 = new File(output, outputName);
        if(!file2.exists() || overwrite){
            write(file2.getAbsolutePath(), loadFromAssetToByte(ctx, fileName));
        }
    }

    public static void showError(Context ctx, Throwable e) {
        showError(ctx, e, false);
    }

    public static void showError(final Context ctx, final Throwable e, final boolean exitIfOk) {
        showError(ctx, R.string.global_error, e, exitIfOk, false);
    }

    public static void showError(final Context ctx, final int titleId, final Throwable e, final boolean exitIfOk) {
        showError(ctx, titleId, e, exitIfOk, false);
    }

    private static void showError(final Context ctx, final int titleId, final Throwable e, final boolean exitIfOk, final boolean showMore) {
        e.printStackTrace();
        
        Runnable runnable = new Runnable(){

            @Override
            public void run()
            {
                final String errMsg = showMore ? Log.getStackTraceString(e): e.getMessage();
                AlertDialog.Builder builder = new AlertDialog.Builder((Context) ctx)
                    .setTitle(titleId)
                    .setMessage(errMsg)
                    .setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) (p1, p2) -> {
                        if(exitIfOk) {
                            if (ctx instanceof BaseMainActivity) {
                                BaseMainActivity.fullyExit();
                            } else if (ctx instanceof Activity) {
                                ((Activity) ctx).finish();
                            }
                        }
                    })
                    .setNegativeButton(showMore ? R.string.error_show_less : R.string.error_show_more, (DialogInterface.OnClickListener) (p1, p2) -> showError(ctx, titleId, e, exitIfOk, !showMore))
                    .setNeutralButton(android.R.string.copy, (DialogInterface.OnClickListener) (p1, p2) -> {
                        ClipboardManager mgr = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        mgr.setPrimaryClip(ClipData.newPlainText("error", Log.getStackTraceString(e)));
                        if(exitIfOk) {
                            if (ctx instanceof BaseMainActivity) {
                                BaseMainActivity.fullyExit();
                            } else {
                                ((Activity) ctx).finish();
                            }
                        }
                    })
                    .setCancelable(!exitIfOk);
                try {
                    builder.show();
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        };

        if (ctx instanceof Activity) {
            ((Activity) ctx).runOnUiThread(runnable);
        } else {
            runnable.run();
        }
    }

    public static void dialogOnUiThread(final Activity ctx, final CharSequence title, final CharSequence message) {
        ctx.runOnUiThread(() -> new AlertDialog.Builder(ctx)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show());
    }

    public static void moveInside(String from, String to) {
        File fromFile = new File(from);
        for (File fromInside : fromFile.listFiles()) {
            moveRecursive(fromInside.getAbsolutePath(), to);
        }
        fromFile.delete();
    }

    public static void moveRecursive(String from, String to) {
        moveRecursive(new File(from), new File(to));
    }

    public static void moveRecursive(File from, File to) {
        File toFrom = new File(to, from.getName());
        try {
            if (from.isDirectory()) {
                for (File child : from.listFiles()) {
                    moveRecursive(child, toFrom);
                }
            }
        } finally {
            from.getParentFile().mkdirs();
            from.renameTo(toFrom);
        }
    }
    
    public static String convertStream(InputStream inputStream, Charset charset) throws IOException {
        String out = "";
        int len;
        byte[] buf = new byte[512];
        while((len = inputStream.read(buf))!=-1) {
            out += new String(buf,0,len,charset);
        }
        return out;
    }

    public static File lastFileModified(String dir) {
        File fl = new File(dir);

        File[] files = fl.listFiles(new FileFilter() {          
                public boolean accept(File file) {
                    return file.isFile();
                }
            });

        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }

        return choice;
    }


    public static String read(InputStream is) throws IOException {
        String out = "";
        int len;
        byte[] buf = new byte[512];
        while((len = is.read(buf))!=-1) {
            out += new String(buf,0,len);
        }
        return out;
    }

    public static String read(String path) throws IOException {
        return read(new FileInputStream(path));
    }

    public static void write(String path, byte[] content) throws IOException
    {
        File outPath = new File(path);
        outPath.getParentFile().mkdirs();
        outPath.createNewFile();

        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
        fos.write(content, 0, content.length);
        fos.close();
    }

    public static void write(String path, String content) throws IOException {
        write(path, content.getBytes());
    }

    public static byte[] loadFromAssetToByte(Context ctx, String inFile) {
        byte[] buffer = null;

        try {
            InputStream stream = ctx.getAssets().open(inFile);

            int size = stream.available();
            buffer = new byte[size];
            stream.read(buffer);
            stream.close();
        } catch (IOException e) {
            // Handle exceptions here
            e.printStackTrace();
        }
        return buffer;
    }

    public static class ZipTool
    {
        private ZipTool(){}
        public static void zip(List<File> files, File zipFile) throws IOException {
            final int BUFFER_SIZE = 2048;

            BufferedInputStream origin = null;
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

            try {
                byte data[] = new byte[BUFFER_SIZE];

                for (File file : files) {
                    FileInputStream fileInputStream = new FileInputStream( file );

                    origin = new BufferedInputStream(fileInputStream, BUFFER_SIZE);

                    try {
                        ZipEntry entry = new ZipEntry(file.getName());

                        out.putNextEntry(entry);

                        int count;
                        while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                            out.write(data, 0, count);
                        }
                    }
                    finally {
                        origin.close();
                    }
                }
            } finally {
                out.close();
            }
        }
        public static void unzip(File zipFile, File targetDirectory) throws IOException {
            final int BUFFER_SIZE = 1024;
            ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
            try {
                ZipEntry ze;
                int count;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((ze = zis.getNextEntry()) != null) {
                    File file = new File(targetDirectory, ze.getName());
                    File dir = ze.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                                        dir.getAbsolutePath());
                    if (ze.isDirectory())
                        continue;
                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        fout.close();
                    }
                    /* if time should be restored as well
                     long time = ze.getTime();
                     if (time > 0)
                     file.setLastModified(time);
                     */
                }
            } finally {
                zis.close();
            }
        }
    }

    public static int getTotalDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.totalMem / 1048576L);
    }

    public static int getFreeDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.availMem / 1048576L);
    }

    public static String getFileName(Context ctx, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = ctx.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    //result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
