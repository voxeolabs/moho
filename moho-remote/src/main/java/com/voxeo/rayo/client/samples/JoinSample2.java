package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.VerbRef;
import com.rayo.core.JoinDestinationType;


public class JoinSample2 extends BaseSample {

	public void run() throws Exception {
		
		connect("192.168.1.34", "usera", "1", "localhost");
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Thread.sleep(3000);
		//dial and get a call ref
		VerbRef result = client.dial(new URI("sip:usera@192.168.1.34:5060"), new URI("sip:mpermar@iptel.org"));
		client.waitFor("answered");
		Thread.sleep(3000);
		client.join(result.getVerbId(), "direct", "duplex", JoinDestinationType.CALL, callId);
		Thread.sleep(10000);
		Thread.sleep(3000);
		client.unjoin(result.getVerbId(), JoinDestinationType.CALL, callId);
		client.hangup(callId);
	}
		
	public static void main(String[] args) throws Exception {
		
		new JoinSample2().run();
	}
}
