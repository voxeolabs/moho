package com.voxeo.rayo.client.response;

import com.voxeo.rayo.client.xmpp.stanza.XmppObject;

public interface ResponseHandler {

	public void handle(XmppObject response);
}
