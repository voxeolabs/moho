package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.CallRef;
import com.rayo.core.JoinDestinationType;


public class JoinSample1 extends BaseSample {

	public void run() throws Exception {
		
		connect("localhost", "usera", "1", "localhost");
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Thread.sleep(3000);
		//dial and get a call ref
		CallRef result = client.dial(new URI("sip:userc@127.0.0.1:5060"), new URI("sip:mperez@127.0.0.1:3060"));
		client.waitFor("answered");
		Thread.sleep(3000);
		client.join(result.getCallId(), "direct", "duplex", JoinDestinationType.CALL, callId);
		Thread.sleep(3000);
		client.unjoin(result.getCallId(), JoinDestinationType.CALL, callId);
		Thread.sleep(6000);
		client.say("now you can talk", callId);
		Thread.sleep(10000);
		client.hangup(callId);
	}
		
	public static void main(String[] args) throws Exception {
		
		new JoinSample1().run();
	}
}
