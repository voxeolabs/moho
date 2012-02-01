package com.voxeo.rayo.client;

import com.voxeo.rayo.client.auth.AuthenticationSupport;
import com.voxeo.rayo.client.filter.XmppObjectFilterSupport;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.listener.StanzaListenerSupport;
import com.voxeo.rayo.client.response.ResponseHandler;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.XmppObject;


public interface XmppConnection extends XmppObjectFilterSupport, AuthenticationSupport, StanzaListenerSupport {

	public ConnectionConfiguration getConfiguration();
	public void connect() throws XmppException;
	public void connect(int timeout) throws XmppException;
	public void disconnect() throws XmppException;
	public void send(XmppObject object) throws XmppException;
	public void send(XmppObject object, ResponseHandler handler) throws XmppException;
	public XmppObject sendAndWait(XmppObject object) throws XmppException;
	public XmppObject sendAndWait(XmppObject object, int timeout) throws XmppException;
	public void login(String username, String password, String resourceName) throws XmppException;
	public void login(String username, String password, String resourceName, int timeout) throws XmppException;

	public String getConnectionId();
	public String getServiceName();
	public boolean isConnected();
	public boolean isAuthenticated();

	public void addXmppConnectionListener(XmppConnectionListener connectionListener);
	public void removeXmppConnectionListener(XmppConnectionListener connectionListener);
	
	XmppObject waitFor(String node) throws XmppException;
	XmppObject waitFor(String node, Integer timeout) throws XmppException;
	Extension waitForExtension(String extensionName) throws XmppException;
	Extension waitForExtension(String extensionName, Integer timeout) throws XmppException;

	public String getUsername();
	public String getResource();
}
