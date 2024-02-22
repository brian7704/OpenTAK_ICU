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
import com.pedro.library.generic.GenericCamera2;
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
    static final String TOOK_PICTURE = "took_picture";
    static final String NEW_BITRATE = "new_bitrate";

    private NotificationManager notificationManager;
    private static final String LOGTAG = "CameraService";
    private final String channelId = "CameraServiceChannel";
    private final int notifyId = 3425;
    private SharedPreferences preferences;

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
    ExecutorService executor = Executors.newSingleThreadExecutor();

    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOGTAG, "onReceive: " + intent);
            if (action != null) {
                Log.d(LOGTAG, "Got broadcast " + action);
                switch (action) {
                    case START_STREAM:
                        startStream();
                        break;
                    case STOP_STREAM:
                        stopStream();
                        break;
                    case EXIT_APP:
                        exiting = true;
                        stopStream();
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

        executor.execute(() -> {
            tcpClient = new TcpClient(getApplicationContext(), address, port, new TcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    Log.d(LOGTAG, message);
                }
            });
        });

        _locListener = new ICULocationListener();
        _locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);

        genericCamera2.setCameraCallbacks(new CameraCallbacks() {
            @Override
            public void onCameraChanged(@NonNull CameraHelper.Facing facing) {
                CameraCharacteristics cameraCharacteristics = genericCamera2.getCameraCharacteristics();
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
        if (genericCamera2 != null && genericCamera2.isStreaming()) {
            notificationBuilder.addAction(stopStreamAction());
        } else {
            notificationBuilder.addAction(startStreamAction());
        }

        notificationBuilder.addAction(exit);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(notifyId, notification);

        return notification;
    }

    public GenericCamera2 getCamera() {
        return genericCamera2;
    }

    public void startPreview() {
        if (!genericCamera2.isOnPreview()) {
            Log.d(LOGTAG, "Starting Preview");
            genericCamera2.startPreview();
        }
    }

    public void stopPreview() {
        if (genericCamera2.isOnPreview()) {
            Log.d(LOGTAG, "Stopping Preview");
            genericCamera2.stopPreview();
        }
    }

    public void setView(OpenGlView openGlView) {
        genericCamera2.replaceView(openGlView);
    }

    public void setView(Context context) {
        genericCamera2.replaceView(context);
    }

    public void toggleLantern() {
        try {
            if (genericCamera2.isLanternSupported() && genericCamera2.isLanternEnabled())
                genericCamera2.disableLantern();
            else if (genericCamera2.isLanternSupported() && !genericCamera2.isLanternEnabled())
                genericCamera2.enableLantern();
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to toggle lantern: " + e.getMessage());
        }
    }

    public void switchCamera() {
        genericCamera2.switchCamera();
    }

    public void setZoom(MotionEvent motionEvent) {
        genericCamera2.setZoom(motionEvent);
    }

    public void tapToFocus(MotionEvent motionEvent) {
        genericCamera2.tapToFocus(motionEvent);
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
        showNotification(getString(R.string.auth_error));
        Intent intent = new Intent(AUTH_ERROR);
        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onAuthSuccess() {

    }

    @Override
    public void onConnectionFailed(@NonNull String reason) {
        Log.e(LOGTAG, "Connection failed: ".concat(reason));
        showNotification(getString(R.string.connection_failed) + ": " + reason);
    }

    @Override
    public void onConnectionStarted(@NonNull String s) {

    }

    @Override
    public void onConnectionSuccess() {
        if (adaptive_bitrate) {
            Log.d(LOGTAG, "Setting adaptive bitrate");
            bitrateAdapter = new BitrateAdapter(bitrate -> {
                genericCamera2.setVideoBitrateOnFly(bitrate);
                Log.d(LOGTAG, "Set bitrate to ".concat(String.valueOf(bitrate)));
            });
            bitrateAdapter.setMaxBitrate(genericCamera2.getBitrate());
        } else {
            Log.d(LOGTAG, "Not doing adaptive bitrate");
        }
        Toast.makeText(getApplicationContext(), "Connection success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisconnect() {
        showNotification(getString(R.string.ready_to_stream));
    }

    @Override
    public void onNewBitrate(final long bitrate) {
        if (bitrateAdapter != null) {
            Log.d(LOGTAG, "Set bitrate to " + bitrate);
            bitrateAdapter.adaptBitrate(bitrate, genericCamera2.getStreamClient().hasCongestion());
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

                genericCamera2.getStreamClient().addCertificates(trustManagerFactory.getTrustManagers());

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

        if (Objects.equals(codec, "H264"))
            genericCamera2.setVideoCodec(VideoCodec.H264);
        else if (Objects.equals(codec, "H265"))
            genericCamera2.setVideoCodec(VideoCodec.H265);
        else if (Objects.equals(codec, "AV1"))
            genericCamera2.setVideoCodec(VideoCodec.AV1);

        if (Objects.equals(audio_codec, "G711"))
            try {
                genericCamera2.setAudioCodec(AudioCodec.G711);
                Log.d(LOGTAG, "Set audio codec to G711");
            } catch (RuntimeException e) {
                genericCamera2.setAudioCodec(AudioCodec.AAC);
                Log.d(LOGTAG, "Failed to set audio codec to G711, falling back to AAC: " + e.getMessage());
            }
        else {
            genericCamera2.setAudioCodec(AudioCodec.AAC);
            Log.d(LOGTAG, "Set audio codec to AAC");
        }

        Log.d(LOGTAG, "Setting video bitrate to ".concat(String.valueOf(bitrate)));
        Log.d(LOGTAG, "Setting audio bitrate to ".concat(String.valueOf(audio_bitrate)));
        Log.d(LOGTAG, "Setting res to ".concat(String.valueOf(width)).concat(" x ").concat(String.valueOf(height)));

        addCert();

        boolean prepareVideo = genericCamera2.prepareVideo(width, height, fps,
                bitrate * 1024,
                CameraHelper.getCameraOrientation(this));


        boolean prepareAudio = genericCamera2.prepareAudio(
                audio_bitrate * 1024,
                samplerate,
                stereo,
                echo_cancel,
                noise_reduction);

        if (!enable_audio) {
            Log.d(LOGTAG, "disabling audio");
            genericCamera2.disableAudio();
        } else {
            genericCamera2.enableAudio();
            Log.d(LOGTAG, "enabling audio");
        }

        Log.d(LOGTAG, "PrepareVideo: ".concat(String.valueOf(prepareVideo)).concat(" Audio ").concat(String.valueOf(prepareAudio)));
        return prepareVideo && prepareAudio;
    }

    public void getSettings() {
        Log.d(LOGTAG, "Get settings");
        protocol = preferences.getString("protocol", "rtsp");

        genericCamera2 = new GenericCamera2(getApplicationContext(), true, this);

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
        samplerate = Integer.parseInt(preferences.getString("samplerate", "44100"));
        stereo = preferences.getBoolean("stereo", true);
        echo_cancel = preferences.getBoolean("echo_cancel", true);
        noise_reduction = preferences.getBoolean("noise_reduction", true);
        fps = Integer.parseInt(preferences.getString("fps", "30"));
        record = preferences.getBoolean("record", false);
        codec = preferences.getString("codec", "H264");
        bitrate = Integer.parseInt(preferences.getString("bitrate", "3000"));
        audio_bitrate = Integer.parseInt(preferences.getString("audio_bitrate", "128"));
        audio_codec = preferences.getString("audio_codec", "AAC");
        adaptive_bitrate = preferences.getBoolean("adaptive_bitrate", true);
        uid = preferences.getString("uid", "OpenTAK-ICU-" + UUID.randomUUID().toString());

        getResolution();
        prepareEncoders();

        try {
            genericCamera2.getStreamClient().setAuthorization(username, password);
        } catch (NotImplementedError e) {
            // RootEncoder throws NotImplementedError for the SrtClient in GenericCamera2, so just ignore it
        }
    }

    private void getResolution() {
        Log.d(LOGTAG, "Get res");
        ArrayList<Size> resolutions = new ArrayList<>();
        List<Size> frontResolutions = genericCamera2.getResolutionsFront();

        // Only get resolutions supported by both cameras
        for (Size res : genericCamera2.getResolutionsBack()) {
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
                if (!genericCamera2.isStreaming()) {
                    if (prepareEncoders()) {
                        genericCamera2.startRecord(
                                folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    } else {
                        showNotification(getString(R.string.error_preparing_stream));
                    }
                } else {
                    genericCamera2.startRecord(
                            folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                genericCamera2.stopRecord();
                PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopRecording() {
        Log.d(LOGTAG, "Stop recording");
        if (genericCamera2.isRecording()) {
            genericCamera2.stopRecord();
            //unlockScreenOrientation();
            PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
            Toast.makeText(this,
                    "file ".concat(currentDateAndTime).concat(".mp4 saved in ").concat(folder.getAbsolutePath()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void startStream() {
        Log.d(LOGTAG, "startStream");
        if (!genericCamera2.isStreaming() && !genericCamera2.isRecording()) {
            if (protocol.equals("rtsp") && tcp) {
                genericCamera2.getStreamClient().setProtocol(Protocol.TCP);
            } else if (Objects.equals(protocol, "rtsp")) {
                genericCamera2.getStreamClient().setProtocol(Protocol.UDP);
            }

            if (genericCamera2.isRecording() || prepareEncoders()) {

                if (!protocol.equals("srt") && !username.isEmpty() && !password.isEmpty()) {
                    try {
                        genericCamera2.getStreamClient().setAuthorization(username, password);
                    } catch (NotImplementedError e) {
                        Log.d(LOGTAG, e.getMessage());
                    }
                }

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port)).concat("/").concat(path);
                Log.d(LOGTAG, url);

                if (!genericCamera2.isAutoFocusEnabled())
                    genericCamera2.enableAutoFocus();

                if (stream) {
                    genericCamera2.startStream(url);
                    Log.d(LOGTAG, "Started stream to ".concat(url));
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //This will probably never show since the OnBoardingActivity forces users to grant permission before using the app
                        Toast.makeText(getApplicationContext(), R.string.no_location_permissions, Toast.LENGTH_LONG).show();
                        return;
                    }
                    _locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, _locListener);
                    postVideoStream();
                    executor.execute(() -> tcpClient.run());

                    sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

                    showNotification(getString(R.string.stream_in_progress));
                }

                startRecording();
            } else {
                Toast.makeText(this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void stopStream() {
        Log.d(LOGTAG, "stopStream");
        if (genericCamera2.isStreaming())
            genericCamera2.stopStream();

        if (tcpClient != null) {
            executor.execute(() -> tcpClient.stopClient());
        }

        stopRecording();
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, accelerometer);

        _locManager.removeUpdates(_locListener);
    }

    public void take_photo() {
        genericCamera2.getGlInterface().takePhoto(bitmap -> {

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
                event event = new event();
                event.setUid(uid);

                Point point = new Point(location.getLatitude(), location.getLongitude(), location.getAltitude());
                event.setPoint(point);

                Contact contact = new Contact(path);

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port)).concat("/").concat(path);
                __Video __video = new __Video(url, uid);

                Device device = new Device(rotationInDegrees,0);
                Sensor sensor = new Sensor(horizonalFov, rotationInDegrees);

                Takv takv = new Takv(getApplicationContext());

                Detail detail = new Detail(contact, __video, device, sensor, takv);
                event.setDetail(detail);

                XmlFactory xmlFactory = XmlFactory.builder()
                        .xmlInputFactory(new WstxInputFactory())
                        .xmlOutputFactory(new WstxOutputFactory())
                        .build();

                XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
                String cot = xmlMapper.writeValueAsString(event);
                if (tcpClient != null)
                    tcpClient.sendMessage(cot);

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