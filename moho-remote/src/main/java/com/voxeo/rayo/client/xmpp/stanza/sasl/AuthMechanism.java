package com.voxeo.rayo.client.xmpp.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.rayo.client.xmpp.Namespaces;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class AuthMechanism extends AbstractXmppObject {

	public AuthMechanism() {
		
		super(Namespaces.SASL);
	}
	
	public AuthMechanism(Element element) {
		
		this();
		setElement(element);
	}
	
    public AuthMechanism(Type name, String authenticationText) {
    	
    	this();
    	
        if (name == null) {
            throw new IllegalArgumentException("SASL mechanism name shouldn't be null.");
        }
        setMechanism(name);
        set(authenticationText);
    }
    
    public AuthMechanism setMechanism(Type mechanism) {
    	
    	setAttribute("mechanism", mechanism.toString());
    	return this;
    }
    
    public String getMechanism() {
    	
    	return attribute("mechanism");
    }
    
    
    public AuthMechanism setText(String text) {
    	
    	set(text);
    	return this;
    }
    
    public String getText() {
    	
    	return text();
    }
    
    @Override
    public String getStanzaName() {

    	return "auth";
    }
    
    public enum Type {
    	
    	//TODO: Add more modes
    	PLAIN
    }
}
