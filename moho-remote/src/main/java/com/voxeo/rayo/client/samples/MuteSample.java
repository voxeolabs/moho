package com.voxeo.rayo.client.samples;


public class MuteSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.say("Muting the call", callId);
		Thread.sleep(5000);
		client.mute(callId);
		Thread.sleep(3000);
		client.say("Whats app!!", callId);
		Thread.sleep(5000);
		client.unmute(callId);
		Thread.sleep(3000);
		client.say("We are back to normal", callId);
		Thread.sleep(5000);
		client.hangup(callId);
		System.out.println("Wait for complete");
		Thread.sleep(2000);
	}
	
	public static void main(String[] args) throws Exception {
		
		MuteSample sample = new MuteSample();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
