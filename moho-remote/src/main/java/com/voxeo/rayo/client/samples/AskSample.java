package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.AskCompleteEvent;

public class AskSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.ask("Welcome to Orlando Bank. Please enter your five digits number.", "[4-5 DIGITS]", callId);
		
		AskCompleteEvent complete = (AskCompleteEvent)client.waitFor("complete");
		System.out.println("Success: " + complete.isSuccess());

		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AskSample sample = new AskSample();
		sample.connect("localhost", "user100", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
