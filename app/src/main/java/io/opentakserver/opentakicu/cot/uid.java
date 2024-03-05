package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class uid {
    private String Droid;

    public uid(String droid) {
        Droid = droid;
    }

    @JacksonXmlProperty(isAttribute=true)
    public String getDroid() {
        return Droid;
    }

    public void setDroid(String droid) {
        Droid = droid;
    }
}
