package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.Record;
import com.rayo.core.verb.VerbRef;

public class RecordSample3 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		//client.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"));
		Record record = new Record();
		//record.setFormat("WAV");
		//record.setCodec("INFERRED");
		VerbRef ref = client.record(record, callId);
		Thread.sleep(1000);
		client.output("thanks frank", callId);
		Thread.sleep(1000);
		//client.stop(ref);
		//Thread.sleep(10000);		
		client.hangup(callId);
		Thread.sleep(1000);
	}
	
	public static void main(String[] args) throws Exception {
		
		RecordSample3 sample = new RecordSample3();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
