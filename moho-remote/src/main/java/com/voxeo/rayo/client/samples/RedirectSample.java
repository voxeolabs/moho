package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.RedirectCommand;

public class RedirectSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		//client.answer();
		//client.dial(new URI("sip:192.168.1.34:5060"));
		
		RedirectCommand redirect = new RedirectCommand();
		redirect.setTo(new URI("sip:mperez@127.0.0.1:3060"));
		client.command(redirect, callId);
		Thread.sleep(60000);
		client.hangup(callId);
	}
		
	public static void main(String[] args) throws Exception {
		
		RedirectSample sample = new RedirectSample();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
