package com.voxeo.rayo.client.test;

import org.junit.Test;

import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.XmppIntegrationTest;

public class ConferenceTest extends XmppIntegrationTest {
	
	@Test
	public void testConference() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.conference("123456", lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><conference xmlns=\"urn:xmpp:tropo:conference:1\" name=\"123456\" mute=\"false\" terminator=\"#\" tone-passthrough=\"true\" beep=\"true\" moderator=\"true\"></conference></iq>");
	}
	
	
	@Override
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
