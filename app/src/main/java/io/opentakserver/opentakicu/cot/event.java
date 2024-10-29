package io.opentakserver.opentakicu.cot;

import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.util.Log;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Date;

public class event {
    private String how = "m-g";
    private String stale;
    private String start;
    private String time;
    private String type = "b-m-p-s-p-loc";
    private String uid;
    private String version = "2.0";
    private Point point;
    private Detail detail;

    private static final String LOGTAG = "Event";

    public event() {
        Date now = new Date();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);

        start = time = df.format(now);
        // Set stale to 1 day from now
        Calendar c = Calendar.getInstance();
        c.setTime(now);
        c.add(Calendar.MINUTE, 10);
        stale = df.format(c.getTime());
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getHow() {
        return how;
    }

    public void setHow(String how) {
        this.how = how;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getStale() {
        return stale;
    }

    public void setStale(String stale) {
        this.stale = stale;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Point getPoint() {
        return point;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public Detail getDetail() {
        return detail;
    }

    public void setDetail(Detail detail) {
        this.detail = detail;
    }
}
