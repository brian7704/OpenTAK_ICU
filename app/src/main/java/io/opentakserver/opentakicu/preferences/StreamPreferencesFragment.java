package io.opentakserver.opentakicu.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import com.pedro.common.AudioCodec;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;
import io.opentakserver.opentakicu.R;
import io.opentakserver.opentakicu.contants.Preferences;

public class StreamPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private final static String LOGTAG = "StreamPreferences";
    private ActivityResultLauncher fileBrowserLauncher;
    private SharedPreferences pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(getContext());

        findPreference(Preferences.STREAM_ADDRESS).setOnPreferenceChangeListener(this);
        findPreference(Preferences.STREAM_PROTOCOL).setOnPreferenceChangeListener(this);

        handleTcpSwitch(null);

        fileBrowserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        Intent data = result.getData();
                        Log.d(LOGTAG, "Got file: " + data.getData().getPath());

                        InputStream certInputStream = getContext().getContentResolver().openInputStream(data.getData());
                        String certFileName = data.getData().getPath().split(":")[1];
                        File filesDir = getContext().getFilesDir();
                        File dest = new File(filesDir.getAbsolutePath() + "/" + certFileName);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtils.copy(certInputStream, Files.newOutputStream(dest.toPath()));
                        } else {
                            copy(certInputStream, dest);
                        }

                        pref.edit().putString("certificate_temp", dest.getAbsolutePath()).apply();
                    } catch (Exception e) {
                        Log.d(LOGTAG, "Failed to get cert: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        );
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.streaming_preferences, rootKey);
        findPreference(Preferences.STREAM_CERTIFICATE).setOnPreferenceClickListener(this);
        findPreference("test_certificate").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals(Preferences.STREAM_CERTIFICATE)) {
            pref.edit().putString(Preferences.STREAM_CERTIFICATE, null).apply();
            Intent fileBrowserIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileBrowserIntent.setType("*/*");
            fileBrowserLauncher.launch(fileBrowserIntent);
            return true;
        } else if (preference.getKey().equals("test_certificate")) {
            testCert();
            return true;
        }
        return false;
    }

    public static void copy(InputStream in, File dst) throws IOException {
        try (OutputStream out = new FileOutputStream(dst)) {
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private void handleTcpSwitch(String newValue) {
        SwitchPreference tcp = findPreference(Preferences.STREAM_USE_TCP);
        String protocol = newValue;
        if (protocol == null)
            protocol = pref.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT);

        if (protocol.equals("rtsp")) {
            tcp.setEnabled(true);
        } else if (protocol.equals("udp")) {
            tcp.setChecked(false);
            tcp.setEnabled(false);
        } else {
            tcp.setChecked(true);
            tcp.setEnabled(false);
        }
    }

    private void testCert() {
        String certFileString = pref.getString("certificate_temp", null);
        if (certFileString == null) certFileString = pref.getString(Preferences.STREAM_CERTIFICATE, null);

        String certPassword = pref.getString(Preferences.STREAM_CERTIFICATE_PASSWORD, Preferences.STREAM_CERTIFICATE_PASSWORD_DEFAULT);

        if (certFileString != null) {
            try {
                Log.d(LOGTAG, certFileString);

                FileInputStream certInputStream = new FileInputStream(certFileString);

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(certInputStream, certPassword.toCharArray());

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslctx = SSLContext.getInstance("TLS");
                sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                File certFile = new File(certFileString);
                File filesDir = getContext().getFilesDir();
                File dest = new File(filesDir.getAbsolutePath() + "/" + certFile.getName());

                pref.edit().putString(Preferences.STREAM_CERTIFICATE, dest.getAbsolutePath()).apply();
                pref.edit().putString("certificate_temp", null).apply();

                showNotification(getString(R.string.success), getString(R.string.cert_success));

            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
                e.printStackTrace();

                pref.edit().putString(Preferences.STREAM_CERTIFICATE, null).apply();
                pref.edit().putString("certificate_temp", null).apply();
                new File(certFileString).delete();

                showNotification(getString(R.string.failed_to_open_cert), e.getMessage());
            }
        } else {
            showNotification(getString(R.string.error), getString(R.string.choose_cert));
        }
    }

    private void showNotification(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.d(LOGTAG, "pref change " + key + " " + newValue);
        if (key != null && key.equals(Preferences.STREAM_ADDRESS) && pref.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT).equals("udp")) {
            try {
                Inet4Address address = (Inet4Address) Inet4Address.getByName((String) newValue);
                if (!address.isMulticastAddress()) {
                    Log.e(LOGTAG, "Protocol is UDP but " + address + " is not multicast");
                    showNotification(getString(R.string.error), getString(R.string.invalid_multicast_ip_message));
                    return false;
                }
            } catch (UnknownHostException|NetworkOnMainThreadException e) {
                Log.e(LOGTAG, "Invalid IP address", e);
                showNotification(getString(R.string.error), getString(R.string.invalid_ip));
                return false;
            }
        }

        if (key != null && key.equals(Preferences.STREAM_PROTOCOL)) {
            String audio_codec = pref.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT);
            String protocol = (String) newValue;
            handleTcpSwitch(protocol);

            if (Objects.equals(audio_codec, AudioCodec.G711.name()) && !protocol.equals("srt") && !protocol.equals("udp")) {
                Log.d(LOGTAG, "Audio codec is G711 and protocol is " + protocol);
            } else if (audio_codec.equals(AudioCodec.OPUS.name()) && !protocol.startsWith("rtmp")) {
                Log.d(LOGTAG, "Audio codec is OPUS and protocol is " + protocol);
            } else {
                // Fall back to AAC since all streaming protocol support it
                pref.edit().putString(Preferences.AUDIO_CODEC, AudioCodec.AAC.name()).apply();
                Log.d(LOGTAG, "Setting audio codec to AAC and protocol is " + protocol);
            }
        }
        return true;
    }
}
