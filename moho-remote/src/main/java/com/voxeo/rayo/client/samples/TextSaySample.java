package com.voxeo.rayo.client.samples;

import java.net.URI;


public class TextSaySample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer(900000).getCallId();
		client.answer(callId);
		client.output("Hello World.", callId);
		client.output("And this is me.", callId);
		Thread.sleep(15000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		TextSaySample sample = new TextSaySample();
		//sample.connect("go.rayo.org", "mpermar", "Voxeo2008", "telefonica116.orl.voxeo.net");
		sample.connect("go.rayo.org", "mpermar", "xxxx", "rayo-gw111.orl.voxeo.net");
		sample.run();
		sample.shutdown();
	}
}
