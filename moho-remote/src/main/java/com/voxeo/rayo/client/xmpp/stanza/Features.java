package com.voxeo.rayo.client.xmpp.stanza;

import org.dom4j.Element;

public class Features extends AbstractXmppObject {

	public Features() {
		
	}
	
	public Features(Element element) {
		
		this();
		setElement(element);
	}
	
	@Override
	public String getStanzaName() {

		return "features";
	}
}
