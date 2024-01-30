package io.opentakserver.opentakicu;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.pedro.common.ConnectChecker;
import com.pedro.library.rtsp.RtspCamera2;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;

public class SettingsFragment extends PreferenceFragmentCompat implements ConnectChecker {
    private RtspCamera2 rtspCamera2;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SetFrag", "onCreate");
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        Log.d("SetFrag", "onCreatePrefs " + rootKey);
        rtspCamera2 = new RtspCamera2(getActivity(), false, this);
        setPreferencesFromResource(R.xml.preferences, rootKey);
        ListPreference resolutions = findPreference("resolution");
        ArrayList<String> resolutionsList = new ArrayList<>();
        ArrayList<String> resolutionsInts = new ArrayList<>();
        int x = 0;
        for (Size res : rtspCamera2.getResolutionsBack()) {
            resolutionsList.add(res.getWidth() + " x " + res.getHeight());
            resolutionsInts.add(String.valueOf(x));
            x++;
        }
        resolutions.setEntries(resolutionsList.toArray(new CharSequence[resolutionsList.size()]));
        resolutions.setEntryValues(resolutionsInts.toArray(new CharSequence[resolutionsInts.size()]));
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
