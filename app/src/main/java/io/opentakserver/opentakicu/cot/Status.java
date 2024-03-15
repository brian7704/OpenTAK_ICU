package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Status {
    private float battery;

    public Status(float battery) {
        this.battery = battery;
    }

    @JacksonXmlProperty(isAttribute = true)
    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }
}
