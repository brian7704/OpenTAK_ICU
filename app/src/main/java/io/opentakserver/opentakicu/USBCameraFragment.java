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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.BuildConfig;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pedro.encoder.input.video.CameraHelper;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.contants.Preferences;

import com.pedro.library.view.OpenGlView;

import java.util.ArrayList;

public class USBCameraFragment extends Fragment
        implements Button.OnClickListener, SurfaceHolder.Callback, View.OnTouchListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOGTAG = "USBCameraFragment";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();
    SharedPreferences pref;

    private OpenGlView openGlView;
    private FloatingActionButton bStartStop;

    private View whiteOverlay;
    private TextView tvBitrate;
    private TextView tvLocationFix;
    private TextView tvStreamPath;
    private TextView tvRecording;
    private TextView tvTakServer;
    private FloatingActionButton pictureButton;

    private boolean service_bound = false;
    private USBCameraService camera_service;
    private FirebaseAnalytics mFirebaseAnalytics;

    public USBCameraFragment() {
        super(R.layout.camera2_fragment);
        Log.d(LOGTAG, "constructor");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceBundle) {
        Log.d(LOGTAG, "onCreateView");
        return inflater.inflate(R.layout.camera2_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(LOGTAG, "onViewCreated");

        pictureButton = view.findViewById(R.id.pictureButton);
        pictureButton.setOnClickListener(this);

        openGlView = getActivity().findViewById(R.id.openGlView);
        openGlView.getHolder().addCallback(this);
        openGlView.setOnTouchListener(this);

        tvBitrate = getActivity().findViewById(R.id.bitrate_value);
        tvLocationFix = getActivity().findViewById(R.id.location_fix_status);
        tvStreamPath = getActivity().findViewById(R.id.stream_path_name);
        tvRecording = getActivity().findViewById(R.id.recording_status);
        tvTakServer = getActivity().findViewById(R.id.atak_connection_status);

        setStatusState();

        bStartStop = getActivity().findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);

        FloatingActionButton switchCamera = getActivity().findViewById(R.id.switch_camera);
        switchCamera.setVisibility(View.GONE);

        FloatingActionButton flashlight = getActivity().findViewById(R.id.flashlight);
        flashlight.setVisibility(View.GONE);

        whiteOverlay = getActivity().findViewById(R.id.white_color_overlay);

        FloatingActionButton settingsButton = getActivity().findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(USBCameraService.EXIT_APP);
        intentFilter.addAction(USBCameraService.START_STREAM);
        intentFilter.addAction(USBCameraService.STOP_STREAM);
        intentFilter.addAction(USBCameraService.AUTH_ERROR);
        intentFilter.addAction(USBCameraService.CONNECTION_FAILED);
        intentFilter.addAction(USBCameraService.TOOK_PICTURE);
        intentFilter.addAction(USBCameraService.NEW_BITRATE);
        intentFilter.addAction(USBCameraService.LOCATION_CHANGE);
        intentFilter.addAction(TcpClient.TAK_SERVER_CONNECTED);
        intentFilter.addAction(TcpClient.TAK_SERVER_DISCONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getActivity().registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            getActivity().registerReceiver(receiver, intentFilter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(LOGTAG, "STARTING SERVICE");
            getActivity().startForegroundService(new Intent(getActivity(), USBCameraService.class));
        } else {
            getActivity().startService(new Intent(getActivity(), USBCameraService.class));
        }
        USBCameraService.observer.observe(getViewLifecycleOwner(), cameraService -> {
            Log.d(LOGTAG, "observer");
            camera_service = cameraService;

            if (openGlView.getHolder().getSurface().isValid() && camera_service != null && !camera_service.getStream().isOnPreview()) {
                camera_service.setView(openGlView);
                camera_service.startPreview();
                Log.d(LOGTAG, "Observer started preview");
            }
        });
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOGTAG, "onReceive " + action);
            if (action != null && action.equals(USBCameraService.EXIT_APP)) {
                Log.d(LOGTAG, "Exiting app");
                getActivity().finishAndRemoveTask();
            } else if (action != null && action.equals(USBCameraService.START_STREAM)) {
                if (!service_bound) {
                    getActivity().bindService(new Intent(getContext(), USBCameraService.class), mConnection, Context.BIND_IMPORTANT);
                    bStartStop.setImageResource(R.drawable.stop);
                    lockScreenOrientation();
                }
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else if (action != null && (action.equals(USBCameraService.STOP_STREAM) || action.equals(USBCameraService.AUTH_ERROR) || action.equals(USBCameraService.CONNECTION_FAILED))) {
                bStartStop.setImageResource(R.drawable.ic_record);
                if (service_bound)
                    getActivity().unbindService(mConnection);

                service_bound = false;
                unlockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.no);
                    tvRecording.setTextColor(Color.RED);
                }

                setStatusState();
            } else if (action != null && action.equals(USBCameraService.TOOK_PICTURE)) {
                getActivity().runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
            } else if (action != null && action.equals(USBCameraService.NEW_BITRATE)) {
                long bitrate = intent.getLongExtra(USBCameraService.NEW_BITRATE, 0) / 1000;
                tvBitrate.setText(bitrate + "kb/s");
            } else if (action != null && action.equals(USBCameraService.LOCATION_CHANGE)) {
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
            Toast.makeText(getContext(), "Network lost, stream stopping", Toast.LENGTH_LONG).show();
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
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        pref.registerOnSharedPreferenceChangeListener(this);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!BuildConfig.DEBUG) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
            Bundle bundle = new Bundle();
            bundle.putString("Activity", "MainActivity");
            mFirebaseAnalytics.logEvent("Start", bundle);
        }

        String uid = pref.getString(Preferences.UID, null);
        if (uid == null)
            pref.edit().putString(Preferences.UID, Preferences.UID_DEFAULT).apply();

        permissions();

        if (!hasPermissions(getContext(), PERMISSIONS)) {
            Intent intent = new Intent(getContext(), OnBoardingActivity.class);
            startActivity(intent);
            getActivity().finish();
            return;
        }

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager connectivityManager = getActivity().getSystemService(ConnectivityManager.class);
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
        getActivity().stopService(new Intent(getContext(), USBCameraService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOGTAG, "onPause");
        if (camera_service != null)
            camera_service.stopPreview();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (camera_service != null) {
            Log.d(LOGTAG, "onResume starting preview");
            camera_service.startPreview();
        } else {
            Log.d(LOGTAG, "onResume says you're fucked");
        }
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
        switch (CameraHelper.getCameraOrientation(getContext())) {
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
        getActivity().setRequestedOrientation(orientation);
    }

    private void unlockScreenOrientation() {
        Log.d(LOGTAG, "unlockScreenOrientation");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
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
                getActivity().bindService(new Intent(getContext(), USBCameraService.class), mConnection, Context.BIND_IMPORTANT);
                bStartStop.setImageResource(R.drawable.stop);
                lockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else {
                bStartStop.setImageResource(R.drawable.ic_record);
                camera_service.stopStream(null, null);
                getActivity().unbindService(mConnection);
                service_bound = false;
                unlockScreenOrientation();
                setStatusState();
            }
        } else if (id == R.id.settingsButton) {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.pictureButton) {
            getActivity().runOnUiThread(() -> whiteOverlay.setVisibility(View.VISIBLE));
            camera_service.take_photo();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE && camera_service != null) {
                //camera_service.setZoom(motionEvent);
            }
        } else if (action == MotionEvent.ACTION_DOWN && camera_service != null) {
            //camera_service.tapToFocus(motionEvent);
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
        if (camera_service != null && !camera_service.getStream().isOnPreview() && openGlView.getHolder().getSurface().isValid()) {
            camera_service.setView(openGlView);
            camera_service.startPreview();
        } else {
            Log.d(LOGTAG, "Fuck preview, cam service: " + camera_service + " " + openGlView.getHolder().getSurface().isValid());
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(LOGTAG, "surfaceDestroyed");
        if (camera_service != null) {
            camera_service.setView(getContext());
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