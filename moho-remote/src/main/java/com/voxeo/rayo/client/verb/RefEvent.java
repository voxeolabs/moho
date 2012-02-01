package com.voxeo.rayo.client.verb;

import com.rayo.core.verb.AbstractVerbEvent;
import com.rayo.core.verb.Verb;

public class RefEvent extends AbstractVerbEvent {

	private String jid;

	public RefEvent() {}
	
    public RefEvent(Verb verb) {

    	super(verb);
    }
    
    public RefEvent(Verb verb, String jid) {
        
    	this(verb);
    	this.jid = jid;
    }

	public String getJid() {
		return jid;
	}

	public void setJid(String jid) {
		this.jid = jid;
	}

    

}
