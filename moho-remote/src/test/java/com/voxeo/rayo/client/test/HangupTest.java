package com.voxeo.rayo.client.test;

import org.junit.Test;

import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.XmppIntegrationTest;

public class HangupTest extends XmppIntegrationTest {
	
	@Test
	public void testHangup() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.hangup(lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><hangup xmlns=\"urn:xmpp:rayo:1\"></hangup></iq>");
	}
	
	@Override
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
