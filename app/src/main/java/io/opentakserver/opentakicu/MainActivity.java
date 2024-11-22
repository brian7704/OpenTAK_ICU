package io.opentakserver.opentakicu;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "MainActivity";
    SharedPreferences prefs;
    String videoSource = "camera2";

    public MainActivity() {
        super(R.layout.main_activity);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        videoSource = prefs.getString("video_source", videoSource);

        Log.d(LOGTAG, "Video Source " + videoSource + " " + savedInstanceState);

        if (videoSource.equals("usb") && savedInstanceState == null) {
            Log.d(LOGTAG, "Launching USB Fragment");
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, USBCameraFragment.class, null)
                    .commit();

        } else if (savedInstanceState == null) {
            Log.d(LOGTAG, "Launching Camera2 Fragment");
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, Camera2Fragment.class, null)
                    .commit();
        } else {
            Log.d(LOGTAG, "GO FUCK YOURSELF " + savedInstanceState + " " + videoSource);
        }
    }
}
