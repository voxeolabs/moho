package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.VerbRef;

public class PlaybackSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);

		VerbRef say = client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"), callId);
		Thread.sleep(10000);
		client.pause(say);
		Thread.sleep(10000);
		client.resume(say);
		Thread.sleep(10000);
		client.stop(say);
		Thread.sleep(10000);		

		client.hangup(callId);
	}
		
	public static void main(String[] args) throws Exception {
		
		PlaybackSample sample = new PlaybackSample();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
