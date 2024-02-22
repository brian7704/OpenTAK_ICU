package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Sensor {
    private int displayMagneticReference = 0;
    private double fov = 65.0;
    private double fovRed = 1.0;
    private double fovGreen = 1.0;
    private double fovBlue = 1.0;
    private double azimuth = 180;
    private double range = 100;

    public Sensor() {}

    @JacksonXmlProperty(isAttribute = true)
    public int getDisplayMagneticReference() {
        return displayMagneticReference;
    }

    public void setDisplayMagneticReference(int displayMagneticReference) {
        this.displayMagneticReference = displayMagneticReference;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getFov() {
        return fov;
    }

    public void setFov(double fov) {
        this.fov = fov;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getFovRed() {
        return fovRed;
    }

    public void setFovRed(double fovRed) {
        this.fovRed = fovRed;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getFovGreen() {
        return fovGreen;
    }

    public void setFovGreen(double fovGreen) {
        this.fovGreen = fovGreen;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getFovBlue() {
        return fovBlue;
    }

    public void setFovBlue(double fovBlue) {
        this.fovBlue = fovBlue;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }

    @JacksonXmlProperty(isAttribute = true)
    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
