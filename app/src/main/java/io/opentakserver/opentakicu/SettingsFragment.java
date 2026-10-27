package io.opentakserver.opentakicu;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Locale;

import io.opentakserver.opentakicu.contants.Preferences;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static String LOGTAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning from a sub screen where values may have changed
        updateSummaries();
    }

    private void updateSummaries() {
        SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
        if (prefs == null) {
            return;
        }
        setSummary("stream_settings", streamSummary(prefs));
        setSummary("video_settings", videoSummary(prefs));
        setSummary("audio_settings", audioSummary(prefs));
        setSummary("atak_settings", takSummary(prefs));
    }

    private void setSummary(String key, String summary) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(summary);
        }
    }

    private String streamSummary(SharedPreferences prefs) {
        if (!prefs.getBoolean(Preferences.STREAM_VIDEO, Preferences.STREAM_VIDEO_DEFAULT)) {
            return getString(R.string.settings_summary_off);
        }
        String protocol = prefs.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT);
        String address = prefs.getString(Preferences.STREAM_ADDRESS, Preferences.STREAM_ADDRESS_DEFAULT);
        String port = prefs.getString(Preferences.STREAM_PORT, Preferences.STREAM_PORT_DEFAULT);
        return protocol.toUpperCase(Locale.ROOT) + " · " + address + ":" + port;
    }

    private String videoSummary(SharedPreferences prefs) {
        String source = prefs.getString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_DEFAULT);
        String fps = prefs.getString(Preferences.VIDEO_FPS, Preferences.VIDEO_FPS_DEFAULT);
        String codec = prefs.getString(Preferences.VIDEO_CODEC, Preferences.VIDEO_CODEC_DEFAULT);
        String bitrate = prefs.getString(Preferences.VIDEO_BITRATE, Preferences.VIDEO_BITRATE_DEFAULT);
        return videoSourceLabel(source)
                + " · " + getString(R.string.settings_summary_fps, fps)
                + " · " + codec
                + " · " + getString(R.string.settings_summary_kbps, bitrate);
    }

    private String audioSummary(SharedPreferences prefs) {
        if (!prefs.getBoolean(Preferences.ENABLE_AUDIO, Preferences.ENABLE_AUDIO_DEFAULT)) {
            return getString(R.string.settings_summary_off);
        }
        String codec = prefs.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT);
        String bitrate = prefs.getString(Preferences.AUDIO_BITRATE, Preferences.AUDIO_BITRATE_DEFAULT);
        return codec + " · " + getString(R.string.settings_summary_kbps, bitrate);
    }

    private String takSummary(SharedPreferences prefs) {
        if (!prefs.getBoolean(Preferences.ATAK_SEND_COT, Preferences.ATAK_SEND_COT_DEFAULT)) {
            return getString(R.string.settings_summary_off);
        }
        String address = prefs.getString(Preferences.ATAK_SERVER_ADDRESS, Preferences.ATAK_SERVER_ADDRESS_DEFAULT);
        String port = prefs.getString(Preferences.ATAK_SERVER_PORT, Preferences.ATAK_SERVER_PORT_DEFAULT);
        return address + ":" + port;
    }

    private String videoSourceLabel(String source) {
        if (Preferences.VIDEO_SOURCE_SCREEN.equals(source)) {
            return getString(R.string.screen);
        }
        String[] labels = getResources().getStringArray(R.array.video_sources);
        String[] values = getResources().getStringArray(R.array.video_sources_values);
        for (int i = 0; i < values.length && i < labels.length; i++) {
            if (values[i].equals(source)) {
                return labels[i];
            }
        }
        return source;
    }
}
