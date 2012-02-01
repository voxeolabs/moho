package com.voxeo.rayo.client.xmpp.stanza;

import com.voxeo.rayo.client.xmpp.Namespaces;

public class Session extends AbstractXmppObject {

	public static final String NAME = "session";
	
	public Session() {
		
		super(Namespaces.SESSION);
	}
	
	@Override
	public String getStanzaName() {

		return NAME;
	}
	
	@Override
	public XmppObject copy() {

		Session session = new Session();
		session.copy(this);
		return session;
	}
}
