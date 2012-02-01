package com.voxeo.rayo.client.samples;

import java.util.ArrayList;
import java.util.List;

import com.rayo.core.verb.Choices;
import com.rayo.core.verb.Input;
import com.rayo.core.verb.InputCompleteEvent;
import com.rayo.core.verb.InputMode;
import com.rayo.core.verb.Output;
import com.rayo.core.verb.Ssml;
import com.rayo.core.AnswerCommand;
import com.voxeo.rayo.client.SimpleXmppConnection;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;


public class InputSample {

	private String callId;
	
	public void run() throws Exception {
		
		XmppConnection connection = new SimpleXmppConnection("localhost");
		connection.addStanzaListener(new RayoMessageListener("offer") {
			
			@Override
			@SuppressWarnings("rawtypes")
			public void messageReceived(Object object) {
				
				Stanza stanza = (Stanza)object;
				callId = stanza.getFrom().substring(0, stanza.getFrom().indexOf('@'));
			}
		});		
		connection.connect();
		connection.login("userc", "1", "voxeo");
		connection.waitForExtension("offer");
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(buildTo(connection,callId))
			.setChild(Extension.create(answer));
		connection.send(iq);	
		
		Ssml item = new Ssml("Welcome to our Application. Please enter 5 digits.");
		Output output = new Output();
		output.setPrompt(item);
				
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(buildTo(connection,callId))
			.setChild(Extension.create(output));
		connection.sendAndWait(iq);
		
		Thread.sleep(5000);
		
		Input input = new Input();
		List<Choices> list = new ArrayList<Choices>();
		Choices grammars = new Choices();
		grammars.setContent("[4-5 DIGITS]");
		grammars.setContentType("application/grammar+voxeo");
		list.add(grammars);
		input.setGrammars(list);
		input.setTerminator('#');
		input.setMode(InputMode.DTMF);
		
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(buildTo(connection,callId))
			.setChild(Extension.create(input));
		System.out.println(String.format("sending out iq: %s",iq));
		connection.sendAndWait(iq);
		
		System.out.println("Waiting for the complete event. Type some digits in the keypad.");
		Extension extension = connection.waitForExtension("complete");
		InputCompleteEvent complete = (InputCompleteEvent)extension.getObject();
		System.out.println("Success: " + complete.isSuccess());		
	}
	
	private String buildFrom(XmppConnection connection) {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
	
	private String buildTo(XmppConnection connection, String callid) {
		
		return callid + "@" + connection.getServiceName();
	}
		
	public static void main(String[] args) throws Exception {
		
		new InputSample().run();
	}
}
