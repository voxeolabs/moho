package com.voxeo.rayo.client.test;

import org.junit.Test;

import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.XmppIntegrationTest;

public class AnswerTest extends XmppIntegrationTest {
	
	@Test
	public void testAnswer() throws Exception {
		
		rayo.answer(lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><answer xmlns=\"urn:xmpp:rayo:1\"></answer></iq>");
	}
	
	@Override
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
