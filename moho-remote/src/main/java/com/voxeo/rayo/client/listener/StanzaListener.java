package com.voxeo.rayo.client.listener;

import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;

public interface StanzaListener {

	public void onIQ(IQ iq);
	
	public void onMessage(Message message);
	
	public void onPresence(Presence presence);
	
	public void onError(Error error);
}
