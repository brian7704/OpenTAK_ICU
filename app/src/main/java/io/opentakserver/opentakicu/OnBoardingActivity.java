package io.opentakserver.opentakicu;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.appintro.AppIntro;
import com.github.appintro.SlidePolicy;
import com.github.appintro.model.SliderPagerBuilder;
import com.pedro.encoder.input.sources.video.Camera2Source;
import com.pedro.encoder.input.video.CameraHelper;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import io.opentakserver.opentakicu.contants.Preferences;

public class OnBoardingActivity extends AppIntro {
    private static final String ENABLE_BACK_BUTTON = "back_enabled";
    private static final String LOGTAG = "OnBoardingActivity";
    private static final String WELCOME_SLIDE = "welcome_slide";
    private static final String PERMISSIONS_SLIDE = "permissions_slide";
    private static final String NETWORK_PERMISSIONS_SLIDE = "network_permissions_slide";
    private static final String BACKGROUND_LOCATION_SLIDE = "background_location_slide";
    private static final String STORAGE_SLIDE = "storage_slide";
    private static final String FINALIZE_SLIDE = "finalize_slide";

    private static final String DEEPLINK_PREFIX = "DEEPLINK_PREFERENCE_";

    public static class CustomLayout extends Fragment implements SlidePolicy {

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstance) {
            View view = layoutInflater.inflate(R.layout.appintro_with_button, container, false);
            if(view == null) {
                return null;
            }

            Bundle args = getArguments();
            CharSequence title = args.getCharSequence("title");
            int image = args.getInt("image");

            DisplayMetrics metrics = getResources().getDisplayMetrics();

            TextView titleTextView = view.findViewById(R.id.title);
            titleTextView.setText(title);

            // fixes links from Utils.getText not clickable
            TextView tv = view.findViewById(R.id.description);
            tv.setAutoLinkMask(0);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            tv.setText(args.getCharSequence("pd_descr"));
            tv.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

            // Disable auto-sizing, as it makes the text not readable
            TextViewCompat.setAutoSizeTextTypeWithDefaults(tv, TextView.AUTO_SIZE_TEXT_TYPE_NONE);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            // Fix excessive vertical padding, causing scroll
            ViewGroup.LayoutParams lp = tv.getLayoutParams();
            if (lp instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) lp;
                params.setMargins(0, (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, metrics), 0, 0);
                tv.setPadding(tv.getPaddingLeft(), 0, tv.getPaddingRight(), 0);
                tv.setLayoutParams(params);
            }

