package io.opentakserver.opentakicu;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedro.common.AudioCodec;
import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.encoder.input.video.Camera2ApiManager;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;

import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.utils.PathUtils;

import com.pedro.library.base.Camera2Base;
import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.library.rtsp.RtspCamera2;
import com.pedro.library.srt.SrtCamera2;
import com.pedro.library.util.BitrateAdapter;
import com.pedro.library.view.OpenGlView;
import com.pedro.library.view.TakePhotoCallback;
import com.pedro.rtsp.rtsp.Protocol;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity
        implements Button.OnClickListener, ConnectChecker, SurfaceHolder.Callback,
        View.OnTouchListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String LOGTAG = "MainActivity";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();
    SharedPreferences pref;

    private Camera2Base camera2Base;
    private RtspCamera2 rtspCamera2;
    private RtmpCamera2 rtmpCamera2;
    private SrtCamera2 srtCamera2;
    private OpenGlView openGlView;
    private FloatingActionButton bStartStop;
    private String currentDateAndTime = "";
    private File folder;
    //options menu
    private TextView tvBitrate;
    private FloatingActionButton pictureButton;
    private FloatingActionButton flashlight;
    private View whiteOverlay;

    private String protocol;
    private String address;
    private int port;
    private String path;
    private String username;
    private String password;
    private int samplerate;
    private boolean stereo;
    private boolean echo_cancel;
    private boolean noise_reduction;
    private int fps;
    private Size resolution;
    private boolean adaptive_bitrate;
    private boolean record;
    private boolean stream;
    private boolean enable_audio;
    private int bitrate;
    private int audio_bitrate;
    private String audio_codec;
    private String codec;

    private BitrateAdapter bitrateAdapter;
    private Context context;

    private float mPreviousBrightness = -1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        folder = PathUtils.getRecordPath();
        pictureButton = findViewById(R.id.pictureButton);
        pictureButton.setOnClickListener(this);

        permissions();
        pref.registerOnSharedPreferenceChangeListener(this);

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

        getSettings();

        tvBitrate = findViewById(R.id.tv_bitrate);
        bStartStop = findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);
        FloatingActionButton switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
        whiteOverlay = findViewById(R.id.white_color_overlay);

        flashlight = findViewById(R.id.flashlight);
        flashlight.setOnClickListener(this);

        prepareEncoders();
        context = this;
    }

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
            if (!camera2Base.isStreaming() && !camera2Base.isRecording()) {
                bStartStop.setImageResource(R.drawable.stop);

                if (Objects.equals(protocol, "rtsp") && pref.getBoolean("tcp", true)) {
                    rtspCamera2.getStreamClient().setProtocol(Protocol.TCP);
                } else if (Objects.equals(protocol, "rtsp")){
                    rtspCamera2.getStreamClient().setProtocol(Protocol.UDP);
                }

                if (camera2Base.isRecording() || prepareEncoders()) {

                    if (!protocol.equals("srt") && !username.isEmpty() && !password.isEmpty()) {
                         camera2Base.getStreamClient().setAuthorization(username, password);
                         Log.d(LOGTAG, "set auth " + username + " " + password);
                    }
                    else {
                        Log.d(LOGTAG, "NO AUTH");
                    }

                    String url = protocol + "://" + address + ":" + port + "/" + path;
                    Log.d(LOGTAG, url);

                    if (!camera2Base.isAutoFocusEnabled())
                        camera2Base.enableAutoFocus();

                    if (stream) {
                        camera2Base.startStream(url);
                        Log.d(LOGTAG, "Started stream to " + url);
                    }

                    lockScreenOrientation();
                    startRecording();
                } else {
                    //If you see this all time when you start stream,
                    //it is because your encoder device doesn't support the configuration
                    //in video encoder maybe color format.
                    //If you have more encoder go to VideoEncoder or AudioEncoder class,
                    //change encoder and try
                    Toast.makeText(this, "Error preparing stream, This device cant do it",
                            Toast.LENGTH_SHORT).show();
                    bStartStop.setImageResource(R.drawable.ic_record);
                }
            } else {
                bStartStop.setImageResource(R.drawable.ic_record);
                if (camera2Base.isStreaming())
                    camera2Base.stopStream();
                stopRecording();
                unlockScreenOrientation();
            }
        } else if (id == R.id.switch_camera) {
            try {
                camera2Base.switchCamera();
            } catch (CameraOpenException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            //options menu
        } else if (id == R.id.settingsButton) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.flashlight) {
            if (camera2Base.isLanternEnabled()) {
                camera2Base.disableLantern();
                flashlight.setImageResource(R.drawable.flashlight_off);
            }
            else if (camera2Base.isLanternSupported()){
                try {
                    camera2Base.enableLantern();
                    flashlight.setImageResource(R.drawable.flashlight_on);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (id == R.id.pictureButton) {
            rtspCamera2.getGlInterface().takePhoto(new TakePhotoCallback() {
                @Override
                public void onTakePhoto(Bitmap bitmap) {
                    MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.VISIBLE));
                    HandlerThread handlerThread = new HandlerThread("HandlerThreadName");
                    handlerThread.start();
                    Looper looper = handlerThread.getLooper();
                    Handler handler = new Handler(looper);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                long start = System.currentTimeMillis();

                                String filename = "OpenTAKICU_" + System.currentTimeMillis();

                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                    MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, filename, "image:"+filename);
                                    MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
                                    MainActivity.this.runOnUiThread(() -> Toast.makeText(context, "Saved photo", Toast.LENGTH_SHORT).show());
                                } else {
                                    boolean savedSuccessfully;
                                    OutputStream fos;
                                    ContentResolver resolver =  context.getContentResolver();
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/OpenTAKICU");
                                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                                    fos = resolver.openOutputStream(imageUri);

                                    MainActivity.this.runOnUiThread(() -> whiteOverlay.setVisibility(View.INVISIBLE));
                                    savedSuccessfully = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                    fos.flush();
                                    fos.close();

                                    if (savedSuccessfully) {
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(context, "Saved photo", Toast.LENGTH_SHORT).show());
                                    } else {
                                        Log.d(LOGTAG, "Failed to save photo");
                                        MainActivity.this.runOnUiThread(() -> Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show());
                                    }
                                }
                            } catch (NullPointerException | IOException e) {
                                    Log.d(LOGTAG, "Failed to save photo: " + e.getMessage());
                                    MainActivity.this.runOnUiThread(() -> Toast.makeText(context, "Failed to save photo: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                }
            });
        }
    }

    private void startRecording() {
        if (record) {
            try {
                if (!folder.exists()) {
                    Log.d(LOGTAG, "Trying to make folder " + folder.mkdir());
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                currentDateAndTime = sdf.format(new Date());
                if (!camera2Base.isStreaming()) {
                    if (prepareEncoders()) {
                        camera2Base.startRecord(
                                folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                        lockScreenOrientation();
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error preparing stream, This device cant do it",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    camera2Base.startRecord(
                            folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                    lockScreenOrientation();
                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                camera2Base.stopRecord();
                unlockScreenOrientation();
                PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        if (camera2Base.isRecording()) {
            camera2Base.stopRecord();
            unlockScreenOrientation();
            PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean prepareEncoders() {
        Log.d(LOGTAG, "prepareEncoders");
        int width = resolution.getWidth();
        int height = resolution.getHeight();
        fps = Integer.parseInt(pref.getString("fps", "30"));
        bitrate = Integer.parseInt(pref.getString("bitrate", "1000"));
        audio_bitrate = Integer.parseInt(pref.getString("audioBitrate", "128"));
        samplerate = Integer.parseInt(pref.getString("samplerate", "44100"));
        stereo = pref.getBoolean("stereo", true);
        echo_cancel = pref.getBoolean("echo_cancel", true);
        noise_reduction = pref.getBoolean("noise_reduction", true);

        if (Objects.equals(codec, "H264"))
            camera2Base.setVideoCodec(VideoCodec.H264);
        else if (Objects.equals(codec, "H265"))
            camera2Base.setVideoCodec(VideoCodec.H265);
        else if (Objects.equals(codec, "AV1"))
            camera2Base.setVideoCodec(VideoCodec.AV1);


        if (Objects.equals(audio_codec, "G711"))
            try {
                camera2Base.setAudioCodec(AudioCodec.G711);
                Log.d(LOGTAG, "Set audio codec to G711");
            } catch (RuntimeException e) {
                camera2Base.setAudioCodec(AudioCodec.AAC);
                Log.d(LOGTAG, "Failed to set audio codec to  G711, falling back to AAC");
            }
        else {
            camera2Base.setAudioCodec(AudioCodec.AAC);
            Log.d(LOGTAG, "Set audio codec to AAC");
        }

        Log.d(LOGTAG, "Setting bitrate to " + bitrate);
        Log.d(LOGTAG, "Setting res to " + width + " x " + height);

        boolean prepareVideo = camera2Base.prepareVideo(width, height, fps,
                bitrate * 1024,
                CameraHelper.getCameraOrientation(this));


        boolean prepareAudio = camera2Base.prepareAudio(
                    audio_bitrate * 1024,
                    samplerate,
                    stereo,
                    echo_cancel,
                    noise_reduction);

        if (!enable_audio) {
            Log.d(LOGTAG, "disabling audio");
            camera2Base.disableAudio();
        } else {
            camera2Base.enableAudio();
            Log.d(LOGTAG, "enabling audio");
        }

        Log.d(LOGTAG, "PrepareVideo: " + prepareVideo + " Audio " + prepareAudio);
        return prepareVideo && prepareAudio;
    }

    @Override
    public void onConnectionStarted(@NotNull String rtspUrl) {
    }

    @Override
    public void onConnectionSuccess() {
        Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
        if (adaptive_bitrate) {
            tvBitrate.setText("ADAPTIVE ENABLED");
            bitrateAdapter = new BitrateAdapter(new BitrateAdapter.Listener() {
                @Override
                public void onBitrateAdapted(int bitrate) {
                    camera2Base.setVideoBitrateOnFly(bitrate);
                    Log.d(LOGTAG, "Set bitrate to " + bitrate);
                    tvBitrate.setText(bitrate + " bps");
                }
            });
            bitrateAdapter.setMaxBitrate(camera2Base.getBitrate());
        }
        else tvBitrate.setText("ADAPTIVE SHIT NOT ENABLED");
    }

    @Override
    public void onConnectionFailed(@NonNull final String reason) {
        Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                .show();
        camera2Base.stopStream();
        stopRecording();
        bStartStop.setImageResource(R.drawable.ic_record);
    }

    @Override
    public void onNewBitrate(final long bitrate) {
        if (bitrateAdapter != null) {
            bitrateAdapter.adaptBitrate(bitrate, camera2Base.getStreamClient().hasCongestion());
        }
    }

    @Override
    public void onDisconnect() {
        bStartStop.setImageResource(R.drawable.ic_record);
        Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthError() {
        bStartStop.setImageResource(R.drawable.ic_record);
        camera2Base.stopStream();
        stopRecording();
        Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthSuccess() {
        Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE) {
                camera2Base.setZoom(motionEvent);
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            camera2Base.tapToFocus(motionEvent);
        }
        return true;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(LOGTAG, "Setting preview to " + width + " x " + height + " " + format);
        camera2Base.startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        if (camera2Base.isRecording()) {
            camera2Base.stopRecord();
            PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            Toast.makeText(this,
                    "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                    Toast.LENGTH_SHORT).show();
            currentDateAndTime = "";
        }
        if (camera2Base.isStreaming()) {
            camera2Base.stopStream();
            bStartStop.setImageResource(R.drawable.ic_record);
        }
        unlockScreenOrientation();
        camera2Base.stopPreview();
    }

    private void getSettings() {
        protocol = pref.getString("protocol", "rtsp");

        if (protocol.equals("srt")) {
            srtCamera2 = new SrtCamera2(openGlView, this);
            camera2Base = srtCamera2;
            rtspCamera2 = null;
            rtmpCamera2 = null;
        }
        else if (protocol.startsWith("rtmp")) {
            rtmpCamera2 = new RtmpCamera2(openGlView, this);
            camera2Base = rtmpCamera2;
            rtspCamera2 = null;
            srtCamera2 = null;

        }
        else {
            rtspCamera2 = new RtspCamera2(openGlView, this);
            camera2Base = rtspCamera2;
            rtmpCamera2 = null;
            srtCamera2 = null;
        }

        stream = pref.getBoolean("stream_video", true);
        enable_audio = pref.getBoolean("enable_audio", true);
        address = pref.getString("address", "192.168.1.10");
        port = Integer.parseInt(pref.getString("port", "8554"));
        path = pref.getString("path", "stream");
        username = pref.getString("username", "administrator");
        password = pref.getString("password", "password");
        samplerate = Integer.parseInt(pref.getString("samplerate", "44100"));
        stereo = pref.getBoolean("stereo", true);
        echo_cancel = pref.getBoolean("echo_cancel", true);
        noise_reduction = pref.getBoolean("noise_reduction", true);
        fps = Integer.parseInt(pref.getString("fps", "30"));
        record = pref.getBoolean("record", false);
        codec = pref.getString("codec", "H264");
        audio_codec = pref.getString("audio_codec", "AAC");
        adaptive_bitrate = pref.getBoolean("adaptive_bitrate", true);
        getResolution();
        prepareEncoders();
    }

    private void getResolution() {
        ArrayList<Size> resolutions = new ArrayList<>();
        List<Size> frontResolutions = camera2Base.getResolutionsFront();

        // Only get resolutions supported by both cameras
        for (Size res : camera2Base.getResolutionsBack()) {
            if (frontResolutions.contains(res)) {
                resolutions.add(res);
            }
        }

        resolution = resolutions.get(Integer.parseInt(pref.getString("resolution", "0")));
        Log.d(LOGTAG, "getResultion " + resolution.getWidth() + " x " + resolution.getHeight());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        Log.d(LOGTAG, "onSharedPreferenceChange " + s);
        getSettings();
    }
}