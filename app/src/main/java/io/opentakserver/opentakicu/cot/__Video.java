package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class __Video {
    private String url;
    private String uid;

    public __Video(String url, String uid) {
        this.url = url;
        this.uid = uid;
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
}
