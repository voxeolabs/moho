package com.voxeo.rayo.client.internal;

import org.junit.After;
import org.junit.Before;

import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.test.config.TestConfig;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;

public abstract class XmppIntegrationTest {

	protected RayoClient rayo;
	private String username = "userc";
	private NettyServer server;
	
	protected String lastCallId;
		
	@Before
	public void setUp() throws Exception {
		
		server = NettyServer.newInstance(TestConfig.port);

		rayo = new RayoClient(createConnection(TestConfig.serverEndpoint, TestConfig.port), TestConfig.serverEndpoint);
		login(username, "1", "voxeo");
		
		rayo.addStanzaListener(new RayoMessageListener("offer") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				lastCallId = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
			}
		});
		
		server.sendRayoOffer();
		// Let the offer come back and be caught by the RayoClient's listener
		Thread.sleep(300);
	}
		
	public void assertServerReceived(String message) {
		
		message = message.replaceAll("#callId", lastCallId);
		server.assertReceived(message);
	}
	
	@After
	public void dispose() throws Exception {
		
		rayo.disconnect();
	}
		
	void login(String username, String password, String resource) throws Exception {
		
		rayo.connect(username, password, resource);		
	}
	
	void disconnect() throws Exception {
		
		rayo.disconnect();
	}
	
	protected void setUsername(String username) {
		
		this.username = username;
	}
	
	protected abstract XmppConnection createConnection(String hostname, Integer port);
}
