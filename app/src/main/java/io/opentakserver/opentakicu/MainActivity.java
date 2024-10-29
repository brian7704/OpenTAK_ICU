package io.opentakserver.opentakicu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.BuildConfig;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pedro.encoder.input.video.CameraHelper;

import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.contants.Preferences;

import com.pedro.library.view.OpenGlView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements Button.OnClickListener, SurfaceHolder.Callback, View.OnTouchListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOGTAG = "MainActivity";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();
    SharedPreferences pref;

    private OpenGlView openGlView;
    private FloatingActionButton bStartStop;

    private TextView tvBitrate;
    private TextView tvLocationFix;
    private TextView tvStreamPath;
    private TextView tvRecording;
    private TextView tvTakServer;
    private FloatingActionButton pictureButton;
    private FloatingActionButton flashlight;
    private View whiteOverlay;

    private boolean service_bound = false;
    private CameraService camera_service;
    private long last_fix_time = 0;
    private FirebaseAnalytics mFirebaseAnalytics;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(CameraService.EXIT_APP)) {
                Log.d(LOGTAG, "Exiting app");
                finishAndRemoveTask();
            } else if (action != null && action.equals(CameraService.START_STREAM)) {
                if (!service_bound) {
                    bindService(new Intent(getApplicationContext(), CameraService.class), mConnection, Context.BIND_IMPORTANT);
                    bStartStop.setImageResource(R.drawable.stop);
                    lockScreenOrientation();
                }
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else if (action != null && (action.equals(CameraService.STOP_STREAM) || action.equals(CameraService.AUTH_ERROR) || action.equals(CameraService.CONNECTION_FAILED))) {
                bStartStop.setImageResource(R.drawable.ic_record);
                if (service_bound)
                    unbindService(mConnection);

                service_bound = false;
                unlockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.no);
                    tvRecording.setTextColor(Color.RED);
                }

                setStatusState();
            } else if (action != null && action.equals(CameraService.TOOK_PICTURE)) {
                MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
            } else if (action != null && action.equals(CameraService.NEW_BITRATE)) {
                long bitrate = intent.getLongExtra(CameraService.NEW_BITRATE, 0) / 1000;
                tvBitrate.setText(bitrate + "kb/s");
            } else if (action != null && action.equals(CameraService.LOCATION_CHANGE)) {
                last_fix_time = System.currentTimeMillis();
                tvLocationFix.setText(R.string.yes);
                tvLocationFix.setTextColor(Color.GREEN);
            } else if (action != null && action.equals(TcpClient.TAK_SERVER_CONNECTED)) {
                tvTakServer.setText(R.string.connected);
                tvTakServer.setTextColor(Color.GREEN);
            } else if (action != null && action.equals(TcpClient.TAK_SERVER_DISCONNECTED)) {
                tvTakServer.setText(R.string.disconnected);
                tvTakServer.setTextColor(Color.RED);
            }
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            camera_service.stopStream(getString(R.string.network_lost), null);
            Toast.makeText(getApplicationContext(), "Network lost, stream stopping", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
    };


    //Suppress this warning for Android versions less than 13
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        pictureButton = findViewById(R.id.pictureButton);
        pictureButton.setOnClickListener(this);

        if (!BuildConfig.DEBUG) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            Bundle bundle = new Bundle();
            bundle.putString("Activity", "MainActivity");
            mFirebaseAnalytics.logEvent("Start", bundle);
        }

        String uid = pref.getString(Preferences.UID, null);
        if (uid == null)
            pref.edit().putString(Preferences.UID, Preferences.UID_DEFAULT).apply();

        permissions();

        openGlView = findViewById(R.id.openGlView);
        openGlView.getHolder().addCallback(this);
        openGlView.setOnTouchListener(this);

        FloatingActionButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this);

        if (!hasPermissions(this, PERMISSIONS)) {
            Intent intent = new Intent(MainActivity.this, OnBoardingActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        tvBitrate = findViewById(R.id.bitrate_value);
        tvLocationFix = findViewById(R.id.location_fix_status);
        tvStreamPath = findViewById(R.id.stream_path_name);
        tvRecording = findViewById(R.id.recording_status);
        tvTakServer = findViewById(R.id.atak_connection_status);

        setStatusState();

        bStartStop = findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);
        FloatingActionButton switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        whiteOverlay = findViewById(R.id.white_color_overlay);

        flashlight = findViewById(R.id.flashlight);
        flashlight.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getApplicationContext(), CameraService.class));
        } else {
            startService(new Intent(this, CameraService.class));
        }
        CameraService.observer.observe(this, cameraService -> {
            Log.d(LOGTAG, "observer");
            camera_service = cameraService;
            if (openGlView.getHolder().getSurface().isValid() && camera_service != null) {
                camera_service.setView(openGlView);
                camera_service.startPreview();
                Log.d(LOGTAG, "Observer started preview");
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CameraService.EXIT_APP);
        intentFilter.addAction(CameraService.START_STREAM);
        intentFilter.addAction(CameraService.STOP_STREAM);
        intentFilter.addAction(CameraService.AUTH_ERROR);
        intentFilter.addAction(CameraService.CONNECTION_FAILED);
        intentFilter.addAction(CameraService.TOOK_PICTURE);
        intentFilter.addAction(CameraService.NEW_BITRATE);
        intentFilter.addAction(CameraService.LOCATION_CHANGE);
        intentFilter.addAction(TcpClient.TAK_SERVER_CONNECTED);
        intentFilter.addAction(TcpClient.TAK_SERVER_DISCONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager connectivityManager = getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);

    }

    private void setStatusState() {
        if (pref.getBoolean(Preferences.ATAK_SEND_COT, Preferences.ATAK_SEND_COT_DEFAULT)) {
            tvLocationFix.setText(R.string.not_streaming);
            tvLocationFix.setTextColor(Color.YELLOW);
            tvTakServer.setText(R.string.not_streaming);
            tvTakServer.setTextColor(Color.YELLOW);
        } else {
            tvLocationFix.setText(R.string.disabled);
            tvLocationFix.setTextColor(Color.YELLOW);
            tvTakServer.setText(R.string.disabled);
            tvTakServer.setTextColor(Color.YELLOW);
        }

        tvStreamPath.setText(pref.getString(Preferences.STREAM_PATH, Preferences.STREAM_PATH_DEFAULT));
        tvBitrate.setText(pref.getString(Preferences.VIDEO_BITRATE, Preferences.VIDEO_BITRATE_DEFAULT) + "kb/s");

        if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
            tvRecording.setText(R.string.not_streaming);
            tvRecording.setTextColor(Color.YELLOW);
        } else {
            tvRecording.setText(R.string.disabled);
            tvRecording.setTextColor(Color.YELLOW);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy");
        if (camera_service != null)
            camera_service.stopPreview();
        stopService(new Intent(this, CameraService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "onPause");
        if (camera_service != null)
            camera_service.stopPreview();
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(LOGTAG, "onServiceConnected");
            service_bound = true;
            bStartStop.setImageResource(R.drawable.stop);
            tvLocationFix.setText(R.string.no);
            tvLocationFix.setTextColor(Color.RED);
            tvTakServer.setText(R.string.no);
            tvTakServer.setTextColor(Color.RED);
            camera_service.startStream();
        }

        public void onServiceDisconnected(ComponentName className) {
            service_bound = false;
        }
    };

    private void lockScreenOrientation() {
        int orientation;
        switch (CameraHelper.getCameraOrientation(this)) {
            case 90:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case 180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case 270:
                orientation =ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                break;
            default:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        Log.d(LOGTAG, "lockScreenOrientation " + orientation);
        this.setRequestedOrientation(orientation);
    }

    private void unlockScreenOrientation() {
        Log.d(LOGTAG, "unlockScreenOrientation");
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void permissions() {
        PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
        PERMISSIONS.add(Manifest.permission.CAMERA);
        PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PERMISSIONS.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private boolean hasPermissions(Context context, ArrayList<String> permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.b_start_stop) {
            if (!service_bound) {
                bindService(new Intent(this, CameraService.class), mConnection, Context.BIND_IMPORTANT);
                bStartStop.setImageResource(R.drawable.stop);
                lockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else {
                bStartStop.setImageResource(R.drawable.ic_record);
                camera_service.stopStream(null, null);
                unbindService(mConnection);
                service_bound = false;
                unlockScreenOrientation();
                setStatusState();
            }
        } else if (id == R.id.switch_camera) {
            camera_service.switchCamera();
        } else if (id == R.id.settingsButton) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.flashlight) {
            if (camera_service.getCamera().isLanternEnabled()) {
                flashlight.setImageResource(R.drawable.flashlight_off);
            } else {
                flashlight.setImageResource(R.drawable.flashlight_on);
            }

            camera_service.toggleLantern();
        } else if (id == R.id.pictureButton) {
            MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.VISIBLE));
            camera_service.take_photo();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE && camera_service != null) {
                camera_service.setZoom(motionEvent);
            }
        } else if (action == MotionEvent.ACTION_DOWN && camera_service != null) {
            camera_service.tapToFocus(motionEvent);
        }
        return true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(LOGTAG, "surfacecreated");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(LOGTAG, "surfacechanged");
        if (camera_service != null && openGlView.getHolder().getSurface().isValid()) {
            camera_service.setView(openGlView);
            camera_service.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(LOGTAG, "surfaceDestroyed");
        if (camera_service != null) {
            camera_service.setView(getApplicationContext());
            camera_service.stopPreview();
        }
    }

    //Handle screen rotation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(LOGTAG, "onConfigChange " + newConfig);
        camera_service.stopPreview();
        camera_service.prepareEncoders();
        camera_service.startPreview();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        setStatusState();
    }
}