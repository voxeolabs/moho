package com.voxeo.rayo.client.test;

import java.net.URI;

import org.junit.Test;

import com.rayo.core.verb.VerbRef;
import com.voxeo.rayo.client.DefaultXmppConnectionFactory;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.internal.XmppIntegrationTest;

public class SayTest extends XmppIntegrationTest {
	
	@Test
	public void testTextSay() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.say("hello!",lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:tropo:say:1\">hello!</say></iq>");
	}

	@Test
	public void testAudioSay() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.sayAudio("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3",lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:tropo:say:1\"><audio xmlns=\"\" src=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"></audio></say></iq>");
	}

	@Test
	public void testAudioSayUri() throws Exception {
		
		rayo.answer(lastCallId);
		rayo.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"),lastCallId);
		
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"#callId@localhost\"><say xmlns=\"urn:xmpp:tropo:say:1\"><audio xmlns=\"\" src=\"http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3\"></audio></say></iq>");
	}

	@Test
	public void testStop() throws Exception {
		
		rayo.answer(lastCallId);
		VerbRef say = rayo.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"),lastCallId);
		rayo.stop(say);
		
		String toJid = say.getCallId()+"@"+rayo.getXmppConnection().getServiceName()+"/"+say.getVerbId();
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + toJid +"\"><stop xmlns=\"urn:xmpp:rayo:ext:1\"/></iq>");
	}

	@Test
	public void testResume() throws Exception {
		
		rayo.answer(lastCallId);
		VerbRef say = rayo.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"),lastCallId);
		rayo.resume(say);
		
		String toJid = say.getCallId()+"@"+rayo.getXmppConnection().getServiceName()+"/"+say.getVerbId();
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + toJid +"\"><resume xmlns=\"urn:xmpp:rayo:output:1\"></resume></iq>");
	}

	@Test
	public void testPause() throws Exception {
		
		rayo.answer(lastCallId);
		VerbRef say = rayo.say(new URI("http://ccmixter.org/content/DoKashiteru/DoKashiteru_-_you_(na-na-na-na).mp3"),lastCallId);
		rayo.pause(say);
		
		String toJid = say.getCallId()+"@"+rayo.getXmppConnection().getServiceName()+"/"+say.getVerbId();
		Thread.sleep(400);
		assertServerReceived("<iq id=\"*\" type=\"set\" from=\"userc@localhost/voxeo\" to=\"" + toJid +"\"><pause xmlns=\"urn:xmpp:rayo:output:1\"></pause></iq>");
	}
	
	@Override
	protected XmppConnection createConnection(String hostname, Integer port) {

		return new DefaultXmppConnectionFactory().createConnection(hostname, port);
	}
}
