package io.opentakserver.opentakicu;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.pedro.common.ConnectChecker;
import com.pedro.library.rtsp.RtspCamera2;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

public class SettingsFragment extends PreferenceFragmentCompat implements ConnectChecker, Preference.OnPreferenceClickListener {
    private static String LOGTAG = "SettingsFragment";
    private RtspCamera2 rtspCamera2;
    private ActivityResultLauncher fileBrowserLauncher;
    private SharedPreferences pref;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pref = PreferenceManager.getDefaultSharedPreferences(getContext());

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
        setPreferencesFromResource(R.xml.preferences, rootKey);
        rtspCamera2 = new RtspCamera2(getActivity(), false, this);
        ListPreference resolutions = findPreference("resolution");
        ArrayList<String> resolutionsList = new ArrayList<>();
        ArrayList<String> resolutionsInts = new ArrayList<>();
        int x = 0;
        List<Size> frontResolutions = rtspCamera2.getResolutionsFront();

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

        findPreference("certificate").setOnPreferenceClickListener(this);
        findPreference("test_certificate").setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals("certificate")) {
            pref.edit().putString("certificate", null).apply();
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

    private void testCert() {
        String certFileString = pref.getString("certificate_temp", null);
        if (certFileString == null) certFileString = pref.getString("certificate", null);

        String certPassword = pref.getString("certificate_password", "");

        Log.d(LOGTAG, certFileString + " " + certPassword);

        if (certFileString != null) {
            try {
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

                pref.edit().putString("certificate", dest.getAbsolutePath()).apply();
                pref.edit().putString("certificate_temp", null).apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.success));
                builder.setMessage(getString(R.string.cert_success));
                builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.create().show();

            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
                e.printStackTrace();

                pref.edit().putString("certificate", null).apply();
                pref.edit().putString("certificate_temp", null).apply();
                new File(certFileString).delete();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.failed_to_open_cert));
                builder.setMessage(e.getMessage());
                builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
                builder.create().show();
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.error));
            builder.setMessage(getString(R.string.choose_cert));
            builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.create().show();
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
}
