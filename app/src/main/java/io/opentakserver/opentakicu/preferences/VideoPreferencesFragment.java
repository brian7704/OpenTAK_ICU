package io.opentakserver.opentakicu.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.library.rtsp.RtspCamera2;

import java.util.ArrayList;
import java.util.List;

import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.R;
import io.opentakserver.opentakicu.contants.Preferences;

public class VideoPreferencesFragment extends PreferenceFragmentCompat implements ConnectChecker {

    private RtspCamera2 rtspCamera2;
    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.video_settings, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        rtspCamera2 = new RtspCamera2(getActivity(), false, this);
        ListPreference resolutions = findPreference(Preferences.VIDEO_RESOLUTION);
        ArrayList<String> resolutionsList = new ArrayList<>();
        ArrayList<String> resolutionsInts = new ArrayList<>();
        int x = 0;
        List<Size> frontResolutions = rtspCamera2.getResolutionsFront();

        setVideoCodecs();

        // Only get resolutions supported by both cameras
        for (Size res : rtspCamera2.getResolutionsBack()) {
            if (frontResolutions.contains(res)) {
                resolutionsList.add(res.getWidth() + " x " + res.getHeight());
                resolutionsInts.add(String.valueOf(x));
                x++;
            }
        }
        resolutions.setEntries(resolutionsList.toArray(new CharSequence[resolutionsList.size()]));
        resolutions.setEntryValues(resolutionsInts.toArray(new CharSequence[resolutionsInts.size()]));
    }

    private void setVideoCodecs() {
        ArrayList<String> codecs = new ArrayList<>();
        codecs.add(VideoCodec.H264.name());
        codecs.add(VideoCodec.H265.name());

        String protocol = prefs.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT);

        if (!protocol.equals("srt") && !protocol.equals("udp")) {
            codecs.add(VideoCodec.AV1.name());
        }

        ListPreference video_codecs = findPreference(Preferences.VIDEO_CODEC);
        video_codecs.setEntries(codecs.toArray(new CharSequence[codecs.size()]));
        video_codecs.setEntryValues(codecs.toArray(new CharSequence[codecs.size()]));
    }

    @Override
    public void onAuthError() {

    }

    @Override
    public void onAuthSuccess() {

    }

    @Override
    public void onConnectionFailed(@NonNull String s) {

    }

    @Override
    public void onConnectionStarted(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onNewBitrate(long l) {

    }
}
