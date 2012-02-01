package com.voxeo.rayo.client.samples;

import java.net.URI;

public class AudioSaySample2 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);

		// Does not exist. It should throw an error
		client.say(new URI("http://ccmixter.org/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"), callId);

		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AudioSaySample2 sample = new AudioSaySample2();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
