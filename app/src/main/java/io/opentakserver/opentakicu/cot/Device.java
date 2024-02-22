package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Device {
    private double azimuth;
    private double pitch;

    public Device(double azimuth, double pitch) {
        this.azimuth = azimuth;
        this.pitch = pitch;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }
}
