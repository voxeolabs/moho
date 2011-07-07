package com.voxeo.moho.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class DtmfRelayEventTest {

    @Test
    public void goodSimpleSpec() {
        String spec = "Signal=7";
        DtmfRelayEvent event = new DtmfRelayEvent(null, spec);
        assertEquals("7", event.getInput());
    }

    @Test
    public void goodFullSpec() {
        String spec = "\nDuration=160\nSignal=7";
        DtmfRelayEvent event = new DtmfRelayEvent(null, spec);
        assertEquals("7", event.getInput());
    }

    @Test
    public void badSpecMissingSignal() {
        String spec = "\nDuration=160";
        try {
            new DtmfRelayEvent(null, spec);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                return;
            }
        }
        fail("Did not get expected exception");
    }

    @Test
    public void badSpecInvalidFormat() {
        String spec = "\nSignal>160";
        try {
            new DtmfRelayEvent(null, spec);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                return;
            }
        }
        fail("Did not get expected exception");
    }

}
