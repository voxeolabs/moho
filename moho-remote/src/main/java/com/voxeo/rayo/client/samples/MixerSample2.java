package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Conference;
import com.rayo.core.verb.Ssml;


public class MixerSample2 extends BaseSample {

	public void run() throws Exception {
		
		connect("localhost", "userc", "1", "localhost");
		
		String firstCall = client.waitForOffer().getCallId();
		client.answer(firstCall);
		client.say("We are going to create a conference and then say something to it", firstCall);
		Thread.sleep(10000);
		Conference conference = buildConference();
		client.conference(conference, firstCall);
		client.ask("Questions are now allowed", "[4-5 DIGITS]", conference.getRoomName());
		Thread.sleep(15000);
		//Thread.sleep(3000);
		client.say("You obviously didn't heard that", conference.getRoomName());
		Thread.sleep(10000);
		client.hangup(firstCall);
	}
	
	private Conference buildConference() {
		
        Conference conference = new Conference();
        conference.setRoomName("123");
        conference.setModerator(true);
        conference.setBeep(false);
        Ssml announcementSsml = new Ssml("has joined the conference");  
        announcementSsml.setVoice("allison");
        conference.setAnnouncement(announcementSsml);
        Ssml musicSsml = new Ssml("The moderator how not yet joined.. Listen to this awesome music while you wait.<audio src='http://www.yanni.com/music/awesome.mp3' />");
        musicSsml.setVoice("herbert");
        conference.setHoldMusic(musicSsml);
        
        return conference;
	}
		
	public static void main(String[] args) throws Exception {
		
		new MixerSample2().run();
	}
}
