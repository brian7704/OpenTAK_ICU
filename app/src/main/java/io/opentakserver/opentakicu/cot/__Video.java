package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class __Video {
    private String url;
    private String uid;
    private ConnectionEntry ConnectionEntry;

    public __Video(String url, String uid, ConnectionEntry ConnectionEntry) {
        this.url = url;
        this.uid = uid;
        this.ConnectionEntry = ConnectionEntry;
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
    public io.opentakserver.opentakicu.cot.ConnectionEntry getConnectionEntry() {
        return ConnectionEntry;
    }

    public void setConnectionEntry(io.opentakserver.opentakicu.cot.ConnectionEntry connectionEntry) {
        ConnectionEntry = connectionEntry;
    }
}
