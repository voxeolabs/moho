package com.voxeo.rayo.client.test;


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.NettyServer;
import com.voxeo.rayo.client.test.config.TestConfig;
import com.voxeo.rayo.client.test.util.MockConnectionListener;
import com.voxeo.rayo.client.xmpp.stanza.Bind;
import com.voxeo.rayo.client.xmpp.stanza.IQ;

public class ConnectionListenerTest {
	
	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testRegisterConnectionListener() throws Exception {

		XmppConnection connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);
		MockConnectionListener mockConnectionListener = new MockConnectionListener();
		connection.addXmppConnectionListener(mockConnectionListener);
		connection.connect();

		// Wait a little bit
		Thread.sleep(150);
		
		assertEquals(mockConnectionListener.getEstablishedCount(),1);
		assertEquals(mockConnectionListener.getFinishedCount(),0);
		
		connection.disconnect();

		assertEquals(mockConnectionListener.getFinishedCount(),1);
	}

	@Test
	public void testUnregisterConnectionListener() throws Exception {

		XmppConnection connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);
		MockConnectionListener mockConnectionListener = new MockConnectionListener();
		connection.addXmppConnectionListener(mockConnectionListener);
		connection.removeXmppConnectionListener(mockConnectionListener);
		connection.connect();

		// Wait a little bit
		Thread.sleep(150);
		
		assertEquals(mockConnectionListener.getEstablishedCount(),0);
		
		connection.disconnect();

		assertEquals(mockConnectionListener.getFinishedCount(),0);
	}

	@Test
	public void testMessageSent() throws Exception {

		XmppConnection connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);
		MockConnectionListener mockConnectionListener = new MockConnectionListener();
		connection.addXmppConnectionListener(mockConnectionListener);
		connection.connect();
		connection.login("userc","1","voxeo");
		
		int count = mockConnectionListener.getSent();
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		connection.send(iq);
		
		// Wait a little bit
		Thread.sleep(150);
		
		assertEquals(mockConnectionListener.getSent(),count+1);		
		connection.disconnect();
	}
	
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
