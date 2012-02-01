package com.voxeo.rayo.client.samples;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;

import com.rayo.core.verb.Ask;
import com.rayo.core.verb.AskCompleteEvent;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.Ssml;

public class AskSample3 extends BaseSample {
	
	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		
		Ask ask = new Ask();

		Ssml ssml = new Ssml("this is a test");
		ask.setPrompt(ssml);

		List<Choices> list = new ArrayList<Choices>();
		Choices choices = new Choices();
		choices.setContent("[5 DIGITS]");
		choices.setContentType("application/grammar+voxeo");
		list.add(choices);
		ask.setChoices(list);
		ask.setRecognizer("it-it");
		ask.setMode(InputMode.DTMF);
		ask.setTimeout(new Duration(30));		
		
		client.command(ask, callId);
		
		AskCompleteEvent complete = (AskCompleteEvent)client.waitFor("complete");
		System.out.println("Success: " + complete.isSuccess());

		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AskSample3 sample = new AskSample3();
		sample.connect("localhost", "usera", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
