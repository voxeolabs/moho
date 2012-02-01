package com.voxeo.rayo.client.samples;


public class AcceptSample extends BaseSample {

	public void run() throws Exception {

		// Two answers should be NOOP
		
		String callId = client.waitForOffer().getCallId();
		client.accept(callId);
		client.accept(callId);

		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AcceptSample sample = new AcceptSample();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
