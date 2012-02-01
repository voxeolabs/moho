package com.voxeo.rayo.client.test;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.voxeo.rayo.client.SimpleXmppConnection;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.NettyServer;
import com.voxeo.rayo.client.test.config.TestConfig;
import com.voxeo.rayo.client.test.util.MockStanzaListener;
import com.voxeo.rayo.client.xmpp.stanza.Bind;
import com.voxeo.rayo.client.xmpp.stanza.IQ;

public class StanzaListenerTest {
	
	protected XmppConnection connection;

	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testRegisterStanzaListener() throws Exception {

		setConnection();
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@127.0.0.1")
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);

		// Wait for a response
		Thread.sleep(150);
		
		assertEquals(stanzaListener.getEventsCount(),1);
		assertNotNull(stanzaListener.getLatestIQ());
		assertEquals(stanzaListener.getLatestIQ().getId(),iq.getId());
		
		connection.disconnect();
	}


	@Test
	public void testUnregisterStanzaListener() throws Exception {

		setConnection();
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		MockStanzaListener stanzaListener = new MockStanzaListener();
		connection.addStanzaListener(stanzaListener);
		connection.removeStanzaListener(stanzaListener);
		
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@127.0.0.1")
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);

		// Wait for a response
		Thread.sleep(150);
		
		assertEquals(stanzaListener.getEventsCount(),0);		
		connection.disconnect();
	}
	
	@After
	public void shutdown() throws Exception {

		connection.disconnect();
	}
	
	protected void setConnection() {
		
		connection = new SimpleXmppConnection(TestConfig.serverEndpoint, TestConfig.port);
	}
}
