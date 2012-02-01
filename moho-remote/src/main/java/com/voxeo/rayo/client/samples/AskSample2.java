package com.voxeo.rayo.client.samples;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.Duration;

import com.rayo.core.verb.Ask;
import com.rayo.core.verb.AskCompleteEvent;
import com.rayo.core.verb.Choices;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.Ssml;

public class AskSample2 extends BaseSample {

	private String grammar = "<grammar>";
	
	public void run() throws Exception {
		
		String callId = client.waitForOffer().getCallId();
		client.answer(callId);
		
		Ask ask = new Ask();

		Ssml ssml = new Ssml("this is a test");
		ask.setPrompt(ssml);

		List<Choices> list = new ArrayList<Choices>();
		Choices choices = new Choices();
		choices.setContent(grammar);
		choices.setContentType("application/grammar+grxml");
		list.add(choices);
		ask.setChoices(list);
		ask.setTerminator('#');
		ask.setMode(InputMode.DTMF);
		ask.setTimeout(new Duration(650000));		
		
		client.command(ask, callId);
		
		AskCompleteEvent complete = (AskCompleteEvent)client.waitFor("complete");
		System.out.println("Success: " + complete.isSuccess());

		Thread.sleep(500000);
		client.hangup(callId);
	}
	
	public static void main(String[] args) throws Exception {
		
		AskSample2 sample = new AskSample2();
		sample.connect("localhost", "userc", "1", "localhost");
		sample.run();
		sample.shutdown();
	}
}
