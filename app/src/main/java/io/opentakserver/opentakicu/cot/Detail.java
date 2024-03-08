package io.opentakserver.opentakicu.cot;

public class Detail {
    Contact contact;
    __Video __video;
    Device device;
    Sensor sensor;
    Takv takv;
    uid uid;
    Track track;

    public Detail(Contact contact, __Video __video, Device device, Sensor sensor, Takv takv, uid uid, Track track) {
        this.contact = contact;
        this.__video = __video;
        this.device = device;
        this.sensor = sensor;
        this.takv = takv;
        this.uid = uid;
        this.track = track;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public __Video get__video() {
        return __video;
    }

    public void set__video(__Video __video) {
        this.__video = __video;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public Takv getTakv() {
        return takv;
    }

    public void setTakv(Takv takv) {
        this.takv = takv;
    }

    public io.opentakserver.opentakicu.cot.uid getUid() {
        return uid;
    }

    public void setUid(io.opentakserver.opentakicu.cot.uid uid) {
        this.uid = uid;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }
}

