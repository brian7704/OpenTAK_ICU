package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class __Video {
    private String url;
    private String uid;
    private ConnectionEntry connectionEntry;

    public __Video(String url, String uid, ConnectionEntry connectionEntry) {
        this.url = url;
        this.uid = uid;
        this.connectionEntry = connectionEntry;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    @JacksonXmlProperty(localName = "ConnectionEntry")
    public ConnectionEntry getConnectionEntry() {
        return connectionEntry;
    }

    public void setConnectionEntry(io.opentakserver.opentakicu.cot.ConnectionEntry ConnectionEntry) {
        this.connectionEntry = ConnectionEntry;
    }
}
