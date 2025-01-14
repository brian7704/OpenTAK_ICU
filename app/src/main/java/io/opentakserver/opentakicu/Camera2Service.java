package io.opentakserver.opentakicu;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.contants.Preferences;
import io.opentakserver.opentakicu.cot.ConnectionEntry;
import io.opentakserver.opentakicu.cot.Contact;
import io.opentakserver.opentakicu.cot.Detail;
import io.opentakserver.opentakicu.cot.Device;
import io.opentakserver.opentakicu.cot.Status;
import io.opentakserver.opentakicu.cot.Track;
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
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.BatteryManager;
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.pedro.common.AudioCodec;
import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.encoder.input.sources.audio.MicrophoneSource;
import com.pedro.encoder.input.sources.audio.NoAudioSource;
import com.pedro.encoder.input.sources.video.Camera2Source;
import com.pedro.encoder.input.sources.video.VideoSource;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.utils.CodecUtil;
import com.pedro.extrasources.CameraUvcSource;
import com.pedro.library.base.StreamBase;
import com.pedro.library.rtmp.RtmpStream;
import com.pedro.library.rtsp.RtspStream;
import com.pedro.library.srt.SrtStream;
import com.pedro.library.udp.UdpStream;
import com.pedro.library.util.AndroidMuxerRecordController;
import com.pedro.library.util.BitrateAdapter;
import com.pedro.library.util.streamclient.RtmpStreamClient;
import com.pedro.library.util.streamclient.RtspStreamClient;
import com.pedro.library.view.OpenGlView;
import com.pedro.rtsp.rtsp.Protocol;
import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class Camera2Service extends Service implements ConnectChecker,
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

    private RtspStream rtspStream;
    private RtmpStream rtmpStream;
    private SrtStream srtStream;
    private UdpStream udpStream;
    private BitrateAdapter bitrateAdapter;

    private String protocol;
    private String address;
    private int port;
    private String path;
    private boolean tcp;
    private String username;
    private String password;
    private boolean stream_self_signed_cert;
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
    public String videoSource;

    private boolean send_cot = false;
    private boolean send_stream_details = false;
    private String atak_address;
    private long last_fix_time = 0;

    private OpenGlView openGlView;
    private boolean prepareAudio = false;
    private boolean prepareVideo = false;

    private int currentCameraId = 0;
    private boolean hasRedLightCamera = false;
    private boolean redLightEnabled = false;
    private int redLightCameraId = -1;
    private boolean isRooted = false;
    private final ArrayList<String> cameraIds = new ArrayList<>();
    private boolean lanternEnabled = false; //Keeps track of lantern when using a USB camera

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
    public static MutableLiveData<Camera2Service> observer = new MutableLiveData<>();

    private LocationListener _locListener;
    private LocationManager _locManager;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private TcpClient tcpClient;
    private Thread tcpClientThread;
    private MulticastClient multicastClient;
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

    //Suppress this warning for Android versions less than 13
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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

        Notification notification = showNotification(getString(R.string.ready_to_stream), true);

        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE|ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            startForeground(notifyId, notification, type);
        } else {
            startForeground(notifyId, notification);
        }

        getSettings();
        //startPreview();
        observer.postValue(this);

        // Setup broadcast receiver for action in the notification
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(START_STREAM);
        intentFilter.addAction(STOP_STREAM);
        intentFilter.addAction(EXIT_APP);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, intentFilter);
        }

        _locListener = new ICULocationListener();
        _locManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER);

        getCameraIds();
    }

    private void getCameraIds() {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            cameraIds.addAll(Arrays.asList(camera2Source.camerasAvailable()));

            // Adds the 200 MegaPixel camera on the Ulefone Armor 26 Ultra
            if (Objects.equals(Build.MODEL, "Armor 26 Ultra"))
                cameraIds.add("2");
            else
                Log.d(LOGTAG, "Device model is " + Build.MODEL);

            redLightCameraId = FeatureSwitcher.getRedLightCamId();
            if (redLightCameraId != -1) {
                hasRedLightCamera = true;
                cameraIds.add(redLightCameraId + "");
                Log.d(LOGTAG, "This device has a red light camera (" + redLightCameraId + "), checking for root...");
                isRooted = Shell.getShell().isRoot();
                Log.d(LOGTAG, "Device rooted: " + isRooted);
            }

            int wideAngleCamId = FeatureSwitcher.getWideAngleCamId();
            if (wideAngleCamId != -1) {
                Log.d(LOGTAG, "This device has a wide angle camera with ID " + wideAngleCamId);
                cameraIds.add(wideAngleCamId + "");
            }

            Log.d(LOGTAG, "Got cameraIds " + cameraIds);
        }
    }

    public float getZoom() {
        Log.d(LOGTAG, "GetZoom " + (getStream().getVideoSource() instanceof Camera2Source));
        if (getStream().getVideoSource() instanceof Camera2Source) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            Log.d(LOGTAG, "Zoom is " + camera2Source.getZoom());
            if (camera2Source.getZoom() < camera2Source.getZoomRange().getLower() || camera2Source.getZoom() > camera2Source.getZoomRange().getUpper())
                return camera2Source.getZoomRange().getLower();

            return camera2Source.getZoom();
        }
        Log.d(LOGTAG, "Zoom is 0");
        return 0f;
    }

    public VideoSource getVideoSource() {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_USB)) {
            Log.d(LOGTAG, "returning new usb cam");
            return new CameraUvcSource();
        }
        else {
            Log.d(LOGTAG, "returning new cam2");
            return new Camera2Source(getApplicationContext());
        }

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

    public Notification showNotification(String content, boolean silent) {
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
                .setSilent(silent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentTitle("OpenTAK ICU")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchActivity)
                .setContentText(content);

        // Show start/stop stream button in notification
        if (getStream().isStreaming()) {
            notificationBuilder.addAction(stopStreamAction());
        } else {
            notificationBuilder.addAction(startStreamAction());
        }

        notificationBuilder.addAction(exit);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(notifyId, notification);

        return notification;
    }

    public StreamBase getStream() {
        if (rtspStream != null)
            return rtspStream;
        if  (rtmpStream != null)
            return rtmpStream;
        if (srtStream != null)
            return srtStream;
        if (udpStream != null)
            return udpStream;

        return  new RtspStream(getApplicationContext(), this);
    }

    public void startPreview(OpenGlView openGlView) {
        this.openGlView = openGlView;
        if (!getStream().isOnPreview()) {
            Log.d(LOGTAG, "Starting Preview");
            getStream().startPreview(openGlView, true);
        } else {
            Log.e(LOGTAG, "not starting preview");
        }
    }

    public void stopPreview() {
        if (getStream().isOnPreview()) {
            Log.d(LOGTAG, "Stopping Preview");
            getStream().stopPreview();
        }
    }

    public void setView(OpenGlView openGlView) {
        Log.d(LOGTAG, "setView openGlView");
        //getCamera().replaceView(openGlView);
    }

    public void setView(Context context) {
        Log.d(LOGTAG, "setView context");
        //getCamera().replaceView(context);
    }

    public boolean toggleLantern() {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            if (Objects.equals(camera2Source.getCurrentCameraId(), redLightCameraId + "")) {
                return toggleRedLights();
            }
            else if (camera2Source.isLanternEnabled()) {
                camera2Source.disableLantern();
            } else {
                try {
                    camera2Source.enableLantern();
                } catch (Exception e) {
                    Log.d(LOGTAG, "Failed to enable lantern: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
            return camera2Source.isLanternEnabled();
        }

        else {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                camManager.setTorchMode(camManager.getCameraIdList()[0], !lanternEnabled);   //Turn ON
                lanternEnabled = !lanternEnabled;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            return lanternEnabled;
        }
    }

    private boolean toggleRedLights() {
        Camera2Source camera2Source;
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT))
            camera2Source = (Camera2Source) getStream().getVideoSource();
        else
            return false;

        if (hasRedLightCamera && isRooted && Objects.equals(camera2Source.getCurrentCameraId(), FeatureSwitcher.getRedLightCamId() + "")) {
            /*
                The magic file to toggle the IR LEDs will either be /sys/class/flash_irtouch/flash_irtouch_data/irtouch_value or
                /sys/class/flashlight_core/flashlight/flashlight_irtorch depending on the device. Running the ls command followed by &&
                ensures that the echo command will only run if the file actually exists
             */

            if (redLightEnabled) {
                Shell.cmd("ls /sys/class/flash_irtouch/flash_irtouch_data/irtouch_value && echo 0 > /sys/class/flash_irtouch/flash_irtouch_data/irtouch_value").exec();
                Shell.cmd("ls /sys/class/flashlight_core/flashlight/flashlight_irtorch && echo 0 > /sys/class/flashlight_core/flashlight/flashlight_irtorch").exec();
                redLightEnabled = false;
            } else {
                Shell.cmd("ls /sys/class/flash_irtouch/flash_irtouch_data/irtouch_value && echo 1 > /sys/class/flash_irtouch/flash_irtouch_data/irtouch_value").exec();
                Shell.cmd("ls /sys/class/flashlight_core/flashlight/flashlight_irtorch && echo 1 > /sys/class/flashlight_core/flashlight/flashlight_irtorch").exec();
                redLightEnabled = true;
            }
        }

        return redLightEnabled;
    }

    public void switchCamera() {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Log.d(LOGTAG, "Camera Changed");
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();

            if (cameraIds.isEmpty())
                getCameraIds();

            // Turn off the IR LEDs if they're on and we're switching away from the IR Camera
            if (cameraIds.get(currentCameraId).equals(redLightCameraId + "") && redLightEnabled) {
                toggleRedLights();
            }

            // Switch the camera
            currentCameraId++;
            if (currentCameraId > cameraIds.size() - 1) {
                currentCameraId = 0;
            }
            Log.d(LOGTAG, "Switching to camera " + cameraIds.get(currentCameraId));
            camera2Source.openCameraId(cameraIds.get(currentCameraId));

            // Turn on the IR LEDs if we're switching to the IR Camera
            if (cameraIds.get(currentCameraId).equals(redLightCameraId + "")) {
                toggleRedLights();
            }

            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(camera2Source.getCurrentCameraId());
                float[] maxFocus = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                SizeF size = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                float w = size.getWidth();
                float h = size.getHeight();
                horizonalFov = (2*Math.atan(w/(maxFocus[0]*2))) * 180/Math.PI;
                verticalFov = (2*Math.atan(h/(maxFocus[0]*2))) * 180/Math.PI;
                Log.d(LOGTAG, "horizontalFov = " + horizonalFov);
                Log.d(LOGTAG, "verticalFov = " + verticalFov);
            } catch (CameraAccessException e) {
                Log.e(LOGTAG, "Failed to get camera characteristics", e);
            }
        }
    }

    public void setZoom(MotionEvent motionEvent) {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            camera2Source.setZoom(motionEvent);
        }
    }

    public void tapToFocus(MotionEvent motionEvent) {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            camera2Source.tapToFocus(motionEvent);
        }
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
            float[] identityMatrix = new float[9];
            float[] rotationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix,
                    gravityData, geomagneticData);

            if (success) {
                float[] orientationMatrix = new float[3];
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
        Camera2Service getService() {
            return Camera2Service.this;
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
                getStream().setVideoBitrateOnFly(bitrate);
            });
            bitrateAdapter.setMaxBitrate(bitrate * 1024);
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
            bitrateAdapter.adaptBitrate(bitrate, getStream().getStreamClient().hasCongestion());
            Intent intent = new Intent(NEW_BITRATE);
            intent.putExtra(NEW_BITRATE, bitrate);
            getApplicationContext().sendBroadcast(intent);
        }
    }

    private void addCert() {
        Log.d(LOGTAG, "add cert");
        if (stream_self_signed_cert && cert_file != null && (protocol.equals("rtsps") || protocol.equals("rtmps"))) {
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

                if (protocol.equals("rtsps")) {
                    RtspStreamClient rtspStreamClient = (RtspStreamClient) getStream().getStreamClient();
                    rtspStreamClient.addCertificates(trustManagerFactory.getTrustManagers()[0]);
                } else if (protocol.equals("rtmps")) {
                    RtmpStreamClient rtmpStreamClient = (RtmpStreamClient) getStream().getStreamClient();
                    rtmpStreamClient.addCertificates(trustManagerFactory.getTrustManagers()[0]);
                }

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
        /*if (prepareAudio && prepareVideo) {
            Log.d(LOGTAG, "already prepared");
            return true;
        }*/
        int width = resolution.getWidth();
        int height = resolution.getHeight();

        if (Objects.equals(codec, VideoCodec.H265.name()))
            getStream().setVideoCodec(VideoCodec.H265);
        else if (Objects.equals(codec, VideoCodec.AV1.name()) && !protocol.equals("udp") && !protocol.equals("srt"))
            getStream().setVideoCodec(VideoCodec.AV1);
        else {
            getStream().setVideoCodec(VideoCodec.H264);
        }

        if (Objects.equals(audio_codec, AudioCodec.G711.name()) && !protocol.equals("srt") && !protocol.equals("udp")) {
            getStream().setAudioCodec(AudioCodec.G711);
            Log.d(LOGTAG, "Set audio codec to G711");
        } else if (audio_codec.equals(AudioCodec.OPUS.name()) && !protocol.startsWith("rtmp")) {
            getStream().setAudioCodec(AudioCodec.OPUS);
            Log.d(LOGTAG, "Set audio codec to OPUS");
        } else {
            // Fall back to AAC since all streaming protocol support it
            getStream().setAudioCodec(AudioCodec.AAC);
            Log.d(LOGTAG, "Set audio codec to AAC");
        }

        Log.d(LOGTAG, "Setting video bitrate to ".concat(String.valueOf(bitrate)));
        Log.d(LOGTAG, "Setting audio bitrate to ".concat(String.valueOf(audio_bitrate)));
        Log.d(LOGTAG, "Setting res to ".concat(String.valueOf(width)).concat(" x ").concat(String.valueOf(height)));

        addCert();

        if (prepareVideo)
            getStream().stopPreview();

        if (videoSource.equals(Preferences.VIDEO_SOURCE_USB)) {
            prepareVideo = getStream().prepareVideo(width, height, bitrate, fps);
            getStream().changeVideoSource(new CameraUvcSource());
        } else {
            prepareVideo = getStream().prepareVideo(width, height, bitrate, fps, 2, CameraHelper.getCameraOrientation(getApplicationContext()));
            getStream().changeVideoSource(new Camera2Source(getApplicationContext()));
        }

        Log.d(LOGTAG, "Sample rate: " + samplerate + " stereo " + stereo);
        prepareAudio = getStream().prepareAudio( samplerate, stereo, audio_bitrate * 1024, echo_cancel, noise_reduction);

        if (!enable_audio) {
            Log.d(LOGTAG, "disabling audio");
            getStream().changeAudioSource(new NoAudioSource());
        } else {
            getStream().changeAudioSource(new MicrophoneSource());
            Log.d(LOGTAG, "enabling audio");
        }

        if (openGlView != null)
            getStream().startPreview(openGlView, true);

        Log.d(LOGTAG, "PrepareVideo: ".concat(String.valueOf(prepareVideo)).concat(" Audio ").concat(String.valueOf(prepareAudio)));
        return prepareVideo && prepareAudio;
    }

    public void getSettings() {
        Log.d(LOGTAG, "Get settings");
        uid = preferences.getString(Preferences.UID, Preferences.UID_DEFAULT);

        String oldProtocol = protocol;
        protocol = preferences.getString(Preferences.STREAM_PROTOCOL, Preferences.STREAM_PROTOCOL_DEFAULT);

        if (!protocol.equals(oldProtocol)) {
            if (protocol.startsWith("rtmp")) {
                rtmpStream = new RtmpStream(getApplicationContext(), this);
                rtspStream = null;
                srtStream = null;
                udpStream = null;
            } else if (protocol.equals("srt")) {
                srtStream = new SrtStream(getApplicationContext(), this);
                rtspStream = null;
                rtmpStream = null;
                udpStream = null;
            } else if (protocol.startsWith("rtsp")) {
                rtspStream = new RtspStream(getApplicationContext(), this);
                rtmpStream = null;
                srtStream = null;
                udpStream = null;
            } else {
                udpStream = new UdpStream(getApplicationContext(), this);
                rtmpStream = null;
                srtStream = null;
                rtspStream = null;
            }
        }

        /* Stream Preferences */
        stream = preferences.getBoolean(Preferences.STREAM_VIDEO, Preferences.STREAM_VIDEO_DEFAULT);
        address = preferences.getString(Preferences.STREAM_ADDRESS, Preferences.STREAM_ADDRESS_DEFAULT);
        port = Integer.parseInt(preferences.getString(Preferences.STREAM_PORT, Preferences.STREAM_PORT_DEFAULT));
        path = preferences.getString(Preferences.STREAM_PATH, Preferences.STREAM_PATH_DEFAULT);
        tcp = preferences.getBoolean(Preferences.STREAM_USE_TCP, Preferences.STREAM_USE_TCP_DEFAULT);
        username = preferences.getString(Preferences.STREAM_USERNAME, Preferences.STREAM_USERNAME_DEFAULT);
        password = preferences.getString(Preferences.STREAM_PASSWORD, Preferences.STREAM_PASSWORD_DEFAULT);
        stream_self_signed_cert = preferences.getBoolean(Preferences.STREAM_SELF_SIGNED_CERT, Preferences.STREAM_SELF_SIGNED_CERT_DEFAULT);
        cert_file = preferences.getString(Preferences.STREAM_CERTIFICATE, Preferences.STREAM_CERTIFICATE_DEFAULT);
        cert_password = preferences.getString(Preferences.STREAM_CERTIFICATE_PASSWORD, Preferences.STREAM_CERTIFICATE_PASSWORD_DEFAULT);
        Log.d(LOGTAG, "Got cert: " + cert_file);

        /* Video Preferences */
        fps = Integer.parseInt(preferences.getString(Preferences.VIDEO_FPS, Preferences.VIDEO_FPS_DEFAULT));
        record = preferences.getBoolean(Preferences.RECORD_VIDEO, Preferences.RECORD_VIDEO_DEFAULT);
        codec = preferences.getString(Preferences.VIDEO_CODEC, Preferences.VIDEO_CODEC_DEFAULT);
        bitrate = Integer.parseInt(preferences.getString(Preferences.VIDEO_BITRATE, Preferences.VIDEO_BITRATE_DEFAULT));
        adaptive_bitrate = preferences.getBoolean(Preferences.VIDEO_ADAPTIVE_BITRATE, Preferences.VIDEO_ADAPTIVE_BITRATE_DEFAULT);

        /* Audio Preferences */
        enable_audio = preferences.getBoolean(Preferences.ENABLE_AUDIO, Preferences.ENABLE_AUDIO_DEFAULT);
        echo_cancel = preferences.getBoolean(Preferences.AUDIO_ECHO_CANCEL, Preferences.AUDIO_ECHO_CANCEL_DEFAULT);
        noise_reduction = preferences.getBoolean(Preferences.AUDIO_NOISE_REDUCTION, Preferences.AUDIO_NOISE_REDUCTION_DEFAULT);
        audio_bitrate = Integer.parseInt(preferences.getString(Preferences.AUDIO_BITRATE, Preferences.AUDIO_BITRATE_DEFAULT));
        audio_codec = preferences.getString(Preferences.AUDIO_CODEC, Preferences.AUDIO_CODEC_DEFAULT);
        if (audio_codec.equals(AudioCodec.G711.name())) {
            Log.d(LOGTAG, "Forcing G711 settings");
            stereo = false;
            samplerate = 8000;
        } else {
            Log.d(LOGTAG, "Audio Codec " + audio_codec);
            stereo = preferences.getBoolean(Preferences.STEREO_AUDIO, Preferences.STEREO_AUDIO_DEFAULT);
            samplerate = Integer.parseInt(preferences.getString(Preferences.AUDIO_SAMPLE_RATE, Preferences.AUDIO_SAMPLE_RATE_DEFAULT));
        }

        /* ATAK Preferences */
        atak_address = preferences.getString(Preferences.ATAK_SERVER_ADDRESS, Preferences.ATAK_SERVER_ADDRESS_DEFAULT);
        send_cot = preferences.getBoolean(Preferences.ATAK_SEND_COT, Preferences.ATAK_SEND_COT_DEFAULT);
        send_stream_details = preferences.getBoolean(Preferences.ATAK_SEND_STREAM_DETAILS, Preferences.ATAK_SEND_STREAM_DETAILS_DEFAULT);

        String oldVideoSource = videoSource;
        videoSource = preferences.getString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_DEFAULT);
        Log.d(LOGTAG, "videoSourcePref = " + videoSource);

        getResolutions();
        prepareEncoders();

        getStream().getStreamClient().setLogs(false);
        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
            for (String camera : camera2Source.camerasAvailable()) {
                Log.d(LOGTAG, "camerasAvailable: " + camera);
            }
        }

        if (protocol.startsWith("rtsp") && username != null && password != null) {
            RtspStreamClient rtspStreamClient = (RtspStreamClient) getStream().getStreamClient();
            rtspStreamClient.setAuthorization(username, password);
        }
        else if (protocol.startsWith("rtmp") && username != null && password != null) {
            RtmpStreamClient rtmpStreamClient = (RtmpStreamClient) getStream().getStreamClient();
            rtmpStreamClient.setAuthorization(username, password);
        }
    }

    private void getCamera2Resolutions() {
        Log.d(LOGTAG, "Get res");

        if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
            Camera2Source camera2Source = new Camera2Source(getApplicationContext());
            ArrayList<Size> resolutions = new ArrayList<>(camera2Source.getCameraResolutions(CameraHelper.Facing.BACK));

            String resolution_pref = preferences.getString(Preferences.VIDEO_RESOLUTION, null);
            if (resolution_pref == null) {
                // Default to 1080p if no res is selected
                resolution = new Size(1920, 1080);
            } else {
                resolution = resolutions.get(Integer.parseInt(resolution_pref));
            }
            Log.d(LOGTAG, "getResolution ".concat(String.valueOf(resolution.getWidth())).concat(" x ").concat(String.valueOf(resolution.getHeight())));
        }
    }

    private void getUsbResolution() {
        if (videoSource.equals(Preferences.VIDEO_SOURCE_USB)) {
            int width = Integer.parseInt(preferences.getString(Preferences.USB_WIDTH, Preferences.USB_WIDTH_DEFAULT));
            int height = Integer.parseInt(preferences.getString(Preferences.USB_HEIGHT, Preferences.USB_HEIGHT_DEFAULT));
            resolution = new Size(width, height);
            Log.i(LOGTAG, "Got USB Res " + width + " x " + height);
        }
    }

    private void getResolutions() {
        getCamera2Resolutions();
        getUsbResolution();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        Log.d(LOGTAG, "onSharedPreferenceChanged");
        if (s != null && !s.equals(Preferences.TEXT_OVERLAY))
            getSettings();
    }

    private void startRecording() {
        Log.d(LOGTAG, "Start recording");
        if (record) {
            try {
                if (!folder.exists()) {
                    folder.mkdir();
                }

                if (enable_audio && !audio_codec.equals(AudioCodec.AAC.name())) {
                    Log.d(LOGTAG, "Trying to record but audio codec is " + audio_codec);
                    // Recordings only support AAC audio and will fail if any other codec is used.
                    // This attempts to create a new recording controller using AAC.
                    // It allows the video to record, but not audio, which is better than no video or audio.
                    // TODO: Figure out if multiple audio encoders can be used at the same time
                    AndroidMuxerRecordController androidMuxerRecordController = new AndroidMuxerRecordController();
                    androidMuxerRecordController.setAudioCodec(AudioCodec.AAC);

                    MediaFormat audioFormat = MediaFormat.createAudioFormat(CodecUtil.AAC_MIME, samplerate, (stereo) ? 2 : 1);
                    audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, audio_bitrate * 1000);
                    audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
                    audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
                            MediaCodecInfo.CodecProfileLevel.AACObjectLC);

                    MediaCodec mediaCodec = MediaCodec.createEncoderByType("audio/mp4a-latm");
                    mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    androidMuxerRecordController.setAudioFormat(audioFormat);
                    getStream().setRecordController(androidMuxerRecordController);
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                currentDateAndTime = sdf.format(new Date());
                if (!getStream().isStreaming()) {
                    if (prepareEncoders()) {
                        getStream().startRecord(folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"), new RecordingListener());
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    } else {
                        showNotification(getString(R.string.error_preparing_stream), false);
                    }
                } else {
                    getStream().startRecord(folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"), new RecordingListener());
                    Log.d(LOGTAG, "Recording!");
                    Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Log.e(LOGTAG, "Failed to start recording", e);;
                getStream().stopRecord();
                PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOGTAG, "recording disabled");
        }
    }

    private void stopRecording() {
        Log.d(LOGTAG, "Stop recording");
        if (getStream().isRecording()) {
            getStream().stopRecord();
            PathUtils.updateGallery(this, folder.getAbsolutePath().concat("/").concat(currentDateAndTime).concat(".mp4"));
            Toast.makeText(this,
                    "file ".concat(currentDateAndTime).concat(".mp4 saved in ").concat(folder.getAbsolutePath()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void startStream() {
        Log.d(LOGTAG, "startStream");
        if (!getStream().isStreaming() && !getStream().isRecording()) {
            if (protocol.equals("rtsp") && tcp) {
                RtspStreamClient rtspStreamClient = (RtspStreamClient) getStream().getStreamClient();
                rtspStreamClient.setProtocol(Protocol.TCP);
            } else if (Objects.equals(protocol, "rtsp")) {
                RtspStreamClient rtspStreamClient = (RtspStreamClient) getStream().getStreamClient();
                rtspStreamClient.setProtocol(Protocol.UDP);
            }

            if (getStream().isRecording() || prepareEncoders()) {

                if (!protocol.equals("srt") && !protocol.startsWith("udp") && !username.isEmpty() && !password.isEmpty()) {
                    try {
                        getStream().getStreamClient().setAuthorization(username, password);
                    } catch (NotImplementedError e) {
                        Log.e(LOGTAG, e.getMessage());
                    }
                }

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port));

                // Support for MediaMTX's way of doing RTMP authentication
                if (protocol.startsWith("rtmp") && !username.equals(Preferences.STREAM_USERNAME_DEFAULT) && !password.equals(Preferences.STREAM_PASSWORD_DEFAULT)) {
                    url = url.concat("/").concat(path).concat("?user=").concat(username).concat("&pass=").concat(password);
                }
                else if (!protocol.equals("udp") && !protocol.equals("srt"))
                    url = url.concat("/").concat(path);
                else if (protocol.equals("srt")) {
                    url += "/publish:" + path;
                }
                // UDP Multicast
                else {
                    try {
                        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            //This will probably never show since the OnBoardingActivity forces users to grant permission before using the app
                            Toast.makeText(getApplicationContext(), R.string.no_location_permissions, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (Build.VERSION.SDK_INT >= 31)
                            _locManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 5000, 0, _locListener);
                        else
                            _locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, _locListener);
                        Log.d(LOGTAG,  "Requesting Location updates");
                        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
                        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

                        multicastClient = new MulticastClient(getApplicationContext());

                        event event = new event();
                        event.setUid(uid);

                        Point point = new Point(9999999, 9999999, 9999999);
                        event.setPoint(point);

                        ConnectionEntry connectionEntry = new ConnectionEntry(address, path, uid, port, path, protocol);
                        connectionEntry.setRtspReliable(0);

                        __Video __video = new __Video(url, uid, connectionEntry);
                        Device device = new Device(rotationInDegrees, 0);
                        Sensor sensor = new Sensor(horizonalFov, rotationInDegrees);
                        Contact contact = new Contact(path);

                        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

                        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                        float batteryPct = level * 100 / (float)scale;

                        Detail detail = new Detail(contact, __video, device, sensor, null, null, null, new Status(batteryPct));
                        event.setDetail(detail);

                        XmlFactory xmlFactory = XmlFactory.builder()
                                .xmlInputFactory(new WstxInputFactory())
                                .xmlOutputFactory(new WstxOutputFactory())
                                .build();

                        XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
                        xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

                        try {
                            String cot = xmlMapper.writeValueAsString(event);
                            multicastClient.send_cot(cot);
                        } catch (JsonProcessingException e) {
                            Log.d(LOGTAG, "Failed to generate CoT: " + e.getMessage());
                        }

                    } catch (Exception e) {
                        Log.e(LOGTAG, "Failed to send UDP CoT", e);
                    }
                }
                Log.d(LOGTAG, url);

                if (videoSource.equals(Preferences.VIDEO_SOURCE_DEFAULT)) {
                    Camera2Source camera2Source = (Camera2Source) getStream().getVideoSource();
                    if (!camera2Source.isAutoFocusEnabled())
                        camera2Source.enableAutoFocus();
                }

                if (stream) {
                    getStream().startStream(url);
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

                    showNotification(getString(R.string.stream_in_progress), true);
                }

                startRecording();
            } else {
                showNotification(getString(R.string.codec_error), false);
            }
        }
    }

    public void stopStream(String error, String broadcastIntent) {
        Log.d(LOGTAG, "stopStream " + error);
        if (getStream().isStreaming())
            getStream().stopStream();

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
            showNotification(error, false);
            getApplicationContext().sendBroadcast(new Intent(broadcastIntent));
        } else {
            showNotification(getString(R.string.ready_to_stream), true);
        }
    }

    public void take_photo() {
        getStream().getGlInterface().takePhoto(bitmap -> {

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
                        showNotification(getString(R.string.saved_photo), true);
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
                            showNotification(getString(R.string.saved_photo), true);
                        } else {
                            Log.e(LOGTAG, "Failed to save photo");
                            showNotification(getString(R.string.saved_photo_failed), false);
                        }
                    }
                } catch (NullPointerException | IOException e) {
                    Log.e(LOGTAG, "Failed to save photo: ".concat(e.getMessage()));
                    showNotification(getString(R.string.saved_photo_failed) + ": " + e.getMessage(), false);
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

                IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                float batteryPct = level * 100 / (float)scale;

                event event = new event();
                event.setUid(uid);

                Point point = new Point(location.getLatitude(), location.getLongitude(), location.getAltitude());
                event.setPoint(point);

                Contact contact = new Contact(path);

                String url = protocol.concat("://").concat(address).concat(":").concat(String.valueOf(port));

                ConnectionEntry connectionEntry = null;
                if (send_stream_details) {
                    connectionEntry = new ConnectionEntry(address, path, uid, port, path, protocol);
                    if (protocol.equals("udp")) {
                        connectionEntry.setRtspReliable(0);
                    } else {
                        url = url.concat("/").concat(path);
                    }
                }
                __Video __video = new __Video(url, uid, connectionEntry);

                Device device = new Device(rotationInDegrees,0);
                Sensor sensor = new Sensor(horizonalFov, rotationInDegrees);

                Detail detail = new Detail(contact, __video, device, sensor, null, null, new Track(location.getBearing(), location.getSpeed()), new Status(batteryPct));
                event.setDetail(detail);

                XmlFactory xmlFactory = XmlFactory.builder()
                        .xmlInputFactory(new WstxInputFactory())
                        .xmlOutputFactory(new WstxOutputFactory())
                        .build();

                XmlMapper xmlMapper = XmlMapper.builder(xmlFactory).build();
                xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
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