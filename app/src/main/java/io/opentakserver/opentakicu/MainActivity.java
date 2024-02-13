package io.opentakserver.opentakicu;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedro.encoder.input.video.CameraHelper;

import androidx.preference.PreferenceManager;

import com.pedro.library.view.OpenGlView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements Button.OnClickListener, SurfaceHolder.Callback, View.OnTouchListener {

    private final String LOGTAG = "MainActivity";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();
    SharedPreferences pref;

    private OpenGlView openGlView;
    private FloatingActionButton bStartStop;

    private TextView tvBitrate;
    private FloatingActionButton pictureButton;
    private FloatingActionButton flashlight;
    private View whiteOverlay;

    private boolean service_bound = false;
    private CameraService camera_service;

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOGTAG, "Got broadcast: " + intent);
            if (action != null && action.equals(CameraService.EXIT_APP)) {
                Log.d(LOGTAG, "Exiting app");
                finishAndRemoveTask();
            } else if (action != null && action.equals(CameraService.START_STREAM)) {
                if (!service_bound) {
                    bindService(new Intent(getApplicationContext(), CameraService.class), mConnection, Context.BIND_IMPORTANT);
                    bStartStop.setImageResource(R.drawable.stop);
                    lockScreenOrientation();
                }
            } else if (action != null && (action.equals(CameraService.STOP_STREAM) || action.equals(CameraService.AUTH_ERROR))) {
                bStartStop.setImageResource(R.drawable.ic_record);
                if (camera_service != null)
                    camera_service.stopStream();
                unbindService(mConnection);
                service_bound = false;
                unlockScreenOrientation();
            } else if (action != null && action.equals(CameraService.TOOK_PICTURE)) {
                MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
            } else if (action != null && action.equals(CameraService.NEW_BITRATE)) {
                long bitrate = intent.getLongExtra(CameraService.NEW_BITRATE, 0) / 1000;
                tvBitrate.setText(bitrate + "kb/s");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        pictureButton = findViewById(R.id.pictureButton);
        pictureButton.setOnClickListener(this);

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
        }

        tvBitrate = findViewById(R.id.tv_bitrate);
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
        intentFilter.addAction(CameraService.TOOK_PICTURE);
        intentFilter.addAction(CameraService.NEW_BITRATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
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
        this.setRequestedOrientation(orientation);
    }

    private void unlockScreenOrientation() {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void permissions() {
        PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
        PERMISSIONS.add(Manifest.permission.CAMERA);
        PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
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
            } else {
                bStartStop.setImageResource(R.drawable.ic_record);
                camera_service.stopStream();
                unbindService(mConnection);
                service_bound = false;
                unlockScreenOrientation();
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
            camera_service.setView(this);
            camera_service.stopPreview();
        }
    }
}