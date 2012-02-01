package com.voxeo.rayo.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rayo.core.AnswerCommand;
import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.SimpleXmppConnection;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.internal.NettyServer;
import com.voxeo.rayo.client.test.config.TestConfig;
import com.voxeo.rayo.client.test.util.MockResponseHandler;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.Bind;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Error.Condition;
import com.voxeo.rayo.client.xmpp.stanza.Error.Type;

public class ConnectionTest {
	
	private XmppConnection connection;

	@Before
	public void setUp() throws Exception {
		
		 NettyServer.newInstance(TestConfig.port);
	}

	@Test
	public void testSendFailsOnNotAuthenticated() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);
		connection.connect();
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		try {
			connection.send(iq);
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Not authenticated. You need to authenticate first.");
			assertEquals(xe.getError().getCondition(), Condition.not_authorized);
			assertEquals(xe.getError().getType(), Type.cancel);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testSendFailsOnNotConnected() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);
		
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		try {
			connection.send(iq);
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Not connected to the server. You need to connect first.");
			assertEquals(xe.getError().getCondition(), Condition.service_unavailable);
			assertEquals(xe.getError().getType(), Type.cancel);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testSendFailsOnNonExistentServer() throws Exception {
		
		connection = createConnection("1234", TestConfig.port);
		
		try {
			connection.connect();
		} catch (XmppException xe) {
			assertEquals(xe.getMessage(), "Error while connecting to 1234:10299");
			assertEquals(xe.getError().getCondition(), Condition.service_unavailable);
			return;
		}
		fail("Expected exception");
	}

	@Test
	public void testWaitForWithTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		long time = System.currentTimeMillis();
		XmppObject object = null;
		try {
			object = connection.waitFor("something", 100);
		} catch (XmppException xe) {
			assertTrue(xe.getMessage().startsWith("Timed out"));
			long timeoff = System.currentTimeMillis();
			assertNull(object);
			assertTrue(timeoff - time >= 100 && timeoff - time < 300);
			return;
		}
		fail("Expected timeout exception");
	}	

	@Test
	public void testWaitForDefaultTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		long time = System.currentTimeMillis();
		XmppObject object = null;
		try {
			connection.waitFor("something");
		} catch (XmppException xe) {
			assertTrue(xe.getMessage().startsWith("Timed out"));
			long timeoff = System.currentTimeMillis();
			assertNull(object);
			return;
		}
		fail("Expected timeout exception");
	}	

	@Test
	public void testWaitForExtensionWithTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		long time = System.currentTimeMillis();
		XmppObject object = null;
		try {
			object = connection.waitForExtension("something", 100);
		} catch (XmppException xe) {
			assertTrue(xe.getMessage().startsWith("Timed out"));
			long timeoff = System.currentTimeMillis();
			assertNull(object);
			assertTrue(timeoff - time >= 100 && timeoff - time < 300);
			return;
		}
		fail("Expected timeout exception");
	}	

	@Test
	public void testWaitForExtensionDefaultTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		long time = System.currentTimeMillis();
		XmppObject object = null;
		try {
			object = connection.waitForExtension("something");
		} catch (XmppException xe) {
			assertTrue(xe.getMessage().startsWith("Timed out"));
			long timeoff = System.currentTimeMillis();
			assertNull(object);
			System.out.println("Test time: " + (timeoff - time));
			assertTrue(timeoff - time >= 100 && timeoff - time < 300);
			return;
		}
		fail("Expected timeout exception");
	}
	
	@Test
	public void testSendAndWaitWithTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));

		long time = System.currentTimeMillis();
		XmppObject object = connection.sendAndWait(iq,100);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);		
	}	

	@Test
	public void testSendAndWaitWithDefaultTimeout() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		((SimpleXmppConnection)connection).setDefaultTimeout(100);
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));

		long time = System.currentTimeMillis();
		XmppObject object = connection.sendAndWait(iq);
		long timeoff = System.currentTimeMillis();
		assertNull(object);
		assertTrue(timeoff - time >= 100 && timeoff - time < 300);		
	}

	@Test
	public void testSendWithResponseHandler() throws Exception {
		
		connection = createConnection(TestConfig.serverEndpoint, TestConfig.port);		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		
		MockResponseHandler handler = new MockResponseHandler();
		IQ iq = new IQ(IQ.Type.set)
			.setChild(new Bind().setResource("clienttest"));
		assertEquals(handler.getHandled(),0);
		connection.send(iq, handler);
		Thread.sleep(500);
		assertEquals(handler.getHandled(),1);
		
		AnswerCommand answer = new AnswerCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom("userc@localhost")
			.setTo("something")
			.setChild(Extension.create(answer));
		connection.send(iq,handler);
		Thread.sleep(500);
		assertEquals(handler.getHandled(),2);
	}
	
	@After
	public void shutdown() throws Exception {
		
		connection.disconnect();
	}
	
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
