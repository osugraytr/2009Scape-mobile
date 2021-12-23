package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SettingsMenu extends Activity {

    private static final int FILE_SELECT_CODE = 0;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    Log.d("TAG", "File Uri: " + uri.toString());
                    // Get the path
                    Log.d("TAG", "File Path: " + uri.getPath());
                    // Get the file instance
                    File config = new File(getFilesDir(), "config.json");
                    try {
                        Log.d("TAG", "Starting copy: " + uri.getPath());
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        FileOutputStream fileOutputStream = new FileOutputStream(config);
                        byte buf[]=new byte[1024];
                        int len;
                        while((len=inputStream.read(buf))>0) {
                            fileOutputStream.write(buf,0,len);
                        }
                        fileOutputStream.close();
                        inputStream.close();
                        System.out.println("Wrote new config to Local");
                    } catch (IOException e1) {
                        Log.d("error", "Error with file " + e1);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

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
        final Button loadConfig = findViewById(R.id.loadconfig);

        loadConfig.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i("Pressed","Button");
                        showFileChooser();
                        return true; // if you want to handle the touch event
                    case MotionEvent.ACTION_UP:
                        // RELEASED
                        return true; // if you want to handle the touch event
                }
                return false;
            }
        });


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
