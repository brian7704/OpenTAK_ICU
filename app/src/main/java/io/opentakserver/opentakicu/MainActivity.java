package io.opentakserver.opentakicu;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "MainActivity";

    public MainActivity() {
        super(R.layout.main_activity);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");


        Log.d(LOGTAG, "Launching Camera2 Fragment");
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, Camera2Fragment.class, null)
                .commit();
    }
}
