package com.voxeo.rayo.client.listener;

import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;

public abstract class StanzaAdapter implements StanzaListener {

	@Override
	public void onIQ(IQ iq) {
		
	}
	
	@Override
	public void onMessage(Message message) {
		
	}
	
	@Override
	public void onPresence(Presence presence) {
		
	}
	
	@Override
	public void onError(Error error) {
		
	}
}
