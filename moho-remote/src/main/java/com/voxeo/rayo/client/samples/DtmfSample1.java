package com.voxeo.rayo.client.samples;


public class DtmfSample1 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.dtmf("1", callId);
		Thread.sleep(5000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		DtmfSample1 sample = new DtmfSample1();
		sample.connect("localhost", "user100", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
