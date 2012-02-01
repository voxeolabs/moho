package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.Conference;
import com.rayo.core.verb.Record;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbRef;


public class ConferenceSample extends BaseSample {

	public void run(Conference conference) throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		client.command(conference, callId);
		Record record = new Record();
		VerbRef ref = client.record(record, conference.getRoomName());
		client.stop(ref);
		Thread.sleep(10000);		
		client.hangup(callId);
		
		Thread.sleep(1000);
		client.hangup(callId);
		//client.say("Thank you");

	}
	
	public static void main(String[] args) throws Exception {
		
		ConferenceSample sample1 = new ConferenceSample();
		sample1.connect("localhost", "usera", "1", "localhost");
		
        Conference conference = new Conference();
        conference.setRoomName("1238");
        conference.setModerator(true);
        conference.setBeep(false);
        Ssml announcementSsml = new Ssml("has joined the conference");  
        announcementSsml.setVoice("allison");
        conference.setAnnouncement(announcementSsml);
        Ssml musicSsml = new Ssml("The moderator how not yet joined.. Listen to this awesome music while you wait.<audio src='http://www.yanni.com/music/awesome.mp3' />");
        musicSsml.setVoice("herbert");
        conference.setHoldMusic(musicSsml);

		// Launch your first soft phone to answer this call
		sample1.run(conference);
	}	
}
