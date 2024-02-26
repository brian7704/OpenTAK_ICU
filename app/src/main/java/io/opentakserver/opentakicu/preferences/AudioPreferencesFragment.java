package io.opentakserver.opentakicu.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import io.opentakserver.opentakicu.R;

public class AudioPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private EditTextPreference sample_rate;
    private SwitchPreference stereo;
    private SharedPreferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.audio_preferences, rootKey);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        findPreference("audio_codec").setOnPreferenceChangeListener(this);
        sample_rate = findPreference("samplerate");
        stereo = findPreference("stereo");
        if (prefs.getString("audio_codec", "AAC").equals("G711")) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals("audio_codec")) {

            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference.getKey().equals("audio_codec") && newValue.equals("G711")) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
            prefs.edit().putString("audio_codec", (String) newValue).apply();
            prefs.edit().putBoolean("stereo", false).apply();
            prefs.edit().putString("sample_rate", "8000").apply();
            return true;
        } else if (preference.getKey().equals("audio_codec")) {
            sample_rate.setEnabled(true);
            stereo.setEnabled(true);
            stereo.setChecked(true);
            return true;
        }
        return false;
    }
}
