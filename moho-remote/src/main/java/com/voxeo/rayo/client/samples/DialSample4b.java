package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.net.URISyntaxException;

import com.rayo.core.CallRef;
import com.rayo.core.DialCommand;

public class DialSample4b extends BaseSample {

	public void run() throws Exception {
		
		DialCommand dial = new DialCommand();
		dial.setTo(new URI("sip:usera@127.0.0.1"));
		try {
			dial.setFrom(new URI("sip:userb@127.0.0.1"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CallRef dialRef = client.dial(dial);
		Thread.sleep(6000);
		client.hangup(dialRef.getCallId());
	}
		
	public static void main(String[] args) throws Exception {
		
		System.out.println("CLIENT B");
		System.out.println("--------------");
		DialSample4b sample = new DialSample4b();
		sample.connect("localhost", "user100", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
