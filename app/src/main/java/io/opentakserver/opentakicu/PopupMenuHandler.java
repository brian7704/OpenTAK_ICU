package io.opentakserver.opentakicu;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pedro.encoder.input.gl.render.filters.AnalogTVFilterRender;
import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.input.gl.render.filters.BasicDeformationFilterRender;
import com.pedro.encoder.input.gl.render.filters.BeautyFilterRender;
import com.pedro.encoder.input.gl.render.filters.BlackFilterRender;
import com.pedro.encoder.input.gl.render.filters.BlurFilterRender;
import com.pedro.encoder.input.gl.render.filters.BrightnessFilterRender;
import com.pedro.encoder.input.gl.render.filters.CartoonFilterRender;
import com.pedro.encoder.input.gl.render.filters.ChromaFilterRender;
import com.pedro.encoder.input.gl.render.filters.CircleFilterRender;
import com.pedro.encoder.input.gl.render.filters.ColorFilterRender;
import com.pedro.encoder.input.gl.render.filters.ContrastFilterRender;
import com.pedro.encoder.input.gl.render.filters.CropFilterRender;
import com.pedro.encoder.input.gl.render.filters.DuotoneFilterRender;
import com.pedro.encoder.input.gl.render.filters.EarlyBirdFilterRender;
import com.pedro.encoder.input.gl.render.filters.EdgeDetectionFilterRender;
import com.pedro.encoder.input.gl.render.filters.ExposureFilterRender;
import com.pedro.encoder.input.gl.render.filters.FireFilterRender;
import com.pedro.encoder.input.gl.render.filters.GammaFilterRender;
import com.pedro.encoder.input.gl.render.filters.GlitchFilterRender;
import com.pedro.encoder.input.gl.render.filters.GreyScaleFilterRender;
import com.pedro.encoder.input.gl.render.filters.HalftoneLinesFilterRender;
import com.pedro.encoder.input.gl.render.filters.Image70sFilterRender;
import com.pedro.encoder.input.gl.render.filters.LamoishFilterRender;
import com.pedro.encoder.input.gl.render.filters.MoneyFilterRender;
import com.pedro.encoder.input.gl.render.filters.NegativeFilterRender;
import com.pedro.encoder.input.gl.render.filters.NoFilterRender;
import com.pedro.encoder.input.gl.render.filters.NoiseFilterRender;
import com.pedro.encoder.input.gl.render.filters.PixelatedFilterRender;
import com.pedro.encoder.input.gl.render.filters.PolygonizationFilterRender;
import com.pedro.encoder.input.gl.render.filters.RGBSaturationFilterRender;
import com.pedro.encoder.input.gl.render.filters.RainbowFilterRender;
import com.pedro.encoder.input.gl.render.filters.RippleFilterRender;
import com.pedro.encoder.input.gl.render.filters.RotationFilterRender;
import com.pedro.encoder.input.gl.render.filters.SaturationFilterRender;
import com.pedro.encoder.input.gl.render.filters.SepiaFilterRender;
import com.pedro.encoder.input.gl.render.filters.SharpnessFilterRender;
import com.pedro.encoder.input.gl.render.filters.SnowFilterRender;
import com.pedro.encoder.input.gl.render.filters.SwirlFilterRender;
import com.pedro.encoder.input.gl.render.filters.TemperatureFilterRender;
import com.pedro.encoder.input.gl.render.filters.ZebraFilterRender;
import com.pedro.encoder.input.gl.render.filters.object.TextObjectFilterRender;
import com.pedro.encoder.utils.gl.TranslateTo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import io.opentakserver.opentakicu.contants.Preferences;

public class PopupMenuHandler implements SharedPreferences.OnSharedPreferenceChangeListener, LocationListener {
    private static final String LOGTAG = "PopupHandler";
    private TextObjectFilterRender pathText = new TextObjectFilterRender();
    private TextObjectFilterRender timestamp = new TextObjectFilterRender();
    private TextObjectFilterRender locationText = new TextObjectFilterRender();
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean showText = false;
    SharedPreferences pref;
    private Camera2Service camera2Service;
    private String pathName;
    private Context context;
    private LocationManager locationManager;
    private BaseFilterRender currentFilter = new NoFilterRender();

    public PopupMenuHandler(Camera2Service camera2Service, Context context) {
        this.camera2Service = camera2Service;
        this.context = context;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.registerOnSharedPreferenceChangeListener(this);
        pathName = pref.getString(Preferences.STREAM_PATH, "my_path");
        showText = pref.getBoolean(Preferences.TEXT_OVERLAY, Preferences.TEXT_OVERLAY_DEFAULT);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //Add NoFilterRender as the first element in the filter list. Prevents setFilter() from replacing the text overlay
        camera2Service.getStream().getGlInterface().addFilter(currentFilter);
        toggleText();
    }

