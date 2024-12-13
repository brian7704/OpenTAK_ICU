package io.opentakserver.opentakicu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "MainActivity";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();

    public MainActivity() {
        super(R.layout.main_activity);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");

        if (!hasPermissions(this, PERMISSIONS)) {
            Intent intent = new Intent(this, OnBoardingActivity.class);
            startActivity(intent);
            this.finish();
            return;
        }

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setNavigationBarDividerColor(Color.TRANSPARENT);
        }

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, Camera2Fragment.class, null)
                .commit();
    }

    private boolean hasPermissions(Context context, ArrayList<String> permissions) {
        permissions();

        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void permissions() {
        PERMISSIONS.add(android.Manifest.permission.RECORD_AUDIO);
        PERMISSIONS.add(android.Manifest.permission.CAMERA);
        PERMISSIONS.add(android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            PERMISSIONS.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PERMISSIONS.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}
