package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public class SettingsMenu extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_menu);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(((int)(width*0.8)),((int)(height*0.8)));

        final EditText username = findViewById(R.id.username);
        final EditText password = findViewById(R.id.password);
        final Switch righthanded = findViewById(R.id.righthanded);


        //For storing string value in sharedPreference
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();

        // Restore from saved values
        username.setText(preferences.getString("username",""));
        password.setText(preferences.getString("password",""));
        righthanded.setChecked(Boolean.parseBoolean(preferences.getString("righthanded","")));


        username.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                System.out.println("Username: "+username.getText());
                editor.putString("username",username.getText().toString());
                editor.commit();
                return true;
            }
            return false;
        });

        password.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                System.out.println("Password: "+password.getText());
                editor.putString("password",password.getText().toString());
                editor.commit();
            }
            return false;
        });

        righthanded.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
                System.out.println(isChecked);
                editor.putString("righthanded",""+isChecked);
                editor.commit();
            }
        });

    }
}
