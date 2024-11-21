package io.opentakserver.opentakicu;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import android.util.Log;
//import com.pri.prialert.BuildConfig;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

public class FeatureSwitcher {
    private static int IS_LOW_PLATFORM = -1;
    private static int IS_S5_DUALCAM = -1;
    private static final String[] LOW_SPEED_PLATFORMS = {"MT6763", "MT6761", "MT6739", "MT6580"};
    private static String SUPPORT_MODES;
    //private static final LogUtil.Tag TAG = new LogUtil.Tag(FeatureSwitcher.class.getSimpleName());
    private static final String LOGTAG = "FeatureSwitcher";
    public static final float WIDEANGLE_SCALE;
    private static int mSurpportZoomCamera = -1;

    public static int getBackMaxZoom() {
        return 10;
    }

    public static int getDualZoomId() {
        return 5;
    }

    public static int getScreenType() {
        return 0;
    }

    public static boolean isNewSettingStyleSupport() {
        return true;
    }

    public static boolean isPicselfInSlideBar() {
        return true;
    }

    public static boolean isRollerZoom() {
        return true;
    }

    public static boolean isSupport60FPSEis() {
        return false;
    }

    public static boolean isSupportVibrate() {
        return true;
    }

    public static boolean isSupportZoomCamera() {
        return false;
    }

    public static boolean isSupportZoomSeekbar() {
        return true;
    }

    static {
        if (SystemProperties.getInt("ro.odm.widezoom5", 0) == 1) {
            WIDEANGLE_SCALE = 0.5f;
        } else {
            WIDEANGLE_SCALE = 0.6f;
        }
    }

    public static boolean isArcsoftSupperZoomSupported() {
        return SystemProperties.getInt("ro.odm.superzoom.arcsoft", 0) == 1 || isRealUHD();
    }

    public static boolean isArcsoftNightShotSupported() {
        if ((SystemProperties.getInt("ro.odm.nightshot.arcsoft", 0) == 1) || isSupportNightCam()) {
            return true;
        }
        return false;
    }

    public static boolean isArcsoftHDRSupported() {
        return SystemProperties.getInt("ro.odm.hdr.arcsoft", 0) == 1;
    }

    public static boolean isFaceBeautyupported() {
        return SystemProperties.getInt("ro.odm.camera_fn_facebeauty", 0) == 1;
    }

    public static boolean isPortraitupported() {
        return SystemProperties.getInt("ro.odm.portrait.mode", 1) == 1;
    }

    public static boolean isFalseFocusSupported() {
        return SystemProperties.getInt("ro.odm.false.focus", 0) == 1 || isSupportAeWithoutFocus();
    }

    public static boolean isFrontModeNormal() {
        return SystemProperties.getInt("ro.odm.def_front_mode_normal", 0) == 1;
    }

    public static boolean isLiftCameraSupport() {
        return SystemProperties.getInt("ro.odm.liftcamera.support", 0) == 1;
    }

    public static int getCurrentProjectValue() {
        return SystemProperties.getInt("ro.odm.current.project", 0);
    }

    public static String getStoragePathDefaultValue() {
        if (!isSupportStoragePath()) {
            return "phone";
        }
        return SystemProperties.getString("ro.odm.storagepath.default.value", "phone");
    }

    public static String getBrandWaterDefaultValue() {
        if (!isSupportSelfdefWater()) {
            return "0".equals(SystemProperties.getString("ro.odm.brandwatermark.default.value", "0")) ? "off" : "on";
        }
        String str = SystemProperties.getString("ro.odm.brandwatermark.default.value", (String) null);
        if (str == null || str.length() <= 0 || !isNumeric(str)) {
            return String.valueOf(3);
        }
        return str;
    }

    public static String getDefaultVideoSize(int i) {
        if (i == 1) {
            return SystemProperties.getString("ro.odm.front.default.videosize", (String) null);
        }
        return SystemProperties.getString("ro.odm.back.default.videosize", (String) null);
    }

