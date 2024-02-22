package io.opentakserver.opentakicu.cot;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Contact {
    private String callsign;

    public Contact(String callsign) {
        this.callsign = callsign;
    }

    @JacksonXmlProperty(isAttribute = true)
    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }
}
