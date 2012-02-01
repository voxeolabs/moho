package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Output;
import com.rayo.core.verb.Ssml;
import com.voxeo.moho.media.output.OutputCommand.BargeinType;

public class InterruptibleMediaSample extends BaseSample {

	public void run() throws Exception {

		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Output output = new Output();
		output.setBargeinType(BargeinType.DTMF);
		output.setPrompt(new Ssml("<audio src='http://ccmixter.org/content/7OOP3D/7OOP3D_-_One_Two_Three_(Countess_Cipher).mp3'/>"));
		client.output(output, callId);
		Thread.sleep(5000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		InterruptibleMediaSample sample = new InterruptibleMediaSample();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
