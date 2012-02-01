package com.voxeo.rayo.client.auth;

import java.util.Collection;

import com.voxeo.rayo.client.xmpp.stanza.sasl.Challenge;
import com.voxeo.rayo.client.xmpp.stanza.sasl.Failure;
import com.voxeo.rayo.client.xmpp.stanza.sasl.Success;

public interface AuthenticationListener {

	public void authSettingsReceived(Collection<String> mechanisms);
	
	public void authSuccessful(Success success);
	
	public void authFailure(Failure failure);
	
	public void authChallenge(Challenge challenge);
	
	public void authBindingRequired();
	
	public void authSessionsSupported();
}
