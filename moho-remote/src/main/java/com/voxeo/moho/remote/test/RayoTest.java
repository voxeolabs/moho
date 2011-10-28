package com.voxeo.moho.remote.test;

import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.rayo.core.EndEvent;
import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.OutgoingCall;
import com.voxeo.moho.State;
import com.voxeo.moho.common.event.MohoOutputCompleteEvent;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.Event;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;
import com.voxeo.moho.remote.sample.SimpleAuthenticateCallbackImpl;

public class RayoTest implements Observer {

	private String xmppServer;
	private String rayoServer;
	private String username;
	private String password;
	private String sipAddress;
	private boolean autodial = true;

	private LinkedBlockingQueue<IncomingCall> callsQueue = new LinkedBlockingQueue<IncomingCall>();

	public RayoTest(String[] args) {

		xmppServer = args[0];
		rayoServer = args[1];
		username = args[2];
		password = args[3];
		sipAddress = "sip:"+username+"@"+rayoServer;
		
		if (args.length > 4) {
			for (int i=4; i < args.length; i++) {
				if (args[i].startsWith("sip")) {
					sipAddress = args[i];					
				} else if (args[i].equals("noautodial")) {
					autodial = false;
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		
		if (args.length < 4) {
			System.out.println("Usage:");
			System.out.println("====================================");
			System.out.println("java -jar moho-remote.jar xmppserver rayoserver username password [sipaddress] [noautodial]");
			System.out.println("");
			System.out.println("    xmppserver: The name or IP address of the XMPP server you are going to use.");
			System.out.println("    rayoserver: The name of the Rayo Server you are going to use. In a single ");
			System.out.println("                node deployment both XMPP and Rayo Server will be the same.");
			System.out.println("    username  : Your username on the XMPP Server.");
			System.out.println("    rayoServer: Your username's password.");
			System.out.println("    sipAddress: Optional. The SIP address to dial. If not specified it will be ");
			System.out.println("                sip:username@rayoServer");
			System.out.println("    noautodial: If specified then this test will not dial and you will have to");
			System.out.println("                use a soft phone to call Rayo. In such case, the application will ");
			System.out.println("                wait 30 seconds for a call to be made.");
			System.out.println("");
			System.out.println("Example: java -jar moho-remote.jar localhost localhost usera 1");
			System.out.println("");
			System.exit(0);
		}

		new RayoTest(args).test();
	}

	private void test() throws Exception {

		try {
	    	System.out.println("[MOHO] Beginning test.");
			
		    MohoRemote mohoRemote = new MohoRemoteImpl();
	    	System.out.println("[MOHO] Adding observer.");
		    mohoRemote.addObserver(this);
		    
	    	System.out.println("[MOHO] Authenticating...");
		    mohoRemote.connect(new SimpleAuthenticateCallbackImpl(username, password, "", "voxeo"), xmppServer, rayoServer);
	    	System.out.println("[MOHO] User authenticated.");
			
		    if (autodial) {
		    	System.out.println("[MOHO] Dialing.");
		    	OutgoingCall outgoing = dial(mohoRemote);
		    }
		    IncomingCall incoming = getIncomingCall();
		    if (incoming ==  null) {		    	
		    	System.out.println("[MOHO] No call has been received.");
		    }
	    	System.out.println("[MOHO] Got an incoming call.");
		    incoming.answer();
		    Thread.sleep(200);
		    incoming.hangup();
		    Thread.sleep(200);
		    
		    System.out.println("[MOHO] Exiting...");
		    mohoRemote.disconnect();
		    Thread.sleep(500);
		} catch (Throwable e) {
	    	System.out.println("[MOHO] Error: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		} finally {
		    System.out.println("[MOHO] Done.");
			System.exit(0);
		}
	}
	
	public OutgoingCall dial(MohoRemote mohoRemote) {
		
		System.out.println("[MOHO] Dialing Rayo Server.");						
	    CallableEndpoint endpoint = (CallableEndpoint)mohoRemote.createEndpoint(URI.create(sipAddress));
	    Call call = endpoint.createCall("sip:test@test.com");
	    call.addObserver(this);
	    call.join();
	    
	    return (OutgoingCall)call;
	}
	
	protected synchronized IncomingCall getIncomingCall() {

		try {
			return callsQueue.poll(30000,TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	
	@State
	public void handleEvent(Event event) {
		
		if (event instanceof IncomingCall) {
			System.out.println("[MOHO] Received Incoming Call.");
			IncomingCall call = (IncomingCall)event;
			call.addObserver(this);
			callsQueue.add(call);
		} else if (event instanceof AnsweredEvent) {
			System.out.println("[MOHO] Call has been answered.");			
		} else if (event instanceof MohoOutputCompleteEvent) {
			System.out.println("[MOHO] Media has been delivered to the call.");						
		} else if (event instanceof EndEvent) {
			System.out.println("[MOHO] Call has been finished.");						
		}
	}
}
