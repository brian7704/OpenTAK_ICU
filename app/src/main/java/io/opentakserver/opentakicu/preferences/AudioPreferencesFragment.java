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
import io.opentakserver.opentakicu.contants.Preferences;

public class AudioPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private ListPreference sample_rate;
    private SwitchPreference stereo;
    private SharedPreferences prefs;
    private ListPreference codec;
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

    private void setAudioCodecs() {
        ArrayList<String> codecs = new ArrayList<>();
        codec = findPreference(Preferences.AUDIO_CODEC);
        String protocol = prefs.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT);
        if (protocol.startsWith("rtsp")) {
            codecs.add(AudioCodec.OPUS.name());
            codecs.add(AudioCodec.AAC.name());
            codecs.add(AudioCodec.G711.name());
        } else if (protocol.startsWith("rtmp")) {
            codecs.add(AudioCodec.AAC.name());
            codecs.add(AudioCodec.G711.name());
        } else if (protocol.equals("srt") || protocol.equals("udp")){
            codecs.add(AudioCodec.OPUS.name());
            codecs.add(AudioCodec.AAC.name());
        } else {
            codecs.add(AudioCodec.OPUS.name());
            codecs.add(AudioCodec.AAC.name());
            codecs.add(AudioCodec.G711.name());
        }

        codec.setEntries(codecs.toArray(new CharSequence[codecs.size()]));
        codec.setEntryValues(codecs.toArray(new CharSequence[codecs.size()]));
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.audio_preferences, rootKey);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        sample_rate = findPreference(Preferences.AUDIO_SAMPLE_RATE);
        sample_rate.setOnPreferenceClickListener(this);
        setSampleRates(prefs.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT));

        setAudioCodecs();

        findPreference(Preferences.AUDIO_CODEC).setOnPreferenceChangeListener(this);
        stereo = findPreference(Preferences.STEREO_AUDIO);
        if (prefs.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT).equals(AudioCodec.G711.name())) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals(Preferences.AUDIO_SAMPLE_RATE)) {
            setSampleRates(prefs.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT));
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference.getKey().equals(Preferences.AUDIO_CODEC) && newValue.equals(AudioCodec.G711.name())) {
            sample_rate.setEnabled(false);
            stereo.setEnabled(false);
            stereo.setChecked(false);
            prefs.edit().putString(Preferences.AUDIO_CODEC, (String) newValue).apply();
            prefs.edit().putBoolean(Preferences.STEREO_AUDIO, false).apply();
            prefs.edit().putString(Preferences.AUDIO_SAMPLE_RATE, "8000").apply();
            setSampleRates((String) newValue);
            return true;
        } else if (preference.getKey().equals(Preferences.AUDIO_CODEC)) {
            sample_rate.setEnabled(true);
            stereo.setEnabled(true);
            stereo.setChecked(true);
            setSampleRates((String) newValue);
            return true;
        }
        return false;
    }
}
