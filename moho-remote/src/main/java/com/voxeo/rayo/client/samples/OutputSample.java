package com.voxeo.rayo.client.samples;

import com.rayo.core.verb.Output;
import com.rayo.core.verb.SeekCommand;
import com.rayo.core.verb.SpeedDownCommand;
import com.rayo.core.verb.SpeedUpCommand;
import com.rayo.core.verb.Ssml;
import com.rayo.core.verb.VolumeDownCommand;
import com.rayo.core.verb.VolumeUpCommand;
import com.rayo.core.AnswerCommand;
import com.voxeo.rayo.client.SimpleXmppConnection;
import com.voxeo.rayo.client.XmppConnection;
import com.voxeo.rayo.client.listener.RayoMessageListener;
import com.voxeo.rayo.client.verb.ClientPauseCommand;
import com.voxeo.rayo.client.verb.ClientResumeCommand;
import com.voxeo.rayo.client.verb.RefEvent;
import com.voxeo.rayo.client.xmpp.extensions.Extension;
import com.voxeo.rayo.client.xmpp.stanza.IQ;
import com.voxeo.rayo.client.xmpp.stanza.Stanza;


public class OutputSample {

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
		connection.login("usera", "1", "voxeo");
		connection.waitForExtension("offer");
		
		AnswerCommand answer = new AnswerCommand();
		IQ iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(buildTo(connection,callId))
			.setChild(Extension.create(answer));
		connection.send(iq);	
		
		Ssml item = new Ssml("<audio src='http://ccmixter.org/content/7OOP3D/7OOP3D_-_One_Two_Three_(Countess_Cipher).mp3'/>");
		Output output = new Output();
		output.setPrompt(item);
				
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(buildTo(connection,callId))
			.setChild(Extension.create(output));
		System.out.println(String.format("Sending out IQ: %s", iq));
		IQ result = ((IQ)connection.sendAndWait(iq));
		
		RefEvent reference = (RefEvent)result.getExtension().getObject();
		System.out.println(String.format("Received ref: %s", reference));
		String to = callId+"@localhost/"+reference.getJid();
		
		
		ClientPauseCommand pause = new ClientPauseCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(pause));
		connection.send(iq);
		
		Thread.sleep(3000);
		
		ClientResumeCommand resume = new ClientResumeCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(resume));
		connection.send(iq);	
		
		Thread.sleep(3000);
		
		
		SeekCommand seek = new SeekCommand();
		seek.setDirection(SeekCommand.Direction.FORWARD);
		seek.setAmount(10);
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(seek));
		connection.send(iq);	
		
		Thread.sleep(3000);		
		
		SpeedDownCommand speeddown = new SpeedDownCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(speeddown));
		connection.send(iq);	
		
		Thread.sleep(3000);

		SpeedUpCommand speedup = new SpeedUpCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(speedup));
		connection.send(iq);	
		
		Thread.sleep(3000);

		VolumeUpCommand volumeup = new VolumeUpCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(volumeup));
		connection.send(iq);
		
		Thread.sleep(3000);		

		VolumeDownCommand volumedown = new VolumeDownCommand();
		iq = new IQ(IQ.Type.set)
			.setFrom(buildFrom(connection))
			.setTo(to)
			.setChild(Extension.create(volumedown));
		connection.send(iq);
		
		Thread.sleep(3000);	
	}
	
	private String buildFrom(XmppConnection connection) {
		
		return connection.getUsername() + "@" + connection.getServiceName() + "/" + connection.getResource();
	}
	
	private String buildTo(XmppConnection connection, String callid) {
		
		return callid + "@" + connection.getServiceName();
	}
		
	public static void main(String[] args) throws Exception {
		
		new OutputSample().run();
	}
}
