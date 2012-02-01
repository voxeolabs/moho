package com.voxeo.rayo.client.xmpp.stanza.sasl;

import org.dom4j.Element;

import com.voxeo.rayo.client.xmpp.Namespaces;
import com.voxeo.rayo.client.xmpp.stanza.AbstractXmppObject;

public class Failure extends AbstractXmppObject {

	private String condition;
	
	public Failure() {
		
		super(Namespaces.SASL);
	}
	
	public Failure(String condition) {
		
		this();
		
		this.condition = condition;
		set(condition, "");
	}
	
	public Failure(Element element) {
		
		this();
		setElement(element);
	}
	
	@Override
	public String getStanzaName() {

		return "failure";
	}
	
    public String getCondition() {

    	if (condition == null) {
    		// try to find it
    		String child = getChildName();
    		return child;
    	}
    	return condition;
    }
    
    public Failure setCondition(String condition) {
    	
    	set(condition,"");
    	return this;
    }
}
