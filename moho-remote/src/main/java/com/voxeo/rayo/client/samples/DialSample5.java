package com.voxeo.rayo.client.samples;

import java.net.URI;
import java.net.URISyntaxException;

import javax.media.mscontrol.join.Joinable;

import com.rayo.core.verb.VerbRef;
import com.rayo.core.DialCommand;
import com.rayo.core.JoinCommand;
import com.rayo.core.JoinDestinationType;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.rayo.client.XmppException;

public class DialSample5 extends BaseSample {

	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		
		VerbRef dial1Ref = dial(callId, "sip:mperez@127.0.0.1:3060", JoinType.BRIDGE, callId);
		VerbRef dial2Ref = dial(callId, "sip:mperez@127.0.0.1:3060", JoinType.BRIDGE, callId);
		client.hangup(dial2Ref.getVerbId());
		
		Thread.sleep(6000);
		client.unjoin(dial1Ref.getVerbId(), JoinDestinationType.CALL, callId);
		//client.unjoin(dial2Ref.getVerbId(), JoinDestinationType.CALL);
		Thread.sleep(6000);
		client.hangup(callId);
	}

	private VerbRef dial(String callId, String endpoint, JoinType type, String id) throws URISyntaxException,XmppException {
		
		DialCommand dial = new DialCommand();
		dial.setTo(new URI(endpoint));
		//dial.setTo(new URI("sip:192.168.1.34:5060"));
		JoinCommand join = new JoinCommand();
		join.setDirection(Joinable.Direction.DUPLEX);
		join.setMedia(type);
		join.setTo(id);
		join.setType(JoinDestinationType.CALL);
		dial.setJoin(join);
		
		try {
			dial.setFrom(new URI("sip:user100@127.0.0.1:5060"));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		VerbRef dialRef = client.dial(dial);

		return dialRef;
	}
		
	public static void main(String[] args) throws Exception {
		
		DialSample5 sample = new DialSample5();
		sample.connect("localhost", "user100", "1", "localhost");
		sample.run();
		sample.shutdown();
	}	
}
