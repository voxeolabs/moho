package com.voxeo.moho.event;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * <p>This event is fired when an application/dtmf-relay message is sent to an {@link EventSource}
 * typically via a SIP INFO message.</p>
 * 
 * <p>An example message looks something like this:<p/>
 * 
 * <pre>
 * INFO sip:7007471000@example.com SIP/2.0
 * Via: SIP/2.0/UDP alice.uk.example.com:5060
 * From: <sip:7007471234@alice.uk.example.com>;tag=d3f423d
 * To: <sip:7007471000@example.com>;tag=8942
 * Call-ID: 312352@myphone
 * CSeq: 5 INFO
 * Content-Length: 24
 * Content-Type: application/dtmf-relay
 * 
 * Signal=5
 * Duration=160
 * </pre>
 * 
 * @author jdecastro
 *
 */
public class DtmfRelayEvent extends InputDetectedEvent {

    /**
     * @param source The {@link EventSource} on which the event occurred
     * @param spec Payload in application/dtmf-relay form
     * @throws IllegalArgumentException Thrown is the spec does not comply with application/dtmf-relay format
     */
    public DtmfRelayEvent(EventSource source, String spec) {
        super(source, resolveSignal(spec));
    }

    private static String resolveSignal(String spec) {
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(spec.getBytes()));
            String signal = properties.getProperty("Signal");
            if (signal == null) {
                throw new IllegalArgumentException("Missing 'Signal' in dtmf-relay [spec=" + spec + "]");
            }
            return signal;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to parse dtmf-relay [spec=" + spec + "]");
        }
    }

}
