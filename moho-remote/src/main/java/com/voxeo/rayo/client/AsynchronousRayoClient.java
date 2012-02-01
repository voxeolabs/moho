package com.voxeo.rayo.client;

import com.voxeo.rayo.client.xmpp.stanza.IQ;

/**
 * This class is an asynchronousr Rayo Client. Basically extends the base class
 * RayoClient but all the operations invoked will be asynchronous.
 * 
 * @author martin
 *
 */ 
public class AsynchronousRayoClient extends RayoClient {

	
	public AsynchronousRayoClient(String server, Integer port, String rayoServer) {
		super(server, port, rayoServer);
	}

	public AsynchronousRayoClient(String server, String rayoServer) {
		super(server, rayoServer);
	}

	@Override
	protected IQ sendIQ(IQ iq) throws XmppException {

		connection.send(iq);
		return null;
	}
}
