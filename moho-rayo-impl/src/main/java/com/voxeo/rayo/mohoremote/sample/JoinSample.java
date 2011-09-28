package com.voxeo.rayo.mohoremote.sample;

import java.net.URI;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.AnsweredEvent;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.JoinCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.rayo.mohoremote.MohoRemote;
import com.voxeo.rayo.mohoremote.MohoRemoteFactory;

public class JoinSample implements Observer {

  static Observer observer = null;

  static Call outboundCall;

  /**
   * @param args
   */
  public static void main(String[] args) {
    MohoRemoteFactory mohoRemoteFactory = MohoRemoteFactory.newInstance();
    MohoRemote mohoRemote = mohoRemoteFactory.newMohoRemote();
    observer = new JoinSample();
    mohoRemote.addObserver(observer);

    mohoRemote.connect(new SimpleAuthenticateCallbackImpl("usera", "1", "", "voxeo"), "localhost");

    CallableEndpoint endpoint = mohoRemote.createEndpoint(URI.create("sip:sipuserf@127.0.0.1:54880"));

    outboundCall = endpoint.createCall("sip:mohosample@example.com");
    outboundCall.addObserver(observer);
    try {
      Thread.sleep(100 * 60 * 1000);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @State
  public void outboundAnswered(final AnsweredEvent<Call> event) {
    //event.getSource().output("Hello world");
  }

  @State
  public void handleInvite(final IncomingCall call) {
    call.answer();
    call.addObserver(observer);
    //call.output("hello world");
    call.join(outboundCall, JoinType.BRIDGE, Direction.DUPLEX);
  }

  @State
  public void joinComplete(final JoinCompleteEvent event) {
    System.out.print("Join complete:" + event.getSource() + ":" + event.getCause());
  }

  @State
  public void CallComplete(final CallCompleteEvent event) {
    System.out.print("Call complete:" + event.getSource() + ":" + event.getCause());
  }
}
