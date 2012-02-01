package com.voxeo.rayo.client.xmpp.extensions;

public interface Provider {

	public Object deserialize(String xml) throws ProviderException;
	
	public String serialize(Object object) throws ProviderException;
}
