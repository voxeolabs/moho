package com.voxeo.rayo.client.xmpp.stanza;

import com.voxeo.rayo.client.xmpp.Namespaces;


public class Ping extends AbstractXmppObject {

	public static final String NAME = "ping";
	
	public Ping() {
		
		super(Namespaces.PING);
	}
		
	@Override
	public String getStanzaName() {

		return NAME;
	}
	
	@Override
	public Ping copy() {

		Ping ping = new Ping();
		ping.copy(this);
		return ping;
	}
}
