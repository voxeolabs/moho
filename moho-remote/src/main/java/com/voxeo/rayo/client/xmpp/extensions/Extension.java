package com.voxeo.rayo.client.xmpp.extensions;

import org.dom4j.Element;

import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class Extension extends AbstractXmppObject {

	public static Extension create(Object object) throws ProviderException {
		
		return ExtensionsManager.buildExtension(object);
	}
	
	public Extension(Element element) {
		
		super(element);
	}
	
	@Override
	public String getStanzaName() {

		return getRootName();
	}

	public Object getObject() {
		
		return ExtensionsManager.unmarshall(this);
	}
	
	public <T> T to(Class<T> clazz) {

		return ExtensionsManager.unmarshall(this, clazz);
	}
}
