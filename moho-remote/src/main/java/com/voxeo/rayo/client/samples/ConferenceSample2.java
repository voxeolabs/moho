package com.voxeo.rayo.client.samples;

import java.net.URI;

import com.rayo.core.verb.Conference;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VerbRef;
import com.voxeo.rayo.client.JmxClient;


public class ConferenceSample2 extends BaseSample {

	public void run() throws Exception {
		
		JmxClient jmxClient = new JmxClient("localhost", "8080");
		
		VerbRef call1 = client.dial(new URI("sip:mperez@localhost:3060"));
		VerbRef call2 = client.dial(new URI("sip:mperez@localhost:3060"));
		
		Thread.sleep(10000);
		client.conference(createConference("1243"), call1.getVerbId());
		System.out.println(String.format("Active mixers: %s", jmxClient.jmxValue("com.rayo:Type=MixerStatistics", "ActiveMixersCount")));
		Thread.sleep(1000);
		client.conference(createConference("1243"), call2.getVerbId());
		System.out.println(String.format("Active mixers: %s", jmxClient.jmxValue("com.rayo:Type=MixerStatistics", "ActiveMixersCount")));
		Thread.sleep(5000);
		
		client.hangup(call1.getVerbId());
		Thread.sleep(5000);
		System.out.println(String.format("Active mixers: %s", jmxClient.jmxValue("com.rayo:Type=MixerStatistics", "ActiveMixersCount")));
		Thread.sleep(1000);
		client.hangup(call2.getVerbId());
		Thread.sleep(5000);
		System.out.println(String.format("Active mixers: %s", jmxClient.jmxValue("com.rayo:Type=MixerStatistics", "ActiveMixersCount")));
		
		Thread.sleep(1000);
		//client.say("Thank you");
	}
	
	private Conference createConference(String id) {

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

        return conference;
	}
	
	public static void main(String[] args) throws Exception {
		
		ConferenceSample2 sample1 = new ConferenceSample2();
		sample1.connect("localhost", "usera", "1", "localhost");
		

		// Launch your first soft phone to answer this call
		sample1.run();
	}	
}
