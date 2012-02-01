package com.voxeo.rayo.client.xmpp.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.rayo.client.xmpp.Namespaces;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class Response extends AbstractXmppObject {

    public Response() {
    	
    	super(Namespaces.SASL);
    }
    
    public Response(String data) {
    	
    	this();
        setText(data);
    }
    
	public Response(Element element) {
		
    	this();
		setElement(element);
	}
    
    public Response setText(String text) {
    	
    	set(text);
    	return this;
    }
    
    public String getText() {
    	
    	return text();
    }
    
    @Override
    public String getStanzaName() {

    	return "response";
    }
}
