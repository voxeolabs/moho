package com.voxeo.rayo.client.test;

import org.junit.Test;

import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.XmppIntegrationTest;

public class AskTest extends XmppIntegrationTest {
	
	@Test
	public void testAsk() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.ask("What's your favorite colour?", "red,green", lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><ask xmlns=\"urn:xmpp:tropo:ask:1\" min-confidence=\"0.3\" mode=\"dtmf\" terminator=\"#\" timeout=\"650000\" bargein=\"true\"><prompt>What's your favorite colour?</prompt><choices content-type=\"application/grammar+voxeo\"><![CDATA[red,green]]></choices></ask></iq>");
	}	
	
	
	@Override
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
