package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Point {
    private double lat;
    private double lon;
    private double hae;
    private int ce = 9999999;
    private int le = 9999999;

    public Point(double lat, double lon, double hae) {
        this.lat = lat;
        this.lon = lon;
        this.hae = hae;
    }

    @JacksonXmlProperty(isAttribute=true)
    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    @JacksonXmlProperty(isAttribute=true)
    public double getLon() {
        return lon;
    }

    public void setLon(float lon) {
        this.lon = lon;
    }

    @JacksonXmlProperty(isAttribute=true)
    public double getHae() {
        return hae;
    }

    public void setHae(float hae) {
        this.hae = hae;
    }

    @JacksonXmlProperty(isAttribute=true)
    public int getCe() {
        return ce;
    }

    public void setCe(int ce) {
        this.ce = ce;
    }

    @JacksonXmlProperty(isAttribute=true)
    public int getLe() {
        return le;
    }

    public void setLe(int le) {
        this.le = le;
    }
}
