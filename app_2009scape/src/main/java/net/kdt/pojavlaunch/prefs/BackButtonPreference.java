package net.kdt.pojavlaunch.prefs;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

import net.kdt.pojavlaunch.R;

public class BackButtonPreference extends Preference {
    public BackButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackButtonPreference(Context context) {
        this(context, null);
    }


    @Override
    protected void onClick() {
        // It is caught by an ExtraListener in the LauncherActivity
    }
}
