package com.voxeo.rayo.client.filter;

import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;

public class XmppObjectExtensionNameFilter extends AbstractXmppObjectFilter {

	private String name;
	private String from;
	
	public XmppObjectExtensionNameFilter(String name) {
		
		this.name = name;
	}

	
	public XmppObjectExtensionNameFilter(String name, String from) {
		
		this.name = name;
		this.from = from;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public AbstractXmppObject doFilter(AbstractXmppObject object) {

		if (object instanceof Stanza) {
			Stanza stanza = (Stanza)object;
			if (stanza.hasExtension()) {
				if (name.equalsIgnoreCase(stanza.getExtension().getStanzaName())) {
					if (from != null) {
						if (stanza.getFrom().equals(from)) {
							return object;
						}
					} else {
						return object;
					}
				}
			} else {
				if (stanza.getChildName() != null) {
					// will still handle default stanzas as extensions for the sake of this filter
					if (name.equalsIgnoreCase(stanza.getChildName())) {
						if (from != null) {
							if (stanza.getFrom().equals(from)) {
								return object;
							}
						} else {
							return object;
						}
					}
				}
			}
		}
		return null;
	}
}
