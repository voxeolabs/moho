package com.voxeo.rayo.client.io;

import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;

public interface XmppWriter {

	public void openStream(String serviceName) throws XmppException;
	
	public void write(XmppObject object) throws XmppException;
	
	public void write(String string) throws XmppException;

	public void close() throws XmppException;
}
