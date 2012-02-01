package com.voxeo.rayo.client.samples;

import java.net.URI;

public class TransferSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		// Feel free to configure with your prefered sip phone
		client.transfer(new URI("sip:mperez@127.0.0.1:3060"), callId);
		//client.transfer(new URI("sip:192.168.1.34:5060"));
		Thread.sleep(60000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {

		TransferSample sample = new TransferSample();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