    public static String getDefaultShouNum() {
        return SystemProperties.getString("ro.odm.continuousshotnum.default.value", "20");
    }

    public static boolean isContinuousShotnumSupport() {
        return SystemProperties.getInt("ro.odm.continuousshotnum.on", 0) == 1;
    }

    public static boolean isSupportDualCam() {
        return "1".equals(SystemProperties.getString("ro.odm.mode.aperture", "0")) && (!isS5() || isS5DualCam());
    }

    public static boolean isSupportNightCam() {
        return "1".equals(SystemProperties.getString("ro.odm.mode.nightcam", "0"));
    }

    public static boolean isAiSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.ai.scene", "0"));
    }

    public static boolean isAiTitleSupport() {
        return SystemProperties.getInt("ro.odm.ai.scene_title", 0) == 1;
    }

    public static int getVideoFocusMode() {
        return SystemProperties.getInt("ro.odm.video.focus.mode", 0);
    }

    public static boolean isProfessionalSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.professional.mode", "0"));
    }

    public static boolean isFullScreenRatioSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.fullscreen.support", "0"));
    }

    public static boolean isFrontPanoSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.frontpano", "0"));
    }

    public static boolean isShowPicselfieSeekbar() {
        return "1".equals(SystemProperties.getString("ro.odm.arcsoftpicselfie.seekbar", "0"));
    }

    public static boolean isOnly4_3SizeForPicselfie() {
        return "1".equals(SystemProperties.getString("ro.odm.only43.picslfie", "0"));
    }

    private static void initPlatform() {
        if (-1 == IS_LOW_PLATFORM) {
            String str = SystemProperties.getString("ro.board.platform", "");
            IS_LOW_PLATFORM = 0;
            if (!isBeautyWithOpengl()) {
                String[] strArr = LOW_SPEED_PLATFORMS;
                strArr[strArr.length - 2] = "MT6765";
                strArr[strArr.length - 1] = "MT6762";
            }
            if (str != null) {
                for (String equalsIgnoreCase : LOW_SPEED_PLATFORMS) {
                    if (equalsIgnoreCase.equalsIgnoreCase(str)) {
                        IS_LOW_PLATFORM = 1;
                        return;
                    }
                }
            }
        }
    }

    public static boolean isSlowPlatform() {
        initPlatform();
        return IS_LOW_PLATFORM == 1;
    }

    public static boolean isOpenBackcameraDefault() {
        return "1".equals(SystemProperties.getString("ro.odm.open.backcamera.default", "0"));
    }

    public static boolean canPlayFocusSound() {
        return "0".equals(SystemProperties.getString("ro.odm.disablefocussound", "1"));
    }

    public static boolean isSupportSmartLink() {
        return "1".equals(SystemProperties.getString("ro.odm.underwater_camera_mode", "0"));
    }

    public static boolean isSupportNotchScreen() {
        return isFullScreenRatioSupport();
    }

    public static boolean isSupportTimeLapse() {
        return "1".equals(SystemProperties.getString("ro.odm.timelapse", "1"));
    }

    public static String getDefaultModes() {
        if (SUPPORT_MODES == null) {
            String str = SystemProperties.getString("ro.odm.defaultmodes", (String) null);
            if (str == null || str.length() <= 0) {
                StringBuilder sb = new StringBuilder();
                if (!isNorthAmeric()) {
                    if (isSupportNightCam()) {
                        if (isSupportZoomCamera()) {
                            sb.append("NightCam2");
                            sb.append(",");
                        } else {
                            sb.append("NightCam");
                            sb.append(",");
                        }
                    } else if (isArcsoftNightShotSupported()) {
                        sb.append("LowLight");
                        sb.append(",");
                    }
                }
                sb.append("Video");
                sb.append(",");
                sb.append("Picture");
                if (isSupportFilmMode()) {
                    sb.append(",");
                    sb.append("Film");
                }
                if (isWideAngleModeShow()) {
                    sb.append(",");
                    sb.append("WideAngle");
                }
                if (isPicselfInSlideBar() && isPortraitupported()) {
                    sb.append(",");
                    sb.append("Picselfie");
                }
                SUPPORT_MODES = sb.toString();
            } else {
                SUPPORT_MODES = str;
            }
        }
        return SUPPORT_MODES;
    }

    /*public static boolean isDefaultMode(String str) {
        String slidebarModes = PrizePluginModeManager.getSlidebarModes((Context) null);
        if (slidebarModes != null && slidebarModes.length() > 0) {
            /*for (String equals : slidebarModes.replace(" ", BuildConfig.FLAVOR).split(",")) {
                if (equals.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }*/

    public static boolean isWideAngleCamSupport() {
        return SystemProperties.getInt("ro.odm.wideangle.support", 0) == 1;
    }

    public static boolean isSupportNightVideo() {
        return isSupportNightCam() && ("BLU".equals(Build.BRAND) || "1".equals(SystemProperties.getString("ro.odm.nightvideo", (String) null)));
    }

    public static String getPictureZoomModeName() {
        return SystemProperties.getString("ro.odm.picturezoom.mode.name", (String) null);
    }

    public static int isWideCorrectionSupport() {
        return SystemProperties.getInt("ro.odm.ldc.from.pi", 0);
    }

    public static boolean isPanoSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.camera.pano", "1"));
    }

    public static boolean isFilterSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.camera.filter", "1"));
    }

    public static boolean isSuperNightSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.camera.supernight", "0"));
    }

    public static String getMacroModeId() {
        if (!"-1".equals(SystemProperties.getString("ro.odm.camera.macroid", "-1"))) {
            return SystemProperties.getString("ro.odm.camera.macroid", "-1");
        }
        if (isMacroFeatureOnBackCamera()) {
            return "0";
        }
        return SystemProperties.getString("ro.odm.camera.macroid", "-1");
    }

    public static String getPlatformName() {
        return SystemProperties.getString("ro.board.platform", "");
    }

    public static int getWideAngleCamId() {
        return SystemProperties.getInt("ro.odm.wideangle.cameraid", -1);
    }

    public static int getSuperPictureMinSize() {
        try {
            return Integer.parseInt(SystemProperties.getString("ro.odm.camera.minsuperzoomsize", "50000000"));
        } catch (Exception e) {
            e.printStackTrace();
            return 51000000;
        }
    }

    public static int isScreenFlashAutoSupport() {
        return SystemProperties.getInt("ro.odm.screenflash.auto", 1);
    }

    public static boolean isPreviewSizeMatchScreen() {
        return isSlowPlatform() || SystemProperties.getInt("ro.odm.preview.matchscreen", 0) == 1;
    }

    public static boolean isNoExitCameraAfter2Min() {
        return SystemProperties.getInt("ro.odm.noexit.camera", 0) == 1;
    }

    public static boolean showZsdIcon() {
        return SystemProperties.getInt("ro.odm.zsd_hide", 0) != 1;
    }

    public static boolean showSimulateDualCameraTips() {
        return SystemProperties.getInt("ro.odm.simulate.dual.camera.tip", 0) == 1;
    }

    public static String getSupperzoomName() {
        return SystemProperties.getString("ro.odm.picturezoom.mode.name", (String) null);
    }

    public static boolean isPiDualCam() {
        return SystemProperties.getInt("ro.odm.bokeh.from.pi", 0) == 1;
    }

    public static String getBrandCustomerName() {
        return Build.BRAND;
    }

    public static String getModelCustomerName() {
        String str = Build.MODEL;
        Log.d(LOGTAG, "getModelCustomerName modelName = " + str);
        return str;
    }

    public static boolean isMacroNeedTips() {
        return "kewei".equals(SystemProperties.getString("ro.odm.pcba_oversea_customer", "")) || SystemProperties.getInt("ro.odm.macrotips", 0) == 1;
    }

    private static String getNodeValue(String str) {
        String str2 = "";
        try {
            str2 = new BufferedReader(new FileReader(str)).readLine();
        } catch (IOException e) {
            Log.d(LOGTAG, "getNodeValue error");
            e.printStackTrace();
            //str2 = BuildConfig.FLAVOR;
        }
        Log.d(LOGTAG, "getNodeValue prop=" + str2);
        return str2;
    }

    public static boolean isS5() {
        return "S5 Pro".equals(SystemProperties.getString("ro.product.model", ""));
    }

    public static boolean isS5DualCam() {
        if (IS_S5_DUALCAM == -1) {
            if ("0".equals(getNodeValue("/sys/class/sensordrv/kd_camera_hw/subcamera_check"))) {
                IS_S5_DUALCAM = 0;
            } else {
                IS_S5_DUALCAM = 1;
            }
            Log.d(LOGTAG, "IS_S5_DUALCAM=" + IS_S5_DUALCAM);
        }
        if (IS_S5_DUALCAM == 1) {
            return true;
        }
        return false;
    }

    public static String getNightCameraId() {
        return SystemProperties.getString("ro.odm.night.cameraid", "3");
    }

    public static boolean isWideCameraSupportFlash() {
        return SystemProperties.getInt("ro.odm.widecamera.flash", 0) == 1;
    }

    public static boolean isWideAngleModeShow() {
        return isWideAngleCamSupport() && isCustomGigaset();
    }

    public static boolean isWideCameraMacro() {
        String macroModeId = getMacroModeId();
        if (macroModeId != null && isWideAngleCamSupport() && macroModeId.equals(String.valueOf(getWideAngleCamId()))) {
            return true;
        }
        return false;
    }

    public static int getRedLightCamId() {
        return SystemProperties.getInt("ro.odm.redlight.camid", -1);
    }

    public static boolean isSupportLocation() {
        return SystemProperties.getInt("ro.odm.camera.location", 1) == 1;
    }

    public static boolean isSupportLensApp() {
        return SystemProperties.getInt("ro.odm.camera.lensapp", 0) == 1;
    }

    public static boolean isSupportFrontScreenFlash() {
        return SystemProperties.getInt("ro.odm.flash.front", 0) == 1;
    }

    public static boolean isSupportMacroFlash() {
        return SystemProperties.getInt("ro.odm.flash.macro", 0) == 1;
    }

    public static boolean isSupportWideangleZoom() {
        return isSupportZoomSeekbar() && isWideAngleCamSupport() && SystemProperties.getInt("ro.odm.wideangle.zoom", 1) == 1;
    }

    public static String getWatermarkModel() {
        if (SystemProperties.getInt("ro.odm.deviceinfo1", 0) == 1) {
            return SystemProperties.getString("ro.odm.watermark.model", SystemProperties.getString("ro.odm.device_name_custom", Build.MODEL));
        }
        return SystemProperties.getString("ro.odm.watermark.model", Build.MODEL).replace(";", " ");
    }

    public static boolean isSupportSelfdefWater() {
        return SystemProperties.getInt("ro.odm.camera.selfdefwater", 1) == 1;
    }

    public static boolean isChineseVersion() {
        String string = SystemProperties.getString("ro.odm.cust", (String) null);
        if (string != null) {
            return "pcba-china".equals(string) || "koobee".equals(string) || "coosea".equals(string);
        }
        return false;
    }

    public static boolean isBaiduLocation() {
        if (!isChineseVersion() && SystemProperties.getInt("ro.odm.baidulocation", 0) != 1) {
            return false;
        }
        return true;
    }

    public static boolean isSupportFlashForProMode() {
        return SystemProperties.getInt("ro.odm.addflash.promode", 0) == 1;
    }

    public static boolean isPcbaOversea() {
        return "pcba-sea".equals(SystemProperties.getString("ro.odm.cust", ""));
    }

    public static boolean isNumeric(String str) {
        return Pattern.compile("[0-9]*").matcher(str).matches();
    }

    public static boolean isVideoAFSwitchSupport() {
        return "1".equals(SystemProperties.getString("ro.odm.video.af.support", "0"));
    }

    public static boolean isRatioFor1_1Support() {
        return showFourSizes() || "1".equals(SystemProperties.getString("ro.odm.ratio.1_1.support", "0"));
    }

    public static boolean isRemoveAEOnProMode() {
        return "1".equals(SystemProperties.getString("ro.odm.professional_mode_remove_ae", "0"));
    }

    public static boolean isCancelSelfTimeCapture() {
        return "Hisense".equals(getBrandCustomerName()) || "Vestel".equals(getBrandCustomerName());
    }

    public static boolean isNeedCropWideCameraSize() {
        return SystemProperties.getInt("ro.odm.wideangle.cropsize", 0) == 1;
    }

    public static boolean isRaWSupport() {
        return SystemProperties.getInt("ro.odm.raw.support", 0) == 1;
    }

    public static boolean isHDRInTopBar() {
        return SystemProperties.getInt("ro.odm.hdr.top", 0) == 1;
    }

    public static boolean isFilterInTopBar() {
        return SystemProperties.getInt("ro.odm.filter.top", 0) == 1;
    }

    public static boolean isBeautyInPicself() {
        return SystemProperties.getInt("ro.odm.beautyinself", 0) == 1;
    }

    public static boolean isFPS60Support() {
        return SystemProperties.getInt("ro.odm.fps60", 0) == 1;
    }

    public static boolean isEISSupport() {
        return SystemProperties.getInt("ro.odm.eisenable", 0) == 1;
    }

    public static boolean isMtkEISSupport() {
        return SystemProperties.getInt("ro.odm.mtkeisenable", 0) == 1;
    }

    public static boolean isSupportFrontHDR() {
        return SystemProperties.getInt("ro.odm.front.hdr.support", 0) == 1;
    }

    public static boolean isSupportFrontTimeLapse() {
        return SystemProperties.getInt("ro.odm.fronttimelapse", 0) == 1;
    }

    public static boolean isSupportAutoHdr() {
        return SystemProperties.getInt("ro.odm.autohdr", 0) == 1;
    }

    public static boolean isSupportGlSurfaceView() {
        if (isFilterInTopBar()) {
            return true;
        }
        return !isNorthAmeric();
    }

    public static int getSdofMaxPreviewSize() {
        if (isSlowPlatform()) {
            return 307200;
        }
        String str = SystemProperties.getString("ro.board.platform", "");
        if ("mt6765".equalsIgnoreCase(str) || "mt6762".equalsIgnoreCase(str)) {
            return 307200;
        }
        return 691200;
    }

    public static boolean isBeautyWithOpengl() {
        return SystemProperties.getInt("ro.odm.beauty.opengl", 0) == 1;
    }

    public static int getWatermarkZoomValue() {
        if (!isSupportSelfdefWater()) {
            return SystemProperties.getInt("ro.odm.watermark.ratio", 18);
        }
        int i = 13;
        if (isLavaWaterMark() || isWTWaterMark()) {
            i = 37;
        }
        return SystemProperties.getInt("ro.odm.watermark.ratio", i);
    }

    public static String getProIsoValue() {
        return SystemProperties.getString("ro.odm.pro.iso.value", (String) null);
    }

    public static boolean isPhotoWithBeauty() {
        return SystemProperties.getInt("ro.odm.photowithbeauty", 0) == 1;
    }

    public static boolean isFaceRedSupport() {
        return SystemProperties.getInt("ro.odm.beauty.facered", 0) == 1;
    }

    public static boolean isObjectTrackSupport() {
        return SystemProperties.getInt("ro.odm.objecttrack", 0) == 1;
    }

    public static boolean isCasperVersion() {
        return "Casper".equals(Build.BRAND);
    }

    public static boolean isLavaVersion() {
        return "LZX404".equals(SystemProperties.getString("ro.build.product", (String) null));
    }

    public static boolean isFrontContinusShotSupport() {
        return isCasperVersion();
    }

    public static float getBeautyStrength() {
        int i = SystemProperties.getInt("ro.odm.beautystrength", 100);
        if (i <= 0 || i > 100) {
            return 1.0f;
        }
        return ((float) i) / 100.0f;
    }

    public static boolean isFastThumbnail() {
        return SystemProperties.getInt("ro.odm.fastthumbnail", 0) == 1;
    }

    public static boolean isMacroFeatureOnBackCamera() {
        return "-1".equals(SystemProperties.getString("ro.odm.camera.macroid", "-1")) && SystemProperties.getInt("ro.odm.macro.onbackcamera", 0) == 1;
    }

    public static boolean hideSettingSecuCam() {
        return SystemProperties.getInt("ro.odm.secuhidesetting", 0) == 1;
    }

    public static boolean isPhotoNightSupport() {
        return SystemProperties.getInt("ro.odm.photonight", 0) == 1;
    }

    public static boolean isHdrDefaultAuto() {
        return isSupportAutoHdr() && SystemProperties.getInt("ro.odm.defaultautohdr", 0) == 1;
    }

    public static boolean isSupportZoomRatio_10() {
        return "Hisense".equals(getBrandCustomerName()) || SystemProperties.getInt("ro.odm.zoom10", 0) == 1;
    }

    public static boolean isRemoveFrontZoom() {
        return SystemProperties.getInt("ro.odm.frontzoom", 0) == 0;
    }

    public static boolean isLocationDefaultOff() {
        return "BLU".equals(Build.BRAND) || SystemProperties.getInt("ro.odm.camlocationoff", 0) == 1;
    }

    public static boolean isWatermarkWithLogo() {
        return SystemProperties.getInt("ro.odm.watermarklogo", 1) == 1;
    }

    public static boolean isWideangleSupportHDR() {
        return SystemProperties.getInt("ro.odm.wideanglehdr", 0) == 1;
    }

    public static boolean showPanoTips() {
        return SystemProperties.getInt("ro.odm.pano_port_tips", 0) == 1;
    }

    public static boolean isPictureQualitySupport() {
        return isNorthAmeric();
    }

    public static boolean showFourSizes() {
        return SystemProperties.getInt("ro.odm.camerafoursizes", 0) == 1;
    }

    public static String getScreenFlashPosition() {
        return SystemProperties.getString("ro.odm.screenflash_position", (String) null);
    }

    public static boolean isNorthAmeric() {
        return SystemProperties.getInt("ro.odm.camera.northamerica", 0) == 1;
    }

    public static boolean isSupportAutoRedlight() {
        return SystemProperties.getInt("ro.odm.autoredlight", 0) == 1;
    }

    public static String getStarLightCameraId() {
        return SystemProperties.getString("ro.odm.camera.starcamid", "-1");
    }

    public static int getRedlightIsoValue() {
        return SystemProperties.getInt("ro.odm.redlightiso", 400);
    }

    public static boolean isSupportWideangleVideo() {
        return SystemProperties.getInt("ro.odm.wideanglevideo", 0) == 1;
    }

    public static boolean isRealUHD() {
        return SystemProperties.getInt("ro.odm.realuhd", 0) == 1;
    }

    public static int getVideoSoundDelay() {
        return SystemProperties.getInt("ro.odm.videosounddelay", 100);
    }

    public static boolean isSupportModeEnd() {
        return SystemProperties.getInt("ro.odm.modeend", 0) == 1;
    }

    public static boolean isSupportWaterFont() {
        return SystemProperties.getInt("ro.odm.water_font", 0) == 1;
    }

    public static boolean isBeautyDefault30() {
        return "Hisense".equals(getBrandCustomerName());
    }

    public static boolean isCustomGigaset() {
        String str = Build.BRAND;
        return "Gigaset".equals(str) || "rephone".equals(str);
    }

    public static boolean isVideoLock30fps() {
        return isCustomGigaset() || SystemProperties.getInt("ro.odm.vdeo.lock30fps", 0) == 1;
    }

    public static boolean isSupportAeWithoutFocus() {
        return SystemProperties.getInt("ro.odm.camera.frontae", 0) == 1 || isCustomLAVA();
    }

    public static boolean isHXPano() {
        return SystemProperties.getInt("ro.odm.panohx", 0) == 1;
    }

    public static boolean isCustomHX() {
        return "Hisense".equals(Build.BRAND);
    }

    public static int getDefaultTheme() {
        return SystemProperties.getInt("ro.odm.camera.defaulttheme", 0);
    }

    public static int getDefaultMoreStyle() {
        return SystemProperties.getInt("ro.odm.camera.morestyle", 0);
    }

    public static boolean isPhotoNightNoISO() {
        return SystemProperties.getInt("ro.odm.nightnoeis", 0) == 1;
    }

    public static boolean isHX241E() {
        String string = SystemProperties.getString("ro.build.product", (String) null);
        return string != null && string.contains("HLTE241E");
    }

    public static String getCustomFocusDistanceRange() {
        return SystemProperties.getString("ro.odm.camera.custom_mf_dist_range", "");
    }

    public static boolean showFlipAnimation() {
        return SystemProperties.getInt("ro.odm.camera.flipanim", 0) == 1 && SystemProperties.getInt("ro.odm.switch_camera_animation", 0) != 2;
    }

    public static boolean isSupportScanWIFI() {
        return SystemProperties.getInt("ro.odm.camera.scanwifi", 0) == 1;
    }

    public static boolean isSupportFlashDisable() {
        return "LAVA LXX501".equals(SystemProperties.getString("ro.product.model", ""));
    }

    public static boolean isSupportStoragePath() {
        return SystemProperties.getInt("ro.odm.camera.storagepath", 0) == 1;
    }

    public static boolean isSupportFilmMode() {
        return SystemProperties.getInt("ro.odm.camera.filmmode", 0) == 1;
    }

    public static boolean isGifModeSupport() {
        return SystemProperties.getInt("ro.odm.camera.gifmode", 0) == 1;
    }

    public static boolean isSupportPhotoQRCode() {
        return SystemProperties.getInt("ro.odm.photoqrcode", 0) == 1;
    }

    public static boolean isSmartScanModeSupport() {
        return SystemProperties.getInt("ro.odm.camera.smartscanmode", 0) == 1;
    }

    public static boolean isShortVideoSupport() {
        return SystemProperties.getInt("ro.odm.camera.shortvideo", 1) == 1;
    }

    public static boolean isSupportSlowMotion() {
        if (SystemProperties.getInt("ro.vendor.mtk_slow_motion_support", 0) != 1) {
            return false;
        }
        String str = SystemProperties.getString("ro.board.platform", "");
        if ("MT6761".equalsIgnoreCase(str) || "MT6739".equalsIgnoreCase(str) || SystemProperties.getInt("ro.odm.camera.slowmotion", 1) != 1) {
            return false;
        }
        return true;
    }

    public static boolean isCustomLAVA() {
        return "LAVA".equals(Build.BRAND);
    }

    public static boolean isLavaWaterMark() {
        return isCustomLAVA();
    }

    public static boolean isWTWaterMark() {
        return isCustomWT();
    }

    public static boolean isShowExifMake() {
        return SystemProperties.getInt("ro.odm.camera.showexifmake", 1) == 1;
    }

    public static boolean isProfessionalZSDEnabled() {
        return SystemProperties.getInt("ro.odm.camera.pro.zsddisabled", 1) != 1;
    }

    public static boolean closeModeByBAck() {
        return SystemProperties.getInt("ro.odm.camera.closemodebyback", 0) == 1;
    }

    public static boolean canRemoveMFMode() {
        return SystemProperties.getInt("ro.odm.professional_mode_remove_mf", 0) == 1;
    }

    public static boolean isSupportPortraitGpu() {
        return SystemProperties.getInt("ro.odm.portrait.gpu", 0) == 1;
    }

    public static boolean changeBrightnessFromSystem() {
        return "GM".equals(getBrandCustomerName());
    }

    public static boolean isContinusShortSupport() {
        return SystemProperties.getInt("ro.odm.continusshort", 1) == 1;
    }

    public static boolean isPhotoUHD() {
        return SystemProperties.getInt("ro.odm.photouhd", 0) == 1;
    }

    public static boolean savePicselfStrength() {
        return isCustomGigaset();
    }

    public static int getPicselfDefault() {
        return isCustomGigaset() ? 3 : 4;
    }

    public static int getFlashDisableLevel() {
        return SystemProperties.getInt("ro.odm.flashlight_disable_level", 15);
    }

    public static boolean isCustomWT() {
        return "WALTON".equals(Build.BRAND);
    }

    public static boolean removeQrcodeMenu() {
        return SystemProperties.getInt("ro.odm.removeqrcodemenu", 0) == 1;
    }

    public static int getFrontMaxZoom() {
        return SystemProperties.getInt("ro.odm.frontmaxzoom", -1);
    }

    public static boolean is39Platform() {
        return "MT6739".equalsIgnoreCase(SystemProperties.getString("ro.board.platform", ""));
    }

    public static boolean showHdrTips() {
        return SystemProperties.getInt("ro.odm.hidehdrtips", 0) != 1;
    }

    public static boolean needVideoCif() {
        return SystemProperties.getInt("ro.odm.videocif", 0) == 1;
    }

    public static boolean isFastPlatform() {
        String str = SystemProperties.getString("ro.board.platform", "");
        return "mt6877".equals(str) || "mt6853".equals(str) || "mt6833".equals(str) || "mt6893".equals(str);
    }

    public static boolean isPicselfSupportZoom() {
        return SystemProperties.getInt("ro.odm.picselfzoom", 0) == 1;
    }

    public static int getMinBrightness() {
        if (isCustomLAVA()) {
            return SystemProperties.getInt("ro.odm.camera.minbrightness", 5);
        }
        return SystemProperties.getInt("ro.odm.camera.minbrightness", 165);
    }

    public static boolean isDCODE() {
        return "DCODE".equals(Build.BRAND);
    }

    public static boolean isColoredWatermark() {
        return SystemProperties.getInt("ro.odm.coloredwatermark", 0) == 1;
    }

    public static boolean isSupportPQ() {
        return SystemProperties.getInt("ro.odm.camerapq", 0) == 1;
    }

    public static boolean isCustomGM() {
        return "GM".equals(Build.BRAND);
    }

    public static boolean isWideangleEISSupport() {
        return SystemProperties.getInt("ro.odm.wideangle.eis", 0) == 1;
    }

    public static int isEISDefaultOn() {
        return SystemProperties.getInt("ro.odm.default.eis", 0);
    }

    public static boolean isFrontEISSupport() {
        return SystemProperties.getInt("ro.odm.front.eis", 0) == 1;
    }

    public static boolean is4K60FpsSupport() {
        return SystemProperties.getInt("ro.odm.4k.60fps", 0) == 1;
    }

    public static boolean isNightOnly43() {
        return SystemProperties.getInt("ro.odm.lowlight43", 0) == 1;
    }
}
