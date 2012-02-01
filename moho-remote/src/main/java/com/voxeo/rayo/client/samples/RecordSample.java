package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Record;

public class RecordSample extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		Record record = new Record();
		//record.setMaxDuration(new Duration(2000));
		record.setFormat("mp3");
		client.record(record, callId);
		Thread.sleep(10000);
		client.hangup(callId);
		System.out.println("Wait for complete");
		Thread.sleep(2000);
	}
	
	public static void main(String[] args) throws Exception {
		
		RecordSample sample = new RecordSample();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
