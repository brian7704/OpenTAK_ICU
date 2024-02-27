package io.opentakserver.opentakicu.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.pedro.common.AudioCodec;

import java.util.ArrayList;

import io.opentakserver.opentakicu.R;

public class AudioPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private ListPreference sample_rate;
    private SwitchPreference stereo;
    private SharedPreferences prefs;
    private static final String LOGTAG = "AudioPreferences";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    private void setSampleRates(String codec) {
        Log.d(LOGTAG, "setSampleRates");
        ArrayList<String> samplerates = new ArrayList<>();

        if (codec.equals(AudioCodec.OPUS.name())) {
            Log.d(LOGTAG, "OPUS");
            samplerates.add("8000");
            samplerates.add("12000");
            samplerates.add("16000");
            samplerates.add("24000");
            samplerates.add("48000");
        } else if (codec.equals(AudioCodec.AAC.name())) {
            Log.d(LOGTAG, "AAC");
            samplerates.add("8000");
            samplerates.add("11025");
            samplerates.add("12000");
            samplerates.add("16000");
            samplerates.add("22050");
            samplerates.add("24000");
            samplerates.add("32000");
            samplerates.add("44100");
            samplerates.add("48000");
        } else if (codec.equals(AudioCodec.G711.name())) {
            Log.d(LOGTAG, "G711");
            samplerates.add("8000");
        }

        sample_rate.setEntries(samplerates.toArray(new CharSequence[samplerates.size()]));
        sample_rate.setEntryValues(samplerates.toArray(new CharSequence[samplerates.size()]));
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.audio_preferences, rootKey);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        sample_rate = findPreference("samplerate");
        sample_rate.setOnPreferenceClickListener(this);
        setSampleRates(prefs.getString("audio_codec", AudioCodec.OPUS.name()));

        findPreference("audio_codec").setOnPreferenceChangeListener(this);
        stereo = findPreference("stereo");
        if (prefs.getString("audio_codec", AudioCodec.OPUS.name()).equals(AudioCodec.G711.name())) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals("samplerate")) {
            setSampleRates(prefs.getString("audio_codec", AudioCodec.OPUS.name()));
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference.getKey().equals("audio_codec") && newValue.equals(AudioCodec.G711.name())) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
            prefs.edit().putString("audio_codec", (String) newValue).apply();
            prefs.edit().putBoolean("stereo", false).apply();
            prefs.edit().putString("sample_rate", "8000").apply();
            setSampleRates((String) newValue);
            return true;
        } else if (preference.getKey().equals("audio_codec")) {
            sample_rate.setEnabled(true);
            stereo.setEnabled(true);
            stereo.setChecked(true);
            setSampleRates((String) newValue);
            return true;
        }
        return false;
    }
}
