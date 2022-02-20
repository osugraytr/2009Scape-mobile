package net.kdt.pojavlaunch;

import android.os.*;
import androidx.appcompat.app.*;
import net.kdt.pojavlaunch.utils.*;

public class BaseActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.setFullscreen(this);
        Tools.updateWindowSize(this);
    }
}
