package com.voxeo.rayo.client.auth;

import com.voxeo.rayo.client.XmppException;

public interface AuthenticationHandler extends AuthenticationListener {

	public void login(String username, String password, String resource, int timeout) throws XmppException;
	
	public boolean isAuthenticated();
}