    public boolean onMenuItemClick(MenuItem menuItem, FloatingActionButton flashlight) {
        int item = menuItem.getItemId();

        // Video Sources
        if (item == R.id.video_source_camera2) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!camera2Service.getStream().isStreaming() && !camera2Service.getStream().isRecording()) {
                preferences.edit().putString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_DEFAULT).apply();
                flashlight.setImageResource(R.drawable.flashlight_off);
            } else {
                Toast.makeText(context, "Can't switch video sources while streaming or recording", Toast.LENGTH_LONG).show();
            }

            return true;
        }
        if (item == R.id.video_source_camera_uvc) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (!camera2Service.getStream().isStreaming() && !camera2Service.getStream().isRecording()) {
                preferences.edit().putString(Preferences.VIDEO_SOURCE, Preferences.VIDEO_SOURCE_USB).apply();
                flashlight.setImageResource(R.drawable.flashlight_off);
            } else {
                Toast.makeText(context, "Can't switch video sources while streaming or recording", Toast.LENGTH_LONG).show();
            }

            return true;
        }

        // Filters
        if (item == R.id.no_filter) {
            currentFilter = new NoFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.analog_tv) {
            currentFilter = new AnalogTVFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.basic_deformation) {
            currentFilter = new BasicDeformationFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.beauty) {
            currentFilter = new BeautyFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.black) {
            currentFilter = new BlackFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.blur) {
            currentFilter = new BlurFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.brightness) {
            //TODO: Make configurable
            currentFilter = new BrightnessFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.cartoon) {
            currentFilter = new CartoonFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.chroma) {
            //TODO: Let the user choose a background
            ChromaFilterRender chromaFilterRender = new ChromaFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(chromaFilterRender);
            chromaFilterRender.setImage(BitmapFactory.decodeResource(context.getResources(), R.drawable.bg_chroma));
            currentFilter = chromaFilterRender;
            return true;
        }
        if (item == R.id.circle) {
            currentFilter = new CircleFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.color) {
            currentFilter = new ColorFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.contrast) {
            currentFilter = new ContrastFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.crop) {
            CropFilterRender cropFilterRender = new CropFilterRender();
            cropFilterRender.setCropArea(30f, 30f, 40f, 40f);
            camera2Service.getStream().getGlInterface().setFilter(cropFilterRender);
            currentFilter = cropFilterRender;
            return true;
        }
        if (item == R.id.duotone) {
            currentFilter = new DuotoneFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.early_bird) {
            currentFilter = new EarlyBirdFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.edge_detection) {
            currentFilter = new EdgeDetectionFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.exposure) {
            //TODO: Make the exposure configurable
            ExposureFilterRender exposureFilterRender = new ExposureFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(exposureFilterRender);
            currentFilter = exposureFilterRender;
            return true;
        }
        if (item == R.id.fire) {
            currentFilter = new FireFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.gamma) {
            //TODO: Make configurable
            GammaFilterRender gammaFilterRender = new GammaFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(gammaFilterRender);
            currentFilter = gammaFilterRender;
            return true;
        }
        if (item == R.id.glitch) {
            currentFilter = new GlitchFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.grey_scale) {
            currentFilter = new GreyScaleFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.halftone_lines) {
            //TODO: Make configurable
            HalftoneLinesFilterRender halftoneLinesFilterRender = new HalftoneLinesFilterRender();
            halftoneLinesFilterRender.setMode(2);
            camera2Service.getStream().getGlInterface().setFilter(halftoneLinesFilterRender);
            currentFilter = halftoneLinesFilterRender;
            return true;
        }
        if (item == R.id.image_70s) {
            currentFilter = new Image70sFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.lamoish) {
            currentFilter= new LamoishFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.money) {
            currentFilter = new MoneyFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.negative) {
            currentFilter = new NegativeFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.noise) {
            //TODO: Make strength configurable
            NoiseFilterRender noiseFilterRender = new NoiseFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(noiseFilterRender);
            currentFilter = noiseFilterRender;
            return true;
        }
        if (item == R.id.pixelated) {
            //TODO: Make configurable
            PixelatedFilterRender pixelatedFilterRender = new PixelatedFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(pixelatedFilterRender);
            currentFilter = pixelatedFilterRender;
            return true;
        }
        if (item == R.id.polygonization) {
            currentFilter = new PolygonizationFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.rainbow) {
            currentFilter = new RainbowFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.rgb_saturate) {
            //TODO: Make configurable
            RGBSaturationFilterRender rgbSaturationFilterRender = new RGBSaturationFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(rgbSaturationFilterRender);
            currentFilter = rgbSaturationFilterRender;
            return true;
        }
        if (item == R.id.ripple) {
            //TODO: Make configurable
            RippleFilterRender rippleFilterRender = new RippleFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(rippleFilterRender);
            currentFilter = rippleFilterRender;
            return true;
        }
        if (item == R.id.rotation) {
            //TODO: Make configurable
            RotationFilterRender rotationFilterRender = new RotationFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(rotationFilterRender);
            currentFilter = rotationFilterRender;
            return true;
        }
        if (item == R.id.saturation) {
            //TODO: Make configurable
            SaturationFilterRender saturationFilterRender = new SaturationFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(saturationFilterRender);
            currentFilter = saturationFilterRender;
            return true;
        }
        if (item == R.id.sepia) {
            currentFilter = new SepiaFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        if (item == R.id.sharpness) {
            //TODO: Make configurable
            SharpnessFilterRender sharpnessFilterRender = new SharpnessFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(sharpnessFilterRender);
            currentFilter = sharpnessFilterRender;
            return true;
        }
        if (item == R.id.snow) {
            //TODO: Make configurable
            SnowFilterRender snowFilterRender = new SnowFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(snowFilterRender);
            currentFilter = snowFilterRender;
            return true;
        }
        if (item == R.id.swirl) {
            //TODO: Make configurable
            SwirlFilterRender swirlFilterRender = new SwirlFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(swirlFilterRender);
            currentFilter = swirlFilterRender;
            return true;
        }
        if (item == R.id.temperature) {
            //TODO: Make configurable
            TemperatureFilterRender temperatureFilterRender = new TemperatureFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(temperatureFilterRender);
            currentFilter = temperatureFilterRender;
            return true;
        }
        if (item == R.id.text) {
            showText = !showText;
            pref.edit().putBoolean(Preferences.TEXT_OVERLAY, showText).apply();

            return true;
        }
        if (item == R.id.zebra) {
            currentFilter = new ZebraFilterRender();
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
            return true;
        }
        return false;
    }

    public void stopClock() {
        handler.removeCallbacks(clock);
    }

    public void toggleText() {
        if (camera2Service.getStream().getGlInterface().filtersCount() == 0) {
            camera2Service.getStream().getGlInterface().setFilter(currentFilter);
        }

        if (showText) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT < 31)
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
                else
                    locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 5000, 0, this);
            }

            camera2Service.getStream().getGlInterface().addFilter(pathText);
            pathText.setText(pathName, 20f, Color.WHITE, Color.BLACK);
            pathText.setScale(pathName.length(),  5f);
            pathText.setPosition(0, 85f);

            camera2Service.getStream().getGlInterface().addFilter(locationText);
            locationText.setText("0, 0", 20f, Color.WHITE, Color.BLACK);
            locationText.setScale(4f, 5f);
            locationText.setPosition(0, 90f);

            camera2Service.getStream().getGlInterface().addFilter(timestamp);
            String text = getTime();
            timestamp.setText(text, 20f, Color.WHITE, Color.BLACK);
            timestamp.setScale(text.length(),5f);
            timestamp.setPosition(TranslateTo.BOTTOM_LEFT);

            clock.run();
        } else {
            camera2Service.getStream().getGlInterface().removeFilter(pathText);
            camera2Service.getStream().getGlInterface().removeFilter(timestamp);
            camera2Service.getStream().getGlInterface().removeFilter(locationText);
            handler.removeCallbacks(clock);
            locationManager.removeUpdates(this);
        }
    }

    Runnable clock = new Runnable() {
        @Override
        public void run() {
            String time = getTime();
            timestamp.setText(time, 20f, Color.WHITE, Color.BLACK);
            timestamp.setScale(time.length(), 5f);
            handler.postDelayed(this, 1000);
        }
    };

    public static String getTime() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        return  df.format(new Date());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String s) {
        if (s != null && s.equals(Preferences.TEXT_OVERLAY)) {
            showText = sharedPreferences.getBoolean(s, false);
            toggleText();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        String text = location.getLatitude() + ", " + location.getLongitude() + ", " + location.getBearing() + "°";
        locationText.setText(text, 20f, Color.WHITE, Color.BLACK);
        locationText.setScale(text.length(), 5f);
        locationText.setPosition(0, 90f);
    }
}
