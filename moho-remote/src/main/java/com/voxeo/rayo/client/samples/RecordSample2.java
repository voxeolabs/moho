package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.Record;
import com.rayo.core.verb.VerbRef;

public class RecordSample2 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"), callId);
		Record record = new Record();
		VerbRef ref = client.record(record, callId);
		client.stop(ref);
		Thread.sleep(10000);		
		client.hangup(callId);
		Thread.sleep(10000);
	}
	
	public static void main(String[] args) throws Exception {
		
		RecordSample2 sample = new RecordSample2();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
