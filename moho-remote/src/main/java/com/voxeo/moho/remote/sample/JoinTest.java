package com.voxeo.moho.remote.sample;

import java.net.URI;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.event.RingEvent;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class JoinTest implements Observer {
  static MohoRemote mohoRemote;

  /**
   * @param args
   */
  public static void main(String[] args) {
    mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new JoinTest());
    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost", "localhost");

    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void handleInvite(final IncomingCall call) throws Exception {
    call.answer();
    call.output("hello incoming");
    call.addObserver(this);

    call.setApplicationState("personal-call");
    CallableEndpoint endpoint2 = (CallableEndpoint) mohoRemote.createEndpoint(URI
        .create("sip:mperez@localhost:3060"));
    Call call2 = endpoint2.createCall("sip:martin@example.com");
    call2.addObserver(this);

    call2.join(call, JoinType.BRIDGE, Direction.DUPLEX);
  }

  @State
  public void handleInvite(final JoinCompleteEvent event) throws Exception {
    System.out.println(event.getCause() + ".." + event.getParticipant());
  }

  @State
  public void handleInvite(final RingEvent event) throws Exception {
    System.out.println("Received ring event" + event);
  }
}
