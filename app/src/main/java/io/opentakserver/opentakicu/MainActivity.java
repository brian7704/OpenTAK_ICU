package io.opentakserver.opentakicu;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.library.rtsp.RtspCamera1;
import io.opentakserver.opentakicu.utils.PathUtils;
import com.pedro.rtsp.rtsp.Protocol;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * More documentation see:
 * {@link com.pedro.library.base.Camera1Base}
 * {@link com.pedro.library.rtsp.RtspCamera1}
 */
public class MainActivity extends AppCompatActivity
        implements Button.OnClickListener, ConnectChecker, SurfaceHolder.Callback,
        View.OnTouchListener {

    private final String LOGTAG = "MainActivity";
    private final ArrayList<String> PERMISSIONS = new ArrayList<>();
    private final Integer[] orientations = new Integer[] { 0, 90, 180, 270 };
    private final Context context = this;
    private final RxDataStore<Preferences> prefsDataStore = new RxPreferenceDataStoreBuilder(context, "settings").build();

    private RtspCamera1 rtspCamera1;
    private SurfaceView surfaceView;
    private Button bStartStop, bRecord;
    private EditText etUrl;
    private String currentDateAndTime = "";
    private File folder;
    //options menu
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private RadioGroup rgChannel;
    private RadioButton rbTcp, rbUdp;
    private Spinner spResolution;
    private CheckBox cbEchoCanceler, cbNoiseSuppressor;
    private EditText etVideoBitrate, etFps, etAudioBitrate, etSampleRate, etWowzaUser,
            etWowzaPassword;
    private String lastVideoBitrate;
    private TextView tvBitrate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        folder = PathUtils.getRecordPath();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        permissions();

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(this);

        if (!hasPermissions(this, PERMISSIONS)) {
            Intent intent = new Intent(MainActivity.this, OnBoardingActivity.class);
            startActivity(intent);
            finish();
        }

        rtspCamera1 = new RtspCamera1(surfaceView, this);

        prepareOptionsMenuViews();

        tvBitrate = findViewById(R.id.tv_bitrate);
        etUrl = findViewById(R.id.et_rtp_url);
        etUrl.setHint(R.string.hint_rtsp);
        bStartStop = findViewById(R.id.b_start_stop);
        bStartStop.setOnClickListener(this);
        bRecord = findViewById(R.id.b_record);
        bRecord.setOnClickListener(this);
        Button switchCamera = findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);
    }

    private void permissions() {
        PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
        PERMISSIONS.add(Manifest.permission.CAMERA);
        PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);

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

    private void prepareOptionsMenuViews() {
        Log.d(LOGTAG, "prepareOptionsMenu");
        drawerLayout = findViewById(R.id.activity_custom);
        navigationView = findViewById(R.id.nv_rtp);
        navigationView.inflateMenu(R.menu.options_rtsp);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.rtsp_streamer,
                R.string.rtsp_streamer) {

            public void onDrawerOpened(View drawerView) {
                actionBarDrawerToggle.syncState();
                lastVideoBitrate = etVideoBitrate.getText().toString();
            }

            public void onDrawerClosed(View view) {
                actionBarDrawerToggle.syncState();
                if (lastVideoBitrate != null && !lastVideoBitrate.equals(
                        etVideoBitrate.getText().toString()) && rtspCamera1.isStreaming()) {
                    int bitrate = Integer.parseInt(etVideoBitrate.getText().toString()) * 1024;
                    rtspCamera1.setVideoBitrateOnFly(bitrate);
                    Toast.makeText(MainActivity.this, "New bitrate: " + bitrate, Toast.LENGTH_SHORT).
                            show();
                }
            }
        };
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        //checkboxs
        cbEchoCanceler =
                (CheckBox) navigationView.getMenu().findItem(R.id.cb_echo_canceler).getActionView();
        cbNoiseSuppressor =
                (CheckBox) navigationView.getMenu().findItem(R.id.cb_noise_suppressor).getActionView();
        //radiobuttons
        rbTcp = (RadioButton) navigationView.getMenu().findItem(R.id.rb_tcp).getActionView();
        rbUdp = (RadioButton) navigationView.getMenu().findItem(R.id.rb_udp).getActionView();
        rgChannel = (RadioGroup) navigationView.getMenu().findItem(R.id.channel).getActionView();
        rbTcp.setChecked(true);
        rbTcp.setOnClickListener(this);
        rbUdp.setOnClickListener(this);
        //spinners
        spResolution = (Spinner) navigationView.getMenu().findItem(R.id.sp_resolution).getActionView();

        ArrayAdapter<Integer> orientationAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        orientationAdapter.addAll(orientations);

        ArrayAdapter<String> resolutionAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        List<String> list = new ArrayList<>();
        for (Camera.Size size : rtspCamera1.getResolutionsBack()) {
            list.add(size.width + "X" + size.height);
        }
        resolutionAdapter.addAll(list);
        spResolution.setAdapter(resolutionAdapter);
        //edittexts
        etVideoBitrate =
                (EditText) navigationView.getMenu().findItem(R.id.et_video_bitrate).getActionView();
        etFps = (EditText) navigationView.getMenu().findItem(R.id.et_fps).getActionView();
        etAudioBitrate =
                (EditText) navigationView.getMenu().findItem(R.id.et_audio_bitrate).getActionView();
        etSampleRate = (EditText) navigationView.getMenu().findItem(R.id.et_samplerate).getActionView();
        etVideoBitrate.setText("2500");
        etFps.setText("30");
        etAudioBitrate.setText("128");
        etSampleRate.setText("44100");
        etWowzaUser = (EditText) navigationView.getMenu().findItem(R.id.et_user).getActionView();
        etWowzaPassword =
                (EditText) navigationView.getMenu().findItem(R.id.et_password).getActionView();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.b_start_stop) {
            if (!rtspCamera1.isStreaming()) {
                bStartStop.setText(getResources().getString(R.string.stop_button));
                if (rbTcp.isChecked()) {
                    rtspCamera1.getStreamClient().setProtocol(Protocol.TCP);
                } else {
                    rtspCamera1.getStreamClient().setProtocol(Protocol.UDP);
                }
                String user = etWowzaUser.getText().toString();
                String password = etWowzaPassword.getText().toString();
                if (!user.isEmpty() && !password.isEmpty()) {
                    rtspCamera1.getStreamClient().setAuthorization(user, password);
                }
                if (rtspCamera1.isRecording() || prepareEncoders()) {
                    rtspCamera1.startStream(etUrl.getText().toString());
                } else {
                    //If you see this all time when you start stream,
                    //it is because your encoder device dont support the configuration
                    //in video encoder maybe color format.
                    //If you have more encoder go to VideoEncoder or AudioEncoder class,
                    //change encoder and try
                    Toast.makeText(this, "Error preparing stream, This device cant do it",
                            Toast.LENGTH_SHORT).show();
                    bStartStop.setText(getResources().getString(R.string.start_button));
                }
            } else {
                bStartStop.setText(getResources().getString(R.string.start_button));
                rtspCamera1.stopStream();
            }
        } else if (id == R.id.b_record) {
            if (!rtspCamera1.isRecording()) {
                try {
                    if (!folder.exists()) {
                        folder.mkdir();
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                    currentDateAndTime = sdf.format(new Date());
                    if (!rtspCamera1.isStreaming()) {
                        if (prepareEncoders()) {
                            rtspCamera1.startRecord(
                                    folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                            bRecord.setText(R.string.stop_record);
                            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error preparing stream, This device cant do it",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        rtspCamera1.startRecord(
                                folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                        bRecord.setText(R.string.stop_record);
                        Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    rtspCamera1.stopRecord();
                    PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                    bRecord.setText(R.string.start_record);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                rtspCamera1.stopRecord();
                PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                bRecord.setText(R.string.start_record);
                Toast.makeText(this,
                        "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                        Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.switch_camera) {
            try {
                rtspCamera1.switchCamera();
            } catch (CameraOpenException e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            //options menu
        } else if (id == R.id.rb_tcp) {
            if (rbUdp.isChecked()) {
                rbUdp.setChecked(false);
                rbTcp.setChecked(true);
            }
        } else if (id == R.id.rb_udp) {
            if (rbTcp.isChecked()) {
                rbTcp.setChecked(false);
                rbUdp.setChecked(true);
            }
        }
    }

    private boolean prepareEncoders() {
        Camera.Size resolution =
                rtspCamera1.getResolutionsBack().get(spResolution.getSelectedItemPosition());
        int width = resolution.width;
        int height = resolution.height;
        return rtspCamera1.prepareVideo(width, height, Integer.parseInt(etFps.getText().toString()),
                Integer.parseInt(etVideoBitrate.getText().toString()) * 1024,
                CameraHelper.getCameraOrientation(this)) && rtspCamera1.prepareAudio(
                Integer.parseInt(etAudioBitrate.getText().toString()) * 1024,
                Integer.parseInt(etSampleRate.getText().toString()),
                rgChannel.getCheckedRadioButtonId() == R.id.rb_stereo, cbEchoCanceler.isChecked(),
                cbNoiseSuppressor.isChecked());
    }

    @Override
    public void onConnectionStarted(@NotNull String rtspUrl) {
    }

    @Override
    public void onConnectionSuccess() {
        Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull final String reason) {
        Toast.makeText(MainActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
                .show();
        rtspCamera1.stopStream();
        bStartStop.setText(getResources().getString(R.string.start_button));
    }

    @Override
    public void onNewBitrate(final long bitrate) {
        tvBitrate.setText(bitrate + " bps");
    }

    @Override
    public void onDisconnect() {
        Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthError() {
        bStartStop.setText(getResources().getString(R.string.start_button));
        rtspCamera1.stopStream();
        Toast.makeText(MainActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAuthSuccess() {
        Toast.makeText(MainActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        rtspCamera1.startPreview();
        // optionally:
        //rtspCamera1.startPreview(CameraHelper.Facing.BACK);
        //or
        //rtspCamera1.startPreview(CameraHelper.Facing.FRONT);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (rtspCamera1.isStreaming()) {
            rtspCamera1.stopStream();
            bStartStop.setText(getResources().getString(R.string.start_button));
        }
        rtspCamera1.stopPreview();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (motionEvent.getPointerCount() > 1) {
            if (action == MotionEvent.ACTION_MOVE) {
                rtspCamera1.setZoom(motionEvent);
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            rtspCamera1.tapToFocus(view, motionEvent);
        }
        return true;
    }
}