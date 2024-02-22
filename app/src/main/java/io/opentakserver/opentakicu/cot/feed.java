package io.opentakserver.opentakicu.cot;

public class feed {
    private String protocol;
    private String alias;
    private String uid;
    private String address;
    private int port;
    private int roverPort = -1;
    private boolean ignoreEmbeddedKLV = false;
    private String preferredMacAddress;
    private String preferredInterfaceAddress;
    private String path;
    private int buffer = 0;
    private int timeout = 5000;
    private int rtspReliable = 1;

    public feed(String protocol, String alias, String uid, String address, int port, String path) {
        this.protocol = protocol;
        this.alias = alias;
        this.uid = uid;
        this.address = address;
        this.port = port;
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getRoverPort() {
        return roverPort;
    }

    public void setRoverPort(int roverPort) {
        this.roverPort = roverPort;
    }

    public boolean isIgnoreEmbeddedKLV() {
        return ignoreEmbeddedKLV;
    }

    public void setIgnoreEmbeddedKLV(boolean ignoreEmbeddedKLV) {
        this.ignoreEmbeddedKLV = ignoreEmbeddedKLV;
    }

    public String getPreferredMacAddress() {
        return preferredMacAddress;
    }

    public void setPreferredMacAddress(String preferredMacAddress) {
        this.preferredMacAddress = preferredMacAddress;
    }

    public String getPreferredInterfaceAddress() {
        return preferredInterfaceAddress;
    }

    public void setPreferredInterfaceAddress(String preferredInterfaceAddress) {
        this.preferredInterfaceAddress = preferredInterfaceAddress;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getBuffer() {
        return buffer;
    }

    public void setBuffer(int buffer) {
        this.buffer = buffer;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRtspReliable() {
        return rtspReliable;
    }

    public void setRtspReliable(int rtspReliable) {
        this.rtspReliable = rtspReliable;
    }
}
