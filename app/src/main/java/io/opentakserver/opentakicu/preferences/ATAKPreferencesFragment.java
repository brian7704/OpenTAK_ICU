package io.opentakserver.opentakicu.preferences;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.R;

public class ATAKPreferencesFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceClickListener {
    private final static String LOGTAG = "ATAKPrefsFragment";
    private ActivityResultLauncher trustStoreFileBrowserLauncher;
    private ActivityResultLauncher clientCertFileBrowserLauncher;
    private SharedPreferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        trustStoreFileBrowserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    try {
                        Intent data = result.getData();
                        Log.d(LOGTAG, "Got file: " + data.getData().getPath());

                        InputStream certInputStream = getContext().getContentResolver().openInputStream(data.getData());
                        String certFileName = data.getData().getPath().split(":")[1];
                        Path p = Paths.get(certFileName);
                        File filesDir = getContext().getFilesDir();
                        File dest = new File(filesDir.getAbsolutePath() + "/" + p.getFileName().toString());
                        Log.d(LOGTAG, dest.getAbsolutePath());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            FileUtils.copy(certInputStream, Files.newOutputStream(dest.toPath()));
                        } else {
                            copy(certInputStream, dest);
                        }

                        prefs.edit().putString("trust_store_temp", dest.getAbsolutePath()).apply();
                    } catch (Exception e) {
                        Log.d(LOGTAG, "Failed to get cert: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        );

        clientCertFileBrowserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        try {
                            Intent data = result.getData();
                            Log.d(LOGTAG, "Got file: " + data.getData().getPath());

                            InputStream certInputStream = getContext().getContentResolver().openInputStream(data.getData());
                            String certFileName = data.getData().getPath().split(":")[1];
                            Path p = Paths.get(certFileName);
                            File filesDir = getContext().getFilesDir();
                            File dest = new File(filesDir.getAbsolutePath() + "/" + p.getFileName().toString());

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                FileUtils.copy(certInputStream, Files.newOutputStream(dest.toPath()));
                            } else {
                                copy(certInputStream, dest);
                            }

                            prefs.edit().putString("client_cert_temp", dest.getAbsolutePath()).apply();
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
        setPreferencesFromResource(R.xml.atak_preferences, rootKey);
        findPreference("trust_store_certificate").setOnPreferenceClickListener(this);
        findPreference("test_trust_store").setOnPreferenceClickListener(this);

        findPreference("client_certificate").setOnPreferenceClickListener(this);
        findPreference("test_client_cert").setOnPreferenceClickListener(this);
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

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        if (preference.getKey().equals("trust_store_certificate")) {
            //prefs.edit().putString("trust_store_certificate", null).apply();
            Intent fileBrowserIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileBrowserIntent.setType("*/*");
            trustStoreFileBrowserLauncher.launch(fileBrowserIntent);
            return true;
        } else if (preference.getKey().equals("client_certificate")) {
            //prefs.edit().putString("trust_store_certificate", null).apply();
            Intent fileBrowserIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fileBrowserIntent.setType("*/*");
            clientCertFileBrowserLauncher.launch(fileBrowserIntent);
            return true;
        } else if (preference.getKey().equals("test_trust_store")) {
            testTrustStoreCert();
            return true;
        } else if (preference.getKey().equals("test_client_cert")) {
            testClientCert();
            return true;
        }
        return false;
    }

    private void testTrustStoreCert() {
        String trust_store_temp = prefs.getString("trust_store_temp", null);
        if (trust_store_temp == null) trust_store_temp = prefs.getString("trust_store_certificate", null);

        String trust_store_password = prefs.getString("trust_store_cert_password", "atakatak");

        if (trust_store_temp != null) {
            try {
                FileInputStream certInputStream = new FileInputStream(trust_store_temp);

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(certInputStream, trust_store_password.toCharArray());

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslctx = SSLContext.getInstance("TLS");
                sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                File certFile = new File(trust_store_temp);
                File filesDir = getContext().getFilesDir();
                File dest = new File(filesDir.getAbsolutePath() + "/" + certFile.getName());

                prefs.edit().putString("trust_store_certificate", dest.getAbsolutePath()).apply();
                prefs.edit().putString("trust_store_temp", null).apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.success));
                builder.setMessage(getString(R.string.cert_success));
                builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.create().show();

            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
                e.printStackTrace();

                prefs.edit().putString("trust_store_certificate", null).apply();
                prefs.edit().putString("trust_store_temp", null).apply();
                new File(trust_store_temp).delete();

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

    private void testClientCert() {
        String client_cert_temp = prefs.getString("client_cert_temp", null);
        if (client_cert_temp == null) client_cert_temp = prefs.getString("client_certificate", null);

        String client_cert_password = prefs.getString("client_cert_password", "atakatak");

        if (client_cert_temp != null) {
            try {
                FileInputStream certInputStream = new FileInputStream(client_cert_temp);

                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(certInputStream, client_cert_password.toCharArray());

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslctx = SSLContext.getInstance("TLS");
                sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                File certFile = new File(client_cert_temp);
                File filesDir = getContext().getFilesDir();
                File dest = new File(filesDir.getAbsolutePath() + "/" + certFile.getName());

                prefs.edit().putString("client_certificate", dest.getAbsolutePath()).apply();
                prefs.edit().putString("client_cert_temp", null).apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getString(R.string.success));
                builder.setMessage(getString(R.string.cert_success));
                builder.setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.create().show();

            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
                e.printStackTrace();

                prefs.edit().putString("client_certificate", null).apply();
                prefs.edit().putString("client_cert_temp", null).apply();
                new File(client_cert_temp).delete();

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
}
