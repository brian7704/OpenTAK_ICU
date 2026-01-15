package io.opentakserver.opentakicu;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.datastore.preferences.core.PreferencesKt;
import androidx.fragment.app.FragmentManager;

public class SettingsActivity extends AppCompatActivity {
    private static final String LOGTAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        setContentView(R.layout.activity_settings);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.idFrameLayout, new SettingsFragment(), "")
                .commit();
    }
}
