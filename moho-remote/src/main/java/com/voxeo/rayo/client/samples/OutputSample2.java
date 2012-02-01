package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Output;
import com.rayo.core.verb.Ssml;

public class OutputSample2 extends BaseSample {

	public void run() throws Exception {
		// Invalid SSML. Should send an error
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Output output = new Output();
		output.setPrompt(new Ssml("<output-as interpret-as=\"ordinal\">100</output-as>"));
		client.output(output, callId);
		Thread.sleep(5000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		OutputSample2 sample = new OutputSample2();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
