package io.opentakserver.opentakicu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Settings");

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .add(R.id.idFrameLayout, new SettingsFragment(), "SOMEFUCKINGLOGTAG")
                .commit();
    }
}
