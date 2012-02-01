package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.net.URISyntaxException;

import com.rayo.core.verb.VerbRef;
import com.rayo.core.DialCommand;
import com.voxeo.rayo.client.XmppException;

public class DialSample6 extends BaseSample {

	public void run() throws Exception {
		
		// Does not exist. Will timeout
		dial("sip:mperez@127.0.0.1:6060");
		Thread.sleep(50000);
	}

	private VerbRef dial(String endpoint) throws URISyntaxException,XmppException {
		
		DialCommand dial = new DialCommand();
		dial.setTo(new URI(endpoint));
		
		try {
			dial.setFrom(new URI("sip:user100@localhost:5060"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		VerbRef dialRef = client.dial(dial);

		return dialRef;
	}
		
	public static void main(String[] args) throws Exception {
		
		DialSample6 sample = new DialSample6();
		sample.connect("localhost", "user100", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
