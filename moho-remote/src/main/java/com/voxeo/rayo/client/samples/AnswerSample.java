package com.voxeo.rayo.client.samples;


public class AnswerSample extends BaseSample {

	public void run() throws Exception {

		// Two answers should be NOOP
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);

		Thread.sleep(5000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AnswerSample sample = new AnswerSample();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
