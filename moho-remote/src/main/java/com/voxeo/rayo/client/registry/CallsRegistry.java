package com.voxeo.rayo.client.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This registry stores information for calls. 
 * 
 * @author martin
 *
 */
public class CallsRegistry {

	private Map<String, Call> callsMap = new ConcurrentHashMap<String, Call>(50);
	
	public void registerCall(String callId, Call call) {
		
		callsMap.put(callId, call);
	}
	
	public Call get(String callId) {
		
		return callsMap.get(callId);
	}
	
	public void unregisterCal(String callId) {
		
		callsMap.remove(callId);
	}
}
