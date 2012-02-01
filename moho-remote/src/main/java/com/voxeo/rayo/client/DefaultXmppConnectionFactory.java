package com.voxeo.rayo.client;

public class DefaultXmppConnectionFactory implements XmppConnectionFactory {

	@Override
	public XmppConnection createConnection(String hostname, Integer port) {

		return new SimpleXmppConnection(hostname, port);
	}
}
