package com.voxeo.rayo.client.samples;

import com.voxeo.rayo.client.RayoClient;
import com.voxeo.rayo.client.listener.StanzaListener;
import com.voxeo.rayo.client.xmpp.stanza.Error;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Message;
import com.voxeo.rayo.client.xmpp.stanza.Presence;

public abstract class BaseSample {

	protected RayoClient client;
	
	public void connect(String xmppServer, String username, String password, String rayoServer) throws Exception {
		
		client = new RayoClient(xmppServer, rayoServer);
		login(username,password,"voxeo");
	}
	
	public void shutdown() throws Exception {
		
		client.disconnect();
	}
		
	void login(String username, String password, String resource) throws Exception {
		
		client.connect(username, password, resource);
		
		client.addStanzaListener(new StanzaListener() {
			
			@Override
			public void onPresence(Presence presence) {

				//System.out.println(String.format("Message from server: [%s]",presence));
			}
			
			@Override
			public void onMessage(Message message) {

				//System.out.println(String.format("Message from server: [%s]",message));
			}
			
			@Override
			public void onIQ(IQ iq) {

				//System.out.println(String.format("Message from server: [%s]",iq));				
			}
			
			@Override
			public void onError(Error error) {

				//System.out.println(String.format("Message from server: [%s]",error));
			}
		});
	}
}
