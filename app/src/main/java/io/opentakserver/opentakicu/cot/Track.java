package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Track {
    private double course;
    private double speed;

    public Track(double course, double speed) {
        this.course = course;
        this.speed = speed;
    }

    @JacksonXmlProperty(isAttribute=true)
    public double getCourse() {
        return course;
    }

    public void setCourse(double course) {
        this.course = course;
    }

    @JacksonXmlProperty(isAttribute=true)
    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
