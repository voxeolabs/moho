package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.net.URISyntaxException;

import javax.media.mscontrol.join.Joinable;

import com.rayo.core.verb.VerbRef;
import com.rayo.core.DialCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.voxeo.moho.Participant.JoinType;

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
		
		VerbRef dialRef = client.dial(dial);
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
