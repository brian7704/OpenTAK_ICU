package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Cot {
    String username;
    String password;
    String uid;

    public Cot(String username, String password, String uid) {
        this.username = username;
        this.password = password;
        this.uid = uid;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
