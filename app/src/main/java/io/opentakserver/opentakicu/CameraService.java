package io.opentakserver.opentakicu;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.cot.ConnectionEntry;
import io.opentakserver.opentakicu.cot.Contact;
import io.opentakserver.opentakicu.cot.Detail;
import io.opentakserver.opentakicu.cot.Device;
import io.opentakserver.opentakicu.cot.Takv;
import io.opentakserver.opentakicu.cot.feed;
import io.opentakserver.opentakicu.cot.videoConnections;
import io.opentakserver.opentakicu.cot.event;
import io.opentakserver.opentakicu.cot.Point;
import io.opentakserver.opentakicu.cot.Sensor;
import io.opentakserver.opentakicu.cot.__Video;
import io.opentakserver.opentakicu.utils.PathUtils;
import kotlin.NotImplementedError;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pedro.common.AudioCodec;
import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.encoder.input.video.CameraCallbacks;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.utils.CodecUtil;
import com.pedro.library.base.Camera2Base;
import com.pedro.library.generic.GenericCamera2;
import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.library.rtsp.RtspCamera2;
import com.pedro.library.srt.SrtCamera2;
import com.pedro.library.util.BitrateAdapter;
import com.pedro.library.view.OpenGlView;
import com.pedro.rtsp.rtsp.Protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class CameraService extends Service implements ConnectChecker,
        SharedPreferences.OnSharedPreferenceChangeListener, SensorEventListener {
    static final String START_STREAM = "start_stream";
    static final String STOP_STREAM = "stop_stream";
    static final String EXIT_APP = "exit_app";
    static final String AUTH_ERROR = "auth_error";
    static final String CONNECTION_FAILED = "connection_failed";
    static final String TOOK_PICTURE = "took_picture";
    static final String NEW_BITRATE = "new_bitrate";
    static final String LOCATION_CHANGE = "location_change";

    private NotificationManager notificationManager;
    private static final String LOGTAG = "CameraService";
    private final String channelId = "CameraServiceChannel";
    private final int notifyId = 3425;
    private SharedPreferences preferences;

    private RtspCamera2 rtspCamera2;
    private RtmpCamera2 rtmpCamera2;
    private SrtCamera2 srtCamera2;
    private GenericCamera2 genericCamera2;
    private BitrateAdapter bitrateAdapter;

    private String protocol;
    private String address;
    private int port;
    private String path;
    private boolean tcp;
    private String username;
    private String password;
    private String cert_file;
    private String cert_password;
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
    private String uid;
    private double horizonalFov;
    private double verticalFov;

    private boolean send_cot = false;
    private String atak_address;
    private long last_fix_time = 0;

    private SensorManager sensorManager;
    private android.hardware.Sensor magnetometer;
    private android.hardware.Sensor accelerometer;
    private float[] gravityData = new float[3];
    private float[] geomagneticData  = new float[3];
    private boolean hasGravityData = false;
    private boolean hasGeomagneticData = false;
    private double rotationInDegrees;

    private boolean exiting = false;
    private final IBinder binder = new LocalBinder();

    private File folder;
    private String currentDateAndTime;
    public static MutableLiveData<CameraService> observer = new MutableLiveData<>();

    private LocationListener _locListener;
    private LocationManager _locManager;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private TcpClient tcpClient;
    private Thread tcpClientThread;
    private MulticastClient multicastClient;
    private Thread multicastThread;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case START_STREAM:
                        startStream();
                        break;
                    case STOP_STREAM:
                        stopStream(null, null);
                        break;
                    case EXIT_APP:
                        exiting = true;
                        stopStream(null, null);
                        stopSelf();
                        break;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "onCreate");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        folder = PathUtils.getRecordPath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, LOGTAG, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = showNotification(getString(R.string.ready_to_stream));

        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            startForeground(notifyId, notification, type);
        } else {
            startForeground(notifyId, notification);
        }

        getSettings();
        startPreview();
        observer.postValue(this);

        // Setup broadcast receiver for action in the notification
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(START_STREAM);
        intentFilter.addAction(STOP_STREAM);
        intentFilter.addAction(EXIT_APP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(LOGTAG, "Setting up receiver");
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }

        _locListener = new ICULocationListener();
        _locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);

        getCamera().setCameraCallbacks(new CameraCallbacks() {
            @Override
            public void onCameraChanged(@NonNull CameraHelper.Facing facing) {
                CameraCharacteristics cameraCharacteristics = getCamera().getCameraCharacteristics();
                float[] maxFocus = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF size = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                float w = size.getWidth();
                float h = size.getHeight();
                horizonalFov = (2*Math.atan(w/(maxFocus[0]*2))) * 180/Math.PI;
                verticalFov = (2*Math.atan(h/(maxFocus[0]*2))) * 180/Math.PI;
                Log.d(LOGTAG, "horizontalFov = " + horizonalFov);
                Log.d(LOGTAG, "verticalFov = " + verticalFov);
            }

            @Override
            public void onCameraError(@NonNull String s) {

            }

            @Override
            public void onCameraOpened() {

            }

            @Override
            public void onCameraDisconnected() {

            }
        });
    }

    private NotificationCompat.Action startStreamAction() {
        Intent start_streaming = new Intent();
        start_streaming.setAction(START_STREAM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, start_streaming, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action(R.drawable.ic_record, getString(R.string.start_stream), pendingIntent);
    }

    private NotificationCompat.Action stopStreamAction() {
        Intent stop = new Intent();
        stop.setAction(STOP_STREAM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, stop, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Action(R.drawable.stop, getString(R.string.stop_stream), pendingIntent);
    }

    public Notification showNotification(String content) {
        if (exiting)
            return null;

        // Always show the exit app button in the notification
        Intent exitIntent = new Intent();
        exitIntent.setAction(EXIT_APP);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 69, exitIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Action exit = new NotificationCompat.Action(R.drawable.icon_microphone_off, getString(R.string.exit), pendingIntent);

        // Bring MainActivity to the screen when the notification is pressed
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent launchActivity = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setContentTitle("OpenTAK ICU")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchActivity)
                .setContentText(content);

        // Show start/stop stream button in notification
        if (getCamera() != null && getCamera().isStreaming()) {
            notificationBuilder.addAction(stopStreamAction());
        } else {
            notificationBuilder.addAction(startStreamAction());
        }

        notificationBuilder.addAction(exit);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(notifyId, notification);

        return notification;
    }

    public void startPreview() {
        if (!getCamera().isOnPreview()) {
            Log.d(LOGTAG, "Starting Preview");
            getCamera().startPreview();
        }
    }

    public void stopPreview() {
        if (getCamera().isOnPreview()) {
            Log.d(LOGTAG, "Stopping Preview");
            getCamera().stopPreview();
        }
    }

    public void setView(OpenGlView openGlView) {
        getCamera().replaceView(openGlView);
    }

    public void setView(Context context) {
        getCamera().replaceView(context);
    }

    public void toggleLantern() {
        try {
            if (getCamera().isLanternSupported() && getCamera().isLanternEnabled())
                getCamera().disableLantern();
            else if (getCamera().isLanternSupported() && !getCamera().isLanternEnabled())
                getCamera().enableLantern();
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to toggle lantern: " + e.getMessage());
        }
    }

    public void switchCamera() {
        getCamera().switchCamera();
    }

    public void setZoom(MotionEvent motionEvent) {
        getCamera().setZoom(motionEvent);
    }

    public void tapToFocus(MotionEvent motionEvent) {
        getCamera().tapToFocus(motionEvent);
    }

    @Override
    public void onDestroy() {
        observer.postValue(null);
        unregisterReceiver(receiver);
        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int start_id) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()){
            case android.hardware.Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(sensorEvent.values, 0, gravityData, 0, 3);
                hasGravityData = true;
                break;
            case android.hardware.Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, geomagneticData, 0, 3);
                hasGeomagneticData = true;
                break;
            default:
                return;
        }

        if (hasGravityData && hasGeomagneticData) {
            float identityMatrix[] = new float[9];
            float rotationMatrix[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix,
                    gravityData, geomagneticData);

            if (success) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                float rotationInRadians = orientationMatrix[0];
                rotationInDegrees = Math.toDegrees(rotationInRadians);

                WindowManager windowService = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                int rotation =  windowService.getDefaultDisplay().getRotation();
                int screen_orientation;
                switch (rotation) {
                    case Surface.ROTATION_90:
                        screen_orientation = 90;
                        break;
                    case Surface.ROTATION_180:
                        screen_orientation = -180;
                        break;
                    case Surface.ROTATION_270:
                        screen_orientation = -90;
                        break;
                    default:
                        screen_orientation = 0;
                        break;
                }

                rotationInDegrees += screen_orientation;

                if (rotationInDegrees < 0.0f) {
                    rotationInDegrees += 360.0f;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(android.hardware.Sensor sensor, int i) {

    }

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOGTAG, "onBind");
        return binder;
    }


    @Override
    public void onAuthError() {
        Log.d(LOGTAG, "Auth error");
        stopStream(getString(R.string.auth_error), AUTH_ERROR);
    }

    @Override
    public void onAuthSuccess() {
        Log.d(LOGTAG, "Auth success");
    }

    @Override
    public void onConnectionFailed(@NonNull String reason) {
        Log.e(LOGTAG, "Connection failed: ".concat(reason));
        stopStream(getString(R.string.connection_failed) + ": " + reason, CONNECTION_FAILED);
    }

    @Override
    public void onConnectionStarted(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccess() {
        if (adaptive_bitrate) {
            Log.d(LOGTAG, "Setting adaptive bitrate");
            bitrateAdapter = new BitrateAdapter(bitrate -> {
                getCamera().setVideoBitrateOnFly(bitrate);
            });
            bitrateAdapter.setMaxBitrate(getCamera().getBitrate());
        } else {
            Log.d(LOGTAG, "Not doing adaptive bitrate");
        }
        Toast.makeText(getApplicationContext(), "Connection success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onNewBitrate(final long bitrate) {
        if (bitrateAdapter != null) {
            bitrateAdapter.adaptBitrate(bitrate, getCamera().getStreamClient().hasCongestion());
            Intent intent = new Intent(NEW_BITRATE);
            intent.putExtra(NEW_BITRATE, bitrate);
            getApplicationContext().sendBroadcast(intent);
        }
    }

    private void addCert() {
        Log.d(LOGTAG, "add cert");
        if (cert_file != null) {
            try {
                Log.d(LOGTAG, "Using cert: " + getFilesDir().getAbsolutePath());
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                FileInputStream caFile = new FileInputStream(cert_file);
                keyStore.load(caFile, cert_password.toCharArray());
                caFile.close();

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                SSLContext sslctx = SSLContext.getInstance("TLS");
                sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                if (rtspCamera2 != null)
                    rtspCamera2.getStreamClient().addCertificates(trustManagerFactory.getTrustManagers());
                else if (rtmpCamera2 != null)
                    rtmpCamera2.getStreamClient().addCertificates(trustManagerFactory.getTrustManagers());

            } catch (Exception e) {
                Log.e(LOGTAG, e.getMessage());
                Toast.makeText(this, "Failed to open cert: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOGTAG, "Cert null");
        }
    }

    public boolean prepareEncoders() {
        Log.d(LOGTAG, "prepareEncoders");
        int width = resolution.getWidth();
        int height = resolution.getHeight();

        if (Objects.equals(codec, VideoCodec.H264.name()))
            getCamera().setVideoCodec(VideoCodec.H264);
        else if (Objects.equals(codec, VideoCodec.H265.name()))
            getCamera().setVideoCodec(VideoCodec.H265);
        else if (Objects.equals(codec, VideoCodec.AV1.name()))
            getCamera().setVideoCodec(VideoCodec.AV1);

        if (protocol.equals("udp")) {
            getCamera().setAudioCodec(AudioCodec.AAC);
        } else if (Objects.equals(audio_codec, AudioCodec.G711))
            try {
                getCamera().setAudioCodec(AudioCodec.G711);
                Log.d(LOGTAG, "Set audio codec to G711");
            } catch (RuntimeException e) {
                getCamera().setAudioCodec(AudioCodec.AAC);
                Log.d(LOGTAG, "Failed to set audio codec to G711, falling back to AAC: " + e.getMessage());
            }
        else if (audio_codec.equals(AudioCodec.AAC.name())) {
            getCamera().setAudioCodec(AudioCodec.AAC);
            Log.d(LOGTAG, "Set audio codec to AAC");
        } else if (!protocol.startsWith("rtmp")){
            getCamera().setAudioCodec(AudioCodec.OPUS);
            Log.d(LOGTAG, "Set audio codec to Opus");
        } else if (protocol.startsWith("rtmp")) {
            getCamera().setAudioCodec(AudioCodec.G711);
            stereo = false;
            samplerate = 8000;
            Log.d(LOGTAG, "Protocol is RTMP, setting audio codec to G711");
        }

        Log.d(LOGTAG, "Setting video bitrate to ".concat(String.valueOf(bitrate)));
        Log.d(LOGTAG, "Setting audio bitrate to ".concat(String.valueOf(audio_bitrate)));
        Log.d(LOGTAG, "Setting res to ".concat(String.valueOf(width)).concat(" x ").concat(String.valueOf(height)));

        addCert();

        boolean prepareVideo = getCamera().prepareVideo(width, height, fps,
                bitrate * 1024,
                CameraHelper.getCameraOrientation(this));


        Log.d(LOGTAG, "Sample rate: " + samplerate + " stereo " + stereo);
        boolean prepareAudio = getCamera().prepareAudio(
                audio_bitrate * 1024,
                samplerate,
                stereo,
                echo_cancel,
                noise_reduction);

        if (!enable_audio) {
            Log.d(LOGTAG, "disabling audio");
            getCamera().disableAudio();
        } else {
            getCamera().enableAudio();
            Log.d(LOGTAG, "enabling audio");
        }

        Log.d(LOGTAG, "PrepareVideo: ".concat(String.valueOf(prepareVideo)).concat(" Audio ").concat(String.valueOf(prepareAudio)));
        return prepareVideo && prepareAudio;
    }

    public void getSettings() {
        Log.d(LOGTAG, "Get settings");
        protocol = preferences.getString("protocol", "rtsp");

        if (protocol.startsWith("rtmp")) {
            rtmpCamera2 = new RtmpCamera2(getApplicationContext(), true, this);
            rtspCamera2 = null;
            srtCamera2 = null;
            genericCamera2 = null;
        } else if (protocol.equals("srt")) {
            srtCamera2 = new SrtCamera2(getApplicationContext(), true, this);
            rtspCamera2 = null;
            rtmpCamera2 = null;
            genericCamera2 = null;
        } else if (protocol.startsWith("rtsp")){
            rtspCamera2 = new RtspCamera2(getApplicationContext(), true, this);
            rtmpCamera2 = null;
            srtCamera2 = null;
            genericCamera2 = null;
        } else {
            genericCamera2 = new GenericCamera2(getApplicationContext(), true, this);
            rtmpCamera2 = null;
            srtCamera2 = null;
            rtspCamera2 = null;
        }

        getCamera().getStreamClient().setLogs(false);

        stream = preferences.getBoolean("stream_video", true);
        enable_audio = preferences.getBoolean("enable_audio", true);
        address = preferences.getString("address", "192.168.1.10");
        port = Integer.parseInt(preferences.getString("port", "8554"));
        path = preferences.getString("path", "stream");
        tcp = preferences.getBoolean("tcp", false);
        username = preferences.getString("username", "administrator");
        password = preferences.getString("password", "password");
        cert_file = preferences.getString("certificate", null);
        cert_password = preferences.getString("certificate_password", "atakatak");
        atak_address = preferences.getString("atak_address", address);
        Log.d(LOGTAG, "Got cert: " + cert_file);
        echo_cancel = preferences.getBoolean("echo_cancel", true);
        noise_reduction = preferences.getBoolean("noise_reduction", true);
        fps = Integer.parseInt(preferences.getString("fps", "30"));
        record = preferences.getBoolean("record", false);
        codec = preferences.getString("codec", "H264");
        bitrate = Integer.parseInt(preferences.getString("bitrate", "3000"));
        audio_bitrate = Integer.parseInt(preferences.getString("audio_bitrate", "128"));
        audio_codec = preferences.getString("audio_codec", AudioCodec.OPUS.name());
        send_cot = preferences.getBoolean("send_cot", false);

        if (audio_codec.equals(AudioCodec.G711.name())) {
            Log.d(LOGTAG, "Forcing G711 settings");
            stereo = false;
            samplerate = 8000;
        } else {
            Log.d(LOGTAG, "Audio Codec " + audio_codec);
            stereo = preferences.getBoolean("stereo", true);
            samplerate = Integer.parseInt(preferences.getString("samplerate", "44100"));
        }

        adaptive_bitrate = preferences.getBoolean("adaptive_bitrate", true);
        uid = preferences.getString("uid", "OpenTAK-ICU-" + UUID.randomUUID().toString());

        getResolution();
        prepareEncoders();

        if (protocol.startsWith("rtsp"))
            rtspCamera2.getStreamClient().setAuthorization(username, password);
        else if (protocol.startsWith("rtmp")) {
            rtmpCamera2.getStreamClient().setAuthorization(username, password);
            Log.d(LOGTAG, "Set RTMP username and password");
        }
    }

    public Camera2Base getCamera() {
        if (rtspCamera2 != null)
            return rtspCamera2;
        if (rtmpCamera2 != null)
            return rtmpCamera2;
        if (srtCamera2 != null)
            return srtCamera2;
        if (genericCamera2 != null)
            return genericCamera2;

        return new RtmpCamera2(getApplicationContext(), true, this);
    }

    private void getResolution() {
        Log.d(LOGTAG, "Get res");
        ArrayList<Size> resolutions = new ArrayList<>();

        List<Size> frontResolutions = getCamera().getResolutionsFront();

        // Only get resolutions supported by both cameras
        for (Size res : getCamera().getResolutionsBack()) {
            if (frontResolutions.contains(res)) {
                resolutions.add(res);
            }
        }

        resolution = resolutions.get(Integer.parseInt(preferences.getString("resolution", "0")));
        Log.d(LOGTAG, "getResolution ".concat(String.valueOf(resolution.getWidth())).concat(" x ").concat(String.valueOf(resolution.getHeight())));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        Log.d(LOGTAG, "onSharedPreferenceChanged");
        getSettings();
    }

    private void startRecording() {
        Log.d(LOGTAG, "Start recording");
        if (record) {
            try {
                if (!folder.exists()) {
                    folder.mkdir();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                currentDateAndTime = sdf.format(new Date());
                if (!getCamera().isStreaming()) {
                    if (prepareEncoders()) {
                        getCamera().startRecord(
                                folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    } else {
                        showNotification(getString(R.string.error_preparing_stream));
                    }
                } else {
                    getCamera().startRecord(
                            folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                getCamera().stopRecord();
                PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        Log.d(LOGTAG, "Stop recording");
        if (getCamera().isRecording()) {
            getCamera().stopRecord();
            PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
            Toast.makeText(this,
                    "file ".concat(currentDateAndTime).concat(".mp4 saved in ").concat(folder.getAbsolutePath()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void startStream() {
        Log.d(LOGTAG, "startStream");
        if (!getCamera().isStreaming() && !getCamera().isRecording()) {
            if (protocol.equals("rtsp") && tcp) {
                rtspCamera2.getStreamClient().setProtocol(Protocol.TCP);
            } else if (Objects.equals(protocol, "rtsp")) {
                rtspCamera2.getStreamClient().setProtocol(Protocol.UDP);
            }

            if (getCamera().isRecording() || prepareEncoders()) {

                if (!protocol.equals("srt") && !protocol.startsWith("udp") && !username.isEmpty() && !password.isEmpty()) {
                    try {
                        getCamera().getStreamClient().setAuthorization(username, password);
                    } catch (NotImplementedError e) {
                        Log.d(LOGTAG, e.getMessage());
                    }
                }

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port));
                if (!protocol.equals("udp"))
                    url = url.concat("/").concat(path);
                else {
                    try {
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            //This will probably never show since the OnBoardingActivity forces users to grant permission before using the app
                            Toast.makeText(getApplicationContext(), R.string.no_location_permissions, Toast.LENGTH_LONG).show();
                            return;
                        }
                        _locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, _locListener);
                        Log.d(LOGTAG,  "Requesting Locatiion updates");
                        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

                        multicastClient = new MulticastClient(getApplicationContext());
                        event event = new event();
                        event.setUid(uid);

                        Point point = new Point(9999999, 9999999, 9999999);
                        event.setPoint(point);

                        ConnectionEntry connectionEntry = new ConnectionEntry(address, path, uid, port, null, protocol);
                        connectionEntry.setRtspReliable(null);
                        __Video __video = new __Video(url, uid, connectionEntry);
                        Device device = new Device(rotationInDegrees, 0);
                        Sensor sensor = new Sensor(horizonalFov, rotationInDegrees);
                        Contact contact = new Contact(path);

                        Detail detail = new Detail(contact, __video, device, sensor, null, null);
                        event.setDetail(detail);

                        XmlFactory xmlFactory = XmlFactory.builder()
                                .xmlInputFactory(new WstxInputFactory())
                                .xmlOutputFactory(new WstxOutputFactory())
                                .build();

                        XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
                        String cot = xmlMapper.writeValueAsString(event);
                        multicastClient.send_cot(cot);
                    } catch (Exception e) {
                        Log.e(LOGTAG, "Failed to send UDP CoT", e);
                    }
                }
                Log.d(LOGTAG, url);

                if (!getCamera().isAutoFocusEnabled())
                    getCamera().enableAutoFocus();

                if (stream) {
                    getCamera().startStream(url);
                    Log.d(LOGTAG, "Started stream to ".concat(url));
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //This will probably never show since the OnBoardingActivity forces users to grant permission before using the app
                        Toast.makeText(getApplicationContext(), R.string.no_location_permissions, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (send_cot) {
                        _locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, _locListener);
                        postVideoStream();
                        if (!protocol.equals("udp")) {
                            Log.d(LOGTAG, "Starting Tcp Thread");
                            tcpClient = new TcpClient(getApplicationContext(), address, port, message -> Log.d(LOGTAG, message));
                            tcpClientThread = new Thread(tcpClient);
                            tcpClientThread.start();
                        }

                        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                    }

                    showNotification(getString(R.string.stream_in_progress));
                }

                startRecording();
            } else {
                Toast.makeText(this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stopStream(String error, String broadcastIntent) {
        Log.d(LOGTAG, "stopStream " + error);
        if (getCamera().isStreaming())
            getCamera().stopStream();

        if (tcpClient != null) {
            Log.d(LOGTAG, "Stopping TcpClient");
            tcpClient.setmRun(false);
            tcpClientThread.interrupt();
        }

        if (multicastClient != null) {
            multicastClient = null;
        }

        stopRecording();
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, accelerometer);

        _locManager.removeUpdates(_locListener);

        // Only show the "Ready to Stream" message if there is no error
        if (error != null && broadcastIntent != null) {
            showNotification(error);
            getApplicationContext().sendBroadcast(new Intent(broadcastIntent));
        } else {
            showNotification(getString(R.string.ready_to_stream));
        }
    }

    public void take_photo() {
        getCamera().getGlInterface().takePhoto(bitmap -> {

            HandlerThread handlerThread = new HandlerThread("HandlerThread");
            handlerThread.start();
            Looper looper = handlerThread.getLooper();
            Handler handler = new Handler(looper);

            handler.post(() -> {
                try {
                    String filename = "OpenTAKICU_".concat(String.valueOf(System.currentTimeMillis()));

                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, filename, "image:".concat(filename));
                        getApplicationContext().sendBroadcast(new Intent(TOOK_PICTURE));
                        showNotification(getString(R.string.saved_photo));
                    } else {
                        boolean savedSuccessfully;
                        OutputStream fos;
                        ContentResolver resolver =  getApplicationContext().getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/OpenTAKICU");
                        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                        fos = resolver.openOutputStream(imageUri);
                        getApplicationContext().sendBroadcast(new Intent(TOOK_PICTURE).setPackage(getPackageName()));
                        savedSuccessfully = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.flush();
                        fos.close();

                        if (savedSuccessfully) {
                            showNotification(getString(R.string.saved_photo));
                        } else {
                            Log.e(LOGTAG, "Failed to save photo");
                            showNotification(getString(R.string.saved_photo_failed));
                        }
                    }
                } catch (NullPointerException | IOException e) {
                    Log.e(LOGTAG, "Failed to save photo: ".concat(e.getMessage()));
                    showNotification(getString(R.string.saved_photo_failed) + ": " + e.getMessage());
                }
            });
        });
    }

    class ICULocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            Log.d(LOGTAG, "onLocationChanged");
            try {
                last_fix_time = System.currentTimeMillis();
                getApplicationContext().sendBroadcast(new Intent(LOCATION_CHANGE));

                event event = new event();
                event.setUid(uid);

                Point point = new Point(location.getLatitude(), location.getLongitude(), location.getAltitude());
                event.setPoint(point);

                Contact contact = new Contact(path);

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port)).concat("/").concat(path);
                ConnectionEntry connectionEntry = null;
                if (protocol.equals("udp"))
                    connectionEntry = new ConnectionEntry(address, path, uid, port, null, protocol);
                __Video __video = new __Video(url, uid, connectionEntry);

                Device device = new Device(rotationInDegrees,0);
                Sensor sensor = new Sensor(horizonalFov, rotationInDegrees);

                Detail detail = new Detail(contact, __video, device, sensor, null, null);
                event.setDetail(detail);

                XmlFactory xmlFactory = XmlFactory.builder()
                        .xmlInputFactory(new WstxInputFactory())
                        .xmlOutputFactory(new WstxOutputFactory())
                        .build();

                XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
                String cot = xmlMapper.writeValueAsString(event);
                if (!protocol.equals("udp") && tcpClient != null)
                    tcpClient.sendMessage(cot);
                else if (protocol.equals("udp") && multicastClient != null)
                    multicastClient.send_cot(cot);

            } catch (Exception e) {
                Log.e(LOGTAG, "Failed to send location to ATAK: " + e.getLocalizedMessage());
            }
        }
    }

    private void postVideoStream() {
        try {
            feed Feed = new feed(protocol, path, uid, address, port, path);
            videoConnections VideoConnections = new videoConnections(Feed);
            XmlFactory xmlFactory = XmlFactory.builder()
                    .xmlInputFactory(new WstxInputFactory())
                    .xmlOutputFactory(new WstxOutputFactory())
                    .build();

            XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();

            RequestBody requestBody = RequestBody.create(xmlMapper.writeValueAsString(VideoConnections).getBytes());
            Request request = new Request.Builder()
                    .url("http://" + atak_address + ":8080/Marti/vcm")
                    .post(requestBody)
                    .build();
            executor.execute(() -> {
                try {
                    Response response = okHttpClient.newCall(request).execute();
                    Log.d(LOGTAG, "Posted video: " + response.message() + " " + response.code());
                    Log.d(LOGTAG, response.toString());
                } catch (IOException e) {
                    Log.e(LOGTAG, "Failed to post video stream: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to post video");
            Log.e(LOGTAG, e.toString());
        }
    }
}