package com.voxeo.rayo.client.samples;

import java.net.URI;


public class HoldSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.say("Putting the call on hold", callId);
		client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"), callId);

		Thread.sleep(5000);
		client.hold(callId);
		Thread.sleep(3000);
		//client.say("Unholding call");
		Thread.sleep(5000);
		client.unhold(callId);
		Thread.sleep(3000);
		client.say("We are back to normal", callId);
		Thread.sleep(5000);
		client.hangup(callId);
		System.out.println("Wait for complete");
		Thread.sleep(2000);
	}
	
	public static void main(String[] args) throws Exception {
		
		HoldSample sample = new HoldSample();
		sample.connect("192.168.1.33", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
