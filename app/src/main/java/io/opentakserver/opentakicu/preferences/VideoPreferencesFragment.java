package io.opentakserver.opentakicu.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.util.Size;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.library.rtsp.RtspCamera2;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.R;
import io.opentakserver.opentakicu.contants.Preferences;

import static io.opentakserver.opentakicu.preferences.ATAKPreferencesFragment.copy;

public class VideoPreferencesFragment extends PreferenceFragmentCompat implements ConnectChecker,
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String LOGTAG = "VideoPreferencesFragment";
    private RtspCamera2 rtspCamera2;
    private SharedPreferences prefs;
    private ActivityResultLauncher chromaBgFileBrowserLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        chromaBgFileBrowserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            Intent data = result.getData();
                            Log.d(LOGTAG, "Got file: " + data.getData().getPath());
                            //prefs.edit().putString(Preferences.CHROMA_KEY_BACKGROUND, data.getData().getPath()).apply();

                            InputStream bgInputStream = requireContext().getContentResolver().openInputStream(data.getData());
                            String bgFileName = data.getData().getPath().split(":")[1];
                            Path p = Paths.get(bgFileName);
                            File filesDir = requireContext().getFilesDir();
                            File dest = new File(filesDir.getAbsolutePath() + "/" + p.getFileName().toString());
                            Log.d(LOGTAG, dest.getAbsolutePath());

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                FileUtils.copy(bgInputStream, Files.newOutputStream(dest.toPath()));
                            } else {
                                copy(bgInputStream, dest);
                            }

                            prefs.edit().putString(Preferences.CHROMA_KEY_BACKGROUND, dest.getAbsolutePath()).apply();
                            Log.d(LOGTAG, "Chroma BG File: " + dest.getAbsolutePath());
                        } catch (Exception e) {
                            Log.d(LOGTAG, "Failed to get chroma bg: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.video_settings, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        rtspCamera2 = new RtspCamera2(getActivity(), this);
        ListPreference resolutions = findPreference(Preferences.VIDEO_RESOLUTION);
        ArrayList<String> resolutionsList = new ArrayList<>();
        ArrayList<String> resolutionsInts = new ArrayList<>();
        int x = 0;

        setVideoCodecs();

        for (Size res : rtspCamera2.getResolutionsBack()) {
            resolutionsList.add(res.getWidth() + " x " + res.getHeight());
            resolutionsInts.add(String.valueOf(x));
            x++;
        }

        resolutions.setEntries(resolutionsList.toArray(new CharSequence[resolutionsList.size()]));
        resolutions.setEntryValues(resolutionsInts.toArray(new CharSequence[resolutionsInts.size()]));

        String video_source = prefs.getString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_DEFAULT);
        if (!video_source.equals(Preferences.VIDEO_SOURCE_USB)) {
            findPreference(Preferences.USB_WIDTH).setEnabled(false);
            findPreference(Preferences.USB_HEIGHT).setEnabled(false);
        }

        if (!video_source.equals(Preferences.VIDEO_SOURCE_DEFAULT))
            findPreference(Preferences.VIDEO_RESOLUTION).setEnabled(false);

        findPreference(Preferences.CHROMA_KEY_BACKGROUND).setOnPreferenceClickListener(this);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        String video_source = prefs.getString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_DEFAULT);
        if (!video_source.equals("usb")) {
            findPreference(Preferences.USB_WIDTH).setEnabled(false);
            findPreference(Preferences.USB_HEIGHT).setEnabled(false);
            findPreference(Preferences.VIDEO_RESOLUTION).setEnabled(true);
        } else {
            findPreference(Preferences.USB_WIDTH).setEnabled(true);
            findPreference(Preferences.USB_HEIGHT).setEnabled(true);
            findPreference(Preferences.VIDEO_RESOLUTION).setEnabled(false);
        }
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

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        Log.d(LOGTAG, "onPrefClick: " + preference.getKey());
        if (preference.getKey().equals(Preferences.CHROMA_KEY_BACKGROUND)) {
            Intent fileBrowserIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileBrowserIntent.setType("*/*");
            chromaBgFileBrowserLauncher.launch(fileBrowserIntent);
            return true;
        }
        return false;
    }
}
