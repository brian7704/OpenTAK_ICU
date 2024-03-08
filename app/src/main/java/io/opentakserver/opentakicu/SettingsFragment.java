package io.opentakserver.opentakicu;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static String LOGTAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}