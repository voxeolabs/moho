package com.voxeo.moho.remote.sample;

import java.net.URI;

import javax.media.mscontrol.join.Joinable.Direction;

import com.voxeo.moho.Call;
import com.voxeo.moho.CallableEndpoint;
import com.voxeo.moho.Endpoint;
import com.voxeo.moho.IncomingCall;
import com.voxeo.moho.Participant.JoinType;
import com.voxeo.moho.State;
import com.voxeo.moho.event.CallCompleteEvent;
import com.voxeo.moho.event.Observer;
import com.voxeo.moho.remote.MohoRemote;
import com.voxeo.moho.remote.impl.MohoRemoteImpl;

public class TransferSample implements Observer {

  protected static MohoRemote mohoRemote;

  public static void main(String[] args) throws Exception {
    mohoRemote = new MohoRemoteImpl();
    mohoRemote.addObserver(new TransferSample());
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
    call.addObserver(this);
    call.output("Welcome, please wait while connecting for you").get();

    Endpoint endpoint = mohoRemote.createEndpoint(URI.create("sip:sipuserf@127.0.0.1:40000"));
    Call outgoingCall = ((CallableEndpoint) endpoint).createCall(call.getAddress());
    outgoingCall.addObserver(this);
    outgoingCall.setAttribute("incomingCll", call);
    call.join(outgoingCall, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
  }

  @State
  public void hangup(CallCompleteEvent event) throws Exception {
    Call incomingCall = (Call) event.getSource().getAttribute("incomingCll");
    if (incomingCall != null) {
      incomingCall.output("please wait while we transfering you").get();

      Endpoint endpoint = mohoRemote.createEndpoint(URI.create("sip:mzhang@172.21.99.154:7788"));
      Call outgoingCall = ((CallableEndpoint) endpoint).createCall(incomingCall.getAddress());
      incomingCall.join(outgoingCall, JoinType.BRIDGE_SHARED, Direction.DUPLEX);
    }
  }
}
