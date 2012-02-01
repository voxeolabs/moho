package com.voxeo.rayo.client.io;

import java.io.Reader;

import com.voxeo.rayo.client.XmppConnectionListener;
import com.voxeo.rayo.client.XmppException;
import com.voxeo.rayo.client.auth.AuthenticationSupport;
import com.voxeo.rayo.client.filter.XmppObjectFilterSupport;
import com.voxeo.rayo.client.listener.StanzaListener;


public interface XmppReader extends XmppObjectFilterSupport, AuthenticationSupport {

	public void init(Reader reader) throws XmppException;
	public void start() throws XmppException;
	
	public void close() throws XmppException;
	
	public void addXmppConnectionListener(XmppConnectionListener listener);
	public void removeXmppConnectionListener(XmppConnectionListener listener);
	
    public void addStanzaListener(StanzaListener stanzaListener);
    public void removeStanzaListener(StanzaListener stanzaListener);

}
