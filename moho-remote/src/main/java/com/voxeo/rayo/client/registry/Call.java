package com.voxeo.rayo.client.registry;

/**
 * Record information for a call
 * 
 * @author martin
 *
 */
public class Call {

	private String callId;
	private Object callDomain;

	public Call(String callId, String callDomain) {
		
		this.callId = callId;
		this.callDomain = callDomain;
	}

	public String getCallId() {
		return callId;
	}

	public void setCallId(String callId) {
		this.callId = callId;
	}

	public Object getCallDomain() {
		return callDomain;
	}

	public void setCallDomain(Object callDomain) {
		this.callDomain = callDomain;
	}
}