            // fix drawable tint and size
            ImageView imageView = view.findViewById(R.id.image);
            imageView.setImageResource(image);
            int tint = args.getInt("pd_image_tint");
            if(tint > 0)
                imageView.setColorFilter(ContextCompat.getColor(view.getContext(), tint));
            if(args.getBoolean("pd_image_autosz")) {
                imageView.setAdjustViewBounds(true);
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220, metrics);
            }

            return view;
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstance) {
            super.onViewCreated(view, savedInstance);

            Bundle args = getArguments();
            CharSequence id = args.getCharSequence("slide_id");
        }

        @Override
        public boolean isPolicyRespected() {
            return true;
        }

        @Override
        public void onUserIllegallyRequestedNextPage() {

        }

        public static CustomLayout createInstance(CharSequence title, CharSequence description, int imageRes, boolean imageAutosize, String id) {
            CustomLayout fragment = new CustomLayout();
            Bundle args = new SliderPagerBuilder()
                    .title(title)
                    //.description(description) see below
                    .imageDrawable(imageRes)
                    .backgroundColorRes(R.color.colorBackground)
                    .titleColorRes(R.color.textColor)
                    .descriptionColorRes(R.color.textColor)
                    .build().toBundle();

            args.putCharSequence("pd_descr", description);
            args.putBoolean("pd_image_autosz", imageAutosize);
            args.putCharSequence("slide_id", id);
            args.putCharSequence("title", title);
            args.putInt("image", imageRes);
            fragment.setArguments(args);

            return fragment;
        }
    }

    public static class ImportSettingsSlide extends Fragment implements SlidePolicy {
        private static final String STATE_DECIDED = "import_decided";

        // Settings are only saved by the Import button, so the slide must not be skipped
        // by swiping or the next arrow before the user has chosen Import or Skip (#18).
        private boolean decided = false;

        @Override
        public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstance) {
            View view = layoutInflater.inflate(R.layout.onboarding_import, container, false);
            Bundle args = getArguments();
            if (savedInstance != null) {
                decided = savedInstance.getBoolean(STATE_DECIDED, false);
            }

            TextView title = view.findViewById(R.id.title);
            TextView desc = view.findViewById(R.id.description);
            Button btnAccept = view.findViewById(R.id.btn_accept);
            Button btnSkip = view.findViewById(R.id.btn_skip);

            title.setText(R.string.import_dialog_title);
            desc.setMovementMethod(new ScrollingMovementMethod());
            if (args != null) {
                desc.setText(args.getCharSequence("display_msg"));
            }

            btnAccept.setOnClickListener(v -> {
                if (getActivity() instanceof OnBoardingActivity) {
                    OnBoardingActivity activity = (OnBoardingActivity) getActivity();

                    activity.saveSettingsFromBundle(args);
                    decided = true;
                    activity.goToNextSlide();
                }
            });

            btnSkip.setOnClickListener(v -> {
                if (getActivity() instanceof OnBoardingActivity) {
                    OnBoardingActivity activity = (OnBoardingActivity) getActivity();

                    decided = true;
                    Toast.makeText(activity, R.string.import_skipped, Toast.LENGTH_SHORT).show();
                    activity.goToNextSlide();
                }
            });

            return view;
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(STATE_DECIDED, decided);
        }

        @Override public boolean isPolicyRespected() { return decided; }

        @Override
        public void onUserIllegallyRequestedNextPage() {
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.import_choose_option, Toast.LENGTH_SHORT).show();
            }
        }

        public static ImportSettingsSlide createInstance(Bundle configData, String msg) {
            ImportSettingsSlide fragment = new ImportSettingsSlide();
            configData.putCharSequence("display_msg", msg);
            fragment.setArguments(configData);
            return fragment;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Welcome slide
        addSlide(CustomLayout.createInstance(getString(R.string.welcome_to_open_tak_icu),
                getText(R.string.app_intro_welcome_msg),
                R.mipmap.ic_launcher, true, WELCOME_SLIDE));


        // Basic permissions slide
        if (needsPermission(Manifest.permission.CAMERA) || needsPermission(Manifest.permission.RECORD_AUDIO) || needsPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            addSlide(CustomLayout.createInstance(getString(R.string.permissions),
                    getText(R.string.permissions_slide_description),
                    R.mipmap.ic_launcher, true, PERMISSIONS_SLIDE));
        }

        // Network permission slide
        //addSlide(CustomLayout.createInstance("Network State", "Network state perms", R.mipmap.ic_launcher, true, NETWORK_PERMISSIONS_SLIDE));

        int slide_number = 3;

        // For android 9 or less request storage permission to save photos and video recordings
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && needsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            addSlide(CustomLayout.createInstance(getString(R.string.storage_slide_title), getString(R.string.storage_slide_description), R.mipmap.ic_launcher, true, STORAGE_SLIDE));
            String[] storage_permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            askForPermissions(storage_permission, slide_number, false);
            slide_number++;
        }

        // For android 10 or later ask for background location for sending CoTs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && needsPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            addSlide(CustomLayout.createInstance(getString(R.string.background_location_permission),
                    getText(R.string.background_location_permissions_slide_description),
                    R.mipmap.ic_launcher, true, BACKGROUND_LOCATION_SLIDE));

            String[] background_location_permission = new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION};
            askForPermissions(background_location_permission, slide_number, true);
        }

        // Handle Deeplink imports if present
        handleIntentImport();

        // Finished slide
        addSlide(CustomLayout.createInstance(getString(R.string.finished),
                getText(R.string.finished_onboarding),
                R.mipmap.ic_launcher, true, FINALIZE_SLIDE));

        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        askForPermissions(permissions.toArray(new String[0]), 2, true);

        /*permissions.clear();
        permissions.add(Manifest.permission.WRITE_SETTINGS);
        permissions.add(Manifest.permission.CHANGE_NETWORK_STATE);
        askForPermissions(permissions.toArray(new String[0]), 3, true);*/

        showStatusBar(true);
        setSkipButtonEnabled(false);
        setIndicatorEnabled(true);
        setSystemBackButtonLocked(false);

        // Theme
        int colorAccent = ContextCompat.getColor(this, R.color.colorAccent);
        setIndicatorColor(colorAccent, ContextCompat.getColor(this, R.color.colorAccent));
        setBackArrowColor(colorAccent);
        setColorSkipButton(colorAccent);
        setNextArrowColor(colorAccent);
        setBackArrowColor(colorAccent);
        setColorDoneText(colorAccent);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        applyAppIntroSystemBarInsets();
    }

    private void applyAppIntroSystemBarInsets() {
        View root = findViewById(com.github.appintro.R.id.background);
        if (root == null) {
            Log.w(LOGTAG, "AppIntro background root missing; cannot apply system bar insets");
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            int mask = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            Insets bars = windowInsets.getInsets(mask);
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
        root.post(() -> ViewCompat.requestApplyInsets(root));
    }

    private void handleIntentImport() {
        Uri data = getIntent().getData();
        if (data == null || !"opentakicu".equals(data.getScheme())) {
            return;
        }

        Map<String, String> params = new LinkedHashMap<>();
        for (String name : data.getQueryParameterNames()) {
            params.put(name, data.getQueryParameter(name));
        }
        if (params.isEmpty()) {
            return;
        }

        ImportSchema.Result result = ImportSchema.parse(params);
        Bundle configData = new Bundle();
        StringBuilder msg = new StringBuilder();
        msg.append(getString(R.string.import_dialog_message));

        for (Map.Entry<String, Object> setting : result.accepted.entrySet()) {
            String key = setting.getKey();
            Object value = setting.getValue();
            String display;

            if (value instanceof ImportSchema.Resolution) {
                ImportSchema.Resolution requested = (ImportSchema.Resolution) value;
                List<Size> sizes = backCameraResolutions();
                List<int[]> supported = new ArrayList<>();
                for (Size size : sizes) {
                    supported.add(new int[]{size.getWidth(), size.getHeight()});
                }
                int index = ImportSchema.pickResolutionIndex(supported, requested);
                if (index < 0) {
                    result.rejected.put(key, requested.toString());
                    continue;
                }
                Size chosen = sizes.get(index);
                String chosenText = chosen.getWidth() + "x" + chosen.getHeight();
                configData.putString(DEEPLINK_PREFIX + key, String.valueOf(index));
                display = chosenText.equals(requested.toString())
                        ? chosenText
                        : getString(R.string.import_resolution_fallback, requested.toString(), chosenText);
            } else if (value instanceof Boolean) {
                boolean enabled = (Boolean) value;
                configData.putBoolean(DEEPLINK_PREFIX + key, enabled);
                if (Preferences.TEXT_OVERLAY_TIMEZONE.equals(key)) {
                    display = getString(enabled ? R.string.import_value_utc : R.string.import_value_device_time);
                } else {
                    display = getString(enabled ? R.string.import_value_on : R.string.import_value_off);
                }
            } else {
                String text = (String) value;
                configData.putString(DEEPLINK_PREFIX + key, text);
                display = Preferences.STREAM_PASSWORD.equals(key) && !text.isEmpty() ? "********" : text;
            }

            msg.append(getString(R.string.import_label_format, getString(importLabel(key)), display)).append("\n");
        }

        if (!result.rejected.isEmpty()) {
            msg.append("\n").append(getString(R.string.import_rejected_header)).append("\n");
            for (Map.Entry<String, String> rejected : result.rejected.entrySet()) {
                msg.append(rejected.getKey()).append(" = ").append(rejected.getValue()).append("\n");
            }
        }

        addSlide(ImportSettingsSlide.createInstance(configData, msg.toString()));
    }

    /** Back camera sizes in the same order Camera2Service uses for the saved resolution index. */
    private List<Size> backCameraResolutions() {
        try {
            Camera2Source camera2Source = new Camera2Source(getApplicationContext());
            return new ArrayList<>(camera2Source.getCameraResolutions(CameraHelper.Facing.BACK));
        } catch (Exception e) {
            Log.w(LOGTAG, "Could not read camera resolutions for import", e);
            return new ArrayList<>();
        }
    }

    private static int importLabel(String key) {
        switch (key) {
            case Preferences.STREAM_VIDEO: return R.string.import_key_stream_video;
            case Preferences.STREAM_PROTOCOL: return R.string.import_key_protocol;
            case Preferences.STREAM_ADDRESS: return R.string.import_key_address;
            case Preferences.STREAM_PORT: return R.string.import_key_port;
            case Preferences.STREAM_PATH: return R.string.import_key_path;
            case Preferences.STREAM_USERNAME: return R.string.import_key_username;
            case Preferences.STREAM_PASSWORD: return R.string.import_key_password;
            case Preferences.VIDEO_SOURCE: return R.string.import_key_video_source;
            case Preferences.VIDEO_RESOLUTION: return R.string.import_key_resolution;
            case Preferences.VIDEO_BITRATE: return R.string.import_key_bitrate;
            case Preferences.VIDEO_ADAPTIVE_BITRATE: return R.string.import_key_adaptive_bitrate;
            case Preferences.VIDEO_FPS: return R.string.import_key_fps;
            case Preferences.VIDEO_CODEC: return R.string.import_key_codec;
            case Preferences.RECORD_VIDEO: return R.string.import_key_record;
            case Preferences.TEXT_OVERLAY: return R.string.import_key_text_overlay;
            case Preferences.TEXT_OVERLAY_TIMEZONE: return R.string.import_key_text_overlay_timezone;
            case Preferences.ENABLE_AUDIO: return R.string.import_key_enable_audio;
            case Preferences.ATAK_SEND_COT: return R.string.import_key_send_cot;
            default: throw new IllegalArgumentException("No label for import key " + key);
        }
    }

    public void saveSettingsFromBundle(Bundle data) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        for (String bundleKey : data.keySet()) {
            if (bundleKey.startsWith(DEEPLINK_PREFIX)) {
                String prefKey = bundleKey.substring(DEEPLINK_PREFIX.length());
                Object value = data.get(bundleKey);

                // Switches are read with getBoolean(), so they must be stored as booleans
                if (value instanceof Boolean) {
                    editor.putBoolean(prefKey, (Boolean) value);
                } else if (value instanceof String) {
                    editor.putString(prefKey, (String) value);
                }
            }
        }

        editor.apply();
        Toast.makeText(this, R.string.settings_updated_success, Toast.LENGTH_SHORT).show();
    }
    private boolean needsPermission(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onSkipPressed(@Nullable Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        runMainActivity();
    }

    @Override
    protected void onDonePressed(@Nullable Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        runMainActivity();
    }

    private void runMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void error(String message) {
        Log.e(LOGTAG, message);
    }
}
