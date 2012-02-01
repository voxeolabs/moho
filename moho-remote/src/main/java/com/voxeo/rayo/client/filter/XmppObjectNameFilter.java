package com.voxeo.rayo.client.filter;

import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class XmppObjectNameFilter extends AbstractXmppObjectFilter {

	private String name;
	
	public XmppObjectNameFilter(String name) {
		
		this.name = name;
	}
	
	@Override
	public AbstractXmppObject doFilter(AbstractXmppObject object) {

		if (object != null) {
			if (name.equalsIgnoreCase(object.getStanzaName())) {
				return object;
			}
		}
		return null;
			
	}
}
