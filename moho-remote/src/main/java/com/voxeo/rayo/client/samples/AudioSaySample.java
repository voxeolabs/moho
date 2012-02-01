package com.voxeo.rayo.client.samples;

import java.net.URI;

public class AudioSaySample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		//client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		client.say(new URI("file:///tmp/folders/Ec/Ec0JM4nCEDSkXKbgTXk++++++TI/-Tmp-/rayo190304528159119832.mp3"), callId);
		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AudioSaySample sample = new AudioSaySample();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
