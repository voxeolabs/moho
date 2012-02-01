package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.net.URISyntaxException;

import javax.media.mscontrol.join.Joinable;

import com.rayo.core.verb.VerbRef;
import com.rayo.core.DialCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.voxeo.moho.Participant.JoinType;

public class DialSample2 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		
		DialCommand dial = new DialCommand();
		dial.setTo(new URI("sip:mperez@127.0.0.1:3060"));
		//dial.setTo(new URI("sip:192.168.1.34:5060"));
		JoinCommand join = new JoinCommand();
		join.setDirection(Joinable.Direction.DUPLEX);
		join.setMedia(JoinType.BRIDGE);
		join.setTo(callId);
		join.setType(JoinDestinationType.CALL);
		dial.setJoin(join);
		
		try {
			dial.setFrom(new URI("sip:usera@127.0.0.1:5060"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		VerbRef dialRef = client.dial(dial);
		client.waitFor("answered");
		Thread.sleep(6000);
		client.unjoin(dialRef.getVerbId(), JoinDestinationType.CALL, callId);
		Thread.sleep(6000);
		client.hangup(callId);
	}
		
	public static void main(String[] args) throws Exception {
		
		DialSample2 sample = new DialSample2();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
