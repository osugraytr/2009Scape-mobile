package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.getFileName;

import android.app.*;
import android.content.*;
import android.view.*;

import androidx.annotation.Nullable;

public abstract class BaseLauncherActivity extends BaseActivity {
    protected boolean canBack = false;

    /**
     * Used by the install button from the layout_main_v4
     * @param view The view triggering the function
     */


    public static final int RUN_MOD_INSTALLER = 2050;
    
    @Override
    public void onBackPressed() {
        if (canBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        System.out.println("call to onPostResume");
        Tools.updateWindowSize(this);
        System.out.println("call to onPostResume; E");
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        System.out.println("call to onResume");
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        System.out.println("call to onResume; E");
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        System.out.println(resultCode);
        if(resultCode == Activity.RESULT_OK) {
            if (requestCode == RUN_MOD_INSTALLER) {
                if (data == null) return;
                Thread t = new Thread(()->{
                    BaseLauncherActivity.this.runOnUiThread(() -> {
                        Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                        startActivity(intent);
                    });
                });
                t.start();
            }
        }
    }
}
