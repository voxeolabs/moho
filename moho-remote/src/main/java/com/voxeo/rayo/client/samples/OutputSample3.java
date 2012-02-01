package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Output;
import com.rayo.core.verb.Ssml;

public class OutputSample3 extends BaseSample {

	public void run() throws Exception {

		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Output output = new Output();
		output.setPrompt(new Ssml("<speak><say-as interpret-as=\"ordinal\">100</say-as></speak>"));
		client.output(output, callId);
		Thread.sleep(5000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		OutputSample3 sample = new OutputSample3();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
