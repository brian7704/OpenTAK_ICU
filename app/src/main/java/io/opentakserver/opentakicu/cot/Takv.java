package io.opentakserver.opentakicu.cot;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Takv {
    private String device = Build.MANUFACTURER + " " + Build.MODEL;
    private int os = Build.VERSION.SDK_INT;
    private String platform = "OpenTAK ICU";
    private String version;

    public Takv(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {

        }
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    @JacksonXmlProperty(isAttribute=true)
    public int getOs() {
        return os;
    }

    public void setOs(int os) {
        this.os = os;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
