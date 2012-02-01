package com.voxeo.rayo.client.samples;

public class DialSample4a extends BaseSample {

	public void run() throws Exception {
				
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.say("Thank you for calling. I'm going now to hang up" ,callId);
		client.hangup(callId);
		Thread.sleep(100);
	}
		
	public static void main(String[] args) throws Exception {
		
		System.out.println("CLIENT A");
		System.out.println("--------------");
		DialSample4a sample = new DialSample4a();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
