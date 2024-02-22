package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Device {
    private float azimuth;
    private float pitch;

    public Device(float azimuth, float pitch) {
        this.azimuth = azimuth;
        this.pitch = pitch;
    }

    @JacksonXmlProperty(isAttribute = true)
    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
    }

    @JacksonXmlProperty(isAttribute = true)
    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
