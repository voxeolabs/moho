package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class TransferSample2 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		// Feel free to configure with your prefered sip phone
		//client.transfer(new URI("sip:mperez@127.0.0.1:3060"));
		List<URI> destinations = new ArrayList<URI>();
		destinations.add(new URI("sip:mpermar@sip.linphone.org:5060"));
		//destinations.add(new URI("sip:user1@81.218.235.58:5060"));
		client.transfer(destinations, callId);
		
		Thread.sleep(60000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {

		TransferSample2 sample = new TransferSample2();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
