package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ConnectionEntry {
    private String address;
    private String alias;
    private String uid;
    private int port;
    private int bufferTime = -1;
    private int roverPort = -1;
    private boolean ignoreEmbeddedKLV = false;
    private String path;
    private String protocol;
    private boolean rtspReliable = true;
    private int networkTimeout = 5000;

    public ConnectionEntry(String address, String alias, String uid, int port,
                           String path, String protocol) {
        this.address = address;
        this.alias = alias;
        this.uid = uid;
        this.port = port;
        this.path = path;
        this.protocol = protocol;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JacksonXmlProperty(isAttribute = true)
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @JacksonXmlProperty(isAttribute = true)
    public int getBufferTime() {
        return bufferTime;
    }

    public void setBufferTime(int bufferTime) {
        this.bufferTime = bufferTime;
    }

    @JacksonXmlProperty(isAttribute = true)
    public int getRoverPort() {
        return roverPort;
    }

    public void setRoverPort(int roverPort) {
        this.roverPort = roverPort;
    }

    @JacksonXmlProperty(isAttribute = true)
    public boolean isIgnoreEmbeddedKLV() {
        return ignoreEmbeddedKLV;
    }

    public void setIgnoreEmbeddedKLV(boolean ignoreEmbeddedKLV) {
        this.ignoreEmbeddedKLV = ignoreEmbeddedKLV;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @JacksonXmlProperty(isAttribute = true)
    public boolean isRtspReliable() {
        return rtspReliable;
    }

    public void setRtspReliable(boolean rtspReliable) {
        this.rtspReliable = rtspReliable;
    }

    @JacksonXmlProperty(isAttribute = true)
    public int getNetworkTimeout() {
        return networkTimeout;
    }

    public void setNetworkTimeout(int networkTimeout) {
        this.networkTimeout = networkTimeout;
    }
}
