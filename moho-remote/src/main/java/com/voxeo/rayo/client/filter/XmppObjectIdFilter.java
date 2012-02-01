package com.voxeo.rayo.client.filter;

import com.voxeo.rayo.client.response.ResponseHandler;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class XmppObjectIdFilter extends AbstractXmppObjectFilter {

	private String id;
	private ResponseHandler handler;
	
	public XmppObjectIdFilter(String id, ResponseHandler handler) {
		
		this.id = id;
		this.handler = handler;
	}
	
	public XmppObjectIdFilter(String id) {
		
		this(id,null);
	}
	
	@Override
	public AbstractXmppObject doFilter(AbstractXmppObject object) {

		if (id.equals(object.getId())) {
			if (handler != null) {
				handler.handle(object);
			}
			return object;
		}
		return null;
	}
}
