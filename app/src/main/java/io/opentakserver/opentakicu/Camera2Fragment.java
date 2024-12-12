package io.opentakserver.opentakicu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.firebase.BuildConfig;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.sources.video.Camera2Source;
import com.pedro.encoder.input.video.CameraHelper;

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.contants.Preferences;

import com.pedro.library.view.OpenGlView;

public class Camera2Fragment extends Fragment
        implements Button.OnClickListener, SurfaceHolder.Callback, View.OnTouchListener,
        SharedPreferences.OnSharedPreferenceChangeListener, ConnectChecker, PopupMenu.OnMenuItemClickListener {

    private final String LOGTAG = "Camera2Fragment";
    SharedPreferences pref;

    private Activity activity;
    private OpenGlView openGlView;
    private FloatingActionButton bStartStop;
    private PopupMenu popupMenu;
    PopupMenuHandler popupMenuHandler;
    private Handler handler = new Handler();

    private TextView tvBitrate;
    private TextView tvLocationFix;
    private TextView tvStreamPath;
    private TextView tvRecording;
    private TextView tvTakServer;
    private FloatingActionButton pictureButton;
    private FloatingActionButton flashlight;
    private View whiteOverlay;
    private FloatingActionButton videoSourceButton;
    private FloatingActionButton switchCameraButton;
    private Slider zoomSlider;

    private boolean service_bound = false;
    private Camera2Service camera_service;
    private long last_fix_time = 0;
    private FirebaseAnalytics mFirebaseAnalytics;

    public Camera2Fragment() {
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

        openGlView = activity.findViewById(R.id.openGlView);
        openGlView.getHolder().addCallback(this);
        openGlView.setOnTouchListener(this);

        tvBitrate = activity.findViewById(R.id.bitrate_value);
        tvLocationFix = activity.findViewById(R.id.location_fix_status);
        tvStreamPath = activity.findViewById(R.id.stream_path_name);
        tvRecording = activity.findViewById(R.id.recording_status);
        tvTakServer = activity.findViewById(R.id.atak_connection_status);

        videoSourceButton = activity.findViewById(R.id.videoSource);
        videoSourceButton.setOnClickListener(this);

        zoomSlider = activity.findViewById(R.id.zoom_slider);
        zoomSlider.setOnTouchListener(this);
        handler.postDelayed(setZoomSliderVisibility, 3000);

        setStatusState();

        bStartStop = activity.findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);
        switchCameraButton = activity.findViewById(R.id.switch_camera);
        switchCameraButton.setOnClickListener(this);
        whiteOverlay = activity.findViewById(R.id.white_color_overlay);

        flashlight = activity.findViewById(R.id.flashlight);
        flashlight.setOnClickListener(this);

        FloatingActionButton settingsButton = activity.findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(this);

        popupMenu = new PopupMenu(activity, videoSourceButton);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Camera2Service.EXIT_APP);
        intentFilter.addAction(Camera2Service.START_STREAM);
        intentFilter.addAction(Camera2Service.STOP_STREAM);
        intentFilter.addAction(Camera2Service.AUTH_ERROR);
        intentFilter.addAction(Camera2Service.CONNECTION_FAILED);
        intentFilter.addAction(Camera2Service.TOOK_PICTURE);
        intentFilter.addAction(Camera2Service.NEW_BITRATE);
        intentFilter.addAction(Camera2Service.LOCATION_CHANGE);
        intentFilter.addAction(TcpClient.TAK_SERVER_CONNECTED);
        intentFilter.addAction(TcpClient.TAK_SERVER_DISCONNECTED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            activity.registerReceiver(receiver, intentFilter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(new Intent(activity, Camera2Service.class));
        } else {
            activity.startService(new Intent(activity, Camera2Service.class));
        }

        Camera2Service.observer.observe(getViewLifecycleOwner(), cameraService -> {
            Log.d(LOGTAG, "observer");
            camera_service = cameraService;
            popupMenuHandler = new PopupMenuHandler(cameraService, getActivity());
            setZoomRange();

            if (openGlView.getHolder().getSurface().isValid() && camera_service != null) {
                camera_service.startPreview(openGlView);
                Log.d(LOGTAG, "Observer started preview");
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d(LOGTAG, "onAttach");

        if (context instanceof Activity) {
            activity = (Activity) context;
        }
    }

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOGTAG, "onReceive " + action);
            if (action != null && action.equals(Camera2Service.EXIT_APP)) {
                Log.d(LOGTAG, "Exiting app");
                activity.finishAndRemoveTask();
            } else if (action != null && action.equals(Camera2Service.START_STREAM)) {
                if (!service_bound) {
                    activity.bindService(new Intent(activity, Camera2Service.class), mConnection, Context.BIND_IMPORTANT);
                    bStartStop.setImageResource(R.drawable.stop);
                    lockScreenOrientation();
                }
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else if (action != null && (action.equals(Camera2Service.STOP_STREAM) || action.equals(Camera2Service.AUTH_ERROR) || action.equals(Camera2Service.CONNECTION_FAILED))) {
                bStartStop.setImageResource(R.drawable.ic_record);
                if (service_bound)
                    activity.unbindService(mConnection);

                service_bound = false;
                unlockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.no);
                    tvRecording.setTextColor(Color.RED);
                }

                setStatusState();
            } else if (action != null && action.equals(Camera2Service.TOOK_PICTURE)) {
                activity.runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
            } else if (action != null && action.equals(Camera2Service.NEW_BITRATE)) {
                long bitrate = intent.getLongExtra(Camera2Service.NEW_BITRATE, 0) / 1000;
                tvBitrate.setText(bitrate + "kb/s");
            } else if (action != null && action.equals(Camera2Service.LOCATION_CHANGE)) {
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
            Toast.makeText(activity, "Network lost, stream stopping", Toast.LENGTH_LONG).show();
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
        pref = PreferenceManager.getDefaultSharedPreferences(activity);
        pref.registerOnSharedPreferenceChangeListener(this);

        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!BuildConfig.DEBUG) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(activity);
            Bundle bundle = new Bundle();
            bundle.putString("Activity", "MainActivity");
            mFirebaseAnalytics.logEvent("Start", bundle);
        }

        String uid = pref.getString(Preferences.UID, null);
        if (uid == null)
            pref.edit().putString(Preferences.UID, Preferences.UID_DEFAULT).apply();

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        ConnectivityManager connectivityManager = activity.getSystemService(ConnectivityManager.class);
        connectivityManager.requestNetwork(networkRequest, networkCallback);
    }

    private void setZoomRange() {
        if (camera_service != null && camera_service.getStream().getVideoSource() instanceof Camera2Source) {
            Camera2Source camera2Source = (Camera2Source) camera_service.getStream().getVideoSource();
            Range<Float> zoomRange = camera2Source.getZoomRange();
            zoomSlider.setValueFrom(zoomRange.getLower());
            zoomSlider.setValueTo(zoomRange.getUpper());
            zoomSlider.setValue(zoomRange.getLower());
        }
    }

    private void setStatusState() {
        if (tvLocationFix == null)
            return;

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
        if (camera_service != null) {
            camera_service.stopPreview();
            camera_service.getStream().release();
        }
        activity.stopService(new Intent(activity, Camera2Service.class));
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
            camera_service.startPreview(openGlView);
            service_bound = true;
            bStartStop.setImageResource(R.drawable.stop);
            tvLocationFix.setText(R.string.no);
            tvLocationFix.setTextColor(Color.RED);
            tvTakServer.setText(R.string.no);
            tvTakServer.setTextColor(Color.RED);
            camera_service.startStream();
            popupMenuHandler.stopClock();
            popupMenuHandler.toggleText();
        }

        public void onServiceDisconnected(ComponentName className) {
            service_bound = false;
        }
    };

    private void lockScreenOrientation() {
        int orientation;
        switch (CameraHelper.getCameraOrientation(activity)) {
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
        activity.setRequestedOrientation(orientation);
    }

    private void unlockScreenOrientation() {
        Log.d(LOGTAG, "unlockScreenOrientation");
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    Runnable setZoomSliderVisibility = new Runnable() {
        @Override
        public void run() {
            zoomSlider.animate().alpha(0f);
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.b_start_stop) {
            if (!service_bound) {
                activity.bindService(new Intent(activity, Camera2Service.class), mConnection, Context.BIND_IMPORTANT);
                bStartStop.setImageResource(R.drawable.stop);
                lockScreenOrientation();
                if (pref.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT)) {
                    tvRecording.setText(R.string.yes);
                    tvRecording.setTextColor(Color.GREEN);
                }
            } else {
                bStartStop.setImageResource(R.drawable.ic_record);
                camera_service.stopStream(null, null);
                activity.unbindService(mConnection);
                service_bound = false;
                unlockScreenOrientation();
                setStatusState();
            }
        } else if (id == R.id.switch_camera) {
            camera_service.switchCamera();
            setZoomRange();
            flashlight.setImageResource(R.drawable.flashlight_off);
        } else if (id == R.id.settingsButton) {
            Intent intent = new Intent(activity, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.flashlight) {
            boolean lanternEnabled = camera_service.toggleLantern();

            if (lanternEnabled) flashlight.setImageResource(R.drawable.flashlight_on);
            else flashlight.setImageResource(R.drawable.flashlight_off);

        } else if (id == R.id.pictureButton) {
            activity.runOnUiThread(() -> whiteOverlay.setVisibility(View.VISIBLE));
            camera_service.take_photo();
        } else if (id == R.id.videoSource) {
            popupMenu.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        return popupMenuHandler.onMenuItemClick(menuItem, flashlight);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            zoomSlider.animate().alpha(1f);
            handler.removeCallbacks(setZoomSliderVisibility);
        }

        if (action == MotionEvent.ACTION_UP) {
            handler.postDelayed(setZoomSliderVisibility, 5000);
            Log.d(LOGTAG, "ACTION UP");
        }

        if (view == zoomSlider && camera_service != null) {
            if (camera_service.getStream().getVideoSource() instanceof Camera2Source) {
                Camera2Source camera2Source = (Camera2Source) camera_service.getStream().getVideoSource();
                camera2Source.setZoom(zoomSlider.getValue());
                return false;
            }
        }
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
        Log.d(LOGTAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(LOGTAG, "surfaceChanged");
        if (camera_service != null && openGlView.getHolder().getSurface().isValid()) {
            Log.i(LOGTAG, "surfacechanged starting preview");
            camera_service.startPreview(openGlView);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        Log.d(LOGTAG, "surfaceDestroyed");
        if (camera_service != null) {
            camera_service.stopPreview();
        }
    }

    //Handle screen rotation
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(LOGTAG, "onConfigChange " + newConfig);
        camera_service.stopPreview();
        camera_service.prepareEncoders();
        camera_service.startPreview(openGlView);

        //When the screen rotates, the timestamp text will disappear but the clock runnable will still run.
        //This stops the clock if it's running and shows the text again if it's enabled.
        popupMenuHandler.stopClock();
        popupMenuHandler.toggleText();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        setStatusState();
    }

    @Override
    public void onAuthError() {

    }

    @Override
    public void onConnectionStarted(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccess() {

    }

    @Override
    public void onConnectionFailed(@NonNull String s) {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onAuthSuccess() {

    }
}