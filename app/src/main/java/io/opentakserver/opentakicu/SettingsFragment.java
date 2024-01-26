package io.opentakserver.opentakicu;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        Log.d("SetFrag", "onCreatePrefs");
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
